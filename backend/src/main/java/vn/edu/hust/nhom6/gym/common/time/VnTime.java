package vn.edu.hust.nhom6.gym.common.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mọi phép tính "hôm nay", "bây giờ" của nghiệp vụ đi qua lớp này, không gọi {@code LocalDate.now()}
 * trực tiếp. Lý do: {@code LocalDate.now()} lấy múi giờ của máy chủ; Render chạy UTC nên từ 00:00
 * đến 07:00 giờ Việt Nam sẽ ra ngày hôm trước.
 *
 * <p>Quy ước: ngày nghiệp vụ là ngày Việt Nam ({@link LocalDate}); mốc thời điểm trả về dạng
 * {@link OffsetDateTime} ở UTC, khớp cột timestamptz (quy tắc mã 7). Kiểm thử đổi giờ bằng cách
 * thay bean {@link Clock} (lớp {@code MutableClock} trong thư mục kiểm thử).
 */
@Component
@RequiredArgsConstructor
public class VnTime {
    /** Dùng trong {@code @Scheduled(cron = ..., zone = VnTime.ZONE_ID)}. */
    public static final String ZONE_ID = "Asia/Ho_Chi_Minh";
    public static final ZoneId ZONE = ZoneId.of(ZONE_ID);

    private final Clock clock;

    /** Ngày hiện tại theo giờ Việt Nam. */
    public LocalDate today() {
        return LocalDate.now(clock.withZone(ZONE));
    }

    /** Thời điểm hiện tại, offset UTC. */
    public OffsetDateTime now() {
        return OffsetDateTime.now(clock.withZone(ZoneOffset.UTC));
    }

    /** 00:00 giờ Việt Nam của {@code date}, offset UTC (tức 17:00 UTC ngày hôm trước). */
    public OffsetDateTime dayStart(LocalDate date) {
        return at(date, LocalTime.MIDNIGHT);
    }

    /**
     * Mốc kết thúc ngày, dạng <b>mở</b>: bằng {@code dayStart(date + 1)}. Lọc "trong ngày" viết
     * {@code t >= dayStart(d) AND t < dayEnd(d)}, không dùng {@code <=} và không dùng 23:59:59.
     */
    public OffsetDateTime dayEnd(LocalDate date) {
        return dayStart(date.plusDays(1));
    }

    /** Giờ {@code time} của ngày {@code date} theo giờ Việt Nam, offset UTC (ví dụ giờ mở cửa chi nhánh). */
    public OffsetDateTime at(LocalDate date, LocalTime time) {
        return date.atTime(time).atZone(ZONE).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }

    /** Ngày Việt Nam của một thời điểm đã lưu (ví dụ {@code checked_in_at}). */
    public LocalDate toVnDate(OffsetDateTime instant) {
        return instant.atZoneSameInstant(ZONE).toLocalDate();
    }
}
