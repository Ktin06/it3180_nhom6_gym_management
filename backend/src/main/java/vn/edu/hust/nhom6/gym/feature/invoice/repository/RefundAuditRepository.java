package vn.edu.hust.nhom6.gym.feature.invoice.repository;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * Ghi dòng {@code audit_logs} khi quản lý hoàn tiền hóa đơn (BR-F5-12, S9). Chưa có service nhật ký chung (F17 thuộc
 * v3), nên F5 tự ghi bằng câu SQL có tham số, cùng mẫu {@code ClassRepository} (ghi nhật ký khóa và gỡ khóa đặt lớp).
 */
@Repository
@RequiredArgsConstructor
public class RefundAuditRepository {
    public static final String ACTION_REFUND = "REFUND";
    public static final String ENTITY_INVOICES = "invoices";

    private final EntityManager entities;

    public void insertRefund(Long userId, Long branchId, Long invoiceId, String oldValueJson, String newValueJson,
            String ip, OffsetDateTime at) {
        entities.createNativeQuery("""
            INSERT INTO audit_logs (user_id, branch_id, action, entity, entity_id, old_value, new_value, ip,
                                    created_at, updated_at)
            VALUES (:userId, :branchId, :action, :entity, :entityId, CAST(:oldValue AS jsonb),
                    CAST(:newValue AS jsonb), :ip, :at, :at)""")
            .setParameter("userId", userId)
            .setParameter("branchId", branchId)
            .setParameter("action", ACTION_REFUND)
            .setParameter("entity", ENTITY_INVOICES)
            .setParameter("entityId", invoiceId)
            .setParameter("oldValue", oldValueJson)
            .setParameter("newValue", newValueJson)
            .setParameter("ip", ip)
            .setParameter("at", at)
            .executeUpdate();
    }
}
