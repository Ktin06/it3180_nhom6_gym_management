package vn.edu.hust.nhom6.gym.feature.checkin.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
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
import vn.edu.hust.nhom6.gym.common.util.PhoneUtils;
import vn.edu.hust.nhom6.gym.feature.branch.repository.BranchLookupRepository.OpeningHours;
import vn.edu.hust.nhom6.gym.feature.branch.service.BranchLookupService;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckinHistoryItemDto;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckinResultDto;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckinResultDto.AppliedMembership;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckinStatus;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckoutResultDto;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.ClassArrivalDto;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.MemberSearchDto;
import vn.edu.hust.nhom6.gym.feature.checkin.entity.Checkin;
import vn.edu.hust.nhom6.gym.feature.checkin.entity.CheckinMethod;
import vn.edu.hust.nhom6.gym.feature.checkin.repository.CheckinLookupRepository;
import vn.edu.hust.nhom6.gym.feature.checkin.repository.CheckinLookupRepository.FreezeRow;
import vn.edu.hust.nhom6.gym.feature.checkin.repository.CheckinLookupRepository.HistoryRow;
import vn.edu.hust.nhom6.gym.feature.checkin.repository.CheckinLookupRepository.MembershipRow;
import vn.edu.hust.nhom6.gym.feature.checkin.repository.CheckinRepository;
import vn.edu.hust.nhom6.gym.feature.groupclass.service.ClassBookingService;
import vn.edu.hust.nhom6.gym.feature.member.entity.Member;
import vn.edu.hust.nhom6.gym.feature.member.entity.MemberStatus;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.membership.entity.Membership;
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipService;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;

