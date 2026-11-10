# Kế hoạch Sprint 2 (10/11 – 23/11/2026)

- **Trạng thái:** dự thảo, chốt tại [họp tổng kết Sprint 1](../meetings/2026-11-09-tong-ket-sprint-1.md)
- **Người lập:** Nguyễn Trọng Việt
- **Nguồn:** [bản chốt phạm vi](../requirements/ban-chot-pham-vi.md), [tiến độ Sprint 1](sprint-1-tien-do.md), [ADR 0009](../decisions/0009-bo-so-do-phan-tich-thiet-ke.md), user story F1–F10 trên GitHub Issues

Cột "Người" là **đề xuất** theo nguyên tắc của Sprint 1: ai viết đặc tả và migration của chức năng nào thì làm API và giao diện chức năng đó; ai vẽ wireframe màn hình nào thì làm giao diện màn hình đó. Đổi người hoặc hạn ở cuộc họp thì sửa trong bảng này.

## 1. Phạm vi

- **Phần còn lại của v1:** F1 (đổi mật khẩu, quên và đặt lại mật khẩu, khóa đăng nhập tạm), F3 (đăng ký và gia hạn gói), F4 (check-in tại quầy), F5 (thanh toán tại quầy), F6 (dashboard cơ bản), ảnh đại diện của F2.
- **v2:** F7 (cổng hội viên), F8 (lớp học nhóm), F9 (huấn luyện viên), F10 (thông báo).
- **Chuyển từ Sprint 1:** [liệt kê việc chưa xong sau họp tổng kết].
- **Ngoài phạm vi Sprint 2:** v3 trở đi (F11–F20).

## 2. Việc cần làm

### 2.1 Chốt và chuẩn bị

| Mã | Việc | Người | Hạn | Ghi chú |
| --- | --- | --- | --- | --- |
| S2-01 | Chốt nơi lưu ảnh đại diện và ghi ADR | Hoàng Kim Tiến chốt, Lê Phước Minh Quân ghi ADR | 12/11 | F4 cần ảnh để lễ tân đối chiếu |
| S2-02 | Chuyển việc Sprint 1 còn dở thành issue Sprint 2 | Nguyễn Trọng Việt | 10/11 | Sau họp tổng kết |

### 2.2 Phần còn lại của v1

| Mã | Việc | Người | Hạn | Phụ thuộc |
| --- | --- | --- | --- | --- |
| S2-03 | F1: đổi mật khẩu (US-F1.4) | Bùi Trần Đức Việt | 13/11 | F1 đăng nhập đã xong |
| S2-04 | F1: khóa đăng nhập tạm sau 5 lần sai (US-F1.6, S4) | Bùi Trần Đức Việt | 14/11 | Cột `failed_login_count`, `locked_until` đã có |
| S2-05 | F1: quên và đặt lại mật khẩu, gửi email (US-F1.5) | Bùi Trần Đức Việt | 17/11 | Cấu hình SMTP dùng chung với F10 |
| S2-06 | F2: ảnh đại diện, API và giao diện | Thái Hùng Lân (API), Nguyễn Anh Tú (giao diện) | 18/11 | S2-01 |
| S2-07 | F3: API đăng ký và gia hạn gói, tác vụ nền chuyển EXPIRED và kích hoạt UPCOMING | Thái Hùng Lân | 17/11 | Quy tắc gói trong bản chốt, `uq_one_active_per_type` |
| S2-08 | F3: giao diện đăng ký và gia hạn gói | Nguyễn Anh Tú | 19/11 | S2-07, wireframe bán và gia hạn gói |
| S2-09 | F5: API hóa đơn, ghi nhận thanh toán tại quầy, VietQR, hóa đơn PDF; xử lý hóa đơn chờ do hội viên gửi | Thái Hùng Lân | 20/11 | S2-07; khóa `BANK_*` trong `system_configs` |
| S2-10 | F5: giao diện hóa đơn và thanh toán | Thái Hùng Lân | 21/11 | Wireframe check-in, hóa đơn |
| S2-11 | F4: API và giao diện check-in tại quầy (tìm theo mã hoặc số điện thoại, kiểm tra gói, trừ buổi gói SESSION) | Lê Phước Minh Quân | 19/11 | S2-07, S2-01 |
| S2-12 | F6: API và giao diện dashboard cơ bản | Lê Phước Minh Quân | 21/11 | S2-11, S2-09 |

