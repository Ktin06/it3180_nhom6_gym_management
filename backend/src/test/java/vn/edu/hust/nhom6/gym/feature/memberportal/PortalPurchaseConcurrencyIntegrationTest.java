package vn.edu.hust.nhom6.gym.feature.memberportal;

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
import vn.edu.hust.nhom6.gym.feature.invoice.SaleFixtures;
import vn.edu.hust.nhom6.gym.feature.memberportal.PortalFixtures.MemberAccount;

/**
 * NF6 của UC-F7.4: hội viên bấm "Mua" hai lần gần như cùng lúc (hai tab, mạng chậm). Hai luồng chờ ở một chốt rồi
 * cùng gửi, lặp nhiều vòng. Không khóa dòng {@code members} thì cả hai cùng qua bước kiểm tra trùng.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class PortalPurchaseConcurrencyIntegrationTest {
    private static final int ROUNDS = 5;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired JsonMapper json;
    @Autowired MutableClock clock;

    SaleFixtures f;
    PortalFixtures p;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        f = new SaleFixtures(jdbc, tokens);
        f.reset();
        p = new PortalFixtures(jdbc);
        p.resetTtl();
    }

    @Test
    @DisplayName("US-F7.4.1 NF6: Hai yêu cầu mua cùng gói gửi cùng lúc: một 201, một 409; chỉ một hóa đơn và một gói")
    void twoPurchasesAtOnceCreateExactlyOne() throws Exception {
        Long timePkg = f.pkg("TIME_30", "TIME", null, 500_000, f.cn01, "ACTIVE");
        String body = "{\"packageId\": " + timePkg + "}";
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                MemberAccount me = p.memberAccount("HV00040" + round, "091200040" + round, f.cn01, "ACTIVE");
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    futures.add(pool.submit(() -> {
                        ready.countDown();
                        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                        return mvc.perform(post("/api/v1/me/purchase-requests")
                                .with(f.bearer(me.userId(), UserRole.MEMBER))
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
                    .containsExactlyInAnyOrder(201, 409);
                MockHttpServletResponse rejected = responses.stream().filter(r -> r.getStatus() == 409)
                    .findFirst().orElseThrow();
                assertThat(json.readTree(rejected.getContentAsString()).get("detail").asString())
                    .isEqualTo(PortalPurchaseIntegrationTest.DUPLICATE);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM invoices WHERE member_id = ?", Long.class,
                    me.memberId())).isEqualTo(1);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM memberships WHERE member_id = ?", Long.class,
                    me.memberId())).isEqualTo(1);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
