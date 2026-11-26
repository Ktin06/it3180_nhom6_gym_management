package vn.edu.hust.nhom6.gym.feature.checkin.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import vn.edu.hust.nhom6.gym.feature.checkin.entity.Checkin;

public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    /** Id các gói của hội viên đã có lượt trừ buổi trong ngày {@code date} (BR-F4.3). */
    @Query("""
        SELECT DISTINCT c.membershipId FROM Checkin c
        WHERE c.memberId = :memberId AND c.checkinDate = :date AND c.sessionDeducted = true""")
    List<Long> findDeductedMembershipIds(Long memberId, LocalDate date);

    /** Hội viên đang có mặt ở chi nhánh: có lượt hôm nay chưa check-out. */
    @Query("""
        SELECT count(c) > 0 FROM Checkin c
        WHERE c.memberId = :memberId AND c.branchId = :branchId AND c.checkinDate = :date
          AND c.checkedOutAt IS NULL""")
    boolean existsOpen(Long memberId, Long branchId, LocalDate date);

    /** Khóa dòng lượt check-in đúng chi nhánh trước khi đọc trạng thái check-out (UC-F4.4, NF6). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Checkin c WHERE c.id = :id AND c.branchId = :branchId")
    Optional<Checkin> findByIdAndBranchIdForUpdate(@Param("id") Long id, @Param("branchId") Long branchId);
}