### 2.3 v2: F7 đến F10

| Mã | Việc | Người | Hạn | Phụ thuộc |
| --- | --- | --- | --- | --- |
| S2-13 | F7: API xem gói, lịch sử check-in, hóa đơn của hội viên | Nguyễn Trọng Việt | 18/11 | S2-07, S2-09, S2-11 |
| S2-14 | F7: API gửi yêu cầu mua hoặc gia hạn (hóa đơn chờ, gói PENDING) và tác vụ hủy hóa đơn chờ quá `PENDING_INVOICE_TTL_DAYS` | Nguyễn Trọng Việt | 20/11 | S2-09 |
| S2-15 | F7: giao diện "Gói của tôi" theo wireframe cổng hội viên | Nguyễn Trọng Việt | 21/11 | S2-13, S2-14 |
| S2-16 | F8: API lịch lớp lặp, sinh buổi, đặt chỗ, hủy chỗ, quy tắc vi phạm | Phạm Gia Bảo | 18/11 | S2-07 (gói TIME) |
| S2-17 | F8: danh sách chờ tự động đưa vào lớp, đổi giờ, đổi phòng, hủy buổi, điểm danh | Phạm Gia Bảo | 22/11 | S2-16; việc khó, xem mục 5 |
| S2-18 | F8: giao diện lịch lớp, đặt chỗ, danh sách chờ theo wireframe cổng hội viên | Phạm Gia Bảo | 22/11 | S2-16 |
| S2-19 | F9: API lịch rảnh, đặt buổi PT, xác nhận buổi | Nguyễn Trọng Việt | 21/11 | S2-07 (gói PT), bảng `staff` |
| S2-20 | F9: API giáo án, nhật ký tập, chỉ số cơ thể, tiến độ; giao diện huấn luyện viên | Nguyễn Trọng Việt | 23/11 | S2-19 |
| S2-21 | F10: tác vụ nền gửi email nhắc hết hạn và nhắc lịch lớp | Bùi Trần Đức Việt | 20/11 | S2-05 (SMTP), S2-07 |
| S2-22 | F10: thông báo trong web qua WebSocket, API xem và đánh dấu đã đọc | Bùi Trần Đức Việt | 22/11 | S2-21; thông báo được vào lớp và đổi giờ cần S2-17 |
| S2-23 | F10: giao diện thông báo | Nguyễn Anh Tú | 22/11 | S2-22 |

### 2.4 Sơ đồ còn lại (theo ADR 0009)

| Mã | Việc | Người | Hạn |
| --- | --- | --- | --- |
| S2-24 | Sơ đồ phân cấp chức năng | Nguyễn Anh Tú | 14/11 |
| S2-25 | Sơ đồ kiến trúc | Nguyễn Phú Trọng | 16/11 |
| S2-26 | Sơ đồ lớp phân tích | Nguyễn Anh Tú | 18/11 |
| S2-27 | Sơ đồ tuần tự: mỗi người vẽ một luồng của chức năng mình làm (đăng nhập, gia hạn gói, check-in, đặt buổi PT, đặt lớp và danh sách chờ); Minh Quân tổng hợp | Từng người, Lê Phước Minh Quân tổng hợp | 20/11 |
| S2-28 | Sơ đồ lớp thiết kế: mỗi người vẽ phần chức năng của mình, Đức Việt tổng hợp | Từng người, Bùi Trần Đức Việt tổng hợp | 23/11 |

