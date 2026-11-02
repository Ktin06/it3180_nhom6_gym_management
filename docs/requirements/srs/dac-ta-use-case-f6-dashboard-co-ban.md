# Đặc tả Use Case – F6: Dashboard cơ bản

- **Mã chức năng:** F6
- **Version:** v1 (Bắt buộc)
- **Người biên soạn:** Lê Phước Minh Quân
- **Người duyệt chéo:** Nguyễn Trọng Việt
- **Tài liệu chuẩn tham chiếu:** 
  - [Bản chốt phạm vi](../ban-chot-pham-vi.md) (mục F6 – Dashboard cơ bản; yêu cầu phi chức năng NF1, NF4)
  - [ERD v2 (`erd-v2.dbml`)](../../design/erd/erd-v2.dbml)
  - [Thiết kế dữ liệu](../../design/erd/thiet-ke-du-lieu.md) (Chỉ mục `ix_checkins_branch_checked_in`, bảng `invoices`, `memberships`)
  - [Quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md) (Quy tắc số 6: Tiền là số nguyên VND; Báo cáo doanh thu dùng `LEFT JOIN` với `members`)
  - [ADR 0001](../../decisions/0001-cong-nghe.md), [ADR 0007](../../decisions/0007-cau-truc-ma-nguon.md)

---

## 1. Tổng quan chức năng

Chức năng **F6 – Dashboard cơ bản** là màn hình trung tâm đầu tiên mà nhân viên lễ tân (`RECEPTIONIST`) và quản lý chi nhánh (`BRANCH_MANAGER`) nhìn thấy khi đăng nhập vào hệ thống:
- Cung cấp cái nhìn toàn diện, tức thời về tình hình vận hành phòng tập gym tại chi nhánh trong ngày.
- **Thống kê theo thời gian thực số lượng hội viên đang có mặt:** Đếm số lượt check-in trong ngày mà chưa có mốc thời gian check-out (`checked_out_at IS NULL`).
- **Tổng số lượt check-in trong ngày:** Đếm tổng số lượt khách đến tập từ đầu ngày đến thời điểm hiện tại, kèm biểu đồ cột thể hiện mật độ check-in theo từng khung giờ (từ 06:00 đến 22:00) nhằm nhận biết giờ cao điểm.
- **Tổng doanh thu trong ngày tại quầy:** Tính tổng số tiền thu được từ các hóa đơn có trạng thái `PAID` phát sinh trong ngày tại chi nhánh. **Bắt buộc sử dụng `LEFT JOIN` với bảng `members`** vì các hóa đơn bán lẻ hàng hóa/nước uống tại quầy cho khách vãng lai (`POS` - F12) có thể có `member_id` để trống (`NULL`). Doanh thu được phân loại chi tiết theo phương thức thanh toán: Tiền mặt (`CASH`) và Chuyển khoản ngân hàng (`BANK_TRANSFER` qua VietQR).
- **Cảnh báo gói tập sắp hết hạn:** Tự động lọc và hiển thị danh sách các gói tập của hội viên sẽ hết hạn trong vòng **7 ngày tới** để nhân viên chủ động nhắc nhở, tư vấn gia hạn kịp thời trước khi hội viên gián đoạn việc tập luyện.
- **Dòng dữ liệu check-in gần nhất (Live Feed):** Bảng hiển thị danh sách 5–10 lượt check-in mới nhất kèm trạng thái, ảnh đại diện và thời gian vào.

---

## 2. Danh sách Use Case

| Mã UC | Tên Use Case | Tác nhân chính | Mức ưu tiên | Sprint thực hiện |
| --- | --- | --- | --- | --- |
| **UC-F6.1** | Xem số lượng hội viên đang có mặt tại phòng tập | Lễ tân, Quản lý chi nhánh | Cao | Sprint 1 |
| **UC-F6.2** | Xem tổng lượt check-in và biểu đồ phân bổ theo giờ | Lễ tân, Quản lý chi nhánh | Cao | Sprint 1 |
| **UC-F6.3** | Xem tổng doanh thu trong ngày tại quầy | Lễ tân, Quản lý chi nhánh | Cao | Sprint 1 |
| **UC-F6.4** | Xem danh sách cảnh báo gói tập sắp hết hạn | Lễ tân, Quản lý chi nhánh | Cao | Sprint 1 |
| **UC-F6.5** | Xem danh sách lượt check-in mới nhất (Live Feed) | Lễ tân, Quản lý chi nhánh | Trung bình | Sprint 1 |

