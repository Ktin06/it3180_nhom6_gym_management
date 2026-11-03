# Đặc tả Use Case – F4: Check-in tại quầy

- **Mã chức năng:** F4
- **Version:** v1 (Bắt buộc)
- **Người biên soạn:** Lê Phước Minh Quân
- **Người duyệt chéo:** Nguyễn Trọng Việt
- **Tài liệu chuẩn tham chiếu:** 
  - [Bản chốt phạm vi](../ban-chot-pham-vi.md) (mục Check-in F4, F20; quy tắc gói TIME, SESSION, PT)
  - [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml)
  - [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md) (Nhóm 4 bảng `checkins`, `class_bookings`)
  - [Quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md)
  - [ADR 0001](../../decisions/0001-cong-nghe.md), [ADR 0007](../../decisions/0007-cau-truc-ma-nguon.md)

---

## 1. Tổng quan chức năng

Chức năng **F4 – Check-in tại quầy** là nghiệp vụ cốt lõi tại quầy lễ tân của phòng tập gym:
- Cho phép nhân viên lễ tân (`RECEPTIONIST`) hoặc quản lý chi nhánh (`BRANCH_MANAGER`) tra cứu nhanh hội viên qua **Mã hội viên (`HV` + 6 chữ số)** hoặc **Số điện thoại (10 chữ số bắt đầu bằng 0)**.
- Hiển thị trực quan thông tin hội viên cùng **ảnh đại diện** trên màn hình để nhân viên đối chiếu khuôn mặt trực tiếp, chống hành vi mượn thẻ hội viên.
- Tự động kiểm tra tính hợp lệ của các gói tập hội viên đang sở hữu tại chi nhánh:
  - Chỉ cho phép check-in nếu hội viên có **gói theo thời gian (`TIME`)** hoặc **gói theo lượt (`SESSION`)** đang ở trạng thái `ACTIVE` và còn hiệu lực.
  - **Gói tập cá nhân 1-1 (`PT`) tuyệt đối không dùng để check-in** vào phòng tập.
  - Trường hợp hội viên sở hữu đồng thời cả gói `TIME` và gói `SESSION`, hệ thống **ưu tiên sử dụng gói `TIME`** và không trừ lượt của gói `SESSION`.
  - Đối với gói `SESSION`: Hệ thống chỉ trừ **1 buổi ở lần check-in đầu tiên trong ngày** (căn cứ theo ngày dương lịch giờ Việt Nam `UTC+7`); các lần check-in tiếp theo trong cùng ngày không bị trừ thêm.
- Tự động kiểm tra lịch đặt lớp học nhóm (`class_bookings` thuộc F8) của hội viên trong ngày: nếu hội viên có lịch học sắp diễn ra (trong khoảng trước giờ học 30–60 phút), hệ thống tự động cập nhật mốc thời gian **đã đến (`arrived_at`)** cho chỗ đặt lớp.
- Ghi nhận lượt check-out khi hội viên rời cơ sở tập luyện để phục vụ tính toán số lượng hội viên đang có mặt theo thời gian thực (Dashboard F6).
- Đảm bảo thời gian phản hồi tra cứu và thực hiện check-in **dưới 2 giây** (NF1).

---

## 2. Danh sách Use Case

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F4.1** | Tra cứu hội viên và đối chiếu thông tin/ảnh tại quầy | Lễ tân, Quản lý chi nhánh | Cao | Sprint 2 |
| **UC-F4.2** | Thực hiện check-in thủ công tại quầy | Lễ tân, Quản lý chi nhánh | Cao | Sprint 2 |
| **UC-F4.3** | Tự động đánh dấu có mặt lớp học nhóm khi check-in | Hệ thống (tự động) | Cao | Sprint 2 |
| **UC-F4.4** | Ghi nhận check-out cho hội viên | Lễ tân, Quản lý chi nhánh | Trung bình | Sprint 2 |
| **UC-F4.5** | Xem lịch sử lượt check-in theo ngày tại quầy | Lễ tân, Quản lý chi nhánh | Trung bình | Sprint 2 |

---

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F4.1: Tra cứu hội viên và đối chiếu thông tin/ảnh tại quầy

