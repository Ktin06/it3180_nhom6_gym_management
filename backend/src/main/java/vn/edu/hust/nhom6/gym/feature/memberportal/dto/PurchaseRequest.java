package vn.edu.hust.nhom6.gym.feature.memberportal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Hội viên gửi yêu cầu mua gói (UC-F7.4). Chỉ nhận ID gói; giá do server tính, hội viên lấy từ token. */
public record PurchaseRequest(
        @NotNull(message = "Vui lòng chọn gói tập.")
        @Positive(message = "ID gói phải lớn hơn 0.")
        Long packageId) { }
