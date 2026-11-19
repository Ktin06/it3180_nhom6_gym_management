package vn.edu.hust.nhom6.gym.feature.packagecatalog;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import vn.edu.hust.nhom6.gym.common.exception.UnauthorizedException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.CreatePackageRequest;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.UpdatePackageRequest;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.repository.PackageRepository;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.service.PackageCatalogService;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PackageCatalogServiceTest {
    PackageRepository packages;
    StaffService staff;
    PackageCatalogService service;

    @BeforeEach
    void setUp() {
        packages = mock(PackageRepository.class);
        staff = mock(StaffService.class);
        service = new PackageCatalogService(packages, staff, mock(jakarta.validation.Validator.class));
    }

    @Test
    @DisplayName("TC-F3.1.1-04b: Chủ chuỗi xem danh mục không cần gán chi nhánh (kiểm ở service)")
    void ownerDoesNotRequireStaffAssignment() {
        when(packages.search(eq(true), isNull(), isNull(), eq(PackageStatus.ACTIVE), any(Pageable.class)))
            .thenReturn(Page.empty());
        service.list(identity("1", "SUPER_ADMIN"), null, PackageStatus.ACTIVE, 0, 20);
        verifyNoInteractions(staff);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUPER_ADMIN", "RECEPTIONIST", "MEMBER", "TRAINER"})
    @DisplayName("TC-F3.2.1-06c: Service tự chặn vai trò khác Quản lý chi nhánh khi thêm gói")
    void createChecksRoleEvenWhenCalledWithoutController(String role) {
        var request = new CreatePackageRequest("TEST", "Gói test", PackageType.TIME, 30, null, 0, null);
        assertThatThrownBy(() -> service.create(identity("1", role), request))
            .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(staff, packages);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUPER_ADMIN", "RECEPTIONIST", "MEMBER", "TRAINER"})
    @DisplayName("TC-F3.3.1-08c: Service tự chặn vai trò khác Quản lý chi nhánh khi sửa gói")
    void updateChecksRoleWithoutController(String role) {
        assertThatThrownBy(() -> service.update(identity("1", role), 1L, new UpdatePackageRequest()))
            .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(staff, packages);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUPER_ADMIN", "RECEPTIONIST", "MEMBER", "TRAINER"})
    @DisplayName("TC-F3.4.1-06g: Service tự chặn vai trò khác Quản lý chi nhánh khi ngừng bán")
    void deactivateChecksRoleWithoutController(String role) {
        assertThatThrownBy(() -> service.deactivate(identity("1", role), 1L))
            .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(staff, packages);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BRANCH_MANAGER", "RECEPTIONIST"})
    @DisplayName("TC-F3.1.1-05d: Nhân viên chưa gán chi nhánh không bao giờ được xem mọi chi nhánh")
    void missingAssignmentNeverFallsBackToAllBranches(String role) {
        when(staff.requireBranchId(2L)).thenThrow(new AccessDeniedException("Chưa có chi nhánh"));
        assertThatThrownBy(() -> service.list(identity("2", role), null, PackageStatus.ACTIVE, 0, 20))
            .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(packages);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRAINER", "MEMBER"})
    @DisplayName("TC-F3.1.1-05e: Vai trò không được phép không chạm tới truy vấn danh mục")
    void deniedRolesCannotQueryRepository(String role) {
        assertThatThrownBy(() -> service.list(identity("2", role), null, PackageStatus.ACTIVE, 0, 20))
            .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(staff, packages);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "not-an-id"})
    @DisplayName("S2: JWT có subject không phải id hợp lệ bị từ chối 401")
    void invalidSubjectIsUnauthorized(String subject) {
        assertThatThrownBy(() -> service.list(identity(subject, "SUPER_ADMIN"), null, PackageStatus.ACTIVE, 0, 20))
            .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(staff, packages);
    }

    /** Controller đổi Authentication thành CurrentUser; sub sai thì CurrentUser.from ném 401 trước khi vào service. */
    private CurrentUser identity(String id, String role) {
        return CurrentUser.from(new UsernamePasswordAuthenticationToken(id, "unused",
            List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
