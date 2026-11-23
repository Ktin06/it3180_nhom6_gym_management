package vn.edu.hust.nhom6.gym.feature.systemconfig.service;

/**
 * Tên các khóa đang có trong bảng system_configs (V1_06; V9_03 đổi tên và bỏ khóa danh sách chờ
 * theo ADR 0014). Dùng hằng thay vì gõ chuỗi để tránh sai chính tả. Thêm khóa mới phải có migration.
 */
public final class SystemConfigKeys {
    public static final String BOOKING_WINDOW_DAYS = "BOOKING_WINDOW_DAYS";
    public static final String BOOKING_CUTOFF_MIN = "BOOKING_CUTOFF_MIN";
    public static final String LATE_CANCEL_HOURS = "LATE_CANCEL_HOURS";
    public static final String PENALTY_LIMIT_PER_MONTH = "PENALTY_LIMIT_PER_MONTH";
    public static final String PENALTY_LOCK_DAYS = "PENALTY_LOCK_DAYS";
    public static final String PT_MIN_LEAD_HOURS = "PT_MIN_LEAD_HOURS";
    public static final String FREEZE_MIN_DAYS = "FREEZE_MIN_DAYS";
    public static final String FREEZE_MAX_DAYS = "FREEZE_MAX_DAYS";
    public static final String FREEZE_MAX_TIMES = "FREEZE_MAX_TIMES";
    public static final String TRANSFER_MIN_DAYS = "TRANSFER_MIN_DAYS";
    public static final String TRANSFER_FEE = "TRANSFER_FEE";
    public static final String PENDING_INVOICE_TTL_DAYS = "PENDING_INVOICE_TTL_DAYS";
    public static final String SESSION_WARNING_THRESHOLD = "SESSION_WARNING_THRESHOLD";

    private SystemConfigKeys() {
    }
}
