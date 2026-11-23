package vn.edu.hust.nhom6.gym.common.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Không gửi email thật, chỉ ghi người nhận, tiêu đề, nội dung (kèm liên kết) ra log mức INFO
 * (ADR 0013 điểm 4). Dùng trên máy cá nhân và trong kiểm thử; lấy liên kết trong log để thử luồng.
 *
 * <p>Nội dung có thể chứa liên kết đặt mật khẩu, vì vậy không dùng lớp này trên staging: staging
 * phải có bản gửi qua Brevo.
 */
@Slf4j
@RequiredArgsConstructor
public class LogEmailSender implements EmailSender {
    private final MailProperties properties;

    @Override
    public void send(String to, String subject, String body) {
        log.info("[EMAIL GIẢ LẬP] Từ: {} <{}> | Đến: {} | Tiêu đề: {}\n{}",
            properties.fromName(), properties.fromAddress(), to, subject, body);
    }
}
