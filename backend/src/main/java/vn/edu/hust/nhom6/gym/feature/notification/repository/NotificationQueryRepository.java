package vn.edu.hust.nhom6.gym.feature.notification.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.feature.notification.service.OpeningHours;

/**
 * Truy vấn chỉ đọc nhiều bảng phục vụ các tác vụ nền F10 (nhắc gói tập F3, nhắc lịch lớp F8,
 * giờ mở cửa chi nhánh và kiểm tra điều kiện gửi email) mà không ghép cặp trực tiếp vào service/repository
 * của chức năng khác.
 */
@Repository
@RequiredArgsConstructor
public class NotificationQueryRepository {
    /** Khóa tư vấn PostgreSQL cho lượt nhắc gói và lượt nhắc lớp để hai lượt chồng nhau không tạo trùng. */
    public static final long LOCK_MEMBERSHIP_REMINDER = 1_021_001L;
    public static final long LOCK_CLASS_REMINDER = 1_021_002L;

    private final EntityManager entities;

    /** Gói tập cần nhắc sắp hết hạn hoặc sắp hết buổi (UC-F10.1). */
    public record ExpiringMembershipRow(
            Long membershipId,
            Long userId,
            Long branchId,
            String packageName,
            String packageType,
            LocalDate endDate,
            Integer remainingSessions) { }

    /** Chỗ đặt lớp nhóm cần nhắc trước giờ học (UC-F10.2). */
    public record ClassReminderRow(
            Long bookingId,
            Long userId,
            Long branchId,
            String className,
            OffsetDateTime startAt,
            String roomName,
            String branchName) { }

    /** Thông tin người nhận, khung giờ mở cửa và trạng thái còn hợp lệ tại thời điểm gửi email. */
    public record EmailTarget(String email, String userStatus, OpeningHours hours, boolean stillRelevant) { }

    /** Giữ khóa tư vấn tới hết giao dịch hiện tại để tránh hai tiến trình cùng chạy một tác vụ nhắc. */
    public void lockForRun(long key) {
        entities.createNativeQuery("SELECT 1 FROM pg_advisory_xact_lock(:key)")
            .setParameter("key", key)
            .getSingleResult();
    }

    /** Danh sách mã chi nhánh (gồm cả chi nhánh ngừng hoạt động vì gói cũ vẫn có thể đang còn hạn). */
    @SuppressWarnings("unchecked")
    public List<Long> findBranchIds() {
        return entities.createNativeQuery("SELECT id FROM branches ORDER BY id", Long.class).getResultList();
    }

    /** Đọc giờ mở/đóng cửa của chi nhánh dạng chuỗi {@code HH24:MI:SS} để tránh lệch múi giờ qua JDBC. */
    public Optional<OpeningHours> findOpeningHours(Long branchId) {
        List<?> rows = entities.createNativeQuery("""
            SELECT to_char(open_time, 'HH24:MI:SS') || ' ' || to_char(close_time, 'HH24:MI:SS')
            FROM branches WHERE id = :id""", String.class)
            .setParameter("id", branchId)
            .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(parseHours((String) rows.getFirst()));
    }

    /**
     * Chọn các gói {@code ACTIVE} thuộc chi nhánh {@code branchId} (gói toàn chuỗi tính theo chi nhánh gốc hội viên)
     * có {@code end_date} trong {@code [today, expireBefore)} hoặc gói {@code SESSION} còn không quá
     * {@code sessionThreshold} buổi, chưa có gói {@code UPCOMING} cùng loại cùng phạm vi, hội viên có tài khoản
     * và chưa từng nhận thông báo {@code MEMBERSHIP_EXPIRING}.
     */
    @SuppressWarnings("unchecked")
    public List<ExpiringMembershipRow> findExpiringForReminder(
            Long branchId, LocalDate today, LocalDate expireBefore, int sessionThreshold) {
        NativeQuery<Object[]> query = nativeQuery("""
            SELECT ms.id AS membership_id,
                   m.user_id,
                   p.name AS package_name,
                   ms.package_type,
                   to_char(ms.end_date, 'YYYY-MM-DD') AS end_date,
                   ms.remaining_sessions
            FROM memberships ms
            JOIN members m ON m.id = ms.member_id
            JOIN packages p ON p.id = ms.package_id
            WHERE ms.status = 'ACTIVE'
              AND m.deleted_at IS NULL
              AND m.user_id IS NOT NULL
              AND COALESCE(ms.branch_id, m.home_branch_id) = :branchId
              AND ms.end_date >= CAST(:today AS date)
              AND (
                  ms.end_date < CAST(:expireBefore AS date)
                  OR (ms.package_type = 'SESSION' AND ms.remaining_sessions <= :sessionThreshold)
              )
              AND NOT EXISTS (
                  SELECT 1 FROM memberships up
                  WHERE up.member_id = ms.member_id
                    AND up.package_type = ms.package_type
                    AND up.branch_id IS NOT DISTINCT FROM ms.branch_id
                    AND up.status = 'UPCOMING'
              )
              AND NOT EXISTS (
                  SELECT 1 FROM notifications n
                  WHERE n.type = 'MEMBERSHIP_EXPIRING'
                    AND n.related_entity = 'memberships'
                    AND n.related_id = ms.id
              )
            ORDER BY ms.id""")
            .addScalar("membership_id", StandardBasicTypes.LONG)
            .addScalar("user_id", StandardBasicTypes.LONG)
            .addScalar("package_name", StandardBasicTypes.STRING)
            .addScalar("package_type", StandardBasicTypes.STRING)
            .addScalar("end_date", StandardBasicTypes.STRING)
            .addScalar("remaining_sessions", StandardBasicTypes.INTEGER);
        query.setParameter("branchId", branchId)
            .setParameter("today", today.toString())
            .setParameter("expireBefore", expireBefore.toString())
            .setParameter("sessionThreshold", sessionThreshold);
        return query.getResultList().stream()
            .map(r -> new ExpiringMembershipRow(
                (Long) r[0],
                (Long) r[1],
                branchId,
                (String) r[2],
                (String) r[3],
                LocalDate.parse((String) r[4]),
                (Integer) r[5]))
            .toList();
    }

