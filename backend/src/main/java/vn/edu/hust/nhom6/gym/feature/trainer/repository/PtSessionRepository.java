package vn.edu.hust.nhom6.gym.feature.trainer.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import vn.edu.hust.nhom6.gym.feature.trainer.entity.PtSession;

public interface PtSessionRepository extends JpaRepository<PtSession, Long> {

    /** {@code SELECT ... FOR UPDATE} một buổi (NF6) trước khi hủy, để hai lần hủy cùng lúc không cùng đọc BOOKED. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PtSession p WHERE p.id = :id")
    Optional<PtSession> findByIdForUpdate(Long id);

    /**
     * Buổi {@code BOOKED} của huấn luyện viên {@code trainerId} <b>hoặc</b> của hội viên {@code memberId} giao với
     * {@code [from, to)} (BR-F9.3). Buổi chạm biên không tính; buổi hủy, xong, vắng không chặn.
     */
    @Query("""
        SELECT p FROM PtSession p
        WHERE p.status = vn.edu.hust.nhom6.gym.feature.trainer.entity.PtSessionStatus.BOOKED
          AND (p.trainerId = :trainerId OR p.memberId = :memberId)
          AND p.endAt > :from AND p.startAt < :to
        ORDER BY p.startAt, p.id""")
    List<PtSession> findBookedOverlap(Long trainerId, Long memberId, OffsetDateTime from, OffsetDateTime to);

    /**
     * Tổng số buổi {@code BOOKED} đang giữ chỗ trên gói {@code membershipId} (mọi giờ, kể cả buổi đã qua giờ mà PT
     * chưa xác nhận; BR-F9.4, VD-23 lựa chọn D).
     */
    @Query("""
        SELECT COUNT(p) FROM PtSession p
        WHERE p.membershipId = :membershipId
          AND p.status = vn.edu.hust.nhom6.gym.feature.trainer.entity.PtSessionStatus.BOOKED""")
    long countBooked(Long membershipId);

    /**
     * Buổi {@code BOOKED} chưa diễn ra ({@code start_at > now}) của gói {@code membershipId}, khóa {@code FOR UPDATE},
     * sớm trước (S2-19.3, VD-21 GĐ-8: gói vừa hết buổi thì chuyển sang gói nối tiếp hoặc hủy).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM PtSession p
        WHERE p.membershipId = :membershipId
          AND p.status = vn.edu.hust.nhom6.gym.feature.trainer.entity.PtSessionStatus.BOOKED
          AND p.startAt > :now
        ORDER BY p.startAt, p.id""")
    List<PtSession> findUpcomingBookedForUpdate(Long membershipId, OffsetDateTime now);
}
