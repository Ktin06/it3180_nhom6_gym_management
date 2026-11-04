package vn.edu.hust.nhom6.gym.feature.auth.service;

import java.time.OffsetDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.exception.UnauthorizedException;
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

        String normalizedPhone = normalizePhone(request.phone());

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
     * Đăng nhập bằng email và mật khẩu (UC-F1.2).
     */
    @Transactional
    public AuthResult login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không chính xác."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email hoặc mật khẩu không chính xác.");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Tài khoản đã bị khóa hoặc tạm ngừng hoạt động.");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String rawRefreshToken = issueRefreshToken(user);

        return new AuthResult(accessToken, rawRefreshToken);
    }

    /**
     * Làm mới Access Token và xoay vòng Refresh Token (UC-F1.3).
     */
    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Không tìm thấy phiên làm mới.");
        }

        String tokenHash = TokenHashUtil.sha256Hex(rawRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new UnauthorizedException("Phiên đăng nhập đã hết hạn hoặc không hợp lệ."));

        if (refreshToken.getRevokedAt() != null || refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Phiên đăng nhập đã hết hạn hoặc không hợp lệ.");
        }

        User user = refreshToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Tài khoản đã bị khóa hoặc tạm ngừng hoạt động.");
        }

        // Xoay vòng refresh token: thu hồi token cũ ngay lập tức (S2, ADR 0008)
        refreshToken.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(refreshToken);

        String newAccessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String newRawRefreshToken = issueRefreshToken(user);

        return new AuthResult(newAccessToken, newRawRefreshToken);
    }

    /**
     * Đăng xuất và thu hồi Refresh Token (UC-F1.4).
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

    /**
     * Chuẩn hóa số điện thoại về 10 chữ số bắt đầu bằng số 0 (ví dụ {@code +84 912 345 678} thành {@code 0912345678}).
     */
    public static String normalizePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new BusinessRuleException("Số điện thoại không được để trống.");
        }
        String cleaned = rawPhone.replaceAll("[\\s\\-\\.]", "");
        if (cleaned.startsWith("+84")) {
            cleaned = "0" + cleaned.substring(3);
        } else if (cleaned.startsWith("84") && cleaned.length() == 11) {
            cleaned = "0" + cleaned.substring(2);
        }
        if (!cleaned.matches("^0\\d{9}$")) {
            throw new BusinessRuleException("Số điện thoại không hợp lệ. Phải gồm 10 chữ số bắt đầu bằng số 0.");
        }
        return cleaned;
    }
}
