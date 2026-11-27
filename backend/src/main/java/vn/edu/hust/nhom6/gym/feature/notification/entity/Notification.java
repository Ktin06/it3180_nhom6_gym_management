package vn.edu.hust.nhom6.gym.feature.notification.entity;

import java.time.OffsetDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;

/**
 * Bản ghi bảng {@code notifications} (F10).
 *
 * <p>Không mở setter công khai: tạo mới qua {@link #inApp} hoặc {@link #email}, chuyển trạng thái gửi email
 * qua {@link #markSent}, {@link #markFailed}, {@link #reschedule}.
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Convert(converter = NotificationRelatedEntity.JpaConverter.class)
    @Column(name = "related_entity", length = 50)
    private NotificationRelatedEntity relatedEntity;

    @Column(name = "related_id")
    private Long relatedId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    /** Thời điểm sớm nhất được gửi của dòng {@code EMAIL}, luôn nằm trong giờ mở cửa chi nhánh (BR-F10.3). */
    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    /** Tạo dòng thông báo trong web ({@code SENT} ngay khi lưu, {@code scheduled_at} rỗng). */
    public static Notification inApp(Long userId, NotificationType type, String title, String content,
            NotificationRelatedEntity relatedEntity, Long relatedId, OffsetDateTime now) {
        Notification n = base(userId, type, title, content, relatedEntity, relatedId);
        n.channel = NotificationChannel.IN_APP;
        n.status = NotificationStatus.SENT;
        n.sentAt = now;
        return n;
    }

    /** Tạo dòng thông báo email ({@code PENDING}, hẹn gửi tại {@code scheduledAt}). */
    public static Notification email(Long userId, NotificationType type, String title, String content,
            NotificationRelatedEntity relatedEntity, Long relatedId, OffsetDateTime scheduledAt) {
        Notification n = base(userId, type, title, content, relatedEntity, relatedId);
        n.channel = NotificationChannel.EMAIL;
        n.status = NotificationStatus.PENDING;
        n.scheduledAt = scheduledAt;
        return n;
    }

    private static Notification base(Long userId, NotificationType type, String title, String content,
            NotificationRelatedEntity relatedEntity, Long relatedId) {
        Notification n = new Notification();
        n.userId = userId;
        n.type = type;
        n.title = title;
        n.content = content;
        n.relatedEntity = relatedEntity;
        n.relatedId = relatedId;
        return n;
    }

    /** Ghi nhận email đã gửi thành công. */
    public void markSent(OffsetDateTime at) {
        requirePendingEmail();
        this.status = NotificationStatus.SENT;
        this.sentAt = at;
    }

    /** Ghi nhận email gửi thất bại hoặc bị bỏ qua (không thử lại theo SRS F10 E1). */
    public void markFailed() {
        requirePendingEmail();
        this.status = NotificationStatus.FAILED;
    }

    /** Hẹn lại thời điểm gửi email tới giờ mở cửa kế tiếp khi lúc gửi đã ngoài giờ mở cửa (BR-F10.3). */
    public void reschedule(OffsetDateTime at) {
        requirePendingEmail();
        this.scheduledAt = at;
    }

    /**
     * Đánh dấu thông báo kênh {@code IN_APP} đã đọc (BR-F10.8). Nếu đã có {@code read_at} thì giữ nguyên
     * thời điểm đọc ban đầu (idempotent).
     */
    public void markRead(OffsetDateTime at) {
        Objects.requireNonNull(at, "at");
        if (channel != NotificationChannel.IN_APP) {
            throw new IllegalStateException("Chỉ đánh dấu đã đọc cho dòng IN_APP, dòng " + getId());
        }
        if (this.readAt == null) {
            this.readAt = at;
        }
    }

    private void requirePendingEmail() {
        if (channel != NotificationChannel.EMAIL || status != NotificationStatus.PENDING) {
            throw new IllegalStateException("Chỉ đổi được dòng EMAIL đang PENDING, dòng " + getId());
        }
    }
}
