package vn.edu.hust.nhom6.gym.feature.dashboard.dto;

/** Lý do cảnh báo gói tập sắp hết hạn hoặc sắp hết buổi trên Dashboard (F6 UC-F6.4). */
public enum ExpiringWarningReason {
    /** Sắp hết hạn theo ngày (trong 7 ngày tới). */
    EXPIRING_DATE,
    /** Gói SESSION sắp hết buổi (hoặc đã về 0 buổi). */
    LOW_SESSIONS
}
