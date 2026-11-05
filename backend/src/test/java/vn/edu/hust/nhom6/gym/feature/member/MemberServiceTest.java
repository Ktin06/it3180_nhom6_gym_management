package vn.edu.hust.nhom6.gym.feature.member;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.security.access.AccessDeniedException;
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
    MemberServiceImpl service;
    final MemberActor receptionist = new MemberActor(10L, Role.RECEPTIONIST);

    @BeforeEach
    void setUp() {
        service = new MemberServiceImpl(repository, integrations, new MemberMapper());
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
    void trainerCannotReadEvenExistingMember() {
        assertThatThrownBy(() -> service.detail(new MemberActor(3L, Role.TRAINER), 1L))
            .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository);
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
}
