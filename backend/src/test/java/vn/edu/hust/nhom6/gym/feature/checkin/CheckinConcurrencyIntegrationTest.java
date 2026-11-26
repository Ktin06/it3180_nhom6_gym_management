package vn.edu.hust.nhom6.gym.feature.checkin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckinResultDto;
import vn.edu.hust.nhom6.gym.feature.checkin.service.CheckinService;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures.Account;

/**
 * Check-in đồng thời (US-F4.2.1 KB7, E6; NF6). Gọi thẳng {@link CheckinService} từ nhiều luồng, mỗi lời gọi một giao
 * dịch thật (không bọc {@code @Transactional} ở lớp kiểm thử).
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class CheckinConcurrencyIntegrationTest {
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

    /**
     * SRS F4 Scenario 7 (VD-20 GĐ-1, trưởng nhóm chốt 20/11: sửa mã theo SRS). Lễ tân thứ nhất check-in thật trong một
     * giao dịch và <b>giữ</b> giao dịch mở (latch), tức là đang giữ khóa dòng {@code members}. Lễ tân thứ hai bấm lúc
     * đó: phải nhận 409 câu E6 <b>ngay</b> (không chờ bên thứ nhất). Bên thứ nhất commit xong, lễ tân thứ hai bấm lại thì
     * 201 không trừ buổi. Kết quả: gói 5 buổi còn 4, đúng một lượt trừ, hai lượt check-in.
     */
    @Test
    @DisplayName("US-F4.2.1 KB7: hai lễ tân cùng check-in hội viên gói SESSION 5 buổi: bên đang giữ khóa trừ 1 buổi, bên sau nhận 409 ngay; bấm lại 201 không trừ, còn 4 buổi")
    void kb7SecondReceptionistGets409Immediately() throws Exception {
        Account a = c.member();
        Long s = c.session(a.memberId(), 5);
        CountDownLatch firstDone = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<CheckinResultDto> first = pool.submit(() -> tx.execute(status -> {
            CheckinResultDto r = checkins.checkin(reception(), a.memberId());
            firstDone.countDown();
            try {
                release.await(20, TimeUnit.SECONDS); // giữ giao dịch mở = giữ khóa members
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return r;
        }));
        assertThat(firstDone.await(10, TimeUnit.SECONDS)).isTrue();

        long started = System.nanoTime();
        Future<Object> second = pool.submit(() -> {
            try {
                return checkins.checkin(manager(), a.memberId());
            } catch (ConflictException conflict) {
                return conflict;
            }
        });
        // Không chờ: nếu bên sau phải đợi khóa thì sẽ treo tới khi release (20 giây) và get() hết hạn.
        Object result = second.get(5, TimeUnit.SECONDS);
        long waitedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        assertThat(result).isInstanceOf(ConflictException.class);
        assertThat(((ConflictException) result).getMessage()).isEqualTo("Dữ liệu lượt check-in đang được xử lý đồng "
            + "thời bởi một thao tác khác. Vui lòng thử lại.");
        assertThat(waitedMs).as("409 trả ngay, không chờ khóa").isLessThan(3_000);

        release.countDown();
        CheckinResultDto winner = first.get(30, TimeUnit.SECONDS);
        assertThat(winner.appliedMembership().sessionDeducted()).isTrue();
        assertThat(c.remaining(s)).isEqualTo(4);

        CheckinResultDto retry = checkins.checkin(manager(), a.memberId());
        assertThat(retry.appliedMembership().sessionDeducted()).isFalse();
        assertThat(c.remaining(s)).isEqualTo(4);
        assertThat(c.deductions(s)).isEqualTo(1);
        assertThat(c.checkins(a.memberId())).isEqualTo(2);
    }

    /**
     * E6 (lưới an toàn): một giao dịch khác (giả lập máy check-in khuôn mặt v5 hoặc mã đi vòng không khóa hội viên) ghi
     * lượt trừ buổi hôm nay nhưng chưa commit. Check-in tại quầy không thấy dòng đó, trừ buổi, rồi đụng chỉ mục
     * {@code uq_one_deduction_per_day} khi bên kia commit: phải trả 409 câu E6, không trừ buổi; bấm lại thì 201 không trừ.
     */
    @Test
    @DisplayName("US-F4.2.1 E6: đụng chỉ mục uq_one_deduction_per_day thì 409 đúng câu SRS, rollback số buổi; thử lại thì 201 không trừ")
    void e6UniqueIndexConflictThenRetry() throws Exception {
        Account a = c.member();
        Long s = c.session(a.memberId(), 5);
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> other = pool.submit(() -> tx.executeWithoutResult(status -> {
            jdbc.update("""
                INSERT INTO checkins (member_id, membership_id, branch_id, created_by, method, checkin_date,
                                      session_deducted)
                VALUES (?, ?, ?, ?, 'MANUAL', ?, true)""", a.memberId(), s, c.cn01, c.receptionist,
                LocalDate.of(2026, 11, 20));
            inserted.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
                Thread.sleep(500); // để check-in tại quầy chạy tới chỗ ghi checkins và chờ chỉ mục
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        assertThat(inserted.await(10, TimeUnit.SECONDS)).isTrue();
        Future<Object> counter = pool.submit(() -> {
            try {
                return checkins.checkin(reception(), a.memberId());
            } catch (ConflictException conflict) {
                return conflict;
            }
        });
        release.countDown();
        other.get(30, TimeUnit.SECONDS);
        Object result = counter.get(30, TimeUnit.SECONDS);
        assertThat(result).isInstanceOf(ConflictException.class);
        assertThat(((ConflictException) result).getMessage()).isEqualTo("Dữ liệu lượt check-in đang được xử lý đồng "
            + "thời bởi một thao tác khác. Vui lòng thử lại.");
        assertThat(c.remaining(s)).as("giao dịch 409 đã rollback phần trừ buổi").isEqualTo(5);

        CheckinResultDto retry = checkins.checkin(reception(), a.memberId());
        assertThat(retry.appliedMembership().sessionDeducted()).isFalse();
        assertThat(c.remaining(s)).isEqualTo(5);
        assertThat(c.deductions(s)).isEqualTo(1);
    }

    @Test
    @DisplayName("US-F4.2.1 S3: gọi thẳng service với người không có chi nhánh (chủ chuỗi) thì 403, không ghi gì")
    void ownerWithoutBranchIsDenied() {
        Account a = c.member();
        c.time(a.memberId());
        assertThatThrownBy(() -> checkins.checkin(new CurrentUser(c.owner, UserRole.SUPER_ADMIN), a.memberId()))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(c.checkins(a.memberId())).isZero();
    }
}
