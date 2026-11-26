package vn.edu.hust.nhom6.gym.feature.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.invoice.SaleFixtures;
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipService;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * {@code GET /api/v1/memberships?memberId=} (AC-F3.5.1-12) và truy vấn lõi
 * {@code MembershipService.findUsable}. Hôm nay 20/11/2026 giờ Việt Nam.
 */
@SpringBootTest(properties = "app.scheduling.enabled=false")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class MembershipListIntegrationTest {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired MembershipService memberships;

    SaleFixtures f;
    Long receptionist;
    Long member;
    Long timePkg;
    Long sessionPkg;
    Long ptPkg;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        f = new SaleFixtures(jdbc, tokens);
        f.reset();
        receptionist = f.staff("le-tan@example.com", UserRole.RECEPTIONIST, f.cn01);
        member = f.member("HV000123", "0912000123", f.cn01, "ACTIVE");
        timePkg = f.pkg("TIME_30", "TIME", null, 500_000, f.cn01, "ACTIVE");
        sessionPkg = f.pkg("SES_10", "SESSION", 10, 800_000, f.cn01, "ACTIVE");
        ptPkg = f.pkg("PT_10", "PT", 10, 3_000_000, f.cn01, "ACTIVE");
    }

    @Test
    @DisplayName("TC-F3.5.1-12a: danh sách gói kèm renewalStartDate do server tính; gói đang chờ chặn gia hạn; bỏ gói EXPIRED")
    void listsHeldMembershipsWithRenewalStartDate() throws Exception {
        f.membership(member, timePkg, "TIME", f.cn01, "EXPIRED", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30), null);
        f.membership(member, timePkg, "TIME", f.cn01, "ACTIVE", LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 31), null);
        f.membership(member, sessionPkg, "SESSION", f.cn01, "ACTIVE", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 0);
        f.membership(member, ptPkg, "PT", f.cn01, "ACTIVE", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 4);
        f.membership(member, ptPkg, "PT", f.cn01, "PENDING", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 30), 10);

        mvc.perform(get("/api/v1/memberships").param("memberId", member.toString())
                .with(f.bearer(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(4)))
            .andExpect(jsonPath("$[?(@.packageType=='TIME')].renewalStartDate").value("2027-01-01"))
            .andExpect(jsonPath("$[?(@.packageType=='TIME')].packageName").value("Gói TIME_30"))
            .andExpect(jsonPath("$[?(@.packageType=='SESSION')].renewalStartDate").value("2026-11-21"))
            .andExpect(jsonPath("$[?(@.packageType=='SESSION')].remainingSessions").value(0))
            .andExpect(jsonPath("$[?(@.packageType=='PT' && @.status=='ACTIVE')].renewalStartDate").value(contains(nullValue())))
            .andExpect(jsonPath("$[?(@.packageType=='PT' && @.status=='PENDING')].renewalStartDate").value(contains(nullValue())));
    }

    @Test
    @DisplayName("TC-F3.5.1-12b: renewalStartDate trùng ngày bắt đầu dự kiến khi bán thật (cùng một hàm tính)")
    void renewalStartDateMatchesSale() throws Exception {
        f.membership(member, timePkg, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(10), TODAY.plusDays(5), null);
        mvc.perform(get("/api/v1/memberships").param("memberId", member.toString())
                .with(f.bearer(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(jsonPath("$[0].renewalStartDate").value(TODAY.plusDays(6).toString()));
        mvc.perform(post("/api/v1/invoices").with(f.bearer(receptionist, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\": %d, \"items\": [{\"itemType\": \"PACKAGE\", \"packageId\": %d}]}"
                    .formatted(member, timePkg)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.startDate").value(TODAY.plusDays(6).toString()));
    }

    @Test
    @DisplayName("TC-F3.5.1-12c: hội viên ngoài chi nhánh 404; chủ chuỗi xem được; quản lý xem được; HLV, hội viên 403; thiếu memberId 400")
    void scopeAndRoles() throws Exception {
        Long cn02Member = f.member("HV000125", "0912000125", f.cn02, "ACTIVE");
        mvc.perform(get("/api/v1/memberships").param("memberId", cn02Member.toString())
                .with(f.bearer(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isNotFound());

        Long owner = f.staff("chu@example.com", UserRole.SUPER_ADMIN, null);
        mvc.perform(get("/api/v1/memberships").param("memberId", cn02Member.toString())
                .with(f.bearer(owner, UserRole.SUPER_ADMIN)))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        Long manager = f.staff("ql@example.com", UserRole.BRANCH_MANAGER, f.cn01);
        mvc.perform(get("/api/v1/memberships").param("memberId", member.toString())
                .with(f.bearer(manager, UserRole.BRANCH_MANAGER)))
            .andExpect(status().isOk());

        for (UserRole role : new UserRole[] {UserRole.TRAINER, UserRole.MEMBER}) {
            mvc.perform(get("/api/v1/memberships").param("memberId", member.toString())
                    .with(f.bearer(receptionist, role)))
                .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/memberships").with(f.bearer(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/memberships").param("memberId", member.toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("BR-F3-10: findUsable trả gói ACTIVE của chi nhánh hoặc toàn chuỗi còn trong hạn, kể cả SESSION 0 buổi")
    void findUsableReturnsActiveInRangeForBranchOrChain() {
        Long chainSession = f.pkg("ALL_SES", "SESSION", 10, 800_000, null, "ACTIVE");
        Long cn02Session = f.pkg("CN02_SES", "SESSION", 10, 800_000, f.cn02, "ACTIVE");
        Long own = f.membership(member, sessionPkg, "SESSION", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY, 0);
        Long chain = f.membership(member, chainSession, "SESSION", null, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(5), 3);
        f.membership(member, cn02Session, "SESSION", f.cn02, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(5), 3);
        f.membership(member, timePkg, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(5), null);

        assertThat(memberships.findUsable(member, f.cn01, PackageType.SESSION, TODAY))
            .extracting(m -> m.getId()).containsExactly(own, chain);
        assertThat(memberships.findUsable(member, f.cn01, PackageType.SESSION, TODAY.plusDays(1)))
            .extracting(m -> m.getId()).containsExactly(chain);
        assertThat(memberships.findUsable(member, f.cn01, PackageType.PT, TODAY)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "1.1"})
    @DisplayName("TC-F3.5.1-12d: ID hội viên không phải số nguyên dương bị 400")
    void invalidMemberIdIsRejected(String id) throws Exception {
        mvc.perform(get("/api/v1/memberships").param("memberId", id)
                .with(f.bearer(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isMap());
    }

    @Test
    @DisplayName("TC-F3.5.1-02a: Trước thanh toán, gói PENDING không được trả trong findUsable")
    void pendingSaleIsNotUsableBeforePayment() throws Exception {
        mvc.perform(post("/api/v1/invoices").with(f.bearer(receptionist, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":%d,\"items\":[{\"itemType\":\"PACKAGE\",\"packageId\":%d}]}".formatted(member, timePkg)))
            .andExpect(status().isCreated());
        assertThat(memberships.findUsable(member, f.cn01, PackageType.TIME, TODAY)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT status FROM memberships", String.class)).isEqualTo("PENDING");
    }
}
