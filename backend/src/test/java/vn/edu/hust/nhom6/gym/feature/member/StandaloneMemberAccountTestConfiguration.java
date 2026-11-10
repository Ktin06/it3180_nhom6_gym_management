package vn.edu.hust.nhom6.gym.feature.member;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.feature.member.service.integration.MemberAccountAccess;

/** Chỉ bài kiểm thử F2 độc lập import cấu hình này; không dùng trong bài IT với service F1 thật. */
@TestConfiguration(proxyBeanMethods = false)
class StandaloneMemberAccountTestConfiguration {
    @Bean
    @Primary
    MemberAccountAccess memberAccountAccess(JdbcClient jdbc) { return new TestAccounts(jdbc); }

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
}
