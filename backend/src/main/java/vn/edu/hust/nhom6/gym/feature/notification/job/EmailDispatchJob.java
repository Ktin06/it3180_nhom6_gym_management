package vn.edu.hust.nhom6.gym.feature.notification.job;

import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.common.scheduling.ScheduledJob;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.notification.service.EmailDispatchService;

/** Tác vụ định kỳ gửi email thông báo đã đến giờ hẹn (UC-F10.1 bước 5, BR-F10.3), chạy mỗi phút. */
@Slf4j
@ScheduledJob
@RequiredArgsConstructor
public class EmailDispatchJob {
    private final EmailDispatchService dispatcher;
    private final VnTime time;

    @Scheduled(cron = "0 * * * * *", zone = VnTime.ZONE_ID)
    public void run() {
        try {
            dispatcher.dispatchDue(time.now());
        } catch (RuntimeException ex) {
            log.error("Gửi email thông báo gặp lỗi", ex);
        }
    }
}
