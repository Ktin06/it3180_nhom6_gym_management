package vn.edu.hust.nhom6.gym.feature.groupclass.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassBooking;
import vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassBookingStatus;
import vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassSession;

public interface ClassBookingRepository extends JpaRepository<ClassBooking, Long> {

    /** Phạm vi của một chỗ đặt, đọc trước khi khóa (không nạp entity, để sau khóa đọc được bản mới). */
    record BookingScope(Long id, Long memberId, Long sessionId, Long branchId, ClassBookingStatus status) { }

    @Query("""
        SELECT new vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassBookingRepository$BookingScope(
                   b.id, b.memberId, b.classSessionId, s.branchId, b.status)
        FROM ClassBooking b JOIN ClassSession s ON s.id = b.classSessionId
        WHERE b.id = :id""")
    Optional<BookingScope> findScope(Long id);

    /** Phạm vi của chỗ đặt khi điểm danh, đọc trước khi khóa (không nạp entity), để so trạng thái trước và sau khóa. */
    record AttendanceScope(Long id, Long memberId, Long sessionId, ClassBookingStatus status) { }

    @Query("""
        SELECT new vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassBookingRepository$AttendanceScope(
                   b.id, b.memberId, b.classSessionId, b.status)
        FROM ClassBooking b WHERE b.id IN :ids""")
    List<AttendanceScope> findAttendanceScopes(Collection<Long> ids);

    /**
     * Đặt lại {@code updated_at} của một chỗ đặt (sau khi sửa điểm danh đã flush). {@code updated_at} của chỗ NO_SHOW là
     * lúc ghi vắng mặt <b>lần đầu</b>, dùng để so với mốc khóa trong {@link #countViolations}; sửa điểm danh không được
     * làm vi phạm cũ trông như vi phạm mới (BR-F8.6-03, BR-F8.3-02).
     */
    @Modifying(flushAutomatically = true)
    @Query(nativeQuery = true, value = "UPDATE class_bookings SET updated_at = :at WHERE id = :id")
    int restoreUpdatedAt(Long id, OffsetDateTime at);

    /** {@code SELECT ... FOR UPDATE} một chỗ đặt (hủy chỗ: khóa sau {@code members}). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ClassBooking b WHERE b.id = :id")
    Optional<ClassBooking> findByIdForUpdate(Long id);

    /** Số chỗ đang chiếm buổi: BOOKED, ATTENDED, NO_SHOW (BR-F8.1-03, BR-F8.6-04). */
    @Query("""
        SELECT count(b) FROM ClassBooking b
        WHERE b.classSessionId = :sessionId
          AND b.status <> vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassBookingStatus.CANCELLED""")
    long countOccupied(Long sessionId);

    /** Hội viên đã có chỗ chưa hủy trong buổi này chưa (cùng điều kiện với {@code uq_active_booking}). */
    @Query("""
        SELECT count(b) > 0 FROM ClassBooking b
        WHERE b.classSessionId = :sessionId AND b.memberId = :memberId
          AND b.status <> vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassBookingStatus.CANCELLED""")
    boolean existsActive(Long sessionId, Long memberId);

    /**
     * Buổi chưa hủy mà hội viên đang giữ chỗ chưa hủy, có giờ giao với {@code [from, to)} (BR-F8.1-04). Buổi nối tiếp
     * (kết thúc đúng lúc buổi kia bắt đầu) không tính là trùng.
     */
    @Query("""
        SELECT s FROM ClassBooking b JOIN ClassSession s ON s.id = b.classSessionId
        WHERE b.memberId = :memberId AND s.id <> :excludeSessionId
          AND b.status <> vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassBookingStatus.CANCELLED
          AND s.status <> vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassSessionStatus.CANCELLED
          AND s.startAt < :to AND s.endAt > :from
        ORDER BY s.startAt""")
    List<ClassSession> findOverlapping(Long memberId, Long excludeSessionId, OffsetDateTime from, OffsetDateTime to);

    /**
     * Số vi phạm của hội viên (BR-F8.3-02), mọi biên đều mở:
     * <ul>
     *   <li>hủy muộn: {@code cancelled_at} trong {@code (monthAfter, before)} và sau mốc khóa {@code lockAfter};</li>
     *   <li>vắng mặt (NO_SHOW): giờ bắt đầu buổi trong {@code (monthAfter, before)} (tháng của buổi học) và lần ghi
     *       điểm danh ({@code updated_at}) sau mốc khóa {@code lockAfter}, để NO_SHOW ghi sau một lần khóa vẫn được
     *       tính cho lần khóa sau dù buổi bắt đầu trước mốc khóa.</li>
     * </ul>
     */
    @Query(nativeQuery = true, value = """
        SELECT count(*) FROM class_bookings b JOIN class_sessions s ON s.id = b.class_session_id
        WHERE b.member_id = :memberId
          AND ((b.status = 'CANCELLED' AND b.is_late_cancel AND b.cancelled_at > :monthAfter
                AND b.cancelled_at < :before AND b.cancelled_at > :lockAfter)
            OR (b.status = 'NO_SHOW' AND s.start_at > :monthAfter AND s.start_at < :before
                AND b.updated_at > :lockAfter))""")
    long countViolations(Long memberId, OffsetDateTime monthAfter, OffsetDateTime before, OffsetDateTime lockAfter);

    /**
     * {@code SELECT ... FOR UPDATE} các chỗ BOOKED của buổi, theo id tăng dần (hủy buổi, BR-F8.7-05). Gọi <b>sau</b> khi
     * đã khóa dòng {@code class_sessions}: đặt chỗ mới cũng phải khóa buổi trước, nên không chỗ BOOKED nào lọt sau lần
     * đọc này.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM ClassBooking b
        WHERE b.classSessionId = :sessionId
          AND b.status = vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassBookingStatus.BOOKED
        ORDER BY b.id""")
    List<ClassBooking> findBookedForUpdate(Long sessionId);

    /**
     * Id các chỗ BOOKED chưa có {@code arrived_at} của hội viên, ở buổi chưa hủy thuộc chi nhánh {@code branchId}, bắt
     * đầu trong {@code [from, to]} (cả hai đầu đều tính), tăng dần (check-in F4, UC-F4.3). Không khóa: nơi gọi khóa từng
     * chỗ bằng {@link #findByIdForUpdate} rồi kiểm lại.
     */
    @Query("""
        SELECT b.id FROM ClassBooking b JOIN ClassSession s ON s.id = b.classSessionId
        WHERE b.memberId = :memberId AND s.branchId = :branchId
          AND b.status = vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassBookingStatus.BOOKED
          AND b.arrivedAt IS NULL
          AND s.status <> vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassSessionStatus.CANCELLED
          AND s.startAt >= :from AND s.startAt <= :to
        ORDER BY b.id""")
    List<Long> findArrivableIds(Long memberId, Long branchId, OffsetDateTime from, OffsetDateTime to);

    /** {@code members.id} của các chỗ BOOKED trong buổi, tăng dần (người cần báo khi đổi giờ, đổi phòng). */
    @Query("""
        SELECT b.memberId FROM ClassBooking b
        WHERE b.classSessionId = :sessionId
          AND b.status = vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassBookingStatus.BOOKED
        ORDER BY b.memberId""")
    List<Long> findBookedMemberIds(Long sessionId);
}
