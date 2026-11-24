package vn.edu.hust.nhom6.gym.feature.membership.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.edu.hust.nhom6.gym.feature.membership.entity.Membership;
import vn.edu.hust.nhom6.gym.feature.membership.entity.MembershipStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Truy vấn gói hội viên. "Cùng chi nhánh" của gói luôn so bằng {@code IS NOT DISTINCT FROM}: gói toàn chuỗi có
 * {@code branch_id} rỗng, và {@code NULL = NULL} trong SQL là không đúng. JPQL không có toán tử đó nên viết
 * {@code ((:branchId IS NULL AND m.branchId IS NULL) OR m.branchId = :branchId)}.
 */
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    /** Dòng đọc cho danh sách gói của hội viên (kèm tên gói từ danh mục). */
    record HeldRow(Membership membership, String packageName) { }

    /** Gói cùng hội viên, cùng loại, cùng phạm vi chi nhánh, ở một trong các trạng thái cho trước. */
    @Query("""
        SELECT m FROM Membership m
        WHERE m.memberId = :memberId AND m.packageType = :type
          AND ((:branchId IS NULL AND m.branchId IS NULL) OR m.branchId = :branchId)
          AND m.status IN :statuses
        ORDER BY m.endDate DESC, m.id DESC
        """)
    List<Membership> findSameType(@Param("memberId") Long memberId, @Param("type") PackageType type,
        @Param("branchId") Long branchId, @Param("statuses") Collection<MembershipStatus> statuses);

    /**
     * Gói ACTIVE dùng được tại chi nhánh {@code branchId} vào ngày {@code date}: gói của chính chi nhánh đó
     * hoặc gói toàn chuỗi. Gói SESSION 0 buổi vẫn ACTIVE đến hết ngày nên vẫn có trong kết quả.
     */
    @Query("""
        SELECT m FROM Membership m
        WHERE m.memberId = :memberId AND m.packageType = :type AND m.status = 'ACTIVE'
          AND (m.branchId IS NULL OR m.branchId = :branchId)
          AND m.startDate <= :date AND m.endDate >= :date
        ORDER BY m.endDate, m.id
        """)
    List<Membership> findUsable(@Param("memberId") Long memberId, @Param("branchId") Long branchId,
        @Param("type") PackageType type, @Param("date") LocalDate date);

    /** Truy vấn chỉ đọc nhiều bảng (G4): gói hội viên đang giữ kèm tên gói. */
    @Query("""
        SELECT new vn.edu.hust.nhom6.gym.feature.membership.repository.MembershipRepository$HeldRow(m, p.name)
        FROM Membership m JOIN GymPackage p ON p.id = m.packageId
        WHERE m.memberId = :memberId AND m.status IN :statuses
        ORDER BY m.packageType, m.startDate, m.id
        """)
    List<HeldRow> findHeldWithPackageName(@Param("memberId") Long memberId,
        @Param("statuses") Collection<MembershipStatus> statuses);

    Optional<Membership> findByInvoiceItemId(Long invoiceItemId);

    /**
     * Đọc bảng checkins đã có từ Sprint 1, không phụ thuộc entity/API F4 chưa gộp.
     * Ngày nghiệp vụ do VnTime truyền vào; SESSION hết buổi hôm nay vẫn dùng được trong ngày.
     */
    @Query(value = """
        SELECT m.* FROM memberships m
        WHERE m.status = 'ACTIVE'
          AND (m.end_date < :today
               OR (m.package_type = 'SESSION' AND m.remaining_sessions = 0
                   AND NOT EXISTS (SELECT 1 FROM checkins c
                                   WHERE c.membership_id = m.id AND c.checkin_date = :today
                                     AND c.session_deducted = true)))
        ORDER BY m.id
        """, nativeQuery = true)
    List<Membership> findDueToExpire(@Param("today") LocalDate today);

    @Query("""
        SELECT m FROM Membership m
        WHERE m.status = 'UPCOMING' AND m.startDate <= :today
        ORDER BY m.memberId, m.startDate, m.id
        """)
    List<Membership> findDueUpcoming(@Param("today") LocalDate today);
}
