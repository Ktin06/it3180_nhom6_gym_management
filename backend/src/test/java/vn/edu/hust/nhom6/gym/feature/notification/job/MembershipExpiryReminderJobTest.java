package vn.edu.hust.nhom6.gym.feature.notification.job;

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
import vn.edu.hust.nhom6.gym.feature.notification.service.MembershipExpiryReminderService;

/** Kiểm thử đơn vị tác vụ nhắc gói sắp hết: cron, chạy bù khi khởi động và thử lại khi lỗi. */
class MembershipExpiryReminderJobTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 21, 0, 10);

    MutableClock clock;
    MembershipExpiryReminderService reminders;
    TaskScheduler scheduler;
    MembershipExpiryReminderJob job;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.EPOCH);
        clock.setVn(NOW);
        reminders = mock(MembershipExpiryReminderService.class);
        scheduler = mock(TaskScheduler.class);
        job = new MembershipExpiryReminderJob(reminders, new VnTime(clock), scheduler);
    }

    @Test
    @DisplayName("TC-F10.1.1-01c: cron 0 10 0 * * * múi giờ Asia/Ho_Chi_Minh, chạy bù sau 1 phút và là @ScheduledJob")
    void annotationsAndCatchUp() throws Exception {
        Scheduled scheduled = MembershipExpiryReminderJob.class.getMethod("runDaily").getAnnotation(Scheduled.class);
        assertThat(scheduled.cron()).isEqualTo("0 10 0 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Ho_Chi_Minh");
        EventListener listener = MembershipExpiryReminderJob.class.getMethod("catchUpOnStartup")
            .getAnnotation(EventListener.class);
        assertThat(listener.value()).containsExactly(ApplicationReadyEvent.class);
        assertThat(MembershipExpiryReminderJob.class.isAnnotationPresent(ScheduledJob.class)).isTrue();

        job.catchUpOnStartup();
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(task.capture(), eq(clock.instant().plus(MembershipExpiryReminderJob.CATCH_UP_DELAY)));
        verify(reminders, never()).run(any());
        task.getValue().run();
        verify(reminders).run(LocalDate.of(2026, 11, 21));
    }

    @Test
    @DisplayName("TC-F10.1.1-01d: lượt nhắc gói lỗi (chi nhánh thiếu cấu hình hoặc CSDL ngắt) thì thử lại mỗi 5 phút tới khi đạt")
    void retriesEveryFiveMinutesUntilSuccess() {
        doThrow(new IllegalStateException("Lỗi chi nhánh"))
            .doThrow(new IllegalStateException("Lỗi chi nhánh"))
            .doThrow(new IllegalStateException("Lỗi chi nhánh"))
            .doReturn(java.util.List.of())
            .when(reminders).run(any());
        ArgumentCaptor<Runnable> retry = ArgumentCaptor.forClass(Runnable.class);

        job.runDaily();
        verify(scheduler).schedule(retry.capture(), eq(clock.instant().plus(MembershipExpiryReminderJob.RETRY_DELAY)));
        for (int i = 2; i <= 4; i++) {
            clock.advance(MembershipExpiryReminderJob.RETRY_DELAY);
            retry.getValue().run();
            if (i < 4) {
                verify(scheduler, times(i)).schedule(retry.capture(), any(Instant.class));
            }
        }

        verify(reminders, times(4)).run(LocalDate.of(2026, 11, 21));
        verify(scheduler, times(3)).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("TC-F10.1.1-01e: lần thử lại rơi sang ngày Việt Nam mới thì dừng để lượt cron ngày mới chạy")
    void stopsRetryingOnNewDay() {
        doThrow(new IllegalStateException("db down")).when(reminders).run(any());
        ArgumentCaptor<Runnable> retry = ArgumentCaptor.forClass(Runnable.class);
        clock.setVn(LocalDateTime.of(2026, 11, 21, 23, 58));

        job.runDaily();
        verify(scheduler).schedule(retry.capture(), any(Instant.class));
        clock.setVn(LocalDateTime.of(2026, 11, 22, 0, 3));
        retry.getValue().run();

        verify(reminders, times(1)).run(any());
        verify(scheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
    }
}
