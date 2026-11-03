# Đặc tả Use Case – F5: Thanh toán tại quầy

- **Mã chức năng:** F5
- **Version:** v1 (Bắt buộc)
- **Người biên soạn:** Thái Hùng Lân
- **Người duyệt chéo:** Bùi Trần Đức Việt (theo phân công cặp duyệt mục 2.4 Sprint 1)
- **Tài liệu chuẩn tham chiếu:**
  - [Bản chốt phạm vi](../ban-chot-pham-vi.md) (mục Thanh toán; quy tắc S3, S7, S9, NF1, NF6)
  - [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml) (Nhóm 6: Bán hàng và thanh toán – `invoices`, `invoice_items`, `payments`)
  - [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md)
  - [ADR 0004: Thanh toán chỉ tại quầy](../../decisions/0004-thanh-toan-chi-tai-quay.md)
  - [Quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md)

---

## 1. Tổng quan chức năng

Chức năng **F5 – Thanh toán tại quầy** là trung tâm dòng tiền và vận hành tài chính trực tiếp tại cơ sở của chuỗi phòng tập gym:
- **Phương thức thanh toán:** Hệ thống chỉ hỗ trợ thanh toán trực tiếp tại quầy bằng **Tiền mặt (`CASH`)** hoặc **Chuyển khoản ngân hàng (`BANK_TRANSFER`) qua mã VietQR**.
- **Không thanh toán online qua cổng:** Dự án không tích hợp cổng thanh toán trực tuyến (VNPay, Momo...) để tránh phức tạp về tài khoản doanh nghiệp, callback và đối soát tự động ([ADR 0004](../../decisions/0004-thanh-toan-chi-tai-quay.md)).
- **Cơ chế VietQR:** Đối với hình thức chuyển khoản, hệ thống tự động sinh ảnh/chuỗi VietQR động chuẩn Napas 247 chứa đúng số tài khoản chi nhánh, số tiền cần thanh toán (`invoices.total`) và nội dung chuyển khoản chuẩn theo mã hóa đơn (ví dụ: `CN01 HD000123`). Lễ tân kiểm tra biến động số dư và bấm xác nhận đã nhận tiền (S7).
- **Kích hoạt dịch vụ:** Gói tập (`memberships`) hoặc dịch vụ chỉ được kích hoạt sang `ACTIVE` (hoặc `UPCOMING` nếu là gia hạn) sau khi hóa đơn được chuyển thành công sang trạng thái đã thanh toán (`PAID`).
- **Phân quyền chặt chẽ:** Chỉ Lễ tân và Quản lý chi nhánh được xác nhận thu tiền (S7). Riêng nghiệp vụ **Hoàn tiền (`REFUND`)**, chỉ Quản lý chi nhánh mới có thẩm quyền và mọi thao tác hoàn tiền bắt buộc phải lưu vết vào nhật ký kiểm toán (`audit_logs` – S9).
- **Phạm vi Sprint 1:** Lập hóa đơn và xác nhận thanh toán tại quầy (`CASH`, `VietQR`), tra cứu hóa đơn chi nhánh, hủy hóa đơn chờ tại quầy, xuất/in hóa đơn PDF.
- **Phạm vi Sprint 2:** Xử lý tiếp nhận hóa đơn chờ từ Cổng hội viên (F7), tác vụ nền tự động hủy hóa đơn chờ `PENDING` quá hạn 3 ngày (`PENDING_INVOICE_TTL_DAYS`).

---

## 2. Danh sách Use Case

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F5.1** | Sinh mã VietQR và xem chi tiết thanh toán hóa đơn | Lễ tân, Quản lý chi nhánh | Cao | Sprint 1 |
| **UC-F5.2** | Ghi nhận thanh toán tại quầy và kích hoạt dịch vụ | Lễ tân, Quản lý chi nhánh | Cao | Sprint 1 |
| **UC-F5.3** | Tra cứu và quản lý hóa đơn tại quầy | Lễ tân, Quản lý chi nhánh | Cao | Sprint 1 |
| **UC-F5.4** | In và xuất hóa đơn PDF | Lễ tân, Quản lý chi nhánh, Hội viên | Trung bình | Sprint 1 |
| **UC-F5.5** | Hoàn tiền hóa đơn (Refund) | Quản lý chi nhánh | Trung bình | Sprint 1 / Sprint 2 |

---

## 3. Đặc tả chi tiết từng Use Case

---

### 3.1. UC-F5.1: Sinh mã VietQR và xem chi tiết thanh toán hóa đơn

- **Mã UC:** UC-F5.1
- **Tên Use Case:** Sinh mã VietQR và xem chi tiết thanh toán hóa đơn
- **Tác nhân chính:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`)
- **Tác nhân phụ:** Dịch vụ VietQR Quick Link
- **Mô tả ngắn gọn:** Khi khách chọn hình thức chuyển khoản, hệ thống hiển thị thông tin hóa đơn và sinh mã QR động VietQR có sẵn số tiền và nội dung chuyển khoản để khách quét trên ứng dụng ngân hàng.

#### A. Tiền điều kiện (Preconditions)
- Lễ tân/Quản lý đã đăng nhập với JWT hợp lệ (S2).
- Hóa đơn cần thanh toán tồn tại trong hệ thống, thuộc chi nhánh của nhân viên thao tác, và đang ở trạng thái `PENDING`.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Màn hình quầy hiển thị đầy đủ thông tin hóa đơn, chi tiết số tiền và ảnh mã VietQR kèm số tài khoản ngân hàng thụ hưởng của chi nhánh.
- **Thất bại:** Không hiển thị mã VietQR; trả thông báo lỗi tương ứng.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Lễ tân mở chi tiết một hóa đơn đang ở trạng thái `PENDING` và chọn phương thức thanh toán "Chuyển khoản VietQR".
2. Hệ thống kiểm tra JWT (S2) và quyền thao tác của Lễ tân/Quản lý chi nhánh (S3).
3. Hệ thống kiểm tra hóa đơn tồn tại, thuộc chi nhánh hiện tại, và đang ở trạng thái `status = PENDING`.
4. Hệ thống tra cứu thông tin tài khoản ngân hàng của chi nhánh từ bảng `system_configs` theo `branch_id` (các key: `BANK_ID`, `BANK_ACCOUNT_NO`, `BANK_ACCOUNT_NAME`; nếu chi nhánh chưa có cấu hình riêng thì lấy giá trị mặc định toàn chuỗi với `branch_id` rỗng) và số tiền cần thanh toán `invoices.total`.
5. Hệ thống khởi tạo nội dung chuyển khoản chuẩn theo định dạng: `{branch_code} {invoice_code}` (ví dụ: `CN01 HD000088`).
6. Hệ thống tạo đường dẫn sinh ảnh VietQR Quick Link chuẩn Napas247:
   `https://img.vietqr.io/image/<BANK_ID>-<ACCOUNT_NO>-compact.png?amount=<total>&addInfo=<content>&accountName=<account_name>`
