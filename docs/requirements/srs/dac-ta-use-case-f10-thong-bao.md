# Đặc tả Use Case – F10: Thông báo

- **Mã chức năng:** F10
- **Version:** v2 (Bắt buộc)
- **Người biên soạn:** Nguyễn Trọng Việt
- **Người duyệt chéo:** Lê Phước Minh Quân
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
- **Email (`EMAIL`):** gửi qua SMTP (Spring Mail) tới địa chỉ `users.email` của hội viên, chỉ trong giờ mở cửa của chi nhánh liên quan (`branches.open_time` đến `close_time`); thông báo email phát sinh ngoài giờ được hẹn gửi vào giờ mở cửa (`scheduled_at`).

Có 4 loại thông báo: sắp hết hạn gói (`MEMBERSHIP_EXPIRING`), nhắc lịch lớp (`CLASS_REMINDER`), được đưa vào lớp từ danh sách chờ (`CLASS_PROMOTED`), đổi giờ lớp (`CLASS_RESCHEDULED`). ERD còn khai báo loại `CLASS_CANCELLED` (hủy buổi lớp); loại này không nằm trong danh sách yêu cầu của F10, được coi là thuộc phạm vi F8.

Người nhận là **Hội viên có tài khoản** (thông báo gắn với `users.id`). Ở Sprint 1, tài liệu này chỉ đặc tả; phần hiện thực làm ở Sprint 2.

Mỗi sự kiện tạo **hai dòng** trong bảng `notifications`: một dòng kênh `IN_APP`, một dòng kênh `EMAIL`.

---

## 2. Danh sách Use Case

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F10.1** | Gửi thông báo sắp hết hạn gói | Hệ thống (tự động) | Cao | Sprint 2 |
| **UC-F10.2** | Gửi thông báo nhắc lịch lớp | Hệ thống (tự động) | Trung bình | Sprint 2 |
| **UC-F10.3** | Gửi thông báo được đưa vào lớp từ danh sách chờ | Hệ thống (tự động) | Cao | Sprint 2 |
| **UC-F10.4** | Gửi thông báo đổi giờ lớp | Hệ thống (tự động) | Cao | Sprint 2 |
| **UC-F10.5** | Xem thông báo trong web | Hội viên | Trung bình | Sprint 2 |

---

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F10.1: Gửi thông báo sắp hết hạn gói

- **Mã UC:** UC-F10.1
- **Tên Use Case:** Gửi thông báo sắp hết hạn gói
- **Tác nhân chính:** Hệ thống (tác vụ chạy định kỳ)
- **Tác nhân phụ (nếu có):** Dịch vụ gửi email; Hội viên (người nhận)
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
5. Tác vụ gửi email quét các dòng `EMAIL` có `status` = `PENDING` và `scheduled_at` đã đến, gửi tới `users.email` qua SMTP, rồi cập nhật `status` = `SENT` và `sent_at`.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Hội viên chưa có tài khoản:**
  - Tại bước 3, vì `notifications.user_id` bắt buộc có giá trị, hệ thống không tạo thông báo cho hội viên này.
- **A2 – Email phát sinh ngoài giờ mở cửa:**
  - Tại bước 4, hệ thống đặt `scheduled_at` vào giờ mở cửa kế tiếp; thông báo trong web hiển thị ngay.
- **A3 – Hội viên không mở web lúc có thông báo:**
  - Tại bước 4, không có kết nối WebSocket nào; thông báo vẫn lưu trong `notifications` và hội viên thấy khi mở web (UC-F10.5).
- **E1 – Gửi email thất bại:**
  - Tại bước 5, dịch vụ email lỗi. Dòng email chuyển `FAILED`, thông báo trong web không bị ảnh hưởng *(cơ chế thử lại là đề xuất, cần nhóm xác nhận)*.
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

---

### 3.2. UC-F10.2: Gửi thông báo nhắc lịch lớp

- **Mã UC:** UC-F10.2
- **Tên Use Case:** Gửi thông báo nhắc lịch lớp
- **Tác nhân chính:** Hệ thống (tác vụ chạy định kỳ)
- **Tác nhân phụ (nếu có):** Dịch vụ gửi email; Hội viên (người nhận)
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
- **E1 – Gửi email thất bại:** dòng email chuyển `FAILED`; thông báo trong web vẫn có *(cơ chế thử lại là đề xuất)*.
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
  | `class_bookings` | `status` | `varchar` | NOT NULL, CHECK (BOOKED, WAITLIST, PENDING_RECONFIRM, CANCELLED, ATTENDED, NO_SHOW) | Chỉ nhắc khi BOOKED |
  | `class_sessions` | `start_at` | `timestamptz` | NOT NULL | Giờ bắt đầu buổi lớp |
  | `class_sessions` | `status` | `varchar` | NOT NULL, default `SCHEDULED` | Không nhắc khi CANCELLED |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** Không có; tác vụ nền.

---

