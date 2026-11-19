# ADR 0014: Bỏ danh sách chờ lớp học nhóm, đặt chỗ theo kiểu còn chỗ thì đặt

- **Ngày:** 19/11/2026
- **Người quyết định:** Hoàng Kim Tiến (trưởng nhóm)
- **Trạng thái:** Đã chấp nhận. Thay thế [ADR 0010](0010-danh-sach-cho-tu-dong-dua-vao-lop-ban-2.md)

## Bối cảnh

[ADR 0010](0010-danh-sach-cho-tu-dong-dua-vao-lop-ban-2.md) chọn danh sách chờ tự động đưa vào lớp. Khi chuẩn bị code Sprint 2 (17/11 – 30/11), nhóm thấy phần này kéo theo nhiều quy tắc phụ:

1. **Ân hạn 15 phút** sau khi được đưa vào lớp. Ân hạn phải ghi cùng giao dịch với thông báo trong web, nên F8 (Phạm Gia Bảo) phụ thuộc F10 (Bùi Trần Đức Việt) ngay từ đầu. Hai đặc tả còn ghi mốc ân hạn khác nhau (SRS F8 BR-F8.2-04 và SRS F10 UC-F10.3).
2. **Xác nhận lại trong 60 phút khi đổi giờ.** Cần thêm trạng thái `PENDING_RECONFIRM`, một tác vụ nền xử lý hết hạn, một user story (US-F8.4.1) và thông báo kèm hạn.
3. **Mốc 30 phút ngừng đưa vào lớp, bỏ qua người không đủ điều kiện, xét người chờ trước người mới.** Mỗi quy tắc cần thêm nhánh xử lý và kiểm thử đồng thời.

Sprint 2 phải làm cả v1 còn lại lẫn toàn bộ v2 trong 14 ngày. Làm danh sách chờ không trọn vẹn thì bị đánh giá là không hoàn thành cam kết, nặng hơn việc không cam kết từ đầu. Phạm vi chưa được trình bày chính thức với giảng viên, nên vẫn điều chỉnh được.

## Quyết định

Lớp học nhóm đặt chỗ theo kiểu **còn chỗ thì đặt, đầy thì hết chỗ**, giống đăng ký học phần hay mua vé xem phim. Không có danh sách chờ.

- **Đặt chỗ:** chỉ hội viên có gói TIME đang ACTIVE dùng được tại chi nhánh. Mở trước 7 ngày\* (`BOOKING_WINDOW_DAYS`), đóng khi còn dưới 30 phút\* trước giờ học (`BOOKING_CUTOFF_MIN`, đổi tên từ `PROMOTION_CUTOFF_MIN`). Không trùng giờ với buổi khác đã đặt. Lớp đã đủ chỗ thì từ chối và báo "Lớp đã đủ chỗ". Đã hủy một buổi thì vẫn đặt lại được nếu còn chỗ.
- **Hủy chỗ:** chỗ trả lại ngay, ai đặt trước thì được trước. Hệ thống không tự xếp ai vào.
- **Hủy muộn và vắng mặt:** hủy trước giờ học ít nhất 2 giờ\* (`LATE_CANCEL_HOURS`) thì không bị phạt. Hủy muộn hoặc vắng mặt (theo điểm danh) 3 lần\* trong tháng dương lịch giờ Việt Nam (`PENALTY_LIMIT_PER_MONTH`) thì khóa đặt lớp 7 ngày\* (`PENALTY_LOCK_DAYS`). Chỉ đếm vi phạm sau lần khóa gần nhất.
- **Sửa điểm danh không tự tính lại khóa.** Quản lý chi nhánh có thể gỡ khóa đặt lớp của hội viên bằng tay; mỗi lần gỡ ghi `audit_logs`.
- **Đổi giờ:** chỗ đặt giữ nguyên, hội viên được thông báo giờ cũ và giờ mới (`class_sessions.original_start_at`). Buổi đã đổi giờ thì hủy lúc nào cũng không bị tính hủy muộn.
- **Đổi phòng:** chỉ thông báo. **Hủy buổi:** hủy mọi chỗ đặt, không phạt, thông báo cho tất cả.
- **Thông báo:** F8 phát sự kiện (đổi giờ, đổi phòng, hủy buổi); F10 nhận sự kiện và tạo thông báo. Thông báo không nằm trong giao dịch của F8, lỗi thông báo không làm hỏng thao tác của F8.
- **Giữ nguyên:** lễ tân đặt và hủy hộ hội viên (ghi `created_by`); khóa bản ghi khi đặt chỗ (NF6): khóa `members` trước, `class_sessions` sau, kiểm tra lại còn chỗ và trùng giờ sau khi lấy khóa; check-in ghi `arrived_at` (F4).

