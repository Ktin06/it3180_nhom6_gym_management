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

/**
 * Kiểm thử ràng buộc cơ sở dữ liệu trên PostgreSQL thật qua Testcontainers (WBS 1.3.10, Issue #24).
 *
 * <p>Đối chiếu các ràng buộc trong bảng "Quan hệ và ràng buộc quan trọng" của {@code thiet-ke-du-lieu.md}.
 * Chạy trên CI với Docker (PostgreSQL 16).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
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
    @DisplayName("Số điện thoại, định dạng SĐT và tài khoản liên kết của hội viên phải hợp lệ, duy nhất (uq_members_phone, ck_members_phone, uq_members_user_id)")
    void rangBuocBangMembersHopLeVaDuyNhat() {
        Long branchId = defaultBranchId();
        Long userId = insertTestUser("member.linked@example.com");

        jdbcTemplate.update(
            "INSERT INTO members (user_id, home_branch_id, member_code, full_name, phone, status) VALUES (?, ?, 'HV000201', 'Hội viên 201', '0910000201', 'ACTIVE')",
            userId, branchId);

        // Trùng số điện thoại -> vi phạm uq_members_phone
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO members (home_branch_id, member_code, full_name, phone, status) VALUES (?, 'HV000202', 'Hội viên 202', '0910000201', 'ACTIVE')",
            branchId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_members_phone");

        // Số điện thoại sai định dạng 10 chữ số bắt đầu bằng 0 -> vi phạm ck_members_phone
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO members (home_branch_id, member_code, full_name, phone, status) VALUES (?, 'HV000203', 'Hội viên 203', '84910000203', 'ACTIVE')",
            branchId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_members_phone");

        // Hai hồ sơ hội viên cùng trỏ tới một user_id -> vi phạm uq_members_user_id
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO members (user_id, home_branch_id, member_code, full_name, phone, status) VALUES (?, ?, 'HV000204', 'Hội viên 204', '0910000204', 'ACTIVE')",
            userId, branchId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_members_user_id");
    }

    @Test
    @DisplayName("Mỗi hội viên chỉ có tối đa một gói ACTIVE hoặc FROZEN cho mỗi loại gói và chi nhánh (uq_one_active_per_type)")
    void moiHoiVienChiCoMotGoiDangHoatDongTheoTungLoaiVaChiNhanh() {
        Long branchId = defaultBranchId();
        Long memberId = insertTestMember("HV000100", "0910000100");
        Long timePackageId = insertTestPackage("PKG_ACTIVE_TIME", "TIME", 30, null, 500000);
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(29);

        Long firstMembershipId = jdbcTemplate.queryForObject(
            "INSERT INTO memberships (member_id, package_id, package_type, branch_id, sold_branch_id, start_date, end_date, status) VALUES (?, ?, 'TIME', NULL, ?, ?, ?, 'ACTIVE') RETURNING id",
            Long.class, memberId, timePackageId, branchId, today, endDate);

        // Thêm gói TIME thứ hai cùng ACTIVE và cùng branch_id rỗng (NULLS NOT DISTINCT) -> bị từ chối
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO memberships (member_id, package_id, package_type, branch_id, sold_branch_id, start_date, end_date, status) VALUES (?, ?, 'TIME', NULL, ?, ?, ?, 'ACTIVE')",
            memberId, timePackageId, branchId, today, endDate))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_one_active_per_type");

        // Khi gói cũ đã EXPIRED thì thêm gói ACTIVE mới cùng loại và cùng branch_id rỗng được phép
        jdbcTemplate.update("UPDATE memberships SET status = 'EXPIRED' WHERE id = ?", firstMembershipId);

        assertThatCode(() -> jdbcTemplate.update(
            "INSERT INTO memberships (member_id, package_id, package_type, branch_id, sold_branch_id, start_date, end_date, status) VALUES (?, ?, 'TIME', NULL, ?, ?, ?, 'ACTIVE')",
            memberId, timePackageId, branchId, today, endDate))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Gói tập phải khớp số buổi theo loại gói TIME hoặc SESSION/PT (ck_memberships_sessions)")
    void goiTapPhaiKhopSoBuoiTheoLoaiGoi() {
        Long branchId = defaultBranchId();
        Long memberId = insertTestMember("HV000101", "0910000101");
        Long timePackageId = insertTestPackage("PKG_TIME_01", "TIME", 30, null, 500000);
        Long sessionPackageId = insertTestPackage("PKG_SES_01", "SESSION", 60, 20, 1000000);
        LocalDate today = LocalDate.now();

        // Gói TIME nhưng lại có total_sessions / remaining_sessions -> vi phạm ck_memberships_sessions
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO memberships (member_id, package_id, package_type, sold_branch_id, start_date, end_date, total_sessions, remaining_sessions, status) VALUES (?, ?, 'TIME', ?, ?, ?, 10, 10, 'PENDING')",
            memberId, timePackageId, branchId, today, today.plusDays(29)))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_memberships_sessions");

        // Gói SESSION nhưng thiếu total_sessions -> vi phạm ck_memberships_sessions
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO memberships (member_id, package_id, package_type, sold_branch_id, start_date, end_date, total_sessions, remaining_sessions, status) VALUES (?, ?, 'SESSION', ?, ?, ?, NULL, NULL, 'PENDING')",
            memberId, sessionPackageId, branchId, today, today.plusDays(59)))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_memberships_sessions");

        // Gói SESSION có remaining_sessions > total_sessions -> vi phạm ck_memberships_sessions
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO memberships (member_id, package_id, package_type, sold_branch_id, start_date, end_date, total_sessions, remaining_sessions, status) VALUES (?, ?, 'SESSION', ?, ?, ?, 20, 25, 'PENDING')",
            memberId, sessionPackageId, branchId, today, today.plusDays(59)))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_memberships_sessions");
    }

    @Test
    @DisplayName("Ngày kết thúc bảo lưu gói tập không được trước ngày bắt đầu (ck_membership_freezes_dates)")
    void ngayBaoLuuGoiTapPhaiHopLe() {
        Long userId = insertTestUser("freeze.requester@example.com");
        Long memberId = insertTestMember("HV000102", "0910000102");
        Long packageId = insertTestPackage("PKG_FREEZE_01", "TIME", 90, null, 1200000);
        Long membershipId = insertTestMembership(memberId, packageId, "TIME", 90, null);
        LocalDate fromDate = LocalDate.now().plusDays(10);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO membership_freezes (membership_id, requested_by, from_date, to_date, status) VALUES (?, ?, ?, ?, 'PENDING')",
            membershipId, userId, fromDate, fromDate.minusDays(1)))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_membership_freezes_dates");
    }

    @Test
    @DisplayName("Trừ tối đa 1 buổi mỗi ngày trên gói tập (uq_one_deduction_per_day)")
    void truToiDaMotBuoiMoiNgay() {
        LocalDate today = LocalDate.now();
        Long branchId = defaultBranchId();
        Long staffUserId = insertTestUser("checkin.staff@example.com");
        Long memberId = insertTestMember("HV000001", "0910000001");
        Long packageId = insertTestPackage("PKG_CHECKIN_01", "SESSION", 30, 12, 600000);
        Long membershipId = insertTestMembership(memberId, packageId, "SESSION", 30, 12);

        jdbcTemplate.update(
            "INSERT INTO checkins (member_id, membership_id, branch_id, created_by, checkin_date, session_deducted) VALUES (?, ?, ?, ?, ?, ?)",
            memberId, membershipId, branchId, staffUserId, today, true);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO checkins (member_id, membership_id, branch_id, created_by, checkin_date, session_deducted) VALUES (?, ?, ?, ?, ?, ?)",
            memberId, membershipId, branchId, staffUserId, today, true))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_one_deduction_per_day");

        // Không trừ buổi (session_deducted = false) thì checkin cùng ngày được phép
        assertThatCode(() -> jdbcTemplate.update(
            "INSERT INTO checkins (member_id, membership_id, branch_id, created_by, checkin_date, session_deducted) VALUES (?, ?, ?, ?, ?, ?)",
            memberId, membershipId, branchId, staffUserId, today, false))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Điểm khớp khuôn mặt chỉ được phép có khi check-in bằng FACE (ck_checkins_face_score)")
    void diemKhopKhuonMatChiCoKhiCheckinFace() {
        LocalDate today = LocalDate.now();
        Long branchId = defaultBranchId();
        Long staffUserId = insertTestUser("face.staff@example.com");
        Long memberId = insertTestMember("HV000103", "0910000103");
        Long packageId = insertTestPackage("PKG_FACE_01", "TIME", 30, null, 500000);
        Long membershipId = insertTestMembership(memberId, packageId, "TIME", 30, null);

        // MANUAL nhưng lại truyền match_score -> vi phạm ck_checkins_face_score
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO checkins (member_id, membership_id, branch_id, created_by, method, match_score, checkin_date, session_deducted) VALUES (?, ?, ?, ?, 'MANUAL', 0.9500, ?, false)",
            memberId, membershipId, branchId, staffUserId, today))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_checkins_face_score");
    }

    @Test
    @DisplayName("Không sinh trùng buổi lớp học nhóm trên cùng lịch lặp và ngày (uq_class_sessions_schedule_occurrence)")
    void khongSinhTrungBuoiLop() {
        Long branchId = defaultBranchId();
        Long trainerId = insertTestStaff("trainer.zumba@example.com", "NV000001", "0920000001");
        Long roomId = insertTestRoom("Phòng Zumba 101", 30);

        Long classTypeId = jdbcTemplate.queryForObject(
            "INSERT INTO class_types (name, description) VALUES (?, ?) RETURNING id",
            Long.class, "Zumba Test", "Mô tả Zumba");

        Long scheduleId = jdbcTemplate.queryForObject(
            "INSERT INTO class_schedules (branch_id, class_type_id, trainer_id, room_id, day_of_week, start_time, end_time, capacity, valid_from) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class, branchId, classTypeId, trainerId, roomId, 3, LocalTime.of(18, 0), LocalTime.of(19, 0), 25, LocalDate.now());

        LocalDate occurrenceDate = LocalDate.now();
        jdbcTemplate.update(
            "INSERT INTO class_sessions (class_schedule_id, branch_id, trainer_id, room_id, occurrence_date, start_at, end_at, capacity) VALUES (?, ?, ?, ?, ?, now(), now() + interval '1 hour', ?)",
            scheduleId, branchId, trainerId, roomId, occurrenceDate, 25);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO class_sessions (class_schedule_id, branch_id, trainer_id, room_id, occurrence_date, start_at, end_at, capacity) VALUES (?, ?, ?, ?, ?, now() + interval '2 hours', now() + interval '3 hours', ?)",
            scheduleId, branchId, trainerId, roomId, occurrenceDate, 25))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_class_sessions_schedule_occurrence");
    }

    @Test
    @DisplayName("Một chỗ đặt đang hoạt động cho mỗi buổi lớp nhóm (uq_active_booking)")
    void motChoDatDangHoatDongChoMoiBuoi() {
        Long branchId = defaultBranchId();
        Long trainerId = insertTestStaff("trainer.yoga@example.com", "NV000002", "0920000002");
        Long roomId = insertTestRoom("Phòng Yoga 102", 25);
        Long memberId = insertTestMember("HV000002", "0910000002");
        Long bookerId = insertTestUser("booker.yoga@example.com");

        Long classTypeId = jdbcTemplate.queryForObject(
            "INSERT INTO class_types (name, description) VALUES (?, ?) RETURNING id",
            Long.class, "Yoga Test", "Mô tả Yoga");

        Long scheduleId = jdbcTemplate.queryForObject(
            "INSERT INTO class_schedules (branch_id, class_type_id, trainer_id, room_id, day_of_week, start_time, end_time, capacity, valid_from) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class, branchId, classTypeId, trainerId, roomId, 2, LocalTime.of(8, 0), LocalTime.of(9, 0), 20, LocalDate.now());

        Long sessionId = jdbcTemplate.queryForObject(
            "INSERT INTO class_sessions (class_schedule_id, branch_id, trainer_id, room_id, occurrence_date, start_at, end_at, capacity) VALUES (?, ?, ?, ?, ?, now(), now() + interval '1 hour', ?) RETURNING id",
            Long.class, scheduleId, branchId, trainerId, roomId, LocalDate.now(), 20);

        jdbcTemplate.update(
            "INSERT INTO class_bookings (class_session_id, member_id, created_by, status) VALUES (?, ?, ?, ?)",
            sessionId, memberId, bookerId, "BOOKED");

        // Đặt lại khi đã có chỗ BOOKED sẽ vi phạm uq_active_booking
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO class_bookings (class_session_id, member_id, created_by, status) VALUES (?, ?, ?, ?)",
            sessionId, memberId, bookerId, "BOOKED"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_active_booking");

        // Chỗ đã CANCELLED (đủ thời điểm và lý do hủy) thì hội viên được phép đặt lại
        jdbcTemplate.update(
            "UPDATE class_bookings SET status = 'CANCELLED', cancelled_at = now(), cancel_reason = 'BY_MEMBER' WHERE class_session_id = ? AND member_id = ?",
            sessionId, memberId);

        assertThatCode(() -> jdbcTemplate.update(
            "INSERT INTO class_bookings (class_session_id, member_id, created_by, status) VALUES (?, ?, ?, ?)",
            sessionId, memberId, bookerId, "BOOKED"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Trạng thái hủy đặt lớp phải đồng bộ với thời điểm hủy, lý do hủy và cờ hủy muộn (ck_class_bookings_cancelled); chỗ đặt phải có người tạo (created_by NOT NULL)")
    void trangThaiHuyDatLopPhaiDongBoThongTinHuy() {
        Long branchId = defaultBranchId();
        Long trainerId = insertTestStaff("trainer.pilates@example.com", "NV000003", "0920000003");
        Long roomId = insertTestRoom("Phòng Pilates 103", 20);
        Long memberId = insertTestMember("HV000104", "0910000104");
        Long bookerId = insertTestUser("booker.pilates@example.com");

        Long classTypeId = jdbcTemplate.queryForObject(
            "INSERT INTO class_types (name, description) VALUES (?, ?) RETURNING id",
            Long.class, "Pilates Test", "Mô tả Pilates");

        Long scheduleId = jdbcTemplate.queryForObject(
            "INSERT INTO class_schedules (branch_id, class_type_id, trainer_id, room_id, day_of_week, start_time, end_time, capacity, valid_from) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class, branchId, classTypeId, trainerId, roomId, 4, LocalTime.of(17, 0), LocalTime.of(18, 0), 15, LocalDate.now());

        Long sessionId = jdbcTemplate.queryForObject(
            "INSERT INTO class_sessions (class_schedule_id, branch_id, trainer_id, room_id, occurrence_date, start_at, end_at, capacity) VALUES (?, ?, ?, ?, ?, now(), now() + interval '1 hour', ?) RETURNING id",
            Long.class, scheduleId, branchId, trainerId, roomId, LocalDate.now(), 15);

        // CANCELLED nhưng thiếu cancelled_at / cancel_reason -> vi phạm ck_class_bookings_cancelled
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO class_bookings (class_session_id, member_id, created_by, status, cancelled_at, cancel_reason) VALUES (?, ?, ?, 'CANCELLED', NULL, NULL)",
            sessionId, memberId, bookerId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_class_bookings_cancelled");

        // BOOKED nhưng lại có is_late_cancel = true -> vi phạm ck_class_bookings_cancelled
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO class_bookings (class_session_id, member_id, created_by, status, is_late_cancel) VALUES (?, ?, ?, 'BOOKED', true)",
            sessionId, memberId, bookerId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_class_bookings_cancelled");

        // Thiếu người tạo chỗ đặt -> vi phạm NOT NULL của created_by (ADR 0010, migration V9_01)
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO class_bookings (class_session_id, member_id, status) VALUES (?, ?, 'BOOKED')",
            sessionId, memberId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("created_by");
    }

    @Test
    @DisplayName("Dòng hóa đơn phải khớp loại và công thức thành tiền (ck_invoice_items_*)")
    void dongHoaDonKhopLoaiVaThanhTien() {
        Long branchId = defaultBranchId();
        Long userId = insertTestUser("invoice.creator@example.com");
        Long packageId = insertTestPackage("PKG_INV_01", "TIME", 30, null, 100000);
        Long invoiceId = jdbcTemplate.queryForObject(
            "INSERT INTO invoices (code, branch_id, member_id, created_by, source, subtotal, discount, total, status) VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class, "INV_TEST_01", branchId, userId, "COUNTER", 100000, 0, 100000, "PENDING");

        // PACKAGE nhưng thiếu package_id -> vi phạm ck_invoice_items_type_reference
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO invoice_items (invoice_id, item_type, package_id, product_id, description, quantity, unit_price, amount, discount_amount) VALUES (?, 'PACKAGE', NULL, NULL, 'Gói lỗi', 1, 100000, 100000, 0)",
            invoiceId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_invoice_items_type_reference");

        // TRANSFER_FEE nhưng lại truyền package_id -> vi phạm ck_invoice_items_type_reference
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO invoice_items (invoice_id, item_type, package_id, product_id, description, quantity, unit_price, amount, discount_amount) VALUES (?, 'TRANSFER_FEE', ?, NULL, 'Phí lỗi', 1, 200000, 200000, 0)",
            invoiceId, packageId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_invoice_items_type_reference");

        // amount != quantity * unit_price -> vi phạm ck_invoice_items_amount
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO invoice_items (invoice_id, item_type, package_id, product_id, description, quantity, unit_price, amount, discount_amount) VALUES (?, 'PACKAGE', ?, NULL, 'Sai tiền', 2, 100000, 150000, 0)",
            invoiceId, packageId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_invoice_items_amount");

        // discount_amount > amount -> vi phạm ck_invoice_items_discount
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO invoice_items (invoice_id, item_type, package_id, product_id, description, quantity, unit_price, amount, discount_amount) VALUES (?, 'PACKAGE', ?, NULL, 'Giảm giá lố', 1, 100000, 100000, 120000)",
            invoiceId, packageId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_invoice_items_discount");
    }

    @Test
    @DisplayName("Kho điều chỉnh có dấu: IN/OUT dương, ADJUST khác 0 (ck_stock_movements_quantity)")
    void khoDieuChinhCoDau() {
        Long branchId = defaultBranchId();
        Long userId = insertTestUser("stock.creator@example.com");
        Long productId = jdbcTemplate.queryForObject(
            "INSERT INTO products (sku, name, price, unit, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
            Long.class, "SKU_TEST_01", "Nước khoáng", 10000, "Chai", "ACTIVE");

        // IN với số âm -> vi phạm ck_stock_movements_quantity
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO stock_movements (branch_id, product_id, created_by, type, quantity, unit_cost) VALUES (?, ?, ?, 'IN', -5, 8000)",
            branchId, productId, userId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_stock_movements_quantity");

        // ADJUST bằng 0 -> vi phạm ck_stock_movements_quantity
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO stock_movements (branch_id, product_id, created_by, type, quantity) VALUES (?, ?, ?, 'ADJUST', 0)",
            branchId, productId, userId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_stock_movements_quantity");

        // ADJUST âm (giảm tồn kho kiểm kê) -> hợp lệ
        assertThatCode(() -> jdbcTemplate.update(
            "INSERT INTO stock_movements (branch_id, product_id, created_by, type, quantity) VALUES (?, ?, ?, 'ADJUST', -3)",
            branchId, productId, userId))
            .doesNotThrowAnyException();
    }

    private Long defaultBranchId() {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM branches WHERE code = 'CN01'",
            Long.class);
    }

    private Long insertTestUser(String email) {
        return jdbcTemplate.queryForObject(
            "INSERT INTO users (email, password_hash, role, status) VALUES (?, ?, ?, ?) RETURNING id",
            Long.class, email, "hash", "MEMBER", "ACTIVE");
    }

    private Long insertTestMember(String memberCode, String phone) {
        return jdbcTemplate.queryForObject(
            "INSERT INTO members (home_branch_id, member_code, full_name, phone, status) VALUES (?, ?, ?, ?, 'ACTIVE') RETURNING id",
            Long.class, defaultBranchId(), memberCode, "Hội viên " + memberCode, phone);
    }

    private Long insertTestPackage(String code, String type, int durationDays, Integer sessionCount, int price) {
        return jdbcTemplate.queryForObject(
            "INSERT INTO packages (code, name, type, duration_days, session_count, price, status) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE') RETURNING id",
            Long.class, code, "Gói " + code, type, durationDays, sessionCount, price);
    }

    private Long insertTestMembership(Long memberId, Long packageId, String packageType, int durationDays, Integer sessions) {
        LocalDate today = LocalDate.now();
        return jdbcTemplate.queryForObject(
            "INSERT INTO memberships (member_id, package_id, package_type, sold_branch_id, start_date, end_date, total_sessions, remaining_sessions, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE') RETURNING id",
            Long.class, memberId, packageId, packageType, defaultBranchId(), today, today.plusDays(durationDays - 1L), sessions, sessions);
    }

    private Long insertTestStaff(String email, String staffCode, String phone) {
        Long userId = jdbcTemplate.queryForObject(
            "INSERT INTO users (email, password_hash, role, status) VALUES (?, ?, 'TRAINER', 'ACTIVE') RETURNING id",
            Long.class, email, "hash");
        return jdbcTemplate.queryForObject(
            "INSERT INTO staff (user_id, branch_id, staff_code, full_name, phone, base_salary, status) VALUES (?, ?, ?, ?, ?, 10000000, 'ACTIVE') RETURNING id",
            Long.class, userId, defaultBranchId(), staffCode, "HLV " + staffCode, phone);
    }

    private Long insertTestRoom(String name, int capacity) {
        return jdbcTemplate.queryForObject(
            "INSERT INTO rooms (branch_id, name, capacity, status) VALUES (?, ?, ?, 'ACTIVE') RETURNING id",
            Long.class, defaultBranchId(), name, capacity);
    }
}
