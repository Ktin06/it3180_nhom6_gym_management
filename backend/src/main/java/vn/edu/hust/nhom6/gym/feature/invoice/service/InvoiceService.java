package vn.edu.hust.nhom6.gym.feature.invoice.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.ActivatedMembershipDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.CancelledInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.CreateInvoiceRequest;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.InvoiceDetailDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.InvoiceBranchDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.InvoiceItemDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.InvoicePaymentDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.InvoiceSummaryDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PackageInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PayInvoiceRequest;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PaymentConfirmationDto;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.Invoice;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceItem;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceItemType;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.Payment;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.InvoiceRepository;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.InvoiceDocumentRepository;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.PaymentRepository;
import vn.edu.hust.nhom6.gym.feature.member.entity.Member;
import vn.edu.hust.nhom6.gym.feature.member.entity.MemberStatus;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.membership.entity.Membership;
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipService;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.SellablePackage;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.service.PackageCatalogService;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigKeys;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigService;

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
    private final InvoiceDocumentRepository invoiceLookups;
    private final PaymentRepository payments;
    private final MemberService members;
    private final PackageCatalogService packages;
    private final MembershipService memberships;
    private final StaffService staff;
    private final VnTime time;
    private final SystemConfigService configs;

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
    // S2-09.1: thu tiền tại quầy, kích hoạt gói; hủy hóa đơn chờ
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code POST /api/v1/invoices/{id}/payments}: nhân viên xác nhận đã nhận tiền (UC-F5.2). Trong một giao dịch:
     * hóa đơn PAID, phiếu thu CONFIRMED, gói của mỗi dòng PACKAGE ACTIVE hoặc UPCOMING. Lỗi ở bước nào thì rollback
     * hết, không để phiếu thu thừa.
     *
     * <p>Hai lớp chống bấm trùng (NF6):
     * <ol>
     *   <li>Khóa dòng hội viên trước khi đổi gì: mọi lần bán, thu tiền của cùng hội viên chạy lần lượt, nên kiểm tra
     *       E6b luôn thấy gói UPCOMING mà giao dịch trước vừa tạo.</li>
     *   <li>Khóa lạc quan {@code invoices.version}: hai người xác nhận cùng hóa đơn thì người sau nhận 409 E5.</li>
     * </ol>
     *
     * @throws NotFoundException (404) hóa đơn không có hoặc thuộc chi nhánh khác (E4)
     * @throws BusinessRuleException (422) hóa đơn không PENDING (E6a), đã có gói cùng loại UPCOMING (E6b),
     *     hóa đơn MEMBER_PORTAL quá hạn (E6c)
     * @throws ConflictException (409) người khác vừa xác nhận hoặc hủy hóa đơn này (E5)
     */
    @Transactional
    public PaymentConfirmationDto pay(CurrentUser me, Long invoiceId, PayInvoiceRequest request) {
        Invoice invoice = getInScope(invoiceId, me);
        if (!invoice.isPending()) {
            throw new BusinessRuleException("Hóa đơn đã được thanh toán hoặc đã bị hủy trước đó.");
        }
        if (invoice.getMemberId() != null) {
            members.lockForUpdate(invoice.getMemberId());
        }
        requireWithinPaymentDeadline(invoice);

        invoice.markPaid(time.now());
        try {
            invoices.saveAndFlush(invoice);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException("Dữ liệu hóa đơn vừa được cập nhật bởi thao tác khác. Vui lòng tải lại hóa đơn.");
        }

        List<ActivatedMembershipDto> activated = invoice.getItems().stream()
            .filter(item -> item.getItemType() == InvoiceItemType.PACKAGE)
            .map(item -> ActivatedMembershipDto.of(memberships.activateForPaidInvoice(item.getId())))
            .toList();

        // Hóa đơn 0 đồng (gói giá 0) cũng có phiếu thu 0 đồng, để biết ai xác nhận, lúc nào (V9_06).
        Payment payment = payments.saveAndFlush(Payment.confirmed(invoice, request.method(),
            request.normalizedTransactionRef(), me.userId()));

        return new PaymentConfirmationDto(payment.getId(), invoice.getId(), invoice.getCode(), payment.getMethod(),
            invoice.getTotal(), invoice.getStatus(), invoice.getPaidAt(), me.userId(),
            staff.findFullName(me.userId()).orElse(null), activated);
    }

    /**
     * {@code PUT /api/v1/invoices/{id}/cancel}: hủy hóa đơn chờ tại quầy, kèm gói PENDING của nó (UC-F5.3 luồng A1,
     * BR-F5-08). Không phân biệt nguồn: hóa đơn cổng hội viên cũng hủy tay được.
     *
     * @throws NotFoundException (404) hóa đơn không có hoặc thuộc chi nhánh khác
     * @throws BusinessRuleException (422) hóa đơn không PENDING (E4)
     * @throws OptimisticLockingFailureException (409, thông báo chung) người khác vừa thu tiền hoặc hủy hóa đơn này
     */
    @Transactional
    public CancelledInvoiceDto cancel(CurrentUser me, Long invoiceId) {
        Invoice invoice = getInScope(invoiceId, me);
        if (!invoice.isPending()) {
            throw new BusinessRuleException("Chỉ có thể hủy hóa đơn đang ở trạng thái chờ thanh toán.");
        }
        invoice.cancel();
        invoices.saveAndFlush(invoice);
        invoice.getItems().stream()
            .filter(item -> item.getItemType() == InvoiceItemType.PACKAGE)
            .forEach(item -> memberships.cancelPending(item.getId()));
        return new CancelledInvoiceDto(invoice.getId(), invoice.getCode(), invoice.getStatus(),
            invoice.getUpdatedAt());
    }

    // ---------------------------------------------------------------------------------------------
    // Truy vấn dùng chung (S2-09 thu tiền, S2-09.3 xem hóa đơn)
    // ---------------------------------------------------------------------------------------------

    /**
     * Hóa đơn trong phạm vi nhân viên đang gọi: chủ chuỗi thấy mọi chi nhánh, nhân viên chỉ thấy hóa đơn của chi
     * nhánh mình. Ngoài phạm vi trả 404 như không tồn tại, không lộ hóa đơn chi nhánh khác.
     */
    public Invoice getInScope(Long invoiceId, CurrentUser me) {
        Invoice invoice = invoices.findById(invoiceId).orElseThrow(InvoiceService::invoiceNotFound);
        if (!me.isOwner() && !staff.requireBranchId(me.userId()).equals(invoice.getBranchId())) {
            throw invoiceNotFound();
        }
        return invoice;
    }

    /** BR-F5-07: chặn hóa đơn cổng hội viên quá hạn dù tác vụ tự hủy của F7 chưa chạy. */
    public void requireWithinPaymentDeadline(Invoice invoice) {
        if (invoice.getSource() == InvoiceSource.MEMBER_PORTAL) {
            int ttlDays = configs.getInt(invoice.getBranchId(), SystemConfigKeys.PENDING_INVOICE_TTL_DAYS);
            if (invoice.getCreatedAt().plusDays(ttlDays).isBefore(time.now())) {
                throw new BusinessRuleException(
                    "Hóa đơn chờ đã quá hạn thanh toán, vui lòng lập hóa đơn mới tại quầy.");
            }
        }
    }

    /** Câu 404 của SRS F5 (UC-F5.1 E3, UC-F5.2 E4, UC-F5.3 E3). */
    private static NotFoundException invoiceNotFound() {
        return new NotFoundException("Hóa đơn không tồn tại trên hệ thống.");
    }

    private String nextCode() {
        long code = invoices.nextInvoiceCode();
        if (code < 1 || code > 999_999) {
            throw new BusinessRuleException("Đã hết dải mã hóa đơn HD sáu chữ số. Vui lòng liên hệ quản trị viên.");
        }
        return String.format(Locale.ROOT, "HD%06d", code);
    }

    // ---------------------------------------------------------------------------------------------
    // S2-09.3: tra cứu danh sách và chi tiết hóa đơn (UC-F5.3, BR-F5-06)
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code GET /api/v1/invoices}: danh sách hóa đơn phân trang, mới nhất trước. Lễ tân và quản lý chi nhánh luôn
     * bị khóa vào chi nhánh của mình ({@code branchId} truyền vào bị bỏ qua); chủ chuỗi xem toàn chuỗi hoặc lọc theo
     * {@code branchId}. Khoảng ngày {@code fromDate..toDate} tính theo giờ Việt Nam dạng nửa mở qua {@link VnTime}.
     */
    public PageResponse<InvoiceSummaryDto> search(CurrentUser me, InvoiceStatus status, InvoiceSource source,
            LocalDate fromDate, LocalDate toDate, String keyword, Long branchId, int page, int size) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new InvalidRequestException("Ngày bắt đầu không được lớn hơn ngày kết thúc.");
        }
        Long effectiveBranchId = me.isOwner() ? branchId : staff.requireBranchId(me.userId());

        OffsetDateTime epoch = Instant.EPOCH.atOffset(ZoneOffset.UTC);
        boolean hasFrom = fromDate != null;
        OffsetDateTime fromInstant = hasFrom ? time.dayStart(fromDate) : epoch;
        boolean hasTo = toDate != null;
        OffsetDateTime toInstant = hasTo ? time.dayEnd(toDate) : epoch;

        String trimmed = keyword == null ? "" : keyword.trim();
        boolean hasKeyword = !trimmed.isEmpty();
        String keywordPattern = hasKeyword ? "%" + escapeLike(trimmed.toLowerCase(Locale.ROOT)) + "%" : "";

        return PageResponse.of(invoices.searchSummaries(effectiveBranchId, status, source, hasFrom, fromInstant,
            hasTo, toInstant, hasKeyword, keywordPattern, PageRequest.of(page, size)));
    }

    /** Danh mục chi nhánh cho bộ lọc S2-10 (AC-F5.3.1-04); quyền SUPER_ADMIN kiểm tra tại controller. */
    public List<InvoiceBranchDto> branchesForFilter() {
        return invoiceLookups.findBranchesForInvoiceFilter();
    }

    /**
     * {@code GET /api/v1/invoices/{id}}: chi tiết một hóa đơn trong phạm vi chi nhánh (dùng {@link #getInScope}),
     * gồm thông tin hội viên (rỗng với khách lẻ), danh sách dòng hóa đơn và phiếu thu.
     *
     * @throws NotFoundException (404) hóa đơn không có hoặc thuộc chi nhánh khác
     */
    public InvoiceDetailDto detail(CurrentUser me, Long invoiceId) {
        Invoice invoice = getInScope(invoiceId, me);
        InvoiceRepository.MemberSnapshot member = invoice.getMemberId() == null ? null
            : invoices.findMemberSnapshot(invoice.getMemberId()).orElse(null);

        List<InvoiceItemDto> items = invoice.getItems().stream().map(InvoiceItemDto::of).toList();
        Map<Long, Optional<String>> receiverNames = new HashMap<>();
        List<InvoicePaymentDto> paymentDtos = payments.findByInvoiceIdOrderByPaidAtDescIdDesc(invoice.getId())
            .stream()
            .map(p -> InvoicePaymentDto.of(p, receiverNames.computeIfAbsent(p.getReceivedBy(), staff::findFullName)
                .orElse(null)))
            .toList();

        return new InvoiceDetailDto(invoice.getId(), invoice.getCode(), invoice.getBranchId(), invoice.getMemberId(),
            member == null ? null : member.memberCode(),
            member == null ? null : member.fullName(),
            member == null ? null : member.phone(),
            invoice.getCreatedBy(), invoice.getSource(), invoice.getSubtotal(), invoice.getDiscount(),
            invoice.getTotal(), invoice.getStatus(), invoice.getPaidAt(), invoice.getRefundedBy(),
            invoice.getRefundedAt(), invoice.getRefundReason(), invoice.getCreatedAt(), invoice.getUpdatedAt(),
            items, paymentDtos);
    }

    private static String escapeLike(String raw) {
        return raw.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
