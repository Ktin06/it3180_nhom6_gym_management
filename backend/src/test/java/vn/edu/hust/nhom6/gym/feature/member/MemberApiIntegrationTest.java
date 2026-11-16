package vn.edu.hust.nhom6.gym.feature.member;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.member.dto.CreateMemberRequest;
import vn.edu.hust.nhom6.gym.feature.member.dto.UpdateMemberRequest;
import vn.edu.hust.nhom6.gym.feature.member.entity.MemberStatus;
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
@Import({MemberTestConfiguration.class, StandaloneMemberAccountTestConfiguration.class})
class MemberApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
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
        mvc.perform(post("/api/v1/members").with(identity(receptionist, UserRole.RECEPTIONIST))
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
                .with(identity(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.content[0].memberCode").value("HV000902"));
        mvc.perform(get("/api/v1/members").param("keyword", "+84 912 345 601")
                .with(identity(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].email").value("member@example.com"));
        mvc.perform(get("/api/v1/members").param("keyword", "HV000903")
                .with(identity(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/members").with(identity(admin, UserRole.SUPER_ADMIN)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HV000901", "hv000901", "Hv000901", " hv000901 "})
    void memberCodeSearchIsExactAndIgnoresCase(String keyword) throws Exception {
        profile("HV000901", "0912345601", branch, null, "Nguyễn An");
        profile("HV000902", "0912345602", branch, null, "hv000901 tên khác");
        mvc.perform(get("/api/v1/members").with(identity(receptionist, UserRole.RECEPTIONIST))
                .param("keyword", keyword))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].memberCode").value("HV000901"));
    }

    @Test
    void phonePrefixSearchRespectsBranchStatusSoftDeletionAndPagination() throws Exception {
        profile("HV000901", "0912345601", branch, memberUser, "An");
        profile("HV000902", "0912345602", branch, null, "Bình");
        profile("HV000903", "0912345603", otherBranch, null, "Chi");
        Long deleted = profile("HV000904", "0912345604", branch, null, "Dũng");
        jdbc.update("UPDATE members SET deleted_at = now() WHERE id = ?", deleted);
        Long inactive = profile("HV000905", "0912345605", branch, null, "Giang");
        jdbc.update("UPDATE members SET status = 'INACTIVE' WHERE id = ?", inactive);
        profile("HV000906", "0909123401", branch, null, "0912 tên không được khớp");
        RequestPostProcessor auth = identity(receptionist, UserRole.RECEPTIONIST);

        mvc.perform(get("/api/v1/members").with(auth)
                .param("keyword", "0912").param("status", "ACTIVE").param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.content[0].memberCode").value("HV000902"))
            .andExpect(jsonPath("$.content[0].email").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(get("/api/v1/members").with(auth)
                .param("keyword", "0912").param("status", "ACTIVE").param("size", "1").param("page", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].memberCode").value("HV000901"))
            .andExpect(jsonPath("$.content[0].email").value("member@example.com"));
        mvc.perform(get("/api/v1/members").with(identity(admin, UserRole.SUPER_ADMIN))
                .param("keyword", "0912").param("status", "ACTIVE"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/v1/members").with(auth).param("keyword", "0999"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"5678", "1234", "56 78", "84912", "+84 123", "+84 912", "8491234567", "+84 912 345 67"})
    void shortNumericKeywordsWithoutMatchingPrefixReturnAnEmptyPage(String keyword) throws Exception {
        profile("HV000901", "0912345678", branch, null, "5678 tên không được khớp");
        profile("HV000902", "0849123456", branch, null, "Hội viên khác");
        mvc.perform(get("/api/v1/members").with(identity(receptionist, UserRole.RECEPTIONIST))
                .param("keyword", keyword).param("status", "ACTIVE").param("page", "0").param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0))
            .andExpect(jsonPath("$.content").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "09123456789", "+85912345678"})
    void longInvalidNumericKeywordsReturnAnEmptyPage(String keyword) throws Exception {
        profile("HV000901", "0912345678", branch, null, "An");
        mvc.perform(get("/api/v1/members").with(identity(receptionist, UserRole.RECEPTIONIST))
                .param("keyword", keyword))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.content").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0912345601", "+84 912 345 601", "84912345601", "0912.345-601"})
    void completePhoneSearchStillNormalizesAndMatchesExactly(String keyword) throws Exception {
        profile("HV000901", "0912345601", branch, null, "An");
        profile("HV000902", "0912345602", branch, null, "Bình");
        mvc.perform(get("/api/v1/members").with(identity(receptionist, UserRole.RECEPTIONIST))
                .param("keyword", keyword))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].phone").value("0912345601"));
    }

    @ParameterizedTest
    @MethodSource("validBirthDates")
    void createAndUpdateAcceptPastOrAbsentBirthDate(LocalDate dob) throws Exception {
        RequestPostProcessor auth = identity(receptionist, UserRole.RECEPTIONIST);
        var expected = dob == null ? org.hamcrest.Matchers.nullValue() : org.hamcrest.Matchers.is(dob.toString());
        mvc.perform(post("/api/v1/members").with(auth).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new CreateMemberRequest("Nguyễn An", "0912345699", null, dob))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.dob").value(expected));
        Long id = jdbc.queryForObject("SELECT id FROM members WHERE phone = '0912345699'", Long.class);
        // Có ngày sinh cũ để kiểm tra PUT có thể thay đổi hoặc xóa ngày sinh tùy chọn.
        jdbc.update("UPDATE members SET dob = '1980-01-01' WHERE id = ?", id);
        mvc.perform(put("/api/v1/members/{id}", id).with(auth).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new UpdateMemberRequest("Nguyễn An", "0912345699", null, dob, MemberStatus.ACTIVE))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.dob").value(expected));
        assertThat(jdbc.queryForObject("SELECT dob::text FROM members WHERE id = ?", String.class, id))
            .isEqualTo(dob == null ? null : dob.toString());
    }

    @ParameterizedTest
    @MethodSource("invalidBirthDates")
    void createAndUpdateRejectTodayOrFutureBirthDateWithoutChanges(LocalDate dob) throws Exception {
        Long id = profile("HV000901", "0912345601", branch, memberUser, "An");
        jdbc.update("UPDATE members SET dob = '2000-05-20' WHERE id = ?", id);
        refreshToken(memberUser);
        RequestPostProcessor auth = identity(receptionist, UserRole.RECEPTIONIST);
        mvc.perform(post("/api/v1/members").with(auth).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new CreateMemberRequest("Tên mới", "0912345699", null, dob))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.dob").value("Ngày sinh phải ở trong quá khứ"));
        mvc.perform(put("/api/v1/members/{id}", id).with(auth).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new UpdateMemberRequest("Tên mới", "0912345699", null, dob, MemberStatus.INACTIVE))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.dob").value("Ngày sinh phải ở trong quá khứ"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM members", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT full_name FROM members WHERE id = ?", String.class, id)).isEqualTo("An");
        assertThat(jdbc.queryForObject("SELECT phone FROM members WHERE id = ?", String.class, id)).isEqualTo("0912345601");
        assertThat(jdbc.queryForObject("SELECT dob::text FROM members WHERE id = ?", String.class, id)).isEqualTo("2000-05-20");
        assertThat(jdbc.queryForObject("SELECT status FROM members WHERE id = ?", String.class, id)).isEqualTo("ACTIVE");
        assertThat(accountStatus(memberUser)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE revoked_at IS NULL", Integer.class)).isEqualTo(1);
    }

    static Stream<LocalDate> validBirthDates() { return Stream.of(null, LocalDate.of(2000, 5, 20)); }

    static Stream<LocalDate> invalidBirthDates() { return Stream.of(LocalDate.now(), LocalDate.now().plusDays(1)); }

    @Test
    void validatesEndpointAndRecordPermissions() throws Exception {
        Long id = profile("HV000901", "0912345601", branch, memberUser, "An");
        mvc.perform(get("/api/v1/members")).andExpect(status().isUnauthorized());
        for (var role : List.of(UserRole.MEMBER, UserRole.TRAINER)) {
            mvc.perform(get("/api/v1/members").with(identity(memberUser, role)))
                .andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/members").with(identity(memberUser, role))
                    .contentType(MediaType.APPLICATION_JSON).content(createBody("0912345699")))
                .andExpect(status().isForbidden());
            mvc.perform(put("/api/v1/members/{id}", id).with(identity(memberUser, role))
                    .contentType(MediaType.APPLICATION_JSON).content(updateBody("0912345601", "INACTIVE")))
                .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/members/{id}", id).with(identity(memberUser, UserRole.MEMBER)))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/members/{id}", id).with(identity(otherReceptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/members/{id}", id).with(identity(trainer, UserRole.TRAINER)))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/members/{id}", id)
                .with(identity(otherReceptionist, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON).content(updateBody("0912345601", "INACTIVE")))
            .andExpect(status().isNotFound());
        Long stranger = user("stranger@example.com", "MEMBER");
        mvc.perform(get("/api/v1/members/{id}", id).with(identity(stranger, UserRole.MEMBER)))
            .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource("invalidFullNames")
    void invalidNamesAreRejectedByRequestValidationOnCreateAndUpdate(String name) throws Exception {
        Long id = profile("HV000901", "0912345601", branch, null, "An");
        RequestPostProcessor auth = identity(receptionist, UserRole.RECEPTIONIST);
        mvc.perform(post("/api/v1/members").with(auth).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new CreateMemberRequest(name, "0912345699", null, null))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.fullName").exists());
        mvc.perform(put("/api/v1/members/{id}", id).with(auth).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new UpdateMemberRequest(name, "0912345601", null, null, MemberStatus.ACTIVE))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.fullName").exists());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM members", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT full_name FROM members WHERE id = ?", String.class, id)).isEqualTo("An");
    }

    @Test
    void missingUpdateStatusIsRejectedByRequestValidation() throws Exception {
        Long id = profile("HV000901", "0912345601", branch, null, "An");
        mvc.perform(put("/api/v1/members/{id}", id).with(identity(receptionist, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new UpdateMemberRequest("Tên mới", "0912345601", null, null, null))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.status").exists());
        assertThat(jdbc.queryForObject("SELECT full_name FROM members WHERE id = ?", String.class, id)).isEqualTo("An");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void nonPositiveIdsAreRejectedAtController(Long id) throws Exception {
        RequestPostProcessor auth = identity(receptionist, UserRole.RECEPTIONIST);
        mvc.perform(get("/api/v1/members/{id}", id).with(auth))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.id").value("ID hội viên phải lớn hơn 0"));
        mvc.perform(put("/api/v1/members/{id}", id).with(auth)
                .contentType(MediaType.APPLICATION_JSON).content(updateBody("0912345601", "ACTIVE")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.id").value("ID hội viên phải lớn hơn 0"));
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrationInputs")
    void directRegistrationUsesTheSameDtoConstraints(String name, String phone, LocalDate dob, String field) {
        assertThatThrownBy(() -> registration.registerMemberProfile(memberUser, name, phone, dob, null))
            .isInstanceOfSatisfying(ConstraintViolationException.class, failure ->
                assertThat(failure.getConstraintViolations()).extracting(v -> v.getPropertyPath().toString())
                    .contains(field));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM members", Integer.class)).isZero();
    }

    @Test
    void branchManagerCanCreateReadUpdateAndSearchWithinTheirBranch() throws Exception {
        Long manager = user("manager@example.com", "BRANCH_MANAGER");
        staff(manager, branch, "NV03");
        RequestPostProcessor auth = identity(manager, UserRole.BRANCH_MANAGER);
        mvc.perform(post("/api/v1/members").with(auth).contentType(MediaType.APPLICATION_JSON)
                .content(createBody("0912345699")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.homeBranchId").value(branch.intValue()));
        Long id = jdbc.queryForObject("SELECT id FROM members WHERE phone = '0912345699'", Long.class);
        mvc.perform(get("/api/v1/members/{id}", id).with(auth)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/members/{id}", id).with(auth).contentType(MediaType.APPLICATION_JSON)
                .content(updateBody("0912345699", "INACTIVE")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        mvc.perform(get("/api/v1/members").with(auth).param("keyword", "0912"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    static Stream<String> invalidFullNames() { return Stream.of(null, "", "   ", "x".repeat(256)); }

    static Stream<Arguments> invalidRegistrationInputs() {
        return Stream.of(
            Arguments.of(null, "0912345699", null, "fullName"),
            Arguments.of("   ", "0912345699", null, "fullName"),
            Arguments.of("x".repeat(256), "0912345699", null, "fullName"),
            Arguments.of("An", "123", null, "phone"),
            Arguments.of("An", "0912345699", LocalDate.now(), "dob"),
            Arguments.of("An", "0912345699", LocalDate.now().plusDays(1), "dob")
        );
    }

    @Test
    void invalidBodiesAndPaginationAre400AndDuplicateIs409() throws Exception {
        RequestPostProcessor auth = identity(receptionist, UserRole.RECEPTIONIST);
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

    @ParameterizedTest
    @ValueSource(strings = {"123", "5678", "84912"})
    void incompletePhoneCannotBeSavedOnCreateOrUpdate(String phone) throws Exception {
        Long id = profile("HV000901", "0912345601", branch, null, "An");
        RequestPostProcessor auth = identity(receptionist, UserRole.RECEPTIONIST);
        mvc.perform(post("/api/v1/members").with(auth)
                .contentType(MediaType.APPLICATION_JSON).content(createBody(phone)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.phone").exists());
        mvc.perform(put("/api/v1/members/{id}", id).with(auth)
                .contentType(MediaType.APPLICATION_JSON).content(updateBody(phone, "ACTIVE")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.phone").exists());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM members", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT phone FROM members WHERE id = ?", String.class, id))
            .isEqualTo("0912345601");
    }

    @Test
    void updateSamePhoneAndDisableRevokeAndActivate() throws Exception {
        Long id = profile("HV000901", "0912345601", branch, memberUser, "An");
        refreshToken(memberUser);
        mvc.perform(put("/api/v1/members/{id}", id).with(identity(receptionist, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON).content(updateBody("0912345601", "INACTIVE")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        assertThat(accountStatus(memberUser)).isEqualTo("DISABLED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE revoked_at IS NULL", Integer.class)).isZero();
        mvc.perform(put("/api/v1/members/{id}", id).with(identity(receptionist, UserRole.RECEPTIONIST))
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
        mvc.perform(put("/api/v1/members/{id}", id).with(identity(receptionist, UserRole.RECEPTIONIST))
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
        RequestPostProcessor auth = identity(admin, UserRole.SUPER_ADMIN);
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
        mvc.perform(get("/api/v1/members").with(identity(admin, UserRole.SUPER_ADMIN))
                .param("keyword", "An%_").param("status", "INACTIVE"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].memberCode").value("HV000901"));
    }

    @Test
    void duplicateUpdateIs409AndUnassignedStaffCannotAccess() throws Exception {
        Long id = profile("HV000901", "0912345601", branch, null, "An");
        profile("HV000902", "0912345602", branch, null, "Bình");
        mvc.perform(put("/api/v1/members/{id}", id).with(identity(receptionist, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON).content(updateBody("0912345602", "ACTIVE")))
            .andExpect(status().isConflict());
        Long unassigned = user("unassigned@example.com", "RECEPTIONIST");
        mvc.perform(get("/api/v1/members").with(identity(unassigned, UserRole.RECEPTIONIST)))
            .andExpect(status().isForbidden());
    }

    @Test
    void concurrentUpdatesKeepMemberAndAccountStatusesConsistent() throws Exception {
        Long id = profile("HV000901", "0912345601", branch, memberUser, "An");
        RequestPostProcessor auth = identity(admin, UserRole.SUPER_ADMIN);
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
        RequestPostProcessor auth = identity(receptionist, UserRole.RECEPTIONIST);
        // Làm nóng Spring/JPA trước khi đo ba cách tìm theo hợp đồng NF1.
        mvc.perform(get("/api/v1/members").with(auth)).andExpect(status().isOk());
        for (String term : List.of("HV100001", "hv100001", "0900000001", "0900", "Hiệu năng")) {
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

    private RequestPostProcessor identity(Long userId, UserRole role) {
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
