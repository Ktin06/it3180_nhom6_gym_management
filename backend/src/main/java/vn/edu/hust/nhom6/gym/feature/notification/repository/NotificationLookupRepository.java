package vn.edu.hust.nhom6.gym.feature.notification.repository;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Truy vấn chỉ đọc sang các bảng nguồn của F10 ({@code memberships}, {@code class_bookings},
 * {@code class_sessions}, {@code members}, {@code branches}, {@code rooms}) để tránh ghép cặp
 * trực tiếp entity giữa các module (ADR 0007).
 */
@Repository
@RequiredArgsConstructor
public class NotificationLookupRepository {

    private final EntityManager entities;

    public record BranchHours(LocalTime openTime, LocalTime closeTime) {}

    public record ExpiringMembershipRow(
        long membershipId,
        long userId,
        String packageName,
        String packageType,
        LocalDate endDate,
        Integer remainingSessions,
        LocalTime branchOpenTime,
        LocalTime branchCloseTime
    ) {}

    public record UpcomingClassBookingRow(
        long bookingId,
        long sessionId,
        long userId,
        String className,
        String roomName,
        OffsetDateTime startAt,
        LocalTime branchOpenTime,
        LocalTime branchCloseTime
    ) {}

    /**
     * Tìm các gói ACTIVE sắp hết hạn (trong {@code [today, maxEndDate]}) hoặc gói SESSION ACTIVE
     * còn không quá {@code sessionWarningThreshold} buổi, hội viên có tài khoản, chưa có gói
     * UPCOMING cùng loại và chưa từng tạo thông báo {@code MEMBERSHIP_EXPIRING}.
     */
    @SuppressWarnings("unchecked")
    public List<ExpiringMembershipRow> findExpiringMemberships(
            LocalDate today, LocalDate maxEndDate, int sessionWarningThreshold) {
        List<Object[]> rows = entities.createNativeQuery("""
            SELECT m.id,
                   mem.user_id,
                   p.name,
                   m.package_type,
                   m.end_date,
                   m.remaining_sessions,
                   b.open_time,
                   b.close_time
            FROM memberships m
            JOIN members mem ON mem.id = m.member_id
            JOIN packages p ON p.id = m.package_id
            JOIN branches b ON b.id = COALESCE(m.branch_id, m.sold_branch_id, mem.home_branch_id)
            WHERE m.status = 'ACTIVE'
              AND mem.deleted_at IS NULL
              AND mem.user_id IS NOT NULL
              AND (
                  (m.end_date >= :today AND m.end_date <= :maxEndDate)
                  OR (m.package_type = 'SESSION' AND m.remaining_sessions IS NOT NULL AND m.remaining_sessions <= :threshold)
              )
              AND NOT EXISTS (
                  SELECT 1 FROM memberships u
                  WHERE u.member_id = m.member_id
                    AND u.package_type = m.package_type
                    AND u.status = 'UPCOMING'
              )
              AND NOT EXISTS (
                  SELECT 1 FROM notifications n
                  WHERE n.type = 'MEMBERSHIP_EXPIRING'
                    AND n.related_entity = 'memberships'
                    AND n.related_id = m.id
              )
            ORDER BY m.id ASC
            """)
            .setParameter("today", today)
            .setParameter("maxEndDate", maxEndDate)
            .setParameter("threshold", sessionWarningThreshold)
            .getResultList();

        return rows.stream().map(r -> new ExpiringMembershipRow(
            ((Number) r[0]).longValue(),
            ((Number) r[1]).longValue(),
            (String) r[2],
            (String) r[3],
            toLocalDate(r[4]),
            r[5] == null ? null : ((Number) r[5]).intValue(),
            toLocalTime(r[6]),
            toLocalTime(r[7])
        )).toList();
    }

