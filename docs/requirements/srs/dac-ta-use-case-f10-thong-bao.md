# Đặc tả Use Case – F10: Thông báo

- **Mã chức năng:** F10
- **Version:** v2 (Bắt buộc)
- **Người biên soạn:** Nguyễn Trọng Việt
- **Người duyệt chéo:** Lê Phước Minh Quân
- **Cập nhật 24/11/2026 (Nguyễn Trọng Việt):** viết lại mục H của UC-F10.1, UC-F10.2, UC-F10.4, UC-F10.5 theo cây truy vết ([ADR 0012](../../decisions/0012-quy-uoc-ma-truy-vet.md)): 4 user story, 25 AC có mã. Áp ADR 0014 (bỏ danh sách chờ lớp học nhóm): bỏ UC-F10.3, bỏ hạn xác nhận lại ở UC-F10.4. Áp ADR 0013: gửi email qua Brevo. Các mã UC-F10.3, BR-F10.5, CONFIG-F10.2, CONFIG-F10.3 không dùng lại; các mã còn lại giữ nguyên.
- **Tài liệu chuẩn tham chiếu:**
  - [Bản chốt phạm vi](../ban-chot-pham-vi.md)
  - [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml)
  - [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md)
  - [Quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md)
  - [ADR liên quan](../../decisions/)

---

## 1. Tổng quan chức năng

Thông báo giúp hội viên nắm kịp các sự kiện liên quan đến gói tập và lớp học mà không cần hỏi quầy lễ tân. Hệ thống tự động tạo thông báo khi có sự kiện, qua hai kênh:

- **Trong web (`IN_APP`):** hiển thị ngay trong cổng hội viên theo thời gian thực, đẩy tới trình duyệt đang mở qua WebSocket (STOMP).
- **Email (`EMAIL`):** gửi qua Brevo (ADR 0013) tới địa chỉ `users.email` của hội viên, chỉ trong giờ mở cửa của chi nhánh liên quan (`branches.open_time` đến `close_time`); thông báo email phát sinh ngoài giờ được hẹn gửi vào giờ mở cửa (`scheduled_at`).

Có 3 loại thông báo: sắp hết hạn gói (`MEMBERSHIP_EXPIRING`), nhắc lịch lớp (`CLASS_REMINDER`), đổi giờ lớp (`CLASS_RESCHEDULED`). ERD còn khai báo loại `CLASS_CANCELLED` (hủy buổi lớp); loại này không nằm trong danh sách yêu cầu của F10, được coi là thuộc phạm vi F8. Thông báo "được đưa vào lớp từ danh sách chờ" đã bỏ theo ADR 0014.

Người nhận là **Hội viên có tài khoản** (thông báo gắn với `users.id`). Ở Sprint 1, tài liệu này chỉ đặc tả; phần hiện thực làm ở Sprint 2.

Mỗi sự kiện tạo **hai dòng** trong bảng `notifications`: một dòng kênh `IN_APP`, một dòng kênh `EMAIL`.

---

## 2. Danh sách Use Case

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F10.1** | Gửi thông báo sắp hết hạn gói | Hệ thống (tự động) | Cao | Sprint 2 |
| **UC-F10.2** | Gửi thông báo nhắc lịch lớp | Hệ thống (tự động) | Trung bình | Sprint 2 |
| **UC-F10.3** | Đã bỏ (ADR 0014) | — | — | — |
| **UC-F10.4** | Gửi thông báo đổi giờ lớp | Hệ thống (tự động) | Cao | Sprint 2 |
| **UC-F10.5** | Xem thông báo trong web | Hội viên | Trung bình | Sprint 2 |

---

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F10.1: Gửi thông báo sắp hết hạn gói

- **Mã UC:** UC-F10.1
- **Tên Use Case:** Gửi thông báo sắp hết hạn gói
- **Tác nhân chính:** Hệ thống (tác vụ chạy định kỳ)
- **Tác nhân phụ (nếu có):** Dịch vụ gửi email (Brevo); Hội viên (người nhận)
- **Mô tả ngắn gọn:** Hệ thống báo cho hội viên biết gói tập sắp hết hạn hoặc sắp hết buổi để kịp gia hạn.

#### A. Tiền điều kiện (Preconditions)

- Hội viên có tài khoản (`members.user_id` có giá trị) và có một trong hai loại gói sau:
  - Gói ACTIVE hết hạn trong 7 ngày tới (theo `ban-chot-pham-vi.md`, mục "Dashboard (F6)").
  - Gói SESSION ACTIVE còn không quá `SESSION_WARNING_THRESHOLD` buổi (mặc định 2).
