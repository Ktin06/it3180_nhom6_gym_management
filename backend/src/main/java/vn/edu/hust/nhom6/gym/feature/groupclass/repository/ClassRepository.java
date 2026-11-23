package vn.edu.hust.nhom6.gym.feature.groupclass.repository;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.*;

/** SQL có tham số cho các bảng F8 và các phép tra cứu điều kiện liên chức năng. */
@Repository
@RequiredArgsConstructor
public class ClassRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public record MemberRow(Long id, Long userId, Long homeBranchId, String status,
        OffsetDateTime deletedAt, OffsetDateTime bookingLockedUntil) {}

    public record Room(Long branchId, int capacity, String status) {}

    public record Resources(Long roomBranchId, Long trainerBranchId, Integer roomCapacity,
        boolean active, boolean trainerRole) {}

    public <T> List<T> rows(String sql, Map<String, ?> params, Class<T> type) {
        return jdbc.query(sql, params, new DataClassRowMapper<>(type));
    }

    private long scalar(String sql, Map<String, ?> params) {
        return Objects.requireNonNull(jdbc.queryForObject(sql, params, Long.class));
    }

    public Schedule schedule(long id) {
        return rows("SELECT * FROM class_schedules WHERE id=:id", Map.of("id", id), Schedule.class)
            .stream().findFirst().orElseThrow(() -> new NotFoundException("Không tìm thấy lịch lớp."));
    }

    public Session session(long id, boolean lock) {
        return rows("SELECT * FROM class_sessions WHERE id=:id" + (lock ? " FOR UPDATE" : ""),
            Map.of("id", id), Session.class).stream().findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy buổi học."));
    }

    public MemberRow member(long id) {
        return rows("SELECT * FROM members WHERE id=:id AND deleted_at IS NULL", Map.of("id", id), MemberRow.class)
            .stream().findFirst().orElseThrow(() -> new NotFoundException("Không tìm thấy hội viên."));
    }

    public void lockMember(long id) {
        jdbc.queryForList("SELECT id FROM members WHERE id=:id FOR UPDATE", Map.of("id", id), Long.class);
    }

    public void lockBranch(long branchId) {
        jdbc.queryForList("SELECT id FROM branches WHERE id=:id FOR UPDATE", Map.of("id", branchId), Long.class);
    }

    public Resources resources(long branchId, ScheduleRequest request) {
        return rows("""
            SELECT r.branch_id room_branch_id, s.branch_id trainer_branch_id, r.capacity room_capacity,
              (b.status='ACTIVE' AND r.status='ACTIVE' AND s.status='ACTIVE' AND u.status='ACTIVE'
                AND t.status='ACTIVE') active, (u.role='TRAINER') trainer_role
            FROM branches b CROSS JOIN rooms r CROSS JOIN staff s
            JOIN users u ON u.id=s.user_id CROSS JOIN class_types t
            WHERE b.id=:branch AND r.id=:room AND s.id=:trainer AND t.id=:type
            """, Map.of("branch", branchId, "room", request.roomId(), "trainer", request.trainerId(),
                "type", request.classTypeId()), Resources.class).stream().findFirst()
            .orElseThrow(() -> new NotFoundException("Phòng, loại lớp hoặc huấn luyện viên không hợp lệ tại chi nhánh."));
    }

    public List<Schedule> activeSchedules(long branchId) {
        return rows("SELECT * FROM class_schedules WHERE branch_id=:branch AND status='ACTIVE' ORDER BY id",
            Map.of("branch", branchId), Schedule.class);
    }

    public List<Long> activeScheduleIds() {
        return jdbc.queryForList("SELECT id FROM class_schedules WHERE status='ACTIVE' ORDER BY id", Map.of(), Long.class);
    }

    public List<Schedule> schedules(long branchId, int page, int size) {
        return rows("SELECT * FROM class_schedules WHERE branch_id=:branch ORDER BY id DESC LIMIT :size OFFSET :offset",
            Map.of("branch", branchId, "size", size, "offset", (long) page * size), Schedule.class);
    }

    public long scheduleCount(long branchId) {
        return scalar("SELECT count(*) FROM class_schedules WHERE branch_id=:branch", Map.of("branch", branchId));
    }

    public Schedule saveSchedule(Long id, long branch, ScheduleRequest r) {
        var p = new MapSqlParameterSource().addValue("branch", branch).addValue("type", r.classTypeId())
            .addValue("trainer", r.trainerId()).addValue("room", r.roomId()).addValue("day", r.dayOfWeek())
            .addValue("start", r.startTime()).addValue("end", r.endTime()).addValue("capacity", r.capacity())
            .addValue("from", r.validFrom()).addValue("to", r.validTo()).addValue("id", id);
        if (id == null) {
            Long savedId = jdbc.queryForObject("""
                INSERT INTO class_schedules(branch_id,class_type_id,trainer_id,room_id,day_of_week,
                    start_time,end_time,capacity,valid_from,valid_to)
                VALUES (:branch,:type,:trainer,:room,:day,:start,:end,:capacity,:from,:to) RETURNING id
                """, p, Long.class);
            return schedule(savedId);
        }

        jdbc.update("""
            UPDATE class_schedules SET class_type_id=:type, trainer_id=:trainer, room_id=:room,
                day_of_week=:day, start_time=:start, end_time=:end, capacity=:capacity,
                valid_from=:from, valid_to=:to, updated_at=clock_timestamp() WHERE id=:id
            """, p);
        return schedule(id);
    }

    public void deactivate(long id) {
        jdbc.update("UPDATE class_schedules SET status='INACTIVE',updated_at=clock_timestamp() WHERE id=:id", Map.of("id", id));
    }

    public boolean occurrenceExists(long scheduleId, LocalDate date) {
        return scalar("SELECT count(*) FROM class_sessions WHERE class_schedule_id=:id AND occurrence_date=:date",
            Map.of("id", scheduleId, "date", date)) > 0;
    }

    public boolean resourceConflict(long trainer, long room, OffsetDateTime start, OffsetDateTime end) {
        return resourceConflict(trainer, room, start, end, 0);
    }

    public boolean resourceConflict(long trainer, long room, OffsetDateTime start, OffsetDateTime end, long excludedId) {
        var p = Map.of("trainer", trainer, "room", room, "start", start, "end", end, "excludedId", excludedId);
        // Khoảng nửa mở: giờ kết thúc bằng giờ bắt đầu tiếp theo không trùng.
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM class_sessions WHERE status<>'CANCELLED' AND id<>:excludedId
                AND (trainer_id=:trainer OR room_id=:room) AND start_at<:end AND end_at>:start)
              OR EXISTS(SELECT 1 FROM pt_sessions WHERE status<>'CANCELLED' AND trainer_id=:trainer
                AND start_at<:end AND end_at>:start)
            """, p, Boolean.class));
    }

    public boolean recurringResourceConflict(Long scheduleId, ScheduleRequest r) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            WITH busy AS (
              SELECT start_at,end_at FROM class_sessions WHERE status<>'CANCELLED'
                AND (trainer_id=:trainer OR room_id=:room)
              UNION ALL SELECT start_at,end_at FROM pt_sessions WHERE status<>'CANCELLED' AND trainer_id=:trainer
            ) SELECT EXISTS(SELECT 1 FROM busy,
                LATERAL generate_series((start_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date,
                    (end_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, interval '1 day') d
              WHERE d::date>=:from AND (:to::date IS NULL OR d::date<=:to)
                AND extract(isodow FROM d)=:day
                AND NOT EXISTS (SELECT 1 FROM class_sessions existing
                  WHERE existing.class_schedule_id=:schedule AND existing.occurrence_date=d::date)
                AND ((d::date + :start::time) AT TIME ZONE 'Asia/Ho_Chi_Minh')<end_at
                AND ((d::date + :end::time) AT TIME ZONE 'Asia/Ho_Chi_Minh')>start_at)
            """, new MapSqlParameterSource().addValue("trainer", r.trainerId()).addValue("room", r.roomId())
                .addValue("schedule", scheduleId).addValue("from", r.validFrom()).addValue("to", r.validTo())
                .addValue("day", r.dayOfWeek()).addValue("start", r.startTime()).addValue("end", r.endTime()), Boolean.class));
    }

    public int insertSession(Schedule s, LocalDate date, OffsetDateTime start, OffsetDateTime end) {
        return jdbc.update("""
            INSERT INTO class_sessions(class_schedule_id,branch_id,trainer_id,room_id,occurrence_date,start_at,end_at,capacity)
            VALUES (:id,:branch,:trainer,:room,:date,:start,:end,:capacity)
            ON CONFLICT (class_schedule_id,occurrence_date) DO NOTHING
            """, Map.of("id", s.id(), "branch", s.branchId(), "trainer", s.trainerId(), "room", s.roomId(),
                "date", date, "start", start, "end", end, "capacity", s.capacity()));
    }

    public boolean hasActiveTimePackage(long memberId, long branch, LocalDate date) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM memberships WHERE member_id=:id
                AND package_type='TIME' AND status='ACTIVE' AND (branch_id=:branch OR branch_id IS NULL)
                AND start_date<=:date AND end_date>=:date)
            """, Map.of("id", memberId, "branch", branch, "date", date), Boolean.class));
    }

    public boolean overlap(long memberId, Session session) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM class_bookings b JOIN class_sessions s ON s.id=b.class_session_id
                WHERE b.member_id=:member AND b.status IN ('BOOKED','ATTENDED','NO_SHOW')
                AND s.status<>'CANCELLED' AND s.id<>:id AND s.start_at<:end AND s.end_at>:start)
              OR EXISTS(SELECT 1 FROM pt_sessions WHERE member_id=:member AND status<>'CANCELLED'
                AND start_at<:end AND end_at>:start)
            """, Map.of("member", memberId, "id", session.id(), "start", session.startAt(), "end", session.endAt()), Boolean.class));
    }

    public boolean duplicate(long member, long session) {
        return scalar("SELECT count(*) FROM class_bookings WHERE member_id=:m AND class_session_id=:s AND status<>'CANCELLED'",
            Map.of("m", member, "s", session)) > 0;
    }

    public long occupied(long session) {
        return scalar("SELECT count(*) FROM class_bookings WHERE class_session_id=:id AND status IN ('BOOKED','ATTENDED','NO_SHOW')",
            Map.of("id", session));
    }

    public Booking booking(long id) {
        return rows("""
            SELECT b.*, m.booking_locked_until
            FROM class_bookings b JOIN members m ON m.id=b.member_id WHERE b.id=:id
            """, Map.of("id", id), Booking.class).stream().findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chỗ đặt."));
    }

    public long insertBooking(long session, long member, long actor, OffsetDateTime now) {
        return scalar("""
            INSERT INTO class_bookings(class_session_id,member_id,created_by,status,booked_at)
            VALUES (:session,:member,:actor,'BOOKED',:now) RETURNING id
            """, Map.of("session", session, "member", member, "actor", actor, "now", now));
    }

    public void cancel(long id, boolean late, boolean staff, OffsetDateTime now) {
        jdbc.update("""
            UPDATE class_bookings SET status='CANCELLED', cancelled_at=:now, cancel_reason=:reason,
                is_late_cancel=:late,updated_at=:now WHERE id=:id
            """, Map.of("id", id, "now", now, "reason", staff ? "BY_STAFF" : "BY_MEMBER", "late", late));
    }

    public Room room(long id) {
        return rows("SELECT branch_id, capacity, status FROM rooms WHERE id=:id", Map.of("id", id), Room.class)
            .stream().findFirst().orElseThrow(() -> new NotFoundException("Không tìm thấy phòng."));
    }

    public void reschedule(long id, OffsetDateTime start, OffsetDateTime end) {
        jdbc.update("""
            UPDATE class_sessions SET original_start_at=COALESCE(original_start_at,start_at),
                start_at=:start,end_at=:end,status='RESCHEDULED',updated_at=clock_timestamp()
            WHERE id=:id
            """, Map.of("id", id, "start", start, "end", end));
    }

    public void changeRoom(long id, long room) {
        jdbc.update("UPDATE class_sessions SET room_id=:room,updated_at=clock_timestamp() WHERE id=:id",
            Map.of("id", id, "room", room));
    }

    public List<Long> bookedMemberIds(long sessionId) {
        return jdbc.queryForList("""
            SELECT member_id FROM class_bookings WHERE class_session_id=:id AND status='BOOKED' ORDER BY member_id
            """, Map.of("id", sessionId), Long.class);
    }

    public void cancelSession(long id, OffsetDateTime now) {
        var params = Map.of("id", id, "now", now);
        jdbc.update("UPDATE class_sessions SET status='CANCELLED',updated_at=:now WHERE id=:id", params);
        jdbc.update("""
            UPDATE class_bookings SET status='CANCELLED',cancelled_at=:now,cancel_reason='SESSION_CANCELLED',
                is_late_cancel=false,updated_at=:now WHERE class_session_id=:id AND status='BOOKED'
            """, params);
    }

    public boolean teaches(long sessionId, long userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM staff t JOIN class_sessions s ON s.trainer_id=t.id
                WHERE s.id=:session AND t.user_id=:user AND t.status='ACTIVE' AND t.branch_id=s.branch_id)
            """, Map.of("session", sessionId, "user", userId), Boolean.class));
    }

    private static final String ATTENDANCE_SELECT = """
        SELECT b.id booking_id,b.member_id,m.member_code,m.full_name,b.status,b.arrived_at,
            b.attendance_marked_by,b.updated_at
        FROM class_bookings b JOIN members m ON m.id=b.member_id
        """;

    public List<AttendanceRow> attendance(long sessionId) {
        return rows(ATTENDANCE_SELECT + """
            WHERE b.class_session_id=:session AND b.status<>'CANCELLED' ORDER BY b.id
            """, Map.of("session", sessionId), AttendanceRow.class);
    }

    public AttendanceRow attendanceRow(long sessionId, long bookingId) {
        return rows(ATTENDANCE_SELECT + "WHERE b.class_session_id=:session AND b.id=:id",
            Map.of("session", sessionId, "id", bookingId), AttendanceRow.class).stream().findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chỗ đặt trong buổi học."));
    }

    public boolean markAttendance(AttendanceRow old, AttendanceStatus status, long actor) {
        return jdbc.update("""
            UPDATE class_bookings SET status=:status,attendance_marked_by=:actor,
                updated_at=GREATEST(clock_timestamp(),updated_at + interval '1 microsecond')
            WHERE id=:id AND updated_at=:version
            """, Map.of("status", status.name(), "actor", actor, "id", old.bookingId(), "version", old.updatedAt())) == 1;
    }

    public void auditAttendance(AttendanceRow old, AttendanceStatus status, long branch, long actor, OffsetDateTime now) {
        var params = new MapSqlParameterSource()
            .addValue("actor", actor).addValue("branch", branch).addValue("id", old.bookingId())
            .addValue("oldStatus", old.status()).addValue("oldMarker", old.attendanceMarkedBy(), java.sql.Types.BIGINT)
            .addValue("status", status.name()).addValue("now", now);
        jdbc.update("""
            INSERT INTO audit_logs(user_id,branch_id,action,entity,entity_id,old_value,new_value,created_at,updated_at)
            VALUES (:actor,:branch,'CLASS_ATTENDANCE_CORRECT','class_bookings',:id,
                jsonb_build_object('status',:oldStatus::text,'attendanceMarkedBy',:oldMarker::bigint),
                jsonb_build_object('status',:status::text,'attendanceMarkedBy',:actor::bigint),:now,:now)
            """, params);
    }

    public void unlockBooking(long member, long branch, long actor, OffsetDateTime oldUntil, OffsetDateTime now) {
        var params = Map.of("actor", actor, "branch", branch, "id", member, "oldUntil", oldUntil, "now", now);
        jdbc.update("UPDATE members SET booking_locked_until=NULL,updated_at=:now WHERE id=:id", params);
        jdbc.update("""
            INSERT INTO audit_logs(user_id,branch_id,action,entity,entity_id,old_value,new_value,created_at,updated_at)
            VALUES (:actor,:branch,'CLASS_BOOKING_UNLOCK','members',:id,
                jsonb_build_object('lockedUntil',:oldUntil::text),jsonb_build_object('lockedUntil',NULL),:now,:now)
            """, params);
    }

    public OffsetDateTime latestLock(long member) {
        var times = jdbc.queryForList("""
            SELECT created_at FROM audit_logs WHERE entity='members' AND entity_id=:id
              AND action='CLASS_BOOKING_LOCK' ORDER BY created_at DESC,id DESC LIMIT 1
            """, Map.of("id", member), OffsetDateTime.class);
        return times.isEmpty() ? OffsetDateTime.MIN : times.getFirst();
    }

    public long violations(long member, OffsetDateTime month, OffsetDateTime nextMonth, OffsetDateTime lastLock) {
        return scalar("""
            SELECT count(*) FROM class_bookings b JOIN class_sessions s ON s.id=b.class_session_id
            WHERE b.member_id=:member AND ((b.is_late_cancel AND b.cancelled_at>=:month AND b.cancelled_at<:next
                AND b.cancelled_at>:last) OR (b.status='NO_SHOW' AND s.start_at>=:month AND s.start_at<:next AND s.start_at>:last))
            """, Map.of("member", member, "month", month, "next", nextMonth, "last", lastLock));
    }

    public void lockBooking(long member, long branch, long actor, OffsetDateTime now, OffsetDateTime until) {
        jdbc.update("UPDATE members SET booking_locked_until=:until,updated_at=:now WHERE id=:id",
            Map.of("until", until, "now", now, "id", member));
        jdbc.update("""
            INSERT INTO audit_logs(user_id,branch_id,action,entity,entity_id,new_value,created_at,updated_at)
            VALUES (:actor,:branch,'CLASS_BOOKING_LOCK','members',:member,
                jsonb_build_object('lockedUntil',:until::text),:now,:now)
            """, Map.of("actor", actor, "branch", branch, "member", member, "until", until, "now", now));
    }
}
