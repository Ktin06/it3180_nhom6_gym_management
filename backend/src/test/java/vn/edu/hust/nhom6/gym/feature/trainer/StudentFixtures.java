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
import vn.edu.hust.nhom6.gym.feature.invoice.SaleFixtures;

/**
 * Dữ liệu cho kiểm thử giáo án, nhật ký tập, chỉ số cơ thể, tiến độ (S2-20.1, SUA-5). Dựng quan hệ "PT dạy hội viên"
 * bằng buổi PT ({@code pt_sessions}) và hỗ trợ tạo chỗ lớp nhóm để kiểm thử chứng minh lớp nhóm không cấp quyền PT.
 * Public để kiểm thử F2 (gói {@code member}) dùng chung.
 *
 * <p>Thẻ không đọc {@code system_configs}, nên không cần ghi lại cấu hình sau khi truncate (VD-11).
 */
public final class StudentFixtures {
    public final JdbcTemplate jdbc;
    public final SaleFixtures sale;
    public Long cn01;
    public Long cn02;
    private int seq;
    private final java.util.Map<Long, Long> ptMemberships = new java.util.HashMap<>();

    /** Huấn luyện viên: {@code users.id} và {@code staff.id}. */
    public record Trainer(Long userId, Long staffId) { }

    /** Hội viên có tài khoản: {@code users.id} và {@code members.id}. */
    public record Member(Long userId, Long memberId) { }

    public StudentFixtures(JdbcTemplate jdbc, JwtTokenService tokens) {
        this.jdbc = jdbc;
        this.sale = new SaleFixtures(jdbc, tokens);
    }

    public void reset() {
        sale.reset();
        jdbc.execute("""
            TRUNCATE TABLE workout_logs, workout_plans, body_metrics, pt_sessions, class_bookings, class_sessions,
                           class_schedules, rooms, class_types, trainer_availabilities, audit_logs
            RESTART IDENTITY CASCADE""");
        cn01 = sale.cn01;
        cn02 = sale.cn02;
        seq = 0;
        ptMemberships.clear();
    }

    public Trainer trainer(Long branchId) {
        Long userId = staffUser(UserRole.TRAINER, branchId);
        Long staffId = jdbc.queryForObject("SELECT id FROM staff WHERE user_id = ?", Long.class, userId);
        return new Trainer(userId, staffId);
    }

    /** Tài khoản nhân viên ở {@code branchId}; {@code branchId} rỗng = chủ chuỗi (không có dòng staff). */
    public Long staffUser(UserRole role, Long branchId) {
        int n = ++seq;
        return sale.staff(role.name().toLowerCase() + n + "@example.com", role, branchId);
    }

    /** Hội viên ACTIVE có tài khoản, chi nhánh gốc {@code homeBranchId}. */
    public Member member(Long homeBranchId) {
        int n = ++seq;
        Long userId = sale.staff("hv" + n + "@example.com", UserRole.MEMBER, null);
        Long memberId = jdbc.queryForObject("""
            INSERT INTO members (home_branch_id, member_code, full_name, phone, status, user_id)
            VALUES (?, ?, ?, ?, 'ACTIVE', ?) RETURNING id""",
            Long.class, homeBranchId, String.format("HV7%05d", n), "Học viên " + n, String.format("0961%06d", n),
            userId);
        return new Member(userId, memberId);
    }

    public void rename(Long memberId, String fullName) {
        jdbc.update("UPDATE members SET full_name = ? WHERE id = ?", fullName, memberId);
    }

    public void softDelete(Long memberId) {
        jdbc.update("UPDATE members SET deleted_at = now() WHERE id = ?", memberId);
    }

