package vn.edu.hust.nhom6.gym.feature.member;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.feature.branch.service.BranchLookupService;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;

/**
 * Chi nhánh mặc định (BranchLookupService) và chi nhánh nhân viên (StaffService.requireBranchId) với PostgreSQL thật.
 * F2 dùng hai hàm này khi tạo, tìm, xem hồ sơ; trước nền tảng N0.1 lớp này kiểm thử MemberBranchAdapter.
 */
@SpringBootTest
@Import(MemberTestConfiguration.class)
@Transactional
class StaffBranchIntegrationTest {
    @Autowired BranchLookupService branches;
    @Autowired StaffService staffService;
    @Autowired JdbcTemplate jdbc;

    @Test
    void defaultBranchIsLookedUpByCodeRatherThanFixedId() {
        Long original = defaultBranchId();
        jdbc.update("UPDATE branches SET code = 'CN_OLD_TEST' WHERE id = ?", original);
        Long replacement = branch("CN01");

        assertThat(replacement).isNotEqualTo(original);
        assertThat(branches.defaultBranchId()).isEqualTo(replacement);
    }

    @Test
    void missingDefaultBranchIsNotFoundInsteadOfCreatingOne() {
        jdbc.update("UPDATE branches SET code = 'CN_OLD_TEST' WHERE code = 'CN01'");

        assertThatThrownBy(() -> branches.defaultBranchId())
            .isInstanceOf(NotFoundException.class).hasMessageContaining("CN01");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM branches WHERE code = 'CN01'", Integer.class))
            .isZero();
    }

    @Test
    void inactiveDefaultBranchIsNotFound() {
        jdbc.update("UPDATE branches SET status = 'INACTIVE' WHERE code = 'CN01'");

        assertThatThrownBy(() -> branches.defaultBranchId()).isInstanceOf(NotFoundException.class);
    }

    @Test
    void staffBranchComesFromThatUsersAssignmentRatherThanDefaultBranch() {
        Long firstUser = user("first-branch@example.com");
        Long secondUser = user("second-branch@example.com");
        Long firstBranch = defaultBranchId();
        Long secondBranch = branch("CN_TEST_02");
        staff(firstUser, firstBranch, "NV_TEST_01");
        staff(secondUser, secondBranch, "NV_TEST_02");

        assertThat(staffService.requireBranchId(firstUser)).isEqualTo(firstBranch);
        assertThat(staffService.requireBranchId(secondUser)).isEqualTo(secondBranch);
    }

    @Test
    void userWithoutStaffProfileIsDeniedRatherThanAssignedToCn01() {
        Long userId = user("unassigned-branch@example.com");

        assertThatThrownBy(() -> staffService.requireBranchId(userId)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void inactiveStaffIsDenied() {
        Long userId = user("inactive-staff@example.com");
        staff(userId, defaultBranchId(), "NV_TEST_01");
        jdbc.update("UPDATE staff SET status = 'INACTIVE' WHERE user_id = ?", userId);

        assertThatThrownBy(() -> staffService.requireBranchId(userId)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void activeStaffInInactiveBranchIsDenied() {
        Long userId = user("inactive-branch@example.com");
        staff(userId, defaultBranchId(), "NV_TEST_01");
        jdbc.update("UPDATE branches SET status = 'INACTIVE' WHERE code = 'CN01'");

        assertThatThrownBy(() -> staffService.requireBranchId(userId)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void invalidOrUnknownUserIdsNeverReceiveDefaultBranch() {
        for (Long userId : Arrays.asList(null, 0L, -1L, Long.MAX_VALUE)) {
            assertThatThrownBy(() -> staffService.requireBranchId(userId)).isInstanceOf(AccessDeniedException.class);
        }
    }

    private Long defaultBranchId() {
        return jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN01'", Long.class);
    }

    private Long branch(String code) {
        return jdbc.queryForObject("""
            INSERT INTO branches(code,name,open_time,close_time) VALUES (?, 'Chi nhánh kiểm thử', '06:00', '22:00')
            RETURNING id
            """, Long.class, code);
    }

    private Long user(String email) {
        return jdbc.queryForObject("""
            INSERT INTO users(email,password_hash,role) VALUES (?, 'test-only-hash', 'RECEPTIONIST') RETURNING id
            """, Long.class, email);
    }

    private void staff(Long userId, Long branchId, String code) {
        jdbc.update("""
            INSERT INTO staff(user_id,branch_id,staff_code,full_name) VALUES (?, ?, ?, 'Nhân viên kiểm thử')
            """, userId, branchId, code);
    }
}
