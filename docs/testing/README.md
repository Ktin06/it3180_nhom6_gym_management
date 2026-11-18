# Kiểm thử và mã truy vết

Thư mục này chứa tài liệu kiểm thử của dự án. Quy ước mã đầy đủ và lý do nằm ở [ADR 0012](../decisions/0012-quy-uoc-ma-truy-vet.md). File này chỉ hướng dẫn cách làm.

## 1. Cây truy vết trong một ví dụ

```
F3 – Gói tập và gia hạn
 └─ UC-F3.3  Sửa thông tin gói tập                         (file SRS F3, mục 3.3)
     └─ US-F3.3.1  Là quản lý chi nhánh, tôi muốn sửa giá…  (SRS F3, mục 3.3.H; issue GitHub)
         ├─ AC-F3.3.1-01  Sửa giá; gói đã bán không đổi     (BR-F3-06)
         │    ├─ TC-F3.3.1-01a  PackageUpdateIntegrationTest#priceOnlyKeepsOmittedFieldsAndAudit
         │    └─ TC-F3.3.1-01b  PackageUpdateIntegrationTest#historyIncludingPendingPurchaseRemainsIdentical
         └─ AC-F3.3.1-02  Sửa gói chi nhánh khác → 403      (BR-F3-07)
              └─ TC-F3.3.1-02a  …
```

- US, AC, BR viết trong file SRS. File SRS là bản chuẩn.
- TC là chính các hàm kiểm thử trong mã. Mã TC nằm trong **tên hiển thị** của kiểm thử.

## 2. Đặt mã TC

### Quy tắc

1. Mỗi kiểm thử chứng minh một AC thì mang mã `TC-<mã AC><chữ>`. Chữ đi theo thứ tự `a`, `b`, `c`… trong cùng một AC.
2. Mã đứng **đầu** tên hiển thị, sau đó là dấu hai chấm và mô tả ngắn bằng tiếng Việt.
3. Một kiểm thử chứng minh nhiều AC thì ghi các mã cách nhau bằng dấu phẩy, ví dụ `TC-F3.3.1-01c, TC-F3.3.1-02b: …`. Nên hạn chế; tách kiểm thử thì dễ đọc hơn.
4. Kiểm thử thuần kỹ thuật không gắn với AC nào (JWT, ràng buộc database, cấu hình) thì ghi mã yêu cầu bảo mật hoặc mã BR thay cho mã TC, ví dụ `S2: …` hay `BR-F3-05: …`. Nếu không có mã nào phù hợp thì để tên bình thường; script sẽ xếp kiểm thử đó vào nhóm "kiểm thử kỹ thuật".
5. Trước khi đặt mã, mở SRS và kiểm tra AC đó có thật. Nếu kiểm thử chứng minh một hành vi mà SRS chưa có AC nào, hãy **thêm AC vào SRS trong cùng PR**, đừng bịa mã.
6. Gắn mã cho kiểm thử có sẵn thì **chỉ đổi tên hiển thị**. Không sửa logic hay assertion.

### Backend (JUnit 5)

```java
@Test
@DisplayName("TC-F3.3.1-01a: Chỉ sửa giá thì các trường khác giữ nguyên")
void priceOnlyKeepsOmittedFieldsAndAudit() { … }
```

Với `@ParameterizedTest`, đặt `@DisplayName` trên phương thức. Muốn hiện tham số trên từng lần chạy thì thêm `name`:

```java
@ParameterizedTest(name = "{0}, {1}")
@DisplayName("TC-F3.3.1-02a: Sửa gói của chi nhánh khác hoặc gói toàn chuỗi bị từ chối 403, dữ liệu giữ nguyên")
@CsvSource({"OTHER,ACTIVE", "GLOBAL,ACTIVE", "OTHER,INACTIVE", "GLOBAL,INACTIVE"})
void outsideBranchAndGlobalAre403WithoutMutation(String scope, String state) { … }
```

### Frontend (Vitest)

```ts
it('TC-F3.1.1-03a: lễ tân không thấy nút Sửa', async () => { … })
```

Có thể để mã ở `describe` khi mọi ca bên trong cùng chứng minh một AC:

```ts
describe('TC-F1.2.1-01a: đăng nhập thành công', () => {
  it('lưu access token trong bộ nhớ', …)
  it('chuyển tới trang theo vai trò', …)
})
```

### Postman (không bắt buộc)

Đặt mã ở đầu tên request, ví dụ `TC-F3.3.1-02a Sửa gói chi nhánh khác`.

## 3. Ma trận truy vết

### Ma trận truy vết là gì

