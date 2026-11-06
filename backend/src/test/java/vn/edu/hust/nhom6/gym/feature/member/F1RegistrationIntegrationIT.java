package vn.edu.hust.nhom6.gym.feature.member;

import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ClassUtils;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberRegistrationService;
import vn.edu.hust.nhom6.gym.feature.member.service.integration.F1MemberAccountAdapter;
import vn.edu.hust.nhom6.gym.feature.member.service.integration.MemberAccountAccess;
import vn.edu.hust.nhom6.gym.feature.member.service.integration.MemberBranchAccess;
import vn.edu.hust.nhom6.gym.feature.member.service.integration.MemberBranchAdapter;

/**
 * Bài IT chạy riêng trên nền đã có dịch vụ tài khoản F1 (đối chiếu commit 0f4e3de).
 * Chạy bằng -Dtest=F1RegistrationIntegrationIT; không phải bài unit mặc định của nhánh chỉ có F2.
 * Dùng AuthController/AuthService/JWT F1 thật, mọi adapter F2 và service tra cứu production.
 * Cần schema đầy đủ (đối chiếu PR #101), không tạo bảng staff bằng fixture tạm.
 * Các request qua MockMvc với PostgreSQL thật; không thay cho nghiệm thu staging qua trình duyệt.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MemberTestConfiguration.class)
class F1RegistrationIntegrationIT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ApplicationContext context;
    @Autowired JsonMapper json;
    @Autowired PasswordEncoder passwords;
    @Autowired MemberBranchAccess branches;
    @MockitoSpyBean MemberRegistrationService registrations;
    @MockitoSpyBean MemberAccountAccess accounts;

    @BeforeEach
    void fixtures() {
        Class<?> contract = ClassUtils.resolveClassName(
            "vn.edu.hust.nhom6.gym.feature.auth.service.MemberService", context.getClassLoader());
        assertThat(context.getBean(contract)).isSameAs(context.getBean("f2MemberRegistrationAdapter"));
        assertThat(context.getBean(contract)).isNotSameAs(context.getBean("defaultMemberService"));
        assertThat(context.getBeansOfType(MemberAccountAccess.class)).hasSize(1);
        assertThat(accounts).isInstanceOf(F1MemberAccountAdapter.class);
        assertThat(accounts).isSameAs(context.getBean("f1MemberAccountAdapter"));
        assertThat(context.getBeansOfType(MemberBranchAccess.class)).hasSize(1);
        assertThat(branches).isInstanceOf(MemberBranchAdapter.class);
        jdbc.execute("TRUNCATE TABLE members, users, staff, refresh_tokens RESTART IDENTITY CASCADE");
        jdbc.update("UPDATE branches SET status = 'ACTIVE' WHERE code = 'CN01'");
    }

    @Test
    void realF1RegistrationCreatesProfileAndJwtCanReadOwnF2Profile() throws Exception {
        var response = mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body("an@example.com", "+84 912 345 678")))
            .andExpect(status().isCreated()).andExpect(header().exists("Set-Cookie"))
            .andExpect(jsonPath("$.accessToken").isNotEmpty()).andReturn().getResponse();
        Long userId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'an@example.com'", Long.class);
        Long memberId = jdbc.queryForObject("SELECT id FROM members WHERE user_id = ?", Long.class, userId);
        Long branchId = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN01'", Long.class);
        assertThat(jdbc.queryForObject("SELECT phone FROM members WHERE id = ?", String.class, memberId))
            .isEqualTo("0912345678");
        assertThat(jdbc.queryForObject("SELECT member_code FROM members WHERE id = ?", String.class, memberId))
            .matches("HV[0-9]{6}");
        assertThat(jdbc.queryForObject("SELECT home_branch_id FROM members WHERE id = ?", Long.class, memberId))
            .isEqualTo(branchId);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE user_id = ?", Integer.class, userId))
            .isEqualTo(1);
        String token = (String) json.readValue(response.getContentAsString(), Map.class).get("accessToken");
        mvc.perform(get("/api/v1/members/{id}", memberId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("an@example.com"))
            .andExpect(jsonPath("$.phone").value("0912345678"));
        mvc.perform(get("/api/v1/members").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void phoneAlreadyInRealMembersTableRejectsRegistrationAndRollsBackUser() throws Exception {
        Long branchId = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN01'", Long.class);
        jdbc.update("""
            INSERT INTO members(member_code,home_branch_id,full_name,phone)
            VALUES ('HV900001', ?, 'Hồ sơ tạo tại quầy', '0912345678')
            """, branchId);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body("duplicate@example.com", "0912345678")))
            .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = 'duplicate@example.com'", Integer.class))
            .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM members WHERE phone = '0912345678'", Integer.class))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens", Integer.class)).isZero();
    }

    @Test
    void failureAfterPersistingProfileRollsBackBothUserAndMember() throws Exception {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new ConflictException("Kiểm thử lỗi sau khi lưu hồ sơ");
        }).when(registrations).registerMemberProfile(anyLong(), anyString(), anyString(), isNull(), isNull());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body("rollback@example.com", "0912345678")))
            .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = 'rollback@example.com'", Integer.class))
            .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM members WHERE phone = '0912345678'", Integer.class))
            .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens", Integer.class)).isZero();
    }

    @Test
    void listGetsBatchEmailsThroughRealF1AndIncludesUnlinkedProfile() throws Exception {
        Session first = register("an@example.com", "0912345601");
        Session second = register("binh@example.com", "0912345602");
        Long branchId = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN01'", Long.class);
        Long unlinked = jdbc.queryForObject("""
            INSERT INTO members(member_code,home_branch_id,full_name,phone)
            VALUES ('HV900001', ?, 'Hội viên tại quầy', '0912345603') RETURNING id
            """, Long.class, branchId);
        String adminToken = adminToken();

        mvc.perform(get("/api/v1/members").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content[*].email",
                org.hamcrest.Matchers.hasItems("an@example.com", "binh@example.com")));
        verify(accounts).emails(Set.of(first.userId(), second.userId()));
        mvc.perform(get("/api/v1/members/{id}", unlinked).header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").isEmpty());
    }

    @Test
    void disablingRevokesAllMemberSessionsAndReactivationDoesNotRestoreOldTokens() throws Exception {
        Session member = register("member@example.com", "0912345601");
        Session other = register("other@example.com", "0912345602");
        Cookie secondSession = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials("member@example.com")))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("refresh_token");
        assertThat(secondSession).isNotNull();
        assertThat(activeTokens(member.userId())).isEqualTo(2);
        String adminToken = adminToken();

        update(member.memberId(), adminToken, "INACTIVE", 200);
        assertThat(storedStatus("members", member.memberId())).isEqualTo("INACTIVE");
        assertThat(storedStatus("users", member.userId())).isEqualTo("DISABLED");
        assertThat(activeTokens(member.userId())).isZero();
        assertThat(activeTokens(other.userId())).isEqualTo(1);
        verify(accounts).disable(member.userId());
        mvc.perform(post("/api/v1/auth/refresh").cookie(member.refreshToken()))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(secondSession))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials("member@example.com")))
            .andExpect(status().isForbidden());

        update(member.memberId(), adminToken, "ACTIVE", 200);
        assertThat(storedStatus("users", member.userId())).isEqualTo("ACTIVE");
        verify(accounts).activateIfDisabled(member.userId());
        mvc.perform(post("/api/v1/auth/refresh").cookie(member.refreshToken()))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials("member@example.com")))
            .andExpect(status().isOk());
    }

    @Test
    void activatingMemberDoesNotUnlockCurrentlyLockedAccount() throws Exception {
        Session member = register("locked@example.com", "0912345601");
        jdbc.update("UPDATE members SET status = 'INACTIVE' WHERE id = ?", member.memberId());
        jdbc.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", member.userId());

        update(member.memberId(), adminToken(), "ACTIVE", 200);

        assertThat(storedStatus("members", member.memberId())).isEqualTo("ACTIVE");
        assertThat(storedStatus("users", member.userId())).isEqualTo("LOCKED");
        verify(accounts).activateIfDisabled(member.userId());
    }

    @Test
    void failureAfterRealF1DisableRollsBackMemberAccountAndTokenRevocation() throws Exception {
        Session member = register("rollback-disable@example.com", "0912345601");
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("Kiểm thử lỗi sau khi F1 vô hiệu hóa tài khoản");
        }).when(accounts).disable(member.userId());

        update(member.memberId(), adminToken(), "INACTIVE", 500);

        assertThat(storedStatus("members", member.memberId())).isEqualTo("ACTIVE");
        assertThat(storedStatus("users", member.userId())).isEqualTo("ACTIVE");
        assertThat(activeTokens(member.userId())).isEqualTo(1);
        mvc.perform(post("/api/v1/auth/refresh").cookie(member.refreshToken()))
            .andExpect(status().isOk());
    }

    @Test
    void failureAfterRealF1ActivationRollsBackMemberAndAccount() throws Exception {
        Session member = register("rollback-activate@example.com", "0912345601");
        String adminToken = adminToken();
        update(member.memberId(), adminToken, "INACTIVE", 200);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("Kiểm thử lỗi sau khi F1 kích hoạt tài khoản");
        }).when(accounts).activateIfDisabled(member.userId());

        update(member.memberId(), adminToken, "ACTIVE", 500);

        assertThat(storedStatus("members", member.memberId())).isEqualTo("INACTIVE");
        assertThat(storedStatus("users", member.userId())).isEqualTo("DISABLED");
        assertThat(activeTokens(member.userId())).isZero();
    }

    @Test
    void adapterPropagatesRealF1NotFoundExceptionForMissingAccount() {
        assertThatThrownBy(() -> accounts.disable(Long.MAX_VALUE))
            .isInstanceOf(NotFoundException.class).hasMessageContaining("Không tìm thấy tài khoản liên kết");
    }

    @Test
    void realReceptionistJwtCreatesAndManagesCounterProfileInAssignedBranch() throws Exception {
        String token = receptionistToken(true);
        Long branchId = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN01'", Long.class);
        mvc.perform(post("/api/v1/members").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("fullName", "Hội viên tại quầy", "phone", "0912345601"))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.homeBranchId").value(branchId))
            .andExpect(jsonPath("$.userId").isEmpty());
        Long memberId = jdbc.queryForObject("SELECT id FROM members WHERE phone = '0912345601'", Long.class);
        Long staffUserId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'receptionist@example.com'", Long.class);
        assertThat(jdbc.queryForObject("SELECT created_by FROM members WHERE id = ?", Long.class, memberId))
            .isEqualTo(staffUserId);
        mvc.perform(get("/api/v1/members").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        update(memberId, token, "INACTIVE", 200);
    }

    @Test
    void realReceptionistJwtWithoutStaffAssignmentCannotCreateOrListMembers() throws Exception {
        String token = receptionistToken(false);
        mvc.perform(get("/api/v1/members").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/members").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("fullName", "Hội viên tại quầy", "phone", "0912345601"))))
            .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM members", Integer.class)).isZero();
    }

    @Test
    void inactiveDefaultBranchRejectsRealRegistrationAndRollsBackNewAccount() throws Exception {
        jdbc.update("UPDATE branches SET status = 'INACTIVE' WHERE code = 'CN01'");

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body("inactive-cn01@example.com", "0912345601")))
            .andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = 'inactive-cn01@example.com'", Integer.class))
            .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM members", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens", Integer.class)).isZero();
    }

    private String receptionistToken(boolean assigned) throws Exception {
        Long userId = jdbc.queryForObject("""
            INSERT INTO users(email,password_hash,role) VALUES ('receptionist@example.com', ?, 'RECEPTIONIST') RETURNING id
            """, Long.class, passwords.encode("Password123"));
        if (assigned) {
            Long branchId = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN01'", Long.class);
            jdbc.update("""
                INSERT INTO staff(user_id,branch_id,staff_code,full_name) VALUES (?, ?, 'NV900001', 'Lễ tân kiểm thử')
                """, userId, branchId);
        }
        var response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials("receptionist@example.com")))
            .andExpect(status().isOk()).andReturn().getResponse();
        return (String) json.readValue(response.getContentAsString(), Map.class).get("accessToken");
    }

    private Session register(String email, String phone) throws Exception {
        var response = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(email, phone)))
            .andExpect(status().isCreated()).andReturn().getResponse();
        Long userId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        Long memberId = jdbc.queryForObject("SELECT id FROM members WHERE user_id = ?", Long.class, userId);
        Cookie refreshToken = response.getCookie("refresh_token");
        assertThat(refreshToken).isNotNull();
        return new Session(userId, memberId, refreshToken);
    }

    private String adminToken() throws Exception {
        jdbc.update("INSERT INTO users(email,password_hash,role) VALUES ('admin@example.com', ?, 'SUPER_ADMIN')",
            passwords.encode("Password123"));
        var response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials("admin@example.com")))
            .andExpect(status().isOk()).andReturn().getResponse();
        return (String) json.readValue(response.getContentAsString(), Map.class).get("accessToken");
    }

    private String credentials(String email) throws Exception {
        return json.writeValueAsString(Map.of("email", email, "password", "Password123"));
    }

    private void update(Long memberId, String adminToken, String state, int expectedStatus) throws Exception {
        String request = json.writeValueAsString(Map.of("fullName", "Nguyễn An", "phone", "0912345601", "status", state));
        mvc.perform(put("/api/v1/members/{id}", memberId).header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().is(expectedStatus));
    }

    private String storedStatus(String table, Long id) {
        String query = switch (table) {
            case "members" -> "SELECT status FROM members WHERE id = ?";
            case "users" -> "SELECT status FROM users WHERE id = ?";
            default -> throw new IllegalArgumentException("Bảng kiểm thử không hợp lệ");
        };
        return jdbc.queryForObject(query, String.class, id);
    }

    private int activeTokens(Long userId) {
        return jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL",
            Integer.class, userId);
    }

    private record Session(Long userId, Long memberId, Cookie refreshToken) { }

    private String body(String email, String phone) throws Exception {
        return json.writeValueAsString(Map.of("fullName", "Nguyễn An", "email", email,
            "phone", phone, "password", "Password123"));
    }
}
