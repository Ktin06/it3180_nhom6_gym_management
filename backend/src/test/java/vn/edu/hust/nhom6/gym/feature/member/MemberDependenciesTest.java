package vn.edu.hust.nhom6.gym.feature.member;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberDependencies;
import vn.edu.hust.nhom6.gym.feature.member.service.integration.*;

@ExtendWith(MockitoExtension.class)
class MemberDependenciesTest {
    @Mock ObjectProvider<MemberAccountAccess> accounts;
    @Mock ObjectProvider<MemberBranchAccess> branches;

    @Test
    void absentServicesCannotSilentlySucceed() {
        var dependencies = new MemberDependencies(accounts, branches);
        assertThat(dependencies.emails(Set.of())).isEmpty();
        assertThatThrownBy(() -> dependencies.emails(Set.of(1L))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> dependencies.disable(1L)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> dependencies.staffBranchId(1L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void nullStaffBranchCannotGrantChainWideSearch() {
        MemberBranchAccess access = mock(MemberBranchAccess.class);
        when(branches.getIfAvailable()).thenReturn(access);
        var dependencies = new MemberDependencies(accounts, branches);
        assertThatThrownBy(() -> dependencies.staffBranchId(1L)).isInstanceOf(AccessDeniedException.class);
    }
}
