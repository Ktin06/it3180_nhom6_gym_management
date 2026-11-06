# Đặc tả Use Case – F7: Cổng hội viên

- **Mã chức năng:** F7
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

Cổng hội viên là khu vực riêng để hội viên tự theo dõi tình trạng của mình mà không cần đến quầy lễ tân. Chức năng gồm hai nhóm năng lực:

- **Xem thông tin của chính mình:** gói tập, lịch sử check-in, hóa đơn.
- **Gửi yêu cầu mua hoặc gia hạn gói:** hệ thống tạo hóa đơn chờ (nguồn `MEMBER_PORTAL`, trạng thái PENDING) và gói ở trạng thái PENDING; hội viên thanh toán tại quầy, lễ tân hoặc quản lý ghi nhận thanh toán (S7).

Vai trò được phép dùng: **Hội viên** (đã đăng nhập, có hồ sơ `members` liên kết với tài khoản qua `members.user_id`). Hội viên chỉ xem được dữ liệu của chính mình. Ở Sprint 1, tài liệu này chỉ đặc tả; phần hiện thực làm ở Sprint 2.

---

## 2. Danh sách Use Case

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F7.1** | Xem gói tập | Hội viên | Trung bình | Sprint 2 |
| **UC-F7.2** | Xem lịch sử check-in | Hội viên | Trung bình | Sprint 2 |
| **UC-F7.3** | Xem hóa đơn | Hội viên | Trung bình | Sprint 2 |
| **UC-F7.4** | Gửi yêu cầu mua gói | Hội viên | Cao | Sprint 2 |
| **UC-F7.5** | Gửi yêu cầu gia hạn gói | Hội viên | Cao | Sprint 2 |

---

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
- **E2 – Không có quyền truy cập tài nguyên (403 Forbidden):**
  - Tại bước 2, người thao tác không phải Hội viên hoặc tài khoản chưa liên kết với hồ sơ hội viên.
  - Hệ thống trả `ProblemDetail` (403): *"Bạn không có quyền thực hiện thao tác này."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F7.1:** Hội viên chỉ xem được dữ liệu của chính mình (S3).
- **BR-F7.2:** Hiển thị nhãn theo trạng thái gói: PENDING = "Chờ thanh toán", UPCOMING = "Chưa đến ngày bắt đầu", FROZEN = "Đang bảo lưu".
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
- **API Endpoint:** `GET /api/v1/me/memberships` *(đề xuất, thống nhất với người làm API)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body:** Không có.

---

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
  - Hệ thống trả `ProblemDetail` (403): *"Bạn không có quyền thực hiện thao tác này."*

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
- **API Endpoint:** `GET /api/v1/me/checkins?page=0&size=20` *(đề xuất)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body:** Không có.

---

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
- **API Endpoint:** `GET /api/v1/me/invoices?page=0&size=20` *(đề xuất)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body:** Không có.

---

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
4. Hệ thống kiểm tra gói tồn tại và đang được bán; kiểm tra hội viên chưa có hóa đơn chờ cho gói này.
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
  - Tại bước 4, mã gói không tồn tại; hệ thống trả `ProblemDetail` (404): *"Gói tập không tồn tại trên hệ thống."*
- **E4 – Xung đột dữ liệu / Trùng lặp (409 Conflict):**
  - Tại bước 4, hội viên đã có hóa đơn chờ cho gói này; hệ thống không tạo thêm và trả `ProblemDetail` (409).
- **E5 – Vi phạm quy tắc nghiệp vụ (422 Unprocessable Content):**
  - Tại bước 4, gói đã ngừng bán (`INACTIVE`); hệ thống trả `ProblemDetail` (422): *"Gói này hiện không còn được bán."*

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
- **API Endpoint:** `POST /api/v1/me/purchase-requests` *(đề xuất)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body (DTO):**
  ```json
  {
    "packageId": 1
  }
  ```

---

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
4. Hệ thống kiểm tra gói thuộc về hội viên đó, loại gói còn được bán, và chưa có hóa đơn chờ cho gói này.
5. Hệ thống tính số tiền ở phía server (S7), tạo trong một giao dịch (`@Transactional`) hóa đơn PENDING, dòng hóa đơn PACKAGE và gói mới PENDING (cùng `package_id`), gắn qua `memberships.invoice_item_id`.
6. Hệ thống trả về kết quả (201 Created) kèm mã hóa đơn, tổng tiền và hạn tự hủy.
7. Hệ thống hiển thị hướng dẫn thanh toán tại quầy.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Hóa đơn chờ tự hủy:**
  - Khi quá hạn `PENDING_INVOICE_TTL_DAYS` mà chưa thanh toán, tác vụ nền chuyển hóa đơn sang CANCELLED và gói PENDING đi kèm sang CANCELLED.
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):**
  - Tại bước 2, thiếu hoặc sai mã gói; hệ thống trả `ProblemDetail` (400).
- **E2 – Không có quyền truy cập tài nguyên (403 Forbidden):**
  - Tại bước 3, người thao tác không phải Hội viên, hoặc tại bước 4 gói không thuộc hội viên này.
- **E3 – Không tìm thấy bản ghi dữ liệu (404 Not Found):**
  - Tại bước 4, gói của hội viên không tồn tại; hệ thống trả `ProblemDetail` (404).
- **E4 – Xung đột dữ liệu / Trùng lặp (409 Conflict):**
  - Tại bước 4, đã có hóa đơn chờ cho gói này; hệ thống không tạo thêm và trả `ProblemDetail` (409).
- **E5 – Vi phạm quy tắc nghiệp vụ (422 Unprocessable Content):**
  - Tại bước 4, loại gói đã ngừng bán; hệ thống trả `ProblemDetail` (422): *"Gói này hiện không còn được bán."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F7.5:** Hóa đơn chờ tạo từ cổng hội viên tự hủy sau `PENDING_INVOICE_TTL_DAYS`; gói PENDING đi kèm cũng bị hủy.
- **BR-F7.8:** Mỗi hội viên chỉ có một gói ACTIVE hoặc FROZEN cho mỗi loại gói và chi nhánh (chỉ mục duy nhất `uq_one_active_per_type`), nên gia hạn không sửa gói cũ mà tạo gói mới. Khi hóa đơn PAID, gói mới nối tiếp sau ngày gói cũ cùng loại hết hiệu lực (trạng thái UPCOMING nếu chưa đến ngày bắt đầu).
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
- **API Endpoint:** `POST /api/v1/me/renewal-requests` *(đề xuất)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body (DTO):**
  ```json
  {
    "membershipId": 1
  }
  ```