    /**
     * Tìm các lượt đặt chỗ {@code BOOKED} cho buổi lớp {@code SCHEDULED}/{@code RESCHEDULED}
     * bắt đầu trong khoảng {@code (now, remindUntil]}, hội viên có tài khoản và chưa từng nhận
     * thông báo {@code CLASS_REMINDER} cho lượt đặt chỗ đó.
     */
    @SuppressWarnings("unchecked")
    public List<UpcomingClassBookingRow> findUpcomingBookedClasses(
            OffsetDateTime now, OffsetDateTime remindUntil) {
        List<Object[]> rows = entities.createNativeQuery("""
            SELECT cb.id,
                   cs.id,
                   mem.user_id,
                   ct.name,
                   r.name,
                   cs.start_at,
                   b.open_time,
                   b.close_time
            FROM class_bookings cb
            JOIN class_sessions cs ON cs.id = cb.class_session_id
            JOIN class_schedules sch ON sch.id = cs.class_schedule_id
            JOIN class_types ct ON ct.id = sch.class_type_id
            JOIN rooms r ON r.id = cs.room_id
            JOIN members mem ON mem.id = cb.member_id
            JOIN branches b ON b.id = cs.branch_id
            WHERE cb.status = 'BOOKED'
              AND cs.status IN ('SCHEDULED', 'RESCHEDULED')
              AND cs.start_at > :now
              AND cs.start_at <= :remindUntil
              AND mem.deleted_at IS NULL
              AND mem.user_id IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM notifications n
                  WHERE n.type = 'CLASS_REMINDER'
                    AND n.related_entity = 'class_bookings'
                    AND n.related_id = cb.id
              )
            ORDER BY cs.start_at ASC, cb.id ASC
            """)
            .setParameter("now", now)
            .setParameter("remindUntil", remindUntil)
            .getResultList();

        return rows.stream().map(r -> new UpcomingClassBookingRow(
            ((Number) r[0]).longValue(),
            ((Number) r[1]).longValue(),
            ((Number) r[2]).longValue(),
            (String) r[3],
            (String) r[4],
            toOffsetDateTime(r[5]),
            toLocalTime(r[6]),
            toLocalTime(r[7])
        )).toList();
    }

    @SuppressWarnings("unchecked")
    public Optional<BranchHours> findBranchHours(long branchId) {
        List<Object[]> rows = entities.createNativeQuery("""
            SELECT open_time, close_time FROM branches WHERE id = :id
            """)
            .setParameter("id", branchId)
            .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.getFirst();
        return Optional.of(new BranchHours(toLocalTime(row[0]), toLocalTime(row[1])));
    }

    @SuppressWarnings("unchecked")
    public Optional<String> findRoomName(long roomId) {
        List<Object> rows = entities.createNativeQuery("""
            SELECT name FROM rooms WHERE id = :id
            """)
            .setParameter("id", roomId)
            .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.ofNullable((String) rows.getFirst());
    }

    @SuppressWarnings("unchecked")
    public Optional<String> findSessionClassName(long sessionId) {
        List<Object> rows = entities.createNativeQuery("""
            SELECT ct.name
            FROM class_sessions cs
            JOIN class_schedules sch ON sch.id = cs.class_schedule_id
            JOIN class_types ct ON ct.id = sch.class_type_id
            WHERE cs.id = :id
            """)
            .setParameter("id", sessionId)
            .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.ofNullable((String) rows.getFirst());
    }

    /** Ánh xạ {@code memberId -> userId} cho các hội viên chưa xóa mềm và đã có tài khoản. */
    @SuppressWarnings("unchecked")
    public Map<Long, Long> findMemberUserIds(Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = entities.createNativeQuery("""
            SELECT id, user_id
            FROM members
            WHERE id IN (:ids)
              AND deleted_at IS NULL
              AND user_id IS NOT NULL
            """)
            .setParameter("ids", memberIds)
            .getResultList();
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return map;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate ld) {
            return ld;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        throw new IllegalStateException("Không đọc được ngày: " + value);
    }

    private static LocalTime toLocalTime(Object value) {
        if (value instanceof LocalTime lt) {
            return lt;
        }
        if (value instanceof Time sqlTime) {
            return sqlTime.toLocalTime();
        }
        throw new IllegalStateException("Không đọc được giờ: " + value);
    }

    private static OffsetDateTime toOffsetDateTime(Object value) {
        if (value instanceof OffsetDateTime odt) {
            return odt;
        }
        if (value instanceof java.time.Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant().atOffset(ZoneOffset.UTC);
        }
        throw new IllegalStateException("Không đọc được thời điểm: " + value);
    }
}
