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
 * Tác vụ nền chạy mỗi 15 phút nhắc hội viên về lớp học nhóm đã đặt sắp bắt đầu (F10, UC-F10.2).
 */
@Slf4j
@ScheduledJob
@RequiredArgsConstructor
public class ClassReminderJob {

    private final NotificationReminderService reminders;
    private final VnTime time;
    private final TaskScheduler scheduler;

    @Scheduled(cron = "0 */15 * * * *", zone = VnTime.ZONE_ID)
    public void run() {
        try {
            int created = reminders.remindUpcomingClasses();
            if (created > 0) {
                log.info("Đã tạo {} thông báo nhắc lịch lớp học.", created);
            }
        } catch (RuntimeException ex) {
            log.error("Tác vụ nhắc lịch lớp học gặp lỗi: {}", ex.getMessage(), ex);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        scheduler.schedule(this::run, time.now().toInstant());
    }
}