---

## 3. Đặc tả chi tiết từng Use Case

### 3.1. UC-F6.1: Xem số lượng hội viên đang có mặt tại phòng tập

- **Mã UC:** UC-F6.1
- **Tên Use Case:** Xem số lượng hội viên đang có mặt tại phòng tập (Real-time Live Attendance)
- **Tác nhân chính:** Lễ tân (`RECEPTIONIST`), Quản lý chi nhánh (`BRANCH_MANAGER`), Chủ chuỗi (`SUPER_ADMIN`)
- **Mô tả ngắn gọn:** Hiển thị thẻ chỉ số trực quan đếm số hội viên đang ở trong phòng tập tại chi nhánh theo thời gian thực.

#### A. Tiền điều kiện
- Người dùng đã đăng nhập với vai trò hợp lệ.
- Tài khoản được phân quyền xem dữ liệu của chi nhánh (nhân viên xem chi nhánh của mình; chủ chuỗi chọn chi nhánh hoặc xem toàn chuỗi).

#### B. Hậu điều kiện
- Con số hội viên đang có mặt được cập nhật chính xác và nổi bật trên thẻ KPI.

#### C. Luồng sự kiện chính
1. Người dùng mở trang Dashboard.
2. Hệ thống xác định `branch_id` từ phiên làm việc của người dùng.
3. Hệ thống thực hiện truy vấn đếm:
   ```sql
   SELECT count(*) 
   FROM checkins 
   WHERE branch_id = :branchId 
     AND checkin_date = CURRENT_DATE 
     AND checked_out_at IS NULL;
   ```
4. Hệ thống trả về con số đếm và hiển thị trên thẻ Stat Card "Hội viên đang tập" kèm chỉ báo trạng thái hoạt động (chấm xanh nhấp nháy).

---

### 3.2. UC-F6.2: Xem tổng lượt check-in và biểu đồ phân bổ theo giờ

- **Mã UC:** UC-F6.2
- **Tên Use Case:** Xem tổng lượt check-in và biểu đồ phân bổ theo giờ
- **Tác nhân chính:** Lễ tân, Quản lý chi nhánh
- **Mô tả ngắn gọn:** Hiển thị tổng số lượt check-in từ đầu ngày và biểu đồ cột biểu diễn số lượt check-in theo từng khung giờ từ 06:00 đến 22:00.

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
     AND checkin_date = CURRENT_DATE;
   ```
2. Hệ thống tổng hợp số lượt check-in theo từng khung giờ (từ 06:00 đến 22:00 theo múi giờ `Asia/Ho_Chi_Minh`):
   ```sql
   SELECT extract(hour FROM (checked_in_at AT TIME ZONE 'Asia/Ho_Chi_Minh')) AS hour_bucket,
          count(*) AS checkin_count
   FROM checkins
   WHERE branch_id = :branchId
     AND checkin_date = CURRENT_DATE
   GROUP BY hour_bucket
   ORDER BY hour_bucket;
   ```
3. Hệ thống chuẩn bị dữ liệu 17 khung giờ (từ 06h đến 22h, khung giờ nào không có lượt check-in thì gán giá trị 0).
4. Hệ thống hiển thị biểu đồ cột (Bar Chart) giúp lễ tân nhận diện các khung giờ cao điểm (thường từ 17:00 – 20:00).

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
     AND (p.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date = CURRENT_DATE;
   ```