    /** Buổi PT {@code status} giữa PT và hội viên lúc {@code vnStart} (giờ VN), kèm gói PT riêng cho buổi. */
    public Long ptSession(Trainer trainer, Member member, LocalDateTime vnStart, String status) {
        Long branchId = jdbc.queryForObject("SELECT branch_id FROM staff WHERE id = ?", Long.class, trainer.staffId());
        // Mỗi hội viên chỉ một gói PT ACTIVE toàn chuỗi (uq_one_active_per_type): tạo một lần, dùng lại.
        Long membershipId = ptMemberships.computeIfAbsent(member.memberId(), memberId -> {
            Long pkgId = sale.pkg("PTS" + (++seq), "PT", 10, 2_000_000, null, "ACTIVE");
            return sale.membership(memberId, pkgId, "PT", null, "ACTIVE",
                LocalDate.of(2026, 10, 1), LocalDate.of(2027, 3, 31), 10);
        });
        OffsetDateTime start = vn(vnStart);
        return jdbc.queryForObject("""
            INSERT INTO pt_sessions (trainer_id, member_id, membership_id, branch_id, start_at, end_at, status,
                                     confirmed_at, commission_amount, cancelled_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""",
            Long.class, trainer.staffId(), member.memberId(), membershipId, branchId, start, start.plusMinutes(60),
            status, "DONE".equals(status) ? start.plusMinutes(60) : null, "DONE".equals(status) ? 100_000 : null,
            "CANCELLED".equals(status) ? start : null);
    }

    /** Chỗ lớp {@code bookingStatus} của hội viên ở một buổi lớp do {@code teacher} dạy (lịch cũng của teacher). */
    public Long classBooking(Trainer teacher, Member member, String bookingStatus) {
        return classBooking(teacher, teacher, member, bookingStatus);
    }

    /**
     * Chỗ lớp ở buổi có {@code class_schedules.trainer_id} = {@code scheduleTrainer} nhưng
     * {@code class_sessions.trainer_id} = {@code sessionTrainer} (dạy thay).
     */
    public Long classBooking(Trainer scheduleTrainer, Trainer sessionTrainer, Member member, String bookingStatus) {
        int n = ++seq;
        Long room = jdbc.queryForObject("INSERT INTO rooms (branch_id, name, capacity) VALUES (?, ?, 30) RETURNING id",
            Long.class, cn01, "Phòng " + n);
        Long type = jdbc.queryForObject("INSERT INTO class_types (name) VALUES (?) RETURNING id", Long.class,
            "Lớp " + n);
        LocalDate day = LocalDate.of(2026, 11, 17);
        Long schedule = jdbc.queryForObject("""
            INSERT INTO class_schedules (branch_id, class_type_id, trainer_id, room_id, day_of_week,
                                         start_time, end_time, capacity, valid_from, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 20, ?, 'ACTIVE') RETURNING id""",
            Long.class, cn01, type, scheduleTrainer.staffId(), room, day.getDayOfWeek().getValue(),
            LocalTime.of(18, 0), LocalTime.of(19, 0), LocalDate.of(2026, 11, 1));
        Long session = jdbc.queryForObject("""
            INSERT INTO class_sessions (class_schedule_id, branch_id, trainer_id, room_id, occurrence_date,
                                        start_at, end_at, capacity, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 20, 'SCHEDULED') RETURNING id""",
            Long.class, schedule, cn01, sessionTrainer.staffId(), room, day, vn(day.atTime(18, 0)),
            vn(day.atTime(19, 0)));
        boolean cancelled = "CANCELLED".equals(bookingStatus);
        return jdbc.queryForObject("""
            INSERT INTO class_bookings (class_session_id, member_id, created_by, status, booked_at, cancelled_at,
                                        cancel_reason)
            VALUES (?, ?, ?, ?, now(), ?, ?) RETURNING id""",
            Long.class, session, member.memberId(), member.userId(), bookingStatus,
            cancelled ? OffsetDateTime.now() : null, cancelled ? "BY_MEMBER" : null);
    }

    public Long bodyMetric(Long memberId, Long recordedBy, LocalDate measuredAt, String weight) {
        return jdbc.queryForObject("""
            INSERT INTO body_metrics (member_id, recorded_by, measured_at, weight)
            VALUES (?, ?, ?, CAST(? AS numeric)) RETURNING id""", Long.class, memberId, recordedBy, measuredAt, weight);
    }

    public Long workoutLog(Long memberId, LocalDate loggedAt, String exercise) {
        return jdbc.queryForObject("""
            INSERT INTO workout_logs (member_id, logged_at, exercise, sets, reps, weight)
            VALUES (?, ?, ?, 3, 10, 40.00) RETURNING id""", Long.class, memberId, loggedAt, exercise);
    }

    public long count(String table) {
        return sale.count(table);
    }

    public RequestPostProcessor bearer(Long userId, UserRole role) {
        return sale.bearer(userId, role);
    }

    public static OffsetDateTime vn(LocalDateTime vnDateTime) {
        return vnDateTime.atZone(VnTime.ZONE).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }
}
