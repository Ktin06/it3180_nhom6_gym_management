package vn.edu.hust.nhom6.gym.feature.notification.job;

import java.time.OffsetDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.common.scheduling.ScheduledJob;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.notification.service.EmailDispatchService;

/**
 * Tác vụ định kỳ gửi email thông báo đã đến giờ hẹn (UC-F10.1 bước 5, BR-F10.3), chạy mỗi phút.
 *
 * <p>Đẩy việc gọi Brevo sang luồng thực thi riêng (1 luồng) để không bao giờ chặn luồng {@code TaskScheduler}
 * duy nhất của ứng dụng (ví dụ tác vụ chuyển gói lúc 00:00). Nếu lượt trước chưa xong thì bỏ qua lượt hiện tại.
 */
@Slf4j
@ScheduledJob
public class EmailDispatchJob {
    private final EmailDispatchService dispatcher;
    private final VnTime time;
    private final ExecutorService executor;
    private final boolean ownedExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    public EmailDispatchJob(EmailDispatchService dispatcher, VnTime time) {
        this(dispatcher, time, Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "email-dispatch-1");
            t.setDaemon(true);
            return t;
        }), true);
    }

    EmailDispatchJob(EmailDispatchService dispatcher, VnTime time, ExecutorService executor) {
        this(dispatcher, time, executor, false);
    }

    private EmailDispatchJob(EmailDispatchService dispatcher, VnTime time, ExecutorService executor, boolean ownedExecutor) {
        this.dispatcher = dispatcher;
        this.time = time;
        this.executor = executor;
        this.ownedExecutor = ownedExecutor;
    }

    @Scheduled(cron = "0 * * * * *", zone = VnTime.ZONE_ID)
    public void run() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Lượt gửi email trước chưa kết thúc, bỏ qua lượt này");
            return;
        }
        OffsetDateTime now = time.now();
        try {
            executor.execute(() -> {
                try {
                    dispatcher.dispatchDue(now);
                } catch (RuntimeException ex) {
                    log.error("Gửi email thông báo gặp lỗi", ex);
                } finally {
                    running.set(false);
                }
            });
        } catch (RuntimeException ex) {
            running.set(false);
            log.error("Không gửi được lượt gửi email sang luồng riêng", ex);
        }
    }

    @PreDestroy
    void shutdown() {
        if (ownedExecutor) {
            executor.shutdownNow();
        }
    }
}
