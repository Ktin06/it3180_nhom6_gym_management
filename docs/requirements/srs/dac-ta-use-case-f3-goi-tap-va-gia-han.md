# Đặc tả Use Case – F3: Gói tập và gia hạn

- **Mã chức năng:** F3
- **Version:** v1 (Bắt buộc)
- **Người biên soạn:** Thái Hùng Lân
- **Người duyệt chéo:** Bùi Trần Đức Việt (theo phân công cặp duyệt mục 2.4 Sprint 1)
- **Cập nhật 18/11/2026 (Hoàng Kim Tiến):** viết lại mục H của UC-F3.1 đến UC-F3.5 theo cây truy vết ([ADR 0012](../../decisions/0012-quy-uoc-ma-truy-vet.md)): 6 user story, 43 AC có mã. Đây là **bản mẫu** cho các file SRS F1, F2, F4–F10 viết lại theo.
- **Tài liệu chuẩn tham chiếu:**
  - [Bản chốt phạm vi](../ban-chot-pham-vi.md)
  - [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml)
  - [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md)
  - [Quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md)
  - [ADR liên quan](../../decisions/)

---

## 1. Tổng quan chức năng

F3 quản lý **danh mục gói tập** và **vòng đời gói của hội viên** (đăng ký, gia hạn, kiểm tra trạng thái). Đây là chức năng trung tâm của v1, vì check-in (F4) và thanh toán (F5) đều phụ thuộc vào trạng thái gói tập.

**Các năng lực chính:**

| # | Năng lực | Người thực hiện |
|---|---|---|
| 1 | Xem danh mục gói tập đang bán | Lễ tân, Quản lý, Chủ chuỗi (API nội bộ); Khách xem bảng giá công khai (API công khai riêng, US-F3.1.2) |
| 2 | Thêm gói tập mới vào danh mục | Quản lý chi nhánh |
| 3 | Sửa thông tin gói tập | Quản lý chi nhánh |
| 4 | Ngừng bán gói tập | Quản lý chi nhánh |
| 5 | Bán gói tập tại quầy (đăng ký mới hoặc gia hạn) | Lễ tân |
| 6 | Xem gói tập của một hội viên | Lễ tân, Quản lý (xem trong hồ sơ hội viên – phạm vi F2/F4); Hội viên (chỉ xem gói của mình – phạm vi F7 cổng hội viên). F3 không có use case riêng cho năng lực này vì chỉ tập trung vào quản lý danh mục và bán gói. |

**Phạm vi Sprint 1:** Thực hiện UC-F3.1 đến UC-F3.5 (quản lý danh mục và bán gói tại quầy). Chức năng hội viên tự mua/gia hạn từ cổng hội viên (tạo hóa đơn PENDING) thực hiện ở F7 – Sprint 2.

> **Lưu ý phạm vi:** Bảng giá công khai cho Khách (không cần đăng nhập) thuộc UC-F3.1, user story US-F3.1.2. Khách dùng API riêng `GET /api/v1/public/packages`, chỉ trả gói đang bán và các trường cần hiển thị. API `GET /api/v1/packages` vẫn yêu cầu đăng nhập và chỉ phục vụ các vai trò nội bộ. Hai API dùng cùng bảng `packages` nhưng khác đường dẫn và bảo mật.

**Ngoài phạm vi:** Thanh toán online; hội viên xác nhận đã nhận tiền (chỉ lễ tân/quản lý được làm – S7); bảo lưu và chuyển nhượng gói (F15 – v3).

---

## 2. Danh sách Use Case

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F3.1** | Xem danh mục gói tập | Lễ tân, Quản lý chi nhánh, Chủ chuỗi; Khách (bảng giá công khai) | Cao | Sprint 1; bảng giá công khai Sprint 2 |
| **UC-F3.2** | Thêm gói tập mới | Quản lý chi nhánh | Cao | Sprint 1 |
| **UC-F3.3** | Sửa thông tin gói tập | Quản lý chi nhánh | Trung bình | Sprint 1 |
| **UC-F3.4** | Ngừng bán gói tập | Quản lý chi nhánh | Trung bình | Sprint 1 |
| **UC-F3.5** | Bán gói tập tại quầy (đăng ký / gia hạn) | Lễ tân | Cao | Sprint 1 |

---

## 3. Đặc tả chi tiết từng Use Case

---

### 3.1. UC-F3.1: Xem danh mục gói tập

- **Mã UC:** UC-F3.1
- **Tên Use Case:** Xem danh mục gói tập
- **Tác nhân chính:** Lễ tân, Quản lý chi nhánh, Chủ chuỗi; Khách (chỉ bảng giá công khai, US-F3.1.2)
- **Tác nhân phụ:** Không có
- **Mô tả ngắn gọn:** Nhân viên xem danh sách gói tập đang bán (ACTIVE) hoặc ngừng bán (INACTIVE), có thể lọc và phân trang. Khách xem bảng giá các gói đang bán mà không cần đăng nhập.

#### A. Tiền điều kiện (Preconditions)
- **Danh mục nội bộ (`GET /api/v1/packages`):** người dùng đã đăng nhập với vai trò Lễ tân, Quản lý chi nhánh hoặc Chủ chuỗi (JWT hợp lệ – S2).
- **Bảng giá công khai (`GET /api/v1/public/packages`, US-F3.1.2):** không cần đăng nhập. Nếu request có gửi kèm header `Authorization` (kể cả token hết hạn), backend bỏ qua header đó.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Danh sách gói tập được trả về; không có thay đổi dữ liệu nào trong CSDL.
- **Thất bại:** Không áp dụng (thao tác chỉ đọc).

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Người dùng truy cập màn hình danh mục gói tập, có thể lọc theo `type` (TIME / SESSION / PT) và `status` (ACTIVE / INACTIVE), và phân trang.
2. Hệ thống kiểm tra JWT hợp lệ (S2) và vai trò của người dùng (S3).
3. Hệ thống truy vấn bảng `packages`, tự động lọc theo `branch_id` của chi nhánh người dùng (đối với Lễ tân và Quản lý chi nhánh) hoặc trả toàn bộ (Chủ chuỗi). Gói toàn chuỗi (`branch_id` rỗng) luôn được bao gồm.
4. Hệ thống trả về `200 OK` kèm `PageResponse<PackageSummaryDto>` đã sắp xếp theo `type`, sau đó `name`.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **E1 – Token không hợp lệ hoặc hết hạn (401 Unauthorized):**
  - Tại bước 2, JWT hết hạn hoặc không hợp lệ.
  - Hệ thống trả `ProblemDetail` (401): *"Bạn cần đăng nhập để truy cập tài nguyên này."*
- **E2 – Không có quyền truy cập (403 Forbidden):**
  - Tại bước 2, vai trò của người dùng không được phép (ví dụ Huấn luyện viên, Hội viên gọi endpoint này).
  - Hệ thống trả `ProblemDetail` (403): *"Bạn không có quyền thực hiện thao tác này."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F3-01:** Gói toàn chuỗi có `packages.branch_id` rỗng; lễ tân xem được gói toàn chuỗi và gói của chi nhánh mình. ([Bản chốt phạm vi – Gói tập](../ban-chot-pham-vi.md))
- **Hiệu năng (NF1):** Tải danh sách phản hồi dưới 2 giây với số lượng gói bình thường (< 200 gói).
- **Bảo mật (S3):** Lễ tân và Quản lý chi nhánh chỉ xem được gói thuộc chi nhánh của mình hoặc gói toàn chuỗi; backend tự lọc, không tin tham số `branch_id` từ client.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `packages`
- **Chi tiết trường dữ liệu:**

  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `packages` | `id` | `bigint` | PK, Generated Always | Khóa chính tự tăng |
  | `packages` | `branch_id` | `bigint` | FK → `branches.id`, nullable | Rỗng = gói toàn chuỗi |
  | `packages` | `code` | `varchar(30)` | UNIQUE, NOT NULL | Mã gói dùng nội bộ |
  | `packages` | `name` | `varchar(255)` | NOT NULL | Tên gói hiển thị cho hội viên |
  | `packages` | `type` | `varchar(10)` | NOT NULL, CHECK IN ('TIME','SESSION','PT') | Loại gói |
  | `packages` | `duration_days` | `int` | NOT NULL, CHECK > 0 | Số ngày hiệu lực; 1 tháng = 30 ngày |
  | `packages` | `session_count` | `int` | nullable; NOT NULL với SESSION và PT | Tổng số buổi (rỗng với TIME) |
  | `packages` | `price` | `int` | NOT NULL, CHECK >= 0 | Giá bán (VND, số nguyên) |
  | `packages` | `description` | `text` | nullable | Mô tả chi tiết quyền lợi gói tập |
  | `packages` | `status` | `varchar(10)` | NOT NULL, CHECK IN ('ACTIVE','INACTIVE'), default 'ACTIVE' | ACTIVE = đang bán; INACTIVE = ngừng bán |
  | `packages` | `created_at` | `timestamptz` | NOT NULL, default now() | Thời gian tạo (UTC) |
  | `packages` | `updated_at` | `timestamptz` | NOT NULL | Thời gian cập nhật (UTC) |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `GET /api/v1/packages`
- **Header:** `Authorization: Bearer <access_token>`
- **Query Parameters:**
  ```
  type=TIME|SESSION|PT   (tùy chọn)
  status=ACTIVE|INACTIVE (tùy chọn, mặc định ACTIVE)
  page=0&size=20         (phân trang)
  ```