- Hội viên chưa có gói nối tiếp (UPCOMING) cùng loại.
- Gói này chưa có thông báo `MEMBERSHIP_EXPIRING` nào gắn với nó.

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Hai dòng `notifications` được tạo (kênh `IN_APP` và `EMAIL`, `type` = `MEMBERSHIP_EXPIRING`, `related_entity` = `memberships`, `related_id` = mã gói). Thông báo trong web hiển thị ngay; email được gửi trong giờ mở cửa, sau đó dòng email chuyển `SENT`.
- **Thất bại:** Dòng email chuyển `FAILED`; thông báo trong web vẫn có.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Tác vụ định kỳ chạy và truy vấn `memberships`: gói ACTIVE có `end_date` trong 7 ngày tới, và gói SESSION ACTIVE có `remaining_sessions` không quá `SESSION_WARNING_THRESHOLD`.
2. Hệ thống loại những gói đã có thông báo `MEMBERSHIP_EXPIRING` (tìm theo `related_entity`, `related_id`) và những gói mà hội viên đã có gói cùng loại nối tiếp ở trạng thái UPCOMING.
3. Hệ thống xác định tài khoản nhận qua `members.user_id`; hội viên chưa có tài khoản thì bỏ qua.
4. Hệ thống tạo trong một giao dịch (`@Transactional`) hai dòng `notifications`: kênh `IN_APP` và kênh `EMAIL` (`status` = `PENDING`, `scheduled_at` = thời điểm gửi cho phép theo giờ mở cửa chi nhánh). Sau khi lưu, hệ thống đẩy thông báo `IN_APP` tới trình duyệt hội viên đang mở qua WebSocket (STOMP).
5. Tác vụ gửi email quét các dòng `EMAIL` có `status` = `PENDING` và `scheduled_at` đã đến, gửi tới `users.email` qua Brevo, rồi cập nhật `status` = `SENT` và `sent_at`.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **A1 – Hội viên chưa có tài khoản:**
  - Tại bước 3, vì `notifications.user_id` bắt buộc có giá trị, hệ thống không tạo thông báo cho hội viên này.
- **A2 – Email phát sinh ngoài giờ mở cửa:**
  - Tại bước 4, hệ thống đặt `scheduled_at` vào giờ mở cửa kế tiếp; thông báo trong web hiển thị ngay.
- **A3 – Hội viên không mở web lúc có thông báo:**
  - Tại bước 4, không có kết nối WebSocket nào; thông báo vẫn lưu trong `notifications` và hội viên thấy khi mở web (UC-F10.5).
- **E1 – Gửi email thất bại:**
  - Tại bước 5, dịch vụ email lỗi. Dòng email chuyển `FAILED`, thông báo trong web không bị ảnh hưởng **(cơ chế thử lại là đề xuất, cần nhóm xác nhận)**.
- **E5 – Vi phạm quy tắc nghiệp vụ:**
  - Hội viên đã gia hạn trước khi tác vụ chạy (có gói UPCOMING cùng loại): hệ thống không gửi thông báo, theo `ban-chot-pham-vi.md`.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F10.1:** Mỗi sự kiện tạo một thông báo trong web và một email (hai dòng `notifications`).
- **BR-F10.2:** Không gửi trùng: mỗi gói chỉ có một thông báo `MEMBERSHIP_EXPIRING` (xác định qua `related_entity`, `related_id`).
- **BR-F10.3:** Email chỉ gửi trong giờ mở cửa của chi nhánh (`branches.open_time` đến `close_time`).
- **CONFIG-F10.1:** Ngưỡng cảnh báo gói SESSION đọc từ bảng `system_configs` với key `SESSION_WARNING_THRESHOLD` (giá trị mặc định: `2`), không viết cứng (Quy tắc số 15). Mốc 7 ngày với gói ACTIVE là giá trị cố định theo `ban-chot-pham-vi.md`, mục "Dashboard (F6)" (không có dấu `*` nên không phải tham số cấu hình).

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `notifications`; đọc `memberships`, `members`, `users`, `branches`, `system_configs`
- **Chi tiết trường dữ liệu:**

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `notifications` | `user_id` | `bigint` | NOT NULL, FK → `users.id` | Tài khoản nhận |
| `notifications` | `type` | `varchar(50)` | NOT NULL | Giá trị `MEMBERSHIP_EXPIRING` |
| `notifications` | `title` | `varchar(255)` | NOT NULL | Tiêu đề |
| `notifications` | `content` | `text` | | Nội dung (tên gói, ngày hết hạn hoặc số buổi còn lại) |
| `notifications` | `channel` | `varchar` | NOT NULL, CHECK (IN_APP, EMAIL) | Kênh |
| `notifications` | `related_entity`, `related_id` | `varchar(50)`, `bigint` | | `memberships` và mã gói, để mở đúng màn hình và chống gửi trùng |
| `notifications` | `status` | `varchar` | NOT NULL, default `PENDING`, CHECK (PENDING, SENT, FAILED) | Trạng thái gửi |
| `notifications` | `scheduled_at` | `timestamptz` | | Hẹn gửi email tới giờ mở cửa |
| `notifications` | `sent_at` | `timestamptz` | | Thời điểm đã gửi |
| `memberships` | `end_date`, `remaining_sessions`, `package_type`, `status` | `date`, `int`, `varchar`, `varchar` | | Điều kiện chọn gói sắp hết |
| `system_configs` | `config_key`, `config_value` | `varchar(100)`, `varchar(255)` | NOT NULL | `SESSION_WARNING_THRESHOLD` = `2` |

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint:** Không có; đây là tác vụ nền của hệ thống. Thông báo trong web đẩy qua WebSocket (STOMP).
- **Giao diện:** Thông báo hiện trong cổng hội viên (xem UC-F10.5) và trong email.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F10.1.1: Nhận thông báo khi gói tập sắp hết hạn hoặc sắp hết buổi

