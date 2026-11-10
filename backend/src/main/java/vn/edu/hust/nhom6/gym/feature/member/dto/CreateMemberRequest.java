package vn.edu.hust.nhom6.gym.feature.member.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import vn.edu.hust.nhom6.gym.common.validation.ValidPhone;
import vn.edu.hust.nhom6.gym.feature.member.entity.Gender;

public record CreateMemberRequest(
    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên không quá 255 ký tự") String fullName,
    @NotBlank(message = "Số điện thoại không được để trống") @ValidPhone String phone,
    Gender gender,
    @Past(message = "Ngày sinh phải ở trong quá khứ") LocalDate dob
) { }
