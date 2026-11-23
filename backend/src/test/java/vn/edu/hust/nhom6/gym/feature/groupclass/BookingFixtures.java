package vn.edu.hust.nhom6.gym.feature.groupclass;

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
 * Dữ liệu dựng bằng SQL cho kiểm thử đặt chỗ, hủy chỗ (S2-16.2). Dựa trên {@link ClassFixtures}; thêm hội viên có tài
 * khoản, gói, buổi học đặt giờ tùy ý, chỗ đặt ở mọi trạng thái, mốc khóa trong {@code audit_logs} và tham số
 * {@code system_configs}. Gọi {@link #reset()} ở {@code @BeforeEach}.
 */
public final class BookingFixtures {
    private final JdbcTemplate jdbc;
    public final ClassFixtures f;
    public Long cn01;
    public Long cn02;
    /** Lễ tân CN01, CN02 ({@code users.id}). */
    public Long receptionist;
    public Long receptionistCn02;
    private Long classType;
    private int seq;

    public BookingFixtures(JdbcTemplate jdbc, JwtTokenService tokens) {
        this.jdbc = jdbc;
        this.f = new ClassFixtures(jdbc, tokens);
    }

    public void reset() {
        f.reset();
        cn01 = f.cn01;
        cn02 = f.cn02;
        seq = 0;
        // TRUNCATE users ... CASCADE (trong SaleFixtures) kéo theo system_configs và audit_logs: ghi lại tham số F8.
        jdbc.update("DELETE FROM system_configs WHERE config_key IN ('BOOKING_WINDOW_DAYS', 'BOOKING_CUTOFF_MIN', "
            + "'LATE_CANCEL_HOURS', 'PENALTY_LIMIT_PER_MONTH', 'PENALTY_LOCK_DAYS')");
        chainConfig("BOOKING_WINDOW_DAYS", 7);
        chainConfig("BOOKING_CUTOFF_MIN", 30);
        chainConfig("LATE_CANCEL_HOURS", 2);
        chainConfig("PENALTY_LIMIT_PER_MONTH", 3);
        chainConfig("PENALTY_LOCK_DAYS", 7);
        receptionist = f.user("le-tan@example.com", UserRole.RECEPTIONIST, cn01);
        receptionistCn02 = f.user("le-tan-cn02@example.com", UserRole.RECEPTIONIST, cn02);
        classType = f.classType("Yoga");
    }

    public void chainConfig(String key, int value) {
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (NULL, ?, ?)",
            key, String.valueOf(value));
    }

    public void branchConfig(Long branchId, String key, int value) {
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (?, ?, ?)",
            branchId, key, String.valueOf(value));
    }

    /** Hội viên có tài khoản đăng nhập. */
    public record Account(Long userId, Long memberId) { }

    /** Hội viên có tài khoản, chi nhánh gốc {@code homeBranchId}, chưa có gói. */
    public Account account(Long homeBranchId) {
        int n = ++seq;
        Long userId = jdbc.queryForObject(
            "INSERT INTO users (email, password_hash, role) VALUES (?, 'test-only', 'MEMBER') RETURNING id",
            Long.class, "hv" + n + "@example.com");
        Long memberId = jdbc.queryForObject("""
            INSERT INTO members (user_id, home_branch_id, member_code, full_name, phone, status)
            VALUES (?, ?, ?, 'Hội viên đặt lớp', ?, 'ACTIVE') RETURNING id""", Long.class, userId, homeBranchId,
            String.format("HV8%05d", n), String.format("0966%06d", n));
        return new Account(userId, memberId);
    }

    /** Hội viên CN01 có gói TIME CN01 ACTIVE 01/10 – 31/12/2026. */
    public Account timeMember() {
        Account a = account(cn01);
        membership(a.memberId(), "TIME", cn01, "ACTIVE", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31));
        return a;
    }

    /** Gói hội viên; {@code branchId} rỗng = toàn chuỗi. SESSION, PT có 10 buổi. */
    public Long membership(Long memberId, String type, Long branchId, String status, LocalDate start, LocalDate end) {
        Long pkg = f.sale.pkg(type + "_" + (++seq), type, "TIME".equals(type) ? null : 10, 500_000, branchId, "ACTIVE");
        return f.sale.membership(memberId, pkg, type, branchId, status, start, end, "TIME".equals(type) ? null : 10);
    }

    /** Buổi SCHEDULED ở chi nhánh, bắt đầu {@code startVn} giờ Việt Nam, dài 60 phút. Mỗi buổi một lịch riêng. */
    public Long session(Long branchId, LocalDateTime startVn, int capacity) {
        return session(branchId, startVn, 60, capacity, "SCHEDULED", null);
    }

    public Long session(Long branchId, LocalDateTime startVn, int minutes, int capacity, String status,
            LocalDateTime originalStartVn) {
        int n = ++seq;
        Long trainer = f.trainer("hlv" + n + "@example.com", branchId);
        Long room = f.room(branchId, "Phòng " + n, 50);
        LocalDate day = startVn.toLocalDate();
        Long schedule = f.schedule(branchId, classType, trainer, room, day.getDayOfWeek().getValue(),
            LocalTime.of(6, 0), LocalTime.of(7, 0), capacity, day, day, "ACTIVE");
        return jdbc.queryForObject("""
            INSERT INTO class_sessions (class_schedule_id, branch_id, trainer_id, room_id, occurrence_date, start_at,
                                        end_at, original_start_at, capacity, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class,
            schedule, branchId, trainer, room, day, utc(startVn), utc(startVn.plusMinutes(minutes)),
            originalStartVn == null ? null : utc(originalStartVn), capacity, status);
    }

    /** Chỗ đặt ở trạng thái bất kỳ (CANCELLED thì ghi kèm thời điểm, lý do BY_MEMBER, không muộn). */
    public Long booking(Long sessionId, Long memberId, String status) {
        boolean cancelled = "CANCELLED".equals(status);
        return jdbc.queryForObject("""
            INSERT INTO class_bookings (class_session_id, member_id, created_by, status, booked_at, cancelled_at,
                                        cancel_reason)
            VALUES (?, ?, ?, ?, now(), ?, ?) RETURNING id""", Long.class, sessionId, memberId, receptionist, status,
            cancelled ? OffsetDateTime.now(ZoneOffset.UTC) : null, cancelled ? "BY_MEMBER" : null);
    }

    /**
     * Buổi PT của chính hội viên {@code memberId} (VD-21 GĐ-7), dựng kèm huấn luyện viên và gói PT ở CN01, bắt đầu
     * {@code startVn} giờ Việt Nam, dài {@code minutes} phút, trạng thái {@code status} (BOOKED, CANCELLED…).
     */
    public Long memberPtSession(Long memberId, LocalDateTime startVn, int minutes, String status) {
        int n = ++seq;
        Long trainer = f.trainer("pt" + n + "@example.com", cn01);
        // Một hội viên chỉ có một gói PT ACTIVE mỗi chi nhánh (uq_one_active_per_type): có rồi thì dùng lại.
        Long ptMembership = jdbc.query("SELECT id FROM memberships WHERE member_id = ? AND package_type = 'PT' "
            + "AND branch_id = ? AND status = 'ACTIVE'", (rs, i) -> rs.getLong(1), memberId, cn01).stream().findFirst()
            .orElseGet(() -> membership(memberId, "PT", cn01, "ACTIVE", LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 12, 31)));
        boolean cancelled = "CANCELLED".equals(status);
        return jdbc.queryForObject("""
            INSERT INTO pt_sessions (trainer_id, member_id, membership_id, branch_id, start_at, end_at, status,
                                     cancelled_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class, trainer, memberId, ptMembership, cn01,
            utc(startVn), utc(startVn.plusMinutes(minutes)), status, cancelled ? utc(startVn.minusDays(1)) : null);
    }

    /** Vắng mặt (NO_SHOW) trong buổi mới bắt đầu {@code startVn}, điểm danh ghi lúc {@code startVn + 1 giờ}. */
    public Long noShow(Long memberId, LocalDateTime startVn) {
        return noShow(session(cn01, startVn, 20), memberId, startVn.plusHours(1));
    }

    /** Vắng mặt (NO_SHOW) trong buổi có sẵn; {@code updated_at} = lúc ghi điểm danh {@code markedVn}. */
    public Long noShow(Long sessionId, Long memberId, LocalDateTime markedVn) {
        return jdbc.queryForObject("""
            INSERT INTO class_bookings (class_session_id, member_id, created_by, status, booked_at, created_at,
                                        updated_at)
            VALUES (?, ?, ?, 'NO_SHOW', ?, ?, ?) RETURNING id""", Long.class, sessionId, memberId, receptionist,
            utc(markedVn.minusDays(2)), utc(markedVn.minusDays(2)), utc(markedVn));
    }

    /** Một vi phạm hủy muộn đã có: chỗ CANCELLED, {@code is_late_cancel = true}, hủy lúc {@code cancelledVn}. */
    public Long lateCancel(Long memberId, LocalDateTime cancelledVn) {
        Long session = session(cn01, cancelledVn.plusHours(1), 20);
        return jdbc.queryForObject("""
            INSERT INTO class_bookings (class_session_id, member_id, created_by, status, booked_at, cancelled_at,
                                        cancel_reason, is_late_cancel)
            VALUES (?, ?, ?, 'CANCELLED', ?, ?, 'BY_MEMBER', true) RETURNING id""", Long.class,
            session, memberId, receptionist, utc(cancelledVn.minusDays(1)), utc(cancelledVn));
    }

    /** Mốc khóa đặt lớp đã có: dòng {@code audit_logs} BOOKING_LOCK lúc {@code atVn} và cột khóa đến {@code untilVn}. */
    public void lockMark(Long memberId, LocalDateTime atVn, LocalDateTime untilVn) {
        jdbc.update("""
            INSERT INTO audit_logs (user_id, branch_id, action, entity, entity_id, created_at, updated_at)
            VALUES (NULL, ?, 'BOOKING_LOCK', 'members', ?, ?, ?)""", cn01, memberId, utc(atVn), utc(atVn));
        jdbc.update("UPDATE members SET booking_locked_until = ? WHERE id = ?", utc(untilVn), memberId);
    }

    public OffsetDateTime lockedUntil(Long memberId) {
        return jdbc.queryForObject("SELECT booking_locked_until FROM members WHERE id = ?", OffsetDateTime.class,
            memberId);
    }

    public long lockAudits(Long memberId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE action = 'BOOKING_LOCK' AND entity = "
            + "'members' AND entity_id = ?", Long.class, memberId);
    }

    public String status(Long bookingId) {
        return jdbc.queryForObject("SELECT status FROM class_bookings WHERE id = ?", String.class, bookingId);
    }

    public boolean late(Long bookingId) {
        return jdbc.queryForObject("SELECT is_late_cancel FROM class_bookings WHERE id = ?", Boolean.class, bookingId);
    }

    /** Số chỗ đang chiếm buổi (BOOKED, ATTENDED, NO_SHOW). */
    public long occupied(Long sessionId) {
        return jdbc.queryForObject("SELECT count(*) FROM class_bookings WHERE class_session_id = ? "
            + "AND status <> 'CANCELLED'", Long.class, sessionId);
    }

    public long bookings() {
        return f.count("class_bookings");
    }

    public RequestPostProcessor bearer(Long userId, UserRole role) {
        return f.bearer(userId, role);
    }

    public RequestPostProcessor member(Account a) {
        return f.bearer(a.userId(), UserRole.MEMBER);
    }

    public static OffsetDateTime utc(LocalDateTime vn) {
        return vn.atZone(VnTime.ZONE).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }
}
