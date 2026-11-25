# Web quản lý chuỗi phòng tập gym

Bài tập lớn môn **Nhập môn Công nghệ phần mềm (IT3180)** – Đại học Bách khoa Hà Nội.
Giảng viên hướng dẫn: **TS. Bùi Thị Mai Anh**. Nhóm 6.

## Thành viên

| Họ tên | Mã số sinh viên | Ghi chú |
| --- | --- | --- |
| Hoàng Kim Tiến | 202416367 | Trưởng nhóm (quản lý dự án) |
| Bùi Trần Đức Việt | 202416402 | |
| Nguyễn Anh Tú | 202400078 | |
| Lê Phước Minh Quân | 202416324 | |
| Thái Hùng Lân | 202416261 | |
| Nguyễn Trọng Việt | 202416399 | |
| Nguyễn Phú Trọng | 202400075 | |
| Phạm Gia Bảo | 202416425 | |

## Mô tả

Phòng tập đang quản lý hội viên, gói tập, lịch PT và doanh thu bằng sổ sách, Excel và tin nhắn, dẫn đến sai hạn gói, trùng lịch, khó đối soát doanh thu và không theo dõi được nhiều chi nhánh. Hệ thống là một web tập trung cho chủ chuỗi, quản lý chi nhánh, lễ tân, huấn luyện viên (PT) và hội viên: hội viên tự đặt lớp, đặt lịch PT; check-in và thanh toán tại quầy. Web responsive, dùng được trên máy tính quầy và điện thoại; không làm app riêng.

Phạm vi và quy tắc nghiệp vụ chuẩn: [docs/requirements/ban-chot-pham-vi.md](docs/requirements/ban-chot-pham-vi.md).

## 5 version, 20 chức năng

| Version | Chủ đề | Chức năng | Mức cam kết |
| --- | --- | --- | --- |
| v1 | Vận hành cơ bản tại quầy | F1 Tài khoản và phân quyền · F2 Hồ sơ hội viên · F3 Gói tập và gia hạn · F4 Check-in tại quầy · F5 Thanh toán tại quầy · F6 Dashboard cơ bản | Bắt buộc |
| v2 | Hội viên, lớp học và huấn luyện viên | F7 Cổng hội viên · F8 Lớp học nhóm · F9 Huấn luyện viên (PT) · F10 Thông báo | Bắt buộc |
| v3 | Vận hành nâng cao | F11 Thiết bị · F12 Bán hàng tại quầy và kho · F13 Nhân sự và lương · F14 Khuyến mãi · F15 Bảo lưu và chuyển nhượng · F16 Báo cáo nâng cao · F17 Nhật ký hoạt động | Mục tiêu |
| v4 | Chuỗi phòng tập | F18 Đa chi nhánh · F19 Gamification | Mục tiêu; cắt thứ hai nếu trễ |
| v5 | Nhận diện khuôn mặt | F20 Check-in bằng nhận diện khuôn mặt | Cắt đầu tiên nếu trễ |

## Công nghệ

| Tầng | Công nghệ |
| --- | --- |
| Frontend | React 19.3, TypeScript 6.0, Vite 8.3, Ant Design 6.6, React Router 7.18, axios 1.21, dayjs 1.11 |
| Backend | Java 21, Spring Boot 4.1.2 (Web MVC, Security, Data JPA, Validation, Actuator), springdoc-openapi 3.2.0, Lombok |
| Cơ sở dữ liệu | PostgreSQL 16, Flyway |
| Kiểm thử | JUnit 5, Mockito, Testcontainers, Postman; Vitest 5, Testing Library |
| Chất lượng mã | ESLint 10, Prettier 3.9, GitHub Actions |
| Triển khai | Vercel (frontend, chuyển tiếp `/api` sang backend – [ADR 0008](docs/decisions/0008-goi-api-cung-domain.md)), Render bằng Docker (backend), Neon (cơ sở dữ liệu) |

Phiên bản đầy đủ và lý do: [docs/decisions/0001-cong-nghe.md](docs/decisions/0001-cong-nghe.md).

## Cấu trúc thư mục

```
.
├── .github/             Mẫu issue, mẫu pull request, CODEOWNERS, CI (GitHub Actions)
├── .githooks/           Hook Git dùng chung: pre-push chặn đẩy thẳng lên main
├── backend/             Spring Boot: mã nguồn, migration Flyway, kiểm thử, Dockerfile
├── frontend/            React + Vite: mã nguồn, kiểm thử, cấu hình Vercel
├── postman/             Bộ request kiểm thử API và environment local
├── docs/
│   ├── requirements/    Bản chốt phạm vi, bối cảnh đề tài, SRS
│   ├── design/          ERD (erd-v2.dbml là bản chuẩn), use case, activity, wireframe
│   ├── decisions/       ADR – mỗi quyết định kiến trúc một file
│   ├── meetings/        Biên bản họp
│   ├── testing/         Kế hoạch, báo cáo kiểm thử
│   └── guides/          Hướng dẫn cài đặt môi trường, quy tắc mã nguồn
├── report/              Báo cáo LaTeX (sẽ bổ sung)
├── scripts/             Script tiện ích (tạo nhãn GitHub)
├── docker-compose.yml   PostgreSQL cho môi trường phát triển
├── .env.example         Mẫu biến môi trường (sao chép thành .env)
└── CONTRIBUTING.md      Quy ước làm việc của nhóm
```

## Bắt đầu nhanh

Cần có sẵn JDK 21, Node 22, Docker Desktop và Git.

