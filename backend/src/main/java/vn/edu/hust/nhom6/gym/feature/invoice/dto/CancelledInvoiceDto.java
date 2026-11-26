package vn.edu.hust.nhom6.gym.feature.invoice.dto;

import java.time.OffsetDateTime;

import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;

/** Kết quả hủy hóa đơn chờ, đúng JSON mẫu SRS F5 mục G của UC-F5.3. */
public record CancelledInvoiceDto(Long id, String code, InvoiceStatus status, OffsetDateTime updatedAt) { }
