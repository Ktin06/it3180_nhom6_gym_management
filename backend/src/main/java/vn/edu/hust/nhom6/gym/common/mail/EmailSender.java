package vn.edu.hust.nhom6.gym.common.mail;

/**
 * Gửi một email văn bản (ADR 0013). F1, F2, F10 chỉ gọi interface này, không biết nhà cung cấp.
 *
 * <p>Máy cá nhân và kiểm thử dùng {@link LogEmailSender} (chỉ ghi log). Bản gửi thật qua Brevo
 * (S2-05.1) là một {@code @Component} cài đặt interface này; khi bean đó có mặt,
 * {@link MailConfig} tự bỏ {@link LogEmailSender}.
 */
public interface EmailSender {

    /**
     * Gửi email. Người gọi tự ghép liên kết (đặt lại mật khẩu, đặt mật khẩu) vào {@code body}.
     *
     * @param to địa chỉ người nhận
     * @param subject tiêu đề
     * @param body nội dung văn bản thuần
     */
    void send(String to, String subject, String body);
}
