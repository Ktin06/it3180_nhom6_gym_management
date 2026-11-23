package vn.edu.hust.nhom6.gym.common.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Đồng hồ đổi được giờ, chỉ dùng trong kiểm thử. Không đổi đồng hồ máy.
 *
 * <pre>{@code
 * clock.setVn(LocalDateTime.of(2026, 11, 20, 23, 30)); // 23:30 giờ Việt Nam
 * clock.advance(Duration.ofHours(1));                 // sang 00:30 ngày 21/11
 * }</pre>
 *
 * <p>{@link #withZone} trả một đồng hồ dùng chung thời điểm với đồng hồ gốc, nên service lưu sẵn
 * {@code clock.withZone(...)} vẫn thấy mọi lần đổi giờ sau đó.
 */
public class MutableClock extends Clock {
    private final AtomicReference<Instant> instant;
    private final ZoneId zone;

    public MutableClock(Instant instant) {
        this(new AtomicReference<>(instant), VnTime.ZONE);
    }

    private MutableClock(AtomicReference<Instant> instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    /** Đặt thời điểm theo giờ Việt Nam. */
    public void setVn(LocalDateTime vnDateTime) {
        instant.set(vnDateTime.atZone(VnTime.ZONE).toInstant());
    }

    public void setInstant(Instant newInstant) {
        instant.set(newInstant);
    }

    public void advance(Duration duration) {
        instant.updateAndGet(current -> current.plus(duration));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new MutableClock(instant, newZone);
    }

    @Override
    public Instant instant() {
        return instant.get();
    }
}
