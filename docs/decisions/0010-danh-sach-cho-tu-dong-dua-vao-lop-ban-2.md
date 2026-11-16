# ADR 0010: Danh sách chờ tự động đưa vào lớp (bản 2)

- **Ngày:** 12/11/2026
- **Người quyết định:** Hoàng Kim Tiến (trưởng nhóm), chốt khi review đặc tả F8 ngày 04/11/2026, làm rõ ngày 07/11/2026
- **Trạng thái:** Đã chấp nhận. Thay thế [ADR 0003](0003-danh-sach-cho-tu-dong-dua-vao-lop.md)

## Bối cảnh

[ADR 0003](0003-danh-sach-cho-tu-dong-dua-vao-lop.md) (27/10) chọn cách tự động đưa người đầu danh sách chờ vào lớp. Khi viết đặc tả F8 (#9, PR #79), nhóm phát hiện ba chỗ ADR 0003 chưa đủ hoặc gây sai:

1. **Ân hạn tính từ lúc gửi email.** Email ban đêm chỉ gửi khi chi nhánh mở cửa, và email có thể gửi lỗi. Khi đó mốc ân hạn không xác định được, hoặc kéo dài nhiều giờ. Hội viên được đặt hộ mà chưa có tài khoản thì không nhận email.
2. **Hai người vào chờ cùng thời điểm** (`waitlisted_at` bằng nhau) thì chưa có tiêu chí phụ, nên thứ tự không cố định.
3. **Còn dưới 30 phút trước giờ học** thì ADR 0003 chỉ ngừng đưa người chờ vào lớp, nhưng chưa nói người mới có được đặt chỗ trống đó không. Nếu được, người mới sẽ vượt mặt người đang chờ.

Mẫu ADR không cho sửa nội dung ADR đã chấp nhận, nên quyết định được viết lại đầy đủ ở đây. Người đọc chỉ cần đọc ADR này.

Nguồn quyết định: [chốt 04/11](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6633040706) (câu 2, 3, 4, 7, 8, 16), [làm rõ 07/11](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6701988493).

## Quyết định

Khi có chỗ trống, hệ thống **tự động đưa vào lớp** người đủ điều kiện đứng đầu danh sách chờ và báo cho họ, không cần họ xác nhận (giữ nguyên lựa chọn của ADR 0003). Cụ thể:

- **Thứ tự chờ:** đến trước, xếp trước, theo `class_bookings.waitlisted_at` tăng dần. Trùng thời điểm thì dòng có `id` nhỏ hơn đứng trước (`ORDER BY waitlisted_at, id`). Vị trí hiển thị tính khi truy vấn, không lưu số cố định.
- **Bỏ qua người không đủ điều kiện:** gói TIME hết hiệu lực, đang bị khóa đặt lớp, hoặc đã có lịch trùng giờ. Người bị bỏ qua giữ nguyên vị trí; hệ thống xét người tiếp theo.
- **Xét danh sách chờ trước người mới:** khi có người đặt mới vào buổi còn chỗ mà buổi đó có người chờ, hệ thống chạy lại bước tự động đưa vào lớp trước (dùng chung một hàm). Còn chỗ sau bước đó thì người mới mới được đặt.
- **Mốc 30 phút\* (`PROMOTION_CUTOFF_MIN`):** khi còn dưới mốc này trước giờ học, hệ thống ngừng tự động đưa vào lớp, **đồng thời đóng đặt chỗ mới và vào danh sách chờ** cho buổi đó, kể cả khi buổi còn trống. Chỗ trống sau mốc này để trống.
- **Ân hạn 15 phút\* (`PROMOTION_GRACE_MIN`):** tính từ **lúc được đưa vào lớp**, cũng là lúc tạo thông báo trong web, trong cùng giao dịch: `grace_until = promoted_at + PROMOTION_GRACE_MIN`. Hủy trước hoặc đúng mốc `grace_until` (so sánh `<=`) thì không bị tính hủy muộn. Hội viên chưa có tài khoản (không nhận được thông báo) vẫn có ân hạn theo cùng công thức.
- **Ban đêm:** chỗ trống vẫn được lấp ngay. Thông báo trong web hiện ngay; email chờ tới giờ mở cửa chi nhánh mới gửi. Thời điểm gửi email **không** ảnh hưởng mốc ân hạn.
- **Rời danh sách chờ không bao giờ bị tính hủy muộn.** Hủy muộn chỉ áp dụng cho chỗ đã `BOOKED`.
- **Không ưu tiên ngoài thứ tự:** không ai, kể cả lễ tân, được xếp hội viên vào lớp ngoài thứ tự danh sách chờ.
- **Người tạo:** `class_bookings.created_by` ghi `users.id` của tài khoản đã bấm đặt hoặc vào chờ (hội viên hoặc lễ tân). Khi được tự động đưa vào lớp, chỗ đặt giữ nguyên người tạo ban đầu.
- **Khóa bản ghi (NF6):** đặt chỗ và đưa vào lớp chạy trong giao dịch có khóa bản ghi để không vượt sĩ số. Khóa dòng `members` trước, `class_sessions` sau. Sau khi lấy khóa, kiểm tra lại điều kiện (trùng giờ, còn chỗ).

(\*) Tham số cấu hình trong `system_configs`, không viết cứng.

## Phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Ân hạn tính từ lúc được đưa vào lớp (đã chọn) | Mốc luôn xác định, ghi trong cùng giao dịch; không phụ thuộc email; đúng cả với hội viên chưa có tài khoản | Hội viên chỉ dùng email, được đưa vào lớp ban đêm, có thể đọc email khi ân hạn đã hết |
| Ân hạn tính từ lúc gửi email (ADR 0003) | Hội viên chỉ dùng email luôn có đủ 15 phút sau khi nhận tin | Email lỗi hoặc trễ thì mốc không xác định; hội viên chưa có tài khoản không có mốc; cần cập nhật `grace_until` sau, từ tác vụ gửi email |
| Gửi lời mời, chờ xác nhận trong N phút | Chỉ người thật sự muốn đi mới vào lớp | Nhiều trạng thái trung gian, nhiều tác vụ hẹn giờ, khó kiểm thử, chỗ trống có thể bỏ phí |

## Hệ quả

- Bảng `class_bookings` gộp cả đặt chỗ và danh sách chờ (không đổi). `grace_until` được ghi cùng lúc với `promoted_at`. `created_by` thành bắt buộc (`NOT NULL`, migration `V9_01`).
- F10 vẫn cần hẹn giờ gửi email theo giờ mở cửa chi nhánh, nhưng tác vụ gửi email không ghi lại `grace_until`.
- Cần kiểm thử JUnit cho: thứ tự chờ (kể cả trùng `waitlisted_at`), bỏ qua người không đủ điều kiện, xét danh sách chờ trước người mới, mốc 30 phút đóng cả đặt mới, ân hạn đúng mốc `grace_until` (`<=`), rời danh sách chờ sát giờ không bị phạt, không vượt sĩ số khi đặt đồng thời.
- Tài liệu cập nhật theo: [bản chốt phạm vi](../requirements/ban-chot-pham-vi.md) (mục F8), [erd-v2.dbml](../design/erd/erd-v2.dbml) (`class_bookings`), [thiết kế dữ liệu](../design/erd/thiet-ke-du-lieu.md), [từ điển dữ liệu](../design/erd/tu-dien-du-lieu.md), [đặc tả F10](../requirements/srs/dac-ta-use-case-f10-thong-bao.md) (UC-F10.3).
