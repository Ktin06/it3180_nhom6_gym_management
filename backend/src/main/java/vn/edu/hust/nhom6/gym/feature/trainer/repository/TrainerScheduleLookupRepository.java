package vn.edu.hust.nhom6.gym.feature.trainer.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * Truy vấn <b>chỉ đọc</b> có tham số của F9 tới các bảng không thuộc sở hữu trực tiếp của thẻ này: {@code staff},
 * {@code users}, {@code pt_sessions}, {@code members}, {@code class_sessions}, {@code class_schedules},
 * {@code class_types}, {@code rooms} (giả định G4). Không sửa mã F8 và chưa tạo entity {@code PtSession}.
 */
@Repository
@RequiredArgsConstructor
public class TrainerScheduleLookupRepository {
    private final EntityManager entities;

    public record TrainerSummary(Long id, Long branchId, String fullName, boolean active, String role) {
        public boolean isActiveTrainer() {
            return active && "TRAINER".equals(role);
        }
    }

    public record BookedPtSlot(Long id, OffsetDateTime startAt, OffsetDateTime endAt) { }

    public record PtSessionRow(
            Long id,
            Long memberId,
            String memberCode,
            String memberName,
            Long membershipId,
            Long branchId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String status,
            String note) {
    }

    public record ClassSessionRow(
            Long id,
            Long classScheduleId,
            Long branchId,
            Long classTypeId,
            String classTypeName,
            Long roomId,
            String roomName,
            LocalDate occurrenceDate,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            OffsetDateTime originalStartAt,
            Integer capacity,
            String status) {
    }

    public Optional<TrainerSummary> findTrainer(Long trainerId) {
        List<Object[]> rows = rows(native_("""
            SELECT s.id, s.branch_id, s.full_name, s.status, u.role
            FROM staff s JOIN users u ON u.id = s.user_id
            WHERE s.id = :id""")
            .addScalar("id", StandardBasicTypes.LONG)
            .addScalar("branch_id", StandardBasicTypes.LONG)
            .addScalar("full_name", StandardBasicTypes.STRING)
            .addScalar("status", StandardBasicTypes.STRING)
            .addScalar("role", StandardBasicTypes.STRING)
            .setParameter("id", trainerId));
        return rows.stream().findFirst().map(r -> new TrainerSummary(
            (Long) r[0], (Long) r[1], (String) r[2], "ACTIVE".equals(r[3]), (String) r[4]));
    }

    /**
     * Khóa dòng {@code staff} của PT bằng {@code FOR NO KEY UPDATE} để hai thao tác cùng lúc trên khung rảnh không
     * vượt qua kiểm tra chồng khung (BR-F9.14), đồng thời không chặn khóa {@code KEY SHARE} từ khóa ngoại F8.
     */
    public void lockTrainer(Long trainerId) {
        entities.createNativeQuery("SELECT id FROM staff WHERE id = :id FOR NO KEY UPDATE")
            .setParameter("id", trainerId)
            .getResultList();
    }

