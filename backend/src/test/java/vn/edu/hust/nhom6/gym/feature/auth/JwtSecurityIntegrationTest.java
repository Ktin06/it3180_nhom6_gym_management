package vn.edu.hust.nhom6.gym.feature.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.edu.hust.nhom6.gym.config.SecurityConfig;
import vn.edu.hust.nhom6.gym.feature.auth.controller.AuthController;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthCookieHelper;
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthService;

/**
 * Kiểm thử tích hợp tầng bảo mật cho UC-F1.6 (Đọc JWT HS256 và chặn API theo vai trò).
 */
@WebMvcTest(controllers = AuthController.class)
@ContextConfiguration(classes = {
    AuthController.class,
    SecurityConfig.class,
    JwtSecurityIntegrationTest.TestRoleEndpointConfig.class
})
class JwtSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthCookieHelper cookieHelper;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestRoleEndpointConfig {

        @RestController
        @RequestMapping("/api/v1/test-rbac")
        static class TestRbacController {

            @GetMapping("/staff-only")
            @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'RECEPTIONIST')")
            public Map<String, String> staffOnly() {
                return Map.of("access", "allowed");
            }
        }
    }

    @Test
    void chuaGuiTokenTra401ProblemDetailTiengViet() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Chưa xác thực"))
            .andExpect(jsonPath("$.detail").value("Bạn cần đăng nhập để truy cập tài nguyên này."));
    }

    @Test
    void tokenSaiChuKyTra401ProblemDetailTiengViet() throws Exception {
        String wrongSignatureToken = JwtTestSupport.accessTokenWithSecret(
            1L, "member@example.com", UserRole.MEMBER,
            "mot-khoa-bi-mat-khac-dai-hon-32-ky-tu-de-kiem-thu");

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongSignatureToken))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Chưa xác thực"));
    }

    @Test
    void tokenHetHanTra401ProblemDetailTiengViet() throws Exception {
        String expiredToken = JwtTestSupport.expiredAccessToken(2L, "member@example.com", UserRole.MEMBER);

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Chưa xác thực"));
    }

    @Test
    void tokenHopLeLayThongTinTaiKhoanHienTaiThanhCong() throws Exception {
        String bearer = JwtTestSupport.bearerToken(15L, "reception@example.com", UserRole.RECEPTIONIST);

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, bearer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(15))
            .andExpect(jsonPath("$.email").value("reception@example.com"))
            .andExpect(jsonPath("$.role").value("RECEPTIONIST"));
    }

    @Test
    void goiApiKhiDuVaiTroTra200() throws Exception {
        mockMvc.perform(get("/api/v1/test-rbac/staff-only")
                .header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearerToken(UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access").value("allowed"));
    }

    @Test
    void goiApiKhiKhongDuVaiTroTra403ProblemDetailTiengViet() throws Exception {
        mockMvc.perform(get("/api/v1/test-rbac/staff-only")
                .header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearerToken(UserRole.MEMBER)))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.title").value("Không có quyền"))
            .andExpect(jsonPath("$.detail").value("Bạn không có quyền thực hiện thao tác này."));
    }

    @Test
    void goiApiNhanVienBangVaiTroTrainerKhongDuQuyenTra403() throws Exception {
        mockMvc.perform(get("/api/v1/test-rbac/staff-only")
                .header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearerToken(UserRole.TRAINER)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.title").value("Không có quyền"));
    }

    @Test
    void tokenThieuClaimRoleBiChanVoi403KhiGoiApiCanVaiTro() throws Exception {
        String tokenWithoutRole = JwtTestSupport.accessTokenWithoutRole(99L, "norole@example.com");

        mockMvc.perform(get("/api/v1/test-rbac/staff-only")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutRole))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.title").value("Không có quyền"));
    }
}
