package vn.edu.hust.nhom6.gym.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.edu.hust.nhom6.gym.feature.auth.dto.RegisterRequest.MaxPasswordBytes;

/** Yêu cầu đổi mật khẩu qua {@code POST /api/v1/auth/change-password} (UC-F1.4). */
public record ChangePasswordRequest(
    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    String currentPassword,

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
