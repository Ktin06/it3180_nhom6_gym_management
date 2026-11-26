package vn.edu.hust.nhom6.gym.feature.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Nâng database đã có dữ liệu Sprint 1, không chỉ kiểm tra database trắng. */
@Testcontainers
class InvoiceCodeMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @Test
    @DisplayName("TC-F3.5.1-13b: Nâng schema 9.03 lên 9.05 giữ mã hóa đơn cũ và chỉ tạo sequence một lần")
    void upgradesExistingDatabaseWithoutChangingInvoiceHistory() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("9.03").load().migrate();
        var jdbc = new JdbcTemplate(dataSource);
        Long branch = jdbc.queryForObject("SELECT id FROM branches WHERE code='CN01'", Long.class);
        Long user = jdbc.queryForObject("INSERT INTO users(email,password_hash,role) VALUES ('migration@example.com','test-only','RECEPTIONIST') RETURNING id", Long.class);
        jdbc.update("""
            INSERT INTO invoices(code,branch_id,created_by,source,subtotal,total,status)
            VALUES ('INV2611001',?,?,'COUNTER',500000,500000,'PENDING')""", branch, user);
        var before = jdbc.queryForList("SELECT * FROM invoices");
        var flyway = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("9.05").load();
        assertThat(flyway.migrate().migrations).extracting(migration -> migration.version).contains("9.05");
        assertThat(jdbc.queryForList("SELECT * FROM invoices")).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT nextval('invoice_code_seq')", Long.class)).isEqualTo(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT nextval('invoice_code_seq')", Long.class)).isEqualTo(2);
    }
}
