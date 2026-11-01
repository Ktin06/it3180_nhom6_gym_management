# Backend – Spring Boot

Java 21, Spring Boot 4.1, Maven (có wrapper `mvnw`), PostgreSQL 16, Flyway. Phiên bản đầy đủ: [docs/decisions/0001-cong-nghe.md](../docs/decisions/0001-cong-nghe.md).

## Chạy trên máy cá nhân

Yêu cầu: JDK 21, Docker Desktop đang chạy, file `.env` ở thư mục gốc repo (tạo từ `.env.example`).

```bash
# Ở thư mục gốc repo: bật PostgreSQL
docker compose up -d

# Ở thư mục backend/
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Trên Windows dùng Git Bash như trên, hoặc PowerShell: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"`.

- Health: <http://localhost:8080/actuator/health>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>

Profile `local` đọc file `../.env` (thư mục gốc repo). Profile `staging` chỉ đọc biến môi trường (Render).

Cách khác không cần `docker compose`: chạy lớp `TestGymManagementApplication` (thư mục `src/test`) từ IDE; Testcontainers tự bật PostgreSQL tạm.

## Kiểm thử

```bash
./mvnw -B verify                              # toàn bộ kiểm thử (cần Docker cho Testcontainers)
./mvnw test -Dtest=GlobalExceptionHandlerTest # một lớp kiểm thử
```

- Kiểm thử đơn vị (service, quy tắc nghiệp vụ): JUnit 5 + Mockito, không cần Spring.
- Kiểm thử tích hợp: `@SpringBootTest` + `@Import(TestcontainersConfiguration.class)`, chạy với PostgreSQL thật và toàn bộ migration Flyway.
- Mỗi quy tắc nghiệp vụ trong bản chốt phạm vi phải có ít nhất một kiểm thử JUnit.

## Quy ước gói

Đóng gói theo chức năng (xem [ADR 0007](../docs/decisions/0007-cau-truc-ma-nguon.md)):

```
vn.edu.hust.nhom6.gym
├── GymManagementApplication.java
├── config/       SecurityConfig, OpenApiConfig, JpaAuditingConfig
├── common/
│   ├── entity/   BaseEntity (id, createdAt, updatedAt)
│   ├── exception/ GlobalExceptionHandler (ProblemDetail), NotFoundException, BusinessRuleException, ConflictException
│   └── dto/      PageResponse
└── feature/      mỗi chức năng một gói con
    └── member/   controller / service / repository / entity / dto / mapper
```

- Mọi API nghiệp vụ có tiền tố `/api/v1`, ví dụ `GET /api/v1/members`.
- Controller chỉ nhận request (có `@Valid`) và gọi service; quy tắc nghiệp vụ nằm ở service.
- Entity kế thừa `BaseEntity`; trạng thái dùng `@Enumerated(EnumType.STRING)`.
- Lỗi nghiệp vụ ném `BusinessRuleException` (422), không tìm thấy ném `NotFoundException` (404), trùng dữ liệu ném `ConflictException` (409); thông điệp viết tiếng Việt.
- Xung đột cột `version` (`@Version`) và vi phạm ràng buộc cơ sở dữ liệu (chỉ mục duy nhất, CHECK) được `GlobalExceptionHandler` tự trả 409. Đây chỉ là lưới an toàn: service vẫn kiểm tra trước và ném `ConflictException` với thông điệp cụ thể.

## Cơ sở dữ liệu

Chỉ đổi schema bằng migration Flyway trong `src/main/resources/db/migration/`. Quy ước đặt tên và dải phiên bản: [db/migration/README.md](src/main/resources/db/migration/README.md). `ddl-auto` luôn là `validate`. Dữ liệu mẫu cho demo để riêng ở [db/seed](src/main/resources/db/seed/README.md), chỉ profile `local` và `staging` nạp.

## Docker

```bash
docker build -t gym-backend .
docker run --rm -p 8080:8080 -e DB_HOST=... -e DB_NAME=... -e DB_USERNAME=... -e DB_PASSWORD=... \
  -e JWT_SECRET=... -e CORS_ALLOWED_ORIGINS=... gym-backend
```

Ảnh chạy với profile `staging`, cổng lấy từ biến `PORT` (mặc định 8080).
