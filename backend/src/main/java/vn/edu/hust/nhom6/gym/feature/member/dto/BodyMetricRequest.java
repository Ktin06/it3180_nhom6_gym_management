package vn.edu.hust.nhom6.gym.feature.member.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body ghi một lần đo chỉ số cơ thể (UC-F2.5). Không nhận {@code recordedBy} (lấy từ token). Phải có ít nhất một chỉ
 * số và ngày đo không sau hôm nay (service trả 400). Khoảng giá trị theo GĐ-6 của S2-20.1, khớp CHECK của V3_03.
 */
public record BodyMetricRequest(
        @NotNull(message = "Vui lòng nhập ngày đo.")
        LocalDate measuredAt,
        @DecimalMin(value = "0", inclusive = false, message = "Cân nặng phải lớn hơn 0 và tối đa 500 kg.")
        @DecimalMax(value = "500", message = "Cân nặng phải lớn hơn 0 và tối đa 500 kg.")
        @Digits(integer = 3, fraction = 2, message = "Cân nặng có tối đa 2 chữ số thập phân.")
        BigDecimal weight,
        @DecimalMin(value = "0", inclusive = false, message = "Chiều cao phải lớn hơn 0 và tối đa 300 cm.")
        @DecimalMax(value = "300", message = "Chiều cao phải lớn hơn 0 và tối đa 300 cm.")
        @Digits(integer = 3, fraction = 2, message = "Chiều cao có tối đa 2 chữ số thập phân.")
        BigDecimal height,
        @DecimalMin(value = "0", message = "Tỷ lệ mỡ từ 0 đến 99,99%.")
        @DecimalMax(value = "99.99", message = "Tỷ lệ mỡ từ 0 đến 99,99%.")
        @Digits(integer = 2, fraction = 2, message = "Tỷ lệ mỡ có tối đa 2 chữ số thập phân.")
        BigDecimal bodyFat,
        @DecimalMin(value = "0", inclusive = false, message = "Khối lượng cơ phải lớn hơn 0 và tối đa 300 kg.")
        @DecimalMax(value = "300", message = "Khối lượng cơ phải lớn hơn 0 và tối đa 300 kg.")
        @Digits(integer = 3, fraction = 2, message = "Khối lượng cơ có tối đa 2 chữ số thập phân.")
        BigDecimal muscleMass,
        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự.")
        String note) {

    /** Có ít nhất một trong bốn chỉ số. */
    public boolean hasAnyMetric() {
        return weight != null || height != null || bodyFat != null || muscleMass != null;
    }
}
