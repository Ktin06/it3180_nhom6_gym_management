# Đặc tả Use Case – F8: Lớp học nhóm

- **Mã chức năng:** F8
- **Version:** v2 (Bắt buộc)
- **Người biên soạn:** Phạm Gia Bảo
- **Cập nhật:** 04/11/2026 — chỉnh sửa theo review của trưởng nhóm; vẫn là bản nháp.
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

> **Trạng thái:** Đặc tả này diễn giải yêu cầu đã duyệt trong bản chốt phạm vi, ERD và ADR 0003. Các API, DTO, cách phân loại lỗi cụ thể trong mục G là **đề xuất thiết kế để người triển khai backend F8 trong Sprint 2 xác nhận**, chưa phải hợp đồng API đã thống nhất. Những điểm chưa được nguồn chuẩn quyết định được ghi rõ trong mục “Các câu hỏi cần chốt”, không tự đặt thêm quy tắc.

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

> **Truy vết sơ đồ (cần Minh Quân cập nhật, không sửa sơ đồ trong task này):** Trong [`use-case-hoi-vien.drawio.svg`](../../design/use-case/use-case-hoi-vien.drawio.svg), nhãn UC-F8.2 hiện là “Tự động vào danh sách chờ khi lớp đầy”, nhưng UC-F8.2 trong SRS mô tả bước tự động đưa người đủ điều kiện từ `WAITLIST` vào lớp; hãy cập nhật nhãn để khớp. Nhãn UC-F8.3 “Hủy chỗ đặt lớp (Ân hạn 15 phút)” cũng mơ hồ: ân hạn 15 phút chỉ áp dụng cho chỗ đặt vừa được tự động đưa vào lớp, không áp dụng cho mọi lần hủy.

### 2.1. Cách biểu diễn trạng thái trong CSDL

Các khối `Enum` trong DBML mô tả tập giá trị logic để đặc tả và đối chiếu. CSDL vật lý lưu các cột trạng thái/loại/lý do bằng `varchar` kèm `CHECK`, theo [quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md); mục F chỉ liệt kê bảng/cột được sử dụng. Kiểu, khóa, chỉ mục và ràng buộc xem trực tiếp tại [ERD v2](../../design/erd/erd-v2.dbml); không sao chép định nghĩa ở từng UC.

---

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F8.0: Xem lịch lớp học nhóm công khai

- **Mã UC:** UC-F8.0
- **Tên Use Case:** Xem lịch lớp học nhóm công khai
- **Tác nhân chính:** Khách
- **Tác nhân phụ (nếu có):** Không
- **Mô tả ngắn gọn:** Khách xem các buổi lớp nhóm được công bố để tìm hiểu lịch tập trước khi đăng ký tài khoản hoặc đến chi nhánh.

#### A. Tiền điều kiện

- Không cần đăng nhập.
- Chi nhánh và các buổi lớp được yêu cầu tồn tại.

#### B. Hậu điều kiện

- **Thành công:** Không thay đổi dữ liệu. Trả lịch lớp nhóm công khai theo bộ lọc đã yêu cầu.
- **Thất bại:** Không thay đổi dữ liệu; trả lỗi định dạng hoặc không tìm thấy chi nhánh theo quy ước HTTP tại mục D.

#### C. Luồng sự kiện chính

1. Khách mở trang lịch lớp nhóm công khai.
2. Khách chọn chi nhánh và khoảng ngày muốn xem (nếu giao diện cung cấp bộ lọc).
3. Hệ thống kiểm tra cú pháp bộ lọc và chi nhánh.
4. Hệ thống đọc các buổi lớp nhóm thuộc chi nhánh trong khoảng được yêu cầu.
5. Hệ thống trả thông tin lịch công khai: loại lớp, thời gian, phòng và huấn luyện viên nếu các thông tin đó được công bố trên giao diện.
6. Giao diện hiển thị lịch; khách có thể tiếp tục đến luồng đăng nhập/đăng ký của F1.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Không có buổi lớp trong khoảng đã chọn:** Tại bước 4, nếu không có buổi phù hợp, hệ thống trả danh sách rỗng; giao diện hiển thị trạng thái “Không có lớp trong khoảng thời gian này”.
- **E1 – Bộ lọc sai định dạng (400 Bad Request):** Tại bước 3, hệ thống trả `ProblemDetail` tiếng Việt, nêu trường ngày/chi nhánh không hợp lệ.
- **E2 – Không tìm thấy chi nhánh (404 Not Found):** Nếu mã chi nhánh không tồn tại, hệ thống trả `ProblemDetail` (404).

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.0-01:** Khách được xem lịch lớp công khai (F8; use case `UC-F8.0` trên sơ đồ `use-case-khach.drawio.svg`).
- **BR-F8.0-02:** Phạm vi dữ liệu chỉ là thông tin lịch công khai; không trả thông tin cá nhân hoặc dữ liệu hội viên.
- **Câu hỏi nghiệp vụ (trưởng nhóm Hoàng Kim Tiến chốt, xem mục 7):** Nguồn chuẩn không nêu mặc định thời gian hiển thị, có hiển thị số chỗ còn lại hay không, và bộ lọc/chi nhánh mặc định khi F18 chưa có. Không tự áp dụng `BOOKING_WINDOW_DAYS` cho việc xem lịch; tham số này quy định cửa sổ **đặt chỗ**.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** Không có; chỉ đọc.
- **Bảng truy vấn:** `class_sessions`, `class_types`, `class_schedules`, `branches`, `rooms`, `staff`.

- `class_sessions`: `branch_id`, `start_at`, `end_at`, `status`, `room_id`, `trainer_id`, `class_schedule_id`, `occurrence_date`.
- `class_types`: `name`, `description`, `status`.
- `rooms`: `name`.
- `staff`: `id`.

Định nghĩa cột và ràng buộc: xem [ERD v2](../../design/erd/erd-v2.dbml) (mục 2.1).

#### G. Thiết kế giao diện và API liên quan

- **API (đề xuất, người triển khai backend F8 trong Sprint 2 xác nhận):** `GET /api/v1/public/class-sessions?branchId={id}&from={date}&to={date}`
- **Tiêu đề HTTP:** Không yêu cầu `Authorization`.
- **Nội dung yêu cầu (DTO):** Không có; tham số truy vấn đề xuất: `branchId`, `from`, `to`.
- **Phản hồi thành công (200 OK) – Hình dạng đề xuất:**
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
- Các trường công khai cụ thể và việc hiển thị sức chứa/chỗ trống do trưởng nhóm Hoàng Kim Tiến chốt nghiệp vụ; người triển khai backend F8 trong Sprint 2 chốt cách biểu diễn API trước khi coi là hợp đồng API.

#### H. Tiêu chí chấp nhận

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

**Giới hạn luồng đã đặc tả:** Nhánh còn chỗ → `BOOKED`, đầy → `WAITLIST` dưới đây áp dụng khi còn ít nhất `PROMOTION_CUTOFF_MIN` phút trước giờ học và không có người từng bị bỏ qua trong danh sách chờ nay đủ điều kiện lại. Hai ngoại lệ này chưa có kết quả chấp nhận/từ chối được duyệt, chờ trưởng nhóm Hoàng Kim Tiến chốt ở câu 13–14 mục 7; chưa đủ cơ sở triển khai hoặc kiểm thử kết quả của chúng.

#### A. Tiền điều kiện

- Hội viên đăng nhập hợp lệ; hoặc lễ tân đăng nhập và chọn hồ sơ hội viên để thao tác hộ.
- Buổi học tồn tại, chưa bị hủy/kết thúc và nằm trong cửa sổ đặt chỗ.
- Hội viên có gói `TIME` đang `ACTIVE`, dùng được tại chi nhánh của buổi học.
- Hội viên không bị khóa đặt lớp tại thời điểm yêu cầu.

#### B. Hậu điều kiện

- **Thành công:** Một dòng `class_bookings` được tạo cho hội viên/buổi. Nếu còn sức chứa, trạng thái là `BOOKED`; nếu đã đủ sĩ số, trạng thái là `WAITLIST` và `waitlisted_at` được ghi để xác định thứ tự. `created_by` ghi theo quy ước chờ trưởng nhóm Hoàng Kim Tiến chốt ở câu 11 mục 7.
- **Thất bại:** Không tạo chỗ đặt và không thay đổi sĩ số/danh sách chờ. Giao dịch bị hủy nếu có lỗi hoặc xung đột đồng thời.

#### C. Luồng sự kiện chính

