package vn.edu.hust.nhom6.gym.common.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chọn bản cài đặt {@link EmailSender}. Mặc định là {@link LogEmailSender}.
 *
 * <p>Bản Brevo (S2-05.1) khai báo bằng {@code @Component} (được quét trước khi Spring xét điều kiện
 * của các hàm {@code @Bean}), kèm điều kiện có {@code BREVO_API_KEY}. Khi đó bean dưới đây không
 * được tạo, và file này không phải sửa.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    @Bean
    @ConditionalOnMissingBean(EmailSender.class)
    EmailSender logEmailSender(MailProperties properties) {
        return new LogEmailSender(properties);
    }
}
