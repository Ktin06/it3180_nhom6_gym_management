package vn.edu.hust.nhom6.gym.feature.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
import java.util.concurrent.TimeUnit;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.mail.EmailSender;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.AccountService;
import vn.edu.hust.nhom6.gym.feature.auth.service.PasswordResetService;
import vn.edu.hust.nhom6.gym.feature.auth.service.TokenHashUtil;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberAccountService;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;

/**
 * Kiểm thử tích hợp (Testcontainers) cho API tạo tài khoản cho hồ sơ hội viên có sẵn (F2, UC-F2.2, S2-36).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class MemberAccountIntegrationTest {

    private static final String EMAIL_DOMAIN = "@member-account-test.example.com";

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

    private Long branchId;
    private Long otherBranchId;
    private Long adminUserId;
    private Long managerUserId;
    private Long receptionistUserId;
    private Long otherReceptionistUserId;
    private Long existingMemberUserId;
    private Long trainerUserId;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 14, 0));
        reset(emailSender);

        // Xóa riêng dữ liệu của lớp test này, không TRUNCATE users CASCADE làm mất system_configs
        jdbcTemplate.update(
            "DELETE FROM password_resets WHERE user_id IN (SELECT id FROM users WHERE email LIKE ?)",
            "%" + EMAIL_DOMAIN);
        jdbcTemplate.update(
            "DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE email LIKE ?)",
            "%" + EMAIL_DOMAIN);
        jdbcTemplate.update("DELETE FROM members WHERE member_code LIKE 'HV880%'");
        jdbcTemplate.update(
            "DELETE FROM staff WHERE user_id IN (SELECT id FROM users WHERE email LIKE ?)",
            "%" + EMAIL_DOMAIN);
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE ?", "%" + EMAIL_DOMAIN);

        jdbcTemplate.update("UPDATE branches SET status = 'ACTIVE' WHERE code = 'CN01'");
        branchId = jdbcTemplate.queryForObject("SELECT id FROM branches WHERE code = 'CN01'", Long.class);
        jdbcTemplate.update("""
            INSERT INTO branches(code, name, open_time, close_time, status)
            VALUES ('CN02', 'Chi nhánh 2', '06:00', '22:00', 'ACTIVE')
            ON CONFLICT(code) DO UPDATE SET status = 'ACTIVE'
            """);
        otherBranchId = jdbcTemplate.queryForObject("SELECT id FROM branches WHERE code = 'CN02'", Long.class);

        adminUserId = insertUser("admin" + EMAIL_DOMAIN, "SUPER_ADMIN");
        managerUserId = insertUser("manager" + EMAIL_DOMAIN, "BRANCH_MANAGER");
        receptionistUserId = insertUser("reception" + EMAIL_DOMAIN, "RECEPTIONIST");
        otherReceptionistUserId = insertUser("other.reception" + EMAIL_DOMAIN, "RECEPTIONIST");
        existingMemberUserId = insertUser("existing.member" + EMAIL_DOMAIN, "MEMBER");
        trainerUserId = insertUser("trainer" + EMAIL_DOMAIN, "TRAINER");

        insertStaff(managerUserId, branchId, "NV8801");
        insertStaff(receptionistUserId, branchId, "NV8802");
        insertStaff(otherReceptionistUserId, otherBranchId, "NV8803");
    }

    @Test
    @DisplayName("TC-F2.2.1-01f: tạo tài khoản cho hồ sơ ACTIVE chưa có user_id trả 200 không kèm token, lưu users + members.user_id + password_resets hạn 72 giờ và gửi email Kích hoạt tài khoản")
    void createAccountPersistsUserLinkTokenAndSendsActivationEmail() throws Exception {
        Long memberId = insertMember("HV880001", "0978100001", branchId, null, "ACTIVE");
        String inputEmail = "New.Member1" + EMAIL_DOMAIN;
        String normalizedEmail = inputEmail.toLowerCase();

        MvcResult createResult = mockMvc.perform(post("/api/v1/members/{id}/account", memberId)
                .with(identity(receptionistUserId, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", inputEmail))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.memberId").value(memberId))
            .andExpect(jsonPath("$.userId").isNumber())
            .andExpect(jsonPath("$.email").value(normalizedEmail))
            .andExpect(jsonPath("$.token").doesNotExist())
            .andReturn();

        JsonNode json = jsonMapper.readTree(createResult.getResponse().getContentAsString());
        Long createdUserId = json.get("userId").asLong();

        Map<String, Object> userRow = jdbcTemplate.queryForMap(
            "SELECT email, password_hash, role, status FROM users WHERE id = ?", createdUserId);
        assertThat(userRow.get("email")).isEqualTo(normalizedEmail);
        assertThat(userRow.get("role")).isEqualTo("MEMBER");
        assertThat(userRow.get("status")).isEqualTo("ACTIVE");
        assertThat(String.valueOf(userRow.get("password_hash"))).startsWith("$2");

        Long linkedUserId = jdbcTemplate.queryForObject(
            "SELECT user_id FROM members WHERE id = ?", Long.class, memberId);
        assertThat(linkedUserId).isEqualTo(createdUserId);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(
            eq(normalizedEmail),
            eq(MemberAccountService.ACTIVATION_EMAIL_SUBJECT),
            bodyCaptor.capture());
        String rawToken = extractTokenFromBody(bodyCaptor.getValue());

        String storedHash = jdbcTemplate.queryForObject(
            "SELECT token_hash FROM password_resets WHERE user_id = ?", String.class, createdUserId);
        OffsetDateTime expiresAt = jdbcTemplate.queryForObject(
            "SELECT expires_at FROM password_resets WHERE user_id = ?", OffsetDateTime.class, createdUserId);
        assertThat(storedHash).isEqualTo(TokenHashUtil.sha256Hex(rawToken));
        assertThat(storedHash).isNotEqualTo(rawToken);
        assertThat(Duration.between(clock.instant(), expiresAt.toInstant())).isEqualTo(Duration.ofHours(72));
    }

    @Test
    @DisplayName("TC-F2.2.1-02a: hội viên đặt mật khẩu bằng liên kết trong hạn 72 giờ rồi đăng nhập 200; quá 72 giờ bị từ chối 422")
    void resetPasswordWithin72HoursAndLoginSucceedsWhileExpiredReturns422() throws Exception {
        Long memberId = insertMember("HV880002", "0978100002", branchId, null, "ACTIVE");
        String email = "activate.e2e" + EMAIL_DOMAIN;

        mockMvc.perform(post("/api/v1/members/{id}/account", memberId)
                .with(identity(receptionistUserId, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", email))))
            .andExpect(status().isOk());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(email), eq(MemberAccountService.ACTIVATION_EMAIL_SUBJECT), bodyCaptor.capture());
        String rawToken = extractTokenFromBody(bodyCaptor.getValue());

        // Hội viên đặt mật khẩu ở giờ thứ 71 (còn hạn 72 giờ) -> 204
        clock.advance(Duration.ofHours(71));
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "token", rawToken,
                    "newPassword", "MatKhauMoi123"
                ))))
            .andExpect(status().isNoContent());

        // Hội viên đăng nhập bằng mật khẩu vừa đặt -> 200 OK
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "email", email,
                    "password", "MatKhauMoi123"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // Kiểm chứng biên hết hạn đúng 72 giờ với một hồ sơ thứ hai
        reset(emailSender);
        Long secondMemberId = insertMember("HV880010", "0978100010", branchId, null, "ACTIVE");
        String secondEmail = "expire72h" + EMAIL_DOMAIN;
        mockMvc.perform(post("/api/v1/members/{id}/account", secondMemberId)
                .with(identity(managerUserId, UserRole.BRANCH_MANAGER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", secondEmail))))
            .andExpect(status().isOk());

        ArgumentCaptor<String> secondBodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(
            eq(secondEmail),
            eq(MemberAccountService.ACTIVATION_EMAIL_SUBJECT),
            secondBodyCaptor.capture());
        String secondToken = extractTokenFromBody(secondBodyCaptor.getValue());

        clock.advance(Duration.ofHours(72));
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "token", secondToken,
                    "newPassword", "MatKhauMoi123"
                ))))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.detail").value(PasswordResetService.INVALID_OR_EXPIRED_TOKEN_MESSAGE));
    }

    @Test
    @DisplayName("TC-F2.2.1-03d: hồ sơ đã có tài khoản trả 409 và không tạo thêm user hay gửi email")
    void memberAlreadyHasAccountReturns409() throws Exception {
        Long linkedMemberId = insertMember("HV880003", "0978100003", branchId, existingMemberUserId, "ACTIVE");
        Integer usersBefore = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users WHERE email LIKE ?", Integer.class, "%" + EMAIL_DOMAIN);

        mockMvc.perform(post("/api/v1/members/{id}/account", linkedMemberId)
                .with(identity(receptionistUserId, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", "brand.new" + EMAIL_DOMAIN))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value(MemberService.MEMBER_ALREADY_HAS_ACCOUNT));

        Integer usersAfter = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users WHERE email LIKE ?", Integer.class, "%" + EMAIL_DOMAIN);
        assertThat(usersAfter).isEqualTo(usersBefore);
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("TC-F2.2.1-03e: hai yêu cầu tạo tài khoản đồng thời cho cùng một hồ sơ chỉ có 1 thành công 200, yêu cầu còn lại 409 và không sinh user mồ côi")
    void concurrentCreateAccountForSameMemberCreatesSingleUser() throws Exception {
        Long memberId = insertMember("HV880011", "0978100011", branchId, null, "ACTIVE");
        Integer usersBefore = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users WHERE email LIKE ?", Integer.class, "%" + EMAIL_DOMAIN);

        List<Integer> statuses = runConcurrently(
            () -> mockMvc.perform(post("/api/v1/members/{id}/account", memberId)
                    .with(identity(receptionistUserId, UserRole.RECEPTIONIST))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(Map.of("email", "race.member.a" + EMAIL_DOMAIN))))
                .andReturn().getResponse().getStatus(),
            () -> mockMvc.perform(post("/api/v1/members/{id}/account", memberId)
                    .with(identity(managerUserId, UserRole.BRANCH_MANAGER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(Map.of("email", "race.member.b" + EMAIL_DOMAIN))))
                .andReturn().getResponse().getStatus()
        );

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        Integer usersAfter = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users WHERE email LIKE ?", Integer.class, "%" + EMAIL_DOMAIN);
        assertThat(usersAfter).isEqualTo(usersBefore + 1);
        assertThat(jdbcTemplate.queryForObject("SELECT user_id FROM members WHERE id = ?", Long.class, memberId))
            .isNotNull();
    }

    @Test
    @DisplayName("TC-F2.2.1-04c: email đã thuộc tài khoản khác trả 409 và giữ nguyên hồ sơ chưa gắn tài khoản")
    void emailAlreadyUsedByAnotherAccountReturns409() throws Exception {
        Long unlinkedMemberId = insertMember("HV880004", "0978100004", branchId, null, "ACTIVE");

        mockMvc.perform(post("/api/v1/members/{id}/account", unlinkedMemberId)
                .with(identity(receptionistUserId, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                    "email", ("EXISTING.MEMBER" + EMAIL_DOMAIN).toUpperCase()
                ))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value(AccountService.EMAIL_ALREADY_USED_MESSAGE));

        Long linkedUserId = jdbcTemplate.queryForObject(
            "SELECT user_id FROM members WHERE id = ?", Long.class, unlinkedMemberId);
        assertThat(linkedUserId).isNull();
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("TC-F2.2.1-04d: hai yêu cầu dùng cùng một email mới cho hai hồ sơ khác nhau gửi đồng thời chỉ có 1 thành công 200 và 1 trả 409")
    void concurrentCreateAccountWithSameEmailRejectsSecondWith409() throws Exception {
        Long memberA = insertMember("HV880012", "0978100012", branchId, null, "ACTIVE");
        Long memberB = insertMember("HV880013", "0978100013", branchId, null, "ACTIVE");
        String sharedEmail = "shared.race" + EMAIL_DOMAIN;

        List<Integer> statuses = runConcurrently(
            () -> mockMvc.perform(post("/api/v1/members/{id}/account", memberA)
                    .with(identity(receptionistUserId, UserRole.RECEPTIONIST))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(Map.of("email", sharedEmail))))
                .andReturn().getResponse().getStatus(),
            () -> mockMvc.perform(post("/api/v1/members/{id}/account", memberB)
                    .with(identity(managerUserId, UserRole.BRANCH_MANAGER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(Map.of("email", sharedEmail.toUpperCase()))))
                .andReturn().getResponse().getStatus()
        );

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        Integer emailCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users WHERE email = ?", Integer.class, sharedEmail);
        assertThat(emailCount).isEqualTo(1);
        Integer linkedCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM members WHERE id IN (?, ?) AND user_id IS NOT NULL",
            Integer.class, memberA, memberB);
        assertThat(linkedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-F2.2.1-05d: hồ sơ INACTIVE trả 422 và không tạo tài khoản hay gửi email")
    void inactiveMemberReturns422() throws Exception {
        Long inactiveMemberId = insertMember("HV880005", "0978100005", branchId, null, "INACTIVE");

        mockMvc.perform(post("/api/v1/members/{id}/account", inactiveMemberId)
                .with(identity(receptionistUserId, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", "inactive.member" + EMAIL_DOMAIN))))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.detail").value(MemberService.INACTIVE_MEMBER_CANNOT_CREATE_ACCOUNT));

        Integer userCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users WHERE email = ?", Integer.class, "inactive.member" + EMAIL_DOMAIN);
        assertThat(userCount).isZero();
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("TC-F2.2.1-07d: kiểm tra phân quyền vai trò và phạm vi chi nhánh (401/403/404 và SUPER_ADMIN tạo được mọi chi nhánh)")
    void enforcesRoleAndBranchPermissions() throws Exception {
        Long memberId = insertMember("HV880006", "0978100006", branchId, null, "ACTIVE");
        Long otherBranchMemberId = insertMember("HV880007", "0978100007", otherBranchId, null, "ACTIVE");
        Long softDeletedMemberId = insertMember("HV880014", "0978100014", branchId, null, "ACTIVE");
        jdbcTemplate.update("UPDATE members SET deleted_at = NOW() WHERE id = ?", softDeletedMemberId);
        String body = jsonMapper.writeValueAsString(Map.of("email", "perm.test" + EMAIL_DOMAIN));

        // Chưa đăng nhập -> 401
        mockMvc.perform(post("/api/v1/members/{id}/account", memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());

        // MEMBER và TRAINER -> 403
        for (UserRole role : List.of(UserRole.MEMBER, UserRole.TRAINER)) {
            Long actorId = role == UserRole.MEMBER ? existingMemberUserId : trainerUserId;
            mockMvc.perform(post("/api/v1/members/{id}/account", memberId)
                    .with(identity(actorId, role))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isForbidden());
        }

        // Lễ tân chi nhánh khác -> 404 (giấu tồn tại hồ sơ ngoài chi nhánh)
        mockMvc.perform(post("/api/v1/members/{id}/account", memberId)
                .with(identity(otherReceptionistUserId, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound());

        // Hồ sơ không tồn tại hoặc đã xóa mềm -> 404
        mockMvc.perform(post("/api/v1/members/{id}/account", 999999999L)
                .with(identity(receptionistUserId, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/members/{id}/account", softDeletedMemberId)
                .with(identity(receptionistUserId, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound());

        // SUPER_ADMIN tạo được cho hồ sơ ở chi nhánh bất kỳ (CN02) -> 200
        String adminCreatedEmail = "other.branch.member" + EMAIL_DOMAIN;
        mockMvc.perform(post("/api/v1/members/{id}/account", otherBranchMemberId)
                .with(identity(adminUserId, UserRole.SUPER_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", adminCreatedEmail))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.memberId").value(otherBranchMemberId))
            .andExpect(jsonPath("$.email").value(adminCreatedEmail));
    }

    @Test
    @DisplayName("TC-F2.2.1-06b: gửi email gặp lỗi vẫn trả 200, giữ nguyên tài khoản và token trong DB để gửi lại bằng Quên mật khẩu")
    void emailProviderFailureStillReturns200AndCommitsAccount() throws Exception {
        Long memberId = insertMember("HV880008", "0978100008", branchId, null, "ACTIVE");
        String email = "mail.fail" + EMAIL_DOMAIN;
        doThrow(new RuntimeException("Brevo tạm thời quá tải"))
            .when(emailSender).send(eq(email), eq(MemberAccountService.ACTIVATION_EMAIL_SUBJECT), anyString());

        MvcResult result = mockMvc.perform(post("/api/v1/members/{id}/account", memberId)
                .with(identity(receptionistUserId, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", email))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.memberId").value(memberId))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.userId").isNumber())
            .andReturn();

        Long userId = jsonMapper.readTree(result.getResponse().getContentAsString()).get("userId").asLong();

        // Tài khoản, liên kết members.user_id và password_resets vẫn được lưu (không rollback)
        assertThat(jdbcTemplate.queryForObject("SELECT user_id FROM members WHERE id = ?", Long.class, memberId))
            .isEqualTo(userId);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM password_resets WHERE user_id = ?",
            Integer.class, userId)).isEqualTo(1);

        // Khi Brevo hoạt động lại, lễ tân có thể gửi lại liên kết qua trang Quên mật khẩu
        reset(emailSender);
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", email))))
            .andExpect(status().isOk());
        verify(emailSender).send(eq(email), anyString(), anyString());
    }

    @Test
    @DisplayName("TC-F2.2.1-07e: email trống, sai định dạng, vượt quá 255 ký tự hoặc id không dương trả 400 kèm errors")
    void invalidEmailOrNonPositiveMemberIdReturns400() throws Exception {
        Long memberId = insertMember("HV880009", "0978100009", branchId, null, "ACTIVE");
        RequestPostProcessor auth = identity(receptionistUserId, UserRole.RECEPTIONIST);

        mockMvc.perform(post("/api/v1/members/{id}/account", memberId)
                .with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", ""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email").exists());

        mockMvc.perform(post("/api/v1/members/{id}/account", memberId)
                .with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", "khong-phai-email"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email").exists());

        String tooLongEmail = "a".repeat(245) + "@example.com";
        mockMvc.perform(post("/api/v1/members/{id}/account", memberId)
                .with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", tooLongEmail))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email").exists());

        mockMvc.perform(post("/api/v1/members/{id}/account", 0L)
                .with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", "valid" + EMAIL_DOMAIN))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.id").value("ID hội viên phải lớn hơn 0"));
    }

    @FunctionalInterface
    private interface StatusAction {
        int run() throws Exception;
    }

    private List<Integer> runConcurrently(StatusAction first, StatusAction second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (StatusAction action : List.of(first, second)) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                    return action.run();
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(15, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    private Long insertUser(String email, String role) {
        return jdbcTemplate.queryForObject(
            "INSERT INTO users(email, password_hash, role, status) VALUES (?, '$2a$10$dummyhash', ?, 'ACTIVE') RETURNING id",
            Long.class, email, role);
    }

    private void insertStaff(Long userId, Long branchId, String code) {
        jdbcTemplate.update(
            "INSERT INTO staff(user_id, branch_id, staff_code, full_name, status) VALUES (?, ?, ?, 'Nhân viên', 'ACTIVE')",
            userId, branchId, code);
    }

    private Long insertMember(String code, String phone, Long branchId, Long userId, String status) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO members(member_code, phone, home_branch_id, user_id, full_name, status)
            VALUES (?, ?, ?, ?, 'Hội viên kiểm thử', ?) RETURNING id
            """, Long.class, code, phone, branchId, userId, status);
    }

    private RequestPostProcessor identity(Long userId, UserRole role) {
        return authentication(new UsernamePasswordAuthenticationToken(
            userId.toString(), "test-only", List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
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
