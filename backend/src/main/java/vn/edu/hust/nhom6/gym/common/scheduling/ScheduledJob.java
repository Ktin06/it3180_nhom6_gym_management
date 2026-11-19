package vn.edu.hust.nhom6.gym.common.scheduling;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Đánh dấu một lớp tác vụ nền. Lớp chỉ được tạo khi {@code app.scheduling.enabled=true}, nên trong
 * kiểm thử cả lịch cron lẫn phần chạy bù khi khởi động đều không chạy.
 *
 * <p>Quy ước (chi tiết trong bàn giao N0.2):
 * <ol>
 *   <li>Tên lớp {@code <Việc>Job}, đặt ở gói {@code feature/<chức năng>/job}.</li>
 *   <li>Lớp chỉ gọi <b>một</b> hàm public của service; logic và kiểm thử nằm ở service.</li>
 *   <li>Cron luôn ghi múi giờ: {@code @Scheduled(cron = "0 0 0 * * *", zone = VnTime.ZONE_ID)}.</li>
 *   <li>Chạy bù khi khởi động: {@code @EventListener(ApplicationReadyEvent.class)} gọi cùng hàm đó.</li>
 *   <li>Hàm service chạy lặp nhiều lần không sinh dữ liệu sai (lọc theo trạng thái hiện tại,
 *       không cộng dồn), vì cron và chạy bù có thể cùng xử lý một ngày.</li>
 * </ol>
 *
 * <pre>{@code
 * @ScheduledJob
 * @RequiredArgsConstructor
 * public class MembershipDailyJob {
 *     private final MembershipLifecycleService memberships;
 *
 *     @Scheduled(cron = "0 0 0 * * *", zone = VnTime.ZONE_ID)
 *     public void runDaily() { memberships.runDailyTransitions(); }
 *
 *     @EventListener(ApplicationReadyEvent.class)
 *     public void catchUpOnStartup() { memberships.runDailyTransitions(); }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@ConditionalOnProperty(prefix = "app.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public @interface ScheduledJob {
}
