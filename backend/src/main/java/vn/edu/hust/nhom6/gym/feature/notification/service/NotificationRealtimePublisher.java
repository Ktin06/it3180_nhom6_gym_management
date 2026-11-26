package vn.edu.hust.nhom6.gym.feature.notification.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.feature.notification.config.WebSocketConfig;
import vn.edu.hust.nhom6.gym.feature.notification.dto.NotificationResponse;

/**
 * Đẩy thông báo trong web tới phiên WebSocket (STOMP) của hội viên đang mở trình duyệt (UC-F10.5 A3).
 *
 * <p>Mọi lỗi đẩy WebSocket chỉ ghi log, không làm rollback giao dịch lưu {@code notifications}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRealtimePublisher {

    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;

    public void pushToUser(Long userId, NotificationResponse payload) {
        SimpMessagingTemplate template = messagingTemplateProvider.getIfAvailable();
        if (template == null || userId == null || payload == null) {
            return;
        }
        try {
            template.convertAndSendToUser(
                String.valueOf(userId),
                WebSocketConfig.USER_NOTIFICATIONS_DESTINATION,
                payload
            );
        } catch (RuntimeException ex) {
            log.warn("Không đẩy được thông báo id={} qua WebSocket cho userId={}: {}",
                payload.id(), userId, ex.getMessage());
        }
    }
}
