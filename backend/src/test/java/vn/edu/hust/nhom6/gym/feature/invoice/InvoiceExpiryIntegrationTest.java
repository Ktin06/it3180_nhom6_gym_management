package vn.edu.hust.nhom6.gym.feature.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PackageInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.service.InvoiceService;

/** Hạn thanh toán được kiểm tra độc lập với thời điểm chạy tác vụ tự hủy của F7. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class InvoiceExpiryIntegrationTest {
    private static final String EXPIRED =
        "Hóa đơn chờ đã quá hạn thanh toán, vui lòng lập hóa đơn mới tại quầy.";
    private static final String CONFIG_FILTER =
        "config_key IN ('PENDING_INVOICE_TTL_DAYS', 'BANK_ID', 'BANK_ACCOUNT_NO', 'BANK_ACCOUNT_NAME')";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired InvoiceService invoices;

    private SaleFixtures f;
    private Long receptionist;
    private Long member;
    private Long pkg;
    private List<Map<String, Object>> savedConfigs;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 26, 9, 0));
        f = new SaleFixtures(jdbc, tokens);
        f.reset();
        savedConfigs = jdbc.queryForList(
            "SELECT branch_id, config_key, config_value, description FROM system_configs WHERE " + CONFIG_FILTER);
        jdbc.update("DELETE FROM system_configs WHERE " + CONFIG_FILTER);
        jdbc.update("INSERT INTO system_configs(config_key, config_value) VALUES ('PENDING_INVOICE_TTL_DAYS','3')");
        jdbc.update("INSERT INTO system_configs(config_key, config_value) VALUES ('BANK_ID','MB'), "
            + "('BANK_ACCOUNT_NO','0123456789'), ('BANK_ACCOUNT_NAME','GYM NHOM 6')");
        receptionist = f.staff("expiry@example.com", UserRole.RECEPTIONIST, f.cn01);
        member = f.member("HV000123", "0912000123", f.cn01, "ACTIVE");
        pkg = f.pkg("TIME_EXPIRY", "TIME", null, 500_000, f.cn01, "ACTIVE");
    }

    @AfterEach
    void restoreConfigs() {
        if (savedConfigs == null) {
            return;
        }
        jdbc.update("DELETE FROM system_configs WHERE " + CONFIG_FILTER);
        savedConfigs.forEach(row -> jdbc.update("""
            INSERT INTO system_configs(branch_id, config_key, config_value, description) VALUES (?, ?, ?, ?)
            """, row.get("branch_id"), row.get("config_key"), row.get("config_value"), row.get("description")));
    }

    @ParameterizedTest(name = "source={0}, branch TTL={1}, age={2}s → {3}")
    @CsvSource({
        "MEMBER_PORTAL, 0, 259199, 200",
        "MEMBER_PORTAL, 0, 259200, 200",
        "MEMBER_PORTAL, 0, 259201, 422",
        "MEMBER_PORTAL, 2, 172799, 200",
        "MEMBER_PORTAL, 2, 172800, 200",
        "MEMBER_PORTAL, 2, 172801, 422",
        "MEMBER_PORTAL, 5, 431999, 200",
        "MEMBER_PORTAL, 5, 432000, 200",
        "MEMBER_PORTAL, 5, 432001, 422",
        "COUNTER, 0, 259201, 200",
        "COUNTER, 2, 999999, 200"
    })
    @DisplayName("TC-F5.1.1-08a, TC-F5.2.1-12a: VietQR và thu tiền chặn portal quá TTL; kiểm tra biên, fallback/ghi đè chi nhánh và miễn hạn COUNTER")
    void paymentAndVietQrRespectPortalDeadline(InvoiceSource source, int branchTtl, long ageSeconds,
            int expectedStatus) throws Exception {
        if (branchTtl > 0) {
            jdbc.update("INSERT INTO system_configs(branch_id, config_key, config_value) "
                + "VALUES (?, 'PENDING_INVOICE_TTL_DAYS', ?)", f.cn01, Integer.toString(branchTtl));
        }
        PackageInvoiceDto sale = invoices.createPackageInvoice(receptionist, f.cn01, member, pkg, source);
        OffsetDateTime createdAt = clock.instant().minusSeconds(ageSeconds).atOffset(ZoneOffset.UTC);
        jdbc.update("UPDATE invoices SET created_at = ? WHERE id = ?", createdAt, sale.invoiceId());

        var qr = mvc.perform(get("/api/v1/invoices/{id}/vietqr", sale.invoiceId())
            .with(f.bearer(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().is(expectedStatus));
        assertPending(sale);
        var payment = mvc.perform(post("/api/v1/invoices/{id}/payments", sale.invoiceId())
            .with(f.bearer(receptionist, UserRole.RECEPTIONIST))
            .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"CASH\"}"))
            .andExpect(status().is(expectedStatus));

        if (expectedStatus == 422) {
            qr.andExpect(jsonPath("$.detail").value(EXPIRED));
            payment.andExpect(jsonPath("$.detail").value(EXPIRED));
            assertPending(sale);
        } else {
            qr.andExpect(jsonPath("$.total").value(500_000));
            payment.andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.activatedMemberships[0].status").value("ACTIVE"));
            assertThat(f.count("payments")).isEqualTo(1);
        }
    }

    private void assertPending(PackageInvoiceDto sale) {
        assertThat(jdbc.queryForMap("SELECT status, version, paid_at FROM invoices WHERE id = ?", sale.invoiceId()))
            .containsEntry("status", "PENDING").containsEntry("version", 0).containsEntry("paid_at", null);
        assertThat(jdbc.queryForObject("SELECT status FROM memberships WHERE id = ?", String.class,
            sale.membershipId())).isEqualTo("PENDING");
        assertThat(f.count("payments")).isZero();
    }
}
