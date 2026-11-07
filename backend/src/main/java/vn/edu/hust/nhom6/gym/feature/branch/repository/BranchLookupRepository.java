package vn.edu.hust.nhom6.gym.feature.branch.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Chỉ đọc bảng branches; không tạo hoặc cập nhật chi nhánh. */
@Repository
@RequiredArgsConstructor
public class BranchLookupRepository {
    private final EntityManager entities;

    public Optional<Long> findActiveIdByCode(String code) {
        List<?> rows = entities.createNativeQuery("""
            SELECT id FROM branches WHERE code = :code AND status = 'ACTIVE'
            """, Long.class).setParameter("code", code).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of((Long) rows.getFirst());
    }

    public boolean existsActiveById(Long branchId) {
        return !entities.createNativeQuery("""
            SELECT id FROM branches WHERE id = :id AND status = 'ACTIVE'
            """, Long.class).setParameter("id", branchId).getResultList().isEmpty();
    }
}
