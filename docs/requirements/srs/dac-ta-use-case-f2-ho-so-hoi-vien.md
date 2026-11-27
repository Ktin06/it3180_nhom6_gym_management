# Đặc tả Use Case – F2: Hồ sơ hội viên

- **Mã chức năng:** F2
- **Version:** v1 (Bắt buộc)
- **Người biên soạn:** Bùi Trần Đức Việt
- **Người duyệt chéo:** Thái Hùng Lân (API F2), Nguyễn Anh Tú (giao diện F2 & rà SRS Sprint 2, S2-32 #204); Nguyễn Trọng Việt (đối chiếu chỉ số cơ thể US-F2.5.1 với UC-F9.5)
- **Cập nhật 24/11/2026 (Bùi Trần Đức Việt):** viết lại mục H của UC-F2.1 đến UC-F2.5 theo cây truy vết ([ADR 0012](../../decisions/0012-quy-uoc-ma-truy-vet.md)): 6 user story, 31 AC có mã và mã quy tắc nghiệp vụ `BR-F2-01` đến `BR-F2-12`; cập nhật quy tắc đã chốt khi chạy thử Sprint 2 (VD-26: "học viên mình dạy" chỉ tính buổi PT chưa hủy, bỏ lớp nhóm; Q7: liên kết đặt mật khẩu lần đầu hạn 72 giờ).
- **Tài liệu chuẩn tham chiếu:** [Bản chốt phạm vi](../ban-chot-pham-vi.md), [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml), [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md), [ADR 0011 – Lưu ảnh đại diện trong PostgreSQL](../../decisions/0011-anh-dai-dien-luu-trong-postgresql.md), [ADR 0012](../../decisions/0012-quy-uoc-ma-truy-vet.md), [ADR 0013](../../decisions/0013-gui-email-qua-brevo.md)

---

## 1. Tổng quan chức năng

Chức năng **F2 – Hồ sơ hội viên** giúp lễ tân, quản lý chi nhánh và chủ chuỗi quản lý toàn bộ vòng đời hồ sơ hội viên tại phòng tập (huấn luyện viên chỉ xem hồ sơ và xem/ghi chỉ số cơ thể của học viên có buổi PT do mình trực tiếp dạy):
- Lễ tân tạo hồ sơ hội viên mới tại quầy (có thể tạo hồ sơ chưa có tài khoản đăng nhập).
- Lễ tân, quản lý chi nhánh hoặc chủ chuỗi tạo tài khoản đăng nhập cho hồ sơ hội viên đã có sẵn (trường hợp hội viên đăng ký trực tiếp tại quầy trước đó rồi mới muốn dùng cổng hội viên) và gửi email đặt mật khẩu hạn 72 giờ.
- Tìm kiếm nhanh hội viên theo **mã hội viên (`HV` + 6 chữ số)**, **số điện thoại** hoặc **tiền tố họ tên** với thời gian phản hồi dưới 2 giây trên 10.000 hội viên (NF1).
- Cập nhật thông tin hồ sơ, ngừng hoạt động hội viên (đồng thời khóa tài khoản đăng nhập và thu hồi mọi phiên trong cùng giao dịch), và quản lý lịch sử chỉ số cơ thể (`body_metrics`).
- *Ảnh đại diện (theo [ADR 0011](../../decisions/0011-anh-dai-dien-luu-trong-postgresql.md)):* Ảnh đại diện hội viên lưu trong PostgreSQL tại bảng riêng `member_avatars` (bỏ cột `members.avatar_url`), đọc/ghi qua API `GET /api/v1/members/{id}/avatar` và `PUT /api/v1/members/{id}/avatar` (triển khai ở Sprint 2 trước khi làm F4 – Check-in tại quầy).

---

## 2. Danh sách Use Case của F2

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F2.1** | Tạo hồ sơ hội viên tại quầy | Lễ tân, Quản lý chi nhánh, Chủ chuỗi | Cao | Sprint 1 |
| **UC-F2.2** | Tạo tài khoản đăng nhập cho hồ sơ hội viên có sẵn | Lễ tân, Quản lý chi nhánh, Chủ chuỗi | Trung bình | Sprint 2 |
| **UC-F2.3** | Tra cứu, lọc và xem chi tiết hồ sơ hội viên | Lễ tân, Quản lý chi nhánh, Chủ chuỗi (PT chỉ xem chi tiết học viên PT mình dạy; Hội viên chỉ xem chính mình) | Cao | Sprint 1 |
| **UC-F2.4** | Cập nhật hồ sơ, Ảnh đại diện & Ngừng hoạt động hội viên | Lễ tân, Quản lý chi nhánh, Chủ chuỗi | Cao | Sprint 1 (hồ sơ), Sprint 2 (ảnh đại diện) |
| **UC-F2.5** | Ghi nhận và xem lịch sử chỉ số cơ thể (`body_metrics`) | Lễ tân, Quản lý chi nhánh, Chủ chuỗi, PT (học viên PT mình dạy), Hội viên (chỉ xem của chính mình) | Trung bình | Sprint 2 |

---

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F2.1: Tạo hồ sơ hội viên tại quầy

- **Tác nhân:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`), Chủ chuỗi (`SUPER_ADMIN`). *(Ngoài ra chức năng tự đăng ký của F1 cũng gọi dịch vụ F2 khi khách tự đăng ký trên web).*
- **Tiền điều kiện:** Người thao tác đã đăng nhập với vai trò `RECEPTIONIST`, `BRANCH_MANAGER` (đã gắn chi nhánh đang hoạt động) hoặc `SUPER_ADMIN`.
- **Hậu điều kiện:**
  - Bản ghi mới trong bảng `members` được tạo với:
    - `member_code` sinh tự động từ sequence `member_code_seq` theo định dạng `HV` + 6 chữ số (`HV000001`, `HV000002`, ...).
    - `phone` chuẩn hóa 10 chữ số bắt đầu bằng `0` (`^0[0-9]{9}$`), duy nhất toàn hệ thống.
    - `home_branch_id` lấy theo chi nhánh của nhân viên thao tác (với `SUPER_ADMIN` lấy chi nhánh mặc định `CN01` trong v1–v3).
    - `created_by` lưu `users.id` của nhân viên thao tác.
    - `user_id` để `NULL` (hồ sơ chưa có tài khoản đăng nhập).
- **Luồng chính:**
  1. Nhân viên mở form "Thêm hội viên mới" và nhập: Họ và tên (`fullName`), Số điện thoại (`phone`), Giới tính (`gender` – tùy chọn), Ngày sinh (`dob` – tùy chọn). *(Ảnh đại diện tải riêng qua `PUT /api/v1/members/{id}/avatar` theo ADR 0011).*
  2. Hệ thống chuẩn hóa số điện thoại về 10 chữ số bắt đầu bằng `0` (ví dụ `+84 987 654 321` → `0987654321`) và kiểm tra định dạng bằng Bean Validation.
  3. Hệ thống kiểm tra vai trò và chi nhánh làm việc hợp lệ của nhân viên (S3).
  4. Hệ thống kiểm tra `phone` chưa tồn tại trong bảng `members` (kết hợp ràng buộc duy nhất dưới CSDL khi có nhiều yêu cầu đồng thời).
  5. Hệ thống lấy số tiếp theo từ `member_code_seq` để định dạng mã `HVxxxxxx`.
  6. Hệ thống lưu bản ghi `members` với `status = 'ACTIVE'` và trả về thông tin hồ sơ hội viên (`201 Created`, kèm header `Location`).
- **Luồng ngoại lệ:**
  - **E1 – Số điện thoại hoặc dữ liệu đầu vào không đúng định dạng (400 Bad Request):** Trả `ProblemDetail` (400) báo lỗi chi tiết theo từng trường (ví dụ trường `phone`, `fullName`).
  - **E2 – Trùng số điện thoại (409 Conflict):** Số điện thoại đã thuộc về một hội viên khác (kể cả khi hai lễ tân tạo cùng lúc) → Trả `ProblemDetail` (409): *"Số điện thoại này đã được đăng ký cho hội viên <mã hội viên>."*
  - **E3 – Không đủ quyền (403 Forbidden):** Người gọi là `TRAINER`, `MEMBER`, hoặc nhân viên chưa được gắn chi nhánh hợp lệ → Trả `ProblemDetail` (403).

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F2.1.1: Tạo hồ sơ hội viên tại quầy
- **User story:** Là **Lễ tân**, tôi muốn **tạo hồ sơ hội viên tại quầy**, để **bán gói và check-in cho người chưa có tài khoản web**.
- **BR liên quan:** BR-F2-05, BR-F2-06, BR-F2-07, BR-F2-10; S3, S5
- **Version / Sprint:** v1 / Sprint 1
- **Issue:** #123
- **Tiêu chí chấp nhận:**
  - **AC-F2.1.1-01: Tạo hồ sơ mới thành công, chuẩn hóa SĐT và tự sinh mã HV** (BR-F2-05, BR-F2-06, BR-F2-07)
    - **Given** Lễ tân hoặc Quản lý chi nhánh đã được gắn chi nhánh đang hoạt động (hoặc Chủ chuỗi)
    - **When** Gửi `POST /api/v1/members` với họ tên, số điện thoại hợp lệ (ví dụ `+84 987 654 321`), giới tính và ngày sinh tùy chọn
    - **Then** Trả `201 Created` kèm header `Location`; SĐT chuẩn hóa thành `0987654321`; tự sinh `memberCode` dạng `HV` + 6 chữ số; `homeBranchId` là chi nhánh của nhân viên; `createdBy` là người tạo; `status = ACTIVE` và chưa có tài khoản (`userId = null`)
  - **AC-F2.1.1-02: Từ chối khi số điện thoại sai định dạng hoặc đã tồn tại (kể cả tạo đồng thời)** (BR-F2-06)
    - **Given** Số điện thoại sai định dạng; hoặc số đã thuộc hội viên khác; hoặc hai lễ tân gửi tạo hồ sơ cùng một SĐT đồng thời
    - **When** Gửi `POST /api/v1/members`
    - **Then** Sai định dạng trả `400 Bad Request` kèm lỗi theo trường; trùng SĐT trả `409 Conflict` thông báo E2 có mã hội viên đang giữ số; tạo đồng thời chỉ đúng một hồ sơ thành công, yêu cầu còn lại nhận `409`
  - **AC-F2.1.1-03: Chỉ lễ tân, quản lý chi nhánh (đã gắn chi nhánh) và chủ chuỗi được tạo hồ sơ** (BR-F2-10)
    - **Given** Người gọi là Huấn luyện viên, Hội viên, hoặc nhân viên chưa được gán chi nhánh hợp lệ
    - **When** Gửi `POST /api/v1/members`
    - **Then** Trả `403 Forbidden`; không tạo hồ sơ nào

---

### 3.2. UC-F2.2: Tạo tài khoản đăng nhập cho hồ sơ hội viên có sẵn

- **Tác nhân:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`), Chủ chuỗi (`SUPER_ADMIN`).
- **Bối cảnh nghiệp vụ:** Hội viên trước đây đăng ký tập tại quầy chưa có tài khoản (`members.user_id IS NULL`), nay muốn dùng cổng hội viên trên web (hoặc khi tự đăng ký trên web bị chặn do trùng SĐT và được hướng dẫn ra quầy).
- **Tiền điều kiện:**
  - Người thao tác có vai trò `RECEPTIONIST`, `BRANCH_MANAGER` (cùng chi nhánh với hồ sơ) hoặc `SUPER_ADMIN` (mọi chi nhánh).
  - Hồ sơ hội viên tồn tại (`deleted_at IS NULL`), đang ở trạng thái `ACTIVE`, và chưa gắn tài khoản (`user_id IS NULL`).
