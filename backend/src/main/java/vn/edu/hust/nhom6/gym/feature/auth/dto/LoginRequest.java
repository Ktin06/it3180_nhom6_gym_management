package vn.edu.hust.nhom6.gym.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Yêu cầu đăng nhập tài khoản qua {@code POST /api/v1/auth/login}. */
public record LoginRequest(
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    String email,

    @NotBlank(message = "Mật khẩu không được để trống")
    String password
) {
}
