package vn.edu.hust.nhom6.gym.feature.invoice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.PaymentMethod;

/**
 * Xác nhận đã nhận tiền (UC-F5.2 bước 1). Không có số tiền: số tiền là {@code invoices.total} (S7).
 *
 * @param transactionRef mã giao dịch ngân hàng, tùy chọn; chuỗi trắng coi như không nhập
 */
public record PayInvoiceRequest(
        @NotNull(message = "Vui lòng chọn hình thức thanh toán (CASH hoặc BANK_TRANSFER).")
        PaymentMethod method,

        @Size(max = 100, message = "Mã giao dịch tối đa 100 ký tự.")
        String transactionRef) {

    /** {@code transactionRef} đã cắt khoảng trắng; rỗng thì {@code null}. */
    public String normalizedTransactionRef() {
        return transactionRef == null || transactionRef.isBlank() ? null : transactionRef.strip();
    }
}
