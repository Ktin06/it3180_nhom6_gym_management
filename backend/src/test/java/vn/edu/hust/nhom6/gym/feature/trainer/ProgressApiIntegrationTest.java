package vn.edu.hust.nhom6.gym.feature.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.trainer.StudentFixtures.Member;
import vn.edu.hust.nhom6.gym.feature.trainer.StudentFixtures.Trainer;

/**
 * Kiểm thử tích hợp tiến độ {@code GET /api/v1/members/{memberId}/progress} và {@code GET /api/v1/me/progress} (F9,
 * UC-F9.6, BR-F9.10, BR-F9.12, BR-F9.13; S2-20.1 GĐ-8, GĐ-9). SRS F9 chưa có mã AC nên tên hiển thị theo US-F9.6.1.
 *
 * <p>Mốc: 21/11/2026 10:00 giờ Việt Nam; mặc định 90 ngày là 24/08 – 21/11.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class ProgressApiIntegrationTest {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 21);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired JsonMapper json;

    StudentFixtures f;
    Trainer pt;
    Member student;

    @BeforeEach
    void setUp() {
        clock.setVn(TODAY.atTime(10, 0));
        f = new StudentFixtures(jdbc, tokens);
        f.reset();
        pt = f.trainer(f.cn01);
        student = f.member(f.cn01);
        f.ptSession(pt, student, LocalDateTime.of(2026, 11, 10, 9, 0), "DONE");
    }

    ResultActions as(Long userId, UserRole role, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(f.bearer(userId, role)));
    }

    MockHttpServletRequestBuilder progressOf(Long memberId) {
        return get("/api/v1/members/{id}/progress", memberId);
    }

    @Test
    @DisplayName("US-F9.6.1 KB1: PT xem tiến độ học viên mình dạy → 200, chỉ số và nhật ký trong khoảng, sắp ngày tăng dần, không lẫn người khác")
    void trainerSeesStudentProgress() throws Exception {
        Long receptionist = f.staffUser(UserRole.RECEPTIONIST, f.cn01);
        f.bodyMetric(student.memberId(), receptionist, LocalDate.of(2026, 11, 15), "66.8");
        f.bodyMetric(student.memberId(), pt.userId(), LocalDate.of(2026, 11, 1), "68");
        f.bodyMetric(student.memberId(), student.userId(), LocalDate.of(2026, 11, 15), "66.5");
        f.bodyMetric(student.memberId(), pt.userId(), LocalDate.of(2026, 10, 31), "69");
        f.workoutLog(student.memberId(), LocalDate.of(2026, 11, 20), "Deadlift");
        f.workoutLog(student.memberId(), LocalDate.of(2026, 11, 2), "Squat");
        f.workoutLog(student.memberId(), LocalDate.of(2026, 12, 1), "Ngoài khoảng");
        Member other = f.member(f.cn01);
        f.bodyMetric(other.memberId(), receptionist, LocalDate.of(2026, 11, 10), "90");
        f.workoutLog(other.memberId(), LocalDate.of(2026, 11, 10), "Người khác");

        as(pt.userId(), UserRole.TRAINER, progressOf(student.memberId()).param("from", "2026-11-01")
                .param("to", "2026-11-30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.memberId").value(student.memberId()))
            .andExpect(jsonPath("$.memberCode").isString())
            .andExpect(jsonPath("$.fullName").isString())
            .andExpect(jsonPath("$.from").value("2026-11-01"))
            .andExpect(jsonPath("$.to").value("2026-11-30"))
            .andExpect(jsonPath("$.bodyMetrics", hasSize(3)))
            .andExpect(jsonPath("$.bodyMetrics[0].measuredAt").value("2026-11-01"))
            .andExpect(jsonPath("$.bodyMetrics[0].weight").value(68.0))
            .andExpect(jsonPath("$.bodyMetrics[1].measuredAt").value("2026-11-15"))
            .andExpect(jsonPath("$.bodyMetrics[1].recordedBy").value(receptionist))
            .andExpect(jsonPath("$.bodyMetrics[2].recordedBy").value(student.userId()))
            .andExpect(jsonPath("$.workoutLogs", hasSize(2)))
            .andExpect(jsonPath("$.workoutLogs[0].exercise").value("Squat"))
            .andExpect(jsonPath("$.workoutLogs[1].exercise").value("Deadlift"))
            .andExpect(jsonPath("$.workoutLogs[1].weight").value(40.0));
    }

    @Test
    @DisplayName("US-F9.6.1 BR-F9.13: phản hồi chỉ có dữ liệu thô (không có trường gợi ý, nhận xét hay dự đoán)")
    void rawDataOnly() throws Exception {
        String body = as(pt.userId(), UserRole.TRAINER, progressOf(student.memberId()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Map<String, Object> map = json.readValue(body, new tools.jackson.core.type.TypeReference<>() { });
        assertThat(map.keySet()).containsExactlyInAnyOrder("memberId", "memberCode", "fullName", "from", "to",
            "bodyMetrics", "workoutLogs");
    }

    @Test
    @DisplayName("US-F9.6.1 GĐ-8: bỏ trống khoảng → 90 ngày đến hôm nay theo giờ VN (24/08 có, 23/08 không)")
    void defaultRange() throws Exception {
        f.workoutLog(student.memberId(), LocalDate.of(2026, 8, 23), "Quá cũ");
        f.workoutLog(student.memberId(), LocalDate.of(2026, 8, 24), "Ngày đầu");
        f.workoutLog(student.memberId(), TODAY, "Hôm nay");
        clock.setVn(LocalDateTime.of(2026, 11, 21, 6, 0)); // 20/11 23:00 UTC: ngày VN vẫn là 21/11

        as(pt.userId(), UserRole.TRAINER, progressOf(student.memberId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.from").value("2026-08-24"))
            .andExpect(jsonPath("$.to").value("2026-11-21"))
            .andExpect(jsonPath("$.workoutLogs", hasSize(2)))
            .andExpect(jsonPath("$.workoutLogs[0].exercise").value("Ngày đầu"))
            .andExpect(jsonPath("$.workoutLogs[1].exercise").value("Hôm nay"));
    }

    @Test
    @DisplayName("US-F9.6.1 A1: chưa có dữ liệu → 200 với hai danh sách rỗng")
    void emptyProgress() throws Exception {
        as(student.userId(), UserRole.MEMBER, get("/api/v1/me/progress"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bodyMetrics", hasSize(0)))
            .andExpect(jsonPath("$.workoutLogs", hasSize(0)));
    }

    @Test
    @DisplayName("US-F9.6.1 BR-F9.12: hội viên xem của mình qua /me/progress và /members/{id}/progress → 200; id người khác → 404")
    void memberSeesOnlyOwn() throws Exception {
        f.workoutLog(student.memberId(), LocalDate.of(2026, 11, 20), "Squat");
        Member other = f.member(f.cn01);
        f.workoutLog(other.memberId(), LocalDate.of(2026, 11, 20), "Người khác");

        as(student.userId(), UserRole.MEMBER, get("/api/v1/me/progress"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.memberId").value(student.memberId()))
            .andExpect(jsonPath("$.workoutLogs", hasSize(1)))
            .andExpect(jsonPath("$.workoutLogs[0].exercise").value("Squat"));
        as(student.userId(), UserRole.MEMBER, progressOf(student.memberId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workoutLogs[0].exercise").value("Squat"));
        as(student.userId(), UserRole.MEMBER, progressOf(other.memberId()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US-F9.6.1 E1: from sau to, khoảng dài hơn 366 ngày, sai định dạng ngày → 400; đúng 366 ngày → 200")
    void invalidRange() throws Exception {
        as(pt.userId(), UserRole.TRAINER, progressOf(student.memberId()).param("from", "2026-11-21")
                .param("to", "2026-11-20"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Ngày bắt đầu không được sau ngày kết thúc."));
        as(pt.userId(), UserRole.TRAINER, progressOf(student.memberId()).param("from", "2025-11-20")
                .param("to", "2026-11-21"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Khoảng thời gian xem tiến độ tối đa 366 ngày."));
        as(pt.userId(), UserRole.TRAINER, progressOf(student.memberId()).param("from", "2025-11-21")
                .param("to", "2026-11-21"))
            .andExpect(status().isOk());
        as(student.userId(), UserRole.MEMBER, get("/api/v1/me/progress").param("from", "21/11/2026"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-F9.6.1 E2: PT không dạy cùng chi nhánh → 403; chi nhánh khác → 404; không tồn tại → 404")
    void trainerScope() throws Exception {
        Trainer other = f.trainer(f.cn01);
        as(other.userId(), UserRole.TRAINER, progressOf(student.memberId())).andExpect(status().isForbidden());
        Member otherBranch = f.member(f.cn02);
        as(pt.userId(), UserRole.TRAINER, progressOf(otherBranch.memberId())).andExpect(status().isNotFound());
        as(pt.userId(), UserRole.TRAINER, progressOf(999_999L)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US-F9.6.1 GĐ-9: lễ tân, quản lý, chủ chuỗi gọi API tiến độ → 403; PT gọi /me/progress → 403; chưa đăng nhập → 401")
    void otherRoles() throws Exception {
        for (UserRole role : new UserRole[] {UserRole.RECEPTIONIST, UserRole.BRANCH_MANAGER}) {
            Long staff = f.staffUser(role, f.cn01);
            as(staff, role, progressOf(student.memberId())).andExpect(status().isForbidden());
        }
        Long owner = f.staffUser(UserRole.SUPER_ADMIN, null);
        as(owner, UserRole.SUPER_ADMIN, progressOf(student.memberId())).andExpect(status().isForbidden());
        as(pt.userId(), UserRole.TRAINER, get("/api/v1/me/progress")).andExpect(status().isForbidden());
        mvc.perform(progressOf(student.memberId())).andExpect(status().isUnauthorized());
    }
}