- **User story:** Là **Hội viên**, tôi muốn **nhận thông báo khi gói tập sắp hết hạn hoặc sắp hết buổi**, để **chủ động gia hạn và không bị gián đoạn quá trình tập luyện**.
- **BR liên quan:** BR-F10.1, BR-F10.2, BR-F10.3; CONFIG-F10.1.
- **Version / Sprint:** v2 / Sprint 2.
- **Tiêu chí chấp nhận:**
  - **AC-F10.1.1-01: Thông báo khi gói TIME hoặc gói ACTIVE sắp hết hạn** (BR-F10.1)
    - **Given** Hội viên có tài khoản và sở hữu gói tập đang ở trạng thái `ACTIVE`, có `end_date` nằm trong 7 ngày tới, đồng thời chưa có gói cùng loại ở trạng thái `UPCOMING`.
    - **When** Tác vụ định kỳ kiểm tra các gói tập sắp hết hạn.
    - **Then** Hệ thống tạo hai dòng thông báo trong bảng `notifications`: một dòng `IN_APP` và một dòng `EMAIL`, cùng có `type = MEMBERSHIP_EXPIRING`, `related_entity = memberships` và `related_id` là mã gói tập.
  - **AC-F10.1.1-02: Thông báo khi gói SESSION sắp hết buổi** (CONFIG-F10.1)
    - **Given** Hội viên có tài khoản và sở hữu gói `SESSION` đang `ACTIVE`, số buổi còn lại không vượt quá giá trị `SESSION_WARNING_THRESHOLD` trong `system_configs` (mặc định là 2), đồng thời chưa có gói cùng loại nối tiếp ở trạng thái `UPCOMING`.
    - **When** Tác vụ định kỳ kiểm tra số buổi còn lại của các gói tập.
    - **Then** Hệ thống tạo hai dòng thông báo `IN_APP` và `EMAIL` có `type = MEMBERSHIP_EXPIRING`, nội dung thể hiện tên gói và số buổi còn lại; không viết cứng ngưỡng cảnh báo trong mã nguồn.
  - **AC-F10.1.1-03: Không gửi thông báo trùng cho cùng một gói** (BR-F10.2)
    - **Given** Gói tập đã có thông báo `MEMBERSHIP_EXPIRING` với `related_entity = memberships` và `related_id` tương ứng.
    - **When** Tác vụ định kỳ tiếp tục chạy và kiểm tra lại gói tập đó.
    - **Then** Hệ thống không tạo thêm thông báo `IN_APP` hoặc `EMAIL` cho cùng gói tập.
  - **AC-F10.1.1-04: Không gửi thông báo khi hội viên đã có gói nối tiếp**
    - **Given** Hội viên đã đăng ký một gói tập cùng loại ở trạng thái `UPCOMING`.
    - **When** Tác vụ định kỳ kiểm tra gói tập hiện tại sắp hết hạn hoặc sắp hết buổi.
    - **Then** Hệ thống không tạo thông báo `MEMBERSHIP_EXPIRING` cho gói hiện tại.
  - **AC-F10.1.1-05: Không tạo thông báo cho hội viên chưa có tài khoản**
    - **Given** Hội viên đáp ứng điều kiện sắp hết hạn hoặc sắp hết buổi nhưng `members.user_id` không có giá trị.
    - **When** Tác vụ định kỳ xử lý gói tập.
    - **Then** Hệ thống bỏ qua hội viên này, không tạo bản ghi `notifications` vì không xác định được tài khoản nhận.
  - **AC-F10.1.1-06: Hiển thị thông báo trong web và gửi email đúng thời gian**
    - **Given** Hội viên có tài khoản, gói tập thỏa mãn điều kiện cảnh báo và chi nhánh đang trong giờ mở cửa.
    - **When** Hệ thống tạo thông báo cho gói tập.
    - **Then** Thông báo `IN_APP` được đẩy tới trình duyệt hội viên đang mở qua WebSocket (STOMP); thông báo `EMAIL` được gửi tới `users.email` qua Brevo và cập nhật `status = SENT`, `sent_at` sau khi gửi thành công.
  - **AC-F10.1.1-07: Hẹn gửi email khi phát sinh ngoài giờ mở cửa và xử lý lỗi gửi**
    - **Given** Thông báo phát sinh ngoài giờ mở cửa của chi nhánh hoặc dịch vụ gửi email (Brevo) gặp lỗi khi gửi.
    - **When** Hệ thống xử lý hai dòng thông báo.
    - **Then** Nếu ngoài giờ mở cửa, dòng `EMAIL` được đặt `scheduled_at` vào giờ mở cửa kế tiếp, còn `IN_APP` vẫn hiển thị ngay. Nếu gửi email thất bại, dòng `EMAIL` chuyển `FAILED`, thông báo `IN_APP` vẫn được giữ nguyên và không bị ảnh hưởng.

