package vn.edu.hust.nhom6.gym.feature.invoice.dto;

import java.time.LocalDate;

import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;
import vn.edu.hust.nhom6.gym.feature.membership.entity.MembershipStatus;

/**
 * Kết quả lập hóa đơn bán gói: đúng JSON mẫu SRS F3 mục G, thêm {@code invoiceCode} (trả lời câu hỏi S2-08.1).
 * {@code startDate}, {@code endDate} là ngày <b>dự kiến</b>; S2-09 tính lại khi thu tiền (BR-F3-12).
 */
public record PackageInvoiceDto(Long invoiceId, String invoiceCode, Long membershipId, Long memberId,
        Long packageId, String packageName, int subtotal, int discount, int total, InvoiceStatus status,
        MembershipStatus membershipStatus, LocalDate startDate, LocalDate endDate) { }
