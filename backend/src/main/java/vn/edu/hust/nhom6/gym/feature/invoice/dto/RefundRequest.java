package vn.edu.hust.nhom6.gym.feature.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Hoàn tiền hóa đơn (UC-F5.5 bước 1). Không có số tiền: luôn hoàn toàn bộ {@code invoices.total} (S7).
 *
 * <p>Lý do được cắt khoảng trắng hai đầu ngay khi tạo bản ghi. Kiểm tra bằng Bean Validation nhưng gọi trong {@code RefundService} sau bước xét vai trò, để lễ tân gửi body
 * thiếu lý do vẫn nhận 403 (SRS bước 2 trước bước 3).
 */
public record RefundRequest(
        @NotBlank(message = "Vui lòng nhập lý do hoàn tiền.")
        @Size(max = 500, message = "Lý do hoàn tiền tối đa 500 ký tự.")
        String refundReason) {

    /** Cắt khoảng trắng hai đầu trước khi kiểm tra độ dài và lưu; chuỗi chỉ có khoảng trắng thành rỗng (400). */
    public RefundRequest {
        refundReason = refundReason == null ? null : refundReason.strip();
    }
}
