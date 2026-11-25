package vn.edu.hust.nhom6.gym.common.mail;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Gửi email qua API HTTPS của Brevo ({@code POST https://api.brevo.com/v3/smtp/email}) theo ADR 0013.
 *
 * <p>Chỉ kích hoạt khi {@code app.mail.brevo-api-key} (biến môi trường {@code BREVO_API_KEY}) có giá trị
 * không rỗng. Khi khóa để trống, bean này không được tạo và {@link MailConfig} tự kích hoạt
 * {@link LogEmailSender} (ở profile khác {@code staging}).
 */
@Component
@ConditionalOnExpression("!'${app.mail.brevo-api-key:}'.isBlank()")
public class BrevoEmailSender implements EmailSender {

    public static final String BREVO_SEND_URL = "https://api.brevo.com/v3/smtp/email";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final MailProperties mailProperties;
    private final String apiKey;
    private final RestClient restClient;

    @Autowired
    public BrevoEmailSender(
            MailProperties mailProperties,
            @Value("${app.mail.brevo-api-key}") String apiKey,
            ObjectProvider<RestClient.Builder> restClientBuilderProvider) {
        this(mailProperties, apiKey, restClientBuilderProvider.getIfAvailable(BrevoEmailSender::defaultRestClientBuilder));
    }

    BrevoEmailSender(MailProperties mailProperties, String apiKey, RestClient.Builder restClientBuilder) {
        this.mailProperties = mailProperties;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.restClient = restClientBuilder.build();
    }

    private static RestClient.Builder defaultRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Override
    public void send(String to, String subject, String body) {
        BrevoSendRequest payload = new BrevoSendRequest(
            new BrevoSender(mailProperties.fromName(), mailProperties.fromAddress()),
            List.of(new BrevoRecipient(to)),
            subject,
            body
        );

        restClient.post()
            .uri(BREVO_SEND_URL)
            .header("api-key", apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .toBodilessEntity();
    }

    private record BrevoSendRequest(
        BrevoSender sender,
        List<BrevoRecipient> to,
        String subject,
        String textContent
    ) {
    }

    private record BrevoSender(String name, String email) {
    }

    private record BrevoRecipient(String email) {
    }
}
