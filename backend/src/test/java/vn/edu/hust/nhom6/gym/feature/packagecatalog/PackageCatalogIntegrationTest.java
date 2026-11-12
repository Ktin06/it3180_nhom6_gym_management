package vn.edu.hust.nhom6.gym.feature.packagecatalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.postgresql.PostgreSQLContainer;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** PostgreSQL thật, schema Flyway chuẩn, service staff production và JWT ký bởi F1. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PackageCatalogIntegrationTest.Database.class)
class PackageCatalogIntegrationTest {
    @TestConfiguration(proxyBeanMethods = false)
    static class Database {
        @Bean @ServiceConnection
        PostgreSQLContainer packagePostgres() { return new PostgreSQLContainer("postgres:16"); }
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    Long branch;
    Long otherBranch;
    Long employee;

    @BeforeEach
    void fixtures() {
        jdbc.execute("TRUNCATE TABLE users, packages RESTART IDENTITY CASCADE");
        branch = jdbc.queryForObject("SELECT id FROM branches WHERE code='CN01'", Long.class);
        jdbc.update("INSERT INTO branches(code,name,open_time,close_time) VALUES ('F3TEST','F3 test','06:00','22:00') ON CONFLICT(code) DO NOTHING");
        otherBranch = jdbc.queryForObject("SELECT id FROM branches WHERE code='F3TEST'", Long.class);
        employee = jdbc.queryForObject("INSERT INTO users(email,password_hash,role) VALUES ('f3@example.com','test-only','BRANCH_MANAGER') RETURNING id", Long.class);
        jdbc.update("INSERT INTO staff(user_id,branch_id,staff_code,full_name) VALUES (?,?, 'F3NV','F3 test')", employee, branch);
        item("LOCAL", "Same", "TIME", branch, "ACTIVE");
        item("GLOBAL", "Same", "TIME", null, "ACTIVE");
        item("OTHER", "Other", "TIME", otherBranch, "ACTIVE");
        item("SESSION", "Sessions", "SESSION", branch, "ACTIVE");
        item("PT", "Training", "PT", null, "ACTIVE");
        item("OLD", "Old", "TIME", branch, "INACTIVE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"BRANCH_MANAGER", "RECEPTIONIST"})
    void scopesRowsAndCountAndIgnoresClientBranch(String role) throws Exception {
        mvc.perform(get("/api/v1/packages").with(bearer(role)).param("type", "TIME")
                .param("branchId", otherBranch.toString()).param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.content[0].code").value("LOCAL"))
            .andExpect(jsonPath("$.content[0].sessionCount").value(nullValue()))
            .andExpect(jsonPath("$.content[0].description").value(nullValue()))
            .andExpect(jsonPath("$.content[0].branchId").value(branch.intValue()))
            .andExpect(jsonPath("$.content[0].durationDays").value(30))
            .andExpect(jsonPath("$.content[0].price").value(500000));
        mvc.perform(get("/api/v1/packages").with(bearer(role)).param("type", "TIME").param("size", "1").param("page", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].code").value("GLOBAL"))
            .andExpect(jsonPath("$.content[0].branchId").value(nullValue()));
    }

    @Test
    void defaultActiveAndOrderingMatchContract() throws Exception {
        mvc.perform(get("/api/v1/packages").with(bearer("BRANCH_MANAGER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(4))
            .andExpect(jsonPath("$.content[*].code").value(contains("PT", "SESSION", "LOCAL", "GLOBAL")));
    }

    @Test
    void ownerSeesOtherBranchesWithoutStaff() throws Exception {
        jdbc.update("DELETE FROM staff WHERE user_id=?", employee);
        mvc.perform(get("/api/v1/packages").with(bearer("SUPER_ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.content[*].code").value(hasItem("OTHER")));
    }

    @Test
    void inactiveFilterAndEmptyResults() throws Exception {
        mvc.perform(get("/api/v1/packages").with(bearer("BRANCH_MANAGER")).param("status", "INACTIVE"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].code").value("OLD"));
        mvc.perform(get("/api/v1/packages").with(bearer("BRANCH_MANAGER")).param("status", "INACTIVE").param("type", "PT"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(0)).andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void maximumSizeAndPagePastEndAreValid() throws Exception {
        mvc.perform(get("/api/v1/packages").with(bearer("BRANCH_MANAGER")).param("size", "100"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(100))
            .andExpect(jsonPath("$.content.length()").value(4));
        mvc.perform(get("/api/v1/packages").with(bearer("BRANCH_MANAGER")).param("page", "10").param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(4)).andExpect(jsonPath("$.totalPages").value(4));
    }

    @Test
    void inactiveOrMissingStaffIsForbidden() throws Exception {
        jdbc.update("UPDATE staff SET status='INACTIVE' WHERE user_id=?", employee);
        mvc.perform(get("/api/v1/packages").with(bearer("BRANCH_MANAGER"))).andExpect(status().isForbidden());
        jdbc.update("DELETE FROM staff WHERE user_id=?", employee);
        mvc.perform(get("/api/v1/packages").with(bearer("RECEPTIONIST"))).andExpect(status().isForbidden());
    }

    private RequestPostProcessor bearer(String role) {
        String token = tokens.generateAccessToken(employee, "f3@example.com", UserRole.valueOf(role));
        return request -> { request.addHeader("Authorization", "Bearer " + token); return request; };
    }

    private void item(String code, String name, String type, Long branchId, String status) {
        jdbc.update("INSERT INTO packages(code,name,type,branch_id,status,duration_days,session_count,price) VALUES (?,?,?,?,?,30,?,500000)",
            code, name, type, branchId, status, "TIME".equals(type) ? null : 10);
    }
}
