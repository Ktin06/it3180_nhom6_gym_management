package vn.edu.hust.nhom6.gym.feature.invoice.dto;

import java.time.OffsetDateTime;

import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;

/**
 * Dòng hóa đơn trên màn hình danh sách ({@code GET /api/v1/invoices}, SRS F5 UC-F5.3 mục G).
 * Giữ đủ các trường của SRS F5 và bổ sung {@code branchId}, {@code memberPhone}, {@code paidAt} cho chủ chuỗi và
 * giao diện S2-10.1. Hóa đơn khách lẻ có các trường {@code member*} rỗng ({@code null}).
 */
public record InvoiceSummaryDto(Long id, String code, Long branchId, Long memberId, String memberCode,
        String memberName, String memberPhone, Integer total, InvoiceSource source, InvoiceStatus status,
        OffsetDateTime paidAt, OffsetDateTime createdAt) { }