### 2.5 Hạ tầng, kiểm thử, tài liệu, quản lý

| Mã | Việc | Người | Hạn |
| --- | --- | --- | --- |
| S2-29 | Dữ liệu mẫu cho F3–F10 (`db/seed`) | Nguyễn Phú Trọng | 13/11 |
| S2-30 | Triển khai staging sau mỗi đợt gộp, chạy lại Postman | Nguyễn Phú Trọng | 16/11 và 22/11 |
| S2-31 | Kiểm thử ràng buộc cơ sở dữ liệu (nếu chưa xong ở Sprint 1) | Bùi Trần Đức Việt | 14/11 |
| S2-32 | SRS bổ sung F3–F10 và bảng truy vết | Nguyễn Anh Tú | 20/11 |
| S2-33 | Slide và gói bàn giao Sprint 2 | Phạm Gia Bảo | 23/11 |
| S2-34 | Họp giữa sprint: rà tiến độ, gỡ vướng | Nguyễn Trọng Việt | 16/11 |
| S2-35 | Duyệt pull request theo cặp, gộp lên `main` | Hoàng Kim Tiến | Liên tục |

**Quy tắc chung cho mọi việc code:** mỗi người tự viết kiểm thử JUnit và request Postman cho chức năng của mình trong cùng pull request (quy tắc mã nguồn số 14); đọc [checklist-review.md](../guides/checklist-review.md) trước khi mở pull request; thay đổi schema thì thêm file migration mới, không sửa file đã gộp vào `main`.

## 3. Mốc

| Ngày | Mốc |
| --- | --- |
| 10/11 | Bắt đầu Sprint 2; bàn giao và demo Sprint 1 |
| 12/11 | Chốt nơi lưu ảnh đại diện (S2-01) |
| 16/11 | Họp giữa sprint; staging có F1, F3 |
| 20/11 | API v1 hoàn tất; F7, F8 có API cơ bản trên staging |
| 22/11 | F9, F10 có API; chạy Postman toàn bộ trên staging |
| 23/11 | Kết thúc Sprint 2; sơ đồ và slide nộp |
| [điền] | Bàn giao và demo Sprint 2 |

## 4. Cặp duyệt pull request

Giữ như Sprint 1: Đức Việt và Lân duyệt lẫn nhau; Anh Tú và Gia Bảo duyệt lẫn nhau; Minh Quân và Trọng Việt duyệt lẫn nhau; Phú Trọng do Hoàng Kim Tiến duyệt. Người duyệt bấm Approve hoặc Request changes, không chỉ để Comment.

## 5. Rủi ro

- **F8 và F9 khó và phụ thuộc F3.** F8 cần gói TIME còn hiệu lực, F9 cần gói PT và bảng `staff`; nếu S2-07 trễ thì cả hai trễ theo. Nên có người hỗ trợ Gia Bảo ở S2-17; người hỗ trợ: [chốt tại họp].
- **Nhiều việc dồn vào một người.** Trọng Việt giữ F7 và F9, Lân giữ F3 và F5, Minh Quân giữ F4, F6 và sơ đồ tuần tự. Nếu một việc trễ thì chuyển ngay cho người rảnh hơn, không chờ đến cuối sprint.
- **F10 cần F8.** Thông báo được đưa vào lớp và đổi giờ chỉ gửi được khi F8 có sự kiện tương ứng (S2-17).
- **Thay đổi schema giữa sprint.** Không sửa migration đã gộp; mỗi thay đổi là một file mới và cần báo cả nhóm.
- **Render bản miễn phí ngủ sau 15 phút.** Mở staging trước giờ demo vài phút.

## 6. Câu hỏi cần chốt tại họp

1. Nơi lưu ảnh đại diện (S2-01).
2. Ai hỗ trợ F8 (S2-17).
3. Ngày bàn giao và demo Sprint 2.
4. Những việc của Sprint 1 chưa xong: chuyển sang Sprint 2 hay bỏ.
