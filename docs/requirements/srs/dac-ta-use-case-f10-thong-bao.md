# Đặc tả Use Case – F10: Thông báo

- **Mã chức năng:** F10
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

Thông báo giúp hội viên nắm kịp các sự kiện liên quan đến gói tập và lớp học mà không cần hỏi quầy lễ tân. Hệ thống tự động gửi thông báo khi có sự kiện, qua hai kênh:

- **Email.**
- **Trong web:** hiển thị trong cổng hội viên.

Có 4 loại thông báo: sắp hết hạn gói, nhắc lịch lớp, được đưa vào lớp từ danh sách chờ, đổi giờ lớp. Người nhận là **Hội viên**. Ở Sprint 1, tài liệu này chỉ đặc tả; phần hiện thực làm ở Sprint 2.

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
- **Mô tả ngắn gọn:** Hệ thống báo cho hội viên biết gói tập sắp hết hạn để kịp gia hạn.

#### A. Tiền điều kiện (Preconditions)
- Hội viên có gói đang hoạt động, ngày hết hạn còn **[cần chốt với nhóm: bao nhiêu ngày]**.
- Gói này chưa có thông báo hết hạn nào được gửi cho hội viên.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Một thông báo trong web được tạo và một email được gửi cho hội viên. Hệ thống ghi nhận đã gửi để không gửi lặp.
- **Thất bại:** Không có thông báo nào được ghi nhận là đã gửi; lần chạy sau thử lại.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Tác vụ định kỳ chạy và truy vấn các gói sắp hết hạn.
2. Hệ thống loại những gói đã có thông báo hết hạn.
3. Hệ thống tạo thông báo trong web cho từng hội viên.
4. Hệ thống gửi email cho hội viên.
5. Hệ thống ghi nhận trạng thái đã gửi trong một giao dịch (`@Transactional`).

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Hội viên không có email:**
  - Tại bước 4, hệ thống chỉ tạo thông báo trong web, bỏ qua email.
- **E1 – Gửi email thất bại:**
  - Tại bước 4, dịch vụ email lỗi. Thông báo trong web vẫn được tạo; email ghi nhận lỗi và được thử lại ở lần chạy sau *(đề xuất, cần nhóm xác nhận)*.
- **E5 – Vi phạm quy tắc nghiệp vụ:**
  - Gói đã được gia hạn trước khi tác vụ chạy: hệ thống không gửi thông báo.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F10.1:** Mỗi sự kiện tạo một thông báo trong web và một email.
