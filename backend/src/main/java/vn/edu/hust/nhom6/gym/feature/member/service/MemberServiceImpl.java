package vn.edu.hust.nhom6.gym.feature.member.service;

import java.time.LocalDate;
import java.util.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.exception.*;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.member.dto.*;
import vn.edu.hust.nhom6.gym.feature.member.entity.*;
import vn.edu.hust.nhom6.gym.feature.member.mapper.MemberMapper;
import vn.edu.hust.nhom6.gym.feature.member.repository.MemberRepository;
import vn.edu.hust.nhom6.gym.feature.member.service.integration.MemberAccountAccess;
import vn.edu.hust.nhom6.gym.feature.member.service.integration.MemberBranchAccess;

@Service
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberManagementService, MemberRegistrationService {
    private static final String PHONE_HAS_PROFILE =
        "Số điện thoại đã có hồ sơ hội viên. Vui lòng liên hệ quầy lễ tân.";
    private static final String PHONE_TAKEN_BY_OTHER = "Số điện thoại đã được đăng ký cho hội viên khác.";

    private final MemberRepository members;
    private final MemberAccountAccess accounts;
    private final MemberBranchAccess branches;
    private final MemberMapper mapper;
    private final Validator validator;

    /**
     * {@code @Lazy} cắt vòng phụ thuộc: AuthService (F1) gọi F2 để tạo hồ sơ khi đăng ký,
     * còn F2 lại gọi AuthService qua MemberAccountAccess để đổi trạng thái tài khoản.
     */
    public MemberServiceImpl(MemberRepository members, @Lazy MemberAccountAccess accounts,
            MemberBranchAccess branches, MemberMapper mapper, Validator validator) {
        this.members = members;
        this.accounts = accounts;
        this.branches = branches;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Override
    @Transactional
    public MemberDetailResponse create(MemberActor actor, CreateMemberRequest request) {
        Long branch = actor.role() == UserRole.SUPER_ADMIN
            ? branches.defaultBranchId() : branches.staffBranchId(actor.userId());
        Member member = newProfile(null, actor.userId(), branch, request.fullName(), request.phone(),
            request.dob(), request.gender());
        return mapper.detail(save(member, PHONE_HAS_PROFILE), null);
    }

    @Override
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

    @Override
    public MemberDetailResponse detail(MemberActor actor, Long id) {
        Member member = findMember(id);
        if (actor.role() == UserRole.MEMBER) {
            if (!actor.userId().equals(member.getUserId())) { throw notFound(); }
        } else {
            requireBranch(actor, member);
        }
        return response(member);
    }

    @Override
    @Transactional
    public MemberDetailResponse update(MemberActor actor, Long id, UpdateMemberRequest request) {
        // Tuần tự hóa cập nhật cùng hồ sơ để trạng thái member và user không lệch khi hai người cùng sửa.
        Member member = members.findForUpdate(id).orElseThrow(MemberServiceImpl::notFound);
        requireBranch(actor, member);
        String phone = MemberPhoneSupport.normalize(request.phone());
        if (members.existsByPhoneAndIdNot(phone, id)) {
            throw new ConflictException(PHONE_TAKEN_BY_OTHER);
        }
        MemberStatus previous = member.getStatus();
        // Thao tác tài khoản trước khi sửa entity hồ sơ; lỗi F1 không làm đổi dữ liệu đang được quản lý.
        // Cả hai thao tác vẫn cùng giao dịch: lỗi khi lưu hồ sơ cũng rollback tài khoản và token.
        if (member.getUserId() != null && previous != request.status()) {
            if (request.status() == MemberStatus.INACTIVE) { accounts.disable(member.getUserId()); }
            else { accounts.activateIfDisabled(member.getUserId()); }
        }
        member.setFullName(request.fullName().trim());
        member.setPhone(phone);
        member.setGender(request.gender());
        member.setDob(request.dob());
        member.setStatus(request.status());
        save(member, PHONE_TAKEN_BY_OTHER);
        return response(member);
    }

    @Override
    public PageResponse<MemberSummaryResponse> search(MemberActor actor, String keyword,
            MemberStatus status, int page, int size) {
        Long branchId = actor.role() == UserRole.SUPER_ADMIN ? null : branches.staffBranchId(actor.userId());
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
        result.forEach(m -> { if (m.getUserId() != null) { userIds.add(m.getUserId()); } });
        Map<Long, String> emails = userIds.isEmpty() ? Map.of() : accounts.emails(userIds);
        return PageResponse.of(result, m -> mapper.summary(m, m.getUserId() == null ? null : emails.get(m.getUserId())));
    }

    private MemberDetailResponse response(Member member) {
        String email = member.getUserId() == null ? null
            : accounts.emails(Set.of(member.getUserId())).get(member.getUserId());
        return mapper.detail(member, email);
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
        return members.findByIdAndDeletedAtIsNull(id).orElseThrow(MemberServiceImpl::notFound);
    }

    private void requireBranch(MemberActor actor, Member member) {
        if (actor.role() != UserRole.SUPER_ADMIN && !branches.staffBranchId(actor.userId()).equals(member.getHomeBranchId())) {
            throw notFound();
        }
    }

    private static NotFoundException notFound() {
        return new NotFoundException("Không tìm thấy hội viên.");
    }
}
