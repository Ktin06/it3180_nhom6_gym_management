package vn.edu.hust.nhom6.gym.feature.invoice.dto;

/**
 * Thông tin thanh toán chuyển khoản VietQR cho hóa đơn đang chờ thanh toán (SRS F5 UC-F5.1 mục G).
 * Số tiền {@code total} luôn do server lấy từ {@code invoices.total} (BR-F5-01); nội dung chuyển khoản theo mẫu
 * {@code {branch_code} {invoice_code}} (BR-F5-02).
 */
public record VietQrPaymentDto(
        Long invoiceId,
        String invoiceCode,
        Integer total,
        String bankId,
        String bankName,
        String accountNo,
        String accountName,
        String transferContent,
        String qrImageUrl) {
}
