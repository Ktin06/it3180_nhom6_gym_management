- **Cập nhật 23/11/2026 (Nguyễn Trọng Việt):** viết lại mục H của UC-F7.1 đến UC-F7.5 theo cây truy vết ([ADR 0012](../../decisions/0012-quy-uoc-ma-truy-vet.md)): 5 user story, 34 AC có mã.
# Đặc tả Use Case – F7: Cổng hội viên
- **Mã chức năng:** F7
- **Version:** v2 (Bắt buộc)
- **Người biên soạn:** Nguyễn Trọng Việt
- **Người duyệt chéo:** Lê Phước Minh Quân
- **Cập nhật 23/11/2026 (Nguyễn Trọng Việt):** viết lại mục H của UC-F7.1 đến UC-F7.5 theo cây truy vết ([ADR 0012](../../decisions/0012-quy-uoc-ma-truy-vet.md)): 5 user story, 34 AC có mã.
- **Tài liệu chuẩn tham chiếu:**
   - [Bản chốt phạm vi](../ban-chot-pham-vi.md)
   - [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml)
   - [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md)
   - [Quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md)
   - [ADR liên quan](../../decisions/)
**---**

## 1. Tổng quan chức năng

Cổng hội viên là khu vực riêng để hội viên tự theo dõi tình trạng của mình mà không cần đến quầy lễ tân. Chức năng gồm hai nhóm năng lực:
- **Xem thông tin của chính mình:** gói tập, lịch sử check-in, hóa đơn.
- **Gửi yêu cầu mua hoặc gia hạn gói:** hệ thống tạo hóa đơn chờ (nguồn `MEMBER_PORTAL`, trạng thái PENDING) và gói ở trạng thái PENDING; hội viên thanh toán tại quầy, lễ tân hoặc quản lý ghi nhận thanh toán (S7).
Vai trò được phép dùng: **Hội viên** (đã đăng nhập, có hồ sơ `members` liên kết với tài khoản qua `members.user_id`). Hội viên chỉ xem được dữ liệu của chính mình. Ở Sprint 1, tài liệu này chỉ đặc tả; phần hiện thực làm ở Sprint 2.

**---**

## 2. Danh sách Use Case
| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F7.1** | Xem gói tập | Hội viên | Trung bình | Sprint 2 |
| **UC-F7.2** | Xem lịch sử check-in | Hội viên | Trung bình | Sprint 2 |
| **UC-F7.3** | Xem hóa đơn | Hội viên | Trung bình | Sprint 2 |
| **UC-F7.4** | Gửi yêu cầu mua gói | Hội viên | Cao | Sprint 2 |
| **UC-F7.5** | Gửi yêu cầu gia hạn gói | Hội viên | Cao | Sprint 2 |
**---**

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F7.1: Xem gói tập
- **Mã UC:** UC-F7.1
- **Tên Use Case:** Xem gói tập
- **Tác nhân chính:** Hội viên
- **Tác nhân phụ (nếu có):** Không
- **Mô tả ngắn gọn:** Hội viên xem các gói tập của mình cùng trạng thái, ngày hiệu lực và số buổi còn lại.
#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Hội viên và có hồ sơ `members` liên kết với tài khoản.
#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Hệ thống hiển thị danh sách gói của hội viên. Không có dữ liệu nào bị thay đổi.
- **Thất bại:** Không có dữ liệu nào bị thay đổi.
#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Hội viên mở mục "Gói của tôi" trên cổng hội viên.

2. Hệ thống kiểm tra quyền và vai trò của người thao tác (S3); hội viên được xác định từ token qua `members.user_id`, không lấy mã hội viên từ request.

3. Hệ thống truy vấn các gói (`memberships`) của hội viên đó, kèm tên và loại gói từ `packages`.

4. Hệ thống trả về kết quả thành công (200 OK) gồm: tên gói, loại gói, trạng thái, ngày bắt đầu, ngày hết hạn, số buổi còn lại (với gói theo buổi và gói PT).

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Hội viên chưa có gói nào:**
   - Tại bước 3, nếu không có gói nào, hệ thống trả danh sách rỗng và giao diện hiển thị "Bạn chưa có gói tập".
- **E1 – Chưa đăng nhập hoặc token hết hạn (401 Unauthorized):**
   - Tại bước 2, JWT không có hoặc hết hạn; hệ thống trả `ProblemDetail` (401): **"Bạn cần đăng nhập để truy cập tài nguyên này."**
- **E2 – Không có quyền truy cập tài nguyên (403 Forbidden):**
   - Tại bước 2, người thao tác không phải Hội viên hoặc tài khoản chưa liên kết với hồ sơ hội viên.
   - Hệ thống trả `ProblemDetail` (403): **"Bạn không có quyền thực hiện thao tác này."**
#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F7.1:** Hội viên chỉ xem được dữ liệu của chính mình (S3).
- **BR-F7.2:** Hiển thị nhãn theo trạng thái gói: PENDING = "Chờ thanh toán", UPCOMING = "Chưa đến ngày bắt đầu", FROZEN = "Đang bảo lưu", CANCELLED = "Đã hủy".
- **[Yêu cầu phi chức năng]:**
   - Bảo mật (S2, S3): kiểm tra JWT, phân quyền ở backend.
#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động (chỉ đọc):** `members`, `memberships`, `packages`
- **Chi tiết trường dữ liệu:**
   | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
   | --- | --- | --- | --- | --- |
   | `members` | `user_id` | `bigint` | UNIQUE, FK → `users.id` | Liên kết tài khoản với hồ sơ hội viên |
   | `memberships` | `member_id` | `bigint` | NOT NULL, FK → `members.id` | Chủ sở hữu gói |
   | `memberships` | `package_id` | `bigint` | NOT NULL, FK → `packages.id` | Gói đã mua |
   | `memberships` | `package_type` | `varchar` | NOT NULL, CHECK (TIME, SESSION, PT) | Loại gói, chép từ `packages.type` |
   | `memberships` | `status` | `varchar` | NOT NULL, default `PENDING`, CHECK (PENDING, UPCOMING, ACTIVE, FROZEN, EXPIRED, TRANSFERRED, CANCELLED) | Trạng thái gói |
   | `memberships` | `start_date` | `date` | NOT NULL | Ngày bắt đầu |
   | `memberships` | `end_date` | `date` | NOT NULL, CHECK `end_date >= start_date` | Ngày cuối còn dùng được |
   | `memberships` | `total_sessions` | `int` | | Tổng số buổi, chép từ `packages.session_count` |
   | `memberships` | `remaining_sessions` | `int` | CHECK `>= 0` | Số buổi còn lại |
   | `packages` | `name` | `varchar(255)` | NOT NULL | Tên gói |
