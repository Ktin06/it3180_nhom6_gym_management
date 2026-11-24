package vn.edu.hust.nhom6.gym.feature.membership.job;

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
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipLifecycleService;

/**
 * Chuyển trạng thái gói hằng ngày lúc 00:00 giờ Việt Nam: EXPIRED trước, UPCOMING sau (AC-F3.5.1-10, BR-F4.7). Chạy bù
 * mỗi khi ứng dụng khởi động, nên máy chủ tắt qua đêm (hay nhiều ngày) thì bật lên là gói được xử lý theo ngày hiện tại.
 *
 * <p>Lượt chạy lỗi (khóa lạc quan khi có thao tác cùng lúc, deadlock, CSDL tạm ngắt) thì giao dịch rollback cả lượt;
 * thử lại mỗi {@link #RETRY_DELAY} cho tới khi xong hoặc sang ngày Việt Nam mới (lượt cron mới thay thế). Không thử lại
 * thì hội viên phải đợi tới 00:00 hôm sau mới có gói nối tiếp ACTIVE. Từ lần thứ {@link #WARN_ATTEMPTS} ghi log ERROR.
 *
 * <p>Tắt trong kiểm thử ({@code app.scheduling.enabled=false}); kiểm thử gọi thẳng
 * {@link MembershipLifecycleService#runDaily}.
 */
@Slf4j
@ScheduledJob
@RequiredArgsConstructor
public class MembershipLifecycleJob {
    static final int WARN_ATTEMPTS = 3;
    static final Duration RETRY_DELAY = Duration.ofMinutes(5);

    private final MembershipLifecycleService lifecycle;
    private final VnTime time;
    private final TaskScheduler scheduler;

    @Scheduled(cron = "0 0 0 * * *", zone = VnTime.ZONE_ID)
    public void runDaily() {
        attempt(time.today(), 1);
    }

    /** Chạy bù khi khởi động: giao cho {@code TaskScheduler} một luồng, không chồng với lượt cron (quy ước N0.2). */
    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        scheduler.schedule(this::runDaily, time.now().toInstant());
    }

    /** Một lần chạy cho ngày {@code day}; lỗi thì hẹn lần sau, trừ khi đã sang ngày khác. */
    void attempt(LocalDate day, int attempt) {
        if (!day.equals(time.today())) {
            log.warn("Tác vụ gói ngày {} dừng thử lại vì đã sang ngày {}", day, time.today());
            return;
        }
        try {
            lifecycle.runDaily(day);
        } catch (RuntimeException e) {
            if (attempt >= WARN_ATTEMPTS) {
                log.error("Tác vụ gói ngày {} lỗi lần {}, thử lại sau {}", day, attempt, RETRY_DELAY, e);
            } else {
                log.warn("Tác vụ gói ngày {} lỗi lần {}, thử lại sau {}", day, attempt, RETRY_DELAY, e);
            }
            scheduler.schedule(() -> attempt(day, attempt + 1), time.now().toInstant().plus(RETRY_DELAY));
        }
    }
}
