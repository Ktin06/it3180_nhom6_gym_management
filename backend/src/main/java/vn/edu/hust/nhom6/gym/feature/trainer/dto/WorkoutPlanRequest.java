package vn.edu.hust.nhom6.gym.feature.trainer.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body soạn giáo án (UC-F9.4). Không nhận {@code trainerId}, {@code memberId}: PT lấy từ token, hội viên lấy từ đường
 * dẫn. Khoảng giá trị theo GĐ-6 của S2-20.1. Ngày kết thúc trước ngày bắt đầu do service trả 400.
 */
public record WorkoutPlanRequest(
        @NotBlank(message = "Vui lòng nhập tên giáo án.")
        @Size(max = 255, message = "Tên giáo án tối đa 255 ký tự.")
        String title,
        @NotEmpty(message = "Giáo án phải có ít nhất một buổi tập.")
        @Size(max = 100, message = "Giáo án tối đa 100 buổi tập.")
        List<@NotNull(message = "Buổi tập không được để trống.") @Valid PlanSession> content,
        LocalDate startDate,
        LocalDate endDate) {

    /** Một buổi trong giáo án: số thứ tự buổi và danh sách bài tập. */
    public record PlanSession(
            @NotNull(message = "Vui lòng nhập số thứ tự buổi.")
            @Min(value = 1, message = "Số thứ tự buổi từ 1 đến 365.")
            @Max(value = 365, message = "Số thứ tự buổi từ 1 đến 365.")
            Integer session,
            @NotEmpty(message = "Mỗi buổi phải có ít nhất một bài tập.")
            @Size(max = 50, message = "Mỗi buổi tối đa 50 bài tập.")
            List<@NotNull(message = "Bài tập không được để trống.") @Valid PlanExercise> exercises) {
    }

    /** Một bài tập: tên, số hiệp, số lần (hai số sau tùy chọn). */
    public record PlanExercise(
            @NotBlank(message = "Vui lòng nhập tên bài tập.")
            @Size(max = 100, message = "Tên bài tập tối đa 100 ký tự.")
            String name,
            @Min(value = 1, message = "Số hiệp từ 1 đến 100.")
            @Max(value = 100, message = "Số hiệp từ 1 đến 100.")
            Integer sets,
            @Min(value = 1, message = "Số lần từ 1 đến 1000.")
            @Max(value = 1000, message = "Số lần từ 1 đến 1000.")
            Integer reps) {
    }
}