7. Hệ thống trả về `200 OK` kèm `VietQrPaymentDto` (chứa URL ảnh QR, thông tin ngân hàng thụ hưởng, mã hóa đơn, số tiền và cú pháp chuyển khoản).
8. Màn hình giao diện hiển thị mã VietQR cho khách hàng quét thanh toán.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **E1 – Chưa đăng nhập hoặc token hết hạn (401 Unauthorized):**
  - Tại bước 2, JWT không hợp lệ hoặc hết hạn.
  - Hệ thống trả `ProblemDetail` (401): *"Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại."*
- **E2 – Không có quyền truy cập (403 Forbidden):**
  - Tại bước 2, người dùng không phải Lễ tân/Quản lý, hoặc hóa đơn thuộc chi nhánh khác chi nhánh người dùng đang làm việc (S3).
  - Hệ thống trả `ProblemDetail` (403): *"Bạn không có quyền thực hiện thao tác này."*
- **E3 – Hóa đơn không tồn tại (404 Not Found):**
  - Tại bước 3, không tìm thấy `invoices` với `id` được yêu cầu.
  - Hệ thống trả `ProblemDetail` (404): *"Hóa đơn không tồn tại trên hệ thống."*
- **E4 – Hóa đơn không ở trạng thái chờ thanh toán (422 Unprocessable Content):**
  - Tại bước 3, hóa đơn có `status` là `PAID`, `CANCELLED` hoặc `REFUNDED`.
  - Hệ thống trả `ProblemDetail` (422): *"Hóa đơn này không ở trạng thái chờ thanh toán."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F5-01:** Số tiền nhúng trong mã VietQR bắt buộc lấy từ `invoices.total` được tính toán tại server (S7); client không được tự ý gửi số tiền lên để tạo QR.
- **BR-F5-02:** Nội dung chuyển khoản chuẩn hóa theo cấu trúc `{branch_code} {invoice_code}` để hỗ trợ nhân viên đối chiếu bằng mắt nhanh và chính xác với tin nhắn thông báo biến động số dư.
- **NF1:** API sinh thông tin VietQR phản hồi dưới 2 giây.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `invoices`, `branches`, `system_configs` (chỉ đọc)
- **Chi tiết trường dữ liệu:**
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `invoices` | `id` | `bigint` | PK | Khóa chính hóa đơn |
  | `invoices` | `code` | `varchar(30)` | UNIQUE, NOT NULL | Mã hiển thị của hóa đơn |
  | `invoices` | `total` | `int` | NOT NULL, CHECK >= 0 | Số tiền thanh toán (VND) |
  | `invoices` | `status` | `varchar(20)` | NOT NULL | Bắt buộc là `PENDING` |
  | `branches` | `code` | `varchar(20)` | NOT NULL | Mã chi nhánh (ghép vào nội dung chuyển khoản) |
  | `system_configs` | `config_key` | `varchar(100)` | NOT NULL | `BANK_ID`, `BANK_ACCOUNT_NO`, `BANK_ACCOUNT_NAME` |
  | `system_configs` | `config_value` | `varchar(255)` | NOT NULL | Giá trị tài khoản ngân hàng thụ hưởng |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `GET /api/v1/invoices/{id}/vietqr`
- **Header:** `Authorization: Bearer <access_token>`
- **Response Success (200 OK):**
  ```json
  {
    "invoiceId": 88,
    "invoiceCode": "HD000088",
    "total": 3000000,
    "bankId": "MB",
    "bankName": "MBBank (NHTM CP Quân Đội)",
    "accountNo": "0987654321",
    "accountName": "CONG TY GYM NHOM 6 CN01",
    "transferContent": "CN01 HD000088",
    "qrImageUrl": "https://img.vietqr.io/image/MB-0987654321-compact.png?amount=3000000&addInfo=CN01%20HD000088&accountName=CONG%20TY%20GYM%20NHOM%206%20CN01"
  }
  ```
- **Màn hình liên quan:** Màn hình "Thanh toán VietQR tại quầy" – `docs/design/wireframe/f5-thanh-toan-vietqr.drawio.svg`.

#### H. Tiêu chí chấp nhận (Acceptance Criteria)
- **Kịch bản 1: Sinh mã VietQR cho hóa đơn PENDING thành công**
  - **Given** Lễ tân chi nhánh CN01 đã đăng nhập; hóa đơn `id=88` có `status=PENDING`, `total=3000000` thuộc CN01
  - **When** Gọi `GET /api/v1/invoices/88/vietqr`
  - **Then** Hệ thống trả `200 OK` với đường dẫn `qrImageUrl` chứa số tiền `3000000` và nội dung chuyển khoản `CN01 HD000088`
- **Kịch bản 2: Gọi VietQR cho hóa đơn đã thanh toán (422)**
  - **Given** Hóa đơn `id=88` có `status=PAID`
  - **When** Gọi `GET /api/v1/invoices/88/vietqr`
  - **Then** Hệ thống trả `422 Unprocessable Content`: *"Hóa đơn này không ở trạng thái chờ thanh toán."*

---

### 3.2. UC-F5.2: Ghi nhận thanh toán tại quầy và kích hoạt dịch vụ