- **Mã UC:** UC-F4.1
- **Tên Use Case:** Tra cứu hội viên và đối chiếu thông tin/ảnh tại quầy
- **Tác nhân chính:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`)
- **Mô tả ngắn gọn:** Lễ tân nhập mã hội viên hoặc số điện thoại để tìm kiếm hồ sơ và hiển thị ảnh đại diện, thông tin gói tập hiện có nhằm đối chiếu trước khi cho vào tập.

#### A. Tiền điều kiện
- Lễ tân/quản lý đã đăng nhập thành công vào hệ thống với token JWT hợp lệ.
- Tài khoản thao tác thuộc chi nhánh hiện tại.

#### B. Hậu điều kiện
- Thông tin cá nhân, ảnh đại diện, danh sách các gói tập hiện có và trạng thái của từng gói được hiển thị rõ ràng trên màn hình quầy lễ tân.

#### C. Luồng sự kiện chính
1. Lễ tân nhập từ khóa tìm kiếm (Mã hội viên ví dụ `HV000001` hoặc Số điện thoại ví dụ `0912345678`) vào thanh tìm kiếm check-in.
2. Hệ thống chuẩn hóa chuỗi tìm kiếm (xóa khoảng trắng thừa, chuẩn hóa SĐT về 10 chữ số).
3. Hệ thống kiểm tra vai trò người thao tác (phải là `RECEPTIONIST` hoặc `BRANCH_MANAGER` tại chi nhánh; không cho phép `SUPER_ADMIN` check-in tại quầy vì không thuộc biên chế chi nhánh cụ thể).
4. Hệ thống tra cứu bảng `members` chỉ trong chi nhánh của nhân viên (`members.home_branch_id = current_branch_id`, theo quyết định kick-off 02/11; tra cứu và check-in liên chi nhánh để đến v4) kết hợp với bảng `memberships`, và trả về thông tin trong vòng dưới 2 giây (NF1):
   - Thông tin cá nhân: Họ tên, Mã hội viên, Số điện thoại, Giới tính, Ảnh đại diện (`avatar_url`), Trạng thái tài khoản.
   - Danh sách **mọi** gói tập của hội viên ở mọi trạng thái (không lọc như bước 4 của UC-F4.2), để lễ tân giải thích được khi check-in bị từ chối: Tên gói, Loại gói (`TIME`, `SESSION`, `PT`), Trạng thái gói (`ACTIVE`, `UPCOMING`, `EXPIRED`, `FROZEN`), Ngày bắt đầu, Ngày hết hạn, Số buổi còn lại (với gói `SESSION`/`PT`).
   - Lịch học nhóm trong ngày (nếu có buổi học sắp bắt đầu).
5. Lễ tân đối chiếu ảnh đại diện hiển thị trên màn hình với khách hàng đang đứng tại quầy để xác minh danh tính.

#### D. Các luồng thay thế và ngoại lệ
- **E1 – Từ khóa tìm kiếm không đúng định dạng (400 Bad Request):**
  - Từ khóa rỗng hoặc không khớp định dạng mã `HV\d{6}` hoặc SĐT `^0\d{9}$`.
  - Hệ thống báo lỗi: *"Vui lòng nhập mã hội viên hợp lệ (ví dụ: HV000123) hoặc số điện thoại 10 chữ số."*
- **E2 – Không tìm thấy hội viên (404 Not Found):**
  - Không có bản ghi nào khớp trong hệ thống, hoặc hội viên thuộc chi nhánh khác (xử lý như không tìm thấy, không tiết lộ hồ sơ của chi nhánh khác – S3).
  - Hệ thống trả về `ProblemDetail` (404): *"Không tìm thấy thông tin hội viên trong hệ thống. Vui lòng kiểm tra lại mã hoặc số điện thoại."*
- **E3 – Hội viên đang bị ngừng hoạt động (vẫn trả 200 OK):**
  - Bản ghi `members.status = 'INACTIVE'` hoặc tài khoản liên kết `users.status = 'DISABLED'`.
  - Tra cứu vẫn trả 200 kèm trường `status` để màn hình hiện hồ sơ, giúp lễ tân giải thích cho hội viên. Giao diện hiển thị cảnh báo màu đỏ: *"Hồ sơ hội viên này đã ngừng hoạt động. Vui lòng liên hệ quản lý để được hỗ trợ."* và vô hiệu hóa nút Check-in.
  - Nếu vẫn gửi `POST /api/v1/checkins` cho hội viên này, backend từ chối theo E7 của UC-F4.2 (kiểm tra ở backend, không dựa vào nút bị vô hiệu hóa).

---

### 3.2. UC-F4.2: Thực hiện check-in thủ công tại quầy

- **Mã UC:** UC-F4.2
- **Tên Use Case:** Thực hiện check-in thủ công tại quầy
- **Tác nhân chính:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`)
- **Tác nhân phụ:** Hệ thống nền (kích hoạt UC-F4.3 đánh dấu có mặt lớp nhóm)
- **Mô tả ngắn gọn:** Lễ tân bấm nút xác nhận check-in cho hội viên sau khi đã đối chiếu ảnh, hệ thống tự động chọn gói hợp lệ, trừ buổi nếu là gói theo lượt và ghi nhận lượt vào.