- **Response Success (200 OK):**
  ```json
  {
    "content": [
      {
        "id": 1,
        "code": "TIME-1M",
        "name": "Gói 1 tháng",
        "type": "TIME",
        "durationDays": 30,
        "sessionCount": null,
        "price": 500000,
        "description": "Tập luyện không giới hạn trong 30 ngày",
        "status": "ACTIVE",
        "branchId": null
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
  }
  ```
- **Màn hình giao diện liên quan:** Màn hình "Danh mục gói tập" – `docs/design/wireframe/f3-danh-muc-goi-tap.drawio.svg`.

**Bảng giá công khai (US-F3.1.2):**

- **API Endpoint:** `GET /api/v1/public/packages`. Không cần đăng nhập, không nhận tham số. Backend bỏ qua header `Authorization` ở mọi request GET tới `/api/v1/public/**`, nên token hết hạn gửi kèm cũng không làm request bị 401. Frontend gọi API này không gửi header đó.
- **Phạm vi dữ liệu:** gói `ACTIVE` của chi nhánh mặc định `CN01` và gói toàn chuỗi (`branch_id` rỗng). Nếu `CN01` đang ngừng hoạt động thì trả danh sách rỗng `[]`. Từ v1 đến v3 hệ thống chạy một chi nhánh mặc định ([bản chốt phạm vi](../ban-chot-pham-vi.md)). Trả cả danh sách, không phân trang, sắp theo `type` (TIME, SESSION, PT), rồi `price`, rồi `name`.
- **Response Success (200 OK):** chỉ có các trường cần hiển thị, không có `code`, `status`, `branchId`, thời gian tạo hay người tạo.
  ```json
  [
    {
      "id": 1,
      "name": "Gói 1 tháng",
      "type": "TIME",
      "durationDays": 30,
      "sessionCount": null,
      "price": 500000,
      "description": "Tập luyện không giới hạn trong 30 ngày"
    }
  ]
  ```
- **Màn hình giao diện liên quan:** trang `/bang-gia` – [`bang-gia-cong-khai.drawio.svg`](../../design/wireframe/bang-gia-cong-khai.drawio.svg).

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F3.1.1: Xem danh mục gói (nội bộ)
- **User story:** Là **Lễ tân**, tôi muốn **xem gói đang bán và đã ngừng bán, có lọc**, để **tư vấn và bán đúng gói**.
- **BR liên quan:** BR-F3-01; S3 (lọc chi nhánh ở backend)
- **Version / Sprint:** v1 / API Sprint 1, giao diện Sprint 2
- **Issue:** #129
- **Tiêu chí chấp nhận:**
  - **AC-F3.1.1-01: Nhân viên chi nhánh chỉ thấy gói của chi nhánh mình và gói toàn chuỗi** (BR-F3-01)
    - **Given** Lễ tân hoặc Quản lý chi nhánh của `CN01` đã đăng nhập; có gói của `CN01`, gói toàn chuỗi và gói của chi nhánh khác
    - **When** Gọi `GET /api/v1/packages`, kể cả khi tự gửi thêm tham số `branchId` của chi nhánh khác
    - **Then** `200 OK`; danh sách và `totalElements` chỉ gồm gói của `CN01` và gói toàn chuỗi; tham số `branchId` bị bỏ qua
  - **AC-F3.1.1-02: Lọc, sắp xếp và phân trang**
    - **Given** Nhân viên được phép đã đăng nhập
    - **When** Gọi danh mục không kèm tham số, hoặc lọc theo `type`, `status`, `page`, `size`
    - **Then** Mặc định chỉ trả gói `ACTIVE`, `page=0`, `size=20`; sắp theo `type` rồi `name`; lọc `status=INACTIVE` thấy gói đã ngừng bán; `size` từ 1 đến 100; trang vượt quá số trang trả danh sách rỗng, không lỗi
  - **AC-F3.1.1-03: Lễ tân chỉ xem, không có thao tác quản lý trên giao diện**
    - **Given** Lễ tân đã đăng nhập
    - **When** Mở màn hình "Danh mục gói tập"
    - **Then** Không thấy nút Thêm, Sửa, Ngừng bán; Quản lý chi nhánh mở cùng màn hình thì thấy các nút này (phân quyền thật vẫn ở backend, xem AC-F3.2.1-06, AC-F3.3.1-08, AC-F3.4.1-06)
  - **AC-F3.1.1-04: Chủ chuỗi xem gói mọi chi nhánh**
    - **Given** Chủ chuỗi đã đăng nhập, không gắn với chi nhánh nào
    - **When** Gọi `GET /api/v1/packages`
    - **Then** `200 OK`, thấy gói của mọi chi nhánh và gói toàn chuỗi
  - **AC-F3.1.1-05: Từ chối người không được xem**
    - **Given** Người gọi là Huấn luyện viên hoặc Hội viên; hoặc là nhân viên chưa được gán chi nhánh, hoặc hồ sơ nhân viên đã ngừng hoạt động; hoặc không gửi token
    - **When** Gọi `GET /api/v1/packages`
    - **Then** Không gửi token hoặc token sai trả `401`. Huấn luyện viên, Hội viên trả `403` *"Bạn không có quyền thực hiện thao tác này."*; nhân viên chưa có chi nhánh hoặc đã ngừng hoạt động trả `403` *"Nhân viên chưa có chi nhánh làm việc hợp lệ."*
  - **AC-F3.1.1-06: Tham số lọc sai** (S5)
    - **Given** Nhân viên được phép đã đăng nhập
    - **When** Gửi `type` không thuộc TIME/SESSION/PT, `status` lạ, `page < 0` hoặc `size` ngoài 1–100
    - **Then** `400 Bad Request` kèm lỗi theo từng tham số

##### US-F3.1.2: Bảng giá công khai
- **User story:** Là **Khách**, tôi muốn **xem bảng giá các gói đang bán mà không cần đăng nhập**, để **chọn gói trước khi đến phòng tập**.
- **BR liên quan:** BR-F3-01; mọi thanh toán tại quầy, không có thanh toán online ([ADR 0004](../../decisions/0004-thanh-toan-chi-tai-quay.md)).
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #130
- **Tiêu chí chấp nhận:**
  - **AC-F3.1.2-01: Khách xem bảng giá không cần đăng nhập** (BR-F3-01)
    - **Given** Chi nhánh `CN01` có gói ACTIVE, và có gói toàn chuỗi ACTIVE
    - **When** Khách chưa đăng nhập mở trang `/bang-gia` (gọi `GET /api/v1/public/packages` không kèm `Authorization`)
    - **Then** Hệ thống trả `200 OK`; trang hiện từng gói với loại, thời hạn (ngày) hoặc số buổi, giá VND đúng giá trong CSDL, quyền lợi theo loại gói, và dòng "Thanh toán tại quầy". Không có giỏ hàng hay nút thanh toán online.
  - **AC-F3.1.2-02: Không hiện gói ngừng bán và gói chi nhánh khác** (BR-F3-01)
    - **Given** Có gói INACTIVE của `CN01`, và gói ACTIVE của một chi nhánh khác (không phải `CN01`)
    - **When** Khách gọi `GET /api/v1/public/packages`
    - **Then** Kết quả không chứa hai gói đó
  - **AC-F3.1.2-03: Không lộ trường nội bộ** (S3)
    - **Given** Có gói ACTIVE
    - **When** Khách gọi `GET /api/v1/public/packages`
    - **Then** Mỗi phần tử chỉ có `id`, `name`, `type`, `durationDays`, `sessionCount`, `price`, `description`; không có `code`, `status`, `branchId`, thời gian tạo, người tạo
  - **AC-F3.1.2-04: Chưa có gói đang bán**
    - **Given** API trả danh sách rỗng
    - **When** Khách mở `/bang-gia`
    - **Then** Trang hiện "Chưa có gói đang bán" và nút đăng ký tài khoản; không hiện giá nào
  - **AC-F3.1.2-05: Lỗi tải bảng giá**
    - **Given** API lỗi mạng hoặc lỗi server
    - **When** Khách mở `/bang-gia`
    - **Then** Trang hiện thông báo lỗi và nút "Thử lại"; không hiện giá bịa hoặc giá cũ chưa xác minh. Bấm "Thử lại" thì gọi lại API
  - **AC-F3.1.2-06: Chi nhánh mặc định ngừng hoạt động**
    - **Given** Chi nhánh `CN01` có trạng thái INACTIVE, dù vẫn còn gói ACTIVE của `CN01` hoặc gói toàn chuỗi
    - **When** Khách gọi `GET /api/v1/public/packages`
    - **Then** Hệ thống trả `200 OK` với danh sách rỗng `[]`, không trả lỗi và không lộ thông báo nội bộ; trang hiện "Chưa có gói đang bán" như AC-F3.1.2-04

---

### 3.2. UC-F3.2: Thêm gói tập mới

- **Mã UC:** UC-F3.2
- **Tên Use Case:** Thêm gói tập mới vào danh mục
- **Tác nhân chính:** Quản lý chi nhánh
- **Tác nhân phụ:** Không có
- **Mô tả ngắn gọn:** Quản lý tạo một gói tập mới (TIME, SESSION hoặc PT) với giá và thời hạn cụ thể, thêm vào danh mục để lễ tân có thể bán.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Quản lý chi nhánh (JWT hợp lệ – S2).

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Một bản ghi mới được thêm vào bảng `packages` với `status = ACTIVE`; gói mới lập tức xuất hiện trong danh mục cho lễ tân.
- **Thất bại:** Không có dữ liệu rác; giao dịch rollback hoàn toàn.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Quản lý điền form tạo gói mới: `code`, `name`, `type`, `duration_days`, `session_count` (nếu SESSION hoặc PT), `price`, `description` và gửi request.
2. Hệ thống kiểm tra tính hợp lệ đầu vào (Bean Validation – S5):
   - `code`, `name`, `type`, `duration_days`, `price` bắt buộc không rỗng.
   - `type` phải là một trong `TIME`, `SESSION`, `PT`.
   - `duration_days > 0`; `price >= 0`.
   - Nếu `type` là `SESSION` hoặc `PT` thì `session_count` bắt buộc và `> 0`.
   - Nếu `type` là `TIME` thì `session_count` phải rỗng.
