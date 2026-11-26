package vn.edu.hust.nhom6.gym.feature.membership.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;

import vn.edu.hust.nhom6.gym.common.scheduling.ScheduledJob;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipLifecycleService;

/** Lớp tác vụ S2-07.2: lịch cron, múi giờ, chạy bù khi khởi động, thử lại khi lỗi. Logic nằm ở service. */
class MembershipLifecycleJobTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 21, 0, 0);

    MutableClock clock;
    MembershipLifecycleService lifecycle;
    TaskScheduler scheduler;
    MembershipLifecycleJob job;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.EPOCH);
        clock.setVn(NOW);
        lifecycle = mock(MembershipLifecycleService.class);
        scheduler = mock(TaskScheduler.class);
        job = new MembershipLifecycleJob(lifecycle, new VnTime(clock), scheduler);
    }

    @Test
    @DisplayName("TC-F3.5.1-10l: cron 0 0 0 * * * múi giờ Asia/Ho_Chi_Minh; chạy bù nghe ApplicationReadyEvent; là @ScheduledJob (tắt trong kiểm thử)")
    void annotations() throws Exception {
        Scheduled scheduled = MembershipLifecycleJob.class.getMethod("runDaily").getAnnotation(Scheduled.class);
        assertThat(scheduled.cron()).isEqualTo("0 0 0 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Ho_Chi_Minh");
        EventListener listener = MembershipLifecycleJob.class.getMethod("catchUpOnStartup")
            .getAnnotation(EventListener.class);
        assertThat(listener.value()).containsExactly(ApplicationReadyEvent.class);
        assertThat(MembershipLifecycleJob.class.isAnnotationPresent(ScheduledJob.class)).isTrue();
    }

    @Test
    @DisplayName("TC-F3.5.1-10m: lượt chạy truyền ngày Việt Nam của Clock (23:30 UTC 20/11 là 21/11 ở Việt Nam)")
    void passesVietnamDate() {
        clock.setInstant(Instant.parse("2026-11-20T23:30:00Z"));
        job.runDaily();
        verify(lifecycle).runDaily(LocalDate.of(2026, 11, 21));
        verify(scheduler, never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("TC-F3.5.1-10n: chạy bù khi khởi động giao cho TaskScheduler chạy ngay, không gọi thẳng trên luồng main")
    void catchUpGoesThroughScheduler() {
        job.catchUpOnStartup();
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(task.capture(), eq(clock.instant()));
        verify(lifecycle, never()).runDaily(any());
        task.getValue().run();
        verify(lifecycle).runDaily(LocalDate.of(2026, 11, 21));
    }

    @Test
    @DisplayName("TC-F3.5.1-10o: lượt chạy lỗi thì thử lại mỗi 5 phút cho cùng ngày; thành công thì thôi hẹn")
    void retriesEveryFiveMinutesUntilSuccess() {
        doThrow(new IllegalStateException("deadlock")).doThrow(new IllegalStateException("deadlock"))
            .doThrow(new IllegalStateException("deadlock")).doReturn(null)
            .when(lifecycle).runDaily(any());
        ArgumentCaptor<Runnable> retry = ArgumentCaptor.forClass(Runnable.class);

        job.runDaily();
        verify(scheduler).schedule(retry.capture(), eq(clock.instant().plus(MembershipLifecycleJob.RETRY_DELAY)));
        for (int i = 2; i <= 4; i++) {
            clock.advance(MembershipLifecycleJob.RETRY_DELAY);
            retry.getValue().run();
            if (i < 4) {
                verify(scheduler, times(i)).schedule(retry.capture(), any(Instant.class));
            }
        }

        verify(lifecycle, times(4)).runDaily(LocalDate.of(2026, 11, 21));
        verify(scheduler, times(3)).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("TC-F3.5.1-10p: lần thử lại rơi sang ngày Việt Nam mới thì dừng, để lượt cron của ngày mới xử lý")
    void stopsRetryingOnNewDay() {
        doThrow(new IllegalStateException("db down")).when(lifecycle).runDaily(any());
        ArgumentCaptor<Runnable> retry = ArgumentCaptor.forClass(Runnable.class);
        clock.setVn(LocalDateTime.of(2026, 11, 21, 23, 58));

        job.runDaily();
        verify(scheduler).schedule(retry.capture(), any(Instant.class));
        clock.setVn(LocalDateTime.of(2026, 11, 22, 0, 3));
        retry.getValue().run();

        verify(lifecycle, times(1)).runDaily(any());
        verify(scheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
    }
}
