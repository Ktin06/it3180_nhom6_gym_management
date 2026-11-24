# Ma trận truy vết UC → US → AC → TC

> **File sinh tự động, không sửa tay.** Chạy `node scripts/export-truy-vet.cjs` ở gốc repo để sinh lại.
> Nguồn: mã UC, US, AC, BR trong `docs/requirements/srs/`; mã TC trong tên kiểm thử backend (JUnit `@DisplayName`)
> và frontend (Vitest `it`, `test`, `describe`). Quy ước và cách dùng: [README](README.md), [ADR 0012](../decisions/0012-quy-uoc-ma-truy-vet.md).

## 1. Tổng quan theo chức năng

| Chức năng | UC | US | AC | AC đã có TC | AC chưa có TC | TC |
| --- | --- | --- | --- | --- | --- | --- |
| F1 | 6 | 6 | 29 | 0 | 29 | 0 |
| F2 | 5 | 6 | 31 | 0 | 31 | 0 |
| F3 | 5 | 6 | 45 | 43 | 2 | 106 |
| F4 | 5 | 0 | 0 | 0 | 0 | 0 |
| F5 | 5 | 0 | 0 | 0 | 0 | 0 |
| F6 | 5 | 0 | 0 | 0 | 0 | 0 |
| F7 | 5 | 0 | 0 | 0 | 0 | 0 |
| F8 | 9 | 9 | 55 | 29 | 26 | 38 |
| F9 | 7 | 0 | 0 | 0 | 0 | 0 |
| F10 | 5 | 0 | 0 | 0 | 0 | 0 |
| **Tổng** | **57** | **27** | **160** | **72** | **88** | **144** |

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
| AC-F1.1.1-01 | Đăng ký thành công tạo đồng thời tài khoản và hồ sơ hội viên | US-F1.1.1 |
| AC-F1.1.1-02 | Từ chối khi số điện thoại đã thuộc hồ sơ hội viên có sẵn | US-F1.1.1 |
| AC-F1.1.1-03 | Từ chối khi email đã được sử dụng | US-F1.1.1 |
| AC-F1.1.1-04 | Từ chối dữ liệu đầu vào sai định dạng | US-F1.1.1 |
| AC-F1.1.1-05 | Hoàn tác toàn bộ giao dịch khi xảy ra lỗi giữa chừng hoặc chi nhánh mặc định ngừng hoạt động | US-F1.1.1 |
| AC-F1.2.1-01 | Nhập sai mật khẩu từ lần 1 đến lần 4 trả 401 thông báo chung, tăng bộ đếm và không lộ email tồn tại | US-F1.2.1 |
| AC-F1.2.1-02 | Nhập sai mật khẩu lần thứ 5 liên tiếp (kể cả gửi đồng thời) đặt bộ đếm bằng 5 và khóa tạm 15 phút | US-F1.2.1 |
| AC-F1.2.1-03 | Từ chối mọi yêu cầu đăng nhập trong thời gian đang bị khóa tạm 15 phút, kể cả nhập đúng mật khẩu | US-F1.2.1 |
| AC-F1.2.1-04 | Đăng nhập thành công hoặc hết thời gian khóa tạm | US-F1.2.1 |
| AC-F1.2.1-05 | Từ chối tài khoản bị khóa hoặc ngừng hoạt động khi nhập đúng mật khẩu | US-F1.2.1 |
| AC-F1.2.1-06 | Từ chối dữ liệu đăng nhập sai định dạng (không hỗ trợ đăng nhập bằng số điện thoại) | US-F1.2.1 |
| AC-F1.3.1-01 | Xoay vòng refresh token và chặn dùng lại token cũ | US-F1.3.1 |
| AC-F1.3.1-02 | Đăng xuất thu hồi refresh token và xóa cookie | US-F1.3.1 |
| AC-F1.3.1-03 | Từ chối khi phiên không hợp lệ và xóa cookie | US-F1.3.1 |
| AC-F1.3.1-04 | Đồng bộ làm mới phiên và đăng xuất giữa nhiều tab trình duyệt | US-F1.3.1 |
| AC-F1.4.1-01 | Đổi mật khẩu thành công, thu hồi toàn bộ phiên và yêu cầu đăng nhập lại | US-F1.4.1 |
| AC-F1.4.1-02 | Từ chối khi mật khẩu hiện tại sai hoặc mật khẩu mới trùng mật khẩu cũ | US-F1.4.1 |
| AC-F1.4.1-03 | Từ chối khi dữ liệu đầu vào sai định dạng hoặc mật khẩu mới không đạt chuẩn bảo mật | US-F1.4.1 |
| AC-F1.4.1-04 | Chặn truy cập khi chưa đăng nhập và mở đúng khung giao diện cho mọi vai trò đã đăng nhập | US-F1.4.1 |
| AC-F1.5.1-01 | Yêu cầu quên mật khẩu cho tài khoản ACTIVE tạo token băm SHA-256 hạn 30 phút, gửi email và trả 200 câu chung | US-F1.5.1 |
| AC-F1.5.1-02 | Không lộ email khi email không tồn tại, tài khoản không ACTIVE hoặc gửi email gặp lỗi | US-F1.5.1 |
| AC-F1.5.1-03 | Đặt lại mật khẩu với token hợp lệ cập nhật mật khẩu, ghi used_at, mở khóa tạm và thu hồi toàn bộ refresh token | US-F1.5.1 |
| AC-F1.5.1-04 | Từ chối token sai, hết hạn sau 30 phút, đã dùng (kể cả gửi đồng thời) hoặc tài khoản không ACTIVE với 422 | US-F1.5.1 |
| AC-F1.5.1-05 | Từ chối dữ liệu đầu vào sai định dạng, thiếu token trên URL hoặc mật khẩu mới không đạt chuẩn S1 | US-F1.5.1 |
| AC-F1.5.1-06 | Gửi email qua API HTTPS của Brevo khi có cấu hình khóa API và chỉ ghi log ở môi trường phát triển khi khóa rỗng | US-F1.5.1 |
| AC-F1.6.1-01 | Chưa đăng nhập hoặc token không hợp lệ bị từ chối 401 tiếng Việt | US-F1.6.1 |
| AC-F1.6.1-02 | Chặn API khi sai vai trò và cho phép khi đúng vai trò | US-F1.6.1 |
| AC-F1.6.1-03 | Nhân viên chi nhánh phải gắn với chi nhánh đang hoạt động | US-F1.6.1 |
| AC-F1.6.1-04 | Tài khoản ngừng hoạt động mất quyền và bị thu hồi phiên ngay | US-F1.6.1 |
| AC-F2.1.1-01 | Tạo hồ sơ mới thành công, chuẩn hóa SĐT và tự sinh mã HV | US-F2.1.1 |
| AC-F2.1.1-02 | Từ chối khi số điện thoại sai định dạng hoặc đã tồn tại (kể cả tạo đồng thời) | US-F2.1.1 |
| AC-F2.1.1-03 | Chỉ lễ tân, quản lý chi nhánh (đã gắn chi nhánh) và chủ chuỗi được tạo hồ sơ | US-F2.1.1 |
| AC-F2.2.1-01 | Tạo tài khoản và gửi email đặt mật khẩu hạn 72 giờ | US-F2.2.1 |
| AC-F2.2.1-02 | Hội viên đặt mật khẩu lần đầu rồi đăng nhập; quá 72 giờ liên kết hết hạn | US-F2.2.1 |
| AC-F2.2.1-03 | Từ chối khi hồ sơ đã có tài khoản | US-F2.2.1 |
| AC-F2.2.1-04 | Từ chối khi email đã được sử dụng (kể cả đồng thời) | US-F2.2.1 |
| AC-F2.2.1-05 | Từ chối khi hồ sơ đang ngừng hoạt động | US-F2.2.1 |
| AC-F2.2.1-06 | Gửi email gặp lỗi vẫn giữ tài khoản và ghi log không chứa token | US-F2.2.1 |
| AC-F2.2.1-07 | Kiểm tra quyền theo vai trò, chi nhánh và dữ liệu đầu vào | US-F2.2.1 |
| AC-F2.3.1-01 | Tìm kiếm, lọc, phân trang trong chi nhánh mình đạt hiệu năng dưới 2 giây | US-F2.3.1 |
| AC-F2.3.1-02 | Chủ chuỗi tra cứu và xem chi tiết hồ sơ mọi chi nhánh | US-F2.3.1 |
| AC-F2.3.1-03 | Xem chi tiết đúng phạm vi; ngoài phạm vi hoặc đã xóa mềm trả 404 | US-F2.3.1 |
| AC-F2.3.1-04 | Huấn luyện viên chỉ xem chi tiết học viên PT của mình và không được gọi danh sách | US-F2.3.1 |
| AC-F2.4.1-01 | Cập nhật thông tin hồ sơ trong chi nhánh và chuẩn hóa số điện thoại | US-F2.4.1 |
| AC-F2.4.1-02 | Từ chối trùng số điện thoại, thiếu trạng thái hoặc hồ sơ ngoài phạm vi | US-F2.4.1 |
| AC-F2.4.1-03 | Ngừng hoạt động hồ sơ đồng thời khóa tài khoản và thu hồi mọi phiên trong cùng giao dịch | US-F2.4.1 |
| AC-F2.4.1-04 | Kích hoạt lại hồ sơ khôi phục tài khoản nhưng không khôi phục token cũ và không gỡ khóa tạm | US-F2.4.1 |
| AC-F2.4.1-05 | Cập nhật trạng thái đồng thời luôn giữ hồ sơ và tài khoản khớp nhau | US-F2.4.1 |
| AC-F2.4.2-01 | Tải và chuẩn hóa ảnh hợp lệ | US-F2.4.2 |
| AC-F2.4.2-02 | Thay ảnh trên cùng bản ghi | US-F2.4.2 |
| AC-F2.4.2-03 | Từ chối file không hợp lệ | US-F2.4.2 |
| AC-F2.4.2-04 | Xem ảnh theo quyền hồ sơ | US-F2.4.2 |
| AC-F2.4.2-05 | Chỉ nhân viên có quyền sửa mới đổi ảnh | US-F2.4.2 |
| AC-F2.4.2-06 | Chưa có ảnh hoặc không có hồ sơ | US-F2.4.2 |
| AC-F2.4.2-07 | Xác thực và kiểm tra request ảnh | US-F2.4.2 |
| AC-F2.4.2-08 | Chưa có ảnh hoặc tải ảnh lỗi thì hiện chữ cái đầu của tên trên giao diện | US-F2.4.2 |
| AC-F2.5.1-01 | Ghi chỉ số cơ thể thành công với ít nhất một chỉ số | US-F2.5.1 |
| AC-F2.5.1-02 | Từ chối dữ liệu chỉ số cơ thể không hợp lệ | US-F2.5.1 |
| AC-F2.5.1-03 | Xem lịch sử chỉ số cơ thể sắp xếp mới nhất trước | US-F2.5.1 |
| AC-F2.5.1-04 | Phân quyền ghi và xem chỉ số cơ thể theo vai trò, chi nhánh và quan hệ PT | US-F2.5.1 |
| AC-F3.1.1-03 | Lễ tân chỉ xem, không có thao tác quản lý trên giao diện | US-F3.1.1 |
| AC-F3.5.1-09 | Hóa đơn tại quầy không tự hủy | US-F3.5.1 |
| AC-F8.0.1-01 | Xem lịch không cần đăng nhập | US-F8.0.1 |
| AC-F8.0.1-02 | Lịch rỗng | US-F8.0.1 |
| AC-F8.0.1-03 | Bộ lọc không hợp lệ | US-F8.0.1 |
| AC-F8.0.1-04 | Khách muốn đặt | US-F8.0.1 |
| AC-F8.3.1-09 | Hủy buổi từ phía quản lý | US-F8.3.1 |
| AC-F8.5.1-01 | Lịch của người được phân công | US-F8.5.1 |
| AC-F8.5.1-02 | Buổi có người dạy thay | US-F8.5.1 |
| AC-F8.5.1-03 | Không có lớp | US-F8.5.1 |
| AC-F8.5.1-04 | Sai vai trò hoặc yêu cầu lịch người khác | US-F8.5.1 |
| AC-F8.6.1-01 | Điểm danh lần đầu từ BOOKED | US-F8.6.1 |
| AC-F8.6.1-02 | Vắng mặt lần đầu đạt ngưỡng | US-F8.6.1 |
| AC-F8.6.1-03 | Check-in không thay điểm danh | US-F8.6.1 |
| AC-F8.6.1-04 | Sai phân công hoặc chi nhánh | US-F8.6.1 |
| AC-F8.6.1-05 | Biên thời gian của huấn luyện viên | US-F8.6.1 |
| AC-F8.6.1-06 | Quản lý sửa sau hạn | US-F8.6.1 |
| AC-F8.6.1-07 | Sửa NO_SHOW không gỡ khóa | US-F8.6.1 |
| AC-F8.6.1-08 | Sửa ATTENDED không tạo hoặc kéo dài khóa | US-F8.6.1 |
| AC-F8.6.1-09 | Trạng thái không đủ điều kiện | US-F8.6.1 |
| AC-F8.6.1-10 | Gỡ khóa thủ công có audit | US-F8.6.1 |
| AC-F8.6.1-11 | Gỡ khóa sai quyền | US-F8.6.1 |
| AC-F8.7.1-06 | Giảm sức chứa đúng biên | US-F8.7.1 |
| AC-F8.7.1-07 | Buổi không cho sửa sức chứa | US-F8.7.1 |
| AC-F8.7.1-09 | Đổi phòng giữ chỗ | US-F8.7.1 |
| AC-F8.7.1-10 | Đổi giờ giữ chỗ | US-F8.7.1 |
| AC-F8.7.1-11 | Hủy buổi không phạt | US-F8.7.1 |
| AC-F8.7.1-12 | Lỗi xử lý thông báo không đảo ngược thay đổi | US-F8.7.1 |

