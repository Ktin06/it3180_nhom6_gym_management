# Wireframe F2, F3 — Issue #18

Cập nhật: 04/11/2026. Nhánh đã đối chiếu main tại `446ceb3` trước khi nộp.

Bản phác thảo để Nguyễn Anh Tú và nhóm review, gồm đủ bốn phần trong [issue #18](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/18). Chưa phải giao diện chạy được hoặc hợp đồng API của #27. Dữ liệu trong hình là dữ liệu giả.

## File và thứ tự xem

| File | Nội dung |
| --- | --- |
| [Danh sách hội viên](danh-sach-hoi-vien.drawio.svg) | Tìm kiếm, lọc, phân trang, nút thêm/sửa, trạng thái tải/rỗng/lỗi và quyền truy cập |
| [Form hồ sơ](form-ho-so-hoi-vien.drawio.svg) | Thêm/sửa, trường bắt buộc/tùy chọn, lỗi SĐT, ngừng hoạt động; không có ảnh ở Sprint 1 |
| [Danh mục gói](danh-muc-goi-tap.drawio.svg) | TIME/SESSION/PT, form quản lý gói, ngừng bán và quy tắc dữ liệu |
| [Bán và gia hạn](ban-va-gia-han-goi.drawio.svg) | Chọn hội viên/gói, kiểm tra, điểm chuyển sang hóa đơn F5; wireframe phục vụ Sprint 2 |

Mỗi file có khung desktop bên trái, khung mobile rộng 360px bên phải, và ghi chú tương tác phía dưới. Form dài trên điện thoại cuộn theo chiều dọc; không khóa nội dung vào chiều cao khung minh họa.

## Mở xem và chỉnh sửa

- Xem: mở `.drawio.svg` bằng trình duyệt hoặc trình xem SVG trong IDE. Hình được lưu dưới dạng vector.
- Chỉnh sửa: trong VS Code có Draw.io Integration, dùng **Open With… → Draw.io Editor**; hoặc trong draw.io chọn **File → Open From → Device** và chọn file.
- Mỗi file nhúng mô hình `mxfile` có các đối tượng `mxCell` cùng vị trí/kích thước tương ứng hình SVG. Các nhãn, ô nhập, khung và nút đều là đối tượng chỉnh sửa được.
- Khi lưu/xuất lại, giữ định dạng `.drawio.svg` và dữ liệu sơ đồ nhúng (Include a copy of my diagram) để tiếp tục chỉnh sửa.

## Cách đọc các đề xuất

- Điều hướng danh sách → thêm/sửa được mô tả bằng nút và ghi chú; các nút trong file SVG không phải thao tác ứng dụng chạy thật.
- Khung form gói và đổi trạng thái hồ sơ thể hiện đề xuất thiết kế theo nghiệp vụ hiện có. Việc đưa tất cả thao tác này vào code #27 được quyết định ở giai đoạn triển khai, không làm giảm phạm vi vẽ #18.
- Nhãn mã gói ở form là vị trí dữ liệu; cách cấp/sửa mã phải khớp đặc tả API sau này, không tự đặt quy tắc sinh mã mới.
- Bán/gia hạn chỉ vẽ điểm nối sang F5, không thay bản wireframe hóa đơn/VietQR của Lân. Gói chưa thanh toán không được kích hoạt; gia hạn nối tiếp có trạng thái UPCOMING trong thời gian chờ.
- Không triển khai ảnh hội viên, thanh toán online, gói toàn chuỗi hoặc các màn hình ngoài #18.

## Cơ sở yêu cầu và review

Đối chiếu [bản rà soát](ra-soat-yeu-cau-f2-f3.md), [đặc tả F2](../../requirements/srs/dac-ta-use-case-f2-ho-so-hoi-vien.md), [biên bản kick-off](../../meetings/2026-11-02-kick-off.md), [bản chốt phạm vi](../../requirements/ban-chot-pham-vi.md) và [ERD](../erd/erd-v2.dbml).

Khi review, kiểm tra bố cục và đủ luồng của #18, quyền thao tác, nội dung thông báo, phương án mobile và điểm bàn giao F3 → F5. Các câu hỏi DTO/API được giữ trong bản rà soát cho #27; chưa gửi yêu cầu xác nhận cho người khác và chưa đóng issue.

## Kết quả kiểm tra trước khi bàn giao

- Đã cập nhật ghi chú bán/gia hạn theo bản chốt phạm vi và ADR 0005 sau PR #82: phân biệt TIME với SESSION/PT dùng hết buổi trước hạn; gói nối tiếp chỉ được kích hoạt/xếp UPCOMING sau khi hóa đơn PAID. Ví dụ TIME hết hạn 20/11 và nối tiếp 21/11 trong hình vẫn đúng.
- Đã mở cả bốn `.drawio.svg` trong Draw.io ở chế độ nhúng, xuất SVG có dữ liệu sơ đồ và mở lại các bản xuất thành công. Mô hình mở lại lần lượt có 38 ô ở bán/gia hạn, 53 ô ở danh sách, 46 ô ở form hội viên và 65 ô ở danh mục gói.
- Ghi chú gia hạn được cập nhật bằng chức năng merge của editor Draw.io rồi xuất lại file bán/gia hạn; nội dung thay đổi vẫn đúng sau khi mở lại bản xuất. Ba file khác giữ bản đã review; bản xuất của chúng chỉ phục vụ kiểm tra trong thư mục tạm.
- Đã kiểm tra XML, dữ liệu sơ đồ nhúng, ảnh render và tiếng Việt. Tài nguyên editor chính thức được chạy tạm qua localhost; không thêm dependency vào dự án.
- PR #89 giữ nguyên nhánh và lịch sử review; bản cập nhật được push lên cùng nhánh. Người duyệt chính là Gia Bảo (`GamBug`). Chỉ merge sau khi đủ điều kiện của repo.

Các file frontend/backend không thay đổi, nên không chạy lại build hoặc kiểm thử ứng dụng cho thay đổi tài liệu này. Kết quả CI của PR theo GitHub; không suy ra CI xanh từ kiểm tra local.
