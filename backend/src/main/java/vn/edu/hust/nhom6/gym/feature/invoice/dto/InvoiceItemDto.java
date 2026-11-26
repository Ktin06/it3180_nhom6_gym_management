package vn.edu.hust.nhom6.gym.feature.invoice.dto;

import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceItem;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceItemType;

/** Một dòng chi tiết trong hóa đơn ({@code invoice_items}); tên và đơn giá là ảnh chụp tại thời điểm bán. */
public record InvoiceItemDto(Long id, InvoiceItemType itemType, Long packageId, Long productId, String description,
        int quantity, int unitPrice, int amount, int discountAmount) {

    public static InvoiceItemDto of(InvoiceItem item) {
        return new InvoiceItemDto(item.getId(), item.getItemType(), item.getPackageId(), item.getProductId(),
            item.getDescription(), item.getQuantity(), item.getUnitPrice(), item.getAmount(),
            item.getDiscountAmount());
    }
}