3. Hệ thống kiểm tra vai trò Quản lý chi nhánh (S3). Gói được gán `branch_id` của chi nhánh quản lý; request có gửi `branchId` (hoặc `status`, `id`, trường lạ) bị từ chối 400 (AC-F3.2.1-04).
4. Hệ thống kiểm tra `code` chưa tồn tại trong bảng `packages` (UNIQUE).
5. Hệ thống tạo bản ghi `packages` trong giao dịch `@Transactional` với `status = ACTIVE`.
6. Hệ thống trả `201 Created` kèm `PackageDetailDto` của gói vừa tạo.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):**
  - Tại bước 2, vi phạm Bean Validation (thiếu trường, sai kiểu, `price < 0`, `duration_days <= 0`, `session_count` rỗng khi `type = SESSION`...).
  - Hệ thống trả `ProblemDetail` (400) chứa danh sách lỗi chi tiết theo từng trường (`errors`).
- **E2 – Không có quyền (403 Forbidden):**
  - Tại bước 3, người dùng không phải Quản lý chi nhánh.
  - Hệ thống trả `ProblemDetail` (403): *"Bạn không có quyền thực hiện thao tác này."*
- **E3 – Mã gói đã tồn tại (409 Conflict):**
  - Tại bước 4, `code` đã có trong bảng `packages`.
  - Hệ thống trả `ProblemDetail` (409): *"Mã gói '{code}' đã tồn tại trên hệ thống."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F3-02:** Ba loại gói: TIME (theo thời gian), SESSION (theo buổi), PT (buổi tập 1-1). Gói TIME không có `session_count`; SESSION và PT bắt buộc có `session_count`. ([Bản chốt phạm vi – Gói tập](../ban-chot-pham-vi.md))
- **BR-F3-03:** Gói TIME "1 tháng" = 30 ngày; quản lý nhập số ngày trực tiếp (`duration_days`). ([Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md))
- **BR-F3-04:** Giá gói tập được thiết lập bởi Quản lý chi nhánh khi tạo hoặc cập nhật danh mục gói. Khi bán gói (UC-F3.5), hệ thống phải lấy giá niêm yết từ `packages.price` trong cơ sở dữ liệu và tự tính tổng tiền hóa đơn ở server; không sử dụng giá hoặc tổng tiền do client gửi lên (S7). ([Bản chốt phạm vi – Thanh toán](../ban-chot-pham-vi.md))
- **Bảo mật (S3):** `branch_id` của gói luôn lấy từ `staff.branch_id` của quản lý đang đăng nhập, không lấy từ request body.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `packages`
- **Chi tiết trường dữ liệu:** (xem mục 3.1.F – cùng bảng `packages`)

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `POST /api/v1/packages`
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body (DTO):**
  ```json
  {
    "code": "PT-10B",
    "name": "Gói PT 10 buổi",
    "type": "PT",
    "durationDays": 90,
    "sessionCount": 10,
    "price": 3000000,
    "description": "Gói tập cá nhân 10 buổi với huấn luyện viên"
  }
  ```
- **Response Success (201 Created):**
  ```json
  {
    "id": 5,
    "code": "PT-10B",
    "name": "Gói PT 10 buổi",
    "type": "PT",
    "durationDays": 90,
    "sessionCount": 10,
    "price": 3000000,
    "description": "Gói tập cá nhân 10 buổi với huấn luyện viên",
    "status": "ACTIVE",
    "branchId": 1,
    "createdAt": "2026-11-03T08:00:00Z"
  }
  ```
- **Màn hình giao diện liên quan:** Màn hình "Thêm gói tập" – `docs/design/wireframe/f3-danh-muc-goi-tap.drawio.svg`.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F3.2.1: Thêm gói tập
- **User story:** Là **Quản lý chi nhánh**, tôi muốn **thêm gói TIME, SESSION hoặc PT**, để **lễ tân có gói mới để bán**.
- **BR liên quan:** BR-F3-02, BR-F3-03, BR-F3-04; S3, S5
- **Version / Sprint:** v1 / API Sprint 1, giao diện Sprint 2
- **Issue:** #131
- **Tiêu chí chấp nhận:**
  - **AC-F3.2.1-01: Thêm gói hợp lệ của từng loại** (BR-F3-02, BR-F3-03)
    - **Given** Quản lý chi nhánh `CN01` đã đăng nhập
    - **When** Gọi `POST /api/v1/packages` với gói TIME (`durationDays=30`, không có `sessionCount`), gói SESSION hoặc gói PT (có `sessionCount > 0`), giá là số nguyên VND
    - **Then** `201 Created` kèm header `Location`; gói có `status=ACTIVE`, `branchId` là chi nhánh của quản lý; gói TIME có `sessionCount` rỗng; lễ tân thấy gói mới ngay trong danh mục
  - **AC-F3.2.1-02: Số buổi đúng theo loại gói** (BR-F3-02)
    - **Given** Quản lý chi nhánh đã đăng nhập
    - **When** Thêm gói SESSION hoặc PT thiếu `sessionCount` hoặc `sessionCount <= 0`; hoặc thêm gói TIME có `sessionCount`
    - **Then** `400 Bad Request` với lỗi ở trường `sessionCount`; không có gói nào được tạo
  - **AC-F3.2.1-03: Dữ liệu sai bị từ chối, giá trị biên được nhận** (S5)
    - **Given** Quản lý chi nhánh đã đăng nhập
    - **When** Thiếu `code`, `name`, `type`, `durationDays` hoặc `price`; `durationDays <= 0`; `price < 0`; `type` lạ; chuỗi quá độ dài cho phép; JSON sai cú pháp
    - **Then** `400 Bad Request` kèm lỗi từng trường, không ghi gì vào CSDL. Ngược lại `price = 0` và chuỗi đúng độ dài tối đa (`code` 30, `name` 255 ký tự) vẫn tạo được
  - **AC-F3.2.1-04: Không nhận trường do server quản lý** (S3)
    - **Given** Quản lý chi nhánh đã đăng nhập
    - **When** Request có thêm `branchId`, `status`, `id` hoặc một trường lạ
    - **Then** `400 Bad Request` kèm lỗi ở đúng trường không được phép; không có gói nào được tạo
  - **AC-F3.2.1-05: Mã gói không trùng**
    - **Given** Đã có gói mã `TIME-1M`, kể cả khi gói đó đã ngừng bán hoặc là gói toàn chuỗi
    - **When** Thêm gói mới cũng mã `TIME-1M`; hoặc hai quản lý gửi cùng một mã mới gần như đồng thời
    - **Then** `409 Conflict` *"Mã gói 'TIME-1M' đã tồn tại trên hệ thống."*; khi gửi đồng thời thì đúng một request thành công, request còn lại nhận `409`
  - **AC-F3.2.1-06: Chỉ Quản lý chi nhánh được thêm gói** (S3)
    - **Given** Người gọi là Lễ tân, Chủ chuỗi, Huấn luyện viên hoặc Hội viên; hoặc là quản lý chưa được gán chi nhánh, hoặc hồ sơ nhân viên đã ngừng hoạt động; hoặc không gửi token
    - **When** Gọi `POST /api/v1/packages`
    - **Then** Không có token trả `401`; các trường hợp còn lại trả `403`; không có gói nào được tạo

---

### 3.3. UC-F3.3: Sửa thông tin gói tập

- **Mã UC:** UC-F3.3
- **Tên Use Case:** Sửa thông tin gói tập
- **Tác nhân chính:** Quản lý chi nhánh
- **Tác nhân phụ:** Không có
- **Mô tả ngắn gọn:** Quản lý cập nhật tên, giá, mô tả hoặc số ngày của gói tập. Không thay đổi `type` và `code` sau khi đã tạo.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Quản lý chi nhánh (JWT hợp lệ – S2).
- Gói tập cần sửa tồn tại trong CSDL và thuộc chi nhánh của quản lý.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Bản ghi `packages` được cập nhật với thông tin mới; các `memberships` đã bán trước đó **không bị ảnh hưởng** (giá lịch sử lưu ở `invoice_items.unit_price`, số buổi lưu ở `memberships.total_sessions`, và thời hạn hiệu lực được bảo toàn qua khoảng ngày `end_date - start_date + 1` của bản ghi `memberships`).
- **Thất bại:** Bản ghi giữ nguyên giá trị cũ; giao dịch rollback.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Quản lý chọn gói cần sửa và gửi request với các trường muốn cập nhật: `name`, `price`, `duration_days`, `session_count`, `description`.
2. Hệ thống kiểm tra tính hợp lệ đầu vào (Bean Validation – S5): các ràng buộc tương tự UC-F3.2 bước 2; trường nào không gửi thì giữ nguyên.
3. Hệ thống kiểm tra vai trò Quản lý chi nhánh (S3) và xác nhận gói `id` thuộc chi nhánh quản lý (không cho sửa gói chi nhánh khác và gói toàn chuỗi).
4. Hệ thống tìm gói theo `id` trong bảng `packages`.
5. Hệ thống cập nhật bản ghi trong giao dịch `@Transactional`.
6. Hệ thống trả `200 OK` kèm `PackageDetailDto` sau khi cập nhật.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):**
  - Tại bước 2, ràng buộc bị vi phạm (ví dụ `price < 0`, `duration_days <= 0`).
  - Hệ thống trả `ProblemDetail` (400) với chi tiết lỗi từng trường.