1. Hội viên chọn một buổi lớp và bấm đặt chỗ; hoặc lễ tân chọn buổi và hồ sơ hội viên cần thao tác hộ.
2. Hệ thống kiểm tra hợp lệ tham số nhận dạng buổi/hội viên.
3. Hệ thống xác thực vai trò và kiểm tra hội viên chỉ đặt cho chính mình; lễ tân chỉ thao tác hộ trong phạm vi được phân quyền (S3).
4. Hệ thống bắt đầu giao dịch, khóa dòng `members` của hội viên đang yêu cầu trước, rồi khóa dòng `class_sessions` theo NF6; kiểm tra lại điều kiện sau khi lấy khóa.
5. Hệ thống xác nhận buổi có thể đặt và thời điểm yêu cầu nằm trong cửa sổ `BOOKING_WINDOW_DAYS`.
6. Hệ thống xác nhận hội viên có gói `TIME` `ACTIVE` dùng được tại chi nhánh của lớp, không bị khóa đặt lớp, chưa có chỗ đặt đang hoạt động cho cùng buổi, và không vi phạm quy tắc không đăng ký các buổi trùng giờ theo chính sách đã được trưởng nhóm Hoàng Kim Tiến chốt.
7. Hệ thống đếm các chỗ đang giữ: `BOOKED`, `PENDING_RECONFIRM`, `ATTENDED`, `NO_SHOW`.
8. Hệ thống chọn nhánh còn chỗ hoặc đã đầy theo số chỗ giữ ở bước 7. Việc xét danh sách chờ khi chỗ được giải phóng thuộc UC-F8.2, không lặp lại ở đây. Hai ngoại lệ nêu trên vẫn chờ quyết định tại mục 7.
9. Nếu còn chỗ trong phạm vi luồng đã đặc tả, hệ thống tạo `class_bookings.status = 'BOOKED'` và ghi `booked_at`.
10. Nếu lớp đã đầy, hệ thống tạo `class_bookings.status = 'WAITLIST'`, ghi `waitlisted_at`; không ghi vị trí thứ tự cố định.
11. Hệ thống ghi nhận giao dịch và trả kết quả. Khi lớp đầy, hội viên được thông báo mình đang ở danh sách chờ.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Lớp còn chỗ, không có người đang chờ:** Tại bước 9, hệ thống tạo `BOOKED`.
- **A2 – Lớp đầy:** Tại bước 10, hệ thống tạo `WAITLIST`; đây là kết quả thành công, không phải lỗi. Vị trí chờ được tính khi truy vấn theo `waitlisted_at` tăng dần.
- **E1 – Yêu cầu sai định dạng (400 Bad Request):** ID hoặc dữ liệu yêu cầu sai định dạng; trả `ProblemDetail` có `errors` theo trường.
- **E2 – Không có quyền (403 Forbidden):** Tài khoản không có vai trò hợp lệ, hội viên yêu cầu đặt cho người khác, hoặc nhân viên truy cập ngoài phạm vi chi nhánh được phân quyền.
- **E3 – Không tìm thấy buổi/hội viên (404 Not Found):** Tài nguyên không tồn tại.
- **E4 – Đã có chỗ đặt đang hoạt động (409 Conflict):** Hội viên đã có dòng cho cùng buổi với trạng thái khác `CANCELLED`; trả thông báo “Hội viên đã có chỗ đặt hoặc đang trong danh sách chờ của buổi này.”
- **E5 – Xung đột đồng thời (409 Conflict):** Khóa/ràng buộc duy nhất phát hiện cạnh tranh; hủy giao dịch và yêu cầu tải lại hoặc thử lại. Không để vượt sĩ số.
- **E6 – Vi phạm điều kiện đặt (422 Unprocessable Content):** Không có gói `TIME` phù hợp, gói không `ACTIVE`/không dùng tại chi nhánh, bị khóa đặt lớp, ngoài cửa sổ đặt chỗ, hoặc buổi không nhận đặt; nêu lý do cụ thể bằng tiếng Việt.
- **E7 – Trùng giờ với đăng ký đang chiếm chỗ (409 Conflict):** Hội viên đã có chỗ ở buổi khác có thời gian giao nhau; trả `ProblemDetail` nêu lớp bị xung đột. Cách xử lý trường hợp buổi kia chỉ ở `WAITLIST` cần trưởng nhóm Hoàng Kim Tiến chốt riêng.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.1-01:** Chỉ gói `TIME` `ACTIVE` dùng được tại chi nhánh; gói `SESSION` và `PT` không dùng đặt lớp. Bản chốt phạm vi, mục F8; ERD `class_bookings.status`.
- **BR-F8.1-02:** Cửa sổ đặt trước là `BOOKING_WINDOW_DAYS` (mặc định 7 ngày). Cách tính chính xác giữa ngày lịch và 168 giờ chưa được chốt; không viết cứng.
- **BR-F8.1-03:** Số chỗ giữ không vượt `class_sessions.capacity`; các trạng thái giữ chỗ là `BOOKED`, `PENDING_RECONFIRM`, `ATTENDED`, `NO_SHOW`; `WAITLIST` và `CANCELLED` không chiếm chỗ.
- **BR-F8.1-04:** Bản chốt phạm vi cấm hội viên đăng ký hai buổi trùng giờ; một chỗ đặt đã `CANCELLED` không còn ngăn đặt lại. Người thao tác hộ không được nhảy hàng hoặc đặt ngoài thứ tự danh sách chờ.
- **BR-F8.1-05:** Không giới hạn số danh sách chờ một hội viên tham gia.
- **BR-F8.1-06:** Khi một thao tác giải phóng chỗ, UC-F8.2 xét người đủ điều kiện theo FIFO trong cùng giao dịch giải phóng chỗ. Không cho lễ tân chỉ định người phía sau để vượt hàng. Thứ tự giữa người từng bị bỏ qua nay đủ điều kiện lại và người đặt mới chờ trưởng nhóm chốt (câu 13 mục 7).
- **NF6:** Thực hiện trong giao dịch; khóa dòng `members` trước `class_sessions`; kiểm tra trùng giờ và sĩ số sau khi có khóa. Ràng buộc duy nhất đang hoạt động: `(class_session_id, member_id)` với `status <> 'CANCELLED'`.
- **Câu hỏi nghiệp vụ (trưởng nhóm Hoàng Kim Tiến chốt, xem mục 7):** Khi `waitlisted_at` bằng nhau, chưa có tiêu chí phụ để phân định thứ tự. Chưa xác định cửa sổ 7 ngày là 7 ngày lịch hay 168 giờ; thời điểm đúng biên cần được thống nhất. Bản chốt phạm vi cấm hai buổi trùng giờ nhưng cũng nói không giới hạn số danh sách chờ; cần xác nhận một `WAITLIST` có xung đột với một chỗ đang giữ, hoặc với một `WAITLIST` khác, theo cùng cách nào hay không. Thiết kế khóa giao dịch của UC-F8.2 do người triển khai backend F8 trong Sprint 2 xác nhận, giữ thứ tự NF6 và kiểm tra lại điều kiện sau khi lấy khóa. Hai ngoại lệ đặt mới ở câu 13–14 mục 7 do trưởng nhóm Hoàng Kim Tiến quyết định.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_bookings`.
- **Bảng kiểm tra:** `class_sessions`, `class_schedules`, `class_types`, `members`, `memberships`, `packages`, `rooms`, `system_configs`, `staff`, `users`.

- `class_bookings`: `class_session_id`, `member_id`, `created_by`, `status`, `waitlisted_at`, `booked_at`.
- `class_sessions`: `capacity`, `start_at`, `end_at`, `branch_id`, `status`.
- `members`: `booking_locked_until`, `status`.
- `memberships`: `package_type`, `branch_id`, `status`, `end_date`.
- `system_configs`: `config_key`, `config_value`, `branch_id`.

Định nghĩa cột và ràng buộc: xem [ERD v2](../../design/erd/erd-v2.dbml) (mục 2.1).

#### G. Thiết kế giao diện và API liên quan

- **API (đề xuất, người triển khai backend F8 trong Sprint 2 xác nhận):** `POST /api/v1/class-sessions/{sessionId}/bookings`
- **Tiêu đề HTTP:** `Authorization: Bearer <access_token>`.
- **Nội dung yêu cầu (DTO đề xuất):**
  ```json
  {
    "memberId": 42
  }
  ```
  `memberId` chỉ dùng trong luồng lễ tân thao tác hộ; với hội viên tự đặt, danh tính phải lấy từ phiên đăng nhập. Cách biểu diễn hai luồng trong DTO do người triển khai backend F8 trong Sprint 2 xác nhận.
- **Phản hồi thành công (201 Created) – Hình dạng đề xuất:**
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
- **Mã lỗi đề xuất:** `400`, `401`, `403`, `404`, `409`, `422` theo mẫu; lớp đầy trả `201` với `WAITLIST`, không trả lỗi.

#### H. Tiêu chí chấp nhận

- **Kịch bản 1: Đặt chỗ còn sức chứa**
  - **Given** Hội viên có gói `TIME` `ACTIVE` dùng tại chi nhánh, chưa bị khóa; buổi còn chỗ, còn ít nhất `PROMOTION_CUTOFF_MIN` phút trước giờ học và không có người chờ từng bị bỏ qua nay đủ điều kiện lại.
  - **When** Hội viên đặt chỗ.
  - **Then** Hệ thống tạo một chỗ đặt `BOOKED`, ghi `booked_at`, trả `201 Created` và sĩ số không vượt sức chứa.
- **Kịch bản 2: Lớp đầy thì vào FIFO danh sách chờ**
  - **Given** Buổi đã đủ chỗ và một hội viên đủ điều kiện chưa có chỗ đặt trùng giờ.
  - **When** Hội viên đặt chỗ.
  - **Then** Hệ thống tạo `WAITLIST`, ghi `waitlisted_at`, trả thành công và không tăng số chỗ giữ.
- **Kịch bản 3: Hội viên không có gói `TIME` phù hợp**
  - **Given** Hội viên chỉ có gói `SESSION` hoặc `PT`, hoặc không có gói `TIME` `ACTIVE` dùng tại chi nhánh.
  - **When** Hội viên đặt chỗ.
  - **Then** Hệ thống trả `422 Unprocessable Content`, không tạo chỗ đặt.
- **Kịch bản 4: Hai yêu cầu đồng thời tranh chỗ cuối**
  - **Given** Còn đúng một chỗ trong phạm vi luồng đã đặc tả và hai hội viên hợp lệ gửi yêu cầu đồng thời.
  - **When** Cả hai yêu cầu được xử lý.
  - **Then** Tối đa một chỗ đặt ở trạng thái chiếm chỗ; yêu cầu còn lại được xếp `WAITLIST`; sức chứa không bị vượt.
- **Kịch bản 5: Đặt buổi trùng giờ với chỗ đang có**
  - **Given** Hội viên đã có chỗ đặt `BOOKED` ở một buổi; một buổi khác có giờ giao nhau và còn chỗ.
  - **When** Hội viên đặt buổi thứ hai.
  - **Then** Hệ thống trả `409 Conflict`, không tạo chỗ đặt thứ hai.
- **Kịch bản 6: Hủy trước đó rồi đặt lại**
  - **Given** Chỗ đặt cũ của hội viên cho buổi đã `CANCELLED`.
  - **When** Hội viên đặt lại buổi đó và còn đáp ứng điều kiện.
  - **Then** Hệ thống cho tạo chỗ đặt mới; điều kiện duy nhất không chặn do chỗ đặt cũ đã hủy.
- **Kịch bản 7: Lễ tân không thể vượt thứ tự khi giải phóng chỗ**
  - **Given** Một thao tác hủy đang giải phóng chỗ, có hội viên đủ điều kiện ở đầu danh sách chờ và còn ít nhất `PROMOTION_CUTOFF_MIN` phút trước giờ học.
  - **When** UC-F8.2 xử lý chỗ vừa giải phóng và lễ tân đồng thời thao tác đặt chỗ cho hội viên khác.
  - **Then** UC-F8.2 đưa người đầu danh sách đủ điều kiện vào lớp trong giao dịch giải phóng chỗ; lễ tân không được chọn người khác thay thế. Trường hợp người từng bị bỏ qua nay đủ điều kiện lại vẫn thuộc câu 13 mục 7.
- **Kịch bản 8: Hai yêu cầu đồng thời của cùng hội viên vào hai buổi trùng giờ**
  - **Given** Cùng hội viên đồng thời gửi hai yêu cầu cho hai buổi học trùng giờ, mỗi buổi học còn chỗ.
  - **When** Hai yêu cầu được xử lý đồng thời.
  - **Then** Tối đa một yêu cầu tạo `BOOKED`; yêu cầu còn lại trả `409 Conflict`; không tạo hai chỗ đặt chiếm chỗ chồng giờ.

---

### 3.3. UC-F8.2: Tự động đưa hội viên từ danh sách chờ vào lớp

- **Mã UC:** UC-F8.2
- **Tên Use Case:** Tự động đưa hội viên từ danh sách chờ vào lớp
- **Tác nhân chính:** Hệ thống
- **Tác nhân phụ (nếu có):** F10 (thông báo trong web và email)
- **Mô tả ngắn gọn:** Khi lớp đầy, hệ thống ghi hội viên vào cuối danh sách chờ; khi có chỗ trống và còn đủ thời gian, tự động chuyển người đủ điều kiện đứng đầu FIFO từ `WAITLIST` sang chỗ đã đặt, không chờ hội viên xác nhận.

#### A. Tiền điều kiện

- Có chỗ trong một buổi học chưa bị hủy.
- Còn ít nhất `PROMOTION_CUTOFF_MIN` phút (mặc định 30) trước `class_sessions.start_at`.
- Có một hoặc nhiều chỗ đặt `WAITLIST`.

#### B. Hậu điều kiện

- **Thành công:** Các chỗ đặt đủ điều kiện theo FIFO được chuyển thành `BOOKED`; ghi `promoted_at`. `grace_until` xác định 15 phút từ thời điểm gửi email theo DBML; thông báo trong web được tạo ngay, email được gửi khi chi nhánh mở cửa nếu việc đưa vào lớp diễn ra ban đêm. Số chỗ giữ không vượt sức chứa.
- **Thất bại:** Nếu không tìm thấy ứng viên đủ điều kiện thì các chỗ đặt chờ tiếp tục giữ thứ tự; chỗ có thể còn trống. Nếu đã qua mốc ngừng đưa vào lớp thì không đưa vào lớp.

**Thiết kế giao dịch còn chờ:** Người triển khai backend F8 trong Sprint 2 phải chốt kế hoạch lấy khóa cho toàn bộ thao tác giải phóng chỗ và xét danh sách chờ ngay từ đầu giao dịch, tuân NF6. Không lấy thêm khóa `members` sau khi đã khóa `class_sessions`.

#### C. Luồng sự kiện chính

1. Khi một thao tác giải phóng chỗ, hệ thống thực hiện UC-F8.2 trong cùng giao dịch đó. Trường hợp xét lại người từng bị bỏ qua nay đủ điều kiện lại chờ quyết định câu 13 mục 7.
2. Hệ thống khóa các dòng liên quan theo thứ tự NF6: dòng `members` ứng viên trước, sau đó dòng `class_sessions`; khi xử lý nhiều ứng viên, thứ tự khóa phải nhất quán để hạn chế khóa chéo.
3. Hệ thống kiểm tra buổi chưa bị hủy/kết thúc, còn chỗ theo `capacity` và thời gian đến lớp không ở dưới `PROMOTION_CUTOFF_MIN`.
4. Sau khi khóa buổi học, hệ thống đọc lại các dòng `class_bookings.status = 'WAITLIST'` theo `waitlisted_at` tăng dần; vị trí không được lưu cứng. Chỉ xét ứng viên có dòng `members` đã khóa trước `class_sessions`. Nếu có ứng viên cần xét chưa được khóa từ đầu, phải hủy và xử lý lại toàn bộ giao dịch với tập khóa phù hợp; không lấy khóa hội viên bổ sung sau khóa buổi học. Người triển khai backend F8 trong Sprint 2 chốt cơ chế thử lại ở giao dịch mới, không thử lại trong giao dịch đã bị hủy.
5. Với từng ứng viên theo thứ tự, hệ thống kiểm tra gói `TIME` `ACTIVE` dùng tại chi nhánh, khóa đặt lớp, lịch trùng giờ và điều kiện khác đã được bản chốt xác định.
6. Nếu ứng viên không còn đủ điều kiện, hệ thống bỏ qua và xét người tiếp theo; không cho phép nhân viên chọn một người phía sau để ưu tiên.
7. Với ứng viên đủ điều kiện, hệ thống đổi trạng thái sang `BOOKED`, ghi `promoted_at`, tính ân hạn theo thời điểm email gửi, rồi tạo yêu cầu thông báo F10.
8. Hệ thống tiếp tục cho các chỗ trống còn lại đến khi đủ sức chứa, hết ứng viên đủ điều kiện hoặc chạm mốc ngừng đưa vào lớp.
9. Hệ thống ghi nhận giao dịch và F10 thông báo trong web ngay. Nếu ngoài giờ mở cửa, email được lên lịch gửi khi chi nhánh mở cửa; thời hạn ân hạn bắt đầu khi email gửi.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Người đầu hàng không còn đủ điều kiện:** Tại bước 5, giữ nguyên thứ tự tương đối của hàng chờ và xét ứng viên tiếp theo. Các điều kiện nêu rõ trong bản chốt phạm vi/ADR: gói hết hạn/không còn dùng được, đang bị khóa đặt, đã có lịch trùng giờ.
- **A2 – chỗ trống phát sinh ban đêm:** Đưa vào lớp vẫn chạy ngay; thông báo web hiện ngay; email gửi vào giờ mở cửa của chi nhánh; ân hạn tính từ lúc email được gửi.
- **A3 – Đã dưới 30 phút:** Hệ thống không đổi bất kỳ dòng `WAITLIST` nào; chỗ trống được để trống.
- **A4 – Nhiều chỗ trống:** Hệ thống lần lượt đưa vào lớp đủ người hợp lệ đến khi đạt sức chứa hoặc hết hàng chờ.
- **E1 – Xung đột đồng thời (409 Conflict nội bộ/ghi nhật ký):** Một giao dịch khác đã đổi trạng thái/sức chứa; hủy giao dịch hiện tại và tính lại trạng thái từ dữ liệu mới. Không gửi thông báo trùng cho một lần đưa vào lớp đã được ghi nhận.
- **E2 – Lỗi F10:** Không đảo ngược trạng thái đặt chỗ đã ghi nhận chỉ vì dịch vụ gửi email tạm lỗi; thông báo phải được giữ lại để thử lại theo cơ chế F10. Người triển khai backend F8 trong Sprint 2 phối hợp với người triển khai F10 chốt cơ chế thử lại và chống gửi lặp. Chính sách ân hạn khi email lỗi do trưởng nhóm Hoàng Kim Tiến quyết định (câu 4 mục 7).

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.2-01:** FIFO `waitlisted_at ASC`; vị trí tính khi truy vấn, không lưu số thứ tự cố định (ADR 0003).
- **BR-F8.2-02:** Tự động đưa vào lớp ngay, không cần xác nhận; người đủ điều kiện có ân hạn không phạt. Người không đủ điều kiện bị bỏ qua, xét tiếp người sau.
- **BR-F8.2-03:** Không đưa vào lớp khi còn dưới `PROMOTION_CUTOFF_MIN=30` phút; đúng 30 phút chưa phải “dưới 30 phút” theo câu chữ bản chốt phạm vi, cần giữ phép so sánh `<`.
- **BR-F8.2-04:** `PROMOTION_GRACE_MIN=15`; ban đêm email đợi giờ mở cửa, ân hạn tính từ thời điểm gửi email (ADR 0003 và DBML `grace_until`).
- **BR-F8.2-05:** Số chỗ chiếm vẫn gồm `BOOKED`, `PENDING_RECONFIRM`, `ATTENDED`, `NO_SHOW`; loại trừ `WAITLIST`, `CANCELLED`.
- **NF6:** Giao dịch có khóa bản ghi, không vượt sức chứa; nhất quán thứ tự khóa `members` rồi `class_sessions`.
- **Câu hỏi nghiệp vụ (trưởng nhóm Hoàng Kim Tiến chốt, xem mục 7):** Nếu người trong danh sách chờ không có user/email (lễ tân đã thao tác trên hồ sơ chưa có tài khoản), ADR chưa quy định cách bắt đầu ân hạn. `waitlisted_at` cùng thời điểm chưa có phân định thứ tự khi trùng thời điểm. Cơ chế thử lại email và chống gửi lặp do người triển khai backend F8 trong Sprint 2 phối hợp với người triển khai F10 chốt.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_bookings`, `notifications`.
- **Bảng đọc:** `class_sessions`, `members`, `memberships`, `packages`, `system_configs`, `branches`, `users`.

