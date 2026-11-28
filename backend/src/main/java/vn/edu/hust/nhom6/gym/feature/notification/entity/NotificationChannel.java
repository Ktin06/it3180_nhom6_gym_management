package vn.edu.hust.nhom6.gym.feature.notification.entity;

/**
 * Kênh gửi thông báo (ràng buộc CHECK {@code ck_notifications_channel}).
 * Mỗi sự kiện tạo một dòng {@code IN_APP} và một dòng {@code EMAIL} (BR-F10.1).
 */
public enum NotificationChannel {
    /** Thông báo hiển thị trong cổng hội viên. */
    IN_APP,
    /** Email gửi tới {@code users.email} trong giờ mở cửa chi nhánh (BR-F10.3). */
    EMAIL
}
