# ADR 0012: Quy ước mã truy vết Chức năng → UC → US → AC → TC

- **Ngày:** 18/11/2026
- **Người quyết định:** Hoàng Kim Tiến (trưởng nhóm), chốt ngày 18/11/2026 trước khi tạo issue chia việc Sprint 2
- **Trạng thái:** Đã chấp nhận

## Bối cảnh

Trong buổi demo Sprint 1, cô nhận xét nhóm 2, 5 và 9 theo cùng một hướng. Đặc tả phải chia theo chức năng và truy vết được theo cây:

```
Chức năng → UC (use case) → US (user story) → AC (tiêu chí chấp nhận) → TC (ca kiểm thử)
```

Cây phải đọc được theo hai chiều:

- **Chiều xuôi:** AC này đã có kiểm thử chưa?
- **Chiều ngược:** kiểm thử này báo lỗi thì vi phạm AC nào, US nào, quy tắc nghiệp vụ (BR) nào?

Hiện trạng trên `main` ngày 17/11/2026:

- **UC:** có 56 UC, mã `UC-F<x>.<y>`, mỗi chức năng một file trong `docs/requirements/srs/`. Phần này đã đạt.
- **US:** có 31 US ở issue #46–#76. Mỗi US lại gom nhiều UC, ngược với cây. Ví dụ US-F3.1 (#55) gom UC-F3.1 đến UC-F3.4. Mã `US-F8.1` (lịch lớp lặp) còn trùng số với `UC-F8.1` (đặt chỗ lớp), nhưng nói về hai việc khác nhau.
- **AC:** 117 kịch bản trong issue, cộng một phần trong SRS. Không AC nào có mã. Hai nơi đã có chỗ lệch nhau.
- **BR:** F3–F10 có mã nhưng theo 3 kiểu khác nhau. F1, F2 chưa có mã.
- **TC:** khoảng 201 kiểm thử backend và 69 kiểm thử frontend. Không kiểm thử nào mang mã.

Sprint 2 chưa ai bắt đầu làm, và nhóm sắp tạo issue để chia việc. Đây là lúc đổi quy ước ít tốn kém nhất.

Chi tiết khảo sát: báo cáo nội bộ ngày 17/11/2026 của trưởng nhóm.

## Quyết định

### 1. Dạng mã

| Tầng | Dạng mã | Ví dụ | Ý nghĩa |
| --- | --- | --- | --- |
| UC | `UC-F<x>.<y>` | `UC-F3.3` | Giữ nguyên 56 mã đang có |
| US | `US-F<x>.<y>.<z>` | `US-F3.3.1` | US thứ `z` của `UC-F<x>.<y>` |
| AC | `AC-F<x>.<y>.<z>-<nn>` | `AC-F3.3.1-02` | AC thứ `nn` (hai chữ số) của US đó |
| TC | `TC-F<x>.<y>.<z>-<nn><chữ>` | `TC-F3.3.1-02a` | Kiểm thử thứ nhất (`a`) của AC-F3.3.1-02 |
| BR | `BR-F<x>-<nn>` | `BR-F3-06` | Xem mục 5 |

Đọc mã là biết vị trí trên cây, không cần tra bảng. Ví dụ `TC-F3.3.1-02a` thuộc F3 → UC-F3.3 → US-F3.3.1 → AC-F3.3.1-02.

**Mã đã dùng thì không dùng lại.** Bỏ một AC thì giữ dòng đó, ghi "(bỏ ngày dd/mm/yyyy, lý do)". Không đánh lại số. Nếu đánh lại, kiểm thử cũ mang mã cũ sẽ trỏ nhầm sang AC khác nghĩa.

### 2. Giữ nguyên mã UC (Q1)

Không gom UC thành UC lớn kiểu "Quản lý gói tập". Mã UC đang xuất hiện ở 29 file, gồm 7 sơ đồ use case, wireframe, bảng truy vết, Postman và chú thích trong mã.

Hệ quả chấp nhận được: nhiều UC chỉ có một US, ví dụ UC-F1.4 (đổi mật khẩu) chỉ có US-F1.4.1. Lý do là UC của nhóm vốn nhỏ, mỗi UC một thao tác.

### 3. File SRS là bản chuẩn của US và AC (Q2)

