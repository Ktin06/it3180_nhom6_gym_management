package vn.edu.hust.nhom6.gym.feature.packagecatalog.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.annotation.JsonDeserialize;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.validation.ValidPackageSessions;

@ValidPackageSessions
public record CreatePackageRequest(
        @NotBlank(message = "Mã gói không được để trống.") @Size(max = 30, message = "Mã gói tối đa 30 ký tự.")
        @JsonDeserialize(using = PackageJsonValues.StringValue.class) String code,
        @NotBlank(message = "Tên gói không được để trống.") @Size(max = 255, message = "Tên gói tối đa 255 ký tự.")
        @JsonDeserialize(using = PackageJsonValues.StringValue.class) String name,
        @NotNull(message = "Loại gói là bắt buộc.")
        @JsonDeserialize(using = PackageJsonValues.TypeValue.class) PackageType type,
        @NotNull(message = "Thời hạn là bắt buộc.") @Positive(message = "Thời hạn phải lớn hơn 0.")
        @JsonDeserialize(using = PackageJsonValues.IntegerValue.class) Integer durationDays,
        @JsonDeserialize(using = PackageJsonValues.IntegerValue.class) Integer sessionCount,
        @NotNull(message = "Giá là bắt buộc.") @Min(value = 0, message = "Giá phải lớn hơn hoặc bằng 0.")
        @JsonDeserialize(using = PackageJsonValues.IntegerValue.class) Integer price,
        @JsonDeserialize(using = PackageJsonValues.StringValue.class) String description) {

    /** Cấu hình cục bộ, không thay đổi cách đọc JSON của F1/F2. */
    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("Request chứa trường không được phép.");
    }
}
