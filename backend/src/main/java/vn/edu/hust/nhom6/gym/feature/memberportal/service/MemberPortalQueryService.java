package vn.edu.hust.nhom6.gym.feature.memberportal.service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;
import vn.edu.hust.nhom6.gym.feature.member.entity.Member;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.memberportal.dto.MyCheckinDto;
import vn.edu.hust.nhom6.gym.feature.memberportal.dto.MyInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.memberportal.dto.MyInvoiceItemDto;
import vn.edu.hust.nhom6.gym.feature.memberportal.dto.MyMembershipDto;
import vn.edu.hust.nhom6.gym.feature.memberportal.repository.MemberPortalQueryRepository;
import vn.edu.hust.nhom6.gym.feature.memberportal.repository.MemberPortalQueryRepository.InvoiceItemRow;
import vn.edu.hust.nhom6.gym.feature.memberportal.repository.MemberPortalQueryRepository.InvoiceRow;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigKeys;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigService;

/**
 * Nghiệp vụ chỉ đọc cho cổng hội viên (F7 UC-F7.1–UC-F7.3): xem gói tập, lịch sử check-in, hóa đơn của chính mình.
 *
 * <p>Hội viên luôn xác định từ token qua {@link MemberService#requireByUserId}; không nhận {@code memberId} từ request
 * để hội viên không bao giờ xem được dữ liệu người khác (BR-F7.1, S3). Hạn tự hủy hóa đơn chờ từ cổng hội viên tính
 * bằng {@code created_at + PENDING_INVOICE_TTL_DAYS} (đọc từ {@link SystemConfigService}, cache theo {@code branchId}
 * trong một lần gọi {@link #myInvoices}, BR-F7.4).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberPortalQueryService {
    private final MemberService members;
    private final MemberPortalQueryRepository queries;
    private final SystemConfigService configs;

    /** {@code GET /api/v1/me/memberships} (UC-F7.1). */
    public List<MyMembershipDto> myMemberships(CurrentUser me) {
        Member member = members.requireByUserId(me.userId());
        return queries.findMemberships(member.getId()).stream()
            .map(row -> new MyMembershipDto(
                row.id(),
                row.packageId(),
                row.packageName(),
                row.packageType(),
                row.branchId(),
                row.branchName(),
                row.status(),
                row.startDate(),
                row.endDate(),
                row.totalSessions(),
                row.remainingSessions()))
            .toList();
    }

    /** {@code GET /api/v1/me/checkins} (UC-F7.2). */
    public PageResponse<MyCheckinDto> myCheckins(CurrentUser me, int page, int size) {
        Member member = members.requireByUserId(me.userId());
        return PageResponse.of(queries.findCheckins(member.getId(), page, size),
            row -> new MyCheckinDto(
                row.id(),
                row.branchId(),
                row.branchName(),
                row.membershipId(),
                row.packageName(),
                row.packageType(),
                row.method(),
                row.sessionDeducted(),
                row.checkinDate(),
                row.checkedInAt(),
                row.checkedOutAt()));
    }

    /** {@code GET /api/v1/me/invoices} (UC-F7.3). */
    public PageResponse<MyInvoiceDto> myInvoices(CurrentUser me, int page, int size) {
        Member member = members.requireByUserId(me.userId());
        Page<InvoiceRow> invoices = queries.findInvoices(member.getId(), page, size);
        List<Long> invoiceIds = invoices.getContent().stream().map(InvoiceRow::id).toList();
        Map<Long, List<InvoiceItemRow>> itemsByInvoice = queries.findInvoiceItems(invoiceIds);
        Map<Long, Integer> ttlByBranch = new HashMap<>();

        return PageResponse.of(invoices, row -> {
            OffsetDateTime expiresAt = null;
            if (row.status() == InvoiceStatus.PENDING && row.source() == InvoiceSource.MEMBER_PORTAL) {
                int ttlDays = ttlByBranch.computeIfAbsent(row.branchId(),
                    branchId -> configs.getInt(branchId, SystemConfigKeys.PENDING_INVOICE_TTL_DAYS));
                expiresAt = row.createdAt().plusDays(ttlDays);
            }
            List<MyInvoiceItemDto> items = itemsByInvoice.getOrDefault(row.id(), List.of()).stream()
                .map(item -> new MyInvoiceItemDto(
                    item.id(),
                    item.itemType(),
                    item.packageId(),
                    item.description(),
                    item.quantity(),
                    item.unitPrice(),
                    item.amount(),
                    item.discountAmount()))
                .toList();
            return new MyInvoiceDto(
                row.id(),
                row.code(),
                row.branchId(),
                row.branchName(),
                row.source(),
                row.subtotal(),
                row.discount(),
                row.total(),
                row.status(),
                row.paidAt(),
                row.createdAt(),
                expiresAt,
                items);
        });
    }
}