- **Mã UC:** UC-F5.2
- **Tên Use Case:** Ghi nhận thanh toán tại quầy và kích hoạt dịch vụ
- **Tác nhân chính:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`)
- **Tác nhân phụ:** Không có
- **Mô tả ngắn gọn:** Lễ tân bấm xác nhận sau khi đã thu đủ tiền mặt hoặc đã đối chiếu tin nhắn biến động số dư ngân hàng khớp với mã VietQR. Hệ thống tạo phiếu thu `payments`, chuyển hóa đơn sang `PAID`, và tự động kích hoạt gói tập (`memberships`) liên đới.

#### A. Tiền điều kiện (Preconditions)
- Lễ tân hoặc Quản lý chi nhánh đã đăng nhập với JWT hợp lệ (S2).
- Hóa đơn tồn tại trong hệ thống, thuộc chi nhánh của nhân viên thao tác, và đang ở trạng thái `PENDING`.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:**
  - Bảng `payments`: Thêm 1 dòng mới với `status = CONFIRMED`, `amount = invoices.total`, `received_by = user_id`, `paid_at = now()`.
  - Bảng `invoices`: Cập nhật `status = PAID`, `paid_at = now()`.
  - Bảng `memberships` (nếu hóa đơn chứa dòng gói tập `PACKAGE`):
    - Xác định số ngày hiệu lực của gói từ chính bản ghi `memberships` PENDING: `số ngày = (memberships.end_date - memberships.start_date + 1)` (không đọc lại `packages.duration_days` từ bảng `packages` để tránh bị ảnh hưởng nếu danh mục gói bị sửa đổi sau khi lập hóa đơn).
    - Chuyển `status = ACTIVE` (nếu hội viên mua mới, chưa có gói cùng loại đang `ACTIVE` hoặc `FROZEN` tại chi nhánh); `start_date` cập nhật bằng ngày thanh toán, `end_date = start_date + số ngày - 1`.
    - Chuyển `status = UPCOMING` (nếu là gói gia hạn nối tiếp khi hội viên đã có gói cùng loại đang `ACTIVE` hoặc `FROZEN` theo BR-F3-11); `start_date = end_date_gói_cũ + 1 ngày`, `end_date = start_date + số ngày - 1`.
- **Thất bại:** Dữ liệu giữ nguyên vẹn (rollback giao dịch); không có bản ghi `payments` rác.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Lễ tân chọn phương thức thanh toán (`CASH` hoặc `BANK_TRANSFER`), nhập mã tham chiếu giao dịch `transactionRef` (tùy chọn với chuyển khoản), và bấm "Xác nhận đã nhận tiền".
2. Hệ thống kiểm tra JWT hợp lệ và vai trò Lễ tân/Quản lý chi nhánh (Spring Security – S2, S3, S7).
3. Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào (Bean Validation – S5): `method` bắt buộc là `CASH` hoặc `BANK_TRANSFER`.
4. Hệ thống nạp hóa đơn từ bảng `invoices`, kiểm tra thuộc chi nhánh và có `status = PENDING`.
5. Hệ thống thực hiện các thay đổi dữ liệu trong cùng một giao dịch an toàn `@Transactional`:
   - a. Khóa lạc quan kiểm tra phiên bản `@Version` trên bản ghi `invoices` để chống ghi đè đồng thời.
   - b. Tạo bản ghi `payments`: `invoice_id`, `method`, `amount = invoices.total`, `status = CONFIRMED`, `transaction_ref`, `received_by = current_user.id`, `paid_at = now()`.
   - c. Cập nhật `invoices`: `status = PAID`, `paid_at = now()`.
   - d. Kích hoạt dịch vụ: Tra cứu các dòng `invoice_items` của hóa đơn:
     - Nếu có dòng `item_type = PACKAGE`: Tra cứu bản ghi `memberships` liên kết qua `invoice_item_id`.
     - Xác định số ngày hiệu lực của gói đã chốt lúc tạo hóa đơn: `số ngày = (memberships.end_date - memberships.start_date + 1)` (hệ thống tính toán từ 2 mốc ngày dự kiến đã lưu lúc tạo PENDING, **tuyệt đối không đọc lại `packages.duration_days`** từ bảng `packages` để tránh trường hợp quản lý chi nhánh chỉnh sửa cấu hình gói trong danh mục trong thời gian hóa đơn đang chờ thanh toán).
     - Nếu hội viên chưa có gói cùng loại đang `ACTIVE` hoặc `FROZEN`: cập nhật `memberships.status = ACTIVE`, `start_date = current_date`, `end_date = start_date + số ngày - 1`.
     - Nếu hội viên đã có gói cùng loại đang `ACTIVE` hoặc `FROZEN`: cập nhật `memberships.status = UPCOMING`, `start_date = end_date_gói_cũ + 1 ngày`, `end_date = start_date + số ngày - 1` (theo BR-F3-11).
6. Hệ thống trả về `200 OK` kèm `PaymentConfirmationDto` thông báo xác nhận thanh toán thành công và trạng thái gói tập đã được kích hoạt.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):**
  - Tại bước 3, `method` không hợp lệ (khác `CASH` và `BANK_TRANSFER`).
  - Hệ thống trả `ProblemDetail` (400) thông báo lỗi chi tiết trường `method`.
- **E2 – Chưa đăng nhập hoặc token hết hạn (401 Unauthorized):**
  - Tại bước 2, JWT không hợp lệ hoặc hết hạn.
  - Hệ thống trả `ProblemDetail` (401): *"Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại."*
- **E3 – Không có quyền thao tác (403 Forbidden):**
  - Tại bước 2, người dùng không phải Lễ tân hoặc Quản lý chi nhánh (ví dụ: Huấn luyện viên, Hội viên cố gọi API xác nhận – vi phạm S7).
  - Hệ thống trả `ProblemDetail` (403): *"Bạn không có quyền thực hiện thao tác này."*
- **E4 – Hóa đơn không tồn tại (404 Not Found):**
  - Tại bước 4, không tìm thấy hóa đơn theo `id`.
  - Hệ thống trả `ProblemDetail` (404): *"Hóa đơn không tồn tại trên hệ thống."*
- **E5 – Xung đột dữ liệu đồng thời (409 Conflict):**
  - Tại bước 5a, hai lễ tân cùng mở màn hình và cùng bấm xác nhận thu tiền cho cùng một hóa đơn; phiên bản `invoices.version` bị xung đột (`@Version`).
  - Hệ thống hủy giao dịch của người bấm sau và trả `ProblemDetail` (409): *"Hóa đơn này vừa được xác nhận thanh toán bởi một nhân viên khác."*
- **E6 – Hóa đơn không ở trạng thái chờ thanh toán (422 Unprocessable Content):**
  - Tại bước 4, hóa đơn không có `status = PENDING` (đã là `PAID`, `CANCELLED` hoặc `REFUNDED`).
  - Hệ thống trả `ProblemDetail` (422): *"Hóa đơn đã được thanh toán hoặc đã bị hủy trước đó."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F5-03:** Chỉ Lễ tân và Quản lý chi nhánh mới có quyền xác nhận đã thu tiền tại quầy (S7, [ADR 0004](../../decisions/0004-thanh-toan-chi-tai-quay.md)).
- **BR-F5-04:** Gói tập chỉ kích hoạt (`ACTIVE` hoặc `UPCOMING`) khi và chỉ khi hóa đơn chuyển sang trạng thái `PAID`. Ngày bắt đầu và ngày kết thúc của gói được tính toán lại tại chính thời điểm thanh toán thành công (BR-F3-12). Số ngày hiệu lực của gói được bảo toàn nguyên vẹn dựa trên khoảng ngày dự kiến đã lưu lúc lập hóa đơn (`memberships.end_date - memberships.start_date + 1`), không bị ảnh hưởng nếu Quản lý chi nhánh thay đổi `packages.duration_days` trong danh mục sau đó (BR-F3-06). Nếu hội viên đã có gói cùng loại đang `ACTIVE` hoặc `FROZEN`, gói mới bắt buộc chuyển `UPCOMING` nối tiếp ngày hết hạn của gói cũ để đảm bảo ràng buộc duy nhất `uq_one_active_per_type` (BR-F3-10, BR-F3-11).
- **BR-F5-05:** Toàn vẹn đồng thời (NF6): Bảng `invoices` sử dụng khóa lạc quan `@Version`. Nếu hai nhân viên cùng xác nhận một hóa đơn, chỉ một giao dịch thành công, giao dịch còn lại nhận lỗi 409.
- **NF1:** Phản hồi xác nhận thanh toán và kích hoạt gói dưới 2 giây.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `invoices`, `payments`, `memberships`, `invoice_items`
- **Chi tiết trường dữ liệu:**
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `payments` | `id` | `bigint` | PK, Generated Always | Khóa chính tự tăng |
  | `payments` | `invoice_id` | `bigint` | NOT NULL, FK → `invoices.id` | Hóa đơn được thanh toán |
  | `payments` | `method` | `varchar(20)` | NOT NULL, CHECK IN ('CASH','BANK_TRANSFER') | Hình thức thanh toán |
  | `payments` | `amount` | `int` | NOT NULL, CHECK amount > 0 | Số tiền thu (bằng `invoices.total`) |
  | `payments` | `status` | `varchar(20)` | NOT NULL, default 'CONFIRMED' | Trạng thái phiếu thu |
  | `payments` | `transaction_ref` | `varchar(100)` | nullable | Mã giao dịch ngân hàng / tham chiếu VietQR |
  | `payments` | `received_by` | `bigint` | NOT NULL, FK → `users.id` | Nhân viên xác nhận thu tiền |
  | `payments` | `paid_at` | `timestamptz` | NOT NULL | Thời điểm xác nhận thu tiền (UTC) |
  | `invoices` | `status` | `varchar(20)` | NOT NULL | Chuyển `PENDING` → `PAID` |
  | `invoices` | `paid_at` | `timestamptz` | nullable | Cập nhật thời điểm thanh toán |
  | `invoices` | `version` | `int` | NOT NULL, default 0 | Khóa lạc quan chống ghi đè đồng thời |
  | `memberships` | `status` | `varchar(20)` | NOT NULL | Chuyển `PENDING` → `ACTIVE` hoặc `UPCOMING` |
  | `memberships` | `start_date` | `date` | NOT NULL | Tính lại ngày bắt đầu |
  | `memberships` | `end_date` | `date` | NOT NULL | Tính lại ngày kết thúc |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `POST /api/v1/invoices/{id}/payments`
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body (DTO):**
  ```json
  {
    "method": "BANK_TRANSFER",
    "transactionRef": "FT263078912345"
  }
  ```
- **Response Success (200 OK):**
  ```json
  {
    "paymentId": 105,
    "invoiceId": 88,
    "invoiceCode": "HD000088",
    "method": "BANK_TRANSFER",
    "amount": 3000000,
    "status": "PAID",
    "paidAt": "2026-11-03T09:15:00Z",
    "receivedBy": 2,
    "receivedByName": "Nguyễn Văn Lễ Tân",
    "activatedMemberships": [
      {
        "membershipId": 77,
        "packageType": "PT",
        "status": "ACTIVE",
        "startDate": "2026-11-03",
        "endDate": "2027-01-31",
        "remainingSessions": 10
      }
    ]
  }
  ```
- **Màn hình liên quan:** Màn hình "Xác nhận thanh toán tại quầy" – `docs/design/wireframe/f5-thanh-toan-vietqr.drawio.svg`.

#### H. Tiêu chí chấp nhận (Acceptance Criteria)
- **Kịch bản 1: Xác nhận thanh toán tiền mặt kích hoạt gói ACTIVE**
  - **Given** Lễ tân đã đăng nhập; hóa đơn `id=88` (`status=PENDING`, `total=500000`) chứa gói TIME 30 ngày của hội viên chưa có gói
  - **When** Gọi `POST /api/v1/invoices/88/payments` với `method=CASH`
  - **Then** Hệ thống trả `200 OK`; `invoices.status = PAID`; bảng `payments` ghi nhận 1 bản ghi `amount=500000`; gói tập chuyển `status=ACTIVE`, `start_date` = ngày hôm nay, `end_date` = hôm nay + 29 ngày
- **Kịch bản 2: Xác nhận thanh toán chuyển khoản kích hoạt gói gia hạn nối tiếp UPCOMING**
  - **Given** Hội viên có gói TIME `status=ACTIVE` (hoặc `status=FROZEN`) đến hết ngày `2026-12-31`; hóa đơn `id=89` là hóa đơn gia hạn gói TIME 30 ngày đang `PENDING`
  - **When** Lễ tân gọi `POST /api/v1/invoices/89/payments` với `method=BANK_TRANSFER`
  - **Then** Hệ thống trả `200 OK`; gói mới chuyển `status=UPCOMING`, `start_date=2027-01-01`, `end_date=2027-01-30`; gói cũ vẫn giữ nguyên trạng thái `ACTIVE` (hoặc `FROZEN`)
- **Kịch bản 3: Hai lễ tân cùng bấm xác nhận một hóa đơn (409 Conflict)**
  - **Given** Hóa đơn `id=88` đang `PENDING` với `version=0`
  - **When** Lễ tân A và Lễ tân B cùng bấm xác nhận gần như đồng thời
  - **Then** Một request thành công trả về `200 OK` (tăng `version=1`); request còn lại bị từ chối trả về `409 Conflict`

---

### 3.3. UC-F5.3: Tra cứu và quản lý hóa đơn tại quầy

- **Mã UC:** UC-F5.3
- **Tên Use Case:** Tra cứu và quản lý hóa đơn tại quầy
- **Tác nhân chính:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`)
- **Tác nhân phụ:** Không có
- **Mô tả ngắn gọn:** Nhân viên xem danh sách hóa đơn theo chi nhánh, tìm kiếm theo mã hóa đơn, hội viên, trạng thái hoặc khoảng thời gian. Cho phép hủy hóa đơn chờ `PENDING` khi khách đổi ý không mua.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Lễ tân hoặc Quản lý chi nhánh (JWT hợp lệ – S2).

