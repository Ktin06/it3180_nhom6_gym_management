package vn.edu.hust.nhom6.gym.feature.invoice;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

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
 * {@code GET /api/v1/invoices} và {@code GET /api/v1/invoices/{id}}: tra cứu danh sách và chi tiết hóa đơn
 * (UC-F5.3, BR-F5-06, S2-09.3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class InvoiceQueryIntegrationTest {
    static final String NOT_FOUND_DETAIL = "Hóa đơn không tồn tại trên hệ thống.";

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
    Long managerCn02;
    Long owner;
    Long memberAn;
    Long memberBinh;
    Long memberCn02;
    Long pkgTimeCn01;
    Long pkgSessionCn01;
    Long pkgPtCn01;
    Long pkgTimeCn02;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        f = new SaleFixtures(jdbc, tokens);
        f.reset();
        jdbc.execute("TRUNCATE TABLE products RESTART IDENTITY CASCADE");

        receptionistCn01 = f.staff("le-tan-cn01@example.com", UserRole.RECEPTIONIST, f.cn01);
        jdbc.update("UPDATE staff SET full_name = 'Nguyễn Văn Lễ Tân' WHERE user_id = ?", receptionistCn01);
        managerCn01 = f.staff("quan-ly-cn01@example.com", UserRole.BRANCH_MANAGER, f.cn01);
        receptionistCn02 = f.staff("le-tan-cn02@example.com", UserRole.RECEPTIONIST, f.cn02);
        managerCn02 = f.staff("quan-ly-cn02@example.com", UserRole.BRANCH_MANAGER, f.cn02);
        owner = f.staff("chu-chuoi@example.com", UserRole.SUPER_ADMIN, null);

        memberAn = createMember("HV000101", "Nguyễn Văn An", "0912345678", f.cn01);
        memberBinh = createMember("HV000102", "Trần Thị Bình", "0987654321", f.cn01);
        memberCn02 = createMember("HV000201", "Lê Văn Cường", "0901112223", f.cn02);

        pkgTimeCn01 = f.pkg("TIME_CN01", "TIME", null, 500_000, f.cn01, "ACTIVE");
        pkgSessionCn01 = f.pkg("SESS_CN01", "SESSION", 10, 800_000, f.cn01, "ACTIVE");
        pkgPtCn01 = f.pkg("PT_CN01", "PT", 10, 3_000_000, f.cn01, "ACTIVE");
        pkgTimeCn02 = f.pkg("TIME_CN02", "TIME", null, 600_000, f.cn02, "ACTIVE");
    }

    @Test
    @DisplayName("TC-F5.3.1-01a: lễ tân xem danh sách hóa đơn chi nhánh mình, mới nhất trước, phân trang PageResponse")
    void receptionistListsOwnBranchNewestFirstWithPaging() throws Exception {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 8, 0));
        PackageInvoiceDto first = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);

        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        PackageInvoiceDto second = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgSessionCn01, InvoiceSource.COUNTER);

        clock.setVn(LocalDateTime.of(2026, 11, 20, 10, 0));
        PackageInvoiceDto third = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberBinh, pkgPtCn01, InvoiceSource.COUNTER);

        // Hóa đơn CN02 không được xuất hiện với lễ tân CN01
        clock.setVn(LocalDateTime.of(2026, 11, 20, 11, 0));
        invoiceService.createPackageInvoice(receptionistCn02, f.cn02, memberCn02, pkgTimeCn02, InvoiceSource.COUNTER);

        mvc.perform(get("/api/v1/invoices")
                .param("page", "0")
                .param("size", "2")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].id").value(third.invoiceId()))
            .andExpect(jsonPath("$.content[0].code").value(third.invoiceCode()))
            .andExpect(jsonPath("$.content[0].branchId").value(f.cn01))
            .andExpect(jsonPath("$.content[0].memberId").value(memberBinh))
            .andExpect(jsonPath("$.content[0].memberCode").value("HV000102"))
            .andExpect(jsonPath("$.content[0].memberName").value("Trần Thị Bình"))
            .andExpect(jsonPath("$.content[0].memberPhone").value("0987654321"))
            .andExpect(jsonPath("$.content[0].total").value(3_000_000))
            .andExpect(jsonPath("$.content[0].source").value("COUNTER"))
            .andExpect(jsonPath("$.content[0].status").value("PENDING"))
            .andExpect(jsonPath("$.content[0].paidAt").value(nullValue()))
            .andExpect(jsonPath("$.content[0].createdAt").isString())
            .andExpect(jsonPath("$.content[1].id").value(second.invoiceId()));

        mvc.perform(get("/api/v1/invoices")
                .param("page", "1")
                .param("size", "2")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(first.invoiceId()));
    }

    @Test
    @DisplayName("TC-F5.3.1-02a: lọc danh sách theo status, source và keyword (mã hóa đơn, tên, số điện thoại)")
    void filterByStatusSourceAndKeyword() throws Exception {
        PackageInvoiceDto paidCounter = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);
        payInvoice(paidCounter.invoiceId(), "CASH", null);

        PackageInvoiceDto pendingPortal = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberBinh, pkgTimeCn01, InvoiceSource.MEMBER_PORTAL);

        PackageInvoiceDto cancelledCounter = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgSessionCn01, InvoiceSource.COUNTER);
        jdbc.update("UPDATE invoices SET status = 'CANCELLED' WHERE id = ?", cancelledCounter.invoiceId());

        // Lọc theo status = PAID
        mvc.perform(get("/api/v1/invoices")
                .param("status", "PAID")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(paidCounter.invoiceId()))
            .andExpect(jsonPath("$.content[0].paidAt").isString());

        // Lọc theo source = MEMBER_PORTAL
        mvc.perform(get("/api/v1/invoices")
                .param("source", "MEMBER_PORTAL")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(pendingPortal.invoiceId()));

        // Lọc kết hợp status + source
        mvc.perform(get("/api/v1/invoices")
                .param("status", "CANCELLED")
                .param("source", "COUNTER")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(cancelledCounter.invoiceId()));

        // Lọc keyword theo mã hóa đơn (không phân biệt hoa thường)
        mvc.perform(get("/api/v1/invoices")
                .param("keyword", pendingPortal.invoiceCode().toLowerCase())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(pendingPortal.invoiceId()));

        // Lọc keyword theo tên hội viên
        mvc.perform(get("/api/v1/invoices")
                .param("keyword", "  thị bình  ")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(pendingPortal.invoiceId()));

        // Lọc keyword theo số điện thoại
        mvc.perform(get("/api/v1/invoices")
                .param("keyword", "345678")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2));

        // Ký tự đặc biệt LIKE được escape, không khớp toàn bộ
        mvc.perform(get("/api/v1/invoices")
                .param("keyword", "%")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("TC-F5.3.1-02b: lọc fromDate/toDate theo ngày Việt Nam nửa mở, hóa đơn 23:30 VN thuộc đúng ngày")
    void filterByVietnamDateRangeHalfOpen() throws Exception {
        // 19/11 23:59 giờ VN = 19/11 16:59 UTC
        clock.setVn(LocalDateTime.of(2026, 11, 19, 23, 59));
        PackageInvoiceDto inv19 = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);

        // 20/11 23:30 giờ VN = 20/11 16:30 UTC -> phải thuộc ngày 20/11
        clock.setVn(LocalDateTime.of(2026, 11, 20, 23, 30));
        PackageInvoiceDto inv20Late = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgSessionCn01, InvoiceSource.COUNTER);

        // 21/11 00:00 giờ VN = 20/11 17:00 UTC -> mốc mở của ngày 20/11, phải thuộc ngày 21/11
        clock.setVn(LocalDateTime.of(2026, 11, 21, 0, 0));
        PackageInvoiceDto inv21Midnight = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgPtCn01, InvoiceSource.COUNTER);

        // 21/11 00:30 giờ VN = 20/11 17:30 UTC -> nếu lọc sai theo ngày UTC sẽ bị lọt vào 20/11!
        clock.setVn(LocalDateTime.of(2026, 11, 21, 0, 30));
        PackageInvoiceDto inv21Early = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberBinh, pkgTimeCn01, InvoiceSource.COUNTER);

        // Lọc đúng ngày 20/11: chỉ có inv20Late (23:30 VN)
        mvc.perform(get("/api/v1/invoices")
                .param("fromDate", "2026-11-20")
                .param("toDate", "2026-11-20")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(inv20Late.invoiceId()));

        // Lọc đúng ngày 21/11: gồm inv21Early và inv21Midnight, không có inv20Late
        mvc.perform(get("/api/v1/invoices")
                .param("fromDate", "2026-11-21")
                .param("toDate", "2026-11-21")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].id").value(inv21Early.invoiceId()))
            .andExpect(jsonPath("$.content[1].id").value(inv21Midnight.invoiceId()));

        // Lọc chỉ có fromDate = 2026-11-21: gồm inv21Early và inv21Midnight
        mvc.perform(get("/api/v1/invoices")
                .param("fromDate", "2026-11-21")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].id").value(inv21Early.invoiceId()))
            .andExpect(jsonPath("$.content[1].id").value(inv21Midnight.invoiceId()));

        // Lọc chỉ có toDate = 2026-11-20: gồm inv20Late và inv19
        mvc.perform(get("/api/v1/invoices")
                .param("toDate", "2026-11-20")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].id").value(inv20Late.invoiceId()))
            .andExpect(jsonPath("$.content[1].id").value(inv19.invoiceId()));
    }

    @Test
    @DisplayName("TC-F5.3.1-03a: LEFT JOIN members giúp hóa đơn khách lẻ (member_id rỗng) vẫn hiện ở danh sách và chi tiết")
    void walkInInvoiceWithNullMemberIsVisibleInListAndDetail() throws Exception {
        Long productId = jdbc.queryForObject("""
            INSERT INTO products (sku, name, price, unit, status)
            VALUES ('WATER-01', 'Nước khoáng 500ml', 15000, 'chai', 'ACTIVE') RETURNING id""", Long.class);
        OffsetDateTime createdAt = vnTime.now();
        Long walkInInvoiceId = jdbc.queryForObject("""
            INSERT INTO invoices (code, branch_id, member_id, created_by, source, subtotal, discount, total,
                                  status, paid_at, created_at, updated_at)
            VALUES ('HD990001', ?, NULL, ?, 'COUNTER', 30000, 0, 30000, 'PAID', ?, ?, ?) RETURNING id""",
            Long.class, f.cn01, receptionistCn01, createdAt, createdAt, createdAt);
        jdbc.update("""
            INSERT INTO invoice_items (invoice_id, item_type, product_id, description, quantity, unit_price, amount,
                                       discount_amount, created_at, updated_at)
            VALUES (?, 'PRODUCT', ?, 'Nước khoáng 500ml', 2, 15000, 30000, 0, ?, ?)""",
            walkInInvoiceId, productId, createdAt, createdAt);
        jdbc.update("""
            INSERT INTO payments (invoice_id, method, amount, status, received_by, paid_at, created_at, updated_at)
            VALUES (?, 'CASH', 30000, 'CONFIRMED', ?, ?, ?, ?)""",
            walkInInvoiceId, receptionistCn01, createdAt, createdAt, createdAt);

        // Danh sách không lọc: hóa đơn khách lẻ vẫn hiện
        mvc.perform(get("/api/v1/invoices")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(walkInInvoiceId))
            .andExpect(jsonPath("$.content[0].code").value("HD990001"))
            .andExpect(jsonPath("$.content[0].memberId").value(nullValue()))
            .andExpect(jsonPath("$.content[0].memberCode").value(nullValue()))
            .andExpect(jsonPath("$.content[0].memberName").value(nullValue()))
            .andExpect(jsonPath("$.content[0].memberPhone").value(nullValue()))
            .andExpect(jsonPath("$.content[0].total").value(30_000));

        // Tìm keyword theo mã hóa đơn của khách lẻ
        mvc.perform(get("/api/v1/invoices")
                .param("keyword", "HD990001")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(walkInInvoiceId));

        // Chi tiết hóa đơn khách lẻ
        mvc.perform(get("/api/v1/invoices/{id}", walkInInvoiceId)
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(walkInInvoiceId))
            .andExpect(jsonPath("$.memberId").value(nullValue()))
            .andExpect(jsonPath("$.memberName").value(nullValue()))
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].itemType").value("PRODUCT"))
            .andExpect(jsonPath("$.items[0].quantity").value(2))
            .andExpect(jsonPath("$.payments", hasSize(1)))
            .andExpect(jsonPath("$.payments[0].method").value("CASH"))
            .andExpect(jsonPath("$.payments[0].receivedByName").value("Nguyễn Văn Lễ Tân"));
    }

    @Test
    @DisplayName("TC-F5.3.1-01b: lễ tân và quản lý chỉ thấy chi nhánh mình, branchId truyền vào bị bỏ qua")
    void staffCannotOverrideBranchFilter() throws Exception {
        PackageInvoiceDto cn01Inv = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);
        invoiceService.createPackageInvoice(receptionistCn02, f.cn02, memberCn02, pkgTimeCn02, InvoiceSource.COUNTER);

        mvc.perform(get("/api/v1/invoices")
                .param("branchId", String.valueOf(f.cn02))
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(cn01Inv.invoiceId()));

        mvc.perform(get("/api/v1/invoices")
                .param("branchId", String.valueOf(f.cn02))
                .with(f.bearer(managerCn01, UserRole.BRANCH_MANAGER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(cn01Inv.invoiceId()));
    }

    @Test
    @DisplayName("TC-F5.3.1-04a: SUPER_ADMIN thấy hóa đơn toàn chuỗi và lọc được theo branchId")
    void superAdminListsAllBranchesAndFiltersByBranchId() throws Exception {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        PackageInvoiceDto cn01Inv = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);

        clock.setVn(LocalDateTime.of(2026, 11, 20, 10, 0));
        PackageInvoiceDto cn02Inv = invoiceService.createPackageInvoice(
            receptionistCn02, f.cn02, memberCn02, pkgTimeCn02, InvoiceSource.COUNTER);

        // Không truyền branchId -> thấy cả hai chi nhánh
        mvc.perform(get("/api/v1/invoices")
                .with(f.bearer(owner, UserRole.SUPER_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].id").value(cn02Inv.invoiceId()))
            .andExpect(jsonPath("$.content[1].id").value(cn01Inv.invoiceId()));

        // Truyền branchId = CN02 -> chỉ thấy CN02
        mvc.perform(get("/api/v1/invoices")
                .param("branchId", String.valueOf(f.cn02))
                .with(f.bearer(owner, UserRole.SUPER_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(cn02Inv.invoiceId()))
            .andExpect(jsonPath("$.content[0].branchId").value(f.cn02));

        // Xem chi tiết hóa đơn ở bất kỳ chi nhánh nào
        mvc.perform(get("/api/v1/invoices/{id}", cn02Inv.invoiceId())
                .with(f.bearer(owner, UserRole.SUPER_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(cn02Inv.invoiceId()))
            .andExpect(jsonPath("$.branchId").value(f.cn02));
    }

    @Test
    @DisplayName("TC-F5.3.1-05a: chi tiết hóa đơn trả đủ dòng hóa đơn và phiếu thu kèm tên người thu")
    void detailReturnsItemsAndPaymentsWithReceiverName() throws Exception {
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgPtCn01, InvoiceSource.COUNTER);

        // Khi PENDING: có 1 item, chưa có phiếu thu
        mvc.perform(get("/api/v1/invoices/{id}", sale.invoiceId())
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(sale.invoiceId()))
            .andExpect(jsonPath("$.code").value(sale.invoiceCode()))
            .andExpect(jsonPath("$.branchId").value(f.cn01))
            .andExpect(jsonPath("$.memberId").value(memberAn))
            .andExpect(jsonPath("$.memberCode").value("HV000101"))
            .andExpect(jsonPath("$.memberName").value("Nguyễn Văn An"))
            .andExpect(jsonPath("$.memberPhone").value("0912345678"))
            .andExpect(jsonPath("$.createdBy").value(receptionistCn01))
            .andExpect(jsonPath("$.source").value("COUNTER"))
            .andExpect(jsonPath("$.subtotal").value(3_000_000))
            .andExpect(jsonPath("$.discount").value(0))
            .andExpect(jsonPath("$.total").value(3_000_000))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.paidAt").value(nullValue()))
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].itemType").value("PACKAGE"))
            .andExpect(jsonPath("$.items[0].packageId").value(pkgPtCn01))
            .andExpect(jsonPath("$.items[0].description").value("Gói PT_CN01"))
            .andExpect(jsonPath("$.items[0].quantity").value(1))
            .andExpect(jsonPath("$.items[0].unitPrice").value(3_000_000))
            .andExpect(jsonPath("$.items[0].amount").value(3_000_000))
            .andExpect(jsonPath("$.payments", hasSize(0)));

        // Sau khi thu tiền: có phiếu thu CONFIRMED kèm receivedByName
        payInvoice(sale.invoiceId(), "BANK_TRANSFER", "FT261120001");

        mvc.perform(get("/api/v1/invoices/{id}", sale.invoiceId())
                .with(f.bearer(managerCn01, UserRole.BRANCH_MANAGER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"))
            .andExpect(jsonPath("$.paidAt").isString())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.payments", hasSize(1)))
            .andExpect(jsonPath("$.payments[0].method").value("BANK_TRANSFER"))
            .andExpect(jsonPath("$.payments[0].amount").value(3_000_000))
            .andExpect(jsonPath("$.payments[0].status").value("CONFIRMED"))
            .andExpect(jsonPath("$.payments[0].transactionRef").value("FT261120001"))
            .andExpect(jsonPath("$.payments[0].receivedBy").value(receptionistCn01))
            .andExpect(jsonPath("$.payments[0].receivedByName").value("Nguyễn Văn Lễ Tân"))
            .andExpect(jsonPath("$.payments[0].paidAt").isString());
    }

    @Test
    @DisplayName("TC-F5.3.1-08d: chi tiết hóa đơn chi nhánh khác hoặc không tồn tại trả 404")
    void detailOfOtherBranchOrUnknownIs404() throws Exception {
        PackageInvoiceDto cn01Inv = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);

        mvc.perform(get("/api/v1/invoices/{id}", cn01Inv.invoiceId())
                .with(f.bearer(receptionistCn02, UserRole.RECEPTIONIST)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(NOT_FOUND_DETAIL));

        mvc.perform(get("/api/v1/invoices/{id}", cn01Inv.invoiceId())
                .with(f.bearer(managerCn02, UserRole.BRANCH_MANAGER)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(NOT_FOUND_DETAIL));

        mvc.perform(get("/api/v1/invoices/{id}", 999_999L)
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(NOT_FOUND_DETAIL));
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"TRAINER", "MEMBER"})
    @DisplayName("TC-F5.3.1-08e: TRAINER và MEMBER bị 403 ở cả danh sách và chi tiết hóa đơn")
    void trainerAndMemberAreForbidden(UserRole role) throws Exception {
        Long userId = f.staff("role-" + role.name().toLowerCase() + "@example.com", role,
            role == UserRole.TRAINER ? f.cn01 : null);
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(
            receptionistCn01, f.cn01, memberAn, pkgTimeCn01, InvoiceSource.COUNTER);

        mvc.perform(get("/api/v1/invoices").with(f.bearer(userId, role)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/invoices/{id}", sale.invoiceId()).with(f.bearer(userId, role)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-F5.3.1-08f: chưa đăng nhập gọi danh sách hoặc chi tiết hóa đơn trả 401")
    void unauthenticatedRequestsAre401() throws Exception {
        mvc.perform(get("/api/v1/invoices")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/invoices/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-F5.3.1-10a: lễ tân chưa có dòng staff ACTIVE gọi danh sách hóa đơn trả 403")
    void staffWithoutActiveBranchIs403() throws Exception {
        Long unassignedReceptionist = f.staff("khong-chi-nhanh@example.com", UserRole.RECEPTIONIST, null);
        mvc.perform(get("/api/v1/invoices")
                .with(f.bearer(unassignedReceptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-F5.3.1-09a: tham số lọc không hợp lệ trả 400")
    void invalidQueryParamsAndIdReturn400() throws Exception {
        mvc.perform(get("/api/v1/invoices")
                .param("fromDate", "2026-11-25")
                .param("toDate", "2026-11-20")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/invoices")
                .param("status", "UNKNOWN")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.status").isString());

        mvc.perform(get("/api/v1/invoices")
                .param("source", "ONLINE")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.source").isString());

        mvc.perform(get("/api/v1/invoices")
                .param("fromDate", "20-11-2026")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.fromDate").isString());

        mvc.perform(get("/api/v1/invoices")
                .param("page", "-1")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.page").isString());

        mvc.perform(get("/api/v1/invoices")
                .param("size", "101")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.size").isString());

        mvc.perform(get("/api/v1/invoices/0")
                .with(f.bearer(receptionistCn01, UserRole.RECEPTIONIST)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.id").isString());
    }

    private Long createMember(String code, String fullName, String phone, Long branchId) {
        return jdbc.queryForObject("""
            INSERT INTO members (home_branch_id, member_code, full_name, phone, status)
            VALUES (?, ?, ?, ?, 'ACTIVE') RETURNING id""", Long.class, branchId, code, fullName, phone);
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
