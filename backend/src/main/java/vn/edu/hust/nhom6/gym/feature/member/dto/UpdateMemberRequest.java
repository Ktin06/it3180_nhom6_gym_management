package vn.edu.hust.nhom6.gym.feature.member.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.*;
import vn.edu.hust.nhom6.gym.common.validation.ValidPhone;
import vn.edu.hust.nhom6.gym.feature.member.entity.Gender;
import vn.edu.hust.nhom6.gym.feature.member.entity.MemberStatus;

public record UpdateMemberRequest(
    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên không quá 255 ký tự") String fullName,
    @NotBlank(message = "Số điện thoại không được để trống") @ValidPhone String phone,
    Gender gender, LocalDate dob,
    @NotNull(message = "Trạng thái không được để trống") MemberStatus status
) { }
