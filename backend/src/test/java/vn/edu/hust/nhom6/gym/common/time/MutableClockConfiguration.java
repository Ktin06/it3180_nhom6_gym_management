package vn.edu.hust.nhom6.gym.common.time;

import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Kiểm thử tích hợp cần đặt thời điểm thì {@code @Import(MutableClockConfiguration.class)} rồi
 * {@code @Autowired MutableClock clock}. Bean này {@code @Primary} nên thay đồng hồ thật của
 * {@code TimeConfig}. Mặc định 20/11/2026 09:00 giờ Việt Nam; mỗi kiểm thử nên tự đặt lại giờ.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MutableClockConfiguration {

    @Bean
    @Primary
    MutableClock mutableClock() {
        MutableClock clock = new MutableClock(Instant.EPOCH);
        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        return clock;
    }
}
