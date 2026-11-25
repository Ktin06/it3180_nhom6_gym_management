package vn.edu.hust.nhom6.gym.feature.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vn.edu.hust.nhom6.gym.feature.membership.entity.Membership;
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipService;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/** Hàm tính ngày bắt đầu dự kiến (BR-F3-11), dùng chung cho bán gói và {@code renewalStartDate}. */
class MembershipServiceTest {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    @Test
    @DisplayName("TC-F3.5.1-03b: chưa có gói cùng loại thì bắt đầu hôm nay; gói TIME còn hạn thì ngày sau end_date")
    void newOrTimeRenewal() {
        assertThat(MembershipService.plannedStartDate(null, TODAY)).isEqualTo(TODAY);
        assertThat(MembershipService.plannedStartDate(held(PackageType.TIME, LocalDate.of(2026, 12, 31), null), TODAY))
            .isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    @DisplayName("TC-F3.5.1-04c: gói SESSION 0 buổi thì ngày mai; còn buổi thì ngày sau end_date")
    void sessionRenewal() {
        assertThat(MembershipService.plannedStartDate(held(PackageType.SESSION, LocalDate.of(2026, 11, 30), 0), TODAY))
            .isEqualTo(TODAY.plusDays(1));
        assertThat(MembershipService.plannedStartDate(held(PackageType.SESSION, LocalDate.of(2026, 11, 30), 2), TODAY))
            .isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    @DisplayName("TC-F3.5.1-03c: gói PT còn buổi thì gia hạn nối tiếp sau end_date")
    void ptRenewalWithSessionsLeft() {
        assertThat(MembershipService.plannedStartDate(held(PackageType.PT, LocalDate.of(2026, 11, 30), 2), TODAY))
            .isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    @DisplayName("TC-F3.5.1-04d: ngoại lệ bắt đầu ngày mai của SESSION hết buổi không áp dụng cho PT")
    void emptyPtDoesNotUseSessionTomorrowRule() {
        // Dựng riêng đầu vào hàm thuần để kiểm tra ranh giới loại gói.
        // Trong luồng PT hợp lệ, buổi cuối được xác nhận thì gói đã chuyển EXPIRED ngay.
        assertThat(MembershipService.plannedStartDate(held(PackageType.PT, LocalDate.of(2026, 11, 30), 0), TODAY))
            .isNotEqualTo(TODAY.plusDays(1));
    }

    @Test
    @DisplayName("TC-F3.5.1-12e: gói cũ đã quá end_date mà tác vụ nền chưa chạy thì không bắt đầu trước hôm nay")
    void neverBeforeToday() {
        assertThat(MembershipService.plannedStartDate(held(PackageType.TIME, TODAY.minusDays(3), null), TODAY))
            .isEqualTo(TODAY);
    }

    /** Entity không có setter; giả lập loại gói, ngày cuối và số buổi cho hàm tính ngày. */
    private static Membership held(PackageType type, LocalDate end, Integer remaining) {
        Membership m = mock(Membership.class);
        when(m.getPackageType()).thenReturn(type);
        when(m.getEndDate()).thenReturn(end);
        when(m.getRemainingSessions()).thenReturn(remaining);
        return m;
    }
}
