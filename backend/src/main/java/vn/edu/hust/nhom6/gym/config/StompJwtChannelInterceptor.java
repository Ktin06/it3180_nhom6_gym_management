package vn.edu.hust.nhom6.gym.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.exception.UnauthorizedException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;

/**
 * Xác thực JWT ở khung mở kết nối STOMP ({@code CONNECT} và {@code STOMP} của STOMP 1.2) và giới hạn kênh
 * {@code SUBSCRIBE} cho WebSocket thông báo (F10, ADR 0008).
 *
 * <ul>
 *   <li>{@code CONNECT} / {@code STOMP} ({@link SimpMessageType#CONNECT}): đọc native header
 *       {@code Authorization: Bearer <access_token>}, xác thực chữ ký HS256, hạn dùng và claim {@code sub}/{@code role}
 *       bằng {@link JwtDecoder} dùng chung với REST API, gán {@code Principal} có tên là {@code String.valueOf(userId)}.</li>
 *   <li>{@code SUBSCRIBE}: chỉ cho phép người đã xác thực đăng ký đúng {@value #USER_NOTIFICATIONS_DESTINATION}.</li>
 *   <li>{@code SEND}: từ chối vì kênh thông báo chỉ đẩy một chiều từ máy chủ tới trình duyệt.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {
    public static final String USER_NOTIFICATIONS_DESTINATION = "/user/queue/notifications";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (accessor.getMessageType() == SimpMessageType.CONNECT) {
            AbstractAuthenticationToken authentication = authenticate(message, accessor);
            accessor.setUser(authentication);
        } else if (command == StompCommand.SUBSCRIBE) {
            if (accessor.getUser() == null) {
                throw new MessageDeliveryException(message, "Bạn cần xác thực trước khi đăng ký kênh thông báo.");
            }
            if (!USER_NOTIFICATIONS_DESTINATION.equals(accessor.getDestination())) {
                throw new MessageDeliveryException(message, "Không được phép đăng ký kênh này.");
            }
        } else if (command == StompCommand.SEND) {
            throw new MessageDeliveryException(message, "Kênh thông báo chỉ hỗ trợ nhận tin từ máy chủ.");
        }
        return message;
    }

    private AbstractAuthenticationToken authenticate(Message<?> message, StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null) {
            header = accessor.getFirstNativeHeader("authorization");
        }
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new MessageDeliveryException(message,
                "Thiếu mã xác thực (Authorization Bearer) khi kết nối WebSocket.");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new MessageDeliveryException(message,
                "Thiếu mã xác thực (Authorization Bearer) khi kết nối WebSocket.");
        }
        try {
            Jwt jwt = jwtDecoder.decode(token);
            AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);
            CurrentUser.from(authentication);
            return authentication;
        } catch (JwtException | UnauthorizedException ex) {
            throw new MessageDeliveryException(message,
                "Mã xác thực WebSocket không hợp lệ hoặc đã hết hạn.", ex);
        }
    }
}
