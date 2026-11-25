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
  - **Gói tập cá nhân 1-1 (`PT`):** Không dùng để check-in tự do vào phòng tập. Hội viên chỉ sở hữu gói PT được phép check-in vào đúng ngày có lịch hẹn tập cá nhân (`BOOKED`) hợp lệ tại chi nhánh với huấn luyện viên (BR-F4.8, chốt 19/11/2026); lượt check-in gắn gói PT và không trừ số buổi PT.
  - Trường hợp hội viên sở hữu đồng thời cả gói `TIME` và gói `SESSION`, hệ thống **ưu tiên sử dụng gói `TIME`** và không trừ lượt của gói `SESSION`.
  - Đối với gói `SESSION`: Hệ thống chỉ trừ **1 buổi ở lần check-in đầu tiên trong ngày** (căn cứ theo ngày dương lịch giờ Việt Nam `UTC+7`); các lần check-in tiếp theo trong cùng ngày không bị trừ thêm.
- Tự động kiểm tra lịch đặt lớp học nhóm (`class_bookings` thuộc F8) của hội viên trong ngày: nếu hội viên có chỗ lớp `BOOKED` bắt đầu trong khoảng `[thời điểm check-in − 15 phút, thời điểm check-in + 60 phút]` (tức đến sớm tối đa 60 phút hoặc đến muộn tối đa 15 phút, xem UC-F4.3; chốt 22/11/2026), hệ thống tự động cập nhật mốc thời gian **đã đến (`arrived_at`)** cho chỗ đặt lớp.
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
   - Thông tin cá nhân: Họ tên, Mã hội viên, Số điện thoại, Giới tính, cờ ảnh đại diện (`hasAvatar`, xem ADR 0011 / VD-20), Trạng thái tài khoản.
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

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F4.1.1: Tra cứu và đối chiếu ảnh tại quầy
- **User story:** Là **Lễ tân hoặc Quản lý chi nhánh**, tôi muốn **tra cứu hồ sơ hội viên và đối chiếu ảnh đại diện tại quầy**, để **xác minh danh tính trước khi cho hội viên vào tập**.
- **BR liên quan:** BR-F4.1, BR-F4.6; S3 (bảo mật dữ liệu chi nhánh), NF1 (thời gian phản hồi < 2s).
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #135
- **Tiêu chí chấp nhận:**
  - **AC-F4.1.1-01: Tìm theo mã hội viên hoặc số điện thoại** (BR-F4.6, NF1)
    - **Given** Hội viên `HV000012` thuộc chi nhánh làm việc của lễ tân/quản lý.
    - **When** Lễ tân/quản lý nhập mã hội viên (kể cả chữ thường, khoảng trắng thừa) hoặc số điện thoại (10 chữ số, kể cả có khoảng trắng).
    - **Then** Hệ thống trả về mã `200 OK` cùng hồ sơ chi tiết hội viên, danh sách **mọi gói tập ở mọi trạng thái** (`ACTIVE`, `UPCOMING`, `EXPIRED`, `FROZEN`) kèm tên gói và trạng thái; trả cờ `hasAvatar` (cho biết hội viên đã có ảnh đại diện hay chưa), và cờ `isCurrentlyCheckedIn` (cho biết có lượt chưa check-out trong ngày không).
  - **AC-F4.1.1-02: Hiển thị ảnh đại diện và thông tin lớp học sắp diễn ra** (VD-20, ADR 0011)
    - **Given** Hội viên đã có ảnh đại diện và đang có chỗ lớp học nhóm `BOOKED` sắp diễn ra hôm nay tại chi nhánh.
    - **When** Lễ tân thực hiện tra cứu.
    - **Then** Giao diện hiển thị ảnh đại diện kích thước lớn để đối chiếu trực tiếp với người đang đứng tại quầy; hiển thị chi tiết lớp học sắp diễn ra gồm tên lớp, phòng tập, giờ bắt đầu (theo giờ Việt Nam `UTC+7`). Nếu hội viên chưa có ảnh đại diện (`hasAvatar = false`), giao diện hiển thị ảnh mặc định và không gọi API lấy ảnh.
  - **AC-F4.1.1-03: Từ khóa tìm kiếm sai định dạng** (E1)
    - **Given** Từ khóa tìm kiếm bị để trống, hoặc không khớp định dạng mã `HV` + 6 chữ số (`HV\d{6}`) và không phải số điện thoại 10 chữ số bắt đầu bằng 0 (`0\d{9}`).
    - **When** Người dùng gửi yêu cầu tra cứu.
    - **Then** Hệ thống trả về `400 Bad Request` kèm thông báo lỗi E1: *"Vui lòng nhập mã hội viên hợp lệ (ví dụ: HV000123) hoặc số điện thoại 10 chữ số."*; giao diện hiển thị câu thông báo lỗi này.
  - **AC-F4.1.1-04: Không tìm thấy hội viên hoặc hội viên thuộc chi nhánh khác** (E2, S3)
    - **Given** Không có bản ghi hội viên khớp trong hệ thống, hoặc hội viên thuộc chi nhánh khác (`home_branch_id` khác chi nhánh của nhân viên thao tác).
    - **When** Lễ tân thực hiện tra cứu.
    - **Then** Hệ thống trả về `404 Not Found` cùng câu thông báo thống nhất (E2): *"Không tìm thấy thông tin hội viên trong hệ thống. Vui lòng kiểm tra lại mã hoặc số điện thoại."* (không phân biệt giữa không tồn tại và khác chi nhánh để bảo mật dữ liệu – S3).
  - **AC-F4.1.1-05: Hồ sơ hội viên ngừng hoạt động** (E3)
    - **Given** Hồ sơ hội viên có trạng thái `INACTIVE` (`members.status = 'INACTIVE'`) hoặc tài khoản liên kết `users.status = 'DISABLED'`.
    - **When** Lễ tân tra cứu hội viên.
    - **Then** Hệ thống trả về `200 OK` kèm trạng thái `INACTIVE` để lễ tân xem thông tin giải thích cho khách; giao diện hiển thị cảnh báo viền đỏ nổi bật: *"Hồ sơ hội viên này đã ngừng hoạt động. Vui lòng liên hệ quản lý để được hỗ trợ."* và vô hiệu hóa nút Check-in.
  - **AC-F4.1.1-06: Phân quyền truy cập chức năng tra cứu** (BR-F4.6, S3)
    - **Given** Người dùng đăng nhập với các vai trò khác nhau (`RECEPTIONIST`, `BRANCH_MANAGER`, `SUPER_ADMIN`, `TRAINER`, `MEMBER`).
    - **When** Truy cập giao diện hoặc gọi API tra cứu check-in.
    - **Then** Lễ tân và Quản lý chi nhánh được phép truy cập (`200 OK`); Huấn luyện viên (`TRAINER`), Hội viên (`MEMBER`) và Chủ chuỗi (`SUPER_ADMIN` – do không gắn với chi nhánh cụ thể theo BR-F4.6) bị từ chối với mã lỗi `403 Forbidden` *"Bạn không có quyền thực hiện thao tác này."*; menu chức năng Check-in chỉ hiển thị cho Lễ tân và Quản lý chi nhánh.

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
  - Nếu sử dụng gói `SESSION` và đây là lần đầu tiên check-in trong ngày: `session_deducted = true`, đồng thời giảm `memberships.remaining_sessions` đi 1 đơn vị. Nếu số buổi còn lại về 0, gói **vẫn giữ trạng thái `ACTIVE` đến hết ngày** (để hội viên có thể quay lại tập tiếp trong ngày mà không bị trừ thêm/từ chối); tác vụ nền đầu ngày hôm sau chuyển gói về `EXPIRED`. Nếu hội viên đã có gói gia hạn nối tiếp (`UPCOMING`) cùng loại, gói đó được kéo lịch về bắt đầu từ ngày mai (BR-F4.7).
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
       - Nếu `remaining_sessions > 0`: Hệ thống trừ 1 buổi (`remaining_sessions = remaining_sessions - 1`), đặt `session_deducted = true`. **Lưu ý nghiệp vụ:** Kể cả khi số buổi còn lại giảm về 0, gói tập **vẫn giữ trạng thái `ACTIVE` đến hết ngày** (để hội viên có thể check-in lại trong ngày). Tác vụ nền đầu ngày hôm sau chuyển gói sang `EXPIRED`.
         - Nếu sau khi trừ, `remaining_sessions = 0`: hệ thống kéo lịch các gói `UPCOMING` nối tiếp theo BR-F4.7, trong cùng giao dịch.
       - Nếu `remaining_sessions <= 0`: Hệ thống từ chối check-in với mã lỗi 422 (xem luồng ngoại lệ E4).
     - Nếu **đã có lượt trừ buổi trong ngày**:
       - Kể cả khi `remaining_sessions = 0`, hệ thống vẫn cho phép check-in vào phòng tập bình thường theo quy tắc một ngày chỉ trừ tối đa một buổi. Giữ nguyên số buổi (0 buổi) và đặt `session_deducted = false`.
   - **Trường hợp 3 (Chỉ có gói PT, hôm nay có buổi PT, chốt 19/11/2026):** Nếu hội viên không có gói `TIME`/`SESSION` dùng được, nhưng có gói `PT` đang `ACTIVE` và **hôm nay (giờ Việt Nam) có buổi PT `BOOKED` của chính hội viên tại chi nhánh này** → cho check-in bằng gói PT: `membership_id = gói_PT.id`, `session_deducted = false`. Check-in **không trừ buổi PT**; buổi PT chỉ trừ khi huấn luyện viên xác nhận hoặc ghi vắng (F9, BR-F9.6). Xem BR-F4.8.
   - **Trường hợp 4 (Chỉ có gói PT mà hôm nay không có buổi PT, hoặc không có gói hợp lệ):** Hệ thống từ chối check-in (xem luồng ngoại lệ E3, E4).
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
- **E3 – Hội viên chỉ có gói tập cá nhân PT và hôm nay không có buổi PT (422 Unprocessable Content):**
  - Hội viên chỉ sở hữu gói `package_type = 'PT'` và hôm nay không có buổi PT `BOOKED` tại chi nhánh này (BR-F4.8).
  - Hệ thống từ chối theo quy tắc nghiệp vụ: *"Gói huấn luyện viên cá nhân (PT) chỉ dùng cho các buổi tập 1-1 có lịch hẹn, không dùng để check-in tự do vào phòng tập. Hội viên cần có thêm gói theo thời gian hoặc theo lượt."*