    /**
     * Chọn các chỗ đặt lớp {@code BOOKED} của buổi {@code SCHEDULED}/{@code RESCHEDULED} bắt đầu trong
     * {@code (now, until]}, hội viên có tài khoản và chưa từng nhận thông báo {@code CLASS_REMINDER}.
     */
    @SuppressWarnings("unchecked")
    public List<ClassReminderRow> findClassReminders(OffsetDateTime now, OffsetDateTime until) {
        NativeQuery<Object[]> query = nativeQuery("""
            SELECT cb.id AS booking_id,
                   m.user_id,
                   cs.branch_id,
                   ct.name AS class_name,
                   cs.start_at,
                   r.name AS room_name,
                   b.name AS branch_name
            FROM class_bookings cb
            JOIN class_sessions cs ON cs.id = cb.class_session_id
            JOIN class_types ct ON ct.id = cs.class_type_id
            JOIN rooms r ON r.id = cs.room_id
            JOIN branches b ON b.id = cs.branch_id
            JOIN members m ON m.id = cb.member_id
            WHERE cb.status = 'BOOKED'
              AND cs.status IN ('SCHEDULED', 'RESCHEDULED')
              AND cs.start_at > :now
              AND cs.start_at <= :until
              AND m.user_id IS NOT NULL
              AND m.deleted_at IS NULL
              AND NOT EXISTS (
                  SELECT 1 FROM notifications n
                  WHERE n.type = 'CLASS_REMINDER'
                    AND n.related_entity = 'class_bookings'
                    AND n.related_id = cb.id
              )
            ORDER BY cs.start_at, cb.id""")
            .addScalar("booking_id", StandardBasicTypes.LONG)
            .addScalar("user_id", StandardBasicTypes.LONG)
            .addScalar("branch_id", StandardBasicTypes.LONG)
            .addScalar("class_name", StandardBasicTypes.STRING)
            .addScalar("start_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .addScalar("room_name", StandardBasicTypes.STRING)
            .addScalar("branch_name", StandardBasicTypes.STRING);
        query.setParameter("now", now).setParameter("until", until);
        return query.getResultList().stream()
            .map(r -> new ClassReminderRow(
                (Long) r[0],
                (Long) r[1],
                (Long) r[2],
                (String) r[3],
                ((OffsetDateTime) r[4]).withOffsetSameInstant(ZoneOffset.UTC),
                (String) r[5],
                (String) r[6]))
            .toList();
    }

    /** Đọc thông tin buổi lớp hiện tại của một lượt đặt chỗ để dựng lại nội dung email nhắc lớp lúc gửi. */
    @SuppressWarnings("unchecked")
    public Optional<ClassReminderRow> findClassReminderByBookingId(Long bookingId) {
        NativeQuery<Object[]> query = nativeQuery("""
            SELECT cb.id AS booking_id,
                   m.user_id,
                   cs.branch_id,
                   ct.name AS class_name,
                   cs.start_at,
                   r.name AS room_name,
                   b.name AS branch_name
            FROM class_bookings cb
            JOIN class_sessions cs ON cs.id = cb.class_session_id
            JOIN class_types ct ON ct.id = cs.class_type_id
            JOIN rooms r ON r.id = cs.room_id
            JOIN branches b ON b.id = cs.branch_id
            JOIN members m ON m.id = cb.member_id
            WHERE cb.id = :bookingId""")
            .addScalar("booking_id", StandardBasicTypes.LONG)
            .addScalar("user_id", StandardBasicTypes.LONG)
            .addScalar("branch_id", StandardBasicTypes.LONG)
            .addScalar("class_name", StandardBasicTypes.STRING)
            .addScalar("start_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .addScalar("room_name", StandardBasicTypes.STRING)
            .addScalar("branch_name", StandardBasicTypes.STRING);
        query.setParameter("bookingId", bookingId);
        List<Object[]> rows = query.getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] r = rows.getFirst();
        return Optional.of(new ClassReminderRow(
            (Long) r[0],
            (Long) r[1],
            (Long) r[2],
            (String) r[3],
            ((OffsetDateTime) r[4]).withOffsetSameInstant(ZoneOffset.UTC),
            (String) r[5],
            (String) r[6]));
    }

    /** Lấy danh sách mã dòng {@code EMAIL} đang {@code PENDING} đã tới {@code scheduled_at}, cũ trước. */
    @SuppressWarnings("unchecked")
    public List<Long> findDueEmailIds(OffsetDateTime now, int limit) {
        return entities.createNativeQuery("""
            SELECT id FROM notifications
            WHERE status = 'PENDING' AND channel = 'EMAIL' AND scheduled_at <= :now
            ORDER BY scheduled_at, id
            LIMIT :limit""", Long.class)
            .setParameter("now", now)
            .setParameter("limit", limit)
            .getResultList();
    }

    /**
     * Đọc email người nhận, trạng thái tài khoản, giờ mở cửa chi nhánh (suy ra từ {@code related_entity},
     * dự phòng về chi nhánh gốc hội viên hoặc chi nhánh nhân viên) và kiểm tra lại điều kiện nhắc còn hiệu lực
     * tại thời điểm gửi {@code now}.
     */
    public Optional<EmailTarget> findEmailTarget(Long notificationId, OffsetDateTime now) {
        List<?> rows = entities.createNativeQuery("""
            SELECT u.email, u.status,
                   to_char(b.open_time, 'HH24:MI:SS') || ' ' || to_char(b.close_time, 'HH24:MI:SS'),
                   CASE n.type
                       WHEN 'CLASS_REMINDER' THEN EXISTS (
                           SELECT 1 FROM class_bookings cb
                           JOIN class_sessions cs ON cs.id = cb.class_session_id
                           WHERE cb.id = n.related_id
                             AND cb.status = 'BOOKED'
                             AND cs.status IN ('SCHEDULED', 'RESCHEDULED')
                             AND cs.start_at > :now)
                       WHEN 'MEMBERSHIP_EXPIRING' THEN EXISTS (
                           SELECT 1 FROM memberships ms
                           WHERE ms.id = n.related_id
                             AND ms.status = 'ACTIVE'
                             AND NOT EXISTS (
                                 SELECT 1 FROM memberships up
                                 WHERE up.member_id = ms.member_id
                                   AND up.package_type = ms.package_type
                                   AND up.branch_id IS NOT DISTINCT FROM ms.branch_id
                                   AND up.status = 'UPCOMING'))
                       ELSE true
                   END AS still_relevant
            FROM notifications n
            JOIN users u ON u.id = n.user_id
            LEFT JOIN branches b ON b.id = COALESCE(
                CASE n.related_entity
                    WHEN 'memberships' THEN (SELECT COALESCE(ms.branch_id, m.home_branch_id)
                                             FROM memberships ms JOIN members m ON m.id = ms.member_id
                                             WHERE ms.id = n.related_id)
                    WHEN 'class_bookings' THEN (SELECT cs.branch_id
                                                FROM class_bookings cb
                                                JOIN class_sessions cs ON cs.id = cb.class_session_id
                                                WHERE cb.id = n.related_id)
                    WHEN 'class_sessions' THEN (SELECT cs.branch_id FROM class_sessions cs WHERE cs.id = n.related_id)
                    WHEN 'pt_sessions' THEN (SELECT ps.branch_id FROM pt_sessions ps WHERE ps.id = n.related_id)
                END,
                (SELECT m.home_branch_id FROM members m WHERE m.user_id = n.user_id),
                (SELECT s.branch_id FROM staff s WHERE s.user_id = n.user_id))
            WHERE n.id = :id""")
            .setParameter("id", notificationId)
            .setParameter("now", now)
            .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] r = (Object[]) rows.getFirst();
        String hours = (String) r[2];
        return Optional.of(new EmailTarget(
            (String) r[0],
            (String) r[1],
            hours == null ? null : parseHours(hours),
            Boolean.TRUE.equals(r[3])));
    }

    private static OpeningHours parseHours(String text) {
        String[] parts = text.split(" ");
        return new OpeningHours(LocalTime.parse(parts[0]), LocalTime.parse(parts[1]));
    }

    @SuppressWarnings("unchecked")
    private NativeQuery<Object[]> nativeQuery(String sql) {
        return entities.createNativeQuery(sql).unwrap(NativeQuery.class);
    }
}
