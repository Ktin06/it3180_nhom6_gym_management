package vn.edu.hust.nhom6.gym.feature.invoice.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.RefundRequest;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.RefundResultDto;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.Invoice;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.Payment;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.PaymentStatus;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.InvoiceRepository;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.PaymentRepository;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.RefundAuditRepository;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.RefundLookupRepository;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.RefundLookupRepository.UsageRow;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipService;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;

/**
 * Hoàn tiền hóa đơn bán nhầm (UC-F5.5, S2-38.1). Hoàn tiền chỉ để sửa sai sót khi thu tiền, không hoàn vì hội viên
 * đổi ý (chốt 19/11): chỉ hoàn khi <b>mọi</b> gói của hóa đơn chưa dùng (BR-F5-14).
 *
 * <p>Thứ tự kiểm tra theo SRS: vai trò (403, bước 2) → lý do (400, bước 3) → hóa đơn trong chi nhánh (404) →
 * PAID (422 E6, bước 4) → gói chưa dùng (422 E7, bước 4a). Đổi dữ liệu trong một giao dịch (bước 5).
 */
@Service
@RequiredArgsConstructor
public class RefundService {
    static final String FORBIDDEN = "Chỉ quản lý chi nhánh mới có quyền hoàn tiền hóa đơn.";
    static final String NOT_PAID = "Chỉ có thể hoàn tiền cho hóa đơn đã thanh toán thành công.";
    static final String CONFLICT = "Dữ liệu hóa đơn vừa được cập nhật bởi thao tác khác. Vui lòng thử lại.";
    static final String MEMBERSHIP_USED = "Gói tập của hóa đơn đã được sử dụng nên không thể hoàn tiền. "
        + "Hội viên có thể bảo lưu hoặc chuyển nhượng gói.";
    static final String CLASS_BOOKED = "Hội viên còn chỗ đặt lớp nhóm chưa hủy. Hủy chỗ đặt trước khi hoàn tiền.";

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final RefundLookupRepository lookup;
    private final RefundAuditRepository audit;
    private final MemberService members;
    private final MembershipService memberships;
    private final StaffService staff;
    private final Validator validator;
    private final JsonMapper json;
    private final VnTime time;

    /**
     * {@code POST /api/v1/invoices/{id}/refund}. Thành công thì trong một giao dịch: hóa đơn REFUNDED, phiếu thu
     * CANCELLED, gói của hóa đơn CANCELLED, một dòng {@code audit_logs} REFUND. Bị từ chối ở bất kỳ bước nào thì
     * rollback hết: hóa đơn vẫn PAID, gói và phiếu thu giữ nguyên, không có dòng nhật ký.
     *
     * <p>Chống chạy đua (NF6):
     * <ol>
     *   <li>Khóa dòng hội viên ({@code members FOR UPDATE}) trước khi kiểm tra "gói chưa dùng". Check-in, đặt lớp, đặt
     *       PT cũng khóa dòng này trước, nên không có lượt dùng nào chen vào giữa lúc kiểm tra và lúc hủy gói.</li>
     *   <li>Khóa lạc quan {@code invoices.version}: hai quản lý cùng hoàn thì người sau nhận 409 E5. Hóa đơn được ghi
     *       trước bước kiểm tra BR-F5-14 để người sau vấp version (409), không bị nhầm thành 422 E7 do thấy gói vừa
     *       bị người trước hủy.</li>
     * </ol>
     *
     * @param request body; {@code null} khi không gửi body (coi như thiếu lý do)
     * @param ip địa chỉ người gọi, ghi vào {@code audit_logs.ip} (S9)
     * @throws ForbiddenException (403, E3) không phải quản lý chi nhánh
     * @throws ConstraintViolationException (400, E1) thiếu, trắng, hoặc quá 500 ký tự lý do
     * @throws NotFoundException (404, E4) hóa đơn không có hoặc thuộc chi nhánh khác
     * @throws BusinessRuleException (422) hóa đơn không PAID (E6), gói đã dùng hoặc còn chỗ đặt lớp (E7)
     * @throws ConflictException (409, E5) thao tác khác vừa cập nhật hóa đơn
     */
    @Transactional
    public RefundResultDto refund(CurrentUser me, Long invoiceId, RefundRequest request, String ip) {
        if (me.role() != UserRole.BRANCH_MANAGER) {
            throw new ForbiddenException(FORBIDDEN);
        }
        RefundRequest body = request == null ? new RefundRequest(null) : request;
        Set<ConstraintViolation<RefundRequest>> violations = validator.validate(body);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        String reason = body.refundReason();

        Invoice invoice = invoiceService.getInScope(invoiceId, me);
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new BusinessRuleException(NOT_PAID);
        }
        if (invoice.getMemberId() != null) {
            members.lockForUpdate(invoice.getMemberId());
        }

        OffsetDateTime now = time.now();
        invoice.markRefunded(me.userId(), now, reason);
        try {
            invoices.saveAndFlush(invoice);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException(CONFLICT);
        }

        List<UsageRow> usage = lookup.findUsage(invoiceId, now);
        requireUnused(usage);

        List<Long> paymentIds;
        try {
            paymentIds = payments.findByInvoiceIdOrderByPaidAtDescIdDesc(invoiceId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.CONFIRMED)
                .map(this::cancel)
                .toList();
            usage.forEach(row -> memberships.cancelForRefund(row.invoiceItemId()));
        } catch (OptimisticLockingFailureException ex) {
            // Gói vừa bị thao tác khác sửa (memberships.version), ví dụ tác vụ nền kích hoạt gói UPCOMING.
            throw new ConflictException(CONFLICT);
        }

        audit.insertRefund(me.userId(), invoice.getBranchId(), invoiceId, oldValue(invoice),
            newValue(invoice, usage, paymentIds), ip, now);

        return new RefundResultDto(invoice.getId(), invoice.getCode(), invoice.getStatus(), invoice.getRefundedAt(),
            invoice.getRefundedBy(), staff.findFullName(me.userId()).orElse(null), invoice.getRefundReason());
    }

    /**
     * BR-F5-14: mọi gói phải chưa dùng. Gói đã dùng (điều kiện 1–3) báo câu chung; chỉ còn vướng chỗ đặt lớp (điều
     * kiện 4) thì báo câu riêng để quản lý biết hủy chỗ đặt rồi hoàn lại.
     */
    private static void requireUnused(List<UsageRow> usage) {
        if (usage.stream().anyMatch(UsageRow::used)) {
            throw new BusinessRuleException(MEMBERSHIP_USED);
        }
        if (usage.stream().anyMatch(UsageRow::hasUpcomingClassBooking)) {
            throw new BusinessRuleException(CLASS_BOOKED);
        }
    }

    private Long cancel(Payment payment) {
        payment.cancel();
        return payments.saveAndFlush(payment).getId();
    }

    private String oldValue(Invoice invoice) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("status", InvoiceStatus.PAID.name());
        value.put("total", invoice.getTotal());
        return json.writeValueAsString(value);
    }

    /** Người duyệt nằm ở cột {@code user_id}, thời gian ở {@code created_at}; đây là số tiền, lý do, phần bị hủy. */
    private String newValue(Invoice invoice, List<UsageRow> usage, List<Long> paymentIds) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("status", invoice.getStatus().name());
        value.put("total", invoice.getTotal());
        value.put("refundReason", invoice.getRefundReason());
        value.put("refundedAt", invoice.getRefundedAt().toString());
        value.put("membershipIds", usage.stream().map(UsageRow::membershipId).toList());
        value.put("paymentIds", paymentIds);
        return json.writeValueAsString(value);
    }
}
