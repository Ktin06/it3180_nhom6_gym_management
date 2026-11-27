package vn.edu.hust.nhom6.gym.feature.invoice.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;
import vn.edu.hust.nhom6.gym.feature.membership.entity.MembershipStatus;

/**
 * Kết quả hội viên gửi yêu cầu mua hoặc gia hạn trên cổng (UC-F7.4, UC-F7.5 bước 6): mã hóa đơn, tổng tiền và hạn tự
 * hủy {@code expiresAt} (UTC). {@code startDate}, {@code endDate} là ngày dự kiến, tính lại khi thu tiền (BR-F7.6).
 */
public record PortalInvoiceDto(Long invoiceId, String invoiceCode, Long membershipId, Long packageId,
        String packageName, int subtotal, int discount, int total, InvoiceStatus status,
        MembershipStatus membershipStatus, LocalDate startDate, LocalDate endDate, OffsetDateTime expiresAt) {

    public static PortalInvoiceDto of(PackageInvoiceDto sale, OffsetDateTime expiresAt) {
        return new PortalInvoiceDto(sale.invoiceId(), sale.invoiceCode(), sale.membershipId(), sale.packageId(),
            sale.packageName(), sale.subtotal(), sale.discount(), sale.total(), sale.status(),
            sale.membershipStatus(), sale.startDate(), sale.endDate(), expiresAt);
    }
}
