# Đặc tả Use Case – F9: Huấn luyện viên

- **Mã chức năng:** F9

- **Version:** v2 (Bắt buộc)

- **Người biên soạn:** Nguyễn Trọng Việt

- **Người duyệt chéo:** Lê Phước Minh Quân

- **Cập nhật 23/11/2026 (Nguyễn Trọng Việt):** viết lại mục H của UC-F9.1 đến UC-F9.7 theo cây truy vết ([ADR 0012](../../decisions/0012-quy-uoc-ma-truy-vet.md)): 7 user story, 63 AC có mã.

- **Nguồn yêu cầu:** US-F9.1 (#71), US-F9.2 (#72), US-F9.3 (#73), US-F9.4 (#74); quyết định ngày 03/11 (ADR 0005 mục 25–32, PR #82)

- **Tài liệu chuẩn tham chiếu:**

  - [Bản chốt phạm vi](../ban-chot-pham-vi.md)

  - [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml)

  - [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md)

  - [Quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md)

  - [ADR liên quan](../../decisions/)

---

## 1. Tổng quan chức năng

Chức năng huấn luyện viên (PT) hỗ trợ việc tập một kèm một giữa PT và hội viên:

- **Lịch:** PT khai báo khung giờ rảnh và xem lịch dạy; hội viên chỉ đặt buổi PT trong giờ PT rảnh, không trùng lịch.

- **Buổi tập:** đặt buổi chỉ giữ chỗ; khi PT xác nhận buổi đã diễn ra thì hệ thống mới trừ một buổi của gói PT.

- **Theo dõi:** PT soạn giáo án, ghi nhật ký tập và chỉ số cơ thể của học viên mình dạy; PT và hội viên xem tiến độ qua biểu đồ trên dữ liệu thực.

Vai trò liên quan: **PT** và **Hội viên**. Dữ liệu huấn luyện viên trỏ tới bảng `staff`. Không có gợi ý bằng AI. Phần tính lương và giao diện hoa hồng (F13) thuộc v3, không làm trong Sprint 2. Ở Sprint 1, tài liệu này chỉ đặc tả; phần hiện thực làm từ Sprint 2.

---

## 2. Danh sách Use Case

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F9.1** | Khai báo lịch rảnh và xem lịch dạy | PT | Cao | Sprint 2 |
| **UC-F9.2** | Đặt buổi PT | Hội viên | Cao | Sprint 2 |
| **UC-F9.3** | Xác nhận buổi PT đã diễn ra | PT | Cao | Sprint 2 |
| **UC-F9.4** | Soạn giáo án | PT | Trung bình | Sprint 2 |
| **UC-F9.5** | Ghi nhật ký tập và chỉ số cơ thể | PT | Trung bình | Sprint 2 |
| **UC-F9.6** | Xem tiến độ học viên | PT, Hội viên | Trung bình | Sprint 2 |
| **UC-F9.7** | Hủy buổi PT | Hội viên, Quản lý chi nhánh | Cao | Sprint 2 |

---

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F9.1: Khai báo lịch rảnh và xem lịch dạy

- **Mã UC:** UC-F9.1

- **Tên Use Case:** Khai báo lịch rảnh và xem lịch dạy

- **Tác nhân chính:** PT

- **Tác nhân phụ (nếu có):** Không

- **Mô tả ngắn gọn:** PT khai báo khung giờ rảnh để hội viên chọn thời gian hợp lệ, và xem các buổi PT, lớp nhóm được phân công cho mình.

#### A. Tiền điều kiện (Preconditions)

- Người dùng đã đăng nhập với vai trò PT (có bản ghi `staff` tương ứng).

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Khung giờ rảnh được lưu cho PT đó; lịch dạy hiển thị các buổi PT và lớp được phân công.

- **Thất bại:** Không có khung giờ nào được lưu hoặc thay đổi (rollback giao dịch); lịch của PT khác không bị ảnh hưởng.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. PT mở mục "Lịch của tôi", chọn thứ trong tuần, giờ bắt đầu, giờ kết thúc và chi nhánh áp dụng cho khung giờ rảnh.

2. Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào (Bean Validation), gồm giờ kết thúc sau giờ bắt đầu.

3. Hệ thống kiểm tra quyền và vai trò (S3); mã PT lấy từ token, PT chỉ thao tác trên lịch của chính mình.

4. Hệ thống lưu khung giờ rảnh trong một giao dịch (`@Transactional`).

5. Hệ thống trả về kết quả (201 Created).

6. Khi PT xem lịch, hệ thống trả khung giờ rảnh cùng các buổi PT và buổi lớp nhóm được phân công cho PT (200 OK).

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **E1 – Dữ liệu đầu vào không hợp lệ (400):**

  - Tại bước 2, giờ kết thúc không hợp lệ (không sau giờ bắt đầu), thiếu trường.

  - Hệ thống từ chối, trả `ProblemDetail` (400) kèm danh sách lỗi theo từng trường.

- **E2 – Không có quyền (403):**

  - Tại bước 3, người thao tác không phải PT, hoặc PT cố sửa lịch của PT khác.

  - Hệ thống từ chối, không sửa lịch của người khác; trả `ProblemDetail` (403).

- **E3 – Không tìm thấy (404):** khung giờ cần sửa không tồn tại.

- **E4 – Khung giờ chồng nhau (422):** khung mới hoặc khung sau khi sửa chồng lên một khung khác của chính PT đó cùng thứ (BR-F9.14). Thông báo: **"Khung giờ bị trùng với khung rảnh đã khai báo."**

- **E5 – Khung giờ đã có hội viên đặt (422):** PT xóa khung, hoặc sửa khung làm một buổi đã đặt rơi ra ngoài khung (BR-F9.15). Thông báo: **"Khung giờ đã có hội viên đặt, không thể xóa hoặc thu hẹp."**

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F9.1:** PT chỉ khai báo, sửa, xóa và xem lịch rảnh của chính mình (S3).

- **BR-F9.2:** Hội viên chỉ đặt được buổi PT trong khung giờ PT đã khai báo rảnh (xem UC-F9.2).

- **BR-F9.14:** Hai khung rảnh của **cùng một PT** cùng thứ không được chồng nhau. Hai PT khác nhau có khung trùng giờ là bình thường (chốt 19/11/2026).

- **BR-F9.15:** PT chỉ được xóa khung rảnh, hoặc sửa làm khung hẹp lại, khi **không còn buổi PT `BOOKED` chưa diễn ra** của PT đó rơi vào đúng thứ và nằm trong khoảng giờ cũ của khung mà không còn nằm trong khung mới (tính theo giờ Việt Nam). Buổi đã qua, `DONE`, `CANCELLED`, `NO_SHOW` không chặn. Lý do: đã hứa với khách thì phải giữ, tránh mất uy tín. PT muốn nghỉ buổi đã có khách thì nhờ quản lý chi nhánh hủy hộ (UC-F9.7). Xóa là xóa hẳn dòng `trainer_availabilities` (ngoại lệ quy tắc mã số 8: đây là cấu hình lịch, không bảng nào trỏ tới), có ghi `audit_logs` (chốt 19/11/2026).

  - Ví dụ: khung "Thứ Ba 18:00–21:00", hội viên đã đặt buổi 19:00 thứ Ba 25/11. Trước buổi đó: xóa khung, hoặc sửa thành "Thứ Ba 18:00–19:00", đều bị từ chối. Sửa thành "Thứ Ba 17:00–21:00" thì được.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `trainer_availabilities`, `staff`; đọc `pt_sessions`, `class_sessions`

- **Chi tiết trường dữ liệu:**

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `trainer_availabilities` | `id` | `bigint` | PK, tự tăng | Mã khung giờ rảnh |
| `trainer_availabilities` | `trainer_id` | `bigint` | NOT NULL, FK → `staff.id` | PT sở hữu khung giờ rảnh |
| `trainer_availabilities` | `branch_id` | `bigint` | NOT NULL, FK → `branches.id` | Chi nhánh áp dụng |
| `trainer_availabilities` | `day_of_week` | `int` | NOT NULL, giá trị 1–7 theo ISO | Thứ trong tuần (1 = thứ Hai, 7 = Chủ nhật) |
| `trainer_availabilities` | `start_time` | `time` | NOT NULL | Giờ bắt đầu |
| `trainer_availabilities` | `end_time` | `time` | NOT NULL, CHECK `end_time > start_time` | Giờ kết thúc |
| `trainer_availabilities` | `created_at` | `timestamptz` | NOT NULL | Thời điểm tạo |
| `trainer_availabilities` | `updated_at` | `timestamptz` | NOT NULL | Thời điểm cập nhật |

- **Chỉ mục:** `(trainer_id, day_of_week)`.

- **Lưu ý:** Đây là lịch rảnh lặp lại theo thứ trong tuần, không phải một khung giờ gắn với ngày cụ thể.

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint:** `POST /api/v1/pt/availabilities`, `PUT /api/v1/pt/availabilities/{id}`, `DELETE /api/v1/pt/availabilities/{id}`, `GET /api/v1/pt/schedule` **(đề xuất, thống nhất với người làm API)**

- **Header:** `Authorization: Bearer <access_token>`

- **Request Body (DTO):**

  ```json
  {
    "branchId": 1,
    "dayOfWeek": 2,
    "startTime": "08:00:00",
    "endTime": "12:00:00"
  }
  ```

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F9.1.1: Lịch rảnh và lịch dạy

- **User story:** Là **PT**, tôi muốn **khai báo khung giờ rảnh và xem lịch dạy của mình**, để **hội viên chỉ đặt được buổi PT vào giờ tôi nhận dạy**.

- **BR liên quan:** BR-F9.1, BR-F9.2, BR-F9.14, BR-F9.15; S2, S3, S5

- **Version / Sprint:** v2 / Sprint 2

- **Issue:** #164

- **Tiêu chí chấp nhận:**

  - **AC-F9.1.1-01: Khai báo khung rảnh hợp lệ** (BR-F9.1)

    - **Given** PT Hòa đã đăng nhập; Hòa chưa có khung rảnh nào vào thứ Ba

    - **When** Gọi `POST /api/v1/pt/availabilities` với `{"branchId": 1, "dayOfWeek": 2, "startTime": "08:00:00", "endTime": "12:00:00"}`

    - **Then** `201 Created`; khung rảnh được lưu cho Hòa, mã PT lấy từ token, không nhận từ request

  - **AC-F9.1.1-02: Dữ liệu sai bị từ chối** (S5)

    - **Given** PT Hòa đã đăng nhập

    - **When** Thiếu `branchId`, `dayOfWeek`, `startTime` hoặc `endTime`; `dayOfWeek` ngoài khoảng 1–7; `endTime` bằng hoặc sớm hơn `startTime`; JSON sai cú pháp

    - **Then** `400 Bad Request` kèm lỗi theo từng trường; không có khung nào được lưu

  - **AC-F9.1.1-03: Hai khung của cùng một PT không chồng nhau** (BR-F9.14)

    - **Given** Hòa đã có khung thứ Ba 08:00–12:00

    - **When** Hòa thêm khung thứ Ba 11:00–14:00; hoặc sửa một khung thứ Ba khác thành khoảng chồng lên 08:00–12:00

    - **Then** `422 Unprocessable Content` **"Khung giờ bị trùng với khung rảnh đã khai báo."**; dữ liệu giữ nguyên

  - **AC-F9.1.1-04: Khung liền kề hoặc khác thứ không bị coi là chồng** (BR-F9.14)

    - **Given** Hòa đã có khung thứ Ba 08:00–12:00

    - **When** Hòa thêm khung thứ Ba 12:00–14:00; hoặc khung thứ Tư 08:00–12:00

    - **Then** Cả hai trường hợp đều `201 Created`

  - **AC-F9.1.1-05: Hai PT khác nhau được trùng giờ** (BR-F9.14)

    - **Given** PT Hòa có khung thứ Ba 08:00–12:00; PT Nam đã đăng nhập

    - **When** Nam thêm khung thứ Ba 08:00–12:00

    - **Then** `201 Created`; khung của Hòa không đổi

  - **AC-F9.1.1-06: Sửa khung rảnh của mình** (BR-F9.1)

    - **Given** Hòa có khung thứ Ba 08:00–12:00 (`id=10`), chưa có hội viên đặt trong khung này

    - **When** Gọi `PUT /api/v1/pt/availabilities/10` đổi thành 08:00–13:00

    - **Then** `200 OK`; khung được cập nhật, `updated_at` thay đổi

  - **AC-F9.1.1-07: Không xóa hoặc thu hẹp khung đang có buổi đã đặt** (BR-F9.15)

    - **Given** Khung thứ Ba 18:00–21:00 của Hòa; hội viên đã đặt buổi PT 19:00 thứ Ba 24/11/2026, trạng thái `BOOKED`, chưa diễn ra

    - **When** Hòa xóa khung; hoặc sửa thành 18:00–19:00; hoặc sửa thành 17:00–21:00

    - **Then** Xóa và sửa thành 18:00–19:00 trả `422 Unprocessable Content` **"Khung giờ đã có hội viên đặt, không thể xóa hoặc thu hẹp."**; sửa thành 17:00–21:00 trả `200 OK`; buổi đã đặt không đổi

  - **AC-F9.1.1-08: Buổi đã xong hoặc đã hủy không chặn việc xóa khung** (BR-F9.15)

    - **Given** Khung thứ Ba 18:00–21:00 chỉ còn các buổi đã qua, hoặc ở trạng thái `DONE`, `CANCELLED`, `NO_SHOW`

    - **When** Hòa xóa khung

    - **Then** `200 OK` (hoặc `204`, theo API); dòng `trainer_availabilities` bị xóa hẳn; có bản ghi `audit_logs` cho việc xóa

  - **AC-F9.1.1-09: Xem lịch dạy gồm khung rảnh, buổi PT và lớp nhóm** (BR-F9.1)

    - **Given** Hòa có 2 khung rảnh, 3 buổi PT đã đặt và 1 buổi lớp nhóm được phân công; PT Nam cũng có lịch riêng

    - **When** Hòa gọi `GET /api/v1/pt/schedule`

    - **Then** `200 OK`; kết quả gồm khung rảnh, buổi PT và buổi lớp nhóm của Hòa; không có dữ liệu của Nam

  - **AC-F9.1.1-10: PT chỉ thao tác trên lịch của mình** (BR-F9.1, S3)

    - **Given** Khung `id=20` thuộc PT Nam; Hòa đã đăng nhập

    - **When** Hòa sửa hoặc xóa khung `id=20`; hoặc sửa khung không tồn tại

    - **Then** Khung của Nam bị Hòa sửa hoặc xóa trả `403` **"Bạn không có quyền thực hiện thao tác này."**; khung không tồn tại trả `404`; dữ liệu giữ nguyên

  - **AC-F9.1.1-11: Từ chối người không phải PT** (S2, S3)

    - **Given** Người gọi là Hội viên, Lễ tân, Quản lý chi nhánh hoặc Chủ chuỗi; hoặc không gửi token

    - **When** Gọi các API khai báo, sửa, xóa lịch rảnh hoặc xem lịch dạy

    - **Then** Không có token hoặc token sai trả `401`; các trường hợp còn lại trả `403` **"Bạn không có quyền thực hiện thao tác này."**

---

### 3.2. UC-F9.2: Đặt buổi PT

- **Mã UC:** UC-F9.2

- **Tên Use Case:** Đặt buổi PT

- **Tác nhân chính:** Hội viên

- **Tác nhân phụ (nếu có):** PT (nhận buổi tập)

- **Mô tả ngắn gọn:** Hội viên đặt buổi một kèm một trong giờ PT rảnh bằng gói PT hợp lệ; hệ thống giữ buổi nhưng chưa trừ số buổi của gói.

#### A. Tiền điều kiện (Preconditions)

- Người dùng đã đăng nhập với vai trò Hội viên.

- Hội viên có gói PT ở trạng thái ACTIVE.

- PT được chọn rảnh trong khung giờ đó, thời điểm bắt đầu cách hiện tại ít nhất `PT_MIN_LEAD_HOURS` giờ (mặc định 12).

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Một buổi PT được tạo và giữ chỗ trong `pt_sessions`. **Số buổi của gói chưa bị trừ.**

- **Thất bại:** Không tạo buổi nào; không thay đổi gói (rollback giao dịch).

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Hội viên chọn PT, ngày và giờ trong khung rảnh, rồi xác nhận đặt.

2. Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào (Bean Validation).

3. Hệ thống kiểm tra quyền và vai trò (S3); mã hội viên lấy từ token.

4. Hệ thống kiểm tra hội viên có gói PT ACTIVE.

5. Hệ thống kiểm tra khung giờ nằm trong giờ PT rảnh.

6. Hệ thống kiểm tra thời điểm bắt đầu còn ít nhất `PT_MIN_LEAD_HOURS` giờ.

7. Hệ thống kiểm tra không trùng lịch của PT (kể cả giờ PT dạy lớp nhóm) và không trùng lịch của hội viên.

8. Hệ thống kiểm tra số buổi PT `BOOKED` của gói (kể cả buổi đã qua giờ mà chưa xác nhận) **ít hơn** số buổi còn lại của gói (BR-F9.4).

9. Hệ thống khóa bản ghi theo thứ tự `members` rồi `staff` (NF6), tạo buổi PT trong một giao dịch (`@Transactional`).

10. Hệ thống trả về kết quả (201 Created) kèm thông tin buổi PT.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **E1 – Dữ liệu đầu vào không hợp lệ (400):** thiếu PT hoặc giờ, sai định dạng; trả `ProblemDetail` (400).

- **E2 – Không có quyền (403):** người thao tác không phải Hội viên.

- **E3 – Không tìm thấy (404):** PT hoặc khung giờ không tồn tại.

- **E4 – Xung đột đồng thời (409):** hai yêu cầu cùng lúc cho cùng khung giờ của PT hoặc của hội viên; khóa NF6 chỉ nhận yêu cầu không trùng, yêu cầu còn lại bị từ chối.

- **E5 – Vi phạm quy tắc nghiệp vụ (422):** hệ thống từ chối kèm lý do bằng tiếng Việt, không tạo buổi, trong các trường hợp:

  - Hội viên chỉ có gói kiểu TIME hoặc SESSION, không có gói PT ACTIVE.

  - Giờ đặt nằm ngoài giờ PT rảnh.

  - Đặt sát giờ, chưa đủ `PT_MIN_LEAD_HOURS` giờ.

  - Trùng lịch với buổi PT hoặc lớp nhóm của PT, hoặc với lịch của hội viên.

  - Đã đặt đủ số buổi còn lại của gói. Ví dụ: gói còn 2 buổi và hội viên đang có 2 buổi `BOOKED` (chưa diễn ra, hoặc đã qua giờ mà chưa xác nhận) thì từ chối buổi thứ ba; hệ thống trả `ProblemDetail` (422): **"Bạn đã đặt đủ số buổi còn lại của gói PT. Vui lòng gia hạn gói để đặt thêm."**

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F9.2:** Chỉ đặt trong giờ PT rảnh.

- **BR-F9.3:** Không trùng lịch cả PT lẫn hội viên, kể cả giờ PT dạy lớp nhóm.

- **BR-F9.4:** Số buổi PT `BOOKED` của gói (gồm cả buổi đã qua giờ bắt đầu mà huấn luyện viên chưa xác nhận) phải ít hơn số buổi còn lại của gói thì mới đặt thêm được. Lý do: sau giờ bắt đầu không ai hủy được buổi PT, buổi đó chỉ kết thúc bằng xác nhận hoặc ghi vắng, cả hai đều trừ buổi; nên buổi `BOOKED` đã qua giờ chắc chắn đã tiêu một buổi. Ví dụ: gói còn 1 buổi, hội viên tập buổi sáng nay mà huấn luyện viên chưa bấm xác nhận thì chưa đặt được buổi chiều (chốt 21/11/2026, thay câu chốt 19/11 "buổi đã qua giờ không chặn").

- **BR-F9.5:** Đặt buổi chỉ giữ chỗ, không trừ số buổi của gói.

- **CONFIG-F9.1:** Đọc từ bảng `system_configs` với key `PT_MIN_LEAD_HOURS` (giá trị mặc định: `12`), không viết cứng trong mã nguồn (Quy tắc số 15).

- **[Yêu cầu phi chức năng]:**

  - Toàn vẹn đồng thời (NF6): khóa `members` rồi `staff`, chống trùng lịch kể cả với `class_sessions`.

  - Bảo mật (S2, S3): kiểm tra JWT, phân quyền ở backend.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `pt_sessions`; đọc `trainer_availabilities`, `memberships`, `staff`, `members`, `class_sessions`, `system_configs`

- **Chi tiết trường dữ liệu:**

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `pt_sessions` | `id` | `bigint` | PK, tự tăng | Mã buổi PT |
| `pt_sessions` | `trainer_id` | `bigint` | NOT NULL, FK → `staff.id` | PT phụ trách |
| `pt_sessions` | `member_id` | `bigint` | NOT NULL, FK → `members.id` | Hội viên đặt buổi |
| `pt_sessions` | `membership_id` | `bigint` | NOT NULL, FK → `memberships.id` | Gói PT được sử dụng |
| `pt_sessions` | `branch_id` | `bigint` | NOT NULL, FK → `branches.id` | Chi nhánh diễn ra buổi tập |
| `pt_sessions` | `start_at` | `timestamptz` | NOT NULL | Thời điểm bắt đầu |
| `pt_sessions` | `end_at` | `timestamptz` | NOT NULL | Thời điểm kết thúc |
| `pt_sessions` | `status` | `pt_session_status` | NOT NULL, DEFAULT `BOOKED` | Trạng thái buổi: BOOKED, DONE, CANCELLED, NO_SHOW |
| `pt_sessions` | `confirmed_at` | `timestamptz` | Có thể NULL | Thời điểm PT xác nhận buổi đã diễn ra |
| `pt_sessions` | `commission_amount` | `int` | Có thể NULL | Hoa hồng tính bằng VND, chốt khi buổi chuyển DONE |
| `pt_sessions` | `cancelled_at` | `timestamptz` | Có thể NULL | Thời điểm hủy buổi |
| `pt_sessions` | `created_by` | `bigint` | FK → `users.id`, có thể NULL | Người tạo buổi |
| `pt_sessions` | `note` | `text` | Có thể NULL | Ghi chú |
| `pt_sessions` | `created_at` | `timestamptz` | NOT NULL | Thời điểm tạo |
| `pt_sessions` | `updated_at` | `timestamptz` | NOT NULL | Thời điểm cập nhật |

- **Chỉ mục:** `(trainer_id, start_at)`, `(member_id, start_at)`, `membership_id`.

- **Bảng `memberships`:**

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `memberships` | `id` | `bigint` | PK, tự tăng | Mã gói hội viên |
| `memberships` | `member_id` | `bigint` | NOT NULL, FK → `members.id` | Hội viên sở hữu gói |
| `memberships` | `package_id` | `bigint` | NOT NULL, FK → `packages.id` | Gói được mua |
| `memberships` | `package_type` | `package_type` | NOT NULL | Loại gói, sao chép từ `packages.type` |
| `memberships` | `branch_id` | `bigint` | Có thể NULL, FK → `branches.id` | Chi nhánh được sử dụng; NULL nghĩa là toàn chuỗi |
| `memberships` | `sold_branch_id` | `bigint` | NOT NULL, FK → `branches.id` | Chi nhánh bán gói |
| `memberships` | `invoice_item_id` | `bigint` | Có thể NULL, FK → `invoice_items.id` | Dòng hóa đơn tạo ra gói |
| `memberships` | `start_date` | `date` | NOT NULL | Ngày bắt đầu hiệu lực |
| `memberships` | `end_date` | `date` | NOT NULL, CHECK `end_date >= start_date` | Ngày cuối cùng còn hiệu lực |
| `memberships` | `total_sessions` | `int` | Có thể NULL | Tổng số buổi của gói |
| `memberships` | `remaining_sessions` | `int` | Có thể NULL, CHECK `remaining_sessions >= 0` | Số buổi còn lại |
| `memberships` | `freeze_count` | `int` | NOT NULL, DEFAULT `0` | Số lần bảo lưu |
| `memberships` | `status` | `membership_status` | NOT NULL, DEFAULT `PENDING` | Trạng thái gói |
| `memberships` | `version` | `int` | NOT NULL, DEFAULT `0` | Phiên bản khóa lạc quan (@Version) |
| `memberships` | `created_at` | `timestamptz` | NOT NULL | Thời điểm tạo |
| `memberships` | `updated_at` | `timestamptz` | NOT NULL | Thời điểm cập nhật |

- **Chỉ mục của `memberships`:** `(member_id, status)`, `end_date`, `start_date` (chỉ mục có điều kiện `WHERE status = UPCOMING`).

- **Lưu ý:** `pt_sessions.member_id` phải trùng với `memberships.member_id`; hệ thống kiểm tra điều kiện này trong code. Gói được sử dụng phải là gói loại PT.

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint:** `POST /api/v1/pt-sessions` **(đề xuất)**

- **Header:** `Authorization: Bearer <access_token>`

- **Request Body (DTO):**

  ```json
  {
    "ptId": 1,
    "startTime": "2026-11-10T08:00:00Z"
  }
  ```

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F9.2.1: Đặt buổi PT

- **User story:** Là **Hội viên**, tôi muốn **đặt buổi tập một kèm một với PT trong giờ PT rảnh**, để **có chỗ tập đúng giờ mà không trùng lịch**.

- **BR liên quan:** BR-F9.2, BR-F9.3, BR-F9.4, BR-F9.5, CONFIG-F9.1; S2, S3, S5, NF6

- **Version / Sprint:** v2 / Sprint 2

- **Issue:** #165

- **Tiêu chí chấp nhận:**

  - **AC-F9.2.1-01: Đặt buổi hợp lệ chỉ giữ chỗ, không trừ buổi** (BR-F9.5)

    - **Given** Hội viên An có gói PT `ACTIVE` còn 5 buổi; PT Hòa có khung thứ Ba 08:00–12:00; hôm nay là thứ Hai 23/11/2026 lúc 09:00; `PT_MIN_LEAD_HOURS = 12`

    - **When** An gọi `POST /api/v1/pt-sessions` với `{"ptId": <Hòa>, "startTime": "2026-11-24T08:00:00+07:00"}`

    - **Then** `201 Created` kèm thông tin buổi; `pt_sessions.status = BOOKED`, gắn đúng gói PT của An; `remaining_sessions` của gói vẫn là 5

  - **AC-F9.2.1-02: Chỉ đặt trong giờ PT rảnh** (BR-F9.2)

    - **Given** Hòa chỉ có khung thứ Ba 08:00–12:00

    - **When** An đặt buổi lúc 14:00 thứ Ba; hoặc vào thứ Tư

    - **Then** `422 Unprocessable Content` kèm lý do bằng tiếng Việt; không có buổi nào được tạo

  - **AC-F9.2.1-03: Đặt sát giờ bị từ chối, đúng mốc thì được** (CONFIG-F9.1)

    - **Given** `PT_MIN_LEAD_HOURS = 12`; bây giờ là 20:00 ngày 23/11/2026; Hòa rảnh thứ Ba 24/11/2026 từ 08:00

    - **When** An đặt buổi lúc 08:00 ngày 24/11 (cách đúng 12 giờ); và đặt buổi lúc 07:00 ngày 24/11 (cách 11 giờ), nếu khung của Hòa gồm cả 07:00

    - **Then** Buổi cách đúng 12 giờ được `201 Created`; buổi cách 11 giờ trả `422` vì chưa đủ `PT_MIN_LEAD_HOURS` giờ

  - **AC-F9.2.1-04: Số giờ tối thiểu đọc từ cấu hình** (CONFIG-F9.1)

    - **Given** Quản lý đổi `PT_MIN_LEAD_HOURS` thành 24 trong `system_configs`

    - **When** An đặt buổi cách bây giờ 20 giờ

    - **Then** `422`; không có số 12 viết cứng trong mã nguồn

  - **AC-F9.2.1-05: Không trùng lịch của PT** (BR-F9.3)

    - **Given** Hòa đã có buổi PT `BOOKED` của hội viên khác lúc 08:00–09:00 thứ Ba, và dạy một lớp nhóm lúc 10:00–11:00 thứ Ba

    - **When** An đặt buổi PT với Hòa chồng giờ lên buổi PT đó; hoặc chồng giờ lên lớp nhóm

    - **Then** Cả hai trường hợp đều `422` kèm lý do trùng lịch; không có buổi nào được tạo

  - **AC-F9.2.1-06: Không trùng lịch của hội viên** (BR-F9.3)

    - **Given** An đã có buổi PT với PT Nam lúc 08:00 thứ Ba

    - **When** An đặt thêm buổi với Hòa cũng lúc 08:00 thứ Ba

    - **Then** `422` kèm lý do trùng lịch của hội viên; không có buổi nào được tạo

  - **AC-F9.2.1-07: Phải có gói PT đang hiệu lực** (BR-F9.4)

    - **Given** Hội viên Bình chỉ có gói TIME hoặc SESSION `ACTIVE`, hoặc gói PT đã `EXPIRED`

    - **When** Bình đặt buổi PT

    - **Then** `422` kèm lý do không có gói PT còn hiệu lực; không có buổi nào được tạo

  - **AC-F9.2.1-08: Không đặt quá số buổi còn lại, kể cả buổi đã qua giờ chưa xác nhận** (BR-F9.4)

    - **Given** An có gói PT còn 1 buổi; sáng nay An đã tập buổi 08:00 nhưng PT chưa xác nhận (buổi vẫn `BOOKED`)

    - **When** An đặt thêm buổi chiều nay hoặc buổi ngày mai

    - **Then** `422 Unprocessable Content` **"Bạn đã đặt đủ số buổi còn lại của gói PT. Vui lòng gia hạn gói để đặt thêm."**; không có buổi nào được tạo

  - **AC-F9.2.1-09: Còn chỗ khi số buổi `BOOKED` ít hơn số buổi còn lại** (BR-F9.4)

    - **Given** An có gói PT còn 2 buổi và đang có 2 buổi `BOOKED`; An hủy một buổi trước hạn

    - **When** An đặt buổi mới

    - **Then** `201 Created`; số buổi `BOOKED` là 2, không vượt số buổi còn lại; trước khi hủy, đặt buổi thứ ba bị `422` như AC-F9.2.1-08

  - **AC-F9.2.1-10: Hai yêu cầu cùng lúc cho một giờ** (NF6)

    - **Given** Hòa còn đúng một khung giờ trống; hai hội viên An và Bình đặt đúng giờ đó gần như đồng thời; hoặc An gửi hai yêu cầu cùng lúc từ hai thiết bị

    - **When** Cả hai yêu cầu tới server

    - **Then** Đúng một yêu cầu `201 Created`; yêu cầu còn lại bị từ chối (mã `409` theo ngoại lệ E4 của UC); chỉ có một buổi `BOOKED` ở khung giờ đó, gói không bị trừ buổi

  - **AC-F9.2.1-11: Dữ liệu sai, PT không tồn tại** (S5)

    - **Given** An đã đăng nhập

    - **When** Thiếu `ptId` hoặc `startTime`, `startTime` sai định dạng, JSON sai cú pháp; hoặc `ptId` không tồn tại

    - **Then** Dữ liệu sai trả `400 Bad Request` kèm lỗi theo trường; PT không tồn tại trả `404`; không có buổi nào được tạo

  - **AC-F9.2.1-12: Từ chối người không được đặt** (S2, S3)

    - **Given** Người gọi không phải Hội viên (PT, Lễ tân, Quản lý, Chủ chuỗi); hoặc không gửi token

    - **When** Gọi `POST /api/v1/pt-sessions`

    - **Then** Không có token hoặc token sai trả `401`; các trường hợp còn lại trả `403` **"Bạn không có quyền thực hiện thao tác này."**; mã hội viên luôn lấy từ token

---

### 3.3. UC-F9.3: Xác nhận buổi PT đã diễn ra

- **Mã UC:** UC-F9.3

- **Tên Use Case:** Xác nhận buổi PT đã diễn ra

- **Tác nhân chính:** PT

- **Tác nhân phụ (nếu có):** Hội viên (gói PT bị trừ buổi)

- **Mô tả ngắn gọn:** PT xác nhận buổi một kèm một đã hoàn thành để hệ thống ghi nhận, trừ một buổi của gói và chốt hoa hồng.

#### A. Tiền điều kiện (Preconditions)

- Người dùng đã đăng nhập với vai trò PT.

- Buổi PT thuộc về PT đó (đúng phân công), đã diễn ra và chưa được xác nhận.

- Gói PT của hội viên hợp lệ.

- Chưa tới giờ bắt đầu thì không xác nhận được (chốt 19/11/2026, 422 "Buổi tập chưa diễn ra").

- PT xác nhận hoặc ghi vắng tới 23:59 cùng ngày giờ Việt Nam của buổi; sau đó chỉ quản lý chi nhánh làm được, có ghi `audit_logs` (BR-F9.19).

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Buổi chuyển sang DONE; số buổi của gói giảm 1; `commission_amount` được chốt. Nếu là buổi cuối của gói thì gói chuyển EXPIRED và gói PT nối tiếp (UPCOMING) được kích hoạt (xem bước 6).

- **Thất bại:** Trạng thái buổi và gói giữ nguyên, không trừ buổi (rollback giao dịch).

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. PT mở danh sách buổi của mình và chọn buổi đã diễn ra để xác nhận.

2. Hệ thống kiểm tra quyền và vai trò (S3): buổi phải thuộc PT này.

3. Hệ thống kiểm tra buổi chưa ở trạng thái DONE (không xác nhận lần hai) và gói hợp lệ.

4. Hệ thống ghi trạng thái DONE và trừ một buổi của gói trong một giao dịch (`@Transactional`).

5. Hệ thống chốt `commission_amount` theo ERD (xem mục E); sau khi chốt, giá hoặc tỷ lệ thay đổi về sau không làm đổi số đã chốt.

6. Nếu đây là buổi cuối của gói: hệ thống **ghi và flush** gói hiện tại sang EXPIRED trước, rồi dời gói PT UPCOMING nối tiếp về bắt đầu hôm nay và chuyển ACTIVE, giữ nguyên số ngày hiệu lực, trong cùng giao dịch.

7. Hệ thống trả về kết quả (200 OK).

**Ví dụ (từ US-F9.3): gói PT A còn 1 buổi; gói PT B (60 ngày) đã gia hạn, đang UPCOMING từ 01/12. PT xác nhận buổi cuối của A ngày 03/11 thì A chuyển EXPIRED; B dời về 03/11 đến 01/01 và ACTIVE ngay; hội viên đặt được buổi PT mới bằng B.**

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **E2 – Không có quyền (403):** buổi không thuộc PT này (sai PT bị từ chối).

- **E3 – Không tìm thấy (404):** buổi PT không tồn tại.

- **E4 – Xung đột đồng thời (409):** hai thao tác cùng lúc trên một buổi (`@Version`); chỉ một thao tác được ghi, buổi không bị trừ hai lần.

- **E5 – Vi phạm quy tắc nghiệp vụ (422):** buổi đã được xác nhận (không trừ lần hai) hoặc buổi chỉ mới được đặt, chưa đủ điều kiện xác nhận. **(Mã lỗi cụ thể cần đối chiếu với nhóm.)**

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F9.6:** Chỉ trừ gói PT **khi PT xác nhận buổi đã diễn ra** (`DONE`) **hoặc ghi hội viên vắng mặt** (`NO_SHOW`, BR-F9.18); chỉ đặt buổi thì không trừ; mỗi buổi chỉ trừ một lần.

- **BR-F9.7:** Xác nhận buổi cuối thì gói PT chuyển EXPIRED ngay (ghi và flush trước), rồi gói PT UPCOMING nối tiếp được dời về hôm nay và ACTIVE, giữ nguyên số ngày hiệu lực, trong cùng giao dịch.

- **BR-F9.8:** `commission_amount` có giá trị khi và chỉ khi buổi ở trạng thái DONE; bằng tỷ lệ hoa hồng × (giá gói sau giảm / số buổi), làm tròn VND và chốt lúc xác nhận, không đổi về sau.

- **BR-F9.9:** Chỉ PT được phân công cho buổi đó mới được xác nhận (S3).

- **BR-F9.19:** Hạn xác nhận hoặc ghi vắng: PT được phân công làm tới 23:59 cùng ngày giờ Việt Nam của buổi; quá hạn thì PT bị từ chối (422 "Đã quá hạn xác nhận, liên hệ quản lý chi nhánh"), quản lý chi nhánh vẫn làm được và có ghi `audit_logs`. Giống quy tắc điểm danh lớp nhóm (chốt 19/11/2026).

- **BR-F9.18:** Hội viên vắng mặt không báo: sau giờ bắt đầu, PT được phân công ghi buổi là `NO_SHOW`. Buổi `NO_SHOW` **vẫn trừ một buổi** của gói, giống buổi `DONE` (hội viên không hủy đúng hạn theo BR-F9.16 thì mất buổi). Nếu là buổi cuối thì áp dụng BR-F9.7. Buổi `NO_SHOW` không có `commission_amount` (theo BR-F9.8, chỉ buổi `DONE` mới có). Chốt 19/11/2026.

- **Ngoài phạm vi Sprint 2:** tính lương và giao diện hoa hồng (F13) thuộc v3.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `pt_sessions`, `memberships`

- **Chi tiết trường dữ liệu:**

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `pt_sessions` | `id` | `bigint` | PK, tự tăng | Mã buổi PT |
| `pt_sessions` | `status` | `pt_session_status` | NOT NULL, DEFAULT `BOOKED` | Trạng thái buổi; chuyển thành DONE khi PT xác nhận |
| `pt_sessions` | `confirmed_at` | `timestamptz` | Có thể NULL | Thời điểm xác nhận hoàn thành |
| `pt_sessions` | `commission_amount` | `int` | Có thể NULL | Hoa hồng tính bằng VND; chỉ có giá trị khi status = DONE |
| `pt_sessions` | `updated_at` | `timestamptz` | NOT NULL | Thời điểm cập nhật |

- **Bảng `memberships`:**

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `memberships` | `id` | `bigint` | PK, tự tăng | Mã gói |
| `memberships` | `total_sessions` | `int` | Có thể NULL | Tổng số buổi của gói |
| `memberships` | `remaining_sessions` | `int` | Có thể NULL, CHECK >= 0 | Số buổi còn lại; giảm 1 khi xác nhận buổi PT |
| `memberships` | `start_date` | `date` | NOT NULL | Ngày bắt đầu hiệu lực |
| `memberships` | `end_date` | `date` | NOT NULL | Ngày kết thúc hiệu lực |
| `memberships` | `status` | `membership_status` | NOT NULL, DEFAULT `PENDING` | Trạng thái gói: PENDING, UPCOMING, ACTIVE, FROZEN, EXPIRED, TRANSFERRED, CANCELLED |
| `memberships` | `version` | `int` | NOT NULL, DEFAULT `0` | Phiên bản khóa lạc quan (@Version) |
| `memberships` | `updated_at` | `timestamptz` | NOT NULL | Thời điểm cập nhật |

- **Quy tắc dữ liệu:**

  - `commission_amount` có kiểu `int`, đơn vị VND.

  - `commission_amount` chỉ có giá trị khi `pt_sessions.status = DONE`.

  - `remaining_sessions` không được nhỏ hơn 0.

  - Khi xác nhận buổi PT cuối cùng, hệ thống cập nhật gói hiện tại sang EXPIRED và kích hoạt gói PT UPCOMING nối tiếp theo quy tắc nghiệp vụ BR-F9.7.

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint:** `PATCH /api/v1/pt-sessions/{id}/confirm` **(đề xuất)**

- **Header:** `Authorization: Bearer <access_token>`

- **Request Body:** Không có.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F9.3.1: Xác nhận buổi PT đã diễn ra

- **User story:** Là **PT**, tôi muốn **xác nhận buổi tập đã diễn ra hoặc ghi hội viên vắng mặt**, để **hệ thống trừ đúng một buổi của gói và chốt hoa hồng**.

- **BR liên quan:** BR-F9.6, BR-F9.7, BR-F9.8, BR-F9.9, BR-F9.18, BR-F9.19; S2, S3, NF6

- **Version / Sprint:** v2 / Sprint 2

- **Issue:** #166

- **Tiêu chí chấp nhận:**

  - **AC-F9.3.1-01: Xác nhận buổi đã diễn ra, trừ một buổi** (BR-F9.6)

    - **Given** PT Hòa đã đăng nhập; buổi PT `id=1` của Hòa với An đã qua giờ bắt đầu, `BOOKED`; gói PT của An còn 5 buổi

    - **When** Gọi `PATCH /api/v1/pt-sessions/1/confirm`

    - **Then** `200 OK`; buổi chuyển `DONE`, `confirmed_at` được ghi; gói của An còn 4 buổi

  - **AC-F9.3.1-02: Hoa hồng chốt lúc xác nhận và không đổi về sau** (BR-F9.8)

    - **Given** Gói PT giá 3.000.000 đ sau giảm, 10 buổi; tỷ lệ hoa hồng của Hòa là 10%

    - **When** Hòa xác nhận buổi; sau đó tỷ lệ hoa hồng của Hòa đổi thành 20%

    - **Then** `commission_amount = 30000` (10% × 3.000.000 / 10, làm tròn VND); sau khi đổi tỷ lệ, số đã chốt của buổi này vẫn là 30000

  - **AC-F9.3.1-03: Chưa tới giờ bắt đầu thì không xác nhận được**

    - **Given** Buổi PT của Hòa bắt đầu lúc 16:00 hôm nay; bây giờ là 15:00

    - **When** Hòa xác nhận buổi

    - **Then** `422 Unprocessable Content` **"Buổi tập chưa diễn ra."**; buổi vẫn `BOOKED`, gói không bị trừ

  - **AC-F9.3.1-04: Không xác nhận hai lần** (BR-F9.6)

    - **Given** Buổi `id=1` đã `DONE`

    - **When** Hòa xác nhận lại buổi `id=1`

    - **Then** `422`; gói không bị trừ thêm; `commission_amount` và `confirmed_at` không đổi

  - **AC-F9.3.1-05: Hai thao tác cùng lúc chỉ trừ một lần** (NF6)

    - **Given** Buổi `BOOKED` đã qua giờ; Hòa và quản lý chi nhánh cùng bấm xác nhận gần như đồng thời

    - **When** Cả hai yêu cầu tới server

    - **Then** Đúng một thao tác thành công; thao tác còn lại bị từ chối (mã `409` theo ngoại lệ E4 của UC, hoặc `422` nếu đọc lại thấy buổi đã `DONE`); gói chỉ bị trừ một buổi

  - **AC-F9.3.1-06: Buổi cuối thì gói hết hạn và gói nối tiếp được kích hoạt ngay** (BR-F9.7)

    - **Given** Gói PT A của An còn 1 buổi; gói PT B (60 ngày) đang `UPCOMING`, bắt đầu 01/12/2026; PT xác nhận buổi cuối của A ngày 03/11/2026

    - **When** Hòa xác nhận buổi

    - **Then** A chuyển `EXPIRED` trước (ghi và flush), rồi B chuyển `ACTIVE` với `start_date = 2026-11-03`, `end_date = 2027-01-01` (giữ nguyên 60 ngày); không lúc nào có hai gói PT cùng `ACTIVE`; An đặt được buổi PT mới bằng gói B

  - **AC-F9.3.1-07: Buổi cuối nhưng không có gói nối tiếp**

    - **Given** Gói PT của An còn 1 buổi và An không có gói PT `UPCOMING`

    - **When** Hòa xác nhận buổi cuối

    - **Then** Gói chuyển `EXPIRED`; `remaining_sessions = 0`; không có gói nào khác bị đổi

  - **AC-F9.3.1-08: Hội viên vắng mặt vẫn bị trừ buổi, không có hoa hồng** (BR-F9.18, BR-F9.8)

    - **Given** Buổi PT đã qua giờ bắt đầu, `BOOKED`; hội viên không đến và không hủy đúng hạn

    - **When** PT được phân công ghi buổi là `NO_SHOW`

    - **Then** Buổi chuyển `NO_SHOW`; gói bị trừ một buổi như buổi `DONE`; `commission_amount` rỗng; nếu là buổi cuối thì áp dụng AC-F9.3.1-06

  - **AC-F9.3.1-09: Hạn xác nhận 23:59 cùng ngày** (BR-F9.19)

    - **Given** Buổi PT ngày 03/11/2026 chưa được xác nhận

    - **When** Hòa xác nhận lúc 23:30 ngày 03/11; và một buổi khác Hòa xác nhận lúc 08:00 ngày 04/11; và quản lý chi nhánh xác nhận buổi quá hạn đó

    - **Then** Lúc 23:30 ngày 03/11: `200 OK`. Lúc 08:00 ngày 04/11: `422 Unprocessable Content` **"Đã quá hạn xác nhận, liên hệ quản lý chi nhánh."**; buổi giữ nguyên. Quản lý chi nhánh xác nhận quá hạn: `200 OK` và có bản ghi `audit_logs`

  - **AC-F9.3.1-10: Chỉ PT được phân công mới xác nhận** (BR-F9.9, S3)

    - **Given** Buổi `id=1` thuộc PT Hòa; PT Nam đã đăng nhập

    - **When** Nam xác nhận buổi `id=1`; hoặc xác nhận buổi không tồn tại; hoặc người gọi không phải PT hay quản lý; hoặc không gửi token

    - **Then** Lần lượt trả `403` **"Bạn không có quyền thực hiện thao tác này."**, `404`, `403`, `401`; buổi và gói giữ nguyên

---

### 3.4. UC-F9.4: Soạn giáo án

- **Mã UC:** UC-F9.4

- **Tên Use Case:** Soạn giáo án

- **Tác nhân chính:** PT

- **Tác nhân phụ (nếu có):** Hội viên (học viên)

- **Mô tả ngắn gọn:** PT soạn giáo án tập luyện cho học viên mình dạy.

#### A. Tiền điều kiện (Preconditions)

- Người dùng đã đăng nhập với vai trò PT.

- PT có quan hệ dạy với hội viên: hội viên có buổi PT hoặc lớp do PT đó dạy.

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Giáo án được lưu đúng học viên.

- **Thất bại:** Không lưu giáo án nào.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. PT chọn học viên của mình và mở mục "Giáo án".

2. PT nhập nội dung giáo án.

3. Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào.

4. Hệ thống kiểm tra quyền (S3) và quan hệ dạy giữa PT và hội viên.

5. Hệ thống lưu giáo án trong một giao dịch.

6. Hệ thống trả về kết quả (201 Created).

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **E1 – Dữ liệu đầu vào không hợp lệ (400):** thiếu nội dung hoặc sai định dạng.

- **E2 – Không có quyền (403):** PT không dạy hội viên này; backend từ chối theo S3.

- **E3 – Không tìm thấy (404):** hội viên không tồn tại.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F9.10:** PT chỉ xem và ghi dữ liệu cho học viên có buổi PT hoặc lớp do mình dạy (S3).

- **[Cần chốt với nhóm]:** một hội viên có một giáo án hiện hành hay nhiều giáo án theo thời gian.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `workout_plans`; đọc `members`, `pt_sessions`, `class_sessions`

- **Chi tiết trường dữ liệu:**

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `workout_plans` | `id` | `bigint` | PK, tự tăng | Mã giáo án |
| `workout_plans` | `trainer_id` | `bigint` | NOT NULL, FK → `staff.id` | PT soạn giáo án |
| `workout_plans` | `member_id` | `bigint` | NOT NULL, FK → `members.id` | Hội viên nhận giáo án |
| `workout_plans` | `title` | `varchar(255)` | NOT NULL | Tên giáo án |
| `workout_plans` | `content` | `jsonb` | Có thể NULL | Nội dung giáo án, danh sách bài tập theo buổi |
| `workout_plans` | `start_date` | `date` | Có thể NULL | Ngày bắt đầu áp dụng |
| `workout_plans` | `end_date` | `date` | Có thể NULL | Ngày kết thúc áp dụng |
| `workout_plans` | `created_at` | `timestamptz` | NOT NULL | Thời điểm tạo |
| `workout_plans` | `updated_at` | `timestamptz` | NOT NULL | Thời điểm cập nhật |

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint:** `POST /api/v1/pt/members/{memberId}/workout-plans` **(đề xuất)**

- **Header:** `Authorization: Bearer <access_token>`

- **Request Body (DTO):**

  ```json
  {
    "title": "Giáo án tăng cơ 4 tuần",
    "content": [
      {
        "session": 1,
        "exercises": [
          { "name": "Squat", "sets": 4, "reps": 10 }
        ]
      }
    ],
    "startDate": "2026-11-10",
    "endDate": "2026-12-07"
  }
  ```

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F9.4.1: Soạn giáo án

- **User story:** Là **PT**, tôi muốn **soạn giáo án tập luyện cho học viên mình dạy**, để **học viên có kế hoạch tập rõ ràng theo mục tiêu**.

- **BR liên quan:** BR-F9.10; S2, S3, S5

- **Version / Sprint:** v2 / Sprint 2

- **Issue:** #167

- **Tiêu chí chấp nhận:**

  - **AC-F9.4.1-01: Soạn giáo án cho học viên có buổi PT với mình** (BR-F9.10)

    - **Given** PT Hòa đã đăng nhập; An có buổi PT với Hòa

    - **When** Gọi `POST /api/v1/pt/members/{memberId}/workout-plans` với `title`, `content`, `startDate`, `endDate` hợp lệ

    - **Then** `201 Created`; giáo án được lưu với `trainer_id` là Hòa (lấy từ token) và `member_id` là An

  - **AC-F9.4.1-02: Soạn giáo án cho học viên có đặt chỗ lớp do mình dạy** (BR-F9.10)

    - **Given** Bình đặt chỗ (chưa hủy) ở một buổi lớp nhóm do Hòa dạy, chưa từng có buổi PT với Hòa

    - **When** Hòa soạn giáo án cho Bình

    - **Then** `201 Created`; giáo án được lưu đúng học viên

  - **AC-F9.4.1-03: Không soạn cho học viên mình không dạy** (BR-F9.10, S3)

    - **Given** Hội viên Cường không có buổi PT hoặc lớp nào do Hòa dạy

    - **When** Hòa soạn giáo án cho Cường

    - **Then** `403` **"Bạn không có quyền thực hiện thao tác này."**; không có giáo án nào được lưu

  - **AC-F9.4.1-04: Hội viên không tồn tại**

    - **Given** Không có hội viên với `memberId` được gửi

    - **When** Hòa gọi `POST /api/v1/pt/members/{memberId}/workout-plans`

    - **Then** `404 Not Found`; không có giáo án nào được lưu

  - **AC-F9.4.1-05: Dữ liệu sai bị từ chối** (S5)

    - **Given** Hòa soạn giáo án cho học viên của mình

    - **When** Thiếu `title`; `content` sai định dạng; `memberId` không phải số nguyên dương; hoặc JSON sai cú pháp

    - **Then** `400 Bad Request` kèm lỗi theo trường; không có giáo án nào được lưu

  - **AC-F9.4.1-06: Từ chối người không phải PT** (S2, S3)

    - **Given** Người gọi là Hội viên, Lễ tân, Quản lý hoặc Chủ chuỗi; hoặc không gửi token

    - **When** Gọi `POST /api/v1/pt/members/{memberId}/workout-plans`

    - **Then** Không có token hoặc token sai trả `401`; các trường hợp còn lại trả `403` **"Bạn không có quyền thực hiện thao tác này."**

---

### 3.5. UC-F9.5: Ghi nhật ký tập và chỉ số cơ thể

- **Mã UC:** UC-F9.5

- **Tên Use Case:** Ghi nhật ký tập và chỉ số cơ thể

- **Tác nhân chính:** PT

- **Tác nhân phụ (nếu có):** Hội viên (học viên)

- **Mô tả ngắn gọn:** PT ghi nhật ký tập và chỉ số cơ thể của học viên mình dạy để làm dữ liệu cho biểu đồ tiến độ.

#### A. Tiền điều kiện (Preconditions)

- Người dùng đã đăng nhập với vai trò PT.

- PT có quan hệ dạy với hội viên (có buổi PT hoặc lớp do PT dạy).

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Nhật ký và chỉ số được lưu đúng học viên và hiển thị trong tiến độ trên dữ liệu thực.

- **Thất bại:** Không lưu dữ liệu nào.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. PT chọn học viên và mở mục "Nhật ký" hoặc "Chỉ số cơ thể".

2. PT nhập nhật ký tập (bài tập, số hiệp, số lần, mức tạ) và các chỉ số cơ thể (cân nặng, chiều cao, tỷ lệ mỡ, khối lượng cơ).

3. Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào.

4. Hệ thống kiểm tra quyền (S3) và quan hệ dạy giữa PT và hội viên.

5. Hệ thống lưu dữ liệu trong một giao dịch.

6. Hệ thống trả về kết quả (201 Created).

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **E1 – Dữ liệu đầu vào không hợp lệ (400):** thiếu nội dung, giá trị chỉ số sai hoặc ngoài khoảng cho phép.

- **E2 – Không có quyền (403):** PT không dạy hội viên này; backend từ chối theo S3.

- **E3 – Không tìm thấy (404):** hội viên không tồn tại.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F9.10:** PT chỉ ghi dữ liệu cho học viên mình dạy (S3).

- **BR-F9.11:** Nhật ký và chỉ số là dữ liệu đầu vào của biểu đồ tiến độ (UC-F9.6).

- **[Cần chốt với nhóm]:** quan hệ với chỉ số cơ thể hội viên tự nhập ở F2, nếu có.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `workout_logs`, `body_metrics`; đọc `members`, `staff`, `pt_sessions`, `class_sessions`, `class_bookings`

- **Chi tiết trường dữ liệu:**

- **Bảng `workout_logs`** (ghi mới, mỗi bài tập một dòng):

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `workout_logs` | `id` | `bigint` | PK, tự tăng | Mã nhật ký tập |
| `workout_logs` | `member_id` | `bigint` | NOT NULL, FK → `members.id` | Học viên được ghi nhật ký; lấy từ `{memberId}` trên đường dẫn, phải là học viên PT đang dạy |
| `workout_logs` | `pt_session_id` | `bigint` | Có thể NULL, FK → `pt_sessions.id` | Buổi PT mà nhật ký thuộc về; NULL nếu ghi ngoài buổi PT (ví dụ buổi lớp nhóm). Nếu có giá trị, buổi đó phải thuộc đúng PT và đúng hội viên (kiểm tra trong code) |
| `workout_logs` | `logged_at` | `date` | NOT NULL | Ngày tập |
| `workout_logs` | `exercise` | `varchar(100)` | NOT NULL | Tên bài tập |
| `workout_logs` | `sets` | `int` | Có thể NULL | Số hiệp |
| `workout_logs` | `reps` | `int` | Có thể NULL | Số lần |
| `workout_logs` | `weight` | `decimal(6,2)` | Có thể NULL | Mức tạ, đơn vị kg |
| `workout_logs` | `created_at` | `timestamptz` | NOT NULL | Thời điểm tạo (hệ thống ghi) |
| `workout_logs` | `updated_at` | `timestamptz` | NOT NULL | Thời điểm cập nhật (hệ thống ghi) |

- **Chỉ mục của `workout_logs`:** `(member_id, logged_at)`, phục vụ biểu đồ tiến độ (UC-F9.6).

- **Bảng `body_metrics`** (ghi mới, mỗi lần đo một dòng):

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `body_metrics` | `id` | `bigint` | PK, tự tăng | Mã bản ghi chỉ số cơ thể |
| `body_metrics` | `member_id` | `bigint` | NOT NULL, FK → `members.id` | Học viên được đo; lấy từ `{memberId}` trên đường dẫn |
| `body_metrics` | `recorded_by` | `bigint` | Có thể NULL, FK → `users.id` | `users.id` của PT đang đăng nhập, lấy từ token, không nhận từ client |
| `body_metrics` | `measured_at` | `date` | NOT NULL | Ngày đo |
| `body_metrics` | `weight` | `decimal(5,2)` | Có thể NULL | Cân nặng, đơn vị kg |
| `body_metrics` | `height` | `decimal(5,2)` | Có thể NULL | Chiều cao, đơn vị cm |
| `body_metrics` | `body_fat` | `decimal(4,2)` | Có thể NULL | Tỷ lệ mỡ cơ thể, đơn vị % (tối đa 99,99) |
| `body_metrics` | `muscle_mass` | `decimal(5,2)` | Có thể NULL | Khối lượng cơ, đơn vị kg |
| `body_metrics` | `note` | `text` | Có thể NULL | Ghi chú |
| `body_metrics` | `created_at` | `timestamptz` | NOT NULL | Thời điểm tạo (hệ thống ghi) |
| `body_metrics` | `updated_at` | `timestamptz` | NOT NULL | Thời điểm cập nhật (hệ thống ghi) |

- **Chỉ mục của `body_metrics`:** `(member_id, measured_at)`.

- **Các bảng chỉ đọc** (kiểm tra hội viên tồn tại và quan hệ dạy, BR-F9.10):

| Tên bảng | Tên cột dùng | Mục đích |
| --- | --- | --- |
| `members` | `id`, `status`, `deleted_at` | Trả 404 (E3) nếu hội viên không tồn tại hoặc đã xóa mềm (`deleted_at` có giá trị) |
| `staff` | `id`, `user_id`, `status` | Đổi `users.id` của PT đang đăng nhập (từ token) thành `staff.id` (`trainer_id`) |
| `pt_sessions` | `trainer_id`, `member_id`, `status` | Có buổi PT giữa PT và hội viên thì có quan hệ dạy |
| `class_sessions` | `id`, `trainer_id` | Tìm các buổi lớp nhóm do PT dạy |
| `class_bookings` | `class_session_id`, `member_id`, `status` | Hội viên có đặt chỗ ở buổi lớp do PT dạy (booking chưa `CANCELLED`) thì có quan hệ dạy |

- **Ràng buộc và kiểm tra áp dụng:**

  - Ghi cả hai bảng trong cùng một giao dịch (bước 5); lỗi ở bảng nào thì không lưu bảng nào.

  - Thiếu `exercise`, thiếu `logged_at` hoặc `measured_at`, hoặc giá trị âm hoặc ngoài khoảng cho phép thì trả 400 (E1). Khoảng cho phép của từng chỉ số do code kiểm tra; ERD chỉ quy định độ rộng cột.

  - Không có quan hệ dạy giữa PT và hội viên thì trả 403 (E2); hội viên không tồn tại thì trả 404 (E3).

  - Dữ liệu không xóa cứng; nếu cần sửa thì cập nhật dòng cũ hoặc ghi dòng mới.

  - Có thể ghi `audit_logs` với `entity` = `workout_logs` hoặc `body_metrics`, `action` = `CREATE`.

- **Lưu ý (khớp mục "[Cần chốt với nhóm]" ở phần E):**

  - `body_metrics` dùng chung cho PT ghi (UC này) và hội viên tự nhập (F2). Hai nguồn phân biệt qua `recorded_by` (là `users.id` của PT hay của chính hội viên). Bảng không có ràng buộc duy nhất `(member_id, measured_at)`, nên một ngày có thể có nhiều dòng; cần chốt biểu đồ tiến độ (UC-F9.6) lấy dòng nào hoặc hiển thị cả hai.

  - `workout_logs` không có cột người ghi, chỉ có `pt_session_id`. Với nhật ký PT ghi ngoài buổi PT, dấu vết PT nào ghi chỉ còn trong `audit_logs`. Nếu cần hiển thị "PT nào ghi", nên đề xuất thêm cột `recorded_by` vào ERD; nếu không thì chấp nhận tra qua `audit_logs`.

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint:** `POST /api/v1/pt/members/{memberId}/workout-logs`, `POST /api/v1/pt/members/{memberId}/body-metrics` **(đề xuất)**

- **Header:** `Authorization: Bearer <access_token>`

- **Request Body (DTO) cho `workout-logs`:**

  ```json
  {
    "ptSessionId": 1,
    "loggedAt": "2026-11-10",
    "exercise": "Squat",
    "sets": 4,
    "reps": 10,
    "weight": 60.0
  }
  ```

- **Request Body (DTO) cho `body-metrics`:**

  ```json
  {
    "measuredAt": "2026-11-10",
    "weight": 65.5,
    "height": 170.0,
    "bodyFat": 18.5,
    "muscleMass": 30.2,
    "note": ""
  }
  ```

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F9.5.1: Nhật ký tập

- **User story:** Là **PT**, tôi muốn **ghi nhật ký tập và chỉ số cơ thể của học viên mình dạy**, để **có dữ liệu thực vẽ biểu đồ tiến độ**.

- **BR liên quan:** BR-F9.10, BR-F9.11; S2, S3, S5

- **Version / Sprint:** v2 / Sprint 2

- **Issue:** #168

- **Tiêu chí chấp nhận:**

  - **AC-F9.5.1-01: Ghi nhật ký tập cho học viên của mình** (BR-F9.10, BR-F9.11)

    - **Given** PT Hòa đã đăng nhập; An là học viên của Hòa

    - **When** Gọi `POST /api/v1/pt/members/{memberId}/workout-logs` với `loggedAt = 2026-11-10`, `exercise = "Squat"`, `sets = 4`, `reps = 10`, `weight = 60.0`

    - **Then** `201 Created`; một dòng `workout_logs` được lưu cho An, `member_id` lấy từ đường dẫn

  - **AC-F9.5.1-02: Ghi chỉ số cơ thể, người ghi lấy từ token** (BR-F9.10)

    - **Given** Hòa đã đăng nhập; An là học viên của Hòa

    - **When** Gọi `POST /api/v1/pt/members/{memberId}/body-metrics` với `measuredAt = 2026-11-10`, `weight = 65.5`, `height = 170.0`, `bodyFat = 18.5`, `muscleMass = 30.2`; kể cả khi tự gửi thêm `recordedBy`

    - **Then** `201 Created`; một dòng `body_metrics` được lưu cho An; `recorded_by` là `users.id` của Hòa, giá trị client gửi kèm bị bỏ qua

  - **AC-F9.5.1-03: Nhật ký gắn buổi PT phải thuộc đúng PT và hội viên** (BR-F9.10)

    - **Given** Buổi PT `id=5` thuộc PT Nam hoặc thuộc hội viên khác An

    - **When** Hòa ghi nhật ký cho An với `ptSessionId = 5`

    - **Then** Yêu cầu bị từ chối (mã lỗi cần chốt, đề xuất `422`); không có dòng nào được lưu. Nếu `ptSessionId` rỗng (ghi ngoài buổi PT) thì được `201 Created`

  - **AC-F9.5.1-04: Dữ liệu sai bị từ chối** (S5)

    - **Given** Hòa ghi cho học viên của mình

    - **When** Thiếu `exercise`, `loggedAt` hoặc `measuredAt`; giá trị âm; chỉ số vượt độ rộng cột (ví dụ `bodyFat` lớn hơn 99,99); JSON sai cú pháp

    - **Then** `400 Bad Request` kèm lỗi theo trường; không có dòng nào được lưu

  - **AC-F9.5.1-05: Không ghi cho học viên mình không dạy, hội viên không tồn tại** (BR-F9.10, S3)

    - **Given** Hội viên Cường không có quan hệ dạy với Hòa; hoặc `memberId` không tồn tại; hoặc hội viên đã xóa mềm

    - **When** Hòa ghi nhật ký hoặc chỉ số cho Cường hoặc `memberId` đó

    - **Then** Cường trả `403` **"Bạn không có quyền thực hiện thao tác này."**; hội viên không tồn tại hoặc đã xóa mềm trả `404`; không có dòng nào được lưu

  - **AC-F9.5.1-06: Từ chối người không phải PT** (S2, S3)

    - **Given** Người gọi là Hội viên, Lễ tân, Quản lý hoặc Chủ chuỗi; hoặc không gửi token

    - **When** Gọi API ghi nhật ký hoặc chỉ số

    - **Then** Không có token hoặc token sai trả `401`; các trường hợp còn lại trả `403`

---

### 3.6. UC-F9.6: Xem tiến độ học viên

- **Mã UC:** UC-F9.6

- **Tên Use Case:** Xem tiến độ học viên

- **Tác nhân chính:** PT, Hội viên

- **Tác nhân phụ (nếu có):** Không

- **Mô tả ngắn gọn:** Xem biểu đồ tiến độ dựa trên nhật ký tập và chỉ số cơ thể thực tế, không có gợi ý bằng AI.

#### A. Tiền điều kiện (Preconditions)

- Người dùng đã đăng nhập.

- Hội viên xem tiến độ của chính mình; PT xem tiến độ của học viên có buổi PT hoặc lớp do mình dạy.

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Hệ thống hiển thị biểu đồ tiến độ. Không có dữ liệu nào bị thay đổi.

- **Thất bại:** Không có dữ liệu nào bị thay đổi.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Người dùng mở mục "Tiến độ" và chọn khoảng thời gian.

2. Hệ thống kiểm tra tính hợp lệ của khoảng thời gian.

3. Hệ thống kiểm tra quyền (S3): hội viên chỉ xem của mình; PT chỉ xem của học viên mình dạy.

4. Hệ thống tổng hợp nhật ký tập và chỉ số cơ thể theo thời gian.

5. Hệ thống trả về kết quả (200 OK) gồm các điểm dữ liệu để vẽ biểu đồ.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **A1 – Chưa có dữ liệu:** trả dữ liệu rỗng; giao diện hiển thị "Chưa có dữ liệu tiến độ".

- **E1 – Dữ liệu đầu vào không hợp lệ (400):** khoảng thời gian sai.

- **E2 – Không có quyền (403):** hội viên yêu cầu tiến độ của người khác, hoặc PT xem học viên mình không dạy; backend từ chối theo S3.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F9.12:** Hội viên chỉ xem được tiến độ cá nhân của chính mình (S3).

- **BR-F9.10:** PT chỉ xem được học viên có buổi PT hoặc lớp do mình dạy (S3).

- **BR-F9.13:** Biểu đồ chỉ dựa trên dữ liệu thực; không có gợi ý bằng AI.

- **[Cần chốt với nhóm]:** các chỉ số được vẽ trên biểu đồ.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `workout_logs`, `body_metrics`; đọc `members`

- **Bảng `workout_logs`:**

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `workout_logs` | `id` | `bigint` | PK, tự tăng | Mã nhật ký tập |
| `workout_logs` | `member_id` | `bigint` | NOT NULL, FK → `members.id` | Hội viên thực hiện bài tập |
| `workout_logs` | `pt_session_id` | `bigint` | Có thể NULL, FK → `pt_sessions.id` | Buổi PT liên quan; NULL nếu hội viên tự ghi |
| `workout_logs` | `logged_at` | `date` | NOT NULL | Ngày ghi nhật ký |
| `workout_logs` | `exercise` | `varchar(100)` | NOT NULL | Tên bài tập |
| `workout_logs` | `sets` | `int` | Có thể NULL | Số hiệp |
| `workout_logs` | `reps` | `int` | Có thể NULL | Số lần lặp |
| `workout_logs` | `weight` | `decimal(6,2)` | Có thể NULL | Mức tạ sử dụng, đơn vị kg |
| `workout_logs` | `created_at` | `timestamptz` | NOT NULL | Thời điểm tạo |
| `workout_logs` | `updated_at` | `timestamptz` | NOT NULL | Thời điểm cập nhật |

- **Chỉ mục:** `(member_id, logged_at)`.

- **Bảng `body_metrics`:**

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `body_metrics` | `id` | `bigint` | PK, tự tăng | Mã bản ghi chỉ số cơ thể |
| `body_metrics` | `member_id` | `bigint` | NOT NULL, FK → `members.id` | Hội viên được đo |
| `body_metrics` | `recorded_by` | `bigint` | Có thể NULL, FK → `users.id` | Người ghi nhận chỉ số |
| `body_metrics` | `measured_at` | `date` | NOT NULL | Ngày đo |
| `body_metrics` | `weight` | `decimal(5,2)` | Có thể NULL | Cân nặng, đơn vị kg |
| `body_metrics` | `height` | `decimal(5,2)` | Có thể NULL | Chiều cao, đơn vị cm |
| `body_metrics` | `body_fat` | `decimal(4,2)` | Có thể NULL | Tỷ lệ mỡ cơ thể, đơn vị % |
| `body_metrics` | `muscle_mass` | `decimal(5,2)` | Có thể NULL | Khối lượng cơ, đơn vị kg |
| `body_metrics` | `note` | `text` | Có thể NULL | Ghi chú |
| `body_metrics` | `created_at` | `timestamptz` | NOT NULL | Thời điểm tạo |
| `body_metrics` | `updated_at` | `timestamptz` | NOT NULL | Thời điểm cập nhật |

- **Chỉ mục:** `(member_id, measured_at)`.

- **Lưu ý:** Các chỉ số cơ thể gồm cân nặng, chiều cao, tỷ lệ mỡ và khối lượng cơ. Các trường chỉ số có thể NULL theo ERD.

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint:** `GET /api/v1/members/{memberId}/progress?from=...&to=...` **(đề xuất)**

- **Header:** `Authorization: Bearer <access_token>`

- **Request Body:** Không có.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F9.6.1: Xem tiến độ học viên

- **User story:** Là **PT hoặc Hội viên**, tôi muốn **xem biểu đồ tiến độ dựa trên nhật ký tập và chỉ số cơ thể thực tế**, để **biết việc tập luyện có hiệu quả hay không**.

- **BR liên quan:** BR-F9.10, BR-F9.12, BR-F9.13; S2, S3, S5

- **Version / Sprint:** v2 / Sprint 2

- **Issue:** #169

- **Tiêu chí chấp nhận:**

  - **AC-F9.6.1-01: Hội viên xem tiến độ của chính mình** (BR-F9.12)

    - **Given** Hội viên An đã đăng nhập; An có nhật ký tập và chỉ số cơ thể trong tháng 11/2026

    - **When** Gọi `GET /api/v1/members/{memberId của An}/progress?from=2026-11-01&to=2026-11-30`

    - **Then** `200 OK`; kết quả là các điểm dữ liệu theo thời gian từ nhật ký tập và chỉ số cơ thể của An trong khoảng đã chọn, sắp theo ngày

  - **AC-F9.6.1-02: PT xem tiến độ học viên mình dạy** (BR-F9.10)

    - **Given** PT Hòa đã đăng nhập; An có buổi PT với Hòa

    - **When** Hòa gọi `GET /api/v1/members/{memberId của An}/progress`

    - **Then** `200 OK`; kết quả giống với những gì An xem được về chính mình

  - **AC-F9.6.1-03: Không xem tiến độ của người khác** (BR-F9.10, BR-F9.12, S3)

    - **Given** Hội viên An và Bình; PT Hòa không dạy Bình

    - **When** An xem tiến độ của Bình; hoặc Hòa xem tiến độ của Bình

    - **Then** Cả hai trường hợp trả `403` **"Bạn không có quyền thực hiện thao tác này."**; không có dữ liệu nào được trả

  - **AC-F9.6.1-04: Chưa có dữ liệu**

    - **Given** Hội viên mới, chưa có nhật ký hay chỉ số nào

    - **When** Xem tiến độ

    - **Then** `200 OK` với dữ liệu rỗng; giao diện hiển thị **"Chưa có dữ liệu tiến độ"**

  - **AC-F9.6.1-05: Khoảng thời gian sai** (S5)

    - **Given** Người dùng được phép xem

    - **When** `from` muộn hơn `to`; hoặc `from`, `to` sai định dạng ngày

    - **Then** `400 Bad Request` kèm lỗi theo tham số; không có dữ liệu nào được trả

  - **AC-F9.6.1-06: Chỉ dựa trên dữ liệu thực, không có gợi ý AI** (BR-F9.13)

    - **Given** Hội viên có dữ liệu tiến độ

    - **When** Xem tiến độ

    - **Then** Kết quả chỉ gồm các điểm dữ liệu đã ghi; không có trường hay màn hình nào chứa gợi ý, đánh giá hoặc dự đoán do AI tạo ra; không có dữ liệu nào bị thay đổi

  - **AC-F9.6.1-07: Từ chối người chưa đăng nhập hoặc vai trò khác** (S2, S3)

    - **Given** Không gửi token; hoặc người gọi là Lễ tân, Quản lý, Chủ chuỗi

    - **When** Gọi `GET /api/v1/members/{memberId}/progress`

    - **Then** Không có token hoặc token sai trả `401`; Lễ tân, Quản lý, Chủ chuỗi trả `403`

---

### 3.7. UC-F9.7: Hủy buổi PT

- **Mã UC:** UC-F9.7

- **Tên Use Case:** Hủy buổi PT

- **Tác nhân chính:** Hội viên (hủy buổi của mình), Quản lý chi nhánh (hủy hộ, ví dụ khi PT ốm)

- **Tác nhân phụ (nếu có):** Lễ tân (hủy hộ hội viên theo đúng quy tắc của hội viên)

- **Mô tả ngắn gọn:** Hủy một buổi PT đã đặt (`BOOKED`). Hủy không trừ buổi của gói; giờ đó trống lại cho người khác đặt. Chốt 19/11/2026.

#### A. Tiền điều kiện (Preconditions)

- Buổi PT đang `BOOKED`.

- Hội viên hoặc lễ tân hủy: thời điểm bắt đầu buổi còn cách hiện tại ít nhất `PT_MIN_LEAD_HOURS` giờ (mặc định 12, cùng mốc với lúc đặt).

- Quản lý chi nhánh hủy: buổi thuộc chi nhánh mình và chưa tới giờ bắt đầu.

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** `pt_sessions.status = CANCELLED`, `cancelled_at = now()`; số buổi còn lại của gói giữ nguyên; giờ đó trống lại. Quản lý hủy hộ thì hội viên nhận thông báo (F10) và có bản ghi `audit_logs`.

- **Thất bại:** buổi giữ nguyên `BOOKED`.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Người dùng mở buổi PT, bấm "Hủy buổi". Quản lý chi nhánh nhập lý do (bắt buộc).

2. Hệ thống kiểm tra quyền (S3): hội viên chỉ hủy buổi của mình; lễ tân và quản lý chỉ hủy buổi thuộc chi nhánh mình. PT **không** được tự hủy (BR-F9.17).

3. Hệ thống kiểm tra buổi đang `BOOKED` và mốc thời gian theo BR-F9.16.

4. Trong một giao dịch, khóa dòng `pt_sessions` (NF6), cập nhật `status = CANCELLED`, `cancelled_at = now()`.

5. Quản lý hủy hộ: ghi `audit_logs` (người hủy, lý do) và gửi thông báo cho hội viên (F10).

6. Hệ thống trả `200 OK`.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **E1 – Không có quyền (403):** hội viên hủy buổi của người khác; PT gọi API hủy; nhân viên hủy buổi chi nhánh khác.

- **E2 – Không tìm thấy (404):** buổi không tồn tại.

- **E3 – Buổi không còn ở trạng thái đặt (422):** buổi đã `DONE`, `CANCELLED` hoặc `NO_SHOW`. Thông báo: **"Buổi tập không còn ở trạng thái đã đặt."**

- **E4 – Hủy quá sát giờ (422):** hội viên hoặc lễ tân hủy khi còn ít hơn `PT_MIN_LEAD_HOURS` giờ. Thông báo: **"Chỉ được hủy buổi PT trước giờ bắt đầu ít nhất 12 giờ. Vui lòng liên hệ quầy."** Buổi vẫn diễn ra; hội viên vắng thì PT ghi `NO_SHOW`.

- **E5 – Buổi đã bắt đầu (422):** quản lý hủy khi đã tới giờ bắt đầu.

- **E6 – Thiếu lý do (400):** quản lý hủy hộ mà không nhập lý do.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F9.16:** Hội viên (hoặc lễ tân hủy hộ) chỉ hủy được buổi PT khi còn ít nhất `PT_MIN_LEAD_HOURS` giờ trước giờ bắt đầu. Hủy không trừ buổi, không có phạt hay khóa. Muộn hơn thì không cho hủy. Lý do: PT cần biết trước để sắp xếp, cùng mốc với lúc đặt.

- **BR-F9.17:** PT không tự hủy buổi đã có hội viên đặt (giữ uy tín với khách). PT bận đột xuất thì báo quản lý chi nhánh; quản lý hủy hộ bất kỳ lúc nào trước giờ bắt đầu, bắt buộc nhập lý do, ghi `audit_logs`, và hội viên được thông báo.

- **CONFIG-F9.1:** dùng lại `PT_MIN_LEAD_HOURS` trong `system_configs`, không viết cứng.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `pt_sessions` (`status`, `cancelled_at`), `audit_logs`, `notifications`; đọc `system_configs`. Không cần thêm cột.

- **Lưu ý:** lý do hủy của quản lý lưu trong `audit_logs.new_value`, vì `pt_sessions` không có cột lý do hủy.

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint:** `POST /api/v1/pt-sessions/{id}/cancel` **(đề xuất)**

- **Header:** `Authorization: Bearer <access_token>`

- **Request Body (DTO):** `{ "reason": "PT ốm đột xuất" }` (bắt buộc khi quản lý hủy; hội viên không cần)

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F9.7.1: Hủy buổi PT

- **User story:** Là **Hội viên**, tôi muốn **hủy buổi PT đã đặt khi còn đủ thời gian**, để **không mất buổi và giờ đó được nhường cho người khác**; và là **Quản lý chi nhánh**, tôi muốn **hủy hộ buổi PT khi PT bận đột xuất**.

- **BR liên quan:** BR-F9.16, BR-F9.17, CONFIG-F9.1; S2, S3, NF6

- **Version / Sprint:** v2 / Sprint 2

- **Issue:** *(chưa có, cần trưởng nhóm tạo issue cho US-F9.7.1)*

- **Tiêu chí chấp nhận:**

  - **AC-F9.7.1-01: Hội viên hủy trước hạn, không mất buổi** (BR-F9.16)

    - **Given** An có buổi PT `BOOKED` lúc 08:00 ngày 24/11/2026; bây giờ là 19:00 ngày 23/11 (còn 13 giờ); `PT_MIN_LEAD_HOURS = 12`; gói PT của An còn 5 buổi

    - **When** An gọi `POST /api/v1/pt-sessions/{id}/cancel`

    - **Then** `200 OK`; buổi chuyển `CANCELLED`, `cancelled_at` được ghi; gói của An vẫn còn 5 buổi; giờ đó trống lại cho người khác đặt

  - **AC-F9.7.1-02: Hủy sát giờ bị từ chối, đúng mốc thì được** (BR-F9.16)

    - **Given** Buổi PT lúc 08:00 ngày 24/11/2026; `PT_MIN_LEAD_HOURS = 12`

    - **When** An hủy lúc 20:00 ngày 23/11 (đúng 12 giờ); và một buổi khác An hủy khi còn 11 giờ

    - **Then** Đúng 12 giờ: `200 OK`. Còn 11 giờ: `422 Unprocessable Content` **"Chỉ được hủy buổi PT trước giờ bắt đầu ít nhất 12 giờ. Vui lòng liên hệ quầy."**; buổi vẫn `BOOKED`; hội viên vắng thì PT ghi `NO_SHOW`

  - **AC-F9.7.1-03: Mốc hủy đọc từ cấu hình** (CONFIG-F9.1)

    - **Given** Quản lý đổi `PT_MIN_LEAD_HOURS` thành 24

    - **When** An hủy buổi khi còn 20 giờ

    - **Then** `422`; thông báo lỗi nêu đúng số giờ đang cấu hình; không có số 12 viết cứng trong mã nguồn

  - **AC-F9.7.1-04: Lễ tân hủy hộ theo đúng quy tắc của hội viên** (BR-F9.16)

    - **Given** Lễ tân chi nhánh `CN01` đã đăng nhập; buổi PT thuộc `CN01`

    - **When** Lễ tân hủy hộ khi còn 13 giờ; và hủy hộ khi còn 11 giờ

    - **Then** Còn 13 giờ: `200 OK`. Còn 11 giờ: `422` như AC-F9.7.1-02

  - **AC-F9.7.1-05: Quản lý chi nhánh hủy hộ, có lý do, có dấu vết** (BR-F9.17)

    - **Given** Quản lý chi nhánh `CN01` đã đăng nhập; buổi PT `BOOKED` của chi nhánh còn 3 giờ nữa mới bắt đầu

    - **When** Gọi hủy với `{"reason": "PT ốm đột xuất"}`

    - **Then** `200 OK`; buổi `CANCELLED`; gói của hội viên không bị trừ; có bản ghi `audit_logs` ghi người hủy và lý do; hội viên nhận thông báo (F10, loại thông báo cần chốt)

  - **AC-F9.7.1-06: Quản lý hủy hộ phải nhập lý do** (BR-F9.17)

    - **Given** Quản lý chi nhánh đã đăng nhập

    - **When** Hủy hộ mà thiếu hoặc để trống `reason`

    - **Then** `400 Bad Request` kèm lỗi ở `reason`; buổi giữ nguyên `BOOKED`

  - **AC-F9.7.1-07: Quản lý không hủy buổi đã tới giờ bắt đầu**

    - **Given** Buổi PT đã bắt đầu lúc 08:00; bây giờ là 08:30

    - **When** Quản lý chi nhánh hủy hộ

    - **Then** `422 Unprocessable Content`; buổi giữ nguyên

  - **AC-F9.7.1-08: Chỉ hủy được buổi đang `BOOKED`**

    - **Given** Buổi đã `DONE`, `CANCELLED` hoặc `NO_SHOW`

    - **When** Hủy buổi đó

    - **Then** `422 Unprocessable Content` **"Buổi tập không còn ở trạng thái đã đặt."**; dữ liệu giữ nguyên

  - **AC-F9.7.1-09: PT không tự hủy buổi đã có khách** (BR-F9.17)

    - **Given** PT Hòa đã đăng nhập; buổi PT `BOOKED` của Hòa với An

    - **When** Hòa gọi `POST /api/v1/pt-sessions/{id}/cancel`

    - **Then** `403` **"Bạn không có quyền thực hiện thao tác này."**; buổi giữ nguyên

  - **AC-F9.7.1-10: Phân quyền theo chủ buổi và chi nhánh** (S3)

    - **Given** Buổi của hội viên Bình; An đã đăng nhập; hoặc buổi thuộc chi nhánh `CN02` còn nhân viên là `CN01`; hoặc buổi không tồn tại; hoặc không gửi token

    - **When** Gọi `POST /api/v1/pt-sessions/{id}/cancel`

    - **Then** Lần lượt trả `403`, `403`, `404`, `401`; buổi giữ nguyên

  - **AC-F9.7.1-11: Hai yêu cầu hủy cùng lúc** (NF6)

    - **Given** Hội viên và lễ tân cùng bấm hủy một buổi `BOOKED` gần như đồng thời

    - **When** Cả hai yêu cầu tới server

    - **Then** Đúng một yêu cầu `200 OK`; yêu cầu còn lại `422` **"Buổi tập không còn ở trạng thái đã đặt."**; `cancelled_at` chỉ được ghi một lần
