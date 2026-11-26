package vn.edu.hust.nhom6.gym.feature.invoice.dto;

import java.time.OffsetDateTime;
import java.util.List;

import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.PaymentMethod;

/**
 * Kết quả xác nhận thu tiền, đúng JSON mẫu SRS F5 mục G của UC-F5.2. {@code status} là trạng thái hóa đơn (PAID).
 * Hóa đơn 0 đồng cũng có phiếu thu ({@code amount = 0}, V9_06).
 */
public record PaymentConfirmationDto(Long paymentId, Long invoiceId, String invoiceCode, PaymentMethod method,
        int amount, InvoiceStatus status, OffsetDateTime paidAt, Long receivedBy, String receivedByName,
        List<ActivatedMembershipDto> activatedMemberships) { }
