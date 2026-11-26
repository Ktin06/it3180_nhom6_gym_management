package vn.edu.hust.nhom6.gym.feature.trainer.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Một giáo án trả về cho PT. {@code trainerId} là {@code staff.id}. */
public record WorkoutPlanDto(
        Long id,
        Long trainerId,
        String trainerName,
        Long memberId,
        String title,
        List<WorkoutPlanRequest.PlanSession> content,
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime createdAt) {
}
