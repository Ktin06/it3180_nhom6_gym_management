package vn.edu.hust.nhom6.gym;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Dọn dữ liệu giữa các kiểm thử tích hợp mà <b>không làm mất {@code system_configs}</b>.
 *
 * <p>Vì sao cần: {@code system_configs.updated_by} có khóa ngoại trỏ {@code users}. Lệnh
 * {@code TRUNCATE users ... CASCADE} vì thế xóa luôn mọi dòng cấu hình (TTL hóa đơn chờ, cửa sổ đặt lớp, ...).
 * Kiểm thử chạy sau mà đọc cấu hình qua {@code SystemConfigService} sẽ lỗi "thiếu tham số", và chỉ lỗi tùy
 * thứ tự chạy nên rất khó tìm.
 *
 * <p>Cách làm: đọc các dòng cấu hình đang có, chạy {@code TRUNCATE}, rồi ghi lại đúng các dòng đó
 * ({@code updated_by} để rỗng vì tài khoản đã bị xóa). Không chép giá trị từ migration, nên migration sau có
 * thêm, sửa hay bỏ khóa nào thì dữ liệu kiểm thử vẫn khớp.
 *
 * <p>Ví dụ: {@code DatabaseCleaner.truncate(jdbc, "members", "users", "staff", "refresh_tokens");}
 */
public final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    /** {@code TRUNCATE TABLE <tables> RESTART IDENTITY CASCADE}, giữ nguyên các dòng {@code system_configs}. */
    public static void truncate(JdbcTemplate jdbc, String... tables) {
        if (tables.length == 0) {
            throw new IllegalArgumentException("Cần ít nhất một bảng để dọn.");
        }
        List<Map<String, Object>> configs = jdbc.queryForList(
            "SELECT branch_id, config_key, config_value, description FROM system_configs ORDER BY id");
        jdbc.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
        for (Map<String, Object> row : configs) {
            jdbc.update("""
                INSERT INTO system_configs (branch_id, config_key, config_value, description)
                VALUES (?, ?, ?, ?)
                ON CONFLICT ON CONSTRAINT uq_system_configs_branch_key DO NOTHING""",
                row.get("branch_id"), row.get("config_key"), row.get("config_value"), row.get("description"));
        }
    }
}