Là một file tổng hợp **nối AC trong SRS với kiểm thử trong mã**: [`ma-tran-truy-vet.md`](ma-tran-truy-vet.md). AC nằm trong file SRS, TC nằm trong tên kiểm thử. Hai nơi này rời nhau, không ai nhìn bằng mắt mà biết AC nào đã được kiểm thử. Script đọc cả hai rồi ghép lại.

File gồm:

| Mục | Trả lời câu hỏi |
| --- | --- |
| 1. Tổng quan theo chức năng | Mỗi chức năng có bao nhiêu UC, US, AC; bao nhiêu AC đã có kiểm thử |
| 2. Lỗi cần sửa | Có TC nào ghi mã AC không tồn tại (gõ sai, hoặc AC đã bị đổi số)? Có mã AC nào trùng? |
| 3. AC chưa có TC | Còn AC nào chưa có kiểm thử chứng minh? |
| 4. Cây xuôi | Một US gồm những AC nào, mỗi AC được kiểm thử bởi hàm nào? |
| 5. Bảng ngược | Một kiểm thử báo lỗi thì nó vi phạm AC, US, quy tắc nghiệp vụ nào? |
| 6. Kiểm thử chưa mang mã | Còn bao nhiêu kiểm thử chưa gắn mã, nằm ở file nào? |

**File này do script sinh, không sửa tay.** Sửa tay sẽ bị ghi đè ở lần chạy sau.

### Cách chạy

Ở gốc repo (cần Node, không cần cài gói nào):

```bash
node scripts/export-truy-vet.cjs           # sinh lại docs/testing/ma-tran-truy-vet.md
node scripts/export-truy-vet.cjs --check   # chỉ kiểm tra, không ghi file
```

Dòng cuối in ra tóm tắt, ví dụ `56 UC, 6 US, 43 AC (31 có TC, 12 chưa có), 63 TC, …; lỗi: 0 TC trỏ sai, 0 AC trùng, 0 US lạc`. Có lỗi ở mục 2 thì lệnh thoát với mã 1, để sau này CI dùng được.

### Dùng khi nào

| Lúc | Ai | Làm gì |
| --- | --- | --- |
| Trước khi mở PR chức năng | Người làm | Chạy script, xem mục 3: mọi AC mà PR ghi "đã làm" không còn trong danh sách "AC chưa có TC". Xem mục 2 trống. Commit luôn file ma trận đã sinh lại |
| Khi review PR | Người duyệt | Mở diff của `ma-tran-truy-vet.md`: thấy ngay PR thêm TC cho AC nào |
| Khi viết lại SRS, đổi số AC | Người viết SRS | Chạy script: mục 2.1 báo các kiểm thử đang trỏ tới mã AC cũ |
| Trước khi đóng issue user story | Người nghiệm thu | Mục 4: US có dấu ✅ "mọi AC có TC" mới đủ điều kiện 2 trong 3 điều kiện đóng ([CONTRIBUTING.md, mục 5](../../CONTRIBUTING.md#5-định-nghĩa-hoàn-thành)) |
| Một kiểm thử báo đỏ trên CI | Bất kỳ ai | Tìm mã TC trong mục 5 để biết quy tắc nghiệp vụ nào đang bị vi phạm |
| Họp cuối sprint, viết báo cáo | Trưởng nhóm | Mục 1 cho số liệu độ phủ AC theo chức năng; mục 4 chèn được vào báo cáo làm bằng chứng truy vết |

### Ví dụ

Kiểm thử `PackageUpdateIntegrationTest#outsideBranchAndGlobalAre403WithoutMutation` báo lỗi. Tìm `TC-F3.3.1-02a` ở mục 5:

```
| TC-F3.3.1-02a | AC-F3.3.1-02 | US-F3.3.1 | UC-F3.3 | BR-F3-07, BR-F3-05, BR-F3-06 | PackageUpdateIntegrationTest#… |
```

Vậy mã vừa sửa đã làm hỏng quy tắc "quản lý không được sửa gói chi nhánh khác hoặc gói toàn chuỗi" (BR-F3-07), thuộc US sửa gói (#132).

### Script đọc gì

- **SRS:** tiêu đề `### 3.x. UC-Fx.y: …`, tiêu đề `##### US-Fx.y.z: …`, dòng `- **BR liên quan:**`, `- **Issue:**` và dòng AC `- **AC-Fx.y.z-nn: tên** (BR-…)`. Viết đúng mẫu đặc tả thì script đọc được. AC ghi "(bỏ ngày …)" không tính.
- **Backend:** mọi `@Test`, `@ParameterizedTest`, `@RepeatedTest` trong `backend/src/test/java`; mã TC lấy từ `@DisplayName`.
- **Frontend:** `it(…)`, `test(…)`, `describe(…)` trong file `*.test.ts(x)` của `frontend/src`; mã TC ở đầu tên.
