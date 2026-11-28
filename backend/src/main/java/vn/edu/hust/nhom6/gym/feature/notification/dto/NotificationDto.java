package vn.edu.hust.nhom6.gym.feature.notification.dto;

import java.time.OffsetDateTime;

import vn.edu.hust.nhom6.gym.feature.notification.entity.Notification;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;

/**
 * Thông báo kênh {@code IN_APP} trả về qua REST API (UC-F10.5) và đẩy thời gian thực qua WebSocket
 * ({@code /user/queue/notifications}).
 */
public record NotificationDto(
        Long id,
        NotificationType type,
        String title,
        String content,
        String relatedEntity,
        Long relatedId,
        boolean read,
        OffsetDateTime readAt,
        OffsetDateTime createdAt) {

    public static NotificationDto of(Notification notification) {
        return new NotificationDto(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getContent(),
            notification.getRelatedEntity() == null ? null : notification.getRelatedEntity().table(),
            notification.getRelatedId(),
            notification.getReadAt() != null,
            notification.getReadAt(),
            notification.getCreatedAt() != null ? notification.getCreatedAt() : notification.getSentAt()
        );
    }
}
