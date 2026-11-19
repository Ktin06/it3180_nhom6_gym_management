package vn.edu.hust.nhom6.gym.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import vn.edu.hust.nhom6.gym.config.TimeConfig;

/** Kiểm thử đơn vị giờ Việt Nam; không phụ thuộc múi giờ của máy chạy kiểm thử. */
class VnTimeTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-20T23:30:00Z"));
    private final VnTime vnTime = new VnTime(clock);

    @Test
    @DisplayName("Nền tảng: 23:30 UTC ngày 20/11 là ngày 21/11 ở Việt Nam")
    void todayTheoGioVietNam() {
        assertThat(vnTime.today()).isEqualTo(LocalDate.of(2026, 11, 21));
        assertThat(vnTime.now()).isEqualTo(OffsetDateTime.of(2026, 11, 20, 23, 30, 0, 0, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Nền tảng: 06:59 giờ Việt Nam vẫn là cùng ngày (Render chạy UTC thì LocalDate.now() ra hôm trước)")
    void sangSomVanCungNgay() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 6, 59));
        assertThat(vnTime.today()).isEqualTo(LocalDate.of(2026, 11, 21));
    }

    @Test
    @DisplayName("Nền tảng: dayStart, dayEnd là 00:00 giờ Việt Nam, đổi về UTC; dayEnd là mốc mở")
    void dauVaCuoiNgay() {
        LocalDate ngay = LocalDate.of(2026, 11, 21);
        assertThat(vnTime.dayStart(ngay)).isEqualTo(OffsetDateTime.parse("2026-11-20T17:00:00Z"));
        assertThat(vnTime.dayEnd(ngay)).isEqualTo(OffsetDateTime.parse("2026-11-21T17:00:00Z"));
        assertThat(vnTime.at(ngay, LocalTime.of(6, 0))).isEqualTo(OffsetDateTime.parse("2026-11-20T23:00:00Z"));
    }

    @Test
    @DisplayName("Nền tảng: toVnDate đổi thời điểm UTC về ngày Việt Nam; MutableClock tiến giờ được")
    void doiVeNgayVietNam() {
        assertThat(vnTime.toVnDate(OffsetDateTime.parse("2026-11-20T16:59:59Z"))).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(vnTime.toVnDate(OffsetDateTime.parse("2026-11-20T17:00:00Z"))).isEqualTo(LocalDate.of(2026, 11, 21));

        clock.setVn(LocalDateTime.of(2026, 11, 20, 23, 30));
        clock.advance(Duration.ofHours(1));
        assertThat(vnTime.today()).isEqualTo(LocalDate.of(2026, 11, 21));
    }

    @Test
    @DisplayName("Nền tảng: MutableClockConfiguration thay đồng hồ thật của TimeConfig trong ngữ cảnh Spring")
    void dongHoKiemThuThayDongHoThat() {
        new ApplicationContextRunner()
            .withUserConfiguration(TimeConfig.class, MutableClockConfiguration.class, VnTime.class)
            .run(ctx -> assertThat(ctx.getBean(VnTime.class).today()).isEqualTo(LocalDate.of(2026, 11, 20)));
    }
}
