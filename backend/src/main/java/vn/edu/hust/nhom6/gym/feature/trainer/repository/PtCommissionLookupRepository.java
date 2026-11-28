package vn.edu.hust.nhom6.gym.feature.trainer.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * Truy vấn chỉ đọc nhiều bảng (G4) lấy số liệu tính hoa hồng buổi PT (BR-F9.8, ERD {@code pt_sessions.commission_amount}):
 * {@code staff.commission_rate}, {@code invoice_items.amount}, {@code discount_amount} của dòng hóa đơn đã bán gói,
 * và {@code memberships.total_sessions}. Không ghi bảng nào.
 */
@Repository
@RequiredArgsConstructor
public class PtCommissionLookupRepository {
    private final EntityManager entities;

    /**
     * Số liệu chụp lúc xác nhận.
     *
     * @param rate tỷ lệ hoa hồng của huấn luyện viên; rỗng nếu chưa khai báo
     * @param amount {@code invoice_items.amount}; rỗng nếu gói không có dòng hóa đơn
     * @param discountAmount {@code invoice_items.discount_amount}; rỗng nếu gói không có dòng hóa đơn
     * @param totalSessions {@code memberships.total_sessions}
     */
    public record CommissionInput(BigDecimal rate, Integer amount, Integer discountAmount, Integer totalSessions) { }

    public CommissionInput find(Long trainerId, Long membershipId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entities.createNativeQuery("""
            SELECT s.commission_rate, ii.amount, ii.discount_amount, m.total_sessions
            FROM memberships m
            JOIN staff s ON s.id = :trainerId
            LEFT JOIN invoice_items ii ON ii.id = m.invoice_item_id
            WHERE m.id = :membershipId""")
            .setParameter("trainerId", trainerId)
            .setParameter("membershipId", membershipId)
            .getResultList();
        if (rows.isEmpty()) {
            throw new IllegalStateException("Không có gói " + membershipId + " hoặc huấn luyện viên " + trainerId);
        }
        Object[] r = rows.getFirst();
        return new CommissionInput((BigDecimal) r[0], toInteger(r[1]), toInteger(r[2]), toInteger(r[3]));
    }

    private static Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }
}
