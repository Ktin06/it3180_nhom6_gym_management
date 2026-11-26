package vn.edu.hust.nhom6.gym.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.notification.service.BranchWindowScheduler;

class BranchWindowSchedulerTest {

    private static final LocalTime OPEN = LocalTime.of(5, 30);
    private static final LocalTime CLOSE = LocalTime.of(22, 0);

    private static OffsetDateTime vn(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute)
            .atZone(VnTime.ZONE)
            .toOffsetDateTime()
            .withOffsetSameInstant(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("BR-F10.3: trong giờ mở cửa chi nhánh (05:30–22:00) thì hẹn gửi ngay")
    void trongGioMoCuaGuiNgay() {
        OffsetDateTime now = vn(2026, 11, 26, 9, 0);
        assertThat(BranchWindowScheduler.nextAllowedSendTime(now, OPEN, CLOSE)).isEqualTo(now);
    }

    @Test
    @DisplayName("BR-F10.3: trước giờ mở cửa (04:00) thì hẹn vào 05:30 cùng ngày")
    void truocGioMoCuaHenCungNgay() {
        OffsetDateTime now = vn(2026, 11, 26, 4, 0);
        assertThat(BranchWindowScheduler.nextAllowedSendTime(now, OPEN, CLOSE))
            .isEqualTo(vn(2026, 11, 26, 5, 30));
    }

    @Test
    @DisplayName("BR-F10.3: từ giờ đóng cửa (22:00) trở đi thì hẹn vào 05:30 ngày hôm sau")
    void sauGioDongCuaHenHomSau() {
        OffsetDateTime atClose = vn(2026, 11, 26, 22, 0);
        OffsetDateTime lateNight = vn(2026, 11, 26, 23, 15);
        OffsetDateTime expected = vn(2026, 11, 27, 5, 30);

        assertThat(BranchWindowScheduler.nextAllowedSendTime(atClose, OPEN, CLOSE)).isEqualTo(expected);
        assertThat(BranchWindowScheduler.nextAllowedSendTime(lateNight, OPEN, CLOSE)).isEqualTo(expected);
    }
}
