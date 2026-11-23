package vn.edu.hust.nhom6.gym.common.scheduling;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Điều kiện duy nhất cho mọi thứ thuộc tác vụ nền: {@code app.scheduling.enabled=true} (mặc định bật).
 * Dùng chung cho {@code SchedulingConfig} và {@link ScheduledJob} để hai nơi không lệch nhau.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(prefix = "app.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnSchedulingEnabled {
}
