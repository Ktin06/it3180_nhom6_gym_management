package vn.edu.hust.nhom6.gym.feature.notification.dto;

import java.time.OffsetDateTime;

import vn.edu.hust.nhom6.gym.feature.notification.entity.Notification;

/** Dữ liệu một thông báo trong web trả cho hội viên qua REST và WebSocket (UC-F10.5). */
public record NotificationResponse(
    Long id,
    String type,
    String title,
    String content,
    String relatedEntity,
    Long relatedId,
    OffsetDateTime readAt,
    OffsetDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
            n.getId(),
            n.getType(),
            n.getTitle(),
            n.getContent(),
            n.getRelatedEntity(),
            n.getRelatedId(),
            n.getReadAt(),
            n.getCreatedAt()
        );
    }
}
