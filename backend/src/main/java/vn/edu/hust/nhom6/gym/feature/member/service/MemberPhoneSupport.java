package vn.edu.hust.nhom6.gym.feature.member.service;

import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;
import vn.edu.hust.nhom6.gym.common.util.PhoneUtils;

/** Thích ứng lỗi đầu vào và từ khóa của riêng F2; chuẩn hóa dùng tiện ích chung của F1. */
public final class MemberPhoneSupport {
    private MemberPhoneSupport() { }

    public static String normalize(String rawPhone) {
        try {
            return PhoneUtils.normalizePhone(rawPhone);
        } catch (BusinessRuleException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
    }

    /**
     * Từ khóa ngắn tìm theo tiền tố nguyên dạng; đủ độ dài thì chuẩn hóa.
     * Dãy số dài nhưng không phải số điện thoại hợp lệ cũng giữ nguyên dạng nên không khớp hồ sơ nào (trả trang rỗng).
     */
    public static String normalizeSearchPrefix(String keyword) {
        String cleaned = keyword == null ? "" : keyword.replaceAll("[\\s.\\-]", "");
        if (!cleaned.matches("\\+?[0-9]+")) { return normalize(keyword); }
        int fullLength = cleaned.startsWith("+84") ? 12 : cleaned.startsWith("84") ? 11 : 10;
        if (cleaned.length() < fullLength) { return cleaned; }
        try {
            return PhoneUtils.normalizePhone(cleaned);
        } catch (BusinessRuleException ex) {
            return cleaned;
        }
    }

    public static boolean isPhoneKeyword(String keyword) {
        return keyword != null && keyword.matches("[+0-9\\s.\\-]+") && keyword.matches(".*[0-9].*");
    }
}
