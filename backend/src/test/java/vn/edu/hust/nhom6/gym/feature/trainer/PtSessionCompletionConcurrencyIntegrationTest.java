package vn.edu.hust.nhom6.gym.feature.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.Account;
import vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.Trainer;
import vn.edu.hust.nhom6.gym.feature.trainer.service.PtSessionCompletionService;
import vn.edu.hust.nhom6.gym.feature.trainer.service.PtSessionCompletionService.Outcome;
import vn.edu.hust.nhom6.gym.feature.trainer.service.PtSessionService;

/**
 * Tranh chấp đồng thời khi xác nhận, ghi vắng buổi PT (UC-F9.3 E4, BR-F9.6 "mỗi buổi chỉ trừ một lần", NF6, #212).
 * Thứ tự khóa: {@code members → memberships → pt_sessions} (khớp check-in F4). Mỗi yêu cầu chạy trong giao dịch riêng
 * trên luồng riêng; chạy đua nhiều vòng, bên thua chỉ được nhận 409 hoặc 422 (lỗi khác như khóa chéo làm hỏng kiểm thử).
 *
 * <p>Mốc: 03/11/2026 19:30 giờ Việt Nam; buổi 03/11 18:00.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class PtSessionCompletionConcurrencyIntegrationTest {
    static final LocalDate DAY = LocalDate.of(2026, 11, 3);
    static final LocalDateTime NOW = DAY.atTime(19, 30);
    static final LocalDateTime AT_18 = DAY.atTime(18, 0);
    static final int ROUNDS = 5;

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired PtSessionCompletionService completions;
    @Autowired PtSessionService ptSessions;

    PtCompletionFixtures c;
    ExecutorService pool;

    @BeforeEach
    void setUp() {
        clock.setVn(NOW);
        c = new PtCompletionFixtures(jdbc, tokens);
        c.reset();
        pool = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    static CurrentUser trainer(Trainer t) {
        return new CurrentUser(t.userId(), UserRole.TRAINER);
    }

    Long packageOf(Account a, int remaining) {
        return c.soldPt(a, "ACTIVE", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30), 10, remaining,
            5_000_000, 500_000);
    }

    /** Chạy các tác vụ cùng lúc; trả số tác vụ thành công, ném lại lỗi lạ (không phải 409, 422). */
    int race(List<Callable<Object>> tasks) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Object> task : tasks) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    task.call();
                    return true;
                } catch (ConflictException | BusinessRuleException lost) {
                    return false;
                }
            }));
        }
        start.countDown();
        int ok = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(30, TimeUnit.SECONDS)) {
                ok++;
            }
        }
        return ok;
    }

    void awaitBlockedOnLock() throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            Integer waiting = jdbc.queryForObject("""
                SELECT COUNT(*) FROM pg_stat_activity
                WHERE datname = current_database() AND wait_event_type = 'Lock'""", Integer.class);
            if (waiting != null && waiting > 0) {
                return;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Yêu cầu xác nhận không bị chặn ở bước khóa dòng.");
    }

    @Test
    @DisplayName("TC-F9.3.1-05a: thao tác khác ghi DONE và commit trong lúc yêu cầu này chờ khóa members → 409, gói không bị trừ thêm")
    void sessionCompletedWhileWaitingIs409() throws Exception {
        Trainer pt = c.trainer("0.1000");
        Account a = c.member();
        Long membership = packageOf(a, 5);
        Long id = c.session(pt, a, membership, AT_18);

        try (Connection held = dataSource.getConnection()) {
            held.setAutoCommit(false);
            try (PreparedStatement lock = held.prepareStatement("SELECT id FROM members WHERE id = ? FOR UPDATE")) {
                lock.setLong(1, a.memberId());
                lock.executeQuery().close();
            }
            try (PreparedStatement done = held.prepareStatement("""
                    UPDATE pt_sessions SET status = 'DONE', commission_amount = 1, confirmed_at = now() WHERE id = ?""")) {
                done.setLong(1, id);
                done.executeUpdate();
            }
            Future<Object> pending = pool.submit(() -> completions.complete(trainer(pt), id, Outcome.DONE));
            awaitBlockedOnLock();
            held.commit();
            assertThatThrownBy(() -> pending.get(30, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ConflictException.class);
        }
        assertThat(c.membership(membership)).containsEntry("remaining_sessions", 5);
    }

    @Test
    @DisplayName("TC-F9.3.1-05b: hai lần xác nhận cùng lúc một buổi → đúng một thành công, gói trừ đúng 1 buổi")
    void doubleConfirmRace() throws Exception {
        Trainer pt = c.trainer("0.1000");
        for (int round = 0; round < ROUNDS; round++) {
            Account a = c.member();
            Long membership = packageOf(a, 5);
            Long id = c.session(pt, a, membership, AT_18);

            int ok = race(List.of(
                () -> completions.complete(trainer(pt), id, Outcome.DONE),
                () -> completions.complete(new CurrentUser(c.manager, UserRole.BRANCH_MANAGER), id, Outcome.DONE)));

            assertThat(ok).as("vòng %d", round).isEqualTo(1);
            assertThat(c.membership(membership)).containsEntry("remaining_sessions", 4);
            assertThat(c.ptSession(id)).containsEntry("status", "DONE");
        }
    }

    @Test
    @DisplayName("TC-F9.3.1-05c: xác nhận và ghi vắng cùng lúc một buổi → đúng một thành công, gói trừ đúng 1 buổi")
    void confirmVersusNoShowRace() throws Exception {
        Trainer pt = c.trainer("0.1000");
        for (int round = 0; round < ROUNDS; round++) {
            Account a = c.member();
            Long membership = packageOf(a, 5);
            Long id = c.session(pt, a, membership, AT_18);

            int ok = race(List.of(
                () -> completions.complete(trainer(pt), id, Outcome.DONE),
                () -> completions.complete(trainer(pt), id, Outcome.NO_SHOW)));

            assertThat(ok).as("vòng %d", round).isEqualTo(1);
            assertThat(c.membership(membership)).containsEntry("remaining_sessions", 4);
        }
    }

    @Test
    @DisplayName("TC-F9.3.1-05d: gói còn 1 buổi, hai buổi đã qua giờ được xác nhận cùng lúc → một thành công, bên kia 422, gói 0 buổi không âm")
    void lastSessionRace() throws Exception {
        Trainer pt = c.trainer("0.1000");
        for (int round = 0; round < ROUNDS; round++) {
            Account a = c.member();
            Long membership = packageOf(a, 1);
            Long morning = c.session(pt, a, membership, DAY.atTime(8, 0));
            Long evening = c.session(pt, a, membership, AT_18);

            int ok = race(List.of(
                () -> completions.complete(trainer(pt), morning, Outcome.DONE),
                () -> completions.complete(trainer(pt), evening, Outcome.NO_SHOW)));

            assertThat(ok).as("vòng %d", round).isEqualTo(1);
            assertThat(c.membership(membership)).containsEntry("remaining_sessions", 0)
                .containsEntry("status", "EXPIRED");
        }
    }

    @Test
    @DisplayName("TC-F9.3.1-05e: hội viên hủy buổi tương lai trong lúc xác nhận buổi cuối chuyển hoặc hủy buổi đó → không khóa chéo, buổi tương lai CANCELLED đúng một lần")
    void confirmLastRacesMemberCancellingFuture() throws Exception {
        Trainer pt = c.trainer("0.1000");
        for (int round = 0; round < ROUNDS; round++) {
            Account a = c.member();
            Long membership = packageOf(a, 1);
            Long id = c.session(pt, a, membership, AT_18);
            Long future = c.session(pt, a, membership, LocalDateTime.of(2026, 11, 10, 9, 0));

            int ok = race(List.of(
                () -> completions.complete(trainer(pt), id, Outcome.DONE),
                () -> ptSessions.cancel(new CurrentUser(a.userId(), UserRole.MEMBER), future, null)));

            assertThat(c.ptSession(id)).as("vòng %d", round).containsEntry("status", "DONE");
            assertThat(c.ptSession(future)).containsEntry("status", "CANCELLED");
            assertThat(ok).isBetween(1, 2);
        }
    }
}