- **BR-F10.2:** Không gửi trùng: một gói chỉ có một thông báo sắp hết hạn cho mỗi lần hết hạn.
- **CONFIG-F10.1:** Số ngày báo trước đọc từ bảng `system_configs`, không viết cứng (Quy tắc số 15). Tên khóa và giá trị mặc định: **[cần chốt với nhóm]**.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `[bảng thông báo]`, `[bảng gói đã đăng ký của hội viên]`, `members`, `system_configs` (đọc)
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`)*
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `[...]` | `[...]` | `[...]` | `[...]` | Loại thông báo |
  | `[...]` | `[...]` | `[...]` | `[...]` | Nội dung thông báo |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** Không có; đây là tác vụ nền của hệ thống.
- **Giao diện:** Thông báo hiện trong cổng hội viên (xem UC-F10.5) và trong email.

---

### 3.2. UC-F10.2: Gửi thông báo nhắc lịch lớp

- **Mã UC:** UC-F10.2
- **Tên Use Case:** Gửi thông báo nhắc lịch lớp
- **Tác nhân chính:** Hệ thống (tác vụ chạy định kỳ)
- **Tác nhân phụ (nếu có):** Dịch vụ gửi email; Hội viên (người nhận)
- **Mô tả ngắn gọn:** Hệ thống nhắc hội viên về lớp đã đặt sắp diễn ra.

#### A. Tiền điều kiện (Preconditions)
- Hội viên có chỗ trong một lớp, lớp bắt đầu sau **[cần chốt với nhóm: bao lâu]**.
- Lớp chưa bị hủy.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Một thông báo trong web được tạo và một email được gửi; ghi nhận đã nhắc.
- **Thất bại:** Không ghi nhận đã gửi; lần chạy sau thử lại.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Tác vụ định kỳ chạy và truy vấn các lớp sắp diễn ra.
2. Hệ thống lấy danh sách hội viên đã có chỗ trong lớp, loại người đã được nhắc.
3. Hệ thống tạo thông báo trong web cho từng hội viên.
4. Hệ thống gửi email cho hội viên.
5. Hệ thống ghi nhận đã nhắc trong một giao dịch (`@Transactional`).

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Hội viên không có email:** chỉ tạo thông báo trong web.
- **E1 – Gửi email thất bại:** thông báo trong web vẫn được tạo; email thử lại ở lần chạy sau *(đề xuất)*.
- **E5 – Lớp đã bị hủy hoặc hội viên đã hủy chỗ:** hệ thống không gửi nhắc.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F10.1:** Mỗi sự kiện tạo một thông báo trong web và một email.
- **BR-F10.2:** Mỗi hội viên chỉ được nhắc một lần cho mỗi buổi lớp.
- **CONFIG-F10.2:** Thời điểm nhắc trước giờ lớp đọc từ `system_configs`. Tên khóa và giá trị mặc định: **[cần chốt với nhóm]**.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `[bảng thông báo]`, `[bảng lớp học và chỗ đăng ký]`, `members`, `system_configs` (đọc)
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`)*

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** Không có; tác vụ nền.

---

### 3.3. UC-F10.3: Gửi thông báo được đưa vào lớp từ danh sách chờ

- **Mã UC:** UC-F10.3
- **Tên Use Case:** Gửi thông báo được đưa vào lớp từ danh sách chờ
- **Tác nhân chính:** Hệ thống (kích hoạt khi có chỗ trống)
- **Tác nhân phụ (nếu có):** Dịch vụ gửi email; Hội viên (người nhận)
- **Mô tả ngắn gọn:** Khi có chỗ trống và một hội viên trong danh sách chờ được chuyển vào lớp, hệ thống báo cho hội viên đó.

