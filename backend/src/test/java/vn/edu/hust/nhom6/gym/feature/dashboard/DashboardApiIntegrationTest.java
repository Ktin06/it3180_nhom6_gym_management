package vn.edu.hust.nhom6.gym.feature.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigKeys;

/**
 * Kiểm thử tích hợp 4 API dashboard chi nhánh F6 (S2-12.1, #188): {@code /stats}, {@code /hourly-checkins},
 * {@code /expiring-memberships}, {@code /latest-checkins}. SRS F6 chưa có mã AC nên tên theo {@code US-F6.x.1 …}.
 * {@link DashboardFixtures#reset()} tự ghi lại {@code SESSION_WARNING_THRESHOLD} trong {@code @BeforeEach} (VD-11).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class DashboardApiIntegrationTest {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 21);
    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 21, 10, 0);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;

    final ObjectMapper json = new ObjectMapper();
    DashboardFixtures f;

    @BeforeEach
    void setUp() {
        clock.setVn(NOW);
        f = new DashboardFixtures(jdbc, tokens);
        f.reset();
    }

    @Test
    @DisplayName("US-F6.1.1 BR-F6.1: số đang tập bằng số hội viên khác nhau hôm nay chưa check-out, lượt 23:30 VN hôm qua không tính hôm nay, bỏ todayCheckinGrowthRate")
    void currentlyInGymAndVietnamDateBoundary() throws Exception {
        Long m1 = f.member(f.cn01, "Nguyễn Văn A");
        Long m2 = f.member(f.cn01, "Trần Thị B");
        Long pkg1 = f.membership(m1, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(25), null);
        Long pkg2 = f.membership(m2, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(25), null);

        // Hôm qua 23:30 VN (16:30 UTC hôm qua), chưa check-out -> KHÔNG tính vào hôm nay
        f.checkin(m1, pkg1, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 20, 23, 30), null);
        // Hôm nay: 3 lượt (2 chưa check-out, 1 đã check-out)
        f.checkin(m1, pkg1, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 6, 15),
            LocalDateTime.of(2026, 11, 21, 7, 30));
        f.checkin(m1, pkg1, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 8, 10), null);
        f.checkin(m2, pkg2, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 9, 0), null);
        // Ngày mai 00:00 VN (17:00 UTC hôm nay, biên mở dayEnd) -> KHÔNG tính
        f.checkin(m2, pkg2, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 22, 0, 0), null);

        // Chi nhánh CN02 -> KHÔNG lẫn vào CN01
        Long mCn02 = f.member(f.cn02, "Hội viên CN02");
        Long pkgCn02 = f.membership(mCn02, "TIME", f.cn02, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(25), null);
        f.checkin(mCn02, pkgCn02, f.cn02, f.receptionistCn02, "MANUAL", LocalDateTime.of(2026, 11, 21, 8, 0), null);

        mvc.perform(get("/api/v1/dashboard/stats").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentlyInGym").value(2))
            .andExpect(jsonPath("$.todayCheckinCount").value(3))
            .andExpect(jsonPath("$.todayRevenue").value(0))
            .andExpect(jsonPath("$.cashRevenue").value(0))
            .andExpect(jsonPath("$.transferRevenue").value(0))
            .andExpect(jsonPath("$.expiringPackagesCount").value(0))
            .andExpect(jsonPath("$.todayCheckinGrowthRate").doesNotExist());
    }

    @Test
    @DisplayName("US-F6.1.1 KB: SUPER_ADMIN thiếu branchId trả 400, branchId không tồn tại trả 404, branchId hợp lệ trả đúng chi nhánh; nhân viên gửi branchId khác bị bỏ qua")
    void branchScopeForOwnerAndStaff() throws Exception {
        Long m1 = f.member(f.cn01, "Hội viên CN01");
        Long pkg1 = f.membership(m1, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(1), TODAY.plusDays(20), null);
        f.checkin(m1, pkg1, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 7, 0), null);

        Long m2 = f.member(f.cn02, "Hội viên CN02");
        Long pkg2 = f.membership(m2, "TIME", f.cn02, "ACTIVE", TODAY.minusDays(1), TODAY.plusDays(20), null);
        f.checkin(m2, pkg2, f.cn02, f.receptionistCn02, "MANUAL", LocalDateTime.of(2026, 11, 21, 8, 0), null);
        f.checkin(m2, pkg2, f.cn02, f.receptionistCn02, "MANUAL", LocalDateTime.of(2026, 11, 21, 9, 0), null);

        String[] endpoints = {
            "/api/v1/dashboard/stats",
            "/api/v1/dashboard/hourly-checkins",
            "/api/v1/dashboard/expiring-memberships",
            "/api/v1/dashboard/latest-checkins"
        };

        // SUPER_ADMIN thiếu branchId -> 400; branchId không tồn tại -> 404
        for (String ep : endpoints) {
            mvc.perform(get(ep).with(f.owner()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Chủ chuỗi cần chọn chi nhánh cụ thể để xem Dashboard."));
            mvc.perform(get(ep).with(f.owner()).param("branchId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy chi nhánh."));
        }

        // SUPER_ADMIN chọn đúng CN02 -> 2 lượt check-in mở của cùng 1 hội viên: todayCheckinCount = 2, currentlyInGym = 1
        mvc.perform(get("/api/v1/dashboard/stats").with(f.owner()).param("branchId", String.valueOf(f.cn02)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.todayCheckinCount").value(2))
            .andExpect(jsonPath("$.currentlyInGym").value(1));

        // RECEPTIONIST và BRANCH_MANAGER CN01 gửi ?branchId={cn02} -> bị bỏ qua, vẫn trả dữ liệu CN01 (1 lượt)
        for (RequestPostProcessor staffAuth : new RequestPostProcessor[] {f.receptionCn01(), f.managerCn01()}) {
            mvc.perform(get("/api/v1/dashboard/stats").with(staffAuth).param("branchId", String.valueOf(f.cn02)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayCheckinCount").value(1))
                .andExpect(jsonPath("$.currentlyInGym").value(1));
        }
    }

    @Test
    @DisplayName("US-F6.1.1 VD25-1: một hội viên hai lượt mở trong ngày thì đang tập là 1, lượt check-in hôm nay vẫn là 2")
    void singleMemberTwoOpenCheckinsCountsAsOneCurrentlyInGym() throws Exception {
        Long member = f.member(f.cn01, "Quên check-out buổi sáng");
        Long pkg = f.membership(member, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(25), null);

        // Sáng tập quên check-out, chiều quay lại check-in lượt thứ hai cũng chưa check-out
        f.checkin(member, pkg, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 7, 30), null);
        f.checkin(member, pkg, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 17, 30), null);

        mvc.perform(get("/api/v1/dashboard/stats").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentlyInGym").value(1))
            .andExpect(jsonPath("$.todayCheckinCount").value(2));
    }

    @Test
    @DisplayName("US-F6.1.1 VD25-2: hai hội viên mỗi người một lượt mở trong ngày thì đang tập là 2, một người có thêm lượt mở thứ hai vẫn là 2")
    void twoDistinctMembersWithOpenCheckinsCountAsTwo() throws Exception {
        Long m1 = f.member(f.cn01, "Hội viên thứ nhất");
        Long m2 = f.member(f.cn01, "Hội viên thứ hai");
        Long pkg1 = f.membership(m1, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(25), null);
        Long pkg2 = f.membership(m2, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(25), null);

        f.checkin(m1, pkg1, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 8, 0), null);
        f.checkin(m2, pkg2, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 8, 15), null);

        mvc.perform(get("/api/v1/dashboard/stats").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentlyInGym").value(2))
            .andExpect(jsonPath("$.todayCheckinCount").value(2));

        // Hội viên 1 mở thêm lượt thứ hai -> số người đang tập vẫn là 2, tổng lượt tăng lên 3
        f.checkin(m1, pkg1, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 9, 30), null);

        mvc.perform(get("/api/v1/dashboard/stats").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentlyInGym").value(2))
            .andExpect(jsonPath("$.todayCheckinCount").value(3));
    }

    @Test
    @DisplayName("US-F6.1.1 VD25-3: lượt đã check-out không tính vào đang tập nhưng vẫn tính vào lượt check-in hôm nay")
    void checkedOutVisitsAreExcludedFromCurrentlyInGym() throws Exception {
        Long mCheckedOutOnly = f.member(f.cn01, "Chỉ có lượt đã về");
        Long mMixed = f.member(f.cn01, "Sáng đã về chiều đang tập");
        Long pkg1 = f.membership(mCheckedOutOnly, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(25), null);
        Long pkg2 = f.membership(mMixed, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(25), null);

        // mCheckedOutOnly: 2 lượt đều đã check-out
        f.checkin(mCheckedOutOnly, pkg1, f.cn01, f.receptionistCn01, "MANUAL",
            LocalDateTime.of(2026, 11, 21, 6, 30), LocalDateTime.of(2026, 11, 21, 7, 15));
        f.checkin(mCheckedOutOnly, pkg1, f.cn01, f.receptionistCn01, "MANUAL",
            LocalDateTime.of(2026, 11, 21, 8, 0), LocalDateTime.of(2026, 11, 21, 8, 45));
        // mMixed: 1 lượt đã check-out + 1 lượt đang mở
        f.checkin(mMixed, pkg2, f.cn01, f.receptionistCn01, "MANUAL",
            LocalDateTime.of(2026, 11, 21, 7, 0), LocalDateTime.of(2026, 11, 21, 7, 50));
        Long openCheckinId = f.checkin(mMixed, pkg2, f.cn01, f.receptionistCn01, "MANUAL",
            LocalDateTime.of(2026, 11, 21, 9, 10), null);

        mvc.perform(get("/api/v1/dashboard/stats").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentlyInGym").value(1))
            .andExpect(jsonPath("$.todayCheckinCount").value(4));

        // Khi lượt mở cuối cùng cũng check-out -> đang tập về 0, tổng lượt vẫn là 4
        jdbc.update("UPDATE checkins SET checked_out_at = ? WHERE id = ?",
            DashboardFixtures.utc(LocalDateTime.of(2026, 11, 21, 9, 55)), openCheckinId);

        mvc.perform(get("/api/v1/dashboard/stats").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentlyInGym").value(0))
            .andExpect(jsonPath("$.todayCheckinCount").value(4));
    }

    @Test
    @DisplayName("US-F6.1.1 S3: MEMBER và TRAINER bị 403 trên mọi API dashboard, thiếu token 401, tham số sai 400")
    void roleAccessAndValidation() throws Exception {
        String[] endpoints = {
            "/api/v1/dashboard/stats",
            "/api/v1/dashboard/hourly-checkins",
            "/api/v1/dashboard/expiring-memberships",
            "/api/v1/dashboard/latest-checkins"
        };
        for (String ep : endpoints) {
            mvc.perform(get(ep)).andExpect(status().isUnauthorized());
            mvc.perform(get(ep).with(f.as(f.memberUser, UserRole.MEMBER))).andExpect(status().isForbidden());
            mvc.perform(get(ep).with(f.as(f.trainerUser, UserRole.TRAINER))).andExpect(status().isForbidden());
        }

        // Tham số sai kiểu / ngoài biên -> 400
        mvc.perform(get("/api/v1/dashboard/stats").with(f.owner()).param("branchId", "0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/dashboard/stats").with(f.owner()).param("branchId", "-3"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/dashboard/stats").with(f.owner()).param("branchId", "abc"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/dashboard/expiring-memberships").with(f.receptionCn01()).param("limit", "0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/dashboard/expiring-memberships").with(f.receptionCn01()).param("limit", "101"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/dashboard/latest-checkins").with(f.receptionCn01()).param("limit", "0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/dashboard/latest-checkins").with(f.receptionCn01()).param("limit", "51"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-F6.2.1 KB: biểu đồ đủ khung giờ từ mở đến trước đóng, khung trống bằng 0, tổng khung bằng tổng lượt, không tính lượt 23:30 hôm qua")
    void hourlyCheckinsDefaultHoursZeroFillAndSumMatchesTotal() throws Exception {
        Long m = f.member(f.cn01, "Hội viên biểu đồ");
        Long pkg = f.membership(m, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(25), null);

        // Lượt 23:30 VN hôm qua -> KHÔNG tính
        f.checkin(m, pkg, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 20, 23, 30), null);
        // Lượt hôm nay: 06:10, 06:45 (khung 06:00 = 2), 18:20 (khung 18:00 = 1), 21:55 (khung 21:00 = 1)
        f.checkin(m, pkg, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 6, 10), null);
        f.checkin(m, pkg, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 6, 45), null);
        f.checkin(m, pkg, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 18, 20), null);
        f.checkin(m, pkg, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 21, 55), null);

        String body = mvc.perform(get("/api/v1/dashboard/hourly-checkins").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(16)))
            .andExpect(jsonPath("$[0].hour").value("06:00"))
            .andExpect(jsonPath("$[0].count").value(2))
            .andExpect(jsonPath("$[1].hour").value("07:00"))
            .andExpect(jsonPath("$[1].count").value(0))
            .andExpect(jsonPath("$[12].hour").value("18:00"))
            .andExpect(jsonPath("$[12].count").value(1))
            .andExpect(jsonPath("$[15].hour").value("21:00"))
            .andExpect(jsonPath("$[15].count").value(1))
            .andReturn().getResponse().getContentAsString();

        JsonNode arr = json.readTree(body);
        long sum = 0;
        for (JsonNode bucket : arr) {
            sum += bucket.get("count").asLong();
        }
        assertThat(sum).isEqualTo(4L);

        mvc.perform(get("/api/v1/dashboard/stats").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.todayCheckinCount").value(sum));
    }

    @Test
    @DisplayName("US-F6.2.1 KB: chi nhánh mở 05:30 thì khung đầu là 05:00 và nhận lượt 05:30–05:59, đọc cột time đúng giờ Việt Nam (VD-13)")
    void hourlyCheckinsEarlyOpeningTimeAndVd13() throws Exception {
        // Ghi giờ mở cửa 05:30–22:00 bằng SQL (VD-13)
        f.setBranchHours(f.cn01, "05:30", "22:00");
        Long m = f.member(f.cn01, "Hội viên tập sớm");
        Long pkg = f.membership(m, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(25), null);

        f.checkin(m, pkg, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 5, 30), null);
        f.checkin(m, pkg, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 5, 59), null);
        f.checkin(m, pkg, f.cn01, f.receptionistCn01, "MANUAL", LocalDateTime.of(2026, 11, 21, 6, 0), null);

        mvc.perform(get("/api/v1/dashboard/hourly-checkins").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(17)))
            .andExpect(jsonPath("$[0].hour").value("05:00"))
            .andExpect(jsonPath("$[0].count").value(2))
            .andExpect(jsonPath("$[1].hour").value("06:00"))
            .andExpect(jsonPath("$[1].count").value(1))
            .andExpect(jsonPath("$[2].hour").value("07:00"))
            .andExpect(jsonPath("$[2].count").value(0))
            .andExpect(jsonPath("$[16].hour").value("21:00"))
            .andExpect(jsonPath("$[16].count").value(0));

        // Thử thêm ca 18:30–20:30 (VD-13: không lệch 18:30 thành 10:30; đóng 20:30 -> khung cuối 20:00)
        f.setBranchHours(f.cn02, "18:30", "20:30");
        mvc.perform(get("/api/v1/dashboard/hourly-checkins").with(f.owner()).param("branchId", String.valueOf(f.cn02)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].hour").value("18:00"))
            .andExpect(jsonPath("$[1].hour").value("19:00"))
            .andExpect(jsonPath("$[2].hour").value("20:00"));
    }

    @Test
    @DisplayName("US-F6.3.1 BR-F6.2: doanh thu chỉ tính payment CONFIRMED của hóa đơn PAID hôm nay, tách tiền mặt và chuyển khoản, gồm hóa đơn khách lẻ (LEFT JOIN members)")
    void todayRevenueIncludesWalkInAndExcludesInvalidStatusesAndOtherDays() throws Exception {
        Long m = f.member(f.cn01, "Hội viên mua gói");

        // 1. Hóa đơn hội viên PAID + payment CONFIRMED BANK_TRANSFER lúc 00:10 VN hôm nay (17:10 UTC hôm qua -> vẫn là hôm nay VN!)
        f.invoiceWithPayment(f.cn01, m, "PAID", 2_000_000, "BANK_TRANSFER", "CONFIRMED",
            LocalDateTime.of(2026, 11, 21, 0, 10));
        // 2. Hóa đơn khách lẻ (member_id = NULL) PAID + payment CONFIRMED CASH -> BẮT BUỘC được tính (LEFT JOIN members)
        f.invoiceWithPayment(f.cn01, null, "PAID", 50_000, "CASH", "CONFIRMED",
            LocalDateTime.of(2026, 11, 21, 9, 15));
        // 3. Hóa đơn khách lẻ chuyển khoản thêm 120.000
        f.invoiceWithPayment(f.cn01, null, "PAID", 120_000, "BANK_TRANSFER", "CONFIRMED",
            LocalDateTime.of(2026, 11, 21, 9, 45));

        // Các hóa đơn KHÔNG ĐƯỢC tính:
        // - Hóa đơn PENDING (chưa thanh toán)
        f.invoiceWithPayment(f.cn01, m, "PENDING", 500_000, null, null, null);
        // - Hóa đơn CANCELLED
        f.invoiceWithPayment(f.cn01, m, "CANCELLED", 600_000, null, null, null);
        // - Hóa đơn REFUNDED + payment CANCELLED
        f.invoiceWithPayment(f.cn01, m, "REFUNDED", 700_000, "CASH", "CANCELLED",
            LocalDateTime.of(2026, 11, 21, 8, 0));
        // - Hóa đơn PAID nhưng payment bị CANCELLED
        f.invoiceWithPayment(f.cn01, m, "PAID", 800_000, "CASH", "CANCELLED",
            LocalDateTime.of(2026, 11, 21, 8, 30));
        // - Hóa đơn PAID nhưng paid_at là 23:30 VN hôm qua
        f.invoiceWithPayment(f.cn01, m, "PAID", 900_000, "CASH", "CONFIRMED",
            LocalDateTime.of(2026, 11, 20, 23, 30));
        // - Hóa đơn PAID nhưng paid_at là 00:00 VN ngày mai (biên mở dayEnd)
        f.invoiceWithPayment(f.cn01, m, "PAID", 950_000, "BANK_TRANSFER", "CONFIRMED",
            LocalDateTime.of(2026, 11, 22, 0, 0));
        // - Hóa đơn PAID chi nhánh CN02
        f.invoiceWithPayment(f.cn02, null, "PAID", 300_000, "CASH", "CONFIRMED",
            LocalDateTime.of(2026, 11, 21, 9, 0));

        mvc.perform(get("/api/v1/dashboard/stats").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.todayRevenue").value(2_170_000))
            .andExpect(jsonPath("$.cashRevenue").value(50_000))
            .andExpect(jsonPath("$.transferRevenue").value(2_120_000));
    }

    @Test
    @DisplayName("US-F6.4.1 BR-F6.5: lọc gói hết hạn trong [today, today+7) và gói SESSION còn <= SESSION_WARNING_THRESHOLD đọc từ system_configs (kể cả ghi đè theo chi nhánh)")
    void expiringMembershipsDateWindowAndConfigThreshold() throws Exception {
        Long mToday = f.member(f.cn01, "Hết hạn hôm nay");
        Long mDay6 = f.member(f.cn01, "Hết hạn sau 6 ngày");
        Long mDay7 = f.member(f.cn01, "Hết hạn sau 7 ngày (ngoài biên)");
        Long mYesterday = f.member(f.cn01, "Đã quá hạn hôm qua");
        Long mSess2 = f.member(f.cn01, "SESSION còn 2 buổi");
        Long mSess3 = f.member(f.cn01, "SESSION còn 3 buổi");
        Long mPtLow = f.member(f.cn01, "PT còn 1 buổi nhưng xa hạn");
        Long mPtExpiring = f.member(f.cn01, "PT sắp hết hạn theo ngày");

        Long msToday = f.membership(mToday, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(29), TODAY, null);
        Long msDay6 = f.membership(mDay6, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(23), TODAY.plusDays(6), null);
        f.membership(mDay7, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(22), TODAY.plusDays(7), null);
        f.membership(mYesterday, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(30), TODAY.minusDays(1), null);
        Long msSess2 = f.membership(mSess2, "SESSION", f.cn01, "ACTIVE", TODAY.minusDays(10), TODAY.plusDays(30), 2);
        Long msSess3 = f.membership(mSess3, "SESSION", f.cn01, "ACTIVE", TODAY.minusDays(10), TODAY.plusDays(30), 3);
        f.membership(mPtLow, "PT", f.cn01, "ACTIVE", TODAY.minusDays(10), TODAY.plusDays(30), 1);
        Long msPtExpiring = f.membership(mPtExpiring, "PT", f.cn01, "ACTIVE", TODAY.minusDays(25), TODAY.plusDays(4), 5);

        // Với mặc định toàn chuỗi SESSION_WARNING_THRESHOLD = 2 -> 4 gói: msToday, msPtExpiring, msDay6, msSess2
        mvc.perform(get("/api/v1/dashboard/expiring-memberships").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(4)))
            .andExpect(jsonPath("$[0].membershipId").value(msToday))
            .andExpect(jsonPath("$[0].daysRemaining").value(0))
            .andExpect(jsonPath("$[0].remainingSessions").value(nullValue()))
            .andExpect(jsonPath("$[0].warningReason").value("EXPIRING_DATE"))
            .andExpect(jsonPath("$[1].membershipId").value(msPtExpiring))
            .andExpect(jsonPath("$[1].packageType").value("PT"))
            .andExpect(jsonPath("$[1].daysRemaining").value(4))
            .andExpect(jsonPath("$[1].remainingSessions").value(nullValue()))
            .andExpect(jsonPath("$[1].warningReason").value("EXPIRING_DATE"))
            .andExpect(jsonPath("$[2].membershipId").value(msDay6))
            .andExpect(jsonPath("$[2].daysRemaining").value(6))
            .andExpect(jsonPath("$[2].warningReason").value("EXPIRING_DATE"))
            .andExpect(jsonPath("$[3].membershipId").value(msSess2))
            .andExpect(jsonPath("$[3].remainingSessions").value(2))
            .andExpect(jsonPath("$[3].warningReason").value("LOW_SESSIONS"));

        // Đổi cấu hình riêng cho CN01: SESSION_WARNING_THRESHOLD = 3 -> msSess3 xuất hiện thêm (tổng 5)
        f.setBranchConfig(f.cn01, SystemConfigKeys.SESSION_WARNING_THRESHOLD, 3);
        mvc.perform(get("/api/v1/dashboard/expiring-memberships").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(5)))
            .andExpect(jsonPath("$[4].membershipId").value(msSess3))
            .andExpect(jsonPath("$[4].remainingSessions").value(3))
            .andExpect(jsonPath("$[4].warningReason").value("LOW_SESSIONS"));

        mvc.perform(get("/api/v1/dashboard/stats").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expiringPackagesCount").value(5));
    }

    @Test
    @DisplayName("US-F6.4.1 BR-F6.5: bỏ gói đã có UPCOMING cùng loại và phạm vi, giữ gói mới có PENDING hoặc UPCOMING khác loại; gói toàn chuỗi chỉ hiện ở chi nhánh nhà")
    void expiringMembershipsExcludesUpcomingAndScopesChainPackagesToHomeBranch() throws Exception {
        // 1. Có UPCOMING cùng loại + cùng CN01 -> BỊ LOẠI
        Long mRenewed = f.member(f.cn01, "Đã gia hạn CN01");
        f.membership(mRenewed, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(27), TODAY.plusDays(2), null);
        f.membership(mRenewed, "TIME", f.cn01, "UPCOMING", TODAY.plusDays(3), TODAY.plusDays(32), null);

        // 2. Chỉ mới có PENDING cùng loại -> VẪN CẢNH BÁO
        Long mPendingOnly = f.member(f.cn01, "Mới lập hóa đơn PENDING");
        Long msPendingOnly = f.membership(mPendingOnly, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(27), TODAY.plusDays(2), null);
        f.membership(mPendingOnly, "TIME", f.cn01, "PENDING", TODAY.plusDays(3), TODAY.plusDays(32), null);

        // 3. Có UPCOMING khác loại (PT) -> VẪN CẢNH BÁO gói TIME
        Long mOtherType = f.member(f.cn01, "UPCOMING khác loại");
        Long msOtherType = f.membership(mOtherType, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(27), TODAY.plusDays(3), null);
        f.membership(mOtherType, "PT", f.cn01, "UPCOMING", TODAY.plusDays(1), TODAY.plusDays(30), 10);

        // 4. Gói toàn chuỗi (branch_id IS NULL) của hội viên chi nhánh nhà CN01 -> hiện ở CN01, KHÔNG hiện ở CN02
        Long mChainCn01 = f.member(f.cn01, "Gói toàn chuỗi CN01");
        Long msChainCn01 = f.membership(mChainCn01, "TIME", null, "ACTIVE", TODAY.minusDays(27), TODAY.plusDays(1), null);

        // 5. Gói toàn chuỗi đã có UPCOMING toàn chuỗi (IS NOT DISTINCT FROM NULL) -> BỊ LOẠI
        Long mChainRenewed = f.member(f.cn01, "Toàn chuỗi đã gia hạn");
        f.membership(mChainRenewed, "TIME", null, "ACTIVE", TODAY.minusDays(27), TODAY.plusDays(1), null);
        f.membership(mChainRenewed, "TIME", null, "UPCOMING", TODAY.plusDays(2), TODAY.plusDays(31), null);

        // 6. Hội viên xóa mềm -> BỊ LOẠI
        Long mDeleted = f.member(f.cn01, "Hội viên đã xóa");
        f.membership(mDeleted, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(27), TODAY.plusDays(1), null);
        jdbc.update("UPDATE members SET deleted_at = now() WHERE id = ?", mDeleted);

        // CN01 thấy đúng 3 gói: msChainCn01 (+1 ngày), msPendingOnly (+2 ngày), msOtherType (+3 ngày)
        mvc.perform(get("/api/v1/dashboard/expiring-memberships").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].membershipId").value(msChainCn01))
            .andExpect(jsonPath("$[1].membershipId").value(msPendingOnly))
            .andExpect(jsonPath("$[2].membershipId").value(msOtherType));

        // CN02 KHÔNG thấy gói toàn chuỗi của hội viên CN01
        mvc.perform(get("/api/v1/dashboard/expiring-memberships").with(f.receptionCn02()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("US-F6.4.1 BR-F6.5: sắp xếp gói SESSION 0 buổi lên đầu rồi end_date tăng dần, warningReason đúng quy tắc, limit và expiringPackagesCount khớp")
    void expiringMembershipsOrderingReasonLimitAndAvatar() throws Exception {
        Long mZeroFar = f.member(f.cn01, "SESSION 0 buổi hạn xa");
        f.addAvatar(mZeroFar);
        Long mSessBoth = f.member(f.cn01, "SESSION 1 buổi và sắp hết ngày");
        Long mTimeSoon = f.member(f.cn01, "TIME hết hạn ngày mai");
        Long mSessLowFar = f.member(f.cn01, "SESSION 1 buổi hạn xa");

        // SESSION 0 buổi nhưng còn 20 ngày -> phải xếp ĐẦU TIÊN và warningReason = LOW_SESSIONS
        Long msZeroFar = f.membership(mZeroFar, "SESSION", f.cn01, "ACTIVE", TODAY.minusDays(10), TODAY.plusDays(20), 0);
        // TIME hết hạn +1 ngày -> xếp thứ 2, EXPIRING_DATE
        Long msTimeSoon = f.membership(mTimeSoon, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(28), TODAY.plusDays(1), null);
        // SESSION còn 1 buổi VÀ hết hạn +3 ngày -> xếp thứ 3, thỏa cả hai nhưng > 0 buổi -> EXPIRING_DATE (UC-F6.4 bước 3)
        Long msSessBoth = f.membership(mSessBoth, "SESSION", f.cn01, "ACTIVE", TODAY.minusDays(26), TODAY.plusDays(3), 1);
        // SESSION còn 1 buổi, hạn +25 ngày -> xếp thứ 4, LOW_SESSIONS
        Long msSessLowFar = f.membership(mSessLowFar, "SESSION", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(25), 1);

        mvc.perform(get("/api/v1/dashboard/expiring-memberships").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(4)))
            .andExpect(jsonPath("$[0].membershipId").value(msZeroFar))
            .andExpect(jsonPath("$[0].hasAvatar").value(true))
            .andExpect(jsonPath("$[0].avatarUrl").doesNotExist())
            .andExpect(jsonPath("$[0].remainingSessions").value(0))
            .andExpect(jsonPath("$[0].warningReason").value("LOW_SESSIONS"))
            .andExpect(jsonPath("$[1].membershipId").value(msTimeSoon))
            .andExpect(jsonPath("$[1].hasAvatar").value(false))
            .andExpect(jsonPath("$[1].warningReason").value("EXPIRING_DATE"))
            .andExpect(jsonPath("$[2].membershipId").value(msSessBoth))
            .andExpect(jsonPath("$[2].remainingSessions").value(1))
            .andExpect(jsonPath("$[2].warningReason").value("EXPIRING_DATE"))
            .andExpect(jsonPath("$[3].membershipId").value(msSessLowFar))
            .andExpect(jsonPath("$[3].remainingSessions").value(1))
            .andExpect(jsonPath("$[3].warningReason").value("LOW_SESSIONS"));

        // limit=2 chỉ trả 2 dòng đầu, nhưng /stats.expiringPackagesCount vẫn là 4
        mvc.perform(get("/api/v1/dashboard/expiring-memberships").with(f.receptionCn01()).param("limit", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].membershipId").value(msZeroFar))
            .andExpect(jsonPath("$[1].membershipId").value(msTimeSoon));

        mvc.perform(get("/api/v1/dashboard/stats").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expiringPackagesCount").value(4));
    }

    @Test
    @DisplayName("US-F6.5.1 KB: 10 lượt check-in mới nhất trong ngày có hasAvatar, không có avatarUrl, đúng trạng thái và thời lượng, không gồm lượt 23:30 hôm qua")
    void latestCheckinsTopTenWithHasAvatarAndNoAvatarUrl() throws Exception {
        Long mWithAvatar = f.member(f.cn01, "Có ảnh đại diện");
        f.addAvatar(mWithAvatar);
        Long mNoAvatar = f.member(f.cn01, "Chưa có ảnh");
        Long pkg1 = f.membership(mWithAvatar, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(25), null);
        Long pkg2 = f.membership(mNoAvatar, "SESSION", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(25), 5);

        // Lượt 23:30 hôm qua -> KHÔNG xuất hiện
        f.checkin(mWithAvatar, pkg1, f.cn01, f.receptionistCn01, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 23, 30), null);

        // Tạo 12 lượt hôm nay (từ 07:00 đến 08:50, mỗi lượt cách 10 phút)
        Long[] ids = new Long[12];
        for (int i = 0; i < 12; i++) {
            LocalDateTime inVn = LocalDateTime.of(2026, 11, 21, 7, 0).plusMinutes(i * 10L);
            boolean even = (i % 2 == 0);
            // Lượt cuối cùng (i = 11) đang tập (chưa check-out), lượt kế cuối (i = 10) đã check-out sau 45 phút
            LocalDateTime outVn = (i == 11) ? null : inVn.plusMinutes(45);
            ids[i] = f.checkin(even ? mWithAvatar : mNoAvatar, even ? pkg1 : pkg2, f.cn01, f.receptionistCn01,
                even ? "MANUAL" : "FACE", inVn, outVn);
        }

        // Chi nhánh CN02 -> KHÔNG xuất hiện
        Long mCn02 = f.member(f.cn02, "Khách CN02");
        Long pkgCn02 = f.membership(mCn02, "TIME", f.cn02, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(25), null);
        f.checkin(mCn02, pkgCn02, f.cn02, f.receptionistCn02, "MANUAL", LocalDateTime.of(2026, 11, 21, 9, 50), null);

        mvc.perform(get("/api/v1/dashboard/latest-checkins").with(f.receptionCn01()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(10)))
            // Mới nhất: i = 11 (lẻ -> mNoAvatar, FACE, đang tập)
            .andExpect(jsonPath("$[0].checkinId").value(ids[11]))
            .andExpect(jsonPath("$[0].memberId").value(mNoAvatar))
            .andExpect(jsonPath("$[0].fullName").value("Chưa có ảnh"))
            .andExpect(jsonPath("$[0].hasAvatar").value(false))
            .andExpect(jsonPath("$[0].avatarUrl").doesNotExist())
            .andExpect(jsonPath("$[0].packageType").value("SESSION"))
            .andExpect(jsonPath("$[0].method").value("FACE"))
            .andExpect(jsonPath("$[0].checkedOutAt").value(nullValue()))
            .andExpect(jsonPath("$[0].durationMinutes").value(nullValue()))
            .andExpect(jsonPath("$[0].status").value("CHECKED_IN"))
            // Kế tiếp: i = 10 (chẵn -> mWithAvatar, MANUAL, đã check-out sau 45 phút)
            .andExpect(jsonPath("$[1].checkinId").value(ids[10]))
            .andExpect(jsonPath("$[1].memberId").value(mWithAvatar))
            .andExpect(jsonPath("$[1].hasAvatar").value(true))
            .andExpect(jsonPath("$[1].avatarUrl").doesNotExist())
            .andExpect(jsonPath("$[1].packageType").value("TIME"))
            .andExpect(jsonPath("$[1].method").value("MANUAL"))
            .andExpect(jsonPath("$[1].durationMinutes").value(45))
            .andExpect(jsonPath("$[1].status").value("CHECKED_OUT"))
            // Lượt thứ 10 là i = 2 (i = 0, 1 bị cắt do LIMIT 10)
            .andExpect(jsonPath("$[9].checkinId").value(ids[2]));

        // Truyền limit=5 -> trả đúng 5 lượt mới nhất
        mvc.perform(get("/api/v1/dashboard/latest-checkins").with(f.receptionCn01()).param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(5)))
            .andExpect(jsonPath("$[0].checkinId").value(ids[11]))
            .andExpect(jsonPath("$[4].checkinId").value(ids[7]));
    }
}
