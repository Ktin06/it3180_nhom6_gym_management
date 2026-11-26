package vn.edu.hust.nhom6.gym.feature.membership.service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.membership.dto.HeldMembershipDto;
import vn.edu.hust.nhom6.gym.feature.membership.entity.Membership;
import vn.edu.hust.nhom6.gym.feature.membership.entity.MembershipStatus;
import vn.edu.hust.nhom6.gym.feature.membership.repository.MembershipRepository;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.SellablePackage;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Lõi gói hội viên (F3). <b>Chỉ lớp này được đổi trạng thái, ngày, số buổi của gói</b>; chức năng khác gọi hàm ở
 * đây, không tự sửa entity (G13). Mỗi issue sau thêm hàm của chính nó vào mục tương ứng bên dưới:
 * kích hoạt khi hóa đơn PAID (S2-09), trừ buổi check-in (S2-11), trừ buổi PT (S2-19), hủy khi hoàn tiền (S2-38).
 * Tác vụ nền EXPIRED/UPCOMING ở S2-07.2.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipService {
    static final String WAITING_EXISTS =
        "Hội viên đã có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt. Không thể tạo thêm gói mới.";


    /** Gói đang giữ quyền lợi; chỉ mục {@code uq_one_active_per_type} bảo đảm tối đa một gói mỗi loại. */
    private static final Set<MembershipStatus> HELD = EnumSet.of(MembershipStatus.ACTIVE, MembershipStatus.FROZEN);
    /** Gói đang chờ; chỉ mục CSDL không phủ nên phải chặn bằng mã (BR-F3-11). */
    private static final Set<MembershipStatus> WAITING = EnumSet.of(MembershipStatus.PENDING, MembershipStatus.UPCOMING);
    /** Gói hiện trong danh sách cho nhân viên, cũng là tập cần xét khi bán gói cùng loại. */
    private static final Set<MembershipStatus> LISTED = EnumSet.of(MembershipStatus.PENDING,
        MembershipStatus.UPCOMING, MembershipStatus.ACTIVE, MembershipStatus.FROZEN);

    private final MembershipRepository memberships;
    private final MemberService members;
    private final VnTime time;


    // ---------------------------------------------------------------------------------------------
    // S2-07.1: tạo gói PENDING khi lập hóa đơn bán gói
    // ---------------------------------------------------------------------------------------------

    /**
     * Kiểm tra bán được gói cùng loại cho hội viên chưa (UC-F3.5 bước 4c). Gọi <b>trước</b> khi lập hóa đơn để lỗi
     * 422 không tiêu mã hóa đơn và không ghi dòng thừa. {@link #createPending} vẫn kiểm tra lại, nên gọi riêng
     * {@code createPending} cũng an toàn.
     *
     * @throws BusinessRuleException (422) đã có gói cùng loại PENDING hoặc UPCOMING (E6d)
     */
    public void requireCanSell(Long memberId, SellablePackage pkg) {
        plan(memberId, pkg);
    }

    /**
     * Tạo gói PENDING đi kèm dòng hóa đơn vừa lập (UC-F3.5 bước 4c, 6). Nơi gọi <b>phải đã khóa dòng hội viên</b>
     * ({@code MemberService.lockForUpdate}) trong cùng giao dịch, để hai yêu cầu bán đồng thời không cùng qua
     * bước kiểm tra (NF6).
     *
     * @param soldBranchId chi nhánh bán (của lễ tân, hoặc chi nhánh gốc hội viên khi bán qua cổng F7)
     * @throws BusinessRuleException (422) đã có gói cùng loại PENDING hoặc UPCOMING (E6d)
     */
    @Transactional
    public Membership createPending(Long memberId, SellablePackage pkg, Long soldBranchId, Long invoiceItemId) {
        LocalDate start = plan(memberId, pkg);
        LocalDate end = start.plusDays(pkg.durationDays() - 1L);
        return memberships.saveAndFlush(Membership.pending(memberId, pkg.id(), pkg.type(), pkg.branchId(),
            soldBranchId, invoiceItemId, start, end, pkg.sessionCount()));
    }

    /** Một nơi duy nhất cho quy tắc E6d và ngày dự kiến khi bán: trả ngày bắt đầu dự kiến hoặc ném 422. */
    private LocalDate plan(Long memberId, SellablePackage pkg) {
        List<Membership> sameType = memberships.findSameType(memberId, pkg.type(), pkg.branchId(), LISTED);
        if (hasWaiting(sameType)) {
            throw new BusinessRuleException(WAITING_EXISTS);
        }
        Membership held = sameType.stream().filter(m -> HELD.contains(m.getStatus())).findFirst().orElse(null);
        return plannedStartDate(held, time.today());
    }

    /** BR-F3-11: đã có gói chờ (PENDING/UPCOMING) trong nhóm gói cùng loại, cùng phạm vi chi nhánh. */
    private static boolean hasWaiting(List<Membership> sameTypeAndScope) {
        return sameTypeAndScope.stream().anyMatch(m -> WAITING.contains(m.getStatus()));
    }

    /**
     * Ngày bắt đầu dự kiến của gói mới cùng loại (BR-F3-11, AC-F3.5.1-03, -04). Dùng chung cho lúc tạo gói
     * PENDING và cho {@code renewalStartDate} ở danh sách gói, để giao diện không tự tính.
     * <ul>
     *   <li>Chưa có gói cùng loại đang giữ: hôm nay (mua mới).</li>
     *   <li>Gói đang giữ còn buổi hoặc là gói TIME: ngày sau {@code end_date} của gói đó.</li>

     * </ul>
     * Không bao giờ sớm hơn hôm nay (gói cũ quá hạn mà tác vụ nền chưa chuyển EXPIRED).
     *
     * @param held gói ACTIVE/FROZEN cùng loại, cùng phạm vi chi nhánh; {@code null} nếu chưa có
     */
    public static LocalDate plannedStartDate(Membership held, LocalDate today) {
        if (held == null) {
            return today;
        }

            ? today.plusDays(1)
            : held.getEndDate().plusDays(1);
        return start.isBefore(today) ? today : start;
    }

    // ---------------------------------------------------------------------------------------------

    // Truy vấn lõi dùng chung (S2-11 check-in, S2-16 đặt lớp, S2-19 đặt PT)
    // ---------------------------------------------------------------------------------------------

    /**
     * Gói ACTIVE loại {@code type} của hội viên dùng được tại chi nhánh {@code branchId} vào ngày {@code date}
     * (gói của chi nhánh đó hoặc gói toàn chuỗi). Gói SESSION 0 buổi vẫn có trong kết quả (vẫn ACTIVE tới hết
     * ngày); nơi gọi tự quyết có trừ buổi không. Thứ tự: hết hạn sớm trước.
     */
    public List<Membership> findUsable(Long memberId, Long branchId, PackageType type, LocalDate date) {
        return memberships.findUsable(memberId, branchId, type, date);
    }

    // ---------------------------------------------------------------------------------------------
    // Danh sách gói của một hội viên cho nhân viên (trả lời câu hỏi S2-08.1)
    // ---------------------------------------------------------------------------------------------

    /**
     * Gói PENDING, UPCOMING, ACTIVE, FROZEN của hội viên, kèm {@code renewalStartDate}: ngày bắt đầu dự kiến nếu
     * bán thêm gói cùng loại, cùng phạm vi chi nhánh. Rỗng khi bán sẽ bị 422 (đã có gói chờ cùng loại), và rỗng
     * trên chính dòng PENDING/UPCOMING.
     *
     * @throws vn.edu.hust.nhom6.gym.common.exception.NotFoundException (404) hội viên không có hoặc ngoài chi nhánh
     */
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


    @Transactional
    public List<Long> expireOverdue(LocalDate today) {
        List<Membership> due = memberships.findDueToExpire(today);
        due.forEach(Membership::expire);
        memberships.saveAllAndFlush(due);
        return due.stream().map(Membership::getId).toList();
    }


     */
    @Transactional
    public UpcomingActivation activateDueUpcoming(LocalDate today) {
        List<Long> activated = new java.util.ArrayList<>();

                skipped.add(next.getId());
            } else if (next.getEndDate().isBefore(today)) {
                next.expireUnstarted();
                memberships.saveAndFlush(next);
            } else {
                next.activateOnSchedule();
                memberships.saveAndFlush(next);
                activated.add(next.getId());
            }
        }

    }
}
