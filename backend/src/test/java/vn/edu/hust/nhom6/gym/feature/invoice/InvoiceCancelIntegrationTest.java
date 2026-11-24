package vn.edu.hust.nhom6.gym.feature.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PackageInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.service.InvoiceService;

/** {@code PUT /api/v1/invoices/{id}/cancel}: hủy hóa đơn chờ tại quầy (UC-F5.3 luồng A1, BR-F5-08, S2-09.1). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class InvoiceCancelIntegrationTest {
    static final String NOT_PENDING = "Chỉ có thể hủy hóa đơn đang ở trạng thái chờ thanh toán.";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired InvoiceService invoiceService;

    SaleFixtures f;
    Long receptionist;
    Long member;
    Long timePkg;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        f = new SaleFixtures(jdbc, tokens);
        f.reset();
        receptionist = f.staff("le-tan@example.com", UserRole.RECEPTIONIST, f.cn01);
        member = f.member("HV000123", "0912000123", f.cn01, "ACTIVE");
        timePkg = f.pkg("TIME_30", "TIME", null, 500_000, f.cn01, "ACTIVE");
    }

    @ParameterizedTest
    @EnumSource(InvoiceSource.class)
    @DisplayName("TC-F5.3.1-06a: Hủy hóa đơn PENDING từ quầy hoặc cổng hội viên đồng thời hủy gói chờ")
    void cancelPendingInvoiceCancelsMembership(InvoiceSource source) throws Exception {
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(receptionist, f.cn01, member, timePkg, source);
        cancel(receptionist, UserRole.RECEPTIONIST, sale.invoiceId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(sale.invoiceId()))
            .andExpect(jsonPath("$.code").value(sale.invoiceCode()))
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.updatedAt").isString());
        assertThat(jdbc.queryForObject("SELECT status FROM invoices", String.class)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT status FROM memberships WHERE id = ?", String.class,
            sale.membershipId())).isEqualTo("CANCELLED");
        assertThat(f.count("payments")).isZero();
    }

    @Test
    @DisplayName("TC-F5.3.1-06b: Hủy xong thì bán lại được gói cùng loại (gói chờ cũ không còn chặn)")
    void cancelledInvoiceNoLongerBlocksSale() throws Exception {
        PackageInvoiceDto sale = sell();
        cancel(receptionist, UserRole.RECEPTIONIST, sale.invoiceId()).andExpect(status().isOk());
        assertThat(sell().membershipStatus().name()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("TC-F5.3.1-07a: Hủy hóa đơn đã PAID thì 422; hóa đơn, gói, phiếu thu không đổi")
    void cancelPaidInvoiceIs422() throws Exception {
        PackageInvoiceDto sale = sell();
        mvc.perform(post("/api/v1/invoices/{id}/payments", sale.invoiceId())
                .with(f.bearer(receptionist, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\": \"CASH\"}"))
            .andExpect(status().isOk());

        cancel(receptionist, UserRole.RECEPTIONIST, sale.invoiceId())
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(NOT_PENDING));
        assertThat(jdbc.queryForObject("SELECT status FROM invoices", String.class)).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("SELECT status FROM memberships", String.class)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT status FROM payments", String.class)).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("TC-F5.3.1-07b: Hủy lần hai hóa đơn đã CANCELLED thì 422")
    void cancelTwiceIs422() throws Exception {
        PackageInvoiceDto sale = sell();
        cancel(receptionist, UserRole.RECEPTIONIST, sale.invoiceId()).andExpect(status().isOk());
        cancel(receptionist, UserRole.RECEPTIONIST, sale.invoiceId())
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(NOT_PENDING));
    }

    @Test
    @DisplayName("TC-F5.3.1-08a: Hóa đơn không tồn tại thì 404")
    void unknownInvoiceIs404() throws Exception {
        cancel(receptionist, UserRole.RECEPTIONIST, 999_999L)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Hóa đơn không tồn tại trên hệ thống."));
    }

    @Test
    @DisplayName("TC-F5.3.1-08b: Lễ tân chi nhánh khác hủy hóa đơn CN01 thì 404, hóa đơn vẫn PENDING")
    void otherBranchIs404() throws Exception {
        Long other = f.staff("le-tan-cn02@example.com", UserRole.RECEPTIONIST, f.cn02);
        PackageInvoiceDto sale = sell();
        cancel(other, UserRole.RECEPTIONIST, sale.invoiceId()).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT status FROM invoices", String.class)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT status FROM memberships", String.class)).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("TC-F5.3.1-06c: Quản lý chi nhánh và chủ chuỗi hủy được hóa đơn chờ")
    void managerAndOwnerCanCancel() throws Exception {
        Long manager = f.staff("quan-ly@example.com", UserRole.BRANCH_MANAGER, f.cn01);
        Long owner = f.staff("chu-chuoi@example.com", UserRole.SUPER_ADMIN, null);
        PackageInvoiceDto first = sell();
        cancel(manager, UserRole.BRANCH_MANAGER, first.invoiceId()).andExpect(status().isOk());
        PackageInvoiceDto second = sell();
        cancel(owner, UserRole.SUPER_ADMIN, second.invoiceId()).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM invoices WHERE status = 'CANCELLED'", Long.class))
            .isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"TRAINER", "MEMBER"})
    @DisplayName("TC-F5.3.1-08c: Huấn luyện viên, hội viên hủy hóa đơn thì 403")
    void otherRolesAreForbidden(UserRole role) throws Exception {
        Long user = f.staff("khac-" + role.name().toLowerCase() + "@example.com", role,
            role == UserRole.TRAINER ? f.cn01 : null);
        PackageInvoiceDto sale = sell();
        cancel(user, role, sale.invoiceId()).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT status FROM invoices", String.class)).isEqualTo("PENDING");
    }

    private PackageInvoiceDto sell() {
        return invoiceService.createPackageInvoice(receptionist, f.cn01, member, timePkg, InvoiceSource.COUNTER);
    }

    private ResultActions cancel(Long userId, UserRole role, Long invoiceId) throws Exception {
        return mvc.perform(put("/api/v1/invoices/{id}/cancel", invoiceId).with(f.bearer(userId, role)));
    }
}
