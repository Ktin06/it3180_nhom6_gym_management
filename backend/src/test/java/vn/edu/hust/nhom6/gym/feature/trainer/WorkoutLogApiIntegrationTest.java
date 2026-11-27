package vn.edu.hust.nhom6.gym.feature.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.trainer.StudentFixtures.Member;
import vn.edu.hust.nhom6.gym.feature.trainer.StudentFixtures.Trainer;

/**
 * Kiểm thử tích hợp ghi nhật ký tập {@code POST /api/v1/pt/members/{memberId}/workout-logs} (F9, UC-F9.5,
 * BR-F9.10, BR-F9.11; S2-20.1 GĐ-4, GĐ-5, GĐ-6, GĐ-11). SRS F9 chưa có mã AC nên tên hiển thị theo US-F9.5.1.
 *
 * <p>Mốc: thứ Bảy 21/11/2026 10:00 giờ Việt Nam. Buổi PT mẫu: 20/11 06:30 giờ VN (tức 19/11 23:30 UTC), để thử ngày
 * của buổi tính theo giờ Việt Nam.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class WorkoutLogApiIntegrationTest {
    static final LocalDateTime SESSION_AT = LocalDateTime.of(2026, 11, 20, 6, 30);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;

    StudentFixtures f;
    Trainer pt;
    Member student;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 10, 0));
        f = new StudentFixtures(jdbc, tokens);
        f.reset();
        pt = f.trainer(f.cn01);
        student = f.member(f.cn01);
    }

    ResultActions log(Trainer trainer, Long memberId, String body) throws Exception {
        return mvc.perform(post("/api/v1/pt/members/{id}/workout-logs", memberId)
            .with(f.bearer(trainer.userId(), UserRole.TRAINER))
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    static String body(Long sessionId, String loggedAt) {
        return """
            {"ptSessionId": %s, "loggedAt": "%s", "exercise": "Squat", "sets": 4, "reps": 10, "weight": 60.5}"""
            .formatted(sessionId == null ? "null" : sessionId, loggedAt);
    }

    @Test
    @DisplayName("TC-F9.5.1-01a: PT ghi nhật ký ngoài buổi PT cho học viên PT mình dạy → 201, ghi audit_logs CREATE với người ghi là PT")
    void createWithoutSession() throws Exception {
        f.ptSession(pt, student, LocalDateTime.of(2026, 11, 25, 9, 0), "BOOKED");

        log(pt, student.memberId(), body(null, "2026-11-21"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.memberId").value(student.memberId()))
            .andExpect(jsonPath("$.ptSessionId").value(nullValue()))
            .andExpect(jsonPath("$.loggedAt").value("2026-11-21"))
            .andExpect(jsonPath("$.exercise").value("Squat"))
            .andExpect(jsonPath("$.sets").value(4))
            .andExpect(jsonPath("$.reps").value(10))
            .andExpect(jsonPath("$.weight").value(60.5));

        Map<String, Object> audit = jdbc.queryForMap(
            "SELECT user_id, branch_id, action, entity, entity_id, new_value ->> 'exercise' AS exercise FROM audit_logs");
        Long logId = jdbc.queryForObject("SELECT id FROM workout_logs", Long.class);
        assertThat(audit).containsEntry("user_id", pt.userId()).containsEntry("branch_id", f.cn01)
            .containsEntry("action", "CREATE").containsEntry("entity", "workout_logs")
            .containsEntry("entity_id", logId).containsEntry("exercise", "Squat");
    }

    @Test
    @DisplayName("TC-F9.5.1-03a: gắn buổi PT DONE của đúng PT, đúng hội viên, đúng ngày VN (buổi 06:30 VN = 23:30 UTC hôm trước) → 201")
    void createWithDoneSessionSameVnDay() throws Exception {
        Long session = f.ptSession(pt, student, SESSION_AT, "DONE");

        log(pt, student.memberId(), body(session, "2026-11-20"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ptSessionId").value(session));
        assertThat(jdbc.queryForObject("SELECT pt_session_id FROM workout_logs", Long.class)).isEqualTo(session);
    }

    @Test
    @DisplayName("TC-F9.5.1-03b: ngày tập khác ngày VN của buổi (ghi theo ngày UTC 19/11) → 422")
    void sessionDateMismatch() throws Exception {
        Long session = f.ptSession(pt, student, SESSION_AT, "DONE");

        log(pt, student.memberId(), body(session, "2026-11-19"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Ngày tập phải trùng ngày diễn ra buổi PT."));
        assertThat(f.count("workout_logs")).isZero();
    }

    @ParameterizedTest(name = "buổi {0}")
    @ValueSource(strings = {"BOOKED", "NO_SHOW", "CANCELLED"})
    @DisplayName("TC-F9.5.1-03c: buổi PT chưa DONE (BOOKED, NO_SHOW, CANCELLED) → 422, không lưu")
    void sessionNotDone(String status) throws Exception {
        f.ptSession(pt, student, LocalDateTime.of(2026, 11, 10, 9, 0), "DONE");
        Long session = f.ptSession(pt, student, SESSION_AT, status);

        log(pt, student.memberId(), body(session, "2026-11-20"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Chỉ ghi nhật ký cho buổi PT đã xác nhận diễn ra."));
        assertThat(f.count("workout_logs")).isZero();
        assertThat(f.count("audit_logs")).isZero();
    }

    @Test
    @DisplayName("TC-F9.5.1-03d: buổi PT của hội viên khác, của PT khác, hoặc không tồn tại → 422")
    void sessionNotBelonging() throws Exception {
        f.ptSession(pt, student, LocalDateTime.of(2026, 11, 10, 9, 0), "DONE");
        Member other = f.member(f.cn01);
        Long otherMembersSession = f.ptSession(pt, other, SESSION_AT, "DONE");
        Trainer otherPt = f.trainer(f.cn01);
        Long otherTrainersSession = f.ptSession(otherPt, student, SESSION_AT.plusHours(3), "DONE");

        for (Long session : new Long[] {otherMembersSession, otherTrainersSession, 999_999L}) {
            log(pt, student.memberId(), body(session, "2026-11-20"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Buổi PT không thuộc huấn luyện viên và hội viên này."));
        }
        assertThat(f.count("workout_logs")).isZero();
    }

    @Test
    @DisplayName("TC-F9.5.1-04a: ngày tập sau hôm nay (giờ VN) → 400; đúng hôm nay → 201")
    void futureDate() throws Exception {
        f.ptSession(pt, student, LocalDateTime.of(2026, 11, 25, 9, 0), "BOOKED");
        clock.setVn(LocalDateTime.of(2026, 11, 21, 23, 30)); // 16:30 UTC, ngày UTC vẫn 21/11

        log(pt, student.memberId(), body(null, "2026-11-22"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Ngày tập không được sau hôm nay."));
        clock.setVn(LocalDateTime.of(2026, 11, 22, 0, 30)); // 21/11 17:30 UTC nhưng đã là 22/11 ở Việt Nam
        log(pt, student.memberId(), body(null, "2026-11-22")).andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "{\"exercise\": \"Squat\"}",
        "{\"loggedAt\": \"2026-11-21\"}",
        "{\"loggedAt\": \"2026-11-21\", \"exercise\": \" \"}",
        "{\"loggedAt\": \"2026-11-21\", \"exercise\": \"Squat\", \"sets\": 0}",
        "{\"loggedAt\": \"2026-11-21\", \"exercise\": \"Squat\", \"reps\": 1001}",
        "{\"loggedAt\": \"2026-11-21\", \"exercise\": \"Squat\", \"weight\": -1}",
        "{\"loggedAt\": \"2026-11-21\", \"exercise\": \"Squat\", \"weight\": 10.555}",
        "{\"loggedAt\": \"2026-11-21\", \"exercise\": \"Squat\", \"weight\": 10000}",
        "{\"loggedAt\": \"21/11/2026\", \"exercise\": \"Squat\"}",
        "{\"loggedAt\": \"2026-11-21\", \"exercise\": \"Squat\", \"ptSessionId\": 0}"
    })
    @DisplayName("TC-F9.5.1-04b: thiếu ngày, thiếu bài tập, giá trị ngoài khoảng, sai định dạng → 400, không lưu")
    void invalidInput(String json) throws Exception {
        f.ptSession(pt, student, LocalDateTime.of(2026, 11, 25, 9, 0), "BOOKED");

        log(pt, student.memberId(), json).andExpect(status().isBadRequest());
        assertThat(f.count("workout_logs")).isZero();
    }

    @Test
    @DisplayName("TC-F9.5.1-01b: bài không tạ (weight = 0) và không có số hiệp, số lần vẫn hợp lệ → 201")
    void minimalValid() throws Exception {
        f.ptSession(pt, student, LocalDateTime.of(2026, 11, 25, 9, 0), "BOOKED");

        log(pt, student.memberId(), "{\"loggedAt\": \"2026-11-21\", \"exercise\": \"Plank\", \"weight\": 0}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sets").value(nullValue()));
    }

    @Test
    @DisplayName("TC-F9.5.1-05a: PT không dạy cùng chi nhánh → 403; không tồn tại hoặc chi nhánh khác → 404; không lưu")
    void forbiddenAndNotFound() throws Exception {
        log(pt, student.memberId(), body(null, "2026-11-21")).andExpect(status().isForbidden());
        log(pt, 999_999L, body(null, "2026-11-21")).andExpect(status().isNotFound());
        Member otherBranch = f.member(f.cn02);
        log(pt, otherBranch.memberId(), body(null, "2026-11-21")).andExpect(status().isNotFound());
        assertThat(f.count("workout_logs")).isZero();
    }

    @Test
    @DisplayName("TC-F9.5.1-06a: hội viên tự ghi qua API của PT → 403; lễ tân → 403")
    void otherRolesForbidden() throws Exception {
        f.ptSession(pt, student, LocalDateTime.of(2026, 11, 25, 9, 0), "BOOKED");
        mvc.perform(post("/api/v1/pt/members/{id}/workout-logs", student.memberId())
                .with(f.bearer(student.userId(), UserRole.MEMBER))
                .contentType(MediaType.APPLICATION_JSON).content(body(null, "2026-11-21")))
            .andExpect(status().isForbidden());
        Long receptionist = f.staffUser(UserRole.RECEPTIONIST, f.cn01);
        mvc.perform(post("/api/v1/pt/members/{id}/workout-logs", student.memberId())
                .with(f.bearer(receptionist, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON).content(body(null, "2026-11-21")))
            .andExpect(status().isForbidden());
        assertThat(f.count("workout_logs")).isZero();
    }
}
