package vn.edu.hust.nhom6.gym.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Yêu cầu gửi liên kết đặt lại mật khẩu qua {@code POST /api/v1/auth/forgot-password} (UC-F1.5). */
public record ForgotPasswordRequest(
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    String email
) {
}
