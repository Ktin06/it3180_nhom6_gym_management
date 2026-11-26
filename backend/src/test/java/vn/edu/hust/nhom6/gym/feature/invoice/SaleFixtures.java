package vn.edu.hust.nhom6.gym.feature.invoice;

import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import vn.edu.hust.nhom6.gym.DatabaseCleaner;

import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;

/**
 * Dữ liệu dựng bằng SQL cho kiểm thử bán gói và danh sách gói. Mỗi lớp kiểm thử gọi {@link #reset()} ở
 * {@code @BeforeEach}. Chi nhánh CN01 có sẵn từ migration; CN02 tạo thêm để thử phạm vi chi nhánh.
 */
public final class SaleFixtures {
    private final JdbcTemplate jdbc;
    private final JwtTokenService tokens;
    public Long cn01;
    public Long cn02;

    public SaleFixtures(JdbcTemplate jdbc, JwtTokenService tokens) {
        this.jdbc = jdbc;
        this.tokens = tokens;
    }

    public void reset() {
        DatabaseCleaner.truncate(jdbc, "users", "staff", "members", "packages", "memberships", "invoices", "invoice_items");
        jdbc.update("UPDATE branches SET status = 'ACTIVE' WHERE code = 'CN01'");
        jdbc.update("""
            INSERT INTO branches (code, name, open_time, close_time) VALUES ('CN02', 'Chi nhánh 2', '06:00', '22:00')
            ON CONFLICT (code) DO NOTHING""");
        cn01 = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN01'", Long.class);
        cn02 = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN02'", Long.class);
    }

    /** Tài khoản nhân viên; {@code branchId} rỗng thì không có dòng staff (chủ chuỗi). */
    public Long staff(String email, UserRole role, Long branchId) {
        Long userId = jdbc.queryForObject(
            "INSERT INTO users (email, password_hash, role) VALUES (?, 'test-only', ?) RETURNING id",
            Long.class, email, role.name());
        if (branchId != null) {
            jdbc.update("INSERT INTO staff (user_id, branch_id, staff_code, full_name) VALUES (?, ?, ?, 'Nhân viên test')",
                userId, branchId, "NV" + userId);
        }
        return userId;
    }

    public Long member(String code, String phone, Long homeBranchId, String status) {
        return jdbc.queryForObject("""
            INSERT INTO members (home_branch_id, member_code, full_name, phone, status)
            VALUES (?, ?, 'Hội viên test', ?, ?) RETURNING id""", Long.class, homeBranchId, code, phone, status);
    }

    /** Gói danh mục; {@code sessions} rỗng với TIME; {@code branchId} rỗng = toàn chuỗi. */
    public Long pkg(String code, String type, Integer sessions, int price, Long branchId, String status) {
        return jdbc.queryForObject("""
            INSERT INTO packages (branch_id, code, name, type, duration_days, session_count, price, status)
            VALUES (?, ?, ?, ?, 30, ?, ?, ?) RETURNING id""", Long.class,
            branchId, code, "Gói " + code, type, sessions, price, status);
    }

    /** Gói hội viên dựng sẵn (không qua hóa đơn). */
    public Long membership(Long memberId, Long packageId, String type, Long branchId, String status,
            LocalDate start, LocalDate end, Integer remaining) {
        Integer total = remaining == null ? null : Math.max(remaining, 10);
        return jdbc.queryForObject("""
            INSERT INTO memberships (member_id, package_id, package_type, branch_id, sold_branch_id, start_date,
                                     end_date, total_sessions, remaining_sessions, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class,
            memberId, packageId, type, branchId, cn01, start, end, total, remaining, status);
    }

    public long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    public RequestPostProcessor bearer(Long userId, UserRole role) {
        String token = tokens.generateAccessToken(userId, "u" + userId + "@example.com", role);
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }
}
