package vn.edu.hust.nhom6.gym.feature.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.vn;

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

import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.Account;
import vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.Trainer;
import vn.edu.hust.nhom6.gym.feature.trainer.event.PtSessionCancelledByManagerEvent;

/**
 * Kiểm thử tích hợp API hủy buổi PT {@code POST /api/v1/pt-sessions/{id}/cancel} (F9, UC-F9.7, Q13, S2-19.2). SRS F9
 * chưa có mã AC nên tên hiển thị theo US-F9.7.1 và luồng của SRS.
 *
 * <p>Mốc mặc định: thứ Sáu 20/11/2026 06:00 giờ Việt Nam. Buổi mặc định: 20/11 18:00 (còn đúng 12 giờ).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class, PtSessionEventProbe.class})
class PtSessionCancelApiIntegrationTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 6, 0);
    static final LocalDateTime AT_18 = LocalDateTime.of(2026, 11, 20, 18, 0);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired JsonMapper json;
    @Autowired PtSessionEventProbe.Recorder events;

    PtSessionFixtures f;
    Trainer pt;
    Account owner;
    Long membership;
    Long receptionist;
    Long manager;

    @BeforeEach
    void setUp() {
        clock.setVn(NOW);
        f = new PtSessionFixtures(jdbc, tokens);
        f.reset();
        events.reset();
        pt = f.trainer(f.cn01);
        owner = f.ptMember(10);
        membership = f.ptMembershipOf(owner.memberId());
        receptionist = f.staffUser(UserRole.RECEPTIONIST, f.cn01);
        manager = f.staffUser(UserRole.BRANCH_MANAGER, f.cn01);
    }

    Long session(LocalDateTime vnStart, String status) {
        return f.ptSession(pt.staffId(), owner.memberId(), membership, vnStart, status);
    }

    ResultActions cancel(Long userId, UserRole role, Long sessionId, String reason) throws Exception {
        var request = post("/api/v1/pt-sessions/{id}/cancel", sessionId).with(f.bearer(userId, role));
        if (reason != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("reason", reason)));
        }
        return mvc.perform(request);
    }

    long auditRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE entity = 'pt_sessions'", Long.class);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Hội viên, lễ tân (BR-F9.16)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US-F9.7.1 KB: hội viên hủy buổi của mình khi còn đúng 12 giờ → 200 CANCELLED, cancelled_at, không trừ buổi, giờ đó đặt lại được")
    void memberCancelsAtExactlyTwelveHours() throws Exception {
        Long id = session(AT_18, "BOOKED");

        cancel(owner.userId(), UserRole.MEMBER, id, null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.cancelledAt").value("2026-11-19T23:00:00Z"));

        assertThat(f.status(id)).isEqualTo("CANCELLED");
        assertThat(f.remaining(membership)).isEqualTo(10);
        assertThat(auditRows()).as("hội viên hủy không ghi audit_logs (GĐ-5)").isZero();
        assertThat(events.events()).isEmpty();

        Account other = f.ptMember(10);
        mvc.perform(post("/api/v1/pt-sessions").with(f.bearer(other.userId(), UserRole.MEMBER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("trainerId", pt.staffId(),
                    "startAt", vn(AT_18).toString()))))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("US-F9.7.1 E4: hội viên hủy khi còn 11 giờ 59 phút → 422, buổi vẫn BOOKED")
    void memberCancelsTooLate() throws Exception {
        Long id = session(AT_18, "BOOKED");
        clock.setVn(NOW.plusMinutes(1));
        cancel(owner.userId(), UserRole.MEMBER, id, null)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail", containsString("ít nhất 12 giờ")));
        assertThat(f.status(id)).isEqualTo("BOOKED");
    }

    @Test
    @DisplayName("US-F9.7.1 KB: lễ tân cùng chi nhánh hủy hộ khi còn ≥ 12 giờ → 200; còn < 12 giờ → 422")
    void receptionistCancelsOnBehalf() throws Exception {
        Long early = session(AT_18, "BOOKED");
        Long late = session(AT_18.minusHours(2), "BOOKED");
        cancel(receptionist, UserRole.RECEPTIONIST, early, null).andExpect(status().isOk());
        cancel(receptionist, UserRole.RECEPTIONIST, late, null).andExpect(status().isUnprocessableContent());
        assertThat(f.status(early)).isEqualTo("CANCELLED");
        assertThat(f.status(late)).isEqualTo("BOOKED");
    }

    @Test
    @DisplayName("US-F9.7.1 CONFIG-F9.1: mốc hủy đọc PT_MIN_LEAD_HOURS của chi nhánh buổi (CN01 = 24) → còn 12 giờ bị 422")
    void cancelLeadReadsBranchConfig() throws Exception {
        f.branchConfig(f.cn01, "PT_MIN_LEAD_HOURS", 24);
        Long id = session(AT_18, "BOOKED");
        cancel(owner.userId(), UserRole.MEMBER, id, null)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail", containsString("24 giờ")));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Quản lý chi nhánh (BR-F9.17)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US-F9.7.1 KB: quản lý chi nhánh hủy hộ khi còn 1 giờ, có lý do → 200, ghi audit_logs, phát sự kiện cho F10 sau commit")
    void managerCancelsWithReason() throws Exception {
        Long id = session(LocalDateTime.of(2026, 11, 20, 7, 0), "BOOKED");

        cancel(manager, UserRole.BRANCH_MANAGER, id, "  PT ốm đột xuất  ")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(f.remaining(membership)).isEqualTo(10);
        var audit = jdbc.queryForMap("""
            SELECT user_id, branch_id, action, entity_id, old_value->>'status' AS old_status,
                   new_value->>'status' AS new_status, new_value->>'reason' AS reason
            FROM audit_logs WHERE entity = 'pt_sessions'""");
        assertThat(audit.get("user_id")).isEqualTo(manager);
        assertThat(audit.get("branch_id")).isEqualTo(f.cn01);
        assertThat(audit.get("action")).isEqualTo("CANCEL");
        assertThat(audit.get("entity_id")).isEqualTo(id);
        assertThat(audit.get("old_status")).isEqualTo("BOOKED");
        assertThat(audit.get("new_status")).isEqualTo("CANCELLED");
        assertThat(audit.get("reason")).isEqualTo("PT ốm đột xuất");

        assertThat(events.events()).singleElement().satisfies(e -> {
            assertThat(e.sessionId()).isEqualTo(id);
            assertThat(e.memberId()).isEqualTo(owner.memberId());
            assertThat(e.reason()).isEqualTo("PT ốm đột xuất");
            assertThat(e.trainerId()).isEqualTo(pt.staffId());
            assertThat(e.branchId()).isEqualTo(f.cn01);
            assertThat(e.startAt().toInstant()).isEqualTo(vn(LocalDateTime.of(2026, 11, 20, 7, 0)).toInstant());
            assertThat(e.cancelledBy()).isEqualTo(manager);
        });
    }

    @ParameterizedTest(name = "lý do = [{0}]")
    @ValueSource(strings = {"", "   "})
    @DisplayName("US-F9.7.1 E6: quản lý hủy hộ không nhập lý do (rỗng, toàn khoảng trắng) → 400")
    void managerBlankReason(String reason) throws Exception {
        Long id = session(AT_18, "BOOKED");
        cancel(manager, UserRole.BRANCH_MANAGER, id, reason).andExpect(status().isBadRequest());
        assertThat(f.status(id)).isEqualTo("BOOKED");
        assertThat(auditRows()).isZero();
        assertThat(events.events()).isEmpty();
    }

    @Test
    @DisplayName("US-F9.7.1 E6: quản lý hủy hộ không gửi body → 400")
    void managerNoBody() throws Exception {
        Long id = session(AT_18, "BOOKED");
        cancel(manager, UserRole.BRANCH_MANAGER, id, null).andExpect(status().isBadRequest());
        assertThat(f.status(id)).isEqualTo("BOOKED");
    }

    @Test
    @DisplayName("US-F9.7.1 E5: quản lý hủy khi đã tới giờ bắt đầu → 422; còn 1 phút → 200")
    void managerAtStartTime() throws Exception {
        Long started = session(NOW, "BOOKED");
        Long soon = session(NOW.plusMinutes(1), "BOOKED");
        cancel(manager, UserRole.BRANCH_MANAGER, started, "PT ốm")
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail", containsString("đã bắt đầu")));
        cancel(manager, UserRole.BRANCH_MANAGER, soon, "PT ốm").andExpect(status().isOk());
        assertThat(f.status(started)).isEqualTo("BOOKED");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Quyền, không tìm thấy, trạng thái
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US-F9.7.1 E1: huấn luyện viên (kể cả PT của buổi) và chủ chuỗi gọi API hủy → 403")
    void trainerAndOwnerForbidden() throws Exception {
        Long id = session(AT_18, "BOOKED");
        cancel(pt.userId(), UserRole.TRAINER, id, "bận").andExpect(status().isForbidden());
        Long superAdmin = f.staffUser(UserRole.SUPER_ADMIN, null);
        cancel(superAdmin, UserRole.SUPER_ADMIN, id, "bận").andExpect(status().isForbidden());
        assertThat(f.status(id)).isEqualTo("BOOKED");
    }

    @Test
    @DisplayName("US-F9.7.1 E1: hội viên hủy buổi của hội viên khác → 403")
    void memberCannotCancelOthers() throws Exception {
        Long id = session(AT_18, "BOOKED");
        Account stranger = f.ptMember(10);
        cancel(stranger.userId(), UserRole.MEMBER, id, null).andExpect(status().isForbidden());
        assertThat(f.status(id)).isEqualTo("BOOKED");
    }

    @Test
    @DisplayName("US-F9.7.1 E1: lễ tân, quản lý chi nhánh khác hủy → 403")
    void otherBranchStaffForbidden() throws Exception {
        Long id = session(AT_18, "BOOKED");
        Long receptionistCn02 = f.staffUser(UserRole.RECEPTIONIST, f.cn02);
        Long managerCn02 = f.staffUser(UserRole.BRANCH_MANAGER, f.cn02);
        cancel(receptionistCn02, UserRole.RECEPTIONIST, id, null).andExpect(status().isForbidden());
        cancel(managerCn02, UserRole.BRANCH_MANAGER, id, "PT ốm").andExpect(status().isForbidden());
        assertThat(f.status(id)).isEqualTo("BOOKED");
        assertThat(auditRows()).isZero();
    }

    @Test
    @DisplayName("US-F9.7.1 E2: buổi không tồn tại → 404")
    void notFound() throws Exception {
        cancel(owner.userId(), UserRole.MEMBER, 999_999L, null).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US-F9.7.1 E1, E2: quản lý không gửi lý do cho buổi không tồn tại → 404, buổi chi nhánh khác → 403 (quyền xét trước lý do)")
    void managerWithoutReasonGetsNotFoundOrForbiddenFirst() throws Exception {
        cancel(manager, UserRole.BRANCH_MANAGER, 999_999L, null).andExpect(status().isNotFound());
        Long id = session(AT_18, "BOOKED");
        Long managerCn02 = f.staffUser(UserRole.BRANCH_MANAGER, f.cn02);
        cancel(managerCn02, UserRole.BRANCH_MANAGER, id, null).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-F9.7.1 E4: lễ tân hủy sát giờ nhận câu báo không có 'liên hệ quầy'; hội viên thì có")
    void lateCancelMessageDependsOnRole() throws Exception {
        Long id = session(AT_18.minusHours(2), "BOOKED");
        cancel(receptionist, UserRole.RECEPTIONIST, id, null)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Chỉ được hủy buổi PT trước giờ bắt đầu ít nhất 12 giờ."));
        cancel(owner.userId(), UserRole.MEMBER, id, null)
            .andExpect(jsonPath("$.detail", containsString("Vui lòng liên hệ quầy")));
    }

    @Test
    @DisplayName("US-F9.7.1 E3: buổi DONE, CANCELLED, NO_SHOW → 422 'không còn ở trạng thái đã đặt'")
    void notBooked() throws Exception {
        for (String state : new String[] {"DONE", "CANCELLED", "NO_SHOW"}) {
            Long id = session(AT_18.plusDays(1), state);
            cancel(owner.userId(), UserRole.MEMBER, id, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail", containsString("không còn ở trạng thái đã đặt")));
            cancel(manager, UserRole.BRANCH_MANAGER, id, "PT ốm").andExpect(status().isUnprocessableContent());
            assertThat(f.status(id)).isEqualTo(state);
        }
        assertThat(auditRows()).isZero();
    }
}
