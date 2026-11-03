# Đặc tả Use Case – F8: Lớp học nhóm

- **Mã chức năng:** F8
- **Version:** v2 (Bắt buộc)
- **Người biên soạn:** Phạm Gia Bảo
- **Người duyệt chéo:** Nguyễn Anh Tú
- **Truy vết:** [#9](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/9) · WBS 1.2.9 · bản nháp chờ review và các quyết định ở mục 7.
- **Tài liệu chuẩn tham chiếu:**
  - [Bản chốt phạm vi](../ban-chot-pham-vi.md), mục “Lớp học nhóm (F8)” và NF6
  - [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml)
  - [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md)
  - [Quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md)
  - [ADR 0003: Danh sách chờ tự động đưa vào lớp](../../decisions/0003-danh-sach-cho-tu-dong-dua-vao-lop.md)
  - [ADR 0009: Bộ sơ đồ phân tích và thiết kế](../../decisions/0009-bo-so-do-phan-tich-thiet-ke.md)
  - [Mẫu đặc tả Use Case](mau-dac-ta-use-case.md)

> **Trạng thái:** Đặc tả này diễn giải yêu cầu đã duyệt trong bản chốt phạm vi, ERD và ADR 0003. Các endpoint, DTO, cách phân loại lỗi cụ thể trong mục G là **đề xuất thiết kế để owner F8/backend xác nhận**, chưa phải API contract đã thống nhất. Những điểm chưa được nguồn chuẩn quyết định được ghi rõ trong mục “Các câu hỏi cần chốt”, không tự đặt thêm quy tắc.

---

## 1. Tổng quan chức năng

F8 quản lý lịch lớp nhóm lặp hằng tuần và từng buổi học được sinh từ lịch đó; cho khách xem lịch công khai; cho hội viên đặt chỗ, hủy chỗ và tham gia danh sách chờ; tự động đưa hội viên đủ điều kiện từ danh sách chờ vào chỗ trống; cho hội viên xác nhận lại khi đổi giờ; cho huấn luyện viên điểm danh; và cho quản lý chi nhánh quản lý lịch lặp cùng phân công huấn luyện viên.

Chỉ hội viên có gói `TIME` đang `ACTIVE` và sử dụng được tại chi nhánh của buổi học mới được đặt chỗ hoặc vào danh sách chờ. Lễ tân có thể thao tác hộ hội viên nhưng không được ưu tiên người đó. Thứ tự danh sách chờ là FIFO. Việc đặt chỗ phải giữ đúng sĩ số và không tạo lịch trùng cho một hội viên ngay cả khi có yêu cầu đồng thời (NF6).

**Vai trò trong phạm vi F8:**

- Khách: xem lịch lớp công khai; không cần đăng nhập.
- Hội viên: xem lịch, đặt chỗ, vào danh sách chờ, hủy chỗ, xác nhận hoặc từ chối giờ học mới.
- Lễ tân (`RECEPTIONIST`): thao tác hộ hội viên theo cùng điều kiện và thứ tự như hội viên.
- Huấn luyện viên (`TRAINER`): xem lịch dạy lớp nhóm và điểm danh học viên lớp được phân công.
- Quản lý chi nhánh (`BRANCH_MANAGER`): quản lý loại lớp, lịch lặp, buổi học và phân công huấn luyện viên trong chi nhánh.
- Hệ thống nền: sinh buổi từ lịch lặp, tự động đưa hội viên từ danh sách chờ vào chỗ trống và xử lý hết hạn xác nhận lại.

**Phụ thuộc:** F3 cung cấp gói và trạng thái gói; F1 cung cấp danh tính và vai trò; F4 ghi `class_bookings.arrived_at` khi hội viên check-in gần giờ học. `arrived_at` chỉ ghi nhận đã đến chi nhánh, **không thay cho** điểm danh `ATTENDED`/`NO_SHOW` của huấn luyện viên. F10 gửi thông báo trong web và email theo thời điểm quy định. Sơ đồ use case hiện có ở `docs/design/use-case/`; hoạt động danh sách chờ hiện có ở `docs/design/activity/so-do-hoat-dong-danh-sach-cho.drawio.svg`.

---

## 2. Danh sách Use Case

Các mã UC dưới đây giữ nguyên số đã có trên sơ đồ. `UC-F8.4` được dùng cho luồng xác nhận lại khi đổi giờ để nối phần đánh số đang khuyết; `UC-F8.8` là UC hệ thống hỗ trợ sinh từng buổi từ lịch lặp, chưa có trên sơ đồ use case.

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F8.0** | Xem lịch lớp học nhóm công khai | Khách | Cao | Sprint 2 |
| **UC-F8.1** | Đặt chỗ lớp nhóm | Hội viên, Lễ tân | Cao | Sprint 2 |
| **UC-F8.2** | Tự động đưa hội viên từ danh sách chờ vào lớp | Hệ thống | Cao | Sprint 2 |
| **UC-F8.3** | Hủy chỗ lớp nhóm | Hội viên, Lễ tân | Cao | Sprint 2 |
| **UC-F8.4** | Xác nhận lại hoặc từ chối khi đổi giờ lớp | Hội viên, Lễ tân, Hệ thống | Cao | Sprint 2 |
| **UC-F8.5** | Xem lịch dạy lớp nhóm | Huấn luyện viên | Trung bình | Sprint 2 |
| **UC-F8.6** | Điểm danh học viên lớp học nhóm | Huấn luyện viên | Cao | Sprint 2 |
| **UC-F8.7** | Quản lý lịch lặp lớp học và phân công huấn luyện viên | Quản lý chi nhánh | Cao | Sprint 2 |
| **UC-F8.8** | Sinh buổi học từ lịch lặp | Hệ thống | Cao | Sprint 2 |

> **Truy vết sơ đồ (cần Minh Quân cập nhật, không sửa sơ đồ trong task này):** Trong [`use-case-hoi-vien.drawio.svg`](../../design/use-case/use-case-hoi-vien.drawio.svg), nhãn UC-F8.2 hiện là “Tự động vào danh sách chờ khi lớp đầy”, nhưng UC-F8.2 trong SRS mô tả bước tự động đưa người đủ điều kiện từ `WAITLIST` vào lớp; hãy cập nhật nhãn để khớp. Nhãn UC-F8.3 “Hủy chỗ đặt lớp (Ân hạn 15 phút)” cũng mơ hồ: grace 15 phút chỉ áp dụng cho booking vừa được tự động promotion, không áp dụng cho mọi lần hủy.

### 2.1. Cách biểu diễn trạng thái trong CSDL

Các khối `Enum` trong DBML mô tả tập giá trị logic để đặc tả và đối chiếu. CSDL vật lý lưu các cột trạng thái/loại/lý do bằng `varchar` kèm `CHECK`, theo [quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md); các bảng ở mục F ghi kiểu vật lý và tập giá trị CHECK.

---

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F8.0: Xem lịch lớp học nhóm công khai

- **Mã UC:** UC-F8.0
- **Tên Use Case:** Xem lịch lớp học nhóm công khai
- **Tác nhân chính:** Khách
- **Tác nhân phụ (nếu có):** Không
- **Mô tả ngắn gọn:** Khách xem các buổi lớp nhóm được công bố để tìm hiểu lịch tập trước khi đăng ký tài khoản hoặc đến chi nhánh.

#### A. Tiền điều kiện (Preconditions)

- Không cần đăng nhập.
- Chi nhánh và các buổi lớp được yêu cầu tồn tại.

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Không thay đổi dữ liệu. Trả lịch lớp nhóm công khai theo bộ lọc đã yêu cầu.
- **Thất bại:** Không thay đổi dữ liệu; trả lỗi định dạng hoặc không tìm thấy chi nhánh theo quy ước HTTP tại mục D.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Khách mở trang lịch lớp nhóm công khai.
2. Khách chọn chi nhánh và khoảng ngày muốn xem (nếu giao diện cung cấp bộ lọc).
3. Hệ thống kiểm tra cú pháp bộ lọc và chi nhánh.
4. Hệ thống đọc các buổi lớp nhóm thuộc chi nhánh trong khoảng được yêu cầu.
5. Hệ thống trả thông tin lịch công khai: loại lớp, thời gian, phòng và huấn luyện viên nếu các thông tin đó được công bố trên giao diện.
6. Giao diện hiển thị lịch; khách có thể tiếp tục đến luồng đăng nhập/đăng ký của F1.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **A1 – Không có buổi lớp trong khoảng đã chọn:** Tại bước 4, nếu không có buổi phù hợp, hệ thống trả danh sách rỗng; giao diện hiển thị trạng thái “Không có lớp trong khoảng thời gian này”.
- **E1 – Bộ lọc sai định dạng (400 Bad Request):** Tại bước 3, hệ thống trả `ProblemDetail` tiếng Việt, nêu trường ngày/chi nhánh không hợp lệ.
- **E2 – Không tìm thấy chi nhánh (404 Not Found):** Nếu mã chi nhánh không tồn tại, hệ thống trả `ProblemDetail` (404).

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F8.0-01:** Khách được xem lịch lớp công khai (F8; use case `UC-F8.0` trên sơ đồ `use-case-khach.drawio.svg`).
- **BR-F8.0-02:** Phạm vi dữ liệu chỉ là thông tin lịch công khai; không trả thông tin cá nhân hoặc dữ liệu hội viên.
- **Câu hỏi cần chốt:** Nguồn chuẩn không nêu mặc định thời gian hiển thị, có hiển thị số chỗ còn lại hay không, và bộ lọc/chi nhánh mặc định khi F18 chưa có. Không tự áp dụng `BOOKING_WINDOW_DAYS` cho việc xem lịch; tham số này quy định cửa sổ **đặt chỗ**.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** Không có; chỉ đọc.
- **Bảng truy vấn:** `class_sessions`, `class_types`, `class_schedules`, `branches`, `rooms`, `staff`.

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `class_sessions` | `branch_id`, `start_at`, `end_at`, `status`, `room_id`, `trainer_id`, `class_schedule_id` | `bigint`, `timestamptz`, `varchar`, `bigint` | Các cột bắt buộc NOT NULL; `status` có CHECK (`SCHEDULED`, `RESCHEDULED`, `CANCELLED`, `COMPLETED`) | Buổi học được hiển thị |
| `class_sessions` | `occurrence_date` | `date` | NOT NULL | Ngày phát sinh theo lịch lặp, ngày theo giờ Việt Nam |
| `class_types` | `name`, `description`, `status` | `varchar(100)`, `text`, `varchar` | `name` UNIQUE; `status` NOT NULL, CHECK (`ACTIVE`, `INACTIVE`) | Tên và mô tả loại lớp |
| `rooms` | `name` | `varchar` | Theo ERD | Phòng tổ chức buổi học |
| `staff` | `id` | `bigint` | PK | Huấn luyện viên được phân công |

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint (đề xuất, cần owner xác nhận):** `GET /api/v1/public/class-sessions?branchId={id}&from={date}&to={date}`
- **Header:** Không yêu cầu `Authorization`.
- **Request Body (DTO):** Không có; tham số query đề xuất: `branchId`, `from`, `to`.
- **Response Success (200 OK) – hình dạng đề xuất:**
  ```json
  {
    "items": [
      {
        "sessionId": 123,
        "classTypeName": "Yoga",
        "occurrenceDate": "2026-11-12",
        "startAt": "2026-11-12T18:00:00+07:00",
        "endAt": "2026-11-12T19:00:00+07:00",
        "roomName": "Phòng 201",
        "trainerName": "Huấn luyện viên"
      }
    ]
  }
  ```
- **Màn hình giao diện liên quan:** Trang khách / lịch lớp công khai; wireframe thuộc issue #17, hiện chưa có file wireframe trang này trong repo.
- Các trường public cụ thể và việc hiển thị sức chứa/chỗ trống cần được owner giao diện và backend xác nhận trước khi coi là API contract.

#### H. Tiêu chí chấp nhận (Acceptance Criteria)

- **Kịch bản 1: Khách xem lịch công khai**
  - **Given** Có các buổi lớp thuộc chi nhánh và khoảng ngày hợp lệ.
  - **When** Khách gọi API lịch công khai không kèm access token.
  - **Then** Hệ thống trả `200 OK` với các buổi phù hợp và không trả dữ liệu cá nhân hội viên.
- **Kịch bản 2: Khoảng ngày không có lớp**
  - **Given** Khoảng ngày hợp lệ không có buổi phù hợp.
  - **When** Khách xem lịch.
  - **Then** Hệ thống trả `200 OK` với danh sách rỗng.
- **Kịch bản 3: Bộ lọc không hợp lệ**
  - **Given** Ngày hoặc mã chi nhánh sai định dạng.
  - **When** Khách yêu cầu lịch.
  - **Then** Hệ thống trả `400 Bad Request` với `ProblemDetail` tiếng Việt.

---

### 3.2. UC-F8.1: Đặt chỗ lớp nhóm

- **Mã UC:** UC-F8.1
- **Tên Use Case:** Đặt chỗ lớp nhóm
- **Tác nhân chính:** Hội viên (`MEMBER`), Lễ tân (`RECEPTIONIST`) khi thao tác hộ hội viên
- **Tác nhân phụ (nếu có):** Hệ thống cơ sở dữ liệu
- **Mô tả ngắn gọn:** Tạo chỗ đặt nếu buổi còn sức chứa; nếu đã đầy thì ghi hội viên vào cuối danh sách chờ FIFO.

#### A. Tiền điều kiện (Preconditions)

- Hội viên đăng nhập hợp lệ; hoặc lễ tân đăng nhập và chọn hồ sơ hội viên để thao tác hộ.
- Buổi học tồn tại, chưa bị hủy/kết thúc và nằm trong cửa sổ đặt chỗ.
- Hội viên có gói `TIME` đang `ACTIVE`, dùng được tại chi nhánh của buổi học.
- Hội viên không bị khóa đặt lớp tại thời điểm yêu cầu.

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Một dòng `class_bookings` được tạo cho hội viên/buổi. Nếu còn sức chứa, trạng thái là `BOOKED`; nếu đã đủ sĩ số, trạng thái là `WAITLIST` và `waitlisted_at` được ghi để xác định thứ tự. `created_by` ghi người dùng thao tác nếu có theo quy ước được backend xác nhận.
- **Thất bại:** Không tạo booking và không thay đổi sĩ số/danh sách chờ. Giao dịch bị rollback nếu có lỗi hoặc xung đột đồng thời.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Hội viên chọn một buổi lớp và bấm đặt chỗ; hoặc lễ tân chọn buổi và hồ sơ hội viên cần thao tác hộ.
2. Hệ thống validate tham số nhận dạng buổi/hội viên.
3. Hệ thống xác thực vai trò và kiểm tra hội viên chỉ đặt cho chính mình; lễ tân chỉ thao tác hộ trong phạm vi được phân quyền (S3).
4. Trước khi lấy khóa, hệ thống xác định nhánh xử lý. Nếu không cần arbitrate waitlist, khóa dòng `members` của hội viên đang yêu cầu rồi mới khóa dòng `class_sessions` theo NF6. Nếu cần phân xử queue, owner/backend phải chọn thiết kế trước khi bắt đầu transaction: khóa đủ các dòng `members` ứng viên theo thứ tự ổn định (gồm hội viên đang yêu cầu nếu có) rồi mới khóa `class_sessions`, sau đó đọc lại queue/điều kiện; hoặc rollback request transaction, xử lý promotion trong transaction riêng, rồi đọc lại và retry request.
5. Hệ thống xác nhận buổi có thể đặt và thời điểm yêu cầu nằm trong cửa sổ `BOOKING_WINDOW_DAYS`.
6. Hệ thống xác nhận hội viên có gói `TIME` `ACTIVE` dùng được tại chi nhánh của lớp, không bị khóa đặt lớp, chưa có booking đang hoạt động cho cùng buổi, và không vi phạm quy tắc không đăng ký các buổi trùng giờ theo chính sách đã được owner chốt.
7. Hệ thống đếm các chỗ đang giữ: `BOOKED`, `PENDING_RECONFIRM`, `ATTENDED`, `NO_SHOW`.
8. Nếu còn chỗ nhưng có waitlist, hệ thống phải phân xử FIFO qua UC-F8.2 trước khi cấp chỗ cho request mới. Không được lấy thêm khóa `members` sau khi đã giữ khóa `class_sessions`. Nếu transaction hiện tại chưa khóa đủ các dòng ứng viên trước session, rollback và chạy promotion trong transaction riêng, sau đó đọc lại trạng thái và retry request; không lồng transaction có thứ tự khóa ngược.
9. Nếu sau bước 8 còn chỗ cho request hiện tại, hệ thống tạo `class_bookings.status = 'BOOKED'` và ghi `booked_at`.
10. Nếu lớp đã đầy, hệ thống tạo `class_bookings.status = 'WAITLIST'`, ghi `waitlisted_at`; không ghi vị trí thứ tự cố định.
11. Hệ thống commit giao dịch và trả kết quả. Khi lớp đầy, hội viên được thông báo mình đang ở danh sách chờ.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **A1 – Lớp còn chỗ, không có người đang chờ:** Tại bước 8, hệ thống tạo `BOOKED`.
- **A2 – Lớp đầy:** Tại bước 9, hệ thống tạo `WAITLIST`; đây là kết quả thành công, không phải lỗi. Vị trí chờ được tính khi truy vấn theo `waitlisted_at` tăng dần.
- **E1 – Request sai định dạng (400 Bad Request):** ID hoặc dữ liệu request sai định dạng; trả `ProblemDetail` có `errors` theo trường.
- **E2 – Không có quyền (403 Forbidden):** Tài khoản không có vai trò hợp lệ, hội viên yêu cầu đặt cho người khác, hoặc nhân viên truy cập ngoài phạm vi chi nhánh được phân quyền.
- **E3 – Không tìm thấy buổi/hội viên (404 Not Found):** Tài nguyên không tồn tại.
- **E4 – Đã có booking đang hoạt động (409 Conflict):** Hội viên đã có dòng cho cùng buổi với trạng thái khác `CANCELLED`; trả thông báo “Hội viên đã có chỗ đặt hoặc đang trong danh sách chờ của buổi này.”
- **E5 – Xung đột đồng thời (409 Conflict):** Khóa/unique constraint phát hiện cạnh tranh; rollback và yêu cầu tải lại hoặc thử lại. Không để vượt sĩ số.
- **E6 – Vi phạm điều kiện đặt (422 Unprocessable Content):** Không có gói TIME phù hợp, gói không `ACTIVE`/không dùng tại chi nhánh, bị khóa đặt lớp, ngoài cửa sổ đặt chỗ, hoặc buổi không nhận đặt; nêu lý do cụ thể bằng tiếng Việt.
- **E7 – Trùng giờ với đăng ký đang chiếm chỗ (409 Conflict):** Hội viên đã có chỗ ở buổi khác có thời gian giao nhau; trả `ProblemDetail` nêu lớp bị xung đột. Cách xử lý trường hợp buổi kia chỉ ở `WAITLIST` cần owner chốt riêng.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F8.1-01:** Chỉ gói `TIME` `ACTIVE` dùng được tại chi nhánh; gói `SESSION` và `PT` không dùng đặt lớp. Bản chốt phạm vi, mục F8; ERD `class_bookings.status`.
- **BR-F8.1-02:** Cửa sổ đặt trước là `BOOKING_WINDOW_DAYS` (mặc định 7 ngày). Cách tính chính xác giữa ngày lịch và 168 giờ chưa được chốt; không hardcode.
- **BR-F8.1-03:** Số chỗ giữ không vượt `class_sessions.capacity`; các trạng thái giữ chỗ là `BOOKED`, `PENDING_RECONFIRM`, `ATTENDED`, `NO_SHOW`; `WAITLIST` và `CANCELLED` không chiếm chỗ.
- **BR-F8.1-04:** Bản chốt phạm vi cấm hội viên đăng ký hai buổi trùng giờ; một booking đã `CANCELLED` không còn ngăn đặt lại. Người thao tác hộ không được nhảy hàng hoặc đặt ngoài thứ tự waitlist.
- **BR-F8.1-05:** Không giới hạn số danh sách chờ một hội viên tham gia.
- **BR-F8.1-06:** Một request đặt mới không được chiếm chỗ đang trống nếu còn hội viên đủ điều kiện ở waitlist; hệ thống xét FIFO trước khi xác nhận chỗ mới.
- **NF6:** Thực hiện trong transaction; khóa dòng `members` trước `class_sessions`; kiểm tra trùng giờ và sĩ số sau khi có khóa. Ràng buộc duy nhất đang hoạt động: `(class_session_id, member_id)` với `status <> 'CANCELLED'`.
- **Câu hỏi cần chốt:** Khi `waitlisted_at` bằng nhau, chưa có tie-break thứ hai. Chưa xác định cửa sổ 7 ngày là 7 ngày lịch hay 168 giờ; thời điểm đúng biên cần được thống nhất. Bản chốt phạm vi cấm hai buổi trùng giờ nhưng cũng nói không giới hạn số waitlist; cần xác nhận một `WAITLIST` có xung đột với một chỗ đang giữ, hoặc với một `WAITLIST` khác, theo cùng cách nào hay không. Cũng cần owner/backend duyệt cách phối hợp FIFO với NF6 để không lấy khóa `members` sau `class_sessions` và phải revalidate queue/eligibility sau khi lấy khóa.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_bookings`.
- **Bảng kiểm tra:** `class_sessions`, `class_schedules`, `class_types`, `members`, `memberships`, `packages`, `rooms`, `system_configs`, `staff`, `users`.

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `class_bookings` | `class_session_id`, `member_id` | `bigint` | NOT NULL, FK | Buổi và hội viên |
| `class_bookings` | `created_by` | `bigint` | FK tới `users.id`, nullable | ERD ghi lễ tân khi thao tác hộ; convention khi hội viên tự đặt cần xác nhận |
| `class_bookings` | `status` | `varchar` | NOT NULL, CHECK (`BOOKED`, `WAITLIST`, `PENDING_RECONFIRM`, `CANCELLED`, `ATTENDED`, `NO_SHOW`) | Trạng thái booking |
| `class_bookings` | `waitlisted_at`, `booked_at` | `timestamptz` | nullable | Thời điểm vào chờ / đặt chỗ |
| `class_sessions` | `capacity`, `start_at`, `end_at`, `branch_id`, `status` | `int`, `timestamptz`, `bigint`, `varchar` | Các cột bắt buộc NOT NULL; `status` có CHECK (`SCHEDULED`, `RESCHEDULED`, `CANCELLED`, `COMPLETED`) | Sức chứa, thời điểm và chi nhánh |
| `members` | `booking_locked_until`, `status` | `timestamptz`, `varchar` | `booking_locked_until` nullable; `status` NOT NULL, CHECK (`ACTIVE`, `INACTIVE`) | Khóa đặt lớp và trạng thái hồ sơ |
| `memberships` | `package_type`, `branch_id`, `status`, `end_date` | `varchar`, `bigint`, `varchar`, `date` | `package_type` CHECK (`TIME`, `SESSION`, `PT`); `status` CHECK (`PENDING`, `UPCOMING`, `ACTIVE`, `FROZEN`, `EXPIRED`, `TRANSFERRED`, `CANCELLED`) | Xác nhận gói TIME đang dùng được tại chi nhánh |
| `system_configs` | `config_key`, `config_value`, `branch_id` | `varchar(100)`, `varchar(255)`, `bigint` | NOT NULL / branch nullable | Cấu hình toàn chuỗi; cấu hình chi nhánh ghi đè cấu hình chung |

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint (đề xuất, cần owner xác nhận):** `POST /api/v1/class-sessions/{sessionId}/bookings`
- **Header:** `Authorization: Bearer <access_token>`.
- **Request Body (DTO đề xuất):**
  ```json
  {
    "memberId": 42
  }
  ```
  `memberId` chỉ dùng trong luồng lễ tân thao tác hộ; với hội viên tự đặt, danh tính phải lấy từ phiên đăng nhập. Cách biểu diễn hai luồng trong DTO cần backend xác nhận.
- **Response Success (201 Created) – hình dạng đề xuất:**
  ```json
  {
    "bookingId": 101,
    "sessionId": 123,
    "status": "WAITLIST",
    "waitlistedAt": "2026-11-03T10:00:00Z",
    "queuePosition": 2
  }
  ```
- **Màn hình giao diện liên quan:** Lịch lớp hội viên / trang đặt chỗ; wireframe chưa có trong repo.
- **Mã lỗi đề xuất:** `400`, `401`, `403`, `404`, `409`, `422` theo mẫu; full capacity trả `201` với `WAITLIST`, không trả lỗi.

#### H. Tiêu chí chấp nhận (Acceptance Criteria)

- **Kịch bản 1: Đặt chỗ còn sức chứa**
  - **Given** Hội viên có gói TIME `ACTIVE` dùng tại chi nhánh, chưa bị khóa và buổi còn chỗ.
  - **When** Hội viên đặt chỗ.
  - **Then** Hệ thống tạo một booking `BOOKED`, ghi `booked_at`, trả `201 Created` và sĩ số không vượt capacity.
- **Kịch bản 2: Lớp đầy thì vào FIFO waitlist**
  - **Given** Buổi đã đủ chỗ và một hội viên đủ điều kiện chưa có booking trùng giờ.
  - **When** Hội viên đặt chỗ.
  - **Then** Hệ thống tạo `WAITLIST`, ghi `waitlisted_at`, trả thành công và không tăng số chỗ giữ.
- **Kịch bản 3: Hội viên không có gói TIME phù hợp**
  - **Given** Hội viên chỉ có gói `SESSION` hoặc `PT`, hoặc không có gói TIME `ACTIVE` dùng tại chi nhánh.
  - **When** Hội viên đặt chỗ.
  - **Then** Hệ thống trả `422 Unprocessable Content`, không tạo booking.
- **Kịch bản 4: Hai yêu cầu đồng thời tranh chỗ cuối**
  - **Given** Còn đúng một chỗ và hai hội viên hợp lệ gửi yêu cầu đồng thời.
  - **When** Cả hai request được xử lý.
  - **Then** Tối đa một booking ở trạng thái chiếm chỗ; request còn lại được xếp `WAITLIST`; capacity không bị vượt.
- **Kịch bản 5: Hai yêu cầu đồng thời của cùng hội viên bị trùng giờ**
  - **Given** Hội viên đã có booking `BOOKED` ở một buổi; một buổi khác có giờ giao nhau và còn chỗ.
  - **When** Hội viên đặt buổi thứ hai.
  - **Then** Hệ thống trả `409 Conflict`, không tạo booking thứ hai.
- **Kịch bản 8: Hai request đồng thời của cùng hội viên vào hai buổi trùng giờ**
  - **Given** Cùng hội viên đồng thời gửi hai request cho hai session trùng giờ, mỗi session còn chỗ.
  - **When** Hai request được xử lý đồng thời.
  - **Then** Tối đa một request tạo `BOOKED`; request còn lại trả `409 Conflict`; không tạo hai booking chiếm chỗ chồng giờ.
- **Kịch bản 6: Hủy trước đó rồi đặt lại**
  - **Given** Booking cũ của hội viên cho buổi đã `CANCELLED`.
  - **When** Hội viên đặt lại buổi đó và còn đáp ứng điều kiện.
  - **Then** Hệ thống cho tạo booking mới; unique condition không chặn do booking cũ đã hủy.
- **Kịch bản 7: Lễ tân không thể vượt người đang chờ**
  - **Given** Buổi vừa có một chỗ trống và có hội viên đủ điều kiện đang ở đầu waitlist.
  - **When** Lễ tân thao tác đặt chỗ cho hội viên khác.
  - **Then** Hệ thống xử lý người đủ điều kiện đứng đầu waitlist trước; không cấp chỗ cho hội viên mới thay thế người đang chờ.

---

### 3.3. UC-F8.2: Tự động đưa hội viên từ danh sách chờ vào lớp

- **Mã UC:** UC-F8.2
- **Tên Use Case:** Tự động đưa hội viên từ danh sách chờ vào lớp
- **Tác nhân chính:** Hệ thống
- **Tác nhân phụ (nếu có):** F10 (thông báo trong web và email)
- **Mô tả ngắn gọn:** Khi lớp đầy, hệ thống ghi hội viên vào cuối waitlist; khi có chỗ trống và còn đủ thời gian, tự động chuyển người đủ điều kiện đứng đầu FIFO từ `WAITLIST` sang chỗ đã đặt, không chờ hội viên xác nhận.

#### A. Tiền điều kiện (Preconditions)

- Có chỗ trong một buổi học chưa bị hủy.
- Còn ít nhất 30 phút trước `class_sessions.start_at` theo cutoff cấu hình.
- Có một hoặc nhiều booking `WAITLIST`.

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Các booking đủ điều kiện theo FIFO được chuyển thành `BOOKED`; ghi `promoted_at`. `grace_until` xác định 15 phút từ thời điểm gửi email theo DBML; in-app notification được tạo ngay, email được gửi khi chi nhánh mở cửa nếu promotion diễn ra ban đêm. Số chỗ giữ không vượt capacity.
- **Thất bại:** Nếu không tìm thấy ứng viên đủ điều kiện thì các booking chờ tiếp tục giữ thứ tự; chỗ có thể còn trống. Nếu đã qua cutoff thì không promotion.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Hệ thống được kích hoạt khi chỗ được giải phóng hoặc khi có sự kiện cần xét lại waitlist.
2. Hệ thống khóa các dòng liên quan theo thứ tự NF6: dòng `members` ứng viên trước, sau đó dòng `class_sessions`; khi xử lý nhiều ứng viên, thứ tự khóa phải nhất quán để hạn chế deadlock.
3. Hệ thống kiểm tra buổi chưa bị hủy/kết thúc, còn chỗ theo `capacity` và thời gian đến lớp không ở dưới `PROMOTION_CUTOFF_MIN`.
4. Hệ thống lấy các dòng `class_bookings.status = 'WAITLIST'` theo `waitlisted_at` tăng dần; vị trí không được lưu cứng.
5. Với từng ứng viên theo thứ tự, hệ thống kiểm tra gói TIME `ACTIVE` dùng tại chi nhánh, booking lock, lịch trùng giờ và điều kiện khác đã được bản chốt xác định.
6. Nếu ứng viên không còn đủ điều kiện, hệ thống bỏ qua và xét người tiếp theo; không cho phép nhân viên chọn một người phía sau để ưu tiên.
7. Với ứng viên đủ điều kiện, hệ thống đổi trạng thái sang `BOOKED`, ghi `promoted_at`, tính grace theo thời điểm email gửi, rồi tạo yêu cầu thông báo F10.
8. Hệ thống tiếp tục cho các chỗ trống còn lại đến khi đủ capacity, hết ứng viên đủ điều kiện hoặc chạm cutoff.
9. Hệ thống commit giao dịch và F10 thông báo trong web ngay. Nếu ngoài giờ mở cửa, email được lên lịch gửi khi chi nhánh mở cửa; thời hạn grace bắt đầu khi email gửi.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **A1 – Người đầu hàng không còn đủ điều kiện:** Tại bước 5, giữ nguyên thứ tự tương đối của hàng chờ và xét ứng viên tiếp theo. Các điều kiện nêu rõ trong scope/ADR: gói hết hạn/không còn dùng được, đang bị khóa đặt, đã có lịch trùng giờ.
- **A2 – Vacancy phát sinh ban đêm:** Promotion vẫn chạy ngay; thông báo web hiện ngay; email gửi vào giờ mở cửa của chi nhánh; grace tính từ lúc email được gửi.
- **A3 – Đã dưới 30 phút:** Hệ thống không đổi bất kỳ dòng `WAITLIST` nào; chỗ trống được để trống.
- **A4 – Nhiều chỗ trống:** Hệ thống lần lượt promotion đủ người hợp lệ đến khi đạt capacity hoặc hết hàng chờ.
- **E1 – Xung đột đồng thời (409 Conflict nội bộ/ghi log):** Một transaction khác đã đổi trạng thái/sức chứa; hủy transaction hiện tại và tính lại trạng thái từ dữ liệu mới. Không gửi thông báo trùng cho một promotion đã commit.
- **E2 – Lỗi F10:** Không rollback trạng thái đặt chỗ đã commit chỉ vì dịch vụ gửi email tạm lỗi; notification phải được giữ lại để retry theo cơ chế F10. Cách retry cụ thể thuộc owner F10, cần thống nhất giữa hai chức năng.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F8.2-01:** FIFO `waitlisted_at ASC`; vị trí tính khi truy vấn, không lưu số thứ tự cố định (ADR 0003).
- **BR-F8.2-02:** Auto-promote ngay, không cần xác nhận; người đủ điều kiện có grace không phạt. Người không đủ điều kiện bị bỏ qua, xét tiếp người sau.
- **BR-F8.2-03:** Không promotion khi còn dưới `PROMOTION_CUTOFF_MIN=30` phút; đúng 30 phút chưa phải “dưới 30 phút” theo câu chữ scope, cần giữ phép so sánh `<`.
- **BR-F8.2-04:** `PROMOTION_GRACE_MIN=15`; ban đêm email đợi giờ mở cửa, grace tính từ thời điểm gửi email (ADR 0003 và DBML `grace_until`).
- **BR-F8.2-05:** Số chỗ chiếm vẫn gồm `BOOKED`, `PENDING_RECONFIRM`, `ATTENDED`, `NO_SHOW`; exclude `WAITLIST`, `CANCELLED`.
- **NF6:** Giao dịch có khóa bản ghi, không vượt capacity; nhất quán thứ tự khóa `members` rồi `class_sessions`.
- **Câu hỏi cần chốt:** Nếu người waitlist không có user/email (lễ tân đã thao tác trên hồ sơ chưa có tài khoản), ADR chưa quy định cách bắt đầu grace. `waitlisted_at` đồng timestamp chưa có tie-break. Cần xác nhận cách retry email với F10 và cơ chế chống gửi lặp.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_bookings`, `notifications`.
- **Bảng đọc:** `class_sessions`, `members`, `memberships`, `packages`, `system_configs`, `branches`, `users`.

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `class_bookings` | `status` | `varchar` | NOT NULL, CHECK (`BOOKED`, `WAITLIST`, `PENDING_RECONFIRM`, `CANCELLED`, `ATTENDED`, `NO_SHOW`) | `WAITLIST` → `BOOKED` |
| `class_bookings` | `waitlisted_at`, `promoted_at`, `grace_until` | `timestamptz` | nullable | FIFO, thời điểm promotion, mốc grace |
| `class_bookings` | `class_session_id`, `member_id` | `bigint` | NOT NULL, FK | Session/hội viên |
| `class_sessions` | `capacity`, `start_at`, `branch_id`, `status` | `int`, `timestamptz`, `bigint`, `varchar` | Các cột bắt buộc NOT NULL; `status` CHECK (`SCHEDULED`, `RESCHEDULED`, `CANCELLED`, `COMPLETED`) | Sức chứa, cutoff, chi nhánh và trạng thái buổi |
| `members` | `booking_locked_until` | `timestamptz` | nullable | Hội viên đang bị khóa đặt lớp |
| `memberships` | `package_type`, `branch_id`, `status`, `end_date` | `varchar`, `bigint`, `varchar`, `date` | `package_type` CHECK (`TIME`, `SESSION`, `PT`); `status` CHECK (`PENDING`, `UPCOMING`, `ACTIVE`, `FROZEN`, `EXPIRED`, `TRANSFERRED`, `CANCELLED`) | Xác nhận tính đủ điều kiện khi promotion |
| `notifications` | `user_id`, `type`, `channel`, `status`, `scheduled_at`, `sent_at`, `related_entity`, `related_id` | theo ERD | `channel` varchar + CHECK (`IN_APP`, `EMAIL`); `status` varchar + CHECK (`PENDING`, `SENT`, `FAILED`) | F10 in-app/email, tham chiếu booking |
| `system_configs` | `config_key`, `config_value`, `branch_id` | `varchar(100)`, `varchar(255)`, `bigint` | NOT NULL / nullable | Cutoff/grace, override theo chi nhánh |

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint:** Không có API do người dùng gọi; đây là luồng hệ thống được kích hoạt bởi sự kiện giải phóng chỗ.
- **Header / Request Body:** Không áp dụng.
- **Response Success:** Không áp dụng; hệ thống cập nhật booking và tạo notification trong các transaction/luồng có idempotency phù hợp.
- **Màn hình giao diện liên quan:** Danh sách chờ của hội viên và màn hình quản lý lớp; thông báo do F10 hiển thị/gửi.
- Nếu backend cần endpoint nội bộ/job trigger, thiết kế đó phải được owner backend xác nhận; không expose API vận hành promotion cho client để tùy ý chọn thứ tự.

#### H. Tiêu chí chấp nhận (Acceptance Criteria)

- **Kịch bản 1: FIFO và promotion tự động**
  - **Given** Có một chỗ trống, còn ít nhất 30 phút, hai hội viên đủ điều kiện có `waitlisted_at` khác nhau.
  - **When** Hệ thống xử lý chỗ trống.
  - **Then** Hội viên có `waitlisted_at` sớm hơn được đổi từ `WAITLIST` sang `BOOKED`, có `promoted_at`; người tiếp theo chỉ được promotion nếu còn chỗ; không yêu cầu xác nhận.
- **Kịch bản 2: Bỏ qua người không đủ điều kiện**
  - **Given** Người đầu hàng có gói TIME hết hạn hoặc đang bị khóa/trùng giờ; người tiếp theo đủ điều kiện.
  - **When** Hệ thống xử lý vacancy.
  - **Then** Người đầu bị bỏ qua; người kế tiếp được promotion; thông báo tới đúng người được promotion.
- **Kịch bản 3: Vacancy ban đêm**
  - **Given** Chỗ trống phát sinh ban đêm và chi nhánh đang đóng cửa.
  - **When** Người đủ điều kiện được promotion.
  - **Then** `BOOKED` được ghi ngay, in-app notice có ngay, email được hẹn đến giờ mở cửa; grace 15 phút bắt đầu lúc email gửi.
- **Kịch bản 4: Đúng cutoff 30 phút**
  - **Given** Còn đúng 30 phút trước `start_at` và có người đủ điều kiện trong waitlist.
  - **When** Hệ thống xử lý chỗ trống.
  - **Then** Vì rule là ngừng khi còn **dưới** 30 phút, hệ thống vẫn được phép xét promotion tại đúng mốc 30 phút.
- **Kịch bản 5: Đã dưới cutoff**
  - **Given** Còn 29 phút trước giờ bắt đầu.
  - **When** Có chỗ trống.
  - **Then** Hệ thống không promotion; chỗ để trống.
- **Kịch bản 6: Nhiều yêu cầu đồng thời**
  - **Given** Nhiều transaction cùng xử lý cùng buổi và chỉ còn một chỗ.
  - **When** Các transaction chạy đồng thời.
  - **Then** Tối đa một booking được promotion vào chỗ cuối; sĩ số không vượt capacity và không gửi thông báo promotion trùng do transaction rollback.
- **Kịch bản 7: Promotion đồng thời cho hai buổi trùng giờ sau khi đổi lịch**
  - **Given** Hai waitlist của cùng một hội viên được tạo khi hai session chưa trùng giờ; sau đó một session được đổi giờ nên hai session giao nhau; mỗi session có một chỗ trống và hội viên B đủ điều kiện đứng sau ở waitlist của session thứ hai.
  - **When** Hai tác vụ promotion chạy đồng thời cho hai session.
  - **Then** Hai transaction khóa dòng `members` trước `class_sessions`; chỉ một session được chuyển booking của hội viên đầu hàng sang `BOOKED`. Transaction còn lại kiểm tra lại sau khi lấy khóa, nhận ra lịch trùng và bỏ qua hội viên này theo ADR 0003, rồi xét hội viên kế tiếp. Không tạo hai booking `BOOKED` chồng giờ cho cùng hội viên.

---

### 3.4. UC-F8.3: Hủy chỗ lớp nhóm

- **Mã UC:** UC-F8.3
- **Tên Use Case:** Hủy chỗ lớp nhóm
- **Tác nhân chính:** Hội viên (`MEMBER`), Lễ tân (`RECEPTIONIST`) khi thao tác hộ hội viên
- **Tác nhân phụ (nếu có):** Hệ thống tự động đưa người đủ điều kiện từ waitlist vào chỗ trống (UC-F8.2)
- **Mô tả ngắn gọn:** Hội viên hủy booking của mình; lễ tân có thể thực hiện thay. Hệ thống ghi nhận đúng hủy sớm/hủy muộn và xem xét người tiếp theo trong waitlist khi có chỗ.

#### A. Tiền điều kiện (Preconditions)

- Người thao tác đã đăng nhập; hội viên chỉ hủy booking của mình, lễ tân có quyền thao tác hộ theo S3.
- Booking tồn tại, thuộc hội viên được phép thao tác, và đang ở trạng thái có thể hủy.

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Booking chuyển `CANCELLED`, ghi `cancelled_at`, `cancel_reason` và `is_late_cancel` theo quy tắc đã chốt. Nếu booking chiếm chỗ, hệ thống kích hoạt UC-F8.2 để xét lấp chỗ; việc này vẫn tuân cutoff.
- **Thất bại:** Booking giữ nguyên; không tính phạt và không mở chỗ nếu transaction thất bại.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Hội viên mở booking của mình và chọn hủy; hoặc lễ tân tra cứu booking theo yêu cầu hội viên.
2. Hệ thống xác thực vai trò, quyền sở hữu hội viên và phạm vi chi nhánh.
3. Hệ thống kiểm tra booking còn hoạt động.
4. Hệ thống đọc `LATE_CANCEL_HOURS` và thời gian bắt đầu của buổi.
5. Nếu booking là chỗ đã xác nhận (`BOOKED`) hoặc chỗ vừa được promotion trong grace, hệ thống xác định hủy sớm/hủy muộn; trạng thái WAITLIST không được tính là chỗ đã xác nhận.
6. Hệ thống trong transaction khóa dữ liệu liên quan để tránh hủy hai lần/đưa vào lớp vượt sĩ số.
7. Hệ thống đặt `status = 'CANCELLED'`, `cancelled_at = now()`, `cancel_reason` phù hợp; với hủy muộn đặt `is_late_cancel = true`.
8. Nếu hủy muộn, hệ thống cập nhật số lần vi phạm của tháng theo dữ liệu booking và áp dụng `booking_locked_until` khi đủ ngưỡng `PENALTY_LIMIT_PER_MONTH`.
9. Nếu chỗ được giải phóng, hệ thống kích hoạt UC-F8.2; không đưa ai vào nếu đã qua cutoff.
10. Hệ thống trả kết quả và thông báo kết quả hủy.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **A1 – Hủy trước giờ học ít nhất 2 giờ:** Tại bước 5, nếu còn ít nhất `LATE_CANCEL_HOURS`, hệ thống đặt `is_late_cancel = false`; không tăng số lần vi phạm.
- **A2 – Hủy booking vừa được promotion trong grace:** Chỉ booking vừa được hệ thống tự động chuyển từ `WAITLIST` sang `BOOKED` được hưởng 15 phút grace không phạt; grace tính từ lúc email promotion được gửi (`grace_until`). Không áp dụng grace này cho booking `BOOKED` đặt trực tiếp, dòng `WAITLIST`, hay hủy buổi học. Trường hợp hội viên không có email cần owner F10/F8 chốt mốc grace.
- **A3 – Hủy booking WAITLIST:** Hệ thống cho hủy dòng waitlist và đặt `CANCELLED`; không có chỗ lớp được giải phóng. Nguồn chuẩn chưa đặc tả kết quả xử lý penalty cho trường hợp này; phần xử lý penalty của nhánh này chưa đủ điều kiện triển khai cho đến khi owner quyết định. Không suy ra mặc định miễn phạt.
- **E1 – Request sai định dạng (400 Bad Request):** Booking ID sai định dạng; trả `ProblemDetail` có lỗi trường.
- **E2 – Không có quyền (403 Forbidden):** Hội viên thao tác booking người khác hoặc nhân viên ngoài phạm vi quyền.
- **E3 – Không tìm thấy booking (404 Not Found):** Booking ID không tồn tại hoặc không hiển thị trong phạm vi dữ liệu caller.
- **E4 – Booking đã kết thúc/hủy hoặc không thể hủy (422 Unprocessable Content):** Trả lý do bằng tiếng Việt; không đổi trạng thái lần nữa.
- **E5 – Xung đột đồng thời (409 Conflict):** Booking đã bị hủy/đổi trạng thái ở request khác; rollback thao tác hiện tại.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F8.3-01:** Hủy trước giờ bắt đầu ít nhất 2 giờ không bị phạt; mốc cấu hình `LATE_CANCEL_HOURS=2`.
- **BR-F8.3-02:** Hủy muộn hoặc no-show đạt 3 lần trong tháng làm khóa đặt lớp 7 ngày (`PENALTY_LIMIT_PER_MONTH=3`, `PENALTY_LOCK_DAYS=7`).
- **BR-F8.3-03:** Chỉ booking vừa được auto-promotion từ `WAITLIST` sang `BOOKED` có 15 phút grace không phạt; ERD mô tả `grace_until` bằng thời điểm email promotion gửi + cấu hình grace. Grace không áp dụng cho mọi lần hủy.
- **BR-F8.3-04:** Hủy booking mở chỗ để xử lý FIFO, nhưng không được auto-promote khi còn dưới 30 phút đến giờ bắt đầu.
- **Câu hỏi cần chốt:** “Trong tháng” là tháng dương lịch theo múi giờ nào hay khoảng trượt 30 ngày; mốc exactly-2-hours đã được scope quyết định là không phạt (“ít nhất 2 giờ”). Grace endpoint inclusive/exclusive và chính sách hủy WAITLIST cần được chốt. Nếu hội viên không có tài khoản/email, thời điểm bắt đầu grace thay thế chưa được định nghĩa.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_bookings`, `members` khi áp khóa đặt lớp.
- **Bảng đọc:** `class_sessions`, `system_configs`, `notifications` (xác định thời điểm gửi email), `users`.

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `class_bookings` | `status` | `varchar` | NOT NULL, CHECK (`BOOKED`, `WAITLIST`, `PENDING_RECONFIRM`, `CANCELLED`, `ATTENDED`, `NO_SHOW`) | Chuyển sang `CANCELLED` |
| `class_bookings` | `cancelled_at` | `timestamptz` | nullable | Thời điểm hủy UTC |
| `class_bookings` | `cancel_reason` | `varchar` | nullable, CHECK (`BY_MEMBER`, `BY_STAFF`, `NOT_RECONFIRMED`, `SESSION_CANCELLED`) | Lý do hủy |
| `class_bookings` | `is_late_cancel` | `boolean` | NOT NULL, default false | Đánh dấu hủy muộn |
| `class_bookings` | `grace_until`, `promoted_at` | `timestamptz` | nullable | Grace sau auto-promotion |
| `members` | `booking_locked_until` | `timestamptz` | nullable | Hạn kết thúc khóa đặt lớp |
| `class_sessions` | `start_at` | `timestamptz` | NOT NULL | Dùng so với giờ hủy |
| `system_configs` | `config_key`, `config_value` | `varchar(100)`, `varchar(255)` | NOT NULL | Thời hạn, ngưỡng phạt, thời gian khóa |

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint (đề xuất, cần owner xác nhận):** `POST /api/v1/class-bookings/{bookingId}/cancel`
- **Header:** `Authorization: Bearer <access_token>`.
- **Request Body (DTO đề xuất):** `{}`; caller role xác định `cancel_reason` (`BY_MEMBER` hoặc `BY_STAFF`), không nhận `is_late_cancel` từ client.
- **Response Success (200 OK) – hình dạng đề xuất:**
  ```json
  {
    "bookingId": 101,
    "status": "CANCELLED",
    "isLateCancel": false,
    "bookingLockedUntil": null
  }
  ```
- **Màn hình giao diện liên quan:** Chi tiết booking / lịch lớp hội viên và màn hình lễ tân thao tác hộ.
- **Mã lỗi đề xuất:** `400`, `401`, `403`, `404`, `409`, `422`; không nhận cờ phạt từ trình duyệt.

#### H. Tiêu chí chấp nhận (Acceptance Criteria)

- **Kịch bản 1: Hủy đúng mốc 2 giờ**
  - **Given** Hội viên có booking `BOOKED` và thời điểm hủy cách `start_at` đúng 2 giờ.
  - **When** Hội viên hủy booking.
  - **Then** Booking thành `CANCELLED`, `is_late_cancel = false`, không tăng số lần vi phạm; chỗ được xét cho waitlist theo UC-F8.2.
- **Kịch bản 2: Hủy muộn lần thứ ba trong tháng**
  - **Given** Hội viên đã có 2 lần hủy muộn/no-show trong kỳ tháng đang áp dụng theo cấu hình đã được owner xác nhận.
  - **When** Hội viên hủy booking muộn.
  - **Then** Lần hủy được ghi `is_late_cancel = true`, hội viên bị khóa đặt trong 7 ngày theo `PENALTY_LOCK_DAYS`, trả thời điểm hết khóa.
- **Kịch bản 3: Hủy promotion trong grace**
  - **Given** Hội viên vừa được đưa vào lớp và thời điểm hiện tại nằm trong khoảng grace tính từ email promotion đã gửi.
  - **When** Hội viên hủy booking.
  - **Then** Booking chuyển `CANCELLED`, không đánh dấu hủy muộn và không tăng penalty count.
- **Kịch bản 4: Hai yêu cầu hủy đồng thời**
  - **Given** Hai request cùng hủy một booking đang hoạt động.
  - **When** Hai request được xử lý đồng thời.
  - **Then** Chỉ một lần chuyển trạng thái thành công; không mở hai chỗ hoặc tạo hai promotion cho cùng một chỗ.

---

### 3.5. UC-F8.4: Xác nhận lại hoặc từ chối khi đổi giờ lớp

- **Mã UC:** UC-F8.4
- **Tên Use Case:** Xác nhận lại hoặc từ chối khi đổi giờ lớp
- **Tác nhân chính:** Hội viên (`MEMBER`); Hệ thống cho hạn xác nhận; Lễ tân có thể hỗ trợ theo quyền thao tác hộ
- **Tác nhân phụ (nếu có):** Quản lý chi nhánh (`BRANCH_MANAGER`), F10
- **Mô tả ngắn gọn:** Khi quản lý đổi giờ một buổi, hội viên đang có chỗ xác nhận tham gia giờ mới trong thời hạn cấu hình hoặc từ chối. Hết hạn/từ chối thì mất chỗ, không bị phạt; chỗ được xét cho waitlist.

#### A. Tiền điều kiện (Preconditions)

- Buổi học tồn tại và quản lý chi nhánh đã thay đổi giờ bắt đầu/kết thúc.
- Hội viên có booking chỗ đã xác nhận trước khi đổi giờ.
- Booking đang ở trạng thái `PENDING_RECONFIRM` và chưa hết `reconfirm_expires_at`.

#### B. Hậu điều kiện (Postconditions)

- **Thành công – xác nhận:** Booking chuyển về `BOOKED`; hội viên giữ chỗ theo thời gian mới.
- **Thành công – từ chối/hết hạn:** Booking chuyển `CANCELLED`, không phạt; chỗ được giải phóng và UC-F8.2 được xét theo cutoff.
- **Thất bại:** Không đổi trạng thái booking; không mở chỗ nếu thao tác không commit.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Quản lý chi nhánh thay đổi giờ của `class_sessions`.
2. Hệ thống lưu `original_start_at`, cập nhật `start_at`/`end_at`, đặt trạng thái buổi `RESCHEDULED`.
3. Hệ thống xác định các booking `BOOKED` đang chiếm chỗ và chuyển sang `PENDING_RECONFIRM`.
4. Hệ thống đặt `reconfirm_expires_at` theo `RECONFIRM_TTL_MIN=60`, phát thông báo đổi giờ qua F10.
5. Hội viên mở thông báo/lịch lớp và chọn xác nhận giờ mới hoặc không tham gia; lễ tân có thể thao tác thay theo quyền đã cấp.
6. Hệ thống kiểm tra quyền, booking vẫn `PENDING_RECONFIRM`, và request đến trước hạn.
7. Nếu xác nhận, hệ thống chuyển booking về `BOOKED`; nếu từ chối, hệ thống chuyển booking `CANCELLED` với lý do phù hợp, không phạt.
8. Hệ thống commit thay đổi. Khi chỗ được giải phóng, hệ thống gọi UC-F8.2; không promotion sau cutoff.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **A1 – Hội viên không phản hồi trước hạn:** Tác vụ nền tìm `PENDING_RECONFIRM` quá `reconfirm_expires_at`, chuyển booking sang `CANCELLED` và giải phóng chỗ; không tính phạt; xét waitlist.
- **A2 – Hội viên báo không tham gia:** Hệ thống hủy booking ngay, không phạt, rồi xét người trong waitlist.
- **A3 – Chỉ đổi phòng:** Không chuyển booking sang `PENDING_RECONFIRM`; chỉ thông báo thay đổi phòng theo rule F8.
- **A4 – Chỉ đổi giờ:** Giữ nguyên chỗ hiện tại nhưng yêu cầu reconfirm trong hạn 60 phút.
- **E1 – Request sai định dạng (400 Bad Request):** Payload/booking ID không hợp lệ.
- **E2 – Không có quyền (403 Forbidden):** Caller không phải chủ booking, lễ tân không có quyền thao tác hộ, hoặc quản lý không thuộc chi nhánh.
- **E3 – Không tìm thấy buổi/booking (404 Not Found):** Tài nguyên không tồn tại trong phạm vi truy cập.
- **E4 – Hạn đã hết hoặc booking không chờ xác nhận (422 Unprocessable Content):** Hệ thống từ chối cập nhật và trả trạng thái mới nhất.
- **E5 – Xung đột đồng thời (409 Conflict):** Request xác nhận và tác vụ timeout đồng thời; chỉ một chuyển trạng thái được commit; thao tác còn lại nhận `409`.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F8.4-01:** Đổi giờ yêu cầu hội viên đã có chỗ xác nhận lại trong 60 phút (`RECONFIRM_TTL_MIN=60`).
- **BR-F8.4-02:** Không xác nhận hoặc báo không tham gia thì mất chỗ, không bị phạt; chỗ chuyển cho danh sách chờ theo UC-F8.2.
- **BR-F8.4-03:** Đổi phòng chỉ thông báo; không yêu cầu reconfirm.
- **BR-F8.4-04:** `PENDING_RECONFIRM` vẫn chiếm chỗ trong capacity (ERD/thiet-ke-du-lieu.md).
- **Câu hỏi cần chốt:** `cancel_reason` chính xác cho từ chối chủ động và hết hạn chưa được tách rõ; ERD có `NOT_RECONFIRMED`. Chưa nêu cách xử lý khi 60 phút hết sau giờ bắt đầu lớp, hoặc giờ mới tạo trùng lịch với buổi khác của hội viên. SRS này không tự chọn chính sách cho các trường hợp đó.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_sessions`, `class_bookings`, `notifications`.
- **Bảng đọc:** `members`, `system_configs`, `users`.

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `class_sessions` | `start_at`, `end_at`, `original_start_at`, `status` | `timestamptz`, `varchar` | start/end NOT NULL; status CHECK (`SCHEDULED`, `RESCHEDULED`, `CANCELLED`, `COMPLETED`) | Giờ mới, giờ cũ, `RESCHEDULED` |
| `class_bookings` | `status` | `varchar` | NOT NULL, CHECK (`BOOKED`, `WAITLIST`, `PENDING_RECONFIRM`, `CANCELLED`, `ATTENDED`, `NO_SHOW`) | `BOOKED` → `PENDING_RECONFIRM` → `BOOKED` hoặc `CANCELLED` |
| `class_bookings` | `reconfirm_expires_at` | `timestamptz` | nullable | Deadline xác nhận lại |
| `class_bookings` | `cancel_reason`, `cancelled_at`, `is_late_cancel` | `varchar`, `timestamptz`, `boolean` | reason nullable + CHECK (`BY_MEMBER`, `BY_STAFF`, `NOT_RECONFIRMED`, `SESSION_CANCELLED`); thời gian nullable; `is_late_cancel` NOT NULL | Hủy do từ chối/hết hạn không phạt |
| `notifications` | `user_id`, `type`, `channel`, `status`, `scheduled_at`, `sent_at`, `related_entity`, `related_id` | theo ERD | channel varchar + CHECK (`IN_APP`, `EMAIL`); status varchar + CHECK (`PENDING`, `SENT`, `FAILED`) | F10 báo đổi giờ |
| `system_configs` | `config_key`, `config_value` | `varchar(100)`, `varchar(255)` | NOT NULL | Key `RECONFIRM_TTL_MIN`, mặc định 60 phút; thời hạn confirm lại |

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint (đề xuất, cần owner xác nhận):**
  - Quản lý đổi giờ: `POST /api/v1/class-sessions/{sessionId}/reschedule`.
  - Hội viên xác nhận/từ chối: `POST /api/v1/class-bookings/{bookingId}/reconfirm`.
- **Header:** `Authorization: Bearer <access_token>`.
- **Request Body (DTO đề xuất):**
  ```json
  {
    "startAt": "2026-11-12T19:00:00+07:00",
    "endAt": "2026-11-12T20:00:00+07:00"
  }
  ```
  ```json
  {
    "confirm": true
  }
  ```
- **Response Success – hình dạng đề xuất:** `200 OK` kèm `status`, `reconfirmExpiresAt`, `startAt`, `endAt` mới.
- **Màn hình giao diện liên quan:** Quản lý chi tiết buổi; hội viên lịch lớp/notification center.
- Route, method, cách mã hóa từ chối và HTTP response cần owner F8/backend xác nhận.

#### H. Tiêu chí chấp nhận (Acceptance Criteria)

- **Kịch bản 1: Xác nhận giờ mới trong 60 phút**
  - **Given** Booking đang `PENDING_RECONFIRM` và còn trước `reconfirm_expires_at`.
  - **When** Hội viên xác nhận tham gia giờ mới.
  - **Then** Booking về `BOOKED`, vẫn chiếm một chỗ, lưu giờ mới ở session và không nhân đôi booking.
- **Kịch bản 2: Từ chối tham gia**
  - **Given** Booking đang `PENDING_RECONFIRM`.
  - **When** Hội viên chọn không tham gia.
  - **Then** Booking thành `CANCELLED`, không ghi phạt, chỗ được giải phóng và chuyển sang UC-F8.2 nếu chưa qua cutoff.
- **Kịch bản 3: Hết hạn xác nhận**
  - **Given** Booking chờ xác nhận đã quá hạn 60 phút.
  - **When** Tác vụ nền xử lý deadline.
  - **Then** Booking bị hủy không phạt và chỗ được xét cho danh sách chờ.
- **Kịch bản 4: Đổi phòng nhưng không đổi giờ**
  - **Given** Quản lý chỉ đổi `room_id`.
  - **When** Cập nhật buổi.
  - **Then** Hội viên được thông báo; booking không chuyển `PENDING_RECONFIRM`.
- **Kịch bản 5: Timeout cạnh tranh xác nhận**
  - **Given** Hội viên gửi xác nhận đúng lúc tác vụ hết hạn đang xử lý cùng booking.
  - **When** Hai transaction cạnh tranh.
  - **Then** Chỉ một trạng thái cuối được commit; không vừa giữ chỗ vừa chuyển booking sang waitlist/đã hủy.

---

### 3.6. UC-F8.5: Xem lịch dạy lớp nhóm

- **Mã UC:** UC-F8.5
- **Tên Use Case:** Xem lịch dạy lớp nhóm
- **Tác nhân chính:** Huấn luyện viên (`TRAINER`)
- **Tác nhân phụ (nếu có):** Không
- **Mô tả ngắn gọn:** Huấn luyện viên xem các buổi lớp nhóm được phân công để chuẩn bị và vào màn hình điểm danh.

#### A. Tiền điều kiện (Preconditions)

- Huấn luyện viên đã đăng nhập với role `TRAINER`.
- Hồ sơ `staff` của người dùng tồn tại.

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Không thay đổi dữ liệu; hệ thống trả lịch chỉ cho buổi có `trainer_id` tương ứng với huấn luyện viên đã đăng nhập.
- **Thất bại:** Không thay đổi dữ liệu; truy cập ngoài quyền bị chặn.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Huấn luyện viên mở lịch dạy và chọn khoảng thời gian.
2. Hệ thống xác thực role `TRAINER` và ánh xạ tài khoản tới `staff.id`.
3. Hệ thống truy vấn `class_sessions` theo `trainer_id` của người dùng và khoảng ngày.
4. Hệ thống trả thông tin buổi: thời gian, loại lớp, phòng, trạng thái buổi và tổng số booking theo các trạng thái liên quan.
5. Huấn luyện viên chọn một buổi để mở UC-F8.6 điểm danh.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **A1 – Không có lịch trong khoảng:** Trả danh sách rỗng.
- **A2 – Buổi có huấn luyện viên dạy thay:** Nếu `class_sessions.trainer_id` đã được đổi theo ERD, chỉ người được gán hiện tại thấy buổi trong lịch của mình; lịch lặp gốc không thay thế phân công của session cụ thể.
- **E1 – Query sai định dạng (400 Bad Request):** Ngày lọc không hợp lệ.
- **E2 – Chưa đăng nhập (401 Unauthorized):** Token thiếu/sai/hết hạn.
- **E3 – Sai vai trò hoặc xem lịch người khác (403 Forbidden):** Không trả lịch huấn luyện viên khác.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F8.5-01:** `class_sessions.trainer_id` là huấn luyện viên được phân công cho từng buổi; ERD cho phép thay đổi người dạy riêng ở session.
- **BR-F8.5-02:** S3: quyền và phạm vi dữ liệu phải kiểm tra ở backend, không dựa vào lọc giao diện.
- **BR-F8.5-03:** Lịch lớp nhóm và lịch PT có thể cùng xuất hiện trong sơ đồ tổng hợp F9; use case này chỉ trả buổi lớp nhóm.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** Không có; chỉ đọc.
- **Bảng truy vấn:** `staff`, `users`, `class_sessions`, `class_schedules`, `class_types`, `rooms`, `class_bookings`.

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `class_sessions` | `trainer_id`, `branch_id`, `start_at`, `end_at`, `room_id`, `status` | bigint, timestamptz, varchar | Các cột bắt buộc NOT NULL; status CHECK (`SCHEDULED`, `RESCHEDULED`, `CANCELLED`, `COMPLETED`) | Lịch từng buổi và phân công hiện tại |
| `class_schedules` | `class_type_id` | `bigint` | FK | Loại lớp của buổi |
| `class_types` | `name` | `varchar(100)` | UNIQUE, NOT NULL | Tên lớp |
| `class_bookings` | `class_session_id`, `status` | bigint, varchar | NOT NULL; status CHECK (`BOOKED`, `WAITLIST`, `PENDING_RECONFIRM`, `CANCELLED`, `ATTENDED`, `NO_SHOW`) | Số chỗ và trạng thái attendance, nếu giao diện hiển thị |
| `staff` | `id` | `bigint` | PK | Khớp `trainer_id` |

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint (đề xuất, cần owner xác nhận):** `GET /api/v1/trainers/me/class-sessions?from={date}&to={date}`.
- **Header:** `Authorization: Bearer <access_token>`.
- **Request Body (DTO):** Không có; bộ lọc ngày là query params.
- **Response Success (200 OK) – hình dạng đề xuất:**
  ```json
  {
    "items": [
      {
        "sessionId": 123,
        "classTypeName": "Yoga",
        "startAt": "2026-11-12T18:00:00+07:00",
        "endAt": "2026-11-12T19:00:00+07:00",
        "roomName": "Phòng 201",
        "status": "SCHEDULED"
      }
    ]
  }
  ```
- **Màn hình giao diện liên quan:** Lịch dạy của huấn luyện viên; wireframe hiện chưa được liên kết trong repo.

#### H. Tiêu chí chấp nhận (Acceptance Criteria)

- **Kịch bản 1: Huấn luyện viên xem đúng lịch được phân công**
  - **Given** Trainer A có các session được gán `trainer_id` của A và có session của Trainer B trong cùng chi nhánh.
  - **When** Trainer A gọi lịch của mình.
  - **Then** Chỉ trả session có `trainer_id` của A, bất kể client truyền tham số nào nhằm xem Trainer B.
- **Kịch bản 2: Khoảng thời gian không có lớp**
  - **Given** Khoảng lọc hợp lệ nhưng không có session được phân công.
  - **When** Trainer xem lịch.
  - **Then** Trả `200 OK` với danh sách rỗng.
- **Kịch bản 3: Nhân viên role khác yêu cầu lịch trainer**
  - **Given** Caller đã đăng nhập nhưng không có role `TRAINER`.
  - **When** Caller gọi endpoint.
  - **Then** Trả `403 Forbidden` và không lộ dữ liệu lịch.

---

### 3.7. UC-F8.6: Điểm danh học viên lớp học nhóm

- **Mã UC:** UC-F8.6
- **Tên Use Case:** Điểm danh học viên lớp học nhóm
- **Tác nhân chính:** Huấn luyện viên (`TRAINER`) được phân công dạy buổi
- **Tác nhân phụ (nếu có):** Không
- **Mô tả ngắn gọn:** Huấn luyện viên ghi nhận học viên đã tham dự hoặc vắng mặt cho buổi mình dạy. Check-in ở quầy chỉ đánh dấu `arrived_at`; chỉ điểm danh của huấn luyện viên quyết định `ATTENDED`/`NO_SHOW`.

#### A. Tiền điều kiện (Preconditions)

- Huấn luyện viên đăng nhập role `TRAINER`.
- Buổi học tồn tại và `class_sessions.trainer_id` thuộc huấn luyện viên.
- Booking cần điểm danh đang chiếm chỗ và chưa bị hủy.

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** `class_bookings.status` chuyển thành `ATTENDED` hoặc `NO_SHOW`; `attendance_marked_by` lưu `users.id` của huấn luyện viên. `NO_SHOW` được tính vào quy tắc khóa đặt lớp theo bản chốt.
- **Thất bại:** Không thay đổi attendance; trạng thái hiện tại được giữ nguyên.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Huấn luyện viên mở một buổi được phân công từ UC-F8.5.
2. Hệ thống xác thực role, `staff.id` và so khớp với `class_sessions.trainer_id`.
3. Hệ thống trả danh sách booking đang có chỗ cần điểm danh; không trộn các dòng chỉ đang `WAITLIST` hoặc đã `CANCELLED`.
4. Huấn luyện viên chọn từng hội viên và ghi nhận có mặt (`ATTENDED`) hoặc vắng mặt (`NO_SHOW`).
5. Hệ thống validate rằng trạng thái được chọn là một trong hai kết quả attendance và booking hợp lệ.
6. Hệ thống cập nhật booking và ghi `attendance_marked_by`.
7. Nếu trạng thái là `NO_SHOW`, hệ thống cộng sự kiện này vào quy tắc “3 lần trong tháng”, và khóa đặt lớp 7 ngày khi đạt ngưỡng.
8. Hệ thống trả kết quả đã điểm danh.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **A1 – Hội viên đã check-in ở quầy:** Nếu `arrived_at` đã có, vẫn cần huấn luyện viên xác nhận `ATTENDED`; check-in không tự chuyển trạng thái attendance.
- **A2 – Điểm danh cả danh sách:** Huấn luyện viên có thể gửi nhiều kết quả trong một request nếu API được owner chấp nhận; mọi dòng phải được validate và transaction semantics cần chốt.
- **E1 – Payload sai định dạng (400 Bad Request):** Trạng thái ngoài tập cho phép hoặc trường bắt buộc thiếu.
- **E2 – Chưa đăng nhập (401 Unauthorized):** Token không hợp lệ.
- **E3 – Không được phân công / sai quyền (403 Forbidden):** Huấn luyện viên không phải trainer của session.
- **E4 – Session/booking không tồn tại (404 Not Found):** Trả `ProblemDetail` tiếng Việt.
- **E5 – Booking không thể điểm danh (422 Unprocessable Content):** Booking đang `WAITLIST`, `CANCELLED` hoặc đã được điểm danh; không cập nhật.
- **E6 – Xung đột cập nhật đồng thời (409 Conflict):** Trạng thái attendance đã được thay đổi trong request khác.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F8.6-01:** Điểm danh lớp là của huấn luyện viên; `attendance_marked_by` tham chiếu `users.id`.
- **BR-F8.6-02:** F4 ghi `arrived_at` khi check-in có lớp sắp bắt đầu; điều đó không thay cho `ATTENDED` do trainer xác nhận (scope F4 và ERD).
- **BR-F8.6-03:** Vắng mặt theo điểm danh của huấn luyện viên là một trong các sự kiện tính penalty; đủ 3 lần trong tháng khóa đặt lớp 7 ngày.
- **BR-F8.6-04:** Booking `ATTENDED` và `NO_SHOW` vẫn được tính trong sĩ số của session (thiết kế dữ liệu ERD).
- **Câu hỏi cần chốt:** Khoảng thời gian được phép điểm danh, có cho sửa kết quả đã ghi hay không, và xử lý NO_SHOW khi huấn luyện viên chưa điểm danh chưa được scope định nghĩa. Không tự coi hết giờ là vắng.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_bookings`, có thể `members.booking_locked_until` khi đủ ngưỡng.
- **Bảng đọc:** `class_sessions`, `staff`, `users`, `system_configs`.

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `class_bookings` | `status` | `varchar` | NOT NULL, CHECK (`BOOKED`, `WAITLIST`, `PENDING_RECONFIRM`, `CANCELLED`, `ATTENDED`, `NO_SHOW`); UC này chỉ cập nhật sang `ATTENDED` hoặc `NO_SHOW` | `ATTENDED` hoặc `NO_SHOW` |
| `class_bookings` | `attendance_marked_by` | `bigint` | FK tới `users.id` | Tài khoản trainer điểm danh |
| `class_bookings` | `arrived_at` | `timestamptz` | nullable | F4 ghi nhận check-in tại quầy, khác attendance |
| `class_sessions` | `trainer_id` | `bigint` | FK tới `staff.id` | Quyền điểm danh buổi được phân công |
| `members` | `booking_locked_until` | `timestamptz` | nullable | Hạn khóa sau đủ số late cancel/no-show |
| `system_configs` | `config_key`, `config_value` | `varchar(100)`, `varchar(255)` | NOT NULL | Keys `PENALTY_LIMIT_PER_MONTH` (mặc định 3) và `PENALTY_LOCK_DAYS` (mặc định 7); ngưỡng và thời hạn khóa |

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint (đề xuất, cần owner xác nhận):** `PUT /api/v1/class-sessions/{sessionId}/attendance`.
- **Header:** `Authorization: Bearer <access_token>`.
- **Request Body (DTO đề xuất):**
  ```json
  {
    "items": [
      {
        "bookingId": 101,
        "status": "ATTENDED"
      },
      {
        "bookingId": 102,
        "status": "NO_SHOW"
      }
    ]
  }
  ```
- **Response Success (200 OK) – hình dạng đề xuất:**
  ```json
  {
    "sessionId": 123,
    "updatedCount": 2
  }
  ```
- **Màn hình giao diện liên quan:** Danh sách học viên và điểm danh buổi lớp nhóm; wireframe hiện chưa có file được chỉ định.
- Gửi từng học viên hay cả danh sách, và tính nguyên tử của bulk update cần owner xác nhận.

#### H. Tiêu chí chấp nhận (Acceptance Criteria)

- **Kịch bản 1: Trainer ghi nhận học viên có mặt**
  - **Given** Trainer được phân công; booking `BOOKED` có `arrived_at` hoặc học viên đã có mặt tại lớp.
  - **When** Trainer đánh dấu `ATTENDED`.
  - **Then** Booking thành `ATTENDED`, ghi `attendance_marked_by`; `arrived_at` không bị dùng làm nguồn thay thế cho thao tác này.
- **Kịch bản 2: Trainer ghi nhận vắng mặt đạt ngưỡng**
  - **Given** Hội viên có 2 lần vi phạm được tính trong tháng và trainer được phân công cho buổi.
  - **When** Trainer đánh dấu booking `NO_SHOW`.
  - **Then** Booking thành `NO_SHOW`; hệ thống áp dụng khóa đặt 7 ngày khi sự kiện này là lần thứ ba theo kỳ tháng được cấu hình/owner xác nhận.
- **Kịch bản 3: Trainer khác thử điểm danh**
  - **Given** Trainer B không được gán vào buổi của Trainer A.
  - **When** Trainer B gửi điểm danh.
  - **Then** Trả `403 Forbidden`, không đổi booking.
- **Kịch bản 4: Hội viên đã check-in nhưng chưa được điểm danh**
  - **Given** `arrived_at` đã có nhưng booking vẫn `BOOKED`.
  - **When** Chưa có thao tác attendance của trainer.
  - **Then** Hệ thống vẫn chưa coi booking là `ATTENDED`; check-in tại quầy không tự điểm danh.

---

### 3.8. UC-F8.7: Quản lý lịch lặp lớp học và phân công huấn luyện viên

- **Mã UC:** UC-F8.7
- **Tên Use Case:** Quản lý lịch lặp lớp học và phân công huấn luyện viên
- **Tác nhân chính:** Quản lý chi nhánh (`BRANCH_MANAGER`)
- **Tác nhân phụ (nếu có):** Hệ thống sinh buổi UC-F8.8; huấn luyện viên được phân công
- **Mô tả ngắn gọn:** Quản lý loại lớp và lịch lặp hằng tuần tại chi nhánh; gán phòng, thời gian, sức chứa và huấn luyện viên có role TRAINER. Quản lý cũng có thể hủy một buổi cụ thể theo quy tắc F8.

#### A. Tiền điều kiện (Preconditions)

- Quản lý đăng nhập với role `BRANCH_MANAGER` và có chi nhánh hợp lệ.
- Phòng, loại lớp, huấn luyện viên được chọn tồn tại và thuộc phạm vi chi nhánh theo quy tắc dữ liệu.

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Lịch lặp được tạo/cập nhật hoặc chuyển `INACTIVE`; dữ liệu gốc không bị xóa cứng. Lịch hợp lệ là đầu vào cho UC-F8.8.
- **Thành công khi hủy buổi:** Buổi cụ thể chuyển `CANCELLED`; các booking đang hoạt động và waitlist của buổi bị hủy không phạt; F10 thông báo cho tất cả người bị ảnh hưởng.
- **Thất bại:** Không lưu một phần lịch; transaction rollback.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Quản lý mở mục quản lý lớp học, chọn loại lớp hoặc tạo loại lớp.
2. Quản lý tạo lịch lặp: loại lớp, huấn luyện viên, phòng, thứ trong tuần, giờ bắt đầu/kết thúc, sức chứa, ngày hiệu lực `valid_from` và ngày kết thúc `valid_to` nếu có.
3. Hệ thống validate dữ liệu request và quyền quản lý đúng chi nhánh.
4. Hệ thống xác nhận `day_of_week` theo ISO từ 1 đến 7, `end_time > start_time`, sức chứa dương và không lớn hơn sức chứa phòng.
5. Hệ thống xác nhận `trainer_id` trỏ tới nhân viên có role `TRAINER`, phòng và loại lớp thuộc phạm vi hợp lệ.
6. Hệ thống tạo `class_schedules` trạng thái `ACTIVE` và ghi metadata thời gian.
7. Khi cần ngừng lịch, quản lý chuyển trạng thái sang `INACTIVE` thay vì xóa dữ liệu.
8. Khi hủy một buổi cụ thể, quản lý chọn session; hệ thống hủy session cùng mọi booking đang hoạt động/waitlist của session, không đánh dấu hủy muộn, rồi tạo thông báo cho các hội viên bị ảnh hưởng qua F10.
9. Hệ thống trả lịch/buổi đã lưu; UC-F8.8 chịu trách nhiệm sinh `class_sessions` theo ngày hiệu lực.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **A1 – Cập nhật lịch lặp:** Quản lý sửa các trường được phép của schedule; phạm vi ảnh hưởng đến các `class_sessions` đã sinh chưa được quy định (xem câu hỏi cần chốt).
- **A2 – Ngừng lịch:** Chuyển status sang `INACTIVE`; không xóa cứng schedule.
- **A3 – Chưa có ngày kết thúc:** `valid_to = NULL` biểu thị không khai báo ngày dừng; cách sinh session vẫn phụ thuộc horizon cần chốt.
- **A4 – Hủy một buổi cụ thể:** Chuyển `class_sessions.status = 'CANCELLED'`; mọi booking đang hoạt động, gồm `BOOKED`, `PENDING_RECONFIRM`, `WAITLIST`, được chuyển `CANCELLED` với `cancel_reason = 'SESSION_CANCELLED'`, không phạt; gửi thông báo cho tất cả người có booking/waitlist của buổi. Không áp dụng auto-promotion cho buổi đã hủy.
- **E1 – Dữ liệu sai định dạng/không thỏa constraint (400 Bad Request):** Trả `ProblemDetail` theo field.
- **E2 – Không có quyền/khác chi nhánh (403 Forbidden):** Từ chối và không tiết lộ dữ liệu của chi nhánh khác.
- **E3 – Không tìm thấy phòng/loại lớp/nhân viên (404 Not Found):** Trả lỗi tiếng Việt.
- **E4 – Trùng hoặc xung đột dữ liệu (409 Conflict):** Unique constraint hoặc cập nhật cạnh tranh.
- **E5 – Vi phạm nghiệp vụ (422 Unprocessable Content):** Capacity lớn hơn room capacity, giờ kết thúc không sau giờ bắt đầu, hoặc trainer không có role `TRAINER`.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F8.7-01:** `class_schedules` là lịch lặp theo tuần; `day_of_week`: 1 = thứ Hai … 7 = Chủ nhật (ISO).
- **BR-F8.7-02:** `0 < capacity <= rooms.capacity`; `end_time > start_time`; huấn luyện viên phải có role TRAINER (ERD).
- **BR-F8.7-03:** Lịch lặp chứa chi nhánh, loại lớp, trainer, phòng, ngày/giờ, sức chứa, `valid_from`, `valid_to`, trạng thái.
- **BR-F8.7-04:** Chuyển trạng thái `INACTIVE`, không hard-delete dữ liệu nghiệp vụ.
- **BR-F8.7-05:** Hủy buổi phải hủy toàn bộ chỗ đặt/waitlist, không phạt và thông báo mọi người bị ảnh hưởng (bản chốt phạm vi F8). Không hard-delete `class_sessions`/`class_bookings`.
- **BR-F8.7-06:** Không xếp huấn luyện viên trùng giờ với buổi PT hoặc lớp nhóm khác; quy tắc F9 nêu chặn trùng lịch trainer, kể cả giờ dạy lớp nhóm. Việc xác minh phải tính theo các session cụ thể.
- **Câu hỏi cần chốt:** Scope không mô tả kiểm tra trùng phòng giữa lịch lặp; chưa nêu cách xử lý khi hai recurring schedule đè cùng phòng. Cũng chưa định nghĩa sửa/ngừng lịch sau khi session/booking đã sinh, ngày lễ/ngoại lệ, hoặc giảm capacity dưới số chỗ đã đặt.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_types`, `class_schedules`.
- **Bảng đọc:** `branches`, `rooms`, `staff`, `users`.

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `class_types` | `name`, `description`, `status` | `varchar(100)`, `text`, `varchar` | `name` UNIQUE; `status` NOT NULL, CHECK (`ACTIVE`, `INACTIVE`) | Danh mục Yoga/Zumba/… |
| `class_schedules` | `branch_id`, `class_type_id`, `trainer_id`, `room_id` | `bigint` | NOT NULL, FK | Phạm vi và các liên kết |
| `class_schedules` | `day_of_week` | `int` | CHECK 1..7 | Thứ ISO |
| `class_schedules` | `start_time`, `end_time` | `time` | CHECK end > start | Giờ lặp theo tuần |
| `class_schedules` | `capacity` | `int` | CHECK capacity >0; <= sức chứa phòng theo code | Sĩ số |
| `class_schedules` | `valid_from`, `valid_to` | `date` | `valid_from` NOT NULL; `valid_to` nullable | Khoảng hiệu lực |
| `class_schedules` | `status` | `varchar` | NOT NULL, CHECK (`ACTIVE`, `INACTIVE`), default `ACTIVE` | Bật/ngừng lịch |
| `class_sessions` | `status` | `varchar` | NOT NULL, CHECK (`SCHEDULED`, `RESCHEDULED`, `CANCELLED`, `COMPLETED`) | Buổi riêng chuyển `CANCELLED` khi quản lý hủy |
| `class_bookings` | `status`, `cancel_reason`, `is_late_cancel` | `varchar`, `varchar`, `boolean` | status CHECK (`BOOKED`, `WAITLIST`, `PENDING_RECONFIRM`, `CANCELLED`, `ATTENDED`, `NO_SHOW`); cancel_reason CHECK (`BY_MEMBER`, `BY_STAFF`, `NOT_RECONFIRMED`, `SESSION_CANCELLED`); late NOT NULL, default false | Hủy mọi booking active/waitlist của session với `SESSION_CANCELLED`, không phạt |
| `notifications` | `related_entity`, `related_id`, `user_id`, `type`, `channel`, `status` | theo ERD | `channel` varchar + CHECK (`IN_APP`, `EMAIL`); `status` varchar + CHECK (`PENDING`, `SENT`, `FAILED`) | F10 thông báo tất cả hội viên bị ảnh hưởng |
| `rooms` | `capacity`, `branch_id` | `int`, `bigint` | theo ERD | Xác thực sức chứa và chi nhánh |
| `staff` | `id`, `branch_id` | `bigint` | PK, branch scope | Kiểm tra nhân viên trainer |

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint (đề xuất, cần owner xác nhận):**
  - `POST /api/v1/class-types`
  - `POST /api/v1/class-schedules`
  - `PATCH /api/v1/class-schedules/{scheduleId}`
  - `PATCH /api/v1/class-schedules/{scheduleId}/status`
  - `POST /api/v1/class-sessions/{sessionId}/cancel`
- **Header:** `Authorization: Bearer <access_token>`.
- **Request Body (DTO đề xuất):**
  ```json
  {
    "classTypeId": 5,
    "trainerId": 22,
    "roomId": 3,
    "dayOfWeek": 2,
    "startTime": "18:00:00",
    "endTime": "19:00:00",
    "capacity": 20,
    "validFrom": "2026-11-10",
    "validTo": null
  }
  ```
- **Response Success (201 Created) – hình dạng đề xuất:** trả `scheduleId`, `branchId`, các trường lịch và `status: "ACTIVE"`.
- **Màn hình giao diện liên quan:** Quản lý lịch lớp ở chi nhánh; wireframe chưa được liên kết trong repo.
- Danh sách endpoint, cách cập nhật, phân quyền OWNER và HTTP response là đề xuất, cần owner F8/backend duyệt.

#### H. Tiêu chí chấp nhận (Acceptance Criteria)

- **Kịch bản 1: Tạo lịch lặp hợp lệ**
  - **Given** Quản lý đúng chi nhánh chọn trainer role `TRAINER`, phòng hợp lệ, sức chứa không vượt phòng và giờ kết thúc sau giờ bắt đầu.
  - **When** Quản lý tạo lịch thứ Ba.
  - **Then** Hệ thống lưu `day_of_week = 2`, `status = ACTIVE`, `valid_from` cùng các trường đã xác nhận và trả `201 Created`.
- **Kịch bản 2: Sức chứa vượt phòng**
  - **Given** Phòng có sức chứa 20 nhưng request đặt capacity 21.
  - **When** Quản lý lưu lịch.
  - **Then** Hệ thống trả `422 Unprocessable Content`, không tạo schedule.
- **Kịch bản 3: Huấn luyện viên không có role TRAINER**
  - **Given** `trainer_id` hợp lệ nhưng role nhân viên không phải `TRAINER`.
  - **When** Quản lý lưu lịch.
  - **Then** Hệ thống từ chối `422` và không lưu lịch.
- **Kịch bản 4: Quản lý chi nhánh khác**
  - **Given** Phòng/trainer thuộc chi nhánh khác với người quản lý.
  - **When** Quản lý gửi request tạo/cập nhật lịch.
  - **Then** Hệ thống trả `403 Forbidden`; dữ liệu không đổi.
- **Kịch bản 5: Ngừng lịch**
  - **Given** Schedule đang `ACTIVE`.
  - **When** Quản lý ngừng lịch.
  - **Then** Schedule chuyển `INACTIVE`, dữ liệu schedule không bị xóa vật lý.
- **Kịch bản 6: Hủy buổi đã có booking và waitlist**
  - **Given** Session chưa diễn ra có booking `BOOKED`/`PENDING_RECONFIRM` và các dòng `WAITLIST`.
  - **When** Quản lý chi nhánh hủy buổi.
  - **Then** Session thành `CANCELLED`; các booking đang hoạt động và waitlist thành `CANCELLED` với `cancel_reason = SESSION_CANCELLED`, `is_late_cancel = false`; không áp penalty; F10 thông báo cho mọi người bị ảnh hưởng.
- **Kịch bản 7: Trainer đã có buổi PT trùng giờ**
  - **Given** Trainer được xếp một buổi PT trùng giờ với session lớp nhóm cụ thể.
  - **When** Quản lý lưu phân công tạo ra trùng lịch.
  - **Then** Hệ thống từ chối tạo/xếp lịch; không để trainer có buổi PT và lớp nhóm chồng giờ.

---

### 3.9. UC-F8.8: Sinh buổi học từ lịch lặp

- **Mã UC:** UC-F8.8
- **Tên Use Case:** Sinh buổi học từ lịch lặp
- **Tác nhân chính:** Hệ thống
- **Tác nhân phụ (nếu có):** UC-F8.7
- **Mô tả ngắn gọn:** Tạo các dòng `class_sessions` tương ứng với ngày xảy ra của `class_schedules` lặp hằng tuần để hội viên có thể xem và đặt chỗ.

#### A. Tiền điều kiện (Preconditions)

- Có `class_schedules` trạng thái `ACTIVE`; ngày occurrence cần sinh nằm trong khoảng hiệu lực `valid_from` đến `valid_to` (nếu có). Điều kiện này áp dụng cho **ngày occurrence**, không buộc thời điểm chạy job phải sau `valid_from`; horizon sinh trước chưa được chốt.
- Loại lớp, phòng và huấn luyện viên của lịch vẫn hợp lệ.

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Mỗi lần occurrence được xử lý có tối đa một dòng `class_sessions` theo unique `(class_schedule_id, occurrence_date)`. Session chép `branch_id`, `trainer_id`, `room_id`, `capacity`; ghi `occurrence_date` theo ngày Việt Nam và `start_at/end_at` là thời điểm tương ứng.
- **Thất bại:** Không tạo session trùng; lỗi một occurrence không được tạo bản ghi sai hoặc sai lịch.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Tác vụ nền được kích hoạt theo lịch chạy của hệ thống.
2. Hệ thống tìm `class_schedules` `ACTIVE` có ngày hiệu lực bao gồm ngày cần sinh.
3. Hệ thống xác định các ngày xảy ra theo `day_of_week` ISO trong khoảng hiệu lực.
4. Hệ thống phối hợp ngày + giờ địa phương chi nhánh thành `start_at`/`end_at`; `occurrence_date` lưu ngày theo giờ Việt Nam và không thay đổi khi session được đổi giờ.
5. Hệ thống chép các giá trị cần snapshot từ schedule sang session: `class_schedule_id`, `branch_id`, `trainer_id`, `room_id`, `capacity`; đặt trạng thái `SCHEDULED`.
6. Hệ thống insert session; unique `(class_schedule_id, occurrence_date)` ngăn sinh trùng khi job chạy lại.
7. Hệ thống hoàn tất các occurrence và ghi kết quả job.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **A1 – Session đã tồn tại:** Khi unique key đã có occurrence, không tạo bản sao; job có thể tiếp tục với occurrence khác.
- **A2 – Schedule hết hạn/INACTIVE:** Bỏ qua lịch và không sinh thêm session.
- **E1 – Schedule tham chiếu dữ liệu không còn hợp lệ:** Không tạo session; ghi lỗi để owner vận hành xử lý. Quy tắc sửa session/bookings hiện có không được tự suy rộng.
- **E2 – Xung đột đồng thời giữa hai job (409/unique conflict nội bộ):** Unique constraint bảo vệ khỏi duplicate; xử lý idempotent và tiếp tục các occurrence khác.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F8.8-01:** F8 yêu cầu lịch lớp lặp hằng tuần; bảng `class_sessions` lưu từng buổi cụ thể.
- **BR-F8.8-02:** Duy nhất `(class_schedule_id, occurrence_date)`; ngày occurrence là ngày theo lịch lặp tại lúc sinh, không đổi khi đổi giờ (`thiet-ke-du-lieu.md`, DBML).
- **BR-F8.8-03:** Giá trị phòng, trainer và capacity được chép sang `class_sessions`; DBML cho phép thay trainer của từng session khi có người dạy thay.
- **Câu hỏi cần chốt:** Nguồn chuẩn chưa xác định job chạy lúc nào, horizon sinh trước bao nhiêu ngày, có sinh ngay đúng ngày `valid_from` hay không, xử lý buổi đã sinh khi schedule sửa/ngừng, ngày lễ và các buổi bị hủy. `BOOKING_WINDOW_DAYS=7` là giới hạn đặt chỗ, không được tự coi là horizon sinh session.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_sessions`.
- **Bảng đọc:** `class_schedules`, `class_types`, `branches`, `rooms`, `staff`.

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `class_schedules` | `day_of_week`, `start_time`, `end_time`, `valid_from`, `valid_to`, `status` | int, time, date, varchar | `status` CHECK (`ACTIVE`, `INACTIVE`); constraints khác theo ERD | Nguồn của occurrence hằng tuần |
| `class_sessions` | `class_schedule_id`, `occurrence_date` | `bigint`, `date` | NOT NULL; UNIQUE kết hợp | Khóa idempotency cho việc sinh buổi |
| `class_sessions` | `branch_id`, `trainer_id`, `room_id`, `capacity` | bigint, int | NOT NULL, FK | Snapshot trường từ schedule |
| `class_sessions` | `start_at`, `end_at`, `original_start_at` | `timestamptz` | start/end NOT NULL | Thời điểm cụ thể, giờ cũ khi reschedule |
| `class_sessions` | `status` | `varchar` | NOT NULL, CHECK (`SCHEDULED`, `RESCHEDULED`, `CANCELLED`, `COMPLETED`), default `SCHEDULED` | Trạng thái buổi |

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint:** Không có endpoint người dùng trong đặc tả này; tác vụ hệ thống nội bộ.
- **Header / Request Body:** Không áp dụng.
- **Response Success:** Không áp dụng; job ghi nhận kết quả nội bộ.
- **Màn hình giao diện liên quan:** Không có màn hình riêng; session được hiển thị ở lịch công khai/hội viên/trainer.
- Lịch chạy, giới hạn horizon và cơ chế retry phải được owner backend xác nhận; không tạo API contract từ UC này.

#### H. Tiêu chí chấp nhận (Acceptance Criteria)

- **Kịch bản 1: Tạo buổi vào thứ phù hợp**
  - **Given** Schedule `ACTIVE`, `day_of_week = 2`, ngày occurrence thứ Ba nằm trong khoảng hiệu lực.
  - **When** Job sinh session trong khoảng occurrence đã được owner chọn.
  - **Then** Hệ thống tạo đúng một `class_sessions` với `occurrence_date` là ngày thứ Ba theo giờ Việt Nam và snapshot đúng trainer/phòng/capacity.
- **Kịch bản 2: Job chạy lặp không tạo trùng**
  - **Given** Session của `(class_schedule_id, occurrence_date)` đã tồn tại.
  - **When** Job chạy lại cùng occurrence.
  - **Then** Không có session thứ hai; unique constraint giữ tính idempotent.
- **Kịch bản 3: Lịch inactive hoặc ngoài hiệu lực**
  - **Given** Schedule `INACTIVE`, hoặc ngày occurrence đang xét nằm trước `valid_from` hay sau `valid_to` nếu có.
  - **When** Job xét schedule.
  - **Then** Không sinh occurrence ngoài khoảng hiệu lực.
- **Kịch bản 4: Đổi giờ session đã sinh**
  - **Given** Session được đổi giờ sau khi sinh.
  - **When** Job chạy lại cho cùng occurrence.
  - **Then** Không tạo session ở giờ cũ hoặc bản sao; `occurrence_date` giữ nguyên.

---

## 4. Quy tắc nghiệp vụ áp dụng chung

1. **Eligibility:** Chỉ gói TIME `ACTIVE` dùng được tại đúng chi nhánh cho phép đặt hoặc vào waitlist; PT và SESSION không thay thế điều kiện này.
2. **Capacity:** Số booking giữ chỗ gồm `BOOKED`, `PENDING_RECONFIRM`, `ATTENDED`, `NO_SHOW`; loại `WAITLIST`, `CANCELLED`. Không vượt `class_sessions.capacity`.
3. **Trùng giờ:** Một hội viên không được có hai booking đang hoạt động cho các buổi có thời gian giao nhau; check đồng thời phải khóa bản ghi.
4. **FIFO:** `waitlisted_at` tăng dần; không lưu vị trí queue cố định và không cho người thao tác hộ vượt hàng.
5. **Promotion:** Tự động, không cần xác nhận; bỏ qua người không đủ điều kiện và xét kế tiếp; cutoff strictly “dưới 30 phút”; chỗ sau cutoff để trống.
6. **Cancellation:** Hủy trước giờ ít nhất 2 giờ không phạt; grace 15 phút chỉ áp dụng cho booking vừa được auto-promotion, không áp dụng chung cho mọi booking; đủ 3 late-cancel/no-show trong tháng khóa đặt 7 ngày.
7. **Reschedule:** Đổi phòng chỉ báo; đổi giờ buộc booking đang có chỗ reconfirm trong 60 phút; từ chối/hết hạn mất chỗ không phạt, sau đó xét waitlist.
8. **Attendance:** F4 chỉ ghi `arrived_at`; trainer chịu trách nhiệm cập nhật `ATTENDED`/`NO_SHOW`.
9. **Lịch lặp:** ISO weekday 1–7, `end_time > start_time`, `0 < capacity <= room.capacity`, trainer phải có role `TRAINER`; từng buổi cụ thể được lưu ở `class_sessions`.
10. **Thời gian:** timestamp trong DB là UTC (`timestamptz`); `occurrence_date` là ngày theo lịch Việt Nam. Trình bày giờ cho người dùng theo `Asia/Ho_Chi_Minh`.
11. **Cấu hình:** Đọc `system_configs`; giá trị chi nhánh ghi đè giá trị toàn chuỗi. Không hardcode giá trị có dấu `*`.

### 4.1. Tham số cấu hình F8

| Key | Giá trị mặc định | Nơi quy định | Ý nghĩa |
| --- | ---: | --- | --- |
| `BOOKING_WINDOW_DAYS` | 7 | ERD DBML, `system_configs.config_key` | Đặt chỗ trước bao nhiêu ngày |
| `PROMOTION_CUTOFF_MIN` | 30 | Scope, ERD, ADR 0003 | Không auto-promote khi còn dưới ngưỡng này |
| `PROMOTION_GRACE_MIN` | 15 | Scope, ERD, ADR 0003 | Ân hạn hủy không phạt sau khi promotion |
| `RECONFIRM_TTL_MIN` | 60 | Scope, ERD DBML | Thời hạn xác nhận lại khi đổi giờ |
| `LATE_CANCEL_HOURS` | 2 | Scope, ERD DBML | Hủy trước giờ ít nhất ngưỡng này thì không phạt |
| `PENALTY_LIMIT_PER_MONTH` | 3 | Scope, ERD DBML | Số late-cancel/no-show trong tháng trước khi khóa |
| `PENALTY_LOCK_DAYS` | 7 | Scope, ERD DBML | Thời lượng khóa đặt lớp |

`system_configs.config_value` lưu dạng chuỗi; code cần parse/validate. ERD cho phép một cấu hình toàn chuỗi (`branch_id IS NULL`) và giá trị theo chi nhánh ghi đè. Quy tắc xử lý cấu hình thiếu/sai định dạng hoặc hai giá trị cùng hiệu lực chưa được quy định trong SRS; cần thống nhất với owner cấu hình.

---

## 5. Bảng dữ liệu liên quan

| Bảng / Đối tượng | Nhóm | Vai trò trong F8 |
| --- | --- | --- |
| `class_types` | Nhóm 4 | Danh mục loại lớp |
| `class_schedules` | Nhóm 4 | Lịch lặp hằng tuần |
| `class_sessions` | Nhóm 4 | Từng buổi cụ thể được sinh từ lịch lặp |
| `class_bookings` | Nhóm 4 | Chỗ đặt, waitlist, reconfirm và attendance |
| `members` | Nhóm 3 | Hồ sơ hội viên, `booking_locked_until`, `home_branch_id` |
| `memberships` | Nhóm 3 | Xác nhận gói đang có hiệu lực |
| `packages` | Nhóm 3 | Loại và phạm vi sử dụng của gói |
| `branches` | Nhóm 2 | Giờ mở cửa và phạm vi chi nhánh |
| `rooms` | Nhóm 2 | Phòng và sức chứa tối đa |
| `staff` | Nhóm 7 | Huấn luyện viên, chi nhánh và phân công |
| `users` | Nhóm 1 | Actor thao tác, trainer/RECEPTIONIST role |
| `system_configs` | Nhóm 1 | Các tham số có dấu `*` |
| `notifications` | Nhóm 1 | F10 thông báo trong web/email, lịch gửi và trạng thái gửi |

---

## 6. Tích hợp với chức năng liên quan

- **F3 – Gói tập:** F8 chỉ đọc trạng thái và loại gói; không kích hoạt hay gia hạn gói.
- **F4 – Check-in tại quầy:** F4 có thể ghi `class_bookings.arrived_at` cho lớp sắp diễn ra. Không suy diễn `arrived_at` thành `ATTENDED`.
- **F9 – Huấn luyện viên:** Trainer xem lịch lớp nhóm cùng lịch PT theo sơ đồ F8/F9; không cho trùng lịch trainer với lớp nhóm theo quy tắc không trùng của scope/NF6.
- **F10 – Thông báo:** F8 tạo yêu cầu thông báo cho promotion, đổi giờ, hủy buổi; F10 phụ trách delivery/retry và giờ gửi email. Email promotion phát sinh ban đêm được gửi khi tới giờ mở cửa chi nhánh; grace bắt đầu lúc gửi email.
- **F18 – Đa chi nhánh:** V2 vận hành một chi nhánh mặc định, nhưng bảng F8 đã chứa `branch_id`; logic truy vấn/authorization phải theo chi nhánh được xác thực. Không cho caller đổi `branchId` để vượt quyền.

---

## 7. Các câu hỏi cần chốt trước khi triển khai

Các điểm dưới đây chưa được chốt trong bản phạm vi, ERD hoặc ADR; không phải quy tắc mới:

1. Cửa sổ `BOOKING_WINDOW_DAYS=7` tính từ hiện tại theo 168 giờ hay tính theo ngày lịch; biên chính xác có bao gồm thời điểm/ngày thứ bảy không.
2. FIFO nếu hai `waitlisted_at` bằng nhau: cần tie-break ổn định (ví dụ ID) hay quy tắc khác.
3. Kỳ “trong tháng” của penalty là tháng dương lịch theo giờ Việt Nam hay khoảng trượt; hành vi khi đạt ngưỡng có cần reset gì ngoài `booking_locked_until` không.
4. Grace đến đúng `grace_until` có còn không phạt; cách xử lý nếu email không gửi được hoặc hội viên không có tài khoản/email.
5. Hủy booking `WAITLIST` có tính là hủy muộn hay không; nguồn hiện không nói.
6. Nếu `RECONFIRM_TTL_MIN` kết thúc sau giờ bắt đầu lớp thì giữ hay hủy booking tại thời điểm nào.
7. Khi đổi giờ, cần kiểm tra giờ mới gây trùng với booking khác của hội viên không; xử lý booking đang `WAITLIST` ra sao.
8. Thời điểm chạy/horizon của UC-F8.8; tác động của sửa/ngừng lịch lên session đã sinh/booking hiện có; ngày lễ và ngoại lệ lịch.
9. Xung đột phòng/trainer giữa hai lịch lặp; giảm capacity khi session đã có booking; kết thúc buổi/lịch điểm danh được mở trong khoảng thời gian nào.
10. Phân quyền cụ thể cho quản lý sửa session riêng lẻ (đổi phòng/giờ/hủy buổi) và mapping `cancel_reason` cho từ chối/hết hạn reconfirm.
11. Cách ghi `class_bookings.created_by` khi hội viên tự đặt; ERD chỉ ghi chú lễ tân khi thao tác hộ.
12. Nội dung lịch public (có hiển thị số chỗ trống không), endpoint/DTO F8, bulk attendance và HTTP response chính xác.

---

## 8. Truy vết nguồn

| Nội dung | Nguồn |
| --- | --- |
| Phạm vi chức năng, actor, điều kiện gói, booking window, cancellation, reschedule, penalties, notifications | [Bản chốt phạm vi](../ban-chot-pham-vi.md), mục F8 |
| FIFO, auto-promotion, cutoff, grace, vacancy ban đêm, notification, khóa chỗ | [ADR 0003](../../decisions/0003-danh-sach-cho-tu-dong-dua-vao-lop.md) |
| Tập giá trị Enum khái niệm, unique constraints, capacity states, khóa NF6, config keys | [ERD v2](../../design/erd/erd-v2.dbml), [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md) |
| Mã Use Case, A/E flows, HTTP codes, Given/When/Then, cấu trúc A–H | [Mẫu đặc tả](mau-dac-ta-use-case.md) |
| Public schedule, member booking/cancel/waitlist, trainer view/attendance, manager recurring schedule | Sơ đồ draw.io hiện có trong `docs/design/use-case/` |

### 8.1. Truy vết user story đã xuất bản

Các issue dưới đây là yêu cầu, không phải bằng chứng đã triển khai. Không phụ thuộc file chưa merge của PR backlog #77.

| Story issue | Nội dung | Use case |
| --- | --- | --- |
| [#64](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/64) | Lịch tuần và sinh buổi | UC-F8.7, UC-F8.8 |
| [#65](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/65) | Đặt chỗ/vào chờ | UC-F8.1 |
| [#66](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/66) | Hủy chỗ/penalty | UC-F8.3, UC-F8.6 |
| [#67](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/67) | FIFO promotion | UC-F8.2 |
| [#68](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/68) | Đổi phòng/giờ, hủy buổi | UC-F8.4, UC-F8.7 |
| [#69](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/69) | Lịch dạy và điểm danh | UC-F8.5, UC-F8.6 |
| [#70](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/70) | Lịch công khai | UC-F8.0 |
