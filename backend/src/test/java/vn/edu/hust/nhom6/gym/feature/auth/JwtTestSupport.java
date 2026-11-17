package vn.edu.hust.nhom6.gym.feature.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;

/**
 * Tiện ích tạo JWT Access Token phục vụ kiểm thử tích hợp của các chức năng (F1, F2, F3, ...).
 *
 * <p>Cách dùng trong kiểm thử {@code MockMvc}:
 * <pre>{@code
 * mockMvc.perform(get("/api/v1/members")
 *     .header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearerToken(UserRole.RECEPTIONIST)))
 * }</pre>
 */
public final class JwtTestSupport {

    /** Khóa bí mật dành riêng cho kiểm thử trong {@code src/test/resources/config/application.yml}. */
    public static final String DEFAULT_TEST_SECRET = "khoa-bi-mat-kiem-thu-jwt-rieng-biet-toi-thieu-32-byte";

    private static final JwtTokenService DEFAULT_TOKEN_SERVICE = createTokenService(DEFAULT_TEST_SECRET);

    private JwtTestSupport() {
    }

    /** Tạo chuỗi {@code "Bearer <jwt>"} với {@code userId = 1L} và email mặc định theo vai trò. */
    public static String bearerToken(UserRole role) {
        return bearerToken(1L, role.name().toLowerCase() + "@example.com", role);
    }

    /** Tạo chuỗi {@code "Bearer <jwt>"} với {@code userId}, {@code email} và {@code role} tùy chỉnh. */
    public static String bearerToken(Long userId, String email, UserRole role) {
        return "Bearer " + accessToken(userId, email, role);
    }

    /** Tạo JWT Access Token (hạn 15 phút) ký bằng khóa kiểm thử mặc định. */
    public static String accessToken(Long userId, String email, UserRole role) {
        return DEFAULT_TOKEN_SERVICE.generateAccessToken(userId, email, role);
    }

    /** Tạo JWT Access Token đã hết hạn để kiểm thử trường hợp token quá hạn. */
    public static String expiredAccessToken(Long userId, String email, UserRole role) {
        Instant pastIssuedAt = Instant.now().minus(JwtTokenService.ACCESS_TOKEN_TTL).minusSeconds(120);
        return DEFAULT_TOKEN_SERVICE.generateAccessToken(userId, email, role, pastIssuedAt);
    }

    /** Tạo JWT Access Token ký bằng một khóa bí mật khác để kiểm thử trường hợp sai chữ ký. */
    public static String accessTokenWithSecret(Long userId, String email, UserRole role, String secret) {
        return createTokenService(secret).generateAccessToken(userId, email, role);
    }

    /** Tạo JWT thiếu claim {@code role} để kiểm thử trường hợp token không có quyền. */
    public static String accessTokenWithoutRole(Long userId, String email) {
        return accessTokenWithCustomClaims(String.valueOf(userId), email, null);
    }

    /** Tạo JWT với {@code sub} và {@code role} tùy ý (dùng để kiểm thử vé lạ/sai định dạng claim). */
    public static String accessTokenWithCustomClaims(String subject, String email, String role) {
        SecretKey key = new SecretKeySpec(DEFAULT_TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        Instant now = Instant.now();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(now.plus(JwtTokenService.ACCESS_TOKEN_TTL));
        if (subject != null) {
            builder.subject(subject);
        }
        if (email != null) {
            builder.claim("email", email);
        }
        if (role != null) {
            builder.claim("role", role);
        }
        return encoder.encode(JwtEncoderParameters.from(header, builder.build())).getTokenValue();
    }

    public static String authorizationHeaderName() {
        return HttpHeaders.AUTHORIZATION;
    }

    private static JwtTokenService createTokenService(String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new JwtTokenService(new NimbusJwtEncoder(new ImmutableSecret<>(key)));
    }
}
