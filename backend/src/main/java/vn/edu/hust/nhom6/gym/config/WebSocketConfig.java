package vn.edu.hust.nhom6.gym.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket (STOMP) cho thông báo thời gian thực (F10, ADR 0008).
 *
 * <p>Mở endpoint WebSocket thuần {@value #WS_ENDPOINT} (không dùng SockJS để tương thích trực tiếp với
 * {@code @stomp/stompjs}), giới hạn CORS theo {@code app.cors.allowed-origins} và gắn
 * {@link StompJwtChannelInterceptor} xác thực JWT ở khung mở kết nối STOMP.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    public static final String WS_ENDPOINT = "/ws";

    private final StompJwtChannelInterceptor stompJwtChannelInterceptor;
    private final List<String> allowedOrigins;

    public WebSocketConfig(
            StompJwtChannelInterceptor stompJwtChannelInterceptor,
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.stompJwtChannelInterceptor = stompJwtChannelInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOrigins.stream()
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toArray(String[]::new);
        registry.addEndpoint(WS_ENDPOINT)
            .setAllowedOrigins(origins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompJwtChannelInterceptor);
    }
}
