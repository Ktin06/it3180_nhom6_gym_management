package vn.edu.hust.nhom6.gym.feature.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.invoice.service.InvoiceService;

/**
 * {@code POST /api/v1/invoices} qua API thật, JWT thật, PostgreSQL thật (US-F3.5.1, S2-07.1). Hôm nay đặt cố định
 * 20/11/2026 giờ Việt Nam bằng {@link MutableClock}.
 */
@SpringBootTest(properties = "app.scheduling.enabled=false")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class InvoiceSaleIntegrationTest {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 20);
    static final String WAITING =
        "Hội viên đã có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt. Không thể tạo thêm gói mới.";

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

    @Test
    @DisplayName("TC-F3.5.1-01a: Bán gói tạo hóa đơn PENDING tại quầy và gói PENDING, giá lấy ở server, bỏ qua giá client gửi")
    void sellCreatesPendingInvoiceAndMembershipWithServerPrice() throws Exception {
        String body = """
            {"memberId": %d, "subtotal": 1, "total": 1, "discount": 99,
             "items": [{"itemType": "PACKAGE", "packageId": %d, "unitPrice": 1, "price": 1}]}
            """.formatted(member, timePkg);
        sell(receptionist, body)
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", matchesPattern("/api/v1/invoices/[0-9]+")))
            .andExpect(jsonPath("$.invoiceId").isNumber())
            .andExpect(jsonPath("$.invoiceCode").value(matchesPattern("HD[0-9]{6}")))
            .andExpect(jsonPath("$.membershipId").isNumber())
            .andExpect(jsonPath("$.memberId").value(member))
            .andExpect(jsonPath("$.packageId").value(timePkg))
            .andExpect(jsonPath("$.packageName").value("Gói TIME_30"))
            .andExpect(jsonPath("$.subtotal").value(500_000))
            .andExpect(jsonPath("$.discount").value(0))
            .andExpect(jsonPath("$.total").value(500_000))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.membershipStatus").value("PENDING"))
            .andExpect(jsonPath("$.startDate").value("2026-11-20"))
            .andExpect(jsonPath("$.endDate").value("2026-12-19"));

        Map<String, Object> invoice = jdbc.queryForMap("SELECT * FROM invoices");
        assertThat(invoice).containsEntry("status", "PENDING").containsEntry("source", "COUNTER")
            .containsEntry("subtotal", 500_000).containsEntry("discount", 0).containsEntry("total", 500_000)
            .containsEntry("branch_id", f.cn01).containsEntry("member_id", member)
            .containsEntry("created_by", receptionist);
        assertThat(invoice.get("promotion_id")).isNull();
        assertThat(invoice.get("paid_at")).isNull();

        Map<String, Object> item = jdbc.queryForMap("SELECT * FROM invoice_items");
        assertThat(item).containsEntry("item_type", "PACKAGE").containsEntry("package_id", timePkg)
            .containsEntry("description", "Gói TIME_30").containsEntry("unit_price", 500_000)
            .containsEntry("quantity", 1).containsEntry("amount", 500_000).containsEntry("discount_amount", 0)
            .containsEntry("invoice_id", invoice.get("id"));

        Map<String, Object> m = jdbc.queryForMap("SELECT * FROM memberships");
        assertThat(m).containsEntry("status", "PENDING").containsEntry("package_type", "TIME")
            .containsEntry("member_id", member).containsEntry("package_id", timePkg)
            .containsEntry("branch_id", f.cn01).containsEntry("sold_branch_id", f.cn01)
            .containsEntry("invoice_item_id", item.get("id")).containsEntry("freeze_count", 0);
        assertThat(m.get("total_sessions")).isNull();
        assertThat(m.get("remaining_sessions")).isNull();

        // BR-F3-13: giá trên dòng hóa đơn đã chụp, sửa giá gói sau đó không đổi.
        jdbc.update("UPDATE packages SET price = 900000 WHERE id = ?", timePkg);
        assertThat(jdbc.queryForObject("SELECT unit_price FROM invoice_items", Integer.class)).isEqualTo(500_000);
    }

    @Test
    @DisplayName("TC-F3.5.1-01b: Bán gói SESSION chép số buổi; end_date = start_date + số ngày - 1")
    void sessionPackageCopiesSessionCount() throws Exception {
        Long session = f.pkg("SES_10", "SESSION", 10, 800_000, null, "ACTIVE");
        sell(receptionist, member, session)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.total").value(800_000))
            .andExpect(jsonPath("$.startDate").value("2026-11-20"))
            .andExpect(jsonPath("$.endDate").value("2026-12-19"));
        Map<String, Object> m = jdbc.queryForMap("SELECT * FROM memberships");
        assertThat(m).containsEntry("total_sessions", 10).containsEntry("remaining_sessions", 10)
            .containsEntry("package_type", "SESSION");
        assertThat(m.get("branch_id")).as("gói toàn chuỗi giữ branch_id rỗng").isNull();
    }

    @Test
    @DisplayName("TC-F3.5.1-03a: Gia hạn khi gói TIME cũ còn hạn tới 31/12: ngày bắt đầu dự kiến 01/01, gói cũ giữ nguyên")
    void renewalStartsDayAfterOldTimePackage() throws Exception {
        Long old = f.membership(member, timePkg, "TIME", f.cn01, "ACTIVE",
            LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 31), null);
        sell(receptionist, member, timePkg)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.membershipStatus").value("PENDING"))
            .andExpect(jsonPath("$.startDate").value("2027-01-01"))
            .andExpect(jsonPath("$.endDate").value("2027-01-30"));
        assertThat(jdbc.queryForObject("SELECT status FROM memberships WHERE id = ?", String.class, old))
            .isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("TC-F3.5.1-04a: Gia hạn khi gói SESSION cũ đã hết buổi (còn hạn tới 30/11): bắt đầu dự kiến ngày mai")
    void renewalAfterEmptySessionPackageStartsTomorrow() throws Exception {
        Long session = f.pkg("SES_10", "SESSION", 10, 800_000, f.cn01, "ACTIVE");
        f.membership(member, session, "SESSION", f.cn01, "ACTIVE", LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30), 0);
        sell(receptionist, member, session)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.startDate").value(TODAY.plusDays(1).toString()))
            .andExpect(jsonPath("$.endDate").value(TODAY.plusDays(30).toString()));
    }

    @Test
    @DisplayName("TC-F3.5.1-04b: Gói SESSION cũ còn buổi thì gia hạn nối tiếp sau end_date, không bắt đầu ngày mai")
    void renewalAfterSessionPackageWithSessionsLeftStartsAfterEndDate() throws Exception {
        Long session = f.pkg("SES_10", "SESSION", 10, 800_000, f.cn01, "ACTIVE");
        f.membership(member, session, "SESSION", f.cn01, "ACTIVE", LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30), 3);
        sell(receptionist, member, session)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.startDate").value("2026-12-01"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UPCOMING", "PENDING"})
    @DisplayName("TC-F3.5.1-06a: Đã có gói cùng loại UPCOMING hoặc PENDING thì 422, không tạo hóa đơn hay gói")
    void secondWaitingPackageOfSameTypeIsRejected(String waitingStatus) throws Exception {
        f.membership(member, timePkg, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(24), null);
        f.membership(member, timePkg, "TIME", f.cn01, waitingStatus, TODAY.plusDays(25), TODAY.plusDays(54), null);
        sell(receptionist, member, timePkg)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(WAITING));
        assertNothingCreated(2);
    }

    @Test
    @DisplayName("TC-F3.5.1-06b: Mua mới khi đã có gói PENDING cùng loại (chưa có gói ACTIVE) cũng 422")
    void pendingWithoutActiveAlsoBlocks() throws Exception {
        f.membership(member, timePkg, "TIME", f.cn01, "PENDING", TODAY, TODAY.plusDays(29), null);
        sell(receptionist, member, timePkg).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value(WAITING));
        assertNothingCreated(1);
    }

    @Test
    @DisplayName("TC-F3.5.1-06c: Gói chờ khác loại không chặn (giữ đồng thời nhiều loại gói, BR-F3-10)")
    void waitingPackageOfOtherTypeDoesNotBlock() throws Exception {
        Long pt = f.pkg("PT_10", "PT", 10, 3_000_000, f.cn01, "ACTIVE");
        f.membership(member, pt, "PT", f.cn01, "PENDING", TODAY, TODAY.plusDays(29), 10);
        sell(receptionist, member, timePkg).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-F3.5.1-08a: Hội viên đã ngừng hoạt động thì 422, không tạo gì")
    void inactiveMemberIsRejected() throws Exception {
        Long inactive = f.member("HV000124", "0912000124", f.cn01, "INACTIVE");
        sell(receptionist, inactive, timePkg).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Hội viên đã ngừng hoạt động, không thể bán gói tập."));
        assertNothingCreated(0);
    }

    @Test
    @DisplayName("TC-F3.5.1-05a: Đang có TIME vẫn bán PT PENDING độc lập, không sửa gói TIME")
    void differentPackageTypesKeepExistingMembershipIntact() throws Exception {
        Long old = f.membership(member, timePkg, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(10), TODAY.plusDays(19), null);
        var before = jdbc.queryForMap("SELECT * FROM memberships WHERE id=?", old);
        Long pt = f.pkg("PT_NEW", "PT", 10, 3000000, f.cn01, "ACTIVE");
        sell(receptionist, member, pt).andExpect(status().isCreated())
            .andExpect(jsonPath("$.membershipStatus").value("PENDING"));
        assertThat(jdbc.queryForMap("SELECT * FROM memberships WHERE id=?", old)).isEqualTo(before);
    }

    @Test
    @DisplayName("TC-F3.5.1-01c: Lỗi tạo membership rollback hóa đơn và dòng hóa đơn đã insert")
    void membershipFailureRollsBackEntireSale() throws Exception {
        jdbc.execute("""
            CREATE FUNCTION test_reject_membership() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN RAISE EXCEPTION 'test membership insert failure'; END $$""");
        jdbc.execute("CREATE TRIGGER test_membership BEFORE INSERT ON memberships FOR EACH ROW EXECUTE FUNCTION test_reject_membership()");
        try {
            sell(receptionist, member, timePkg).andExpect(status().isInternalServerError());
            assertNothingCreated(0);
        } finally {
            jdbc.execute("DROP TRIGGER test_membership ON memberships");
            jdbc.execute("DROP FUNCTION test_reject_membership()");
        }
        sell(receptionist, member, timePkg).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-F3.5.1-08b: Gói đã ngừng bán thì 422, không tạo gì")
    void inactivePackageIsRejected() throws Exception {
        Long stopped = f.pkg("OLD", "TIME", null, 100_000, f.cn01, "INACTIVE");
        sell(receptionist, member, stopped).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Gói tập này đã ngừng bán."));
        assertNothingCreated(0);
    }

    @Test
    @DisplayName("TC-F3.5.1-08c: Gói thuộc chi nhánh khác thì 422, không tạo gì")
    void otherBranchPackageIsRejected() throws Exception {
        Long cn02Pkg = f.pkg("CN02_TIME", "TIME", null, 100_000, f.cn02, "ACTIVE");
        sell(receptionist, member, cn02Pkg).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Gói tập này không áp dụng tại chi nhánh của bạn."));
        assertNothingCreated(0);
    }

    @Test
    @DisplayName("TC-F3.5.1-08d: memberId hoặc packageId không tồn tại thì 404, không tạo gì")
    void unknownMemberOrPackageIsNotFound() throws Exception {
        sell(receptionist, 999_999L, timePkg).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Hội viên không tồn tại trên hệ thống."));
        sell(receptionist, member, 999_999L).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Gói tập không tồn tại trên hệ thống."));
        assertNothingCreated(0);
    }

    @Test
    @DisplayName("TC-F3.5.1-08e: hội viên thuộc chi nhánh khác thì 404 như F2, không bán chéo chi nhánh")
    void memberOfOtherBranchIsHidden() throws Exception {
        Long cn02Member = f.member("HV000125", "0912000125", f.cn02, "ACTIVE");
        Long global = f.pkg("ALL_TIME", "TIME", null, 100_000, null, "ACTIVE");
        sell(receptionist, cn02Member, global).andExpect(status().isNotFound());
        assertNothingCreated(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"items\": [{\"itemType\": \"PACKAGE\", \"packageId\": 1}]}",
        "{\"memberId\": 1}",
        "{\"memberId\": 1, \"items\": []}",
        "{\"memberId\": 1, \"items\": [{\"itemType\": \"PACKAGE\"}]}",
        "{\"memberId\": 1, \"items\": [{\"packageId\": 1}]}",
        "{\"memberId\": 1, \"items\": [{\"itemType\": \"PRODUCT\", \"packageId\": 1}]}",
        "{\"memberId\": 1, \"items\": [{\"itemType\": \"PACKAGE\", \"packageId\": 1}, {\"itemType\": \"PACKAGE\", \"packageId\": 1}]}",
        "{\"memberId\": 0, \"items\": [{\"itemType\": \"PACKAGE\", \"packageId\": 1}]}",
        "{\"memberId\": \"abc\", \"items\": [{\"itemType\": \"PACKAGE\", \"packageId\": 1}]}",
        "{\"memberId\": 1, \"items\": [{\"itemType\": \"KHAC\", \"packageId\": 1}]}",
        "{"
    })
    @DisplayName("TC-F3.5.1-11a: Thiếu memberId, items, packageId hoặc dữ liệu sai thì 400 kèm lỗi từng trường, không tạo gì")
    void invalidBodyIsBadRequest(String body) throws Exception {
        sell(receptionist, body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isMap());
        assertNothingCreated(0);
    }

    @Test
    @DisplayName("TC-F3.5.1-11d: Lỗi 400 chỉ đúng trường thiếu")
    void missingFieldsAreNamed() throws Exception {
        sell(receptionist, "{\"items\": [{\"itemType\": \"PACKAGE\"}]}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.memberId").value("Vui lòng chọn hội viên."))
            .andExpect(jsonPath("$.errors['items[0].packageId']").value("Vui lòng chọn gói tập."));
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"RECEPTIONIST"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("TC-F3.5.1-11b: Vai trò khác Lễ tân thì 403, không tạo gì")
    void otherRolesAreForbidden(UserRole role) throws Exception {
        Long user = f.staff("khac-" + role.name().toLowerCase() + "@example.com", role,
            role == UserRole.SUPER_ADMIN || role == UserRole.MEMBER ? null : f.cn01);
        mvc.perform(post("/api/v1/invoices").with(f.bearer(user, role))
                .contentType(MediaType.APPLICATION_JSON).content(body(member, timePkg)))
            .andExpect(status().isForbidden());
        assertNothingCreated(0);
    }

    @Test
    @DisplayName("TC-F3.5.1-11c: Không gửi token thì 401, không tạo gì")
    void missingTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON).content(body(member, timePkg)))
            .andExpect(status().isUnauthorized());
        assertNothingCreated(0);
    }

    @Test
    @DisplayName("TC-F3.5.1-13a: mã hóa đơn HD + 6 chữ số, mỗi hóa đơn một mã khác nhau")
    void invoiceCodesAreSequential() throws Exception {
        Long other = f.member("HV000126", "0912000126", f.cn01, "ACTIVE");
        sell(receptionist, member, timePkg).andExpect(status().isCreated());
        sell(receptionist, other, timePkg).andExpect(status().isCreated());
        assertThat(jdbc.queryForList("SELECT code FROM invoices ORDER BY id", String.class))
            .hasSize(2).allMatch(c -> c.matches("HD[0-9]{6}")).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("BR-F3-13: kiểm tra gói chờ trước khi lấy số; lỗi nghiệp vụ 422 không lấy số mới")
    void rejectedSaleDoesNotConsumeInvoiceCode() throws Exception {
        Long other = f.member("HV000127", "0912000127", f.cn01, "ACTIVE");
        sell(receptionist, member, timePkg).andExpect(status().isCreated());
        sell(receptionist, member, timePkg).andExpect(status().isUnprocessableContent());
        sell(receptionist, other, timePkg).andExpect(status().isCreated());
        List<Integer> numbers = jdbc.queryForList("SELECT substring(code, 3)::int FROM invoices ORDER BY id", Integer.class);
        assertThat(numbers.get(1)).isEqualTo(numbers.get(0) + 1);
    }

    @Test
    @DisplayName("S3: getInScope trả hóa đơn cùng chi nhánh, chủ chuỗi xem mọi chi nhánh, chi nhánh khác 404")
    void getInScopeHidesOtherBranches() throws Exception {
        sell(receptionist, member, timePkg).andExpect(status().isCreated());
        Long invoiceId = jdbc.queryForObject("SELECT id FROM invoices", Long.class);
        Long cn02Staff = f.staff("le-tan-cn02@example.com", UserRole.RECEPTIONIST, f.cn02);
        Long owner = f.staff("chu@example.com", UserRole.SUPER_ADMIN, null);

        assertThat(invoiceService.getInScope(invoiceId, new CurrentUser(receptionist, UserRole.RECEPTIONIST)).getTotal())
            .isEqualTo(500_000);
        assertThat(invoiceService.getInScope(invoiceId, new CurrentUser(owner, UserRole.SUPER_ADMIN)).getId())
            .isEqualTo(invoiceId);
        assertThatThrownBy(() -> invoiceService.getInScope(invoiceId, new CurrentUser(cn02Staff, UserRole.RECEPTIONIST)))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> invoiceService.getInScope(999_999L, new CurrentUser(owner, UserRole.SUPER_ADMIN)))
            .isInstanceOf(NotFoundException.class);
    }

    private void assertNothingCreated(long membershipsBefore) {
        assertThat(f.count("invoices")).isZero();
        assertThat(f.count("invoice_items")).isZero();
        assertThat(f.count("memberships")).isEqualTo(membershipsBefore);
    }

    private ResultActions sell(Long userId, Long memberId, Long packageId) throws Exception {
        return sell(userId, body(memberId, packageId));
    }

    private ResultActions sell(Long userId, String body) throws Exception {
        return mvc.perform(post("/api/v1/invoices").with(f.bearer(userId, UserRole.RECEPTIONIST))
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    static String body(Long memberId, Long packageId) {
        return "{\"memberId\": %d, \"items\": [{\"itemType\": \"PACKAGE\", \"packageId\": %d}]}"
            .formatted(memberId, packageId);
    }
}
