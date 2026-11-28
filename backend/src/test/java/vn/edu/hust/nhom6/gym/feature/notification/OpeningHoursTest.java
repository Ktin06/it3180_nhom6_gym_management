package vn.edu.hust.nhom6.gym.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vn.edu.hust.nhom6.gym.feature.notification.service.OpeningHours;

/** BR-F10.3: tính thời điểm gửi email theo giờ mở cửa chi nhánh ({@code [open, close)} giờ Việt Nam). */
class OpeningHoursTest {
    final OpeningHours cn01 = new OpeningHours(LocalTime.of(5, 30), LocalTime.of(22, 0));

    @Test
    @DisplayName("TC-F10.1.1-07g: trong giờ mở cửa thì gửi ngay")
    void insideHoursSendsNow() {
        var now = NotificationFixtures.utc(LocalDateTime.of(2026, 11, 21, 10, 0));
        assertThat(cn01.isOpenAt(now)).isTrue();
        assertThat(cn01.nextSendAt(now)).isEqualTo(now);
    }

    @Test
    @DisplayName("TC-F10.1.1-07h: tạo lúc 23:00 thì hẹn 05:30 hôm sau")
    void afterCloseSendsNextMorning() {
        var now = NotificationFixtures.utc(LocalDateTime.of(2026, 11, 21, 23, 0));
        assertThat(cn01.isOpenAt(now)).isFalse();
        assertThat(cn01.nextSendAt(now)).isEqualTo(NotificationFixtures.utc(LocalDateTime.of(2026, 11, 22, 5, 30)));
    }

    @Test
    @DisplayName("TC-F10.1.1-07i: trước giờ mở cửa thì hẹn giờ mở cửa cùng ngày Việt Nam (00:10 VN là 17:10 UTC hôm trước)")
    void beforeOpenSendsSameDay() {
        var now = NotificationFixtures.utc(LocalDateTime.of(2026, 11, 21, 0, 10));
        assertThat(cn01.nextSendAt(now)).isEqualTo(NotificationFixtures.utc(LocalDateTime.of(2026, 11, 21, 5, 30)));
    }

    @Test
    @DisplayName("TC-F10.1.1-07j: biên 05:29 đóng, 05:30 mở, 21:59 mở, 22:00 đóng")
    void boundaries() {
        assertThat(cn01.isOpenAt(NotificationFixtures.utc(LocalDateTime.of(2026, 11, 21, 5, 29)))).isFalse();
        assertThat(cn01.isOpenAt(NotificationFixtures.utc(LocalDateTime.of(2026, 11, 21, 5, 30)))).isTrue();
        assertThat(cn01.isOpenAt(NotificationFixtures.utc(LocalDateTime.of(2026, 11, 21, 21, 59)))).isTrue();
        var close = NotificationFixtures.utc(LocalDateTime.of(2026, 11, 21, 22, 0));
        assertThat(cn01.isOpenAt(close)).isFalse();
        assertThat(cn01.nextSendAt(close)).isEqualTo(NotificationFixtures.utc(LocalDateTime.of(2026, 11, 22, 5, 30)));
    }
}
