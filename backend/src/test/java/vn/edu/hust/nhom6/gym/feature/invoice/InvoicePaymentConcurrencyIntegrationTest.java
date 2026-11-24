package vn.edu.hust.nhom6.gym.feature.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PackageInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.service.InvoiceService;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;

/**
 * Hai nhân viên bấm cùng lúc (NF6, BR-F5-05). Theo mẫu {@code InvoiceSaleConcurrencyIntegrationTest}: hai luồng chờ
 * ở một chốt rồi cùng gửi, lặp nhiều vòng, mỗi vòng một hội viên mới.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class InvoicePaymentConcurrencyIntegrationTest {
    static final int ROUNDS = 5;
    static final String CONFLICT = "Hóa đơn này vừa được xác nhận thanh toán bởi một nhân viên khác.";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired JsonMapper json;
    @Autowired MutableClock clock;
    @Autowired InvoiceService invoiceService;
    @MockitoSpyBean MemberService memberService;

    SaleFixtures f;
    Long first;
    Long second;
    Long timePkg;
    ExecutorService pool;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        f = new SaleFixtures(jdbc, tokens);
        f.reset();
        first = f.staff("le-tan-1@example.com", UserRole.RECEPTIONIST, f.cn01);
        second = f.staff("le-tan-2@example.com", UserRole.RECEPTIONIST, f.cn01);
        timePkg = f.pkg("TIME_30", "TIME", null, 500_000, f.cn01, "ACTIVE");
        pool = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @Test
    @DisplayName("TC-F5.2.1-07a: Hai lễ tân cùng xác nhận thu tiền một hóa đơn: một 200, một 409; đúng một phiếu thu, version = 1")
    void twoReceptionistsPayingSameInvoice() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            Long member = f.member("HV00030" + round, "091200030" + round, f.cn01, "ACTIVE");
            PackageInvoiceDto sale = sell(member, timePkg);
            synchronizeBeforeMemberLock(member);

            List<MockHttpServletResponse> responses = race(
                () -> pay(first, sale.invoiceId()), () -> pay(second, sale.invoiceId()));

            assertThat(responses).extracting(MockHttpServletResponse::getStatus)
                .containsExactlyInAnyOrder(200, 409);
            for (MockHttpServletResponse r : responses) {
                if (r.getStatus() == 409) {
                    assertThat(json.readTree(r.getContentAsString()).get("detail").asString()).isEqualTo(CONFLICT);
                }
            }
            assertThat(jdbc.queryForObject("SELECT count(*) FROM payments WHERE invoice_id = ?", Long.class,
                sale.invoiceId())).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT version FROM invoices WHERE id = ?", Integer.class,
                sale.invoiceId())).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT status FROM memberships WHERE id = ?", String.class,
                sale.membershipId())).isEqualTo("ACTIVE");
        }
    }

    /**
     * Hai hóa đơn gói cùng loại của một hội viên (gói toàn chuỗi và gói CN01 là hai phạm vi khác nhau nên dùng hai
     * gói cùng phạm vi CN01, hóa đơn thứ hai dựng bằng SQL vì luồng bán chặn E6d) được thu tiền cùng lúc khi hội
     * viên đang có gói ACTIVE. Không khóa dòng hội viên thì cả hai cùng thấy "chưa có UPCOMING" và cùng thành
     * UPCOMING; có khóa thì đúng một 200, một 422 E6b.
     */
    @Test
    @DisplayName("TC-F5.2.1-09a: Thu tiền cùng lúc hai hóa đơn gia hạn cùng loại: một 200, một 422; chỉ một gói UPCOMING")
    void twoRenewalsPaidAtOnceYieldOneUpcoming() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            Long member = f.member("HV00040" + round, "091200040" + round, f.cn01, "ACTIVE");
            f.membership(member, timePkg, "TIME", f.cn01, "ACTIVE",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null);
            PackageInvoiceDto a = sell(member, timePkg);
            Long b = secondPendingInvoice(member, a);
            synchronizeBeforeMemberLock(member);

            List<MockHttpServletResponse> responses = race(() -> pay(first, a.invoiceId()), () -> pay(second, b));

            assertThat(responses).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(200, 422);
            assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM memberships WHERE member_id = ? AND status = 'UPCOMING'", Long.class, member))
                .isEqualTo(1);
            assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM invoices WHERE member_id = ? AND status = 'PAID'", Long.class, member))
                .isEqualTo(1);
        }
    }

    private List<MockHttpServletResponse> race(Callable<MockHttpServletResponse> a,
            Callable<MockHttpServletResponse> b) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
        for (Callable<MockHttpServletResponse> task : List.of(a, b)) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                return task.call();
            }));
        }
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        List<MockHttpServletResponse> responses = new ArrayList<>();
        for (Future<MockHttpServletResponse> future : futures) {
            responses.add(future.get(20, TimeUnit.SECONDS));
        }
        return responses;
    }

    /** Cả hai giao dịch đã đọc hóa đơn PENDING trước khi một bên được phép lấy khóa hội viên thật. */
    private void synchronizeBeforeMemberLock(Long memberId) {
        CyclicBarrier bothReadPending = new CyclicBarrier(2);
        doAnswer(invocation -> {
            bothReadPending.await(10, TimeUnit.SECONDS);
            return invocation.callRealMethod();
        }).when(memberService).lockForUpdate(memberId);
    }

    private MockHttpServletResponse pay(Long userId, Long invoiceId) throws Exception {
        return mvc.perform(post("/api/v1/invoices/{id}/payments", invoiceId)
                .with(f.bearer(userId, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\": \"CASH\"}"))
            .andReturn().getResponse();
    }

    private PackageInvoiceDto sell(Long member, Long packageId) {
        return invoiceService.createPackageInvoice(first, f.cn01, member, packageId, InvoiceSource.COUNTER);
    }

    /** Hóa đơn PENDING thứ hai cùng gói, dựng bằng SQL (giống hóa đơn cổng hội viên lọt qua trước khi có Q4). */
    private Long secondPendingInvoice(Long member, PackageInvoiceDto a) {
        Long invoiceId = jdbc.queryForObject("""
            INSERT INTO invoices (code, branch_id, member_id, created_by, source, subtotal, discount, total, status)
            VALUES (?, ?, ?, ?, 'MEMBER_PORTAL', 500000, 0, 500000, 'PENDING') RETURNING id""", Long.class,
            "HD9" + a.invoiceCode().substring(3), f.cn01, member, first);
        Long itemId = jdbc.queryForObject("""
            INSERT INTO invoice_items (invoice_id, item_type, package_id, description, quantity, unit_price, amount,
                                       discount_amount)
            VALUES (?, 'PACKAGE', ?, 'Gói TIME_30', 1, 500000, 500000, 0) RETURNING id""", Long.class,
            invoiceId, timePkg);
        jdbc.update("""
            INSERT INTO memberships (member_id, package_id, package_type, branch_id, sold_branch_id, invoice_item_id,
                                     start_date, end_date, status)
            VALUES (?, ?, 'TIME', ?, ?, ?, ?, ?, 'PENDING')""",
            member, timePkg, f.cn01, f.cn01, itemId, a.startDate(), a.endDate());
        return invoiceId;
    }
}
