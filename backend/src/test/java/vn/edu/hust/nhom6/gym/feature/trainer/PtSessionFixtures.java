package vn.edu.hust.nhom6.gym.feature.trainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;

/**
 * Dữ liệu cho kiểm thử đặt, hủy buổi PT (S2-19.2). Dựa trên {@link TrainerFixtures}; thêm hội viên có tài khoản, gói PT,
 * buổi PT gắn đúng hội viên và gói, chỗ lớp nhóm.
 *
 * <p>VD-11: {@code TRUNCATE users ... CASCADE} trong {@code SaleFixtures} xóa luôn {@code system_configs}, nên
 * {@link #reset()} ghi lại {@code PT_MIN_LEAD_HOURS = 12} sau khi xóa.
 */
final class PtSessionFixtures {
    static final LocalDate PKG_START = LocalDate.of(2026, 11, 1);
    static final LocalDate PKG_END = LocalDate.of(2026, 12, 31);

    final JdbcTemplate jdbc;
    final TrainerFixtures t;
    Long cn01;
    Long cn02;
    private int seq;

    /** Hội viên có tài khoản đăng nhập. */
    record Account(Long userId, Long memberId) { }

    /** Huấn luyện viên: {@code users.id} và {@code staff.id}. */
    record Trainer(Long userId, Long staffId) { }

    PtSessionFixtures(JdbcTemplate jdbc, JwtTokenService tokens) {
        this.jdbc = jdbc;
        this.t = new TrainerFixtures(jdbc, tokens);
    }

    void reset() {
        t.reset();
        cn01 = t.cn01;
        cn02 = t.cn02;
        seq = 0;
        jdbc.update("DELETE FROM system_configs WHERE config_key = 'PT_MIN_LEAD_HOURS'");
        chainConfig("PT_MIN_LEAD_HOURS", 12);
    }

