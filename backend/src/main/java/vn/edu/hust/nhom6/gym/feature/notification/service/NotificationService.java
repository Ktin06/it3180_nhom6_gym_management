package vn.edu.hust.nhom6.gym.feature.notification.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.notification.dto.NotificationDto;
import vn.edu.hust.nhom6.gym.feature.notification.entity.Notification;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationRelatedEntity;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;
import vn.edu.hust.nhom6.gym.feature.notification.event.InAppNotificationCreatedEvent;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationQueryRepository;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationRepository;

/**
 * Dịch vụ tạo cặp thông báo dùng chung của F10 (BR-F10.1, BR-F10.3).
 *
 * <p>Mỗi lần gọi {@link #create} luôn tạo hai dòng {@code notifications} trong cùng giao dịch:
 * dòng {@code IN_APP} ({@code SENT} ngay) và dòng {@code EMAIL} ({@code PENDING}, hẹn gửi trong giờ mở cửa
 * chi nhánh liên quan). Sau khi lưu sẽ phát {@link InAppNotificationCreatedEvent} để
 * {@link NotificationRealtimePublisher} đẩy qua WebSocket ở pha {@code AFTER_COMMIT}.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    public static final int TITLE_MAX_LENGTH = 255;

    private final NotificationRepository notifications;
    private final NotificationQueryRepository queries;
    private final VnTime time;
    private final ApplicationEventPublisher events;

    /** Cặp dòng thông báo vừa được lưu cho một sự kiện. */
    public record CreatedNotifications(Notification inApp, Notification email) { }

    @Transactional
    public CreatedNotifications create(Long userId, NotificationType type, String title, String content,
            NotificationRelatedEntity relatedEntity, Long relatedId, Long branchId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(branchId, "branchId");
        if (title == null || title.isBlank() || title.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("Tiêu đề thông báo phải có từ 1 đến " + TITLE_MAX_LENGTH + " ký tự");
        }
        if ((relatedEntity == null) != (relatedId == null)) {
            throw new IllegalArgumentException("relatedEntity và relatedId phải cùng có hoặc cùng rỗng");
        }
        OpeningHours hours = queries.findOpeningHours(branchId)
            .orElseThrow(() -> new IllegalArgumentException("Không có chi nhánh " + branchId));
        OffsetDateTime now = time.now();
        Notification inApp = Notification.inApp(userId, type, title, content, relatedEntity, relatedId, now);
        Notification email = Notification.email(userId, type, title, content, relatedEntity, relatedId,
            hours.nextSendAt(now));
        notifications.saveAll(List.of(inApp, email));
        events.publishEvent(new InAppNotificationCreatedEvent(userId, NotificationDto.of(inApp)));
        return new CreatedNotifications(inApp, email);
    }
}
