# Đặc tả Use Case – F9: Huấn luyện viên

- **Mã chức năng:** F9
- **Version:** v2 (Bắt buộc)
- **Người biên soạn:** Nguyễn Trọng Việt
- **Người duyệt chéo:** Lê Phước Minh Quân
- **Nguồn yêu cầu:** US-F9.1 (#71), US-F9.2 (#72), US-F9.3 (#73), US-F9.4 (#74); quyết định ngày 03/11 (ADR 0005 mục 25–32, PR #82)
- **Tài liệu chuẩn tham chiếu:**
  - [Bản chốt phạm vi](../ban-chot-pham-vi.md)
  - [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml)
  - [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md)
  - [Quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md)
  - [ADR liên quan](../../decisions/)

---

## 1. Tổng quan chức năng

Chức năng huấn luyện viên (PT) hỗ trợ việc tập một kèm một giữa PT và hội viên:

- **Lịch:** PT khai báo khung giờ rảnh và xem lịch dạy; hội viên chỉ đặt buổi PT trong giờ PT rảnh, không trùng lịch.
- **Buổi tập:** đặt buổi chỉ giữ chỗ; khi PT xác nhận buổi đã diễn ra thì hệ thống mới trừ một buổi của gói PT.
- **Theo dõi:** PT soạn giáo án, ghi nhật ký tập và chỉ số cơ thể của học viên mình dạy; PT và hội viên xem tiến độ qua biểu đồ trên dữ liệu thực.

Vai trò liên quan: **PT** và **Hội viên**. Dữ liệu huấn luyện viên trỏ tới bảng `staff`. Không có gợi ý bằng AI. Phần tính lương và giao diện hoa hồng (F13) thuộc v3, không làm trong Sprint 2. Ở Sprint 1, tài liệu này chỉ đặc tả; phần hiện thực làm từ Sprint 2.

---

## 2. Danh sách Use Case

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F9.1** | Khai báo lịch rảnh và xem lịch dạy | PT | Cao | Sprint 2 |
| **UC-F9.2** | Đặt buổi PT | Hội viên | Cao | Sprint 2 |
| **UC-F9.3** | Xác nhận buổi PT đã diễn ra | PT | Cao | Sprint 2 |
| **UC-F9.4** | Soạn giáo án | PT | Trung bình | Sprint 2 |
| **UC-F9.5** | Ghi nhật ký tập và chỉ số cơ thể | PT | Trung bình | Sprint 2 |
| **UC-F9.6** | Xem tiến độ học viên | PT, Hội viên | Trung bình | Sprint 2 |

---

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F9.1: Khai báo lịch rảnh và xem lịch dạy

- **Mã UC:** UC-F9.1
- **Tên Use Case:** Khai báo lịch rảnh và xem lịch dạy
- **Tác nhân chính:** PT
- **Tác nhân phụ (nếu có):** Không
- **Mô tả ngắn gọn:** PT khai báo khung giờ rảnh để hội viên chọn thời gian hợp lệ, và xem các buổi PT, lớp nhóm được phân công cho mình.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò PT (có bản ghi `staff` tương ứng).

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Khung giờ rảnh được lưu cho PT đó; lịch dạy hiển thị các buổi PT và lớp được phân công.
- **Thất bại:** Không có khung giờ nào được lưu hoặc thay đổi (rollback giao dịch); lịch của PT khác không bị ảnh hưởng.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. PT mở mục "Lịch của tôi", chọn ngày, giờ bắt đầu, giờ kết thúc của khung rảnh.
2. Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào (Bean Validation), gồm giờ kết thúc sau giờ bắt đầu.
3. Hệ thống kiểm tra quyền và vai trò (S3); mã PT lấy từ token, PT chỉ thao tác trên lịch của chính mình.
4. Hệ thống lưu khung giờ rảnh trong một giao dịch (`@Transactional`).
5. Hệ thống trả về kết quả (201 Created).
6. Khi PT xem lịch, hệ thống trả khung giờ rảnh cùng các buổi PT và buổi lớp nhóm được phân công cho PT (200 OK).

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **E1 – Dữ liệu đầu vào không hợp lệ (400):**
  - Tại bước 2, giờ kết thúc không hợp lệ (không sau giờ bắt đầu), thiếu trường.
  - Hệ thống từ chối, trả `ProblemDetail` (400) kèm danh sách lỗi theo từng trường.
- **E2 – Không có quyền (403):**
  - Tại bước 3, người thao tác không phải PT, hoặc PT cố sửa lịch của PT khác.
  - Hệ thống từ chối, không sửa lịch của người khác; trả `ProblemDetail` (403).
- **E3 – Không tìm thấy (404):** khung giờ cần sửa không tồn tại.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F9.1:** PT chỉ khai báo, sửa và xem lịch rảnh của chính mình (S3).
- **BR-F9.2:** Hội viên chỉ đặt được buổi PT trong khung giờ PT đã khai báo rảnh (xem UC-F9.2).
- **[Cần chốt với nhóm]:** PT có được sửa hoặc xóa khung rảnh đã có hội viên đặt hay không; hai khung rảnh của cùng một PT có được chồng nhau hay không.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `trainer_availabilities`, `staff`; đọc `pt_sessions`, `class_sessions`
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`)*
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `trainer_availabilities` | `[...]` | `[...]` | `[...]` | PT sở hữu khung rảnh (trỏ `staff`) |
  | `trainer_availabilities` | `[...]` | `[...]` | `[...]` | Giờ bắt đầu, giờ kết thúc |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `POST /api/v1/pt/availabilities`, `GET /api/v1/pt/schedule` *(đề xuất, thống nhất với người làm API)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body (DTO):**
  ```json
  {
    "startTime": "2026-11-10T08:00:00Z",
    "endTime": "2026-11-10T12:00:00Z"
  }
  ```

---

### 3.2. UC-F9.2: Đặt buổi PT

- **Mã UC:** UC-F9.2
- **Tên Use Case:** Đặt buổi PT
- **Tác nhân chính:** Hội viên
- **Tác nhân phụ (nếu có):** PT (nhận buổi tập)
- **Mô tả ngắn gọn:** Hội viên đặt buổi một kèm một trong giờ PT rảnh bằng gói PT hợp lệ; hệ thống giữ buổi nhưng chưa trừ số buổi của gói.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò Hội viên.
- Hội viên có gói PT ở trạng thái ACTIVE.
- PT được chọn rảnh trong khung giờ đó, thời điểm bắt đầu cách hiện tại ít nhất `PT_MIN_LEAD_HOURS` giờ (mặc định 12).

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Một buổi PT được tạo và giữ chỗ trong `pt_sessions`. **Số buổi của gói chưa bị trừ.**
- **Thất bại:** Không tạo buổi nào; không thay đổi gói (rollback giao dịch).

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Hội viên chọn PT, ngày và giờ trong khung rảnh, rồi xác nhận đặt.
2. Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào (Bean Validation).
3. Hệ thống kiểm tra quyền và vai trò (S3); mã hội viên lấy từ token.
4. Hệ thống kiểm tra hội viên có gói PT ACTIVE.
5. Hệ thống kiểm tra khung giờ nằm trong giờ PT rảnh.
6. Hệ thống kiểm tra thời điểm bắt đầu còn ít nhất `PT_MIN_LEAD_HOURS` giờ.
7. Hệ thống kiểm tra không trùng lịch của PT (kể cả giờ PT dạy lớp nhóm) và không trùng lịch của hội viên.
8. Hệ thống kiểm tra số buổi PT hội viên đã đặt mà chưa diễn ra **ít hơn** số buổi còn lại của gói.
9. Hệ thống khóa bản ghi theo thứ tự `members` rồi `staff` (NF6), tạo buổi PT trong một giao dịch (`@Transactional`).
10. Hệ thống trả về kết quả (201 Created) kèm thông tin buổi PT.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **E1 – Dữ liệu đầu vào không hợp lệ (400):** thiếu PT hoặc giờ, sai định dạng; trả `ProblemDetail` (400).
- **E2 – Không có quyền (403):** người thao tác không phải Hội viên.
- **E3 – Không tìm thấy (404):** PT hoặc khung giờ không tồn tại.
- **E4 – Xung đột đồng thời (409):** hai yêu cầu cùng lúc cho cùng khung giờ của PT hoặc của hội viên; khóa NF6 chỉ nhận yêu cầu không trùng, yêu cầu còn lại bị từ chối.
- **E5 – Vi phạm quy tắc nghiệp vụ (422):** hệ thống từ chối kèm lý do bằng tiếng Việt, không tạo buổi, trong các trường hợp:
  - Hội viên chỉ có gói kiểu TIME hoặc SESSION, không có gói PT ACTIVE.
  - Giờ đặt nằm ngoài giờ PT rảnh.
  - Đặt sát giờ, chưa đủ `PT_MIN_LEAD_HOURS` giờ.
  - Trùng lịch với buổi PT hoặc lớp nhóm của PT, hoặc với lịch của hội viên.
  - Đã đặt đủ số buổi còn lại của gói. Ví dụ: gói còn 2 buổi và hội viên đã đặt 2 buổi chưa diễn ra thì từ chối buổi thứ ba.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F9.2:** Chỉ đặt trong giờ PT rảnh.
- **BR-F9.3:** Không trùng lịch cả PT lẫn hội viên, kể cả giờ PT dạy lớp nhóm.
- **BR-F9.4:** Số buổi PT đã đặt mà chưa diễn ra phải ít hơn số buổi còn lại của gói.
- **BR-F9.5:** Đặt buổi chỉ giữ chỗ, không trừ số buổi của gói.
- **CONFIG-F9.1:** Đọc từ bảng `system_configs` với key `PT_MIN_LEAD_HOURS` (giá trị mặc định: `12`), không viết cứng trong mã nguồn (Quy tắc số 15).
- **[Yêu cầu phi chức năng]:**
  - Toàn vẹn đồng thời (NF6): khóa `members` rồi `staff`, chống trùng lịch kể cả với `class_sessions`.
  - Bảo mật (S2, S3): kiểm tra JWT, phân quyền ở backend.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `pt_sessions`; đọc `trainer_availabilities`, `memberships`, `staff`, `members`, `class_sessions`, `system_configs`
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`)*
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `pt_sessions` | `[...]` | `[...]` | `[...]` | Giờ bắt đầu, giờ kết thúc |
  | `pt_sessions` | `[...]` | `[...]` | `[...]` | Trạng thái buổi (đã đặt / DONE) |
  | `memberships` | `[...]` | `[...]` | `[...]` | Trạng thái gói (ACTIVE) và số buổi còn lại |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `POST /api/v1/pt-sessions` *(đề xuất)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body (DTO):**
  ```json
  {
    "ptId": 1,
    "startTime": "2026-11-10T08:00:00Z"
  }
  ```