#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `GET /api/v1/me/memberships` **(đề xuất, thống nhất với người làm API)**
- **Header:** `Authorization: Bearer \<access_token>`
- **Request Body:** Không có.
#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F7.1.1: Xem gói của tôi
- **User story:** Là **Hội viên**, tôi muốn **xem các gói tập của mình cùng trạng thái, ngày hiệu lực và số buổi còn lại**, để **biết còn dùng được bao lâu và khi nào cần gia hạn**.
- **BR liên quan:** BR-F7.1, BR-F7.2, BR-F7.9; S2, S3
- **Version / Sprint:** v2 / Sprint 2
- **Issue:** #150
- **Tiêu chí chấp nhận:**
   - **AC-F7.1.1-01: Hội viên thấy đủ thông tin các gói của mình** (BR-F7.2)
      - **Given** Hội viên An đã đăng nhập và có hồ sơ hội viên; An có gói TIME `ACTIVE` (01/11/2026 – 29/01/2027), gói PT `ACTIVE` còn 7/10 buổi và một gói TIME `PENDING`
      - **When** Gọi `GET /api/v1/me/memberships`
      - **Then** `200 OK`; mỗi gói có tên gói, `packageType`, `status`, `startDate`, `endDate`; gói SESSION và PT có thêm `remainingSessions`, `totalSessions` (gói TIME để rỗng). Giao diện đổi `status` thành nhãn: `PENDING` "Chờ thanh toán", `UPCOMING` "Chưa đến ngày bắt đầu", `FROZEN` "Đang bảo lưu", `CANCELLED` "Đã hủy"
   - **AC-F7.1.1-02: Chỉ thấy gói của chính mình** (BR-F7.1, S3)
      - **Given** Hai hội viên An và Bình đều có gói; An đã đăng nhập
      - **When** An gọi `GET /api/v1/me/memberships`, kể cả khi tự gửi thêm `memberId` của Bình
      - **Then** `200 OK`; danh sách chỉ gồm gói của An; tham số `memberId` bị bỏ qua vì mã hội viên lấy từ token
   - **AC-F7.1.1-03: Hội viên chưa có gói nào**
      - **Given** Hội viên mới đăng ký, chưa mua gói
      - **When** Gọi `GET /api/v1/me/memberships`
      - **Then** `200 OK` với danh sách rỗng; giao diện hiển thị **"Bạn chưa có gói tập"**
   - **AC-F7.1.1-04: Gói đã hủy không có nút Gia hạn, gói hết hạn vẫn có** (BR-F7.9)
      - **Given** Hội viên có một gói `CANCELLED` và một gói `EXPIRED`
      - **When** Mở màn hình "Gói của tôi"
      - **Then** Gói `CANCELLED` hiển thị nhãn "Đã hủy" và không có nút "Gia hạn"; gói `EXPIRED` vẫn có nút "Gia hạn" (xem AC-F7.5.1-03)
   - **AC-F7.1.1-05: Từ chối người không được xem** (S2, S3)
      - **Given** Người gọi không phải Hội viên (Lễ tân, Huấn luyện viên, Quản lý, Chủ chuỗi), hoặc tài khoản hội viên chưa liên kết hồ sơ hội viên; hoặc không gửi token
      - **When** Gọi `GET /api/v1/me/memberships`
      - **Then** Không có token hoặc token hết hạn trả `401` **"Bạn cần đăng nhập để truy cập tài nguyên này."**; các trường hợp còn lại trả `403` **"Bạn không có quyền thực hiện thao tác này."**
**---**

### 3.2. UC-F7.2: Xem lịch sử check-in
- **Mã UC:** UC-F7.2
- **Tên Use Case:** Xem lịch sử check-in
- **Tác nhân chính:** Hội viên
- **Tác nhân phụ (nếu có):** Không
- **Mô tả ngắn gọn:** Hội viên xem danh sách các lần mình đã check-in, có phân trang.
#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Hội viên và có hồ sơ `members` liên kết với tài khoản.
#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Hệ thống hiển thị lịch sử check-in của hội viên. Không có dữ liệu nào bị thay đổi.
- **Thất bại:** Không có dữ liệu nào bị thay đổi.
#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Hội viên mở mục "Lịch sử check-in".

2. Hệ thống kiểm tra tính hợp lệ của tham số phân trang (Bean Validation).

3. Hệ thống kiểm tra quyền và vai trò (S3); hội viên được xác định từ token.

4. Hệ thống truy vấn các lần check-in (`checkins`) của hội viên, sắp xếp mới nhất trước.

5. Hệ thống trả về kết quả (200 OK) dạng `PageResponse` gồm: thời điểm vào, thời điểm ra (nếu có), chi nhánh.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Chưa có lần check-in nào:**
   - Tại bước 4, hệ thống trả danh sách rỗng; giao diện hiển thị "Chưa có lịch sử check-in".
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):**
   - Tại bước 2, tham số phân trang sai định dạng hoặc ngoài phạm vi.
   - Hệ thống trả `ProblemDetail` (400) kèm danh sách lỗi theo từng trường.
- **E2 – Không có quyền truy cập tài nguyên (403 Forbidden):**
   - Tại bước 3, người thao tác không phải Hội viên.
   - Hệ thống trả `ProblemDetail` (403): **"Bạn không có quyền thực hiện thao tác này."**
- **E3 – Chưa đăng nhập hoặc token hết hạn (401 Unauthorized):**
   - Tại bước 3, JWT không có hoặc hết hạn; hệ thống trả `ProblemDetail` (401): **"Bạn cần đăng nhập để truy cập tài nguyên này."**
#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F7.1:** Hội viên chỉ xem được lịch sử check-in của chính mình (S3).
- **[Yêu cầu phi chức năng]:**
   - Hiệu năng (NF1): tải trang danh sách dưới 2 giây.
