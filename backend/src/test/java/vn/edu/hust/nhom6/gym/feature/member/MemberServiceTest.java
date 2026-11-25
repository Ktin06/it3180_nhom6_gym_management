package vn.edu.hust.nhom6.gym.feature.member;

import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import jakarta.validation.Validator;
import vn.edu.hust.nhom6.gym.common.exception.*;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.member.dto.*;
import vn.edu.hust.nhom6.gym.feature.member.entity.*;
import vn.edu.hust.nhom6.gym.feature.member.mapper.MemberMapper;
import vn.edu.hust.nhom6.gym.feature.member.repository.MemberRepository;
import vn.edu.hust.nhom6.gym.feature.member.service.*;
import vn.edu.hust.nhom6.gym.feature.auth.service.AccountService;
import vn.edu.hust.nhom6.gym.feature.branch.service.BranchLookupService;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @Mock MemberRepository repository;
    @Mock AccountService accounts;
    @Mock StaffService staff;
    @Mock BranchLookupService branches;
    @Mock Validator validator;
    MemberService service;
    final CurrentUser receptionist = new CurrentUser(10L, UserRole.RECEPTIONIST);

    @BeforeEach
    void setUp() {
        service = new MemberService(repository, accounts, staff, branches, new MemberMapper(), validator);
    }

    @Test
    @DisplayName("TC-F2.1.1-01e: tạo hồ sơ lấy chi nhánh nhân viên và chuẩn hóa SĐT")
    void createsWithStaffBranchAndNormalizedPhone() {
        when(staff.requireBranchId(10L)).thenReturn(7L);
        when(repository.nextMemberCode()).thenReturn(123L);
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        var result = service.create(receptionist, new CreateMemberRequest("  Nguyễn An  ", "+84 912 345 678", null, null));
        assertThat(result.memberCode()).isEqualTo("HV000123");
        assertThat(result.homeBranchId()).isEqualTo(7L);
        assertThat(result.phone()).isEqualTo("0912345678");
        assertThat(result.fullName()).isEqualTo("Nguyễn An");
        assertThat(result.userId()).isNull();
        verify(branches, never()).defaultBranchId();
    }

    @Test
    @DisplayName("TC-F2.1.1-02f: trùng SĐT không tiêu tốn sequence mã hội viên")
    void duplicatePhoneDoesNotConsumeSequence() {
        when(staff.requireBranchId(10L)).thenReturn(7L);
        when(repository.existsByPhone("0912345678")).thenReturn(true);
        assertThatThrownBy(() -> service.create(receptionist,
            new CreateMemberRequest("An", "0912345678", null, null))).isInstanceOf(ConflictException.class);
        verify(repository, never()).nextMemberCode();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC-F1.1.1-02c, TC-F2.1.1-02g: trùng SĐT đồng thời trả 409 kèm hướng dẫn liên hệ quầy")
    void concurrentDuplicatePhoneStillGetsFrontDeskGuidance() {
        when(staff.requireBranchId(10L)).thenReturn(7L);
        when(repository.nextMemberCode()).thenReturn(1L);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("lỗi",
            new RuntimeException("duplicate key value violates unique constraint \"uq_members_phone\"")));
        assertThatThrownBy(() -> service.create(receptionist,
            new CreateMemberRequest("An", "0912345678", null, null)))
            .isInstanceOf(ConflictException.class).hasMessageContaining("liên hệ quầy lễ tân");
    }

    @Test
    @DisplayName("BR-F2-06: vi phạm ràng buộc khác không báo nhầm thành trùng SĐT")
    void otherConstraintViolationsAreNotReportedAsDuplicatePhone() {
        when(staff.requireBranchId(10L)).thenReturn(7L);
        when(repository.nextMemberCode()).thenReturn(1L);
        var failure = new DataIntegrityViolationException("lỗi", new RuntimeException("violates uq_members_member_code"));
        when(repository.saveAndFlush(any())).thenThrow(failure);
        assertThatThrownBy(() -> service.create(receptionist,
            new CreateMemberRequest("An", "0912345678", null, null))).isSameAs(failure);
    }

    @Test
    @DisplayName("BR-F2-05: hết dải 6 chữ số mã hội viên bị từ chối, không cắt mã")
    void exhaustedSequenceDoesNotTruncateCode() {
        when(staff.requireBranchId(10L)).thenReturn(7L);
        when(repository.nextMemberCode()).thenReturn(1000000L);
        assertThatThrownBy(() -> service.create(receptionist,
            new CreateMemberRequest("An", "0912345678", null, null))).isInstanceOf(BusinessRuleException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC-F2.4.1-02f: cập nhật hồ sơ chi nhánh khác trả 404 và không tác động tài khoản")
    void outsideBranchIsNotFoundAndDoesNotModifyAccount() {
        Member member = new Member();
        member.setHomeBranchId(8L);
        when(repository.findForUpdate(1L)).thenReturn(Optional.of(member));
        when(staff.requireBranchId(10L)).thenReturn(7L);
        assertThatThrownBy(() -> service.update(receptionist, 1L,
            new UpdateMemberRequest("An", "0912345678", null, null, MemberStatus.INACTIVE)))
            .isInstanceOf(NotFoundException.class);
        verify(accounts, never()).disableAccount(any());
    }

    @Test
    @DisplayName("TC-F2.3.1-03c: hội viên xem hồ sơ người khác trả 404")
    void memberCannotReadSomeoneElse() {
        Member member = new Member();
        member.setUserId(99L);
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));
        assertThatThrownBy(() -> service.detail(new CurrentUser(3L, UserRole.MEMBER), 1L))
            .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(accounts, staff, branches);
    }

    @Test
    @DisplayName("TC-F2.4.1-03g: lỗi chuyển trạng thái tài khoản không làm đổi dữ liệu hồ sơ")
    void failedAccountTransitionDoesNotMutateNamePhoneOrStatus() {
        Member member = linkedMember();
        when(repository.findForUpdate(1L)).thenReturn(Optional.of(member));
        when(staff.requireBranchId(10L)).thenReturn(7L);
        var failure = new NotFoundException("Không tìm thấy tài khoản liên kết.");
        doThrow(failure).when(accounts).disableAccount(42L);

        assertThatThrownBy(() -> service.update(receptionist, 1L,
            new UpdateMemberRequest("Tên mới", "0912345699", null, null, MemberStatus.INACTIVE)))
            .isSameAs(failure);

        assertThat(member.getFullName()).isEqualTo("Tên cũ");
        assertThat(member.getPhone()).isEqualTo("0912345601");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC-F2.4.1-01d: sửa thông tin không đổi trạng thái không gọi khóa hoặc mở tài khoản")
    void editingProfileWithoutStatusTransitionDoesNotDisableOrActivateAccount() {
        Member member = linkedMember();
        when(repository.findForUpdate(1L)).thenReturn(Optional.of(member));
        when(staff.requireBranchId(10L)).thenReturn(7L);
        when(accounts.getEmailsByUserIds(Set.of(42L))).thenReturn(Map.of(42L, "member@example.com"));

        var result = service.update(receptionist, 1L,
            new UpdateMemberRequest("Tên mới", "0912345699", null, null, MemberStatus.ACTIVE));

        assertThat(result.fullName()).isEqualTo("Tên mới");
        assertThat(result.phone()).isEqualTo("0912345699");
        assertThat(result.email()).isEqualTo("member@example.com");
        verify(accounts, never()).disableAccount(any());
        verify(accounts, never()).activateAccountIfDisabled(any());
    }

    @Test
    @DisplayName("Nền tảng: tài khoản đã gắn hồ sơ thì lấy được hồ sơ của chính mình")
    void requireByUserIdReturnsLinkedProfile() {
        Member member = linkedMember();
        when(repository.findByUserIdAndDeletedAtIsNull(42L)).thenReturn(Optional.of(member));

        assertThat(service.requireByUserId(42L)).isSameAs(member);
    }

    @Test
    @DisplayName("Nền tảng: tài khoản chưa gắn hồ sơ hội viên thì 403")
    void requireByUserIdWithoutProfileIsForbidden() {
        when(repository.findByUserIdAndDeletedAtIsNull(43L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireByUserId(43L)).isInstanceOf(ForbiddenException.class);
    }

    private Member linkedMember() {
        Member member = new Member();
        member.setUserId(42L);
        member.setHomeBranchId(7L);
        member.setFullName("Tên cũ");
        member.setPhone("0912345601");
        return member;
    }
}
