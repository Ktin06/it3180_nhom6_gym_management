# Đặc tả Use Case – F8: Lớp học nhóm

- **Mã chức năng:** F8
- **Version:** v2 (Bắt buộc)
- **Người biên soạn:** Phạm Gia Bảo
- **Cập nhật:** 10/11/2026 — áp dụng quyết định ngày 09/11 của trưởng nhóm về cách tính lại vi phạm khi sửa điểm danh, sau phần làm rõ sức chứa và sửa điểm danh ngày 07/11; xem mục 7.
- **Người duyệt chéo:** Nguyễn Anh Tú
- **Truy vết:** [#9](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/9) · WBS 1.2.9 · bản nháp chờ review và đồng bộ tài liệu chuẩn ở mục 7.6.
- **Tài liệu chuẩn tham chiếu:**
  - [Bản chốt phạm vi](../ban-chot-pham-vi.md), mục “Lớp học nhóm (F8)” và NF6
  - [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml)
  - [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md)
  - [Quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md)
  - [ADR 0003: Danh sách chờ tự động đưa vào lớp](../../decisions/0003-danh-sach-cho-tu-dong-dua-vao-lop.md)
  - [ADR 0009: Bộ sơ đồ phân tích và thiết kế](../../decisions/0009-bo-so-do-phan-tich-thiet-ke.md)
  - [Mẫu đặc tả Use Case](mau-dac-ta-use-case.md)

> **Nguồn quyết định:** [Trưởng nhóm Hoàng Kim Tiến chốt 04/11/2026](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6633040706) và [làm rõ quyết định 13, sửa điểm danh ngày 07/11/2026](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6701988493) và [cách tính lại vi phạm khi sửa điểm danh ngày 09/11/2026](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6734172961). Mục 7 ghi các quyết định; các luồng bên dưới đã cập nhật theo đó. Những ghi chú cũ về mốc ân hạn tính từ email trong ADR/ERD cần được người phụ trách nguồn chuẩn đồng bộ, không dùng để đảo ngược quyết định mới (mục 7.6).

> **Trạng thái:** Đặc tả này diễn giải yêu cầu trong bản chốt phạm vi, ERD và ADR 0003 cùng quyết định của trưởng nhóm. Các API, DTO, cách phân loại lỗi cụ thể trong mục G là **đề xuất thiết kế để người triển khai backend F8 trong Sprint 2 xác nhận**, chưa phải hợp đồng API đã thống nhất. Thiết kế kỹ thuật và việc đồng bộ nguồn chuẩn được ghi ở mục 7.5–7.6; không tự đặt thêm quy tắc.

---

## 1. Tổng quan chức năng

F8 quản lý lịch lớp nhóm lặp hằng tuần và từng buổi học được sinh từ lịch đó; cho khách xem lịch công khai; cho hội viên đặt chỗ, hủy chỗ và tham gia danh sách chờ; tự động đưa hội viên đủ điều kiện từ danh sách chờ vào chỗ trống; cho hội viên xác nhận lại khi đổi giờ; cho huấn luyện viên điểm danh; và cho quản lý chi nhánh quản lý lịch lặp cùng phân công huấn luyện viên.

Chỉ hội viên có gói `TIME` đang `ACTIVE` và sử dụng được tại chi nhánh của buổi học mới được đặt chỗ hoặc vào danh sách chờ. Lễ tân có thể thao tác hộ hội viên nhưng không được ưu tiên người đó. Thứ tự danh sách chờ là FIFO theo `waitlisted_at`, trùng thời điểm thì `id` nhỏ hơn đứng trước. Việc đặt chỗ phải giữ đúng sĩ số và không tạo lịch trùng cho một hội viên ngay cả khi có yêu cầu đồng thời (NF6).

**Vai trò trong phạm vi F8:**

- Khách: xem lịch lớp công khai; không cần đăng nhập.
- Hội viên: xem lịch, đặt chỗ, vào danh sách chờ, hủy chỗ, xác nhận hoặc từ chối giờ học mới.
- Lễ tân (`RECEPTIONIST`): thao tác hộ hội viên theo cùng điều kiện và thứ tự như hội viên.
- Huấn luyện viên (`TRAINER`): xem lịch dạy lớp nhóm và điểm danh học viên lớp được phân công.
- Quản lý chi nhánh (`BRANCH_MANAGER`): quản lý loại lớp, lịch lặp, buổi học, phân công huấn luyện viên trong chi nhánh và sửa điểm danh sau thời hạn huấn luyện viên được sửa.
- Chủ chuỗi (`SUPER_ADMIN`): đổi phòng/giờ hoặc hủy buổi riêng lẻ ở mọi chi nhánh; không tự mở rộng quyền quản lý lịch lặp hoặc điểm danh ngoài quyết định đã chốt.
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
| **UC-F8.6** | Điểm danh học viên lớp học nhóm | Huấn luyện viên; quản lý chi nhánh sửa sau hạn | Cao | Sprint 2 |
| **UC-F8.7** | Quản lý lịch lặp lớp học và phân công huấn luyện viên | Quản lý chi nhánh; chủ chuỗi chỉ với buổi riêng lẻ | Cao | Sprint 2 |
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
- **Thiết kế còn mở:** Nội dung lịch công khai, bộ lọc và dữ liệu chỗ trống do người triển khai backend F8 trong Sprint 2 chốt theo mục 7.5. `BOOKING_WINDOW_DAYS` chỉ giới hạn đặt chỗ, không tự áp cho xem lịch.

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
- Các trường công khai cụ thể và việc hiển thị sức chứa/chỗ trống do người triển khai backend F8 trong Sprint 2 chốt theo chỉ đạo tại mục 7.5 trước khi coi là hợp đồng API.

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

#### A. Tiền điều kiện

- Hội viên đăng nhập hợp lệ; hoặc lễ tân đăng nhập và chọn hồ sơ hội viên để thao tác hộ.
- Buổi học tồn tại, chưa bị hủy/kết thúc và nằm trong cửa sổ đặt chỗ.
- Hội viên có gói `TIME` đang `ACTIVE`, dùng được tại chi nhánh của buổi học.
- Hội viên không bị khóa đặt lớp tại thời điểm yêu cầu.

#### B. Hậu điều kiện

- **Thành công:** Một dòng `class_bookings` được tạo cho hội viên/buổi. Nếu còn sức chứa, trạng thái là `BOOKED`; nếu đã đủ sĩ số, trạng thái là `WAITLIST` và `waitlisted_at` được ghi để xác định thứ tự. `created_by` ghi `users.id` của tài khoản đã bấm đặt (hội viên hoặc lễ tân); giữ nguyên khi tự động đưa vào lớp.
- **Thất bại:** Không tạo chỗ đặt và không thay đổi sĩ số/danh sách chờ. Giao dịch bị hủy nếu có lỗi hoặc xung đột đồng thời.

#### C. Luồng sự kiện chính

1. Hội viên chọn một buổi lớp và bấm đặt chỗ; hoặc lễ tân chọn buổi và hồ sơ hội viên cần thao tác hộ.
2. Hệ thống kiểm tra hợp lệ tham số nhận dạng buổi/hội viên.
3. Hệ thống xác thực vai trò và kiểm tra hội viên chỉ đặt cho chính mình; lễ tân chỉ thao tác hộ trong phạm vi được phân quyền (S3).
4. Hệ thống bắt đầu giao dịch với kế hoạch khóa theo NF6 cho hội viên yêu cầu và các ứng viên danh sách chờ; khóa `members` trước `class_sessions`. Người triển khai backend F8 trong Sprint 2 chốt cách phối hợp với UC-F8.2; không lấy khóa hội viên bổ sung sau khóa buổi học.
5. Hệ thống xác nhận buổi có thể đặt: ngày học theo `Asia/Ho_Chi_Minh` thuộc từ hôm nay đến hết hôm nay + `BOOKING_WINDOW_DAYS` ngày (hai đầu bao gồm), đồng thời còn ít nhất `PROMOTION_CUTOFF_MIN` phút trước giờ học. Dưới mốc này đóng cả đặt mới và vào danh sách chờ, kể cả còn chỗ.
6. Hệ thống xác nhận hội viên có gói `TIME` `ACTIVE` dùng được tại chi nhánh của lớp, không bị khóa đặt lớp, chưa có chỗ đặt đang hoạt động cho cùng buổi, và không vi phạm quy tắc không đăng ký các buổi trùng giờ theo chính sách đã được trưởng nhóm Hoàng Kim Tiến chốt.
7. Hệ thống đếm các chỗ đang giữ: `BOOKED`, `PENDING_RECONFIRM`, `ATTENDED`, `NO_SHOW`.
8. Nếu còn chỗ và có danh sách chờ, hệ thống dùng lại UC-F8.2 để xét người đủ điều kiện theo FIFO trước, gồm người từng bị bỏ qua nay đủ điều kiện lại. Việc này nằm trong cùng giao dịch trước khi cấp chỗ cho người mới; không chép thuật toán xét chờ vào UC này.
9. Sau bước 8, nếu còn chỗ, hệ thống tạo `class_bookings.status = 'BOOKED'`, ghi `booked_at` và `created_by` của tài khoản thao tác.
10. Nếu lớp đã đầy, hệ thống tạo `class_bookings.status = 'WAITLIST'`, ghi `waitlisted_at`; không ghi vị trí thứ tự cố định.
11. Hệ thống ghi nhận giao dịch và trả kết quả. Khi lớp đầy, hội viên được thông báo mình đang ở danh sách chờ.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Lớp còn chỗ, không có người đang chờ:** Tại bước 9, hệ thống tạo `BOOKED`.
- **A2 – Lớp đầy:** Tại bước 10, hệ thống tạo `WAITLIST`; đây là kết quả thành công, không phải lỗi. Vị trí chờ được tính khi truy vấn theo `waitlisted_at ASC, id ASC`.
- **E1 – Yêu cầu sai định dạng (400 Bad Request):** ID hoặc dữ liệu yêu cầu sai định dạng; trả `ProblemDetail` có `errors` theo trường.
- **E2 – Không có quyền (403 Forbidden):** Tài khoản không có vai trò hợp lệ, hội viên yêu cầu đặt cho người khác, hoặc nhân viên truy cập ngoài phạm vi chi nhánh được phân quyền.
- **E3 – Không tìm thấy buổi/hội viên (404 Not Found):** Tài nguyên không tồn tại.
- **E4 – Đã có chỗ đặt đang hoạt động (409 Conflict):** Hội viên đã có dòng cho cùng buổi với trạng thái khác `CANCELLED`; trả thông báo “Hội viên đã có chỗ đặt hoặc đang trong danh sách chờ của buổi này.”
- **E5 – Xung đột đồng thời (409 Conflict):** Khóa/ràng buộc duy nhất phát hiện cạnh tranh; hủy giao dịch và yêu cầu tải lại hoặc thử lại. Không để vượt sĩ số.
- **E6 – Vi phạm điều kiện đặt (422 Unprocessable Content):** Không có gói `TIME` phù hợp, gói không `ACTIVE`/không dùng tại chi nhánh, bị khóa đặt lớp, ngoài cửa sổ đặt chỗ, còn dưới `PROMOTION_CUTOFF_MIN` phút, hoặc buổi không nhận đặt; nêu lý do cụ thể bằng tiếng Việt.
- **E7 – Trùng giờ với đăng ký đang chiếm chỗ (409 Conflict):** Hội viên đã có chỗ ở buổi khác có thời gian giao nhau; trả `ProblemDetail` nêu lớp bị xung đột. Người đang chờ giữ thứ tự; khi xét đưa vào lớp phải kiểm tra lại lịch trùng theo UC-F8.2.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.1-01:** Chỉ gói `TIME` `ACTIVE` dùng được tại chi nhánh; gói `SESSION` và `PT` không dùng đặt lớp. Bản chốt phạm vi, mục F8; ERD `class_bookings.status`.
- **BR-F8.1-02:** Cửa sổ `BOOKING_WINDOW_DAYS` (mặc định 7) tính theo ngày lịch Việt Nam, từ hôm nay đến hết ngày hôm nay + số ngày cấu hình. Còn dưới `PROMOTION_CUTOFF_MIN` phút thì đóng mọi đặt mới và vào danh sách chờ (quyết định 1, 4 mục 7.1).
- **BR-F8.1-03:** Số chỗ giữ không vượt `class_sessions.capacity`; các trạng thái giữ chỗ là `BOOKED`, `PENDING_RECONFIRM`, `ATTENDED`, `NO_SHOW`; `WAITLIST` và `CANCELLED` không chiếm chỗ.
- **BR-F8.1-04:** Bản chốt phạm vi cấm hội viên đăng ký hai buổi trùng giờ; một chỗ đặt đã `CANCELLED` không còn ngăn đặt lại. Người thao tác hộ không được nhảy hàng hoặc đặt ngoài thứ tự danh sách chờ.
- **BR-F8.1-05:** Không giới hạn số danh sách chờ một hội viên tham gia.
- **BR-F8.1-06:** Khi có chỗ trống phải xét danh sách chờ đủ điều kiện trước người mới, gồm người từng bị bỏ qua nay đủ điều kiện lại. Dùng lại UC-F8.2 trong giao dịch; không cho lễ tân chỉ định người phía sau để vượt hàng (quyết định 3 mục 7.1).
- **NF6:** Thực hiện trong giao dịch; khóa dòng `members` trước `class_sessions`; kiểm tra trùng giờ và sĩ số sau khi có khóa. Ràng buộc duy nhất đang hoạt động: `(class_session_id, member_id)` với `status <> 'CANCELLED'`.
- **Quyết định đã chốt:** FIFO thêm `id ASC` khi trùng `waitlisted_at`; `created_by` luôn ghi người bấm đặt. Hai quy tắc trước đây còn mở đã được chốt ở quyết định 3–4 mục 7.1; không còn nhánh chờ quyết định cho hai trường hợp đó. Thiết kế khóa giao dịch vẫn do backend F8 Sprint 2 chốt (mục 7.5).

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
  - **Given** Hội viên có gói `TIME` `ACTIVE` dùng tại chi nhánh, chưa bị khóa; buổi còn chỗ, còn ít nhất `PROMOTION_CUTOFF_MIN` phút trước giờ học và người chờ đủ điều kiện đã được UC-F8.2 xét trước.
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
  - **Given** Còn đúng một chỗ sau khi xét danh sách chờ và hai hội viên hợp lệ gửi yêu cầu đồng thời.
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
  - **Then** UC-F8.2 đưa người đầu danh sách đủ điều kiện vào lớp trong giao dịch giải phóng chỗ; lễ tân không được chọn người khác thay thế. Người từng bị bỏ qua nay đủ điều kiện lại cũng được xét trước người mới.
- **Kịch bản 8: Hai yêu cầu đồng thời của cùng hội viên vào hai buổi trùng giờ**
  - **Given** Cùng hội viên đồng thời gửi hai yêu cầu cho hai buổi học trùng giờ, mỗi buổi học còn chỗ.
  - **When** Hai yêu cầu được xử lý đồng thời.
  - **Then** Tối đa một yêu cầu tạo `BOOKED`; yêu cầu còn lại trả `409 Conflict`; không tạo hai chỗ đặt chiếm chỗ chồng giờ.

- **Kịch bản 9: Cửa sổ theo ngày lịch Việt Nam**
  - **Given** `BOOKING_WINDOW_DAYS=7`, hôm nay 04/11/2026 lúc 10:00 giờ Việt Nam; buổi ngày 11/11 lúc 18:00 hợp lệ và còn chỗ.
  - **When** Hội viên đủ điều kiện đặt buổi đó.
  - **Then** Cho đặt sau khi xét danh sách chờ, dù thời gian đến buổi học dài hơn 168 giờ; buổi ngày 12/11 bị từ chối vì ngoài cửa sổ.

- **Kịch bản 10: Người chờ đủ điều kiện lại được xét trước**
  - **Given** Một người có `WAITLIST` từng bị bỏ qua, nay có gói hợp lệ; lớp còn một chỗ và còn trên mốc đóng đặt.
  - **When** Người mới bấm đặt.
  - **Then** UC-F8.2 xét người chờ trước; nếu người đó được `BOOKED` thì người mới vào `WAITLIST`, không vượt FIFO.

- **Kịch bản 11: Đóng mọi đặt mới và vào chờ dưới mốc**
  - **Given** Còn 29 phút với `PROMOTION_CUTOFF_MIN=30`; xét cả buổi còn chỗ và buổi đã đầy.
  - **When** Hội viên hoặc lễ tân gửi đặt mới/vào danh sách chờ.
  - **Then** Từ chối theo điều kiện đặt, không tạo `BOOKED` hoặc `WAITLIST` trong cả hai trường hợp.

- **Kịch bản 12: Đúng mốc đóng đặt và ghi người tạo**
  - **Given** Còn đúng 30 phút, không có người chờ đủ điều kiện đứng trước, hội viên hoặc lễ tân đủ quyền thao tác.
  - **When** Tài khoản gửi đặt.
  - **Then** Vẫn xét được yêu cầu theo sức chứa; `created_by` là `users.id` của tài khoản đã bấm đặt, không phải mã hội viên được thao tác hộ.

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

- **Thành công:** Các chỗ đặt đủ điều kiện theo FIFO được chuyển thành `BOOKED`; ghi `promoted_at`. `grace_until` bằng thời điểm tạo thông báo trong web cộng `PROMOTION_GRACE_MIN` (mặc định 15 phút); thông báo trong web được tạo ngay, email được gửi khi chi nhánh mở cửa nếu việc đưa vào lớp diễn ra ban đêm. Số chỗ giữ không vượt sức chứa.
- **Thất bại:** Nếu không tìm thấy ứng viên đủ điều kiện thì các chỗ đặt chờ tiếp tục giữ thứ tự; chỗ có thể còn trống. Nếu đã qua mốc ngừng đưa vào lớp thì không đưa vào lớp.

**Thiết kế giao dịch còn chờ:** Người triển khai backend F8 trong Sprint 2 phải chốt kế hoạch lấy khóa cho toàn bộ thao tác giải phóng chỗ và xét danh sách chờ ngay từ đầu giao dịch, tuân NF6. Không lấy thêm khóa `members` sau khi đã khóa `class_sessions`.

#### C. Luồng sự kiện chính

1. Khi một thao tác giải phóng chỗ, hệ thống thực hiện UC-F8.2 trong cùng giao dịch đó. Trước khi nhận đặt mới lúc còn chỗ cũng gọi lại UC này để xét người từng bị bỏ qua nay đủ điều kiện lại (quyết định 3).
2. Hệ thống khóa các dòng liên quan theo thứ tự NF6: dòng `members` ứng viên trước, sau đó dòng `class_sessions`; khi xử lý nhiều ứng viên, thứ tự khóa phải nhất quán để hạn chế khóa chéo.
3. Hệ thống kiểm tra buổi chưa bị hủy/kết thúc, còn chỗ theo `capacity` và thời gian đến lớp không ở dưới `PROMOTION_CUTOFF_MIN`.
4. Sau khi khóa buổi học, hệ thống đọc lại các dòng `class_bookings.status = 'WAITLIST'` theo `waitlisted_at` tăng dần, trùng thì `id` tăng dần; vị trí không được lưu cứng. Chỉ xét ứng viên có dòng `members` đã khóa trước `class_sessions`. Nếu có ứng viên cần xét chưa được khóa từ đầu, phải hủy và xử lý lại toàn bộ giao dịch với tập khóa phù hợp; không lấy khóa hội viên bổ sung sau khóa buổi học. Người triển khai backend F8 trong Sprint 2 chốt cơ chế thử lại ở giao dịch mới, không thử lại trong giao dịch đã bị hủy.
5. Với từng ứng viên theo thứ tự, hệ thống kiểm tra gói `TIME` `ACTIVE` dùng tại chi nhánh, khóa đặt lớp, lịch trùng giờ và điều kiện khác đã được bản chốt xác định.
6. Nếu ứng viên không còn đủ điều kiện, hệ thống bỏ qua và xét người tiếp theo; không cho phép nhân viên chọn một người phía sau để ưu tiên.
7. Với ứng viên đủ điều kiện, hệ thống chuyển sang `BOOKED`, ghi `promoted_at`, giữ nguyên `created_by` ban đầu và tạo thông báo trong web. Mốc tạo thông báo trong web là đầu ân hạn; `grace_until` bằng mốc đó cộng `PROMOTION_GRACE_MIN`, không phụ thuộc email có gửi được hay không.
8. Hệ thống tiếp tục cho các chỗ trống còn lại đến khi đủ sức chứa, hết ứng viên đủ điều kiện hoặc chạm mốc ngừng đưa vào lớp.
9. Hệ thống ghi nhận giao dịch và F10 hiển thị thông báo trong web. Nếu ngoài giờ mở cửa, email gửi khi chi nhánh mở cửa; email chậm/lỗi hoặc không có email không dời mốc ân hạn đã tính từ thông báo trong web.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Người đầu hàng không còn đủ điều kiện:** Tại bước 5, giữ nguyên thứ tự tương đối của hàng chờ và xét ứng viên tiếp theo. Các điều kiện nêu rõ trong bản chốt phạm vi/ADR: gói hết hạn/không còn dùng được, đang bị khóa đặt, đã có lịch trùng giờ.
- **A2 – chỗ trống phát sinh ban đêm:** Đưa vào lớp vẫn chạy ngay; thông báo web hiện ngay; email gửi vào giờ mở cửa của chi nhánh; ân hạn tính từ lúc tạo thông báo trong web.
- **A3 – Đã dưới 30 phút:** Hệ thống không đổi bất kỳ dòng `WAITLIST` nào; chỗ trống được để trống.
- **A4 – Nhiều chỗ trống:** Hệ thống lần lượt đưa vào lớp đủ người hợp lệ đến khi đạt sức chứa hoặc hết hàng chờ.
- **E1 – Xung đột đồng thời (409 Conflict nội bộ/ghi nhật ký):** Một giao dịch khác đã đổi trạng thái/sức chứa; hủy giao dịch hiện tại và tính lại trạng thái từ dữ liệu mới. Không gửi thông báo trùng cho một lần đưa vào lớp đã được ghi nhận.
- **E2 – Lỗi F10:** Không đảo ngược trạng thái đặt chỗ đã ghi nhận chỉ vì dịch vụ gửi email tạm lỗi; thông báo phải được giữ lại để thử lại theo cơ chế F10. Người triển khai backend F8 trong Sprint 2 phối hợp với người triển khai F10 chốt cơ chế thử lại và chống gửi lặp. Email lỗi không làm thay đổi ân hạn tính từ thông báo trong web (quyết định 7).

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.2-01:** FIFO theo `waitlisted_at ASC, id ASC`; vị trí tính khi truy vấn, không lưu số thứ tự cố định (quyết định 2 mục 7.1).
- **BR-F8.2-02:** Tự động đưa vào lớp ngay, không cần xác nhận; người đủ điều kiện có ân hạn không phạt. Người không đủ điều kiện bị bỏ qua, xét tiếp người sau.
- **BR-F8.2-03:** Không đưa vào lớp khi còn dưới `PROMOTION_CUTOFF_MIN=30` phút; đúng 30 phút chưa phải “dưới 30 phút” theo câu chữ bản chốt phạm vi, cần giữ phép so sánh `<`.
- **BR-F8.2-04:** Ân hạn `PROMOTION_GRACE_MIN` (mặc định 15 phút) tính từ thời điểm tạo thông báo trong web, hủy tại đúng `grace_until` vẫn miễn phạt (`<=`). Email ban đêm vẫn đợi giờ mở cửa nhưng không quyết định mốc ân hạn (quyết định 7 mục 7.2; cần đồng bộ ghi chú ADR/ERD cũ).
- **BR-F8.2-05:** Số chỗ chiếm vẫn gồm `BOOKED`, `PENDING_RECONFIRM`, `ATTENDED`, `NO_SHOW`; loại trừ `WAITLIST`, `CANCELLED`.
- **NF6:** Giao dịch có khóa bản ghi, không vượt sức chứa; nhất quán thứ tự khóa `members` rồi `class_sessions`.
- **Thiết kế còn mở:** Người triển khai backend F8 Sprint 2 phối hợp F10 chốt ghi nhận thông báo trong web cùng giao dịch, thử lại/chống gửi lặp và trường hợp hồ sơ chưa có tài khoản; không thay mốc ân hạn đã được trưởng nhóm chốt. Xem mục 7.5–7.6.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_bookings`, `notifications`.
- **Bảng đọc:** `class_sessions`, `members`, `memberships`, `packages`, `system_configs`, `branches`, `users`.

- `class_bookings`: `status`, `waitlisted_at`, `promoted_at`, `grace_until`, `class_session_id`, `member_id`.
- `class_sessions`: `capacity`, `start_at`, `branch_id`, `status`.
- `members`: `booking_locked_until`.
- `memberships`: `package_type`, `branch_id`, `status`, `end_date`.
- `notifications`: `user_id`, `type`, `channel`, `status`, `created_at`, `scheduled_at`, `sent_at`, `related_entity`, `related_id`.
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
  - **Then** `BOOKED` được ghi ngay, thông báo trong web có ngay, email được hẹn đến giờ mở cửa; ân hạn 15 phút bắt đầu lúc tạo thông báo trong web.
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

- **Kịch bản 8: FIFO khi cùng thời điểm**
  - **Given** Hai người đủ điều kiện có cùng `waitlisted_at`, ID lần lượt 101 và 102; chỉ một chỗ còn trống, còn đủ thời gian.
  - **When** UC-F8.2 xét danh sách chờ.
  - **Then** Dòng ID 101 được đưa vào lớp trước; giữ `created_by` gốc của dòng đó.

- **Kịch bản 9: Email lỗi không dời ân hạn**
  - **Given** Thông báo trong web được tạo lúc 10:00, `PROMOTION_GRACE_MIN=15`, email gửi lỗi hoặc chưa được gửi.
  - **When** Hệ thống ghi nhận chỗ được đưa vào lớp.
  - **Then** `grace_until` là 10:15; không chờ email thành công để bắt đầu ân hạn.

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
5. Nếu trạng thái trước hủy là `BOOKED`, hệ thống xét hủy sớm/hủy muộn và ân hạn sau khi được tự động đưa vào lớp. Hủy `WAITLIST` không bao giờ là hủy muộn, không tính phạt; từ chối đổi giờ ở UC-F8.4 cũng không phạt.
6. Hệ thống trong giao dịch khóa dữ liệu liên quan để tránh hủy hai lần/đưa vào lớp vượt sĩ số.
7. Hệ thống đặt `status = 'CANCELLED'`, `cancelled_at = now()`, `cancel_reason` phù hợp; với hủy muộn đặt `is_late_cancel = true`.
8. Nếu hủy muộn, hệ thống đếm các vi phạm trong tháng dương lịch theo giờ Việt Nam và xảy ra sau lần bị khóa gần nhất. Đủ `PENALTY_LIMIT_PER_MONTH` thì khóa `PENALTY_LOCK_DAYS` ngày; không dùng lại các vi phạm đã dẫn đến lần khóa trước.
9. Nếu chỗ được giải phóng, hệ thống thực hiện UC-F8.2 trong cùng giao dịch; không đưa ai vào nếu đã qua mốc ngừng đưa vào lớp.
10. Hệ thống trả kết quả và thông báo kết quả hủy.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Hủy trước giờ học ít nhất 2 giờ:** Tại bước 5, nếu còn ít nhất `LATE_CANCEL_HOURS`, hệ thống đặt `is_late_cancel = false`; không tăng số lần vi phạm.
- **A2 – Hủy chỗ vừa được đưa vào lớp trong ân hạn:** Chỉ chỗ chuyển từ `WAITLIST` sang `BOOKED` có ân hạn `PROMOTION_GRACE_MIN` phút kể từ thông báo trong web. Hủy khi thời điểm hiện tại `<= grace_until` không phạt, dù email lỗi/chưa gửi hoặc không có email. Không áp dụng ân hạn này cho chỗ `BOOKED` đặt trực tiếp.
- **A3 – Rời danh sách chờ:** Chuyển `WAITLIST` sang `CANCELLED`, không bao giờ ghi hủy muộn hoặc tăng vi phạm, kể cả sát giờ; không giải phóng chỗ đang chiếm.
- **E1 – Yêu cầu sai định dạng (400 Bad Request):** Mã chỗ đặt sai định dạng; trả `ProblemDetail` có lỗi trường.
- **E2 – Không có quyền (403 Forbidden):** Hội viên thao tác chỗ đặt người khác hoặc nhân viên ngoài phạm vi quyền.
- **E3 – Không tìm thấy chỗ đặt (404 Not Found):** Mã chỗ đặt không tồn tại hoặc không hiển thị trong phạm vi dữ liệu người gọi.
- **E4 – Chỗ đặt đã kết thúc/hủy hoặc không thể hủy (422 Unprocessable Content):** Trả lý do bằng tiếng Việt; không đổi trạng thái lần nữa.
- **E5 – Xung đột đồng thời (409 Conflict):** Chỗ đặt đã bị hủy/đổi trạng thái ở yêu cầu khác; hủy giao dịch thao tác hiện tại.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.3-01:** Hủy trước giờ bắt đầu ít nhất 2 giờ không bị phạt; mốc cấu hình `LATE_CANCEL_HOURS=2`.
- **BR-F8.3-02:** Hủy muộn hoặc `NO_SHOW` đủ `PENALTY_LIMIT_PER_MONTH` (mặc định 3) lần thì khóa `PENALTY_LOCK_DAYS` (mặc định 7) ngày. Chỉ đếm vi phạm trong tháng dương lịch Việt Nam và sau lần bị khóa gần nhất; sang tháng mới đếm từ 0, hết khóa không dùng lại vi phạm cũ (quyết định 5–6 mục 7.2).
- **BR-F8.3-03:** Chỗ vừa được tự động đưa từ `WAITLIST` sang `BOOKED` có ân hạn tính từ thông báo trong web, bao gồm đúng `grace_until` (`<=`). Chỗ đặt trực tiếp không có ân hạn này; rời `WAITLIST` luôn không phạt (quyết định 7–8 mục 7.2).
- **BR-F8.3-04:** Hủy chỗ đặt mở chỗ để xử lý FIFO, nhưng không được tự động đưa vào lớp khi còn dưới 30 phút đến giờ bắt đầu.
- **Thiết kế còn mở:** Backend F8 Sprint 2 chốt cách lưu/đọc mốc lần khóa gần nhất và thời điểm vi phạm theo quyết định 5–6; không tự thêm cột hay suy ra mốc lịch sử chỉ từ giá trị cấu hình hiện tại (mục 7.5–7.6).

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_bookings`, `members` khi áp khóa đặt lớp.
- **Bảng đọc:** `class_sessions`, `system_configs`, `notifications` (xác định thời điểm tạo thông báo trong web), `users`.

- `class_bookings`: `status`, `cancelled_at`, `cancel_reason`, `is_late_cancel`, `grace_until`, `promoted_at`, `member_id`, `class_session_id`.
- `members`: `booking_locked_until`.
- `class_sessions`: `start_at`.
- `system_configs`: `config_key`, `config_value`.
- `notifications`: `user_id`, `channel`, `created_at`, `related_entity`, `related_id`.

Các cột lịch sử phục vụ đếm vi phạm theo tháng Việt Nam và sau lần khóa gần nhất; ERD chưa quy định đầy đủ cách lưu mốc khóa lịch sử, backend F8 Sprint 2 chốt thiết kế (mục 7.6).

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
  - **Given** Hội viên đã có 2 lần hủy muộn/no-show trong kỳ tháng đang áp dụng trong tháng dương lịch Việt Nam và sau lần khóa gần nhất.
  - **When** Hội viên hủy chỗ đặt muộn.
  - **Then** Lần hủy được ghi `is_late_cancel = true`, hội viên bị khóa đặt trong 7 ngày theo `PENALTY_LOCK_DAYS`, trả thời điểm hết khóa.
- **Kịch bản 3: Hủy chỗ vừa được đưa vào lớp trong ân hạn**
  - **Given** Hội viên vừa được đưa vào lớp và thời điểm hiện tại nằm trong khoảng ân hạn tính từ thông báo trong web đã tạo.
  - **When** Hội viên hủy chỗ đặt.
  - **Then** Chỗ đặt chuyển `CANCELLED`, không đánh dấu hủy muộn và không tăng số lần vi phạm.
- **Kịch bản 4: Hai yêu cầu hủy đồng thời**
  - **Given** Hai yêu cầu cùng hủy một chỗ đặt đang hoạt động.
  - **When** Hai yêu cầu được xử lý đồng thời.
  - **Then** Chỉ một lần chuyển trạng thái thành công; không mở hai chỗ hoặc tạo hai lần đưa vào lớp cho cùng một chỗ.

- **Kịch bản 5: Hủy đúng cuối ân hạn**
  - **Given** Chỗ vừa được đưa vào lớp, thông báo trong web tạo lúc 10:00, `grace_until` là 10:15.
  - **When** Hội viên hủy đúng 10:15.
  - **Then** Không tính hủy muộn hoặc tăng vi phạm vì phép so sánh bao gồm đúng mốc (`<=`).

- **Kịch bản 6: Rời danh sách chờ sát giờ**
  - **Given** Hội viên đang `WAITLIST`, buổi bắt đầu sau một phút.
  - **When** Hội viên hủy dòng chờ.
  - **Then** Chuyển `CANCELLED`, không ghi hủy muộn, không tăng vi phạm hoặc mở thêm chỗ chiếm.

- **Kịch bản 7: Đổi tháng Việt Nam đếm lại từ 0**
  - **Given** Các vi phạm cũ xảy ra trong tháng 10; thời điểm hiện tại thuộc tháng 11 theo giờ Việt Nam.
  - **When** Một vi phạm mới xảy ra trong tháng 11.
  - **Then** Chỉ đếm vi phạm của tháng 11; không cộng vi phạm tháng 10 để đạt ngưỡng.

- **Kịch bản 8: Hết khóa không phạt lại lỗi cũ**
  - **Given** Hội viên đã bị khóa do 3vi phạm, khóa 7ngày đã hết; vẫn trong cùng tháng.
  - **When** Có vi phạm đầu tiên sau lần khóa gần nhất.
  - **Then** Số đếm mới là 1; không dùng lại 3lỗi cũ để khóa tiếp và không phạt hai lần cho cùng một lỗi.

---

### 3.5. UC-F8.4: Xác nhận lại hoặc từ chối khi đổi giờ lớp

- **Mã UC:** UC-F8.4
- **Tên Use Case:** Xác nhận lại hoặc từ chối khi đổi giờ lớp
- **Tác nhân chính:** Hội viên (`MEMBER`); Hệ thống cho hạn xác nhận; Lễ tân có thể hỗ trợ theo quyền thao tác hộ
- **Tác nhân phụ (nếu có):** Quản lý chi nhánh (`BRANCH_MANAGER`), Chủ chuỗi (`SUPER_ADMIN`), F10
- **Mô tả ngắn gọn:** Khi quản lý đổi giờ một buổi, hội viên đang có chỗ xác nhận tham gia giờ mới trong thời hạn cấu hình hoặc từ chối. Hết hạn/từ chối thì mất chỗ, không bị phạt; chỗ được xét cho danh sách chờ.

#### A. Tiền điều kiện

- Buổi học tồn tại và quản lý đúng chi nhánh hoặc chủ chuỗi đã thay đổi giờ bắt đầu/kết thúc.
- Hội viên có chỗ đặt đã xác nhận trước khi đổi giờ.
- Chỗ đặt đang ở trạng thái `PENDING_RECONFIRM` và chưa hết `reconfirm_expires_at`.

#### B. Hậu điều kiện

- **Thành công – xác nhận:** Chỗ đặt chuyển về `BOOKED`; hội viên giữ chỗ theo thời gian mới.
- **Thành công – từ chối/hết hạn:** Chỗ đặt chuyển `CANCELLED`, không phạt; chỗ được giải phóng và UC-F8.2 được xét theo mốc ngừng đưa vào lớp.
- **Thất bại:** Không đổi trạng thái chỗ đặt; không mở chỗ nếu thao tác không ghi nhận giao dịch.

#### C. Luồng sự kiện chính

1. Quản lý chi nhánh của buổi hoặc chủ chuỗi thay đổi giờ `class_sessions`; lễ tân không được đổi phòng/giờ hoặc hủy buổi riêng lẻ.
2. Hệ thống lưu `original_start_at`, cập nhật `start_at`/`end_at`, đặt trạng thái buổi `RESCHEDULED`.
3. Hệ thống xác định các chỗ đặt `BOOKED` đang chiếm chỗ và chuyển sang `PENDING_RECONFIRM`.
4. Hệ thống đặt `reconfirm_expires_at = min(thời điểm đổi giờ + RECONFIRM_TTL_MIN, giờ bắt đầu mới)`, rồi phát thông báo đổi giờ qua F10.
5. Hội viên mở thông báo/lịch lớp và chọn xác nhận giờ mới hoặc không tham gia; lễ tân có thể thao tác thay theo quyền đã cấp.
6. Hệ thống kiểm tra quyền, chỗ đặt còn `PENDING_RECONFIRM` và chưa đến hạn. Khi hội viên bấm xác nhận, kiểm tra lịch trùng như đặt mới; nếu trùng thì từ chối và báo “hãy hủy buổi kia trước”, giữ trạng thái chờ xác nhận và hạn hiện tại.
7. Nếu xác nhận, hệ thống chuyển chỗ đặt về `BOOKED`; nếu từ chối, hệ thống chuyển chỗ đặt `CANCELLED` với lý do phù hợp, không phạt.
8. Khi chỗ được giải phóng, hệ thống thực hiện UC-F8.2 trong cùng giao dịch rồi ghi nhận giao dịch thay đổi; không đưa vào lớp sau mốc ngừng đưa vào lớp.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Hội viên không phản hồi trước hạn:** Tác vụ nền tìm `PENDING_RECONFIRM` đã tới hoặc qua `reconfirm_expires_at`, chuyển chỗ đặt sang `CANCELLED` và giải phóng chỗ; không tính phạt; xét danh sách chờ.
- **A2 – Hội viên báo không tham gia:** Hệ thống hủy chỗ đặt ngay, không phạt, rồi xét người trong danh sách chờ.
- **A3 – Chỉ đổi phòng:** Không chuyển chỗ đặt sang `PENDING_RECONFIRM`; chỉ thông báo thay đổi phòng theo quy tắc F8.
- **A4 – Chỉ đổi giờ:** Giữ nguyên chỗ hiện tại nhưng yêu cầu xác nhận lại trước hạn đã tính, tối đa `RECONFIRM_TTL_MIN` và không vượt giờ bắt đầu mới.
- **E1 – Yêu cầu sai định dạng (400 Bad Request):** Dữ liệu hoặc mã chỗ đặt không hợp lệ.
- **E2 – Không có quyền (403 Forbidden):** Người gọi không phải chủ chỗ đặt, lễ tân không có quyền thao tác hộ, hoặc quản lý không thuộc chi nhánh.
- **E3 – Không tìm thấy buổi/chỗ đặt (404 Not Found):** Tài nguyên không tồn tại trong phạm vi truy cập.
- **E4 – Hạn đã hết hoặc chỗ đặt không chờ xác nhận (422 Unprocessable Content):** Hệ thống từ chối cập nhật và trả trạng thái mới nhất.
- **E6 – Giờ mới trùng chỗ đã đặt (409 Conflict):** Khi bấm xác nhận phát hiện trùng giờ, báo “hãy hủy buổi kia trước”; không giữ thêm chỗ hoặc kéo dài hạn xác nhận.
- **E5 – Xung đột đồng thời (409 Conflict):** Yêu cầu xác nhận và tác vụ hết hạn đồng thời; chỉ một chuyển trạng thái được ghi nhận giao dịch; thao tác còn lại nhận `409`.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.4-01:** Hạn xác nhận lại là mốc đến trước giữa lúc đổi giờ + `RECONFIRM_TTL_MIN` (mặc định 60 phút) và giờ bắt đầu mới. Hết hạn chưa xác nhận thì mất chỗ, không phạt (quyết định 9 mục 7.3).
- **BR-F8.4-02:** Không xác nhận hoặc báo không tham gia thì mất chỗ, không bị phạt; chỗ chuyển cho danh sách chờ theo UC-F8.2.
- **BR-F8.4-03:** Đổi phòng chỉ thông báo; không yêu cầu xác nhận lại.
- **BR-F8.4-04:** `PENDING_RECONFIRM` vẫn chiếm chỗ trong sức chứa (ERD/thiet-ke-du-lieu.md).
- **Quyết định đã chốt:** Kiểm tra trùng giờ lúc hội viên xác nhận; người đang chờ giữ vị trí, nếu trùng giờ thì bỏ qua khi xét đưa vào lớp. Quản lý đúng chi nhánh và chủ chuỗi được sửa buổi riêng lẻ, lễ tân không được (quyết định 10, 15). Mã `cancel_reason` cụ thể do backend F8 Sprint 2 chốt (mục 7.5).

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

- **Kịch bản 1: Xác nhận giờ mới trước hạn sớm hơn**
  - **Given** Chỗ đặt đang `PENDING_RECONFIRM` và còn trước `reconfirm_expires_at`.
  - **When** Hội viên xác nhận tham gia giờ mới.
  - **Then** Chỗ đặt về `BOOKED`, vẫn chiếm một chỗ, lưu giờ mới ở buổi học và không nhân đôi chỗ đặt.
- **Kịch bản 2: Từ chối tham gia**
  - **Given** Chỗ đặt đang `PENDING_RECONFIRM`.
  - **When** Hội viên chọn không tham gia.
  - **Then** Chỗ đặt thành `CANCELLED`, không ghi phạt, chỗ được giải phóng và chuyển sang UC-F8.2 nếu chưa qua mốc ngừng đưa vào lớp.
- **Kịch bản 3: Hết hạn xác nhận**
  - **Given** Chỗ đặt chờ xác nhận đã tới hạn sớm hơn giữa lúc đổi giờ + TTL và giờ bắt đầu mới.
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

- **Kịch bản 6: Hạn xác nhận bị chặn bởi giờ mới**
  - **Given** Đổi giờ lúc 10:00, giờ bắt đầu mới 10:30, `RECONFIRM_TTL_MIN=60`.
  - **When** Hệ thống yêu cầu xác nhận lại.
  - **Then** `reconfirm_expires_at` là 10:30; chưa xác nhận khi hết hạn thì hủy không phạt, xét danh sách chờ nếu còn được phép.

- **Kịch bản 7: Xác nhận giờ mới trùng chỗ khác**
  - **Given** Chỗ đang `PENDING_RECONFIRM`, chưa hết hạn, giờ mới trùng một buổi khác hội viên đã đặt.
  - **When** Hội viên bấm xác nhận.
  - **Then** Từ chối, báo “hãy hủy buổi kia trước”; giữ trạng thái/hạn hiện tại. Người `WAITLIST` vẫn giữ vị trí, người trùng giờ bị bỏ qua khi xét đưa vào lớp.

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
- **Tác nhân chính:** Huấn luyện viên (`TRAINER`) được phân công dạy buổi; Quản lý chi nhánh của buổi khi sửa sau thời hạn huấn luyện viên được phép sửa
- **Tác nhân phụ (nếu có):** Không
- **Mô tả ngắn gọn:** Huấn luyện viên ghi nhận học viên đã tham dự hoặc vắng mặt cho buổi mình dạy. Check-in ở quầy chỉ đánh dấu `arrived_at`; chỉ điểm danh của huấn luyện viên quyết định `ATTENDED`/`NO_SHOW`.

#### A. Tiền điều kiện

- Huấn luyện viên đăng nhập vai trò `TRAINER`; hoặc quản lý đúng chi nhánh thực hiện nhánh sửa sau thời hạn.
- Buổi học tồn tại; huấn luyện viên được phân công hoặc quản lý có quyền đúng chi nhánh. Huấn luyện viên chỉ điểm danh/sửa từ giờ bắt đầu đến 23:59 cùng ngày theo giờ Việt Nam.
- Chỗ đặt cần điểm danh đang chiếm chỗ và chưa bị hủy.

#### B. Hậu điều kiện

- **Thành công:** `class_bookings.status` chuyển thành `ATTENDED` hoặc `NO_SHOW`; mỗi lần ghi/sửa kết quả, `attendance_marked_by` lưu `users.id` của người thao tác; khi sửa kết quả đã ghi, `audit_logs` ghi giá trị cũ/mới trong cùng giao dịch. Khi sửa điểm danh, hệ thống tính lại vi phạm theo BR-F8.6-03: nếu hội viên đang có khóa thì xét lần khóa đang có hiệu lực, dưới ngưỡng thì đặt `members.booking_locked_until = NULL` ngay, vẫn đạt ngưỡng thì giữ nguyên khóa, không rút ngắn. Lần điểm danh hoặc sửa thành `NO_SHOW` làm đạt ngưỡng thì áp khóa ngay.
- **Thất bại:** Không thay đổi điểm danh; trạng thái hiện tại được giữ nguyên.

#### C. Luồng sự kiện chính

1. Huấn luyện viên mở một buổi được phân công từ UC-F8.5.
2. Hệ thống xác thực vai trò/phạm vi: huấn luyện viên phải khớp `class_sessions.trainer_id`, thời điểm từ giờ bắt đầu đến 23:59 cùng ngày Việt Nam; sau đó chỉ quản lý chi nhánh của buổi được sửa.
3. Hệ thống trả danh sách chỗ đặt đang có chỗ cần điểm danh; không trộn các dòng chỉ đang `WAITLIST` hoặc đã `CANCELLED`.
4. Huấn luyện viên chọn từng hội viên và ghi nhận có mặt (`ATTENDED`) hoặc vắng mặt (`NO_SHOW`).
5. Hệ thống kiểm tra trạng thái điểm danh hợp lệ và chỗ không ở `WAITLIST`/`CANCELLED`; kết quả đã ghi được sửa trong thời hạn, sau hạn chỉ quản lý đúng chi nhánh được sửa.
6. Hệ thống cập nhật kết quả, ghi `attendance_marked_by` là `users.id` của người thao tác; nếu sửa kết quả đã ghi thì thêm một dòng `audit_logs` với giá trị cũ/mới trong cùng giao dịch.
7. Khi điểm danh mới là `NO_SHOW`, đếm vi phạm gồm hủy muộn (`is_late_cancel`) và `NO_SHOW` trong tháng dương lịch Việt Nam, sau lần bị khóa gần nhất theo quyết định 5–6; nếu đạt `PENALTY_LIMIT_PER_MONTH`, áp khóa theo `PENALTY_LOCK_DAYS` ngay. Với mỗi lần sửa kết quả đã ghi, tính lại vi phạm trong cùng giao dịch theo BR-F8.6-03: nếu hội viên đang có khóa, đếm hủy muộn (`is_late_cancel`) và `NO_SHOW` trong tháng dương lịch Việt Nam, sau lần khóa liền trước lần khóa đang xét; dưới ngưỡng thì đặt `members.booking_locked_until = NULL` ngay, vẫn đạt ngưỡng thì giữ nguyên khóa, không rút ngắn. Nếu hội viên không đang bị khóa, tính như điểm danh mới; sửa `ATTENDED` thành `NO_SHOW` làm đạt ngưỡng thì áp khóa ngay.
8. Hệ thống trả kết quả đã điểm danh.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Hội viên đã check-in ở quầy:** Nếu `arrived_at` đã có, vẫn cần huấn luyện viên xác nhận `ATTENDED`; check-in không tự chuyển trạng thái điểm danh.
- **A2 – Điểm danh cả danh sách:** Người triển khai backend F8 trong Sprint 2 chốt API và tính nguyên tử khi nhận nhiều kết quả; mọi dòng vẫn kiểm tra quyền, thời hạn và trạng thái.
- **A3 – Sửa điểm danh:** Huấn luyện viên được phân công có thể sửa trong khoảng từ giờ bắt đầu đến 23:59 cùng ngày Việt Nam. Sau thời hạn, huấn luyện viên bị khóa sửa; chỉ quản lý đúng chi nhánh được sửa.
- **E1 – Dữ liệu gửi sai định dạng (400 Bad Request):** Trạng thái ngoài tập cho phép hoặc trường bắt buộc thiếu.
- **E2 – Chưa đăng nhập (401 Unauthorized):** Token không hợp lệ.
- **E3 – Không được phân công / sai quyền (403 Forbidden):** Huấn luyện viên không phải huấn luyện viên của buổi học.
- **E4 – Buổi học/chỗ đặt không tồn tại (404 Not Found):** Trả `ProblemDetail` tiếng Việt.
- **E5 – chỗ đặt không thể điểm danh (422 Unprocessable Content):** Chỗ đặt đang `WAITLIST`, `CANCELLED` hoặc không thuộc thời hạn/quyền được điểm danh; không cập nhật.
- **E6 – Xung đột cập nhật đồng thời (409 Conflict):** Trạng thái điểm danh đã được thay đổi trong yêu cầu khác.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.6-01:** Huấn luyện viên được phân công điểm danh/sửa trong thời hạn; quản lý chi nhánh sửa sau thời hạn. Mỗi lần ghi/sửa điểm danh, `attendance_marked_by` ghi `users.id` của người thao tác; khi sửa kết quả đã ghi, `audit_logs` ghi giá trị cũ/mới trong cùng giao dịch (quyết định ngày 07/11/2026). Các cột cần thiết đã có trong ERD; việc đồng bộ ghi chú ERD thuộc mục 7.6.
- **BR-F8.6-02:** F4 ghi `arrived_at` khi check-in có lớp sắp bắt đầu; điều đó không thay cho `ATTENDED` do huấn luyện viên xác nhận (bản chốt phạm vi F4 và ERD).
- **BR-F8.6-03:** Vi phạm gồm hủy muộn (`is_late_cancel`) và `NO_SHOW`. Mỗi vi phạm chỉ được tính cho đúng một lần khóa (quyết định 5–6, làm rõ ngày 09/11/2026). Lần ghi mới hoặc sửa thành `NO_SHOW` làm đạt `PENALTY_LIMIT_PER_MONTH` thì áp khóa ngay. Cách đếm phụ thuộc tình huống, đều thực hiện trong cùng giao dịch với việc ghi điểm danh:
  - **Điểm danh mới:** đếm trong tháng dương lịch Việt Nam, chỉ sau lần bị khóa gần nhất.
  - **Sửa điểm danh khi hội viên đang có khóa hiệu lực:** đếm trong tháng dương lịch Việt Nam, chỉ sau **lần khóa liền trước lần khóa đang xét**, tức là bỏ các vi phạm đã được dùng cho một lần khóa cũ hơn. Nếu chưa có lần khóa nào trước đó thì chỉ giới hạn trong tháng. Dưới ngưỡng thì đặt `members.booking_locked_until = NULL` ngay; vẫn đạt ngưỡng thì giữ nguyên khóa hiện có, không rút ngắn.
  - **Sửa điểm danh khi hội viên không đang bị khóa:** tính như điểm danh mới.

  Lý do không dùng “sau lần khóa gần nhất” cho phép tính lại: khi hội viên đang bị khóa thì lần khóa gần nhất chính là lần đang xét, đếm sau nó luôn bằng 0 nên sửa bất kỳ điểm danh nào cũng gỡ khóa; còn đếm cả tháng thì một vi phạm có thể góp vào hai lần khóa.
- **BR-F8.6-04:** Chỗ đặt `ATTENDED` và `NO_SHOW` vẫn được tính trong sĩ số của buổi học (thiết kế dữ liệu ERD).
- **Quyết định đã chốt:** Huấn luyện viên được điểm danh và sửa từ lúc bắt đầu buổi đến 23:59 cùng ngày Việt Nam; sau đó chỉ quản lý chi nhánh được sửa (quyết định 14). Không tự coi hết giờ là `NO_SHOW` khi chưa có thao tác điểm danh. Sửa `NO_SHOW`/`ATTENDED` tính lại vi phạm và cập nhật khóa ngay theo BR-F8.6-03; không chờ tác vụ nền.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_bookings`, `audit_logs`, và `members.booking_locked_until` khi việc tính lại khóa cần thay đổi giá trị.
- **Bảng đọc:** `class_sessions`, `staff`, `users`, `system_configs`.

- `class_bookings`: `status`, `attendance_marked_by`, `arrived_at`, `member_id`, `class_session_id`, `is_late_cancel`, `cancelled_at`.
- `class_sessions`: `capacity`, `trainer_id`, `start_at`, `status`.
- `members`: `booking_locked_until`.
- `audit_logs`: `user_id`, `branch_id`, `action`, `entity`, `entity_id`, `old_value`, `new_value`.
- `system_configs`: `config_key`, `config_value`.

Lịch sử cần để đếm theo tháng Việt Nam, sau lần khóa gần nhất (điểm danh mới) và sau lần khóa liền trước lần khóa đang xét (sửa điểm danh) do backend F8 Sprint 2 thiết kế; không suy ra cột chưa có trong ERD (mục 7.6). Dòng `audit_logs` khi sửa điểm danh dùng các cột cũ/mới đã có; không thêm cột.

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
  - **Then** Chỗ đặt thành `ATTENDED`, `attendance_marked_by` ghi người thao tác; `arrived_at` không bị dùng làm nguồn thay thế cho thao tác này.
- **Kịch bản 2: Huấn luyện viên ghi nhận vắng mặt đạt ngưỡng**
  - **Given** Hội viên có 2 lần vi phạm được tính trong tháng và huấn luyện viên được phân công cho buổi.
  - **When** Huấn luyện viên đánh dấu chỗ đặt `NO_SHOW`.
  - **Then** Chỗ đặt thành `NO_SHOW`; hệ thống áp dụng khóa đặt 7 ngày khi sự kiện này là lần thứ ba trong tháng dương lịch Việt Nam và sau lần khóa gần nhất.
- **Kịch bản 3: Huấn luyện viên khác thử điểm danh**
  - **Given** Huấn luyện viên B không được gán vào buổi của huấn luyện viên A.
  - **When** Huấn luyện viên B gửi điểm danh.
  - **Then** Trả `403 Forbidden`, không đổi chỗ đặt.
- **Kịch bản 4: Hội viên đã check-in nhưng chưa được điểm danh**
  - **Given** `arrived_at` đã có nhưng chỗ đặt vẫn `BOOKED`.
  - **When** Chưa có thao tác điểm danh của huấn luyện viên.
  - **Then** Hệ thống vẫn chưa coi chỗ đặt là `ATTENDED`; check-in tại quầy không tự điểm danh.

- **Kịch bản 5: Biên điểm danh và sửa trong ngày**
  - **Given** Buổi bắt đầu 18:00 ngày 04/11; huấn luyện viên được phân công.
  - **When** Thao tác trước 18:00, đúng 18:00, sửa lúc 23:59 và thử lại ngày 05/11.
  - **Then** Trước giờ bắt đầu bị từ chối; đúng 18:00 và 23:59 được phép; sang ngày sau huấn luyện viên không được sửa.

- **Kịch bản 6: Quản lý sửa sau hạn huấn luyện viên**
  - **Given** Buổi đã qua ngày diễn ra; tài khoản quản lý thuộc đúng chi nhánh.
  - **When** Quản lý sửa kết quả điểm danh.
  - **Then** Cho sửa theo quyền quản lý; huấn luyện viên và quản lý chi nhánh khác không được dùng quyền này. `attendance_marked_by` ghi người quản lý và `audit_logs` ghi giá trị cũ/mới trong cùng giao dịch.

- **Kịch bản 7: Sửa `NO_SHOW` thành `ATTENDED` làm số vi phạm xuống dưới ngưỡng**
  - **Given** Điểm danh hiện là `NO_SHOW`, hội viên đang bị khóa và số vi phạm đếm theo BR-F8.6-03 (tháng Việt Nam, sau lần khóa liền trước lần khóa đang xét) thấp hơn `PENALTY_LIMIT_PER_MONTH` sau khi sửa; quản lý đúng chi nhánh được phép sửa.
  - **When** Quản lý sửa kết quả thành `ATTENDED`.
  - **Then** Trong cùng giao dịch, trạng thái được sửa, `attendance_marked_by` ghi người quản lý, `audit_logs` lưu giá trị cũ/mới và `members.booking_locked_until` được đặt `NULL` ngay, không chờ tác vụ ban đêm.

- **Kịch bản 8: Sửa điểm danh nhưng số vi phạm vẫn đạt ngưỡng**
  - **Given** Điểm danh đã ghi; hội viên đang có khóa và sau khi tính lại theo BR-F8.6-03, số vi phạm vẫn bằng hoặc lớn hơn `PENALTY_LIMIT_PER_MONTH`.
  - **When** Người có quyền sửa điểm danh cập nhật kết quả.
  - **Then** Ghi người sửa và giá trị cũ/mới như BR-F8.6-01; giữ nguyên `booking_locked_until`, không rút ngắn khóa.

- **Kịch bản 9: Sửa `ATTENDED` thành `NO_SHOW` làm đạt ngưỡng**
  - **Given** Điểm danh hiện là `ATTENDED`; hội viên không đang bị khóa và có số vi phạm thấp hơn ngưỡng trước lần sửa; sau khi sửa, số vi phạm tính như điểm danh mới (tháng Việt Nam, sau lần khóa gần nhất) đạt `PENALTY_LIMIT_PER_MONTH`.
  - **When** Người có quyền sửa kết quả thành `NO_SHOW`.
  - **Then** Trong cùng giao dịch, ghi người sửa và giá trị cũ/mới; áp khóa theo `PENALTY_LOCK_DAYS` ngay.

- **Kịch bản 10: Sửa điểm danh khi đã có hai lần khóa, mỗi vi phạm chỉ tính cho một lần khóa**
  - **Given** Trong tháng 11, hội viên có ba vi phạm A, B, C dẫn đến khóa lần 1. Hết khóa, hội viên có thêm ba vi phạm D, E, F, đều là `NO_SHOW`, dẫn đến khóa lần 2 và hội viên đang bị khóa lần 2. `PENALTY_LIMIT_PER_MONTH` là 3; người sửa là quản lý đúng chi nhánh.
  - **When** Quản lý sửa điểm danh E thành `ATTENDED`.
  - **Then** Hệ thống chỉ đếm các vi phạm sau lần khóa liền trước lần khóa 2, tức là sau lần khóa 1, gồm D và F, được 2 vi phạm, thấp hơn ngưỡng 3. Khóa lần 2 được gỡ ngay bằng cách đặt `members.booking_locked_until = NULL`. A, B, C không được tính lại vì đã dùng cho khóa lần 1. Ghi người sửa và giá trị cũ/mới như BR-F8.6-01.

---

### 3.8. UC-F8.7: Quản lý lịch lặp lớp học và phân công huấn luyện viên

- **Mã UC:** UC-F8.7
- **Tên Use Case:** Quản lý lịch lặp lớp học và phân công huấn luyện viên
- **Tác nhân chính:** Quản lý chi nhánh (`BRANCH_MANAGER`)
- **Tác nhân phụ (nếu có):** Hệ thống sinh buổi UC-F8.8; huấn luyện viên được phân công; Chủ chuỗi (`SUPER_ADMIN`) chỉ cho nhánh đổi phòng/giờ hoặc hủy buổi riêng lẻ
- **Mô tả ngắn gọn:** Quản lý loại lớp và lịch lặp hằng tuần tại chi nhánh; gán phòng, thời gian, sức chứa và huấn luyện viên có vai trò `TRAINER`. Quản lý cũng có thể hủy một buổi cụ thể theo quy tắc F8.

#### A. Tiền điều kiện

- Với lịch lặp: quản lý đăng nhập vai trò `BRANCH_MANAGER` và có chi nhánh hợp lệ. Với đổi phòng/giờ hoặc hủy buổi riêng lẻ: quản lý đúng chi nhánh hoặc `SUPER_ADMIN`; lễ tân không được.
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
5. Hệ thống xác nhận `trainer_id` trỏ tới nhân viên vai trò `TRAINER`, phòng/loại lớp hợp lệ; chặn lịch lặp trùng phòng hoặc huấn luyện viên và báo rõ lịch bị trùng.
6. Hệ thống tạo `class_schedules` trạng thái `ACTIVE` và ghi thời điểm tạo/cập nhật.
7. Khi cần ngừng lịch, quản lý chuyển trạng thái sang `INACTIVE` thay vì xóa dữ liệu.
8. Khi hủy một buổi cụ thể, quản lý chi nhánh của buổi hoặc chủ chuỗi chọn buổi học; hệ thống hủy buổi học cùng mọi chỗ đặt đang hoạt động và danh sách chờ của buổi học, không đánh dấu hủy muộn, rồi tạo thông báo cho các hội viên bị ảnh hưởng qua F10.
9. Hệ thống trả lịch/buổi đã lưu; UC-F8.8 chịu trách nhiệm sinh `class_sessions` theo ngày hiệu lực.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Cập nhật sức chứa:** `class_schedules.capacity` chỉ dùng cho các buổi sẽ sinh về sau; kiểm tra `0 < capacity <= rooms.capacity`, không đổi buổi đã sinh. Với `class_sessions.capacity`, chỉ cho sửa khi `start_at > now` và trạng thái là `SCHEDULED` hoặc `RESCHEDULED`; sức chứa mới vẫn phải dương, không vượt sức chứa phòng và ít nhất bằng số `BOOKED` + `PENDING_RECONFIRM` đang giữ chỗ (quyết định 13 và ràng buộc ERD). Không cho sửa sức chứa buổi đã bắt đầu hoặc không ở hai trạng thái này.
- **A2 – Ngừng lịch:** Chuyển trạng thái sang `INACTIVE`, không xóa cứng; không đổi các buổi đã sinh. Ngày lễ xử lý bằng hủy từng buổi.
- **A3 – Chưa có ngày kết thúc:** `valid_to = NULL` biểu thị không khai báo ngày dừng; số ngày sinh buổi trước do backend F8 Sprint 2 chốt.
- **A4 – Hủy một buổi cụ thể:** Chuyển `class_sessions.status = 'CANCELLED'`; mọi chỗ đặt đang hoạt động, gồm `BOOKED`, `PENDING_RECONFIRM`, `WAITLIST`, được chuyển `CANCELLED` với `cancel_reason = 'SESSION_CANCELLED'`, không phạt; gửi thông báo cho tất cả người có chỗ đặt/danh sách chờ của buổi. Không áp dụng tự động đưa vào lớp cho buổi đã hủy.
- **E1 – Dữ liệu sai định dạng/không thỏa ràng buộc (400 Bad Request):** Trả `ProblemDetail` theo trường dữ liệu.
- **E2 – Không có quyền (403 Forbidden):** Từ chối quản lý truy cập ngoài chi nhánh của mình và lễ tân sửa/hủy buổi riêng lẻ; không giới hạn chủ chuỗi vào một chi nhánh cho nhánh được phép.
- **E3 – Không tìm thấy phòng/loại lớp/nhân viên (404 Not Found):** Trả lỗi tiếng Việt.
- **E4 – Trùng hoặc xung đột dữ liệu (409 Conflict):** Ràng buộc duy nhất hoặc cập nhật cạnh tranh.
- **E5 – Vi phạm nghiệp vụ (422 Unprocessable Content):** Sức chứa không dương hoặc lớn hơn sức chứa phòng, sức chứa buổi thấp hơn tổng `BOOKED` và `PENDING_RECONFIRM`, giờ kết thúc không sau giờ bắt đầu, hoặc huấn luyện viên không có vai trò `TRAINER`.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.7-01:** `class_schedules` là lịch lặp theo tuần; `day_of_week`: 1 = thứ Hai … 7 = Chủ nhật (ISO).
- **BR-F8.7-02:** Với lịch lặp, `0 < class_schedules.capacity <= rooms.capacity`; sức chứa này chỉ dùng cho buổi sinh về sau, không cập nhật buổi đã sinh. Với buổi cụ thể, chỉ cho sửa `class_sessions.capacity` khi `start_at > now` và `status` là `SCHEDULED` hoặc `RESCHEDULED`; giá trị mới vẫn phải thỏa `0 < capacity <= rooms.capacity` và `>=` tổng `BOOKED` + `PENDING_RECONFIRM` đang giữ chỗ. `end_time > start_time`; huấn luyện viên phải có vai trò `TRAINER` (ERD và quyết định 13 ngày 07/11/2026).
- **BR-F8.7-03:** Lịch lặp chứa chi nhánh, loại lớp, huấn luyện viên, phòng, ngày/giờ, sức chứa, `valid_from`, `valid_to`, trạng thái.
- **BR-F8.7-04:** Chuyển trạng thái `INACTIVE`, không xóa cứng dữ liệu nghiệp vụ.
- **BR-F8.7-05:** Hủy buổi phải hủy toàn bộ chỗ đặt/danh sách chờ, không phạt và thông báo mọi người bị ảnh hưởng (bản chốt phạm vi F8). Không xóa cứng `class_sessions`/`class_bookings`.
- **BR-F8.7-06:** Chặn hai lịch lặp trùng phòng hoặc trùng huấn luyện viên khi thứ/ngày, giờ và khoảng hiệu lực tạo ra buổi thực tế trùng nhau; báo rõ lịch bị trùng. Vẫn chặn lịch huấn luyện viên với buổi PT/lớp nhóm theo F9/NF6 (quyết định 11 mục 7.3).
- **Quyết định đã chốt:** Sửa/ngừng lịch chỉ tác động buổi sinh về sau; ngày lễ hủy từng buổi (quyết định 12). Sức chứa lịch lặp chỉ áp dụng cho buổi sinh về sau và tuân theo BR-F8.7-02. Với buổi cụ thể, áp dụng giới hạn và điều kiện sửa trong BR-F8.7-02; không làm yếu bất biến sức chứa theo ERD. Đổi phòng/giờ hoặc hủy buổi riêng lẻ: quản lý đúng chi nhánh và chủ chuỗi được làm, lễ tân không được (quyết định 15). Backend F8 Sprint 2 chốt API, không tự mở rộng quyền của chủ chuỗi sang tạo lịch lặp.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `class_types`, `class_schedules`; khi sửa sức chứa buổi hoặc hủy buổi còn có `class_sessions`; khi hủy buổi còn có `class_bookings`, `notifications`.
- **Bảng đọc:** `branches`, `rooms`, `staff`, `users`.

- `class_types`: `name`, `description`, `status`.
- `class_schedules`: `branch_id`, `class_type_id`, `trainer_id`, `room_id`, `day_of_week`, `start_time`, `end_time`, `capacity`, `valid_from`, `valid_to`, `status`.
- `class_sessions`: `capacity`, `start_at`, `status` (khi sửa sức chứa buổi riêng lẻ hoặc hủy buổi).
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

- **Kịch bản 8: Chặn lịch lặp trùng phòng hoặc huấn luyện viên**
  - **Given** Hai lịch cùng phòng hoặc cùng huấn luyện viên có ngày lặp, giờ và khoảng hiệu lực giao nhau, tạo ra buổi thực tế trùng nhau.
  - **When** Quản lý tạo hoặc sửa lịch gây trùng.
  - **Then** Từ chối, nêu rõ lịch bị trùng, không lưu một phần dữ liệu.

- **Kịch bản 9: Sửa/ngừng lịch không đổi buổi đã sinh**
  - **Given** Lịch đã sinh các buổi cụ thể, có buổi đã có chỗ đặt và sức chứa lịch lặp hợp lệ (`0 < capacity <= rooms.capacity`).
  - **When** Quản lý sửa sức chứa hoặc ngừng lịch lặp.
  - **Then** Các buổi đã sinh, sức chứa và chỗ đặt của chúng giữ nguyên; sức chứa mới chỉ dùng cho buổi sinh về sau. Muốn xử lý ngày lễ thì hủy từng buổi.

- **Kịch bản 10: Giới hạn giảm sĩ số theo quyết định 13**
  - **Given** Buổi `SCHEDULED` hoặc `RESCHEDULED` có `start_at > now`, `class_sessions.capacity = 5`, với 3 chỗ `BOOKED` và 1 chỗ `PENDING_RECONFIRM` đang giữ chỗ.
  - **When** Quản lý sửa `class_sessions.capacity` xuống 3 hoặc 4.
  - **Then** Mức 3 bị từ chối với `422` và thông báo “đã có 4 người giữ chỗ”; mức 4 được chấp nhận. Sức chứa mới không thấp hơn `BOOKED` + `PENDING_RECONFIRM`.

  **Biên từ chối thuộc kịch bản 10:**
  - **Given** Buổi có `start_at <= now` hoặc trạng thái không phải `SCHEDULED`/`RESCHEDULED`.
  - **When** Quản lý yêu cầu sửa `class_sessions.capacity`.
  - **Then** Hệ thống từ chối thay đổi và giữ nguyên sức chứa.

- **Kịch bản 11: Quyền sửa buổi riêng lẻ**
  - **Given** Buổi thuộc chi nhánh A; có quản lý A, quản lý B, chủ chuỗi và lễ tân.
  - **When** Các tài khoản yêu cầu đổi phòng/giờ hoặc hủy buổi.
  - **Then** Quản lý A và chủ chuỗi được phép; quản lý B và lễ tân bị từ chối; không mở quyền tạo lịch lặp cho chủ chuỗi từ quyết định này.
- **Kịch bản 12: Cùng giờ nhưng không có buổi thực tế trùng nhau**
  - **Given** Hai lịch dùng cùng phòng/huấn luyện viên, nhưng thứ lặp khác nhau hoặc khoảng hiệu lực không giao nhau; không phát sinh buổi thực tế trùng nhau.
  - **When** Quản lý lưu lịch.
  - **Then** Không từ chối chỉ vì giờ trên đồng hồ giống nhau; vẫn kiểm tra các điều kiện hợp lệ còn lại.


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
- **E1 – Lịch lặp tham chiếu dữ liệu không còn hợp lệ:** Không tạo buổi học; ghi lỗi để người triển khai backend F8 trong Sprint 2 xử lý. Các buổi đã sinh giữ nguyên khi sửa/ngừng lịch; sửa/hủy từng buổi theo UC-F8.7.
- **E2 – Xung đột đồng thời giữa hai tác vụ nền (409/xung đột ràng buộc duy nhất nội bộ):** Ràng buộc duy nhất bảo vệ khỏi trùng lặp; xử lý chạy lại không tạo trùng và tiếp tục các ngày phát sinh khác.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.8-01:** F8 yêu cầu lịch lớp lặp hằng tuần; bảng `class_sessions` lưu từng buổi cụ thể.
- **BR-F8.8-02:** Duy nhất `(class_schedule_id, occurrence_date)`; ngày phát sinh là ngày theo lịch lặp tại lúc sinh, không đổi khi đổi giờ (`thiet-ke-du-lieu.md`, DBML).
- **BR-F8.8-03:** Giá trị phòng, huấn luyện viên và sức chứa được chép sang `class_sessions`; DBML cho phép thay huấn luyện viên của từng buổi học khi có người dạy thay.
- **Thiết kế còn mở:** Lịch chạy, số ngày sinh buổi trước và cơ chế thử lại do người triển khai backend F8 Sprint 2 chốt. Sửa/ngừng lịch không đổi buổi đã sinh; ngày lễ hủy từng buổi theo quyết định 12 mục 7.3. Không tự dùng `BOOKING_WINDOW_DAYS` làm số ngày sinh buổi trước.

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
- Lịch chạy, số ngày sinh buổi trước và cơ chế thử lại do người triển khai backend F8 trong Sprint 2 chốt; không tạo hợp đồng API từ UC này.

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
3. **Trùng giờ:** Kiểm tra lúc đặt mới, lúc xác nhận giờ mới và lúc đưa vào lớp; không tạo hai chỗ đã xác nhận trùng giờ cho cùng hội viên. Người đang chờ giữ vị trí; nếu trùng giờ thì bị bỏ qua lúc xét đưa vào lớp.
4. **FIFO:** `waitlisted_at ASC, id ASC`; không lưu vị trí danh sách chờ cố định và không cho người thao tác hộ vượt hàng.
5. **Đặt mới/đưa vào lớp:** Xét danh sách chờ đủ điều kiện trước người mới khi còn chỗ. Dưới `PROMOTION_CUTOFF_MIN` phút thì không tự đưa vào lớp và đóng cả đặt mới/vào danh sách chờ; đúng mốc chưa phải dưới mốc.
6. **Hủy chỗ:** Hủy trước ít nhất `LATE_CANCEL_HOURS` không phạt; rời `WAITLIST` luôn không phạt. Chỗ vừa được tự động đưa vào lớp có ân hạn từ thông báo trong web, bao gồm đúng `grace_until`. Đếm vi phạm theo tháng Việt Nam và sau lần bị khóa gần nhất; không phạt lại lỗi cũ.
7. **Đổi giờ:** Đổi phòng chỉ báo; đổi giờ yêu cầu xác nhận trước mốc sớm hơn giữa thời điểm đổi + `RECONFIRM_TTL_MIN` và giờ mới. Kiểm tra trùng giờ khi xác nhận; từ chối/hết hạn mất chỗ không phạt.
8. **Điểm danh:** F4 chỉ ghi `arrived_at`. Huấn luyện viên được phân công điểm danh/sửa từ giờ bắt đầu đến 23:59 cùng ngày Việt Nam; sau đó chỉ quản lý chi nhánh được sửa.
9. **Lịch lặp:** Thứ trong tuần theo ISO 1–7, `end_time > start_time`, `0 < capacity <= rooms.capacity`, huấn luyện viên phải có vai trò `TRAINER`; từng buổi cụ thể được lưu ở `class_sessions`.
10. **Thời gian:** Thời điểm trong DB là UTC (`timestamptz`); `occurrence_date` là ngày theo lịch Việt Nam. Trình bày giờ cho người dùng theo `Asia/Ho_Chi_Minh`.
11. **Cấu hình:** Đọc `system_configs`; giá trị chi nhánh ghi đè giá trị toàn chuỗi. Không viết cứng giá trị có dấu `*`.

**Mốc đóng đặt chỗ** là `PROMOTION_CUTOFF_MIN` (mặc định 30 phút), áp dụng cả đặt mới/vào danh sách chờ và tự động đưa từ danh sách chờ vào lớp theo quyết định 4 ngày 04/11.

### 4.1. Tham số cấu hình F8

Các số trong kịch bản kiểm thử dùng bộ giá trị mặc định bên dưới; khi triển khai phải đọc cấu hình có hiệu lực, không viết cứng các giá trị này.

| Key | Giá trị mặc định | Nơi quy định | Ý nghĩa |
| --- | ---: | --- | --- |
| `BOOKING_WINDOW_DAYS` | 7 | ERD DBML, `system_configs.config_key` | Đặt chỗ trước bao nhiêu ngày |
| `PROMOTION_CUTOFF_MIN` | 30 | Bản chốt phạm vi, ERD, ADR 0003 | Đóng đặt mới/vào chờ và ngừng tự động đưa vào lớp khi còn dưới ngưỡng này |
| `PROMOTION_GRACE_MIN` | 15 | Bản chốt phạm vi, ERD, ADR 0003 | Ân hạn từ thông báo trong web sau khi đưa vào lớp |
| `RECONFIRM_TTL_MIN` | 60 | Bản chốt phạm vi, ERD DBML | Thời hạn xác nhận lại khi đổi giờ |
| `LATE_CANCEL_HOURS` | 2 | Bản chốt phạm vi, ERD DBML | Hủy trước giờ ít nhất ngưỡng này thì không phạt |
| `PENALTY_LIMIT_PER_MONTH` | 3 | Bản chốt phạm vi, ERD DBML | Số vi phạm trong tháng Việt Nam và sau lần khóa gần nhất trước khi khóa |
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
- **F10 – Thông báo:** F8 tạo yêu cầu thông báo cho đưa vào lớp, đổi giờ, hủy buổi; F10 phụ trách gửi thông báo, thử lại và giờ gửi email. Email ban đêm vẫn gửi khi tới giờ mở cửa chi nhánh; ân hạn bắt đầu lúc tạo thông báo trong web và không chờ email (quyết định 7 ngày 04/11).
- **F18 – Đa chi nhánh:** V2 vận hành một chi nhánh mặc định, nhưng bảng F8 đã chứa `branch_id`; logic truy vấn/phân quyền phải theo chi nhánh được xác thực. Không cho người gọi đổi `branchId` để vượt quyền.

---

## 7. Quyết định nghiệp vụ và thiết kế còn mở

**Chốt 04/11/2026, trưởng nhóm Hoàng Kim Tiến (`Ktin06`).** Nguồn: [comment xác nhận 16 quyết định](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6633040706). Các bảng sau giữ nội dung quyết định của trưởng nhóm; không còn coi hai quy tắc đặt mới trước đây là chưa chốt.

### 7.1. Đặt chỗ và danh sách chờ

| # | Câu hỏi | Quyết định |
| --- | --- | --- |
| 1 | Cửa sổ đặt chỗ 7 ngày tính thế nào | **Theo ngày lịch (giờ Việt Nam):** đặt được mọi buổi có ngày học từ hôm nay đến hết ngày (hôm nay + 7), bất kể giờ. Ví dụ thứ Tư 04/11 đặt được mọi buổi đến hết 11/11. |
| 2 | Hai người vào chờ cùng thời điểm | Xếp theo `waitlisted_at`; trùng thì **dòng có `id` nhỏ hơn đứng trước**. |
| 3 | Lớp còn chỗ, có người chờ vừa đủ điều kiện lại, người mới bấm đặt | **Xét danh sách chờ trước:** trước khi nhận người mới, chạy lại bước xét danh sách chờ (dùng lại hàm tự động đưa vào lớp). Còn chỗ thì người mới mới được đặt. |
| 4 | Còn dưới 30 phút trước giờ học, người mới đặt được không | **Không.** Khi còn dưới `PROMOTION_CUTOFF_MIN` (30 phút), đóng đặt chỗ mới và vào danh sách chờ cho mọi buổi, kể cả buổi còn trống. |

### 7.2. Phạt và ân hạn

| # | Câu hỏi | Quyết định |
| --- | --- | --- |
| 5 | "3 lần trong tháng" | **Tháng dương lịch theo giờ Việt Nam**; sang tháng mới đếm lại từ 0. |
| 6 | Hết khóa 7 ngày, vi phạm cũ còn tính không | **Đếm lại từ 0:** chỉ đếm vi phạm xảy ra sau lần bị khóa gần nhất (và trong tháng hiện tại). Không phạt hai lần cho cùng một lỗi. |
| 7 | Không có email hoặc gửi email lỗi thì ân hạn tính thế nào | Ân hạn 15 phút **tính từ lúc tạo thông báo trong web** (thời điểm được đưa vào lớp). Hủy đúng mốc `grace_until` vẫn tính là trong ân hạn (so sánh `<=`). |
| 8 | Rời danh sách chờ sát giờ có bị tính hủy muộn | **Không bao giờ.** Hủy muộn chỉ áp dụng cho chỗ đã `BOOKED`. |

**Làm rõ quyết định 6 về tính lại vi phạm khi sửa điểm danh (09/11/2026).** Nguồn: [comment của trưởng nhóm](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6734172961). Mỗi vi phạm chỉ được tính cho đúng một lần khóa.

- **Điểm danh mới:** đếm hủy muộn và `NO_SHOW` trong tháng dương lịch Việt Nam, sau lần khóa gần nhất.
- **Sửa điểm danh làm phải tính lại khóa đang có hiệu lực:** đếm trong tháng Việt Nam, sau lần khóa liền trước lần khóa đang xét. Dưới ngưỡng thì gỡ khóa ngay; vẫn đạt ngưỡng thì giữ nguyên, không rút ngắn.
- **Hội viên không đang bị khóa:** tính lại như điểm danh mới.
- **Ví dụ của trưởng nhóm:** tháng 11 có vi phạm A, B, C thì khóa lần 1; hết khóa, có thêm D, E, F thì khóa lần 2. Sửa E thành `ATTENDED`: chỉ đếm D, F (sau khóa lần 1) được 2, thấp hơn 3 nên gỡ khóa lần 2; A, B, C không được tính lại. Ví dụ này là kịch bản 10 của UC-F8.6.
- **Lý do:** khi hội viên đang bị khóa, lần khóa gần nhất chính là lần đang xét nên đếm sau nó luôn bằng 0 và sửa bất kỳ điểm danh nào cũng gỡ khóa; còn đếm cả tháng thì một vi phạm có thể góp vào hai lần khóa.

### 7.3. Đổi giờ và lịch lặp

| # | Câu hỏi | Quyết định |
| --- | --- | --- |
| 9 | Hạn xác nhận 60 phút vượt quá giờ học mới | **Hạn = mốc nào đến trước:** `reconfirm_expires_at = min(lúc đổi giờ + RECONFIRM_TTL_MIN, giờ bắt đầu mới)`. Quá hạn chưa xác nhận thì mất chỗ, không phạt. |
| 10 | Giờ mới trùng buổi khác hội viên đã đặt | **Kiểm tra trùng giờ khi hội viên bấm xác nhận**, giống lúc đặt mới. Trùng thì từ chối và báo "hãy hủy buổi kia trước". Người đang chờ giữ nguyên vị trí; ai trùng giờ bị bỏ qua khi xét, theo quy tắc sẵn có. |
| 11 | Hai lịch lặp trùng phòng hoặc trùng huấn luyện viên | **Chặn, báo lỗi** rõ trùng với lịch nào. |
| 12 | Sửa hoặc ngừng lịch lặp khi đã sinh buổi | **Chỉ áp dụng cho buổi sinh về sau.** Buổi đã sinh giữ nguyên; muốn đổi thì quản lý sửa hoặc hủy từng buổi. Ngày lễ xử lý bằng cách hủy từng buổi. |

### 7.4. Sĩ số, điểm danh, phân quyền, dữ liệu

| # | Câu hỏi | Quyết định |
| --- | --- | --- |
| 13 | Giảm sức chứa buổi hoặc lịch lặp | Với `class_sessions.capacity`, chỉ cho sửa khi `start_at > now` và trạng thái `SCHEDULED` hoặc `RESCHEDULED`; sức chứa mới phải `>= BOOKED + PENDING_RECONFIRM` đang giữ chỗ. Với `class_schedules.capacity`, chỉ dùng cho buổi sinh về sau và kiểm tra `0 < capacity <= rooms.capacity`; buổi đã sinh không đổi. |
| 14 | Khoảng thời gian điểm danh | **Từ giờ bắt đầu buổi đến 23:59 cùng ngày (giờ Việt Nam)**, sửa được trong khoảng đó. Sau đó khóa; cần sửa thì quản lý chi nhánh sửa. |
| 15 | Ai sửa buổi lớp riêng lẻ (đổi phòng, đổi giờ, hủy) | **Quản lý chi nhánh** (chỉ chi nhánh mình) **và chủ chuỗi** (mọi chi nhánh). Lễ tân không được. |
| 16 | `class_bookings.created_by` khi hội viên tự đặt | **Ghi `users.id` của tài khoản đã bấm đặt** (hội viên hoặc lễ tân). Chỗ được tự động đưa từ danh sách chờ vào lớp giữ nguyên người tạo ban đầu. |

### 7.5. Phần kỹ thuật được giao chốt trong Sprint 2

**Người quyết định:** Người triển khai backend F8 được phân công khi lập kế hoạch Sprint 2; phối hợp F10 cho thông báo. Chưa tự gán thành viên hoặc tạo API contract đã duyệt.

- API, DTO, mã HTTP chi tiết và tính nguyên tử khi điểm danh cả danh sách.
- Kế hoạch khóa toàn giao dịch đặt mới/xét danh sách chờ/giải phóng chỗ theo NF6, xử lý lại giao dịch khi tập ứng viên thay đổi.
- Mã `cancel_reason` cho từ chối/hết hạn xác nhận lại.
- Nội dung lịch công khai (có số chỗ trống hay không), bộ lọc và dữ liệu trả về.
- Lịch chạy, số ngày sinh buổi trước và cách thử lại UC-F8.8.
- Cách lưu mốc lần khóa gần nhất, thời điểm vi phạm và mốc tạo thông báo trong web. Không tự thêm tên cột/migration trong đặc tả này.

### 7.6. Đồng bộ nguồn chuẩn và phụ thuộc còn lại

- **Người phụ trách ADR và ERD (trưởng nhóm giao trong buổi lập kế hoạch Sprint 2 theo comment ngày 09/11; chưa ghi tên người nhận):** cập nhật ADR 0003, bản chốt phạm vi và ghi chú `class_bookings.grace_until` trong ERD từ mốc email sang mốc thông báo trong web theo quyết định 7 mới; bổ sung tiêu chí phụ `id` của FIFO, `created_by` cho cả hội viên/lễ tân và cập nhật ghi chú `attendance_marked_by` để phản ánh người điểm danh hoặc người sửa (kể cả quản lý). Ghi nhận `audit_logs.old_value`/`new_value` cho giá trị điểm danh cũ/mới; các cột này đã có trong ERD, không thêm cột. Giữ nguyên luồng gửi email ban đêm. SRS chỉ ghi quyết định đã được xác nhận, không sửa hộ tài liệu chuẩn của đồng đội trong nhiệm vụ #9.
- **Backend F8 Sprint 2:** `created_by` bắt buộc có `users.id` người bấm đặt theo quyết định 16; ERD hiện để nullable, nên cần chốt ràng buộc/ý nghĩa NULL và đồng bộ ghi chú, không dùng tính nullable để bỏ qua người tạo của chỗ đặt mới.
- **Backend F8 Sprint 2 phối hợp F10:** thiết kế dữ liệu để đếm vi phạm theo BR-F8.6-03, gồm sau lần bị khóa gần nhất khi điểm danh mới và sau lần khóa liền trước lần khóa đang xét khi sửa điểm danh; vì vậy cách lưu phải xác định được từng lần khóa theo thứ tự, không chỉ lần khóa gần nhất. Không suy ra lịch sử chỉ từ `booking_locked_until` trừ số ngày cấu hình hiện tại. ERD hiện chưa có mốc lịch sử khóa được chỉ định cho quy tắc này. Hồ sơ được lễ tân đặt hộ nhưng chưa có tài khoản cần cách ghi mốc thông báo trong web phù hợp với `notifications.user_id`; không tự tạo cột hoặc bỏ ân hạn.
- **Ranh giới tính lại khi sửa điểm danh đã được trưởng nhóm chốt ngày 09/11/2026**, xem mục 7.2 và BR-F8.6-03; không còn là điểm chờ xác nhận.

---

## 8. Truy vết nguồn

| Nội dung | Nguồn |
| --- | --- |
| 16 quyết định nghiệp vụ ngày 04/11, phần làm rõ ngày 07/11 về sức chứa, sửa điểm danh và quyết định ngày 09/11 về tính lại vi phạm khi sửa điểm danh | [Xác nhận của trưởng nhóm](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6633040706), [làm rõ 07/11](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6701988493), [quyết định 09/11](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6734172961), mục 7.1–7.4; nguồn chuẩn cần đồng bộ theo mục 7.6 |
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
| 04/11/2026 | Việt hóa diễn đạt; ghi người quyết định; rút chỉ mục dữ liệu theo ERD; đơn giản UC-F8.1 trong phạm vi đã rõ; sửa tham chiếu và thứ tự kịch bản. Ở lần sửa trước comment chốt mới, câu 13–14 còn chờ trưởng nhóm; trạng thái đã được cập nhật theo 16 quyết định ở dòng lịch sử tiếp theo. |
| 04/11/2026 | Bổ sung 16 quyết định nghiệp vụ từ comment của trưởng nhóm; sửa luồng/ân hạn/FIFO/đóng đặt/điểm danh và thêm kịch bản biên. Ghi phụ thuộc đồng bộ ADR/ERD và câu hỏi tác động sửa điểm danh sai lên lệnh khóa. |
| 07/11/2026 | Cập nhật quyết định 13 về sức chứa lịch lặp/buổi cụ thể; bổ sung điều kiện sửa và tiêu chí chấp nhận. Cập nhật việc sửa điểm danh, tính lại khóa và audit log theo comment trưởng nhóm; giữ phụ thuộc lưu lịch sử khóa/thời điểm vi phạm và ghi chú khả năng cần xác nhận cửa sổ đếm. |
| 10/11/2026 | Áp dụng quyết định ngày 09/11 của trưởng nhóm: mỗi vi phạm chỉ tính cho một lần khóa; khi sửa điểm danh làm tính lại khóa đang có hiệu lực thì đếm trong tháng, sau lần khóa liền trước lần đang xét; hội viên không bị khóa thì tính như điểm danh mới. Cập nhật BR-F8.6-03, mục 7.2, 7.6, UC-F8.6, kịch bản 7–9 và thêm kịch bản 10 (hai lần khóa). Ghi trách nhiệm đồng bộ ADR/ERD theo việc trưởng nhóm giao trong buổi lập kế hoạch Sprint 2. |
