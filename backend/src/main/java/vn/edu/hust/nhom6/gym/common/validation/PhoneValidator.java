package vn.edu.hust.nhom6.gym.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;
import vn.edu.hust.nhom6.gym.common.util.PhoneUtils;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) { return true; } // @NotBlank xử lý trường bắt buộc.
        try {
            PhoneUtils.normalize(value);
            return true;
        } catch (InvalidRequestException ex) {
            return false;
        }
    }
}
