package vn.edu.hust.nhom6.gym.feature.groupclass.entity;

/**
 * Trạng thái chỗ đặt ({@code class_bookings.status}, CHECK sau V9_03). Chỉ {@link #CANCELLED} không chiếm chỗ; sĩ số
 * tính {@code BOOKED}, {@code ATTENDED}, {@code NO_SHOW} (BR-F8.1-03). Không có danh sách chờ (ADR 0014).
 */
public enum ClassBookingStatus {
    BOOKED,
    CANCELLED,
    ATTENDED,
    NO_SHOW
}
