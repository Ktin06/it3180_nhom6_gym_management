package vn.edu.hust.nhom6.gym.common.util;

import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;

/**
 * Tiện ích chuẩn hóa số điện thoại dùng chung cho toàn bộ hệ thống (F1, F2, F4...).
 *
 * <p>Chuẩn hóa số điện thoại về đúng 10 chữ số bắt đầu bằng số 0 theo ràng buộc CSDL {@code ^0[0-9]{9}$}.
 */
public final class PhoneUtils {

    private PhoneUtils() {
    }

    /**
     * Chuẩn hóa số điện thoại về 10 chữ số bắt đầu bằng số 0 (ví dụ {@code +84 912 345 678} thành {@code 0912345678}).
     */
    public static String normalizePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new BusinessRuleException("Số điện thoại không được để trống.");
        }
        String cleaned = rawPhone.replaceAll("[\\s\\-\\.]", "");
        if (cleaned.startsWith("+84")) {
            cleaned = "0" + cleaned.substring(3);
        } else if (cleaned.startsWith("84") && cleaned.length() == 11) {
            cleaned = "0" + cleaned.substring(2);
        }
        if (!cleaned.matches("^0\\d{9}$")) {
            throw new BusinessRuleException("Số điện thoại không hợp lệ. Phải gồm 10 chữ số bắt đầu bằng số 0.");
        }
        return cleaned;
    }
}
