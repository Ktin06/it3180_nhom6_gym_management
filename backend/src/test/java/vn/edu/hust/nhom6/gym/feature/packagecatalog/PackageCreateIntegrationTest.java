package vn.edu.hust.nhom6.gym.feature.packagecatalog;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.repository.PackageRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** API thật, JWT F1, schema chuẩn, DB UNIQUE và rollback; không mock service nghiệp vụ. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PackageCatalogIntegrationTest.Database.class)
class PackageCreateIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired JsonMapper json;
    @MockitoSpyBean PackageRepository packages;
    Long branch;
    Long manager;
    static final String VALID = """
        {"code":"NEW","name":"Gói mới","type":"TIME","durationDays":30,"price":500000}
        """;

    @BeforeEach
    void fixtures() {
        jdbc.execute("TRUNCATE TABLE users, packages RESTART IDENTITY CASCADE");
        branch = jdbc.queryForObject("SELECT id FROM branches WHERE code='CN01'", Long.class);
        manager = jdbc.queryForObject("INSERT INTO users(email,password_hash,role) VALUES ('create-f3@example.com','test-only','BRANCH_MANAGER') RETURNING id", Long.class);
        jdbc.update("INSERT INTO staff(user_id,branch_id,staff_code,full_name) VALUES (?,?,'F3CREATE','Quản lý test')", manager, branch);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TIME", "SESSION", "PT"})
    void createsAllTypesWithServerBranchAuditAndReadVisibility(String type) throws Exception {
        var body = json.readTree(VALID).deepCopy();
        // ObjectNode từ JSON mẫu giúp mỗi request có cùng dữ liệu, chỉ thay loại/số buổi.
        var object = (tools.jackson.databind.node.ObjectNode) body;
        object.put("type", type);
        if (!"TIME".equals(type)) { object.put("sessionCount", 10); }
        var result = mvc.perform(post("/api/v1/packages").with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(object)))
            .andExpect(status().isCreated()).andExpect(header().string("Location", matchesPattern("/api/v1/packages/[0-9]+")))
            .andExpect(jsonPath("$.code").value("NEW")).andExpect(jsonPath("$.name").value("Gói mới"))
            .andExpect(jsonPath("$.type").value(type)).andExpect(jsonPath("$.price").value(500000))
            .andExpect(jsonPath("$.branchId").value(branch.intValue())).andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty()).andExpect(jsonPath("$.updatedAt").isNotEmpty())
            .andExpect(jsonPath("$.description").value(nullValue())).andReturn();
        var response = json.readTree(result.getResponse().getContentAsString());
        assertThat(response.get("sessionCount").isNull()).isEqualTo("TIME".equals(type));
        assertThat(jdbc.queryForObject("SELECT branch_id FROM packages WHERE code='NEW'", Long.class)).isEqualTo(branch);
        mvc.perform(get("/api/v1/packages").with(bearer("RECEPTIONIST")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].code").value("NEW"));
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
            Arguments.of("code", "null"), Arguments.of("code", "\" \""), Arguments.of("name", "null"),
            Arguments.of("name", "\"\""), Arguments.of("type", "null"), Arguments.of("type", "\"INVALID\""),
            Arguments.of("type", "0"), Arguments.of("durationDays", "0"), Arguments.of("durationDays", "-1"),
            Arguments.of("durationDays", "null"), Arguments.of("price", "null"), Arguments.of("price", "-1"),
            Arguments.of("price", "1.5"), Arguments.of("price", "\"500000\""), Arguments.of("price", "2147483648"),
            Arguments.of("sessionCount", "1"), Arguments.of("description", "42"), Arguments.of("name", "42"));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void invalidInputsReturnFieldErrorsWithoutWrites(String field, String raw) throws Exception {
        var body = (tools.jackson.databind.node.ObjectNode) json.readTree(VALID);
        body.set(field, json.readTree(raw));
        mvc.perform(post("/api/v1/packages").with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors." + field).isNotEmpty());
        assertThat(count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SESSION", "PT"})
    void sessionsAreRequiredAndPositive(String type) throws Exception {
        var body = (tools.jackson.databind.node.ObjectNode) json.readTree(VALID);
        body.put("type", type);
        for (Integer sessions : new Integer[] {null, 0, -1}) {
            if (sessions == null) { body.putNull("sessionCount"); } else { body.put("sessionCount", sessions); }
            mvc.perform(post("/api/v1/packages").with(bearer("BRANCH_MANAGER"))
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.sessionCount").isNotEmpty());
        }
        body.remove("sessionCount");
        mvc.perform(post("/api/v1/packages").with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.sessionCount").isNotEmpty());
        assertThat(count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "branchId", "status", "createdAt", "unknownField"})
    void rejectsServerManagedAndUnknownFields(String field) throws Exception {
        var body = (tools.jackson.databind.node.ObjectNode) json.readTree(VALID);
        body.put(field, "forged");
        mvc.perform(post("/api/v1/packages").with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isMap());
        assertThat(count()).isZero();
    }

    @Test
    void missingFieldsMalformedJsonAndLengthLimitsAreRejected() throws Exception {
        for (String body : List.of("{}", "{", "null", "")) {
            mvc.perform(post("/api/v1/packages").with(bearer("BRANCH_MANAGER"))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        for (String field : List.of("code", "name")) {
            var body = (tools.jackson.databind.node.ObjectNode) json.readTree(VALID);
            body.put(field, "A".repeat(field.equals("code") ? 31 : 256));
            mvc.perform(post("/api/v1/packages").with(bearer("BRANCH_MANAGER"))
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors." + field).isNotEmpty());
        }
        assertThat(count()).isZero();
    }

    @Test
    void acceptsZeroPriceAndMaximumLengthsAndIntegers() throws Exception {
        var body = (tools.jackson.databind.node.ObjectNode) json.readTree(VALID);
        body.put("code", "A".repeat(30)); body.put("name", "B".repeat(255));
        body.put("price", 0); body.put("durationDays", Integer.MAX_VALUE); body.putNull("sessionCount");
        mvc.perform(post("/api/v1/packages").with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.price").value(0));
        body.put("code", "MAX"); body.put("price", Integer.MAX_VALUE);
        mvc.perform(post("/api/v1/packages").with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.price").value(Integer.MAX_VALUE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUPER_ADMIN", "RECEPTIONIST", "MEMBER", "TRAINER"})
    void onlyManagerCanCreate(String role) throws Exception {
        mvc.perform(post("/api/v1/packages").with(bearer(role)).contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isForbidden());
        assertThat(count()).isZero();
    }

    @Test
    void missingTokenOrInactiveStaffCannotCreate() throws Exception {
        mvc.perform(post("/api/v1/packages").contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isUnauthorized());
        jdbc.update("UPDATE staff SET status='INACTIVE' WHERE user_id=?", manager);
        mvc.perform(post("/api/v1/packages").with(bearer("BRANCH_MANAGER")).contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isForbidden());
        jdbc.update("DELETE FROM staff WHERE user_id=?", manager);
        mvc.perform(post("/api/v1/packages").with(bearer("BRANCH_MANAGER")).contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isForbidden());
        assertThat(count()).isZero();
    }

    @Test
    void duplicateInactiveGlobalCodeIsConflict() throws Exception {
        jdbc.update("INSERT INTO packages(code,name,type,duration_days,price,status) VALUES ('NEW','Old','TIME',30,1,'INACTIVE')");
        mvc.perform(post("/api/v1/packages").with(bearer("BRANCH_MANAGER")).contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Mã gói 'NEW' đã tồn tại trên hệ thống."));
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void concurrentDuplicateCodeCreatesOneAndRollsBackOther() throws Exception {
        var barrier = new CyclicBarrier(2);
        doAnswer(invocation -> {
            // Spring Data là interface proxy, không gọi callRealMethod trên abstract method.
            // Đọc DB thật rồi đồng bộ hai request trước INSERT để buộc đi qua nhánh UNIQUE.
            boolean exists = Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM packages WHERE code=?)", Boolean.class, "NEW"));
            barrier.await(15, TimeUnit.SECONDS);
            return exists;
        }).when(packages).existsByCode("NEW");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = Stream.of(1, 2).map(i -> executor.submit(() ->
                mvc.perform(post("/api/v1/packages").with(bearer("BRANCH_MANAGER"))
                    .contentType(MediaType.APPLICATION_JSON).content(VALID)).andReturn().getResponse())).toList();
            var first = tasks.get(0).get(25, TimeUnit.SECONDS);
            var second = tasks.get(1).get(25, TimeUnit.SECONDS);
            assertThat(List.of(first.getStatus(), second.getStatus())).containsExactlyInAnyOrder(201, 409);
            var conflict = first.getStatus() == 409 ? first : second;
            assertThat(json.readTree(conflict.getContentAsString()).get("detail").asString())
                .isEqualTo("Mã gói 'NEW' đã tồn tại trên hệ thống.");
        }
        assertThat(count()).isEqualTo(1);
    }

    private long count() { return jdbc.queryForObject("SELECT count(*) FROM packages", Long.class); }
    private RequestPostProcessor bearer(String role) {
        String token = tokens.generateAccessToken(manager, "create-f3@example.com", UserRole.valueOf(role));
        return request -> { request.addHeader("Authorization", "Bearer " + token); return request; };
    }
}
