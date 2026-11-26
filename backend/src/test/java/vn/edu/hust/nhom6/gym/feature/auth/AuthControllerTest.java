package vn.edu.hust.nhom6.gym.feature.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.GlobalExceptionHandler;
import vn.edu.hust.nhom6.gym.common.exception.UnauthorizedException;
import vn.edu.hust.nhom6.gym.config.SecurityConfig;
import vn.edu.hust.nhom6.gym.feature.auth.controller.AuthController;
import vn.edu.hust.nhom6.gym.feature.auth.dto.AuthMeResponse;
import vn.edu.hust.nhom6.gym.feature.auth.dto.ForgotPasswordRequest;
import vn.edu.hust.nhom6.gym.feature.auth.dto.LoginRequest;
import vn.edu.hust.nhom6.gym.feature.auth.dto.RegisterRequest;
import vn.edu.hust.nhom6.gym.feature.auth.dto.ResetPasswordRequest;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthCookieHelper;
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthService;
import vn.edu.hust.nhom6.gym.feature.auth.service.PasswordResetService;

@WebMvcTest(controllers = AuthController.class)
@ContextConfiguration(classes = {
    AuthController.class,
    SecurityConfig.class,
    GlobalExceptionHandler.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private AuthCookieHelper cookieHelper;

    @BeforeEach
    void setUp() {
        when(cookieHelper.createRefreshCookie(any())).thenAnswer(invocation ->
            ResponseCookie.from("refresh_token", invocation.getArgument(0))
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth")
                .sameSite("Strict")
                .maxAge(Duration.ofDays(7))
                .build());

        when(cookieHelper.createCleanRefreshCookie()).thenReturn(
            ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth")
                .sameSite("Strict")
                .maxAge(0)
                .build());
    }

    @Test
    @DisplayName("TC-F1.1.1-01a: đăng ký thành công trả 201 kèm cookie refresh_token và accessToken")
    void dangKyThanhCongTra201KemCookieVaAccessToken() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "Nguyễn Văn A", "member@example.com", "0912345678", "Password123", null, null);

        when(authService.register(any(RegisterRequest.class)))
            .thenReturn(new AuthService.AuthResult("access-token-123", "refresh-token-xyz"));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").value("access-token-123"))
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refresh_token=")));
    }

    @Test
    @DisplayName("TC-F1.1.1-04a: đăng ký thiếu email trả 400 kèm errors.email")
    void dangKyThieuEmailTra400KemProblemDetailErrors() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "Nguyễn Văn A", "", "0912345678", "Password123", null, null);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("TC-F1.1.1-04b: đăng ký mật khẩu thiếu chữ số trả 400 theo S1")
    void dangKyMatKhauThieuChuSoTra400TheoS1() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "Nguyễn Văn A", "member@example.com", "0912345678", "OnlyLettersPassword", null, null);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("TC-F1.1.1-04c: đăng ký mật khẩu dài hơn 72 ký tự trả 400 thay vì 500")
    void dangKyMatKhauDaiHon72KyTuTra400ThayVi500() throws Exception {
        String password80Chars = "A1" + "a".repeat(78);
        RegisterRequest request = new RegisterRequest(
            "Nguyễn Văn A", "member@example.com", "0912345678", password80Chars, null, null);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.password").value(AuthService.PASSWORD_MAX_BYTES_MESSAGE));
    }

    @Test
    @DisplayName("TC-F1.1.1-04d: đăng ký mật khẩu có dấu vượt 72 byte UTF-8 trả 400")
    void dangKyMatKhauCoDau50KyTuVuot72ByteTra400ThayVi422() throws Exception {
        String multiBytePassword = "ă".repeat(30) + "Pass1234567890123456";
        RegisterRequest request = new RegisterRequest(
            "Nguyễn Văn A", "member@example.com", "0912345678", multiBytePassword, null, null);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.password").value(AuthService.PASSWORD_MAX_BYTES_MESSAGE));
    }

    @Test
    @DisplayName("TC-F1.1.1-04e: đăng ký số điện thoại sai định dạng trả 400")
    void dangKySoDienThoaiSaiDinhDangTra400() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "Nguyễn Văn A", "member@example.com", "12345", "Password123", null, null);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    @DisplayName("TC-F1.1.1-04f: đăng ký ngày sinh tương lai trả 400")
    void dangKyNgaySinhTuongLaiTra400() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "Nguyễn Văn A", "member@example.com", "0912345678", "Password123", LocalDate.now().plusYears(1), null);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.dob").exists());
    }

    @Test
    @DisplayName("TC-F1.2.1-04d: đăng nhập thành công trả 200 kèm cookie và accessToken")
    void dangNhapThanhCongTra200KemCookieVaAccessToken() throws Exception {
        LoginRequest request = new LoginRequest("member@example.com", "Password123");

        when(authService.login(any(LoginRequest.class)))
            .thenReturn(new AuthService.AuthResult("access-token-abc", "refresh-token-def"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token-abc"))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refresh_token=")));
    }

    @Test
    @DisplayName("TC-F1.2.1-01d: đăng nhập sai mật khẩu trả 401 ProblemDetail tiếng Việt")
    void dangNhapSaiMatKhauTra401ProblemDetailTiengViet() throws Exception {
        LoginRequest request = new LoginRequest("member@example.com", "WrongPassword");

        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new UnauthorizedException("Email hoặc mật khẩu không chính xác."));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Chưa xác thực"))
            .andExpect(jsonPath("$.detail").value("Email hoặc mật khẩu không chính xác."));
    }

    @Test
    @DisplayName("TC-F1.2.1-05a: đăng nhập tài khoản bị khóa trả 403 ProblemDetail tiếng Việt")
    void dangNhapTaiKhoanBiKhoaTra403ProblemDetailTiengViet() throws Exception {
        LoginRequest request = new LoginRequest("locked@example.com", "Password123");

        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new ForbiddenException("Tài khoản đã bị khóa hoặc ngừng hoạt động. Vui lòng liên hệ quản trị viên."));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.title").value("Không có quyền"))
            .andExpect(jsonPath("$.detail").value("Tài khoản đã bị khóa hoặc ngừng hoạt động. Vui lòng liên hệ quản trị viên."));
    }

    @Test
    @DisplayName("TC-F1.3.1-01a: làm mới phiên thành công trả 200 kèm cookie mới")
    void lamMoiPhienThanhCongTra200KemCookieMoi() throws Exception {
        when(authService.refresh("my-refresh-token"))
            .thenReturn(new AuthService.AuthResult("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie("refresh_token", "my-refresh-token")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("new-access-token"))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("new-refresh-token")));
    }

    @Test
    @DisplayName("TC-F1.3.1-03a: làm mới thiếu cookie trả 401 và xóa cookie")
    void lamMoiThieuCookieTra401ProblemDetailVaXoaCookie() throws Exception {
        when(authService.refresh(null))
            .thenThrow(new UnauthorizedException("Không tìm thấy phiên làm mới."));

        mockMvc.perform(post("/api/v1/auth/refresh"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Không tìm thấy phiên làm mới."))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
    }

    @Test
    @DisplayName("TC-F1.3.1-02a: đăng xuất thành công trả 204 và xóa cookie")
    void dangXuatThanhCongTra204VaXoaCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie("refresh_token", "token-to-revoke")))
            .andExpect(status().isNoContent())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

        verify(authService).logout("token-to-revoke");
    }

    @Test
    @DisplayName("TC-F1.3.1-02b: đăng xuất kèm JWT cùng chủ sở hữu thu hồi và xóa cookie")
    void dangXuatCoJwtCungNguoiDungThuHoiVaXoaCookie() throws Exception {
        when(authService.logoutForUser("token-user-a", 10L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearerToken(10L, "a@example.com", UserRole.MEMBER))
                .cookie(new Cookie("refresh_token", "token-user-a")))
            .andExpect(status().isNoContent())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

        verify(authService).logoutForUser("token-user-a", 10L);
    }

    @Test
    @DisplayName("TC-F1.3.1-04a: đăng xuất kèm JWT khác chủ sở hữu cookie không xóa cookie chung")
    void dangXuatCoJwtKhacChuCookieKhongXoaCookieChung() throws Exception {
        when(authService.logoutForUser("token-user-b", 10L)).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearerToken(10L, "a@example.com", UserRole.MEMBER))
                .cookie(new Cookie("refresh_token", "token-user-b")))
            .andExpect(status().isNoContent())
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        verify(authService).logoutForUser("token-user-b", 10L);
    }

    @Test
    @DisplayName("TC-F1.3.1-02c, TC-F1.3.1-04b: đăng xuất kèm JWT hết hạn vẫn đối chiếu chủ sở hữu cookie")
    void dangXuatCoJwtHetHanVanDoiChieuChuSoHuuCookie() throws Exception {
        when(authService.logoutForUser("token-user-b", 10L)).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION,
                    "Bearer " + JwtTestSupport.expiredAccessToken(10L, "a@example.com", UserRole.MEMBER))
                .cookie(new Cookie("refresh_token", "token-user-b")))
            .andExpect(status().isNoContent())
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        verify(authService).logoutForUser("token-user-b", 10L);
    }

    @Test
    @DisplayName("S2: đăng xuất kèm JWT sai chữ ký coi như không gửi JWT")
    void dangXuatCoJwtSaiChuKyCoiNhuKhongGuiJwt() throws Exception {
        String forged = JwtTestSupport.accessTokenWithSecret(10L, "a@example.com", UserRole.MEMBER,
            "mot-khoa-bi-mat-khac-hoan-toan-dai-hon-32-byte");

        mockMvc.perform(post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + forged)
                .cookie(new Cookie("refresh_token", "token-to-revoke")))
            .andExpect(status().isNoContent())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

        verify(authService).logout("token-to-revoke");
    }

    @Test
    @DisplayName("TC-F1.6.1-02a: GET /auth/me với JWT hợp lệ trả 200 thông tin tài khoản hiện tại")
    void layThongTinTaiKhoanHienTaiUyQuyenChoAuthService() throws Exception {
        when(authService.getCurrentUser(any(Jwt.class)))
            .thenReturn(new AuthMeResponse(10L, "member@example.com", UserRole.MEMBER));

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearerToken(10L, "member@example.com", UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(10))
            .andExpect(jsonPath("$.email").value("member@example.com"))
            .andExpect(jsonPath("$.role").value("MEMBER"));

        verify(authService).getCurrentUser(any(Jwt.class));
    }

    @Test
    @DisplayName("TC-F1.5.1-01b: yêu cầu quên mật khẩu công khai trả 200 với câu thông báo chung")
    void quenMatKhauCongKhaiTra200CauChung() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("member@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(
                "Nếu email có trong hệ thống, bạn sẽ nhận được liên kết đặt lại mật khẩu."));

        verify(passwordResetService).forgotPassword(request);
    }

    @Test
    @DisplayName("TC-F1.5.1-05a: yêu cầu quên mật khẩu sai định dạng email trả 400 kèm errors")
    void quenMatKhauEmailSaiDinhDangTra400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(new ForgotPasswordRequest("khong-phai-email"))))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.email").exists());

        verify(passwordResetService, never()).forgotPassword(any());
    }

    @Test
    @DisplayName("TC-F1.5.1-03b: đặt lại mật khẩu hợp lệ công khai trả 204 No Content")
    void datLaiMatKhauHopLeCongKhaiTra204() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("valid-reset-token", "NewPass123");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(passwordResetService).resetPassword(request);
    }

    @Test
    @DisplayName("TC-F1.5.1-05b: mật khẩu mới sai chuẩn S1 hoặc thiếu token trả 400 kèm errors")
    void datLaiMatKhauSaiChuanS1Tra400() throws Exception {
        // Thiếu token và mật khẩu dưới 8 ký tự
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(new ResetPasswordRequest("", "short1"))))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.token").exists())
            .andExpect(jsonPath("$.errors.newPassword").exists());

        // Mật khẩu mới thiếu chữ số
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(new ResetPasswordRequest("some-token", "OnlyLettersPass"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.newPassword").exists());

        // Mật khẩu mới thiếu chữ cái
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(new ResetPasswordRequest("some-token", "123456789"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.newPassword").exists());

        verify(passwordResetService, never()).resetPassword(any());
    }

    @Test
    @DisplayName("TC-F1.5.1-04b: đặt lại mật khẩu với token hết hạn hoặc đã dùng trả 422")
    void datLaiMatKhauTokenLoiTra422() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "NewPass123");
        doThrow(new BusinessRuleException(PasswordResetService.INVALID_OR_EXPIRED_TOKEN_MESSAGE))
            .when(passwordResetService).resetPassword(request);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.detail").value("Liên kết đã hết hạn hoặc đã được dùng."));
    }
}
