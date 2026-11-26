# Đặc tả Use Case – F6: Dashboard cơ bản

- **Mã chức năng:** F6
- **Version:** v1 (Bắt buộc)
- **Người biên soạn:** Lê Phước Minh Quân
- **Người duyệt chéo:** Nguyễn Trọng Việt
- **Tài liệu chuẩn tham chiếu:** 
  - [Bản chốt phạm vi](../ban-chot-pham-vi.md) (mục F6 – Dashboard cơ bản; yêu cầu phi chức năng NF1, NF4)
  - [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml)
  - [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md) (bảng `checkins`, `invoices`, `payments`, `memberships`)
  - [Quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md) (Quy tắc số 6: Tiền là số nguyên VND; Báo cáo doanh thu dùng `LEFT JOIN` với `members`)
  - [ADR 0001](../../decisions/0001-cong-nghe.md), [ADR 0007](../../decisions/0007-cau-truc-ma-nguon.md)

---

## 1. Tổng quan chức năng

Chức năng **F6 – Dashboard cơ bản** là màn hình trung tâm đầu tiên mà nhân viên lễ tân (`RECEPTIONIST`) và quản lý chi nhánh (`BRANCH_MANAGER`) nhìn thấy khi đăng nhập vào hệ thống; chủ chuỗi (`SUPER_ADMIN`) xem được dashboard của từng chi nhánh bằng cách chọn chi nhánh:
- Cung cấp cái nhìn toàn diện, tức thời về tình hình vận hành phòng tập gym tại chi nhánh trong ngày.
- **Thống kê theo thời gian thực số lượng hội viên đang có mặt:** Đếm số hội viên duy nhất (`COUNT(DISTINCT member_id)`) có lượt check-in trong ngày mà chưa có mốc thời gian check-out (`checked_out_at IS NULL`), tránh trường hợp một hội viên check-in nhiều lần bị đếm trùng (VD-25).
- **Tổng số lượt check-in trong ngày:** Đếm tổng số lượt khách đến tập từ đầu ngày đến thời điểm hiện tại (`COUNT(*)`), kèm biểu đồ cột thể hiện mật độ check-in theo từng khung giờ (trong giờ mở cửa của chi nhánh, `branches.open_time` – `branches.close_time`) nhằm nhận biết giờ cao điểm.
- **Tổng doanh thu trong ngày tại quầy:** Tính tổng số tiền thu được từ các hóa đơn có trạng thái `PAID` phát sinh trong ngày tại chi nhánh. **Bắt buộc sử dụng `LEFT JOIN` với bảng `members`** vì các hóa đơn bán lẻ hàng hóa/nước uống tại quầy cho khách vãng lai (`POS` - F12) có thể có `member_id` để trống (`NULL`). Doanh thu được phân loại chi tiết theo phương thức thanh toán: Tiền mặt (`CASH`) và Chuyển khoản ngân hàng (`BANK_TRANSFER` qua VietQR).
- **Cảnh báo gói tập sắp hết hạn:** Tự động lọc và hiển thị danh sách các gói tập của hội viên sẽ hết hạn trong vòng **7 ngày tới**, hoặc gói theo buổi (`SESSION`) chỉ còn không quá `SESSION_WARNING_THRESHOLD` buổi (mặc định 2), để nhân viên chủ động nhắc nhở, tư vấn gia hạn kịp thời trước khi hội viên gián đoạn việc tập luyện.
- **Dòng dữ liệu check-in gần nhất (Live Feed):** Bảng hiển thị danh sách 5–10 lượt check-in mới nhất kèm trạng thái, ảnh đại diện và thời gian vào.

---

## 2. Danh sách Use Case

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F6.1** | Xem số lượng hội viên đang có mặt tại phòng tập | Lễ tân, Quản lý chi nhánh, Chủ chuỗi | Cao | Sprint 2 |
| **UC-F6.2** | Xem tổng lượt check-in và biểu đồ phân bổ theo giờ | Lễ tân, Quản lý chi nhánh, Chủ chuỗi | Cao | Sprint 2 |
| **UC-F6.3** | Xem tổng doanh thu trong ngày tại quầy | Lễ tân, Quản lý chi nhánh, Chủ chuỗi | Cao | Sprint 2 |
| **UC-F6.4** | Xem danh sách cảnh báo gói tập sắp hết hạn | Lễ tân, Quản lý chi nhánh, Chủ chuỗi | Cao | Sprint 2 |
| **UC-F6.5** | Xem danh sách lượt check-in mới nhất (Live Feed) | Lễ tân, Quản lý chi nhánh, Chủ chuỗi | Trung bình | Sprint 2 |

---

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F6.1: Xem số lượng hội viên đang có mặt tại phòng tập