#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động (chỉ đọc):** `checkins`, `branches`, `members`
- **Chi tiết trường dữ liệu:**
   | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
   | --- | --- | --- | --- | --- |
   | `checkins` | `member_id` | `bigint` | NOT NULL, FK → `members.id` | Hội viên check-in |
   | `checkins` | `branch_id` | `bigint` | NOT NULL, FK → `branches.id` | Chi nhánh check-in |
   | `checkins` | `checked_in_at` | `timestamptz` | NOT NULL | Thời điểm vào (UTC, hiển thị giờ Việt Nam) |
   | `checkins` | `checked_out_at` | `timestamptz` | | Thời điểm ra |
   | `branches` | `name` | `varchar(255)` | NOT NULL | Tên chi nhánh hiển thị |
#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `GET /api/v1/me/checkins?page=0&size=20` **(đề xuất)**
- **Header:** `Authorization: Bearer \<access_token>`
- **Request Body:** Không có.
#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F7.2.1: Lịch sử check-in của tôi
- **User story:** Là **Hội viên**, tôi muốn **xem lịch sử các lần check-in của mình**, để **theo dõi mình đã đến tập những ngày nào**.
- **BR liên quan:** BR-F7.1; NF1, S2, S3
- **Version / Sprint:** v2 / Sprint 2
- **Issue:** #151
- **Tiêu chí chấp nhận:**
   - **AC-F7.2.1-01: Xem lịch sử mới nhất trước, có phân trang**
      - **Given** Hội viên An đã đăng nhập; An có 25 lượt check-in ở chi nhánh CN01 và CN02
      - **When** Gọi `GET /api/v1/me/checkins?page=0&size=20`
      - **Then** `200 OK` dạng `PageResponse`: 20 lượt mới nhất trước, `totalElements = 25`; mỗi lượt có thời điểm vào, thời điểm ra (nếu có) và tên chi nhánh
   - **AC-F7.2.1-02: Hội viên chưa check-in lần nào**
      - **Given** Hội viên mới, chưa có lượt check-in
      - **When** Gọi `GET /api/v1/me/checkins`
      - **Then** `200 OK` với danh sách rỗng; giao diện hiển thị **"Chưa có lịch sử check-in"**
   - **AC-F7.2.1-03: Chỉ thấy lượt của chính mình** (BR-F7.1, S3)
      - **Given** Hai hội viên An và Bình đều có lượt check-in; An đã đăng nhập
      - **When** An gọi `GET /api/v1/me/checkins`, kể cả khi tự gửi thêm `memberId` của Bình
      - **Then** `200 OK`; danh sách và `totalElements` chỉ gồm lượt của An; `memberId` bị bỏ qua
   - **AC-F7.2.1-04: Tham số phân trang** (S5)
      - **Given** Hội viên An đã đăng nhập
      - **When** Gọi không kèm tham số; hoặc gửi `page < 0`, `size` ngoài khoảng 1–100; hoặc `page` vượt số trang
      - **Then** Không kèm tham số: mặc định `page=0`, `size=20`. `page < 0` hoặc `size` ngoài 1–100 trả `400 Bad Request` kèm lỗi theo từng tham số. `page` vượt số trang trả `200 OK` với danh sách rỗng, không lỗi (cùng quy ước với danh mục gói của F3)
   - **AC-F7.2.1-05: Tải trang dưới 2 giây** (NF1)
      - **Given** Hệ thống có 10.000 hội viên và hàng chục nghìn lượt check-in (dữ liệu mẫu)
      - **When** Hội viên An gọi `GET /api/v1/me/checkins?page=0&size=20`
      - **Then** Phản hồi `200 OK` trong dưới 2 giây
   - **AC-F7.2.1-06: Từ chối người không được xem** (S2, S3)
      - **Given** Người gọi không phải Hội viên (Lễ tân, Huấn luyện viên, Quản lý, Chủ chuỗi), hoặc tài khoản hội viên chưa liên kết hồ sơ hội viên; hoặc không gửi token
      - **When** Gọi `GET /api/v1/me/checkins`
      - **Then** Không có token hoặc token hết hạn trả `401` **"Bạn cần đăng nhập để truy cập tài nguyên này."**; các trường hợp còn lại trả `403` **"Bạn không có quyền thực hiện thao tác này."**
**---**

### 3.3. UC-F7.3: Xem hóa đơn
- **Mã UC:** UC-F7.3
- **Tên Use Case:** Xem hóa đơn
- **Tác nhân chính:** Hội viên
- **Tác nhân phụ (nếu có):** Không
- **Mô tả ngắn gọn:** Hội viên xem danh sách hóa đơn của mình, gồm cả hóa đơn chờ thanh toán do chính mình gửi yêu cầu.
#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Hội viên và có hồ sơ `members` liên kết với tài khoản.
#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Hệ thống hiển thị danh sách hóa đơn của hội viên. Không có dữ liệu nào bị thay đổi.
- **Thất bại:** Không có dữ liệu nào bị thay đổi.
#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Hội viên mở mục "Hóa đơn".

2. Hệ thống kiểm tra tham số phân trang (Bean Validation).

3. Hệ thống kiểm tra quyền và vai trò (S3); hội viên được xác định từ token.

4. Hệ thống truy vấn các hóa đơn (`invoices`) có `member_id` là hội viên đó, mới nhất trước.

5. Hệ thống trả về kết quả (200 OK) dạng `PageResponse` gồm: mã hóa đơn, tổng tiền, trạng thái, ngày tạo, ngày thanh toán, và với hóa đơn chờ do cổng hội viên tạo thì kèm hạn tự hủy.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Chưa có hóa đơn nào:**
   - Tại bước 4, hệ thống trả danh sách rỗng; giao diện hiển thị "Bạn chưa có hóa đơn".
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):**
   - Tại bước 2, tham số phân trang không hợp lệ; hệ thống trả `ProblemDetail` (400).
- **E2 – Không có quyền truy cập tài nguyên (403 Forbidden):**
   - Tại bước 3, người thao tác không phải Hội viên; hệ thống trả `ProblemDetail` (403).
- **E3 – Chưa đăng nhập hoặc token hết hạn (401 Unauthorized):**
   - Tại bước 3, JWT không có hoặc hết hạn; hệ thống trả `ProblemDetail` (401): **"Bạn cần đăng nhập để truy cập tài nguyên này."**
