package vn.edu.hust.nhom6.gym.feature.invoice.dto;

import java.time.OffsetDateTime;

import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;

/**
 * Kết quả hoàn tiền (SRS F5 UC-F5.5 mục G).
 *
 * @param refundedBy {@code users.id} quản lý duyệt hoàn tiền
 * @param refundedByName họ tên quản lý ({@code staff.full_name}); rỗng nếu không có hồ sơ nhân viên
 * @param refundReason lý do đã cắt khoảng trắng hai đầu, đúng như lưu trong {@code invoices.refund_reason}
 */
public record RefundResultDto(
        Long invoiceId,
        String invoiceCode,
        InvoiceStatus status,
        OffsetDateTime refundedAt,
        Long refundedBy,
        String refundedByName,
        String refundReason) {
}
