package vn.edu.hust.nhom6.gym.feature.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;

/**
 * Hai lễ tân bán gói cùng loại cho một hội viên gần như cùng lúc (AC-F3.5.1-07, NF6). Theo mẫu
 * {@code AuthSessionConcurrencyIntegrationTest}: hai luồng chờ ở một chốt rồi cùng gửi, lặp nhiều vòng, mỗi vòng
 * một hội viên mới. Không có khóa dòng {@code members} thì cả hai cùng qua bước kiểm tra và tạo hai gói PENDING.
 */
@SpringBootTest(properties = "app.scheduling.enabled=false")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class InvoiceSaleConcurrencyIntegrationTest {
    static final int ROUNDS = 5;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired JsonMapper json;
    @Autowired MutableClock clock;

    SaleFixtures f;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        f = new SaleFixtures(jdbc, tokens);
        f.reset();
    }

    @Test
    @DisplayName("TC-F3.5.1-07a: Hai lễ tân bán gói TIME cùng lúc cho một hội viên: một 201, một 422; chỉ một hóa đơn và một gói")
    void twoReceptionistsSellingAtOnceCreateExactlyOne() throws Exception {
        Long first = f.staff("le-tan-1@example.com", UserRole.RECEPTIONIST, f.cn01);
        Long second = f.staff("le-tan-2@example.com", UserRole.RECEPTIONIST, f.cn01);
        Long timePkg = f.pkg("TIME_30", "TIME", null, 500_000, f.cn01, "ACTIVE");
        List<Long> sellers = List.of(first, second);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                Long member = f.member("HV00020" + round, "091200020" + round, f.cn01, "ACTIVE");
                String body = InvoiceSaleIntegrationTest.body(member, timePkg);
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<MockHttpServletResponse>> futures = new ArrayList<>();

                for (Long seller : sellers) {
                    futures.add(pool.submit(() -> {
                        ready.countDown();
                        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                        return mvc.perform(post("/api/v1/invoices").with(f.bearer(seller, UserRole.RECEPTIONIST))
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                            .andReturn().getResponse();
                    }));
                }

                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
                start.countDown();

                List<MockHttpServletResponse> responses = new ArrayList<>();
                for (Future<MockHttpServletResponse> future : futures) {
                    responses.add(future.get(20, TimeUnit.SECONDS));
                }

                assertThat(responses).extracting(MockHttpServletResponse::getStatus)
                    .containsExactlyInAnyOrder(201, 422);
                MockHttpServletResponse rejected = responses.stream().filter(r -> r.getStatus() == 422)
                    .findFirst().orElseThrow();
                assertThat(json.readTree(rejected.getContentAsString()).get("detail").asString())
                    .isEqualTo(InvoiceSaleIntegrationTest.WAITING);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM invoices WHERE member_id = ?", Long.class, member))
                    .isEqualTo(1);
                assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM memberships WHERE member_id = ? AND status = 'PENDING'", Long.class, member))
                    .isEqualTo(1);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(f.count("invoice_items")).isEqualTo(ROUNDS);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("TC-F3.5.1-08f: Bán chờ khóa sửa/ngừng bán gói và kiểm tra lại trạng thái, giá sau commit")
    void saleWaitsForCatalogChangeAndReadsCommittedValues(boolean deactivate) throws Exception {
        Long seller = f.staff("counter@example.com", UserRole.RECEPTIONIST, f.cn01);
        Long member = f.member("HV000123", "0912000123", f.cn01, "ACTIVE");
        Long pkg = f.pkg("CATALOG_LOCK", "TIME", null, 500000, f.cn01, "ACTIVE");
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (var connection = jdbc.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try (var lock = connection.prepareStatement("SELECT id FROM packages WHERE id=? FOR UPDATE")) {
                lock.setLong(1, pkg);
                try (var row = lock.executeQuery()) { assertThat(row.next()).isTrue(); }
            }
            Future<MockHttpServletResponse> sale = pool.submit(() ->
                mvc.perform(post("/api/v1/invoices").with(f.bearer(seller, UserRole.RECEPTIONIST))
                    .contentType(MediaType.APPLICATION_JSON).content(InvoiceSaleIntegrationTest.body(member, pkg)))
                    .andReturn().getResponse());
            try {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                boolean blocked = false;
                while (!blocked && !sale.isDone() && System.nanoTime() < deadline) {
                    blocked = jdbc.queryForObject("SELECT count(*)>0 FROM pg_stat_activity WHERE cardinality(pg_blocking_pids(pid))>0", Boolean.class);
                    if (!blocked) { Thread.sleep(20); }
                }
                assertThat(blocked).as("Bán phải đợi khóa ghi của thao tác danh mục").isTrue();
                try (var update = connection.prepareStatement(deactivate
                        ? "UPDATE packages SET status='INACTIVE' WHERE id=?"
                        : "UPDATE packages SET price=900000 WHERE id=?")) {
                    update.setLong(1, pkg);
                    update.executeUpdate();
                }
                connection.commit();
                var response = sale.get(20, TimeUnit.SECONDS);
                assertThat(response.getStatus()).isEqualTo(deactivate ? 422 : 201);
                assertThat(f.count("invoices")).isEqualTo(deactivate ? 0 : 1);
                if (!deactivate) {
                    assertThat(jdbc.queryForObject("SELECT unit_price FROM invoice_items", Integer.class)).isEqualTo(900000);
                }
            } finally {
                connection.rollback();
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
