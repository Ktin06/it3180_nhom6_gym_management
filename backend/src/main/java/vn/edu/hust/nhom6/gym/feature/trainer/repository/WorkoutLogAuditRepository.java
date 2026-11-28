package vn.edu.hust.nhom6.gym.feature.trainer.repository;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * Ghi dòng {@code audit_logs} khi huấn luyện viên ghi nhật ký tập (UC-F9.5, S2-20.1 GĐ-11).
 * {@code workout_logs} không có cột người ghi, nên dấu vết PT nào ghi nằm ở bảng này.
 */
@Repository
@RequiredArgsConstructor
public class WorkoutLogAuditRepository {
    public static final String ACTION_CREATE = "CREATE";
    public static final String ENTITY_WORKOUT_LOG = "workout_logs";

    private final EntityManager entities;

    public void insertWorkoutLogCreated(Long userId, Long branchId, Long workoutLogId, String newValueJson,
            OffsetDateTime at) {
        entities.createNativeQuery("""
            INSERT INTO audit_logs (user_id, branch_id, action, entity, entity_id, old_value, new_value,
                                    created_at, updated_at)
            VALUES (:userId, :branchId, :action, :entity, :entityId, NULL, CAST(:newValue AS jsonb), :at, :at)""")
            .setParameter("userId", userId)
            .setParameter("branchId", branchId)
            .setParameter("action", ACTION_CREATE)
            .setParameter("entity", ENTITY_WORKOUT_LOG)
            .setParameter("entityId", workoutLogId)
            .setParameter("newValue", newValueJson)
            .setParameter("at", at)
            .executeUpdate();
    }
}