#### A. Tiền điều kiện
- Lễ tân đã tra cứu thành công hồ sơ hội viên tại UC-F4.1.
- Thời điểm check-in nằm trong khung giờ mở cửa của chi nhánh: từ `branches.open_time` đến trước `branches.close_time`.

#### B. Hậu điều kiện
- **Thành công:**
  - Một bản ghi mới được tạo trong bảng `checkins` với `checked_in_at = now()`, `method = 'MANUAL'`, `checkin_date = :today` (ngày hiện tại theo giờ Việt Nam, xem BR-F4.3).
  - Nếu sử dụng gói `SESSION` và đây là lần đầu tiên check-in trong ngày: `session_deducted = true`, đồng thời giảm `memberships.remaining_sessions` đi 1 đơn vị. Nếu số buổi còn lại về 0, gói **vẫn giữ trạng thái `ACTIVE` đến hết ngày** (để hội viên có thể quay lại tập tiếp trong ngày mà không bị trừ thêm/từ chối); tác vụ nền ban đêm (batch job 23:59:59) sẽ chuyển gói về `EXPIRED`.
  - Nếu sử dụng gói `TIME` hoặc gói `SESSION` đã trừ buổi trong ngày: `session_deducted = false`, không thay đổi số buổi.
  - Nếu hội viên có lịch lớp học nhóm trong khung giờ từ `now() - 15 phút` đến `now() + 60 phút`: cập nhật `class_bookings.arrived_at = now()`.
  - Màn hình hiển thị thông báo check-in thành công màu xanh lá, kèm âm thanh thông báo và thông tin gói tập được áp dụng.
- **Thất bại:** Giao dịch bị rollback, không tạo bản ghi `checkins`, không trừ số buổi của gói.

#### C. Luồng sự kiện chính
1. Lễ tân bấm nút **"Xác nhận Check-in"** trên giao diện chi tiết hội viên.
2. Hệ thống kiểm tra giờ hiện tại (theo giờ Việt Nam) có thỏa `open_time <= giờ hiện tại < close_time` của chi nhánh hay không (đúng giờ đóng cửa là ngoài giờ; migration bảo đảm `close_time > open_time` nên không có khung qua nửa đêm).
3. Hệ thống kiểm tra quyền thao tác của nhân viên tại chi nhánh hiện tại, và kiểm tra hồ sơ hội viên thuộc chi nhánh này (`members.home_branch_id = current_branch_id`, nếu không trả 404 như E2 của UC-F4.1) và còn hoạt động (`members.status = 'ACTIVE'`; khi hồ sơ chuyển `INACTIVE` thì `users.status` cũng chuyển `DISABLED` cùng lúc). Hồ sơ ngừng hoạt động thì từ chối theo E7.
4. Hệ thống tải toàn bộ gói tập của hội viên (`memberships`) thỏa mãn:
   - `member_id = member.id`
   - `status = 'ACTIVE'`
   - `start_date <= :today` và `end_date >= :today`
   - Có phạm vi sử dụng tại chi nhánh hiện tại (`branch_id IS NULL` - toàn chuỗi hoặc `branch_id = current_branch_id`).