- `class_bookings`: `status`, `waitlisted_at`, `promoted_at`, `grace_until`, `class_session_id`, `member_id`.
- `class_sessions`: `capacity`, `start_at`, `branch_id`, `status`.
- `members`: `booking_locked_until`.
- `memberships`: `package_type`, `branch_id`, `status`, `end_date`.
- `notifications`: `user_id`, `type`, `channel`, `status`, `scheduled_at`, `sent_at`, `related_entity`, `related_id`.
- `system_configs`: `config_key`, `config_value`, `branch_id`.

Định nghĩa cột và ràng buộc: xem [ERD v2](../../design/erd/erd-v2.dbml) (mục 2.1).

#### G. Thiết kế giao diện và API liên quan

- **API:** Không có API do người dùng gọi; đây là luồng hệ thống được kích hoạt bởi sự kiện giải phóng chỗ.
- **Tiêu đề HTTP / Nội dung yêu cầu:** Không áp dụng.
- **Phản hồi thành công:** Không áp dụng; hệ thống cập nhật chỗ đặt và tạo thông báo bằng các giao dịch/luồng chạy lại không tạo trùng.
- **Màn hình giao diện liên quan:** Danh sách chờ của hội viên và màn hình quản lý lớp; thông báo do F10 hiển thị/gửi.
- Nếu backend cần API nội bộ hoặc tác vụ nền, thiết kế đó phải được người triển khai backend F8 trong Sprint 2 xác nhận; không công khai API vận hành đưa vào lớp cho ứng dụng gọi để tùy ý chọn thứ tự.

#### H. Tiêu chí chấp nhận

- **Kịch bản 1: FIFO và đưa vào lớp tự động**
  - **Given** Có một chỗ trống, còn ít nhất 30 phút, hai hội viên đủ điều kiện có `waitlisted_at` khác nhau.
  - **When** Hệ thống xử lý chỗ trống.
  - **Then** Hội viên có `waitlisted_at` sớm hơn được đổi từ `WAITLIST` sang `BOOKED`, có `promoted_at`; người tiếp theo chỉ được đưa vào lớp nếu còn chỗ; không yêu cầu xác nhận.
- **Kịch bản 2: Bỏ qua người không đủ điều kiện**
  - **Given** Người đầu hàng có gói `TIME` hết hạn hoặc đang bị khóa/trùng giờ; người tiếp theo đủ điều kiện.
  - **When** Hệ thống xử lý chỗ trống.
  - **Then** Người đầu bị bỏ qua; người kế tiếp được đưa vào lớp; thông báo tới đúng người được đưa vào lớp.
- **Kịch bản 3: Chỗ trống ban đêm**
  - **Given** Chỗ trống phát sinh ban đêm và chi nhánh đang đóng cửa.
  - **When** Người đủ điều kiện được đưa vào lớp.
  - **Then** `BOOKED` được ghi ngay, thông báo trong web có ngay, email được hẹn đến giờ mở cửa; ân hạn 15 phút bắt đầu lúc email gửi.
- **Kịch bản 4: Đúng mốc ngừng đưa vào lớp 30 phút**
  - **Given** Còn đúng 30 phút trước `start_at` và có người đủ điều kiện trong danh sách chờ.
  - **When** Hệ thống xử lý chỗ trống.
  - **Then** Vì quy tắc là ngừng khi còn **dưới** 30 phút, hệ thống vẫn được phép xét đưa vào lớp tại đúng mốc 30 phút.
- **Kịch bản 5: Đã dưới mốc ngừng đưa vào lớp**
  - **Given** Còn 29 phút trước giờ bắt đầu.
  - **When** Có chỗ trống.
  - **Then** Hệ thống không đưa vào lớp; chỗ để trống.
- **Kịch bản 6: Nhiều yêu cầu đồng thời**
  - **Given** Nhiều giao dịch cùng xử lý cùng buổi và chỉ còn một chỗ.
  - **When** Các giao dịch chạy đồng thời.
  - **Then** Tối đa một chỗ đặt được đưa vào chỗ cuối; sĩ số không vượt sức chứa và không gửi thông báo đưa vào lớp trùng do giao dịch bị hủy.
- **Kịch bản 7: Đưa vào lớp đồng thời cho hai buổi trùng giờ sau khi đổi lịch**
  - **Given** Hai danh sách chờ của cùng một hội viên được tạo khi hai buổi học chưa trùng giờ; sau đó một buổi học được đổi giờ nên hai buổi học giao nhau; mỗi buổi học có một chỗ trống và hội viên B đủ điều kiện đứng sau ở danh sách chờ của buổi học thứ hai.
  - **When** Hai tác vụ đưa vào lớp chạy đồng thời cho hai buổi học.
  - **Then** Hai giao dịch khóa dòng `members` trước `class_sessions`; chỉ một buổi học được chuyển chỗ đặt của hội viên đầu hàng sang `BOOKED`. Giao dịch còn lại kiểm tra lại sau khi lấy khóa, nhận ra lịch trùng và bỏ qua hội viên này theo ADR 0003, rồi xét hội viên kế tiếp. Không tạo hai chỗ đặt `BOOKED` chồng giờ cho cùng hội viên.

---

### 3.4. UC-F8.3: Hủy chỗ lớp nhóm

- **Mã UC:** UC-F8.3
- **Tên Use Case:** Hủy chỗ lớp nhóm
- **Tác nhân chính:** Hội viên (`MEMBER`), Lễ tân (`RECEPTIONIST`) khi thao tác hộ hội viên
- **Tác nhân phụ (nếu có):** Hệ thống tự động đưa người đủ điều kiện từ danh sách chờ vào chỗ trống (UC-F8.2)
- **Mô tả ngắn gọn:** Hội viên hủy chỗ đặt của mình; lễ tân có thể thực hiện thay. Hệ thống ghi nhận đúng hủy sớm/hủy muộn và xem xét người tiếp theo trong danh sách chờ khi có chỗ.

#### A. Tiền điều kiện

- Người thao tác đã đăng nhập; hội viên chỉ hủy chỗ đặt của mình, lễ tân có quyền thao tác hộ theo S3.
- Chỗ đặt tồn tại, thuộc hội viên được phép thao tác, và đang ở trạng thái có thể hủy.

#### B. Hậu điều kiện

- **Thành công:** Chỗ đặt chuyển `CANCELLED`, ghi `cancelled_at`, `cancel_reason` và `is_late_cancel` theo quy tắc đã chốt. Nếu chỗ đặt chiếm chỗ, hệ thống kích hoạt UC-F8.2 để xét lấp chỗ; việc này vẫn tuân mốc ngừng đưa vào lớp.
- **Thất bại:** Chỗ đặt giữ nguyên; không tính phạt và không mở chỗ nếu giao dịch thất bại.

#### C. Luồng sự kiện chính