- **E2 – Không có quyền (403 Forbidden):**
  - Tại bước 3, người dùng không phải Quản lý chi nhánh, gói không thuộc chi nhánh của họ, hoặc gói là gói toàn chuỗi (`branch_id` rỗng; xem BR-F3-07).
  - Hệ thống trả `ProblemDetail` (403): *"Bạn không có quyền thực hiện thao tác này."*
- **E3 – Không tìm thấy gói (404 Not Found):**
  - Tại bước 4, không có bản ghi `packages` với `id` tương ứng.
  - Hệ thống trả `ProblemDetail` (404): *"Gói tập không tồn tại trên hệ thống."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F3-05:** Không cho sửa `type` và `code` của gói sau khi đã tạo, vì `memberships.package_type` đã chép giá trị này lúc bán.
- **BR-F3-06:** Thay đổi giá và thông số gói (giá, thời hạn, số buổi) chỉ áp dụng cho lần bán mới; hóa đơn và gói đã tạo trước đó (kể cả đang ở trạng thái `PENDING` chờ thanh toán) không bị ảnh hưởng. Giá đã bán được bảo toàn trong `invoice_items.unit_price`, số buổi bảo toàn trong `memberships.total_sessions`, và thời hạn gói được bảo toàn qua khoảng ngày `memberships.end_date - memberships.start_date + 1` chốt lúc tạo. ([Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md))
- **BR-F3-07:** Quản lý chi nhánh chỉ thêm, sửa, ngừng bán gói của chi nhánh mình; mọi thao tác trên gói toàn chuỗi (`branch_id` rỗng) hoặc gói chi nhánh khác bị từ chối 403. Từ v1 đến v3, gói toàn chuỗi chỉ có từ dữ liệu mẫu, lễ tân bán được nhưng không ai sửa qua API; Chủ chuỗi tạo và sửa gói toàn chuỗi ở F18 (v4, đa chi nhánh – "gói dùng một hoặc toàn chuỗi" trong [bản chốt phạm vi](../ban-chot-pham-vi.md)).

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `packages`
- **Chi tiết trường dữ liệu:** (xem mục 3.1.F)

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `PUT /api/v1/packages/{id}`
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body (DTO):**
  ```json
  {
    "name": "Gói 1 tháng – Ưu đãi",
    "price": 450000,
    "durationDays": 30,
    "description": "Giá ưu đãi tháng 11"
  }
  ```
- **Response Success (200 OK):**
  ```json
  {
    "id": 1,
    "code": "TIME-1M",
    "name": "Gói 1 tháng – Ưu đãi",
    "type": "TIME",
    "durationDays": 30,
    "sessionCount": null,
    "price": 450000,
    "description": "Giá ưu đãi tháng 11",
    "status": "ACTIVE",
    "branchId": 1,
    "updatedAt": "2026-11-03T09:00:00Z"
  }
  ```
- **Màn hình giao diện liên quan:** Màn hình "Sửa gói tập" – `docs/design/wireframe/f3-danh-muc-goi-tap.drawio.svg`.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F3.3.1: Sửa thông tin gói tập
- **User story:** Là **Quản lý chi nhánh**, tôi muốn **sửa tên, giá, thời hạn, số buổi của gói**, để **điều chỉnh bảng giá mà không ảnh hưởng gói đã bán**.
- **BR liên quan:** BR-F3-05, BR-F3-06, BR-F3-07; S3, S5
- **Version / Sprint:** v1 / API Sprint 1, giao diện Sprint 2
- **Issue:** #132
- **Tiêu chí chấp nhận:**
  - **AC-F3.3.1-01: Sửa giá; gói đã bán không đổi** (BR-F3-06)
    - **Given** Quản lý chi nhánh `CN01` đã đăng nhập; gói `id=1` thuộc `CN01` đã được bán, có hóa đơn đã thanh toán và có một hóa đơn còn `PENDING`
    - **When** Gọi `PUT /api/v1/packages/1` chỉ với `price=450000`
    - **Then** `200 OK`; `price=450000`, các trường không gửi giữ nguyên, `updated_at` thay đổi; `invoice_items.unit_price`, `memberships.total_sessions` và khoảng ngày `end_date - start_date + 1` của mọi gói đã bán (kể cả gói `PENDING`) không đổi
  - **AC-F3.3.1-02: Sửa gói chi nhánh khác hoặc gói toàn chuỗi bị từ chối 403** (BR-F3-07)
    - **Given** Quản lý chi nhánh `CN01` đã đăng nhập
    - **When** Sửa gói thuộc chi nhánh khác, hoặc gói toàn chuỗi (`branch_id` rỗng), dù gói đang bán hay đã ngừng bán
    - **Then** `403 Forbidden` *"Bạn không có quyền thực hiện thao tác này."*; dữ liệu gói giữ nguyên
  - **AC-F3.3.1-03: Xóa mô tả và gửi body rỗng**
    - **Given** Quản lý sửa gói của chi nhánh mình
    - **When** Gửi `description: null`; hoặc gửi body rỗng `{}`
    - **Then** Gửi `null` thì mô tả bị xóa; gửi body rỗng thì không trường nào đổi, kể cả `updated_at`
  - **AC-F3.3.1-04: Không đổi mã, loại và không xóa trường bắt buộc** (BR-F3-05, BR-F3-02)
    - **Given** Quản lý sửa gói của chi nhánh mình
    - **When** Gửi `code`, `type` hoặc trường lạ; hoặc gửi `null` cho `name`, `price`, `durationDays`; hoặc xóa `sessionCount` của gói SESSION hay PT
    - **Then** `400 Bad Request` kèm lỗi theo trường; dữ liệu gói giữ nguyên
  - **AC-F3.3.1-05: Dữ liệu sai thì không đổi gì** (S5)
    - **Given** Quản lý sửa gói của chi nhánh mình
    - **When** Gửi một trường hợp lệ cùng một trường sai (ví dụ `name` mới và `price < 0`); hoặc `id` không phải số nguyên dương; hoặc JSON sai cú pháp
    - **Then** `400 Bad Request`; không trường nào bị đổi, kể cả trường hợp lệ
  - **AC-F3.3.1-06: Sửa gói đã ngừng bán không làm gói bán lại**
    - **Given** Gói của chi nhánh đang `INACTIVE`
    - **When** Quản lý sửa giá hoặc tên gói đó
    - **Then** `200 OK`; thông tin được cập nhật; `status` vẫn là `INACTIVE`
  - **AC-F3.3.1-07: Gói không tồn tại**
    - **Given** Không có gói với `id` được gửi
    - **When** Gọi `PUT /api/v1/packages/{id}`
    - **Then** `404 Not Found` *"Gói tập không tồn tại trên hệ thống."*
  - **AC-F3.3.1-08: Chỉ Quản lý chi nhánh được sửa** (S3)
    - **Given** Người gọi là Lễ tân, Chủ chuỗi, Huấn luyện viên hoặc Hội viên; hoặc là quản lý chưa được gán chi nhánh, hoặc hồ sơ nhân viên đã ngừng hoạt động; hoặc không gửi token
    - **When** Gọi `PUT /api/v1/packages/{id}`
    - **Then** Không có token trả `401`; các trường hợp còn lại trả `403`; dữ liệu gói giữ nguyên

---

### 3.4. UC-F3.4: Ngừng bán gói tập

- **Mã UC:** UC-F3.4
- **Tên Use Case:** Ngừng bán gói tập
- **Tác nhân chính:** Quản lý chi nhánh
- **Tác nhân phụ:** Không có
- **Mô tả ngắn gọn:** Quản lý đánh dấu gói tập là INACTIVE để lễ tân không còn bán gói đó cho hội viên mới. Gói đã mua trước đó vẫn hoạt động bình thường.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Quản lý chi nhánh (JWT hợp lệ – S2).
- Gói tập cần ngừng bán có `status = ACTIVE` và thuộc chi nhánh của quản lý.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Bảng `packages` cập nhật `status = INACTIVE` cho gói tương ứng. Các `memberships` đã bán giữ nguyên trạng thái và thời hạn; gói INACTIVE không xuất hiện trong danh mục khi lễ tân chọn gói để bán.
- **Thất bại:** Trạng thái gói giữ nguyên; giao dịch rollback.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Quản lý chọn gói muốn ngừng bán và xác nhận hành động.
2. Hệ thống kiểm tra JWT hợp lệ và vai trò Quản lý chi nhánh (S2, S3).
3. Hệ thống tìm bản ghi `packages` theo `id`.
4. Hệ thống kiểm tra gói thuộc chi nhánh của quản lý và có `status = ACTIVE`.
5. Hệ thống cập nhật `status = INACTIVE` trong giao dịch `@Transactional`.
6. Hệ thống trả `200 OK` kèm thông tin gói đã cập nhật.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **E2 – Không có quyền (403 Forbidden):**
  - Tại bước 2 hoặc 4, người dùng không đúng vai trò hoặc gói không thuộc chi nhánh của họ.
  - Hệ thống trả `ProblemDetail` (403): *"Bạn không có quyền thực hiện thao tác này."*