- **E4 – Gói theo lượt đã hết số buổi (422 Unprocessable Content):**
  - Gói `SESSION` có `remaining_sessions <= 0` và trong ngày hôm nay hội viên chưa có lượt check-in nào được ghi nhận trừ buổi (`session_deducted = true`).
  - Hệ thống từ chối: *"Gói tập theo buổi của hội viên đã hết lượt sử dụng. Vui lòng gia hạn thêm gói mới."*
- **E5 – Gói tập đang bị bảo lưu (422 Unprocessable Content):**
  - Gói tập duy nhất của hội viên đang ở trạng thái `FROZEN`.
  - Hệ thống từ chối: *"Gói tập của hội viên đang trong thời gian bảo lưu (từ ngày... đến ngày...). Vui lòng liên hệ quản lý để hủy bảo lưu sớm nếu muốn tập tiếp."*
- **E6 – Xung đột lượt trừ buổi đồng thời (409 Conflict):**
  - Hai yêu cầu check-in cùng lúc vi phạm khóa lạc quan `@Version` trên bảng `memberships` hoặc vi phạm chỉ mục duy nhất có điều kiện `uq_one_deduction_per_day` (`WHERE session_deducted = true`).
  - Hệ thống hủy giao dịch bị xung đột và trả về mã lỗi **409 Conflict**. `GlobalExceptionHandler` có sẵn chỉ trả thông báo chung cho `OptimisticLockingFailureException` và `DataIntegrityViolationException`, nên service check-in ghi bằng `saveAndFlush` cả bản ghi `checkins`, gói đang dùng và các gói `UPCOMING` được kéo lịch theo BR-F4.7 (để lỗi phát sinh ngay trong service, không đợi lúc commit), bắt hai ngoại lệ này rồi ném `ConflictException` với thông báo: *"Dữ liệu lượt check-in đang được xử lý đồng thời bởi một thao tác khác. Vui lòng thử lại."* Khi nhân viên bấm thử lại (retry), hệ thống sẽ nhận diện đã có lượt trừ buổi và cho phép check-in thành công với `session_deducted = false`.
