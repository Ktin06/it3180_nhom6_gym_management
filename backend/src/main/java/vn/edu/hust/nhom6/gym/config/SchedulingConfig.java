package vn.edu.hust.nhom6.gym.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import vn.edu.hust.nhom6.gym.common.scheduling.ScheduledJob;

/**
 * Bật tác vụ nền ({@code @Scheduled}) khi {@code app.scheduling.enabled=true} (mặc định).
 * Kiểm thử đặt {@code false}: tác vụ không tự chạy chen giữa kiểm thử, kiểm thử gọi thẳng hàm service.
 *
 * <p>Bộ lập lịch mặc định của Spring có một luồng, nên các tác vụ chạy lần lượt, không chồng nhau.
 * Quy ước viết tác vụ xem {@link ScheduledJob}.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "app.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