---

### 3.2. UC-F10.2: Gửi thông báo nhắc lịch lớp

- **Mã UC:** UC-F10.2
- **Tên Use Case:** Gửi thông báo nhắc lịch lớp
- **Tác nhân chính:** Hệ thống (tác vụ chạy định kỳ)
- **Tác nhân phụ (nếu có):** Dịch vụ gửi email (Brevo); Hội viên (người nhận)
- **Mô tả ngắn gọn:** Hệ thống nhắc hội viên về lớp đã đặt sắp diễn ra.

#### A. Tiền điều kiện (Preconditions)

- Hội viên có tài khoản và một lượt đặt chỗ `class_bookings` ở trạng thái BOOKED cho một buổi lớp sắp bắt đầu **[cần chốt: nhắc trước bao lâu; `ban-chot-pham-vi.md` và danh sách khóa `system_configs` trong ERD chưa có mốc này]**.
- Buổi lớp chưa bị hủy (`class_sessions.status` là SCHEDULED hoặc RESCHEDULED).

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Hai dòng `notifications` được tạo (`type` = `CLASS_REMINDER`, `related_entity` = `class_bookings`, `related_id` = mã đặt chỗ); thông báo trong web hiển thị ngay, email gửi theo UC-F10.1 bước 5.
- **Thất bại:** Dòng email chuyển `FAILED`; thông báo trong web vẫn có.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Tác vụ định kỳ chạy và truy vấn các buổi lớp (`class_sessions`) sắp bắt đầu.
2. Hệ thống lấy các lượt đặt chỗ BOOKED của buổi đó, loại các lượt đã có thông báo `CLASS_REMINDER` (theo `related_entity`, `related_id`).
3. Hệ thống xác định tài khoản nhận qua `members.user_id`.
4. Hệ thống tạo hai dòng `notifications` (kênh `IN_APP` và `EMAIL`) trong một giao dịch (`@Transactional`), rồi đẩy thông báo `IN_APP` tới trình duyệt hội viên đang mở qua WebSocket (STOMP).
5. Email được gửi như UC-F10.1 bước 5.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **A1 – Hội viên chưa có tài khoản:** hệ thống không tạo thông báo cho hội viên này.
- **E1 – Gửi email thất bại:** dòng email chuyển `FAILED`; thông báo trong web vẫn có **(cơ chế thử lại là đề xuất)**.
- **E5 – Lớp đã bị hủy hoặc hội viên đã hủy chỗ:** hệ thống không gửi nhắc (buổi CANCELLED hoặc lượt đặt chỗ CANCELLED).

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F10.1:** Mỗi sự kiện tạo một thông báo trong web và một email.
- **BR-F10.4:** Mỗi lượt đặt chỗ chỉ được nhắc một lần (xác định qua `related_entity`, `related_id`).
- **BR-F10.3:** Email chỉ gửi trong giờ mở cửa của chi nhánh.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `notifications`; đọc `class_bookings`, `class_sessions`, `members`, `users`, `branches`
- **Chi tiết trường dữ liệu:**

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `notifications` | `type` | `varchar(50)` | NOT NULL | Giá trị `CLASS_REMINDER` |
| `notifications` | `channel`, `status`, `scheduled_at`, `sent_at` | như UC-F10.1 | | |
| `class_bookings` | `class_session_id` | `bigint` | NOT NULL, FK → `class_sessions.id` | Buổi lớp đã đặt |
| `class_bookings` | `member_id` | `bigint` | NOT NULL, FK → `members.id` | Hội viên đặt chỗ |
| `class_bookings` | `status` | `varchar` | NOT NULL, CHECK (BOOKED, CANCELLED, ATTENDED, NO_SHOW) | Chỉ nhắc khi BOOKED |
| `class_sessions` | `start_at` | `timestamptz` | NOT NULL | Giờ bắt đầu buổi lớp |
| `class_sessions` | `status` | `varchar` | NOT NULL, default `SCHEDULED` | Không nhắc khi CANCELLED |

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint:** Không có; tác vụ nền.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F10.2.1: Nhận thông báo nhắc lịch lớp học nhóm

