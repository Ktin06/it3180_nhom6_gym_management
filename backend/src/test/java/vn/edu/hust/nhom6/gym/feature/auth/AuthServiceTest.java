package vn.edu.hust.nhom6.gym.feature.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.crypto.password.PasswordEncoder;

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
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthService;
import vn.edu.hust.nhom6.gym.feature.auth.service.DefaultMemberService;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.auth.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.auth.service.TokenHashUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MemberService memberService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository,
            refreshTokenRepository,
            jwtTokenService,
            passwordEncoder,
            memberService
        );
    }

    @Test
    void dangKyThanhCongTaoUserVaGoiMemberService() {
        RegisterRequest request = new RegisterRequest(
            "Nguyễn Văn A", "member@example.com", "+84 912 345 678", "Password123",
            LocalDate.of(2000, 1, 1), "MALE"
        );

        when(userRepository.existsByEmail("member@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return u;
        });
        when(jwtTokenService.generateAccessToken(any(), eq("member@example.com"), eq(UserRole.MEMBER)))
            .thenReturn("mock-access-token");

        AuthService.AuthResult result = authService.register(request);

        assertThat(result.accessToken()).isEqualTo("mock-access-token");
        assertThat(result.rawRefreshToken()).isNotBlank();

        verify(memberService).registerMemberProfile(
            any(), eq("Nguyễn Văn A"), eq("0912345678"), eq(LocalDate.of(2000, 1, 1)), eq("MALE"));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void dangKyEmailDaTonTaiNemConflictException() {
        RegisterRequest request = new RegisterRequest(
            "Nguyễn Văn A", "duplicate@example.com", "0912345678", "Password123", null, null);

        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Email đã được đăng ký");

        verify(userRepository, never()).save(any());
        verify(memberService, never()).registerMemberProfile(any(), any(), any(), any(), any());
    }

    @Test
    void dangKySoDienThoaiSaiDinhDangNemBusinessRuleException() {
        RegisterRequest request = new RegisterRequest(
            "Nguyễn Văn A", "member@example.com", "12345", "Password123", null, null);

        when(userRepository.existsByEmail("member@example.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Số điện thoại không hợp lệ");
    }

    @Test
    void dangKyMatKhauVuotQua72ByteUtf8NemBusinessRuleException() {
        // 30 ký tự 'ă' (mỗi ký tự 2 byte) + 20 ký tự ASCII = 50 ký tự (<= 72 ký tự) nhưng dài 80 byte (> 72 byte)
        String multiBytePassword = "ă".repeat(30) + "Pass1234567890123456";
        RegisterRequest request = new RegisterRequest(
            "Nguyễn Văn A", "member@example.com", "0912345678", multiBytePassword, null, null);

        when(userRepository.existsByEmail("member@example.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage(AuthService.PASSWORD_MAX_BYTES_MESSAGE);

        verify(userRepository, never()).save(any());
    }

    @Test
    void dangNhapThanhCongCapTokenVaCapNhatLastLoginAt() {
        LoginRequest request = new LoginRequest("member@example.com", "Password123");
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashed-pwd")).thenReturn(true);
        when(jwtTokenService.generateAccessToken(any(), eq("member@example.com"), eq(UserRole.MEMBER)))
            .thenReturn("mock-access-token");

        AuthService.AuthResult result = authService.login(request);

        assertThat(result.accessToken()).isEqualTo("mock-access-token");
        assertThat(result.rawRefreshToken()).isNotBlank();
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(userRepository).save(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void dangNhapSaiMatKhauNemUnauthorizedException() {
        LoginRequest request = new LoginRequest("member@example.com", "WrongPassword");
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed-pwd")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Email hoặc mật khẩu không chính xác");
    }

    @Test
    void dangNhapMatKhauVuotQua72ByteNemUnauthorizedExceptionKhongNem500() {
        String longPassword = "ă".repeat(30) + "Pass1234567890123456";
        LoginRequest request = new LoginRequest("member@example.com", longPassword);
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq("hashed-pwd"))).thenAnswer(invocation -> {
            String raw = invocation.getArgument(0);
            if (raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > AuthService.MAX_PASSWORD_BYTES) {
                throw new IllegalArgumentException("password cannot be more than 72 bytes");
            }
            return true;
        });

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Email hoặc mật khẩu không chính xác");

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void dangNhapEmailKhongTonTaiVanChayPasswordEncoderMatchesDeChongDoanEmailQuaThoiGian() {
        LoginRequest request = new LoginRequest("unknown@example.com", "Password123");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Email hoặc mật khẩu không chính xác");

        verify(passwordEncoder).matches(eq("Password123"), anyString());
    }

    @Test
    void dangNhapTaiKhoanBiKhoaNemForbiddenException() {
        LoginRequest request = new LoginRequest("member@example.com", "Password123");
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.LOCKED)
            .build();

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashed-pwd")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Tài khoản đã bị khóa");
    }

    @Test
    void lamMoiThanhCongXoayVongRefreshToken() {
        String rawToken = "raw-refresh-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);

        User user = User.builder()
            .email("member@example.com")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();

        RefreshToken existingToken = RefreshToken.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(OffsetDateTime.now().plusDays(5))
            .revokedAt(null)
            .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(existingToken));
        when(jwtTokenService.generateAccessToken(any(), eq("member@example.com"), eq(UserRole.MEMBER)))
            .thenReturn("new-access-token");

        AuthService.AuthResult result = authService.refresh(rawToken);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.rawRefreshToken()).isNotBlank();
        assertThat(existingToken.getRevokedAt()).isNotNull();

        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any(RefreshToken.class));
    }

    @Test
    void lamMoiTokenDaBiThuHoiThuHoiToanBoPhienVaNemUnauthorizedException() {
        String rawToken = "revoked-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);

        User user = User.builder()
            .email("member@example.com")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 77L);

        RefreshToken existingToken = RefreshToken.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(OffsetDateTime.now().plusDays(5))
            .revokedAt(OffsetDateTime.now().minusHours(1))
            .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(existingToken));

        assertThatThrownBy(() -> authService.refresh(rawToken))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Phiên đăng nhập đã hết hạn hoặc không hợp lệ");

        verify(refreshTokenRepository).revokeAllByUserId(eq(77L), any(OffsetDateTime.class));
    }

    @Test
    void lamMoiTokenHetHanNemUnauthorizedException() {
        String rawToken = "expired-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);

        User user = User.builder()
            .email("member@example.com")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 77L);

        RefreshToken existingToken = RefreshToken.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(OffsetDateTime.now().minusDays(1))
            .revokedAt(null)
            .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(existingToken));

        assertThatThrownBy(() -> authService.refresh(rawToken))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Phiên đăng nhập đã hết hạn hoặc không hợp lệ");

        verify(refreshTokenRepository, never()).revokeAllByUserId(any(), any());
    }

    @Test
    void dangXuatThuHoiRefreshToken() {
        String rawToken = "valid-token-to-logout";
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);

        RefreshToken existingToken = RefreshToken.builder()
            .tokenHash(tokenHash)
            .expiresAt(OffsetDateTime.now().plusDays(5))
            .revokedAt(null)
            .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(existingToken));

        authService.logout(rawToken);

        assertThat(existingToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(existingToken);
    }

    @Test
    void getCurrentUserDocThongTinTuJwtVaNem401KhiClaimKhongHopLe() {
        org.springframework.security.oauth2.jwt.Jwt validJwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("tok")
            .header("alg", "HS256")
            .subject("25")
            .claim("email", "staff@example.com")
            .claim("role", "RECEPTIONIST")
            .build();

        AuthMeResponse me = authService.getCurrentUser(validJwt);
        assertThat(me.userId()).isEqualTo(25L);
        assertThat(me.email()).isEqualTo("staff@example.com");
        assertThat(me.role()).isEqualTo(UserRole.RECEPTIONIST);

        org.springframework.security.oauth2.jwt.Jwt badRoleJwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("tok")
            .header("alg", "HS256")
            .subject("25")
            .claim("email", "hacker@example.com")
            .claim("role", "HACKER")
            .build();

        assertThatThrownBy(() -> authService.getCurrentUser(badRoleJwt))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void chuanHoaSoDienThoaiCacDangPhoBien() {
        assertThat(PhoneUtils.normalizePhone("0912345678")).isEqualTo("0912345678");
        assertThat(PhoneUtils.normalizePhone("+84 912 345 678")).isEqualTo("0912345678");
        assertThat(PhoneUtils.normalizePhone("84912345678")).isEqualTo("0912345678");
        assertThat(PhoneUtils.normalizePhone("0912.345.678")).isEqualTo("0912345678");
        assertThat(PhoneUtils.normalizePhone("0912-345-678")).isEqualTo("0912345678");
    }

    @Test
    void defaultMemberServiceTuDongDangKyKhiChuaCoF2VaNhuongQuyenKhiCoF2() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ScanOnlyDefaultConfig.class)) {
            assertThat(ctx.getBean(MemberService.class)).isInstanceOf(DefaultMemberService.class);
        }

        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                ScanOnlyDefaultConfig.class, CustomMemberServiceStub.class)) {
            assertThat(ctx.getBean(MemberService.class)).isInstanceOf(CustomMemberServiceStub.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
        basePackageClasses = DefaultMemberService.class,
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = DefaultMemberService.class)
    )
    static class ScanOnlyDefaultConfig {
    }

    static class CustomMemberServiceStub implements MemberService {
        @Override
        public void registerMemberProfile(Long userId, String fullName, String phone, LocalDate dob, String gender) {
        }
    }
}