- **Hậu điều kiện:**
  - Tạo bản ghi `users` mới (`role = 'MEMBER'`, `status = 'ACTIVE'`, `email` chữ thường, mật khẩu ngẫu nhiên băm bcrypt).
  - Gắn `members.user_id = users.id` trong cùng giao dịch có khóa dòng hồ sơ.
  - Tạo token đặt mật khẩu lần đầu lưu mã băm SHA-256 trong `password_resets` với hạn **72 giờ**, gửi email tiêu đề *"Kích hoạt tài khoản"* chứa liên kết đặt mật khẩu qua Brevo ([ADR 0013](../../decisions/0013-gui-email-qua-brevo.md)).
- **Luồng chính (`POST /api/v1/members/{id}/account`):**
  1. Nhân viên mở chi tiết hồ sơ hội viên `ACTIVE` chưa có tài khoản, chọn "Tạo tài khoản" và nhập `email` của hội viên (sau khi đối chiếu thông tin tại quầy).
  2. Hệ thống kiểm tra định dạng `id` (`> 0`) và `email` bằng Bean Validation (S5), chuẩn hóa `email` về chữ thường.
  3. Hệ thống kiểm tra vai trò và phạm vi chi nhánh (S3), khóa dòng hồ sơ `members` (`SELECT ... FOR UPDATE`).
  4. Hệ thống kiểm tra hồ sơ đang `ACTIVE`, `user_id IS NULL`, và `email` chưa tồn tại trong bảng `users`.
  5. Trong cùng giao dịch, hệ thống tạo bản ghi `users` (`role = 'MEMBER'`, mật khẩu ngẫu nhiên băm bcrypt), gắn `members.user_id` và phát hành token đặt mật khẩu hạn **72 giờ**. Sau khi giao dịch hoàn tất thành công, hệ thống gửi email *"Kích hoạt tài khoản"*.
  6. Trả `200 OK` kèm `memberId`, `userId`, `email` (không trả token). Giao diện thông báo đã gửi email, tải lại hồ sơ và ẩn nút "Tạo tài khoản".
