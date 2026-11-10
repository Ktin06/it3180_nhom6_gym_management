package vn.edu.hust.nhom6.gym.feature.auth.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;

/**
 * Sinh JWT Access Token (HS256, thời hạn 15 phút) chứa {@code sub} (id tài khoản), {@code email} và {@code role}.
 */
@Service
public class JwtTokenService {

    public static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    private final JwtEncoder jwtEncoder;

    public JwtTokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    /** Tạo Access Token có hạn 15 phút cho người dùng đã xác thực. */
    public String generateAccessToken(Long userId, String email, UserRole role) {
        return generateAccessToken(userId, email, role, Instant.now());
    }

    /** Tạo Access Token với mốc phát hành tùy chỉnh (dùng trong kiểm thử thời hạn token). */
    public String generateAccessToken(Long userId, String email, UserRole role, Instant issuedAt) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(String.valueOf(userId))
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(ACCESS_TOKEN_TTL))
            .claim("email", email)
            .claim("role", role.name())
            .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
