package vn.edu.hust.nhom6.gym.feature.member;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Chỉ cung cấp PostgreSQL kiểm thử. F2 luôn gọi AccountService, StaffService, BranchLookupService thật.
 */
@TestConfiguration(proxyBeanMethods = false)
class MemberTestConfiguration {
    @Bean
    @ServiceConnection
    PostgreSQLContainer memberPostgres() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
    }
}
