package vn.edu.hust.nhom6.gym.feature.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.vn;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.Account;
import vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.Trainer;

/**
 * Kiểm thử tích hợp API đặt buổi PT {@code POST /api/v1/pt-sessions} (F9, UC-F9.2, S2-19.2, #193). SRS F9 chưa có mã AC
 * nên tên hiển thị theo US và luồng của SRS.
 *
 * <p>Mốc mặc định: thứ Sáu 20/11/2026 06:00 giờ Việt Nam. Huấn luyện viên rảnh 08:00–20:00 mọi ngày. Buổi mặc định:
 * thứ Bảy 21/11 10:00–11:00 giờ Việt Nam (03:00Z).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class PtSessionBookingApiIntegrationTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 6, 0);
    static final LocalDateTime SAT_10 = LocalDateTime.of(2026, 11, 21, 10, 0);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired JsonMapper json;

    PtSessionFixtures f;
    Trainer pt;

    @BeforeEach
    void setUp() {
        clock.setVn(NOW);
        f = new PtSessionFixtures(jdbc, tokens);
        f.reset();
        pt = f.trainer(f.cn01);
    }

    ResultActions book(Account member, Long trainerId, LocalDateTime vnStart) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trainerId", trainerId);
        body.put("startAt", vn(vnStart).toString());
        return send(member.userId(), UserRole.MEMBER, body);
    }

    ResultActions send(Long userId, UserRole role, Map<String, Object> body) throws Exception {
        return mvc.perform(post("/api/v1/pt-sessions")
            .with(f.bearer(userId, role))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body)));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Thành công
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("TC-F9.2.1-01a: gói PT ACTIVE, trong giờ rảnh, còn ≥ 12 giờ, không trùng → 201 BOOKED 60 phút, không trừ buổi")
    void bookSuccess() throws Exception {
        Account a = f.ptMember(10);
        Long membershipId = f.ptMembershipOf(a.memberId());

        book(a, pt.staffId(), SAT_10)
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/api/v1/pt-sessions/")))
            .andExpect(jsonPath("$.trainerId").value(pt.staffId()))
            .andExpect(jsonPath("$.memberId").value(a.memberId()))
            .andExpect(jsonPath("$.membershipId").value(membershipId))
            .andExpect(jsonPath("$.branchId").value(f.cn01))
            .andExpect(jsonPath("$.status").value("BOOKED"))
            .andExpect(jsonPath("$.startAt").value("2026-11-21T03:00:00Z"))
            .andExpect(jsonPath("$.endAt").value("2026-11-21T04:00:00Z"))
            .andExpect(jsonPath("$.cancelledAt").doesNotExist());

        assertThat(f.remaining(membershipId)).as("BR-F9.5: đặt buổi không trừ buổi").isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT created_by FROM pt_sessions", Long.class)).isEqualTo(a.userId());
        assertThat(jdbc.queryForObject("SELECT end_at - start_at = interval '60 minutes' FROM pt_sessions",
            Boolean.class)).isTrue();
    }

    @Test
    @DisplayName("TC-F9.2.1-01b: chấp nhận tên trường của API nháp SRS (ptId, startTime) → 201")
    void acceptsSrsDraftFieldNames() throws Exception {
        Account a = f.ptMember(10);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ptId", pt.staffId());
        body.put("startTime", "2026-11-21T03:00:00Z");
        send(a.userId(), UserRole.MEMBER, body).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-F9.2.1-01c: gói PT toàn chuỗi (branch_id rỗng) dùng được ở chi nhánh của PT → 201")
    void chainWidePtPackageIsUsable() throws Exception {
        Account a = f.account(f.cn02, "ACTIVE");
        f.membership(a.memberId(), "PT", null, "ACTIVE", PtSessionFixtures.PKG_START, PtSessionFixtures.PKG_END, 5);
        book(a, pt.staffId(), SAT_10).andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // E5 (422): gói PT
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("TC-F9.2.1-07a: không có gói PT ACTIVE dùng được (chỉ TIME/SESSION, PT chi nhánh khác, PT hết hạn trước ngày tập, PT UPCOMING) → 422")
    void noUsablePtPackage() throws Exception {
        Account onlyTimeAndSession = f.account(f.cn01, "ACTIVE");
        f.membership(onlyTimeAndSession.memberId(), "TIME", f.cn01, "ACTIVE", PtSessionFixtures.PKG_START,
            PtSessionFixtures.PKG_END, null);
        f.membership(onlyTimeAndSession.memberId(), "SESSION", f.cn01, "ACTIVE", PtSessionFixtures.PKG_START,
            PtSessionFixtures.PKG_END, 10);
        Account otherBranch = f.account(f.cn01, "ACTIVE");
        f.membership(otherBranch.memberId(), "PT", f.cn02, "ACTIVE", PtSessionFixtures.PKG_START,
            PtSessionFixtures.PKG_END, 10);
        Account endsBefore = f.account(f.cn01, "ACTIVE");
        f.membership(endsBefore.memberId(), "PT", f.cn01, "ACTIVE", PtSessionFixtures.PKG_START,
            LocalDate.of(2026, 11, 20), 10);
        Account upcoming = f.account(f.cn01, "ACTIVE");
        f.membership(upcoming.memberId(), "PT", f.cn01, "UPCOMING", LocalDate.of(2026, 12, 1),
            LocalDate.of(2026, 12, 31), 10);

        for (Account a : new Account[] {onlyTimeAndSession, otherBranch, endsBefore, upcoming}) {
            book(a, pt.staffId(), SAT_10)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail", containsString("gói PT")));
        }
        assertThat(f.count("pt_sessions")).isZero();
    }

    @Test
    @DisplayName("TC-F9.2.1-07b: hồ sơ hội viên INACTIVE → 422")
    void inactiveMember() throws Exception {
        Account a = f.account(f.cn01, "INACTIVE");
        f.membership(a.memberId(), "PT", f.cn01, "ACTIVE", PtSessionFixtures.PKG_START, PtSessionFixtures.PKG_END, 10);
        book(a, pt.staffId(), SAT_10).andExpect(status().isUnprocessableContent());
        assertThat(f.count("pt_sessions")).isZero();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // E5 (422): giờ rảnh và thời gian đặt trước
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("TC-F9.2.1-02b: ngoài giờ rảnh (19:30 vượt 20:00, 07:30 trước 08:00, PT không khai báo khung) → 422; 19:00 chạm biên → 201")
    void outsideAvailability() throws Exception {
        Account a = f.ptMember(10);
        book(a, pt.staffId(), LocalDateTime.of(2026, 11, 21, 19, 30))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail", containsString("giờ rảnh")));
        book(a, pt.staffId(), LocalDateTime.of(2026, 11, 21, 7, 30)).andExpect(status().isUnprocessableContent());
        Trainer noSlot = f.trainerWithoutAvailability(f.cn01);
        book(a, noSlot.staffId(), SAT_10).andExpect(status().isUnprocessableContent());

        book(a, pt.staffId(), LocalDateTime.of(2026, 11, 21, 19, 0)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-F9.2.1-03a: biên PT_MIN_LEAD_HOURS – còn đúng 12 giờ → 201")
    void exactlyTwelveHoursAhead() throws Exception {
        Account a = f.ptMember(10);
        book(a, pt.staffId(), LocalDateTime.of(2026, 11, 20, 18, 0)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-F9.2.1-03b: còn 11 giờ 59 phút → 422 đặt sát giờ")
    void elevenHoursFiftyNineMinutesAhead() throws Exception {
        clock.setVn(NOW.plusMinutes(1));
        Account a = f.ptMember(10);
        book(a, pt.staffId(), LocalDateTime.of(2026, 11, 20, 18, 0))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail", containsString("12 giờ")));
        assertThat(f.count("pt_sessions")).isZero();
    }

    @Test
    @DisplayName("TC-F9.2.1-04a: PT_MIN_LEAD_HOURS đọc từ system_configs, không viết cứng (đổi thành 24 thì 12 giờ bị 422)")
    void leadHoursReadFromConfig() throws Exception {
        f.chainConfig("PT_MIN_LEAD_HOURS", 24);
        Account a = f.ptMember(10);
        book(a, pt.staffId(), LocalDateTime.of(2026, 11, 20, 18, 0))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail", containsString("24 giờ")));
        book(a, pt.staffId(), LocalDateTime.of(2026, 11, 21, 6, 0).plusHours(2)).andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // E5 (422): trùng lịch (BR-F9.3)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("TC-F9.2.1-05a: trùng buổi PT BOOKED của huấn luyện viên → 422")
    void overlapsTrainerPtSession() throws Exception {
        f.otherPtSession(pt.staffId(), SAT_10.plusMinutes(30), "BOOKED");
        Account a = f.ptMember(10);
        book(a, pt.staffId(), SAT_10)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail", containsString("Huấn luyện viên đã có buổi PT")));
        assertThat(f.bookedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-F9.2.1-05b: buổi chạm biên và buổi PT đã hủy không chặn → 201")
    void adjacentOrCancelledDoesNotBlock() throws Exception {
        f.otherPtSession(pt.staffId(), SAT_10.plusHours(1), "BOOKED");
        f.otherPtSession(pt.staffId(), SAT_10.minusHours(1), "BOOKED");
        f.otherPtSession(pt.staffId(), SAT_10, "CANCELLED");
        f.classSession(pt.staffId(), SAT_10.minusMinutes(30), SAT_10.minusMinutes(1), "SCHEDULED");
        Account a = f.ptMember(10);
        book(a, pt.staffId(), SAT_10).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-F9.2.1-05c: trùng buổi lớp nhóm huấn luyện viên đang dạy → 422; buổi lớp đã hủy không chặn")
    void overlapsTrainerClassSession() throws Exception {
        f.classSession(pt.staffId(), SAT_10.plusMinutes(30), SAT_10.plusMinutes(90), "SCHEDULED");
        Account a = f.ptMember(10);
        book(a, pt.staffId(), SAT_10)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail", containsString("lớp nhóm")));

        LocalDateTime sun10 = SAT_10.plusDays(1);
        f.classSession(pt.staffId(), sun10, sun10.plusHours(1), "CANCELLED");
        book(a, pt.staffId(), sun10).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-F9.2.1-05d: trùng buổi lớp đã đổi giờ sang khung PT đặt (RESCHEDULED) → 422")
    void overlapsRescheduledClassSession() throws Exception {
        f.classSession(pt.staffId(), SAT_10, SAT_10.plusHours(1), "RESCHEDULED");
        Account a = f.ptMember(10);
        book(a, pt.staffId(), SAT_10).andExpect(status().isUnprocessableContent());
    }

    @Test
    @DisplayName("TC-F9.2.1-05e: trùng lịch lặp ACTIVE của huấn luyện viên chưa sinh buổi (đặt xa hơn 14 ngày) → 422")
    void overlapsTrainerScheduleNotYetGenerated() throws Exception {
        LocalDateTime far = LocalDateTime.of(2026, 12, 12, 10, 0); // thứ Bảy, sau cửa sổ sinh buổi 14 ngày
        f.classScheduleOnly(pt.staffId(), 6, LocalTime.of(10, 30), LocalTime.of(11, 30));
        Account a = f.ptMember(10);
        book(a, pt.staffId(), far)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail", containsString("lớp nhóm")));
        book(a, pt.staffId(), far.plusDays(1)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-F9.2.1-05f: lịch lặp trong cửa sổ 14 ngày mà không có buổi (tác vụ sinh đã bỏ qua) không chặn → 201")
    void skippedOccurrenceInsideGenerationWindowDoesNotBlock() throws Exception {
        f.classScheduleOnly(pt.staffId(), 6, LocalTime.of(10, 30), LocalTime.of(11, 30));
        Account a = f.ptMember(10);
        book(a, pt.staffId(), SAT_10).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-F9.2.1-06a: trùng buổi PT BOOKED của chính hội viên với huấn luyện viên khác → 422")
    void overlapsMemberPtSessionWithOtherTrainer() throws Exception {
        Trainer other = f.trainer(f.cn01);
        Account a = f.ptMember(10);
        f.ptSession(other.staffId(), a.memberId(), f.ptMembershipOf(a.memberId()), SAT_10.minusMinutes(30), "BOOKED");
        book(a, pt.staffId(), SAT_10)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail", containsString("Bạn đã có")));
    }

    @Test
    @DisplayName("TC-F9.2.1-06b: trùng chỗ lớp nhóm BOOKED của hội viên → 422; chỗ đã hủy không chặn")
    void overlapsMemberClassBooking() throws Exception {
        Trainer classTrainer = f.trainer(f.cn01);
        Account a = f.ptMember(10);
        Long session = f.classSession(classTrainer.staffId(), SAT_10.plusMinutes(15), SAT_10.plusMinutes(75),
            "SCHEDULED");
        f.classBooking(session, a, "BOOKED");
        book(a, pt.staffId(), SAT_10)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail", containsString("lớp nhóm")));

        LocalDateTime sun10 = SAT_10.plusDays(1);
        Long sunday = f.classSession(classTrainer.staffId(), sun10, sun10.plusHours(1), "SCHEDULED");
        f.classBooking(sunday, a, "CANCELLED");
        book(a, pt.staffId(), sun10).andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // E5 (422): số buổi đã đặt (BR-F9.4, VD-23 lựa chọn D)
    // ---------------------------------------------------------------------------------------------------------------

    static final String QUOTA_FULL_MESSAGE =
        "Bạn đã đặt đủ số buổi còn lại của gói PT. Vui lòng gia hạn gói để đặt thêm.";

    @Test
    @DisplayName("TC-F9.2.1-08a: gói còn 2 buổi, đã đặt 2 buổi chưa diễn ra → buổi thứ ba 422, câu lỗi không gợi ý chờ")
    void bookedEnoughSessions() throws Exception {
        Account a = f.ptMember(2);
        book(a, pt.staffId(), SAT_10).andExpect(status().isCreated());
        book(a, pt.staffId(), SAT_10.plusDays(1)).andExpect(status().isCreated());
        book(a, pt.staffId(), SAT_10.plusDays(2))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(QUOTA_FULL_MESSAGE))
            .andExpect(jsonPath("$.detail", not(containsString("chờ"))));
        assertThat(f.bookedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-F9.2.1-08b: buổi BOOKED đã qua giờ chưa xác nhận vẫn chặn đặt thêm; buổi CANCELLED, DONE, NO_SHOW không đếm")
    void pastUnconfirmedSessionBlocksBooking() throws Exception {
        Account blocked = f.ptMember(1);
        Long blockedMembership = f.ptMembershipOf(blocked.memberId());
        f.ptSession(pt.staffId(), blocked.memberId(), blockedMembership, LocalDateTime.of(2026, 11, 19, 10, 0),
            "BOOKED");
        book(blocked, pt.staffId(), SAT_10)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(QUOTA_FULL_MESSAGE));

        Account free = f.ptMember(1);
        Long freeMembership = f.ptMembershipOf(free.memberId());
        f.ptSession(pt.staffId(), free.memberId(), freeMembership, LocalDateTime.of(2026, 11, 19, 12, 0), "DONE");
        f.ptSession(pt.staffId(), free.memberId(), freeMembership, LocalDateTime.of(2026, 11, 19, 14, 0), "NO_SHOW");
        f.ptSession(pt.staffId(), free.memberId(), freeMembership, LocalDateTime.of(2026, 11, 22, 15, 0), "CANCELLED");
        book(free, pt.staffId(), SAT_10).andExpect(status().isCreated());
        book(free, pt.staffId(), SAT_10.plusDays(2))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(QUOTA_FULL_MESSAGE));
    }

    @Test
    @DisplayName("TC-F9.2.1-08c: ví dụ VD-23 – gói còn 1 buổi, buổi 08:00 đã qua giờ chưa xác nhận → đặt 18:00 bị 422; PT xác nhận 08:00 → gói về 0, đặt tiếp vẫn bị từ chối")
    void vd23PastMorningBlocksEveningAndConfirmExpiresPackage() throws Exception {
        f.chainConfig("PT_MIN_LEAD_HOURS", 1);
        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        Account a = f.ptMember(1);
        Long membership = f.ptMembershipOf(a.memberId());
        Long morningId = f.ptSession(pt.staffId(), a.memberId(), membership, LocalDateTime.of(2026, 11, 20, 8, 0),
            "BOOKED");
        LocalDateTime evening = LocalDateTime.of(2026, 11, 20, 18, 0);

        book(a, pt.staffId(), evening)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(QUOTA_FULL_MESSAGE));

        mvc.perform(patch("/api/v1/pt-sessions/{id}/confirm", morningId).with(f.bearer(pt.userId(), UserRole.TRAINER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DONE"))
            .andExpect(jsonPath("$.remainingSessions").value(0))
            .andExpect(jsonPath("$.membershipStatus").value("EXPIRED"));

        book(a, pt.staffId(), evening)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail", containsString("gói PT")));
        assertThat(f.bookedCount()).isZero();
    }

    @Test
    @DisplayName("TC-F9.2.1-08d, TC-F9.2.1-09a: gói còn 2 buổi, 1 buổi đã qua giờ chưa xác nhận → đặt được đúng 1 buổi nữa, buổi thứ hai bị 422")
    void twoRemainingWithOnePastUnconfirmedAllowsExactlyOneMore() throws Exception {
        Account a = f.ptMember(2);
        Long membership = f.ptMembershipOf(a.memberId());
        f.ptSession(pt.staffId(), a.memberId(), membership, LocalDateTime.of(2026, 11, 19, 10, 0), "BOOKED");

        book(a, pt.staffId(), SAT_10).andExpect(status().isCreated());
        book(a, pt.staffId(), SAT_10.plusDays(1))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(QUOTA_FULL_MESSAGE));
        assertThat(f.bookedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-F9.2.1-09b: hủy buổi BOOKED đúng hạn trả lại chỗ → hội viên đặt lại được buổi khác")
    void cancelOnTimeFreesQuotaForRebooking() throws Exception {
        Account a = f.ptMember(1);
        String body = book(a, pt.staffId(), SAT_10)
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        Long sessionId = json.readTree(body).get("id").asLong();

        book(a, pt.staffId(), SAT_10.plusDays(1))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(QUOTA_FULL_MESSAGE));

        mvc.perform(post("/api/v1/pt-sessions/{id}/cancel", sessionId).with(f.bearer(a.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        book(a, pt.staffId(), SAT_10.plusDays(1)).andExpect(status().isCreated());
        assertThat(f.bookedCount()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // E1, E2, E3
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("TC-F9.2.1-11b: thiếu huấn luyện viên, thiếu giờ, giờ sai định dạng → 400")
    void invalidInput() throws Exception {
        Account a = f.ptMember(10);
        send(a.userId(), UserRole.MEMBER, Map.of("startAt", "2026-11-21T03:00:00Z"))
            .andExpect(status().isBadRequest());
        send(a.userId(), UserRole.MEMBER, Map.of("trainerId", pt.staffId())).andExpect(status().isBadRequest());
        send(a.userId(), UserRole.MEMBER, Map.of("trainerId", pt.staffId(), "startAt", "21/11/2026 10:00"))
            .andExpect(status().isBadRequest());
        assertThat(f.count("pt_sessions")).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = UserRole.class, names = {"TRAINER", "RECEPTIONIST", "BRANCH_MANAGER", "SUPER_ADMIN"})
    @DisplayName("TC-F9.2.1-12b: người gọi không phải hội viên → 403")
    void nonMemberIsForbidden(UserRole role) throws Exception {
        Long userId = role == UserRole.TRAINER ? pt.userId() : f.staffUser(role, f.cn01);
        send(userId, role, Map.of("trainerId", pt.staffId(), "startAt", "2026-11-21T03:00:00Z"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-F9.2.1-12c: chưa đăng nhập → 401")
    void anonymousIs401() throws Exception {
        mvc.perform(post("/api/v1/pt-sessions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"trainerId\":1,\"startAt\":\"2026-11-21T03:00:00Z\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-F9.2.1-11c: huấn luyện viên không tồn tại, đã nghỉ, hoặc không phải vai trò TRAINER → 404")
    void trainerNotFound() throws Exception {
        Account a = f.ptMember(10);
        book(a, 999_999L, SAT_10).andExpect(status().isNotFound());

        Trainer inactive = f.trainer(f.cn01);
        jdbc.update("UPDATE staff SET status = 'INACTIVE' WHERE id = ?", inactive.staffId());
        book(a, inactive.staffId(), SAT_10).andExpect(status().isNotFound());

        Long receptionistStaff = f.t.staffId(f.staffUser(UserRole.RECEPTIONIST, f.cn01));
        book(a, receptionistStaff, SAT_10).andExpect(status().isNotFound());
        assertThat(f.count("pt_sessions")).isZero();
    }

    @Test
    @DisplayName("TC-F9.2.1-01d: buổi PT ở chi nhánh của huấn luyện viên (CN02), gói CN02 → branch_id = CN02")
    void sessionBranchIsTrainerBranch() throws Exception {
        Trainer cn02Trainer = f.trainer(f.cn02);
        Account a = f.account(f.cn02, "ACTIVE");
        f.membership(a.memberId(), "PT", f.cn02, "ACTIVE", PtSessionFixtures.PKG_START, PtSessionFixtures.PKG_END, 3);
        book(a, cn02Trainer.staffId(), SAT_10)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.branchId").value(f.cn02));
        OffsetDateTime stored = jdbc.queryForObject("SELECT start_at FROM pt_sessions", OffsetDateTime.class);
        assertThat(stored.toInstant()).isEqualTo(vn(SAT_10).toInstant());
    }
}
