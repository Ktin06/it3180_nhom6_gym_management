package vn.edu.hust.nhom6.gym.feature.invoice.dto;

import java.time.OffsetDateTime;
import java.util.List;

import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;

/**
 * Chi tiết một hóa đơn ({@code GET /api/v1/invoices/{id}}, UC-F5.3), gồm thông tin hóa đơn, thông tin hội viên
 * (rỗng với khách lẻ), danh sách dòng hóa đơn ({@code items}) và danh sách phiếu thu ({@code payments}).
 */
public record InvoiceDetailDto(Long id, String code, Long branchId, Long memberId, String memberCode,
        String memberName, String memberPhone, Long createdBy, InvoiceSource source, int subtotal, int discount,
        int total, InvoiceStatus status, OffsetDateTime paidAt, Long refundedBy, OffsetDateTime refundedAt,
        String refundReason, OffsetDateTime createdAt, OffsetDateTime updatedAt, List<InvoiceItemDto> items,
        List<InvoicePaymentDto> payments) { }
