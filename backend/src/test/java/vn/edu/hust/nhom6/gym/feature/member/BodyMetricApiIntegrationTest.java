package vn.edu.hust.nhom6.gym.feature.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import vn.edu.hust.nhom6.gym.feature.trainer.StudentFixtures;
import vn.edu.hust.nhom6.gym.feature.trainer.StudentFixtures.Member;
import vn.edu.hust.nhom6.gym.feature.trainer.StudentFixtures.Trainer;

/**
 * Kiểm thử tích hợp chỉ số cơ thể {@code GET/POST /api/v1/members/{id}/body-metrics} (F2 UC-F2.5, mục 6; F9 UC-F9.5
 * BR-F9.10; S2-20.1 GĐ-5, GĐ-6). SRS F2 chưa có mã AC cho UC-F2.5 nên tên hiển thị theo US-F2.5.1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class BodyMetricApiIntegrationTest {
    static final String VALID = """
        {"measuredAt": "2026-11-21", "weight": 65.5, "height": 170.0, "bodyFat": 18.5, "muscleMass": 30.2,
         "note": "Đo buổi sáng"}""";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;

    StudentFixtures f;
    Trainer pt;
    Member student;
    Long receptionist;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 10, 0));
        f = new StudentFixtures(jdbc, tokens);
        f.reset();
        pt = f.trainer(f.cn01);
        student = f.member(f.cn01);
        f.ptSession(pt, student, LocalDateTime.of(2026, 11, 10, 9, 0), "DONE");
        receptionist = f.staffUser(UserRole.RECEPTIONIST, f.cn01);
    }

    ResultActions record(Long userId, UserRole role, Long memberId, String body) throws Exception {
        return mvc.perform(post("/api/v1/members/{id}/body-metrics", memberId).with(f.bearer(userId, role))
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    ResultActions history(Long userId, UserRole role, Long memberId) throws Exception {
        return mvc.perform(get("/api/v1/members/{id}/body-metrics", memberId).with(f.bearer(userId, role)));
    }

    @Test
    @DisplayName("US-F2.5.1 KB1: lễ tân ghi chỉ số cho hội viên chi nhánh mình → 201, recorded_by = người đang đăng nhập")
    void receptionistRecords() throws Exception {
        record(receptionist, UserRole.RECEPTIONIST, student.memberId(), VALID)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.memberId").value(student.memberId()))
            .andExpect(jsonPath("$.measuredAt").value("2026-11-21"))
            .andExpect(jsonPath("$.weight").value(65.5))
            .andExpect(jsonPath("$.height").value(170.0))
            .andExpect(jsonPath("$.bodyFat").value(18.5))
            .andExpect(jsonPath("$.muscleMass").value(30.2))
            .andExpect(jsonPath("$.note").value("Đo buổi sáng"))
            .andExpect(jsonPath("$.recordedBy").value(receptionist));

        Map<String, Object> row = jdbc.queryForMap("SELECT member_id, recorded_by, weight::text AS w FROM body_metrics");
        assertThat(row).containsEntry("member_id", student.memberId()).containsEntry("recorded_by", receptionist)
            .containsEntry("w", "65.50");
    }

    @Test
    @DisplayName("US-F2.5.1 KB1: PT ghi chỉ số cho học viên mình dạy → 201; chỉ một chỉ số cũng được")
    void trainerRecordsForStudent() throws Exception {
        record(pt.userId(), UserRole.TRAINER, student.memberId(), "{\"measuredAt\": \"2026-11-20\", \"weight\": 64}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.recordedBy").value(pt.userId()))
            .andExpect(jsonPath("$.height").value(nullValue()));
    }

    @Test
    @DisplayName("US-F2.5.1 BR-F9.10: PT không dạy hội viên cùng chi nhánh → 403; chi nhánh khác → 404; không lưu")
    void trainerNotTeaching() throws Exception {
        Trainer other = f.trainer(f.cn01);
        record(other.userId(), UserRole.TRAINER, student.memberId(), VALID).andExpect(status().isForbidden());
        Member otherBranch = f.member(f.cn02);
        record(pt.userId(), UserRole.TRAINER, otherBranch.memberId(), VALID).andExpect(status().isNotFound());
        assertThat(f.count("body_metrics")).isZero();
    }

    @Test
    @DisplayName("US-F2.5.1 S3: nhân viên chi nhánh khác → 404; chủ chuỗi ghi được mọi chi nhánh → 201; hội viên tự ghi → 403")
    void staffScope() throws Exception {
        Long otherReceptionist = f.staffUser(UserRole.RECEPTIONIST, f.cn02);
        record(otherReceptionist, UserRole.RECEPTIONIST, student.memberId(), VALID).andExpect(status().isNotFound());
        Long otherManager = f.staffUser(UserRole.BRANCH_MANAGER, f.cn02);
        record(otherManager, UserRole.BRANCH_MANAGER, student.memberId(), VALID).andExpect(status().isNotFound());
        record(student.userId(), UserRole.MEMBER, student.memberId(), VALID).andExpect(status().isForbidden());
        assertThat(f.count("body_metrics")).isZero();

        Long owner = f.staffUser(UserRole.SUPER_ADMIN, null);
        Member cn02Member = f.member(f.cn02);
        record(owner, UserRole.SUPER_ADMIN, cn02Member.memberId(), VALID).andExpect(status().isCreated());
        record(receptionist, UserRole.RECEPTIONIST, 999_999L, VALID).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US-F2.5.1 GĐ-6: không có chỉ số nào → 400 với câu báo rõ")
    void noMetric() throws Exception {
        record(receptionist, UserRole.RECEPTIONIST, student.memberId(), "{\"measuredAt\": \"2026-11-21\", \"note\": \"x\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Vui lòng nhập ít nhất một chỉ số cơ thể."));
        assertThat(f.count("body_metrics")).isZero();
    }

    @Test
    @DisplayName("US-F2.5.1 GĐ-5: ngày đo sau hôm nay (giờ VN) → 400")
    void futureDate() throws Exception {
        record(receptionist, UserRole.RECEPTIONIST, student.memberId(), "{\"measuredAt\": \"2026-11-22\", \"weight\": 60}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Ngày đo không được sau hôm nay."));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "{\"weight\": 60}",
        "{\"measuredAt\": \"2026-11-21\", \"weight\": 0}",
        "{\"measuredAt\": \"2026-11-21\", \"weight\": 500.01}",
        "{\"measuredAt\": \"2026-11-21\", \"height\": -170}",
        "{\"measuredAt\": \"2026-11-21\", \"bodyFat\": 100}",
        "{\"measuredAt\": \"2026-11-21\", \"bodyFat\": -0.5}",
        "{\"measuredAt\": \"2026-11-21\", \"muscleMass\": 0}",
        "{\"measuredAt\": \"2026-11-21\", \"weight\": 60.123}"
    })
    @DisplayName("US-F2.5.1 E1: thiếu ngày đo, chỉ số âm, bằng 0 hoặc ngoài khoảng, quá 2 chữ số thập phân → 400")
    void invalidInput(String json) throws Exception {
        record(receptionist, UserRole.RECEPTIONIST, student.memberId(), json).andExpect(status().isBadRequest());
        assertThat(f.count("body_metrics")).isZero();
    }

    @Test
    @DisplayName("US-F2.5.1 KB3: xem lịch sử sắp measured_at giảm dần (cùng ngày thì dòng ghi sau trước); không lẫn hội viên khác")
    void historySorted() throws Exception {
        Long oldest = f.bodyMetric(student.memberId(), receptionist, LocalDate.of(2026, 10, 1), "70");
        Long sameDayFirst = f.bodyMetric(student.memberId(), pt.userId(), LocalDate.of(2026, 11, 15), "67");
        Long sameDaySecond = f.bodyMetric(student.memberId(), student.userId(), LocalDate.of(2026, 11, 15), "66.8");
        Member other = f.member(f.cn01);
        f.bodyMetric(other.memberId(), receptionist, LocalDate.of(2026, 11, 20), "80");

        history(receptionist, UserRole.RECEPTIONIST, student.memberId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].id").value(sameDaySecond))
            .andExpect(jsonPath("$[1].id").value(sameDayFirst))
            .andExpect(jsonPath("$[2].id").value(oldest))
            .andExpect(jsonPath("$[2].weight").value(70.0));
    }

    @Test
    @DisplayName("US-F2.5.1 BR-F9.12: hội viên xem chỉ số của mình → 200; của người khác → 404")
    void memberSeesOnlyOwn() throws Exception {
        f.bodyMetric(student.memberId(), receptionist, LocalDate.of(2026, 11, 1), "70");
        Member other = f.member(f.cn01);
        f.bodyMetric(other.memberId(), receptionist, LocalDate.of(2026, 11, 1), "80");

        history(student.userId(), UserRole.MEMBER, student.memberId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].weight").value(70.0));
        history(student.userId(), UserRole.MEMBER, other.memberId()).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US-F2.5.1 BR-F9.10: PT dạy xem được → 200; PT không dạy → 403; nhân viên chi nhánh khác → 404; chủ chuỗi → 200")
    void historyScopes() throws Exception {
        f.bodyMetric(student.memberId(), receptionist, LocalDate.of(2026, 11, 1), "70");

        history(pt.userId(), UserRole.TRAINER, student.memberId()).andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
        Trainer other = f.trainer(f.cn01);
        history(other.userId(), UserRole.TRAINER, student.memberId()).andExpect(status().isForbidden());
        Long otherReceptionist = f.staffUser(UserRole.RECEPTIONIST, f.cn02);
        history(otherReceptionist, UserRole.RECEPTIONIST, student.memberId()).andExpect(status().isNotFound());
        Long owner = f.staffUser(UserRole.SUPER_ADMIN, null);
        history(owner, UserRole.SUPER_ADMIN, student.memberId()).andExpect(status().isOk());
        mvc.perform(get("/api/v1/members/{id}/body-metrics", student.memberId())).andExpect(status().isUnauthorized());
    }
}
