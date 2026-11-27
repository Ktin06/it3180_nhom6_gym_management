package vn.edu.hust.nhom6.gym.feature.notification.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.common.mail.EmailSender;
import vn.edu.hust.nhom6.gym.feature.notification.entity.Notification;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationQueryRepository;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationQueryRepository.EmailTarget;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationRepository;

/**
 * Quét và gửi các thông báo kênh {@code EMAIL} đã đến {@code scheduled_at} (UC-F10.1 bước 5, BR-F10.3).
 *
 * <p>Mỗi bản ghi được xử lý trong một giao dịch riêng với khóa {@code FOR UPDATE SKIP LOCKED}: kiểm tra lại
 * điều kiện nhắc còn hiệu lực (E5), kiểm tra lại giờ mở cửa chi nhánh (nếu ngoài giờ thì hẹn lại), bỏ qua tài
 * khoản {@code DISABLED} và chặn miền {@code @example.com} ngoài profile {@code local} (ADR 0013).
 */
@Slf4j
@Service
public class EmailDispatchService {
    public static final int BATCH_SIZE = 100;
    static final String BLOCKED_DOMAIN = "@example.com";

    private final NotificationRepository notifications;
    private final NotificationQueryRepository queries;
    private final EmailSender emailSender;
    private final TransactionTemplate tx;
    private final boolean localProfile;

    public EmailDispatchService(
            NotificationRepository notifications,
            NotificationQueryRepository queries,
            EmailSender emailSender,
            PlatformTransactionManager transactionManager,
            Environment environment) {
        this.notifications = notifications;
        this.queries = queries;
        this.emailSender = emailSender;
        this.tx = new TransactionTemplate(transactionManager);
        this.localProfile = environment.acceptsProfiles(Profiles.of("local"));
    }

    /** Kết quả thống kê một lượt gửi email. */
    public record DispatchResult(int sent, int failed, int rescheduled) { }

    private enum Outcome { SENT, FAILED, RESCHEDULED, SKIPPED }

    /** Xử lý tối đa {@link #BATCH_SIZE} email đến hạn tại thời điểm {@code now}. */
    public DispatchResult dispatchDue(OffsetDateTime now) {
        List<Long> ids = queries.findDueEmailIds(now, BATCH_SIZE);
        int sent = 0;
        int failed = 0;
        int rescheduled = 0;
        for (Long id : ids) {
            Outcome outcome;
            try {
                outcome = tx.execute(status -> dispatchOne(id, now));
            } catch (RuntimeException ex) {
                log.error("Xử lý email thông báo {} gặp lỗi", id, ex);
                continue;
            }
            if (outcome == null) {
                continue;
            }
            switch (outcome) {
                case SENT -> sent++;
                case FAILED -> failed++;
                case RESCHEDULED -> rescheduled++;
                case SKIPPED -> { }
            }
        }
        if (!ids.isEmpty()) {
            log.info("Gửi email thông báo lúc {}: {} gửi, {} lỗi, {} hẹn lại", now, sent, failed, rescheduled);
        }
        return new DispatchResult(sent, failed, rescheduled);
    }

    private Outcome dispatchOne(Long id, OffsetDateTime now) {
        Notification row = notifications.lockPendingEmail(id).orElse(null);
        if (row == null) {
            return Outcome.SKIPPED;
        }
        EmailTarget target = queries.findEmailTarget(id, now).orElseThrow();
        if (!target.stillRelevant()) {
            log.info("Bỏ email thông báo {}: điều kiện nhắc không còn hiệu lực", id);
            row.markFailed();
            return Outcome.FAILED;
        }
        if (target.hours() != null && !target.hours().isOpenAt(now)) {
            row.reschedule(target.hours().nextSendAt(now));
            return Outcome.RESCHEDULED;
        }
        if ("DISABLED".equals(target.userStatus())) {
            log.warn("Bỏ email thông báo {}: tài khoản đã bị vô hiệu hóa", id);
            row.markFailed();
            return Outcome.FAILED;
        }
        if (isBlockedAddress(target.email(), localProfile)) {
            log.warn("Bỏ email thông báo {}: địa chỉ {} không gửi ngoài profile local", id, BLOCKED_DOMAIN);
            row.markFailed();
            return Outcome.FAILED;
        }
        try {
            emailSender.send(target.email(), row.getTitle(), row.getContent() == null ? "" : row.getContent());
            row.markSent(now);
            return Outcome.SENT;
        } catch (RuntimeException ex) {
            log.error("Gửi email thông báo {} thất bại: {}", id, ex.getMessage());
            row.markFailed();
            return Outcome.FAILED;
        }
    }

    static boolean isBlockedAddress(String email, boolean localProfile) {
        return !localProfile && email != null && email.toLowerCase(Locale.ROOT).endsWith(BLOCKED_DOMAIN);
    }
}
