package vn.edu.hust.nhom6.gym.feature.auth.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.mail.EmailSender;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.dto.ForgotPasswordRequest;
import vn.edu.hust.nhom6.gym.feature.auth.dto.ResetPasswordRequest;
import vn.edu.hust.nhom6.gym.feature.auth.entity.PasswordReset;
import vn.edu.hust.nhom6.gym.feature.auth.entity.User;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserStatus;
import vn.edu.hust.nhom6.gym.feature.auth.repository.PasswordResetRepository;
import vn.edu.hust.nhom6.gym.feature.auth.repository.RefreshTokenRepository;
import vn.edu.hust.nhom6.gym.feature.auth.repository.UserRepository;

/**
 * Nghiệp vụ quên mật khẩu và đặt lại mật khẩu bằng token dùng một lần (F1, UC-F1.5).
 */
@Slf4j
@Service
public class PasswordResetService {

    public static final String FORGOT_PASSWORD_MESSAGE =
        "Nếu email có trong hệ thống, bạn sẽ nhận được liên kết đặt lại mật khẩu.";
    public static final String INVALID_OR_EXPIRED_TOKEN_MESSAGE =
        "Liên kết đã hết hạn hoặc đã được dùng.";
    public static final Duration FORGOT_PASSWORD_TTL = Duration.ofMinutes(30);

    private static final String RESET_PATH = "/dat-lai-mat-khau?token=";
    private static final String FORGOT_PASSWORD_SUBJECT = "Đặt lại mật khẩu tài khoản Gym Nhóm 6";

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final EmailSender emailSender;
    private final VnTime vnTime;
    private final String frontendBaseUrl;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetRepository passwordResetRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthService authService,
            EmailSender emailSender,
            VnTime vnTime,
            @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.emailSender = emailSender;
        this.vnTime = vnTime;
        this.frontendBaseUrl = frontendBaseUrl == null ? "" : frontendBaseUrl.replaceAll("/+$", "");
    }

    /**
     * Phát hành token đặt lại mật khẩu dùng một lần cho tài khoản {@code userId} với thời hạn {@code ttl}.
     * Chỉ lưu mã băm SHA-256 vào bảng {@code password_resets}; trả về token thô để người gọi ghép vào email.
     *
     * <p>Mở {@code public} để F2 (S2-36 – tạo tài khoản cho hồ sơ có sẵn) tái sử dụng với hạn 72 giờ (Q7 B1).
     */
    @Transactional
    public String issue(Long userId, Duration ttl) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản."));
        return saveResetToken(user, ttl);
    }

    /**
     * Xử lý yêu cầu quên mật khẩu (UC-F1.5 bước 1–2): chỉ phát hành token và gửi email khi tài khoản
     * tồn tại và đang {@code ACTIVE}. Luôn hoàn tất không ném lỗi khi email không tồn tại, không ACTIVE
     * hoặc nhà cung cấp email gặp sự cố để không lộ email (Q7 C1).
     *
     * <p>Không bọc {@code @Transactional} quanh bước gọi HTTP gửi email để tránh giữ kết nối CSDL trong
     * lúc chờ dịch vụ ngoài (Brevo).
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().toLowerCase().trim();
        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty() || found.get().getStatus() != UserStatus.ACTIVE) {
            return;
        }

        User user = found.get();
        String rawToken = saveResetToken(user, FORGOT_PASSWORD_TTL);
        String resetLink = frontendBaseUrl + RESET_PATH + rawToken;
        String body = "Xin chào,\n\n"
            + "Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản phòng tập.\n"
            + "Mở liên kết sau trong vòng 30 phút để đặt mật khẩu mới:\n"
            + resetLink + "\n\n"
            + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.";

        try {
            emailSender.send(user.getEmail(), FORGOT_PASSWORD_SUBJECT, body);
        } catch (RuntimeException ex) {
            // Không ném lỗi ra ngoài để giữ phản hồi 200 câu chung (C1); tuyệt đối không ghi token ra log.
            log.error("Không gửi được email đặt lại mật khẩu cho người dùng id={}: {}", user.getId(), ex.getMessage());
        }
    }

    /**
     * Đặt lại mật khẩu bằng token dùng một lần (UC-F1.5 bước 3): khóa dòng token và dòng {@code users},
     * đổi mật khẩu mới, xóa khóa đăng nhập tạm, đánh dấu {@code used_at} và thu hồi mọi refresh token.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        authService.validatePasswordLength(request.newPassword());

        String tokenHash = TokenHashUtil.sha256Hex(request.token().trim());
        PasswordReset reset = passwordResetRepository.findByTokenHashForUpdate(tokenHash)
            .orElseThrow(() -> new BusinessRuleException(INVALID_OR_EXPIRED_TOKEN_MESSAGE));

        // Khóa dòng users trước (SELECT ... FOR UPDATE) để không có refresh token mới nào sống sót
        User user = userRepository.findByIdForUpdate(reset.getUser().getId())
            .orElseThrow(() -> new BusinessRuleException(INVALID_OR_EXPIRED_TOKEN_MESSAGE));

        OffsetDateTime now = vnTime.now();
        if (reset.getUsedAt() != null
                || !reset.getExpiresAt().isAfter(now)
                || user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException(INVALID_OR_EXPIRED_TOKEN_MESSAGE);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        reset.setUsedAt(now);
        passwordResetRepository.save(reset);

        refreshTokenRepository.revokeAllByUserId(user.getId(), now);
    }

    private String saveResetToken(User user, Duration ttl) {
        String rawToken = TokenHashUtil.generateRawToken();
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);
        OffsetDateTime expiresAt = vnTime.now().plus(ttl);

        PasswordReset passwordReset = PasswordReset.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(expiresAt)
            .build();
        passwordResetRepository.save(passwordReset);
        return rawToken;
    }
}