- **User story:** Là **Hội viên**, tôi muốn **nhận thông báo nhắc về lớp học nhóm đã đặt trước khi lớp bắt đầu**, để **không bỏ lỡ lịch tập và chủ động sắp xếp thời gian tham gia**.
- **BR liên quan:** BR-F10.1, BR-F10.3, BR-F10.4; tích hợp F8.
- **Version / Sprint:** v2 / Sprint 2.
- **Tiêu chí chấp nhận:**
  - **AC-F10.2.1-01: Tạo thông báo nhắc cho lượt đặt chỗ BOOKED** (BR-F10.4)
    - **Given** Hội viên có tài khoản và có lượt đặt chỗ lớp học nhóm ở trạng thái `BOOKED`; buổi lớp có trạng thái `SCHEDULED` hoặc `RESCHEDULED`.
    - **When** Tác vụ định kỳ phát hiện buổi lớp sắp bắt đầu theo khoảng thời gian nhắc đã được thống nhất.
    - **Then** Hệ thống tạo hai dòng thông báo `IN_APP` và `EMAIL`, có `type = CLASS_REMINDER`, `related_entity = class_bookings` và `related_id` là mã lượt đặt chỗ.
  - **AC-F10.2.1-02: Không gửi thông báo nhắc trùng cho cùng lượt đặt chỗ** (BR-F10.4)
    - **Given** Lượt đặt chỗ đã có thông báo `CLASS_REMINDER` tương ứng.
    - **When** Tác vụ định kỳ tiếp tục quét các buổi lớp sắp diễn ra.
    - **Then** Hệ thống không tạo thêm thông báo nhắc cho lượt đặt chỗ đó.
  - **AC-F10.2.1-03: Không gửi nhắc khi hội viên đã hủy chỗ hoặc lớp bị hủy**
    - **Given** Lượt đặt chỗ đã chuyển sang `CANCELLED` hoặc buổi lớp đã chuyển sang trạng thái `CANCELLED`.
    - **When** Tác vụ định kỳ kiểm tra danh sách lượt đặt chỗ cần gửi thông báo.
    - **Then** Hệ thống không tạo thông báo `CLASS_REMINDER` cho lượt đặt chỗ hoặc buổi lớp này.
  - **AC-F10.2.1-04: Không tạo thông báo cho hội viên chưa có tài khoản**
    - **Given** Hội viên có lượt đặt chỗ `BOOKED` hợp lệ nhưng chưa liên kết tài khoản (`members.user_id` không có giá trị).
    - **When** Hệ thống xử lý thông báo nhắc lịch lớp.
    - **Then** Hệ thống bỏ qua hội viên này và không tạo hai dòng `notifications`.
  - **AC-F10.2.1-05: Hiển thị thông báo trong web và gửi email nhắc lịch**
    - **Given** Lượt đặt chỗ và buổi lớp thỏa mãn điều kiện gửi nhắc, hội viên có tài khoản và chi nhánh đang trong giờ mở cửa.
    - **When** Hệ thống tạo thông báo `CLASS_REMINDER`.
    - **Then** Hệ thống đẩy thông báo `IN_APP` qua WebSocket (STOMP); email được gửi tới `users.email` qua Brevo và cập nhật trạng thái gửi tương ứng.
  - **AC-F10.2.1-06: Xử lý email phát sinh ngoài giờ và lỗi gửi email**
    - **Given** Thông báo nhắc lịch lớp phát sinh ngoài giờ mở cửa hoặc dịch vụ gửi email (Brevo) gặp lỗi.
    - **When** Hệ thống xử lý thông báo.
    - **Then** Nếu ngoài giờ mở cửa, email được đặt lịch gửi vào giờ mở cửa kế tiếp, thông báo trong web vẫn hiển thị ngay. Nếu gửi thất bại, dòng `EMAIL` chuyển `FAILED`, không làm mất thông báo `IN_APP`.
- **Ghi chú cần chốt:** Khoảng thời gian gửi thông báo trước giờ bắt đầu lớp chưa được xác định trong `ban-chot-pham-vi.md` và danh sách khóa cấu hình của ERD. Không tự đặt số phút hoặc giá trị mặc định cho đến khi nhóm thống nhất.

---

### 3.3. UC-F10.3: Đã bỏ (ADR 0014)

Use case "Gửi thông báo được đưa vào lớp từ danh sách chờ" không còn trong phạm vi vì nhóm đã bỏ danh sách chờ lớp học nhóm theo ADR 0014. Mã UC-F10.3 giữ lại trong bảng mục 2 để các UC khác không đổi mã. Không còn user story US-F10.3.1, tiêu chí chấp nhận, loại thông báo hay quy tắc nào của use case này.

---

### 3.4. UC-F10.4: Gửi thông báo đổi giờ lớp

- **Mã UC:** UC-F10.4
- **Tên Use Case:** Gửi thông báo đổi giờ lớp
- **Tác nhân chính:** Hệ thống (kích hoạt khi nhận sự kiện đổi giờ lớp từ F8)
- **Tác nhân phụ (nếu có):** Dịch vụ gửi email (Brevo); Hội viên (người nhận)
- **Mô tả ngắn gọn:** Khi giờ của buổi lớp đã đặt bị thay đổi, hệ thống báo cho các hội viên có chỗ trong buổi đó biết giờ cũ và giờ mới.

#### A. Tiền điều kiện (Preconditions)

