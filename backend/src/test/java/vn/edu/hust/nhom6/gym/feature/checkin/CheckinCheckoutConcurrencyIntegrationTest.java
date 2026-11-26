package vn.edu.hust.nhom6.gym.feature.checkin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckinResultDto;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckoutResultDto;
import vn.edu.hust.nhom6.gym.feature.checkin.service.CheckinService;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures.Account;

/**
 * Check-out đồng thời (US-F4.4.1, NF6): khóa dòng {@code checkins} ({@code PESSIMISTIC_WRITE}) trước khi đọc trạng thái
 * để hai lễ tân bấm cùng lúc chỉ ghi đúng một lần.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class CheckinCheckoutConcurrencyIntegrationTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 9, 0);

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired CheckinService checkins;
    @Autowired TransactionTemplate tx;

    CheckinFixtures c;
    ExecutorService pool;

    @BeforeEach
    void setUp() {
        clock.setVn(NOW);
        c = new CheckinFixtures(jdbc, tokens);
        c.reset();
        pool = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    CurrentUser reception() {
        return new CurrentUser(c.receptionist, UserRole.RECEPTIONIST);
    }

    CurrentUser manager() {
        return new CurrentUser(c.manager, UserRole.BRANCH_MANAGER);
    }

    @Test
    @DisplayName("US-F4.4.1 NF6: hai lễ tân bấm check-out cùng lúc trên cùng một lượt thì chỉ một lần ghi (một 200, một 404 E1)")
    void twoReceptionistsCheckoutSameRowAtOnce() throws Exception {
        for (int round = 0; round < 5; round++) {
            clock.setVn(NOW);
            Account a = c.member();
            c.time(a.memberId());
            CheckinResultDto created = checkins.checkin(reception(), a.memberId());

            clock.setVn(NOW.plusMinutes(45));
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Object>> results = new ArrayList<>();
            for (CurrentUser who : new CurrentUser[] {reception(), manager()}) {
                Callable<Object> task = () -> {
                    start.await(10, TimeUnit.SECONDS);
                    try {
                        return checkins.checkout(who, created.checkinId());
                    } catch (NotFoundException notFound) {
                        return notFound;
                    }
                };
                results.add(pool.submit(task));
            }
            start.countDown();

            int ok = 0;
            int notFound = 0;
            for (Future<Object> f : results) {
                Object r = f.get(30, TimeUnit.SECONDS);
                if (r instanceof CheckoutResultDto dto) {
                    ok++;
                    assertThat(dto.durationMinutes()).isEqualTo(45);
                } else if (r instanceof NotFoundException nf) {
                    notFound++;
                    assertThat(nf.getMessage()).isEqualTo("Hội viên không có lượt tập nào đang diễn ra cần check-out.");
                }
            }
            assertThat(ok).as("vòng %d: đúng một yêu cầu check-out thành công", round).isEqualTo(1);
            assertThat(notFound).as("vòng %d: yêu cầu còn lại nhận 404 E1", round).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT checked_out_at FROM checkins WHERE id = ?",
                OffsetDateTime.class, created.checkinId())).isNotNull();
        }
    }

    @Test
    @DisplayName("US-F4.4.1 NF6: khóa dòng FOR UPDATE khiến yêu cầu thứ hai chờ giao dịch đầu ghi xong rồi trả 404, không ghi đè")
    void rowLockPreventsOverwritingCheckout() throws Exception {
        Account a = c.member();
        c.time(a.memberId());
        CheckinResultDto created = checkins.checkin(reception(), a.memberId());
        OffsetDateTime firstCheckout = BookingFixtures.utc(NOW.plusMinutes(30));

        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> holder = pool.submit(() -> tx.executeWithoutResult(status -> {
            jdbc.queryForObject("SELECT id FROM checkins WHERE id = ? FOR UPDATE", Long.class, created.checkinId());
            locked.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
                Thread.sleep(300); // để luồng check-out chạm đúng khóa dòng FOR UPDATE
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            jdbc.update("UPDATE checkins SET checked_out_at = ? WHERE id = ?", firstCheckout, created.checkinId());
        }));
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        clock.setVn(NOW.plusMinutes(90));
        Future<Object> contender = pool.submit(() -> {
            try {
                return checkins.checkout(manager(), created.checkinId());
            } catch (NotFoundException ex) {
                return ex;
            }
        });
        release.countDown();
        holder.get(30, TimeUnit.SECONDS);
        Object result = contender.get(30, TimeUnit.SECONDS);

        assertThat(result).isInstanceOf(NotFoundException.class);
        assertThat(((NotFoundException) result).getMessage())
            .isEqualTo("Hội viên không có lượt tập nào đang diễn ra cần check-out.");
        assertThat(jdbc.queryForObject("SELECT checked_out_at FROM checkins WHERE id = ?",
            OffsetDateTime.class, created.checkinId())).isEqualTo(firstCheckout);
    }
}
