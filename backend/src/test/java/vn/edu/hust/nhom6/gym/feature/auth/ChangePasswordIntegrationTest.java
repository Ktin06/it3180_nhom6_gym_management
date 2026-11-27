package vn.edu.hust.nhom6.gym.feature.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import org.springframework.transaction.support.TransactionTemplate;

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

    @Autowired
    private TransactionTemplate transactions;

    @BeforeEach
    void cleanDatabase() {
        DatabaseCleaner.truncate(jdbcTemplate, "members", "users", "staff", "refresh_tokens");
        jdbcTemplate.update("UPDATE branches SET status = 'ACTIVE' WHERE code = 'CN01'");
    }

    @Test
    @DisplayName("TC-F1.4.1-01a: đổi mật khẩu thành công, mật khẩu mới đăng nhập được, mật khẩu cũ không")
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
    @DisplayName("TC-F1.4.1-01b: thu hồi mọi refresh token của người dùng khi đổi mật khẩu")
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

    @Test
    @DisplayName("TC-F1.4.1-05a: Sai mật khẩu hiện tại 5 lần thì khóa tạm 15 phút; đang khóa thì đổi mật khẩu và đăng nhập đều trả 403, mật khẩu không đổi")
    void saiMatKhauHienTaiNhieuLanThiKhoaTam() throws Exception {
        SessionTokens session = registerMember("Nguyễn Văn A", "member@example.com", "0912345678", "OldPass123");
        String oldHash = passwordHash("member@example.com");

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(changePassword(session, "WrongPass1", "NewPass456"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Mật khẩu hiện tại không đúng."));
            assertThat(failedLoginCount("member@example.com")).isEqualTo(i);
        }
        assertThat(lockedUntilSet("member@example.com")).isTrue();

        // Đang khóa: kể cả mật khẩu hiện tại đúng cũng bị từ chối, không tăng bộ đếm
        mockMvc.perform(changePassword(session, "OldPass123", "NewPass456"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith(
                "Tài khoản đang bị khóa tạm thời do nhập sai mật khẩu nhiều lần.")));
        assertThat(failedLoginCount("member@example.com")).isEqualTo(5);
        assertThat(passwordHash("member@example.com")).isEqualTo(oldHash);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "email", "member@example.com", "password", "OldPass123"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-F1.4.1-05b: Đăng nhập sai và đổi mật khẩu sai dùng chung một bộ đếm: 4 lần đăng nhập sai cộng 1 lần đổi sai thì khóa tạm")
    void dangNhapSaiVaDoiMatKhauSaiDungChungBoDem() throws Exception {
        SessionTokens session = registerMember("Nguyễn Văn A", "member@example.com", "0912345678", "OldPass123");
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(Map.of(
                        "email", "member@example.com", "password", "WrongPass1"))))
                .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(changePassword(session, "WrongPass1", "NewPass456"))
            .andExpect(status().isUnprocessableContent());

        assertThat(failedLoginCount("member@example.com")).isEqualTo(5);
        assertThat(lockedUntilSet("member@example.com")).isTrue();
    }

    @Test
    @DisplayName("TC-F1.4.1-06a: Đổi mật khẩu thành công đặt bộ đếm sai về 0, xóa khóa tạm đã hết hạn và trả cookie refresh_token đã hết hạn")
    void doiMatKhauThanhCongDatLaiBoDemVaXoaCookie() throws Exception {
        SessionTokens session = registerMember("Nguyễn Văn A", "member@example.com", "0912345678", "OldPass123");
        jdbcTemplate.update("""
            UPDATE users SET failed_login_count = 3, locked_until = now() - interval '1 minute'
            WHERE email = 'member@example.com'""");

        MvcResult result = mockMvc.perform(changePassword(session, "OldPass123", "NewPass456"))
            .andExpect(status().isNoContent())
            .andReturn();

        assertThat(failedLoginCount("member@example.com")).isZero();
        assertThat(lockedUntilSet("member@example.com")).isFalse();
        Cookie cleared = result.getResponse().getCookie("refresh_token");
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isZero();
        assertThat(cleared.getValue()).isEmpty();
    }

    @Test
    @DisplayName("TC-F1.4.1-06b: Mật khẩu mới trùng mật khẩu hiện tại thì 422 và không tăng bộ đếm sai")
    void matKhauMoiTrungKhongTangBoDem() throws Exception {
        SessionTokens session = registerMember("Nguyễn Văn A", "member@example.com", "0912345678", "OldPass123");

        mockMvc.perform(changePassword(session, "OldPass123", "OldPass123"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Mật khẩu mới phải khác mật khẩu hiện tại."));

        assertThat(failedLoginCount("member@example.com")).isZero();
    }

    private org.springframework.test.web.servlet.RequestBuilder changePassword(
            SessionTokens session, String currentPassword, String newPassword) throws Exception {
        return post("/api/v1/auth/change-password")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(Map.of(
                "currentPassword", currentPassword, "newPassword", newPassword)));
    }

    private int failedLoginCount(String email) {
        return jdbcTemplate.queryForObject(
            "SELECT failed_login_count FROM users WHERE email = ?", Integer.class, email);
    }

    private boolean lockedUntilSet(String email) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
            "SELECT locked_until IS NOT NULL FROM users WHERE email = ?", Boolean.class, email));
    }

    private String passwordHash(String email) {
        return jdbcTemplate.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);
    }

    private int countActiveRefreshTokens(Long userId) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL",
            Integer.class, userId);
    }

    @Test
    @DisplayName("TC-F1.4.1-01h: Hai yêu cầu đổi đồng thời chỉ dùng được mật khẩu cũ một lần")
    void concurrentPasswordChangesAcceptOldPasswordOnlyOnce() throws Exception {
        SessionTokens session = registerMember("Hội viên", "member@example.com", "0912345678", "OldPass123");
        long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = 'member@example.com'", Long.class);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var pending = transactions.execute(status -> {
                jdbcTemplate.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE", Long.class, userId);
                var first = executor.submit(() -> changePasswordStatus(session, "NewPass456"));
                var second = executor.submit(() -> changePasswordStatus(session, "NewPass789"));
                awaitBlockedAuthQueries(2);
                return List.of(first, second);
            });
            assertThat(List.of(pending.get(0).get(10, TimeUnit.SECONDS),
                pending.get(1).get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder(204, 422);
        }
    }

    @Test
    @DisplayName("TC-F1.4.1-01i: Đổi mật khẩu thu hồi token được tạo bởi lượt refresh đang thực hiện")
    void passwordChangeRevokesInFlightRefreshSuccessor() throws Exception {
        SessionTokens session = registerMember("Hội viên", "member@example.com", "0912345678", "OldPass123");
        long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = 'member@example.com'", Long.class);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var change = transactions.execute(status -> {
                try {
                    mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new Cookie("refresh_token", session.refreshToken())))
                        .andExpect(status().isOk());
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
                var pending = executor.submit(() -> changePasswordStatus(session, "NewPass456"));
                awaitBlockedAuthQueries(1);
                return pending;
            });
            assertThat(change.get(10, TimeUnit.SECONDS)).isEqualTo(204);
            assertThat(countRevokedRefreshTokens(userId)).isEqualTo(2);
            assertThat(countActiveRefreshTokens(userId)).isZero();
        }
    }

    private int changePasswordStatus(SessionTokens session, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "currentPassword", "OldPass123", "newPassword", newPassword))))
            .andReturn().getResponse().getStatus();
    }

    private void awaitBlockedAuthQueries(int expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        do {
            jdbcTemplate.execute("SELECT pg_stat_clear_snapshot()");
            Integer blocked = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM pg_stat_activity
                WHERE datname=current_database() AND wait_event_type='Lock'
                  AND (query LIKE '%users%' OR query LIKE '%refresh_tokens%')
                """, Integer.class);
            if (blocked >= expected) return;
            try {
                Thread.sleep(20);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        } while (System.nanoTime() < deadline);
        throw new AssertionError("Không quan sát được " + expected + " yêu cầu xác thực đang chờ khóa.");
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
