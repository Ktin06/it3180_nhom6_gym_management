package vn.edu.hust.nhom6.gym.feature.membership.service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
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
    /** SRS F5 UC-F5.2 E6b. */
    static final String UPCOMING_EXISTS = "Hội viên đã có gói cùng loại đang chờ kích hoạt (UPCOMING). Không thể "
        + "kích hoạt thêm gói này. Vui lòng liên hệ quản lý để kiểm tra hoặc hoàn tiền hóa đơn.";
    /** SRS F4 UC-F4.2 E6. */
    public static final String CHECKIN_CONFLICT =
        "Dữ liệu lượt check-in đang được xử lý đồng thời bởi một thao tác khác. Vui lòng thử lại.";
    /** SRS F4 UC-F4.2 E4. */
    public static final String SESSIONS_USED_UP =
        "Gói tập theo buổi của hội viên đã hết lượt sử dụng. Vui lòng gia hạn thêm gói mới.";
    /** SRS F5 UC-F5.2 E6b. */
    static final String UPCOMING_EXISTS = "Hội viên đã có gói cùng loại đang chờ kích hoạt (UPCOMING). Không thể "
        + "kích hoạt thêm gói này. Vui lòng liên hệ quản lý để kiểm tra hoặc hoàn tiền hóa đơn.";

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
    private final EntityManager entities;

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
     *   <li>Gói đang giữ đã về 0 buổi (vẫn ACTIVE tới hết hôm nay): ngày mai.</li>
     *   <li>Gói SESSION đang giữ đã về 0 buổi (vẫn ACTIVE tới hết hôm nay): ngày mai.</li>
     * </ul>
     * Không bao giờ sớm hơn hôm nay (gói cũ quá hạn mà tác vụ nền chưa chuyển EXPIRED).
     *
     * @param held gói ACTIVE/FROZEN cùng loại, cùng phạm vi chi nhánh; {@code null} nếu chưa có
     */
    public static LocalDate plannedStartDate(Membership held, LocalDate today) {
        if (held == null) {
            return today;
        }
        LocalDate start = Integer.valueOf(0).equals(held.getRemainingSessions())
        LocalDate start = held.getPackageType() == PackageType.SESSION
                && Integer.valueOf(0).equals(held.getRemainingSessions())
            ? today.plusDays(1)
            : held.getEndDate().plusDays(1);
        return start.isBefore(today) ? today : start;
    }

    // ---------------------------------------------------------------------------------------------
    // S2-09.1: kích hoạt gói khi hóa đơn PAID, hủy gói khi hủy hóa đơn chờ
    // ---------------------------------------------------------------------------------------------

    /**
     * Kích hoạt gói PENDING của một dòng hóa đơn vừa thu tiền (UC-F5.2 bước 5d, BR-F5-04, BR-F3-11, BR-F3-12).
     * Nơi gọi <b>phải đã khóa dòng hội viên</b> ({@code MemberService.lockForUpdate}) trong cùng giao dịch, để hai
     * hóa đơn cùng loại thu tiền đồng thời không cùng qua bước kiểm tra E6b.
     * <ul>
     *   <li>Đã có gói cùng loại, cùng phạm vi chi nhánh đang UPCOMING: 422 E6b.</li>
     *   <li>Chưa có gói cùng loại ACTIVE/FROZEN: ACTIVE từ hôm nay (giờ Việt Nam).</li>
     *   <li>Có: UPCOMING từ {@link #plannedStartDate} (sau {@code end_date} gói cũ, hoặc ngày mai nếu gói cũ 0 buổi).</li>
     *   <li>Có: UPCOMING từ {@link #plannedStartDate} (sau {@code end_date} gói cũ; chỉ SESSION 0 buổi bắt đầu ngày mai).</li>
     * </ul>
     * Số ngày hiệu lực giữ đúng số đã chốt lúc lập hóa đơn ({@link Membership#durationDays()}). Không đổi gói cũ.
     *
     * @return gói sau khi kích hoạt
     * @throws BusinessRuleException (422) đã có gói cùng loại UPCOMING (E6b)
     * @throws IllegalStateException dòng hóa đơn không có gói PENDING (lỗi dữ liệu)
     */
    @Transactional
    public Membership activateForPaidInvoice(Long invoiceItemId) {
        Membership pending = memberships.findByInvoiceItemId(invoiceItemId)
            .orElseThrow(() -> new IllegalStateException("Dòng hóa đơn " + invoiceItemId + " không có gói tập."));
        List<Membership> others = memberships.findSameType(pending.getMemberId(), pending.getPackageType(),
            pending.getBranchId(), EnumSet.of(MembershipStatus.UPCOMING, MembershipStatus.ACTIVE,
                MembershipStatus.FROZEN));
        if (others.stream().anyMatch(m -> m.getStatus() == MembershipStatus.UPCOMING)) {
            throw new BusinessRuleException(UPCOMING_EXISTS);
        }
        LocalDate today = time.today();
        Membership held = others.stream().filter(m -> HELD.contains(m.getStatus())).findFirst().orElse(null);
        if (held == null) {
            pending.activate(today);
        } else {
            pending.scheduleFrom(plannedStartDate(held, today));
        }
        return memberships.saveAndFlush(pending);
    }

    /**
     * Hủy gói PENDING đi kèm một dòng hóa đơn chờ bị hủy (BR-F5-08).
     *
     * @throws IllegalStateException gói của hóa đơn chờ không còn PENDING (lỗi dữ liệu): rollback cả việc hủy hóa
     *     đơn, không để gói đang dùng được gắn với hóa đơn đã hủy
     */
    @Transactional
    public void cancelPending(Long invoiceItemId) {
        memberships.findByInvoiceItemId(invoiceItemId).ifPresent(m -> {
            m.cancelPending();
            memberships.saveAndFlush(m);
        });
    }

    // ---------------------------------------------------------------------------------------------
    // S2-11.1: trừ buổi khi check-in tại quầy (UC-F4.2 bước 5, BR-F4.3, BR-F4.7)
    // ---------------------------------------------------------------------------------------------

    /**
     * Trừ một buổi của gói SESSION khi check-in lần đầu trong ngày. Khóa dòng gói ({@code FOR UPDATE}) và nạp lại số
     * buổi, trạng thái. Về 0 thì gói vẫn ACTIVE đến hết ngày, và các gói UPCOMING cùng hội viên, cùng loại, cùng
     * phạm vi chi nhánh được dời sớm {@code D} ngày để gói sớm nhất bắt đầu từ ngày mai (BR-F4.7; {@code D <= 0} thì giữ nguyên).
     *
     * <p>Nơi gọi <b>phải đã khóa</b> {@code members} (thứ tự members → memberships → class_bookings) và tự kiểm tra
     * "hôm nay đã trừ chưa" (bảng {@code checkins} thuộc F4). Ghi bằng {@code saveAndFlush}, nên lỗi {@code @Version}
     * lộ ra ngay tại đây; nơi gọi đổi thành 409 (E6).
     *
     * @param today ngày Việt Nam của lượt check-in
     * @return gói sau khi trừ
     * @throws BusinessRuleException (422) gói đã hết buổi (E4)
     * @throws ConflictException (409) gói vừa bị thao tác khác đổi khỏi ACTIVE (E6), lễ tân bấm lại để chọn lại gói
     */
    @Transactional
    public Membership deductSessionForCheckin(Long membershipId, LocalDate today) {
        Membership membership = memberships.findById(membershipId)
            .orElseThrow(() -> new IllegalStateException("Không có gói " + membershipId));
        // Nơi gọi thường đã nạp gói này (findUsable) trong cùng giao dịch; truy vấn khóa sẽ trả lại bản đã nạp mà không
        // đọc lại cột. refresh kèm khóa vừa SELECT ... FOR UPDATE vừa nạp lại số buổi, trạng thái mới nhất.
        entities.refresh(membership, LockModeType.PESSIMISTIC_WRITE);
        if (membership.getPackageType() != PackageType.SESSION || membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new ConflictException(CHECKIN_CONFLICT);
        }
        if (membership.getRemainingSessions() == null || membership.getRemainingSessions() <= 0) {
            throw new BusinessRuleException(SESSIONS_USED_UP);
        }
        membership.deductOneSession();
        memberships.saveAndFlush(membership);
        if (membership.getRemainingSessions() == 0) {
            pullUpcomingToTomorrow(membership, today);
        }
        return membership;
    }

    /** BR-F4.7: dời mọi gói UPCOMING cùng nhóm sớm {@code D} ngày, giữ thứ tự nối tiếp và số ngày hiệu lực. */
    private void pullUpcomingToTomorrow(Membership finished, LocalDate today) {
        List<Membership> upcoming = memberships.findSameType(finished.getMemberId(), finished.getPackageType(),
            finished.getBranchId(), EnumSet.of(MembershipStatus.UPCOMING));
        LocalDate earliest = upcoming.stream().map(Membership::getStartDate).min(LocalDate::compareTo).orElse(null);
        if (earliest == null) {
            return;
        }
        long days = earliest.toEpochDay() - today.plusDays(1).toEpochDay();
        if (days <= 0) {
            return;
        }
        for (Membership next : upcoming) {
            next.moveEarlier(days);
            memberships.saveAndFlush(next);
        }
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

    // ---------------------------------------------------------------------------------------------
    // S2-14.1: hội viên gia hạn gói của mình trên cổng (UC-F7.5)
    // ---------------------------------------------------------------------------------------------

    /**
     * Gói {@code membershipId} của đúng hội viên {@code memberId} (UC-F7.5 bước 4). Nơi gọi chỉ đọc entity trả về.
     *
     * <p>Gói của hội viên khác trả 404 giống gói không tồn tại, như mọi API khác của hội viên, để không lộ
     * mã gói nào đang có (trưởng nhóm chọn VD-30 = B, 22/11; SRS UC-F7.5 E2 còn ghi 403, chờ sửa).
     *
     * @throws NotFoundException (404) gói không tồn tại hoặc của hội viên khác (E2, E3)
     */
    public Membership requireOwnedBy(Long membershipId, Long memberId) {
        return memberships.findById(membershipId)
            .filter(membership -> membership.getMemberId().equals(memberId))
            .orElseThrow(() -> new NotFoundException(
                "Gói tập của hội viên không tồn tại trên hệ thống."));
    }

    /** Loại gói + phạm vi chi nhánh (rỗng = toàn chuỗi); hai gói cùng khóa thì tính là "cùng loại". */
    private record ScopeKey(PackageType type, Long branchId) {
        static ScopeKey of(Membership m) {
            return new ScopeKey(m.getPackageType(), m.getBranchId());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // S2-38.1: hủy gói khi hoàn tiền hóa đơn (UC-F5.5 bước 5d, BR-F5-13)
    // ---------------------------------------------------------------------------------------------

    /**
     * Hủy gói của một dòng hóa đơn vừa hoàn tiền: ACTIVE/UPCOMING → CANCELLED. Chỉ đụng đúng gói của dòng này; gói
     * khác của hội viên (kể cả gói UPCOMING nối tiếp mua bằng hóa đơn khác) giữ nguyên trạng thái và ngày (Q5 = A).
     * Không hủy buổi PT hay chỗ đặt lớp: nơi gọi đã chặn hoàn tiền khi gói đã dùng (BR-F5-14).
     *
     * <p>Dòng không có gói (dữ liệu cũ không qua luồng bán) thì bỏ qua.
     *
     * @throws IllegalStateException gói không ACTIVE/UPCOMING (nơi gọi đã kiểm tra trước): rollback cả việc hoàn tiền
     */
    @Transactional
    public void cancelForRefund(Long invoiceItemId) {
        memberships.findByInvoiceItemId(invoiceItemId).ifPresent(m -> {
            m.cancelForRefund();
            memberships.saveAndFlush(m);
        });
    }

    // ---------------------------------------------------------------------------------------------
    // S2-19.3: trừ buổi PT khi PT xác nhận buổi hoặc ghi vắng (UC-F9.3, BR-F9.6, BR-F9.7, BR-F9.18)
    // ---------------------------------------------------------------------------------------------

    /** Gói PT của buổi không ở trạng thái trừ được (UC-F9.3 tiền điều kiện "gói PT hợp lệ"). */
    public static final String PT_PACKAGE_UNUSABLE =
        "Gói PT của buổi này không còn hiệu lực, không trừ buổi được. Vui lòng liên hệ quản lý chi nhánh.";
    /** Gói PT của buổi đã hết buổi (buổi đã qua giờ còn sót lại khi gói về 0). */
    public static final String PT_SESSIONS_USED_UP =
        "Gói PT của buổi này đã hết buổi, không trừ thêm được. Vui lòng liên hệ quản lý chi nhánh.";

    /**
     * Kết quả trừ buổi PT.
     *
     * @param deducted gói vừa bị trừ
     * @param expiredNow {@code true} nếu lần trừ này làm gói về 0 và EXPIRED (BR-F9.7)
     * @param activatedNext gói PT nối tiếp vừa được kéo về ACTIVE hôm nay; {@code null} nếu không có
     */
    public record PtDeduction(Membership deducted, boolean expiredNow, Membership activatedNext) { }

    /**
     * Trừ một buổi của gói PT khi buổi PT chuyển {@code DONE} hoặc {@code NO_SHOW} (BR-F9.6, BR-F9.18).
     * <ul>
     *   <li>Gói ACTIVE: trừ 1. Về 0 thì gói EXPIRED ngay và flush (nhường chỉ mục {@code uq_one_active_per_type}),
     *       rồi gói PT UPCOMING sớm nhất cùng hội viên, cùng phạm vi chi nhánh được kéo về bắt đầu {@code today} và
     *       ACTIVE, giữ số ngày hiệu lực (BR-F9.7). Gói UPCOMING sau nó (nếu có) dời sớm cùng số ngày.</li>
     *   <li>Gói đã EXPIRED vì hết ngày, còn buổi, và {@code sessionDate} nằm trong hạn gói (quản lý xác nhận muộn):
     *       chỉ trừ 1, không đổi trạng thái gói nào (trưởng nhóm chốt 20/11).</li>
     * </ul>
     *
     * <p>Nơi gọi <b>phải đã khóa</b> {@code members} (thứ tự members → memberships → pt_sessions, khớp check-in F4) và tự
     * bảo đảm mỗi buổi chỉ trừ một lần (trạng thái buổi thuộc F9). Hàm khóa dòng gói ({@code FOR UPDATE}) và nạp lại.
     *
     * @param sessionDate ngày Việt Nam của buổi PT
     * @param today ngày Việt Nam lúc xác nhận
     * @throws BusinessRuleException (422) gói không còn hiệu lực hoặc đã hết buổi
     */
    @Transactional
    public PtDeduction deductPtSession(Long membershipId, LocalDate sessionDate, LocalDate today) {
        Membership membership = memberships.findById(membershipId)
            .orElseThrow(() -> new IllegalStateException("Không có gói " + membershipId));
        // Như deductSessionForCheckin: refresh kèm khóa để vừa SELECT ... FOR UPDATE vừa nạp lại số buổi, trạng thái.
        entities.refresh(membership, LockModeType.PESSIMISTIC_WRITE);
        if (membership.getPackageType() != PackageType.PT) {
            throw new IllegalStateException("Gói " + membershipId + " không phải gói PT");
        }
        boolean hasSessions = membership.getRemainingSessions() != null && membership.getRemainingSessions() > 0;
        if (membership.getStatus() == MembershipStatus.EXPIRED) {
            if (!hasSessions) {
                throw new BusinessRuleException(PT_SESSIONS_USED_UP);
            }
            if (sessionDate.isBefore(membership.getStartDate()) || sessionDate.isAfter(membership.getEndDate())) {
                throw new BusinessRuleException(PT_PACKAGE_UNUSABLE);
            }
            membership.deductSessionAfterExpiry();
            memberships.saveAndFlush(membership);
            return new PtDeduction(membership, false, null);
        }
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new BusinessRuleException(PT_PACKAGE_UNUSABLE);
        }
        if (!hasSessions) {
            throw new BusinessRuleException(PT_SESSIONS_USED_UP);
        }
        membership.deductOneSession();
        memberships.saveAndFlush(membership);
        if (membership.getRemainingSessions() > 0) {
            return new PtDeduction(membership, false, null);
        }
        membership.expireAfterLastPtSession();
        // Flush trước để gói nối tiếp lên ACTIVE không vướng chỉ mục uq_one_active_per_type (BR-F9.7).
        memberships.saveAndFlush(membership);
        return new PtDeduction(membership, true, activateNextPtMembership(membership, today));
    }

    /**
     * BR-F9.7: gói PT UPCOMING sớm nhất cùng nhóm bắt đầu {@code today} và ACTIVE; gói UPCOMING sau nó (thường không
     * có, vì BR-F3-11 chỉ cho một gói chờ) dời sớm cùng số ngày để giữ thứ tự nối tiếp.
     */
    private Membership activateNextPtMembership(Membership finished, LocalDate today) {
        List<Membership> upcoming = memberships.findSameType(finished.getMemberId(), finished.getPackageType(),
            finished.getBranchId(), EnumSet.of(MembershipStatus.UPCOMING));
        Membership next = upcoming.stream()
            .min(java.util.Comparator.comparing(Membership::getStartDate).thenComparing(Membership::getId))
            .orElse(null);
        if (next == null) {
            return null;
        }
        long shift = next.getStartDate().toEpochDay() - today.toEpochDay();
        // Gói đã tới ngày bắt đầu mà tác vụ nền chưa kích hoạt: giữ nguyên ngày, không dời muộn hơn (không kéo dài hạn).
        next.activateEarly(shift > 0 ? today : next.getStartDate());
        memberships.saveAndFlush(next);
        if (shift > 0) {
            for (Membership later : upcoming) {
                if (later != next) {
                    later.moveEarlier(shift);
                    memberships.saveAndFlush(later);
                }
            }
        }
        return next;
    }

    // ---------------------------------------------------------------------------------------------
    // S2-07.2: tác vụ nền 00:00 – bước 1 chuyển EXPIRED, bước 2 kích hoạt UPCOMING (AC-F3.5.1-10, BR-F4.7).
    // Thứ tự và giao dịch do MembershipLifecycleService.runDaily giữ; hai hàm dưới chỉ là từng bước.
    // ---------------------------------------------------------------------------------------------

    /**
     * Bước 1: chuyển EXPIRED mọi gói ACTIVE có {@code end_date < today}, và gói SESSION 0 buổi mà hôm nay chưa trừ buổi.
     * Flush trước khi trả về, để bước 2 không vướng chỉ mục {@code uq_one_active_per_type}.
     *
     * @return id các gói vừa EXPIRED
     */
    /** Bước một của tác vụ: ghi EXPIRED trước khi kích hoạt gói cùng nhóm. */
    @Transactional
    public List<Long> expireOverdue(LocalDate today) {
        List<Membership> due = memberships.findDueToExpire(today);
        due.forEach(Membership::expire);
        memberships.saveAllAndFlush(due);
        return due.stream().map(Membership::getId).toList();
    }

    /**
     * Kết quả bước 2.
     *
     * @param activated gói UPCOMING vừa ACTIVE
     * @param expiredUnstarted gói UPCOMING mà cả hạn đã trôi qua lúc máy chủ tắt, chuyển thẳng EXPIRED
     * @param skipped gói UPCOMING tới ngày nhưng cùng nhóm còn gói ACTIVE/FROZEN, giữ nguyên chờ lần chạy sau
     */
    public record UpcomingActivation(List<Long> activated, List<Long> expiredUnstarted, List<Long> skipped) { }

    /**
     * Bước 2: kích hoạt gói UPCOMING có {@code start_date <= today}, giữ nguyên ngày đã xếp. Xét theo hội viên, ngày bắt
     * đầu tăng dần, nên chuỗi nối tiếp được xử lý đúng thứ tự:
     * <ul>
     *   <li>Cùng hội viên, cùng loại, cùng phạm vi chi nhánh còn gói ACTIVE/FROZEN: bỏ qua (dữ liệu lệch, hoặc gói đang
     *       bảo lưu ở v3), không để một dòng lỗi làm hỏng cả lượt chạy. Xét điều này trước tiên.</li>
     *   <li>{@code end_date < today} (cả hạn trôi qua khi máy chủ tắt nhiều ngày): EXPIRED thẳng.</li>
     *   <li>Còn lại: ACTIVE, flush ngay để gói sau trong cùng nhóm thấy chỉ mục đã có chủ.</li>
     * </ul>
     * Gọi <b>sau</b> {@link #expireOverdue} trong cùng giao dịch.
    public record UpcomingActivation(List<Long> activated, List<Long> expiredUnstarted, List<Long> skipped) { }

    /**
     * Bước hai: giữ nguyên ngày đã chốt. Gói chờ sau ACTIVE/FROZEN giữ nguyên;
     * gói đã trôi hết hạn khi tắt máy chuyển EXPIRED, không kích hoạt ngược về quá khứ.
     */
    @Transactional
    public UpcomingActivation activateDueUpcoming(LocalDate today) {
        List<Long> activated = new java.util.ArrayList<>();
        List<Long> expiredUnstarted = new java.util.ArrayList<>();
        List<Long> skipped = new java.util.ArrayList<>();
        for (Membership next : memberships.findDueUpcoming(today)) {
            // Kiểm tra nhóm trước: gói chờ sau gói FROZEN (bảo lưu) không được EXPIRED dù đã quá end_date.
            if (!memberships.findSameType(next.getMemberId(), next.getPackageType(), next.getBranchId(),
                    HELD).isEmpty()) {
        List<Long> expired = new java.util.ArrayList<>();
        List<Long> skipped = new java.util.ArrayList<>();
        for (Membership next : memberships.findDueUpcoming(today)) {
            if (!memberships.findSameType(next.getMemberId(), next.getPackageType(), next.getBranchId(), HELD).isEmpty()) {
                skipped.add(next.getId());
            } else if (next.getEndDate().isBefore(today)) {
                next.expireUnstarted();
                memberships.saveAndFlush(next);package vn.edu.hust.nhom6.gym.feature.membership.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
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
    /** SRS F5 UC-F5.2 E6b. */
    static final String UPCOMING_EXISTS = "Hội viên đã có gói cùng loại đang chờ kích hoạt (UPCOMING). Không thể "
        + "kích hoạt thêm gói này. Vui lòng liên hệ quản lý để kiểm tra hoặc hoàn tiền hóa đơn.";
    /** SRS F4 UC-F4.2 E6. */
    public static final String CHECKIN_CONFLICT =
        "Dữ liệu lượt check-in đang được xử lý đồng thời bởi một thao tác khác. Vui lòng thử lại.";
    /** SRS F4 UC-F4.2 E4. */
    public static final String SESSIONS_USED_UP =
        "Gói tập theo buổi của hội viên đã hết lượt sử dụng. Vui lòng gia hạn thêm gói mới.";

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
    private final EntityManager entities;

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
     *   <li>Gói SESSION đang giữ đã về 0 buổi (vẫn ACTIVE tới hết hôm nay): ngày mai.</li>
     * </ul>
     * Không bao giờ sớm hơn hôm nay (gói cũ quá hạn mà tác vụ nền chưa chuyển EXPIRED).
     *
     * @param held gói ACTIVE/FROZEN cùng loại, cùng phạm vi chi nhánh; {@code null} nếu chưa có
     */
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

    // ---------------------------------------------------------------------------------------------
    // S2-09.1: kích hoạt gói khi hóa đơn PAID, hủy gói khi hủy hóa đơn chờ
    // ---------------------------------------------------------------------------------------------

    /**
     * Kích hoạt gói PENDING của một dòng hóa đơn vừa thu tiền (UC-F5.2 bước 5d, BR-F5-04, BR-F3-11, BR-F3-12).
     * Nơi gọi <b>phải đã khóa dòng hội viên</b> ({@code MemberService.lockForUpdate}) trong cùng giao dịch, để hai
     * hóa đơn cùng loại thu tiền đồng thời không cùng qua bước kiểm tra E6b.
     * <ul>
     *   <li>Đã có gói cùng loại, cùng phạm vi chi nhánh đang UPCOMING: 422 E6b.</li>
     *   <li>Chưa có gói cùng loại ACTIVE/FROZEN: ACTIVE từ hôm nay (giờ Việt Nam).</li>
     *   <li>Có: UPCOMING từ {@link #plannedStartDate} (sau {@code end_date} gói cũ; chỉ SESSION 0 buổi bắt đầu ngày mai).</li>
     * </ul>
     * Số ngày hiệu lực giữ đúng số đã chốt lúc lập hóa đơn ({@link Membership#durationDays()}). Không đổi gói cũ.
     *
     * @return gói sau khi kích hoạt
     * @throws BusinessRuleException (422) đã có gói cùng loại UPCOMING (E6b)
     * @throws IllegalStateException dòng hóa đơn không có gói PENDING (lỗi dữ liệu)
     */
    @Transactional
    public Membership activateForPaidInvoice(Long invoiceItemId) {
        Membership pending = memberships.findByInvoiceItemId(invoiceItemId)
            .orElseThrow(() -> new IllegalStateException("Dòng hóa đơn " + invoiceItemId + " không có gói tập."));
        List<Membership> others = memberships.findSameType(pending.getMemberId(), pending.getPackageType(),
            pending.getBranchId(), EnumSet.of(MembershipStatus.UPCOMING, MembershipStatus.ACTIVE,
                MembershipStatus.FROZEN));
        if (others.stream().anyMatch(m -> m.getStatus() == MembershipStatus.UPCOMING)) {
            throw new BusinessRuleException(UPCOMING_EXISTS);
        }
        LocalDate today = time.today();
        Membership held = others.stream().filter(m -> HELD.contains(m.getStatus())).findFirst().orElse(null);
        if (held == null) {
            pending.activate(today);
        } else {
            pending.scheduleFrom(plannedStartDate(held, today));
        }
        return memberships.saveAndFlush(pending);
    }

    /**
     * Hủy gói PENDING đi kèm một dòng hóa đơn chờ bị hủy (BR-F5-08).
     *
     * @throws IllegalStateException gói của hóa đơn chờ không còn PENDING (lỗi dữ liệu): rollback cả việc hủy hóa
     *     đơn, không để gói đang dùng được gắn với hóa đơn đã hủy
     */
    @Transactional
    public void cancelPending(Long invoiceItemId) {
        memberships.findByInvoiceItemId(invoiceItemId).ifPresent(m -> {
            m.cancelPending();
            memberships.saveAndFlush(m);
        });
    }

    // ---------------------------------------------------------------------------------------------
    // S2-11.1: trừ buổi khi check-in tại quầy (UC-F4.2 bước 5, BR-F4.3, BR-F4.7)
    // ---------------------------------------------------------------------------------------------

    /**
     * Trừ một buổi của gói SESSION khi check-in lần đầu trong ngày. Khóa dòng gói ({@code FOR UPDATE}) và nạp lại số
     * buổi, trạng thái. Về 0 thì gói vẫn ACTIVE đến hết ngày, và các gói UPCOMING cùng hội viên, cùng loại, cùng
     * phạm vi chi nhánh được dời sớm {@code D} ngày để gói sớm nhất bắt đầu từ ngày mai (BR-F4.7; {@code D <= 0} thì giữ nguyên).
     *
     * <p>Nơi gọi <b>phải đã khóa</b> {@code members} (thứ tự members → memberships → class_bookings) và tự kiểm tra
     * "hôm nay đã trừ chưa" (bảng {@code checkins} thuộc F4). Ghi bằng {@code saveAndFlush}, nên lỗi {@code @Version}
     * lộ ra ngay tại đây; nơi gọi đổi thành 409 (E6).
     *
     * @param today ngày Việt Nam của lượt check-in
     * @return gói sau khi trừ
     * @throws BusinessRuleException (422) gói đã hết buổi (E4)
     * @throws ConflictException (409) gói vừa bị thao tác khác đổi khỏi ACTIVE (E6), lễ tân bấm lại để chọn lại gói
     */
    @Transactional
    public Membership deductSessionForCheckin(Long membershipId, LocalDate today) {
        Membership membership = memberships.findById(membershipId)
            .orElseThrow(() -> new IllegalStateException("Không có gói " + membershipId));
        // Nơi gọi thường đã nạp gói này (findUsable) trong cùng giao dịch; truy vấn khóa sẽ trả lại bản đã nạp mà không
        // đọc lại cột. refresh kèm khóa vừa SELECT ... FOR UPDATE vừa nạp lại số buổi, trạng thái mới nhất.
        entities.refresh(membership, LockModeType.PESSIMISTIC_WRITE);
        if (membership.getPackageType() != PackageType.SESSION || membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new ConflictException(CHECKIN_CONFLICT);
        }
        if (membership.getRemainingSessions() == null || membership.getRemainingSessions() <= 0) {
            throw new BusinessRuleException(SESSIONS_USED_UP);
        }
        membership.deductOneSession();
        memberships.saveAndFlush(membership);
        if (membership.getRemainingSessions() == 0) {
            pullUpcomingToTomorrow(membership, today);
        }
        return membership;
    }

    /** BR-F4.7: dời mọi gói UPCOMING cùng nhóm sớm {@code D} ngày, giữ thứ tự nối tiếp và số ngày hiệu lực. */
    private void pullUpcomingToTomorrow(Membership finished, LocalDate today) {
        List<Membership> upcoming = memberships.findSameType(finished.getMemberId(), finished.getPackageType(),
            finished.getBranchId(), EnumSet.of(MembershipStatus.UPCOMING));
        LocalDate earliest = upcoming.stream().map(Membership::getStartDate).min(LocalDate::compareTo).orElse(null);
        if (earliest == null) {
            return;
        }
        long days = earliest.toEpochDay() - today.plusDays(1).toEpochDay();
        if (days <= 0) {
            return;
        }
        for (Membership next : upcoming) {
            next.moveEarlier(days);
            memberships.saveAndFlush(next);
        }
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
     * @throws NotFoundException (404) hội viên không có hoặc ngoài chi nhánh
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

    // ---------------------------------------------------------------------------------------------
    // S2-14.1: hội viên gia hạn gói của mình trên cổng (UC-F7.5)
    // ---------------------------------------------------------------------------------------------

    /**
     * Gói {@code membershipId} của đúng hội viên {@code memberId} (UC-F7.5 bước 4). Nơi gọi chỉ đọc entity trả về.
     *
     * <p>Gói của hội viên khác trả 404 giống gói không tồn tại, như mọi API khác của hội viên, để không lộ
     * mã gói nào đang có (trưởng nhóm chọn VD-30 = B, 22/11; SRS UC-F7.5 E2 còn ghi 403, chờ sửa).
     *
     * @throws NotFoundException (404) gói không tồn tại hoặc của hội viên khác (E2, E3)
     */
    public Membership requireOwnedBy(Long membershipId, Long memberId) {
        return memberships.findById(membershipId)
            .filter(membership -> membership.getMemberId().equals(memberId))
            .orElseThrow(() -> new NotFoundException(
                "Gói tập của hội viên không tồn tại trên hệ thống."));
    }

    /** Loại gói + phạm vi chi nhánh (rỗng = toàn chuỗi); hai gói cùng khóa thì tính là "cùng loại". */
    private record ScopeKey(PackageType type, Long branchId) {
        static ScopeKey of(Membership m) {
            return new ScopeKey(m.getPackageType(), m.getBranchId());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // S2-38.1: hủy gói khi hoàn tiền hóa đơn (UC-F5.5 bước 5d, BR-F5-13)
    // ---------------------------------------------------------------------------------------------

    /**
     * Hủy gói của một dòng hóa đơn vừa hoàn tiền: ACTIVE/UPCOMING → CANCELLED. Chỉ đụng đúng gói của dòng này; gói
     * khác của hội viên (kể cả gói UPCOMING nối tiếp mua bằng hóa đơn khác) giữ nguyên trạng thái và ngày (Q5 = A).
     * Không hủy buổi PT hay chỗ đặt lớp: nơi gọi đã chặn hoàn tiền khi gói đã dùng (BR-F5-14).
     *
     * <p>Dòng không có gói (dữ liệu cũ không qua luồng bán) thì bỏ qua.
     *
     * @throws IllegalStateException gói không ACTIVE/UPCOMING (nơi gọi đã kiểm tra trước): rollback cả việc hoàn tiền
     */
    @Transactional
    public void cancelForRefund(Long invoiceItemId) {
        memberships.findByInvoiceItemId(invoiceItemId).ifPresent(m -> {
            m.cancelForRefund();
            memberships.saveAndFlush(m);
        });
    }

    // ---------------------------------------------------------------------------------------------
    // S2-19.3: trừ buổi PT khi PT xác nhận buổi hoặc ghi vắng (UC-F9.3, BR-F9.6, BR-F9.7, BR-F9.18)
    // ---------------------------------------------------------------------------------------------

    /** Gói PT của buổi không ở trạng thái trừ được (UC-F9.3 tiền điều kiện "gói PT hợp lệ"). */
    public static final String PT_PACKAGE_UNUSABLE =
        "Gói PT của buổi này không còn hiệu lực, không trừ buổi được. Vui lòng liên hệ quản lý chi nhánh.";
    /** Gói PT của buổi đã hết buổi (buổi đã qua giờ còn sót lại khi gói về 0). */
    public static final String PT_SESSIONS_USED_UP =
        "Gói PT của buổi này đã hết buổi, không trừ thêm được. Vui lòng liên hệ quản lý chi nhánh.";

    /**
     * Kết quả trừ buổi PT.
     *
     * @param deducted gói vừa bị trừ
     * @param expiredNow {@code true} nếu lần trừ này làm gói về 0 và EXPIRED (BR-F9.7)
     * @param activatedNext gói PT nối tiếp vừa được kéo về ACTIVE hôm nay; {@code null} nếu không có
     */
    public record PtDeduction(Membership deducted, boolean expiredNow, Membership activatedNext) { }

    /**
     * Trừ một buổi của gói PT khi buổi PT chuyển {@code DONE} hoặc {@code NO_SHOW} (BR-F9.6, BR-F9.18).
     * <ul>
     *   <li>Gói ACTIVE: trừ 1. Về 0 thì gói EXPIRED ngay và flush (nhường chỉ mục {@code uq_one_active_per_type}),
     *       rồi gói PT UPCOMING sớm nhất cùng hội viên, cùng phạm vi chi nhánh được kéo về bắt đầu {@code today} và
     *       ACTIVE, giữ số ngày hiệu lực (BR-F9.7). Gói UPCOMING sau nó (nếu có) dời sớm cùng số ngày.</li>
     *   <li>Gói đã EXPIRED vì hết ngày, còn buổi, và {@code sessionDate} nằm trong hạn gói (quản lý xác nhận muộn):
     *       chỉ trừ 1, không đổi trạng thái gói nào (trưởng nhóm chốt 20/11).</li>
     * </ul>
     *
     * <p>Nơi gọi <b>phải đã khóa</b> {@code members} (thứ tự members → memberships → pt_sessions, khớp check-in F4) và tự
     * bảo đảm mỗi buổi chỉ trừ một lần (trạng thái buổi thuộc F9). Hàm khóa dòng gói ({@code FOR UPDATE}) và nạp lại.
     *
     * @param sessionDate ngày Việt Nam của buổi PT
     * @param today ngày Việt Nam lúc xác nhận
     * @throws BusinessRuleException (422) gói không còn hiệu lực hoặc đã hết buổi
     */
    @Transactional
    public PtDeduction deductPtSession(Long membershipId, LocalDate sessionDate, LocalDate today) {
        Membership membership = memberships.findById(membershipId)
            .orElseThrow(() -> new IllegalStateException("Không có gói " + membershipId));
        // Như deductSessionForCheckin: refresh kèm khóa để vừa SELECT ... FOR UPDATE vừa nạp lại số buổi, trạng thái.
        entities.refresh(membership, LockModeType.PESSIMISTIC_WRITE);
        if (membership.getPackageType() != PackageType.PT) {
            throw new IllegalStateException("Gói " + membershipId + " không phải gói PT");
        }
        boolean hasSessions = membership.getRemainingSessions() != null && membership.getRemainingSessions() > 0;
        if (membership.getStatus() == MembershipStatus.EXPIRED) {
            if (!hasSessions) {
                throw new BusinessRuleException(PT_SESSIONS_USED_UP);
            }
            if (sessionDate.isBefore(membership.getStartDate()) || sessionDate.isAfter(membership.getEndDate())) {
                throw new BusinessRuleException(PT_PACKAGE_UNUSABLE);
            }
            membership.deductSessionAfterExpiry();
            memberships.saveAndFlush(membership);
            return new PtDeduction(membership, false, null);
        }
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new BusinessRuleException(PT_PACKAGE_UNUSABLE);
        }
        if (!hasSessions) {
            throw new BusinessRuleException(PT_SESSIONS_USED_UP);
        }
        membership.deductOneSession();
        memberships.saveAndFlush(membership);
        if (membership.getRemainingSessions() > 0) {
            return new PtDeduction(membership, false, null);
        }
        membership.expireAfterLastPtSession();
        // Flush trước để gói nối tiếp lên ACTIVE không vướng chỉ mục uq_one_active_per_type (BR-F9.7).
        memberships.saveAndFlush(membership);
        return new PtDeduction(membership, true, activateNextPtMembership(membership, today));
    }

    /**
     * BR-F9.7: gói PT UPCOMING sớm nhất cùng nhóm bắt đầu {@code today} và ACTIVE; gói UPCOMING sau nó (thường không
     * có, vì BR-F3-11 chỉ cho một gói chờ) dời sớm cùng số ngày để giữ thứ tự nối tiếp.
     */
    private Membership activateNextPtMembership(Membership finished, LocalDate today) {
        List<Membership> upcoming = memberships.findSameType(finished.getMemberId(), finished.getPackageType(),
            finished.getBranchId(), EnumSet.of(MembershipStatus.UPCOMING));
        Membership next = upcoming.stream()
            .min(Comparator.comparing(Membership::getStartDate).thenComparing(Membership::getId))
            .orElse(null);
        if (next == null) {
            return null;
        }
        long shift = next.getStartDate().toEpochDay() - today.toEpochDay();
        // Gói đã tới ngày bắt đầu mà tác vụ nền chưa kích hoạt: giữ nguyên ngày, không dời muộn hơn (không kéo dài hạn).
        next.activateEarly(shift > 0 ? today : next.getStartDate());
        memberships.saveAndFlush(next);
        if (shift > 0) {
            for (Membership later : upcoming) {
                if (later != next) {
                    later.moveEarlier(shift);
                    memberships.saveAndFlush(later);
                }
            }
        }
        return next;
    }

    // ---------------------------------------------------------------------------------------------
    // S2-07.2: tác vụ nền 00:00 – bước 1 chuyển EXPIRED, bước 2 kích hoạt UPCOMING (AC-F3.5.1-10, BR-F4.7).
    // Thứ tự và giao dịch do MembershipLifecycleService.runDaily giữ; hai hàm dưới chỉ là từng bước.
    // ---------------------------------------------------------------------------------------------

    /**
     * Bước 1: chuyển EXPIRED mọi gói ACTIVE có {@code end_date < today}, và gói SESSION 0 buổi mà hôm nay chưa trừ buổi.
     * Flush trước khi trả về, để bước 2 không vướng chỉ mục {@code uq_one_active_per_type}.
     *
     * @return id các gói vừa EXPIRED
     */
    @Transactional
    public List<Long> expireOverdue(LocalDate today) {
        List<Membership> due = memberships.findDueToExpire(today);
        due.forEach(Membership::expire);
        memberships.saveAllAndFlush(due);
        return due.stream().map(Membership::getId).toList();
    }

    /**
     * Kết quả bước 2.
     *
     * @param activated gói UPCOMING vừa ACTIVE
     * @param expiredUnstarted gói UPCOMING mà cả hạn đã trôi qua lúc máy chủ tắt, chuyển thẳng EXPIRED
     * @param skipped gói UPCOMING tới ngày nhưng cùng nhóm còn gói ACTIVE/FROZEN, giữ nguyên chờ lần chạy sau
     */
    public record UpcomingActivation(List<Long> activated, List<Long> expiredUnstarted, List<Long> skipped) { }

    /**
     * Bước 2: kích hoạt gói UPCOMING có {@code start_date <= today}, giữ nguyên ngày đã xếp. Xét theo hội viên, ngày bắt
     * đầu tăng dần, nên chuỗi nối tiếp được xử lý đúng thứ tự:
     * <ul>
     *   <li>Cùng hội viên, cùng loại, cùng phạm vi chi nhánh còn gói ACTIVE/FROZEN: bỏ qua (dữ liệu lệch, hoặc gói đang
     *       bảo lưu ở v3), không để một dòng lỗi làm hỏng cả lượt chạy. Xét điều này trước tiên.</li>
     *   <li>{@code end_date < today} (cả hạn trôi qua khi máy chủ tắt nhiều ngày): EXPIRED thẳng.</li>
     *   <li>Còn lại: ACTIVE, flush ngay để gói sau trong cùng nhóm thấy chỉ mục đã có chủ.</li>
     * </ul>
     * Gọi <b>sau</b> {@link #expireOverdue} trong cùng giao dịch.
     */
    @Transactional
    public UpcomingActivation activateDueUpcoming(LocalDate today) {
        List<Long> activated = new ArrayList<>();
        List<Long> expiredUnstarted = new ArrayList<>();
        List<Long> skipped = new ArrayList<>();
        for (Membership next : memberships.findDueUpcoming(today)) {
            // Kiểm tra nhóm trước: gói chờ sau gói FROZEN (bảo lưu) không được EXPIRED dù đã quá end_date.
            if (!memberships.findSameType(next.getMemberId(), next.getPackageType(), next.getBranchId(),
                    HELD).isEmpty()) {
                skipped.add(next.getId());
            } else if (next.getEndDate().isBefore(today)) {
                next.expireUnstarted();
                memberships.saveAndFlush(next);
                expiredUnstarted.add(next.getId());
            } else {
                next.activateOnSchedule();
                memberships.saveAndFlush(next);
                activated.add(next.getId());
            }
        }
        return new UpcomingActivation(List.copyOf(activated), List.copyOf(expiredUnstarted), List.copyOf(skipped));
    }
}
                expiredUnstarted.add(next.getId());
                expired.add(next.getId());
            } else {
                next.activateOnSchedule();
                memberships.saveAndFlush(next);
                activated.add(next.getId());
            }
        }
        return new UpcomingActivation(activated, expiredUnstarted, skipped);
        return new UpcomingActivation(List.copyOf(activated), List.copyOf(expired), List.copyOf(skipped));
    }

    /** Loại gói + phạm vi chi nhánh (rỗng = toàn chuỗi); hai gói cùng khóa thì tính là "cùng loại". */
    private record ScopeKey(PackageType type, Long branchId) {
        static ScopeKey of(Membership m) {
            return new ScopeKey(m.getPackageType(), m.getBranchId());
        }
    }
}
