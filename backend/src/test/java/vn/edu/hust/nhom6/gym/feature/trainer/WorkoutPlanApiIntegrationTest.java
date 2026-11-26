package vn.edu.hust.nhom6.gym.feature.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

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
 * Kiểm thử tích hợp soạn và xem giáo án {@code /api/v1/pt/members/{memberId}/workout-plans} (F9, UC-F9.4, BR-F9.10;
 * S2-20.1). SRS F9 chưa có mã AC nên tên hiển thị theo US-F9.4.1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class WorkoutPlanApiIntegrationTest {
    static final String VALID = """
        {"title": "Giáo án tăng cơ 4 tuần",
         "content": [{"session": 1, "exercises": [{"name": "Squat", "sets": 4, "reps": 10}, {"name": "Plank"}]},
                     {"session": 2, "exercises": [{"name": "Deadlift", "sets": 3, "reps": 8}]}],
         "startDate": "2026-11-23", "endDate": "2026-12-20"}""";

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
        f.ptSession(pt, student, LocalDateTime.of(2026, 11, 10, 9, 0), "DONE");
    }

    ResultActions create(Long userId, UserRole role, Long memberId, String body) throws Exception {
        return mvc.perform(post("/api/v1/pt/members/{id}/workout-plans", memberId).with(f.bearer(userId, role))
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    @DisplayName("US-F9.4.1 KB1: PT soạn giáo án cho học viên mình dạy → 201, trainerId lấy từ token, nội dung lưu jsonb đúng")
    void createSuccess() throws Exception {
        create(pt.userId(), UserRole.TRAINER, student.memberId(), VALID)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.trainerId").value(pt.staffId()))
            .andExpect(jsonPath("$.trainerName").value("Nhân viên test"))
            .andExpect(jsonPath("$.memberId").value(student.memberId()))
            .andExpect(jsonPath("$.title").value("Giáo án tăng cơ 4 tuần"))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].session").value(1))
            .andExpect(jsonPath("$.content[0].exercises[0].name").value("Squat"))
            .andExpect(jsonPath("$.content[0].exercises[0].sets").value(4))
            .andExpect(jsonPath("$.content[0].exercises[1].name").value("Plank"))
            .andExpect(jsonPath("$.startDate").value("2026-11-23"))
            .andExpect(jsonPath("$.endDate").value("2026-12-20"))
            .andExpect(jsonPath("$.createdAt").value("2026-11-21T03:00:00Z"));

        assertThat(jdbc.queryForObject(
            "SELECT content -> 1 -> 'exercises' -> 0 ->> 'name' FROM workout_plans", String.class))
            .isEqualTo("Deadlift");
        assertThat(jdbc.queryForObject("SELECT trainer_id FROM workout_plans", Long.class)).isEqualTo(pt.staffId());
    }

    @Test
    @DisplayName("US-F9.4.1 KB1: không gửi ngày áp dụng vẫn lưu được (start_date, end_date rỗng)")
    void datesAreOptional() throws Exception {
        create(pt.userId(), UserRole.TRAINER, student.memberId(), """
            {"title": "Giáo án cơ bản", "content": [{"session": 1, "exercises": [{"name": "Chạy bộ"}]}]}""")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.startDate").doesNotExist());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "{\"content\": [{\"session\": 1, \"exercises\": [{\"name\": \"Squat\"}]}]}",
        "{\"title\": \"  \", \"content\": [{\"session\": 1, \"exercises\": [{\"name\": \"Squat\"}]}]}",
        "{\"title\": \"A\"}",
        "{\"title\": \"A\", \"content\": []}",
        "{\"title\": \"A\", \"content\": [{\"session\": 1, \"exercises\": []}]}",
        "{\"title\": \"A\", \"content\": [{\"session\": 0, \"exercises\": [{\"name\": \"Squat\"}]}]}",
        "{\"title\": \"A\", \"content\": [{\"session\": 1, \"exercises\": [{\"name\": \"\"}]}]}",
        "{\"title\": \"A\", \"content\": [{\"session\": 1, \"exercises\": [{\"name\": \"Squat\", \"sets\": 0}]}]}",
        "{\"title\": \"A\", \"content\": [{\"session\": 1, \"exercises\": [{\"name\": \"Squat\", \"reps\": 1001}]}]}",
        "{\"title\": \"A\", \"content\": \"Squat 4x10\"}"
    })
    @DisplayName("US-F9.4.1 E1: thiếu tên, nội dung rỗng hoặc sai định dạng → 400, không lưu")
    void invalidInput(String body) throws Exception {
        create(pt.userId(), UserRole.TRAINER, student.memberId(), body).andExpect(status().isBadRequest());
        assertThat(f.count("workout_plans")).isZero();
    }

    @Test
    @DisplayName("US-F9.4.1 E1: ngày kết thúc trước ngày bắt đầu → 400")
    void endBeforeStart() throws Exception {
        create(pt.userId(), UserRole.TRAINER, student.memberId(), """
            {"title": "A", "content": [{"session": 1, "exercises": [{"name": "Squat"}]}],
             "startDate": "2026-12-01", "endDate": "2026-11-30"}""")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Ngày kết thúc không được trước ngày bắt đầu."));
        assertThat(f.count("workout_plans")).isZero();
    }

    @Test
    @DisplayName("US-F9.4.1 E2: PT không dạy hội viên cùng chi nhánh → 403, không lưu")
    void notTeachingIs403() throws Exception {
        Trainer other = f.trainer(f.cn01);

        create(other.userId(), UserRole.TRAINER, student.memberId(), VALID).andExpect(status().isForbidden());
        assertThat(f.count("workout_plans")).isZero();
    }

    @Test
    @DisplayName("US-F9.4.1 E3: hội viên không tồn tại → 404; hội viên chi nhánh khác PT không dạy → 404")
    void notFoundAndOtherBranch() throws Exception {
        create(pt.userId(), UserRole.TRAINER, 999_999L, VALID).andExpect(status().isNotFound());
        Member otherBranch = f.member(f.cn02);
        create(pt.userId(), UserRole.TRAINER, otherBranch.memberId(), VALID).andExpect(status().isNotFound());
        assertThat(f.count("workout_plans")).isZero();
    }

    @Test
    @DisplayName("US-F9.4.1 S3: hội viên, lễ tân, quản lý, chủ chuỗi không soạn giáo án → 403; chưa đăng nhập → 401")
    void otherRolesForbidden() throws Exception {
        create(student.userId(), UserRole.MEMBER, student.memberId(), VALID).andExpect(status().isForbidden());
        for (UserRole role : new UserRole[] {UserRole.RECEPTIONIST, UserRole.BRANCH_MANAGER}) {
            Long staff = f.staffUser(role, f.cn01);
            create(staff, role, student.memberId(), VALID).andExpect(status().isForbidden());
        }
        Long owner = f.staffUser(UserRole.SUPER_ADMIN, null);
        create(owner, UserRole.SUPER_ADMIN, student.memberId(), VALID).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pt/members/{id}/workout-plans", student.memberId())
            .contentType(MediaType.APPLICATION_JSON).content(VALID)).andExpect(status().isUnauthorized());
        assertThat(f.count("workout_plans")).isZero();
    }

    @Test
    @DisplayName("US-F9.4.1 GĐ-3: một hội viên có nhiều giáo án; PT xem được mọi giáo án của học viên (kể cả PT khác soạn), mới nhất trước")
    void listPlans() throws Exception {
        Trainer second = f.trainer(f.cn01);
        f.ptSession(second, student, LocalDateTime.of(2026, 11, 25, 9, 0), "BOOKED");
        create(pt.userId(), UserRole.TRAINER, student.memberId(), VALID).andExpect(status().isCreated());
        clock.setVn(LocalDateTime.of(2026, 11, 21, 11, 0));
        create(second.userId(), UserRole.TRAINER, student.memberId(), """
            {"title": "Giáo án bổ sung", "content": [{"session": 1, "exercises": [{"name": "Plank"}]}]}""")
            .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/pt/members/{id}/workout-plans", student.memberId())
                .with(f.bearer(pt.userId(), UserRole.TRAINER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].title").value("Giáo án bổ sung"))
            .andExpect(jsonPath("$[0].trainerId").value(second.staffId()))
            .andExpect(jsonPath("$[1].title").value("Giáo án tăng cơ 4 tuần"))
            .andExpect(jsonPath("$[1].content[0].exercises[0].name").value("Squat"));
    }

    @Test
    @DisplayName("US-F9.4.1 E2: PT không dạy xem giáo án → 403; hội viên gọi API xem của PT → 403")
    void listForbidden() throws Exception {
        Trainer other = f.trainer(f.cn01);
        mvc.perform(get("/api/v1/pt/members/{id}/workout-plans", student.memberId())
                .with(f.bearer(other.userId(), UserRole.TRAINER)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/pt/members/{id}/workout-plans", student.memberId())
                .with(f.bearer(student.userId(), UserRole.MEMBER)))
            .andExpect(status().isForbidden());
    }
}