1. Hội viên mở chỗ đặt của mình và chọn hủy; hoặc lễ tân tra cứu chỗ đặt theo yêu cầu hội viên.
2. Hệ thống xác thực vai trò, quyền sở hữu hội viên và phạm vi chi nhánh.
3. Hệ thống kiểm tra chỗ đặt còn hoạt động.
4. Hệ thống đọc `LATE_CANCEL_HOURS` và thời gian bắt đầu của buổi.
5. Nếu chỗ đặt là chỗ đã xác nhận (`BOOKED`) hoặc chỗ vừa được đưa vào lớp trong ân hạn, hệ thống xác định hủy sớm/hủy muộn; trạng thái danh sách chờ không được tính là chỗ đã xác nhận.
6. Hệ thống trong giao dịch khóa dữ liệu liên quan để tránh hủy hai lần/đưa vào lớp vượt sĩ số.
7. Hệ thống đặt `status = 'CANCELLED'`, `cancelled_at = now()`, `cancel_reason` phù hợp; với hủy muộn đặt `is_late_cancel = true`.
8. Nếu hủy muộn, hệ thống cập nhật số lần vi phạm của tháng theo dữ liệu chỗ đặt và áp dụng `booking_locked_until` khi đủ ngưỡng `PENALTY_LIMIT_PER_MONTH`.
9. Nếu chỗ được giải phóng, hệ thống thực hiện UC-F8.2 trong cùng giao dịch; không đưa ai vào nếu đã qua mốc ngừng đưa vào lớp.
10. Hệ thống trả kết quả và thông báo kết quả hủy.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Hủy trước giờ học ít nhất 2 giờ:** Tại bước 5, nếu còn ít nhất `LATE_CANCEL_HOURS`, hệ thống đặt `is_late_cancel = false`; không tăng số lần vi phạm.
- **A2 – Hủy chỗ đặt vừa được đưa vào lớp trong ân hạn:** Chỉ chỗ đặt vừa được hệ thống tự động chuyển từ `WAITLIST` sang `BOOKED` được hưởng 15 phút ân hạn không phạt; ân hạn tính từ lúc email đưa vào lớp được gửi (`grace_until`). Không áp dụng ân hạn này cho chỗ đặt `BOOKED` đặt trực tiếp, dòng `WAITLIST`, hay hủy buổi học. Trường hợp hội viên không có email cần trưởng nhóm Hoàng Kim Tiến chốt mốc ân hạn.
- **A3 – Hủy chỗ đặt danh sách chờ:** Hệ thống cho hủy dòng danh sách chờ và đặt `CANCELLED`; không có chỗ lớp được giải phóng. Nguồn chuẩn chưa đặc tả kết quả xử lý vi phạm cho trường hợp này; phần xử lý vi phạm của nhánh này chưa đủ điều kiện triển khai cho đến khi trưởng nhóm Hoàng Kim Tiến quyết định. Không suy ra mặc định miễn phạt.
- **E1 – Yêu cầu sai định dạng (400 Bad Request):** Mã chỗ đặt sai định dạng; trả `ProblemDetail` có lỗi trường.
- **E2 – Không có quyền (403 Forbidden):** Hội viên thao tác chỗ đặt người khác hoặc nhân viên ngoài phạm vi quyền.
- **E3 – Không tìm thấy chỗ đặt (404 Not Found):** Mã chỗ đặt không tồn tại hoặc không hiển thị trong phạm vi dữ liệu người gọi.
- **E4 – Chỗ đặt đã kết thúc/hủy hoặc không thể hủy (422 Unprocessable Content):** Trả lý do bằng tiếng Việt; không đổi trạng thái lần nữa.
- **E5 – Xung đột đồng thời (409 Conflict):** Chỗ đặt đã bị hủy/đổi trạng thái ở yêu cầu khác; hủy giao dịch thao tác hiện tại.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.3-01:** Hủy trước giờ bắt đầu ít nhất 2 giờ không bị phạt; mốc cấu hình `LATE_CANCEL_HOURS=2`.
- **BR-F8.3-02:** Hủy muộn hoặc vắng mặt đạt 3 lần trong tháng làm khóa đặt lớp 7 ngày (`PENALTY_LIMIT_PER_MONTH=3`, `PENALTY_LOCK_DAYS=7`).
- **BR-F8.3-03:** Chỉ chỗ đặt vừa được tự động đưa vào lớp từ `WAITLIST` sang `BOOKED` có 15 phút ân hạn không phạt; ERD mô tả `grace_until` bằng thời điểm email đưa vào lớp gửi + cấu hình ân hạn. Ân hạn không áp dụng cho mọi lần hủy.
- **BR-F8.3-04:** Hủy chỗ đặt mở chỗ để xử lý FIFO, nhưng không được tự động đưa vào lớp khi còn dưới 30 phút đến giờ bắt đầu.
- **Câu hỏi nghiệp vụ (trưởng nhóm Hoàng Kim Tiến chốt, xem mục 7):** “Trong tháng” là tháng dương lịch theo múi giờ nào hay khoảng trượt 30 ngày; mốc đúng 2 giờ đã được bản chốt phạm vi quyết định là không phạt (“ít nhất 2 giờ”). Ân hạn có bao gồm đúng mốc kết thúc hay không và chính sách hủy danh sách chờ cần được chốt. Nếu hội viên không có tài khoản/email, thời điểm bắt đầu ân hạn thay thế chưa được định nghĩa.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_bookings`, `members` khi áp khóa đặt lớp.
- **Bảng đọc:** `class_sessions`, `system_configs`, `notifications` (xác định thời điểm gửi email), `users`.

- `class_bookings`: `status`, `cancelled_at`, `cancel_reason`, `is_late_cancel`, `grace_until`, `promoted_at`, `member_id`, `class_session_id`.
- `members`: `booking_locked_until`.
- `class_sessions`: `start_at`.
- `system_configs`: `config_key`, `config_value`.

Các cột lịch sử phục vụ kiểm tra vi phạm; mốc sự kiện và kỳ tháng chờ trưởng nhóm Hoàng Kim Tiến chốt (câu 3 mục 7), chưa tự chọn truy vấn đếm theo `start_at` hay `cancelled_at`.

Định nghĩa cột và ràng buộc: xem [ERD v2](../../design/erd/erd-v2.dbml) (mục 2.1).

#### G. Thiết kế giao diện và API liên quan

- **API (đề xuất, người triển khai backend F8 trong Sprint 2 xác nhận):** `POST /api/v1/class-bookings/{bookingId}/cancel`
- **Tiêu đề HTTP:** `Authorization: Bearer <access_token>`.
- **Nội dung yêu cầu (DTO đề xuất):** `{}`; người gọi vai trò xác định `cancel_reason` (`BY_MEMBER` hoặc `BY_STAFF`), không nhận `is_late_cancel` từ ứng dụng gọi.
- **Phản hồi thành công (200 OK) – Hình dạng đề xuất:**
  ```json
  {
    "bookingId": 101,
    "status": "CANCELLED",
    "isLateCancel": false,
    "bookingLockedUntil": null
  }
  ```
- **Màn hình giao diện liên quan:** Chi tiết chỗ đặt / lịch lớp hội viên và màn hình lễ tân thao tác hộ.
- **Mã lỗi đề xuất:** `400`, `401`, `403`, `404`, `409`, `422`; không nhận cờ phạt từ trình duyệt.

#### H. Tiêu chí chấp nhận

- **Kịch bản 1: Hủy đúng mốc 2 giờ**
  - **Given** Hội viên có chỗ đặt `BOOKED` và thời điểm hủy cách `start_at` đúng 2 giờ.
  - **When** Hội viên hủy chỗ đặt.
  - **Then** Chỗ đặt thành `CANCELLED`, `is_late_cancel = false`, không tăng số lần vi phạm; chỗ được xét cho danh sách chờ theo UC-F8.2.
- **Kịch bản 2: Hủy muộn lần thứ ba trong tháng**
  - **Given** Hội viên đã có 2 lần hủy muộn/no-show trong kỳ tháng đang áp dụng theo kỳ tháng do trưởng nhóm Hoàng Kim Tiến xác nhận.
  - **When** Hội viên hủy chỗ đặt muộn.
  - **Then** Lần hủy được ghi `is_late_cancel = true`, hội viên bị khóa đặt trong 7 ngày theo `PENALTY_LOCK_DAYS`, trả thời điểm hết khóa.
- **Kịch bản 3: Hủy chỗ vừa được đưa vào lớp trong ân hạn**
  - **Given** Hội viên vừa được đưa vào lớp và thời điểm hiện tại nằm trong khoảng ân hạn tính từ email đưa vào lớp đã gửi.
  - **When** Hội viên hủy chỗ đặt.
  - **Then** Chỗ đặt chuyển `CANCELLED`, không đánh dấu hủy muộn và không tăng số lần vi phạm.
- **Kịch bản 4: Hai yêu cầu hủy đồng thời**
  - **Given** Hai yêu cầu cùng hủy một chỗ đặt đang hoạt động.
  - **When** Hai yêu cầu được xử lý đồng thời.
  - **Then** Chỉ một lần chuyển trạng thái thành công; không mở hai chỗ hoặc tạo hai lần đưa vào lớp cho cùng một chỗ.

---

### 3.5. UC-F8.4: Xác nhận lại hoặc từ chối khi đổi giờ lớp

- **Mã UC:** UC-F8.4
- **Tên Use Case:** Xác nhận lại hoặc từ chối khi đổi giờ lớp
- **Tác nhân chính:** Hội viên (`MEMBER`); Hệ thống cho hạn xác nhận; Lễ tân có thể hỗ trợ theo quyền thao tác hộ
- **Tác nhân phụ (nếu có):** Quản lý chi nhánh (`BRANCH_MANAGER`), F10
- **Mô tả ngắn gọn:** Khi quản lý đổi giờ một buổi, hội viên đang có chỗ xác nhận tham gia giờ mới trong thời hạn cấu hình hoặc từ chối. Hết hạn/từ chối thì mất chỗ, không bị phạt; chỗ được xét cho danh sách chờ.

#### A. Tiền điều kiện

- Buổi học tồn tại và quản lý chi nhánh đã thay đổi giờ bắt đầu/kết thúc.
- Hội viên có chỗ đặt đã xác nhận trước khi đổi giờ.
- Chỗ đặt đang ở trạng thái `PENDING_RECONFIRM` và chưa hết `reconfirm_expires_at`.

#### B. Hậu điều kiện

- **Thành công – xác nhận:** Chỗ đặt chuyển về `BOOKED`; hội viên giữ chỗ theo thời gian mới.
- **Thành công – từ chối/hết hạn:** Chỗ đặt chuyển `CANCELLED`, không phạt; chỗ được giải phóng và UC-F8.2 được xét theo mốc ngừng đưa vào lớp.
- **Thất bại:** Không đổi trạng thái chỗ đặt; không mở chỗ nếu thao tác không ghi nhận giao dịch.

#### C. Luồng sự kiện chính

1. Quản lý chi nhánh thay đổi giờ của `class_sessions`.
2. Hệ thống lưu `original_start_at`, cập nhật `start_at`/`end_at`, đặt trạng thái buổi `RESCHEDULED`.
3. Hệ thống xác định các chỗ đặt `BOOKED` đang chiếm chỗ và chuyển sang `PENDING_RECONFIRM`.
4. Hệ thống đặt `reconfirm_expires_at` theo `RECONFIRM_TTL_MIN=60`, phát thông báo đổi giờ qua F10.
5. Hội viên mở thông báo/lịch lớp và chọn xác nhận giờ mới hoặc không tham gia; lễ tân có thể thao tác thay theo quyền đã cấp.
6. Hệ thống kiểm tra quyền, chỗ đặt vẫn `PENDING_RECONFIRM`, và yêu cầu đến trước hạn.
7. Nếu xác nhận, hệ thống chuyển chỗ đặt về `BOOKED`; nếu từ chối, hệ thống chuyển chỗ đặt `CANCELLED` với lý do phù hợp, không phạt.
8. Khi chỗ được giải phóng, hệ thống thực hiện UC-F8.2 trong cùng giao dịch rồi ghi nhận giao dịch thay đổi; không đưa vào lớp sau mốc ngừng đưa vào lớp.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Hội viên không phản hồi trước hạn:** Tác vụ nền tìm `PENDING_RECONFIRM` quá `reconfirm_expires_at`, chuyển chỗ đặt sang `CANCELLED` và giải phóng chỗ; không tính phạt; xét danh sách chờ.
- **A2 – Hội viên báo không tham gia:** Hệ thống hủy chỗ đặt ngay, không phạt, rồi xét người trong danh sách chờ.
- **A3 – Chỉ đổi phòng:** Không chuyển chỗ đặt sang `PENDING_RECONFIRM`; chỉ thông báo thay đổi phòng theo quy tắc F8.
- **A4 – Chỉ đổi giờ:** Giữ nguyên chỗ hiện tại nhưng yêu cầu xác nhận lại trong hạn 60 phút.
- **E1 – Yêu cầu sai định dạng (400 Bad Request):** Dữ liệu hoặc mã chỗ đặt không hợp lệ.
- **E2 – Không có quyền (403 Forbidden):** Người gọi không phải chủ chỗ đặt, lễ tân không có quyền thao tác hộ, hoặc quản lý không thuộc chi nhánh.
- **E3 – Không tìm thấy buổi/chỗ đặt (404 Not Found):** Tài nguyên không tồn tại trong phạm vi truy cập.
- **E4 – Hạn đã hết hoặc chỗ đặt không chờ xác nhận (422 Unprocessable Content):** Hệ thống từ chối cập nhật và trả trạng thái mới nhất.
- **E5 – Xung đột đồng thời (409 Conflict):** Yêu cầu xác nhận và tác vụ hết hạn đồng thời; chỉ một chuyển trạng thái được ghi nhận giao dịch; thao tác còn lại nhận `409`.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.4-01:** Đổi giờ yêu cầu hội viên đã có chỗ xác nhận lại trong 60 phút (`RECONFIRM_TTL_MIN=60`).
- **BR-F8.4-02:** Không xác nhận hoặc báo không tham gia thì mất chỗ, không bị phạt; chỗ chuyển cho danh sách chờ theo UC-F8.2.
- **BR-F8.4-03:** Đổi phòng chỉ thông báo; không yêu cầu xác nhận lại.
- **BR-F8.4-04:** `PENDING_RECONFIRM` vẫn chiếm chỗ trong sức chứa (ERD/thiet-ke-du-lieu.md).
- **Câu hỏi nghiệp vụ (trưởng nhóm Hoàng Kim Tiến chốt, xem mục 7):** `cancel_reason` chính xác cho từ chối chủ động và hết hạn chưa được tách rõ; ERD có `NOT_RECONFIRMED`. Chưa nêu cách xử lý khi 60 phút hết sau giờ bắt đầu lớp, hoặc giờ mới tạo trùng lịch với buổi khác của hội viên. SRS này không tự chọn chính sách cho các trường hợp đó.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_sessions`, `class_bookings`, `notifications`.
- **Bảng đọc:** `members`, `system_configs`, `users`.