```bash
git clone https://github.com/Ktin06/it3180_nhom6_gym_management.git && cd it3180_nhom6_gym_management
git config core.hooksPath .githooks          # bắt buộc: bật hook chặn đẩy thẳng lên main
cp .env.example .env                         # rồi sửa DB_PASSWORD, JWT_SECRET
docker compose up -d                         # PostgreSQL
(cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local)   # http://localhost:8080
(cd frontend && npm ci && npm run dev)       # terminal khác: http://localhost:5173
```

Swagger UI: <http://localhost:8080/swagger-ui/index.html>.

## API ảnh đại diện – S2-06 (#183)

Theo [SRS US-F2.4.2](docs/requirements/srs/dac-ta-use-case-f2-ho-so-hoi-vien.md) (AC-F2.4.2-01 đến 07) và [ADR 0011](docs/decisions/0011-anh-dai-dien-luu-trong-postgresql.md):

- `PUT /api/v1/members/{id}/avatar`: Bearer token nhân viên có quyền sửa hồ sơ; multipart trường `file`, JPEG/PNG thật ≤ 5 MiB, mỗi cạnh ≤ 8000 px; thành công trả `200` với body rỗng. Ảnh được giữ tỷ lệ, không phóng lớn, chuyển JPEG nền trắng, cạnh dài nhất ≤ 256 px và dung lượng ≤ 100 KiB.
- `GET /api/v1/members/{id}/avatar`: cùng phạm vi quyền xem hồ sơ, trả `image/jpeg`; chưa có ảnh, hồ sơ xóa mềm/không tồn tại hoặc ngoài phạm vi trả `404`. MEMBER chỉ xem chính mình; không tự đổi ảnh. TRAINER hiện bị chặn như API hồ sơ trên main.
- API tạo, xem, cập nhật và danh sách hồ sơ trả thêm boolean `hasAvatar`: hồ sơ mới/chưa có ảnh là `false`, có ảnh đã lưu là `true`. Giao diện không gọi GET ảnh khi `false` (phần backend hỗ trợ AC-F2.4.2-08); nội dung nhị phân chỉ trả qua endpoint ảnh. Danh sách kiểm tra ảnh theo cả trang, không nạp ảnh từng hồ sơ.
- Flyway `V9_04__member_avatars.sql` tạo bảng ảnh và bỏ cột `members.avatar_url`. Không trỏ nhánh này vào database prototype đã chạy migration khác (ví dụ V9_06); dùng database local riêng và cấu hình profile `local` theo hướng dẫn cài đặt.
- Thứ tự gộp/chạy Flyway: #250 (`V9_04`) → #251 (`V9_05`) → #257 (`V9_06`), ghi nhận tại [#110](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/110). F4 #242 dùng bảng ảnh của #250, cần cập nhật nhánh và kiểm thử tích hợp sau khi #250 gộp; không tạo thêm migration ảnh trong F4.

Chạy toàn bộ backend hoặc riêng các kiểm thử ảnh và hồ sơ trong PowerShell (cần Docker; Testcontainers tự tạo database kiểm thử, không cần `.env`):

```powershell
Set-Location backend
.\mvnw.cmd -B verify "-DargLine=-Duser.timezone=UTC"
# Hoặc khi chỉ kiểm tra F2:
.\mvnw.cmd test "-DargLine=-Duser.timezone=UTC" "-Dtest=MemberAvatarIntegrationTest,MemberApiIntegrationTest"
```

`MemberAvatarIntegrationTest` kiểm tra cả upload đồng thời, giá trị biên và multipart qua HTTP thật; mã TC trỏ về các AC trong SRS. JVM UTC tránh lỗi kết nối PostgreSQL với tên múi giờ `Asia/Saigon` trên một số máy Windows.

Thử thủ công: import `postman/local.postman_environment.json` và collection F2, chọn thư mục **S2-06 – Ảnh đại diện**. Lấy token qua API đăng nhập F1; request chuẩn bị tìm `HV999001` trong dữ liệu seed và tự lưu `memberId`. Chọn file ở từng PUT, để Postman tự tạo `Content-Type`. Lưu token MEMBER trong `memberAccessToken`, hội viên khác trong `otherMemberAccessToken`, nhân viên khác chi nhánh trong `otherBranchAccessToken`; không xuất các giá trị thật vào Git. Dữ liệu seed trên main chưa có sẵn nhân viên CN02, cần chuẩn bị tài khoản kiểm thử riêng cho ca khác chi nhánh. Không chạy các request thay trạng thái hồ sơ của bộ F2 cũ trước khi thử ảnh.

Đối chiếu #183: số migration cũ `V9_02` đã được cập nhật theo thứ tự hiện có trên main; mã AC được bổ sung từ ADR 0011 trong cùng thay đổi vì #127/SRS chưa có cây truy vết ảnh. Quyền TRAINER theo học viên trong SRS tổng quát chưa có trên API hồ sơ main; task này giữ cùng phạm vi hiện hành và không triển khai quyền PT của F9. Phần giao diện thuộc #203. Kết quả chạy prototype trước đây không thay thế kiểm thử và nghiệm thu nhánh này trên staging.

## Tài liệu

- [CONTRIBUTING.md](CONTRIBUTING.md) – nhánh, commit, pull request, định nghĩa hoàn thành
- [docs/README.md](docs/README.md) – mục lục tài liệu
- [docs/guides/cai-dat-moi-truong.md](docs/guides/cai-dat-moi-truong.md) – hướng dẫn cài đặt chi tiết cho Windows và macOS
