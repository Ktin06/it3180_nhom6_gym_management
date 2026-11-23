# Đặc tả Use Case – F1: Tài khoản và phân quyền

- **Mã chức năng:** F1
- **Version:** v1 (Bắt buộc)
- **Người biên soạn:** Bùi Trần Đức Việt
- **Người duyệt chéo:** Thái Hùng Lân
- **Tài liệu chuẩn tham chiếu:** [Bản chốt phạm vi](../ban-chot-pham-vi.md), [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml), [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md), [ADR 0001](../../decisions/0001-cong-nghe.md), [ADR 0008](../../decisions/0008-goi-api-cung-domain.md)

---

## 1. Tổng quan chức năng

Chức năng **F1 – Tài khoản và phân quyền** cung cấp cơ chế xác thực (Authentication) và phân quyền (Authorization) cho toàn bộ hệ thống quản lý chuỗi phòng tập gym:
- Hỗ trợ 6 vai trò trong hệ thống, trong đó **5 vai trò có tài khoản đăng nhập** (`SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST`, `TRAINER`, `MEMBER`) và **1 vai trò Khách** sử dụng các trang công khai không cần đăng nhập.
- **Chỉ đăng nhập bằng email** (đã chuẩn hóa chữ thường).
- Sử dụng **Access Token (JWT HS256, thời hạn 15 phút)** gửi qua header `Authorization: Bearer <token>` kết hợp với **Refresh Token (lưu mã băm SHA-256 trong CSDL, gửi qua cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`)**.
- Kiểm tra vai trò và quyền sở hữu dữ liệu ở tầng backend (`Spring Security`) trên mọi API (S3).

---

## 2. Danh sách Use Case của F1

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F1.1** | Khách tự đăng ký tài khoản hội viên | Khách | Cao | Sprint 1 |
| **UC-F1.2** | Đăng nhập bằng email | Chủ chuỗi, Quản lý, Lễ tân, PT, Hội viên | Cao | Sprint 1 |
| **UC-F1.3** | Làm mới phiên đăng nhập (Refresh Token) & Đăng xuất | Mọi người dùng đã đăng nhập | Cao | Sprint 1 |
| **UC-F1.4** | Đổi mật khẩu | Mọi người dùng đã đăng nhập | Trung bình | Sprint 1 |
| **UC-F1.5** | Quên mật khẩu và đặt lại mật khẩu | Khách / Người dùng quên mật khẩu | Trung bình | Sprint 2 |
| **UC-F1.6** | Quản lý trạng thái tài khoản & Phân quyền truy cập API | Chủ chuỗi, Quản lý chi nhánh, Hệ thống | Cao | Sprint 1 |

---

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F1.1: Khách tự đăng ký tài khoản hội viên

- **Tác nhân:** Khách (chưa có tài khoản đăng nhập).
- **Mô tả:** Khách truy cập trang đăng ký trên web để tự tạo tài khoản hội viên mới kèm hồ sơ hội viên tại chi nhánh mặc định.
- **Tiền điều kiện:**
  - Chi nhánh mặc định (`CN01`) đang ở trạng thái `ACTIVE`.
- **Hậu điều kiện:**
  - Một bản ghi mới được tạo trong bảng `users` với `role = 'MEMBER'`, `status = 'ACTIVE'`, `email` lưu chữ thường, `password_hash` băm bằng bcrypt.
  - Một bản ghi mới được tạo trong bảng `members` gắn với `user_id` vừa tạo, `home_branch_id` là chi nhánh mặc định (`CN01`), `member_code` sinh tự động theo định dạng `HV` + 6 chữ số (`HV000001`), `phone` chuẩn hóa 10 chữ số bắt đầu bằng `0`.
  - Cấp ngay cặp Access Token (JWT 15 phút) và Refresh Token (cookie `HttpOnly`) để hội viên đăng nhập tự động sau khi đăng ký.
- **Luồng chính (Basic Flow):**
  1. Khách mở màn hình Đăng ký và nhập: Họ và tên (`fullName`), Email (`email`), Số điện thoại (`phone`), Mật khẩu (`password`), Ngày sinh (`dob` – tùy chọn), Giới tính (`gender` – tùy chọn).
  2. Hệ thống chuẩn hóa `email` về chữ thường (`trim().toLowerCase()`) và chuẩn hóa `phone` về dạng 10 chữ số bắt đầu bằng `0` (ví dụ `+84 912 345 678` → `0912345678`).
  3. Hệ thống kiểm tra định dạng dữ liệu đầu vào (Bean Validation):
     - Email đúng định dạng chuẩn.
     - Số điện thoại khớp biểu thức `^0[0-9]{9}$`.
     - Mật khẩu tối thiểu 8 ký tự, bao gồm cả chữ cái và chữ số (S1).
  4. Hệ thống kiểm tra `email` chưa tồn tại trong bảng `users`.
  5. Hệ thống gọi `MemberService` (F2) để kiểm tra `phone` chưa tồn tại trong bảng `members` và tạo hồ sơ hội viên mới gắn với chi nhánh mặc định.
  6. Hệ thống băm mật khẩu bằng `BCryptPasswordEncoder`, lưu bản ghi `users` và `members` trong cùng một giao dịch CSDL (`@Transactional`).
  7. Hệ thống tạo JWT Access Token (15 phút) và Refresh Token (lưu mã băm SHA-256 vào `refresh_tokens`, đính kèm cookie `HttpOnly`), trả về thông tin tài khoản (`201 Created`).
- **Luồng thay thế và ngoại lệ (Alternative / Exception Flows):**
  - **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):** Email sai định dạng, mật khẩu dưới 8 ký tự hoặc thiếu chữ/số, số điện thoại không hợp lệ → Trả `ProblemDetail` (400) kèm danh sách lỗi theo từng trường (`errors`).
  - **E2 – Email đã được sử dụng (409 Conflict):** `email` đã tồn tại trong `users` → Từ chối và trả `ProblemDetail` (409): *"Email này đã được đăng ký tài khoản."*
  - **E3 – Số điện thoại đã thuộc hồ sơ hội viên có sẵn (409 Conflict):** `phone` đã tồn tại trong `members` (do lễ tân lập hồ sơ tại quầy trước đó hoặc hội viên khác đã dùng) → **Không tự động gắn tài khoản mới vào hồ sơ cũ** (tránh lộ dữ liệu cá nhân theo S3). Từ chối và trả `ProblemDetail` (409): *"Số điện thoại này đã có hồ sơ hội viên tại phòng tập. Vui lòng liên hệ quầy lễ tân để được kích hoạt tài khoản."*

---

### 3.2. UC-F1.2: Đăng nhập bằng email

- **Tác nhân:** Chủ chuỗi (`SUPER_ADMIN`), Quản lý chi nhánh (`BRANCH_MANAGER`), Lễ tân (`RECEPTIONIST`), Huấn luyện viên (`TRAINER`), Hội viên (`MEMBER`).
- **Tiền điều kiện:** Người dùng đã có tài khoản trong bảng `users`.
- **Hậu điều kiện:**
  - Cập nhật `last_login_at = now()`, đặt lại `failed_login_count = 0` và `locked_until = null`.
  - Tạo bản ghi `refresh_tokens` mới, đặt cookie `refresh_token` (`HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`) và trả về `accessToken` kèm thông tin người dùng.
- **Luồng chính:**
  1. Người dùng nhập `email` và `password` tại màn hình Đăng nhập.
  2. Hệ thống chuẩn hóa `email` về chữ thường và tìm tài khoản trong bảng `users`.
  3. Hệ thống kiểm tra trạng thái tài khoản:
     - `status` phải là `ACTIVE` (không phải `LOCKED` hay `DISABLED`).
     - Nếu có `locked_until`, thời điểm hiện tại phải sau `locked_until` (S4).
  4. Hệ thống đối chiếu `password` với `password_hash` bằng bcrypt.
  5. Hệ thống đặt lại `failed_login_count = 0`, `locked_until = null`, cập nhật `last_login_at`.
  6. Hệ thống sinh JWT Access Token (hết hạn sau 15 phút, chứa `sub = user.id`, `email`, `role`) và Refresh Token ngẫu nhiên (lưu SHA-256 vào `refresh_tokens.token_hash`), trả kết quả `200 OK`.
- **Luồng ngoại lệ:**
  - **E1 – Sai email hoặc mật khẩu (401 Unauthorized):**
    - Nếu email tồn tại, tăng `failed_login_count` thêm 1. Nếu `failed_login_count >= 5`, đặt `locked_until = now() + 15 phút` (S4 – hoàn thiện ở Sprint 2, cột và cấu trúc xử lý có sẵn).
    - Trả `ProblemDetail` (401) với thông báo chung: *"Email hoặc mật khẩu không đúng."* (không tiết lộ email có tồn tại hay không).
  - **E2 – Tài khoản bị khóa tạm thời do nhập sai quá 5 lần (403 Forbidden):** Khi `locked_until > now()`, từ chối đăng nhập và trả `ProblemDetail` (403) thông báo tài khoản đang bị khóa tạm thời kèm thời gian mở khóa còn lại (không dùng mã `423 Locked` vì `GlobalExceptionHandler` thống nhất trả `403 Forbidden`).
  - **E3 – Tài khoản bị khóa hoặc ngừng hoạt động (403 Forbidden):** Khi `status` là `LOCKED` hoặc `DISABLED`, trả `ProblemDetail` (403): *"Tài khoản đã bị khóa hoặc ngừng hoạt động. Vui lòng liên hệ quản trị viên."*

---

### 3.3. UC-F1.3: Làm mới phiên đăng nhập (Refresh Token) & Đăng xuất

- **Tác nhân:** Người dùng đã đăng nhập.
- **Tiền điều kiện:** Trình duyệt có cookie `refresh_token` hợp lệ gửi tới `/api/v1/auth/refresh` hoặc `/api/v1/auth/logout`.
- **Luồng chính – Làm mới token (`POST /api/v1/auth/refresh`):**
  1. Trình duyệt gửi request kèm cookie `refresh_token`.
  2. Hệ thống băm giá trị token bằng SHA-256 và tra cứu trong bảng `refresh_tokens`.
  3. Kiểm tra bản ghi tồn tại, `revoked_at IS NULL`, `expires_at > now()`, và tài khoản `users.status = 'ACTIVE'`.
  4. **Xoay vòng token (Token Rotation):** Đánh dấu `revoked_at = now()` cho bản ghi `refresh_tokens` cũ, tạo một Refresh Token mới (lưu bản ghi mới vào `refresh_tokens`, cập nhật cookie `refresh_token`) và cấp Access Token (JWT) mới.
- **Luồng chính – Đăng xuất (`POST /api/v1/auth/logout`):**
  1. Nếu có cookie `refresh_token`, hệ thống tìm bản ghi tương ứng trong `refresh_tokens` và cập nhật `revoked_at = now()`.
  2. Hệ thống trả về header `Set-Cookie` với `Max-Age=0` để xóa cookie `refresh_token` trên trình duyệt, trả `204 No Content`.
- **Luồng ngoại lệ:**
  - **E1 – Thiếu cookie, token hết hạn hoặc đã thu hồi (401 Unauthorized):** Xóa cookie `refresh_token` và trả `ProblemDetail` (401): *"Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại."*

---

### 3.4. UC-F1.4: Đổi mật khẩu

- **Tác nhân:** Người dùng đang đăng nhập.
- **Tiền điều kiện:** Người dùng đã xác thực bằng JWT hợp lệ.
- **Luồng chính (`POST /api/v1/auth/change-password`):**
  1. Người dùng gửi mật khẩu hiện tại (`currentPassword`) và mật khẩu mới (`newPassword`).
  2. Hệ thống kiểm tra định dạng dữ liệu đầu vào bằng Bean Validation (`currentPassword` không được để trống; `newPassword` tối thiểu 8 ký tự, bao gồm cả chữ cái và chữ số theo S1).
  3. Hệ thống kiểm tra quy tắc nghiệp vụ: `currentPassword` phải khớp với `users.password_hash` và `newPassword` phải khác `currentPassword`.
  4. Hệ thống cập nhật `password_hash` mới và **thu hồi toàn bộ các phiên đăng nhập hiện có** của người dùng (`UPDATE refresh_tokens SET revoked_at = now() WHERE user_id = ? AND revoked_at IS NULL`).
  5. Trả `204 No Content`.
- **Luồng ngoại lệ:**
  - **E1 – Dữ liệu đầu vào sai định dạng (400 Bad Request):** Để trống trường bắt buộc hoặc `newPassword` dưới 8 ký tự / thiếu chữ cái hoặc chữ số (Bean Validation) → Trả `ProblemDetail` (400) kèm danh sách lỗi theo từng trường (`errors`).
  - **E2 – Vi phạm quy tắc nghiệp vụ (422 Unprocessable Content):** Mật khẩu hiện tại (`currentPassword`) không đúng, hoặc mật khẩu mới (`newPassword`) trùng với mật khẩu hiện tại (`BusinessRuleException`) → Trả `ProblemDetail` (422) thông báo chi tiết lý do.

---

### 3.5. UC-F1.5: Quên mật khẩu và đặt lại mật khẩu (Sprint 2)

- **Tác nhân:** Người dùng quên mật khẩu, hoặc Hội viên có hồ sơ tại quầy vừa được lễ tân tạo tài khoản (UC-F2.2).
- **Luồng chính:**
  1. Người dùng nhập `email` yêu cầu đặt lại mật khẩu (`POST /api/v1/auth/forgot-password`).
  2. Nếu `email` tồn tại và đang `ACTIVE`, hệ thống sinh token ngẫu nhiên, lưu mã băm SHA-256 vào bảng `password_resets` (`token_hash`, `expires_at` = `now + 30 phút`), gửi đường dẫn `${FRONTEND_BASE_URL}/dat-lai-mat-khau?token=...` qua API HTTPS của Brevo (hoặc ghi log ở máy cá nhân khi `BREVO_API_KEY` rỗng theo ADR 0013). Dù email có tồn tại hay không, tài khoản không `ACTIVE` hay gửi email lỗi, API vẫn trả `200 OK` cùng một câu thông báo chung.
  3. Khi người dùng gửi token kèm mật khẩu mới hợp lệ (`POST /api/v1/auth/reset-password`): khóa dòng `password_resets` và `users` (`SELECT ... FOR UPDATE`), kiểm tra `used_at IS NULL`, `expires_at > now()` và `users.status = 'ACTIVE'`, cập nhật `users.password_hash`, xóa khóa đăng nhập tạm (`failed_login_count = 0`, `locked_until = null`), đánh dấu `password_resets.used_at = now()`, và thu hồi mọi `refresh_tokens` của tài khoản đó.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F1.5.1: Quên và đặt lại mật khẩu
- **User story:** Là **người dùng quên mật khẩu**, tôi muốn **nhận email đặt lại mật khẩu**, để **lấy lại quyền truy cập mà không cần nhờ quầy**.
- **BR liên quan:** BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08; S1, S2, S4, S6
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #121
- **Tiêu chí chấp nhận:**
  - **AC-F1.5.1-01: Yêu cầu quên mật khẩu cho tài khoản ACTIVE tạo token băm SHA-256 hạn 30 phút, gửi email và trả 200 câu chung** (BR-F1-01, BR-F1-08)
    - **Given** Người dùng mở `/quen-mat-khau` (từ liên kết **Quên mật khẩu?** trên `/login`) hoặc gọi công khai `POST /api/v1/auth/forgot-password` với email của tài khoản đang `ACTIVE` (kể cả viết hoa hoặc có khoảng trắng đầu/cuối)
    - **When** Hệ thống xử lý yêu cầu quên mật khẩu (hoặc phát hành token theo `userId` và thời hạn `ttl` 72 giờ cho luồng tạo tài khoản S2-36)
    - **Then** Hệ thống chuẩn hóa email về chữ thường, lưu duy nhất mã băm SHA-256 vào `password_resets` với `expires_at = now + 30 phút`, gửi email chứa liên kết `${FRONTEND_BASE_URL}/dat-lai-mat-khau?token=<rawToken>` và trả `200 OK` với câu thông báo chung *"Nếu email có trong hệ thống, bạn sẽ nhận được liên kết đặt lại mật khẩu."* (giao diện luôn gọi bằng `publicRequest` và hiển thị nguyên câu chung từ API)
  - **AC-F1.5.1-02: Không lộ email khi email không tồn tại, tài khoản không ACTIVE hoặc gửi email gặp lỗi** (BR-F1-08)
    - **Given** Email chưa tồn tại trong `users`, hoặc tài khoản đang `DISABLED`/`LOCKED`, hoặc dịch vụ gửi email Brevo gặp sự cố
    - **When** Người dùng gửi `POST /api/v1/auth/forgot-password`
    - **Then** Hệ thống không tạo bản ghi `password_resets` và không gửi email nếu email không tồn tại hoặc tài khoản không `ACTIVE`; nếu gửi email gặp lỗi thì bắt ngoại lệ và ghi log `ERROR` không chứa token thô; trong mọi trường hợp API vẫn trả `200 OK` cùng câu thông báo chung
  - **AC-F1.5.1-03: Đặt lại mật khẩu với token hợp lệ cập nhật mật khẩu, ghi used_at, mở khóa tạm và thu hồi toàn bộ refresh token** (BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08)
    - **Given** Tài khoản `ACTIVE` có token đặt lại mật khẩu còn hạn, chưa dùng (kể cả tài khoản đang bị khóa tạm 15 phút và có nhiều phiên `refresh_tokens` đang hiệu lực)
    - **When** Người dùng mở trang công khai `/dat-lai-mat-khau?token=...` (tiêu đề chung **Đặt mật khẩu**) hoặc gọi `POST /api/v1/auth/reset-password` với token hợp lệ và mật khẩu mới đạt chuẩn S1
    - **Then** Hệ thống khóa dòng `password_resets` và `users`, trả `204 No Content`, cập nhật `users.password_hash`, đặt `failed_login_count = 0`, `locked_until = null`, ghi `password_resets.used_at = now` và thu hồi toàn bộ `refresh_tokens` của tài khoản; mật khẩu mới đăng nhập được ngay (`200`), mật khẩu cũ và các refresh token cũ bị từ chối (`401`); giao diện chuyển về `/login` kèm thông báo đặt mật khẩu thành công
  - **AC-F1.5.1-04: Từ chối token sai, hết hạn sau 30 phút, đã dùng (kể cả gửi đồng thời) hoặc tài khoản không ACTIVE với 422** (BR-F1-08)
    - **Given** Token không tồn tại, đã quá hạn 30 phút (`expires_at <= now`), đã được sử dụng (`used_at IS NOT NULL`), tài khoản đã bị vô hiệu hóa (`DISABLED`), hoặc có 2 yêu cầu gửi đồng thời dùng cùng một token
    - **When** Người dùng gửi `POST /api/v1/auth/reset-password`
    - **Then** Hệ thống từ chối với `422 Unprocessable Content` kèm thông báo *"Liên kết đã hết hạn hoặc đã được dùng."* (khi gửi đồng thời cùng một token thì chỉ đúng 1 yêu cầu thành công `204`, yêu cầu còn lại nhận `422`), không thu hồi refresh token và không đổi mật khẩu; giao diện hiển thị đúng câu `detail` từ API kèm nút **Gửi lại liên kết** dẫn về `/quen-mat-khau`
  - **AC-F1.5.1-05: Từ chối dữ liệu đầu vào sai định dạng, thiếu token trên URL hoặc mật khẩu mới không đạt chuẩn S1** (BR-F1-01, BR-F1-03)
    - **Given** Người dùng ở `/quen-mat-khau`, `/dat-lai-mat-khau` hoặc gọi trực tiếp API
    - **When** Nhập email sai định dạng, mở `/dat-lai-mat-khau` thiếu/rỗng tham số `token`, nhập mật khẩu mới dưới 8 ký tự, thiếu chữ cái hoặc chữ số, vượt quá 72 byte UTF-8, hoặc ô nhập lại mật khẩu không khớp
    - **Then** Trình duyệt chặn gửi (hoặc báo lỗi thiếu token ngay kèm nút **Gửi lại liên kết** mà không gọi API); nếu gọi thẳng API với dữ liệu sai định dạng thì backend trả `400 Bad Request` kèm danh sách lỗi theo từng trường (`errors.email`, `errors.token`, `errors.newPassword`) và giao diện gắn lỗi vào đúng ô nhập
  - **AC-F1.5.1-06: Gửi email qua API HTTPS của Brevo khi có BREVO_API_KEY và fallback sang LogEmailSender khi khóa rỗng** (BR-F1-08)
    - **Given** Ứng dụng khởi chạy với hoặc không có cấu hình `app.mail.brevo-api-key` (`BREVO_API_KEY`) ở profile mặc định hoặc `staging`
    - **When** Spring chọn bean `EmailSender` và gửi email đặt lại mật khẩu
    - **Then** Khi có `BREVO_API_KEY` hợp lệ, bean là `BrevoEmailSender` gửi `POST https://api.brevo.com/v3/smtp/email` kèm header `api-key` và JSON body chuẩn Brevo; khi khóa rỗng hoặc toàn khoảng trắng ở profile khác `staging` thì bean là `LogEmailSender`; ở profile `staging` mà khóa rỗng thì không tạo `LogEmailSender` để tránh lộ liên kết ra log

---

### 3.6. UC-F1.6: Phân quyền truy cập API theo vai trò

- **Tác nhân:** Mọi người dùng gọi API `/api/v1/**`.
- **Luồng chính:**
  1. Bộ lọc `BearerTokenAuthenticationFilter` (`spring-boot-starter-oauth2-resource-server`) đọc JWT từ header `Authorization: Bearer <token>`, xác thực chữ ký HS256 bằng `JWT_SECRET` và hạn `exp`.
  2. `JwtAuthenticationConverter` chuyển claim `role` trong JWT thành quyền `ROLE_<ROLE>` của Spring Security (ví dụ `ROLE_SUPER_ADMIN`, `ROLE_BRANCH_MANAGER`, `ROLE_RECEPTIONIST`, `ROLE_TRAINER`, `ROLE_MEMBER`) và đặt `userId` từ claim `sub`.
  3. Các controller/service kiểm tra quyền bằng `@PreAuthorize` và kiểm tra quyền sở hữu dữ liệu (ví dụ `MEMBER` chỉ xem được dữ liệu của chính mình).
- **Luồng ngoại lệ:**
  - **Chưa đăng nhập / JWT hết hạn / sai chữ ký:** Trả `401 Unauthorized` dạng `ProblemDetail` tiếng Việt.
  - **Đã đăng nhập nhưng không đủ vai trò hoặc truy cập dữ liệu của người khác:** Trả `403 Forbidden` dạng `ProblemDetail` tiếng Việt.

---

## 4. Quy tắc nghiệp vụ và bảo mật áp dụng

Chi tiết đối chiếu với [Bản chốt phạm vi](../ban-chot-pham-vi.md):
- **BR-F1-01 – Chỉ đăng nhập bằng email:** Email luôn chuyển về chữ thường trước khi lưu và tìm kiếm (`CHECK email = lower(email)`).
- **BR-F1-02 – Chống chiếm đoạt hồ sơ qua số điện thoại (S3):** Khi khách tự đăng ký mà số điện thoại đã thuộc một hồ sơ hội viên có sẵn, hệ thống **bắt buộc từ chối (409)** và hướng dẫn liên hệ quầy; tuyệt đối không tự gắn tài khoản mới vào hồ sơ cũ.
- **BR-F1-03 – Mật khẩu (S1):** Băm bằng bcrypt (`BCryptPasswordEncoder`), tối thiểu 8 ký tự gồm cả chữ cái và chữ số, không vượt quá 72 byte UTF-8.
- **BR-F1-04 – Phiên đăng nhập (S2, ADR 0008):** Access Token (JWT) hết hạn sau **15 phút**; Refresh Token lưu mã băm SHA-256 trong bảng `refresh_tokens`, truyền qua cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`. Khi đổi mật khẩu hoặc đặt lại mật khẩu, toàn bộ refresh token đang hiệu lực của tài khoản bị thu hồi ngay trong cùng giao dịch.
- **BR-F1-05 – Phân quyền ở Backend (S3):** Mọi API nghiệp vụ dưới `/api/v1/**` (trừ các endpoint đăng ký/đăng nhập/làm mới/công khai) đều kiểm tra vai trò và quyền sở hữu dữ liệu ở backend.
- **BR-F1-06 – Khóa đăng nhập tạm thời (S4):** Khóa 15 phút (`locked_until`) sau 5 lần nhập sai mật khẩu liên tiếp (`failed_login_count`).
- **BR-F1-08 – Liên kết đặt lại mật khẩu dùng một lần và không lộ email (UC-F1.5, ADR 0013):** Token đặt lại mật khẩu chỉ lưu mã băm SHA-256 trong `password_resets` với hạn 30 phút (quên mật khẩu) hoặc 72 giờ (tạo tài khoản lần đầu cho hồ sơ có sẵn), chỉ dùng một lần (`used_at`), gửi qua API HTTPS của Brevo khi có `BREVO_API_KEY`. Yêu cầu quên mật khẩu luôn trả `200 OK` cùng câu thông báo chung dù email không tồn tại, không `ACTIVE` hay gửi email gặp sự cố, và tuyệt đối không ghi token ra log lỗi.

---

## 5. Bảng dữ liệu liên quan

| Bảng | Nhóm | Vai trò trong F1 |
| --- | --- | --- |
| `users` | Nhóm 1 | Lưu `email`, `password_hash`, `role`, `status`, `failed_login_count`, `locked_until`, `last_login_at` |
| `refresh_tokens` | Nhóm 1 | Lưu mã băm SHA-256 của refresh token (`token_hash`), hạn dùng (`expires_at`) và thời điểm thu hồi (`revoked_at`) |
| `password_resets` | Nhóm 1 | Lưu mã băm token đặt lại mật khẩu dùng một lần (`token_hash`, `expires_at`, `used_at`) |
| `members` | Nhóm 3 | Tạo hồ sơ hội viên đồng thời khi khách tự đăng ký (gọi qua `MemberService` của F2) |
| `branches` | Nhóm 2 | Lấy chi nhánh mặc định (`CN01`) khi khách tự đăng ký |

---

## 6. Thiết kế API của F1 (`/api/v1/auth`)

| Phương thức & Đường dẫn | Quyền truy cập | Mô tả | Mã phản hồi |
| --- | --- | --- | --- |
| `POST /api/v1/auth/register` | Công khai (Khách) | Khách tự đăng ký tài khoản + hồ sơ hội viên | `201`, `400`, `409` |
| `POST /api/v1/auth/login` | Công khai | Đăng nhập bằng email & mật khẩu, nhận JWT + cookie `refresh_token` | `200`, `400`, `401`, `403` |
| `POST /api/v1/auth/refresh` | Công khai (cần cookie) | Làm mới Access Token và xoay vòng Refresh Token | `200`, `401` |
| `POST /api/v1/auth/logout` | Công khai / Đã đăng nhập | Thu hồi Refresh Token và xóa cookie | `204` |
| `GET /api/v1/auth/me` | Mọi vai trò đã đăng nhập | Lấy thông tin tài khoản hiện tại từ JWT | `200`, `401` |
| `POST /api/v1/auth/change-password` | Mọi vai trò đã đăng nhập | Đổi mật khẩu và thu hồi toàn bộ refresh token | `204`, `400`, `401`, `422` |
| `POST /api/v1/auth/forgot-password` | Công khai | Yêu cầu gửi liên kết đặt lại mật khẩu qua email (câu thông báo chung) | `200`, `400` |
| `POST /api/v1/auth/reset-password` | Công khai | Đặt lại mật khẩu bằng token dùng một lần, mở khóa tạm và thu hồi toàn bộ refresh token | `204`, `400`, `422` |

### 6.1. Định dạng JSON Request / Response chuẩn (dùng chung Backend & Frontend)

1. **Đăng ký hội viên (`POST /api/v1/auth/register`):**
   - **Request Body:**
     ```json
     {
       "fullName": "Nguyễn Văn A",
       "email": "member@example.com",
       "phone": "0912345678",
       "password": "Password123",
       "dob": "2000-05-15",
       "gender": "MALE"
     }
     ```
     *(Lưu ý: `dob` và `gender` là tùy chọn; `gender` nhận `"MALE"`, `"FEMALE"`, `"OTHER"` hoặc `null`. Trường `phone` chấp nhận cả dạng `"0912345678"` và `"+84 912 345 678"`, backend tự chuẩn hóa về 10 chữ số bắt đầu bằng `0` trước khi kiểm tra).*
   - **Response (`201 Created`):** Trả kèm header `Set-Cookie: refresh_token=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth` và JSON body giống `POST /api/v1/auth/login` bên dưới.

2. **Đăng nhập (`POST /api/v1/auth/login`) và Làm mới phiên (`POST /api/v1/auth/refresh`):**
   - **Request Body (`POST /api/v1/auth/login`):**
     ```json
     {
       "email": "reception@example.com",
       "password": "Password123"
     }
     ```
     *(`POST /api/v1/auth/refresh` không có request body, đọc trực tiếp từ cookie `refresh_token`).*
   - **Response (`200 OK`):** Đặt/xoay vòng cookie `refresh_token` (`HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`) và trả JSON phẳng không bọc thêm envelope:
     ```json
     {
       "accessToken": "<jwt_hs256_token>"
     }
     ```

3. **Lấy thông tin tài khoản hiện tại (`GET /api/v1/auth/me`):**
   - **Header:** `Authorization: Bearer <access_token>`
   - **Response (`200 OK` – `AuthMeResponse`):** Trả đối tượng JSON phẳng ở cấp gốc (không bọc trong `user`), khớp với 5 giá trị `role` trong `users.role` (`SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST`, `TRAINER`, `MEMBER`):
     ```json
     {
       "userId": 15,
       "email": "reception@example.com",
       "role": "RECEPTIONIST"
     }
     ```

4. **Đổi mật khẩu (`POST /api/v1/auth/change-password`):**
   - **Header:** `Authorization: Bearer <access_token>`
   - **Request Body:**
     ```json
     {
       "currentPassword": "Password123",
       "newPassword": "NewPassword456"
     }
     ```
   - **Response:** `204 No Content`.

---

## 7. Tiêu chí chấp nhận (Acceptance Criteria – Given/When/Then)

1. **AC1 – Tự đăng ký thành công:**
   - **Given** khách nhập email chưa tồn tại, số điện thoại 10 số hợp lệ chưa có trong `members`, mật khẩu ≥ 8 ký tự có chữ và số,
   - **When** gửi `POST /api/v1/auth/register`,
   - **Then** hệ thống trả `201 Created` kèm `accessToken` (hạn 15 phút), đặt cookie `refresh_token` (`HttpOnly`, `SameSite=Strict`, `Path=/api/v1/auth`), đồng thời tạo bản ghi `users` (`role = MEMBER`) và hồ sơ `members`.
2. **AC2 – Từ chối tự đăng ký khi trùng số điện thoại:**
   - **Given** số điện thoại `0912345678` đã thuộc một hồ sơ hội viên do lễ tân tạo trước đó,
   - **When** khách dùng số `0912345678` (hoặc `+84 912 345 678`) gửi `POST /api/v1/auth/register`,
   - **Then** hệ thống trả `409 Conflict` dạng `ProblemDetail` hướng dẫn liên hệ quầy lễ tân, và không tạo tài khoản mới.
3. **AC3 – Đăng nhập và phân quyền theo vai trò:**
   - **Given** tài khoản `ACTIVE` với email và mật khẩu đúng,
   - **When** gửi `POST /api/v1/auth/login`,
   - **Then** hệ thống trả `200 OK` kèm `accessToken` chứa đúng `role`, và khi dùng token đó gọi API của vai trò cao hơn thì bị chặn với `403 Forbidden`.
4. **AC4 – Xoay vòng và thu hồi Refresh Token:**
   - **Given** người dùng đã đăng nhập và có cookie `refresh_token` hợp lệ,
   - **When** gửi `POST /api/v1/auth/refresh`,
   - **Then** token cũ bị đánh dấu `revoked_at`, hệ thống cấp `accessToken` và `refresh_token` mới; nếu dùng lại `refresh_token` cũ một lần nữa thì bị từ chối với `401 Unauthorized`.
