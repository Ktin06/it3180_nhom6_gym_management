package vn.edu.hust.nhom6.gym.feature.notification.job;

import java.time.Duration;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.common.scheduling.ScheduledJob;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.notification.service.MembershipExpiryReminderService;

/**
 * Tác vụ định kỳ nhắc gói tập sắp hết hạn/sắp hết buổi (UC-F10.1): chạy 00:10 giờ Việt Nam (sau tác vụ
 * vòng đời gói lúc 00:00) và chạy bù sau khi ứng dụng khởi động 1 phút.
 */
@Slf4j
@ScheduledJob
@RequiredArgsConstructor
public class MembershipExpiryReminderJob {
    static final Duration CATCH_UP_DELAY = Duration.ofMinutes(1);

    private final MembershipExpiryReminderService reminders;
    private final VnTime time;
    private final TaskScheduler scheduler;

    @Scheduled(cron = "0 10 0 * * *", zone = VnTime.ZONE_ID)
    public void runDaily() {
        try {
            reminders.run(time.today());
        } catch (RuntimeException ex) {
            log.error("Nhắc gói sắp hết ngày {} gặp lỗi", time.today(), ex);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        scheduler.schedule(this::runDaily, time.now().toInstant().plus(CATCH_UP_DELAY));
    }
}