- **E3 – Không tìm thấy gói (404 Not Found):**
  - Tại bước 3, không có `packages` với `id` tương ứng.
  - Hệ thống trả `ProblemDetail` (404): *"Gói tập không tồn tại trên hệ thống."*
- **E5 – Gói đã ngừng bán (422 Unprocessable Content):**
  - Tại bước 4, gói đã có `status = INACTIVE`.
  - Hệ thống trả `ProblemDetail` (422): *"Gói tập này đã ở trạng thái ngừng bán."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F3-08:** Ngừng bán là xóa mềm (`status = INACTIVE`); không xóa bản ghi vật lý để bảo toàn lịch sử `memberships` và `invoice_items`. ([Thiết kế dữ liệu – Nguyên tắc thiết kế](../../design/erd/thiet-ke-du-lieu.md))
- **BR-F3-09:** Gói INACTIVE không được chọn khi bán mới (UC-F3.5) nhưng vẫn xuất hiện trong màn hình quản lý danh mục khi lọc `status=INACTIVE`.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `packages`
- **Chi tiết trường dữ liệu:** (xem mục 3.1.F)

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `PUT /api/v1/packages/{id}/deactivate`
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body:** Không cần. Gửi `{}` cũng được; nội dung body không được dùng.
- **Response Success (200 OK):**
  ```json
  {
    "id": 1,
    "code": "TIME-1M",
    "status": "INACTIVE",
    "updatedAt": "2026-11-03T10:00:00Z"
  }
  ```
- **Màn hình giao diện liên quan:** Màn hình "Danh mục gói tập" (nút "Ngừng bán") – `docs/design/wireframe/f3-danh-muc-goi-tap.drawio.svg`.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F3.4.1: Ngừng bán gói tập
- **User story:** Là **Quản lý chi nhánh**, tôi muốn **ngừng bán một gói**, để **lễ tân không bán nữa nhưng gói đã bán vẫn dùng được**.
- **BR liên quan:** BR-F3-07, BR-F3-08, BR-F3-09; S3
- **Version / Sprint:** v1 / API Sprint 1, giao diện Sprint 2
- **Issue:** #133
- **Tiêu chí chấp nhận:**
  - **AC-F3.4.1-01: Ngừng bán gói đang bán** (BR-F3-08)
    - **Given** Quản lý chi nhánh `CN01` đã đăng nhập; gói `id=1` thuộc `CN01` đang `ACTIVE`
    - **When** Gọi `PUT /api/v1/packages/1/deactivate` không có body, hoặc với body `{}`
    - **Then** `200 OK` trả `id`, `code`, `status=INACTIVE`, `updatedAt`; các trường khác của gói không đổi; bản ghi không bị xóa
  - **AC-F3.4.1-02: Gói đã bán vẫn dùng được** (BR-F3-08)
    - **Given** Gói `id=1` đã được bán, có gói hội viên `ACTIVE` và dòng hóa đơn liên quan
    - **When** Quản lý ngừng bán gói `id=1`
    - **Then** Mọi bản ghi `memberships`, `invoices`, `invoice_items` liên quan giữ nguyên trạng thái, ngày và giá
  - **AC-F3.4.1-03: Gói ngừng bán không bán mới được nhưng vẫn tra cứu được** (BR-F3-09)
    - **Given** Gói `id=1` đã `INACTIVE`
    - **When** Lễ tân bán gói này (UC-F3.5), hoặc nhân viên lọc danh mục với `status=INACTIVE`
    - **Then** Bán mới bị từ chối `422` (AC-F3.5.1-08); lọc `status=INACTIVE` vẫn thấy gói
  - **AC-F3.4.1-04: Ngừng bán lần nữa bị từ chối**
    - **Given** Gói đã `INACTIVE`; hoặc hai quản lý cùng bấm ngừng bán một gói gần như đồng thời
    - **When** Gọi lại `PUT /api/v1/packages/{id}/deactivate`
    - **Then** `422 Unprocessable Content` *"Gói tập này đã ở trạng thái ngừng bán."*; khi gửi đồng thời thì đúng một request `200`, request còn lại `422`; `updated_at` không đổi thêm
  - **AC-F3.4.1-05: Sửa cùng lúc không làm gói bán lại**
    - **Given** Một quản lý đang sửa gói trong khi quản lý khác ngừng bán chính gói đó
    - **When** Hai thao tác diễn ra gần như đồng thời
    - **Then** Sau khi cả hai xong, gói vẫn `INACTIVE`
  - **AC-F3.4.1-06: Từ chối gói ngoài chi nhánh, gói không tồn tại và vai trò khác** (BR-F3-07, S3)
    - **Given** Gói thuộc chi nhánh khác hoặc là gói toàn chuỗi (kể cả đã `INACTIVE`); hoặc `id` không tồn tại; hoặc `id` không phải số nguyên dương; hoặc người gọi không phải Quản lý chi nhánh đang hoạt động; hoặc không gửi token
    - **When** Gọi `PUT /api/v1/packages/{id}/deactivate`
    - **Then** Lần lượt trả `403`, `404` *"Gói tập không tồn tại trên hệ thống."*, `400`, `403`, `401`; dữ liệu gói giữ nguyên

---

### 3.5. UC-F3.5: Bán gói tập tại quầy (đăng ký mới / gia hạn)

- **Mã UC:** UC-F3.5
- **Tên Use Case:** Bán gói tập tại quầy
- **Tác nhân chính:** Lễ tân
- **Tác nhân phụ:** Hệ thống thanh toán nội bộ (F5 – xác nhận thu tiền); Hệ thống thông báo (F10 – nhắc hết hạn)
- **Mô tả ngắn gọn:** Lễ tân tạo hóa đơn và gói tập (PENDING) cho hội viên sau khi thu tiền mặt hoặc chuyển khoản; khi xác nhận đã thu tiền (F5), hóa đơn chuyển PAID và gói được kích hoạt (ACTIVE hoặc UPCOMING nếu gia hạn nối tiếp). Đây là use case cốt lõi nhất của F3.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Lễ tân (JWT hợp lệ – S2).
- Hội viên đã có hồ sơ trong bảng `members` với `status = ACTIVE`.
- Gói muốn bán có `status = ACTIVE` trong bảng `packages`.

#### B. Hậu điều kiện (Postconditions)
- **Thành công – Tạo hóa đơn PENDING:**
  - Bảng `invoices`: một dòng mới với `status = PENDING`, `source = COUNTER`, `code` sinh tự động.
  - Bảng `invoice_items`: một dòng mới với `item_type = PACKAGE`, `description` chép tên gói, `unit_price` chép từ `packages.price` tại thời điểm bán (S7).
  - Bảng `memberships`: một dòng mới với `status = PENDING`, `start_date` dự kiến, `package_type` chép từ `packages.type`, `freeze_count = 0`.
- **Thành công – Sau khi xác nhận thu tiền (F5 kích hoạt):**
  - `invoices.status = PAID`; `payments` ghi một dòng.
  - `memberships.status` chuyển ACTIVE (nếu gói mới hoặc gói cũ đã hết hạn) hoặc UPCOMING (nếu gia hạn khi gói cũ còn hạn); `start_date` được tính lại tại thời điểm thanh toán.
- **Thất bại:** Giao dịch rollback; không có hóa đơn hay gói rác.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Lễ tân tìm hội viên theo mã hội viên (`member_code`) hoặc số điện thoại, chọn gói muốn bán, và gửi request tạo hóa đơn.
2. Hệ thống kiểm tra JWT hợp lệ và vai trò Lễ tân (Spring Security lọc trước Bean Validation – S2, S3).
3. Hệ thống kiểm tra tính hợp lệ đầu vào (Bean Validation – S5): `memberId`, `items` (chứa `itemType`, `packageId`) bắt buộc hợp lệ.
4. Hệ thống kiểm tra các quy tắc nghiệp vụ:
   - a. Hội viên `memberId` tồn tại và `status = ACTIVE`.
   - b. Gói `packageId` tồn tại, `status = ACTIVE`, và có thể dùng tại chi nhánh này (`branch_id` khớp hoặc rỗng).
   - c. Kiểm tra ràng buộc gói đang hiệu lực và gói gia hạn nối tiếp:
     - *Trường hợp 1 (Mua mới):* Hội viên chưa có gói cùng loại (`package_type`) đang `ACTIVE` hoặc `FROZEN` tại chi nhánh này (hoặc toàn chuỗi, xét theo `branch_id IS NOT DISTINCT FROM`): hệ thống kiểm tra thêm điều kiện hội viên **chưa có gói nào cùng loại ở trạng thái `PENDING`** đang chờ thanh toán. Nếu đã có gói `PENDING` cùng loại, từ chối và chuyển sang ngoại lệ **E6d** (422 Unprocessable Content); nếu chưa có, cho phép tạo gói (ràng buộc đảm bảo qua chỉ mục CSDL `uq_one_active_per_type` và kiểm tra ứng dụng).
     - *Trường hợp 2 (Gia hạn nối tiếp):* Hội viên đã có gói cùng loại đang `ACTIVE` hoặc `FROZEN`: hệ thống kiểm tra bằng mã nghiệp vụ xem hội viên đã có gói nào khác cùng loại ở trạng thái `UPCOMING` hoặc `PENDING` chưa. Nếu **đã có**, từ chối và chuyển sang ngoại lệ **E6d** (422 Unprocessable Content); nếu **chưa có**, cho phép tạo gói gia hạn (xem luồng A1 và BR-F3-11).
