package vn.edu.hust.nhom6.gym.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;

class PhoneUtilsTest {
    @ParameterizedTest
    @ValueSource(strings = {"0912345678", "+84 912 345 678", "84912345678", "0912-345-678", "0912.345.678"})
    void alwaysReturnsTenDigitsStartingWithZero(String input) {
        assertThat(PhoneUtils.normalizePhone(input)).isEqualTo("0912345678");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "912345678", "091234567", "09123456789", "09123abc78", "+1 912345678", "0１２３４５６７８９"})
    void rejectsMalformedPhone(String input) {
        assertThatThrownBy(() -> PhoneUtils.normalizePhone(input)).isInstanceOf(BusinessRuleException.class);
    }
}
