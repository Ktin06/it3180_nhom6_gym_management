package vn.edu.hust.nhom6.gym.feature.notification.config;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;

/**
 * Cấu hình WebSocket (STOMP) đẩy thông báo trong web theo thời gian thực (F10, UC-F10.5).
 *
 * <p>Client kết nối tới {@code /ws}, gửi header {@code Authorization: Bearer <accessToken>} trong
 * khung STOMP {@code CONNECT} và đăng ký kênh {@code /user/queue/notifications}.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    public static final String WS_ENDPOINT = "/ws";
    public static final String USER_NOTIFICATIONS_DESTINATION = "/queue/notifications";
    private static final String ALLOWED_SUBSCRIBE_DESTINATION = "/user/queue/notifications";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(WS_ENDPOINT)
            .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    accessor.setUser(authenticate(accessor));
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    if (!ALLOWED_SUBSCRIBE_DESTINATION.equals(destination)) {
                        throw new AccessDeniedException(
                            "Chỉ được đăng ký kênh " + ALLOWED_SUBSCRIBE_DESTINATION);
                    }
                }
                return message;
            }
        });
    }

    private Principal authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("Thiếu JWT khi kết nối WebSocket.");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String role = jwt.getClaimAsString("role");
            if (!UserRole.MEMBER.name().equals(role)) {
                throw new AccessDeniedException("Chỉ hội viên được nhận thông báo trên kênh này.");
            }
            String userId = jwt.getSubject();
            return () -> userId;
        } catch (JwtException ex) {
            throw new AccessDeniedException("JWT WebSocket không hợp lệ hoặc đã hết hạn.", ex);
        }
    }
}
