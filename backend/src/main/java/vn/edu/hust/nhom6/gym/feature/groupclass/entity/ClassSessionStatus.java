package vn.edu.hust.nhom6.gym.feature.groupclass.entity;

/**
 * Trạng thái buổi học ({@code class_sessions.status}). Tác vụ sinh buổi chỉ tạo SCHEDULED; RESCHEDULED và
 * CANCELLED do đổi giờ, hủy buổi (S2-17.1); COMPLETED do điểm danh.
 */
public enum ClassSessionStatus {
    SCHEDULED,
    RESCHEDULED,
    CANCELLED,
    COMPLETED
}
