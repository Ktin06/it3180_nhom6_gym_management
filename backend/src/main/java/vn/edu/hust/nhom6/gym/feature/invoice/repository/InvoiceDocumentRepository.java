package vn.edu.hust.nhom6.gym.feature.invoice.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Truy vấn chỉ đọc thông tin chi nhánh phục vụ sinh nội dung VietQR và in hóa đơn PDF (G4: đặt trong gói chức năng
 * hóa đơn để không sửa repository của chức năng khác).
 */
@Repository
@RequiredArgsConstructor
public class InvoiceDocumentRepository {
    private final EntityManager entities;

    /** Thông tin chi nhánh dùng cho nội dung VietQR ({@code code}) và tiêu đề hóa đơn PDF. */
    public record BranchSnapshot(String code, String name, String address, String phone) {
    }

    public Optional<BranchSnapshot> findBranchSnapshot(Long branchId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entities.createNativeQuery("""
            SELECT code, name, address, phone
            FROM branches
            WHERE id = :branchId
            """)
            .setParameter("branchId", branchId)
            .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.getFirst();
        return Optional.of(new BranchSnapshot(
            (String) row[0],
            (String) row[1],
            (String) row[2],
            (String) row[3]));
    }
}
