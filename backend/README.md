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

## F8 – Lịch lặp và đặt chỗ lớp nhóm

Nguồn nghiệp vụ: [SRS F8](../docs/requirements/srs/dac-ta-use-case-f8-lop-hoc-nhom.md),
UC-F8.1, F8.3, F8.7, F8.8 và [ADR 0010](../docs/decisions/0010-danh-sach-cho-tu-dong-dua-vao-lop-ban-2.md).
Các API dưới đây yêu cầu JWT. Vai trò ngoài bảng bị từ chối, kể cả chủ chuỗi tạo lịch lặp.

| Phương thức và đường dẫn (sau `/api/v1`) | Vai trò | Kết quả |
| --- | --- | --- |
| `GET /class-schedules?page=0&size=20` | Quản lý chi nhánh | `PageResponse`, size 1–100; gồm cả lịch đã ngừng, ID giảm dần |
| `GET /class-schedules/{id}` | Quản lý chi nhánh | Lịch tại chi nhánh làm việc |
| `POST /class-schedules` | Quản lý chi nhánh | 201, `Location`, lịch ACTIVE; chi nhánh lấy từ hồ sơ nhân viên |
| `PUT /class-schedules/{id}` | Quản lý chi nhánh | Sửa lịch ACTIVE; không sửa các buổi đã sinh |
| `PUT /class-schedules/{id}/deactivate` | Quản lý chi nhánh | Ngừng lịch; giữ nguyên các buổi và chỗ đặt đã sinh |
| `POST /class-sessions/{id}/bookings` | Hội viên, lễ tân | 201 cho cả BOOKED/WAITLIST, kèm `Location` |
| `GET /class-bookings/{id}` | Hội viên, lễ tân | Trạng thái, vị trí chờ hiện tại, hạn khóa đặt lớp |
| `PUT /class-bookings/{id}/cancel` | Hội viên, lễ tân | Chỗ CANCELLED, `isLateCancel`, `bookingLockedUntil` |

Quản lý chỉ thao tác lịch thuộc chi nhánh làm việc ACTIVE. Hội viên chỉ đặt/xem/hủy của mình.
Lễ tân chỉ thao tác buổi và hội viên thuộc chi nhánh làm việc. Ngoài phạm vi trả 403;
không có tài nguyên trả 404. Dữ liệu request sai trả 400 với `errors`; trùng lịch/chỗ đặt hoặc
cạnh tranh trả 409; điều kiện nghiệp vụ không đạt trả 422. Có thể thử lại request sau 409 do
danh sách chờ vừa thay đổi; cần đọc trạng thái trước khi thử lại lỗi trùng chỗ đặt.

Ví dụ nội dung tạo/sửa lịch (thay ID và ngày bằng dữ liệu hợp lệ):

```json
{
  "classTypeId": 1, "trainerId": 3, "roomId": 1,
  "dayOfWeek": 2, "startTime": "18:00:00", "endTime": "19:00:00",
  "capacity": 10, "validFrom": "2026-11-19", "validTo": "2026-12-19"
}
```

Thứ ISO 1–7; giờ lịch lặp là giờ Việt Nam. `validTo` được để null. Phòng/huấn luyện viên
phải ACTIVE tại đúng chi nhánh, tài khoản huấn luyện viên ACTIVE có vai trò TRAINER,
loại lớp ACTIVE; sức chứa không vượt phòng. Chặn các lần phát sinh thực sự trùng phòng
hoặc huấn luyện viên, cả lịch lặp, buổi lớp và buổi PT đã lưu. Hai khoảng giờ tiếp giáp không trùng.
Request đặt chỗ chỉ nhận `{"memberId": 1}`; `createdBy`, chi nhánh và trạng thái do server xác định.
ID/số lượng phải là số nguyên JSON, không nhận chuỗi hoặc cắt số thực; trường lạ bị từ chối.

### Sinh buổi nội bộ

Job chạy bù khi ứng dụng sẵn sàng và lúc phút 05 mỗi giờ, theo giờ Việt Nam.
Mặc định xét hôm nay đến hết hôm nay + 14 ngày; không dùng `BOOKING_WINDOW_DAYS` làm số ngày sinh trước.
Có thể cấu hình bằng Spring properties:

| Property | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `app.classes.generation.enabled` | `true` | Tắt job khi kiểm thử hoặc vận hành cần thiết |
| `app.classes.generation.days` | `14` | Khoảng sinh trước, 1–90 ngày |
| `app.classes.generation.cron` | `0 5 * * * *` | Lịch chạy theo `Asia/Ho_Chi_Minh` |

