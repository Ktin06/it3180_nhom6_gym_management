package vn.edu.hust.nhom6.gym.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.util.PhoneUtils;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) { return true; } // @NotBlank xử lý trường bắt buộc.
        try {
            PhoneUtils.normalizePhone(value);
            return true;
        } catch (BusinessRuleException ex) {
            return false;
        }
    }
}
