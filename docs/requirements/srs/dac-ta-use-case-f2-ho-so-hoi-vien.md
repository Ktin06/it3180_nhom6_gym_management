# Đặc tả Use Case – F2: Hồ sơ hội viên

- **Mã chức năng:** F2
- **Version:** v1 (Bắt buộc)
- **Người biên soạn:** Bùi Trần Đức Việt
- **Người duyệt chéo:** Thái Hùng Lân (phụ trách phát triển API F2) & Nguyễn Anh Tú (phụ trách giao diện F2)
- **Tài liệu chuẩn tham chiếu:** [Bản chốt phạm vi](../ban-chot-pham-vi.md), [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml), [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md)

---

## 1. Tổng quan chức năng

Chức năng **F2 – Hồ sơ hội viên** giúp lễ tân, quản lý chi nhánh và huấn luyện viên quản lý toàn bộ vòng đời hồ sơ hội viên tại phòng tập:
- Lễ tân tạo hồ sơ hội viên mới tại quầy (có thể tạo hồ sơ chưa có tài khoản đăng nhập hoặc tạo kèm tài khoản).
- Lễ tân kích hoạt/tạo tài khoản đăng nhập cho hồ sơ hội viên đã có sẵn (trường hợp hội viên đăng ký trực tiếp tại quầy trước đó rồi mới muốn dùng cổng hội viên).
- Tìm kiếm nhanh hội viên theo **mã hội viên (`HV` + 6 chữ số)**, **số điện thoại** hoặc **tiền tố họ tên** với thời gian phản hồi dưới 2 giây trên 10.000 hội viên (NF1).
- Cập nhật thông tin hồ sơ, ngừng hoạt động hội viên (đồng thời khóa tài khoản đăng nhập trong cùng giao dịch), và quản lý lịch sử chỉ số cơ thể (`body_metrics`).
- *Lưu ý phạm vi Sprint 1:* Cột `avatar_url` đã có trong bảng `members`; chức năng tải lên ảnh đại diện sẽ chốt nơi lưu trữ đám mây và ghép vào ở đầu Sprint 2 trước khi làm F4 (Check-in tại quầy).

---

## 2. Danh sách Use Case của F2

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F2.1** | Tạo hồ sơ hội viên tại quầy | Lễ tân, Quản lý chi nhánh | Cao | Sprint 1 |
| **UC-F2.2** | Tạo tài khoản đăng nhập cho hồ sơ hội viên có sẵn | Lễ tân, Quản lý chi nhánh | Trung bình | Sprint 1 / Sprint 2 |
| **UC-F2.3** | Tra cứu, lọc và xem chi tiết hồ sơ hội viên | Lễ tân, Quản lý chi nhánh, PT, Chủ chuỗi | Cao | Sprint 1 |
| **UC-F2.4** | Cập nhật hồ sơ & Ngừng hoạt động hội viên | Lễ tân, Quản lý chi nhánh | Cao | Sprint 1 |
| **UC-F2.5** | Ghi nhận và xem lịch sử chỉ số cơ thể (`body_metrics`) | Lễ tân, Quản lý chi nhánh, PT, Hội viên | Trung bình | Sprint 1 / Sprint 2 |

---

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F2.1: Tạo hồ sơ hội viên tại quầy