5. **Hệ thống lựa chọn gói tập theo thứ tự ưu tiên nghiệp vụ:**
   - **Trường hợp 1 (Có gói TIME):** Nếu hội viên có gói `package_type = 'TIME'` đang `ACTIVE` → Chọn gói `TIME` này. Đặt `membership_id = gói_TIME.id`, `session_deducted = false`. (Quy tắc nghiệp vụ: Có cả gói TIME và SESSION thì dùng gói TIME, không trừ buổi).
   - **Trường hợp 2 (Chỉ có gói SESSION):** Nếu hội viên không có gói `TIME`, nhưng có gói `package_type = 'SESSION'` đang `ACTIVE`:
     - Kiểm tra trong bảng `checkins`: xem hội viên với gói này đã có lượt check-in nào trong ngày (`checkin_date = :today`) mà `session_deducted = true` hay chưa.
     - Nếu **chưa có lượt trừ buổi nào trong ngày**:
       - Nếu `remaining_sessions > 0`: Hệ thống trừ 1 buổi (`remaining_sessions = remaining_sessions - 1`), đặt `session_deducted = true`. **Lưu ý nghiệp vụ:** Kể cả khi số buổi còn lại giảm về 0, gói tập **vẫn giữ trạng thái `ACTIVE` đến hết ngày** (để hội viên có thể check-in lại trong ngày). Tác vụ nền ban đêm sẽ chuyển sang `EXPIRED`.
       - Nếu `remaining_sessions <= 0`: Hệ thống từ chối check-in với mã lỗi 422 (xem luồng ngoại lệ E4).
     - Nếu **đã có lượt trừ buổi trong ngày**:
       - Kể cả khi `remaining_sessions = 0`, hệ thống vẫn cho phép check-in vào phòng tập bình thường theo quy tắc một ngày chỉ trừ tối đa một buổi. Giữ nguyên số buổi (0 buổi) và đặt `session_deducted = false`.
   - **Trường hợp 3 (Chỉ có gói PT hoặc không có gói hợp lệ):** Hệ thống từ chối check-in (xem luồng ngoại lệ E3, E4).
6. Hệ thống tạo bản ghi mới vào bảng `checkins`:
   - `member_id`: ID hội viên
   - `membership_id`: ID gói tập đã chọn ở bước 5
   - `branch_id`: Chi nhánh của lễ tân
   - `created_by`: ID tài khoản lễ tân thực hiện
   - `method`: `'MANUAL'`
   - `checkin_date`: Ngày hiện tại theo giờ Việt Nam
   - `session_deducted`: `true` hoặc `false`
   - `checked_in_at`: Thời điểm hiện tại (UTC)
7. Hệ thống tự động kích hoạt **UC-F4.3** kiểm tra và đánh dấu lớp học nhóm sắp diễn ra.
8. Hệ thống lưu toàn bộ thay đổi trong một giao dịch cơ sở dữ liệu (`@Transactional`).
9. Hệ thống trả về kết quả check-in thành công (`201 Created`) với thông tin chi tiết: Tên hội viên, Mã gói sử dụng, Loại gói, Trạng thái trừ buổi, Số buổi còn lại (nếu là gói SESSION). Thời gian phản hồi < 2s (NF1).

#### D. Các luồng thay thế và ngoại lệ
- **E1 – Ngoài giờ mở cửa phòng tập (422 Unprocessable Content):**
  - Thời gian check-in trước `branches.open_time` hoặc từ `branches.close_time` trở đi.
  - Hệ thống từ chối: *"Phòng tập hiện đang đóng cửa (Giờ mở cửa: {open_time} - {close_time}). Không thể thực hiện check-in."*
- **E2 – Không có gói tập nào đang hoạt động (422 Unprocessable Content):**
  - Hội viên chưa mua gói nào hoặc mọi gói đều đã hết hạn (`EXPIRED`), bị hủy (`CANCELLED`), hoặc đang bảo lưu (`FROZEN`).
  - Hệ thống từ chối: *"Hội viên không có gói tập nào đang hoạt động. Vui lòng đăng ký hoặc gia hạn gói tập tại quầy."*
- **E3 – Hội viên chỉ có gói tập cá nhân PT (422 Unprocessable Content):**
  - Hội viên chỉ sở hữu gói `package_type = 'PT'`.
  - Hệ thống từ chối theo quy tắc nghiệp vụ: *"Gói huấn luyện viên cá nhân (PT) chỉ dùng cho các buổi tập 1-1 có lịch hẹn, không dùng để check-in tự do vào phòng tập. Hội viên cần có thêm gói theo thời gian hoặc theo lượt."*
- **E4 – Gói theo lượt đã hết số buổi (422 Unprocessable Content):**
  - Gói `SESSION` có `remaining_sessions <= 0` và trong ngày hôm nay hội viên chưa có lượt check-in nào được ghi nhận trừ buổi (`session_deducted = true`).
  - Hệ thống từ chối: *"Gói tập theo buổi của hội viên đã hết lượt sử dụng. Vui lòng gia hạn thêm gói mới."*
- **E5 – Gói tập đang bị bảo lưu (422 Unprocessable Content):**
  - Gói tập duy nhất của hội viên đang ở trạng thái `FROZEN`.
  - Hệ thống từ chối: *"Gói tập của hội viên đang trong thời gian bảo lưu (từ ngày... đến ngày...). Vui lòng liên hệ quản lý để hủy bảo lưu sớm nếu muốn tập tiếp."*
