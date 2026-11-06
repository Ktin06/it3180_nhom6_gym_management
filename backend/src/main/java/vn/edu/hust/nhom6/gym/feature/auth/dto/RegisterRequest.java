package vn.edu.hust.nhom6.gym.feature.auth.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Yêu cầu đăng ký tài khoản hội viên qua {@code POST /api/v1/auth/register}. */
public record RegisterRequest(
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
    String fullName,

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    String email,

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
        regexp = "^(\\+?84|0)[\\s\\-.]?(\\d[\\s\\-.]?){9}$",
        message = "Số điện thoại không hợp lệ. Phải gồm 10 chữ số bắt đầu bằng số 0"
    )
    String phone,

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
        message = "Mật khẩu phải có ít nhất 8 ký tự, bao gồm cả chữ cái và chữ số"
    )
    String password,

    LocalDate dob,

    @Pattern(
        regexp = "^(MALE|FEMALE|OTHER)$",
        message = "Giới tính chỉ nhận MALE, FEMALE hoặc OTHER"
    )
    String gender
) {
}