- `class_sessions`: `start_at`, `end_at`, `original_start_at`, `status`.
- `class_bookings`: `status`, `reconfirm_expires_at`, `cancel_reason`, `cancelled_at`, `is_late_cancel`.
- `notifications`: `user_id`, `type`, `channel`, `status`, `scheduled_at`, `sent_at`, `related_entity`, `related_id`.
- `system_configs`: `config_key`, `config_value`.

Định nghĩa cột và ràng buộc: xem [ERD v2](../../design/erd/erd-v2.dbml) (mục 2.1).

#### G. Thiết kế giao diện và API liên quan

- **API (đề xuất, người triển khai backend F8 trong Sprint 2 xác nhận):**
  - Quản lý đổi giờ: `POST /api/v1/class-sessions/{sessionId}/reschedule`.
  - Hội viên xác nhận/từ chối: `POST /api/v1/class-bookings/{bookingId}/reconfirm`.
- **Tiêu đề HTTP:** `Authorization: Bearer <access_token>`.
- **Nội dung yêu cầu (DTO đề xuất):**
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
- **Phản hồi thành công – Hình dạng đề xuất:** `200 OK` kèm `status`, `reconfirmExpiresAt`, `startAt`, `endAt` mới.
- **Màn hình giao diện liên quan:** Quản lý chi tiết buổi; hội viên lịch lớp/trung tâm thông báo.
- Đường dẫn, phương thức HTTP, cách mã hóa từ chối và mã HTTP phản hồi cần người triển khai backend F8 trong Sprint 2 xác nhận.

#### H. Tiêu chí chấp nhận

- **Kịch bản 1: Xác nhận giờ mới trong 60 phút**
  - **Given** Chỗ đặt đang `PENDING_RECONFIRM` và còn trước `reconfirm_expires_at`.
  - **When** Hội viên xác nhận tham gia giờ mới.
  - **Then** Chỗ đặt về `BOOKED`, vẫn chiếm một chỗ, lưu giờ mới ở buổi học và không nhân đôi chỗ đặt.
- **Kịch bản 2: Từ chối tham gia**
  - **Given** Chỗ đặt đang `PENDING_RECONFIRM`.
  - **When** Hội viên chọn không tham gia.
  - **Then** Chỗ đặt thành `CANCELLED`, không ghi phạt, chỗ được giải phóng và chuyển sang UC-F8.2 nếu chưa qua mốc ngừng đưa vào lớp.
- **Kịch bản 3: Hết hạn xác nhận**
  - **Given** Chỗ đặt chờ xác nhận đã quá hạn 60 phút.
  - **When** Tác vụ nền xử lý mốc hết hạn.
  - **Then** Chỗ đặt bị hủy không phạt và chỗ được xét cho danh sách chờ.
- **Kịch bản 4: Đổi phòng nhưng không đổi giờ**
  - **Given** Quản lý chỉ đổi `room_id`.
  - **When** Cập nhật buổi.
  - **Then** Hội viên được thông báo; chỗ đặt không chuyển `PENDING_RECONFIRM`.
- **Kịch bản 5: Hết hạn cạnh tranh xác nhận**
  - **Given** Hội viên gửi xác nhận đúng lúc tác vụ hết hạn đang xử lý cùng chỗ đặt.
  - **When** Hai giao dịch cạnh tranh.
  - **Then** Chỉ một trạng thái cuối được ghi nhận giao dịch; không vừa giữ chỗ vừa hủy chỗ đặt.

---

### 3.6. UC-F8.5: Xem lịch dạy lớp nhóm

- **Mã UC:** UC-F8.5
- **Tên Use Case:** Xem lịch dạy lớp nhóm
- **Tác nhân chính:** Huấn luyện viên (`TRAINER`)
- **Tác nhân phụ (nếu có):** Không
- **Mô tả ngắn gọn:** Huấn luyện viên xem các buổi lớp nhóm được phân công để chuẩn bị và vào màn hình điểm danh.

#### A. Tiền điều kiện

- Huấn luyện viên đã đăng nhập với vai trò `TRAINER`.
- Hồ sơ `staff` của người dùng tồn tại.

#### B. Hậu điều kiện

- **Thành công:** Không thay đổi dữ liệu; hệ thống trả lịch chỉ cho buổi có `trainer_id` tương ứng với huấn luyện viên đã đăng nhập.
- **Thất bại:** Không thay đổi dữ liệu; truy cập ngoài quyền bị chặn.

#### C. Luồng sự kiện chính

1. Huấn luyện viên mở lịch dạy và chọn khoảng thời gian.
2. Hệ thống xác thực vai trò `TRAINER` và ánh xạ tài khoản tới `staff.id`.
3. Hệ thống truy vấn `class_sessions` theo `trainer_id` của người dùng và khoảng ngày.
4. Hệ thống trả thông tin buổi: thời gian, loại lớp, phòng, trạng thái buổi và tổng số chỗ đặt theo các trạng thái liên quan.
5. Huấn luyện viên chọn một buổi để mở UC-F8.6 điểm danh.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Không có lịch trong khoảng:** Trả danh sách rỗng.
- **A2 – Buổi có huấn luyện viên dạy thay:** Nếu `class_sessions.trainer_id` đã được đổi theo ERD, chỉ người được gán hiện tại thấy buổi trong lịch của mình; lịch lặp gốc không thay thế phân công của buổi học cụ thể.
- **E1 – Bộ lọc sai định dạng (400 Bad Request):** Ngày lọc không hợp lệ.
- **E2 – Chưa đăng nhập (401 Unauthorized):** Token thiếu/sai/hết hạn.
- **E3 – Sai vai trò hoặc xem lịch người khác (403 Forbidden):** Không trả lịch huấn luyện viên khác.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.5-01:** `class_sessions.trainer_id` là huấn luyện viên được phân công cho từng buổi; ERD cho phép thay đổi người dạy riêng ở buổi học.
- **BR-F8.5-02:** S3: quyền và phạm vi dữ liệu phải kiểm tra ở backend, không dựa vào lọc giao diện.
- **BR-F8.5-03:** Lịch lớp nhóm và lịch PT có thể cùng xuất hiện trong sơ đồ tổng hợp F9; use case này chỉ trả buổi lớp nhóm.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** Không có; chỉ đọc.
- **Bảng truy vấn:** `staff`, `users`, `class_sessions`, `class_schedules`, `class_types`, `rooms`, `class_bookings`.

- `class_sessions`: `trainer_id`, `branch_id`, `start_at`, `end_at`, `room_id`, `status`.
- `class_schedules`: `class_type_id`.
- `class_types`: `name`.
- `class_bookings`: `class_session_id`, `status`.
- `staff`: `id`.

Định nghĩa cột và ràng buộc: xem [ERD v2](../../design/erd/erd-v2.dbml) (mục 2.1).

#### G. Thiết kế giao diện và API liên quan

- **API (đề xuất, người triển khai backend F8 trong Sprint 2 xác nhận):** `GET /api/v1/trainers/me/class-sessions?from={date}&to={date}`.
- **Tiêu đề HTTP:** `Authorization: Bearer <access_token>`.
- **Nội dung yêu cầu (DTO):** Không có; bộ lọc ngày là tham số truy vấn.
- **Phản hồi thành công (200 OK) – Hình dạng đề xuất:**
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

#### H. Tiêu chí chấp nhận

- **Kịch bản 1: Huấn luyện viên xem đúng lịch được phân công**
  - **Given** Huấn luyện viên A có các buổi học được gán `trainer_id` của A và có buổi học của huấn luyện viên B trong cùng chi nhánh.
  - **When** Huấn luyện viên A gọi lịch của mình.
  - **Then** Chỉ trả buổi học có `trainer_id` của A, bất kể ứng dụng gọi truyền tham số nào nhằm xem huấn luyện viên B.
- **Kịch bản 2: Khoảng thời gian không có lớp**
  - **Given** Khoảng lọc hợp lệ nhưng không có buổi học được phân công.
  - **When** Huấn luyện viên xem lịch.
  - **Then** Trả `200 OK` với danh sách rỗng.
- **Kịch bản 3: Nhân viên vai trò khác yêu cầu lịch huấn luyện viên**
  - **Given** Người gọi đã đăng nhập nhưng không có vai trò `TRAINER`.
  - **When** Người dùng gọi API.
  - **Then** Trả `403 Forbidden` và không lộ dữ liệu lịch.

---

### 3.7. UC-F8.6: Điểm danh học viên lớp học nhóm

- **Mã UC:** UC-F8.6
- **Tên Use Case:** Điểm danh học viên lớp học nhóm
- **Tác nhân chính:** Huấn luyện viên (`TRAINER`) được phân công dạy buổi
- **Tác nhân phụ (nếu có):** Không
- **Mô tả ngắn gọn:** Huấn luyện viên ghi nhận học viên đã tham dự hoặc vắng mặt cho buổi mình dạy. Check-in ở quầy chỉ đánh dấu `arrived_at`; chỉ điểm danh của huấn luyện viên quyết định `ATTENDED`/`NO_SHOW`.

#### A. Tiền điều kiện

- Huấn luyện viên đăng nhập vai trò `TRAINER`.
- Buổi học tồn tại và `class_sessions.trainer_id` thuộc huấn luyện viên.
- Chỗ đặt cần điểm danh đang chiếm chỗ và chưa bị hủy.

#### B. Hậu điều kiện

- **Thành công:** `class_bookings.status` chuyển thành `ATTENDED` hoặc `NO_SHOW`; `attendance_marked_by` lưu `users.id` của huấn luyện viên. `NO_SHOW` được tính vào quy tắc khóa đặt lớp theo bản chốt.
- **Thất bại:** Không thay đổi điểm danh; trạng thái hiện tại được giữ nguyên.

#### C. Luồng sự kiện chính

