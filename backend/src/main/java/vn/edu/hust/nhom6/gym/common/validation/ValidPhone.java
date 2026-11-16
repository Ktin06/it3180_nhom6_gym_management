package vn.edu.hust.nhom6.gym.common.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({FIELD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
public @interface ValidPhone {
    String message() default "Số điện thoại phải gồm 10 chữ số bắt đầu bằng số 0 sau chuẩn hóa";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
