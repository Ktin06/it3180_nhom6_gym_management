package vn.edu.hust.nhom6.gym.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.JwtTestSupport;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures.Account;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.SessionTimeRequest;
import vn.edu.hust.nhom6.gym.feature.groupclass.service.ClassSessionService;

/**
 * Kiểm thử WebSocket STOMP thật (S2-22, #180): xác thực JWT ở khung {@code CONNECT} và {@code STOMP} 1.2,
 * chặn {@code SUBSCRIBE} ngoài {@code /user/queue/notifications}, và đẩy thông báo thật tới đúng hội viên
 * sau khi commit.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class NotificationWebSocketIntegrationTest {
    static final LocalDateTime NOW_VN = LocalDateTime.of(2026, 11, 21, 10, 0);
    static final LocalDateTime CLASS_START_VN = LocalDateTime.of(2026, 11, 22, 18, 0);
    static final String ALLOWED_ORIGIN = "http://localhost:5173";

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired ClassSessionService classSessions;
    @Autowired JsonMapper jsonMapper;
    @Autowired SimpUserRegistry userRegistry;

    NotificationFixtures f;
    CurrentUser manager;

    @BeforeEach
    void setUp() {
        clock.setVn(NOW_VN);
        f = new NotificationFixtures(jdbc, tokens);
        f.reset();
        Long managerUserId = f.bookings.f.user("ql-ws@example.com", UserRole.BRANCH_MANAGER, f.cn01);
        manager = new CurrentUser(managerUserId, UserRole.BRANCH_MANAGER);
    }

    @Test
    @DisplayName("US-F10.5.1 A3: STOMP CONNECT không có token, token sai chữ ký hoặc hết hạn đều bị từ chối")
    void stompConnectRejectsMissingOrInvalidTokens() {
        Account hv = f.account(f.cn01);

        assertConnectRejected(new StompHeaders());

        String wrongSig = JwtTestSupport.accessTokenWithSecret(
            hv.userId(), "hv@gym-test.vn", UserRole.MEMBER,
            "mot-khoa-bi-mat-khac-dai-hon-32-ky-tu-de-kiem-thu-ws");
        assertConnectRejected(bearerConnectHeaders(wrongSig));

        String expired = tokens.generateAccessToken(
            hv.userId(), "hv@gym-test.vn", UserRole.MEMBER, Instant.now().minus(Duration.ofHours(1)));
        assertConnectRejected(bearerConnectHeaders(expired));

        StompHeaders rawHeader = new StompHeaders();
        rawHeader.add(HttpHeaders.AUTHORIZATION,
            tokens.generateAccessToken(hv.userId(), "hv@gym-test.vn", UserRole.MEMBER));
        assertConnectRejected(rawHeader);
    }

    @Test
    @DisplayName("US-F10.5.1 A3: STOMP 1.2 frame STOMP không token trả ERROR, kèm token hợp lệ trả CONNECTED")
    void stomp12FrameRequiresValidJwt() throws Exception {
        Account hv = f.account(f.cn01);
        String validToken = tokens.generateAccessToken(hv.userId(), "hv-stomp12@gym-test.vn", UserRole.MEMBER);

        String unauthFrame = "STOMP\naccept-version:1.2\nhost:localhost\n\n\0";
        assertThat(firstLine(sendRawStompFrame(unauthFrame))).isEqualTo("ERROR");

        String authFrame = "STOMP\naccept-version:1.2\nhost:localhost\nAuthorization:Bearer " + validToken + "\n\n\0";
        assertThat(firstLine(sendRawStompFrame(authFrame))).isEqualTo("CONNECTED");
    }

    @Test
    @DisplayName("US-F10.5.1 A3: thông báo mới đẩy qua /user/queue/notifications tới đúng hội viên, người khác không nhận")
    void notificationPushedOnlyToIntendedUserAfterCommit() throws Exception {
        Account hvA = f.account(f.cn01);
        Account hvB = f.account(f.cn01);
        Long sessionId = f.session(f.cn01, CLASS_START_VN, "SCHEDULED");
        f.booking(sessionId, hvA.memberId(), "BOOKED");

        String tokenA = tokens.generateAccessToken(hvA.userId(), "hva@gym-test.vn", UserRole.MEMBER);
        String tokenB = tokens.generateAccessToken(hvB.userId(), "hvb@gym-test.vn", UserRole.MEMBER);

        WebSocketStompClient clientA = newStompClient();
        WebSocketStompClient clientB = newStompClient();
        StompSession sessionA = connect(clientA, tokenA);
        StompSession sessionB = connect(clientB, tokenB);
        try {
            BlockingQueue<String> inboxA = subscribeUserQueue(sessionA);
            BlockingQueue<String> inboxB = subscribeUserQueue(sessionB);
            awaitUserSubscription(hvA.userId(), "/user/queue/notifications", Duration.ofSeconds(5));
            awaitUserSubscription(hvB.userId(), "/user/queue/notifications", Duration.ofSeconds(5));

            classSessions.changeTime(manager, sessionId, new SessionTimeRequest(
                NotificationFixtures.utc(CLASS_START_VN.plusHours(1)),
                NotificationFixtures.utc(CLASS_START_VN.plusHours(2))));

            String rawA = inboxA.poll(5, TimeUnit.SECONDS);
            assertThat(rawA).as("Hội viên A phải nhận thông báo qua /user/queue/notifications").isNotNull();
            JsonNode jsonA = jsonMapper.readTree(rawA);
            assertThat(jsonA.path("id").asLong()).isPositive();
            assertThat(jsonA.path("type").asString()).isEqualTo("CLASS_RESCHEDULED");
            assertThat(jsonA.path("title").asString()).contains("Yoga");
            assertThat(jsonA.path("content").asString()).contains("18:00").contains("19:00").contains("22/11/2026");
            assertThat(jsonA.path("relatedEntity").asString()).isEqualTo("class_sessions");
            assertThat(jsonA.path("relatedId").asLong()).isEqualTo(sessionId);
            assertThat(jsonA.path("read").asBoolean()).isFalse();
            assertThat(jsonA.path("readAt").isNull()).isTrue();
            assertThat(jsonA.path("createdAt").asString()).isNotBlank();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE id = ? AND channel = 'IN_APP'",
                Integer.class, jsonA.path("id").asLong())).isEqualTo(1);

            String rawB = inboxB.poll(500, TimeUnit.MILLISECONDS);
            assertThat(rawB).as("Hội viên B không được nhận thông báo của hội viên A").isNull();
        } finally {
            if (sessionA.isConnected()) {
                sessionA.disconnect();
            }
            if (sessionB.isConnected()) {
                sessionB.disconnect();
            }
            clientA.stop();
            clientB.stop();
        }
    }

    @Test
    @DisplayName("US-F10.5.1 A3: STOMP SUBSCRIBE ngoài /user/queue/notifications bị từ chối")
    void stompSubscribeOutsideUserQueueIsRejected() throws Exception {
        Account hv = f.account(f.cn01);
        String token = tokens.generateAccessToken(hv.userId(), "hv-sub@gym-test.vn", UserRole.MEMBER);

        WebSocketStompClient client = newStompClient();
        CompletableFuture<Throwable> transportError = new CompletableFuture<>();
        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        httpHeaders.add(HttpHeaders.ORIGIN, ALLOWED_ORIGIN);
        StompSession session = client.connectAsync(wsUrl(), httpHeaders, bearerConnectHeaders(token),
            new StompSessionHandlerAdapter() {
                @Override
                public void handleTransportError(StompSession s, Throwable ex) {
                    transportError.complete(ex);
                }

                @Override
                public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {
                    transportError.complete(ex);
                }
            }).get(5, TimeUnit.SECONDS);

        try {
            session.subscribe("/queue/notifications", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return byte[].class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                }
            });

            Throwable error = transportError.get(5, TimeUnit.SECONDS);
            assertThat(error).isNotNull();
        } finally {
            if (session.isConnected()) {
                session.disconnect();
            }
            client.stop();
        }
    }

    private void assertConnectRejected(StompHeaders connectHeaders) {
        WebSocketStompClient client = newStompClient();
        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        httpHeaders.add(HttpHeaders.ORIGIN, ALLOWED_ORIGIN);
        try {
            assertThatThrownBy(() -> client.connectAsync(wsUrl(), httpHeaders, connectHeaders,
                    new StompSessionHandlerAdapter() { }).get(5, TimeUnit.SECONDS))
                .isInstanceOf(Exception.class)
                .hasMessageNotContaining("HTTP response code [401]")
                .hasMessageNotContaining("HTTP response code [404]");
        } finally {
            client.stop();
        }
    }

    private StompSession connect(WebSocketStompClient client, String accessToken) throws Exception {
        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        httpHeaders.add(HttpHeaders.ORIGIN, ALLOWED_ORIGIN);
        return client.connectAsync(wsUrl(), httpHeaders, bearerConnectHeaders(accessToken),
            new StompSessionHandlerAdapter() {
                @Override
                public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {
                    throw new AssertionError("Lỗi xử lý frame STOMP: " + ex.getMessage(), ex);
                }
            }).get(5, TimeUnit.SECONDS);
    }

    private static BlockingQueue<String> subscribeUserQueue(StompSession session) {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof byte[] bytes) {
                    queue.add(new String(bytes, StandardCharsets.UTF_8));
                }
            }
        });
        return queue;
    }

    private static StompHeaders bearerConnectHeaders(String accessToken) {
        StompHeaders headers = new StompHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        return headers;
    }

    private static WebSocketStompClient newStompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new SimpleMessageConverter());
        return client;
    }

    private void awaitUserSubscription(Long userId, String destination, Duration timeout) throws InterruptedException {
        String userName = String.valueOf(userId);
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            var user = userRegistry.getUser(userName);
            boolean subscribed = user != null && user.getSessions().stream()
                .flatMap(s -> s.getSubscriptions().stream())
                .anyMatch(sub -> destination.equals(sub.getDestination()));
            if (subscribed) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(20);
        }
        throw new AssertionError("Hết thời gian chờ đăng ký WebSocket " + destination + " cho userId=" + userId);
    }

    private String sendRawStompFrame(String frame) throws Exception {
        StandardWebSocketClient rawClient = new StandardWebSocketClient();
        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        httpHeaders.add(HttpHeaders.ORIGIN, ALLOWED_ORIGIN);
        CompletableFuture<String> firstReply = new CompletableFuture<>();
        WebSocketSession session = rawClient.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                firstReply.complete(message.getPayload());
            }
        }, httpHeaders, URI.create(wsUrl())).get(5, TimeUnit.SECONDS);
        try {
            session.sendMessage(new TextMessage(frame));
            return firstReply.get(5, TimeUnit.SECONDS);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
    }

    private static String firstLine(String stompFrame) {
        int newline = stompFrame.indexOf('\n');
        String line = newline >= 0 ? stompFrame.substring(0, newline) : stompFrame;
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }

    private String wsUrl() {
        return "ws://localhost:" + port + "/ws";
    }
}
