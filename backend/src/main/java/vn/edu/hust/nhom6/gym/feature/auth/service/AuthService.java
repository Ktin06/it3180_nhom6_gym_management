package vn.edu.hust.nhom6.gym.feature.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.UnauthorizedException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.common.util.PhoneUtils;
import vn.edu.hust.nhom6.gym.feature.auth.dto.AuthMeResponse;
import vn.edu.hust.nhom6.gym.feature.auth.dto.ChangePasswordRequest;
import vn.edu.hust.nhom6.gym.feature.auth.dto.LoginRequest;
import vn.edu.hust.nhom6.gym.feature.auth.dto.RegisterRequest;
import vn.edu.hust.nhom6.gym.feature.auth.entity.RefreshToken;
import vn.edu.hust.nhom6.gym.feature.auth.entity.User;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserStatus;
import vn.edu.hust.nhom6.gym.feature.auth.repository.RefreshTokenRepository;
import vn.edu.hust.nhom6.gym.feature.auth.repository.UserRepository;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;

/**
 * Xử lý nghiệp vụ xác thực người dùng: đăng ký, đăng nhập, làm mới và đăng xuất (F1).
 */
@Service
public class AuthService {

    public static final int MAX_PASSWORD_BYTES = 72;
    public static final String PASSWORD_MAX_BYTES_MESSAGE = "Mật khẩu không được vượt quá 72 byte";
    public static final int MAX_FAILED = 5;
    public static final int LOCK_MINUTES = 15;

