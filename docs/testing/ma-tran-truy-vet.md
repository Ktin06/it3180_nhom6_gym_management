# Ma trận truy vết UC → US → AC → TC

> **File sinh tự động, không sửa tay.** Chạy `node scripts/export-truy-vet.cjs` ở gốc repo để sinh lại.
> Nguồn: mã UC, US, AC, BR trong `docs/requirements/srs/`; mã TC trong tên kiểm thử backend (JUnit `@DisplayName`)
> và frontend (Vitest `it`, `test`, `describe`). Quy ước và cách dùng: [README](README.md), [ADR 0012](../decisions/0012-quy-uoc-ma-truy-vet.md).

## 1. Tổng quan theo chức năng

| Chức năng | UC | US | AC | AC đã có TC | AC chưa có TC | TC |
| --- | --- | --- | --- | --- | --- | --- |
| F1 | 6 | 6 | 31 | 30 | 1 | 144 |
| F2 | 5 | 6 | 31 | 27 | 4 | 110 |
| F3 | 5 | 6 | 45 | 45 | 0 | 137 |
| F4 | 5 | 5 | 37 | 0 | 37 | 0 |
| F5 | 5 | 5 | 49 | 49 | 0 | 154 |
| F6 | 5 | 5 | 16 | 0 | 16 | 0 |
| F7 | 5 | 5 | 34 | 14 | 20 | 31 |
| F8 | 9 | 9 | 55 | 53 | 2 | 81 |
| F9 | 7 | 7 | 63 | 0 | 63 | 0 |
| F10 | 5 | 4 | 25 | 12 | 13 | 29 |
| **Tổng** | **57** | **58** | **386** | **230** | **156** | **686** |

Chức năng có US = 0 là chức năng chưa viết lại SRS theo ADR 0012 (chưa có mã US, AC).

## 2. Lỗi cần sửa

### 2.1. TC trỏ tới AC không tồn tại hoặc đã bỏ

Không có.

### 2.2. Mã AC bị trùng trong SRS

Không có.

### 2.3. US không nằm dưới UC nào

Không có.

## 3. AC chưa có TC

Một US chỉ đủ điều kiện nghiệm thu và đóng khi không còn AC nào của nó trong bảng này ([CONTRIBUTING.md, mục 5](../../CONTRIBUTING.md#5-định-nghĩa-hoàn-thành)).

| AC | Tên | US |
| --- | --- | --- |
| AC-F1.2.1-06 | Từ chối dữ liệu đăng nhập sai định dạng (không hỗ trợ đăng nhập bằng số điện thoại) | US-F1.2.1 |
| AC-F2.5.1-01 | Ghi chỉ số cơ thể thành công với ít nhất một chỉ số | US-F2.5.1 |
| AC-F2.5.1-02 | Từ chối dữ liệu chỉ số cơ thể không hợp lệ | US-F2.5.1 |
| AC-F2.5.1-03 | Xem lịch sử chỉ số cơ thể sắp xếp mới nhất trước | US-F2.5.1 |
| AC-F2.5.1-04 | Phân quyền ghi và xem chỉ số cơ thể theo vai trò, chi nhánh và quan hệ PT | US-F2.5.1 |
| AC-F4.1.1-01 | Tìm theo mã hội viên hoặc số điện thoại | US-F4.1.1 |
| AC-F4.1.1-02 | Hiển thị ảnh đại diện và thông tin lớp học sắp diễn ra | US-F4.1.1 |
| AC-F4.1.1-03 | Từ khóa tìm kiếm sai định dạng | US-F4.1.1 |
| AC-F4.1.1-04 | Không tìm thấy hội viên hoặc hội viên thuộc chi nhánh khác | US-F4.1.1 |
| AC-F4.1.1-05 | Hồ sơ hội viên ngừng hoạt động | US-F4.1.1 |
| AC-F4.1.1-06 | Phân quyền truy cập chức năng tra cứu | US-F4.1.1 |
| AC-F4.2.1-01 | Check-in thành công với gói TIME | US-F4.2.1 |
| AC-F4.2.1-02 | Xác định ngày check-in theo múi giờ Việt Nam | US-F4.2.1 |
| AC-F4.2.1-03 | Gói SESSION trừ đúng một buổi mỗi ngày dương lịch | US-F4.2.1 |
| AC-F4.2.1-04 | Ưu tiên gói TIME khi hội viên có cả TIME và SESSION | US-F4.2.1 |
| AC-F4.2.1-05 | Buổi tập cuối của gói SESSION vẫn được tập không giới hạn hết ngày | US-F4.2.1 |
| AC-F4.2.1-06 | Trừ buổi cuối kéo gói gia hạn UPCOMING về ngày mai | US-F4.2.1 |
| AC-F4.2.1-07 | Check-in bằng gói PT đúng ngày có buổi tập hẹn trước | US-F4.2.1 |
| AC-F4.2.1-08 | Ràng buộc biên của quy tắc check-in gói PT | US-F4.2.1 |
| AC-F4.2.1-09 | Phạm vi hiệu lực của gói toàn chuỗi và gói riêng chi nhánh | US-F4.2.1 |
| AC-F4.2.1-10 | Từ chối khi không có gói tập nào hợp lệ | US-F4.2.1 |
| AC-F4.2.1-11 | Từ chối khi gói SESSION hết buổi và hôm nay chưa trừ buổi | US-F4.2.1 |
| AC-F4.2.1-12 | Từ chối khi gói tập đang bảo lưu | US-F4.2.1 |
| AC-F4.2.1-13 | Thứ tự ưu tiên thông báo lỗi khi có nhiều lý do vi phạm | US-F4.2.1 |
| AC-F4.2.1-14 | Xử lý khi hội viên có nhiều gói SESSION cùng dùng được | US-F4.2.1 |
| AC-F4.2.1-15 | Từ chối khi hồ sơ hội viên INACTIVE hoặc ngoài phạm vi | US-F4.2.1 |
| AC-F4.2.1-16 | Kiểm tra giờ mở cửa phòng tập | US-F4.2.1 |
| AC-F4.2.1-17 | Xử lý cạnh tranh khi hai lễ tân cùng check-in một hội viên | US-F4.2.1 |
| AC-F4.2.1-18 | Kiểm tra tính hợp lệ của dữ liệu đầu vào và nhân viên thao tác | US-F4.2.1 |
| AC-F4.3.1-01 | Ghi nhận thời điểm đến lớp trong khung giờ quy định | US-F4.3.1 |
| AC-F4.3.1-02 | Không ghi nhận thời điểm đến khi ngoài khung giờ hoặc sai chi nhánh | US-F4.3.1 |
| AC-F4.3.1-03 | Chỗ đặt lớp đã điểm danh hoặc kết thúc giữ nguyên trạng thái | US-F4.3.1 |
| AC-F4.3.1-04 | Không ghi đè và không ghi nhận khi lượt check-in thất bại | US-F4.3.1 |
| AC-F4.4.1-01 | Check-out thành công cho lượt tập đang diễn ra | US-F4.4.1 |
| AC-F4.4.1-02 | Xử lý trường hợp hội viên có nhiều lượt check-in mở trong ngày | US-F4.4.1 |
| AC-F4.4.1-03 | Từ chối khi không tìm thấy lượt tập đang mở hoặc khác chi nhánh | US-F4.4.1 |
| AC-F4.4.1-04 | Xử lý xung đột đồng thời khi hai nhân viên cùng check-out một lượt | US-F4.4.1 |
| AC-F4.4.1-05 | Phân quyền truy cập chức năng check-out | US-F4.4.1 |
| AC-F4.5.1-01 | Mặc định hiển thị danh sách trong ngày hiện tại theo giờ Việt Nam | US-F4.5.1 |
| AC-F4.5.1-02 | Đầy đủ thông tin chi tiết của từng lượt check-in | US-F4.5.1 |
| AC-F4.5.1-03 | Lọc trạng thái, phương thức và tìm kiếm theo tên hoặc mã hội viên | US-F4.5.1 |
| AC-F4.5.1-04 | Phân quyền và phạm vi dữ liệu theo chi nhánh | US-F4.5.1 |
| AC-F6.1.1-01 | Đếm chính xác số hội viên duy nhất đang tập và tổng lượt check-in | US-F6.1.1 |
| AC-F6.1.1-02 | Chủ chuỗi bắt buộc phải chọn chi nhánh cụ thể | US-F6.1.1 |
| AC-F6.1.1-03 | Nhân viên chi nhánh chỉ xem số liệu chi nhánh mình | US-F6.1.1 |
| AC-F6.1.1-04 | Phân quyền truy cập các chỉ số Dashboard | US-F6.1.1 |
| AC-F6.1.1-05 | Chủ chuỗi xem số liệu của chi nhánh đã ngừng hoạt động | US-F6.1.1 |
| AC-F6.2.1-01 | Biểu đồ thể hiện đầy đủ các khung giờ trong giờ mở cửa | US-F6.2.1 |
| AC-F6.2.1-02 | Xử lý khung giờ với chi nhánh có giờ mở cửa lẻ | US-F6.2.1 |
| AC-F6.3.1-01 | Doanh thu bao gồm cả khách lẻ và phân loại theo hình thức thanh toán | US-F6.3.1 |
| AC-F6.4.1-01 | Lọc chính xác các gói hết hạn trong vòng 7 ngày tới | US-F6.4.1 |
| AC-F6.4.1-02 | Cảnh báo gói SESSION sắp hết buổi theo cấu hình ngưỡng | US-F6.4.1 |
| AC-F6.4.1-03 | Loại trừ hội viên đã mua gói gia hạn UPCOMING | US-F6.4.1 |
| AC-F6.4.1-04 | Gói toàn chuỗi chỉ hiển thị cảnh báo tại chi nhánh gốc | US-F6.4.1 |
| AC-F6.4.1-05 | Quy tắc sắp xếp, lý do cảnh báo, giới hạn phân trang và loại trừ hội viên xóa mềm | US-F6.4.1 |
| AC-F6.4.1-06 | Thao tác Gia hạn nhanh trên giao diện | US-F6.4.1 |
| AC-F6.5.1-01 | Danh sách các lượt check-in mới nhất trong ngày | US-F6.5.1 |
| AC-F6.5.1-02 | Thao tác Check-out nhanh từ danh sách Live Feed | US-F6.5.1 |
| AC-F7.1.1-01 | Hội viên thấy đủ thông tin các gói của mình | US-F7.1.1 |
| AC-F7.1.1-02 | Chỉ thấy gói của chính mình | US-F7.1.1 |
| AC-F7.1.1-03 | Hội viên chưa có gói nào | US-F7.1.1 |
| AC-F7.1.1-04 | Gói đã hủy không có nút Gia hạn, gói hết hạn vẫn có | US-F7.1.1 |
| AC-F7.1.1-05 | Từ chối người không được xem | US-F7.1.1 |
| AC-F7.2.1-01 | Xem lịch sử mới nhất trước, có phân trang | US-F7.2.1 |
| AC-F7.2.1-02 | Hội viên chưa check-in lần nào | US-F7.2.1 |
| AC-F7.2.1-03 | Chỉ thấy lượt của chính mình | US-F7.2.1 |
| AC-F7.2.1-04 | Tham số phân trang | US-F7.2.1 |
| AC-F7.2.1-05 | Tải trang dưới 2 giây | US-F7.2.1 |
| AC-F7.2.1-06 | Từ chối người không được xem | US-F7.2.1 |
| AC-F7.3.1-01 | Xem hóa đơn của mình, mới nhất trước | US-F7.3.1 |
| AC-F7.3.1-02 | Hóa đơn chờ do cổng hội viên kèm hạn tự hủy | US-F7.3.1 |
| AC-F7.3.1-03 | Chỉ thấy hóa đơn của chính mình | US-F7.3.1 |
| AC-F7.3.1-04 | Hội viên chưa có hóa đơn nào | US-F7.3.1 |
| AC-F7.3.1-05 | Tham số phân trang | US-F7.3.1 |
| AC-F7.3.1-06 | Từ chối người không được xem | US-F7.3.1 |
| AC-F7.4.1-02 | Gói chỉ hiệu lực sau khi lễ tân thu tiền | US-F7.4.1 |
| AC-F7.5.1-07 | Hai yêu cầu gia hạn gửi cùng lúc | US-F7.5.1 |
| AC-F7.5.1-08 | Hóa đơn gia hạn tự hủy kéo theo gói chờ | US-F7.5.1 |
| AC-F8.7.1-06 | Giảm sức chứa đúng biên | US-F8.7.1 |
| AC-F8.7.1-07 | Buổi không cho sửa sức chứa | US-F8.7.1 |
| AC-F9.1.1-01 | Khai báo khung rảnh hợp lệ | US-F9.1.1 |
| AC-F9.1.1-02 | Dữ liệu sai bị từ chối | US-F9.1.1 |
| AC-F9.1.1-03 | Hai khung của cùng một PT không chồng nhau | US-F9.1.1 |
| AC-F9.1.1-04 | Khung liền kề hoặc khác thứ không bị coi là chồng | US-F9.1.1 |
| AC-F9.1.1-05 | Hai PT khác nhau được trùng giờ | US-F9.1.1 |
| AC-F9.1.1-06 | Sửa khung rảnh của mình | US-F9.1.1 |
| AC-F9.1.1-07 | Không xóa hoặc thu hẹp khung đang có buổi đã đặt | US-F9.1.1 |
| AC-F9.1.1-08 | Buổi đã xong hoặc đã hủy không chặn việc xóa khung | US-F9.1.1 |
| AC-F9.1.1-09 | Xem lịch dạy gồm khung rảnh, buổi PT và lớp nhóm | US-F9.1.1 |
| AC-F9.1.1-10 | PT chỉ thao tác trên lịch của mình | US-F9.1.1 |
| AC-F9.1.1-11 | Từ chối người không phải PT | US-F9.1.1 |
| AC-F9.2.1-01 | Đặt buổi hợp lệ chỉ giữ chỗ, không trừ buổi | US-F9.2.1 |
| AC-F9.2.1-02 | Chỉ đặt trong giờ PT rảnh | US-F9.2.1 |
| AC-F9.2.1-03 | Đặt sát giờ bị từ chối, đúng mốc thì được | US-F9.2.1 |
| AC-F9.2.1-04 | Số giờ tối thiểu đọc từ cấu hình | US-F9.2.1 |
| AC-F9.2.1-05 | Không trùng lịch của PT | US-F9.2.1 |
| AC-F9.2.1-06 | Không trùng lịch của hội viên | US-F9.2.1 |
| AC-F9.2.1-07 | Phải có gói PT đang hiệu lực | US-F9.2.1 |
| AC-F9.2.1-08 | Không đặt quá số buổi còn lại, kể cả buổi đã qua giờ chưa xác nhận | US-F9.2.1 |
| AC-F9.2.1-09 | Còn chỗ khi số buổi `BOOKED` ít hơn số buổi còn lại | US-F9.2.1 |
| AC-F9.2.1-10 | Hai yêu cầu cùng lúc cho một giờ | US-F9.2.1 |
| AC-F9.2.1-11 | Dữ liệu sai, PT không tồn tại | US-F9.2.1 |
| AC-F9.2.1-12 | Từ chối người không được đặt | US-F9.2.1 |
| AC-F9.3.1-01 | Xác nhận buổi đã diễn ra, trừ một buổi | US-F9.3.1 |
| AC-F9.3.1-02 | Hoa hồng chốt lúc xác nhận và không đổi về sau | US-F9.3.1 |
| AC-F9.3.1-03 | Chưa tới giờ bắt đầu thì không xác nhận được | US-F9.3.1 |
| AC-F9.3.1-04 | Không xác nhận hai lần | US-F9.3.1 |
| AC-F9.3.1-05 | Hai thao tác cùng lúc chỉ trừ một lần | US-F9.3.1 |
| AC-F9.3.1-06 | Buổi cuối thì gói hết hạn và gói nối tiếp được kích hoạt ngay | US-F9.3.1 |
| AC-F9.3.1-07 | Buổi cuối nhưng không có gói nối tiếp | US-F9.3.1 |
| AC-F9.3.1-08 | Hội viên vắng mặt vẫn bị trừ buổi, không có hoa hồng | US-F9.3.1 |
| AC-F9.3.1-09 | Hạn xác nhận 23:59 cùng ngày | US-F9.3.1 |
| AC-F9.3.1-10 | Chỉ PT được phân công mới xác nhận | US-F9.3.1 |
| AC-F9.4.1-01 | Soạn giáo án cho học viên có buổi PT với mình | US-F9.4.1 |
| AC-F9.4.1-02 | Soạn giáo án cho học viên có đặt chỗ lớp do mình dạy | US-F9.4.1 |
| AC-F9.4.1-03 | Không soạn cho học viên mình không dạy | US-F9.4.1 |
| AC-F9.4.1-04 | Hội viên không tồn tại | US-F9.4.1 |
| AC-F9.4.1-05 | Dữ liệu sai bị từ chối | US-F9.4.1 |
| AC-F9.4.1-06 | Từ chối người không phải PT | US-F9.4.1 |
| AC-F9.5.1-01 | Ghi nhật ký tập cho học viên của mình | US-F9.5.1 |
| AC-F9.5.1-02 | Ghi chỉ số cơ thể, người ghi lấy từ token | US-F9.5.1 |
| AC-F9.5.1-03 | Nhật ký gắn buổi PT phải thuộc đúng PT và hội viên | US-F9.5.1 |
| AC-F9.5.1-04 | Dữ liệu sai bị từ chối | US-F9.5.1 |
| AC-F9.5.1-05 | Không ghi cho học viên mình không dạy, hội viên không tồn tại | US-F9.5.1 |
| AC-F9.5.1-06 | Từ chối người không phải PT | US-F9.5.1 |
| AC-F9.6.1-01 | Hội viên xem tiến độ của chính mình | US-F9.6.1 |
| AC-F9.6.1-02 | PT xem tiến độ học viên mình dạy | US-F9.6.1 |
| AC-F9.6.1-03 | Không xem tiến độ của người khác | US-F9.6.1 |
| AC-F9.6.1-04 | Chưa có dữ liệu | US-F9.6.1 |
| AC-F9.6.1-05 | Khoảng thời gian sai | US-F9.6.1 |
| AC-F9.6.1-06 | Chỉ dựa trên dữ liệu thực, không có gợi ý AI | US-F9.6.1 |
| AC-F9.6.1-07 | Từ chối người chưa đăng nhập hoặc vai trò khác | US-F9.6.1 |
| AC-F9.7.1-01 | Hội viên hủy trước hạn, không mất buổi | US-F9.7.1 |
| AC-F9.7.1-02 | Hủy sát giờ bị từ chối, đúng mốc thì được | US-F9.7.1 |
| AC-F9.7.1-03 | Mốc hủy đọc từ cấu hình | US-F9.7.1 |
| AC-F9.7.1-04 | Lễ tân hủy hộ theo đúng quy tắc của hội viên | US-F9.7.1 |
| AC-F9.7.1-05 | Quản lý chi nhánh hủy hộ, có lý do, có dấu vết | US-F9.7.1 |
| AC-F9.7.1-06 | Quản lý hủy hộ phải nhập lý do | US-F9.7.1 |
| AC-F9.7.1-07 | Quản lý không hủy buổi đã tới giờ bắt đầu | US-F9.7.1 |
| AC-F9.7.1-08 | Chỉ hủy được buổi đang `BOOKED` | US-F9.7.1 |
| AC-F9.7.1-09 | PT không tự hủy buổi đã có khách | US-F9.7.1 |
| AC-F9.7.1-10 | Phân quyền theo chủ buổi và chi nhánh | US-F9.7.1 |
| AC-F9.7.1-11 | Hai yêu cầu hủy cùng lúc | US-F9.7.1 |
| AC-F10.2.1-04 | Không tạo thông báo cho hội viên chưa có tài khoản | US-F10.2.1 |
| AC-F10.4.1-01 | Tạo thông báo khi nhận sự kiện đổi giờ từ F8 | US-F10.4.1 |
| AC-F10.4.1-02 | Nội dung thông báo có giờ cũ và giờ mới | US-F10.4.1 |
| AC-F10.4.1-03 | Không tạo thông báo khi buổi lớp không có hội viên đặt chỗ | US-F10.4.1 |
| AC-F10.4.1-04 | Chỉ báo hội viên còn giữ chỗ | US-F10.4.1 |
| AC-F10.4.1-05 | Không tạo thông báo cho hội viên chưa có tài khoản | US-F10.4.1 |
| AC-F10.4.1-06 | Gửi thông báo qua hai kênh và xử lý lỗi email | US-F10.4.1 |
| AC-F10.5.1-01 | Xem danh sách thông báo của tài khoản đang đăng nhập | US-F10.5.1 |
| AC-F10.5.1-02 | Sắp xếp và phân trang danh sách thông báo | US-F10.5.1 |
| AC-F10.5.1-03 | Hiển thị đầy đủ nội dung và trạng thái đã đọc | US-F10.5.1 |
| AC-F10.5.1-04 | Đánh dấu thông báo đã đọc | US-F10.5.1 |
| AC-F10.5.1-05 | Không cho phép xem hoặc đánh dấu thông báo của người khác | US-F10.5.1 |
| AC-F10.5.1-06 | Xử lý dữ liệu đầu vào sai và thông báo không tồn tại | US-F10.5.1 |

## 4. Cây xuôi: chức năng → UC → US → AC → TC

### F1

- **UC-F1.1** Khách tự đăng ký tài khoản hội viên
  - **US-F1.1.1** Tự đăng ký tài khoản hội viên (#117) ✅ mọi AC có TC
    - AC-F1.1.1-01 Đăng ký thành công tạo đồng thời tài khoản và hồ sơ hội viên (BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-09)
      - TC-F1.1.1-01a `AuthControllerTest#dangKyThanhCongTra201KemCookieVaAccessToken`
      - TC-F1.1.1-01b `AuthServiceTest#dangKyThanhCongTaoUserVaGoiMemberService`
      - TC-F1.1.1-01c `F1RegistrationIntegrationTest#realF1RegistrationCreatesProfileAndJwtCanReadOwnF2Profile`
      - TC-F1.1.1-01d `features/auth/pages/RegisterPage.test.tsx › TC-F1.1.1-01d: chuẩn hóa dữ liệu, giữ họ tên tiếng Việt và t`
    - AC-F1.1.1-02 Từ chối khi số điện thoại đã thuộc hồ sơ hội viên có sẵn (BR-F1-02)
      - TC-F1.1.1-02a `F1RegistrationIntegrationTest#phoneAlreadyInRealMembersTableRejectsRegistrationAndRollsBackUser`
      - TC-F1.1.1-02b `MemberApiIntegrationTest#registrationPortJoinsCallerTransactionAndRollsBackOnDuplicate`
      - TC-F1.1.1-02c `MemberServiceTest#concurrentDuplicatePhoneStillGetsFrontDeskGuidance`
      - TC-F1.1.1-02d `features/auth/pages/RegisterPage.test.tsx › TC-F1.1.1-02d: lỗi 409 không có errors hiện chung, không suy`
    - AC-F1.1.1-03 Từ chối khi email đã được sử dụng (BR-F1-01)
      - TC-F1.1.1-03a `AuthServiceTest#dangKyEmailDaTonTaiNemConflictException`
      - TC-F1.1.1-03b `MemberApiIntegrationTest#registrationPortJoinsCallerTransactionAndRollsBackOnDuplicate`
      - TC-F1.1.1-03c `features/auth/pages/RegisterPage.test.tsx › TC-F1.1.1-03c: dùng tên trường lỗi thay vì suy từ câu báo lỗ`
    - AC-F1.1.1-04 Từ chối dữ liệu đầu vào sai định dạng (BR-F1-01, BR-F1-03)
      - TC-F1.1.1-04a `AuthControllerTest#dangKyThieuEmailTra400KemProblemDetailErrors`
      - TC-F1.1.1-04b `AuthControllerTest#dangKyMatKhauThieuChuSoTra400TheoS1`
      - TC-F1.1.1-04c `AuthControllerTest#dangKyMatKhauDaiHon72KyTuTra400ThayVi500`
      - TC-F1.1.1-04d `AuthControllerTest#dangKyMatKhauCoDau50KyTuVuot72ByteTra400ThayVi422`
      - TC-F1.1.1-04e `AuthControllerTest#dangKySoDienThoaiSaiDinhDangTra400`
      - TC-F1.1.1-04f `AuthControllerTest#dangKyNgaySinhTuongLaiTra400`
      - TC-F1.1.1-04g `AuthServiceTest#dangKySoDienThoaiSaiDinhDangNemBusinessRuleException`
      - TC-F1.1.1-04h `AuthServiceTest#dangKyMatKhauVuotQua72ByteUtf8NemBusinessRuleException`
      - TC-F1.1.1-04i `MemberApiIntegrationTest#directRegistrationUsesTheSameDtoConstraints`
      - TC-F1.1.1-04j `features/auth/pages/RegisterPage.test.tsx › TC-F1.1.1-04j: không gửi dữ liệu khi số điện thoại hoặc mật `
      - TC-F1.1.1-04k `features/auth/api/validation.test.ts › TC-F1.1.1-04k: khớp quy tắc chữ ASCII và số của backend, vẫn`
      - TC-F1.1.1-04l `features/auth/api/validation.test.ts › TC-F1.1.1-04l: giới hạn đúng 72 byte UTF-8 thay vì đếm ký tự`
    - AC-F1.1.1-05 Hoàn tác toàn bộ giao dịch khi xảy ra lỗi giữa chừng hoặc chi nhánh mặc định ngừng hoạt động (BR-F1-09)
      - TC-F1.1.1-05a `F1RegistrationIntegrationTest#failureAfterPersistingProfileRollsBackBothUserAndMember`
      - TC-F1.1.1-05b `F1RegistrationIntegrationTest#inactiveDefaultBranchRejectsRealRegistrationAndRollsBackNewAccount`
      - TC-F1.1.1-05c `MemberApiIntegrationTest#registrationPortJoinsCallerTransactionAndRollsBackOnDuplicate`
      - TC-F1.1.1-05d `StaffBranchIntegrationTest#missingDefaultBranchIsNotFoundInsteadOfCreatingOne`
      - TC-F1.1.1-05e `StaffBranchIntegrationTest#inactiveDefaultBranchIsNotFound`
- **UC-F1.2** Đăng nhập bằng email
  - **US-F1.2.1** Đăng nhập bằng email (#118)
    - AC-F1.2.1-01 Nhập sai mật khẩu từ lần 1 đến lần 4 trả 401 thông báo chung, tăng bộ đếm và không lộ email tồn tại (BR-F1-01, BR-F1-03, BR-F1-06)
      - TC-F1.2.1-01a `AuthServiceTest#saiMatKhauLan1Den4Tra401CauChungVaTangBoDem`
      - TC-F1.2.1-01b `AuthServiceTest#khongLoEmailKhiEmailKhongTonTai`
      - TC-F1.2.1-01c `AuthServiceTest#taiKhoanDisabledNhapSaiMatKhauTra401KhongPhai403`
      - TC-F1.2.1-01d `AuthControllerTest#dangNhapSaiMatKhauTra401ProblemDetailTiengViet`
      - TC-F1.2.1-01e `AuthServiceTest#dangNhapSaiMatKhauNemUnauthorizedException`
      - TC-F1.2.1-01f `AuthServiceTest#dangNhapMatKhauVuotQua72ByteNemUnauthorizedExceptionKhongNem500`
      - TC-F1.2.1-01g `AuthServiceTest#dangNhapEmailKhongTonTaiVanChayPasswordEncoderMatchesDeChongDoanEmailQuaThoiGian`
    - AC-F1.2.1-02 Nhập sai mật khẩu lần thứ 5 liên tiếp (kể cả gửi đồng thời) đặt bộ đếm bằng 5 và khóa tạm 15 phút (BR-F1-06)
      - TC-F1.2.1-02a `AuthServiceTest#lanSaiThu5Khoa15Phut`
      - TC-F1.2.1-02b `AuthSessionConcurrencyIntegrationTest#namRequestDangNhapSaiDongThoiTangBoDemDung5VaKhoaTaiKhoan`
    - AC-F1.2.1-03 Từ chối mọi yêu cầu đăng nhập trong thời gian đang bị khóa tạm 15 phút, kể cả nhập đúng mật khẩu (BR-F1-06)
      - TC-F1.2.1-03a `AuthServiceTest#dangKhoaNhapDungMatKhauVanTra403KemThoiGianConLai`
    - AC-F1.2.1-04 Đăng nhập thành công hoặc hết thời gian khóa tạm (BR-F1-01, BR-F1-04, BR-F1-06)
      - TC-F1.2.1-04a `AuthServiceTest#hetKhoaThiDangNhapLaiDuoc`
      - TC-F1.2.1-04b `AuthServiceTest#dangNhapDungDatLaiBoDemVe0VaXoaLockedUntil`
      - TC-F1.2.1-04c `AuthServiceTest#hetKhoaRoiNhapSaiThiBoDemTinhLaiTu1`
      - TC-F1.2.1-04d `AuthControllerTest#dangNhapThanhCongTra200KemCookieVaAccessToken`
      - TC-F1.2.1-04e `AuthServiceTest#dangNhapThanhCongCapTokenVaCapNhatLastLoginAt`
      - TC-F1.2.1-04f `JwtSecurityIntegrationTest#tokenHopLeLayThongTinTaiKhoanHienTaiThanhCong`
      - TC-F1.2.1-04g `features/auth/pages/LoginPage.test.tsx › TC-F1.2.1-04g: đăng nhập bằng email đã chuẩn hóa rồi mở tran`
    - AC-F1.2.1-05 Từ chối tài khoản bị khóa hoặc ngừng hoạt động khi nhập đúng mật khẩu (BR-F1-05)
      - TC-F1.2.1-05a `AuthControllerTest#dangNhapTaiKhoanBiKhoaTra403ProblemDetailTiengViet`
      - TC-F1.2.1-05b `AuthServiceTest#dangNhapTaiKhoanBiKhoaNemForbiddenException`
    - AC-F1.2.1-06 Từ chối dữ liệu đăng nhập sai định dạng (không hỗ trợ đăng nhập bằng số điện thoại) (BR-F1-01) ❌ chưa có TC
- **UC-F1.3** Làm mới phiên đăng nhập (Refresh Token) & Đăng xuất
  - **US-F1.3.1** Làm mới phiên và đăng xuất (#119) ✅ mọi AC có TC
    - AC-F1.3.1-01 Xoay vòng refresh token và chặn dùng lại token cũ (BR-F1-04)
      - TC-F1.3.1-01a `AuthControllerTest#lamMoiPhienThanhCongTra200KemCookieMoi`
      - TC-F1.3.1-01b `AuthServiceTest#lamMoiThanhCongXoayVongRefreshToken`
      - TC-F1.3.1-01c `AuthServiceTest#lamMoiTokenDaBiThuHoiThuHoiToanBoPhienVaNemUnauthorizedException`
      - TC-F1.3.1-01d `AuthSessionConcurrencyIntegrationTest#haiYeuCauRefreshDongThoiChiMotThanhCongVaMotNhan401`
      - TC-F1.3.1-01e `auth/AuthProvider.test.tsx › TC-F1.3.1-01e: bootstrap làm mới cookie rồi lấy user từ /me`
    - AC-F1.3.1-02 Đăng xuất thu hồi refresh token và xóa cookie (BR-F1-04)
      - TC-F1.3.1-02a `AuthControllerTest#dangXuatThanhCongTra204VaXoaCookie`
      - TC-F1.3.1-02b `AuthControllerTest#dangXuatCoJwtCungNguoiDungThuHoiVaXoaCookie`
      - TC-F1.3.1-02c `AuthControllerTest#dangXuatCoJwtHetHanVanDoiChieuChuSoHuuCookie`
      - TC-F1.3.1-02d `AuthServiceTest#dangXuatThuHoiRefreshToken`
      - TC-F1.3.1-02e `AuthServiceTest#dangXuatVoiJwtThuHoiCookieKhiCungChuSoHuu`
      - TC-F1.3.1-02f `AuthSessionConcurrencyIntegrationTest#dangXuatBangJwtHetHanCuaTaiKhoanAVanGiuPhienCuaTaiKhoanB`
      - TC-F1.3.1-02g `auth/AuthProvider.test.tsx › TC-F1.3.1-02g: logout chỉ xóa phiên sau khi server xác nhận;`
    - AC-F1.3.1-03 Từ chối khi phiên không hợp lệ và xóa cookie (BR-F1-04)
      - TC-F1.3.1-03a `AuthControllerTest#lamMoiThieuCookieTra401ProblemDetailVaXoaCookie`
      - TC-F1.3.1-03b `AuthServiceTest#lamMoiTokenDaBiThuHoiThuHoiToanBoPhienVaNemUnauthorizedException`
      - TC-F1.3.1-03c `AuthServiceTest#lamMoiTokenHetHanNemUnauthorizedException`
      - TC-F1.3.1-03d `auth/AuthProvider.test.tsx › TC-F1.3.1-03d: bootstrap không có cookie kết thúc anonymous`
    - AC-F1.3.1-04 Đồng bộ làm mới phiên và đăng xuất giữa nhiều tab trình duyệt (BR-F1-04)
      - TC-F1.3.1-04a `AuthControllerTest#dangXuatCoJwtKhacChuCookieKhongXoaCookieChung`
      - TC-F1.3.1-04b `AuthControllerTest#dangXuatCoJwtHetHanVanDoiChieuChuSoHuuCookie`
      - TC-F1.3.1-04c `AuthServiceTest#dangXuatVoiJwtKhongThuHoiCookieCuaTaiKhoanKhac`
      - TC-F1.3.1-04d `AuthSessionConcurrencyIntegrationTest#dangXuatBangJwtCuaTaiKhoanAKhongThuHoiHoacXoaCookieCuaTaiKhoanB`
      - TC-F1.3.1-04e `AuthSessionConcurrencyIntegrationTest#dangXuatBangJwtHetHanCuaTaiKhoanAVanGiuPhienCuaTaiKhoanB`
      - TC-F1.3.1-04f `auth/account-change.test.tsx › TC-F1.3.1-04f: refresh khác tài khoản đóng trang bảo vệ và đ`
- **UC-F1.4** Đổi mật khẩu
  - **US-F1.4.1** Đổi mật khẩu (#120) ✅ mọi AC có TC
    - AC-F1.4.1-01 Đổi mật khẩu thành công, thu hồi toàn bộ phiên và yêu cầu đăng nhập lại (BR-F1-03, BR-F1-04)
      - TC-F1.4.1-01a `ChangePasswordIntegrationTest#doiMatKhauThanhCongMatKhauMoiDangNhapDuocMatKhauCuKhong`
      - TC-F1.4.1-01b `ChangePasswordIntegrationTest#doiMatKhauThuHoiMoiRefreshTokenCuaNguoiDung`
      - TC-F1.4.1-01c `AuthServiceTest#doiMatKhauThanhCongCapNhatPasswordHash`
      - TC-F1.4.1-01d `AuthServiceTest#doiMatKhauThuHoiMoiPhienCuaNguoiDung`
      - TC-F1.4.1-01e `features/auth/api/auth-api.test.ts › TC-F1.4.1-01e: changePassword gọi POST /auth/change-password`
      - TC-F1.4.1-01f `features/auth/pages/ChangePasswordPage.test.tsx › TC-F1.4.1-01f: Đổi mật khẩu thành công gọi API, xóa phiên tr`
      - TC-F1.4.1-01g `auth/AuthProvider.test.tsx › TC-F1.4.1-01g: completePasswordChange phát publishSessionCha`
      - TC-F1.4.1-01h `ChangePasswordIntegrationTest#concurrentPasswordChangesAcceptOldPasswordOnlyOnce`
      - TC-F1.4.1-01i `ChangePasswordIntegrationTest#passwordChangeRevokesInFlightRefreshSuccessor`
      - TC-F1.4.1-01j `features/auth/pages/ChangePasswordPage.test.tsx › TC-F1.4.1-01j: Phản hồi đổi mật khẩu đến muộn giữ phiên mới `
      - TC-F1.4.1-01k `auth/AuthProvider.test.tsx › TC-F1.4.1-01k: giữ lượt đăng nhập mới cùng tài khoản khi tab`
    - AC-F1.4.1-02 Từ chối khi mật khẩu hiện tại sai hoặc mật khẩu mới trùng mật khẩu cũ (BR-F1-07)
      - TC-F1.4.1-02a `AuthServiceTest#doiMatKhauHienTaiSaiHoacMatKhauMoiTrungCuNemBusinessRuleException`
      - TC-F1.4.1-02b `AuthControllerTest#doiMatKhauViPhamQuyTacNghiepVuTra422`
      - TC-F1.4.1-02c `features/auth/pages/ChangePasswordPage.test.tsx › TC-F1.4.1-02c: Lỗi 422 từ API hiển thị đúng thông báo và khô`
    - AC-F1.4.1-03 Từ chối khi dữ liệu đầu vào sai định dạng hoặc mật khẩu mới không đạt chuẩn bảo mật (BR-F1-03)
      - TC-F1.4.1-03a `AuthControllerTest#doiMatKhauSaiDinhDangTra400KemErrors`
      - TC-F1.4.1-03b `features/auth/pages/ChangePasswordPage.test.tsx › TC-F1.4.1-03b: Kiểm tra quy tắc mật khẩu S1, khớp mật khẩu n`
    - AC-F1.4.1-04 Chặn truy cập khi chưa đăng nhập và mở đúng khung giao diện cho mọi vai trò đã đăng nhập (BR-F1-05)
      - TC-F1.4.1-04a `AuthControllerTest#doiMatKhauKhongCoTokenTra401`
      - TC-F1.4.1-04b `features/auth/pages/ChangePasswordPage.test.tsx › TC-F1.4.1-04b: mở /doi-mat-khau đúng layout theo vai trò và `
      - TC-F1.4.1-04c `layouts/UserMenu.test.tsx › TC-F1.4.1-04c: UserMenu có mục Đổi mật khẩu cho mọi vai trò,`
      - TC-F1.4.1-04d `layouts/UserMenu.test.tsx › TC-F1.4.1-04d: ẩn chữ của nút trên màn hẹp 360px, giữ tên nú`
      - TC-F1.4.1-04e `AuthServiceTest#doiMatKhauTaiKhoanKhongActiveNemForbiddenException`
    - AC-F1.4.1-05 Nhập sai mật khẩu hiện tại dùng chung bộ đếm khóa tạm với đăng nhập (BR-F1-06)
      - TC-F1.4.1-05a `ChangePasswordIntegrationTest#saiMatKhauHienTaiNhieuLanThiKhoaTam`
      - TC-F1.4.1-05b `ChangePasswordIntegrationTest#dangNhapSaiVaDoiMatKhauSaiDungChungBoDem`
      - TC-F1.4.1-05c `features/auth/pages/ChangePasswordPage.test.tsx › TC-F1.4.1-05c: Tài khoản đang khóa tạm (403) thì hiển thị câ`
    - AC-F1.4.1-06 Đổi mật khẩu thành công đặt lại bộ đếm sai và khóa tạm (BR-F1-04, BR-F1-06)
      - TC-F1.4.1-06a `ChangePasswordIntegrationTest#doiMatKhauThanhCongDatLaiBoDemVaXoaCookie`
      - TC-F1.4.1-06b `ChangePasswordIntegrationTest#matKhauMoiTrungKhongTangBoDem`
- **UC-F1.5** Quên mật khẩu và đặt lại mật khẩu (Sprint 2)
  - **US-F1.5.1** Quên và đặt lại mật khẩu (#121) ✅ mọi AC có TC
    - AC-F1.5.1-01 Yêu cầu quên mật khẩu cho tài khoản ACTIVE tạo token băm SHA-256 hạn 30 phút, gửi email và trả 200 câu chung (BR-F1-01, BR-F1-08)
      - TC-F1.5.1-01a `PasswordResetServiceTest#emailActiveTaoPasswordResetBamHan30PhutVaGuiEmail`
      - TC-F1.5.1-01b `AuthControllerTest#quenMatKhauCongKhaiTra200CauChung`
      - TC-F1.5.1-01c `features/auth/pages/ForgotPasswordPage.test.tsx › TC-F1.5.1-01c: nhập email ở /quen-mat-khau gửi email chuẩn h`
      - TC-F1.5.1-01d `features/auth/pages/LoginPage.test.tsx › TC-F1.5.1-01d: LoginPage có liên kết Quên mật khẩu? tới /que`
      - TC-F1.5.1-01e `features/auth/api/auth-api.test.ts › TC-F1.5.1-01e: forgotPassword và resetPassword gọi API công `
      - TC-F1.5.1-01f `PasswordResetServiceTest#phatHanhTokenTheoUserIdVaThoiHan72GioChoS236`
    - AC-F1.5.1-02 Không lộ email khi email không tồn tại, tài khoản không ACTIVE hoặc gửi email gặp lỗi (BR-F1-08)
      - TC-F1.5.1-02a `PasswordResetServiceTest#khongLoEmailKhiKhongTonTaiHoacKhongActive`
      - TC-F1.5.1-02b `PasswordResetServiceTest#guiEmailLoiVanKhongNemNgoaiLeVaKhongGhiTokenRaLog`
    - AC-F1.5.1-03 Đặt lại mật khẩu với token hợp lệ cập nhật mật khẩu, ghi used_at, mở khóa tạm và thu hồi toàn bộ refresh token (BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08)
      - TC-F1.5.1-03a `PasswordResetServiceTest#datLaiMatKhauDungTokenKhoaUserGhiUsedAtXoaKhoaTamVaThuHoiPhien`
      - TC-F1.5.1-03b `AuthControllerTest#datLaiMatKhauHopLeCongKhaiTra204`
      - TC-F1.5.1-03c `PasswordResetIntegrationTest#luongTronVenQuenVaDatLaiMatKhau`
      - TC-F1.5.1-03d `features/auth/pages/ResetPasswordPage.test.tsx › TC-F1.5.1-03d: Trang Đặt mật khẩu gửi token và mật khẩu mới `
      - TC-F1.5.1-03e `features/auth/pages/ResetPasswordPage.test.tsx › TC-F1.5.1-03e: /quen-mat-khau và /dat-lai-mat-khau là trang `
      - TC-F1.5.1-03f `PasswordResetIntegrationTest#datLaiMatKhauVoHieuHoaTatCaLienKetQuenMatKhauKhacCuaCungUser`
    - AC-F1.5.1-04 Từ chối token sai, hết hạn sau 30 phút, đã dùng (kể cả gửi đồng thời) hoặc tài khoản không ACTIVE với 422 (BR-F1-08)
      - TC-F1.5.1-04a `PasswordResetServiceTest#tokenSaiDaDungHoacHetHanTra422`
      - TC-F1.5.1-04b `AuthControllerTest#datLaiMatKhauTokenLoiTra422`
      - TC-F1.5.1-04c `PasswordResetIntegrationTest#tokenHetHanSau30PhutTra422VaEmailKhongTonTaiTra200`
      - TC-F1.5.1-04d `PasswordResetIntegrationTest#datLaiMatKhauDongThoiCungMotTokenChiMotLanThanhCong`
      - TC-F1.5.1-04e `features/auth/pages/ResetPasswordPage.test.tsx › TC-F1.5.1-04e: lỗi 422 hết hạn hoặc đã dùng hiển thị đúng câ`
    - AC-F1.5.1-05 Từ chối dữ liệu đầu vào sai định dạng, thiếu token trên URL hoặc mật khẩu mới không đạt chuẩn S1 (BR-F1-01, BR-F1-03)
      - TC-F1.5.1-05a `AuthControllerTest#quenMatKhauEmailSaiDinhDangTra400`
      - TC-F1.5.1-05b `AuthControllerTest#datLaiMatKhauSaiChuanS1Tra400`
      - TC-F1.5.1-05c `features/auth/pages/ForgotPasswordPage.test.tsx › TC-F1.5.1-05c: email sai định dạng ở trình duyệt không gọi A`
      - TC-F1.5.1-05d `features/auth/pages/ResetPasswordPage.test.tsx › TC-F1.5.1-05d: thiếu hoặc rỗng token trên URL báo lỗi ngay, `
      - TC-F1.5.1-05e `features/auth/pages/ResetPasswordPage.test.tsx › TC-F1.5.1-05e: kiểm tra chuẩn S1, hai mật khẩu không khớp kh`
    - AC-F1.5.1-06 Gửi email qua API HTTPS của Brevo khi có cấu hình khóa API và chỉ ghi log ở môi trường phát triển khi khóa rỗng (BR-F1-08)
      - TC-F1.5.1-06a `BrevoEmailSenderTest#guiEmailQuaBrevoBangRestClientKhongGoiMangThat`
      - TC-F1.5.1-06b `BrevoEmailSenderTest#khoaBrevoRongThiBeanLaLogEmailSender`
      - TC-F1.5.1-06c `BrevoEmailSenderTest#coKhoaBrevoThiBeanLaBrevoEmailSender`
- **UC-F1.6** Quản lý trạng thái tài khoản & Phân quyền truy cập API
  - **US-F1.6.1** Trạng thái tài khoản và phân quyền (#122) ✅ mọi AC có TC
    - AC-F1.6.1-01 Chưa đăng nhập hoặc token không hợp lệ bị từ chối 401 tiếng Việt (BR-F1-04, BR-F1-05)
      - TC-F1.6.1-01a `AuthServiceTest#getCurrentUserDocThongTinTuJwtVaNem401KhiClaimKhongHopLe`
      - TC-F1.6.1-01b `JwtSecurityIntegrationTest#chuaGuiTokenTra401ProblemDetailTiengViet`
      - TC-F1.6.1-01c `JwtSecurityIntegrationTest#tokenSaiChuKyTra401ProblemDetailTiengViet`
      - TC-F1.6.1-01d `JwtSecurityIntegrationTest#tokenHetHanTra401ProblemDetailTiengViet`
      - TC-F1.6.1-01e `JwtSecurityIntegrationTest#tokenThieuClaimRoleBiChanVoi401KhiGoiApi`
      - TC-F1.6.1-01f `JwtSecurityIntegrationTest#tokenCoRoleLaHoacSubKhongPhaiSoTra401ThayVi500`
      - TC-F1.6.1-01g `routes/ProtectedRoute.test.tsx › TC-F1.6.1-01g: chuyển về /login khi chưa đăng nhập`
    - AC-F1.6.1-02 Chặn API khi sai vai trò và cho phép khi đúng vai trò (BR-F1-05)
      - TC-F1.6.1-02a `AuthControllerTest#layThongTinTaiKhoanHienTaiUyQuyenChoAuthService`
      - TC-F1.6.1-02b `JwtSecurityIntegrationTest#tokenHopLeLayThongTinTaiKhoanHienTaiThanhCong`
      - TC-F1.6.1-02c `JwtSecurityIntegrationTest#goiApiKhiDuVaiTroTra200`
      - TC-F1.6.1-02d `JwtSecurityIntegrationTest#goiApiKhiKhongDuVaiTroTra403ProblemDetailTiengViet`
      - TC-F1.6.1-02e `JwtSecurityIntegrationTest#goiApiNhanVienBangVaiTroTrainerKhongDuQuyenTra403`
      - TC-F1.6.1-02f `routes/ProtectedRoute.test.tsx › TC-F1.6.1-02f: chuyển về /403 khi sai vai trò`
      - TC-F1.6.1-02g `routes/ProtectedRoute.test.tsx › TC-F1.6.1-02g: hiển thị nội dung khi đúng vai trò`
    - AC-F1.6.1-03 Nhân viên chi nhánh phải gắn với chi nhánh đang hoạt động (BR-F1-05)
      - TC-F1.6.1-03a `F1RegistrationIntegrationTest#realReceptionistJwtWithoutStaffAssignmentCannotCreateOrListMembers`
      - TC-F1.6.1-03b `StaffBranchIntegrationTest#staffBranchComesFromThatUsersAssignmentRatherThanDefaultBranch`
      - TC-F1.6.1-03c `StaffBranchIntegrationTest#userWithoutStaffProfileIsDeniedRatherThanAssignedToCn01`
      - TC-F1.6.1-03d `StaffBranchIntegrationTest#inactiveStaffIsDenied`
      - TC-F1.6.1-03e `StaffBranchIntegrationTest#activeStaffInInactiveBranchIsDenied`
      - TC-F1.6.1-03f `StaffBranchIntegrationTest#invalidOrUnknownUserIdsNeverReceiveDefaultBranch`
    - AC-F1.6.1-04 Tài khoản ngừng hoạt động mất quyền và bị thu hồi phiên ngay (BR-F1-04, BR-F1-05)
      - TC-F1.6.1-04a `AccountServiceTest#disableAccountCapNhatDisabledVaThuHoiRefreshToken`
      - TC-F1.6.1-04b `F1RegistrationIntegrationTest#disablingRevokesAllMemberSessionsAndReactivationDoesNotRestoreOldTokens`

### F2

- **UC-F2.1** Tạo hồ sơ hội viên tại quầy
  - **US-F2.1.1** Tạo hồ sơ hội viên tại quầy (#123) ✅ mọi AC có TC
    - AC-F2.1.1-01 Tạo hồ sơ mới thành công, chuẩn hóa SĐT và tự sinh mã HV (BR-F2-05, BR-F2-06, BR-F2-07)
      - TC-F2.1.1-01a `F1RegistrationIntegrationTest#realReceptionistJwtCreatesAndManagesCounterProfileInAssignedBranch`
      - TC-F2.1.1-01b `MemberApiIntegrationTest#createNormalizesAndReturnsLocationAndAudit`
      - TC-F2.1.1-01c `MemberApiIntegrationTest#createAndUpdateAcceptPastOrAbsentBirthDate`
      - TC-F2.1.1-01d `MemberApiIntegrationTest#branchManagerCanCreateReadUpdateAndSearchWithinTheirBranch`
      - TC-F2.1.1-01e `MemberServiceTest#createsWithStaffBranchAndNormalizedPhone`
    - AC-F2.1.1-02 Từ chối khi số điện thoại sai định dạng hoặc đã tồn tại (kể cả tạo đồng thời) (BR-F2-06)
      - TC-F2.1.1-02a `MemberApiIntegrationTest#createAndUpdateRejectTodayOrFutureBirthDateWithoutChanges`
      - TC-F2.1.1-02b `MemberApiIntegrationTest#invalidNamesAreRejectedByRequestValidationOnCreateAndUpdate`
      - TC-F2.1.1-02c `MemberApiIntegrationTest#invalidBodiesAndPaginationAre400AndDuplicateIs409`
      - TC-F2.1.1-02d `MemberApiIntegrationTest#incompletePhoneCannotBeSavedOnCreateOrUpdate`
      - TC-F2.1.1-02e `MemberApiIntegrationTest#concurrentRequestsCannotCreateDuplicatePhone`
      - TC-F2.1.1-02f `MemberServiceTest#duplicatePhoneDoesNotConsumeSequence`
      - TC-F2.1.1-02g `MemberServiceTest#concurrentDuplicatePhoneStillGetsFrontDeskGuidance`
      - TC-F2.1.1-02h `features/members/components/MemberFormModal.test.tsx › TC-F2.1.1-02h: hiển thị lỗi validation do backend trả về nga`
      - TC-F2.1.1-02i `features/members/components/MemberFormModal.test.tsx › TC-F2.1.1-02i: giữ form và dữ liệu đã nhập khi server từ chố`
    - AC-F2.1.1-03 Chỉ lễ tân, quản lý chi nhánh (đã gắn chi nhánh) và chủ chuỗi được tạo hồ sơ (BR-F2-10)
      - TC-F2.1.1-03a `F1RegistrationIntegrationTest#realReceptionistJwtWithoutStaffAssignmentCannotCreateOrListMembers`
      - TC-F2.1.1-03b `MemberApiIntegrationTest#validatesEndpointAndRecordPermissions`
- **UC-F2.2** Tạo tài khoản đăng nhập cho hồ sơ hội viên có sẵn
  - **US-F2.2.1** Tạo tài khoản cho hồ sơ có sẵn (#124) ✅ mọi AC có TC
    - AC-F2.2.1-01 Tạo tài khoản và gửi email đặt mật khẩu hạn 72 giờ (BR-F2-06, BR-F2-11)
      - TC-F2.2.1-01a `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-01a: tạo tài khoản thành công báo đã gửi email, tả`
      - TC-F2.2.1-01b `features/members/pages/MembersPage.test.tsx › TC-F2.2.1-01b: chỉ tạo tài khoản cho hồ sơ ACTIVE chưa có em`
      - TC-F2.2.1-01c `AccountServiceTest#createMemberAccountChuanHoaEmailVaBamMatKhauNgauNhien`
      - TC-F2.2.1-01d `MemberServiceTest#linkAccountAttachesNewUserToActiveUnlinkedMember`
      - TC-F2.2.1-01e `MemberAccountServiceTest#createAccountIssues72HourTokenAndSendsActivationEmail`
      - TC-F2.2.1-01f `MemberAccountIntegrationTest#createAccountPersistsUserLinkTokenAndSendsActivationEmail`
    - AC-F2.2.1-02 Hội viên đặt mật khẩu lần đầu rồi đăng nhập; quá 72 giờ liên kết hết hạn (BR-F2-11)
      - TC-F2.2.1-02a `MemberAccountIntegrationTest#resetPasswordWithin72HoursAndLoginSucceedsWhileExpiredReturns422`
    - AC-F2.2.1-03 Từ chối khi hồ sơ đã có tài khoản (BR-F2-11)
      - TC-F2.2.1-03a `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-03a, TC-F2.2.1-05a: hồ sơ đã có tài khoản hiện ema`
      - TC-F2.2.1-03b `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-03b, TC-F2.2.1-04a, TC-F2.2.1-05b, TC-F2.2.1-07c: `
      - TC-F2.2.1-03c `MemberServiceTest#linkAccountRejectsAlreadyLinkedMemberWith409`
      - TC-F2.2.1-03d `MemberAccountIntegrationTest#memberAlreadyHasAccountReturns409`
      - TC-F2.2.1-03e `MemberAccountIntegrationTest#concurrentCreateAccountForSameMemberCreatesSingleUser`
    - AC-F2.2.1-04 Từ chối khi email đã được sử dụng (kể cả đồng thời) (BR-F2-06, BR-F2-11)
      - TC-F2.2.1-04a `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-03b, TC-F2.2.1-04a, TC-F2.2.1-05b, TC-F2.2.1-07c: `
      - TC-F2.2.1-04b `AccountServiceTest#createMemberAccountTrungEmailTra409`
      - TC-F2.2.1-04c `MemberAccountIntegrationTest#emailAlreadyUsedByAnotherAccountReturns409`
      - TC-F2.2.1-04d `MemberAccountIntegrationTest#concurrentCreateAccountWithSameEmailRejectsSecondWith409`
    - AC-F2.2.1-05 Từ chối khi hồ sơ đang ngừng hoạt động (BR-F2-11)
      - TC-F2.2.1-05a `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-03a, TC-F2.2.1-05a: hồ sơ đã có tài khoản hiện ema`
      - TC-F2.2.1-05b `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-03b, TC-F2.2.1-04a, TC-F2.2.1-05b, TC-F2.2.1-07c: `
      - TC-F2.2.1-05c `MemberServiceTest#linkAccountRejectsInactiveMemberWith422`
      - TC-F2.2.1-05d `MemberAccountIntegrationTest#inactiveMemberReturns422`
    - AC-F2.2.1-06 Gửi email gặp lỗi vẫn giữ tài khoản và ghi log không chứa token (BR-F2-11)
      - TC-F2.2.1-06a `MemberAccountServiceTest#emailFailureDoesNotFailAccountCreationAndDoesNotLogToken`
      - TC-F2.2.1-06b `MemberAccountIntegrationTest#emailProviderFailureStillReturns200AndCommitsAccount`
    - AC-F2.2.1-07 Kiểm tra quyền theo vai trò, chi nhánh và dữ liệu đầu vào (BR-F2-10, BR-F2-11)
      - TC-F2.2.1-07b `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-07b: kiểm tra định dạng email ở trình duyệt trước `
      - TC-F2.2.1-07c `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-03b, TC-F2.2.1-04a, TC-F2.2.1-05b, TC-F2.2.1-07c: `
      - TC-F2.2.1-07d `MemberAccountIntegrationTest#enforcesRoleAndBranchPermissions`
      - TC-F2.2.1-07e `MemberAccountIntegrationTest#invalidEmailOrNonPositiveMemberIdReturns400`
- **UC-F2.3** Tra cứu, lọc và xem chi tiết hồ sơ hội viên
  - **US-F2.3.1** Tra cứu và xem hồ sơ hội viên (#125) ✅ mọi AC có TC
    - AC-F2.3.1-01 Tìm kiếm, lọc, phân trang trong chi nhánh mình đạt hiệu năng dưới 2 giây (BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10)
      - TC-F2.3.1-01a `F1RegistrationIntegrationTest#listGetsBatchEmailsThroughRealF1AndIncludesUnlinkedProfile`
      - TC-F2.3.1-01b `MemberApiIntegrationTest#searchScopesBothRowsAndCountAndIncludesUnlinkedProfiles`
      - TC-F2.3.1-01c `MemberApiIntegrationTest#memberCodeSearchIsExactAndIgnoresCase`
      - TC-F2.3.1-01d `MemberApiIntegrationTest#phonePrefixSearchRespectsBranchStatusSoftDeletionAndPagination`
      - TC-F2.3.1-01e `MemberApiIntegrationTest#shortNumericKeywordsWithoutMatchingPrefixReturnAnEmptyPage`
      - TC-F2.3.1-01f `MemberApiIntegrationTest#longInvalidNumericKeywordsReturnAnEmptyPage`
      - TC-F2.3.1-01g `MemberApiIntegrationTest#completePhoneSearchStillNormalizesAndMatchesExactly`
      - TC-F2.3.1-01h `MemberApiIntegrationTest#escapesNameWildcardsAndFiltersStatus`
      - TC-F2.3.1-01i `MemberApiIntegrationTest#measuresSearchOnTenThousandProfiles`
      - TC-F2.3.1-01j `features/members/pages/MembersPage.test.tsx › TC-F2.3.1-01j: đổi trang gọi API từ 0, tìm kiếm mới trở về t`
      - TC-F2.3.1-01k `features/members/pages/MembersPage.test.tsx › TC-F2.3.1-01k: áp dụng từ khóa đang nhập khi đổi trạng thái `
    - AC-F2.3.1-02 Chủ chuỗi tra cứu và xem chi tiết hồ sơ mọi chi nhánh (BR-F2-10)
      - TC-F2.3.1-02a `F1RegistrationIntegrationTest#listGetsBatchEmailsThroughRealF1AndIncludesUnlinkedProfile`
      - TC-F2.3.1-02b `MemberApiIntegrationTest#searchScopesBothRowsAndCountAndIncludesUnlinkedProfiles`
    - AC-F2.3.1-03 Xem chi tiết đúng phạm vi; ngoài phạm vi hoặc đã xóa mềm trả 404 (BR-F2-09, BR-F2-10)
      - TC-F2.3.1-03a `F1RegistrationIntegrationTest#realF1RegistrationCreatesProfileAndJwtCanReadOwnF2Profile`
      - TC-F2.3.1-03b `MemberApiIntegrationTest#validatesEndpointAndRecordPermissions`
      - TC-F2.3.1-03c `MemberServiceTest#memberCannotReadSomeoneElse`
    - AC-F2.3.1-04 Huấn luyện viên chỉ xem chi tiết học viên PT của mình và không được gọi danh sách (BR-F2-10)
      - TC-F2.3.1-04a `MemberApiIntegrationTest#validatesEndpointAndRecordPermissions`
- **UC-F2.4** Cập nhật hồ sơ, Ảnh đại diện & Ngừng hoạt động hội viên
  - **US-F2.4.1** Cập nhật và ngừng hoạt động hồ sơ (#126) ✅ mọi AC có TC
    - AC-F2.4.1-01 Cập nhật thông tin hồ sơ trong chi nhánh và chuẩn hóa số điện thoại (BR-F2-06, BR-F2-10)
      - TC-F2.4.1-01a `MemberApiIntegrationTest#createAndUpdateAcceptPastOrAbsentBirthDate`
      - TC-F2.4.1-01b `MemberApiIntegrationTest#branchManagerCanCreateReadUpdateAndSearchWithinTheirBranch`
      - TC-F2.4.1-01c `MemberApiIntegrationTest#updateSamePhoneAndDisableRevokeAndActivate`
      - TC-F2.4.1-01d `MemberServiceTest#editingProfileWithoutStatusTransitionDoesNotDisableOrActivateAccount`
      - TC-F2.4.1-01e `features/members/components/MemberFormModal.test.tsx › TC-F2.4.1-01e: sửa hồ sơ gửi đầy đủ trạng thái và giữ giá tr`
    - AC-F2.4.1-02 Từ chối trùng số điện thoại, thiếu trạng thái hoặc hồ sơ ngoài phạm vi (BR-F2-06, BR-F2-10)
      - TC-F2.4.1-02a `MemberApiIntegrationTest#createAndUpdateRejectTodayOrFutureBirthDateWithoutChanges`
      - TC-F2.4.1-02b `MemberApiIntegrationTest#invalidNamesAreRejectedByRequestValidationOnCreateAndUpdate`
      - TC-F2.4.1-02c `MemberApiIntegrationTest#missingUpdateStatusIsRejectedByRequestValidation`
      - TC-F2.4.1-02d `MemberApiIntegrationTest#incompletePhoneCannotBeSavedOnCreateOrUpdate`
      - TC-F2.4.1-02e `MemberApiIntegrationTest#duplicateUpdateIs409AndUnassignedStaffCannotAccess`
      - TC-F2.4.1-02f `MemberServiceTest#outsideBranchIsNotFoundAndDoesNotModifyAccount`
    - AC-F2.4.1-03 Ngừng hoạt động hồ sơ đồng thời khóa tài khoản và thu hồi mọi phiên trong cùng giao dịch (BR-F2-08, BR-F2-09)
      - TC-F2.4.1-03a `AccountServiceTest#disableAccountCapNhatDisabledVaThuHoiRefreshToken`
      - TC-F2.4.1-03b `F1RegistrationIntegrationTest#disablingRevokesAllMemberSessionsAndReactivationDoesNotRestoreOldTokens`
      - TC-F2.4.1-03c `F1RegistrationIntegrationTest#failureAfterRealF1DisableRollsBackMemberAccountAndTokenRevocation`
      - TC-F2.4.1-03d `F1RegistrationIntegrationTest#failureAfterSavingMemberRollsBackProfileAccountAndTokenRevocation`
      - TC-F2.4.1-03e `MemberApiIntegrationTest#updateSamePhoneAndDisableRevokeAndActivate`
      - TC-F2.4.1-03f `MemberApiIntegrationTest#failedAccountOperationRollsBackMemberUserAndTokens`
      - TC-F2.4.1-03g `MemberServiceTest#failedAccountTransitionDoesNotMutateNamePhoneOrStatus`
    - AC-F2.4.1-04 Kích hoạt lại hồ sơ khôi phục tài khoản nhưng không khôi phục token cũ và không gỡ khóa tạm (BR-F2-08)
      - TC-F2.4.1-04a `AccountServiceTest#activateAccountIfDisabledChiMoTaiKhoanDisabledKhongMoLocked`
      - TC-F2.4.1-04b `F1RegistrationIntegrationTest#disablingRevokesAllMemberSessionsAndReactivationDoesNotRestoreOldTokens`
      - TC-F2.4.1-04c `F1RegistrationIntegrationTest#activatingMemberDoesNotUnlockCurrentlyLockedAccount`
      - TC-F2.4.1-04d `F1RegistrationIntegrationTest#failureAfterRealF1ActivationRollsBackMemberAndAccount`
      - TC-F2.4.1-04e `MemberApiIntegrationTest#updateSamePhoneAndDisableRevokeAndActivate`
    - AC-F2.4.1-05 Cập nhật trạng thái đồng thời luôn giữ hồ sơ và tài khoản khớp nhau (BR-F2-08)
      - TC-F2.4.1-05a `MemberApiIntegrationTest#concurrentUpdatesKeepMemberAndAccountStatusesConsistent`
  - **US-F2.4.2** Ảnh đại diện hội viên (#127) ✅ mọi AC có TC
    - AC-F2.4.2-01 Tải và chuẩn hóa ảnh hợp lệ (BR-F2-01, BR-F2-02)
      - TC-F2.4.2-01a `MemberAvatarIntegrationTest#validImagesAreResizedAndStored`
      - TC-F2.4.2-01b `MemberAvatarIntegrationTest#smallTransparentImagesKeepDimensions`
      - TC-F2.4.2-01c `MemberAvatarIntegrationTest#acceptsDimensionBoundary`
      - TC-F2.4.2-01d `MemberAvatarIntegrationTest#acceptsUploadSizeBoundary`
      - TC-F2.4.2-01e `features/members/components/MemberFormModal.test.tsx › TC-F2.4.2-01e: chọn ảnh JPEG/PNG hợp lệ gọi API tải lên và t`
    - AC-F2.4.2-02 Thay ảnh trên cùng bản ghi (BR-F2-03)
      - TC-F2.4.2-02a `MemberAvatarIntegrationTest#overwritesSameRow`
      - TC-F2.4.2-02b `MemberAvatarIntegrationTest#concurrentInitialUploadsAreSerialized`
      - TC-F2.4.2-02c `MemberAvatarIntegrationTest#invalidFilesDoNotReplaceExistingAvatar`
    - AC-F2.4.2-03 Từ chối file không hợp lệ (BR-F2-01)
      - TC-F2.4.2-03a `MemberAvatarIntegrationTest#invalidFilesDoNotReplaceExistingAvatar`
      - TC-F2.4.2-03b `MemberAvatarIntegrationTest#servletRejectsOversizedMultipart`
      - TC-F2.4.2-03c `features/members/components/MemberFormModal.test.tsx › TC-F2.4.2-03c: báo lỗi sớm với file sai định dạng hoặc vượt `
      - TC-F2.4.2-03d `features/members/components/MemberFormModal.test.tsx › TC-F2.4.2-03d: lỗi 400 từ API hiển thị đúng câu errors.file `
    - AC-F2.4.2-04 Xem ảnh theo quyền hồ sơ (BR-F2-04)
      - TC-F2.4.2-04a `MemberAvatarIntegrationTest#authorizedReadersReceiveStoredJpeg`
      - TC-F2.4.2-04b `MemberAvatarIntegrationTest#otherReadersCannotSeeAvatar`
    - AC-F2.4.2-05 Chỉ nhân viên có quyền sửa mới đổi ảnh (BR-F2-04)
      - TC-F2.4.2-05a `MemberAvatarIntegrationTest#ownerMayUploadAcrossBranches`
      - TC-F2.4.2-05b `MemberAvatarIntegrationTest#deniedWritersDoNotMutateAvatar`
      - TC-F2.4.2-05c `MemberAvatarIntegrationTest#inactiveAssignmentsAreDenied`
      - TC-F2.4.2-05d `features/members/components/MemberFormModal.test.tsx › TC-F2.4.2-05d: hội viên không thấy nút đổi ảnh`
    - AC-F2.4.2-06 Chưa có ảnh hoặc không có hồ sơ (BR-F2-04)
      - TC-F2.4.2-06a `MemberAvatarIntegrationTest#missingAvatarIsNotFound`
      - TC-F2.4.2-06b `MemberAvatarIntegrationTest#missingOrDeletedProfilesAreNotFound`
    - AC-F2.4.2-07 Xác thực và kiểm tra request ảnh
      - TC-F2.4.2-07a `MemberAvatarIntegrationTest#unauthenticatedRequestsAreDenied`
      - TC-F2.4.2-07b `MemberAvatarIntegrationTest#invalidIdsAreRejected`
      - TC-F2.4.2-07c `MemberAvatarIntegrationTest#missingMultipartFileIsRejected`
    - AC-F2.4.2-08 Chưa có ảnh hoặc tải ảnh lỗi thì hiện chữ cái đầu của tên trên giao diện (BR-F2-04)
      - TC-F2.4.2-08a `MemberAvatarIntegrationTest#newProfileHasNoAvatar`
      - TC-F2.4.2-08a `features/members/components/MemberAvatar.test.tsx › TC-F2.4.2-08a: hiện chữ cái đầu của tên khi hasAvatar là fal`
      - TC-F2.4.2-08b `MemberAvatarIntegrationTest#profileResponsesReflectAvatarPresence`
      - TC-F2.4.2-08b `features/members/components/MemberFormModal.test.tsx › TC-F2.4.2-08b: mở hồ sơ chưa có ảnh thì hiện chữ cái đầu và `
- **UC-F2.5** Ghi nhận và xem lịch sử chỉ số cơ thể (`body_metrics`)
  - **US-F2.5.1** Chỉ số cơ thể (#128)
    - AC-F2.5.1-01 Ghi chỉ số cơ thể thành công với ít nhất một chỉ số (BR-F2-12) ❌ chưa có TC
    - AC-F2.5.1-02 Từ chối dữ liệu chỉ số cơ thể không hợp lệ (BR-F2-12) ❌ chưa có TC
    - AC-F2.5.1-03 Xem lịch sử chỉ số cơ thể sắp xếp mới nhất trước (BR-F2-12) ❌ chưa có TC
    - AC-F2.5.1-04 Phân quyền ghi và xem chỉ số cơ thể theo vai trò, chi nhánh và quan hệ PT (BR-F2-10, BR-F2-12) ❌ chưa có TC

### F3

- **UC-F3.1** Xem danh mục gói tập
  - **US-F3.1.1** Xem danh mục gói (nội bộ) (#129) ✅ mọi AC có TC
    - AC-F3.1.1-01 Nhân viên chi nhánh chỉ thấy gói của chi nhánh mình và gói toàn chuỗi (BR-F3-01)
      - TC-F3.1.1-01a `PackageCatalogIntegrationTest#scopesRowsAndCountAndIgnoresClientBranch`
    - AC-F3.1.1-02 Lọc, sắp xếp và phân trang
      - TC-F3.1.1-02a `PackageCatalogIntegrationTest#defaultActiveAndOrderingMatchContract`
      - TC-F3.1.1-02b `PackageCatalogIntegrationTest#inactiveFilterAndEmptyResults`
      - TC-F3.1.1-02c `PackageCatalogIntegrationTest#maximumSizeAndPagePastEndAreValid`
      - TC-F3.1.1-02d `PackageControllerTest#defaultContract`
      - TC-F3.1.1-02e `features/packages/pages/PackagesPage.test.tsx › TC-F3.1.1-02e: mặc định lấy gói đang bán; chọn loại PT giữ n`
      - TC-F3.1.1-02f `features/packages/api/packages.test.ts › TC-F3.1.1-02f: gửi bộ lọc và phân trang đúng endpoint đã đặc`
    - AC-F3.1.1-03 Lễ tân chỉ xem, không có thao tác quản lý trên giao diện
      - TC-F3.1.1-03a `features/packages/pages/PackagesPage.test.tsx › TC-F3.1.1-03a: Chỉ quản lý chi nhánh thấy nút thêm, sửa, ngừ`
    - AC-F3.1.1-04 Chủ chuỗi xem gói mọi chi nhánh
      - TC-F3.1.1-04a `PackageCatalogIntegrationTest#ownerSeesOtherBranchesWithoutStaff`
      - TC-F3.1.1-04b `PackageCatalogServiceTest#ownerDoesNotRequireStaffAssignment`
    - AC-F3.1.1-05 Từ chối người không được xem
      - TC-F3.1.1-05a `PackageCatalogIntegrationTest#inactiveOrMissingStaffIsForbidden`
      - TC-F3.1.1-05b `PackageControllerTest#rejectsDeniedRole`
      - TC-F3.1.1-05c `PackageControllerTest#missingAndInvalidBearerAreUnauthorized`
      - TC-F3.1.1-05d `PackageCatalogServiceTest#missingAssignmentNeverFallsBackToAllBranches`
    - AC-F3.1.1-06 Tham số lọc sai
      - TC-F3.1.1-06a `PackageControllerTest#rejectsInvalidQueryWithFieldErrors`
  - **US-F3.1.2** Bảng giá công khai (#130) ✅ mọi AC có TC
    - AC-F3.1.2-01 Khách xem bảng giá không cần đăng nhập (BR-F3-01)
      - TC-F3.1.2-01a `PublicPackageIntegrationTest#guestSeesActiveBranchAndGlobalPackagesInDisplayOrder`
      - TC-F3.1.2-01b `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-01b: hiện giá từ server, thời hạn hoặc số buổi, qu`
      - TC-F3.1.2-01c `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-01c: gọi API công khai không kèm access token và k`
      - TC-F3.1.2-01d `PublicPackageIntegrationTest#staleOrForgedTokenIsIgnoredOnPublicRead`
    - AC-F3.1.2-02 Không hiện gói ngừng bán và gói chi nhánh khác (BR-F3-01)
      - TC-F3.1.2-02a `PublicPackageIntegrationTest#hidesInactiveAndOtherBranchPackages`
    - AC-F3.1.2-03 Không lộ trường nội bộ
      - TC-F3.1.2-03a `PublicPackageIntegrationTest#exposesOnlyDisplayFields`
    - AC-F3.1.2-04 Chưa có gói đang bán
      - TC-F3.1.2-04a `PublicPackageIntegrationTest#emptyCatalogReturnsEmptyList`
      - TC-F3.1.2-04b `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-04b: chưa có gói đang bán thì báo trống, không hiệ`
    - AC-F3.1.2-05 Lỗi tải bảng giá
      - TC-F3.1.2-05a `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-05a: lỗi tải thì báo lỗi, không hiện giá; bấm Thử `
    - AC-F3.1.2-06 Chi nhánh mặc định ngừng hoạt động
      - TC-F3.1.2-06a `PublicPackageIntegrationTest#inactiveDefaultBranchReturnsEmptyList`
- **UC-F3.2** Thêm gói tập mới
  - **US-F3.2.1** Thêm gói tập (#131) ✅ mọi AC có TC
    - AC-F3.2.1-01 Thêm gói hợp lệ của từng loại (BR-F3-02, BR-F3-03)
      - TC-F3.2.1-01a `PackageCreateIntegrationTest#createsAllTypesWithServerBranchAuditAndReadVisibility`
      - TC-F3.2.1-01b `features/packages/components/PackageFormModal.test.tsx › TC-F3.2.1-01b: Thêm gói TIME hợp lệ gửi đúng DTO không chứa `
      - TC-F3.2.1-01c `features/packages/pages/PackagesPage.test.tsx › TC-F3.2.1-01c: Tạo gói mới từ trang danh mục đóng hộp thoại `
      - TC-F3.2.1-01d `features/packages/components/PackageFormModal.test.tsx › TC-F3.2.1-01d: Thêm gói SESSION hợp lệ gửi đúng DTO có số bu`
      - TC-F3.2.1-01e `features/packages/components/PackageFormModal.test.tsx › TC-F3.2.1-01e: Thêm gói PT hợp lệ gửi đúng DTO có số buổi và`
    - AC-F3.2.1-02 Số buổi đúng theo loại gói (BR-F3-02)
      - TC-F3.2.1-02a `PackageCreateIntegrationTest#sessionsAreRequiredAndPositive`
      - TC-F3.2.1-02b `features/packages/components/PackageFormModal.test.tsx › TC-F3.2.1-02b: Gói TIME ẩn ô số buổi và không gửi sessionCou`
    - AC-F3.2.1-03 Dữ liệu sai bị từ chối, giá trị biên được nhận
      - TC-F3.2.1-03a `PackageCreateIntegrationTest#invalidInputsReturnFieldErrorsWithoutWrites`
      - TC-F3.2.1-03b `PackageCreateIntegrationTest#missingFieldsMalformedJsonAndLengthLimitsAreRejected`
      - TC-F3.2.1-03c `PackageCreateIntegrationTest#acceptsZeroPriceAndMaximumLengthsAndIntegers`
      - TC-F3.2.1-03d `features/packages/components/PackageFormModal.test.tsx › TC-F3.2.1-03d: Kiểm tra đầu vào, cho phép giá 0 đồng và gán `
    - AC-F3.2.1-04 Không nhận trường do server quản lý
      - TC-F3.2.1-04a `PackageCreateIntegrationTest#rejectsServerManagedAndUnknownFields`
    - AC-F3.2.1-05 Mã gói không trùng
      - TC-F3.2.1-05a `PackageCreateIntegrationTest#duplicateInactiveGlobalCodeIsConflict`
      - TC-F3.2.1-05b `PackageCreateIntegrationTest#concurrentDuplicateCodeCreatesOneAndRollsBackOther`
      - TC-F3.2.1-05c `features/packages/components/PackageFormModal.test.tsx › TC-F3.2.1-05c: Lỗi 409 trùng mã gói hiển thị thông báo tiếng`
    - AC-F3.2.1-06 Chỉ Quản lý chi nhánh được thêm gói
      - TC-F3.2.1-06a `PackageCreateIntegrationTest#onlyManagerCanCreate`
      - TC-F3.2.1-06b `PackageCreateIntegrationTest#missingTokenOrInactiveStaffCannotCreate`
      - TC-F3.2.1-06d `features/packages/components/PackageFormModal.test.tsx › TC-F3.2.1-06d: Lỗi 403 khi tạo gói hiển thị thông báo tiếng `
- **UC-F3.3** Sửa thông tin gói tập
  - **US-F3.3.1** Sửa thông tin gói tập (#132) ✅ mọi AC có TC
    - AC-F3.3.1-01 Sửa giá; gói đã bán không đổi (BR-F3-06)
      - TC-F3.3.1-01a `PackageUpdateIntegrationTest#priceOnlyKeepsOmittedFieldsAndAudit`
      - TC-F3.3.1-01b `PackageUpdateIntegrationTest#historyIncludingPendingPurchaseRemainsIdentical`
      - TC-F3.3.1-01c `features/packages/components/PackageFormModal.test.tsx › TC-F3.3.1-01c, TC-F3.3.1-04c: Sửa gói giữ nguyên và không gử`
    - AC-F3.3.1-02 Sửa gói chi nhánh khác hoặc gói toàn chuỗi bị từ chối 403 (BR-F3-07)
      - TC-F3.3.1-02a `PackageUpdateIntegrationTest#outsideBranchAndGlobalAre403WithoutMutation`
    - AC-F3.3.1-03 Xóa mô tả và gửi body rỗng
      - TC-F3.3.1-03a `PackageUpdateIntegrationTest#explicitNullClearsDescriptionAndEmptyBodyDoesNotChangeAudit`
      - TC-F3.3.1-03b `features/packages/components/PackageFormModal.test.tsx › TC-F3.3.1-03b: Xóa trống mô tả khi sửa gửi description là nu`
    - AC-F3.3.1-04 Không đổi mã, loại và không xóa trường bắt buộc (BR-F3-05, BR-F3-02)
      - TC-F3.3.1-04a `PackageUpdateIntegrationTest#refusesImmutableAndUnknownFields`
      - TC-F3.3.1-04b `PackageUpdateIntegrationTest#sessionsMergeWithExistingTypeAndCannotBeCleared`
      - TC-F3.3.1-04c `features/packages/components/PackageFormModal.test.tsx › TC-F3.3.1-01c, TC-F3.3.1-04c: Sửa gói giữ nguyên và không gử`
    - AC-F3.3.1-05 Dữ liệu sai thì không đổi gì
      - TC-F3.3.1-05a `PackageUpdateIntegrationTest#invalidUpdateIsAtomicAndReturnsFieldError`
      - TC-F3.3.1-05b `PackageUpdateIntegrationTest#invalidIdIsBadRequest`
      - TC-F3.3.1-05c `PackageUpdateIntegrationTest#malformedBodyIsRejected`
      - TC-F3.3.1-05d `features/packages/components/PackageFormModal.test.tsx › TC-F3.3.1-05d, TC-F3.3.1-07b: Lỗi 400 theo trường hoặc 404 k`
    - AC-F3.3.1-06 Sửa gói đã ngừng bán không làm gói bán lại
      - TC-F3.3.1-06a `PackageUpdateIntegrationTest#acceptsBoundariesAndCanEditInactiveWithoutReactivation`
      - TC-F3.3.1-06b `features/packages/pages/PackagesPage.test.tsx › TC-F3.3.1-06b: Gói ngừng bán vẫn hiện nút Sửa cho quản lý ch`
    - AC-F3.3.1-07 Gói không tồn tại
      - TC-F3.3.1-07a `PackageUpdateIntegrationTest#missingPackageIs404WithoutMutation`
      - TC-F3.3.1-07b `features/packages/components/PackageFormModal.test.tsx › TC-F3.3.1-05d, TC-F3.3.1-07b: Lỗi 400 theo trường hoặc 404 k`
    - AC-F3.3.1-08 Chỉ Quản lý chi nhánh được sửa
      - TC-F3.3.1-08a `PackageUpdateIntegrationTest#deniedRolesCannotUpdate`
      - TC-F3.3.1-08b `PackageUpdateIntegrationTest#noTokenOrInactiveMissingStaffCannotUpdate`
- **UC-F3.4** Ngừng bán gói tập
  - **US-F3.4.1** Ngừng bán gói tập (#133) ✅ mọi AC có TC
    - AC-F3.4.1-01 Ngừng bán gói đang bán (BR-F3-08)
      - TC-F3.4.1-01a `PackageDeactivateIntegrationTest#deactivatesWithNoBodyOrEmptyJsonWithoutChangingOtherFields`
      - TC-F3.4.1-01b `features/packages/pages/PackagesPage.test.tsx › TC-F3.4.1-01b, TC-F3.4.1-03b: Xác nhận ngừng bán gói ACTIVE `
    - AC-F3.4.1-02 Gói đã bán vẫn dùng được (BR-F3-08)
      - TC-F3.4.1-02a `PackageDeactivateIntegrationTest#preservesAllHistoricalRows`
    - AC-F3.4.1-03 Gói ngừng bán không bán mới được nhưng vẫn tra cứu được (BR-F3-09)
      - TC-F3.4.1-03a `PackageCatalogIntegrationTest#inactiveFilterAndEmptyResults`
      - TC-F3.4.1-03b `features/packages/pages/PackagesPage.test.tsx › TC-F3.4.1-01b, TC-F3.4.1-03b: Xác nhận ngừng bán gói ACTIVE `
    - AC-F3.4.1-04 Ngừng bán lần nữa bị từ chối
      - TC-F3.4.1-04a `PackageDeactivateIntegrationTest#repeatIs422WithoutFurtherAuditChange`
      - TC-F3.4.1-04b `PackageDeactivateIntegrationTest#simultaneousDeactivationHasOneSuccessAndOne422`
      - TC-F3.4.1-04c `features/packages/pages/PackagesPage.test.tsx › TC-F3.4.1-04c, TC-F3.4.1-06h: Lỗi 422, 403, 404 khi ngừng bá`
    - AC-F3.4.1-05 Sửa cùng lúc không làm gói bán lại
      - TC-F3.4.1-05a `PackageDeactivateIntegrationTest#concurrentEditCannotReactivatePackage`
    - AC-F3.4.1-06 Từ chối gói ngoài chi nhánh, gói không tồn tại và vai trò khác (BR-F3-07)
      - TC-F3.4.1-06a `PackageDeactivateIntegrationTest#outsideBranchAndGlobalAre403EvenIfAlreadyInactive`
      - TC-F3.4.1-06b `PackageDeactivateIntegrationTest#deniedRolesCannotDeactivate`
      - TC-F3.4.1-06c `PackageDeactivateIntegrationTest#invalidIdIs400`
      - TC-F3.4.1-06d `PackageDeactivateIntegrationTest#missingAndBadTokenAre401`
      - TC-F3.4.1-06e `PackageDeactivateIntegrationTest#inactiveAndMissingStaffAre403`
      - TC-F3.4.1-06f `PackageDeactivateIntegrationTest#missingPackageIs404WithEmptyJsonBodyWithoutMutation`
      - TC-F3.4.1-06h `features/packages/pages/PackagesPage.test.tsx › TC-F3.4.1-04c, TC-F3.4.1-06h: Lỗi 422, 403, 404 khi ngừng bá`
- **UC-F3.5** Bán gói tập tại quầy (đăng ký mới / gia hạn)
  - **US-F3.5.1** Bán và gia hạn gói tại quầy (#134) ✅ mọi AC có TC
    - AC-F3.5.1-01 Bán gói tạo hóa đơn chờ và gói chờ, giá lấy ở server (BR-F3-04, BR-F3-13)
      - TC-F3.5.1-01a `InvoiceSaleIntegrationTest#sellCreatesPendingInvoiceAndMembershipWithServerPrice`
      - TC-F3.5.1-01b `InvoiceSaleIntegrationTest#sessionPackageCopiesSessionCount`
      - TC-F3.5.1-01c `InvoiceSaleIntegrationTest#membershipFailureRollsBackEntireSale`
      - TC-F3.5.1-01d `features/packages/api/package-sales.test.ts › TC-F3.5.1-01d: livePackageSalesApi gọi đúng GET /memberships`
      - TC-F3.5.1-01e `features/packages/pages/SellPackagePage.test.tsx › TC-F3.5.1-01e: Chọn hội viên và gói, chỉ gửi memberId và ite`
    - AC-F3.5.1-02 Gói chỉ hiệu lực sau khi hóa đơn PAID; ngày hết hạn tính đúng (BR-F3-12)
      - TC-F3.5.1-02a `MembershipListIntegrationTest#pendingSaleIsNotUsableBeforePayment`
      - TC-F3.5.1-02b `InvoicePaymentIntegrationTest#payActivatesWithDatesRecomputedAtPaymentTime`
    - AC-F3.5.1-03 Gia hạn khi gói cũ còn hiệu lực (BR-F3-11)
      - TC-F3.5.1-03a `InvoiceSaleIntegrationTest#renewalStartsDayAfterOldTimePackage`
      - TC-F3.5.1-03b `MembershipServiceTest#newOrTimeRenewal`
      - TC-F3.5.1-03c `InvoicePaymentIntegrationTest#payRenewalAfterActiveTimePackageIsUpcoming`
      - TC-F3.5.1-03c `MembershipServiceTest#ptRenewalWithSessionsLeft`
      - TC-F3.5.1-03d `features/packages/pages/SellPackagePage.test.tsx › TC-F3.5.1-03d: Hội viên có gói TIME ACTIVE cùng phạm vi thì `
    - AC-F3.5.1-04 Gia hạn khi gói SESSION cũ đã hết buổi (BR-F3-11)
      - TC-F3.5.1-04a `InvoiceSaleIntegrationTest#renewalAfterEmptySessionPackageStartsTomorrow`
      - TC-F3.5.1-04b `InvoiceSaleIntegrationTest#renewalAfterSessionPackageWithSessionsLeftStartsAfterEndDate`
      - TC-F3.5.1-04c `MembershipServiceTest#sessionRenewal`
      - TC-F3.5.1-04d `InvoicePaymentIntegrationTest#payRenewalAfterEmptySessionPackageStartsTomorrow`
      - TC-F3.5.1-04d `MembershipServiceTest#emptyPtDoesNotUseSessionTomorrowRule`
      - TC-F3.5.1-04e `features/packages/pages/SellPackagePage.test.tsx › TC-F3.5.1-04e: Hội viên có gói SESSION đã hết buổi hiển thị `
    - AC-F3.5.1-05 Giữ đồng thời nhiều loại gói (BR-F3-10)
      - TC-F3.5.1-05a `InvoiceSaleIntegrationTest#differentPackageTypesKeepExistingMembershipIntact`
      - TC-F3.5.1-05b `InvoicePaymentIntegrationTest#payPtWhileHoldingTimeActivatesPt`
    - AC-F3.5.1-06 Chỉ một gói chờ cho mỗi loại (BR-F3-11)
      - TC-F3.5.1-06a `InvoiceSaleIntegrationTest#secondWaitingPackageOfSameTypeIsRejected`
      - TC-F3.5.1-06b `InvoiceSaleIntegrationTest#pendingWithoutActiveAlsoBlocks`
      - TC-F3.5.1-06c `InvoiceSaleIntegrationTest#waitingPackageOfOtherTypeDoesNotBlock`
    - AC-F3.5.1-07 Hai lễ tân bán cùng lúc cho một hội viên
      - TC-F3.5.1-07a `InvoiceSaleConcurrencyIntegrationTest#twoReceptionistsSellingAtOnceCreateExactlyOne`
    - AC-F3.5.1-08 Hội viên hoặc gói không hợp lệ (BR-F3-09)
      - TC-F3.5.1-08a `InvoiceSaleIntegrationTest#inactiveMemberIsRejected`
      - TC-F3.5.1-08b `InvoiceSaleIntegrationTest#inactivePackageIsRejected`
      - TC-F3.5.1-08c `InvoiceSaleIntegrationTest#otherBranchPackageIsRejected`
      - TC-F3.5.1-08d `InvoiceSaleIntegrationTest#unknownMemberOrPackageIsNotFound`
      - TC-F3.5.1-08e `InvoiceSaleIntegrationTest#memberOfOtherBranchIsHidden`
      - TC-F3.5.1-08f `InvoiceSaleConcurrencyIntegrationTest#saleWaitsForCatalogChangeAndReadsCommittedValues`
      - TC-F3.5.1-08g `features/packages/pages/SellPackagePage.test.tsx › TC-F3.5.1-08g: Lỗi 422 hoặc 404 khi hội viên ngừng hoạt động`
    - AC-F3.5.1-09 Hóa đơn tại quầy không tự hủy (BR-F3-14)
      - TC-F3.5.1-09a `PendingInvoiceExpiryIntegrationTest#counterInvoiceIsNeverAutoCancelled`
    - AC-F3.5.1-10 Tự kích hoạt gói nối tiếp đúng ngày (BR-F3-11)
      - TC-F3.5.1-10a `MembershipLifecycleIntegrationTest#expiresOldBeforeActivatingNext`
      - TC-F3.5.1-10b `MembershipLifecycleIntegrationTest#lastDeductionTodayRetainsAccess`
      - TC-F3.5.1-10c `MembershipLifecycleIntegrationTest#zeroSessionsFromYesterdayExpire`
      - TC-F3.5.1-10d `MembershipLifecycleIntegrationTest#repeatedRunDoesNotMutateAgain`
      - TC-F3.5.1-10e `MembershipLifecycleIntegrationTest#catchesUpAfterSeveralMissedDays`
      - TC-F3.5.1-10f `MembershipLifecycleIntegrationTest#endDateIsInclusive`
      - TC-F3.5.1-10g `MembershipLifecycleIntegrationTest#ignoresOtherStates`
      - TC-F3.5.1-10h `MembershipLifecycleIntegrationTest#frozenBlocksWithoutChangingWaitingPackage`
      - TC-F3.5.1-10i `MembershipLifecycleIntegrationTest#chainAndBranchAreIndependentGroups`
      - TC-F3.5.1-10j `MembershipLifecycleIntegrationTest#expiredPtKeepsRemainingSessions`
      - TC-F3.5.1-10k `MembershipLifecycleIntegrationTest#activationFailureRollsBackExpiration`
      - TC-F3.5.1-10l `MembershipLifecycleJobTest#annotations`
      - TC-F3.5.1-10m `MembershipLifecycleJobTest#passesVietnamDate`
      - TC-F3.5.1-10n `MembershipLifecycleJobTest#catchUpGoesThroughScheduler`
      - TC-F3.5.1-10o `MembershipLifecycleJobTest#retriesEveryFiveMinutesUntilSuccess`
      - TC-F3.5.1-10p `MembershipLifecycleJobTest#stopsRetryingOnNewDay`
    - AC-F3.5.1-11 Dữ liệu sai và vai trò khác bị từ chối
      - TC-F3.5.1-11a `InvoiceSaleIntegrationTest#invalidBodyIsBadRequest`
      - TC-F3.5.1-11b `InvoiceSaleIntegrationTest#otherRolesAreForbidden`
      - TC-F3.5.1-11c `InvoiceSaleIntegrationTest#missingTokenIsUnauthorized`
      - TC-F3.5.1-11d `InvoiceSaleIntegrationTest#missingFieldsAreNamed`
    - AC-F3.5.1-12 Xem gói đang giữ và ngày gia hạn dự kiến (BR-F3-11)
      - TC-F3.5.1-12a `MembershipListIntegrationTest#listsHeldMembershipsWithRenewalStartDate`
      - TC-F3.5.1-12b `MembershipListIntegrationTest#renewalStartDateMatchesSale`
      - TC-F3.5.1-12c `MembershipListIntegrationTest#scopeAndRoles`
      - TC-F3.5.1-12d `MembershipListIntegrationTest#invalidMemberIdIsRejected`
      - TC-F3.5.1-12e `MembershipServiceTest#neverBeforeToday`
    - AC-F3.5.1-13 Mã hóa đơn tự sinh và duy nhất
      - TC-F3.5.1-13a `InvoiceSaleIntegrationTest#invoiceCodesAreSequential`
      - TC-F3.5.1-13b `InvoiceCodeMigrationIntegrationTest#upgradesExistingDatabaseWithoutChangingInvoiceHistory`

### F4

- **UC-F4.1** Tra cứu hội viên và đối chiếu thông tin/ảnh tại quầy
  - **US-F4.1.1** Tra cứu và đối chiếu ảnh tại quầy (#135)
    - AC-F4.1.1-01 Tìm theo mã hội viên hoặc số điện thoại ❌ chưa có TC
    - AC-F4.1.1-02 Hiển thị ảnh đại diện và thông tin lớp học sắp diễn ra ❌ chưa có TC
    - AC-F4.1.1-03 Từ khóa tìm kiếm sai định dạng ❌ chưa có TC
    - AC-F4.1.1-04 Không tìm thấy hội viên hoặc hội viên thuộc chi nhánh khác ❌ chưa có TC
    - AC-F4.1.1-05 Hồ sơ hội viên ngừng hoạt động ❌ chưa có TC
    - AC-F4.1.1-06 Phân quyền truy cập chức năng tra cứu ❌ chưa có TC
- **UC-F4.2** Thực hiện check-in thủ công tại quầy
  - **US-F4.2.1** Check-in thủ công tại quầy (#136)
    - AC-F4.2.1-01 Check-in thành công với gói TIME ❌ chưa có TC
    - AC-F4.2.1-02 Xác định ngày check-in theo múi giờ Việt Nam ❌ chưa có TC
    - AC-F4.2.1-03 Gói SESSION trừ đúng một buổi mỗi ngày dương lịch ❌ chưa có TC
    - AC-F4.2.1-04 Ưu tiên gói TIME khi hội viên có cả TIME và SESSION ❌ chưa có TC
    - AC-F4.2.1-05 Buổi tập cuối của gói SESSION vẫn được tập không giới hạn hết ngày ❌ chưa có TC
    - AC-F4.2.1-06 Trừ buổi cuối kéo gói gia hạn UPCOMING về ngày mai ❌ chưa có TC
    - AC-F4.2.1-07 Check-in bằng gói PT đúng ngày có buổi tập hẹn trước ❌ chưa có TC
    - AC-F4.2.1-08 Ràng buộc biên của quy tắc check-in gói PT ❌ chưa có TC
    - AC-F4.2.1-09 Phạm vi hiệu lực của gói toàn chuỗi và gói riêng chi nhánh ❌ chưa có TC
    - AC-F4.2.1-10 Từ chối khi không có gói tập nào hợp lệ ❌ chưa có TC
    - AC-F4.2.1-11 Từ chối khi gói SESSION hết buổi và hôm nay chưa trừ buổi ❌ chưa có TC
    - AC-F4.2.1-12 Từ chối khi gói tập đang bảo lưu ❌ chưa có TC
    - AC-F4.2.1-13 Thứ tự ưu tiên thông báo lỗi khi có nhiều lý do vi phạm ❌ chưa có TC
    - AC-F4.2.1-14 Xử lý khi hội viên có nhiều gói SESSION cùng dùng được ❌ chưa có TC
    - AC-F4.2.1-15 Từ chối khi hồ sơ hội viên INACTIVE hoặc ngoài phạm vi ❌ chưa có TC
    - AC-F4.2.1-16 Kiểm tra giờ mở cửa phòng tập ❌ chưa có TC
    - AC-F4.2.1-17 Xử lý cạnh tranh khi hai lễ tân cùng check-in một hội viên ❌ chưa có TC
    - AC-F4.2.1-18 Kiểm tra tính hợp lệ của dữ liệu đầu vào và nhân viên thao tác ❌ chưa có TC
- **UC-F4.3** Tự động đánh dấu có mặt lớp học nhóm khi check-in
  - **US-F4.3.1** Tự đánh dấu đến lớp khi check-in (#137)
    - AC-F4.3.1-01 Ghi nhận thời điểm đến lớp trong khung giờ quy định ❌ chưa có TC
    - AC-F4.3.1-02 Không ghi nhận thời điểm đến khi ngoài khung giờ hoặc sai chi nhánh ❌ chưa có TC
    - AC-F4.3.1-03 Chỗ đặt lớp đã điểm danh hoặc kết thúc giữ nguyên trạng thái ❌ chưa có TC
    - AC-F4.3.1-04 Không ghi đè và không ghi nhận khi lượt check-in thất bại ❌ chưa có TC
- **UC-F4.4** Ghi nhận check-out cho hội viên
  - **US-F4.4.1** Check-out cho hội viên (#138)
    - AC-F4.4.1-01 Check-out thành công cho lượt tập đang diễn ra ❌ chưa có TC
    - AC-F4.4.1-02 Xử lý trường hợp hội viên có nhiều lượt check-in mở trong ngày ❌ chưa có TC
    - AC-F4.4.1-03 Từ chối khi không tìm thấy lượt tập đang mở hoặc khác chi nhánh ❌ chưa có TC
    - AC-F4.4.1-04 Xử lý xung đột đồng thời khi hai nhân viên cùng check-out một lượt ❌ chưa có TC
    - AC-F4.4.1-05 Phân quyền truy cập chức năng check-out ❌ chưa có TC
- **UC-F4.5** Xem lịch sử lượt check-in theo ngày tại quầy
  - **US-F4.5.1** Xem lịch sử check-in trong ngày (#139)
    - AC-F4.5.1-01 Mặc định hiển thị danh sách trong ngày hiện tại theo giờ Việt Nam ❌ chưa có TC
    - AC-F4.5.1-02 Đầy đủ thông tin chi tiết của từng lượt check-in ❌ chưa có TC
    - AC-F4.5.1-03 Lọc trạng thái, phương thức và tìm kiếm theo tên hoặc mã hội viên ❌ chưa có TC
    - AC-F4.5.1-04 Phân quyền và phạm vi dữ liệu theo chi nhánh ❌ chưa có TC

### F5

- **UC-F5.1** Sinh mã VietQR và xem chi tiết thanh toán hóa đơn
  - **US-F5.1.1** Mã VietQR cho hóa đơn (#140) ✅ mọi AC có TC
    - AC-F5.1.1-01 Sinh VietQR cho hóa đơn chờ bằng số tiền server (BR-F5-01, BR-F5-02)
      - TC-F5.1.1-01a `InvoiceDocumentIntegrationTest#vietQrForPendingInvoiceUsesServerTotalAndEncodesParams`
      - TC-F5.1.1-01b `features/invoices/api/invoices.test.ts › TC-F5.1.1-01b: vietqr gọi GET /invoices/{id}/vietqr`
      - TC-F5.1.1-01c `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.1.1-01c: chọn chuyển khoản gọi VietQR, hiện ảnh QR và `
      - TC-F5.1.1-01d `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.1.1-01d: ảnh QR lỗi vẫn giữ thông tin chuyển khoản và `
      - TC-F5.1.1-01e `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.1.1-01e: phản hồi QR cũ không ghi đè yêu cầu tải QR mớ`
      - TC-F5.1.1-01f `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.1.1-01f: thiếu Clipboard API không được báo sao chép t`
    - AC-F5.1.1-02 Chỉ sinh VietQR khi hóa đơn PENDING
      - TC-F5.1.1-02a `InvoiceDocumentIntegrationTest#vietQrForNonPendingInvoiceIs422`
    - AC-F5.1.1-03 Cấu hình ngân hàng ưu tiên theo chi nhánh
      - TC-F5.1.1-03a `InvoiceDocumentIntegrationTest#vietQrBranchBankConfigOverridesChainConfig`
      - TC-F5.1.1-03b `InvoiceDocumentIntegrationTest#vietQrFallsBackForEachMissingBranchKey`
    - AC-F5.1.1-04 Từ chối chưa đăng nhập và sai vai trò
      - TC-F5.1.1-04a `InvoiceDocumentIntegrationTest#vietQrSecurityAndScope`
    - AC-F5.1.1-05 Hóa đơn ngoài chi nhánh được giấu như không tồn tại
      - TC-F5.1.1-05a `InvoiceDocumentIntegrationTest#vietQrSecurityAndScope`
    - AC-F5.1.1-06 Thiếu cấu hình ngân hàng không sinh VietQR
      - TC-F5.1.1-06a `InvoiceDocumentIntegrationTest#vietQrMissingBankConfigIs422`
      - TC-F5.1.1-06b `InvoiceDocumentIntegrationTest#blankBranchBankConfigIs422`
      - TC-F5.1.1-06c `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.1.1-06c: lỗi 422 khi tải VietQR hiện đúng câu detail c`
    - AC-F5.1.1-07 ID VietQR không hợp lệ
      - TC-F5.1.1-07a `InvoiceDocumentIntegrationTest#walkInPaidInvoicePdfAndInvalidId`
      - TC-F5.1.1-07b `InvoiceDocumentIntegrationTest#invalidDocumentIdsReturnFieldErrors`
    - AC-F5.1.1-08 Không sinh VietQR cho hóa đơn cổng hội viên quá hạn (BR-F5-07)
      - TC-F5.1.1-08a `InvoiceExpiryIntegrationTest#paymentAndVietQrRespectPortalDeadline`
      - TC-F5.1.1-08b `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.1.1-08b: hóa đơn portal quá hạn hiện lỗi VietQR và khô`
- **UC-F5.2** Ghi nhận thanh toán tại quầy và kích hoạt dịch vụ
  - **US-F5.2.1** Ghi nhận thanh toán, kích hoạt gói (#141) ✅ mọi AC có TC
    - AC-F5.2.1-01 Thu tiền mặt kích hoạt gói mới đúng ngày và giữ thời hạn đã chốt (BR-F5-03, BR-F5-04, BR-F3-06)
      - TC-F5.2.1-01a `InvoicePaymentIntegrationTest#payActivatesWithDatesRecomputedAtPaymentTime`
      - TC-F5.2.1-01b `InvoicePaymentIntegrationTest#payPtWhileHoldingTimeActivatesPt`
      - TC-F5.2.1-01c `InvoicePaymentIntegrationTest#paysPortalInvoiceAtCounter`
      - TC-F5.2.1-01d `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-01d: xác nhận thu tiền mặt gửi method CASH và khôn`
      - TC-F5.2.1-01e `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-01e: hiển thị ACTIVE từ server sau thu tiền`
      - TC-F5.2.1-01f `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-01f: hóa đơn nguồn %s vẫn thu được tại quầy`
    - AC-F5.2.1-02 Thu chuyển khoản cho gia hạn nối tiếp (BR-F5-04, BR-F3-11)
      - TC-F5.2.1-02a `InvoicePaymentIntegrationTest#payRenewalAfterActiveTimePackageIsUpcoming`
      - TC-F5.2.1-02b `InvoicePaymentIntegrationTest#payRenewalAfterFrozenPackageIsUpcoming`
      - TC-F5.2.1-02c `InvoicePaymentIntegrationTest#payRenewalAfterEmptySessionPackageStartsTomorrow`
      - TC-F5.2.1-02d `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-02d: hiển thị trạng thái UPCOMING và ngày hiệu lực`
    - AC-F5.2.1-03 Hóa đơn 0 đồng vẫn ghi phiếu thu và kích hoạt gói (BR-F5-04)
      - TC-F5.2.1-03a `InvoicePaymentIntegrationTest#zeroTotalInvoiceActivatesWithZeroPaymentRow`
      - TC-F5.2.1-03b `PaymentAmountMigrationIntegrationTest#upgradesExistingPaymentsWithoutChangingHistory`
      - TC-F5.2.1-03c `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-03c: cho phép xác nhận hóa đơn 0 đồng và hiển thị `
    - AC-F5.2.1-04 Chỉ Lễ tân và Quản lý chi nhánh được xác nhận thu (BR-F5-03)
      - TC-F5.2.1-04a `InvoicePaymentIntegrationTest#branchManagerCanPay`
      - TC-F5.2.1-04b `InvoicePaymentIntegrationTest#otherRolesAreForbidden`
      - TC-F5.2.1-04c `InvoicePaymentIntegrationTest#noTokenIs401`
      - TC-F5.2.1-04d `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-04d: %s không thấy chức năng thu tiền hoặc VietQR`
    - AC-F5.2.1-05 Dữ liệu thu tiền sai không làm thay đổi hóa đơn
      - TC-F5.2.1-05a `InvoicePaymentIntegrationTest#invalidMethodIs400`
      - TC-F5.2.1-05b `InvoicePaymentIntegrationTest#tooLongTransactionRefIs400`
      - TC-F5.2.1-05c `InvoicePaymentIntegrationTest#invalidPaymentIdIs400`
      - TC-F5.2.1-05d `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-05d: lỗi transactionRef 400 được hiển thị ngay dướ`
    - AC-F5.2.1-06 Thu tiền hóa đơn không tồn tại hoặc ngoài chi nhánh
      - TC-F5.2.1-06a `InvoicePaymentIntegrationTest#unknownInvoiceIs404`
      - TC-F5.2.1-06b `InvoicePaymentIntegrationTest#otherBranchIs404`
    - AC-F5.2.1-07 Hai nhân viên cùng thu một hóa đơn (BR-F5-05)
      - TC-F5.2.1-07a `InvoicePaymentConcurrencyIntegrationTest#twoReceptionistsPayingSameInvoice`
      - TC-F5.2.1-07b `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-07b: lỗi 409 hiện đúng câu detail của API và tải l`
      - TC-F5.2.1-07c `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-07c: bấm xác nhận lặp khi đang gửi chỉ gọi pay một`
    - AC-F5.2.1-08 Không thu lại hóa đơn đã xử lý
      - TC-F5.2.1-08a `InvoicePaymentIntegrationTest#payingTwiceIs422`
      - TC-F5.2.1-08b `InvoicePaymentIntegrationTest#payingCancelledInvoiceIs422`
      - TC-F5.2.1-08c `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-08c: lỗi 422 hiện đúng câu detail của API`
    - AC-F5.2.1-09 Kiểm tra lại gói UPCOMING ở thời điểm thu tiền (BR-F5-04, BR-F3-11)
      - TC-F5.2.1-09a `InvoicePaymentConcurrencyIntegrationTest#twoRenewalsPaidAtOnceYieldOneUpcoming`
      - TC-F5.2.1-09b `InvoicePaymentIntegrationTest#upcomingSameTypeBlocksActivation`
      - TC-F5.2.1-09c `InvoicePaymentIntegrationTest#upcomingOfOtherTypeOrScopeDoesNotBlock`
      - TC-F5.2.1-09d `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-09d: 422 do có UPCOMING tải lại PENDING, không báo`
    - AC-F5.2.1-10 Không sử dụng số tiền giả từ trình duyệt (BR-F5-01)
      - TC-F5.2.1-10a `InvoicePaymentIntegrationTest#ignoresClientSuppliedAmounts`
      - TC-F5.2.1-10b `features/invoices/api/invoices.test.ts › TC-F5.2.1-10b: pay gửi đúng method và không gửi số tiền lên `
      - TC-F5.2.1-10c `features/invoices/api/invoices.test.ts › TC-F5.2.1-10c: loại bỏ giá trị tiền gửi thêm và chuẩn hóa mã`
    - AC-F5.2.1-11 Gia hạn sau gói PT 0 buổi còn giữ trạng thái ACTIVE hoặc FROZEN (BR-F3-11)
      - TC-F5.2.1-11a `InvoicePaymentIntegrationTest#zeroSessionPtStillHeldSchedulesAfterOldEndDate`
    - AC-F5.2.1-12 Không thu tiền hóa đơn cổng hội viên quá hạn (BR-F5-07)
      - TC-F5.2.1-12a `InvoiceExpiryIntegrationTest#paymentAndVietQrRespectPortalDeadline`
      - TC-F5.2.1-12b `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-12b: hóa đơn portal quá hạn hiện lỗi server và khô`
- **UC-F5.3** Tra cứu và quản lý hóa đơn tại quầy (và toàn chuỗi)
  - **US-F5.3.1** Tra cứu và hủy hóa đơn chờ (#142) ✅ mọi AC có TC
    - AC-F5.3.1-01 Danh sách hóa đơn theo chi nhánh của nhân viên (BR-F5-06)
      - TC-F5.3.1-01a `InvoiceQueryIntegrationTest#receptionistListsOwnBranchNewestFirstWithPaging`
      - TC-F5.3.1-01b `InvoiceQueryIntegrationTest#staffCannotOverrideBranchFilter`
      - TC-F5.3.1-01c `features/invoices/pages/InvoicesPage.test.tsx › TC-F5.3.1-01c: %s không tải danh mục hoặc chọn chi nhánh khá`
    - AC-F5.3.1-02 Lọc và tìm hóa đơn theo ngày Việt Nam (BR-F5-06)
      - TC-F5.3.1-02a `InvoiceQueryIntegrationTest#filterByStatusSourceAndKeyword`
      - TC-F5.3.1-02b `InvoiceQueryIntegrationTest#filterByVietnamDateRangeHalfOpen`
      - TC-F5.3.1-02c `features/invoices/pages/InvoicesPage.test.tsx › TC-F5.3.1-02c: lọc danh sách hóa đơn theo trạng thái, nguồn,`
      - TC-F5.3.1-02d `features/invoices/pages/InvoicesPage.test.tsx › TC-F5.3.1-02d: lỗi danh sách có thể tải lại và hiển thị trạn`
    - AC-F5.3.1-03 Hóa đơn có member_id rỗng vẫn tra cứu được (BR-F5-06)
      - TC-F5.3.1-03a `InvoiceQueryIntegrationTest#walkInInvoiceWithNullMemberIsVisibleInListAndDetail`
      - TC-F5.3.1-03b `features/invoices/pages/InvoicesPage.test.tsx › TC-F5.3.1-03b: hiển thị hóa đơn khách lẻ và tiền định dạng V`
    - AC-F5.3.1-04 Chủ chuỗi xem toàn chuỗi hoặc lọc chi nhánh (BR-F5-06)
      - TC-F5.3.1-04a `InvoiceQueryIntegrationTest#superAdminListsAllBranchesAndFiltersByBranchId`
      - TC-F5.3.1-04b `InvoiceQueryIntegrationTest#ownerListsBranchOptionsIncludingInactiveBranches`
      - TC-F5.3.1-04c `features/invoices/api/invoices.test.ts › TC-F5.3.1-04c: danh mục chi nhánh dùng endpoint hóa đơn dành`
      - TC-F5.3.1-04d `features/invoices/pages/InvoicesPage.test.tsx › TC-F5.3.1-04d: Chủ chuỗi lọc chi nhánh bằng danh mục toàn ch`
      - TC-F5.3.1-04e `features/invoices/pages/InvoicesPage.test.tsx › TC-F5.3.1-04e: lỗi danh mục chi nhánh có thể thử lại, danh s`
    - AC-F5.3.1-05 Chi tiết hóa đơn gồm dòng hóa đơn và lịch sử thu (BR-F5-06)
      - TC-F5.3.1-05a `InvoiceQueryIntegrationTest#detailReturnsItemsAndPaymentsWithReceiverName`
      - TC-F5.3.1-05b `features/invoices/api/invoices.test.ts › TC-F5.3.1-05b: list và detail gọi đúng endpoint và tham số l`
      - TC-F5.3.1-05c `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.3.1-05c: đổi mã hóa đơn trong route phải tải dữ liệu m`
    - AC-F5.3.1-06 Hủy hóa đơn chờ đồng thời hủy gói chờ (BR-F5-06, BR-F5-08)
      - TC-F5.3.1-06a `InvoiceCancelIntegrationTest#cancelPendingInvoiceCancelsMembership`
      - TC-F5.3.1-06b `InvoiceCancelIntegrationTest#cancelledInvoiceNoLongerBlocksSale`
      - TC-F5.3.1-06c `InvoiceCancelIntegrationTest#managerAndOwnerCanCancel`
      - TC-F5.3.1-06d `features/invoices/api/invoices.test.ts › TC-F5.3.1-06d: cancel gọi PUT /invoices/{id}/cancel`
      - TC-F5.3.1-06e `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.3.1-06e: hủy hóa đơn PENDING thành công và cập nhật tr`
    - AC-F5.3.1-07 Không hủy hóa đơn ngoài trạng thái PENDING
      - TC-F5.3.1-07a `InvoiceCancelIntegrationTest#cancelPaidInvoiceIs422`
      - TC-F5.3.1-07b `InvoiceCancelIntegrationTest#cancelTwiceIs422`
      - TC-F5.3.1-07c `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.3.1-07c: hủy gặp %s phải tải lại trạng thái mới`
    - AC-F5.3.1-08 Phân biệt sai vai trò với hóa đơn ngoài phạm vi (BR-F5-06)
      - TC-F5.3.1-08a `InvoiceCancelIntegrationTest#unknownInvoiceIs404`
      - TC-F5.3.1-08b `InvoiceCancelIntegrationTest#otherBranchIs404`
      - TC-F5.3.1-08c `InvoiceCancelIntegrationTest#otherRolesAreForbidden`
      - TC-F5.3.1-08d `InvoiceQueryIntegrationTest#detailOfOtherBranchOrUnknownIs404`
      - TC-F5.3.1-08e `InvoiceQueryIntegrationTest#trainerAndMemberAreForbidden`
      - TC-F5.3.1-08f `InvoiceQueryIntegrationTest#unauthenticatedRequestsAre401`
      - TC-F5.3.1-08g `InvoiceQueryIntegrationTest#nonOwnerCannotReadBranchOptions`
      - TC-F5.3.1-08h `InvoiceQueryIntegrationTest#anonymousCannotReadBranchOptions`
      - TC-F5.3.1-08i `layouts/staff-menu.test.ts › TC-F5.3.1-08i: menu Thanh toán bật đúng vai trò theo quyền r`
      - TC-F5.3.1-08j `routes/route-access.test.ts › TC-F5.3.1-08j: %s mở được danh sách và chi tiết hóa đơn`
      - TC-F5.3.1-08k `routes/route-access.test.ts › TC-F5.3.1-08k: %s không mở được trang hóa đơn của nhân viên`
    - AC-F5.3.1-09 Bộ lọc và phân trang không hợp lệ
      - TC-F5.3.1-09a `InvoiceQueryIntegrationTest#invalidQueryParamsAndIdReturn400`
      - TC-F5.3.1-09b `InvoiceCancelIntegrationTest#invalidCancelIdIs400`
      - TC-F5.3.1-09c `features/invoices/pages/InvoicesPage.test.tsx › TC-F5.3.1-09c: khoảng ngày ngược báo lỗi và không gửi bộ lọc`
      - TC-F5.3.1-09d `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.3.1-09d: mã route không hợp lệ %s không gọi API`
    - AC-F5.3.1-10 Nhân viên không có phân công chi nhánh hoạt động
      - TC-F5.3.1-10a `InvoiceQueryIntegrationTest#staffWithoutActiveBranchIs403`
- **UC-F5.4** In và xuất hóa đơn PDF
  - **US-F5.4.1** Hóa đơn PDF (#143) ✅ mọi AC có TC
    - AC-F5.4.1-01 Xuất PDF hóa đơn PAID hoặc REFUNDED
      - TC-F5.4.1-01a `InvoiceDocumentIntegrationTest#pdfForPaidAndRefundedInvoiceIsReadableWithVietnameseText`
      - TC-F5.4.1-01b `features/invoices/api/invoices.test.ts › TC-F5.4.1-01b: pdf gọi GET /invoices/{id}/pdf với responseTy`
      - TC-F5.4.1-01c `features/invoices/api/invoices.test.ts › TC-F5.4.1-01c: lỗi PDF không phải JSON vẫn giữ được lỗi HTTP`
      - TC-F5.4.1-01d `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.4.1-01d: hóa đơn PAID và REFUNDED tải PDF bằng Blob, m`
      - TC-F5.4.1-01e `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.4.1-01e: mở được tab PDF thì không tải file lần nữa; b`
      - TC-F5.4.1-01f `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.4.1-01f: rời trang khi PDF đang tải không tạo URL hoặc`
    - AC-F5.4.1-02 PDF giữ tên và giá ở thời điểm bán (BR-F5-09)
      - TC-F5.4.1-02a `InvoiceDocumentIntegrationTest#pdfUsesSnapshotPriceAndDescriptionFromInvoiceItems`
    - AC-F5.4.1-03 Không xuất PDF hóa đơn chưa thanh toán
      - TC-F5.4.1-03a `InvoiceDocumentIntegrationTest#pdfForPendingOrCancelledInvoiceIs422`
      - TC-F5.4.1-03b `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.4.1-03b: hóa đơn PENDING không hiển thị nút In / tải P`
    - AC-F5.4.1-04 Hội viên chỉ tải hóa đơn của mình; nhân viên đúng chi nhánh
      - TC-F5.4.1-04a `InvoiceDocumentIntegrationTest#pdfSecurityOwnershipAndScope`
      - TC-F5.4.1-04b `features/invoices/api/invoices.test.ts › TC-F5.4.1-04b: giữ status và detail của lỗi PDF Blob %s`
      - TC-F5.4.1-04c `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.4.1-04c: PDF 404 dạng Blob phải hiện detail server tha`
      - TC-F5.4.1-04d `features/invoices/pages/MemberInvoicePdfPage.test.tsx › TC-F5.4.1-04d: gọi API PDF trực tiếp, không cần quyền API ch`
      - TC-F5.4.1-04e `features/invoices/pages/MemberInvoicePdfPage.test.tsx › TC-F5.4.1-04e: hiển thị lỗi API %s, không tạo PDF từ dữ liệu`
      - TC-F5.4.1-04f `routes/route-access.test.ts › TC-F5.4.1-04f: chỉ MEMBER mở điểm vào PDF hội viên`
    - AC-F5.4.1-05 Vai trò không được tải PDF và chưa đăng nhập
      - TC-F5.4.1-05a `InvoiceDocumentIntegrationTest#pdfSecurityOwnershipAndScope`
      - TC-F5.4.1-05b `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.4.1-05b: Chủ chuỗi không thấy nút PDF dù hóa đơn PAID`
    - AC-F5.4.1-06 Xuất PDF hóa đơn khách lẻ đã có (BR-F5-09)
      - TC-F5.4.1-06a `InvoiceDocumentIntegrationTest#walkInPaidInvoicePdfAndInvalidId`
    - AC-F5.4.1-07 ID xuất PDF không hợp lệ
      - TC-F5.4.1-07a `InvoiceDocumentIntegrationTest#walkInPaidInvoicePdfAndInvalidId`
      - TC-F5.4.1-07b `InvoiceDocumentIntegrationTest#invalidDocumentIdsReturnFieldErrors`
      - TC-F5.4.1-07c `features/invoices/pages/MemberInvoicePdfPage.test.tsx › TC-F5.4.1-07c: ID %s bị chặn trước khi gọi tải PDF`
- **UC-F5.5** Hoàn tiền hóa đơn (Refund)
  - **US-F5.5.1** Hoàn tiền có kiểm soát (#144) ✅ mọi AC có TC
    - AC-F5.5.1-01 Hoàn toàn bộ hóa đơn có gói chưa dùng (BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14)
      - TC-F5.5.1-01a `RefundIntegrationTest#refundSucceeds`
      - TC-F5.5.1-01b `RefundIntegrationTest#refundUpcomingRenewal`
      - TC-F5.5.1-01c `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.5.1-01c: quản lý chi nhánh hoàn tiền hai bước thành cô`
      - TC-F5.5.1-01d `features/invoices/api/invoices.test.ts › TC-F5.5.1-01d: refund gửi POST /invoices/{id}/refund với ref`
    - AC-F5.5.1-02 Không làm thay đổi gói nối tiếp mua bằng hóa đơn khác (BR-F5-13)
      - TC-F5.5.1-02a `RefundIntegrationTest#refundKeepsFollowingUpcomingUntouched`
    - AC-F5.5.1-03 Chỉ Quản lý chi nhánh được hoàn tiền (BR-F5-11)
      - TC-F5.5.1-03a `RefundIntegrationTest#receptionistIsForbidden`
      - TC-F5.5.1-03b `RefundIntegrationTest#otherRolesAreForbidden`
      - TC-F5.5.1-03c `RefundIntegrationTest#forbiddenComesBeforeValidation`
      - TC-F5.5.1-03d `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.5.1-03d: nút Hoàn tiền chỉ hiện cho BRANCH_MANAGER với`
    - AC-F5.5.1-04 Bắt buộc lý do hoàn tiền có nội dung
      - TC-F5.5.1-04a `RefundIntegrationTest#missingReasonIs400`
      - TC-F5.5.1-04b `RefundIntegrationTest#noBodyOrTooLongReasonIs400`
      - TC-F5.5.1-04c `RefundIntegrationTest#reasonLengthCountedAfterStrip`
      - TC-F5.5.1-04d `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.5.1-04d: để trống lý do hoàn tiền không gửi API và hiệ`
      - TC-F5.5.1-04e `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.5.1-04e: lỗi 400 từ API hiện đúng câu errors.refundRea`
    - AC-F5.5.1-05 Hóa đơn không tồn tại, ngoài chi nhánh và chưa đăng nhập (BR-F5-11)
      - TC-F5.5.1-05a `RefundIntegrationTest#anonymousIs401`
      - TC-F5.5.1-05b `RefundIntegrationTest#unknownInvoiceIs404`
      - TC-F5.5.1-05c `RefundIntegrationTest#otherBranchManagerIs404`
      - TC-F5.5.1-05d `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.5.1-05d: lỗi 403 và 404 khi hoàn tiền hiện đúng câu de`
    - AC-F5.5.1-06 Hai quản lý cùng hoàn một hóa đơn (BR-F5-12)
      - TC-F5.5.1-06a `RefundConcurrencyIntegrationTest#twoManagersRefundingSameInvoice`
      - TC-F5.5.1-06b `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.5.1-06b: lỗi 409 và 422 hóa đơn không PAID hiện đúng c`
    - AC-F5.5.1-07 Chỉ hoàn hóa đơn đang PAID
      - TC-F5.5.1-07a `RefundIntegrationTest#pendingInvoiceIs422`
      - TC-F5.5.1-07b `RefundIntegrationTest#cancelledInvoiceIs422`
      - TC-F5.5.1-07c `RefundIntegrationTest#refundTwiceIs422`
    - AC-F5.5.1-08 Gói ngoài ACTIVE hoặc UPCOMING không đủ điều kiện (BR-F5-14)
      - TC-F5.5.1-08a `RefundIntegrationTest#frozenMembershipIs422`
      - TC-F5.5.1-08b `RefundConcurrencyIntegrationTest#membershipExpiredByJobBetweenCheckAndCancel`
    - AC-F5.5.1-09 Gói đã có check-in không được hoàn (BR-F5-14)
      - TC-F5.5.1-09a `RefundIntegrationTest#checkedInMembershipIs422`
      - TC-F5.5.1-09b `RefundIntegrationTest#ptMembershipUsedForCheckinIs422`
    - AC-F5.5.1-10 Gói PT có buổi đặt kể cả đã hủy không được hoàn (BR-F5-14)
      - TC-F5.5.1-10a `RefundIntegrationTest#bookedPtSessionIs422`
      - TC-F5.5.1-10b `RefundIntegrationTest#cancelledPtSessionStillCounts`
    - AC-F5.5.1-11 Chỗ lớp BOOKED chưa kết thúc chặn hoàn gói TIME (BR-F5-14)
      - TC-F5.5.1-11a `RefundIntegrationTest#upcomingClassBookingIs422`
      - TC-F5.5.1-11b `RefundIntegrationTest#inProgressClassBookingIs422`
      - TC-F5.5.1-11c `RefundIntegrationTest#pastOrCancelledBookingDoesNotBlock`
      - TC-F5.5.1-11d `RefundIntegrationTest#classBookingDoesNotBlockPtRefund`
      - TC-F5.5.1-11e `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.5.1-11e: lỗi 422 gói đã dùng và còn chỗ đặt lớp hiện đ`
    - AC-F5.5.1-12 Lượt dùng gắn với gói khác không chặn hoàn gói chưa dùng (BR-F5-14)
      - TC-F5.5.1-12a `RefundIntegrationTest#otherMembershipUsageDoesNotBlock`

### F6

- **UC-F6.1** Xem số lượng hội viên đang có mặt tại phòng tập
  - **US-F6.1.1** Xem số người đang có mặt tại phòng tập (#145)
    - AC-F6.1.1-01 Đếm chính xác số hội viên duy nhất đang tập và tổng lượt check-in ❌ chưa có TC
    - AC-F6.1.1-02 Chủ chuỗi bắt buộc phải chọn chi nhánh cụ thể ❌ chưa có TC
    - AC-F6.1.1-03 Nhân viên chi nhánh chỉ xem số liệu chi nhánh mình ❌ chưa có TC
    - AC-F6.1.1-04 Phân quyền truy cập các chỉ số Dashboard ❌ chưa có TC
    - AC-F6.1.1-05 Chủ chuỗi xem số liệu của chi nhánh đã ngừng hoạt động ❌ chưa có TC
- **UC-F6.2** Xem tổng lượt check-in và biểu đồ phân bổ theo giờ
  - **US-F6.2.1** Lượt check-in theo giờ (#146)
    - AC-F6.2.1-01 Biểu đồ thể hiện đầy đủ các khung giờ trong giờ mở cửa ❌ chưa có TC
    - AC-F6.2.1-02 Xử lý khung giờ với chi nhánh có giờ mở cửa lẻ ❌ chưa có TC
- **UC-F6.3** Xem tổng doanh thu trong ngày tại quầy
  - **US-F6.3.1** Doanh thu trong ngày (#147)
    - AC-F6.3.1-01 Doanh thu bao gồm cả khách lẻ và phân loại theo hình thức thanh toán ❌ chưa có TC
- **UC-F6.4** Xem danh sách cảnh báo gói tập sắp hết hạn
  - **US-F6.4.1** Gói sắp hết hạn (#148)
    - AC-F6.4.1-01 Lọc chính xác các gói hết hạn trong vòng 7 ngày tới ❌ chưa có TC
    - AC-F6.4.1-02 Cảnh báo gói SESSION sắp hết buổi theo cấu hình ngưỡng ❌ chưa có TC
    - AC-F6.4.1-03 Loại trừ hội viên đã mua gói gia hạn UPCOMING ❌ chưa có TC
    - AC-F6.4.1-04 Gói toàn chuỗi chỉ hiển thị cảnh báo tại chi nhánh gốc ❌ chưa có TC
    - AC-F6.4.1-05 Quy tắc sắp xếp, lý do cảnh báo, giới hạn phân trang và loại trừ hội viên xóa mềm ❌ chưa có TC
    - AC-F6.4.1-06 Thao tác Gia hạn nhanh trên giao diện ❌ chưa có TC
- **UC-F6.5** Xem danh sách lượt check-in mới nhất (Live Feed)
  - **US-F6.5.1** Lượt check-in mới nhất (Live Feed) (#149)
    - AC-F6.5.1-01 Danh sách các lượt check-in mới nhất trong ngày ❌ chưa có TC
    - AC-F6.5.1-02 Thao tác Check-out nhanh từ danh sách Live Feed ❌ chưa có TC

### F7

- **UC-F7.1** Xem gói tập
  - **US-F7.1.1** Xem gói của tôi (#150)
    - AC-F7.1.1-01 Hội viên thấy đủ thông tin các gói của mình ❌ chưa có TC
    - AC-F7.1.1-02 Chỉ thấy gói của chính mình ❌ chưa có TC
    - AC-F7.1.1-03 Hội viên chưa có gói nào ❌ chưa có TC
    - AC-F7.1.1-04 Gói đã hủy không có nút Gia hạn, gói hết hạn vẫn có ❌ chưa có TC
    - AC-F7.1.1-05 Từ chối người không được xem ❌ chưa có TC
- **UC-F7.2** Xem lịch sử check-in
  - **US-F7.2.1** Lịch sử check-in của tôi (#151)
    - AC-F7.2.1-01 Xem lịch sử mới nhất trước, có phân trang ❌ chưa có TC
    - AC-F7.2.1-02 Hội viên chưa check-in lần nào ❌ chưa có TC
    - AC-F7.2.1-03 Chỉ thấy lượt của chính mình ❌ chưa có TC
    - AC-F7.2.1-04 Tham số phân trang ❌ chưa có TC
    - AC-F7.2.1-05 Tải trang dưới 2 giây ❌ chưa có TC
    - AC-F7.2.1-06 Từ chối người không được xem ❌ chưa có TC
- **UC-F7.3** Xem hóa đơn
  - **US-F7.3.1** Hóa đơn của tôi (#152)
    - AC-F7.3.1-01 Xem hóa đơn của mình, mới nhất trước ❌ chưa có TC
    - AC-F7.3.1-02 Hóa đơn chờ do cổng hội viên kèm hạn tự hủy ❌ chưa có TC
    - AC-F7.3.1-03 Chỉ thấy hóa đơn của chính mình ❌ chưa có TC
    - AC-F7.3.1-04 Hội viên chưa có hóa đơn nào ❌ chưa có TC
    - AC-F7.3.1-05 Tham số phân trang ❌ chưa có TC
    - AC-F7.3.1-06 Từ chối người không được xem ❌ chưa có TC
- **UC-F7.4** Gửi yêu cầu mua gói
  - **US-F7.4.1** Gửi yêu cầu mua gói (#153)
    - AC-F7.4.1-01 Gửi yêu cầu mua tạo hóa đơn chờ và gói chờ, giá lấy ở server
      - TC-F7.4.1-01a `PortalPurchaseIntegrationTest#purchaseCreatesPortalInvoice`
    - AC-F7.4.1-02 Gói chỉ hiệu lực sau khi lễ tân thu tiền ❌ chưa có TC
    - AC-F7.4.1-03 Hóa đơn chờ tự hủy sau `PENDING_INVOICE_TTL_DAYS`, kéo theo gói chờ
      - TC-F7.4.1-03a `PendingInvoiceExpiryIntegrationTest#cancelsPortalInvoiceExactlyAtTtl`
      - TC-F7.4.1-03b `PendingInvoiceExpiryIntegrationTest#runningTwiceChangesNothingMore`
      - TC-F7.4.1-03c `PendingInvoiceExpiryIntegrationTest#paidPortalInvoiceIsUntouched`
      - TC-F7.4.1-03d `PendingInvoiceExpiryIntegrationTest#canBuyAgainAfterAutoCancel`
      - TC-F7.4.1-03e `PendingInvoiceExpiryIntegrationTest#brokenInvoiceDoesNotBlockOthers`
      - TC-F7.4.1-03f `PendingInvoiceExpiryIntegrationTest#expiredOwnInvoiceIsCancelledOnNewRequest`
      - TC-F7.4.1-03g `PendingInvoiceExpiryIntegrationTest#notYetExpiredOwnInvoiceStillBlocks`
    - AC-F7.4.1-04 Số ngày tự hủy đọc từ cấu hình
      - TC-F7.4.1-04a `PortalPurchaseIntegrationTest#expiresAtUsesBranchTtl`
      - TC-F7.4.1-04b `PendingInvoiceExpiryIntegrationTest#ttlIsReadPerBranch`
    - AC-F7.4.1-05 Đã có gói cùng loại đang chờ
      - TC-F7.4.1-05a `PortalPurchaseIntegrationTest#duplicatePendingForSamePackageIs422`
      - TC-F7.4.1-05b `PortalPurchaseIntegrationTest#counterPendingForSamePackageIs422`
      - TC-F7.4.1-05c `PortalPurchaseIntegrationTest#otherPackageOfSameTypePendingIs422`
      - TC-F7.4.1-05d `PortalPurchaseIntegrationTest#upcomingOfSameTypeIs422`
      - TC-F7.4.1-05e `PortalPurchaseIntegrationTest#inactiveMemberWithPendingIs422`
    - AC-F7.4.1-06 Hai yêu cầu gửi cùng lúc
      - TC-F7.4.1-06a `PortalPurchaseConcurrencyIntegrationTest#twoPurchasesAtOnceCreateExactlyOne`
    - AC-F7.4.1-07 Gói không hợp lệ để mua
      - TC-F7.4.1-07a `PortalPurchaseIntegrationTest#unknownPackageIs404`
      - TC-F7.4.1-07b `PortalPurchaseIntegrationTest#inactivePackageIs422`
      - TC-F7.4.1-07c `PortalPurchaseIntegrationTest#packageOfOtherBranchIs422`
      - TC-F7.4.1-07d `PortalPurchaseIntegrationTest#inactiveMemberIs422`
    - AC-F7.4.1-08 Dữ liệu sai và người không được gửi
      - TC-F7.4.1-08a `PortalPurchaseIntegrationTest#invalidPurchaseBodyIs400`
      - TC-F7.4.1-08b `PortalPurchaseIntegrationTest#nonMemberIs403`
      - TC-F7.4.1-08c `PortalPurchaseIntegrationTest#memberWithoutProfileIs403`
- **UC-F7.5** Gửi yêu cầu gia hạn gói
  - **US-F7.5.1** Gửi yêu cầu gia hạn (#154)
    - AC-F7.5.1-01 Gia hạn gói còn hiệu lực: tạo gói mới chờ, gói cũ không đổi
      - TC-F7.5.1-01a `PortalPurchaseIntegrationTest#renewCreatesNewPendingMembership`
    - AC-F7.5.1-02 Gói đã hết hạn vẫn gia hạn được
      - TC-F7.5.1-02a `PortalPurchaseIntegrationTest#renewCancelledMembershipIs422`
    - AC-F7.5.1-03 Gói đã hủy không gia hạn được
      - TC-F7.5.1-03a `PortalPurchaseIntegrationTest#renewCancelledMembershipIs422`
    - AC-F7.5.1-04 Gói của hội viên khác hoặc không tồn tại: cùng một câu trả lời
      - TC-F7.5.1-04a `PortalPurchaseIntegrationTest#renewOthersMembershipIs404`
      - TC-F7.5.1-04b `PortalPurchaseIntegrationTest#renewUnknownMembershipIs404`
    - AC-F7.5.1-05 Loại gói đã ngừng bán
      - TC-F7.5.1-05a `PortalPurchaseIntegrationTest#renewInactivePackageIs422`
    - AC-F7.5.1-06 Đã có gói cùng loại đang chờ
      - TC-F7.5.1-06a `PortalPurchaseIntegrationTest#renewTwiceIs422`
    - AC-F7.5.1-07 Hai yêu cầu gia hạn gửi cùng lúc ❌ chưa có TC
    - AC-F7.5.1-08 Hóa đơn gia hạn tự hủy kéo theo gói chờ ❌ chưa có TC
    - AC-F7.5.1-09 Dữ liệu sai và người không được gửi
      - TC-F7.5.1-09a `PortalPurchaseIntegrationTest#invalidRenewBodyIs400`

### F8

- **UC-F8.0** Xem lịch lớp học nhóm công khai
  - **US-F8.0.1** Xem lịch lớp công khai (#155) ✅ mọi AC có TC
    - AC-F8.0.1-01 Xem lịch không cần đăng nhập (BR-F8.0-01, BR-F8.0-02)
      - TC-F8.0.1-01a `ClassApiIntegrationTest#publicCalendarShowsOnlyPublicInformationWithVietnameseDates`
      - TC-F8.0.1-01b `features/classes/ClassCalendarPages.test.tsx › TC-F8.0.1-01b, TC-F8.0.1-04b: khách xem giờ Việt Nam và đăng`
    - AC-F8.0.1-02 Lịch rỗng (BR-F8.0-01)
      - TC-F8.0.1-02a `ClassApiIntegrationTest#publicCalendarShowsOnlyPublicInformationWithVietnameseDates`
      - TC-F8.0.1-02b `features/classes/ClassCalendarPages.test.tsx › TC-F8.0.1-02b, TC-F8.0.1-03b: lịch rỗng và bộ lọc từ chối kh`
    - AC-F8.0.1-03 Bộ lọc không hợp lệ (BR-F8.0-01)
      - TC-F8.0.1-03a `ClassApiIntegrationTest#publicCalendarShowsOnlyPublicInformationWithVietnameseDates`
      - TC-F8.0.1-03b `features/classes/ClassCalendarPages.test.tsx › TC-F8.0.1-02b, TC-F8.0.1-03b: lịch rỗng và bộ lọc từ chối kh`
    - AC-F8.0.1-04 Khách muốn đặt (BR-F8.0-01)
      - TC-F8.0.1-04a `ClassApiIntegrationTest#publicCalendarShowsOnlyPublicInformationWithVietnameseDates`
      - TC-F8.0.1-04b `features/classes/ClassCalendarPages.test.tsx › TC-F8.0.1-01b, TC-F8.0.1-04b: khách xem giờ Việt Nam và đăng`
- **UC-F8.1** Đặt chỗ lớp nhóm
  - **US-F8.1.1** Đặt chỗ lớp nhóm (#156) ✅ mọi AC có TC
    - AC-F8.1.1-01 Đặt khi còn chỗ và ghi người tạo (BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8-01)
      - TC-F8.1.1-01a `ClassApiIntegrationTest#apiAuthorizationValidationAndPersistence`
      - TC-F8.1.1-01b `ClassApiIntegrationTest#receptionistIsRecordedWhenBookingForMember`
      - TC-F8.1.1-01c `ClassApiIntegrationTest#memberCalendarReturnsOwnBookingAndAvailability`
      - TC-F8.1.1-01d `features/classes/ClassCalendarPages.test.tsx › TC-F8.1.1-01d, TC-F8.1.1-03d: đặt đúng hội viên, làm mới sĩ `
    - AC-F8.1.1-02 Gói hoặc quyền đặt không hợp lệ (BR-F8.1-01, BR-F8.3-02)
      - TC-F8.1.1-02a `ClassApiIntegrationTest#rejectsIneligibleMemberships`
      - TC-F8.1.1-02b `features/classes/ClassCalendarPages.test.tsx › TC-F8.1.1-02b: hiển thị nguyên nhân server từ chối đặt chỗ`
    - AC-F8.1.1-03 Lớp đủ chỗ (BR-F8.1-03, BR-F8-01)
      - TC-F8.1.1-03a `ClassApiIntegrationTest#fullClassRejectsBookingAndCancellationReleasesSeat`
      - TC-F8.1.1-03b `ClassApiIntegrationTest#occupiedStatusesCountTowardsCapacity`
      - TC-F8.1.1-03c `ClassApiIntegrationTest#memberCalendarReturnsOwnBookingAndAvailability`
      - TC-F8.1.1-03d `features/classes/ClassCalendarPages.test.tsx › TC-F8.1.1-01d, TC-F8.1.1-03d: đặt đúng hội viên, làm mới sĩ `
    - AC-F8.1.1-04 Hai yêu cầu tranh chỗ cuối (BR-F8.1-03)
      - TC-F8.1.1-04a `ClassApiIntegrationTest#concurrentRequestsDoNotExceedCapacity`
    - AC-F8.1.1-05 Trùng buổi và trùng giờ (BR-F8.1-04)
      - TC-F8.1.1-05a `ClassApiIntegrationTest#concurrentOverlappingRequestsRespectMemberLock`
      - TC-F8.1.1-05b `ClassApiIntegrationTest#apiAuthorizationValidationAndPersistence`
    - AC-F8.1.1-06 Đặt lại sau khi hủy (BR-F8.1-04)
      - TC-F8.1.1-06a `ClassApiIntegrationTest#memberCanBookAgainAfterCancellation`
    - AC-F8.1.1-07 Biên cửa sổ theo ngày Việt Nam (BR-F8.1-02)
      - TC-F8.1.1-07a `ClassApiIntegrationTest#dayWindowUsesVietnamDatesAndBranchOverrides`
    - AC-F8.1.1-08 Đúng và dưới mốc đóng đặt (BR-F8.1-02, BR-F8-01)
      - TC-F8.1.1-08a `ClassApiIntegrationTest#cutoffBoundary`
      - TC-F8.1.1-08b `ClassApiIntegrationTest#afterCutoffCancellationReleasesSeatWithoutReopeningBooking`
    - AC-F8.1.1-09 Phạm vi đặt hộ (BR-F8-01)
      - TC-F8.1.1-09a `ClassApiIntegrationTest#apiAuthorizationValidationAndPersistence`
      - TC-F8.1.1-09b `ClassApiIntegrationTest#staffCannotCrossBranchesAndMemberCannotReadOrCancelOthersBookings`
    - AC-F8.1.1-10 Cấu hình theo chi nhánh (BR-F8.1-02)
      - TC-F8.1.1-10a `ClassApiIntegrationTest#branchCutoffOverridesGlobalValue`
- **UC-F8.2** Đã bỏ (ADR 0014)
  - **US-F8.2.1** Đã bỏ (ADR 0014) (#157)
- **UC-F8.3** Hủy chỗ lớp nhóm
  - **US-F8.3.1** Hủy chỗ lớp nhóm (#158) ✅ mọi AC có TC
    - AC-F8.3.1-01 Hủy sớm hoặc đúng biên (BR-F8.3-01, BR-F8.3-04)
      - TC-F8.3.1-01a `ClassApiIntegrationTest#lateCancellationBoundary`
      - TC-F8.3.1-01b `ClassApiIntegrationTest#receptionistIsRecordedWhenBookingForMember`
    - AC-F8.3.1-02 Hủy muộn đạt ngưỡng khóa (BR-F8.3-01, BR-F8.3-02)
      - TC-F8.3.1-02a `ClassApiIntegrationTest#violationsLockOnceAndExpiredLockDoesNotReuseOldViolations`
    - AC-F8.3.1-03 Hủy chỗ buổi đã đổi giờ (BR-F8.3-01, BR-F8-03)
      - TC-F8.3.1-03a `ClassApiIntegrationTest#cancellationAfterRescheduleIsNotLate`
      - TC-F8.3.1-03b `features/classes/ClassCalendarPages.test.tsx › TC-F8.3.1-03b: xác nhận hủy chỗ đã đổi giờ và hiển thị trạng`
    - AC-F8.3.1-04 Đặt suất vừa hủy (BR-F8.3-04, BR-F8.1-03)
      - TC-F8.3.1-04a `ClassApiIntegrationTest#fullClassRejectsBookingAndCancellationReleasesSeat`
    - AC-F8.3.1-05 Qua tháng Việt Nam (BR-F8.3-02)
      - TC-F8.3.1-05a `ClassApiIntegrationTest#oldCalendarMonthViolationsDoNotCountAtVietnamMidnight`
    - AC-F8.3.1-06 Hết khóa rồi có vi phạm mới (BR-F8.3-02)
      - TC-F8.3.1-06a `ClassApiIntegrationTest#violationsLockOnceAndExpiredLockDoesNotReuseOldViolations`
    - AC-F8.3.1-07 Hai yêu cầu hủy cùng chỗ (BR-F8.3-02, BR-F8.3-04)
      - TC-F8.3.1-07a `ClassApiIntegrationTest#doubleCancellationOnlyRecordsOneViolation`
    - AC-F8.3.1-08 Phạm vi và trạng thái hủy (BR-F8.3-04)
      - TC-F8.3.1-08a `ClassApiIntegrationTest#staffCannotCrossBranchesAndMemberCannotReadOrCancelOthersBookings`
    - AC-F8.3.1-09 Hủy buổi từ phía quản lý (BR-F8.7-05)
      - TC-F8.3.1-09a `ClassApiIntegrationTest#changesPreserveBookingsUntilCancellationAndPublishRecipientSnapshot`
- **UC-F8.4** Đã bỏ (ADR 0014)
  - **US-F8.4.1** Đã bỏ (ADR 0014) (#159)
- **UC-F8.5** Xem lịch dạy lớp nhóm
  - **US-F8.5.1** Xem lịch dạy lớp nhóm (#160) ✅ mọi AC có TC
    - AC-F8.5.1-01 Lịch của người được phân công (BR-F8.5-01, BR-F8.5-02, BR-F8.5-03)
      - TC-F8.5.1-01a `ClassApiIntegrationTest#trainerCalendarUsesActualSessionAssignment`
      - TC-F8.5.1-01b `features/classes/ClassCalendarPages.test.tsx › TC-F8.5.1-01b, TC-F8.6.1-01b, TC-F8.6.1-03b: đi vào buổi đượ`
    - AC-F8.5.1-02 Buổi có người dạy thay (BR-F8.5-01)
      - TC-F8.5.1-02a `ClassApiIntegrationTest#trainerCalendarUsesActualSessionAssignment`
    - AC-F8.5.1-03 Không có lớp (BR-F8.5-01)
      - TC-F8.5.1-03a `ClassApiIntegrationTest#trainerCalendarUsesActualSessionAssignment`
    - AC-F8.5.1-04 Sai vai trò hoặc yêu cầu lịch người khác (BR-F8.5-02)
      - TC-F8.5.1-04a `ClassApiIntegrationTest#trainerCalendarUsesActualSessionAssignment`
- **UC-F8.6** Điểm danh học viên lớp học nhóm
  - **US-F8.6.1** Điểm danh học viên lớp học nhóm (#161) ✅ mọi AC có TC
    - AC-F8.6.1-01 Điểm danh lần đầu từ BOOKED (BR-F8.6-01, BR-F8.6-04, BR-F8.6-05)
      - TC-F8.6.1-01a `ClassApiIntegrationTest#attendanceHonorsAssignmentAndVietnameseDay`
      - TC-F8.6.1-01b `features/classes/ClassCalendarPages.test.tsx › TC-F8.5.1-01b, TC-F8.6.1-01b, TC-F8.6.1-03b: đi vào buổi đượ`
      - TC-F8.6.1-01c `ClassApiIntegrationTest#noShowOfSoftDeletedMemberIsRecordedWithoutPenalty`
    - AC-F8.6.1-02 Vắng mặt lần đầu đạt ngưỡng (BR-F8.6-03, BR-F8.3-02)
      - TC-F8.6.1-02a `ClassApiIntegrationTest#firstNoShowLocksAndCorrectionDoesNotReevaluateLock`
    - AC-F8.6.1-03 Check-in không thay điểm danh (BR-F8.6-02)
      - TC-F8.6.1-03a `ClassApiIntegrationTest#attendanceHonorsAssignmentAndVietnameseDay`
      - TC-F8.6.1-03b `features/classes/ClassCalendarPages.test.tsx › TC-F8.5.1-01b, TC-F8.6.1-01b, TC-F8.6.1-03b: đi vào buổi đượ`
    - AC-F8.6.1-04 Sai phân công hoặc chi nhánh (BR-F8.6-01)
      - TC-F8.6.1-04a `ClassApiIntegrationTest#attendanceHonorsAssignmentAndVietnameseDay`
    - AC-F8.6.1-05 Biên thời gian của huấn luyện viên (BR-F8.6-01, BR-F8.6-05)
      - TC-F8.6.1-05a `ClassApiIntegrationTest#attendanceHonorsAssignmentAndVietnameseDay`
    - AC-F8.6.1-06 Quản lý sửa sau hạn (BR-F8.6-01, BR-F8.6-05)
      - TC-F8.6.1-06a `ClassApiIntegrationTest#attendanceHonorsAssignmentAndVietnameseDay`
    - AC-F8.6.1-07 Sửa NO_SHOW không gỡ khóa (BR-F8.6-01, BR-F8.6-03)
      - TC-F8.6.1-07a `ClassApiIntegrationTest#firstNoShowLocksAndCorrectionDoesNotReevaluateLock`
    - AC-F8.6.1-08 Sửa ATTENDED không tạo hoặc kéo dài khóa (BR-F8.6-01, BR-F8.6-03)
      - TC-F8.6.1-08a `ClassApiIntegrationTest#firstNoShowLocksAndCorrectionDoesNotReevaluateLock`
    - AC-F8.6.1-09 Trạng thái không đủ điều kiện (BR-F8.6-05, BR-F8.6-06)
      - TC-F8.6.1-09a `ClassApiIntegrationTest#firstNoShowLocksAndCorrectionDoesNotReevaluateLock`
    - AC-F8.6.1-10 Gỡ khóa thủ công có audit (BR-F8-02)
      - TC-F8.6.1-10a `ClassApiIntegrationTest#firstNoShowLocksAndCorrectionDoesNotReevaluateLock`
    - AC-F8.6.1-11 Gỡ khóa sai quyền (BR-F8-02)
      - TC-F8.6.1-11a `ClassApiIntegrationTest#firstNoShowLocksAndCorrectionDoesNotReevaluateLock`
- **UC-F8.7** Quản lý lịch lặp lớp học và phân công huấn luyện viên
  - **US-F8.7.1** Quản lý lịch lặp và buổi riêng lẻ (#162)
    - AC-F8.7.1-01 Tạo lịch lặp hợp lệ (BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04)
      - TC-F8.7.1-01a `ClassApiIntegrationTest#scheduleApiCreatesReadsUpdatesDeactivatesAndChecksNumericFields`
    - AC-F8.7.1-02 Sức chứa, giờ hoặc vai trò không hợp lệ (BR-F8.7-02)
      - TC-F8.7.1-02a `ClassApiIntegrationTest#validatesResourcesDatesCapacityAndRole`
    - AC-F8.7.1-03 Chặn xung đột thực tế (BR-F8.7-06)
      - TC-F8.7.1-03a `ClassApiIntegrationTest#recurringOverlapUsesActualWeekdaysNotJustIntersectingDateRanges`
      - TC-F8.7.1-03b `ClassApiIntegrationTest#recurringUpdateRejectsConflictWithItsOwnRescheduledSession`
      - TC-F8.7.1-03c `ClassApiIntegrationTest#scheduleRejectsConcreteRoomTrainerAndPtConflicts`
    - AC-F8.7.1-04 Cùng giờ nhưng khác ngày thực tế (BR-F8.7-06)
      - TC-F8.7.1-04a `ClassApiIntegrationTest#recurringOverlapUsesActualWeekdaysNotJustIntersectingDateRanges`
    - AC-F8.7.1-05 Sửa hoặc ngừng lịch không đổi buổi đã sinh (BR-F8.7-02, BR-F8.7-04)
      - TC-F8.7.1-05a `ClassApiIntegrationTest#scheduleGenerationIsIdempotentSnapshotsAndDeactivationPreserveSessions`
      - TC-F8.7.1-05b `ClassApiIntegrationTest#calendarsPreserveGeneratedClassTypeAfterScheduleEdit`
      - TC-F8.7.1-05c `ClassSessionTypeMigrationIntegrationTest#migrationBackfillsExistingSessionsAndAcceptsInsertsFromPreviousBackend`
    - AC-F8.7.1-06 Giảm sức chứa đúng biên (BR-F8.7-02, BR-F8.1-03) ❌ chưa có TC
    - AC-F8.7.1-07 Buổi không cho sửa sức chứa (BR-F8.7-02) ❌ chưa có TC
    - AC-F8.7.1-08 Quyền lịch lặp và buổi riêng lẻ (BR-F8-04)
      - TC-F8.7.1-08a `ClassApiIntegrationTest#staffCannotCrossBranchesAndMemberCannotReadOrCancelOthersBookings`
      - TC-F8.7.1-08b `ClassApiIntegrationTest#scheduleApiDistinguishesMissingOutOfScopeAndIneligibleResources`
      - TC-F8.7.1-08c `ClassApiIntegrationTest#changesPreserveBookingsUntilCancellationAndPublishRecipientSnapshot`
    - AC-F8.7.1-09 Đổi phòng giữ chỗ (BR-F8.4-03, BR-F8.7-02, BR-F8.7-06)
      - TC-F8.7.1-09a `ClassApiIntegrationTest#changesPreserveBookingsUntilCancellationAndPublishRecipientSnapshot`
      - TC-F8.7.1-09b `ClassApiIntegrationTest#rejectsRoomCapacityAndActualTimeConflict`
    - AC-F8.7.1-10 Đổi giờ giữ chỗ (BR-F8-03, BR-F8.8-02)
      - TC-F8.7.1-10a `ClassApiIntegrationTest#changesPreserveBookingsUntilCancellationAndPublishRecipientSnapshot`
      - TC-F8.7.1-10b `ClassApiIntegrationTest#rejectsRoomCapacityAndActualTimeConflict`
    - AC-F8.7.1-11 Hủy buổi không phạt (BR-F8.7-05)
      - TC-F8.7.1-11a `ClassApiIntegrationTest#changesPreserveBookingsUntilCancellationAndPublishRecipientSnapshot`
    - AC-F8.7.1-12 Lỗi xử lý thông báo không đảo ngược thay đổi (BR-F8.4-03, BR-F8-03, BR-F8.7-05)
      - TC-F8.7.1-12a `ClassApiIntegrationTest#notificationFailureIsIsolatedAndRollbackDoesNotPublish`
- **UC-F8.8** Sinh buổi học từ lịch lặp
  - **US-F8.8.1** Sinh buổi học từ lịch lặp (#163) ✅ mọi AC có TC
    - AC-F8.8.1-01 Sinh đúng thứ, dữ liệu và múi giờ (BR-F8.8-01, BR-F8.8-03)
      - TC-F8.8.1-01a `ClassApiIntegrationTest#scheduleGenerationIsIdempotentSnapshotsAndDeactivationPreserveSessions`
    - AC-F8.8.1-02 Chạy lại hoặc chạy đồng thời (BR-F8.8-02)
      - TC-F8.8.1-02a `ClassApiIntegrationTest#concurrentGenerationAndChangedSessionDoNotCreateDuplicates`
    - AC-F8.8.1-03 Lịch ngoài hiệu lực hoặc đã ngừng (BR-F8.8-01)
      - TC-F8.8.1-03a `ClassApiIntegrationTest#generationSkipsInactiveAndOutOfValidityDates`
    - AC-F8.8.1-04 Sinh lại sau khi buổi đổi giờ (BR-F8.8-02)
      - TC-F8.8.1-04a `ClassApiIntegrationTest#concurrentGenerationAndChangedSessionDoNotCreateDuplicates`
    - AC-F8.8.1-05 Tài nguyên không còn dùng được (BR-F8.8-01, BR-F8.8-03, BR-F8.7-06)
      - TC-F8.8.1-05a `ClassApiIntegrationTest#generationRejectsInactiveRoomOrConflictingSession`

### F9

- **UC-F9.1** Khai báo lịch rảnh và xem lịch dạy
  - **US-F9.1.1** Lịch rảnh và lịch dạy (#164)
    - AC-F9.1.1-01 Khai báo khung rảnh hợp lệ ❌ chưa có TC
    - AC-F9.1.1-02 Dữ liệu sai bị từ chối ❌ chưa có TC
    - AC-F9.1.1-03 Hai khung của cùng một PT không chồng nhau (BR-F9.14) ❌ chưa có TC
    - AC-F9.1.1-04 Khung liền kề hoặc khác thứ không bị coi là chồng (BR-F9.14) ❌ chưa có TC
    - AC-F9.1.1-05 Hai PT khác nhau được trùng giờ (BR-F9.14) ❌ chưa có TC
    - AC-F9.1.1-06 Sửa khung rảnh của mình ❌ chưa có TC
    - AC-F9.1.1-07 Không xóa hoặc thu hẹp khung đang có buổi đã đặt (BR-F9.15) ❌ chưa có TC
    - AC-F9.1.1-08 Buổi đã xong hoặc đã hủy không chặn việc xóa khung (BR-F9.15) ❌ chưa có TC
    - AC-F9.1.1-09 Xem lịch dạy gồm khung rảnh, buổi PT và lớp nhóm ❌ chưa có TC
    - AC-F9.1.1-10 PT chỉ thao tác trên lịch của mình ❌ chưa có TC
    - AC-F9.1.1-11 Từ chối người không phải PT ❌ chưa có TC
- **UC-F9.2** Đặt buổi PT
  - **US-F9.2.1** Đặt buổi PT (#165)
    - AC-F9.2.1-01 Đặt buổi hợp lệ chỉ giữ chỗ, không trừ buổi ❌ chưa có TC
    - AC-F9.2.1-02 Chỉ đặt trong giờ PT rảnh ❌ chưa có TC
    - AC-F9.2.1-03 Đặt sát giờ bị từ chối, đúng mốc thì được ❌ chưa có TC
    - AC-F9.2.1-04 Số giờ tối thiểu đọc từ cấu hình ❌ chưa có TC
    - AC-F9.2.1-05 Không trùng lịch của PT ❌ chưa có TC
    - AC-F9.2.1-06 Không trùng lịch của hội viên ❌ chưa có TC
    - AC-F9.2.1-07 Phải có gói PT đang hiệu lực ❌ chưa có TC
    - AC-F9.2.1-08 Không đặt quá số buổi còn lại, kể cả buổi đã qua giờ chưa xác nhận ❌ chưa có TC
    - AC-F9.2.1-09 Còn chỗ khi số buổi `BOOKED` ít hơn số buổi còn lại ❌ chưa có TC
    - AC-F9.2.1-10 Hai yêu cầu cùng lúc cho một giờ ❌ chưa có TC
    - AC-F9.2.1-11 Dữ liệu sai, PT không tồn tại ❌ chưa có TC
    - AC-F9.2.1-12 Từ chối người không được đặt ❌ chưa có TC
- **UC-F9.3** Xác nhận buổi PT đã diễn ra
  - **US-F9.3.1** Xác nhận buổi PT đã diễn ra (#166)
    - AC-F9.3.1-01 Xác nhận buổi đã diễn ra, trừ một buổi ❌ chưa có TC
    - AC-F9.3.1-02 Hoa hồng chốt lúc xác nhận và không đổi về sau ❌ chưa có TC
    - AC-F9.3.1-03 Chưa tới giờ bắt đầu thì không xác nhận được ❌ chưa có TC
    - AC-F9.3.1-04 Không xác nhận hai lần ❌ chưa có TC
    - AC-F9.3.1-05 Hai thao tác cùng lúc chỉ trừ một lần ❌ chưa có TC
    - AC-F9.3.1-06 Buổi cuối thì gói hết hạn và gói nối tiếp được kích hoạt ngay ❌ chưa có TC
    - AC-F9.3.1-07 Buổi cuối nhưng không có gói nối tiếp ❌ chưa có TC
    - AC-F9.3.1-08 Hội viên vắng mặt vẫn bị trừ buổi, không có hoa hồng (BR-F9.18) ❌ chưa có TC
    - AC-F9.3.1-09 Hạn xác nhận 23:59 cùng ngày (BR-F9.19) ❌ chưa có TC
    - AC-F9.3.1-10 Chỉ PT được phân công mới xác nhận ❌ chưa có TC
- **UC-F9.4** Soạn giáo án
  - **US-F9.4.1** Soạn giáo án (#167)
    - AC-F9.4.1-01 Soạn giáo án cho học viên có buổi PT với mình (BR-F9.10) ❌ chưa có TC
    - AC-F9.4.1-02 Soạn giáo án cho học viên có đặt chỗ lớp do mình dạy (BR-F9.10) ❌ chưa có TC
    - AC-F9.4.1-03 Không soạn cho học viên mình không dạy (BR-F9.10) ❌ chưa có TC
    - AC-F9.4.1-04 Hội viên không tồn tại ❌ chưa có TC
    - AC-F9.4.1-05 Dữ liệu sai bị từ chối ❌ chưa có TC
    - AC-F9.4.1-06 Từ chối người không phải PT ❌ chưa có TC
- **UC-F9.5** Ghi nhật ký tập và chỉ số cơ thể
  - **US-F9.5.1** Nhật ký tập (#168)
    - AC-F9.5.1-01 Ghi nhật ký tập cho học viên của mình (BR-F9.10, BR-F9.11) ❌ chưa có TC
    - AC-F9.5.1-02 Ghi chỉ số cơ thể, người ghi lấy từ token (BR-F9.10) ❌ chưa có TC
    - AC-F9.5.1-03 Nhật ký gắn buổi PT phải thuộc đúng PT và hội viên (BR-F9.10) ❌ chưa có TC
    - AC-F9.5.1-04 Dữ liệu sai bị từ chối ❌ chưa có TC
    - AC-F9.5.1-05 Không ghi cho học viên mình không dạy, hội viên không tồn tại (BR-F9.10) ❌ chưa có TC
    - AC-F9.5.1-06 Từ chối người không phải PT ❌ chưa có TC
- **UC-F9.6** Xem tiến độ học viên
  - **US-F9.6.1** Xem tiến độ học viên (#169)
    - AC-F9.6.1-01 Hội viên xem tiến độ của chính mình (BR-F9.12) ❌ chưa có TC
    - AC-F9.6.1-02 PT xem tiến độ học viên mình dạy (BR-F9.10) ❌ chưa có TC
    - AC-F9.6.1-03 Không xem tiến độ của người khác (BR-F9.10, BR-F9.12) ❌ chưa có TC
    - AC-F9.6.1-04 Chưa có dữ liệu ❌ chưa có TC
    - AC-F9.6.1-05 Khoảng thời gian sai ❌ chưa có TC
    - AC-F9.6.1-06 Chỉ dựa trên dữ liệu thực, không có gợi ý AI (BR-F9.13) ❌ chưa có TC
    - AC-F9.6.1-07 Từ chối người chưa đăng nhập hoặc vai trò khác ❌ chưa có TC
- **UC-F9.7** Hủy buổi PT
  - **US-F9.7.1** Hủy buổi PT (*(chưa có, cần trưởng nhóm tạo issue cho US-F9.7.1)*)
    - AC-F9.7.1-01 Hội viên hủy trước hạn, không mất buổi (BR-F9.16) ❌ chưa có TC
    - AC-F9.7.1-02 Hủy sát giờ bị từ chối, đúng mốc thì được (BR-F9.16) ❌ chưa có TC
    - AC-F9.7.1-03 Mốc hủy đọc từ cấu hình ❌ chưa có TC
    - AC-F9.7.1-04 Lễ tân hủy hộ theo đúng quy tắc của hội viên (BR-F9.16) ❌ chưa có TC
    - AC-F9.7.1-05 Quản lý chi nhánh hủy hộ, có lý do, có dấu vết (BR-F9.17) ❌ chưa có TC
    - AC-F9.7.1-06 Quản lý hủy hộ phải nhập lý do (BR-F9.17) ❌ chưa có TC
    - AC-F9.7.1-07 Quản lý không hủy buổi đã tới giờ bắt đầu ❌ chưa có TC
    - AC-F9.7.1-08 Chỉ hủy được buổi đang `BOOKED` ❌ chưa có TC
    - AC-F9.7.1-09 PT không tự hủy buổi đã có khách (BR-F9.17) ❌ chưa có TC
    - AC-F9.7.1-10 Phân quyền theo chủ buổi và chi nhánh ❌ chưa có TC
    - AC-F9.7.1-11 Hai yêu cầu hủy cùng lúc ❌ chưa có TC

### F10

- **UC-F10.1** Gửi thông báo sắp hết hạn gói
  - **US-F10.1.1** Nhận thông báo khi gói tập sắp hết hạn hoặc sắp hết buổi ✅ mọi AC có TC
    - AC-F10.1.1-01 Thông báo khi gói TIME hoặc gói ACTIVE sắp hết hạn
      - TC-F10.1.1-01b `NotificationReminderIntegrationTest#chainWideMembershipUsesHomeBranch`
      - TC-F10.1.1-01c `MembershipExpiryReminderJobTest#annotationsAndCatchUp`
      - TC-F10.1.1-01d `MembershipExpiryReminderJobTest#retriesEveryFiveMinutesUntilSuccess`
      - TC-F10.1.1-01e `MembershipExpiryReminderJobTest#stopsRetryingOnNewDay`
      - TC-F10.1.1-01f `NotificationReminderIntegrationTest#branchConfigErrorDoesNotRollbackOtherBranch`
    - AC-F10.1.1-02 Thông báo khi gói SESSION sắp hết buổi
      - TC-F10.1.1-02a `NotificationReminderIntegrationTest#sessionThresholdFromConfig`
    - AC-F10.1.1-03 Không gửi thông báo trùng cho cùng một gói
      - TC-F10.1.1-03a `NotificationReminderIntegrationTest#rerunDoesNotDuplicate`
    - AC-F10.1.1-04 Không gửi thông báo khi hội viên đã có gói nối tiếp
      - TC-F10.1.1-04a `NotificationReminderIntegrationTest#upcomingSameTypeSkips`
      - TC-F10.1.1-04b `EmailDispatchIntegrationTest#membershipReminderRecheckedAtDispatch`
    - AC-F10.1.1-05 Không tạo thông báo cho hội viên chưa có tài khoản
      - TC-F10.1.1-05a `NotificationReminderIntegrationTest#memberWithoutAccountSkipped`
    - AC-F10.1.1-06 Hiển thị thông báo trong web và gửi email đúng thời gian
      - TC-F10.1.1-06a `NotificationReminderIntegrationTest#createMakesTwoRows`
      - TC-F10.1.1-06b `NotificationReminderIntegrationTest#createRejectsInvalidArguments`
    - AC-F10.1.1-07 Hẹn gửi email khi phát sinh ngoài giờ mở cửa và xử lý lỗi gửi
      - TC-F10.1.1-07a `NotificationReminderIntegrationTest#createAfterCloseSchedulesNextOpening`
      - TC-F10.1.1-07b `EmailDispatchIntegrationTest#sendsOnlyWhenDue`
      - TC-F10.1.1-07c `EmailDispatchIntegrationTest#outsideHoursAtDispatchReschedules`
      - TC-F10.1.1-07d `EmailDispatchJobTest#runsOffSchedulerThreadAndSkipsOverlap`
      - TC-F10.1.1-07e `EmailDispatchIntegrationTest#sendErrorMarksFailed`
      - TC-F10.1.1-07f `EmailDispatchIntegrationTest#blockedAddressAndDisabledAccount`
      - TC-F10.1.1-07g `OpeningHoursTest#insideHoursSendsNow`
      - TC-F10.1.1-07h `OpeningHoursTest#afterCloseSendsNextMorning`
      - TC-F10.1.1-07i `OpeningHoursTest#beforeOpenSendsSameDay`
      - TC-F10.1.1-07j `OpeningHoursTest#boundaries`
- **UC-F10.2** Gửi thông báo nhắc lịch lớp
  - **US-F10.2.1** Nhận thông báo nhắc lịch lớp học nhóm
    - AC-F10.2.1-01 Tạo thông báo nhắc cho lượt đặt chỗ BOOKED
      - TC-F10.2.1-01a `NotificationReminderIntegrationTest#classWithin24Hours`
      - TC-F10.2.1-01b `NotificationReminderIntegrationTest#classReminderUsesSessionClassTypeNotSchedule`
    - AC-F10.2.1-02 Không gửi thông báo nhắc trùng cho cùng lượt đặt chỗ
      - TC-F10.2.1-02a `NotificationReminderIntegrationTest#classReminderOnce`
    - AC-F10.2.1-03 Không gửi nhắc khi hội viên đã hủy chỗ hoặc lớp bị hủy
      - TC-F10.2.1-03a `NotificationReminderIntegrationTest#cancelledOrStartedNotReminded`
      - TC-F10.2.1-03b `EmailDispatchIntegrationTest#classReminderRecheckedAtDispatch`
    - AC-F10.2.1-04 Không tạo thông báo cho hội viên chưa có tài khoản ❌ chưa có TC
    - AC-F10.2.1-05 Hiển thị thông báo trong web và gửi email nhắc lịch
      - TC-F10.2.1-05a `EmailDispatchIntegrationTest#classReminderRebuildsContentWhenSessionRescheduledBeforeDispatch`
    - AC-F10.2.1-06 Xử lý email phát sinh ngoài giờ và lỗi gửi email
      - TC-F10.2.1-06a `EmailDispatchIntegrationTest#branchDerivedFromRelatedEntity`
- **UC-F10.3** Đã bỏ (ADR 0014)
- **UC-F10.4** Gửi thông báo đổi giờ lớp
  - **US-F10.4.1** Nhận thông báo khi lịch lớp học nhóm thay đổi
    - AC-F10.4.1-01 Tạo thông báo khi nhận sự kiện đổi giờ từ F8 ❌ chưa có TC
    - AC-F10.4.1-02 Nội dung thông báo có giờ cũ và giờ mới ❌ chưa có TC
    - AC-F10.4.1-03 Không tạo thông báo khi buổi lớp không có hội viên đặt chỗ ❌ chưa có TC
    - AC-F10.4.1-04 Chỉ báo hội viên còn giữ chỗ ❌ chưa có TC
    - AC-F10.4.1-05 Không tạo thông báo cho hội viên chưa có tài khoản ❌ chưa có TC
    - AC-F10.4.1-06 Gửi thông báo qua hai kênh và xử lý lỗi email ❌ chưa có TC
- **UC-F10.5** Xem thông báo trong web
  - **US-F10.5.1** Xem và quản lý trạng thái đã đọc của thông báo
    - AC-F10.5.1-01 Xem danh sách thông báo của tài khoản đang đăng nhập ❌ chưa có TC
    - AC-F10.5.1-02 Sắp xếp và phân trang danh sách thông báo ❌ chưa có TC
    - AC-F10.5.1-03 Hiển thị đầy đủ nội dung và trạng thái đã đọc ❌ chưa có TC
    - AC-F10.5.1-04 Đánh dấu thông báo đã đọc ❌ chưa có TC
    - AC-F10.5.1-05 Không cho phép xem hoặc đánh dấu thông báo của người khác ❌ chưa có TC
    - AC-F10.5.1-06 Xử lý dữ liệu đầu vào sai và thông báo không tồn tại ❌ chưa có TC

## 5. Bảng ngược: TC → AC → US → UC → BR

Dùng khi một kiểm thử báo lỗi: tìm mã TC để biết nó vi phạm AC, US và quy tắc nghiệp vụ nào.

| TC | AC | US | UC | BR | Kiểm thử |
| --- | --- | --- | --- | --- | --- |
| TC-F1.1.1-01a | AC-F1.1.1-01 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-09, BR-F1-02 | `AuthControllerTest#dangKyThanhCongTra201KemCookieVaAccessToken` |
| TC-F1.1.1-01b | AC-F1.1.1-01 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-09, BR-F1-02 | `AuthServiceTest#dangKyThanhCongTaoUserVaGoiMemberService` |
| TC-F1.1.1-01c | AC-F1.1.1-01 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-09, BR-F1-02 | `F1RegistrationIntegrationTest#realF1RegistrationCreatesProfileAndJwtCanReadOwnF2Profile` |
| TC-F1.1.1-01d | AC-F1.1.1-01 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-09, BR-F1-02 | `features/auth/pages/RegisterPage.test.tsx › TC-F1.1.1-01d: chuẩn hóa dữ liệu, giữ họ tên tiếng Việt và t` |
| TC-F1.1.1-02a | AC-F1.1.1-02 | US-F1.1.1 | UC-F1.1 | BR-F1-02, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-09 | `F1RegistrationIntegrationTest#phoneAlreadyInRealMembersTableRejectsRegistrationAndRollsBackUser` |
| TC-F1.1.1-02b | AC-F1.1.1-02 | US-F1.1.1 | UC-F1.1 | BR-F1-02, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-09 | `MemberApiIntegrationTest#registrationPortJoinsCallerTransactionAndRollsBackOnDuplicate` |
| TC-F1.1.1-02c | AC-F1.1.1-02 | US-F1.1.1 | UC-F1.1 | BR-F1-02, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-09 | `MemberServiceTest#concurrentDuplicatePhoneStillGetsFrontDeskGuidance` |
| TC-F1.1.1-02d | AC-F1.1.1-02 | US-F1.1.1 | UC-F1.1 | BR-F1-02, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-09 | `features/auth/pages/RegisterPage.test.tsx › TC-F1.1.1-02d: lỗi 409 không có errors hiện chung, không suy` |
| TC-F1.1.1-03a | AC-F1.1.1-03 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-02, BR-F1-03, BR-F1-04, BR-F1-09 | `AuthServiceTest#dangKyEmailDaTonTaiNemConflictException` |
| TC-F1.1.1-03b | AC-F1.1.1-03 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-02, BR-F1-03, BR-F1-04, BR-F1-09 | `MemberApiIntegrationTest#registrationPortJoinsCallerTransactionAndRollsBackOnDuplicate` |
| TC-F1.1.1-03c | AC-F1.1.1-03 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-02, BR-F1-03, BR-F1-04, BR-F1-09 | `features/auth/pages/RegisterPage.test.tsx › TC-F1.1.1-03c: dùng tên trường lỗi thay vì suy từ câu báo lỗ` |
| TC-F1.1.1-04a | AC-F1.1.1-04 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-02, BR-F1-04, BR-F1-09 | `AuthControllerTest#dangKyThieuEmailTra400KemProblemDetailErrors` |
| TC-F1.1.1-04b | AC-F1.1.1-04 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-02, BR-F1-04, BR-F1-09 | `AuthControllerTest#dangKyMatKhauThieuChuSoTra400TheoS1` |
| TC-F1.1.1-04c | AC-F1.1.1-04 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-02, BR-F1-04, BR-F1-09 | `AuthControllerTest#dangKyMatKhauDaiHon72KyTuTra400ThayVi500` |
| TC-F1.1.1-04d | AC-F1.1.1-04 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-02, BR-F1-04, BR-F1-09 | `AuthControllerTest#dangKyMatKhauCoDau50KyTuVuot72ByteTra400ThayVi422` |
| TC-F1.1.1-04e | AC-F1.1.1-04 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-02, BR-F1-04, BR-F1-09 | `AuthControllerTest#dangKySoDienThoaiSaiDinhDangTra400` |
| TC-F1.1.1-04f | AC-F1.1.1-04 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-02, BR-F1-04, BR-F1-09 | `AuthControllerTest#dangKyNgaySinhTuongLaiTra400` |
| TC-F1.1.1-04g | AC-F1.1.1-04 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-02, BR-F1-04, BR-F1-09 | `AuthServiceTest#dangKySoDienThoaiSaiDinhDangNemBusinessRuleException` |
| TC-F1.1.1-04h | AC-F1.1.1-04 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-02, BR-F1-04, BR-F1-09 | `AuthServiceTest#dangKyMatKhauVuotQua72ByteUtf8NemBusinessRuleException` |
| TC-F1.1.1-04i | AC-F1.1.1-04 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-02, BR-F1-04, BR-F1-09 | `MemberApiIntegrationTest#directRegistrationUsesTheSameDtoConstraints` |
| TC-F1.1.1-04j | AC-F1.1.1-04 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-02, BR-F1-04, BR-F1-09 | `features/auth/pages/RegisterPage.test.tsx › TC-F1.1.1-04j: không gửi dữ liệu khi số điện thoại hoặc mật ` |
| TC-F1.1.1-04k | AC-F1.1.1-04 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-02, BR-F1-04, BR-F1-09 | `features/auth/api/validation.test.ts › TC-F1.1.1-04k: khớp quy tắc chữ ASCII và số của backend, vẫn` |
| TC-F1.1.1-04l | AC-F1.1.1-04 | US-F1.1.1 | UC-F1.1 | BR-F1-01, BR-F1-03, BR-F1-02, BR-F1-04, BR-F1-09 | `features/auth/api/validation.test.ts › TC-F1.1.1-04l: giới hạn đúng 72 byte UTF-8 thay vì đếm ký tự` |
| TC-F1.1.1-05a | AC-F1.1.1-05 | US-F1.1.1 | UC-F1.1 | BR-F1-09, BR-F1-01, BR-F1-02, BR-F1-03, BR-F1-04 | `F1RegistrationIntegrationTest#failureAfterPersistingProfileRollsBackBothUserAndMember` |
| TC-F1.1.1-05b | AC-F1.1.1-05 | US-F1.1.1 | UC-F1.1 | BR-F1-09, BR-F1-01, BR-F1-02, BR-F1-03, BR-F1-04 | `F1RegistrationIntegrationTest#inactiveDefaultBranchRejectsRealRegistrationAndRollsBackNewAccount` |
| TC-F1.1.1-05c | AC-F1.1.1-05 | US-F1.1.1 | UC-F1.1 | BR-F1-09, BR-F1-01, BR-F1-02, BR-F1-03, BR-F1-04 | `MemberApiIntegrationTest#registrationPortJoinsCallerTransactionAndRollsBackOnDuplicate` |
| TC-F1.1.1-05d | AC-F1.1.1-05 | US-F1.1.1 | UC-F1.1 | BR-F1-09, BR-F1-01, BR-F1-02, BR-F1-03, BR-F1-04 | `StaffBranchIntegrationTest#missingDefaultBranchIsNotFoundInsteadOfCreatingOne` |
| TC-F1.1.1-05e | AC-F1.1.1-05 | US-F1.1.1 | UC-F1.1 | BR-F1-09, BR-F1-01, BR-F1-02, BR-F1-03, BR-F1-04 | `StaffBranchIntegrationTest#inactiveDefaultBranchIsNotFound` |
| TC-F1.2.1-01a | AC-F1.2.1-01 | US-F1.2.1 | UC-F1.2 | BR-F1-01, BR-F1-03, BR-F1-06, BR-F1-04, BR-F1-05 | `AuthServiceTest#saiMatKhauLan1Den4Tra401CauChungVaTangBoDem` |
| TC-F1.2.1-01b | AC-F1.2.1-01 | US-F1.2.1 | UC-F1.2 | BR-F1-01, BR-F1-03, BR-F1-06, BR-F1-04, BR-F1-05 | `AuthServiceTest#khongLoEmailKhiEmailKhongTonTai` |
| TC-F1.2.1-01c | AC-F1.2.1-01 | US-F1.2.1 | UC-F1.2 | BR-F1-01, BR-F1-03, BR-F1-06, BR-F1-04, BR-F1-05 | `AuthServiceTest#taiKhoanDisabledNhapSaiMatKhauTra401KhongPhai403` |
| TC-F1.2.1-01d | AC-F1.2.1-01 | US-F1.2.1 | UC-F1.2 | BR-F1-01, BR-F1-03, BR-F1-06, BR-F1-04, BR-F1-05 | `AuthControllerTest#dangNhapSaiMatKhauTra401ProblemDetailTiengViet` |
| TC-F1.2.1-01e | AC-F1.2.1-01 | US-F1.2.1 | UC-F1.2 | BR-F1-01, BR-F1-03, BR-F1-06, BR-F1-04, BR-F1-05 | `AuthServiceTest#dangNhapSaiMatKhauNemUnauthorizedException` |
| TC-F1.2.1-01f | AC-F1.2.1-01 | US-F1.2.1 | UC-F1.2 | BR-F1-01, BR-F1-03, BR-F1-06, BR-F1-04, BR-F1-05 | `AuthServiceTest#dangNhapMatKhauVuotQua72ByteNemUnauthorizedExceptionKhongNem500` |
| TC-F1.2.1-01g | AC-F1.2.1-01 | US-F1.2.1 | UC-F1.2 | BR-F1-01, BR-F1-03, BR-F1-06, BR-F1-04, BR-F1-05 | `AuthServiceTest#dangNhapEmailKhongTonTaiVanChayPasswordEncoderMatchesDeChongDoanEmailQuaThoiGian` |
| TC-F1.2.1-02a | AC-F1.2.1-02 | US-F1.2.1 | UC-F1.2 | BR-F1-06, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-05 | `AuthServiceTest#lanSaiThu5Khoa15Phut` |
| TC-F1.2.1-02b | AC-F1.2.1-02 | US-F1.2.1 | UC-F1.2 | BR-F1-06, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-05 | `AuthSessionConcurrencyIntegrationTest#namRequestDangNhapSaiDongThoiTangBoDemDung5VaKhoaTaiKhoan` |
| TC-F1.2.1-03a | AC-F1.2.1-03 | US-F1.2.1 | UC-F1.2 | BR-F1-06, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-05 | `AuthServiceTest#dangKhoaNhapDungMatKhauVanTra403KemThoiGianConLai` |
| TC-F1.2.1-04a | AC-F1.2.1-04 | US-F1.2.1 | UC-F1.2 | BR-F1-01, BR-F1-04, BR-F1-06, BR-F1-03, BR-F1-05 | `AuthServiceTest#hetKhoaThiDangNhapLaiDuoc` |
| TC-F1.2.1-04b | AC-F1.2.1-04 | US-F1.2.1 | UC-F1.2 | BR-F1-01, BR-F1-04, BR-F1-06, BR-F1-03, BR-F1-05 | `AuthServiceTest#dangNhapDungDatLaiBoDemVe0VaXoaLockedUntil` |
| TC-F1.2.1-04c | AC-F1.2.1-04 | US-F1.2.1 | UC-F1.2 | BR-F1-01, BR-F1-04, BR-F1-06, BR-F1-03, BR-F1-05 | `AuthServiceTest#hetKhoaRoiNhapSaiThiBoDemTinhLaiTu1` |
| TC-F1.2.1-04d | AC-F1.2.1-04 | US-F1.2.1 | UC-F1.2 | BR-F1-01, BR-F1-04, BR-F1-06, BR-F1-03, BR-F1-05 | `AuthControllerTest#dangNhapThanhCongTra200KemCookieVaAccessToken` |
| TC-F1.2.1-04e | AC-F1.2.1-04 | US-F1.2.1 | UC-F1.2 | BR-F1-01, BR-F1-04, BR-F1-06, BR-F1-03, BR-F1-05 | `AuthServiceTest#dangNhapThanhCongCapTokenVaCapNhatLastLoginAt` |
| TC-F1.2.1-04f | AC-F1.2.1-04 | US-F1.2.1 | UC-F1.2 | BR-F1-01, BR-F1-04, BR-F1-06, BR-F1-03, BR-F1-05 | `JwtSecurityIntegrationTest#tokenHopLeLayThongTinTaiKhoanHienTaiThanhCong` |
| TC-F1.2.1-04g | AC-F1.2.1-04 | US-F1.2.1 | UC-F1.2 | BR-F1-01, BR-F1-04, BR-F1-06, BR-F1-03, BR-F1-05 | `features/auth/pages/LoginPage.test.tsx › TC-F1.2.1-04g: đăng nhập bằng email đã chuẩn hóa rồi mở tran` |
| TC-F1.2.1-05a | AC-F1.2.1-05 | US-F1.2.1 | UC-F1.2 | BR-F1-05, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06 | `AuthControllerTest#dangNhapTaiKhoanBiKhoaTra403ProblemDetailTiengViet` |
| TC-F1.2.1-05b | AC-F1.2.1-05 | US-F1.2.1 | UC-F1.2 | BR-F1-05, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06 | `AuthServiceTest#dangNhapTaiKhoanBiKhoaNemForbiddenException` |
| TC-F1.3.1-01a | AC-F1.3.1-01 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthControllerTest#lamMoiPhienThanhCongTra200KemCookieMoi` |
| TC-F1.3.1-01b | AC-F1.3.1-01 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthServiceTest#lamMoiThanhCongXoayVongRefreshToken` |
| TC-F1.3.1-01c | AC-F1.3.1-01 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthServiceTest#lamMoiTokenDaBiThuHoiThuHoiToanBoPhienVaNemUnauthorizedException` |
| TC-F1.3.1-01d | AC-F1.3.1-01 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthSessionConcurrencyIntegrationTest#haiYeuCauRefreshDongThoiChiMotThanhCongVaMotNhan401` |
| TC-F1.3.1-01e | AC-F1.3.1-01 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `auth/AuthProvider.test.tsx › TC-F1.3.1-01e: bootstrap làm mới cookie rồi lấy user từ /me` |
| TC-F1.3.1-02a | AC-F1.3.1-02 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthControllerTest#dangXuatThanhCongTra204VaXoaCookie` |
| TC-F1.3.1-02b | AC-F1.3.1-02 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthControllerTest#dangXuatCoJwtCungNguoiDungThuHoiVaXoaCookie` |
| TC-F1.3.1-02c | AC-F1.3.1-02 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthControllerTest#dangXuatCoJwtHetHanVanDoiChieuChuSoHuuCookie` |
| TC-F1.3.1-02d | AC-F1.3.1-02 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthServiceTest#dangXuatThuHoiRefreshToken` |
| TC-F1.3.1-02e | AC-F1.3.1-02 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthServiceTest#dangXuatVoiJwtThuHoiCookieKhiCungChuSoHuu` |
| TC-F1.3.1-02f | AC-F1.3.1-02 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthSessionConcurrencyIntegrationTest#dangXuatBangJwtHetHanCuaTaiKhoanAVanGiuPhienCuaTaiKhoanB` |
| TC-F1.3.1-02g | AC-F1.3.1-02 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `auth/AuthProvider.test.tsx › TC-F1.3.1-02g: logout chỉ xóa phiên sau khi server xác nhận;` |
| TC-F1.3.1-03a | AC-F1.3.1-03 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthControllerTest#lamMoiThieuCookieTra401ProblemDetailVaXoaCookie` |
| TC-F1.3.1-03b | AC-F1.3.1-03 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthServiceTest#lamMoiTokenDaBiThuHoiThuHoiToanBoPhienVaNemUnauthorizedException` |
| TC-F1.3.1-03c | AC-F1.3.1-03 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthServiceTest#lamMoiTokenHetHanNemUnauthorizedException` |
| TC-F1.3.1-03d | AC-F1.3.1-03 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `auth/AuthProvider.test.tsx › TC-F1.3.1-03d: bootstrap không có cookie kết thúc anonymous` |
| TC-F1.3.1-04a | AC-F1.3.1-04 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthControllerTest#dangXuatCoJwtKhacChuCookieKhongXoaCookieChung` |
| TC-F1.3.1-04b | AC-F1.3.1-04 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthControllerTest#dangXuatCoJwtHetHanVanDoiChieuChuSoHuuCookie` |
| TC-F1.3.1-04c | AC-F1.3.1-04 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthServiceTest#dangXuatVoiJwtKhongThuHoiCookieCuaTaiKhoanKhac` |
| TC-F1.3.1-04d | AC-F1.3.1-04 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthSessionConcurrencyIntegrationTest#dangXuatBangJwtCuaTaiKhoanAKhongThuHoiHoacXoaCookieCuaTaiKhoanB` |
| TC-F1.3.1-04e | AC-F1.3.1-04 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `AuthSessionConcurrencyIntegrationTest#dangXuatBangJwtHetHanCuaTaiKhoanAVanGiuPhienCuaTaiKhoanB` |
| TC-F1.3.1-04f | AC-F1.3.1-04 | US-F1.3.1 | UC-F1.3 | BR-F1-04 | `auth/account-change.test.tsx › TC-F1.3.1-04f: refresh khác tài khoản đóng trang bảo vệ và đ` |
| TC-F1.4.1-01a | AC-F1.4.1-01 | US-F1.4.1 | UC-F1.4 | BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06, BR-F1-07 | `ChangePasswordIntegrationTest#doiMatKhauThanhCongMatKhauMoiDangNhapDuocMatKhauCuKhong` |
| TC-F1.4.1-01b | AC-F1.4.1-01 | US-F1.4.1 | UC-F1.4 | BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06, BR-F1-07 | `ChangePasswordIntegrationTest#doiMatKhauThuHoiMoiRefreshTokenCuaNguoiDung` |
| TC-F1.4.1-01c | AC-F1.4.1-01 | US-F1.4.1 | UC-F1.4 | BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06, BR-F1-07 | `AuthServiceTest#doiMatKhauThanhCongCapNhatPasswordHash` |
| TC-F1.4.1-01d | AC-F1.4.1-01 | US-F1.4.1 | UC-F1.4 | BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06, BR-F1-07 | `AuthServiceTest#doiMatKhauThuHoiMoiPhienCuaNguoiDung` |
| TC-F1.4.1-01e | AC-F1.4.1-01 | US-F1.4.1 | UC-F1.4 | BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06, BR-F1-07 | `features/auth/api/auth-api.test.ts › TC-F1.4.1-01e: changePassword gọi POST /auth/change-password` |
| TC-F1.4.1-01f | AC-F1.4.1-01 | US-F1.4.1 | UC-F1.4 | BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06, BR-F1-07 | `features/auth/pages/ChangePasswordPage.test.tsx › TC-F1.4.1-01f: Đổi mật khẩu thành công gọi API, xóa phiên tr` |
| TC-F1.4.1-01g | AC-F1.4.1-01 | US-F1.4.1 | UC-F1.4 | BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06, BR-F1-07 | `auth/AuthProvider.test.tsx › TC-F1.4.1-01g: completePasswordChange phát publishSessionCha` |
| TC-F1.4.1-01h | AC-F1.4.1-01 | US-F1.4.1 | UC-F1.4 | BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06, BR-F1-07 | `ChangePasswordIntegrationTest#concurrentPasswordChangesAcceptOldPasswordOnlyOnce` |
| TC-F1.4.1-01i | AC-F1.4.1-01 | US-F1.4.1 | UC-F1.4 | BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06, BR-F1-07 | `ChangePasswordIntegrationTest#passwordChangeRevokesInFlightRefreshSuccessor` |
| TC-F1.4.1-01j | AC-F1.4.1-01 | US-F1.4.1 | UC-F1.4 | BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06, BR-F1-07 | `features/auth/pages/ChangePasswordPage.test.tsx › TC-F1.4.1-01j: Phản hồi đổi mật khẩu đến muộn giữ phiên mới ` |
| TC-F1.4.1-01k | AC-F1.4.1-01 | US-F1.4.1 | UC-F1.4 | BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06, BR-F1-07 | `auth/AuthProvider.test.tsx › TC-F1.4.1-01k: giữ lượt đăng nhập mới cùng tài khoản khi tab` |
| TC-F1.4.1-02a | AC-F1.4.1-02 | US-F1.4.1 | UC-F1.4 | BR-F1-07, BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06 | `AuthServiceTest#doiMatKhauHienTaiSaiHoacMatKhauMoiTrungCuNemBusinessRuleException` |
| TC-F1.4.1-02b | AC-F1.4.1-02 | US-F1.4.1 | UC-F1.4 | BR-F1-07, BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06 | `AuthControllerTest#doiMatKhauViPhamQuyTacNghiepVuTra422` |
| TC-F1.4.1-02c | AC-F1.4.1-02 | US-F1.4.1 | UC-F1.4 | BR-F1-07, BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06 | `features/auth/pages/ChangePasswordPage.test.tsx › TC-F1.4.1-02c: Lỗi 422 từ API hiển thị đúng thông báo và khô` |
| TC-F1.4.1-03a | AC-F1.4.1-03 | US-F1.4.1 | UC-F1.4 | BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06, BR-F1-07 | `AuthControllerTest#doiMatKhauSaiDinhDangTra400KemErrors` |
| TC-F1.4.1-03b | AC-F1.4.1-03 | US-F1.4.1 | UC-F1.4 | BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-06, BR-F1-07 | `features/auth/pages/ChangePasswordPage.test.tsx › TC-F1.4.1-03b: Kiểm tra quy tắc mật khẩu S1, khớp mật khẩu n` |
| TC-F1.4.1-04a | AC-F1.4.1-04 | US-F1.4.1 | UC-F1.4 | BR-F1-05, BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-07 | `AuthControllerTest#doiMatKhauKhongCoTokenTra401` |
| TC-F1.4.1-04b | AC-F1.4.1-04 | US-F1.4.1 | UC-F1.4 | BR-F1-05, BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-07 | `features/auth/pages/ChangePasswordPage.test.tsx › TC-F1.4.1-04b: mở /doi-mat-khau đúng layout theo vai trò và ` |
| TC-F1.4.1-04c | AC-F1.4.1-04 | US-F1.4.1 | UC-F1.4 | BR-F1-05, BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-07 | `layouts/UserMenu.test.tsx › TC-F1.4.1-04c: UserMenu có mục Đổi mật khẩu cho mọi vai trò,` |
| TC-F1.4.1-04d | AC-F1.4.1-04 | US-F1.4.1 | UC-F1.4 | BR-F1-05, BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-07 | `layouts/UserMenu.test.tsx › TC-F1.4.1-04d: ẩn chữ của nút trên màn hẹp 360px, giữ tên nú` |
| TC-F1.4.1-04e | AC-F1.4.1-04 | US-F1.4.1 | UC-F1.4 | BR-F1-05, BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-07 | `AuthServiceTest#doiMatKhauTaiKhoanKhongActiveNemForbiddenException` |
| TC-F1.4.1-05a | AC-F1.4.1-05 | US-F1.4.1 | UC-F1.4 | BR-F1-06, BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-07 | `ChangePasswordIntegrationTest#saiMatKhauHienTaiNhieuLanThiKhoaTam` |
| TC-F1.4.1-05b | AC-F1.4.1-05 | US-F1.4.1 | UC-F1.4 | BR-F1-06, BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-07 | `ChangePasswordIntegrationTest#dangNhapSaiVaDoiMatKhauSaiDungChungBoDem` |
| TC-F1.4.1-05c | AC-F1.4.1-05 | US-F1.4.1 | UC-F1.4 | BR-F1-06, BR-F1-03, BR-F1-04, BR-F1-05, BR-F1-07 | `features/auth/pages/ChangePasswordPage.test.tsx › TC-F1.4.1-05c: Tài khoản đang khóa tạm (403) thì hiển thị câ` |
| TC-F1.4.1-06a | AC-F1.4.1-06 | US-F1.4.1 | UC-F1.4 | BR-F1-04, BR-F1-06, BR-F1-03, BR-F1-05, BR-F1-07 | `ChangePasswordIntegrationTest#doiMatKhauThanhCongDatLaiBoDemVaXoaCookie` |
| TC-F1.4.1-06b | AC-F1.4.1-06 | US-F1.4.1 | UC-F1.4 | BR-F1-04, BR-F1-06, BR-F1-03, BR-F1-05, BR-F1-07 | `ChangePasswordIntegrationTest#matKhauMoiTrungKhongTangBoDem` |
| TC-F1.5.1-01a | AC-F1.5.1-01 | US-F1.5.1 | UC-F1.5 | BR-F1-01, BR-F1-08, BR-F1-03, BR-F1-04, BR-F1-06 | `PasswordResetServiceTest#emailActiveTaoPasswordResetBamHan30PhutVaGuiEmail` |
| TC-F1.5.1-01b | AC-F1.5.1-01 | US-F1.5.1 | UC-F1.5 | BR-F1-01, BR-F1-08, BR-F1-03, BR-F1-04, BR-F1-06 | `AuthControllerTest#quenMatKhauCongKhaiTra200CauChung` |
| TC-F1.5.1-01c | AC-F1.5.1-01 | US-F1.5.1 | UC-F1.5 | BR-F1-01, BR-F1-08, BR-F1-03, BR-F1-04, BR-F1-06 | `features/auth/pages/ForgotPasswordPage.test.tsx › TC-F1.5.1-01c: nhập email ở /quen-mat-khau gửi email chuẩn h` |
| TC-F1.5.1-01d | AC-F1.5.1-01 | US-F1.5.1 | UC-F1.5 | BR-F1-01, BR-F1-08, BR-F1-03, BR-F1-04, BR-F1-06 | `features/auth/pages/LoginPage.test.tsx › TC-F1.5.1-01d: LoginPage có liên kết Quên mật khẩu? tới /que` |
| TC-F1.5.1-01e | AC-F1.5.1-01 | US-F1.5.1 | UC-F1.5 | BR-F1-01, BR-F1-08, BR-F1-03, BR-F1-04, BR-F1-06 | `features/auth/api/auth-api.test.ts › TC-F1.5.1-01e: forgotPassword và resetPassword gọi API công ` |
| TC-F1.5.1-01f | AC-F1.5.1-01 | US-F1.5.1 | UC-F1.5 | BR-F1-01, BR-F1-08, BR-F1-03, BR-F1-04, BR-F1-06 | `PasswordResetServiceTest#phatHanhTokenTheoUserIdVaThoiHan72GioChoS236` |
| TC-F1.5.1-02a | AC-F1.5.1-02 | US-F1.5.1 | UC-F1.5 | BR-F1-08, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06 | `PasswordResetServiceTest#khongLoEmailKhiKhongTonTaiHoacKhongActive` |
| TC-F1.5.1-02b | AC-F1.5.1-02 | US-F1.5.1 | UC-F1.5 | BR-F1-08, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06 | `PasswordResetServiceTest#guiEmailLoiVanKhongNemNgoaiLeVaKhongGhiTokenRaLog` |
| TC-F1.5.1-03a | AC-F1.5.1-03 | US-F1.5.1 | UC-F1.5 | BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08, BR-F1-01 | `PasswordResetServiceTest#datLaiMatKhauDungTokenKhoaUserGhiUsedAtXoaKhoaTamVaThuHoiPhien` |
| TC-F1.5.1-03b | AC-F1.5.1-03 | US-F1.5.1 | UC-F1.5 | BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08, BR-F1-01 | `AuthControllerTest#datLaiMatKhauHopLeCongKhaiTra204` |
| TC-F1.5.1-03c | AC-F1.5.1-03 | US-F1.5.1 | UC-F1.5 | BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08, BR-F1-01 | `PasswordResetIntegrationTest#luongTronVenQuenVaDatLaiMatKhau` |
| TC-F1.5.1-03d | AC-F1.5.1-03 | US-F1.5.1 | UC-F1.5 | BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08, BR-F1-01 | `features/auth/pages/ResetPasswordPage.test.tsx › TC-F1.5.1-03d: Trang Đặt mật khẩu gửi token và mật khẩu mới ` |
| TC-F1.5.1-03e | AC-F1.5.1-03 | US-F1.5.1 | UC-F1.5 | BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08, BR-F1-01 | `features/auth/pages/ResetPasswordPage.test.tsx › TC-F1.5.1-03e: /quen-mat-khau và /dat-lai-mat-khau là trang ` |
| TC-F1.5.1-03f | AC-F1.5.1-03 | US-F1.5.1 | UC-F1.5 | BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08, BR-F1-01 | `PasswordResetIntegrationTest#datLaiMatKhauVoHieuHoaTatCaLienKetQuenMatKhauKhacCuaCungUser` |
| TC-F1.5.1-04a | AC-F1.5.1-04 | US-F1.5.1 | UC-F1.5 | BR-F1-08, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06 | `PasswordResetServiceTest#tokenSaiDaDungHoacHetHanTra422` |
| TC-F1.5.1-04b | AC-F1.5.1-04 | US-F1.5.1 | UC-F1.5 | BR-F1-08, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06 | `AuthControllerTest#datLaiMatKhauTokenLoiTra422` |
| TC-F1.5.1-04c | AC-F1.5.1-04 | US-F1.5.1 | UC-F1.5 | BR-F1-08, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06 | `PasswordResetIntegrationTest#tokenHetHanSau30PhutTra422VaEmailKhongTonTaiTra200` |
| TC-F1.5.1-04d | AC-F1.5.1-04 | US-F1.5.1 | UC-F1.5 | BR-F1-08, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06 | `PasswordResetIntegrationTest#datLaiMatKhauDongThoiCungMotTokenChiMotLanThanhCong` |
| TC-F1.5.1-04e | AC-F1.5.1-04 | US-F1.5.1 | UC-F1.5 | BR-F1-08, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06 | `features/auth/pages/ResetPasswordPage.test.tsx › TC-F1.5.1-04e: lỗi 422 hết hạn hoặc đã dùng hiển thị đúng câ` |
| TC-F1.5.1-05a | AC-F1.5.1-05 | US-F1.5.1 | UC-F1.5 | BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08 | `AuthControllerTest#quenMatKhauEmailSaiDinhDangTra400` |
| TC-F1.5.1-05b | AC-F1.5.1-05 | US-F1.5.1 | UC-F1.5 | BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08 | `AuthControllerTest#datLaiMatKhauSaiChuanS1Tra400` |
| TC-F1.5.1-05c | AC-F1.5.1-05 | US-F1.5.1 | UC-F1.5 | BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08 | `features/auth/pages/ForgotPasswordPage.test.tsx › TC-F1.5.1-05c: email sai định dạng ở trình duyệt không gọi A` |
| TC-F1.5.1-05d | AC-F1.5.1-05 | US-F1.5.1 | UC-F1.5 | BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08 | `features/auth/pages/ResetPasswordPage.test.tsx › TC-F1.5.1-05d: thiếu hoặc rỗng token trên URL báo lỗi ngay, ` |
| TC-F1.5.1-05e | AC-F1.5.1-05 | US-F1.5.1 | UC-F1.5 | BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08 | `features/auth/pages/ResetPasswordPage.test.tsx › TC-F1.5.1-05e: kiểm tra chuẩn S1, hai mật khẩu không khớp kh` |
| TC-F1.5.1-06a | AC-F1.5.1-06 | US-F1.5.1 | UC-F1.5 | BR-F1-08, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06 | `BrevoEmailSenderTest#guiEmailQuaBrevoBangRestClientKhongGoiMangThat` |
| TC-F1.5.1-06b | AC-F1.5.1-06 | US-F1.5.1 | UC-F1.5 | BR-F1-08, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06 | `BrevoEmailSenderTest#khoaBrevoRongThiBeanLaLogEmailSender` |
| TC-F1.5.1-06c | AC-F1.5.1-06 | US-F1.5.1 | UC-F1.5 | BR-F1-08, BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-06 | `BrevoEmailSenderTest#coKhoaBrevoThiBeanLaBrevoEmailSender` |
| TC-F1.6.1-01a | AC-F1.6.1-01 | US-F1.6.1 | UC-F1.6 | BR-F1-04, BR-F1-05 | `AuthServiceTest#getCurrentUserDocThongTinTuJwtVaNem401KhiClaimKhongHopLe` |
| TC-F1.6.1-01b | AC-F1.6.1-01 | US-F1.6.1 | UC-F1.6 | BR-F1-04, BR-F1-05 | `JwtSecurityIntegrationTest#chuaGuiTokenTra401ProblemDetailTiengViet` |
| TC-F1.6.1-01c | AC-F1.6.1-01 | US-F1.6.1 | UC-F1.6 | BR-F1-04, BR-F1-05 | `JwtSecurityIntegrationTest#tokenSaiChuKyTra401ProblemDetailTiengViet` |
| TC-F1.6.1-01d | AC-F1.6.1-01 | US-F1.6.1 | UC-F1.6 | BR-F1-04, BR-F1-05 | `JwtSecurityIntegrationTest#tokenHetHanTra401ProblemDetailTiengViet` |
| TC-F1.6.1-01e | AC-F1.6.1-01 | US-F1.6.1 | UC-F1.6 | BR-F1-04, BR-F1-05 | `JwtSecurityIntegrationTest#tokenThieuClaimRoleBiChanVoi401KhiGoiApi` |
| TC-F1.6.1-01f | AC-F1.6.1-01 | US-F1.6.1 | UC-F1.6 | BR-F1-04, BR-F1-05 | `JwtSecurityIntegrationTest#tokenCoRoleLaHoacSubKhongPhaiSoTra401ThayVi500` |
| TC-F1.6.1-01g | AC-F1.6.1-01 | US-F1.6.1 | UC-F1.6 | BR-F1-04, BR-F1-05 | `routes/ProtectedRoute.test.tsx › TC-F1.6.1-01g: chuyển về /login khi chưa đăng nhập` |
| TC-F1.6.1-02a | AC-F1.6.1-02 | US-F1.6.1 | UC-F1.6 | BR-F1-05, BR-F1-04 | `AuthControllerTest#layThongTinTaiKhoanHienTaiUyQuyenChoAuthService` |
| TC-F1.6.1-02b | AC-F1.6.1-02 | US-F1.6.1 | UC-F1.6 | BR-F1-05, BR-F1-04 | `JwtSecurityIntegrationTest#tokenHopLeLayThongTinTaiKhoanHienTaiThanhCong` |
| TC-F1.6.1-02c | AC-F1.6.1-02 | US-F1.6.1 | UC-F1.6 | BR-F1-05, BR-F1-04 | `JwtSecurityIntegrationTest#goiApiKhiDuVaiTroTra200` |
| TC-F1.6.1-02d | AC-F1.6.1-02 | US-F1.6.1 | UC-F1.6 | BR-F1-05, BR-F1-04 | `JwtSecurityIntegrationTest#goiApiKhiKhongDuVaiTroTra403ProblemDetailTiengViet` |
| TC-F1.6.1-02e | AC-F1.6.1-02 | US-F1.6.1 | UC-F1.6 | BR-F1-05, BR-F1-04 | `JwtSecurityIntegrationTest#goiApiNhanVienBangVaiTroTrainerKhongDuQuyenTra403` |
| TC-F1.6.1-02f | AC-F1.6.1-02 | US-F1.6.1 | UC-F1.6 | BR-F1-05, BR-F1-04 | `routes/ProtectedRoute.test.tsx › TC-F1.6.1-02f: chuyển về /403 khi sai vai trò` |
| TC-F1.6.1-02g | AC-F1.6.1-02 | US-F1.6.1 | UC-F1.6 | BR-F1-05, BR-F1-04 | `routes/ProtectedRoute.test.tsx › TC-F1.6.1-02g: hiển thị nội dung khi đúng vai trò` |
| TC-F1.6.1-03a | AC-F1.6.1-03 | US-F1.6.1 | UC-F1.6 | BR-F1-05, BR-F1-04 | `F1RegistrationIntegrationTest#realReceptionistJwtWithoutStaffAssignmentCannotCreateOrListMembers` |
| TC-F1.6.1-03b | AC-F1.6.1-03 | US-F1.6.1 | UC-F1.6 | BR-F1-05, BR-F1-04 | `StaffBranchIntegrationTest#staffBranchComesFromThatUsersAssignmentRatherThanDefaultBranch` |
| TC-F1.6.1-03c | AC-F1.6.1-03 | US-F1.6.1 | UC-F1.6 | BR-F1-05, BR-F1-04 | `StaffBranchIntegrationTest#userWithoutStaffProfileIsDeniedRatherThanAssignedToCn01` |
| TC-F1.6.1-03d | AC-F1.6.1-03 | US-F1.6.1 | UC-F1.6 | BR-F1-05, BR-F1-04 | `StaffBranchIntegrationTest#inactiveStaffIsDenied` |
| TC-F1.6.1-03e | AC-F1.6.1-03 | US-F1.6.1 | UC-F1.6 | BR-F1-05, BR-F1-04 | `StaffBranchIntegrationTest#activeStaffInInactiveBranchIsDenied` |
| TC-F1.6.1-03f | AC-F1.6.1-03 | US-F1.6.1 | UC-F1.6 | BR-F1-05, BR-F1-04 | `StaffBranchIntegrationTest#invalidOrUnknownUserIdsNeverReceiveDefaultBranch` |
| TC-F1.6.1-04a | AC-F1.6.1-04 | US-F1.6.1 | UC-F1.6 | BR-F1-04, BR-F1-05 | `AccountServiceTest#disableAccountCapNhatDisabledVaThuHoiRefreshToken` |
| TC-F1.6.1-04b | AC-F1.6.1-04 | US-F1.6.1 | UC-F1.6 | BR-F1-04, BR-F1-05 | `F1RegistrationIntegrationTest#disablingRevokesAllMemberSessionsAndReactivationDoesNotRestoreOldTokens` |
| TC-F2.1.1-01a | AC-F2.1.1-01 | US-F2.1.1 | UC-F2.1 | BR-F2-05, BR-F2-06, BR-F2-07, BR-F2-10 | `F1RegistrationIntegrationTest#realReceptionistJwtCreatesAndManagesCounterProfileInAssignedBranch` |
| TC-F2.1.1-01b | AC-F2.1.1-01 | US-F2.1.1 | UC-F2.1 | BR-F2-05, BR-F2-06, BR-F2-07, BR-F2-10 | `MemberApiIntegrationTest#createNormalizesAndReturnsLocationAndAudit` |
| TC-F2.1.1-01c | AC-F2.1.1-01 | US-F2.1.1 | UC-F2.1 | BR-F2-05, BR-F2-06, BR-F2-07, BR-F2-10 | `MemberApiIntegrationTest#createAndUpdateAcceptPastOrAbsentBirthDate` |
| TC-F2.1.1-01d | AC-F2.1.1-01 | US-F2.1.1 | UC-F2.1 | BR-F2-05, BR-F2-06, BR-F2-07, BR-F2-10 | `MemberApiIntegrationTest#branchManagerCanCreateReadUpdateAndSearchWithinTheirBranch` |
| TC-F2.1.1-01e | AC-F2.1.1-01 | US-F2.1.1 | UC-F2.1 | BR-F2-05, BR-F2-06, BR-F2-07, BR-F2-10 | `MemberServiceTest#createsWithStaffBranchAndNormalizedPhone` |
| TC-F2.1.1-02a | AC-F2.1.1-02 | US-F2.1.1 | UC-F2.1 | BR-F2-06, BR-F2-05, BR-F2-07, BR-F2-10 | `MemberApiIntegrationTest#createAndUpdateRejectTodayOrFutureBirthDateWithoutChanges` |
| TC-F2.1.1-02b | AC-F2.1.1-02 | US-F2.1.1 | UC-F2.1 | BR-F2-06, BR-F2-05, BR-F2-07, BR-F2-10 | `MemberApiIntegrationTest#invalidNamesAreRejectedByRequestValidationOnCreateAndUpdate` |
| TC-F2.1.1-02c | AC-F2.1.1-02 | US-F2.1.1 | UC-F2.1 | BR-F2-06, BR-F2-05, BR-F2-07, BR-F2-10 | `MemberApiIntegrationTest#invalidBodiesAndPaginationAre400AndDuplicateIs409` |
| TC-F2.1.1-02d | AC-F2.1.1-02 | US-F2.1.1 | UC-F2.1 | BR-F2-06, BR-F2-05, BR-F2-07, BR-F2-10 | `MemberApiIntegrationTest#incompletePhoneCannotBeSavedOnCreateOrUpdate` |
| TC-F2.1.1-02e | AC-F2.1.1-02 | US-F2.1.1 | UC-F2.1 | BR-F2-06, BR-F2-05, BR-F2-07, BR-F2-10 | `MemberApiIntegrationTest#concurrentRequestsCannotCreateDuplicatePhone` |
| TC-F2.1.1-02f | AC-F2.1.1-02 | US-F2.1.1 | UC-F2.1 | BR-F2-06, BR-F2-05, BR-F2-07, BR-F2-10 | `MemberServiceTest#duplicatePhoneDoesNotConsumeSequence` |
| TC-F2.1.1-02g | AC-F2.1.1-02 | US-F2.1.1 | UC-F2.1 | BR-F2-06, BR-F2-05, BR-F2-07, BR-F2-10 | `MemberServiceTest#concurrentDuplicatePhoneStillGetsFrontDeskGuidance` |
| TC-F2.1.1-02h | AC-F2.1.1-02 | US-F2.1.1 | UC-F2.1 | BR-F2-06, BR-F2-05, BR-F2-07, BR-F2-10 | `features/members/components/MemberFormModal.test.tsx › TC-F2.1.1-02h: hiển thị lỗi validation do backend trả về nga` |
| TC-F2.1.1-02i | AC-F2.1.1-02 | US-F2.1.1 | UC-F2.1 | BR-F2-06, BR-F2-05, BR-F2-07, BR-F2-10 | `features/members/components/MemberFormModal.test.tsx › TC-F2.1.1-02i: giữ form và dữ liệu đã nhập khi server từ chố` |
| TC-F2.1.1-03a | AC-F2.1.1-03 | US-F2.1.1 | UC-F2.1 | BR-F2-10, BR-F2-05, BR-F2-06, BR-F2-07 | `F1RegistrationIntegrationTest#realReceptionistJwtWithoutStaffAssignmentCannotCreateOrListMembers` |
| TC-F2.1.1-03b | AC-F2.1.1-03 | US-F2.1.1 | UC-F2.1 | BR-F2-10, BR-F2-05, BR-F2-06, BR-F2-07 | `MemberApiIntegrationTest#validatesEndpointAndRecordPermissions` |
| TC-F2.2.1-01a | AC-F2.2.1-01 | US-F2.2.1 | UC-F2.2 | BR-F2-06, BR-F2-11, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-01a: tạo tài khoản thành công báo đã gửi email, tả` |
| TC-F2.2.1-01b | AC-F2.2.1-01 | US-F2.2.1 | UC-F2.2 | BR-F2-06, BR-F2-11, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `features/members/pages/MembersPage.test.tsx › TC-F2.2.1-01b: chỉ tạo tài khoản cho hồ sơ ACTIVE chưa có em` |
| TC-F2.2.1-01c | AC-F2.2.1-01 | US-F2.2.1 | UC-F2.2 | BR-F2-06, BR-F2-11, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `AccountServiceTest#createMemberAccountChuanHoaEmailVaBamMatKhauNgauNhien` |
| TC-F2.2.1-01d | AC-F2.2.1-01 | US-F2.2.1 | UC-F2.2 | BR-F2-06, BR-F2-11, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberServiceTest#linkAccountAttachesNewUserToActiveUnlinkedMember` |
| TC-F2.2.1-01e | AC-F2.2.1-01 | US-F2.2.1 | UC-F2.2 | BR-F2-06, BR-F2-11, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberAccountServiceTest#createAccountIssues72HourTokenAndSendsActivationEmail` |
| TC-F2.2.1-01f | AC-F2.2.1-01 | US-F2.2.1 | UC-F2.2 | BR-F2-06, BR-F2-11, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberAccountIntegrationTest#createAccountPersistsUserLinkTokenAndSendsActivationEmail` |
| TC-F2.2.1-02a | AC-F2.2.1-02 | US-F2.2.1 | UC-F2.2 | BR-F2-11, BR-F2-06, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberAccountIntegrationTest#resetPasswordWithin72HoursAndLoginSucceedsWhileExpiredReturns422` |
| TC-F2.2.1-03a | AC-F2.2.1-03 | US-F2.2.1 | UC-F2.2 | BR-F2-11, BR-F2-06, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-03a, TC-F2.2.1-05a: hồ sơ đã có tài khoản hiện ema` |
| TC-F2.2.1-03b | AC-F2.2.1-03 | US-F2.2.1 | UC-F2.2 | BR-F2-11, BR-F2-06, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-03b, TC-F2.2.1-04a, TC-F2.2.1-05b, TC-F2.2.1-07c: ` |
| TC-F2.2.1-03c | AC-F2.2.1-03 | US-F2.2.1 | UC-F2.2 | BR-F2-11, BR-F2-06, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberServiceTest#linkAccountRejectsAlreadyLinkedMemberWith409` |
| TC-F2.2.1-03d | AC-F2.2.1-03 | US-F2.2.1 | UC-F2.2 | BR-F2-11, BR-F2-06, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberAccountIntegrationTest#memberAlreadyHasAccountReturns409` |
| TC-F2.2.1-03e | AC-F2.2.1-03 | US-F2.2.1 | UC-F2.2 | BR-F2-11, BR-F2-06, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberAccountIntegrationTest#concurrentCreateAccountForSameMemberCreatesSingleUser` |
| TC-F2.2.1-04a | AC-F2.2.1-04 | US-F2.2.1 | UC-F2.2 | BR-F2-06, BR-F2-11, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-03b, TC-F2.2.1-04a, TC-F2.2.1-05b, TC-F2.2.1-07c: ` |
| TC-F2.2.1-04b | AC-F2.2.1-04 | US-F2.2.1 | UC-F2.2 | BR-F2-06, BR-F2-11, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `AccountServiceTest#createMemberAccountTrungEmailTra409` |
| TC-F2.2.1-04c | AC-F2.2.1-04 | US-F2.2.1 | UC-F2.2 | BR-F2-06, BR-F2-11, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberAccountIntegrationTest#emailAlreadyUsedByAnotherAccountReturns409` |
| TC-F2.2.1-04d | AC-F2.2.1-04 | US-F2.2.1 | UC-F2.2 | BR-F2-06, BR-F2-11, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberAccountIntegrationTest#concurrentCreateAccountWithSameEmailRejectsSecondWith409` |
| TC-F2.2.1-05a | AC-F2.2.1-05 | US-F2.2.1 | UC-F2.2 | BR-F2-11, BR-F2-06, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-03a, TC-F2.2.1-05a: hồ sơ đã có tài khoản hiện ema` |
| TC-F2.2.1-05b | AC-F2.2.1-05 | US-F2.2.1 | UC-F2.2 | BR-F2-11, BR-F2-06, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-03b, TC-F2.2.1-04a, TC-F2.2.1-05b, TC-F2.2.1-07c: ` |
| TC-F2.2.1-05c | AC-F2.2.1-05 | US-F2.2.1 | UC-F2.2 | BR-F2-11, BR-F2-06, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberServiceTest#linkAccountRejectsInactiveMemberWith422` |
| TC-F2.2.1-05d | AC-F2.2.1-05 | US-F2.2.1 | UC-F2.2 | BR-F2-11, BR-F2-06, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberAccountIntegrationTest#inactiveMemberReturns422` |
| TC-F2.2.1-06a | AC-F2.2.1-06 | US-F2.2.1 | UC-F2.2 | BR-F2-11, BR-F2-06, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberAccountServiceTest#emailFailureDoesNotFailAccountCreationAndDoesNotLogToken` |
| TC-F2.2.1-06b | AC-F2.2.1-06 | US-F2.2.1 | UC-F2.2 | BR-F2-11, BR-F2-06, BR-F2-10, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberAccountIntegrationTest#emailProviderFailureStillReturns200AndCommitsAccount` |
| TC-F2.2.1-07b | AC-F2.2.1-07 | US-F2.2.1 | UC-F2.2 | BR-F2-10, BR-F2-11, BR-F2-06, BR-F1-01, BR-F1-03, BR-F1-08 | `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-07b: kiểm tra định dạng email ở trình duyệt trước ` |
| TC-F2.2.1-07c | AC-F2.2.1-07 | US-F2.2.1 | UC-F2.2 | BR-F2-10, BR-F2-11, BR-F2-06, BR-F1-01, BR-F1-03, BR-F1-08 | `features/members/components/MemberFormModal.test.tsx › TC-F2.2.1-03b, TC-F2.2.1-04a, TC-F2.2.1-05b, TC-F2.2.1-07c: ` |
| TC-F2.2.1-07d | AC-F2.2.1-07 | US-F2.2.1 | UC-F2.2 | BR-F2-10, BR-F2-11, BR-F2-06, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberAccountIntegrationTest#enforcesRoleAndBranchPermissions` |
| TC-F2.2.1-07e | AC-F2.2.1-07 | US-F2.2.1 | UC-F2.2 | BR-F2-10, BR-F2-11, BR-F2-06, BR-F1-01, BR-F1-03, BR-F1-08 | `MemberAccountIntegrationTest#invalidEmailOrNonPositiveMemberIdReturns400` |
| TC-F2.3.1-01a | AC-F2.3.1-01 | US-F2.3.1 | UC-F2.3 | BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10 | `F1RegistrationIntegrationTest#listGetsBatchEmailsThroughRealF1AndIncludesUnlinkedProfile` |
| TC-F2.3.1-01b | AC-F2.3.1-01 | US-F2.3.1 | UC-F2.3 | BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10 | `MemberApiIntegrationTest#searchScopesBothRowsAndCountAndIncludesUnlinkedProfiles` |
| TC-F2.3.1-01c | AC-F2.3.1-01 | US-F2.3.1 | UC-F2.3 | BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10 | `MemberApiIntegrationTest#memberCodeSearchIsExactAndIgnoresCase` |
| TC-F2.3.1-01d | AC-F2.3.1-01 | US-F2.3.1 | UC-F2.3 | BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10 | `MemberApiIntegrationTest#phonePrefixSearchRespectsBranchStatusSoftDeletionAndPagination` |
| TC-F2.3.1-01e | AC-F2.3.1-01 | US-F2.3.1 | UC-F2.3 | BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10 | `MemberApiIntegrationTest#shortNumericKeywordsWithoutMatchingPrefixReturnAnEmptyPage` |
| TC-F2.3.1-01f | AC-F2.3.1-01 | US-F2.3.1 | UC-F2.3 | BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10 | `MemberApiIntegrationTest#longInvalidNumericKeywordsReturnAnEmptyPage` |
| TC-F2.3.1-01g | AC-F2.3.1-01 | US-F2.3.1 | UC-F2.3 | BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10 | `MemberApiIntegrationTest#completePhoneSearchStillNormalizesAndMatchesExactly` |
| TC-F2.3.1-01h | AC-F2.3.1-01 | US-F2.3.1 | UC-F2.3 | BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10 | `MemberApiIntegrationTest#escapesNameWildcardsAndFiltersStatus` |
| TC-F2.3.1-01i | AC-F2.3.1-01 | US-F2.3.1 | UC-F2.3 | BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10 | `MemberApiIntegrationTest#measuresSearchOnTenThousandProfiles` |
| TC-F2.3.1-01j | AC-F2.3.1-01 | US-F2.3.1 | UC-F2.3 | BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10 | `features/members/pages/MembersPage.test.tsx › TC-F2.3.1-01j: đổi trang gọi API từ 0, tìm kiếm mới trở về t` |
| TC-F2.3.1-01k | AC-F2.3.1-01 | US-F2.3.1 | UC-F2.3 | BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10 | `features/members/pages/MembersPage.test.tsx › TC-F2.3.1-01k: áp dụng từ khóa đang nhập khi đổi trạng thái ` |
| TC-F2.3.1-02a | AC-F2.3.1-02 | US-F2.3.1 | UC-F2.3 | BR-F2-10, BR-F2-05, BR-F2-06, BR-F2-09 | `F1RegistrationIntegrationTest#listGetsBatchEmailsThroughRealF1AndIncludesUnlinkedProfile` |
| TC-F2.3.1-02b | AC-F2.3.1-02 | US-F2.3.1 | UC-F2.3 | BR-F2-10, BR-F2-05, BR-F2-06, BR-F2-09 | `MemberApiIntegrationTest#searchScopesBothRowsAndCountAndIncludesUnlinkedProfiles` |
| TC-F2.3.1-03a | AC-F2.3.1-03 | US-F2.3.1 | UC-F2.3 | BR-F2-09, BR-F2-10, BR-F2-05, BR-F2-06 | `F1RegistrationIntegrationTest#realF1RegistrationCreatesProfileAndJwtCanReadOwnF2Profile` |
| TC-F2.3.1-03b | AC-F2.3.1-03 | US-F2.3.1 | UC-F2.3 | BR-F2-09, BR-F2-10, BR-F2-05, BR-F2-06 | `MemberApiIntegrationTest#validatesEndpointAndRecordPermissions` |
| TC-F2.3.1-03c | AC-F2.3.1-03 | US-F2.3.1 | UC-F2.3 | BR-F2-09, BR-F2-10, BR-F2-05, BR-F2-06 | `MemberServiceTest#memberCannotReadSomeoneElse` |
| TC-F2.3.1-04a | AC-F2.3.1-04 | US-F2.3.1 | UC-F2.3 | BR-F2-10, BR-F2-05, BR-F2-06, BR-F2-09 | `MemberApiIntegrationTest#validatesEndpointAndRecordPermissions` |
| TC-F2.4.1-01a | AC-F2.4.1-01 | US-F2.4.1 | UC-F2.4 | BR-F2-06, BR-F2-10, BR-F2-08, BR-F2-09 | `MemberApiIntegrationTest#createAndUpdateAcceptPastOrAbsentBirthDate` |
| TC-F2.4.1-01b | AC-F2.4.1-01 | US-F2.4.1 | UC-F2.4 | BR-F2-06, BR-F2-10, BR-F2-08, BR-F2-09 | `MemberApiIntegrationTest#branchManagerCanCreateReadUpdateAndSearchWithinTheirBranch` |
| TC-F2.4.1-01c | AC-F2.4.1-01 | US-F2.4.1 | UC-F2.4 | BR-F2-06, BR-F2-10, BR-F2-08, BR-F2-09 | `MemberApiIntegrationTest#updateSamePhoneAndDisableRevokeAndActivate` |
| TC-F2.4.1-01d | AC-F2.4.1-01 | US-F2.4.1 | UC-F2.4 | BR-F2-06, BR-F2-10, BR-F2-08, BR-F2-09 | `MemberServiceTest#editingProfileWithoutStatusTransitionDoesNotDisableOrActivateAccount` |
| TC-F2.4.1-01e | AC-F2.4.1-01 | US-F2.4.1 | UC-F2.4 | BR-F2-06, BR-F2-10, BR-F2-08, BR-F2-09 | `features/members/components/MemberFormModal.test.tsx › TC-F2.4.1-01e: sửa hồ sơ gửi đầy đủ trạng thái và giữ giá tr` |
| TC-F2.4.1-02a | AC-F2.4.1-02 | US-F2.4.1 | UC-F2.4 | BR-F2-06, BR-F2-10, BR-F2-08, BR-F2-09 | `MemberApiIntegrationTest#createAndUpdateRejectTodayOrFutureBirthDateWithoutChanges` |
| TC-F2.4.1-02b | AC-F2.4.1-02 | US-F2.4.1 | UC-F2.4 | BR-F2-06, BR-F2-10, BR-F2-08, BR-F2-09 | `MemberApiIntegrationTest#invalidNamesAreRejectedByRequestValidationOnCreateAndUpdate` |
| TC-F2.4.1-02c | AC-F2.4.1-02 | US-F2.4.1 | UC-F2.4 | BR-F2-06, BR-F2-10, BR-F2-08, BR-F2-09 | `MemberApiIntegrationTest#missingUpdateStatusIsRejectedByRequestValidation` |
| TC-F2.4.1-02d | AC-F2.4.1-02 | US-F2.4.1 | UC-F2.4 | BR-F2-06, BR-F2-10, BR-F2-08, BR-F2-09 | `MemberApiIntegrationTest#incompletePhoneCannotBeSavedOnCreateOrUpdate` |
| TC-F2.4.1-02e | AC-F2.4.1-02 | US-F2.4.1 | UC-F2.4 | BR-F2-06, BR-F2-10, BR-F2-08, BR-F2-09 | `MemberApiIntegrationTest#duplicateUpdateIs409AndUnassignedStaffCannotAccess` |
| TC-F2.4.1-02f | AC-F2.4.1-02 | US-F2.4.1 | UC-F2.4 | BR-F2-06, BR-F2-10, BR-F2-08, BR-F2-09 | `MemberServiceTest#outsideBranchIsNotFoundAndDoesNotModifyAccount` |
| TC-F2.4.1-03a | AC-F2.4.1-03 | US-F2.4.1 | UC-F2.4 | BR-F2-08, BR-F2-09, BR-F2-06, BR-F2-10 | `AccountServiceTest#disableAccountCapNhatDisabledVaThuHoiRefreshToken` |
| TC-F2.4.1-03b | AC-F2.4.1-03 | US-F2.4.1 | UC-F2.4 | BR-F2-08, BR-F2-09, BR-F2-06, BR-F2-10 | `F1RegistrationIntegrationTest#disablingRevokesAllMemberSessionsAndReactivationDoesNotRestoreOldTokens` |
| TC-F2.4.1-03c | AC-F2.4.1-03 | US-F2.4.1 | UC-F2.4 | BR-F2-08, BR-F2-09, BR-F2-06, BR-F2-10 | `F1RegistrationIntegrationTest#failureAfterRealF1DisableRollsBackMemberAccountAndTokenRevocation` |
| TC-F2.4.1-03d | AC-F2.4.1-03 | US-F2.4.1 | UC-F2.4 | BR-F2-08, BR-F2-09, BR-F2-06, BR-F2-10 | `F1RegistrationIntegrationTest#failureAfterSavingMemberRollsBackProfileAccountAndTokenRevocation` |
| TC-F2.4.1-03e | AC-F2.4.1-03 | US-F2.4.1 | UC-F2.4 | BR-F2-08, BR-F2-09, BR-F2-06, BR-F2-10 | `MemberApiIntegrationTest#updateSamePhoneAndDisableRevokeAndActivate` |
| TC-F2.4.1-03f | AC-F2.4.1-03 | US-F2.4.1 | UC-F2.4 | BR-F2-08, BR-F2-09, BR-F2-06, BR-F2-10 | `MemberApiIntegrationTest#failedAccountOperationRollsBackMemberUserAndTokens` |
| TC-F2.4.1-03g | AC-F2.4.1-03 | US-F2.4.1 | UC-F2.4 | BR-F2-08, BR-F2-09, BR-F2-06, BR-F2-10 | `MemberServiceTest#failedAccountTransitionDoesNotMutateNamePhoneOrStatus` |
| TC-F2.4.1-04a | AC-F2.4.1-04 | US-F2.4.1 | UC-F2.4 | BR-F2-08, BR-F2-06, BR-F2-09, BR-F2-10 | `AccountServiceTest#activateAccountIfDisabledChiMoTaiKhoanDisabledKhongMoLocked` |
| TC-F2.4.1-04b | AC-F2.4.1-04 | US-F2.4.1 | UC-F2.4 | BR-F2-08, BR-F2-06, BR-F2-09, BR-F2-10 | `F1RegistrationIntegrationTest#disablingRevokesAllMemberSessionsAndReactivationDoesNotRestoreOldTokens` |
| TC-F2.4.1-04c | AC-F2.4.1-04 | US-F2.4.1 | UC-F2.4 | BR-F2-08, BR-F2-06, BR-F2-09, BR-F2-10 | `F1RegistrationIntegrationTest#activatingMemberDoesNotUnlockCurrentlyLockedAccount` |
| TC-F2.4.1-04d | AC-F2.4.1-04 | US-F2.4.1 | UC-F2.4 | BR-F2-08, BR-F2-06, BR-F2-09, BR-F2-10 | `F1RegistrationIntegrationTest#failureAfterRealF1ActivationRollsBackMemberAndAccount` |
| TC-F2.4.1-04e | AC-F2.4.1-04 | US-F2.4.1 | UC-F2.4 | BR-F2-08, BR-F2-06, BR-F2-09, BR-F2-10 | `MemberApiIntegrationTest#updateSamePhoneAndDisableRevokeAndActivate` |
| TC-F2.4.1-05a | AC-F2.4.1-05 | US-F2.4.1 | UC-F2.4 | BR-F2-08, BR-F2-06, BR-F2-09, BR-F2-10 | `MemberApiIntegrationTest#concurrentUpdatesKeepMemberAndAccountStatusesConsistent` |
| TC-F2.4.2-01a | AC-F2.4.2-01 | US-F2.4.2 | UC-F2.4 | BR-F2-01, BR-F2-02, BR-F2-03, BR-F2-04 | `MemberAvatarIntegrationTest#validImagesAreResizedAndStored` |
| TC-F2.4.2-01b | AC-F2.4.2-01 | US-F2.4.2 | UC-F2.4 | BR-F2-01, BR-F2-02, BR-F2-03, BR-F2-04 | `MemberAvatarIntegrationTest#smallTransparentImagesKeepDimensions` |
| TC-F2.4.2-01c | AC-F2.4.2-01 | US-F2.4.2 | UC-F2.4 | BR-F2-01, BR-F2-02, BR-F2-03, BR-F2-04 | `MemberAvatarIntegrationTest#acceptsDimensionBoundary` |
| TC-F2.4.2-01d | AC-F2.4.2-01 | US-F2.4.2 | UC-F2.4 | BR-F2-01, BR-F2-02, BR-F2-03, BR-F2-04 | `MemberAvatarIntegrationTest#acceptsUploadSizeBoundary` |
| TC-F2.4.2-01e | AC-F2.4.2-01 | US-F2.4.2 | UC-F2.4 | BR-F2-01, BR-F2-02, BR-F2-03, BR-F2-04 | `features/members/components/MemberFormModal.test.tsx › TC-F2.4.2-01e: chọn ảnh JPEG/PNG hợp lệ gọi API tải lên và t` |
| TC-F2.4.2-02a | AC-F2.4.2-02 | US-F2.4.2 | UC-F2.4 | BR-F2-03, BR-F2-01, BR-F2-02, BR-F2-04 | `MemberAvatarIntegrationTest#overwritesSameRow` |
| TC-F2.4.2-02b | AC-F2.4.2-02 | US-F2.4.2 | UC-F2.4 | BR-F2-03, BR-F2-01, BR-F2-02, BR-F2-04 | `MemberAvatarIntegrationTest#concurrentInitialUploadsAreSerialized` |
| TC-F2.4.2-02c | AC-F2.4.2-02 | US-F2.4.2 | UC-F2.4 | BR-F2-03, BR-F2-01, BR-F2-02, BR-F2-04 | `MemberAvatarIntegrationTest#invalidFilesDoNotReplaceExistingAvatar` |
| TC-F2.4.2-03a | AC-F2.4.2-03 | US-F2.4.2 | UC-F2.4 | BR-F2-01, BR-F2-02, BR-F2-03, BR-F2-04 | `MemberAvatarIntegrationTest#invalidFilesDoNotReplaceExistingAvatar` |
| TC-F2.4.2-03b | AC-F2.4.2-03 | US-F2.4.2 | UC-F2.4 | BR-F2-01, BR-F2-02, BR-F2-03, BR-F2-04 | `MemberAvatarIntegrationTest#servletRejectsOversizedMultipart` |
| TC-F2.4.2-03c | AC-F2.4.2-03 | US-F2.4.2 | UC-F2.4 | BR-F2-01, BR-F2-02, BR-F2-03, BR-F2-04 | `features/members/components/MemberFormModal.test.tsx › TC-F2.4.2-03c: báo lỗi sớm với file sai định dạng hoặc vượt ` |
| TC-F2.4.2-03d | AC-F2.4.2-03 | US-F2.4.2 | UC-F2.4 | BR-F2-01, BR-F2-02, BR-F2-03, BR-F2-04 | `features/members/components/MemberFormModal.test.tsx › TC-F2.4.2-03d: lỗi 400 từ API hiển thị đúng câu errors.file ` |
| TC-F2.4.2-04a | AC-F2.4.2-04 | US-F2.4.2 | UC-F2.4 | BR-F2-04, BR-F2-01, BR-F2-02, BR-F2-03 | `MemberAvatarIntegrationTest#authorizedReadersReceiveStoredJpeg` |
| TC-F2.4.2-04b | AC-F2.4.2-04 | US-F2.4.2 | UC-F2.4 | BR-F2-04, BR-F2-01, BR-F2-02, BR-F2-03 | `MemberAvatarIntegrationTest#otherReadersCannotSeeAvatar` |
| TC-F2.4.2-05a | AC-F2.4.2-05 | US-F2.4.2 | UC-F2.4 | BR-F2-04, BR-F2-01, BR-F2-02, BR-F2-03 | `MemberAvatarIntegrationTest#ownerMayUploadAcrossBranches` |
| TC-F2.4.2-05b | AC-F2.4.2-05 | US-F2.4.2 | UC-F2.4 | BR-F2-04, BR-F2-01, BR-F2-02, BR-F2-03 | `MemberAvatarIntegrationTest#deniedWritersDoNotMutateAvatar` |
| TC-F2.4.2-05c | AC-F2.4.2-05 | US-F2.4.2 | UC-F2.4 | BR-F2-04, BR-F2-01, BR-F2-02, BR-F2-03 | `MemberAvatarIntegrationTest#inactiveAssignmentsAreDenied` |
| TC-F2.4.2-05d | AC-F2.4.2-05 | US-F2.4.2 | UC-F2.4 | BR-F2-04, BR-F2-01, BR-F2-02, BR-F2-03 | `features/members/components/MemberFormModal.test.tsx › TC-F2.4.2-05d: hội viên không thấy nút đổi ảnh` |
| TC-F2.4.2-06a | AC-F2.4.2-06 | US-F2.4.2 | UC-F2.4 | BR-F2-04, BR-F2-01, BR-F2-02, BR-F2-03 | `MemberAvatarIntegrationTest#missingAvatarIsNotFound` |
| TC-F2.4.2-06b | AC-F2.4.2-06 | US-F2.4.2 | UC-F2.4 | BR-F2-04, BR-F2-01, BR-F2-02, BR-F2-03 | `MemberAvatarIntegrationTest#missingOrDeletedProfilesAreNotFound` |
| TC-F2.4.2-07a | AC-F2.4.2-07 | US-F2.4.2 | UC-F2.4 | BR-F2-01, BR-F2-02, BR-F2-03, BR-F2-04 | `MemberAvatarIntegrationTest#unauthenticatedRequestsAreDenied` |
| TC-F2.4.2-07b | AC-F2.4.2-07 | US-F2.4.2 | UC-F2.4 | BR-F2-01, BR-F2-02, BR-F2-03, BR-F2-04 | `MemberAvatarIntegrationTest#invalidIdsAreRejected` |
| TC-F2.4.2-07c | AC-F2.4.2-07 | US-F2.4.2 | UC-F2.4 | BR-F2-01, BR-F2-02, BR-F2-03, BR-F2-04 | `MemberAvatarIntegrationTest#missingMultipartFileIsRejected` |
| TC-F2.4.2-08a | AC-F2.4.2-08 | US-F2.4.2 | UC-F2.4 | BR-F2-04, BR-F2-01, BR-F2-02, BR-F2-03 | `MemberAvatarIntegrationTest#newProfileHasNoAvatar` |
| TC-F2.4.2-08a | AC-F2.4.2-08 | US-F2.4.2 | UC-F2.4 | BR-F2-04, BR-F2-01, BR-F2-02, BR-F2-03 | `features/members/components/MemberAvatar.test.tsx › TC-F2.4.2-08a: hiện chữ cái đầu của tên khi hasAvatar là fal` |
| TC-F2.4.2-08b | AC-F2.4.2-08 | US-F2.4.2 | UC-F2.4 | BR-F2-04, BR-F2-01, BR-F2-02, BR-F2-03 | `MemberAvatarIntegrationTest#profileResponsesReflectAvatarPresence` |
| TC-F2.4.2-08b | AC-F2.4.2-08 | US-F2.4.2 | UC-F2.4 | BR-F2-04, BR-F2-01, BR-F2-02, BR-F2-03 | `features/members/components/MemberFormModal.test.tsx › TC-F2.4.2-08b: mở hồ sơ chưa có ảnh thì hiện chữ cái đầu và ` |
| TC-F3.1.1-01a | AC-F3.1.1-01 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#scopesRowsAndCountAndIgnoresClientBranch` |
| TC-F3.1.1-02a | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#defaultActiveAndOrderingMatchContract` |
| TC-F3.1.1-02b | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#inactiveFilterAndEmptyResults` |
| TC-F3.1.1-02c | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#maximumSizeAndPagePastEndAreValid` |
| TC-F3.1.1-02d | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageControllerTest#defaultContract` |
| TC-F3.1.1-02e | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `features/packages/pages/PackagesPage.test.tsx › TC-F3.1.1-02e: mặc định lấy gói đang bán; chọn loại PT giữ n` |
| TC-F3.1.1-02f | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `features/packages/api/packages.test.ts › TC-F3.1.1-02f: gửi bộ lọc và phân trang đúng endpoint đã đặc` |
| TC-F3.1.1-03a | AC-F3.1.1-03 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `features/packages/pages/PackagesPage.test.tsx › TC-F3.1.1-03a: Chỉ quản lý chi nhánh thấy nút thêm, sửa, ngừ` |
| TC-F3.1.1-04a | AC-F3.1.1-04 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#ownerSeesOtherBranchesWithoutStaff` |
| TC-F3.1.1-04b | AC-F3.1.1-04 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogServiceTest#ownerDoesNotRequireStaffAssignment` |
| TC-F3.1.1-05a | AC-F3.1.1-05 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#inactiveOrMissingStaffIsForbidden` |
| TC-F3.1.1-05b | AC-F3.1.1-05 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageControllerTest#rejectsDeniedRole` |
| TC-F3.1.1-05c | AC-F3.1.1-05 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageControllerTest#missingAndInvalidBearerAreUnauthorized` |
| TC-F3.1.1-05d | AC-F3.1.1-05 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogServiceTest#missingAssignmentNeverFallsBackToAllBranches` |
| TC-F3.1.1-06a | AC-F3.1.1-06 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageControllerTest#rejectsInvalidQueryWithFieldErrors` |
| TC-F3.1.2-01a | AC-F3.1.2-01 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `PublicPackageIntegrationTest#guestSeesActiveBranchAndGlobalPackagesInDisplayOrder` |
| TC-F3.1.2-01b | AC-F3.1.2-01 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-01b: hiện giá từ server, thời hạn hoặc số buổi, qu` |
| TC-F3.1.2-01c | AC-F3.1.2-01 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-01c: gọi API công khai không kèm access token và k` |
| TC-F3.1.2-01d | AC-F3.1.2-01 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `PublicPackageIntegrationTest#staleOrForgedTokenIsIgnoredOnPublicRead` |
| TC-F3.1.2-02a | AC-F3.1.2-02 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `PublicPackageIntegrationTest#hidesInactiveAndOtherBranchPackages` |
| TC-F3.1.2-03a | AC-F3.1.2-03 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `PublicPackageIntegrationTest#exposesOnlyDisplayFields` |
| TC-F3.1.2-04a | AC-F3.1.2-04 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `PublicPackageIntegrationTest#emptyCatalogReturnsEmptyList` |
| TC-F3.1.2-04b | AC-F3.1.2-04 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-04b: chưa có gói đang bán thì báo trống, không hiệ` |
| TC-F3.1.2-05a | AC-F3.1.2-05 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-05a: lỗi tải thì báo lỗi, không hiện giá; bấm Thử ` |
| TC-F3.1.2-06a | AC-F3.1.2-06 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `PublicPackageIntegrationTest#inactiveDefaultBranchReturnsEmptyList` |
| TC-F3.2.1-01a | AC-F3.2.1-01 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#createsAllTypesWithServerBranchAuditAndReadVisibility` |
| TC-F3.2.1-01b | AC-F3.2.1-01 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `features/packages/components/PackageFormModal.test.tsx › TC-F3.2.1-01b: Thêm gói TIME hợp lệ gửi đúng DTO không chứa ` |
| TC-F3.2.1-01c | AC-F3.2.1-01 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `features/packages/pages/PackagesPage.test.tsx › TC-F3.2.1-01c: Tạo gói mới từ trang danh mục đóng hộp thoại ` |
| TC-F3.2.1-01d | AC-F3.2.1-01 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `features/packages/components/PackageFormModal.test.tsx › TC-F3.2.1-01d: Thêm gói SESSION hợp lệ gửi đúng DTO có số bu` |
| TC-F3.2.1-01e | AC-F3.2.1-01 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `features/packages/components/PackageFormModal.test.tsx › TC-F3.2.1-01e: Thêm gói PT hợp lệ gửi đúng DTO có số buổi và` |
| TC-F3.2.1-02a | AC-F3.2.1-02 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#sessionsAreRequiredAndPositive` |
| TC-F3.2.1-02b | AC-F3.2.1-02 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `features/packages/components/PackageFormModal.test.tsx › TC-F3.2.1-02b: Gói TIME ẩn ô số buổi và không gửi sessionCou` |
| TC-F3.2.1-03a | AC-F3.2.1-03 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#invalidInputsReturnFieldErrorsWithoutWrites` |
| TC-F3.2.1-03b | AC-F3.2.1-03 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#missingFieldsMalformedJsonAndLengthLimitsAreRejected` |
| TC-F3.2.1-03c | AC-F3.2.1-03 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#acceptsZeroPriceAndMaximumLengthsAndIntegers` |
| TC-F3.2.1-03d | AC-F3.2.1-03 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `features/packages/components/PackageFormModal.test.tsx › TC-F3.2.1-03d: Kiểm tra đầu vào, cho phép giá 0 đồng và gán ` |
| TC-F3.2.1-04a | AC-F3.2.1-04 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#rejectsServerManagedAndUnknownFields` |
| TC-F3.2.1-05a | AC-F3.2.1-05 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#duplicateInactiveGlobalCodeIsConflict` |
| TC-F3.2.1-05b | AC-F3.2.1-05 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#concurrentDuplicateCodeCreatesOneAndRollsBackOther` |
| TC-F3.2.1-05c | AC-F3.2.1-05 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `features/packages/components/PackageFormModal.test.tsx › TC-F3.2.1-05c: Lỗi 409 trùng mã gói hiển thị thông báo tiếng` |
| TC-F3.2.1-06a | AC-F3.2.1-06 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#onlyManagerCanCreate` |
| TC-F3.2.1-06b | AC-F3.2.1-06 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#missingTokenOrInactiveStaffCannotCreate` |
| TC-F3.2.1-06d | AC-F3.2.1-06 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `features/packages/components/PackageFormModal.test.tsx › TC-F3.2.1-06d: Lỗi 403 khi tạo gói hiển thị thông báo tiếng ` |
| TC-F3.3.1-01a | AC-F3.3.1-01 | US-F3.3.1 | UC-F3.3 | BR-F3-06, BR-F3-05, BR-F3-07 | `PackageUpdateIntegrationTest#priceOnlyKeepsOmittedFieldsAndAudit` |
| TC-F3.3.1-01b | AC-F3.3.1-01 | US-F3.3.1 | UC-F3.3 | BR-F3-06, BR-F3-05, BR-F3-07 | `PackageUpdateIntegrationTest#historyIncludingPendingPurchaseRemainsIdentical` |
| TC-F3.3.1-01c | AC-F3.3.1-01 | US-F3.3.1 | UC-F3.3 | BR-F3-06, BR-F3-05, BR-F3-07 | `features/packages/components/PackageFormModal.test.tsx › TC-F3.3.1-01c, TC-F3.3.1-04c: Sửa gói giữ nguyên và không gử` |
| TC-F3.3.1-02a | AC-F3.3.1-02 | US-F3.3.1 | UC-F3.3 | BR-F3-07, BR-F3-05, BR-F3-06 | `PackageUpdateIntegrationTest#outsideBranchAndGlobalAre403WithoutMutation` |
| TC-F3.3.1-03a | AC-F3.3.1-03 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#explicitNullClearsDescriptionAndEmptyBodyDoesNotChangeAudit` |
| TC-F3.3.1-03b | AC-F3.3.1-03 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `features/packages/components/PackageFormModal.test.tsx › TC-F3.3.1-03b: Xóa trống mô tả khi sửa gửi description là nu` |
| TC-F3.3.1-04a | AC-F3.3.1-04 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-02, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#refusesImmutableAndUnknownFields` |
| TC-F3.3.1-04b | AC-F3.3.1-04 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-02, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#sessionsMergeWithExistingTypeAndCannotBeCleared` |
| TC-F3.3.1-04c | AC-F3.3.1-04 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-02, BR-F3-06, BR-F3-07 | `features/packages/components/PackageFormModal.test.tsx › TC-F3.3.1-01c, TC-F3.3.1-04c: Sửa gói giữ nguyên và không gử` |
| TC-F3.3.1-05a | AC-F3.3.1-05 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#invalidUpdateIsAtomicAndReturnsFieldError` |
| TC-F3.3.1-05b | AC-F3.3.1-05 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#invalidIdIsBadRequest` |
| TC-F3.3.1-05c | AC-F3.3.1-05 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#malformedBodyIsRejected` |
| TC-F3.3.1-05d | AC-F3.3.1-05 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `features/packages/components/PackageFormModal.test.tsx › TC-F3.3.1-05d, TC-F3.3.1-07b: Lỗi 400 theo trường hoặc 404 k` |
| TC-F3.3.1-06a | AC-F3.3.1-06 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#acceptsBoundariesAndCanEditInactiveWithoutReactivation` |
| TC-F3.3.1-06b | AC-F3.3.1-06 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `features/packages/pages/PackagesPage.test.tsx › TC-F3.3.1-06b: Gói ngừng bán vẫn hiện nút Sửa cho quản lý ch` |
| TC-F3.3.1-07a | AC-F3.3.1-07 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#missingPackageIs404WithoutMutation` |
| TC-F3.3.1-07b | AC-F3.3.1-07 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `features/packages/components/PackageFormModal.test.tsx › TC-F3.3.1-05d, TC-F3.3.1-07b: Lỗi 400 theo trường hoặc 404 k` |
| TC-F3.3.1-08a | AC-F3.3.1-08 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#deniedRolesCannotUpdate` |
| TC-F3.3.1-08b | AC-F3.3.1-08 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#noTokenOrInactiveMissingStaffCannotUpdate` |
| TC-F3.4.1-01a | AC-F3.4.1-01 | US-F3.4.1 | UC-F3.4 | BR-F3-08, BR-F3-07, BR-F3-09 | `PackageDeactivateIntegrationTest#deactivatesWithNoBodyOrEmptyJsonWithoutChangingOtherFields` |
| TC-F3.4.1-01b | AC-F3.4.1-01 | US-F3.4.1 | UC-F3.4 | BR-F3-08, BR-F3-07, BR-F3-09 | `features/packages/pages/PackagesPage.test.tsx › TC-F3.4.1-01b, TC-F3.4.1-03b: Xác nhận ngừng bán gói ACTIVE ` |
| TC-F3.4.1-02a | AC-F3.4.1-02 | US-F3.4.1 | UC-F3.4 | BR-F3-08, BR-F3-07, BR-F3-09 | `PackageDeactivateIntegrationTest#preservesAllHistoricalRows` |
| TC-F3.4.1-03a | AC-F3.4.1-03 | US-F3.4.1 | UC-F3.4 | BR-F3-09, BR-F3-07, BR-F3-08 | `PackageCatalogIntegrationTest#inactiveFilterAndEmptyResults` |
| TC-F3.4.1-03b | AC-F3.4.1-03 | US-F3.4.1 | UC-F3.4 | BR-F3-09, BR-F3-07, BR-F3-08 | `features/packages/pages/PackagesPage.test.tsx › TC-F3.4.1-01b, TC-F3.4.1-03b: Xác nhận ngừng bán gói ACTIVE ` |
| TC-F3.4.1-04a | AC-F3.4.1-04 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#repeatIs422WithoutFurtherAuditChange` |
| TC-F3.4.1-04b | AC-F3.4.1-04 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#simultaneousDeactivationHasOneSuccessAndOne422` |
| TC-F3.4.1-04c | AC-F3.4.1-04 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `features/packages/pages/PackagesPage.test.tsx › TC-F3.4.1-04c, TC-F3.4.1-06h: Lỗi 422, 403, 404 khi ngừng bá` |
| TC-F3.4.1-05a | AC-F3.4.1-05 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#concurrentEditCannotReactivatePackage` |
| TC-F3.4.1-06a | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#outsideBranchAndGlobalAre403EvenIfAlreadyInactive` |
| TC-F3.4.1-06b | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#deniedRolesCannotDeactivate` |
| TC-F3.4.1-06c | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#invalidIdIs400` |
| TC-F3.4.1-06d | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#missingAndBadTokenAre401` |
| TC-F3.4.1-06e | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#inactiveAndMissingStaffAre403` |
| TC-F3.4.1-06f | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#missingPackageIs404WithEmptyJsonBodyWithoutMutation` |
| TC-F3.4.1-06h | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `features/packages/pages/PackagesPage.test.tsx › TC-F3.4.1-04c, TC-F3.4.1-06h: Lỗi 422, 403, 404 khi ngừng bá` |
| TC-F3.5.1-01a | AC-F3.5.1-01 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-13, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-14 | `InvoiceSaleIntegrationTest#sellCreatesPendingInvoiceAndMembershipWithServerPrice` |
| TC-F3.5.1-01b | AC-F3.5.1-01 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-13, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-14 | `InvoiceSaleIntegrationTest#sessionPackageCopiesSessionCount` |
| TC-F3.5.1-01c | AC-F3.5.1-01 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-13, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-14 | `InvoiceSaleIntegrationTest#membershipFailureRollsBackEntireSale` |
| TC-F3.5.1-01d | AC-F3.5.1-01 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-13, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-14 | `features/packages/api/package-sales.test.ts › TC-F3.5.1-01d: livePackageSalesApi gọi đúng GET /memberships` |
| TC-F3.5.1-01e | AC-F3.5.1-01 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-13, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-14 | `features/packages/pages/SellPackagePage.test.tsx › TC-F3.5.1-01e: Chọn hội viên và gói, chỉ gửi memberId và ite` |
| TC-F3.5.1-02a | AC-F3.5.1-02 | US-F3.5.1 | UC-F3.5 | BR-F3-12, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-13, BR-F3-14 | `MembershipListIntegrationTest#pendingSaleIsNotUsableBeforePayment` |
| TC-F3.5.1-02b | AC-F3.5.1-02 | US-F3.5.1 | UC-F3.5 | BR-F3-12, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-13, BR-F3-14 | `InvoicePaymentIntegrationTest#payActivatesWithDatesRecomputedAtPaymentTime` |
| TC-F3.5.1-03a | AC-F3.5.1-03 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#renewalStartsDayAfterOldTimePackage` |
| TC-F3.5.1-03b | AC-F3.5.1-03 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipServiceTest#newOrTimeRenewal` |
| TC-F3.5.1-03c | AC-F3.5.1-03 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoicePaymentIntegrationTest#payRenewalAfterActiveTimePackageIsUpcoming` |
| TC-F3.5.1-03c | AC-F3.5.1-03 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipServiceTest#ptRenewalWithSessionsLeft` |
| TC-F3.5.1-03d | AC-F3.5.1-03 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `features/packages/pages/SellPackagePage.test.tsx › TC-F3.5.1-03d: Hội viên có gói TIME ACTIVE cùng phạm vi thì ` |
| TC-F3.5.1-04a | AC-F3.5.1-04 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#renewalAfterEmptySessionPackageStartsTomorrow` |
| TC-F3.5.1-04b | AC-F3.5.1-04 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#renewalAfterSessionPackageWithSessionsLeftStartsAfterEndDate` |
| TC-F3.5.1-04c | AC-F3.5.1-04 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipServiceTest#sessionRenewal` |
| TC-F3.5.1-04d | AC-F3.5.1-04 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoicePaymentIntegrationTest#payRenewalAfterEmptySessionPackageStartsTomorrow` |
| TC-F3.5.1-04d | AC-F3.5.1-04 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipServiceTest#emptyPtDoesNotUseSessionTomorrowRule` |
| TC-F3.5.1-04e | AC-F3.5.1-04 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `features/packages/pages/SellPackagePage.test.tsx › TC-F3.5.1-04e: Hội viên có gói SESSION đã hết buổi hiển thị ` |
| TC-F3.5.1-05a | AC-F3.5.1-05 | US-F3.5.1 | UC-F3.5 | BR-F3-10, BR-F3-04, BR-F3-09, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#differentPackageTypesKeepExistingMembershipIntact` |
| TC-F3.5.1-05b | AC-F3.5.1-05 | US-F3.5.1 | UC-F3.5 | BR-F3-10, BR-F3-04, BR-F3-09, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoicePaymentIntegrationTest#payPtWhileHoldingTimeActivatesPt` |
| TC-F3.5.1-06a | AC-F3.5.1-06 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#secondWaitingPackageOfSameTypeIsRejected` |
| TC-F3.5.1-06b | AC-F3.5.1-06 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#pendingWithoutActiveAlsoBlocks` |
| TC-F3.5.1-06c | AC-F3.5.1-06 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#waitingPackageOfOtherTypeDoesNotBlock` |
| TC-F3.5.1-07a | AC-F3.5.1-07 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleConcurrencyIntegrationTest#twoReceptionistsSellingAtOnceCreateExactlyOne` |
| TC-F3.5.1-08a | AC-F3.5.1-08 | US-F3.5.1 | UC-F3.5 | BR-F3-09, BR-F3-04, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#inactiveMemberIsRejected` |
| TC-F3.5.1-08b | AC-F3.5.1-08 | US-F3.5.1 | UC-F3.5 | BR-F3-09, BR-F3-04, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#inactivePackageIsRejected` |
| TC-F3.5.1-08c | AC-F3.5.1-08 | US-F3.5.1 | UC-F3.5 | BR-F3-09, BR-F3-04, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#otherBranchPackageIsRejected` |
| TC-F3.5.1-08d | AC-F3.5.1-08 | US-F3.5.1 | UC-F3.5 | BR-F3-09, BR-F3-04, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#unknownMemberOrPackageIsNotFound` |
| TC-F3.5.1-08e | AC-F3.5.1-08 | US-F3.5.1 | UC-F3.5 | BR-F3-09, BR-F3-04, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#memberOfOtherBranchIsHidden` |
| TC-F3.5.1-08f | AC-F3.5.1-08 | US-F3.5.1 | UC-F3.5 | BR-F3-09, BR-F3-04, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleConcurrencyIntegrationTest#saleWaitsForCatalogChangeAndReadsCommittedValues` |
| TC-F3.5.1-08g | AC-F3.5.1-08 | US-F3.5.1 | UC-F3.5 | BR-F3-09, BR-F3-04, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `features/packages/pages/SellPackagePage.test.tsx › TC-F3.5.1-08g: Lỗi 422 hoặc 404 khi hội viên ngừng hoạt động` |
| TC-F3.5.1-09a | AC-F3.5.1-09 | US-F3.5.1 | UC-F3.5 | BR-F3-14, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13 | `PendingInvoiceExpiryIntegrationTest#counterInvoiceIsNeverAutoCancelled` |
| TC-F3.5.1-10a | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#expiresOldBeforeActivatingNext` |
| TC-F3.5.1-10b | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#lastDeductionTodayRetainsAccess` |
| TC-F3.5.1-10c | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#zeroSessionsFromYesterdayExpire` |
| TC-F3.5.1-10d | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#repeatedRunDoesNotMutateAgain` |
| TC-F3.5.1-10e | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#catchesUpAfterSeveralMissedDays` |
| TC-F3.5.1-10f | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#endDateIsInclusive` |
| TC-F3.5.1-10g | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#ignoresOtherStates` |
| TC-F3.5.1-10h | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#frozenBlocksWithoutChangingWaitingPackage` |
| TC-F3.5.1-10i | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#chainAndBranchAreIndependentGroups` |
| TC-F3.5.1-10j | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#expiredPtKeepsRemainingSessions` |
| TC-F3.5.1-10k | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#activationFailureRollsBackExpiration` |
| TC-F3.5.1-10l | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleJobTest#annotations` |
| TC-F3.5.1-10m | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleJobTest#passesVietnamDate` |
| TC-F3.5.1-10n | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleJobTest#catchUpGoesThroughScheduler` |
| TC-F3.5.1-10o | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleJobTest#retriesEveryFiveMinutesUntilSuccess` |
| TC-F3.5.1-10p | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleJobTest#stopsRetryingOnNewDay` |
| TC-F3.5.1-11a | AC-F3.5.1-11 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#invalidBodyIsBadRequest` |
| TC-F3.5.1-11b | AC-F3.5.1-11 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#otherRolesAreForbidden` |
| TC-F3.5.1-11c | AC-F3.5.1-11 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#missingTokenIsUnauthorized` |
| TC-F3.5.1-11d | AC-F3.5.1-11 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#missingFieldsAreNamed` |
| TC-F3.5.1-12a | AC-F3.5.1-12 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipListIntegrationTest#listsHeldMembershipsWithRenewalStartDate` |
| TC-F3.5.1-12b | AC-F3.5.1-12 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipListIntegrationTest#renewalStartDateMatchesSale` |
| TC-F3.5.1-12c | AC-F3.5.1-12 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipListIntegrationTest#scopeAndRoles` |
| TC-F3.5.1-12d | AC-F3.5.1-12 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipListIntegrationTest#invalidMemberIdIsRejected` |
| TC-F3.5.1-12e | AC-F3.5.1-12 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipServiceTest#neverBeforeToday` |
| TC-F3.5.1-13a | AC-F3.5.1-13 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#invoiceCodesAreSequential` |
| TC-F3.5.1-13b | AC-F3.5.1-13 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceCodeMigrationIntegrationTest#upgradesExistingDatabaseWithoutChangingInvoiceHistory` |
| TC-F5.1.1-01a | AC-F5.1.1-01 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `InvoiceDocumentIntegrationTest#vietQrForPendingInvoiceUsesServerTotalAndEncodesParams` |
| TC-F5.1.1-01b | AC-F5.1.1-01 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `features/invoices/api/invoices.test.ts › TC-F5.1.1-01b: vietqr gọi GET /invoices/{id}/vietqr` |
| TC-F5.1.1-01c | AC-F5.1.1-01 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.1.1-01c: chọn chuyển khoản gọi VietQR, hiện ảnh QR và ` |
| TC-F5.1.1-01d | AC-F5.1.1-01 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.1.1-01d: ảnh QR lỗi vẫn giữ thông tin chuyển khoản và ` |
| TC-F5.1.1-01e | AC-F5.1.1-01 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.1.1-01e: phản hồi QR cũ không ghi đè yêu cầu tải QR mớ` |
| TC-F5.1.1-01f | AC-F5.1.1-01 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.1.1-01f: thiếu Clipboard API không được báo sao chép t` |
| TC-F5.1.1-02a | AC-F5.1.1-02 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `InvoiceDocumentIntegrationTest#vietQrForNonPendingInvoiceIs422` |
| TC-F5.1.1-03a | AC-F5.1.1-03 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `InvoiceDocumentIntegrationTest#vietQrBranchBankConfigOverridesChainConfig` |
| TC-F5.1.1-03b | AC-F5.1.1-03 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `InvoiceDocumentIntegrationTest#vietQrFallsBackForEachMissingBranchKey` |
| TC-F5.1.1-04a | AC-F5.1.1-04 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `InvoiceDocumentIntegrationTest#vietQrSecurityAndScope` |
| TC-F5.1.1-05a | AC-F5.1.1-05 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `InvoiceDocumentIntegrationTest#vietQrSecurityAndScope` |
| TC-F5.1.1-06a | AC-F5.1.1-06 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `InvoiceDocumentIntegrationTest#vietQrMissingBankConfigIs422` |
| TC-F5.1.1-06b | AC-F5.1.1-06 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `InvoiceDocumentIntegrationTest#blankBranchBankConfigIs422` |
| TC-F5.1.1-06c | AC-F5.1.1-06 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.1.1-06c: lỗi 422 khi tải VietQR hiện đúng câu detail c` |
| TC-F5.1.1-07a | AC-F5.1.1-07 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `InvoiceDocumentIntegrationTest#walkInPaidInvoicePdfAndInvalidId` |
| TC-F5.1.1-07b | AC-F5.1.1-07 | US-F5.1.1 | UC-F5.1 | BR-F5-01, BR-F5-02, BR-F5-07 | `InvoiceDocumentIntegrationTest#invalidDocumentIdsReturnFieldErrors` |
| TC-F5.1.1-08a | AC-F5.1.1-08 | US-F5.1.1 | UC-F5.1 | BR-F5-07, BR-F5-01, BR-F5-02 | `InvoiceExpiryIntegrationTest#paymentAndVietQrRespectPortalDeadline` |
| TC-F5.1.1-08b | AC-F5.1.1-08 | US-F5.1.1 | UC-F5.1 | BR-F5-07, BR-F5-01, BR-F5-02 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.1.1-08b: hóa đơn portal quá hạn hiện lỗi VietQR và khô` |
| TC-F5.2.1-01a | AC-F5.2.1-01 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F3-06, BR-F5-05, BR-F5-07, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#payActivatesWithDatesRecomputedAtPaymentTime` |
| TC-F5.2.1-01b | AC-F5.2.1-01 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F3-06, BR-F5-05, BR-F5-07, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#payPtWhileHoldingTimeActivatesPt` |
| TC-F5.2.1-01c | AC-F5.2.1-01 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F3-06, BR-F5-05, BR-F5-07, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#paysPortalInvoiceAtCounter` |
| TC-F5.2.1-01d | AC-F5.2.1-01 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F3-06, BR-F5-05, BR-F5-07, BR-F3-11, BR-F3-12 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-01d: xác nhận thu tiền mặt gửi method CASH và khôn` |
| TC-F5.2.1-01e | AC-F5.2.1-01 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F3-06, BR-F5-05, BR-F5-07, BR-F3-11, BR-F3-12 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-01e: hiển thị ACTIVE từ server sau thu tiền` |
| TC-F5.2.1-01f | AC-F5.2.1-01 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F3-06, BR-F5-05, BR-F5-07, BR-F3-11, BR-F3-12 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-01f: hóa đơn nguồn %s vẫn thu được tại quầy` |
| TC-F5.2.1-02a | AC-F5.2.1-02 | US-F5.2.1 | UC-F5.2 | BR-F5-04, BR-F3-11, BR-F5-03, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-12 | `InvoicePaymentIntegrationTest#payRenewalAfterActiveTimePackageIsUpcoming` |
| TC-F5.2.1-02b | AC-F5.2.1-02 | US-F5.2.1 | UC-F5.2 | BR-F5-04, BR-F3-11, BR-F5-03, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-12 | `InvoicePaymentIntegrationTest#payRenewalAfterFrozenPackageIsUpcoming` |
| TC-F5.2.1-02c | AC-F5.2.1-02 | US-F5.2.1 | UC-F5.2 | BR-F5-04, BR-F3-11, BR-F5-03, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-12 | `InvoicePaymentIntegrationTest#payRenewalAfterEmptySessionPackageStartsTomorrow` |
| TC-F5.2.1-02d | AC-F5.2.1-02 | US-F5.2.1 | UC-F5.2 | BR-F5-04, BR-F3-11, BR-F5-03, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-12 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-02d: hiển thị trạng thái UPCOMING và ngày hiệu lực` |
| TC-F5.2.1-03a | AC-F5.2.1-03 | US-F5.2.1 | UC-F5.2 | BR-F5-04, BR-F5-03, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#zeroTotalInvoiceActivatesWithZeroPaymentRow` |
| TC-F5.2.1-03b | AC-F5.2.1-03 | US-F5.2.1 | UC-F5.2 | BR-F5-04, BR-F5-03, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `PaymentAmountMigrationIntegrationTest#upgradesExistingPaymentsWithoutChangingHistory` |
| TC-F5.2.1-03c | AC-F5.2.1-03 | US-F5.2.1 | UC-F5.2 | BR-F5-04, BR-F5-03, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-03c: cho phép xác nhận hóa đơn 0 đồng và hiển thị ` |
| TC-F5.2.1-04a | AC-F5.2.1-04 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#branchManagerCanPay` |
| TC-F5.2.1-04b | AC-F5.2.1-04 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#otherRolesAreForbidden` |
| TC-F5.2.1-04c | AC-F5.2.1-04 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#noTokenIs401` |
| TC-F5.2.1-04d | AC-F5.2.1-04 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-04d: %s không thấy chức năng thu tiền hoặc VietQR` |
| TC-F5.2.1-05a | AC-F5.2.1-05 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#invalidMethodIs400` |
| TC-F5.2.1-05b | AC-F5.2.1-05 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#tooLongTransactionRefIs400` |
| TC-F5.2.1-05c | AC-F5.2.1-05 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#invalidPaymentIdIs400` |
| TC-F5.2.1-05d | AC-F5.2.1-05 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-05d: lỗi transactionRef 400 được hiển thị ngay dướ` |
| TC-F5.2.1-06a | AC-F5.2.1-06 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#unknownInvoiceIs404` |
| TC-F5.2.1-06b | AC-F5.2.1-06 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#otherBranchIs404` |
| TC-F5.2.1-07a | AC-F5.2.1-07 | US-F5.2.1 | UC-F5.2 | BR-F5-05, BR-F5-03, BR-F5-04, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `InvoicePaymentConcurrencyIntegrationTest#twoReceptionistsPayingSameInvoice` |
| TC-F5.2.1-07b | AC-F5.2.1-07 | US-F5.2.1 | UC-F5.2 | BR-F5-05, BR-F5-03, BR-F5-04, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-07b: lỗi 409 hiện đúng câu detail của API và tải l` |
| TC-F5.2.1-07c | AC-F5.2.1-07 | US-F5.2.1 | UC-F5.2 | BR-F5-05, BR-F5-03, BR-F5-04, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-07c: bấm xác nhận lặp khi đang gửi chỉ gọi pay một` |
| TC-F5.2.1-08a | AC-F5.2.1-08 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#payingTwiceIs422` |
| TC-F5.2.1-08b | AC-F5.2.1-08 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#payingCancelledInvoiceIs422` |
| TC-F5.2.1-08c | AC-F5.2.1-08 | US-F5.2.1 | UC-F5.2 | BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-08c: lỗi 422 hiện đúng câu detail của API` |
| TC-F5.2.1-09a | AC-F5.2.1-09 | US-F5.2.1 | UC-F5.2 | BR-F5-04, BR-F3-11, BR-F5-03, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-12 | `InvoicePaymentConcurrencyIntegrationTest#twoRenewalsPaidAtOnceYieldOneUpcoming` |
| TC-F5.2.1-09b | AC-F5.2.1-09 | US-F5.2.1 | UC-F5.2 | BR-F5-04, BR-F3-11, BR-F5-03, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-12 | `InvoicePaymentIntegrationTest#upcomingSameTypeBlocksActivation` |
| TC-F5.2.1-09c | AC-F5.2.1-09 | US-F5.2.1 | UC-F5.2 | BR-F5-04, BR-F3-11, BR-F5-03, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-12 | `InvoicePaymentIntegrationTest#upcomingOfOtherTypeOrScopeDoesNotBlock` |
| TC-F5.2.1-09d | AC-F5.2.1-09 | US-F5.2.1 | UC-F5.2 | BR-F5-04, BR-F3-11, BR-F5-03, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-12 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-09d: 422 do có UPCOMING tải lại PENDING, không báo` |
| TC-F5.2.1-10a | AC-F5.2.1-10 | US-F5.2.1 | UC-F5.2 | BR-F5-01, BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `InvoicePaymentIntegrationTest#ignoresClientSuppliedAmounts` |
| TC-F5.2.1-10b | AC-F5.2.1-10 | US-F5.2.1 | UC-F5.2 | BR-F5-01, BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `features/invoices/api/invoices.test.ts › TC-F5.2.1-10b: pay gửi đúng method và không gửi số tiền lên ` |
| TC-F5.2.1-10c | AC-F5.2.1-10 | US-F5.2.1 | UC-F5.2 | BR-F5-01, BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-11, BR-F3-12 | `features/invoices/api/invoices.test.ts › TC-F5.2.1-10c: loại bỏ giá trị tiền gửi thêm và chuẩn hóa mã` |
| TC-F5.2.1-11a | AC-F5.2.1-11 | US-F5.2.1 | UC-F5.2 | BR-F3-11, BR-F5-03, BR-F5-04, BR-F5-05, BR-F5-07, BR-F3-06, BR-F3-12 | `InvoicePaymentIntegrationTest#zeroSessionPtStillHeldSchedulesAfterOldEndDate` |
| TC-F5.2.1-12a | AC-F5.2.1-12 | US-F5.2.1 | UC-F5.2 | BR-F5-07, BR-F5-03, BR-F5-04, BR-F5-05, BR-F3-06, BR-F3-11, BR-F3-12 | `InvoiceExpiryIntegrationTest#paymentAndVietQrRespectPortalDeadline` |
| TC-F5.2.1-12b | AC-F5.2.1-12 | US-F5.2.1 | UC-F5.2 | BR-F5-07, BR-F5-03, BR-F5-04, BR-F5-05, BR-F3-06, BR-F3-11, BR-F3-12 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.2.1-12b: hóa đơn portal quá hạn hiện lỗi server và khô` |
| TC-F5.3.1-01a | AC-F5.3.1-01 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#receptionistListsOwnBranchNewestFirstWithPaging` |
| TC-F5.3.1-01b | AC-F5.3.1-01 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#staffCannotOverrideBranchFilter` |
| TC-F5.3.1-01c | AC-F5.3.1-01 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `features/invoices/pages/InvoicesPage.test.tsx › TC-F5.3.1-01c: %s không tải danh mục hoặc chọn chi nhánh khá` |
| TC-F5.3.1-02a | AC-F5.3.1-02 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#filterByStatusSourceAndKeyword` |
| TC-F5.3.1-02b | AC-F5.3.1-02 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#filterByVietnamDateRangeHalfOpen` |
| TC-F5.3.1-02c | AC-F5.3.1-02 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `features/invoices/pages/InvoicesPage.test.tsx › TC-F5.3.1-02c: lọc danh sách hóa đơn theo trạng thái, nguồn,` |
| TC-F5.3.1-02d | AC-F5.3.1-02 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `features/invoices/pages/InvoicesPage.test.tsx › TC-F5.3.1-02d: lỗi danh sách có thể tải lại và hiển thị trạn` |
| TC-F5.3.1-03a | AC-F5.3.1-03 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#walkInInvoiceWithNullMemberIsVisibleInListAndDetail` |
| TC-F5.3.1-03b | AC-F5.3.1-03 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `features/invoices/pages/InvoicesPage.test.tsx › TC-F5.3.1-03b: hiển thị hóa đơn khách lẻ và tiền định dạng V` |
| TC-F5.3.1-04a | AC-F5.3.1-04 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#superAdminListsAllBranchesAndFiltersByBranchId` |
| TC-F5.3.1-04b | AC-F5.3.1-04 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#ownerListsBranchOptionsIncludingInactiveBranches` |
| TC-F5.3.1-04c | AC-F5.3.1-04 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `features/invoices/api/invoices.test.ts › TC-F5.3.1-04c: danh mục chi nhánh dùng endpoint hóa đơn dành` |
| TC-F5.3.1-04d | AC-F5.3.1-04 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `features/invoices/pages/InvoicesPage.test.tsx › TC-F5.3.1-04d: Chủ chuỗi lọc chi nhánh bằng danh mục toàn ch` |
| TC-F5.3.1-04e | AC-F5.3.1-04 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `features/invoices/pages/InvoicesPage.test.tsx › TC-F5.3.1-04e: lỗi danh mục chi nhánh có thể thử lại, danh s` |
| TC-F5.3.1-05a | AC-F5.3.1-05 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#detailReturnsItemsAndPaymentsWithReceiverName` |
| TC-F5.3.1-05b | AC-F5.3.1-05 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `features/invoices/api/invoices.test.ts › TC-F5.3.1-05b: list và detail gọi đúng endpoint và tham số l` |
| TC-F5.3.1-05c | AC-F5.3.1-05 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.3.1-05c: đổi mã hóa đơn trong route phải tải dữ liệu m` |
| TC-F5.3.1-06a | AC-F5.3.1-06 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-08, BR-F5-07 | `InvoiceCancelIntegrationTest#cancelPendingInvoiceCancelsMembership` |
| TC-F5.3.1-06b | AC-F5.3.1-06 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-08, BR-F5-07 | `InvoiceCancelIntegrationTest#cancelledInvoiceNoLongerBlocksSale` |
| TC-F5.3.1-06c | AC-F5.3.1-06 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-08, BR-F5-07 | `InvoiceCancelIntegrationTest#managerAndOwnerCanCancel` |
| TC-F5.3.1-06d | AC-F5.3.1-06 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-08, BR-F5-07 | `features/invoices/api/invoices.test.ts › TC-F5.3.1-06d: cancel gọi PUT /invoices/{id}/cancel` |
| TC-F5.3.1-06e | AC-F5.3.1-06 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-08, BR-F5-07 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.3.1-06e: hủy hóa đơn PENDING thành công và cập nhật tr` |
| TC-F5.3.1-07a | AC-F5.3.1-07 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceCancelIntegrationTest#cancelPaidInvoiceIs422` |
| TC-F5.3.1-07b | AC-F5.3.1-07 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceCancelIntegrationTest#cancelTwiceIs422` |
| TC-F5.3.1-07c | AC-F5.3.1-07 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.3.1-07c: hủy gặp %s phải tải lại trạng thái mới` |
| TC-F5.3.1-08a | AC-F5.3.1-08 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceCancelIntegrationTest#unknownInvoiceIs404` |
| TC-F5.3.1-08b | AC-F5.3.1-08 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceCancelIntegrationTest#otherBranchIs404` |
| TC-F5.3.1-08c | AC-F5.3.1-08 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceCancelIntegrationTest#otherRolesAreForbidden` |
| TC-F5.3.1-08d | AC-F5.3.1-08 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#detailOfOtherBranchOrUnknownIs404` |
| TC-F5.3.1-08e | AC-F5.3.1-08 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#trainerAndMemberAreForbidden` |
| TC-F5.3.1-08f | AC-F5.3.1-08 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#unauthenticatedRequestsAre401` |
| TC-F5.3.1-08g | AC-F5.3.1-08 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#nonOwnerCannotReadBranchOptions` |
| TC-F5.3.1-08h | AC-F5.3.1-08 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#anonymousCannotReadBranchOptions` |
| TC-F5.3.1-08i | AC-F5.3.1-08 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `layouts/staff-menu.test.ts › TC-F5.3.1-08i: menu Thanh toán bật đúng vai trò theo quyền r` |
| TC-F5.3.1-08j | AC-F5.3.1-08 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `routes/route-access.test.ts › TC-F5.3.1-08j: %s mở được danh sách và chi tiết hóa đơn` |
| TC-F5.3.1-08k | AC-F5.3.1-08 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `routes/route-access.test.ts › TC-F5.3.1-08k: %s không mở được trang hóa đơn của nhân viên` |
| TC-F5.3.1-09a | AC-F5.3.1-09 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#invalidQueryParamsAndIdReturn400` |
| TC-F5.3.1-09b | AC-F5.3.1-09 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceCancelIntegrationTest#invalidCancelIdIs400` |
| TC-F5.3.1-09c | AC-F5.3.1-09 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `features/invoices/pages/InvoicesPage.test.tsx › TC-F5.3.1-09c: khoảng ngày ngược báo lỗi và không gửi bộ lọc` |
| TC-F5.3.1-09d | AC-F5.3.1-09 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.3.1-09d: mã route không hợp lệ %s không gọi API` |
| TC-F5.3.1-10a | AC-F5.3.1-10 | US-F5.3.1 | UC-F5.3 | BR-F5-06, BR-F5-07, BR-F5-08 | `InvoiceQueryIntegrationTest#staffWithoutActiveBranchIs403` |
| TC-F5.4.1-01a | AC-F5.4.1-01 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `InvoiceDocumentIntegrationTest#pdfForPaidAndRefundedInvoiceIsReadableWithVietnameseText` |
| TC-F5.4.1-01b | AC-F5.4.1-01 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `features/invoices/api/invoices.test.ts › TC-F5.4.1-01b: pdf gọi GET /invoices/{id}/pdf với responseTy` |
| TC-F5.4.1-01c | AC-F5.4.1-01 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `features/invoices/api/invoices.test.ts › TC-F5.4.1-01c: lỗi PDF không phải JSON vẫn giữ được lỗi HTTP` |
| TC-F5.4.1-01d | AC-F5.4.1-01 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.4.1-01d: hóa đơn PAID và REFUNDED tải PDF bằng Blob, m` |
| TC-F5.4.1-01e | AC-F5.4.1-01 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.4.1-01e: mở được tab PDF thì không tải file lần nữa; b` |
| TC-F5.4.1-01f | AC-F5.4.1-01 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.4.1-01f: rời trang khi PDF đang tải không tạo URL hoặc` |
| TC-F5.4.1-02a | AC-F5.4.1-02 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `InvoiceDocumentIntegrationTest#pdfUsesSnapshotPriceAndDescriptionFromInvoiceItems` |
| TC-F5.4.1-03a | AC-F5.4.1-03 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `InvoiceDocumentIntegrationTest#pdfForPendingOrCancelledInvoiceIs422` |
| TC-F5.4.1-03b | AC-F5.4.1-03 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.4.1-03b: hóa đơn PENDING không hiển thị nút In / tải P` |
| TC-F5.4.1-04a | AC-F5.4.1-04 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `InvoiceDocumentIntegrationTest#pdfSecurityOwnershipAndScope` |
| TC-F5.4.1-04b | AC-F5.4.1-04 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `features/invoices/api/invoices.test.ts › TC-F5.4.1-04b: giữ status và detail của lỗi PDF Blob %s` |
| TC-F5.4.1-04c | AC-F5.4.1-04 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.4.1-04c: PDF 404 dạng Blob phải hiện detail server tha` |
| TC-F5.4.1-04d | AC-F5.4.1-04 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `features/invoices/pages/MemberInvoicePdfPage.test.tsx › TC-F5.4.1-04d: gọi API PDF trực tiếp, không cần quyền API ch` |
| TC-F5.4.1-04e | AC-F5.4.1-04 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `features/invoices/pages/MemberInvoicePdfPage.test.tsx › TC-F5.4.1-04e: hiển thị lỗi API %s, không tạo PDF từ dữ liệu` |
| TC-F5.4.1-04f | AC-F5.4.1-04 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `routes/route-access.test.ts › TC-F5.4.1-04f: chỉ MEMBER mở điểm vào PDF hội viên` |
| TC-F5.4.1-05a | AC-F5.4.1-05 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `InvoiceDocumentIntegrationTest#pdfSecurityOwnershipAndScope` |
| TC-F5.4.1-05b | AC-F5.4.1-05 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.4.1-05b: Chủ chuỗi không thấy nút PDF dù hóa đơn PAID` |
| TC-F5.4.1-06a | AC-F5.4.1-06 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `InvoiceDocumentIntegrationTest#walkInPaidInvoicePdfAndInvalidId` |
| TC-F5.4.1-07a | AC-F5.4.1-07 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `InvoiceDocumentIntegrationTest#walkInPaidInvoicePdfAndInvalidId` |
| TC-F5.4.1-07b | AC-F5.4.1-07 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `InvoiceDocumentIntegrationTest#invalidDocumentIdsReturnFieldErrors` |
| TC-F5.4.1-07c | AC-F5.4.1-07 | US-F5.4.1 | UC-F5.4 | BR-F5-09, BR-F5-10 | `features/invoices/pages/MemberInvoicePdfPage.test.tsx › TC-F5.4.1-07c: ID %s bị chặn trước khi gọi tải PDF` |
| TC-F5.5.1-01a | AC-F5.5.1-01 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `RefundIntegrationTest#refundSucceeds` |
| TC-F5.5.1-01b | AC-F5.5.1-01 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `RefundIntegrationTest#refundUpcomingRenewal` |
| TC-F5.5.1-01c | AC-F5.5.1-01 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.5.1-01c: quản lý chi nhánh hoàn tiền hai bước thành cô` |
| TC-F5.5.1-01d | AC-F5.5.1-01 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `features/invoices/api/invoices.test.ts › TC-F5.5.1-01d: refund gửi POST /invoices/{id}/refund với ref` |
| TC-F5.5.1-02a | AC-F5.5.1-02 | US-F5.5.1 | UC-F5.5 | BR-F5-13, BR-F5-11, BR-F5-12, BR-F5-14 | `RefundIntegrationTest#refundKeepsFollowingUpcomingUntouched` |
| TC-F5.5.1-03a | AC-F5.5.1-03 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `RefundIntegrationTest#receptionistIsForbidden` |
| TC-F5.5.1-03b | AC-F5.5.1-03 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `RefundIntegrationTest#otherRolesAreForbidden` |
| TC-F5.5.1-03c | AC-F5.5.1-03 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `RefundIntegrationTest#forbiddenComesBeforeValidation` |
| TC-F5.5.1-03d | AC-F5.5.1-03 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.5.1-03d: nút Hoàn tiền chỉ hiện cho BRANCH_MANAGER với` |
| TC-F5.5.1-04a | AC-F5.5.1-04 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `RefundIntegrationTest#missingReasonIs400` |
| TC-F5.5.1-04b | AC-F5.5.1-04 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `RefundIntegrationTest#noBodyOrTooLongReasonIs400` |
| TC-F5.5.1-04c | AC-F5.5.1-04 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `RefundIntegrationTest#reasonLengthCountedAfterStrip` |
| TC-F5.5.1-04d | AC-F5.5.1-04 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.5.1-04d: để trống lý do hoàn tiền không gửi API và hiệ` |
| TC-F5.5.1-04e | AC-F5.5.1-04 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.5.1-04e: lỗi 400 từ API hiện đúng câu errors.refundRea` |
| TC-F5.5.1-05a | AC-F5.5.1-05 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `RefundIntegrationTest#anonymousIs401` |
| TC-F5.5.1-05b | AC-F5.5.1-05 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `RefundIntegrationTest#unknownInvoiceIs404` |
| TC-F5.5.1-05c | AC-F5.5.1-05 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `RefundIntegrationTest#otherBranchManagerIs404` |
| TC-F5.5.1-05d | AC-F5.5.1-05 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.5.1-05d: lỗi 403 và 404 khi hoàn tiền hiện đúng câu de` |
| TC-F5.5.1-06a | AC-F5.5.1-06 | US-F5.5.1 | UC-F5.5 | BR-F5-12, BR-F5-11, BR-F5-13, BR-F5-14 | `RefundConcurrencyIntegrationTest#twoManagersRefundingSameInvoice` |
| TC-F5.5.1-06b | AC-F5.5.1-06 | US-F5.5.1 | UC-F5.5 | BR-F5-12, BR-F5-11, BR-F5-13, BR-F5-14 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.5.1-06b: lỗi 409 và 422 hóa đơn không PAID hiện đúng c` |
| TC-F5.5.1-07a | AC-F5.5.1-07 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `RefundIntegrationTest#pendingInvoiceIs422` |
| TC-F5.5.1-07b | AC-F5.5.1-07 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `RefundIntegrationTest#cancelledInvoiceIs422` |
| TC-F5.5.1-07c | AC-F5.5.1-07 | US-F5.5.1 | UC-F5.5 | BR-F5-11, BR-F5-12, BR-F5-13, BR-F5-14 | `RefundIntegrationTest#refundTwiceIs422` |
| TC-F5.5.1-08a | AC-F5.5.1-08 | US-F5.5.1 | UC-F5.5 | BR-F5-14, BR-F5-11, BR-F5-12, BR-F5-13 | `RefundIntegrationTest#frozenMembershipIs422` |
| TC-F5.5.1-08b | AC-F5.5.1-08 | US-F5.5.1 | UC-F5.5 | BR-F5-14, BR-F5-11, BR-F5-12, BR-F5-13 | `RefundConcurrencyIntegrationTest#membershipExpiredByJobBetweenCheckAndCancel` |
| TC-F5.5.1-09a | AC-F5.5.1-09 | US-F5.5.1 | UC-F5.5 | BR-F5-14, BR-F5-11, BR-F5-12, BR-F5-13 | `RefundIntegrationTest#checkedInMembershipIs422` |
| TC-F5.5.1-09b | AC-F5.5.1-09 | US-F5.5.1 | UC-F5.5 | BR-F5-14, BR-F5-11, BR-F5-12, BR-F5-13 | `RefundIntegrationTest#ptMembershipUsedForCheckinIs422` |
| TC-F5.5.1-10a | AC-F5.5.1-10 | US-F5.5.1 | UC-F5.5 | BR-F5-14, BR-F5-11, BR-F5-12, BR-F5-13 | `RefundIntegrationTest#bookedPtSessionIs422` |
| TC-F5.5.1-10b | AC-F5.5.1-10 | US-F5.5.1 | UC-F5.5 | BR-F5-14, BR-F5-11, BR-F5-12, BR-F5-13 | `RefundIntegrationTest#cancelledPtSessionStillCounts` |
| TC-F5.5.1-11a | AC-F5.5.1-11 | US-F5.5.1 | UC-F5.5 | BR-F5-14, BR-F5-11, BR-F5-12, BR-F5-13 | `RefundIntegrationTest#upcomingClassBookingIs422` |
| TC-F5.5.1-11b | AC-F5.5.1-11 | US-F5.5.1 | UC-F5.5 | BR-F5-14, BR-F5-11, BR-F5-12, BR-F5-13 | `RefundIntegrationTest#inProgressClassBookingIs422` |
| TC-F5.5.1-11c | AC-F5.5.1-11 | US-F5.5.1 | UC-F5.5 | BR-F5-14, BR-F5-11, BR-F5-12, BR-F5-13 | `RefundIntegrationTest#pastOrCancelledBookingDoesNotBlock` |
| TC-F5.5.1-11d | AC-F5.5.1-11 | US-F5.5.1 | UC-F5.5 | BR-F5-14, BR-F5-11, BR-F5-12, BR-F5-13 | `RefundIntegrationTest#classBookingDoesNotBlockPtRefund` |
| TC-F5.5.1-11e | AC-F5.5.1-11 | US-F5.5.1 | UC-F5.5 | BR-F5-14, BR-F5-11, BR-F5-12, BR-F5-13 | `features/invoices/pages/InvoiceDetailPage.test.tsx › TC-F5.5.1-11e: lỗi 422 gói đã dùng và còn chỗ đặt lớp hiện đ` |
| TC-F5.5.1-12a | AC-F5.5.1-12 | US-F5.5.1 | UC-F5.5 | BR-F5-14, BR-F5-11, BR-F5-12, BR-F5-13 | `RefundIntegrationTest#otherMembershipUsageDoesNotBlock` |
| TC-F7.4.1-01a | AC-F7.4.1-01 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseIntegrationTest#purchaseCreatesPortalInvoice` |
| TC-F7.4.1-03a | AC-F7.4.1-03 | US-F7.4.1 | UC-F7.4 |  | `PendingInvoiceExpiryIntegrationTest#cancelsPortalInvoiceExactlyAtTtl` |
| TC-F7.4.1-03b | AC-F7.4.1-03 | US-F7.4.1 | UC-F7.4 |  | `PendingInvoiceExpiryIntegrationTest#runningTwiceChangesNothingMore` |
| TC-F7.4.1-03c | AC-F7.4.1-03 | US-F7.4.1 | UC-F7.4 |  | `PendingInvoiceExpiryIntegrationTest#paidPortalInvoiceIsUntouched` |
| TC-F7.4.1-03d | AC-F7.4.1-03 | US-F7.4.1 | UC-F7.4 |  | `PendingInvoiceExpiryIntegrationTest#canBuyAgainAfterAutoCancel` |
| TC-F7.4.1-03e | AC-F7.4.1-03 | US-F7.4.1 | UC-F7.4 |  | `PendingInvoiceExpiryIntegrationTest#brokenInvoiceDoesNotBlockOthers` |
| TC-F7.4.1-03f | AC-F7.4.1-03 | US-F7.4.1 | UC-F7.4 |  | `PendingInvoiceExpiryIntegrationTest#expiredOwnInvoiceIsCancelledOnNewRequest` |
| TC-F7.4.1-03g | AC-F7.4.1-03 | US-F7.4.1 | UC-F7.4 |  | `PendingInvoiceExpiryIntegrationTest#notYetExpiredOwnInvoiceStillBlocks` |
| TC-F7.4.1-04a | AC-F7.4.1-04 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseIntegrationTest#expiresAtUsesBranchTtl` |
| TC-F7.4.1-04b | AC-F7.4.1-04 | US-F7.4.1 | UC-F7.4 |  | `PendingInvoiceExpiryIntegrationTest#ttlIsReadPerBranch` |
| TC-F7.4.1-05a | AC-F7.4.1-05 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseIntegrationTest#duplicatePendingForSamePackageIs422` |
| TC-F7.4.1-05b | AC-F7.4.1-05 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseIntegrationTest#counterPendingForSamePackageIs422` |
| TC-F7.4.1-05c | AC-F7.4.1-05 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseIntegrationTest#otherPackageOfSameTypePendingIs422` |
| TC-F7.4.1-05d | AC-F7.4.1-05 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseIntegrationTest#upcomingOfSameTypeIs422` |
| TC-F7.4.1-05e | AC-F7.4.1-05 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseIntegrationTest#inactiveMemberWithPendingIs422` |
| TC-F7.4.1-06a | AC-F7.4.1-06 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseConcurrencyIntegrationTest#twoPurchasesAtOnceCreateExactlyOne` |
| TC-F7.4.1-07a | AC-F7.4.1-07 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseIntegrationTest#unknownPackageIs404` |
| TC-F7.4.1-07b | AC-F7.4.1-07 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseIntegrationTest#inactivePackageIs422` |
| TC-F7.4.1-07c | AC-F7.4.1-07 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseIntegrationTest#packageOfOtherBranchIs422` |
| TC-F7.4.1-07d | AC-F7.4.1-07 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseIntegrationTest#inactiveMemberIs422` |
| TC-F7.4.1-08a | AC-F7.4.1-08 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseIntegrationTest#invalidPurchaseBodyIs400` |
| TC-F7.4.1-08b | AC-F7.4.1-08 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseIntegrationTest#nonMemberIs403` |
| TC-F7.4.1-08c | AC-F7.4.1-08 | US-F7.4.1 | UC-F7.4 |  | `PortalPurchaseIntegrationTest#memberWithoutProfileIs403` |
| TC-F7.5.1-01a | AC-F7.5.1-01 | US-F7.5.1 | UC-F7.5 |  | `PortalPurchaseIntegrationTest#renewCreatesNewPendingMembership` |
| TC-F7.5.1-02a | AC-F7.5.1-02 | US-F7.5.1 | UC-F7.5 |  | `PortalPurchaseIntegrationTest#renewCancelledMembershipIs422` |
| TC-F7.5.1-03a | AC-F7.5.1-03 | US-F7.5.1 | UC-F7.5 |  | `PortalPurchaseIntegrationTest#renewCancelledMembershipIs422` |
| TC-F7.5.1-04a | AC-F7.5.1-04 | US-F7.5.1 | UC-F7.5 |  | `PortalPurchaseIntegrationTest#renewOthersMembershipIs404` |
| TC-F7.5.1-04b | AC-F7.5.1-04 | US-F7.5.1 | UC-F7.5 |  | `PortalPurchaseIntegrationTest#renewUnknownMembershipIs404` |
| TC-F7.5.1-05a | AC-F7.5.1-05 | US-F7.5.1 | UC-F7.5 |  | `PortalPurchaseIntegrationTest#renewInactivePackageIs422` |
| TC-F7.5.1-06a | AC-F7.5.1-06 | US-F7.5.1 | UC-F7.5 |  | `PortalPurchaseIntegrationTest#renewTwiceIs422` |
| TC-F7.5.1-09a | AC-F7.5.1-09 | US-F7.5.1 | UC-F7.5 |  | `PortalPurchaseIntegrationTest#invalidRenewBodyIs400` |
| TC-F8.0.1-01a | AC-F8.0.1-01 | US-F8.0.1 | UC-F8.0 | BR-F8.0-01, BR-F8.0-02 | `ClassApiIntegrationTest#publicCalendarShowsOnlyPublicInformationWithVietnameseDates` |
| TC-F8.0.1-01b | AC-F8.0.1-01 | US-F8.0.1 | UC-F8.0 | BR-F8.0-01, BR-F8.0-02 | `features/classes/ClassCalendarPages.test.tsx › TC-F8.0.1-01b, TC-F8.0.1-04b: khách xem giờ Việt Nam và đăng` |
| TC-F8.0.1-02a | AC-F8.0.1-02 | US-F8.0.1 | UC-F8.0 | BR-F8.0-01, BR-F8.0-02 | `ClassApiIntegrationTest#publicCalendarShowsOnlyPublicInformationWithVietnameseDates` |
| TC-F8.0.1-02b | AC-F8.0.1-02 | US-F8.0.1 | UC-F8.0 | BR-F8.0-01, BR-F8.0-02 | `features/classes/ClassCalendarPages.test.tsx › TC-F8.0.1-02b, TC-F8.0.1-03b: lịch rỗng và bộ lọc từ chối kh` |
| TC-F8.0.1-03a | AC-F8.0.1-03 | US-F8.0.1 | UC-F8.0 | BR-F8.0-01, BR-F8.0-02 | `ClassApiIntegrationTest#publicCalendarShowsOnlyPublicInformationWithVietnameseDates` |
| TC-F8.0.1-03b | AC-F8.0.1-03 | US-F8.0.1 | UC-F8.0 | BR-F8.0-01, BR-F8.0-02 | `features/classes/ClassCalendarPages.test.tsx › TC-F8.0.1-02b, TC-F8.0.1-03b: lịch rỗng và bộ lọc từ chối kh` |
| TC-F8.0.1-04a | AC-F8.0.1-04 | US-F8.0.1 | UC-F8.0 | BR-F8.0-01, BR-F8.0-02 | `ClassApiIntegrationTest#publicCalendarShowsOnlyPublicInformationWithVietnameseDates` |
| TC-F8.0.1-04b | AC-F8.0.1-04 | US-F8.0.1 | UC-F8.0 | BR-F8.0-01, BR-F8.0-02 | `features/classes/ClassCalendarPages.test.tsx › TC-F8.0.1-01b, TC-F8.0.1-04b: khách xem giờ Việt Nam và đăng` |
| TC-F8.1.1-01a | AC-F8.1.1-01 | US-F8.1.1 | UC-F8.1 | BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8-01, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#apiAuthorizationValidationAndPersistence` |
| TC-F8.1.1-01b | AC-F8.1.1-01 | US-F8.1.1 | UC-F8.1 | BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8-01, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#receptionistIsRecordedWhenBookingForMember` |
| TC-F8.1.1-01c | AC-F8.1.1-01 | US-F8.1.1 | UC-F8.1 | BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8-01, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#memberCalendarReturnsOwnBookingAndAvailability` |
| TC-F8.1.1-01d | AC-F8.1.1-01 | US-F8.1.1 | UC-F8.1 | BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8-01, BR-F8.3-02, BR-F8.1-04 | `features/classes/ClassCalendarPages.test.tsx › TC-F8.1.1-01d, TC-F8.1.1-03d: đặt đúng hội viên, làm mới sĩ ` |
| TC-F8.1.1-02a | AC-F8.1.1-02 | US-F8.1.1 | UC-F8.1 | BR-F8.1-01, BR-F8.3-02, BR-F8.1-02, BR-F8.1-03, BR-F8-01, BR-F8.1-04 | `ClassApiIntegrationTest#rejectsIneligibleMemberships` |
| TC-F8.1.1-02b | AC-F8.1.1-02 | US-F8.1.1 | UC-F8.1 | BR-F8.1-01, BR-F8.3-02, BR-F8.1-02, BR-F8.1-03, BR-F8-01, BR-F8.1-04 | `features/classes/ClassCalendarPages.test.tsx › TC-F8.1.1-02b: hiển thị nguyên nhân server từ chối đặt chỗ` |
| TC-F8.1.1-03a | AC-F8.1.1-03 | US-F8.1.1 | UC-F8.1 | BR-F8.1-03, BR-F8-01, BR-F8.1-01, BR-F8.1-02, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#fullClassRejectsBookingAndCancellationReleasesSeat` |
| TC-F8.1.1-03b | AC-F8.1.1-03 | US-F8.1.1 | UC-F8.1 | BR-F8.1-03, BR-F8-01, BR-F8.1-01, BR-F8.1-02, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#occupiedStatusesCountTowardsCapacity` |
| TC-F8.1.1-03c | AC-F8.1.1-03 | US-F8.1.1 | UC-F8.1 | BR-F8.1-03, BR-F8-01, BR-F8.1-01, BR-F8.1-02, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#memberCalendarReturnsOwnBookingAndAvailability` |
| TC-F8.1.1-03d | AC-F8.1.1-03 | US-F8.1.1 | UC-F8.1 | BR-F8.1-03, BR-F8-01, BR-F8.1-01, BR-F8.1-02, BR-F8.3-02, BR-F8.1-04 | `features/classes/ClassCalendarPages.test.tsx › TC-F8.1.1-01d, TC-F8.1.1-03d: đặt đúng hội viên, làm mới sĩ ` |
| TC-F8.1.1-04a | AC-F8.1.1-04 | US-F8.1.1 | UC-F8.1 | BR-F8.1-03, BR-F8.1-01, BR-F8.1-02, BR-F8-01, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#concurrentRequestsDoNotExceedCapacity` |
| TC-F8.1.1-05a | AC-F8.1.1-05 | US-F8.1.1 | UC-F8.1 | BR-F8.1-04, BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8-01, BR-F8.3-02 | `ClassApiIntegrationTest#concurrentOverlappingRequestsRespectMemberLock` |
| TC-F8.1.1-05b | AC-F8.1.1-05 | US-F8.1.1 | UC-F8.1 | BR-F8.1-04, BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8-01, BR-F8.3-02 | `ClassApiIntegrationTest#apiAuthorizationValidationAndPersistence` |
| TC-F8.1.1-06a | AC-F8.1.1-06 | US-F8.1.1 | UC-F8.1 | BR-F8.1-04, BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8-01, BR-F8.3-02 | `ClassApiIntegrationTest#memberCanBookAgainAfterCancellation` |
| TC-F8.1.1-07a | AC-F8.1.1-07 | US-F8.1.1 | UC-F8.1 | BR-F8.1-02, BR-F8.1-01, BR-F8.1-03, BR-F8-01, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#dayWindowUsesVietnamDatesAndBranchOverrides` |
| TC-F8.1.1-08a | AC-F8.1.1-08 | US-F8.1.1 | UC-F8.1 | BR-F8.1-02, BR-F8-01, BR-F8.1-01, BR-F8.1-03, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#cutoffBoundary` |
| TC-F8.1.1-08b | AC-F8.1.1-08 | US-F8.1.1 | UC-F8.1 | BR-F8.1-02, BR-F8-01, BR-F8.1-01, BR-F8.1-03, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#afterCutoffCancellationReleasesSeatWithoutReopeningBooking` |
| TC-F8.1.1-09a | AC-F8.1.1-09 | US-F8.1.1 | UC-F8.1 | BR-F8-01, BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#apiAuthorizationValidationAndPersistence` |
| TC-F8.1.1-09b | AC-F8.1.1-09 | US-F8.1.1 | UC-F8.1 | BR-F8-01, BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#staffCannotCrossBranchesAndMemberCannotReadOrCancelOthersBookings` |
| TC-F8.1.1-10a | AC-F8.1.1-10 | US-F8.1.1 | UC-F8.1 | BR-F8.1-02, BR-F8.1-01, BR-F8.1-03, BR-F8-01, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#branchCutoffOverridesGlobalValue` |
| TC-F8.3.1-01a | AC-F8.3.1-01 | US-F8.3.1 | UC-F8.3 | BR-F8.3-01, BR-F8.3-04, BR-F8.3-02, BR-F8-03, BR-F8.1-03, BR-F8.7-05 | `ClassApiIntegrationTest#lateCancellationBoundary` |
| TC-F8.3.1-01b | AC-F8.3.1-01 | US-F8.3.1 | UC-F8.3 | BR-F8.3-01, BR-F8.3-04, BR-F8.3-02, BR-F8-03, BR-F8.1-03, BR-F8.7-05 | `ClassApiIntegrationTest#receptionistIsRecordedWhenBookingForMember` |
| TC-F8.3.1-02a | AC-F8.3.1-02 | US-F8.3.1 | UC-F8.3 | BR-F8.3-01, BR-F8.3-02, BR-F8.3-04, BR-F8-03, BR-F8.1-03, BR-F8.7-05 | `ClassApiIntegrationTest#violationsLockOnceAndExpiredLockDoesNotReuseOldViolations` |
| TC-F8.3.1-03a | AC-F8.3.1-03 | US-F8.3.1 | UC-F8.3 | BR-F8.3-01, BR-F8-03, BR-F8.3-04, BR-F8.3-02, BR-F8.1-03, BR-F8.7-05 | `ClassApiIntegrationTest#cancellationAfterRescheduleIsNotLate` |
| TC-F8.3.1-03b | AC-F8.3.1-03 | US-F8.3.1 | UC-F8.3 | BR-F8.3-01, BR-F8-03, BR-F8.3-04, BR-F8.3-02, BR-F8.1-03, BR-F8.7-05 | `features/classes/ClassCalendarPages.test.tsx › TC-F8.3.1-03b: xác nhận hủy chỗ đã đổi giờ và hiển thị trạng` |
| TC-F8.3.1-04a | AC-F8.3.1-04 | US-F8.3.1 | UC-F8.3 | BR-F8.3-04, BR-F8.1-03, BR-F8.3-01, BR-F8.3-02, BR-F8-03, BR-F8.7-05 | `ClassApiIntegrationTest#fullClassRejectsBookingAndCancellationReleasesSeat` |
| TC-F8.3.1-05a | AC-F8.3.1-05 | US-F8.3.1 | UC-F8.3 | BR-F8.3-02, BR-F8.3-01, BR-F8.3-04, BR-F8-03, BR-F8.1-03, BR-F8.7-05 | `ClassApiIntegrationTest#oldCalendarMonthViolationsDoNotCountAtVietnamMidnight` |
| TC-F8.3.1-06a | AC-F8.3.1-06 | US-F8.3.1 | UC-F8.3 | BR-F8.3-02, BR-F8.3-01, BR-F8.3-04, BR-F8-03, BR-F8.1-03, BR-F8.7-05 | `ClassApiIntegrationTest#violationsLockOnceAndExpiredLockDoesNotReuseOldViolations` |
| TC-F8.3.1-07a | AC-F8.3.1-07 | US-F8.3.1 | UC-F8.3 | BR-F8.3-02, BR-F8.3-04, BR-F8.3-01, BR-F8-03, BR-F8.1-03, BR-F8.7-05 | `ClassApiIntegrationTest#doubleCancellationOnlyRecordsOneViolation` |
| TC-F8.3.1-08a | AC-F8.3.1-08 | US-F8.3.1 | UC-F8.3 | BR-F8.3-04, BR-F8.3-01, BR-F8.3-02, BR-F8-03, BR-F8.1-03, BR-F8.7-05 | `ClassApiIntegrationTest#staffCannotCrossBranchesAndMemberCannotReadOrCancelOthersBookings` |
| TC-F8.3.1-09a | AC-F8.3.1-09 | US-F8.3.1 | UC-F8.3 | BR-F8.7-05, BR-F8.3-01, BR-F8.3-04, BR-F8.3-02, BR-F8-03, BR-F8.1-03 | `ClassApiIntegrationTest#changesPreserveBookingsUntilCancellationAndPublishRecipientSnapshot` |
| TC-F8.5.1-01a | AC-F8.5.1-01 | US-F8.5.1 | UC-F8.5 | BR-F8.5-01, BR-F8.5-02, BR-F8.5-03 | `ClassApiIntegrationTest#trainerCalendarUsesActualSessionAssignment` |
| TC-F8.5.1-01b | AC-F8.5.1-01 | US-F8.5.1 | UC-F8.5 | BR-F8.5-01, BR-F8.5-02, BR-F8.5-03 | `features/classes/ClassCalendarPages.test.tsx › TC-F8.5.1-01b, TC-F8.6.1-01b, TC-F8.6.1-03b: đi vào buổi đượ` |
| TC-F8.5.1-02a | AC-F8.5.1-02 | US-F8.5.1 | UC-F8.5 | BR-F8.5-01, BR-F8.5-02, BR-F8.5-03 | `ClassApiIntegrationTest#trainerCalendarUsesActualSessionAssignment` |
| TC-F8.5.1-03a | AC-F8.5.1-03 | US-F8.5.1 | UC-F8.5 | BR-F8.5-01, BR-F8.5-02, BR-F8.5-03 | `ClassApiIntegrationTest#trainerCalendarUsesActualSessionAssignment` |
| TC-F8.5.1-04a | AC-F8.5.1-04 | US-F8.5.1 | UC-F8.5 | BR-F8.5-02, BR-F8.5-01, BR-F8.5-03 | `ClassApiIntegrationTest#trainerCalendarUsesActualSessionAssignment` |
| TC-F8.6.1-01a | AC-F8.6.1-01 | US-F8.6.1 | UC-F8.6 | BR-F8.6-01, BR-F8.6-04, BR-F8.6-05, BR-F8.6-03, BR-F8.3-02, BR-F8.6-02, BR-F8.6-06, BR-F8-02 | `ClassApiIntegrationTest#attendanceHonorsAssignmentAndVietnameseDay` |
| TC-F8.6.1-01b | AC-F8.6.1-01 | US-F8.6.1 | UC-F8.6 | BR-F8.6-01, BR-F8.6-04, BR-F8.6-05, BR-F8.6-03, BR-F8.3-02, BR-F8.6-02, BR-F8.6-06, BR-F8-02 | `features/classes/ClassCalendarPages.test.tsx › TC-F8.5.1-01b, TC-F8.6.1-01b, TC-F8.6.1-03b: đi vào buổi đượ` |
| TC-F8.6.1-01c | AC-F8.6.1-01 | US-F8.6.1 | UC-F8.6 | BR-F8.6-01, BR-F8.6-04, BR-F8.6-05, BR-F8.6-03, BR-F8.3-02, BR-F8.6-02, BR-F8.6-06, BR-F8-02 | `ClassApiIntegrationTest#noShowOfSoftDeletedMemberIsRecordedWithoutPenalty` |
| TC-F8.6.1-02a | AC-F8.6.1-02 | US-F8.6.1 | UC-F8.6 | BR-F8.6-03, BR-F8.3-02, BR-F8.6-01, BR-F8.6-04, BR-F8.6-05, BR-F8.6-02, BR-F8.6-06, BR-F8-02 | `ClassApiIntegrationTest#firstNoShowLocksAndCorrectionDoesNotReevaluateLock` |
| TC-F8.6.1-03a | AC-F8.6.1-03 | US-F8.6.1 | UC-F8.6 | BR-F8.6-02, BR-F8.6-01, BR-F8.6-04, BR-F8.6-05, BR-F8.6-03, BR-F8.3-02, BR-F8.6-06, BR-F8-02 | `ClassApiIntegrationTest#attendanceHonorsAssignmentAndVietnameseDay` |
| TC-F8.6.1-03b | AC-F8.6.1-03 | US-F8.6.1 | UC-F8.6 | BR-F8.6-02, BR-F8.6-01, BR-F8.6-04, BR-F8.6-05, BR-F8.6-03, BR-F8.3-02, BR-F8.6-06, BR-F8-02 | `features/classes/ClassCalendarPages.test.tsx › TC-F8.5.1-01b, TC-F8.6.1-01b, TC-F8.6.1-03b: đi vào buổi đượ` |
| TC-F8.6.1-04a | AC-F8.6.1-04 | US-F8.6.1 | UC-F8.6 | BR-F8.6-01, BR-F8.6-04, BR-F8.6-05, BR-F8.6-03, BR-F8.3-02, BR-F8.6-02, BR-F8.6-06, BR-F8-02 | `ClassApiIntegrationTest#attendanceHonorsAssignmentAndVietnameseDay` |
| TC-F8.6.1-05a | AC-F8.6.1-05 | US-F8.6.1 | UC-F8.6 | BR-F8.6-01, BR-F8.6-05, BR-F8.6-04, BR-F8.6-03, BR-F8.3-02, BR-F8.6-02, BR-F8.6-06, BR-F8-02 | `ClassApiIntegrationTest#attendanceHonorsAssignmentAndVietnameseDay` |
| TC-F8.6.1-06a | AC-F8.6.1-06 | US-F8.6.1 | UC-F8.6 | BR-F8.6-01, BR-F8.6-05, BR-F8.6-04, BR-F8.6-03, BR-F8.3-02, BR-F8.6-02, BR-F8.6-06, BR-F8-02 | `ClassApiIntegrationTest#attendanceHonorsAssignmentAndVietnameseDay` |
| TC-F8.6.1-07a | AC-F8.6.1-07 | US-F8.6.1 | UC-F8.6 | BR-F8.6-01, BR-F8.6-03, BR-F8.6-04, BR-F8.6-05, BR-F8.3-02, BR-F8.6-02, BR-F8.6-06, BR-F8-02 | `ClassApiIntegrationTest#firstNoShowLocksAndCorrectionDoesNotReevaluateLock` |
| TC-F8.6.1-08a | AC-F8.6.1-08 | US-F8.6.1 | UC-F8.6 | BR-F8.6-01, BR-F8.6-03, BR-F8.6-04, BR-F8.6-05, BR-F8.3-02, BR-F8.6-02, BR-F8.6-06, BR-F8-02 | `ClassApiIntegrationTest#firstNoShowLocksAndCorrectionDoesNotReevaluateLock` |
| TC-F8.6.1-09a | AC-F8.6.1-09 | US-F8.6.1 | UC-F8.6 | BR-F8.6-05, BR-F8.6-06, BR-F8.6-01, BR-F8.6-04, BR-F8.6-03, BR-F8.3-02, BR-F8.6-02, BR-F8-02 | `ClassApiIntegrationTest#firstNoShowLocksAndCorrectionDoesNotReevaluateLock` |
| TC-F8.6.1-10a | AC-F8.6.1-10 | US-F8.6.1 | UC-F8.6 | BR-F8-02, BR-F8.6-01, BR-F8.6-04, BR-F8.6-05, BR-F8.6-03, BR-F8.3-02, BR-F8.6-02, BR-F8.6-06 | `ClassApiIntegrationTest#firstNoShowLocksAndCorrectionDoesNotReevaluateLock` |
| TC-F8.6.1-11a | AC-F8.6.1-11 | US-F8.6.1 | UC-F8.6 | BR-F8-02, BR-F8.6-01, BR-F8.6-04, BR-F8.6-05, BR-F8.6-03, BR-F8.3-02, BR-F8.6-02, BR-F8.6-06 | `ClassApiIntegrationTest#firstNoShowLocksAndCorrectionDoesNotReevaluateLock` |
| TC-F8.7.1-01a | AC-F8.7.1-01 | US-F8.7.1 | UC-F8.7 | BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-06, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#scheduleApiCreatesReadsUpdatesDeactivatesAndChecksNumericFields` |
| TC-F8.7.1-02a | AC-F8.7.1-02 | US-F8.7.1 | UC-F8.7 | BR-F8.7-02, BR-F8.7-01, BR-F8.7-03, BR-F8-04, BR-F8.7-06, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#validatesResourcesDatesCapacityAndRole` |
| TC-F8.7.1-03a | AC-F8.7.1-03 | US-F8.7.1 | UC-F8.7 | BR-F8.7-06, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#recurringOverlapUsesActualWeekdaysNotJustIntersectingDateRanges` |
| TC-F8.7.1-03b | AC-F8.7.1-03 | US-F8.7.1 | UC-F8.7 | BR-F8.7-06, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#recurringUpdateRejectsConflictWithItsOwnRescheduledSession` |
| TC-F8.7.1-03c | AC-F8.7.1-03 | US-F8.7.1 | UC-F8.7 | BR-F8.7-06, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#scheduleRejectsConcreteRoomTrainerAndPtConflicts` |
| TC-F8.7.1-04a | AC-F8.7.1-04 | US-F8.7.1 | UC-F8.7 | BR-F8.7-06, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#recurringOverlapUsesActualWeekdaysNotJustIntersectingDateRanges` |
| TC-F8.7.1-05a | AC-F8.7.1-05 | US-F8.7.1 | UC-F8.7 | BR-F8.7-02, BR-F8.7-04, BR-F8.7-01, BR-F8.7-03, BR-F8-04, BR-F8.7-06, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#scheduleGenerationIsIdempotentSnapshotsAndDeactivationPreserveSessions` |
| TC-F8.7.1-05b | AC-F8.7.1-05 | US-F8.7.1 | UC-F8.7 | BR-F8.7-02, BR-F8.7-04, BR-F8.7-01, BR-F8.7-03, BR-F8-04, BR-F8.7-06, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#calendarsPreserveGeneratedClassTypeAfterScheduleEdit` |
| TC-F8.7.1-05c | AC-F8.7.1-05 | US-F8.7.1 | UC-F8.7 | BR-F8.7-02, BR-F8.7-04, BR-F8.7-01, BR-F8.7-03, BR-F8-04, BR-F8.7-06, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassSessionTypeMigrationIntegrationTest#migrationBackfillsExistingSessionsAndAcceptsInsertsFromPreviousBackend` |
| TC-F8.7.1-08a | AC-F8.7.1-08 | US-F8.7.1 | UC-F8.7 | BR-F8-04, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8.7-06, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#staffCannotCrossBranchesAndMemberCannotReadOrCancelOthersBookings` |
| TC-F8.7.1-08b | AC-F8.7.1-08 | US-F8.7.1 | UC-F8.7 | BR-F8-04, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8.7-06, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#scheduleApiDistinguishesMissingOutOfScopeAndIneligibleResources` |
| TC-F8.7.1-08c | AC-F8.7.1-08 | US-F8.7.1 | UC-F8.7 | BR-F8-04, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8.7-06, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#changesPreserveBookingsUntilCancellationAndPublishRecipientSnapshot` |
| TC-F8.7.1-09a | AC-F8.7.1-09 | US-F8.7.1 | UC-F8.7 | BR-F8.4-03, BR-F8.7-02, BR-F8.7-06, BR-F8.7-01, BR-F8.7-03, BR-F8-04, BR-F8.7-04, BR-F8.1-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#changesPreserveBookingsUntilCancellationAndPublishRecipientSnapshot` |
| TC-F8.7.1-09b | AC-F8.7.1-09 | US-F8.7.1 | UC-F8.7 | BR-F8.4-03, BR-F8.7-02, BR-F8.7-06, BR-F8.7-01, BR-F8.7-03, BR-F8-04, BR-F8.7-04, BR-F8.1-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#rejectsRoomCapacityAndActualTimeConflict` |
| TC-F8.7.1-10a | AC-F8.7.1-10 | US-F8.7.1 | UC-F8.7 | BR-F8-03, BR-F8.8-02, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-06, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8.7-05 | `ClassApiIntegrationTest#changesPreserveBookingsUntilCancellationAndPublishRecipientSnapshot` |
| TC-F8.7.1-10b | AC-F8.7.1-10 | US-F8.7.1 | UC-F8.7 | BR-F8-03, BR-F8.8-02, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-06, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8.7-05 | `ClassApiIntegrationTest#rejectsRoomCapacityAndActualTimeConflict` |
| TC-F8.7.1-11a | AC-F8.7.1-11 | US-F8.7.1 | UC-F8.7 | BR-F8.7-05, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-06, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02 | `ClassApiIntegrationTest#changesPreserveBookingsUntilCancellationAndPublishRecipientSnapshot` |
| TC-F8.7.1-12a | AC-F8.7.1-12 | US-F8.7.1 | UC-F8.7 | BR-F8.4-03, BR-F8-03, BR-F8.7-05, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-06, BR-F8.7-04, BR-F8.1-03, BR-F8.8-02 | `ClassApiIntegrationTest#notificationFailureIsIsolatedAndRollbackDoesNotPublish` |
| TC-F8.8.1-01a | AC-F8.8.1-01 | US-F8.8.1 | UC-F8.8 | BR-F8.8-01, BR-F8.8-03, BR-F8.8-02, BR-F8.7-06 | `ClassApiIntegrationTest#scheduleGenerationIsIdempotentSnapshotsAndDeactivationPreserveSessions` |
| TC-F8.8.1-02a | AC-F8.8.1-02 | US-F8.8.1 | UC-F8.8 | BR-F8.8-02, BR-F8.8-01, BR-F8.8-03, BR-F8.7-06 | `ClassApiIntegrationTest#concurrentGenerationAndChangedSessionDoNotCreateDuplicates` |
| TC-F8.8.1-03a | AC-F8.8.1-03 | US-F8.8.1 | UC-F8.8 | BR-F8.8-01, BR-F8.8-03, BR-F8.8-02, BR-F8.7-06 | `ClassApiIntegrationTest#generationSkipsInactiveAndOutOfValidityDates` |
| TC-F8.8.1-04a | AC-F8.8.1-04 | US-F8.8.1 | UC-F8.8 | BR-F8.8-02, BR-F8.8-01, BR-F8.8-03, BR-F8.7-06 | `ClassApiIntegrationTest#concurrentGenerationAndChangedSessionDoNotCreateDuplicates` |
| TC-F8.8.1-05a | AC-F8.8.1-05 | US-F8.8.1 | UC-F8.8 | BR-F8.8-01, BR-F8.8-03, BR-F8.7-06, BR-F8.8-02 | `ClassApiIntegrationTest#generationRejectsInactiveRoomOrConflictingSession` |
| TC-F10.1.1-01b | AC-F10.1.1-01 | US-F10.1.1 | UC-F10.1 |  | `NotificationReminderIntegrationTest#chainWideMembershipUsesHomeBranch` |
| TC-F10.1.1-01c | AC-F10.1.1-01 | US-F10.1.1 | UC-F10.1 |  | `MembershipExpiryReminderJobTest#annotationsAndCatchUp` |
| TC-F10.1.1-01d | AC-F10.1.1-01 | US-F10.1.1 | UC-F10.1 |  | `MembershipExpiryReminderJobTest#retriesEveryFiveMinutesUntilSuccess` |
| TC-F10.1.1-01e | AC-F10.1.1-01 | US-F10.1.1 | UC-F10.1 |  | `MembershipExpiryReminderJobTest#stopsRetryingOnNewDay` |
| TC-F10.1.1-01f | AC-F10.1.1-01 | US-F10.1.1 | UC-F10.1 |  | `NotificationReminderIntegrationTest#branchConfigErrorDoesNotRollbackOtherBranch` |
| TC-F10.1.1-02a | AC-F10.1.1-02 | US-F10.1.1 | UC-F10.1 |  | `NotificationReminderIntegrationTest#sessionThresholdFromConfig` |
| TC-F10.1.1-03a | AC-F10.1.1-03 | US-F10.1.1 | UC-F10.1 |  | `NotificationReminderIntegrationTest#rerunDoesNotDuplicate` |
| TC-F10.1.1-04a | AC-F10.1.1-04 | US-F10.1.1 | UC-F10.1 |  | `NotificationReminderIntegrationTest#upcomingSameTypeSkips` |
| TC-F10.1.1-04b | AC-F10.1.1-04 | US-F10.1.1 | UC-F10.1 |  | `EmailDispatchIntegrationTest#membershipReminderRecheckedAtDispatch` |
| TC-F10.1.1-05a | AC-F10.1.1-05 | US-F10.1.1 | UC-F10.1 |  | `NotificationReminderIntegrationTest#memberWithoutAccountSkipped` |
| TC-F10.1.1-06a | AC-F10.1.1-06 | US-F10.1.1 | UC-F10.1 |  | `NotificationReminderIntegrationTest#createMakesTwoRows` |
| TC-F10.1.1-06b | AC-F10.1.1-06 | US-F10.1.1 | UC-F10.1 |  | `NotificationReminderIntegrationTest#createRejectsInvalidArguments` |
| TC-F10.1.1-07a | AC-F10.1.1-07 | US-F10.1.1 | UC-F10.1 |  | `NotificationReminderIntegrationTest#createAfterCloseSchedulesNextOpening` |
| TC-F10.1.1-07b | AC-F10.1.1-07 | US-F10.1.1 | UC-F10.1 |  | `EmailDispatchIntegrationTest#sendsOnlyWhenDue` |
| TC-F10.1.1-07c | AC-F10.1.1-07 | US-F10.1.1 | UC-F10.1 |  | `EmailDispatchIntegrationTest#outsideHoursAtDispatchReschedules` |
| TC-F10.1.1-07d | AC-F10.1.1-07 | US-F10.1.1 | UC-F10.1 |  | `EmailDispatchJobTest#runsOffSchedulerThreadAndSkipsOverlap` |
| TC-F10.1.1-07e | AC-F10.1.1-07 | US-F10.1.1 | UC-F10.1 |  | `EmailDispatchIntegrationTest#sendErrorMarksFailed` |
| TC-F10.1.1-07f | AC-F10.1.1-07 | US-F10.1.1 | UC-F10.1 |  | `EmailDispatchIntegrationTest#blockedAddressAndDisabledAccount` |
| TC-F10.1.1-07g | AC-F10.1.1-07 | US-F10.1.1 | UC-F10.1 |  | `OpeningHoursTest#insideHoursSendsNow` |
| TC-F10.1.1-07h | AC-F10.1.1-07 | US-F10.1.1 | UC-F10.1 |  | `OpeningHoursTest#afterCloseSendsNextMorning` |
| TC-F10.1.1-07i | AC-F10.1.1-07 | US-F10.1.1 | UC-F10.1 |  | `OpeningHoursTest#beforeOpenSendsSameDay` |
| TC-F10.1.1-07j | AC-F10.1.1-07 | US-F10.1.1 | UC-F10.1 |  | `OpeningHoursTest#boundaries` |
| TC-F10.2.1-01a | AC-F10.2.1-01 | US-F10.2.1 | UC-F10.2 |  | `NotificationReminderIntegrationTest#classWithin24Hours` |
| TC-F10.2.1-01b | AC-F10.2.1-01 | US-F10.2.1 | UC-F10.2 |  | `NotificationReminderIntegrationTest#classReminderUsesSessionClassTypeNotSchedule` |
| TC-F10.2.1-02a | AC-F10.2.1-02 | US-F10.2.1 | UC-F10.2 |  | `NotificationReminderIntegrationTest#classReminderOnce` |
| TC-F10.2.1-03a | AC-F10.2.1-03 | US-F10.2.1 | UC-F10.2 |  | `NotificationReminderIntegrationTest#cancelledOrStartedNotReminded` |
| TC-F10.2.1-03b | AC-F10.2.1-03 | US-F10.2.1 | UC-F10.2 |  | `EmailDispatchIntegrationTest#classReminderRecheckedAtDispatch` |
| TC-F10.2.1-05a | AC-F10.2.1-05 | US-F10.2.1 | UC-F10.2 |  | `EmailDispatchIntegrationTest#classReminderRebuildsContentWhenSessionRescheduledBeforeDispatch` |
| TC-F10.2.1-06a | AC-F10.2.1-06 | US-F10.2.1 | UC-F10.2 |  | `EmailDispatchIntegrationTest#branchDerivedFromRelatedEntity` |

## 6. Kiểm thử chưa mang mã TC

Có 263 kiểm thử chưa mang mã TC. Gồm kiểm thử thuần kỹ thuật (JWT, cấu hình, ràng buộc database; được phép ghi mã `S…` hay `BR-…` thay mã TC) và kiểm thử của chức năng chưa gắn mã.

| File | Số kiểm thử |
| --- | --- |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/common/exception/GlobalExceptionHandlerTest.java` | 9 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/common/mail/EmailSenderTest.java` | 4 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/common/scheduling/SchedulingConfigTest.java` | 4 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/common/security/CurrentUserTest.java` | 6 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/common/time/VnTimeTest.java` | 5 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/common/util/PhoneUtilsTest.java` | 2 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/DatabaseCleanerTest.java` | 3 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/DatabaseConstraintsTest.java` | 21 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/AuthControllerTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/AuthServiceTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/JwtTokenServiceTest.java` | 5 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/checkin/CheckinApiIntegrationTest.java` | 36 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/checkin/CheckinCheckoutConcurrencyIntegrationTest.java` | 2 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/checkin/CheckinCheckoutHistoryIntegrationTest.java` | 10 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/checkin/CheckinConcurrencyIntegrationTest.java` | 3 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/dashboard/DashboardApiIntegrationTest.java` | 13 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/groupclass/ClassApiIntegrationTest.java` | 2 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/invoice/InvoiceSaleIntegrationTest.java` | 2 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/F1RegistrationIntegrationTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/MemberApiIntegrationTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/MemberPhoneSupportTest.java` | 6 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/MemberServiceTest.java` | 4 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/StaffBranchIntegrationTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/membership/MembershipListIntegrationTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/packagecatalog/PackageCatalogServiceTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/packagecatalog/PackageMapperTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/packagecatalog/PublicPackageIntegrationTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/staff/StaffServiceTest.java` | 4 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/systemconfig/SystemConfigServiceTest.java` | 4 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/SeedDataMigrationIntegrationTest.java` | 1 |
| `frontend/src/app/router.test.ts` | 3 |
| `frontend/src/auth/AuthProvider.test.tsx` | 6 |
| `frontend/src/auth/cookie-operation-queue.test.ts` | 5 |
| `frontend/src/auth/redirect.test.ts` | 7 |
| `frontend/src/auth/session-coordination.test.ts` | 5 |
| `frontend/src/auth/session-store.test.ts` | 3 |
| `frontend/src/features/auth/api/auth-api.test.ts` | 5 |
| `frontend/src/features/auth/api/validation.test.ts` | 1 |
| `frontend/src/features/auth/pages/LoginPage.test.tsx` | 5 |
| `frontend/src/features/auth/pages/RegisterPage.test.tsx` | 1 |
| `frontend/src/features/checkins/api/checkins.test.ts` | 1 |
| `frontend/src/features/checkins/pages/CheckinPage.test.tsx` | 12 |
| `frontend/src/features/dashboard/api/dashboard.test.ts` | 1 |
| `frontend/src/features/dashboard/pages/DashboardPage.test.tsx` | 9 |
| `frontend/src/features/members/api/members.test.ts` | 6 |
| `frontend/src/features/members/components/MemberAvatar.test.tsx` | 2 |
| `frontend/src/features/members/components/MemberFormModal.test.tsx` | 1 |
| `frontend/src/features/members/pages/MembersPage.test.tsx` | 4 |
| `frontend/src/features/packages/api/packages.test.ts` | 2 |
| `frontend/src/features/packages/pages/SellPackagePage.test.tsx` | 3 |
| `frontend/src/features/shared/use-remote-query.test.ts` | 2 |
| `frontend/src/layouts/member-menu.test.ts` | 2 |
| `frontend/src/layouts/staff-menu.test.ts` | 2 |
| `frontend/src/layouts/UserMenu.test.tsx` | 1 |
| `frontend/src/routes/ProtectedRoute.test.tsx` | 2 |
| `frontend/src/routes/route-access.test.ts` | 5 |
| `frontend/src/services/http.test.ts` | 12 |
