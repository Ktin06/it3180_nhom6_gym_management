package vn.edu.hust.nhom6.gym.feature.member;

import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberActor.Role;
import vn.edu.hust.nhom6.gym.feature.member.service.integration.MemberAccountAccess;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberRegistrationService;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/** Kiểm thử riêng F2 với Authentication và adapter test; không kiểm thử triển khai hay JWT của F1. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MemberTestConfiguration.class)
@Sql("/db/fixtures/f2-staff.sql")
class MemberApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean MemberAccountAccess accounts;
    @Autowired MemberRegistrationService registration;
    @Autowired PlatformTransactionManager transactions;
    Long admin;
    Long receptionist;
    Long otherReceptionist;
    Long memberUser;
    Long trainer;
    Long branch;
    Long otherBranch;

    @BeforeEach
    void fixtures() {
        jdbc.execute("TRUNCATE TABLE members, users, staff, refresh_tokens RESTART IDENTITY CASCADE");
        branch = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN01'", Long.class);
        jdbc.update("""
            INSERT INTO branches(code, name, open_time, close_time) VALUES ('CN02','Chi nhánh kiểm thử','06:00','22:00')
            ON CONFLICT(code) DO NOTHING
            """);
        otherBranch = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN02'", Long.class);
        admin = user("admin@example.com", "SUPER_ADMIN");
        receptionist = user("reception@example.com", "RECEPTIONIST");
        otherReceptionist = user("other@example.com", "RECEPTIONIST");
        memberUser = user("member@example.com", "MEMBER");
        trainer = user("trainer@example.com", "TRAINER");
        staff(receptionist, branch, "NV01");
        staff(otherReceptionist, otherBranch, "NV02");
    }

    @Test
    void createNormalizesAndReturnsLocationAndAudit() throws Exception {
        mvc.perform(post("/api/v1/members").with(identity(receptionist, Role.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON).content(createBody("+84 912 345 678")))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.memberCode").value(org.hamcrest.Matchers.matchesPattern("HV[0-9]{6}")))
            .andExpect(jsonPath("$.phone").value("0912345678"))
            .andExpect(jsonPath("$.homeBranchId").value(branch.intValue()))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
        assertThat(jdbc.queryForObject("SELECT created_by FROM members", Long.class)).isEqualTo(receptionist);
        assertThat(jdbc.queryForObject("SELECT avatar_url FROM members", String.class)).isNull();
    }

    @Test
    void searchScopesBothRowsAndCountAndIncludesUnlinkedProfiles() throws Exception {
        profile("HV000901", "0912345601", branch, memberUser, "Nguyễn An");
        profile("HV000902", "0912345602", branch, null, "Nguyễn Bình");
        profile("HV000903", "0912345603", otherBranch, null, "Nguyễn Chi");
        Long deleted = profile("HV000904", "0912345604", branch, null, "Nguyễn Dũng");
        jdbc.update("UPDATE members SET deleted_at = now() WHERE id = ?", deleted);
        mvc.perform(get("/api/v1/members").param("keyword", "Nguyễn").param("size", "1")
                .with(identity(receptionist, Role.RECEPTIONIST)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.content[0].memberCode").value("HV000902"));
        mvc.perform(get("/api/v1/members").param("keyword", "+84 912 345 601")
                .with(identity(receptionist, Role.RECEPTIONIST)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].email").value("member@example.com"));
        mvc.perform(get("/api/v1/members").param("keyword", "HV000903")
                .with(identity(receptionist, Role.RECEPTIONIST)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/members").with(identity(admin, Role.SUPER_ADMIN)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void validatesEndpointAndRecordPermissions() throws Exception {
        Long id = profile("HV000901", "0912345601", branch, memberUser, "An");
        mvc.perform(get("/api/v1/members")).andExpect(status().isUnauthorized());
        for (var role : List.of(Role.MEMBER, Role.TRAINER)) {
            mvc.perform(get("/api/v1/members").with(identity(memberUser, role)))
                .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/members/{id}", id).with(identity(memberUser, Role.MEMBER)))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/members/{id}", id).with(identity(otherReceptionist, Role.RECEPTIONIST)))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/members/{id}", id).with(identity(trainer, Role.TRAINER)))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/members/{id}", id)
                .with(identity(otherReceptionist, Role.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON).content(updateBody("0912345601", "INACTIVE")))
            .andExpect(status().isNotFound());
        Long stranger = user("stranger@example.com", "MEMBER");
        mvc.perform(get("/api/v1/members/{id}", id).with(identity(stranger, Role.MEMBER)))
            .andExpect(status().isNotFound());
    }

    @Test
    void invalidBodiesAndPaginationAre400AndDuplicateIs409() throws Exception {
        RequestPostProcessor auth = identity(receptionist, Role.RECEPTIONIST);
        for (String phone : List.of("123", "09123abc78", "")) {
            mvc.perform(post("/api/v1/members").with(auth)
                    .contentType(MediaType.APPLICATION_JSON).content(createBody(phone)))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/members").param("size", "101").with(auth))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/members").param("status", "WRONG").with(auth))
            .andExpect(status().isBadRequest());
        profile("HV000901", "0912345601", branch, null, "An");
        mvc.perform(post("/api/v1/members").with(auth)
                .contentType(MediaType.APPLICATION_JSON).content(createBody("0912345601")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updateSamePhoneAndDisableRevokeAndActivate() throws Exception {
        Long id = profile("HV000901", "0912345601", branch, memberUser, "An");
        refreshToken(memberUser);
        mvc.perform(put("/api/v1/members/{id}", id).with(identity(receptionist, Role.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON).content(updateBody("0912345601", "INACTIVE")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        assertThat(accountStatus(memberUser)).isEqualTo("DISABLED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE revoked_at IS NULL", Integer.class)).isZero();
        mvc.perform(put("/api/v1/members/{id}", id).with(identity(receptionist, Role.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON).content(updateBody("0912345601", "ACTIVE")))
            .andExpect(status().isOk());
        assertThat(accountStatus(memberUser)).isEqualTo("ACTIVE");
        jdbc.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", memberUser);
        accounts.activateIfDisabled(memberUser);
        assertThat(accountStatus(memberUser)).isEqualTo("LOCKED");
    }

    @Test
    void failedAccountOperationRollsBackMemberUserAndTokens() throws Exception {
        Long id = profile("HV000901", "0912345601", branch, memberUser, "An");
        refreshToken(memberUser);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("Kiểm thử rollback sau cập nhật tài khoản");
        }).when(accounts).disable(anyLong());
        mvc.perform(put("/api/v1/members/{id}", id).with(identity(receptionist, Role.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON).content(updateBody("0912345601", "INACTIVE")))
            .andExpect(status().isInternalServerError());
        assertThat(jdbc.queryForObject("SELECT status FROM members WHERE id = ?", String.class, id)).isEqualTo("ACTIVE");
        assertThat(accountStatus(memberUser)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE revoked_at IS NULL", Integer.class)).isEqualTo(1);
    }

    @Test
    void registrationPortJoinsCallerTransactionAndRollsBackOnDuplicate() {
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        Long userId = transaction.execute(status -> {
            Long id = user("new@example.com", "MEMBER");
            registration.registerMemberProfile(id, "Khách mới", "0912345699", null, null);
            return id;
        });
        assertThat(jdbc.queryForObject("SELECT user_id FROM members WHERE phone = '0912345699'", Long.class)).isEqualTo(userId);
        assertThatThrownBy(() -> transaction.execute(status -> {
            Long id = user("duplicate@example.com", "MEMBER");
            registration.registerMemberProfile(id, "Trùng số", "0912345699", null, null);
            return id;
        })).isInstanceOf(vn.edu.hust.nhom6.gym.common.exception.ConflictException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = 'duplicate@example.com'", Integer.class)).isZero();
    }

    @Test
    void concurrentRequestsCannotCreateDuplicatePhone() throws Exception {
        RequestPostProcessor auth = identity(admin, Role.SUPER_ADMIN);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> request = () -> {
                start.await();
                return mvc.perform(post("/api/v1/members").with(auth)
                    .contentType(MediaType.APPLICATION_JSON).content(createBody("0912345698")))
                    .andReturn().getResponse().getStatus();
            };
            var first = executor.submit(request);
            var second = executor.submit(request);
            start.countDown();
            assertThat(List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM members WHERE phone = '0912345698'", Integer.class)).isEqualTo(1);
    }

    @Test
    void escapesNameWildcardsAndFiltersStatus() throws Exception {
        Long id = profile("HV000901", "0912345601", branch, null, "An%_ Bình");
        profile("HV000902", "0912345602", branch, null, "An khác");
        jdbc.update("UPDATE members SET status = 'INACTIVE' WHERE id = ?", id);
        mvc.perform(get("/api/v1/members").with(identity(admin, Role.SUPER_ADMIN))
                .param("keyword", "An%_").param("status", "INACTIVE"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].memberCode").value("HV000901"));
    }

    @Test
    void duplicateUpdateIs409AndUnassignedStaffCannotAccess() throws Exception {
        Long id = profile("HV000901", "0912345601", branch, null, "An");
        profile("HV000902", "0912345602", branch, null, "Bình");
        mvc.perform(put("/api/v1/members/{id}", id).with(identity(receptionist, Role.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON).content(updateBody("0912345602", "ACTIVE")))
            .andExpect(status().isConflict());
        Long unassigned = user("unassigned@example.com", "RECEPTIONIST");
        mvc.perform(get("/api/v1/members").with(identity(unassigned, Role.RECEPTIONIST)))
            .andExpect(status().isForbidden());
    }

    @Test
    void concurrentUpdatesKeepMemberAndAccountStatusesConsistent() throws Exception {
        Long id = profile("HV000901", "0912345601", branch, memberUser, "An");
        RequestPostProcessor auth = identity(admin, Role.SUPER_ADMIN);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> disable = () -> {
                start.await();
                return mvc.perform(put("/api/v1/members/{id}", id).with(auth)
                    .contentType(MediaType.APPLICATION_JSON).content(updateBody("0912345601", "INACTIVE")))
                    .andReturn().getResponse().getStatus();
            };
            Callable<Integer> activate = () -> {
                start.await();
                return mvc.perform(put("/api/v1/members/{id}", id).with(auth)
                    .contentType(MediaType.APPLICATION_JSON).content(updateBody("0912345601", "ACTIVE")))
                    .andReturn().getResponse().getStatus();
            };
            var first = executor.submit(disable);
            var second = executor.submit(activate);
            start.countDown();
            assertThat(first.get(30, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(second.get(30, TimeUnit.SECONDS)).isEqualTo(200);
        }
        String memberStatus = jdbc.queryForObject("SELECT status FROM members WHERE id = ?", String.class, id);
        assertThat(accountStatus(memberUser)).isEqualTo(memberStatus.equals("ACTIVE") ? "ACTIVE" : "DISABLED");
    }

    @Test
    void measuresSearchOnTenThousandProfiles() throws Exception {
        jdbc.update("""
            INSERT INTO members(member_code,phone,home_branch_id,full_name)
            SELECT 'HV' || lpad((100000 + n)::text,6,'0'),
                   '09' || lpad(n::text,8,'0'), ?, 'Hiệu năng ' || n
            FROM generate_series(1,10000) n
            """, branch);
        jdbc.execute("ANALYZE members");
        RequestPostProcessor auth = identity(receptionist, Role.RECEPTIONIST);
        // Làm nóng Spring/JPA trước khi đo ba cách tìm theo hợp đồng NF1.
        mvc.perform(get("/api/v1/members").with(auth)).andExpect(status().isOk());
        for (String term : List.of("HV100001", "0900000001", "Hiệu năng")) {
            long start = System.nanoTime();
            mvc.perform(get("/api/v1/members").with(auth).param("keyword", term))
                .andExpect(status().isOk());
            long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            System.out.printf("F2_NF1 keyword=%s profiles=10000 size=20 elapsedMs=%d%n", term, millis);
            assertThat(millis).as("NF1 trong môi trường kiểm thử cục bộ, từ khóa %s", term).isLessThan(2000);
        }
        List<String> plan = jdbc.queryForList("""
            EXPLAIN (ANALYZE, BUFFERS)
            SELECT * FROM members WHERE deleted_at IS NULL AND home_branch_id = ?
            AND lower(full_name) LIKE 'hiệu năng%' ORDER BY id DESC LIMIT 20
            """, String.class, branch);
        System.out.println("F2_NF1 namePrefixPlan=" + String.join(" | ", plan));
    }

    private Long user(String email, String role) {
        return jdbc.queryForObject("INSERT INTO users(email,password_hash,role) VALUES (?, 'hash', ?) RETURNING id",
            Long.class, email, role);
    }

    private void staff(Long userId, Long branchId, String code) {
        jdbc.update("INSERT INTO staff(user_id,branch_id,staff_code,full_name) VALUES (?,?,?,'Nhân viên')", userId, branchId, code);
    }

    private Long profile(String code, String phone, Long branchId, Long userId, String name) {
        return jdbc.queryForObject("""
            INSERT INTO members(member_code,phone,home_branch_id,user_id,full_name) VALUES (?,?,?,?,?) RETURNING id
            """, Long.class, code, phone, branchId, userId, name);
    }

    private void refreshToken(Long userId) {
        jdbc.update("INSERT INTO refresh_tokens(user_id,token_hash,expires_at) VALUES (?, 'test-hash', now() + interval '1 day')", userId);
    }

    private String accountStatus(Long userId) {
        return jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, userId);
    }

    private RequestPostProcessor identity(Long userId, Role role) {
        return authentication(new UsernamePasswordAuthenticationToken(
            userId.toString(), "test-only", List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
    private String createBody(String phone) {
        return "{\"fullName\":\"Nguyễn An\",\"phone\":\"" + phone + "\"}";
    }
    private String updateBody(String phone, String status) {
        return "{\"fullName\":\"Nguyễn An\",\"phone\":\"" + phone + "\",\"status\":\"" + status + "\"}";
    }
}