- US, AC và BR viết trong file đặc tả của chức năng: `docs/requirements/srs/dac-ta-use-case-f<x>-*.md`. Đặt trong mục H của từng UC, theo [mẫu đặc tả](../requirements/srs/mau-dac-ta-use-case.md).
- Issue **không chép lại** nội dung AC. Issue chỉ ghi mã US, danh sách mã AC và đường dẫn tới SRS.
- Lý do: file trong Git có lịch sử thay đổi và được duyệt qua PR. Công cụ AI cũng đọc trực tiếp được. Đây đúng là ý "spec theo chức năng để Agent làm việc" của cô.
- Câu "issue là bản chuẩn" trong [user-stories-f1-f10.md](../requirements/user-stories-f1-f10.md) hết hiệu lực từ ADR này.

### 4. Bỏ mã US cũ hai cấp; mỗi US mới là một issue (Q3)

- Bỏ hẳn các mã `US-F<x>.<y>` cũ. Nếu để chúng song song với mã mới, người đọc sẽ tưởng `US-F2.1` (cũ, gom 3 UC) có liên quan tới `US-F2.1.1` (mới, thuộc UC-F2.1).
- **Mỗi US mới là một issue** dùng mẫu "User story", tiêu đề dạng `[F3] US-F3.3.1 — Sửa thông tin gói`.
- 31 issue cũ #46–#76 được đóng với lý do *not planned* (không làm theo issue này). Mỗi issue cũ có một bình luận liệt kê các issue US mới thay thế nó. 4 issue đã đóng (#46, #47, #48, #52) chỉ cần thêm bình luận đó.
- [user-stories-f1-f10.md](../requirements/user-stories-f1-f10.md) đổi thành bảng ánh xạ US cũ → US mới, ghi một lần.
- Cách này được chọn thay cho "giữ 31 issue, đổi nội dung" vì Sprint 2 chưa bắt đầu. Chưa có task hay PR nào trỏ vào issue cũ, nên đổi bây giờ không làm gãy liên kết nào.

### 5. Mã BR (Q4)

- Giữ nguyên mọi mã BR đang có ở F3–F10, dù có 3 kiểu (`BR-F3-06`, `BR-F4.7`, `BR-F8.6-03`). Đổi hết sẽ chạm khoảng 130 chỗ, có cả ADR 0010 và các issue F8.
- F1, F2 thêm mã mới theo dạng `BR-F<x>-<nn>`.
- **Mọi BR thêm mới sau ADR này** dùng dạng `BR-F<x>-<nn>`, số tiếp theo trong chức năng đó.
- Mỗi US có dòng "BR liên quan". Mỗi AC ghi mã BR mà nó kiểm chứng, nếu có.

### 6. Phạm vi đợt đầu (Q5)

- **F1–F3:** làm đủ tới TC. Đây là phần đã có mã nguồn và kiểm thử, và cũng là phần sẽ demo.
- **F4–F10:** issue user story được tạo ngay (mỗi UC ít nhất một US), chưa có mã AC. Người phụ trách từng chức năng viết US và AC có mã vào SRS **trước khi mở PR chức năng đầu tiên** (hạn 21/11/2026), rồi ghi danh sách mã AC vào issue. TC được gắn khi viết mã chức năng.
- **F11–F20:** viết theo quy ước này ngay từ đầu, khi tới version tương ứng.

### 7. Điểm lệch giữa SRS và issue cũ (Q6)

Khi gộp AC từ issue cũ vào SRS, nếu hai bên ghi khác nhau thì người soạn **không tự chọn**. Ví dụ: SRS F2 ghi hội viên xem hồ sơ người khác bị trả `403`, nhưng backend trả `404`.

Người soạn (người hoặc công cụ AI) liệt kê mọi điểm lệch thành bảng trong mô tả PR. Trưởng nhóm hoặc người phụ trách chức năng chọn bản đúng, theo thứ tự ưu tiên: bản chốt phạm vi, ERD, rồi ADR.

### 8. Gắn mã vào kiểm thử

- **Backend (JUnit):** `@DisplayName("TC-F3.3.1-02a: Sửa gói chi nhánh khác bị từ chối 403")`.
- **Frontend (Vitest):** mã đứng đầu tên ca, ví dụ `it('TC-F3.1.1-03a: lễ tân không thấy nút Sửa', …)`.
- Gắn mã cho kiểm thử có sẵn **chỉ đổi tên hiển thị, không đổi logic kiểm thử**.
- Kiểm thử thuần kỹ thuật (JWT, ràng buộc database, cấu hình) không gắn với AC nào. Những kiểm thử này mang mã yêu cầu bảo mật (`S1`–`S10`) hoặc mã BR thay cho mã TC.
- Cú pháp chi tiết nằm trong [docs/testing/README.md](../testing/README.md).

### 9. Issue, PR và đóng user story

