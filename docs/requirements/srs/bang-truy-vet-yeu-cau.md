# Bảng truy vết chức năng – use case – bảng dữ liệu

Cập nhật: 07/11/2026 · Nguyễn Anh Tú · Nhiệm vụ [#14](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/14) · WBS 1.2.2

## 1. Phạm vi và cách đọc

Bảng này nối 20 chức năng F1–F20 với 56 use case đã có đặc tả và 39 bảng ERD. Trong 56 use case, 47 nằm trên main và 9 thuộc F8 trong PR nháp #79. Đây là truy vết yêu cầu, không phải xác nhận chức năng đã được lập trình hoặc nghiệm thu.

- Mốc đối chiếu main: `81f6ae05f534f2402bde5d89a8c245c058f93274`. Nghiệp vụ theo [bản chốt phạm vi](../ban-chot-pham-vi.md); dữ liệu theo [ERD v2](../../design/erd/erd-v2.dbml).
- Mục 2 giữ nguyên ánh xạ bảng ↔ chức năng từ [thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md). Ánh xạ gốc mô tả chức năng chính của bảng, không liệt kê mọi nơi đọc/ghi bảng đó.
- Mục 3 bổ sung truy vết từng use case từ luồng, mục dữ liệu và quan hệ ERD. Danh sách gồm bảng đọc, ghi hoặc kiểm tra quyền; không có nghĩa mọi bảng đều bị cập nhật. Các phụ thuộc dùng chung như người thao tác/chi nhánh chỉ nêu khi cần giải thích luồng, không thay thế thiết kế truy vấn chi tiết.
- F8 dùng [bản nháp cố định tại commit 5233c53](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/5233c53ab4d014f5344ac6de112e67d467f25c29/docs/requirements/srs/dac-ta-use-case-f8-lop-hoc-nhom.md) của [PR #79](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79), đang mở và Draft lúc đối chiếu. Sau khi merge cần đổi nguồn sang file trong main và rà lại các dòng F8.
- F11–F20 chưa có đặc tả UC trong cây main được đối chiếu; giữ ánh xạ bảng gốc, ghi “Chưa đặc tả”, không tự đặt mã UC. Version là lộ trình chức năng, không phải số sprint.
- TV-01…TV-13 ở mục 5 là các điểm cần đồng bộ hoặc cần chốt; việc ghi nhận tại đây chưa đồng nghĩa đã báo qua comment hay đã được trưởng nhóm duyệt.

## 2. Tổng quan F1–F20 theo ánh xạ gốc

| Chức năng | Version | Mã use case / nguồn | Bảng trong ánh xạ gốc |
| --- | --- | --- | --- |
| F1 – Tài khoản và phân quyền | v1 | UC-F1.1, UC-F1.2, UC-F1.3, UC-F1.4, UC-F1.5, UC-F1.6 ([đặc tả](dac-ta-use-case-f1-tai-khoan-va-phan-quyen.md)) | `users`, `refresh_tokens`, `password_resets` |
| F2 – Hồ sơ hội viên | v1 | UC-F2.1, UC-F2.2, UC-F2.3, UC-F2.4, UC-F2.5 ([đặc tả](dac-ta-use-case-f2-ho-so-hoi-vien.md)) | `members`, `body_metrics` |
| F3 – Gói tập và gia hạn | v1 | UC-F3.1, UC-F3.2, UC-F3.3, UC-F3.4, UC-F3.5 ([đặc tả](dac-ta-use-case-f3-goi-tap-va-gia-han.md)) | `packages`, `memberships` |
| F4 – Check-in tại quầy | v1 | UC-F4.1, UC-F4.2, UC-F4.3, UC-F4.4, UC-F4.5 ([đặc tả](dac-ta-use-case-f4-check-in-tai-quay.md)) | `branches`, `memberships`, `checkins` |
| F5 – Thanh toán tại quầy | v1 | UC-F5.1, UC-F5.2, UC-F5.3, UC-F5.4, UC-F5.5 ([đặc tả](dac-ta-use-case-f5-thanh-toan-tai-quay.md)) | `invoices`, `invoice_items`, `payments` |
| F6 – Dashboard cơ bản | v1 | UC-F6.1, UC-F6.2, UC-F6.3, UC-F6.4, UC-F6.5 ([đặc tả](dac-ta-use-case-f6-dashboard-co-ban.md)) | `system_configs`, `checkins` |
| F7 – Cổng hội viên | v2 | UC-F7.1, UC-F7.2, UC-F7.3, UC-F7.4, UC-F7.5 ([đặc tả](dac-ta-use-case-f7-cong-hoi-vien.md)) | `memberships`, `invoices` |
| F8 – Lớp học nhóm | v2 | UC-F8.0, UC-F8.1, UC-F8.2, UC-F8.3, UC-F8.4, UC-F8.5, UC-F8.6, UC-F8.7, UC-F8.8 ([đặc tả](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/5233c53ab4d014f5344ac6de112e67d467f25c29/docs/requirements/srs/dac-ta-use-case-f8-lop-hoc-nhom.md)) **Nháp** | `system_configs`, `rooms`, `class_types`, `class_schedules`, `class_sessions`, `class_bookings` |
| F9 – Huấn luyện viên (PT) | v2 | UC-F9.1, UC-F9.2, UC-F9.3, UC-F9.4, UC-F9.5, UC-F9.6 ([đặc tả](dac-ta-use-case-f9-huan-luyen-vien.md)) | `system_configs`, `body_metrics`, `trainer_availabilities`, `pt_sessions`, `workout_plans`, `workout_logs`, `staff` |
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

Các dòng bổ sung quan hệ từ luồng/ERD được ghi rõ ở cột cuối. Không suy ra cần tạo bảng mới chỉ vì một bảng được dùng chung giữa nhiều chức năng.

### F1 – Tài khoản và phân quyền

Nguồn: [đặc tả F1](dac-ta-use-case-f1-tai-khoan-va-phan-quyen.md). Số mục trong cột đầu là vị trí của use case trong tài liệu nguồn.

| Mã UC và mục nguồn | Nội dung | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- |
| UC-F1.1 · 3.1 | Khách tự đăng ký tài khoản hội viên | `users`, `members`, `branches`, `refresh_tokens` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F1.2 · 3.2 | Đăng nhập bằng email | `users`, `refresh_tokens` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F1.3 · 3.3 | Làm mới phiên đăng nhập (Refresh Token) & Đăng xuất | `users`, `refresh_tokens` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F1.4 · 3.4 | Đổi mật khẩu | `users`, `refresh_tokens` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F1.5 · 3.5 | Quên mật khẩu và đặt lại mật khẩu (Sprint 2) | `users`, `password_resets`, `refresh_tokens` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F1.6 · 3.6 | Phân quyền truy cập API theo vai trò | `users`, `members`, `staff` | Vai trò lấy từ JWT; users/members/staff là dữ liệu nền cho quyền sở hữu và chi nhánh, không khẳng định mỗi request đều truy vấn cả ba bảng. TV-10. |

### F2 – Hồ sơ hội viên

Nguồn: [đặc tả F2](dac-ta-use-case-f2-ho-so-hoi-vien.md). Số mục trong cột đầu là vị trí của use case trong tài liệu nguồn.

| Mã UC và mục nguồn | Nội dung | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- |
| UC-F2.1 · 3.1 | Tạo hồ sơ hội viên tại quầy | `members`, `users`, `staff`, `branches` | Chi nhánh từ staff.branch_id; chủ chuỗi không có staff. TV-11. |
| UC-F2.2 · 3.2 | Tạo tài khoản đăng nhập cho hồ sơ hội viên có sẵn | `members`, `users`, `password_resets` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F2.3 · 3.3 | Tra cứu, lọc và xem chi tiết hồ sơ hội viên | `members`, `users`, `staff` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F2.4 · 3.4 | Cập nhật hồ sơ & Ngừng hoạt động hội viên | `members`, `users`, `refresh_tokens` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F2.5 · 3.5 | Ghi nhận và xem lịch sử chỉ số cơ thể (`body_metrics`) | `members`, `body_metrics`, `users` | Người ghi lưu users.id ở recorded_by; không tạo bảng chỉ số riêng cho PT. |

### F3 – Gói tập và gia hạn

Nguồn: [đặc tả F3](dac-ta-use-case-f3-goi-tap-va-gia-han.md). Số mục trong cột đầu là vị trí của use case trong tài liệu nguồn.

| Mã UC và mục nguồn | Nội dung | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- |
| UC-F3.1 · 3.1 | Xem danh mục gói tập | `packages` | Theo mục dữ liệu của đặc tả. |
| UC-F3.2 · 3.2 | Thêm gói tập mới | `packages` | Theo mục dữ liệu của đặc tả. |
| UC-F3.3 · 3.3 | Sửa thông tin gói tập | `packages` | Theo mục dữ liệu của đặc tả. |
| UC-F3.4 · 3.4 | Ngừng bán gói tập | `packages` | Theo mục dữ liệu của đặc tả. |
| UC-F3.5 · 3.5 | Bán gói tập tại quầy (đăng ký mới / gia hạn) | `packages`, `invoices`, `invoice_items`, `memberships`, `payments`, `members` | payments chỉ được ghi ở bước F5; promotions chưa dùng ở v1. Bảng members dùng kiểm tra người mua. |

### F4 – Check-in tại quầy

Nguồn: [đặc tả F4](dac-ta-use-case-f4-check-in-tai-quay.md). Số mục trong cột đầu là vị trí của use case trong tài liệu nguồn.

| Mã UC và mục nguồn | Nội dung | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- |
| UC-F4.1 · 3.1 | Tra cứu hội viên và đối chiếu thông tin/ảnh tại quầy | `members`, `memberships`, `staff`, `users` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F4.2 · 3.2 | Thực hiện check-in thủ công tại quầy | `checkins`, `memberships`, `members`, `branches`, `users`, `staff`, `class_bookings`, `class_sessions` | Bao gồm UC-F4.3 được gọi trong giao dịch; SESSION trừ buổi cuối còn dời gói UPCOMING. |
| UC-F4.3 · 3.3 | Tự động đánh dấu có mặt lớp học nhóm khi check-in | `class_bookings`, `class_sessions`, `class_schedules`, `class_types`, `rooms` | Chỉ ghi arrived_at, không đổi sang ATTENDED; bảng lịch/loại/phòng phục vụ tên lớp, phòng trên phản hồi. |
| UC-F4.4 · 3.4 | Ghi nhận check-out cho hội viên | `checkins`, `staff` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F4.5 · 3.5 | Xem lịch sử lượt check-in theo ngày tại quầy | `checkins`, `members`, `memberships`, `packages`, `users`, `staff` | Đối chiếu luồng và quan hệ trong ERD. |

### F5 – Thanh toán tại quầy

Nguồn: [đặc tả F5](dac-ta-use-case-f5-thanh-toan-tai-quay.md). Số mục trong cột đầu là vị trí của use case trong tài liệu nguồn.

| Mã UC và mục nguồn | Nội dung | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- |
| UC-F5.1 · 3.1 | Sinh mã VietQR và xem chi tiết thanh toán hóa đơn | `invoices`, `branches`, `system_configs` | Theo mục dữ liệu của đặc tả. |
| UC-F5.2 · 3.2 | Ghi nhận thanh toán tại quầy và kích hoạt dịch vụ | `invoices`, `payments`, `memberships`, `invoice_items` | Theo mục dữ liệu F5; nhánh chuyển nhượng có phí của F15 cần đặc tả riêng (TV-09). |
| UC-F5.3 · 3.3 | Tra cứu và quản lý hóa đơn tại quầy (và toàn chuỗi) | `invoices`, `memberships`, `members`, `invoice_items` | Đọc members để tìm theo tên/số điện thoại; qua invoice_items xác định memberships cần hủy. Không có thao tác payments trong luồng này. |
| UC-F5.4 · 3.4 | In và xuất hóa đơn PDF | `invoices`, `invoice_items`, `payments`, `branches`, `members` | Theo mục dữ liệu của đặc tả. |
| UC-F5.5 · 3.5 | Hoàn tiền hóa đơn (Refund) | `invoices`, `payments`, `memberships`, `audit_logs`, `invoice_items` | Đọc invoice_items để tìm gói liên quan theo mục 3.5.C bước 5d. Phải ghi audit_logs khi hoàn tiền theo ADR 0004, không đợi màn hình F17 ở v3. |

### F6 – Dashboard cơ bản

Nguồn: [đặc tả F6](dac-ta-use-case-f6-dashboard-co-ban.md). Số mục trong cột đầu là vị trí của use case trong tài liệu nguồn.

| Mã UC và mục nguồn | Nội dung | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- |
| UC-F6.1 · 3.1 | Xem số lượng hội viên đang có mặt tại phòng tập | `checkins`, `staff`, `branches` | Đọc branches để kiểm tra chi nhánh chủ chuỗi chọn có tồn tại (mục 3.1.C bước 2); staff xác định chi nhánh của nhân viên. |
| UC-F6.2 · 3.2 | Xem tổng lượt check-in và biểu đồ phân bổ theo giờ | `checkins`, `branches`, `staff` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F6.3 · 3.3 | Xem tổng doanh thu trong ngày tại quầy | `invoices`, `payments`, `members`, `staff` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F6.4 · 3.4 | Xem danh sách cảnh báo gói tập sắp hết hạn | `memberships`, `members`, `packages`, `system_configs`, `staff` | Đối chiếu luồng và quan hệ trong ERD. |
| UC-F6.5 · 3.5 | Xem danh sách lượt check-in mới nhất (Live Feed) | `checkins`, `members`, `staff`, `memberships`, `packages` | Truy vấn mục 3.5.C bước 1 JOIN checkins, members, memberships và packages để lấy tên và loại gói; staff xác định chi nhánh của nhân viên. |

### F7 – Cổng hội viên

Nguồn: [đặc tả F7](dac-ta-use-case-f7-cong-hoi-vien.md). Số mục trong cột đầu là vị trí của use case trong tài liệu nguồn.

| Mã UC và mục nguồn | Nội dung | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- |
| UC-F7.1 · 3.1 | Xem gói tập | `members`, `memberships`, `packages` | Theo mục dữ liệu của đặc tả. |
| UC-F7.2 · 3.2 | Xem lịch sử check-in | `checkins`, `branches`, `members` | Theo mục dữ liệu của đặc tả. |
| UC-F7.3 · 3.3 | Xem hóa đơn | `invoices`, `invoice_items`, `system_configs`, `members` | Theo mục dữ liệu của đặc tả. |
| UC-F7.4 · 3.4 | Gửi yêu cầu mua gói | `invoices`, `invoice_items`, `memberships`, `packages`, `members`, `system_configs` | Theo mục dữ liệu của đặc tả. |
| UC-F7.5 · 3.5 | Gửi yêu cầu gia hạn gói | `invoices`, `invoice_items`, `memberships`, `packages`, `members`, `system_configs` | Theo mục dữ liệu của đặc tả. |

### F8 – Lớp học nhóm (bản nháp)

Nguồn: [đặc tả F8](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/5233c53ab4d014f5344ac6de112e67d467f25c29/docs/requirements/srs/dac-ta-use-case-f8-lop-hoc-nhom.md). Số mục trong cột đầu là vị trí của use case trong tài liệu nguồn.

| Mã UC và mục nguồn | Nội dung | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- |
| UC-F8.0 · 3.1 | Xem lịch lớp học nhóm công khai | `class_sessions`, `class_types`, `class_schedules`, `branches`, `rooms`, `staff` | Bản nháp. Theo mục dữ liệu của đặc tả. |
| UC-F8.1 · 3.2 | Đặt chỗ lớp nhóm | `class_bookings`, `class_sessions`, `class_schedules`, `class_types`, `members`, `memberships`, `packages`, `rooms`, `system_configs`, `staff`, `users` | Bản nháp; TV-02 (người tạo), TV-13 (thứ tự FIFO). |
| UC-F8.2 · 3.3 | Tự động đưa hội viên từ danh sách chờ vào lớp | `class_bookings`, `notifications`, `class_sessions`, `members`, `memberships`, `packages`, `system_configs`, `branches`, `users` | Bản nháp; TV-01, TV-03, TV-04, TV-13. |
| UC-F8.3 · 3.4 | Hủy chỗ lớp nhóm | `class_bookings`, `members`, `class_sessions`, `system_configs`, `notifications`, `users` | Bản nháp; TV-01, TV-03. |
| UC-F8.4 · 3.5 | Xác nhận lại hoặc từ chối khi đổi giờ lớp | `class_sessions`, `class_bookings`, `notifications`, `members`, `system_configs`, `users` | Bản nháp. Theo mục dữ liệu của đặc tả. |
| UC-F8.5 · 3.6 | Xem lịch dạy lớp nhóm | `staff`, `users`, `class_sessions`, `class_schedules`, `class_types`, `rooms`, `class_bookings` | Bản nháp. Theo mục dữ liệu của đặc tả. |
| UC-F8.6 · 3.7 | Điểm danh học viên lớp học nhóm | `class_bookings`, `members`, `class_sessions`, `staff`, `users`, `system_configs` | Bản nháp; TV-03, TV-05. |
| UC-F8.7 · 3.8 | Quản lý lịch lặp lớp học và phân công huấn luyện viên | `class_types`, `class_schedules`, `class_sessions`, `class_bookings`, `notifications`, `branches`, `rooms`, `staff`, `users` | Bản nháp; TV-06 (giảm sức chứa). |
| UC-F8.8 · 3.9 | Sinh buổi học từ lịch lặp | `class_sessions`, `class_schedules`, `class_types`, `branches`, `rooms`, `staff` | Bản nháp. Theo mục dữ liệu của đặc tả. |

### F9 – Huấn luyện viên (PT)

Nguồn: [đặc tả F9](dac-ta-use-case-f9-huan-luyen-vien.md). Số mục trong cột đầu là vị trí của use case trong tài liệu nguồn.

| Mã UC và mục nguồn | Nội dung | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- |
| UC-F9.1 · 3.1 | Khai báo lịch rảnh và xem lịch dạy | `trainer_availabilities`, `staff`, `pt_sessions`, `class_sessions` | Theo mục dữ liệu của đặc tả. |
| UC-F9.2 · 3.2 | Đặt buổi PT | `pt_sessions`, `trainer_availabilities`, `memberships`, `staff`, `members`, `class_sessions`, `system_configs` | Theo mục dữ liệu của đặc tả. |
| UC-F9.3 · 3.3 | Xác nhận buổi PT đã diễn ra | `pt_sessions`, `memberships`, `staff`, `invoice_items` | Đọc staff.commission_rate và invoice_items.amount/discount_amount qua memberships.invoice_item_id để chốt hoa hồng. |
| UC-F9.4 · 3.4 | Soạn giáo án | `workout_plans`, `members`, `pt_sessions`, `class_sessions`, `staff`, `class_bookings` | class_bookings nối hội viên với lớp do PT dạy để kiểm tra quan hệ dạy. |
| UC-F9.5 · 3.5 | Ghi nhật ký tập và chỉ số cơ thể | `workout_logs`, `body_metrics`, `members`, `staff`, `pt_sessions`, `class_sessions`, `class_bookings` | audit_logs chỉ là phương án lưu dấu người ghi được đặc tả đề cập, chưa coi là thay thế bắt buộc cho cột người ghi. TV-07, TV-08. |
| UC-F9.6 · 3.6 | Xem tiến độ học viên | `workout_logs`, `body_metrics`, `members`, `staff`, `pt_sessions`, `class_sessions`, `class_bookings` | Chỉ đọc để vẽ tiến độ; kiểm tra quan hệ dạy qua PT/lớp. TV-08. |

### F10 – Thông báo

Nguồn: [đặc tả F10](dac-ta-use-case-f10-thong-bao.md). Số mục trong cột đầu là vị trí của use case trong tài liệu nguồn.

| Mã UC và mục nguồn | Nội dung | Bảng liên quan | Ghi chú truy vết |
| --- | --- | --- | --- |
| UC-F10.1 · 3.1 | Gửi thông báo sắp hết hạn gói | `notifications`, `memberships`, `members`, `users`, `branches`, `system_configs`, `packages` | packages cung cấp tên gói trong nội dung thông báo qua memberships.package_id. |
| UC-F10.2 · 3.2 | Gửi thông báo nhắc lịch lớp | `notifications`, `class_bookings`, `class_sessions`, `members`, `users`, `branches` | Thời điểm gửi nhắc trước giờ bắt đầu lớp tập nhóm còn cần chốt — xem TV-12. Chưa thêm system_configs vì chưa chốt dùng cấu hình. |
| UC-F10.3 · 3.3 | Gửi thông báo được đưa vào lớp từ danh sách chờ | `class_bookings`, `notifications`, `class_sessions`, `members`, `users`, `branches`, `system_configs` | TV-01: mốc ân hạn chưa đồng bộ với quyết định F8; F8 chịu trách nhiệm đưa vào lớp, cần thống nhất ranh giới ghi dữ liệu với F10. |
| UC-F10.4 · 3.4 | Gửi thông báo đổi giờ lớp | `notifications`, `class_sessions`, `class_bookings`, `members`, `users`, `branches`, `system_configs` | Theo mục dữ liệu của đặc tả. |
| UC-F10.5 · 3.5 | Xem thông báo trong web | `notifications` | Theo mục dữ liệu của đặc tả. |

## 4. Truy vết ngược đủ 39 bảng

Cột chức năng gốc giữ nguyên nguồn thiết kế dữ liệu. Cột UC liệt kê các dòng mục 3 có dùng bảng; vì vậy có thể xuất hiện UC ngoài chức năng gốc. “Chưa có UC” chỉ là thiếu đặc tả trong phạm vi nguồn đã đối chiếu, không có nghĩa bảng thừa. Các UC-F8.* đều là nháp.

| Bảng ERD | Chức năng gốc | Use case liên quan trong mục 3 |
| --- | --- | --- |
| `users` | F1 | UC-F1.1, UC-F1.2, UC-F1.3, UC-F1.4, UC-F1.5, UC-F1.6, UC-F2.1, UC-F2.2, UC-F2.3, UC-F2.4, UC-F2.5, UC-F4.1, UC-F4.2, UC-F4.5, UC-F8.1, UC-F8.2, UC-F8.3, UC-F8.4, UC-F8.5, UC-F8.6, UC-F8.7, UC-F10.1, UC-F10.2, UC-F10.3, UC-F10.4 |
| `refresh_tokens` | F1 | UC-F1.1, UC-F1.2, UC-F1.3, UC-F1.4, UC-F1.5, UC-F2.4 |
| `password_resets` | F1 | UC-F1.5, UC-F2.2 |
| `audit_logs` | F17 | UC-F5.5 |
| `notifications` | F10 | UC-F8.2, UC-F8.3, UC-F8.4, UC-F8.7, UC-F10.1, UC-F10.2, UC-F10.3, UC-F10.4, UC-F10.5 |
| `system_configs` | F6, F8, F9, F15, F18 | UC-F5.1, UC-F6.4, UC-F7.3, UC-F7.4, UC-F7.5, UC-F8.1, UC-F8.2, UC-F8.3, UC-F8.4, UC-F8.6, UC-F9.2, UC-F10.1, UC-F10.3, UC-F10.4 |
| `branches` | F18, F4 | UC-F1.1, UC-F2.1, UC-F4.2, UC-F5.1, UC-F5.4, UC-F6.1, UC-F6.2, UC-F7.2, UC-F8.0, UC-F8.2, UC-F8.7, UC-F8.8, UC-F10.1, UC-F10.2, UC-F10.3, UC-F10.4 |
| `rooms` | F8 | UC-F4.3, UC-F8.0, UC-F8.1, UC-F8.5, UC-F8.7, UC-F8.8 |
| `equipment` | F11 | Chưa có UC trong F1–F10; chờ đặc tả chức năng tương ứng (TV-09). |
| `maintenance_logs` | F11 | Chưa có UC trong F1–F10; chờ đặc tả chức năng tương ứng (TV-09). |
| `members` | F2 | UC-F1.1, UC-F1.6, UC-F2.1, UC-F2.2, UC-F2.3, UC-F2.4, UC-F2.5, UC-F3.5, UC-F4.1, UC-F4.2, UC-F4.5, UC-F5.3, UC-F5.4, UC-F6.3, UC-F6.4, UC-F6.5, UC-F7.1, UC-F7.2, UC-F7.3, UC-F7.4, UC-F7.5, UC-F8.1, UC-F8.2, UC-F8.3, UC-F8.4, UC-F8.6, UC-F9.2, UC-F9.4, UC-F9.5, UC-F9.6, UC-F10.1, UC-F10.2, UC-F10.3, UC-F10.4 |
| `face_embeddings` | F20 | Chưa có UC trong F1–F10; chờ đặc tả chức năng tương ứng (TV-09). |
| `body_metrics` | F2, F9 | UC-F2.5, UC-F9.5, UC-F9.6 |
| `packages` | F3 | UC-F3.1, UC-F3.2, UC-F3.3, UC-F3.4, UC-F3.5, UC-F4.5, UC-F6.4, UC-F6.5, UC-F7.1, UC-F7.4, UC-F7.5, UC-F8.1, UC-F8.2, UC-F10.1 |
| `memberships` | F3, F4, F7 | UC-F3.5, UC-F4.1, UC-F4.2, UC-F4.5, UC-F5.2, UC-F5.3, UC-F5.5, UC-F6.4, UC-F6.5, UC-F7.1, UC-F7.4, UC-F7.5, UC-F8.1, UC-F8.2, UC-F9.2, UC-F9.3, UC-F10.1 |
| `membership_freezes` | F15 | Chưa có UC trong F1–F10; chờ đặc tả chức năng tương ứng (TV-09). |
| `membership_transfers` | F15 | Chưa có UC trong F1–F10; chờ đặc tả chức năng tương ứng (TV-09). |
| `checkins` | F4, F6, F20 | UC-F4.2, UC-F4.4, UC-F4.5, UC-F6.1, UC-F6.2, UC-F6.5, UC-F7.2 |
| `class_types` | F8 | UC-F4.3, UC-F8.0, UC-F8.1, UC-F8.5, UC-F8.7, UC-F8.8 |
| `class_schedules` | F8 | UC-F4.3, UC-F8.0, UC-F8.1, UC-F8.5, UC-F8.7, UC-F8.8 |
| `class_sessions` | F8 | UC-F4.2, UC-F4.3, UC-F8.0, UC-F8.1, UC-F8.2, UC-F8.3, UC-F8.4, UC-F8.5, UC-F8.6, UC-F8.7, UC-F8.8, UC-F9.1, UC-F9.2, UC-F9.4, UC-F9.5, UC-F9.6, UC-F10.2, UC-F10.3, UC-F10.4 |
| `class_bookings` | F8, F10 | UC-F4.2, UC-F4.3, UC-F8.1, UC-F8.2, UC-F8.3, UC-F8.4, UC-F8.5, UC-F8.6, UC-F8.7, UC-F9.4, UC-F9.5, UC-F9.6, UC-F10.2, UC-F10.3, UC-F10.4 |
| `trainer_availabilities` | F9 | UC-F9.1, UC-F9.2 |
| `pt_sessions` | F9, F13 | UC-F9.1, UC-F9.2, UC-F9.3, UC-F9.4, UC-F9.5, UC-F9.6 |
| `workout_plans` | F9 | UC-F9.4 |
| `workout_logs` | F9 | UC-F9.5, UC-F9.6 |
| `products` | F12 | Chưa có UC trong F1–F10; chờ đặc tả chức năng tương ứng (TV-09). |
| `inventories` | F12 | Chưa có UC trong F1–F10; chờ đặc tả chức năng tương ứng (TV-09). |
| `stock_movements` | F12 | Chưa có UC trong F1–F10; chờ đặc tả chức năng tương ứng (TV-09). |
| `promotions` | F14 | Chưa có UC trong F1–F10; chờ đặc tả chức năng tương ứng (TV-09). |
| `invoices` | F5, F7, F12, F14 | UC-F3.5, UC-F5.1, UC-F5.2, UC-F5.3, UC-F5.4, UC-F5.5, UC-F6.3, UC-F7.3, UC-F7.4, UC-F7.5 |
| `invoice_items` | F5, F12, F15, F16 | UC-F3.5, UC-F5.2, UC-F5.3, UC-F5.4, UC-F5.5, UC-F7.3, UC-F7.4, UC-F7.5, UC-F9.3 |
| `payments` | F5 | UC-F3.5, UC-F5.2, UC-F5.4, UC-F5.5, UC-F6.3 |
| `staff` | F13, F9 | UC-F1.6, UC-F2.1, UC-F2.3, UC-F4.1, UC-F4.2, UC-F4.4, UC-F4.5, UC-F6.1, UC-F6.2, UC-F6.3, UC-F6.4, UC-F6.5, UC-F8.0, UC-F8.1, UC-F8.5, UC-F8.6, UC-F8.7, UC-F8.8, UC-F9.1, UC-F9.2, UC-F9.3, UC-F9.4, UC-F9.5, UC-F9.6 |
| `shifts` | F13 | Chưa có UC trong F1–F10; chờ đặc tả chức năng tương ứng (TV-09). |
| `attendances` | F13 | Chưa có UC trong F1–F10; chờ đặc tả chức năng tương ứng (TV-09). |
| `payrolls` | F13 | Chưa có UC trong F1–F10; chờ đặc tả chức năng tương ứng (TV-09). |
| `badges` | F19 | Chưa có UC trong F1–F10; chờ đặc tả chức năng tương ứng (TV-09). |
| `member_badges` | F19 | Chưa có UC trong F1–F10; chờ đặc tả chức năng tương ứng (TV-09). |

## 5. Điểm lệch và phần còn cần chốt

Các dòng dưới đây là kết quả rà soát tài liệu, chưa phải kết luận có lỗi trong mã chạy. Giữ nguyên nguồn chuẩn; trưởng nhóm xác nhận nghiệp vụ, người phụ trách đặc tả/ERD cập nhật nguồn rồi mới cập nhật bảng truy vết này. Những thiếu sót chỉ ở cột ánh xạ gốc (ví dụ `audit_logs` thuộc F17 nhưng F5 cũng ghi) đã được bổ sung tại mục 3–4, không coi là thiếu bảng.

| Mã | Use case / dữ liệu | Bằng chứng và ảnh hưởng | Người cần xử lý / bước tiếp theo |
| --- | --- | --- | --- |
| TV-01 | UC-F8.2, UC-F8.3, UC-F10.3; `class_bookings.grace_until` | F8 nháp mục 3.3, 7.2 lấy mốc tạo thông báo trong web theo [quyết định trưởng nhóm ngày 04/11](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6633040706). [F10 mục 3.3.C bước 5](dac-ta-use-case-f10-thong-bao.md), [ADR 0003](../../decisions/0003-danh-sach-cho-tu-dong-dua-vao-lop.md), [ERD](../../design/erd/erd-v2.dbml) vẫn tính từ gửi email. Email trễ/lỗi khiến hai cách tính khác nhau. | Hoàng Kim Tiến cùng người phụ trách F8/F10 và ERD đồng bộ các nguồn theo quyết định; kiểm tra cả điều kiện hủy đúng mốc `grace_until`. |
| TV-02 | UC-F8.1; `class_bookings.created_by` | F8 nháp mục 3.2, 7.4 yêu cầu lưu tài khoản bấm đặt, gồm hội viên/lễ tân; ERD để nullable và ghi chú “lễ tân nếu thao tác hộ”. | Người phụ trách ERD và backend F8 thống nhất ý nghĩa NULL, ràng buộc dữ liệu và cập nhật ghi chú; không bỏ người tạo ở chỗ đặt mới chỉ vì cột cho phép NULL. |
| TV-03 | UC-F8.3, UC-F8.6; `members.booking_locked_until`, `class_bookings` | F8 nháp mục 7.2 yêu cầu chỉ đếm vi phạm trong tháng Việt Nam và sau lần khóa gần nhất; mục 7.6 ghi chưa có thiết kế lưu mốc khóa lịch sử trong ERD. Chỉ có thời điểm hết khóa chưa đủ để suy ra lịch sử khi cấu hình thay đổi. | Backend F8 cùng Hoàng Kim Tiến chốt cách lưu mốc/sự kiện, xử lý lặp và ảnh hưởng việc sửa NO_SHOW sai lên lệnh khóa đã áp. Chưa tự đặt tên cột hay bảng mới. |
| TV-04 | UC-F8.2, UC-F10.3; `notifications.user_id` | ERD yêu cầu `notifications.user_id` NOT NULL nhưng `members.user_id` có thể NULL. F10 mục 3.3.D A1 bỏ thông báo cho hồ sơ chưa có tài khoản; F8 nháp mục 7.6 còn mở cách ghi mốc ân hạn từ thông báo web trong trường hợp này. | Hoàng Kim Tiến và người phụ trách F8/F10 chốt cách xử lý hội viên được đặt hộ chưa có tài khoản; không tự tạo tài khoản hay bỏ quyền ân hạn. |
| TV-05 | UC-F8.6; `class_bookings.attendance_marked_by` | F8 nháp mục 3.7 cho quản lý đúng chi nhánh sửa điểm danh sau hạn của PT; ERD chỉ ghi “huấn luyện viên điểm danh”. F8 mục 7.5 chưa chọn ghi đè người điểm danh hay giữ lịch sử sửa riêng. | Người phụ trách F8/ERD chốt cách lưu người sửa và lịch sử theo S9; đồng bộ ghi chú và tiêu chí nghiệm thu. |
| TV-06 | UC-F8.7; `class_sessions.capacity` | F8 nháp mục 7.4 quyết định 13 nói không giảm dưới số BOOKED; ERD/[thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md) tính cả BOOKED, PENDING_RECONFIRM, ATTENDED, NO_SHOW là chỗ chiếm. F8 mục 7.6 đã yêu cầu làm rõ đối tượng sửa là lịch lặp hay buổi riêng lẻ. | Hoàng Kim Tiến xác nhận nhánh có trạng thái giữ chỗ khác; giữ bất biến không vượt sức chứa NF6 trong khi chờ chốt. |
| TV-07 | UC-F9.5; `workout_logs` | [F9 mục 3.5.F](dac-ta-use-case-f9-huan-luyen-vien.md) nêu nhật ký PT ghi ngoài buổi PT có thể để `pt_session_id` NULL; ERD không có cột người ghi. SRS chỉ nói “có thể ghi audit_logs”, chưa bảo đảm truy ra PT nào ghi. | Người phụ trách F9 và Hoàng Kim Tiến xác nhận nhu cầu hiển thị/truy vết người ghi, rồi chọn cơ chế audit hoặc đề xuất thay đổi ERD. Không coi phương án tùy chọn là dữ liệu đã có. |
| TV-08 | UC-F2.5, UC-F9.5, UC-F9.6; `body_metrics` | [F9 mục 3.5.F](dac-ta-use-case-f9-huan-luyen-vien.md) để mở cách chọn số đo khi hội viên và PT cùng ghi trong một ngày. ERD không có UNIQUE trên (`member_id`, `measured_at`); có thể có nhiều dòng hợp lệ. | Người phụ trách F2/F9 cùng Hoàng Kim Tiến chốt biểu đồ lấy dòng nào hoặc hiển thị tất cả; không tự thêm UNIQUE để loại bản ghi. |
| TV-09 | F11–F20; các bảng tương ứng ở mục 2, 4 | Có chức năng trong bản chốt và bảng ERD, nhưng chưa có file đặc tả UC của F11–F20 trong main tại mốc đối chiếu. Phụ thuộc F15 thu phí chuyển nhượng cần nối với F5 khi có đặc tả; F19 còn tính chuỗi ngày từ `checkins` theo thiết kế dữ liệu. | Người được phân công các chức năng bổ sung đặc tả ở giai đoạn tương ứng, rồi thêm mã UC và cập nhật cả hai chiều. Đây là phần chưa đặc tả, không kết luận bảng dư hoặc chức năng đã xong. |
| TV-10 | UC-F1.6; `users.status` | [F1 mục 2](dac-ta-use-case-f1-tai-khoan-va-phan-quyen.md) đặt tên UC gồm “Quản lý trạng thái tài khoản & Phân quyền truy cập API”, nhưng mục 3.6 chỉ mô tả kiểm tra JWT/vai trò/quyền sở hữu. Ngừng hoạt động qua UC-F2.4 đã có; luồng quản trị trạng thái tài khoản tổng quát chưa được mô tả trong UC-F1.6. | Người phụ trách F1 cùng trưởng nhóm thống nhất tên/phạm vi UC hoặc bổ sung luồng còn thiếu; không suy ra endpoint quản trị từ riêng tên UC. |
| TV-11 | UC-F2.1; `members.home_branch_id`, `staff.branch_id` | [F2 mục 3.1](dac-ta-use-case-f2-ho-so-hoi-vien.md) liệt kê tác nhân lễ tân/quản lý nhưng tiền điều kiện thêm SUPER_ADMIN; chi nhánh tạo hồ sơ lấy từ nhân viên thao tác. [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md) xác định chủ chuỗi không có dòng `staff`. | Người phụ trách F2 và Hoàng Kim Tiến thống nhất quyền tạo của chủ chuỗi và nguồn chi nhánh nếu được phép; không tự giả định chủ chuỗi có `staff.branch_id`. |
| TV-12 | UC-F10.2; thời điểm gửi nhắc lịch lớp | [F10 mục 3.2.A](dac-ta-use-case-f10-thong-bao.md) để mở việc gửi nhắc trước giờ bắt đầu lớp tập nhóm bao lâu; bản chốt phạm vi và danh sách khóa system_configs trong ERD chưa quy định mốc này. Chưa chốt thời gian cố định hay tham số cấu hình, nên chưa thể xác định thời điểm kích hoạt gửi nhắc. | Hoàng Kim Tiến cùng người phụ trách F10 chốt khoảng thời gian nhắc và cách quản lý giá trị này. Sau đó cập nhật đặc tả F10 và tiêu chí chấp nhận; nếu dùng cấu hình thì bổ sung khóa, giá trị mặc định và phạm vi áp dụng vào tài liệu dữ liệu, rồi thêm system_configs vào truy vết. Không tự chọn thời gian hoặc đặt tên khóa cấu hình. |
| TV-13 | UC-F8.1, UC-F8.2; `class_bookings.waitlisted_at`, `class_bookings.id` | F8 nháp tại commit `5233c53`, mục 3.2–3.3 và 7.1, đã chốt FIFO theo `waitlisted_at ASC, id ASC`; [ADR 0003](../../decisions/0003-danh-sach-cho-tu-dong-dua-vao-lop.md) chỉ nêu `waitlisted_at` tăng dần. Khi thời điểm vào chờ trùng nhau, nguồn ADR chưa ghi tiêu chí phụ để xác định thứ tự. | Hoàng Kim Tiến cùng người phụ trách ADR/F8 đồng bộ tiêu chí phụ `id ASC` theo quyết định đã chốt; đối chiếu kịch bản hai booking trùng `waitlisted_at` thì ID nhỏ hơn được xét trước. Đây là điểm thiếu ghi chú ở nguồn chuẩn, không phải đề xuất đổi quy tắc FIFO. |

## 6. Cách cập nhật và tự kiểm tra

1. Khi thêm/sửa use case, giữ mã đã được đặc tả; cập nhật dòng ở mục 3, tổng quan mục 2 và chiều ngược mục 4.
2. Kiểm tra mọi tên bảng tồn tại trong `erd-v2.dbml`. Đối chiếu cả luồng đọc/ghi, không chỉ cột “Chức năng” của thiết kế dữ liệu.
3. Khi F8 được merge, thay liên kết commit nháp bằng nguồn đã gộp; rà lại TV-01…TV-06 cùng đặc tả F10 và TV-13 cùng ADR 0003. Không tự đánh dấu các điểm này đã xử lý chỉ vì PR F8 được merge.
4. Khi nguồn liên quan thay đổi, ghi ngày/mốc đối chiếu mới. Điểm TV chỉ chuyển sang đã xử lý khi có nguồn chốt và các tài liệu liên quan đã đồng bộ.

Kiểm tra tại lần lập bảng: đủ 20 chức năng; đủ 56 mã UC trong 10 đặc tả đã nêu; đủ 39 bảng trong chiều ngược; các tên bảng tồn tại trong DBML. Các kiểm tra này xác nhận tính đầy đủ của bảng truy vết, không chứng minh đã giải quyết các điểm TV hoặc đã triển khai các chức năng.
