package vn.edu.hust.nhom6.gym.feature.notification.event;

import vn.edu.hust.nhom6.gym.feature.notification.dto.NotificationDto;

/**
 * Sự kiện nội bộ phát khi một dòng thông báo {@code IN_APP} vừa được lưu, để
 * {@code NotificationRealtimePublisher} đẩy qua WebSocket ở pha {@code AFTER_COMMIT}.
 */
public record InAppNotificationCreatedEvent(Long userId, NotificationDto notification) { }
