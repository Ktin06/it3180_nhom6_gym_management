package vn.edu.hust.nhom6.gym.feature.trainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.Account;
import vn.edu.hust.nhom6.gym.feature.trainer.PtSessionFixtures.Trainer;

/**
 * Dữ liệu cho kiểm thử xác nhận buổi PT, ghi vắng (S2-19.3). Dựa trên {@link PtSessionFixtures}; thêm gói PT bán qua
 * hóa đơn thật (để tính hoa hồng từ {@code invoice_items}) và tỷ lệ hoa hồng của huấn luyện viên.
 *
 * <p>Thẻ không đọc {@code system_configs}; {@link PtSessionFixtures#reset()} vẫn ghi lại {@code PT_MIN_LEAD_HOURS}
 * (VD-11) cho các kiểm thử đặt buổi chạy chung.
 */
final class PtCompletionFixtures {
    final JdbcTemplate jdbc;
    final PtSessionFixtures f;
    Long manager;
    private int seq;

    PtCompletionFixtures(JdbcTemplate jdbc, JwtTokenService tokens) {
        this.jdbc = jdbc;
        this.f = new PtSessionFixtures(jdbc, tokens);
    }

    void reset() {
        f.reset();
        seq = 0;
        manager = f.staffUser(UserRole.BRANCH_MANAGER, f.cn01);
    }

    /** Huấn luyện viên CN01 có tỷ lệ hoa hồng {@code rate} (rỗng = chưa khai báo). */
    Trainer trainer(String rate) {
        Trainer pt = f.trainer(f.cn01);
        jdbc.update("UPDATE staff SET commission_rate = ? WHERE id = ?",
            rate == null ? null : new BigDecimal(rate), pt.staffId());
        return pt;
    }

    Account member() {
        return f.account(f.cn01, "ACTIVE");
    }

    /**
     * Gói PT CN01 bán qua hóa đơn PAID: dòng hóa đơn {@code amount}, giảm {@code discount}; gói {@code total} buổi, còn
     * {@code remaining}, trạng thái {@code status}, hiệu lực {@code start}–{@code end}.
     */
    Long soldPt(Account a, String status, LocalDate start, LocalDate end, int total, int remaining, int amount,
            int discount) {
        int n = ++seq;
        Long pkg = f.t.sale.pkg("PTC" + n, "PT", total, amount, f.cn01, "ACTIVE");
        Long invoice = jdbc.queryForObject("""
            INSERT INTO invoices (code, branch_id, member_id, created_by, source, subtotal, discount, total, status,
                                  paid_at)
            VALUES (?, ?, ?, ?, 'COUNTER', ?, ?, ?, 'PAID', now()) RETURNING id""",
            Long.class, "HDPT" + n, f.cn01, a.memberId(), manager, amount, discount, amount - discount);
        Long item = jdbc.queryForObject("""
            INSERT INTO invoice_items (invoice_id, item_type, package_id, description, quantity, unit_price, amount,
                                       discount_amount)
            VALUES (?, 'PACKAGE', ?, 'Gói PT', 1, ?, ?, ?) RETURNING id""",
            Long.class, invoice, pkg, amount, amount, discount);
        return jdbc.queryForObject("""
            INSERT INTO memberships (member_id, package_id, package_type, branch_id, sold_branch_id, invoice_item_id,
                                     start_date, end_date, total_sessions, remaining_sessions, status)
            VALUES (?, ?, 'PT', ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""",
            Long.class, a.memberId(), pkg, f.cn01, f.cn01, item, start, end, total, remaining, status);
    }

    Long session(Trainer pt, Account a, Long membershipId, LocalDateTime vnStart) {
        return f.ptSession(pt.staffId(), a.memberId(), membershipId, vnStart, "BOOKED");
    }

    Map<String, Object> membership(Long id) {
        return jdbc.queryForMap("""
            SELECT status, start_date, end_date, remaining_sessions FROM memberships WHERE id = ?""", id);
    }

    Map<String, Object> ptSession(Long id) {
        return jdbc.queryForMap("""
            SELECT status, membership_id, confirmed_at, commission_amount, cancelled_at FROM pt_sessions WHERE id = ?""",
            id);
    }

    long auditRows(String action) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit_logs WHERE entity = 'pt_sessions' AND action = ?", Long.class, action);
    }
}
