package vn.edu.hust.nhom6.gym.feature.packagecatalog.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PackageSessionsValidator.class)
public @interface ValidPackageSessions {
    String message() default "Số buổi không phù hợp với loại gói.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
