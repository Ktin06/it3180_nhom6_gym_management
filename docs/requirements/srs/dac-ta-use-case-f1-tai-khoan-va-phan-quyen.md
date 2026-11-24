# Đặc tả Use Case – F1: Tài khoản và phân quyền

- **Mã chức năng:** F1
- **Version:** v1 (Bắt buộc)
- **Người biên soạn:** Bùi Trần Đức Việt
- **Người duyệt chéo:** Thái Hùng Lân (Sprint 1) & Nguyễn Anh Tú (rà SRS Sprint 2, S2-32 #204)
- **Cập nhật 24/11/2026 (Bùi Trần Đức Việt):** viết lại mục H của UC-F1.1 đến UC-F1.6 theo cây truy vết ([ADR 0012](../../decisions/0012-quy-uoc-ma-truy-vet.md)): 6 user story, 29 AC có mã và mã quy tắc nghiệp vụ `BR-F1-01` đến `BR-F1-09`; bổ sung quy tắc gửi email qua Brevo ([ADR 0013](../../decisions/0013-gui-email-qua-brevo.md)), hạn liên kết quên mật khẩu 30 phút và ngoại lệ cho UC-F1.5.
- **Tài liệu chuẩn tham chiếu:** [Bản chốt phạm vi](../ban-chot-pham-vi.md), [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml), [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md), [ADR 0001](../../decisions/0001-cong-nghe.md), [ADR 0008](../../decisions/0008-goi-api-cung-domain.md), [ADR 0012](../../decisions/0012-quy-uoc-ma-truy-vet.md), [ADR 0013](../../decisions/0013-gui-email-qua-brevo.md)

---

## 1. Tổng quan chức năng

Chức năng **F1 – Tài khoản và phân quyền** cung cấp cơ chế xác thực (Authentication) và phân quyền (Authorization) cho toàn bộ hệ thống quản lý chuỗi phòng tập gym:
- Hỗ trợ 6 vai trò trong hệ thống, trong đó **5 vai trò có tài khoản đăng nhập** (`SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST`, `TRAINER`, `MEMBER`) và **1 vai trò Khách** sử dụng các trang công khai không cần đăng nhập.
- **Chỉ đăng nhập bằng email** (đã chuẩn hóa chữ thường); không hỗ trợ đăng nhập bằng số điện thoại hay mạng xã hội.
- Sử dụng **Access Token (JWT HS256, thời hạn 15 phút)** gửi qua header `Authorization: Bearer <token>` kết hợp với **Refresh Token (lưu mã băm SHA-256 trong CSDL, gửi qua cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`)**.
- Kiểm tra vai trò, trạng thái tài khoản/chi nhánh nhân viên và quyền sở hữu dữ liệu ở tầng backend (`Spring Security`) trên mọi API (S3).
- Email đặt lại mật khẩu gửi qua API HTTPS của Brevo theo [ADR 0013](../../decisions/0013-gui-email-qua-brevo.md) (khi chạy cục bộ không cấu hình khóa API thì ghi log để kiểm thử).

---

## 2. Danh sách Use Case của F1

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F1.1** | Khách tự đăng ký tài khoản hội viên | Khách | Cao | Sprint 1 |
| **UC-F1.2** | Đăng nhập bằng email | Chủ chuỗi, Quản lý, Lễ tân, PT, Hội viên | Cao | Sprint 1 (khóa tạm S4: Sprint 2) |
| **UC-F1.3** | Làm mới phiên đăng nhập (Refresh Token) & Đăng xuất | Mọi người dùng đã đăng nhập | Cao | Sprint 1 (đồng bộ nhiều tab: Sprint 2) |
| **UC-F1.4** | Đổi mật khẩu | Mọi người dùng đã đăng nhập | Trung bình | Sprint 2 |
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
  - Nếu lỗi xảy ra ở bất kỳ bước nào, toàn bộ giao dịch được hoàn tác (rollback), không để lại tài khoản hoặc hồ sơ dở dang.
- **Luồng chính (Basic Flow):**
  1. Khách mở màn hình Đăng ký và nhập: Họ và tên (`fullName`), Email (`email`), Số điện thoại (`phone`), Mật khẩu (`password`), Ngày sinh (`dob` – tùy chọn), Giới tính (`gender` – tùy chọn).
  2. Hệ thống chuẩn hóa `email` về chữ thường (`trim().toLowerCase()`) và chuẩn hóa `phone` về dạng 10 chữ số bắt đầu bằng `0` (ví dụ `+84 912 345 678` → `0912345678`).
  3. Hệ thống kiểm tra định dạng dữ liệu đầu vào (Bean Validation – S5):
     - Email đúng định dạng chuẩn.
     - Số điện thoại khớp biểu thức `^0[0-9]{9}$`.
     - Mật khẩu tối thiểu 8 ký tự, bao gồm cả chữ cái và chữ số, không vượt quá 72 byte UTF-8 (S1).
  4. Hệ thống kiểm tra `email` chưa tồn tại trong bảng `users`.
  5. Hệ thống kiểm tra chi nhánh mặc định (`CN01`) đang `ACTIVE`, gọi dịch vụ hồ sơ hội viên (F2) kiểm tra `phone` chưa tồn tại trong bảng `members` và tạo hồ sơ hội viên mới gắn với chi nhánh mặc định.
  6. Hệ thống băm mật khẩu bằng bcrypt, lưu bản ghi `users` và `members` trong cùng một giao dịch CSDL (`@Transactional`).
  7. Hệ thống tạo JWT Access Token (15 phút) và Refresh Token (lưu mã băm SHA-256 vào `refresh_tokens`, đính kèm cookie `HttpOnly`), trả về thông tin phiên đăng nhập (`201 Created`).
- **Luồng thay thế và ngoại lệ (Alternative / Exception Flows):**
  - **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):** Email sai định dạng, mật khẩu dưới 8 ký tự hoặc thiếu chữ/số hoặc quá 72 byte UTF-8, số điện thoại không hợp lệ → Trả `ProblemDetail` (400) kèm danh sách lỗi theo từng trường (`errors`).
  - **E2 – Email đã được sử dụng (409 Conflict):** `email` đã tồn tại trong `users` (so sánh không phân biệt hoa thường) → Từ chối và trả `ProblemDetail` (409): *"Email này đã được đăng ký tài khoản."*
  - **E3 – Số điện thoại đã thuộc hồ sơ hội viên có sẵn (409 Conflict):** `phone` đã tồn tại trong `members` (do lễ tân lập hồ sơ tại quầy trước đó hoặc hội viên khác đã dùng) → **Không tự động gắn tài khoản mới vào hồ sơ cũ** (tránh lộ dữ liệu cá nhân theo S3). Từ chối và trả `ProblemDetail` (409): *"Số điện thoại này đã có hồ sơ hội viên tại phòng tập. Vui lòng liên hệ quầy lễ tân để được kích hoạt tài khoản."*
  - **E4 – Chi nhánh mặc định ngừng hoạt động hoặc lỗi giữa chừng:** Giao dịch hoàn tác toàn bộ; không lưu bản ghi `users` lẫn `members`.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F1.1.1: Tự đăng ký tài khoản hội viên
- **User story:** Là **Khách**, tôi muốn **tự đăng ký tài khoản bằng email**, để **xem gói và đặt lịch trên web mà không phải ra quầy**.
- **BR liên quan:** BR-F1-01, BR-F1-02, BR-F1-03, BR-F1-04, BR-F1-09; S1, S2, S3, S5
- **Version / Sprint:** v1 / Sprint 1
- **Issue:** #117
- **Tiêu chí chấp nhận:**
  - **AC-F1.1.1-01: Đăng ký thành công tạo đồng thời tài khoản và hồ sơ hội viên** (BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-09)
    - **Given** Chi nhánh mặc định (`CN01`) đang `ACTIVE`; email chưa tồn tại; số điện thoại 10 số hợp lệ chưa có trong hồ sơ nào; mật khẩu tối thiểu 8 ký tự gồm cả chữ cái và chữ số
    - **When** Khách gửi yêu cầu đăng ký (`POST /api/v1/auth/register`)
    - **Then** Hệ thống trả `201 Created`; tạo tài khoản `users` (`role = MEMBER`, `status = ACTIVE`, email chữ thường, mật khẩu băm bcrypt) và hồ sơ `members` (gắn chi nhánh mặc định, mã `HV` + 6 chữ số, SĐT chuẩn hóa 10 số) trong cùng một giao dịch; trả `accessToken` (hạn 15 phút) và đặt cookie `refresh_token` (`HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`)
  - **AC-F1.1.1-02: Từ chối khi số điện thoại đã thuộc hồ sơ hội viên có sẵn** (BR-F1-02)
    - **Given** Số điện thoại `0912345678` đã thuộc một hồ sơ hội viên (do lễ tân tạo tại quầy hoặc hội viên khác đã dùng)
    - **When** Khách gửi đăng ký bằng `0912345678` hoặc `+84 912 345 678`
    - **Then** Hệ thống trả `409 Conflict` với thông báo E3 *"Số điện thoại này đã có hồ sơ hội viên tại phòng tập. Vui lòng liên hệ quầy lễ tân để được kích hoạt tài khoản."*; không tạo tài khoản mới và không gắn tài khoản vào hồ sơ cũ
  - **AC-F1.1.1-03: Từ chối khi email đã được sử dụng** (BR-F1-01)
    - **Given** Email đã thuộc một tài khoản trong `users` (không phân biệt chữ hoa, chữ thường)
    - **When** Khách gửi đăng ký bằng email đó
    - **Then** Hệ thống trả `409 Conflict` với thông báo E2 *"Email này đã được đăng ký tài khoản."*; không tạo tài khoản hay hồ sơ mới
  - **AC-F1.1.1-04: Từ chối dữ liệu đầu vào sai định dạng** (BR-F1-01, BR-F1-03)
    - **Given** Khách nhập email sai định dạng, thiếu trường bắt buộc, mật khẩu dưới 8 ký tự hoặc thiếu chữ/thiếu số hoặc quá 72 byte UTF-8, hoặc số điện thoại sai định dạng
    - **When** Khách gửi đăng ký trên giao diện hoặc gọi API
    - **Then** Hệ thống từ chối với `400 Bad Request` kèm danh sách lỗi theo từng trường (`errors`); không ghi dữ liệu vào CSDL
  - **AC-F1.1.1-05: Hoàn tác toàn bộ giao dịch khi xảy ra lỗi giữa chừng hoặc chi nhánh mặc định ngừng hoạt động** (BR-F1-09)
    - **Given** Lưu tài khoản thành công nhưng bước lưu hồ sơ hội viên gặp lỗi, hoặc chi nhánh mặc định (`CN01`) đang ngừng hoạt động (`INACTIVE`)
    - **When** Khách gửi đăng ký
    - **Then** Giao dịch rollback hoàn toàn; không tồn tại bản ghi `users` lẫn `members` dở dang trong CSDL

---

### 3.2. UC-F1.2: Đăng nhập bằng email

- **Tác nhân:** Chủ chuỗi (`SUPER_ADMIN`), Quản lý chi nhánh (`BRANCH_MANAGER`), Lễ tân (`RECEPTIONIST`), Huấn luyện viên (`TRAINER`), Hội viên (`MEMBER`).
- **Tiền điều kiện:** Người dùng đã có tài khoản trong bảng `users`.
- **Hậu điều kiện:**
  - Cập nhật `last_login_at = now()`, đặt lại `failed_login_count = 0` và `locked_until = null`.
  - Tạo bản ghi `refresh_tokens` mới, đặt cookie `refresh_token` (`HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`) và trả về `accessToken` chứa đúng vai trò của tài khoản.
- **Luồng chính:**
  1. Người dùng nhập `email` và `password` tại màn hình Đăng nhập (`POST /api/v1/auth/login`).
  2. Hệ thống kiểm tra định dạng dữ liệu đầu vào (Bean Validation – email hợp lệ, mật khẩu không trống; không hỗ trợ đăng nhập bằng số điện thoại).
  3. Hệ thống chuẩn hóa `email` về chữ thường và tìm (khóa dòng `SELECT ... FOR UPDATE`) tài khoản trong bảng `users`.
  4. Nếu tài khoản đang trong thời gian khóa tạm (`locked_until > now()`), hệ thống từ chối ngay theo ngoại lệ E2 (trước khi kiểm tra mật khẩu).
  5. Hệ thống đối chiếu `password` với `password_hash` bằng bcrypt (khi email không tồn tại vẫn đối chiếu với mã băm giả để cân bằng thời gian phản hồi). Nếu sai mật khẩu, xử lý đếm lỗi theo ngoại lệ E1.
  6. Sau khi mật khẩu khớp, hệ thống kiểm tra trạng thái tài khoản (`users.status = 'ACTIVE'`; nếu `LOCKED` hoặc `DISABLED` thì trả ngoại lệ E3 để không lộ trạng thái tài khoản cho người không biết mật khẩu).
  7. Hệ thống đặt lại `failed_login_count = 0`, `locked_until = null`, cập nhật `last_login_at = now()`.
  8. Hệ thống sinh JWT Access Token (hết hạn sau 15 phút, chứa `sub = user.id`, `email`, `role`) và Refresh Token ngẫu nhiên (lưu SHA-256 vào `refresh_tokens.token_hash`), trả kết quả `200 OK`.
- **Luồng ngoại lệ:**
  - **E1 – Sai email hoặc mật khẩu (401 Unauthorized):**
    - Nếu email tồn tại: nếu tài khoản từng bị khóa tạm nhưng đã hết hạn (`locked_until <= now()`), đặt lại `failed_login_count = 1` và `locked_until = null` (không khóa lại ngay ở lần sai đầu tiên sau khi hết khóa); ngược lại tăng `failed_login_count` thêm 1. Khi `failed_login_count >= 5`, đặt `locked_until = now() + 15 phút` (S4, NF6).
    - Trả `ProblemDetail` (401) với thông báo chung: *"Email hoặc mật khẩu không chính xác."* (không tiết lộ email có tồn tại hay tài khoản đang bị vô hiệu hóa khi nhập sai mật khẩu).
  - **E2 – Tài khoản bị khóa tạm thời do nhập sai quá 5 lần (403 Forbidden):** Khi `locked_until > now()`, từ chối mọi yêu cầu đăng nhập (kể cả nhập đúng mật khẩu), không tăng thêm `failed_login_count`, trả `ProblemDetail` (403): *"Tài khoản đang bị khóa tạm thời do nhập sai mật khẩu nhiều lần. Vui lòng thử lại sau N phút."* (`N` làm tròn lên theo phút, tối thiểu 1 phút; thống nhất trả `403 Forbidden` thay vì `423 Locked`).
  - **E3 – Tài khoản bị khóa hoặc ngừng hoạt động (403 Forbidden):** Khi mật khẩu đúng nhưng `status` là `LOCKED` hoặc `DISABLED`, trả `ProblemDetail` (403): *"Tài khoản đã bị khóa hoặc ngừng hoạt động. Vui lòng liên hệ quản trị viên."*
  - **E4 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):** Email sai định dạng (bao gồm nhập số điện thoại) hoặc bỏ trống trường bắt buộc → Trả `ProblemDetail` (400) kèm danh sách lỗi theo trường (`errors`).

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F1.2.1: Đăng nhập bằng email
- **User story:** Là **người dùng có tài khoản**, tôi muốn **đăng nhập bằng email và mật khẩu**, để **dùng đúng các chức năng của vai trò mình** và **bảo vệ tài khoản khỏi dò mật khẩu**.
- **BR liên quan:** BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06; S1, S2, S4, S5, NF6
- **Version / Sprint:** v1 / Sprint 1 (đăng nhập cơ bản), Sprint 2 (khóa tạm S4)
- **Issue:** #118
- **Tiêu chí chấp nhận:**
  - **AC-F1.2.1-01: Nhập sai mật khẩu từ lần 1 đến lần 4 trả 401 thông báo chung, tăng bộ đếm và không lộ email tồn tại** (BR-F1-01, BR-F1-03, BR-F1-06)
    - **Given** Email không tồn tại; hoặc tài khoản `ACTIVE` nhập sai mật khẩu lần 1 đến lần 4 (kể cả mật khẩu vượt quá 72 byte UTF-8); hoặc tài khoản `DISABLED`/`LOCKED` nhưng nhập sai mật khẩu
    - **When** Người dùng gửi `POST /api/v1/auth/login`
    - **Then** Hệ thống trả `401 Unauthorized` với cùng một thông báo chung *"Email hoặc mật khẩu không chính xác."* (không lộ email có tồn tại hay trạng thái `DISABLED`/`LOCKED`); nếu email tồn tại thì tăng `users.failed_login_count` lên tương ứng `1..4` và giữ `users.locked_until = null`
  - **AC-F1.2.1-02: Nhập sai mật khẩu lần thứ 5 liên tiếp (kể cả gửi đồng thời) đặt bộ đếm bằng 5 và khóa tạm 15 phút** (BR-F1-06)
    - **Given** Tài khoản đã có 4 lần sai liên tiếp và gửi thêm 1 lần mật khẩu sai; hoặc nhiều yêu cầu đăng nhập sai gửi đồng thời vào cùng một email
    - **When** Hệ thống khóa dòng tài khoản (`SELECT ... FOR UPDATE`) và ghi nhận lượt sai
    - **Then** Đếm đủ từng lần, không mất lượt nào; đến lần sai thứ 5, trả `401 Unauthorized` câu chung, lưu `failed_login_count = 5` và `locked_until = now + 15 phút`
  - **AC-F1.2.1-03: Từ chối mọi yêu cầu đăng nhập trong thời gian đang bị khóa tạm 15 phút, kể cả nhập đúng mật khẩu** (BR-F1-06)
    - **Given** Tài khoản đang có `users.locked_until > now`
    - **When** Người dùng gửi `POST /api/v1/auth/login` với mật khẩu đúng hoặc sai
    - **Then** Hệ thống từ chối ngay với `403 Forbidden` kèm thời gian còn lại (*"Tài khoản đang bị khóa tạm thời do nhập sai mật khẩu nhiều lần. Vui lòng thử lại sau N phút."*), không tăng thêm `failed_login_count` và không cấp token
  - **AC-F1.2.1-04: Đăng nhập thành công hoặc hết thời gian khóa tạm** (BR-F1-01, BR-F1-04, BR-F1-06)
    - **Given** Tài khoản `ACTIVE` (kể cả trước đó đã sai vài lần hoặc vừa hết 15 phút khóa tạm `locked_until <= now`)
    - **When** Người dùng đăng nhập bằng đúng email (không phân biệt hoa thường) và đúng mật khẩu; hoặc đăng nhập sai sau khi đã hết hạn khóa tạm
    - **Then** Nếu đúng mật khẩu: trả `200 OK`, cấp `accessToken` chứa đúng `role` và cookie `refresh_token`, đặt lại `failed_login_count = 0`, xóa `locked_until = null`, cập nhật `last_login_at`. Nếu đã hết hạn khóa tạm mà nhập sai mật khẩu: trả `401`, bộ đếm **tính lại từ 1** và xóa `locked_until = null` (không khóa lại ngay)
  - **AC-F1.2.1-05: Từ chối tài khoản bị khóa hoặc ngừng hoạt động khi nhập đúng mật khẩu** (BR-F1-05)
    - **Given** Tài khoản có trạng thái `LOCKED` hoặc `DISABLED`
    - **When** Người dùng gửi `POST /api/v1/auth/login` với đúng mật khẩu
    - **Then** Hệ thống từ chối với `403 Forbidden` thông báo E3 *"Tài khoản đã bị khóa hoặc ngừng hoạt động. Vui lòng liên hệ quản trị viên."* và không cấp token
  - **AC-F1.2.1-06: Từ chối dữ liệu đăng nhập sai định dạng (không hỗ trợ đăng nhập bằng số điện thoại)** (BR-F1-01)
    - **Given** Người dùng bỏ trống email/mật khẩu hoặc nhập số điện thoại thay cho email
    - **When** Gửi yêu cầu đăng nhập
    - **Then** Hệ thống từ chối với `400 Bad Request` kèm danh sách lỗi theo từng trường (`errors`)

---

### 3.3. UC-F1.3: Làm mới phiên đăng nhập (Refresh Token) & Đăng xuất

- **Tác nhân:** Người dùng đã đăng nhập.
- **Tiền điều kiện:** Trình duyệt có cookie `refresh_token` hợp lệ gửi tới `/api/v1/auth/refresh` hoặc `/api/v1/auth/logout`.
- **Luồng chính – Làm mới token (`POST /api/v1/auth/refresh`):**
  1. Trình duyệt gửi request kèm cookie `refresh_token`.
  2. Hệ thống băm giá trị token bằng SHA-256 và tra cứu (có khóa dòng) trong bảng `refresh_tokens`.
  3. Kiểm tra bản ghi tồn tại, `revoked_at IS NULL`, `expires_at > now()`, và tài khoản `users.status = 'ACTIVE'`. Nếu phát hiện dùng lại token đã bị thu hồi (`revoked_at IS NOT NULL`), coi là nguy cơ đánh cắp và thu hồi toàn bộ refresh token đang hiệu lực của tài khoản đó, trả `401`.
  4. **Xoay vòng token (Token Rotation):** Đánh dấu `revoked_at = now()` cho bản ghi `refresh_tokens` cũ, tạo một Refresh Token mới (lưu bản ghi mới vào `refresh_tokens`, cập nhật cookie `refresh_token`) và cấp Access Token (JWT) mới (`200 OK`).
- **Luồng chính – Đăng xuất (`POST /api/v1/auth/logout`):**
  1. Trình duyệt gửi request đăng xuất (gửi kèm header `Authorization: Bearer` nếu tab đang giữ access token, kể cả khi access token đã hết hạn, và **không** tự gọi làm mới phiên trước khi đăng xuất).
  2. Nếu có cookie `refresh_token` hợp lệ (và khớp chủ sở hữu với JWT nếu có gửi kèm), hệ thống tìm bản ghi tương ứng trong `refresh_tokens` và cập nhật `revoked_at = now()`.
  3. Hệ thống trả về header `Set-Cookie` với `Max-Age=0` để xóa cookie `refresh_token` trên trình duyệt (khi cùng chủ sở hữu), trả `204 No Content`. Frontend xóa access token trong bộ nhớ.
- **Luồng bổ sung – Đồng bộ phiên giữa nhiều tab (giao diện, Sprint 2 – S2-55):**
  - Khi người dùng mở nhiều tab cùng lúc, frontend dùng khóa dùng chung giữa các tab để chỉ một tab gửi `POST /api/v1/auth/refresh` tại một thời điểm và chia sẻ `accessToken` mới cho các tab khác đang chờ; khi một tab đăng xuất thì các tab khác cùng tài khoản xóa phiên và về trang đăng nhập kèm thông báo (tab đang dùng tài khoản khác không bị đăng xuất); khi một tab đăng nhập tài khoản khác thì tab còn lại về trang đăng nhập.
- **Luồng ngoại lệ:**
  - **E1 – Thiếu cookie, token hết hạn hoặc đã thu hồi (401 Unauthorized):** Xóa cookie `refresh_token` và trả `ProblemDetail` (401): *"Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại."* Frontend xóa phiên cục bộ và không tạo vòng lặp tự làm mới.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F1.3.1: Làm mới phiên và đăng xuất
- **User story:** Là **người dùng đã đăng nhập**, tôi muốn **phiên tự làm mới và đăng xuất được**, để **không phải đăng nhập lại liên tục và an toàn khi dùng máy chung**.
- **BR liên quan:** BR-F1-04; S2, NF6
- **Version / Sprint:** v1 / Sprint 1 (API cơ bản & khóa đồng thời), Sprint 2 (đồng bộ nhiều tab)
- **Issue:** #119
- **Tiêu chí chấp nhận:**
  - **AC-F1.3.1-01: Xoay vòng refresh token và chặn dùng lại token cũ** (BR-F1-04)
    - **Given** Người dùng đã đăng nhập và có cookie `refresh_token` hợp lệ, chưa hết hạn, chưa bị thu hồi
    - **When** Gửi `POST /api/v1/auth/refresh`; sau đó gửi lại chính `refresh_token` cũ một lần nữa (hoặc gửi đồng thời hai yêu cầu bằng cùng một token)
    - **Then** Lần làm mới hợp lệ trả `200 OK`, đánh dấu `revoked_at` cho token cũ và cấp cặp `accessToken` + `refresh_token` mới; dùng lại token đã thu hồi bị từ chối `401 Unauthorized` và thu hồi toàn bộ phiên đang hiệu lực của tài khoản
  - **AC-F1.3.1-02: Đăng xuất thu hồi refresh token và xóa cookie** (BR-F1-04)
    - **Given** Người dùng đã đăng nhập (kể cả khi `accessToken` đã hết hạn)
    - **When** Gửi `POST /api/v1/auth/logout`
    - **Then** Hệ thống trả `204 No Content`, thu hồi `refresh_token` tương ứng và gửi header xóa cookie; frontend gửi `Bearer` cả khi token hết hạn, **không** tự làm mới phiên trước khi đăng xuất, và xóa `accessToken` khỏi bộ nhớ
  - **AC-F1.3.1-03: Từ chối khi phiên không hợp lệ và xóa cookie** (BR-F1-04)
    - **Given** Thiếu cookie `refresh_token`, hoặc token đã hết hạn, hoặc token đã thu hồi, hoặc tài khoản không còn `ACTIVE`
    - **When** Gửi `POST /api/v1/auth/refresh`
    - **Then** Hệ thống trả `401 Unauthorized` với thông báo E1 *"Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại."*, xóa cookie `refresh_token`; giao diện chuyển về trang đăng nhập mà không tạo vòng lặp làm mới
  - **AC-F1.3.1-04: Đồng bộ làm mới phiên và đăng xuất giữa nhiều tab trình duyệt** (BR-F1-04)
    - **Given** Người dùng mở nhiều tab trên cùng trình duyệt
    - **When** Hai tab cùng tải lại hoặc cùng gặp `401`; hoặc một tab đăng xuất; hoặc một tab đăng nhập tài khoản khác
    - **Then** Chỉ gửi **một** yêu cầu `POST /api/v1/auth/refresh`, các tab cùng tài khoản dùng chung `accessToken` mới; đăng xuất ở tab 1 thì tab cùng tài khoản về trang đăng nhập kèm thông báo, còn tab của **tài khoản khác** không bị đăng xuất; tab khác đăng nhập tài khoản khác thì tab hiện tại về trang đăng nhập

---

### 3.4. UC-F1.4: Đổi mật khẩu

- **Tác nhân:** Người dùng đang đăng nhập (mọi vai trò: `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST`, `TRAINER`, `MEMBER`).
- **Tiền điều kiện:** Người dùng đã xác thực bằng JWT hợp lệ.
- **Hậu điều kiện:**
  - **Thành công:** Cập nhật `users.password_hash` bằng bcrypt, thu hồi toàn bộ `refresh_tokens` đang hiệu lực của tài khoản trong cùng giao dịch; giao diện xóa phiên trình duyệt và chuyển về `/login` kèm thông báo.
  - **Thất bại:** Giữ nguyên `password_hash` và các phiên hiện tại.
- **Luồng chính (`POST /api/v1/auth/change-password`):**
  1. Người dùng gửi mật khẩu hiện tại (`currentPassword`) và mật khẩu mới (`newPassword`).
  2. Hệ thống kiểm tra định dạng dữ liệu đầu vào bằng Bean Validation (`currentPassword` không được để trống; `newPassword` tối thiểu 8 ký tự, bao gồm cả chữ cái và chữ số, tối đa 72 byte UTF-8 theo S1).
  3. Hệ thống kiểm tra quy tắc nghiệp vụ: `currentPassword` phải khớp với `users.password_hash` và `newPassword` phải khác `currentPassword`.
  4. Hệ thống cập nhật `password_hash` mới và **thu hồi toàn bộ các phiên đăng nhập hiện có** của người dùng (`UPDATE refresh_tokens SET revoked_at = now() WHERE user_id = ? AND revoked_at IS NULL`).
  5. Trả `204 No Content`.
- **Luồng ngoại lệ:**
  - **E1 – Dữ liệu đầu vào sai định dạng (400 Bad Request):** Để trống trường bắt buộc hoặc `newPassword` dưới 8 ký tự / thiếu chữ cái hoặc chữ số / vượt quá 72 byte UTF-8 (Bean Validation) → Trả `ProblemDetail` (400) kèm danh sách lỗi theo từng trường (`errors`).
  - **E2 – Vi phạm quy tắc nghiệp vụ (422 Unprocessable Content):** Mật khẩu hiện tại (`currentPassword`) không đúng (*"Mật khẩu hiện tại không đúng."*), hoặc mật khẩu mới (`newPassword`) trùng với mật khẩu hiện tại (*"Mật khẩu mới phải khác mật khẩu hiện tại."*) → Trả `ProblemDetail` (422) thông báo chi tiết lý do, không xóa phiên.
  - **E3 – Chưa đăng nhập (401 Unauthorized):** Không gửi JWT hợp lệ → Trả `ProblemDetail` (401).
  - **E4 – Tài khoản bị khóa hoặc ngừng hoạt động (403 Forbidden):** Tài khoản không còn ở trạng thái `ACTIVE` (`LOCKED` / `DISABLED`) dù Access Token chưa hết hạn → Trả `ProblemDetail` (403): *"Tài khoản đã bị khóa hoặc ngừng hoạt động. Vui lòng liên hệ quản trị viên."*

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F1.4.1: Đổi mật khẩu
- **User story:** Là **người dùng đã đăng nhập**, tôi muốn **đổi mật khẩu**, để **giữ tài khoản an toàn**.
- **BR liên quan:** BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-07; S1, S2, S3
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #120
- **Tiêu chí chấp nhận:**
  - **AC-F1.4.1-01: Đổi mật khẩu thành công, thu hồi toàn bộ phiên và yêu cầu đăng nhập lại** (BR-F1-03, BR-F1-04)
    - **Given** Người dùng đã đăng nhập (bất kỳ vai trò nào trong 5 vai trò) và có một hoặc nhiều phiên `refresh_tokens` đang hiệu lực
    - **When** Gửi `POST /api/v1/auth/change-password` (hoặc gửi biểu mẫu tại `/doi-mat-khau`) với `currentPassword` đúng và `newPassword` hợp lệ, khác mật khẩu hiện tại
    - **Then** Hệ thống trả `204 No Content`, cập nhật `users.password_hash` bằng bcrypt, đánh dấu `revoked_at` cho toàn bộ `refresh_tokens` đang hiệu lực của tài khoản đó (không ảnh hưởng tài khoản khác); mật khẩu mới đăng nhập được, mật khẩu cũ và các refresh token cũ bị từ chối `401`; giao diện xóa phiên cục bộ, báo cho các tab khác qua kênh đồng bộ phiên (`publishSessionChange(null)`) và chuyển về `/login` kèm thông báo *"Đã đổi mật khẩu, vui lòng đăng nhập lại"*
  - **AC-F1.4.1-02: Từ chối khi mật khẩu hiện tại sai hoặc mật khẩu mới trùng mật khẩu cũ** (BR-F1-07)
    - **Given** Người dùng đã đăng nhập
    - **When** Gửi `POST /api/v1/auth/change-password` với `currentPassword` không khớp (kể cả vượt quá 72 byte UTF-8) hoặc `newPassword` trùng `currentPassword`
    - **Then** Hệ thống trả `422 Unprocessable Content` kèm thông báo tiếng Việt (*"Mật khẩu hiện tại không đúng."* hoặc *"Mật khẩu mới phải khác mật khẩu hiện tại."*), không thay đổi `password_hash` và không thu hồi `refresh_tokens`; giao diện hiển thị thông báo lỗi và giữ nguyên phiên đăng nhập hiện tại
  - **AC-F1.4.1-03: Từ chối khi dữ liệu đầu vào sai định dạng hoặc mật khẩu mới không đạt chuẩn bảo mật** (BR-F1-03)
    - **Given** Người dùng đã đăng nhập mở màn hình `/doi-mat-khau` hoặc gọi API
    - **When** Bỏ trống trường bắt buộc, nhập `newPassword` dưới 8 ký tự, thiếu chữ cái hoặc chữ số, vượt quá 72 byte UTF-8, hoặc ô nhập lại mật khẩu mới không khớp
    - **Then** Trình duyệt chặn gửi và hiển thị lỗi tại ô nhập; nếu gọi thẳng API với dữ liệu không hợp lệ thì backend trả `400 Bad Request` kèm danh sách lỗi theo từng trường (`errors.currentPassword`, `errors.newPassword`) và giao diện gắn lỗi vào đúng trường
  - **AC-F1.4.1-04: Chặn truy cập khi chưa đăng nhập và mở đúng khung giao diện cho mọi vai trò đã đăng nhập** (BR-F1-05)
    - **Given** Người dùng chưa đăng nhập hoặc đã đăng nhập với bất kỳ vai trò nào (`SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST`, `TRAINER`, `MEMBER`)
    - **When** Gọi `POST /api/v1/auth/change-password` không kèm JWT hợp lệ, hoặc bấm **Đổi mật khẩu** để mở `/doi-mat-khau`
    - **Then** Request không có JWT hợp lệ bị chặn với `401 Unauthorized`; người dùng đã đăng nhập mở được `/doi-mat-khau` trong đúng khung giao diện của vai trò mình mà không bị chuyển sang `/403`

---

### 3.5. UC-F1.5: Quên mật khẩu và đặt lại mật khẩu (Sprint 2)

- **Tác nhân:** Người dùng quên mật khẩu, hoặc Hội viên có hồ sơ tại quầy vừa được lễ tân tạo tài khoản (UC-F2.2).
- **Tiền điều kiện:** Không yêu cầu đăng nhập (hai endpoint công khai).
- **Hậu điều kiện:**
  - **Yêu cầu quên mật khẩu:** Nếu email tồn tại và tài khoản `ACTIVE`, tạo một bản ghi `password_resets` chứa mã băm SHA-256 của token (hạn **30 phút**) và gửi email chứa liên kết đặt lại mật khẩu qua Brevo ([ADR 0013](../../decisions/0013-gui-email-qua-brevo.md)).
  - **Đặt lại mật khẩu thành công:** Cập nhật `users.password_hash`, xóa khóa đăng nhập tạm (`failed_login_count = 0`, `locked_until = null`), đánh dấu `password_resets.used_at = now()`, và thu hồi toàn bộ `refresh_tokens` đang hiệu lực của tài khoản trong cùng giao dịch.
- **Luồng chính:**
  1. Người dùng mở trang "Quên mật khẩu?" (`/quen-mat-khau`) và nhập `email` yêu cầu đặt lại mật khẩu (`POST /api/v1/auth/forgot-password`).
  2. Hệ thống kiểm tra định dạng email (Bean Validation) và chuẩn hóa về chữ thường. Nếu `email` tồn tại và tài khoản đang `ACTIVE`, hệ thống sinh token ngẫu nhiên, lưu mã băm SHA-256 vào bảng `password_resets` (`token_hash`, `expires_at = now + 30 phút`), gửi đường dẫn `${FRONTEND_BASE_URL}/dat-lai-mat-khau?token=...` qua API HTTPS của Brevo (ADR 0013). Trả `200 OK` cùng câu thông báo chung: *"Nếu email có trong hệ thống, bạn sẽ nhận được liên kết đặt lại mật khẩu."*
  3. Người dùng mở liên kết (hoặc liên kết kích hoạt hạn **72 giờ** từ UC-F2.2) tại trang `/dat-lai-mat-khau?token=...`, nhập mật khẩu mới đạt chuẩn S1 và gửi `POST /api/v1/auth/reset-password`.
  4. Hệ thống khóa dòng `password_resets` và `users` (`SELECT ... FOR UPDATE`), kiểm tra `used_at IS NULL`, `expires_at > now()` và `users.status = 'ACTIVE'`, cập nhật `users.password_hash`, xóa khóa đăng nhập tạm (`failed_login_count = 0`, `locked_until = null`), đánh dấu `password_resets.used_at = now()`, thu hồi mọi `refresh_tokens` của tài khoản, và trả `204 No Content`. Giao diện chuyển về `/login` kèm thông báo thành công.
- **Luồng thay thế và ngoại lệ (Alternative / Exception Flows):**
  - **A1 – Email không tồn tại hoặc tài khoản không `ACTIVE` (200 OK):** Tại bước 2, nếu email chưa có trong `users` hoặc tài khoản đang `DISABLED`/`LOCKED`, hệ thống không tạo bản ghi `password_resets` và không gửi email, vẫn trả `200 OK` cùng câu thông báo chung để không lộ email.
  - **A2 – Dịch vụ gửi email gặp lỗi (200 OK):** Tại bước 2, nếu dịch vụ gửi email (Brevo) báo lỗi, hệ thống ghi log mức `ERROR` **không chứa token thô**, vẫn trả `200 OK` câu thông báo chung.
  - **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):** Email sai định dạng, thiếu token, mật khẩu mới dưới 8 ký tự / thiếu chữ cái hoặc chữ số / quá 72 byte UTF-8, hoặc hai mật khẩu không khớp (trên giao diện) → Trả `ProblemDetail` (400) kèm danh sách lỗi theo trường (`errors`); giao diện chặn sớm, không gọi API.
  - **E2 – Token không tồn tại, đã hết hạn, đã dùng hoặc tài khoản không `ACTIVE` (422 Unprocessable Content):** Tại bước 4, token sai, quá hạn (`expires_at <= now()`), đã sử dụng (`used_at IS NOT NULL`, kể cả yêu cầu gửi đồng thời), hoặc tài khoản không còn `ACTIVE` → Từ chối và trả `ProblemDetail` (422): *"Liên kết đã hết hạn hoặc đã được dùng."* Giao diện hiển thị thông báo kèm nút **Gửi lại liên kết** dẫn về `/quen-mat-khau`.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F1.5.1: Quên và đặt lại mật khẩu
- **User story:** Là **người dùng quên mật khẩu**, tôi muốn **nhận email đặt lại mật khẩu**, để **lấy lại quyền truy cập mà không cần nhờ quầy**.
- **BR liên quan:** BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08; S1, S2, S4, S5, S6
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #121
- **Tiêu chí chấp nhận:**
  - **AC-F1.5.1-01: Yêu cầu quên mật khẩu cho tài khoản ACTIVE tạo token băm SHA-256 hạn 30 phút, gửi email và trả 200 câu chung** (BR-F1-01, BR-F1-08)
    - **Given** Tài khoản đang `ACTIVE`; người dùng mở `/quen-mat-khau` (từ liên kết **Quên mật khẩu?** trên `/login`) hoặc gọi công khai `POST /api/v1/auth/forgot-password` (kể cả email viết hoa hoặc có khoảng trắng đầu/cuối)
    - **When** Gửi yêu cầu quên mật khẩu
    - **Then** Hệ thống chuẩn hóa email về chữ thường, lưu mã băm SHA-256 vào `password_resets` với hạn **30 phút**, gửi email chứa liên kết đặt lại mật khẩu và trả `200 OK` với câu thông báo chung *"Nếu email có trong hệ thống, bạn sẽ nhận được liên kết đặt lại mật khẩu."*
  - **AC-F1.5.1-02: Không lộ email khi email không tồn tại, tài khoản không ACTIVE hoặc gửi email gặp lỗi** (BR-F1-08)
    - **Given** Email chưa tồn tại trong `users`; hoặc tài khoản đang `DISABLED`/`LOCKED`; hoặc dịch vụ gửi email Brevo gặp lỗi
    - **When** Gửi `POST /api/v1/auth/forgot-password`
    - **Then** Không tạo bản ghi `password_resets` và không gửi email nếu email không tồn tại hoặc tài khoản không `ACTIVE`; nếu gửi email gặp lỗi thì bắt lỗi và ghi log mức `ERROR` **không chứa token**; mọi trường hợp vẫn trả `200 OK` cùng câu thông báo chung
  - **AC-F1.5.1-03: Đặt lại mật khẩu với token hợp lệ cập nhật mật khẩu, ghi used_at, mở khóa tạm và thu hồi toàn bộ refresh token** (BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08)
    - **Given** Tài khoản `ACTIVE` có token đặt lại mật khẩu còn hạn, chưa dùng (kể cả đang bị khóa tạm 15 phút và có nhiều phiên đang hiệu lực)
    - **When** Người dùng mở trang công khai `/dat-lai-mat-khau?token=...` hoặc gọi `POST /api/v1/auth/reset-password` với token hợp lệ và mật khẩu mới đúng chuẩn S1
    - **Then** Hệ thống trả `204 No Content`, cập nhật mật khẩu mới, ghi `used_at`, **mở khóa tạm** (`failed_login_count = 0`, `locked_until = null`) và **thu hồi toàn bộ `refresh_tokens`**; mật khẩu mới đăng nhập được (`200`), mật khẩu cũ và token cũ bị từ chối (`401`); giao diện chuyển về `/login` kèm thông báo thành công
  - **AC-F1.5.1-04: Từ chối token sai, hết hạn sau 30 phút, đã dùng (kể cả gửi đồng thời) hoặc tài khoản không ACTIVE với 422** (BR-F1-08)
    - **Given** Token không tồn tại, quá hạn 30 phút (`expires_at <= now`), đã dùng (`used_at IS NOT NULL`), tài khoản không `ACTIVE`, hoặc hai yêu cầu gửi đồng thời cùng một token
    - **When** Gửi `POST /api/v1/auth/reset-password`
    - **Then** Hệ thống từ chối với `422 Unprocessable Content` thông báo *"Liên kết đã hết hạn hoặc đã được dùng."* (khi gửi đồng thời chỉ đúng 1 yêu cầu `204`, yêu cầu còn lại `422`), không đổi mật khẩu và không thu hồi phiên; giao diện hiển thị đúng thông báo kèm nút **Gửi lại liên kết** dẫn về `/quen-mat-khau`
  - **AC-F1.5.1-05: Từ chối dữ liệu đầu vào sai định dạng, thiếu token trên URL hoặc mật khẩu mới không đạt chuẩn S1** (BR-F1-01, BR-F1-03)
    - **Given** Người dùng nhập email sai định dạng, mở `/dat-lai-mat-khau` thiếu/rỗng `token`, nhập mật khẩu mới sai chuẩn S1 (dưới 8 ký tự, thiếu chữ/số hoặc quá 72 byte UTF-8), hoặc hai mật khẩu nhập không khớp
    - **When** Gửi yêu cầu ở giao diện hoặc gọi thẳng API
    - **Then** Giao diện chặn gửi (hoặc báo thiếu token kèm nút **Gửi lại liên kết**) mà không gọi API; gọi API trực tiếp trả `400 Bad Request` kèm danh sách lỗi theo trường (`errors`) và giao diện gắn lỗi đúng ô nhập
  - **AC-F1.5.1-06: Gửi email qua API HTTPS của Brevo khi có cấu hình khóa API và chỉ ghi log ở môi trường phát triển khi khóa rỗng** (BR-F1-08)
    - **Given** Ứng dụng chạy với cấu hình có hoặc không có khóa `BREVO_API_KEY` (theo ADR 0013)
    - **When** Hệ thống chọn kênh gửi email và thực hiện gửi email đặt lại mật khẩu
    - **Then** Khi có khóa API hợp lệ, hệ thống gửi qua API HTTPS của Brevo (`POST /v3/smtp/email`); khi khóa rỗng ở môi trường cục bộ/kiểm thử thì ghi log thay vì gửi thật; ở môi trường `staging` không để lộ liên kết ra log khi thiếu khóa

---

### 3.6. UC-F1.6: Quản lý trạng thái tài khoản & Phân quyền truy cập API

- **Tác nhân:** Chủ chuỗi (`SUPER_ADMIN`), Quản lý chi nhánh (`BRANCH_MANAGER`), Hệ thống (áp dụng cho mọi người dùng gọi API `/api/v1/**`).
- **Luồng chính:**
  1. Bộ lọc xác thực JWT đọc token từ header `Authorization: Bearer <token>`, xác thực chữ ký HS256 bằng `JWT_SECRET`, kiểm tra hạn `exp`, cấu trúc `sub` (mã người dùng kiểu số) và claim `role`.
  2. Hệ thống ánh xạ claim `role` sang quyền tương ứng (`ROLE_SUPER_ADMIN`, `ROLE_BRANCH_MANAGER`, `ROLE_RECEPTIONIST`, `ROLE_TRAINER`, `ROLE_MEMBER`) và xác định người gọi từ token (không tin vai trò hay mã người dùng từ client gửi trong body).
  3. Tầng controller và service kiểm tra vai trò, kiểm tra phân công chi nhánh đang hoạt động đối với nhân viên (không tự gán chi nhánh mặc định), kiểm tra trạng thái tài khoản (`ACTIVE`) và kiểm tra quyền sở hữu dữ liệu (ví dụ `MEMBER` chỉ xem được dữ liệu của chính mình).
- **Luồng ngoại lệ:**
  - **E1 – Chưa đăng nhập / JWT không hợp lệ (401 Unauthorized):** Không gửi token, sai chữ ký, hết hạn, thiếu vai trò hoặc `sub` không hợp lệ → Trả `401 Unauthorized` dạng `ProblemDetail` tiếng Việt (không bao giờ trả `500`).
  - **E2 – Không đủ quyền theo vai trò hoặc chưa gắn chi nhánh (403 Forbidden):** Đã đăng nhập nhưng không đủ vai trò (ví dụ Huấn luyện viên gọi API nhân viên quầy, Hội viên gọi API quản lý), hoặc nhân viên chưa được phân chi nhánh / nhân viên `INACTIVE` / chi nhánh `INACTIVE` → Trả `403 Forbidden` dạng `ProblemDetail` tiếng Việt.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F1.6.1: Trạng thái tài khoản và phân quyền
- **User story:** Là **Quản lý chi nhánh**, tôi muốn **hệ thống chặn API theo vai trò, chi nhánh và trạng thái tài khoản**, để **mỗi người chỉ thấy và làm được phần việc của mình**.
- **BR liên quan:** BR-F1-04, BR-F1-05; S2, S3
- **Version / Sprint:** v1 / Sprint 1
- **Issue:** #122
- **Tiêu chí chấp nhận:**
  - **AC-F1.6.1-01: Chưa đăng nhập hoặc token không hợp lệ bị từ chối 401 tiếng Việt** (BR-F1-04, BR-F1-05)
    - **Given** Yêu cầu không gửi token; hoặc gửi JWT sai chữ ký, đã hết hạn, thiếu vai trò, hoặc mã người dùng không phải số
    - **When** Gọi API nghiệp vụ dưới `/api/v1/**` (trừ endpoint công khai)
    - **Then** Hệ thống trả `401 Unauthorized` dạng `ProblemDetail` tiếng Việt (không bao giờ trả `500`)
  - **AC-F1.6.1-02: Chặn API khi sai vai trò và cho phép khi đúng vai trò** (BR-F1-05)
    - **Given** Người dùng đã đăng nhập hợp lệ
    - **When** Huấn luyện viên gọi API dành cho nhân viên quầy, hoặc Hội viên gọi API quản lý; so với người dùng gọi API đúng vai trò của mình
    - **Then** Sai vai trò trả `403 Forbidden` dạng `ProblemDetail` tiếng Việt; đúng vai trò được phép xử lý (`200 OK`)
  - **AC-F1.6.1-03: Nhân viên chi nhánh phải gắn với chi nhánh đang hoạt động** (BR-F1-05)
    - **Given** Tài khoản Lễ tân hoặc Quản lý chi nhánh chưa được phân chi nhánh, hoặc hồ sơ nhân viên `INACTIVE`, hoặc chi nhánh được phân đang `INACTIVE`
    - **When** Gọi API nghiệp vụ yêu cầu phạm vi chi nhánh
    - **Then** Hệ thống từ chối (`403 Forbidden`), không tự gán chi nhánh mặc định
  - **AC-F1.6.1-04: Tài khoản ngừng hoạt động mất quyền và bị thu hồi phiên ngay** (BR-F1-04, BR-F1-05)
    - **Given** Tài khoản bị chuyển sang trạng thái `DISABLED` (ví dụ khi ngừng hoạt động hồ sơ hội viên tại UC-F2.4, xem chi tiết `AC-F2.4.1-03`)
    - **When** Tài khoản đó cố đăng nhập lại (với đúng mật khẩu) hoặc dùng refresh token cũ để làm mới phiên
    - **Then** Đăng nhập bị từ chối `403 Forbidden` (`AC-F1.2.1-05`) và làm mới phiên bị từ chối `401 Unauthorized` (`AC-F1.3.1-03`)

---

## 4. Quy tắc nghiệp vụ và bảo mật áp dụng

Chi tiết đối chiếu với [Bản chốt phạm vi](../ban-chot-pham-vi.md):
- **BR-F1-01 – Chỉ đăng nhập bằng email:** Email luôn chuyển về chữ thường trước khi lưu và tìm kiếm (`CHECK email = lower(email)`); không hỗ trợ đăng nhập bằng số điện thoại hay mạng xã hội.
- **BR-F1-02 – Chống chiếm đoạt hồ sơ qua số điện thoại (S3):** Khi khách tự đăng ký mà số điện thoại đã thuộc một hồ sơ hội viên có sẵn, hệ thống **bắt buộc từ chối (409)** và hướng dẫn liên hệ quầy; tuyệt đối không tự gắn tài khoản mới vào hồ sơ cũ.
- **BR-F1-03 – Mật khẩu (S1):** Băm bằng bcrypt (`BCryptPasswordEncoder`), tối thiểu 8 ký tự gồm cả chữ cái và chữ số, không vượt quá 72 byte UTF-8.
- **BR-F1-04 – Phiên đăng nhập (S2, ADR 0008):** Access Token (JWT) hết hạn sau **15 phút**; Refresh Token lưu mã băm SHA-256 trong bảng `refresh_tokens`, truyền qua cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`, xoay vòng mỗi lần làm mới và thu hồi toàn bộ phiên nếu phát hiện dùng lại token đã thu hồi. Khi đổi mật khẩu hoặc đặt lại mật khẩu, toàn bộ refresh token đang hiệu lực của tài khoản bị thu hồi ngay trong cùng giao dịch.
- **BR-F1-05 – Phân quyền ở Backend (S3):** Mọi API nghiệp vụ dưới `/api/v1/**` (trừ các endpoint đăng ký/đăng nhập/làm mới/quên mật khẩu/công khai) đều kiểm tra JWT, vai trò, chi nhánh nhân viên, trạng thái tài khoản và quyền sở hữu dữ liệu ở backend.
- **BR-F1-06 – Khóa đăng nhập tạm thời (S4, NF6):** Khóa 15 phút (`locked_until`) sau 5 lần nhập sai mật khẩu liên tiếp (`failed_login_count`), đếm an toàn khi có nhiều yêu cầu đồng thời; không đồng nhất khóa tạm (`locked_until`) với trạng thái `LOCKED`/`DISABLED` của tài khoản.
- **BR-F1-07 – Đối chiếu khi đổi mật khẩu (UC-F1.4):** Mật khẩu hiện tại phải khớp `users.password_hash` và mật khẩu mới phải khác mật khẩu hiện tại (vi phạm trả `422 Unprocessable Content`).
- **BR-F1-08 – Liên kết đặt lại mật khẩu dùng một lần và không lộ email (UC-F1.5, ADR 0013):** Token đặt lại mật khẩu chỉ lưu mã băm SHA-256 trong `password_resets` với hạn **30 phút** (quên mật khẩu) hoặc **72 giờ** (tạo tài khoản lần đầu cho hồ sơ có sẵn ở UC-F2.2), chỉ dùng một lần (`used_at`), gửi qua API HTTPS của Brevo khi có `BREVO_API_KEY`. Yêu cầu quên mật khẩu luôn trả `200 OK` cùng câu thông báo chung dù email không tồn tại, tài khoản không `ACTIVE` hay gửi email gặp lỗi, và tuyệt đối không ghi token ra log lỗi.
- **BR-F1-09 – Đăng ký nguyên tử (UC-F1.1):** Khách tự đăng ký tạo đồng thời bản ghi `users` (`role = MEMBER`) và `members` (gắn chi nhánh mặc định `CN01` đang `ACTIVE`) trong cùng một giao dịch; lỗi ở bất kỳ bước nào đều rollback hoàn toàn.

---

## 5. Bảng dữ liệu liên quan

| Bảng | Nhóm | Vai trò trong F1 |
| --- | --- | --- |
| `users` | Nhóm 1 | Lưu `email`, `password_hash`, `role`, `status`, `failed_login_count`, `locked_until`, `last_login_at` |
| `refresh_tokens` | Nhóm 1 | Lưu mã băm SHA-256 của refresh token (`token_hash`), hạn dùng (`expires_at`) và thời điểm thu hồi (`revoked_at`) |
| `password_resets` | Nhóm 1 | Lưu mã băm token đặt lại mật khẩu dùng một lần (`token_hash`, `expires_at`, `used_at`) |
| `members` | Nhóm 3 | Tạo hồ sơ hội viên đồng thời khi khách tự đăng ký (gọi qua dịch vụ F2) |
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

5. **Quên mật khẩu (`POST /api/v1/auth/forgot-password`):**
   - **Request Body:**
     ```json
     {
       "email": "member@example.com"
     }
     ```
   - **Response (`200 OK`):**
     ```json
     {
       "message": "Nếu email có trong hệ thống, bạn sẽ nhận được liên kết đặt lại mật khẩu."
     }
     ```

6. **Đặt lại mật khẩu (`POST /api/v1/auth/reset-password`):**
   - **Request Body:**
     ```json
     {
       "token": "<raw_reset_token>",
       "newPassword": "NewPassword456"
     }
     ```
   - **Response:** `204 No Content`.
