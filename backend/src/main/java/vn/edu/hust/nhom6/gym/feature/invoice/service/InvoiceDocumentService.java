package vn.edu.hust.nhom6.gym.feature.invoice.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.InvoicePaymentDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.InvoicePdfDocument;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.VietQrPaymentDto;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.Invoice;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.InvoiceDocumentRepository;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.InvoiceDocumentRepository.BranchSnapshot;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.InvoiceRepository;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.InvoiceRepository.MemberSnapshot;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.PaymentRepository;
import vn.edu.hust.nhom6.gym.feature.member.entity.Member;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigService;

/**
 * Sinh thông tin thanh toán VietQR động (UC-F5.1) và xuất hóa đơn PDF (UC-F5.4) cho thẻ S2-09.2.
 * Tách riêng khỏi {@link InvoiceService} để giữ các luồng chứng từ độc lập với luồng thu tiền và tra cứu.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceDocumentService {
    private static final String BANK_ID_KEY = "BANK_ID";
    private static final String BANK_ACCOUNT_NO_KEY = "BANK_ACCOUNT_NO";
    private static final String BANK_ACCOUNT_NAME_KEY = "BANK_ACCOUNT_NAME";
    private static final String MISSING_BANK_CONFIG = "Chi nhánh chưa cấu hình tài khoản nhận chuyển khoản";
    private static final String INVOICE_NOT_FOUND = "Hóa đơn không tồn tại trên hệ thống.";

    private static final Map<String, String> BANK_NAMES = Map.ofEntries(
        Map.entry("MB", "MBBank (NHTM CP Quân Đội)"),
        Map.entry("MBB", "MBBank (NHTM CP Quân Đội)"),
        Map.entry("VCB", "Vietcombank (NHTM CP Ngoại Thương Việt Nam)"),
        Map.entry("TCB", "Techcombank (NHTM CP Kỹ Thương Việt Nam)"),
        Map.entry("BIDV", "BIDV (NHTM CP Đầu Tư và Phát Triển Việt Nam)"),
        Map.entry("ICB", "VietinBank (NHTM CP Công Thương Việt Nam)"),
        Map.entry("VTB", "VietinBank (NHTM CP Công Thương Việt Nam)"),
        Map.entry("ACB", "ACB (NHTM CP Á Châu)"),
        Map.entry("VPB", "VPBank (NHTM CP Việt Nam Thịnh Vượng)"),
        Map.entry("TPB", "TPBank (NHTM CP Tiên Phong)"),
        Map.entry("STB", "Sacombank (NHTM CP Sài Gòn Thương Tín)"),
        Map.entry("VIB", "VIB (NHTM CP Quốc Tế Việt Nam)"));

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final InvoiceDocumentRepository documents;
    private final SystemConfigService configs;
    private final MemberService members;
    private final StaffService staff;
    private final InvoicePdfRenderer pdfRenderer;

    /**
     * {@code GET /api/v1/invoices/{id}/vietqr}: sinh đường dẫn ảnh VietQR Quick Link cho hóa đơn {@code PENDING}
     * (UC-F5.1, BR-F5-01, BR-F5-02, G2). Không gọi mạng ra ngoài; chỉ ghép URL theo mẫu SRS.
     *
     * @throws NotFoundException (404) hóa đơn không tồn tại hoặc thuộc chi nhánh khác
     * @throws BusinessRuleException (422) hóa đơn không ở trạng thái {@code PENDING}, MEMBER_PORTAL quá hạn
     *     hoặc thiếu cấu hình {@code BANK_*}
     */
    public VietQrPaymentDto generateVietQr(CurrentUser me, Long invoiceId) {
        Invoice invoice = invoiceService.getInScope(invoiceId, me);
        if (!invoice.isPending()) {
            throw new BusinessRuleException("Hóa đơn này không ở trạng thái chờ thanh toán.");
        }
        invoiceService.requireWithinPaymentDeadline(invoice);

        Long branchId = invoice.getBranchId();
        String bankId = requireBankConfig(branchId, BANK_ID_KEY);
        String accountNo = requireBankConfig(branchId, BANK_ACCOUNT_NO_KEY);
        String accountName = requireBankConfig(branchId, BANK_ACCOUNT_NAME_KEY);

        BranchSnapshot branch = documents.findBranchSnapshot(branchId)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy chi nhánh id=" + branchId));

        String transferContent = branch.code() + " " + invoice.getCode();
        String qrImageUrl = "https://img.vietqr.io/image/"
            + UriUtils.encodePathSegment(bankId, StandardCharsets.UTF_8)
            + "-"
            + UriUtils.encodePathSegment(accountNo, StandardCharsets.UTF_8)
            + "-compact.png?amount=" + invoice.getTotal()
            + "&addInfo=" + UriUtils.encodeQueryParam(transferContent, StandardCharsets.UTF_8)
            + "&accountName=" + UriUtils.encodeQueryParam(accountName, StandardCharsets.UTF_8);

        return new VietQrPaymentDto(
            invoice.getId(),
            invoice.getCode(),
            invoice.getTotal(),
            bankId,
            resolveBankName(bankId),
            accountNo,
            accountName,
            transferContent,
            qrImageUrl);
    }

    /**
     * {@code GET /api/v1/invoices/{id}/pdf}: xuất hóa đơn PDF cho hóa đơn {@code PAID} hoặc {@code REFUNDED}
     * (UC-F5.4, BR-F5-09, BR-F5-10).
     *
     * <p>Nhân viên chỉ xuất được hóa đơn chi nhánh mình; hội viên chỉ xuất được hóa đơn mang {@code member_id} của
     * chính mình. Ngoài phạm vi trả 404 để không lộ sự tồn tại của hóa đơn.
     *
     * @throws NotFoundException (404) hóa đơn không tồn tại, ngoài chi nhánh hoặc thuộc hội viên khác
     * @throws BusinessRuleException (422) hóa đơn đang {@code PENDING} hoặc {@code CANCELLED}
     */
    public InvoicePdfDocument generatePdf(CurrentUser me, Long invoiceId) {
        Invoice invoice = resolvePdfInvoiceInScope(me, invoiceId);
        if (invoice.getStatus() != InvoiceStatus.PAID && invoice.getStatus() != InvoiceStatus.REFUNDED) {
            throw new BusinessRuleException(
                "Chỉ có thể xuất hóa đơn cho các giao dịch đã thanh toán hoặc đã hoàn tiền.");
        }

        BranchSnapshot branch = documents.findBranchSnapshot(invoice.getBranchId())
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy chi nhánh id=" + invoice.getBranchId()));
        MemberSnapshot member = invoice.getMemberId() == null ? null
            : invoices.findMemberSnapshot(invoice.getMemberId()).orElse(null);

        Map<Long, Optional<String>> receiverNames = new HashMap<>();
        List<InvoicePaymentDto> paymentDtos = payments.findByInvoiceIdOrderByPaidAtDescIdDesc(invoice.getId())
            .stream()
            .map(p -> InvoicePaymentDto.of(p, receiverNames.computeIfAbsent(p.getReceivedBy(), staff::findFullName)
                .orElse(null)))
            .toList();

        byte[] content = pdfRenderer.render(invoice, branch, member, paymentDtos);
        return new InvoicePdfDocument("hoa-don-" + invoice.getCode() + ".pdf", content);
    }

    private Invoice resolvePdfInvoiceInScope(CurrentUser me, Long invoiceId) {
        if (me.is(UserRole.MEMBER)) {
            Member member = members.requireByUserId(me.userId());
            Invoice invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException(INVOICE_NOT_FOUND));
            if (invoice.getMemberId() == null || !member.getId().equals(invoice.getMemberId())) {
                throw new NotFoundException(INVOICE_NOT_FOUND);
            }
            return invoice;
        }
        return invoiceService.getInScope(invoiceId, me);
    }

    private String requireBankConfig(Long branchId, String key) {
        try {
            String value = configs.getString(branchId, key);
            if (value == null || value.isBlank()) {
                throw new BusinessRuleException(MISSING_BANK_CONFIG);
            }
            return value.trim();
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(MISSING_BANK_CONFIG);
        }
    }

    private static String resolveBankName(String bankId) {
        return BANK_NAMES.getOrDefault(bankId.toUpperCase(Locale.ROOT), bankId);
    }
}