- **E6 – Xung đột lượt trừ buổi đồng thời (409 Conflict):**
- **E6 – Xung đột lượt trừ buổi đồng thời (409 Conflict):**
  - Hai yêu cầu check-in cùng lúc vi phạm khóa lạc quan `@Version` trên bảng `memberships` hoặc vi phạm chỉ mục duy nhất có điều kiện `uq_one_deduction_per_day` (`WHERE session_deducted = true`).
  - Hệ thống hủy giao dịch bị xung đột và trả về mã lỗi **409 Conflict** (thông qua `GlobalExceptionHandler` có sẵn) với thông báo: *"Dữ liệu lượt check-in đang được xử lý đồng thời bởi một thao tác khác. Vui lòng thử lại."* Khi nhân viên bấm thử lại (retry), hệ thống sẽ nhận diện đã có lượt trừ buổi và cho phép check-in thành công với `session_deducted = false`.
- **E7 – Hồ sơ hội viên ngừng hoạt động (422 Unprocessable Content):**
  - `members.status = 'INACTIVE'` (kiểm tra ở bước 3), kể cả khi hội viên vẫn còn gói tập `ACTIVE`.
  - Hệ thống từ chối: *"Hồ sơ hội viên này đã ngừng hoạt động. Vui lòng liên hệ quản lý để được hỗ trợ."*

---

### 3.3. UC-F4.3: Tự động đánh dấu có mặt lớp học nhóm khi check-in

- **Mã UC:** UC-F4.3
- **Tên Use Case:** Tự động đánh dấu có mặt lớp học nhóm khi check-in
- **Tác nhân chính:** Hệ thống (tự động kích hoạt sau khi UC-F4.2 thành công)
- **Mô tả ngắn gọn:** Kiểm tra xem hội viên có đặt chỗ lớp học nhóm (`class_bookings`) bắt đầu trong khung giờ từ `now() - 15 phút` đến `now() + 60 phút` hay không (đến sớm tối đa 60 phút hoặc đến trễ tối đa 15 phút so với giờ bắt đầu). Nếu có, tự động ghi nhận thời điểm đến (`arrived_at = now()`) để huấn luyện viên dễ dàng điểm danh tại lớp.

#### A. Tiền điều kiện
- Lượt check-in tại UC-F4.2 vừa được tạo thành công.

#### B. Hậu điều kiện
- Bản ghi `class_bookings` tương ứng có `arrived_at` được gán mốc thời gian hiện tại.

#### C. Luồng sự kiện chính
1. Hệ thống tìm kiếm các bản ghi trong `class_bookings` của `member_id` có:
   - `status = 'BOOKED'`
   - `class_sessions.start_at` nằm trong khoảng từ `now() - 15 phút` đến `now() + 60 phút`
   - `class_sessions.branch_id = current_branch_id`
   - `arrived_at IS NULL`
2. Nếu tìm thấy bản ghi thỏa mãn, hệ thống cập nhật `arrived_at = now()`.
3. Thông tin về lớp học sắp diễn ra (Tên loại lớp, phòng, giờ bắt đầu) được đính kèm vào dữ liệu phản hồi check-in để lễ tân nhắc nhở hội viên (ví dụ: *"Hội viên có lớp Yoga tại Phòng 201 bắt đầu lúc 18:00"*).

---

### 3.4. UC-F4.4: Ghi nhận check-out cho hội viên