#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F7.1:** Hội viên chỉ xem được hóa đơn của chính mình (S3).
- **BR-F7.3:** Số tiền hiển thị lấy từ dữ liệu do server tính (`invoices.total`), không nhận từ phía client (S7).
- **BR-F7.4:** Hạn tự hủy của hóa đơn chờ do cổng hội viên tạo bằng `created_at` cộng `PENDING_INVOICE_TTL_DAYS` ngày; ERD không có cột `expires_at`, hạn này do server tính khi trả kết quả.
#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động (chỉ đọc):** `invoices`, `invoice_items`, `system_configs`, `members`
- **Chi tiết trường dữ liệu:**
   | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
   | --- | --- | --- | --- | --- |
   | `invoices` | `code` | `varchar(30)` | UNIQUE, NOT NULL | Mã hóa đơn |
   | `invoices` | `member_id` | `bigint` | FK → `members.id` | Hội viên của hóa đơn |
   | `invoices` | `total` | `int` | NOT NULL, CHECK `total = subtotal - discount`, `total >= 0` | Tổng tiền (VND) |
   | `invoices` | `status` | `varchar` | NOT NULL, default `PENDING`, CHECK (PENDING, PAID, CANCELLED, REFUNDED) | Trạng thái hóa đơn |
   | `invoices` | `source` | `varchar` | NOT NULL, CHECK (COUNTER, MEMBER_PORTAL) | Nguồn lập hóa đơn |
   | `invoices` | `paid_at` | `timestamptz` | | Thời điểm thanh toán |
   | `invoices` | `created_at` | `timestamptz` | NOT NULL | Thời điểm tạo (cơ sở tính hạn tự hủy) |
   | `invoice_items` | `description` | `varchar(255)` | NOT NULL | Tên dòng hóa đơn, chép lúc bán |
   | `invoice_items` | `amount` | `int` | NOT NULL, CHECK `amount = quantity * unit_price` | Thành tiền dòng, trước giảm giá |
#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `GET /api/v1/me/invoices?page=0&size=20` **(đề xuất)**
- **Header:** `Authorization: Bearer \<access_token>`
- **Request Body:** Không có.
#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F7.3.1: Hóa đơn của tôi
- **User story:** Là **Hội viên**, tôi muốn **xem danh sách hóa đơn của mình, gồm cả hóa đơn đang chờ thanh toán**, để **biết mình đã thanh toán gì và cần ra quầy trả khoản nào**.
- **BR liên quan:** BR-F7.1, BR-F7.3, BR-F7.4; S2, S3, S7
- **Version / Sprint:** v2 / Sprint 2
- **Issue:** #152
- **Tiêu chí chấp nhận:**
   - **AC-F7.3.1-01: Xem hóa đơn của mình, mới nhất trước** (BR-F7.3, S7)
      - **Given** Hội viên An đã đăng nhập; An có hóa đơn `PAID` lập tại quầy, hóa đơn `PENDING` do cổng hội viên tạo và một hóa đơn `CANCELLED`
      - **When** Gọi `GET /api/v1/me/invoices`
      - **Then** `200 OK` dạng `PageResponse` mới nhất trước; mỗi hóa đơn có mã, tổng tiền (`total`, do server tính), trạng thái, ngày tạo và ngày thanh toán (nếu có)
   - **AC-F7.3.1-02: Hóa đơn chờ do cổng hội viên kèm hạn tự hủy** (BR-F7.4)
      - **Given** Hóa đơn `source = MEMBER_PORTAL` đang `PENDING`, tạo ngày 05/11/2026; `PENDING_INVOICE_TTL_DAYS = 3`; và một hóa đơn `source = COUNTER` đang `PENDING`
      - **When** Hội viên xem danh sách hóa đơn
      - **Then** Hóa đơn `MEMBER_PORTAL` kèm hạn tự hủy 08/11/2026 (`created_at` cộng `PENDING_INVOICE_TTL_DAYS` ngày, server tính); hóa đơn `COUNTER` không có hạn tự hủy
   - **AC-F7.3.1-03: Chỉ thấy hóa đơn của chính mình** (BR-F7.1, S3)
      - **Given** Hóa đơn của hội viên Bình, và hóa đơn khách lẻ không gắn hội viên; An đã đăng nhập
      - **When** An gọi `GET /api/v1/me/invoices`, kể cả khi tự gửi thêm `memberId`
      - **Then** `200 OK`; danh sách chỉ gồm hóa đơn có hội viên là An; `memberId` bị bỏ qua
   - **AC-F7.3.1-04: Hội viên chưa có hóa đơn nào**
      - **Given** Hội viên mới, chưa có hóa đơn
      - **When** Gọi `GET /api/v1/me/invoices`
      - **Then** `200 OK` với danh sách rỗng; giao diện hiển thị **"Bạn chưa có hóa đơn"**
   - **AC-F7.3.1-05: Tham số phân trang** (S5)
      - **Given** Hội viên An đã đăng nhập
      - **When** Gửi `page < 0` hoặc `size` ngoài khoảng 1–100
      - **Then** `400 Bad Request` kèm lỗi theo từng tham số; không kèm tham số thì mặc định `page=0`, `size=20`
   - **AC-F7.3.1-06: Từ chối người không được xem** (S2, S3)
      - **Given** Người gọi không phải Hội viên (Lễ tân, Huấn luyện viên, Quản lý, Chủ chuỗi), hoặc tài khoản hội viên chưa liên kết hồ sơ hội viên; hoặc không gửi token
      - **When** Gọi `GET /api/v1/me/invoices`
      - **Then** Không có token hoặc token hết hạn trả `401` **"Bạn cần đăng nhập để truy cập tài nguyên này."**; các trường hợp còn lại trả `403` **"Bạn không có quyền thực hiện thao tác này."**
**---**

