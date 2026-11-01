# Quy ước làm việc

Áp dụng cho cả 8 thành viên và cho mọi mã do công cụ AI sinh ra. Lý do của từng quy ước nằm trong [docs/decisions/](docs/decisions/).

## 1. Thiết lập lần đầu

Sau khi clone, chạy **ngay**:

```bash
git config core.hooksPath .githooks
```

Lệnh này bật hook `pre-push` chặn đẩy thẳng lên `main`. Cài đặt đầy đủ: [docs/guides/cai-dat-moi-truong.md](docs/guides/cai-dat-moi-truong.md). Quy tắc viết mã bắt buộc: [docs/guides/quy-tac-ma-nguon.md](docs/guides/quy-tac-ma-nguon.md).

## 2. Nhánh

- `main` luôn chạy được và là nguồn triển khai staging. **Không đẩy thẳng lên `main`.**
- Không dùng nhánh `develop`. Mọi nhánh tách từ `main` mới nhất:

| Loại | Mẫu tên | Ví dụ |
| --- | --- | --- |
| Chức năng | `feature/F<số>-<mo-ta>` | `feature/F2-ho-so-hoi-vien` |
| Sửa lỗi | `fix/F<số>-<mo-ta>` | `fix/F4-check-in-goi-pt` |
| Tài liệu | `docs/<mo-ta>` | `docs/srs-f1-f6` |
| Kiểm thử | `test/F<số>-<mo-ta>` | `test/F8-danh-sach-cho` |
| Việc khác | `chore/<mo-ta>` | `chore/cap-nhat-thu-vien` |

Tên nhánh chữ thường, không dấu, nối gạch ngang.

```bash
git switch main
git pull
git switch -c feature/F2-ho-so-hoi-vien
```

## 3. Commit

Theo [Conventional Commits](https://www.conventionalcommits.org/), phạm vi là mã chức năng:

```
feat(F2): thêm API tạo hồ sơ hội viên
fix(F4): chặn check-in khi chỉ có gói PT
docs(srs): bổ sung use case F1–F6
test(F8): kiểm thử thứ tự danh sách chờ
chore(db): thêm migration V3_01 tạo bảng members
ci: cache Maven trong GitHub Actions
```

Loại: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `ci`. Mô tả viết tiếng Việt, ngắn gọn, ở thể mệnh lệnh.

## 4. Pull request

- **Một việc một pull request.** Liên kết issue bằng `Closes #<số>` trong phần mô tả.
- Điền đủ mẫu pull request: mã chức năng, mô tả, cách kiểm thử, checklist.
- **Ít nhất 1 người duyệt**, **CI phải xanh** và mọi bình luận review đã được xử lý (bấm *Resolve conversation*).
- Gộp bằng **squash merge**; xóa nhánh sau khi gộp.
- Người bấm gộp phải giải thích được mã trong pull request.
- GitHub chặn gộp vào `main` khi chưa đủ 1 lượt duyệt, CI chưa xanh hoặc còn bình luận chưa xử lý, và không ai được vượt, kể cả trưởng nhóm ([ADR 0006](docs/decisions/0006-git-workflow.md)). Người mở pull request không tự duyệt được.
- **Chỉ bấm gộp khi đủ 1 lượt duyệt và CI xanh, kể cả khi GitHub không chặn** (ví dụ ruleset bị tắt).

## 5. Định nghĩa hoàn thành

Một việc chỉ tính là xong khi:

1. Issue có tiêu chí chấp nhận (Given/When/Then) và đáp ứng đủ.
2. Có kiểm thử JUnit cho mỗi quy tắc nghiệp vụ.
3. Có request Postman cho mỗi API mới.
4. CI xanh.
5. Tài liệu đã cập nhật (SRS, ADR, README, mô tả Swagger).
6. Chạy được trên staging.

## 6. Cơ sở dữ liệu

- Chỉ đổi schema bằng migration Flyway trong `backend/src/main/resources/db/migration/`.
- **Không sửa migration đã gộp vào `main`**; cần thay đổi thì tạo migration mới.
- Schema ban đầu (đủ 39 bảng) tạo trong một pull request ở Sprint 1, theo dải `V1_xx` … `V8_xx` (8 nhóm trong `erd-v2.dbml`). Sau đó mọi migration mới dùng dải tuần tự chung `V9_01`, `V9_02`, …; không thêm file vào dải `V1_xx` … `V8_xx`, vì Flyway từ chối chạy migration có số nhỏ hơn số đã chạy. Chi tiết: [db/migration/README.md](backend/src/main/resources/db/migration/README.md).
- `spring.jpa.hibernate.ddl-auto` luôn là `validate`.
- Quy ước dữ liệu (tóm tắt từ [thiet-ke-du-lieu.md](docs/design/erd/thiet-ke-du-lieu.md)):
  - Bảng, cột snake_case; tên bảng số nhiều; khóa chính `id bigint` tự tăng; khóa ngoại `<bảng số ít>_id`.
  - Người thao tác trỏ `users.id`, đặt tên `*_by`; huấn luyện viên trong vai trò dạy trỏ `staff.id`, đặt tên `trainer_id`.
  - Mọi bảng có `created_at`, `updated_at` kiểu `timestamptz`, lưu UTC; cần "ngày theo giờ Việt Nam" thì thêm cột `date` riêng.
  - Tiền là số nguyên VND.
  - Trạng thái viết hoa, lưu `varchar` kèm `CHECK`; entity dùng `@Enumerated(EnumType.STRING)`.
  - Mỗi thông tin chỉ lưu ở một nơi: vai trò `users.role`, email `users.email`, chi nhánh nhân viên `staff.branch_id`, số điện thoại `members.phone` / `staff.phone`.
  - Không xóa cứng dữ liệu nghiệp vụ (dùng trạng thái hoặc `deleted_at`).
  - Mọi bảng nghiệp vụ gốc có `branch_id` ngay từ v1; bảng con lấy chi nhánh qua bảng cha (ví dụ `payments` qua `invoices`).

## 7. Bảo mật

- Không commit khóa bí mật (mật khẩu, JWT secret, chuỗi kết nối). Dùng file `.env` (đã có trong `.gitignore`), tạo từ `.env.example`.
- Không dán khóa bí mật, nội dung `.env` hay dữ liệu thật của người dùng vào prompt AI.
- Lỡ commit khóa bí mật: báo ngay cho trưởng nhóm và đổi khóa đó; xóa khỏi lịch sử Git là chưa đủ.

## 8. Dùng công cụ AI hỗ trợ lập trình

- **Có user story rồi mới sinh mã**: issue phải có user story và tiêu chí chấp nhận trước khi yêu cầu sinh mã.
- Cấu hình công cụ đọc [docs/guides/quy-tac-ma-nguon.md](docs/guides/quy-tac-ma-nguon.md) làm hướng dẫn dự án; file cấu hình riêng của công cụ không đưa lên Git.
- Đọc lại **toàn bộ** thay đổi trước khi commit; bạn chịu trách nhiệm về mã mình commit như mã tự viết.
- Không thêm tính năng ngoài [bản chốt phạm vi](docs/requirements/ban-chot-pham-vi.md), kể cả khi AI gợi ý.

## 9. Quyết định và biên bản

- Quyết định quan trọng (công nghệ, phạm vi, dữ liệu, quy trình) ghi thành ADR trong [docs/decisions/](docs/decisions/) theo [mẫu](docs/decisions/0000-mau-adr.md).
- Biên bản họp lưu trong [docs/meetings/](docs/meetings/) theo [mẫu](docs/meetings/0000-mau-bien-ban.md).
