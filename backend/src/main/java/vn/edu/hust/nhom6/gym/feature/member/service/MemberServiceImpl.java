package vn.edu.hust.nhom6.gym.feature.member.service;

import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.exception.*;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberActor.Role;
import vn.edu.hust.nhom6.gym.feature.member.dto.*;
import vn.edu.hust.nhom6.gym.feature.member.entity.*;
import vn.edu.hust.nhom6.gym.feature.member.mapper.MemberMapper;
import vn.edu.hust.nhom6.gym.feature.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberManagementService, MemberRegistrationService {
    private final MemberRepository members;
    private final MemberDependencies integrations;
    private final MemberMapper mapper;

    @Override
    @Transactional
    public MemberDetailResponse create(MemberActor actor, CreateMemberRequest request) {
        requireManagement(actor);
        Long branch = actor.role() == Role.SUPER_ADMIN
            ? integrations.defaultBranchId() : integrations.staffBranchId(actor.userId());
        Member member = newProfile(null, actor.userId(), branch, request.fullName(), request.phone(),
            request.dob(), request.gender());
        return mapper.detail(members.saveAndFlush(member), null);
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
        Member member = newProfile(userId, userId, integrations.defaultBranchId(), fullName, phone, dob, parsedGender);
        members.saveAndFlush(member);
    }

    private Member newProfile(Long userId, Long createdBy, Long branchId, String fullName,
            String rawPhone, LocalDate dob, Gender gender) {
        validateName(fullName);
        String phone = MemberPhoneSupport.normalize(rawPhone);
        if (members.existsByPhone(phone)) {
            throw new ConflictException("Số điện thoại đã có hồ sơ hội viên. Vui lòng liên hệ quầy lễ tân.");
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
        if (actor.role() == Role.TRAINER) { throw denied(); }
        Member member = findMember(id);
        if (actor.role() == Role.MEMBER) {
            if (!actor.userId().equals(member.getUserId())) { throw notFound(); }
        } else {
            requireManagement(actor);
            requireBranch(actor, member);
        }
        return response(member);
    }

    @Override
    @Transactional
    public MemberDetailResponse update(MemberActor actor, Long id, UpdateMemberRequest request) {
        requireManagement(actor);
        if (id == null || id <= 0) { throw new InvalidRequestException("ID hội viên phải lớn hơn 0."); }
        // Tuần tự hóa cập nhật cùng hồ sơ để trạng thái member và user không lệch khi hai người cùng sửa.
        Member member = members.findForUpdate(id).orElseThrow(MemberServiceImpl::notFound);
        requireBranch(actor, member);
        validateName(request.fullName());
        String phone = MemberPhoneSupport.normalize(request.phone());
        if (members.existsByPhoneAndIdNot(phone, id)) {
            throw new ConflictException("Số điện thoại đã được đăng ký cho hội viên khác.");
        }
        if (request.status() == null) { throw new InvalidRequestException("Trạng thái không được để trống."); }
        MemberStatus previous = member.getStatus();
        member.setFullName(request.fullName().trim());
        member.setPhone(phone);
        member.setGender(request.gender());
        member.setDob(request.dob());
        member.setStatus(request.status());
        // UNIQUE được kiểm tra trước thao tác tài khoản; lỗi sau đó vẫn rollback toàn giao dịch.
        members.saveAndFlush(member);
        if (member.getUserId() != null && previous != request.status()) {
            if (request.status() == MemberStatus.INACTIVE) { integrations.disable(member.getUserId()); }
            else { integrations.activateIfDisabled(member.getUserId()); }
        }
        return response(member);
    }

    @Override
    public PageResponse<MemberSummaryResponse> search(MemberActor actor, String keyword,
            MemberStatus status, int page, int size) {
        requireManagement(actor);
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("Trang phải từ 0; kích thước trang từ 1 đến 100.");
        }
        Long branchId = actor.role() == Role.SUPER_ADMIN ? null : integrations.staffBranchId(actor.userId());
        String term = keyword == null ? "" : keyword.trim();
        boolean code = term.matches("^HV[0-9]{6}$");
        boolean phone = !term.isEmpty() && MemberPhoneSupport.isPhoneKeyword(term);
        String value = phone ? MemberPhoneSupport.normalize(term) : term;
        Specification<Member> spec = (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            filters.add(cb.isNull(root.get("deletedAt")));
            if (branchId != null) { filters.add(cb.equal(root.get("homeBranchId"), branchId)); }
            if (status != null) { filters.add(cb.equal(root.get("status"), status)); }
            if (!value.isEmpty()) {
                if (code) { filters.add(cb.equal(root.get("memberCode"), value)); }
                else if (phone) { filters.add(cb.equal(root.get("phone"), value)); }
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
        Map<Long, String> emails = integrations.emails(userIds);
        return PageResponse.of(result, m -> mapper.summary(m, m.getUserId() == null ? null : emails.get(m.getUserId())));
    }

    private MemberDetailResponse response(Member member) {
        String email = member.getUserId() == null ? null
            : integrations.emails(Set.of(member.getUserId())).get(member.getUserId());
        return mapper.detail(member, email);
    }

    private Member findMember(Long id) {
        if (id == null || id <= 0) { throw new InvalidRequestException("ID hội viên phải lớn hơn 0."); }
        return members.findByIdAndDeletedAtIsNull(id).orElseThrow(MemberServiceImpl::notFound);
    }

    private void requireBranch(MemberActor actor, Member member) {
        if (actor.role() != Role.SUPER_ADMIN && !integrations.staffBranchId(actor.userId()).equals(member.getHomeBranchId())) {
            throw notFound();
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 255) {
            throw new InvalidRequestException("Họ tên bắt buộc, tối đa 255 ký tự.");
        }
    }

    private static void requireManagement(MemberActor actor) {
        if (actor.role() != Role.SUPER_ADMIN && actor.role() != Role.BRANCH_MANAGER
                && actor.role() != Role.RECEPTIONIST) { throw denied(); }
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
    }

    private static NotFoundException notFound() {
        return new NotFoundException("Không tìm thấy hội viên.");
    }
}
