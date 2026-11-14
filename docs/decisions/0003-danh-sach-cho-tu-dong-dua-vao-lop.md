# ADR 0003: Danh sách chờ tự động đưa vào lớp

- **Ngày:** 27/10/2026
- **Người quyết định:** cả nhóm 6
- **Trạng thái:** Đã thay thế bởi [ADR 0010](0010-danh-sach-cho-tu-dong-dua-vao-lop-ban-2.md) (12/11/2026)

## Bối cảnh

Lớp học nhóm (F8) có sĩ số tối đa. Khi lớp đầy, hội viên vào danh sách chờ. Cần quyết định khi có chỗ trống thì xử lý thế nào: tự động đưa người đầu danh sách vào lớp, hay gửi lời mời và chờ người đó xác nhận. Nguồn: mục "Lớp học nhóm (F8)" trong [bản chốt phạm vi](../requirements/ban-chot-pham-vi.md).

## Quyết định

Khi có chỗ trống, hệ thống **tự động đưa vào lớp** người đủ điều kiện đứng đầu danh sách chờ và báo cho họ; không cần xác nhận.

- Thứ tự: đến trước, xếp trước, theo `class_bookings.waitlisted_at` tăng dần; vị trí hiển thị tính khi truy vấn, không lưu số cố định.
- Người được đưa vào có 15 phút\* ân hạn để hủy mà không bị tính hủy muộn (`grace_until`).
- Người không còn đủ điều kiện (gói hết hạn, đang bị khóa đặt lớp, đã có lịch trùng giờ) bị bỏ qua, xét người tiếp theo.
- Ngừng tự động đưa vào lớp khi còn dưới 30 phút\* trước giờ học; chỗ trống sau mốc này để trống.
- Chỗ trống ban đêm vẫn được lấp ngay; thông báo trong web hiện ngay, email gửi khi tới giờ mở cửa chi nhánh và ân hạn 15 phút tính từ lúc gửi email.
- Không ai (kể cả lễ tân) được xếp hội viên vào lớp ngoài thứ tự danh sách chờ.
- Đặt chỗ chạy trong giao dịch có khóa bản ghi (NF6) để không vượt sĩ số.

(\*) Tham số cấu hình trong `system_configs`: `PROMOTION_GRACE_MIN`, `PROMOTION_CUTOFF_MIN`.

## Phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Tự động đưa vào lớp (đã chọn) | Ít bước, dễ kiểm thử, chỗ trống được lấp nhanh | Người được đưa vào có thể không còn muốn đi → giải quyết bằng ân hạn hủy không phạt |
| Gửi lời mời, chờ xác nhận trong N phút | Chỉ người thật sự muốn đi mới vào lớp | Nhiều trạng thái trung gian, nhiều tác vụ hẹn giờ, khó kiểm thử, chỗ trống có thể bỏ phí |

## Hệ quả

- Bảng `class_bookings` gộp cả đặt chỗ và danh sách chờ, có các cột `waitlisted_at`, `promoted_at`, `grace_until`.
- Cần kiểm thử JUnit cho: thứ tự chờ, bỏ qua người không đủ điều kiện, mốc 30 phút, ân hạn 15 phút, không vượt sĩ số khi đặt đồng thời.
- F10 (thông báo) phải hỗ trợ email hẹn giờ theo giờ mở cửa chi nhánh.