- **Mã UC:** UC-F6.1
- **Tên Use Case:** Xem số lượng hội viên đang có mặt tại phòng tập (Real-time Live Attendance)
- **Tác nhân chính:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`), Chủ chuỗi (`SUPER_ADMIN`)
- **Mô tả ngắn gọn:** Hiển thị thẻ chỉ số trực quan đếm số hội viên đang ở trong phòng tập tại chi nhánh theo thời gian thực.

#### A. Tiền điều kiện
- Người dùng đã đăng nhập với vai trò hợp lệ (`RECEPTIONIST`, `BRANCH_MANAGER`, hoặc `SUPER_ADMIN`).
- Với `RECEPTIONIST` và `BRANCH_MANAGER`: Tài khoản gắn với chi nhánh cụ thể thông qua bảng `staff` (`staff.branch_id`).
- Với `SUPER_ADMIN` (Chủ chuỗi): Không thuộc biên chế cố định của một chi nhánh nào trong bảng `staff`, bắt buộc phải chọn chi nhánh cụ thể thông qua tham số truy vấn `?branchId=`.

#### B. Hậu điều kiện
- Con số hội viên đang có mặt được cập nhật chính xác và nổi bật trên thẻ KPI của chi nhánh được chọn.

#### C. Luồng sự kiện chính
1. Người dùng mở trang Dashboard.
2. Hệ thống xác định chi nhánh cần lấy dữ liệu (`branch_id`):
   - **Với `RECEPTIONIST` và `BRANCH_MANAGER`:** Hệ thống luôn sử dụng `staff.branch_id` từ hồ sơ nhân viên trong phiên làm việc, tự động bỏ qua tham số `branchId` nếu có truyền từ giao diện.
   - **Với `SUPER_ADMIN`:** Bắt buộc phải truyền tham số query `?branchId={id}` trên URL để chỉ định chi nhánh muốn xem (`id` là số nguyên, khóa `branches.id` kiểu `bigint`). Nếu thiếu tham số `branchId`, hệ thống trả về mã lỗi `400 Bad Request` với thông báo: *"Chủ chuỗi cần chọn chi nhánh cụ thể để xem Dashboard."* Nếu `branchId` không tồn tại trong `branches`, hệ thống trả về `404 Not Found` (`NotFoundException`) với thông báo: *"Không tìm thấy chi nhánh."*, không trả các chỉ số bằng 0.
3. Hệ thống thực hiện truy vấn đếm:
   ```sql
   SELECT count(DISTINCT member_id)
   FROM checkins 
   WHERE branch_id = :branchId 
     AND checked_in_at >= :dayStart AND checked_in_at < :dayEnd
     AND checked_out_at IS NULL;
   ```
4. Hệ thống trả về con số đếm và hiển thị trên thẻ Stat Card "Hội viên đang tập" kèm chỉ báo trạng thái hoạt động (chấm xanh nhấp nháy).

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F6.1.1: Xem số người đang có mặt tại phòng tập
- **User story:** Là **Lễ tân, Quản lý chi nhánh hoặc Chủ chuỗi**, tôi muốn **xem số lượng hội viên đang có mặt tại phòng tập theo thời gian thực và tổng lượt check-in hôm nay**, để **nắm bắt mật độ phòng tập và tình hình vận hành cơ sở**.
- **BR liên quan:** BR-F6.1, BR-F6.4; S3; VD-25 (đếm số hội viên duy nhất).
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #145
- **Tiêu chí chấp nhận:**
  - **AC-F6.1.1-01: Đếm chính xác số hội viên duy nhất đang tập và tổng lượt check-in** (BR-F6.1, VD-25)
    - **Given** Trong ngày hôm nay tại chi nhánh: hội viên A có 2 lượt check-in chưa check-out; hội viên B có 1 lượt check-in chưa check-out; hội viên C có 1 lượt check-in đã check-out; ngoài ra có 1 lượt check-in lúc 23:30 hôm qua chưa check-out.
    - **When** Lễ tân mở màn hình Dashboard.
    - **Then** Chỉ số "Số người đang tập" hiển thị là **2** (đếm số hội viên duy nhất A và B, không đếm trùng lượt của A theo VD-25). Chỉ số "Lượt check-in hôm nay" hiển thị là **4** (đếm tổng số sự kiện check-in của A, B, C phát sinh hôm nay). Lượt check-in lúc 23:30 hôm qua không được tính vào cả hai chỉ số.
  - **AC-F6.1.1-02: Chủ chuỗi bắt buộc phải chọn chi nhánh cụ thể** (S3)
    - **Given** Người dùng đăng nhập với vai trò Chủ chuỗi (`SUPER_ADMIN`).
    - **When** Gọi API `GET /api/v1/dashboard/stats` không truyền tham số `branchId`; hoặc truyền `?branchId=1` (chi nhánh tồn tại); hoặc truyền `?branchId=999` (chi nhánh không tồn tại).
    - **Then** Khi không truyền `branchId`: hệ thống trả về `400 Bad Request` yêu cầu chọn chi nhánh; giao diện hiển thị câu nhắc chọn chi nhánh và không gửi API. Khi truyền `branchId=1`: hệ thống trả về đúng số liệu của chi nhánh 1. Khi truyền mã chi nhánh không tồn tại: hệ thống trả về `404 Not Found` *"Không tìm thấy chi nhánh."*, không trả số liệu bằng 0.
  - **AC-F6.1.1-03: Nhân viên chi nhánh chỉ xem số liệu chi nhánh mình** (S3)
    - **Given** Nhân viên lễ tân hoặc quản lý của chi nhánh `CN01` gửi request kèm tham số `?branchId=2` (chi nhánh `CN02`).
    - **When** Gọi API Dashboard.
    - **Then** Hệ thống tự động ghi đè và chỉ trả về số liệu của chi nhánh `CN01`; tham số `branchId=2` bị bỏ qua; trên giao diện nhân viên không hiển thị dropdown chọn chi nhánh.
  - **AC-F6.1.1-04: Phân quyền truy cập các chỉ số Dashboard** (S3)
    - **Given** Người dùng với vai trò Hội viên (`MEMBER`), Huấn luyện viên (`TRAINER`), hoặc chưa đăng nhập; hoặc tham số sai định dạng.
    - **When** Gọi bất kỳ API nào của Dashboard.
    - **Then** Hội viên và HLV nhận `403 Forbidden` *"Bạn không có quyền thực hiện thao tác này."*; chưa đăng nhập nhận `401 Unauthorized`; tham số sai nhận `400 Bad Request`. Menu Dashboard trên giao diện chỉ hiển thị cho Lễ tân, Quản lý chi nhánh và Chủ chuỗi.
  - **AC-F6.1.1-05: Chủ chuỗi xem số liệu của chi nhánh đã ngừng hoạt động** (VD-25, GĐ-4)
    - **Given** Chi nhánh có trạng thái `INACTIVE` (đã ngừng hoạt động) nhưng vẫn còn lưu dữ liệu lịch sử trong hệ thống.
    - **When** Chủ chuỗi chọn xem chi nhánh đó trên Dashboard.
    - **Then** Hệ thống vẫn hiển thị đầy đủ số liệu thống kê lịch sử của chi nhánh đó; chỉ khi chi nhánh không tồn tại mới trả về `404 Not Found`.

---

### 3.2. UC-F6.2: Xem tổng lượt check-in và biểu đồ phân bổ theo giờ

- **Mã UC:** UC-F6.2
- **Tên Use Case:** Xem tổng lượt check-in và biểu đồ phân bổ theo giờ
- **Tác nhân chính:** Lễ tân, Quản lý chi nhánh, Chủ chuỗi
- **Mô tả ngắn gọn:** Hiển thị tổng số lượt check-in từ đầu ngày và biểu đồ cột biểu diễn số lượt check-in theo từng khung giờ trong giờ mở cửa của chi nhánh.

#### A. Tiền điều kiện
- Người dùng đã đăng nhập và truy cập trang Dashboard.

#### B. Hậu điều kiện
- Thẻ KPI "Lượt check-in hôm nay" và biểu đồ cột trực quan được kết xuất đầy đủ.

#### C. Luồng sự kiện chính
1. Hệ thống truy vấn tổng số lượt check-in trong ngày tại chi nhánh:
   ```sql
   SELECT count(*) 
   FROM checkins 
   WHERE branch_id = :branchId 
     AND checked_in_at >= :dayStart AND checked_in_at < :dayEnd;
   ```
2. Hệ thống tổng hợp số lượt check-in theo từng khung giờ (theo múi giờ `Asia/Ho_Chi_Minh`):
   ```sql
   SELECT extract(hour FROM (checked_in_at AT TIME ZONE 'Asia/Ho_Chi_Minh')) AS hour_bucket,
          count(*) AS checkin_count
   FROM checkins
   WHERE branch_id = :branchId
     AND checked_in_at >= :dayStart AND checked_in_at < :dayEnd
   GROUP BY hour_bucket
   ORDER BY hour_bucket;
   ```
3. Hệ thống chuẩn bị đủ các khung giờ từ giờ của `branches.open_time` đến giờ của (`branches.close_time` trừ 1 phút), vì đúng giờ đóng cửa là ngoài giờ (F4). Ví dụ chi nhánh mở 06:00, đóng 22:00 thì có 16 khung 06h–21h; chi nhánh mở 05:30 thì khung đầu là 05h (lượt check-in 05:30–05:59). Khung giờ nào không có lượt check-in thì gán giá trị 0. Tổng các khung luôn bằng tổng lượt check-in ở bước 1, vì F4 chỉ nhận check-in trong giờ mở cửa.
4. Hệ thống hiển thị biểu đồ cột (Bar Chart) giúp lễ tân nhận diện các khung giờ cao điểm (thường từ 17:00 – 20:00).

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F6.2.1: Lượt check-in theo giờ
- **User story:** Là **Lễ tân, Quản lý chi nhánh hoặc Chủ chuỗi**, tôi muốn **xem biểu đồ phân bổ lượt check-in theo từng khung giờ trong ngày**, để **nhận biết giờ cao điểm và điều phối nhân sự/dịch vụ hợp lý**.
- **BR liên quan:** BR-F6.4 (múi giờ Việt Nam, dải giờ mở cửa).
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #146
- **Tiêu chí chấp nhận:**
  - **AC-F6.2.1-01: Biểu đồ thể hiện đầy đủ các khung giờ trong giờ mở cửa** (BR-F6.4)
    - **Given** Chi nhánh có giờ mở cửa 06:00 – 22:00; có các lượt check-in phân bổ ở một số khung giờ, và có 1 lượt check-in lúc 23:30 hôm qua.
    - **When** Người dùng xem biểu đồ phân bổ theo giờ trên Dashboard.
    - **Then** Biểu đồ hiển thị đủ tất cả các khung giờ từ giờ mở cửa đến trước giờ đóng cửa (từ 06:00 đến 21:00, gồm 16 khung); các khung không có lượt check-in hiển thị giá trị 0; tổng số lượt của tất cả các khung bằng đúng tổng lượt check-in hôm nay; không bao gồm lượt check-in của ngày hôm qua.
  - **AC-F6.2.1-02: Xử lý khung giờ với chi nhánh có giờ mở cửa lẻ** (BR-F6.4)
    - **Given** Chi nhánh có cấu hình giờ mở cửa lẻ, ví dụ mở cửa lúc 05:30.
    - **When** Người dùng xem biểu đồ phân bổ theo giờ.
    - **Then** Khung giờ đầu tiên trên biểu đồ là 05:00, gom toàn bộ các lượt check-in phát sinh từ 05:30 đến 05:59.

---

### 3.3. UC-F6.3: Xem tổng doanh thu trong ngày tại quầy

- **Mã UC:** UC-F6.3
- **Tên Use Case:** Xem tổng doanh thu trong ngày tại quầy
- **Tác nhân chính:** Lễ tân, Quản lý chi nhánh, Chủ chuỗi
- **Mô tả ngắn gọn:** Tính toán và hiển thị tổng số tiền thu được tại quầy trong ngày, tách bạch giữa Tiền mặt và Chuyển khoản VietQR.

#### A. Tiền điều kiện
- Người dùng đã đăng nhập và truy cập Dashboard.

#### B. Hậu điều kiện
- Tổng doanh thu trong ngày hiển thị bằng đơn vị VND chuẩn, kèm tỷ lệ giữa Tiền mặt và Chuyển khoản ngân hàng.

#### C. Luồng sự kiện chính
1. Hệ thống truy vấn tổng tiền từ bảng `invoices` kết hợp `payments` của chi nhánh trong ngày hiện tại:
   ```sql
   SELECT coalesce(sum(p.amount), 0) AS total_revenue,
          coalesce(sum(CASE WHEN p.method = 'CASH' THEN p.amount ELSE 0 END), 0) AS cash_revenue,
          coalesce(sum(CASE WHEN p.method = 'BANK_TRANSFER' THEN p.amount ELSE 0 END), 0) AS transfer_revenue
   FROM payments p
   JOIN invoices i ON p.invoice_id = i.id
   LEFT JOIN members m ON i.member_id = m.id
   WHERE i.branch_id = :branchId
     AND i.status = 'PAID'
     AND p.status = 'CONFIRMED'
     AND (p.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date = :today;
   ```
2. **Quy tắc bắt buộc:** Truy vấn sử dụng `LEFT JOIN members m ON i.member_id = m.id` vì đối với các hóa đơn khách lẻ mua nước, khăn, phụ kiện tại quầy, trường `member_id` có giá trị `NULL`. Nếu dùng `INNER JOIN` sẽ làm thất thoát số liệu doanh thu bán lẻ (Quy tắc nghiệp vụ dễ làm sai số 8).
3. Hệ thống định dạng tiền tệ theo tiêu chuẩn Việt Nam (ví dụ: `15.450.000 đ`) và hiển thị trên thẻ Stat Card "Doanh thu hôm nay".

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F6.3.1: Doanh thu trong ngày
- **User story:** Là **Lễ tân, Quản lý chi nhánh hoặc Chủ chuỗi**, tôi muốn **xem tổng doanh thu và chi tiết tiền mặt, chuyển khoản phát sinh trong ngày**, để **theo dõi doanh thu tại quầy và đối soát dòng tiền**.
- **BR liên quan:** BR-F6.2, BR-F6.3, BR-F6.4.
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #147
- **Tiêu chí chấp nhận:**
  - **AC-F6.3.1-01: Doanh thu bao gồm cả khách lẻ và phân loại theo hình thức thanh toán** (BR-F6.2, BR-F6.3)
    - **Given** Trong ngày hôm nay tại chi nhánh có: 01 hóa đơn mua gói tập trị giá 2.000.000 đ thanh toán chuyển khoản (`BANK_TRANSFER`, PAID, payment CONFIRMED); 01 hóa đơn mua nước của khách lẻ không có tài khoản hội viên (`member_id IS NULL`) trị giá 50.000 đ thanh toán tiền mặt (`CASH`, PAID, payment CONFIRMED); 01 khoản thanh toán bị hủy/hoàn tiền (`REFUNDED`); và 01 hóa đơn đang chờ thanh toán (`PENDING`).
    - **When** Người dùng xem Dashboard doanh thu.
    - **Then** Tổng doanh thu hôm nay hiển thị chính xác là **2.050.000 đ**, trong đó tiền mặt là **50.000 đ** và chuyển khoản là **2.000.000 đ**. Hệ thống chỉ tính các khoản thanh toán `CONFIRMED` thuộc hóa đơn `PAID` phát sinh hôm nay theo giờ Việt Nam; không bị thất thoát tiền của khách lẻ và không tính các hóa đơn đã hoàn tiền hoặc đang chờ thanh toán.

---

### 3.4. UC-F6.4: Xem danh sách cảnh báo gói tập sắp hết hạn

- **Mã UC:** UC-F6.4
- **Tên Use Case:** Xem danh sách cảnh báo gói tập sắp hết hạn
- **Tác nhân chính:** Lễ tân, Quản lý chi nhánh, Chủ chuỗi
- **Mô tả ngắn gọn:** Hiển thị danh sách các hội viên có gói tập (`memberships`) sẽ kết thúc trong vòng 7 ngày tới, hoặc gói `SESSION` sắp hết buổi, tại chi nhánh, hỗ trợ nút "Gia hạn nhanh" chuyển thẳng sang form bán gói của F3.

#### A. Tiền điều kiện
- Người dùng đã đăng nhập và truy cập Dashboard.

#### B. Hậu điều kiện
- Bảng danh sách hội viên sắp hết hạn hiển thị kèm số ngày còn lại, số buổi còn lại (gói `SESSION`) và thông tin liên hệ.

#### C. Luồng sự kiện chính
1. Hệ thống tìm kiếm các gói tập thỏa mãn toàn bộ các điều kiện nghiệp vụ:
   - **Trạng thái gói:** `memberships.status = 'ACTIVE'`.
   - **Sắp hết hạn hoặc sắp hết buổi** (thỏa một trong hai):
     - **Sắp hết hạn (chuẩn 7 ngày tới):** `memberships.end_date >= :today AND memberships.end_date < :today + 7` (`:today` là ngày Việt Nam do ứng dụng tính, xem BR-F6.4) (không dùng `BETWEEN` để tránh lấy cả 2 đầu mút dẫn tới 8 ngày).
     - **Sắp hết buổi:** `memberships.package_type = 'SESSION' AND memberships.remaining_sessions <= :sessionThreshold`, với `:sessionThreshold` đọc từ `system_configs` khóa `SESSION_WARNING_THRESHOLD` (mặc định 2; giá trị của chi nhánh ghi đè giá trị toàn chuỗi). Gói còn 0 buổi nhưng vẫn `ACTIVE` (vừa trừ buổi cuối hôm nay) cũng được cảnh báo.
   - **Phân định theo chi nhánh:**
     - Nếu gói gắn với chi nhánh cụ thể (`memberships.branch_id IS NOT NULL`): `memberships.branch_id = :branchId`.
     - Nếu là gói toàn chuỗi (`memberships.branch_id IS NULL`): Chỉ hiển thị cảnh báo tại chi nhánh gốc đăng ký ban đầu của hội viên (`members.home_branch_id = :branchId`), tránh việc nhiều chi nhánh cùng nhìn thấy một hội viên và gọi điện trùng lặp gây phiền toái cho khách.
   - **Loại trừ hội viên đã mua gói gia hạn mới:** Không hiển thị các gói mà hội viên đó đã có sẵn một gói tập khác cùng loại và cùng phạm vi chi nhánh (cùng `memberships.package_type` và `memberships.branch_id`, so sánh `branch_id` theo `IS NOT DISTINCT FROM` vì gói toàn chuỗi có `branch_id` rỗng) ở trạng thái `memberships.status = 'UPCOMING'` (đã mua để nối tiếp sau khi gói hiện tại hết hạn).
2. Hệ thống liên kết với bảng `members` để lấy: Mã hội viên, Họ tên, Số điện thoại, Ảnh đại diện, Tên gói tập, Loại gói, Lý do cảnh báo (`EXPIRING_DATE` hoặc `LOW_SESSIONS`), Ngày hết hạn, Số ngày còn lại (`end_date - :today`), Số buổi còn lại (`remaining_sessions`, chỉ với gói `SESSION`).
3. Sắp xếp: gói `SESSION` đã về 0 buổi (ngày mai hết dùng) xếp lên đầu; còn lại theo ngày hết hạn tăng dần. Mỗi dòng ghi lý do cảnh báo: "Sắp hết hạn" (theo ngày) hoặc "Sắp hết buổi" (theo ngưỡng buổi); gói `SESSION` đã về 0 buổi luôn ghi "Sắp hết buổi"; các gói khác thỏa cả hai thì ghi "Sắp hết hạn".
4. Hệ thống hiển thị bảng cảnh báo trên giao diện Dashboard.
5. Khi lễ tân bấm nút **"Gia hạn"** tại một dòng hội viên, hệ thống điều hướng sang màn hình bán/gia hạn gói tập (F3) với thông tin hội viên đã được điền sẵn.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F6.4.1: Gói sắp hết hạn
- **User story:** Là **Lễ tân hoặc Quản lý chi nhánh**, tôi muốn **xem danh sách hội viên có gói tập sắp hết hạn hoặc sắp hết buổi**, để **chủ động liên hệ tư vấn gia hạn kịp thời**.
- **BR liên quan:** BR-F6.5; VD-25 (loại trừ xóa mềm, phân trang, ưu tiên gói 0 buổi).
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #148
- **Tiêu chí chấp nhận:**
  - **AC-F6.4.1-01: Lọc chính xác các gói hết hạn trong vòng 7 ngày tới** (BR-F6.5)
    - **Given** Hôm nay là ngày 02/11/2026; gói A hết hạn ngày 05/11/2026; gói B hết hạn ngày 20/11/2026; gói C hết hạn đúng ngày 09/11/2026.
    - **When** Lễ tân xem danh sách cảnh báo gói hết hạn.
    - **Then** Gói A xuất hiện trong danh sách; gói B và gói C không xuất hiện (khoảng lọc nửa mở `[hôm nay, hôm nay + 7)` tức từ 02/11 đến trước 09/11).
  - **AC-F6.4.1-02: Cảnh báo gói SESSION sắp hết buổi theo cấu hình ngưỡng** (BR-F6.5)
    - **Given** Hệ thống có cấu hình `SESSION_WARNING_THRESHOLD = 2` (chi nhánh có thể cấu hình riêng); gói SESSION C còn 2 buổi, gói SESSION D còn 3 buổi, cả hai cùng hết hạn 30/11/2026.
    - **When** Lễ tân xem bảng cảnh báo.
    - **Then** Gói C xuất hiện kèm số buổi còn lại là 2; gói D không xuất hiện. Ngưỡng cảnh báo được đọc động từ cấu hình chi nhánh hoặc toàn chuỗi.
  - **AC-F6.4.1-03: Loại trừ hội viên đã mua gói gia hạn UPCOMING** (BR-F6.5)
    - **Given** Hội viên có gói `TIME` sắp hết hạn sau 3 ngày; hội viên đã mua trước một gói tập cùng loại, cùng phạm vi chi nhánh ở trạng thái `UPCOMING`.
    - **When** Lễ tân xem bảng cảnh báo.
    - **Then** Gói sắp hết hạn này không hiển thị trong danh sách cảnh báo. Nếu gói mới mua vẫn đang ở trạng thái `PENDING` (chưa thanh toán), hoặc là gói `UPCOMING` khác loại, thì gói cũ vẫn được hiển thị cảnh báo bình thường.
  - **AC-F6.4.1-04: Gói toàn chuỗi chỉ hiển thị cảnh báo tại chi nhánh gốc** (BR-F6.5)
    - **Given** Hội viên sở hữu gói toàn chuỗi (`memberships.branch_id IS NULL`), hồ sơ hội viên đăng ký tại chi nhánh Cầu Giấy (`members.home_branch_id = Cầu Giấy`), gói sắp hết hạn.
    - **When** Lễ tân chi nhánh Đống Đa và lễ tân chi nhánh Cầu Giấy xem bảng cảnh báo trên Dashboard.
    - **Then** Cảnh báo chỉ xuất hiện trên Dashboard của chi nhánh Cầu Giấy, không xuất hiện tại Đống Đa (tránh việc nhiều chi nhánh cùng gọi điện trùng lặp cho khách).
  - **AC-F6.4.1-05: Quy tắc sắp xếp, lý do cảnh báo, giới hạn phân trang và loại trừ hội viên xóa mềm** (VD-25, GĐ-2, GĐ-3, GĐ-5)
    - **Given** Danh sách gồm nhiều gói tập cần cảnh báo, trong đó có gói `SESSION` 0 buổi (vừa trừ buổi cuối hôm nay), và có hội viên đã bị xóa mềm (`deleted_at IS NOT NULL`).
    - **When** Gọi API `GET /api/v1/dashboard/expiring-memberships` (mặc định hoặc kèm tham số `limit`).
    - **Then** Gói `SESSION` 0 buổi được ưu tiên sắp xếp lên đầu danh sách, sau đó sắp xếp theo ngày hết hạn tăng dần; mỗi phần tử trả về đầy đủ lý do cảnh báo (`EXPIRING_DATE` hoặc `LOW_SESSIONS`); số buổi còn lại chỉ hiển thị đối với gói `SESSION`; số lượng phần tử trả về mặc định là 10 dòng (cho phép tham số `limit` từ 1 đến 100); tổng số gói cần cảnh báo trên KPI khớp với dữ liệu; và hoàn toàn loại trừ các hội viên đã bị xóa mềm.
  - **AC-F6.4.1-06: Thao tác Gia hạn nhanh trên giao diện**
    - **Given** Lễ tân xem danh sách cảnh báo trên Dashboard.
    - **When** Bấm nút "Gia hạn" tại một dòng hội viên.
    - **Then** Hệ thống điều hướng sang màn hình Bán gói tập (F3), tự động điền sẵn thông tin hội viên và tải danh mục gói phù hợp. Chủ chuỗi xem Dashboard thì không hiển thị nút Gia hạn này.

---

### 3.5. UC-F6.5: Xem danh sách lượt check-in mới nhất (Live Feed)

- **Mã UC:** UC-F6.5
- **Tên Use Case:** Xem danh sách lượt check-in mới nhất (Live Feed)
- **Tác nhân chính:** Lễ tân, Quản lý chi nhánh, Chủ chuỗi
- **Mô tả ngắn gọn:** Hiển thị 5–10 lượt check-in gần nhất trong ngày để lễ tân dễ dàng theo dõi dòng người ra vào và thực hiện check-out khi cần.

#### A. Tiền điều kiện
- Người dùng truy cập Dashboard.

#### B. Hậu điều kiện
- Bảng danh sách lượt vào/ra gần nhất hiển thị trực quan.

#### C. Luồng sự kiện chính
1. Hệ thống truy vấn 10 bản ghi mới nhất từ bảng `checkins` tại chi nhánh trong ngày:
   ```sql
   SELECT c.id, c.checked_in_at, c.checked_out_at, c.method,
          m.member_code, m.full_name, m.avatar_url,
          pkg.name AS package_name, ms.package_type
   FROM checkins c
   JOIN members m ON c.member_id = m.id
   JOIN memberships ms ON c.membership_id = ms.id
   JOIN packages pkg ON ms.package_id = pkg.id
   WHERE c.branch_id = :branchId
     AND c.checked_in_at >= :dayStart AND c.checked_in_at < :dayEnd
   ORDER BY c.checked_in_at DESC
   LIMIT 10;
   ```
2. Hiển thị bảng kèm tag trạng thái:
   - Nếu `checked_out_at IS NULL`: Tag xanh lá "Đang tập" kèm nút bấm "Check-out nhanh".
   - Nếu `checked_out_at IS NOT NULL`: Tag xám "Đã về" kèm thời lượng tập.

#### H. User story và tiêu chí chấp nhận (Acceptance Criteria)

##### US-F6.5.1: Lượt check-in mới nhất (Live Feed)
- **User story:** Là **Lễ tân hoặc Quản lý chi nhánh**, tôi muốn **xem dòng danh sách các lượt check-in mới nhất hôm nay**, để **theo dõi hội viên vừa ra vào và hỗ trợ check-out nhanh**.
- **BR liên quan:** BR-F6.1, BR-F6.4; VD-25 (GĐ-2).
- **Version / Sprint:** v1 / Sprint 2
- **Issue:** #149
- **Tiêu chí chấp nhận:**
  - **AC-F6.5.1-01: Danh sách các lượt check-in mới nhất trong ngày** (VD-25, GĐ-2)
    - **Given** Hôm nay có nhiều lượt check-in phát sinh tại chi nhánh, và có 1 lượt check-in lúc 23:30 hôm qua.
    - **When** Người dùng xem Dashboard.
    - **Then** Hệ thống hiển thị 10 lượt check-in mới nhất trong ngày (cho phép tùy chỉnh `limit` từ 1 đến 50); mỗi dòng hiển thị giờ check-in (theo giờ Việt Nam), cờ `hasAvatar`, trạng thái ("Đang tập" hoặc "Đã về"), thời lượng tập luyện; không bao gồm lượt check-in lúc 23:30 của ngày hôm qua.
  - **AC-F6.5.1-02: Thao tác Check-out nhanh từ danh sách Live Feed**
    - **Given** Lễ tân thấy một dòng hội viên có trạng thái "Đang tập" trên bảng Live Feed.
    - **When** Lễ tân bấm nút "Check-out nhanh".
    - **Then** Hệ thống gọi API check-out của F4 (`POST /api/v1/checkins/{id}/checkout`), sau đó tự động làm mới lại dữ liệu Dashboard; nếu xảy ra lỗi thì hiển thị đúng thông báo lỗi của API. Chủ chuỗi xem Dashboard thì không hiển thị nút này.

---

## 4. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F6.1 (Thời gian thực):** Chỉ số hội viên đang có mặt phải phản ánh trạng thái tức thời của các lượt check-in chưa check-out trong ngày (`checked_out_at IS NULL`).
- **BR-F6.2 (Toàn vẹn doanh thu):** Mọi tính toán doanh thu phải dựa trên các khoản thanh toán đã xác nhận (`payments.status = 'CONFIRMED'`) thuộc hóa đơn đã thanh toán (`invoices.status = 'PAID'`). Bắt buộc dùng `LEFT JOIN` với bảng `members` để không bỏ sót các hóa đơn khách lẻ (Quy tắc số 8).
- **BR-F6.3 (Đơn vị tiền tệ):** Tiền là số nguyên VND (kiểu `integer`), không dùng số thực để tránh sai số dấu phẩy động (Quy tắc số 6).
- **BR-F6.4 (Khung ngày giờ):** Toàn bộ phân nhóm ngày và giờ trên Dashboard được chuyển đổi theo múi giờ Việt Nam (`UTC+7`, `Asia/Ho_Chi_Minh`), dù dữ liệu lưu trữ trong CSDL dưới dạng UTC (`timestamptz`). Tham số `:today` là ngày hiện tại theo giờ Việt Nam; `:dayStart`, `:dayEnd` là 00:00 hôm nay và 00:00 ngày mai theo giờ Việt Nam (đổi sang `OffsetDateTime`). Cả ba do ứng dụng tính (`ZoneId.of("Asia/Ho_Chi_Minh")`) rồi truyền vào truy vấn; không dùng `CURRENT_DATE` của PostgreSQL vì máy chủ chạy theo UTC.
- **BR-F6.5 (Quy tắc lọc cảnh báo gói sắp hết hạn):** Khoảng thời gian cảnh báo là 7 ngày đặt thành hằng số trong mã nguồn (bản chốt không có dấu `*` và ERD không có cấu hình này). Khoảng lọc tính theo công thức nửa mở `end_date >= :today AND end_date < :today + 7` ngày. Ngưỡng sắp hết buổi của gói `SESSION` là tham số có dấu `*` trong bản chốt, đọc từ `system_configs` khóa `SESSION_WARNING_THRESHOLD` (mặc định 2), không viết cứng; gói thỏa một trong hai điều kiện thì được cảnh báo, mỗi gói chỉ xuất hiện một lần. Bắt buộc loại trừ các hội viên đã có gói `UPCOMING` cùng loại và cùng phạm vi chi nhánh. Các điều kiện dùng cột chép sẵn trên `memberships` (`package_type`, `branch_id`), không dùng cột của `packages` (bảng này đặt tên cột là `type`). Với gói dùng toàn chuỗi (`memberships.branch_id IS NULL`), chỉ hiển thị cảnh báo tại chi nhánh gốc của hội viên (`members.home_branch_id = :branchId`).

---

## 5. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động / đọc:** `checkins`, `members`, `memberships`, `packages`, `invoices`, `payments`
- **Chỉ mục có sẵn trong ERD (`erd-v2.dbml`):**
  - `checkins (branch_id, checked_in_at)`: ghi chú trong ERD là dùng cho dashboard theo ngày, theo giờ (F6). Vì vậy các truy vấn `checkins` ở UC-F6.1, F6.2, F6.5 lọc theo khoảng `checked_in_at` (`:dayStart`, `:dayEnd`) chứ không theo `checkin_date`, để dùng được chỉ mục này mà không cần migration mới. Cột `checkin_date` vẫn dùng cho quy tắc trừ buổi của F4.
  - `invoices (branch_id, created_at)`. Truy vấn doanh thu lọc theo `payments.paid_at`, chỉ mục này không phủ; với quy mô v1 (một chi nhánh mặc định) chấp nhận được. Nếu đo thấy chậm thì đề xuất chỉ mục bằng migration mới dải `V9_xx`, không tự đặt tên chỉ mục ngoài ERD.

---

## 6. Thiết kế API nháp

### 6.1. Lấy tổng hợp chỉ số KPI Dashboard
- **Endpoint:** `GET /api/v1/dashboard/stats?branchId={branchId}`
- **Quyền truy cập:** `RECEPTIONIST`, `BRANCH_MANAGER`, `SUPER_ADMIN`
- **Quy tắc tham số `branchId`:**
  - Với `SUPER_ADMIN`: Tham số `?branchId=` là **bắt buộc**, kiểu số nguyên (trả về 400 Bad Request nếu không truyền hoặc sai kiểu, 404 Not Found nếu chi nhánh không tồn tại).
  - Với `RECEPTIONIST` và `BRANCH_MANAGER`: Tham số `?branchId=` là tùy chọn và luôn bị ghi đè bởi `staff.branch_id` từ hồ sơ của chính nhân viên đó.
- **Response Success (200 OK):**
  ```json
  {
    "currentlyInGym": 42,
    "todayCheckinCount": 158,
    "todayRevenue": 15450000,
    "cashRevenue": 4200000,
    "transferRevenue": 11250000,
    "expiringPackagesCount": 8
  }
  ```

### 6.2. Lấy dữ liệu biểu đồ check-in theo khung giờ
- **Endpoint:** `GET /api/v1/dashboard/hourly-checkins`
- **Quyền truy cập:** giống mục 6.1; chỉ trả dữ liệu của chi nhánh được xác định như ở UC-F6.1. Vai trò khác (`MEMBER`, `TRAINER`) nhận 403.
- **Response Success (200 OK):**
  ```json
  [
    { "hour": "06:00", "count": 5 },
    { "hour": "07:00", "count": 14 },
    { "hour": "08:00", "count": 8 },
    { "hour": "17:00", "count": 28 },
    { "hour": "18:00", "count": 35 },
    { "hour": "19:00", "count": 30 },
    { "hour": "20:00", "count": 16 },
    { "hour": "21:00", "count": 6 }
  ]
  ```

### 6.3. Lấy danh sách gói tập sắp hết hạn
- **Endpoint:** `GET /api/v1/dashboard/expiring-memberships?limit=5`
- **Quyền truy cập:** giống mục 6.1; chỉ trả dữ liệu của chi nhánh được xác định như ở UC-F6.1. Vai trò khác (`MEMBER`, `TRAINER`) nhận 403, vì API này trả họ tên và số điện thoại của hội viên (S3).
- **Response Success (200 OK):**
  ```json
  [
    {
      "membershipId": 78,
      "memberId": 15,
      "memberCode": "HV000015",
      "fullName": "Trần Thị Mai",
      "phone": "0987654321",
      "packageName": "Gói Hội Viên Vàng 3 Tháng",
      "packageType": "TIME",
      "endDate": "2026-11-05",
      "daysRemaining": 3,
      "remainingSessions": null,
      "warningReason": "EXPIRING_DATE"
    }
  ]
  ```

---

## 7. Tiêu chí chấp nhận (Acceptance Criteria)

Tiêu chí chấp nhận chi tiết cho từng chức năng đã được chuyển vào mục **H. User story và tiêu chí chấp nhận (Acceptance Criteria)** trong từng Use Case tương ứng ở Mục 3 theo chuẩn cây truy vết US – UC – AC – TC:
- **UC-F6.1 (US-F6.1.1):** Số người đang có mặt tại phòng tập (`AC-F6.1.1-01` đến `AC-F6.1.1-05`, Issue #145).
- **UC-F6.2 (US-F6.2.1):** Lượt check-in theo giờ (`AC-F6.2.1-01` đến `AC-F6.2.1-02`, Issue #146).
- **UC-F6.3 (US-F6.3.1):** Doanh thu trong ngày (`AC-F6.3.1-01`, Issue #147).
- **UC-F6.4 (US-F6.4.1):** Gói sắp hết hạn (`AC-F6.4.1-01` đến `AC-F6.4.1-06`, Issue #148).
- **UC-F6.5 (US-F6.5.1):** Lượt check-in mới nhất (Live Feed) (`AC-F6.5.1-01` đến `AC-F6.5.1-02`, Issue #149).

Các kịch bản kiểm thử nguyên bản trước đây (Scenario 1 đến 6) đã được chuẩn hóa và ánh xạ đầy đủ vào các mã AC chính thức nói trên.