2. **Quy tắc bắt buộc:** Truy vấn sử dụng `LEFT JOIN members m ON i.member_id = m.id` vì đối với các hóa đơn khách lẻ mua nước, khăn, phụ kiện tại quầy, trường `member_id` có giá trị `NULL`. Nếu dùng `INNER JOIN` sẽ làm thất thoát số liệu doanh thu bán lẻ (Quy tắc nghiệp vụ dễ làm sai số 8).
3. Hệ thống định dạng tiền tệ theo tiêu chuẩn Việt Nam (ví dụ: `15.450.000 đ`) và hiển thị trên thẻ Stat Card "Doanh thu hôm nay".

---

### 3.4. UC-F6.4: Xem danh sách cảnh báo gói tập sắp hết hạn

- **Mã UC:** UC-F6.4
- **Tên Use Case:** Xem danh sách cảnh báo gói tập sắp hết hạn
- **Tác nhân chính:** Lễ tân, Quản lý chi nhánh
- **Mô tả ngắn gọn:** Hiển thị danh sách các hội viên có gói tập (`memberships`) sẽ kết thúc trong vòng 7 ngày tới tại chi nhánh, hỗ trợ nút "Gia hạn nhanh" chuyển thẳng sang form bán gói của F3.

#### A. Tiền điều kiện
- Người dùng đã đăng nhập và truy cập Dashboard.

#### B. Hậu điều kiện
- Bảng danh sách hội viên sắp hết hạn hiển thị kèm số ngày còn lại và thông tin liên hệ.

#### C. Luồng sự kiện chính
1. Hệ thống tìm kiếm các gói tập thỏa mãn điều kiện:
   - `memberships.status = 'ACTIVE'`
   - Gói thuộc chi nhánh hiện tại hoặc gói dùng toàn chuỗi
   - `memberships.end_date BETWEEN CURRENT_DATE AND (CURRENT_DATE + interval '7 days')`
2. Hệ thống liên kết với bảng `members` để lấy: Mã hội viên, Họ tên, Số điện thoại, Ảnh đại diện, Tên gói tập, Ngày hết hạn, Số ngày còn lại.
3. Sắp xếp danh sách theo ngày hết hạn tăng dần (gói nào hết hạn sớm nhất xếp lên đầu).
4. Hệ thống hiển thị bảng cảnh báo trên giao diện Dashboard.
5. Khi lễ tân bấm nút **"Gia hạn"** tại một dòng hội viên, hệ thống điều hướng sang màn hình bán/gia hạn gói tập (F3) với thông tin hội viên đã được điền sẵn.

---

### 3.5. UC-F6.5: Xem danh sách lượt check-in mới nhất (Live Feed)

