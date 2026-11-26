package vn.edu.hust.nhom6.gym.feature.notification.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;

/**
 * Thông báo gửi cho hội viên qua kênh trong web ({@code IN_APP}) hoặc email ({@code EMAIL}) (F10).
 *
 * <p>Khớp bảng {@code notifications} ở {@code V1_05__create_notifications.sql}.
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "related_entity", length = 50)
    private String relatedEntity;

    @Column(name = "related_id")
    private Long relatedId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    public static Notification inApp(Long userId, NotificationType type, String title, String content,
            String relatedEntity, Long relatedId, OffsetDateTime sentAt) {
        Notification n = new Notification();
        n.userId = userId;
        n.type = type.name();
        n.title = title;
        n.content = content;
        n.channel = NotificationChannel.IN_APP;
        n.relatedEntity = relatedEntity;
        n.relatedId = relatedId;
        n.status = NotificationStatus.SENT;
        n.scheduledAt = sentAt;
        n.sentAt = sentAt;
        return n;
    }

    public static Notification email(Long userId, NotificationType type, String title, String content,
            String relatedEntity, Long relatedId, OffsetDateTime scheduledAt) {
        Notification n = new Notification();
        n.userId = userId;
        n.type = type.name();
        n.title = title;
        n.content = content;
        n.channel = NotificationChannel.EMAIL;
        n.relatedEntity = relatedEntity;
        n.relatedId = relatedId;
        n.status = NotificationStatus.PENDING;
        n.scheduledAt = scheduledAt;
        return n;
    }

    public void markSent(OffsetDateTime sentAt) {
        this.status = NotificationStatus.SENT;
        this.sentAt = sentAt;
    }

    public void markFailed() {
        this.status = NotificationStatus.FAILED;
    }

    public boolean markRead(OffsetDateTime now) {
        if (this.readAt != null) {
            return false;
        }
        this.readAt = now;
        return true;
    }
}