### 3.4. UC-F7.4: Gửi yêu cầu mua gói
- **Mã UC:** UC-F7.4
- **Tên Use Case:** Gửi yêu cầu mua gói
- **Tác nhân chính:** Hội viên
- **Tác nhân phụ (nếu có):** Hệ thống (tác vụ tự hủy hóa đơn chờ); lễ tân hoặc quản lý (ghi nhận thanh toán, ngoài phạm vi use case này)
- **Mô tả ngắn gọn:** Hội viên chọn một gói để mua; hệ thống tạo hóa đơn chờ và gói ở trạng thái PENDING.
#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Hội viên và có hồ sơ `members` liên kết với tài khoản.
- Gói được chọn đang được bán (`packages.status` = ACTIVE).
#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Một hóa đơn (`source` = MEMBER_PORTAL, `status` = PENDING), một dòng hóa đơn loại PACKAGE và một gói (`memberships`) ở trạng thái PENDING được tạo cho hội viên. Hóa đơn tự hủy nếu quá hạn `PENDING_INVOICE_TTL_DAYS`.
- **Thất bại:** Không tạo hóa đơn hay gói nào (rollback giao dịch).
#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Hội viên chọn một gói trong danh sách và bấm "Mua".

2. Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào (mã gói bắt buộc).

3. Hệ thống kiểm tra quyền và vai trò (S3); hội viên được xác định từ token.

4. Hệ thống kiểm tra gói tồn tại và đang được bán; kiểm tra hội viên chưa có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt.

5. Hệ thống tính số tiền ở phía server (S7), tạo trong một giao dịch (`@Transactional`): hóa đơn PENDING (`created_by` là tài khoản hội viên), dòng hóa đơn PACKAGE, và gói PENDING gắn với dòng hóa đơn qua `memberships.invoice_item_id`.

6. Hệ thống trả về kết quả (201 Created) kèm mã hóa đơn, tổng tiền và hạn tự hủy.

7. Hệ thống hiển thị hướng dẫn thanh toán tại quầy.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Hóa đơn chờ tự hủy:**
   - Khi hóa đơn `MEMBER_PORTAL` còn PENDING quá `PENDING_INVOICE_TTL_DAYS` ngày, tác vụ nền chuyển hóa đơn sang CANCELLED và chuyển luôn gói PENDING đi kèm sang CANCELLED.
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):**
   - Tại bước 2, thiếu hoặc sai mã gói; hệ thống trả `ProblemDetail` (400) kèm danh sách lỗi.
- **E2 – Không có quyền truy cập tài nguyên (403 Forbidden):**
   - Tại bước 3, người thao tác không phải Hội viên.
- **E3 – Không tìm thấy bản ghi dữ liệu (404 Not Found):**
   - Tại bước 4, mã gói không tồn tại; hệ thống trả `ProblemDetail` (404): **"Gói tập không tồn tại trên hệ thống."**
- **E4 – Đã có gói cùng loại đang chờ (422 Unprocessable Content):**
   - Tại bước 4, hội viên đã có gói cùng loại đang `PENDING` hoặc `UPCOMING`; hệ thống không tạo thêm và trả `ProblemDetail` (422): **"Hội viên đã có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt. Không thể tạo thêm gói mới."** (cùng câu với F3, AC-F3.5.1-06)
- **E5 – Vi phạm quy tắc nghiệp vụ (422 Unprocessable Content):**
   - Tại bước 4, gói đã ngừng bán (`INACTIVE`); hệ thống trả `ProblemDetail` (422): **"Gói tập này đã ngừng bán."** (cùng câu với F3, chốt 22/11/2026)
- **E6 – Chưa đăng nhập hoặc token hết hạn (401 Unauthorized):**
   - Tại bước 3, JWT không có hoặc hết hạn; hệ thống trả `ProblemDetail` (401): **"Bạn cần đăng nhập để truy cập tài nguyên này."**
#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F7.5:** Hóa đơn chờ tạo từ cổng hội viên tự hủy sau `PENDING_INVOICE_TTL_DAYS`; gói PENDING đi kèm cũng bị hủy (CANCELLED). Chỉ hóa đơn `MEMBER_PORTAL` bị tự hủy.
- **BR-F7.6:** Ngày bắt đầu của gói PENDING chỉ là ngày dự kiến; khi hóa đơn chuyển PAID, hệ thống tính lại: ngày sau ngày gói cũ cùng loại hết hiệu lực.
- **BR-F7.7:** Chỉ lễ tân hoặc quản lý ghi nhận thanh toán (S7); hội viên không tự chuyển hóa đơn sang PAID.
- **CONFIG-F7.1:** Đọc từ bảng `system_configs` với key `PENDING_INVOICE_TTL_DAYS` (giá trị mặc định: `3`), không viết cứng trong mã nguồn (Quy tắc số 15).
- **[Yêu cầu phi chức năng]:**
   - Toàn vẹn đồng thời (NF6): hai yêu cầu cùng lúc cho cùng một gói không được tạo hai hóa đơn chờ.
   - Bảo mật (S2, S3, S7): kiểm tra JWT, phân quyền ở backend, tính tiền ở server.
#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `invoices`, `invoice_items`, `memberships`; đọc `packages`, `members`, `system_configs`
- **Chi tiết trường dữ liệu:**
   | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
   | --- | --- | --- | --- | --- |
   | `invoices` | `code` | `varchar(30)` | UNIQUE, NOT NULL | Mã hóa đơn |
   | `invoices` | `member_id` | `bigint` | FK → `members.id` | Bắt buộc có khi hóa đơn chứa dòng PACKAGE |
   | `invoices` | `created_by` | `bigint` | NOT NULL, FK → `users.id` | Tài khoản hội viên tự gửi yêu cầu |
   | `invoices` | `source` | `varchar` | NOT NULL | Giá trị `MEMBER_PORTAL` |
   | `invoices` | `subtotal`, `discount`, `total` | `int` | NOT NULL, CHECK `total = subtotal - discount` | Tiền do server tính (VND) |
   | `invoices` | `status` | `varchar` | NOT NULL, default `PENDING` | Hóa đơn chờ; tự hủy thì `CANCELLED` |
   | `invoice_items` | `invoice_id` | `bigint` | NOT NULL, FK → `invoices.id` | Hóa đơn chứa dòng |
   | `invoice_items` | `item_type` | `varchar` | NOT NULL, CHECK (PACKAGE, PRODUCT, TRANSFER_FEE) | Giá trị `PACKAGE` |
   | `invoice_items` | `package_id` | `bigint` | FK → `packages.id` | Bắt buộc khi `item_type` = PACKAGE |
   | `invoice_items` | `quantity`, `unit_price`, `amount` | `int` | NOT NULL, CHECK `amount = quantity * unit_price` | Số lượng, giá lúc bán, thành tiền |
   | `memberships` | `invoice_item_id` | `bigint` | FK → `invoice_items.id` | Dòng hóa đơn tạo ra gói; cơ sở tìm gói cần kích hoạt hoặc hủy |
   | `memberships` | `status` | `varchar` | NOT NULL, default `PENDING` | Gói chờ thanh toán; hết hạn chờ thì `CANCELLED` |
   | `memberships` | `start_date`, `end_date` | `date` | NOT NULL | Ngày dự kiến, tính lại khi hóa đơn PAID |
