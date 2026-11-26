package vn.edu.hust.nhom6.gym.feature.notification.job;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.common.scheduling.ScheduledJob;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationReminderService;

/**
 * Tác vụ nền chạy mỗi 5 phút quét và gửi các thông báo kênh {@code EMAIL} đang {@code PENDING} đã
 * đến {@code scheduled_at} (trong giờ mở cửa chi nhánh, BR-F10.3).
 */
@Slf4j
@ScheduledJob
@RequiredArgsConstructor
public class NotificationEmailDispatchJob {

    private final NotificationReminderService reminders;
    private final VnTime time;
    private final TaskScheduler scheduler;

    @Scheduled(cron = "0 */5 * * * *", zone = VnTime.ZONE_ID)
    public void run() {
        try {
            int sent = reminders.dispatchPendingEmails();
            if (sent > 0) {
                log.info("Đã gửi {} email thông báo F10.", sent);
            }
        } catch (RuntimeException ex) {
            log.error("Tác vụ gửi email thông báo F10 gặp lỗi: {}", ex.getMessage(), ex);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        scheduler.schedule(this::run, time.now().toInstant());
    }
}