---

### 3.3. UC-F9.3: Xác nhận buổi PT đã diễn ra

- **Mã UC:** UC-F9.3
- **Tên Use Case:** Xác nhận buổi PT đã diễn ra
- **Tác nhân chính:** PT
- **Tác nhân phụ (nếu có):** Hội viên (gói PT bị trừ buổi)
- **Mô tả ngắn gọn:** PT xác nhận buổi một kèm một đã hoàn thành để hệ thống ghi nhận, trừ một buổi của gói và chốt hoa hồng.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò PT.
- Buổi PT thuộc về PT đó (đúng phân công), đã diễn ra và chưa được xác nhận.
- Gói PT của hội viên hợp lệ.
- **[Cần chốt với nhóm]:** hệ thống có chặn xác nhận trước giờ bắt đầu của buổi hay không.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Buổi chuyển sang DONE; số buổi của gói giảm 1; `commission_amount` được chốt. Nếu là buổi cuối của gói thì gói chuyển EXPIRED và gói PT nối tiếp (UPCOMING) được kích hoạt (xem bước 6).
- **Thất bại:** Trạng thái buổi và gói giữ nguyên, không trừ buổi (rollback giao dịch).

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. PT mở danh sách buổi của mình và chọn buổi đã diễn ra để xác nhận.
2. Hệ thống kiểm tra quyền và vai trò (S3): buổi phải thuộc PT này.
3. Hệ thống kiểm tra buổi chưa ở trạng thái DONE (không xác nhận lần hai) và gói hợp lệ.
4. Hệ thống ghi trạng thái DONE và trừ một buổi của gói trong một giao dịch (`@Transactional`).
5. Hệ thống chốt `commission_amount` theo ERD (xem mục E); sau khi chốt, giá hoặc tỷ lệ thay đổi về sau không làm đổi số đã chốt.
6. Nếu đây là buổi cuối của gói: hệ thống **ghi và flush** gói hiện tại sang EXPIRED trước, rồi dời gói PT UPCOMING nối tiếp về bắt đầu hôm nay và chuyển ACTIVE, giữ nguyên số ngày hiệu lực, trong cùng giao dịch.
7. Hệ thống trả về kết quả (200 OK).

