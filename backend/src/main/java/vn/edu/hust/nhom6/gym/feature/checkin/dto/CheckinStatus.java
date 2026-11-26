package vn.edu.hust.nhom6.gym.feature.checkin.dto;

/** Trạng thái của một lượt check-in trên danh sách trong ngày (UC-F4.5). */
public enum CheckinStatus {
    /** Đang tập ({@code checked_out_at IS NULL}). */
    CHECKED_IN,
    /** Đã ra về ({@code checked_out_at IS NOT NULL}). */
    CHECKED_OUT
}
