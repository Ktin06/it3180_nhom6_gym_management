package vn.edu.hust.nhom6.gym.config;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
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
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.common.exception.UnauthorizedException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;

/**
 * Xác thực JWT ở khung mở kết nối STOMP ({@code CONNECT} và {@code STOMP} của STOMP 1.2), lưu hạn dùng
 * {@code exp} vào phiên để kiểm tra ở các khung về sau (cả chiều vào và chiều đẩy tin ra) và đóng kết nối
 * ngay khi token quá hạn (F10, ADR 0008).
 *
 * <ul>
 *   <li>{@code CONNECT} / {@code STOMP} ({@link SimpMessageType#CONNECT}): đọc native header
 *       {@code Authorization: Bearer <access_token>}, xác thực chữ ký HS256, hạn dùng và claim {@code sub}/{@code role}
 *       bằng {@link JwtDecoder} dùng chung với REST API, gán {@code Principal} có tên là {@code String.valueOf(userId)}
 *       và lưu {@code exp} vào phiên ({@value #SESSION_EXPIRES_AT}).</li>
 *   <li>{@code SUBSCRIBE}: chỉ cho phép người đã xác thực và token chưa hết hạn đăng ký đúng
 *       {@value #USER_NOTIFICATIONS_DESTINATION}.</li>
 *   <li>{@code MESSAGE} (chiều đẩy ra) và các khung sau khi kết nối: nếu quá {@code exp} thì chặn tin và đóng
 *       {@link WebSocketSession} với {@link CloseStatus#POLICY_VIOLATION}.</li>
 *   <li>{@code SEND}: từ chối vì kênh thông báo chỉ đẩy một chiều từ máy chủ tới trình duyệt.</li>
 * </ul>
 */
@Slf4j
@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {
    public static final String USER_NOTIFICATIONS_DESTINATION = "/user/queue/notifications";
    public static final String SESSION_EXPIRES_AT = "stompJwtExpiresAt";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final Clock clock;
    private final ConcurrentMap<String, Instant> sessionExpirations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    @Autowired
    public StompJwtChannelInterceptor(
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter) {
        this(jwtDecoder, jwtAuthenticationConverter, Clock.systemUTC());
    }

    public StompJwtChannelInterceptor(
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            Clock clock) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.clock = clock;
    }

    /** Ghi nhận kết nối WebSocket đang mở để có thể chủ động đóng khi JWT hết hạn. */
    public void registerSession(WebSocketSession session) {
        if (session != null && session.getId() != null) {
            activeSessions.put(session.getId(), session);
        }
    }

    /** Xóa thông tin phiên khi kết nối WebSocket đóng. */
    public void unregisterSession(String sessionId) {
        if (sessionId != null) {
            activeSessions.remove(sessionId);
            sessionExpirations.remove(sessionId);
        }
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
        if (accessor == null) {
            accessor = SimpMessageHeaderAccessor.wrap(message);
        }
        StompHeaderAccessor stomp = accessor instanceof StompHeaderAccessor s ? s : null;
        SimpMessageType messageType = accessor.getMessageType();

        if (stomp != null && stomp.getCommand() == StompCommand.SEND) {
            throw new MessageDeliveryException(message, "Kênh thông báo chỉ hỗ trợ nhận tin từ máy chủ.");
        }
        if (messageType == SimpMessageType.CONNECT && stomp != null) {
            AuthenticatedSession authenticated = authenticate(message, stomp);
            stomp.setUser(authenticated.authentication());
            Map<String, Object> attrs = stomp.getSessionAttributes();
            if (attrs != null) {
                attrs.put(SESSION_EXPIRES_AT, authenticated.expiresAt());
            }
            if (stomp.getSessionId() != null) {
                sessionExpirations.put(stomp.getSessionId(), authenticated.expiresAt());
            }
            return message;
        }
        if (messageType == SimpMessageType.DISCONNECT) {
            if (accessor.getSessionId() != null) {
                sessionExpirations.remove(accessor.getSessionId());
            }
            return message;
        }
        if (messageType == SimpMessageType.MESSAGE) {
            if (isSessionExpired(accessor)) {
                closeExpiredSession(accessor.getSessionId());
                return null;
            }
            return message;
        }
        if (messageType == SimpMessageType.SUBSCRIBE) {
            if (accessor.getUser() == null) {
                throw new MessageDeliveryException(message, "Bạn cần xác thực trước khi đăng ký kênh thông báo.");
            }
            if (isSessionExpired(accessor)) {
                closeExpiredSession(accessor.getSessionId());
                throw new MessageDeliveryException(message, "Mã xác thực WebSocket đã hết hạn.");
            }
            if (!USER_NOTIFICATIONS_DESTINATION.equals(accessor.getDestination())) {
                throw new MessageDeliveryException(message, "Không được phép đăng ký kênh này.");
            }
            return message;
        }
        if (isSessionExpired(accessor)) {
            closeExpiredSession(accessor.getSessionId());
            if (messageType == SimpMessageType.HEARTBEAT) {
                return null;
            }
            throw new MessageDeliveryException(message, "Mã xác thực WebSocket đã hết hạn.");
        }
        return message;
    }

    private boolean isSessionExpired(SimpMessageHeaderAccessor accessor) {
        Instant expiresAt = null;
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null && attrs.get(SESSION_EXPIRES_AT) instanceof Instant attrExp) {
            expiresAt = attrExp;
        }
        if (expiresAt == null && accessor.getSessionId() != null) {
            expiresAt = sessionExpirations.get(accessor.getSessionId());
        }
        return expiresAt != null && !clock.instant().isBefore(expiresAt);
    }

    private void closeExpiredSession(String sessionId) {
        if (sessionId == null) {
            return;
        }
        sessionExpirations.remove(sessionId);
        WebSocketSession session = activeSessions.remove(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Mã xác thực WebSocket đã hết hạn."));
            } catch (IOException ex) {
                log.debug("Không đóng được phiên WebSocket {} đã hết hạn", sessionId, ex);
            }
        }
    }

    private AuthenticatedSession authenticate(Message<?> message, StompHeaderAccessor accessor) {
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
            Instant expiresAt = jwt.getExpiresAt();
            if (expiresAt == null || !clock.instant().isBefore(expiresAt)) {
                throw new MessageDeliveryException(message,
                    "Mã xác thực WebSocket không hợp lệ hoặc đã hết hạn.");
            }
            AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);
            CurrentUser.from(authentication);
            return new AuthenticatedSession(authentication, expiresAt);
        } catch (JwtException | UnauthorizedException ex) {
            throw new MessageDeliveryException(message,
                "Mã xác thực WebSocket không hợp lệ hoặc đã hết hạn.", ex);
        }
    }

    private record AuthenticatedSession(AbstractAuthenticationToken authentication, Instant expiresAt) { }
}