*Ví dụ (từ US-F9.3): gói PT A còn 1 buổi; gói PT B (60 ngày) đã gia hạn, đang UPCOMING từ 01/12. PT xác nhận buổi cuối của A ngày 03/11 thì A chuyển EXPIRED; B dời về 03/11 đến 01/01 và ACTIVE ngay; hội viên đặt được buổi PT mới bằng B.*

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **E2 – Không có quyền (403):** buổi không thuộc PT này (sai PT bị từ chối).
- **E3 – Không tìm thấy (404):** buổi PT không tồn tại.
- **E4 – Xung đột đồng thời (409):** hai thao tác cùng lúc trên một buổi (`@Version`); chỉ một thao tác được ghi, buổi không bị trừ hai lần.
- **E5 – Vi phạm quy tắc nghiệp vụ (422):** buổi đã được xác nhận (không trừ lần hai) hoặc buổi chỉ mới được đặt, chưa đủ điều kiện xác nhận. *(Mã lỗi cụ thể cần đối chiếu với nhóm.)*

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F9.6:** Chỉ trừ gói PT **khi PT xác nhận buổi đã diễn ra**; chỉ đặt buổi thì không trừ; mỗi buổi chỉ trừ một lần.
- **BR-F9.7:** Xác nhận buổi cuối thì gói PT chuyển EXPIRED ngay (ghi và flush trước), rồi gói PT UPCOMING nối tiếp được dời về hôm nay và ACTIVE, giữ nguyên số ngày hiệu lực, trong cùng giao dịch.
- **BR-F9.8:** `commission_amount` có giá trị khi và chỉ khi buổi ở trạng thái DONE; bằng tỷ lệ hoa hồng × (giá gói sau giảm / số buổi), làm tròn VND và chốt lúc xác nhận, không đổi về sau.
- **BR-F9.9:** Chỉ PT được phân công cho buổi đó mới được xác nhận (S3).
- **Ngoài phạm vi Sprint 2:** tính lương và giao diện hoa hồng (F13) thuộc v3.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `pt_sessions`, `memberships`
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`)*
  | Tên bảng | Tên cột | Kiểu dữ liệu | Ràng buộc | Ý nghĩa / Ghi chú |
  | --- | --- | --- | --- | --- |
  | `pt_sessions` | `[...]` | `[...]` | `[...]` | Trạng thái buổi (DONE) |
  | `pt_sessions` | `commission_amount` | `[...]` | `[...]` | Hoa hồng, chỉ có giá trị khi DONE |
  | `memberships` | `[...]` | `[...]` | `[...]` | Trạng thái gói, số buổi còn lại, ngày hiệu lực |

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `PATCH /api/v1/pt-sessions/{id}/confirm` *(đề xuất)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body:** Không có.

---

### 3.4. UC-F9.4: Soạn giáo án

- **Mã UC:** UC-F9.4
- **Tên Use Case:** Soạn giáo án
- **Tác nhân chính:** PT
- **Tác nhân phụ (nếu có):** Hội viên (học viên)
- **Mô tả ngắn gọn:** PT soạn giáo án tập luyện cho học viên mình dạy.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò PT.
- PT có quan hệ dạy với hội viên: hội viên có buổi PT hoặc lớp do PT đó dạy.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Giáo án được lưu đúng học viên.
- **Thất bại:** Không lưu giáo án nào.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. PT chọn học viên của mình và mở mục "Giáo án".
2. PT nhập nội dung giáo án.
3. Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào.
4. Hệ thống kiểm tra quyền (S3) và quan hệ dạy giữa PT và hội viên.
5. Hệ thống lưu giáo án trong một giao dịch.
6. Hệ thống trả về kết quả (201 Created).

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **E1 – Dữ liệu đầu vào không hợp lệ (400):** thiếu nội dung hoặc sai định dạng.
- **E2 – Không có quyền (403):** PT không dạy hội viên này; backend từ chối theo S3.
- **E3 – Không tìm thấy (404):** hội viên không tồn tại.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F9.10:** PT chỉ xem và ghi dữ liệu cho học viên có buổi PT hoặc lớp do mình dạy (S3).
- **[Cần chốt với nhóm]:** một hội viên có một giáo án hiện hành hay nhiều giáo án theo thời gian.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `workout_plans`; đọc `members`, `pt_sessions`, `class_sessions`
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`, cần đối chiếu tên bảng khi triển khai)*

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `POST /api/v1/pt/members/{memberId}/workout-plans` *(đề xuất)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body (DTO):**
  ```json
  {
    "content": "..."
  }
  ```