- **E7 – Hồ sơ hội viên ngừng hoạt động (422 Unprocessable Content):**
  - `members.status = 'INACTIVE'` (kiểm tra ở bước 3), kể cả khi hội viên vẫn còn gói tập `ACTIVE`.
  - Hệ thống từ chối: *"Hồ sơ hội viên này đã ngừng hoạt động. Vui lòng liên hệ quản lý để được hỗ trợ."*

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F4.2.1: Check-in thủ công tại quầy
- **User story:** Là **Lễ tân hoặc Quản lý chi nhánh**, tôi muốn **thực hiện check-in cho hội viên tại quầy**, để **ghi nhận lượt tập vào hệ thống, trừ buổi đúng quy định và mở quyền vào phòng tập**.
- **BR liên quan:** BR-F4.1, BR-F4.2, BR-F4.3, BR-F4.4, BR-F4.6, BR-F4.7, BR-F4.8; NF1 (thời gian phản hồi < 2s), NF6 (chống xung đột đồng thời).
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #136
- **Tiêu chí chấp nhận:**
  - **AC-F4.2.1-01: Check-in thành công với gói TIME** (BR-F4.1, BR-F4.6)
    - **Given** Hội viên có gói tập `TIME` ở trạng thái `ACTIVE` còn hạn, thời điểm check-in nằm trong khung giờ mở cửa của chi nhánh.
    - **When** Lễ tân bấm xác nhận check-in.
    - **Then** Hệ thống trả về `201 Created`; ghi nhận bản ghi `checkins` với phương thức `MANUAL`, chi nhánh và người tạo tương ứng; `session_deducted = false` và không trừ buổi nào. Giao diện hiển thị thông báo thành công màu xanh, hiển thị gói tập áp dụng và thông tin nhắc lớp học nhóm (nếu có).
  - **AC-F4.2.1-02: Xác định ngày check-in theo múi giờ Việt Nam** (BR-F4.3)
    - **Given** Thời điểm check-in là lúc 00:30 theo giờ Việt Nam (tương đương 17:30 UTC của ngày hôm trước).
    - **When** Thực hiện check-in cho hội viên.
    - **Then** Trường `checkin_date` được ghi nhận theo đúng ngày dương lịch tại Việt Nam (`UTC+7`), không bị lệch sang ngày hôm trước theo UTC của máy chủ.
  - **AC-F4.2.1-03: Gói SESSION trừ đúng một buổi mỗi ngày dương lịch** (BR-F4.3)
    - **Given** Hội viên sở hữu gói `SESSION` đang có `remaining_sessions = 10`.
    - **When** Hội viên check-in lần đầu tiên trong ngày; sau đó check-in lần thứ hai trong cùng ngày; rồi tiếp tục check-in sang ngày hôm sau.
    - **Then** Lần đầu tiên: `session_deducted = true`, số buổi giảm còn 9; lần thứ hai trong cùng ngày: `session_deducted = false`, số buổi vẫn giữ nguyên là 9; sang ngày mới: `session_deducted = true`, số buổi giảm còn 8.
  - **AC-F4.2.1-04: Ưu tiên gói TIME khi hội viên có cả TIME và SESSION** (BR-F4.2)
    - **Given** Hội viên sở hữu đồng thời gói `TIME` và gói `SESSION` đều đang ở trạng thái `ACTIVE` và còn hiệu lực.
    - **When** Lễ tân bấm xác nhận check-in.
    - **Then** Hệ thống tự động chọn áp dụng gói `TIME`; đặt `session_deducted = false`, gói `SESSION` giữ nguyên số buổi không bị trừ.
  - **AC-F4.2.1-05: Buổi tập cuối của gói SESSION vẫn được tập không giới hạn hết ngày** (BR-F4.3)
    - **Given** Hội viên sở hữu gói `SESSION` còn đúng 1 buổi.
    - **When** 08:00 sáng check-in (số buổi trừ về 0); sau đó 17:00 chiều cùng ngày quay lại check-in lần 2.
    - **Then** Lần check-in sáng (08:00): trừ 1 buổi về 0, gói vẫn giữ nguyên trạng thái `ACTIVE`; lần check-in chiều (17:00): hệ thống trả về `201 Created`, `session_deducted = false`, không trừ buổi và không bị từ chối hết lượt.
  - **AC-F4.2.1-06: Trừ buổi cuối kéo gói gia hạn UPCOMING về ngày mai** (BR-F4.7)
    - **Given** Hôm nay là 03/11/2026; gói SESSION A còn 1 buổi; hội viên có gói SESSION B trạng thái `UPCOMING` với thời hạn 60 ngày từ 01/12/2026 đến 29/01/2027.
    - **When** Lễ tân check-in cho hội viên ngày 03/11 trừ buổi cuối của gói A về 0; sau đó tác vụ nền đầu ngày 04/11 chạy.
    - **Then** Gói B được dời lịch bắt đầu từ ngày mai: `start_date = 04/11/2026`, `end_date = 02/01/2027` (vẫn nguyên vẹn 60 ngày hiệu lực); các gói khác loại (TIME, PT) không bị đổi lịch. Sang ngày 04/11, gói A chuyển `EXPIRED`, gói B chuyển `ACTIVE`, hội viên check-in được ngay bằng gói B. Nếu trừ buổi mà chưa về 0 thì không dời; nếu gói B đã bắt đầu từ ngày mai thì giữ nguyên.
  - **AC-F4.2.1-07: Check-in bằng gói PT đúng ngày có buổi tập hẹn trước** (BR-F4.8)
    - **Given** Hội viên chỉ sở hữu duy nhất gói `PT` (không có gói `TIME` hay `SESSION`).
    - **When** Check-in vào ngày không có buổi tập PT nào; hoặc check-in vào ngày có buổi tập PT `BOOKED` lúc 18:00 tại chi nhánh này, check-in lúc 17:45.
    - **Then** Ngày không có buổi tập: hệ thống từ chối `422 Unprocessable Content` kèm câu thông báo E3: *"Gói huấn luyện viên cá nhân (PT) chỉ dùng cho các buổi tập 1-1 có lịch hẹn, không dùng để check-in tự do vào phòng tập. Hội viên cần có thêm gói theo thời gian hoặc theo lượt."*, không ghi nhận lượt check-in. Ngày có lịch hẹn hợp lệ: hệ thống cho phép check-in `201 Created` gắn với gói PT, `session_deducted = false`, không trừ số buổi PT.
  - **AC-F4.2.1-08: Ràng buộc biên của quy tắc check-in gói PT** (BR-F4.8)
    - **Given** Hội viên có buổi hẹn PT tại chi nhánh khác, hoặc vào ngày khác, hoặc đã hủy (`CANCELLED`), hoặc đã hoàn thành (`DONE`); buổi hẹn PT lúc 00:30 hôm nay; buổi hẹn lúc 23:30 hôm qua; buổi hẹn của gói PT đã hết hạn; hoặc hội viên có thêm gói `TIME`.
    - **When** Thực hiện check-in.
    - **Then** Buổi hẹn khác chi nhánh, khác ngày, đã hủy, đã hoàn thành, hoặc buổi hôm qua đều bị từ chối `422` E3. Buổi hẹn lúc 00:30 hôm nay được tính hợp lệ theo ngày Việt Nam. Buổi hẹn của gói PT đã hết hạn không được áp dụng sang gói PT khác. Trường hợp hội viên có thêm gói `TIME` hợp lệ thì tự động cho check-in bằng gói `TIME`.
  - **AC-F4.2.1-09: Phạm vi hiệu lực của gói toàn chuỗi và gói riêng chi nhánh** (BR-F4.1)
    - **Given** Hội viên có gói `SESSION` toàn chuỗi (`branch_id IS NULL`), và một gói `SESSION` chỉ có hiệu lực tại chi nhánh `CN02`.
    - **When** Hội viên thực hiện check-in tại chi nhánh `CN01`.
    - **Then** Gói toàn chuỗi được chấp nhận check-in; gói riêng của `CN02` không được phép sử dụng tại `CN01` và bị bỏ qua (báo E2 nếu không còn gói nào khác hợp lệ).
  - **AC-F4.2.1-10: Từ chối khi không có gói tập nào hợp lệ** (E2)
    - **Given** Hội viên chưa từng đăng ký gói tập nào; hoặc mọi gói đều ở trạng thái `EXPIRED`, `CANCELLED`, `PENDING`, `UPCOMING`, hoặc ngày hiện tại nằm ngoài khoảng `[start_date, end_date]`.
    - **When** Lễ tân bấm xác nhận check-in.
    - **Then** Hệ thống từ chối với mã lỗi `422 Unprocessable Content` kèm thông báo E2: *"Hội viên không có gói tập nào đang hoạt động. Vui lòng đăng ký hoặc gia hạn gói tập tại quầy."*; không tạo bản ghi check-in.
  - **AC-F4.2.1-11: Từ chối khi gói SESSION hết buổi và hôm nay chưa trừ buổi** (E4)
    - **Given** Hội viên sở hữu gói `SESSION` có số buổi còn lại `remaining_sessions <= 0` và hôm nay chưa có lượt check-in nào trừ buổi.
    - **When** Lễ tân bấm xác nhận check-in.
    - **Then** Hệ thống từ chối với mã `422 Unprocessable Content` kèm thông báo E4: *"Gói tập theo buổi của hội viên đã hết lượt sử dụng. Vui lòng gia hạn thêm gói mới."*; không tạo bản ghi check-in.
  - **AC-F4.2.1-12: Từ chối khi gói tập đang bảo lưu** (E5)
    - **Given** Hội viên chỉ có duy nhất một gói tập đang ở trạng thái bảo lưu `FROZEN`.
    - **When** Lễ tân bấm xác nhận check-in.
    - **Then** Hệ thống từ chối với mã `422 Unprocessable Content` kèm thông báo E5: *"Gói tập của hội viên đang trong thời gian bảo lưu (từ ngày... đến ngày...). Vui lòng liên hệ quản lý để hủy bảo lưu sớm nếu muốn tập tiếp."*
  - **AC-F4.2.1-13: Thứ tự ưu tiên thông báo lỗi khi có nhiều lý do vi phạm** (VD-20, GĐ-4)
    - **Given** Hội viên vừa có gói `SESSION` 0 buổi (hôm nay chưa trừ) vừa có gói `TIME` đang bảo lưu `FROZEN`; hoặc gói `SESSION` 0 buổi và hôm nay có buổi hẹn PT hợp lệ.
    - **When** Thực hiện check-in.
    - **Then** Thứ tự ưu tiên kiểm tra và báo lỗi là **E5 (Bảo lưu) → E4 (Hết buổi) → E3 (Gói PT) → E2 (Không có gói)** (ví dụ trường hợp trên sẽ báo lỗi bảo lưu E5, không báo hết buổi E4). Trường hợp có gói SESSION 0 buổi nhưng hôm nay có lịch hẹn PT hợp lệ thì được check-in bằng gói PT, không bị chặn lỗi hết buổi.
  - **AC-F4.2.1-14: Xử lý khi hội viên có nhiều gói SESSION cùng dùng được** (VD-20, GĐ-3)
    - **Given** Hội viên sở hữu hai gói `SESSION` cùng đang `ACTIVE`, sáng nay đã check-in và trừ buổi ở gói thứ nhất.
    - **When** Chiều cùng ngày hội viên quay lại check-in lần nữa.
    - **Then** Hệ thống tái sử dụng đúng gói `SESSION` đã trừ buổi sáng nay để gắn vào lượt check-in mới (`session_deducted = false`), tuyệt đối không trừ buổi của gói thứ hai.
  - **AC-F4.2.1-15: Từ chối khi hồ sơ hội viên INACTIVE hoặc ngoài phạm vi** (E7, S3)
    - **Given** Hồ sơ hội viên có `status = 'INACTIVE'` (dù vẫn còn gói tập `ACTIVE`); hoặc mã hội viên không tồn tại, hoặc hội viên thuộc chi nhánh khác.
    - **When** Thực hiện check-in.
    - **Then** Trường hợp `INACTIVE`: trả về `422 Unprocessable Content` kèm thông báo E7: *"Hồ sơ hội viên này đã ngừng hoạt động. Vui lòng liên hệ quản lý để được hỗ trợ."* Trường hợp hội viên không tồn tại hoặc thuộc chi nhánh khác: trả về `404 Not Found` (S3).
  - **AC-F4.2.1-16: Kiểm tra giờ mở cửa phòng tập** (BR-F4.4, E1)
    - **Given** Chi nhánh có cấu hình giờ mở cửa từ 06:00 đến 22:00 đọc từ cơ sở dữ liệu.
    - **When** Thực hiện check-in vào các mốc thời gian: 05:59, 06:00, 21:59, 22:00.
    - **Then** Tại 05:59 và 22:00 (ngoài giờ mở cửa): hệ thống từ chối `422 Unprocessable Content` kèm thông báo E1: *"Phòng tập hiện đang đóng cửa (Giờ mở cửa: 06:00 - 22:00). Không thể thực hiện check-in."* Tại 06:00 và 21:59: hệ thống xử lý check-in thành công `201 Created`. Các chi nhánh có giờ mở cửa lẻ (ví dụ 18:30 – 20:45) được xử lý chính xác theo múi giờ Việt Nam.
  - **AC-F4.2.1-17: Xử lý cạnh tranh khi hai lễ tân cùng check-in một hội viên** (NF6, E6, VD-20, GĐ-1)
    - **Given** Hội viên có gói `SESSION` còn 5 buổi, chưa check-in hôm nay.
    - **When** Hai lễ tân tại quầy cùng bấm xác nhận check-in cho hội viên này đồng thời.
    - **Then** Yêu cầu đến trước thành công trả về `201 Created` và trừ 1 buổi còn 4 (`session_deducted = true`). Yêu cầu đến sau ngay lập tức nhận mã lỗi `409 Conflict` kèm thông báo E6: *"Dữ liệu lượt check-in đang được xử lý đồng thời bởi một thao tác khác. Vui lòng thử lại."* Khi lễ tân bấm "Thử lại", yêu cầu được xử lý thành công `201 Created`, ghi nhận `session_deducted = false` và không trừ thêm buổi nào, số buổi giữ nguyên là 4.
  - **AC-F4.2.1-18: Kiểm tra tính hợp lệ của dữ liệu đầu vào và nhân viên thao tác** (S3, S5)
    - **Given** Dữ liệu gửi lên thiếu `memberId` hoặc `memberId <= 0`; hoặc tài khoản nhân viên gọi API không gắn với chi nhánh nào.
    - **When** Gửi yêu cầu check-in `POST /api/v1/checkins`.
    - **Then** Dữ liệu thiếu/sai: hệ thống trả về `400 Bad Request`. Nhân viên không gắn chi nhánh: hệ thống trả về `403 Forbidden` *"Nhân viên chưa có chi nhánh làm việc hợp lệ."*, không tạo bản ghi check-in.

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

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F4.3.1: Tự đánh dấu đến lớp khi check-in
- **User story:** Là **Hệ thống**, tôi muốn **tự động ghi nhận thời điểm đến (`arrived_at`) cho chỗ đặt lớp học nhóm khi hội viên check-in hợp lệ**, để **huấn luyện viên nắm được học viên đã có mặt và hỗ trợ điểm danh chính xác**.
- **BR liên quan:** BR-F4.1; tích hợp F8 (`class_bookings`); VD-05 (khung giờ `[start_at - 15 phút, start_at + 60 phút]`).
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #137
- **Tiêu chí chấp nhận:**
  - **AC-F4.3.1-01: Ghi nhận thời điểm đến lớp trong khung giờ quy định** (VD-05)
    - **Given** Hội viên có chỗ đặt lớp học nhóm `BOOKED` tại chi nhánh này, lớp bắt đầu trong khoảng từ 15 phút trước đến 60 phút sau thời điểm check-in (`[thời điểm check-in − 15 phút, thời điểm check-in + 60 phút]`).
    - **When** Hội viên thực hiện check-in hợp lệ vào phòng tập.
    - **Then** Hệ thống tự động ghi nhận `class_bookings.arrived_at = now()`; trạng thái đặt chỗ vẫn giữ nguyên là `BOOKED` (huấn luyện viên vẫn cần điểm danh chính thức tại lớp); dữ liệu phản hồi check-in đính kèm thông tin lớp học (tên lớp, phòng, giờ bắt đầu) để nhắc nhở hội viên.
  - **AC-F4.3.1-02: Không ghi nhận thời điểm đến khi ngoài khung giờ hoặc sai chi nhánh**
    - **Given** Hội viên có chỗ đặt lớp bắt đầu trước thời điểm check-in 16 phút, hoặc bắt đầu sau thời điểm check-in 61 phút; hoặc lớp học diễn ra tại chi nhánh khác; hoặc chỗ đặt lớp đã bị hủy (`CANCELLED`).
    - **When** Hội viên check-in vào phòng tập.
    - **Then** Hệ thống không ghi nhận `arrived_at` cho chỗ đặt lớp này.
  - **AC-F4.3.1-03: Chỗ đặt lớp đã điểm danh hoặc kết thúc giữ nguyên trạng thái**
    - **Given** Chỗ đặt lớp trong khung giờ hợp lệ nhưng đã ở trạng thái `ATTENDED` (đã tham gia) hoặc `NO_SHOW` (vắng mặt).
    - **When** Hội viên check-in.
    - **Then** Hệ thống giữ nguyên trạng thái đặt chỗ, không cập nhật lại `arrived_at`, không làm thay đổi `updated_at` (bảo đảm F8 đếm vi phạm chính xác).
  - **AC-F4.3.1-04: Không ghi đè và không ghi nhận khi lượt check-in thất bại**
    - **Given** Chỗ đặt lớp đã có sẵn giá trị `arrived_at` (do lần check-in trước đó trong ngày); hoặc yêu cầu check-in bị từ chối (do lỗi thẻ, ngoài giờ...).
    - **When** Hội viên thực hiện check-in lần hai hoặc gặp lỗi check-in.
    - **Then** Hệ thống không ghi đè giá trị `arrived_at` đã có; trường hợp check-in thất bại thì tuyệt đối không ghi nhận `arrived_at`.

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

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F4.4.1: Check-out cho hội viên
- **User story:** Là **Lễ tân hoặc Quản lý chi nhánh**, tôi muốn **ghi nhận giờ về (check-out) cho hội viên khi rời phòng tập**, để **cập nhật trạng thái lượt tập và tính toán chính xác số lượng người đang có mặt theo thời gian thực**.
- **BR liên quan:** BR-F6.1 (thời gian thực); S3 (phân quyền chi nhánh); NF6 (xung đột đồng thời).
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #138
- **Tiêu chí chấp nhận:**
  - **AC-F4.4.1-01: Check-out thành công cho lượt tập đang diễn ra**
    - **Given** Lượt check-in của hội viên được mở lúc 09:00 tại chi nhánh.
    - **When** Lễ tân hoặc Quản lý chi nhánh thực hiện bấm check-out lúc 10:45 cùng ngày.
    - **Then** Hệ thống trả về mã `200 OK`; cập nhật `checked_out_at` là thời điểm hiện tại; tính toán thời lượng tập luyện là 105 phút; giao diện tab "Đang tập" cập nhật biến mất dòng của hội viên này.
  - **AC-F4.4.1-02: Xử lý trường hợp hội viên có nhiều lượt check-in mở trong ngày**
    - **Given** Hội viên có hai lượt check-in chưa check-out trong ngày (do vào tập nhiều lần).
    - **When** Lễ tân thực hiện check-out theo một mã lượt tập (`checkinId`) cụ thể.
    - **Then** Hệ thống chỉ đóng đúng lượt check-in được chỉ định, lượt check-in còn lại vẫn duy trì trạng thái đang mở (`checked_out_at IS NULL`).
  - **AC-F4.4.1-03: Từ chối khi không tìm thấy lượt tập đang mở hoặc khác chi nhánh** (E1, S3)
    - **Given** Mã lượt check-in đã được check-out trước đó, hoặc thuộc chi nhánh khác, hoặc không tồn tại trong hệ thống.
    - **When** Gửi yêu cầu check-out.
    - **Then** Hệ thống trả về `404 Not Found` kèm thông báo lỗi E1: *"Hội viên không có lượt tập nào đang diễn ra cần check-out."* Trên giao diện, nếu lượt tập đã được tab làm việc khác check-out, hệ thống tự động tải lại danh sách và loại bỏ dòng đó.
  - **AC-F4.4.1-04: Xử lý xung đột đồng thời khi hai nhân viên cùng check-out một lượt** (NF6)
    - **Given** Một lượt check-in đang mở tại chi nhánh.
    - **When** Hai lễ tân tại quầy cùng lúc bấm nút check-out cho lượt này.
    - **Then** Yêu cầu đến trước được xử lý thành công `200 OK`; yêu cầu đến sau nhận mã lỗi `404 Not Found` kèm thông báo E1; đảm bảo giờ check-out chỉ được ghi nhận một lần duy nhất.
  - **AC-F4.4.1-05: Phân quyền truy cập chức năng check-out** (S3, S5)
    - **Given** Người dùng với vai trò Hội viên (`MEMBER`), Huấn luyện viên (`TRAINER`), hoặc Chủ chuỗi (`SUPER_ADMIN`); hoặc gửi mã lượt check-in không phải số nguyên dương.
    - **When** Gửi yêu cầu `POST /api/v1/checkins/{id}/checkout`.
    - **Then** Vai trò không được phép trả về `403 Forbidden` *"Bạn không có quyền thực hiện thao tác này."*; mã lượt tập sai trả về `400 Bad Request`.

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
4. Hệ thống trả về dữ liệu phân trang: Mã check-in, Mã hội viên, Họ tên hội viên, cờ ảnh đại diện (`hasAvatar`), Tên gói tập, Giờ vào, Giờ ra, Nhân viên thực hiện, Thời lượng tập luyện, Trạng thái.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F4.5.1: Xem lịch sử check-in trong ngày
- **User story:** Là **Lễ tân hoặc Quản lý chi nhánh**, tôi muốn **xem danh sách các lượt check-in trong ngày tại chi nhánh có lọc và tìm kiếm**, để **theo dõi lưu lượng khách ra vào và đối soát khi cần thiết**.
- **BR liên quan:** BR-F4.3 (ngày theo giờ VN), BR-F4.6; S3 (bảo mật chi nhánh), S5 (tham số phân trang).
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #139
- **Tiêu chí chấp nhận:**
  - **AC-F4.5.1-01: Mặc định hiển thị danh sách trong ngày hiện tại theo giờ Việt Nam** (BR-F4.3)
    - **Given** Chi nhánh có các lượt check-in: 23:30 hôm qua, 00:10 hôm nay, 09:00 hôm nay và 00:00 ngày mai (tính theo giờ Việt Nam `UTC+7`).
    - **When** Lễ tân truy cập nhật ký check-in mà không truyền tham số ngày.
    - **Then** Danh sách chỉ hiển thị các lượt check-in của ngày hôm nay (00:10 và 09:00), sắp xếp theo thời gian vào mới nhất lên đầu; lượt 23:30 hôm qua chỉ hiển thị khi chọn xem ngày hôm qua.
  - **AC-F4.5.1-02: Đầy đủ thông tin chi tiết của từng lượt check-in** (VD-20, ADR 0011)
    - **Given** Chi nhánh có dữ liệu lượt check-in.
    - **When** Xem danh sách nhật ký check-in.
    - **Then** Mỗi dòng dữ liệu hiển thị đầy đủ: Mã check-in, Mã hội viên, Họ tên hội viên, cờ `hasAvatar`, Tên gói tập áp dụng, Thời điểm vào, Thời điểm ra, Tên nhân viên thực hiện, Thời lượng tập luyện (nếu đã ra về), và Trạng thái ("Đang tập" hoặc "Đã về").
  - **AC-F4.5.1-03: Lọc trạng thái, phương thức và tìm kiếm theo tên hoặc mã hội viên**
    - **Given** Danh sách gồm nhiều lượt check-in với các trạng thái và phương thức khác nhau.
    - **When** Lễ tân lọc theo trạng thái ("Đang tập" / "Đã về"), phương thức (`MANUAL` / `FACE`), tìm kiếm theo tên hoặc mã hội viên (không phân biệt chữ hoa chữ thường; các ký tự `%`, `_` được xử lý đúng nghĩa đen không gây lỗi), chuyển đổi trang dữ liệu.
    - **Then** Hệ thống trả về chính xác danh sách các bản ghi thỏa mãn điều kiện lọc, sắp xếp theo thời gian mới nhất lên đầu.
  - **AC-F4.5.1-04: Phân quyền và phạm vi dữ liệu theo chi nhánh** (S3, S5)
    - **Given** Nhân viên chi nhánh `CN01`, hoặc người dùng có vai trò Hội viên, Huấn luyện viên, Chủ chuỗi; hoặc tham số lọc phân trang sai (`page < 0`, `size` ngoài khoảng 1–100).
    - **When** Gửi yêu cầu xem nhật ký check-in.
    - **Then** Nhân viên chỉ xem được danh sách của chi nhánh `CN01`; các vai trò không được phép nhận `403 Forbidden`; tham số sai nhận `400 Bad Request`.