5. Hệ thống tính tổng tiền ở server (S7): `subtotal = package.price`; ở v1 (Sprint 1) chưa áp dụng khuyến mãi F14 nên `discount = 0`; `total = subtotal`.
6. Hệ thống tạo trong một giao dịch `@Transactional`:
   - Dòng `invoices` với `status = PENDING`, `source = COUNTER`, `code` sinh tự động (`discount = 0`, `total = subtotal`, `promotion_id = NULL`).
   - Dòng `invoice_items` với `unit_price = packages.price` tại thời điểm bán, `description` chép từ `packages.name`, `discount_amount = 0`.
   - Dòng `memberships` với `status = PENDING`, `package_type` chép từ `packages.type`, `total_sessions` chép từ `packages.session_count`, `remaining_sessions = total_sessions`, `freeze_count = 0`:
     - `start_date` dự kiến = `:today` (nếu mua mới; hoặc ngay sau `end_date` của gói cũ nếu đang gia hạn gói TIME hoặc gói SESSION còn buổi; hoặc `:today + 1 ngày` nếu gói SESSION cũ đã về 0 buổi).
     - `end_date` dự kiến = `start_date + packages.duration_days - 1`.
7. Hệ thống trả `201 Created` kèm `InvoiceDto` bao gồm `invoiceId` và `membershipId` để lễ tân chuyển sang màn hình thanh toán (F5).
8. Sau khi F5 xác nhận thu tiền: hệ thống cập nhật `invoices.status = PAID`, `memberships.status = ACTIVE` hoặc `UPCOMING`, tính lại `start_date` và `end_date` theo số ngày hiệu lực đã chốt lúc tạo hóa đơn (`số ngày = memberships.end_date - memberships.start_date + 1`), tuyệt đối không đọc lại `packages.duration_days` tại thời điểm thanh toán để bảo đảm tính toàn vẹn nếu gói tập bị sửa trong thời gian chờ (BR-F3-06, BR-F5-04).

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Gia hạn gói khi gói cũ còn hạn (UPCOMING):**
  - Tại bước 4c, hội viên đã có gói cùng loại đang `ACTIVE` hoặc `FROZEN`. Nếu bán thêm gói mới cùng loại, hệ thống cho phép nhưng gói mới ở trạng thái `PENDING`:
    - Nếu gói cũ là gói `TIME` (hoặc `SESSION`/`PT` còn buổi `remaining_sessions > 0`): `start_date` dự kiến = `end_date_gói_cũ + 1 ngày`.
    - Nếu gói cũ là gói `SESSION` đã về 0 buổi (`remaining_sessions = 0`, vẫn giữ `ACTIVE` đến hết ngày hôm nay): `start_date` dự kiến = ngày mai (`:today + 1 ngày`). Ngoại lệ này chỉ áp dụng cho `SESSION`; gói `PT` hết buổi chuyển `EXPIRED` ngay khi buổi cuối được xác nhận, theo bản chốt phạm vi.
    - `end_date` dự kiến = `start_date + packages.duration_days - 1`.
  - Khi thanh toán xong (F5), gói mới chuyển `UPCOMING` (chờ đến `start_date` để tự động kích hoạt `ACTIVE`).
  - Luồng tiếp tục từ bước 5.
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):**
  - Tại bước 3, thiếu `memberId` hoặc `packageId`, hoặc vi phạm Bean Validation.
  - Hệ thống trả `ProblemDetail` (400) với chi tiết lỗi từng trường.
- **E2 – Chưa đăng nhập hoặc token hết hạn (401 Unauthorized):**
  - Tại bước 2, JWT không có hoặc hết hạn.
  - Hệ thống trả `ProblemDetail` (401): *"Bạn cần đăng nhập để truy cập tài nguyên này."*
- **E3 – Không có quyền (403 Forbidden):**
  - Tại bước 2, người dùng đã xác thực nhưng không phải Lễ tân.
  - Hệ thống trả `ProblemDetail` (403): *"Bạn không có quyền thực hiện thao tác này."*
- **E4 – Hội viên hoặc gói không tồn tại (404 Not Found):**
  - Tại bước 4a hoặc 4b, `memberId` hoặc `packageId` không tìm thấy.
  - Hệ thống trả `ProblemDetail` (404): *"Hội viên không tồn tại trên hệ thống."* hoặc *"Gói tập không tồn tại trên hệ thống."*
- **E5 – Xung đột dữ liệu khi hai nhân viên thao tác đồng thời (409 Conflict / 422):**
  - Tại bước 4c, hệ thống thực hiện khóa bi quan bản ghi hội viên `members` bằng `SELECT ... FOR UPDATE` (NF6). Nếu hai lễ tân cùng thao tác tạo gói cho cùng một hội viên tại cùng một thời điểm, request thứ hai phải chờ request đầu hoàn tất giao dịch. Sau khi request đầu commit (đã tạo 1 gói `PENDING`), request thứ hai đọc lại dữ liệu sẽ bị chặn tại bước 4c và trả về lỗi **422 Unprocessable Content (E6d)** do đã có gói `PENDING` cùng loại.
  - Ngoại lệ `409 Conflict` chỉ phát sinh khi có xung đột khóa lạc quan `@Version` trên dữ liệu hội viên `members` (nếu thông tin hội viên bị cập nhật đồng thời bởi tiến trình khác).
- **E6a – Hội viên không hoạt động (422 Unprocessable Content):**
  - Tại bước 4a, hội viên có `status = INACTIVE`.
  - Hệ thống trả `ProblemDetail` (422): *"Hội viên đã ngừng hoạt động, không thể bán gói tập."*
- **E6b – Gói ngừng bán (422 Unprocessable Content):**
  - Tại bước 4b, gói có `status = INACTIVE`.
  - Hệ thống trả `ProblemDetail` (422): *"Gói tập này đã ngừng bán."*
- **E6c – Gói không áp dụng cho chi nhánh (422 Unprocessable Content):**
  - Tại bước 4b, `packages.branch_id` không rỗng và không khớp với chi nhánh lễ tân.
  - Hệ thống trả `ProblemDetail` (422): *"Gói tập này không áp dụng tại chi nhánh của bạn."*
- **E6d – Hội viên đã có gói UPCOMING hoặc PENDING cùng loại (422 Unprocessable Content):**
  - Tại bước 4c, hội viên đã có gói cùng loại ở trạng thái `UPCOMING` (đã gia hạn nối tiếp một lần) hoặc đang có một gói `PENDING` chờ thanh toán. Không cho phép tạo thêm gói khi gói `UPCOMING` hoặc `PENDING` vẫn còn để tránh xung đột quyền lợi và vi phạm quy tắc một gói gia hạn.
  - Hệ thống trả `ProblemDetail` (422): *"Hội viên đã có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt. Không thể tạo thêm gói mới."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F3-10:** Một hội viên được giữ đồng thời nhiều gói khác loại (TIME + PT), nhưng mỗi loại tối đa một gói ACTIVE hoặc FROZEN tại mỗi chi nhánh. Ràng buộc này thực thi trực tiếp tại cơ sở dữ liệu qua chỉ mục duy nhất có điều kiện `uq_one_active_per_type` (`UNIQUE NULLS NOT DISTINCT` trên `member_id, package_type, branch_id` WHERE `status IN ('ACTIVE','FROZEN')`). ([Bản chốt phạm vi – Gói tập](../ban-chot-pham-vi.md))
- **BR-F3-11:** Gia hạn khi gói cũ còn hạn: gói mới sau khi thanh toán ở trạng thái `UPCOMING`.
  - Quy tắc xác định ngày bắt đầu (`start_date`):
    - *Trường hợp thông thường:* Nếu gói cũ là gói `TIME` hoặc gói `SESSION`/`PT` vẫn còn buổi (`remaining_sessions > 0`), gói mới bắt đầu nối tiếp: `start_date = end_date_gói_cũ + 1 ngày`.
    - *Trường hợp đặc biệt (gói `SESSION` đã hết buổi):* Nếu gói `ACTIVE` cũ là gói `SESSION` đã hết buổi (`remaining_sessions = 0`, vẫn giữ `ACTIVE` đến hết ngày hôm nay để bảo đảm quyền lợi check-in trong ngày theo `ban-chot-pham-vi.md` dòng 93), khi gia hạn gói mới bắt đầu từ **ngày mai** (`start_date = :today + 1 ngày`) thay vì chờ đến hết `end_date_gói_cũ + 1 ngày`.
  - Trong thời gian chờ đến `start_date`, gói mới ở trạng thái `UPCOMING`, hội viên vẫn dùng gói cũ. Vì chỉ mục CSDL `uq_one_active_per_type` chỉ lọc trên `ACTIVE`/`FROZEN`, mã nghiệp vụ (Service) bắt buộc phải kiểm tra để chặn: hội viên không thể có đồng thời từ hai gói `UPCOMING` hoặc `PENDING` cùng loại (chỉ cho phép tối đa 1 gói gia hạn đang chờ kích hoạt). ([Bản chốt phạm vi – Gói tập](../ban-chot-pham-vi.md), [ADR 0005](../../decisions/0005-erd-v2.md) mục 27)
