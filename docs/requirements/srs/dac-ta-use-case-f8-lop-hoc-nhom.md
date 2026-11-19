# Đặc tả Use Case – F8: Lớp học nhóm

- **Mã chức năng:** F8
- **Version:** v2 (Bắt buộc)
- **Người biên soạn:** Phạm Gia Bảo
- **Người duyệt chéo:** Nguyễn Anh Tú
- **Nguồn chuẩn:** [Bản chốt phạm vi](../ban-chot-pham-vi.md), [ERD v2](../../design/erd/erd-v2.dbml), [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md), [ADR 0014](../../decisions/0014-bo-danh-sach-cho-lop-hoc-nhom.md).
- **Truy vết:** [Mẫu đặc tả](mau-dac-ta-use-case.md), [ADR 0012](../../decisions/0012-quy-uoc-ma-truy-vet.md), [ma trận truy vết](../../testing/ma-tran-truy-vet.md).

## 1. Tổng quan chức năng

Khách xem lịch công khai. Hội viên có gói TIME phù hợp đặt và hủy chỗ, lễ tân được thao tác hộ trong chi nhánh. Lớp đầy thì từ chối đặt. Quản lý chi nhánh quản lý lịch lặp và buổi học, sửa điểm danh sau thời hạn của huấn luyện viên và gỡ khóa đặt lớp. Chủ chuỗi được đổi phòng, đổi giờ và hủy buổi riêng lẻ ở mọi chi nhánh. Huấn luyện viên xem và điểm danh buổi được phân công. Hệ thống sinh buổi từ lịch lặp và phát sự kiện thay đổi buổi cho F10.

[Sơ đồ hoạt động đặt và hủy chỗ](../../design/activity/so-do-hoat-dong-dat-cho-lop.drawio.svg) · [Sơ đồ use case hội viên](../../design/use-case/use-case-hoi-vien.drawio.svg)

## 2. Danh sách Use Case

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint |
| --- | --- | --- | --- | --- |
| **UC-F8.0** | Xem lịch lớp học nhóm công khai | Khách | Cao | Sprint 2 |
| **UC-F8.1** | Đặt chỗ lớp nhóm | Hội viên, lễ tân | Cao | Sprint 2 |
| **UC-F8.2** | Đã bỏ (ADR 0014) | — | — | — |
| **UC-F8.3** | Hủy chỗ lớp nhóm | Hội viên, lễ tân | Cao | Sprint 2 |
| **UC-F8.4** | Đã bỏ (ADR 0014) | — | — | — |
| **UC-F8.5** | Xem lịch dạy lớp nhóm | Huấn luyện viên | Trung bình | Sprint 2 |
| **UC-F8.6** | Điểm danh học viên lớp học nhóm | Huấn luyện viên, quản lý chi nhánh | Cao | Sprint 2 |
| **UC-F8.7** | Quản lý lịch lặp lớp học và phân công huấn luyện viên | Quản lý chi nhánh, chủ chuỗi với buổi riêng lẻ | Cao | Sprint 2 |
| **UC-F8.8** | Sinh buổi học từ lịch lặp | Hệ thống | Cao | Sprint 2 |

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F8.0: Xem lịch lớp học nhóm công khai

#### A. Tiền điều kiện

Khách truy cập được trang công khai mà không cần phiên đăng nhập.

#### B. Hậu điều kiện

Trả các buổi công khai phù hợp hoặc danh sách rỗng. Dữ liệu lưu giữ nguyên.

#### C. Luồng sự kiện chính

1. Khách mở lịch lớp và chọn bộ lọc được cung cấp.
2. Hệ thống xác thực bộ lọc, truy vấn các buổi công khai và trả loại lớp, thời gian, phòng, huấn luyện viên và trạng thái công khai của buổi.
3. Giao diện hiển thị lịch theo giờ Việt Nam và dẫn tới đăng nhập hoặc đăng ký nếu khách muốn đặt.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Không có buổi phù hợp:** Trả danh sách rỗng và hiển thị trạng thái trống.
- **E1 – Bộ lọc sai định dạng (400):** Trả lỗi theo trường.
- **E2 – Chi nhánh không tồn tại (404):** Trả lỗi nhận diện chi nhánh.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.0-01:** Lịch lớp công khai không yêu cầu đăng nhập. Cửa sổ đặt chỗ chỉ áp dụng cho UC-F8.1.
- **BR-F8.0-02:** Chỉ trả thông tin lịch công khai, không trả danh tính hoặc dữ liệu chỗ đặt của hội viên.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

Đọc `class_sessions`, `class_schedules`, `class_types`, `branches`, `rooms`, `staff`.

#### G. Thiết kế giao diện và API liên quan

[Wireframe lịch lớp công khai](../../design/wireframe/lich-lop-cong-khai.drawio.svg). API đề xuất `GET /api/v1/public/class-sessions`; bộ lọc và dữ liệu chỗ trống xem mục 7.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F8.0.1: Xem lịch lớp công khai

- **User story:** Là **Khách**, tôi muốn **xem lịch lớp nhóm**, để **chọn lớp trước khi đăng ký**.
- **BR liên quan:** BR-F8.0-01, BR-F8.0-02
- **Version / Sprint:** v2 / Sprint 2
- **Issue:** #155
- **Tiêu chí chấp nhận:**
  - **AC-F8.0.1-01: Xem lịch không cần đăng nhập** (BR-F8.0-01, BR-F8.0-02)
    - **Given** Có buổi đã công bố trong khoảng ngày và chi nhánh đã chọn
    - **When** Khách xem lịch mà không có access token
    - **Then** Trả 200 với lịch phù hợp, gồm loại lớp, giờ Việt Nam, phòng, huấn luyện viên và trạng thái công khai; không trả người đặt hay dữ liệu chỗ đặt.
  - **AC-F8.0.1-02: Lịch rỗng** (BR-F8.0-01)
    - **Given** Không có buổi công khai trong khoảng ngày hợp lệ
    - **When** Khách xem lịch
    - **Then** Trả 200 với danh sách rỗng và hiển thị trạng thái trống.
  - **AC-F8.0.1-03: Bộ lọc không hợp lệ** (BR-F8.0-01)
    - **Given** Ngày sai định dạng hoặc chi nhánh không tồn tại
    - **When** Khách xem lịch
    - **Then** Trả ProblemDetail 400 nếu sai định dạng hoặc 404 nếu chi nhánh không tồn tại.
  - **AC-F8.0.1-04: Khách muốn đặt** (BR-F8.0-01)
    - **Given** Khách chưa đăng nhập
    - **When** Khách chọn đặt một buổi
    - **Then** Giao diện dẫn đến đăng nhập hoặc đăng ký; API đặt chỗ yêu cầu xác thực và không tạo chỗ.

### 3.2. UC-F8.1: Đặt chỗ lớp nhóm

#### A. Tiền điều kiện

Hội viên đã đăng nhập và đặt cho chính mình, hoặc lễ tân thao tác hộ trong phạm vi chi nhánh. Buổi học còn nhận đặt.

#### B. Hậu điều kiện

Thành công tạo một chỗ `BOOKED` và ghi người đặt. Khi từ chối, dữ liệu chỗ đặt và sĩ số giữ nguyên.

#### C. Luồng sự kiện chính

