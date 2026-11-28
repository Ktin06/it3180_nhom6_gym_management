package vn.edu.hust.nhom6.gym.feature.memberportal.dto;

import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceItemType;

/** Một dòng hàng/gói trong hóa đơn của hội viên ({@code GET /api/v1/me/invoices}, UC-F7.3). */
public record MyInvoiceItemDto(
        Long id,
        InvoiceItemType itemType,
        Long packageId,
        String description,
        int quantity,
        int unitPrice,
        int amount,
        int discountAmount) { }
