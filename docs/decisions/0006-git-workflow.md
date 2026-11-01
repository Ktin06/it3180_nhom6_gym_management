# ADR 0006: Quy trình Git – main và nhánh tính năng

- **Ngày:** 01/11/2026
- **Người quyết định:** Hoàng Kim Tiến (trưởng nhóm), cần cả nhóm xác nhận
- **Trạng thái:** Đã chấp nhận

## Bối cảnh

8 người cùng làm trên một repo, phần lớn lần đầu làm việc nhóm với Git. Cần quy trình đơn giản, ít nhánh, khó làm hỏng `main`. Repo để chế độ công khai (public); trên repo công khai, GitHub cho bật ruleset bảo vệ nhánh miễn phí, không phụ thuộc gói trả phí của ai. Cần thêm lớp chặn ở máy cá nhân để báo lỗi sớm, trước khi đẩy lên GitHub.

## Quyết định

- Chỉ có `main` và các nhánh ngắn hạn tách từ `main` (`feature/F2-ho-so-hoi-vien`, `fix/F4-...`, `docs/...`, `test/F8-...`, `chore/...`). **Không** dùng nhánh `develop`.
- `main` luôn chạy được và là nguồn triển khai staging.
- Mọi thay đổi vào `main` qua pull request: ít nhất 1 người duyệt, mọi bình luận review đã được xử lý (resolve), CI xanh, **squash merge**, xóa nhánh sau khi gộp.
- GitHub chặn bằng ruleset `protect-main` cho `main` (cấu hình ở mục Hệ quả); **không ai được vượt**, kể cả chủ repo.
- Hook `.githooks/pre-push` chặn đẩy thẳng lên `main` ở máy cá nhân. Mỗi người chạy `git config core.hooksPath .githooks` ngay sau khi clone. Không dùng `--no-verify` để vượt hook. Ngoại lệ duy nhất: hook cho phép tạo nhánh `main` khi repo trên GitHub còn trống (lần đẩy commit đầu tiên).
- Commit theo Conventional Commits, phạm vi là mã chức năng (`feat(F2): ...`).

## Phương án đã cân nhắc

| Phương án | Lý do không chọn |
| --- | --- |
| Git Flow (main + develop + release) | Thừa với dự án 4 sprint; thêm một nhánh phải đồng bộ, dễ nhầm nơi tách nhánh |
| Merge commit thay vì squash | Lịch sử `main` lẫn commit "sửa lỗi chính tả", khó đọc khi viết báo cáo |
| Chỉ dựa vào ruleset của GitHub, bỏ hook | GitHub chỉ báo lỗi sau khi đã đẩy; hook chặn sớm ở máy, trước khi commit rời khỏi máy |
| Cho chủ repo vượt ruleset để gộp gấp | Trái quy tắc "không đẩy thẳng lên `main`"; mọi thay đổi đều cần người thứ hai duyệt |
| Để repo riêng tư, bảo vệ nhánh bằng GitHub Pro (gói Student của trưởng nhóm) | Ruleset phụ thuộc gói của một người, mất tác dụng khi gói hết hạn; repo công khai không cần gói nào |

## Hệ quả

- Lịch sử `main` mỗi pull request một commit, dễ đối chiếu với issue và mã chức năng.
- Hook chỉ có tác dụng khi người dùng đã chạy lệnh `git config core.hooksPath`; cần nhắc trong hướng dẫn cài đặt và CONTRIBUTING.
- Người mở pull request không tự duyệt được pull request của mình; mỗi thay đổi cần người thứ hai đọc.
- Repo công khai nên mọi người đều đọc được: không bao giờ commit `.env`, mật khẩu, khóa bí mật hay dữ liệu cá nhân thật (số điện thoại, email của người ngoài nhóm); dữ liệu mẫu dùng giá trị giả.
- Ruleset `protect-main` đã tạo sẵn ở trạng thái **Disabled** để lần đẩy commit đầu tiên tạo được nhánh `main` (ruleset nhắm nhánh mặc định có thể chặn cả lần tạo nhánh). Trưởng nhóm hoàn tất cấu hình **sau** lần đẩy đó:
  1. Settings → General → Pull Requests: chỉ bật **Allow squash merging**; bật **Automatically delete head branches**.
  2. Settings → Rules → Rulesets → `protect-main`: Enforcement **Active**; Bypass list **để trống**; Target **Include default branch**.
  3. Bật các quy tắc: **Restrict deletions**; **Block force pushes**; **Require a pull request before merging** (Required approvals = 1, bật **Dismiss stale pull request approvals when new commits are pushed** và **Require conversation resolution before merging**, Allowed merge methods = **chỉ Squash**); **Require status checks to pass** với hai kiểm tra `Backend` và `Frontend` (tên job trong `.github/workflows/ci.yml`; hai kiểm tra chỉ hiện trong danh sách sau khi CI đã chạy ít nhất một lần).
  4. Mời 7 thành viên vào repo với quyền **Write** (Settings → Collaborators).
- Nếu sau này chuyển repo về riêng tư mà không có GitHub Pro, ruleset ngừng có tác dụng; khi đó hook và quy tắc "chỉ bấm gộp khi đủ 1 lượt duyệt và CI xanh" trong CONTRIBUTING là lớp chặn còn lại.
