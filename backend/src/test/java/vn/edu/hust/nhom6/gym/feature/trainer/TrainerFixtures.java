package vn.edu.hust.nhom6.gym.feature.trainer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.invoice.SaleFixtures;

/**
 * Dữ liệu dựng bằng SQL cho kiểm thử chức năng huấn luyện viên (F9). Tái sử dụng {@link SaleFixtures} cho tài khoản,
 * chi nhánh, hội viên và gói; thêm khung rảnh, buổi PT và buổi lớp nhóm.
 */
final class TrainerFixtures {
    private final JdbcTemplate jdbc;
    final SaleFixtures sale;
    Long cn01;
    Long cn02;
    private int seq = 0;

    TrainerFixtures(JdbcTemplate jdbc, JwtTokenService tokens) {
        this.jdbc = jdbc;
        this.sale = new SaleFixtures(jdbc, tokens);
    }

    void reset() {
        sale.reset();
        jdbc.execute("""
            TRUNCATE TABLE trainer_availabilities, pt_sessions, class_bookings, class_sessions,
                           class_schedules, rooms, class_types, audit_logs
            RESTART IDENTITY CASCADE""");
        cn01 = sale.cn01;
        cn02 = sale.cn02;
    }

    Long user(String email, UserRole role, Long branchId) {
        return sale.staff(email, role, branchId);
    }

    Long staffId(Long userId) {
        return jdbc.queryForObject("SELECT id FROM staff WHERE user_id = ?", Long.class, userId);
    }

    Long insertAvailabilityRaw(Long trainerId, Long branchId, int dayOfWeek, String startTimeSql, String endTimeSql) {
        return jdbc.queryForObject("""
            INSERT INTO trainer_availabilities (trainer_id, branch_id, day_of_week, start_time, end_time)
            VALUES (?, ?, ?, CAST(? AS time), CAST(? AS time)) RETURNING id""",
            Long.class, trainerId, branchId, dayOfWeek, startTimeSql, endTimeSql);
    }

    Long insertPtSession(Long trainerId, Long branchId, OffsetDateTime startAt, OffsetDateTime endAt, String status) {
        return insertPtSession(trainerId, branchId, startAt, endAt, status, null);
    }

    Long insertPtSession(Long trainerId, Long branchId, OffsetDateTime startAt, OffsetDateTime endAt,
            String status, String note) {
        int n = ++seq;
        Long memberId = sale.member(String.format("HV9%05d", n), String.format("0971%06d", n), branchId, "ACTIVE");
        Long pkgId = sale.pkg("PT" + n, "PT", 10, 2_000_000, branchId, "ACTIVE");
        Long membershipId = sale.membership(memberId, pkgId, "PT", branchId, "ACTIVE",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), 10);
        return jdbc.queryForObject("""
            INSERT INTO pt_sessions (trainer_id, member_id, membership_id, branch_id, start_at, end_at, status,
                                     commission_amount, cancelled_at, note)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""",
            Long.class,
            trainerId, memberId, membershipId, branchId, startAt, endAt, status,
            "DONE".equals(status) ? 200_000 : null,
            "CANCELLED".equals(status) ? startAt : null,
            note);
    }

    Long room(Long branchId, String name, int capacity) {
        return jdbc.queryForObject("INSERT INTO rooms (branch_id, name, capacity) VALUES (?, ?, ?) RETURNING id",
            Long.class, branchId, name, capacity);
    }

    Long classType(String name) {
        return jdbc.queryForObject("INSERT INTO class_types (name) VALUES (?) RETURNING id", Long.class, name);
    }

    Long classSchedule(Long branchId, Long classTypeId, Long trainerId, Long roomId, int dayOfWeek,
            LocalTime start, LocalTime end, LocalDate validFrom) {
        return jdbc.queryForObject("""
            INSERT INTO class_schedules (branch_id, class_type_id, trainer_id, room_id, day_of_week,
                                         start_time, end_time, capacity, valid_from, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 20, ?, 'ACTIVE') RETURNING id""",
            Long.class, branchId, classTypeId, trainerId, roomId, dayOfWeek, start, end, validFrom);
    }

    Long classSession(Long scheduleId, Long branchId, Long trainerId, Long roomId, LocalDate occurrenceDate,
            OffsetDateTime startAt, OffsetDateTime endAt, String status) {
        return jdbc.queryForObject("""
            INSERT INTO class_sessions (class_schedule_id, branch_id, trainer_id, room_id, occurrence_date,
                                        start_at, end_at, capacity, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 20, ?) RETURNING id""",
            Long.class, scheduleId, branchId, trainerId, roomId, occurrenceDate, startAt, endAt, status);
    }

    long count(String table) {
        return sale.count(table);
    }

    RequestPostProcessor bearer(Long userId, UserRole role) {
        return sale.bearer(userId, role);
    }
}