- Một buổi lớp (`class_sessions`) có hội viên đã đặt chỗ (`BOOKED`) bị đổi giờ bởi người có quyền (trạng thái buổi chuyển RESCHEDULED), và F8 gửi sự kiện đổi giờ cho F10.

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Mỗi hội viên có tài khoản và có chỗ trong buổi lớp nhận hai dòng `notifications` (`type` = `CLASS_RESCHEDULED`) nêu rõ giờ cũ (`original_start_at`) và giờ mới (`start_at`).
- **Thất bại:** Việc đổi giờ lớp không bị rollback chỉ vì gửi thông báo lỗi.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Người có quyền đổi giờ buổi lớp; F8 lưu giờ mới, ghi giờ cũ vào `original_start_at` và gửi sự kiện đổi giờ cho F10.
2. F10 nhận sự kiện và lấy danh sách hội viên có lượt đặt chỗ `BOOKED` của buổi lớp đó (`class_bookings`).
3. Hệ thống tạo hai dòng `notifications` (kênh `IN_APP` và `EMAIL`) cho từng hội viên có tài khoản, ghi giờ cũ và giờ mới, rồi đẩy thông báo `IN_APP` tới trình duyệt hội viên đang mở qua WebSocket (STOMP).
4. Email được gửi như UC-F10.1 bước 5.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **A1 – Buổi lớp chưa có hội viên nào:** không có thông báo nào được tạo.
- **A2 – Hội viên chưa có tài khoản:** hệ thống không tạo thông báo cho hội viên này.
- **A3 – Hội viên đã hủy chỗ trước khi đổi giờ:** hệ thống không báo cho hội viên đó (chỉ báo lượt đặt chỗ còn `BOOKED`).
- **E1 – Gửi email thất bại:** giờ lớp vẫn được lưu; dòng email chuyển `FAILED`; thông báo trong web vẫn có **(cơ chế thử lại là đề xuất)**.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F10.1:** Mỗi sự kiện tạo một thông báo trong web và một email.
- **BR-F10.6:** Thông báo phải nêu cả giờ cũ và giờ mới của buổi lớp.
- **BR-F10.3:** Email chỉ gửi trong giờ mở cửa của chi nhánh.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `notifications`; đọc `class_sessions`, `class_bookings`, `members`, `users`, `branches`
- **Chi tiết trường dữ liệu:**

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `class_sessions` | `start_at` | `timestamptz` | NOT NULL | Giờ bắt đầu mới |
| `class_sessions` | `original_start_at` | `timestamptz` | | Giờ cũ, để ghi vào thông báo |
| `class_sessions` | `status` | `varchar` | NOT NULL, default `SCHEDULED` | Chuyển RESCHEDULED khi đổi giờ |
| `class_bookings` | `status` | `varchar` | NOT NULL, CHECK (BOOKED, CANCELLED, ATTENDED, NO_SHOW) | Chỉ lấy lượt BOOKED |
| `notifications` | `type` | `varchar(50)` | NOT NULL | Giá trị `CLASS_RESCHEDULED` |
| `notifications` | `related_entity`, `related_id` | `varchar(50)`, `bigint` | | `class_bookings` và mã đặt chỗ |

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint:** Không có; kích hoạt bởi sự kiện đổi giờ lớp từ F8.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F10.4.1: Nhận thông báo khi lịch lớp học nhóm thay đổi

- **User story:** Là **Hội viên đã đặt chỗ lớp học nhóm**, tôi muốn **nhận thông báo khi buổi lớp đổi giờ**, để **biết giờ cũ và giờ mới của lớp mình đã đặt**.
- **BR liên quan:** BR-F10.1, BR-F10.3, BR-F10.6; tích hợp F8.
- **Version / Sprint:** v2 / Sprint 2.
- **Tiêu chí chấp nhận:**
  - **AC-F10.4.1-01: Tạo thông báo khi nhận sự kiện đổi giờ từ F8** (BR-F10.6)
    - **Given** Buổi lớp "Yoga nâng cao" ngày 08/11/2026 có 8 hội viên đã đặt chỗ (`BOOKED`), tất cả có tài khoản; người có quyền đổi giờ thành công và buổi lớp chuyển sang trạng thái `RESCHEDULED`.
    - **When** F8 gửi sự kiện đổi giờ lớp cho F10.
    - **Then** F10 tạo hai dòng thông báo `IN_APP` và `EMAIL` cho mỗi hội viên trong 8 hội viên đó, với `type = CLASS_RESCHEDULED`, `related_entity = class_bookings` và `related_id` là mã lượt đặt chỗ của từng người.
  - **AC-F10.4.1-02: Nội dung thông báo có giờ cũ và giờ mới** (BR-F10.6)
    - **Given** Buổi lớp đổi từ 17:00 sang 18:00 ngày 08/11/2026 (`original_start_at = 17:00`, `start_at = 18:00`).
    - **When** Hệ thống tạo thông báo đổi giờ lớp.
    - **Then** Nội dung thông báo thể hiện rõ giờ bắt đầu cũ 17:00 và giờ bắt đầu mới 18:00; không có thời hạn hay bước xác nhận lại nào trong thông báo.
  - **AC-F10.4.1-03: Không tạo thông báo khi buổi lớp không có hội viên đặt chỗ**
    - **Given** Buổi lớp được đổi giờ nhưng không có lượt đặt chỗ nào ở trạng thái `BOOKED`.
    - **When** Hệ thống xử lý sự kiện đổi giờ lớp.
    - **Then** Không có dòng thông báo nào được tạo.
  - **AC-F10.4.1-04: Chỉ báo hội viên còn giữ chỗ**
    - **Given** Buổi lớp có hai lượt đặt chỗ: hội viên An còn `BOOKED`, hội viên Bình đã `CANCELLED` trước khi đổi giờ.
    - **When** F10 xử lý sự kiện đổi giờ lớp.
    - **Then** Chỉ An nhận hai dòng thông báo `IN_APP` và `EMAIL`; Bình không nhận thông báo nào.
  - **AC-F10.4.1-05: Không tạo thông báo cho hội viên chưa có tài khoản**
    - **Given** Hội viên có lượt đặt chỗ `BOOKED` trong buổi lớp được đổi giờ nhưng `members.user_id` không có giá trị.
    - **When** F10 xử lý danh sách người nhận thông báo.
    - **Then** Hệ thống bỏ qua hội viên này, không tạo hai dòng `notifications`; việc đổi giờ và lượt đặt chỗ do F8 xử lý vẫn được giữ nguyên.
  - **AC-F10.4.1-06: Gửi thông báo qua hai kênh và xử lý lỗi email**
    - **Given** Buổi lớp đã đổi giờ thành công, hội viên có tài khoản và dịch vụ gửi email (Brevo) có thể hoạt động hoặc gặp lỗi.
    - **When** F10 tạo và gửi thông báo `CLASS_RESCHEDULED`.
    - **Then** Thông báo `IN_APP` được đẩy qua WebSocket (STOMP); email được gửi trong giờ mở cửa hoặc đặt lịch vào giờ mở cửa kế tiếp. Nếu gửi email thất bại, dòng `EMAIL` chuyển `FAILED`, nhưng việc đổi giờ lớp không bị rollback và thông báo `IN_APP` vẫn được giữ nguyên.