### 3.3. UC-F10.3: Gửi thông báo được đưa vào lớp từ danh sách chờ

- **Mã UC:** UC-F10.3
- **Tên Use Case:** Gửi thông báo được đưa vào lớp từ danh sách chờ
- **Tác nhân chính:** Hệ thống (kích hoạt khi có chỗ trống)
- **Tác nhân phụ (nếu có):** Dịch vụ gửi email; Hội viên (người nhận)
- **Mô tả ngắn gọn:** Khi có chỗ trống và một hội viên trong danh sách chờ được tự động đưa vào lớp, hệ thống báo cho hội viên đó.

#### A. Tiền điều kiện (Preconditions)
- Hội viên có tài khoản và đang ở trạng thái WAITLIST trong `class_bookings` của một buổi lớp.
- Buổi lớp có chỗ trống, còn từ `PROMOTION_CUTOFF_MIN` phút trở lên trước giờ học, và hội viên là người đủ điều kiện đứng đầu danh sách chờ (xếp theo `waitlisted_at`, đến trước xếp trước).

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Lượt đặt chỗ chuyển sang BOOKED, `promoted_at` được ghi nhận. Hai dòng `notifications` được tạo (`type` = `CLASS_PROMOTED`, `related_entity` = `class_bookings`, `related_id` = mã đặt chỗ). Thông báo nêu lớp, giờ học và thời hạn hủy không phạt (`grace_until`).
- **Thất bại:** Việc đưa vào lớp không bị ảnh hưởng nếu chỉ riêng việc gửi thông báo lỗi.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Có chỗ trống trong buổi lớp (ví dụ hội viên khác hủy chỗ).
2. Hệ thống chọn lượt WAITLIST đứng đầu và đủ điều kiện theo `waitlisted_at`, có khóa bản ghi để chống xung đột (NF6).
3. Hệ thống cập nhật lượt đặt chỗ sang BOOKED, ghi `promoted_at` (việc đưa vào lớp thuộc F8; F10 chỉ gửi thông báo).
4. Hệ thống tạo hai dòng `notifications` (kênh `IN_APP` và `EMAIL`) trong cùng giao dịch, rồi đẩy thông báo `IN_APP` tới trình duyệt hội viên đang mở qua WebSocket (STOMP).
5. Email được gửi như UC-F10.1 bước 5 (chỉ trong giờ mở cửa chi nhánh); hạn hủy không phạt `grace_until` bằng thời điểm gửi email cộng `PROMOTION_GRACE_MIN` (mặc định 15 phút), theo ERD.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Hội viên chưa có tài khoản:** hệ thống không tạo thông báo (hội viên vẫn được đưa vào lớp).
- **A2 – Chỗ trống phát sinh ngoài giờ mở cửa:** vẫn đưa vào lớp ngay (trước mốc `PROMOTION_CUTOFF_MIN`); thông báo trong web hiển thị ngay, email gửi khi tới giờ mở cửa, ân hạn tính từ lúc gửi email.
- **E1 – Gửi email thất bại:** hội viên vẫn ở trong lớp; dòng email chuyển `FAILED`; thông báo trong web vẫn có *(cơ chế thử lại là đề xuất)*.
- **E4 – Xung đột đồng thời (409):** hai chỗ trống xuất hiện cùng lúc; hệ thống khóa `members` rồi `class_sessions` (NF6) để không đưa cùng một người vào hai lần hoặc vượt sĩ số.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F10.5:** Theo `ban-chot-pham-vi.md`, mục "Lớp học nhóm (F8)": thứ tự đến trước xếp trước (`waitlisted_at`); hệ thống tự động đưa vào lớp, không cần xác nhận; người không còn đủ điều kiện (gói hết hạn, đang bị khóa đặt lớp, đã có lịch trùng giờ) bị bỏ qua; ngừng tự động đưa vào lớp khi còn dưới `PROMOTION_CUTOFF_MIN` phút trước giờ học.
- **BR-F10.1:** Mỗi sự kiện tạo một thông báo trong web và một email.
- **CONFIG-F10.2:** `PROMOTION_GRACE_MIN` (mặc định `15`) và `PROMOTION_CUTOFF_MIN` (mặc định `30`) đọc từ `system_configs`, không viết cứng (Quy tắc số 15).
- **[Yêu cầu phi chức năng]:**
  - Toàn vẹn đồng thời (NF6): không đưa cùng một hội viên vào lớp hai lần.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `class_bookings`, `notifications`; đọc `class_sessions`, `members`, `users`, `branches`, `system_configs`
- **Chi tiết trường dữ liệu:**
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `class_bookings` | `status` | `varchar` | NOT NULL | WAITLIST chuyển sang BOOKED |
  | `class_bookings` | `waitlisted_at` | `timestamptz` | | Căn cứ thứ tự chờ |
  | `class_bookings` | `promoted_at` | `timestamptz` | | Thời điểm được tự động đưa vào lớp |
  | `class_bookings` | `grace_until` | `timestamptz` | | Hết ân hạn hủy không phạt |
  | `notifications` | `type` | `varchar(50)` | NOT NULL | Giá trị `CLASS_PROMOTED` |
  | `notifications` | `channel`, `status`, `scheduled_at`, `sent_at` | như UC-F10.1 | | |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** Không có; kích hoạt bởi nghiệp vụ lớp học (F8).

