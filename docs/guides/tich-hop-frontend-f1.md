# Tích hợp frontend F1

Cập nhật 10/11/2026. Hợp đồng JSON đã được Bùi Trần Đức Việt (`VietBTD306`, phụ trách backend F1) xác nhận tại [comment PR #80](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/80#issuecomment-6624574276) và bổ sung vào [SRS F1 mục 6.1 tại commit a2fa017](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/a2fa01764b377fad74dac68cbeee97dce8c569d1/docs/requirements/srs/dac-ta-use-case-f1-tai-khoan-va-phan-quyen.md). Backend auth đã merge qua [#87](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/87) và [#91](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/91); schema chung [#101](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/101) và API hồ sơ hội viên F2 [#97](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/97) cũng đã vào main. Ngày 10/11 đã kiểm tra frontend với backend và cơ sở dữ liệu thật trên máy phát triển (mục [Kết quả tích hợp](#kết-quả-tích-hợp-với-backend-thật-10112026)); môi trường staging Vercel/Render và Postman F1–F3 chưa được xác minh.

## Đường dẫn và dữ liệu đã thống nhất

Axios dùng base `/api/v1`; các hàm trong `features/auth/api/` truyền phần đường dẫn `/auth/...`. Trình duyệt gọi cùng domain để cookie refresh đi qua proxy Vite/Vercel theo ADR 0008.

| API | Yêu cầu / phản hồi | Dùng ở frontend |
| --- | --- | --- |
| `POST /api/v1/auth/register` | `fullName`, `email`, `phone`, `password`, `dob?`, `gender?`; trả `201`, `{accessToken}` và cookie refresh | Form gửi trường bắt buộc và `dob?`; không gửi `gender` vì trường này tùy chọn |
| `POST /api/v1/auth/login` | `email`, `password`; trả `200`, `{accessToken}` và cookie refresh | Đăng nhập bằng email/mật khẩu |
| `POST /api/v1/auth/refresh` | Không có body, dùng cookie `refresh_token`; trả `200`, `{accessToken}`, xoay cookie | Client riêng cùng domain, không gắn bearer |
| `POST /api/v1/auth/logout` | Thu hồi cookie refresh, trả `204` | Chỉ xóa access token và điều hướng sau khi server xác nhận; nếu lỗi mạng thì giữ giao diện hiện tại để thử lại. Tải lại trang sẽ kiểm tra phiên bằng cookie refresh hiện có |
| `GET /api/v1/auth/me` | Bearer access token; trả `200`, `{userId,email,role}` | Sau lấy token, đọc tài khoản và ánh xạ email/role cho menu |

## DTO xác nhận

Login/register/refresh trả JSON phẳng, không bọc thêm đối tượng:

```json
{ "accessToken": "<jwt>" }
```

`GET /api/v1/auth/me` trả JSON phẳng:

```json
{ "userId": 15, "email": "reception@example.com", "role": "RECEPTIONIST" }
```

- `AuthMeResponse.userId` là số trong JSON, ánh xạ khóa `bigint` backend. Adapter kiểm tra số nguyên dương; `AuthUser` hiện chỉ giữ email/role vì các màn hình hiện tại chưa dùng ID.
- Vai trò hợp lệ: `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST`, `TRAINER`, `MEMBER`.
- `RegisterRequest.gender?`: `MALE`, `FEMALE`, `OTHER` hoặc `null`. Form hiện để trống/không gửi trường tùy chọn này; kiểu TypeScript hỗ trợ đầy đủ tập giá trị đã chốt, không còn dùng chuỗi bất kỳ.
- Backend nhận số điện thoại `0912345678` hoặc `+84 912 345 678`, tự chuẩn hóa về 10 chữ số bắt đầu bằng `0` trước khi kiểm tra. Frontend vẫn chuẩn hóa để hỗ trợ người dùng; backend chịu trách nhiệm kiểm tra và tính duy nhất.

Refresh cookie: `HttpOnly; SameSite=Strict; Path=/api/v1/auth`, kèm `Secure` ở staging (`app.auth.cookie-secure: true`); profile `local` không đặt `Secure` để chạy được trên `http://localhost`. JWT sống 15 phút và chỉ lưu trong bộ nhớ tab; frontend không đọc cookie hoặc ghi token vào Web Storage. Mật khẩu xác nhận chỉ dùng phía giao diện, không nằm trong request.

## Lỗi và phục hồi phiên sau review 07/11

- Khi đăng ký đã thành công nhưng `/auth/me` lỗi, giao diện báo tài khoản đã được tạo. Nút tải lại thông tin chỉ hoàn tất phiên, không gửi lại đăng ký; người dùng cũng có thể chuyển sang đăng nhập. Nội dung cần quyền vẫn đóng tới khi nhận được identity hợp lệ.
- Refresh trả `401` xác nhận phiên không hợp lệ thì kết thúc phiên. Lỗi kết nối hoặc server tạm thời đưa giao diện vào trạng thái chưa xác minh, ẩn nội dung được bảo vệ và cho thử lại; không tự coi là đã đăng xuất. Token access chỉ ở bộ nhớ, cookie vẫn do backend quản lý.
- Logout chỉ kết thúc phiên sau khi backend xác nhận. Lỗi mạng giữ giao diện hiện tại và cảnh báo “đăng xuất chưa hoàn tất” qua message portal; nút Thử lại vẫn có thể dùng. Nếu tải lại trước khi logout thành công, bootstrap kiểm tra cookie và có thể khôi phục phiên còn hiệu lực. Frontend không tự xóa được cookie HttpOnly.

Các ca này vừa được kiểm thử bằng Vitest (giả lập ở ranh giới frontend) vừa được thử lại với backend thật ngày 10/11; xem mục bên dưới để phân biệt hai loại bằng chứng.

## Kết quả tích hợp với backend thật (10/11/2026)

**Môi trường (để chạy lại):**

| Thành phần | Chi tiết |
| --- | --- |
| Frontend | Nhánh `feature/f1-dang-ky-dang-nhap` tại `a41bced` (đã đồng bộ main `872386d` vào nhánh), Node v24.18.0, `npm run dev` |
| Backend | Đúng mã main `872386d`: nhánh này không đổi `backend/`. Đóng gói bằng `backend/Dockerfile`, chạy profile `local` có dữ liệu mẫu; Flyway áp 45 migration (schema và `db/seed`) |
| Cơ sở dữ liệu | PostgreSQL 16. Máy thử dùng ảnh `postgres:16-alpine` có sẵn thay cho `postgres:16` trong `docker-compose.yml`, cùng bản chính |
| Trình duyệt | Chromium điều khiển bằng Playwright, khung 1280 px và 360 px |
| Cổng | Cổng 8080 của máy thử đang do dự án khác dùng, nên backend chạy ở 8081 và Vite trỏ proxy `/api` sang 8081 bằng một file cấu hình tạm, không commit. Cấu hình mặc định của repo vẫn là `localhost:8080` (`frontend/vite.config.ts`) |

**Lưu ý khi chạy lại:** mở `http://localhost:5173`, không dùng `127.0.0.1:5173`. `CORS_ALLOWED_ORIGINS` mặc định là `http://localhost:5173`; origin khác bị backend trả `403 Invalid CORS request` ở mọi `POST` (đăng ký, đăng nhập, refresh, đăng xuất). Đã gặp lỗi này khi thử lần đầu và xác nhận bằng `curl` với hai giá trị `Origin`.

**Kết quả:** 15/15 kiểm tra đạt. "Thật" nghĩa là không giả lập API; "dừng backend" nghĩa là lỗi mạng tạo thật bằng cách dừng container backend rồi bật lại.

| # | Kiểm tra | Phạm vi | Kết quả |
| --- | --- | --- | --- |
| 1 | Đăng ký hội viên mới qua giao diện | Thật | `POST /auth/register` 201, `GET /auth/me` 200, vào `/member`; `localStorage`/`sessionStorage` rỗng |
| 2 | Hồ sơ lưu bền vững | Thật | `users` có vai trò `MEMBER`, trạng thái `ACTIVE`, mật khẩu là mã băm bcrypt; `members` có mã `HVxxxxxx`, `user_id` trỏ đúng tài khoản |
| 3 | Cookie refresh | Thật | Một cookie `refresh_token`: `HttpOnly`, `SameSite=Strict`, `Path=/api/v1/auth`, `Secure=false` (profile `local`); `document.cookie` rỗng |
| 4 | Tải lại trang giữ phiên | Thật | `POST /auth/refresh` 200 rồi `GET /auth/me` 200; tab mới cùng trình duyệt vẫn đăng nhập |
| 5 | Đăng xuất rồi tải lại | Thật | `POST /auth/logout` 204; `/member` chuyển về `/login`; refresh lại bằng cookie cũ trả 401; 0 refresh token còn hiệu lực trong CSDL |
| 6 | Đăng nhập bằng tài khoản vừa đăng ký | Thật | `POST /auth/login` 200, vào `/member` |
| 7 | Sai mật khẩu | Thật | Hiện “Email hoặc mật khẩu không chính xác.”, không chuyển trang |
| 8 | Đăng nhập năm vai trò dữ liệu mẫu | Thật | `SUPER_ADMIN`→`/admin`, `BRANCH_MANAGER`→`/manager`, `RECEPTIONIST`→`/reception`, `TRAINER`→`/trainer`, `MEMBER`→`/member` |
| 9 | Đăng ký trùng SĐT hồ sơ hội viên đã có | Thật | Bị từ chối với thông báo “đã có hồ sơ hội viên”; không tạo tài khoản cho email đó |
| 10 | Đăng ký trùng email | Thật | `POST /auth/register` 409; lỗi “Email đã được đăng ký trong hệ thống.” hiện dưới ô Email; vẫn đúng một tài khoản |
| 11 | Đăng ký thành công nhưng `/me` lỗi | Lai: đăng ký thật, `/me` bị chặn bằng Playwright | Giao diện báo “Tài khoản đã được tạo”; sau khi bỏ chặn, “Thử tải thông tin tài khoản” chỉ gọi `/me` (không `POST /auth/register` lần hai) và vào `/member`; vẫn đúng một tài khoản và một hồ sơ |
| 12 | Đăng xuất khi backend ngừng | Thật, dừng backend | `POST /auth/logout` 502; cảnh báo “Đăng xuất chưa hoàn tất”, phiên và trang giữ nguyên, nút “Thử đăng xuất lại”; sau khi bật backend, thử lại trả 204 và về `/login` |
| 13 | Tải lại khi backend ngừng | Thật, dừng backend | `POST /auth/refresh` 502; hiện “Chưa thể xác minh phiên đăng nhập”, không lộ nội dung được bảo vệ; sau khi bật backend, “Thử lại” gọi refresh 200 và `/me` 200 rồi quay lại trang |
| 14 | Bố cục 360 px | Thật | `/register` và `/login` không tràn ngang (`scrollWidth` 360/360) |
| 15 | Lỗi JavaScript trên trang | Thật | Không có |

Chạy thêm trên nhánh đã đồng bộ main: ESLint, kiểm tra kiểu TypeScript và 10 file/54 Vitest đều qua. Bước đóng gói chỉ chạy ở CI của PR.

**Chưa được kiểm chứng:**

- **Staging Vercel/Render:** GET lúc 10:06 UTC ngày 10/11 cho thấy frontend HTTP 200, nhưng `/api/v1/auth/me` qua Vercel không phản hồi trong 20 giây và `/actuator/health` của Render không phản hồi trong 60 giây. Chưa xác nhận backend staging chạy, nên cookie `Secure`, rewrite của Vercel và dữ liệu staging chưa được kiểm tra. Không kết luận backend hỏng từ timeout vì Render Free có thể khởi động chậm.
- **Postman F1–F3:** theo dõi tại [#31](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/31); chưa chạy trong lần kiểm tra này.
- **PR [#105](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/105)** (vá JWT, giới hạn mật khẩu BCrypt) chưa merge nên không nằm trong backend đã thử.
- Kịch bản Playwright chỉ chạy cục bộ, không được lưu trong repo; các bước ở bảng trên đủ để chạy lại thủ công.

## Chạy lại thủ công

Làm theo [cai-dat-moi-truong.md](cai-dat-moi-truong.md): tạo file môi trường từ `.env.example` (đặt `JWT_SECRET` ngẫu nhiên từ 32 ký tự và `SEED_PASSWORD_HASH` theo [db/seed/README.md](../../backend/src/main/resources/db/seed/README.md)), `docker compose up -d postgres`, chạy backend profile `local`, rồi `npm run dev` ở `frontend/` và mở `http://localhost:5173`. Thử lần lượt các dòng 1–13 ở bảng trên. Dòng 12–13: dừng backend rồi bật lại để tạo lỗi mạng thật.

## Điều kiện gộp PR

Theo [review của trưởng nhóm](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/80#pullrequestreview-5756512423), PR #80 giữ Draft tới khi backend auth đã merge và tích hợp thật được xác minh. Phần backend đã đủ (#25/#26, #101, #97 đã merge) và đã được xác minh cục bộ như trên; việc còn lại là kiểm tra staging và Postman theo #31. Việc chuyển sang Ready do người duyệt quyết định. Nhánh này bỏ công cụ giả lập vai trò (`DevRoleSwitcher`), nên chỉ gộp khi đăng nhập thật dùng được trên nhánh chính để đồng đội không bị chặn.