    /**
     * Mã băm BCrypt giả (cost 10) dùng để cân bằng thời gian phản hồi khi đăng nhập với email không tồn tại,
     * tránh lộ thông tin tài khoản qua timing attack.
     */
    private static final String DUMMY_PASSWORD_HASH =
        "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final MemberService memberService;
    private final VnTime vnTime;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenService jwtTokenService,
            PasswordEncoder passwordEncoder,
            MemberService memberService,
            VnTime vnTime) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.memberService = memberService;
        this.vnTime = vnTime;
    }

    /** Kết quả xác thực chứa JWT Access Token và Refresh Token thô (để đưa vào cookie). */
    public record AuthResult(String accessToken, String rawRefreshToken) {
    }

    /**
     * Đăng ký tài khoản hội viên mới và hồ sơ hội viên gắn liền (UC-F1.1).
     */
    @Transactional
    public AuthResult register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email đã được đăng ký trong hệ thống.");
        }

        validatePasswordLength(request.password());

        String normalizedPhone = PhoneUtils.normalizePhone(request.phone());

        User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .failedLoginCount(0)
            .build();
        user = userRepository.save(user);

        // Gọi thẳng service F2 (ADR 0007) trong cùng giao dịch; trùng SĐT ném 409 và hoàn tác cả tài khoản
        memberService.registerMemberProfile(
            user.getId(), request.fullName().trim(), normalizedPhone, request.dob(), request.gender());

        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String rawRefreshToken = issueRefreshToken(user);

        return new AuthResult(accessToken, rawRefreshToken);
    }

    /**
     * Kiểm tra độ dài mật khẩu không vượt quá giới hạn 72 byte của BCrypt
     * (dùng chung cho đăng ký, đổi mật khẩu và đặt lại mật khẩu).
     */
    public void validatePasswordLength(String password) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new BusinessRuleException(PASSWORD_MAX_BYTES_MESSAGE);
        }
    }

    /**
     * Đăng nhập bằng email và mật khẩu (UC-F1.2).
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResult login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        String rawPassword = request.password();
        boolean passwordTooLong = rawPassword == null
            || rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
        String passwordToMatch = passwordTooLong ? "dummy-password-under-72-bytes" : rawPassword;

        // Bước 1: Tìm user và khóa dòng để nhiều request đồng thời tăng đúng bộ đếm
        User user = userRepository.findByEmailForUpdate(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(passwordToMatch, DUMMY_PASSWORD_HASH);
            throw new UnauthorizedException("Email hoặc mật khẩu không chính xác.");
        }

        OffsetDateTime now = vnTime.now();

        // Bước 2: Đang khóa tạm thì trả 403 E2 kèm số phút còn lại, không tăng bộ đếm (G12)
        rejectIfTemporarilyLocked(user, now);

        // Bước 3: So mật khẩu (luôn chạy passwordEncoder.matches); sai thì tăng bộ đếm rồi trả 401
        boolean passwordMatched = passwordEncoder.matches(passwordToMatch, user.getPasswordHash());
        if (passwordTooLong || !passwordMatched) {
            recordFailedPassword(user, now);
            throw new UnauthorizedException("Email hoặc mật khẩu không chính xác.");
        }

        // Bước 4: Mật khẩu đúng nhưng trạng thái tài khoản không ACTIVE thì trả 403 E3
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("Tài khoản đã bị khóa hoặc ngừng hoạt động. Vui lòng liên hệ quản trị viên.");
        }

        // Bước 5: Thành công -> đặt lại bộ đếm và cập nhật last_login_at
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        userRepository.save(user);

        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String rawRefreshToken = issueRefreshToken(user);

        return new AuthResult(accessToken, rawRefreshToken);
    }

    /**
     * Làm mới Access Token và xoay vòng Refresh Token (UC-F1.3).
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Không tìm thấy phiên làm mới.");
        }

        String tokenHash = TokenHashUtil.sha256Hex(rawRefreshToken);
        User user = userRepository.findByRefreshTokenHashForUpdate(tokenHash)
            .orElseThrow(() -> new UnauthorizedException("Phiên đăng nhập đã hết hạn hoặc không hợp lệ."));
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
            .orElseThrow(() -> new UnauthorizedException("Phiên đăng nhập đã hết hạn hoặc không hợp lệ."));

        if (refreshToken.getRevokedAt() != null) {
            refreshTokenRepository.revokeAllByUserId(user.getId(), OffsetDateTime.now());
            throw new UnauthorizedException("Phiên đăng nhập đã hết hạn hoặc không hợp lệ.");
        }

        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Phiên đăng nhập đã hết hạn hoặc không hợp lệ.");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Phiên đăng nhập đã hết hạn hoặc không hợp lệ.");
        }

        // Xoay vòng refresh token: thu hồi token cũ ngay lập tức (S2, ADR 0008)
        refreshToken.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(refreshToken);

        String newAccessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String newRawRefreshToken = issueRefreshToken(user);

        return new AuthResult(newAccessToken, newRawRefreshToken);
    }

    /**
     * Đọc thông tin tài khoản hiện tại từ JWT đã xác thực.
     */
    public AuthMeResponse getCurrentUser(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new UnauthorizedException("Token không hợp lệ.");
        }
        try {
            Long userId = Long.valueOf(jwt.getSubject());
            String email = jwt.getClaimAsString("email");
            String roleClaim = jwt.getClaimAsString("role");
            if (roleClaim == null || roleClaim.isBlank()) {
                throw new UnauthorizedException("Token không hợp lệ.");
            }
            UserRole role = UserRole.valueOf(roleClaim.trim());
            return new AuthMeResponse(userId, email, role);
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("Token không hợp lệ.");
        }
    }

    /**
     * Đăng xuất và thu hồi Refresh Token (UC-F1.3).
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        logoutForUser(rawRefreshToken, null);
    }

    /**
     * Đăng xuất có đối chiếu chủ sở hữu khi có JWT đi kèm (#112).
     *
     * <p>Nếu người dùng đang đăng nhập ở tab hiện tại khác chủ sở hữu của refresh token trong cookie dùng chung
     * (tab khác đã đăng nhập tài khoản khác), backend giữ nguyên phiên của tài khoản trong cookie và trả
     * {@code false} để controller không xóa cookie chung.
     *
     * @return {@code true} nếu được phép xóa cookie {@code refresh_token} ở trình duyệt
     */
    @Transactional
    public boolean logoutForUser(String rawRefreshToken, Long authenticatedUserId) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return true;
        }
        String tokenHash = TokenHashUtil.sha256Hex(rawRefreshToken);
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHashForUpdate(tokenHash);
        if (found.isEmpty()) {
            return true;
        }
        RefreshToken token = found.get();
        if (authenticatedUserId != null
                && token.getUser() != null
                && !authenticatedUserId.equals(token.getUser().getId())) {
            return false;
        }
        if (token.getRevokedAt() == null) {
            token.setRevokedAt(OffsetDateTime.now());
            refreshTokenRepository.save(token);
        }
        return true;
    }

    /**
     * Đổi mật khẩu và thu hồi toàn bộ các phiên đăng nhập hiện có của người dùng (UC-F1.4).
     *
     * <p>Nhập sai mật khẩu hiện tại dùng chung bộ đếm và khóa tạm với đăng nhập (S4, AC-F1.4.1-05), để người cầm
     * access token không thử mật khẩu được không giới hạn. Vì vậy giao dịch không rollback khi trả 422: bộ đếm phải
     * được lưu. Thành công thì đặt lại bộ đếm và khóa tạm như đăng nhập thành công (AC-F1.4.1-06).
     */
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public void changePassword(CurrentUser me, ChangePasswordRequest request) {
        User user = userRepository.findByIdForUpdate(me.userId())
            .orElseThrow(() -> new UnauthorizedException("Token không hợp lệ."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("Tài khoản đã bị khóa hoặc ngừng hoạt động. Vui lòng liên hệ quản trị viên.");
        }

        OffsetDateTime now = vnTime.now();
        rejectIfTemporarilyLocked(user, now);

        validatePasswordLength(request.newPassword());

        String currentPassword = request.currentPassword();
        boolean currentTooLong = currentPassword == null
            || currentPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
        if (currentTooLong || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            recordFailedPassword(user, now);
            throw new BusinessRuleException("Mật khẩu hiện tại không đúng.");
        }

        if (currentPassword.equals(request.newPassword())) {
            throw new BusinessRuleException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(me.userId(), now);
    }

    /** Đang khóa tạm (S4) thì trả 403 kèm số phút còn lại, không tăng bộ đếm (G12). */
    private static void rejectIfTemporarilyLocked(User user, OffsetDateTime now) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long remainingSeconds = Duration.between(now, user.getLockedUntil()).toSeconds();
            long remainingMinutes = Math.max(1L, (long) Math.ceil(remainingSeconds / 60.0));
            throw new ForbiddenException(
                "Tài khoản đang bị khóa tạm thời do nhập sai mật khẩu nhiều lần. Vui lòng thử lại sau "
                    + remainingMinutes + " phút.");
        }
    }

    /**
     * Ghi một lần nhập sai mật khẩu (đăng nhập hoặc đổi mật khẩu): khóa cũ đã hết hạn thì đếm lại từ 1; đủ
     * {@link #MAX_FAILED} lần thì khóa tạm {@link #LOCK_MINUTES} phút (S4).
     */
    private void recordFailedPassword(User user, OffsetDateTime now) {
        boolean hadExpiredLock = user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now);
        int nextFailedCount = hadExpiredLock ? 1 : user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(nextFailedCount);
        if (nextFailedCount >= MAX_FAILED) {
            user.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
        } else if (hadExpiredLock) {
            user.setLockedUntil(null);
        }
        userRepository.save(user);
    }

    private String issueRefreshToken(User user) {
        String rawToken = TokenHashUtil.generateRawToken();
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);

        RefreshToken entity = RefreshToken.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(OffsetDateTime.now().plus(AuthCookieHelper.REFRESH_TOKEN_TTL))
            .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }
}