/**
 * Tra cứu hội viên tại quầy (UC-F4.1), check-in thủ công (UC-F4.2) và ghi đã đến lớp nhóm (UC-F4.3). Vai trò
 * ({@code RECEPTIONIST}, {@code BRANCH_MANAGER}) kiểm ở controller; service kiểm chi nhánh của nhân viên (S3, BR-F4.6):
 * chỉ hội viên có {@code home_branch_id} là chi nhánh của nhân viên (quyết định kick-off 02/11).
 *
 * <p><b>Chọn gói</b> (UC-F4.2 bước 5, gói ACTIVE còn hạn hôm nay, của chi nhánh hoặc toàn chuỗi):
 * <ol>
 *   <li>Có gói TIME → dùng, không trừ buổi (BR-F4.2).</li>
 *   <li>Có gói SESSION → gói đã trừ hôm nay thì dùng lại, không trừ (BR-F4.3, KB6); chưa thì trừ 1 buổi của gói hết hạn
 *       sớm nhất còn buổi; mọi gói đều 0 buổi thì xét tiếp bước 3, không được thì 422 E4.</li>
 *   <li>Hôm nay có buổi PT {@code BOOKED} ở chi nhánh mà gói PT của buổi đó dùng được → dùng gói đó, không trừ buổi
 *       (BR-F4.8).</li>
 *   <li>Không chọn được: 422 theo thứ tự E5 (gói bảo lưu) → E4 (gói SESSION hết buổi) → E3 (chỉ có PT) → E2.</li>
 * </ol>
 *
 * <p><b>Khóa dòng (NF6):</b> {@code members} → {@code memberships} (chỉ khi trừ buổi) → {@code class_bookings}, khớp
 * thứ tự của F8 (hủy chỗ, điểm danh: members → … → class_bookings). Khóa {@code members} <b>không chờ</b> (NOWAIT,
 * SRS F4 Scenario 7): hai lễ tân cùng check-in một hội viên thì yêu cầu sau nhận 409 (E6) ngay; bấm lại sau khi yêu cầu
 * trước commit thì thấy lượt trừ buổi đã có, 201 không trừ thêm. Tác dụng phụ: hội viên đang đặt lớp, đặt PT đúng lúc
 * đó cũng làm check-in nhận 409, bấm lại là được. Chỉ mục {@code uq_one_deduction_per_day} và {@code @Version} của gói
 * vẫn là lưới an toàn (409 E6).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckinService {
    static final String BAD_KEYWORD =
        "Vui lòng nhập mã hội viên hợp lệ (ví dụ: HV000123) hoặc số điện thoại 10 chữ số.";
    static final String MEMBER_NOT_FOUND =
        "Không tìm thấy thông tin hội viên trong hệ thống. Vui lòng kiểm tra lại mã hoặc số điện thoại.";
    static final String CLOSED = "Phòng tập hiện đang đóng cửa (Giờ mở cửa: %s - %s). Không thể thực hiện check-in.";
    static final String NO_PACKAGE =
        "Hội viên không có gói tập nào đang hoạt động. Vui lòng đăng ký hoặc gia hạn gói tập tại quầy.";
    static final String PT_ONLY = "Gói huấn luyện viên cá nhân (PT) chỉ dùng cho các buổi tập 1-1 có lịch hẹn, không "
        + "dùng để check-in tự do vào phòng tập. Hội viên cần có thêm gói theo thời gian hoặc theo lượt.";
    static final String FROZEN = "Gói tập của hội viên đang trong thời gian bảo lưu%s. Vui lòng liên hệ quản lý để hủy "
        + "bảo lưu sớm nếu muốn tập tiếp.";
    static final String INACTIVE = "Hồ sơ hội viên này đã ngừng hoạt động. Vui lòng liên hệ quản lý để được hỗ trợ.";
    static final String CONCURRENT = MembershipService.CHECKIN_CONFLICT;

    /** UC-F4.3 (G5, VD-05): lớp bắt đầu từ {@code now − 15 phút} đến {@code now + 60 phút}, hai đầu đều tính. */
    static final int ARRIVAL_LATE_MINUTES = 15;
    static final int ARRIVAL_EARLY_MINUTES = 60;

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CheckinRepository checkins;
    private final CheckinLookupRepository lookup;
    private final MemberService members;
    private final MembershipService memberships;
    private final ClassBookingService classBookings;
    private final StaffService staff;
    private final BranchLookupService branches;
    private final VnTime time;

    // ---------------------------------------------------------------------------------------------
    // UC-F4.1: tra cứu
    // ---------------------------------------------------------------------------------------------

    /**
     * Tra cứu theo mã hội viên hoặc số điện thoại (UC-F4.1). Hồ sơ ngừng hoạt động vẫn trả 200 kèm {@code status} (E3).
     *
     * @throws InvalidRequestException (400) từ khóa rỗng hoặc sai định dạng (E1)
     * @throws NotFoundException (404) không có, hoặc hội viên chi nhánh khác (E2)
     */
    public MemberSearchDto search(CurrentUser me, String keyword) {
        Long branchId = staff.requireBranchId(me.userId());
        Long memberId = findByKeyword(keyword).orElseThrow(() -> new NotFoundException(MEMBER_NOT_FOUND));
        Member member = requireInBranch(me, memberId);
        OffsetDateTime now = time.now();
        LocalDate today = time.today();
        List<MemberSearchDto.HeldPackage> held = lookup.findMemberships(memberId).stream()
            .map(CheckinService::held).toList();
        ClassArrivalDto upcoming = lookup.findUpcomingClass(memberId, branchId,
            now.minusMinutes(ARRIVAL_LATE_MINUTES), time.dayEnd(today)).map(ClassArrivalDto::of).orElse(null);
        return new MemberSearchDto(member.getId(), member.getMemberCode(), member.getFullName(), member.getPhone(),
            member.getGender() == null ? null : member.getGender().name(), lookup.hasAvatar(memberId),
            member.getStatus(), checkins.existsOpen(memberId, branchId, today), held, upcoming);
    }

    /** Chuẩn hóa từ khóa (UC-F4.1 bước 2): bỏ khoảng trắng, dấu chấm, gạch; mã viết hoa; SĐT {@code +84} về {@code 0}. */
    private Optional<Long> findByKeyword(String keyword) {
        String cleaned = keyword == null ? "" : keyword.replaceAll("[\\s.\\-]", "").toUpperCase();
        if (cleaned.matches("HV\\d{6}")) {
            return lookup.findMemberIdByCode(cleaned);
        }
        if (cleaned.matches("(\\+84|84)\\d{9}|0\\d{9}")) {
            return lookup.findMemberIdByPhone(PhoneUtils.normalizePhone(cleaned));
        }
        throw new InvalidRequestException(BAD_KEYWORD);
    }

    private static MemberSearchDto.HeldPackage held(MembershipRow r) {
        return new MemberSearchDto.HeldPackage(r.id(), r.packageName(), r.packageType(), r.branchId(), r.status(),
            r.startDate(), r.endDate(), r.totalSessions(), r.remainingSessions());
    }

    // ---------------------------------------------------------------------------------------------
    // UC-F4.2 + UC-F4.3: check-in
    // ---------------------------------------------------------------------------------------------

    /**
     * Check-in thủ công tại quầy (UC-F4.2), rồi ghi {@code arrived_at} cho lớp nhóm sắp diễn ra (UC-F4.3), trong một
     * giao dịch. Thất bại thì không tạo dòng {@code checkins}, không trừ buổi, không ghi {@code arrived_at}.
     *
     * @throws org.springframework.security.access.AccessDeniedException (403) nhân viên không có chi nhánh
     * @throws NotFoundException (404) không có hội viên hoặc hội viên chi nhánh khác
     * @throws BusinessRuleException (422) ngoài giờ mở cửa (E1), không có gói (E2), chỉ có gói PT mà hôm nay không có
     *         buổi PT (E3), gói SESSION hết buổi (E4), gói đang bảo lưu (E5), hồ sơ ngừng hoạt động (E7)
     * @throws ConflictException (409) xung đột đồng thời ở chỉ mục trừ buổi hoặc {@code @Version} của gói (E6)
     */
    @Transactional
    public CheckinResultDto checkin(CurrentUser me, Long memberId) {
        Long branchId = staff.requireBranchId(me.userId());
        OffsetDateTime now = time.now();
        LocalDate today = time.today();
        requireOpen(branchId, now);
        // NF6: khóa members trước; mọi điều kiện dưới đây đọc sau khóa. Không nạp hồ sơ trước khóa, để trạng thái
        // đọc được là bản mới nhất (entity đã nạp sẵn sẽ không được đọc lại khi khóa).
        // Scenario 7 (VD-20 GĐ-1): khóa không chờ. Dòng hội viên đang bị một giao dịch khác giữ (lễ tân khác đang
        // check-in, hoặc hội viên đang đặt lớp, đặt PT) thì 409 E6 ngay; bấm lại sau đó thì 201, không trừ thêm buổi.
        Member member;
        try {
            member = members.lockForUpdateNoWait(memberId);
        } catch (NotFoundException ex) {
            throw new NotFoundException(MEMBER_NOT_FOUND);
        } catch (PessimisticLockingFailureException ex) {
            throw new ConflictException(CONCURRENT);
        }
        if (!member.getHomeBranchId().equals(branchId)) {
            throw new NotFoundException(MEMBER_NOT_FOUND);
        }
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessRuleException(INACTIVE);
        }
        try {
            Choice choice = choose(memberId, branchId, today);
            Membership applied = choice.deduct()
                ? memberships.deductSessionForCheckin(choice.membership().getId(), today)
                : choice.membership();
            Checkin saved = checkins.saveAndFlush(Checkin.manual(memberId, applied.getId(), branchId, me.userId(), today,
                choice.deduct(), now));

            List<Long> marked = classBookings.markArrived(memberId, branchId,
                now.minusMinutes(ARRIVAL_LATE_MINUTES), now.plusMinutes(ARRIVAL_EARLY_MINUTES), now);
            List<ClassArrivalDto> arrivals = lookup.findClassRows(marked).stream().map(ClassArrivalDto::of).toList();

            String packageName = lookup.findMemberships(memberId).stream()
                .filter(r -> r.id().equals(applied.getId())).map(MembershipRow::packageName).findFirst().orElse(null);
            return new CheckinResultDto(saved.getId(), memberId, member.getMemberCode(), member.getFullName(),
                new AppliedMembership(applied.getId(), packageName, applied.getPackageType(), choice.deduct(),
                    applied.getPackageType() == PackageType.TIME ? null : applied.getRemainingSessions()),
                today, now, !arrivals.isEmpty(), arrivals, CheckinResultDto.SUCCESS);
        } catch (OptimisticLockingFailureException | PessimisticLockingFailureException ex) {
            // E6: không thử lại trong giao dịch đã hỏng; lễ tân bấm lại thì thấy lượt trừ buổi đã có.
            throw new ConflictException(CONCURRENT);
        } catch (DataIntegrityViolationException ex) {
            if (String.valueOf(ex.getMostSpecificCause().getMessage()).contains("uq_one_deduction_per_day")) {
                throw new ConflictException(CONCURRENT);
            }
            throw ex;
        }
    }

    /** Gói được chọn và có trừ buổi không. */
    private record Choice(Membership membership, boolean deduct) { }

    private Choice choose(Long memberId, Long branchId, LocalDate today) {
        List<Membership> timePackages = memberships.findUsable(memberId, branchId, PackageType.TIME, today);
        if (!timePackages.isEmpty()) {
            return new Choice(timePackages.getFirst(), false);
        }
        List<Membership> sessions = memberships.findUsable(memberId, branchId, PackageType.SESSION, today);
        if (!sessions.isEmpty()) {
            List<Long> deductedToday = checkins.findDeductedMembershipIds(memberId, today);
            Optional<Membership> already = sessions.stream().filter(m -> deductedToday.contains(m.getId())).findFirst();
            if (already.isPresent()) {
                return new Choice(already.get(), false);
            }
            Optional<Membership> withSessions = sessions.stream()
                .filter(m -> m.getRemainingSessions() != null && m.getRemainingSessions() > 0).findFirst();
            if (withSessions.isPresent()) {
                return new Choice(withSessions.get(), true);
            }
        }
        // Gói SESSION còn 0 buổi (chưa trừ hôm nay) không còn dùng được: vẫn xét gói PT, rồi mới báo E4.
        List<Membership> pt = memberships.findUsable(memberId, branchId, PackageType.PT, today);
        if (!pt.isEmpty()) {
            List<Long> ptToday = lookup.findBookedPtMembershipIds(memberId, branchId, time.dayStart(today),
                time.dayEnd(today));
            // BR-F4.8: chỉ gói PT của chính buổi PT hôm nay (gói đó phải còn dùng được); không gắn sang gói PT khác.
            Optional<Membership> ofSession = pt.stream().filter(m -> ptToday.contains(m.getId())).findFirst();
            if (ofSession.isPresent()) {
                return new Choice(ofSession.get(), false);
            }
        }
        Optional<FreezeRow> frozen = lookup.findFrozen(memberId, branchId);
        if (frozen.isPresent()) {
            throw new BusinessRuleException(String.format(FROZEN, freezeRange(frozen.get())));
        }
        if (!sessions.isEmpty()) {
            throw new BusinessRuleException(MembershipService.SESSIONS_USED_UP);
        }
        throw new BusinessRuleException(pt.isEmpty() ? NO_PACKAGE : PT_ONLY);
    }

    private static String freezeRange(FreezeRow row) {
        if (row.fromDate() == null || row.toDate() == null) {
            return "";
        }
        return " (từ ngày " + row.fromDate().format(DD_MM_YYYY) + " đến ngày " + row.toDate().format(DD_MM_YYYY) + ")";
    }

    /** BR-F4.4, E1: mở cửa khi {@code open_time <= giờ Việt Nam hiện tại < close_time}. */
    private void requireOpen(Long branchId, OffsetDateTime now) {
        OpeningHours hours = branches.openingHours(branchId);
        LocalTime vnNow = now.atZoneSameInstant(VnTime.ZONE).toLocalTime();
        if (vnNow.isBefore(hours.open()) || !vnNow.isBefore(hours.close())) {
            throw new BusinessRuleException(String.format(CLOSED, hours.open().format(HH_MM),
                hours.close().format(HH_MM)));
        }
    }

    /**
     * Hồ sơ trong chi nhánh của nhân viên (đọc, không khóa). Khác chi nhánh hoặc không có thì 404 câu E2 (S3); nơi gọi đã
     * chặn chủ chuỗi bằng {@code StaffService.requireBranchId}.
     */
    private Member requireInBranch(CurrentUser me, Long memberId) {
        try {
            return members.requireInStaffScope(me, memberId);
        } catch (NotFoundException ex) {
            throw new NotFoundException(MEMBER_NOT_FOUND);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // S2-11.2 · UC-F4.4 (check-out) + UC-F4.5 (lịch sử check-in trong ngày)
    // ---------------------------------------------------------------------------------------------

    static final String CHECKOUT_NOT_FOUND = "Hội viên không có lượt tập nào đang diễn ra cần check-out.";

    /**
     * Ghi nhận giờ ra về cho một lượt check-in đang mở thuộc chi nhánh nhân viên (UC-F4.4). Khóa dòng {@code checkins}
     * ({@code PESSIMISTIC_WRITE}) trước khi đọc {@code checkedOutAt} để hai lễ tân bấm cùng lúc chỉ ghi đúng một lần.
     * Lượt đã check-out, thuộc chi nhánh khác, hoặc không tồn tại đều trả 404 cùng câu E1 (S3).
     *
     * @throws org.springframework.security.access.AccessDeniedException (403) nhân viên không có chi nhánh
     * @throws NotFoundException (404) không có lượt đang mở thuộc chi nhánh (E1)
     */
    @Transactional
    public CheckoutResultDto checkout(CurrentUser me, Long checkinId) {
        Long branchId = staff.requireBranchId(me.userId());
        Checkin checkin = checkins.findByIdAndBranchIdForUpdate(checkinId, branchId)
            .orElseThrow(() -> new NotFoundException(CHECKOUT_NOT_FOUND));
        if (checkin.getCheckedOutAt() != null) {
            throw new NotFoundException(CHECKOUT_NOT_FOUND);
        }
        OffsetDateTime now = time.now();
        checkin.checkOut(now);
        checkins.saveAndFlush(checkin);
        long durationMinutes = Duration.between(checkin.getCheckedInAt(), now).toMinutes();
        return new CheckoutResultDto(checkin.getId(), now, durationMinutes, CheckoutResultDto.SUCCESS);
    }

    /**
     * Lịch sử check-in theo ngày tại chi nhánh nhân viên (UC-F4.5), mặc định hôm nay theo ngày Việt Nam. Lọc nửa mở
     * {@code [VnTime.dayStart(date), VnTime.dayEnd(date))} trên {@code checked_in_at} để dùng chỉ mục
     * {@code (branch_id, checked_in_at)}, sắp xếp mới nhất trước.
     */
    public PageResponse<CheckinHistoryItemDto> history(CurrentUser me, LocalDate date, CheckinStatus status,
            CheckinMethod method, String keyword, int page, int size) {
        Long branchId = staff.requireBranchId(me.userId());
        LocalDate targetDate = date == null ? time.today() : date;
        OffsetDateTime from = time.dayStart(targetDate);
        OffsetDateTime to = time.dayEnd(targetDate);
        String trimmed = keyword == null ? "" : keyword.trim();
        String keywordPattern = trimmed.isEmpty() ? null
            : "%" + escapeLike(trimmed.toLowerCase(Locale.ROOT)) + "%";
        return PageResponse.of(lookup.findHistory(branchId, from, to, status, method, keywordPattern, page, size),
            CheckinService::historyItem);
    }

    private static CheckinHistoryItemDto historyItem(HistoryRow r) {
        Long durationMinutes = r.checkedOutAt() == null ? null
            : Duration.between(r.checkedInAt(), r.checkedOutAt()).toMinutes();
        CheckinStatus status = r.checkedOutAt() == null ? CheckinStatus.CHECKED_IN : CheckinStatus.CHECKED_OUT;
        return new CheckinHistoryItemDto(r.checkinId(), r.memberId(), r.memberCode(), r.fullName(), r.hasAvatar(),
            r.membershipId(), r.packageName(), r.packageType(), r.method(), r.sessionDeducted(), r.checkinDate(),
            r.checkedInAt(), r.checkedOutAt(), durationMinutes, status, r.createdBy(), r.createdByName());
    }

    private static String escapeLike(String raw) {
        return raw.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
