package vn.edu.hust.nhom6.gym.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.config.SecurityConfig;
import vn.edu.hust.nhom6.gym.config.StompJwtChannelInterceptor;
import vn.edu.hust.nhom6.gym.feature.auth.JwtTestSupport;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;

/** Kiểm thử đơn vị bộ chặn JWT, hạn phiên và phân quyền kênh STOMP ({@link StompJwtChannelInterceptor}). */
class StompJwtChannelInterceptorTest {
    private static final MessageChannel NOOP_CHANNEL = (message, timeout) -> true;

    MutableClock clock;
    StompJwtChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.now());
        SecurityConfig securityConfig = new SecurityConfig();
        JwtDecoder decoder = securityConfig.jwtDecoder(JwtTestSupport.DEFAULT_TEST_SECRET);
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        interceptor = new StompJwtChannelInterceptor(decoder, converter, clock);
    }

    @Test
    @DisplayName("TC-F10.5.1-01a: CONNECT và STOMP hợp lệ gán Principal theo userId và lưu exp; thiếu hoặc sai JWT bị từ chối")
    void connectAndStompAuthenticateJwt() {
        String token = JwtTestSupport.accessToken(42L, "hv42@gym-test.vn", UserRole.MEMBER);

        StompHeaderAccessor connectAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        connectAccessor.setLeaveMutable(true);
        connectAccessor.setSessionId("ws-1");
        Map<String, Object> attrs = new HashMap<>();
        connectAccessor.setSessionAttributes(attrs);
        connectAccessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<?> connectMsg = MessageBuilder.createMessage(new byte[0], connectAccessor.getMessageHeaders());
        interceptor.preSend(connectMsg, NOOP_CHANNEL);
        assertThat(connectAccessor.getUser()).isNotNull();
        assertThat(connectAccessor.getUser().getName()).isEqualTo("42");
        assertThat(attrs.get(StompJwtChannelInterceptor.SESSION_EXPIRES_AT)).isInstanceOf(Instant.class);

        StompHeaderAccessor stomp12Accessor = StompHeaderAccessor.create(StompCommand.STOMP);
        stomp12Accessor.setLeaveMutable(true);
        stomp12Accessor.setSessionId("ws-2");
        stomp12Accessor.setSessionAttributes(new HashMap<>());
        stomp12Accessor.setNativeHeader("authorization", "bearer " + token);
        Message<?> stomp12Msg = MessageBuilder.createMessage(new byte[0], stomp12Accessor.getMessageHeaders());
        interceptor.preSend(stomp12Msg, NOOP_CHANNEL);
        assertThat(stomp12Accessor.getUser()).isNotNull();
        assertThat(stomp12Accessor.getUser().getName()).isEqualTo("42");

        StompHeaderAccessor missingToken = StompHeaderAccessor.create(StompCommand.STOMP);
        Message<?> missingMsg = MessageBuilder.createMessage(new byte[0], missingToken.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(missingMsg, NOOP_CHANNEL))
            .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    @DisplayName("TC-F10.5.1-05a: SUBSCRIBE chỉ cho phép /user/queue/notifications khi đã xác thực; SEND bị chặn")
    void subscribeAndSendRules() {
        Principal user = () -> "42";

        StompHeaderAccessor validSub = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        validSub.setUser(user);
        validSub.setDestination(StompJwtChannelInterceptor.USER_NOTIFICATIONS_DESTINATION);
        Message<?> validSubMsg = MessageBuilder.createMessage(new byte[0], validSub.getMessageHeaders());
        assertThat(interceptor.preSend(validSubMsg, NOOP_CHANNEL)).isSameAs(validSubMsg);

        StompHeaderAccessor foreignSub = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        foreignSub.setUser(user);
        foreignSub.setDestination("/queue/notifications");
        Message<?> foreignSubMsg = MessageBuilder.createMessage(new byte[0], foreignSub.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(foreignSubMsg, NOOP_CHANNEL))
            .isInstanceOf(MessageDeliveryException.class);

        StompHeaderAccessor unauthSub = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        unauthSub.setDestination(StompJwtChannelInterceptor.USER_NOTIFICATIONS_DESTINATION);
        Message<?> unauthSubMsg = MessageBuilder.createMessage(new byte[0], unauthSub.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(unauthSubMsg, NOOP_CHANNEL))
            .isInstanceOf(MessageDeliveryException.class);

        StompHeaderAccessor sendAccessor = StompHeaderAccessor.create(StompCommand.SEND);
        sendAccessor.setUser(user);
        sendAccessor.setDestination("/app/anything");
        Message<?> sendMsg = MessageBuilder.createMessage(new byte[0], sendAccessor.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(sendMsg, NOOP_CHANNEL))
            .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    @DisplayName("TC-F10.5.1-05b: sau khi JWT quá hạn exp, preSend chặn SUBSCRIBE và chặn tin đẩy ra đồng thời đóng phiên WebSocket")
    void expiredSessionRejectsInboundAndDropsOutboundAndClosesSocket() throws Exception {
        String token = JwtTestSupport.accessToken(42L, "hv42@gym-test.vn", UserRole.MEMBER);
        WebSocketSession ws1 = mock(WebSocketSession.class);
        when(ws1.getId()).thenReturn("ws-exp-1");
        when(ws1.isOpen()).thenReturn(true);
        interceptor.registerSession(ws1);

        Map<String, Object> attrs = new HashMap<>();
        StompHeaderAccessor connect = StompHeaderAccessor.create(StompCommand.CONNECT);
        connect.setLeaveMutable(true);
        connect.setSessionId("ws-exp-1");
        connect.setSessionAttributes(attrs);
        connect.setNativeHeader("Authorization", "Bearer " + token);
        interceptor.preSend(MessageBuilder.createMessage(new byte[0], connect.getMessageHeaders()), NOOP_CHANNEL);

        // Chưa hết hạn: tin đẩy ra từ SimpleBrokerMessageHandler (SimpMessageHeaderAccessor) đi qua bình thường
        SimpMessageHeaderAccessor outBefore = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        outBefore.setSessionId("ws-exp-1");
        Message<?> outBeforeMsg = MessageBuilder.createMessage(new byte[0], outBefore.getMessageHeaders());
        assertThat(interceptor.preSend(outBeforeMsg, NOOP_CHANNEL)).isSameAs(outBeforeMsg);

        // Sau 16 phút (quá TTL 15 phút): tin đẩy ra bị chặn (trả null) và phiên WebSocket bị đóng
        clock.advance(Duration.ofMinutes(16));
        SimpMessageHeaderAccessor outAfter = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        outAfter.setSessionId("ws-exp-1");
        Message<?> outAfterMsg = MessageBuilder.createMessage(new byte[0], outAfter.getMessageHeaders());
        assertThat(interceptor.preSend(outAfterMsg, NOOP_CHANNEL)).isNull();
        verify(ws1).close(any(CloseStatus.class));

        // Frame SUBSCRIBE với phiên đã quá exp cũng bị từ chối
        StompHeaderAccessor subAfter = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subAfter.setSessionId("ws-exp-1");
        subAfter.setSessionAttributes(attrs);
        subAfter.setUser(() -> "42");
        subAfter.setDestination(StompJwtChannelInterceptor.USER_NOTIFICATIONS_DESTINATION);
        Message<?> subAfterMsg = MessageBuilder.createMessage(new byte[0], subAfter.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(subAfterMsg, NOOP_CHANNEL))
            .isInstanceOf(MessageDeliveryException.class);
    }
}
