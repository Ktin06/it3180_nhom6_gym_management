# Mẫu Đặc tả Use Case (Use Case Specification Template)

> **Tài liệu hướng dẫn và mẫu chuẩn dùng chung** cho cả 5 thành viên phụ trách viết đặc tả SRS trong dự án (Đức Việt – F1, F2; Lân – F3, F5; Minh Quân – F4, F6; Trọng Việt – F7, F9, F10; Gia Bảo – F8).  
> **Quy định:** Không dùng GitHub Spec Kit ([kế hoạch Sprint 1, mục 4](../../plans/sprint-1-tien-do.md#4-chỉnh-lại-so-với-kế-hoạch-cũ)). Mọi file đặc tả lưu tại thư mục `docs/requirements/srs/`, đặt tên dạng `dac-ta-use-case-f<số>-<ten-chuc-nang>.md`.  
> Khi viết đặc tả, hãy sao chép toàn bộ cấu trúc từ **Mục 2: Khung mẫu đặc tả** bên dưới, điền đầy đủ thông tin và xóa bỏ các đoạn chú thích gợi ý trong ngoặc vuông `[...]`.

---

## 1. Hướng dẫn và quy ước viết đặc tả

1. **Tuân thủ bản chốt phạm vi:** Mọi quy tắc nghiệp vụ phải trỏ đúng về [Bản chốt phạm vi](../ban-chot-pham-vi.md), không tự ý bổ sung tính năng ngoài phạm vi (đặc biệt: không thanh toán online, không AI ngoài F20, không đăng nhập bằng số điện thoại, giá và số tiền tính ở server).
2. **Tuân thủ thiết kế dữ liệu:** Tên bảng, tên cột, kiểu dữ liệu, trạng thái và khóa ngoại phải khớp hoàn toàn với [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml) và [Giải thích dữ liệu](../../design/erd/thiet-ke-du-lieu.md).
3. **Mã hóa Use Case:** Đặt theo cấu trúc `UC-F<Mã_chức_năng>.<Số_thứ_tự>`, ví dụ: `UC-F4.1`, `UC-F4.2`.
4. **Mã hóa luồng:**
   - Luồng chính: Đánh số thứ tự bước `1, 2, 3...` mô tả rõ hành động của tác nhân và phản hồi tương ứng của hệ thống.
   - Luồng thay thế (Alternative Flow): Đặt mã `A1, A2...`.
   - Luồng ngoại lệ (Exception Flow): Đặt mã `E1, E2...`, ghi rõ mã lỗi HTTP (400, 401, 403, 404, 409, 422) và thông báo lỗi bằng tiếng Việt (`ProblemDetail`).
5. **Quy định mã lỗi HTTP chuẩn:**
   - `400 Bad Request`: Sai định dạng dữ liệu đầu vào (Bean Validation).
   - `401 Unauthorized`: Chưa đăng nhập hoặc token không hợp lệ/hết hạn.
   - `403 Forbidden`: Đã đăng nhập nhưng không có vai trò hoặc không có quyền thao tác trên tài nguyên.
   - `404 Not Found`: Không tìm thấy bản ghi theo id/mã.
   - `409 Conflict`: Vi phạm ràng buộc duy nhất (trùng email, trùng SĐT, trùng mã, trùng lịch) hoặc xung đột phiên bản `@Version` (Optimistic Locking).
   - `422 Unprocessable Content`: Vi phạm quy tắc nghiệp vụ chuyên sâu (`BusinessRuleException`), ví dụ: gói không hợp lệ, không đủ số dư buổi, ngoài khung giờ mở cửa.
6. **Tiêu chí chấp nhận (Acceptance Criteria):** Viết theo cấu trúc `Given ... When ... Then ...` để làm căn cứ trực tiếp khi viết kiểm thử tự động (JUnit) và tạo Issue trên GitHub Projects.

---

## 2. Khung mẫu đặc tả chuẩn (Template)

*(Bắt đầu sao chép từ đây)*

```markdown
# Đặc tả Use Case – F[Số]: [Tên chức năng]

- **Mã chức năng:** F[Số]
- **Version:** [v1 / v2 / v3 / v4 / v5] ([Bắt buộc / Mục tiêu])
- **Người biên soạn:** [Họ và tên thành viên]
- **Người duyệt chéo:** [Họ và tên thành viên duyệt theo phân công mục 2.4 Sprint 1]
- **Tài liệu chuẩn tham chiếu:** 
  - [Bản chốt phạm vi](../ban-chot-pham-vi.md)
  - [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml)
  - [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md)
  - [Quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md)
  - [ADR liên quan](../../decisions/)

---

## 1. Tổng quan chức năng

[Mô tả tổng quát mục đích, phạm vi của chức năng trong hệ thống quản lý chuỗi phòng tập gym. Liệt kê các năng lực chính của chức năng, các vai trò được phép tiếp cận, và lưu ý về phạm vi của chức năng ở sprint hiện tại.]

---

## 2. Danh sách Use Case

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F[X].1** | [Tên use case 1] | [Khách / Hội viên / Lễ tân / PT / Quản lý / Chủ chuỗi] | Cao / Trung bình / Thấp | Sprint 1 / Sprint 2 |
| **UC-F[X].2** | [Tên use case 2] | [...] | [...] | [...] |

---

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F[X].1: [Tên Use Case 1]

- **Mã UC:** UC-F[X].1
- **Tên Use Case:** [Tên đầy đủ của use case]
- **Tác nhân chính:** [Vai trò kích hoạt và tương tác trực tiếp với hệ thống]
- **Tác nhân phụ (nếu có):** [Hệ thống nền, dịch vụ thông báo, vai trò liên đới]
- **Mô tả ngắn gọn:** [Tóm tắt 1-2 câu về mục tiêu của use case]

#### A. Tiền điều kiện (Preconditions)
- [Điều kiện hệ thống, dữ liệu hoặc quyền hạn cần thỏa mãn trước khi use case bắt đầu, ví dụ: Người dùng đã đăng nhập với vai trò X, gói tập đang ở trạng thái ACTIVE...]

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** [Dữ liệu nào được tạo mới, cập nhật, xóa mềm trong CSDL; trạng thái thay đổi ra sao; email/thông báo nào được phát ra...]
- **Thất bại:** [Trạng thái hệ thống được giữ nguyên vẹn (rollback giao dịch), không có dữ liệu rác...]

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Tác nhân thực hiện thao tác [hành động cụ thể trên giao diện hoặc gửi request API].
2. Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào [Bean Validation: định dạng, bắt buộc...].
3. Hệ thống kiểm tra quyền hạn và vai trò của người thao tác (S3).
4. Hệ thống thực hiện kiểm tra quy tắc nghiệp vụ [ví dụ: tra cứu CSDL, kiểm tra trạng thái gói, kiểm tra trùng lịch...].
5. Hệ thống thực hiện thay đổi dữ liệu trong CSDL trong một giao dịch an toàn (`@Transactional`).
6. Hệ thống trả về kết quả thành công [mã HTTP 200 OK / 201 Created] kèm dữ liệu phản hồi tương ứng.
7. [Tùy chọn] Hệ thống kích hoạt các tác vụ nền liên quan (gửi email, thông báo real-time, ghi audit log...).

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – [Tên luồng thay thế]:**
  - Tại bước [X] của luồng chính, nếu [điều kiện thay thế xảy ra]...
  - Hệ thống thực hiện [xử lý tương ứng] và tiếp tục bước [Y] của luồng chính.
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):**
  - Tại bước 2, nếu một hoặc nhiều trường vi phạm Bean Validation (thiếu trường bắt buộc, sai định dạng số/ngày, chuỗi quá dài...).
  - Hệ thống từ chối xử lý và trả về `ProblemDetail` (400) chứa danh sách lỗi chi tiết theo từng trường (`errors`).
- **E2 – Không có quyền truy cập tài nguyên (403 Forbidden):**
  - Tại bước 3, người thao tác không đúng vai trò hoặc thao tác trên dữ liệu chi nhánh/hội viên khác (S3).
  - Hệ thống trả `ProblemDetail` (403): *"Bạn không có quyền thực hiện thao tác này."*
- **E3 – Không tìm thấy bản ghi dữ liệu (404 Not Found):**
  - Tại bước 4, định danh yêu cầu không tồn tại trong CSDL.
  - Hệ thống trả `ProblemDetail` (404): *"[Tên thực thể] không tồn tại trên hệ thống."*
- **E4 – Xung đột dữ liệu / Trùng lặp (409 Conflict):**
  - Tại bước 4, vi phạm ràng buộc duy nhất hoặc xung đột phiên bản `@Version` (lạc quan).
  - Hệ thống trả `ProblemDetail` (409): *"[Mô tả nguyên nhân xung đột cụ thể]."*
- **E5 – Vi phạm quy tắc nghiệp vụ (422 Unprocessable Content):**
  - Tại bước 4, dữ liệu hợp lệ về cú pháp nhưng vi phạm logic kinh doanh phòng tập.
  - Hệ thống từ chối và trả `ProblemDetail` (422): *"[Thông báo lý do nghiệp vụ bằng tiếng Việt]."*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **[Mã quy tắc, ví dụ: BR-01]:** [Mô tả chi tiết quy tắc nghiệp vụ áp dụng, trỏ cụ thể về mục tương ứng trong `docs/requirements/ban-chot-pham-vi.md`].
- **[Tham số cấu hình, ví dụ: CONFIG-01]:** Đọc từ bảng `system_configs` với key `[KEY_NAME]` (giá trị mặc định: `[Giá trị]`), không viết cứng mã nguồn (Quy tắc số 15).
- **[Yêu cầu phi chức năng]:**
  - Hiệu năng (NF1): Thời gian phản hồi API dưới [X] giây.
  - Toàn vẹn đồng thời (NF6): [Mô tả cơ chế khóa bi quan / lạc quan nếu có giao dịch cạnh tranh].
  - Bảo mật (S1–S10): [Kiểm tra JWT (S2), phân quyền backend (S3), tính tiền server (S7)...].

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `[tên_bảng_1]`, `[tên_bảng_2]`
- **Chi tiết trường dữ liệu:**
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `[table]` | `id` | `bigint` | PK, Generated Always | Khóa chính tự tăng |
  | `[table]` | `[column]` | `varchar(20)` | NOT NULL, CHECK (...) | [Giải thích] |
  | `[table]` | `created_at` | `timestamptz` | NOT NULL, Default now() | Thời gian tạo (UTC) |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `[POST / GET / PUT / DELETE] /api/v1/[duong-dan]`
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body (DTO):**
  ```json
  {
    "truong1": "gia_tri",
    "truong2": 123
  }
  ```
- **Response Success (200 OK / 201 Created):**
  ```json
  {
    "id": 1,
    "truong1": "gia_tri"
  }
  ```
- **Màn hình giao diện liên quan:** [Tên màn hình, trỏ tới file wireframe tương ứng trong `docs/design/wireframe/`].

#### H. Tiêu chí chấp nhận (Acceptance Criteria)
- **Kịch bản 1: [Tên kịch bản thành công]**
  - **Given** [Trạng thái ban đầu của hệ thống và người dùng]
  - **When** [Người dùng thực hiện hành động]
  - **Then** [Kết quả mong đợi, dữ liệu thay đổi và mã trả về]
- **Kịch bản 2: [Tên kịch bản ngoại lệ / thất bại]**
  - **Given** [...]
  - **When** [...]
  - **Then** [...]

---

*(Lặp lại mục 3 cho các Use Case tiếp theo: UC-F[X].2, UC-F[X].3...)*
```
