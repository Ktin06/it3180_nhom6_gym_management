package vn.edu.hust.nhom6.gym.feature.invoice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceItemType;

/** Một dòng hóa đơn client gửi: chỉ loại dòng và ID gói, không có giá. */
public record InvoiceItemRequest(
        @NotNull(message = "Vui lòng chọn loại dòng hóa đơn.")
        InvoiceItemType itemType,

        @NotNull(message = "Vui lòng chọn gói tập.")
        @Positive(message = "ID gói phải lớn hơn 0.")
        Long packageId) {

    /** Sprint 2 chỉ bán gói tập; dòng PRODUCT (F12) và TRANSFER_FEE chưa mở. */
    @JsonIgnore
    @AssertTrue(message = "Hiện chỉ hỗ trợ dòng hóa đơn loại PACKAGE.")
    public boolean isPackageItem() {
        return itemType == null || itemType == InvoiceItemType.PACKAGE;
    }
}
