package vn.edu.hust.nhom6.gym.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import vn.edu.hust.nhom6.gym.config.SecurityConfig;
import vn.edu.hust.nhom6.gym.config.StompJwtChannelInterceptor;
import vn.edu.hust.nhom6.gym.feature.auth.JwtTestSupport;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;

/** Kiểm thử đơn vị bộ chặn JWT và phân quyền kênh STOMP ({@link StompJwtChannelInterceptor}). */
class StompJwtChannelInterceptorTest {
    private static final MessageChannel NOOP_CHANNEL = (message, timeout) -> true;

    StompJwtChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        SecurityConfig securityConfig = new SecurityConfig();
        JwtDecoder decoder = securityConfig.jwtDecoder(JwtTestSupport.DEFAULT_TEST_SECRET);
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        interceptor = new StompJwtChannelInterceptor(decoder, converter);
    }

    @Test
    @DisplayName("US-F10.5.1 A3: CONNECT và STOMP hợp lệ gán Principal theo userId; thiếu hoặc sai JWT bị từ chối")
    void connectAndStompAuthenticateJwt() {
        String token = JwtTestSupport.accessToken(42L, "hv42@gym-test.vn", UserRole.MEMBER);

        StompHeaderAccessor connectAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        connectAccessor.setLeaveMutable(true);
        connectAccessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<?> connectMsg = MessageBuilder.createMessage(new byte[0], connectAccessor.getMessageHeaders());
        interceptor.preSend(connectMsg, NOOP_CHANNEL);
        assertThat(connectAccessor.getUser()).isNotNull();
        assertThat(connectAccessor.getUser().getName()).isEqualTo("42");

        StompHeaderAccessor stomp12Accessor = StompHeaderAccessor.create(StompCommand.STOMP);
        stomp12Accessor.setLeaveMutable(true);
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
    @DisplayName("US-F10.5.1 A3: SUBSCRIBE chỉ cho phép /user/queue/notifications khi đã xác thực; SEND bị chặn")
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
}