- **Luồng thay thế và ngoại lệ (Alternative / Exception Flows):**
  - **A1 – Gửi email gặp lỗi (200 OK):** Nếu dịch vụ gửi email gặp lỗi, hệ thống ghi log mức `ERROR` **không chứa token**, vẫn giữ tài khoản và trả `200 OK`; hội viên dùng chức năng "Quên mật khẩu" (UC-F1.5) để nhận lại liên kết.
  - **E1 – Hồ sơ đã có tài khoản (409 Conflict):** `members.user_id` đã có giá trị → Trả `ProblemDetail` (409): *"Hội viên này đã có tài khoản đăng nhập."*; không tạo thêm tài khoản và không gửi email.
  - **E2 – Email đã được sử dụng (409 Conflict):** `email` đã thuộc tài khoản khác (kể cả khi gửi đồng thời) → Trả `ProblemDetail` (409): *"Email này đã được sử dụng bởi một tài khoản khác."*; hồ sơ giữ nguyên `user_id IS NULL`.
  - **E3 – Hồ sơ đang ngừng hoạt động (422 Unprocessable Content):** `members.status = 'INACTIVE'` → Trả `ProblemDetail` (422): *"Chỉ có thể tạo tài khoản cho hồ sơ hội viên đang hoạt động."*; không tạo tài khoản và không gửi email.
  - **E4 – Không tìm thấy hoặc ngoài phạm vi chi nhánh (404 Not Found):** Hồ sơ không tồn tại, đã xóa mềm, hoặc nhân viên chi nhánh gọi cho hồ sơ chi nhánh khác → Trả `ProblemDetail` (404).
  - **E5 – Không đủ quyền (403 Forbidden):** Người gọi là `MEMBER`, `TRAINER` hoặc nhân viên chưa gắn chi nhánh hợp lệ → Trả `ProblemDetail` (403).
  - **E6 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):** Email trống, sai định dạng, vượt 255 ký tự hoặc `id <= 0` → Trả `ProblemDetail` (400) kèm danh sách lỗi theo trường (`errors`).

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F2.2.1: Tạo tài khoản cho hồ sơ có sẵn
- **User story:** Là **Lễ tân**, tôi muốn **tạo tài khoản đăng nhập cho hồ sơ có sẵn**, để **hội viên dùng cổng web mà không bị tạo hồ sơ trùng**.
- **BR liên quan:** BR-F2-06, BR-F2-10, BR-F2-11; BR-F1-01, BR-F1-03, BR-F1-08; S3, S5
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #124
- **Tiêu chí chấp nhận:**
  - **AC-F2.2.1-01: Tạo tài khoản và gửi email đặt mật khẩu hạn 72 giờ** (BR-F2-06, BR-F2-11)
    - **Given** Hồ sơ hội viên `ACTIVE` chưa có tài khoản, thuộc chi nhánh của nhân viên thao tác
    - **When** Lễ tân nhập email (ví dụ `New.Member@Example.com`) và gửi `POST /api/v1/members/{id}/account`
    - **Then** Trả `200 OK` (`memberId`, `userId`, `email` chữ thường, không trả token); tạo tài khoản `MEMBER` với mật khẩu ngẫu nhiên băm bcrypt và gắn `members.user_id` trong cùng giao dịch; gửi email *"Kích hoạt tài khoản"* chứa liên kết đặt mật khẩu hạn **72 giờ**; giao diện báo đã gửi email, tải lại hồ sơ và ẩn nút "Tạo tài khoản"
  - **AC-F2.2.1-02: Hội viên đặt mật khẩu lần đầu rồi đăng nhập; quá 72 giờ liên kết hết hạn** (BR-F2-11)
    - **Given** Tài khoản vừa được tạo qua `AC-F2.2.1-01`
    - **When** Hội viên mở liên kết đặt mật khẩu mới (UC-F1.5) trong vòng 72 giờ rồi đăng nhập; hoặc mở liên kết sau khi đã quá 72 giờ
    - **Then** Trong hạn 72 giờ đặt mật khẩu trả `204` và đăng nhập trả `200 OK`; quá 72 giờ đặt mật khẩu bị từ chối `422`
  - **AC-F2.2.1-03: Từ chối khi hồ sơ đã có tài khoản** (BR-F2-11)
    - **Given** Hồ sơ hội viên đã gắn tài khoản (`user_id IS NOT NULL`)
    - **When** Gọi `POST /api/v1/members/{id}/account`
    - **Then** Trả `409 Conflict` thông báo E1 *"Hội viên này đã có tài khoản đăng nhập."*; không tạo thêm tài khoản, không gửi email; trên giao diện chỉ hiện email và không có nút "Tạo tài khoản"
  - **AC-F2.2.1-04: Từ chối khi email đã được sử dụng (kể cả đồng thời)** (BR-F2-06, BR-F2-11)
    - **Given** Email (không phân biệt hoa thường) đã thuộc tài khoản khác, hoặc hai yêu cầu dùng cùng một email mới gửi đồng thời
    - **When** Gọi `POST /api/v1/members/{id}/account`
    - **Then** Trả `409 Conflict` thông báo E2 *"Email này đã được sử dụng bởi một tài khoản khác."*; hồ sơ không bị gắn tài khoản sai
  - **AC-F2.2.1-05: Từ chối khi hồ sơ đang ngừng hoạt động** (BR-F2-11)
    - **Given** Hồ sơ hội viên có trạng thái `INACTIVE`
    - **When** Gọi `POST /api/v1/members/{id}/account`
    - **Then** Trả `422 Unprocessable Content` thông báo E3 *"Chỉ có thể tạo tài khoản cho hồ sơ hội viên đang hoạt động."*; không tạo tài khoản, không gửi email; giao diện không hiện nút "Tạo tài khoản"
  - **AC-F2.2.1-06: Gửi email gặp lỗi vẫn giữ tài khoản và ghi log không chứa token** (BR-F2-11)
    - **Given** Dịch vụ gửi email gặp lỗi khi tạo tài khoản cho hồ sơ hợp lệ
    - **When** Gọi `POST /api/v1/members/{id}/account`
    - **Then** Giao dịch tạo tài khoản vẫn thành công (`200 OK`), ghi log mức `ERROR` **không chứa token**; hội viên có thể dùng "Quên mật khẩu" để nhận lại liên kết đặt mật khẩu
  - **AC-F2.2.1-07: Kiểm tra quyền theo vai trò, chi nhánh và dữ liệu đầu vào** (BR-F2-10, BR-F2-11)
    - **Given** Nhân viên chi nhánh khác; Hội viên hoặc Huấn luyện viên; Chủ chuỗi; hoặc dữ liệu email sai/trống, mã hồ sơ không dương
    - **When** Gọi `POST /api/v1/members/{id}/account`
    - **Then** Nhân viên chi nhánh khác hoặc hồ sơ không tồn tại trả `404`; Hội viên và Huấn luyện viên trả `403`; Chủ chuỗi tạo được cho mọi chi nhánh (`200`); dữ liệu sai trả `400` kèm lỗi theo trường (giao diện kiểm tra email sớm trước khi gọi API)

---

### 3.3. UC-F2.3: Tra cứu, lọc và xem chi tiết hồ sơ hội viên