- **Mã UC:** UC-F6.5
- **Tên Use Case:** Xem danh sách lượt check-in mới nhất (Live Feed)
- **Tác nhân chính:** Lễ tân, Quản lý chi nhánh
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
          pkg.name AS package_name, pkg.package_type
   FROM checkins c
   JOIN members m ON c.member_id = m.id
   JOIN memberships ms ON c.membership_id = ms.id
   JOIN packages pkg ON ms.package_id = pkg.id
   WHERE c.branch_id = :branchId
     AND c.checkin_date = CURRENT_DATE
   ORDER BY c.checked_in_at DESC
   LIMIT 10;
   ```
2. Hiển thị bảng kèm tag trạng thái:
   - Nếu `checked_out_at IS NULL`: Tag xanh lá "Đang tập" kèm nút bấm "Check-out nhanh".
   - Nếu `checked_out_at IS NOT NULL`: Tag xám "Đã về" kèm thời lượng tập.

---

## 4. Quy tắc nghiệp vụ liên quan (Business Rules)

- **BR-F6.1 (Thời gian thực):** Chỉ số hội viên đang có mặt phải phản ánh trạng thái tức thời của các lượt check-in chưa check-out trong ngày (`checked_out_at IS NULL`).
- **BR-F6.2 (Toàn vẹn doanh thu):** Mọi tính toán doanh thu phải dựa trên các khoản thanh toán đã xác nhận (`payments.status = 'CONFIRMED'`) thuộc hóa đơn đã thanh toán (`invoices.status = 'PAID'`). Bắt buộc dùng `LEFT JOIN` với bảng `members` để không bỏ sót các hóa đơn khách lẻ (Quy tắc số 8).
- **BR-F6.3 (Đơn vị tiền tệ):** Tiền là số nguyên VND (kiểu `integer`), không dùng số thực để tránh sai số dấu phẩy động (Quy tắc số 6).
- **BR-F6.4 (Khung ngày giờ):** Toàn bộ phân nhóm ngày và giờ trên Dashboard được chuyển đổi theo múi giờ Việt Nam (`UTC+7`, `Asia/Ho_Chi_Minh`), dù dữ liệu lưu trữ trong CSDL dưới dạng UTC (`timestamptz`).
- **BR-F6.5 (Khoảng cảnh báo hết hạn):** Khoảng thời gian cảnh báo gói sắp hết hạn là 7 ngày (đọc từ cấu hình `system_configs.config_key = 'EXPIRING_WARNING_DAYS'`, mặc định 7 ngày).

---

## 5. Dữ liệu và bảng cơ sở dữ liệu liên quan

- **Bảng tác động / đọc:** `checkins`, `members`, `memberships`, `packages`, `invoices`, `payments`
- **Chỉ mục tối ưu hóa:**
  - `ix_checkins_branch_checked_in`: Tối ưu hóa truy vấn đếm và lọc theo giờ.
  - `ix_invoices_branch_status`: Tối ưu hóa tính toán doanh thu chi nhánh.

---

## 6. Thiết kế API nháp

### 6.1. Lấy tổng hợp chỉ số KPI Dashboard
- **Endpoint:** `GET /api/v1/dashboard/stats`
- **Quyền truy cập:** `RECEPTIONIST`, `BRANCH_MANAGER`, `SUPER_ADMIN`
- **Response Success (200 OK):**
  ```json
  {
    "currentlyInGym": 42,
    "todayCheckinCount": 158,
    "todayCheckinGrowthRate": 12.5,
    "todayRevenue": 15450000,
    "cashRevenue": 4200000,
    "transferRevenue": 11250000,
    "expiringPackagesCount": 8
  }
  ```

### 6.2. Lấy dữ liệu biểu đồ check-in theo khung giờ
- **Endpoint:** `GET /api/v1/dashboard/hourly-checkins`
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
      "endDate": "2026-11-05",
      "daysRemaining": 3
    }
  ]
  ```

---

## 7. Tiêu chí chấp nhận (Acceptance Criteria)

- **Scenario 1: Hiển thị đúng số hội viên đang có mặt**
  - **Given** Chi nhánh hôm nay có 10 lượt check-in, trong đó 3 lượt đã check-out và 7 lượt chưa check-out.
  - **When** Lễ tân truy cập Dashboard.
  - **Then** Thẻ KPI "Hội viên đang tập" hiển thị chính xác con số 7.

- **Scenario 2: Tính toán chính xác doanh thu bao gồm cả khách lẻ**
  - **Given** Hôm nay chi nhánh có:
    - 01 hóa đơn bán gói cho hội viên `HV000001` trị giá 2.000.000 đ (Chuyển khoản VietQR, PAID).
    - 01 hóa đơn bán nước cho khách lẻ không có hồ sơ hội viên (`member_id IS NULL`) trị giá 50.000 đ (Tiền mặt, PAID).
  - **When** Người dùng xem Dashboard doanh thu.
  - **Then** Tổng doanh thu hiển thị là 2.050.000 đ, trong đó Tiền mặt là 50.000 đ và Chuyển khoản là 2.000.000 đ. Không bị mất 50.000 đ của khách lẻ.

- **Scenario 3: Lọc chính xác các gói hết hạn trong vòng 7 ngày**
  - **Given** Hôm nay là ngày 02/11/2026. Có gói A hết hạn ngày 05/11/2026 (còn 3 ngày) và gói B hết hạn ngày 20/11/2026 (còn 18 ngày).
  - **When** Lễ tân xem danh sách cảnh báo gói hết hạn.
  - **Then** Gói A xuất hiện trong danh sách cảnh báo; gói B không xuất hiện.
