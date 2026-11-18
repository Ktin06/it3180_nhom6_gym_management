package vn.edu.hust.nhom6.gym.feature.groupclass.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Hợp đồng F8; chi nhánh, trạng thái và người tạo đều do server xác định. */
public final class ClassDtos {
    private ClassDtos() {}

    public record ScheduleRequest(
        @NotNull(message = "Trường này là bắt buộc.") @Positive(message = "Giá trị phải lớn hơn 0.") @JsonDeserialize(using = ClassJsonValues.LongValue.class) Long classTypeId, @NotNull(message = "Trường này là bắt buộc.") @Positive(message = "Giá trị phải lớn hơn 0.") @JsonDeserialize(using = ClassJsonValues.LongValue.class) Long trainerId,
        @NotNull(message = "Trường này là bắt buộc.") @Positive(message = "Giá trị phải lớn hơn 0.") @JsonDeserialize(using = ClassJsonValues.LongValue.class) Long roomId, @NotNull(message = "Trường này là bắt buộc.") @Min(value = 1, message = "Thứ phải từ 1 đến 7.") @Max(value = 7, message = "Thứ phải từ 1 đến 7.") @JsonDeserialize(using = ClassJsonValues.IntegerValue.class) Integer dayOfWeek,
        @NotNull(message = "Trường này là bắt buộc.") LocalTime startTime, @NotNull(message = "Trường này là bắt buộc.") LocalTime endTime,
        @NotNull(message = "Trường này là bắt buộc.") @Positive(message = "Giá trị phải lớn hơn 0.") @JsonDeserialize(using = ClassJsonValues.IntegerValue.class) Integer capacity, @NotNull(message = "Trường này là bắt buộc.") LocalDate validFrom, LocalDate validTo) {
        @JsonAnySetter
        public void rejectUnknown(String field, Object value) {
            throw new IllegalArgumentException("Request chứa trường không được phép.");
        }
    }

    public record BookingRequest(@NotNull(message = "Trường này là bắt buộc.") @Positive(message = "Giá trị phải lớn hơn 0.") @JsonDeserialize(using = ClassJsonValues.LongValue.class) Long memberId) {
        @JsonAnySetter
        public void rejectUnknown(String field, Object value) {
            throw new IllegalArgumentException("Request chứa trường không được phép.");
        }
    }

    public record Schedule(Long id, Long branchId, Long classTypeId, Long trainerId, Long roomId,
        Integer dayOfWeek, LocalTime startTime, LocalTime endTime, Integer capacity,
        LocalDate validFrom, LocalDate validTo, String status) {}

    public record Session(Long id, Long classScheduleId, Long branchId, Long trainerId, Long roomId,
        LocalDate occurrenceDate, OffsetDateTime startAt, OffsetDateTime endAt, Integer capacity,
        String status) {}

    public record Booking(Long id, Long classSessionId, Long memberId, Long createdBy, String status,
        OffsetDateTime waitlistedAt, OffsetDateTime bookedAt, OffsetDateTime promotedAt,
        OffsetDateTime graceUntil, OffsetDateTime cancelledAt, String cancelReason,
        Boolean isLateCancel, Long waitlistPosition, OffsetDateTime bookingLockedUntil) {}
}