Mỗi lịch xử lý trong một giao dịch; lỗi được ghi log kèm ID lịch, không ngăn các lịch khác,
và được thử lại lượt tiếp theo. Unique `(class_schedule_id, occurrence_date)` chống sinh trùng;
không ghi đè buổi đã có, kể cả đã đổi giờ hoặc hủy. Ngày phát sinh theo Việt Nam,
`start_at`/`end_at` lưu bằng `timestamptz`. Job chỉ sinh buổi; không tự điểm danh hoặc gửi email.

### Khóa giao dịch và tích hợp

- Đặt/hủy lấy snapshot người chờ, khóa tất cả dòng `members` liên quan theo ID tăng dần,
  rồi khóa `class_sessions`; sau khóa kiểm tra lại điều kiện và tập ứng viên. Có ứng viên mới
  chưa khóa thì rollback/trả 409 để gọi lại bằng giao dịch mới, không khóa thêm member sau session.
- Đặt mới và hủy giải phóng chỗ dùng cùng hàm xét FIFO (`waitlisted_at`, rồi `id`). Người
  không còn TIME hợp lệ, bị khóa hoặc trùng giờ được bỏ qua và giữ thứ tự. Không vượt sĩ số;
  BOOKED, PENDING_RECONFIRM, ATTENDED, NO_SHOW đều chiếm chỗ.
- Chỉ TIME ACTIVE áp dụng tại chi nhánh, còn hiệu lực ngày học. Cửa sổ theo ngày Việt Nam;
  đúng cutoff vẫn nhận đặt/đưa vào lớp, dưới cutoff thì đóng. Cấu hình chi nhánh ghi đè toàn chuỗi.
- Hủy trước giờ học: BOOKED mới có thể bị hủy muộn; đúng mốc hủy sớm hoặc ân hạn không phạt;
  rời WAITLIST không phạt. Hủy không cho phép sau khi buổi bắt đầu. PENDING_RECONFIRM được
  xử lý bằng luồng từ chối/xác nhận đổi giờ của chức năng đổi buổi.
- Mỗi lần khóa tạo `audit_logs.action='CLASS_BOOKING_LOCK'`, `entity='members'`, `entity_id`
  là hội viên, `created_at` là mốc khóa, `new_value.lockedUntil` là hạn khóa. Đếm hủy muộn theo
  `cancelled_at`, NO_SHOW theo giờ bắt đầu buổi; chỉ trong tháng Việt Nam và sau lần khóa gần nhất.
  Không suy ngược mốc từ thời hạn khóa hoặc cấu hình số ngày. Phần sửa điểm danh phải dùng lịch sử
  các lần khóa này để xét đúng lần khóa liền trước theo SRS F8.6.
- Đưa vào lớp ghi `promoted_at`, `grace_until` và thông báo IN_APP trong cùng giao dịch; tạo
  EMAIL PENDING với `scheduled_at` trong giờ mở cửa chi nhánh. F10 gửi/thử lại email; email
  không đổi ân hạn. Hồ sơ chưa có tài khoản vẫn được ghi mốc và ân hạn, không tạo user giả.
- Tạo/sửa/ngừng lịch và sinh buổi khóa dòng `branches` trước khi kiểm tra trùng. Khi triển khai
  các luồng F9/đổi buổi khác có thay đổi lịch phòng/huấn luyện viên, cần dùng cùng khóa chi nhánh
  và kiểm tra lại lịch trong giao dịch; chỉ truy vấn trước khi ghi không đủ để chống cạnh tranh.

Không cần migration mới. Không thay đổi hoặc chạy seed trên database đang có để thử API.
Nếu chuẩn bị dữ liệu trên database có dữ liệu cần giữ, sao lưu trước theo quy trình dự án.

### Kiểm thử và Postman

```bash
./mvnw -B test -Dtest=ClassApiIntegrationTest
./mvnw -B verify
```

`ClassApiIntegrationTest` dùng database PostgreSQL 16 Testcontainers mới với toàn bộ migration;
chỉ mock đồng hồ để kiểm tra biên. Có ca cạnh tranh chỗ cuối, cùng người đặt trùng giờ,
hủy đồng thời, sinh buổi đồng thời và rollback cả hủy/đưa vào lớp/thông báo khi ghi lỗi.
Truy vết hiện dựa trên BR và kịch bản SRS: F8.1-01…06, F8.3-01…04, F8.7-01…04/06,
F8.8-01…03. Mã AC/TC chính thức cần đồng bộ khi SRS được chuẩn hóa trong task #201;
không coi tên test hiện tại là mã AC đã nghiệm thu.

Import [collection F8](../postman/f8-lich-lop-dat-cho.postman_collection.json), điền ID thật,
ngày hiệu lực và access token đúng vai trò. Có request cho từng API trong bảng trên.
Đợi job sinh buổi rồi chọn `sessionId` đã tồn tại. Không lưu token trong file collection.
Các thao tác cập nhật buổi/điểm danh/giao diện và dịch vụ gửi email có phần việc riêng;
API này chưa phải bằng chứng đã nghiệm thu trên staging.
