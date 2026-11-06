package vn.edu.hust.nhom6.gym.feature.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import vn.edu.hust.nhom6.gym.config.SecurityConfig;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;

/** Kiểm thử đơn vị cho tạo/giải mã JWT HS256 và chuyển đổi claim {@code role} sang {@code ROLE_<vai trò>}. */
class JwtTokenServiceTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void taoVaGiaiMaJwtHs256ChuaDuSubEmailRoleVaHan15Phut() {
        SecretKey key = new SecretKeySpec(
            JwtTestSupport.DEFAULT_TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTokenService service = new JwtTokenService(encoder);

        Instant issuedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String token = service.generateAccessToken(42L, "reception@example.com", UserRole.RECEPTIONIST, issuedAt);

        Jwt jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("reception@example.com");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("RECEPTIONIST");
        assertThat(jwt.getExpiresAt()).isEqualTo(issuedAt.plusSeconds(900));
    }

    @Test
    void chuyenClaimRoleThanhQuyenRolePrefixCho5VaiTro() {
        SecretKey key = new SecretKeySpec(
            JwtTestSupport.DEFAULT_TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null || role.isBlank()) {
                return java.util.List.of();
            }
            return java.util.List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.trim()));
        });

        for (UserRole role : UserRole.values()) {
            String token = JwtTestSupport.accessToken(10L, "user@example.com", role);
            Jwt jwt = decoder.decode(token);
            JwtAuthenticationToken authToken = (JwtAuthenticationToken) converter.convert(jwt);

            assertThat(authToken).isNotNull();
            assertThat(authToken.getName()).isEqualTo("10");
            assertThat(authToken.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_" + role.name());
        }
    }

    @Test
    void tokenHetHanBiJwtDecoderTuChoi() {
        SecretKey key = new SecretKeySpec(
            JwtTestSupport.DEFAULT_TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();

        String expiredToken = JwtTestSupport.expiredAccessToken(1L, "member@example.com", UserRole.MEMBER);

        assertThatThrownBy(() -> decoder.decode(expiredToken))
            .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void jwtSecretNganHon32ByteBiTuChoiKhiKhoiTao() {
        assertThatThrownBy(() -> {
            var method = SecurityConfig.class.getDeclaredMethod("hmacSha256Key", String.class);
            method.setAccessible(true);
            try {
                method.invoke(securityConfig, "khoa-qua-ngan");
            } catch (java.lang.reflect.InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32 byte");
    }
}
