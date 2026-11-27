package vn.edu.hust.nhom6.gym.feature.notification.entity;

/**
 * Trạng thái gửi thông báo (ràng buộc CHECK {@code ck_notifications_status}).
 *
 * <p>Dòng {@code IN_APP} lưu {@link #SENT} ngay lúc tạo; dòng {@code EMAIL} khởi tạo ở {@link #PENDING}
 * rồi được tác vụ gửi email chuyển sang {@link #SENT} hoặc {@link #FAILED}.
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED
}
