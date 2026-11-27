package vn.edu.hust.nhom6.gym.feature.notification.job;

import java.time.Duration;
import java.time.LocalDate;

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
 *
 * <p>Nếu lượt chạy gặp lỗi (cấu hình thiếu hoặc CSDL tạm ngắt ở một chi nhánh), thử lại mỗi
 * {@link #RETRY_DELAY} cho tới khi thành công hoặc sang ngày Việt Nam mới (tương tự
 * {@code MembershipLifecycleJob}).
 */
@Slf4j
@ScheduledJob
@RequiredArgsConstructor
public class MembershipExpiryReminderJob {
    static final int WARN_ATTEMPTS = 3;
    static final Duration CATCH_UP_DELAY = Duration.ofMinutes(1);
    static final Duration RETRY_DELAY = Duration.ofMinutes(5);

    private final MembershipExpiryReminderService reminders;
    private final VnTime time;
    private final TaskScheduler scheduler;

    @Scheduled(cron = "0 10 0 * * *", zone = VnTime.ZONE_ID)
    public void runDaily() {
        attempt(time.today(), 1);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        scheduler.schedule(this::runDaily, time.now().toInstant().plus(CATCH_UP_DELAY));
    }

    /** Một lần chạy cho ngày {@code day}; lỗi thì hẹn thử lại, dừng nếu đã sang ngày Việt Nam mới. */
    void attempt(LocalDate day, int attempt) {
        if (!day.equals(time.today())) {
            log.warn("Tác vụ nhắc gói ngày {} dừng thử lại vì đã sang ngày {}", day, time.today());
            return;
        }
        try {
            reminders.run(day);
        } catch (RuntimeException ex) {
            if (attempt >= WARN_ATTEMPTS) {
                log.error("Nhắc gói sắp hết ngày {} lỗi lần {}, thử lại sau {}", day, attempt, RETRY_DELAY, ex);
            } else {
                log.warn("Nhắc gói sắp hết ngày {} lỗi lần {}, thử lại sau {}", day, attempt, RETRY_DELAY, ex);
            }
            scheduler.schedule(() -> attempt(day, attempt + 1), time.now().toInstant().plus(RETRY_DELAY));
        }
    }
}
