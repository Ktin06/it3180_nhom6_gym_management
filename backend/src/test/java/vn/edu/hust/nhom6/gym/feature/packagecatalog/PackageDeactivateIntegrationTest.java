package vn.edu.hust.nhom6.gym.feature.packagecatalog;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** JWT, phân công chi nhánh, khóa dòng, schema/FK và dữ liệu lịch sử đều production/PostgreSQL thật. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PackageCatalogIntegrationTest.Database.class)
class PackageDeactivateIntegrationTest {
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
        jdbc.update("INSERT INTO branches(code,name,open_time,close_time) VALUES ('F3STOP','Other','06:00','22:00') ON CONFLICT(code) DO NOTHING");
        otherBranch = jdbc.queryForObject("SELECT id FROM branches WHERE code='F3STOP'", Long.class);
        manager = jdbc.queryForObject("INSERT INTO users(email,password_hash,role) VALUES ('stop-f3@example.com','test-only','BRANCH_MANAGER') RETURNING id", Long.class);
        jdbc.update("INSERT INTO staff(user_id,branch_id,staff_code,full_name) VALUES (?,?,'F3STOP','Quản lý test')", manager, branch);
        item = jdbc.queryForObject("INSERT INTO packages(code,name,type,branch_id,duration_days,price,description) VALUES ('STOP','Gói cũ','TIME',?,30,500000,'Mô tả') RETURNING id", Long.class, branch);
    }

    @ParameterizedTest
    @CsvSource({"TIME,false", "SESSION,false", "PT,false", "TIME,true", "SESSION,true", "PT,true"})
    void deactivatesWithNoBodyOrEmptyJsonWithoutChangingOtherFields(String type, boolean emptyJsonBody) throws Exception {
        if (!type.equals("TIME")) { jdbc.update("UPDATE packages SET type=?,session_count=10 WHERE id=?", type, item); }
        var before = row();
        var request = put(endpoint(item.toString())).with(bearer("BRANCH_MANAGER"));
        if (emptyJsonBody) { request.contentType(MediaType.APPLICATION_JSON).content("{}"); }
        var response = mvc.perform(request)
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(item.intValue()))
            .andExpect(jsonPath("$.code").value("STOP")).andExpect(jsonPath("$.status").value("INACTIVE"))
            .andExpect(jsonPath("$.updatedAt").isNotEmpty()).andReturn().getResponse();
        assertThat(json.readTree(response.getContentAsString()).size()).isEqualTo(4);
        var after = row();
        assertThat(after.get("status")).isEqualTo("INACTIVE");
        assertThat(after.get("updated_at")).isNotEqualTo(before.get("updated_at"));
        before.remove("status"); before.remove("updated_at");
        after.remove("status"); after.remove("updated_at");
        assertThat(after).isEqualTo(before);
        mvc.perform(get("/api/v1/packages").with(bearer("RECEPTIONIST")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/packages").with(bearer("RECEPTIONIST")).param("status", "INACTIVE"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].code").value("STOP"));
    }

    @Test
    void repeatIs422WithoutFurtherAuditChange() throws Exception {
        mvc.perform(put(endpoint(item.toString())).with(bearer("BRANCH_MANAGER"))).andExpect(status().isOk());
        var before = row();
        mvc.perform(put(endpoint(item.toString())).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.detail").value("Gói tập này đã ở trạng thái ngừng bán."));
        assertThat(row()).isEqualTo(before);
    }

    @ParameterizedTest
    @CsvSource({"OTHER,ACTIVE", "GLOBAL,ACTIVE", "OTHER,INACTIVE", "GLOBAL,INACTIVE"})
    void outsideBranchAndGlobalAre403EvenIfAlreadyInactive(String scope, String state) throws Exception {
        jdbc.update("UPDATE packages SET branch_id=?,status=? WHERE id=?",
            scope.equals("GLOBAL") ? null : otherBranch, state, item);
        var before = row();
        mvc.perform(put(endpoint(item.toString())).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Bạn không có quyền thực hiện thao tác này."));
        assertThat(row()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUPER_ADMIN", "RECEPTIONIST", "MEMBER", "TRAINER"})
    void deniedRolesCannotDeactivate(String role) throws Exception {
        var before = row();
        mvc.perform(put(endpoint(item.toString())).with(bearer(role))).andExpect(status().isForbidden());
        assertThat(row()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "9223372036854775808"})
    void invalidIdIs400(String id) throws Exception {
        mvc.perform(put(endpoint(id)).with(bearer("BRANCH_MANAGER"))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.id").isNotEmpty());
    }

    @Test
    void missingAndBadTokenAre401() throws Exception {
        var before = row();
        mvc.perform(put(endpoint(item.toString()))).andExpect(status().isUnauthorized());
        mvc.perform(put(endpoint(item.toString())).header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized());
        assertThat(row()).isEqualTo(before);
    }

    @Test
    void inactiveAndMissingStaffAre403() throws Exception {
        var before = row();
        jdbc.update("UPDATE staff SET status='INACTIVE' WHERE user_id=?", manager);
        mvc.perform(put(endpoint(item.toString())).with(bearer("BRANCH_MANAGER"))).andExpect(status().isForbidden());
        jdbc.update("DELETE FROM staff WHERE user_id=?", manager);
        mvc.perform(put(endpoint(item.toString())).with(bearer("BRANCH_MANAGER"))).andExpect(status().isForbidden());
        assertThat(row()).isEqualTo(before);
    }

    @Test
    void missingPackageIs404WithEmptyJsonBodyWithoutMutation() throws Exception {
        var before = row();
        mvc.perform(put(endpoint("999999")).with(bearer("BRANCH_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Gói tập không tồn tại trên hệ thống."));
        assertThat(row()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "ACTIVE"})
    void preservesAllHistoricalRows(String state) throws Exception {
        jdbc.update("UPDATE packages SET type='SESSION',session_count=10 WHERE id=?", item);
        Long member = jdbc.queryForObject("INSERT INTO members(home_branch_id,member_code,full_name,phone) VALUES (?,'HV000001','Test','0912345678') RETURNING id", Long.class, branch);
        Long invoice = jdbc.queryForObject("INSERT INTO invoices(code,branch_id,member_id,created_by,source,subtotal,total,status) VALUES ('F3STOP',?,?,?,'COUNTER',500000,500000,?) RETURNING id", Long.class,
            branch, member, manager, state.equals("ACTIVE") ? "PAID" : "PENDING");
        Long line = jdbc.queryForObject("INSERT INTO invoice_items(invoice_id,item_type,package_id,description,unit_price,amount) VALUES (?,'PACKAGE',?,'Gói cũ',500000,500000) RETURNING id", Long.class, invoice, item);
        jdbc.update("INSERT INTO memberships(member_id,package_id,package_type,branch_id,sold_branch_id,invoice_item_id,start_date,end_date,total_sessions,remaining_sessions,status) VALUES (?,?,'SESSION',?,?,?,'2026-11-01','2026-11-30',10,10,?)", member,item,branch,branch,line,state);
        var memberships = jdbc.queryForList("SELECT * FROM memberships");
        var invoices = jdbc.queryForList("SELECT * FROM invoices");
        var lines = jdbc.queryForList("SELECT * FROM invoice_items");
        mvc.perform(put(endpoint(item.toString())).with(bearer("BRANCH_MANAGER"))).andExpect(status().isOk());
        assertThat(jdbc.queryForList("SELECT * FROM memberships")).isEqualTo(memberships);
        assertThat(jdbc.queryForList("SELECT * FROM invoices")).isEqualTo(invoices);
        assertThat(jdbc.queryForList("SELECT * FROM invoice_items")).isEqualTo(lines);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM packages", Long.class)).isEqualTo(1);
    }

    @Test
    void concurrentEditCannotReactivatePackage() throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var stop = executor.submit(() -> {
                barrier.await(15, TimeUnit.SECONDS);
                return mvc.perform(put(endpoint(item.toString())).with(bearer("BRANCH_MANAGER"))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andReturn().getResponse().getStatus();
            });
            var edit = executor.submit(() -> {
                barrier.await(15, TimeUnit.SECONDS);
                return mvc.perform(put("/api/v1/packages/{id}", item).with(bearer("BRANCH_MANAGER"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"price\":1}"))
                    .andReturn().getResponse().getStatus();
            });
            assertThat(stop.get(25, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(edit.get(25, TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(row().get("status")).isEqualTo("INACTIVE");
        assertThat(row().get("price")).isEqualTo(1);
    }

    @Test
    void simultaneousDeactivationHasOneSuccessAndOne422() throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = Stream.of(1,2).map(i -> executor.submit(() -> {
                barrier.await(15, TimeUnit.SECONDS);
                return mvc.perform(put(endpoint(item.toString())).with(bearer("BRANCH_MANAGER")))
                    .andReturn().getResponse().getStatus();
            })).toList();
            assertThat(List.of(futures.get(0).get(25, TimeUnit.SECONDS), futures.get(1).get(25, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(200,422);
        }
        assertThat(row().get("status")).isEqualTo("INACTIVE");
    }

    private String endpoint(String id) { return "/api/v1/packages/" + id + "/deactivate"; }
    private Map<String,Object> row() { return jdbc.queryForMap("SELECT * FROM packages WHERE id=?", item); }
    private RequestPostProcessor bearer(String role) {
        String token = tokens.generateAccessToken(manager,"stop-f3@example.com",UserRole.valueOf(role));
        return request -> { request.addHeader("Authorization","Bearer " + token); return request; };
    }
}