- **Mã UC:** UC-F4.4
- **Tên Use Case:** Ghi nhận check-out cho hội viên
- **Tác nhân chính:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`)
- **Mô tả ngắn gọn:** Ghi nhận giờ ra về (`checked_out_at`) của hội viên để giải phóng trạng thái "đang có mặt" trên Dashboard thống kê.

#### A. Tiền điều kiện
- Hội viên đã check-in trong ngày và bản ghi `checkins.checked_out_at` đang để trống (`NULL`).

#### B. Hậu điều kiện
- Bản ghi `checkins` gần nhất của hội viên được cập nhật `checked_out_at = now()`.
- Số lượng hội viên đang có mặt tại phòng tập trên Dashboard (F6) giảm đi 1.

#### C. Luồng sự kiện chính
1. Lễ tân tìm kiếm hội viên hoặc chọn từ danh sách "Hội viên đang tập".
2. Lễ tân bấm nút **"Check-out"**.
3. Hệ thống tìm bản ghi `checkins` theo `id` được gửi lên, kèm điều kiện `branch_id` bằng chi nhánh của nhân viên thao tác và `checked_out_at IS NULL`. Lượt check-in của chi nhánh khác được xử lý như không tìm thấy (E1), không tiết lộ là bản ghi có tồn tại (S3).
4. Hệ thống cập nhật `checked_out_at = now()`, `updated_at = now()`.
5. Hệ thống hiển thị thông báo check-out thành công kèm tổng thời gian tập luyện của hội viên (tính theo phút/giờ).

#### D. Các luồng thay thế và ngoại lệ
- **E1 – Không tìm thấy lượt check-in chưa check-out (404 Not Found):**
  - Hội viên chưa thực hiện check-in trước đó, đã check-out rồi, hoặc lượt check-in thuộc chi nhánh khác.
  - Hệ thống báo: *"Hội viên không có lượt tập nào đang diễn ra cần check-out."*

---

### 3.5. UC-F4.5: Xem lịch sử lượt check-in theo ngày tại quầy

- **Mã UC:** UC-F4.5
- **Tên Use Case:** Xem lịch sử lượt check-in theo ngày tại quầy
- **Tác nhân chính:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`)
- **Mô tả ngắn gọn:** Lễ tân/quản lý xem danh sách các lượt check-in trong ngày hoặc khoảng ngày tại chi nhánh, hỗ trợ lọc theo trạng thái (đang tập / đã về) và tìm kiếm theo tên hội viên.

#### A. Tiền điều kiện
- Người dùng đã đăng nhập với vai trò `RECEPTIONIST` hoặc `BRANCH_MANAGER`.

#### B. Hậu điều kiện
- Danh sách check-in được hiển thị phân trang (`PageResponse`), sắp xếp theo thời gian check-in giảm dần.

#### C. Luồng sự kiện chính
1. Lễ tân truy cập màn hình **"Nhật ký Check-in"**.
2. Hệ thống mặc định tải danh sách check-in của ngày hôm nay tại chi nhánh hiện tại.
3. Người dùng có thể lọc theo: Ngày tập, Trạng thái (Đang tập: `checked_out_at IS NULL` / Đã ra về: `checked_out_at IS NOT NULL`), Phương thức (`MANUAL` / `FACE`), hoặc tìm theo tên/mã hội viên.
4. Hệ thống trả về dữ liệu phân trang: Mã check-in, Mã hội viên, Họ tên hội viên, Ảnh đại diện, Tên gói tập, Giờ vào, Giờ ra, Nhân viên thực hiện.

---

## 4. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F4.1 (Gói tập hợp lệ):** Check-in chỉ chấp nhận gói có loại `TIME` hoặc `SESSION` ở trạng thái `ACTIVE`, còn trong thời hạn sử dụng và hợp lệ tại chi nhánh. Gói `PT` không được dùng để check-in tự do (chốt ngày 01/11).
- **BR-F4.2 (Ưu tiên gói TIME):** Nếu hội viên sở hữu cả gói `TIME` và `SESSION` đang cùng `ACTIVE`, hệ thống bắt buộc sử dụng gói `TIME` và không trừ buổi của gói `SESSION` (chốt ngày 01/11).
- **BR-F4.3 (Quy tắc trừ buổi một lần mỗi ngày):** Với gói `SESSION`, một ngày dương lịch theo giờ Việt Nam (`UTC+7`) chỉ bị trừ tối đa 1 buổi tại lượt check-in đầu tiên. Các lượt check-in kế tiếp trong ngày không bị trừ thêm (`session_deducted = false`). Ràng buộc CSDL bằng chỉ mục duy nhất có điều kiện: `CREATE UNIQUE INDEX uq_one_deduction_per_day ON checkins (membership_id, checkin_date) WHERE session_deducted = true;`. Ngày hiện tại (`:today`) do ứng dụng tính theo `ZoneId.of("Asia/Ho_Chi_Minh")` rồi truyền vào truy vấn; không dùng `CURRENT_DATE` của PostgreSQL, vì máy chủ chạy theo UTC nên từ 00:00 đến 07:00 giờ Việt Nam `CURRENT_DATE` vẫn là ngày hôm trước.
- **BR-F4.4 (Giờ mở cửa phòng tập):** Giờ mở cửa và đóng cửa đọc từ `branches.open_time`, `branches.close_time` của chi nhánh (theo ERD), không viết cứng mã nguồn. Được check-in khi `open_time <= giờ hiện tại < close_time`.
- **BR-F4.5 (Hiệu năng):** Phản hồi tra cứu hội viên và xác nhận check-in phải đạt dưới 2 giây trên tập dữ liệu 10.000 hội viên (NF1).
- **BR-F4.6 (Phân quyền):** Chỉ tài khoản nhân viên gắn với chi nhánh mới được thực hiện check-in cho chi nhánh đó (S3). Dữ liệu check-in được tự động gắn `branch_id` của nhân viên thao tác.