## 4. Cây xuôi: chức năng → UC → US → AC → TC

### F1

- **UC-F1.1** Khách tự đăng ký tài khoản hội viên
  - **US-F1.1.1** Tự đăng ký tài khoản hội viên (#117)
    - AC-F1.1.1-01 Đăng ký thành công tạo đồng thời tài khoản và hồ sơ hội viên (BR-F1-01, BR-F1-03, BR-F1-04, BR-F1-09) ❌ chưa có TC
    - AC-F1.1.1-02 Từ chối khi số điện thoại đã thuộc hồ sơ hội viên có sẵn (BR-F1-02) ❌ chưa có TC
    - AC-F1.1.1-03 Từ chối khi email đã được sử dụng (BR-F1-01) ❌ chưa có TC
    - AC-F1.1.1-04 Từ chối dữ liệu đầu vào sai định dạng (BR-F1-01, BR-F1-03) ❌ chưa có TC
    - AC-F1.1.1-05 Hoàn tác toàn bộ giao dịch khi xảy ra lỗi giữa chừng hoặc chi nhánh mặc định ngừng hoạt động (BR-F1-09) ❌ chưa có TC
- **UC-F1.2** Đăng nhập bằng email
  - **US-F1.2.1** Đăng nhập bằng email (#118)
    - AC-F1.2.1-01 Nhập sai mật khẩu từ lần 1 đến lần 4 trả 401 thông báo chung, tăng bộ đếm và không lộ email tồn tại (BR-F1-01, BR-F1-03, BR-F1-06) ❌ chưa có TC
    - AC-F1.2.1-02 Nhập sai mật khẩu lần thứ 5 liên tiếp (kể cả gửi đồng thời) đặt bộ đếm bằng 5 và khóa tạm 15 phút (BR-F1-06) ❌ chưa có TC
    - AC-F1.2.1-03 Từ chối mọi yêu cầu đăng nhập trong thời gian đang bị khóa tạm 15 phút, kể cả nhập đúng mật khẩu (BR-F1-06) ❌ chưa có TC
    - AC-F1.2.1-04 Đăng nhập thành công hoặc hết thời gian khóa tạm (BR-F1-01, BR-F1-04, BR-F1-06) ❌ chưa có TC
    - AC-F1.2.1-05 Từ chối tài khoản bị khóa hoặc ngừng hoạt động khi nhập đúng mật khẩu (BR-F1-05) ❌ chưa có TC
    - AC-F1.2.1-06 Từ chối dữ liệu đăng nhập sai định dạng (không hỗ trợ đăng nhập bằng số điện thoại) (BR-F1-01) ❌ chưa có TC
- **UC-F1.3** Làm mới phiên đăng nhập (Refresh Token) & Đăng xuất
  - **US-F1.3.1** Làm mới phiên và đăng xuất (#119)
    - AC-F1.3.1-01 Xoay vòng refresh token và chặn dùng lại token cũ (BR-F1-04) ❌ chưa có TC
    - AC-F1.3.1-02 Đăng xuất thu hồi refresh token và xóa cookie (BR-F1-04) ❌ chưa có TC
    - AC-F1.3.1-03 Từ chối khi phiên không hợp lệ và xóa cookie (BR-F1-04) ❌ chưa có TC
    - AC-F1.3.1-04 Đồng bộ làm mới phiên và đăng xuất giữa nhiều tab trình duyệt (BR-F1-04) ❌ chưa có TC
- **UC-F1.4** Đổi mật khẩu
  - **US-F1.4.1** Đổi mật khẩu (#120)
    - AC-F1.4.1-01 Đổi mật khẩu thành công, thu hồi toàn bộ phiên và yêu cầu đăng nhập lại (BR-F1-03, BR-F1-04) ❌ chưa có TC
    - AC-F1.4.1-02 Từ chối khi mật khẩu hiện tại sai hoặc mật khẩu mới trùng mật khẩu cũ (BR-F1-07) ❌ chưa có TC
    - AC-F1.4.1-03 Từ chối khi dữ liệu đầu vào sai định dạng hoặc mật khẩu mới không đạt chuẩn bảo mật (BR-F1-03) ❌ chưa có TC
    - AC-F1.4.1-04 Chặn truy cập khi chưa đăng nhập và mở đúng khung giao diện cho mọi vai trò đã đăng nhập (BR-F1-05) ❌ chưa có TC
- **UC-F1.5** Quên mật khẩu và đặt lại mật khẩu (Sprint 2)
  - **US-F1.5.1** Quên và đặt lại mật khẩu (#121)
    - AC-F1.5.1-01 Yêu cầu quên mật khẩu cho tài khoản ACTIVE tạo token băm SHA-256 hạn 30 phút, gửi email và trả 200 câu chung (BR-F1-01, BR-F1-08) ❌ chưa có TC
    - AC-F1.5.1-02 Không lộ email khi email không tồn tại, tài khoản không ACTIVE hoặc gửi email gặp lỗi (BR-F1-08) ❌ chưa có TC
    - AC-F1.5.1-03 Đặt lại mật khẩu với token hợp lệ cập nhật mật khẩu, ghi used_at, mở khóa tạm và thu hồi toàn bộ refresh token (BR-F1-03, BR-F1-04, BR-F1-06, BR-F1-08) ❌ chưa có TC
    - AC-F1.5.1-04 Từ chối token sai, hết hạn sau 30 phút, đã dùng (kể cả gửi đồng thời) hoặc tài khoản không ACTIVE với 422 (BR-F1-08) ❌ chưa có TC
    - AC-F1.5.1-05 Từ chối dữ liệu đầu vào sai định dạng, thiếu token trên URL hoặc mật khẩu mới không đạt chuẩn S1 (BR-F1-01, BR-F1-03) ❌ chưa có TC
    - AC-F1.5.1-06 Gửi email qua API HTTPS của Brevo khi có cấu hình khóa API và chỉ ghi log ở môi trường phát triển khi khóa rỗng (BR-F1-08) ❌ chưa có TC
- **UC-F1.6** Quản lý trạng thái tài khoản & Phân quyền truy cập API
  - **US-F1.6.1** Trạng thái tài khoản và phân quyền (#122)
    - AC-F1.6.1-01 Chưa đăng nhập hoặc token không hợp lệ bị từ chối 401 tiếng Việt (BR-F1-04, BR-F1-05) ❌ chưa có TC
    - AC-F1.6.1-02 Chặn API khi sai vai trò và cho phép khi đúng vai trò (BR-F1-05) ❌ chưa có TC
    - AC-F1.6.1-03 Nhân viên chi nhánh phải gắn với chi nhánh đang hoạt động (BR-F1-05) ❌ chưa có TC
    - AC-F1.6.1-04 Tài khoản ngừng hoạt động mất quyền và bị thu hồi phiên ngay (BR-F1-04, BR-F1-05) ❌ chưa có TC

### F2

- **UC-F2.1** Tạo hồ sơ hội viên tại quầy
  - **US-F2.1.1** Tạo hồ sơ hội viên tại quầy (#123)
    - AC-F2.1.1-01 Tạo hồ sơ mới thành công, chuẩn hóa SĐT và tự sinh mã HV (BR-F2-05, BR-F2-06, BR-F2-07) ❌ chưa có TC
    - AC-F2.1.1-02 Từ chối khi số điện thoại sai định dạng hoặc đã tồn tại (kể cả tạo đồng thời) (BR-F2-06) ❌ chưa có TC
    - AC-F2.1.1-03 Chỉ lễ tân, quản lý chi nhánh (đã gắn chi nhánh) và chủ chuỗi được tạo hồ sơ (BR-F2-10) ❌ chưa có TC
- **UC-F2.2** Tạo tài khoản đăng nhập cho hồ sơ hội viên có sẵn
  - **US-F2.2.1** Tạo tài khoản cho hồ sơ có sẵn (#124)
    - AC-F2.2.1-01 Tạo tài khoản và gửi email đặt mật khẩu hạn 72 giờ (BR-F2-06, BR-F2-11) ❌ chưa có TC
    - AC-F2.2.1-02 Hội viên đặt mật khẩu lần đầu rồi đăng nhập; quá 72 giờ liên kết hết hạn (BR-F2-11) ❌ chưa có TC
    - AC-F2.2.1-03 Từ chối khi hồ sơ đã có tài khoản (BR-F2-11) ❌ chưa có TC
    - AC-F2.2.1-04 Từ chối khi email đã được sử dụng (kể cả đồng thời) (BR-F2-06, BR-F2-11) ❌ chưa có TC
    - AC-F2.2.1-05 Từ chối khi hồ sơ đang ngừng hoạt động (BR-F2-11) ❌ chưa có TC
    - AC-F2.2.1-06 Gửi email gặp lỗi vẫn giữ tài khoản và ghi log không chứa token (BR-F2-11) ❌ chưa có TC
    - AC-F2.2.1-07 Kiểm tra quyền theo vai trò, chi nhánh và dữ liệu đầu vào (BR-F2-10, BR-F2-11) ❌ chưa có TC
- **UC-F2.3** Tra cứu, lọc và xem chi tiết hồ sơ hội viên
  - **US-F2.3.1** Tra cứu và xem hồ sơ hội viên (#125)
    - AC-F2.3.1-01 Tìm kiếm, lọc, phân trang trong chi nhánh mình đạt hiệu năng dưới 2 giây (BR-F2-05, BR-F2-06, BR-F2-09, BR-F2-10) ❌ chưa có TC
    - AC-F2.3.1-02 Chủ chuỗi tra cứu và xem chi tiết hồ sơ mọi chi nhánh (BR-F2-10) ❌ chưa có TC
    - AC-F2.3.1-03 Xem chi tiết đúng phạm vi; ngoài phạm vi hoặc đã xóa mềm trả 404 (BR-F2-09, BR-F2-10) ❌ chưa có TC
    - AC-F2.3.1-04 Huấn luyện viên chỉ xem chi tiết học viên PT của mình và không được gọi danh sách (BR-F2-10) ❌ chưa có TC
- **UC-F2.4** Cập nhật hồ sơ, Ảnh đại diện & Ngừng hoạt động hội viên
  - **US-F2.4.1** Cập nhật và ngừng hoạt động hồ sơ (#126)
    - AC-F2.4.1-01 Cập nhật thông tin hồ sơ trong chi nhánh và chuẩn hóa số điện thoại (BR-F2-06, BR-F2-10) ❌ chưa có TC
    - AC-F2.4.1-02 Từ chối trùng số điện thoại, thiếu trạng thái hoặc hồ sơ ngoài phạm vi (BR-F2-06, BR-F2-10) ❌ chưa có TC
    - AC-F2.4.1-03 Ngừng hoạt động hồ sơ đồng thời khóa tài khoản và thu hồi mọi phiên trong cùng giao dịch (BR-F2-08, BR-F2-09) ❌ chưa có TC
    - AC-F2.4.1-04 Kích hoạt lại hồ sơ khôi phục tài khoản nhưng không khôi phục token cũ và không gỡ khóa tạm (BR-F2-08) ❌ chưa có TC
    - AC-F2.4.1-05 Cập nhật trạng thái đồng thời luôn giữ hồ sơ và tài khoản khớp nhau (BR-F2-08) ❌ chưa có TC
  - **US-F2.4.2** Ảnh đại diện hội viên (#127)
    - AC-F2.4.2-01 Tải và chuẩn hóa ảnh hợp lệ (BR-F2-01, BR-F2-02) ❌ chưa có TC
    - AC-F2.4.2-02 Thay ảnh trên cùng bản ghi (BR-F2-03) ❌ chưa có TC
    - AC-F2.4.2-03 Từ chối file không hợp lệ (BR-F2-01) ❌ chưa có TC
    - AC-F2.4.2-04 Xem ảnh theo quyền hồ sơ (BR-F2-04) ❌ chưa có TC
    - AC-F2.4.2-05 Chỉ nhân viên có quyền sửa mới đổi ảnh (BR-F2-04) ❌ chưa có TC
    - AC-F2.4.2-06 Chưa có ảnh hoặc không có hồ sơ (BR-F2-04) ❌ chưa có TC
    - AC-F2.4.2-07 Xác thực và kiểm tra request ảnh ❌ chưa có TC
    - AC-F2.4.2-08 Chưa có ảnh hoặc tải ảnh lỗi thì hiện chữ cái đầu của tên trên giao diện (BR-F2-04) ❌ chưa có TC
- **UC-F2.5** Ghi nhận và xem lịch sử chỉ số cơ thể (`body_metrics`)
  - **US-F2.5.1** Chỉ số cơ thể (#128)
    - AC-F2.5.1-01 Ghi chỉ số cơ thể thành công với ít nhất một chỉ số (BR-F2-12) ❌ chưa có TC
    - AC-F2.5.1-02 Từ chối dữ liệu chỉ số cơ thể không hợp lệ (BR-F2-12) ❌ chưa có TC
    - AC-F2.5.1-03 Xem lịch sử chỉ số cơ thể sắp xếp mới nhất trước (BR-F2-12) ❌ chưa có TC
    - AC-F2.5.1-04 Phân quyền ghi và xem chỉ số cơ thể theo vai trò, chi nhánh và quan hệ PT (BR-F2-10, BR-F2-12) ❌ chưa có TC

### F3

- **UC-F3.1** Xem danh mục gói tập
  - **US-F3.1.1** Xem danh mục gói (nội bộ) (#129)
    - AC-F3.1.1-01 Nhân viên chi nhánh chỉ thấy gói của chi nhánh mình và gói toàn chuỗi (BR-F3-01)
      - TC-F3.1.1-01a `PackageCatalogIntegrationTest#scopesRowsAndCountAndIgnoresClientBranch`
    - AC-F3.1.1-02 Lọc, sắp xếp và phân trang
      - TC-F3.1.1-02a `PackageCatalogIntegrationTest#defaultActiveAndOrderingMatchContract`
      - TC-F3.1.1-02b `PackageCatalogIntegrationTest#inactiveFilterAndEmptyResults`
      - TC-F3.1.1-02c `PackageCatalogIntegrationTest#maximumSizeAndPagePastEndAreValid`
      - TC-F3.1.1-02d `PackageControllerTest#defaultContract`
      - TC-F3.1.1-02e `features/packages/pages/PackagesPage.test.tsx › TC-F3.1.1-02e: mặc định lấy gói đang bán; chọn loại PT giữ n`
      - TC-F3.1.1-02f `features/packages/api/packages.test.ts › TC-F3.1.1-02f: gửi bộ lọc và phân trang đúng endpoint đã đặc`
    - AC-F3.1.1-03 Lễ tân chỉ xem, không có thao tác quản lý trên giao diện ❌ chưa có TC
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
    - AC-F3.2.1-02 Số buổi đúng theo loại gói (BR-F3-02)
      - TC-F3.2.1-02a `PackageCreateIntegrationTest#sessionsAreRequiredAndPositive`
    - AC-F3.2.1-03 Dữ liệu sai bị từ chối, giá trị biên được nhận
      - TC-F3.2.1-03a `PackageCreateIntegrationTest#invalidInputsReturnFieldErrorsWithoutWrites`
      - TC-F3.2.1-03b `PackageCreateIntegrationTest#missingFieldsMalformedJsonAndLengthLimitsAreRejected`
      - TC-F3.2.1-03c `PackageCreateIntegrationTest#acceptsZeroPriceAndMaximumLengthsAndIntegers`
    - AC-F3.2.1-04 Không nhận trường do server quản lý
      - TC-F3.2.1-04a `PackageCreateIntegrationTest#rejectsServerManagedAndUnknownFields`
    - AC-F3.2.1-05 Mã gói không trùng
      - TC-F3.2.1-05a `PackageCreateIntegrationTest#duplicateInactiveGlobalCodeIsConflict`
      - TC-F3.2.1-05b `PackageCreateIntegrationTest#concurrentDuplicateCodeCreatesOneAndRollsBackOther`
    - AC-F3.2.1-06 Chỉ Quản lý chi nhánh được thêm gói
      - TC-F3.2.1-06a `PackageCreateIntegrationTest#onlyManagerCanCreate`
      - TC-F3.2.1-06b `PackageCreateIntegrationTest#missingTokenOrInactiveStaffCannotCreate`
- **UC-F3.3** Sửa thông tin gói tập
  - **US-F3.3.1** Sửa thông tin gói tập (#132) ✅ mọi AC có TC
    - AC-F3.3.1-01 Sửa giá; gói đã bán không đổi (BR-F3-06)
      - TC-F3.3.1-01a `PackageUpdateIntegrationTest#priceOnlyKeepsOmittedFieldsAndAudit`
      - TC-F3.3.1-01b `PackageUpdateIntegrationTest#historyIncludingPendingPurchaseRemainsIdentical`
    - AC-F3.3.1-02 Sửa gói chi nhánh khác hoặc gói toàn chuỗi bị từ chối 403 (BR-F3-07)
      - TC-F3.3.1-02a `PackageUpdateIntegrationTest#outsideBranchAndGlobalAre403WithoutMutation`
    - AC-F3.3.1-03 Xóa mô tả và gửi body rỗng
      - TC-F3.3.1-03a `PackageUpdateIntegrationTest#explicitNullClearsDescriptionAndEmptyBodyDoesNotChangeAudit`
    - AC-F3.3.1-04 Không đổi mã, loại và không xóa trường bắt buộc (BR-F3-05, BR-F3-02)
      - TC-F3.3.1-04a `PackageUpdateIntegrationTest#refusesImmutableAndUnknownFields`
      - TC-F3.3.1-04b `PackageUpdateIntegrationTest#sessionsMergeWithExistingTypeAndCannotBeCleared`
    - AC-F3.3.1-05 Dữ liệu sai thì không đổi gì
      - TC-F3.3.1-05a `PackageUpdateIntegrationTest#invalidUpdateIsAtomicAndReturnsFieldError`
      - TC-F3.3.1-05b `PackageUpdateIntegrationTest#invalidIdIsBadRequest`
      - TC-F3.3.1-05c `PackageUpdateIntegrationTest#malformedBodyIsRejected`
    - AC-F3.3.1-06 Sửa gói đã ngừng bán không làm gói bán lại
      - TC-F3.3.1-06a `PackageUpdateIntegrationTest#acceptsBoundariesAndCanEditInactiveWithoutReactivation`
    - AC-F3.3.1-07 Gói không tồn tại
      - TC-F3.3.1-07a `PackageUpdateIntegrationTest#missingPackageIs404WithoutMutation`
    - AC-F3.3.1-08 Chỉ Quản lý chi nhánh được sửa
      - TC-F3.3.1-08a `PackageUpdateIntegrationTest#deniedRolesCannotUpdate`
      - TC-F3.3.1-08b `PackageUpdateIntegrationTest#noTokenOrInactiveMissingStaffCannotUpdate`
- **UC-F3.4** Ngừng bán gói tập
  - **US-F3.4.1** Ngừng bán gói tập (#133) ✅ mọi AC có TC
    - AC-F3.4.1-01 Ngừng bán gói đang bán (BR-F3-08)
      - TC-F3.4.1-01a `PackageDeactivateIntegrationTest#deactivatesWithNoBodyOrEmptyJsonWithoutChangingOtherFields`
    - AC-F3.4.1-02 Gói đã bán vẫn dùng được (BR-F3-08)
      - TC-F3.4.1-02a `PackageDeactivateIntegrationTest#preservesAllHistoricalRows`
    - AC-F3.4.1-03 Gói ngừng bán không bán mới được nhưng vẫn tra cứu được (BR-F3-09)
      - TC-F3.4.1-03a `PackageCatalogIntegrationTest#inactiveFilterAndEmptyResults`
    - AC-F3.4.1-04 Ngừng bán lần nữa bị từ chối
      - TC-F3.4.1-04a `PackageDeactivateIntegrationTest#repeatIs422WithoutFurtherAuditChange`
      - TC-F3.4.1-04b `PackageDeactivateIntegrationTest#simultaneousDeactivationHasOneSuccessAndOne422`
    - AC-F3.4.1-05 Sửa cùng lúc không làm gói bán lại
      - TC-F3.4.1-05a `PackageDeactivateIntegrationTest#concurrentEditCannotReactivatePackage`
    - AC-F3.4.1-06 Từ chối gói ngoài chi nhánh, gói không tồn tại và vai trò khác (BR-F3-07)
      - TC-F3.4.1-06a `PackageDeactivateIntegrationTest#outsideBranchAndGlobalAre403EvenIfAlreadyInactive`
      - TC-F3.4.1-06b `PackageDeactivateIntegrationTest#deniedRolesCannotDeactivate`
      - TC-F3.4.1-06c `PackageDeactivateIntegrationTest#invalidIdIs400`
      - TC-F3.4.1-06d `PackageDeactivateIntegrationTest#missingAndBadTokenAre401`
      - TC-F3.4.1-06e `PackageDeactivateIntegrationTest#inactiveAndMissingStaffAre403`
      - TC-F3.4.1-06f `PackageDeactivateIntegrationTest#missingPackageIs404WithEmptyJsonBodyWithoutMutation`
- **UC-F3.5** Bán gói tập tại quầy (đăng ký mới / gia hạn)
  - **US-F3.5.1** Bán và gia hạn gói tại quầy (#134)
    - AC-F3.5.1-01 Bán gói tạo hóa đơn chờ và gói chờ, giá lấy ở server (BR-F3-04, BR-F3-13)
      - TC-F3.5.1-01a `InvoiceSaleIntegrationTest#sellCreatesPendingInvoiceAndMembershipWithServerPrice`
      - TC-F3.5.1-01b `InvoiceSaleIntegrationTest#sessionPackageCopiesSessionCount`
      - TC-F3.5.1-01c `InvoiceSaleIntegrationTest#membershipFailureRollsBackEntireSale`
    - AC-F3.5.1-02 Gói chỉ hiệu lực sau khi hóa đơn PAID; ngày hết hạn tính đúng (BR-F3-12)
      - TC-F3.5.1-02a `MembershipListIntegrationTest#pendingSaleIsNotUsableBeforePayment`
    - AC-F3.5.1-03 Gia hạn khi gói cũ còn hiệu lực (BR-F3-11)
      - TC-F3.5.1-03a `InvoiceSaleIntegrationTest#renewalStartsDayAfterOldTimePackage`
      - TC-F3.5.1-03b `MembershipServiceTest#newOrTimeRenewal`
    - AC-F3.5.1-04 Gia hạn khi gói SESSION cũ đã hết buổi (BR-F3-11)
      - TC-F3.5.1-04a `InvoiceSaleIntegrationTest#renewalAfterEmptySessionPackageStartsTomorrow`
      - TC-F3.5.1-04b `InvoiceSaleIntegrationTest#renewalAfterSessionPackageWithSessionsLeftStartsAfterEndDate`
      - TC-F3.5.1-04c `MembershipServiceTest#sessionRenewal`
    - AC-F3.5.1-05 Giữ đồng thời nhiều loại gói (BR-F3-10)
      - TC-F3.5.1-05a `InvoiceSaleIntegrationTest#differentPackageTypesKeepExistingMembershipIntact`
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
    - AC-F3.5.1-09 Hóa đơn tại quầy không tự hủy (BR-F3-14) ❌ chưa có TC
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

### F8

- **UC-F8.0** Xem lịch lớp học nhóm công khai
  - **US-F8.0.1** Xem lịch lớp công khai (#155)
    - AC-F8.0.1-01 Xem lịch không cần đăng nhập (BR-F8.0-01, BR-F8.0-02) ❌ chưa có TC
    - AC-F8.0.1-02 Lịch rỗng (BR-F8.0-01) ❌ chưa có TC
    - AC-F8.0.1-03 Bộ lọc không hợp lệ (BR-F8.0-01) ❌ chưa có TC
    - AC-F8.0.1-04 Khách muốn đặt (BR-F8.0-01) ❌ chưa có TC
- **UC-F8.1** Đặt chỗ lớp nhóm
  - **US-F8.1.1** Đặt chỗ lớp nhóm (#156) ✅ mọi AC có TC
    - AC-F8.1.1-01 Đặt khi còn chỗ và ghi người tạo (BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8-01)
      - TC-F8.1.1-01a `ClassApiIntegrationTest#apiAuthorizationValidationAndPersistence`
      - TC-F8.1.1-01b `ClassApiIntegrationTest#receptionistIsRecordedWhenBookingForMember`
    - AC-F8.1.1-02 Gói hoặc quyền đặt không hợp lệ (BR-F8.1-01, BR-F8.3-02)
      - TC-F8.1.1-02a `ClassApiIntegrationTest#rejectsIneligibleMemberships`
    - AC-F8.1.1-03 Lớp đủ chỗ (BR-F8.1-03, BR-F8-01)
      - TC-F8.1.1-03a `ClassApiIntegrationTest#fullClassRejectsBookingAndCancellationReleasesSeat`
      - TC-F8.1.1-03b `ClassApiIntegrationTest#occupiedStatusesCountTowardsCapacity`
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
  - **US-F8.3.1** Hủy chỗ lớp nhóm (#158)
    - AC-F8.3.1-01 Hủy sớm hoặc đúng biên (BR-F8.3-01, BR-F8.3-04)
      - TC-F8.3.1-01a `ClassApiIntegrationTest#lateCancellationBoundary`
      - TC-F8.3.1-01b `ClassApiIntegrationTest#receptionistIsRecordedWhenBookingForMember`
    - AC-F8.3.1-02 Hủy muộn đạt ngưỡng khóa (BR-F8.3-01, BR-F8.3-02)
      - TC-F8.3.1-02a `ClassApiIntegrationTest#violationsLockOnceAndExpiredLockDoesNotReuseOldViolations`
    - AC-F8.3.1-03 Hủy chỗ buổi đã đổi giờ (BR-F8.3-01, BR-F8-03)
      - TC-F8.3.1-03a `ClassApiIntegrationTest#cancellationAfterRescheduleIsNotLate`
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
    - AC-F8.3.1-09 Hủy buổi từ phía quản lý (BR-F8.7-05) ❌ chưa có TC
- **UC-F8.4** Đã bỏ (ADR 0014)
  - **US-F8.4.1** Đã bỏ (ADR 0014) (#159)
- **UC-F8.5** Xem lịch dạy lớp nhóm
  - **US-F8.5.1** Xem lịch dạy lớp nhóm (#160)
    - AC-F8.5.1-01 Lịch của người được phân công (BR-F8.5-01, BR-F8.5-02, BR-F8.5-03) ❌ chưa có TC
    - AC-F8.5.1-02 Buổi có người dạy thay (BR-F8.5-01) ❌ chưa có TC
    - AC-F8.5.1-03 Không có lớp (BR-F8.5-01) ❌ chưa có TC
    - AC-F8.5.1-04 Sai vai trò hoặc yêu cầu lịch người khác (BR-F8.5-02) ❌ chưa có TC
- **UC-F8.6** Điểm danh học viên lớp học nhóm
  - **US-F8.6.1** Điểm danh học viên lớp học nhóm (#161)
    - AC-F8.6.1-01 Điểm danh lần đầu từ BOOKED (BR-F8.6-01, BR-F8.6-04, BR-F8.6-05) ❌ chưa có TC
    - AC-F8.6.1-02 Vắng mặt lần đầu đạt ngưỡng (BR-F8.6-03, BR-F8.3-02) ❌ chưa có TC
    - AC-F8.6.1-03 Check-in không thay điểm danh (BR-F8.6-02) ❌ chưa có TC
    - AC-F8.6.1-04 Sai phân công hoặc chi nhánh (BR-F8.6-01) ❌ chưa có TC
    - AC-F8.6.1-05 Biên thời gian của huấn luyện viên (BR-F8.6-01, BR-F8.6-05) ❌ chưa có TC
    - AC-F8.6.1-06 Quản lý sửa sau hạn (BR-F8.6-01, BR-F8.6-05) ❌ chưa có TC
    - AC-F8.6.1-07 Sửa NO_SHOW không gỡ khóa (BR-F8.6-01, BR-F8.6-03) ❌ chưa có TC
    - AC-F8.6.1-08 Sửa ATTENDED không tạo hoặc kéo dài khóa (BR-F8.6-01, BR-F8.6-03) ❌ chưa có TC
    - AC-F8.6.1-09 Trạng thái không đủ điều kiện (BR-F8.6-05, BR-F8.6-06) ❌ chưa có TC
    - AC-F8.6.1-10 Gỡ khóa thủ công có audit (BR-F8-02) ❌ chưa có TC
    - AC-F8.6.1-11 Gỡ khóa sai quyền (BR-F8-02) ❌ chưa có TC
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
    - AC-F8.7.1-06 Giảm sức chứa đúng biên (BR-F8.7-02, BR-F8.1-03) ❌ chưa có TC
    - AC-F8.7.1-07 Buổi không cho sửa sức chứa (BR-F8.7-02) ❌ chưa có TC
    - AC-F8.7.1-08 Quyền lịch lặp và buổi riêng lẻ (BR-F8-04)
      - TC-F8.7.1-08a `ClassApiIntegrationTest#staffCannotCrossBranchesAndMemberCannotReadOrCancelOthersBookings`
      - TC-F8.7.1-08b `ClassApiIntegrationTest#scheduleApiDistinguishesMissingOutOfScopeAndIneligibleResources`
    - AC-F8.7.1-09 Đổi phòng giữ chỗ (BR-F8.4-03, BR-F8.7-02, BR-F8.7-06) ❌ chưa có TC
    - AC-F8.7.1-10 Đổi giờ giữ chỗ (BR-F8-03, BR-F8.8-02) ❌ chưa có TC
    - AC-F8.7.1-11 Hủy buổi không phạt (BR-F8.7-05) ❌ chưa có TC
    - AC-F8.7.1-12 Lỗi xử lý thông báo không đảo ngược thay đổi (BR-F8.4-03, BR-F8-03, BR-F8.7-05) ❌ chưa có TC
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

## 5. Bảng ngược: TC → AC → US → UC → BR

Dùng khi một kiểm thử báo lỗi: tìm mã TC để biết nó vi phạm AC, US và quy tắc nghiệp vụ nào.

| TC | AC | US | UC | BR | Kiểm thử |
| --- | --- | --- | --- | --- | --- |
| TC-F3.1.1-01a | AC-F3.1.1-01 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#scopesRowsAndCountAndIgnoresClientBranch` |
| TC-F3.1.1-02a | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#defaultActiveAndOrderingMatchContract` |
| TC-F3.1.1-02b | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#inactiveFilterAndEmptyResults` |
| TC-F3.1.1-02c | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#maximumSizeAndPagePastEndAreValid` |
| TC-F3.1.1-02d | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageControllerTest#defaultContract` |
| TC-F3.1.1-02e | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `features/packages/pages/PackagesPage.test.tsx › TC-F3.1.1-02e: mặc định lấy gói đang bán; chọn loại PT giữ n` |
| TC-F3.1.1-02f | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `features/packages/api/packages.test.ts › TC-F3.1.1-02f: gửi bộ lọc và phân trang đúng endpoint đã đặc` |
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
| TC-F3.2.1-02a | AC-F3.2.1-02 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#sessionsAreRequiredAndPositive` |
| TC-F3.2.1-03a | AC-F3.2.1-03 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#invalidInputsReturnFieldErrorsWithoutWrites` |
| TC-F3.2.1-03b | AC-F3.2.1-03 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#missingFieldsMalformedJsonAndLengthLimitsAreRejected` |
| TC-F3.2.1-03c | AC-F3.2.1-03 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#acceptsZeroPriceAndMaximumLengthsAndIntegers` |
| TC-F3.2.1-04a | AC-F3.2.1-04 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#rejectsServerManagedAndUnknownFields` |
| TC-F3.2.1-05a | AC-F3.2.1-05 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#duplicateInactiveGlobalCodeIsConflict` |
| TC-F3.2.1-05b | AC-F3.2.1-05 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#concurrentDuplicateCodeCreatesOneAndRollsBackOther` |
| TC-F3.2.1-06a | AC-F3.2.1-06 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#onlyManagerCanCreate` |
| TC-F3.2.1-06b | AC-F3.2.1-06 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#missingTokenOrInactiveStaffCannotCreate` |
| TC-F3.3.1-01a | AC-F3.3.1-01 | US-F3.3.1 | UC-F3.3 | BR-F3-06, BR-F3-05, BR-F3-07 | `PackageUpdateIntegrationTest#priceOnlyKeepsOmittedFieldsAndAudit` |
| TC-F3.3.1-01b | AC-F3.3.1-01 | US-F3.3.1 | UC-F3.3 | BR-F3-06, BR-F3-05, BR-F3-07 | `PackageUpdateIntegrationTest#historyIncludingPendingPurchaseRemainsIdentical` |
| TC-F3.3.1-02a | AC-F3.3.1-02 | US-F3.3.1 | UC-F3.3 | BR-F3-07, BR-F3-05, BR-F3-06 | `PackageUpdateIntegrationTest#outsideBranchAndGlobalAre403WithoutMutation` |
| TC-F3.3.1-03a | AC-F3.3.1-03 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#explicitNullClearsDescriptionAndEmptyBodyDoesNotChangeAudit` |
| TC-F3.3.1-04a | AC-F3.3.1-04 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-02, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#refusesImmutableAndUnknownFields` |
| TC-F3.3.1-04b | AC-F3.3.1-04 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-02, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#sessionsMergeWithExistingTypeAndCannotBeCleared` |
| TC-F3.3.1-05a | AC-F3.3.1-05 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#invalidUpdateIsAtomicAndReturnsFieldError` |
| TC-F3.3.1-05b | AC-F3.3.1-05 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#invalidIdIsBadRequest` |
| TC-F3.3.1-05c | AC-F3.3.1-05 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#malformedBodyIsRejected` |
| TC-F3.3.1-06a | AC-F3.3.1-06 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#acceptsBoundariesAndCanEditInactiveWithoutReactivation` |
| TC-F3.3.1-07a | AC-F3.3.1-07 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#missingPackageIs404WithoutMutation` |
| TC-F3.3.1-08a | AC-F3.3.1-08 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#deniedRolesCannotUpdate` |
| TC-F3.3.1-08b | AC-F3.3.1-08 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#noTokenOrInactiveMissingStaffCannotUpdate` |
| TC-F3.4.1-01a | AC-F3.4.1-01 | US-F3.4.1 | UC-F3.4 | BR-F3-08, BR-F3-07, BR-F3-09 | `PackageDeactivateIntegrationTest#deactivatesWithNoBodyOrEmptyJsonWithoutChangingOtherFields` |
| TC-F3.4.1-02a | AC-F3.4.1-02 | US-F3.4.1 | UC-F3.4 | BR-F3-08, BR-F3-07, BR-F3-09 | `PackageDeactivateIntegrationTest#preservesAllHistoricalRows` |
| TC-F3.4.1-03a | AC-F3.4.1-03 | US-F3.4.1 | UC-F3.4 | BR-F3-09, BR-F3-07, BR-F3-08 | `PackageCatalogIntegrationTest#inactiveFilterAndEmptyResults` |
| TC-F3.4.1-04a | AC-F3.4.1-04 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#repeatIs422WithoutFurtherAuditChange` |
| TC-F3.4.1-04b | AC-F3.4.1-04 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#simultaneousDeactivationHasOneSuccessAndOne422` |
| TC-F3.4.1-05a | AC-F3.4.1-05 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#concurrentEditCannotReactivatePackage` |
| TC-F3.4.1-06a | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#outsideBranchAndGlobalAre403EvenIfAlreadyInactive` |
| TC-F3.4.1-06b | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#deniedRolesCannotDeactivate` |
| TC-F3.4.1-06c | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#invalidIdIs400` |
| TC-F3.4.1-06d | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#missingAndBadTokenAre401` |
| TC-F3.4.1-06e | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#inactiveAndMissingStaffAre403` |
| TC-F3.4.1-06f | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#missingPackageIs404WithEmptyJsonBodyWithoutMutation` |
| TC-F3.5.1-01a | AC-F3.5.1-01 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-13, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-14 | `InvoiceSaleIntegrationTest#sellCreatesPendingInvoiceAndMembershipWithServerPrice` |
| TC-F3.5.1-01b | AC-F3.5.1-01 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-13, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-14 | `InvoiceSaleIntegrationTest#sessionPackageCopiesSessionCount` |
| TC-F3.5.1-01c | AC-F3.5.1-01 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-13, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-14 | `InvoiceSaleIntegrationTest#membershipFailureRollsBackEntireSale` |
| TC-F3.5.1-02a | AC-F3.5.1-02 | US-F3.5.1 | UC-F3.5 | BR-F3-12, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-13, BR-F3-14 | `MembershipListIntegrationTest#pendingSaleIsNotUsableBeforePayment` |
| TC-F3.5.1-03a | AC-F3.5.1-03 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#renewalStartsDayAfterOldTimePackage` |
| TC-F3.5.1-03b | AC-F3.5.1-03 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipServiceTest#newOrTimeRenewal` |
| TC-F3.5.1-04a | AC-F3.5.1-04 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#renewalAfterEmptySessionPackageStartsTomorrow` |
| TC-F3.5.1-04b | AC-F3.5.1-04 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#renewalAfterSessionPackageWithSessionsLeftStartsAfterEndDate` |
| TC-F3.5.1-04c | AC-F3.5.1-04 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipServiceTest#sessionRenewal` |
| TC-F3.5.1-05a | AC-F3.5.1-05 | US-F3.5.1 | UC-F3.5 | BR-F3-10, BR-F3-04, BR-F3-09, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#differentPackageTypesKeepExistingMembershipIntact` |
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
| TC-F8.1.1-01a | AC-F8.1.1-01 | US-F8.1.1 | UC-F8.1 | BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8-01, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#apiAuthorizationValidationAndPersistence` |
| TC-F8.1.1-01b | AC-F8.1.1-01 | US-F8.1.1 | UC-F8.1 | BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8-01, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#receptionistIsRecordedWhenBookingForMember` |
| TC-F8.1.1-02a | AC-F8.1.1-02 | US-F8.1.1 | UC-F8.1 | BR-F8.1-01, BR-F8.3-02, BR-F8.1-02, BR-F8.1-03, BR-F8-01, BR-F8.1-04 | `ClassApiIntegrationTest#rejectsIneligibleMemberships` |
| TC-F8.1.1-03a | AC-F8.1.1-03 | US-F8.1.1 | UC-F8.1 | BR-F8.1-03, BR-F8-01, BR-F8.1-01, BR-F8.1-02, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#fullClassRejectsBookingAndCancellationReleasesSeat` |
| TC-F8.1.1-03b | AC-F8.1.1-03 | US-F8.1.1 | UC-F8.1 | BR-F8.1-03, BR-F8-01, BR-F8.1-01, BR-F8.1-02, BR-F8.3-02, BR-F8.1-04 | `ClassApiIntegrationTest#occupiedStatusesCountTowardsCapacity` |
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
| TC-F8.3.1-04a | AC-F8.3.1-04 | US-F8.3.1 | UC-F8.3 | BR-F8.3-04, BR-F8.1-03, BR-F8.3-01, BR-F8.3-02, BR-F8-03, BR-F8.7-05 | `ClassApiIntegrationTest#fullClassRejectsBookingAndCancellationReleasesSeat` |
| TC-F8.3.1-05a | AC-F8.3.1-05 | US-F8.3.1 | UC-F8.3 | BR-F8.3-02, BR-F8.3-01, BR-F8.3-04, BR-F8-03, BR-F8.1-03, BR-F8.7-05 | `ClassApiIntegrationTest#oldCalendarMonthViolationsDoNotCountAtVietnamMidnight` |
| TC-F8.3.1-06a | AC-F8.3.1-06 | US-F8.3.1 | UC-F8.3 | BR-F8.3-02, BR-F8.3-01, BR-F8.3-04, BR-F8-03, BR-F8.1-03, BR-F8.7-05 | `ClassApiIntegrationTest#violationsLockOnceAndExpiredLockDoesNotReuseOldViolations` |
| TC-F8.3.1-07a | AC-F8.3.1-07 | US-F8.3.1 | UC-F8.3 | BR-F8.3-02, BR-F8.3-04, BR-F8.3-01, BR-F8-03, BR-F8.1-03, BR-F8.7-05 | `ClassApiIntegrationTest#doubleCancellationOnlyRecordsOneViolation` |
| TC-F8.3.1-08a | AC-F8.3.1-08 | US-F8.3.1 | UC-F8.3 | BR-F8.3-04, BR-F8.3-01, BR-F8.3-02, BR-F8-03, BR-F8.1-03, BR-F8.7-05 | `ClassApiIntegrationTest#staffCannotCrossBranchesAndMemberCannotReadOrCancelOthersBookings` |
| TC-F8.7.1-01a | AC-F8.7.1-01 | US-F8.7.1 | UC-F8.7 | BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-06, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#scheduleApiCreatesReadsUpdatesDeactivatesAndChecksNumericFields` |
| TC-F8.7.1-02a | AC-F8.7.1-02 | US-F8.7.1 | UC-F8.7 | BR-F8.7-02, BR-F8.7-01, BR-F8.7-03, BR-F8-04, BR-F8.7-06, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#validatesResourcesDatesCapacityAndRole` |
| TC-F8.7.1-03a | AC-F8.7.1-03 | US-F8.7.1 | UC-F8.7 | BR-F8.7-06, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#recurringOverlapUsesActualWeekdaysNotJustIntersectingDateRanges` |
| TC-F8.7.1-03b | AC-F8.7.1-03 | US-F8.7.1 | UC-F8.7 | BR-F8.7-06, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#recurringUpdateRejectsConflictWithItsOwnRescheduledSession` |
| TC-F8.7.1-03c | AC-F8.7.1-03 | US-F8.7.1 | UC-F8.7 | BR-F8.7-06, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#scheduleRejectsConcreteRoomTrainerAndPtConflicts` |
| TC-F8.7.1-04a | AC-F8.7.1-04 | US-F8.7.1 | UC-F8.7 | BR-F8.7-06, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#recurringOverlapUsesActualWeekdaysNotJustIntersectingDateRanges` |
| TC-F8.7.1-05a | AC-F8.7.1-05 | US-F8.7.1 | UC-F8.7 | BR-F8.7-02, BR-F8.7-04, BR-F8.7-01, BR-F8.7-03, BR-F8-04, BR-F8.7-06, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#scheduleGenerationIsIdempotentSnapshotsAndDeactivationPreserveSessions` |
| TC-F8.7.1-08a | AC-F8.7.1-08 | US-F8.7.1 | UC-F8.7 | BR-F8-04, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8.7-06, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#staffCannotCrossBranchesAndMemberCannotReadOrCancelOthersBookings` |
| TC-F8.7.1-08b | AC-F8.7.1-08 | US-F8.7.1 | UC-F8.7 | BR-F8-04, BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8.7-06, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05 | `ClassApiIntegrationTest#scheduleApiDistinguishesMissingOutOfScopeAndIneligibleResources` |
| TC-F8.8.1-01a | AC-F8.8.1-01 | US-F8.8.1 | UC-F8.8 | BR-F8.8-01, BR-F8.8-03, BR-F8.8-02, BR-F8.7-06 | `ClassApiIntegrationTest#scheduleGenerationIsIdempotentSnapshotsAndDeactivationPreserveSessions` |
| TC-F8.8.1-02a | AC-F8.8.1-02 | US-F8.8.1 | UC-F8.8 | BR-F8.8-02, BR-F8.8-01, BR-F8.8-03, BR-F8.7-06 | `ClassApiIntegrationTest#concurrentGenerationAndChangedSessionDoNotCreateDuplicates` |
| TC-F8.8.1-03a | AC-F8.8.1-03 | US-F8.8.1 | UC-F8.8 | BR-F8.8-01, BR-F8.8-03, BR-F8.8-02, BR-F8.7-06 | `ClassApiIntegrationTest#generationSkipsInactiveAndOutOfValidityDates` |
| TC-F8.8.1-04a | AC-F8.8.1-04 | US-F8.8.1 | UC-F8.8 | BR-F8.8-02, BR-F8.8-01, BR-F8.8-03, BR-F8.7-06 | `ClassApiIntegrationTest#concurrentGenerationAndChangedSessionDoNotCreateDuplicates` |
| TC-F8.8.1-05a | AC-F8.8.1-05 | US-F8.8.1 | UC-F8.8 | BR-F8.8-01, BR-F8.8-03, BR-F8.7-06, BR-F8.8-02 | `ClassApiIntegrationTest#generationRejectsInactiveRoomOrConflictingSession` |

## 6. Kiểm thử chưa mang mã TC

Có 283 kiểm thử chưa mang mã TC. Gồm kiểm thử thuần kỹ thuật (JWT, cấu hình, ràng buộc database; được phép ghi mã `S…` hay `BR-…` thay mã TC) và kiểm thử của chức năng chưa gắn mã.

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
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/AccountServiceTest.java` | 2 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/AuthControllerTest.java` | 18 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/AuthServiceTest.java` | 17 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/AuthSessionConcurrencyIntegrationTest.java` | 3 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/JwtSecurityIntegrationTest.java` | 9 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/JwtTokenServiceTest.java` | 5 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/groupclass/ClassApiIntegrationTest.java` | 2 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/invoice/InvoiceSaleIntegrationTest.java` | 2 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/F1RegistrationIntegrationTest.java` | 13 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/MemberApiIntegrationTest.java` | 25 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/MemberPhoneSupportTest.java` | 6 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/MemberServiceTest.java` | 11 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/StaffBranchIntegrationTest.java` | 8 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/membership/MembershipListIntegrationTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/packagecatalog/PackageCatalogServiceTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/packagecatalog/PackageMapperTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/packagecatalog/PublicPackageIntegrationTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/staff/StaffServiceTest.java` | 4 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/systemconfig/SystemConfigServiceTest.java` | 4 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/SeedDataMigrationIntegrationTest.java` | 1 |
| `frontend/src/app/router.test.ts` | 3 |
| `frontend/src/auth/account-change.test.tsx` | 1 |
| `frontend/src/auth/AuthProvider.test.tsx` | 9 |
| `frontend/src/auth/cookie-operation-queue.test.ts` | 5 |
| `frontend/src/auth/redirect.test.ts` | 7 |
| `frontend/src/auth/session-coordination.test.ts` | 5 |
| `frontend/src/auth/session-store.test.ts` | 3 |
| `frontend/src/features/auth/api/auth-api.test.ts` | 5 |
| `frontend/src/features/auth/api/validation.test.ts` | 3 |
| `frontend/src/features/auth/pages/LoginPage.test.tsx` | 6 |
| `frontend/src/features/auth/pages/RegisterPage.test.tsx` | 5 |
| `frontend/src/features/members/api/members.test.ts` | 4 |
| `frontend/src/features/members/components/MemberFormModal.test.tsx` | 3 |
| `frontend/src/features/members/pages/MembersPage.test.tsx` | 6 |
| `frontend/src/features/packages/api/packages.test.ts` | 1 |
| `frontend/src/features/shared/use-remote-query.test.ts` | 2 |
| `frontend/src/layouts/member-menu.test.ts` | 2 |
| `frontend/src/layouts/staff-menu.test.ts` | 2 |
| `frontend/src/layouts/UserMenu.test.tsx` | 1 |
| `frontend/src/routes/ProtectedRoute.test.tsx` | 5 |
| `frontend/src/routes/route-access.test.ts` | 5 |
| `frontend/src/services/http.test.ts` | 12 |
