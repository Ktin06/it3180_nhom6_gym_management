package vn.edu.hust.nhom6.gym.feature.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.vn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
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
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.SessionTimeRequest;
import vn.edu.hust.nhom6.gym.feature.groupclass.service.ClassSessionService;
import vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.Account;
import vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.Trainer;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.PtSessionBookRequest;
import vn.edu.hust.nhom6.gym.feature.trainer.service.PtSessionService;

/**
 * Tranh chấp đồng thời khi đặt, hủy buổi PT (NF6, #212). Mỗi yêu cầu chạy trong giao dịch riêng trên luồng riêng.
 *
 * <p>Hai loại kiểm thử:
 * <ul>
 *   <li><b>Xác định:</b> kiểm thử tự giữ khóa bằng một kết nối JDBC riêng, chèn buổi trùng nhưng chưa commit, đợi yêu cầu
 *       đặt bị chặn ở bước khóa rồi mới commit. Yêu cầu đặt chắc chắn đã qua bước đọc trước khóa nên phải nhận 409.</li>
 *   <li><b>Chạy đua:</b> các yêu cầu cùng xuất phát sau một chốt chặn, chạy nhiều vòng; đúng một yêu cầu thành công,
 *       bên thua nhận 409 hoặc 422 tùy thời điểm đọc (như S2-16.2). Lỗi khác (khóa chéo, vi phạm ràng buộc) làm hỏng
 *       kiểm thử.</li>
 * </ul>
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class PtSessionConcurrencyIntegrationTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 6, 0);
    static final int ROUNDS = 5;

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired PtSessionService ptSessions;
    @Autowired ClassSessionService classSessions;

    PtSessionFixtures f;
    ExecutorService pool;

    @BeforeEach
    void setUp() {
        clock.setVn(NOW);
        f = new PtSessionFixtures(jdbc, tokens);
        f.reset();
        pool = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    static CurrentUser me(Account a) {
        return new CurrentUser(a.userId(), UserRole.MEMBER);
    }

    PtSessionBookRequest at(Trainer t, LocalDateTime vnStart) {
        return new PtSessionBookRequest(t.staffId(), vn(vnStart));
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

    /** Đợi tới khi có một phiên khác của database đang chờ khóa dòng. */
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
        throw new AssertionError("Yêu cầu đặt buổi không bị chặn ở bước khóa dòng.");
    }

    /** Trong một giao dịch JDBC riêng: khóa {@code lockSql}, chèn buổi PT BOOKED, chưa commit. */
    Connection holdLockAndInsert(String lockSql, Long lockId, Long trainerId, Account member, LocalDateTime vnStart)
            throws Exception {
        Connection c = dataSource.getConnection();
        c.setAutoCommit(false);
        try (PreparedStatement lock = c.prepareStatement(lockSql)) {
            lock.setLong(1, lockId);
            lock.executeQuery().close();
        }
        try (PreparedStatement insert = c.prepareStatement("""
                INSERT INTO pt_sessions (trainer_id, member_id, membership_id, branch_id, start_at, end_at, status,
                                         created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'BOOKED', now(), now())""")) {
            insert.setLong(1, trainerId);
            insert.setLong(2, member.memberId());
            insert.setLong(3, f.ptMembershipOf(member.memberId()));
            insert.setLong(4, f.cn01);
            insert.setTimestamp(5, Timestamp.from(vn(vnStart).toInstant()));
            insert.setTimestamp(6, Timestamp.from(vn(vnStart).plusMinutes(60).toInstant()));
            insert.executeUpdate();
        }
        return c;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // E4 (409) xác định
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("TC-F9.2.1-10a: yêu cầu khác đặt cùng khung của PT và commit trong lúc yêu cầu này chờ khóa staff → 409")
    void trainerSlotTakenWhileWaitingIs409() throws Exception {
        Trainer pt = f.trainer(f.cn01);
        Account winner = f.ptMember(10);
        Account loser = f.ptMember(10);
        LocalDateTime slot = LocalDateTime.of(2026, 11, 21, 10, 0);

        try (Connection held = holdLockAndInsert("SELECT id FROM staff WHERE id = ? FOR UPDATE", pt.staffId(),
                pt.staffId(), winner, slot)) {
            Future<Object> pending = pool.submit(() -> ptSessions.book(me(loser), at(pt, slot)));
            awaitBlockedOnLock();
            held.commit();
            assertThatThrownBy(() -> pending.get(30, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ConflictException.class);
        }
        assertThat(f.bookedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-F9.2.1-10b: chính hội viên vừa đặt cùng giờ với PT khác và commit trong lúc yêu cầu này chờ khóa members → 409")
    void memberSlotTakenWhileWaitingIs409() throws Exception {
        Trainer pt1 = f.trainer(f.cn01);
        Trainer pt2 = f.trainer(f.cn01);
        Account a = f.ptMember(10);
        LocalDateTime slot = LocalDateTime.of(2026, 11, 21, 10, 0);

        try (Connection held = holdLockAndInsert("SELECT id FROM members WHERE id = ? FOR UPDATE", a.memberId(),
                pt2.staffId(), a, slot)) {
            Future<Object> pending = pool.submit(() -> ptSessions.book(me(a), at(pt1, slot)));
            awaitBlockedOnLock();
            held.commit();
            assertThatThrownBy(() -> pending.get(30, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ConflictException.class);
        }
        assertThat(f.bookedCount()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Chạy đua nhiều vòng
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("TC-F9.2.1-10c: hai hội viên cùng đặt một khung giờ của một PT cùng lúc → đúng một BOOKED")
    void twoMembersRaceForSameTrainerSlot() throws Exception {
        Trainer pt = f.trainer(f.cn01);
        for (int round = 0; round < ROUNDS; round++) {
            LocalDateTime slot = LocalDateTime.of(2026, 11, 21, 8 + 2 * round, 0);
            Account first = f.ptMember(10);
            Account second = f.ptMember(10);
            int ok = race(List.of(
                () -> ptSessions.book(me(first), at(pt, slot)),
                () -> ptSessions.book(me(second), at(pt, slot.plusMinutes(30)))));
            assertThat(ok).as("vòng %d", round).isEqualTo(1);
            assertThat(f.bookedCount()).as("vòng %d", round).isEqualTo(round + 1L);
        }
    }

    @Test
    @DisplayName("TC-F9.2.1-10d: một hội viên đặt cùng lúc hai PT khác nhau cùng giờ → đúng một BOOKED")
    void sameMemberRacesTwoTrainers() throws Exception {
        Trainer pt1 = f.trainer(f.cn01);
        Trainer pt2 = f.trainer(f.cn01);
        for (int round = 0; round < ROUNDS; round++) {
            LocalDateTime slot = LocalDateTime.of(2026, 11, 22, 8 + 2 * round, 0);
            Account a = f.ptMember(10);
            int ok = race(List.of(
                () -> ptSessions.book(me(a), at(pt1, slot)),
                () -> ptSessions.book(me(a), at(pt2, slot))));
            assertThat(ok).as("vòng %d", round).isEqualTo(1);
            assertThat(f.bookedCount()).as("vòng %d", round).isEqualTo(round + 1L);
        }
    }

    @Test
    @DisplayName("TC-F9.2.1-08e, TC-F9.2.1-10e: gói còn 1 buổi, hội viên đặt cùng lúc hai khung khác nhau → đúng một BOOKED")
    void lastSessionRace() throws Exception {
        Trainer pt1 = f.trainer(f.cn01);
        Trainer pt2 = f.trainer(f.cn01);
        for (int round = 0; round < ROUNDS; round++) {
            Account a = f.ptMember(1);
            LocalDateTime day = LocalDateTime.of(2026, 11, 23 + round, 9, 0);
            int ok = race(List.of(
                () -> ptSessions.book(me(a), at(pt1, day)),
                () -> ptSessions.book(me(a), at(pt2, day.plusHours(3)))));
            assertThat(ok).as("vòng %d", round).isEqualTo(1);
        }
        assertThat(f.bookedCount()).isEqualTo(ROUNDS);
    }

    @Test
    @DisplayName("TC-F9.2.1-05g, TC-F9.2.1-10f: đặt buổi PT và đổi giờ buổi lớp của cùng PT sang khung đó cùng lúc → không khóa chéo, đúng một bên thành công")
    void bookingRacesClassReschedule() throws Exception {
        Trainer pt = f.trainer(f.cn01);
        Long manager = f.staffUser(UserRole.BRANCH_MANAGER, f.cn01);
        CurrentUser managerUser = new CurrentUser(manager, UserRole.BRANCH_MANAGER);
        for (int round = 0; round < ROUNDS; round++) {
            LocalDateTime slot = LocalDateTime.of(2026, 11, 24 + round, 10, 0);
            Long classSession = f.classSession(pt.staffId(), slot.plusHours(3), slot.plusHours(4), "SCHEDULED");
            Account a = f.ptMember(10);
            int ok = race(List.of(
                () -> ptSessions.book(me(a), at(pt, slot)),
                () -> classSessions.changeTime(managerUser, classSession,
                    new SessionTimeRequest(vn(slot.plusMinutes(30)), vn(slot.plusMinutes(90))))));
            assertThat(ok).as("vòng %d", round).isEqualTo(1);
            long ptBooked = jdbc.queryForObject("""
                SELECT COUNT(*) FROM pt_sessions WHERE status = 'BOOKED' AND trainer_id = ? AND start_at = ?""",
                Long.class, pt.staffId(), vn(slot));
            long moved = jdbc.queryForObject("SELECT COUNT(*) FROM class_sessions WHERE id = ? AND start_at = ?",
                Long.class, classSession, vn(slot.plusMinutes(30)));
            assertThat(ptBooked + moved).as("vòng %d: chỉ một trong hai thay đổi được ghi", round).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("TC-F9.7.1-11a: hội viên và lễ tân cùng hủy một buổi → đúng một lần hủy thành công")
    void doubleCancelRace() throws Exception {
        Trainer pt = f.trainer(f.cn01);
        Long receptionist = f.staffUser(UserRole.RECEPTIONIST, f.cn01);
        CurrentUser desk = new CurrentUser(receptionist, UserRole.RECEPTIONIST);
        for (int round = 0; round < ROUNDS; round++) {
            Account a = f.ptMember(10);
            Long id = f.ptSession(pt.staffId(), a.memberId(), f.ptMembershipOf(a.memberId()),
                LocalDateTime.of(2026, 11, 25 + round, 10, 0), "BOOKED");
            int ok = race(List.of(
                () -> ptSessions.cancel(me(a), id, null),
                () -> ptSessions.cancel(desk, id, null)));
            assertThat(ok).as("vòng %d", round).isEqualTo(1);
            assertThat(f.status(id)).isEqualTo("CANCELLED");
        }
    }
}
