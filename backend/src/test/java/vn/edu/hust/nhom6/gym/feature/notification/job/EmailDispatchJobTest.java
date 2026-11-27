package vn.edu.hust.nhom6.gym.feature.notification.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.notification.service.EmailDispatchService;

/** Kiểm thử đơn vị {@link EmailDispatchJob}: chạy trên luồng riêng không chặn scheduler và bỏ qua lượt chồng. */
class EmailDispatchJobTest {
    MutableClock clock;
    EmailDispatchService dispatcher;
    ExecutorService executor;
    EmailDispatchJob job;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.EPOCH);
        clock.setVn(LocalDateTime.of(2026, 11, 21, 10, 0));
        dispatcher = mock(EmailDispatchService.class);
        executor = Executors.newSingleThreadExecutor();
        job = new EmailDispatchJob(dispatcher, new VnTime(clock), executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @DisplayName("TC-F10.1.1-07d: EmailDispatchJob đẩy gửi email sang luồng riêng và bỏ qua lượt mới khi lượt trước chưa xong")
    void runsOffSchedulerThreadAndSkipsOverlap() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);

        doAnswer(invocation -> {
            entered.countDown();
            release.await(5, TimeUnit.SECONDS);
            finished.countDown();
            return new EmailDispatchService.DispatchResult(0, 0, 0);
        }).when(dispatcher).dispatchDue(any());

        job.run();
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

        // Lượt thứ hai gọi trong lúc lượt đầu đang chờ Brevo -> bỏ qua ngay, không xếp hàng.
        job.run();

        release.countDown();
        assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();

        // Đợi khối finally reset cờ running rồi gọi lượt kế tiếp.
        executor.submit(() -> { }).get(5, TimeUnit.SECONDS);
        verify(dispatcher, times(1)).dispatchDue(any());
    }
}
