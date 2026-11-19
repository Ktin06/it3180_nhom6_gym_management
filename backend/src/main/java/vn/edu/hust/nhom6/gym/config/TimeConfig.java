package vn.edu.hust.nhom6.gym.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import vn.edu.hust.nhom6.gym.common.time.VnTime;

/**
 * Đồng hồ dùng chung, cố định múi giờ Việt Nam, không phụ thuộc múi giờ máy chủ.
 * Service cần thời điểm thì nhận {@link VnTime} (hoặc {@link Clock}) qua constructor.
 */
@Configuration(proxyBeanMethods = false)
public class TimeConfig {

    @Bean
    Clock clock() {
        return Clock.system(VnTime.ZONE);
    }
}
