package vn.edu.hust.nhom6.gym.feature.member.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Một lần đo chỉ số cơ thể. {@code recordedBy} là {@code users.id} người ghi. */
public record BodyMetricDto(
        Long id,
        Long memberId,
        LocalDate measuredAt,
        BigDecimal weight,
        BigDecimal height,
        BigDecimal bodyFat,
        BigDecimal muscleMass,
        String note,
        Long recordedBy,
        OffsetDateTime createdAt) {
}