#### B. Hậu điều kiện (Postconditions)
- **Thành công (Xem):** Danh sách hóa đơn phân trang được trả về kèm trạng thái và tổng tiền.
- **Thành công (Hủy hóa đơn chờ):** `invoices.status = CANCELLED`; các bản ghi `memberships` đang ở trạng thái `PENDING` đi kèm chuyển sang `CANCELLED`.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Nhân viên truy cập danh sách hóa đơn, chọn các tiêu chí lọc: `status` (`PENDING`, `PAID`, `CANCELLED`, `REFUNDED`), `source` (`COUNTER`, `MEMBER_PORTAL`), `fromDate`, `toDate`, hoặc từ khóa tìm kiếm (`keyword` = mã hóa đơn, tên hoặc số điện thoại hội viên).
2. Hệ thống kiểm tra JWT (S2) và vai trò người dùng (S3).
3. Hệ thống tự động gắn điều kiện lọc theo `branch_id` của chi nhánh người dùng đang làm việc (không cho xem chéo chi nhánh – S3).
4. Hệ thống truy vấn CSDL và trả về `PageResponse<InvoiceSummaryDto>` sắp xếp giảm dần theo thời gian tạo (`created_at DESC`).

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Hủy hóa đơn chờ tại quầy (Cancel Pending Invoice):**
  - Tại danh sách, Lễ tân chọn một hóa đơn đang ở trạng thái `PENDING` và bấm "Hủy hóa đơn".
  - Hệ thống kiểm tra hóa đơn đang ở trạng thái `PENDING`.
  - Trong giao dịch `@Transactional`, hệ thống cập nhật `invoices.status = CANCELLED`. Đồng thời tìm các gói `memberships` đang `PENDING` gắn với hóa đơn này và cập nhật sang `CANCELLED`.
  - Hệ thống trả `200 OK` xác nhận đã hủy hóa đơn.
