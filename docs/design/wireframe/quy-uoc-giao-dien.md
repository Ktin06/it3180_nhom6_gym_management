# Quy ước giao diện và wireframe công khai

- **Người biên soạn:** Phạm Gia Bảo · #17 · WBS 1.4.1, 1.4.2.
- **Cơ sở:** [phạm vi chuẩn](../../requirements/ban-chot-pham-vi.md), [quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md), [ADR 0007](../../decisions/0007-cau-truc-ma-nguon.md), [ADR 0008](../../decisions/0008-goi-api-cung-domain.md), [SRS F1](../../requirements/srs/dac-ta-use-case-f1-tai-khoan-va-phan-quyen.md).
- Đây là quy ước và thiết kế để triển khai; wireframe chưa phải bằng chứng chức năng đã chạy với backend.

## Thành phần và ngôn ngữ

Dùng Ant Design hiện có trong `frontend/package.json` và `ConfigProvider` tiếng Việt trong `src/app/providers.tsx`; không thêm hệ UI khác. Dùng `Typography`, `Button`, `Card`, `Form`, `Input`, `Select`, `DatePicker`, `Alert`, `Empty`, `Skeleton` theo mục đích tương ứng. Mọi nhãn, hướng dẫn, trạng thái và lỗi hiển thị bằng tiếng Việt có dấu.

Giữ màu chủ đạo `#1677ff`, nền bố cục `#f5f5f5`, thẻ trắng, chữ chính `#262626`; màu trạng thái đi kèm chữ/biểu tượng, không dùng màu làm tín hiệu duy nhất. Chữ nội dung 14–16px; tiêu đề theo `Typography.Title`, khoảng cách theo bội số 8px, lề nội dung tối thiểu 16px. Các giá trị này là quy ước trình bày trong phạm vi WBS 1.4.1, không phải thay đổi nghiệp vụ.

Tiền hiển thị số nguyên VND theo định dạng Việt Nam (`1.000.000 đ`), lấy từ server; thời gian hiển thị theo giờ Việt Nam, dữ liệu thời điểm lưu UTC. Không tự thêm giá bán, thời hạn, sĩ số hay tham số khác vào UI; đọc theo nguồn dữ liệu và quy tắc đã duyệt.

## Bố cục và responsive

| Khung có sẵn | Mục đích | Quy ước giữ khi triển khai |
| --- | --- | --- |
| `PublicLayout` | Khách xem thông tin chung | Nội dung tối đa 1080px, padding 16px; liên kết bảng giá, lịch lớp, đăng ký, đăng nhập. Tại 360px cho các mục xuống dòng, không ép một hàng quá rộng. |
| `AuthLayout` | Đăng nhập, đăng ký | Một thẻ giữa trang, tối đa 400px và co theo màn hình; padding ngoài 16px. Khi form cao hơn viewport phải cuộn dọc tới được nút gửi, không cắt phần đầu. |
| `StaffLayout` | Chủ chuỗi, quản lý, lễ tân, PT | Menu theo vai trò; breakpoint `lg` hiện Sider 240px, màn hình hẹp dùng Drawer đã có. Không tự làm chức năng menu chưa được giao. |
| `MemberLayout` | Hội viên | Nội dung một cột tối đa 720px; giữ thanh điều hướng đáy và khoảng chừa 64px để không che nội dung. |

- Hỗ trợ từ **360px** (NF4), không cuộn ngang toàn trang. Mobile xếp thẻ gói theo cột; lịch tuần chuyển thành danh sách nhóm theo ngày; form dùng một cột.
- Theo breakpoint Ant Design đang dùng: `<576px` ưu tiên một cột; `576–991px` co nội dung; `>=992px` có thể dùng bố cục rộng. Kiểm tra tại 360, 768 và 1280px; các file minh họa có cả desktop và mobile đúng rộng 360px.
- Điều hướng dùng React Router; bảo vệ vai trò dùng `ProtectedRoute`. Menu chỉ phục vụ trải nghiệm; quyền sở hữu dữ liệu và vai trò vẫn do backend kiểm tra (S3).
- Form và nút thao tác chạm dùng kích thước dễ bấm (mục tiêu cao tối thiểu 40px). Không dùng hover làm cách duy nhất để mở chức năng.

## Form, khả năng truy cập và trạng thái

- `Form layout="vertical"`; mỗi ô có label thật, không thay label bằng placeholder; đánh dấu trường bắt buộc rõ ràng. Tab theo thứ tự đọc, nhãn liên kết input; nút icon có tên truy cập.
- Cho phép tên tiếng Việt; chuẩn hóa email bằng trim/chữ thường, SĐT theo SRS về 10 chữ số bắt đầu `0` (hỗ trợ nhập `+84`). Không trim/thay đổi mật khẩu. Validation frontend giúp sửa lỗi nhanh; backend vẫn validate mọi request.
- Lỗi trường đặt sát ô nhập; lỗi chung dùng `Alert` có nội dung cụ thể. Chuyển focus tới lỗi đầu tiên khi phù hợp; giữ dữ liệu để người dùng sửa. Hiển thị lỗi dạng chữ, có thông báo được trình đọc màn hình nhận biết.
- Khi gửi: nút có loading và chặn gửi lặp; hoàn tất mới chuyển trang. Không tạo thành công giả hay ẩn lỗi mạng. Có lỗi tải thì cho thử lại; không chèn dữ liệu minh họa vào ứng dụng thật.
- Khi khôi phục phiên: hiển thị đang tải đến khi biết trạng thái, sau đó mới quyết định chuyển đăng nhập. Đăng nhập/đăng ký thành công chuyển tới trang hợp lệ theo vai trò; không nhận URL ngoài ứng dụng làm đích chuyển hướng.
- Danh sách có trạng thái loading (`Skeleton`), rỗng (`Empty`) và lỗi (`Alert` + Thử lại). 403 giải thích thiếu quyền; 404 cho biết không tìm thấy; lỗi 401 khi refresh thất bại yêu cầu đăng nhập lại.