- **Tác nhân:**
  - Tra cứu, tìm kiếm danh sách hội viên (`GET /api/v1/members`): Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`), Chủ chuỗi (`SUPER_ADMIN`). **Huấn luyện viên (`TRAINER`) và Hội viên (`MEMBER`) không có quyền gọi API này.**
  - Xem chi tiết hồ sơ hội viên (`GET /api/v1/members/{id}`): Chủ chuỗi (`SUPER_ADMIN`), Quản lý chi nhánh (`BRANCH_MANAGER`), Lễ tân (`RECEPTIONIST`), Huấn luyện viên (`TRAINER` – **chỉ với học viên PT mình dạy**, tính buổi PT chưa hủy `BOOKED`/`DONE`/`NO_SHOW`, bỏ lớp nhóm theo VD-26), Hội viên (`MEMBER` – chỉ xem hồ sơ của chính mình).
- **Tiền điều kiện:** Người dùng đã đăng nhập hợp lệ.
- **Luồng chính – Tra cứu và lọc danh sách (`GET /api/v1/members`):**
  1. Nhân viên (`RECEPTIONIST`, `BRANCH_MANAGER`, `SUPER_ADMIN`) nhập từ khóa tìm kiếm (`keyword`), lọc theo trạng thái (`status`: `ACTIVE` / `INACTIVE`) và phân trang (`page`, `size`).
  2. Hệ thống xử lý tìm kiếm tối ưu hiệu năng (đảm bảo dưới 2 giây với 10.000 hội viên – NF1):
     - Nếu `keyword` có dạng mã hội viên (`HV...`, không phân biệt hoa thường): tìm theo chỉ mục duy nhất `member_code`.
     - Nếu `keyword` là chuỗi chữ số / số điện thoại (kể cả phần đầu): chuẩn hóa và tìm theo chỉ mục `phone`.
     - Trường hợp khác: thoát ký tự đại diện (`%`, `_`) và tìm theo phần đầu của `full_name` (sử dụng chỉ mục trên `full_name`).
     - Tự động lọc theo chi nhánh của nhân viên (`members.home_branch_id = staff.branch_id`), trừ `SUPER_ADMIN` xem được toàn chuỗi.
     - Không lấy các bản ghi có `deleted_at IS NOT NULL`.
  3. Hệ thống trả về `PageResponse<MemberResponse>` gồm thông tin hội viên (kèm `email` lấy từ bảng `users` qua `LEFT JOIN` nếu hội viên đã có tài khoản).
- **Luồng chính – Xem chi tiết hồ sơ (`GET /api/v1/members/{id}`):**
  1. Người dùng yêu cầu xem chi tiết hội viên theo `id`.
  2. Hệ thống kiểm tra hội viên tồn tại (`deleted_at IS NULL`) và kiểm tra quyền phạm vi dữ liệu (S3, VD-26):
     - `SUPER_ADMIN`: xem được hội viên thuộc mọi chi nhánh.
     - Nhân viên chi nhánh (`BRANCH_MANAGER`, `RECEPTIONIST`): **chỉ xem được hội viên trong chi nhánh của mình** (`members.home_branch_id = staff.branch_id`).
     - `TRAINER`: **chỉ xem được học viên PT mình dạy** (có ít nhất một buổi PT với trạng thái chưa hủy: `BOOKED`, `DONE`, `NO_SHOW`; không tính học viên chỉ học lớp nhóm).
     - `MEMBER`: **chỉ xem được hồ sơ của chính mình** (`members.user_id = currentUserId`).
  3. Hệ thống trả về thông tin chi tiết của hội viên đó (`200 OK`).
- **Luồng ngoại lệ:**
  - **E1 – Không đủ quyền theo vai trò hoặc không dạy hội viên cùng chi nhánh (403 Forbidden):** `TRAINER` hoặc `MEMBER` gọi `GET /api/v1/members`; hoặc `TRAINER` gọi `GET /api/v1/members/{id}` với hội viên **cùng chi nhánh** nhưng không có buổi PT do mình dạy (bao gồm hội viên chỉ học lớp nhóm do PT đó dạy) → Trả `ProblemDetail` (403).
  - **E2 – Không tìm thấy hoặc ngoài phạm vi chi nhánh/cá nhân (404 Not Found):** Không tồn tại hội viên với `id` yêu cầu, `deleted_at IS NOT NULL`, nhân viên (`BRANCH_MANAGER`, `RECEPTIONIST`) xem hội viên thuộc chi nhánh khác (`members.home_branch_id != staff.branch_id`), `TRAINER` xem hội viên không dạy thuộc **chi nhánh khác**, hoặc `MEMBER` xem hồ sơ của hội viên khác → Trả `ProblemDetail` (404) *(cố ý trả 404 để người ngoài phạm vi không biết hồ sơ có tồn tại)*.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F2.3.1: Tra cứu và xem hồ sơ hội viên
- **User story:** Là **Lễ tân**, tôi muốn **tìm hồ sơ theo mã, số điện thoại hoặc họ tên**, để **phục vụ hội viên nhanh tại quầy**.
- **BR liên quan:** BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10; NF1, S3, S5
- **Version / Sprint:** v1 / Sprint 1 (quyền PT theo VD-26 cập nhật Sprint 2)
- **Issue:** #125
- **Tiêu chí chấp nhận:**
  - **AC-F2.3.1-01: Tìm kiếm, lọc, phân trang trong chi nhánh mình đạt hiệu năng dưới 2 giây** (BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10)
    - **Given** Lễ tân hoặc Quản lý chi nhánh `CN01` đã đăng nhập; CSDL có hồ sơ ở `CN01`, `CN02` và hồ sơ đã xóa mềm
    - **When** Gọi `GET /api/v1/members` tìm theo mã `HV…` (không phân biệt hoa thường), phần đầu SĐT hoặc phần đầu họ tên (kể cả chứa ký tự `%`, `_`), lọc trạng thái và phân trang
    - **Then** Trả `200 OK`, danh sách và tổng số bản ghi chỉ gồm hồ sơ `CN01` chưa xóa mềm, có kèm `email` nếu đã có tài khoản; thời gian phản hồi dưới 2 giây với 10.000 hồ sơ (NF1)
  - **AC-F2.3.1-02: Chủ chuỗi tra cứu và xem chi tiết hồ sơ mọi chi nhánh** (BR-F2-10)
    - **Given** Chủ chuỗi (`SUPER_ADMIN`) đã đăng nhập
    - **When** Tìm kiếm danh sách (`GET /api/v1/members`) hoặc xem chi tiết (`GET /api/v1/members/{id}`)
    - **Then** Trả `200 OK` và thấy được hồ sơ của mọi chi nhánh chưa xóa mềm
  - **AC-F2.3.1-03: Xem chi tiết đúng phạm vi; ngoài phạm vi hoặc đã xóa mềm trả 404** (BR-F2-09, BR-F2-10)
    - **Given** Lễ tân `CN01` xem hồ sơ `CN02`; Hội viên A xem hồ sơ của chính mình và xem hồ sơ Hội viên B; hoặc hồ sơ không tồn tại / đã xóa mềm
    - **When** Gọi `GET /api/v1/members/{id}`
    - **Then** Hội viên xem chính mình trả `200 OK`; mọi trường hợp ngoài phạm vi, không tồn tại hoặc đã xóa mềm đều trả `404 Not Found` (cố ý ẩn sự tồn tại của hồ sơ)
  - **AC-F2.3.1-04: Huấn luyện viên chỉ xem chi tiết học viên PT của mình và không được gọi danh sách** (BR-F2-10)
    - **Given** Huấn luyện viên X có buổi PT chưa hủy (`BOOKED`, `DONE`, `NO_SHOW`) với hội viên A; X chỉ dạy lớp nhóm có hội viên B; X không dạy hội viên C (cùng chi nhánh) và D (chi nhánh khác)
    - **When** X gọi `GET /api/v1/members/{id}` cho A, B, C, D; hoặc gọi `GET /api/v1/members`
    - **Then** Xem hồ sơ A trả `200 OK`; xem B và C trả `403 Forbidden`; xem D trả `404 Not Found`; gọi danh sách `GET /api/v1/members` trả `403 Forbidden`

---

### 3.4. UC-F2.4: Cập nhật hồ sơ, Ảnh đại diện & Ngừng hoạt động hội viên

- **Tác nhân:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`), Chủ chuỗi (`SUPER_ADMIN`). *(Hội viên chỉ được xem ảnh đại diện của chính mình, không tự đổi ảnh).*
- **Tiền điều kiện:** Hồ sơ hội viên tồn tại (`deleted_at IS NULL`).
- **Luồng chính – Cập nhật thông tin & Trạng thái (`PUT /api/v1/members/{id}`):**
  1. Nhân viên sửa các thông tin: `fullName`, `phone`, `gender`, `dob`, `status` (bắt buộc).
  2. Nếu đổi `phone`, hệ thống chuẩn hóa về 10 chữ số bắt đầu bằng `0` và kiểm tra không trùng với hội viên khác (giữ nguyên số cũ của chính hội viên đó vẫn hợp lệ).
  3. **Quy tắc ngừng hoạt động hội viên:** Nếu chuyển `members.status` sang `INACTIVE` và hội viên đó đã có tài khoản (`user_id IS NOT NULL`), hệ thống **bắt buộc chuyển đồng thời `users.status = 'DISABLED'` và thu hồi mọi `refresh_tokens` đang hiệu lực của tài khoản đó trong cùng giao dịch CSDL**.
  4. Nếu kích hoạt lại hội viên (`members.status = 'ACTIVE'`) và tài khoản đang là `DISABLED`, chuyển `users.status = 'ACTIVE'` (không khôi phục refresh token cũ đã thu hồi và không gỡ khóa đăng nhập tạm `locked_until` nếu đang khóa).
  5. Lưu thay đổi (có khóa dòng khi cập nhật trạng thái đồng thời – NF6) và trả về hồ sơ sau cập nhật (`200 OK`).