- **BR-F3-12:** Gói PENDING chuyển UPCOMING hoặc ACTIVE khi hóa đơn chuyển PAID (do F5 kích hoạt). Ngày bắt đầu (`start_date`) và ngày kết thúc (`end_date`) tính lại tại thời điểm thanh toán, không phải thời điểm tạo hóa đơn. ([Bản chốt phạm vi – Gói tập](../ban-chot-pham-vi.md))
- **BR-F3-13:** `invoice_items.unit_price` chép từ `packages.price` tại thời điểm tạo hóa đơn và không thay đổi kể cả khi giá gói được sửa sau đó (S7). ([Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md))
- **BR-F3-14:** Hóa đơn tại quầy (`source = COUNTER`) **không tự hủy**; chỉ hóa đơn từ cổng hội viên (`source = MEMBER_PORTAL`) mới tự hủy sau `PENDING_INVOICE_TTL_DAYS` ngày. ([Bản chốt phạm vi – Gói tập](../ban-chot-pham-vi.md))
- **CONFIG-01:** `PENDING_INVOICE_TTL_DAYS` – đọc từ `system_configs` với key `PENDING_INVOICE_TTL_DAYS` (giá trị mặc định: `3`). Không áp dụng cho hóa đơn tại quầy (quy tắc số 15).
- **Toàn vẹn đồng thời (NF6):** Khi tạo `memberships` (bước 4), Service thực hiện khóa bi quan dòng cha `members` bằng `SELECT ... FOR UPDATE` (theo `ADR 0005` mục 21). Nhờ đó, hai giao dịch đồng thời tạo gói cho cùng một hội viên sẽ tuần tự hóa: giao dịch thứ hai đợi giao dịch thứ nhất commit rồi mới kiểm tra bước 4c, phát hiện đã có gói `PENDING` cùng loại và từ chối 422 (E6d). Vì bản ghi `memberships` được tạo mới (`INSERT`), nó không gây xung đột `@Version` trên `memberships.version`.
- **Bảo mật (S7):** Tổng tiền (`total`) luôn tính ở server; không tin giá trị `total` do client gửi lên.
- **Hiệu năng (NF1):** API tạo hóa đơn phản hồi dưới 2 giây trong điều kiện bình thường.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `packages`, `invoices`, `invoice_items`, `memberships`, `payments` (sau F5). *(Lưu ý: cột `invoices.promotion_id` để NULL ở v1, bảng `promotions` thuộc phạm vi F14 ở v3).*
- **Chi tiết trường dữ liệu:**

  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `invoices` | `id` | `bigint` | PK, Generated Always | Khóa chính tự tăng |
  | `invoices` | `code` | `varchar(30)` | UNIQUE, NOT NULL | Mã hóa đơn dùng nội bộ và in ra PDF |
  | `invoices` | `branch_id` | `bigint` | NOT NULL, FK → `branches.id` | Chi nhánh lập hóa đơn |
  | `invoices` | `member_id` | `bigint` | nullable, FK → `members.id` | Hội viên mua gói; bắt buộc khi hóa đơn có dòng PACKAGE (kiểm tra trong code) |
  | `invoices` | `created_by` | `bigint` | NOT NULL, FK → `users.id` | Lễ tân lập hóa đơn |
  | `invoices` | `source` | `varchar(20)` | NOT NULL, CHECK IN ('COUNTER','MEMBER_PORTAL') | COUNTER khi lễ tân lập |
  | `invoices` | `promotion_id` | `bigint` | nullable, FK → `promotions.id` | Mã khuyến mãi (ở v1 luôn để NULL; kích hoạt ở F14 – v3) |
  | `invoices` | `subtotal` | `int` | NOT NULL | Giá trước giảm (VND) |
  | `invoices` | `discount` | `int` | NOT NULL, default 0 | Tổng giảm giá (VND; ở v1 luôn bằng 0) |
  | `invoices` | `total` | `int` | NOT NULL, CHECK total = subtotal - discount AND total >= 0 | Giá sau giảm – tính ở server (S7) |
  | `invoices` | `status` | `varchar(20)` | NOT NULL, CHECK IN ('PENDING','PAID','CANCELLED','REFUNDED') | PENDING → PAID khi F5 xác nhận |
  | `invoices` | `version` | `int` | NOT NULL, default 0 | Khóa lạc quan (`@Version`) |
  | `invoice_items` | `id` | `bigint` | PK, Generated Always | Khóa chính tự tăng |
  | `invoice_items` | `invoice_id` | `bigint` | NOT NULL, FK → `invoices.id` | Hóa đơn cha |
  | `invoice_items` | `item_type` | `varchar(20)` | NOT NULL, CHECK IN ('PACKAGE','PRODUCT','TRANSFER_FEE') | PACKAGE khi bán gói |
  | `invoice_items` | `package_id` | `bigint` | nullable, FK → `packages.id` | Bắt buộc khi `item_type = PACKAGE` |
  | `invoice_items` | `description` | `varchar(255)` | NOT NULL | Tên hiển thị trên hóa đơn PDF, chép tên gói lúc bán |
  | `invoice_items` | `unit_price` | `int` | NOT NULL | Giá tại thời điểm bán (chép từ `packages.price`) |
  | `invoice_items` | `quantity` | `int` | NOT NULL, default 1, CHECK > 0 | Số lượng (thường = 1 cho gói) |
  | `invoice_items` | `amount` | `int` | NOT NULL, CHECK amount = quantity * unit_price | Thành tiền (trước giảm giá) |
  | `invoice_items` | `discount_amount` | `int` | NOT NULL, default 0, CHECK 0 <= discount_amount <= amount | Phần giảm giá phân bổ cho dòng (ở v1 luôn bằng 0) |
  | `memberships` | `id` | `bigint` | PK, Generated Always | Khóa chính tự tăng |
  | `memberships` | `member_id` | `bigint` | NOT NULL, FK → `members.id` | Chủ gói |
  | `memberships` | `package_id` | `bigint` | NOT NULL, FK → `packages.id` | Gói mua |
  | `memberships` | `package_type` | `varchar(10)` | NOT NULL, CHECK IN ('TIME','SESSION','PT') | Chép từ `packages.type` lúc mua |
  | `memberships` | `branch_id` | `bigint` | nullable, FK → `branches.id` | Chép từ `packages.branch_id`; rỗng = toàn chuỗi |
  | `memberships` | `sold_branch_id` | `bigint` | NOT NULL, FK → `branches.id` | Chi nhánh bán (dùng cho báo cáo) |
  | `memberships` | `invoice_item_id` | `bigint` | nullable, FK → `invoice_items.id` | Dòng hóa đơn tạo ra gói |
  | `memberships` | `start_date` | `date` | NOT NULL | Dự kiến khi PENDING; tính lại lúc PAID |
  | `memberships` | `end_date` | `date` | NOT NULL, CHECK >= start_date | Ngày hết hạn |
  | `memberships` | `total_sessions` | `int` | nullable | Chép từ `packages.session_count` |
  | `memberships` | `remaining_sessions` | `int` | nullable, CHECK >= 0 | Số buổi còn lại |
  | `memberships` | `status` | `varchar(20)` | NOT NULL, CHECK IN ('PENDING','UPCOMING','ACTIVE','FROZEN','EXPIRED','TRANSFERRED','CANCELLED') | PENDING → ACTIVE hoặc UPCOMING sau khi thanh toán |
  | `memberships` | `freeze_count` | `int` | NOT NULL, default 0 | Số lần đã bảo lưu (tối đa `FREEZE_MAX_TIMES` lần) |
  | `memberships` | `version` | `int` | NOT NULL, default 0 | Khóa lạc quan (`@Version`) |

#### G. Thiết kế giao diện và API liên quan

**Bước 1 – Tạo hóa đơn và gói PENDING:**
- **API Endpoint:** `POST /api/v1/invoices`
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body (DTO):**
  ```json
  {
    "memberId": 123,
    "items": [
      {
        "itemType": "PACKAGE",
        "packageId": 5
      }
    ]
  }
  ```
- **Response Success (201 Created):**
  ```json
  {
    "invoiceId": 88,
    "invoiceCode": "HD000088",
    "membershipId": 77,
    "memberId": 123,
    "packageId": 5,
    "packageName": "Gói PT 10 buổi",
    "subtotal": 3000000,
    "discount": 0,
    "total": 3000000,
    "status": "PENDING",
    "membershipStatus": "PENDING",
    "startDate": "2026-11-03",
    "endDate": "2027-01-31"
  }
  ```

**Bước 2 – Xác nhận thu tiền (do F5 đảm nhiệm):**
- **API Endpoint:** `POST /api/v1/invoices/{invoiceId}/payments`
- Xem chi tiết tại đặc tả F5 ([`dac-ta-use-case-f5-thanh-toan-tai-quay.md`](dac-ta-use-case-f5-thanh-toan-tai-quay.md)).

