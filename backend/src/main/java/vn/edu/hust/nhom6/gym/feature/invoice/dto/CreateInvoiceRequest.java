package vn.edu.hust.nhom6.gym.feature.invoice.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Lập hóa đơn bán gói tại quầy (UC-F3.5). Chỉ nhận ID; giá, tổng tiền do server tính (S7). Trường lạ như
 * {@code price}, {@code total} client gửi kèm bị bỏ qua, không dùng (AC-F3.5.1-01).
 *
 * <p>Sprint 2 mỗi hóa đơn đúng một dòng gói tập; dòng sản phẩm là F12 (v3).
 */
public record CreateInvoiceRequest(
        @NotNull(message = "Vui lòng chọn hội viên.")
        @Positive(message = "ID hội viên phải lớn hơn 0.")
        Long memberId,

        @NotNull(message = "Vui lòng chọn gói tập.")
        @Size(min = 1, max = 1, message = "Mỗi hóa đơn bán đúng một gói tập.")
        List<@NotNull(message = "Dòng hóa đơn không được rỗng.") @Valid InvoiceItemRequest> items) { }
