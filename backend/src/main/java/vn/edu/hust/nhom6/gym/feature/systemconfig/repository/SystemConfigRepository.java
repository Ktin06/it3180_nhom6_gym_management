package vn.edu.hust.nhom6.gym.feature.systemconfig.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Chỉ đọc bảng system_configs (tham số có dấu * trong bản chốt phạm vi); không ghi, không thêm khóa. */
@Repository
@RequiredArgsConstructor
public class SystemConfigRepository {
    private final EntityManager entities;

    /** Giá trị riêng của một chi nhánh (dòng có {@code branch_id = branchId}). */
    public Optional<String> findBranchValue(Long branchId, String key) {
        return first(entities.createNativeQuery("""
            SELECT config_value FROM system_configs WHERE branch_id = :branchId AND config_key = :key
            """, String.class).setParameter("branchId", branchId).setParameter("key", key).getResultList());
    }

    /** Giá trị toàn chuỗi (dòng có {@code branch_id} rỗng). */
    public Optional<String> findChainValue(String key) {
        return first(entities.createNativeQuery("""
            SELECT config_value FROM system_configs WHERE branch_id IS NULL AND config_key = :key
            """, String.class).setParameter("key", key).getResultList());
    }

    private static Optional<String> first(List<?> rows) {
        return rows.isEmpty() ? Optional.empty() : Optional.of((String) rows.getFirst());
    }
}
