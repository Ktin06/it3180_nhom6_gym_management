package vn.edu.hust.nhom6.gym.config;

import java.util.Optional;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Đọc người dùng từ access token gửi kèm khi đăng xuất (#112).
 *
 * <p>Kiểm tra chữ ký và claim {@code sub}/{@code role} như mọi API khác, nhưng <b>bỏ qua hạn dùng</b>.
 * Lý do: người dùng để tab lâu thì access token trong tab đã hết hạn, nhưng backend vẫn cần biết tab đó
 * của ai để không thu hồi phiên của tài khoản khác đang nằm trong cookie dùng chung.
 *
 * <p>Bỏ qua hạn dùng là an toàn vì kết quả chỉ dùng để <i>thu hẹp</i> việc thu hồi refresh token
 * (chỉ thu hồi khi token trong cookie thuộc đúng người này), không cấp thêm quyền nào.
 * Token sai chữ ký hoặc sai định dạng thì coi như không gửi kèm.
 */
public class LogoutTokenDecoder {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder decoder;

    LogoutTokenDecoder(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    /**
     * @param authorizationHeader giá trị header {@code Authorization}, có thể {@code null}
     * @return id người dùng trong token nếu chữ ký hợp lệ, ngược lại rỗng
     */
    public Optional<Long> userIdOf(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(decoder.decode(token).getSubject()));
        } catch (JwtException | NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
