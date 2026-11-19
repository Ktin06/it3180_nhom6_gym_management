package vn.edu.hust.nhom6.gym.common.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Kiểm thử đơn vị phần email, không cần database. */
@ExtendWith(OutputCaptureExtension.class)
class EmailSenderTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
        .withUserConfiguration(MailConfig.class)
        .withPropertyValues("app.mail.from-address=gui@example.com", "app.mail.from-name=Gym Nhom 6");

    @Test
    @DisplayName("Nền tảng: LogEmailSender ghi đúng người nhận, tiêu đề và liên kết ra log")
    void logEmailSenderGhiDungNguoiNhan(CapturedOutput output) {
        new LogEmailSender(new MailProperties("gui@example.com", "Gym Nhom 6"))
            .send("hoivien@example.com", "Đặt lại mật khẩu", "Mở liên kết: http://localhost:5173/reset?token=abc");

        assertThat(output.getOut())
            .contains("Đến: hoivien@example.com")
            .contains("Tiêu đề: Đặt lại mật khẩu")
            .contains("http://localhost:5173/reset?token=abc")
            .contains("gui@example.com");
    }

    @Test
    @DisplayName("Nền tảng: không có bản gửi thật thì dùng LogEmailSender, đọc người gửi từ app.mail")
    void macDinhLaLogEmailSender() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(EmailSender.class);
            assertThat(ctx.getBean(EmailSender.class)).isInstanceOf(LogEmailSender.class);
            assertThat(ctx.getBean(MailProperties.class).fromAddress()).isEqualTo("gui@example.com");
        });
    }

    @Test
    @DisplayName("Nền tảng: có bean EmailSender khác (ví dụ Brevo) thì không tạo LogEmailSender")
    void coBanGuiThatThiBoLogEmailSender() {
        EmailSender fake = (to, subject, body) -> { };
        context.withBean(EmailSender.class, () -> fake).run(ctx -> {
            assertThat(ctx).hasSingleBean(EmailSender.class);
            assertThat(ctx.getBean(EmailSender.class)).isSameAs(fake);
        });
    }
}
