package vn.edu.hust.nhom6.gym.feature.member.service;

import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import java.time.LocalDate;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.exception.*;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.member.dto.*;
import vn.edu.hust.nhom6.gym.feature.member.entity.*;
import vn.edu.hust.nhom6.gym.feature.member.mapper.MemberMapper;
import vn.edu.hust.nhom6.gym.feature.member.repository.MemberRepository;
import vn.edu.hust.nhom6.gym.feature.member.repository.MemberAvatarRepository;
import vn.edu.hust.nhom6.gym.feature.auth.service.AccountService;
import vn.edu.hust.nhom6.gym.feature.branch.service.BranchLookupService;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;

/**
 * Nghiệp vụ hồ sơ hội viên (F2).
 *
 * <p>Controller đã kiểm tra vai trò bằng {@code @PreAuthorize} và validate đầu vào. Service này kiểm tra
 * phạm vi chi nhánh, quyền sở hữu và các quy tắc nghiệp vụ. Gọi chức năng khác thẳng qua service của chức
 * năng đó (ADR 0007): F1 {@link AccountService}, F7 {@link StaffService}, chi nhánh {@link BranchLookupService}.
 * F1 ({@code AuthService}) gọi ngược lại {@link #registerMemberProfile} khi tự đăng ký; không có vòng phụ thuộc
 * vì {@code AccountService} không gọi F2.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    public static final String MEMBER_ALREADY_HAS_ACCOUNT = "Hội viên này đã có tài khoản đăng nhập.";
    public static final String INACTIVE_MEMBER_CANNOT_CREATE_ACCOUNT =
        "Chỉ có thể tạo tài khoản cho hồ sơ hội viên đang hoạt động.";
    private static final String PHONE_HAS_PROFILE =
        "Số điện thoại đã có hồ sơ hội viên. Vui lòng liên hệ quầy lễ tân.";
    private static final String PHONE_TAKEN_BY_OTHER = "Số điện thoại đã được đăng ký cho hội viên khác.";

    private final MemberRepository members;
    private final MemberAvatarRepository avatars;
    private final AccountService accounts;
    private final StaffService staff;
    private final BranchLookupService branches;
    private final MemberMapper mapper;
    private final Validator validator;

    @Transactional
    public MemberDetailResponse create(CurrentUser actor, CreateMemberRequest request) {
        Long branch = actor.isOwner() ? branches.defaultBranchId() : staff.requireBranchId(actor.userId());
        Member member = newProfile(null, actor.userId(), branch, request.fullName(), request.phone(),
            request.dob(), request.gender());
        return mapper.detail(save(member, PHONE_HAS_PROFILE), null, false);
    }

    /**
     * Tạo hồ sơ cho tài khoản vừa tự đăng ký (F1 gọi, UC-F1.1). Tham gia giao dịch của F1: trùng số điện thoại
     * thì ném 409 và tài khoản vừa tạo cũng bị hoàn tác. Kiểm tra lại bằng cùng ràng buộc DTO với API tạo hồ sơ.
     */
    @Transactional
    public void registerMemberProfile(Long userId, String fullName, String phone, LocalDate dob, String gender) {
        if (userId == null || userId <= 0) { throw new InvalidRequestException("Tài khoản liên kết không hợp lệ."); }
        Gender parsedGender;
        try {
            parsedGender = gender == null ? null : Gender.valueOf(gender);
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Giới tính không hợp lệ.");
        }
        // Cổng F1 gọi trực tiếp, không đi qua @Valid của controller F2: dùng lại cùng DTO/ràng buộc.
        var request = new CreateMemberRequest(fullName, phone, parsedGender, dob);
        var violations = validator.validate(request);
        if (!violations.isEmpty()) { throw new ConstraintViolationException(violations); }
        Member member = newProfile(userId, userId, branches.defaultBranchId(), fullName, phone, dob, parsedGender);
        save(member, PHONE_HAS_PROFILE);
    }

    private Member newProfile(Long userId, Long createdBy, Long branchId, String fullName,
            String rawPhone, LocalDate dob, Gender gender) {
        String phone = MemberPhoneSupport.normalize(rawPhone);
        if (members.existsByPhone(phone)) {
            throw new ConflictException(PHONE_HAS_PROFILE);
        }
        long code = members.nextMemberCode();
        if (code < 1 || code > 999999) {
            throw new BusinessRuleException("Đã hết dải mã hội viên HV sáu chữ số. Vui lòng liên hệ quản trị viên.");
        }
        Member member = new Member();
        member.setUserId(userId);
        member.setCreatedBy(createdBy);
        member.setHomeBranchId(branchId);
        member.setMemberCode(String.format(Locale.ROOT, "HV%06d", code));
        member.setFullName(fullName.trim());
        member.setPhone(phone);
        member.setGender(gender);
        member.setDob(dob);
        return member;
    }

    /**
     * Hồ sơ hội viên gắn với tài khoản đang đăng nhập. Chức năng khác dùng khi hội viên tự thao tác
     * (xem gói của mình, đặt lớp, đặt buổi PT...) để biết {@code member.id} và chi nhánh gốc.
     * Không lọc theo trạng thái: hồ sơ INACTIVE vẫn trả về, nơi gọi tự quyết có cho làm tiếp không.
     *
     * @throws ForbiddenException (403) khi tài khoản chưa gắn hồ sơ hoặc hồ sơ đã xóa mềm
     */
    public Member requireByUserId(Long userId) {
        return members.findByUserIdAndDeletedAtIsNull(userId).orElseThrow(() -> new ForbiddenException(
            "Tài khoản chưa gắn hồ sơ hội viên. Vui lòng liên hệ quầy lễ tân."));
    }

    /**
     * Tìm hồ sơ chưa xóa mềm gắn với tài khoản đang đăng nhập; trả rỗng nếu chưa gắn hoặc đã xóa mềm.
     * Dùng cho luồng tự quyết định mã lỗi khi không có hồ sơ (ví dụ PDF hóa đơn F5 trả 404).
     */
    public Optional<Member> findByUserId(Long userId) {
        return members.findByUserIdAndDeletedAtIsNull(userId);
    }

    /** Khóa hội viên đến hết giao dịch bán, trước khi kiểm tra gói đang chờ (NF6). */
    @Transactional
    public Member lockForUpdate(Long memberId) {
        return members.findForUpdate(memberId).orElseThrow(() ->
            new NotFoundException("Hội viên không tồn tại trên hệ thống."));
    }

    /**
     * Khóa dòng hội viên (kể cả hồ sơ đã xóa mềm) đến hết giao dịch thu tiền (NF6, S2-09).
     *
     * @throws NotFoundException (404) khi không có bản ghi hội viên nào mang {@code memberId}
     */
    @Transactional
    public Member lockForUpdateIncludingDeleted(Long memberId) {
        return members.findForUpdateIncludingDeleted(memberId).orElseThrow(() ->
            new NotFoundException("Hội viên không tồn tại trên hệ thống."));
    }

    /**
     * Khóa dòng hội viên như {@link #lockForUpdate} nhưng <b>không chờ</b>: dòng đang bị giao dịch khác khóa (check-in,
     * đặt lớp, đặt PT của cùng hội viên) thì ném ngay {@link org.springframework.dao.PessimisticLockingFailureException}.
     * Chỉ dùng cho check-in tại quầy (SRS F4 Scenario 7: bên đến sau nhận 409 ngay); F8, F9 vẫn dùng
     * {@link #lockForUpdate} (chờ).
     *
     * @throws NotFoundException (404) khi không có hồ sơ hoặc hồ sơ đã xóa mềm
     */
    @Transactional
    public Member lockForUpdateNoWait(Long memberId) {
        return members.findForUpdateNoWait(memberId).orElseThrow(() ->
            new NotFoundException("Hội viên không tồn tại trên hệ thống."));
    }

    /** Chức năng khác đọc hồ sơ theo cùng phạm vi chi nhánh với F2. */
    public Member requireInStaffScope(CurrentUser actor, Long memberId) {
        Member member = findMember(memberId);
        requireBranch(actor, member);
        return member;
    }

    /** Quyền đọc dùng chung cho hồ sơ và ảnh: ngoài phạm vi trả 404 (S3, ADR 0011). */
    public Member requireReadable(CurrentUser actor, Long id) {
        Member member = findMember(id);
        if (actor.role() == UserRole.MEMBER) {
            if (!actor.userId().equals(member.getUserId())) { throw notFound(); }
        } else {
            requireBranch(actor, member);
        }
        return member;
    }

    /** Khóa dòng cha và kiểm tra phạm vi chi nhánh trước khi thay hồ sơ, ảnh hoặc gắn tài khoản (NF6, S3). */
    @Transactional
    public Member requireEditableForUpdate(CurrentUser actor, Long id) {
        Member member = members.findForUpdate(id).orElseThrow(MemberService::notFound);
        requireBranch(actor, member);
        return member;
    }

    public MemberDetailResponse detail(CurrentUser actor, Long id) {
        return response(requireReadable(actor, id));
    }

    @Transactional
    public MemberDetailResponse update(CurrentUser actor, Long id, UpdateMemberRequest request) {
        // Tuần tự hóa cập nhật cùng hồ sơ để trạng thái member và user không lệch khi hai người cùng sửa.
        Member member = requireEditableForUpdate(actor, id);
        String phone = MemberPhoneSupport.normalize(request.phone());
        if (members.existsByPhoneAndIdNot(phone, id)) {
            throw new ConflictException(PHONE_TAKEN_BY_OTHER);
        }
        MemberStatus previous = member.getStatus();
        // Thao tác tài khoản trước khi sửa entity hồ sơ; lỗi F1 không làm đổi dữ liệu đang được quản lý.
        // Cả hai thao tác vẫn cùng giao dịch: lỗi khi lưu hồ sơ cũng rollback tài khoản và token.
        if (member.getUserId() != null && previous != request.status()) {
            if (request.status() == MemberStatus.INACTIVE) { accounts.disableAccount(member.getUserId()); }
            else { accounts.activateAccountIfDisabled(member.getUserId()); }
        }
        member.setFullName(request.fullName().trim());
        member.setPhone(phone);
        member.setGender(request.gender());
        member.setDob(request.dob());
        member.setStatus(request.status());
        save(member, PHONE_TAKEN_BY_OTHER);
        return response(member);
    }

    public PageResponse<MemberSummaryResponse> search(CurrentUser actor, String keyword,
            MemberStatus status, int page, int size) {
        Long branchId = actor.isOwner() ? null : staff.requireBranchId(actor.userId());
        String term = keyword == null ? "" : keyword.trim();
        String upperTerm = term.toUpperCase(Locale.ROOT);
        boolean code = upperTerm.matches("^HV[0-9]{6}$");
        boolean phone = !term.isEmpty() && MemberPhoneSupport.isPhoneKeyword(term);
        String value = code ? upperTerm : phone ? MemberPhoneSupport.normalizeSearchPrefix(term) : term;
        Specification<Member> spec = (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            filters.add(cb.isNull(root.get("deletedAt")));
            if (branchId != null) { filters.add(cb.equal(root.get("homeBranchId"), branchId)); }
            if (status != null) { filters.add(cb.equal(root.get("status"), status)); }
            if (!value.isEmpty()) {
                if (code) { filters.add(cb.equal(root.get("memberCode"), value)); }
                else if (phone) {
                    filters.add(value.length() == 10 ? cb.equal(root.get("phone"), value)
                        : cb.like(root.get("phone"), value + "%"));
                }
                else {
                    String prefix = value.toLowerCase(Locale.ROOT).replace("!", "!!")
                        .replace("%", "!%").replace("_", "!_") + "%";
                    filters.add(cb.like(cb.lower(root.get("fullName")), prefix, '!'));
                }
            }
            return cb.and(filters.toArray(Predicate[]::new));
        };
        var result = members.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        Set<Long> userIds = new HashSet<>();
        Set<Long> memberIds = new HashSet<>();
        result.forEach(m -> {
            memberIds.add(m.getId());
            if (m.getUserId() != null) { userIds.add(m.getUserId()); }
        });
        Map<Long, String> emails = userIds.isEmpty() ? Map.of() : accounts.getEmailsByUserIds(userIds);
        Set<Long> withAvatar = memberIds.isEmpty() ? Set.of() : avatars.findMemberIdsWithAvatar(memberIds);
        return PageResponse.of(result, m -> mapper.summary(m,
            m.getUserId() == null ? null : emails.get(m.getUserId()), withAvatar.contains(m.getId())));
    }

    /**
     * Khóa dòng hội viên, kiểm tra phạm vi chi nhánh, trạng thái {@code ACTIVE} và chưa gắn tài khoản,
     * rồi tạo bản ghi {@code users} qua {@link AccountService} và gắn {@code members.user_id} trong cùng giao dịch (UC-F2.2).
     */
    @Transactional
    public CreateMemberAccountResponse linkAccount(CurrentUser actor, Long memberId, String rawEmail) {
        Member member = requireEditableForUpdate(actor, memberId);
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessRuleException(INACTIVE_MEMBER_CANNOT_CREATE_ACCOUNT);
        }
        if (member.getUserId() != null) {
            throw new ConflictException(MEMBER_ALREADY_HAS_ACCOUNT);
        }
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        Long userId = accounts.createMemberAccount(email);
        member.setUserId(userId);
        members.saveAndFlush(member);
        return new CreateMemberAccountResponse(member.getId(), userId, email);
    }

    private MemberDetailResponse response(Member member) {
        String email = member.getUserId() == null ? null
            : accounts.getEmailsByUserIds(Set.of(member.getUserId())).get(member.getUserId());
        return mapper.detail(member, email, avatars.existsByMemberId(member.getId()));
    }

    /**
     * Hai yêu cầu cùng số điện thoại đến cùng lúc có thể cùng qua bước kiểm tra trùng;
     * khi đó ràng buộc uq_members_phone chặn lại và vẫn trả 409 kèm hướng dẫn rõ ràng.
     */
    private Member save(Member member, String duplicatePhoneMessage) {
        try {
            return members.saveAndFlush(member);
        } catch (DataIntegrityViolationException ex) {
            String cause = String.valueOf(ex.getMostSpecificCause().getMessage());
            if (cause.contains("uq_members_phone")) { throw new ConflictException(duplicatePhoneMessage); }
            throw ex;
        }
    }

    private Member findMember(Long id) {
        return members.findByIdAndDeletedAtIsNull(id).orElseThrow(MemberService::notFound);
    }

    private void requireBranch(CurrentUser actor, Member member) {
        if (!actor.isOwner() && !staff.requireBranchId(actor.userId()).equals(member.getHomeBranchId())) {
            throw notFound();
        }
    }

    private static NotFoundException notFound() {
        return new NotFoundException("Không tìm thấy hội viên.");
    }
}