- **E1 – Chưa đăng nhập hoặc token hết hạn (401 Unauthorized):**
  - Tại bước 2, JWT không có hoặc hết hạn.
  - Hệ thống trả `ProblemDetail` (401): *"Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại."*
- **E2 – Không có quyền thao tác (403 Forbidden):**
  - Người dùng truy cập ngoài phạm vi chi nhánh của mình hoặc không đúng vai trò cho phép.
  - Hệ thống trả `ProblemDetail` (403): *"Bạn không có quyền thực hiện thao tác này."*
- **E3 – Không tìm thấy hóa đơn (404 Not Found):**
  - Hóa đơn yêu cầu hủy không tồn tại trên hệ thống.
  - Hệ thống trả `ProblemDetail` (404): *"Hóa đơn không tồn tại trên hệ thống."*
- **E4 – Hóa đơn không thể hủy (422 Unprocessable Content):**
  - Lễ tân cố bấm hủy một hóa đơn đã `PAID` hoặc đã `REFUNDED`.
  - Hệ thống từ chối và trả `ProblemDetail` (422): *"Chỉ có thể hủy hóa đơn đang ở trạng thái chờ thanh toán."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F5-06:** Lễ tân và Quản lý chi nhánh chỉ được xem và thao tác trên hóa đơn thuộc chi nhánh của mình; Chủ chuỗi xem được hóa đơn toàn hệ thống (S3).
- **BR-F5-07:** Hóa đơn lập tại quầy (`source = COUNTER`) **không tự động hủy**; chỉ hóa đơn do hội viên tự tạo từ cổng web (`source = MEMBER_PORTAL` – F7) mới tự hủy sau `PENDING_INVOICE_TTL_DAYS` (3 ngày) ([ADR 0004](../../decisions/0004-thanh-toan-chi-tai-quay.md)).
- **BR-F5-08:** Hủy hóa đơn `PENDING` thì bắt buộc phải đồng thời hủy gói tập `memberships` ở trạng thái `PENDING` tương ứng để không để lại dữ liệu rác.
- **NF1:** Tải danh sách hóa đơn và tìm kiếm phản hồi dưới 2 giây.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `invoices`, `memberships`
- **Chi tiết trường dữ liệu:**
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `invoices` | `id` | `bigint` | PK | Khóa chính hóa đơn |
  | `invoices` | `code` | `varchar(30)` | UNIQUE, NOT NULL | Mã hóa đơn |
  | `invoices` | `branch_id` | `bigint` | NOT NULL | Dùng để phân quyền chi nhánh (S3) |
  | `invoices` | `member_id` | `bigint` | nullable | Hội viên liên kết (nếu có) |
  | `invoices` | `source` | `varchar(20)` | NOT NULL | `COUNTER` hoặc `MEMBER_PORTAL` |
  | `invoices` | `total` | `int` | NOT NULL | Tổng tiền cần thu / đã thu |
  | `invoices` | `status` | `varchar(20)` | NOT NULL | Lọc trạng thái |
  | `invoices` | `created_at` | `timestamptz` | NOT NULL | Thời điểm tạo hóa đơn |
  | `memberships` | `status` | `varchar(20)` | NOT NULL | Chuyển `CANCELLED` khi hóa đơn PENDING bị hủy |