1. Hội viên chọn buổi hoặc lễ tân chọn buổi và hội viên cần đặt hộ.
2. Hệ thống kiểm tra quyền, khóa dòng `members` trước và `class_sessions` sau. Sau khi lấy khóa, hệ thống kiểm tra gói, khóa đặt lớp, cửa sổ đặt, chỗ hiện có, trùng giờ và sĩ số.
3. Hệ thống tạo chỗ `BOOKED` và trả kết quả. `created_by` lưu `users.id` của tài khoản thực hiện.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Đặt lại:** Dòng đã `CANCELLED` không cản đặt lại. Yêu cầu mới qua đủ các kiểm tra ở bước 2.
- **E1 – Sai đầu vào hoặc chưa đăng nhập (400/401):** Từ chối trước khi ghi.
- **E2 – Sai quyền hoặc phạm vi (403):** Hội viên thao tác cho người khác hoặc lễ tân thao tác ngoài phạm vi.
- **E3 – Thiếu buổi hoặc hội viên (404):** Nêu tài nguyên không tìm thấy.
- **E4 – Trùng chỗ hoặc trùng giờ (409):** Nêu xung đột, kể cả khi hai yêu cầu chạy đồng thời.
- **E5 – Vi phạm điều kiện đặt (422):** Nêu lý do về gói, khóa đặt, cửa sổ hoặc trạng thái buổi.
- **E6 – Lớp đã đủ chỗ:** Từ chối và báo “Lớp đã đủ chỗ”. Không tạo dòng đặt chỗ.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.1-01:** Chỉ gói `TIME` `ACTIVE` dùng được tại chi nhánh của buổi học. Gói `SESSION` và `PT` không đủ điều kiện.
- **BR-F8.1-02:** Cửa sổ `BOOKING_WINDOW_DAYS` bắt đầu từ hôm nay đến hết ngày hôm nay cộng số ngày cấu hình theo giờ Việt Nam. Chỉ đóng khi thời gian còn lại **dưới** `BOOKING_CUTOFF_MIN`.
- **BR-F8.1-03:** Sĩ số tính các chỗ `BOOKED`, `ATTENDED`, `NO_SHOW`. Nếu đã đạt `class_sessions.capacity`, yêu cầu bị từ chối. Kiểm tra lại sĩ số sau khi khóa theo NF6.
- **BR-F8.1-04:** Cấm đặt trùng buổi hoặc trùng giờ. Chỗ `CANCELLED` không cản đặt lại. Kiểm tra lại lịch sau khi khóa theo NF6.
- **BR-F8.1-05**, **BR-F8.1-06:** Đã bỏ (ADR 0014).
- **BR-F8-01:** Hội viên tự đặt hoặc lễ tân đặt hộ theo cùng điều kiện. Ghi `created_by` là tài khoản bấm đặt theo [ERD v2](../../design/erd/erd-v2.dbml), kể cả khi đặt hộ.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

Ghi `class_bookings`. Đọc `members`, `memberships`, `packages`, `class_sessions`, `system_configs` theo [ERD v2](../../design/erd/erd-v2.dbml).

#### G. Thiết kế giao diện và API liên quan

