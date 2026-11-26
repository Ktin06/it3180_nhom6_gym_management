package vn.edu.hust.nhom6.gym.feature.notification.service;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.notification.dto.NotificationResponse;
import vn.edu.hust.nhom6.gym.feature.notification.dto.UnreadCountResponse;
import vn.edu.hust.nhom6.gym.feature.notification.entity.Notification;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationChannel;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationRepository;

/**
 * Nghiệp vụ tạo cặp thông báo (trong web + email), xem danh sách và đánh dấu đã đọc (F10).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String NOTIFICATION_NOT_FOUND_MESSAGE = "Không tìm thấy thông báo.";
    public static final String NOTIFICATION_FORBIDDEN_MESSAGE =
        "Bạn không có quyền thao tác với thông báo của người khác.";

    private final NotificationRepository notifications;
    private final NotificationRealtimePublisher realtimePublisher;
    private final VnTime vnTime;

    /** Danh sách thông báo kênh {@code IN_APP} của hội viên hiện tại, mới nhất trước (UC-F10.5). */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listMyNotifications(CurrentUser actor, int page, int size) {
        Page<Notification> result = notifications.findByUserIdAndChannelOrderByCreatedAtDescIdDesc(
            actor.userId(), NotificationChannel.IN_APP, PageRequest.of(page, size));
        return PageResponse.of(result, NotificationResponse::from);
    }

    /** Số lượng thông báo {@code IN_APP} chưa đọc của hội viên hiện tại. */
    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(CurrentUser actor) {
        long count = notifications.countByUserIdAndChannelAndReadAtIsNull(
            actor.userId(), NotificationChannel.IN_APP);
        return new UnreadCountResponse(count);
    }

    /**
     * Đánh dấu một thông báo {@code IN_APP} là đã đọc (BR-F10.7, BR-F10.8).
     *
     * @throws NotFoundException (404) khi thông báo không tồn tại
     * @throws ForbiddenException (403) khi thông báo thuộc về tài khoản khác
     */
    @Transactional
    public NotificationResponse markAsRead(CurrentUser actor, Long notificationId) {
        Notification notification = notifications.findByIdAndChannel(notificationId, NotificationChannel.IN_APP)
            .orElseThrow(() -> new NotFoundException(NOTIFICATION_NOT_FOUND_MESSAGE));
        if (!notification.getUserId().equals(actor.userId())) {
            throw new ForbiddenException(NOTIFICATION_FORBIDDEN_MESSAGE);
        }
        if (notification.markRead(vnTime.now())) {
            notification = notifications.save(notification);
        }
        return NotificationResponse.from(notification);
    }

    /**
     * Tạo trong cùng giao dịch hai dòng {@code notifications} theo BR-F10.1: một dòng {@code IN_APP}
     * (đẩy qua WebSocket ngay sau khi commit) và một dòng {@code EMAIL} hẹn gửi theo giờ mở cửa chi
     * nhánh (BR-F10.3).
     */
    @Transactional
    public NotificationResponse createPair(
            Long userId,
            NotificationType type,
            String title,
            String content,
            String relatedEntity,
            Long relatedId,
            LocalTime branchOpenTime,
            LocalTime branchCloseTime) {
        OffsetDateTime now = vnTime.now();
        OffsetDateTime scheduledAt = BranchWindowScheduler.nextAllowedSendTime(
            now, branchOpenTime, branchCloseTime);

        Notification inApp = Notification.inApp(userId, type, title, content, relatedEntity, relatedId, now);
        Notification email = Notification.email(userId, type, title, content, relatedEntity, relatedId, scheduledAt);
        List<Notification> saved = notifications.saveAll(List.of(inApp, email));
        NotificationResponse payload = NotificationResponse.from(saved.getFirst());

        pushAfterCommit(userId, payload);
        return payload;
    }

    private void pushAfterCommit(Long userId, NotificationResponse payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    realtimePublisher.pushToUser(userId, payload);
                }
            });
        } else {
            realtimePublisher.pushToUser(userId, payload);
        }
    }
}
