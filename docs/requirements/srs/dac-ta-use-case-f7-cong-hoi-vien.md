# Đặc tả Use Case – F7: Cổng hội viên

- **Mã chức năng:** F7
- **Version:** v2 (xác nhận Bắt buộc / Mục tiêu theo bản chốt phạm vi)
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
- **Gửi yêu cầu mua hoặc gia hạn gói:** hệ thống tạo hóa đơn chờ và gói ở trạng thái PENDING, hội viên hoàn tất thanh toán theo quy trình đã chốt.

Vai trò được phép dùng: **Hội viên** (đã đăng nhập). Hội viên chỉ xem được dữ liệu của chính mình. Ở Sprint 1, tài liệu này chỉ đặc tả; phần hiện thực làm ở Sprint 2.

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
- **Mô tả ngắn gọn:** Hội viên xem các gói tập của mình cùng trạng thái và ngày hết hạn.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Hội viên và có hồ sơ hội viên.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Hệ thống hiển thị danh sách gói của hội viên. Không có dữ liệu nào bị thay đổi.
- **Thất bại:** Không có dữ liệu nào bị thay đổi.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Hội viên mở mục "Gói của tôi" trên cổng hội viên.
2. Hệ thống kiểm tra quyền và vai trò của người thao tác (S3); mã hội viên lấy từ token đăng nhập, không lấy từ request.
3. Hệ thống truy vấn các gói của hội viên đó.
4. Hệ thống trả về kết quả thành công (200 OK) gồm: tên gói, trạng thái, ngày bắt đầu, ngày hết hạn.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Hội viên chưa có gói nào:**
  - Tại bước 3, nếu không có gói nào, hệ thống trả danh sách rỗng và giao diện hiển thị "Bạn chưa có gói tập".
- **E2 – Không có quyền truy cập tài nguyên (403 Forbidden):**
  - Tại bước 2, người thao tác không phải Hội viên hoặc chưa có hồ sơ hội viên.
  - Hệ thống trả `ProblemDetail` (403): *"Bạn không có quyền thực hiện thao tác này."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F7.1:** Hội viên chỉ xem được dữ liệu của chính mình (S3).
- **BR-F7.2:** Gói ở trạng thái PENDING được hiển thị kèm nhãn "Chờ thanh toán".
- **[Yêu cầu phi chức năng]:**
  - Bảo mật (S2, S3): kiểm tra JWT, phân quyền ở backend.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động (chỉ đọc):** `members`, `[bảng gói đã đăng ký của hội viên]`, `packages`
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`)*
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `[...]` | `[...]` | `[...]` | `[...]` | Trạng thái gói |
  | `[...]` | `[...]` | `[...]` | `[...]` | Ngày bắt đầu, ngày hết hạn |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `GET /api/v1/me/packages` *(đề xuất, thống nhất với người làm API)*
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
- Người dùng đã đăng nhập với vai trò Hội viên và có hồ sơ hội viên.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Hệ thống hiển thị lịch sử check-in của hội viên. Không có dữ liệu nào bị thay đổi.
- **Thất bại:** Không có dữ liệu nào bị thay đổi.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Hội viên mở mục "Lịch sử check-in".
2. Hệ thống kiểm tra tính hợp lệ của tham số phân trang (Bean Validation).
3. Hệ thống kiểm tra quyền và vai trò (S3); mã hội viên lấy từ token.
4. Hệ thống truy vấn các lần check-in của hội viên, sắp xếp mới nhất trước.
5. Hệ thống trả về kết quả (200 OK) dạng `PageResponse` gồm: thời điểm check-in, chi nhánh.

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
  - Hiệu năng (NF1): thời gian phản hồi API dưới [X] giây *(lấy giá trị theo NF1 trong SRS)*.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động (chỉ đọc):** `checkins`, `members`
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`)*
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `checkins` | `[...]` | `[...]` | `[...]` | Thời điểm check-in |
  | `checkins` | `[...]` | `[...]` | `[...]` | Chi nhánh |

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
- **Mô tả ngắn gọn:** Hội viên xem danh sách hóa đơn của mình, gồm cả hóa đơn chờ thanh toán.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Hội viên và có hồ sơ hội viên.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Hệ thống hiển thị danh sách hóa đơn của hội viên. Không có dữ liệu nào bị thay đổi.
- **Thất bại:** Không có dữ liệu nào bị thay đổi.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Hội viên mở mục "Hóa đơn".
2. Hệ thống kiểm tra tham số phân trang (Bean Validation).
3. Hệ thống kiểm tra quyền và vai trò (S3); mã hội viên lấy từ token.
4. Hệ thống truy vấn các hóa đơn của hội viên, mới nhất trước.
5. Hệ thống trả về kết quả (200 OK) dạng `PageResponse` gồm: mã hóa đơn, số tiền, trạng thái, ngày tạo, và hạn tự hủy với hóa đơn chờ.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Chưa có hóa đơn nào:**
  - Tại bước 4, hệ thống trả danh sách rỗng; giao diện hiển thị "Bạn chưa có hóa đơn".
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):**
  - Tại bước 2, tham số phân trang không hợp lệ; hệ thống trả `ProblemDetail` (400).