## Đăng ký — WBS 1.4.1

Wireframe chỉnh sửa được: [dang-ky.drawio.svg](dang-ky.drawio.svg).

| Trường | Bắt buộc | Quy tắc |
| --- | --- | --- |
| Họ và tên | Có | Không rỗng sau trim; hỗ trợ tiếng Việt; độ dài theo DTO/`members.full_name` (ERD 255 ký tự). |
| Email | Có | Đúng định dạng, trim/chữ thường; duy nhất do backend kiểm tra. |
| Số điện thoại | Có | Theo SRS: chuẩn hóa `+84 912 345 678` thành `0912345678`, khớp `^0[0-9]{9}$`. |
| Mật khẩu | Có | S1: tối thiểu 8 ký tự có chữ và số; cho phép ẩn/hiện; `autocomplete="new-password"`. |
| Xác nhận mật khẩu | Có trên form | Khớp mật khẩu, chỉ dùng frontend, không gửi thêm trường chưa có trong DTO. |
| Ngày sinh | Không | Nếu nhập gửi ngày `YYYY-MM-DD`; không tự đặt giới hạn tuổi chưa có trong yêu cầu. |
| Giới tính | Không | Tập giá trị DTO chưa được định nghĩa trong SRS/ERD; chốt với owner API trước khi bật các lựa chọn, không tự đặt enum. |

Tự đăng ký tạo cả tài khoản `MEMBER` và hồ sơ ở chi nhánh mặc định, không cho khách chọn vai trò/chi nhánh. Nếu SĐT đã có hồ sơ, báo 409 và hướng dẫn liên hệ quầy; không tự liên kết tài khoản với hồ sơ cũ. Email trùng báo lỗi backend; không xem đăng ký tài khoản là đã mua/kích hoạt gói. Khi thành công tự đăng nhập theo SRS F1; JWT access chỉ ở bộ nhớ, refresh trong cookie HttpOnly theo ADR 0008.

Các trạng thái bắt buộc để kiểm tra khi triển khai: form ban đầu, dữ liệu sai tại trường, đang gửi, 409 SĐT có hồ sơ, email trùng, lỗi mạng/server, thành công. Không dựng thêm form quên mật khẩu/đổi mật khẩu khi ngoài phân công #30.

## Bảng giá và lịch lớp — WBS 1.4.2

Hai wireframe còn lại thuộc WBS 1.4.2. Chức năng công khai tương ứng được thiết kế ở Sprint 1 và triển khai theo phân công Sprint 2; không thêm màn hình chạy thật vào nhiệm vụ tài liệu #17.

**Bảng giá:** hiển thị danh mục đang bán, loại `TIME`/`SESSION`/`PT`, thời hạn hoặc số buổi và giá từ server. TIME có quyền lợi lớp nhóm; SESSION/PT không được đặt lớp; PT chỉ tập 1–1, không dùng để vào phòng tập. CTA đăng ký chỉ tạo tài khoản; thanh toán tại quầy. Không có giỏ hàng, cổng thanh toán online hoặc khuyến mãi tự phát ngoài phạm vi. Endpoint public phải được owner F3 xác nhận; không giả định API quản trị #29 được công khai.

**Lịch lớp:** lịch đọc công khai, chọn tuần/loại lớp là cách trình bày đề xuất, ngày giờ theo Việt Nam; hiển thị loại lớp, thời gian, phòng, PT, trạng thái buổi. Mobile dùng danh sách theo ngày. Không công khai tên/SĐT/email hay danh sách hội viên đặt chỗ. Khách cần đăng nhập/đăng ký và có TIME ACTIVE hợp lệ mới đặt/vào chờ. Chỉ chi nhánh mặc định ở v1–v3; không bật chọn chuỗi F18. Không áp cửa sổ đặt 7 ngày làm giới hạn xem lịch, vì nguồn chuẩn không quy định như vậy. Bộ lọc public/API/trường có thể hiển thị cần owner F8 xác nhận trước triển khai.

Các chỗ `[Giá từ server]`, `[Số buổi...]`, `[Phòng]`, `[PT phụ trách]`, ngày/giờ và tên lớp trong wireframe là ký hiệu minh họa cấu trúc, không phải giá, lịch hay nhân sự vận hành thật.

## Định dạng và kiểm tra bàn giao

- File `.drawio.svg` phải có `svg@content` chứa `mxfile` → `diagram` → `mxGraphModel` → các `mxCell` có hình học, chữ và kiểu; mỗi ô/nhãn là đối tượng sửa được, không phải một ảnh nền raster duy nhất.
- Mở file bằng draw.io hoặc tiện ích Draw.io Integration để chỉnh sửa, lưu lại SVG có nhúng nguồn. SVG đồng thời xem trực tiếp trên GitHub/trình duyệt. Không xuất đè bằng SVG thường làm mất `content`.
- Kiểm tra XML của SVG và nguồn nhúng; số đối tượng có geometry phải lớn hơn 0; kiểm tra bằng trình duyệt desktop/mobile, đặc biệt nhãn và nút không chồng nhau/cắt chữ.
- Bản này không thay wireframe F2/F3 quản trị của Anh Tú, check-in/thanh toán của Lân, cổng hội viên/PT của Trọng Việt hoặc dashboard của Minh Quân.
