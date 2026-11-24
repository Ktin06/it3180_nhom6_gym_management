package vn.edu.hust.nhom6.gym.feature.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.DatabaseCleaner;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;

/**
 * Kiểm thử tích hợp với PostgreSQL thật (Testcontainers) cho luồng làm mới phiên và đăng xuất nhiều tab (#104, #112).
 *
 * <p>Hậu tố {@code *Test} để Maven Surefire tự chạy trong pha {@code verify} và CI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthSessionConcurrencyIntegrationTest {

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
    @DisplayName("TC-F1.3.1-01d: hai yêu cầu refresh đồng thời chỉ một thành công 200 và một nhận 401")
    void haiYeuCauRefreshDongThoiChiMotThanhCongVaMotNhan401() throws Exception {
        SessionTokens session = registerMember("Người dùng Test", "concurrent@example.com", "0912345678");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 5; round++) {
                String currentRefresh = session.refreshToken();
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<MvcResult>> futures = new ArrayList<>();

                for (int i = 0; i < 2; i++) {
                    futures.add(pool.submit(() -> {
                        ready.countDown();
                        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                        return mockMvc.perform(post("/api/v1/auth/refresh")
                                .cookie(new Cookie("refresh_token", currentRefresh)))
                            .andReturn();
                    }));
                }

                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
                start.countDown();

                List<Integer> statuses = new ArrayList<>();
                for (Future<MvcResult> future : futures) {
                    statuses.add(future.get(10, TimeUnit.SECONDS).getResponse().getStatus());
                }

                assertThat(statuses).containsExactlyInAnyOrder(200, 401);

                // Tạo phiên đăng nhập mới cho vòng lặp tiếp theo vì reuse detection đã thu hồi mọi token của user.
                session = loginMember("concurrent@example.com", "Password123");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("TC-F1.3.1-04d: đăng xuất bằng JWT tài khoản A không thu hồi hay xóa cookie tài khoản B")
    void dangXuatBangJwtCuaTaiKhoanAKhongThuHoiHoacXoaCookieCuaTaiKhoanB() throws Exception {
        SessionTokens sessionA = registerMember("Thành viên A", "user.a@example.com", "0911111111");
        SessionTokens sessionB = registerMember("Thành viên B", "user.b@example.com", "0922222222");

        // Tab 1 (A) gửi logout với Bearer JWT của A nhưng cookie chung trên trình duyệt đang là của B.
        mockMvc.perform(post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionA.accessToken())
                .cookie(new Cookie("refresh_token", sessionB.refreshToken())))
            .andExpect(status().isNoContent())
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        // Phiên của B ở tab 2 vẫn làm mới bình thường.
        MvcResult refreshBResult = mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie("refresh_token", sessionB.refreshToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn();

        String newAccessB = jsonMapper.readTree(refreshBResult.getResponse().getContentAsString())
            .get("accessToken").asString();
        Cookie rotatedCookieB = refreshBResult.getResponse().getCookie("refresh_token");
        assertThat(rotatedCookieB).isNotNull();

        // Khi chính B đăng xuất kèm JWT của B, token bị thu hồi và cookie bị xóa (Max-Age=0).
        mockMvc.perform(post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessB)
                .cookie(new Cookie("refresh_token", rotatedCookieB.getValue())))
            .andExpect(status().isNoContent())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie("refresh_token", rotatedCookieB.getValue())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-F1.3.1-02f, TC-F1.3.1-04e: đăng xuất bằng JWT hết hạn của tài khoản A vẫn giữ phiên tài khoản B")
    void dangXuatBangJwtHetHanCuaTaiKhoanAVanGiuPhienCuaTaiKhoanB() throws Exception {
        registerMember("Thành viên A", "user.a@example.com", "0911111111");
        SessionTokens sessionB = registerMember("Thành viên B", "user.b@example.com", "0922222222");
        Long userIdA = jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = ?", Long.class, "user.a@example.com");

        // Tab 1 (A) để lâu nên access token đã hết hạn; cookie chung trên trình duyệt đang là của B.
        mockMvc.perform(post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION,
                    "Bearer " + JwtTestSupport.expiredAccessToken(userIdA, "user.a@example.com", UserRole.MEMBER))
                .cookie(new Cookie("refresh_token", sessionB.refreshToken())))
            .andExpect(status().isNoContent())
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie("refresh_token", sessionB.refreshToken())))
            .andExpect(status().isOk());
    }

    private SessionTokens registerMember(String fullName, String email, String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "fullName", fullName,
                    "email", email,
                    "phone", phone,
                    "password", "Password123"
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
        assertThat(cookie.getValue()).isNotBlank();
        return new SessionTokens(accessToken, cookie.getValue());
    }

    private record SessionTokens(String accessToken, String refreshToken) {
    }
}
