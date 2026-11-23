package vn.edu.hust.nhom6.gym.feature.groupclass.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.BranchOption;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.CalendarSession;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.MemberSession;

@Repository
@RequiredArgsConstructor
public class ClassCalendarRepository {
    private static final String SESSION_COLUMNS = """
        s.id,s.branch_id,b.name branch_name,t.name class_name,r.name room_name,st.full_name trainer_name,
        s.start_at,s.end_at,s.status
        """;

    private static final String SESSION_TABLES = """
        FROM class_sessions s JOIN branches b ON b.id=s.branch_id
        JOIN class_schedules sc ON sc.id=s.class_schedule_id JOIN class_types t ON t.id=sc.class_type_id
        JOIN rooms r ON r.id=s.room_id JOIN staff st ON st.id=s.trainer_id
        """;

    private static final String DATE_RANGE = "s.start_at>=:start AND s.start_at<:end";

    private static final String PUBLIC_FILTERS = """
        AND b.status='ACTIVE' AND s.status IN ('SCHEDULED','RESCHEDULED','CANCELLED')
        AND (:branch::bigint IS NULL OR s.branch_id=:branch)
        """;

    private static final String ORDERED = " ORDER BY s.start_at,s.id";

    private final ClassRepository classes;
    private final NamedParameterJdbcTemplate jdbc;

    public List<BranchOption> branches() {
        return classes.rows("SELECT id,name FROM branches WHERE status='ACTIVE' ORDER BY name,id",
            Map.of(), BranchOption.class);
    }

    public boolean activeBranch(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM branches WHERE id=:id AND status='ACTIVE')",
            Map.of("id", id), Boolean.class));
    }

    public List<CalendarSession> publicSessions(Long branch, OffsetDateTime start, OffsetDateTime end) {
        return classes.rows("SELECT " + SESSION_COLUMNS + SESSION_TABLES + " WHERE " + DATE_RANGE
            + " " + PUBLIC_FILTERS + ORDERED, dates(start, end).addValue("branch", branch).getValues(), CalendarSession.class);
    }

    public List<CalendarSession> trainerSessions(long actorId, long branch, OffsetDateTime start, OffsetDateTime end) {
        return classes.rows("SELECT " + SESSION_COLUMNS + SESSION_TABLES + " WHERE " + DATE_RANGE
            + " AND st.user_id=:actor AND st.status='ACTIVE' AND s.branch_id=:branch" + ORDERED,
            dates(start, end).addValue("actor", actorId).addValue("branch", branch).getValues(), CalendarSession.class);
    }

    public long memberId(long userId) {
        var ids = jdbc.queryForList("SELECT id FROM members WHERE user_id=:id AND deleted_at IS NULL",
            Map.of("id", userId), Long.class);
        return ids.stream().findFirst().orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ hội viên."));
    }

    public List<MemberSession> memberSessions(long member, Long branch, OffsetDateTime start, OffsetDateTime end) {
        return classes.rows("SELECT " + SESSION_COLUMNS + """
            ,s.capacity,own.id booking_id,own.status booking_status,s.original_start_at,
            GREATEST(0,s.capacity-(SELECT count(*) FROM class_bookings occupied
                WHERE occupied.class_session_id=s.id AND occupied.status IN ('BOOKED','ATTENDED','NO_SHOW'))) remaining
            """ + SESSION_TABLES + """
            LEFT JOIN LATERAL (
                SELECT cb.id,cb.status FROM class_bookings cb WHERE cb.class_session_id=s.id AND cb.member_id=:member
                ORDER BY (cb.status<>'CANCELLED') DESC,cb.id DESC LIMIT 1
            ) own ON true
            WHERE
            """ + DATE_RANGE + " " + PUBLIC_FILTERS + " AND (s.status<>'CANCELLED' OR own.id IS NOT NULL)" + ORDERED,
            dates(start, end).addValue("branch", branch).addValue("member", member).getValues(), MemberSession.class);
    }

    private static MapSqlParameterSource dates(OffsetDateTime start, OffsetDateTime end) {
        return new MapSqlParameterSource().addValue("start", start).addValue("end", end);
    }
}
