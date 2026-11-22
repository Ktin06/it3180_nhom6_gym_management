package vn.edu.hust.nhom6.gym;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigKeys;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigService;

/** Hàm dọn dữ liệu dùng chung giữ nguyên system_configs (VD-11); mỗi kiểm thử rollback nên không để lại dữ liệu. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class DatabaseCleanerTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SystemConfigService configs;

    @Test
    @DisplayName("Nền tảng: TRUNCATE users kèm CASCADE xóa luôn system_configs (lý do cần DatabaseCleaner)")
    void truncateThuongXoaLuonCauHinh() {
        assertThat(soDongCauHinh()).isPositive();

        jdbc.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        assertThat(soDongCauHinh()).isZero();
    }

    @Test
    @DisplayName("Nền tảng: DatabaseCleaner dọn users mà vẫn giữ mọi dòng cấu hình toàn chuỗi, đọc lại được giá trị cũ")
    void giuNguyenCauHinhToanChuoi() {
        int truoc = soDongCauHinh();
        int ttl = configs.getInt(null, SystemConfigKeys.PENDING_INVOICE_TTL_DAYS);

        DatabaseCleaner.truncate(jdbc, "members", "users", "staff", "refresh_tokens");

        assertThat(soDongCauHinh()).isEqualTo(truoc);
        assertThat(configs.getInt(null, SystemConfigKeys.PENDING_INVOICE_TTL_DAYS)).isEqualTo(ttl);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isZero();
    }

    @Test
    @DisplayName("Nền tảng: DatabaseCleaner giữ cả dòng cấu hình ghi đè theo chi nhánh")
    void giuNguyenCauHinhChiNhanh() {
        Long branchId = jdbc.queryForObject(
            "INSERT INTO branches (code, name, open_time, close_time) VALUES ('DCL01', 'Chi nhanh DCL01', '06:00', '22:00') "
                + "RETURNING id", Long.class);
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (?, ?, '9')",
            branchId, SystemConfigKeys.PENDING_INVOICE_TTL_DAYS);

        DatabaseCleaner.truncate(jdbc, "users", "packages");

        assertThat(configs.getInt(branchId, SystemConfigKeys.PENDING_INVOICE_TTL_DAYS)).isEqualTo(9);
    }

    private int soDongCauHinh() {
        return jdbc.queryForObject("SELECT count(*) FROM system_configs", Integer.class);
    }
}
