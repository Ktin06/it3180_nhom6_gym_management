package vn.edu.hust.nhom6.gym.feature.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.UnauthorizedException;
import vn.edu.hust.nhom6.gym.common.util.PhoneUtils;
import vn.edu.hust.nhom6.gym.feature.auth.dto.AuthMeResponse;
import vn.edu.hust.nhom6.gym.feature.auth.dto.LoginRequest;
import vn.edu.hust.nhom6.gym.feature.auth.dto.RegisterRequest;
import vn.edu.hust.nhom6.gym.feature.auth.entity.RefreshToken;
import vn.edu.hust.nhom6.gym.feature.auth.entity.User;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserStatus;
import vn.edu.hust.nhom6.gym.feature.auth.repository.RefreshTokenRepository;
import vn.edu.hust.nhom6.gym.feature.auth.repository.UserRepository;

/**
 * Xử lý nghiệp vụ xác thực người dùng: đăng ký, đăng nhập, làm mới và đăng xuất (F1).
 */
@Service
public class AuthService {

    public static final int MAX_PASSWORD_BYTES = 72;
    public static final String PASSWORD_MAX_BYTES_MESSAGE = "Mật khẩu không được vượt quá 72 byte";

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

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenService jwtTokenService,
            PasswordEncoder passwordEncoder,
            MemberService memberService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.memberService = memberService;
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

        // Tạo hồ sơ hội viên qua MemberService (F2) theo ADR 0007; trùng SĐT sẽ ném 409
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
    @Transactional
    public AuthResult login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        String rawPassword = request.password();
        boolean passwordTooLong = rawPassword == null
            || rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
        String passwordToMatch = passwordTooLong ? "dummy-password-under-72-bytes" : rawPassword;

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(passwordToMatch, DUMMY_PASSWORD_HASH);
            throw new UnauthorizedException("Email hoặc mật khẩu không chính xác.");
        }

        boolean passwordMatched = passwordEncoder.matches(passwordToMatch, user.getPasswordHash());
        if (passwordTooLong || !passwordMatched) {
            throw new UnauthorizedException("Email hoặc mật khẩu không chính xác.");
        }

        if (user.getStatus() != UserStatus.ACTIVE
                || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now()))) {
            throw new ForbiddenException("Tài khoản đã bị khóa hoặc ngừng hoạt động. Vui lòng liên hệ quản trị viên.");
        }

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(OffsetDateTime.now());
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
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new UnauthorizedException("Phiên đăng nhập đã hết hạn hoặc không hợp lệ."));

        if (refreshToken.getRevokedAt() != null) {
            if (refreshToken.getUser() != null) {
                refreshTokenRepository.revokeAllByUserId(refreshToken.getUser().getId(), OffsetDateTime.now());
            }
            throw new UnauthorizedException("Phiên đăng nhập đã hết hạn hoặc không hợp lệ.");
        }

        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Phiên đăng nhập đã hết hạn hoặc không hợp lệ.");
        }

        User user = refreshToken.getUser();
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
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = TokenHashUtil.sha256Hex(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                if (token.getRevokedAt() == null) {
                    token.setRevokedAt(OffsetDateTime.now());
                    refreshTokenRepository.save(token);
                }
            });
        }
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