- **Luồng chính – Tải lên và xem ảnh đại diện (`PUT/GET /api/v1/members/{id}/avatar`, ADR 0011):**
  1. Nhân viên có quyền sửa hồ sơ gửi file ảnh JPEG/PNG (`≤ 5 MiB`, mỗi cạnh `≤ 8.000 px`) qua multipart trường `file`.
  2. Server kiểm tra định dạng thực từ nội dung file, đọc kích thước trước khi giải nén bitmap, thu nhỏ giữ tỷ lệ (cạnh dài tối đa 256 px, không phóng lớn ảnh nhỏ, nền trong suốt vẽ lên nền trắng), nén JPEG (`≤ 100 KiB`) và lưu hoặc ghi đè đúng 1 bản ghi trong `member_avatars` (kèm `uploaded_by`, khóa dòng `members`), trả `200 OK`.
  3. Người có quyền xem hồ sơ gọi `GET /api/v1/members/{id}/avatar` nhận nội dung nhị phân `image/jpeg` (`200 OK`); chưa có ảnh trả `404`.
- **Luồng ngoại lệ:**
  - **E1 – Không tìm thấy hoặc ngoài phạm vi chi nhánh (404 Not Found):** Trả `ProblemDetail` (404).
  - **E2 – Số điện thoại mới bị trùng (409 Conflict):** Trả `ProblemDetail` (409).
  - **E3 – Dữ liệu đầu vào hoặc file ảnh không hợp lệ (400 Bad Request):** Thiếu trường bắt buộc (như `status`), sai định dạng SĐT, `id <= 0`, hoặc file ảnh rỗng / sai định dạng JPEG-PNG thật / quá 5 MiB / cạnh vượt 8.000 px → Trả `ProblemDetail` (400) kèm lỗi theo trường.
  - **E4 – Không đủ quyền (403 Forbidden):** Vai trò không được phép (ví dụ `MEMBER` tự đổi ảnh hoặc `TRAINER` sửa hồ sơ) hoặc nhân viên chưa gắn chi nhánh hợp lệ → Trả `ProblemDetail` (403).

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F2.4.1: Cập nhật và ngừng hoạt động hồ sơ
- **User story:** Là **Lễ tân**, tôi muốn **sửa hồ sơ và đánh dấu ngừng hoạt động**, để **dữ liệu luôn đúng mà không xóa lịch sử**.
- **BR liên quan:** BR-F2-06, BR-F2-08, BR-F2-09, BR-F2-10; NF6, S3, S5
- **Version / Sprint:** v1 / Sprint 1
- **Issue:** #126
- **Tiêu chí chấp nhận:**
  - **AC-F2.4.1-01: Cập nhật thông tin hồ sơ trong chi nhánh và chuẩn hóa số điện thoại** (BR-F2-06, BR-F2-10)
    - **Given** Nhân viên chi nhánh (hoặc Chủ chuỗi) đã đăng nhập và hồ sơ thuộc phạm vi chi nhánh
    - **When** Gửi `PUT /api/v1/members/{id}` sửa họ tên, số điện thoại (kể cả giữ nguyên số cũ), giới tính, ngày sinh
    - **Then** Trả `200 OK`; thông tin được cập nhật và số điện thoại chuẩn hóa 10 chữ số bắt đầu bằng `0`
  - **AC-F2.4.1-02: Từ chối trùng số điện thoại, thiếu trạng thái hoặc hồ sơ ngoài phạm vi** (BR-F2-06, BR-F2-10)
    - **Given** Số điện thoại mới đã thuộc hội viên khác; hoặc request không gửi `status`; hoặc hồ sơ không tồn tại / thuộc chi nhánh khác; hoặc nhân viên chưa gắn chi nhánh
    - **When** Gửi `PUT /api/v1/members/{id}`
    - **Then** Lần lượt trả `409 Conflict`, `400 Bad Request`, `404 Not Found`, `403 Forbidden`; không thay đổi dữ liệu hồ sơ
  - **AC-F2.4.1-03: Ngừng hoạt động hồ sơ đồng thời khóa tài khoản và thu hồi mọi phiên trong cùng giao dịch** (BR-F2-08, BR-F2-09)
    - **Given** Hồ sơ `ACTIVE` đã có tài khoản và hội viên đang có phiên đăng nhập hiệu lực
    - **When** Chuyển `members.status` sang `INACTIVE`
    - **Then** Trong cùng giao dịch: `members.status = INACTIVE` (không xóa cứng), `users.status = DISABLED`, mọi `refresh_tokens` bị thu hồi ngay; nếu có lỗi giữa chừng thì rollback toàn bộ, không đổi trạng thái nào
  - **AC-F2.4.1-04: Kích hoạt lại hồ sơ khôi phục tài khoản nhưng không khôi phục token cũ và không gỡ khóa tạm** (BR-F2-08)
    - **Given** Hồ sơ `INACTIVE` có tài khoản `DISABLED`; hoặc tài khoản đang bị khóa tạm do nhập sai mật khẩu (`locked_until > now`)
    - **When** Chuyển `members.status` về `ACTIVE`
    - **Then** Tài khoản `DISABLED` chuyển về `ACTIVE`; các refresh token đã thu hồi trước đó **không** dùng lại được; tài khoản đang khóa tạm **vẫn giữ khóa** tới hết hạn
  - **AC-F2.4.1-05: Cập nhật trạng thái đồng thời luôn giữ hồ sơ và tài khoản khớp nhau** (BR-F2-08)
    - **Given** Hai nhân viên cùng đổi trạng thái (`ACTIVE`/`INACTIVE`) của một hồ sơ gần như cùng lúc
    - **When** Hai yêu cầu cập nhật chạy đồng thời
    - **Then** Trạng thái cuối cùng của `members.status` và `users.status` luôn khớp nhau (`ACTIVE` ↔ `ACTIVE`, `INACTIVE` ↔ `DISABLED`)

