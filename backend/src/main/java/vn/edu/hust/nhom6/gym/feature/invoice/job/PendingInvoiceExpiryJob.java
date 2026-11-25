package vn.edu.hust.nhom6.gym.feature.invoice.job;

import java.time.Instant;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.scheduling.ScheduledJob;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.invoice.service.PortalInvoiceService;

/**
 * Tự hủy hóa đơn chờ cổng hội viên quá {@code PENDING_INVOICE_TTL_DAYS} (BR-F7.5). Hạn tính theo thời điểm
 * ({@code created_at + N ngày}), không theo ngày, nên chạy mỗi giờ: hóa đơn quá hạn bị hủy chậm nhất một giờ sau.
 * Không phụ thuộc thứ tự với tác vụ gói EXPIRED/UPCOMING (chỉ đụng gói PENDING).
 */
@ScheduledJob
@RequiredArgsConstructor
public class PendingInvoiceExpiryJob {
    private final PortalInvoiceService portalInvoices;
    private final TaskScheduler scheduler;

    @Scheduled(cron = "0 0 * * * *", zone = VnTime.ZONE_ID)
    public void runHourly() {
        portalInvoices.cancelExpired();
    }

    /** Chạy bù khi khởi động (máy chủ tắt qua đêm thì hóa đơn quá hạn vẫn được hủy ngay khi bật lại). */
    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        scheduler.schedule(this::runHourly, Instant.now());
    }
}
