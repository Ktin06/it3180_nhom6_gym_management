package vn.edu.hust.nhom6.gym.feature.member;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberPhoneSupport;

class MemberPhoneSupportTest {
    @Test
    void usesSharedNormalizationButAdaptsF2InputError() {
        assertThat(MemberPhoneSupport.normalize("+84 912 345 678")).isEqualTo("0912345678");
        assertThatThrownBy(() -> MemberPhoneSupport.normalize("123"))
            .isInstanceOf(InvalidRequestException.class).hasMessageContaining("Số điện thoại");
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0", "09, 09", "0912, 0912", "0 912, 0912", "0912.3, 09123",
        "0912-345, 0912345", "091234567, 091234567", "0912345678, 0912345678",
        "+84 912 345 678, 0912345678", "84912345678, 0912345678",
        "123, 123", "5678, 5678", "56 78, 5678", "84912, 84912",
        "8491234567, 8491234567", "+84 912, +84912", "+84 912 345 67, +8491234567"
    })
    void preservesShortNumericKeywordsAndNormalizesCompletePhones(String keyword, String expected) {
        assertThat(MemberPhoneSupport.normalizeSearchPrefix(keyword)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "1234567890", "09123456789", "+85912345678", "09%", "09_"})
    void rejectsInvalidSearchPrefixes(String keyword) {
        assertThatThrownBy(() -> MemberPhoneSupport.normalizeSearchPrefix(keyword))
            .isInstanceOf(InvalidRequestException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"5678", "84912", "0912", "0912345678", "+84 912 345 678", "0912.345.678", "0912-345-678"})
    void detectsPhoneKeywords(String keyword) {
        assertThat(MemberPhoneSupport.isPhoneKeyword(keyword)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "HV000123", "Nguyễn An", "+"})
    void namesAndMemberCodesAreNotPhoneKeywords(String keyword) {
        assertThat(MemberPhoneSupport.isPhoneKeyword(keyword)).isFalse();
    }
}
