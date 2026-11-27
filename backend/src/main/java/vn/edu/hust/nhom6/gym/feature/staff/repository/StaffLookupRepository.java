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

    /** Họ tên trên dòng {@code staff} của tài khoản (kể cả nhân viên đã nghỉ, để hiện tên trên chứng từ cũ). */
    public Optional<String> findFullNameByUserId(Long userId) {
        List<?> rows = entities.createNativeQuery("""
            SELECT full_name FROM staff WHERE user_id = :userId
            """, String.class).setParameter("userId", userId).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of((String) rows.getFirst());
    }

    /** Cặp {@code (staff.id, staff.branch_id)} của nhân viên đang ACTIVE theo {@code users.id}. */
    public Optional<StaffIdentityRow> findActiveIdentityByUserId(Long userId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entities.createNativeQuery("""
            SELECT id, branch_id FROM staff WHERE user_id = :userId AND status = 'ACTIVE'
            """).setParameter("userId", userId).getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.getFirst();
        return Optional.of(new StaffIdentityRow(((Number) row[0]).longValue(), ((Number) row[1]).longValue()));
    }

    public record StaffIdentityRow(Long staffId, Long branchId) { }
}
