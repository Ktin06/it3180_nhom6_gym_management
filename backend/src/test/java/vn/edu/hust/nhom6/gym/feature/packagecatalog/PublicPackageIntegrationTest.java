package vn.edu.hust.nhom6.gym.feature.packagecatalog;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;

/** Bảng giá công khai với PostgreSQL thật, gọi không kèm access token (US-F3.1.2). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PublicPackageIntegrationTest {
    private static final String URL = "/api/v1/public/packages";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    Long branch;
    Long otherBranch;

    @BeforeEach
    void fixtures() {
        jdbc.execute("TRUNCATE TABLE packages RESTART IDENTITY CASCADE");
        jdbc.update("UPDATE branches SET status = 'ACTIVE' WHERE code = 'CN01'");
        branch = jdbc.queryForObject("SELECT id FROM branches WHERE code='CN01'", Long.class);
        jdbc.update("INSERT INTO branches(code,name,open_time,close_time) VALUES ('F3PUB','F3 public','06:00','22:00') "
            + "ON CONFLICT(code) DO NOTHING");
        otherBranch = jdbc.queryForObject("SELECT id FROM branches WHERE code='F3PUB'", Long.class);
    }

    @Test
    @DisplayName("TC-F3.1.2-01a: Khách không đăng nhập xem gói ACTIVE của CN01 và toàn chuỗi, đúng giá, sắp TIME→SESSION→PT rồi theo giá")
    void guestSeesActiveBranchAndGlobalPackagesInDisplayOrder() throws Exception {
        item("PT10", "PT 10 buổi", "PT", null, "ACTIVE", 3_000_000);
        item("S12", "12 buổi", "SESSION", branch, "ACTIVE", 600_000);
        item("T3", "Gói 3 tháng", "TIME", null, "ACTIVE", 1_350_000);
        item("T1", "Gói 1 tháng", "TIME", branch, "ACTIVE", 500_000);

        mvc.perform(get(URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name").value(contains("Gói 1 tháng", "Gói 3 tháng", "12 buổi", "PT 10 buổi")))
            .andExpect(jsonPath("$[0].type").value("TIME"))
            .andExpect(jsonPath("$[0].price").value(500_000))
            .andExpect(jsonPath("$[0].durationDays").value(30))
            .andExpect(jsonPath("$[0].sessionCount").value(nullValue()))
            .andExpect(jsonPath("$[2].sessionCount").value(10));
    }

    @Test
    @DisplayName("TC-F3.1.2-02a: Không trả gói ngừng bán và gói của chi nhánh khác")
    void hidesInactiveAndOtherBranchPackages() throws Exception {
        item("LOCAL", "Gói chi nhánh", "TIME", branch, "ACTIVE", 500_000);
        item("OLD", "Gói ngừng bán", "TIME", branch, "INACTIVE", 400_000);
        item("OTHER", "Gói chi nhánh khác", "TIME", otherBranch, "ACTIVE", 450_000);
        item("OLD-GLOBAL", "Gói toàn chuỗi ngừng bán", "SESSION", null, "INACTIVE", 300_000);

        mvc.perform(get(URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name").value(containsInAnyOrder("Gói chi nhánh")));
    }

    @Test
    @DisplayName("TC-F3.1.2-03a: Mỗi gói chỉ có trường hiển thị, không lộ mã, trạng thái, chi nhánh, thời gian tạo")
    void exposesOnlyDisplayFields() throws Exception {
        item("LOCAL", "Gói chi nhánh", "TIME", branch, "ACTIVE", 500_000);

        mvc.perform(get(URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].name").value("Gói chi nhánh"))
            .andExpect(jsonPath("$[0].type").value("TIME"))
            .andExpect(jsonPath("$[0].price").value(500_000))
            .andExpect(jsonPath("$[0].code").doesNotExist())
            .andExpect(jsonPath("$[0].status").doesNotExist())
            .andExpect(jsonPath("$[0].branchId").doesNotExist())
            .andExpect(jsonPath("$[0].createdAt").doesNotExist())
            .andExpect(jsonPath("$[0].updatedAt").doesNotExist())
            .andExpect(jsonPath("$[0].createdBy").doesNotExist());
    }

    @Test
    @DisplayName("TC-F3.1.2-04a: Không có gói đang bán thì trả danh sách rỗng, không lỗi")
    void emptyCatalogReturnsEmptyList() throws Exception {
        item("OLD", "Gói ngừng bán", "TIME", branch, "INACTIVE", 400_000);

        mvc.perform(get(URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("S3: Đường dẫn công khai chỉ mở GET; ghi dữ liệu vẫn cần đăng nhập")
    void onlyGetIsPublic() throws Exception {
        mvc.perform(post(URL)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/packages")).andExpect(status().isUnauthorized());
        mvc.perform(get(URL)).andExpect(status().isOk());
    }

    private void item(String code, String name, String type, Long branchId, String status, int price) {
        jdbc.update("INSERT INTO packages(code,name,type,branch_id,status,duration_days,session_count,price) "
                + "VALUES (?,?,?,?,?,30,?,?)",
            code, name, type, branchId, status, "TIME".equals(type) ? null : 10, price);
    }
}
