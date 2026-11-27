package vn.edu.hust.nhom6.gym.feature.memberportal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.invoice.SaleFixtures;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.service.InvoiceService;
import vn.edu.hust.nhom6.gym.feature.memberportal.PortalFixtures.MemberAccount;

/**
- {@code POST /api/v1/me/purchase-requests}, {@code POST /api/v1/me/renewal-requests} (UC-F7.4, UC-F7.5, S2-14.1).
- Quy tắc chặn trùng gói đang chờ trả 422 thống nhất theo SRS F7 (#249) và mẫu F3.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class PortalPurchaseIntegrationTest {
    static final String WAITING =
        "Hội viên đã có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt. Không thể tạo thêm gói mới.";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired JsonMapper json;
    @Autowired MutableClock clock;
    @Autowired InvoiceService invoiceService;

    SaleFixtures f;
    PortalFixtures p;
    MemberAccount me;
    Long timePkg;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        f = new SaleFixtures(jdbc, tokens);
        f.reset();
        p = new PortalFixtures(jdbc);
        p.resetTtl();
        me = p.memberAccount("HV000301", "0912000301", f.cn01, "ACTIVE");
        timePkg = f.pkg("TIME_30", "TIME", null, 500_000, f.cn01, "ACTIVE");
    }

    // --- UC-F7.4: mua gói --------------------------------------------------------------------------

    @Test
    @DisplayName("TC-F7.4.1-01a: Hội viên gửi yêu cầu mua thì 201 kèm mã hóa đơn, tổng tiền server tính, hạn tự hủy = created_at + 3 ngày")
    void purchaseCreatesPortalInvoice() throws Exception {
        String body = purchase(me, "{\"packageId\": " + timePkg + ", \"price\": 1, \"memberId\": 999}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invoiceCode", org.hamcrest.Matchers.matchesPattern("HD\\d{6}")))
            .andExpect(jsonPath("$.total").value(500_000))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.membershipStatus").value("PENDING"))
            .andExpect(jsonPath("$.startDate").value("2026-11-20"))
            .andExpect(jsonPath("$.endDate").value("2026-12-19"))
            .andReturn().getResponse().getContentAsString();

        OffsetDateTime expiresAt = OffsetDateTime.parse(json.readTree(body).get("expiresAt").asString());
        assertThat(expiresAt.toInstant()).isEqualTo(clock.instant().plus(java.time.Duration.ofDays(3)));
        var row = jdbc.queryForMap("SELECT source, created_by, branch_id, member_id, total FROM invoices");
        assertThat(row).containsEntry("source", "MEMBER_PORTAL").containsEntry("created_by", me.userId())
            .containsEntry("branch_id", f.cn01).containsEntry("member_id", me.memberId())
            .containsEntry("total", 500_000);
        assertThat(jdbc.queryForObject("SELECT status FROM memberships WHERE member_id = ?", String.class,
            me.memberId())).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("TC-F7.4.1-04a: Hạn tự hủy đọc theo chi nhánh của hóa đơn khi chi nhánh có giá trị riêng")
    void expiresAtUsesBranchTtl() throws Exception {
        p.branchTtl(f.cn01, 1);
        String body = purchase(me, "{\"packageId\": " + timePkg + "}").andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        OffsetDateTime expiresAt = OffsetDateTime.parse(json.readTree(body).get("expiresAt").asString());
        assertThat(expiresAt).isEqualTo(OffsetDateTime.of(2026, 11, 21, 2, 0, 0, 0, ZoneOffset.UTC));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"packageId\": null}", "{\"packageId\": 0}", "{\"packageId\": -5}"})
    @DisplayName("TC-F7.4.1-08a: Thiếu hoặc sai mã gói thì 400, không tạo gì")
    void invalidPurchaseBodyIs400(String body) throws Exception {
        purchase(me, body).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.packageId").isString());
        assertThat(f.count("invoices")).isZero();
    }

    @Test
    @DisplayName("TC-F7.4.1-08b: Lễ tân gọi API cổng hội viên thì 403; không có token thì 401")
    void nonMemberIs403() throws Exception {
        Long receptionist = f.staff("le-tan@example.com", UserRole.RECEPTIONIST, f.cn01);
        mvc.perform(post("/api/v1/me/purchase-requests").with(f.bearer(receptionist, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON).content("{\"packageId\": " + timePkg + "}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/me/purchase-requests")
                .contentType(MediaType.APPLICATION_JSON).content("{\"packageId\": " + timePkg + "}"))
            .andExpect(status().isUnauthorized());
        assertThat(f.count("invoices")).isZero();
    }

    @Test
    @DisplayName("TC-F7.4.1-08c: Tài khoản MEMBER chưa gắn hồ sơ hội viên thì 403")
    void memberWithoutProfileIs403() throws Exception {
        Long orphan = jdbc.queryForObject("INSERT INTO users (email, password_hash, role) "
            + "VALUES ('mo-coi@example.com', 'test-only', 'MEMBER') RETURNING id", Long.class);
        mvc.perform(post("/api/v1/me/purchase-requests").with(f.bearer(orphan, UserRole.MEMBER))
                .contentType(MediaType.APPLICATION_JSON).content("{\"packageId\": " + timePkg + "}"))
            .andExpect(status().isForbidden());
        assertThat(f.count("invoices")).isZero();
    }

    @Test
    @DisplayName("TC-F7.4.1-07a: Gói không tồn tại thì 404")
    void unknownPackageIs404() throws Exception {
        purchase(me, "{\"packageId\": 999999}").andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Gói tập không tồn tại trên hệ thống."));
        assertThat(f.count("invoices")).isZero();
    }

    @Test
    @DisplayName("TC-F7.4.1-05a: Đã có gói chờ cho đúng gói này thì 422; vẫn chỉ một hóa đơn")
    void duplicatePendingForSamePackageIs422() throws Exception {
        purchase(me, "{\"packageId\": " + timePkg + "}").andExpect(status().isCreated());
        purchase(me, "{\"packageId\": " + timePkg + "}").andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(WAITING));
        assertThat(f.count("invoices")).isEqualTo(1);
        assertThat(f.count("memberships")).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-F7.4.1-05b: Hóa đơn chờ cùng gói do lễ tân lập tại quầy cũng chặn 422")
    void counterPendingForSamePackageIs422() throws Exception {
        Long receptionist = f.staff("le-tan@example.com", UserRole.RECEPTIONIST, f.cn01);
        invoiceService.createPackageInvoice(receptionist, f.cn01, me.memberId(), timePkg, InvoiceSource.COUNTER);
        purchase(me, "{\"packageId\": " + timePkg + "}").andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(WAITING));
        assertThat(f.count("invoices")).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-F7.4.1-07b: Gói ngừng bán thì 422, không tạo gì")
    void inactivePackageIs422() throws Exception {
        Long stopped = f.pkg("TIME_OLD", "TIME", null, 300_000, f.cn01, "INACTIVE");
        purchase(me, "{\"packageId\": " + stopped + "}").andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Gói tập này đã ngừng bán."));
        assertThat(f.count("invoices")).isZero();
    }

    @Test
    @DisplayName("TC-F7.4.1-07c: Gói của chi nhánh khác chi nhánh gốc thì 422; gói toàn chuỗi mua được")
    void packageOfOtherBranchIs422() throws Exception {
        Long otherBranch = f.pkg("TIME_CN02", "TIME", null, 400_000, f.cn02, "ACTIVE");
        purchase(me, "{\"packageId\": " + otherBranch + "}").andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Gói tập này không áp dụng tại chi nhánh của bạn."));
        Long chain = f.pkg("SESSION_ALL", "SESSION", 10, 900_000, null, "ACTIVE");
        purchase(me, "{\"packageId\": " + chain + "}").andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT branch_id FROM invoices", Long.class)).isEqualTo(f.cn01);
    }

    @Test
    @DisplayName("TC-F7.4.1-05c: Gói khác nhưng cùng loại đang PENDING thì 422 như AC-F3.5.1-06")
    void otherPackageOfSameTypePendingIs422() throws Exception {
        Long time90 = f.pkg("TIME_90", "TIME", null, 1_300_000, f.cn01, "ACTIVE");
        purchase(me, "{\"packageId\": " + timePkg + "}").andExpect(status().isCreated());
        purchase(me, "{\"packageId\": " + time90 + "}").andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(WAITING));
        assertThat(f.count("invoices")).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-F7.4.1-05d: Đã có gói cùng loại UPCOMING thì 422; khác loại vẫn mua được")
    void upcomingOfSameTypeIs422() throws Exception {
        f.membership(me.memberId(), timePkg, "TIME", f.cn01, "ACTIVE", LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30), null);
        f.membership(me.memberId(), timePkg, "TIME", f.cn01, "UPCOMING", LocalDate.of(2026, 12, 1),
            LocalDate.of(2026, 12, 30), null);
        Long time90 = f.pkg("TIME_90", "TIME", null, 1_300_000, f.cn01, "ACTIVE");
        purchase(me, "{\"packageId\": " + time90 + "}").andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(WAITING));
        Long session = f.pkg("SESSION_10", "SESSION", 10, 800_000, f.cn01, "ACTIVE");
        purchase(me, "{\"packageId\": " + session + "}").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-F7.4.1-07d: Hội viên ngừng hoạt động thì 422, không tạo gì")
    void inactiveMemberIs422() throws Exception {
        MemberAccount inactive = p.memberAccount("HV000302", "0912000302", f.cn01, "INACTIVE");
        purchase(inactive, "{\"packageId\": " + timePkg + "}").andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Hội viên đã ngừng hoạt động, không thể bán gói tập."));
        assertThat(f.count("invoices")).isZero();
    }

    @Test
    @DisplayName("TC-F7.4.1-05e: Hội viên ngừng hoạt động mà còn hóa đơn chờ cùng gói thì vẫn 422 ngừng hoạt động")
    void inactiveMemberWithPendingIs422() throws Exception {
        purchase(me, "{\"packageId\": " + timePkg + "}").andExpect(status().isCreated());
        jdbc.update("UPDATE members SET status = 'INACTIVE' WHERE id = ?", me.memberId());
        purchase(me, "{\"packageId\": " + timePkg + "}").andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Hội viên đã ngừng hoạt động, không thể bán gói tập."));
        assertThat(f.count("invoices")).isEqualTo(1);
    }

    // --- UC-F7.5: gia hạn gói ----------------------------------------------------------------------

    @Test
    @DisplayName("TC-F7.5.1-01a: Gia hạn gói ACTIVE thì 201, gói mới PENDING cùng gói danh mục, nối sau end_date; gói cũ không đổi")
    void renewCreatesNewPendingMembership() throws Exception {
        Long current = f.membership(me.memberId(), timePkg, "TIME", f.cn01, "ACTIVE", LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30), null);
        renew(me, "{\"membershipId\": " + current + "}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.packageId").value(timePkg))
            .andExpect(jsonPath("$.membershipStatus").value("PENDING"))
            .andExpect(jsonPath("$.startDate").value("2026-12-01"))
            .andExpect(jsonPath("$.endDate").value("2026-12-30"))
            .andExpect(jsonPath("$.expiresAt").isString());
        assertThat(jdbc.queryForObject("SELECT source FROM invoices", String.class)).isEqualTo("MEMBER_PORTAL");
        var old = jdbc.queryForMap("SELECT status, end_date FROM memberships WHERE id = ?", current);
        assertThat(old.get("status")).isEqualTo("ACTIVE");
        assertThat(old.get("end_date").toString()).isEqualTo("2026-11-30");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"membershipId\": 0}"})
    @DisplayName("TC-F7.5.1-09a: Thiếu hoặc sai mã gói của hội viên thì 400")
    void invalidRenewBodyIs400(String body) throws Exception {
        renew(me, body).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.membershipId").isString());
    }

    @Test
    @DisplayName("TC-F7.5.1-04a: Gia hạn gói của hội viên khác thì 404 như gói không tồn tại, không tạo gì")
    void renewOthersMembershipIs404() throws Exception {
        MemberAccount other = p.memberAccount("HV000303", "0912000303", f.cn01, "ACTIVE");
        Long theirs = f.membership(other.memberId(), timePkg, "TIME", f.cn01, "ACTIVE", LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30), null);
        renew(me, "{\"membershipId\": " + theirs + "}").andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Gói tập của hội viên không tồn tại trên hệ thống."));
        assertThat(f.count("invoices")).isZero();
    }

    @Test
    @DisplayName("TC-F7.5.1-03a TC-F7.5.1-02a: Gia hạn gói Đã hủy thì 422, không tạo gì; gói Hết hạn vẫn gia hạn được")
    void renewCancelledMembershipIs422() throws Exception {
        Long cancelled = f.membership(me.memberId(), timePkg, "TIME", f.cn01, "CANCELLED", LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30), null);
        renew(me, "{\"membershipId\": " + cancelled + "}").andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Gói này đã hủy. Vui lòng chọn gói ở mục Mua gói."));
        assertThat(f.count("invoices")).isZero();

        Long expired = f.membership(me.memberId(), timePkg, "TIME", f.cn01, "EXPIRED", LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 30), null);
        renew(me, "{\"membershipId\": " + expired + "}").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-F7.5.1-04b: Gói của hội viên không tồn tại thì 404")
    void renewUnknownMembershipIs404() throws Exception {
        renew(me, "{\"membershipId\": 999999}").andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Gói tập của hội viên không tồn tại trên hệ thống."));
    }

    @Test
    @DisplayName("TC-F7.5.1-06a: Gửi gia hạn hai lần thì lần hai 422")
    void renewTwiceIs422() throws Exception {
        Long current = f.membership(me.memberId(), timePkg, "TIME", f.cn01, "ACTIVE", LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30), null);
        renew(me, "{\"membershipId\": " + current + "}").andExpect(status().isCreated());
        renew(me, "{\"membershipId\": " + current + "}").andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(WAITING));
        assertThat(f.count("invoices")).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-F7.5.1-05a: Gói danh mục của gói cũ đã ngừng bán thì 422")
    void renewInactivePackageIs422() throws Exception {
        Long stopped = f.pkg("TIME_OLD", "TIME", null, 300_000, f.cn01, "INACTIVE");
        Long current = f.membership(me.memberId(), stopped, "TIME", f.cn01, "ACTIVE", LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30), null);
        renew(me, "{\"membershipId\": " + current + "}").andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Gói tập này đã ngừng bán."));
        assertThat(f.count("invoices")).isZero();
    }

    private ResultActions purchase(MemberAccount who, String body) throws Exception {
        return mvc.perform(post("/api/v1/me/purchase-requests").with(f.bearer(who.userId(), UserRole.MEMBER))
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions renew(MemberAccount who, String body) throws Exception {
        return mvc.perform(post("/api/v1/me/renewal-requests").with(f.bearer(who.userId(), UserRole.MEMBER))
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}