---

### 3.4. UC-F10.4: Gửi thông báo đổi giờ lớp

- **Mã UC:** UC-F10.4
- **Tên Use Case:** Gửi thông báo đổi giờ lớp
- **Tác nhân chính:** Hệ thống (kích hoạt khi buổi lớp bị đổi giờ)
- **Tác nhân phụ (nếu có):** Dịch vụ gửi email; Hội viên (người nhận)
- **Mô tả ngắn gọn:** Khi giờ của buổi lớp đã đặt bị thay đổi, hệ thống báo cho các hội viên có chỗ trong buổi đó, kèm hạn xác nhận lại.

#### A. Tiền điều kiện (Preconditions)
- Một buổi lớp (`class_sessions`) có hội viên đã đặt chỗ bị đổi giờ bởi người có quyền (trạng thái buổi chuyển RESCHEDULED); F8 đưa các lượt đặt chỗ sang trạng thái PENDING_RECONFIRM.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Mỗi hội viên có tài khoản và có chỗ trong buổi lớp nhận hai dòng `notifications` (`type` = `CLASS_RESCHEDULED`) nêu rõ giờ cũ (`original_start_at`), giờ mới (`start_at`) và hạn xác nhận lại (`reconfirm_expires_at`).
- **Thất bại:** Việc đổi giờ lớp không bị rollback chỉ vì gửi thông báo lỗi.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Người có quyền đổi giờ buổi lớp và lưu thành công; hệ thống ghi giờ cũ vào `original_start_at`.
2. Hệ thống lấy danh sách hội viên có chỗ trong buổi lớp đó (`class_bookings`).
3. Hệ thống tạo hai dòng `notifications` (kênh `IN_APP` và `EMAIL`) cho từng hội viên có tài khoản, ghi giờ cũ, giờ mới và hạn xác nhận lại (`RECONFIRM_TTL_MIN` phút), rồi đẩy thông báo `IN_APP` tới trình duyệt hội viên đang mở qua WebSocket (STOMP).
4. Email được gửi như UC-F10.1 bước 5.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Buổi lớp chưa có hội viên nào:** không có thông báo nào được tạo.
- **A2 – Hội viên chưa có tài khoản:** hệ thống không tạo thông báo cho hội viên này.
- **E1 – Gửi email thất bại:** giờ lớp vẫn được lưu; dòng email chuyển `FAILED`; thông báo trong web vẫn có *(cơ chế thử lại là đề xuất)*.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F10.1:** Mỗi sự kiện tạo một thông báo trong web và một email.
- **BR-F10.6:** Thông báo phải nêu cả giờ cũ, giờ mới và hạn xác nhận lại của buổi lớp.
- **BR-F10.3:** Email chỉ gửi trong giờ mở cửa của chi nhánh.
- **CONFIG-F10.3:** Hạn xác nhận lại sau khi đổi giờ đọc từ `system_configs` với key `RECONFIRM_TTL_MIN` (giá trị mặc định: `60`). Theo `ban-chot-pham-vi.md`: không xác nhận hoặc báo không tham gia thì mất chỗ, không bị phạt, chỗ chuyển cho danh sách chờ.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `notifications`; đọc `class_sessions`, `class_bookings`, `members`, `users`, `branches`, `system_configs`
- **Chi tiết trường dữ liệu:**
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `class_sessions` | `start_at` | `timestamptz` | NOT NULL | Giờ bắt đầu mới |
  | `class_sessions` | `original_start_at` | `timestamptz` | | Giờ cũ, để ghi vào thông báo |
  | `class_sessions` | `status` | `varchar` | NOT NULL, default `SCHEDULED` | Chuyển RESCHEDULED khi đổi giờ |
  | `class_bookings` | `status` | `varchar` | NOT NULL | PENDING_RECONFIRM khi chờ xác nhận lại |
  | `class_bookings` | `reconfirm_expires_at` | `timestamptz` | | Hạn xác nhận lại khi đổi giờ |
  | `notifications` | `type` | `varchar(50)` | NOT NULL | Giá trị `CLASS_RESCHEDULED` |
  | `notifications` | `related_entity`, `related_id` | `varchar(50)`, `bigint` | | `class_bookings` và mã đặt chỗ |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** Không có; kích hoạt bởi chức năng đổi giờ lớp (F8).

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
- **API Endpoint:** `GET /api/v1/me/notifications?page=0&size=20`; `PATCH /api/v1/me/notifications/{id}/read` *(đề xuất)*; kênh WebSocket (STOMP) để nhận thông báo mới *(đề xuất, thống nhất với người làm backend)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body:** Không có.
