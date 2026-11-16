package vn.edu.hust.nhom6.gym.feature.member.service.integration;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthService;

class F1MemberAccountAdapterTest {
    private final AuthService target = mock(AuthService.class);
    private final F1MemberAccountAdapter adapter = new F1MemberAccountAdapter(target);

    @Test
    void forwardsBatchWithoutSplittingIdsOrChangingResult() {
        List<Long> ids = List.of(11L, 12L, 99L);
        Map<Long, String> result = Map.of(11L, "an@example.com", 12L, "binh@example.com");
        when(target.getEmailsByUserIds(ids)).thenReturn(result);

        assertThat(adapter.emails(ids)).isSameAs(result);
        verify(target).getEmailsByUserIds(same(ids));
        verifyNoMoreInteractions(target);
    }

    @Test
    void delegatesEmptyAndNullCollectionsToF1() {
        when(target.getEmailsByUserIds(List.of())).thenReturn(Map.of());
        when(target.getEmailsByUserIds(null)).thenReturn(Map.of());

        assertThat(adapter.emails(List.of())).isEmpty();
        assertThat(adapter.emails(null)).isEmpty();
        verify(target).getEmailsByUserIds(List.of());
        verify(target).getEmailsByUserIds(null);
    }

    @Test
    void disableForwardsIdAndPreservesOriginalBusinessException() {
        adapter.disable(11L);
        verify(target).disableAccount(11L);

        var failure = new NotFoundException("Không tìm thấy tài khoản liên kết.");
        doThrow(failure).when(target).disableAccount(99L);
        assertThatThrownBy(() -> adapter.disable(99L)).isSameAs(failure);
    }

    @Test
    void activationDelegatesWithoutImplementingAccountStatusRulesInF2() {
        adapter.activateIfDisabled(12L);
        verify(target).activateAccountIfDisabled(12L);

        var failure = new IllegalStateException("Lỗi giao dịch F1");
        doThrow(failure).when(target).activateAccountIfDisabled(99L);
        assertThatThrownBy(() -> adapter.activateIfDisabled(99L)).isSameAs(failure);
    }

    @Test
    void emailLookupPreservesOriginalFailure() {
        var failure = new IllegalStateException("Không thể đọc tài khoản");
        when(target.getEmailsByUserIds(List.of(11L))).thenThrow(failure);

        assertThatThrownBy(() -> adapter.emails(List.of(11L))).isSameAs(failure);
    }

}