---

## 4. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F4.1 (Gói tập hợp lệ):** Check-in chỉ chấp nhận gói có loại `TIME` hoặc `SESSION` ở trạng thái `ACTIVE`, còn trong thời hạn sử dụng và hợp lệ tại chi nhánh. Gói `PT` không được dùng để check-in tự do (chốt ngày 01/11).
- **BR-F4.2 (Ưu tiên gói TIME):** Nếu hội viên sở hữu cả gói `TIME` và `SESSION` đang cùng `ACTIVE`, hệ thống bắt buộc sử dụng gói `TIME` và không trừ buổi của gói `SESSION` (chốt ngày 01/11).
- **BR-F4.3 (Quy tắc trừ buổi một lần mỗi ngày):** Với gói `SESSION`, một ngày dương lịch theo giờ Việt Nam (`UTC+7`) chỉ bị trừ tối đa 1 buổi tại lượt check-in đầu tiên. Các lượt check-in kế tiếp trong ngày không bị trừ thêm (`session_deducted = false`). Ràng buộc CSDL bằng chỉ mục duy nhất có điều kiện: `CREATE UNIQUE INDEX uq_one_deduction_per_day ON checkins (membership_id, checkin_date) WHERE session_deducted = true;`. Ngày hiện tại (`:today`) do ứng dụng tính theo `ZoneId.of("Asia/Ho_Chi_Minh")` rồi truyền vào truy vấn; không dùng `CURRENT_DATE` của PostgreSQL, vì máy chủ chạy theo UTC nên từ 00:00 đến 07:00 giờ Việt Nam `CURRENT_DATE` vẫn là ngày hôm trước.
- **BR-F4.4 (Giờ mở cửa phòng tập):** Giờ mở cửa và đóng cửa đọc từ `branches.open_time`, `branches.close_time` của chi nhánh (theo ERD), không viết cứng mã nguồn. Được check-in khi `open_time <= giờ hiện tại < close_time`.
- **BR-F4.5 (Hiệu năng):** Phản hồi tra cứu hội viên và xác nhận check-in phải đạt dưới 2 giây trên tập dữ liệu 10.000 hội viên (NF1).
- **BR-F4.6 (Phân quyền):** Chỉ tài khoản nhân viên gắn với chi nhánh mới được thực hiện check-in cho chi nhánh đó (S3). Dữ liệu check-in được tự động gắn `branch_id` của nhân viên thao tác.
- **BR-F4.8 (Hội viên chỉ có gói PT, chốt 19/11/2026):** Gói PT không dùng để tập tự do. Hội viên chỉ có gói PT được check-in **đúng ngày có buổi PT `BOOKED`** của mình tại chi nhánh đó, để vào tập với huấn luyện viên; lượt check-in gắn gói PT, `session_deducted = false`, không trừ buổi. Ngày không có buổi PT thì từ chối (E3).
- **BR-F4.7 (Gói SESSION hết buổi trước hạn, chốt 03/11):** Khi lượt check-in trừ buổi cuối (`remaining_sessions` về 0), hệ thống tìm các gói của cùng hội viên có `status = 'UPCOMING'`, cùng `package_type = 'SESSION'`, cùng `branch_id` (so sánh `IS NOT DISTINCT FROM` vì gói toàn chuỗi có `branch_id` rỗng). Gọi `D` = `start_date` của gói UPCOMING sớm nhất − (`:today` + 1 ngày); nếu `D > 0`, mọi gói UPCOMING đó được trừ `D` ngày ở cả `start_date` và `end_date` (giữ nguyên số ngày hiệu lực và thứ tự nối tiếp). Thao tác nằm trong cùng giao dịch check-in, dùng khóa lạc quan `@Version` của `memberships`. Tác vụ nền chạy lúc 00:00 giờ Việt Nam (chạy bù mỗi khi máy chủ khởi động) chuyển `EXPIRED` các gói `SESSION` `ACTIVE` có `remaining_sessions = 0` mà hôm nay chưa có lượt check-in trừ buổi trước, rồi mới kích hoạt gói `UPCOMING` có `start_date <= :today`, để không vi phạm `uq_one_active_per_type`.

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
    "hasAvatar": true,
    "status": "ACTIVE",
    "isCurrentlyCheckedIn": false,
    "memberships": [
      {
        "id": 45,
        "packageName": "Gói Tiêu Chuẩn 1 Tháng",
        "packageType": "TIME",
        "startDate": "2026-11-01",
        "endDate": "2026-12-01",
        "status": "ACTIVE",
        "remainingSessions": null
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

Tiêu chí chấp nhận chi tiết cho từng chức năng đã được chuyển vào mục **H. User story và tiêu chí chấp nhận (Acceptance Criteria)** trong từng Use Case tương ứng ở Mục 3 theo chuẩn cây truy vết US – UC – AC – TC:
- **UC-F4.1 (US-F4.1.1):** Tra cứu và đối chiếu ảnh tại quầy (`AC-F4.1.1-01` đến `AC-F4.1.1-06`, Issue #135).
- **UC-F4.2 (US-F4.2.1):** Check-in thủ công tại quầy (`AC-F4.2.1-01` đến `AC-F4.2.1-18`, Issue #136).
- **UC-F4.3 (US-F4.3.1):** Tự đánh dấu đến lớp khi check-in (`AC-F4.3.1-01` đến `AC-F4.3.1-04`, Issue #137).
- **UC-F4.4 (US-F4.4.1):** Check-out cho hội viên (`AC-F4.4.1-01` đến `AC-F4.4.1-05`, Issue #138).
- **UC-F4.5 (US-F4.5.1):** Xem lịch sử check-in trong ngày (`AC-F4.5.1-01` đến `AC-F4.5.1-04`, Issue #139).

Các kịch bản kiểm thử nguyên bản trước đây (Scenario 1 đến 8) đã được chuẩn hóa và ánh xạ đầy đủ vào các mã AC chính thức nói trên.
