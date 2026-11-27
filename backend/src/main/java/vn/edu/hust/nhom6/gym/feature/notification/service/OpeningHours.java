package vn.edu.hust.nhom6.gym.feature.notification.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import vn.edu.hust.nhom6.gym.common.time.VnTime;

/**
 * Khung giờ mở cửa một chi nhánh theo giờ Việt Nam ({@code [open, close)}).
 *
 * <p>Dùng cho BR-F10.3: email chỉ được gửi trong giờ mở cửa; phát sinh ngoài giờ thì hẹn vào giờ mở cửa
 * kế tiếp của đúng chi nhánh.
 */
public record OpeningHours(LocalTime open, LocalTime close) {

    /** Kiểm tra thời điểm {@code at} có nằm trong khoảng nửa mở {@code [open, close)} giờ Việt Nam hay không. */
    public boolean isOpenAt(OffsetDateTime at) {
        LocalTime localTime = at.atZoneSameInstant(VnTime.ZONE).toLocalTime();
        return !localTime.isBefore(open) && localTime.isBefore(close);
    }

    /**
     * Thời điểm sớm nhất được phép gửi email tính từ {@code now} (trả về UTC):
     * <ul>
     *   <li>đang mở cửa → chính {@code now};</li>
     *   <li>trước giờ mở cửa → giờ mở cửa cùng ngày Việt Nam;</li>
     *   <li>từ giờ đóng cửa trở đi → giờ mở cửa ngày Việt Nam hôm sau.</li>
     * </ul>
     */
    public OffsetDateTime nextSendAt(OffsetDateTime now) {
        if (isOpenAt(now)) {
            return now;
        }
        ZonedDateTime vn = now.atZoneSameInstant(VnTime.ZONE);
        LocalDate targetDay = vn.toLocalTime().isBefore(open) ? vn.toLocalDate() : vn.toLocalDate().plusDays(1);
        return targetDay.atTime(open).atZone(VnTime.ZONE).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }
}
