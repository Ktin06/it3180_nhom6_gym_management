package vn.edu.hust.nhom6.gym.feature.trainer.repository;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * Ghi dòng {@code audit_logs} khi huấn luyện viên xóa khung giờ rảnh (BR-F9.15, S9).
 * Chưa có service nhật ký chung (F17 thuộc v3), nên F9 tự ghi bằng câu SQL có tham số giống {@code GroupClassAuditRepository}.
 */
@Repository
@RequiredArgsConstructor
public class TrainerAuditRepository {
    public static final String ACTION_DELETE = "DELETE";
    public static final String ENTITY_AVAILABILITY = "trainer_availabilities";

    private final EntityManager entities;

    public static final String ACTION_CANCEL = "CANCEL";
    public static final String ENTITY_PT_SESSION = "pt_sessions";

    /**
     * Quản lý chi nhánh hủy hộ buổi PT (BR-F9.17). Lý do nằm trong {@code new_value} vì {@code pt_sessions} không có cột
     * lý do hủy (SRS UC-F9.7 mục F).
     */
    public void insertPtSessionCancelled(Long userId, Long branchId, Long sessionId, String oldValueJson,
            String newValueJson, OffsetDateTime at) {
        entities.createNativeQuery("""
            INSERT INTO audit_logs (user_id, branch_id, action, entity, entity_id, old_value, new_value,
                                    created_at, updated_at)
            VALUES (:userId, :branchId, :action, :entity, :entityId, CAST(:oldValue AS jsonb),
                    CAST(:newValue AS jsonb), :at, :at)""")
            .setParameter("userId", userId)
            .setParameter("branchId", branchId)
            .setParameter("action", ACTION_CANCEL)
            .setParameter("entity", ENTITY_PT_SESSION)
            .setParameter("entityId", sessionId)
            .setParameter("oldValue", oldValueJson)
            .setParameter("newValue", newValueJson)
            .setParameter("at", at)
            .executeUpdate();
    }

    public static final String ACTION_CONFIRM = "CONFIRM";
    public static final String ACTION_NO_SHOW = "NO_SHOW";

    /**
     * Quản lý chi nhánh xác nhận hoặc ghi vắng buổi PT thay huấn luyện viên (BR-F9.19, S2-19.3).
     *
     * @param action {@link #ACTION_CONFIRM} hoặc {@link #ACTION_NO_SHOW}
     */
    public void insertPtSessionCompleted(Long userId, Long branchId, Long sessionId, String action,
            String oldValueJson, String newValueJson, OffsetDateTime at) {
        entities.createNativeQuery("""
            INSERT INTO audit_logs (user_id, branch_id, action, entity, entity_id, old_value, new_value,
                                    created_at, updated_at)
            VALUES (:userId, :branchId, :action, :entity, :entityId, CAST(:oldValue AS jsonb),
                    CAST(:newValue AS jsonb), :at, :at)""")
            .setParameter("userId", userId)
            .setParameter("branchId", branchId)
            .setParameter("action", action)
            .setParameter("entity", ENTITY_PT_SESSION)
            .setParameter("entityId", sessionId)
            .setParameter("oldValue", oldValueJson)
            .setParameter("newValue", newValueJson)
            .setParameter("at", at)
            .executeUpdate();
    }

    public void insertAvailabilityDeleted(Long userId, Long branchId, Long availabilityId,
            String oldValueJson, OffsetDateTime at) {
        entities.createNativeQuery("""
            INSERT INTO audit_logs (user_id, branch_id, action, entity, entity_id, old_value, new_value,
                                    created_at, updated_at)
            VALUES (:userId, :branchId, :action, :entity, :entityId, CAST(:oldValue AS jsonb), NULL, :at, :at)""")
            .setParameter("userId", userId)
            .setParameter("branchId", branchId)
            .setParameter("action", ACTION_DELETE)
            .setParameter("entity", ENTITY_AVAILABILITY)
            .setParameter("entityId", availabilityId)
            .setParameter("oldValue", oldValueJson)
            .setParameter("at", at)
            .executeUpdate();
    }

    public static final String ACTION_CREATE = "CREATE";
    public static final String ENTITY_WORKOUT_LOG = "workout_logs";

    /**
     * PT ghi một dòng nhật ký tập (UC-F9.5, S2-20.1 GĐ-11). {@code workout_logs} không có cột người ghi, nên dấu vết PT
     * nào ghi nằm ở dòng này.
     */
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
