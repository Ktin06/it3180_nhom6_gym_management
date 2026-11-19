package vn.edu.hust.nhom6.gym.common.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Đồng hồ đổi được giờ, chỉ dùng trong kiểm thử. Không đổi đồng hồ máy.
 *
 * <pre>{@code
 * clock.setVn(LocalDateTime.of(2026, 11, 20, 23, 30)); // 23:30 giờ Việt Nam
 * clock.advance(Duration.ofHours(1));                 // sang 00:30 ngày 21/11
 * }</pre>
 */
public class MutableClock extends Clock {
    private volatile Instant instant;
    private final ZoneId zone;

    public MutableClock(Instant instant) {
        this(instant, VnTime.ZONE);
    }

    private MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    /** Đặt thời điểm theo giờ Việt Nam. */
    public void setVn(LocalDateTime vnDateTime) {
        this.instant = vnDateTime.atZone(VnTime.ZONE).toInstant();
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public void advance(Duration duration) {
        this.instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    /** Trả đồng hồ cố định tại thời điểm hiện tại của đồng hồ này; đổi giờ sau đó không ảnh hưởng bản đã trả. */
    @Override
    public Clock withZone(ZoneId newZone) {
        return Clock.fixed(instant, newZone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
