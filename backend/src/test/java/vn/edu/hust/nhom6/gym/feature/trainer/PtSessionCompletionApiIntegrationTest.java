package vn.edu.hust.nhom6.gym.feature.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.vn;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.Account;
import vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.Trainer;
import vn.edu.hust.nhom6.gym.feature.trainer.event.PtSessionCancelledByPackageEndEvent;

/**
 * Kiểm thử tích hợp API xác nhận buổi PT {@code PATCH /api/v1/pt-sessions/{id}/confirm} và ghi vắng
 * {@code PATCH /api/v1/pt-sessions/{id}/no-show} (F9, UC-F9.3, BR-F9.6–F9.9, BR-F9.18, BR-F9.19, VD-21 GĐ-8; S2-19.3).
 * SRS F9 chưa có mã AC nên tên hiển thị theo US-F9.3.1 và luồng của SRS.
 *
 * <p>Mốc mặc định (ví dụ của SRS): thứ Ba 03/11/2026 19:30 giờ Việt Nam. Buổi mặc định: 03/11 18:00. Gói A: PT CN01
 * 01/10–30/11, 10 buổi, hóa đơn 5.000.000 giảm 500.000; huấn luyện viên tỷ lệ 0,1000.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class, PtPackageEndEventProbe.class})
class PtSessionCompletionApiIntegrationTest {
    static final LocalDate DAY = LocalDate.of(2026, 11, 3);
    static final LocalDateTime NOW = DAY.atTime(19, 30);
    static final LocalDateTime AT_18 = DAY.atTime(18, 0);
    static final LocalDate A_START = LocalDate.of(2026, 10, 1);
    static final LocalDate A_END = LocalDate.of(2026, 11, 30);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired PtPackageEndEventProbe.Recorder events;

    PtCompletionFixtures c;
    Trainer pt;
    Account owner;

    @BeforeEach
    void setUp() {
        clock.setVn(NOW);
        c = new PtCompletionFixtures(jdbc, tokens);
        c.reset();
        events.reset();
        pt = c.trainer("0.1000");
        owner = c.member();
    }

    /** Gói A ACTIVE còn {@code remaining} buổi. */
    Long packageA(int remaining) {
        return c.soldPt(owner, "ACTIVE", A_START, A_END, 10, remaining, 5_000_000, 500_000);
    }

    /** Gói B 60 ngày, UPCOMING từ 01/12 (ví dụ SRS), 10 buổi. */
    Long packageB(int remaining) {
        return c.soldPt(owner, "UPCOMING", LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 29), 10, remaining,
            6_000_000, 0);
    }

    ResultActions confirm(Long userId, UserRole role, Long sessionId) throws Exception {
        return mvc.perform(patch("/api/v1/pt-sessions/{id}/confirm", sessionId).with(c.f.bearer(userId, role)));
    }

    ResultActions noShow(Long userId, UserRole role, Long sessionId) throws Exception {
        return mvc.perform(patch("/api/v1/pt-sessions/{id}/no-show", sessionId).with(c.f.bearer(userId, role)));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Luồng chính, hoa hồng (BR-F9.6, BR-F9.8)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US-F9.3.1 KB1: PT xác nhận buổi đã diễn ra → 200 DONE, confirmed_at, trừ 1 buổi, hoa hồng 0,1 × 4.500.000 ÷ 10 = 45.000")
    void confirmSuccess() throws Exception {
        Long a = packageA(5);
        Long id = c.session(pt, owner, a, AT_18);

        confirm(pt.userId(), UserRole.TRAINER, id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DONE"))
            .andExpect(jsonPath("$.confirmedAt").value("2026-11-03T12:30:00Z"))
            .andExpect(jsonPath("$.commissionAmount").value(45_000))
            .andExpect(jsonPath("$.remainingSessions").value(4))
            .andExpect(jsonPath("$.membershipStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.activatedMembershipId").value(nullValue()));

        assertThat(c.ptSession(id)).containsEntry("status", "DONE").containsEntry("commission_amount", 45_000);
        assertThat(c.membership(a)).containsEntry("remaining_sessions", 4).containsEntry("status", "ACTIVE");
        assertThat(c.auditRows("CONFIRM")).as("PT tự xác nhận trong hạn không ghi audit_logs").isZero();
    }

    @Test
    @DisplayName("US-F9.3.1 KB2: hoa hồng làm tròn HALF_UP một lần: 0,1 × 1.000.050 ÷ 2 = 50.002,5 → 50.003; đổi tỷ lệ sau khi chốt không đổi số đã chốt")
    void commissionRoundsHalfUpAndIsFrozen() throws Exception {
        Long a = c.soldPt(owner, "ACTIVE", A_START, A_END, 2, 2, 1_000_050, 0);
        Long id = c.session(pt, owner, a, AT_18);

        confirm(pt.userId(), UserRole.TRAINER, id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.commissionAmount").value(50_003));

        jdbc.update("UPDATE staff SET commission_rate = 0.5 WHERE id = ?", pt.staffId());
        jdbc.update("UPDATE invoice_items SET discount_amount = 1000000");
        assertThat(c.ptSession(id)).containsEntry("commission_amount", 50_003);
    }

    @Test
    @DisplayName("US-F9.3.1 KB2: hoa hồng dùng giá sau giảm của dòng hóa đơn: 0,15 × (3.000.000 − 1.000.000) ÷ 12 = 25.000")
    void commissionUsesNetPrice() throws Exception {
        Trainer pt15 = c.trainer("0.1500");
        Long a = c.soldPt(owner, "ACTIVE", A_START, A_END, 12, 12, 3_000_000, 1_000_000);
        Long id = c.session(pt15, owner, a, AT_18);

        confirm(pt15.userId(), UserRole.TRAINER, id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.commissionAmount").value(25_000));
    }

    @Test
    @DisplayName("US-F9.3.1 KB2: huấn luyện viên chưa có tỷ lệ hoa hồng → DONE, hoa hồng 0 (GĐ-3), vẫn trừ buổi")
    void nullRateGivesZeroCommission() throws Exception {
        Trainer noRate = c.trainer(null);
        Long a = packageA(5);
        Long id = c.session(noRate, owner, a, AT_18);

        confirm(noRate.userId(), UserRole.TRAINER, id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.commissionAmount").value(0));
        assertThat(c.membership(a)).containsEntry("remaining_sessions", 4);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Buổi cuối (BR-F9.7) và ghi vắng (BR-F9.18)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US-F9.3.1 KB3 (ví dụ SRS): xác nhận buổi cuối của gói A ngày 03/11 → A EXPIRED 0 buổi; gói B 60 ngày dời về 03/11–01/01 và ACTIVE")
    void lastSessionExpiresAndPullsNextPackage() throws Exception {
        Long a = packageA(1);
        Long b = packageB(10);
        Long id = c.session(pt, owner, a, AT_18);

        confirm(pt.userId(), UserRole.TRAINER, id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.remainingSessions").value(0))
            .andExpect(jsonPath("$.membershipStatus").value("EXPIRED"))
            .andExpect(jsonPath("$.activatedMembershipId").value(b));

        assertThat(c.membership(a)).containsEntry("status", "EXPIRED").containsEntry("remaining_sessions", 0);
        assertThat(c.membership(b))
            .containsEntry("status", "ACTIVE")
            .containsEntry("start_date", Date.valueOf(DAY))
            .containsEntry("end_date", Date.valueOf(LocalDate.of(2027, 1, 1)))
            .containsEntry("remaining_sessions", 10);
    }

    @Test
    @DisplayName("US-F9.3.1 KB3: gói nối tiếp đã tới ngày bắt đầu (01/11) mà tác vụ nền chưa kích hoạt → ACTIVE, giữ nguyên 01/11–30/12, không dời muộn hơn")
    void overdueUpcomingKeepsItsDates() throws Exception {
        Long a = c.soldPt(owner, "ACTIVE", A_START, LocalDate.of(2026, 10, 31), 10, 1, 5_000_000, 500_000);
        Long b = c.soldPt(owner, "UPCOMING", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 30), 10, 10,
            6_000_000, 0);
        Long id = c.session(pt, owner, a, LocalDateTime.of(2026, 10, 31, 18, 0));
        clock.setVn(LocalDate.of(2026, 11, 3).atTime(8, 0));

        confirm(c.manager, UserRole.BRANCH_MANAGER, id).andExpect(status().isOk());

        assertThat(c.membership(b)).containsEntry("status", "ACTIVE")
            .containsEntry("start_date", Date.valueOf(LocalDate.of(2026, 11, 1)))
            .containsEntry("end_date", Date.valueOf(LocalDate.of(2026, 12, 30)));
    }

    @Test
    @DisplayName("US-F9.3.1 KB3: buổi cuối, không có gói nối tiếp → gói EXPIRED, không kích hoạt gì")
    void lastSessionWithoutNextPackage() throws Exception {
        Long a = packageA(1);
        Long id = c.session(pt, owner, a, AT_18);

        confirm(pt.userId(), UserRole.TRAINER, id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.membershipStatus").value("EXPIRED"))
            .andExpect(jsonPath("$.activatedMembershipId").value(nullValue()));
        assertThat(c.membership(a)).containsEntry("status", "EXPIRED");
    }

    @Test
    @DisplayName("US-F9.3.1 BR-F9.18: PT ghi vắng sau giờ bắt đầu → NO_SHOW, vẫn trừ 1 buổi, commission_amount rỗng")
    void noShowDeductsWithoutCommission() throws Exception {
        Long a = packageA(5);
        Long id = c.session(pt, owner, a, AT_18);

        noShow(pt.userId(), UserRole.TRAINER, id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NO_SHOW"))
            .andExpect(jsonPath("$.commissionAmount").value(nullValue()))
            .andExpect(jsonPath("$.confirmedAt").value("2026-11-03T12:30:00Z"))
            .andExpect(jsonPath("$.remainingSessions").value(4));

        Map<String, Object> row = c.ptSession(id);
        assertThat(row).containsEntry("status", "NO_SHOW");
        assertThat(row.get("commission_amount")).isNull();
        assertThat(c.membership(a)).containsEntry("remaining_sessions", 4);
    }

    @Test
    @DisplayName("US-F9.3.1 BR-F9.18: ghi vắng buổi cuối → áp dụng BR-F9.7 như DONE: A EXPIRED, B về hôm nay và ACTIVE")
    void noShowOnLastSessionPullsNextPackage() throws Exception {
        Long a = packageA(1);
        Long b = packageB(10);
        Long id = c.session(pt, owner, a, AT_18);

        noShow(pt.userId(), UserRole.TRAINER, id).andExpect(status().isOk());

        assertThat(c.membership(a)).containsEntry("status", "EXPIRED");
        assertThat(c.membership(b)).containsEntry("status", "ACTIVE").containsEntry("start_date", Date.valueOf(DAY));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // E2, E3, E5
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US-F9.3.1 E2: PT không được phân công xác nhận hoặc ghi vắng → 403, buổi và gói giữ nguyên")
    void otherTrainerForbidden() throws Exception {
        Long a = packageA(5);
        Long id = c.session(pt, owner, a, AT_18);
        Trainer other = c.trainer("0.1000");

        confirm(other.userId(), UserRole.TRAINER, id).andExpect(status().isForbidden());
        noShow(other.userId(), UserRole.TRAINER, id).andExpect(status().isForbidden());
        assertThat(c.ptSession(id)).containsEntry("status", "BOOKED");
        assertThat(c.membership(a)).containsEntry("remaining_sessions", 5);
    }

    @Test
    @DisplayName("US-F9.3.1 E2: quản lý chi nhánh khác → 403")
    void otherBranchManagerForbidden() throws Exception {
        Long id = c.session(pt, owner, packageA(5), AT_18);
        Long otherManager = c.f.staffUser(UserRole.BRANCH_MANAGER, c.f.cn02);

        confirm(otherManager, UserRole.BRANCH_MANAGER, id).andExpect(status().isForbidden());
        assertThat(c.ptSession(id)).containsEntry("status", "BOOKED");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"MEMBER", "RECEPTIONIST", "SUPER_ADMIN"})
    @DisplayName("US-F9.3.1 E2: hội viên, lễ tân, chủ chuỗi không xác nhận được → 403")
    void otherRolesForbidden(String role) throws Exception {
        Long id = c.session(pt, owner, packageA(5), AT_18);
        Long userId = switch (role) {
            case "MEMBER" -> owner.userId();
            case "SUPER_ADMIN" -> c.f.staffUser(UserRole.SUPER_ADMIN, null);
            default -> c.f.staffUser(UserRole.valueOf(role), c.f.cn01);
        };

        confirm(userId, UserRole.valueOf(role), id).andExpect(status().isForbidden());
        noShow(userId, UserRole.valueOf(role), id).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-F9.3.1 E3: buổi không tồn tại → 404")
    void notFound() throws Exception {
        confirm(pt.userId(), UserRole.TRAINER, 999_999L)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Buổi tập không tồn tại."));
    }

    @Test
    @DisplayName("US-F9.3.1 E5 (Q11): chưa tới giờ bắt đầu → 422 \"Buổi tập chưa diễn ra.\" cho cả xác nhận và ghi vắng; đúng giờ bắt đầu → 200")
    void beforeStartRejected() throws Exception {
        Long a = packageA(5);
        Long id = c.session(pt, owner, a, AT_18);
        clock.setVn(AT_18.minusMinutes(1));

        confirm(pt.userId(), UserRole.TRAINER, id)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Buổi tập chưa diễn ra."));
        noShow(pt.userId(), UserRole.TRAINER, id).andExpect(status().isUnprocessableContent());
        confirm(c.manager, UserRole.BRANCH_MANAGER, id).andExpect(status().isUnprocessableContent());
        assertThat(c.membership(a)).containsEntry("remaining_sessions", 5);

        clock.setVn(AT_18);
        confirm(pt.userId(), UserRole.TRAINER, id).andExpect(status().isOk());
    }

    @Test
    @DisplayName("US-F9.3.1 E5: xác nhận lần hai, hoặc ghi vắng buổi đã xác nhận → 422, không trừ lần hai")
    void secondTimeRejected() throws Exception {
        Long a = packageA(5);
        Long id = c.session(pt, owner, a, AT_18);
        confirm(pt.userId(), UserRole.TRAINER, id).andExpect(status().isOk());

        confirm(pt.userId(), UserRole.TRAINER, id)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail", containsString("không trừ buổi lần hai")));
        noShow(pt.userId(), UserRole.TRAINER, id).andExpect(status().isUnprocessableContent());
        confirm(c.manager, UserRole.BRANCH_MANAGER, id).andExpect(status().isUnprocessableContent());

        assertThat(c.membership(a)).containsEntry("remaining_sessions", 4);
        assertThat(c.ptSession(id)).containsEntry("status", "DONE").containsEntry("commission_amount", 45_000);
    }

    @Test
    @DisplayName("US-F9.3.1 E5: buổi đã hủy → 422, gói giữ nguyên")
    void cancelledRejected() throws Exception {
        Long a = packageA(5);
        Long id = c.f.ptSession(pt.staffId(), owner.memberId(), a, AT_18, "CANCELLED");

        confirm(pt.userId(), UserRole.TRAINER, id)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Buổi PT đã bị hủy."));
        assertThat(c.membership(a)).containsEntry("remaining_sessions", 5);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Hạn thao tác (BR-F9.19)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US-F9.3.1 BR-F9.19: PT xác nhận lúc 23:59 cùng ngày giờ Việt Nam → 200")
    void trainerAt2359Allowed() throws Exception {
        Long id = c.session(pt, owner, packageA(5), AT_18);
        clock.setVn(DAY.atTime(23, 59, 59));

        confirm(pt.userId(), UserRole.TRAINER, id).andExpect(status().isOk());
        assertThat(c.auditRows("CONFIRM")).isZero();
    }

    @Test
    @DisplayName("US-F9.3.1 BR-F9.19: PT xác nhận hoặc ghi vắng lúc 00:00 hôm sau → 422 \"Đã quá hạn xác nhận, liên hệ quản lý chi nhánh\"")
    void trainerAtMidnightRejected() throws Exception {
        Long a = packageA(5);
        Long id = c.session(pt, owner, a, AT_18);
        clock.setVn(DAY.plusDays(1).atStartOfDay());

        confirm(pt.userId(), UserRole.TRAINER, id)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Đã quá hạn xác nhận, liên hệ quản lý chi nhánh"));
        noShow(pt.userId(), UserRole.TRAINER, id)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Đã quá hạn xác nhận, liên hệ quản lý chi nhánh"));
        assertThat(c.membership(a)).containsEntry("remaining_sessions", 5);
    }

    @Test
    @DisplayName("US-F9.3.1 BR-F9.19: quá hạn, quản lý chi nhánh của buổi xác nhận lúc 00:00 hôm sau → 200, hoa hồng của PT, ghi audit_logs CONFIRM")
    void managerAfterDeadlineAllowedWithAudit() throws Exception {
        Long a = packageA(5);
        Long id = c.session(pt, owner, a, AT_18);
        clock.setVn(DAY.plusDays(1).atStartOfDay());

        confirm(c.manager, UserRole.BRANCH_MANAGER, id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DONE"))
            .andExpect(jsonPath("$.commissionAmount").value(45_000));

        assertThat(c.membership(a)).containsEntry("remaining_sessions", 4);
        Map<String, Object> audit = jdbc.queryForMap("""
            SELECT user_id, branch_id, entity_id, old_value::text AS old_value, new_value::text AS new_value
            FROM audit_logs WHERE entity = 'pt_sessions' AND action = 'CONFIRM'""");
        assertThat(audit).containsEntry("user_id", c.manager).containsEntry("branch_id", c.f.cn01)
            .containsEntry("entity_id", id);
        assertThat((String) audit.get("old_value")).contains("BOOKED");
        assertThat((String) audit.get("new_value")).contains("DONE").contains("45000");
    }

    @Test
    @DisplayName("US-F9.3.1 BR-F9.19: quá hạn, quản lý ghi vắng → 200 NO_SHOW, trừ buổi, ghi audit_logs NO_SHOW")
    void managerNoShowAfterDeadline() throws Exception {
        Long a = packageA(5);
        Long id = c.session(pt, owner, a, AT_18);
        clock.setVn(DAY.plusDays(2).atTime(9, 0));

        noShow(c.manager, UserRole.BRANCH_MANAGER, id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NO_SHOW"));
        assertThat(c.membership(a)).containsEntry("remaining_sessions", 4);
        assertThat(c.auditRows("NO_SHOW")).isEqualTo(1);
    }

    @Test
    @DisplayName("US-F9.3.1 KB5: quản lý xác nhận muộn buổi ngày cuối của gói đã EXPIRED vì hết ngày → DONE, vẫn trừ buổi, không đổi trạng thái gói (chốt 20/11)")
    void managerConfirmsOnPackageExpiredByDate() throws Exception {
        Long a = c.soldPt(owner, "EXPIRED", A_START, DAY, 10, 3, 5_000_000, 500_000);
        Long b = c.soldPt(owner, "ACTIVE", DAY.plusDays(1), DAY.plusDays(60), 10, 10, 6_000_000, 0);
        Long id = c.session(pt, owner, a, AT_18);
        clock.setVn(DAY.plusDays(1).atTime(8, 0));

        confirm(pt.userId(), UserRole.TRAINER, id).andExpect(status().isUnprocessableContent());
        confirm(c.manager, UserRole.BRANCH_MANAGER, id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.commissionAmount").value(45_000))
            .andExpect(jsonPath("$.membershipStatus").value("EXPIRED"))
            .andExpect(jsonPath("$.activatedMembershipId").value(nullValue()));

        assertThat(c.membership(a)).containsEntry("status", "EXPIRED").containsEntry("remaining_sessions", 2);
        assertThat(c.membership(b)).containsEntry("status", "ACTIVE").containsEntry("remaining_sessions", 10);
    }

    @Test
    @DisplayName("US-F9.3.1 E5: buổi đã qua giờ còn sót trên gói đã hết buổi → 422 gói đã hết buổi, buổi vẫn BOOKED")
    void leftoverSessionOnUsedUpPackage() throws Exception {
        Long a = packageA(1);
        Long earlier = c.session(pt, owner, a, DAY.atTime(8, 0));
        Long last = c.session(pt, owner, a, AT_18);
        confirm(pt.userId(), UserRole.TRAINER, last).andExpect(status().isOk());

        confirm(pt.userId(), UserRole.TRAINER, earlier)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail", containsString("đã hết buổi")));
        assertThat(c.ptSession(earlier)).containsEntry("status", "BOOKED").containsEntry("membership_id", a);
        assertThat(c.membership(a)).containsEntry("remaining_sessions", 0);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // VD-21 GĐ-8 = A: buổi BOOKED tương lai khi gói về 0
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US-F9.3.1 GĐ-8: buổi cuối làm gói A về 0 → buổi BOOKED tương lai của A chuyển sang gói B vừa ACTIVE, vẫn BOOKED, không phát sự kiện hủy")
    void futureSessionsMoveToNextPackage() throws Exception {
        Long a = packageA(1);
        Long b = packageB(10);
        Long id = c.session(pt, owner, a, AT_18);
        Long future1 = c.session(pt, owner, a, LocalDateTime.of(2026, 11, 10, 9, 0));
        Long future2 = c.session(pt, owner, a, LocalDateTime.of(2026, 11, 17, 9, 0));

        confirm(pt.userId(), UserRole.TRAINER, id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.movedSessionIds[0]").value(future1))
            .andExpect(jsonPath("$.movedSessionIds[1]").value(future2))
            .andExpect(jsonPath("$.cancelledSessionIds").isEmpty());

        assertThat(c.ptSession(future1)).containsEntry("status", "BOOKED").containsEntry("membership_id", b);
        assertThat(c.ptSession(future2)).containsEntry("status", "BOOKED").containsEntry("membership_id", b);
        assertThat(c.membership(b)).as("chuyển gói không trừ buổi").containsEntry("remaining_sessions", 10);
        assertThat(events.events()).isEmpty();
    }

    @Test
    @DisplayName("US-F9.3.1 GĐ-8: buổi cuối, không có gói nối tiếp → hủy mọi buổi BOOKED tương lai (CANCELLED, cancelled_at), phát sự kiện cho F10 sau commit")
    void futureSessionsCancelledWithoutNextPackage() throws Exception {
        Long a = packageA(1);
        Long id = c.session(pt, owner, a, AT_18);
        LocalDateTime futureStart = LocalDateTime.of(2026, 11, 10, 9, 0);
        Long future = c.session(pt, owner, a, futureStart);

        confirm(pt.userId(), UserRole.TRAINER, id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.movedSessionIds").isEmpty())
            .andExpect(jsonPath("$.cancelledSessionIds[0]").value(future));

        assertThat(c.ptSession(future)).containsEntry("status", "CANCELLED").containsEntry("membership_id", a);
        assertThat(jdbc.queryForObject("SELECT cancelled_at FROM pt_sessions WHERE id = ?", OffsetDateTime.class,
            future).toInstant()).isEqualTo(vn(NOW).toInstant());
        assertThat(c.membership(a)).as("hủy không hoàn buổi").containsEntry("remaining_sessions", 0);
        assertThat(events.events()).singleElement().satisfies(e -> {
            assertThat(e.sessionId()).isEqualTo(future);
            assertThat(e.memberId()).isEqualTo(owner.memberId());
            assertThat(e.trainerId()).isEqualTo(pt.staffId());
            assertThat(e.branchId()).isEqualTo(c.f.cn01);
            assertThat(e.membershipId()).isEqualTo(a);
            assertThat(e.startAt().toInstant()).isEqualTo(vn(futureStart).toInstant());
            assertThat(e.cancelledAt().toInstant()).isEqualTo(vn(NOW).toInstant());
        });
    }

    @Test
    @DisplayName("US-F9.3.1 GĐ-8: gói B chỉ còn 1 buổi (BR-F9.4 trên gói mới) → buổi sớm nhất chuyển sang B, buổi dư bị hủy và báo hội viên")
    void extraSessionsCancelledWhenNextPackageTooSmall() throws Exception {
        Long a = packageA(1);
        Long b = packageB(1);
        Long id = c.session(pt, owner, a, AT_18);
        Long first = c.session(pt, owner, a, LocalDateTime.of(2026, 11, 10, 9, 0));
        Long second = c.session(pt, owner, a, LocalDateTime.of(2026, 11, 17, 9, 0));

        noShow(pt.userId(), UserRole.TRAINER, id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.movedSessionIds[0]").value(first))
            .andExpect(jsonPath("$.cancelledSessionIds[0]").value(second));

        assertThat(c.ptSession(first)).containsEntry("status", "BOOKED").containsEntry("membership_id", b);
        assertThat(c.ptSession(second)).containsEntry("status", "CANCELLED");
        assertThat(events.events()).extracting(PtSessionCancelledByPackageEndEvent::sessionId).containsExactly(second);
    }

    @Test
    @DisplayName("US-F9.3.1 GĐ-8: buổi tương lai sau end_date mới của gói B (kéo về hôm nay, 7 ngày) → hủy; buổi trong hạn B → chuyển")
    void sessionsBeyondNextPackageEndAreCancelled() throws Exception {
        Long a = packageA(1);
        Long b = c.soldPt(owner, "UPCOMING", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 7), 10, 10,
            2_000_000, 0);
        Long id = c.session(pt, owner, a, AT_18);
        Long inside = c.session(pt, owner, a, LocalDateTime.of(2026, 11, 9, 19, 0));
        Long beyond = c.session(pt, owner, a, LocalDateTime.of(2026, 11, 10, 8, 0));

        confirm(pt.userId(), UserRole.TRAINER, id).andExpect(status().isOk());

        assertThat(c.membership(b)).containsEntry("start_date", Date.valueOf(DAY))
            .containsEntry("end_date", Date.valueOf(LocalDate.of(2026, 11, 9)));
        assertThat(c.ptSession(inside)).containsEntry("status", "BOOKED").containsEntry("membership_id", b);
        assertThat(c.ptSession(beyond)).containsEntry("status", "CANCELLED");
    }

    @Test
    @DisplayName("US-F9.3.1 GĐ-8: buổi đã qua giờ chưa xác nhận của gói A không bị chuyển, không bị hủy (chỉ xét buổi chưa diễn ra, Q16)")
    void pastUnconfirmedSessionLeftAlone() throws Exception {
        Long a = packageA(1);
        Long b = packageB(10);
        Long past = c.session(pt, owner, a, DAY.atTime(8, 0));
        Long id = c.session(pt, owner, a, AT_18);

        confirm(pt.userId(), UserRole.TRAINER, id).andExpect(status().isOk());

        assertThat(c.ptSession(past)).containsEntry("status", "BOOKED").containsEntry("membership_id", a);
        assertThat(c.membership(b)).containsEntry("status", "ACTIVE");
    }

    @Test
    @DisplayName("US-F9.3.1 GĐ-8: buổi của hội viên khác, gói khác không bị đụng tới")
    void otherMembersSessionsUntouched() throws Exception {
        Long a = packageA(1);
        Long id = c.session(pt, owner, a, AT_18);
        Long othersFuture = c.f.otherPtSession(pt.staffId(), LocalDateTime.of(2026, 11, 10, 9, 0), "BOOKED");

        confirm(pt.userId(), UserRole.TRAINER, id).andExpect(status().isOk());
        assertThat(c.ptSession(othersFuture)).containsEntry("status", "BOOKED");
    }
}
