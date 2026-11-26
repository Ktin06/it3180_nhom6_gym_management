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
 * Kiểm thử quan hệ "PT dạy hội viên" (BR-F9.10, VD-26 vấn đề 1, GĐ-2 của S2-20.1) qua {@code TrainerStudentAccess}.
 * Dùng API chỉ đọc {@code GET /api/v1/pt/members/{id}/workout-plans} làm đầu dò quyền, kiểm thử đủ các API sức khỏe
 * và tập luyện, cùng {@code GET /api/v1/pt/students}. SRS F9 chưa có mã AC nên tên hiển thị theo US-F9.4.1 và VD26.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class TrainerStudentAccessIntegrationTest {
    static final LocalDateTime PAST = LocalDateTime.of(2026, 11, 10, 9, 0);
    static final LocalDateTime FUTURE = LocalDateTime.of(2026, 11, 25, 9, 0);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;

    StudentFixtures f;
    Trainer pt;
    Trainer otherPt;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 10, 0));
        f = new StudentFixtures(jdbc, tokens);
        f.reset();
        pt = f.trainer(f.cn01);
        otherPt = f.trainer(f.cn01);
    }

    ResultActions probe(Trainer trainer, Long memberId) throws Exception {
        return mvc.perform(get("/api/v1/pt/members/{id}/workout-plans", memberId)
            .with(f.bearer(trainer.userId(), UserRole.TRAINER)));
    }

    @ParameterizedTest(name = "buổi PT {0}")
    @ValueSource(strings = {"BOOKED", "DONE", "NO_SHOW"})
    @DisplayName("US-F9.4.1 BR-F9.10: có buổi PT chưa hủy (BOOKED, DONE, NO_SHOW) với PT → PT dạy hội viên, 200")
    void ptSessionNotCancelledCounts(String status) throws Exception {
        Member m = f.member(f.cn01);
        f.ptSession(pt, m, "BOOKED".equals(status) ? FUTURE : PAST, status);

        probe(pt, m.memberId()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("US-F9.4.1 VD26-6: chỉ có buổi PT đã hủy → không dạy, cùng chi nhánh 403 và không có trong /pt/students")
    void cancelledPtSessionDoesNotCount() throws Exception {
        Member m = f.member(f.cn01);
        f.ptSession(pt, m, FUTURE, "CANCELLED");

        probe(pt, m.memberId())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Bạn chỉ thao tác được với học viên mình dạy."));
        mvc.perform(get("/api/v1/pt/students").with(f.bearer(pt.userId(), UserRole.TRAINER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @ParameterizedTest(name = "chỗ lớp {0}")
    @ValueSource(strings = {"BOOKED", "ATTENDED", "NO_SHOW"})
    @DisplayName("US-F9.4.1 VD26-1: chỉ dạy lớp nhóm (BOOKED, ATTENDED, NO_SHOW) không làm hội viên thành học viên PT → 403 và không có trong /pt/students")
    void classBookingDoesNotGrantAccess(String status) throws Exception {
        Member m = f.member(f.cn01);
        f.classBooking(pt, m, status);

        probe(pt, m.memberId())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Bạn chỉ thao tác được với học viên mình dạy."));
        mvc.perform(get("/api/v1/pt/students").with(f.bearer(pt.userId(), UserRole.TRAINER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("US-F9.4.1 VD26-2: dạy thay lớp nhóm cũng không cấp quyền xem dữ liệu sức khỏe → cả hai huấn luyện viên 403")
    void substituteClassTeacherDoesNotGrantAccess() throws Exception {
        Member m = f.member(f.cn01);
        f.classBooking(otherPt, pt, m, "ATTENDED");

        probe(pt, m.memberId()).andExpect(status().isForbidden());
        probe(otherPt, m.memberId()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-F9.4.1 VD26-4: huấn luyện viên chỉ dạy lớp nhóm có hội viên đặt chỗ → 403 mọi API xem/ghi giáo án, nhật ký, chỉ số, tiến độ và không có trong /pt/students; khác chi nhánh vẫn 404")
    void classOnlyStudentForbiddenAcrossAllHealthApis() throws Exception {
        Member sameBranch = f.member(f.cn01);
        f.classBooking(pt, sameBranch, "BOOKED");
        f.classBooking(pt, sameBranch, "ATTENDED");
        Member otherBranch = f.member(f.cn02);
        f.classBooking(pt, otherBranch, "ATTENDED");

        String planBody = """
            {"title": "Giáo án lớp", "content": [{"session": 1, "exercises": [{"name": "Squat"}]}]}""";
        String logBody = """
            {"loggedAt": "2026-11-21", "exercise": "Squat", "sets": 3, "reps": 10, "weight": 40}""";
        String metricBody = """
            {"measuredAt": "2026-11-21", "weight": 65.5}""";

        // Cùng chi nhánh, chỉ có chỗ lớp nhóm → 403 trên mọi API xem và ghi
        mvc.perform(get("/api/v1/pt/members/{id}/workout-plans", sameBranch.memberId())
                .with(f.bearer(pt.userId(), UserRole.TRAINER)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Bạn chỉ thao tác được với học viên mình dạy."));
        mvc.perform(post("/api/v1/pt/members/{id}/workout-plans", sameBranch.memberId())
                .with(f.bearer(pt.userId(), UserRole.TRAINER))
                .contentType(MediaType.APPLICATION_JSON).content(planBody))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pt/members/{id}/workout-logs", sameBranch.memberId())
                .with(f.bearer(pt.userId(), UserRole.TRAINER))
                .contentType(MediaType.APPLICATION_JSON).content(logBody))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/members/{id}/body-metrics", sameBranch.memberId())
                .with(f.bearer(pt.userId(), UserRole.TRAINER)))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/members/{id}/body-metrics", sameBranch.memberId())
                .with(f.bearer(pt.userId(), UserRole.TRAINER))
                .contentType(MediaType.APPLICATION_JSON).content(metricBody))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/members/{id}/progress", sameBranch.memberId())
                .with(f.bearer(pt.userId(), UserRole.TRAINER)))
            .andExpect(status().isForbidden());

        // Khác chi nhánh, chỉ có chỗ lớp nhóm → giữ nguyên mã 404 (VD-26 vấn đề 2)
        probe(pt, otherBranch.memberId()).andExpect(status().isNotFound());

        // Không có trong danh sách học viên PT và không ghi dữ liệu nào
        mvc.perform(get("/api/v1/pt/students").with(f.bearer(pt.userId(), UserRole.TRAINER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
        assertThat(f.count("workout_plans")).isZero();
        assertThat(f.count("workout_logs")).isZero();
        assertThat(f.count("body_metrics")).isZero();
    }

    @Test
    @DisplayName("US-F9.4.1 VD26-5: có buổi PT BOOKED chưa diễn ra → có trong /pt/students (viaClass=false) và được xem/ghi giáo án, nhật ký, chỉ số, tiến độ")
    void futureBookedPtSessionAllowsAccessAndListsStudent() throws Exception {
        Member m = f.member(f.cn01);
        f.ptSession(pt, m, FUTURE, "BOOKED");

        mvc.perform(get("/api/v1/pt/students").with(f.bearer(pt.userId(), UserRole.TRAINER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].memberId").value(m.memberId()))
            .andExpect(jsonPath("$[0].viaPt").value(true))
            .andExpect(jsonPath("$[0].viaClass").value(false));

        String planBody = """
            {"title": "Giáo án trước buổi đầu", "content": [{"session": 1, "exercises": [{"name": "Squat"}]}]}""";
        String logBody = """
            {"loggedAt": "2026-11-21", "exercise": "Squat", "sets": 3, "reps": 10, "weight": 40}""";
        String metricBody = """
            {"measuredAt": "2026-11-21", "weight": 65.5}""";

        mvc.perform(post("/api/v1/pt/members/{id}/workout-plans", m.memberId())
                .with(f.bearer(pt.userId(), UserRole.TRAINER))
                .contentType(MediaType.APPLICATION_JSON).content(planBody))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/pt/members/{id}/workout-plans", m.memberId())
                .with(f.bearer(pt.userId(), UserRole.TRAINER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(post("/api/v1/pt/members/{id}/workout-logs", m.memberId())
                .with(f.bearer(pt.userId(), UserRole.TRAINER))
                .contentType(MediaType.APPLICATION_JSON).content(logBody))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/members/{id}/body-metrics", m.memberId())
                .with(f.bearer(pt.userId(), UserRole.TRAINER))
                .contentType(MediaType.APPLICATION_JSON).content(metricBody))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/members/{id}/body-metrics", m.memberId())
                .with(f.bearer(pt.userId(), UserRole.TRAINER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/members/{id}/progress", m.memberId())
                .with(f.bearer(pt.userId(), UserRole.TRAINER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bodyMetrics", hasSize(1)))
            .andExpect(jsonPath("$.workoutLogs", hasSize(1)));
    }

    @Test
    @DisplayName("US-F9.4.1 BR-F9.10: buổi PT và chỗ lớp của PT khác không làm PT này thành người dạy → 403")
    void otherTrainersStudentIsForbidden() throws Exception {
        Member m = f.member(f.cn01);
        f.ptSession(otherPt, m, PAST, "DONE");
        f.classBooking(otherPt, m, "ATTENDED");

        probe(pt, m.memberId()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-F9.4.1 E3: hội viên không tồn tại hoặc đã xóa mềm → 404")
    void missingOrSoftDeletedIs404() throws Exception {
        probe(pt, 999_999L).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Không tìm thấy hội viên."));

        Member m = f.member(f.cn01);
        f.ptSession(pt, m, PAST, "DONE");
        f.softDelete(m.memberId());
        probe(pt, m.memberId()).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US-F9.4.1 GĐ-2: hội viên chi nhánh khác mà PT không dạy → 404 (không lộ hồ sơ ngoài chi nhánh)")
    void otherBranchNotTaughtIs404() throws Exception {
        Member m = f.member(f.cn02);

        probe(pt, m.memberId()).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US-F9.4.1 GĐ-2: hội viên chi nhánh khác có buổi PT với PT (gói toàn chuỗi) → vẫn là học viên, 200")
    void otherBranchButTaughtIsAllowed() throws Exception {
        Member m = f.member(f.cn02);
        f.ptSession(pt, m, PAST, "DONE");

        probe(pt, m.memberId()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("US-F9.4.1 VD26-3: danh sách học viên chỉ gồm người có buổi PT chưa hủy (viaClass luôn false); bỏ người chỉ có lớp nhóm, buổi hủy, của PT khác, đã xóa mềm")
    void myStudents() throws Exception {
        Member viaPt = f.member(f.cn01);
        f.ptSession(pt, viaPt, PAST, "DONE");
        f.ptSession(pt, viaPt, FUTURE, "BOOKED");
        Member viaClassOnly = f.member(f.cn01);
        f.classBooking(pt, viaClassOnly, "BOOKED");
        Member both = f.member(f.cn02);
        f.ptSession(pt, both, PAST, "NO_SHOW");
        f.classBooking(pt, both, "ATTENDED");
        Member cancelledOnly = f.member(f.cn01);
        f.ptSession(pt, cancelledOnly, FUTURE, "CANCELLED");
        f.classBooking(pt, cancelledOnly, "ATTENDED");
        Member others = f.member(f.cn01);
        f.ptSession(otherPt, others, PAST, "DONE");
        Member deleted = f.member(f.cn01);
        f.ptSession(pt, deleted, PAST, "DONE");
        f.softDelete(deleted.memberId());
        f.rename(viaPt.memberId(), "An");
        f.rename(viaClassOnly.memberId(), "Bình");
        f.rename(both.memberId(), "Chi");

        mvc.perform(get("/api/v1/pt/students").with(f.bearer(pt.userId(), UserRole.TRAINER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].memberId").value(viaPt.memberId()))
            .andExpect(jsonPath("$[0].viaPt").value(true))
            .andExpect(jsonPath("$[0].viaClass").value(false))
            .andExpect(jsonPath("$[0].memberCode").isString())
            .andExpect(jsonPath("$[0].fullName").isString())
            .andExpect(jsonPath("$[1].memberId").value(both.memberId()))
            .andExpect(jsonPath("$[1].homeBranchId").value(f.cn02))
            .andExpect(jsonPath("$[1].viaPt").value(true))
            .andExpect(jsonPath("$[1].viaClass").value(false));
    }

    @Test
    @DisplayName("US-F9.4.1 KB1: PT chưa dạy ai → danh sách rỗng; vai trò khác PT → 403; chưa đăng nhập → 401")
    void studentsEmptyAndRoles() throws Exception {
        mvc.perform(get("/api/v1/pt/students").with(f.bearer(pt.userId(), UserRole.TRAINER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
        Long receptionist = f.staffUser(UserRole.RECEPTIONIST, f.cn01);
        mvc.perform(get("/api/v1/pt/students").with(f.bearer(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/pt/students")).andExpect(status().isUnauthorized());
    }
}