1. Huấn luyện viên mở một buổi được phân công từ UC-F8.5.
2. Hệ thống xác thực vai trò, `staff.id` và so khớp với `class_sessions.trainer_id`.
3. Hệ thống trả danh sách chỗ đặt đang có chỗ cần điểm danh; không trộn các dòng chỉ đang `WAITLIST` hoặc đã `CANCELLED`.
4. Huấn luyện viên chọn từng hội viên và ghi nhận có mặt (`ATTENDED`) hoặc vắng mặt (`NO_SHOW`).
5. Hệ thống kiểm tra hợp lệ rằng trạng thái được chọn là một trong hai kết quả điểm danh và chỗ đặt hợp lệ.
6. Hệ thống cập nhật chỗ đặt và ghi `attendance_marked_by`.
7. Nếu trạng thái là `NO_SHOW`, hệ thống cộng sự kiện này vào quy tắc “3 lần trong tháng”, và khóa đặt lớp 7 ngày khi đạt ngưỡng.
8. Hệ thống trả kết quả đã điểm danh.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Hội viên đã check-in ở quầy:** Nếu `arrived_at` đã có, vẫn cần huấn luyện viên xác nhận `ATTENDED`; check-in không tự chuyển trạng thái điểm danh.
- **A2 – Điểm danh cả danh sách:** Hình thức điểm danh cả danh sách chờ trưởng nhóm Hoàng Kim Tiến chốt nghiệp vụ (câu 12 mục 7). Nếu được chấp nhận, người triển khai backend F8 trong Sprint 2 chốt API, kiểm tra từng dòng và tính nguyên tử của giao dịch.
- **E1 – Dữ liệu gửi sai định dạng (400 Bad Request):** Trạng thái ngoài tập cho phép hoặc trường bắt buộc thiếu.
- **E2 – Chưa đăng nhập (401 Unauthorized):** Token không hợp lệ.
- **E3 – Không được phân công / sai quyền (403 Forbidden):** Huấn luyện viên không phải huấn luyện viên của buổi học.
- **E4 – Buổi học/chỗ đặt không tồn tại (404 Not Found):** Trả `ProblemDetail` tiếng Việt.
- **E5 – chỗ đặt không thể điểm danh (422 Unprocessable Content):** Chỗ đặt đang `WAITLIST`, `CANCELLED` hoặc đã được điểm danh; không cập nhật.
- **E6 – Xung đột cập nhật đồng thời (409 Conflict):** Trạng thái điểm danh đã được thay đổi trong yêu cầu khác.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.6-01:** Điểm danh lớp là của huấn luyện viên; `attendance_marked_by` tham chiếu `users.id`.
- **BR-F8.6-02:** F4 ghi `arrived_at` khi check-in có lớp sắp bắt đầu; điều đó không thay cho `ATTENDED` do huấn luyện viên xác nhận (bản chốt phạm vi F4 và ERD).
- **BR-F8.6-03:** Vắng mặt theo điểm danh của huấn luyện viên là một trong các sự kiện tính vi phạm; đủ 3 lần trong tháng khóa đặt lớp 7 ngày.
- **BR-F8.6-04:** Chỗ đặt `ATTENDED` và `NO_SHOW` vẫn được tính trong sĩ số của buổi học (thiết kế dữ liệu ERD).
- **Câu hỏi nghiệp vụ (trưởng nhóm Hoàng Kim Tiến chốt, xem mục 7):** Khoảng thời gian được phép điểm danh, có cho sửa kết quả đã ghi hay không, và xử lý NO_SHOW khi huấn luyện viên chưa điểm danh chưa được bản chốt phạm vi định nghĩa. Không tự coi hết giờ là vắng.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_bookings`, có thể `members.booking_locked_until` khi đủ ngưỡng.
- **Bảng đọc:** `class_sessions`, `staff`, `users`, `system_configs`.

- `class_bookings`: `status`, `attendance_marked_by`, `arrived_at`, `member_id`, `class_session_id`, `is_late_cancel`, `cancelled_at`.
- `class_sessions`: `trainer_id`, `start_at`.
- `members`: `booking_locked_until`.
- `system_configs`: `config_key`, `config_value`.

Các cột lịch sử phục vụ kiểm tra vi phạm; mốc sự kiện và kỳ tháng chờ trưởng nhóm Hoàng Kim Tiến chốt (câu 3 mục 7), chưa tự chọn truy vấn đếm theo `start_at` hay `cancelled_at`.

Định nghĩa cột và ràng buộc: xem [ERD v2](../../design/erd/erd-v2.dbml) (mục 2.1).

#### G. Thiết kế giao diện và API liên quan

- **API (đề xuất, người triển khai backend F8 trong Sprint 2 xác nhận):** `PUT /api/v1/class-sessions/{sessionId}/attendance`.
- **Tiêu đề HTTP:** `Authorization: Bearer <access_token>`.
- **Nội dung yêu cầu (DTO đề xuất):**
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
- **Phản hồi thành công (200 OK) – Hình dạng đề xuất:**
  ```json
  {
    "sessionId": 123,
    "updatedCount": 2
  }
  ```
- **Màn hình giao diện liên quan:** Danh sách học viên và điểm danh buổi lớp nhóm; wireframe hiện chưa có file được chỉ định.
- Gửi từng học viên hay cả danh sách, và tính nguyên tử của cập nhật cả danh sách cần người triển khai backend F8 trong Sprint 2 xác nhận.

#### H. Tiêu chí chấp nhận

- **Kịch bản 1: Huấn luyện viên ghi nhận học viên có mặt**
  - **Given** Huấn luyện viên được phân công; chỗ đặt `BOOKED` có `arrived_at` hoặc học viên đã có mặt tại lớp.
  - **When** Huấn luyện viên đánh dấu `ATTENDED`.
  - **Then** Chỗ đặt thành `ATTENDED`, ghi `attendance_marked_by`; `arrived_at` không bị dùng làm nguồn thay thế cho thao tác này.
- **Kịch bản 2: Huấn luyện viên ghi nhận vắng mặt đạt ngưỡng**
  - **Given** Hội viên có 2 lần vi phạm được tính trong tháng và huấn luyện viên được phân công cho buổi.
  - **When** Huấn luyện viên đánh dấu chỗ đặt `NO_SHOW`.
  - **Then** Chỗ đặt thành `NO_SHOW`; hệ thống áp dụng khóa đặt 7 ngày khi sự kiện này là lần thứ ba theo kỳ tháng do trưởng nhóm Hoàng Kim Tiến xác nhận.
- **Kịch bản 3: Huấn luyện viên khác thử điểm danh**
  - **Given** Huấn luyện viên B không được gán vào buổi của huấn luyện viên A.
  - **When** Huấn luyện viên B gửi điểm danh.
  - **Then** Trả `403 Forbidden`, không đổi chỗ đặt.
- **Kịch bản 4: Hội viên đã check-in nhưng chưa được điểm danh**
  - **Given** `arrived_at` đã có nhưng chỗ đặt vẫn `BOOKED`.
  - **When** Chưa có thao tác điểm danh của huấn luyện viên.
  - **Then** Hệ thống vẫn chưa coi chỗ đặt là `ATTENDED`; check-in tại quầy không tự điểm danh.

---

### 3.8. UC-F8.7: Quản lý lịch lặp lớp học và phân công huấn luyện viên

- **Mã UC:** UC-F8.7
- **Tên Use Case:** Quản lý lịch lặp lớp học và phân công huấn luyện viên
- **Tác nhân chính:** Quản lý chi nhánh (`BRANCH_MANAGER`)
- **Tác nhân phụ (nếu có):** Hệ thống sinh buổi UC-F8.8; huấn luyện viên được phân công
- **Mô tả ngắn gọn:** Quản lý loại lớp và lịch lặp hằng tuần tại chi nhánh; gán phòng, thời gian, sức chứa và huấn luyện viên có vai trò `TRAINER`. Quản lý cũng có thể hủy một buổi cụ thể theo quy tắc F8.

#### A. Tiền điều kiện

- Quản lý đăng nhập với vai trò `BRANCH_MANAGER` và có chi nhánh hợp lệ.
- Phòng, loại lớp, huấn luyện viên được chọn tồn tại và thuộc phạm vi chi nhánh theo quy tắc dữ liệu.

#### B. Hậu điều kiện

- **Thành công:** Lịch lặp được tạo/cập nhật hoặc chuyển `INACTIVE`; dữ liệu gốc không bị xóa cứng. Lịch hợp lệ là đầu vào cho UC-F8.8.
- **Thành công khi hủy buổi:** Buổi cụ thể chuyển `CANCELLED`; các chỗ đặt đang hoạt động và danh sách chờ của buổi bị hủy không phạt; F10 thông báo cho tất cả người bị ảnh hưởng.
- **Thất bại:** Không lưu một phần lịch; giao dịch bị hủy.

#### C. Luồng sự kiện chính

1. Quản lý mở mục quản lý lớp học, chọn loại lớp hoặc tạo loại lớp.
2. Quản lý tạo lịch lặp: loại lớp, huấn luyện viên, phòng, thứ trong tuần, giờ bắt đầu/kết thúc, sức chứa, ngày hiệu lực `valid_from` và ngày kết thúc `valid_to` nếu có.
3. Hệ thống kiểm tra hợp lệ dữ liệu yêu cầu và quyền quản lý đúng chi nhánh.
4. Hệ thống xác nhận `day_of_week` theo ISO từ 1 đến 7, `end_time > start_time`, sức chứa dương và không lớn hơn sức chứa phòng.
5. Hệ thống xác nhận `trainer_id` trỏ tới nhân viên có vai trò `TRAINER`, phòng và loại lớp thuộc phạm vi hợp lệ.
6. Hệ thống tạo `class_schedules` trạng thái `ACTIVE` và ghi thời điểm tạo/cập nhật.
7. Khi cần ngừng lịch, quản lý chuyển trạng thái sang `INACTIVE` thay vì xóa dữ liệu.
8. Khi hủy một buổi cụ thể, quản lý chọn buổi học; hệ thống hủy buổi học cùng mọi chỗ đặt đang hoạt động và danh sách chờ của buổi học, không đánh dấu hủy muộn, rồi tạo thông báo cho các hội viên bị ảnh hưởng qua F10.
9. Hệ thống trả lịch/buổi đã lưu; UC-F8.8 chịu trách nhiệm sinh `class_sessions` theo ngày hiệu lực.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Cập nhật lịch lặp:** Quản lý sửa các trường được phép của lịch lặp; phạm vi ảnh hưởng đến các `class_sessions` đã sinh chưa được quy định (xem câu hỏi cần chốt).
- **A2 – Ngừng lịch:** Chuyển status sang `INACTIVE`; không xóa cứng lịch lặp.
- **A3 – Chưa có ngày kết thúc:** `valid_to = NULL` biểu thị không khai báo ngày dừng; cách sinh buổi học vẫn phụ thuộc số ngày sinh buổi trước cần chốt.
- **A4 – Hủy một buổi cụ thể:** Chuyển `class_sessions.status = 'CANCELLED'`; mọi chỗ đặt đang hoạt động, gồm `BOOKED`, `PENDING_RECONFIRM`, `WAITLIST`, được chuyển `CANCELLED` với `cancel_reason = 'SESSION_CANCELLED'`, không phạt; gửi thông báo cho tất cả người có chỗ đặt/danh sách chờ của buổi. Không áp dụng tự động đưa vào lớp cho buổi đã hủy.
- **E1 – Dữ liệu sai định dạng/không thỏa ràng buộc (400 Bad Request):** Trả `ProblemDetail` theo trường dữ liệu.
- **E2 – Không có quyền/khác chi nhánh (403 Forbidden):** Từ chối và không tiết lộ dữ liệu của chi nhánh khác.
- **E3 – Không tìm thấy phòng/loại lớp/nhân viên (404 Not Found):** Trả lỗi tiếng Việt.
- **E4 – Trùng hoặc xung đột dữ liệu (409 Conflict):** Ràng buộc duy nhất hoặc cập nhật cạnh tranh.
- **E5 – Vi phạm nghiệp vụ (422 Unprocessable Content):** Sức chứa lớn hơn sức chứa phòng, giờ kết thúc không sau giờ bắt đầu, hoặc huấn luyện viên không có vai trò `TRAINER`.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.7-01:** `class_schedules` là lịch lặp theo tuần; `day_of_week`: 1 = thứ Hai … 7 = Chủ nhật (ISO).
- **BR-F8.7-02:** `0 < capacity <= rooms.capacity`; `end_time > start_time`; huấn luyện viên phải có vai trò `TRAINER` (ERD).
- **BR-F8.7-03:** Lịch lặp chứa chi nhánh, loại lớp, huấn luyện viên, phòng, ngày/giờ, sức chứa, `valid_from`, `valid_to`, trạng thái.
- **BR-F8.7-04:** Chuyển trạng thái `INACTIVE`, không xóa cứng dữ liệu nghiệp vụ.
- **BR-F8.7-05:** Hủy buổi phải hủy toàn bộ chỗ đặt/danh sách chờ, không phạt và thông báo mọi người bị ảnh hưởng (bản chốt phạm vi F8). Không xóa cứng `class_sessions`/`class_bookings`.
- **BR-F8.7-06:** Không xếp huấn luyện viên trùng giờ với buổi PT hoặc lớp nhóm khác; quy tắc F9 nêu chặn trùng lịch huấn luyện viên, kể cả giờ dạy lớp nhóm. Việc xác minh phải tính theo các buổi học cụ thể.
- **Câu hỏi nghiệp vụ (trưởng nhóm Hoàng Kim Tiến chốt, xem mục 7):** Bản chốt phạm vi không mô tả kiểm tra trùng phòng giữa lịch lặp; chưa nêu cách xử lý khi hai lịch lặp đè cùng phòng. Cũng chưa định nghĩa sửa/ngừng lịch sau khi buổi học/chỗ đặt đã sinh, ngày lễ/ngoại lệ, hoặc giảm sức chứa dưới số chỗ đã đặt.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_types`, `class_schedules`; khi hủy buổi còn có `class_sessions`, `class_bookings`, `notifications`.
- **Bảng đọc:** `branches`, `rooms`, `staff`, `users`.

- `class_types`: `name`, `description`, `status`.
- `class_schedules`: `branch_id`, `class_type_id`, `trainer_id`, `room_id`, `day_of_week`, `start_time`, `end_time`, `capacity`, `valid_from`, `valid_to`, `status`.
- `class_sessions`: `status`.
- `class_bookings`: `status`, `cancel_reason`, `is_late_cancel`.
- `notifications`: `related_entity`, `related_id`, `user_id`, `type`, `channel`, `status`.
- `rooms`: `capacity`, `branch_id`.
- `staff`: `id`, `branch_id`.

Định nghĩa cột và ràng buộc: xem [ERD v2](../../design/erd/erd-v2.dbml) (mục 2.1).

#### G. Thiết kế giao diện và API liên quan

- **API (đề xuất, người triển khai backend F8 trong Sprint 2 xác nhận):**
  - `POST /api/v1/class-types`
  - `POST /api/v1/class-schedules`
  - `PATCH /api/v1/class-schedules/{scheduleId}`
  - `PATCH /api/v1/class-schedules/{scheduleId}/status`
  - `POST /api/v1/class-sessions/{sessionId}/cancel`
- **Tiêu đề HTTP:** `Authorization: Bearer <access_token>`.
- **Nội dung yêu cầu (DTO đề xuất):**
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
- **Phản hồi thành công (201 Created) – hình dạng đề xuất:** Trả `scheduleId`, `branchId`, các trường lịch và `status: "ACTIVE"`.
- **Màn hình giao diện liên quan:** Quản lý lịch lớp ở chi nhánh; wireframe chưa được liên kết trong repo.
- Danh sách API, cách cập nhật, quyền quản lý buổi học và mã HTTP phản hồi là đề xuất, cần người triển khai backend F8 trong Sprint 2 duyệt.

