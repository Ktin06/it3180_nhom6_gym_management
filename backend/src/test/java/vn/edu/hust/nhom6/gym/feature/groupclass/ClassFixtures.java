package vn.edu.hust.nhom6.gym.feature.groupclass;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.invoice.SaleFixtures;

/**
 * Dữ liệu dựng bằng SQL cho kiểm thử lớp học nhóm (F8). Dùng lại {@link SaleFixtures} cho tài khoản, hội viên, gói;
 * thêm phòng, loại lớp, lịch lặp, buổi, buổi PT. Gọi {@link #reset()} ở {@code @BeforeEach}.
 */
public final class ClassFixtures {
    private final JdbcTemplate jdbc;
    public final SaleFixtures sale;
    public Long cn01;
    public Long cn02;

    public ClassFixtures(JdbcTemplate jdbc, JwtTokenService tokens) {
        this.jdbc = jdbc;
        this.sale = new SaleFixtures(jdbc, tokens);
    }

    public void reset() {
        sale.reset();
        jdbc.execute("TRUNCATE TABLE rooms, class_types, class_schedules, class_sessions, class_bookings, pt_sessions "
            + "RESTART IDENTITY CASCADE");
        cn01 = sale.cn01;
        cn02 = sale.cn02;
    }

    /** Tài khoản nhân viên; trả {@code users.id}. {@code branchId} rỗng = chủ chuỗi (không có dòng staff). */
    public Long user(String email, UserRole role, Long branchId) {
        return sale.staff(email, role, branchId);
    }

    /** {@code staff.id} của tài khoản (người dạy trỏ staff.id, quy tắc mã 11). */
    public Long staffId(Long userId) {
        return jdbc.queryForObject("SELECT id FROM staff WHERE user_id = ?", Long.class, userId);
    }

    /** Huấn luyện viên ở chi nhánh; trả {@code staff.id}. */
    public Long trainer(String email, Long branchId) {
        return staffId(user(email, UserRole.TRAINER, branchId));
    }

    public Long room(Long branchId, String name, int capacity) {
        return jdbc.queryForObject("INSERT INTO rooms (branch_id, name, capacity) VALUES (?, ?, ?) RETURNING id",
            Long.class, branchId, name, capacity);
    }

    public Long classType(String name) {
        return jdbc.queryForObject("INSERT INTO class_types (name) VALUES (?) RETURNING id", Long.class, name);
    }

    /** Lịch lặp dựng thẳng bằng SQL (bỏ qua kiểm tra của service), dùng cho kiểm thử sinh buổi. */
    public Long schedule(Long branchId, Long classTypeId, Long trainerId, Long roomId, int dayOfWeek,
            LocalTime start, LocalTime end, int capacity, LocalDate validFrom, LocalDate validTo, String status) {
        return jdbc.queryForObject("""
            INSERT INTO class_schedules (branch_id, class_type_id, trainer_id, room_id, day_of_week, start_time,
                                         end_time, capacity, valid_from, valid_to, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class,
            branchId, classTypeId, trainerId, roomId, dayOfWeek, start, end, capacity, validFrom, validTo, status);
    }

    /** Buổi PT của huấn luyện viên (dựng kèm hội viên và gói PT). */
    public Long ptSession(Long trainerId, Long branchId, OffsetDateTime start, OffsetDateTime end, String status) {
        Long memberId = sale.member(memberCode(), phone(), branchId, "ACTIVE");
        Long pkg = sale.pkg("PT" + next(), "PT", 10, 1_000_000, branchId, "ACTIVE");
        Long membership = sale.membership(memberId, pkg, "PT", branchId, "ACTIVE",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), 10);
        return jdbc.queryForObject("""
            INSERT INTO pt_sessions (trainer_id, member_id, membership_id, branch_id, start_at, end_at, status,
                                     commission_amount, cancelled_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class,
            trainerId, memberId, membership, branchId, start, end, status,
            "DONE".equals(status) ? 100_000 : null, "CANCELLED".equals(status) ? start : null);
    }

    /** Chỗ đặt BOOKED của một hội viên mới vào buổi. */
    public Long booking(Long sessionId, Long createdBy) {
        Long memberId = sale.member(memberCode(), phone(), cn01, "ACTIVE");
        return jdbc.queryForObject("""
            INSERT INTO class_bookings (class_session_id, member_id, created_by, status, booked_at)
            VALUES (?, ?, ?, 'BOOKED', now()) RETURNING id""", Long.class, sessionId, memberId, createdBy);
    }

    public long count(String table) {
        return sale.count(table);
    }

    public RequestPostProcessor bearer(Long userId, UserRole role) {
        return sale.bearer(userId, role);
    }

    private int seq = 0;

    private int next() {
        return ++seq;
    }

    private String memberCode() {
        return String.format("HV9%05d", next());
    }

    private String phone() {
        return String.format("0987%06d", next());
    }
}
