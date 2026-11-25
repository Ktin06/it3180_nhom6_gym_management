package vn.edu.hust.nhom6.gym.common.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Kiểm thử đơn vị cho {@link BrevoEmailSender} và điều kiện chọn bean {@link EmailSender} (ADR 0013, S2-05).
 * Sử dụng {@link MockRestServiceServer}, tuyệt đối không gọi mạng thật.
 */
class BrevoEmailSenderTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(MailConfig.class, BrevoEmailSender.class)
        .withPropertyValues(
            "app.mail.from-address=sender@example.com",
            "app.mail.from-name=Gym Nhom 6"
        );

    @Test
    @DisplayName("TC-F1.5.1-06a: gửi email qua Brevo gửi đúng POST /v3/smtp/email, header api-key và JSON body")
    void guiEmailQuaBrevoBangRestClientKhongGoiMangThat() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo(BrevoEmailSender.BREVO_SEND_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("api-key", "fake-brevo-test-key"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.sender.email").value("sender@example.com"))
            .andExpect(jsonPath("$.sender.name").value("Gym Nhom 6"))
            .andExpect(jsonPath("$.to[0].email").value("member@example.com"))
            .andExpect(jsonPath("$.subject").value("Đặt lại mật khẩu"))
            .andExpect(jsonPath("$.textContent").value("Mở liên kết: http://localhost:5173/dat-lai-mat-khau?token=abc"))
            .andRespond(withStatus(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"messageId\":\"<test-id@brevo>\"}"));

        BrevoEmailSender sender = new BrevoEmailSender(
            new MailProperties("sender@example.com", "Gym Nhom 6"),
            "fake-brevo-test-key",
            builder
        );

        sender.send(
            "member@example.com",
            "Đặt lại mật khẩu",
            "Mở liên kết: http://localhost:5173/dat-lai-mat-khau?token=abc"
        );

        server.verify();
    }

    @Test
    @DisplayName("TC-F1.5.1-06b: khi khóa Brevo rỗng hoặc toàn khoảng trắng thì bean EmailSender là LogEmailSender")
    void khoaBrevoRongThiBeanLaLogEmailSender() {
        // Không khai báo khóa
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(EmailSender.class);
            assertThat(ctx.getBean(EmailSender.class)).isInstanceOf(LogEmailSender.class);
        });

        // Khóa rỗng (BREVO_API_KEY=)
        contextRunner.withPropertyValues("app.mail.brevo-api-key=")
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(EmailSender.class);
                assertThat(ctx.getBean(EmailSender.class)).isInstanceOf(LogEmailSender.class);
            });

        // Khóa toàn khoảng trắng
        contextRunner.withPropertyValues("app.mail.brevo-api-key=   ")
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(EmailSender.class);
                assertThat(ctx.getBean(EmailSender.class)).isInstanceOf(LogEmailSender.class);
            });
    }

    @Test
    @DisplayName("TC-F1.5.1-06c: khi có khóa Brevo hợp lệ thì bean EmailSender là BrevoEmailSender, bỏ LogEmailSender")
    void coKhoaBrevoThiBeanLaBrevoEmailSender() {
        contextRunner.withPropertyValues("app.mail.brevo-api-key=fake-brevo-test-key")
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(EmailSender.class);
                assertThat(ctx.getBean(EmailSender.class)).isInstanceOf(BrevoEmailSender.class);
            });

        // Profile staging có khóa Brevo -> vẫn kích hoạt BrevoEmailSender
        contextRunner.withPropertyValues(
                "spring.profiles.active=staging",
                "app.mail.brevo-api-key=fake-brevo-test-key")
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(EmailSender.class);
                assertThat(ctx.getBean(EmailSender.class)).isInstanceOf(BrevoEmailSender.class);
            });

        // Profile staging để trống khóa Brevo -> không có bean EmailSender (dừng khi khởi động)
        contextRunner.withPropertyValues(
                "spring.profiles.active=staging",
                "app.mail.brevo-api-key=")
            .run(ctx -> assertThat(ctx).doesNotHaveBean(EmailSender.class));
    }
}
