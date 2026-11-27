package vn.edu.hust.nhom6.gym.feature.memberportal.dto;

import java.time.OffsetDateTime;
import java.util.List;

import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;

/**
 * Một hóa đơn của chính hội viên đang đăng nhập ({@code GET /api/v1/me/invoices}, UC-F7.3).
 *
 * <p>Số tiền ({@code subtotal}, {@code discount}, {@code total}) lấy từ dữ liệu do server tính (BR-F7.3, S7).
 * {@code expiresAt} chỉ có giá trị khi hóa đơn đang {@code PENDING} và {@code source = MEMBER_PORTAL} (BR-F7.4:
 * {@code created_at + PENDING_INVOICE_TTL_DAYS}); các trường hợp khác trả {@code null}.
 */
public record MyInvoiceDto(
        Long id,
        String code,
        Long branchId,
        String branchName,
        InvoiceSource source,
        int subtotal,
        int discount,
        int total,
        InvoiceStatus status,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        List<MyInvoiceItemDto> items) { }
