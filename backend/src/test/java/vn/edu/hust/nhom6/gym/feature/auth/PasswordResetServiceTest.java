package vn.edu.hust.nhom6.gym.feature.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.mail.EmailSender;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.dto.ForgotPasswordRequest;
import vn.edu.hust.nhom6.gym.feature.auth.dto.ResetPasswordRequest;
import vn.edu.hust.nhom6.gym.feature.auth.entity.PasswordReset;
import vn.edu.hust.nhom6.gym.feature.auth.entity.User;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserStatus;
import vn.edu.hust.nhom6.gym.feature.auth.repository.PasswordResetRepository;
import vn.edu.hust.nhom6.gym.feature.auth.repository.RefreshTokenRepository;
import vn.edu.hust.nhom6.gym.feature.auth.repository.UserRepository;
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthService;
import vn.edu.hust.nhom6.gym.feature.auth.service.PasswordResetService;
import vn.edu.hust.nhom6.gym.feature.auth.service.TokenHashUtil;

/**
 * Kiểm thử đơn vị cho {@link PasswordResetService} (UC-F1.5, S2-05).
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetRepository passwordResetRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @Mock
    private EmailSender emailSender;

    private MutableClock clock;
    private VnTime vnTime;
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.EPOCH);
        clock.setVn(LocalDateTime.of(2026, 11, 20, 10, 0));
        vnTime = new VnTime(clock);
        passwordResetService = new PasswordResetService(
            userRepository,
            passwordResetRepository,
            refreshTokenRepository,
            passwordEncoder,
            authService,
            emailSender,
            vnTime,
            "http://localhost:5173/"
        );
    }

    @Test
    @DisplayName("US-F1.5.1 KB1: email ACTIVE tạo password_resets băm hạn 30 phút, gửi email chứa liên kết và trả 200 câu chung")
    void emailActiveTaoPasswordResetBamHan30PhutVaGuiEmail() {
        User user = createUser(10L, "member@example.com", UserStatus.ACTIVE);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(passwordResetRepository.save(any(PasswordReset.class))).thenAnswer(inv -> inv.getArgument(0));

        OffsetDateTime expectedExpiresAt = vnTime.now().plusMinutes(30);

        passwordResetService.forgotPassword(new ForgotPasswordRequest("  Member@Example.com "));

        ArgumentCaptor<PasswordReset> resetCaptor = ArgumentCaptor.forClass(PasswordReset.class);
        verify(passwordResetRepository).save(resetCaptor.capture());
        PasswordReset saved = resetCaptor.getValue();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("member@example.com"), anyString(), bodyCaptor.capture());
        String emailBody = bodyCaptor.getValue();

        assertThat(emailBody).contains("http://localhost:5173/dat-lai-mat-khau?token=");
        String rawToken = extractTokenFromBody(emailBody);
        assertThat(rawToken).isNotBlank();

        // Chỉ lưu mã băm SHA-256, không lưu token thô
        assertThat(saved.getTokenHash()).isEqualTo(TokenHashUtil.sha256Hex(rawToken));
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getExpiresAt()).isEqualTo(expectedExpiresAt);
        assertThat(saved.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("US-F1.5.1 C1: không lộ email khi email không tồn tại hoặc tài khoản không ACTIVE")
    void khongLoEmailKhiKhongTonTaiHoacKhongActive() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        passwordResetService.forgotPassword(new ForgotPasswordRequest("missing@example.com"));

        User disabledUser = createUser(11L, "disabled@example.com", UserStatus.DISABLED);
        when(userRepository.findByEmail("disabled@example.com")).thenReturn(Optional.of(disabledUser));
        passwordResetService.forgotPassword(new ForgotPasswordRequest("disabled@example.com"));

        User lockedUser = createUser(12L, "locked@example.com", UserStatus.LOCKED);
        when(userRepository.findByEmail("locked@example.com")).thenReturn(Optional.of(lockedUser));
        passwordResetService.forgotPassword(new ForgotPasswordRequest("locked@example.com"));

        verify(passwordResetRepository, never()).save(any());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("US-F1.5.1 C1: khi Brevo gửi email lỗi vẫn không ném ngoại lệ và ghi log ERROR không chứa token")
    void guiEmailLoiVanKhongNemNgoaiLeVaKhongGhiTokenRaLog(CapturedOutput output) {
        User user = createUser(15L, "member@example.com", UserStatus.ACTIVE);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(passwordResetRepository.save(any(PasswordReset.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        doThrow(new RuntimeException("Brevo 503 Service Unavailable"))
            .when(emailSender).send(eq("member@example.com"), anyString(), bodyCaptor.capture());

        passwordResetService.forgotPassword(new ForgotPasswordRequest("member@example.com"));

        String rawToken = extractTokenFromBody(bodyCaptor.getValue());
        assertThat(output.getOut()).contains("ERROR");
        assertThat(output.getOut()).contains("15");
        assertThat(output.getOut()).doesNotContain(rawToken);
    }

    @Test
    @DisplayName("US-F1.5.1 KB3: đặt lại mật khẩu đúng token trả 204, ghi used_at, thu hồi mọi refresh token và mở khóa tạm")
    void datLaiMatKhauDungTokenKhoaUserGhiUsedAtXoaKhoaTamVaThuHoiPhien() {
        User user = createUser(20L, "member@example.com", UserStatus.ACTIVE);
        user.setFailedLoginCount(5);
        user.setLockedUntil(vnTime.now().plusMinutes(10));

        String rawToken = "valid-raw-token-123";
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);
        PasswordReset reset = PasswordReset.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(vnTime.now().plusMinutes(15))
            .usedAt(null)
            .build();

        when(passwordResetRepository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(reset));
        when(userRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("$2a$10$newEncodedHash");

        OffsetDateTime expectedNow = vnTime.now();
        passwordResetService.resetPassword(new ResetPasswordRequest(rawToken, "NewPassword123"));

        verify(authService).validatePasswordLength("NewPassword123");
        // Đảm bảo khóa dòng users trước khi cập nhật và thu hồi refresh token
        InOrder order = inOrder(passwordResetRepository, userRepository, refreshTokenRepository);
        order.verify(passwordResetRepository).findByTokenHashForUpdate(tokenHash);
        order.verify(userRepository).findByIdForUpdate(20L);
        order.verify(userRepository).save(user);
        order.verify(passwordResetRepository).save(reset);
        order.verify(refreshTokenRepository).revokeAllByUserId(20L, expectedNow);

        assertThat(user.getPasswordHash()).isEqualTo("$2a$10$newEncodedHash");
        assertThat(user.getFailedLoginCount()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(reset.getUsedAt()).isEqualTo(expectedNow);
    }

    @Test
    @DisplayName("US-F1.5.1 KB3: token đã dùng, hết hạn hoặc sai trả 422 Liên kết đã hết hạn hoặc đã được dùng")
    void tokenSaiDaDungHoacHetHanTra422() {
        // 1. Token không tồn tại
        when(passwordResetRepository.findByTokenHashForUpdate(TokenHashUtil.sha256Hex("unknown-token")))
            .thenReturn(Optional.empty());
        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new ResetPasswordRequest("unknown-token", "NewPassword123")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage(PasswordResetService.INVALID_OR_EXPIRED_TOKEN_MESSAGE);

        // 2. Token đã dùng
        User activeUser = createUser(21L, "used@example.com", UserStatus.ACTIVE);
        PasswordReset usedReset = PasswordReset.builder()
            .user(activeUser)
            .tokenHash(TokenHashUtil.sha256Hex("used-token"))
            .expiresAt(vnTime.now().plusMinutes(20))
            .usedAt(vnTime.now().minusMinutes(1))
            .build();
        when(passwordResetRepository.findByTokenHashForUpdate(usedReset.getTokenHash()))
            .thenReturn(Optional.of(usedReset));
        when(userRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new ResetPasswordRequest("used-token", "NewPassword123")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage(PasswordResetService.INVALID_OR_EXPIRED_TOKEN_MESSAGE);

        // 3. Token hết hạn (expiresAt == now)
        PasswordReset expiredReset = PasswordReset.builder()
            .user(activeUser)
            .tokenHash(TokenHashUtil.sha256Hex("expired-token"))
            .expiresAt(vnTime.now())
            .usedAt(null)
            .build();
        when(passwordResetRepository.findByTokenHashForUpdate(expiredReset.getTokenHash()))
            .thenReturn(Optional.of(expiredReset));

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new ResetPasswordRequest("expired-token", "NewPassword123")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage(PasswordResetService.INVALID_OR_EXPIRED_TOKEN_MESSAGE);

        // 4. Tài khoản đã bị DISABLED sau khi phát hành token
        User disabledUser = createUser(22L, "disabled@example.com", UserStatus.DISABLED);
        PasswordReset disabledReset = PasswordReset.builder()
            .user(disabledUser)
            .tokenHash(TokenHashUtil.sha256Hex("disabled-token"))
            .expiresAt(vnTime.now().plusMinutes(20))
            .usedAt(null)
            .build();
        when(passwordResetRepository.findByTokenHashForUpdate(disabledReset.getTokenHash()))
            .thenReturn(Optional.of(disabledReset));
        when(userRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(disabledUser));

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new ResetPasswordRequest("disabled-token", "NewPassword123")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage(PasswordResetService.INVALID_OR_EXPIRED_TOKEN_MESSAGE);

        verify(refreshTokenRepository, never()).revokeAllByUserId(any(), any());
    }

    @Test
    @DisplayName("US-F1.5.1 issue: phát hành token theo userId và thời hạn 72 giờ cho S2-36")
    void phatHanhTokenTheoUserIdVaThoiHan72GioChoS236() {
        User user = createUser(30L, "newmember@example.com", UserStatus.ACTIVE);
        when(userRepository.findById(30L)).thenReturn(Optional.of(user));
        when(passwordResetRepository.save(any(PasswordReset.class))).thenAnswer(inv -> inv.getArgument(0));

        String rawToken = passwordResetService.issue(30L, Duration.ofHours(72));

        ArgumentCaptor<PasswordReset> captor = ArgumentCaptor.forClass(PasswordReset.class);
        verify(passwordResetRepository).save(captor.capture());
        PasswordReset saved = captor.getValue();

        assertThat(rawToken).isNotBlank();
        assertThat(saved.getTokenHash()).isEqualTo(TokenHashUtil.sha256Hex(rawToken));
        assertThat(saved.getExpiresAt()).isEqualTo(vnTime.now().plusHours(72));
        assertThat(saved.getUsedAt()).isNull();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> passwordResetService.issue(999L, Duration.ofHours(72)))
            .isInstanceOf(NotFoundException.class);
    }

    private User createUser(Long id, String email, UserStatus status) {
        User user = User.builder()
            .email(email)
            .passwordHash("$2a$10$oldEncodedHash")
            .role(UserRole.MEMBER)
            .status(status)
            .failedLoginCount(0)
            .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private String extractTokenFromBody(String body) {
        String marker = "/dat-lai-mat-khau?token=";
        int start = body.indexOf(marker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        String after = body.substring(start + marker.length());
        int end = after.length();
        for (int i = 0; i < after.length(); i++) {
            if (Character.isWhitespace(after.charAt(i))) {
                end = i;
                break;
            }
        }
        return after.substring(0, end);
    }
}
