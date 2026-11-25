package vn.edu.hust.nhom6.gym.feature.memberportal;

import org.springframework.jdbc.core.JdbcTemplate;

import vn.edu.hust.nhom6.gym.feature.invoice.SaleFixtures;

/**
 * Bổ sung cho {@link SaleFixtures}: hội viên có tài khoản đăng nhập (vai trò MEMBER, {@code members.user_id}), và
 * tham số {@code PENDING_INVOICE_TTL_DAYS} về đúng giá trị seed.
 */
public final class PortalFixtures {
    private final JdbcTemplate jdbc;

    public PortalFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Tài khoản và hồ sơ hội viên gắn nhau. */
    public MemberAccount memberAccount(String code, String phone, Long homeBranchId, String status) {
        Long userId = jdbc.queryForObject(
            "INSERT INTO users (email, password_hash, role) VALUES (?, 'test-only', 'MEMBER') RETURNING id",
            Long.class, code.toLowerCase() + "@example.com");
        Long memberId = jdbc.queryForObject("""
            INSERT INTO members (user_id, home_branch_id, member_code, full_name, phone, status)
            VALUES (?, ?, ?, 'Hội viên cổng', ?, ?) RETURNING id""", Long.class, userId, homeBranchId, code, phone,
            status);
        return new MemberAccount(userId, memberId);
    }

    /**
     * Chỉ giữ dòng toàn chuỗi 3 ngày như seed V1_06. Ghi lại dòng này vì {@code SaleFixtures.reset()} chạy
     * {@code TRUNCATE users ... CASCADE}, kéo theo {@code system_configs} (khóa ngoại {@code updated_by}).
     */
    public void resetTtl() {
        jdbc.update("DELETE FROM system_configs WHERE config_key = 'PENDING_INVOICE_TTL_DAYS'");
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (NULL, "
            + "'PENDING_INVOICE_TTL_DAYS', '3')");
    }

    public void branchTtl(Long branchId, int days) {
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (?, "
            + "'PENDING_INVOICE_TTL_DAYS', ?)", branchId, String.valueOf(days));
    }

    public record MemberAccount(Long userId, Long memberId) { }
}
