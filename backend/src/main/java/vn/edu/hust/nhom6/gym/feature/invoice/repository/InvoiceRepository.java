package vn.edu.hust.nhom6.gym.feature.invoice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import vn.edu.hust.nhom6.gym.feature.invoice.entity.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /** Số tiếp theo của mã hóa đơn (V9_04). Sequence không quay lui khi giao dịch rollback nên mã có thể nhảy số. */
    @Query(value = "SELECT nextval('invoice_code_seq')", nativeQuery = true)
    long nextInvoiceCode();

    // S2-14.1: yêu cầu mua từ cổng hội viên, tác vụ hủy hóa đơn chờ quá hạn.
    // Viết tên lớp đầy đủ thay vì thêm import, để không chạm khối import mà S2-09.3 cùng đợt đang sửa.

    /** Hội viên đã có hóa đơn PENDING (mọi nguồn) chứa dòng gói {@code packageId} chưa (UC-F7.4 E4, UC-F7.5 E4). */
    @Query("""
        SELECT count(i) > 0 FROM Invoice i JOIN i.items it
        WHERE i.memberId = :memberId AND i.status = 'PENDING'
          AND it.packageId = :packageId""")
    boolean existsPendingWithPackage(@org.springframework.data.repository.query.Param("memberId") Long memberId,
            @org.springframework.data.repository.query.Param("packageId") Long packageId);

    /**
     * Hóa đơn PENDING từ cổng hội viên tạo trước {@code createdBefore}, cũ trước (BR-F7.5). Dùng chỉ mục có điều kiện
     * {@code ix_invoices_pending_portal}. Hạn theo chi nhánh do service lọc tiếp; ở đây chỉ chặn trên.
     */
    @Query("""
        SELECT i FROM Invoice i
        WHERE i.status = 'PENDING'
          AND i.source = 'MEMBER_PORTAL'
          AND i.createdAt < :createdBefore
        ORDER BY i.createdAt, i.id""")
    java.util.List<Invoice> findPendingPortalCreatedBefore(
            @org.springframework.data.repository.query.Param("createdBefore") java.time.OffsetDateTime createdBefore);
}
