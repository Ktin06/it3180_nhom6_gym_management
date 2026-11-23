package vn.edu.hust.nhom6.gym.feature.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.DatabaseCleaner;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;

/**
 * Kiểm thử tích hợp cho API đổi mật khẩu {@code POST /api/v1/auth/change-password} (UC-F1.4, S2-03).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ChangePasswordIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void cleanDatabase() {
        DatabaseCleaner.truncate(jdbcTemplate, "members", "users", "staff", "refresh_tokens");
        jdbcTemplate.update("UPDATE branches SET status = 'ACTIVE' WHERE code = 'CN01'");
    }

    @Test
    @DisplayName("US-F1.4.1 KB1: đổi mật khẩu thành công")
    void doiMatKhauThanhCongMatKhauMoiDangNhapDuocMatKhauCuKhong() throws Exception {
        SessionTokens session = registerMember("Nguyễn Văn A", "member@example.com", "0912345678", "OldPass123");

        mockMvc.perform(post("/api/v1/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "currentPassword", "OldPass123",
                    "newPassword", "NewPass456"
                ))))
            .andExpect(status().isNoContent());

        // Mật khẩu cũ không đăng nhập được (401)
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "email", "member@example.com",
                    "password", "OldPass123"
                ))))
            .andExpect(status().isUnauthorized());

        // Mật khẩu mới đăng nhập thành công (200)
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "email", "member@example.com",
                    "password", "NewPass456"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("US-F1.4.1 KB1: thu hồi mọi phiên")
    void doiMatKhauThuHoiMoiRefreshTokenCuaNguoiDung() throws Exception {
        SessionTokens session1 = registerMember("Nguyễn Văn A", "member@example.com", "0912345678", "OldPass123");
        SessionTokens session2 = loginMember("member@example.com", "OldPass123");
        SessionTokens otherUser = registerMember("Trần Thị B", "other@example.com", "0987654321", "OtherPass123");

        Long userId = jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = 'member@example.com'", Long.class);
        Long otherUserId = jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = 'other@example.com'", Long.class);

        assertThat(countActiveRefreshTokens(userId)).isEqualTo(2);
        assertThat(countActiveRefreshTokens(otherUserId)).isEqualTo(1);

        mockMvc.perform(post("/api/v1/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session1.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "currentPassword", "OldPass123",
                    "newPassword", "NewPass456"
                ))))
            .andExpect(status().isNoContent());

        assertThat(countActiveRefreshTokens(userId)).isZero();
        assertThat(countRevokedRefreshTokens(userId)).isEqualTo(2);
        assertThat(countActiveRefreshTokens(otherUserId)).isEqualTo(1);

        // Cả hai refresh token cũ đều không làm mới phiên được nữa
        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie("refresh_token", session1.refreshToken())))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie("refresh_token", session2.refreshToken())))
            .andExpect(status().isUnauthorized());

        // Phiên của người dùng khác không bị ảnh hưởng
        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie("refresh_token", otherUser.refreshToken())))
            .andExpect(status().isOk());
    }

    private int countActiveRefreshTokens(Long userId) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL",
            Integer.class, userId);
    }

    private int countRevokedRefreshTokens(Long userId) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NOT NULL",
            Integer.class, userId);
    }

    private SessionTokens registerMember(String fullName, String email, String phone, String password)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "fullName", fullName,
                    "email", email,
                    "phone", phone,
                    "password", password
                ))))
            .andExpect(status().isCreated())
            .andReturn();
        return extractSessionTokens(result);
    }

    private SessionTokens loginMember(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "email", email,
                    "password", password
                ))))
            .andExpect(status().isOk())
            .andReturn();
        return extractSessionTokens(result);
    }

    private SessionTokens extractSessionTokens(MvcResult result) throws Exception {
        String accessToken = jsonMapper.readTree(result.getResponse().getContentAsString())
            .get("accessToken").asString();
        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        return new SessionTokens(accessToken, cookie.getValue());
    }

    private record SessionTokens(String accessToken, String refreshToken) {
    }
}
