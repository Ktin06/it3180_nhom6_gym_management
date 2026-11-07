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

    /** Tìm theo tiền tố nội địa đầu 0; số đầy đủ vẫn dùng chuẩn hóa chung của F1. */
    public static String normalizeSearchPrefix(String keyword) {
        String cleaned = keyword == null ? "" : keyword.replaceAll("[\\s.\\-]", "");
        if (cleaned.matches("0[0-9]{0,8}")) { return cleaned; }
        return normalize(keyword);
    }

    public static boolean isPhoneKeyword(String keyword) {
        return keyword != null && keyword.matches("[+0-9\\s.\\-]+") && keyword.matches(".*[0-9].*");
    }
}
