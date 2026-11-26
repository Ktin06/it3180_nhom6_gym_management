package vn.edu.hust.nhom6.gym.feature.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Nâng database có lịch sử phiếu thu; giữ nguyên migration cũ và tiếp tục chặn số tiền âm. */
@Testcontainers
class PaymentAmountMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @Test
    @DisplayName("TC-F5.2.1-03b: Nâng schema 9.05 lên 9.06 giữ phiếu thu cũ, cho phép 0 và chặn số âm")
    void upgradesExistingPaymentsWithoutChangingHistory() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("9.05").load().migrate();
        var jdbc = new JdbcTemplate(dataSource);
        Long branch = jdbc.queryForObject("SELECT id FROM branches WHERE code='CN01'", Long.class);
        Long user = jdbc.queryForObject("INSERT INTO users(email,password_hash,role) VALUES ('payment-migration@example.com','test-only','RECEPTIONIST') RETURNING id", Long.class);
        Long invoice = jdbc.queryForObject("""
            INSERT INTO invoices(code,branch_id,created_by,source,subtotal,total,status,paid_at)
            VALUES ('HD000001',?,?,'COUNTER',500000,500000,'PAID',now()) RETURNING id""", Long.class, branch, user);
        jdbc.update("INSERT INTO payments(invoice_id,method,amount,status,received_by,paid_at) VALUES (?,'CASH',500000,'CONFIRMED',?,now())", invoice, user);
        var before = jdbc.queryForList("SELECT * FROM payments");

        var flyway = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
        assertThat(flyway.migrate().migrations).extracting(m -> m.version).contains("9.06");
        assertThat(jdbc.queryForList("SELECT * FROM payments")).isEqualTo(before);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        jdbc.update("INSERT INTO payments(invoice_id,method,amount,status,received_by,paid_at) VALUES (?,'CASH',0,'CONFIRMED',?,now())", invoice, user);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payments WHERE amount=0", Long.class)).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO payments(invoice_id,method,amount,status,received_by,paid_at) VALUES (?,'CASH',-1,'CONFIRMED',?,now())", invoice, user))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ck_payments_amount");
    }
}
