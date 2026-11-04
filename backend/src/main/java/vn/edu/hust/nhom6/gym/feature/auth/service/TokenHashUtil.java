package vn.edu.hust.nhom6.gym.feature.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tiện ích sinh chuỗi ngẫu nhiên an toàn và băm SHA-256 cho Refresh Token (F1, S2, ADR 0008).
 */
public final class TokenHashUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenHashUtil() {
    }

    /** Sinh chuỗi token ngẫu nhiên bảo mật độ dài 32 byte mã hóa Base64 URL không đệm. */
    public static String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Băm SHA-256 chuỗi token đầu vào thành chuỗi hex thường. */
    public static String sha256Hex(String rawToken) {
        if (rawToken == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không tìm thấy thuật toán SHA-256 trong JVM", e);
        }
    }
}
