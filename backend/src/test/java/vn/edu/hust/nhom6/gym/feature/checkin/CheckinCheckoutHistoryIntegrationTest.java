package vn.edu.hust.nhom6.gym.feature.checkin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures.Account;

/**
 * Check-out {@code POST /api/v1/checkins/{id}/checkout} (US-F4.4.1) và lịch sử check-in trong ngày
 * {@code GET /api/v1/checkins} (US-F4.5.1). S2-11.2. SRS F4 chưa có mã AC nên tên theo US và kịch bản.
 * {@link CheckinFixtures#reset()} gọi {@code BookingFixtures.reset()} ghi lại cấu hình {@code system_configs} (VD-11).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class CheckinCheckoutHistoryIntegrationTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 9, 0);
    static final String CHECKOUT_NOT_FOUND = "Hội viên không có lượt tập nào đang diễn ra cần check-out.";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;

    final ObjectMapper json = new ObjectMapper();
    CheckinFixtures c;
    Account hv;

    @BeforeEach
    void setUp() {
        clock.setVn(NOW);
        c = new CheckinFixtures(jdbc, tokens);
        c.reset();
        hv = c.member();
    }

    Long createCheckin(RequestPostProcessor auth, Long memberId) throws Exception {
        String body = mvc.perform(post("/api/v1/checkins").with(auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"memberId\": " + memberId + "}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("checkinId").asLong();
    }

    Long insertCheckinSql(Long memberId, Long membershipId, Long branchId, Long createdBy, String method,
            LocalDateTime checkedInVn, LocalDateTime checkedOutVn) {
        OffsetDateTime inUtc = BookingFixtures.utc(checkedInVn);
        OffsetDateTime outUtc = checkedOutVn == null ? null : BookingFixtures.utc(checkedOutVn);
        return jdbc.queryForObject("""
            INSERT INTO checkins (member_id, membership_id, branch_id, created_by, method, match_score,
                                  checkin_date, session_deducted, checked_in_at, checked_out_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, false, ?, ?) RETURNING id""", Long.class,
            memberId, membershipId, branchId, createdBy, method,
            "FACE".equals(method) ? new java.math.BigDecimal("0.9500") : null,
            checkedInVn.toLocalDate(), inUtc, outUtc);
    }

    ResultActions checkout(RequestPostProcessor auth, long checkinId) throws Exception {
        return mvc.perform(post("/api/v1/checkins/" + checkinId + "/checkout").with(auth));
    }

    String code(Long memberId) {
        return jdbc.queryForObject("SELECT member_code FROM members WHERE id = ?", String.class, memberId);
    }

    // ------------------------------------------------------------------------------------------------
    // US-F4.4.1: check-out tại quầy
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US-F4.4.1 KB: check-out lượt đang tập trả 200, ghi checked_out_at và durationMinutes đúng; quản lý chi nhánh cũng gọi được")
    void checkoutSuccessAndManagerAllowed() throws Exception {
        c.time(hv.memberId());
        Long checkinId = createCheckin(c.reception(), hv.memberId());

        // Kiểm tra đang tập
        mvc.perform(get("/api/v1/checkins/search-member").with(c.reception()).param("keyword", code(hv.memberId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isCurrentlyCheckedIn").value(true));

        // 105 phút sau: 10:45 VN = 03:45 UTC
        clock.setVn(LocalDateTime.of(2026, 11, 20, 10, 45));
        checkout(c.reception(), checkinId).andExpect(status().isOk())
            .andExpect(jsonPath("$.checkinId").value(checkinId))
            .andExpect(jsonPath("$.checkedOutAt").value("2026-11-20T03:45:00Z"))
            .andExpect(jsonPath("$.durationMinutes").value(105))
            .andExpect(jsonPath("$.message").value("Check-out thành công. Hẹn gặp lại quý hội viên!"));

        var row = jdbc.queryForMap("SELECT checked_out_at, updated_at FROM checkins WHERE id = ?", checkinId);
        assertThat(row.get("checked_out_at")).isNotNull();
        assertThat(row.get("updated_at")).isNotNull();

        mvc.perform(get("/api/v1/checkins/search-member").with(c.reception()).param("keyword", code(hv.memberId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isCurrentlyCheckedIn").value(false));

        // Quản lý chi nhánh cũng check-out được (ngay lập tức -> 0 phút)
        Long secondId = createCheckin(c.as(c.manager, UserRole.BRANCH_MANAGER), hv.memberId());
        checkout(c.as(c.manager, UserRole.BRANCH_MANAGER), secondId).andExpect(status().isOk())
            .andExpect(jsonPath("$.checkinId").value(secondId))
            .andExpect(jsonPath("$.durationMinutes").value(0));
    }

    @Test
    @DisplayName("US-F4.4.1 KB: hội viên có hai lượt mở trong ngày thì check-out chỉ đóng đúng lượt được chọn")
    void checkoutClosesOnlyTargetedOpenCheckin() throws Exception {
        c.time(hv.memberId());
        Long first = createCheckin(c.reception(), hv.memberId());
        clock.setVn(NOW.plusMinutes(30));
        Long second = createCheckin(c.reception(), hv.memberId());

        clock.setVn(NOW.plusMinutes(60));
        checkout(c.reception(), first).andExpect(status().isOk())
            .andExpect(jsonPath("$.checkinId").value(first))
            .andExpect(jsonPath("$.durationMinutes").value(60));

        assertThat(jdbc.queryForObject("SELECT checked_out_at FROM checkins WHERE id = ?", OffsetDateTime.class, first))
            .isNotNull();
        assertThat(jdbc.queryForObject("SELECT checked_out_at FROM checkins WHERE id = ?", OffsetDateTime.class, second))
            .isNull();

        // Lượt thứ hai vẫn đang mở
        mvc.perform(get("/api/v1/checkins/search-member").with(c.reception()).param("keyword", code(hv.memberId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isCurrentlyCheckedIn").value(true));

        checkout(c.reception(), second).andExpect(status().isOk())
            .andExpect(jsonPath("$.durationMinutes").value(30));
    }

    @Test
    @DisplayName("US-F4.4.1 E1: check-out hai lần, lượt chi nhánh khác, hoặc id không tồn tại đều trả 404 cùng câu SRS")
    void checkoutTwiceOtherBranchOrNotFoundIs404() throws Exception {
        c.time(hv.memberId());
        Long checkinId = createCheckin(c.reception(), hv.memberId());
        clock.setVn(NOW.plusMinutes(45));
        checkout(c.reception(), checkinId).andExpect(status().isOk());
        OffsetDateTime firstCheckout = jdbc.queryForObject("SELECT checked_out_at FROM checkins WHERE id = ?",
            OffsetDateTime.class, checkinId);

        // Check-out lần hai -> 404 E1, không ghi đè checked_out_at
        clock.setVn(NOW.plusMinutes(90));
        checkout(c.reception(), checkinId).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(CHECKOUT_NOT_FOUND));
        assertThat(jdbc.queryForObject("SELECT checked_out_at FROM checkins WHERE id = ?",
            OffsetDateTime.class, checkinId)).isEqualTo(firstCheckout);

        // Lượt của chi nhánh khác (CN02) -> lễ tân CN01 nhận 404 E1, không đóng lượt CN02
        Account otherBranchMember = c.b.account(c.cn02);
        c.membership(otherBranchMember.memberId(), "TIME", c.cn02, "ACTIVE", null);
        Long cn02CheckinId = createCheckin(c.as(c.receptionistCn02, UserRole.RECEPTIONIST), otherBranchMember.memberId());
        checkout(c.reception(), cn02CheckinId).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(CHECKOUT_NOT_FOUND));
        assertThat(jdbc.queryForObject("SELECT checked_out_at FROM checkins WHERE id = ?",
            OffsetDateTime.class, cn02CheckinId)).isNull();

        // ID không tồn tại -> 404 E1
        checkout(c.reception(), 999_999L).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(CHECKOUT_NOT_FOUND));
    }

    @Test
    @DisplayName("US-F4.4.1 S3: MEMBER, TRAINER, SUPER_ADMIN không check-out được (403); id sai trả 400")
    void checkoutRoleAndValidation() throws Exception {
        c.time(hv.memberId());
        Long checkinId = createCheckin(c.reception(), hv.memberId());

        for (RequestPostProcessor denied : new RequestPostProcessor[] {
                c.b.member(hv), c.as(c.trainerUser, UserRole.TRAINER), c.as(c.owner, UserRole.SUPER_ADMIN)}) {
            checkout(denied, checkinId).andExpect(status().isForbidden());
        }
        assertThat(jdbc.queryForObject("SELECT checked_out_at FROM checkins WHERE id = ?",
            OffsetDateTime.class, checkinId)).isNull();

        mvc.perform(post("/api/v1/checkins/0/checkout").with(c.reception())).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/checkins/-5/checkout").with(c.reception())).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/checkins/abc/checkout").with(c.reception())).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------------------------------------
    // US-F4.5.1: lịch sử check-in trong ngày
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US-F4.5.1 KB: mặc định hôm nay theo ngày Việt Nam (lượt 23:30 VN hôm qua không hiện, lượt 00:10 VN hôm nay có hiện, lượt 00:00 VN ngày mai không hiện), mới nhất trước")
    void historyDefaultTodayVietnamDateBoundaries() throws Exception {
        Long pkg = c.time(hv.memberId());
        // 23:30 VN hôm qua (19/11 16:30 UTC) -> KHÔNG hiện ở hôm nay 20/11
        Long yesterdayLate = insertCheckinSql(hv.memberId(), pkg, c.cn01, c.receptionist, "MANUAL",
            LocalDateTime.of(2026, 11, 19, 23, 30), null);
        // 00:10 VN hôm nay (19/11 17:10 UTC — vẫn là ngày 19 ở UTC!) -> CÓ hiện ở hôm nay 20/11
        Long todayEarly = insertCheckinSql(hv.memberId(), pkg, c.cn01, c.receptionist, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 0, 10), null);
        // 09:00 VN hôm nay (20/11 02:00 UTC) -> CÓ hiện, đứng trước 00:10
        Long todayMorning = insertCheckinSql(hv.memberId(), pkg, c.cn01, c.receptionist, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 9, 0), null);
        // 00:00 VN ngày mai (20/11 17:00 UTC — biên mở dayEnd) -> KHÔNG hiện ở hôm nay 20/11
        Long tomorrowStart = insertCheckinSql(hv.memberId(), pkg, c.cn01, c.receptionist, "MANUAL",
            LocalDateTime.of(2026, 11, 21, 0, 0), null);

        mvc.perform(get("/api/v1/checkins").with(c.reception()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].checkinId").value(todayMorning))
            .andExpect(jsonPath("$.content[1].checkinId").value(todayEarly));

        // Chọn ngày hôm qua 2026-11-19 -> chỉ hiện lượt 23:30 hôm qua
        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("date", "2026-11-19"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].checkinId").value(yesterdayLate));

        // Chọn ngày mai 2026-11-21 -> chỉ hiện lượt 00:00 ngày mai
        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("date", "2026-11-21"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].checkinId").value(tomorrowStart));
    }

    @Test
    @DisplayName("US-F4.5.1 KB: mỗi dòng lịch sử có hasAvatar (không có avatarUrl), tên gói, nhân viên thực hiện, thời lượng và trạng thái")
    void historyFieldsAndHasAvatar() throws Exception {
        Account hvWithAvatar = hv;
        Account hvNoAvatar = c.member();
        jdbc.update("INSERT INTO member_avatars (member_id, content, size_bytes, uploaded_by) VALUES (?, ?, 3, ?)",
            hvWithAvatar.memberId(), new byte[] {1, 2, 3}, c.receptionist);
        Long pkg1 = c.time(hvWithAvatar.memberId());
        Long pkg2 = c.session(hvNoAvatar.memberId(), 10);

        Long closedId = insertCheckinSql(hvWithAvatar.memberId(), pkg1, c.cn01, c.receptionist, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 8, 0), LocalDateTime.of(2026, 11, 20, 9, 30));
        Long openId = insertCheckinSql(hvNoAvatar.memberId(), pkg2, c.cn01, c.manager, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 9, 0), null);

        mvc.perform(get("/api/v1/checkins").with(c.reception()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            // Mới nhất trước: openId (09:00)
            .andExpect(jsonPath("$.content[0].checkinId").value(openId))
            .andExpect(jsonPath("$.content[0].memberId").value(hvNoAvatar.memberId()))
            .andExpect(jsonPath("$.content[0].memberCode").value(code(hvNoAvatar.memberId())))
            .andExpect(jsonPath("$.content[0].fullName").isNotEmpty())
            .andExpect(jsonPath("$.content[0].hasAvatar").value(false))
            .andExpect(jsonPath("$.content[0].avatarUrl").doesNotExist())
            .andExpect(jsonPath("$.content[0].membershipId").value(pkg2))
            .andExpect(jsonPath("$.content[0].packageName").value(containsString("Gói")))
            .andExpect(jsonPath("$.content[0].packageType").value("SESSION"))
            .andExpect(jsonPath("$.content[0].method").value("MANUAL"))
            .andExpect(jsonPath("$.content[0].checkinDate").value("2026-11-20"))
            .andExpect(jsonPath("$.content[0].checkedInAt").value("2026-11-20T02:00:00Z"))
            .andExpect(jsonPath("$.content[0].checkedOutAt").value(nullValue()))
            .andExpect(jsonPath("$.content[0].durationMinutes").value(nullValue()))
            .andExpect(jsonPath("$.content[0].status").value("CHECKED_IN"))
            .andExpect(jsonPath("$.content[0].createdBy").value(c.manager))
            .andExpect(jsonPath("$.content[0].createdByName").isNotEmpty())
            // Lượt trước: closedId (08:00 -> 09:30 = 90 phút, có ảnh)
            .andExpect(jsonPath("$.content[1].checkinId").value(closedId))
            .andExpect(jsonPath("$.content[1].hasAvatar").value(true))
            .andExpect(jsonPath("$.content[1].avatarUrl").doesNotExist())
            .andExpect(jsonPath("$.content[1].packageType").value("TIME"))
            .andExpect(jsonPath("$.content[1].checkedInAt").value("2026-11-20T01:00:00Z"))
            .andExpect(jsonPath("$.content[1].checkedOutAt").value("2026-11-20T02:30:00Z"))
            .andExpect(jsonPath("$.content[1].durationMinutes").value(90))
            .andExpect(jsonPath("$.content[1].status").value("CHECKED_OUT"))
            .andExpect(jsonPath("$.content[1].createdBy").value(c.receptionist))
            .andExpect(jsonPath("$.content[1].createdByName").isNotEmpty());
    }

    @Test
    @DisplayName("US-F4.5.1 KB: lọc theo trạng thái CHECKED_IN và CHECKED_OUT đúng")
    void historyFilterByStatus() throws Exception {
        Long pkg = c.time(hv.memberId());
        Long closedId = insertCheckinSql(hv.memberId(), pkg, c.cn01, c.receptionist, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 7, 30), LocalDateTime.of(2026, 11, 20, 8, 30));
        Long openId = insertCheckinSql(hv.memberId(), pkg, c.cn01, c.receptionist, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 9, 0), null);

        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("status", "CHECKED_IN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].checkinId").value(openId));

        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("status", "CHECKED_OUT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].checkinId").value(closedId));
    }

    @Test
    @DisplayName("US-F4.5.1 KB: lọc theo phương thức MANUAL/FACE và tìm theo tên hoặc mã hội viên (không phân biệt hoa thường, escape ký tự LIKE)")
    void historyFilterByMethodAndKeyword() throws Exception {
        Account hoa = hv;
        Account binh = c.member();
        jdbc.update("UPDATE members SET full_name = 'Nguyễn Thị Hoa' WHERE id = ?", hoa.memberId());
        jdbc.update("UPDATE members SET full_name = 'Trần 100% Bình' WHERE id = ?", binh.memberId());

        Long pkgHoa = c.time(hoa.memberId());
        Long pkgBinh = c.time(binh.memberId());
        Long manualId = insertCheckinSql(hoa.memberId(), pkgHoa, c.cn01, c.receptionist, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 8, 0), null);
        Long faceId = insertCheckinSql(binh.memberId(), pkgBinh, c.cn01, c.receptionist, "FACE",
            LocalDateTime.of(2026, 11, 20, 9, 0), LocalDateTime.of(2026, 11, 20, 10, 0));

        // Lọc phương thức
        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("method", "FACE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].checkinId").value(faceId))
            .andExpect(jsonPath("$.content[0].method").value("FACE"));

        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("method", "MANUAL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].checkinId").value(manualId));

        // Tìm theo tên (chữ thường, có dấu)
        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("keyword", "  nguyễn thị hoa "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].checkinId").value(manualId));

        // Tìm theo mã hội viên (chữ thường)
        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("keyword", code(binh.memberId()).toLowerCase()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].checkinId").value(faceId));

        // Escape ký tự LIKE %: chỉ khớp "Trần 100% Bình", không khớp Hoa
        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("keyword", "100%"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].checkinId").value(faceId));

        // Kết hợp nhiều bộ lọc
        mvc.perform(get("/api/v1/checkins").with(c.reception())
                .param("status", "CHECKED_IN").param("method", "MANUAL").param("keyword", "hoa"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].checkinId").value(manualId));

        mvc.perform(get("/api/v1/checkins").with(c.reception())
                .param("status", "CHECKED_OUT").param("method", "MANUAL").param("keyword", "hoa"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("US-F4.5.1 KB: phân trang page và size trả PageResponse đúng thứ tự mới nhất trước")
    void historyPagination() throws Exception {
        Long pkg = c.time(hv.memberId());
        Long id1 = insertCheckinSql(hv.memberId(), pkg, c.cn01, c.receptionist, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 8, 0), null);
        insertCheckinSql(hv.memberId(), pkg, c.cn01, c.receptionist, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 8, 10), null);
        insertCheckinSql(hv.memberId(), pkg, c.cn01, c.receptionist, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 8, 20), null);
        Long id4 = insertCheckinSql(hv.memberId(), pkg, c.cn01, c.receptionist, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 8, 30), null);
        Long id5 = insertCheckinSql(hv.memberId(), pkg, c.cn01, c.receptionist, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 8, 40), null);

        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("page", "0").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].checkinId").value(id5))
            .andExpect(jsonPath("$.content[1].checkinId").value(id4));

        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("page", "2").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].checkinId").value(id1));

        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("page", "3").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    @DisplayName("US-F4.5.1 S3: lễ tân và quản lý chỉ thấy chi nhánh mình; MEMBER, TRAINER, SUPER_ADMIN trả 403; tham số sai trả 400")
    void historyBranchIsolationRoleAndValidation() throws Exception {
        Long pkgCn01 = c.time(hv.memberId());
        Long cn01Id = insertCheckinSql(hv.memberId(), pkgCn01, c.cn01, c.receptionist, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 8, 30), null);

        Account hvCn02 = c.b.account(c.cn02);
        Long pkgCn02 = c.membership(hvCn02.memberId(), "TIME", c.cn02, "ACTIVE", null);
        Long cn02Id = insertCheckinSql(hvCn02.memberId(), pkgCn02, c.cn02, c.receptionistCn02, "MANUAL",
            LocalDateTime.of(2026, 11, 20, 8, 45), null);

        // Lễ tân và quản lý CN01 chỉ thấy CN01
        for (RequestPostProcessor staffAuth : new RequestPostProcessor[] {
                c.reception(), c.as(c.manager, UserRole.BRANCH_MANAGER)}) {
            mvc.perform(get("/api/v1/checkins").with(staffAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].checkinId").value(cn01Id));
        }

        // Lễ tân CN02 chỉ thấy CN02
        mvc.perform(get("/api/v1/checkins").with(c.as(c.receptionistCn02, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].checkinId").value(cn02Id));

        // MEMBER, TRAINER, SUPER_ADMIN -> 403
        for (RequestPostProcessor denied : new RequestPostProcessor[] {
                c.b.member(hv), c.as(c.trainerUser, UserRole.TRAINER), c.as(c.owner, UserRole.SUPER_ADMIN)}) {
            mvc.perform(get("/api/v1/checkins").with(denied)).andExpect(status().isForbidden());
        }

        // Tham số sai -> 400
        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("date", "20-11-2026"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("status", "UNKNOWN"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("method", "QR"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("page", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("size", "0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/checkins").with(c.reception()).param("size", "101"))
            .andExpect(status().isBadRequest());
    }
}
