package vn.edu.hust.nhom6.gym.feature.trainer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vn.edu.hust.nhom6.gym.feature.trainer.entity.TrainerAvailability;

class TrainerAvailabilityTest {

    @Test
    @DisplayName("US-F9.1.1 E4: kiểm tra chồng khung nửa mở cùng thứ; chạm biên và khác thứ không chồng")
    void overlapsHalfOpenSameDay() {
        TrainerAvailability slot = TrainerAvailability.create(1L, 1L, 2, LocalTime.of(8, 0), LocalTime.of(12, 0));

        assertThat(slot.overlaps(2, LocalTime.of(11, 30), LocalTime.of(13, 0))).isTrue();
        assertThat(slot.overlaps(2, LocalTime.of(7, 0), LocalTime.of(8, 30))).isTrue();
        assertThat(slot.overlaps(2, LocalTime.of(9, 0), LocalTime.of(10, 0))).isTrue();
        assertThat(slot.overlaps(2, LocalTime.of(7, 0), LocalTime.of(13, 0))).isTrue();

        assertThat(slot.overlaps(2, LocalTime.of(12, 0), LocalTime.of(16, 0))).isFalse();
        assertThat(slot.overlaps(2, LocalTime.of(6, 0), LocalTime.of(8, 0))).isFalse();
        assertThat(slot.overlaps(3, LocalTime.of(8, 0), LocalTime.of(12, 0))).isFalse();
    }

    @Test
    @DisplayName("US-F9.1.1 BR-F9.15: buổi 06:30 thứ Ba giờ VN (23:30 thứ Hai UTC) thuộc khung thứ Ba, không thuộc thứ Hai")
    void vnTimezoneBoundaryAtEarlyMorning() {
        // Thứ Ba 2026-11-24 06:30-07:30 Asia/Ho_Chi_Minh == 2026-11-23T23:30:00Z .. 2026-11-24T00:30:00Z
        OffsetDateTime startUtc = OffsetDateTime.parse("2026-11-23T23:30:00Z");
        OffsetDateTime endUtc = OffsetDateTime.parse("2026-11-24T00:30:00Z");

        TrainerAvailability tuesdayMorning = TrainerAvailability.create(1L, 1L, 2, LocalTime.of(6, 0), LocalTime.of(8, 0));
        TrainerAvailability tuesdayNarrow = TrainerAvailability.create(1L, 1L, 2, LocalTime.of(7, 0), LocalTime.of(8, 0));
        TrainerAvailability mondayNight = TrainerAvailability.create(1L, 1L, 1, LocalTime.of(22, 0), LocalTime.of(23, 59));

        assertThat(tuesdayMorning.overlapsInVnTime(startUtc, endUtc)).isTrue();
        assertThat(tuesdayMorning.coversInVnTime(startUtc, endUtc)).isTrue();

        assertThat(tuesdayNarrow.overlapsInVnTime(startUtc, endUtc)).isTrue();
        assertThat(tuesdayNarrow.coversInVnTime(startUtc, endUtc)).isFalse();

        assertThat(mondayNight.overlapsInVnTime(startUtc, endUtc)).isFalse();
        assertThat(mondayNight.coversInVnTime(startUtc, endUtc)).isFalse();
    }

    @Test
    @DisplayName("US-F9.1.1 BR-F9.15: buổi chạm biên khung kề không bị coi là chồng vào khung liền kề")
    void adjacentSlotBoundaryDoesNotOverlap() {
        // Thứ Ba 2026-11-24 12:00-13:00 VN == 05:00-06:00 UTC
        OffsetDateTime startUtc = OffsetDateTime.parse("2026-11-24T05:00:00Z");
        OffsetDateTime endUtc = OffsetDateTime.parse("2026-11-24T06:00:00Z");

        TrainerAvailability morning = TrainerAvailability.create(1L, 1L, 2, LocalTime.of(8, 0), LocalTime.of(12, 0));
        TrainerAvailability afternoon = TrainerAvailability.create(1L, 1L, 2, LocalTime.of(12, 0), LocalTime.of(16, 0));

        assertThat(morning.overlapsInVnTime(startUtc, endUtc)).isFalse();
        assertThat(afternoon.overlapsInVnTime(startUtc, endUtc)).isTrue();
        assertThat(afternoon.coversInVnTime(startUtc, endUtc)).isTrue();
    }
}
