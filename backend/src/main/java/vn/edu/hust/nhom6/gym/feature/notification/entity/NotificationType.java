package vn.edu.hust.nhom6.gym.feature.notification.entity;

/**
 * Loại thông báo trong cột {@code notifications.type}.
 *
 * <p>Không còn {@code CLASS_PROMOTED} do ADR 0014 đã bỏ danh sách chờ lớp học nhóm và UC-F10.3.
 */
public enum NotificationType {
    /** UC-F10.1: gói tập sắp hết hạn hoặc sắp hết buổi ({@code related_entity = memberships}). */
    MEMBERSHIP_EXPIRING,
    /** UC-F10.2: nhắc lịch lớp học nhóm đã đặt còn tối đa 24 giờ ({@code related_entity = class_bookings}). */
    CLASS_REMINDER,
    /** UC-F10.4: buổi lớp nhóm đổi giờ ({@code related_entity = class_sessions}). */
    CLASS_RESCHEDULED,
    /** Buổi lớp nhóm đổi phòng ({@code related_entity = class_sessions}). */
    CLASS_ROOM_CHANGED,
    /** Buổi lớp nhóm bị hủy ({@code related_entity = class_sessions}). */
    CLASS_CANCELLED,
    /** Buổi tập PT bị hủy ({@code related_entity = pt_sessions}). */
    PT_SESSION_CANCELLED
}
