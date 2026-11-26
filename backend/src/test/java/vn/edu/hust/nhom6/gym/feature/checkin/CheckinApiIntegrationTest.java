package vn.edu.hust.nhom6.gym.feature.checkin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
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

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures.Account;

/**
 * Tra cứu tại quầy {@code GET /api/v1/checkins/search-member} (US-F4.1.1), check-in {@code POST /api/v1/checkins}
 * (US-F4.2.1) và ghi {@code arrived_at} (US-F4.3.1). S2-11.1. SRS F4 chưa có mã AC nên tên theo US và kịch bản.
 * Mặc định bây giờ là thứ Sáu 20/11/2026 09:00 giờ Việt Nam; CN01, CN02 mở cửa 06:00 – 22:00.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class CheckinApiIntegrationTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 9, 0);
    static final LocalDate TODAY = NOW.toLocalDate();

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;

    CheckinFixtures c;
    Account hv;

    @BeforeEach
    void setUp() {
        clock.setVn(NOW);
        c = new CheckinFixtures(jdbc, tokens);
        c.reset();
        hv = c.member();
    }

    ResultActions checkin(RequestPostProcessor auth, Long memberId) throws Exception {
        return mvc.perform(post("/api/v1/checkins").with(auth).contentType(MediaType.APPLICATION_JSON)
            .content(memberId == null ? "{}" : "{\"memberId\": " + memberId + "}"));
    }

    ResultActions checkin(Long memberId) throws Exception {
        return checkin(c.reception(), memberId);
    }

    ResultActions search(RequestPostProcessor auth, String keyword) throws Exception {
        var request = get("/api/v1/checkins/search-member").with(auth);
        if (keyword != null) {
            request.param("keyword", keyword);
        }
        return mvc.perform(request);
    }

    String code(Long memberId) {
        return jdbc.queryForObject("SELECT member_code FROM members WHERE id = ?", String.class, memberId);
    }

    String phone(Long memberId) {
        return jdbc.queryForObject("SELECT phone FROM members WHERE id = ?", String.class, memberId);
    }

    // ------------------------------------------------------------------------------------------------
    // US-F4.1.1: tra cứu hội viên tại quầy
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US-F4.1.1 KB: tìm theo mã hội viên trả hồ sơ, mọi gói ở mọi trạng thái kèm tên gói, chưa có ảnh, chưa check-in")
    void searchByCode() throws Exception {
        c.time(hv.memberId());
        c.membership(hv.memberId(), "SESSION", c.cn01, "EXPIRED", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 0);
        search(c.reception(), code(hv.memberId())).andExpect(status().isOk())
            .andExpect(jsonPath("$.memberId").value(hv.memberId()))
            .andExpect(jsonPath("$.memberCode").value(code(hv.memberId())))
            .andExpect(jsonPath("$.fullName").value("Hội viên đặt lớp"))
            .andExpect(jsonPath("$.phone").value(phone(hv.memberId())))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.hasAvatar").value(false))
            .andExpect(jsonPath("$.isCurrentlyCheckedIn").value(false))
            .andExpect(jsonPath("$.memberships", hasSize(2)))
            .andExpect(jsonPath("$.memberships[?(@.packageType == 'TIME')].status").value("ACTIVE"))
            .andExpect(jsonPath("$.memberships[?(@.packageType == 'SESSION')].status").value("EXPIRED"))
            .andExpect(jsonPath("$.memberships[0].packageName").value(containsString("Gói")))
            .andExpect(jsonPath("$.upcomingClassSession").doesNotExist());
    }

    @Test
    @DisplayName("US-F4.1.1 KB: tìm theo số điện thoại có khoảng trắng và mã viết thường đều được chuẩn hóa")
    void searchNormalizesKeyword() throws Exception {
        String p = phone(hv.memberId());
        String spaced = " " + p.substring(0, 4) + " " + p.substring(4, 7) + " " + p.substring(7) + " ";
        search(c.reception(), spaced).andExpect(status().isOk())
            .andExpect(jsonPath("$.memberId").value(hv.memberId()));
        search(c.reception(), code(hv.memberId()).toLowerCase()).andExpect(status().isOk())
            .andExpect(jsonPath("$.memberId").value(hv.memberId()));
        search(c.reception(), "+84" + p.substring(1)).andExpect(status().isOk())
            .andExpect(jsonPath("$.memberId").value(hv.memberId()));
    }

    @Test
    @DisplayName("US-F4.1.1 KB: có ảnh đại diện thì hasAvatar = true; đã check-in chưa ra thì isCurrentlyCheckedIn = true; có lớp sắp tới thì kèm tên lớp, phòng")
    void searchShowsAvatarPresenceAndUpcomingClass() throws Exception {
        c.time(hv.memberId());
        jdbc.update("INSERT INTO member_avatars (member_id, content, size_bytes, uploaded_by) VALUES (?, ?, 3, ?)",
            hv.memberId(), new byte[] {1, 2, 3}, c.receptionist);
        Long session = c.b.session(c.cn01, NOW.plusMinutes(90), 20);
        Long booking = c.b.booking(session, hv.memberId(), "BOOKED");
        checkin(hv.memberId()).andExpect(status().isCreated());

        search(c.reception(), code(hv.memberId())).andExpect(status().isOk())
            .andExpect(jsonPath("$.hasAvatar").value(true))
            .andExpect(jsonPath("$.isCurrentlyCheckedIn").value(true))
            .andExpect(jsonPath("$.upcomingClassSession.bookingId").value(booking))
            .andExpect(jsonPath("$.upcomingClassSession.className").value("Yoga"))
            .andExpect(jsonPath("$.upcomingClassSession.roomName").value(containsString("Phòng")))
            .andExpect(jsonPath("$.upcomingClassSession.startTime").value("2026-11-20T03:30:00Z"));
    }

    @Test
    @DisplayName("US-F4.1.1 E1: từ khóa rỗng, thiếu hoặc sai định dạng trả 400 với câu hướng dẫn")
    void searchRejectsBadKeyword() throws Exception {
        for (String bad : new String[] {"abc", "HV12345", "091234567", "1912345678", "   "}) {
            search(c.reception(), bad).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("mã hội viên hợp lệ")));
        }
        search(c.reception(), null).andExpect(status().isBadRequest());
        search(c.reception(), "HV".repeat(11)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("US-F4.1.1 E2: không có hội viên, hoặc hội viên chi nhánh khác, trả 404 cùng một câu")
    void searchNotFoundOrOtherBranch() throws Exception {
        search(c.reception(), "HV000999").andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(containsString("Không tìm thấy thông tin hội viên")));
        Account other = c.b.account(c.cn02);
        search(c.reception(), code(other.memberId())).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(containsString("Không tìm thấy thông tin hội viên")));
        search(c.as(c.receptionistCn02, UserRole.RECEPTIONIST), code(other.memberId())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("US-F4.1.1 E3: hồ sơ ngừng hoạt động vẫn trả 200 kèm status INACTIVE")
    void searchInactiveMember() throws Exception {
        jdbc.update("UPDATE members SET status = 'INACTIVE' WHERE id = ?", hv.memberId());
        search(c.reception(), code(hv.memberId())).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("US-F4.1.1 S3: chủ chuỗi, huấn luyện viên, hội viên không tra cứu và không check-in được (403); quản lý chi nhánh được")
    void onlyCounterStaff() throws Exception {
        c.time(hv.memberId());
        for (RequestPostProcessor denied : new RequestPostProcessor[] {c.as(c.owner, UserRole.SUPER_ADMIN),
                c.as(c.trainerUser, UserRole.TRAINER), c.b.member(hv)}) {
            search(denied, code(hv.memberId())).andExpect(status().isForbidden());
            checkin(denied, hv.memberId()).andExpect(status().isForbidden());
        }
        assertThat(c.checkins(hv.memberId())).isZero();
        search(c.as(c.manager, UserRole.BRANCH_MANAGER), code(hv.memberId())).andExpect(status().isOk());
        checkin(c.as(c.manager, UserRole.BRANCH_MANAGER), hv.memberId()).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT created_by FROM checkins WHERE member_id = ?", Long.class,
            hv.memberId())).isEqualTo(c.manager);
    }

    // ------------------------------------------------------------------------------------------------
    // US-F4.2.1: check-in, chọn gói, trừ buổi
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US-F4.2.1 KB1: gói TIME ACTIVE thì 201, ghi checkins MANUAL ngày Việt Nam, chi nhánh và người tạo của lễ tân, không trừ buổi")
    void kb1Time() throws Exception {
        Long time = c.time(hv.memberId());
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.checkinId").isNumber())
            .andExpect(jsonPath("$.memberId").value(hv.memberId()))
            .andExpect(jsonPath("$.memberCode").value(code(hv.memberId())))
            .andExpect(jsonPath("$.appliedMembership.id").value(time))
            .andExpect(jsonPath("$.appliedMembership.packageType").value("TIME"))
            .andExpect(jsonPath("$.appliedMembership.sessionDeducted").value(false))
            .andExpect(jsonPath("$.appliedMembership.remainingSessions").doesNotExist())
            .andExpect(jsonPath("$.checkinDate").value("2026-11-20"))
            .andExpect(jsonPath("$.checkedInAt").value("2026-11-20T02:00:00Z"))
            .andExpect(jsonPath("$.classArrivalMarked").value(false))
            .andExpect(jsonPath("$.message").value(containsString("Check-in thành công")));
        var row = jdbc.queryForMap("SELECT * FROM checkins WHERE member_id = ?", hv.memberId());
        assertThat(row.get("membership_id")).isEqualTo(time);
        assertThat(row.get("branch_id")).isEqualTo(c.cn01);
        assertThat(row.get("created_by")).isEqualTo(c.receptionist);
        assertThat(row.get("method")).isEqualTo("MANUAL");
        assertThat(row.get("session_deducted")).isEqualTo(false);
        assertThat(row.get("checkin_date").toString()).isEqualTo("2026-11-20");
        assertThat(row.get("checked_out_at")).isNull();
    }

    @Test
    @DisplayName("US-F4.2.1 BR-F4.3: 00:30 giờ Việt Nam (17:30 UTC hôm trước) ghi checkin_date là ngày Việt Nam")
    void checkinDateIsVietnamDate() throws Exception {
        jdbc.update("UPDATE branches SET open_time = '00:00' WHERE id = ?", c.cn01);
        clock.setVn(LocalDateTime.of(2026, 11, 21, 0, 30));
        c.time(hv.memberId());
        checkin(hv.memberId()).andExpect(status().isCreated()).andExpect(jsonPath("$.checkinDate").value("2026-11-21"));
    }

    @Test
    @DisplayName("US-F4.2.1 KB2: gói SESSION 10 buổi, lần đầu trong ngày thì trừ còn 9, session_deducted = true")
    void kb2SessionFirst() throws Exception {
        Long s = c.session(hv.memberId(), 10);
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.appliedMembership.id").value(s))
            .andExpect(jsonPath("$.appliedMembership.packageType").value("SESSION"))
            .andExpect(jsonPath("$.appliedMembership.sessionDeducted").value(true))
            .andExpect(jsonPath("$.appliedMembership.remainingSessions").value(9));
        assertThat(c.remaining(s)).isEqualTo(9);
        assertThat(c.deductions(s)).isEqualTo(1);
    }

    @Test
    @DisplayName("US-F4.2.1 KB3: lần thứ hai cùng ngày không trừ thêm, vẫn 9 buổi; sang ngày mới thì trừ tiếp")
    void kb3SessionSecond() throws Exception {
        Long s = c.session(hv.memberId(), 10);
        checkin(hv.memberId()).andExpect(status().isCreated());
        clock.setVn(NOW.withHour(15));
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.appliedMembership.sessionDeducted").value(false))
            .andExpect(jsonPath("$.appliedMembership.remainingSessions").value(9));
        assertThat(c.remaining(s)).isEqualTo(9);
        assertThat(c.checkins(hv.memberId())).isEqualTo(2);
        clock.setVn(NOW.plusDays(1));
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.appliedMembership.sessionDeducted").value(true));
        assertThat(c.remaining(s)).isEqualTo(8);
    }

    @Test
    @DisplayName("US-F4.2.1 KB4: chỉ có gói PT, hôm nay không có buổi PT thì 422 E3, không ghi checkins")
    void kb4PtOnlyNoSession() throws Exception {
        Long pt = c.pt(hv.memberId());
        checkin(hv.memberId()).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(containsString("không dùng để check-in tự do")));
        assertThat(c.checkins(hv.memberId())).isZero();
        assertThat(c.remaining(pt)).isEqualTo(10);
    }

    @Test
    @DisplayName("US-F4.2.1 KB4b: chỉ có gói PT, hôm nay 18:00 có buổi PT BOOKED tại CN01, check-in 17:45 thì 201 gắn gói PT, không trừ buổi")
    void kb4bPtWithSessionToday() throws Exception {
        Long pt = c.pt(hv.memberId());
        c.ptSession(hv.memberId(), pt, c.cn01, TODAY.atTime(18, 0), "BOOKED");
        clock.setVn(TODAY.atTime(17, 45));
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.appliedMembership.id").value(pt))
            .andExpect(jsonPath("$.appliedMembership.packageType").value("PT"))
            .andExpect(jsonPath("$.appliedMembership.sessionDeducted").value(false))
            .andExpect(jsonPath("$.appliedMembership.remainingSessions").value(10));
        assertThat(c.remaining(pt)).isEqualTo(10);
        assertThat(c.deductions(pt)).isZero();
    }

    @Test
    @DisplayName("US-F4.2.1 BR-F4.8: buổi PT ở chi nhánh khác, hôm khác, hoặc đã CANCELLED, DONE thì vẫn 422 E3")
    void br48PtSessionMustBeBookedTodayHere() throws Exception {
        Long pt = c.membership(hv.memberId(), "PT", null, "ACTIVE", 10);
        c.ptSession(hv.memberId(), pt, c.cn02, TODAY.atTime(18, 0), "BOOKED");
        c.ptSession(hv.memberId(), pt, c.cn01, TODAY.plusDays(1).atTime(7, 0), "BOOKED");
        c.ptSession(hv.memberId(), pt, c.cn01, TODAY.minusDays(1).atTime(18, 0), "BOOKED");
        c.ptSession(hv.memberId(), pt, c.cn01, TODAY.atTime(10, 0), "CANCELLED");
        c.ptSession(hv.memberId(), pt, c.cn01, TODAY.atTime(6, 0), "DONE");
        checkin(hv.memberId()).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(containsString("không dùng để check-in tự do")));
        assertThat(c.checkins(hv.memberId())).isZero();
    }

    @Test
    @DisplayName("US-F4.2.1 BR-F4.8: buổi PT 00:30 hôm nay (giờ Việt Nam) vẫn tính là hôm nay; buổi 23:30 hôm qua thì không")
    void br48UsesVietnamDay() throws Exception {
        Long pt = c.pt(hv.memberId());
        c.ptSession(hv.memberId(), pt, c.cn01, TODAY.minusDays(1).atTime(23, 30), "BOOKED");
        checkin(hv.memberId()).andExpect(status().isUnprocessableContent());
        c.ptSession(hv.memberId(), pt, c.cn01, TODAY.atTime(0, 30), "BOOKED");
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.appliedMembership.packageType").value("PT"));
    }

    @Test
    @DisplayName("US-F4.2.1 BR-F4.8: có cả gói TIME và gói PT có buổi hôm nay thì dùng gói TIME")
    void br48TimeBeatsPt() throws Exception {
        Long time = c.time(hv.memberId());
        Long pt = c.pt(hv.memberId());
        c.ptSession(hv.memberId(), pt, c.cn01, TODAY.atTime(18, 0), "BOOKED");
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.appliedMembership.id").value(time));
    }

    @Test
    @DisplayName("US-F4.2.1 KB5: có cả gói TIME và SESSION thì dùng gói TIME, gói SESSION giữ nguyên số buổi")
    void kb5TimeBeatsSession() throws Exception {
        Long time = c.time(hv.memberId());
        Long s = c.session(hv.memberId(), 30);
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.appliedMembership.id").value(time))
            .andExpect(jsonPath("$.appliedMembership.sessionDeducted").value(false));
        assertThat(c.remaining(s)).isEqualTo(30);
    }

    @Test
    @DisplayName("US-F4.2.1 KB6: gói SESSION còn 1 buổi, 08:00 trừ về 0 vẫn ACTIVE; 17:00 quay lại 201 không trừ, không bị E4")
    void kb6LastSessionThenReturn() throws Exception {
        Long s = c.session(hv.memberId(), 1);
        clock.setVn(TODAY.atTime(8, 0));
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.appliedMembership.sessionDeducted").value(true))
            .andExpect(jsonPath("$.appliedMembership.remainingSessions").value(0));
        assertThat(c.status(s)).isEqualTo("ACTIVE");
        clock.setVn(TODAY.atTime(17, 0));
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.appliedMembership.id").value(s))
            .andExpect(jsonPath("$.appliedMembership.sessionDeducted").value(false))
            .andExpect(jsonPath("$.appliedMembership.remainingSessions").value(0));
        assertThat(c.remaining(s)).isZero();
        assertThat(c.status(s)).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("US-F4.2.1 KB8: 03/11 trừ buổi cuối gói A thì gói SESSION B UPCOMING 01/12 – 29/01 dời về 04/11 – 02/01, vẫn 60 ngày; gói khác loại không đổi")
    void kb8PullUpcoming() throws Exception {
        clock.setVn(LocalDateTime.of(2026, 11, 3, 8, 0));
        Long a = c.membership(hv.memberId(), "SESSION", c.cn01, "ACTIVE", LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 11, 30), 1);
        Long bPkg = c.membership(hv.memberId(), "SESSION", c.cn01, "UPCOMING", LocalDate.of(2026, 12, 1),
            LocalDate.of(2027, 1, 29), 10);
        Long otherScope = c.membership(hv.memberId(), "SESSION", null, "UPCOMING", LocalDate.of(2026, 12, 5),
            LocalDate.of(2027, 1, 5), 10);
        Long time = c.membership(hv.memberId(), "TIME", c.cn01, "UPCOMING", LocalDate.of(2026, 12, 1),
            LocalDate.of(2026, 12, 30), null);
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.appliedMembership.id").value(a))
            .andExpect(jsonPath("$.appliedMembership.remainingSessions").value(0));
        assertThat(c.status(a)).isEqualTo("ACTIVE");
        assertThat(c.startDate(bPkg)).isEqualTo(LocalDate.of(2026, 11, 4));
        assertThat(c.endDate(bPkg)).isEqualTo(LocalDate.of(2027, 1, 2));
        assertThat(c.status(bPkg)).isEqualTo("UPCOMING");
        assertThat(c.startDate(otherScope)).as("gói toàn chuỗi khác phạm vi").isEqualTo(LocalDate.of(2026, 12, 5));
        assertThat(c.startDate(time)).as("gói TIME khác loại").isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    @DisplayName("US-F4.2.1 BR-F4.7: trừ buổi mà chưa về 0 thì không dời gói UPCOMING; gói UPCOMING đã bắt đầu từ ngày mai thì giữ nguyên")
    void br47NoPullWhenNotZeroOrAlreadyTomorrow() throws Exception {
        Long a = c.session(hv.memberId(), 2);
        Long next = c.membership(hv.memberId(), "SESSION", c.cn01, "UPCOMING", LocalDate.of(2027, 1, 1),
            LocalDate.of(2027, 1, 30), 10);
        checkin(hv.memberId()).andExpect(status().isCreated());
        assertThat(c.remaining(a)).isEqualTo(1);
        assertThat(c.startDate(next)).isEqualTo(LocalDate.of(2027, 1, 1));

        Account other = c.member();
        c.session(other.memberId(), 1);
        Long tomorrow = c.membership(other.memberId(), "SESSION", c.cn01, "UPCOMING", TODAY.plusDays(1),
            TODAY.plusDays(30), 10);
        checkin(other.memberId()).andExpect(status().isCreated());
        assertThat(c.startDate(tomorrow)).isEqualTo(TODAY.plusDays(1));
        assertThat(c.endDate(tomorrow)).isEqualTo(TODAY.plusDays(30));
    }

    @Test
    @DisplayName("US-F4.2.1 BR-F4.1: gói SESSION toàn chuỗi (branch_id rỗng) dùng được ở CN01; gói chỉ của CN02 thì không (E2)")
    void branchScope() throws Exception {
        Long chain = c.membership(hv.memberId(), "SESSION", null, "ACTIVE", 5);
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.appliedMembership.id").value(chain));

        Account other = c.member();
        c.membership(other.memberId(), "TIME", c.cn02, "ACTIVE", null);
        checkin(other.memberId()).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(containsString("không có gói tập nào đang hoạt động")));
    }

    @Test
    @DisplayName("US-F4.2.1 GĐ-3: có hai gói SESSION dùng được thì lần sau trong ngày dùng lại gói đã trừ, không trừ gói thứ hai")
    void twoSessionPackagesDeductOncePerDay() throws Exception {
        Long branchPkg = c.membership(hv.memberId(), "SESSION", c.cn01, "ACTIVE", LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 12, 10), 5);
        Long chainPkg = c.membership(hv.memberId(), "SESSION", null, "ACTIVE", 5);
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.appliedMembership.id").value(branchPkg))
            .andExpect(jsonPath("$.appliedMembership.sessionDeducted").value(true));
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.appliedMembership.id").value(branchPkg))
            .andExpect(jsonPath("$.appliedMembership.sessionDeducted").value(false));
        assertThat(c.remaining(branchPkg)).isEqualTo(4);
        assertThat(c.remaining(chainPkg)).isEqualTo(5);
    }

    @Test
    @DisplayName("US-F4.2.1 E1 (VD-13): giờ mở cửa ghi bằng SQL '06:00' – '22:00'; 05:59 và 22:00 bị 422 kèm giờ đúng, 06:00 và 21:59 vào được")
    void e1OpeningHoursBoundaries() throws Exception {
        c.time(hv.memberId());
        clock.setVn(TODAY.atTime(5, 59, 59));
        checkin(hv.memberId()).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(
                "Phòng tập hiện đang đóng cửa (Giờ mở cửa: 06:00 - 22:00). Không thể thực hiện check-in."));
        clock.setVn(TODAY.atTime(6, 0));
        checkin(hv.memberId()).andExpect(status().isCreated());
        clock.setVn(TODAY.atTime(21, 59, 59));
        checkin(hv.memberId()).andExpect(status().isCreated());
        clock.setVn(TODAY.atTime(22, 0));
        checkin(hv.memberId()).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(containsString("06:00 - 22:00")));
        assertThat(c.checkins(hv.memberId())).isEqualTo(2);
    }

    @Test
    @DisplayName("US-F4.2.1 E1 (VD-13): giờ lẻ '18:30' – '20:45' đọc ra đúng, không lệch múi giờ")
    void e1OddOpeningHours() throws Exception {
        jdbc.update("UPDATE branches SET open_time = '18:30', close_time = '20:45' WHERE id = ?", c.cn01);
        c.time(hv.memberId());
        clock.setVn(TODAY.atTime(18, 29));
        checkin(hv.memberId()).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(containsString("18:30 - 20:45")));
        clock.setVn(TODAY.atTime(18, 30));
        checkin(hv.memberId()).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("US-F4.2.1 E2: chưa có gói, hoặc chỉ có gói EXPIRED, CANCELLED, PENDING, UPCOMING, hết hạn theo ngày thì 422")
    void e2NoActivePackage() throws Exception {
        checkin(hv.memberId()).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(
                "Hội viên không có gói tập nào đang hoạt động. Vui lòng đăng ký hoặc gia hạn gói tập tại quầy."));
        c.membership(hv.memberId(), "TIME", c.cn01, "EXPIRED", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null);
        c.membership(hv.memberId(), "SESSION", c.cn01, "PENDING", 10);
        c.membership(hv.memberId(), "TIME", c.cn01, "UPCOMING", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31), null);
        c.membership(hv.memberId(), "TIME", null, "ACTIVE", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 19), null);
        checkin(hv.memberId()).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(containsString("không có gói tập nào đang hoạt động")));
        assertThat(c.checkins(hv.memberId())).isZero();
    }

    @Test
    @DisplayName("US-F4.2.1 E4: gói SESSION còn 0 buổi và hôm nay chưa trừ buổi thì 422, không ghi checkins")
    void e4NoSessionsLeft() throws Exception {
        Long s = c.session(hv.memberId(), 0);
        checkin(hv.memberId()).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(
                "Gói tập theo buổi của hội viên đã hết lượt sử dụng. Vui lòng gia hạn thêm gói mới."));
        assertThat(c.checkins(hv.memberId())).isZero();
        assertThat(c.remaining(s)).isZero();
    }

    @Test
    @DisplayName("US-F4.2.1 GĐ-4: gói SESSION còn 0 buổi (chưa trừ hôm nay) và hôm nay có buổi PT thì vào bằng gói PT, không trừ buổi")
    void zeroSessionFallsBackToPtSession() throws Exception {
        Long s = c.session(hv.memberId(), 0);
        Long pt = c.pt(hv.memberId());
        c.ptSession(hv.memberId(), pt, c.cn01, TODAY.atTime(10, 0), "BOOKED");
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.appliedMembership.id").value(pt))
            .andExpect(jsonPath("$.appliedMembership.sessionDeducted").value(false));
        assertThat(c.remaining(s)).isZero();
        assertThat(c.remaining(pt)).isEqualTo(10);
    }

    @Test
    @DisplayName("US-F4.2.1 BR-F4.8: buổi PT hôm nay đặt bằng gói PT đã hết hạn thì không gắn sang gói PT khác đang dùng được (422 E3)")
    void br48OnlyTheSessionsOwnPtPackage() throws Exception {
        Long oldPt = c.membership(hv.memberId(), "PT", c.cn01, "EXPIRED", LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 10, 31), 3);
        c.pt(hv.memberId());
        c.ptSession(hv.memberId(), oldPt, c.cn01, TODAY.atTime(10, 0), "BOOKED");
        checkin(hv.memberId()).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(containsString("không dùng để check-in tự do")));
    }

    @Test
    @DisplayName("US-F4.2.1 GĐ-4: có gói SESSION 0 buổi và gói TIME đang bảo lưu thì báo bảo lưu (E5) trước hết buổi (E4)")
    void frozenBeatsUsedUp() throws Exception {
        c.session(hv.memberId(), 0);
        c.membership(hv.memberId(), "TIME", c.cn01, "FROZEN", null);
        checkin(hv.memberId()).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(containsString("đang trong thời gian bảo lưu. Vui lòng")));
    }

    @Test
    @DisplayName("US-F4.2.1 E5: gói duy nhất đang FROZEN thì 422 kèm khoảng bảo lưu")
    void e5Frozen() throws Exception {
        Long frozen = c.membership(hv.memberId(), "TIME", c.cn01, "FROZEN", null);
        jdbc.update("""
            INSERT INTO membership_freezes (membership_id, requested_by, approved_by, from_date, to_date, status)
            VALUES (?, ?, ?, '2026-11-15', '2026-12-14', 'APPROVED')""", frozen, c.manager, c.manager);
        checkin(hv.memberId()).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Gói tập của hội viên đang trong thời gian bảo lưu (từ ngày 15/11/2026 "
                + "đến ngày 14/12/2026). Vui lòng liên hệ quản lý để hủy bảo lưu sớm nếu muốn tập tiếp."));
    }

    @Test
    @DisplayName("US-F4.2.1 E7: hồ sơ INACTIVE dù còn gói ACTIVE thì 422; hội viên chi nhánh khác hoặc không có thì 404")
    void e7InactiveAndScope() throws Exception {
        c.time(hv.memberId());
        jdbc.update("UPDATE members SET status = 'INACTIVE' WHERE id = ?", hv.memberId());
        checkin(hv.memberId()).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(
                "Hồ sơ hội viên này đã ngừng hoạt động. Vui lòng liên hệ quản lý để được hỗ trợ."));

        Account other = c.b.account(c.cn02);
        c.membership(other.memberId(), "TIME", null, "ACTIVE", null);
        checkin(other.memberId()).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(containsString("Không tìm thấy thông tin hội viên")));
        checkin(999_999L).andExpect(status().isNotFound());
        assertThat(c.checkins(other.memberId())).isZero();
    }

    @Test
    @DisplayName("US-F4.2.1 S5: thiếu memberId hoặc memberId không dương thì 400")
    void validation() throws Exception {
        checkin(null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.memberId").exists());
        checkin(0L).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.memberId").exists());
    }

    // ------------------------------------------------------------------------------------------------
    // US-F4.3.1: ghi arrived_at khi check-in
    // ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US-F4.3.1: lớp bắt đầu đúng now + 60 phút và đúng now − 15 phút đều được ghi arrived_at = now, status vẫn BOOKED")
    void arrivedAtWindowEdges() throws Exception {
        c.time(hv.memberId());
        Long late = c.b.booking(c.b.session(c.cn01, NOW.minusMinutes(15), 20), hv.memberId(), "BOOKED");
        Long early = c.b.booking(c.b.session(c.cn01, NOW.plusMinutes(60), 20), hv.memberId(), "BOOKED");
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.classArrivalMarked").value(true))
            .andExpect(jsonPath("$.classArrivals", hasSize(2)))
            .andExpect(jsonPath("$.classArrivals[0].bookingId").value(late))
            .andExpect(jsonPath("$.classArrivals[0].className").value("Yoga"))
            .andExpect(jsonPath("$.classArrivals[1].bookingId").value(early));
        OffsetDateTime now = BookingFixtures.utc(NOW);
        assertThat(c.arrivedAt(late)).isEqualTo(now);
        assertThat(c.arrivedAt(early)).isEqualTo(now);
        assertThat(c.b.status(late)).isEqualTo("BOOKED");
        assertThat(c.b.status(early)).isEqualTo("BOOKED");
    }

    @Test
    @DisplayName("US-F4.3.1: lớp bắt đầu now − 16 phút hoặc now + 61 phút, lớp ở chi nhánh khác, chỗ đã hủy thì không ghi")
    void arrivedAtOutsideWindow() throws Exception {
        c.membership(hv.memberId(), "TIME", null, "ACTIVE", null);
        Long tooLate = c.b.booking(c.b.session(c.cn01, NOW.minusMinutes(16), 20), hv.memberId(), "BOOKED");
        Long tooEarly = c.b.booking(c.b.session(c.cn01, NOW.plusMinutes(61), 20), hv.memberId(), "BOOKED");
        Long otherBranch = c.b.booking(c.b.session(c.cn02, NOW.plusMinutes(30), 20), hv.memberId(), "BOOKED");
        Long cancelled = c.b.booking(c.b.session(c.cn01, NOW.plusMinutes(30), 20), hv.memberId(), "CANCELLED");
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.classArrivalMarked").value(false))
            .andExpect(jsonPath("$.classArrivals", hasSize(0)));
        for (Long id : new Long[] {tooLate, tooEarly, otherBranch, cancelled}) {
            assertThat(c.arrivedAt(id)).as("chỗ %d", id).isNull();
        }
    }

    @Test
    @DisplayName("US-F4.3.1: chỗ đã NO_SHOW hoặc ATTENDED trong khung giữ nguyên status, arrived_at rỗng và updated_at (mốc đếm vi phạm F8)")
    void arrivedAtNeverTouchesMarkedBookings() throws Exception {
        c.time(hv.memberId());
        Long noShowSession = c.b.session(c.cn01, NOW.minusMinutes(10), 20);
        Long noShow = c.b.noShow(noShowSession, hv.memberId(), NOW.minusDays(1));
        Long attendedSession = c.b.session(c.cn01, NOW.plusMinutes(20), 20);
        Long attended = c.b.booking(attendedSession, hv.memberId(), "ATTENDED");
        OffsetDateTime noShowUpdated = c.bookingUpdatedAt(noShow);
        OffsetDateTime attendedUpdated = c.bookingUpdatedAt(attended);
        checkin(hv.memberId()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.classArrivalMarked").value(false));
        assertThat(c.b.status(noShow)).isEqualTo("NO_SHOW");
        assertThat(c.b.status(attended)).isEqualTo("ATTENDED");
        assertThat(c.arrivedAt(noShow)).isNull();
        assertThat(c.arrivedAt(attended)).isNull();
        assertThat(c.bookingUpdatedAt(noShow)).isEqualTo(noShowUpdated);
        assertThat(c.bookingUpdatedAt(attended)).isEqualTo(attendedUpdated);
    }

    @Test
    @DisplayName("US-F4.3.1: check-in lần hai không ghi đè arrived_at đã có; check-in bị từ chối thì không ghi arrived_at")
    void arrivedAtKeepsFirstAndNeedsSuccess() throws Exception {
        Long booking = c.b.booking(c.b.session(c.cn01, NOW.plusMinutes(45), 20), hv.memberId(), "BOOKED");
        checkin(hv.memberId()).andExpect(status().isUnprocessableContent());
        assertThat(c.arrivedAt(booking)).isNull();

        c.time(hv.memberId());
        checkin(hv.memberId()).andExpect(status().isCreated()).andExpect(jsonPath("$.classArrivalMarked").value(true));
        clock.setVn(NOW.plusMinutes(10));
        checkin(hv.memberId()).andExpect(status().isCreated()).andExpect(jsonPath("$.classArrivalMarked").value(false));
        assertThat(c.arrivedAt(booking)).isEqualTo(BookingFixtures.utc(NOW));
    }
}
