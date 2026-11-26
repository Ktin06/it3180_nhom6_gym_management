package vn.edu.hust.nhom6.gym.feature.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.invoice.SaleFixtures;
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipLifecycleService;

/** Kiểm tra vòng đời trên schema main; không yêu cầu API/fixture F4 hoặc F9. */
@SpringBootTest(properties = "app.scheduling.enabled=false")
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class MembershipLifecycleIntegrationTest {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 21);
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired MembershipLifecycleService lifecycle;
    SaleFixtures f;
    Long member;
    Long seller;
    Long timePackage;
    Long sessionPackage;

    @BeforeEach
    void setUp() {
        clock.setVn(TODAY.atStartOfDay());
        f = new SaleFixtures(jdbc, tokens);
        f.reset();
        member = f.member("HV000123", "0912000123", f.cn01, "ACTIVE");
        seller = f.staff("counter@example.com", UserRole.RECEPTIONIST, f.cn01);
        timePackage = f.pkg("TIME", "TIME", null, 500000, f.cn01, "ACTIVE");
        sessionPackage = f.pkg("SESSION", "SESSION", 10, 800000, f.cn01, "ACTIVE");
    }

    Long held(String type, String status, LocalDate start, LocalDate end, Integer remaining) {
        return f.membership(member, type.equals("TIME") ? timePackage : sessionPackage,
            type, f.cn01, status, start, end, remaining);
    }

    String status(Long id) {
        return jdbc.queryForObject("SELECT status FROM memberships WHERE id=?", String.class, id);
    }

    void checkin(Long membership, LocalDate day, boolean deducted) {
        jdbc.update("""
            INSERT INTO checkins(member_id,membership_id,branch_id,created_by,checkin_date,session_deducted)
            VALUES (?,?,?,?,?,?)""", member, membership, f.cn01, seller, day, deducted);
    }

    @Test
    @DisplayName("TC-F3.5.1-10a: EXPIRED được flush trước khi UPCOMING cùng nhóm lên ACTIVE")
    void expiresOldBeforeActivatingNext() {
        Long old = held("TIME", "ACTIVE", TODAY.minusDays(30), TODAY.minusDays(1), null);
        Long next = held("TIME", "UPCOMING", TODAY, TODAY.plusDays(29), null);
        var result = lifecycle.runDaily(TODAY);
        assertThat(result.expired()).containsExactly(old);
        assertThat(result.upcoming().activated()).containsExactly(next);
        assertThat(status(old)).isEqualTo("EXPIRED");
        assertThat(status(next)).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("TC-F3.5.1-10b: SESSION hết buổi hôm nay vẫn ACTIVE khi chạy bù trong ngày")
    void lastDeductionTodayRetainsAccess() {
        Long old = held("SESSION", "ACTIVE", TODAY.minusDays(10), TODAY.plusDays(10), 0);
        checkin(old, TODAY, true);
        assertThat(lifecycle.runDaily(TODAY).expired()).isEmpty();
        assertThat(status(old)).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("TC-F3.5.1-10c: SESSION 0 buổi từ hôm qua hết hạn; check-in không trừ buổi hôm nay không giữ gói")
    void zeroSessionsFromYesterdayExpire() {
        Long old = held("SESSION", "ACTIVE", TODAY.minusDays(10), TODAY.plusDays(10), 0);
        Long next = held("SESSION", "UPCOMING", TODAY, TODAY.plusDays(29), 10);
        checkin(old, TODAY.minusDays(1), true);
        checkin(old, TODAY, false);
        assertThat(lifecycle.runDaily(TODAY).expired()).containsExactly(old);
        assertThat(status(next)).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("TC-F3.5.1-10d: Chạy lại cùng ngày không đổi version hoặc updated_at")
    void repeatedRunDoesNotMutateAgain() {
        held("TIME", "ACTIVE", TODAY.minusDays(30), TODAY.minusDays(1), null);
        held("TIME", "UPCOMING", TODAY, TODAY.plusDays(29), null);
        lifecycle.runDaily(TODAY);
        var before = jdbc.queryForList("SELECT * FROM memberships ORDER BY id");
        clock.setVn(TODAY.atTime(12, 0));
        var result = lifecycle.runDaily(TODAY);
        assertThat(result.expired()).isEmpty();
        assertThat(result.upcoming().activated()).isEmpty();
        assertThat(jdbc.queryForList("SELECT * FROM memberships ORDER BY id")).isEqualTo(before);
    }

    @Test
    @DisplayName("TC-F3.5.1-10e: Chạy bù nhiều ngày bỏ gói đã trôi hết hạn rồi kích hoạt gói hiện tại")
    void catchesUpAfterSeveralMissedDays() {
        Long old = held("TIME", "ACTIVE", TODAY.minusDays(70), TODAY.minusDays(41), null);
        Long past = held("TIME", "UPCOMING", TODAY.minusDays(40), TODAY.minusDays(11), null);
        Long current = held("TIME", "UPCOMING", TODAY.minusDays(10), TODAY.plusDays(19), null);
        var result = lifecycle.runDaily(TODAY);
        assertThat(result.expired()).containsExactly(old);
        assertThat(result.upcoming().expiredUnstarted()).containsExactly(past);
        assertThat(result.upcoming().activated()).containsExactly(current);
        assertThat(status(past)).isEqualTo("EXPIRED");
        assertThat(status(current)).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("TC-F3.5.1-10f: Ngày end_date còn dùng được và gói UPCOMING tương lai chưa kích hoạt")
    void endDateIsInclusive() {
        Long old = held("TIME", "ACTIVE", TODAY.minusDays(29), TODAY, null);
        Long next = held("TIME", "UPCOMING", TODAY.plusDays(1), TODAY.plusDays(30), null);
        lifecycle.runDaily(TODAY);
        assertThat(status(old)).isEqualTo("ACTIVE");
        assertThat(status(next)).isEqualTo("UPCOMING");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "CANCELLED", "EXPIRED", "TRANSFERRED", "FROZEN"})
    @DisplayName("TC-F3.5.1-10g: Tác vụ không đổi gói ngoài ACTIVE và UPCOMING")
    void ignoresOtherStates(String state) {
        Long id = held("TIME", state, TODAY.minusDays(40), TODAY.minusDays(11), null);
        var before = jdbc.queryForMap("SELECT * FROM memberships WHERE id=?", id);
        lifecycle.runDaily(TODAY);
        assertThat(jdbc.queryForMap("SELECT * FROM memberships WHERE id=?", id)).isEqualTo(before);
    }

    @Test
    @DisplayName("TC-F3.5.1-10h: Gói FROZEN chặn kích hoạt; gói chờ giữ nguyên kể cả quá ngày cuối")
    void frozenBlocksWithoutChangingWaitingPackage() {
        held("TIME", "FROZEN", TODAY.minusDays(60), TODAY.minusDays(31), null);
        Long next = held("TIME", "UPCOMING", TODAY.minusDays(30), TODAY.minusDays(1), null);
        assertThat(lifecycle.runDaily(TODAY).upcoming().skipped()).containsExactly(next);
        assertThat(status(next)).isEqualTo("UPCOMING");
    }

    @Test
    @DisplayName("TC-F3.5.1-10i: Gói toàn chuỗi và chi nhánh là hai nhóm độc lập; NULL được so đúng")
    void chainAndBranchAreIndependentGroups() {
        held("TIME", "ACTIVE", TODAY.minusDays(10), TODAY.plusDays(19), null);
        Long pkg = f.pkg("CHAIN", "TIME", null, 900000, null, "ACTIVE");
        Long chain = f.membership(member, pkg, "TIME", null, "UPCOMING", TODAY, TODAY.plusDays(29), null);
        Long duplicate = f.membership(member, pkg, "TIME", null, "UPCOMING", TODAY, TODAY.plusDays(29), null);
        var result = lifecycle.runDaily(TODAY);
        assertThat(result.upcoming().activated()).containsExactly(chain);
        assertThat(result.upcoming().skipped()).containsExactly(duplicate);
    }

    @Test
    @DisplayName("TC-F3.5.1-10j: Gói PT hết ngày còn buổi chuyển EXPIRED; không trừ số buổi còn lại")
    void expiredPtKeepsRemainingSessions() {
        Long pkg = f.pkg("PT", "PT", 10, 3000000, f.cn01, "ACTIVE");
        Long old = f.membership(member, pkg, "PT", f.cn01, "ACTIVE", TODAY.minusDays(30), TODAY.minusDays(1), 3);
        Long next = f.membership(member, pkg, "PT", f.cn01, "UPCOMING", TODAY, TODAY.plusDays(29), 10);
        lifecycle.runDaily(TODAY);
        assertThat(status(old)).isEqualTo("EXPIRED");
        assertThat(jdbc.queryForObject("SELECT remaining_sessions FROM memberships WHERE id=?", Integer.class, old))
            .isEqualTo(3);
        assertThat(status(next)).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("TC-F3.5.1-10k: Lỗi kích hoạt rollback cả bước EXPIRED; chạy lại sau khi hết lỗi được")
    void activationFailureRollsBackExpiration() {
        Long old = held("TIME", "ACTIVE", TODAY.minusDays(30), TODAY.minusDays(1), null);
        Long next = held("TIME", "UPCOMING", TODAY, TODAY.plusDays(29), null);
        jdbc.execute("""
            CREATE FUNCTION test_reject_activation() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN
              IF OLD.status='UPCOMING' AND NEW.status='ACTIVE' THEN
                RAISE EXCEPTION 'test activation failure';
              END IF;
              RETURN NEW;
            END $$""");
        jdbc.execute("CREATE TRIGGER test_activation BEFORE UPDATE ON memberships FOR EACH ROW EXECUTE FUNCTION test_reject_activation()");
        try {
            assertThatThrownBy(() -> lifecycle.runDaily(TODAY)).isInstanceOf(RuntimeException.class);
            assertThat(status(old)).isEqualTo("ACTIVE");
            assertThat(status(next)).isEqualTo("UPCOMING");
        } finally {
            jdbc.execute("DROP TRIGGER test_activation ON memberships");
            jdbc.execute("DROP FUNCTION test_reject_activation()");
        }
        assertThat(lifecycle.runDaily(TODAY).upcoming().activated()).containsExactly(next);
    }
}
