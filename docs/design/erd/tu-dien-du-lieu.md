# Từ điển dữ liệu – ERD v2

- **Người biên soạn:** Nguyễn Anh Tú (`natus26`) · [#15](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/15) · WBS 1.3.11.
- **Cập nhật:** 12/11/2026.
- **Nguồn chuẩn:** [erd-v2.dbml](erd-v2.dbml); mục đích bảng theo [thiết kế dữ liệu](thiet-ke-du-lieu.md).
- **Phạm vi:** 39 bảng, 422 cột, 89 quan hệ, 46 khai báo chỉ mục và 25 tập giá trị Enum.
- **SHA-256 DBML (chuẩn hóa xuống dòng LF):** `a2fb52664b2f7ec4e71a98fcf330a904d2d36313b7dcc0d2b2fd546d6f7a7c6d`.

## 1. Cách đọc và cập nhật

Đây là bản xuất phục vụ tra cứu và báo cáo; khi khác nhau thì DBML là đúng. Chạy lại công cụ theo [hướng dẫn xuất bản](xuat-ban-erd.md) sau khi nguồn chuẩn thay đổi, không sửa tay các bảng dưới đây.

- Cột **Kiểu DBML** giữ nguyên khai báo nguồn. Enum dùng để vẽ; migration lưu `varchar` kèm `CHECK`, không phải kiểu Enum PostgreSQL. Độ dài `varchar` thực tế của các cột Enum xem migration đã duyệt.
- **Rỗng** cho biết được phép NULL hay không; khóa chính luôn không rỗng. `PK` là khóa chính, `FK` là khóa ngoại, `UNIQUE` là duy nhất, `DEFAULT` là mặc định, `IDENTITY` diễn giải thuộc tính `increment` của DBML.
- Ràng buộc trong ngoặc vuông, chỉ mục và ghi chú được giữ riêng: `CHECK`, chỉ mục có điều kiện và `NULLS NOT DISTINCT` mô tả trong ghi chú phải được triển khai bằng migration hoặc mã nghiệp vụ như nguồn yêu cầu; không coi DBML đã tự thực thi chúng.
- Cột **Ý nghĩa / ghi chú** có diễn giải ngắn theo tên trường, sau đó giữ ghi chú DBML nếu có. Với FK, đích tham chiếu được lấy từ khai báo nguồn. Không thêm quy tắc mới từ việc diễn giải tên.
- Thời điểm `timestamptz` lưu UTC; ngày theo nghiệp vụ Việt Nam được ghi trong ghi chú tương ứng. Tiền là số nguyên VND; số đo cơ thể, khối lượng và tỷ lệ hoa hồng giữ kiểu thập phân trong DBML.

## 2. Chỉ mục bảng theo nhóm

| Nhóm | Bảng | Số cột | Mục đích |
| --- | --- | ---: | --- |
| 1. Tài khoản và hệ thống | [`users`](#bang-users) | 10 | Tài khoản đăng nhập của mọi vai trò trừ khách |
| 1. Tài khoản và hệ thống | [`refresh_tokens`](#bang-refresh_tokens) | 7 | Phiên đăng nhập dài hạn, để làm mới token và đăng xuất |
| 1. Tài khoản và hệ thống | [`password_resets`](#bang-password_resets) | 7 | Mã đặt lại mật khẩu, dùng một lần |
| 1. Tài khoản và hệ thống | [`audit_logs`](#bang-audit_logs) | 11 | Ai làm gì, trên bảng nào, giá trị cũ và mới |
| 1. Tài khoản và hệ thống | [`notifications`](#bang-notifications) | 14 | Thông báo trong web và email |
| 1. Tài khoản và hệ thống | [`system_configs`](#bang-system_configs) | 8 | Tham số cấu hình (các giá trị có dấu \* trong bản chốt phạm vi) |
| 2. Chi nhánh và cơ sở vật chất | [`branches`](#bang-branches) | 11 | Chi nhánh; từ v1 đến v3 chỉ có một dòng (chi nhánh mặc định) |
| 2. Chi nhánh và cơ sở vật chất | [`rooms`](#bang-rooms) | 8 | Phòng trong chi nhánh |
| 2. Chi nhánh và cơ sở vật chất | [`equipment`](#bang-equipment) | 10 | Thiết bị, máy tập |
| 2. Chi nhánh và cơ sở vật chất | [`maintenance_logs`](#bang-maintenance_logs) | 10 | Báo hỏng và bảo trì |
| 3. Hội viên và gói tập | [`members`](#bang-members) | 16 | Hồ sơ hội viên |
| 3. Hội viên và gói tập | [`face_embeddings`](#bang-face_embeddings) | 7 | Vector khuôn mặt 128 chiều, không lưu ảnh |
| 3. Hội viên và gói tập | [`body_metrics`](#bang-body_metrics) | 11 | Chỉ số cơ thể theo từng lần đo |
| 3. Hội viên và gói tập | [`packages`](#bang-packages) | 12 | Danh mục gói đang bán (bảng giá) |
| 3. Hội viên và gói tập | [`memberships`](#bang-memberships) | 16 | Gói một hội viên đã mua; bảng trung tâm |
| 3. Hội viên và gói tập | [`membership_freezes`](#bang-membership_freezes) | 11 | Yêu cầu bảo lưu và kết quả duyệt |
| 3. Hội viên và gói tập | [`membership_transfers`](#bang-membership_transfers) | 11 | Lần chuyển nhượng gói |
| 4. Check-in và lớp học | [`checkins`](#bang-checkins) | 13 | Mỗi lượt vào phòng tập |
| 4. Check-in và lớp học | [`class_types`](#bang-class_types) | 6 | Loại lớp: Yoga, Zumba, Boxing… |
| 4. Check-in và lớp học | [`class_schedules`](#bang-class_schedules) | 14 | Lịch lặp hằng tuần |
| 4. Check-in và lớp học | [`class_sessions`](#bang-class_sessions) | 13 | Từng buổi cụ thể sinh ra từ lịch lặp; hội viên đặt chỗ vào buổi |
| 4. Check-in và lớp học | [`class_bookings`](#bang-class_bookings) | 17 | Đặt chỗ và danh sách chờ, gộp một bảng |
| 5. Huấn luyện viên | [`trainer_availabilities`](#bang-trainer_availabilities) | 8 | Khung giờ rảnh theo thứ trong tuần |
| 5. Huấn luyện viên | [`pt_sessions`](#bang-pt_sessions) | 15 | Buổi tập 1-1 đã đặt |
| 5. Huấn luyện viên | [`workout_plans`](#bang-workout_plans) | 9 | Giáo án huấn luyện viên soạn cho hội viên |
| 5. Huấn luyện viên | [`workout_logs`](#bang-workout_logs) | 10 | Nhật ký tập |
| 6. Bán hàng và thanh toán | [`products`](#bang-products) | 8 | Hàng bán tại quầy (danh mục chung toàn chuỗi) |
| 6. Bán hàng và thanh toán | [`inventories`](#bang-inventories) | 8 | Tồn kho theo chi nhánh |
| 6. Bán hàng và thanh toán | [`stock_movements`](#bang-stock_movements) | 11 | Nhập, xuất, điều chỉnh kho |
| 6. Bán hàng và thanh toán | [`promotions`](#bang-promotions) | 15 | Mã giảm giá |
| 6. Bán hàng và thanh toán | [`invoices`](#bang-invoices) | 18 | Hóa đơn |
| 6. Bán hàng và thanh toán | [`invoice_items`](#bang-invoice_items) | 12 | Dòng hóa đơn |
| 6. Bán hàng và thanh toán | [`payments`](#bang-payments) | 10 | Mỗi lần thu tiền |
| 7. Nhân sự | [`staff`](#bang-staff) | 14 | Hồ sơ nhân viên; chức vụ lấy từ `users.role`; chủ chuỗi không có dòng ở bảng này |
| 7. Nhân sự | [`shifts`](#bang-shifts) | 8 | Ca làm đã xếp |
| 7. Nhân sự | [`attendances`](#bang-attendances) | 8 | Chấm công thực tế theo ca |
| 7. Nhân sự | [`payrolls`](#bang-payrolls) | 11 | Bảng lương tháng |
| 8. Gamification | [`badges`](#bang-badges) | 8 | Danh mục huy hiệu và điều kiện đạt |
| 8. Gamification | [`member_badges`](#bang-member_badges) | 6 | Huy hiệu hội viên đã đạt |

## 3. Chi tiết cột, ràng buộc và chỉ mục

### Nhóm 1. Tài khoản và hệ thống

<a id="bang-users"></a>

#### `users`

Tài khoản đăng nhập của mọi vai trò trừ khách. [Khai báo nguồn](erd-v2.dbml#L163).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `email` | `varchar(255)` | Không | UNIQUE | Địa chỉ email<br>DBML: tên đăng nhập (chỉ đăng nhập bằng email) và địa chỉ nhận email thông báo; nơi duy nhất lưu email (chốt 31/10). Lưu chữ thường, CHECK email = lower(email) |
| `password_hash` | `varchar(255)` | Không | — | Mã băm mật khẩu<br>DBML: bcrypt (S1) |
| `role` | `user_role` | Không | [Tập giá trị](#enum-user_role) | Vai trò tài khoản<br>DBML: nguồn duy nhất của vai trò; chi nhánh làm việc lấy từ staff.branch_id |
| `status` | `user_status` | Không | DEFAULT `'ACTIVE'`; [Tập giá trị](#enum-user_status) | Trạng thái bản ghi<br>DBML: LOCKED = quản trị viên khóa tay, đến khi mở lại; DISABLED = hội viên hoặc nhân viên ngừng hoạt động. Khóa tạm do sai mật khẩu (S4) không đổi cột này mà dùng locked_until |
| `failed_login_count` | `int` | Không | DEFAULT `0` | Số lần đăng nhập sai liên tiếp<br>DBML: S4: khóa 15 phút sau 5 lần sai, CHECK failed_login_count &gt;= 0 |
| `locked_until` | `timestamptz` | Có | — | Thời điểm kết thúc khóa đăng nhập tạm<br>DBML: khóa tạm theo S4; hết thời điểm này thì tự đăng nhập lại được |
| `last_login_at` | `timestamptz` | Có | — | Thời điểm đăng nhập gần nhất |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

<a id="bang-refresh_tokens"></a>

#### `refresh_tokens`

Phiên đăng nhập dài hạn, để làm mới token và đăng xuất. [Khai báo nguồn](erd-v2.dbml#L176).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `user_id` | `bigint` | Không | FK → `users.id` | Tham chiếu users.id |
| `token_hash` | `varchar(255)` | Không | UNIQUE | Mã băm token |
| `expires_at` | `timestamptz` | Không | — | Thời điểm hết hạn |
| `revoked_at` | `timestamptz` | Có | — | Thời điểm thu hồi token |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `user_id` | Không | thu hồi mọi phiên khi đổi mật khẩu hoặc khóa tài khoản |

<a id="bang-password_resets"></a>

#### `password_resets`

Mã đặt lại mật khẩu, dùng một lần. [Khai báo nguồn](erd-v2.dbml#L189).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `user_id` | `bigint` | Không | FK → `users.id` | Tham chiếu users.id |
| `token_hash` | `varchar(255)` | Không | UNIQUE | Mã băm token |
| `expires_at` | `timestamptz` | Không | — | Thời điểm hết hạn |
| `used_at` | `timestamptz` | Có | — | Thời điểm sử dụng token |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

<a id="bang-audit_logs"></a>

#### `audit_logs`

Ai làm gì, trên bảng nào, giá trị cũ và mới. [Khai báo nguồn](erd-v2.dbml#L199).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `user_id` | `bigint` | Có | FK → `users.id` | Tham chiếu users.id<br>DBML: rỗng nếu do tác vụ nền của hệ thống |
| `branch_id` | `bigint` | Có | FK → `branches.id` | Tham chiếu branches.id |
| `action` | `varchar(50)` | Không | — | Loại thao tác được ghi nhận<br>DBML: CREATE, UPDATE, DELETE, REFUND, CHANGE_ROLE, ... |
| `entity` | `varchar(50)` | Không | — | Tên bảng được tác động<br>DBML: tên bảng, ví dụ packages |
| `entity_id` | `bigint` | Có | — | Định danh bản ghi được tác động |
| `old_value` | `jsonb` | Có | — | Dữ liệu trước thay đổi |
| `new_value` | `jsonb` | Có | — | Dữ liệu sau thay đổi |
| `ip` | `varchar(45)` | Có | — | Địa chỉ IP của thao tác |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `entity`, `entity_id` | Không | — |
| Không đặt tên | `created_at` | Không | — |
| Không đặt tên | `branch_id`, `created_at` | Không | xem nhật ký theo chi nhánh |

<a id="bang-notifications"></a>

#### `notifications`

Thông báo trong web và email. [Khai báo nguồn](erd-v2.dbml#L218).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `user_id` | `bigint` | Không | FK → `users.id` | Tham chiếu users.id |
| `type` | `varchar(50)` | Không | — | Loại bản ghi<br>DBML: CLASS_PROMOTED, CLASS_RESCHEDULED, CLASS_CANCELLED, CLASS_REMINDER, MEMBERSHIP_EXPIRING, ... |
| `title` | `varchar(255)` | Không | — | Tiêu đề |
| `content` | `text` | Có | — | Nội dung |
| `channel` | `notification_channel` | Không | [Tập giá trị](#enum-notification_channel) | Kênh gửi thông báo |
| `related_entity` | `varchar(50)` | Có | — | Loại đối tượng liên quan<br>DBML: ví dụ class_bookings, để bấm vào thông báo mở đúng màn hình |
| `related_id` | `bigint` | Có | — | Định danh đối tượng liên quan |
| `status` | `notification_status` | Không | DEFAULT `'PENDING'`; [Tập giá trị](#enum-notification_status) | Trạng thái bản ghi |
| `scheduled_at` | `timestamptz` | Có | — | Thời điểm dự kiến gửi<br>DBML: email ngoài giờ mở cửa được hẹn tới giờ mở cửa |
| `sent_at` | `timestamptz` | Có | — | Thời điểm đã gửi |
| `read_at` | `timestamptz` | Có | — | Thời điểm đã đọc |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `user_id`, `read_at` | Không | — |
| Không đặt tên | `status`, `scheduled_at` | Không | tác vụ nền quét email cần gửi |

<a id="bang-system_configs"></a>

#### `system_configs`

Tham số cấu hình (các giá trị có dấu \* trong bản chốt phạm vi). [Khai báo nguồn](erd-v2.dbml#L239).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `branch_id` | `bigint` | Có | FK → `branches.id` | Tham chiếu branches.id<br>DBML: rỗng = áp dụng toàn chuỗi; giá trị của chi nhánh ghi đè giá trị toàn chuỗi |
| `config_key` | `varchar(100)` | Không | — | Khóa cấu hình<br>DBML: BOOKING_WINDOW_DAYS=7, PROMOTION_CUTOFF_MIN=30, PROMOTION_GRACE_MIN=15, RECONFIRM_TTL_MIN=60, LATE_CANCEL_HOURS=2, PENALTY_LIMIT_PER_MONTH=3, PENALTY_LOCK_DAYS=7, PT_MIN_LEAD_HOURS=12, FREEZE_MIN_DAYS=7, FREEZE_MAX_DAYS=30, FREEZE_MAX_TIMES=2, TRANSFER_MIN_DAYS=30, TRANSFER_FEE=200000, PENDING_INVOICE_TTL_DAYS=3, SESSION_WARNING_THRESHOLD=2 |
| `config_value` | `varchar(255)` | Không | — | Giá trị cấu hình |
| `description` | `varchar(255)` | Có | — | Mô tả |
| `updated_by` | `bigint` | Có | FK → `users.id` | Tham chiếu users.id |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `branch_id`, `config_key` | Có | UNIQUE NULLS NOT DISTINCT, để mỗi config_key toàn chuỗi (branch_id rỗng) cũng chỉ có một dòng |

### Nhóm 2. Chi nhánh và cơ sở vật chất

<a id="bang-branches"></a>

#### `branches`

Chi nhánh; từ v1 đến v3 chỉ có một dòng (chi nhánh mặc định). [Khai báo nguồn](erd-v2.dbml#L256).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `code` | `varchar(20)` | Không | UNIQUE | Mã định danh nghiệp vụ<br>DBML: ví dụ CN01 |
| `name` | `varchar(255)` | Không | — | Tên |
| `address` | `varchar(255)` | Có | — | Địa chỉ |
| `phone` | `varchar(20)` | Có | — | Số điện thoại |
| `open_time` | `time` | Không | — | Giờ mở cửa<br>DBML: giờ mở cửa; check-in và email chỉ trong khung open_time–close_time |
| `close_time` | `time` | Không | — | Giờ đóng cửa<br>DBML: CHECK close_time &gt; open_time |
| `capacity` | `int` | Có | — | Sức chứa hoặc sĩ số tối đa<br>DBML: CHECK capacity IS NULL OR capacity &gt; 0 |
| `status` | `active_status` | Không | DEFAULT `'ACTIVE'`; [Tập giá trị](#enum-active_status) | Trạng thái bản ghi |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

<a id="bang-rooms"></a>

#### `rooms`

Phòng trong chi nhánh. [Khai báo nguồn](erd-v2.dbml#L270).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id |
| `name` | `varchar(100)` | Không | — | Tên |
| `type` | `varchar(50)` | Có | — | Loại bản ghi<br>DBML: GYM_FLOOR, STUDIO, PT_ROOM |
| `capacity` | `int` | Không | — | Sức chứa hoặc sĩ số tối đa<br>DBML: CHECK capacity &gt; 0 |
| `status` | `active_status` | Không | DEFAULT `'ACTIVE'`; [Tập giá trị](#enum-active_status) | Trạng thái bản ghi |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `branch_id`, `name` | Có | — |

<a id="bang-equipment"></a>

#### `equipment`

Thiết bị, máy tập. [Khai báo nguồn](erd-v2.dbml#L284).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id |
| `room_id` | `bigint` | Có | FK → `rooms.id` | Tham chiếu rooms.id |
| `code` | `varchar(50)` | Có | UNIQUE | Mã định danh nghiệp vụ<br>DBML: mã tài sản dán trên máy |
| `name` | `varchar(255)` | Không | — | Tên |
| `type` | `varchar(50)` | Có | — | Loại bản ghi |
| `status` | `equipment_status` | Không | DEFAULT `'GOOD'`; [Tập giá trị](#enum-equipment_status) | Trạng thái bản ghi |
| `purchased_at` | `date` | Có | — | Ngày mua thiết bị |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

<a id="bang-maintenance_logs"></a>

#### `maintenance_logs`

Báo hỏng và bảo trì. [Khai báo nguồn](erd-v2.dbml#L297).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `equipment_id` | `bigint` | Không | FK → `equipment.id` | Tham chiếu equipment.id |
| `reported_by` | `bigint` | Không | FK → `users.id` | Tham chiếu users.id |
| `issue` | `text` | Không | — | Nội dung báo hỏng |
| `status` | `maintenance_status` | Không | DEFAULT `'REPORTED'`; [Tập giá trị](#enum-maintenance_status) | Trạng thái bản ghi |
| `scheduled_date` | `date` | Có | — | Ngày dự kiến bảo trì |
| `completed_date` | `date` | Có | — | Ngày hoàn thành bảo trì |
| `cost` | `int` | Có | — | Chi phí bảo trì (VND)<br>DBML: VND, CHECK cost &gt;= 0 |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `equipment_id` | Không | xem lịch sử bảo trì của một thiết bị (F11) |

### Nhóm 3. Hội viên và gói tập

<a id="bang-members"></a>

#### `members`

Hồ sơ hội viên. [Khai báo nguồn](erd-v2.dbml#L316).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `user_id` | `bigint` | Có | UNIQUE; FK → `users.id` (1–1) | Tham chiếu users.id<br>DBML: rỗng nếu lễ tân tạo hồ sơ mà hội viên chưa có tài khoản |
| `home_branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id |
| `member_code` | `varchar(20)` | Không | UNIQUE | Mã hội viên<br>DBML: dùng để check-in (F4). Định dạng HV + 6 chữ số (HV000123), sinh từ sequence member_code_seq dùng chung cả chuỗi, CHECK member_code ~ '^HV[0-9]{6}$' (chốt 01/11) |
| `full_name` | `varchar(255)` | Không | — | Họ và tên |
| `gender` | `varchar(10)` | Có | — | Giới tính |
| `dob` | `date` | Có | — | Ngày sinh |
| `phone` | `varchar(20)` | Không | UNIQUE | Số điện thoại<br>DBML: nơi duy nhất lưu số điện thoại hội viên; duy nhất vì F4 tìm theo số điện thoại. Lưu dạng chuẩn 10 chữ số bắt đầu bằng 0, CHECK phone ~ '^0[0-9]{9}$'. Email lấy từ users.email qua user_id. Tự đăng ký bằng số đã có hồ sơ thì bị từ chối, lễ tân tạo tài khoản cho hồ sơ có sẵn (chốt 01/11) |
| `avatar_url` | `varchar(500)` | Có | — | Đường dẫn ảnh đại diện |
| `face_consent_at` | `timestamptz` | Có | — | Thời điểm đồng ý sử dụng khuôn mặt<br>DBML: thời điểm đồng ý dùng khuôn mặt (S8); rỗng = chưa đồng ý |
| `booking_locked_until` | `timestamptz` | Có | — | Thời điểm kết thúc khóa đặt lớp<br>DBML: khóa đặt lớp do hủy muộn hoặc vắng mặt |
| `status` | `active_status` | Không | DEFAULT `'ACTIVE'`; [Tập giá trị](#enum-active_status) | Trạng thái bản ghi<br>DBML: chuyển INACTIVE thì đồng thời chuyển users.status = DISABLED trong cùng giao dịch |
| `deleted_at` | `timestamptz` | Có | — | Thời điểm xóa mềm |
| `created_by` | `bigint` | Có | FK → `users.id` | Tham chiếu users.id |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| `ix_members_full_name` | `lower(full_name) varchar_pattern_ops` | Không | Tối ưu cho truy vấn LIKE theo chữ thường |

<a id="bang-face_embeddings"></a>

#### `face_embeddings`

Vector khuôn mặt 128 chiều, không lưu ảnh. [Khai báo nguồn](erd-v2.dbml#L338).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `member_id` | `bigint` | Không | FK → `members.id` | Tham chiếu members.id |
| `embedding` | `real[]` | Không | — | Vector đặc trưng khuôn mặt<br>DBML: vector 128 chiều do face-api.js trích xuất, CHECK cardinality(embedding) = 128; so khớp bằng khoảng cách Euclid; không lưu ảnh gốc (F20) |
| `model_version` | `varchar(50)` | Có | — | Phiên bản mô hình nhận diện<br>DBML: phiên bản mô hình face-api.js, để biết khi nào phải đăng ký lại |
| `created_by` | `bigint` | Có | FK → `users.id` | Tham chiếu users.id |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `member_id` | Không | — |

**Ghi chú cấp bảng từ DBML:** Ngoại lệ của quy tắc không xóa cứng (S8): hội viên rút lại đồng ý thì xóa thật mọi dòng của họ và đặt members.face_consent_at = rỗng trong cùng giao dịch; nhật ký hoạt động chỉ ghi việc xóa, không ghi vector.

<a id="bang-body_metrics"></a>

#### `body_metrics`

Chỉ số cơ thể theo từng lần đo. [Khai báo nguồn](erd-v2.dbml#L352).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `member_id` | `bigint` | Không | FK → `members.id` | Tham chiếu members.id |
| `recorded_by` | `bigint` | Có | FK → `users.id` | Tham chiếu users.id |
| `measured_at` | `date` | Không | — | Ngày đo chỉ số cơ thể |
| `weight` | `decimal(5,2)` | Có | — | Khối lượng (kg)<br>DBML: kg. CHECK weight &gt; 0 |
| `height` | `decimal(5,2)` | Có | — | Chiều cao (cm)<br>DBML: cm. CHECK height &gt; 0 |
| `body_fat` | `decimal(4,2)` | Có | — | Tỷ lệ mỡ cơ thể (%)<br>DBML: %. CHECK body_fat &gt;= 0 AND body_fat &lt;= 100 |
| `muscle_mass` | `decimal(5,2)` | Có | — | Khối lượng cơ (kg)<br>DBML: kg |
| `note` | `text` | Có | — | Ghi chú |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `member_id`, `measured_at` | Không | — |

<a id="bang-packages"></a>

#### `packages`

Danh mục gói đang bán (bảng giá). [Khai báo nguồn](erd-v2.dbml#L369).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `branch_id` | `bigint` | Có | FK → `branches.id` | Tham chiếu branches.id<br>DBML: rỗng = dùng toàn chuỗi |
| `code` | `varchar(30)` | Không | UNIQUE | Mã định danh nghiệp vụ |
| `name` | `varchar(255)` | Không | — | Tên |
| `type` | `package_type` | Không | [Tập giá trị](#enum-package_type) | Loại bản ghi |
| `duration_days` | `int` | Không | — | Số ngày hiệu lực của gói<br>DBML: số ngày hiệu lực, CHECK &gt; 0; gói theo buổi và gói PT vẫn có hạn dùng. Gói TIME "1 tháng" = 30 ngày; quản lý nhập số ngày khi tạo gói (chốt 01/11) |
| `session_count` | `int` | Có | — | Số buổi của gói trong danh mục<br>DBML: bắt buộc với SESSION và PT, rỗng với TIME (CHECK) |
| `price` | `int` | Không | — | Giá bán (VND)<br>DBML: VND, CHECK price &gt;= 0 |
| `description` | `text` | Có | — | Mô tả |
| `status` | `active_status` | Không | DEFAULT `'ACTIVE'`; [Tập giá trị](#enum-active_status) | Trạng thái bản ghi<br>DBML: INACTIVE = ngừng bán, không xóa |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

<a id="bang-memberships"></a>

#### `memberships`

Gói một hội viên đã mua; bảng trung tâm. [Khai báo nguồn](erd-v2.dbml#L384).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `member_id` | `bigint` | Không | FK → `members.id` | Tham chiếu members.id |
| `package_id` | `bigint` | Không | FK → `packages.id` | Tham chiếu packages.id |
| `package_type` | `package_type` | Không | [Tập giá trị](#enum-package_type) | Loại gói đã mua<br>DBML: chép từ packages.type để làm chỉ mục duy nhất bên dưới |
| `branch_id` | `bigint` | Có | FK → `branches.id` | Tham chiếu branches.id<br>DBML: chi nhánh được dùng; rỗng = toàn chuỗi (chép từ packages.branch_id) |
| `sold_branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id<br>DBML: chi nhánh bán, dùng cho báo cáo doanh thu |
| `invoice_item_id` | `bigint` | Có | FK → `invoice_items.id` | Tham chiếu invoice_items.id<br>DBML: dòng hóa đơn tạo ra gói; rỗng với gói nhận chuyển nhượng |
| `start_date` | `date` | Không | — | Ngày bắt đầu<br>DBML: gói PENDING mang ngày dự kiến; tính lại lúc hóa đơn chuyển PAID: ngày sau ngày gói cũ cùng loại hết hiệu lực (gói SESSION cũ đã về 0 buổi thì là ngày mai). Gói UPCOMING nối sau gói SESSION được kéo về ngày mai khi gói SESSION bị trừ buổi cuối, trong cùng giao dịch check-in; gói UPCOMING nối sau gói PT được kéo về hôm nay và kích hoạt khi buổi PT cuối được xác nhận (chốt 03/11) |
| `end_date` | `date` | Không | — | Ngày kết thúc<br>DBML: ngày cuối còn dùng được: start_date + packages.duration_days - 1; CHECK end_date &gt;= start_date; cộng thêm số ngày bảo lưu. Khi bảo lưu được duyệt, gói UPCOMING cùng loại nối tiếp phía sau cũng được dời start_date và end_date thêm đúng số ngày đó, trong cùng giao dịch (chốt 01/11) |
| `total_sessions` | `int` | Có | — | Tổng số buổi của gói đã mua<br>DBML: chép từ packages.session_count lúc mua |
| `remaining_sessions` | `int` | Có | — | Số buổi còn lại<br>DBML: CHECK remaining_sessions &gt;= 0. CHECK ((package_type = 'TIME' AND total_sessions IS NULL AND remaining_sessions IS NULL) OR (package_type IN ('SESSION', 'PT') AND total_sessions IS NOT NULL AND remaining_sessions IS NOT NULL AND remaining_sessions &lt;= total_sessions)) |
| `freeze_count` | `int` | Không | DEFAULT `0` | Số lần bảo lưu |
| `status` | `membership_status` | Không | DEFAULT `'PENDING'`; [Tập giá trị](#enum-membership_status) | Trạng thái bản ghi |
| `version` | `int` | Không | DEFAULT `0` | Phiên bản dùng kiểm soát cập nhật đồng thời<br>DBML: khóa lạc quan (@Version): trừ buổi, bảo lưu, kích hoạt |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `member_id`, `status` | Không | — |
| Không đặt tên | `end_date` | Không | tác vụ nền chạy lúc 00:00 giờ Việt Nam, và chạy bù mỗi khi máy chủ khởi động: chuyển EXPIRED gói ACTIVE có end_date &lt; hôm nay, và gói SESSION ACTIVE có remaining_sessions = 0 mà hôm nay chưa có lượt check-in trừ buổi; chạy trước tác vụ kích hoạt UPCOMING |
| `ix_memberships_upcoming` | `start_date` | Không | chỉ mục có điều kiện WHERE status = UPCOMING: tác vụ nền chuyển UPCOMING thành ACTIVE |
| `uq_one_active_per_type` | `member_id`, `package_type`, `branch_id` | Có | chỉ mục duy nhất có điều kiện, NULLS NOT DISTINCT (gói toàn chuỗi có branch_id rỗng): WHERE status IN (ACTIVE, FROZEN) |
| Không đặt tên | `package_id` | Không | — |
| Không đặt tên | `invoice_item_id` | Không | tìm gói cần kích hoạt khi hóa đơn chuyển PAID |

<a id="bang-membership_freezes"></a>

#### `membership_freezes`

Yêu cầu bảo lưu và kết quả duyệt. [Khai báo nguồn](erd-v2.dbml#L413).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `membership_id` | `bigint` | Không | FK → `memberships.id` | Tham chiếu memberships.id |
| `requested_by` | `bigint` | Không | FK → `users.id` | Tham chiếu users.id |
| `approved_by` | `bigint` | Có | FK → `users.id` | Tham chiếu users.id<br>DBML: quản lý chi nhánh |
| `from_date` | `date` | Không | — | Ngày bắt đầu bảo lưu |
| `to_date` | `date` | Không | — | Ngày kết thúc bảo lưu<br>DBML: CHECK to_date &gt;= from_date. CHECK số ngày trong khoảng FREEZE_MIN_DAYS–FREEZE_MAX_DAYS (kiểm tra trong code) |
| `reason` | `text` | Có | — | Lý do yêu cầu |
| `status` | `freeze_status` | Không | DEFAULT `'PENDING'`; [Tập giá trị](#enum-freeze_status) | Trạng thái bản ghi<br>DBML: APPROVED thì cộng số ngày vào end_date của gói, và dời gói UPCOMING cùng loại nối tiếp phía sau |
| `decided_at` | `timestamptz` | Có | — | Thời điểm ra quyết định |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `membership_id` | Không | — |

<a id="bang-membership_transfers"></a>

#### `membership_transfers`

Lần chuyển nhượng gói. [Khai báo nguồn](erd-v2.dbml#L430).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `from_membership_id` | `bigint` | Không | FK → `memberships.id` | Tham chiếu memberships.id<br>DBML: gói cũ chuyển sang TRANSFERRED |
| `to_membership_id` | `bigint` | Có | FK → `memberships.id` | Tham chiếu memberships.id<br>DBML: gói mới tạo cho người nhận, giữ nguyên ngày hết hạn và số buổi còn lại; rỗng khi còn PENDING |
| `from_member_id` | `bigint` | Không | FK → `members.id` | Tham chiếu members.id |
| `to_member_id` | `bigint` | Không | FK → `members.id` | Tham chiếu members.id<br>DBML: CHECK khác from_member_id |
| `invoice_id` | `bigint` | Có | FK → `invoices.id` | Tham chiếu invoices.id<br>DBML: hóa đơn thu phí chuyển nhượng |
| `fee` | `int` | Không | — | Phí chuyển nhượng (VND)<br>DBML: VND |
| `status` | `transfer_status` | Không | DEFAULT `'PENDING'`; [Tập giá trị](#enum-transfer_status) | Trạng thái bản ghi<br>DBML: hóa đơn phí PAID thì chuyển COMPLETED: gói cũ TRANSFERRED, tạo gói mới, trong cùng giao dịch. Hóa đơn bị hủy thì CANCELLED, gói cũ giữ nguyên. CHECK status = COMPLETED khi và chỉ khi to_membership_id có giá trị |
| `approved_by` | `bigint` | Có | FK → `users.id` | Tham chiếu users.id |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

### Nhóm 4. Check-in và lớp học

<a id="bang-checkins"></a>

#### `checkins`

Mỗi lượt vào phòng tập. [Khai báo nguồn](erd-v2.dbml#L447).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `member_id` | `bigint` | Không | FK → `members.id` | Tham chiếu members.id<br>DBML: phải trùng memberships.member_id của gói dùng (kiểm tra trong code) |
| `membership_id` | `bigint` | Không | FK → `memberships.id` | Tham chiếu memberships.id<br>DBML: chỉ gói TIME hoặc SESSION đang ACTIVE; gói PT không dùng để check-in. Có cả hai thì dùng gói TIME, không trừ buổi (chốt 01/11) |
| `branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id |
| `created_by` | `bigint` | Không | FK → `users.id` | Tham chiếu users.id<br>DBML: lễ tân thực hiện |
| `method` | `checkin_method` | Không | DEFAULT `'MANUAL'`; [Tập giá trị](#enum-checkin_method) | Phương thức thực hiện |
| `match_score` | `decimal(5,4)` | Có | — | Kết quả so khớp khuôn mặt<br>DBML: chỉ có khi method = FACE. CHECK method = 'FACE' OR match_score IS NULL |
| `checkin_date` | `date` | Không | — | Ngày check-in theo giờ Việt Nam<br>DBML: ngày theo giờ Việt Nam, để xác định lần check-in đầu tiên trong ngày |
| `session_deducted` | `boolean` | Không | DEFAULT `false` | Lượt check-in có trừ buổi hay không<br>DBML: true nếu lượt này trừ 1 buổi của gói theo buổi |
| `checked_in_at` | `timestamptz` | Không | — | Thời điểm vào phòng tập |
| `checked_out_at` | `timestamptz` | Có | — | Thời điểm ra khỏi phòng tập |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `branch_id`, `checked_in_at` | Không | dashboard theo ngày, theo giờ (F6) |
| Không đặt tên | `member_id`, `checked_in_at` | Không | — |
| `uq_one_deduction_per_day` | `membership_id`, `checkin_date` | Có | chỉ mục duy nhất có điều kiện: WHERE session_deducted = true |

<a id="bang-class_types"></a>

#### `class_types`

Loại lớp: Yoga, Zumba, Boxing…. [Khai báo nguồn](erd-v2.dbml#L468).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `name` | `varchar(100)` | Không | UNIQUE | Tên<br>DBML: Yoga, Zumba, Boxing... |
| `description` | `text` | Có | — | Mô tả |
| `status` | `active_status` | Không | DEFAULT `'ACTIVE'`; [Tập giá trị](#enum-active_status) | Trạng thái bản ghi |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

<a id="bang-class_schedules"></a>

#### `class_schedules`

Lịch lặp hằng tuần. [Khai báo nguồn](erd-v2.dbml#L477).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id |
| `class_type_id` | `bigint` | Không | FK → `class_types.id` | Tham chiếu class_types.id |
| `trainer_id` | `bigint` | Không | FK → `staff.id` | Tham chiếu staff.id<br>DBML: tài khoản của nhân viên này phải có role = TRAINER (kiểm tra trong code) |
| `room_id` | `bigint` | Không | FK → `rooms.id` | Tham chiếu rooms.id |
| `day_of_week` | `int` | Không | — | Thứ trong tuần theo ISO<br>DBML: 1 = thứ Hai ... 7 = Chủ nhật (theo ISO), CHECK 1..7 |
| `start_time` | `time` | Không | — | Giờ bắt đầu |
| `end_time` | `time` | Không | — | Giờ kết thúc<br>DBML: CHECK end_time &gt; start_time |
| `capacity` | `int` | Không | — | Sức chứa hoặc sĩ số tối đa<br>DBML: CHECK 0 &lt; capacity &lt;= sức chứa phòng (kiểm tra trong code) |
| `valid_from` | `date` | Không | — | Ngày bắt đầu áp dụng lịch |
| `valid_to` | `date` | Có | — | Ngày kết thúc áp dụng lịch |
| `status` | `active_status` | Không | DEFAULT `'ACTIVE'`; [Tập giá trị](#enum-active_status) | Trạng thái bản ghi |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| `ix_class_schedules_branch_status` | `branch_id`, `status` | Không | — |

<a id="bang-class_sessions"></a>

#### `class_sessions`

Từng buổi cụ thể sinh ra từ lịch lặp; hội viên đặt chỗ vào buổi. [Khai báo nguồn](erd-v2.dbml#L497).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `class_schedule_id` | `bigint` | Không | FK → `class_schedules.id` | Tham chiếu class_schedules.id |
| `branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id |
| `trainer_id` | `bigint` | Không | FK → `staff.id` | Tham chiếu staff.id<br>DBML: chép từ lịch lặp; đổi được khi có PT dạy thay |
| `room_id` | `bigint` | Không | FK → `rooms.id` | Tham chiếu rooms.id |
| `occurrence_date` | `date` | Không | — | Ngày của buổi theo lịch lặp khi sinh<br>DBML: ngày (giờ Việt Nam) của buổi theo lịch lặp lúc sinh ra; không bao giờ đổi, kể cả khi đổi giờ |
| `start_at` | `timestamptz` | Không | — | Thời điểm bắt đầu |
| `end_at` | `timestamptz` | Không | — | Thời điểm kết thúc |
| `original_start_at` | `timestamptz` | Có | — | Thời điểm bắt đầu trước khi đổi giờ<br>DBML: giờ cũ khi đổi giờ, để ghi vào thông báo |
| `capacity` | `int` | Không | — | Sức chứa hoặc sĩ số tối đa |
| `status` | `session_status` | Không | DEFAULT `'SCHEDULED'`; [Tập giá trị](#enum-session_status) | Trạng thái bản ghi |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `class_schedule_id`, `occurrence_date` | Có | không sinh trùng một buổi hai lần, kể cả khi buổi đã đổi giờ (start_at đổi nhưng occurrence_date giữ nguyên) |
| Không đặt tên | `branch_id`, `start_at` | Không | — |
| Không đặt tên | `trainer_id`, `start_at` | Không | kiểm tra trùng giờ với buổi PT |

<a id="bang-class_bookings"></a>

#### `class_bookings`

Đặt chỗ và danh sách chờ, gộp một bảng. [Khai báo nguồn](erd-v2.dbml#L518).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `class_session_id` | `bigint` | Không | FK → `class_sessions.id` | Tham chiếu class_sessions.id |
| `member_id` | `bigint` | Không | FK → `members.id` | Tham chiếu members.id |
| `created_by` | `bigint` | Không | FK → `users.id` | Tham chiếu users.id<br>DBML: tài khoản đã bấm đặt hoặc vào chờ (hội viên, hoặc lễ tân nếu thao tác hộ); được tự động đưa vào lớp thì giữ nguyên người tạo ban đầu (ADR 0010) |
| `status` | `booking_status` | Không | [Tập giá trị](#enum-booking_status) | Trạng thái bản ghi<br>DBML: chỉ được đặt chỗ hoặc vào chờ khi có gói TIME đang ACTIVE dùng được tại chi nhánh; gói SESSION và PT không dùng cho lớp nhóm (chốt 31/10) |
| `waitlisted_at` | `timestamptz` | Có | — | Thời điểm vào danh sách chờ<br>DBML: căn cứ thứ tự chờ: đến trước, xếp trước; trùng thời điểm thì id nhỏ hơn đứng trước (ADR 0010) |
| `booked_at` | `timestamptz` | Có | — | Thời điểm có chỗ đặt |
| `promoted_at` | `timestamptz` | Có | — | Thời điểm được đưa từ danh sách chờ vào lớp<br>DBML: thời điểm được tự động đưa từ danh sách chờ vào lớp |
| `grace_until` | `timestamptz` | Có | — | Thời điểm kết thúc ân hạn hủy<br>DBML: hết ân hạn hủy không phạt = promoted_at + PROMOTION_GRACE_MIN (15 phút), ghi cùng giao dịch đưa vào lớp; hủy đúng mốc này vẫn không phạt (&lt;=); thời điểm gửi email không ảnh hưởng (ADR 0010) |
| `reconfirm_expires_at` | `timestamptz` | Có | — | Hạn xác nhận lại sau đổi giờ<br>DBML: hạn xác nhận lại khi đổi giờ |
| `arrived_at` | `timestamptz` | Có | — | Thời điểm ghi nhận đã đến tại quầy<br>DBML: đánh dấu "đã đến" khi check-in tại quầy |
| `cancelled_at` | `timestamptz` | Có | — | Thời điểm hủy |
| `cancel_reason` | `cancel_reason` | Có | [Tập giá trị](#enum-cancel_reason) | Lý do hủy |
| `is_late_cancel` | `boolean` | Không | DEFAULT `false` | Có bị tính hủy muộn hay không |
| `attendance_marked_by` | `bigint` | Có | FK → `users.id` | Tham chiếu users.id<br>DBML: người điểm danh hoặc sửa điểm danh gần nhất (huấn luyện viên được phân công, hoặc quản lý chi nhánh sau thời hạn); mỗi lần sửa ghi thêm audit_logs giá trị cũ, mới |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| `uq_active_booking` | `class_session_id`, `member_id` | Có | chỉ mục duy nhất có điều kiện: WHERE status &lt;&gt; CANCELLED (hủy rồi vẫn đặt lại được) |
| Không đặt tên | `class_session_id`, `status`, `waitlisted_at` | Không | lấy người đứng đầu danh sách chờ |
| Không đặt tên | `member_id`, `status` | Không | — |

**Ghi chú cấp bảng từ DBML:** NF6: khi đặt chỗ, khóa (SELECT ... FOR UPDATE) dòng class_sessions để không vượt sĩ số, và dòng members của hội viên để hai yêu cầu đặt đồng thời không lọt kiểm tra trùng giờ. Thứ tự khóa cố định: members trước, class_sessions sau, để tránh deadlock. CHECK ((status = 'CANCELLED') = (cancelled_at IS NOT NULL AND cancel_reason IS NOT NULL)) AND (is_late_cancel = false OR status = 'CANCELLED')

### Nhóm 5. Huấn luyện viên

<a id="bang-trainer_availabilities"></a>

#### `trainer_availabilities`

Khung giờ rảnh theo thứ trong tuần. [Khai báo nguồn](erd-v2.dbml#L547).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `trainer_id` | `bigint` | Không | FK → `staff.id` | Tham chiếu staff.id |
| `branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id |
| `day_of_week` | `int` | Không | — | Thứ trong tuần theo ISO<br>DBML: 1..7 theo ISO |
| `start_time` | `time` | Không | — | Giờ bắt đầu |
| `end_time` | `time` | Không | — | Giờ kết thúc<br>DBML: CHECK end_time &gt; start_time |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `trainer_id`, `day_of_week` | Không | — |

<a id="bang-pt_sessions"></a>

#### `pt_sessions`

Buổi tập 1-1 đã đặt. [Khai báo nguồn](erd-v2.dbml#L561).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `trainer_id` | `bigint` | Không | FK → `staff.id` | Tham chiếu staff.id |
| `member_id` | `bigint` | Không | FK → `members.id` | Tham chiếu members.id<br>DBML: phải trùng memberships.member_id (kiểm tra trong code) |
| `membership_id` | `bigint` | Không | FK → `memberships.id` | Tham chiếu memberships.id<br>DBML: gói loại PT |
| `branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id |
| `start_at` | `timestamptz` | Không | — | Thời điểm bắt đầu<br>DBML: đặt trước ít nhất PT_MIN_LEAD_HOURS |
| `end_at` | `timestamptz` | Không | — | Thời điểm kết thúc<br>DBML: CHECK end_at &gt; start_at |
| `status` | `pt_session_status` | Không | DEFAULT `'BOOKED'`; [Tập giá trị](#enum-pt_session_status) | Trạng thái bản ghi |
| `confirmed_at` | `timestamptz` | Có | — | Thời điểm xác nhận buổi PT đã diễn ra<br>DBML: PT xác nhận buổi đã diễn ra; chỉ lúc này mới trừ buổi |
| `commission_amount` | `int` | Có | — | Tiền hoa hồng PT (VND)<br>DBML: VND, chụp lúc chuyển DONE, không đổi về sau: staff.commission_rate × (invoice_items.amount - invoice_items.discount_amount) ÷ memberships.total_sessions, làm tròn đến đồng. Gói nhận chuyển nhượng lấy dòng hóa đơn của gói gốc qua membership_transfers (chốt 01/11). CHECK có giá trị khi và chỉ khi status = DONE |
| `cancelled_at` | `timestamptz` | Có | — | Thời điểm hủy |
| `created_by` | `bigint` | Có | FK → `users.id` | Tham chiếu users.id |
| `note` | `text` | Có | — | Ghi chú |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `trainer_id`, `start_at` | Không | — |
| Không đặt tên | `member_id`, `start_at` | Không | — |
| Không đặt tên | `membership_id` | Không | — |

**Ghi chú cấp bảng từ DBML:** NF6: khi đặt buổi, khóa (SELECT ... FOR UPDATE) dòng members của hội viên rồi dòng staff của huấn luyện viên, sau đó mới kiểm tra trùng giờ với pt_sessions và class_sessions. Buổi mới chưa có dòng để khóa, nên phải khóa dòng cha.

<a id="bang-workout_plans"></a>

#### `workout_plans`

Giáo án huấn luyện viên soạn cho hội viên. [Khai báo nguồn](erd-v2.dbml#L585).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `trainer_id` | `bigint` | Không | FK → `staff.id` | Tham chiếu staff.id |
| `member_id` | `bigint` | Không | FK → `members.id` | Tham chiếu members.id |
| `title` | `varchar(255)` | Không | — | Tiêu đề |
| `content` | `jsonb` | Có | — | Nội dung<br>DBML: danh sách bài tập theo buổi |
| `start_date` | `date` | Có | — | Ngày bắt đầu |
| `end_date` | `date` | Có | — | Ngày kết thúc |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

<a id="bang-workout_logs"></a>

#### `workout_logs`

Nhật ký tập. [Khai báo nguồn](erd-v2.dbml#L597).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `member_id` | `bigint` | Không | FK → `members.id` | Tham chiếu members.id |
| `pt_session_id` | `bigint` | Có | FK → `pt_sessions.id` | Tham chiếu pt_sessions.id<br>DBML: rỗng nếu hội viên tự ghi |
| `logged_at` | `date` | Không | — | Ngày ghi nhật ký tập |
| `exercise` | `varchar(100)` | Không | — | Tên bài tập |
| `sets` | `int` | Có | — | Số hiệp |
| `reps` | `int` | Có | — | Số lần lặp |
| `weight` | `decimal(6,2)` | Có | — | Khối lượng (kg)<br>DBML: kg |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `member_id`, `logged_at` | Không | — |

### Nhóm 6. Bán hàng và thanh toán

<a id="bang-products"></a>

#### `products`

Hàng bán tại quầy (danh mục chung toàn chuỗi). [Khai báo nguồn](erd-v2.dbml#L616).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `sku` | `varchar(50)` | Không | UNIQUE | Mã hàng hóa |
| `name` | `varchar(255)` | Không | — | Tên |
| `price` | `int` | Không | — | Giá bán (VND)<br>DBML: VND, CHECK price &gt;= 0 |
| `unit` | `varchar(20)` | Có | — | Đơn vị tính |
| `status` | `active_status` | Không | DEFAULT `'ACTIVE'`; [Tập giá trị](#enum-active_status) | Trạng thái bản ghi |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

<a id="bang-inventories"></a>

#### `inventories`

Tồn kho theo chi nhánh. [Khai báo nguồn](erd-v2.dbml#L627).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id |
| `product_id` | `bigint` | Không | FK → `products.id` | Tham chiếu products.id |
| `quantity` | `int` | Không | DEFAULT `0` | Số lượng<br>DBML: CHECK quantity &gt;= 0 |
| `min_quantity` | `int` | Không | DEFAULT `0` | Mức tồn kho cảnh báo<br>DBML: dưới mức này thì cảnh báo sắp hết |
| `version` | `int` | Không | DEFAULT `0` | Phiên bản dùng kiểm soát cập nhật đồng thời<br>DBML: khóa lạc quan (@Version): hai quầy bán cùng lúc |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `branch_id`, `product_id` | Có | — |

<a id="bang-stock_movements"></a>

#### `stock_movements`

Nhập, xuất, điều chỉnh kho. [Khai báo nguồn](erd-v2.dbml#L641).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id |
| `product_id` | `bigint` | Không | FK → `products.id` | Tham chiếu products.id |
| `invoice_id` | `bigint` | Có | FK → `invoices.id` | Tham chiếu invoices.id<br>DBML: có khi xuất kho do bán hàng |
| `created_by` | `bigint` | Không | FK → `users.id` | Tham chiếu users.id |
| `type` | `stock_movement_type` | Không | [Tập giá trị](#enum-stock_movement_type) | Loại bản ghi |
| `quantity` | `int` | Không | — | Số lượng<br>DBML: IN, OUT: số dương, chiều do cột type quyết định. ADJUST: số có dấu (dương = tăng, âm = giảm khi kiểm kê). CHECK (type = ADJUST AND quantity &lt;&gt; 0) OR quantity &gt; 0 |
| `unit_cost` | `int` | Có | — | Đơn giá nhập (VND)<br>DBML: giá nhập, chỉ có khi type = IN |
| `note` | `text` | Có | — | Ghi chú |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `branch_id`, `product_id`, `created_at` | Không | lịch sử kho theo chi nhánh và sản phẩm |
| Không đặt tên | `invoice_id` | Không | — |

<a id="bang-promotions"></a>

#### `promotions`

Mã giảm giá. [Khai báo nguồn](erd-v2.dbml#L659).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `branch_id` | `bigint` | Có | FK → `branches.id` | Tham chiếu branches.id<br>DBML: rỗng = toàn chuỗi |
| `code` | `varchar(50)` | Không | UNIQUE | Mã định danh nghiệp vụ |
| `type` | `promotion_type` | Không | [Tập giá trị](#enum-promotion_type) | Loại bản ghi |
| `value` | `int` | Không | — | Giá trị khuyến mãi theo loại<br>DBML: PERCENT: 1..100; FIXED: số tiền VND |
| `max_discount` | `int` | Có | — | Mức giảm tối đa (VND)<br>DBML: mức giảm tối đa khi type = PERCENT, CHECK (type = 'PERCENT' OR max_discount IS NULL) |
| `min_amount` | `int` | Có | — | Giá trị hóa đơn tối thiểu (VND)<br>DBML: giá trị hóa đơn tối thiểu |
| `start_at` | `timestamptz` | Không | — | Thời điểm bắt đầu |
| `end_at` | `timestamptz` | Không | — | Thời điểm kết thúc |
| `usage_limit` | `int` | Có | — | Giới hạn lượt sử dụng |
| `used_count` | `int` | Không | DEFAULT `0` | Số lượt đã sử dụng<br>DBML: CHECK used_count &lt;= usage_limit |
| `status` | `active_status` | Không | DEFAULT `'ACTIVE'`; [Tập giá trị](#enum-active_status) | Trạng thái bản ghi |
| `version` | `int` | Không | DEFAULT `0` | Phiên bản dùng kiểm soát cập nhật đồng thời<br>DBML: khóa lạc quan (@Version): nhiều hóa đơn dùng cùng mã một lúc |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

<a id="bang-invoices"></a>

#### `invoices`

Hóa đơn. [Khai báo nguồn](erd-v2.dbml#L677).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `code` | `varchar(30)` | Không | UNIQUE | Mã định danh nghiệp vụ |
| `branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id |
| `member_id` | `bigint` | Có | FK → `members.id` | Tham chiếu members.id<br>DBML: rỗng nếu khách lẻ mua tại quầy; bắt buộc có khi hóa đơn chứa dòng PACKAGE hoặc TRANSFER_FEE (kiểm tra trong code); báo cáo doanh thu phải dùng LEFT JOIN với members |
| `created_by` | `bigint` | Không | FK → `users.id` | Tham chiếu users.id<br>DBML: lễ tân lập tại quầy, hoặc tài khoản hội viên khi tự gửi yêu cầu mua/gia hạn trên web (F7, hóa đơn PENDING, chốt 01/11) |
| `source` | `invoice_source` | Không | [Tập giá trị](#enum-invoice_source) | Nguồn tạo hóa đơn<br>DBML: COUNTER hoặc MEMBER_PORTAL (F7); chỉ hóa đơn MEMBER_PORTAL bị tự hủy khi quá hạn (chốt 01/11) |
| `promotion_id` | `bigint` | Có | FK → `promotions.id` | Tham chiếu promotions.id<br>DBML: tối đa một mã mỗi hóa đơn |
| `subtotal` | `int` | Không | — | Tổng tiền trước giảm giá (VND) |
| `discount` | `int` | Không | DEFAULT `0` | Số tiền giảm (VND)<br>DBML: bằng tổng invoice_items.discount_amount (kiểm tra trong code) |
| `total` | `int` | Không | — | Tổng tiền (VND)<br>DBML: CHECK total = subtotal - discount, total &gt;= 0 |
| `status` | `invoice_status` | Không | DEFAULT `'PENDING'`; [Tập giá trị](#enum-invoice_status) | Trạng thái bản ghi<br>DBML: hóa đơn MEMBER_PORTAL còn PENDING quá PENDING_INVOICE_TTL_DAYS thì tác vụ nền chuyển CANCELLED; chỉ lễ tân hoặc quản lý ghi nhận thanh toán (S7) |
| `paid_at` | `timestamptz` | Có | — | Thời điểm thanh toán |
| `refunded_by` | `bigint` | Có | FK → `users.id` | Tham chiếu users.id<br>DBML: chỉ quản lý chi nhánh |
| `refunded_at` | `timestamptz` | Có | — | Thời điểm hoàn tiền |
| `refund_reason` | `text` | Có | — | Lý do hoàn tiền |
| `version` | `int` | Không | DEFAULT `0` | Phiên bản dùng kiểm soát cập nhật đồng thời<br>DBML: khóa lạc quan (@Version): hai người cùng xác nhận thanh toán thì một người bị từ chối |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `branch_id`, `created_at` | Không | — |
| Không đặt tên | `member_id`, `created_at` | Không | — |
| `ix_invoices_pending_portal` | `created_at` | Không | chỉ mục có điều kiện WHERE status = PENDING AND source = MEMBER_PORTAL: tác vụ nền hủy hóa đơn chờ quá hạn |

<a id="bang-invoice_items"></a>

#### `invoice_items`

Dòng hóa đơn. [Khai báo nguồn](erd-v2.dbml#L703).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `invoice_id` | `bigint` | Không | FK → `invoices.id` | Tham chiếu invoices.id |
| `item_type` | `invoice_item_type` | Không | [Tập giá trị](#enum-invoice_item_type) | Loại dòng hóa đơn |
| `package_id` | `bigint` | Có | FK → `packages.id` | Tham chiếu packages.id<br>DBML: bắt buộc khi item_type = PACKAGE |
| `product_id` | `bigint` | Có | FK → `products.id` | Tham chiếu products.id<br>DBML: bắt buộc khi item_type = PRODUCT |
| `description` | `varchar(255)` | Không | — | Mô tả<br>DBML: tên hiển thị trên hóa đơn PDF, chép lúc bán |
| `quantity` | `int` | Không | DEFAULT `1` | Số lượng<br>DBML: CHECK quantity &gt; 0 |
| `unit_price` | `int` | Không | — | Đơn giá lúc bán (VND)<br>DBML: giá lúc bán, lấy từ server |
| `amount` | `int` | Không | — | Số tiền (VND)<br>DBML: CHECK amount = quantity * unit_price (trước giảm giá) |
| `discount_amount` | `int` | Không | DEFAULT `0` | Số tiền giảm phân bổ cho dòng (VND)<br>DBML: phần giảm giá của hóa đơn phân bổ xuống dòng này, server tính theo tỷ lệ amount; CHECK 0 &lt;= discount_amount &lt;= amount. Doanh thu theo gói (F16) và hoa hồng PT dùng amount - discount_amount |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `invoice_id` | Không | — |

<a id="bang-payments"></a>

#### `payments`

Mỗi lần thu tiền. [Khai báo nguồn](erd-v2.dbml#L721).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `invoice_id` | `bigint` | Không | FK → `invoices.id` | Tham chiếu invoices.id |
| `method` | `payment_method` | Không | [Tập giá trị](#enum-payment_method) | Phương thức thực hiện |
| `amount` | `int` | Không | — | Số tiền (VND)<br>DBML: CHECK amount &gt; 0 |
| `status` | `payment_status` | Không | DEFAULT `'CONFIRMED'`; [Tập giá trị](#enum-payment_status) | Trạng thái bản ghi |
| `transaction_ref` | `varchar(100)` | Có | — | Thông tin tham chiếu chuyển khoản<br>DBML: nội dung chuyển khoản VietQR, nếu có |
| `received_by` | `bigint` | Không | FK → `users.id` | Tham chiếu users.id<br>DBML: lễ tân hoặc quản lý xác nhận đã nhận tiền (S7) |
| `paid_at` | `timestamptz` | Không | — | Thời điểm thanh toán |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `invoice_id` | Không | — |

### Nhóm 7. Nhân sự

<a id="bang-staff"></a>

#### `staff`

Hồ sơ nhân viên; chức vụ lấy từ `users.role`; chủ chuỗi không có dòng ở bảng này. [Khai báo nguồn](erd-v2.dbml#L740).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `user_id` | `bigint` | Không | UNIQUE; FK → `users.id` (1–1) | Tham chiếu users.id<br>DBML: chức vụ lấy từ users.role; chủ chuỗi không có dòng ở bảng này |
| `branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id |
| `staff_code` | `varchar(20)` | Không | UNIQUE | Mã nhân viên |
| `full_name` | `varchar(255)` | Không | — | Họ và tên |
| `phone` | `varchar(20)` | Có | — | Số điện thoại<br>DBML: nơi duy nhất lưu số điện thoại nhân viên; cùng định dạng chuẩn với members.phone (CHECK); email lấy từ users.email |
| `base_salary` | `int` | Không | DEFAULT `0` | Lương cơ bản (VND)<br>DBML: CHECK base_salary &gt;= 0 |
| `hourly_rate` | `int` | Có | — | Mức lương theo giờ (VND)<br>DBML: CHECK hourly_rate IS NULL OR hourly_rate &gt;= 0 |
| `commission_rate` | `decimal(5,4)` | Có | — | Tỷ lệ hoa hồng PT<br>DBML: hoa hồng PT, ví dụ 0.1000 = 10%. CHECK commission_rate IS NULL OR (commission_rate &gt;= 0 AND commission_rate &lt;= 1) |
| `specialties` | `varchar(255)` | Có | — | Chuyên môn huấn luyện |
| `hired_at` | `date` | Có | — | Ngày tuyển dụng |
| `status` | `active_status` | Không | DEFAULT `'ACTIVE'`; [Tập giá trị](#enum-active_status) | Trạng thái bản ghi |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

<a id="bang-shifts"></a>

#### `shifts`

Ca làm đã xếp. [Khai báo nguồn](erd-v2.dbml#L757).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `staff_id` | `bigint` | Không | FK → `staff.id` | Tham chiếu staff.id |
| `branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id |
| `start_at` | `timestamptz` | Không | — | Thời điểm bắt đầu |
| `end_at` | `timestamptz` | Không | — | Thời điểm kết thúc<br>DBML: CHECK end_at &gt; start_at |
| `created_by` | `bigint` | Có | FK → `users.id` | Tham chiếu users.id |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `staff_id`, `start_at` | Không | — |

<a id="bang-attendances"></a>

#### `attendances`

Chấm công thực tế theo ca. [Khai báo nguồn](erd-v2.dbml#L771).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `staff_id` | `bigint` | Không | FK → `staff.id` | Tham chiếu staff.id |
| `shift_id` | `bigint` | Không | FK → `shifts.id` | Tham chiếu shifts.id |
| `check_in_at` | `timestamptz` | Có | — | Thời điểm vào ca thực tế |
| `check_out_at` | `timestamptz` | Có | — | Thời điểm ra ca thực tế |
| `status` | `attendance_status` | Không | [Tập giá trị](#enum-attendance_status) | Trạng thái bản ghi |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `shift_id` | Có | mỗi ca một bản ghi chấm công |

<a id="bang-payrolls"></a>

#### `payrolls`

Bảng lương tháng. [Khai báo nguồn](erd-v2.dbml#L785).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `staff_id` | `bigint` | Không | FK → `staff.id` | Tham chiếu staff.id |
| `branch_id` | `bigint` | Không | FK → `branches.id` | Tham chiếu branches.id<br>DBML: chép từ staff.branch_id lúc tính lương, để báo cáo lương theo chi nhánh không đổi khi nhân viên chuyển chi nhánh (v4) |
| `month` | `varchar(7)` | Không | — | Tháng tính lương<br>DBML: YYYY-MM. CHECK month ~ '^[0-9]{4}-(0[1-9]&#124;1[0-2])$' |
| `base_amount` | `int` | Không | — | Tiền lương cơ bản trong kỳ (VND)<br>DBML: CHECK base_amount &gt;= 0 |
| `commission_amount` | `int` | Không | DEFAULT `0` | Tiền hoa hồng PT (VND)<br>DBML: tổng pt_sessions.commission_amount của các buổi DONE trong tháng. CHECK commission_amount &gt;= 0 |
| `deduction` | `int` | Không | DEFAULT `0` | Khoản khấu trừ lương (VND)<br>DBML: CHECK deduction &gt;= 0 |
| `total` | `int` | Không | — | Tổng tiền (VND)<br>DBML: CHECK total = base_amount + commission_amount - deduction |
| `status` | `payroll_status` | Không | DEFAULT `'DRAFT'`; [Tập giá trị](#enum-payroll_status) | Trạng thái bản ghi |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `staff_id`, `month` | Có | — |

### Nhóm 8. Gamification

<a id="bang-badges"></a>

#### `badges`

Danh mục huy hiệu và điều kiện đạt. [Khai báo nguồn](erd-v2.dbml#L805).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `code` | `varchar(50)` | Không | UNIQUE | Mã định danh nghiệp vụ |
| `name` | `varchar(100)` | Không | — | Tên |
| `description` | `text` | Có | — | Mô tả |
| `icon_url` | `varchar(500)` | Có | — | Đường dẫn biểu tượng huy hiệu |
| `rule` | `jsonb` | Không | — | Quy tắc đạt huy hiệu<br>DBML: ví dụ {"type":"STREAK","days":7} hoặc {"type":"CHECKIN_COUNT","count":50} |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

<a id="bang-member_badges"></a>

#### `member_badges`

Huy hiệu hội viên đã đạt. [Khai báo nguồn](erd-v2.dbml#L816).

| Cột | Kiểu DBML | Rỗng | Ràng buộc khai báo | Ý nghĩa / ghi chú |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | Không | PK; IDENTITY | Khóa chính của bản ghi |
| `member_id` | `bigint` | Không | FK → `members.id` | Tham chiếu members.id |
| `badge_id` | `bigint` | Không | FK → `badges.id` | Tham chiếu badges.id |
| `earned_at` | `timestamptz` | Không | — | Thời điểm đạt huy hiệu |
| `created_at` | `timestamptz` | Không | — | Thời điểm tạo bản ghi (UTC) |
| `updated_at` | `timestamptz` | Không | — | Thời điểm cập nhật bản ghi (UTC) |

**Chỉ mục khai báo thêm** (khóa chính và UNIQUE trên cột đã nằm trong bảng cột):

| Tên trong DBML | Cột | Duy nhất | Ghi chú / điều kiện |
| --- | --- | --- | --- |
| Không đặt tên | `member_id`, `badge_id` | Có | — |

## 4. Tập giá trị Enum trong DBML

Các tập giá trị sau được giữ theo nguồn; việc chuyển sang `varchar` và `CHECK` thực hiện trong migration.

<a id="enum-user_role"></a>

### `user_role`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `SUPER_ADMIN` | — |
| `BRANCH_MANAGER` | — |
| `RECEPTIONIST` | — |
| `TRAINER` | — |
| `MEMBER` | — |

<a id="enum-user_status"></a>

### `user_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `ACTIVE` | — |
| `LOCKED` | — |
| `DISABLED` | — |

<a id="enum-package_type"></a>

### `package_type`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `TIME` | — |
| `SESSION` | — |
| `PT` | — |

<a id="enum-membership_status"></a>

### `membership_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `PENDING` | đã tạo hóa đơn, chưa thanh toán |
| `UPCOMING` | đã thanh toán, chưa đến ngày bắt đầu (gia hạn nối tiếp) |
| `ACTIVE` | — |
| `FROZEN` | — |
| `EXPIRED` | — |
| `TRANSFERRED` | — |
| `CANCELLED` | — |

<a id="enum-freeze_status"></a>

### `freeze_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `PENDING` | — |
| `APPROVED` | — |
| `REJECTED` | — |
| `CANCELLED` | — |

<a id="enum-checkin_method"></a>

### `checkin_method`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `MANUAL` | — |
| `FACE` | — |

<a id="enum-session_status"></a>

### `session_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `SCHEDULED` | — |
| `RESCHEDULED` | — |
| `CANCELLED` | — |
| `COMPLETED` | — |

<a id="enum-booking_status"></a>

### `booking_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `BOOKED` | — |
| `WAITLIST` | — |
| `PENDING_RECONFIRM` | — |
| `CANCELLED` | — |
| `ATTENDED` | — |
| `NO_SHOW` | — |

<a id="enum-cancel_reason"></a>

### `cancel_reason`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `BY_MEMBER` | — |
| `BY_STAFF` | — |
| `NOT_RECONFIRMED` | — |
| `SESSION_CANCELLED` | — |

<a id="enum-pt_session_status"></a>

### `pt_session_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `BOOKED` | — |
| `DONE` | — |
| `CANCELLED` | — |
| `NO_SHOW` | — |

<a id="enum-invoice_status"></a>

### `invoice_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `PENDING` | — |
| `PAID` | — |
| `CANCELLED` | — |
| `REFUNDED` | — |

<a id="enum-invoice_source"></a>

### `invoice_source`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `COUNTER` | lễ tân lập tại quầy |
| `MEMBER_PORTAL` | hội viên gửi yêu cầu mua/gia hạn từ cổng hội viên (F7) |

<a id="enum-transfer_status"></a>

### `transfer_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `PENDING` | đã lập hóa đơn phí, chưa thanh toán |
| `COMPLETED` | — |
| `CANCELLED` | — |

<a id="enum-invoice_item_type"></a>

### `invoice_item_type`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `PACKAGE` | — |
| `PRODUCT` | — |
| `TRANSFER_FEE` | — |

<a id="enum-payment_method"></a>

### `payment_method`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `CASH` | — |
| `BANK_TRANSFER` | — |

<a id="enum-payment_status"></a>

### `payment_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `CONFIRMED` | — |
| `CANCELLED` | — |

<a id="enum-promotion_type"></a>

### `promotion_type`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `PERCENT` | — |
| `FIXED` | — |

<a id="enum-stock_movement_type"></a>

### `stock_movement_type`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `IN` | — |
| `OUT` | — |
| `ADJUST` | — |

<a id="enum-equipment_status"></a>

### `equipment_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `GOOD` | — |
| `BROKEN` | — |
| `MAINTENANCE` | — |
| `RETIRED` | — |

<a id="enum-maintenance_status"></a>

### `maintenance_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `REPORTED` | — |
| `SCHEDULED` | — |
| `IN_PROGRESS` | — |
| `DONE` | — |

<a id="enum-notification_channel"></a>

### `notification_channel`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `IN_APP` | — |
| `EMAIL` | — |

<a id="enum-notification_status"></a>

### `notification_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `PENDING` | — |
| `SENT` | — |
| `FAILED` | — |

<a id="enum-attendance_status"></a>

### `attendance_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `ON_TIME` | — |
| `LATE` | — |
| `EARLY_LEAVE` | — |
| `ABSENT` | — |

<a id="enum-payroll_status"></a>

### `payroll_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `DRAFT` | — |
| `CONFIRMED` | — |
| `PAID` | — |

<a id="enum-active_status"></a>

### `active_status`

| Giá trị | Ghi chú trong nguồn |
| --- | --- |
| `ACTIVE` | — |
| `INACTIVE` | — |

## 5. Lịch sử thay đổi

| Ngày | Nội dung |
| --- | --- |
| 05/11/2026 | Xuất từ điển đầy đủ từ ERD v2 cho #15; giữ cột, kiểu, mặc định, khóa, chỉ mục, Enum và ghi chú của nguồn |
| 12/11/2026 | Xuất lại theo nguồn mới: các CHECK và chỉ mục đã thêm vào DBML sau 05/11; `class_bookings` theo ADR 0010 (`created_by` bắt buộc, ghi chú `waitlisted_at`, `grace_until`, `attendance_marked_by`) |
