package vn.edu.hust.nhom6.gym.feature.memberportal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Hội viên gửi yêu cầu gia hạn một gói của mình (UC-F7.5). */
public record RenewalRequest(
        @NotNull(message = "Vui lòng chọn gói cần gia hạn.")
        @Positive(message = "ID gói của hội viên phải lớn hơn 0.")
        Long membershipId) { }
