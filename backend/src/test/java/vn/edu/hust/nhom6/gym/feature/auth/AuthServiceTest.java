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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthService;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.auth.service.TokenHashUtil;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;

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

    @Mock
    private VnTime vnTime;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository,
            refreshTokenRepository,
            jwtTokenService,
            passwordEncoder,
            memberService,
            vnTime
        );
    }

    @Test
    @DisplayName("TC-F1.1.1-01b: đăng ký thành công tạo user, chuẩn hóa SĐT và gọi tạo hồ sơ hội viên")
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
    @DisplayName("TC-F1.1.1-03a: đăng ký email đã tồn tại ném 409 Conflict")
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
    @DisplayName("TC-F1.1.1-04g: đăng ký SĐT sai định dạng bị từ chối ở tầng dịch vụ")
    void dangKySoDienThoaiSaiDinhDangNemBusinessRuleException() {
        RegisterRequest request = new RegisterRequest(
            "Nguyễn Văn A", "member@example.com", "12345", "Password123", null, null);

        when(userRepository.existsByEmail("member@example.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Số điện thoại không hợp lệ");
    }

    @Test
    @DisplayName("TC-F1.1.1-04h: đăng ký mật khẩu vượt 72 byte UTF-8 bị từ chối")
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
    @DisplayName("TC-F1.2.1-04e: đăng nhập đúng cấp token và cập nhật last_login_at")
    void dangNhapThanhCongCapTokenVaCapNhatLastLoginAt() {
        LoginRequest request = new LoginRequest("member@example.com", "Password123");
        OffsetDateTime fixedNow = OffsetDateTime.parse("2026-11-20T03:00:00Z");
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();

        when(vnTime.now()).thenReturn(fixedNow);
        when(userRepository.findByEmailForUpdate("member@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashed-pwd")).thenReturn(true);
        when(jwtTokenService.generateAccessToken(any(), eq("member@example.com"), eq(UserRole.MEMBER)))
            .thenReturn("mock-access-token");

        AuthService.AuthResult result = authService.login(request);

        assertThat(result.accessToken()).isEqualTo("mock-access-token");
        assertThat(result.rawRefreshToken()).isNotBlank();
        assertThat(user.getLastLoginAt()).isEqualTo(fixedNow);
        verify(userRepository).save(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("TC-F1.2.1-01e: đăng nhập sai mật khẩu trả 401 câu chung")
    void dangNhapSaiMatKhauNemUnauthorizedException() {
        LoginRequest request = new LoginRequest("member@example.com", "WrongPassword");
        OffsetDateTime fixedNow = OffsetDateTime.parse("2026-11-20T03:00:00Z");
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();

        when(vnTime.now()).thenReturn(fixedNow);
        when(userRepository.findByEmailForUpdate("member@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed-pwd")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Email hoặc mật khẩu không chính xác");
    }

    @Test
    @DisplayName("TC-F1.2.1-01f: đăng nhập mật khẩu vượt 72 byte trả 401 không ném 500")
    void dangNhapMatKhauVuotQua72ByteNemUnauthorizedExceptionKhongNem500() {
        String longPassword = "ă".repeat(30) + "Pass1234567890123456";
        LoginRequest request = new LoginRequest("member@example.com", longPassword);
        OffsetDateTime fixedNow = OffsetDateTime.parse("2026-11-20T03:00:00Z");
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();

        when(vnTime.now()).thenReturn(fixedNow);
        when(userRepository.findByEmailForUpdate("member@example.com")).thenReturn(Optional.of(user));
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

        assertThat(user.getFailedLoginCount()).isEqualTo(1);
        verify(userRepository).save(user);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-F1.2.1-01g: email không tồn tại vẫn chạy BCrypt và trả 401 câu chung")
    void dangNhapEmailKhongTonTaiVanChayPasswordEncoderMatchesDeChongDoanEmailQuaThoiGian() {
        LoginRequest request = new LoginRequest("unknown@example.com", "Password123");

        when(userRepository.findByEmailForUpdate("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Email hoặc mật khẩu không chính xác");

        verify(passwordEncoder).matches(eq("Password123"), anyString());
    }

    @Test
    @DisplayName("TC-F1.2.1-05b: tài khoản bị khóa nhập đúng mật khẩu bị từ chối 403")
    void dangNhapTaiKhoanBiKhoaNemForbiddenException() {
        LoginRequest request = new LoginRequest("member@example.com", "Password123");
        OffsetDateTime fixedNow = OffsetDateTime.parse("2026-11-20T03:00:00Z");
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.LOCKED)
            .build();

        when(vnTime.now()).thenReturn(fixedNow);
        when(userRepository.findByEmailForUpdate("member@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashed-pwd")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Tài khoản đã bị khóa");
    }

    @Test
    @DisplayName("TC-F1.3.1-01b: làm mới phiên xoay vòng refresh token và thu hồi token cũ")
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

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(existingToken));
        when(userRepository.findByRefreshTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(user));
        when(jwtTokenService.generateAccessToken(any(), eq("member@example.com"), eq(UserRole.MEMBER)))
            .thenReturn("new-access-token");

        AuthService.AuthResult result = authService.refresh(rawToken);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.rawRefreshToken()).isNotBlank();
        assertThat(existingToken.getRevokedAt()).isNotNull();

        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("TC-F1.3.1-01c, TC-F1.3.1-03b: dùng lại token đã thu hồi thu hồi toàn bộ phiên và trả 401")
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

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(existingToken));
        when(userRepository.findByRefreshTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh(rawToken))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Phiên đăng nhập đã hết hạn hoặc không hợp lệ");

        verify(refreshTokenRepository).revokeAllByUserId(eq(77L), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("TC-F1.3.1-03c: làm mới bằng refresh token hết hạn trả 401")
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

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(existingToken));
        when(userRepository.findByRefreshTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh(rawToken))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Phiên đăng nhập đã hết hạn hoặc không hợp lệ");

        verify(refreshTokenRepository, never()).revokeAllByUserId(any(), any());
    }

    @Test
    @DisplayName("TC-F1.3.1-02d: đăng xuất đánh dấu thu hồi refresh token")
    void dangXuatThuHoiRefreshToken() {
        String rawToken = "valid-token-to-logout";
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);

        RefreshToken existingToken = RefreshToken.builder()
            .tokenHash(tokenHash)
            .expiresAt(OffsetDateTime.now().plusDays(5))
            .revokedAt(null)
            .build();

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(existingToken));

        authService.logout(rawToken);

        assertThat(existingToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(existingToken);
    }

    @Test
    @DisplayName("TC-F1.3.1-04c: đăng xuất với JWT không thu hồi cookie của tài khoản khác")
    void dangXuatVoiJwtKhongThuHoiCookieCuaTaiKhoanKhac() {
        String rawToken = "user-b-refresh-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);

        User userB = User.builder()
            .email("userb@example.com")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(userB, "id", 200L);

        RefreshToken tokenB = RefreshToken.builder()
            .user(userB)
            .tokenHash(tokenHash)
            .expiresAt(OffsetDateTime.now().plusDays(5))
            .revokedAt(null)
            .build();

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(tokenB));

        boolean shouldClearCookie = authService.logoutForUser(rawToken, 100L);

        assertThat(shouldClearCookie).isFalse();
        assertThat(tokenB.getRevokedAt()).isNull();
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-F1.3.1-02e: đăng xuất với JWT thu hồi cookie khi cùng chủ sở hữu")
    void dangXuatVoiJwtThuHoiCookieKhiCungChuSoHuu() {
        String rawToken = "user-a-refresh-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);

        User userA = User.builder()
            .email("usera@example.com")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(userA, "id", 100L);

        RefreshToken tokenA = RefreshToken.builder()
            .user(userA)
            .tokenHash(tokenHash)
            .expiresAt(OffsetDateTime.now().plusDays(5))
            .revokedAt(null)
            .build();

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(tokenA));

        boolean shouldClearCookie = authService.logoutForUser(rawToken, 100L);

        assertThat(shouldClearCookie).isTrue();
        assertThat(tokenA.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(tokenA);
    }

    @Test
    @DisplayName("TC-F1.6.1-01a: getCurrentUser đọc JWT hợp lệ và ném 401 khi claim không hợp lệ")
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
    @DisplayName("BR-F2-06: chuẩn hóa số điện thoại Việt Nam về 10 chữ số bắt đầu bằng 0")
    void chuanHoaSoDienThoaiCacDangPhoBien() {
        assertThat(PhoneUtils.normalizePhone("0912345678")).isEqualTo("0912345678");
        assertThat(PhoneUtils.normalizePhone("+84 912 345 678")).isEqualTo("0912345678");
        assertThat(PhoneUtils.normalizePhone("84912345678")).isEqualTo("0912345678");
        assertThat(PhoneUtils.normalizePhone("0912.345.678")).isEqualTo("0912345678");
        assertThat(PhoneUtils.normalizePhone("0912-345-678")).isEqualTo("0912345678");
    }

    @Test
    @DisplayName("TC-F1.2.1-01a: sai mật khẩu lần 1 đến 4 trả 401 câu chung và tăng bộ đếm")
    void saiMatKhauLan1Den4Tra401CauChungVaTangBoDem() {
        LoginRequest request = new LoginRequest("member@example.com", "WrongPass123");
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .failedLoginCount(0)
            .build();
        OffsetDateTime fixedNow = OffsetDateTime.parse("2026-11-20T03:00:00Z");

        when(vnTime.now()).thenReturn(fixedNow);
        when(userRepository.findByEmailForUpdate("member@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass123", "hashed-pwd")).thenReturn(false);

        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Email hoặc mật khẩu không chính xác.");

            assertThat(user.getFailedLoginCount()).isEqualTo(attempt);
            assertThat(user.getLockedUntil()).isNull();
        }
    }

    @Test
    @DisplayName("TC-F1.2.1-02a: lần sai thứ 5 khóa 15 phút")
    void lanSaiThu5Khoa15Phut() {
        LoginRequest request = new LoginRequest("member@example.com", "WrongPass123");
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .failedLoginCount(4)
            .build();
        OffsetDateTime fixedNow = OffsetDateTime.parse("2026-11-20T03:00:00Z");

        when(vnTime.now()).thenReturn(fixedNow);
        when(userRepository.findByEmailForUpdate("member@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass123", "hashed-pwd")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Email hoặc mật khẩu không chính xác.");

        assertThat(user.getFailedLoginCount()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isEqualTo(fixedNow.plusMinutes(15));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("TC-F1.2.1-03a: đang khóa nhập đúng mật khẩu vẫn trả 403 kèm thời gian còn lại")
    void dangKhoaNhapDungMatKhauVanTra403KemThoiGianConLai() {
        LoginRequest request = new LoginRequest("member@example.com", "Password123");
        OffsetDateTime fixedNow = OffsetDateTime.parse("2026-11-20T03:00:00Z");
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .failedLoginCount(5)
            .lockedUntil(fixedNow.plusMinutes(14).plusSeconds(10))
            .build();

        when(vnTime.now()).thenReturn(fixedNow);
        when(userRepository.findByEmailForUpdate("member@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("Tài khoản đang bị khóa tạm thời do nhập sai mật khẩu nhiều lần. Vui lòng thử lại sau 15 phút.");

        assertThat(user.getFailedLoginCount()).isEqualTo(5);
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-F1.2.1-04a: hết khóa thì đăng nhập lại được")
    void hetKhoaThiDangNhapLaiDuoc() {
        LoginRequest request = new LoginRequest("member@example.com", "Password123");
        OffsetDateTime lockTime = OffsetDateTime.parse("2026-11-20T03:00:00Z");
        OffsetDateTime afterLock = lockTime.plusMinutes(15).plusSeconds(1);
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .failedLoginCount(5)
            .lockedUntil(lockTime.plusMinutes(15))
            .build();

        when(vnTime.now()).thenReturn(afterLock);
        when(userRepository.findByEmailForUpdate("member@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashed-pwd")).thenReturn(true);
        when(jwtTokenService.generateAccessToken(any(), eq("member@example.com"), eq(UserRole.MEMBER)))
            .thenReturn("access-token-after-unlock");

        AuthService.AuthResult result = authService.login(request);

        assertThat(result.accessToken()).isEqualTo("access-token-after-unlock");
        assertThat(user.getFailedLoginCount()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLastLoginAt()).isEqualTo(afterLock);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("TC-F1.2.1-04b: đăng nhập đúng đặt lại bộ đếm về 0 và xóa locked_until")
    void dangNhapDungDatLaiBoDemVe0VaXoaLockedUntil() {
        LoginRequest request = new LoginRequest("member@example.com", "Password123");
        OffsetDateTime fixedNow = OffsetDateTime.parse("2026-11-20T03:00:00Z");
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .failedLoginCount(3)
            .lockedUntil(null)
            .build();

        when(vnTime.now()).thenReturn(fixedNow);
        when(userRepository.findByEmailForUpdate("member@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashed-pwd")).thenReturn(true);
        when(jwtTokenService.generateAccessToken(any(), eq("member@example.com"), eq(UserRole.MEMBER)))
            .thenReturn("mock-access-token");

        AuthService.AuthResult result = authService.login(request);

        assertThat(result.accessToken()).isEqualTo("mock-access-token");
        assertThat(user.getFailedLoginCount()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLastLoginAt()).isEqualTo(fixedNow);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("TC-F1.2.1-01b: không lộ email khi email không tồn tại")
    void khongLoEmailKhiEmailKhongTonTai() {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "Password123");

        when(userRepository.findByEmailForUpdate("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Email hoặc mật khẩu không chính xác.");

        verify(passwordEncoder).matches(eq("Password123"), anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-F1.2.1-01c: tài khoản DISABLED nhập sai mật khẩu trả 401, không phải 403")
    void taiKhoanDisabledNhapSaiMatKhauTra401KhongPhai403() {
        LoginRequest request = new LoginRequest("disabled@example.com", "WrongPass123");
        OffsetDateTime fixedNow = OffsetDateTime.parse("2026-11-20T03:00:00Z");
        User user = User.builder()
            .email("disabled@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.DISABLED)
            .failedLoginCount(0)
            .build();

        when(vnTime.now()).thenReturn(fixedNow);
        when(userRepository.findByEmailForUpdate("disabled@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass123", "hashed-pwd")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Email hoặc mật khẩu không chính xác.");

        assertThat(user.getFailedLoginCount()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("TC-F1.2.1-04c: hết khóa rồi nhập sai thì bộ đếm tính lại từ 1 và không khóa lại ngay")
    void hetKhoaRoiNhapSaiThiBoDemTinhLaiTu1() {
        LoginRequest request = new LoginRequest("member@example.com", "WrongPass123");
        OffsetDateTime lockTime = OffsetDateTime.parse("2026-11-20T03:00:00Z");
        OffsetDateTime afterLock = lockTime.plusMinutes(15).plusSeconds(1);
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("hashed-pwd")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .failedLoginCount(5)
            .lockedUntil(lockTime.plusMinutes(15))
            .build();

        when(vnTime.now()).thenReturn(afterLock);
        when(userRepository.findByEmailForUpdate("member@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass123", "hashed-pwd")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Email hoặc mật khẩu không chính xác.");

        assertThat(user.getFailedLoginCount()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("TC-F1.4.1-01c: đổi mật khẩu thành công cập nhật password_hash")
    void doiMatKhauThanhCongCapNhatPasswordHash() {
        CurrentUser me = new CurrentUser(10L, UserRole.MEMBER);
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass123", "NewPass456");
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("old-hash")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();
        OffsetDateTime fixedNow = OffsetDateTime.parse("2026-11-19T16:45:00Z");

        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass456")).thenReturn("new-hash");
        when(vnTime.now()).thenReturn(fixedNow);

        authService.changePassword(me, request);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("TC-F1.4.1-01d: đổi mật khẩu thu hồi mọi phiên của người dùng")
    void doiMatKhauThuHoiMoiPhienCuaNguoiDung() {
        CurrentUser me = new CurrentUser(10L, UserRole.MEMBER);
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass123", "NewPass456");
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("old-hash")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();
        OffsetDateTime fixedNow = OffsetDateTime.parse("2026-11-19T16:45:00Z");

        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass456")).thenReturn("new-hash");
        when(vnTime.now()).thenReturn(fixedNow);

        authService.changePassword(me, request);

        verify(refreshTokenRepository).revokeAllByUserId(10L, fixedNow);
    }

    @Test
    @DisplayName("TC-F1.4.1-02a: mật khẩu hiện tại sai hoặc mật khẩu mới trùng mật khẩu cũ trả 422")
    void doiMatKhauHienTaiSaiHoacMatKhauMoiTrungCuNemBusinessRuleException() {
        CurrentUser me = new CurrentUser(10L, UserRole.MEMBER);
        User user = User.builder()
            .email("member@example.com")
            .passwordHash("old-hash")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();

        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass123", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(
                me, new ChangePasswordRequest("WrongPass123", "NewPass456")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Mật khẩu hiện tại không đúng.");

        String currentTooLong = "ă".repeat(30) + "Pass1234567890123456";
        assertThatThrownBy(() -> authService.changePassword(
                me, new ChangePasswordRequest(currentTooLong, "NewPass456")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Mật khẩu hiện tại không đúng.");
        verify(passwordEncoder, never()).matches(eq(currentTooLong), anyString());

        when(passwordEncoder.matches("OldPass123", "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(
                me, new ChangePasswordRequest("OldPass123", "OldPass123")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Mật khẩu mới phải khác mật khẩu hiện tại.");

        // Không đổi mật khẩu, không thu hồi phiên; hai lần sai mật khẩu hiện tại được đếm (AC-F1.4.1-05),
        // lần mật khẩu mới trùng thì không đếm vì mật khẩu hiện tại đúng.
        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
        assertThat(user.getFailedLoginCount()).isEqualTo(2);
        verify(refreshTokenRepository, never()).revokeAllByUserId(any(), any());
    }

    @Test
    @DisplayName("TC-F1.4.1-04e: tài khoản DISABLED hoặc LOCKED không thể đổi mật khẩu và trả 403")
    void doiMatKhauTaiKhoanKhongActiveNemForbiddenException() {
        CurrentUser me = new CurrentUser(10L, UserRole.MEMBER);
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass123", "NewPass456");

        for (UserStatus nonActive : new UserStatus[] { UserStatus.DISABLED, UserStatus.LOCKED }) {
            User user = User.builder()
                .email("member@example.com")
                .passwordHash("old-hash")
                .role(UserRole.MEMBER)
                .status(nonActive)
                .build();
            when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.changePassword(me, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Tài khoản đã bị khóa hoặc ngừng hoạt động. Vui lòng liên hệ quản trị viên.");
        }

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeAllByUserId(any(), any());
    }
}
