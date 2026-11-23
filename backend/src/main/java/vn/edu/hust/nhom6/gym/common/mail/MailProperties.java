package vn.edu.hust.nhom6.gym.common.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Người gửi email, đọc từ {@code app.mail.*} (biến môi trường {@code MAIL_FROM_ADDRESS},
 * {@code MAIL_FROM_NAME}, khai báo trong {@code .env.example}). Khóa Brevo không nằm ở đây (S2-05.1).
 *
 * @param fromAddress địa chỉ người gửi đã xác minh trong Brevo; có thể rỗng trên máy cá nhân
 * @param fromName tên hiển thị người gửi
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String fromAddress, String fromName) {
}
