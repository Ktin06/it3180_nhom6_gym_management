package vn.edu.hust.nhom6.gym.feature.notification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.notification.dto.NotificationDto;
import vn.edu.hust.nhom6.gym.feature.notification.dto.UnreadNotificationCountDto;
import vn.edu.hust.nhom6.gym.feature.notification.entity.Notification;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationChannel;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationRepository;

/**
 * Nghiệp vụ xem danh sách, đếm chưa đọc và đánh dấu đã đọc thông báo {@code IN_APP} của hội viên hiện tại
 * (UC-F10.5, BR-F10.7, BR-F10.8). Luôn dùng {@code me.userId()} từ JWT và kiểm tra quyền sở hữu.
 */
@Service
@RequiredArgsConstructor
public class MemberNotificationService {
    private static final String NOTIFICATION_NOT_FOUND = "Thông báo không tồn tại.";

    private final NotificationRepository notifications;
    private final VnTime time;

    /** Danh sách thông báo {@code IN_APP} của chính người gọi, mới nhất trước, phân trang. */
    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(CurrentUser me, int page, int size) {
        Page<Notification> result = notifications.findByUserIdAndChannelOrderByCreatedAtDescIdDesc(
            me.userId(), NotificationChannel.IN_APP, PageRequest.of(page, size));
        return PageResponse.of(result, NotificationDto::of);
    }

    /** Đếm số thông báo {@code IN_APP} chưa đọc của chính người gọi. */
    @Transactional(readOnly = true)
    public UnreadNotificationCountDto unreadCount(CurrentUser me) {
        long count = notifications.countByUserIdAndChannelAndReadAtIsNull(me.userId(), NotificationChannel.IN_APP);
        return new UnreadNotificationCountDto(count);
    }

    /**
     * Đánh dấu một thông báo {@code IN_APP} là đã đọc (UC-F10.5 A2, BR-F10.7, BR-F10.8).
     *
     * @throws NotFoundException  (404) khi thông báo không tồn tại hoặc thuộc kênh {@code EMAIL}
     * @throws ForbiddenException (403) khi thông báo thuộc về tài khoản khác
     */
    @Transactional
    public NotificationDto markRead(CurrentUser me, Long notificationId) {
        Notification notification = notifications.findById(notificationId)
            .filter(n -> n.getChannel() == NotificationChannel.IN_APP)
            .orElseThrow(() -> new NotFoundException(NOTIFICATION_NOT_FOUND));
        if (!notification.getUserId().equals(me.userId())) {
            throw new ForbiddenException("Bạn không có quyền truy cập thông báo của người khác.");
        }
        notification.markRead(time.now());
        return NotificationDto.of(notifications.save(notification));
    }
}
