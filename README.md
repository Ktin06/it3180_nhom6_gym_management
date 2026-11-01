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

## Tài liệu

- [CONTRIBUTING.md](CONTRIBUTING.md) – nhánh, commit, pull request, định nghĩa hoàn thành
- [docs/README.md](docs/README.md) – mục lục tài liệu
- [docs/guides/cai-dat-moi-truong.md](docs/guides/cai-dat-moi-truong.md) – hướng dẫn cài đặt chi tiết cho Windows và macOS
