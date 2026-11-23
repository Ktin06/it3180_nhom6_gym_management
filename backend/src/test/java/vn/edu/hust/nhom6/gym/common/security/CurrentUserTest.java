package vn.edu.hust.nhom6.gym.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import vn.edu.hust.nhom6.gym.common.exception.UnauthorizedException;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;

class CurrentUserTest {

    @Test
    @DisplayName("Nền tảng: JWT hợp lệ cho ra id tài khoản và vai trò")
    void readsUserIdAndRoleFromJwt() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject("42").claim("role", "RECEPTIONIST").build();
        var auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_RECEPTIONIST")));

        CurrentUser me = CurrentUser.from(auth);

        assertThat(me.userId()).isEqualTo(42L);
        assertThat(me.role()).isEqualTo(UserRole.RECEPTIONIST);
        assertThat(me.is(UserRole.RECEPTIONIST)).isTrue();
        assertThat(me.isOwner()).isFalse();
    }

    @Test
    @DisplayName("Nền tảng: token thiếu sub thì 401")
    void missingSubjectIsUnauthorized() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").claim("role", "MEMBER").build();
        var auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));

        assertThatThrownBy(() -> CurrentUser.from(auth))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Thông tin xác thực không hợp lệ.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", ""})
    @DisplayName("Nền tảng: sub không phải số dương thì 401")
    void invalidSubjectIsUnauthorized(String subject) {
        var auth = authenticated(subject, "ROLE_MEMBER");

        assertThatThrownBy(() -> CurrentUser.from(auth)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Nền tảng: chưa đăng nhập hoặc thiếu vai trò thì 401")
    void missingAuthenticationOrRoleIsUnauthorized() {
        assertThatThrownBy(() -> CurrentUser.from(null)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> CurrentUser.from(new UsernamePasswordAuthenticationToken("5", "x")))
            .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> CurrentUser.from(authenticated("5", "SCOPE_read")))
            .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> CurrentUser.from(authenticated("5", "ROLE_HACKER")))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Nền tảng: hai vai trò khác nhau trong một token thì 401")
    void twoDifferentRolesAreUnauthorized() {
        assertThatThrownBy(() -> CurrentUser.from(authenticated("5", "ROLE_MEMBER", "ROLE_SUPER_ADMIN")))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Nền tảng: chủ chuỗi là SUPER_ADMIN")
    void ownerIsSuperAdmin() {
        assertThat(CurrentUser.from(authenticated("1", "ROLE_SUPER_ADMIN")).isOwner()).isTrue();
    }

    private static TestingAuthenticationToken authenticated(String name, String... authorities) {
        var token = new TestingAuthenticationToken(name, "x", authorities);
        token.setAuthenticated(true);
        return token;
    }
}
