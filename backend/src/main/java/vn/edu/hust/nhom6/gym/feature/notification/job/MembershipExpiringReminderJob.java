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
 * Tác vụ nền hằng ngày lúc 08:00 giờ Việt Nam nhắc hội viên có gói sắp hết hạn hoặc sắp hết buổi
 * (F10, UC-F10.1). Chạy bù qua {@link TaskScheduler} khi ứng dụng khởi động (quy ước N0.2).
 */
@Slf4j
@ScheduledJob
@RequiredArgsConstructor
public class MembershipExpiringReminderJob {

    private final NotificationReminderService reminders;
    private final VnTime time;
    private final TaskScheduler scheduler;

    @Scheduled(cron = "0 0 8 * * *", zone = VnTime.ZONE_ID)
    public void runDaily() {
        try {
            int created = reminders.remindExpiringMemberships();
            if (created > 0) {
                log.info("Đã tạo {} thông báo nhắc gói tập sắp hết hạn.", created);
            }
        } catch (RuntimeException ex) {
            log.error("Tác vụ nhắc gói sắp hết hạn gặp lỗi: {}", ex.getMessage(), ex);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        scheduler.schedule(this::runDaily, time.now().toInstant());
    }
}
