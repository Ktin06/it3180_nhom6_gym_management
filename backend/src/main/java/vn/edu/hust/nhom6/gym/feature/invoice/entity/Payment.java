package vn.edu.hust.nhom6.gym.feature.invoice.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;

/**
 * Phiếu thu (bảng {@code payments}, F5). Một dòng mỗi lần nhân viên xác nhận đã nhận tiền (UC-F5.2).
 * Số tiền luôn bằng {@code invoices.total} do server tính (S7), không nhận từ client.
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    /** CHECK {@code amount >= 0} (V9_06): hóa đơn 0 đồng có phiếu thu 0 đồng. */
    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    /** Mã giao dịch ngân hàng, tùy chọn. */
    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    /** {@code users.id} nhân viên xác nhận thu tiền. */
    @Column(name = "received_by", nullable = false)
    private Long receivedBy;

    @Column(name = "paid_at", nullable = false)
    private OffsetDateTime paidAt;

    /** Phiếu thu đã xác nhận cho một hóa đơn vừa chuyển PAID. */
    public static Payment confirmed(Invoice invoice, PaymentMethod method, String transactionRef, Long receivedBy) {
        Payment p = new Payment();
        p.invoiceId = invoice.getId();
        p.method = method;
        p.amount = invoice.getTotal();
        p.status = PaymentStatus.CONFIRMED;
        p.transactionRef = transactionRef;
        p.receivedBy = receivedBy;
        p.paidAt = invoice.getPaidAt();
        return p;
    }

    /** Hoàn tiền hóa đơn (UC-F5.5 bước 5c): CONFIRMED → CANCELLED. Phiếu thu đã hủy thì giữ nguyên. */
    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
    }
}
