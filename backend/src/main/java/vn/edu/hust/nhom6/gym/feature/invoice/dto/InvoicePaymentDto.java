package vn.edu.hust.nhom6.gym.feature.invoice.dto;

import java.time.OffsetDateTime;

import vn.edu.hust.nhom6.gym.feature.invoice.entity.Payment;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.PaymentMethod;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.PaymentStatus;

/** Phiếu thu gắn với hóa đơn ({@code payments}), trả kèm trong chi tiết hóa đơn. */
public record InvoicePaymentDto(Long id, PaymentMethod method, int amount, PaymentStatus status, String transactionRef,
        Long receivedBy, String receivedByName, OffsetDateTime paidAt) {

    public static InvoicePaymentDto of(Payment payment, String receivedByName) {
        return new InvoicePaymentDto(payment.getId(), payment.getMethod(), payment.getAmount(), payment.getStatus(),
            payment.getTransactionRef(), payment.getReceivedBy(), receivedByName, payment.getPaidAt());
    }
}
