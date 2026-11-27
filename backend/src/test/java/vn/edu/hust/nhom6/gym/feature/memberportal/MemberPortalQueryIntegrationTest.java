package vn.edu.hust.nhom6.gym.feature.memberportal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.invoice.SaleFixtures;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PackageInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.service.InvoiceService;
import vn.edu.hust.nhom6.gym.feature.memberportal.PortalFixtures.MemberAccount;

/**
 * {@code GET /api/v1/me/memberships}, {@code GET /api/v1/me/checkins}, {@code GET /api/v1/me/invoices}
 * (UC-F7.1, UC-F7.2, UC-F7.3, BR-F7.1–BR-F7.4, S2-13.1).
 *
 * <p>Luôn gọi {@link PortalFixtures#resetTtl()} trong {@code @BeforeEach} để bảo đảm {@code PENDING_INVOICE_TTL_DAYS}
 * không bị mất sau {@code TRUNCATE users CASCADE} (VD-11).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class MemberPortalQueryIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired JsonMapper json;
    @Autowired MutableClock clock;
    @Autowired InvoiceService invoiceService;

    SaleFixtures f;
    PortalFixtures p;
    MemberAccount me;
    MemberAccount other;
    Long receptionist;
    Long timePkg;
    Long sessionChainPkg;
    Long ptPkg;
    Long cn02Pkg;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        f = new SaleFixtures(jdbc, tokens);
        f.reset();
        jdbc.execute("TRUNCATE TABLE products RESTART IDENTITY CASCADE");
        p = new PortalFixtures(jdbc);
        p.resetTtl();

        me = p.memberAccount("HV000401", "0912000401", f.cn01, "ACTIVE");
        other = p.memberAccount("HV000402", "0912000402", f.cn01, "ACTIVE");
        receptionist = f.staff("le-tan-f7@example.com", UserRole.RECEPTIONIST, f.cn01);

        timePkg = f.pkg("TIME_30", "TIME", null, 500_000, f.cn01, "ACTIVE");
        sessionChainPkg = f.pkg("SESSION_10", "SESSION", 10, 800_000, null, "ACTIVE");
        ptPkg = f.pkg("PT_12", "PT", 12, 3_600_000, f.cn01, "ACTIVE");
        cn02Pkg = f.pkg("TIME_CN02", "TIME", null, 600_000, f.cn02, "ACTIVE");
    }

    // --- UC-F7.1: GET /api/v1/me/memberships -------------------------------------------------------

    @Test
    @DisplayName("TC-F7.1.1-01a: Hội viên xem mọi gói của mình kèm tên, loại, mã trạng thái, ngày, số buổi còn lại")
    void membershipsReturnsAllOwnMembershipsWithDetails() throws Exception {
        Long expiredSession = f.membership(me.memberId(), sessionChainPkg, "SESSION", null, "EXPIRED",
            LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 0);
        Long activeSession = f.membership(me.memberId(), sessionChainPkg, "SESSION", null, "ACTIVE",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 7);
        Long frozenTime = f.membership(me.memberId(), timePkg, "TIME", f.cn01, "FROZEN",
            LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 25), null);
        Long upcomingTime = f.membership(me.memberId(), timePkg, "TIME", f.cn01, "UPCOMING",
            LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 30), null);
        Long pendingPt = f.membership(me.memberId(), ptPkg, "PT", f.cn01, "PENDING",
            LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 19), 12);

        String cn01Name = jdbc.queryForObject("SELECT name FROM branches WHERE id = ?", String.class, f.cn01);

        mvc.perform(get("/api/v1/me/memberships").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(5)))
            // Sắp xếp start_date DESC, id DESC: 12-01, 11-20, 11-01, 10-15, 09-01
            .andExpect(jsonPath("$[0].id").value(upcomingTime))
            .andExpect(jsonPath("$[0].packageId").value(timePkg))
            .andExpect(jsonPath("$[0].packageName").value("Gói TIME_30"))
            .andExpect(jsonPath("$[0].packageType").value("TIME"))
            .andExpect(jsonPath("$[0].branchId").value(f.cn01))
            .andExpect(jsonPath("$[0].branchName").value(cn01Name))
            .andExpect(jsonPath("$[0].status").value("UPCOMING"))
            .andExpect(jsonPath("$[0].startDate").value("2026-12-01"))
            .andExpect(jsonPath("$[0].endDate").value("2026-12-30"))
            .andExpect(jsonPath("$[0].totalSessions").value(nullValue()))
            .andExpect(jsonPath("$[0].remainingSessions").value(nullValue()))

            .andExpect(jsonPath("$[1].id").value(pendingPt))
            .andExpect(jsonPath("$[1].packageType").value("PT"))
            .andExpect(jsonPath("$[1].status").value("PENDING"))
            .andExpect(jsonPath("$[1].totalSessions").value(12))
            .andExpect(jsonPath("$[1].remainingSessions").value(12))

            .andExpect(jsonPath("$[2].id").value(activeSession))
            .andExpect(jsonPath("$[2].packageName").value("Gói SESSION_10"))
            .andExpect(jsonPath("$[2].packageType").value("SESSION"))
            .andExpect(jsonPath("$[2].branchId").value(nullValue()))
            .andExpect(jsonPath("$[2].branchName").value(nullValue()))
            .andExpect(jsonPath("$[2].status").value("ACTIVE"))
            .andExpect(jsonPath("$[2].totalSessions").value(10))
            .andExpect(jsonPath("$[2].remainingSessions").value(7))

            .andExpect(jsonPath("$[3].id").value(frozenTime))
            .andExpect(jsonPath("$[3].status").value("FROZEN"))

            .andExpect(jsonPath("$[4].id").value(expiredSession))
            .andExpect(jsonPath("$[4].status").value("EXPIRED"))
            .andExpect(jsonPath("$[4].remainingSessions").value(0));
    }

    @Test
    @DisplayName("TC-F7.1.1-03a: Hội viên chưa có gói nào trả danh sách rỗng")
    void membershipsEmptyWhenMemberHasNone() throws Exception {
        f.membership(other.memberId(), timePkg, "TIME", f.cn01, "ACTIVE",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null);

        mvc.perform(get("/api/v1/me/memberships").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("TC-F7.1.1-02a: Không thấy gói của hội viên khác kể cả khi truyền tham số memberId")
    void membershipsNeverLeaksOtherMembersData() throws Exception {
        Long ownId = f.membership(me.memberId(), timePkg, "TIME", f.cn01, "ACTIVE",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null);
        f.membership(other.memberId(), sessionChainPkg, "SESSION", null, "ACTIVE",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 10);
        f.membership(other.memberId(), ptPkg, "PT", f.cn01, "PENDING",
            LocalDate.of(2026, 11, 10), LocalDate.of(2026, 12, 9), 12);

        mvc.perform(get("/api/v1/me/memberships")
                .param("memberId", String.valueOf(other.memberId()))
                .with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(ownId));
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"RECEPTIONIST", "BRANCH_MANAGER", "TRAINER", "SUPER_ADMIN"})
    @DisplayName("TC-F7.1.1-05a: Vai trò khác MEMBER xem gói của tôi trả 403")
    void membershipsForbiddenForNonMemberRoles(UserRole role) throws Exception {
        Long staffUser = f.staff("role-" + role.name().toLowerCase() + "@example.com", role,
            role == UserRole.SUPER_ADMIN ? null : f.cn01);

        mvc.perform(get("/api/v1/me/memberships").with(f.bearer(staffUser, role)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-F7.1.1-05b: Tài khoản MEMBER chưa gắn hồ sơ hoặc hồ sơ xóa mềm trả 403; không token trả 401")
    void membershipsForbiddenWithoutProfileAndUnauthorizedWithoutToken() throws Exception {
        Long orphan = createOrphanMemberUser("mo-coi-goi@example.com");
        mvc.perform(get("/api/v1/me/memberships").with(f.bearer(orphan, UserRole.MEMBER)))
            .andExpect(status().isForbidden());

        jdbc.update("UPDATE members SET deleted_at = now() WHERE id = ?", me.memberId());
        mvc.perform(get("/api/v1/me/memberships").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/me/memberships"))
            .andExpect(status().isUnauthorized());
    }

    // --- UC-F7.2: GET /api/v1/me/checkins ----------------------------------------------------------

    @Test
    @DisplayName("TC-F7.2.1-01a: Lịch sử check-in phân trang, mới nhất trước, có chi nhánh, giờ vào, giờ ra, ngày Việt Nam")
    void checkinsPaginatedNewestFirstWithBranchAndTimes() throws Exception {
        Long ownMembership = f.membership(me.memberId(), sessionChainPkg, "SESSION", null, "ACTIVE",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 8);

        // Lượt 1 (cũ nhất): CN01, 19/11 08:00 VN, đã check-out 09:30 VN
        OffsetDateTime in1 = OffsetDateTime.of(2026, 11, 19, 1, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime out1 = OffsetDateTime.of(2026, 11, 19, 2, 30, 0, 0, ZoneOffset.UTC);
        Long id1 = insertCheckin(me.memberId(), ownMembership, f.cn01, LocalDate.of(2026, 11, 19), true, in1, out1);

        // Lượt 2 (giữa): 20/11 00:30 VN (= 19/11 17:30 UTC), chứng minh checkinDate đọc ngày Việt Nam
        OffsetDateTime in2 = OffsetDateTime.of(2026, 11, 19, 17, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime out2 = OffsetDateTime.of(2026, 11, 19, 18, 45, 0, 0, ZoneOffset.UTC);
        Long id2 = insertCheckin(me.memberId(), ownMembership, f.cn01, LocalDate.of(2026, 11, 20), true, in2, out2);

        // Lượt 3 (mới nhất): CN02, 20/11 18:00 VN, chưa check-out
        OffsetDateTime in3 = OffsetDateTime.of(2026, 11, 20, 11, 0, 0, 0, ZoneOffset.UTC);
        Long id3 = insertCheckin(me.memberId(), ownMembership, f.cn02, LocalDate.of(2026, 11, 20), false, in3, null);

        String cn01Name = jdbc.queryForObject("SELECT name FROM branches WHERE id = ?", String.class, f.cn01);

        // Trang 0 (size = 2): lượt 3 rồi lượt 2
        String page0 = mvc.perform(get("/api/v1/me/checkins")
                .param("page", "0")
                .param("size", "2")
                .with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].id").value(id3))
            .andExpect(jsonPath("$.content[0].branchId").value(f.cn02))
            .andExpect(jsonPath("$.content[0].branchName").value("Chi nhánh 2"))
            .andExpect(jsonPath("$.content[0].membershipId").value(ownMembership))
            .andExpect(jsonPath("$.content[0].packageName").value("Gói SESSION_10"))
            .andExpect(jsonPath("$.content[0].packageType").value("SESSION"))
            .andExpect(jsonPath("$.content[0].method").value("MANUAL"))
            .andExpect(jsonPath("$.content[0].sessionDeducted").value(false))
            .andExpect(jsonPath("$.content[0].checkinDate").value("2026-11-20"))
            .andExpect(jsonPath("$.content[0].checkedOutAt").value(nullValue()))
            .andExpect(jsonPath("$.content[1].id").value(id2))
            .andExpect(jsonPath("$.content[1].branchName").value(cn01Name))
            .andExpect(jsonPath("$.content[1].sessionDeducted").value(true))
            .andExpect(jsonPath("$.content[1].checkinDate").value("2026-11-20"))
            .andReturn().getResponse().getContentAsString();

        JsonNode content0 = json.readTree(page0).get("content");
        assertThat(OffsetDateTime.parse(content0.get(0).get("checkedInAt").asString()).toInstant())
            .isEqualTo(in3.toInstant());
        assertThat(OffsetDateTime.parse(content0.get(1).get("checkedInAt").asString()).toInstant())
            .isEqualTo(in2.toInstant());
        assertThat(OffsetDateTime.parse(content0.get(1).get("checkedOutAt").asString()).toInstant())
            .isEqualTo(out2.toInstant());

        // Trang 1 (size = 2): lượt 1
        mvc.perform(get("/api/v1/me/checkins")
                .param("page", "1")
                .param("size", "2")
                .with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(id1))
            .andExpect(jsonPath("$.content[0].checkinDate").value("2026-11-19"));

        // Mặc định không truyền page/size -> page=0, size=20
        mvc.perform(get("/api/v1/me/checkins").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    @DisplayName("TC-F7.2.1-02a: Chưa có lượt check-in nào trả PageResponse rỗng")
    void checkinsEmptyWhenMemberHasNone() throws Exception {
        Long otherMembership = f.membership(other.memberId(), timePkg, "TIME", f.cn01, "ACTIVE",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null);
        insertCheckin(other.memberId(), otherMembership, f.cn01, LocalDate.of(2026, 11, 20), false,
            OffsetDateTime.of(2026, 11, 20, 2, 0, 0, 0, ZoneOffset.UTC), null);

        mvc.perform(get("/api/v1/me/checkins").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    @DisplayName("TC-F7.2.1-03a: Không thấy lịch sử check-in của hội viên khác kể cả khi truyền memberId")
    void checkinsNeverLeaksOtherMembersData() throws Exception {
        Long ownMembership = f.membership(me.memberId(), timePkg, "TIME", f.cn01, "ACTIVE",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null);
        Long otherMembership = f.membership(other.memberId(), timePkg, "TIME", f.cn01, "ACTIVE",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null);

        Long ownCheckin = insertCheckin(me.memberId(), ownMembership, f.cn01, LocalDate.of(2026, 11, 20), false,
            OffsetDateTime.of(2026, 11, 20, 2, 0, 0, 0, ZoneOffset.UTC), null);
        insertCheckin(other.memberId(), otherMembership, f.cn01, LocalDate.of(2026, 11, 20), false,
            OffsetDateTime.of(2026, 11, 20, 3, 0, 0, 0, ZoneOffset.UTC), null);
        insertCheckin(other.memberId(), otherMembership, f.cn01, LocalDate.of(2026, 11, 20), false,
            OffsetDateTime.of(2026, 11, 20, 4, 0, 0, 0, ZoneOffset.UTC), null);

        mvc.perform(get("/api/v1/me/checkins")
                .param("memberId", String.valueOf(other.memberId()))
                .with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(ownCheckin));
    }

    @Test
    @DisplayName("TC-F7.2.1-04a: Tham số phân trang check-in không hợp lệ trả 400 kèm lỗi theo trường")
    void checkinsInvalidPagingReturns400() throws Exception {
        mvc.perform(get("/api/v1/me/checkins").param("page", "-1").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.page").isString());

        mvc.perform(get("/api/v1/me/checkins").param("size", "0").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.size").isString());

        mvc.perform(get("/api/v1/me/checkins").param("size", "101").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.size").isString());

        mvc.perform(get("/api/v1/me/checkins").param("page", "abc").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.page").isString());
    }

    @Test
    @DisplayName("TC-F7.2.1-06a: Vai trò khác hoặc MEMBER chưa gắn hồ sơ xem check-in trả 403; không token trả 401")
    void checkinsForbiddenAndUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/me/checkins").with(f.bearer(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isForbidden());

        Long orphan = createOrphanMemberUser("mo-coi-checkin@example.com");
        mvc.perform(get("/api/v1/me/checkins").with(f.bearer(orphan, UserRole.MEMBER)))
            .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/me/checkins"))
            .andExpect(status().isUnauthorized());
    }

    // --- UC-F7.3: GET /api/v1/me/invoices ----------------------------------------------------------

    @Test
    @DisplayName("TC-F7.3.1-01a, TC-F7.3.1-02a: Hóa đơn mới nhất trước, phân trang, có dòng hóa đơn và hạn tự hủy cho hóa đơn chờ MEMBER_PORTAL")
    void invoicesPaginatedNewestFirstWithExpiresAtOnlyForPendingPortal() throws Exception {
        // 1. Cũ nhất: COUNTER + PAID
        clock.setVn(LocalDateTime.of(2026, 11, 18, 8, 0));
        PackageInvoiceDto counterPaid = invoiceService.createPackageInvoice(
            receptionist, f.cn01, me.memberId(), timePkg, InvoiceSource.COUNTER);
        OffsetDateTime paidAt = OffsetDateTime.of(2026, 11, 18, 1, 30, 0, 0, ZoneOffset.UTC);
        jdbc.update("UPDATE invoices SET status = 'PAID', paid_at = ? WHERE id = ?", paidAt, counterPaid.invoiceId());
        jdbc.update("UPDATE memberships SET status = 'ACTIVE' WHERE id = ?", counterPaid.membershipId());

        // 2. COUNTER + PENDING -> không bao giờ tự hủy, expiresAt phải null
        clock.setVn(LocalDateTime.of(2026, 11, 19, 9, 0));
        PackageInvoiceDto counterPending = invoiceService.createPackageInvoice(
            receptionist, f.cn01, me.memberId(), sessionChainPkg, InvoiceSource.COUNTER);

        // 3. MEMBER_PORTAL + CANCELLED -> đã hủy, expiresAt phải null
        clock.setVn(LocalDateTime.of(2026, 11, 19, 15, 0));
        PackageInvoiceDto portalCancelled = invoiceService.createPackageInvoice(
            me.userId(), f.cn01, me.memberId(), ptPkg, InvoiceSource.MEMBER_PORTAL);
        jdbc.update("UPDATE invoices SET status = 'CANCELLED' WHERE id = ?", portalCancelled.invoiceId());
        jdbc.update("UPDATE memberships SET status = 'CANCELLED' WHERE id = ?", portalCancelled.membershipId());

        // 4. Mới nhất: MEMBER_PORTAL + PENDING -> có expiresAt = created_at + 3 ngày
        clock.setVn(LocalDateTime.of(2026, 11, 20, 10, 0));
        PackageInvoiceDto portalPending = invoiceService.createPackageInvoice(
            me.userId(), f.cn01, me.memberId(), ptPkg, InvoiceSource.MEMBER_PORTAL);
        OffsetDateTime portalPendingCreatedAt = jdbc.queryForObject(
            "SELECT created_at FROM invoices WHERE id = ?", OffsetDateTime.class, portalPending.invoiceId());

        String cn01Name = jdbc.queryForObject("SELECT name FROM branches WHERE id = ?", String.class, f.cn01);

        // Trang 0 (size = 2): portalPending rồi portalCancelled
        String page0 = mvc.perform(get("/api/v1/me/invoices")
                .param("page", "0")
                .param("size", "2")
                .with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(4))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].id").value(portalPending.invoiceId()))
            .andExpect(jsonPath("$.content[0].code").value(portalPending.invoiceCode()))
            .andExpect(jsonPath("$.content[0].branchId").value(f.cn01))
            .andExpect(jsonPath("$.content[0].branchName").value(cn01Name))
            .andExpect(jsonPath("$.content[0].source").value("MEMBER_PORTAL"))
            .andExpect(jsonPath("$.content[0].subtotal").value(3_600_000))
            .andExpect(jsonPath("$.content[0].discount").value(0))
            .andExpect(jsonPath("$.content[0].total").value(3_600_000))
            .andExpect(jsonPath("$.content[0].status").value("PENDING"))
            .andExpect(jsonPath("$.content[0].paidAt").value(nullValue()))
            .andExpect(jsonPath("$.content[0].createdAt").isString())
            .andExpect(jsonPath("$.content[0].expiresAt").isString())
            .andExpect(jsonPath("$.content[0].items", hasSize(1)))
            .andExpect(jsonPath("$.content[0].items[0].itemType").value("PACKAGE"))
            .andExpect(jsonPath("$.content[0].items[0].packageId").value(ptPkg))
            .andExpect(jsonPath("$.content[0].items[0].description").value("Gói PT_12"))
            .andExpect(jsonPath("$.content[0].items[0].quantity").value(1))
            .andExpect(jsonPath("$.content[0].items[0].unitPrice").value(3_600_000))
            .andExpect(jsonPath("$.content[0].items[0].amount").value(3_600_000))
            .andExpect(jsonPath("$.content[0].items[0].discountAmount").value(0))
            .andExpect(jsonPath("$.content[1].id").value(portalCancelled.invoiceId()))
            .andExpect(jsonPath("$.content[1].source").value("MEMBER_PORTAL"))
            .andExpect(jsonPath("$.content[1].status").value("CANCELLED"))
            .andExpect(jsonPath("$.content[1].expiresAt").value(nullValue()))
            .andReturn().getResponse().getContentAsString();

        OffsetDateTime returnedExpiresAt = OffsetDateTime.parse(
            json.readTree(page0).get("content").get(0).get("expiresAt").asString());
        assertThat(returnedExpiresAt.toInstant())
            .isEqualTo(portalPendingCreatedAt.toInstant().plus(Duration.ofDays(3)));

        // Trang 1 (size = 2): counterPending rồi counterPaid
        String page1 = mvc.perform(get("/api/v1/me/invoices")
                .param("page", "1")
                .param("size", "2")
                .with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].id").value(counterPending.invoiceId()))
            .andExpect(jsonPath("$.content[0].source").value("COUNTER"))
            .andExpect(jsonPath("$.content[0].status").value("PENDING"))
            .andExpect(jsonPath("$.content[0].expiresAt").value(nullValue()))
            .andExpect(jsonPath("$.content[1].id").value(counterPaid.invoiceId()))
            .andExpect(jsonPath("$.content[1].status").value("PAID"))
            .andExpect(jsonPath("$.content[1].expiresAt").value(nullValue()))
            .andExpect(jsonPath("$.content[1].paidAt").isString())
            .andReturn().getResponse().getContentAsString();

        OffsetDateTime returnedPaidAt = OffsetDateTime.parse(
            json.readTree(page1).get("content").get(1).get("paidAt").asString());
        assertThat(returnedPaidAt.toInstant()).isEqualTo(paidAt.toInstant());
    }

    @Test
    @DisplayName("TC-F7.3.1-02b: Hạn tự hủy đọc PENDING_INVOICE_TTL_DAYS theo chi nhánh từ system_configs, không viết cứng")
    void invoicesExpiresAtUsesBranchConfigAndNullForPaidPortal() throws Exception {
        p.branchTtl(f.cn01, 5); // CN01 = 5 ngày, CN02 dùng toàn chuỗi = 3 ngày
        MemberAccount meCn02 = p.memberAccount("HV000403", "0912000403", f.cn02, "ACTIVE");

        clock.setVn(LocalDateTime.of(2026, 11, 20, 8, 0));
        PackageInvoiceDto portalPaidCn01 = invoiceService.createPackageInvoice(
            me.userId(), f.cn01, me.memberId(), timePkg, InvoiceSource.MEMBER_PORTAL);
        jdbc.update("UPDATE invoices SET status = 'PAID', paid_at = ? WHERE id = ?",
            OffsetDateTime.of(2026, 11, 20, 1, 30, 0, 0, ZoneOffset.UTC), portalPaidCn01.invoiceId());
        jdbc.update("UPDATE memberships SET status = 'ACTIVE' WHERE id = ?", portalPaidCn01.membershipId());

        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        PackageInvoiceDto portalPendingCn02 = invoiceService.createPackageInvoice(
            meCn02.userId(), f.cn02, meCn02.memberId(), cn02Pkg, InvoiceSource.MEMBER_PORTAL);
        OffsetDateTime cn02Created = jdbc.queryForObject(
            "SELECT created_at FROM invoices WHERE id = ?", OffsetDateTime.class, portalPendingCn02.invoiceId());

        clock.setVn(LocalDateTime.of(2026, 11, 20, 10, 0));
        PackageInvoiceDto portalPendingCn01 = invoiceService.createPackageInvoice(
            me.userId(), f.cn01, me.memberId(), ptPkg, InvoiceSource.MEMBER_PORTAL);
        OffsetDateTime cn01Created = jdbc.queryForObject(
            "SELECT created_at FROM invoices WHERE id = ?", OffsetDateTime.class, portalPendingCn01.invoiceId());

        // Hội viên CN01: hóa đơn PENDING dùng TTL riêng của CN01 (5 ngày), hóa đơn PAID có expiresAt = null
        String bodyCn01 = mvc.perform(get("/api/v1/me/invoices").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].id").value(portalPendingCn01.invoiceId()))
            .andExpect(jsonPath("$.content[1].id").value(portalPaidCn01.invoiceId()))
            .andExpect(jsonPath("$.content[1].expiresAt").value(nullValue()))
            .andReturn().getResponse().getContentAsString();

        JsonNode contentCn01 = json.readTree(bodyCn01).get("content");
        assertThat(OffsetDateTime.parse(contentCn01.get(0).get("expiresAt").asString()).toInstant())
            .isEqualTo(cn01Created.toInstant().plus(Duration.ofDays(5)));

        // Hội viên CN02: không có cấu hình riêng ở CN02 -> dùng giá trị toàn chuỗi (3 ngày)
        String bodyCn02 = mvc.perform(get("/api/v1/me/invoices").with(f.bearer(meCn02.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(portalPendingCn02.invoiceId()))
            .andReturn().getResponse().getContentAsString();

        JsonNode contentCn02 = json.readTree(bodyCn02).get("content");
        assertThat(OffsetDateTime.parse(contentCn02.get(0).get("expiresAt").asString()).toInstant())
            .isEqualTo(cn02Created.toInstant().plus(Duration.ofDays(3)));
    }

    @Test
    @DisplayName("TC-F7.3.1-04a: Chưa có hóa đơn nào trả PageResponse rỗng")
    void invoicesEmptyWhenMemberHasNone() throws Exception {
        invoiceService.createPackageInvoice(other.userId(), f.cn01, other.memberId(), timePkg,
            InvoiceSource.MEMBER_PORTAL);

        mvc.perform(get("/api/v1/me/invoices").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    @DisplayName("TC-F7.3.1-03a: Không thấy hóa đơn của hội viên khác và hóa đơn khách lẻ kể cả khi truyền memberId")
    void invoicesNeverLeaksOtherMembersOrWalkInInvoices() throws Exception {
        PackageInvoiceDto ownInv = invoiceService.createPackageInvoice(
            me.userId(), f.cn01, me.memberId(), timePkg, InvoiceSource.MEMBER_PORTAL);
        invoiceService.createPackageInvoice(
            other.userId(), f.cn01, other.memberId(), sessionChainPkg, InvoiceSource.MEMBER_PORTAL);

        // Hóa đơn khách lẻ (member_id IS NULL)
        jdbc.update("""
            INSERT INTO invoices (code, branch_id, member_id, created_by, source, subtotal, discount, total, status)
            VALUES ('HD888001', ?, NULL, ?, 'COUNTER', 20000, 0, 20000, 'PAID')""", f.cn01, receptionist);

        mvc.perform(get("/api/v1/me/invoices")
                .param("memberId", String.valueOf(other.memberId()))
                .with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(ownInv.invoiceId()));
    }

    @Test
    @DisplayName("TC-F7.3.1-05a: Tham số phân trang hóa đơn không hợp lệ trả 400 kèm lỗi theo trường")
    void invoicesInvalidPagingReturns400() throws Exception {
        mvc.perform(get("/api/v1/me/invoices").param("page", "-1").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.page").isString());

        mvc.perform(get("/api/v1/me/invoices").param("size", "0").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.size").isString());

        mvc.perform(get("/api/v1/me/invoices").param("size", "101").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.size").isString());

        mvc.perform(get("/api/v1/me/invoices").param("size", "abc").with(f.bearer(me.userId(), UserRole.MEMBER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.size").isString());
    }

    @Test
    @DisplayName("TC-F7.3.1-06a: Vai trò khác hoặc MEMBER chưa gắn hồ sơ xem hóa đơn trả 403; không token trả 401")
    void invoicesForbiddenAndUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/me/invoices").with(f.bearer(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isForbidden());

        Long orphan = createOrphanMemberUser("mo-coi-hoa-don@example.com");
        mvc.perform(get("/api/v1/me/invoices").with(f.bearer(orphan, UserRole.MEMBER)))
            .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/me/invoices"))
            .andExpect(status().isUnauthorized());
    }

    // --- Helper dựng dữ liệu SQL -------------------------------------------------------------------

    private Long createOrphanMemberUser(String email) {
        return jdbc.queryForObject(
            "INSERT INTO users (email, password_hash, role) VALUES (?, 'test-only', 'MEMBER') RETURNING id",
            Long.class, email);
    }

    private Long insertCheckin(Long memberId, Long membershipId, Long branchId, LocalDate checkinDate,
            boolean sessionDeducted, OffsetDateTime checkedInAt, OffsetDateTime checkedOutAt) {
        return jdbc.queryForObject("""
            INSERT INTO checkins (member_id, membership_id, branch_id, created_by, method, checkin_date,
                                  session_deducted, checked_in_at, checked_out_at)
            VALUES (?, ?, ?, ?, 'MANUAL', ?, ?, ?, ?) RETURNING id""",
            Long.class, memberId, membershipId, branchId, receptionist, checkinDate, sessionDeducted,
            checkedInAt, checkedOutAt);
    }
}
