package vn.edu.hust.nhom6.gym.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import vn.edu.hust.nhom6.gym.common.scheduling.ConditionalOnSchedulingEnabled;
import vn.edu.hust.nhom6.gym.common.scheduling.ScheduledJob;

/**
 * Bật tác vụ nền ({@code @Scheduled}) khi {@code app.scheduling.enabled=true} (mặc định).
 * Kiểm thử đặt {@code false}: tác vụ không tự chạy chen giữa kiểm thử, kiểm thử gọi thẳng hàm service.
 *
 * <p>Spring Boot tạo bean {@code TaskScheduler} một luồng (mặc định {@code spring.task.scheduling.pool.size=1}).
 * Mọi việc giao cho nó, kể cả lần chạy bù khi khởi động, chạy lần lượt, không chồng nhau.
 * Quy ước viết tác vụ xem {@link ScheduledJob}.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnSchedulingEnabled
public class SchedulingConfig {
}
