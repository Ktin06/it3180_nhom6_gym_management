package vn.edu.hust.nhom6.gym.feature.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
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
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.GlobalExceptionHandler;
import vn.edu.hust.nhom6.gym.common.exception.UnauthorizedException;
import vn.edu.hust.nhom6.gym.config.SecurityConfig;
import vn.edu.hust.nhom6.gym.feature.auth.controller.AuthController;
import vn.edu.hust.nhom6.gym.feature.auth.dto.AuthMeResponse;
import vn.edu.hust.nhom6.gym.feature.auth.dto.LoginRequest;
import vn.edu.hust.nhom6.gym.feature.auth.dto.RegisterRequest;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthCookieHelper;
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthService;

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
    void dangKyMatKhauDaiHon72KyTuTra400ThayVi500() throws Exception {
        String password80Chars = "A1" + "a".repeat(78);
        RegisterRequest request = new RegisterRequest(
            "Nguyễn Văn A", "member@example.com", "0912345678", password80Chars, null, null);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.password").value("Mật khẩu phải từ 8 đến 72 ký tự"));
    }

    @Test
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
    void dangXuatThanhCongTra204VaXoaCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie("refresh_token", "token-to-revoke")))
            .andExpect(status().isNoContent())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

        verify(authService).logout("token-to-revoke");
    }

    @Test
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
}
