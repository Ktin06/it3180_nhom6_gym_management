package vn.edu.hust.nhom6.gym.feature.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
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
import vn.edu.hust.nhom6.gym.feature.invoice.service.InvoiceService;

/**
 * Hai quản lý cùng hoàn một hóa đơn (UC-F5.5 E5, NF6). Theo mẫu {@code InvoicePaymentConcurrencyIntegrationTest}: hai
 * luồng chờ ở một chốt rồi cùng gửi, lặp nhiều vòng, mỗi vòng một hội viên mới.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class RefundConcurrencyIntegrationTest {
    static final int ROUNDS = 5;
    static final String CONFLICT = "Dữ liệu hóa đơn vừa được cập nhật bởi thao tác khác. Vui lòng thử lại.";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired JsonMapper json;
    @Autowired MutableClock clock;
    @Autowired InvoiceService invoiceService;

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
    @DisplayName("TC-F5.5.1-06a: Hai quản lý cùng hoàn một hóa đơn: một 200, một 409 đúng câu SRS; đúng một dòng "
        + "audit_logs, hóa đơn REFUNDED")
    void twoManagersRefundingSameInvoice() throws Exception {
        int conflicts = 0;
        for (int round = 0; round < ROUNDS; round++) {
            Long member = f.member("HV00050" + round, "091200050" + round, f.cn01, "ACTIVE");
            PackageInvoiceDto sale = invoiceService.createPackageInvoice(receptionist, f.cn01, member, timePkg,
                InvoiceSource.COUNTER);
            invoiceService.pay(new CurrentUser(receptionist, UserRole.RECEPTIONIST), sale.invoiceId(),
                new PayInvoiceRequest(PaymentMethod.CASH, null));

            List<MockHttpServletResponse> responses = race(
                () -> refund(first, sale.invoiceId()), () -> refund(second, sale.invoiceId()));

            // Người sau đã đọc hóa đơn khi còn PAID thì vấp version → 409 E5. Nếu người trước xong hẳn trước lúc người
            // sau đọc, người sau thấy REFUNDED → 422 E6, cũng đúng và cũng không ghi gì.
            assertThat(responses).extracting(MockHttpServletResponse::getStatus).contains(200)
                .filteredOn(s -> s != 200).hasSize(1).first().isIn(409, 422);
            for (MockHttpServletResponse r : responses) {
                if (r.getStatus() == 409) {
                    conflicts++;
                    assertThat(json.readTree(r.getContentAsString()).get("detail").asString()).isEqualTo(CONFLICT);
                }
            }
            assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM audit_logs WHERE action = 'REFUND' AND entity_id = ?", Long.class,
                sale.invoiceId())).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT status FROM invoices WHERE id = ?", String.class,
                sale.invoiceId())).isEqualTo("REFUNDED");
            assertThat(jdbc.queryForObject("SELECT status FROM memberships WHERE id = ?", String.class,
                sale.membershipId())).isEqualTo("CANCELLED");
        }
        assertThat(conflicts).as("ít nhất một vòng phải đi đúng nhánh 409 của khóa lạc quan").isPositive();
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
