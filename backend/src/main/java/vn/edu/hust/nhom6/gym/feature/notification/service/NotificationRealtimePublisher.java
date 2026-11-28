package vn.edu.hust.nhom6.gym.feature.notification.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.feature.notification.dto.NotificationDto;
import vn.edu.hust.nhom6.gym.feature.notification.event.InAppNotificationCreatedEvent;

/**
 * Đẩy thông báo {@code IN_APP} mới tới trình duyệt đang mở qua WebSocket STOMP
 * ({@code /user/queue/notifications}) ở pha {@link TransactionPhase#AFTER_COMMIT}.
 *
 * <p>Lỗi đẩy tin chỉ ghi log cảnh báo, không ảnh hưởng dữ liệu đã lưu trong cơ sở dữ liệu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRealtimePublisher {
    public static final String QUEUE_NOTIFICATIONS = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInAppCreated(InAppNotificationCreatedEvent event) {
        push(event.userId(), event.notification());
    }

    /** Đẩy một thông báo tới đúng tài khoản {@code userId} trên kênh riêng {@code /user/queue/notifications}. */
    public void push(Long userId, NotificationDto dto) {
        if (userId == null || dto == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), QUEUE_NOTIFICATIONS, dto);
        } catch (Exception ex) {
            log.warn("Không đẩy được thông báo WebSocket {} tới userId={}", dto.id(), userId, ex);
        }
    }
}
