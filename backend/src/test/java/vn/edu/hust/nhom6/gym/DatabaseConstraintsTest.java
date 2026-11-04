package vn.edu.hust.nhom6.gym;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

/**
 * Kiểm thử ràng buộc cơ sở dữ liệu trên PostgreSQL thật qua Testcontainers (WBS 1.3.10, Issue #24).
 *
 * <p>Đối chiếu các ràng buộc trong bảng "Quan hệ và ràng buộc quan trọng" của {@code thiet-ke-du-lieu.md}.
 * Chạy trên CI với Docker (PostgreSQL 16).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@EnabledIfDockerAvailable
class DatabaseConstraintsTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Email người dùng phải ở dạng chữ thường (ck_users_email_lowercase)")
    void emailNguoiDungPhaiLaChuThuong() {
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO users (email, password_hash, role, status) VALUES (?, ?, ?, ?)",
            "Member.UPPER@example.com", "hash", "MEMBER", "ACTIVE"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_users_email_lowercase");
    }

    @Test
    @DisplayName("Email người dùng là duy nhất (uq_users_email)")
    void emailNguoiDungLaDuyNhat() {
        jdbcTemplate.update(
            "INSERT INTO users (email, password_hash, role, status) VALUES (?, ?, ?, ?)",
            "unique.test@example.com", "hash", "MEMBER", "ACTIVE");

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO users (email, password_hash, role, status) VALUES (?, ?, ?, ?)",
            "unique.test@example.com", "hash2", "MEMBER", "ACTIVE"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_users_email");
    }

    @Test
    @DisplayName("Vai trò người dùng phải thuộc 5 vai trò quy định (ck_users_role)")
    void vaiTroNguoiDungPhaiHopLe() {
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO users (email, password_hash, role, status) VALUES (?, ?, ?, ?)",
            "invalid.role@example.com", "hash", "GUEST", "ACTIVE"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_users_role");
    }

    @Test
    @DisplayName("Trạng thái người dùng phải thuộc ACTIVE, LOCKED, DISABLED (ck_users_status)")
    void trangThaiNguoiDungPhaiHopLe() {
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO users (email, password_hash, role, status) VALUES (?, ?, ?, ?)",
            "invalid.status@example.com", "hash", "MEMBER", "DELETED"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_users_status");
    }

    @Test
    @DisplayName("Số lần đăng nhập sai không được âm (ck_users_failed_login_count)")
    void soLanDangNhapSaiKhongDuocAm() {
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO users (email, password_hash, role, status, failed_login_count) VALUES (?, ?, ?, ?, ?)",
            "negative.count@example.com", "hash", "MEMBER", "ACTIVE", -1))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_users_failed_login_count");
    }

    @Test
    @DisplayName("Token hash của refresh token là duy nhất (uq_refresh_tokens_token_hash)")
    void tokenHashRefreshTokenLaDuyNhat() {
        Long userId = insertTestUser("ref.user@example.com");

        jdbcTemplate.update(
            "INSERT INTO refresh_tokens (user_id, token_hash, expires_at) VALUES (?, ?, now() + interval '7 days')",
            userId, "hash-token-123");

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO refresh_tokens (user_id, token_hash, expires_at) VALUES (?, ?, now() + interval '7 days')",
            userId, "hash-token-123"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_refresh_tokens_token_hash");
    }

    @Test
    @DisplayName("Refresh token phải trỏ tới user tồn tại (fk_refresh_tokens_user)")
    void refreshTokenKhoaNgoaiUserHopLe() {
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO refresh_tokens (user_id, token_hash, expires_at) VALUES (?, ?, now() + interval '7 days')",
            999999L, "hash-token-invalid-user"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("fk_refresh_tokens_user");
    }

    @Test
    @DisplayName("Mã chi nhánh là duy nhất (uq_branches_code)")
    void maChiNhanhLaDuyNhat() {
        jdbcTemplate.update(
            "INSERT INTO branches (code, name, address, phone, open_time, close_time, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
            "CN_TEST", "Chi nhánh Test", "Hà Nội", "0900000001",
            LocalTime.of(6, 0), LocalTime.of(22, 0), "ACTIVE");

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO branches (code, name, address, phone, open_time, close_time, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
            "CN_TEST", "Chi nhánh Trùng", "Hà Nội", "0900000002",
            LocalTime.of(6, 0), LocalTime.of(22, 0), "ACTIVE"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_branches_code");
    }

    @Test
    @DisplayName("Giờ mở cửa phải trước giờ đóng cửa (ck_branches_open_close_time)")
    void gioMoCuaPhaiTruocGioDongCua() {
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO branches (code, name, address, phone, open_time, close_time, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
            "CN_HOURS", "Chi nhánh Sai Giờ", "Hà Nội", "0900000003",
            LocalTime.of(22, 0), LocalTime.of(6, 0), "ACTIVE"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_branches_open_close_time");
    }

    @Test
    @DisplayName("Cấu hình toàn chuỗi duy nhất trên (branch_id, config_key) với NULLS NOT DISTINCT (uq_system_configs_branch_key)")
    void cauHinhToanChuoiDuyNhat() {
        jdbcTemplate.update(
            "INSERT INTO system_configs (branch_id, config_key, config_value, description) VALUES (NULL, ?, ?, ?)",
            "SYSTEM_PARAM_A", "10", "Tham số thử nghiệm");

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO system_configs (branch_id, config_key, config_value, description) VALUES (NULL, ?, ?, ?)",
            "SYSTEM_PARAM_A", "20", "Tham số trùng"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_system_configs_branch_key");
    }

    @Test
    @DisplayName("Trừ tối đa 1 buổi mỗi ngày trên gói tập (uq_one_deduction_per_day)")
    void truToiDaMotBuoiMoiNgay() {
        LocalDate today = LocalDate.now();
        Long membershipId = 5001L;

        jdbcTemplate.update(
            "INSERT INTO checkins (member_id, membership_id, branch_id, created_by, checkin_date, session_deducted) VALUES (?, ?, ?, ?, ?, ?)",
            1L, membershipId, 1L, 1L, today, true);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO checkins (member_id, membership_id, branch_id, created_by, checkin_date, session_deducted) VALUES (?, ?, ?, ?, ?, ?)",
            1L, membershipId, 1L, 1L, today, true))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_one_deduction_per_day");

        // Không trừ buổi (session_deducted = false) thì checkin cùng ngày được phép
        assertThatCode(() -> jdbcTemplate.update(
            "INSERT INTO checkins (member_id, membership_id, branch_id, created_by, checkin_date, session_deducted) VALUES (?, ?, ?, ?, ?, ?)",
            1L, membershipId, 1L, 1L, today, false))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Không sinh trùng buổi lớp học nhóm trên cùng lịch lặp và ngày (uq_class_sessions_schedule_occurrence)")
    void khongSinhTrungBuoiLop() {
        Long classTypeId = jdbcTemplate.queryForObject(
            "INSERT INTO class_types (name, description) VALUES (?, ?) RETURNING id",
            Long.class, "Zumba Test", "Mô tả Zumba");

        Long scheduleId = jdbcTemplate.queryForObject(
            "INSERT INTO class_schedules (branch_id, class_type_id, trainer_id, room_id, day_of_week, start_time, end_time, capacity, valid_from) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class, 1L, classTypeId, 1L, 1L, 3, LocalTime.of(18, 0), LocalTime.of(19, 0), 25, LocalDate.now());

        LocalDate occurrenceDate = LocalDate.now();
        jdbcTemplate.update(
            "INSERT INTO class_sessions (class_schedule_id, branch_id, trainer_id, room_id, occurrence_date, start_at, end_at, capacity) VALUES (?, ?, ?, ?, ?, now(), now() + interval '1 hour', ?)",
            scheduleId, 1L, 1L, 1L, occurrenceDate, 25);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO class_sessions (class_schedule_id, branch_id, trainer_id, room_id, occurrence_date, start_at, end_at, capacity) VALUES (?, ?, ?, ?, ?, now() + interval '2 hours', now() + interval '3 hours', ?)",
            scheduleId, 1L, 1L, 1L, occurrenceDate, 25))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_class_sessions_schedule_occurrence");
    }

    @Test
    @DisplayName("Một chỗ đặt đang hoạt động cho mỗi buổi lớp nhóm (uq_active_booking)")
    void motChoDatDangHoatDongChoMoiBuoi() {
        Long classTypeId = jdbcTemplate.queryForObject(
            "INSERT INTO class_types (name, description) VALUES (?, ?) RETURNING id",
            Long.class, "Yoga Test", "Mô tả Yoga");

        Long scheduleId = jdbcTemplate.queryForObject(
            "INSERT INTO class_schedules (branch_id, class_type_id, trainer_id, room_id, day_of_week, start_time, end_time, capacity, valid_from) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class, 1L, classTypeId, 1L, 1L, 2, LocalTime.of(8, 0), LocalTime.of(9, 0), 20, LocalDate.now());

        Long sessionId = jdbcTemplate.queryForObject(
            "INSERT INTO class_sessions (class_schedule_id, branch_id, trainer_id, room_id, occurrence_date, start_at, end_at, capacity) VALUES (?, ?, ?, ?, ?, now(), now() + interval '1 hour', ?) RETURNING id",
            Long.class, scheduleId, 1L, 1L, 1L, LocalDate.now(), 20);

        Long memberId = 1001L;

        jdbcTemplate.update(
            "INSERT INTO class_bookings (class_session_id, member_id, status) VALUES (?, ?, ?)",
            sessionId, memberId, "BOOKED");

        // Đặt lại khi đã có chỗ BOOKED sẽ vi phạm uq_active_booking
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO class_bookings (class_session_id, member_id, status) VALUES (?, ?, ?)",
            sessionId, memberId, "BOOKED"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_active_booking");

        // Chỗ đã CANCELLED thì hội viên được phép đặt lại
        jdbcTemplate.update(
            "UPDATE class_bookings SET status = 'CANCELLED' WHERE class_session_id = ? AND member_id = ?",
            sessionId, memberId);

        assertThatCode(() -> jdbcTemplate.update(
            "INSERT INTO class_bookings (class_session_id, member_id, status) VALUES (?, ?, ?)",
            sessionId, memberId, "BOOKED"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Dòng hóa đơn phải khớp loại và công thức thành tiền (ck_invoice_items_*)")
    void dongHoaDonKhopLoaiVaThanhTien() {
        Long invoiceId = jdbcTemplate.queryForObject(
            "INSERT INTO invoices (code, branch_id, member_id, created_by, source, subtotal, discount, total, status) VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class, "INV_TEST_01", 1L, 1L, "COUNTER", 100000, 0, 100000, "PENDING");

        // PACKAGE nhưng thiếu package_id -> vi phạm ck_invoice_items_type_reference
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO invoice_items (invoice_id, item_type, package_id, product_id, description, quantity, unit_price, amount, discount_amount) VALUES (?, 'PACKAGE', NULL, NULL, 'Gói lỗi', 1, 100000, 100000, 0)",
            invoiceId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_invoice_items_type_reference");

        // TRANSFER_FEE nhưng lại truyền package_id -> vi phạm ck_invoice_items_type_reference
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO invoice_items (invoice_id, item_type, package_id, product_id, description, quantity, unit_price, amount, discount_amount) VALUES (?, 'TRANSFER_FEE', 1, NULL, 'Phí lỗi', 1, 200000, 200000, 0)",
            invoiceId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_invoice_items_type_reference");

        // amount != quantity * unit_price -> vi phạm ck_invoice_items_amount
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO invoice_items (invoice_id, item_type, package_id, product_id, description, quantity, unit_price, amount, discount_amount) VALUES (?, 'PACKAGE', 1, NULL, 'Sai tiền', 2, 100000, 150000, 0)",
            invoiceId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_invoice_items_amount");

        // discount_amount > amount -> vi phạm ck_invoice_items_discount
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO invoice_items (invoice_id, item_type, package_id, product_id, description, quantity, unit_price, amount, discount_amount) VALUES (?, 'PACKAGE', 1, NULL, 'Giảm giá lố', 1, 100000, 100000, 120000)",
            invoiceId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_invoice_items_discount");
    }

    @Test
    @DisplayName("Kho điều chỉnh có dấu: IN/OUT dương, ADJUST khác 0 (ck_stock_movements_quantity)")
    void khoDieuChinhCoDau() {
        Long productId = jdbcTemplate.queryForObject(
            "INSERT INTO products (sku, name, price, unit, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
            Long.class, "SKU_TEST_01", "Nước khoáng", 10000, "Chai", "ACTIVE");

        // IN với số âm -> vi phạm ck_stock_movements_quantity
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO stock_movements (branch_id, product_id, created_by, type, quantity, unit_cost) VALUES (?, ?, ?, 'IN', -5, 8000)",
            1L, productId, 1L))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_stock_movements_quantity");

        // ADJUST bằng 0 -> vi phạm ck_stock_movements_quantity
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO stock_movements (branch_id, product_id, created_by, type, quantity) VALUES (?, ?, ?, 'ADJUST', 0)",
            1L, productId, 1L))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_stock_movements_quantity");

        // ADJUST âm (giảm tồn kho kiểm kê) -> hợp lệ
        assertThatCode(() -> jdbcTemplate.update(
            "INSERT INTO stock_movements (branch_id, product_id, created_by, type, quantity) VALUES (?, ?, ?, 'ADJUST', -3)",
            1L, productId, 1L))
            .doesNotThrowAnyException();
    }

    private Long insertTestUser(String email) {
        return jdbcTemplate.queryForObject(
            "INSERT INTO users (email, password_hash, role, status) VALUES (?, ?, ?, ?) RETURNING id",
            Long.class, email, "hash", "MEMBER", "ACTIVE");
    }
}