- **Tác nhân:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`). (Ngoài ra `AuthService` của F1 cũng gọi `MemberService` khi khách tự đăng ký trên web).
- **Tiền điều kiện:** Người thao tác đã đăng nhập với vai trò `RECEPTIONIST`, `BRANCH_MANAGER` (hoặc `SUPER_ADMIN`).
- **Hậu điều kiện:**
  - Bản ghi mới trong bảng `members` được tạo với:
    - `member_code` sinh tự động từ sequence `member_code_seq` theo định dạng `HV` + 6 chữ số (`HV000001`, `HV000002`, ...).
    - `phone` chuẩn hóa 10 chữ số bắt đầu bằng `0` (`^0[0-9]{9}$`), duy nhất toàn hệ thống.
    - `home_branch_id` lấy theo chi nhánh của nhân viên thao tác (từ v1–v3 là chi nhánh mặc định `CN01`).
    - `created_by` lưu `users.id` của lễ tân/quản lý thao tác.
    - `user_id` để `NULL` nếu hội viên chưa tạo tài khoản đăng nhập.
- **Luồng chính:**
  1. Lễ tân mở form "Thêm hội viên mới" và nhập: Họ và tên (`fullName`), Số điện thoại (`phone`), Giới tính (`gender`), Ngày sinh (`dob`), Ảnh đại diện (`avatarUrl` – tùy chọn).
  2. Hệ thống chuẩn hóa số điện thoại về 10 chữ số bắt đầu bằng `0` (ví dụ `+84 987 654 321` → `0987654321`) và kiểm tra định dạng bằng Bean Validation.
  3. Hệ thống kiểm tra `phone` chưa tồn tại trong bảng `members`.
  4. Hệ thống lấy số tiếp theo từ `member_code_seq` để định dạng mã `HVxxxxxx`.
  5. Hệ thống lưu bản ghi `members` với `status = 'ACTIVE'` và trả về thông tin hồ sơ hội viên (`201 Created`).
- **Luồng ngoại lệ:**
  - **E1 – Số điện thoại không đúng định dạng (400 Bad Request):** Trả `ProblemDetail` (400) báo lỗi trường `phone`.
  - **E2 – Trùng số điện thoại (409 Conflict):** Số điện thoại đã thuộc về một hội viên khác → Trả `ProblemDetail` (409): *"Số điện thoại này đã được đăng ký cho hội viên <mã hội viên>."*

---

### 3.2. UC-F2.2: Tạo tài khoản đăng nhập cho hồ sơ hội viên có sẵn

- **Tác nhân:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`).
- **Bối cảnh nghiệp vụ:** Hội viên trước đây đăng ký tập tại quầy chưa có tài khoản (`members.user_id IS NULL`), nay muốn dùng cổng hội viên trên web (hoặc khi tự đăng ký trên web bị chặn do trùng SĐT và được hướng dẫn ra quầy).
- **Tiền điều kiện:**
  - Hồ sơ hội viên tồn tại, đang ở trạng thái `ACTIVE`, và `user_id IS NULL`.
- **Hậu điều kiện:**
  - Tạo bản ghi `users` mới (`role = 'MEMBER'`, `status = 'ACTIVE'`, `email` chữ thường).
  - Gắn `members.user_id = users.id` trong cùng giao dịch.
  - Tạo mã đặt mật khẩu trong `password_resets` (hoặc gửi email đặt mật khẩu cho hội viên).
- **Luồng chính:**
  1. Lễ tân mở chi tiết hồ sơ hội viên chưa có tài khoản, chọn "Tạo tài khoản", nhập `email` của hội viên (sau khi đã đối chiếu thông tin với hội viên tại quầy).
  2. Hệ thống chuẩn hóa `email` về chữ thường và kiểm tra `email` chưa tồn tại trong bảng `users`.
  3. Hệ thống tạo bản ghi `users` (`role = 'MEMBER'`), cập nhật `members.user_id`, và khởi tạo yêu cầu đặt mật khẩu qua email.
  4. Trả về hồ sơ hội viên đã gắn `email` và `userId` (`200 OK`).
- **Luồng ngoại lệ:**
  - **E1 – Hồ sơ đã có tài khoản (409 Conflict):** `members.user_id` đã có giá trị → Trả `ProblemDetail` (409): *"Hội viên này đã có tài khoản đăng nhập."*
  - **E2 – Email đã được sử dụng (409 Conflict):** `email` đã thuộc tài khoản khác → Trả `ProblemDetail` (409): *"Email này đã được sử dụng bởi một tài khoản khác."*

---

### 3.3. UC-F2.3: Tra cứu, lọc và xem chi tiết hồ sơ hội viên

- **Tác nhân:** Lễ tân, Quản lý chi nhánh, Huấn luyện viên, Chủ chuỗi.
- **Tiền điều kiện:** Người dùng đã đăng nhập với vai trò nhân sự/quản trị.
- **Luồng chính:**
  1. Người dùng nhập từ khóa tìm kiếm (`keyword`), lọc theo trạng thái (`status`: `ACTIVE` / `INACTIVE`) và phân trang (`page`, `size`).
  2. Hệ thống xử lý tìm kiếm tối ưu hiệu năng (đảm bảo dưới 2 giây với 10.000 hội viên – NF1):
     - Nếu `keyword` có dạng mã hội viên (`HV...`): tìm chính xác theo chỉ mục duy nhất `member_code`.
     - Nếu `keyword` là chuỗi chữ số / số điện thoại: chuẩn hóa và tìm theo chỉ mục `phone`.
     - Trường hợp khác: tìm theo phần đầu của `full_name` (sử dụng chỉ mục trên `full_name`).
     - Tự động lọc theo chi nhánh của nhân viên (`staff.branch_id`), trừ `SUPER_ADMIN` xem được toàn chuỗi.
     - Không lấy các bản ghi có `deleted_at IS NOT NULL`.
  3. Hệ thống trả về `PageResponse<MemberResponse>` gồm thông tin hội viên (kèm `email` lấy từ bảng `users` qua `LEFT JOIN` nếu hội viên đã có tài khoản).
  4. Khi bấm vào một hội viên (`GET /api/v1/members/{id}`), hệ thống trả về thông tin chi tiết của hội viên đó.

