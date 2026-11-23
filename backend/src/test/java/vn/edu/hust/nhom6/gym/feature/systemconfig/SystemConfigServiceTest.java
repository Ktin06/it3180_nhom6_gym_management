package vn.edu.hust.nhom6.gym.feature.systemconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigKeys;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigService;

/** Đọc system_configs trên PostgreSQL thật; mỗi kiểm thử rollback nên không để lại dữ liệu. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class SystemConfigServiceTest {

    @Autowired
    private SystemConfigService service;

    @Autowired
    private JdbcTemplate jdbc;

    private Long branchId;
    private Long otherBranchId;

    @BeforeEach
    void taoHaiChiNhanh() {
        branchId = taoChiNhanh("CFG01");
        otherBranchId = taoChiNhanh("CFG02");
    }

    @Test
    @DisplayName("Nền tảng: không có dòng chi nhánh thì dùng giá trị toàn chuỗi")
    void khongCoDongChiNhanhThiDungToanChuoi() {
        assertThat(service.getInt(branchId, SystemConfigKeys.PENDING_INVOICE_TTL_DAYS)).isEqualTo(3);
        assertThat(service.getInt(null, SystemConfigKeys.TRANSFER_FEE)).isEqualTo(200000);
    }

    @Test
    @DisplayName("Nền tảng: dòng chi nhánh ghi đè dòng toàn chuỗi, chỉ cho đúng chi nhánh đó")
    void dongChiNhanhGhiDeDongToanChuoi() {
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (?, ?, ?)",
            branchId, SystemConfigKeys.LATE_CANCEL_HOURS, "4");

        assertThat(service.getInt(branchId, SystemConfigKeys.LATE_CANCEL_HOURS)).isEqualTo(4);
        assertThat(service.getInt(otherBranchId, SystemConfigKeys.LATE_CANCEL_HOURS)).isEqualTo(2);
        assertThat(service.getInt(null, SystemConfigKeys.LATE_CANCEL_HOURS)).isEqualTo(2);
    }

    @Test
    @DisplayName("Nền tảng: thiếu khóa thì báo lỗi nêu tên khóa")
    void thieuKhoaThiBaoLoi() {
        assertThatThrownBy(() -> service.getString(branchId, "KHOA_KHONG_CO"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Thiếu tham số cấu hình KHOA_KHONG_CO");
    }

    @Test
    @DisplayName("Nền tảng: giá trị không phải số nguyên thì getInt báo lỗi nêu tên khóa")
    void giaTriSaiKieuThiBaoLoi() {
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (?, ?, ?)",
            branchId, SystemConfigKeys.FREEZE_MAX_TIMES, "hai");

        assertThat(service.getString(branchId, SystemConfigKeys.FREEZE_MAX_TIMES)).isEqualTo("hai");
        assertThatThrownBy(() -> service.getInt(branchId, SystemConfigKeys.FREEZE_MAX_TIMES))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("FREEZE_MAX_TIMES");
    }

    private Long taoChiNhanh(String code) {
        return jdbc.queryForObject(
            "INSERT INTO branches (code, name, open_time, close_time) VALUES (?, ?, '06:00', '22:00') RETURNING id",
            Long.class, code, "Chi nhanh " + code);
    }
}
