package vn.edu.hust.nhom6.gym.feature.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipService;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * {@code POST /api/v1/invoices/{id}/payments} qua API thật, JWT thật, PostgreSQL thật (UC-F5.2, S2-09.1).
 * Hóa đơn PENDING lập bằng {@link InvoiceService#createPackageInvoice} (luồng bán thật của S2-07.1) ngày 20/11;
 * thu tiền ngày khác thì đổi {@link MutableClock}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class InvoicePaymentIntegrationTest {
    static final LocalDate SALE_DAY = LocalDate.of(2026, 11, 20);
    static final String E6B = "Hội viên đã có gói cùng loại đang chờ kích hoạt (UPCOMING). Không thể kích hoạt thêm "
        + "gói này. Vui lòng liên hệ quản lý để kiểm tra hoặc hoàn tiền hóa đơn.";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired InvoiceService invoiceService;
    @Autowired MembershipService membershipService;

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

    @Test
    @DisplayName("TC-F5.2.1-01a, TC-F3.5.1-02b: Thu tiền ngày 25/11 kích hoạt gói ACTIVE 25/11–24/12, số ngày lấy từ gói PENDING, không đọc lại packages.duration_days; trước PAID gói không dùng được")
    void payActivatesWithDatesRecomputedAtPaymentTime() throws Exception {
        PackageInvoiceDto sale = sellAtCounter(timePkg);
        assertThat(membershipService.findUsable(member, f.cn01, PackageType.TIME, SALE_DAY))
            .as("gói PENDING chưa dùng được").isEmpty();

        // Quản lý sửa thời hạn gói trong danh mục khi hóa đơn còn chờ (BR-F3-06): không được ảnh hưởng gói đã bán.
        jdbc.update("UPDATE packages SET duration_days = 90 WHERE id = ?", timePkg);
        clock.setVn(LocalDateTime.of(2026, 11, 25, 10, 30));

        pay(receptionist, sale.invoiceId(), "{\"method\": \"CASH\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").isNumber())
            .andExpect(jsonPath("$.invoiceId").value(sale.invoiceId()))
            .andExpect(jsonPath("$.invoiceCode").value(sale.invoiceCode()))
            .andExpect(jsonPath("$.method").value("CASH"))
            .andExpect(jsonPath("$.amount").value(500_000))
            .andExpect(jsonPath("$.status").value("PAID"))
            .andExpect(jsonPath("$.paidAt").value("2026-11-25T03:30:00Z"))
            .andExpect(jsonPath("$.receivedBy").value(receptionist))
            .andExpect(jsonPath("$.receivedByName").value("Nhân viên test"))
            .andExpect(jsonPath("$.activatedMemberships.length()").value(1))
            .andExpect(jsonPath("$.activatedMemberships[0].membershipId").value(sale.membershipId()))
            .andExpect(jsonPath("$.activatedMemberships[0].packageType").value("TIME"))
            .andExpect(jsonPath("$.activatedMemberships[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.activatedMemberships[0].startDate").value("2026-11-25"))
            .andExpect(jsonPath("$.activatedMemberships[0].endDate").value("2026-12-24"));

        OffsetDateTime paidAt = OffsetDateTime.of(2026, 11, 25, 3, 30, 0, 0, ZoneOffset.UTC);
        Map<String, Object> invoice = jdbc.queryForMap("SELECT status, paid_at, version FROM invoices");
        assertThat(invoice).containsEntry("status", "PAID").containsEntry("version", 1);
        assertThat(((java.sql.Timestamp) invoice.get("paid_at")).toInstant()).isEqualTo(paidAt.toInstant());

        Map<String, Object> payment = jdbc.queryForMap("SELECT * FROM payments");
        assertThat(payment).containsEntry("invoice_id", sale.invoiceId()).containsEntry("method", "CASH")
            .containsEntry("amount", 500_000).containsEntry("status", "CONFIRMED")
            .containsEntry("received_by", receptionist);
        assertThat(payment.get("transaction_ref")).isNull();
        assertThat(((java.sql.Timestamp) payment.get("paid_at")).toInstant()).isEqualTo(paidAt.toInstant());

        Map<String, Object> m = jdbc.queryForMap("SELECT status, start_date, end_date FROM memberships");
        assertThat(m).containsEntry("status", "ACTIVE");
        assertThat(m.get("start_date").toString()).isEqualTo("2026-11-25");
        assertThat(m.get("end_date").toString()).isEqualTo("2026-12-24");
        assertThat(membershipService.findUsable(member, f.cn01, PackageType.TIME, LocalDate.of(2026, 11, 25)))
            .hasSize(1);
    }

    @Test
    @DisplayName("TC-F5.2.1-02a, TC-F3.5.1-03c: Thu tiền gia hạn khi gói TIME cũ ACTIVE tới 31/12: gói mới UPCOMING 01/01–30/01, gói cũ giữ nguyên")
    void payRenewalAfterActiveTimePackageIsUpcoming() throws Exception {
        Long old = f.membership(member, timePkg, "TIME", f.cn01, "ACTIVE",
            LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 31), null);
        PackageInvoiceDto sale = sellAtCounter(timePkg);

        pay(receptionist, sale.invoiceId(), "{\"method\": \"BANK_TRANSFER\", \"transactionRef\": \"FT263078912345\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.method").value("BANK_TRANSFER"))
            .andExpect(jsonPath("$.activatedMemberships[0].status").value("UPCOMING"))
            .andExpect(jsonPath("$.activatedMemberships[0].startDate").value("2027-01-01"))
            .andExpect(jsonPath("$.activatedMemberships[0].endDate").value("2027-01-30"));

        assertThat(jdbc.queryForObject("SELECT transaction_ref FROM payments", String.class))
            .isEqualTo("FT263078912345");
        Map<String, Object> oldRow = jdbc.queryForMap(
            "SELECT status, start_date, end_date FROM memberships WHERE id = ?", old);
        assertThat(oldRow).containsEntry("status", "ACTIVE");
        assertThat(oldRow.get("end_date").toString()).isEqualTo("2026-12-31");
    }

    @Test
    @DisplayName("TC-F5.2.1-02b: Gói cũ FROZEN tới 31/12 thì gói mới cũng UPCOMING từ 01/01, gói cũ vẫn FROZEN")
    void payRenewalAfterFrozenPackageIsUpcoming() throws Exception {
        Long old = f.membership(member, timePkg, "TIME", f.cn01, "FROZEN",
            LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 31), null);
        PackageInvoiceDto sale = sellAtCounter(timePkg);

        pay(receptionist, sale.invoiceId(), "{\"method\": \"CASH\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activatedMemberships[0].status").value("UPCOMING"))
            .andExpect(jsonPath("$.activatedMemberships[0].startDate").value("2027-01-01"));
        assertThat(jdbc.queryForObject("SELECT status FROM memberships WHERE id = ?", String.class, old))
            .isEqualTo("FROZEN");
    }

    @Test
    @DisplayName("TC-F5.2.1-02c, TC-F3.5.1-04d: Thu tiền gia hạn khi gói SESSION cũ còn hạn tới 30/11 nhưng đã 0 buổi: gói mới UPCOMING bắt đầu ngày mai")
    void payRenewalAfterEmptySessionPackageStartsTomorrow() throws Exception {
        Long session = f.pkg("SES_10", "SESSION", 10, 800_000, f.cn01, "ACTIVE");
        Long old = f.membership(member, session, "SESSION", f.cn01, "ACTIVE", LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30), 3);
        PackageInvoiceDto sale = sellAtCounter(session);
        // Hội viên tập tiếp hai ngày sau, buổi cuối bị trừ ngày 22/11 rồi mới ra quầy trả tiền.
        jdbc.update("UPDATE memberships SET remaining_sessions = 0 WHERE id = ?", old);
        clock.setVn(LocalDateTime.of(2026, 11, 22, 18, 0));

        pay(receptionist, sale.invoiceId(), "{\"method\": \"CASH\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activatedMemberships[0].status").value("UPCOMING"))
            .andExpect(jsonPath("$.activatedMemberships[0].startDate").value("2026-11-23"))
            .andExpect(jsonPath("$.activatedMemberships[0].endDate").value("2026-12-22"))
            .andExpect(jsonPath("$.activatedMemberships[0].remainingSessions").value(10));
        assertThat(jdbc.queryForObject("SELECT status FROM memberships WHERE id = ?", String.class, old))
            .isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("TC-F5.2.1-01b, TC-F3.5.1-05b: Có gói TIME ACTIVE, thu tiền gói PT thì PT ACTIVE từ hôm nay; gói TIME không đổi")
    void payPtWhileHoldingTimeActivatesPt() throws Exception {
        Long time = f.membership(member, timePkg, "TIME", f.cn01, "ACTIVE",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null);
        Long pt = f.pkg("PT_10", "PT", 10, 3_000_000, f.cn01, "ACTIVE");
        PackageInvoiceDto sale = sellAtCounter(pt);

        pay(receptionist, sale.invoiceId(), "{\"method\": \"CASH\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(3_000_000))
            .andExpect(jsonPath("$.activatedMemberships[0].packageType").value("PT"))
            .andExpect(jsonPath("$.activatedMemberships[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.activatedMemberships[0].startDate").value("2026-11-20"))
            .andExpect(jsonPath("$.activatedMemberships[0].remainingSessions").value(10));
        Map<String, Object> timeRow = jdbc.queryForMap("SELECT status, end_date FROM memberships WHERE id = ?", time);
        assertThat(timeRow).containsEntry("status", "ACTIVE");
        assertThat(timeRow.get("end_date").toString()).isEqualTo("2026-11-30");
    }

    @Test
    @DisplayName("TC-F5.2.1-04a: Quản lý chi nhánh cũng thu tiền được")
    void branchManagerCanPay() throws Exception {
        Long manager = f.staff("quan-ly@example.com", UserRole.BRANCH_MANAGER, f.cn01);
        PackageInvoiceDto sale = sellAtCounter(timePkg);
        mvc.perform(post("/api/v1/invoices/{id}/payments", sale.invoiceId())
                .with(f.bearer(manager, UserRole.BRANCH_MANAGER))
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\": \"CASH\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.receivedBy").value(manager));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"method\": null}", "{\"method\": \"MOMO\"}", "{\"method\": \"cash\"}"})
    @DisplayName("TC-F5.2.1-05a: method thiếu hoặc khác CASH, BANK_TRANSFER thì 400 có lỗi trường method, không ghi gì")
    void invalidMethodIs400(String body) throws Exception {
        PackageInvoiceDto sale = sellAtCounter(timePkg);
        pay(receptionist, sale.invoiceId(), body)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.method").isString());
        assertUnpaid(sale);
    }

    @Test
    @DisplayName("TC-F5.2.1-05b: transactionRef dài quá 100 ký tự thì 400")
    void tooLongTransactionRefIs400() throws Exception {
        PackageInvoiceDto sale = sellAtCounter(timePkg);
        pay(receptionist, sale.invoiceId(),
            "{\"method\": \"BANK_TRANSFER\", \"transactionRef\": \"%s\"}".formatted("X".repeat(101)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.transactionRef").isString());
        assertUnpaid(sale);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"TRAINER", "MEMBER", "SUPER_ADMIN"})
    @DisplayName("TC-F5.2.1-04b: Huấn luyện viên, hội viên, chủ chuỗi xác nhận thu tiền thì 403 (BR-F5-03)")
    void otherRolesAreForbidden(UserRole role) throws Exception {
        Long user = f.staff("khac-" + role.name().toLowerCase() + "@example.com", role,
            role == UserRole.TRAINER ? f.cn01 : null);
        PackageInvoiceDto sale = sellAtCounter(timePkg);
        mvc.perform(post("/api/v1/invoices/{id}/payments", sale.invoiceId()).with(f.bearer(user, role))
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\": \"CASH\"}"))
            .andExpect(status().isForbidden());
        assertUnpaid(sale);
    }

    @Test
    @DisplayName("TC-F5.2.1-04c: Không có token thì 401")
    void noTokenIs401() throws Exception {
        PackageInvoiceDto sale = sellAtCounter(timePkg);
        mvc.perform(post("/api/v1/invoices/{id}/payments", sale.invoiceId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\": \"CASH\"}"))
            .andExpect(status().isUnauthorized());
        assertUnpaid(sale);
    }

    @Test
    @DisplayName("TC-F5.2.1-06a: Hóa đơn không tồn tại thì 404")
    void unknownInvoiceIs404() throws Exception {
        pay(receptionist, 999_999L, "{\"method\": \"CASH\"}")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Hóa đơn không tồn tại trên hệ thống."));
    }

    @Test
    @DisplayName("TC-F5.2.1-06b: Lễ tân chi nhánh khác thu tiền hóa đơn CN01 thì 404, không lộ hóa đơn")
    void otherBranchIs404() throws Exception {
        Long otherReceptionist = f.staff("le-tan-cn02@example.com", UserRole.RECEPTIONIST, f.cn02);
        PackageInvoiceDto sale = sellAtCounter(timePkg);
        pay(otherReceptionist, sale.invoiceId(), "{\"method\": \"CASH\"}")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Hóa đơn không tồn tại trên hệ thống."));
        assertUnpaid(sale);
    }

    @Test
    @DisplayName("TC-F5.2.1-08a: Thu tiền lần hai cho hóa đơn đã PAID thì 422, chỉ có một phiếu thu")
    void payingTwiceIs422() throws Exception {
        PackageInvoiceDto sale = sellAtCounter(timePkg);
        pay(receptionist, sale.invoiceId(), "{\"method\": \"CASH\"}").andExpect(status().isOk());
        pay(receptionist, sale.invoiceId(), "{\"method\": \"CASH\"}")
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Hóa đơn đã được thanh toán hoặc đã bị hủy trước đó."));
        assertThat(f.count("payments")).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-F5.2.1-08b: Thu tiền hóa đơn đã hủy thì 422, gói vẫn CANCELLED")
    void payingCancelledInvoiceIs422() throws Exception {
        PackageInvoiceDto sale = sellAtCounter(timePkg);
        mvc.perform(put("/api/v1/invoices/{id}/cancel", sale.invoiceId())
                .with(f.bearer(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk());
        pay(receptionist, sale.invoiceId(), "{\"method\": \"CASH\"}")
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Hóa đơn đã được thanh toán hoặc đã bị hủy trước đó."));
        assertThat(f.count("payments")).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM memberships", String.class)).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("TC-F5.2.1-09b: Đã có gói cùng loại UPCOMING (vừa thu tiền hóa đơn khác) thì 422, không ghi phiếu thu, hóa đơn và gói vẫn PENDING")
    void upcomingSameTypeBlocksActivation() throws Exception {
        f.membership(member, timePkg, "TIME", f.cn01, "ACTIVE",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null);
        PackageInvoiceDto sale = sellAtCounter(timePkg);
        // Gói UPCOMING xuất hiện sau khi lập hóa đơn (ví dụ hóa đơn cổng hội viên F7 được thu tiền trước).
        f.membership(member, timePkg, "TIME", f.cn01, "UPCOMING",
            LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 30), null);

        pay(receptionist, sale.invoiceId(), "{\"method\": \"CASH\"}")
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(E6B));
        assertUnpaid(sale);
    }

    @Test
    @DisplayName("TC-F5.2.1-09c: Gói UPCOMING khác loại hoặc khác phạm vi chi nhánh không chặn thu tiền")
    void upcomingOfOtherTypeOrScopeDoesNotBlock() throws Exception {
        Long pt = f.pkg("PT_10", "PT", 10, 3_000_000, f.cn01, "ACTIVE");
        f.membership(member, pt, "PT", f.cn01, "UPCOMING",
            LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 30), 10);
        Long chainTime = f.pkg("TIME_ALL", "TIME", null, 700_000, null, "ACTIVE");
        f.membership(member, chainTime, "TIME", null, "UPCOMING",
            LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 30), null);
        PackageInvoiceDto sale = sellAtCounter(timePkg);

        pay(receptionist, sale.invoiceId(), "{\"method\": \"CASH\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activatedMemberships[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("TC-F5.2.1-03a: hóa đơn 0 đồng PAID, kích hoạt gói và ghi phiếu thu 0 đồng; phiếu thu số tiền âm vẫn bị CHECK chặn (V9_06)")
    void zeroTotalInvoiceActivatesWithZeroPaymentRow() throws Exception {
        Long free = f.pkg("TIME_FREE", "TIME", null, 0, f.cn01, "ACTIVE");
        PackageInvoiceDto sale = sellAtCounter(free);
        pay(receptionist, sale.invoiceId(), "{\"method\": \"CASH\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").isNumber())
            .andExpect(jsonPath("$.method").value("CASH"))
            .andExpect(jsonPath("$.amount").value(0))
            .andExpect(jsonPath("$.status").value("PAID"))
            .andExpect(jsonPath("$.activatedMemberships[0].status").value("ACTIVE"));
        Map<String, Object> row = jdbc.queryForMap(
            "SELECT amount, status, received_by FROM payments WHERE invoice_id = ?", sale.invoiceId());
        assertThat(row).containsEntry("amount", 0).containsEntry("status", "CONFIRMED")
            .containsEntry("received_by", receptionist);

        assertThatThrownBy(() -> jdbc.update("UPDATE payments SET amount = -1 WHERE invoice_id = ?",
            sale.invoiceId()))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
            .hasMessageContaining("ck_payments_amount");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CASH", "BANK_TRANSFER"})
    @DisplayName("TC-F5.2.1-10a: Tiền giả client gửi không thay đổi số tiền thu hoặc đơn giá lịch sử")
    void ignoresClientSuppliedAmounts(String method) throws Exception {
        PackageInvoiceDto sale = sellAtCounter(timePkg);
        pay(receptionist, sale.invoiceId(),
            "{\"method\":\"%s\",\"amount\":1,\"total\":1,\"unitPrice\":1}".formatted(method))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(500_000));
        assertThat(jdbc.queryForObject("SELECT amount FROM payments WHERE invoice_id=?", Integer.class,
            sale.invoiceId())).isEqualTo(500_000);
        assertThat(jdbc.queryForObject("SELECT total FROM invoices WHERE id=?", Integer.class,
            sale.invoiceId())).isEqualTo(500_000);
        assertThat(jdbc.queryForObject("SELECT unit_price FROM invoice_items WHERE invoice_id=?", Integer.class,
            sale.invoiceId())).isEqualTo(500_000);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "FROZEN"})
    @DisplayName("TC-F5.2.1-11a: PT 0 buổi còn ACTIVE hoặc FROZEN xếp gói mới từ ngày mai theo nền S2-07")
    void zeroSessionPtStillHeldSchedulesTomorrow(String oldStatus) throws Exception {
        Long pt = f.pkg("PT_EMPTY", "PT", 10, 3_000_000, f.cn01, "ACTIVE");
        Long old = f.membership(member, pt, "PT", f.cn01, oldStatus,
            SALE_DAY.minusDays(5), SALE_DAY.plusDays(15), 0);
        PackageInvoiceDto sale = sellAtCounter(pt);
        pay(receptionist, sale.invoiceId(), "{\"method\":\"CASH\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activatedMemberships[0].status").value("UPCOMING"))
            .andExpect(jsonPath("$.activatedMemberships[0].startDate").value("2026-11-21"))
            .andExpect(jsonPath("$.activatedMemberships[0].endDate").value("2026-12-20"))
            .andExpect(jsonPath("$.activatedMemberships[0].remainingSessions").value(10));
        assertThat(jdbc.queryForObject("SELECT status FROM memberships WHERE id=?", String.class, old))
            .isEqualTo(oldStatus);
        assertThat(jdbc.queryForObject("SELECT remaining_sessions FROM memberships WHERE id=?", Integer.class, old))
            .isZero();
    }

    @Test
    @DisplayName("TC-F5.2.1-01c: Thu tiền hóa đơn MEMBER_PORTAL kích hoạt gói như hóa đơn tại quầy")
    void paysPortalInvoiceAtCounter() throws Exception {
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(
            receptionist, f.cn01, member, timePkg, InvoiceSource.MEMBER_PORTAL);
        pay(receptionist, sale.invoiceId(), "{\"method\":\"CASH\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"))
            .andExpect(jsonPath("$.activatedMemberships[0].status").value("ACTIVE"));
        assertThat(jdbc.queryForObject("SELECT source FROM invoices WHERE id=?", String.class, sale.invoiceId()))
            .isEqualTo("MEMBER_PORTAL");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payments WHERE invoice_id=?", Long.class, sale.invoiceId()))
            .isEqualTo(1);
    }

    private PackageInvoiceDto sellAtCounter(Long packageId) {
        return invoiceService.createPackageInvoice(receptionist, f.cn01, member, packageId, InvoiceSource.COUNTER);
    }

    private ResultActions pay(Long userId, Long invoiceId, String body) throws Exception {
        return mvc.perform(post("/api/v1/invoices/{id}/payments", invoiceId)
            .with(f.bearer(userId, UserRole.RECEPTIONIST))
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void assertUnpaid(PackageInvoiceDto sale) {
        Map<String, Object> invoice = jdbc.queryForMap("SELECT status, paid_at, version FROM invoices WHERE id = ?",
            sale.invoiceId());
        assertThat(invoice).containsEntry("status", "PENDING").containsEntry("version", 0);
        assertThat(invoice.get("paid_at")).isNull();
        assertThat(f.count("payments")).isZero();
        Map<String, Object> m = jdbc.queryForMap("SELECT status, start_date FROM memberships WHERE id = ?",
            sale.membershipId());
        assertThat(m).containsEntry("status", "PENDING");
        assertThat(m.get("start_date")).isEqualTo(java.sql.Date.valueOf(sale.startDate()));
    }
}
