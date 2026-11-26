package vn.edu.hust.nhom6.gym.feature.trainer.dto;

import java.time.LocalDate;
import java.util.List;

import vn.edu.hust.nhom6.gym.feature.member.dto.BodyMetricDto;

/**
 * Dữ liệu tiến độ của một hội viên (UC-F9.6): dữ liệu thô sắp theo ngày tăng dần, không gom, không gợi ý (BR-F9.13).
 * {@code from}, {@code to} là ngày Việt Nam, tính cả hai đầu.
 */
public record ProgressDto(
        Long memberId,
        String memberCode,
        String fullName,
        LocalDate from,
        LocalDate to,
        List<BodyMetricDto> bodyMetrics,
        List<WorkoutLogDto> workoutLogs) {
}