---

## 5. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động trực tiếp:** `checkins`, `members`, `memberships`, `class_bookings`
- **Bảng tham chiếu liên quan:** `branches`, `users`, `class_sessions`

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `checkins` | `id` | `bigint` | PK, Generated Always | Khóa chính tự tăng |
| `checkins` | `member_id` | `bigint` | NOT NULL, FK -> members | Hội viên check-in |
| `checkins` | `membership_id`| `bigint` | NOT NULL, FK -> memberships | Gói tập được áp dụng |
| `checkins` | `branch_id` | `bigint` | NOT NULL, FK -> branches | Chi nhánh tập luyện |
| `checkins` | `created_by` | `bigint` | NOT NULL, FK -> users | Lễ tân thực hiện |
| `checkins` | `method` | `varchar(20)`| NOT NULL, CHECK ('MANUAL', 'FACE') | Phương thức check-in (v1 là MANUAL) |
| `checkins` | `match_score` | `numeric(5,4)`| NULL | Điểm khớp khuôn mặt (dùng ở v5) |
| `checkins` | `checkin_date`| `date` | NOT NULL | Ngày theo giờ Việt Nam |
| `checkins` | `session_deducted`| `boolean`| NOT NULL, DEFAULT false | Đánh dấu lượt này có trừ buổi hay không |
| `checkins` | `checked_in_at`| `timestamptz` | NOT NULL, DEFAULT now() | Thời điểm vào (UTC) |
| `checkins` | `checked_out_at`| `timestamptz` | NULL | Thời điểm ra (UTC) |

---

## 6. Thiết kế API nháp

### 6.1. Tra cứu hội viên cho màn hình check-in
- **Endpoint:** `GET /api/v1/checkins/search-member?keyword={keyword}`
- **Quyền truy cập:** `RECEPTIONIST`, `BRANCH_MANAGER`
- **Response Success (200 OK):**
  ```json
  {
    "memberId": 12,
    "memberCode": "HV000012",
    "fullName": "Nguyễn Văn An",
    "phone": "0912345678",
    "avatarUrl": "/avatars/hv000012.jpg",
    "status": "ACTIVE",
    "isCurrentlyCheckedIn": false,
    "activeMemberships": [
      {
        "id": 45,
        "packageName": "Gói Tiêu Chuẩn 1 Tháng",
        "packageType": "TIME",
        "startDate": "2026-11-01",
        "endDate": "2026-12-01",
        "status": "ACTIVE"
      }
    ],
    "upcomingClassSession": {
      "bookingId": 105,
      "className": "Hatha Yoga",
      "roomName": "Phòng Yoga 01",
      "startTime": "2026-11-02T18:00:00+07:00"
    }
  }
  ```

### 6.2. Thực hiện check-in thủ công
- **Endpoint:** `POST /api/v1/checkins`
- **Quyền truy cập:** `RECEPTIONIST`, `BRANCH_MANAGER`
- **Request Body:**
  ```json
  {
    "memberId": 12
  }
  ```
- **Response Success (201 Created):**
  ```json
  {
    "checkinId": 340,
    "memberId": 12,
    "memberCode": "HV000012",
    "fullName": "Nguyễn Văn An",
    "appliedMembership": {
      "id": 45,
      "packageType": "TIME",
      "sessionDeducted": false,
      "remainingSessions": null
    },
    "checkedInAt": "2026-11-02T10:30:15Z",
    "classArrivalMarked": true,
    "message": "Check-in thành công. Chúc quý hội viên buổi tập hiệu quả!"
  }
  ```

### 6.3. Ghi nhận check-out
- **Endpoint:** `POST /api/v1/checkins/{id}/checkout`
- **Quyền truy cập:** `RECEPTIONIST`, `BRANCH_MANAGER`; lượt check-in `{id}` phải thuộc chi nhánh của nhân viên thao tác, nếu không trả 404 (xem UC-F4.4).
- **Response Success (200 OK):**
  ```json
  {
    "checkinId": 340,
    "checkedOutAt": "2026-11-02T12:15:00Z",
    "durationMinutes": 105,
    "message": "Check-out thành công. Hẹn gặp lại quý hội viên!"
  }
  ```