#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `POST /api/v1/me/purchase-requests` **(đề xuất)**
- **Header:** `Authorization: Bearer \<access_token>`
- **Request Body (DTO):**
   ```json

   {

      "packageId": 1

   }

   ```

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F7.4.1: Gửi yêu cầu mua gói
- **User story:** Là **Hội viên**, tôi muốn **chọn một gói trên web để gửi yêu cầu mua**, để **ra quầy thanh toán là dùng được ngay**.
- **BR liên quan:** BR-F7.5, BR-F7.6, BR-F7.7, CONFIG-F7.1; S2, S3, S5, S7, NF6
- **Version / Sprint:** v2 / Sprint 2
- **Issue:** #153
- **Tiêu chí chấp nhận:**
   - **AC-F7.4.1-01: Gửi yêu cầu mua tạo hóa đơn chờ và gói chờ, giá lấy ở server** (BR-F7.5, S7)
      - **Given** Hội viên An đã đăng nhập; gói `id=1` đang bán, loại TIME 30 ngày, giá 500.000 đ; An chưa có gói TIME nào
      - **When** Gọi `POST /api/v1/me/purchase-requests` với `{"packageId": 1}`
      - **Then** `201 Created` trả `invoiceId`, `membershipId`, `total` và hạn tự hủy; hóa đơn `PENDING`, `source = MEMBER_PORTAL`, `created_by` là tài khoản của An, `subtotal = total = 500000`, `discount = 0`; dòng hóa đơn loại `PACKAGE` có `unit_price = 500000` chép từ `packages.price`; gói `PENDING` gắn với dòng hóa đơn qua `invoice_item_id`. Giá hoặc tổng tiền do client gửi kèm (nếu có) không được dùng
   - **AC-F7.4.1-02: Gói chỉ hiệu lực sau khi lễ tân thu tiền** (BR-F7.6, BR-F7.7)
      - **Given** Hóa đơn `MEMBER_PORTAL` đang `PENDING` và gói `PENDING` cho gói TIME 30 ngày
      - **When** Hội viên chưa ra quầy; sau đó lễ tân ghi nhận thu tiền (F5) ngày 01/12/2026
      - **Then** Trước khi thu tiền, hóa đơn và gói vẫn `PENDING`; hội viên không có cách tự chuyển hóa đơn sang `PAID` (S7). Khi hóa đơn chuyển `PAID`, gói chuyển `ACTIVE`, `start_date = 2026-12-01`, `end_date = 2026-12-30` (tính lại tại lúc thanh toán, giống AC-F3.5.1-02)
   - **AC-F7.4.1-03: Hóa đơn chờ tự hủy sau `PENDING_INVOICE_TTL_DAYS`, kéo theo gói chờ** (BR-F7.5, CONFIG-F7.1)
      - **Given** Hóa đơn `MEMBER_PORTAL` đang `PENDING` tạo ngày 05/11/2026 và gói `PENDING` đi kèm; `PENDING_INVOICE_TTL_DAYS = 3`; ngoài ra có hóa đơn `COUNTER` cũng `PENDING` quá hạn
      - **When** Tác vụ nền hủy hóa đơn chờ quá hạn chạy sau ngày 08/11/2026, rồi chạy lại một lần nữa
      - **Then** Hóa đơn `MEMBER_PORTAL` chuyển `CANCELLED` và gói `PENDING` đi kèm chuyển `CANCELLED`; hóa đơn `COUNTER` giữ nguyên (AC-F3.5.1-09); lần chạy thứ hai không đổi thêm dữ liệu nào
   - **AC-F7.4.1-04: Số ngày tự hủy đọc từ cấu hình** (CONFIG-F7.1)
      - **Given** Quản lý đổi `PENDING_INVOICE_TTL_DAYS` thành 5 trong `system_configs`
      - **When** Hội viên gửi yêu cầu mua gói ngày 05/11/2026
      - **Then** Hạn tự hủy trả về là 10/11/2026; không có số 3 viết cứng trong mã nguồn
   - **AC-F7.4.1-05: Đã có gói cùng loại đang chờ** (BR-F7.5)
      - **Given** Hội viên An đã có một gói cùng loại đang `PENDING` hoặc `UPCOMING`
      - **When** An gửi yêu cầu mua thêm gói cùng loại
      - **Then** `422 Unprocessable Content` **"Hội viên đã có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt. Không thể tạo thêm gói mới."**; không có hóa đơn hay gói nào được tạo (cùng quy tắc với AC-F3.5.1-06)
   - **AC-F7.4.1-06: Hai yêu cầu gửi cùng lúc** (NF6)
      - **Given** An chưa có gói TIME; An mở web trên hai thiết bị và gửi hai yêu cầu mua gói TIME gần như đồng thời
      - **When** Cả hai yêu cầu tới server
      - **Then** Đúng một yêu cầu `201 Created`; yêu cầu còn lại `422` với thông báo như AC-F7.4.1-05; chỉ có một hóa đơn `PENDING` và một gói `PENDING`
   - **AC-F7.4.1-07: Gói không hợp lệ để mua**
      - **Given** Gói đã ngừng bán (`INACTIVE`); hoặc `packageId` không tồn tại
      - **When** Hội viên gửi yêu cầu mua
      - **Then** Lần lượt trả `422` **"Gói tập này đã ngừng bán."** và `404` **"Gói tập không tồn tại trên hệ thống."**; không có hóa đơn hay gói nào được tạo
   - **AC-F7.4.1-08: Dữ liệu sai và người không được gửi** (S2, S3, S5)
      - **Given** Request thiếu `packageId`, `packageId` không phải số nguyên dương, hoặc JSON sai cú pháp; hoặc người gọi không phải Hội viên có hồ sơ; hoặc không gửi token
      - **When** Gọi `POST /api/v1/me/purchase-requests`
      - **Then** Lần lượt trả `400` kèm lỗi theo trường, `403` **"Bạn không có quyền thực hiện thao tác này."**, `401`; không có hóa đơn hay gói nào được tạo
