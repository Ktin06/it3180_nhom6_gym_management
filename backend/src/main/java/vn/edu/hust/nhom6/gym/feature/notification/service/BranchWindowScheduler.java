package vn.edu.hust.nhom6.gym.feature.notification.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import vn.edu.hust.nhom6.gym.common.time.VnTime;

/**
 * Tính thời điểm gửi email hợp lệ trong giờ mở cửa của chi nhánh ({@code open_time} đến
 * {@code close_time} theo giờ Việt Nam, BR-F10.3).
 */
public final class BranchWindowScheduler {

    public static final LocalTime DEFAULT_OPEN_TIME = LocalTime.of(5, 30);
    public static final LocalTime DEFAULT_CLOSE_TIME = LocalTime.of(22, 0);

    private BranchWindowScheduler() {}

    /**
     * Nếu {@code now} nằm trong {@code [openTime, closeTime)} theo giờ Việt Nam thì gửi ngay
     * ({@code now}); nếu trước {@code openTime} thì hẹn vào {@code openTime} cùng ngày; nếu từ
     * {@code closeTime} trở đi thì hẹn vào {@code openTime} ngày hôm sau.
     */
    public static OffsetDateTime nextAllowedSendTime(
            OffsetDateTime now, LocalTime openTime, LocalTime closeTime) {
        LocalTime open = openTime != null ? openTime : DEFAULT_OPEN_TIME;
        LocalTime close = closeTime != null ? closeTime : DEFAULT_CLOSE_TIME;

        ZonedDateTime vnNow = now.atZoneSameInstant(VnTime.ZONE);
        LocalTime localTime = vnNow.toLocalTime();
        LocalDate localDate = vnNow.toLocalDate();

        if (!localTime.isBefore(open) && localTime.isBefore(close)) {
            return now.withOffsetSameInstant(ZoneOffset.UTC);
        }
        LocalDate targetDate = localTime.isBefore(open) ? localDate : localDate.plusDays(1);
        return targetDate.atTime(open).atZone(VnTime.ZONE)
            .toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }
}
