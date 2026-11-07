package vn.edu.hust.nhom6.gym.feature.staff.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Chỉ đọc chi nhánh làm việc của nhân viên; vai trò vẫn do F1 xác thực. */
@Repository
@RequiredArgsConstructor
public class StaffLookupRepository {
    private final EntityManager entities;

    public Optional<Long> findActiveBranchIdByUserId(Long userId) {
        List<?> rows = entities.createNativeQuery("""
            SELECT branch_id FROM staff WHERE user_id = :userId AND status = 'ACTIVE'
            """, Long.class).setParameter("userId", userId).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of((Long) rows.getFirst());
    }
}
