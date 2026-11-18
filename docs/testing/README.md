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

File `ma-tran-truy-vet.md` trong thư mục này sẽ do script `scripts/export-truy-vet.cjs` sinh tự động. **Không sửa tay file đó.**

Script đọc mã US, AC, BR trong SRS và mã TC trong các file kiểm thử, rồi sinh ra:

- cây xuôi F → UC → US → AC → TC;
- bảng ngược TC → AC → US → UC → BR;
- danh sách **AC chưa có TC**;
- danh sách **TC trỏ tới AC không tồn tại**.

Danh sách "AC chưa có TC" của một US phải trống thì US đó mới đủ điều kiện nghiệm thu và đóng ([CONTRIBUTING.md, mục 5](../../CONTRIBUTING.md#5-định-nghĩa-hoàn-thành)).

Script đang được viết. Khi xong, cách chạy sẽ được ghi vào đây.
