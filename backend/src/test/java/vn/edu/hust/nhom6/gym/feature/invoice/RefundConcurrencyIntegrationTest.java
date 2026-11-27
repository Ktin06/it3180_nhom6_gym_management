package vn.edu.hust.nhom6.gym.feature.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PackageInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PayInvoiceRequest;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.PaymentMethod;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.RefundLookupRepository;
import vn.edu.hust.nhom6.gym.feature.invoice.service.InvoiceService;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;

/**
 * Hai quản lý cùng hoàn một hóa đơn (UC-F5.5 E5, NF6). Theo mẫu {@code InvoicePaymentConcurrencyIntegrationTest}: hai
 * luồng chờ ở một chốt rồi cùng gửi, lặp nhiều vòng, mỗi vòng một hội viên mới. Cả hai luồng phải đọc hóa đơn PAID
 * trước khi một bên được lấy khóa hội viên, nên người sau luôn vấp version (409), không phụ thuộc lịch chạy luồng.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class RefundConcurrencyIntegrationTest {
    static final int ROUNDS = 5;
    static final String CONFLICT = "Dữ liệu hóa đơn vừa được cập nhật bởi thao tác khác. Vui lòng thử lại.";
    static final String MEMBERSHIP_USED = "Gói tập của hóa đơn đã được sử dụng nên không thể hoàn tiền. "
        + "Hội viên có thể bảo lưu hoặc chuyển nhượng gói.";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired JsonMapper json;
    @Autowired MutableClock clock;
    @Autowired InvoiceService invoiceService;
    @MockitoSpyBean MemberService memberService;
    @MockitoSpyBean RefundLookupRepository lookup;

    SaleFixtures f;
    Long receptionist;
    Long first;
    Long second;
    Long timePkg;
    ExecutorService pool;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        f = new SaleFixtures(jdbc, tokens);
        f.reset();
        receptionist = f.staff("le-tan@example.com", UserRole.RECEPTIONIST, f.cn01);
        first = f.staff("quan-ly-1@example.com", UserRole.BRANCH_MANAGER, f.cn01);
        second = f.staff("quan-ly-2@example.com", UserRole.BRANCH_MANAGER, f.cn01);
        timePkg = f.pkg("TIME_30", "TIME", null, 500_000, f.cn01, "ACTIVE");
        pool = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @Test
    @DisplayName("TC-F5.5.1-06a: Hai quản lý cùng hoàn một hóa đơn: một 200, một 409 đúng câu SRS; đúng một dòng audit_logs, hóa đơn REFUNDED")
    void twoManagersRefundingSameInvoice() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            Long member = f.member("HV00050" + round, "091200050" + round, f.cn01, "ACTIVE");
            PackageInvoiceDto sale = invoiceService.createPackageInvoice(receptionist, f.cn01, member, timePkg,
                InvoiceSource.COUNTER);
            invoiceService.pay(new CurrentUser(receptionist, UserRole.RECEPTIONIST), sale.invoiceId(),
                new PayInvoiceRequest(PaymentMethod.CASH, null));
            synchronizeBeforeMemberLock(member);

            List<MockHttpServletResponse> responses = race(
                () -> refund(first, sale.invoiceId()), () -> refund(second, sale.invoiceId()));

            // Cả hai đã đọc hóa đơn khi còn PAID; người lấy được khóa hội viên sau vấp version → 409 E5.
            assertThat(responses).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(200, 409);
            MockHttpServletResponse loser = responses.stream().filter(r -> r.getStatus() == 409).findFirst().orElseThrow();
            assertThat(json.readTree(loser.getContentAsString()).get("detail").asString()).isEqualTo(CONFLICT);
            assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM audit_logs WHERE action = 'REFUND' AND entity_id = ?", Long.class,
                sale.invoiceId())).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT status FROM invoices WHERE id = ?", String.class,
                sale.invoiceId())).isEqualTo("REFUNDED");
            assertThat(jdbc.queryForObject("SELECT status FROM memberships WHERE id = ?", String.class,
                sale.membershipId())).isEqualTo("CANCELLED");
        }
    }

    @Test
    @DisplayName("TC-F5.5.1-08b: Tác vụ nền đổi gói sang EXPIRED giữa lúc kiểm tra và lúc hủy gói thì 422 đúng câu SRS, không phải 500; hóa đơn vẫn PAID, không ghi audit_logs")
    void membershipExpiredByJobBetweenCheckAndCancel() throws Exception {
        Long member = f.member("HV000600", "0912000600", f.cn01, "ACTIVE");
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(receptionist, f.cn01, member, timePkg,
            InvoiceSource.COUNTER);
        invoiceService.pay(new CurrentUser(receptionist, UserRole.RECEPTIONIST), sale.invoiceId(),
            new PayInvoiceRequest(PaymentMethod.CASH, null));
        // Tác vụ vòng đời gói không khóa dòng hội viên: nó commit EXPIRED ngay sau khi refund đọc xong BR-F5-14.
        doAnswer(invocation -> {
            Object usage = invocation.callRealMethod();
            jdbc.update("UPDATE memberships SET status = 'EXPIRED', version = version + 1 WHERE id = ?",
                sale.membershipId());
            return usage;
        }).when(lookup).findUsage(org.mockito.ArgumentMatchers.eq(sale.invoiceId()),
            org.mockito.ArgumentMatchers.any());

        MockHttpServletResponse response = refund(first, sale.invoiceId());

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(json.readTree(response.getContentAsString()).get("detail").asString()).isEqualTo(MEMBERSHIP_USED);
        assertThat(jdbc.queryForObject("SELECT status FROM invoices WHERE id = ?", String.class,
            sale.invoiceId())).isEqualTo("PAID");
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM audit_logs WHERE action = 'REFUND' AND entity_id = ?", Long.class,
            sale.invoiceId())).isZero();
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM payments WHERE invoice_id = ? AND status = 'CONFIRMED'", Long.class,
            sale.invoiceId())).isEqualTo(1);
    }

    /** Cả hai giao dịch đã đọc hóa đơn PAID trước khi một bên được phép lấy khóa hội viên thật. */
    private void synchronizeBeforeMemberLock(Long memberId) {
        CyclicBarrier bothReadPaid = new CyclicBarrier(2);
        doAnswer(invocation -> {
            bothReadPaid.await(10, TimeUnit.SECONDS);
            return invocation.callRealMethod();
        }).when(memberService).lockForUpdate(memberId);
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

    private MockHttpServletResponse refund(Long userId, Long invoiceId) throws Exception {
        return mvc.perform(post("/api/v1/invoices/{id}/refund", invoiceId)
                .with(f.bearer(userId, UserRole.BRANCH_MANAGER))
                .contentType(MediaType.APPLICATION_JSON).content("{\"refundReason\": \"Bán nhầm gói\"}"))
            .andReturn().getResponse();
    }
}
