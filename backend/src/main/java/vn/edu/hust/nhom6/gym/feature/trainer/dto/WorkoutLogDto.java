package vn.edu.hust.nhom6.gym.feature.trainer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Một dòng nhật ký tập. */
public record WorkoutLogDto(
        Long id,
        Long memberId,
        Long ptSessionId,
        LocalDate loggedAt,
        String exercise,
        Integer sets,
        Integer reps,
        BigDecimal weight,
        OffsetDateTime createdAt) {
}
