package vn.edu.hust.nhom6.gym.feature.groupclass.entity;

/**
 * Lý do hủy chỗ ({@code class_bookings.cancel_reason}). Hội viên tự hủy: {@link #BY_MEMBER}; lễ tân hủy hộ:
 * {@link #BY_STAFF}; chi nhánh hủy buổi (S2-17.1): {@link #SESSION_CANCELLED}, không phạt (BR-F8.7-05).
 */
public enum CancelReason {
    BY_MEMBER,
    BY_STAFF,
    SESSION_CANCELLED
}
