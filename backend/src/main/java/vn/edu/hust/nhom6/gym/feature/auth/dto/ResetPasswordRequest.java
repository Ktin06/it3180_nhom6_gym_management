package vn.edu.hust.nhom6.gym.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.edu.hust.nhom6.gym.feature.auth.dto.RegisterRequest.MaxPasswordBytes;

/** Yêu cầu đặt lại mật khẩu bằng token dùng một lần qua {@code POST /api/v1/auth/reset-password} (UC-F1.5). */
public record ResetPasswordRequest(
    @NotBlank(message = "Mã đặt lại mật khẩu không được để trống")
    String token,

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, message = "Mật khẩu mới phải có ít nhất 8 ký tự")
    @MaxPasswordBytes
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
        message = "Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm cả chữ cái và chữ số"
    )
    String newPassword
) {
}
