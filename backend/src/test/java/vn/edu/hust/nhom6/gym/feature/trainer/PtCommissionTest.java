package vn.edu.hust.nhom6.gym.feature.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import vn.edu.hust.nhom6.gym.feature.trainer.repository.PtCommissionLookupRepository.CommissionInput;
import vn.edu.hust.nhom6.gym.feature.trainer.service.PtCommission;

/** Công thức hoa hồng buổi PT (BR-F9.8, ERD {@code pt_sessions.commission_amount}; S2-19.3). */
class PtCommissionTest {

    static CommissionInput in(String rate, Integer amount, Integer discount, Integer total) {
        return new CommissionInput(rate == null ? null : new BigDecimal(rate), amount, discount, total);
    }

    @ParameterizedTest(name = "{0} × ({1} − {2}) ÷ {3} = {4}")
    @CsvSource({
        "0.1000, 5000000, 500000, 10, 45000",
        "0.1000, 1000050, 0, 2, 50003",      // 50.002,5 → HALF_UP lên 50.003 (HALF_EVEN sẽ ra 50.002)
        "0.1000, 1000030, 0, 2, 50002",      // 50.001,5 → 50.002
        "0.1000, 1000010, 0, 4, 25000",      // 25.000,25 → 25.000
        "0.1500, 3000000, 1000000, 12, 25000",
        "0.1234, 4999999, 0, 7, 88143",      // 88.142,84… → 88.143, làm tròn một lần ở cuối
        "1.0000, 2000000, 2000000, 10, 0",    // giảm hết giá
        "0.0000, 2000000, 0, 10, 0"
    })
    @DisplayName("TC-F9.3.1-02a: hoa hồng = tỷ lệ × (giá sau giảm ÷ tổng buổi), BigDecimal, HALF_UP về đồng")
    void formula(String rate, int amount, int discount, int total, int expected) {
        assertThat(PtCommission.calculate(in(rate, amount, discount, total))).isEqualTo(expected);
    }

    @Test
    @DisplayName("TC-F9.3.1-02b: chưa có tỷ lệ hoặc gói không có dòng hóa đơn → 0 (GĐ-3)")
    void missingDataGivesZero() {
        assertThat(PtCommission.calculate(in(null, 5_000_000, 0, 10))).isZero();
        assertThat(PtCommission.calculate(in("0.1000", null, null, 10))).isZero();
    }

    @Test
    @DisplayName("TC-F9.3.1-02c: gói PT không có tổng số buổi dương → lỗi dữ liệu, không chia cho 0")
    void invalidTotalSessions() {
        assertThatThrownBy(() -> PtCommission.calculate(in("0.1000", 1_000_000, 0, 0)))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> PtCommission.calculate(in("0.1000", 1_000_000, 0, null)))
            .isInstanceOf(IllegalStateException.class);
    }
}