(\*) Tham số cấu hình trong `system_configs`, không viết cứng.

## Phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| A. Còn chỗ thì đặt, không danh sách chờ (đã chọn) | Ít trạng thái, ít tác vụ nền; F8 không phụ thuộc F10; dễ kiểm thử; làm trọn được trong Sprint 2 | Lớp đầy thì hội viên phải tự quay lại xem có chỗ trống không |
| B. Giữ danh sách chờ tự động, bỏ ân hạn gắn thông báo và bỏ xác nhận lại | Giữ được tính năng đã cam kết; bớt khoảng một phần ba độ phức tạp | Vẫn còn thứ tự chờ, bỏ qua người không đủ điều kiện, khóa đồng thời khi đưa vào lớp; rủi ro không làm trọn |
| C. Giữ nguyên ADR 0010 | Không phải sửa tài liệu | Phức tạp nhất; F8 phụ thuộc F10 từ đầu; rủi ro cao nhất |

## Hệ quả

- Bỏ UC-F8.2 (tự động đưa vào lớp), UC-F8.4 (xác nhận lại khi đổi giờ), UC-F10.3 (báo được vào lớp). Giữ mã các UC còn lại, **không đánh số lại**, để không vỡ truy vết (ADR 0012).
- Ghi đè các điểm liên quan đến danh sách chờ trong [ADR 0005](0005-erd-v2.md) mục 24 (sĩ số chỉ còn tính `BOOKED`, `ATTENDED`, `NO_SHOW`) và [ADR 0009](0009-bo-so-do-phan-tich-thiet-ke.md) (sơ đồ hoạt động "danh sách chờ" đổi thành "đặt chỗ và hủy chỗ lớp nhóm").
- Bảng `class_bookings`: bỏ trạng thái `WAITLIST`, `PENDING_RECONFIRM`, lý do hủy `NOT_RECONFIRMED`; bỏ cột `waitlisted_at`, `promoted_at`, `grace_until`, `reconfirm_expires_at` và chỉ mục `ix_class_bookings_waitlist`, bằng migration mới. `system_configs`: bỏ `PROMOTION_GRACE_MIN`, `RECONFIRM_TTL_MIN`; đổi `PROMOTION_CUTOFF_MIN` thành `BOOKING_CUTOFF_MIN`.
- Cần kiểm thử JUnit cho: lớp đủ chỗ thì từ chối; đặt đồng thời không vượt sĩ số; hủy xong người khác đặt được; hủy đúng mốc 2 giờ không bị phạt; vi phạm thứ 3 trong tháng thì khóa; quản lý gỡ khóa có ghi `audit_logs`; buổi đã đổi giờ thì hủy không bị phạt.
- Rủi ro cần theo dõi: phạm vi v2 nhẹ đi. Khi trình bày với giảng viên, nêu rõ lý do và ADR này.
- Tài liệu cập nhật theo: bản chốt phạm vi (mục F8, F10), bối cảnh đề tài, SRS F8, SRS F10, `erd-v2.dbml`, thiết kế dữ liệu, từ điển dữ liệu, bảng truy vết, sơ đồ use case, sơ đồ hoạt động, sơ đồ phân cấp chức năng, wireframe cổng hội viên và huấn luyện viên.
