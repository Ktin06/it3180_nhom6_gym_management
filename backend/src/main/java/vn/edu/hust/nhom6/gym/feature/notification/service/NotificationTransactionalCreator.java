package vn.edu.hust.nhom6.gym.feature.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationRelatedEntity;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationService.CreatedNotifications;

/**
 * Bao bọc {@link NotificationService#create} trong giao dịch mới độc lập ({@link Propagation#REQUIRES_NEW}).
 *
 * <p>Tách riêng thành bean để {@link NotificationEventListener} gọi qua Spring proxy, đảm bảo lỗi ở một
 * người nhận không ảnh hưởng tới thao tác F8 đã commit và không chặn các hội viên khác.
 */
@Service
@RequiredArgsConstructor
public class NotificationTransactionalCreator {
    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CreatedNotifications createInNewTransaction(
            Long userId,
            NotificationType type,
            String title,
            String content,
            NotificationRelatedEntity relatedEntity,
            Long relatedId,
            Long branchId) {
        return notificationService.create(userId, type, title, content, relatedEntity, relatedId, branchId);
    }
}