---

## 7. Tiêu chí chấp nhận (Acceptance Criteria)

- **Scenario 1: Check-in thành công với gói TIME**
  - **Given** Hội viên có mã `HV000001` đang sở hữu gói `TIME` trạng thái `ACTIVE` còn hạn đến ngày 30/11/2026.
  - **When** Lễ tân bấm xác nhận check-in cho hội viên vào lúc 08:00.
  - **Then** Hệ thống tạo bản ghi `checkins` mới với `session_deducted = false`, trả về mã 201 Created và không trừ bất kỳ buổi nào.

- **Scenario 2: Check-in với gói SESSION lần đầu trong ngày**
  - **Given** Hội viên có gói `SESSION` trạng thái `ACTIVE` còn 10 buổi và chưa check-in lần nào trong ngày hôm nay.
  - **When** Lễ tân bấm xác nhận check-in cho hội viên.
  - **Then** Hệ thống tạo bản ghi `checkins` với `session_deducted = true`, trừ `remaining_sessions` còn 9, và trả về mã 201 Created.

- **Scenario 3: Check-in với gói SESSION lần thứ hai trong cùng ngày**
  - **Given** Hội viên ở Scenario 2 quay lại phòng tập vào buổi chiều cùng ngày.
  - **When** Lễ tân bấm xác nhận check-in lần nữa.
  - **Then** Hệ thống tạo bản ghi `checkins` với `session_deducted = false`, giữ nguyên số buổi là 9, và trả về mã 201 Created.

- **Scenario 4: Chặn check-in khi hội viên chỉ có gói PT**
  - **Given** Hội viên chỉ sở hữu 01 gói `PT` (không có gói `TIME` hoặc `SESSION`).
  - **When** Lễ tân bấm xác nhận check-in.
  - **Then** Hệ thống từ chối với mã 422 Unprocessable Content và thông báo gói PT không dùng để check-in tự do.

- **Scenario 5: Ưu tiên gói TIME khi có cả TIME và SESSION**
  - **Given** Hội viên sở hữu đồng thời gói `TIME` 1 tháng (ACTIVE) và gói `SESSION` 30 buổi (ACTIVE).
  - **When** Lễ tân bấm xác nhận check-in.
  - **Then** Hệ thống tự động áp dụng gói `TIME`, ghi nhận `session_deducted = false`, giữ nguyên 30 buổi của gói `SESSION`.

- **Scenario 6: Hội viên dùng hết buổi cuối nhưng quay lại tập lần 2 trong ngày (Trường hợp chị Lan)**
  - **Given** Hội viên Lan sở hữu gói `SESSION` còn đúng 1 buổi.
  - **And** Vào lúc 08:00 sáng, chị Lan check-in thành công: gói bị trừ còn 0 buổi (`session_deducted = true`), gói vẫn duy trì trạng thái `ACTIVE` đến hết ngày.
  - **When** Đến 17:00 chiều cùng ngày, chị Lan quay lại phòng tập check-in lần thứ 2.
  - **Then** Hệ thống phát hiện hôm nay đã có lượt trừ buổi cho gói này, cho phép check-in thành công với mã 201 Created, `session_deducted = false`, số buổi giữ nguyên là 0, gói vẫn là `ACTIVE` (không bị từ chối E4).

- **Scenario 7: Xung đột check-in đồng thời và thử lại thành công (Xử lý E6)**
  - **Given** Hai lễ tân cùng lúc bấm xác nhận check-in cho hội viên Nam (đang dùng gói `SESSION` còn 5 buổi).
  - **When** Cả hai yêu cầu gửi đồng thời đến máy chủ và cạnh tranh trừ buổi.
  - **Then** Một yêu cầu thành công (201 Created, trừ 1 buổi còn 4, `session_deducted = true`). Yêu cầu còn lại gặp xung đột trả về mã **409 Conflict**.
  - **And When** Lễ tân của yêu cầu bị lỗi 409 bấm thử lại (retry) ngay sau đó.
  - **Then** Hệ thống phát hiện đã có lượt trừ buổi trong ngày, xử lý thành công trả về mã **201 Created**, ghi nhận `session_deducted = false` và không trừ thêm buổi nào của hội viên Nam.
