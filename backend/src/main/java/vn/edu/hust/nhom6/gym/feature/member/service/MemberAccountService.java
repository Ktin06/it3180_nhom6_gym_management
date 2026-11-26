package vn.edu.hust.nhom6.gym.feature.member.service;

import java.time.Duration;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.common.mail.EmailSender;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.auth.service.PasswordResetService;
import vn.edu.hust.nhom6.gym.feature.member.dto.CreateMemberAccountRequest;
import vn.edu.hust.nhom6.gym.feature.member.dto.CreateMemberAccountResponse;

/**
 * Nghiệp vụ tạo tài khoản đăng nhập cho hồ sơ hội viên có sẵn và gửi email kích hoạt (F2, UC-F2.2).
 *
 * <p>Tách thành lớp riêng cạnh {@link MemberService} để tránh vòng phụ thuộc
 * {@code MemberService -> PasswordResetService -> AuthService -> MemberService} mà không dùng {@code @Lazy}.
 * Dùng {@link TransactionOperations} gom bước tạo tài khoản, gắn {@code members.user_id} và phát hành token
 * trong một giao dịch CSDL duy nhất, rồi gọi {@link EmailSender} ngoài giao dịch để tránh giữ kết nối
 * HikariCP trong lúc chờ dịch vụ gửi email.
 */
@Slf4j
@Service
public class MemberAccountService {

    public static final Duration ACTIVATION_TOKEN_TTL = Duration.ofHours(72);
    public static final String ACTIVATION_EMAIL_SUBJECT = "Kích hoạt tài khoản";
    private static final String RESET_PATH = "/dat-lai-mat-khau?token=";

    private final MemberService members;
    private final PasswordResetService passwordResets;
    private final EmailSender emailSender;
    private final TransactionOperations transactionOperations;
    private final String frontendBaseUrl;

    public MemberAccountService(
            MemberService members,
            PasswordResetService passwordResets,
            EmailSender emailSender,
            TransactionOperations transactionOperations,
            @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.members = members;
        this.passwordResets = passwordResets;
        this.emailSender = emailSender;
        this.transactionOperations = transactionOperations;
        this.frontendBaseUrl = frontendBaseUrl == null ? "" : frontendBaseUrl.replaceAll("/+$", "");
    }

    public CreateMemberAccountResponse createAccount(
            CurrentUser actor, Long memberId, CreateMemberAccountRequest request) {
        IssuedAccount issued = Objects.requireNonNull(transactionOperations.execute(status -> {
            CreateMemberAccountResponse created = members.linkAccount(actor, memberId, request.email());
            String rawToken = passwordResets.issue(created.userId(), ACTIVATION_TOKEN_TTL);
            return new IssuedAccount(created, rawToken);
        }));

        CreateMemberAccountResponse created = issued.response();
        String activationLink = frontendBaseUrl + RESET_PATH + issued.rawToken();
        String body = "Xin chào,\n\n"
            + "Tài khoản hội viên phòng tập của bạn đã được tạo.\n"
            + "Mở liên kết sau trong vòng 72 giờ để đặt mật khẩu và kích hoạt tài khoản:\n"
            + activationLink + "\n\n"
            + "Nếu liên kết đã hết hạn, vui lòng dùng chức năng Quên mật khẩu trên trang đăng nhập để nhận liên kết mới.";

        try {
            emailSender.send(created.email(), ACTIVATION_EMAIL_SUBJECT, body);
        } catch (RuntimeException ex) {
            // Phản hồi lỗi của nhà cung cấp có thể chứa nội dung thư và liên kết kích hoạt.
            log.error("Không gửi được email kích hoạt tài khoản cho hội viên memberId={}, userId={}",
                created.memberId(), created.userId());
        }
        return created;
    }

    private record IssuedAccount(CreateMemberAccountResponse response, String rawToken) {
    }
}