#### G. Thiết kế giao diện và API liên quan
- **API Tra cứu danh sách:** `GET /api/v1/invoices`
  - **Query Parameters:** `status`, `source`, `keyword`, `fromDate`, `toDate`, `page=0&size=20`
  - **Response Success (200 OK):**
    ```json
    {
      "content": [
        {
          "id": 88,
          "code": "HD000088",
          "memberId": 123,
          "memberCode": "HV000123",
          "memberName": "Nguyễn Văn A",
          "total": 3000000,
          "source": "COUNTER",
          "status": "PENDING",
          "createdAt": "2026-11-03T08:30:00Z"
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1
    }
    ```
- **API Hủy hóa đơn chờ:** `PUT /api/v1/invoices/{id}/cancel`
  - **Header:** `Authorization: Bearer <access_token>`
  - **Response Success (200 OK):**
    ```json
    {
      "id": 88,
      "code": "HD000088",
      "status": "CANCELLED",
      "updatedAt": "2026-11-03T10:00:00Z"
    }
    ```
- **Màn hình liên quan:** Màn hình "Quản lý hóa đơn tại quầy" – `docs/design/wireframe/f5-hoa-don.drawio.svg`.

#### H. Tiêu chí chấp nhận (Acceptance Criteria)
- **Kịch bản 1: Lễ tân hủy hóa đơn PENDING thành công**
  - **Given** Hóa đơn `id=88` đang `PENDING` kèm gói tập `id=77` đang `PENDING`
  - **When** Lễ tân gọi `PUT /api/v1/invoices/88/cancel`
  - **Then** Hệ thống trả `200 OK`; `invoices.status = CANCELLED`; `memberships.status = CANCELLED`
- **Kịch bản 2: Lễ tân cố hủy hóa đơn đã thanh toán (422)**
  - **Given** Hóa đơn `id=89` có `status=PAID`
  - **When** Lễ tân gọi `PUT /api/v1/invoices/89/cancel`
  - **Then** Hệ thống trả `422 Unprocessable Content`: *"Chỉ có thể hủy hóa đơn đang ở trạng thái chờ thanh toán."*

---

### 3.4. UC-F5.4: In và xuất hóa đơn PDF