#### A. Tiền điều kiện (Preconditions)
- Hội viên đang trong danh sách chờ của một lớp.
- Lớp có chỗ trống và hội viên đến lượt được chuyển vào.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Hội viên được ghi nhận vào lớp; một thông báo trong web được tạo và một email được gửi.
- **Thất bại:** Việc chuyển vào lớp không bị ảnh hưởng nếu chỉ riêng việc gửi thông báo lỗi.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Có chỗ trống trong lớp (ví dụ hội viên khác hủy chỗ).
2. Hệ thống chọn hội viên đến lượt trong danh sách chờ theo quy tắc danh sách chờ (xem ADR về danh sách chờ).
3. Hệ thống chuyển hội viên từ danh sách chờ vào lớp.
4. Hệ thống tạo thông báo trong web cho hội viên.
5. Hệ thống gửi email cho hội viên.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Hội viên không có email:** chỉ tạo thông báo trong web.
- **E1 – Gửi email thất bại:** hội viên vẫn ở trong lớp; thông báo trong web được tạo; email thử lại sau *(đề xuất)*.
- **E4 – Xung đột đồng thời (409):** hai chỗ trống xuất hiện cùng lúc; hệ thống xử lý theo cơ chế chống xung đột (NF6) để không đưa cùng một người vào hai lần.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F10.3:** Thứ tự và điều kiện chuyển từ danh sách chờ vào lớp theo ADR danh sách chờ, mục **[ghi số mục sau khi đối chiếu]**.
- **BR-F10.1:** Mỗi sự kiện tạo một thông báo trong web và một email.
- **[Yêu cầu phi chức năng]:**
  - Toàn vẹn đồng thời (NF6): không đưa cùng một hội viên vào lớp hai lần.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `[bảng thông báo]`, `[bảng danh sách chờ]`, `[bảng đăng ký lớp]`, `members`
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`)*

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** Không có; kích hoạt bởi nghiệp vụ lớp học (F8).

---

### 3.4. UC-F10.4: Gửi thông báo đổi giờ lớp

- **Mã UC:** UC-F10.4
- **Tên Use Case:** Gửi thông báo đổi giờ lớp
- **Tác nhân chính:** Hệ thống (kích hoạt khi lớp bị đổi giờ)
- **Tác nhân phụ (nếu có):** Dịch vụ gửi email; Hội viên (người nhận)
- **Mô tả ngắn gọn:** Khi giờ của lớp đã đặt bị thay đổi, hệ thống báo cho các hội viên có chỗ trong lớp đó.

#### A. Tiền điều kiện (Preconditions)
- Một lớp có hội viên đã đăng ký bị đổi giờ bởi người có quyền.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Mỗi hội viên có chỗ trong lớp nhận một thông báo trong web và một email, nêu rõ giờ cũ và giờ mới.
- **Thất bại:** Việc đổi giờ lớp không bị rollback chỉ vì gửi thông báo lỗi.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Người có quyền đổi giờ lớp và lưu thành công.
2. Hệ thống lấy danh sách hội viên có chỗ trong lớp đó.
3. Hệ thống tạo thông báo trong web cho từng hội viên, ghi giờ cũ và giờ mới.
4. Hệ thống gửi email cho từng hội viên.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Lớp chưa có hội viên nào:** không có thông báo nào được gửi.
- **A2 – Hội viên không có email:** chỉ tạo thông báo trong web.
- **E1 – Gửi email thất bại:** giờ lớp vẫn được lưu; thông báo trong web được tạo; email thử lại sau *(đề xuất)*.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F10.1:** Mỗi sự kiện tạo một thông báo trong web và một email.
- **BR-F10.4:** Thông báo phải nêu cả giờ cũ và giờ mới của lớp.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `[bảng thông báo]`, `[bảng lớp học]`, `[bảng đăng ký lớp]`, `members`
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`)*

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** Không có; kích hoạt bởi chức năng đổi giờ lớp (F8).

---

### 3.5. UC-F10.5: Xem thông báo trong web

- **Mã UC:** UC-F10.5
- **Tên Use Case:** Xem thông báo trong web
- **Tác nhân chính:** Hội viên
- **Tác nhân phụ (nếu có):** Không
- **Mô tả ngắn gọn:** Hội viên xem các thông báo đã nhận trong cổng hội viên.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Hội viên.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Hệ thống hiển thị danh sách thông báo của hội viên.
- **Thất bại:** Không có dữ liệu nào bị thay đổi.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Hội viên mở mục "Thông báo" trong cổng hội viên.
2. Hệ thống kiểm tra tham số phân trang (Bean Validation).
3. Hệ thống kiểm tra quyền và vai trò (S3); mã hội viên lấy từ token.
4. Hệ thống truy vấn thông báo của hội viên, mới nhất trước.
5. Hệ thống trả về kết quả (200 OK) dạng `PageResponse` gồm: loại, nội dung, thời điểm.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Chưa có thông báo nào:** trả danh sách rỗng; giao diện hiển thị "Bạn chưa có thông báo".
- **E1 – Dữ liệu đầu vào không hợp lệ (400 Bad Request):** tham số phân trang sai; trả `ProblemDetail` (400).
- **E2 – Không có quyền truy cập (403 Forbidden):** người thao tác không phải Hội viên; trả `ProblemDetail` (403).

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F10.5:** Hội viên chỉ xem được thông báo của chính mình (S3).
- **[Cần chốt với nhóm]:** có cần trạng thái "đã đọc / chưa đọc" trong Sprint 2 hay không.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động (chỉ đọc):** `[bảng thông báo]`, `members`
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`)*

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `GET /api/v1/me/notifications?page=0&size=20` *(đề xuất)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body:** Không có.
