package vn.edu.hust.nhom6.gym.common.util;

import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;

/** Dạng lưu và tìm kiếm duy nhất: 10 chữ số bắt đầu bằng 0. */
public final class PhoneUtils {
    private PhoneUtils() { }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidRequestException("Số điện thoại không được để trống.");
        }
        String phone = raw.replaceAll("[\\s.\\-]", "");
        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        } else if (phone.startsWith("84") && phone.length() == 11) {
            phone = "0" + phone.substring(2);
        }
        if (!phone.matches("^0[0-9]{9}$")) {
            throw new InvalidRequestException("Số điện thoại không hợp lệ. Phải gồm 10 chữ số bắt đầu bằng số 0.");
        }
        return phone;
    }

    public static boolean isPhoneKeyword(String keyword) {
        return keyword.matches("[+0-9\\s.\\-]+") && keyword.matches(".*[0-9].*");
    }
}