---

### 3.5. UC-F9.5: Ghi nhật ký tập và chỉ số cơ thể

- **Mã UC:** UC-F9.5
- **Tên Use Case:** Ghi nhật ký tập và chỉ số cơ thể
- **Tác nhân chính:** PT
- **Tác nhân phụ (nếu có):** Hội viên (học viên)
- **Mô tả ngắn gọn:** PT ghi nhật ký tập và chỉ số cơ thể của học viên mình dạy để làm dữ liệu cho biểu đồ tiến độ.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập với vai trò PT.
- PT có quan hệ dạy với hội viên (có buổi PT hoặc lớp do PT dạy).

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Nhật ký và chỉ số được lưu đúng học viên và hiển thị trong tiến độ trên dữ liệu thực.
- **Thất bại:** Không lưu dữ liệu nào.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. PT chọn học viên và mở mục "Nhật ký" hoặc "Chỉ số cơ thể".
2. PT nhập nội dung nhật ký tập và các chỉ số cơ thể **[cần chốt: danh sách chỉ số, ví dụ cân nặng]**.
3. Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào.
4. Hệ thống kiểm tra quyền (S3) và quan hệ dạy giữa PT và hội viên.
5. Hệ thống lưu dữ liệu trong một giao dịch.
6. Hệ thống trả về kết quả (201 Created).

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **E1 – Dữ liệu đầu vào không hợp lệ (400):** thiếu nội dung, giá trị chỉ số sai hoặc ngoài khoảng cho phép.
- **E2 – Không có quyền (403):** PT không dạy hội viên này; backend từ chối theo S3.
- **E3 – Không tìm thấy (404):** hội viên không tồn tại.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F9.10:** PT chỉ ghi dữ liệu cho học viên mình dạy (S3).
- **BR-F9.11:** Nhật ký và chỉ số là dữ liệu đầu vào của biểu đồ tiến độ (UC-F9.6).
- **[Cần chốt với nhóm]:** quan hệ với chỉ số cơ thể hội viên tự nhập ở F2, nếu có.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động:** `workout_logs`, `body_metrics`; đọc `members`
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`)*

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `POST /api/v1/pt/members/{memberId}/workout-logs`, `POST /api/v1/pt/members/{memberId}/body-metrics` *(đề xuất)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body:** *(điền theo danh sách chỉ số sau khi chốt)*

---

### 3.6. UC-F9.6: Xem tiến độ học viên

- **Mã UC:** UC-F9.6
- **Tên Use Case:** Xem tiến độ học viên
- **Tác nhân chính:** PT, Hội viên
- **Tác nhân phụ (nếu có):** Không
- **Mô tả ngắn gọn:** Xem biểu đồ tiến độ dựa trên nhật ký tập và chỉ số cơ thể thực tế, không có gợi ý bằng AI.

#### A. Tiền điều kiện (Preconditions)
- Người dùng đã đăng nhập.
- Hội viên xem tiến độ của chính mình; PT xem tiến độ của học viên có buổi PT hoặc lớp do mình dạy.

#### B. Hậu điều kiện (Postconditions)
- **Thành công:** Hệ thống hiển thị biểu đồ tiến độ. Không có dữ liệu nào bị thay đổi.
- **Thất bại:** Không có dữ liệu nào bị thay đổi.

#### C. Luồng sự kiện chính (Basic Flow / Happy Path)
1. Người dùng mở mục "Tiến độ" và chọn khoảng thời gian.
2. Hệ thống kiểm tra tính hợp lệ của khoảng thời gian.
3. Hệ thống kiểm tra quyền (S3): hội viên chỉ xem của mình; PT chỉ xem của học viên mình dạy.
4. Hệ thống tổng hợp nhật ký tập và chỉ số cơ thể theo thời gian.
5. Hệ thống trả về kết quả (200 OK) gồm các điểm dữ liệu để vẽ biểu đồ.

#### D. Các luồng thay thế và ngoại lệ (Alternative / Exception Flows)
- **A1 – Chưa có dữ liệu:** trả dữ liệu rỗng; giao diện hiển thị "Chưa có dữ liệu tiến độ".
- **E1 – Dữ liệu đầu vào không hợp lệ (400):** khoảng thời gian sai.
- **E2 – Không có quyền (403):** hội viên yêu cầu tiến độ của người khác, hoặc PT xem học viên mình không dạy; backend từ chối theo S3.

#### E. Quy tắc nghiệp vụ liên quan (Business Rules)
- **BR-F9.12:** Hội viên chỉ xem được tiến độ cá nhân của chính mình (S3).
- **BR-F9.10:** PT chỉ xem được học viên có buổi PT hoặc lớp do mình dạy (S3).
- **BR-F9.13:** Biểu đồ chỉ dựa trên dữ liệu thực; không có gợi ý bằng AI.
- **[Cần chốt với nhóm]:** các chỉ số được vẽ trên biểu đồ.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan
- **Bảng tác động (chỉ đọc):** `workout_logs`, `body_metrics`, `members`
- **Chi tiết trường dữ liệu:** *(điền từ `erd-v2.dbml`)*

#### G. Thiết kế giao diện và API liên quan
- **API Endpoint:** `GET /api/v1/members/{memberId}/progress?from=...&to=...` *(đề xuất)*
- **Header:** `Authorization: Bearer <access_token>`
- **Request Body:** Không có.