**---**

### 3.5. UC-F7.5: Gửi yêu cầu gia hạn gói
- **Mã UC:** UC-F7.5
- **Tên Use Case:** Gửi yêu cầu gia hạn gói
- **Tác nhân chính:** Hội viên
- **Tác nhân phụ (nếu có):** Hệ thống (tác vụ tự hủy hóa đơn chờ); lễ tân hoặc quản lý (ghi nhận thanh toán)
- **Mô tả ngắn gọn:** Hội viên gia hạn một gói đang có; hệ thống tạo hóa đơn chờ và một gói mới ở trạng thái PENDING, nối tiếp sau gói hiện tại khi được thanh toán.
#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Hội viên và có hồ sơ `members` liên kết với tài khoản.
- Hội viên có gói cần gia hạn, và gói đó (`packages`) vẫn đang được bán.
#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Một hóa đơn chờ (`MEMBER_PORTAL`, PENDING), một dòng hóa đơn PACKAGE và một gói mới ở trạng thái PENDING được tạo. Gói hiện tại không bị thay đổi.
- **Thất bại:** Không tạo hóa đơn hay gói nào (rollback giao dịch).
#### C. Luồng sự kiện chính (Basic Flow / Happy Path)

1. Hội viên mở "Gói của tôi", chọn gói cần gia hạn và bấm "Gia hạn".

2. Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào (mã gói của hội viên bắt buộc).

3. Hệ thống kiểm tra quyền và vai trò (S3); hội viên được xác định từ token.

4. Hệ thống kiểm tra gói thuộc về hội viên đó, gói chưa ở trạng thái CANCELLED, loại gói còn được bán, và hội viên chưa có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt.

5. Hệ thống tính số tiền ở phía server (S7), tạo trong một giao dịch (`@Transactional`) hóa đơn PENDING, dòng hóa đơn PACKAGE và gói mới PENDING (cùng `package_id`), gắn qua `memberships.invoice_item_id`.

6. Hệ thống trả về kết quả (201 Created) kèm mã hóa đơn, tổng tiền và hạn tự hủy.

7. Hệ thống hiển thị hướng dẫn thanh toán tại quầy.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Hóa đơn chờ tự hủy:**
   - Khi quá hạn `PENDING_INVOICE_TTL_DAYS` mà chưa thanh toán, tác vụ nền chuyển hóa đơn sang CANCELLED và gói PENDING đi kèm sang CANCELLED.
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):**
   - Tại bước 2, thiếu hoặc sai mã gói; hệ thống trả `ProblemDetail` (400).
- **E2 – Không có quyền truy cập tài nguyên (403 Forbidden):**
   - Tại bước 3, người thao tác không phải Hội viên; hệ thống trả `ProblemDetail` (403).
- **E3 – Không tìm thấy bản ghi dữ liệu (404 Not Found):**
   - Tại bước 4, gói không tồn tại **hoặc thuộc hội viên khác**; hệ thống trả cùng một `ProblemDetail` (404), để không lộ gói của người khác có tồn tại hay không (chốt 22/11/2026, giống các API khác của hội viên).
- **E4 – Đã có gói cùng loại đang chờ (422 Unprocessable Content):**
   - Tại bước 4, hội viên đã có gói cùng loại đang `UPCOMING`, hoặc đang có hóa đơn gia hạn `PENDING` cho loại gói đó; hệ thống không tạo thêm và trả `ProblemDetail` (422): **"Hội viên đã có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt. Không thể tạo thêm gói mới."** (cùng câu với F3, AC-F3.5.1-06)
- **E5 – Vi phạm quy tắc nghiệp vụ (422 Unprocessable Content):**
   - Tại bước 4, loại gói đã ngừng bán; hệ thống trả `ProblemDetail` (422): **"Gói tập này đã ngừng bán."** (cùng câu với F3, chốt 22/11/2026)
   - Tại bước 4, gói ở trạng thái CANCELLED (hóa đơn của gói đã bị hủy hoặc đã hoàn tiền); hệ thống trả `ProblemDetail` (422): **"Gói này đã hủy. Vui lòng chọn gói ở mục Mua gói."** (BR-F7.9).
- **E6 – Chưa đăng nhập hoặc token hết hạn (401 Unauthorized):**
   - Tại bước 3, JWT không có hoặc hết hạn; hệ thống trả `ProblemDetail` (401): **"Bạn cần đăng nhập để truy cập tài nguyên này."**
#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F7.5:** Hóa đơn chờ tạo từ cổng hội viên tự hủy sau `PENDING_INVOICE_TTL_DAYS`; gói PENDING đi kèm cũng bị hủy.
- **BR-F7.8:** Mỗi hội viên chỉ có một gói ACTIVE hoặc FROZEN cho mỗi loại gói và chi nhánh (chỉ mục duy nhất `uq_one_active_per_type`), nên gia hạn không sửa gói cũ mà tạo gói mới. Khi hóa đơn PAID, gói mới nối tiếp sau ngày gói cũ cùng loại hết hiệu lực (trạng thái UPCOMING nếu chưa đến ngày bắt đầu).
- **BR-F7.9:** Gói CANCELLED không gia hạn được, vì hội viên chưa từng dùng gói đó; muốn mua lại thì dùng "Mua gói" (UC-F7.4). Ví dụ: hóa đơn gói PT bị hủy tại quầy thì gói PT đó không có nút "Gia hạn" trên cổng hội viên. Gói hết hạn (EXPIRED) vẫn gia hạn được (chốt 22/11/2026).
- **CONFIG-F7.1:** Đọc từ bảng `system_configs` với key `PENDING_INVOICE_TTL_DAYS` (giá trị mặc định: `3`).
- **[Yêu cầu phi chức năng]:**
   - Toàn vẹn đồng thời (NF6): không tạo hai hóa đơn chờ cho cùng một gói khi có hai yêu cầu đồng thời; bảng `invoices` và `memberships` dùng khóa lạc quan (`version`).
   - Bảo mật (S2, S3, S7): kiểm tra JWT, phân quyền ở backend, tính tiền ở server.
