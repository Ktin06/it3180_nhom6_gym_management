# Checklist tự review trước khi mở pull request

Danh sách các lỗi nhóm hay gặp khi review tài liệu và mã (gom từ các lần review PR #40–#45, #82). Dùng để tự kiểm tra **trước khi** mở pull request, bằng tay hoặc bằng công cụ AI bất kỳ. Checklist không thay cho [quy tắc mã nguồn](quy-tac-ma-nguon.md) và [CONTRIBUTING.md](../../CONTRIBUTING.md); khi mâu thuẫn thì hai file đó đúng.

## Cách dùng với công cụ AI

Dán yêu cầu sau vào công cụ AI đang dùng (Copilot, Antigravity, …), chạy ở thư mục gốc repo:

```text
Đọc docs/guides/quy-tac-ma-nguon.md và docs/guides/checklist-review.md.
Review các thay đổi của nhánh hiện tại so với main (git diff main...HEAD) theo từng mục của checklist.
Với mỗi lỗi: ghi file:dòng, vì sao sai, căn cứ ở tài liệu chuẩn nào (bản chốt phạm vi, erd-v2.dbml, ADR), và cách sửa.
Chỉ báo lỗi có căn cứ; điều tài liệu chuẩn chưa nói thì ghi là "cần trưởng nhóm chốt", không tự đặt quy tắc.
Chưa sửa gì, chỉ liệt kê.
```

Đọc kết quả và tự kiểm tra lại từng lỗi trước khi sửa: công cụ AI có thể báo sai, nhất là khi nó chỉ đọc một phần file.

## 1. Đối chiếu tài liệu chuẩn

- [ ] Quy tắc nghiệp vụ khớp [bản chốt phạm vi](../requirements/ban-chot-pham-vi.md) và các [ADR](../decisions/); không tự thêm quy tắc hay tính năng ngoài phạm vi.
- [ ] Tên bảng, cột, kiểu dữ liệu khớp [`erd-v2.dbml`](../design/erd/erd-v2.dbml). Ví dụ đã gặp: `packages` có cột `type`, còn `package_type` và `branch_id` của gói đã bán nằm ở `memberships`; khóa chính là `bigint`, không phải UUID.
- [ ] Không dùng endpoint, khóa `system_configs`, tên chỉ mục không có trong đặc tả hoặc ERD (ví dụ đã gặp: `OPENING_HOURS`, `EXPIRING_WARNING_DAYS`). Cần thêm thì ghi vào ERD hoặc đặc tả trong cùng pull request.
- [ ] Tham số có dấu \* trong bản chốt đọc từ `system_configs`, không viết cứng; tham số không có dấu \* thì để hằng số.

## 2. Ngày và giờ

- [ ] "Hôm nay" (`:today`, `:dayStart`, `:dayEnd`) do ứng dụng tính theo `Asia/Ho_Chi_Minh` rồi truyền vào truy vấn; không dùng `CURRENT_DATE` hay `now()::date` của PostgreSQL (máy chủ chạy UTC, từ 00:00 đến 07:00 giờ Việt Nam vẫn là hôm trước).
- [ ] Khoảng ngày dùng nửa mở (`>= đầu AND < cuối`), không dùng `BETWEEN`.
- [ ] `end_date` là ngày cuối còn dùng được: `end_date = start_date + duration_days - 1`.
- [ ] Giờ mở cửa: được thao tác khi `open_time <= giờ hiện tại < close_time`; đúng giờ đóng cửa là ngoài giờ.

## 3. Trạng thái gói và ràng buộc

- [ ] Đổi trạng thái gói không vi phạm `uq_one_active_per_type`: chuyển gói cũ sang `EXPIRED` và flush **trước**, rồi mới kích hoạt gói mới.
- [ ] Có gói `UPCOMING` nối tiếp thì đã xét nó khi gói hiện tại đổi ngày (bảo lưu, hết buổi trước hạn).
- [ ] Quy trình có bước chờ (chờ duyệt, chờ thanh toán) thì điều kiện được **kiểm tra lại** ở bước hoàn tất, không chỉ lúc tạo yêu cầu.
- [ ] Tác vụ nền: nghĩ tới trường hợp chạy trễ, chạy lặp, chạy lỗi; chạy lặp không tạo sai dữ liệu.

## 4. Đồng thời và lỗi

- [ ] Bảng có `version` thì cập nhật bằng khóa lạc quan; cần thông báo lỗi riêng thì ghi bằng `saveAndFlush` trong service, bắt lỗi xung đột rồi ném `ConflictException` (nếu không, `GlobalExceptionHandler` chỉ trả thông báo chung).
- [ ] Khóa bản ghi theo đúng thứ tự NF6 ghi trong ERD (ví dụ `members` trước, `class_sessions` sau).
- [ ] Mỗi lỗi có mã HTTP đúng quy ước (`404` không tìm thấy, `409` xung đột, `422` vi phạm quy tắc) và thông báo tiếng Việt.

## 5. Phân quyền và dữ liệu trả về

- [ ] Vai trò và chi nhánh kiểm tra ở backend trên mọi API (S3); hội viên không xem được dữ liệu người khác.
- [ ] Tham số trỏ tới bản ghi không tồn tại thì trả `404`, không trả danh sách rỗng hay số 0 như thể dữ liệu thật.
- [ ] Tác nhân trong bảng use case, mục tiền điều kiện và quyền của API khớp nhau.

## 6. Nhất quán trong tài liệu

- [ ] Mỗi thông tin chỉ lưu một nơi (ví dụ: user story nằm ở issue, file trong repo chỉ giữ bảng chỉ mục).
- [ ] Số bước, số kịch bản được tham chiếu đúng ("tại bước 8" đúng là bước 8); kịch bản đánh số theo thứ tự.
- [ ] Ví dụ số liệu tự kiểm được: tổng các phần bằng tổng chung, ngày tháng tính lại khớp, wireframe khớp đặc tả.
- [ ] Đã sửa ngày ở dòng "Cập nhật" đầu file và thêm dòng vào lịch sử thay đổi (nếu file có); quyết định mới được ghi vào bản chốt hoặc ADR, không chỉ trong đặc tả.
- [ ] Viết tiếng Việt; chỉ giữ nguyên tên bảng, cột, trạng thái, tham số, tên lớp trong mã.

## 7. Kiểm thử

- [ ] Mỗi quy tắc nghiệp vụ có kịch bản Given/When/Then, gồm cả trường hợp bị từ chối và giá trị biên (đúng 2 giờ, đúng 30 phút, đúng ngày hết hạn).
- [ ] Mã có kiểm thử JUnit cho từng quy tắc; API mới có request Postman trong `postman/`.

## 8. Git

- [ ] Không ở nhánh `main`; tên nhánh và commit theo [CONTRIBUTING.md](../../CONTRIBUTING.md).
- [ ] Commit message và mô tả pull request không ghi tên công cụ AI.
- [ ] Đã tự đọc lại toàn bộ thay đổi và giải thích được.
