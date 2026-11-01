# ADR 0001: Công nghệ – React + Spring Boot + PostgreSQL + Flyway

- **Ngày:** 01/11/2026 (tech stack chốt ở buổi kick-off, phiên bản ghi khi khởi tạo repo)
- **Người quyết định:** cả nhóm 6; ghi lại: Hoàng Kim Tiến
- **Trạng thái:** Đã chấp nhận

## Bối cảnh

Nhóm 8 người làm web quản lý chuỗi phòng tập trong 4 sprint. Giảng viên ưu tiên chức năng quản lý dễ kiểm thử, dễ demo. Báo cáo cần sơ đồ lớp khớp với mã nguồn. Mã được viết với hỗ trợ của công cụ AI lập trình. Nguồn: mục "Tech stack" trong [bản chốt phạm vi](../requirements/ban-chot-pham-vi.md).

## Quyết định

| Tầng | Công nghệ | Lý do (theo bản chốt phạm vi) |
| --- | --- | --- |
| Frontend | React, TypeScript, Vite, Ant Design | Có sẵn bảng, form, lịch cho web quản lý; AI sinh code React tốt |
| Backend | Java, Spring Boot (Spring Web, Spring Security, Spring Data JPA) | Phân tầng rõ (controller, service, repository) nên sơ đồ lớp khớp mã nguồn; có sẵn phân quyền, validate, giao dịch; kiểm thử JUnit chuẩn mực |
| Cơ sở dữ liệu | PostgreSQL, Flyway | Flyway quản lý thay đổi schema bằng các file migration có đánh số, 8 người không sửa đè nhau |
| Tài liệu API | springdoc-openapi (Swagger) | Sinh tự động, không phải viết tay |
| Kiểm thử | JUnit 5, Mockito, Testcontainers, Postman; Vitest + Testing Library cho frontend | Kiểm thử đơn vị, kiểm thử với PostgreSQL thật, kiểm thử API |
| Triển khai | Vercel (frontend), Render (backend chạy bằng Docker), Neon (cơ sở dữ liệu) | Có gói miễn phí, đủ cho demo |

Các thành phần sau đã chốt nhưng **chưa thêm** vào khung, sẽ thêm khi làm chức năng tương ứng: Spring Mail (F10), WebSocket STOMP (F10), face-api.js (F20), thư viện JWT (F1). Thư viện JWT là `spring-boot-starter-oauth2-resource-server`, ký HS256 bằng `JWT_SECRET` (chốt 01/11/2026): thư viện chính thức của Spring Security, không phải tự viết bộ lọc đọc token.

## Phiên bản đã dùng khi khởi tạo repo (01/11/2026)

Chọn bản ổn định mới nhất tại thời điểm khởi tạo, trừ các ngoại lệ ghi rõ.

**Backend**

| Thành phần | Phiên bản | Ghi chú |
| --- | --- | --- |
| Java (JDK) | 21 (LTS) – máy khởi tạo dùng Temurin 21.0.13 | Đề bài chọn 21; Spring Initializr đã có 25 nhưng giữ 21 để cả nhóm cài giống nhau |
| Spring Boot | 4.1.2 | Bản ổn định mới nhất trên start.spring.io |
| Maven (qua wrapper) | 3.10.0, Maven Wrapper 3.3.4 | Sinh bởi Spring Initializr |
| springdoc-openapi-starter-webmvc-ui | 3.2.0 | Dòng 3.x dành cho Spring Boot 4; bản 3.2.0 được build trên Spring Boot 4.1.2 |
| Flyway | theo Spring Boot 4.1.2 (kèm `flyway-database-postgresql`) | |
| PostgreSQL JDBC, Hibernate, Lombok, Testcontainers | theo quản lý phiên bản của Spring Boot 4.1.2 | Spring Boot 4 tách starter theo mô-đun: dùng `spring-boot-starter-webmvc`, `spring-boot-starter-flyway` và các starter kiểm thử tương ứng (`spring-boot-starter-security-test` đã gồm `spring-security-test`, các starter `*-test` đã gồm `spring-boot-starter-test`) |
| PostgreSQL | 16 (ảnh Docker `postgres:16`, cũng dùng trong Testcontainers) | Theo đề bài; Neon hỗ trợ 16 |

**Frontend**

| Thành phần | Phiên bản | Ghi chú |
| --- | --- | --- |
| Node.js | 22 (`.nvmrc`), yêu cầu ≥ 22.22.2 | Theo đề bài. jsdom 30 cần ≥ 22.22.2. Node 22 hết hỗ trợ tháng 4/2027, sau khi kết thúc học phần |
| Vite | 8.3.5 (create-vite 9.3.3, mẫu `react-ts`) | |
| React, React DOM | 19.3.1 | |
| TypeScript | 6.0.3 | **Không** dùng 7.0 (mới nhất): typescript-eslint 8.72 chỉ hỗ trợ TypeScript < 6.1 |
| Ant Design / @ant-design/icons | 6.6.8 / 6.3.4 | locale `vi_VN` |
| react-router-dom | 7.18.5 | |
| axios | 1.21.0 | |
| dayjs | 1.11.23 | locale `vi` |
| ESLint | 10.14.0 (typescript-eslint 8.72.1, react-hooks 7.1.1, react-refresh 0.5.7) | Mẫu Vite mới dùng oxlint; đổi sang ESLint theo yêu cầu của nhóm |
| Prettier | 3.9.10 | |
| Vitest | 5.0.5 (jsdom 30.1.3, Testing Library React 16.3.3) | |

**Công cụ khác:** Docker Compose (ảnh `postgres:16`), GitHub Actions (`actions/checkout@v4`, `actions/setup-java@v4`, `actions/setup-node@v4`, `dorny/paths-filter@v3`).

## Phương án đã cân nhắc

| Phương án | Lý do không chọn |
| --- | --- |
| Node.js (Express/NestJS) cho backend | Sơ đồ lớp phân tầng khó khớp bằng; ít kinh nghiệm giao dịch, phân quyền có sẵn như Spring |
| `ddl-auto: update` thay cho Flyway | 8 người sửa entity sẽ ghi đè schema của nhau, không có lịch sử thay đổi |
| MySQL | PostgreSQL có chỉ mục duy nhất có điều kiện (cần cho đặt chỗ, trừ buổi), `timestamptz`, `jsonb`; Neon miễn phí |

## Hệ quả

- Ai cũng cần cài JDK 21, Node 22, Docker Desktop (xem [hướng dẫn cài đặt](../guides/cai-dat-moi-truong.md)).
- Kiểm thử tích hợp cần Docker (Testcontainers); CI trên `ubuntu-latest` có sẵn Docker.
- Khi nâng phiên bản lớn (ví dụ TypeScript 7 khi typescript-eslint hỗ trợ), cập nhật ADR này.
