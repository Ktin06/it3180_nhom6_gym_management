package vn.edu.hust.nhom6.gym.feature.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.invoice.SaleFixtures;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigKeys;

/**
 * Dữ liệu dựng bằng SQL cho kiểm thử dashboard (S2-12.1).
 * <ul>
 *   <li>VD-11: {@link #reset()} tự ghi lại {@code SESSION_WARNING_THRESHOLD = 2} trong {@code system_configs} vì
 *       {@link SaleFixtures#reset()} truncate {@code users ... CASCADE} làm xóa bảng cấu hình.</li>
 *   <li>VD-13: đặt lại {@code open_time = '06:00', close_time = '22:00'} cho CN01 và CN02 mỗi lượt chạy.</li>
 * </ul>
 */
final class DashboardFixtures {
    final JdbcTemplate jdbc;
    final SaleFixtures sale;
    Long cn01;
    Long cn02;
    Long receptionistCn01;
    Long managerCn01;
    Long receptionistCn02;
    Long owner;
    Long trainerUser;
    Long memberUser;
    private int seq;

    DashboardFixtures(JdbcTemplate jdbc, JwtTokenService tokens) {
        this.jdbc = jdbc;
        this.sale = new SaleFixtures(jdbc, tokens);
    }

    void reset() {
        sale.reset();
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS member_avatars (
                member_id BIGINT PRIMARY KEY REFERENCES members(id) ON DELETE CASCADE,
                content BYTEA NOT NULL,
                size_bytes INT NOT NULL,
                uploaded_by BIGINT REFERENCES users(id),
                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """);
        jdbc.execute("TRUNCATE TABLE member_avatars CASCADE");
        cn01 = sale.cn01;
        cn02 = sale.cn02;
        seq = 0;
        jdbc.update("UPDATE branches SET open_time = '06:00', close_time = '22:00', status = 'ACTIVE' WHERE id IN (?, ?)",
            cn01, cn02);
        // VD-11: ghi lại cấu hình SESSION_WARNING_THRESHOLD (mặc định toàn chuỗi = 2)
        jdbc.update("DELETE FROM system_configs WHERE config_key = ?", SystemConfigKeys.SESSION_WARNING_THRESHOLD);
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (NULL, ?, '2')",
            SystemConfigKeys.SESSION_WARNING_THRESHOLD);

        receptionistCn01 = sale.staff("letan-f6@example.com", UserRole.RECEPTIONIST, cn01);
        managerCn01 = sale.staff("quanly-f6@example.com", UserRole.BRANCH_MANAGER, cn01);
        receptionistCn02 = sale.staff("letan-cn02-f6@example.com", UserRole.RECEPTIONIST, cn02);
        owner = sale.staff("chuchuoi-f6@example.com", UserRole.SUPER_ADMIN, null);
        trainerUser = sale.staff("hlv-f6@example.com", UserRole.TRAINER, cn01);
        memberUser = jdbc.queryForObject(
            "INSERT INTO users (email, password_hash, role) VALUES ('hv-f6@example.com', 'test-only', 'MEMBER') RETURNING id",
            Long.class);
    }

    void setBranchHours(Long branchId, String openTime, String closeTime) {
        jdbc.update("UPDATE branches SET open_time = CAST(? AS time), close_time = CAST(? AS time) WHERE id = ?",
            openTime, closeTime, branchId);
    }

    void setBranchConfig(Long branchId, String key, int value) {
        jdbc.update("DELETE FROM system_configs WHERE branch_id = ? AND config_key = ?", branchId, key);
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (?, ?, ?)",
            branchId, key, String.valueOf(value));
    }

    Long member(Long homeBranchId, String fullName) {
        int n = ++seq;
        String code = String.format("HV6%05d", n);
        String phone = String.format("0977%06d", n);
        return jdbc.queryForObject("""
            INSERT INTO members (home_branch_id, member_code, full_name, phone, status)
            VALUES (?, ?, ?, ?, 'ACTIVE') RETURNING id""", Long.class, homeBranchId, code, fullName, phone);
    }

    void addAvatar(Long memberId) {
        jdbc.update("INSERT INTO member_avatars (member_id, content, size_bytes, uploaded_by) VALUES (?, ?, 3, ?)",
            memberId, new byte[] {1, 2, 3}, receptionistCn01);
    }

    Long membership(Long memberId, String type, Long branchId, String status, LocalDate start, LocalDate end,
            Integer remaining) {
        int n = ++seq;
        Integer total = remaining == null ? null : Math.max(remaining, 10);
        Long pkg = sale.pkg("F6_" + type + "_" + n, type, total, 500_000, branchId, "ACTIVE");
        return jdbc.queryForObject("""
            INSERT INTO memberships (member_id, package_id, package_type, branch_id, sold_branch_id, start_date,
                                     end_date, total_sessions, remaining_sessions, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class,
            memberId, pkg, type, branchId, cn01, start, end, total, remaining, status);
    }

    Long checkin(Long memberId, Long membershipId, Long branchId, Long createdBy, String method,
            LocalDateTime checkedInVn, LocalDateTime checkedOutVn) {
        OffsetDateTime inUtc = utc(checkedInVn);
        OffsetDateTime outUtc = checkedOutVn == null ? null : utc(checkedOutVn);
        return jdbc.queryForObject("""
            INSERT INTO checkins (member_id, membership_id, branch_id, created_by, method, match_score,
                                  checkin_date, session_deducted, checked_in_at, checked_out_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, false, ?, ?) RETURNING id""", Long.class,
            memberId, membershipId, branchId, createdBy, method,
            "FACE".equals(method) ? new BigDecimal("0.9400") : null,
            checkedInVn.toLocalDate(), inUtc, outUtc);
    }

    /** Chèn hóa đơn (memberId có thể null cho khách lẻ) và tùy chọn phiếu thu kèm theo. */
    Long invoiceWithPayment(Long branchId, Long memberId, String invoiceStatus, int total,
            String paymentMethod, String paymentStatus, LocalDateTime paidAtVn) {
        int n = ++seq;
        String code = String.format("HD6%05d", n);
        OffsetDateTime paidUtc = paidAtVn == null ? null : utc(paidAtVn);
        Long invoiceId = jdbc.queryForObject("""
            INSERT INTO invoices (code, branch_id, member_id, created_by, source, subtotal, discount, total, status, paid_at)
            VALUES (?, ?, ?, ?, 'COUNTER', ?, 0, ?, ?, ?) RETURNING id""", Long.class,
            code, branchId, memberId, receptionistCn01, total, total, invoiceStatus,
            "PAID".equals(invoiceStatus) ? paidUtc : null);
        if (paymentMethod != null && paymentStatus != null && paidUtc != null) {
            jdbc.update("""
                INSERT INTO payments (invoice_id, method, amount, status, received_by, paid_at)
                VALUES (?, ?, ?, ?, ?, ?)""",
                invoiceId, paymentMethod, total, paymentStatus, receptionistCn01, paidUtc);
        }
        return invoiceId;
    }

    RequestPostProcessor receptionCn01() {
        return sale.bearer(receptionistCn01, UserRole.RECEPTIONIST);
    }

    RequestPostProcessor managerCn01() {
        return sale.bearer(managerCn01, UserRole.BRANCH_MANAGER);
    }

    RequestPostProcessor receptionCn02() {
        return sale.bearer(receptionistCn02, UserRole.RECEPTIONIST);
    }

    RequestPostProcessor owner() {
        return sale.bearer(owner, UserRole.SUPER_ADMIN);
    }

    RequestPostProcessor as(Long userId, UserRole role) {
        return sale.bearer(userId, role);
    }

    static OffsetDateTime utc(LocalDateTime vn) {
        return vn.atZone(VnTime.ZONE).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }
}
