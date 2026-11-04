package vn.edu.hust.nhom6.gym.feature.auth.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Yêu cầu đăng ký tài khoản hội viên qua {@code POST /api/v1/auth/register}. */
public record RegisterRequest(
    @NotBlank(message = "Họ và tên không được để trống")
    String fullName,

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    String email,

    @NotBlank(message = "Số điện thoại không được để trống")
    String phone,

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    String password,

    LocalDate dob,

    String gender
) {
}
