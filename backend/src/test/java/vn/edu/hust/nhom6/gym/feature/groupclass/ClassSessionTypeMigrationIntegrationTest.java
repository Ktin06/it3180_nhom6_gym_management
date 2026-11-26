package vn.edu.hust.nhom6.gym.feature.groupclass;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class ClassSessionTypeMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @Test
    @DisplayName("TC-F8.7.1-05c: Nâng schema giữ buổi, chỗ đặt và hỗ trợ bản backend trước")
    void migrationBackfillsExistingSessionsAndAcceptsInsertsFromPreviousBackend() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("9.05").load().migrate();
        var jdbc = new JdbcTemplate(dataSource);
        long branch = jdbc.queryForObject("SELECT id FROM branches WHERE code='CN01'", Long.class);
        long owner = jdbc.queryForObject("""
            INSERT INTO users(email,password_hash,role)
            VALUES ('trainer@migration.example','test-only','TRAINER') RETURNING id
            """, Long.class);
        long trainer = jdbc.queryForObject("""
            INSERT INTO staff(user_id,branch_id,staff_code,full_name) VALUES (?,?,'PT','Trainer') RETURNING id
            """, Long.class, owner, branch);
        long room = jdbc.queryForObject("""
            INSERT INTO rooms(branch_id,name,capacity) VALUES (?,'Studio',20) RETURNING id
            """, Long.class, branch);
        long yoga = jdbc.queryForObject("INSERT INTO class_types(name) VALUES ('Yoga') RETURNING id", Long.class);
        long zumba = jdbc.queryForObject("INSERT INTO class_types(name) VALUES ('Zumba') RETURNING id", Long.class);
        long schedule = jdbc.queryForObject("""
            INSERT INTO class_schedules(branch_id,class_type_id,trainer_id,room_id,day_of_week,
                start_time,end_time,capacity,valid_from)
            VALUES (?,?,?,?,2,'13:00','14:00',10,'2026-11-01') RETURNING id
            """, Long.class, branch, yoga, trainer, room);
        long existing = insertWithoutType(jdbc, schedule, LocalDate.of(2026, 11, 24));
        long member = jdbc.queryForObject("""
            INSERT INTO members(home_branch_id,member_code,full_name,phone)
            VALUES (?,'HV999001','Migration Member','0999999001') RETURNING id
            """, Long.class, branch);
        jdbc.update("""
            INSERT INTO class_bookings(class_session_id,member_id,created_by,status,booked_at)
            VALUES (?,?,?,'BOOKED',now())
            """, existing, member, owner);
        var originalBookings = jdbc.queryForList("SELECT * FROM class_bookings");

        var flyway = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("9.07").load();
        assertThat(flyway.migrate().migrations).extracting(migration -> migration.version).contains("9.07");
        assertThat(jdbc.queryForObject("SELECT class_type_id FROM class_sessions WHERE id=?", Long.class, existing))
            .isEqualTo(yoga);
        assertThat(jdbc.queryForList("SELECT * FROM class_bookings")).isEqualTo(originalBookings);
        assertThat(jdbc.queryForObject("SELECT capacity FROM class_sessions WHERE id=?", Integer.class, existing))
            .isEqualTo(10);

        jdbc.update("UPDATE class_schedules SET class_type_id=? WHERE id=?", zumba, schedule);
        long next = insertWithoutType(jdbc, schedule, LocalDate.of(2026, 12, 1));
        assertThat(jdbc.queryForList("SELECT class_type_id FROM class_sessions WHERE id IN (?,?) ORDER BY id",
            Long.class, existing, next)).containsExactly(yoga, zumba);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    private long insertWithoutType(JdbcTemplate jdbc, long scheduleId, LocalDate date) {
        return jdbc.queryForObject("""
            INSERT INTO class_sessions(class_schedule_id,branch_id,trainer_id,room_id,occurrence_date,
                start_at,end_at,capacity)
            SELECT id,branch_id,trainer_id,room_id,?::date,
                (?::date + start_time) AT TIME ZONE 'Asia/Ho_Chi_Minh',
                (?::date + end_time) AT TIME ZONE 'Asia/Ho_Chi_Minh',capacity
            FROM class_schedules WHERE id=? RETURNING id
            """, Long.class, date, date, date, scheduleId);
    }
}
