package vn.edu.hust.nhom6.gym.feature.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Yêu cầu tạo tài khoản đăng nhập cho hồ sơ hội viên có sẵn (F2, UC-F2.2). */
public record CreateMemberAccountRequest(
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    String email
) {
}