- **Mã UC:** UC-F5.4
- **Tên Use Case:** In và xuất hóa đơn PDF
- **Tác nhân chính:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`), Hội viên (`MEMBER`)
- **Tác nhân phụ:** Không có
- **Mô tả ngắn gọn:** Xuất file hóa đơn thanh toán định dạng PDF chuẩn để in nhiệt hoặc tải về lưu trữ, hiển thị chi tiết dòng dịch vụ, đơn giá lúc bán và số tiền đã thanh toán.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với JWT hợp lệ (S2).
- Hóa đơn ở trạng thái `PAID` hoặc `REFUNDED`.
- Nếu là Hội viên: Hóa đơn phải thuộc quyền sở hữu của chính hội viên đó (`invoices.member_id = current_member.id` – S3).

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Trả về luồng dữ liệu nhị phân file PDF (`application/pdf`) hiển thị hóa đơn thanh toán hoàn chỉnh.
- **Thất bại:** Không xuất file, trả về mã lỗi thích hợp.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Người dùng bấm "In hóa đơn" hoặc "Tải hóa đơn PDF" từ chi tiết hóa đơn.
2. Hệ thống kiểm tra JWT và xác thực phân quyền (S2, S3):
   - Nhân viên chi nhánh chỉ xem được hóa đơn thuộc chi nhánh mình.
   - Hội viên chỉ xem được hóa đơn mang `member_id` của chính mình.
3. Hệ thống kiểm tra hóa đơn tồn tại và đã ở trạng thái `PAID` hoặc `REFUNDED`.
4. Hệ thống nạp dữ liệu: Thông tin phòng tập & chi nhánh (tên, địa chỉ, hotline), mã hóa đơn, ngày thanh toán, thông tin hội viên (tên, SĐT, mã HV), danh sách `invoice_items` (mô tả, số lượng, đơn giá lúc bán, thành tiền), tổng tiền thanh toán, và thông tin giao dịch `payments` (phương thức thanh toán, mã giao dịch).
5. Hệ thống kết xuất file PDF bằng thư viện sinh tài liệu (OpenPDF).
6. Hệ thống trả về file PDF với `Content-Type: application/pdf` và header `Content-Disposition: inline; filename="hoa-don-{code}.pdf"`.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **E1 – Chưa đăng nhập (401 Unauthorized):**
  - JWT không hợp lệ hoặc hết hạn.
  - Hệ thống trả `ProblemDetail` (401).
- **E2 – Không có quyền truy cập hóa đơn (403 Forbidden):**
  - Hội viên A cố tình truy cập tải hóa đơn của Hội viên B (vi phạm S3).
  - Hệ thống trả `ProblemDetail` (403): *"Bạn không có quyền xem hóa đơn này."*
- **E3 – Hóa đơn không tồn tại (404 Not Found):**
  - Không tìm thấy hóa đơn theo `id`.
  - Hệ thống trả `ProblemDetail` (404): *"Hóa đơn không tồn tại trên hệ thống."*
- **E4 – Hóa đơn chưa thanh toán (422 Unprocessable Content):**
  - Hóa đơn đang ở trạng thái `PENDING` hoặc `CANCELLED`.
  - Hệ thống từ chối xuất file và trả `ProblemDetail` (422): *"Chỉ có thể xuất hóa đơn cho các giao dịch đã thanh toán hoặc đã hoàn tiền."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F5-09:** Dữ liệu in trên hóa đơn PDF về tên sản phẩm/gói tập và đơn giá **bắt buộc lấy từ `invoice_items.description` và `invoice_items.unit_price`** (ảnh chụp tại thời điểm bán), không được truy vấn ngược lại `packages.price` hiện tại để đảm bảo tính toàn vẹn lịch sử hóa đơn.
- **BR-F5-10:** Không làm chức năng xuất hóa đơn điện tử có mã cơ quan thuế (thuộc danh mục ngoài phạm vi dự án – xem Bản chốt phạm vi).
- **NF1:** Thời gian sinh và trả về file PDF dưới 2 giây.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `invoices`, `invoice_items`, `payments`, `branches`, `members` (chỉ đọc)
- **Chi tiết trường dữ liệu xuất PDF:**
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ý nghĩa khi in trên PDF |
  | --- | --- | --- | --- |
  | `branches` | `name`, `address`, `phone` | `varchar` | Thông tin cơ sở phòng tập trên tiêu đề hóa đơn |
  | `invoices` | `code` | `varchar(30)` | Số/mã hóa đơn hiển thị |
  | `invoices` | `paid_at` | `timestamptz` | Ngày giờ thanh toán hóa đơn |
  | `invoices` | `subtotal`, `discount`, `total` | `int` | Tiền hàng, giảm giá (0 ở v1), và tổng thanh toán |
  | `invoice_items` | `description` | `varchar(255)` | Tên gói/sản phẩm đã mua (chụp lúc bán) |
  | `invoice_items` | `quantity`, `unit_price`, `amount` | `int` | Số lượng, đơn giá lúc bán và thành tiền |
  | `payments` | `method`, `transaction_ref` | `varchar` | Hình thức thu tiền (Tiền mặt / Chuyển khoản) |
  | `members` | `member_code`, `full_name`, `phone` | `varchar` | Thông tin người mua hàng (nếu có) |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `GET /api/v1/invoices/{id}/pdf`
- **Header:** `Authorization: Bearer <access_token>`
- **Response Success (200 OK):**
  - Header: `Content-Type: application/pdf`
  - Body: Binary stream dữ liệu PDF

#### H. Tiêu chí chấp nhận (Acceptance Criteria)
- **Kịch bản 1: Lễ tân in hóa đơn đã thanh toán thành công**
  - **Given** Hóa đơn `id=88` có `status=PAID` thuộc chi nhánh Lễ tân đang trực
  - **When** Lễ tân gọi `GET /api/v1/invoices/88/pdf`
  - **Then** Hệ thống trả về `200 OK` với stream file PDF, hiển thị đúng mã hóa đơn `HD000088`, tên gói tập và đơn giá tại thời điểm bán
- **Kịch bản 2: Hội viên A cố tải hóa đơn của Hội viên B (403)**
  - **Given** Hội viên A đã đăng nhập; hóa đơn `id=90` thuộc về Hội viên B
  - **When** Hội viên A gọi `GET /api/v1/invoices/90/pdf`
  - **Then** Hệ thống chặn lại và trả về `403 Forbidden`

---

### 3.5. UC-F5.5: Hoàn tiền hóa đơn (Refund)

- **Mã UC:** UC-F5.5
- **Tên Use Case:** Hoàn tiền hóa đơn
- **Tác nhân chính:** Quản lý chi nhánh (`BRANCH_MANAGER`)
- **Tác nhân phụ:** Không có
- **Mô tả ngắn gọn:** Quản lý chi nhánh thực hiện hoàn tiền cho hóa đơn đã thanh toán khi có khiếu nại hợp lệ. Hệ thống cập nhật hóa đơn sang `REFUNDED`, hủy các gói tập liên đới và ghi vết kiểm toán vào `audit_logs`.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò **Quản lý chi nhánh** (`BRANCH_MANAGER`) (S3).
- Hóa đơn cần hoàn tiền đang ở trạng thái `PAID` và thuộc chi nhánh do quản lý đó điều hành.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:**
  - `invoices.status = REFUNDED`, `refunded_by = user_id`, `refunded_at = now()`, `refund_reason` được cập nhật.
  - Các gói tập (`memberships`) liên kết với hóa đơn này chuyển trạng thái `status = CANCELLED`.
  - Một bản ghi vết kiểm toán được thêm mới vào bảng `audit_logs` (S9).
- **Thất bại:** Trạng thái hóa đơn và gói tập giữ nguyên vẹn; giao dịch rollback.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Quản lý chi nhánh xem chi tiết hóa đơn `PAID`, bấm nút "Hoàn tiền", nhập lý do hoàn tiền `refundReason` và xác nhận.
2. Hệ thống kiểm tra JWT và xác thực phân quyền: **Chỉ duy nhất Quản lý chi nhánh** có quyền thực hiện hoàn tiền (S3, [ADR 0004](../../decisions/0004-thanh-toan-chi-tai-quay.md)).
3. Hệ thống kiểm tra dữ liệu đầu vào: `refundReason` bắt buộc không được để trống (Bean Validation – S5).
4. Hệ thống kiểm tra trạng thái hóa đơn: Bắt buộc là `PAID` và thuộc chi nhánh quản lý.
5. Trong một giao dịch an toàn `@Transactional`:
   - a. Khóa lạc quan kiểm tra `invoices.version` để chống xung đột xử lý.
   - b. Cập nhật `invoices`: `status = REFUNDED`, `refunded_by = current_user.id`, `refunded_at = now()`, `refund_reason = refundReason`.
   - c. Vô hiệu hóa gói tập: Tra cứu các dòng `invoice_items` kiểu `PACKAGE` của hóa đơn, cập nhật các bản ghi `memberships` liên kết sang `status = CANCELLED`.
   - d. Ghi nhật ký kiểm toán vào bảng `audit_logs`: `user_id`, `branch_id`, `action = 'REFUND'`, `entity = 'invoices'`, `entity_id = invoice.id`, `old_value`, `new_value`, địa chỉ IP (S9).
6. Hệ thống trả về `200 OK` thông báo hoàn tiền thành công.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **E1 – Thiếu lý do hoàn tiền (400 Bad Request):**
  - Tại bước 3, quản lý không nhập `refundReason`.
  - Hệ thống trả `ProblemDetail` (400) thông báo lỗi trường `refundReason` là bắt buộc.
- **E2 – Chưa đăng nhập (401 Unauthorized):**
  - JWT không hợp lệ hoặc hết hạn.
  - Hệ thống trả `ProblemDetail` (401).
- **E3 – Không có quyền hoàn tiền (403 Forbidden):**
  - Tại bước 2, Lễ tân hoặc Huấn luyện viên gọi API này.
  - Hệ thống từ chối và trả `ProblemDetail` (403): *"Chỉ quản lý chi nhánh mới có quyền hoàn tiền hóa đơn."*
- **E4 – Hóa đơn không tồn tại (404 Not Found):**
  - Không tìm thấy hóa đơn theo `id`.
  - Hệ thống trả `ProblemDetail` (404): *"Hóa đơn không tồn tại trên hệ thống."*
- **E5 – Xung đột dữ liệu đồng thời (409 Conflict):**
  - Phiên bản hóa đơn bị thay đổi đồng thời.
  - Hệ thống trả `ProblemDetail` (409): *"Dữ liệu hóa đơn vừa được cập nhật bởi thao tác khác. Vui lòng thử lại."*
- **E6 – Hóa đơn không đủ điều kiện hoàn tiền (422 Unprocessable Content):**
  - Tại bước 4, hóa đơn không ở trạng thái `PAID` (ví dụ: đang `PENDING`, đã `CANCELLED`, hoặc đã được `REFUNDED` trước đó).
  - Hệ thống từ chối và trả `ProblemDetail` (422): *"Chỉ có thể hoàn tiền cho hóa đơn đã thanh toán thành công."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F5-11:** Phân quyền hoàn tiền: Chỉ có vai trò Quản lý chi nhánh (`BRANCH_MANAGER`) mới được quyền hoàn tiền. Lễ tân không có quyền này ([Bản chốt phạm vi](../ban-chot-pham-vi.md)).
- **BR-F5-12:** Truy vết kiểm toán bắt buộc (S9): Mọi hành động hoàn tiền bắt buộc phải lưu vết vào bảng `audit_logs` gồm người duyệt, thời gian, số tiền và lý do hoàn tiền.
- **BR-F5-13:** Tính nhất quán dữ liệu: Khi hóa đơn bị hoàn tiền, tất cả các gói tập `memberships` sinh ra từ hóa đơn đó lập tức chuyển sang `CANCELLED` để hội viên không thể tiếp tục check-in (F4) hay đặt lịch (F8, F9).

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `invoices`, `memberships`, `audit_logs`
- **Chi tiết trường dữ liệu:**
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `invoices` | `status` | `varchar(20)` | NOT NULL | Chuyển `PAID` → `REFUNDED` |
  | `invoices` | `refunded_by` | `bigint` | nullable, FK → `users.id` | Quản lý duyệt hoàn tiền |
  | `invoices` | `refunded_at` | `timestamptz` | nullable | Thời điểm hoàn tiền |
  | `invoices` | `refund_reason` | `text` | nullable | Lý do hoàn tiền bắt buộc |
  | `memberships` | `status` | `varchar(20)` | NOT NULL | Chuyển sang `CANCELLED` |
  | `audit_logs` | `user_id` | `bigint` | nullable, FK → `users.id` | Quản lý thực hiện hoàn tiền |
  | `audit_logs` | `branch_id` | `bigint` | nullable, FK → `branches.id` | Chi nhánh phát sinh hoàn tiền |
  | `audit_logs` | `action` | `varchar(50)` | NOT NULL | Ghi nhận giá trị 'REFUND' |
  | `audit_logs` | `entity` | `varchar(50)` | NOT NULL | Ghi nhận giá trị 'invoices' |
  | `audit_logs` | `entity_id` | `bigint` | nullable | ID của hóa đơn được hoàn |
  | `audit_logs` | `old_value` | `jsonb` | nullable | Trạng thái hóa đơn trước khi hoàn tiền |
  | `audit_logs` | `new_value` | `jsonb` | nullable | Trạng thái hóa đơn sau khi hoàn tiền |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `POST /api/v1/invoices/{id}/refund`
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body (DTO):**
  ```json
  {
    "refundReason": "Hội viên chuyển nơi sinh sống, chấp thuận hoàn tiền theo quy chế chi nhánh"
  }
  ```
- **Response Success (200 OK):**
  ```json
  {
    "invoiceId": 88,
    "invoiceCode": "HD000088",
    "status": "REFUNDED",
    "refundedAt": "2026-11-03T11:00:00Z",
    "refundedBy": 3,
    "refundedByName": "Trần Quản Lý",
    "refundReason": "Hội viên chuyển nơi sinh sống, chấp thuận hoàn tiền theo quy chế chi nhánh"
  }
  ```
- **Màn hình liên quan:** Màn hình "Chi tiết hóa đơn và hoàn tiền" – `docs/design/wireframe/f5-hoa-don.drawio.svg`.

#### H. Tiêu chí chấp nhận (Acceptance Criteria)
- **Kịch bản 1: Quản lý chi nhánh hoàn tiền thành công**
  - **Given** Người dùng là Quản lý chi nhánh CN01; hóa đơn `id=88` có `status=PAID` thuộc CN01, gói tập `id=77` đang `ACTIVE`
  - **When** Gọi `POST /api/v1/invoices/88/refund` với lý do hợp lệ
  - **Then** Hệ thống trả `200 OK`; `invoices.status = REFUNDED`; gói tập `id=77` chuyển `CANCELLED`; một bản ghi mới được ghi vào `audit_logs`
- **Kịch bản 2: Lễ tân cố tình thực hiện hoàn tiền (403)**
  - **Given** Người dùng là Lễ tân
  - **When** Gọi `POST /api/v1/invoices/88/refund`
  - **Then** Hệ thống chặn lại và trả về `403 Forbidden`: *"Chỉ quản lý chi nhánh mới có quyền hoàn tiền hóa đơn."*