---

### 3.5. UC-F10.5: Xem thông báo trong web

- **Mã UC:** UC-F10.5
- **Tên Use Case:** Xem thông báo trong web
- **Tác nhân chính:** Hội viên
- **Tác nhân phụ (nếu có):** Không
- **Mô tả ngắn gọn:** Hội viên xem các thông báo đã nhận trong cổng hội viên và biết thông báo nào chưa đọc.

#### A. Tiền điều kiện (Preconditions)

- Người dùng đã đăng nhập với vai trò Hội viên.

#### B. Hậu điều kiện (Postconditions)

- **Thành công:** Hệ thống hiển thị danh sách thông báo kênh `IN_APP` của hội viên. Khi hội viên mở một thông báo, `read_at` được ghi nhận.
- **Thất bại:** Không có dữ liệu nào bị thay đổi.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Hội viên mở mục "Thông báo" trong cổng hội viên.
2. Hệ thống kiểm tra tham số phân trang (Bean Validation).
3. Hệ thống kiểm tra quyền và vai trò (S3); tài khoản lấy từ token.
4. Hệ thống truy vấn các thông báo kênh `IN_APP` có `user_id` là tài khoản đó, mới nhất trước.
5. Hệ thống trả về kết quả (200 OK) dạng `PageResponse` gồm: loại, tiêu đề, nội dung, thời điểm, trạng thái đã đọc (`read_at` khác rỗng là đã đọc), và đối tượng liên quan (`related_entity`, `related_id`) để mở đúng màn hình.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)

- **A1 – Chưa có thông báo nào:** trả danh sách rỗng; giao diện hiển thị "Bạn chưa có thông báo".
- **A2 – Đánh dấu đã đọc:**
  - Khi hội viên mở một thông báo chưa đọc, hệ thống ghi `read_at` bằng thời điểm hiện tại (chỉ ghi nếu thông báo thuộc về chính hội viên đó).
- **A3 – Thông báo mới đến khi đang mở web:**
  - Hệ thống đẩy thông báo qua WebSocket (STOMP); giao diện hiển thị ngay, không cần tải lại trang.
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):** tham số phân trang sai; trả `ProblemDetail` (400).
- **E2 – Không có quyền truy cập (403 Forbidden):** người thao tác không phải Hội viên, hoặc đánh dấu đã đọc thông báo của người khác; trả `ProblemDetail` (403).
- **E3 – Không tìm thấy (404 Not Found):** thông báo cần đánh dấu đã đọc không tồn tại.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F10.7:** Hội viên chỉ xem và đánh dấu đã đọc thông báo của chính mình (S3).
- **BR-F10.8:** Trạng thái đã đọc lưu ở cột `read_at` (`timestamptz`) của bảng `notifications`: khác rỗng là đã đọc, rỗng là chưa đọc.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động:** `notifications` (đọc; ghi `read_at` khi đánh dấu đã đọc)
- **Chi tiết trường dữ liệu:**

| Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
| --- | --- | --- | --- | --- |
| `notifications` | `user_id` | `bigint` | NOT NULL, FK → `users.id` | Chủ thông báo |
| `notifications` | `channel` | `varchar` | NOT NULL, CHECK (IN_APP, EMAIL) | Chỉ lấy `IN_APP` |
| `notifications` | `type`, `title`, `content` | `varchar(50)`, `varchar(255)`, `text` | `type`, `title` NOT NULL | Nội dung hiển thị |
| `notifications` | `related_entity`, `related_id` | `varchar(50)`, `bigint` | | Đối tượng liên quan |
| `notifications` | `read_at` | `timestamptz` | | Rỗng = chưa đọc |
| `notifications` | `created_at` | `timestamptz` | NOT NULL | Thời điểm tạo; chỉ mục `(user_id, read_at)` hỗ trợ lọc chưa đọc |

#### G. Thiết kế giao diện và API liên quan

- **API Endpoint:** `GET /api/v1/me/notifications?page=0&size=20`; `PATCH /api/v1/me/notifications/{id}/read` **(đề xuất)**; kênh WebSocket (STOMP) để nhận thông báo mới **(đề xuất, thống nhất với người làm backend)**
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body:** Không có.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F10.5.1: Xem và quản lý trạng thái đã đọc của thông báo

- **User story:** Là **Hội viên**, tôi muốn **xem danh sách thông báo trong cổng hội viên và biết thông báo nào đã đọc hoặc chưa đọc**, để **theo dõi các thông tin quan trọng về gói tập và lịch học**.
- **BR liên quan:** BR-F10.7, BR-F10.8; S3, S5.
- **Version / Sprint:** v2 / Sprint 2.
- **Tiêu chí chấp nhận:**
  - **AC-F10.5.1-01: Xem danh sách thông báo của tài khoản đang đăng nhập** (BR-F10.7, S3)
    - **Given** Hội viên đã đăng nhập hợp lệ vào cổng hội viên và tài khoản có các thông báo trong bảng `notifications`.
    - **When** Hội viên truy cập màn hình "Thông báo".
    - **Then** Hệ thống chỉ trả về các thông báo có `user_id` bằng ID tài khoản lấy từ token và `channel = IN_APP`; không trả về thông báo của tài khoản khác hoặc các dòng kênh `EMAIL`.
  - **AC-F10.5.1-02: Sắp xếp và phân trang danh sách thông báo** (S5)
    - **Given** Hội viên có nhiều thông báo `IN_APP`.
    - **When** Hội viên truy cập danh sách thông báo với tham số phân trang hợp lệ, ví dụ `page = 0`, `size = 20`.
    - **Then** Hệ thống trả về `200 OK` theo cấu trúc `PageResponse`, sắp xếp thông báo mới nhất trước và trả về đúng số bản ghi theo tham số phân trang.
  - **AC-F10.5.1-03: Hiển thị đầy đủ nội dung và trạng thái đã đọc**
    - **Given** Hội viên có các thông báo với `read_at` khác nhau.
    - **When** Hệ thống trả danh sách thông báo cho giao diện.
    - **Then** Mỗi thông báo hiển thị loại, tiêu đề, nội dung, thời điểm tạo, đối tượng liên quan (`related_entity`, `related_id`) và trạng thái đọc; `read_at` khác rỗng là đã đọc, `read_at` rỗng là chưa đọc.
  - **AC-F10.5.1-04: Đánh dấu thông báo đã đọc**
    - **Given** Hội viên đang sở hữu một thông báo `IN_APP` chưa đọc (`read_at IS NULL`).
    - **When** Hội viên mở thông báo hoặc gọi API `PATCH /api/v1/me/notifications/{id}/read`.
    - **Then** Hệ thống cập nhật `read_at` bằng thời điểm hiện tại; thông báo chuyển sang trạng thái đã đọc trên giao diện. Chỉ cập nhật nếu thông báo thuộc chính tài khoản đang đăng nhập.
  - **AC-F10.5.1-05: Không cho phép xem hoặc đánh dấu thông báo của người khác** (BR-F10.7, S3)
    - **Given** Hội viên A đăng nhập và gửi yêu cầu truy cập hoặc đánh dấu đã đọc thông báo thuộc tài khoản hội viên B.
    - **When** Hội viên A gọi API tương ứng.
    - **Then** Hệ thống trả về `403 Forbidden` theo quy tắc phân quyền; không tiết lộ nội dung thông báo của hội viên B và không thay đổi `read_at`.
  - **AC-F10.5.1-06: Xử lý dữ liệu đầu vào sai và thông báo không tồn tại**
    - **Given** Hội viên gửi tham số phân trang không hợp lệ (`page < 0`, `size < 1` hoặc `size > 100`), hoặc yêu cầu đánh dấu một thông báo không tồn tại.
    - **When** Hệ thống xử lý yêu cầu xem hoặc đánh dấu đã đọc.
    - **Then** Tham số phân trang sai trả về `400 Bad Request`; thông báo không tồn tại trả về `404 Not Found`. Nếu hội viên chưa có thông báo nào, hệ thống trả danh sách rỗng với `200 OK` và giao diện hiển thị "Bạn chưa có thông báo".