#### H. Tiêu chí chấp nhận

- **Kịch bản 1: Tạo lịch lặp hợp lệ**
  - **Given** Quản lý đúng chi nhánh chọn huấn luyện viên vai trò `TRAINER`, phòng hợp lệ, sức chứa không vượt phòng và giờ kết thúc sau giờ bắt đầu.
  - **When** Quản lý tạo lịch thứ Ba.
  - **Then** Hệ thống lưu `day_of_week = 2`, `status = ACTIVE`, `valid_from` cùng các trường đã xác nhận và trả `201 Created`.
- **Kịch bản 2: Sức chứa vượt phòng**
  - **Given** Phòng có sức chứa 20 nhưng yêu cầu đặt sức chứa 21.
  - **When** Quản lý lưu lịch.
  - **Then** Hệ thống trả `422 Unprocessable Content`, không tạo lịch lặp.
- **Kịch bản 3: Huấn luyện viên không có vai trò `TRAINER`**
  - **Given** `trainer_id` hợp lệ nhưng vai trò nhân viên không phải `TRAINER`.
  - **When** Quản lý lưu lịch.
  - **Then** Hệ thống từ chối `422` và không lưu lịch.
- **Kịch bản 4: Quản lý chi nhánh khác**
  - **Given** Phòng/huấn luyện viên thuộc chi nhánh khác với người quản lý.
  - **When** Quản lý gửi yêu cầu tạo/cập nhật lịch.
  - **Then** Hệ thống trả `403 Forbidden`; dữ liệu không đổi.
- **Kịch bản 5: Ngừng lịch**
  - **Given** Lịch lặp đang `ACTIVE`.
  - **When** Quản lý ngừng lịch.
  - **Then** Lịch lặp chuyển `INACTIVE`, dữ liệu lịch lặp không bị xóa vật lý.
- **Kịch bản 6: Hủy buổi đã có chỗ đặt và danh sách chờ**
  - **Given** Buổi học chưa diễn ra có chỗ đặt `BOOKED`/`PENDING_RECONFIRM` và các dòng `WAITLIST`.
  - **When** Quản lý chi nhánh hủy buổi.
  - **Then** Buổi học thành `CANCELLED`; các chỗ đặt đang hoạt động và danh sách chờ thành `CANCELLED` với `cancel_reason = SESSION_CANCELLED`, `is_late_cancel = false`; không áp vi phạm; F10 thông báo cho mọi người bị ảnh hưởng.
- **Kịch bản 7: Huấn luyện viên đã có buổi PT trùng giờ**
  - **Given** Huấn luyện viên được xếp một buổi PT trùng giờ với buổi học lớp nhóm cụ thể.
  - **When** Quản lý lưu phân công tạo ra trùng lịch.
  - **Then** Hệ thống từ chối tạo/xếp lịch; không để huấn luyện viên có buổi PT và lớp nhóm chồng giờ.

---

### 3.9. UC-F8.8: Sinh buổi học từ lịch lặp

- **Mã UC:** UC-F8.8
- **Tên Use Case:** Sinh buổi học từ lịch lặp
- **Tác nhân chính:** Hệ thống
- **Tác nhân phụ (nếu có):** UC-F8.7
- **Mô tả ngắn gọn:** Tạo các dòng `class_sessions` tương ứng với ngày xảy ra của `class_schedules` lặp hằng tuần để hội viên có thể xem và đặt chỗ.

#### A. Tiền điều kiện

- Có `class_schedules` trạng thái `ACTIVE`; ngày phát sinh cần sinh nằm trong khoảng hiệu lực `valid_from` đến `valid_to` (nếu có). Điều kiện này áp dụng cho **ngày phát sinh**, không buộc thời điểm chạy tác vụ nền phải sau `valid_from`; số ngày sinh buổi trước chưa được chốt.
- Loại lớp, phòng và huấn luyện viên của lịch vẫn hợp lệ.

#### B. Hậu điều kiện

- **Thành công:** Mỗi lần ngày phát sinh được xử lý có tối đa một dòng `class_sessions` theo ràng buộc duy nhất `(class_schedule_id, occurrence_date)`. Buổi học chép `branch_id`, `trainer_id`, `room_id`, `capacity`; ghi `occurrence_date` theo ngày Việt Nam và `start_at/end_at` là thời điểm tương ứng.
- **Thất bại:** Không tạo buổi học trùng; lỗi một ngày phát sinh không được tạo bản ghi sai hoặc sai lịch.

#### C. Luồng sự kiện chính

