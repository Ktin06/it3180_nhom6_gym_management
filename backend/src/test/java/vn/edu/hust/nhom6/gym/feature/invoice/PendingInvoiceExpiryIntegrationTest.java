package vn.edu.hust.nhom6.gym.feature.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PackageInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PayInvoiceRequest;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PortalInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.PaymentMethod;
import vn.edu.hust.nhom6.gym.feature.invoice.service.InvoiceService;
import vn.edu.hust.nhom6.gym.feature.invoice.service.PortalInvoiceService;
import vn.edu.hust.nhom6.gym.feature.memberportal.PortalFixtures;
import vn.edu.hust.nhom6.gym.feature.memberportal.PortalFixtures.MemberAccount;

/**
- Tác vụ tự hủy hóa đơn chờ cổng hội viên quá {@code PENDING_INVOICE_TTL_DAYS} (BR-F7.5, UC-F7.4 A1, AC-F3.5.1-09).
- Tác vụ nền tắt trong kiểm thử; gọi thẳng {@link PortalInvoiceService#cancelExpired()} và đổi giờ bằng
- {@link MutableClock}.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class PendingInvoiceExpiryIntegrationTest {
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 11, 20, 9, 0);

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired PortalInvoiceService portalInvoices;
    @Autowired InvoiceService invoiceService;

    SaleFixtures f;
    PortalFixtures p;
    MemberAccount me;
    Long timePkg;
    Long receptionist;

    @BeforeEach
    void setUp() {
        clock.setVn(CREATED);
        f = new SaleFixtures(jdbc, tokens);
        f.reset();
        p = new PortalFixtures(jdbc);
        p.resetTtl();
        me = p.memberAccount("HV000501", "0912000501", f.cn01, "ACTIVE");
        timePkg = f.pkg("TIME_30", "TIME", null, 500_000, f.cn01, "ACTIVE");
        receptionist = f.staff("le-tan@example.com", UserRole.RECEPTIONIST, f.cn01);
    }

    @Test
    @DisplayName("TC-F7.4.1-03a: Chưa đủ 3 ngày thì giữ; đúng created_at + 3 ngày thì hóa đơn và gói PENDING thành CANCELLED")
    void cancelsPortalInvoiceExactlyAtTtl() {
        PortalInvoiceDto sale = buy(me);

        clock.setVn(CREATED.plusDays(3).minusMinutes(1));
        assertThat(portalInvoices.cancelExpired()).isZero();
        assertThat(invoiceStatus(sale.invoiceId())).isEqualTo("PENDING");

        clock.setVn(CREATED.plusDays(3));
        assertThat(portalInvoices.cancelExpired()).isEqualTo(1);
        assertThat(invoiceStatus(sale.invoiceId())).isEqualTo("CANCELLED");
        assertThat(membershipStatus(sale.membershipId())).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("TC-F7.4.1-03b: Chạy lặp lần hai không đổi thêm gì")
    void runningTwiceChangesNothingMore() {
        PortalInvoiceDto sale = buy(me);
        clock.setVn(CREATED.plusDays(4));
        assertThat(portalInvoices.cancelExpired()).isEqualTo(1);
        OffsetDateTime updatedAt = jdbc.queryForObject("SELECT updated_at FROM invoices WHERE id = ?",
            OffsetDateTime.class, sale.invoiceId());

        clock.advance(Duration.ofHours(1));
        assertThat(portalInvoices.cancelExpired()).isZero();
        assertThat(jdbc.queryForObject("SELECT updated_at FROM invoices WHERE id = ?", OffsetDateTime.class,
            sale.invoiceId())).isEqualTo(updatedAt);
        assertThat(membershipStatus(sale.membershipId())).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("TC-F3.5.1-09a: Hóa đơn COUNTER quá hạn giữ nguyên PENDING kèm gói; chỉ hóa đơn MEMBER_PORTAL bị hủy")
    void counterInvoiceIsNeverAutoCancelled() {
        MemberAccount walkIn = p.memberAccount("HV000502", "0912000502", f.cn01, "ACTIVE");
        PackageInvoiceDto counter = invoiceService.createPackageInvoice(receptionist, f.cn01, walkIn.memberId(),
            timePkg, InvoiceSource.COUNTER);
        PortalInvoiceDto portal = buy(me);

        clock.setVn(CREATED.plusDays(30));
        assertThat(portalInvoices.cancelExpired()).isEqualTo(1);

        assertThat(invoiceStatus(counter.invoiceId())).isEqualTo("PENDING");
        assertThat(membershipStatus(counter.membershipId())).isEqualTo("PENDING");
        assertThat(invoiceStatus(portal.invoiceId())).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("TC-F7.4.1-03c: Hóa đơn cổng đã thu tiền thì tác vụ không đụng; gói vẫn ACTIVE")
    void paidPortalInvoiceIsUntouched() {
        PortalInvoiceDto sale = buy(me);
        invoiceService.pay(new CurrentUser(receptionist, UserRole.RECEPTIONIST), sale.invoiceId(),
            new PayInvoiceRequest(PaymentMethod.CASH, null));

        clock.setVn(CREATED.plusDays(10));
        assertThat(portalInvoices.cancelExpired()).isZero();
        assertThat(invoiceStatus(sale.invoiceId())).isEqualTo("PAID");
        assertThat(membershipStatus(sale.membershipId())).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("TC-F7.4.1-04b: Hạn đọc từ system_configs theo chi nhánh (1 ngày ở CN01), không viết cứng")
    void ttlIsReadPerBranch() {
        p.branchTtl(f.cn01, 1);
        MemberAccount cn02Member = p.memberAccount("HV000503", "0912000503", f.cn02, "ACTIVE");
        Long cn02Pkg = f.pkg("TIME_CN02", "TIME", null, 400_000, f.cn02, "ACTIVE");
        PortalInvoiceDto cn01Sale = buy(me);
        PortalInvoiceDto cn02Sale = portalInvoices.createForMember(cn02Member.userId(), cn02Member.memberId(),
            f.cn02, cn02Pkg);

        clock.setVn(CREATED.plusDays(1));
        assertThat(portalInvoices.cancelExpired()).isEqualTo(1);
        assertThat(invoiceStatus(cn01Sale.invoiceId())).isEqualTo("CANCELLED");
        assertThat(invoiceStatus(cn02Sale.invoiceId())).isEqualTo("PENDING");

        clock.setVn(CREATED.plusDays(3));
        assertThat(portalInvoices.cancelExpired()).isEqualTo(1);
        assertThat(invoiceStatus(cn02Sale.invoiceId())).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("TC-F7.4.1-03d: Sau khi tự hủy, hội viên gửi lại yêu cầu mua cùng gói được (không còn 409 hay 422)")
    void canBuyAgainAfterAutoCancel() {
        buy(me);
        clock.setVn(CREATED.plusDays(3));
        portalInvoices.cancelExpired();
        PortalInvoiceDto again = buy(me);
        assertThat(invoiceStatus(again.invoiceId())).isEqualTo("PENDING");
        assertThat(membershipStatus(again.membershipId())).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("TC-F7.4.1-03e: Một hóa đơn lỗi dữ liệu (gói đi kèm không còn PENDING) bị bỏ qua, hóa đơn khác vẫn hủy")
    void brokenInvoiceDoesNotBlockOthers() {
        PortalInvoiceDto broken = buy(me);
        jdbc.update("UPDATE memberships SET status = 'CANCELLED' WHERE id = ?", broken.membershipId());
        clock.advance(Duration.ofMinutes(5));
        MemberAccount other = p.memberAccount("HV000504", "0912000504", f.cn01, "ACTIVE");
        PortalInvoiceDto healthy = buy(other);

        clock.setVn(CREATED.plusDays(5));
        assertThat(portalInvoices.cancelExpired()).isEqualTo(1);
        assertThat(invoiceStatus(broken.invoiceId())).isEqualTo("PENDING");
        assertThat(invoiceStatus(healthy.invoiceId())).isEqualTo("CANCELLED");
    }

    private PortalInvoiceDto buy(MemberAccount who) {
        return portalInvoices.createForMember(who.userId(), who.memberId(), f.cn01, timePkg);
    }

    private String invoiceStatus(Long id) {
        return jdbc.queryForObject("SELECT status FROM invoices WHERE id = ?", String.class, id);
    }

    private String membershipStatus(Long id) {
        return jdbc.queryForObject("SELECT status FROM memberships WHERE id = ?", String.class, id);
    }
}