# Bảng truy vết chức năng – use case – bảng dữ liệu

Cập nhật: 27/11/2026 · Nguyễn Anh Tú · Nhiệm vụ [#204](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/204) · S2-32

## 1. Phạm vi và cách đọc

Bảng nối 20 chức năng với 57 mã use case (54 còn hiệu lực, 3 đã bỏ) và 40 bảng trong [ERD v2](../../design/erd/erd-v2.dbml). Các dòng truy vết cho biết bảng nào được đọc, ghi hoặc dùng để kiểm tra quyền. Tiêu chí chấp nhận nằm trong mục H của từng use case. Đối chiếu mã AC và TC tại [ma trận kiểm thử](../../testing/ma-tran-truy-vet.md).

- Mốc đối chiếu `main` là `275a31a`. Đặc tả F7, F9 và F10 theo cây truy vết đã có trên `main` từ [PR #249](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/249) (`2beeedb`).
- Các mã UC-F8.2, UC-F8.4 và UC-F10.3 được giữ để tra lịch sử theo [ADR 0014](../../decisions/0014-bo-danh-sach-cho-lop-hoc-nhom.md). Các mã đã bỏ không xuất hiện trong chiều truy vết ngược.
- F11–F20 giữ ánh xạ bảng dữ liệu trong [thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md) và ghi “Chưa đặc tả”.

## 2. Tổng quan F1–F20 theo ánh xạ gốc

| Chức năng | Version | Mã use case / nguồn | Bảng trong ánh xạ gốc |
| --- | --- | --- | --- |
| F1 – Tài khoản và phân quyền | v1 | UC-F1.1, UC-F1.2, UC-F1.3, UC-F1.4, UC-F1.5, UC-F1.6 ([đặc tả](dac-ta-use-case-f1-tai-khoan-va-phan-quyen.md)) | `users`, `refresh_tokens`, `password_resets` |
| F2 – Hồ sơ hội viên | v1 | UC-F2.1, UC-F2.2, UC-F2.3, UC-F2.4, UC-F2.5 ([đặc tả](dac-ta-use-case-f2-ho-so-hoi-vien.md)) | `members`, `member_avatars`, `body_metrics` |
| F3 – Gói tập và gia hạn | v1 | UC-F3.1, UC-F3.2, UC-F3.3, UC-F3.4, UC-F3.5 ([đặc tả](dac-ta-use-case-f3-goi-tap-va-gia-han.md)) | `packages`, `memberships` |
| F4 – Check-in tại quầy | v1 | UC-F4.1, UC-F4.2, UC-F4.3, UC-F4.4, UC-F4.5 ([đặc tả](dac-ta-use-case-f4-check-in-tai-quay.md)) | `branches`, `memberships`, `checkins` |
| F5 – Thanh toán tại quầy | v1 | UC-F5.1, UC-F5.2, UC-F5.3, UC-F5.4, UC-F5.5 ([đặc tả](dac-ta-use-case-f5-thanh-toan-tai-quay.md)) | `invoices`, `invoice_items`, `payments` |
| F6 – Dashboard cơ bản | v1 | UC-F6.1, UC-F6.2, UC-F6.3, UC-F6.4, UC-F6.5 ([đặc tả](dac-ta-use-case-f6-dashboard-co-ban.md)) | `system_configs`, `checkins` |
| F7 – Cổng hội viên | v2 | UC-F7.1, UC-F7.2, UC-F7.3, UC-F7.4, UC-F7.5 ([đặc tả](dac-ta-use-case-f7-cong-hoi-vien.md)) | `memberships`, `invoices` |
| F8 – Lớp học nhóm | v2 | UC-F8.0, UC-F8.1, UC-F8.2, UC-F8.3, UC-F8.4, UC-F8.5, UC-F8.6, UC-F8.7, UC-F8.8 ([đặc tả](dac-ta-use-case-f8-lop-hoc-nhom.md)) | `system_configs`, `rooms`, `class_types`, `class_schedules`, `class_sessions`, `class_bookings` |
| F9 – Huấn luyện viên (PT) | v2 | UC-F9.1, UC-F9.2, UC-F9.3, UC-F9.4, UC-F9.5, UC-F9.6, UC-F9.7 ([đặc tả](dac-ta-use-case-f9-huan-luyen-vien.md)) | `system_configs`, `body_metrics`, `trainer_availabilities`, `pt_sessions`, `workout_plans`, `workout_logs`, `staff` |
| F10 – Thông báo | v2 | UC-F10.1, UC-F10.2, UC-F10.3, UC-F10.4, UC-F10.5 ([đặc tả](dac-ta-use-case-f10-thong-bao.md)) | `notifications`, `class_bookings` |
| F11 – Thiết bị | v3 | Chưa đặc tả (TV-09) | `equipment`, `maintenance_logs` |
| F12 – Bán hàng tại quầy (POS) và kho | v3 | Chưa đặc tả (TV-09) | `products`, `inventories`, `stock_movements`, `invoices`, `invoice_items` |
| F13 – Nhân sự và lương | v3 | Chưa đặc tả (TV-09) | `pt_sessions`, `staff`, `shifts`, `attendances`, `payrolls` |
| F14 – Khuyến mãi | v3 | Chưa đặc tả (TV-09) | `promotions`, `invoices` |
| F15 – Bảo lưu và chuyển nhượng gói | v3 | Chưa đặc tả (TV-09) | `system_configs`, `membership_freezes`, `membership_transfers`, `invoice_items` |
| F16 – Báo cáo nâng cao | v3 | Chưa đặc tả (TV-09) | `invoice_items` |
| F17 – Nhật ký hoạt động (audit log) | v3 | Chưa đặc tả (TV-09) | `audit_logs` |
| F18 – Đa chi nhánh | v4 | Chưa đặc tả (TV-09) | `system_configs`, `branches` |
| F19 – Gamification | v4 | Chưa đặc tả (TV-09) | `badges`, `member_badges` |
| F20 – Check-in bằng nhận diện khuôn mặt | v5 | Chưa đặc tả (TV-09) | `face_embeddings`, `checkins` |

## 3. Truy vết từng use case F1–F10

### F1 – Tài khoản và phân quyền

Xem [đặc tả F1](dac-ta-use-case-f1-tai-khoan-va-phan-quyen.md).

| Mã UC · mục nguồn | Nội dung | Mã US | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- | --- |
| UC-F1.1 · 3.1 | Khách tự đăng ký tài khoản hội viên | US-F1.1.1 | `users`, `members`, `branches`, `refresh_tokens` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F1.2 · 3.2 | Đăng nhập bằng email | US-F1.2.1 | `users`, `refresh_tokens` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F1.3 · 3.3 | Làm mới phiên đăng nhập (Refresh Token) & Đăng xuất | US-F1.3.1 | `users`, `refresh_tokens` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F1.4 · 3.4 | Đổi mật khẩu | US-F1.4.1 | `users`, `refresh_tokens` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F1.5 · 3.5 | Quên mật khẩu và đặt lại mật khẩu (Sprint 2) | US-F1.5.1 | `users`, `password_resets`, `refresh_tokens` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F1.6 · 3.6 | Quản lý trạng thái tài khoản & Phân quyền truy cập API | US-F1.6.1 | `users`, `members`, `staff` | Vai trò lấy từ JWT; users/members/staff là dữ liệu nền cho quyền sở hữu và chi nhánh, không khẳng định mỗi request đều truy vấn cả ba bảng. TV-10. |

### F2 – Hồ sơ hội viên

Xem [đặc tả F2](dac-ta-use-case-f2-ho-so-hoi-vien.md).

| Mã UC · mục nguồn | Nội dung | Mã US | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- | --- |
| UC-F2.1 · 3.1 | Tạo hồ sơ hội viên tại quầy | US-F2.1.1 | `members`, `users`, `staff`, `branches` | Nhân viên lấy chi nhánh từ hồ sơ staff. Chủ chuỗi dùng CN01 trong v1–v3 (TV-11). |
| UC-F2.2 · 3.2 | Tạo tài khoản đăng nhập cho hồ sơ hội viên có sẵn | US-F2.2.1 | `members`, `users`, `password_resets` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F2.3 · 3.3 | Tra cứu, lọc và xem chi tiết hồ sơ hội viên | US-F2.3.1 | `members`, `users`, `staff` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F2.4 · 3.4 | Cập nhật hồ sơ, Ảnh đại diện & Ngừng hoạt động hội viên | US-F2.4.1, US-F2.4.2 | `members`, `users`, `refresh_tokens`, `member_avatars` | UC gồm cập nhật hồ sơ, trạng thái tài khoản và ảnh đại diện (ADR 0011). |
| UC-F2.5 · 3.5 | Ghi nhận và xem lịch sử chỉ số cơ thể (`body_metrics`) | US-F2.5.1 | `members`, `body_metrics`, `users` | Người ghi lưu users.id ở recorded_by; không tạo bảng chỉ số riêng cho PT. |

### F3 – Gói tập và gia hạn

Xem [đặc tả F3](dac-ta-use-case-f3-goi-tap-va-gia-han.md).

| Mã UC · mục nguồn | Nội dung | Mã US | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- | --- |
| UC-F3.1 · 3.1 | Xem danh mục gói tập | US-F3.1.1, US-F3.1.2 | `packages` | Theo mục dữ liệu của đặc tả. |
| UC-F3.2 · 3.2 | Thêm gói tập mới | US-F3.2.1 | `packages` | Theo mục dữ liệu của đặc tả. |
| UC-F3.3 · 3.3 | Sửa thông tin gói tập | US-F3.3.1 | `packages` | Theo mục dữ liệu của đặc tả. |
| UC-F3.4 · 3.4 | Ngừng bán gói tập | US-F3.4.1 | `packages` | Theo mục dữ liệu của đặc tả. |
| UC-F3.5 · 3.5 | Bán gói tập tại quầy (đăng ký mới / gia hạn) | US-F3.5.1 | `packages`, `invoices`, `invoice_items`, `memberships`, `payments`, `members` | payments chỉ được ghi ở bước F5; promotions chưa dùng ở v1. Bảng members dùng kiểm tra người mua. |

### F4 – Check-in tại quầy

Xem [đặc tả F4](dac-ta-use-case-f4-check-in-tai-quay.md).

| Mã UC · mục nguồn | Nội dung | Mã US | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- | --- |
| UC-F4.1 · 3.1 | Tra cứu hội viên và đối chiếu thông tin/ảnh tại quầy | US-F4.1.1 | `members`, `member_avatars`, `memberships`, `packages`, `class_bookings`, `class_sessions`, `class_schedules`, `class_types`, `rooms`, `staff`, `users` | Đọc ảnh theo ADR 0011 và lịch lớp để đối chiếu tại quầy. |
| UC-F4.2 · 3.2 | Thực hiện check-in thủ công tại quầy | US-F4.2.1 | `checkins`, `memberships`, `members`, `branches`, `users`, `staff`, `pt_sessions`, `class_bookings`, `class_sessions` | UC-F4.3 chạy trong giao dịch. Gói PT áp dụng khi có buổi BOOKED cùng ngày tại chi nhánh. |
| UC-F4.3 · 3.3 | Tự động đánh dấu có mặt lớp học nhóm khi check-in | US-F4.3.1 | `class_bookings`, `class_sessions`, `class_schedules`, `class_types`, `rooms` | Chỉ ghi arrived_at, không đổi sang ATTENDED; bảng lịch/loại/phòng phục vụ tên lớp, phòng trên phản hồi. |
| UC-F4.4 · 3.4 | Ghi nhận check-out cho hội viên | US-F4.4.1 | `checkins`, `staff` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F4.5 · 3.5 | Xem lịch sử lượt check-in theo ngày tại quầy | US-F4.5.1 | `checkins`, `members`, `member_avatars`, `memberships`, `packages`, `users`, `staff` | Đối chiếu luồng và quan hệ trong ERD. |

### F5 – Thanh toán tại quầy

Xem [đặc tả F5](dac-ta-use-case-f5-thanh-toan-tai-quay.md).

| Mã UC · mục nguồn | Nội dung | Mã US | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- | --- |
| UC-F5.1 · 3.1 | Sinh mã VietQR và xem chi tiết thanh toán hóa đơn | US-F5.1.1 | `invoices`, `branches`, `system_configs` | Theo mục dữ liệu của đặc tả. |
| UC-F5.2 · 3.2 | Ghi nhận thanh toán tại quầy và kích hoạt dịch vụ | US-F5.2.1 | `invoices`, `payments`, `memberships`, `invoice_items`, `staff`, `users` | Theo mục dữ liệu F5; nhánh chuyển nhượng có phí của F15 cần đặc tả riêng (TV-09). |
| UC-F5.3 · 3.3 | Tra cứu và quản lý hóa đơn tại quầy (và toàn chuỗi) | US-F5.3.1 | `invoices`, `memberships`, `invoice_items`, `payments`, `members`, `staff`, `branches` | Đọc lịch sử thu từ payments. Chủ chuỗi dùng danh mục chi nhánh để lọc hóa đơn. |
| UC-F5.4 · 3.4 | In và xuất hóa đơn PDF | US-F5.4.1 | `invoices`, `invoice_items`, `payments`, `branches`, `members` | Theo mục dữ liệu của đặc tả. |
| UC-F5.5 · 3.5 | Hoàn tiền hóa đơn (Refund) | US-F5.5.1 | `invoices`, `payments`, `memberships`, `audit_logs`, `invoice_items`, `checkins`, `pt_sessions`, `class_bookings`, `class_sessions` | Đối chiếu các lần sử dụng gói trước khi hoàn và ghi nhật ký thao tác. |

### F6 – Dashboard cơ bản

Xem [đặc tả F6](dac-ta-use-case-f6-dashboard-co-ban.md).

| Mã UC · mục nguồn | Nội dung | Mã US | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- | --- |
| UC-F6.1 · 3.1 | Xem số lượng hội viên đang có mặt tại phòng tập | US-F6.1.1 | `checkins`, `branches`, `staff` | Đọc branches để kiểm tra chi nhánh chủ chuỗi chọn có tồn tại (mục 3.1.C bước 2); staff xác định chi nhánh của nhân viên. |
| UC-F6.2 · 3.2 | Xem tổng lượt check-in và biểu đồ phân bổ theo giờ | US-F6.2.1 | `checkins`, `branches`, `staff` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F6.3 · 3.3 | Xem tổng doanh thu trong ngày tại quầy | US-F6.3.1 | `invoices`, `payments`, `members`, `branches`, `staff` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F6.4 · 3.4 | Xem danh sách cảnh báo gói tập sắp hết hạn | US-F6.4.1 | `memberships`, `members`, `member_avatars`, `packages`, `system_configs`, `branches`, `staff` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F6.5 · 3.5 | Xem danh sách lượt check-in mới nhất (Live Feed) | US-F6.5.1 | `checkins`, `members`, `member_avatars`, `memberships`, `packages`, `branches`, `staff` | Truy vấn mục 3.5.C bước 1 JOIN checkins, members, memberships và packages để lấy tên và loại gói; staff xác định chi nhánh của nhân viên. |

### F7 – Cổng hội viên

Xem [đặc tả F7](dac-ta-use-case-f7-cong-hoi-vien.md).

| Mã UC · mục nguồn | Nội dung | Mã US | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- | --- |
| UC-F7.1 · 3.1 | Xem gói tập | US-F7.1.1 | `members`, `memberships`, `packages` | Theo mục dữ liệu của đặc tả. |
| UC-F7.2 · 3.2 | Xem lịch sử check-in | US-F7.2.1 | `checkins`, `branches`, `members` | Theo mục dữ liệu của đặc tả. |
| UC-F7.3 · 3.3 | Xem hóa đơn | US-F7.3.1 | `invoices`, `invoice_items`, `system_configs`, `members` | Theo mục dữ liệu của đặc tả. |
| UC-F7.4 · 3.4 | Gửi yêu cầu mua gói | US-F7.4.1 | `invoices`, `invoice_items`, `memberships`, `packages`, `members`, `system_configs` | Theo mục dữ liệu của đặc tả. |
| UC-F7.5 · 3.5 | Gửi yêu cầu gia hạn gói | US-F7.5.1 | `invoices`, `invoice_items`, `memberships`, `packages`, `members`, `system_configs` | Theo mục dữ liệu của đặc tả. |

### F8 – Lớp học nhóm

Xem [đặc tả F8](dac-ta-use-case-f8-lop-hoc-nhom.md).

| Mã UC · mục nguồn | Nội dung | Mã US | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- | --- |
| UC-F8.0 · 3.1 | Xem lịch lớp học nhóm công khai | US-F8.0.1 | `class_sessions`, `class_types`, `class_schedules`, `branches`, `rooms`, `staff` | Đọc lịch công khai, tên lớp, phòng và huấn luyện viên. |
| UC-F8.1 · 3.2 | Đặt chỗ lớp nhóm | US-F8.1.1 | `class_bookings`, `class_sessions`, `class_schedules`, `class_types`, `members`, `memberships`, `packages`, `rooms`, `system_configs`, `staff`, `users` | Lớp đầy thì từ chối. Đặt thành công tạo BOOKED và ghi tài khoản thao tác (TV-02). |
| UC-F8.2 · 3.3 | Đã bỏ (ADR 0014) | US-F8.2.1 (đã bỏ) | — | Giữ mã theo [ADR 0014](../../decisions/0014-bo-danh-sach-cho-lop-hoc-nhom.md). |
| UC-F8.3 · 3.4 | Hủy chỗ lớp nhóm | US-F8.3.1 | `class_bookings`, `class_sessions`, `members`, `system_configs`, `audit_logs` | Hủy chỗ và cập nhật khóa khi đủ ngưỡng. Lấy lần khóa gần nhất từ nhật ký (TV-03). |
| UC-F8.4 · 3.5 | Đã bỏ (ADR 0014) | US-F8.4.1 (đã bỏ) | — | Giữ mã theo [ADR 0014](../../decisions/0014-bo-danh-sach-cho-lop-hoc-nhom.md). |
| UC-F8.5 · 3.6 | Xem lịch dạy lớp nhóm | US-F8.5.1 | `class_sessions`, `class_schedules`, `class_types`, `rooms`, `staff`, `users` | Đọc lịch dạy của huấn luyện viên. |
| UC-F8.6 · 3.7 | Điểm danh học viên lớp học nhóm | US-F8.6.1 | `class_bookings`, `members`, `class_sessions`, `staff`, `users`, `system_configs`, `audit_logs` | Ghi người điểm danh và lưu nhật ký khi sửa điểm danh hoặc gỡ khóa (TV-03, TV-05). |
| UC-F8.7 · 3.8 | Quản lý lịch lặp lớp học và phân công huấn luyện viên | US-F8.7.1 | `class_types`, `class_schedules`, `class_sessions`, `class_bookings`, `branches`, `rooms`, `staff`, `users` | Khi đổi giờ, đổi phòng hoặc hủy buổi, F8 phát sự kiện và F10 ghi notifications. Sĩ số theo TV-06. |
| UC-F8.8 · 3.9 | Sinh buổi học từ lịch lặp | US-F8.8.1 | `class_sessions`, `class_schedules`, `class_types`, `branches`, `rooms`, `staff` | Sinh buổi từ lịch lặp theo chi nhánh và phòng. |

### F9 – Huấn luyện viên (PT)

Xem [đặc tả F9](dac-ta-use-case-f9-huan-luyen-vien.md).

| Mã UC · mục nguồn | Nội dung | Mã US | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- | --- |
| UC-F9.1 · 3.1 | Khai báo lịch rảnh và xem lịch dạy | US-F9.1.1 | `trainer_availabilities`, `staff`, `pt_sessions`, `class_sessions` | Theo mục dữ liệu của đặc tả. |
| UC-F9.2 · 3.2 | Đặt buổi PT | US-F9.2.1 | `pt_sessions`, `trainer_availabilities`, `memberships`, `staff`, `members`, `class_sessions`, `system_configs` | Theo mục dữ liệu của đặc tả. |
| UC-F9.3 · 3.3 | Xác nhận buổi PT đã diễn ra | US-F9.3.1 | `pt_sessions`, `memberships`, `staff`, `invoice_items` | Đọc staff.commission_rate và invoice_items.amount/discount_amount qua memberships.invoice_item_id để chốt hoa hồng. |
| UC-F9.4 · 3.4 | Soạn giáo án | US-F9.4.1 | `workout_plans`, `members`, `pt_sessions`, `class_sessions`, `staff`, `class_bookings` | Ghi giáo án và kiểm tra quan hệ dạy theo đặc tả. |
| UC-F9.5 · 3.5 | Ghi nhật ký tập và chỉ số cơ thể | US-F9.5.1 | `workout_logs`, `body_metrics`, `members`, `staff`, `pt_sessions`, `class_sessions`, `class_bookings` | audit_logs chỉ là phương án lưu dấu người ghi được đặc tả đề cập, chưa coi là thay thế bắt buộc cho cột người ghi. TV-07, TV-08. |
| UC-F9.6 · 3.6 | Xem tiến độ học viên | US-F9.6.1 | `workout_logs`, `body_metrics`, `members`, `staff`, `pt_sessions`, `class_sessions`, `class_bookings` | Chỉ đọc để vẽ tiến độ; kiểm tra quan hệ dạy qua PT/lớp. TV-08. |
| UC-F9.7 · 3.7 | Hủy buổi PT | US-F9.7.1 | `pt_sessions`, `staff`, `members`, `audit_logs`, `notifications`, `system_configs` | Hủy theo quyền và hạn quy định, ghi nhật ký và phát thông báo theo đặc tả. |

### F10 – Thông báo

Xem [đặc tả F10](dac-ta-use-case-f10-thong-bao.md).

| Mã UC · mục nguồn | Nội dung | Mã US | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- | --- |
| UC-F10.1 · 3.1 | Gửi thông báo sắp hết hạn gói | US-F10.1.1 | `notifications`, `memberships`, `members`, `users`, `branches`, `system_configs`, `packages` | packages cung cấp tên gói trong nội dung thông báo qua memberships.package_id. |
| UC-F10.2 · 3.2 | Gửi thông báo nhắc lịch lớp | US-F10.2.1 | `notifications`, `class_bookings`, `class_sessions`, `members`, `users`, `branches` | Mốc nhắc trước giờ lớp cần thống nhất theo TV-12. |
| UC-F10.3 · 3.3 | Đã bỏ (ADR 0014) | US-F10.3.1 (đã bỏ) | — | Giữ mã theo [ADR 0014](../../decisions/0014-bo-danh-sach-cho-lop-hoc-nhom.md). |
| UC-F10.4 · 3.4 | Gửi thông báo đổi giờ lớp | US-F10.4.1 | `notifications`, `class_sessions`, `class_bookings`, `members`, `users`, `branches` | Lượt đặt được giữ khi đổi giờ. Thông báo đổi phòng và hủy buổi nối với tiêu chí của UC-F8.7 (TV-14). |
| UC-F10.5 · 3.5 | Xem thông báo trong web | US-F10.5.1 | `notifications` | Theo mục dữ liệu của đặc tả. |

## 4. Truy vết ngược đủ 40 bảng

Cột chức năng gốc lấy theo thiết kế dữ liệu. Cột use case được tổng hợp từ mục 3, gồm cả các chức năng đọc hoặc ghi dùng chung.

| Bảng ERD | Chức năng gốc | Use case liên quan trong mục 3 |
| --- | --- | --- |
| `users` | F1 | UC-F1.1, UC-F1.2, UC-F1.3, UC-F1.4, UC-F1.5, UC-F1.6, UC-F2.1, UC-F2.2, UC-F2.3, UC-F2.4, UC-F2.5, UC-F4.1, UC-F4.2, UC-F4.5, UC-F5.2, UC-F8.1, UC-F8.5, UC-F8.6, UC-F8.7, UC-F10.1, UC-F10.2, UC-F10.4 |
| `refresh_tokens` | F1 | UC-F1.1, UC-F1.2, UC-F1.3, UC-F1.4, UC-F1.5, UC-F2.4 |
| `password_resets` | F1 | UC-F1.5, UC-F2.2 |
| `audit_logs` | F17 | UC-F5.5, UC-F8.3, UC-F8.6, UC-F9.7 |
| `notifications` | F10 | UC-F9.7, UC-F10.1, UC-F10.2, UC-F10.4, UC-F10.5 |
| `system_configs` | F6, F8, F9, F15, F18 | UC-F5.1, UC-F6.4, UC-F7.3, UC-F7.4, UC-F7.5, UC-F8.1, UC-F8.3, UC-F8.6, UC-F9.2, UC-F9.7, UC-F10.1 |
| `branches` | F18, F4 | UC-F1.1, UC-F2.1, UC-F4.2, UC-F5.1, UC-F5.3, UC-F5.4, UC-F6.1, UC-F6.2, UC-F6.3, UC-F6.4, UC-F6.5, UC-F7.2, UC-F8.0, UC-F8.7, UC-F8.8, UC-F10.1, UC-F10.2, UC-F10.4 |
| `rooms` | F8 | UC-F4.1, UC-F4.3, UC-F8.0, UC-F8.1, UC-F8.5, UC-F8.7, UC-F8.8 |
| `equipment` | F11 | Chưa có UC trong F1–F10 (TV-09). |
| `maintenance_logs` | F11 | Chưa có UC trong F1–F10 (TV-09). |
| `members` | F2 | UC-F1.1, UC-F1.6, UC-F2.1, UC-F2.2, UC-F2.3, UC-F2.4, UC-F2.5, UC-F3.5, UC-F4.1, UC-F4.2, UC-F4.5, UC-F5.3, UC-F5.4, UC-F6.3, UC-F6.4, UC-F6.5, UC-F7.1, UC-F7.2, UC-F7.3, UC-F7.4, UC-F7.5, UC-F8.1, UC-F8.3, UC-F8.6, UC-F9.2, UC-F9.4, UC-F9.5, UC-F9.6, UC-F9.7, UC-F10.1, UC-F10.2, UC-F10.4 |
| `member_avatars` | F2 (ADR 0011) | UC-F2.4, UC-F4.1, UC-F4.5, UC-F6.4, UC-F6.5 |
| `face_embeddings` | F20 | Chưa có UC trong F1–F10 (TV-09). |
| `body_metrics` | F2, F9 | UC-F2.5, UC-F9.5, UC-F9.6 |
| `packages` | F3 | UC-F3.1, UC-F3.2, UC-F3.3, UC-F3.4, UC-F3.5, UC-F4.1, UC-F4.5, UC-F6.4, UC-F6.5, UC-F7.1, UC-F7.4, UC-F7.5, UC-F8.1, UC-F10.1 |
| `memberships` | F3, F4, F7 | UC-F3.5, UC-F4.1, UC-F4.2, UC-F4.5, UC-F5.2, UC-F5.3, UC-F5.5, UC-F6.4, UC-F6.5, UC-F7.1, UC-F7.4, UC-F7.5, UC-F8.1, UC-F9.2, UC-F9.3, UC-F10.1 |
| `membership_freezes` | F15 | Chưa có UC trong F1–F10 (TV-09). |
| `membership_transfers` | F15 | Chưa có UC trong F1–F10 (TV-09). |
| `checkins` | F4, F6, F20 | UC-F4.2, UC-F4.4, UC-F4.5, UC-F5.5, UC-F6.1, UC-F6.2, UC-F6.5, UC-F7.2 |
| `class_types` | F8 | UC-F4.1, UC-F4.3, UC-F8.0, UC-F8.1, UC-F8.5, UC-F8.7, UC-F8.8 |
| `class_schedules` | F8 | UC-F4.1, UC-F4.3, UC-F8.0, UC-F8.1, UC-F8.5, UC-F8.7, UC-F8.8 |
| `class_sessions` | F8 | UC-F4.1, UC-F4.2, UC-F4.3, UC-F5.5, UC-F8.0, UC-F8.1, UC-F8.3, UC-F8.5, UC-F8.6, UC-F8.7, UC-F8.8, UC-F9.1, UC-F9.2, UC-F9.4, UC-F9.5, UC-F9.6, UC-F10.2, UC-F10.4 |
| `class_bookings` | F8, F10 | UC-F4.1, UC-F4.2, UC-F4.3, UC-F5.5, UC-F8.1, UC-F8.3, UC-F8.6, UC-F8.7, UC-F9.4, UC-F9.5, UC-F9.6, UC-F10.2, UC-F10.4 |
| `trainer_availabilities` | F9 | UC-F9.1, UC-F9.2 |
| `pt_sessions` | F9, F13 | UC-F4.2, UC-F5.5, UC-F9.1, UC-F9.2, UC-F9.3, UC-F9.4, UC-F9.5, UC-F9.6, UC-F9.7 |
| `workout_plans` | F9 | UC-F9.4 |
| `workout_logs` | F9 | UC-F9.5, UC-F9.6 |
| `products` | F12 | Chưa có UC trong F1–F10 (TV-09). |
| `inventories` | F12 | Chưa có UC trong F1–F10 (TV-09). |
| `stock_movements` | F12 | Chưa có UC trong F1–F10 (TV-09). |
| `promotions` | F14 | Chưa có UC trong F1–F10 (TV-09). |
| `invoices` | F5, F7, F12, F14 | UC-F3.5, UC-F5.1, UC-F5.2, UC-F5.3, UC-F5.4, UC-F5.5, UC-F6.3, UC-F7.3, UC-F7.4, UC-F7.5 |
| `invoice_items` | F5, F12, F15, F16 | UC-F3.5, UC-F5.2, UC-F5.3, UC-F5.4, UC-F5.5, UC-F7.3, UC-F7.4, UC-F7.5, UC-F9.3 |
| `payments` | F5 | UC-F3.5, UC-F5.2, UC-F5.3, UC-F5.4, UC-F5.5, UC-F6.3 |
| `staff` | F13, F9 | UC-F1.6, UC-F2.1, UC-F2.3, UC-F4.1, UC-F4.2, UC-F4.4, UC-F4.5, UC-F5.2, UC-F5.3, UC-F6.1, UC-F6.2, UC-F6.3, UC-F6.4, UC-F6.5, UC-F8.0, UC-F8.1, UC-F8.5, UC-F8.6, UC-F8.7, UC-F8.8, UC-F9.1, UC-F9.2, UC-F9.3, UC-F9.4, UC-F9.5, UC-F9.6, UC-F9.7 |
| `shifts` | F13 | Chưa có UC trong F1–F10 (TV-09). |
| `attendances` | F13 | Chưa có UC trong F1–F10 (TV-09). |
| `payrolls` | F13 | Chưa có UC trong F1–F10 (TV-09). |
| `badges` | F19 | Chưa có UC trong F1–F10 (TV-09). |
| `member_badges` | F19 | Chưa có UC trong F1–F10 (TV-09). |

## 5. Điểm lệch và trạng thái xử lý

| Mã | Use case / dữ liệu | Trạng thái và căn cứ |
| --- | --- | --- |
| TV-01 | UC-F8.2, UC-F8.3, UC-F10.3 · mốc ân hạn | Đóng theo ADR 0014. Hủy chỗ trả chỗ ngay và không còn ân hạn. |
| TV-02 | UC-F8.1 · `class_bookings.created_by` | Đã đồng bộ. BR-F8-01 và ERD bắt buộc ghi tài khoản bấm đặt, kể cả khi lễ tân thao tác hộ. |
| TV-03 | UC-F8.3, UC-F8.6 · mốc khóa gần nhất | Đã có cách lưu trong triển khai F8. `booking_locked_until` giữ hạn khóa hiện tại. Mốc tính vi phạm lấy từ `audit_logs` có hành động `CLASS_BOOKING_LOCK` ([PR #240](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/240)). Sửa điểm danh giữ nguyên khóa theo ADR 0014. |
| TV-04 | UC-F8.2, UC-F10.3 · `notifications.user_id` | Đóng theo ADR 0014. Hai UC bị bỏ nên không còn trường hợp phải xác định ân hạn từ thông báo cho hồ sơ chưa có tài khoản. |
| TV-05 | UC-F8.6 · `class_bookings.attendance_marked_by` | Đã đồng bộ. BR-F8.6-01 và ERD ghi người điểm danh hoặc sửa gần nhất, đồng thời lưu giá trị cũ và mới vào `audit_logs` khi sửa. |
| TV-06 | UC-F8.7 · sức chứa buổi lớp | Đã đồng bộ theo ADR 0014. Sức chứa của buổi cụ thể không giảm dưới số chỗ `BOOKED`, `ATTENDED`, `NO_SHOW`. Sửa lịch lặp và sửa buổi cụ thể là hai thao tác riêng trong UC-F8.7. |
| TV-07 | UC-F9.5 · người ghi `workout_logs` | Còn mở. Khi `pt_session_id` rỗng, SRS F9 đề cập `audit_logs` như một phương án. Nhóm F9 và trưởng nhóm cần chốt cách truy ra PT đã ghi nhật ký. |
| TV-08 | UC-F2.5, UC-F9.5, UC-F9.6 · `body_metrics` | Còn mở. ERD cho phép nhiều số đo trong cùng ngày. Nhóm F2/F9 và trưởng nhóm cần thống nhất cách hiện trên biểu đồ. |
| TV-09 | F11–F20 | Theo dõi khi có đặc tả. Cập nhật hai chiều sau khi từng nhóm bổ sung UC. |
| TV-10 | UC-F1.6 · quản lý trạng thái tài khoản | Còn mở. Tên UC nhắc quản lý trạng thái, còn đặc tả hiện tập trung vào phân quyền. Nhóm F1 và trưởng nhóm cần thống nhất phạm vi của UC. |
| TV-11 | UC-F2.1 · nguồn chi nhánh của chủ chuỗi | Đã đồng bộ. §3.1 của F2 dùng CN01 trong v1–v3 cho chủ chuỗi và dùng `staff.branch_id` cho nhân viên gắn với chi nhánh. |
| TV-12 | UC-F10.2 · mốc gửi nhắc lịch lớp | Còn mở trong SRS. [PR #269](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/269) triển khai mốc 24 giờ. Trưởng nhóm và nhóm F10 cần xác nhận và ghi cùng một mốc vào SRS. |
| TV-13 | UC-F8.1, UC-F8.2 · thứ tự danh sách chờ | Đóng theo ADR 0014. Lớp đầy từ chối đặt chỗ và đã bỏ hàng chờ FIFO. |
| TV-14 | UC-F8.7, UC-F10.4 · thông báo đổi phòng và hủy buổi | Đã đồng bộ hai SRS theo ADR 0014. F8 phát sự kiện và F10 tạo, gửi thông báo. Tiêu chí đổi phòng và hủy buổi được dẫn từ UC-F8.7. |

## 6. Cách cập nhật và tự kiểm tra

1. Khi thay đổi use case, cập nhật mục 2–4. Giữ mã đã bỏ và bỏ liên kết dữ liệu của mã đó khỏi chiều ngược.
2. Đối chiếu mã UC → US → AC → TC theo ADR 0012 và chạy `node scripts/export-truy-vet.cjs --check` trên cây có đủ đặc tả. Khi cập nhật ma trận kiểm thử, sinh lại bằng `node scripts/export-truy-vet.cjs`.
3. Đối chiếu tên bảng với ERD. Khi đóng mục TV, ghi quyết định hoặc thay đổi đã đồng bộ.
