package vn.edu.hust.nhom6.gym.feature.member;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ClassUtils;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberRegistrationService;

/**
 * Bài IT chạy riêng trên nền đã có F1 (đối chiếu commit 7792436).
 * Chạy bằng -Dtest=F1RegistrationIntegrationIT; không phải bài unit mặc định của nhánh chỉ có F2.
 * Dùng AuthController/AuthService/JWT F1 thật, adapter đăng ký F2 thật và PostgreSQL thật.
 * Dịch vụ tài khoản/chi nhánh vẫn là adapter test, chưa chứng nhận tích hợp production các dịch vụ đó.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MemberTestConfiguration.class)
@Sql("/db/fixtures/f2-staff.sql")
class F1RegistrationIntegrationIT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ApplicationContext context;
    @Autowired JsonMapper json;
    @MockitoSpyBean MemberRegistrationService registrations;

    @BeforeEach
    void fixtures() {
        Class<?> contract = ClassUtils.resolveClassName(
            "vn.edu.hust.nhom6.gym.feature.auth.service.MemberService", context.getClassLoader());
        assertThat(context.getBean(contract)).isSameAs(context.getBean("f2MemberRegistrationAdapter"));
        assertThat(context.getBean(contract)).isNotSameAs(context.getBean("defaultMemberService"));
        jdbc.execute("TRUNCATE TABLE members, users, staff, refresh_tokens RESTART IDENTITY CASCADE");
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

    private String body(String email, String phone) throws Exception {
        return json.writeValueAsString(Map.of("fullName", "Nguyễn An", "email", email,
            "phone", phone, "password", "Password123"));
    }
}