##### US-F2.4.2: Ảnh đại diện hội viên
- **User story:** Là **Lễ tân**, tôi muốn **tải ảnh đại diện cho hồ sơ**, để **đối chiếu đúng người khi check-in, chống mượn thẻ**.
- **BR liên quan:** BR-F2-01, BR-F2-02, BR-F2-03, BR-F2-04; ADR 0011, S2, S3, S5, NF6
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #127
- **Tiêu chí chấp nhận:**
  - **AC-F2.4.2-01: Tải và chuẩn hóa ảnh hợp lệ** (BR-F2-01, BR-F2-02)
    - **Given** Nhân viên có quyền sửa hồ sơ và một file JPEG/PNG hợp lệ, không rỗng, tối đa 5 MiB (5.242.880 byte), mỗi cạnh tối đa 8.000 px
    - **When** Gửi `PUT /api/v1/members/{id}/avatar` với multipart trường `file`
    - **Then** Trả `200 OK` với body rỗng; lưu JPEG trong `member_avatars`, giữ tỷ lệ với cạnh dài nhất ≤ 256 px, dung lượng ≤ 102.400 byte, đúng `uploaded_by`; không phóng lớn ảnh nhỏ và chuyển nền trong suốt thành trắng; giao diện tải lại ảnh mới
  - **AC-F2.4.2-02: Thay ảnh trên cùng bản ghi** (BR-F2-03)
    - **Given** Hồ sơ đã có ảnh và nhân viên có quyền sửa
    - **When** Tải ảnh mới hợp lệ, kể cả hai yêu cầu cùng lúc
    - **Then** Mỗi hội viên vẫn chỉ có một bản ghi ảnh trong `member_avatars`, cập nhật nội dung và người tải gần nhất; các thao tác được tuần tự hóa bằng khóa dòng hội viên. Tải ảnh sai bị từ chối thì ảnh cũ giữ nguyên
  - **AC-F2.4.2-03: Từ chối file không hợp lệ** (BR-F2-01)
    - **Given** Nhân viên có quyền sửa hồ sơ
    - **When** Tải file rỗng, không phải JPEG/PNG thực, ảnh hỏng, file quá 5 MiB hoặc một cạnh quá 8.000 px
    - **Then** Trả `400 Bad Request` dạng `ProblemDetail`, không lưu hoặc thay ảnh; lỗi vượt dung lượng có `errors.file`. Định dạng được kiểm tra từ nội dung, kích thước được đọc trước khi giải nén bitmap; giao diện chặn sớm file sai định dạng hoặc quá 5 MiB mà không gọi API
  - **AC-F2.4.2-04: Xem ảnh theo quyền hồ sơ** (BR-F2-04)
    - **Given** Hội viên đã có ảnh
    - **When** Chủ chuỗi, Quản lý/Lễ tân cùng chi nhánh hoặc Hội viên sở hữu hồ sơ gọi `GET /api/v1/members/{id}/avatar`
    - **Then** Trả `200 OK` và đúng nội dung nhị phân `image/jpeg`; nhân viên khác chi nhánh hoặc hội viên khác nhận `404 Not Found`. Huấn luyện viên không dạy hội viên (hoặc theo phạm vi hiện hành của API hồ sơ) nhận `403 Forbidden` / `404 Not Found`
  - **AC-F2.4.2-05: Chỉ nhân viên có quyền sửa mới đổi ảnh** (BR-F2-04)
    - **Given** Người gọi đã đăng nhập
    - **When** Gửi `PUT /api/v1/members/{id}/avatar` (hoặc mở form hồ sơ trên giao diện)
    - **Then** Chủ chuỗi thao tác được toàn chuỗi, Quản lý/Lễ tân chỉ trong chi nhánh đang hoạt động của mình; ngoài chi nhánh trả `404`, nhân viên không có phân công hợp lệ trả `403`, `MEMBER`/`TRAINER` trả `403`; bị từ chối thì dữ liệu giữ nguyên; giao diện không hiện nút đổi ảnh cho hội viên
  - **AC-F2.4.2-06: Chưa có ảnh hoặc không có hồ sơ** (BR-F2-04)
    - **Given** Người gọi có vai trò phù hợp
    - **When** Gọi `GET` cho hồ sơ chưa có ảnh, hoặc `GET`/`PUT` cho hồ sơ không tồn tại hay đã xóa mềm
    - **Then** Trả `404 Not Found` và không tạo dữ liệu ảnh mới
  - **AC-F2.4.2-07: Xác thực và kiểm tra request ảnh** (S2, S5)
    - **Given** Hai endpoint ảnh đại diện
    - **When** Chưa đăng nhập; hoặc người có quyền gửi `id` không phải số nguyên dương hay `PUT` thiếu trường `file`
    - **Then** Chưa đăng nhập trả `401 Unauthorized`; `id` sai hoặc thiếu `file` trả `400 Bad Request`, không thay dữ liệu
  - **AC-F2.4.2-08: Chưa có ảnh hoặc tải ảnh lỗi thì hiện chữ cái đầu của tên trên giao diện** (BR-F2-04)
    - **Given** Hồ sơ chưa có ảnh (`hasAvatar = false`), hoặc tải ảnh từ API trả lỗi (`404`/lỗi mạng)
    - **When** Người dùng mở màn hình hiển thị ảnh đại diện hội viên
    - **Then** Giao diện hiển thị chữ cái đầu của tên hội viên; không gọi API `GET /api/v1/members/{id}/avatar` khi đã biết chắc `hasAvatar = false`

---

### 3.5. UC-F2.5: Ghi nhận và xem lịch sử chỉ số cơ thể (`body_metrics`)