    void chainConfig(String key, int value) {
        jdbc.update("DELETE FROM system_configs WHERE branch_id IS NULL AND config_key = ?", key);
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (NULL, ?, ?)",
            key, String.valueOf(value));
    }

    void branchConfig(Long branchId, String key, int value) {
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (?, ?, ?)",
            branchId, key, String.valueOf(value));
    }

    /** Huấn luyện viên ở {@code branchId}, rảnh 08:00–20:00 cả bảy ngày (giờ Việt Nam). */
    Trainer trainer(Long branchId) {
        Trainer trainer = trainerWithoutAvailability(branchId);
        for (int day = 1; day <= 7; day++) {
            t.insertAvailabilityRaw(trainer.staffId(), branchId, day, "08:00", "20:00");
        }
        return trainer;
    }

    Trainer trainerWithoutAvailability(Long branchId) {
        int n = ++seq;
        Long userId = t.user("pt" + n + "@example.com", UserRole.TRAINER, branchId);
        return new Trainer(userId, t.staffId(userId));
    }

    Long staffUser(UserRole role, Long branchId) {
        int n = ++seq;
        return t.user(role.name().toLowerCase() + n + "@example.com", role, branchId);
    }

    /** Hội viên có tài khoản, chi nhánh gốc {@code homeBranchId}, trạng thái {@code status}, chưa có gói. */
    Account account(Long homeBranchId, String status) {
        int n = ++seq;
        Long userId = t.user("hv" + n + "@example.com", UserRole.MEMBER, null);
        Long memberId = jdbc.queryForObject("""
            INSERT INTO members (home_branch_id, member_code, full_name, phone, status, user_id)
            VALUES (?, ?, 'Hội viên PT', ?, ?, ?) RETURNING id""",
            Long.class, homeBranchId, String.format("HV8%05d", n), String.format("0981%06d", n), status, userId);
        return new Account(userId, memberId);
    }

    /** Hội viên ACTIVE ở CN01 có một gói PT ACTIVE của CN01 còn {@code remaining} buổi (01/11–31/12/2026). */
    Account ptMember(int remaining) {
        Account a = account(cn01, "ACTIVE");
        membership(a.memberId(), "PT", cn01, "ACTIVE", PKG_START, PKG_END, remaining);
        return a;
    }

    Long membership(Long memberId, String type, Long branchId, String status, LocalDate start, LocalDate end,
            Integer remaining) {
        int n = ++seq;
        Long pkgId = t.sale.pkg("PK" + n, type, "TIME".equals(type) ? null : 10, 2_000_000, branchId, "ACTIVE");
        return t.sale.membership(memberId, pkgId, type, branchId, status, start, end, remaining);
    }

    Long ptMembershipOf(Long memberId) {
        return jdbc.queryForObject(
            "SELECT id FROM memberships WHERE member_id = ? AND package_type = 'PT' ORDER BY id LIMIT 1",
            Long.class, memberId);
    }

    /** Buổi PT gắn đúng hội viên và gói, bắt đầu lúc {@code vnStart} giờ Việt Nam, dài 60 phút. */
    Long ptSession(Long trainerId, Long memberId, Long membershipId, LocalDateTime vnStart, String status) {
        OffsetDateTime start = vn(vnStart);
        return jdbc.queryForObject("""
            INSERT INTO pt_sessions (trainer_id, member_id, membership_id, branch_id, start_at, end_at, status,
                                     commission_amount, cancelled_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""",
            Long.class, trainerId, memberId, membershipId, cn01, start, start.plusMinutes(60), status,
            "DONE".equals(status) ? 200_000 : null, "CANCELLED".equals(status) ? start : null);
    }

    /** Buổi PT của một hội viên phụ (tự tạo hội viên và gói) cho huấn luyện viên {@code trainerId}. */
    Long otherPtSession(Long trainerId, LocalDateTime vnStart, String status) {
        Account other = ptMember(10);
        return ptSession(trainerId, other.memberId(), ptMembershipOf(other.memberId()), vnStart, status);
    }

    Long classSession(Long trainerId, LocalDateTime vnStart, LocalDateTime vnEnd, String status) {
        Long room = t.room(cn01, "Phòng " + (++seq), 30);
        Long type = t.classType("Lớp " + seq);
        Long schedule = t.classSchedule(cn01, type, trainerId, room, vnStart.getDayOfWeek().getValue(),
            vnStart.toLocalTime(), vnEnd.toLocalTime(), LocalDate.of(2026, 11, 1));
        // Lịch đã sinh buổi cho ngày đó; buổi có thể đã đổi giờ hoặc hủy.
        return t.classSession(schedule, cn01, trainerId, room, vnStart.toLocalDate(), vn(vnStart), vn(vnEnd), status);
    }

    /** Lịch lặp ACTIVE của huấn luyện viên, chưa sinh buổi nào. */
    Long classScheduleOnly(Long trainerId, int dayOfWeek, LocalTime start, LocalTime end) {
        Long room = t.room(cn01, "Phòng " + (++seq), 30);
        Long type = t.classType("Lớp " + seq);
        return t.classSchedule(cn01, type, trainerId, room, dayOfWeek, start, end, LocalDate.of(2026, 11, 1));
    }

    /** Chỗ lớp nhóm {@code status} của hội viên trong buổi {@code sessionId}. */
    void classBooking(Long sessionId, Account member, String status) {
        jdbc.update("""
            INSERT INTO class_bookings (class_session_id, member_id, created_by, status, booked_at, cancelled_at,
                                        cancel_reason)
            VALUES (?, ?, ?, ?, now(), ?, ?)""",
            sessionId, member.memberId(), member.userId(), status,
            "CANCELLED".equals(status) ? OffsetDateTime.now() : null,
            "CANCELLED".equals(status) ? "BY_MEMBER" : null);
    }

    Integer remaining(Long membershipId) {
        return jdbc.queryForObject("SELECT remaining_sessions FROM memberships WHERE id = ?", Integer.class,
            membershipId);
    }

    String status(Long sessionId) {
        return jdbc.queryForObject("SELECT status FROM pt_sessions WHERE id = ?", String.class, sessionId);
    }

    long count(String table) {
        return t.count(table);
    }

    long bookedCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM pt_sessions WHERE status = 'BOOKED'", Long.class);
    }

    RequestPostProcessor bearer(Long userId, UserRole role) {
        return t.bearer(userId, role);
    }

    static OffsetDateTime vn(LocalDateTime vnDateTime) {
        return vnDateTime.atZone(VnTime.ZONE).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }
}