    /** Các buổi PT {@code BOOKED} chưa diễn ra ({@code start_at > now}) của PT để kiểm tra BR-F9.15 (E5). */
    public List<BookedPtSlot> findFutureBookedPtSessions(Long trainerId, OffsetDateTime now) {
        List<Object[]> rows = rows(native_("""
            SELECT id, start_at, end_at
            FROM pt_sessions
            WHERE trainer_id = :trainerId AND status = 'BOOKED' AND start_at > :now
            ORDER BY start_at, id""")
            .addScalar("id", StandardBasicTypes.LONG)
            .addScalar("start_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .addScalar("end_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .setParameter("trainerId", trainerId)
            .setParameter("now", now));
        return rows.stream()
            .map(r -> new BookedPtSlot((Long) r[0], (OffsetDateTime) r[1], (OffsetDateTime) r[2]))
            .toList();
    }

    /** Buổi PT của huấn luyện viên có {@code start_at} trong khoảng nửa mở {@code [from, to)}. */
    public List<PtSessionRow> findPtSessionsInRange(Long trainerId, OffsetDateTime from, OffsetDateTime to) {
        List<Object[]> rows = rows(native_("""
            SELECT p.id, p.member_id, m.member_code, m.full_name, p.membership_id, p.branch_id,
                   p.start_at, p.end_at, p.status, p.note
            FROM pt_sessions p
            JOIN members m ON m.id = p.member_id
            WHERE p.trainer_id = :trainerId AND p.start_at >= :from AND p.start_at < :to
            ORDER BY p.start_at, p.id""")
            .addScalar("id", StandardBasicTypes.LONG)
            .addScalar("member_id", StandardBasicTypes.LONG)
            .addScalar("member_code", StandardBasicTypes.STRING)
            .addScalar("full_name", StandardBasicTypes.STRING)
            .addScalar("membership_id", StandardBasicTypes.LONG)
            .addScalar("branch_id", StandardBasicTypes.LONG)
            .addScalar("start_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .addScalar("end_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .addScalar("status", StandardBasicTypes.STRING)
            .addScalar("note", StandardBasicTypes.STRING)
            .setParameter("trainerId", trainerId)
            .setParameter("from", from)
            .setParameter("to", to));
        return rows.stream().map(r -> new PtSessionRow(
            (Long) r[0], (Long) r[1], (String) r[2], (String) r[3], (Long) r[4], (Long) r[5],
            (OffsetDateTime) r[6], (OffsetDateTime) r[7], (String) r[8], (String) r[9])).toList();
    }

    /**
     * Buổi lớp nhóm được phân công cho huấn luyện viên trong khoảng nửa mở {@code [from, to)}.
     * Lọc theo {@code class_sessions.trainer_id} (không theo {@code class_schedules.trainer_id}) để phản ánh dạy thay.
     */
    public List<ClassSessionRow> findClassSessionsInRange(Long trainerId, OffsetDateTime from, OffsetDateTime to) {
        List<Object[]> rows = rows(native_("""
            SELECT cs.id, cs.class_schedule_id, cs.branch_id, sch.class_type_id, ct.name AS class_type_name,
                   cs.room_id, r.name AS room_name, cs.occurrence_date, cs.start_at, cs.end_at,
                   cs.original_start_at, cs.capacity, cs.status
            FROM class_sessions cs
            JOIN class_schedules sch ON sch.id = cs.class_schedule_id
            JOIN class_types ct ON ct.id = sch.class_type_id
            JOIN rooms r ON r.id = cs.room_id
            WHERE cs.trainer_id = :trainerId AND cs.start_at >= :from AND cs.start_at < :to
            ORDER BY cs.start_at, cs.id""")
            .addScalar("id", StandardBasicTypes.LONG)
            .addScalar("class_schedule_id", StandardBasicTypes.LONG)
            .addScalar("branch_id", StandardBasicTypes.LONG)
            .addScalar("class_type_id", StandardBasicTypes.LONG)
            .addScalar("class_type_name", StandardBasicTypes.STRING)
            .addScalar("room_id", StandardBasicTypes.LONG)
            .addScalar("room_name", StandardBasicTypes.STRING)
            .addScalar("occurrence_date", StandardBasicTypes.LOCAL_DATE)
            .addScalar("start_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .addScalar("end_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .addScalar("original_start_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .addScalar("capacity", StandardBasicTypes.INTEGER)
            .addScalar("status", StandardBasicTypes.STRING)
            .setParameter("trainerId", trainerId)
            .setParameter("from", from)
            .setParameter("to", to));
        return rows.stream().map(r -> new ClassSessionRow(
            (Long) r[0], (Long) r[1], (Long) r[2], (Long) r[3], (String) r[4], (Long) r[5], (String) r[6],
            (LocalDate) r[7], (OffsetDateTime) r[8], (OffsetDateTime) r[9], (OffsetDateTime) r[10],
            (Integer) r[11], (String) r[12])).toList();
    }

    /**
     * Hội viên có chỗ lớp nhóm {@code BOOKED} trong buổi chưa hủy giao với {@code [from, to)} không (BR-F9.3, lịch của
     * hội viên). Chỉ đọc (G4): luồng đặt chỗ lớp cũng khóa dòng {@code members} trước, nên giữ khóa đó là đủ.
     */
    public boolean hasMemberClassBookingOverlap(Long memberId, OffsetDateTime from, OffsetDateTime to) {
        Object found = entities.createNativeQuery("""
            SELECT EXISTS (
                SELECT 1 FROM class_bookings b JOIN class_sessions cs ON cs.id = b.class_session_id
                WHERE b.member_id = :memberId AND b.status = 'BOOKED' AND cs.status <> 'CANCELLED'
                  AND cs.end_at > :from AND cs.start_at < :to)""")
            .setParameter("memberId", memberId)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
        return Boolean.TRUE.equals(found);
    }

    @SuppressWarnings("unchecked")
    private NativeQuery<Object[]> native_(String sql) {
        return entities.createNativeQuery(sql).unwrap(NativeQuery.class);
    }

    private static List<Object[]> rows(NativeQuery<Object[]> query) {
        return query.getResultList();
    }
}