- **Issue task** ghi "Thuộc user story: #<số> (US-F…)" và các mã AC mà task làm.
- **PR** ghi `Closes #<task>` và `Refs #<user story>`. Không bao giờ ghi `Closes` cho user story, vì một US thường cần nhiều PR. Đã có trường hợp #46 bị GitHub tự đóng qua PR #114, một PR sửa lỗi nhỏ.
- **User story được đóng bằng tay** khi đủ 3 điều kiện:
  1. Mọi task thuộc US đã đóng.
  2. Mọi AC của US có ít nhất một TC chạy qua.
  3. Một người khác người làm đã thử từng AC trên staging (gọi là *nghiệm thu*), rồi để lại bình luận ghi ngày, tài khoản thử và kết quả từng AC.
- Hạn chót là buổi review cuối sprint. US chưa đạt thì chuyển sang sprint sau, không đóng.

## Phương án đã cân nhắc

| Câu hỏi | Phương án đã chọn | Phương án bỏ | Vì sao bỏ |
| --- | --- | --- | --- |
| Mã UC | Giữ 56 UC, thêm tầng US bên dưới | Gom thành UC lớn | Phải sửa 29 file, gồm 7 sơ đồ use case |
| Bản chuẩn của AC | File SRS | Issue | Không có lịch sử duyệt qua PR; công cụ AI khó đọc; đã lệch với SRS |
| Issue user story | Mỗi US mới một issue, đóng 31 issue cũ | Giữ 31 issue, chỉ đổi nội dung | Một issue chứa nhiều US thì không đóng từng phần được, và khó khớp với cây. Ưu điểm "không gãy liên kết" không còn giá trị vì Sprint 2 chưa bắt đầu |
| Mã BR | Giữ mã cũ, chuẩn hóa mã mới | Đổi toàn bộ về một kiểu | Khoảng 130 chỗ phải sửa, có cả ADR 0010 |
| Nơi lưu TC | Mã đặt trong tên kiểm thử, ma trận sinh tự động | Bảng TC viết tay | Bảng viết tay sẽ lệch với mã chỉ sau vài tuần |

## Hệ quả

- **Dễ hơn:**
  - Hỏi "AC này kiểm thử ở đâu" thì tìm mã là ra.
  - Một kiểm thử báo lỗi thì biết ngay nó vi phạm US và BR nào.
  - Công cụ AI đọc SRS theo mã US, AC trước khi sinh mã.
  - Đóng user story có tiêu chí khách quan.
- **Khó hơn:**
  - Phải tách lại 31 US. Việc này cần hiểu nghiệp vụ, không đổi tên máy móc được.
  - Số issue tăng từ 31 lên khoảng 60.
  - Kiểm thử phải mang mã. Thiếu công cụ kiểm tra thì mã sẽ lệch dần.
- **Cần theo dõi:** viết script `scripts/export-truy-vet.cjs`, cùng kiểu với `export-erd.cjs`. Script sinh `docs/testing/ma-tran-truy-vet.md` gồm: cây xuôi, bảng ngược, danh sách AC chưa có TC, và danh sách TC trỏ tới AC không tồn tại. Về sau có thể cho CI chạy script này.
- **Cần cập nhật theo, theo thứ tự:**
  1. Cùng PR với ADR này: [mẫu đặc tả UC](../requirements/srs/mau-dac-ta-use-case.md), mẫu issue user story và task, mẫu PR, [checklist review](../guides/checklist-review.md), [CONTRIBUTING.md](../../CONTRIBUTING.md), [quy tắc mã nguồn](../guides/quy-tac-ma-nguon.md), [docs/testing/README.md](../testing/README.md).
  2. Viết lại SRS F3, F1, F2 theo cây, mỗi chức năng một PR, kèm bảng điểm lệch (mục 7). Riêng F2 cần xử lý PR #35 (sửa đặc tả F1, F2) trước.
  3. Viết script sinh ma trận truy vết.
  4. Gắn mã TC cho kiểm thử F1–F3. Riêng F1 chờ PR #116 (khóa refresh token) được gộp.
  5. Tạo issue cho từng US mới và issue task Sprint 2, đóng 31 issue cũ, đổi `user-stories-f1-f10.md` thành bảng ánh xạ, đổi cột Issue trong [kế hoạch Sprint 2](../plans/sprint-2-ke-hoach.md) sang mã US mới.
  6. Viết lại SRS F4–F10 tới tầng AC, hạn 21/11/2026. Người phụ trách chức năng nào viết chức năng đó; Nguyễn Anh Tú rà cho đồng đều trong việc S2-32.
