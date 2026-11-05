package vn.edu.hust.nhom6.gym.feature.member;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
    @ValueSource(strings = {"0912345678", "+84 912 345 678", "0912.345.678", "0912-345-678"})
    void detectsPhoneKeywords(String keyword) {
        assertThat(MemberPhoneSupport.isPhoneKeyword(keyword)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "HV000123", "Nguyễn An", "+"})
    void namesAndMemberCodesAreNotPhoneKeywords(String keyword) {
        assertThat(MemberPhoneSupport.isPhoneKeyword(keyword)).isFalse();
    }
}
