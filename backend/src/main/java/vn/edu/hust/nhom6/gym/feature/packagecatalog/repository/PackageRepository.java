package vn.edu.hust.nhom6.gym.feature.packagecatalog.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.GymPackage;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

public interface PackageRepository extends JpaRepository<GymPackage, Long> {
    /** Khóa dòng cho sửa/ngừng bán; service phân biệt ID không tồn tại với gói ngoài chi nhánh. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from GymPackage p where p.id = :id")
    Optional<GymPackage> findForUpdate(@Param("id") Long id);

    boolean existsByCode(String code);

    /** Cùng predicate cho dữ liệu và count; branchId chỉ do service xác định. */
    @Query("""
        select p from GymPackage p
        where (:allBranches = true or p.branchId = :branchId or p.branchId is null)
          and (:type is null or p.type = :type) and p.status = :status
        """)
    Page<GymPackage> search(@Param("allBranches") boolean allBranches,
        @Param("branchId") Long branchId, @Param("type") PackageType type,
        @Param("status") PackageStatus status, Pageable pageable);
}
