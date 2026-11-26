package vn.edu.hust.nhom6.gym.feature.invoice.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.CreateInvoiceRequest;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PackageInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.Invoice;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceItem;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.InvoiceRepository;
import vn.edu.hust.nhom6.gym.feature.member.entity.Member;
import vn.edu.hust.nhom6.gym.feature.member.entity.MemberStatus;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.membership.entity.Membership;
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipService;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.SellablePackage;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.service.PackageCatalogService;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;

/**
 * Hóa đơn (F5). Lập hóa đơn bán gói ở đây vì hóa đơn là bản ghi gốc; gói hội viên do {@link MembershipService}
 * tạo và chịu trách nhiệm (G13). Issue sau thêm hàm của chính nó vào mục tương ứng: thu tiền và kích hoạt gói
 * (S2-09), hủy hóa đơn chờ của cổng hội viên (S2-14), hoàn tiền (S2-38).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {
    private static final String MEMBER_NOT_FOUND = "Hội viên không tồn tại trên hệ thống.";

    private final InvoiceRepository invoices;
    private final MemberService members;
    private final PackageCatalogService packages;
    private final MembershipService memberships;
    private final StaffService staff;

    // ---------------------------------------------------------------------------------------------
    // S2-07.1: bán và gia hạn gói, tạo hóa đơn PENDING
    // ---------------------------------------------------------------------------------------------

    /** {@code POST /api/v1/invoices}: lễ tân bán gói tại chi nhánh của mình (UC-F3.5). */
    @Transactional
    public PackageInvoiceDto createAtCounter(CurrentUser me, CreateInvoiceRequest request) {
        Long branchId = staff.requireBranchId(me.userId());
        return createPackageInvoice(me.userId(), branchId, request.memberId(),
            request.items().getFirst().packageId(), InvoiceSource.COUNTER);
    }

    /**
     * Lập hóa đơn PENDING một dòng gói và gói hội viên PENDING đi kèm, trong một giao dịch (UC-F3.5 bước 4–6).
     * S2-14 (hội viên tự mua trên cổng F7) gọi với {@code source = MEMBER_PORTAL} và chi nhánh gốc của hội viên.
     *
     * <p>Thứ tự: khóa dòng hội viên trước mọi kiểm tra (NF6), để hai yêu cầu đồng thời cho cùng hội viên chạy lần
     * lượt; yêu cầu sau thấy gói PENDING của yêu cầu trước và bị 422 (AC-F3.5.1-07).
     *
     * @param createdBy {@code users.id} người lập (lễ tân, hoặc tài khoản hội viên khi tự mua)
     * @param branchId chi nhánh lập hóa đơn; hội viên phải có chi nhánh gốc ở đây, gói phải dùng được ở đây
     * @throws NotFoundException (404) hội viên hoặc gói không tồn tại, hội viên ngoài chi nhánh
     * @throws BusinessRuleException (422) hội viên ngừng hoạt động (E6a), gói ngừng bán (E6b), gói chi nhánh khác
     *     (E6c), đã có gói cùng loại đang chờ (E6d)
     */
    @Transactional
    public PackageInvoiceDto createPackageInvoice(Long createdBy, Long branchId, Long memberId, Long packageId,
            InvoiceSource source) {
        Member member = members.lockForUpdate(memberId);
        if (!branchId.equals(member.getHomeBranchId())) {
            throw new NotFoundException(MEMBER_NOT_FOUND);
        }
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessRuleException("Hội viên đã ngừng hoạt động, không thể bán gói tập.");
        }
        SellablePackage pkg = packages.requireSellable(packageId, branchId);
        memberships.requireCanSell(memberId, pkg);

        Invoice invoice = Invoice.pending(nextCode(), branchId, memberId, createdBy, source);
        InvoiceItem item = invoice.addItem(InvoiceItem.forPackage(pkg.id(), pkg.name(), pkg.price()));
        invoices.saveAndFlush(invoice);
        Membership membership = memberships.createPending(memberId, pkg, branchId, item.getId());

        return new PackageInvoiceDto(invoice.getId(), invoice.getCode(), membership.getId(), memberId, pkg.id(),
            pkg.name(), invoice.getSubtotal(), invoice.getDiscount(), invoice.getTotal(), invoice.getStatus(),
            membership.getStatus(), membership.getStartDate(), membership.getEndDate());
    }

    // ---------------------------------------------------------------------------------------------
    // Truy vấn dùng chung (S2-09 thu tiền, S2-09.3 xem hóa đơn)
    // ---------------------------------------------------------------------------------------------

    /**
     * Hóa đơn trong phạm vi nhân viên đang gọi: chủ chuỗi thấy mọi chi nhánh, nhân viên chỉ thấy hóa đơn của chi
     * nhánh mình. Ngoài phạm vi trả 404 như không tồn tại, không lộ hóa đơn chi nhánh khác.
     */
    public Invoice getInScope(Long invoiceId, CurrentUser me) {
        Invoice invoice = invoices.findById(invoiceId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn."));
        if (!me.isOwner() && !staff.requireBranchId(me.userId()).equals(invoice.getBranchId())) {
            throw new NotFoundException("Không tìm thấy hóa đơn.");
        }
        return invoice;
    }

    private String nextCode() {
        long code = invoices.nextInvoiceCode();
        if (code < 1 || code > 999_999) {
            throw new BusinessRuleException("Đã hết dải mã hóa đơn HD sáu chữ số. Vui lòng liên hệ quản trị viên.");
        }
        return String.format(Locale.ROOT, "HD%06d", code);
    }
}
