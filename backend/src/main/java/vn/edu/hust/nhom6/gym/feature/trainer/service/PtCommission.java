package vn.edu.hust.nhom6.gym.feature.trainer.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import vn.edu.hust.nhom6.gym.feature.trainer.repository.PtCommissionLookupRepository.CommissionInput;

/**
 * Hoa hồng một buổi PT (BR-F9.8, ERD {@code pt_sessions.commission_amount}):
 * {@code commission_rate × (amount − discount_amount) ÷ total_sessions}, làm tròn đến đồng.
 *
 * <p>Tính hết bằng {@link BigDecimal}, chỉ làm tròn <b>một lần</b> ở bước chia cuối, {@link RoundingMode#HALF_UP}
 * (0,5 đồng làm tròn lên). Không dùng {@code double}: ví dụ 0,1 × 1.000.050 ÷ 2 = 50.002,5 phải ra 50.003.
 */
public final class PtCommission {
    private PtCommission() {
    }

    /**
     * @return số tiền VND; 0 khi huấn luyện viên chưa có tỷ lệ hoặc gói không có dòng hóa đơn (GĐ-3 S2-19.3)
     * @throws IllegalStateException gói PT không có tổng số buổi dương (lỗi dữ liệu)
     */
    public static int calculate(CommissionInput in) {
        if (in.totalSessions() == null || in.totalSessions() <= 0) {
            throw new IllegalStateException("Gói PT không có tổng số buổi hợp lệ");
        }
        if (in.rate() == null || in.amount() == null) {
            return 0;
        }
        int discount = in.discountAmount() == null ? 0 : in.discountAmount();
        BigDecimal net = BigDecimal.valueOf((long) in.amount() - discount);
        return in.rate().multiply(net)
            .divide(BigDecimal.valueOf(in.totalSessions()), 0, RoundingMode.HALF_UP)
            .intValueExact();
    }
}
