# Tích hợp frontend F1

Cập nhật 12/11/2026. Hợp đồng JSON đã được Bùi Trần Đức Việt (`VietBTD306`, phụ trách backend F1) xác nhận tại [comment PR #80](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/80#issuecomment-6624574276) và bổ sung vào [SRS F1 mục 6.1 tại commit a2fa017](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/a2fa01764b377fad74dac68cbeee97dce8c569d1/docs/requirements/srs/dac-ta-use-case-f1-tai-khoan-va-phan-quyen.md). Backend auth đã merge qua [#87](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/87) và [#91](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/91); vá JWT và giới hạn BCrypt đã merge qua [#105](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/105) (merge commit `4610765`); schema chung [#101](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/101) và API hồ sơ hội viên F2 [#97](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/97) cũng đã vào main. Ngày 12/11 đã kiểm tra frontend với `origin/main` `336e13c` (có #105) bằng backend, PostgreSQL và trình duyệt thật trên máy phát triển (mục [Kết quả tích hợp](#kết-quả-tích-hợp-với-backend-thật-12112026)). Kiểm tra HTTPS trên preview Vercel và backend Render staging đã đạt (mục [Staging](#kiểm-tra-https-staging-12112026-đạt)). Postman F1–F3 do [#31](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/31) theo dõi và không thuộc phạm vi của PR này.

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

Các ca này được kiểm thử bằng Vitest (giả lập ở ranh giới frontend) và thử lại trên trình duyệt với backend thật ngày 10/11 và 12/11; xem mục bên dưới để phân biệt các loại bằng chứng.

## Kết quả tích hợp với backend thật (12/11/2026)

**Môi trường (để chạy lại):**

| Thành phần | Chi tiết |
| --- | --- |
| Frontend | Nhánh `feature/f1-dang-ky-dang-nhap` tại commit merge `d2a8288`, đã merge `origin/main` `336e13c`. Node v24.18.0 |
| Backend | `origin/main` `336e13c`, gồm #105. Đóng gói bằng `backend/Dockerfile` (JDK 21 để build, JRE 21 để chạy), chạy profile `local` có dữ liệu mẫu |
| Cơ sở dữ liệu | PostgreSQL 16.15 (`postgres:16-alpine`). Flyway áp toàn bộ migration schema đến `V8_03` và các script dữ liệu mẫu lặp lại; không có lỗi migration trong log |
| Cấu hình thử | Biến môi trường truyền khi chạy container, không ghi vào repo. Đặt `DB_SSLMODE=disable` (mặc định của backend là `require`) vì container PostgreSQL cục bộ không bật TLS |
| Cổng | Cổng 8080 và 5432 của máy thử đã có tiến trình khác đang lắng nghe. Backend container publish ở cổng 8081; PostgreSQL chỉ nằm trong mạng Docker riêng. Vite chạy bằng cấu hình tạm ngoài repo, chỉ đổi đích proxy `/api` sang 8081 |
| Trình duyệt | Chromium 1248 điều khiển bằng Playwright 1.65, khung 1280 px và 360 px |

**Lưu ý khi chạy lại:** mở `http://localhost:5173`, không dùng `127.0.0.1:5173`. `CORS_ALLOWED_ORIGINS` mặc định là `http://localhost:5173`; origin khác bị backend trả `403 Invalid CORS request` ở mọi `POST` (đăng ký, đăng nhập, refresh, đăng xuất).

**Kết quả, tách theo loại bằng chứng:**

*Tầng 1: HTTP đến backend thật (curl, 24 kiểm tra, đạt 24/24)*

- Đăng ký: `201` với `{accessToken}` và cookie refresh. Bảng `users` có một hàng `MEMBER`, `ACTIVE`, mật khẩu băm bcrypt (`$2a$`). Bảng `members` có đúng một hàng liên kết.
- Trùng email: `409`. Trùng số điện thoại: `409`, không tạo tài khoản nào.
- Mật khẩu 72 byte: `201`. Mật khẩu 74 byte: `400`. Mật khẩu không có chữ số: `400`.
- Đăng nhập: đúng thông tin `200`; sai mật khẩu `401`; email không tồn tại `401`.
- `GET /auth/me`: có token `200` với `{userId,email,role}`; không token `401`; token bị sửa đổi `401`.
- Refresh: cookie hợp lệ `200` và cookie được xoay; không có cookie `401`; cookie không hợp lệ `401`; cookie cũ đã thu hồi `401`.
- Đăng xuất: `204`. Sau đăng xuất, bảng `refresh_tokens` không còn token nào chưa thu hồi của tài khoản; refresh bằng cookie đã dùng trả `401`.
- Cookie refresh: `HttpOnly; SameSite=Strict; Path=/api/v1/auth`; không có thuộc tính `Secure` ở profile `local`.
- Năm tài khoản mẫu đăng nhập `200`, `/auth/me` trả đúng vai trò `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST`, `TRAINER`, `MEMBER`.

*Tầng 1b: phân quyền trên API (đạt)*

- `GET /api/v1/members` và `GET /api/v1/packages`: không token `401`; `MEMBER` và `TRAINER` `403`; `RECEPTIONIST` `200`.
- `PUT /api/v1/packages/{id}/deactivate` (chỉ `BRANCH_MANAGER`): `MEMBER`, `TRAINER`, `RECEPTIONIST` đều `403`; `BRANCH_MANAGER` vào được handler và nhận `404` với mã không tồn tại.

*Tầng 2: trình duyệt với backend thật (Playwright, 32 kiểm tra, đạt 32/32)*

- Hội viên mới: đăng ký vào `/member`; Web Storage không có khóa nào và không có chuỗi giống JWT; `document.cookie` không chứa `refresh_token`; cookie có `HttpOnly`, `SameSite=Strict`, `Path=/api/v1/auth`. Tải lại trang và tab mới đều giữ phiên. Đăng xuất gọi `204`, về `/login`; tải lại không khôi phục phiên; truy cập `/member` về `/login`. Đăng ký lại cùng email bị từ chối `409` và hiện lỗi; đăng nhập lại được bằng tài khoản vừa tạo.
- Đăng nhập: sai mật khẩu hiện thông báo và ở lại trang. Năm tài khoản mẫu vào đúng trang: `/admin`, `/manager`, `/reception`, `/trainer`, `/member`. Hội viên vào `/admin` bị chuyển sang `/403`.
- Lỗi mạng và phục hồi (lỗi được tạo bằng cách chặn request trong trình duyệt, backend vẫn chạy; khác với lần 10/11 khi dừng hẳn container backend):
  - Logout bị chặn: hiện cảnh báo “Đăng xuất chưa hoàn tất”, vẫn ở `/member`. Tải lại khôi phục phiên, đúng hành vi đã ghi ở trên vì request logout chưa tới server. Đăng xuất lại khi mạng hoạt động thì về `/login`.
  - Refresh `500` khi tải lại: hiện “Chưa thể xác minh phiên đăng nhập”, không hiện nội dung hội viên. Sau khi lỗi qua đi, “Thử lại” khôi phục phiên.
  - Refresh `401`: phiên kết thúc, về `/login`.
- Đăng ký thành công nhưng `/auth/me` bị chặn: hiện “Tài khoản đã được tạo”. “Thử tải thông tin tài khoản” chỉ gọi `/auth/me`; số lần gọi `POST /auth/register` vẫn là 1, và vào `/member`.
- Bố cục 360 px: `/register` và `/login` có `scrollWidth` bằng `clientWidth` (360).
- Không có lỗi JavaScript không được bắt trong các kịch bản đăng ký, đăng nhập và đăng xuất (kịch bản A và B). Các kịch bản lỗi mạng và 360 px không thu lỗi JavaScript.

*Tầng 3: Vitest và build của frontend (không thay thế tầng 1–2)*

- ESLint: đạt. Vitest: 10 file, 54 kiểm tra, đạt. `tsc -b` và `vite build` trong `npm run build`: đạt; bundle vẫn có cảnh báo kích thước 972 kB đã biết.

Kịch bản Playwright chỉ chạy cục bộ, không lưu trong repo; các bước trên đủ để chạy lại thủ công.

## Kiểm tra HTTPS staging (12/11/2026): đạt

Sau khi trưởng nhóm tắt Vercel Authentication cho project, preview của nhánh mở được ẩn danh và đã chạy lại các bước HTTPS lúc 22:50–22:55 ngày 12/11 (giờ Việt Nam).

| Thành phần | Quan sát |
| --- | --- |
| Frontend | Preview của nhánh `https://it3180-nhom6-gym-git-feature-f1-dang-ky-dang-nhap-ktin06.vercel.app`, bản triển khai Vercel của commit `093fa55` (hoàn tất 14:17 UTC). `/login` trả `200`; bundle chứa chuỗi của #80 (“Nhập lại mật khẩu”, “Chưa thể xác minh phiên đăng nhập”, “Tài khoản đã được tạo”). Production `it3180-nhom6-gym.vercel.app` vẫn chưa chứa mã của #80 nên không dùng làm bằng chứng |
| Backend | Render `https://it3180-nhom6-gym-management.onrender.com`, `/actuator/health` trả `UP`. Lần gọi đầu mất khoảng 1–2 phút vì gói miễn phí ngủ khi không có request. Render không công khai commit (`/actuator/info` yêu cầu đăng nhập) nên chưa xác nhận được mã đang chạy; hành vi giới hạn mật khẩu của #105 có mặt: đăng ký với mật khẩu 73 byte trả `400` “Mật khẩu không được vượt quá 72 byte” |
| Đường đi | Trình duyệt chỉ gọi `/api/v1/...` trên origin của preview; Vercel rewrite sang Render theo `vercel.json`, nên cookie thuộc origin của preview |
| Trình duyệt | Chromium 157.0.8103.38 (bản snap trên máy thử) điều khiển bằng Playwright, khung 1280 px và 360 px |

Tài khoản thử là hội viên mới do kiểm thử tự đăng ký trên preview (họ tên “Kiem Thu HTTPS 12-11”, email thuộc `example.com`). Không có mật khẩu hay tài khoản seed nào được dùng hoặc ghi vào repo. Tài khoản này vẫn nằm trong cơ sở dữ liệu staging vì chưa có API xóa tài khoản; trưởng nhóm có thể dọn khi làm mới dữ liệu staging.

**Kết quả (15/15 đạt):**

- Đăng ký qua HTTPS: `201`, sau đó `GET /auth/me` `200` và vào `/member`.
- Cookie `refresh_token` (quan sát qua trình duyệt và qua header `Set-Cookie`): `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`, `Max-Age=604800`, thuộc origin của preview. `document.cookie` rỗng; `localStorage` và `sessionStorage` không có khóa nào.
- Tải lại trang: `POST /auth/refresh` `200` rồi `GET /auth/me` `200`, vẫn ở `/member`. Tab mới vào `/member` cũng khôi phục phiên.
- Đăng xuất: `204`, về `/login`. Sau đó tải lại và vào `/member`: `POST /auth/refresh` trả `401`, bị chuyển về `/login`, không khôi phục phiên.
- Đăng nhập sai mật khẩu: `401`, ở lại `/login`. Đăng nhập đúng: `200`, vào `/member`.
- Rewrite Vercel → Render hoạt động: `/api/v1/auth/me` không token qua preview trả `401` “Chưa xác thực” từ backend.
- Bố cục 360 px ở `/member`: không tràn ngang (`scrollWidth` bằng chiều rộng khung nhìn).

Hai kiểm tra trong lần chạy đầu (đăng ký rồi `/auth/me` `200`, và đăng xuất `204`) báo lỗi do kịch bản đọc danh sách request trước khi trình nghe kịp ghi; nhật ký request cho thấy cả hai đều đã xảy ra (`/auth/me` `200` ngay sau đăng ký, `/auth/logout` `204`). Kết quả 15/15 dựa trên đối chiếu nhật ký đó. Kịch bản chỉ chạy cục bộ, không lưu trong repo. Chưa kiểm thử lỗi mạng và phục hồi trên staging (đã kiểm thử ở tầng 2 với backend cục bộ), và chưa kiểm thử vai trò nhân viên trên staging vì chưa có tài khoản được cấp.

## Chạy lại thủ công

Làm theo [cai-dat-moi-truong.md](cai-dat-moi-truong.md): tạo file môi trường từ `.env.example` (đặt `JWT_SECRET` ngẫu nhiên từ 32 ký tự và `SEED_PASSWORD_HASH` theo [db/seed/README.md](../../backend/src/main/resources/db/seed/README.md)), `docker compose up -d postgres`, chạy backend profile `local`, rồi `npm run dev` ở `frontend/` và mở `http://localhost:5173`. Nếu PostgreSQL cục bộ không bật TLS, thêm `DB_SSLMODE=disable` vào môi trường backend. Thử lần lượt các mục ở tầng 1 và 2 ở trên. Để tạo lỗi mạng thật, dừng backend rồi bật lại (cách này đã dùng ngày 10/11).

## Điều kiện gộp PR

Theo [review của trưởng nhóm](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/80#pullrequestreview-5756512423), PR #80 giữ Draft tới khi backend auth đã merge và tích hợp thật được xác minh. Backend auth, #105, schema #101 và API F2 #97 đã vào main, nên PR không còn phụ thuộc PR nào khác. Tích hợp thật đã được xác minh cục bộ (tầng 1 và 2) và qua HTTPS trên preview Vercel với backend Render staging (mục trên). Postman F1–F3 do [#31](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/31) theo dõi và không nằm trong điều kiện của #80. Nhánh này bỏ công cụ giả lập vai trò (`DevRoleSwitcher`). Việc approve và merge do người duyệt quyết định.
