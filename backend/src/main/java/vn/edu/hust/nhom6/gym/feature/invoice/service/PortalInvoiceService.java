package vn.edu.hust.nhom6.gym.feature.invoice.service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PackageInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PortalInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.Invoice;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceItemType;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.InvoiceRepository;
import vn.edu.hust.nhom6.gym.feature.member.entity.MemberStatus;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipService;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.service.PackageCatalogService;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigKeys;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigService;

/**
 * Hóa đơn chờ do hội viên tự tạo trên cổng (F7 UC-F7.4, UC-F7.5) và tác vụ hủy khi quá hạn (BR-F7.5, AC-F3.5.1-09).
 * Tách khỏi {@link InvoiceService} để hai việc của S2-14 nằm một chỗ; phần lập hóa đơn vẫn đi qua
 * {@link InvoiceService#createPackageInvoice}, phần hủy gói vẫn đi qua {@link MembershipService#cancelPending}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalInvoiceService {
    /** Không hứa "chờ tự hủy": hóa đơn chờ lập tại quầy cũng chặn mà không bao giờ tự hủy (AC-F3.5.1-09). */
    static final String DUPLICATE_PENDING =
        "Bạn đã có hóa đơn chờ thanh toán cho gói này. Vui lòng thanh toán hóa đơn đó tại quầy lễ tân.";
    private static final String MEMBER_INACTIVE = "Hội viên đã ngừng hoạt động, không thể bán gói tập.";

    private final InvoiceRepository invoices;
    private final InvoiceService invoiceService;
    private final MemberService members;
    private final PackageCatalogService packages;
    private final MembershipService memberships;
    private final SystemConfigService configs;
    private final TransactionTemplate transactions;
    private final VnTime time;

    /**
     * Lập hóa đơn PENDING {@code MEMBER_PORTAL} và gói PENDING cho hội viên tự mua hoặc gia hạn (UC-F7.4, UC-F7.5
     * bước 4–6). Nơi gọi đã lấy hội viên từ token; ở đây không nhận {@code memberId} từ request.
     *
     * <p>Thứ tự kiểm tra theo SRS F7 bước 4, rồi quy tắc F3 (Q4 = C):
     * <ol>
     *   <li>Khóa dòng hội viên (NF6): hai yêu cầu cùng lúc chạy lần lượt, yêu cầu sau thấy hóa đơn của yêu cầu trước.</li>
     *   <li>Gói tồn tại, đang bán, dùng được ở chi nhánh gốc: 404 E3, 422 E5.</li>
     *   <li>Đã có hóa đơn chờ chứa đúng gói này: 409 E4.</li>
     *   <li>Gói khác nhưng cùng loại đang PENDING hoặc UPCOMING: 422 như AC-F3.5.1-06 (trong {@code createPackageInvoice}).</li>
     *   <li>Hủy hóa đơn chờ cổng của chính hội viên đã quá hạn {@code PENDING_INVOICE_TTL_DAYS} mà tác vụ nền chưa
     *       kịp quét (BR-F7.5), rồi mới kiểm tra trùng.</li>
     *   <li>Đã có gói cùng loại đang PENDING hoặc UPCOMING hoặc hóa đơn chờ: 422 như AC-F3.5.1-06
     *       (trong {@code createPackageInvoice}).</li>
     * </ol>
     *
     * @param userId tài khoản hội viên, ghi vào {@code invoices.created_by}
     * @param homeBranchId chi nhánh gốc của hội viên, cũng là chi nhánh của hóa đơn (G11)
     * @throws NotFoundException (404) gói không tồn tại
     * @throws ConflictException (409) đã có hóa đơn chờ cho gói này
     * @throws BusinessRuleException (422) gói ngừng bán hoặc khác chi nhánh, hội viên ngừng hoạt động, đã có gói
     *     cùng loại đang chờ
     */
    @Transactional
    public PortalInvoiceDto createForMember(Long userId, Long memberId, Long homeBranchId, Long packageId) {
        if (members.lockForUpdate(memberId).getStatus() != MemberStatus.ACTIVE) {
            // Kiểm tra trước 409: hội viên ngừng hoạt động nhận đúng lý do (cùng câu với createPackageInvoice).
            throw new BusinessRuleException(MEMBER_INACTIVE);
        }
        packages.requireSellable(packageId, homeBranchId);
        if (invoices.existsPendingWithPackage(memberId, packageId)) {
            throw new ConflictException(DUPLICATE_PENDING);
        }
            // Kiểm tra trước: hội viên ngừng hoạt động nhận đúng lý do (cùng câu với createPackageInvoice).
            throw new BusinessRuleException(MEMBER_INACTIVE);
        }
        packages.requireSellable(packageId, homeBranchId);
        // Hóa đơn chờ của chính hội viên đã quá hạn mà tác vụ nền chưa quét thì hủy ngay (cùng giao dịch, đang giữ
        // khóa dòng hội viên), để gói PENDING cũ không chặn yêu cầu mới ở createPackageInvoice.
        cancelExpiredOfMember(memberId);
        PackageInvoiceDto sale = invoiceService.createPackageInvoice(userId, homeBranchId, memberId, packageId,
            InvoiceSource.MEMBER_PORTAL);
        Invoice invoice = invoices.findById(sale.invoiceId()).orElseThrow();
        return PortalInvoiceDto.of(sale, expiresAt(homeBranchId, invoice.getCreatedAt()));
    }

    /**
     * Hủy các hóa đơn PENDING {@code MEMBER_PORTAL} của một hội viên đã tới hạn, kèm gói PENDING đi kèm, trong giao dịch
     * của nơi gọi (nơi gọi đã khóa dòng {@code members}). Cùng điều kiện hạn với {@link #cancelExpired()}: hạn tính theo
     * chi nhánh của hóa đơn.
     */
    private void cancelExpiredOfMember(Long memberId) {
        OffsetDateTime now = time.now();
        Map<Long, Integer> ttlByBranch = new HashMap<>();
        for (Invoice candidate : invoices.findPendingPortalOfMemberCreatedBefore(memberId, now)) {
            int ttlDays = ttlByBranch.computeIfAbsent(candidate.getBranchId(),
                branchId -> configs.getInt(branchId, SystemConfigKeys.PENDING_INVOICE_TTL_DAYS));
            if (!now.isBefore(candidate.getCreatedAt().plusDays(ttlDays))) {
                cancelIfPending(candidate.getId());
            }
        }
    }

    /**
     * Hạn tự hủy của hóa đơn chờ cổng hội viên: {@code created_at + PENDING_INVOICE_TTL_DAYS} ngày (CONFIG-F7.1).
     * Tham số đọc theo chi nhánh của hóa đơn, không có thì dùng giá trị toàn chuỗi. S2-13.1 ({@code GET /me/invoices})
     * dùng lại hàm này để hiện hạn.
     */
    @Transactional(readOnly = true)
    public OffsetDateTime expiresAt(Long branchId, OffsetDateTime createdAt) {
        return createdAt.plusDays(configs.getInt(branchId, SystemConfigKeys.PENDING_INVOICE_TTL_DAYS));
    }

    /**
     * Tác vụ nền: hủy mọi hóa đơn PENDING {@code MEMBER_PORTAL} đã tới hạn tự hủy, kèm gói PENDING đi kèm (BR-F7.5,
     * UC-F7.4 A1). Hóa đơn {@code COUNTER} không bao giờ bị đụng (AC-F3.5.1-09).
     *
     * <p>Mỗi hóa đơn một giao dịch riêng: một hóa đơn lỗi (ví dụ lễ tân vừa thu tiền đúng lúc đó, khóa lạc quan
     * {@code invoices.version} báo xung đột) thì bỏ qua hóa đơn đó, các hóa đơn khác vẫn hủy. Chạy lặp an toàn: chỉ
     * xét hóa đơn còn PENDING.
     *
     * @return số hóa đơn đã hủy trong lần chạy này
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int cancelExpired() {
        OffsetDateTime now = time.now();
        Map<Long, Integer> ttlByBranch = new HashMap<>();
        int cancelled = 0;
        for (Invoice candidate : invoices.findPendingPortalCreatedBefore(now)) {
            try {
                int ttlDays = ttlByBranch.computeIfAbsent(candidate.getBranchId(),
                    branchId -> configs.getInt(branchId, SystemConfigKeys.PENDING_INVOICE_TTL_DAYS));
                if (now.isBefore(candidate.getCreatedAt().plusDays(ttlDays))) {
                    continue;
                }
                if (Boolean.TRUE.equals(transactions.execute(status -> cancelIfPending(candidate.getId())))) {
                    cancelled++;
                }
            } catch (OptimisticLockingFailureException ex) {
                log.info("Bỏ qua hóa đơn {}: vừa được thu tiền hoặc hủy ở nơi khác", candidate.getCode());
            } catch (RuntimeException ex) {
                // Lỗi dữ liệu hoặc cấu hình của một hóa đơn không được chặn các hóa đơn khác, kể cả ở lần chạy sau
                // (hóa đơn lỗi luôn đứng đầu danh sách vì sắp cũ trước).
                log.error("Không tự hủy được hóa đơn {}; cần kiểm tra dữ liệu", candidate.getCode(), ex);
            }
        }
        if (cancelled > 0) {
            log.info("Đã tự hủy {} hóa đơn chờ cổng hội viên quá hạn", cancelled);
        }
        return cancelled;
    }

    /** Giống thân {@link InvoiceService#cancel} nhưng không qua phạm vi chi nhánh (tác vụ hệ thống, không có người gọi). */
    private boolean cancelIfPending(Long invoiceId) {
        Invoice invoice = invoices.findById(invoiceId).orElseThrow();
        if (!invoice.isPending()) {
            return false;
        }
        invoice.cancel();
        invoices.saveAndFlush(invoice);
        invoice.getItems().stream()
            .filter(item -> item.getItemType() == InvoiceItemType.PACKAGE)
            .forEach(item -> memberships.cancelPending(item.getId()));
        return true;
    }
}
