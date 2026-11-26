package vn.edu.hust.nhom6.gym.feature.invoice.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;

/**
 * Hóa đơn (bảng {@code invoices}, F5). Map đủ mọi cột theo ERD, kể cả cột của thu tiền ({@code paid_at})
 * và hoàn tiền ({@code refunded_*}), để S2-09, S2-38 chỉ thêm hàm nghiệp vụ, không sửa phần ánh xạ.
 *
 * <p>Số tiền luôn tính ở server (S7): tổng được dựng từ các dòng, không nhận từ client.
 */
@Entity
@Table(name = "invoices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice extends BaseEntity {

    /** {@code HD} + 6 chữ số, sinh từ {@code invoice_code_seq} (V9_05). */
    @Column(nullable = false, length = 30, unique = true)
    private String code;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    /** Rỗng với khách lẻ (F12); bắt buộc khi có dòng PACKAGE. Báo cáo doanh thu phải LEFT JOIN members. */
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceSource source;

    /** F14 (v3); ở v1 luôn rỗng. */
    @Column(name = "promotion_id")
    private Long promotionId;

    @Column(nullable = false)
    private Integer subtotal;

    @Column(nullable = false)
    private Integer discount;

    /** {@code subtotal - discount} (CHECK {@code ck_invoices_amounts}). */
    @Column(nullable = false)
    private Integer total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "refunded_by")
    private Long refundedBy;

    @Column(name = "refunded_at")
    private OffsetDateTime refundedAt;

    @Column(name = "refund_reason", columnDefinition = "text")
    private String refundReason;

    @Version
    @Column(nullable = false)
    private Integer version;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<InvoiceItem> items = new ArrayList<>();

    /** Hóa đơn PENDING chưa có dòng; thêm dòng bằng {@link #addItem}. */
    public static Invoice pending(String code, Long branchId, Long memberId, Long createdBy, InvoiceSource source) {
        Invoice invoice = new Invoice();
        invoice.code = code;
        invoice.branchId = branchId;
        invoice.memberId = memberId;
        invoice.createdBy = createdBy;
        invoice.source = source;
        invoice.subtotal = 0;
        invoice.discount = 0;
        invoice.total = 0;
        invoice.status = InvoiceStatus.PENDING;
        return invoice;
    }

    /** Thêm dòng và tính lại tổng ở server. Chưa có khuyến mãi (F14) nên {@code discount = 0}. */
    public InvoiceItem addItem(InvoiceItem item) {
        item.attachTo(this);
        items.add(item);
        subtotal = Math.addExact(subtotal, item.getAmount());
        discount = Math.addExact(discount, item.getDiscountAmount());
        total = subtotal - discount;
        return item;
    }

    public List<InvoiceItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
