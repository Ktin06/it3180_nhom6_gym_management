package vn.edu.hust.nhom6.gym.feature.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import vn.edu.hust.nhom6.gym.config.SecurityConfig;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;

/** Kiểm thử đơn vị cho tạo/giải mã JWT HS256 và chuyển đổi claim {@code role} sang {@code ROLE_<vai trò>}. */
class JwtTokenServiceTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void taoVaGiaiMaJwtHs256ChuaDuSubEmailRoleVaHan15Phut() {
        JwtEncoder encoder = securityConfig.jwtEncoder(JwtTestSupport.DEFAULT_TEST_SECRET);
        JwtDecoder decoder = securityConfig.jwtDecoder(JwtTestSupport.DEFAULT_TEST_SECRET);
        JwtTokenService service = new JwtTokenService(encoder);

        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String token = service.generateAccessToken(42L, "reception@example.com", UserRole.RECEPTIONIST, issuedAt);

        Jwt jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("reception@example.com");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("RECEPTIONIST");
        assertThat(jwt.getExpiresAt()).isEqualTo(issuedAt.plusSeconds(900));
    }

    @Test
    void chuyenClaimRoleThanhQuyenRolePrefixCho5VaiTro() {
        JwtDecoder decoder = securityConfig.jwtDecoder(JwtTestSupport.DEFAULT_TEST_SECRET);
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

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
        JwtDecoder decoder = securityConfig.jwtDecoder(JwtTestSupport.DEFAULT_TEST_SECRET);
        String expiredToken = JwtTestSupport.expiredAccessToken(1L, "member@example.com", UserRole.MEMBER);

        assertThatThrownBy(() -> decoder.decode(expiredToken))
            .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void tokenThieuRoleHoacRoleLaHoacSubKhongPhaiSoBiJwtDecoderTuChoi() {
        JwtDecoder decoder = securityConfig.jwtDecoder(JwtTestSupport.DEFAULT_TEST_SECRET);

        String missingRoleToken = JwtTestSupport.accessTokenWithoutRole(1L, "member@example.com");
        assertThatThrownBy(() -> decoder.decode(missingRoleToken))
            .isInstanceOf(JwtValidationException.class);

        String invalidRoleToken = JwtTestSupport.accessTokenWithCustomClaims("1", "hacker@example.com", "HACKER");
        assertThatThrownBy(() -> decoder.decode(invalidRoleToken))
            .isInstanceOf(JwtValidationException.class);

        String invalidSubToken = JwtTestSupport.accessTokenWithCustomClaims("not-a-number", "member@example.com", "MEMBER");
        assertThatThrownBy(() -> decoder.decode(invalidSubToken))
            .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void jwtSecretNganHon32ByteBiTuChoiKhiKhoiTao() {
        assertThatThrownBy(() -> securityConfig.jwtDecoder("khoa-qua-ngan"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32 byte");

        assertThatThrownBy(() -> securityConfig.jwtEncoder("khoa-qua-ngan"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32 byte");
    }
}
