package vn.edu.hust.nhom6.gym.feature.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.mail.EmailSender;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.service.PasswordResetService;
import vn.edu.hust.nhom6.gym.feature.auth.service.TokenHashUtil;

/**
 * Kiểm thử tích hợp (Testcontainers) cho luồng quên và đặt lại mật khẩu (UC-F1.5, S2-05).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class PasswordResetIntegrationTest {

    private static final String TEST_EMAIL = "forgot.member@reset-test.example.com";
    private static final String TEST_PHONE = "0977111222";
    private static final String OTHER_EMAIL = "other.member@reset-test.example.com";
    private static final String OTHER_PHONE = "0977111333";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private MutableClock clock;

    @MockitoBean
    private EmailSender emailSender;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 10, 0));
        reset(emailSender);
        // Xóa riêng dữ liệu của lớp test này, không TRUNCATE users CASCADE làm mất system_configs
        jdbcTemplate.update(
            "DELETE FROM password_resets WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@reset-test.example.com')");
        jdbcTemplate.update(
            "DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@reset-test.example.com')");
        jdbcTemplate.update("DELETE FROM members WHERE phone IN (?, ?)", TEST_PHONE, OTHER_PHONE);
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE '%@reset-test.example.com'");
        jdbcTemplate.update("UPDATE branches SET status = 'ACTIVE' WHERE code = 'CN01'");
    }

    @Test
    @DisplayName("TC-F1.5.1-03c: đi trọn forgot -> lấy token từ EmailSender giả -> reset -> login mật khẩu mới 200, cũ 401, thu hồi mọi refresh token và mở khóa tạm")
    void luongTronVenQuenVaDatLaiMatKhau() throws Exception {
        // 1. Tạo tài khoản và 2 phiên đăng nhập
        Cookie refreshCookie1 = registerMember("Nguyễn Văn Quên", TEST_EMAIL, TEST_PHONE, "OldPass123");
        Cookie refreshCookie2 = loginAndGetRefreshCookie(TEST_EMAIL, "OldPass123");

        Long userId = jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = ?", Long.class, TEST_EMAIL);

        // 2. Giả lập trạng thái đang bị khóa tạm 15 phút (failed_login_count = 5, locked_until > now)
        jdbcTemplate.update(
            "UPDATE users SET failed_login_count = 5, locked_until = ? WHERE id = ?",
            OffsetDateTime.now().plusMinutes(15), userId);

        // Xác nhận đang bị khóa tạm (403) kể cả khi nhập đúng mật khẩu cũ
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "email", TEST_EMAIL,
                    "password", "OldPass123"
                ))))
            .andExpect(status().isForbidden());

        // 3. Gọi forgot-password -> nhận 200 câu chung
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", TEST_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(PasswordResetService.FORGOT_PASSWORD_MESSAGE));

        // 4. Lấy rawToken từ EmailSender giả và xác nhận DB chỉ lưu mã băm SHA-256
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(TEST_EMAIL), anyString(), bodyCaptor.capture());
        String rawToken = extractTokenFromBody(bodyCaptor.getValue());

        String storedHash = jdbcTemplate.queryForObject(
            "SELECT token_hash FROM password_resets WHERE user_id = ?", String.class, userId);
        assertThat(storedHash).isEqualTo(TokenHashUtil.sha256Hex(rawToken));
        assertThat(storedHash).isNotEqualTo(rawToken);

        // 5. Gọi reset-password với token thật -> 204
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "token", rawToken,
                    "newPassword", "NewPass456"
                ))))
            .andExpect(status().isNoContent());

        // 6. Kiểm tra used_at đã ghi, mọi refresh token đã thu hồi, khóa tạm đã được xóa
        Integer usedCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM password_resets WHERE user_id = ? AND used_at IS NOT NULL",
            Integer.class, userId);
        assertThat(usedCount).isEqualTo(1);

        Integer activeRefreshCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL",
            Integer.class, userId);
        assertThat(activeRefreshCount).isZero();

        Integer failedCount = jdbcTemplate.queryForObject(
            "SELECT failed_login_count FROM users WHERE id = ?", Integer.class, userId);
        Object lockedUntil = jdbcTemplate.queryForObject(
            "SELECT locked_until FROM users WHERE id = ?", Object.class, userId);
        assertThat(failedCount).isZero();
        assertThat(lockedUntil).isNull();

        // Hai refresh token cũ đều không làm mới được nữa (401)
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie1))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie2))
            .andExpect(status().isUnauthorized());

        // 7. Mật khẩu cũ không đăng nhập được (401), mật khẩu mới đăng nhập thành công (200) ngay cả khi chưa hết 15 phút
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "email", TEST_EMAIL,
                    "password", "OldPass123"
                ))))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "email", TEST_EMAIL,
                    "password", "NewPass456"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // 8. Dùng lại token đã dùng -> 422
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "token", rawToken,
                    "newPassword", "AnotherPass789"
                ))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.detail").value(PasswordResetService.INVALID_OR_EXPIRED_TOKEN_MESSAGE));
    }

    @Test
    @DisplayName("TC-F1.5.1-04c: token hết hạn sau 30 phút theo Clock bị từ chối 422 và email không tồn tại vẫn trả 200")
    void tokenHetHanSau30PhutTra422VaEmailKhongTonTaiTra200() throws Exception {
        // Email không tồn tại -> 200 câu chung, không gửi email
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", "khongtontai@reset-test.example.com"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(PasswordResetService.FORGOT_PASSWORD_MESSAGE));
        verify(emailSender, never()).send(anyString(), anyString(), anyString());

        // Đăng ký tài khoản và yêu cầu đặt lại mật khẩu
        registerMember("Nguyễn Văn Quên", TEST_EMAIL, TEST_PHONE, "OldPass123");
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", TEST_EMAIL))))
            .andExpect(status().isOk());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(TEST_EMAIL), anyString(), bodyCaptor.capture());
        String rawToken = extractTokenFromBody(bodyCaptor.getValue());

        // Tua đồng hồ sang đúng 30 phút sau -> token hết hạn (422)
        clock.advance(Duration.ofMinutes(30));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "token", rawToken,
                    "newPassword", "NewPass456"
                ))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.detail").value(PasswordResetService.INVALID_OR_EXPIRED_TOKEN_MESSAGE));
    }

    @Test
    @DisplayName("TC-F1.5.1-04d: hai yêu cầu đặt lại mật khẩu dùng cùng một token đồng thời chỉ có đúng 1 yêu cầu thành công (204), yêu cầu còn lại trả 422")
    void datLaiMatKhauDongThoiCungMotTokenChiMotLanThanhCong() throws Exception {
        registerMember("Nguyễn Văn Quên", TEST_EMAIL, TEST_PHONE, "OldPass123");
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", TEST_EMAIL))))
            .andExpect(status().isOk());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(TEST_EMAIL), anyString(), bodyCaptor.capture());
        String rawToken = extractTokenFromBody(bodyCaptor.getValue());

        String payload = jsonMapper.writeValueAsString(Map.of(
            "token", rawToken,
            "newPassword", "ConcurrentPass123"
        ));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                }));
            }
            ready.await();
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            assertThat(statuses).containsExactlyInAnyOrder(204, 422);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("TC-F1.5.1-03f: bấm quên mật khẩu 2 lần, đặt lại bằng một liên kết sẽ đánh dấu used_at cho mọi liên kết còn lại của user (422) và không ảnh hưởng tài khoản khác")
    void datLaiMatKhauVoHieuHoaTatCaLienKetQuenMatKhauKhacCuaCungUser() throws Exception {
        registerMember("Nguyễn Văn Quên", TEST_EMAIL, TEST_PHONE, "OldPass123");
        registerMember("Trần Văn Khác", OTHER_EMAIL, OTHER_PHONE, "OtherOld123");

        Long userId = jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = ?", Long.class, TEST_EMAIL);
        Long otherUserId = jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = ?", Long.class, OTHER_EMAIL);

        // User 1 bấm "Quên mật khẩu" 2 lần liên tiếp -> có 2 liên kết chưa dùng
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", TEST_EMAIL))))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", TEST_EMAIL))))
            .andExpect(status().isOk());

        // User 2 cũng có 1 liên kết chưa dùng để kiểm tra cô lập giữa các tài khoản
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", OTHER_EMAIL))))
            .andExpect(status().isOk());

        ArgumentCaptor<String> user1BodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender, times(2)).send(eq(TEST_EMAIL), anyString(), user1BodyCaptor.capture());
        String firstToken = extractTokenFromBody(user1BodyCaptor.getAllValues().get(0));
        String secondToken = extractTokenFromBody(user1BodyCaptor.getAllValues().get(1));
        assertThat(secondToken).isNotEqualTo(firstToken);

        ArgumentCaptor<String> user2BodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(OTHER_EMAIL), anyString(), user2BodyCaptor.capture());
        String otherUserToken = extractTokenFromBody(user2BodyCaptor.getValue());

        Integer unusedBefore = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM password_resets WHERE user_id = ? AND used_at IS NULL",
            Integer.class, userId);
        assertThat(unusedBefore).isEqualTo(2);

        // Đặt lại mật khẩu bằng liên kết đầu tiên -> 204
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "token", firstToken,
                    "newPassword", "FirstReset123"
                ))))
            .andExpect(status().isNoContent());

        // Cả 2 bản ghi password_resets của user 1 đều đã được đánh dấu used_at IS NOT NULL
        Integer unusedAfter = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM password_resets WHERE user_id = ? AND used_at IS NULL",
            Integer.class, userId);
        Integer usedAfter = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM password_resets WHERE user_id = ? AND used_at IS NOT NULL",
            Integer.class, userId);
        assertThat(unusedAfter).isZero();
        assertThat(usedAfter).isEqualTo(2);

        // Liên kết thứ hai của user 1 không thể dùng để đặt lại mật khẩu nữa -> 422
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "token", secondToken,
                    "newPassword", "SecondReset456"
                ))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.detail").value(PasswordResetService.INVALID_OR_EXPIRED_TOKEN_MESSAGE));

        // Mật khẩu vẫn là FirstReset123, không bị ghi đè bởi liên kết thứ hai
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "email", TEST_EMAIL,
                    "password", "FirstReset123"
                ))))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "email", TEST_EMAIL,
                    "password", "SecondReset456"
                ))))
            .andExpect(status().isUnauthorized());

        // Tài khoản khác vẫn còn nguyên liên kết chưa dùng và đặt lại được bình thường
        Integer otherUnused = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM password_resets WHERE user_id = ? AND used_at IS NULL",
            Integer.class, otherUserId);
        assertThat(otherUnused).isEqualTo(1);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "token", otherUserToken,
                    "newPassword", "OtherNewPass123"
                ))))
            .andExpect(status().isNoContent());
    }

    private Cookie registerMember(String fullName, String email, String phone, String password) throws Exception {
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
        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private Cookie loginAndGetRefreshCookie(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "email", email,
                    "password", password
                ))))
            .andExpect(status().isOk())
            .andReturn();
        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private String extractTokenFromBody(String body) {
        String marker = "/dat-lai-mat-khau?token=";
        int start = body.indexOf(marker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        String after = body.substring(start + marker.length());
        int end = after.length();
        for (int i = 0; i < after.length(); i++) {
            if (Character.isWhitespace(after.charAt(i))) {
                end = i;
                break;
            }
        }
        return after.substring(0, end);
    }
}
