package vn.edu.hust.nhom6.gym.common.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Chọn bản cài đặt {@link EmailSender}. Mặc định là {@link LogEmailSender}.
 *
 * <p>Bản Brevo (S2-05.1) <b>phải</b> khai báo bằng {@code @Component} (được quét trước khi Spring xét
 * điều kiện của các hàm {@code @Bean}), kèm điều kiện có {@code BREVO_API_KEY}. Khi đó bean dưới đây
 * không được tạo, và file này không phải sửa. Nếu khai báo Brevo bằng {@code @Bean} trong một lớp
 * {@code @Configuration} khác thì thứ tự xử lý không chắc chắn, có thể sinh hai bean.
 *
 * <p>Không tạo {@link LogEmailSender} ở profile {@code staging}: bản này ghi liên kết đặt mật khẩu ra
 * log. Staging thiếu bản Brevo thì ứng dụng dừng khi khởi động (thiếu bean), thay vì âm thầm lộ liên kết.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    @Bean
    @Profile("!staging")
    @ConditionalOnMissingBean(EmailSender.class)
    EmailSender logEmailSender(MailProperties properties) {
        return new LogEmailSender(properties);
    }
}
