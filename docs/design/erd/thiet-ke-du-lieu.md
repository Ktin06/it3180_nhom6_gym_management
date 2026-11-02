# Thiết kế dữ liệu – Web quản lý chuỗi phòng tập Gym

Cập nhật: 01/11/2026 (bản đầu: 27/10/2026) · Hoàng Kim Tiến · Trạng thái: ERD bản 2, cả nhóm đã duyệt ngày 02/11/2026

Tài liệu này giải thích thiết kế cơ sở dữ liệu. **Mã nguồn chuẩn của ERD là file `erd-v2.dbml`** (cùng thư mục); khi hai nơi mâu thuẫn thì file DBML là đúng. Dán file đó vào [dbdiagram.io](https://dbdiagram.io) để xem sơ đồ đầy đủ cột và đường quan hệ, rồi xuất ảnh PNG hoặc PDF cho báo cáo LaTeX. Quy tắc nghiệp vụ nằm trong "Bản chốt phạm vi".

## Nguyên tắc thiết kế

Mọi bảng tuân theo cùng một quy ước, để mã do 8 người (và AI) sinh ra khớp nhau. Hệ thống có 39 bảng chia 8 nhóm, thiết kế cho cả 20 chức năng ngay từ đầu, kể cả các chức năng làm ở v3, v4, v5.

- **Tên:** bảng và cột viết snake_case, tên bảng số nhiều (`members`, `class_bookings`).
- **Khóa chính:** cột `id` kiểu `bigint` tự tăng (khớp kiểu `Long` trong entity Java); khóa ngoại đặt tên `<bảng số ít>_id` (`member_id`).
- **Người thao tác:** lễ tân, quản lý, chủ chuỗi, người duyệt luôn trỏ tới `users.id`, đặt tên `*_by` (`created_by`, `approved_by`). Huấn luyện viên với vai trò dạy trỏ tới `staff.id`, đặt tên `trainer_id`.
- **Thời gian:** mọi bảng có `created_at`, `updated_at`; kiểu `timestamptz`, lưu theo UTC, hiển thị theo giờ Việt Nam. Khi quy tắc cần "ngày" theo giờ Việt Nam thì lưu thêm cột `date` riêng (ví dụ `checkins.checkin_date`).
- **Tiền:** số nguyên đơn vị VND, không dùng số thực để tránh sai số làm tròn.
- **Trạng thái:** giá trị viết hoa (`ACTIVE`, `EXPIRED`). Trong migration lưu kiểu `varchar` kèm ràng buộc `CHECK`; entity Java dùng `@Enumerated(EnumType.STRING)`. Các `Enum` trong file DBML chỉ để vẽ sơ đồ.
- **Mỗi thông tin chỉ lưu ở một nơi:** vai trò ở `users.role`; email ở `users.email`; chi nhánh làm việc của nhân viên ở `staff.branch_id`; số điện thoại ở `members.phone` hoặc `staff.phone`.
- **Cột chép có chủ đích:** một số cột được chép lại như ảnh chụp tại một thời điểm và không đổi về sau, ví dụ giá lúc bán (`invoice_items.unit_price`), loại gói lúc mua (`memberships.package_type`), huấn luyện viên và phòng của từng buổi (`class_sessions`). Trong file DBML các cột này có ghi chú "chép từ".
- **Không xóa cứng** dữ liệu nghiệp vụ (hội viên, hóa đơn, gói tập): dùng trạng thái hoặc cột `deleted_at`. Ngoại lệ duy nhất là `face_embeddings`: hội viên rút lại đồng ý thì xóa thật (S8).
- **Chi nhánh từ ngày đầu:** các bảng nghiệp vụ gốc có `branch_id` ngay từ v1 (dùng một chi nhánh mặc định), dù đa chi nhánh (F18) làm ở v4, để v4 không phải sửa lại cơ sở dữ liệu. Bảng con lấy chi nhánh qua bảng cha (ví dụ `class_bookings` qua `class_sessions`, `payments` qua `invoices`), không chép lại; riêng `payrolls.branch_id` được chép lúc tính lương, vì nhân viên có thể chuyển chi nhánh.
- **Dữ liệu chuẩn hóa:** email lưu chữ thường (`CHECK email = lower(email)`); số điện thoại lưu 10 chữ số bắt đầu bằng 0 (`CHECK phone ~ '^0[0-9]{9}$'`). Mã nguồn chuẩn hóa trước khi lưu và trước khi tìm kiếm.
- **Chỉ mục:** PostgreSQL không tự tạo chỉ mục cho khóa ngoại; khóa ngoại nào được tra cứu thường xuyên thì có chỉ mục riêng (ghi trong file DBML). Chỉ mục duy nhất trên cột có thể rỗng dùng `NULLS NOT DISTINCT` (PostgreSQL 15+), để giá trị rỗng (= toàn chuỗi) cũng bị kiểm tra trùng.
- **Ghi đồng thời:** bảng hay bị sửa cùng lúc (`memberships`, `invoices`, `inventories`, `promotions`) có cột `version` cho khóa lạc quan (`@Version` của JPA). Khi kiểm tra trùng giờ trước lúc thêm dòng mới (đặt lớp, đặt PT), khóa dòng cha bằng `SELECT ... FOR UPDATE` theo thứ tự cố định: `members` trước, rồi `class_sessions` hoặc `staff` (NF6).
- **Ánh xạ và migration:** entity ánh xạ bằng Spring Data JPA (Hibernate); schema tạo và sửa bằng Flyway; `ddl-auto` luôn là `validate`, không tự sinh bảng. Không sửa file migration đã gộp vào `main`. Schema ban đầu tạo đủ 39 bảng trong một lần, mỗi nhóm bảng một dải số phiên bản (`V1_xx` … `V8_xx`), khóa ngoại trỏ sang nhóm bảng khác đặt trong migration cuối; sau đó mọi thay đổi dùng dải tuần tự `V9_xx` trở đi (xem [quy ước migration](../../../backend/src/main/resources/db/migration/README.md)).

## Danh sách bảng theo nhóm

### 1. Tài khoản và hệ thống (6 bảng)

| Bảng | Mục đích | Cột và khóa ngoại chính | Chức năng |
| --- | --- | --- | --- |
| `users` | Tài khoản đăng nhập của mọi vai trò trừ khách | `email` (duy nhất, dùng để đăng nhập), `password_hash`, `role`, `status`, `failed_login_count`, `locked_until` | F1 |
| `refresh_tokens` | Phiên đăng nhập dài hạn, để làm mới token và đăng xuất | `user_id`, `token_hash`, `expires_at`, `revoked_at` | F1 |
| `password_resets` | Mã đặt lại mật khẩu, dùng một lần | `user_id`, `token_hash`, `expires_at`, `used_at` | F1 |
| `audit_logs` | Ai làm gì, trên bảng nào, giá trị cũ và mới | `user_id`, `branch_id`, `action`, `entity`, `entity_id`, `old_value`, `new_value`, `ip` | F17 |
| `notifications` | Thông báo trong web và email | `user_id`, `type`, `channel`, `status`, `scheduled_at`, `sent_at`, `read_at`, `related_entity`, `related_id` | F10 |
| `system_configs` | Tham số cấu hình (các giá trị có dấu \* trong bản chốt phạm vi) | `branch_id` (rỗng = toàn chuỗi), `config_key`, `config_value` | F8, F9, F15, F18 |

### 2. Chi nhánh và cơ sở vật chất (4 bảng)

| Bảng | Mục đích | Cột và khóa ngoại chính | Chức năng |
| --- | --- | --- | --- |
| `branches` | Chi nhánh; từ v1 đến v3 chỉ có một dòng (chi nhánh mặc định) | `code`, `name`, `open_time`, `close_time`, `status` | F18, F4 |
| `rooms` | Phòng trong chi nhánh | `branch_id`, `name`, `type`, `capacity` | F8 |
| `equipment` | Thiết bị, máy tập | `branch_id`, `room_id`, `code`, `status` | F11 |
| `maintenance_logs` | Báo hỏng và bảo trì | `equipment_id`, `reported_by`, `status`, `scheduled_date`, `completed_date`, `cost` | F11 |

### 3. Hội viên và gói tập (7 bảng)

| Bảng | Mục đích | Cột và khóa ngoại chính | Chức năng |
| --- | --- | --- | --- |
| `members` | Hồ sơ hội viên | `user_id` (rỗng nếu chưa có tài khoản), `home_branch_id`, `member_code`, `phone` (duy nhất), `avatar_url`, `face_consent_at`, `booking_locked_until`, `status`, `deleted_at` | F2 |
| `face_embeddings` | Vector khuôn mặt 128 chiều, không lưu ảnh | `member_id`, `embedding` (`real[]`), `model_version` | F20 |
| `body_metrics` | Chỉ số cơ thể theo từng lần đo | `member_id`, `recorded_by`, `measured_at`, cân nặng, chiều cao, % mỡ, khối cơ | F2, F9 |
| `packages` | Danh mục gói đang bán (bảng giá) | `branch_id` (rỗng = toàn chuỗi), `code`, `type` (TIME, SESSION, PT), `duration_days` (gói "1 tháng" = 30 ngày), `session_count`, `price`, `status` | F3 |
| `memberships` | Gói một hội viên đã mua; bảng trung tâm | `member_id`, `package_id`, `package_type`, `branch_id`, `sold_branch_id`, `invoice_item_id`, `start_date`, `end_date`, `total_sessions`, `remaining_sessions`, `freeze_count`, `status`, `version` | F3, F4, F7 |
| `membership_freezes` | Yêu cầu bảo lưu và kết quả duyệt | `membership_id`, `requested_by`, `approved_by`, `from_date`, `to_date`, `status` | F15 |
| `membership_transfers` | Lần chuyển nhượng gói | `from_membership_id`, `to_membership_id`, `from_member_id`, `to_member_id`, `invoice_id`, `fee`, `status` (PENDING, COMPLETED, CANCELLED) | F15 |

Trạng thái gói (`memberships.status`): PENDING (đã có hóa đơn, chưa thanh toán) → UPCOMING (đã thanh toán, chưa tới ngày bắt đầu) hoặc ACTIVE → FROZEN (đang bảo lưu) → EXPIRED; ngoài ra TRANSFERRED (đã chuyển nhượng) và CANCELLED.

### 4. Check-in và lớp học (5 bảng)

| Bảng | Mục đích | Cột và khóa ngoại chính | Chức năng |
| --- | --- | --- | --- |
| `checkins` | Mỗi lượt vào phòng tập | `member_id`, `membership_id` (gói TIME hoặc SESSION), `branch_id`, `created_by`, `method` (MANUAL, FACE), `match_score`, `checkin_date`, `session_deducted`, `checked_in_at`, `checked_out_at` | F4, F6, F20 |
| `class_types` | Loại lớp: Yoga, Zumba, Boxing… | `name`, `status` | F8 |
| `class_schedules` | Lịch lặp hằng tuần | `branch_id`, `class_type_id`, `trainer_id`, `room_id`, `day_of_week` (1 = thứ Hai … 7 = Chủ nhật), `start_time`, `end_time`, `capacity`, `valid_from`, `valid_to` | F8 |
| `class_sessions` | Từng buổi cụ thể sinh ra từ lịch lặp; hội viên đặt chỗ vào buổi | `class_schedule_id`, `branch_id`, `trainer_id`, `room_id`, `occurrence_date` (ngày theo lịch lặp, không đổi khi đổi giờ), `start_at`, `end_at`, `original_start_at`, `capacity`, `status` | F8 |
| `class_bookings` | Đặt chỗ và danh sách chờ, gộp một bảng | `class_session_id`, `member_id`, `created_by`, `status`, `waitlisted_at`, `promoted_at`, `grace_until`, `reconfirm_expires_at`, `arrived_at`, `cancel_reason`, `is_late_cancel`, `attendance_marked_by` | F8, F10 |

### 5. Huấn luyện viên (4 bảng)

| Bảng | Mục đích | Cột và khóa ngoại chính | Chức năng |
| --- | --- | --- | --- |
| `trainer_availabilities` | Khung giờ rảnh theo thứ trong tuần | `trainer_id`, `branch_id`, `day_of_week`, `start_time`, `end_time` | F9 |
| `pt_sessions` | Buổi tập 1-1 đã đặt | `trainer_id`, `member_id`, `membership_id` (gói PT), `branch_id`, `start_at`, `end_at`, `status`, `confirmed_at`, `commission_amount` (chụp lúc DONE) | F9, F13 |
| `workout_plans` | Giáo án huấn luyện viên soạn cho hội viên | `trainer_id`, `member_id`, `title`, `content` (JSON) | F9 |
| `workout_logs` | Nhật ký tập | `member_id`, `pt_session_id` (rỗng nếu tự tập), `exercise`, `sets`, `reps`, `weight` | F9 |

### 6. Bán hàng và thanh toán (7 bảng)

| Bảng | Mục đích | Cột và khóa ngoại chính | Chức năng |
| --- | --- | --- | --- |
| `products` | Hàng bán tại quầy (danh mục chung toàn chuỗi) | `sku`, `name`, `price`, `status` | F12 |
| `inventories` | Tồn kho theo chi nhánh | `branch_id`, `product_id`, `quantity`, `min_quantity`, `version` | F12 |
| `stock_movements` | Nhập, xuất, điều chỉnh kho | `branch_id`, `product_id`, `invoice_id`, `created_by`, `type`, `quantity` (ADJUST mang dấu), `unit_cost` | F12 |
| `promotions` | Mã giảm giá | `branch_id`, `code`, `type` (PERCENT, FIXED), `value`, `max_discount`, `min_amount`, `start_at`, `end_at`, `usage_limit`, `used_count`, `version` | F14 |
| `invoices` | Hóa đơn | `branch_id`, `member_id` (rỗng nếu khách lẻ), `created_by` (lễ tân, hoặc hội viên với hóa đơn chờ từ F7), `source` (COUNTER, MEMBER_PORTAL), `promotion_id`, `subtotal`, `discount`, `total`, `status`, `paid_at`, thông tin hoàn tiền, `version` | F5, F7, F12, F14 |
| `invoice_items` | Dòng hóa đơn | `invoice_id`, `item_type` (PACKAGE, PRODUCT, TRANSFER_FEE), `package_id`, `product_id`, `description`, `quantity`, `unit_price`, `amount`, `discount_amount` (phần giảm giá phân bổ cho dòng) | F5, F12, F15, F16 |
| `payments` | Mỗi lần thu tiền | `invoice_id`, `method` (CASH, BANK_TRANSFER), `amount`, `status`, `transaction_ref`, `received_by`, `paid_at` | F5 |

### 7. Nhân sự (4 bảng)

| Bảng | Mục đích | Cột và khóa ngoại chính | Chức năng |
| --- | --- | --- | --- |
| `staff` | Hồ sơ nhân viên; chức vụ lấy từ `users.role`; chủ chuỗi không có dòng ở bảng này | `user_id`, `branch_id`, `staff_code`, `full_name`, `phone`, `base_salary`, `hourly_rate`, `commission_rate`, `specialties` | F13, F9 |
| `shifts` | Ca làm đã xếp | `staff_id`, `branch_id`, `start_at`, `end_at` | F13 |
| `attendances` | Chấm công thực tế theo ca | `staff_id`, `shift_id` (duy nhất), `check_in_at`, `check_out_at`, `status` | F13 |
| `payrolls` | Bảng lương tháng | `staff_id`, `branch_id` (chép lúc tính lương), `month`, `base_amount`, `commission_amount`, `deduction`, `total`, `status` | F13 |

### 8. Gamification (2 bảng)

| Bảng | Mục đích | Cột và khóa ngoại chính | Chức năng |
| --- | --- | --- | --- |
| `badges` | Danh mục huy hiệu và điều kiện đạt | `code`, `name`, `rule` (JSON) | F19 |
| `member_badges` | Huy hiệu hội viên đã đạt | `member_id`, `badge_id` | F19 |

Chuỗi ngày tập và bảng xếp hạng không có bảng riêng; được tính khi truy vấn từ `checkins`. Không dùng AI.

## Quan hệ và ràng buộc quan trọng

Mỗi ràng buộc dưới đây cần ít nhất một ca kiểm thử. Cột "Nơi kiểm tra" cho biết ràng buộc nằm ở cơ sở dữ liệu (migration phải viết bằng SQL) hay trong mã nghiệp vụ.

| Quan hệ hoặc ràng buộc | Nơi kiểm tra | Ý nghĩa |
| --- | --- | --- |
| `users` – `members`, `users` – `staff` (1 – 0..1) | Cơ sở dữ liệu | `user_id` duy nhất trong mỗi bảng; một tài khoản tối đa một hồ sơ |
| `members.phone`, `members.member_code` duy nhất | Cơ sở dữ liệu | F4 tìm hội viên theo hai cột này phải ra đúng một người; tự đăng ký bằng số đã có hồ sơ thì bị từ chối |
| Email, số điện thoại ở dạng chuẩn | Cơ sở dữ liệu (CHECK) và mã nghiệp vụ | `A@x.com` và `a@x.com` là một email; số điện thoại 10 chữ số bắt đầu bằng 0 |
| Mỗi loại gói tối đa một gói đang hiệu lực | Cơ sở dữ liệu (chỉ mục duy nhất có điều kiện, `NULLS NOT DISTINCT`) | Duy nhất trên (`member_id`, `package_type`, `branch_id`) với các dòng ACTIVE hoặc FROZEN; hai gói toàn chuỗi (`branch_id` rỗng) cùng loại cũng bị chặn |
| Bảo lưu dời gói nối tiếp | Mã nghiệp vụ | Duyệt bảo lưu N ngày thì cộng N ngày vào `end_date` của gói, đồng thời vào `start_date`, `end_date` của gói UPCOMING cùng loại nối tiếp, trong cùng giao dịch |
| Gói chỉ ACTIVE khi hóa đơn đã PAID | Mã nghiệp vụ | Tổng các lần thanh toán thành công bằng tổng hóa đơn thì hóa đơn chuyển PAID và gói được kích hoạt; hai người cùng xác nhận thì một người bị từ chối (`version`) |
| Hóa đơn chờ F7 quá hạn tự hủy | Mã nghiệp vụ (tác vụ nền) | Chỉ hóa đơn `source = MEMBER_PORTAL`: sau `PENDING_INVOICE_TTL_DAYS` ngày, hóa đơn và gói PENDING đi kèm chuyển CANCELLED; hóa đơn `COUNTER` không bị hủy tự động |
| Dòng hóa đơn khớp loại | Cơ sở dữ liệu (CHECK) | PACKAGE thì có `package_id`; PRODUCT thì có `product_id`; TRANSFER_FEE thì không có cả hai; `amount = quantity * unit_price`; `0 <= discount_amount <= amount` |
| Giảm giá phân bổ xuống dòng | Mã nghiệp vụ | Tổng `invoice_items.discount_amount` bằng `invoices.discount`; doanh thu theo gói (F16) dùng `amount - discount_amount` |
| Chuyển nhượng chỉ hoàn tất khi đã thu phí | Mã nghiệp vụ và CHECK | Hóa đơn phí PAID thì `status = COMPLETED`, gói cũ TRANSFERRED, tạo gói mới; hóa đơn hủy thì CANCELLED, gói cũ giữ nguyên |
| Kho điều chỉnh có dấu | Cơ sở dữ liệu (CHECK) | IN, OUT có `quantity > 0`; ADJUST có `quantity <> 0` (âm = giảm) |
| Hóa đơn có gói hoặc phí chuyển nhượng phải có hội viên | Mã nghiệp vụ | `member_id` chỉ được rỗng với hóa đơn chỉ có sản phẩm (khách lẻ); báo cáo doanh thu dùng `LEFT JOIN` |
| Check-in dùng đúng gói | Mã nghiệp vụ | Chỉ gói TIME hoặc SESSION đang ACTIVE; có cả hai thì dùng TIME; `checkins.member_id` trùng chủ của gói |
| Trừ tối đa 1 buổi mỗi ngày | Cơ sở dữ liệu (chỉ mục duy nhất có điều kiện) | Duy nhất trên (`membership_id`, `checkin_date`) với các dòng có `session_deducted = true` |
| Đặt lớp cần gói TIME | Mã nghiệp vụ | Không có gói TIME đang ACTIVE tại chi nhánh thì không được đặt chỗ hay vào chờ |
| Không sinh trùng buổi lớp | Cơ sở dữ liệu (chỉ mục duy nhất) | Duy nhất trên (`class_schedule_id`, `occurrence_date`); buổi đã đổi giờ không bị sinh lại ở giờ cũ |
| Một chỗ đặt đang hoạt động cho mỗi buổi | Cơ sở dữ liệu (chỉ mục duy nhất có điều kiện) | Duy nhất trên (`class_session_id`, `member_id`) với các dòng chưa CANCELLED; hủy rồi vẫn đặt lại được |
| Không vượt sĩ số | Mã nghiệp vụ, có khóa bản ghi (NF6) | Khóa dòng `class_sessions`; số chỗ đang giữ (mọi trạng thái trừ WAITLIST và CANCELLED, tức BOOKED, PENDING_RECONFIRM, ATTENDED, NO_SHOW) không vượt `class_sessions.capacity` |
| Thứ tự danh sách chờ | Mã nghiệp vụ | Đưa vào lớp theo `waitlisted_at` tăng dần; vị trí hiển thị tính khi truy vấn, không lưu số cố định |
| Hội viên không trùng giờ lớp | Mã nghiệp vụ, có khóa bản ghi (NF6) | Khóa dòng `members` trước khi kiểm tra; không có hai chỗ đặt đang hoạt động ở hai buổi trùng giờ, kể cả khi hai yêu cầu đến cùng lúc |
| Huấn luyện viên không trùng giờ | Mã nghiệp vụ, có khóa bản ghi (NF6) | Khóa dòng `members` rồi `staff`; không có hai buổi PT trùng giờ, kể cả với giờ dạy lớp (`class_sessions.trainer_id`) |
| Buổi PT dùng đúng gói | Mã nghiệp vụ | `pt_sessions.membership_id` là gói PT đang ACTIVE của chính hội viên đó; chỉ trừ buổi khi DONE |
| Hoa hồng PT chụp lúc DONE | Mã nghiệp vụ và CHECK | `commission_amount = commission_rate × (amount - discount_amount) ÷ total_sessions` của dòng hóa đơn gốc, làm tròn đến đồng; có giá trị khi và chỉ khi DONE; `payrolls.commission_amount` là tổng trong tháng |
| `trainer_id` là huấn luyện viên | Mã nghiệp vụ | Tài khoản của nhân viên phải có `role = TRAINER` |
| Khuôn mặt cần đồng ý | Mã nghiệp vụ và CHECK | Chỉ tạo `face_embeddings` khi `members.face_consent_at` có giá trị; vector đúng 128 chiều; rút lại đồng ý thì xóa thật mọi dòng và đặt `face_consent_at` rỗng |
| Ngừng hoạt động hội viên | Mã nghiệp vụ | `members.status = INACTIVE` thì `users.status = DISABLED` trong cùng giao dịch |
| Cấu hình toàn chuỗi duy nhất | Cơ sở dữ liệu | Duy nhất `NULLS NOT DISTINCT` trên (`branch_id`, `config_key`) |
| Lọc theo chi nhánh | Mã nghiệp vụ | Mọi truy vấn của quản lý, lễ tân, huấn luyện viên tự động lọc theo `staff.branch_id` của họ |

## Câu hỏi về dữ liệu đã chốt

- [x] Hội viên được giữ đồng thời gói TIME và gói PT (31/10). Lớp nhóm đã gồm trong gói TIME; gói PT chỉ dùng cho tập 1-1.
- [x] Gói SESSION và gói PT không dùng để đặt lớp nhóm (31/10).
- [x] Gói PT không dùng để check-in vào phòng tập; có cả TIME và SESSION thì dùng TIME (01/11).
- [x] Khách lẻ mua hàng tại quầy: `invoices.member_id` để rỗng, không tạo hồ sơ mặc định (31/10).
- [x] Không lưu lịch sử giá gói tập; giá lúc bán trong `invoice_items.unit_price` là đủ (31/10).
- [x] Giới hạn check-in liên chi nhánh của gói toàn chuỗi: để đến v4 (31/10).
- [x] Đăng nhập chỉ bằng email; mỗi thông tin chỉ lưu ở một nơi (31/10).
- [x] Trạng thái lưu `varchar` kèm `CHECK` thay vì kiểu enum của PostgreSQL (31/10).
- [x] F7 tạo hóa đơn chờ do hội viên gửi; tự hủy sau 3 ngày (01/11).
- [x] Tự đăng ký bằng số điện thoại đã có hồ sơ thì từ chối; lễ tân tạo tài khoản cho hồ sơ có sẵn (01/11).
- [x] Nhận diện khuôn mặt: face-api.js, vector 128 chiều, lưu kiểu `real[]` (không cần pgvector).
- [x] Bảo lưu gói đang có gói UPCOMING nối tiếp: dời gói UPCOMING thêm đúng số ngày bảo lưu (01/11).
- [x] Chỉ hóa đơn chờ do hội viên tạo từ F7 (`source = MEMBER_PORTAL`) tự hủy khi quá hạn (01/11).
- [x] Hoa hồng PT: phần trăm giá mỗi buổi, chụp vào `pt_sessions.commission_amount` lúc DONE (01/11).
- [x] Gói TIME tính theo số ngày (`duration_days`); "1 tháng" = 30 ngày (01/11).
- [x] Mã hội viên dạng `HV` + 6 chữ số (`HV000123`), sinh từ sequence `member_code_seq` dùng chung cả chuỗi (01/11).
- [ ] Ảnh đại diện hội viên lưu ở đâu: ổ đĩa Render bản miễn phí bị xóa khi triển khai lại nên không lưu file trên Render. Sprint 1 làm F2 chưa có ảnh (`avatar_url` để rỗng); chốt trước khi làm F4 ở Sprint 2.

## Lịch sử thay đổi

| Ngày | Thay đổi |
| --- | --- |
| 27/10/2026 | Bản đầu: 39 bảng, 8 nhóm |
| 31/10/2026 | ERD bản 2: khóa chính `bigint`, thời gian `timestamptz`; người thao tác trỏ `users.id`; bỏ `users.branch_id`, `users.phone`, `staff.position`, `members.email`; thêm trạng thái gói UPCOMING; `memberships.invoice_item_id`, `memberships.package_type`; `invoice_items.item_type`; `checkins.checkin_date`; `class_sessions.trainer_id`; chỉ mục duy nhất có điều kiện cho đặt chỗ, trừ buổi, gói đang hiệu lực |
| 01/11/2026 | Ghi các quyết định check-in, F7 hóa đơn chờ, trùng số điện thoại khi đăng ký; thêm tham số `PENDING_INVOICE_TTL_DAYS`; bỏ mã DBML nhúng trong tài liệu, dùng `erd-v2.dbml` làm bản chuẩn duy nhất |
| 01/11/2026 | Định dạng mã hội viên `HV` + 6 chữ số; để mở câu hỏi nơi lưu ảnh đại diện đến Sprint 2 |
| 02/11/2026 | Cả nhóm duyệt ERD bản 2 tại buổi kick-off; `TRANSFER_FEE` mặc định 200.000 đ |
| 01/11/2026 | Sau review ERD: `NULLS NOT DISTINCT` cho `uq_one_active_per_type` và `system_configs`; thêm `class_sessions.occurrence_date`, `invoices.source`, `invoice_items.discount_amount`, `pt_sessions.commission_amount`, `membership_transfers.status`, `payrolls.branch_id`, cột `version` cho 4 bảng; ADJUST kho mang dấu; CHECK chuẩn hóa email, số điện thoại, vector 128 chiều; thêm chỉ mục khóa ngoại; ghi rõ cách khóa bản ghi (NF6), ngoại lệ xóa cứng `face_embeddings`, phân biệt `users.status = LOCKED` với `locked_until` |
| 01/11/2026 | Sau review toàn dự án: schema ban đầu tạo đủ 39 bảng một lần, migration về sau dùng dải `V9_xx`; sĩ số tính cả chỗ ATTENDED, NO_SHOW |
