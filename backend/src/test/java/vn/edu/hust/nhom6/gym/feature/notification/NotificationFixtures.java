package vn.edu.hust.nhom6.gym.feature.notification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures.Account;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigKeys;

/**
 * Dữ liệu mẫu dựng trực tiếp bằng SQL cho kiểm thử tích hợp F10.
 *
 * <p>Mỗi lần {@link #reset()} khôi phục {@code SESSION_WARNING_THRESHOLD = 2} toàn chuỗi và đặt giờ mở cửa cố định
 * CN01 05:30–22:00, CN02 06:00–22:00.
 */
final class NotificationFixtures {
    final JdbcTemplate jdbc;
    final BookingFixtures bookings;
    Long cn01;
    Long cn02;
    private int seq;

    NotificationFixtures(JdbcTemplate jdbc, JwtTokenService tokens) {
        this.jdbc = jdbc;
        this.bookings = new BookingFixtures(jdbc, tokens);
    }

    void reset() {
        bookings.reset();
        cn01 = bookings.cn01;
        cn02 = bookings.cn02;
        seq = 0;
        jdbc.update("DELETE FROM notifications");
        jdbc.update("DELETE FROM system_configs WHERE config_key = ?", SystemConfigKeys.SESSION_WARNING_THRESHOLD);
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (NULL, ?, '2')",
            SystemConfigKeys.SESSION_WARNING_THRESHOLD);
        setHours(cn01, "05:30", "22:00");
        setHours(cn02, "06:00", "22:00");
    }

    void setHours(Long branchId, String open, String close) {
        jdbc.update("UPDATE branches SET open_time = CAST(? AS time), close_time = CAST(? AS time) WHERE id = ?",
            open, close, branchId);
    }

    void branchThreshold(Long branchId, int value) {
        bookings.branchConfig(branchId, SystemConfigKeys.SESSION_WARNING_THRESHOLD, value);
    }

    /** Hội viên có tài khoản với email miền {@code @gym-test.vn} (không bị chặn ngoài profile {@code local}). */
    Account account(Long homeBranchId) {
        Account a = bookings.account(homeBranchId);
        jdbc.update("UPDATE users SET email = ? WHERE id = ?", "hv" + a.userId() + "@gym-test.vn", a.userId());
        return a;
    }

    /** Hội viên chưa có tài khoản ({@code members.user_id IS NULL}). */
    Long memberWithoutAccount(Long homeBranchId) {
        int n = ++seq;
        return jdbc.queryForObject("""
            INSERT INTO members (home_branch_id, member_code, full_name, phone, status)
            VALUES (?, ?, 'Hội viên chưa có tài khoản', ?, 'ACTIVE') RETURNING id""", Long.class,
            homeBranchId, String.format("HV9%05d", n), String.format("0955%06d", n));
    }

    /** Tạo gói hội viên; {@code branchId} rỗng biểu thị gói toàn chuỗi. */
    Long membership(Long memberId, String type, Long branchId, String status, LocalDate start, LocalDate end,
            Integer remaining) {
        int n = ++seq;
        Integer total = remaining == null ? null : Math.max(remaining, 10);
        Long pkg = bookings.f.sale.pkg("F10_" + type + "_" + n, type, total, 500_000, branchId, "ACTIVE");
        return jdbc.queryForObject("""
            INSERT INTO memberships (member_id, package_id, package_type, branch_id, sold_branch_id, start_date,
                                     end_date, total_sessions, remaining_sessions, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class,
            memberId, pkg, type, branchId, cn01, start, end, total, remaining, status);
    }

    Long session(Long branchId, LocalDateTime startVn, String status) {
        return bookings.session(branchId, startVn, 60, 20, status, null);
    }

    Long booking(Long sessionId, Long memberId, String status) {
        return bookings.booking(sessionId, memberId, status);
    }

    /** Đọc các dòng {@code notifications} theo đối tượng liên quan, sắp kênh {@code IN_APP} trước {@code EMAIL}. */
    List<Map<String, Object>> rows(String relatedEntity, Long relatedId) {
        return jdbc.queryForList("""
            SELECT id, user_id, type, title, content, channel, status, scheduled_at, sent_at, read_at
            FROM notifications WHERE related_entity = ? AND related_id = ? ORDER BY channel DESC, id""",
            relatedEntity, relatedId);
    }

    int count() {
        return jdbc.queryForObject("SELECT count(*) FROM notifications", Integer.class);
    }

    static OffsetDateTime utc(LocalDateTime vn) {
        return BookingFixtures.utc(vn);
    }
}