---

### 3.4. UC-F2.4: Cập nhật hồ sơ & Ngừng hoạt động hội viên

- **Tác nhân:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`), Chủ chuỗi (`SUPER_ADMIN`).
- **Tiền điều kiện:** Hồ sơ hội viên tồn tại (`deleted_at IS NULL`).
- **Luồng chính – Cập nhật thông tin (`PUT /api/v1/members/{id}`):**
  1. Lễ tân sửa các thông tin: `fullName`, `phone`, `gender`, `dob`, `avatarUrl`, `status`.
  2. Nếu đổi `phone`, hệ thống chuẩn hóa về 10 chữ số bắt đầu bằng `0` và kiểm tra không trùng với hội viên khác.
  3. **Quy tắc ngừng hoạt động hội viên:** Nếu chuyển `members.status` sang `INACTIVE` và hội viên đó đã có tài khoản (`user_id IS NOT NULL`), hệ thống **bắt buộc chuyển đồng thời `users.status = 'DISABLED'` và thu hồi mọi `refresh_tokens` đang hiệu lực của tài khoản đó trong cùng giao dịch CSDL**.
  4. Nếu kích hoạt lại hội viên (`members.status = 'ACTIVE'`) và tài khoản đang là `DISABLED`, chuyển `users.status = 'ACTIVE'`.
  5. Lưu thay đổi và trả về hồ sơ sau cập nhật (`200 OK`).
- **Luồng ngoại lệ:**
  - **E1 – Không tìm thấy hội viên (404 Not Found):** Trả `ProblemDetail` (404).
  - **E2 – Số điện thoại mới bị trùng (409 Conflict):** Trả `ProblemDetail` (409).

---

### 3.5. UC-F2.5: Ghi nhận và xem lịch sử chỉ số cơ thể (`body_metrics`)

- **Tác nhân:** Huấn luyện viên (`TRAINER`), Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`), Hội viên (`MEMBER` – chỉ xem chỉ số của chính mình).
- **Luồng chính:**
  1. Nhân viên (PT/Lễ tân/Quản lý) chọn hội viên và nhập kết quả đo cơ thể: ngày đo (`measuredAt`), cân nặng kg (`weight`), chiều cao cm (`height`), tỷ lệ mỡ % (`bodyFat`), khối lượng cơ kg (`muscleMass`), ghi chú (`note`).
  2. Hệ thống gán `recorded_by = currentUserId`, lưu vào bảng `body_metrics`.
  3. Khi xem lịch sử (`GET /api/v1/members/{id}/body-metrics`), hệ thống trả danh sách sắp xếp theo `measured_at DESC` (phục vụ theo dõi ở hồ sơ F2 và biểu đồ tiến độ ở F9).

---

## 4. Quy tắc nghiệp vụ áp dụng

Đối chiếu với [Bản chốt phạm vi](../ban-chot-pham-vi.md) và [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md):
1. **Định dạng mã hội viên:** `HV` + 6 chữ số (`^HV[0-9]{6}$`), sinh từ sequence `member_code_seq` dùng chung toàn chuỗi.
2. **Mỗi thông tin chỉ lưu một nơi:**
   - Số điện thoại hội viên chỉ lưu ở `members.phone`, chuẩn hóa 10 chữ số bắt đầu bằng `0` (`^0[0-9]{9}$`), duy nhất.
   - Email hội viên chỉ lưu ở `users.email` (qua `members.user_id`), không có cột `email` trong bảng `members`.
3. **Hồ sơ chưa có tài khoản:** Lễ tân được phép tạo hồ sơ hội viên mà chưa cần tài khoản (`user_id = NULL`). Hội viên chưa có tài khoản vẫn mua gói và check-in tại quầy bình thường, nhưng chưa đăng nhập được cổng hội viên và chưa nhận thông báo email.
4. **Đồng bộ trạng thái ngừng hoạt động:** Chuyển `members.status = 'INACTIVE'` thì bắt buộc chuyển `users.status = 'DISABLED'` trong cùng một giao dịch.
5. **Không xóa cứng:** Không dùng câu lệnh `DELETE` trên bảng `members`; sử dụng `status = 'INACTIVE'` hoặc `deleted_at`.