1. Tác vụ nền được kích hoạt theo lịch chạy của hệ thống.
2. Hệ thống tìm `class_schedules` `ACTIVE` có ngày hiệu lực bao gồm ngày cần sinh.
3. Hệ thống xác định các ngày xảy ra theo `day_of_week` ISO trong khoảng hiệu lực.
4. Hệ thống phối hợp ngày + giờ địa phương chi nhánh thành `start_at`/`end_at`; `occurrence_date` lưu ngày theo giờ Việt Nam và không thay đổi khi buổi học được đổi giờ.
5. Hệ thống chép các giá trị tại thời điểm sinh từ lịch lặp sang buổi học: `class_schedule_id`, `branch_id`, `trainer_id`, `room_id`, `capacity`; đặt trạng thái `SCHEDULED`.
6. Hệ thống thêm buổi học; unique `(class_schedule_id, occurrence_date)` ngăn sinh trùng khi tác vụ nền chạy lại.
7. Hệ thống hoàn tất các ngày phát sinh và ghi kết quả tác vụ nền.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Buổi học đã tồn tại:** Khi khóa duy nhất đã có ngày phát sinh, không tạo bản sao; tác vụ nền có thể tiếp tục với ngày phát sinh khác.
- **A2 – Lịch lặp hết hạn/INACTIVE:** Bỏ qua lịch và không sinh thêm buổi học.
- **E1 – Lịch lặp tham chiếu dữ liệu không còn hợp lệ:** Không tạo buổi học; ghi lỗi để người triển khai backend F8 trong Sprint 2 xử lý. Quy tắc sửa buổi học/chỗ đặt hiện có không được tự suy rộng.
- **E2 – Xung đột đồng thời giữa hai tác vụ nền (409/xung đột ràng buộc duy nhất nội bộ):** Ràng buộc duy nhất bảo vệ khỏi trùng lặp; xử lý chạy lại không tạo trùng và tiếp tục các ngày phát sinh khác.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.8-01:** F8 yêu cầu lịch lớp lặp hằng tuần; bảng `class_sessions` lưu từng buổi cụ thể.
- **BR-F8.8-02:** Duy nhất `(class_schedule_id, occurrence_date)`; ngày phát sinh là ngày theo lịch lặp tại lúc sinh, không đổi khi đổi giờ (`thiet-ke-du-lieu.md`, DBML).
- **BR-F8.8-03:** Giá trị phòng, huấn luyện viên và sức chứa được chép sang `class_sessions`; DBML cho phép thay huấn luyện viên của từng buổi học khi có người dạy thay.
- **Câu hỏi nghiệp vụ (trưởng nhóm Hoàng Kim Tiến chốt, xem mục 7):** Nguồn chuẩn chưa xác định tác vụ nền chạy lúc nào, cần sinh buổi trước bao nhiêu ngày, có sinh ngay đúng ngày `valid_from` hay không, xử lý buổi đã sinh khi lịch lặp sửa/ngừng, ngày lễ và các buổi bị hủy. `BOOKING_WINDOW_DAYS=7` là giới hạn đặt chỗ, không được tự coi là số ngày sinh buổi trước.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_sessions`.
- **Bảng đọc:** `class_schedules`, `class_types`, `branches`, `rooms`, `staff`.

- `class_schedules`: `branch_id`, `class_type_id`, `trainer_id`, `room_id`, `capacity`, `day_of_week`, `start_time`, `end_time`, `valid_from`, `valid_to`, `status`.
- `class_sessions`: `class_schedule_id`, `occurrence_date`, `branch_id`, `trainer_id`, `room_id`, `capacity`, `start_at`, `end_at`, `original_start_at`, `status`.

Định nghĩa cột và ràng buộc: xem [ERD v2](../../design/erd/erd-v2.dbml) (mục 2.1).

#### G. Thiết kế giao diện và API liên quan

- **API:** Không có API người dùng trong đặc tả này; tác vụ hệ thống nội bộ.
- **Tiêu đề HTTP / Nội dung yêu cầu:** Không áp dụng.
- **Phản hồi thành công:** Không áp dụng; tác vụ nền ghi nhận kết quả nội bộ.
- **Màn hình giao diện liên quan:** Không có màn hình riêng; buổi học được hiển thị ở lịch công khai/hội viên/huấn luyện viên.
- Số ngày sinh buổi trước do trưởng nhóm Hoàng Kim Tiến chốt; lịch chạy và cơ chế thử lại do người triển khai backend F8 trong Sprint 2 xác nhận; không tạo hợp đồng API từ UC này.

#### H. Tiêu chí chấp nhận

- **Kịch bản 1: Tạo buổi vào thứ phù hợp**
  - **Given** Lịch lặp `ACTIVE`, `day_of_week = 2`, ngày phát sinh thứ Ba nằm trong khoảng hiệu lực.
  - **When** Tác vụ nền sinh buổi học trong khoảng ngày phát sinh đã được trưởng nhóm Hoàng Kim Tiến chọn.
  - **Then** Hệ thống tạo đúng một `class_sessions` với `occurrence_date` là ngày thứ Ba theo giờ Việt Nam và chép đúng huấn luyện viên/phòng/sức chứa tại thời điểm sinh.
- **Kịch bản 2: Tác vụ nền chạy lặp không tạo trùng**
  - **Given** Buổi học của `(class_schedule_id, occurrence_date)` đã tồn tại.
  - **When** Tác vụ nền chạy lại cùng ngày phát sinh.
  - **Then** Không có buổi học thứ hai; ràng buộc duy nhất giữ tính chạy lại không tạo trùng.
- **Kịch bản 3: Lịch không hoạt động hoặc ngoài hiệu lực**
  - **Given** Lịch lặp `INACTIVE`, hoặc ngày phát sinh đang xét nằm trước `valid_from` hay sau `valid_to` nếu có.
  - **When** Tác vụ nền xét lịch lặp.
  - **Then** Không sinh buổi học ngoài khoảng hiệu lực.
- **Kịch bản 4: Đổi giờ buổi học đã sinh**
  - **Given** Buổi học được đổi giờ sau khi sinh.
  - **When** Tác vụ nền chạy lại cho cùng ngày phát sinh.
  - **Then** Không tạo buổi học ở giờ cũ hoặc bản sao; `occurrence_date` giữ nguyên.

---

## 4. Quy tắc nghiệp vụ áp dụng chung

1. **Điều kiện:** Chỉ gói `TIME` `ACTIVE` dùng được tại đúng chi nhánh cho phép đặt hoặc vào danh sách chờ; `PT` và `SESSION` không thay thế điều kiện này.
2. **Sức chứa:** Số chỗ đặt giữ chỗ gồm `BOOKED`, `PENDING_RECONFIRM`, `ATTENDED`, `NO_SHOW`; loại `WAITLIST`, `CANCELLED`. Không vượt `class_sessions.capacity`.
3. **Trùng giờ:** Một hội viên không được có hai chỗ đặt đang hoạt động cho các buổi có thời gian giao nhau; kiểm tra đồng thời phải khóa bản ghi.
4. **FIFO:** `waitlisted_at` tăng dần; không lưu vị trí danh sách chờ cố định và không cho người thao tác hộ vượt hàng.
5. **Đưa vào lớp:** Tự động, không cần xác nhận; bỏ qua người không đủ điều kiện và xét kế tiếp; mốc ngừng đưa vào lớp chỉ khi “dưới 30 phút”; chỗ sau mốc ngừng đưa vào lớp để trống.
6. **Hủy chỗ:** Hủy trước giờ ít nhất 2 giờ không phạt; ân hạn 15 phút chỉ áp dụng cho chỗ đặt vừa được tự động đưa vào lớp, không áp dụng chung cho mọi chỗ đặt; đủ 3 hủy muộn/vắng mặt trong tháng khóa đặt 7 ngày.
7. **Đổi giờ:** Đổi phòng chỉ báo; đổi giờ buộc chỗ đặt đang có chỗ xác nhận lại trong 60 phút; từ chối/hết hạn mất chỗ không phạt, sau đó xét danh sách chờ.
8. **Điểm danh:** F4 chỉ ghi `arrived_at`; huấn luyện viên chịu trách nhiệm cập nhật `ATTENDED`/`NO_SHOW`.
9. **Lịch lặp:** Thứ trong tuần theo ISO 1–7, `end_time > start_time`, `0 < capacity <= rooms.capacity`, huấn luyện viên phải có vai trò `TRAINER`; từng buổi cụ thể được lưu ở `class_sessions`.
10. **Thời gian:** Thời điểm trong DB là UTC (`timestamptz`); `occurrence_date` là ngày theo lịch Việt Nam. Trình bày giờ cho người dùng theo `Asia/Ho_Chi_Minh`.
11. **Cấu hình:** Đọc `system_configs`; giá trị chi nhánh ghi đè giá trị toàn chuỗi. Không viết cứng giá trị có dấu `*`.

**Mốc ngừng đưa vào lớp** trong tài liệu là `PROMOTION_CUTOFF_MIN` (mặc định 30 phút), chỉ chặn việc tự động đưa người từ danh sách chờ vào lớp. Quyền đặt mới trực tiếp dưới mốc này chưa chốt (câu 14 mục 7).

### 4.1. Tham số cấu hình F8

Các số trong kịch bản kiểm thử dùng bộ giá trị mặc định bên dưới; khi triển khai phải đọc cấu hình có hiệu lực, không viết cứng các giá trị này.

| Key | Giá trị mặc định | Nơi quy định | Ý nghĩa |
| --- | ---: | --- | --- |
| `BOOKING_WINDOW_DAYS` | 7 | ERD DBML, `system_configs.config_key` | Đặt chỗ trước bao nhiêu ngày |
| `PROMOTION_CUTOFF_MIN` | 30 | Bản chốt phạm vi, ERD, ADR 0003 | Không tự động đưa vào lớp khi còn dưới ngưỡng này |
| `PROMOTION_GRACE_MIN` | 15 | Bản chốt phạm vi, ERD, ADR 0003 | Ân hạn hủy không phạt sau khi đưa vào lớp |
| `RECONFIRM_TTL_MIN` | 60 | Bản chốt phạm vi, ERD DBML | Thời hạn xác nhận lại khi đổi giờ |
| `LATE_CANCEL_HOURS` | 2 | Bản chốt phạm vi, ERD DBML | Hủy trước giờ ít nhất ngưỡng này thì không phạt |
| `PENALTY_LIMIT_PER_MONTH` | 3 | Bản chốt phạm vi, ERD DBML | Số hủy muộn/vắng mặt trong tháng trước khi khóa |
| `PENALTY_LOCK_DAYS` | 7 | Bản chốt phạm vi, ERD DBML | Thời lượng khóa đặt lớp |

`system_configs.config_value` lưu dạng chuỗi; mã xử lý cần chuyển kiểu và kiểm tra hợp lệ. ERD cho phép một cấu hình toàn chuỗi (`branch_id IS NULL`) và giá trị theo chi nhánh ghi đè. Quy tắc xử lý cấu hình thiếu/sai định dạng hoặc hai giá trị cùng hiệu lực chưa được quy định trong SRS; cần thống nhất với người triển khai backend F8 trong Sprint 2.

---

## 5. Bảng dữ liệu liên quan

| Bảng / Đối tượng | Nhóm | Vai trò trong F8 |
| --- | --- | --- |
| `class_types` | Nhóm 4 | Danh mục loại lớp |
| `class_schedules` | Nhóm 4 | Lịch lặp hằng tuần |
| `class_sessions` | Nhóm 4 | Từng buổi cụ thể được sinh từ lịch lặp |
| `class_bookings` | Nhóm 4 | Chỗ đặt, danh sách chờ, xác nhận lại và điểm danh |
| `members` | Nhóm 3 | Hồ sơ hội viên, `booking_locked_until`, `home_branch_id` |
| `memberships` | Nhóm 3 | Xác nhận gói đang có hiệu lực |
| `packages` | Nhóm 3 | Loại và phạm vi sử dụng của gói |
| `branches` | Nhóm 2 | Giờ mở cửa và phạm vi chi nhánh |
| `rooms` | Nhóm 2 | Phòng và sức chứa tối đa |
| `staff` | Nhóm 7 | Huấn luyện viên, chi nhánh và phân công |
| `users` | Nhóm 1 | Người thao tác và vai trò `TRAINER`/`RECEPTIONIST` |
| `system_configs` | Nhóm 1 | Các tham số có dấu `*` |
| `notifications` | Nhóm 1 | F10 thông báo trong web/email, lịch gửi và trạng thái gửi |

---

## 6. Tích hợp với chức năng liên quan

- **F3 – Gói tập:** F8 chỉ đọc trạng thái và loại gói; không kích hoạt hay gia hạn gói.
- **F4 – Check-in tại quầy:** F4 có thể ghi `class_bookings.arrived_at` cho lớp sắp diễn ra. Không suy diễn `arrived_at` thành `ATTENDED`.
- **F9 – Huấn luyện viên:** Huấn luyện viên xem lịch lớp nhóm cùng lịch PT theo sơ đồ F8/F9; không cho trùng lịch huấn luyện viên với lớp nhóm theo quy tắc không trùng của bản chốt phạm vi/NF6.
- **F10 – Thông báo:** F8 tạo yêu cầu thông báo cho đưa vào lớp, đổi giờ, hủy buổi; F10 phụ trách gửi thông báo, thử lại và giờ gửi email. Email đưa vào lớp phát sinh ban đêm được gửi khi tới giờ mở cửa chi nhánh; ân hạn bắt đầu lúc gửi email.
- **F18 – Đa chi nhánh:** V2 vận hành một chi nhánh mặc định, nhưng bảng F8 đã chứa `branch_id`; logic truy vấn/phân quyền phải theo chi nhánh được xác thực. Không cho người gọi đổi `branchId` để vượt quyền.

---

## 7. Các câu hỏi cần chốt trước khi triển khai

Các điểm dưới đây chưa được chốt trong bản phạm vi, ERD hoặc ADR; không phải quy tắc mới. Trưởng nhóm là **Hoàng Kim Tiến (`Ktin06`)**. Người triển khai backend F8 được phân công khi lập kế hoạch Sprint 2, chưa tự gán cho một thành viên trong đặc tả này.

1. **Người quyết định: trưởng nhóm Hoàng Kim Tiến.** Cửa sổ `BOOKING_WINDOW_DAYS=7` tính từ hiện tại theo 168 giờ hay tính theo ngày lịch; biên chính xác có bao gồm thời điểm/ngày thứ bảy không.
2. **Người quyết định: trưởng nhóm Hoàng Kim Tiến.** FIFO nếu hai `waitlisted_at` bằng nhau: cần phân định thứ tự khi trùng thời điểm ổn định (ví dụ ID) hay quy tắc khác.
3. **Người quyết định: trưởng nhóm Hoàng Kim Tiến.** Kỳ “trong tháng” của vi phạm là tháng dương lịch theo giờ Việt Nam hay khoảng trượt; hành vi khi đạt ngưỡng có cần reset gì ngoài `booking_locked_until` không.
4. **Người quyết định: trưởng nhóm Hoàng Kim Tiến.** Ân hạn đến đúng `grace_until` có còn không phạt; cách xử lý nếu email không gửi được hoặc hội viên không có tài khoản/email.
5. **Người quyết định: trưởng nhóm Hoàng Kim Tiến.** Hủy chỗ đặt `WAITLIST` có tính là hủy muộn hay không; nguồn hiện không nói.
6. **Người quyết định: trưởng nhóm Hoàng Kim Tiến.** Nếu `RECONFIRM_TTL_MIN` kết thúc sau giờ bắt đầu lớp thì giữ hay hủy chỗ đặt tại thời điểm nào.
7. **Người quyết định: trưởng nhóm Hoàng Kim Tiến.** Khi đổi giờ, cần kiểm tra giờ mới gây trùng với chỗ đặt khác của hội viên không; xử lý chỗ đặt đang `WAITLIST` ra sao.
8. **Trưởng nhóm Hoàng Kim Tiến** chốt số ngày sinh buổi trước, tác động sửa/ngừng lịch và ngày lễ; **người triển khai backend F8 trong Sprint 2** chốt lịch chạy và cơ chế thử lại theo quyết định nghiệp vụ. (UC-F8.8).
9. **Người quyết định: trưởng nhóm Hoàng Kim Tiến.** Xung đột phòng/huấn luyện viên giữa hai lịch lặp; giảm sức chứa khi buổi học đã có chỗ đặt; kết thúc buổi/lịch điểm danh được mở trong khoảng thời gian nào.
10. **Người quyết định: trưởng nhóm Hoàng Kim Tiến.** Phân quyền cụ thể cho quản lý sửa buổi học riêng lẻ (đổi phòng/giờ/hủy buổi) và ánh xạ `cancel_reason` cho từ chối/hết hạn xác nhận lại.
11. **Người quyết định: trưởng nhóm Hoàng Kim Tiến.** Cách ghi `class_bookings.created_by` khi hội viên tự đặt; ERD chỉ ghi chú lễ tân khi thao tác hộ.
12. **Trưởng nhóm Hoàng Kim Tiến** chốt dữ liệu lịch công khai và nghiệp vụ điểm danh cả danh sách; **người triển khai backend F8 trong Sprint 2** chốt API/DTO, mã HTTP và tính nguyên tử giao dịch. Cần xác nhận có hiển thị số chỗ trống trên lịch công khai hay không.

13. **Người quyết định: trưởng nhóm Hoàng Kim Tiến; đang chờ xác nhận.** Người trong danh sách chờ từng bị bỏ qua vì hết gói/đang khóa, nay đủ điều kiện lại trong khi lớp còn chỗ: phải xét lại người chờ trước hay cho người mới đặt trực tiếp? Chưa chọn kết quả cho nhánh ngoại lệ này trong UC-F8.1/UC-F8.2.
14. **Người quyết định: trưởng nhóm Hoàng Kim Tiến; đang chờ xác nhận.** Khi còn dưới `PROMOTION_CUTOFF_MIN` phút trước giờ học và lớp còn chỗ: người mới có được đặt trực tiếp hay không? Quy tắc ngừng tự động đưa vào lớp từ danh sách chờ vẫn giữ theo ADR 0003, chưa suy ra câu trả lời về đặt trực tiếp.

---

## 8. Truy vết nguồn

| Nội dung | Nguồn |
| --- | --- |
| Phạm vi chức năng, tác nhân, điều kiện gói, cửa sổ đặt chỗ, hủy chỗ, đổi giờ, vi phạm, thông báo | [Bản chốt phạm vi](../ban-chot-pham-vi.md), mục F8 |
| FIFO, tự động đưa vào lớp, mốc ngừng đưa vào lớp, ân hạn, chỗ trống ban đêm, thông báo, khóa chỗ | [ADR 0003](../../decisions/0003-danh-sach-cho-tu-dong-dua-vao-lop.md) |
| Tập giá trị Enum khái niệm, các ràng buộc duy nhất, trạng thái giữ chỗ, khóa NF6, khóa cấu hình | [ERD v2](../../design/erd/erd-v2.dbml), [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md) |
| Mã Use Case, luồng thay thế/ngoại lệ, mã HTTP, Given/When/Then, cấu trúc A–H | [Mẫu đặc tả](mau-dac-ta-use-case.md) |
| Lịch công khai; hội viên đặt/hủy chỗ, vào danh sách chờ; huấn luyện viên xem lịch/điểm danh; quản lý lịch lặp | Sơ đồ draw.io hiện có trong `docs/design/use-case/` |

### 8.1. Truy vết user story đã xuất bản

Các issue dưới đây là yêu cầu, không phải bằng chứng đã triển khai. Không phụ thuộc file chưa merge của PR backlog #77.

| Story issue | Nội dung | Use case |
| --- | --- | --- |
| [#64](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/64) | Lịch tuần và sinh buổi | UC-F8.7, UC-F8.8 |
| [#65](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/65) | Đặt chỗ/vào chờ | UC-F8.1 |
| [#66](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/66) | Hủy chỗ/vi phạm | UC-F8.3, UC-F8.6 |
| [#67](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/67) | Đưa vào lớp theo FIFO | UC-F8.2 |
| [#68](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/68) | Đổi phòng/giờ, hủy buổi | UC-F8.4, UC-F8.7 |
| [#69](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/69) | Lịch dạy và điểm danh | UC-F8.5, UC-F8.6 |
| [#70](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/70) | Lịch công khai | UC-F8.0 |

### 8.2. Lịch sử chỉnh sửa

| Ngày | Nội dung |
| --- | --- |
| 03/11/2026 | Soạn bản nháp F8 theo phạm vi, ERD và ADR 0003. |
| 04/11/2026 | Việt hóa diễn đạt; ghi người quyết định; rút chỉ mục dữ liệu theo ERD; đơn giản UC-F8.1 trong phạm vi đã rõ; sửa tham chiếu và thứ tự kịch bản. Câu 13–14 và các câu nghiệp vụ còn mở chờ trưởng nhóm chốt; thiết kế API/khóa giao dịch chờ người triển khai backend F8 Sprint 2. |
