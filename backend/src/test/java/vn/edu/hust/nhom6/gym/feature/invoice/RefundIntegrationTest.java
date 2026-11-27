package vn.edu.hust.nhom6.gym.feature.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

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
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PackageInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PayInvoiceRequest;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.PaymentMethod;
import vn.edu.hust.nhom6.gym.feature.invoice.service.InvoiceService;

/**
 * {@code POST /api/v1/invoices/{id}/refund}: quản lý chi nhánh hoàn tiền hóa đơn bán nhầm (UC-F5.5, BR-F5-11…14,
 * S2-38.1).
 *
 * <p>Đồng hồ cố định 20/11/2026 09:00 giờ Việt Nam. Hóa đơn PAID dựng bằng đúng luồng bán, thu tiền của F3, F5;
 * lượt check-in, buổi PT, chỗ đặt lớp dựng bằng SQL (bảng của F4, F8, F9).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class RefundIntegrationTest {
    static final String REASON = "Lễ tân bán nhầm gói TIME 3 tháng, hội viên mua gói 1 tháng";
    static final String FORBIDDEN = "Chỉ quản lý chi nhánh mới có quyền hoàn tiền hóa đơn.";
    static final String NOT_FOUND = "Hóa đơn không tồn tại trên hệ thống.";
    static final String NOT_PAID = "Chỉ có thể hoàn tiền cho hóa đơn đã thanh toán thành công.";
    static final String USED = "Gói tập của hóa đơn đã được sử dụng nên không thể hoàn tiền. "
        + "Hội viên có thể bảo lưu hoặc chuyển nhượng gói.";
    static final String CLASS_BOOKED = "Hội viên còn chỗ đặt lớp nhóm chưa hủy. Hủy chỗ đặt trước khi hoàn tiền.";
    static final LocalDateTime NOW_VN = LocalDateTime.of(2026, 11, 20, 9, 0);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired InvoiceService invoiceService;

    BookingFixtures b;
    Long manager;
    Long member;
    Long timePkg;
    Long ptPkg;

    @BeforeEach
    void setUp() {
        clock.setVn(NOW_VN);
        b = new BookingFixtures(jdbc, tokens);
        b.reset();
        manager = b.f.user("quan-ly@example.com", UserRole.BRANCH_MANAGER, b.cn01);
        member = b.account(b.cn01).memberId();
        timePkg = b.f.sale.pkg("TIME_30", "TIME", null, 500_000, b.cn01, "ACTIVE");
        ptPkg = b.f.sale.pkg("PT_10", "PT", 10, 3_000_000, b.cn01, "ACTIVE");
    }

    // ---------------------------------------------------------------------------------------------
    // Thành công
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("TC-F5.5.1-01a: Quản lý hoàn hóa đơn PAID có gói chưa dùng: 200, hóa đơn REFUNDED, gói CANCELLED, phiếu thu CANCELLED, một dòng audit_logs REFUND")
    void refundSucceeds() throws Exception {
        PackageInvoiceDto sale = paid(timePkg);

        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), "  " + REASON + "  ")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invoiceId").value(sale.invoiceId()))
            .andExpect(jsonPath("$.invoiceCode").value(sale.invoiceCode()))
            .andExpect(jsonPath("$.status").value("REFUNDED"))
            .andExpect(jsonPath("$.refundedAt").value("2026-11-20T02:00:00Z"))
            .andExpect(jsonPath("$.refundedBy").value(manager))
            .andExpect(jsonPath("$.refundedByName").value("Nhân viên test"))
            .andExpect(jsonPath("$.refundReason").value(REASON));

        assertThat(jdbc.queryForMap("SELECT status, refunded_by, refund_reason FROM invoices WHERE id = ?",
            sale.invoiceId()))
            .containsEntry("status", "REFUNDED").containsEntry("refunded_by", manager)
            .containsEntry("refund_reason", REASON);
        assertThat(jdbc.queryForObject("SELECT refunded_at FROM invoices WHERE id = ?", OffsetDateTime.class,
            sale.invoiceId())).isEqualTo(OffsetDateTime.parse("2026-11-20T02:00:00Z"));
        assertThat(membershipStatus(sale)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT status FROM payments WHERE invoice_id = ?", String.class,
            sale.invoiceId())).isEqualTo("CANCELLED");

        assertThat(jdbc.queryForMap("""
            SELECT user_id, branch_id, action, entity, entity_id, old_value ->> 'status' AS old_status,
                   new_value ->> 'status' AS new_status, (new_value ->> 'total')::int AS total,
                   new_value ->> 'refundReason' AS reason, ip
            FROM audit_logs WHERE action = 'REFUND'"""))
            .containsEntry("user_id", manager).containsEntry("branch_id", b.cn01)
            .containsEntry("entity", "invoices").containsEntry("entity_id", sale.invoiceId())
            .containsEntry("old_status", "PAID").containsEntry("new_status", "REFUNDED")
            .containsEntry("total", 500_000).containsEntry("reason", REASON)
            .containsEntry("ip", "127.0.0.1");
        assertThat(refundAudits()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-F5.5.1-01b: Gói UPCOMING chưa dùng (bán gia hạn nhầm) cũng hoàn được, gói đang dùng không đổi")
    void refundUpcomingRenewal() throws Exception {
        PackageInvoiceDto current = paid(timePkg);
        PackageInvoiceDto renewal = paid(timePkg);
        assertThat(membershipStatus(renewal)).isEqualTo("UPCOMING");

        refund(manager, UserRole.BRANCH_MANAGER, renewal.invoiceId(), REASON).andExpect(status().isOk());

        assertThat(membershipStatus(renewal)).isEqualTo("CANCELLED");
        assertThat(membershipStatus(current)).isEqualTo("ACTIVE");
        assertThat(invoiceStatus(current)).isEqualTo("PAID");
    }

    @Test
    @DisplayName("TC-F5.5.1-02a: Hoàn hóa đơn gói A đang ACTIVE thì gói B UPCOMING của hóa đơn khác giữ nguyên trạng thái và ngày bắt đầu (BR-F5-13, Q5 = A)")
    void refundKeepsFollowingUpcomingUntouched() throws Exception {
        PackageInvoiceDto a = paid(timePkg);
        PackageInvoiceDto bSale = paid(timePkg);
        LocalDate bStart = startDate(bSale);
        LocalDate bEnd = jdbc.queryForObject("SELECT end_date FROM memberships WHERE id = ?", LocalDate.class,
            bSale.membershipId());

        refund(manager, UserRole.BRANCH_MANAGER, a.invoiceId(), REASON).andExpect(status().isOk());

        assertThat(membershipStatus(a)).isEqualTo("CANCELLED");
        assertThat(membershipStatus(bSale)).isEqualTo("UPCOMING");
        assertThat(startDate(bSale)).isEqualTo(bStart).isEqualTo(LocalDate.of(2026, 12, 20));
        assertThat(jdbc.queryForObject("SELECT end_date FROM memberships WHERE id = ?", LocalDate.class,
            bSale.membershipId())).isEqualTo(bEnd);
        assertThat(invoiceStatus(bSale)).isEqualTo("PAID");
    }

    // ---------------------------------------------------------------------------------------------
    // Quyền, dữ liệu vào, phạm vi
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("TC-F5.5.1-03a: Lễ tân hoàn tiền thì 403 đúng câu SRS; hóa đơn vẫn PAID, không ghi audit_logs")
    void receptionistIsForbidden() throws Exception {
        PackageInvoiceDto sale = paid(timePkg);

        refund(b.receptionist, UserRole.RECEPTIONIST, sale.invoiceId(), REASON)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value(FORBIDDEN));
        assertUnchanged(sale, "ACTIVE");
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"TRAINER", "MEMBER", "SUPER_ADMIN"})
    @DisplayName("TC-F5.5.1-03b: Huấn luyện viên, hội viên, chủ chuỗi hoàn tiền thì 403 đúng câu SRS (BR-F5-11)")
    void otherRolesAreForbidden(UserRole role) throws Exception {
        Long user = b.f.user("khac-" + role.name().toLowerCase() + "@example.com", role,
            role == UserRole.TRAINER ? b.cn01 : null);
        PackageInvoiceDto sale = paid(timePkg);

        refund(user, role, sale.invoiceId(), REASON)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value(FORBIDDEN));
        assertUnchanged(sale, "ACTIVE");
    }

    @Test
    @DisplayName("TC-F5.5.1-03c: Lễ tân không gửi lý do vẫn nhận 403, không phải 400 (xét quyền trước dữ liệu vào)")
    void forbiddenComesBeforeValidation() throws Exception {
        PackageInvoiceDto sale = paid(timePkg);
        mvc.perform(post("/api/v1/invoices/{id}/refund", sale.invoiceId())
                .with(b.bearer(b.receptionist, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value(FORBIDDEN));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"refundReason\": null}", "{\"refundReason\": \"   \"}"})
    @DisplayName("TC-F5.5.1-04a: Thiếu hoặc trắng lý do hoàn tiền thì 400 lỗi trường refundReason; hóa đơn vẫn PAID")
    void missingReasonIs400(String body) throws Exception {
        PackageInvoiceDto sale = paid(timePkg);
        mvc.perform(post("/api/v1/invoices/{id}/refund", sale.invoiceId())
                .with(b.bearer(manager, UserRole.BRANCH_MANAGER))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.refundReason").value("Vui lòng nhập lý do hoàn tiền."));
        assertUnchanged(sale, "ACTIVE");
    }

    @Test
    @DisplayName("TC-F5.5.1-04b: Không gửi body hoặc lý do dài quá 500 ký tự thì 400")
    void noBodyOrTooLongReasonIs400() throws Exception {
        PackageInvoiceDto sale = paid(timePkg);
        mvc.perform(post("/api/v1/invoices/{id}/refund", sale.invoiceId())
                .with(b.bearer(manager, UserRole.BRANCH_MANAGER)))
            .andExpect(status().isBadRequest());
        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), "x".repeat(501))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.refundReason").value("Lý do hoàn tiền tối đa 500 ký tự."));
        assertUnchanged(sale, "ACTIVE");
    }

    @Test
    @DisplayName("TC-F5.5.1-04c: Lý do 500 ký tự kèm khoảng trắng hai đầu vẫn hợp lệ; lưu bản đã cắt khoảng trắng")
    void reasonLengthCountedAfterStrip() throws Exception {
        PackageInvoiceDto sale = paid(timePkg);
        String reason = "x".repeat(500);
        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), "   " + reason + "   ")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refundReason").value(reason));
    }

    @Test
    @DisplayName("TC-F5.5.1-05a: Không có token thì 401")
    void anonymousIs401() throws Exception {
        PackageInvoiceDto sale = paid(timePkg);
        mvc.perform(post("/api/v1/invoices/{id}/refund", sale.invoiceId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"refundReason\": \"x\"}"))
            .andExpect(status().isUnauthorized());
        assertUnchanged(sale, "ACTIVE");
    }

    @Test
    @DisplayName("TC-F5.5.1-05b: Hóa đơn không tồn tại thì 404 đúng câu SRS")
    void unknownInvoiceIs404() throws Exception {
        refund(manager, UserRole.BRANCH_MANAGER, 999_999L, REASON)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(NOT_FOUND));
    }

    @Test
    @DisplayName("TC-F5.5.1-05c: Quản lý CN02 hoàn hóa đơn CN01 thì 404 như không tồn tại (VD-16); hóa đơn vẫn PAID")
    void otherBranchManagerIs404() throws Exception {
        Long managerCn02 = b.f.user("quan-ly-cn02@example.com", UserRole.BRANCH_MANAGER, b.cn02);
        PackageInvoiceDto sale = paid(timePkg);

        refund(managerCn02, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(NOT_FOUND));
        assertUnchanged(sale, "ACTIVE");
    }

    // ---------------------------------------------------------------------------------------------
    // E6: hóa đơn không PAID
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("TC-F5.5.1-07a: Hóa đơn PENDING thì 422; hóa đơn và gói PENDING giữ nguyên")
    void pendingInvoiceIs422() throws Exception {
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(b.receptionist, b.cn01, member, timePkg,
            InvoiceSource.COUNTER);
        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(NOT_PAID));
        assertThat(invoiceStatus(sale)).isEqualTo("PENDING");
        assertThat(membershipStatus(sale)).isEqualTo("PENDING");
        assertThat(refundAudits()).isZero();
    }

    @Test
    @DisplayName("TC-F5.5.1-07b: Hóa đơn đã CANCELLED thì 422")
    void cancelledInvoiceIs422() throws Exception {
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(b.receptionist, b.cn01, member, timePkg,
            InvoiceSource.COUNTER);
        invoiceService.cancel(new CurrentUser(b.receptionist, UserRole.RECEPTIONIST), sale.invoiceId());
        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(NOT_PAID));
        assertThat(invoiceStatus(sale)).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("TC-F5.5.1-07c: Hoàn lần hai hóa đơn đã REFUNDED thì 422; chỉ một dòng audit_logs")
    void refundTwiceIs422() throws Exception {
        PackageInvoiceDto sale = paid(timePkg);
        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON).andExpect(status().isOk());
        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(NOT_PAID));
        assertThat(refundAudits()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------------------------
    // E7 / BR-F5-14: gói đã dùng thì không hoàn
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("TC-F5.5.1-08a: Gói của hóa đơn không còn ACTIVE/UPCOMING (đang FROZEN) thì 422 (BR-F5-14 điều kiện 1)")
    void frozenMembershipIs422() throws Exception {
        PackageInvoiceDto sale = paid(timePkg);
        jdbc.update("UPDATE memberships SET status = 'FROZEN' WHERE id = ?", sale.membershipId());

        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(USED));
        assertUnchanged(sale, "FROZEN");
    }

    @Test
    @DisplayName("TC-F5.5.1-09a: Gói TIME đã có một lượt check-in thì 422; hóa đơn vẫn PAID, gói vẫn ACTIVE, phiếu thu vẫn CONFIRMED, không ghi audit_logs (BR-F5-14 điều kiện 2)")
    void checkedInMembershipIs422() throws Exception {
        PackageInvoiceDto sale = paid(timePkg);
        checkin(sale.membershipId(), false);

        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(USED));
        assertUnchanged(sale, "ACTIVE");
    }

    @Test
    @DisplayName("TC-F5.5.1-09b: Gói PT đã dùng để check-in (ngày có buổi PT, không trừ buổi) thì 422 (BR-F5-14 điều kiện 2)")
    void ptMembershipUsedForCheckinIs422() throws Exception {
        PackageInvoiceDto sale = paid(ptPkg);
        checkin(sale.membershipId(), false);

        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(USED));
        assertUnchanged(sale, "ACTIVE");
    }

    @Test
    @DisplayName("TC-F5.5.1-10a: Gói PT có một buổi PT BOOKED thì 422; buổi PT giữ nguyên BOOKED (BR-F5-14 điều kiện 3)")
    void bookedPtSessionIs422() throws Exception {
        PackageInvoiceDto sale = paid(ptPkg);
        Long session = ptSession(sale.membershipId(), "BOOKED");

        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(USED));
        assertUnchanged(sale, "ACTIVE");
        assertThat(jdbc.queryForObject("SELECT status FROM pt_sessions WHERE id = ?", String.class, session))
            .isEqualTo("BOOKED");
    }

    @Test
    @DisplayName("TC-F5.5.1-10b: Gói PT chỉ có buổi PT đã CANCELLED vẫn tính là đã dùng, 422 (BR-F5-14 điều kiện 3)")
    void cancelledPtSessionStillCounts() throws Exception {
        PackageInvoiceDto sale = paid(ptPkg);
        ptSession(sale.membershipId(), "CANCELLED");

        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(USED));
        assertUnchanged(sale, "ACTIVE");
    }

    @Test
    @DisplayName("TC-F5.5.1-11a: Gói TIME chưa check-in nhưng hội viên còn chỗ lớp BOOKED ngày mai thì 422 với câu riêng; chỗ đặt giữ nguyên (BR-F5-14 điều kiện 4)")
    void upcomingClassBookingIs422() throws Exception {
        PackageInvoiceDto sale = paid(timePkg);
        Long session = b.session(b.cn01, NOW_VN.plusDays(1).withHour(18), 20);
        Long booking = b.booking(session, member, "BOOKED");

        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(CLASS_BOOKED));
        assertUnchanged(sale, "ACTIVE");
        assertThat(b.status(booking)).isEqualTo("BOOKED");
    }

    @Test
    @DisplayName("TC-F5.5.1-11b: Chỗ lớp BOOKED của buổi đang diễn ra (08:30–09:30, lúc 09:00) cũng chặn hoàn tiền (nháp prototype, GĐ-1)")
    void inProgressClassBookingIs422() throws Exception {
        PackageInvoiceDto sale = paid(timePkg);
        b.booking(b.session(b.cn01, NOW_VN.minusMinutes(30), 20), member, "BOOKED");

        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(CLASS_BOOKED));
        assertUnchanged(sale, "ACTIVE");
    }

    @Test
    @DisplayName("TC-F5.5.1-11c: Chỗ lớp đã CANCELLED, hoặc BOOKED của buổi đã kết thúc, không chặn hoàn tiền (nháp prototype, GĐ-1)")
    void pastOrCancelledBookingDoesNotBlock() throws Exception {
        PackageInvoiceDto sale = paid(timePkg);
        b.booking(b.session(b.cn01, NOW_VN.plusDays(1), 20), member, "CANCELLED");
        b.booking(b.session(b.cn01, NOW_VN.minusHours(2), 20), member, "BOOKED");

        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON).andExpect(status().isOk());
        assertThat(membershipStatus(sale)).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("TC-F5.5.1-11d: Chỗ lớp BOOKED không chặn hoàn hóa đơn gói PT (điều kiện 4 chỉ xét gói TIME)")
    void classBookingDoesNotBlockPtRefund() throws Exception {
        PackageInvoiceDto sale = paid(ptPkg);
        b.booking(b.session(b.cn01, NOW_VN.plusDays(1), 20), member, "BOOKED");

        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON).andExpect(status().isOk());
        assertThat(membershipStatus(sale)).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("TC-F5.5.1-12a: Check-in, buổi PT của gói khác (không thuộc hóa đơn) không chặn hoàn tiền")
    void otherMembershipUsageDoesNotBlock() throws Exception {
        PackageInvoiceDto other = paid(ptPkg);
        checkin(other.membershipId(), false);
        ptSession(other.membershipId(), "BOOKED");
        PackageInvoiceDto sale = paid(timePkg);

        refund(manager, UserRole.BRANCH_MANAGER, sale.invoiceId(), REASON).andExpect(status().isOk());
        assertThat(membershipStatus(sale)).isEqualTo("CANCELLED");
        assertThat(membershipStatus(other)).isEqualTo("ACTIVE");
    }

    // ---------------------------------------------------------------------------------------------

    private PackageInvoiceDto paid(Long packageId) {
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(b.receptionist, b.cn01, member, packageId,
            InvoiceSource.COUNTER);
        invoiceService.pay(new CurrentUser(b.receptionist, UserRole.RECEPTIONIST), sale.invoiceId(),
            new PayInvoiceRequest(PaymentMethod.CASH, null));
        return sale;
    }

    private ResultActions refund(Long userId, UserRole role, Long invoiceId, String reason) throws Exception {
        String body = "{\"refundReason\": \"" + reason + "\"}";
        return mvc.perform(post("/api/v1/invoices/{id}/refund", invoiceId).with(b.bearer(userId, role))
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void checkin(Long membershipId, boolean deducted) {
        jdbc.update("""
            INSERT INTO checkins (member_id, membership_id, branch_id, created_by, checkin_date, session_deducted,
                                  checked_in_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)""", member, membershipId, b.cn01, b.receptionist, NOW_VN.toLocalDate(),
            deducted, BookingFixtures.utc(NOW_VN.minusHours(1)));
    }

    private Long ptSession(Long membershipId, String status) {
        Long trainer = b.f.trainer("pt-" + membershipId + "-" + status.toLowerCase() + "@example.com", b.cn01);
        OffsetDateTime start = BookingFixtures.utc(NOW_VN.plusDays(2));
        return jdbc.queryForObject("""
            INSERT INTO pt_sessions (trainer_id, member_id, membership_id, branch_id, start_at, end_at, status,
                                     cancelled_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class, trainer, member, membershipId, b.cn01,
            start, start.plusHours(1), status, "CANCELLED".equals(status) ? start.minusDays(1) : null);
    }

    private void assertUnchanged(PackageInvoiceDto sale, String membershipStatus) {
        assertThat(invoiceStatus(sale)).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("SELECT refunded_at FROM invoices WHERE id = ?", OffsetDateTime.class,
            sale.invoiceId())).isNull();
        assertThat(membershipStatus(sale)).isEqualTo(membershipStatus);
        assertThat(jdbc.queryForObject("SELECT status FROM payments WHERE invoice_id = ?", String.class,
            sale.invoiceId())).isEqualTo("CONFIRMED");
        assertThat(refundAudits()).isZero();
    }

    private String invoiceStatus(PackageInvoiceDto sale) {
        return jdbc.queryForObject("SELECT status FROM invoices WHERE id = ?", String.class, sale.invoiceId());
    }

    private String membershipStatus(PackageInvoiceDto sale) {
        return jdbc.queryForObject("SELECT status FROM memberships WHERE id = ?", String.class, sale.membershipId());
    }

    private LocalDate startDate(PackageInvoiceDto sale) {
        return jdbc.queryForObject("SELECT start_date FROM memberships WHERE id = ?", LocalDate.class,
            sale.membershipId());
    }

    private long refundAudits() {
        return jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE action = 'REFUND'", Long.class);
    }
}