- **Tác nhân:** Huấn luyện viên (`TRAINER` – chỉ với học viên PT mình dạy), Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`), Chủ chuỗi (`SUPER_ADMIN`), Hội viên (`MEMBER` – chỉ xem chỉ số của chính mình, không tự ghi).
- **Ghi chú đồng bộ với SRS F9:** Chỉ số cơ thể xuất hiện ở cả UC-F2.5 (SRS F2) và UC-F9.5 (SRS F9). **Bản chuẩn các AC của chỉ số cơ thể đặt tại `US-F2.5.1` (#128) dưới đây**; UC-F9.5 trong SRS F9 trỏ sang đây để tránh trùng lặp và lệch nội dung.
- **Tiền điều kiện:** Người dùng đã đăng nhập hợp lệ; hồ sơ hội viên tồn tại (`deleted_at IS NULL`).
- **Luồng chính – Ghi chỉ số cơ thể (`POST /api/v1/members/{id}/body-metrics`):**
  1. Nhân viên (`SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST` trong phạm vi chi nhánh; hoặc `TRAINER` với học viên PT mình dạy) chọn hội viên và nhập: ngày đo (`measuredAt`, bắt buộc, không sau hôm nay theo giờ Việt Nam) và **ít nhất một** chỉ số hợp lệ gồm cân nặng kg (`weight`), chiều cao cm (`height`), tỷ lệ mỡ % (`bodyFat`), khối lượng cơ kg (`muscleMass`), cùng ghi chú (`note` – tùy chọn).
  2. Hệ thống kiểm tra dữ liệu đầu vào (Bean Validation – S5) và kiểm tra quyền phạm vi dữ liệu (S3, VD-26).
  3. Hệ thống gán `recorded_by = currentUserId` (lấy từ token), lưu bản ghi mới vào bảng `body_metrics` và trả `201 Created`.
- **Luồng chính – Xem lịch sử chỉ số cơ thể (`GET /api/v1/members/{id}/body-metrics`):**
  1. Người có quyền xem hồ sơ (`SUPER_ADMIN`, `BRANCH_MANAGER`/`RECEPTIONIST` cùng chi nhánh, `TRAINER` với học viên PT mình dạy, `MEMBER` xem chính mình) gọi API xem lịch sử.
  2. Hệ thống trả danh sách sắp xếp theo `measured_at DESC, id DESC` (mới nhất trước; cùng ngày thì lần ghi sau đứng trước), không lẫn hội viên khác (`200 OK`).
- **Luồng ngoại lệ:**
  - **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):** Thiếu ngày đo, ngày đo sau ngày hôm nay (giờ Việt Nam), không có chỉ số nào trong 4 chỉ số, hoặc chỉ số âm, bằng 0 (trừ `bodyFat` theo ràng buộc khoảng hợp lệ), ngoài khoảng cho phép, hay quá 2 chữ số thập phân → Trả `ProblemDetail` (400) kèm danh sách lỗi theo trường (`errors`).
  - **E2 – Không đủ quyền (403 Forbidden):** `MEMBER` gọi `POST` tự ghi chỉ số; hoặc `TRAINER` xem/ghi cho hội viên **cùng chi nhánh** nhưng không phải học viên PT của mình (kể cả học viên chỉ học lớp nhóm) → Trả `ProblemDetail` (403).
  - **E3 – Không tìm thấy hoặc ngoài phạm vi chi nhánh/cá nhân (404 Not Found):** Hồ sơ không tồn tại hoặc đã xóa mềm; nhân viên chi nhánh khác; `TRAINER` không dạy hội viên thuộc chi nhánh khác; `MEMBER` xem chỉ số của hội viên khác → Trả `ProblemDetail` (404).

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F2.5.1: Chỉ số cơ thể
- **User story:** Là **Huấn luyện viên**, tôi muốn **ghi và xem lịch sử chỉ số cơ thể của hội viên**, để **theo dõi tiến bộ**.
- **BR liên quan:** BR-F2-10, BR-F2-12; S3, S5
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #128
- **Tiêu chí chấp nhận:**
  - **AC-F2.5.1-01: Ghi chỉ số cơ thể thành công với ít nhất một chỉ số** (BR-F2-12)
    - **Given** Lễ tân/Quản lý chi nhánh với hội viên chi nhánh mình, Chủ chuỗi với hội viên bất kỳ, hoặc Huấn luyện viên với học viên PT của mình
    - **When** Gửi `POST /api/v1/members/{id}/body-metrics` với ngày đo hợp lệ và ít nhất một chỉ số (`weight`, `height`, `bodyFat` hoặc `muscleMass`)
    - **Then** Trả `201 Created`; lưu bản ghi mới vào `body_metrics` với `recorded_by` là mã tài khoản người đang đăng nhập
  - **AC-F2.5.1-02: Từ chối dữ liệu chỉ số cơ thể không hợp lệ** (BR-F2-12)
    - **Given** Người dùng có quyền ghi chỉ số
    - **When** Gửi yêu cầu không có chỉ số nào; thiếu ngày đo; ngày đo sau hôm nay (theo giờ Việt Nam); hoặc chỉ số âm, bằng 0, ngoài khoảng cho phép hay quá 2 chữ số thập phân
    - **Then** Trả `400 Bad Request` kèm thông báo rõ theo từng trường (`errors`), không lưu bản ghi; giao diện hiển thị lỗi dưới đúng ô nhập
  - **AC-F2.5.1-03: Xem lịch sử chỉ số cơ thể sắp xếp mới nhất trước** (BR-F2-12)
    - **Given** Hội viên có nhiều lần đo ở nhiều ngày khác nhau, trong đó có hai lần đo cùng ngày
    - **When** Gọi `GET /api/v1/members/{id}/body-metrics`
    - **Then** Trả `200 OK`, danh sách sắp xếp theo ngày đo giảm dần (`measured_at DESC`), cùng ngày thì lần ghi sau đứng trước (`id DESC`), và không lẫn dữ liệu của hội viên khác
  - **AC-F2.5.1-04: Phân quyền ghi và xem chỉ số cơ thể theo vai trò, chi nhánh và quan hệ PT** (BR-F2-10, BR-F2-12)
    - **Given** Hội viên, Huấn luyện viên (có dạy PT và không dạy PT), Nhân viên chi nhánh khác và Chủ chuỗi
    - **When** Gọi `GET` hoặc `POST /api/v1/members/{id}/body-metrics`
    - **Then** Hội viên xem của mình `200`, xem của người khác `404`, tự ghi `403`. Huấn luyện viên dạy PT xem/ghi `200`/`201`; không dạy cùng chi nhánh `403`; không dạy khác chi nhánh `404`. Nhân viên chi nhánh khác `404`. Chủ chuỗi xem/ghi được mọi chi nhánh (`200`/`201`)

---

## 4. Quy tắc nghiệp vụ áp dụng

Đối chiếu với [Bản chốt phạm vi](../ban-chot-pham-vi.md), [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md) và [ADR 0011](../../decisions/0011-anh-dai-dien-luu-trong-postgresql.md):
- **BR-F2-01 – Kiểm tra file ảnh đại diện đầu vào (ADR 0011, S5):** Chỉ nhận nội dung JPEG/PNG hợp lệ (kiểm tra bằng nội dung file), tối đa 5 MiB, mỗi cạnh tối đa 8.000 px; đọc kích thước trước khi giải nén bitmap.
- **BR-F2-02 – Chuẩn hóa và lưu ảnh đại diện (ADR 0011):** Server giữ tỷ lệ, cạnh dài nhất tối đa 256 px, không phóng lớn ảnh nhỏ; chuyển RGB nền trắng và nén JPEG chất lượng khởi điểm 0,8, dung lượng tối đa 102.400 byte; lưu trong bảng `member_avatars` của PostgreSQL.
- **BR-F2-03 – Một hội viên một ảnh (ADR 0011, NF6):** Mỗi hội viên tối đa một bản ghi ảnh; tải lại ghi đè cùng bản ghi và cập nhật `uploaded_by` trong giao dịch có khóa dòng hội viên; không có API xóa ảnh.
- **BR-F2-04 – Phạm vi quyền ảnh đại diện (ADR 0011, S3):** Dùng chung kiểm tra phạm vi hồ sơ và ảnh; hội viên chỉ xem ảnh của chính mình và không tự đổi ảnh; trả `404` cho hồ sơ ngoài phạm vi, không tồn tại hoặc đã xóa mềm.
- **BR-F2-05 – Định dạng mã hội viên:** `HV` + 6 chữ số (`^HV[0-9]{6}$`), sinh tuần tự từ sequence `member_code_seq` dùng chung toàn chuỗi.
- **BR-F2-06 – Mỗi thông tin chỉ lưu một nơi:** Số điện thoại hội viên chỉ lưu ở `members.phone`, chuẩn hóa 10 chữ số bắt đầu bằng `0` (`^0[0-9]{9}$`), duy nhất toàn hệ thống; email hội viên chỉ lưu ở `users.email` (qua `members.user_id`), không có cột `email` trong bảng `members`.
- **BR-F2-07 – Hồ sơ chưa có tài khoản:** Lễ tân được phép tạo hồ sơ hội viên mà chưa cần tài khoản (`user_id = NULL`). Hội viên chưa có tài khoản vẫn mua gói và check-in tại quầy bình thường, nhưng chưa đăng nhập được cổng hội viên và chưa nhận thông báo email.
- **BR-F2-08 – Đồng bộ trạng thái ngừng hoạt động và kích hoạt lại (NF6):** Chuyển `members.status = 'INACTIVE'` bắt buộc chuyển `users.status = 'DISABLED'` và thu hồi mọi `refresh_tokens` đang hiệu lực trong cùng giao dịch. Chuyển `members.status = 'ACTIVE'` khôi phục `users.status = 'ACTIVE'` nếu đang `DISABLED`, không khôi phục token cũ và không gỡ khóa đăng nhập tạm (`locked_until`).
- **BR-F2-09 – Không xóa cứng:** Không dùng câu lệnh `DELETE` trên bảng `members` và `body_metrics`; sử dụng `status = 'INACTIVE'` hoặc `deleted_at`.
- **BR-F2-10 – Phân quyền xem danh sách và chi tiết hội viên theo vai trò, chi nhánh và quan hệ PT (S3, VD-26):** `TRAINER` và `MEMBER` không có quyền gọi `GET /api/v1/members`. Trên `GET /api/v1/members/{id}`, nhân viên (`BRANCH_MANAGER`, `RECEPTIONIST`) chỉ xem được hội viên thuộc chi nhánh mình (khác chi nhánh trả `404`), `SUPER_ADMIN` xem toàn chuỗi, `MEMBER` chỉ xem chính mình (người khác trả `404`), `TRAINER` chỉ xem học viên PT của mình (có ít nhất một buổi PT chưa hủy `BOOKED`, `DONE`, `NO_SHOW`, **không tính lớp nhóm**; không dạy cùng chi nhánh trả `403`, khác chi nhánh trả `404`).
- **BR-F2-11 – Tạo tài khoản cho hồ sơ có sẵn và liên kết đặt mật khẩu 72 giờ (UC-F2.2, ADR 0013):** Chỉ tạo tài khoản cho hồ sơ `ACTIVE` chưa có `user_id`; tạo `users` (`role = MEMBER`, email chữ thường, mật khẩu ngẫu nhiên băm bcrypt) và gắn `members.user_id` trong cùng giao dịch có khóa dòng; phát hành liên kết đặt mật khẩu hạn **72 giờ** qua email *"Kích hoạt tài khoản"*.
- **BR-F2-12 – Ghi nhận và xem lịch sử chỉ số cơ thể (UC-F2.5, VD-26):** Mỗi lần ghi phải có ngày đo không sau hôm nay (giờ Việt Nam) và ít nhất một chỉ số hợp lệ; `recorded_by` luôn lấy từ tài khoản đang đăng nhập (`users.id`); lịch sử sắp xếp theo `measured_at DESC, id DESC`; `MEMBER` chỉ xem của chính mình (`404` với người khác) và không tự ghi (`403`); `TRAINER` chỉ xem/ghi cho học viên PT của mình theo `BR-F2-10`.

---

## 5. Bảng dữ liệu liên quan

| Bảng / Đối tượng | Nhóm | Vai trò trong F2 |
| --- | --- | --- |
| `members` | Nhóm 3 | Lưu hồ sơ hội viên (`user_id`, `home_branch_id`, `member_code`, `full_name`, `gender`, `dob`, `phone`, `status`, `deleted_at`, `created_by`; bỏ `avatar_url` theo ADR 0011) |
| `member_avatars` | Nhóm 3 | Lưu ảnh đại diện hội viên dạng `bytea` JPEG đã thu nhỏ (`member_id`, `content`, `content_type`, `size_bytes`, `uploaded_by`) theo [ADR 0011](../../decisions/0011-anh-dai-dien-luu-trong-postgresql.md) |
| `member_code_seq` | Nhóm 3 | Sequence sinh mã hội viên tuần tự toàn chuỗi |
| `body_metrics` | Nhóm 3 | Lưu lịch sử đo chỉ số cơ thể (`member_id`, `recorded_by`, `measured_at`, `weight`, `height`, `body_fat`, `muscle_mass`, `note`) |
| `users` | Nhóm 1 | Liên kết 1–0..1 với `members` để lấy `email` và đồng bộ trạng thái `DISABLED` khi ngừng hoạt động hội viên |
| `password_resets` | Nhóm 1 | Lưu mã băm token đặt mật khẩu lần đầu hạn 72 giờ khi tạo tài khoản cho hồ sơ có sẵn (UC-F2.2) |
| `branches` | Nhóm 2 | Xác định chi nhánh gốc (`home_branch_id`) của hội viên |

---

## 6. Thiết kế API của F2 (`/api/v1/members`) – Dùng chung Backend & Frontend

| Phương thức & Đường dẫn | Quyền truy cập | Mô tả | Mã phản hồi |
| --- | --- | --- | --- |
| `GET /api/v1/members` | `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST` | Tìm kiếm & phân trang hội viên trong chi nhánh của nhân viên (`SUPER_ADMIN` xem toàn chuỗi; `TRAINER` và `MEMBER` không có quyền) | `200`, `400`, `401`, `403` |
| `GET /api/v1/members/{id}` | `SUPER_ADMIN` (toàn chuỗi); `BRANCH_MANAGER`, `RECEPTIONIST` (hội viên chi nhánh mình); `TRAINER` (chỉ học viên PT mình dạy – VD-26); `MEMBER` (chỉ chính mình) | Xem chi tiết hồ sơ hội viên (nhân viên/PT khác chi nhánh hoặc hội viên khác trả `404`; PT không dạy cùng chi nhánh trả `403`) | `200`, `401`, `403`, `404` |
| `POST /api/v1/members` | `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST` | Tạo hồ sơ hội viên mới tại quầy (không gồm ảnh đại diện; tải ảnh riêng qua `PUT /api/v1/members/{id}/avatar`) | `201`, `400`, `401`, `403`, `409` |
| `PUT /api/v1/members/{id}` | `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST` | Cập nhật thông tin hoặc trạng thái hội viên (không gồm ảnh đại diện; đổi ảnh riêng qua `PUT /api/v1/members/{id}/avatar`) | `200`, `400`, `401`, `403`, `404`, `409` |
| `GET /api/v1/members/{id}/avatar` | Cùng phạm vi quyền như `GET /api/v1/members/{id}` | Lấy ảnh đại diện hội viên (`image/jpeg` từ `member_avatars`; chưa có ảnh hoặc ngoài phạm vi trả `404`) | `200`, `400`, `401`, `403`, `404` |
| `PUT /api/v1/members/{id}/avatar` | `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST` (trong chi nhánh; `MEMBER` không tự đổi ảnh) | Tải lên/ghi đè ảnh đại diện (multipart JPEG/PNG ≤ 5 MiB, cạnh ≤ 8.000 px, server nén JPEG ≤ 100 KiB vào `member_avatars`) | `200`, `400`, `401`, `403`, `404` |
| `POST /api/v1/members/{id}/account` | `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST` | Tạo tài khoản đăng nhập cho hồ sơ hội viên `ACTIVE` chưa có tài khoản, gửi email đặt mật khẩu hạn 72 giờ | `200`, `400`, `401`, `403`, `404`, `409`, `422` |
| `GET /api/v1/members/{id}/body-metrics` | Cùng phạm vi quyền như `GET /api/v1/members/{id}` (PT chỉ xem học viên PT mình dạy – VD-26) | Danh sách lịch sử đo chỉ số cơ thể (`measured_at DESC, id DESC`) | `200`, `401`, `403`, `404` |
| `POST /api/v1/members/{id}/body-metrics` | `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST` (trong chi nhánh), `TRAINER` (học viên PT mình dạy – VD-26) | Ghi nhận lần đo chỉ số cơ thể mới | `201`, `400`, `401`, `403`, `404` |
