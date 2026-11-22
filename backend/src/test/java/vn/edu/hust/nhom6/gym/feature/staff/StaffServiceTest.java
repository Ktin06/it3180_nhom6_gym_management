package vn.edu.hust.nhom6.gym.feature.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import vn.edu.hust.nhom6.gym.feature.branch.service.BranchLookupService;
import vn.edu.hust.nhom6.gym.feature.staff.repository.StaffLookupRepository;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;

class StaffServiceTest {
    StaffLookupRepository repository;
    BranchLookupService branches;
    StaffService service;

    @BeforeEach
    void setUp() {
        repository = mock(StaffLookupRepository.class);
        branches = mock(BranchLookupService.class);
        service = new StaffService(repository, branches);
    }

    @Test
    @DisplayName("Nền tảng: nhân viên ACTIVE ở chi nhánh ACTIVE lấy được chi nhánh")
    void activeStaffInActiveBranch() {
        when(repository.findActiveBranchIdByUserId(5L)).thenReturn(Optional.of(7L));
        when(branches.isActive(7L)).thenReturn(true);

        assertThat(service.requireBranchId(5L)).isEqualTo(7L);
    }

    @Test
    @DisplayName("Nền tảng: chủ chuỗi không có dòng staff thì requireBranchId trả 403")
    void ownerWithoutStaffRowIsDenied() {
        when(repository.findActiveBranchIdByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireBranchId(1L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(branches);
    }

    @Test
    @DisplayName("Nền tảng: chi nhánh của nhân viên đã ngừng hoạt động thì 403")
    void inactiveBranchIsDenied() {
        when(repository.findActiveBranchIdByUserId(5L)).thenReturn(Optional.of(7L));
        when(branches.isActive(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.requireBranchId(5L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Nền tảng: id tài khoản rỗng hoặc không dương thì 403, không truy vấn")
    void invalidUserIdIsDeniedWithoutQuery() {
        assertThatThrownBy(() -> service.requireBranchId(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.requireBranchId(0L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository, branches);
    }
}