- **E2 – Không có quyền truy cập tài nguyên (403 Forbidden):**
  - Tại bước 3, người thao tác không phải Hội viên; hệ thống trả `ProblemDetail` (403).

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F7.1:** Hội viên chỉ xem được hóa đơn của chính mình (S3).
- **BR-F7.3:** Số tiền hiển thị lấy từ dữ liệu do server tính, không nhận từ phía client (S7).

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động (chỉ đọc):** `[bảng hóa đơn]`, `members`
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`, nhóm bảng "Bán hàng và thanh toán")*
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `[...]` | `[...]` | `[...]` | `[...]` | Trạng thái hóa đơn |
  | `[...]` | `[...]` | `[...]` | `[...]` | Số tiền |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `GET /api/v1/me/invoices?page=0&size=20` *(đề xuất)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body:** Không có.

---

### 3.4. UC-F7.4: Gửi yêu cầu mua gói

- **Mã UC:** UC-F7.4
- **Tên Use Case:** Gửi yêu cầu mua gói
- **Tác nhân chính:** Hội viên
- **Tác nhân phụ (nếu có):** Hệ thống (tác vụ tự hủy hóa đơn chờ)
- **Mô tả ngắn gọn:** Hội viên chọn một gói để mua; hệ thống tạo hóa đơn chờ và gói ở trạng thái PENDING.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Hội viên và có hồ sơ hội viên.
- Gói được chọn đang được bán.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Một hóa đơn chờ và một gói ở trạng thái PENDING được tạo cho hội viên. Hóa đơn có hạn tự hủy theo `PENDING_INVOICE_TTL_DAYS`.
- **Thất bại:** Không tạo hóa đơn hay gói nào (rollback giao dịch).

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Hội viên chọn một gói trong danh sách và bấm "Mua".
2. Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào (mã gói bắt buộc).
3. Hệ thống kiểm tra quyền và vai trò (S3).
4. Hệ thống kiểm tra gói tồn tại và đang được bán; kiểm tra hội viên chưa có hóa đơn chờ cho gói này.
5. Hệ thống tính số tiền ở phía server (S7), tạo hóa đơn chờ và gói PENDING trong một giao dịch (`@Transactional`).
6. Hệ thống trả về kết quả (201 Created) kèm thông tin hóa đơn và hạn tự hủy.
7. Hệ thống hiển thị hướng dẫn hoàn tất thanh toán.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Hóa đơn chờ tự hủy:**
  - Khi hóa đơn chờ quá hạn `PENDING_INVOICE_TTL_DAYS` mà chưa thanh toán, hệ thống tự hủy hóa đơn và hủy luôn gói PENDING đi kèm.
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):**
  - Tại bước 2, thiếu hoặc sai mã gói; hệ thống trả `ProblemDetail` (400) kèm danh sách lỗi.
- **E2 – Không có quyền truy cập tài nguyên (403 Forbidden):**
  - Tại bước 3, người thao tác không phải Hội viên.
- **E3 – Không tìm thấy bản ghi dữ liệu (404 Not Found):**
  - Tại bước 4, mã gói không tồn tại; hệ thống trả `ProblemDetail` (404): *"Gói tập không tồn tại trên hệ thống."*
- **E4 – Xung đột dữ liệu / Trùng lặp (409 Conflict):**
  - Tại bước 4, hội viên đã có hóa đơn chờ cho gói này; hệ thống không tạo thêm và trả `ProblemDetail` (409).
- **E5 – Vi phạm quy tắc nghiệp vụ (422 Unprocessable Content):**
  - Tại bước 4, gói đã ngừng bán; hệ thống trả `ProblemDetail` (422): *"Gói này hiện không còn được bán."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F7.4:** Hóa đơn chờ tạo từ cổng hội viên tự hủy sau `PENDING_INVOICE_TTL_DAYS`; gói PENDING đi kèm cũng bị hủy.
- **CONFIG-F7.1:** Đọc từ bảng `system_configs` với key `PENDING_INVOICE_TTL_DAYS` (giá trị mặc định: `3`), không viết cứng trong mã nguồn (Quy tắc số 15).
- **[Yêu cầu phi chức năng]:**
  - Toàn vẹn đồng thời (NF6): hai yêu cầu cùng lúc cho cùng một gói không được tạo hai hóa đơn chờ.
  - Bảo mật (S2, S3, S7): kiểm tra JWT, phân quyền ở backend, tính tiền ở server.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `[bảng hóa đơn]`, `[bảng gói đã đăng ký của hội viên]`, `system_configs` (đọc)
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`)*
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `[...]` | `[...]` | `[...]` | `[...]` | Trạng thái hóa đơn (chờ / đã hủy) |
  | `[...]` | `[...]` | `[...]` | `[...]` | Trạng thái gói (PENDING) |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `POST /api/v1/me/package-requests` *(đề xuất)*
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
- **Tác nhân phụ (nếu có):** Hệ thống (tác vụ tự hủy hóa đơn chờ)
- **Mô tả ngắn gọn:** Hội viên gia hạn một gói đã có; hệ thống tạo hóa đơn chờ và gói ở trạng thái PENDING.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Hội viên.
- Hội viên đã từng đăng ký gói cần gia hạn; gói vẫn đang được bán.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Một hóa đơn chờ và một gói ở trạng thái PENDING được tạo, có hạn tự hủy theo `PENDING_INVOICE_TTL_DAYS`.
- **Thất bại:** Không tạo hóa đơn hay gói nào (rollback giao dịch).

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Hội viên mở "Gói của tôi", chọn gói cần gia hạn và bấm "Gia hạn".
2. Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào (mã gói bắt buộc).
3. Hệ thống kiểm tra quyền và vai trò (S3).
4. Hệ thống kiểm tra gói thuộc về hội viên, còn được bán, và chưa có hóa đơn chờ cho gói này.
5. Hệ thống tính số tiền ở phía server (S7), tạo hóa đơn chờ và gói PENDING trong một giao dịch (`@Transactional`).
6. Hệ thống trả về kết quả (201 Created) kèm thông tin hóa đơn và hạn tự hủy.
7. Hệ thống hiển thị hướng dẫn hoàn tất thanh toán.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Hóa đơn chờ tự hủy:**
  - Khi quá hạn `PENDING_INVOICE_TTL_DAYS` mà chưa thanh toán, hệ thống tự hủy hóa đơn và gói PENDING đi kèm.
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):**
  - Tại bước 2, thiếu hoặc sai mã gói; hệ thống trả `ProblemDetail` (400).
