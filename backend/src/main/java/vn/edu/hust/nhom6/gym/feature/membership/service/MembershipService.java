package vn.edu.hust.nhom6.gym.feature.membership.service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.membership.dto.HeldMembershipDto;
import vn.edu.hust.nhom6.gym.feature.membership.entity.Membership;
import vn.edu.hust.nhom6.gym.feature.membership.entity.MembershipStatus;
import vn.edu.hust.nhom6.gym.feature.membership.repository.MembershipRepository;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.SellablePackage;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipService {
    static final String WAITING_EXISTS =
        "Hội viên đã có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt. Không thể tạo thêm gói mới.";

    private static final Set<MembershipStatus> HELD = EnumSet.of(MembershipStatus.ACTIVE, MembershipStatus.FROZEN);
    private static final Set<MembershipStatus> WAITING = EnumSet.of(MembershipStatus.PENDING, MembershipStatus.UPCOMING);
    private static final Set<MembershipStatus> LISTED = EnumSet.of(MembershipStatus.PENDING,
        MembershipStatus.UPCOMING, MembershipStatus.ACTIVE, MembershipStatus.FROZEN);

    private final MembershipRepository memberships;
    private final MemberService members;
    private final VnTime time;

    public void requireCanSell(Long memberId, SellablePackage pkg) {
        plan(memberId, pkg);
    }

    @Transactional
    public Membership createPending(Long memberId, SellablePackage pkg, Long soldBranchId, Long invoiceItemId) {
        LocalDate start = plan(memberId, pkg);
        LocalDate end = start.plusDays(pkg.durationDays() - 1L);
        return memberships.saveAndFlush(Membership.pending(memberId, pkg.id(), pkg.type(), pkg.branchId(),
            soldBranchId, invoiceItemId, start, end, pkg.sessionCount()));
    }

    private LocalDate plan(Long memberId, SellablePackage pkg) {
        List<Membership> sameType = memberships.findSameType(memberId, pkg.type(), pkg.branchId(), LISTED);
        if (hasWaiting(sameType)) {
            throw new BusinessRuleException(WAITING_EXISTS);
        }
        Membership held = sameType.stream().filter(m -> HELD.contains(m.getStatus())).findFirst().orElse(null);
        return plannedStartDate(held, time.today());
    }

    private static boolean hasWaiting(List<Membership> sameTypeAndScope) {
        return sameTypeAndScope.stream().anyMatch(m -> WAITING.contains(m.getStatus()));
    }

    public static LocalDate plannedStartDate(Membership held, LocalDate today) {
        if (held == null) {
            return today;
        }
        LocalDate start = held.getPackageType() == PackageType.SESSION
                && Integer.valueOf(0).equals(held.getRemainingSessions())
            ? today.plusDays(1)
            : held.getEndDate().plusDays(1);
        return start.isBefore(today) ? today : start;
    }

    public List<Membership> findUsable(Long memberId, Long branchId, PackageType type, LocalDate date) {
        return memberships.findUsable(memberId, branchId, type, date);
    }

    public List<HeldMembershipDto> listForStaff(CurrentUser actor, Long memberId) {
        members.requireInStaffScope(actor, memberId);
        var rows = memberships.findHeldWithPackageName(memberId, LISTED);
        Map<ScopeKey, List<Membership>> byScope = rows.stream().map(MembershipRepository.HeldRow::membership)
            .collect(Collectors.groupingBy(ScopeKey::of));
        LocalDate today = time.today();
        return rows.stream().map(r -> {
            Membership m = r.membership();
            LocalDate renewal = HELD.contains(m.getStatus()) && !hasWaiting(byScope.get(ScopeKey.of(m)))
                ? plannedStartDate(m, today) : null;
            return new HeldMembershipDto(m.getId(), m.getPackageId(), r.packageName(), m.getPackageType(),
                m.getBranchId(), m.getStatus(), m.getStartDate(), m.getEndDate(), m.getTotalSessions(),
                m.getRemainingSessions(), renewal);
        }).toList();
    }

    public Membership requireOwnedBy(Long membershipId, Long memberId) {
        return memberships.findById(membershipId)
            .filter(membership -> membership.getMemberId().equals(memberId))
            .orElseThrow(() -> new NotFoundException(
                "Gói tập của hội viên không tồn tại trên hệ thống."));
    }

    private record ScopeKey(PackageType type, Long branchId) {
        static ScopeKey of(Membership m) {
            return new ScopeKey(m.getPackageType(), m.getBranchId());
        }
    }
}
