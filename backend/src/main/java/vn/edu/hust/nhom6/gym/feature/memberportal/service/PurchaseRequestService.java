package vn.edu.hust.nhom6.gym.feature.memberportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PortalInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.service.PortalInvoiceService;
import vn.edu.hust.nhom6.gym.feature.member.entity.Member;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.membership.entity.Membership;
import vn.edu.hust.nhom6.gym.feature.membership.entity.MembershipStatus;
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipService;

/**
 * Hội viên tự gửi yêu cầu mua hoặc gia hạn gói trên cổng (F7 UC-F7.4, UC-F7.5). Hội viên luôn lấy từ token
 * ({@link MemberService#requireByUserId}), không nhận {@code memberId} từ request (S3). Hóa đơn chờ lập ở chi nhánh gốc
 * của hội viên (G11); hội viên chỉ tạo hóa đơn chờ, thu tiền là việc của quầy (BR-F7.7).
 */
@Service
@RequiredArgsConstructor
public class PurchaseRequestService {
    private final MemberService members;
    private final MembershipService memberships;
    private final PortalInvoiceService portalInvoices;

    /** {@code POST /api/v1/me/purchase-requests}. */
    @Transactional
    public PortalInvoiceDto purchase(CurrentUser me, Long packageId) {
        Member member = members.requireByUserId(me.userId());
        return portalInvoices.createForMember(me.userId(), member.getId(), member.getHomeBranchId(), packageId);
    }

    /**
     * {@code POST /api/v1/me/renewal-requests}: mua thêm đúng gói danh mục của gói đang có; gói cũ không đổi
     * (BR-F7.8). Gói của hội viên khác hoặc không tồn tại đều trả 404 (E2, E3; VD-30 = B).
     * Gói Đã hủy (hóa đơn bị hủy hoặc đã hoàn tiền) không phải "gói đang có" nên trả 422; gói Hết hạn vẫn gia
     * hạn được (trưởng nhóm chọn VD-31 = B, 22/11; chờ thêm AC vào SRS F7).
     */
    @Transactional
    public PortalInvoiceDto renew(CurrentUser me, Long membershipId) {
        Member member = members.requireByUserId(me.userId());
        Membership current = memberships.requireOwnedBy(membershipId, member.getId());
        if (current.getStatus() == MembershipStatus.CANCELLED) {
            throw new BusinessRuleException("Gói này đã hủy. Vui lòng chọn gói ở mục Mua gói.");
        }
        return portalInvoices.createForMember(me.userId(), member.getId(), member.getHomeBranchId(),
            current.getPackageId());
    }
}
