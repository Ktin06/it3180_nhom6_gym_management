package vn.edu.hust.nhom6.gym.feature.notification.job;

import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.common.scheduling.ScheduledJob;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.notification.service.ClassReminderService;

/** Tác vụ định kỳ nhắc lịch lớp học nhóm đã đặt (UC-F10.2), chạy mỗi 15 phút theo giờ Việt Nam. */
@Slf4j
@ScheduledJob
@RequiredArgsConstructor
public class ClassReminderJob {
    private final ClassReminderService reminders;
    private final VnTime time;

    @Scheduled(cron = "0 */15 * * * *", zone = VnTime.ZONE_ID)
    public void run() {
        try {
            reminders.run(time.now());
        } catch (RuntimeException ex) {
            log.error("Nhắc lịch lớp gặp lỗi", ex);
        }
    }
}