- **E2 – Không có quyền truy cập tài nguyên (403 Forbidden):**
  - Tại bước 3, người thao tác không phải Hội viên, hoặc gói không thuộc hội viên này.
- **E3 – Không tìm thấy bản ghi dữ liệu (404 Not Found):**
  - Tại bước 4, mã gói không tồn tại; hệ thống trả `ProblemDetail` (404).
- **E4 – Xung đột dữ liệu / Trùng lặp (409 Conflict):**
  - Tại bước 4, đã có hóa đơn chờ cho gói này; hệ thống không tạo thêm và trả `ProblemDetail` (409).
- **E5 – Vi phạm quy tắc nghiệp vụ (422 Unprocessable Content):**
  - Tại bước 4, gói đã ngừng bán; hệ thống trả `ProblemDetail` (422): *"Gói này hiện không còn được bán."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F7.4:** Hóa đơn chờ tạo từ cổng hội viên tự hủy sau `PENDING_INVOICE_TTL_DAYS`; gói PENDING đi kèm cũng bị hủy.
- **CONFIG-F7.1:** Đọc từ bảng `system_configs` với key `PENDING_INVOICE_TTL_DAYS` (giá trị mặc định: `3`).
- **[Yêu cầu phi chức năng]:**
  - Toàn vẹn đồng thời (NF6): không tạo hai hóa đơn chờ cho cùng một gói khi có hai yêu cầu đồng thời.
  - Bảo mật (S2, S3, S7): kiểm tra JWT, phân quyền ở backend, tính tiền ở server.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `[bảng hóa đơn]`, `[bảng gói đã đăng ký của hội viên]`, `system_configs` (đọc)
- **Chi tiết trường dữ liệu:** *(giống UC-F7.4, điền từ `erd-v2.dbml`)*

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `POST /api/v1/me/package-renewals` *(đề xuất)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body (DTO):**
  ```json
  {
    "memberPackageId": 1
  }
  ```
