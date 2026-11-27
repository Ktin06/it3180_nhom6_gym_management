package vn.edu.hust.nhom6.gym.feature.trainer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body ghi một dòng nhật ký tập (UC-F9.5). {@code ptSessionId} tùy chọn: có thì phải là buổi {@code DONE} của đúng PT,
 * đúng hội viên, cùng ngày (service trả 422). Ngày sau hôm nay do service trả 400. Khoảng giá trị theo GĐ-6.
 */
public record WorkoutLogRequest(
        @Positive(message = "Mã buổi PT không hợp lệ.")
        Long ptSessionId,
        @NotNull(message = "Vui lòng nhập ngày tập.")
        LocalDate loggedAt,
        @NotBlank(message = "Vui lòng nhập tên bài tập.")
        @Size(max = 100, message = "Tên bài tập tối đa 100 ký tự.")
        String exercise,
        @Min(value = 1, message = "Số hiệp từ 1 đến 100.")
        @Max(value = 100, message = "Số hiệp từ 1 đến 100.")
        Integer sets,
        @Min(value = 1, message = "Số lần từ 1 đến 1000.")
        @Max(value = 1000, message = "Số lần từ 1 đến 1000.")
        Integer reps,
        @DecimalMin(value = "0", message = "Mức tạ từ 0 đến 9999,99 kg.")
        @DecimalMax(value = "9999.99", message = "Mức tạ từ 0 đến 9999,99 kg.")
        @Digits(integer = 4, fraction = 2, message = "Mức tạ có tối đa 2 chữ số thập phân.")
        BigDecimal weight) {
}
