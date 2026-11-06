package vn.edu.hust.nhom6.gym.config;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cấu hình bảo mật cho hệ thống (F1 – Tài khoản và phân quyền).
 *
 * <p>Mở các đường dẫn công khai (health, Swagger, các API xác thực công khai của {@code /api/v1/auth});
 * mọi đường dẫn khác yêu cầu xác thực bằng JWT HS256 qua {@code spring-boot-starter-oauth2-resource-server}
 * và phân quyền theo vai trò {@code ROLE_<vai trò>} (S2, S3, ADR 0001).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final int MIN_JWT_SECRET_BYTES = 32;

    private static final String[] PUBLIC_PATHS = {
        "/actuator/health",
        "/actuator/health/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/error",
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout",
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper,
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter) throws Exception {
        AuthenticationEntryPoint entryPoint = (request, response, e) -> writeProblem(
            jsonMapper, request, response, HttpStatus.UNAUTHORIZED,
            "Chưa xác thực", "Bạn cần đăng nhập để truy cập tài nguyên này.");
        AccessDeniedHandler accessDeniedHandler = (request, response, e) -> writeProblem(
            jsonMapper, request, response, HttpStatus.FORBIDDEN,
            "Không có quyền", "Bạn không có quyền thực hiện thao tác này.");

        http
            // Access token (JWT) gửi qua header Authorization nên tắt CSRF. Riêng cookie refresh token
            // là cookie cùng domain với SameSite=Strict (ADR 0008), trình duyệt không gửi kèm request từ trang khác.
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    /** Kiểm tra chữ ký JWT HS256 và hạn dùng bằng khóa {@code JWT_SECRET} (ADR 0001, S2). */
    @Bean
    JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String jwtSecret) {
        return NimbusJwtDecoder.withSecretKey(hmacSha256Key(jwtSecret))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }

    /** Ký JWT HS256 bằng khóa {@code JWT_SECRET}. */
    @Bean
    JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String jwtSecret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(hmacSha256Key(jwtSecret)));
    }

    /** Chuyển claim {@code role} trong JWT thành quyền {@code ROLE_<vai trò>} của Spring Security (S3). */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::extractRoleAuthorities);
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Cho phép gửi cookie httpOnly chứa refresh token (S2).
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Băm mật khẩu bằng bcrypt (S1). */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static SecretKey hmacSha256Key(String jwtSecret) {
        byte[] keyBytes = jwtSecret == null ? new byte[0] : jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_JWT_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET phải dài ít nhất 32 byte cho thuật toán HS256.");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    private static Collection<GrantedAuthority> extractRoleAuthorities(Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        if (role == null || role.isBlank()) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.trim()));
    }

    private static void writeProblem(JsonMapper jsonMapper, HttpServletRequest request,
            HttpServletResponse response, HttpStatus status, String title, String detail)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
