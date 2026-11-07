package vn.edu.hust.nhom6.gym.feature.member;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import jakarta.validation.Validator;
import vn.edu.hust.nhom6.gym.common.exception.*;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberActor.Role;
import vn.edu.hust.nhom6.gym.feature.member.dto.*;
import vn.edu.hust.nhom6.gym.feature.member.entity.*;
import vn.edu.hust.nhom6.gym.feature.member.mapper.MemberMapper;
import vn.edu.hust.nhom6.gym.feature.member.repository.MemberRepository;
import vn.edu.hust.nhom6.gym.feature.member.service.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @Mock MemberRepository repository;
    @Mock MemberDependencies integrations;
    @Mock Validator validator;
    MemberServiceImpl service;
    final MemberActor receptionist = new MemberActor(10L, Role.RECEPTIONIST);

    @BeforeEach
    void setUp() {
        service = new MemberServiceImpl(repository, integrations, new MemberMapper(), validator);
    }

    @Test
    void createsWithStaffBranchAndNormalizedPhone() {
        when(integrations.staffBranchId(10L)).thenReturn(7L);
        when(repository.nextMemberCode()).thenReturn(123L);
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        var result = service.create(receptionist, new CreateMemberRequest("  Nguyễn An  ", "+84 912 345 678", null, null));
        assertThat(result.memberCode()).isEqualTo("HV000123");
        assertThat(result.homeBranchId()).isEqualTo(7L);
        assertThat(result.phone()).isEqualTo("0912345678");
        assertThat(result.fullName()).isEqualTo("Nguyễn An");
        assertThat(result.userId()).isNull();
        verify(integrations, never()).defaultBranchId();
    }

    @Test
    void duplicatePhoneDoesNotConsumeSequence() {
        when(integrations.staffBranchId(10L)).thenReturn(7L);
        when(repository.existsByPhone("0912345678")).thenReturn(true);
        assertThatThrownBy(() -> service.create(receptionist,
            new CreateMemberRequest("An", "0912345678", null, null))).isInstanceOf(ConflictException.class);
        verify(repository, never()).nextMemberCode();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void exhaustedSequenceDoesNotTruncateCode() {
        when(integrations.staffBranchId(10L)).thenReturn(7L);
        when(repository.nextMemberCode()).thenReturn(1000000L);
        assertThatThrownBy(() -> service.create(receptionist,
            new CreateMemberRequest("An", "0912345678", null, null))).isInstanceOf(BusinessRuleException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void outsideBranchIsNotFoundAndDoesNotModifyAccount() {
        Member member = new Member();
        member.setHomeBranchId(8L);
        when(repository.findForUpdate(1L)).thenReturn(Optional.of(member));
        when(integrations.staffBranchId(10L)).thenReturn(7L);
        assertThatThrownBy(() -> service.update(receptionist, 1L,
            new UpdateMemberRequest("An", "0912345678", null, null, MemberStatus.INACTIVE)))
            .isInstanceOf(NotFoundException.class);
        verify(integrations, never()).disable(any());
    }

    @Test
    void memberCannotReadSomeoneElse() {
        Member member = new Member();
        member.setUserId(99L);
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));
        assertThatThrownBy(() -> service.detail(new MemberActor(3L, Role.MEMBER), 1L))
            .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(integrations);
    }

    @Test
    void failedAccountTransitionDoesNotMutateNamePhoneOrStatus() {
        Member member = linkedMember();
        when(repository.findForUpdate(1L)).thenReturn(Optional.of(member));
        when(integrations.staffBranchId(10L)).thenReturn(7L);
        var failure = new NotFoundException("Không tìm thấy tài khoản liên kết.");
        doThrow(failure).when(integrations).disable(42L);

        assertThatThrownBy(() -> service.update(receptionist, 1L,
            new UpdateMemberRequest("Tên mới", "0912345699", null, null, MemberStatus.INACTIVE)))
            .isSameAs(failure);

        assertThat(member.getFullName()).isEqualTo("Tên cũ");
        assertThat(member.getPhone()).isEqualTo("0912345601");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void editingProfileWithoutStatusTransitionDoesNotDisableOrActivateAccount() {
        Member member = linkedMember();
        when(repository.findForUpdate(1L)).thenReturn(Optional.of(member));
        when(integrations.staffBranchId(10L)).thenReturn(7L);
        when(integrations.emails(Set.of(42L))).thenReturn(Map.of(42L, "member@example.com"));

        var result = service.update(receptionist, 1L,
            new UpdateMemberRequest("Tên mới", "0912345699", null, null, MemberStatus.ACTIVE));

        assertThat(result.fullName()).isEqualTo("Tên mới");
        assertThat(result.phone()).isEqualTo("0912345699");
        assertThat(result.email()).isEqualTo("member@example.com");
        verify(integrations, never()).disable(any());
        verify(integrations, never()).activateIfDisabled(any());
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
