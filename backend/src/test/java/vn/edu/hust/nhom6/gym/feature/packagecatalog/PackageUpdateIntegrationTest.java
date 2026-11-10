package vn.edu.hust.nhom6.gym.feature.packagecatalog;

import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PackageCatalogIntegrationTest.Database.class)
class PackageUpdateIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired JsonMapper json;
    Long branch;
    Long otherBranch;
    Long manager;
    Long item;

    @BeforeEach
    void fixtures() {
        jdbc.execute("TRUNCATE TABLE users, packages RESTART IDENTITY CASCADE");
        branch = jdbc.queryForObject("SELECT id FROM branches WHERE code='CN01'", Long.class);
        jdbc.update("INSERT INTO branches(code,name,open_time,close_time) VALUES ('F3UPDATE','Other','06:00','22:00') ON CONFLICT(code) DO NOTHING");
        otherBranch = jdbc.queryForObject("SELECT id FROM branches WHERE code='F3UPDATE'", Long.class);
        manager = jdbc.queryForObject("INSERT INTO users(email,password_hash,role) VALUES ('update-f3@example.com','test-only','BRANCH_MANAGER') RETURNING id", Long.class);
        jdbc.update("INSERT INTO staff(user_id,branch_id,staff_code,full_name) VALUES (?,?,'F3UPDATE','Quản lý test')", manager, branch);
        item = jdbc.queryForObject("INSERT INTO packages(code,name,type,branch_id,duration_days,price,description) VALUES ('EDIT','Gói cũ','TIME',?,30,500000,'Mô tả cũ') RETURNING id", Long.class, branch);
    }

    @Test
    void priceOnlyKeepsOmittedFieldsAndAudit() throws Exception {
        Map<String, Object> before = row();
        mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"price\":450000}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(450000))
            .andExpect(jsonPath("$.name").value("Gói cũ")).andExpect(jsonPath("$.description").value("Mô tả cũ"))
            .andExpect(jsonPath("$.durationDays").value(30)).andExpect(jsonPath("$.code").value("EDIT"))
            .andExpect(jsonPath("$.type").value("TIME")).andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.branchId").value(branch.intValue()));
        assertThat(row().get("created_at")).isEqualTo(before.get("created_at"));
        assertThat(row().get("updated_at")).isNotEqualTo(before.get("updated_at"));
    }

    @Test
    void explicitNullClearsDescriptionAndEmptyBodyDoesNotChangeAudit() throws Exception {
        Map<String, Object> before = row();
        mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Mô tả cũ"));
        assertThat(row()).isEqualTo(before);
        mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"description\":null}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.description").value(nullValue()));
        assertThat(row().get("description")).isNull();
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(Arguments.of("name", "null"), Arguments.of("price", "null"),
            Arguments.of("durationDays", "null"), Arguments.of("name", "\" \""),
            Arguments.of("price", "-1"), Arguments.of("durationDays", "0"),
            Arguments.of("durationDays", "-1"), Arguments.of("price", "1.2"),
            Arguments.of("price", "\"5\""), Arguments.of("price", "2147483648"),
            Arguments.of("name", "42"), Arguments.of("description", "false"),
            Arguments.of("sessionCount", "1"), Arguments.of("sessionCount", "1.5"));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void invalidUpdateIsAtomicAndReturnsFieldError(String field, String raw) throws Exception {
        var before = row();
        var body = json.createObjectNode();
        body.put("description", "Must not persist"); body.set(field, json.readTree(raw));
        mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors." + field).isNotEmpty());
        assertThat(row()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"code", "type", "branchId", "status", "id", "updatedAt", "provided", "unknown"})
    void refusesImmutableAndUnknownFields(String field) throws Exception {
        var before = row();
        var body = json.createObjectNode(); body.put(field, "forged"); body.put("price", 0);
        mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
        assertThat(row()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SESSION", "PT"})
    void sessionsMergeWithExistingTypeAndCannotBeCleared(String type) throws Exception {
        jdbc.update("UPDATE packages SET type=?, session_count=10 WHERE id=?", type, item);
        mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"price\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.sessionCount").value(10));
        var before = row();
        for (String value : List.of("null", "0", "-1")) {
            mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"sessionCount\":" + value + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.sessionCount").isNotEmpty());
            assertThat(row()).isEqualTo(before);
        }
        mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"sessionCount\":1,\"durationDays\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.sessionCount").value(1))
            .andExpect(jsonPath("$.durationDays").value(1));
    }

    @Test
    void acceptsBoundariesAndCanEditInactiveWithoutReactivation() throws Exception {
        jdbc.update("UPDATE packages SET status='INACTIVE' WHERE id=?", item);
        var body = json.createObjectNode(); body.put("name", "N".repeat(255));
        body.put("price", Integer.MAX_VALUE); body.put("durationDays", Integer.MAX_VALUE); body.putNull("sessionCount");
        mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        var before = row(); body.put("name", "N".repeat(256));
        mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").isNotEmpty());
        assertThat(row()).isEqualTo(before);
    }

    @Test
    void outsideBranchGlobalAndMissingHaveSame404() throws Exception {
        jdbc.update("UPDATE packages SET branch_id=? WHERE id=?", otherBranch, item);
        var before = row();
        String first = mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"price\":0}"))
            .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(row()).isEqualTo(before);
        jdbc.update("UPDATE packages SET branch_id=null WHERE id=?", item);
        String global = mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String missing = mvc.perform(put("/api/v1/packages/999999").with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(first).get("detail")).isEqualTo(json.readTree(global).get("detail"))
            .isEqualTo(json.readTree(missing).get("detail"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUPER_ADMIN", "RECEPTIONIST", "TRAINER", "MEMBER"})
    void deniedRolesCannotUpdate(String role) throws Exception {
        var before = row();
        mvc.perform(put("/api/v1/packages/{id}", item).with(bearer(role))
                .contentType(MediaType.APPLICATION_JSON).content("{\"price\":0}"))
            .andExpect(status().isForbidden());
        assertThat(row()).isEqualTo(before);
    }

    @Test
    void noTokenOrInactiveMissingStaffCannotUpdate() throws Exception {
        var before = row();
        mvc.perform(put("/api/v1/packages/{id}", item).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        jdbc.update("UPDATE staff SET status='INACTIVE' WHERE user_id=?", manager);
        mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        jdbc.update("DELETE FROM staff WHERE user_id=?", manager);
        mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        assertThat(row()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "9223372036854775808"})
    void invalidIdIsBadRequest(String id) throws Exception {
        mvc.perform(put("/api/v1/packages/" + id).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.id").isNotEmpty());
    }

    @Test
    void malformedBodyIsRejected() throws Exception {
        for (String body : List.of("{", "null", "", "[]")) {
            mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "ACTIVE"})
    void historyIncludingPendingPurchaseRemainsIdentical(String membershipStatus) throws Exception {
        jdbc.update("UPDATE packages SET type='SESSION',session_count=10 WHERE id=?", item);
        Long member = jdbc.queryForObject("INSERT INTO members(home_branch_id,member_code,full_name,phone) VALUES (?,'HV000001','Test','0912345678') RETURNING id", Long.class, branch);
        Long invoice = jdbc.queryForObject("INSERT INTO invoices(code,branch_id,member_id,created_by,source,subtotal,total,status) VALUES ('F3HISTORY',?,?,?,'COUNTER',500000,500000,?) RETURNING id",
            Long.class, branch, member, manager, membershipStatus.equals("ACTIVE") ? "PAID" : "PENDING");
        Long line = jdbc.queryForObject("INSERT INTO invoice_items(invoice_id,item_type,package_id,description,unit_price,amount) VALUES (?,'PACKAGE',?,'Gói cũ',500000,500000) RETURNING id", Long.class, invoice, item);
        jdbc.update("INSERT INTO memberships(member_id,package_id,package_type,branch_id,sold_branch_id,invoice_item_id,start_date,end_date,total_sessions,remaining_sessions,status) VALUES (?,?,'SESSION',?,?,?,'2026-11-01','2026-11-30',10,10,?)",
            member, item, branch, branch, line, membershipStatus);
        var historicalMembership = jdbc.queryForList("SELECT * FROM memberships");
        var historicalLine = jdbc.queryForList("SELECT * FROM invoice_items");
        var historicalInvoice = jdbc.queryForList("SELECT * FROM invoices");
        mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Gói mới\",\"price\":0,\"durationDays\":90,\"sessionCount\":20}"))
            .andExpect(status().isOk());
        assertThat(jdbc.queryForList("SELECT * FROM memberships")).isEqualTo(historicalMembership);
        assertThat(jdbc.queryForList("SELECT * FROM invoice_items")).isEqualTo(historicalLine);
        assertThat(jdbc.queryForList("SELECT * FROM invoices")).isEqualTo(historicalInvoice);
    }

    private Map<String, Object> row() { return jdbc.queryForMap("SELECT * FROM packages WHERE id=?", item); }
    private RequestPostProcessor bearer(String role) {
        String token = tokens.generateAccessToken(manager, "update-f3@example.com", UserRole.valueOf(role));
        return request -> { request.addHeader("Authorization", "Bearer " + token); return request; };
    }
}
