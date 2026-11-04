# Checklist tự review trước khi mở pull request

Danh sách các lỗi nhóm đã gặp khi review tài liệu và mã (PR #34–#45, #82, và đợt review ERD bản 2), cộng với các lỗi hay gặp khi để công cụ AI sinh mã. Dùng để tự kiểm tra **trước khi** mở pull request, bằng tay hoặc bằng công cụ AI bất kỳ. Checklist không thay cho [quy tắc mã nguồn](quy-tac-ma-nguon.md) và [CONTRIBUTING.md](../../CONTRIBUTING.md); khi mâu thuẫn thì hai file đó đúng.

Người duyệt cũng dùng checklist này. Duyệt nghĩa là đối chiếu từng dòng với tài liệu chuẩn, không phải đọc lướt; pull request dài mà duyệt nhanh thường để lọt lỗi.

## Cách dùng với công cụ AI

Dán yêu cầu sau vào công cụ AI đang dùng (Copilot, Antigravity, …), chạy ở thư mục gốc repo:

```text
Đọc docs/guides/quy-tac-ma-nguon.md và docs/guides/checklist-review.md.
Review các thay đổi của nhánh hiện tại so với main (git diff main...HEAD) theo từng mục của checklist.
Đọc hết các file liên quan trước khi kết luận; khi tìm kiếm thì xem đủ kết quả, không dừng ở vài dòng đầu.
Với mỗi lỗi: ghi file:dòng, vì sao sai, căn cứ ở tài liệu chuẩn nào (bản chốt phạm vi, erd-v2.dbml, ADR, biên bản họp), và cách sửa.
Chỉ báo lỗi có căn cứ; điều tài liệu chuẩn chưa nói thì ghi là "cần trưởng nhóm chốt", không tự đặt quy tắc.
Cuối kết quả, liệt kê: (1) các file và nhánh đã đọc; (2) các file, nhánh, đường link không đọc được hoặc chưa đọc (kể cả kết quả tìm kiếm bị cắt).
Kết luận nào dựa trên việc "không tìm thấy" thì ghi rõ đã tìm ở đâu.
Chưa sửa gì, chỉ liệt kê.
```

Xem danh sách "chưa đọc" và tự kiểm tra lại những phần đó; kết luận "không có" chỉ đúng trong phạm vi công cụ đã tìm. Đọc kết quả và tự kiểm tra lại từng lỗi trước khi sửa. Công cụ AI có thể báo sai, nhất là khi nó chỉ đọc một phần file hoặc chỉ tìm trên `main`. Đã có lần AI kết luận "điều kiện lọc theo chi nhánh nhà là sai" và sửa ngược, trong khi biên bản kick-off đã chốt đúng như vậy. Thấy kết luận nào ngược với bản gốc thì tìm căn cứ trước khi sửa.

## 1. Đối chiếu tài liệu chuẩn

- [ ] Quy tắc nghiệp vụ khớp [bản chốt phạm vi](../requirements/ban-chot-pham-vi.md), các [ADR](../decisions/) và [biên bản họp](../meetings/); không tự thêm quy tắc hay tính năng ngoài phạm vi.
- [ ] Tên bảng, cột, kiểu dữ liệu khớp [`erd-v2.dbml`](../design/erd/erd-v2.dbml). Ví dụ đã gặp: `packages` có cột `type`, còn `package_type` và `branch_id` của gói đã bán nằm ở `memberships`; khóa chính là `bigint`, không phải UUID.
- [ ] Không dùng endpoint, khóa `system_configs`, tên chỉ mục không có trong đặc tả hoặc ERD (ví dụ đã gặp: `OPENING_HOURS`, `EXPIRING_WARNING_DAYS`, `ix_invoices_branch_status`). Cần thêm thì ghi vào ERD hoặc đặc tả trong cùng pull request.
- [ ] Trước khi kết luận "không có" (không có endpoint, không có giá trị mặc định…), đã tìm hết: cả `main` lẫn các nhánh chung chưa gộp như `chore/db-schema-ban-dau`. Ví dụ đã gặp: kết luận "không tài liệu nào quy định giờ mở cửa 05:30" trong khi migration `V2_01` trên nhánh schema tạo chi nhánh CN01 mở 05:30–22:00; hoặc nói đặc tả F1 thiếu `/auth/me` vì kết quả tìm kiếm bị cắt mất dòng đó.
- [ ] Tham số có dấu \* trong bản chốt đọc từ `system_configs`, không viết cứng; tham số không có dấu \* thì để hằng số. Giá trị theo từng chi nhánh (giờ mở cửa, sức chứa) đọc từ bảng của chi nhánh, không viết cứng kiểu "06:00–22:00".

## 2. Ngày và giờ

- [ ] "Hôm nay" (`:today`, `:dayStart`, `:dayEnd`) do ứng dụng tính theo `Asia/Ho_Chi_Minh` rồi truyền vào truy vấn; không dùng `CURRENT_DATE` hay `now()::date` của PostgreSQL (máy chủ chạy UTC, từ 00:00 đến 07:00 giờ Việt Nam vẫn là hôm trước).
- [ ] Khoảng ngày dùng nửa mở (`>= đầu AND < cuối`), không dùng `BETWEEN` (đã gặp: "7 ngày tới" thành 8 ngày).
- [ ] `end_date` là ngày cuối còn dùng được: `end_date = start_date + duration_days - 1`.
- [ ] Giờ mở cửa: được thao tác khi `open_time <= giờ hiện tại < close_time`; đúng giờ đóng cửa là ngoài giờ.
- [ ] Lọc theo thời điểm (`checked_in_at`) khi ERD đã có chỉ mục cho cột đó, thay vì lọc theo cột ngày rồi bỏ phí chỉ mục.

## 3. Trạng thái gói và ràng buộc

- [ ] Đổi trạng thái gói không vi phạm `uq_one_active_per_type`: chuyển gói cũ sang `EXPIRED` và flush **trước**, rồi mới kích hoạt gói mới.
- [ ] Có gói `UPCOMING` nối tiếp thì đã xét nó khi gói hiện tại đổi ngày (bảo lưu, hết buổi trước hạn).
- [ ] Quy trình có bước chờ (chờ duyệt, chờ thanh toán) thì điều kiện được **kiểm tra lại** ở bước hoàn tất, không chỉ lúc tạo yêu cầu.
- [ ] Tác vụ nền: nghĩ tới trường hợp chạy trễ, chạy lặp, chạy lỗi; chạy lặp không tạo sai dữ liệu.
- [ ] Chỉ mục duy nhất trên cột có thể rỗng dùng `NULLS NOT DISTINCT` (PostgreSQL coi hai giá trị rỗng là khác nhau, nên hai gói toàn chuỗi cùng loại lọt qua).
- [ ] Mỗi trạng thái trong `CHECK` đều có đường vào và đường ra được đặc tả; mỗi lần chuyển trạng thái là một quy tắc cần kiểm thử.

## 4. Đồng thời và lỗi

- [ ] Bảng có `version` thì cập nhật bằng khóa lạc quan; cần thông báo lỗi riêng thì ghi bằng `saveAndFlush` trong service, bắt lỗi xung đột rồi ném `ConflictException` (nếu không, `GlobalExceptionHandler` chỉ trả thông báo chung).
- [ ] Kiểm tra trùng giờ khi thêm dòng mới thì khóa **dòng cha** theo đúng thứ tự NF6 ghi trong ERD (ví dụ `members` trước, `class_sessions` sau), vì dòng mới chưa có để khóa.
- [ ] Không "tự thử lại" trong cùng giao dịch sau khi cơ sở dữ liệu báo vi phạm ràng buộc: PostgreSQL đã hủy cả giao dịch đó.
- [ ] Mỗi lỗi có mã HTTP đúng quy ước (`404` không tìm thấy, `409` xung đột, `422` vi phạm quy tắc) và thông báo tiếng Việt qua các ngoại lệ có sẵn trong `common/exception/`; không nuốt lỗi (`catch` rỗng) hay trả `500` cho lỗi nghiệp vụ.

## 5. Phân quyền và dữ liệu

- [ ] Vai trò và chi nhánh kiểm tra ở backend trên **mọi** API (S3), kể cả API phụ như biểu đồ, danh sách, check-out; hội viên không xem được dữ liệu người khác. Giấu nút trên giao diện không phải là phân quyền.
- [ ] Tham số trỏ tới bản ghi không tồn tại thì trả `404`, không trả danh sách rỗng hay số 0 như thể dữ liệu thật. Bản ghi thuộc chi nhánh khác cũng trả `404` để không lộ là nó tồn tại.
- [ ] Vai trò không gắn chi nhánh (chủ chuỗi không có dòng `staff`) thì đặc tả nói rõ lấy `branch_id` từ đâu.
- [ ] Tác nhân trong bảng use case, mục tiền điều kiện và quyền của API khớp nhau.
- [ ] Không tin dữ liệu trình duyệt gửi lên cho số tiền, giá, cờ phạt, `memberId` của chính người gọi (S7); server tự tính hoặc lấy từ phiên đăng nhập.

## 6. Mã do AI sinh

- [ ] Không có API, thư viện, hàm, cột "bịa": mọi lời gọi đều có thật trong mã nguồn, ERD hoặc tài liệu của thư viện ở đúng phiên bản dự án dùng.
- [ ] Không thêm thư viện mới khi chưa cần; nếu thêm thì ghi lý do trong mô tả pull request. `package-lock.json`, `pom.xml` không đổi ngoài ý muốn.
- [ ] Dùng lại phần dùng chung có sẵn (`BaseEntity`, `PageResponse`, các ngoại lệ trong `common/exception/`, `services/http.ts`) thay vì viết lại một bản tương tự.
- [ ] Không sửa file ngoài phạm vi issue; pull request nhỏ, một việc. Không bỏ công cụ mà người khác đang dùng (ví dụ công cụ giả lập vai trò khi backend chưa sẵn sàng).
- [ ] Không còn `TODO`, mã giả lập, `console.log`, mã chết, giá trị tạm như `localhost` hay mật khẩu mẫu ngoài chỗ đã quy ước.
- [ ] Không ghi mật khẩu, token, khóa bí mật vào mã, log hay tài liệu; khóa bí mật chỉ nằm trong `.env` hoặc biến môi trường.
- [ ] Truy vấn dùng tham số (không nối chuỗi SQL), có phân trang với danh sách, không gọi cơ sở dữ liệu trong vòng lặp.
- [ ] Ghi chú trong mã khớp với mã (đã gặp: ghi chú "17 cột" trong khi chỉ vẽ 16 cột).

## 7. Nhất quán trong tài liệu và sơ đồ

- [ ] Mỗi thông tin chỉ lưu một nơi (ví dụ: user story nằm ở issue, file trong repo chỉ giữ bảng chỉ mục).
- [ ] Cùng một quy tắc không được ghi nhiều kiểu trong cùng tài liệu (đã gặp: khung "đã đến lớp" ghi 3 cách khác nhau; E6 vừa nói trả 409 vừa nói tự xử lý không báo lỗi).
- [ ] Số bước, số kịch bản được tham chiếu đúng ("tại bước 8" đúng là bước 8); kịch bản đánh số theo thứ tự. Sửa tài liệu mà người khác đang tham chiếu số bước thì gộp vào bước có sẵn, không chèn bước mới làm lệch số.
- [ ] Ví dụ số liệu tự kiểm được: tổng các phần bằng tổng chung, ngày tháng tính lại khớp, wireframe khớp đặc tả và dữ liệu mẫu.
- [ ] File `.drawio.svg` mở được bằng Draw.io: phần dữ liệu `mxGraphModel` bên trong không rỗng. AI hay sinh SVG chỉ để xem, mở bằng Draw.io thì ra trang trắng. Sửa sơ đồ thì mở bằng Draw.io, không sửa thẳng mã SVG.
- [ ] Đã sửa ngày ở dòng "Cập nhật" đầu file và thêm dòng vào lịch sử thay đổi (nếu file có); quyết định mới được ghi vào bản chốt hoặc ADR, không chỉ trong đặc tả.
- [ ] Viết tiếng Việt, ngắn gọn; chỉ giữ nguyên tên bảng, cột, trạng thái, tham số, tên lớp trong mã. Không lặp lại các bảng cột đã có trong ERD, chỉ dẫn link. Câu hỏi còn mở ghi rõ người phải trả lời, không viết "cần owner xác nhận".

## 8. Kiểm thử

- [ ] Mỗi quy tắc nghiệp vụ có kịch bản Given/When/Then, gồm cả trường hợp bị từ chối và giá trị biên (đúng 2 giờ, đúng 30 phút, đúng ngày hết hạn).
- [ ] Mã có kiểm thử JUnit cho từng quy tắc; API mới có request Postman trong `postman/`.
- [ ] Kiểm thử kiểm tra hành vi thật, không chỉ kiểm tra mock; không sửa assertion cho "qua", không tắt kiểm thử (`@Disabled`, `.skip`).
- [ ] Đã thật sự chạy `./mvnw -B verify` hoặc `npm run lint && npm test -- --run && npm run build`. Không chạy được (ví dụ Docker chưa bật) thì ghi rõ trong pull request, không ghi là đã qua.

## 9. Git và pull request

- [ ] Không ở nhánh `main`; tên nhánh và commit theo [CONTRIBUTING.md](../../CONTRIBUTING.md).
- [ ] `git status` trước khi `git add`: không lọt file cấu hình AI cá nhân, thư mục riêng, `.env`, file Excel hay file tạm. Không dùng `git add .` khi chưa xem danh sách.
- [ ] Commit message và mô tả pull request không ghi tên công cụ AI (kể cả dòng `Co-Authored-By` hay "Generated with …" do công cụ tự thêm).
- [ ] Mô tả pull request có `Closes #<số>` (mỗi issue một dòng), gán đúng người duyệt theo bảng cặp duyệt; không đoán người duyệt theo nội dung.
- [ ] Ô đánh dấu trong checklist pull request chỉ tích khi đã làm thật (đã gặp: tích "khớp bản chốt phạm vi và ERD", "mở được bằng Draw.io" trong khi chưa đúng).
- [ ] Đã tự đọc lại toàn bộ thay đổi và giải thích được.
