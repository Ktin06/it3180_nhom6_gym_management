package vn.edu.hust.nhom6.gym.feature.invoice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;

/** Dòng hóa đơn (bảng {@code invoice_items}). Giá chép tại thời điểm bán, không đổi khi gói sửa giá (BR-F3-13). */
@Entity
@Table(name = "invoice_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoiceItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private InvoiceItemType itemType;

    @Column(name = "package_id")
    private Long packageId;

    /** F12 (v3). */
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    /** {@code quantity * unit_price} (CHECK {@code ck_invoice_items_amount}). */
    @Column(nullable = false)
    private Integer amount;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    /** Dòng bán một gói tập: số lượng 1, giá và tên chép từ {@code packages} (S7, BR-F3-13). */
    public static InvoiceItem forPackage(Long packageId, String packageName, int price) {
        InvoiceItem item = new InvoiceItem();
        item.itemType = InvoiceItemType.PACKAGE;
        item.packageId = packageId;
        item.description = packageName;
        item.quantity = 1;
        item.unitPrice = price;
        item.amount = price;
        item.discountAmount = 0;
        return item;
    }

    void attachTo(Invoice owner) {
        this.invoice = owner;
    }
}
