package vn.edu.hust.nhom6.gym.feature.member.repository;

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
     * Khóa dòng hội viên kể cả hồ sơ đã xóa mềm ({@code deletedAt IS NOT NULL}), phục vụ thu tiền hóa đơn chờ (F5)
     * để phân biệt hồ sơ ngừng hoạt động/xóa mềm (422) với bản ghi không tồn tại (404).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findForUpdateIncludingDeleted(Long id);

    /**
     * Như {@link #findForUpdate} nhưng không chờ ({@code FOR ... NOWAIT}, thời hạn khóa 0): dòng đang bị giao dịch khác
     * khóa thì PostgreSQL báo ngay SQLSTATE 55P03, Spring đổi thành {@code PessimisticLockingFailureException}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("SELECT m FROM Member m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<Member> findForUpdateNoWait(Long id);

    @Query(value = "SELECT nextval('member_code_seq')", nativeQuery = true)
    long nextMemberCode();
}