Lịch lớp của hội viên và thao tác đặt hộ tại quầy. [PR API #217](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/217) dùng `POST /api/v1/class-sessions/{sessionId}/bookings` với `memberId`. Backend đối chiếu hội viên tự đặt với danh tính trong JWT, kiểm tra chi nhánh khi lễ tân đặt hộ và ghi người tạo từ JWT.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F8.1.1: Đặt chỗ lớp nhóm

- **User story:** Là **Hội viên có gói TIME**, tôi muốn **đặt chỗ khi lớp còn chỗ**, để **giữ chỗ tập**.
- **BR liên quan:** BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8-01, BR-F8.3-02, BR-F8.1-04
- **Version / Sprint:** v2 / Sprint 2
- **Issue:** #156
- **Tiêu chí chấp nhận:**
  - **AC-F8.1.1-01: Đặt khi còn chỗ và ghi người tạo** (BR-F8.1-01, BR-F8.1-02, BR-F8.1-03, BR-F8-01)
    - **Given** Buổi còn chỗ, hội viên có TIME ACTIVE dùng được tại chi nhánh, không bị khóa hoặc trùng giờ và còn trong cửa sổ đặt
    - **When** Hội viên tự đặt hoặc lễ tân đúng chi nhánh đặt hộ
    - **Then** Tạo một chỗ BOOKED, ghi created_by là users.id của người bấm đặt.
  - **AC-F8.1.1-02: Gói hoặc quyền đặt không hợp lệ** (BR-F8.1-01, BR-F8.3-02)
    - **Given** Hội viên chỉ có SESSION/PT, TIME hết hiệu lực hoặc TIME không dùng được tại chi nhánh, hoặc đang bị khóa
    - **When** Hội viên hoặc lễ tân gửi yêu cầu đặt
    - **Then** Từ chối và báo cụ thể lý do; không ghi chỗ.
  - **AC-F8.1.1-03: Lớp đủ chỗ** (BR-F8.1-03, BR-F8-01)
    - **Given** Số chỗ chiếm đã bằng sức chứa
    - **When** Hội viên hoặc lễ tân đặt hộ
    - **Then** Từ chối với “Lớp đã đủ chỗ”; không tạo bản ghi mới.
  - **AC-F8.1.1-04: Hai yêu cầu tranh chỗ cuối** (BR-F8.1-03)
    - **Given** Buổi còn một chỗ và hai hội viên đều đủ điều kiện
    - **When** Hai yêu cầu đặt cạnh tranh
    - **Then** Chỉ một yêu cầu tạo BOOKED; sĩ số không vượt mức và yêu cầu còn lại bị từ chối.
  - **AC-F8.1.1-05: Trùng buổi và trùng giờ** (BR-F8.1-04)
    - **Given** Hội viên đã đặt cùng buổi, đã đặt một buổi khác bị chồng giờ hoặc có hai yêu cầu đồng thời cho hai buổi chồng giờ
    - **When** Gửi yêu cầu đặt
    - **Then** Không tạo chỗ trùng; hai yêu cầu cạnh tranh chỉ tạo tối đa một chỗ.
  - **AC-F8.1.1-06: Đặt lại sau khi hủy** (BR-F8.1-04)
    - **Given** Đã hủy buổi đó, buổi vẫn còn chỗ và hội viên thỏa điều kiện
    - **When** Hội viên đặt lại
    - **Then** Tạo BOOKED mới và bảo toàn lịch sử CANCELLED.
  - **AC-F8.1.1-07: Biên cửa sổ theo ngày Việt Nam** (BR-F8.1-02)
    - **Given** BOOKING_WINDOW_DAYS = 7, hiện tại 10:00 ngày 04/11 giờ Việt Nam
    - **When** Hội viên đủ điều kiện thử đặt buổi 18:00 ngày 11/11 và buổi ngày 12/11
    - **Then** Buổi 11/11 vẫn trong cửa sổ dù cách hơn 168 giờ. Buổi 12/11 bị từ chối.
  - **AC-F8.1.1-08: Đúng và dưới mốc đóng đặt** (BR-F8.1-02, BR-F8-01)
    - **Given** BOOKING_CUTOFF_MIN = 30. Hội viên đủ điều kiện và buổi còn chỗ
    - **When** Thử đặt khi còn đúng 30 phút, rồi thử yêu cầu khác khi còn 29 phút, với cả lớp còn chỗ và lớp đầy
    - **Then** Đúng 30 phút vẫn được xét. Dưới 30 phút mọi yêu cầu đặt mới bị từ chối.
  - **AC-F8.1.1-09: Phạm vi đặt hộ** (BR-F8-01)
    - **Given** Hội viên thử đặt cho người khác hoặc lễ tân thao tác ngoài chi nhánh được phép
    - **When** Gửi yêu cầu
    - **Then** Từ chối và không tạo chỗ.
  - **AC-F8.1.1-10: Cấu hình theo chi nhánh** (BR-F8.1-02)
    - **Given** Chi nhánh đặt mốc đóng khác giá trị toàn chuỗi
    - **When** Hội viên yêu cầu đặt sát giờ
    - **Then** Dùng mốc của chi nhánh; chi nhánh không có giá trị riêng dùng mốc toàn chuỗi.

### 3.3. UC-F8.2: Đã bỏ (ADR 0014)

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.2-01**, **BR-F8.2-02**, **BR-F8.2-03**, **BR-F8.2-04**, **BR-F8.2-05:** Đã bỏ (ADR 0014).

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F8.2.1: Đã bỏ (ADR 0014)

- **Issue:** #157
- Không có AC áp dụng. Các kịch bản cũ thuộc #67.

### 3.4. UC-F8.3: Hủy chỗ lớp nhóm

#### A. Tiền điều kiện

Chỗ đang `BOOKED`. Hội viên có quyền hủy chỗ của mình hoặc lễ tân có quyền thao tác hộ.

#### B. Hậu điều kiện

Chỗ chuyển `CANCELLED` và được trả lại ngay. Hủy muộn được ghi nhận và có thể dẫn đến khóa đặt lớp. Yêu cầu bị từ chối không đổi dữ liệu hay số vi phạm.

#### C. Luồng sự kiện chính

1. Hội viên hoặc lễ tân yêu cầu hủy.
2. Hệ thống kiểm tra quyền và đọc lại trạng thái chỗ cùng thời điểm buổi học trong giao dịch.
3. Hệ thống xác định có hủy muộn hay không, lưu thời điểm và lý do hủy, giải phóng chỗ.
4. Nếu có vi phạm mới, hệ thống đếm theo BR-F8.3-02 và lưu kết quả trong cùng giao dịch.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Hủy sớm hoặc buổi đã đổi giờ:** Lưu `is_late_cancel = false`.
- **A2 – Hủy do chi nhánh hủy buổi:** Xem UC-F8.7. Hội viên không bị tính vi phạm.
- **E1 – Sai đầu vào hoặc chưa đăng nhập (400/401):** Không ghi dữ liệu.
- **E2 – Sai quyền hoặc không tìm được chỗ (403/404):** Từ chối thao tác.
- **E3 – Chỗ đã kết thúc, bị hủy hoặc không thể hủy (422):** Giữ dữ liệu hiện tại.
- **E4 – Xung đột đồng thời (409):** Yêu cầu thua xung đột không ghi thêm lượt hủy hay vi phạm.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.3-01:** Hủy trước giờ học ít nhất `LATE_CANCEL_HOURS` thì miễn phạt. Nếu buổi đã đổi giờ (`original_start_at` khác rỗng), mọi lần hủy chỗ hợp lệ đều miễn phạt.
- **BR-F8.3-02:** Đếm hủy muộn và `NO_SHOW` trong tháng dương lịch Việt Nam, chỉ tính vi phạm sau lần khóa gần nhất. Khi đủ `PENALTY_LIMIT_PER_MONTH`, khóa đặt lớp `PENALTY_LOCK_DAYS`. Không dùng lại vi phạm đã dẫn đến lần khóa trước.
- **BR-F8.3-03:** Đã bỏ (ADR 0014).
- **BR-F8.3-04:** Hủy chỗ trả lại suất ngay. Yêu cầu đặt sau đó vẫn phải đáp ứng mọi điều kiện của UC-F8.1.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

`class_bookings`, `class_sessions`, `members.booking_locked_until`, `system_configs` và dữ liệu lưu mốc khóa gần nhất. Lý do hủy chủ động là `BY_MEMBER` hoặc `BY_STAFF` theo người thao tác.

#### G. Thiết kế giao diện và API liên quan

Thao tác Hủy chỗ trong lịch đã đặt của hội viên hoặc tại quầy. [PR API #217](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/217) dùng `PUT /api/v1/class-bookings/{bookingId}/cancel`.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F8.3.1: Hủy chỗ lớp nhóm

- **User story:** Là **Hội viên**, tôi muốn **hủy chỗ đã đặt**, để **nhường chỗ khi không đi được**.
- **BR liên quan:** BR-F8.3-01, BR-F8.3-04, BR-F8.3-02, BR-F8-03, BR-F8.1-03, BR-F8.7-05
- **Version / Sprint:** v2 / Sprint 2
- **Issue:** #158
- **Tiêu chí chấp nhận:**
  - **AC-F8.3.1-01: Hủy sớm hoặc đúng biên** (BR-F8.3-01, BR-F8.3-04)
    - **Given** Có chỗ BOOKED với LATE_CANCEL_HOURS = 2 và còn đúng 2 giờ hoặc lâu hơn
    - **When** Hội viên hoặc lễ tân đúng quyền hủy
    - **Then** Chuyển CANCELLED, is_late_cancel = false và suất được trả ngay.
  - **AC-F8.3.1-02: Hủy muộn đạt ngưỡng khóa** (BR-F8.3-01, BR-F8.3-02)
    - **Given** Buổi chưa đổi giờ, còn dưới 2 giờ và hội viên đã có hai vi phạm hợp lệ trong tháng với ngưỡng mặc định 3
    - **When** Hội viên hủy chỗ
    - **Then** Ghi hủy muộn và khóa 7 ngày theo mặc định. Yêu cầu đặt mới trong thời gian khóa bị từ chối với lý do.
  - **AC-F8.3.1-03: Hủy chỗ buổi đã đổi giờ** (BR-F8.3-01, BR-F8-03)
    - **Given** Buổi đã đổi giờ, còn chỗ BOOKED của hội viên và thời gian đã vào khoảng hủy muộn
    - **When** Hội viên hủy chỗ hợp lệ
    - **Then** Chuyển CANCELLED, is_late_cancel = false, không tăng vi phạm.
  - **AC-F8.3.1-04: Đặt suất vừa hủy** (BR-F8.3-04, BR-F8.1-03)
    - **Given** Lớp đầy vừa được trả một suất và còn trong thời gian nhận đặt
    - **When** Một hội viên khác đủ điều kiện gửi yêu cầu
    - **Then** Có thể đặt đúng một suất trống. Việc hủy tự nó không tạo chỗ cho người khác.
  - **AC-F8.3.1-05: Qua tháng Việt Nam** (BR-F8.3-02)
    - **Given** Vi phạm cũ thuộc tháng trước theo giờ Việt Nam
    - **When** Ghi vi phạm đầu tiên tháng mới
    - **Then** Chỉ đếm vi phạm tháng mới, kể cả khi thời điểm UTC vẫn thuộc tháng trước.
  - **AC-F8.3.1-06: Hết khóa rồi có vi phạm mới** (BR-F8.3-02)
    - **Given** Ba vi phạm đã gây khóa, khóa đã hết và vẫn trong cùng tháng
    - **When** Ghi vi phạm đầu tiên sau lần khóa gần nhất
    - **Then** Số đếm mới là một; không kích hoạt khóa từ ba vi phạm cũ.
  - **AC-F8.3.1-07: Hai yêu cầu hủy cùng chỗ** (BR-F8.3-02, BR-F8.3-04)
    - **Given** Có một chỗ có thể hủy
    - **When** Hai yêu cầu hủy cạnh tranh
    - **Then** Chỉ ghi một chuyển trạng thái, trả đúng một suất và tính tối đa một vi phạm.
  - **AC-F8.3.1-08: Phạm vi và trạng thái hủy** (BR-F8.3-04; S3)
    - **Given** Người gọi ngoài quyền hoặc chỗ đã hủy, đã điểm danh hoặc buổi không còn cho hủy
    - **When** Gửi yêu cầu hủy
    - **Then** Từ chối, giữ trạng thái và số vi phạm.
  - **AC-F8.3.1-09: Hủy buổi từ phía quản lý** (BR-F8.7-05)
    - **Given** Quản lý có quyền hủy một buổi đang có chỗ BOOKED
    - **When** Hệ thống hủy các chỗ cùng buổi
    - **Then** Mọi chỗ BOOKED được hủy không phạt theo UC-F8.7.

### 3.5. UC-F8.4: Đã bỏ (ADR 0014)

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.4-01**, **BR-F8.4-02**, **BR-F8.4-04**, **BR-F8.4-05**, **BR-F8.4-06**, **BR-F8.4-07**, **BR-F8.4-08:** Đã bỏ (ADR 0014). BR-F8.4-03 về đổi phòng được chuyển sang UC-F8.7.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F8.4.1: Đã bỏ (ADR 0014)

- **Issue:** #159
- Không có AC áp dụng. Các kịch bản còn hiệu lực từ #68 thuộc US-F8.7.1.

### 3.6. UC-F8.5: Xem lịch dạy lớp nhóm

#### A. Tiền điều kiện

Huấn luyện viên đã đăng nhập và có hồ sơ nhân viên tương ứng.

#### B. Hậu điều kiện

Trả các buổi được phân công cho người đang đăng nhập. Đây là thao tác đọc.

#### C. Luồng sự kiện chính

1. Huấn luyện viên mở lịch dạy và chọn khoảng ngày.
2. Backend xác định `staff.id` của người gọi và lọc theo phân công hiện tại của từng buổi.
3. Giao diện hiển thị thời gian, loại lớp, phòng và trạng thái của các buổi đã lọc.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Không có lịch:** Trả danh sách rỗng.
- **A2 – Có người dạy thay:** Dùng phân công của buổi học, kể cả khi khác lịch lặp.
- **E1 – Ngày sai định dạng, thiếu đăng nhập hoặc sai quyền (400/401/403):** Không trả lịch ngoài phạm vi.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.5-01:** `class_sessions.trainer_id` xác định người được phân công của từng buổi.
- **BR-F8.5-02:** Backend kiểm tra quyền và phạm vi dữ liệu theo S3.
- **BR-F8.5-03:** UC này chỉ trả lớp nhóm. F9 sở hữu dữ liệu buổi PT khi giao diện hiển thị lịch tổng hợp.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

Đọc `class_sessions`, `class_schedules`, `class_types`, `rooms`, `staff`, `users`.

#### G. Thiết kế giao diện và API liên quan

[Wireframe huấn luyện viên](../../design/wireframe/huan-luyen-vien.drawio.svg). API đề xuất `GET /api/v1/trainers/me/class-sessions?from={date}&to={date}`.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F8.5.1: Xem lịch dạy lớp nhóm

- **User story:** Là **Huấn luyện viên**, tôi muốn **xem các buổi lớp được phân công**, để **chuẩn bị và vào điểm danh**.
- **BR liên quan:** BR-F8.5-01, BR-F8.5-02, BR-F8.5-03
- **Version / Sprint:** v2 / Sprint 2
- **Issue:** #160
- **Tiêu chí chấp nhận:**
  - **AC-F8.5.1-01: Lịch của người được phân công** (BR-F8.5-01, BR-F8.5-02, BR-F8.5-03)
    - **Given** Có lớp do người gọi dạy, lớp của người khác và buổi PT
    - **When** Huấn luyện viên mở lịch lớp nhóm
    - **Then** Chỉ thấy lớp nhóm được phân công cho mình trong khoảng chọn.
  - **AC-F8.5.1-02: Buổi có người dạy thay** (BR-F8.5-01)
    - **Given** Buổi học đổi huấn luyện viên so với lịch lặp
    - **When** Hai huấn luyện viên mở lịch dạy
    - **Then** Chỉ người được gán cho buổi cụ thể thấy buổi đó.
  - **AC-F8.5.1-03: Không có lớp** (BR-F8.5-01)
    - **Given** Không có buổi được phân công trong khoảng chọn
    - **When** Huấn luyện viên xem lịch
    - **Then** Trả 200 với danh sách rỗng.
  - **AC-F8.5.1-04: Sai vai trò hoặc yêu cầu lịch người khác** (BR-F8.5-02)
    - **Given** Người gọi không có vai trò huấn luyện viên, hoặc cố truy cập lịch của người khác
    - **When** Gửi yêu cầu
    - **Then** Backend trả 403 và không trả lịch ngoài quyền.

### 3.7. UC-F8.6: Điểm danh học viên lớp học nhóm

#### A. Tiền điều kiện

Huấn luyện viên được phân công thao tác từ giờ bắt đầu đến hết ngày diễn ra theo giờ Việt Nam. Sau thời hạn đó, chỉ quản lý đúng chi nhánh được sửa kết quả đã ghi.

#### B. Hậu điều kiện

Lưu kết quả và người điểm danh; nếu sửa thì audit cũ/mới. `NO_SHOW` ghi lần đầu có thể dẫn đến khóa. Sửa kết quả giữ nguyên khóa. Quản lý có thể gỡ khóa bằng một thao tác riêng có audit.

#### C. Luồng sự kiện chính

1. Huấn luyện viên mở danh sách lớp được phân công và chọn kết quả `ATTENDED` hoặc `NO_SHOW`.
2. Backend kiểm tra quyền, thời điểm và trạng thái của chỗ đặt trong giao dịch lưu.
3. Hệ thống lưu kết quả và `attendance_marked_by`. Nếu lần đầu ghi `NO_SHOW`, áp dụng BR-F8.6-03.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Đã check-in ở quầy:** Hiển thị đã đến và vẫn yêu cầu xác nhận kết quả.
- **A2 – Sửa điểm danh:** Huấn luyện viên đang trong thời hạn hoặc quản lý đúng chi nhánh sau thời hạn sửa `ATTENDED`/`NO_SHOW`; ghi audit và người sửa, giữ nguyên khóa.
- **A3 – Gỡ khóa thủ công:** Quản lý đúng chi nhánh yêu cầu gỡ khóa. Hệ thống xóa mốc khóa hiện tại và ghi audit trong cùng giao dịch.
- **E1 – Đầu vào sai hoặc chưa đăng nhập (400/401):** Không lưu.
- **E2 – Sai phân công, vai trò hoặc phạm vi (403):** Không trả danh sách và không đổi kết quả.
- **E3 – Không tìm thấy buổi hoặc chỗ đặt (404):** Trả lỗi tiếng Việt.
- **E4 – Ngoài thời hạn hoặc trạng thái không được phép (422):** Từ chối, kể cả khi chỗ đã đổi trạng thái sau lúc mở màn hình.
- **E5 – Xung đột cập nhật đồng thời (409):** Không ghi đè kết quả vừa thay đổi.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.6-01:** `attendance_marked_by` ghi `users.id` của người thao tác gần nhất. Ghi kết quả, người sửa, giá trị cũ và mới trong `audit_logs` cùng giao dịch khi sửa.
- **BR-F8.6-02:** Check-in F4 chỉ ghi `arrived_at`; huấn luyện viên vẫn xác nhận có mặt hoặc vắng.
- **BR-F8.6-03:** Lần đầu ghi `NO_SHOW` tính vi phạm theo BR-F8.3-02. Sửa điểm danh không tự tính lại, tạo, kéo dài hay gỡ khóa.
- **BR-F8.6-04:** Chỗ `ATTENDED` và `NO_SHOW` vẫn được tính trong sĩ số của buổi.
- **BR-F8.6-05:** Điểm danh lần đầu chỉ từ `BOOKED`, sửa kết quả chỉ từ `ATTENDED` hoặc `NO_SHOW`. Kiểm tra lại trạng thái khi ghi.
- **BR-F8.6-06:** Điểm danh bị từ chối không tạo vi phạm và không đổi mốc khóa.
- **BR-F8-02:** Quản lý chi nhánh có thể gỡ khóa đặt lớp trong phạm vi quyền. Mỗi lần gỡ phải ghi `audit_logs`.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

`class_sessions.trainer_id`, `class_bookings.status`, `arrived_at`, `attendance_marked_by`, `members.booking_locked_until`, `audit_logs`.

#### G. Thiết kế giao diện và API liên quan

[Wireframe huấn luyện viên](../../design/wireframe/huan-luyen-vien.drawio.svg). API điểm danh đề xuất `PUT /api/v1/class-sessions/{sessionId}/attendance`. #199 xác nhận hợp đồng điểm danh, sửa và gỡ khóa.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F8.6.1: Điểm danh học viên lớp học nhóm

- **User story:** Là **Huấn luyện viên**, tôi muốn **điểm danh có mặt hoặc vắng**, để **ghi nhận đúng tham dự**.
- **BR liên quan:** BR-F8.6-01, BR-F8.6-04, BR-F8.6-05, BR-F8.6-03, BR-F8.3-02, BR-F8.6-02, BR-F8.6-06, BR-F8-02
- **Version / Sprint:** v2 / Sprint 2
- **Issue:** #161
- **Tiêu chí chấp nhận:**
  - **AC-F8.6.1-01: Điểm danh lần đầu từ BOOKED** (BR-F8.6-01, BR-F8.6-04, BR-F8.6-05)
    - **Given** Huấn luyện viên được phân công, còn trong thời hạn và chỗ đang BOOKED
    - **When** Huấn luyện viên ghi ATTENDED hoặc NO_SHOW
    - **Then** Lưu kết quả và attendance_marked_by của người ghi. Chỗ vẫn tính trong sĩ số.
  - **AC-F8.6.1-02: Vắng mặt lần đầu đạt ngưỡng** (BR-F8.6-03, BR-F8.3-02)
    - **Given** Hội viên có hai vi phạm hợp lệ và ngưỡng là ba
    - **When** Huấn luyện viên điểm danh NO_SHOW lần đầu
    - **Then** Lưu kết quả và áp khóa theo cấu hình trong cùng giao dịch.
  - **AC-F8.6.1-03: Check-in không thay điểm danh** (BR-F8.6-02)
    - **Given** F4 đã ghi arrived_at
    - **When** Huấn luyện viên mở danh sách
    - **Then** Thấy đã đến, kết quả tham dự chỉ được ghi khi huấn luyện viên xác nhận.
  - **AC-F8.6.1-04: Sai phân công hoặc chi nhánh** (BR-F8.6-01; S3)
    - **Given** Huấn luyện viên không dạy buổi hoặc quản lý thuộc chi nhánh khác
    - **When** Thử xem danh sách hoặc cập nhật kết quả
    - **Then** Từ chối, không lộ dữ liệu và không đổi điểm danh.
  - **AC-F8.6.1-05: Biên thời gian của huấn luyện viên** (BR-F8.6-01, BR-F8.6-05)
    - **Given** Buổi bắt đầu 18:00 theo giờ Việt Nam
    - **When** Huấn luyện viên thử điểm danh trước 18:00, lần đầu lúc 18:00, sửa lúc 23:59 và sửa khi sang ngày mới
    - **Then** Trước giờ và sang ngày mới bị từ chối; đúng giờ và 23:59 được phép với trạng thái hợp lệ.
  - **AC-F8.6.1-06: Quản lý sửa sau hạn** (BR-F8.6-01, BR-F8.6-05)
    - **Given** Đã qua ngày diễn ra, buổi có kết quả đã ghi
    - **When** Quản lý đúng chi nhánh sửa
    - **Then** Lưu kết quả mới, người sửa và audit giá trị cũ/mới cùng giao dịch.
  - **AC-F8.6.1-07: Sửa NO_SHOW không gỡ khóa** (BR-F8.6-01, BR-F8.6-03)
    - **Given** Hội viên đang bị khóa, kể cả đã từng có nhiều lần khóa và kết quả NO_SHOW ghi sai
    - **When** Người có quyền sửa thành ATTENDED
    - **Then** Ghi sửa và audit; mốc khóa đang áp dụng giữ nguyên dù kết quả sửa sẽ làm số lỗi xuống dưới ngưỡng.
  - **AC-F8.6.1-08: Sửa ATTENDED không tạo hoặc kéo dài khóa** (BR-F8.6-01, BR-F8.6-03)
    - **Given** Có kết quả ATTENDED cần sửa, hội viên đang hoặc chưa bị khóa
    - **When** Người có quyền sửa thành NO_SHOW
    - **Then** Ghi sửa và audit; không kích hoạt hoặc kéo dài khóa do thao tác sửa.
  - **AC-F8.6.1-09: Trạng thái không đủ điều kiện** (BR-F8.6-05, BR-F8.6-06)
    - **Given** Chỗ đã CANCELLED khi gửi hoặc vừa bị hủy trước lúc ghi, hoặc thao tác sửa nhắm vào BOOKED chưa có kết quả
    - **When** Gửi yêu cầu điểm danh hoặc sửa sai trạng thái
    - **Then** Backend kiểm tra lại, từ chối thao tác sai, giữ nguyên khóa và không tạo NO_SHOW.
  - **AC-F8.6.1-10: Gỡ khóa thủ công có audit** (BR-F8-02)
    - **Given** Hội viên đang bị khóa đặt lớp
    - **When** Quản lý đúng chi nhánh thực hiện gỡ khóa
    - **Then** Xóa mốc khóa hiện tại và lưu audit của lần gỡ cùng giao dịch; giữ lịch sử vi phạm.
  - **AC-F8.6.1-11: Gỡ khóa sai quyền** (BR-F8-02; S3)
    - **Given** Người gọi là huấn luyện viên, lễ tân hoặc quản lý ngoài chi nhánh
    - **When** Yêu cầu gỡ khóa
    - **Then** Từ chối và giữ nguyên trạng thái khóa.

### 3.8. UC-F8.7: Quản lý lịch lặp lớp học và phân công huấn luyện viên

#### A. Tiền điều kiện

Quản lý chi nhánh thao tác lịch lặp tại chi nhánh mình. Quản lý đúng chi nhánh hoặc chủ chuỗi được đổi phòng, đổi giờ hoặc hủy buổi riêng lẻ.

#### B. Hậu điều kiện

Lịch lặp hợp lệ được lưu hoặc ngừng cho các lần sinh sau. Thay đổi buổi riêng lẻ giữ nguyên khóa định danh của buổi. F8 phát sự kiện đổi phòng, đổi giờ hoặc hủy buổi cho F10; lỗi xử lý thông báo không đảo ngược giao dịch F8.

#### C. Luồng sự kiện chính

1. Quản lý chọn loại lớp, huấn luyện viên, phòng, thứ ISO, giờ, sức chứa và khoảng hiệu lực.
2. Hệ thống kiểm tra phạm vi, tài nguyên, sức chứa và xung đột thật sự của lịch.
3. Hệ thống lưu lịch `ACTIVE`. UC-F8.8 sinh từng buổi cụ thể.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Sửa/ngừng lịch lặp:** Áp dụng cho buổi sinh về sau. Ngừng chuyển `INACTIVE`; ngày lễ hủy từng buổi nếu cần.
- **A2 – Sửa sức chứa buổi:** Chỉ áp dụng buổi chưa bắt đầu có trạng thái `SCHEDULED` hoặc `RESCHEDULED`, theo BR-F8.7-02.
- **A3 – Đổi phòng:** Kiểm tra phòng mới và sức chứa, lưu thay đổi rồi phát sự kiện cho F10. Giữ nguyên các chỗ đặt.
- **A4 – Đổi giờ:** Kiểm tra lịch phòng và huấn luyện viên, cập nhật giờ, giữ chỗ hiện tại và phát sự kiện có giờ cũ/mới theo BR-F8-03.
- **A5 – Hủy buổi:** Chuyển buổi và chỗ đang hoạt động sang `CANCELLED` theo BR-F8.7-05, rồi phát sự kiện cho F10.
- **E1 – Sai đầu vào hoặc chưa đăng nhập (400/401):** Từ chối trước khi ghi.
- **E2 – Sai quyền hoặc thiếu tài nguyên (403/404):** Từ chối thao tác.
- **E3 – Trùng lịch hoặc xung đột cập nhật (409):** Nêu xung đột và giữ dữ liệu trước yêu cầu.
- **E4 – Sai sức chứa, giờ hoặc vai trò huấn luyện viên (422):** Từ chối và nêu lý do.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.7-01:** Lịch lặp hằng tuần dùng thứ ISO 1–7 (thứ Hai đến Chủ nhật).
- **BR-F8.7-02:** Sức chứa lịch và buổi phải dương và không vượt sức chứa phòng. Chỉ sửa sức chứa buổi `SCHEDULED` hoặc `RESCHEDULED` khi `start_at > now`, với giá trị mới không thấp hơn số chỗ chiếm theo BR-F8.1-03. Giờ kết thúc phải sau giờ bắt đầu, người dạy có vai trò `TRAINER`.
- **BR-F8.7-03:** Mỗi lịch có chi nhánh, loại lớp, người dạy, phòng, giờ, thứ, sức chứa, trạng thái và thời gian hiệu lực. `valid_to = NULL` biểu thị chưa có ngày kết thúc.
- **BR-F8.7-04:** Ngừng lịch chuyển `INACTIVE` và giữ các buổi đã sinh. Sửa lịch cũng chỉ áp dụng cho lần sinh về sau.
- **BR-F8.7-05:** Hủy buổi chuyển các chỗ `BOOKED` sang `CANCELLED` với lý do `SESSION_CANCELLED`, không phạt và phát sự kiện để F10 báo người bị ảnh hưởng. Giữ lịch sử buổi và chỗ.
- **BR-F8.7-06:** Từ chối xung đột phòng hoặc huấn luyện viên khi có buổi thực tế trùng giờ, kể cả xung đột với buổi PT theo F9/NF6.
- **BR-F8.4-03:** Đổi phòng giữ nguyên chỗ và phát sự kiện thay đổi phòng theo ADR 0014.
- **BR-F8-03:** Khi đổi giờ, ghi giờ cũ qua `original_start_at`, giữ nguyên `occurrence_date` và chỗ đặt, phát sự kiện giờ cũ/mới. F10 xử lý thông báo ngoài giao dịch F8.
- **BR-F8-04:** Quản lý chỉ thao tác trong chi nhánh mình. Chủ chuỗi có quyền đổi phòng, đổi giờ và hủy buổi riêng lẻ ở mọi chi nhánh. Lễ tân không có các quyền đó.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

`class_types`, `class_schedules`, `class_sessions`, `class_bookings`, `branches`, `rooms`, `staff`. F10 sở hữu dữ liệu thông báo.

#### G. Thiết kế giao diện và API liên quan

Màn hình quản lý lịch lặp, phân công và thay đổi buổi. #198 xác nhận API lịch lặp; #199 xác nhận API thay đổi buổi và hợp đồng sự kiện với F10.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F8.7.1: Quản lý lịch lặp và buổi riêng lẻ

- **User story:** Là **Quản lý chi nhánh**, tôi muốn **tạo lịch lặp, phân công phòng và huấn luyện viên, đổi hoặc hủy từng buổi**, để **vận hành lớp nhóm**.
- **BR liên quan:** BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04, BR-F8.7-06, BR-F8.7-04, BR-F8.1-03, BR-F8.4-03, BR-F8-03, BR-F8.8-02, BR-F8.7-05
- **Version / Sprint:** v2 / Sprint 2
- **Issue:** #162
- **Tiêu chí chấp nhận:**
  - **AC-F8.7.1-01: Tạo lịch lặp hợp lệ** (BR-F8.7-01, BR-F8.7-02, BR-F8.7-03, BR-F8-04)
    - **Given** Quản lý đúng chi nhánh chọn phòng, loại lớp, huấn luyện viên, sức chứa và giờ hợp lệ
    - **When** Tạo lịch thứ Ba
    - **Then** Lưu lịch ACTIVE với day_of_week = 2 và dữ liệu đã xác nhận.
  - **AC-F8.7.1-02: Sức chứa, giờ hoặc vai trò không hợp lệ** (BR-F8.7-02)
    - **Given** Sức chứa không dương hoặc vượt phòng, giờ kết thúc không sau giờ bắt đầu hoặc người dạy không mang vai trò TRAINER
    - **When** Tạo hoặc sửa lịch
    - **Then** Từ chối với lý do phù hợp và không ghi lịch.
  - **AC-F8.7.1-03: Chặn xung đột thực tế** (BR-F8.7-06)
    - **Given** Hai lịch cùng phòng hoặc cùng huấn luyện viên tạo ra buổi bị chồng giờ, hoặc huấn luyện viên có buổi PT bị chồng giờ
    - **When** Tạo hoặc sửa lịch hay phân công bị xung đột
    - **Then** Từ chối, nêu lịch bị trùng và không lưu một phần thay đổi.
  - **AC-F8.7.1-04: Cùng giờ nhưng khác ngày thực tế** (BR-F8.7-06)
    - **Given** Hai lịch dùng cùng phòng hoặc người dạy, cùng giờ nhưng khác thứ hoặc khoảng hiệu lực không giao nhau
    - **When** Quản lý lưu lịch hợp lệ
    - **Then** Chấp nhận vì không có buổi thực tế chồng giờ.
  - **AC-F8.7.1-05: Sửa hoặc ngừng lịch không đổi buổi đã sinh** (BR-F8.7-02, BR-F8.7-04)
    - **Given** Lịch đã sinh buổi và có chỗ đặt
    - **When** Quản lý cập nhật hoặc ngừng lịch
    - **Then** Buổi, sức chứa và chỗ đã sinh giữ nguyên; chỉnh sửa dùng cho lần sinh sau, ngừng lịch chuyển trạng thái lịch sang INACTIVE.
  - **AC-F8.7.1-06: Giảm sức chứa đúng biên** (BR-F8.7-02, BR-F8.1-03)
    - **Given** Buổi SCHEDULED hoặc RESCHEDULED chưa bắt đầu, còn bốn chỗ đang chiếm
    - **When** Quản lý yêu cầu giảm xuống ba hoặc bốn với phòng đủ sức chứa
    - **Then** Mức ba bị từ chối và báo bốn chỗ đang chiếm. Mức bốn được chấp nhận nếu vẫn thỏa giới hạn phòng.
  - **AC-F8.7.1-07: Buổi không cho sửa sức chứa** (BR-F8.7-02)
    - **Given** Buổi đã đến giờ bắt đầu hoặc trạng thái khác SCHEDULED và RESCHEDULED
    - **When** Quản lý yêu cầu sửa sức chứa
    - **Then** Từ chối và giữ nguyên sức chứa.
  - **AC-F8.7.1-08: Quyền lịch lặp và buổi riêng lẻ** (BR-F8-04; S3)
    - **Given** Có quản lý A, quản lý B, chủ chuỗi và lễ tân; buổi thuộc A
    - **When** Thử sửa lịch lặp, dùng tài nguyên ngoài chi nhánh hoặc đổi phòng, đổi giờ, hủy buổi
    - **Then** Lịch lặp chỉ do quản lý đúng chi nhánh thao tác bằng tài nguyên hợp lệ. Đổi phòng, đổi giờ và hủy buổi cho phép quản lý A hoặc chủ chuỗi, từ chối quản lý B và lễ tân.
  - **AC-F8.7.1-09: Đổi phòng giữ chỗ** (BR-F8.4-03, BR-F8.7-02, BR-F8.7-06)
    - **Given** Buổi có chỗ đặt và phòng mới hợp lệ, đủ sức chứa, không trùng
    - **When** Người có quyền đổi phòng
    - **Then** Cập nhật phòng, giữ nguyên chỗ và phát sự kiện đổi phòng cho F10.
  - **AC-F8.7.1-10: Đổi giờ giữ chỗ** (BR-F8-03, BR-F8.8-02)
    - **Given** Buổi có chỗ BOOKED và giờ mới hợp lệ
    - **When** Người có quyền đổi giờ một hoặc nhiều lần
    - **Then** Giữ BOOKED, lưu mốc giờ cũ và giữ occurrence_date; mỗi lần đổi phát sự kiện có giờ cũ/mới. Hội viên có thể hủy không phạt theo UC-F8.3.
  - **AC-F8.7.1-11: Hủy buổi không phạt** (BR-F8.7-05)
    - **Given** Buổi chưa diễn ra còn chỗ BOOKED
    - **When** Người có quyền hủy buổi
    - **Then** Buổi và các chỗ đang hoạt động chuyển CANCELLED với lý do SESSION_CANCELLED cho chỗ đặt, không ghi hủy muộn và phát sự kiện để báo tất cả người bị ảnh hưởng.
  - **AC-F8.7.1-12: Lỗi xử lý thông báo không đảo ngược thay đổi** (BR-F8.4-03, BR-F8-03, BR-F8.7-05)
    - **Given** Đổi phòng, đổi giờ hoặc hủy buổi đã ghi thành công và xử lý thông báo ở F10 gặp lỗi
    - **When** F10 xử lý sự kiện
    - **Then** Dữ liệu buổi và chỗ theo kết quả F8 được giữ nguyên; F10 chịu trách nhiệm xử lý thông báo.

### 3.9. UC-F8.8: Sinh buổi học từ lịch lặp

#### A. Tiền điều kiện

Lịch `ACTIVE`, đúng thứ ISO và ngày phát sinh nằm trong khoảng hiệu lực. Điều kiện ngày áp dụng cho ngày phát sinh, kể cả khi tác vụ chạy trước ngày bắt đầu hiệu lực.

#### B. Hậu điều kiện

Mỗi lịch có tối đa một buổi cho mỗi ngày phát sinh. Không tạo buổi sai hoặc trùng, kể cả khi chạy lại.

#### C. Luồng sự kiện chính

1. Tác vụ nền chọn ngày phát sinh theo thứ ISO trong khoảng hiệu lực.
2. Hệ thống ghép ngày Việt Nam với giờ của lịch, lưu thời điểm bắt đầu/kết thúc và sao chép chi nhánh, người dạy, phòng, sức chứa vào buổi `SCHEDULED`.
3. Hệ thống lưu theo khóa `(class_schedule_id, occurrence_date)` và ghi kết quả tác vụ.

#### D. Các luồng thay thế và ngoại lệ

- **A1 – Buổi đã có:** Bỏ qua ngày phát sinh, kể cả khi buổi đã đổi giờ.
- **A2 – Lịch ngừng hoặc ngoài hiệu lực:** Không sinh.
- **E1 – Tài nguyên không hợp lệ hoặc xung đột:** Không tạo buổi sai; ghi nhận nguyên nhân.
- **E2 – Hai tác vụ cùng sinh:** Ràng buộc duy nhất ngăn bản ghi trùng.

#### E. Quy tắc nghiệp vụ liên quan

- **BR-F8.8-01:** Sinh từng buổi cụ thể từ lịch lặp có hiệu lực.
- **BR-F8.8-02:** Duy nhất theo lịch và ngày phát sinh. Đổi giờ giữ nguyên `occurrence_date`.
- **BR-F8.8-03:** Buổi lưu bản sao phân công và sức chứa tại thời điểm sinh. Người dạy của buổi có thể được thay riêng theo ERD.

#### F. Dữ liệu và bảng cơ sở dữ liệu liên quan

Đọc `class_schedules`, `class_types`, `branches`, `rooms`, `staff`; ghi `class_sessions`.

#### G. Thiết kế giao diện và API liên quan

Tác vụ nội bộ. Buổi sinh ra dùng chung cho lịch công khai, hội viên và huấn luyện viên. Lịch chạy, khoảng sinh trước và thử lại xem mục 7.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F8.8.1: Sinh buổi học từ lịch lặp

- **User story:** Là **Quản lý chi nhánh**, tôi muốn **hệ thống tự sinh buổi học từ lịch lặp**, để **hội viên có buổi để đặt mà không phải tạo tay**.
- **BR liên quan:** BR-F8.8-01, BR-F8.8-03, BR-F8.8-02, BR-F8.7-06
- **Version / Sprint:** v2 / Sprint 2
- **Issue:** #163
- **Tiêu chí chấp nhận:**
  - **AC-F8.8.1-01: Sinh đúng thứ, dữ liệu và múi giờ** (BR-F8.8-01, BR-F8.8-03)
    - **Given** Lịch ACTIVE thứ Ba và ngày phát sinh nằm trong khoảng hiệu lực
    - **When** Tác vụ sinh chọn ngày thứ Ba
    - **Then** Tạo đúng buổi SCHEDULED, sao chép chi nhánh, người dạy, phòng và sức chứa. occurrence_date là ngày Việt Nam; thời điểm lưu tương ứng UTC và hiển thị đúng giờ Việt Nam.
  - **AC-F8.8.1-02: Chạy lại hoặc chạy đồng thời** (BR-F8.8-02)
    - **Given** Buổi của một lịch và ngày phát sinh đã có hoặc hai lượt chạy cùng xử lý
    - **When** Tác vụ chạy lại hoặc cạnh tranh
    - **Then** Có tối đa một buổi theo khóa lịch và ngày phát sinh.
  - **AC-F8.8.1-03: Lịch ngoài hiệu lực hoặc đã ngừng** (BR-F8.8-01)
    - **Given** Lịch INACTIVE hoặc ngày phát sinh ngoài khoảng hiệu lực
    - **When** Tác vụ xét lịch
    - **Then** Không sinh buổi cho ngày đó.
  - **AC-F8.8.1-04: Sinh lại sau khi buổi đổi giờ** (BR-F8.8-02)
    - **Given** Buổi đã sinh được đổi giờ hoặc ngày diễn ra
    - **When** Tác vụ chạy lại cho ngày phát sinh gốc
    - **Then** Giữ occurrence_date gốc, giữ giờ mới, không tạo buổi thứ hai hoặc phục hồi giờ cũ.
  - **AC-F8.8.1-05: Tài nguyên không còn dùng được** (BR-F8.8-01, BR-F8.8-03, BR-F8.7-06)
    - **Given** Phòng hoặc người dạy không còn hợp lệ hoặc đã có buổi thực tế trùng giờ
    - **When** Tác vụ sinh buổi
    - **Then** Không tạo bản ghi sai hoặc trùng, ghi nhận nguyên nhân để xử lý.

## 4. Quy tắc áp dụng chung

- Trạng thái chỗ đặt gồm `BOOKED`, `CANCELLED`, `ATTENDED`, `NO_SHOW`. Chỉ `CANCELLED` không chiếm chỗ.
- Dùng ngày lịch và tháng dương lịch theo `Asia/Ho_Chi_Minh` cho cửa sổ đặt, ngày phát sinh, điểm danh và đếm vi phạm. Lưu thời điểm bằng `timestamptz`, đối chiếu theo UTC và hiển thị theo giờ Việt Nam.
- Dùng `system_configs`; cấu hình chi nhánh ghi đè cấu hình toàn chuỗi. Bảng dưới ghi mặc định để đọc các ví dụ, theo [ADR 0014](../../decisions/0014-bo-danh-sach-cho-lop-hoc-nhom.md) và [ERD v2](../../design/erd/erd-v2.dbml).

| Tham số | Mặc định | Áp dụng |
| --- | ---: | --- |
| `BOOKING_WINDOW_DAYS` | 7 | Cửa sổ đặt theo ngày lịch |
| `BOOKING_CUTOFF_MIN` | 30 | Đóng đặt khi thời gian còn lại dưới số phút này |
| `LATE_CANCEL_HOURS` | 2 | Hủy trước giờ ít nhất số giờ này thì miễn phạt |
| `PENALTY_LIMIT_PER_MONTH` | 3 | Ngưỡng vi phạm trong tháng và sau lần khóa gần nhất |
| `PENALTY_LOCK_DAYS` | 7 | Số ngày khóa đặt lớp |

## 5. Bảng dữ liệu liên quan

Xem [ERD v2, nhóm Lớp học nhóm](../../design/erd/erd-v2.dbml) cho tập giá trị, chỉ mục và ràng buộc. Các UC ở mục 3 ghi bảng đọc và ghi theo nghiệp vụ.

## 6. Tích hợp với chức năng liên quan

- **F1 và F3:** F1 xác định danh tính, vai trò và phạm vi. F3 cung cấp loại, trạng thái và phạm vi sử dụng gói.
- **F4:** Check-in ghi `arrived_at`. Kết quả tham dự vẫn do UC-F8.6 xác nhận.
- **F9:** Kiểm tra xung đột giờ dạy giữa lớp nhóm và buổi PT theo NF6.
- **F10:** Nhận sự kiện đổi phòng, đổi giờ và hủy buổi. F10 tạo và gửi thông báo ngoài giao dịch F8.
- **F18:** Kiểm tra phạm vi `branch_id` ở backend ngay cả khi v2 dùng một chi nhánh mặc định.

## 7. Quyết định và thiết kế còn mở

- [ADR 0014](../../decisions/0014-bo-danh-sach-cho-lop-hoc-nhom.md) đã đóng các quyết định về danh sách chờ, FIFO, ân hạn, xác nhận lại khi đổi giờ và tự tính lại khóa khi sửa điểm danh. Các mã đã bỏ được giữ tại UC tương ứng.
- Hợp đồng kỹ thuật còn cần xác nhận trong #198–#200: cấu trúc API và mã lỗi cụ thể, dữ liệu chỗ trống và bộ lọc lịch công khai, cách lưu mốc lần khóa gần nhất, tính nguyên tử nếu điểm danh theo danh sách, lịch chạy và khoảng sinh buổi trước. `BOOKING_WINDOW_DAYS` điều khiển quyền đặt, không xác định số ngày sinh buổi.
- Nguồn của các quy tắc cũ còn hiệu lực về ngày lịch, quyền chi nhánh, sửa lịch lặp và biên điểm danh: [quyết định ngày 04/11](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6633040706) và [làm rõ ngày 07/11](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6701988493).

## 8. Truy vết kịch bản cũ

| Issue cũ | Nơi truy vết hiện tại |
| --- | --- |
| [#64](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/64) | US-F8.7.1 và US-F8.8.1 |
| [#65](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/65) | US-F8.1.1 |
| [#66](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/66) | US-F8.3.1 và luồng hủy buổi ở US-F8.7.1 |
| [#67](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/67) | US-F8.2.1 đã bỏ. Biên giờ đặt và toàn vẹn sĩ số nằm ở US-F8.1.1 |
| [#68](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/68) | US-F8.7.1 và ngoại lệ miễn phạt ở US-F8.3.1. US-F8.4.1 đã bỏ |
| [#69](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/69) | US-F8.5.1 và US-F8.6.1 |
| [#70](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/70) | US-F8.0.1 |
