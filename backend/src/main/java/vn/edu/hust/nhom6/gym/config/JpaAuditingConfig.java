package vn.edu.hust.nhom6.gym.config;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Bật JPA auditing để tự điền created_at, updated_at của {@code BaseEntity}.
 * Thời gian luôn lấy theo UTC (cột timestamptz).
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
public class JpaAuditingConfig {

    /** Đọc từ bean {@link Clock} (TimeConfig), để kiểm thử đổi giờ bằng MutableClock thì created_at cũng đổi theo. */
    @Bean
    DateTimeProvider utcDateTimeProvider(Clock clock) {
        return () -> Optional.of(OffsetDateTime.now(clock.withZone(ZoneOffset.UTC)));
    }
}
