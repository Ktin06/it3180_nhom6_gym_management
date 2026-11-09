package vn.edu.hust.nhom6.gym.feature.auth.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthService;

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
    @MaxPasswordBytes
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

    /**
     * Ràng buộc kiểm tra độ dài mật khẩu theo số byte UTF-8 (tối đa 72 byte của BCrypt)
     * để thống nhất một mã lỗi (400) và một thông báo với {@link AuthService#validatePasswordLength(String)}.
     */
    @Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = MaxPasswordBytesValidator.class)
    public @interface MaxPasswordBytes {
        String message() default AuthService.PASSWORD_MAX_BYTES_MESSAGE;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    public static class MaxPasswordBytesValidator implements ConstraintValidator<MaxPasswordBytes, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null
                || value.getBytes(StandardCharsets.UTF_8).length <= AuthService.MAX_PASSWORD_BYTES;
        }
    }
}
