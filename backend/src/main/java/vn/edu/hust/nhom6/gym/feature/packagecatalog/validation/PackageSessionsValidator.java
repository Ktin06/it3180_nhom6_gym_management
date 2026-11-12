package vn.edu.hust.nhom6.gym.feature.packagecatalog.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.CreatePackageRequest;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

public class PackageSessionsValidator implements ConstraintValidator<ValidPackageSessions, CreatePackageRequest> {
    @Override
    public boolean isValid(CreatePackageRequest request, ConstraintValidatorContext context) {
        if (request == null || request.type() == null) { return true; }
        boolean valid = request.type() == PackageType.TIME ? request.sessionCount() == null
            : request.sessionCount() != null && request.sessionCount() > 0;
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(request.type() == PackageType.TIME
                ? "Gói TIME không có số buổi." : "Gói SESSION/PT bắt buộc có số buổi lớn hơn 0.")
                .addPropertyNode("sessionCount").addConstraintViolation();
        }
        return valid;
    }
}
