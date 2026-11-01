package vn.edu.hust.nhom6.gym;

import org.springframework.boot.SpringApplication;

/** Chạy ứng dụng với PostgreSQL của Testcontainers, không cần docker compose (chạy từ IDE). */
public class TestGymManagementApplication {

    public static void main(String[] args) {
        SpringApplication.from(GymManagementApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