---

## 5. Bảng dữ liệu liên quan

| Bảng / Đối tượng | Nhóm | Vai trò trong F2 |
| --- | --- | --- |
| `members` | Nhóm 3 | Lưu hồ sơ hội viên (`user_id`, `home_branch_id`, `member_code`, `full_name`, `gender`, `dob`, `phone`, `avatar_url`, `status`, `deleted_at`, `created_by`) |
| `member_code_seq` | Nhóm 3 | Sequence sinh mã hội viên tuần tự toàn chuỗi |
| `body_metrics` | Nhóm 3 | Lưu lịch sử đo chỉ số cơ thể (`member_id`, `recorded_by`, `measured_at`, `weight`, `height`, `body_fat`, `muscle_mass`, `note`) |
| `users` | Nhóm 1 | Liên kết 1–0..1 với `members` để lấy `email` và đồng bộ trạng thái `DISABLED` khi ngừng hoạt động hội viên |
| `branches` | Nhóm 2 | Xác định chi nhánh gốc (`home_branch_id`) của hội viên |

---

## 6. Thiết kế API nháp của F2 (`/api/v1/members`) – Dùng chung cho Backend (Lân) & Frontend (Anh Tú)

| Phương thức & Đường dẫn | Quyền truy cập | Mô tả | Mã phản hồi |
| --- | --- | --- | --- |
| `GET /api/v1/members` | `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST`, `TRAINER` | Tìm kiếm & phân trang hội viên (`keyword`, `status`, `page`, `size`) | `200`, `401`, `403` |
| `GET /api/v1/members/{id}` | Nhân viên hoặc chính `MEMBER` đó | Xem chi tiết hồ sơ hội viên | `200`, `403`, `404` |
| `POST /api/v1/members` | `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST` | Tạo hồ sơ hội viên mới tại quầy | `201`, `400`, `409` |
| `PUT /api/v1/members/{id}` | `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST` | Cập nhật thông tin hoặc trạng thái hội viên | `200`, `400`, `404`, `409` |
| `POST /api/v1/members/{id}/account` | `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST` | Tạo tài khoản đăng nhập cho hồ sơ hội viên có sẵn | `200`, `400`, `404`, `409` |
| `GET /api/v1/members/{id}/body-metrics` | Nhân viên hoặc chính `MEMBER` đó | Danh sách lịch sử đo chỉ số cơ thể | `200`, `403`, `404` |
| `POST /api/v1/members/{id}/body-metrics` | `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST`, `TRAINER` | Ghi nhận lần đo chỉ số cơ thể mới | `201`, `400`, `404` |

---

## 7. Tiêu chí chấp nhận (Acceptance Criteria – Given/When/Then)

1. **AC1 – Tạo hồ sơ hội viên tại quầy và tự sinh mã chuẩn:**
   - **Given** lễ tân đã đăng nhập và nhập họ tên cùng số điện thoại `+84 912 345 678` chưa có trong hệ thống,
   - **When** gửi `POST /api/v1/members`,
   - **Then** hệ thống chuẩn hóa SĐT thành `0912345678`, sinh mã `memberCode` theo mẫu `HV` + 6 chữ số (`HV000001`), gán `status = ACTIVE` và trả `201 Created`.
2. **AC2 – Chặn trùng số điện thoại khi thêm/sửa hội viên:**
   - **Given** số điện thoại `0912345678` đã thuộc về một hội viên trong hệ thống,
   - **When** lễ tân tạo hội viên mới hoặc sửa hội viên khác sang số `0912345678`,
   - **Then** hệ thống từ chối và trả `409 Conflict` dạng `ProblemDetail`.
3. **AC3 – Ngừng hoạt động hội viên tự động khóa tài khoản:**
   - **Given** hội viên đang `ACTIVE` và có tài khoản `users` liên kết đang `ACTIVE`,
   - **When** lễ tân/quản lý cập nhật trạng thái hội viên sang `INACTIVE`,
   - **Then** trong cùng giao dịch, `members.status` chuyển thành `INACTIVE`, `users.status` chuyển thành `DISABLED`, và tài khoản đó không thể đăng nhập được nữa.
4. **AC4 – Phân quyền bảo vệ dữ liệu hội viên (S3):**
   - **Given** hội viên A đang đăng nhập với vai trò `MEMBER`,
   - **When** hội viên A gọi `GET /api/v1/members/{id_cua_hoi_vien_B}` hoặc `GET /api/v1/members`,
   - **Then** hệ thống từ chối với `403 Forbidden`.
