package vn.edu.hust.nhom6.gym.feature.member;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.feature.member.service.integration.MemberAccountAccess;
import vn.edu.hust.nhom6.gym.feature.member.service.integration.MemberBranchAccess;

/**
 * Adapter và database chỉ trong src/test: kiểm tra hợp đồng/giao dịch F2.
 * Không phải mã tài khoản hay nhân sự production, không chứng minh JWT/F1 đã tích hợp.
 */
@TestConfiguration(proxyBeanMethods = false)
class MemberTestConfiguration {
    @Bean
    @ServiceConnection
    PostgreSQLContainer memberPostgres() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
    }

    @Bean
    MemberAccountAccess memberAccountAccess(JdbcClient jdbc) { return new TestAccounts(jdbc); }

    @Bean
    MemberBranchAccess memberBranchAccess(JdbcClient jdbc) { return new TestBranches(jdbc); }

    static class TestAccounts implements MemberAccountAccess {
        private final JdbcClient jdbc;
        TestAccounts(JdbcClient jdbc) { this.jdbc = jdbc; }

        @Override
        @Transactional(readOnly = true)
        public Map<Long, String> emails(Collection<Long> userIds) {
            if (userIds.isEmpty()) { return Map.of(); }
            return jdbc.sql("SELECT id, email FROM users WHERE id IN (:ids)").param("ids", userIds)
                .query((rs, row) -> Map.entry(rs.getLong("id"), rs.getString("email")))
                .list().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @Override
        @Transactional
        public void disable(Long userId) {
            int count = jdbc.sql("UPDATE users SET status = 'DISABLED', updated_at = now() WHERE id = :id")
                .param("id", userId).update();
            if (count == 0) { throw new NotFoundException("Không tìm thấy tài khoản liên kết."); }
            jdbc.sql("UPDATE refresh_tokens SET revoked_at = :now WHERE user_id = :id AND revoked_at IS NULL")
                .param("now", OffsetDateTime.now()).param("id", userId).update();
        }

        @Override
        @Transactional
        public void activateIfDisabled(Long userId) {
            jdbc.sql("UPDATE users SET status = 'ACTIVE', updated_at = now() WHERE id = :id AND status = 'DISABLED'")
                .param("id", userId).update();
        }
    }

    static class TestBranches implements MemberBranchAccess {
        private final JdbcClient jdbc;
        TestBranches(JdbcClient jdbc) { this.jdbc = jdbc; }

        @Override
        public Long defaultBranchId() {
            return jdbc.sql("SELECT id FROM branches WHERE code = 'CN01' AND status = 'ACTIVE'")
                .query(Long.class).optional().orElseThrow(() -> new NotFoundException("Không có CN01."));
        }

        @Override
        public Long staffBranchId(Long userId) {
            return jdbc.sql("""
                SELECT s.branch_id FROM staff s JOIN branches b ON b.id = s.branch_id
                WHERE s.user_id = :id AND s.status = 'ACTIVE' AND b.status = 'ACTIVE'
                """).param("id", userId).query(Long.class).optional()
                .orElseThrow(() -> new AccessDeniedException("Chưa có chi nhánh làm việc hợp lệ."));
        }
    }
}
