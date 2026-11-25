package vn.edu.hust.nhom6.gym;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Kiểm thử tích hợp chạy Flyway với cả {@code db/migration} và {@code db/seed} trên một
 * {@link PostgreSQLContainer} riêng biệt (S2-29.1, Issue #209; quy tắc 5 của {@code db/seed/README.md}).
 */
class SeedDataMigrationIntegrationTest {

    private static final String DUMMY_BCRYPT_HASH =
        "$2y$10$7EqJtq98hPqEX7fNZaFWoOHi6rS8nY7bK5vX1pZ3qW9mL2cR4tY6u";

    @Test
    @DisplayName("Chạy Flyway với db/migration và db/seed trên database riêng thành công, chạy lần hai số dòng không đổi")
    void chayFlywayKemSeedHaiLanKhongLoiVaKhongSinhDongTrung() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16"))) {
            postgres.start();

            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
            );
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);

            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration", "classpath:db/seed")
                .placeholders(Map.of("seed_password_hash", DUMMY_BCRYPT_HASH))
                .load();

            flyway.migrate();

            assertSeedCounts(jdbc);

            // Buộc Flyway chạy lại toàn bộ các file R__ bằng cách xóa lịch sử của các repeatable migration
            jdbc.update("DELETE FROM flyway_schema_history WHERE version IS NULL");
            flyway.migrate();

            assertSeedCounts(jdbc);
        }
    }

    private void assertSeedCounts(JdbcTemplate jdbc) {
        assertThat(count(jdbc, "SELECT COUNT(*) FROM branches WHERE code IN ('CN01', 'CN02')"))
            .as("Đủ 2 chi nhánh CN01 và CN02")
            .isEqualTo(2);

        assertThat(count(jdbc, "SELECT COUNT(*) FROM users WHERE email IN ('manager2@example.com', 'receptionist2@example.com', 'member3@example.com')"))
            .as("Đủ tài khoản mẫu bổ sung cho CN02")
            .isEqualTo(3);

        assertThat(count(jdbc, "SELECT COUNT(*) FROM staff WHERE staff_code IN ('NV999001', 'NV999002', 'PT999001', 'PT999002', 'NV999003', 'NV999004')"))
            .as("Đủ 6 nhân viên mẫu ở CN01 và CN02")
            .isEqualTo(6);

        assertThat(count(jdbc, "SELECT COUNT(*) FROM members WHERE member_code BETWEEN 'HV999001' AND 'HV999012'"))
            .as("Đủ 12 hội viên mẫu HV999001..HV999012")
            .isEqualTo(12);

        assertThat(count(jdbc, "SELECT COUNT(*) FROM memberships"))
            .as("Tổng số gói tập mẫu là 12 (10 ACTIVE, 1 UPCOMING, 1 PENDING)")
            .isEqualTo(12);
        assertThat(count(jdbc, "SELECT COUNT(*) FROM memberships WHERE status = 'ACTIVE'"))
            .isEqualTo(10);
        assertThat(count(jdbc, "SELECT COUNT(*) FROM memberships WHERE status = 'UPCOMING'"))
            .isEqualTo(1);
        assertThat(count(jdbc, "SELECT COUNT(*) FROM memberships WHERE status = 'PENDING'"))
            .isEqualTo(1);
        // ERD: memberships.branch_id chép từ packages.branch_id (rỗng = gói toàn chuỗi). Lệch thì bán gói cùng
        // loại không thấy gói đang có: không nối tiếp ngày, không chặn gói chờ trùng (BR-F3-11), hai gói ACTIVE.
        assertThat(count(jdbc, "SELECT COUNT(*) FROM memberships ms JOIN packages p ON p.id = ms.package_id "
                + "WHERE ms.branch_id IS DISTINCT FROM p.branch_id"))
            .as("Phạm vi chi nhánh của gói mẫu khớp gói danh mục (memberships.branch_id = packages.branch_id)")
            .isZero();

        assertThat(count(jdbc, "SELECT COUNT(*) FROM invoices"))
            .as("Tổng số hóa đơn mẫu là 12 (INV2611001 + HD999001..HD999011)")
            .isEqualTo(12);
        assertThat(count(jdbc, "SELECT COUNT(*) FROM invoices WHERE status = 'PAID'"))
            .isEqualTo(11);
        assertThat(count(jdbc, "SELECT COUNT(*) FROM invoices WHERE status = 'PENDING' AND source = 'COUNTER'"))
            .isEqualTo(1);

        assertThat(count(jdbc, "SELECT COUNT(*) FROM invoice_items"))
            .isEqualTo(12);

        assertThat(count(jdbc, "SELECT COUNT(*) FROM payments WHERE status = 'CONFIRMED'"))
            .isEqualTo(11);

        assertThat(count(jdbc, "SELECT COUNT(DISTINCT day_of_week) FROM trainer_availabilities a JOIN staff s ON s.id = a.trainer_id WHERE s.staff_code = 'PT999001'"))
            .as("PT999001 có lịch rảnh đủ 7 ngày trong tuần")
            .isEqualTo(7);
        assertThat(count(jdbc, "SELECT COUNT(DISTINCT day_of_week) FROM trainer_availabilities a JOIN staff s ON s.id = a.trainer_id WHERE s.staff_code = 'PT999002'"))
            .as("PT999002 có lịch rảnh đủ 7 ngày trong tuần")
            .isEqualTo(7);
        assertThat(count(jdbc, "SELECT COUNT(*) FROM trainer_availabilities"))
            .as("Tổng số dòng lịch rảnh của 2 PT không đổi khi chạy lại")
            .isEqualTo(17);

        assertThat(count(jdbc, "SELECT COUNT(*) FROM class_sessions"))
            .as("Không tạo class_sessions trong seed")
            .isZero();
        assertThat(count(jdbc, "SELECT COUNT(*) FROM checkins c JOIN members m ON m.id = c.member_id "
                + "WHERE m.member_code = 'HV999001'"))
            .as("R__05 tạo 1 lượt check-in mẫu cho HV999001 (gói TIME toàn chuỗi)")
            .isEqualTo(1);
        assertThat(count(jdbc, "SELECT COUNT(*) FROM class_bookings"))
            .as("Không tạo class_bookings trong seed")
            .isZero();
        assertThat(count(jdbc, "SELECT COUNT(*) FROM system_configs WHERE branch_id IS NULL AND config_key IN ('BANK_ID', 'BANK_ACCOUNT_NO', 'BANK_ACCOUNT_NAME')"))
            .as("Đủ 3 khóa BANK_* toàn chuỗi trong seed R__04 (G2, S2-09.2)")
            .isEqualTo(3);
    }

    private int count(JdbcTemplate jdbc, String sql) {
        Integer result = jdbc.queryForObject(sql, Integer.class);
        return result != null ? result : 0;
    }
}
