package vn.edu.hust.nhom6.gym.feature.member.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import vn.edu.hust.nhom6.gym.feature.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, Long id);
    Optional<Member> findByIdAndDeletedAtIsNull(Long id);
    Optional<Member> findByUserIdAndDeletedAtIsNull(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<Member> findForUpdate(Long id);

    /**
     * Như {@link #findForUpdate} nhưng không chờ ({@code FOR ... NOWAIT}, thời hạn khóa 0): dòng đang bị giao dịch khác
     * khóa thì PostgreSQL báo ngay SQLSTATE 55P03, Spring đổi thành {@code PessimisticLockingFailureException}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("SELECT m FROM Member m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<Member> findForUpdateNoWait(Long id);

    /** Chỉ đổi cột khóa đặt lớp (không ghi đè cột khác bằng entity đã nạp trước khóa). */
    @Modifying
    @Query("UPDATE Member m SET m.bookingLockedUntil = :until, m.updatedAt = :now WHERE m.id = :id AND m.deletedAt IS NULL")
    int updateBookingLockedUntil(Long id, OffsetDateTime until, OffsetDateTime now);

    @Query(value = "SELECT nextval('member_code_seq')", nativeQuery = true)
    long nextMemberCode();
}