#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `invoices`, `invoice_items`, `memberships`; đọc `packages`, `members`, `system_configs`
- **Chi tiết trường dữ liệu:** như UC-F7.4 (mục F). Thêm:
   | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
   | --- | --- | --- | --- | --- |
   | `memberships` | `version` | `int` | NOT NULL, default `0` | Khóa lạc quan (`@Version`) |
   | `memberships` | `package_type`, `branch_id` | `varchar`, `bigint` | `branch_id` rỗng = toàn chuỗi | Cùng với `member_id` tạo ràng buộc một gói ACTIVE hoặc FROZEN mỗi loại |
#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `POST /api/v1/me/renewal-requests` **(đề xuất)**
- **Header:** `Authorization: Bearer \<access_token>`
- **Request Body (DTO):**
   ```json

   {

      "membershipId": 1

   }

   ```

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F7.5.1: Gửi yêu cầu gia hạn
- **User story:** Là **Hội viên**, tôi muốn **gửi yêu cầu gia hạn cho gói đang dùng hoặc đã hết hạn**, để **gói mới nối tiếp mà không phải ra quầy lập lại từ đầu**.
- **BR liên quan:** BR-F7.5, BR-F7.8, BR-F7.9, CONFIG-F7.1; S2, S3, S5, S7, NF6
- **Version / Sprint:** v2 / Sprint 2
- **Issue:** #154
- **Tiêu chí chấp nhận:**
   - **AC-F7.5.1-01: Gia hạn gói còn hiệu lực: tạo gói mới chờ, gói cũ không đổi** (BR-F7.8)
      - **Given** Hội viên An có gói TIME `ACTIVE` (`membershipId = 1`) hết hạn 31/12/2026; loại gói đó còn đang bán
      - **When** Gọi `POST /api/v1/me/renewal-requests` với `{"membershipId": 1}`
      - **Then** `201 Created`; hóa đơn `MEMBER_PORTAL` `PENDING`, dòng `PACKAGE` và một gói mới `PENDING` cùng `package_id`; gói cũ vẫn `ACTIVE`, không đổi gì. Khi lễ tân thu tiền, gói mới chuyển `UPCOMING` và bắt đầu từ 01/01/2027 (giống AC-F3.5.1-03)
   - **AC-F7.5.1-02: Gói đã hết hạn vẫn gia hạn được** (BR-F7.9)
      - **Given** An có gói TIME `EXPIRED` và không có gói TIME nào khác
      - **When** An gửi yêu cầu gia hạn gói đó
      - **Then** `201 Created`; hóa đơn và gói `PENDING` được tạo; khi lễ tân thu tiền, gói mới bắt đầu từ ngày thanh toán
   - **AC-F7.5.1-03: Gói đã hủy không gia hạn được** (BR-F7.9)
      - **Given** An có một gói `CANCELLED`
      - **When** An gửi yêu cầu gia hạn gói đó
      - **Then** `422 Unprocessable Content` **"Gói này đã hủy. Vui lòng chọn gói ở mục Mua gói."**; không có hóa đơn hay gói nào được tạo; cổng hội viên ẩn nút "Gia hạn" ở gói "Đã hủy"
   - **AC-F7.5.1-04: Gói của hội viên khác hoặc không tồn tại: cùng một câu trả lời** (S3)
      - **Given** `membershipId` thuộc về hội viên Bình; hoặc `membershipId` không tồn tại
      - **When** An gửi yêu cầu gia hạn với `membershipId` đó
      - **Then** Cả hai trường hợp đều trả `404 Not Found` **"Gói tập không tồn tại trên hệ thống."** (cùng câu, để không dò được mã gói của người khác); không có hóa đơn hay gói nào được tạo
   - **AC-F7.5.1-05: Loại gói đã ngừng bán** (BR-F7.8)
      - **Given** Gói `packages` của gói hội viên đã `INACTIVE`
      - **When** An gửi yêu cầu gia hạn
      - **Then** `422 Unprocessable Content` **"Gói tập này đã ngừng bán."** (cùng hàm kiểm tra và cùng câu với F3); không có hóa đơn hay gói nào được tạo
   - **AC-F7.5.1-06: Đã có gói cùng loại đang chờ** (BR-F7.8)
      - **Given** An đã có gói cùng loại đang `UPCOMING`, hoặc đang có hóa đơn gia hạn `PENDING` cho loại gói đó
      - **When** An gửi yêu cầu gia hạn
      - **Then** `422 Unprocessable Content` **"Hội viên đã có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt. Không thể tạo thêm gói mới."**; không có hóa đơn hay gói nào được tạo
   - **AC-F7.5.1-07: Hai yêu cầu gia hạn gửi cùng lúc** (NF6)
      - **Given** An chưa có gói chờ; hai yêu cầu gia hạn cho cùng một gói được gửi gần như đồng thời
      - **When** Cả hai yêu cầu tới server
      - **Then** Đúng một yêu cầu `201 Created`; yêu cầu còn lại `422` với thông báo như AC trước; chỉ có một hóa đơn `PENDING` và một gói `PENDING` mới
   - **AC-F7.5.1-08: Hóa đơn gia hạn tự hủy kéo theo gói chờ** (BR-F7.5, CONFIG-F7.1)
      - **Given** Hóa đơn gia hạn `MEMBER_PORTAL` quá `PENDING_INVOICE_TTL_DAYS` ngày mà chưa thanh toán
      - **When** Tác vụ nền hủy hóa đơn chờ quá hạn chạy
      - **Then** Hóa đơn `CANCELLED` và gói mới `PENDING` đi kèm chuyển `CANCELLED`; gói cũ của An vẫn `ACTIVE` hoặc `EXPIRED` như trước, không đổi
   - **AC-F7.5.1-09: Dữ liệu sai và người không được gửi** (S2, S3, S5)
      - **Given** Request thiếu `membershipId`, sai kiểu, hoặc JSON sai cú pháp; hoặc người gọi không phải Hội viên có hồ sơ; hoặc không gửi token
      - **When** Gọi `POST /api/v1/me/renewal-requests`
      - **Then** Lần lượt trả `400` kèm lỗi theo trường, `403` **"Bạn không có quyền thực hiện thao tác này."**, `401`; không có hóa đơn hay gói nào được tạo
