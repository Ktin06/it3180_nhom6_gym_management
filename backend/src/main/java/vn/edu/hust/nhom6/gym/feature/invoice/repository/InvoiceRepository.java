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

    // ---------------------------------------------------------------------------------------------
    // S2-09.3: tra cứu danh sách và thông tin hội viên cho chi tiết hóa đơn (G4, LEFT JOIN members)
    // ---------------------------------------------------------------------------------------------

    /** Thông tin tối thiểu của hội viên trên hóa đơn (chỉ đọc, không lọc deleted_at để giữ chứng từ lịch sử). */
    record MemberSnapshot(String memberCode, String fullName, String phone) { }

    @Query("""
        SELECT new vn.edu.hust.nhom6.gym.feature.invoice.repository.InvoiceRepository$MemberSnapshot(
            m.memberCode, m.fullName, m.phone)
        FROM Member m
        WHERE m.id = :memberId""")
    java.util.Optional<MemberSnapshot> findMemberSnapshot(
            @org.springframework.data.repository.query.Param("memberId") Long memberId);

    /**
     * Danh sách hóa đơn phân trang, mới nhất trước, {@code LEFT JOIN Member} để hóa đơn khách lẻ không bị mất.
     * Khoảng thời gian nửa mở {@code [fromInstant, toInstant)} tính theo giờ Việt Nam qua {@code VnTime}.
     * Dùng cờ boolean cho các tham số ngày và từ khóa để PostgreSQL luôn xác định được kiểu dữ liệu.
     */
    @Query(value = """
        SELECT new vn.edu.hust.nhom6.gym.feature.invoice.dto.InvoiceSummaryDto(
            i.id, i.code, i.branchId, i.memberId, m.memberCode, m.fullName, m.phone,
            i.total, i.source, i.status, i.paidAt, i.createdAt)
        FROM Invoice i
        LEFT JOIN Member m ON m.id = i.memberId
        WHERE (:branchId IS NULL OR i.branchId = :branchId)
          AND (:status IS NULL OR i.status = :status)
          AND (:source IS NULL OR i.source = :source)
          AND (:hasFrom = false OR i.createdAt >= :fromInstant)
          AND (:hasTo = false OR i.createdAt < :toInstant)
          AND (:hasKeyword = false
               OR LOWER(i.code) LIKE :keywordPattern ESCAPE '!'
               OR LOWER(m.fullName) LIKE :keywordPattern ESCAPE '!'
               OR m.phone LIKE :keywordPattern ESCAPE '!')
        ORDER BY i.createdAt DESC, i.id DESC""",
        countQuery = """
        SELECT count(i)
        FROM Invoice i
        LEFT JOIN Member m ON m.id = i.memberId
        WHERE (:branchId IS NULL OR i.branchId = :branchId)
          AND (:status IS NULL OR i.status = :status)
          AND (:source IS NULL OR i.source = :source)
          AND (:hasFrom = false OR i.createdAt >= :fromInstant)
          AND (:hasTo = false OR i.createdAt < :toInstant)
          AND (:hasKeyword = false
               OR LOWER(i.code) LIKE :keywordPattern ESCAPE '!'
               OR LOWER(m.fullName) LIKE :keywordPattern ESCAPE '!'
               OR m.phone LIKE :keywordPattern ESCAPE '!')""")
    org.springframework.data.domain.Page<vn.edu.hust.nhom6.gym.feature.invoice.dto.InvoiceSummaryDto> searchSummaries(
            @org.springframework.data.repository.query.Param("branchId") Long branchId,
            @org.springframework.data.repository.query.Param("status")
            vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus status,
            @org.springframework.data.repository.query.Param("source")
            vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource source,
            @org.springframework.data.repository.query.Param("hasFrom") boolean hasFrom,
            @org.springframework.data.repository.query.Param("fromInstant") java.time.OffsetDateTime fromInstant,
            @org.springframework.data.repository.query.Param("hasTo") boolean hasTo,
            @org.springframework.data.repository.query.Param("toInstant") java.time.OffsetDateTime toInstant,
            @org.springframework.data.repository.query.Param("hasKeyword") boolean hasKeyword,
            @org.springframework.data.repository.query.Param("keywordPattern") String keywordPattern,
            org.springframework.data.domain.Pageable pageable);
}