- **Màn hình giao diện liên quan:** Màn hình "Bán gói tập" và "Gia hạn gói" – `docs/design/wireframe/f3-ban-gia-han-goi.drawio.svg`.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F3.5.1: Bán và gia hạn gói tại quầy
- **User story:** Là **Lễ tân**, tôi muốn **bán gói mới hoặc gia hạn nối tiếp cho hội viên**, để **hội viên tập liên tục**.
- **BR liên quan:** BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14; S7, NF6
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #134
- **Tiêu chí chấp nhận:**
  - **AC-F3.5.1-01: Bán gói tạo hóa đơn chờ và gói chờ, giá lấy ở server** (BR-F3-04, BR-F3-13, S7)
    - **Given** Lễ tân `CN01` đã đăng nhập; hội viên `id=123` đang hoạt động; gói `id=1` đang bán, giá 500.000 đ
    - **When** Gọi `POST /api/v1/invoices` với `memberId=123`, `items=[{itemType: PACKAGE, packageId: 1}]`
    - **Then** `201 Created` trả `invoiceId`, `membershipId`; hóa đơn `PENDING`, `source=COUNTER`, `subtotal = total = 500000`, `discount = 0`; dòng hóa đơn có `unit_price = 500000` chép từ `packages.price`; gói hội viên `PENDING`. Giá hoặc tổng tiền client gửi kèm (nếu có) không được dùng
  - **AC-F3.5.1-02: Gói chỉ hiệu lực sau khi hóa đơn PAID; ngày hết hạn tính đúng** (BR-F3-12)
    - **Given** Hội viên chưa có gói cùng loại; đã có hóa đơn và gói `PENDING` cho gói TIME 30 ngày
    - **When** Thu tiền qua F5 ngày 01/11/2026 (hóa đơn chuyển `PAID`)
    - **Then** Gói chuyển `ACTIVE`; `start_date = 2026-11-01`; `end_date = 2026-11-30` (`start_date + số ngày - 1`, không phải 01/12). Số ngày lấy từ khoảng ngày đã chốt lúc tạo hóa đơn, không đọc lại `packages.duration_days`. Trước khi `PAID`, gói không dùng được
  - **AC-F3.5.1-03: Gia hạn khi gói cũ còn hiệu lực** (BR-F3-11)
    - **Given** Hội viên có gói TIME `ACTIVE` hết hạn ngày 31/12/2026
    - **When** Lễ tân bán thêm một gói TIME và thu tiền
    - **Then** Gói mới `UPCOMING`, `start_date = 2027-01-01`; gói cũ vẫn `ACTIVE` tới hết ngày 31/12/2026
  - **AC-F3.5.1-04: Gia hạn khi gói SESSION cũ đã hết buổi** (BR-F3-11)
    - **Given** Hội viên có gói SESSION còn hạn tới 30/11 nhưng đã bị trừ buổi cuối ngày hôm nay (vẫn `ACTIVE` tới hết hôm nay)
    - **When** Lễ tân bán gói SESSION gia hạn và thu tiền trong ngày
    - **Then** Gói mới `UPCOMING`, bắt đầu từ ngày mai, không chờ tới 01/12
  - **AC-F3.5.1-05: Giữ đồng thời nhiều loại gói** (BR-F3-10)
    - **Given** Hội viên có gói TIME `ACTIVE`, chưa có gói PT
    - **When** Lễ tân bán gói PT và thu tiền
    - **Then** Gói PT `ACTIVE`; gói TIME không bị ảnh hưởng
  - **AC-F3.5.1-06: Chỉ một gói chờ cho mỗi loại** (BR-F3-11)
    - **Given** Hội viên đã có một gói cùng loại đang `UPCOMING`, hoặc đang có một gói cùng loại `PENDING` chờ thanh toán
    - **When** Lễ tân bán thêm gói cùng loại
    - **Then** `422 Unprocessable Content` *"Hội viên đã có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt. Không thể tạo thêm gói mới."*; không có hóa đơn hay gói nào được tạo
  - **AC-F3.5.1-07: Hai lễ tân bán cùng lúc cho một hội viên** (NF6)
    - **Given** Hội viên chưa có gói TIME; hai lễ tân gửi yêu cầu bán gói TIME gần như đồng thời
    - **When** Cả hai request tới server
    - **Then** Đúng một request `201 Created`; request còn lại `422` với thông báo như AC-F3.5.1-06; chỉ có một hóa đơn và một gói `PENDING`
  - **AC-F3.5.1-08: Hội viên hoặc gói không hợp lệ** (BR-F3-09)
    - **Given** Hội viên đã ngừng hoạt động; hoặc gói đã ngừng bán; hoặc gói thuộc chi nhánh khác; hoặc `memberId`, `packageId` không tồn tại
    - **When** Lễ tân bán gói
    - **Then** Lần lượt trả `422` *"Hội viên đã ngừng hoạt động, không thể bán gói tập."*, `422` *"Gói tập này đã ngừng bán."*, `422` *"Gói tập này không áp dụng tại chi nhánh của bạn."*, `404`; không có hóa đơn hay gói nào được tạo
  - **AC-F3.5.1-09: Hóa đơn tại quầy không tự hủy** (BR-F3-14)
    - **Given** Hóa đơn `source=COUNTER` còn `PENDING` quá `PENDING_INVOICE_TTL_DAYS` ngày
    - **When** Tác vụ nền hủy hóa đơn chờ quá hạn chạy
    - **Then** Hóa đơn và gói `PENDING` đi kèm giữ nguyên; chỉ hóa đơn `source=MEMBER_PORTAL` bị hủy
  - **AC-F3.5.1-10: Tự kích hoạt gói nối tiếp đúng ngày** (BR-F3-11)
    - **Given** Hội viên có gói cũ hết hạn hôm qua và gói nối tiếp `UPCOMING` có `start_date` là hôm nay
    - **When** Tác vụ nền chạy lúc 00:00 giờ Việt Nam (hoặc chạy bù khi ứng dụng khởi động nếu lần chạy trước bị lỡ)
    - **Then** Tác vụ chuyển gói hết hạn sang `EXPIRED` trước, rồi chuyển gói `UPCOMING` sang `ACTIVE`; không lúc nào có hai gói cùng loại `ACTIVE`. Gói SESSION 0 buổi chỉ chuyển `EXPIRED` khi hôm nay chưa bị trừ buổi
  - **AC-F3.5.1-11: Dữ liệu sai và vai trò khác bị từ chối** (S3, S5)
    - **Given** Request thiếu `memberId` hoặc `packageId`; hoặc người gọi không phải Lễ tân; hoặc không gửi token
    - **When** Gọi `POST /api/v1/invoices`
    - **Then** Lần lượt trả `400` kèm lỗi từng trường, `403`, `401`; không có hóa đơn hay gói nào được tạo
  - **AC-F3.5.1-12: Xem gói đang giữ và ngày gia hạn dự kiến** (BR-F3-11, S3)
    - **Given** Chủ chuỗi, Quản lý chi nhánh hoặc Lễ tân đã đăng nhập
    - **When** Gọi `GET /api/v1/memberships?memberId={id}` với ID nguyên dương
    - **Then** Trả `200` và danh sách gói `PENDING`, `UPCOMING`, `ACTIVE`, `FROZEN`, kèm tên gói, ngày, số buổi và `renewalStartDate`. Ngày dự kiến dùng cùng phép tính với lúc bán, không sớm hơn hôm nay; trả `null` nếu gói đang chờ hoặc cùng nhóm đã có gói chờ. Nhóm được xét theo loại và phạm vi chi nhánh (toàn chuỗi là một nhóm riêng). Nhân viên chỉ xem hội viên trong chi nhánh mình; ngoài phạm vi hoặc không tồn tại trả `404`; Chủ chuỗi xem mọi chi nhánh. Vai trò khác trả `403`, thiếu token `401`, thiếu/sai ID `400`
  - **AC-F3.5.1-13: Mã hóa đơn tự sinh và duy nhất**
    - **Given** Yêu cầu bán hợp lệ
    - **When** Tạo hóa đơn tại quầy
    - **Then** Response trả thêm `invoiceCode` dạng `HD` + 6 chữ số, sinh từ sequence dùng chung; các hóa đơn mới có mã khác nhau. Không đổi mã hóa đơn cũ. Sequence có thể có khoảng trống khi giao dịch rollback; không cam kết mã liên tục

#### I. Ranh giới triển khai S2-07 (#182)

- S2-07 tạo hóa đơn/gói `PENDING`, tính ngày dự kiến, chặn gói chờ và triển khai tác vụ vòng đời. Khi bán, hội viên ngoài chi nhánh lễ tân trả `404` theo phạm vi F2; hồ sơ xóa mềm coi như không tồn tại.
- Phần thu tiền của AC-02, AC-03, AC-04, AC-05 do S2-09 (#184) kích hoạt, tính lại ngày theo thời hạn đã chốt. Kiểm thử ngày dự kiến trong S2-07 chỉ chứng minh phần trước thanh toán.
- AC-09 phải nghiệm thu cùng tác vụ hủy hóa đơn của S2-14 (#194). S2-07 lưu `source=COUNTER`; tác vụ vòng đời không sửa hóa đơn hoặc gói `PENDING`.
- Tác vụ dùng ngày Việt Nam, chuyển `EXPIRED` và flush trước khi kích hoạt `UPCOMING`; chạy lại không đổi thêm. Chạy bù xử lý cả gói có `start_date < hôm nay`; gói chờ đã trôi hết hạn chuyển `EXPIRED`. Nếu cùng nhóm còn `ACTIVE`/`FROZEN`, giữ gói chờ và ghi cảnh báo, kể cả đã qua ngày cuối. Các giả định xử lý dữ liệu lệch này cần được người duyệt nghiệp vụ xác nhận.
- Khi tác vụ lỗi, rollback toàn bộ lượt; thử lại sau 5 phút trong cùng ngày Việt Nam. Kiểm thử tắt lịch tự động và gọi service với ngày cố định.
- Mỗi hóa đơn hiện nhận đúng một dòng `PACKAGE`. Mã hóa đơn sáu chữ số kế thừa hướng tham khảo, cần người duyệt xác nhận AC-13 trước nghiệm thu; hết dải mã trả `422`, không quay vòng.

---

> **Tài liệu liên quan tiếp theo:** Xem chi tiết quy trình thu tiền, xuất hóa đơn và kích hoạt gói tại [Đặc tả Use Case – F5: Thanh toán tại quầy](dac-ta-use-case-f5-thanh-toan-tai-quay.md).
