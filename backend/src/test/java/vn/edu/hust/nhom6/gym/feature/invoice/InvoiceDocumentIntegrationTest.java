package vn.edu.hust.nhom6.gym.feature.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PackageInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.service.InvoiceService;

/**
 * {@code GET /api/v1/invoices/{id}/vietqr} và {@code GET /api/v1/invoices/{id}/pdf}: sinh mã VietQR động và xuất
 * hóa đơn PDF (UC-F5.1, UC-F5.4, BR-F5-01, BR-F5-02, BR-F5-09, BR-F5-10, G2, S2-09.2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class InvoiceDocumentIntegrationTest {
    private static final String NOT_FOUND_DETAIL = "Hóa đơn không tồn tại trên hệ thống.";
    private static final String NOT_PENDING_DETAIL = "Hóa đơn này không ở trạng thái chờ thanh toán.";
    private static final String MISSING_BANK_DETAIL = "Chi nhánh chưa cấu hình tài khoản nhận chuyển khoản";
    private static final String PDF_STATUS_DETAIL =
        "Chỉ có thể xuất hóa đơn cho các giao dịch đã thanh toán hoặc đã hoàn tiền.";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired VnTime vnTime;
    @Autowired InvoiceService invoiceService;

    SaleFixtures f;
    Long receptionistCn01;
    Long managerCn01;
    Long receptionistCn02;
    Long trainerCn01;
    Long owner;
    Long memberAnUser;
    Long memberAn;
    Long memberBinhUser;
    Long memberBinh;
    Long memberCn02;
    Long pkgTimeCn01;
    Long pkgTimeCn02;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 30));
        f = new SaleFixtures(jdbc, tokens);
        f.reset();
        jdbc.update("DELETE FROM system_configs WHERE config_key LIKE 'BANK_%'");
        jdbc.execute("TRUNCATE TABLE products RESTART IDENTITY CASCADE");

        receptionistCn01 = f.staff("le-tan-cn01@example.com", UserRole.RECEPTIONIST, f.cn01);
        jdbc.update("UPDATE staff SET full_name = 'Nguyễn Văn Lễ Tân' WHERE user_id = ?", receptionistCn01);
        managerCn01 = f.staff("quan-ly-cn01@example.com", UserRole.BRANCH_MANAGER, f.cn01);
        receptionistCn02 = f.staff("le-tan-cn02@example.com", UserRole.RECEPTIONIST, f.cn02);
        trainerCn01 = f.staff("hlv-cn01@example.com", UserRole.TRAINER, f.cn01);
        owner = f.staff("chu-chuoi@example.com", UserRole.SUPER_ADMIN, null);

        memberAnUser = insertUser("an@example.com", UserRole.MEMBER);
        memberAn = insertMember(memberAnUser, "HV000101", "Nguyễn Văn An", "0912345678", f.cn01);

        memberBinhUser = insertUser("binh@example.com", UserRole.MEMBER);
        memberBinh = insertMember(memberBinhUser, "HV000102", "Trần Thị Bình", "0987654321", f.cn01);

        memberCn02 = insertMember(null, "HV000201", "Lê Văn Cường", "0901112223", f.cn02);

        pkgTimeCn01 = insertPackage("TIME_CN01", "Gói Tập Tháng 1 – Đầy Đủ Tiếng Việt", 3_000_000, f.cn01);
        pkgTimeCn02 = insertPackage("TIME_CN02", "Gói Tập CN02", 600_000, f.cn02);
    }

    @Test
    @DisplayName("TC-F5.1.1-01a: VietQR hóa đơn PENDING lấy total từ server, nội dung {branch_code} {invoice_code} và URL-encode đúng")
    void vietQrForPendingInvoiceUsesServerTotalAndEncodesParams() throws Exception {
        insertBankConfig(null, "MB", "0987654321", "CONG TY GYM NHOM 6");
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);

        String expectedTransfer = "CN01 " + sale.invoiceCode();
        String expectedUrl = "https://img.vietqr.io/image/MB-0987654321-compact.png"
            + "?amount=3000000"
            + "&addInfo=CN01%20" + sale.invoiceCode()
            + "&accountName=CONG%20TY%20GYM%20NHOM%206";

        mvc.perform(get("/api/v1/invoices/{id}/vietqr", sale.invoiceId())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invoiceId").value(sale.invoiceId()))
            .andExpect(jsonPath("$.invoiceCode").value(sale.invoiceCode()))
            .andExpect(jsonPath("$.total").value(3_000_000))
            .andExpect(jsonPath("$.bankId").value("MB"))
            .andExpect(jsonPath("$.bankName").value("MBBank (NHTM CP Quân Đội)"))
            .andExpect(jsonPath("$.accountNo").value("0987654321"))
            .andExpect(jsonPath("$.accountName").value("CONG TY GYM NHOM 6"))
            .andExpect(jsonPath("$.transferContent").value(expectedTransfer))
            .andExpect(jsonPath("$.qrImageUrl").value(expectedUrl));

        // Quản lý chi nhánh cũng gọi được và ký tự đặc biệt trong accountName được URL-encode
        jdbc.update("UPDATE system_configs SET config_value = 'GYM A & B' WHERE branch_id IS NULL AND config_key = 'BANK_ACCOUNT_NAME'");
        mvc.perform(get("/api/v1/invoices/{id}/vietqr", sale.invoiceId())
                .with(f.bearer(managerCn01, UserRole.BRANCH_MANAGER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.qrImageUrl").value(
                "https://img.vietqr.io/image/MB-0987654321-compact.png"
                    + "?amount=3000000&addInfo=CN01%20" + sale.invoiceCode()
                    + "&accountName=GYM%20A%20%26%20B"));
    }

    @Test
    @DisplayName("TC-F5.1.1-02a: gọi VietQR cho hóa đơn PAID, CANCELLED hoặc REFUNDED trả 422")
    void vietQrForNonPendingInvoiceIs422() throws Exception {
        insertBankConfig(null, "MB", "0987654321", "CONG TY GYM NHOM 6");

        PackageInvoiceDto paid = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);
        payInvoice(paid.invoiceId(), "CASH", null);

        mvc.perform(get("/api/v1/invoices/{id}/vietqr", paid.invoiceId())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(NOT_PENDING_DETAIL));

        PackageInvoiceDto cancelled = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberBinh, pkgTimeCn01, InvoiceSource.COUNTER);
        jdbc.update("UPDATE invoices SET status = 'CANCELLED' WHERE id = ?", cancelled.invoiceId());

        mvc.perform(get("/api/v1/invoices/{id}/vietqr", cancelled.invoiceId())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(NOT_PENDING_DETAIL));

        jdbc.update("UPDATE invoices SET status = 'REFUNDED' WHERE id = ?", paid.invoiceId());
        mvc.perform(get("/api/v1/invoices/{id}/vietqr", paid.invoiceId())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(NOT_PENDING_DETAIL));
    }

    @Test
    @DisplayName("TC-F5.1.1-06a: thiếu cấu hình BANK_* trả 422 đúng câu thông báo")
    void vietQrMissingBankConfigIs422() throws Exception {
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);

        // Chưa có cả 3 khóa
        mvc.perform(get("/api/v1/invoices/{id}/vietqr", sale.invoiceId())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(MISSING_BANK_DETAIL));

        // Thiếu 1 trong 3 khóa (BANK_ACCOUNT_NAME)
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (NULL, 'BANK_ID', 'MB')");
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (NULL, 'BANK_ACCOUNT_NO', '0987654321')");

        mvc.perform(get("/api/v1/invoices/{id}/vietqr", sale.invoiceId())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(MISSING_BANK_DETAIL));
    }

    @Test
    @DisplayName("TC-F5.1.1-03a: cấu hình BANK_* của chi nhánh ghi đè giá trị toàn chuỗi")
    void vietQrBranchBankConfigOverridesChainConfig() throws Exception {
        insertBankConfig(null, "MB", "0000000001", "GYM TOAN CHUOI");
        insertBankConfig(f.cn02, "VCB", "9999999999", "GYM CHI NHANH 2");

        PackageInvoiceDto cn01Inv = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);
        PackageInvoiceDto cn02Inv = invoiceService.createPackageInvoice(
            receptionistCn02, f.cn02, memberCn02, pkgTimeCn02, InvoiceSource.COUNTER);

        // CN01 không có dòng riêng -> dùng toàn chuỗi
        mvc.perform(get("/api/v1/invoices/{id}/vietqr", cn01Inv.invoiceId())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bankId").value("MB"))
            .andExpect(jsonPath("$.accountNo").value("0000000001"))
            .andExpect(jsonPath("$.accountName").value("GYM TOAN CHUOI"))
            .andExpect(jsonPath("$.transferContent").value("CN01 " + cn01Inv.invoiceCode()));

        // CN02 có dòng riêng -> ghi đè toàn chuỗi
        mvc.perform(get("/api/v1/invoices/{id}/vietqr", cn02Inv.invoiceId())
                .with(f.bearer(receptionistCn02, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bankId").value("VCB"))
            .andExpect(jsonPath("$.accountNo").value("9999999999"))
            .andExpect(jsonPath("$.accountName").value("GYM CHI NHANH 2"))
            .andExpect(jsonPath("$.transferContent").value("CN02 " + cn02Inv.invoiceCode()))
            .andExpect(jsonPath("$.qrImageUrl").value(
                "https://img.vietqr.io/image/VCB-9999999999-compact.png"
                    + "?amount=600000&addInfo=CN02%20" + cn02Inv.invoiceCode()
                    + "&accountName=GYM%20CHI%20NHANH%202"));
    }

    @Test
    @DisplayName("TC-F5.1.1-03b: Mỗi khóa ngân hàng thiếu tại chi nhánh lấy riêng giá trị toàn chuỗi")
    void vietQrFallsBackForEachMissingBranchKey() throws Exception {
        insertBankConfig(null, "MB", "0000000001", "GYM TOAN CHUOI");
        jdbc.update("INSERT INTO system_configs(branch_id,config_key,config_value) VALUES (?,'BANK_ACCOUNT_NO','1234567890')", f.cn01);
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);
        mvc.perform(get("/api/v1/invoices/{id}/vietqr", sale.invoiceId())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bankId").value("MB"))
            .andExpect(jsonPath("$.accountNo").value("1234567890"))
            .andExpect(jsonPath("$.accountName").value("GYM TOAN CHUOI"));
    }

    @Test
    @DisplayName("TC-F5.1.1-06b: Cấu hình ngân hàng rỗng tại chi nhánh trả 422")
    void blankBranchBankConfigIs422() throws Exception {
        insertBankConfig(null, "MB", "0000000001", "GYM TOAN CHUOI");
        jdbc.update("INSERT INTO system_configs(branch_id,config_key,config_value) VALUES (?,'BANK_ACCOUNT_NAME','   ')", f.cn01);
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);
        mvc.perform(get("/api/v1/invoices/{id}/vietqr", sale.invoiceId())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(MISSING_BANK_DETAIL));
    }

    @Test
    @DisplayName("TC-F5.1.1-04a, TC-F5.1.1-05a: phân quyền vai trò, chưa đăng nhập và hóa đơn chi nhánh khác ở API VietQR")
    void vietQrSecurityAndScope() throws Exception {
        insertBankConfig(null, "MB", "0987654321", "CONG TY GYM NHOM 6");
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);

        // E1: chưa đăng nhập -> 401
        mvc.perform(get("/api/v1/invoices/{id}/vietqr", sale.invoiceId()))
            .andExpect(status().isUnauthorized());

        // E2: TRAINER, MEMBER, SUPER_ADMIN -> 403
        mvc.perform(get("/api/v1/invoices/{id}/vietqr", sale.invoiceId())
                .with(f.bearer(trainerCn01, UserRole.TRAINER)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/invoices/{id}/vietqr", sale.invoiceId())
                .with(f.bearer(memberAnUser, UserRole.MEMBER)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/invoices/{id}/vietqr", sale.invoiceId())
                .with(f.bearer(owner, UserRole.SUPER_ADMIN)))
            .andExpect(status().isForbidden());

        // E3: hóa đơn chi nhánh khác hoặc không tồn tại -> 404
        mvc.perform(get("/api/v1/invoices/{id}/vietqr", sale.invoiceId())
                .with(f.bearer(receptionistCn02, UserRole.RECEPTIONIST)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(NOT_FOUND_DETAIL));

        mvc.perform(get("/api/v1/invoices/{id}/vietqr", 999_999L)
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(NOT_FOUND_DETAIL));
    }

    @Test
    @DisplayName("TC-F5.4.1-01a: xuất PDF hóa đơn PAID và REFUNDED đúng header, đọc lại bằng PdfReader có mã hóa đơn và tiếng Việt có dấu")
    void pdfForPaidAndRefundedInvoiceIsReadableWithVietnameseText() throws Exception {
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);
        payInvoice(sale.invoiceId(), "BANK_TRANSFER", "FT261120999");

        byte[] paidPdf = mvc.perform(get("/api/v1/invoices/{id}/pdf", sale.invoiceId())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"hoa-don-" + sale.invoiceCode() + ".pdf\""))
            .andReturn().getResponse().getContentAsByteArray();

        String paidText = extractPdfText(paidPdf);
        assertThat(paidText)
            .contains(sale.invoiceCode())
            .contains("Chi nhánh Bách Khoa")
            .contains("Nguyễn Văn An")
            .contains("HV000101")
            .contains("Gói Tập Tháng 1 – Đầy Đủ Tiếng Việt")
            .contains("3.000.000")
            .contains("FT261120999")
            .contains("Nguyễn Văn Lễ Tân");

        // REFUNDED cũng xuất được PDF
        jdbc.update("""
            UPDATE invoices
            SET status = 'REFUNDED', refunded_by = ?, refunded_at = ?, refund_reason = 'Lễ tân bán nhầm gói'
            WHERE id = ?""", managerCn01, vnTime.now(), sale.invoiceId());

        byte[] refundedPdf = mvc.perform(get("/api/v1/invoices/{id}/pdf", sale.invoiceId())
                .with(f.bearer(managerCn01, UserRole.BRANCH_MANAGER)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"hoa-don-" + sale.invoiceCode() + ".pdf\""))
            .andReturn().getResponse().getContentAsByteArray();

        String refundedText = extractPdfText(refundedPdf);
        assertThat(refundedText)
            .contains(sale.invoiceCode())
            .contains("Đã hoàn tiền")
            .contains("Lễ tân bán nhầm gói");
    }

    @Test
    @DisplayName("TC-F5.4.1-02a: PDF lấy mô tả và đơn giá từ invoice_items, không đổi khi sửa giá gói")
    void pdfUsesSnapshotPriceAndDescriptionFromInvoiceItems() throws Exception {
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);
        payInvoice(sale.invoiceId(), "CASH", null);

        // Sửa giá và tên gói trong danh mục sau khi đã bán
        jdbc.update("UPDATE packages SET price = 9999000, name = 'Gói Đã Đổi Tên Mới' WHERE id = ?", pkgTimeCn01);

        byte[] pdfBytes = mvc.perform(get("/api/v1/invoices/{id}/pdf", sale.invoiceId())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn().getResponse().getContentAsByteArray();

        String pdfText = extractPdfText(pdfBytes);
        assertThat(pdfText)
            .contains(sale.invoiceCode())
            .contains("Gói Tập Tháng 1 – Đầy Đủ Tiếng Việt")
            .contains("3.000.000")
            .doesNotContain("9.999.000")
            .doesNotContain("Gói Đã Đổi Tên Mới");
    }

    @Test
    @DisplayName("TC-F5.4.1-03a: xuất PDF hóa đơn PENDING hoặc CANCELLED trả 422")
    void pdfForPendingOrCancelledInvoiceIs422() throws Exception {
        PackageInvoiceDto pending = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);

        mvc.perform(get("/api/v1/invoices/{id}/pdf", pending.invoiceId())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(PDF_STATUS_DETAIL));

        jdbc.update("UPDATE invoices SET status = 'CANCELLED' WHERE id = ?", pending.invoiceId());

        mvc.perform(get("/api/v1/invoices/{id}/pdf", pending.invoiceId())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(PDF_STATUS_DETAIL));
    }

    @Test
    @DisplayName("TC-F5.4.1-04a, TC-F5.4.1-05a: hội viên tải hóa đơn của mình 200, của người khác 404; chi nhánh khác 404; TRAINER và SUPER_ADMIN 403; chưa đăng nhập 401")
    void pdfSecurityOwnershipAndScope() throws Exception {
        PackageInvoiceDto anInvoice = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);
        payInvoice(anInvoice.invoiceId(), "CASH", null);

        // Hội viên tải hóa đơn của mình -> 200
        byte[] ownPdf = mvc.perform(get("/api/v1/invoices/{id}/pdf", anInvoice.invoiceId())
                .with(f.bearer(memberAnUser, UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn().getResponse().getContentAsByteArray();
        assertThat(extractPdfText(ownPdf)).contains(anInvoice.invoiceCode());

        // Hội viên khác tải hóa đơn của An -> 404
        mvc.perform(get("/api/v1/invoices/{id}/pdf", anInvoice.invoiceId())
                .with(f.bearer(memberBinhUser, UserRole.MEMBER)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(NOT_FOUND_DETAIL));

        // Lễ tân chi nhánh khác -> 404
        mvc.perform(get("/api/v1/invoices/{id}/pdf", anInvoice.invoiceId())
                .with(f.bearer(receptionistCn02, UserRole.RECEPTIONIST)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(NOT_FOUND_DETAIL));

        // Hóa đơn không tồn tại -> 404
        mvc.perform(get("/api/v1/invoices/{id}/pdf", 999_999L)
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(NOT_FOUND_DETAIL));

        // TRAINER, SUPER_ADMIN -> 403
        mvc.perform(get("/api/v1/invoices/{id}/pdf", anInvoice.invoiceId())
                .with(f.bearer(trainerCn01, UserRole.TRAINER)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/invoices/{id}/pdf", anInvoice.invoiceId())
                .with(f.bearer(owner, UserRole.SUPER_ADMIN)))
            .andExpect(status().isForbidden());

        // Chưa đăng nhập -> 401
        mvc.perform(get("/api/v1/invoices/{id}/pdf", anInvoice.invoiceId()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-F5.4.1-06a, TC-F5.4.1-07a, TC-F5.1.1-07a: hóa đơn khách lẻ PAID xuất PDF thành công và id không hợp lệ trả 400")
    void walkInPaidInvoicePdfAndInvalidId() throws Exception {
        Long productId = jdbc.queryForObject("""
            INSERT INTO products (sku, name, price, unit, status)
            VALUES ('WATER-01', 'Nước khoáng 500ml', 15000, 'chai', 'ACTIVE') RETURNING id""", Long.class);
        OffsetDateTime now = vnTime.now();
        Long walkInId = jdbc.queryForObject("""
            INSERT INTO invoices (code, branch_id, member_id, created_by, source, subtotal, discount, total,
                                  status, paid_at, created_at, updated_at)
            VALUES ('HD990088', ?, NULL, ?, 'COUNTER', 30000, 0, 30000, 'PAID', ?, ?, ?) RETURNING id""",
            Long.class, f.cn01, receptionistCn01, now, now, now);
        jdbc.update("""
            INSERT INTO invoice_items (invoice_id, item_type, product_id, description, quantity, unit_price, amount,
                                       discount_amount, created_at, updated_at)
            VALUES (?, 'PRODUCT', ?, 'Nước khoáng 500ml', 2, 15000, 30000, 0, ?, ?)""",
            walkInId, productId, now, now);
        jdbc.update("""
            INSERT INTO payments (invoice_id, method, amount, status, received_by, paid_at, created_at, updated_at)
            VALUES (?, 'CASH', 30000, 'CONFIRMED', ?, ?, ?, ?)""",
            walkInId, receptionistCn01, now, now, now);

        byte[] pdfBytes = mvc.perform(get("/api/v1/invoices/{id}/pdf", walkInId)
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn().getResponse().getContentAsByteArray();

        String text = extractPdfText(pdfBytes);
        assertThat(text)
            .contains("HD990088")
            .contains("Khách lẻ")
            .contains("Nước khoáng 500ml")
            .contains("30.000");

        // Hội viên không tải được hóa đơn khách lẻ -> 404
        mvc.perform(get("/api/v1/invoices/{id}/pdf", walkInId)
                .with(f.bearer(memberAnUser, UserRole.MEMBER)))
            .andExpect(status().isNotFound());

        // id <= 0 -> 400
        mvc.perform(get("/api/v1/invoices/0/vietqr")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.id").isString());
        mvc.perform(get("/api/v1/invoices/0/pdf")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.id").isString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "9223372036854775808"})
    @DisplayName("TC-F5.1.1-07b, TC-F5.4.1-07b: ID VietQR/PDF không phải số nguyên dương hợp lệ trả 400")
    void invalidDocumentIdsReturnFieldErrors(String id) throws Exception {
        for (String document : new String[]{"vietqr", "pdf"}) {
            mvc.perform(get("/api/v1/invoices/{id}/{document}", id, document)
                    .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.id").isString());
        }
    }

    private String extractPdfText(byte[] pdfBytes) throws Exception {
        try (PdfReader reader = new PdfReader(pdfBytes)) {
            assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder sb = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                sb.append(extractor.getTextFromPage(page)).append('\n');
            }
            return sb.toString();
        }
    }

    private void insertBankConfig(Long branchId, String bankId, String accountNo, String accountName) {
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (?, 'BANK_ID', ?)",
            branchId, bankId);
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (?, 'BANK_ACCOUNT_NO', ?)",
            branchId, accountNo);
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (?, 'BANK_ACCOUNT_NAME', ?)",
            branchId, accountName);
    }

    private Long insertUser(String email, UserRole role) {
        return jdbc.queryForObject(
            "INSERT INTO users (email, password_hash, role) VALUES (?, 'test-only', ?) RETURNING id",
            Long.class, email, role.name());
    }

    private Long insertMember(Long userId, String code, String fullName, String phone, Long branchId) {
        return jdbc.queryForObject("""
            INSERT INTO members (user_id, home_branch_id, member_code, full_name, phone, status)
            VALUES (?, ?, ?, ?, ?, 'ACTIVE') RETURNING id""",
            Long.class, userId, branchId, code, fullName, phone);
    }

    private Long insertPackage(String code, String name, int price, Long branchId) {
        return jdbc.queryForObject("""
            INSERT INTO packages (branch_id, code, name, type, duration_days, session_count, price, status)
            VALUES (?, ?, ?, 'TIME', 30, NULL, ?, 'ACTIVE') RETURNING id""",
            Long.class, branchId, code, name, price);
    }

    private void payInvoice(Long invoiceId, String method, String transactionRef) throws Exception {
        String body = transactionRef == null
            ? "{\"method\": \"" + method + "\"}"
            : "{\"method\": \"" + method + "\", \"transactionRef\": \"" + transactionRef + "\"}";
        mvc.perform(post("/api/v1/invoices/{id}/payments", invoiceId)
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());
    }
}
