# Quy tắc mã nguồn

Tóm tắt các quy tắc bắt buộc khi viết mã cho dự án, dành cho cả 8 thành viên và cho công cụ AI hỗ trợ lập trình. Đọc hết trước khi sửa mã. Quy trình nhánh, commit, pull request xem [CONTRIBUTING.md](../../CONTRIBUTING.md).

> **Dùng công cụ AI:** cấu hình công cụ đọc file này làm hướng dẫn dự án (thường là một file cấu hình ở gốc repo, chỉ cần một dòng nhập file này). File cấu hình đó là của riêng từng máy: thêm tên file vào `.git/info/exclude` để không đưa lên Git.

## Đề tài

Web quản lý chuỗi phòng tập gym – bài tập lớn IT3180 (Nhập môn Công nghệ phần mềm), Đại học Bách khoa Hà Nội, Nhóm 6 (8 người). 20 chức năng F1–F20 chia 5 version (v1–v5), 6 vai trò (chủ chuỗi, quản lý chi nhánh, lễ tân, huấn luyện viên, hội viên, khách). Nhóm làm theo Agile, chia sprint; mỗi việc bắt đầu từ một issue user story có tiêu chí chấp nhận. Ưu tiên mã rõ ràng, dễ kiểm thử, dễ giải thích hơn kỹ thuật phức tạp.

**Nguồn chuẩn – đọc trước khi làm bất kỳ chức năng nào:**

| Chủ đề | File |
| --- | --- |
| Phạm vi, quy tắc nghiệp vụ, yêu cầu phi chức năng (NF1–NF6, S1–S10) | [ban-chot-pham-vi.md](../requirements/ban-chot-pham-vi.md) |
| Dữ liệu (bản chuẩn của ERD) | [erd-v2.dbml](../design/erd/erd-v2.dbml) |
| Giải thích dữ liệu, ràng buộc cần kiểm thử | [thiet-ke-du-lieu.md](../design/erd/thiet-ke-du-lieu.md) |
| Quyết định kiến trúc | [decisions/](../decisions/) |
| Quy ước làm việc | [CONTRIBUTING.md](../../CONTRIBUTING.md) |

Khi tài liệu mâu thuẫn: bản chốt phạm vi (nghiệp vụ) và `erd-v2.dbml` (dữ liệu) là đúng. Điều gì không có trong tài liệu thì **hỏi lại** trưởng nhóm, không tự đoán.

## Lệnh

**Backend** (thư mục `backend/`, JDK 21, Docker phải đang chạy cho Testcontainers):

```bash
./mvnw -B verify                                         # build + toàn bộ kiểm thử
./mvnw test -Dtest=TenLopTest                            # một lớp kiểm thử
./mvnw spring-boot:run -Dspring-boot.run.profiles=local  # chạy, cần `docker compose up -d` ở gốc
```

**Frontend** (thư mục `frontend/`, Node 22):

```bash
npm ci
npm run lint
npm test -- --run
npm run build
npm run dev
```

**Hạ tầng** (gốc repo): `cp .env.example .env`, `docker compose up -d`.

## Quy ước mã nguồn

- **Backend:** gói gốc `vn.edu.hust.nhom6.gym`. Đóng gói theo chức năng: `feature/<chức năng>/{controller,service,repository,entity,dto,mapper}` ([ADR 0007](../decisions/0007-cau-truc-ma-nguon.md)). Phần dùng chung: `config/`, `common/entity/BaseEntity`, `common/exception/` (`NotFoundException` 404, `ConflictException` 409, `BusinessRuleException` 422, trả `ProblemDetail` tiếng Việt), `common/dto/PageResponse`.
- Controller chỉ nhận request (`@Valid`) và gọi service; quy tắc nghiệp vụ nằm ở service; chức năng này chỉ gọi chức năng khác qua service.
- **Mọi API nghiệp vụ có tiền tố `/api/v1`**, ví dụ `GET /api/v1/members/{id}`.
- **Frontend:** `src/features/<chức năng>/{api,components,pages,types}`; dùng chung ở `layouts/`, `routes/`, `services/http.ts`, `auth/`, `components/`. Component `PascalCase.tsx`; module khác chữ thường nối gạch ngang. Giao diện tiếng Việt, responsive từ 360px.
- Tên file, thư mục: chữ thường, không dấu, nối gạch ngang (trừ quy ước bắt buộc của Java/React). Tài liệu viết tiếng Việt có dấu.
- **Nhánh:** tách từ `main`: `feature/F2-ho-so-hoi-vien`, `fix/F4-...`, `docs/...`, `test/F8-...`, `chore/...`.
- **Commit:** Conventional Commits, phạm vi là mã chức năng: `feat(F2): ...`, `fix(F4): ...`, `docs(srs): ...`, `test(F8): ...`, `chore(db): ...`, `ci: ...`.

## Quy tắc Git bắt buộc

1. **Không bao giờ commit hay push khi đang ở nhánh `main`.** Nếu đang ở `main`, tạo nhánh mới trước (`git switch -c feature/F<số>-<mo-ta>`).
2. **Không dùng `--no-verify`** (không vượt hook).
3. **Không `push --force`** lên nhánh của người khác.
4. Đọc lại toàn bộ thay đổi trước khi commit. Công cụ AI chỉ commit, push khi người dùng yêu cầu.
5. Commit message và mô tả pull request **không ghi tên công cụ AI** (kể cả dòng `Co-Authored-By` hay "Generated with …" do công cụ tự thêm); tắt tính năng này trong cấu hình công cụ.

## Quy tắc mã bắt buộc

1. Giá và số tiền **tính ở server**; không tin số tiền do trình duyệt gửi lên (S7).
2. Kiểm tra **vai trò và quyền sở hữu dữ liệu ở backend** trên mọi API (S3); hội viên không xem được dữ liệu của hội viên khác. Phân quyền ở frontend chỉ là trải nghiệm.
3. **Validate mọi request** bằng Bean Validation (S5); dùng JPA có tham số, không nối chuỗi SQL.
4. **Không sửa migration đã gộp vào `main`**; tạo migration mới theo [quy ước](../../backend/src/main/resources/db/migration/README.md) (schema ban đầu dùng dải `V1_xx`…`V8_xx`; sau đó mọi migration mới dùng dải tuần tự `V9_01`, `V9_02`…, không thêm vào dải cũ).
5. `spring.jpa.hibernate.ddl-auto` **luôn là `validate`**; schema chỉ đổi bằng Flyway.
6. **Tiền là số nguyên VND** (`int`/`Integer`, cột `integer`), không dùng số thực.
7. **Thời gian lưu UTC** (`timestamptz`, `OffsetDateTime`); quy tắc theo "ngày Việt Nam" thì dùng cột `date` riêng.
8. **Không xóa cứng** dữ liệu nghiệp vụ; dùng trạng thái hoặc `deleted_at`.
9. **Mọi bảng nghiệp vụ gốc có `branch_id`** ngay từ v1; bảng con lấy chi nhánh qua bảng cha, không chép lại (xem [thiết kế dữ liệu](../design/erd/thiet-ke-du-lieu.md)).
10. **Trạng thái là `varchar` + `CHECK`**; entity dùng `@Enumerated(EnumType.STRING)`.
11. **Người thao tác trỏ `users.id`**, cột `*_by`; huấn luyện viên trong vai trò dạy trỏ `staff.id` (`trainer_id`).
12. Mỗi thông tin chỉ lưu một nơi: vai trò `users.role`, email `users.email`, chi nhánh nhân viên `staff.branch_id`, số điện thoại `members.phone` / `staff.phone`.
13. **Không thêm tính năng ngoài phạm vi** bản chốt (đặc biệt: không thanh toán online, không AI ngoài F20, không đăng nhập bằng số điện thoại).
14. **Mỗi quy tắc nghiệp vụ phải có kiểm thử JUnit**; API mới có request Postman trong `postman/`.
15. Tham số có dấu \* trong bản chốt phạm vi đọc từ `system_configs`, không viết cứng.

## Quy tắc nghiệp vụ dễ làm sai

Tóm tắt; chi tiết và ngoại lệ xem [bản chốt phạm vi](../requirements/ban-chot-pham-vi.md).

- **Chỉ đăng nhập bằng email** (F1).
- **Gói PT không dùng để check-in** vào phòng tập; chỉ gói TIME hoặc SESSION đang ACTIVE (F4).
- Hội viên có **cả gói TIME và SESSION thì dùng gói TIME**, không trừ buổi (F4).
- **Chỉ gói TIME được đặt lớp nhóm** hoặc vào danh sách chờ; gói SESSION và PT thì không (F8).
- **Gói chỉ ACTIVE khi hóa đơn PAID** (F3, F5).
- **Hóa đơn chờ do F7 tạo tự hủy sau `PENDING_INVOICE_TTL_DAYS`** (mặc định 3 ngày), kèm gói PENDING đi cùng.
- **Tự đăng ký bằng số điện thoại đã có hồ sơ thì từ chối** và hướng dẫn liên hệ quầy; không tự gắn tài khoản mới vào hồ sơ cũ (F1, F2).
- **Báo cáo doanh thu dùng `LEFT JOIN` với `members`**, vì hóa đơn khách lẻ có `member_id` rỗng (F6, F16).
- **`end_date = start_date + duration_days - 1`**: `end_date` là ngày cuối còn dùng được, không cộng thẳng số ngày (F3).
- **Gói SESSION về 0 buổi vẫn ACTIVE đến hết ngày** (check-in lại được, không trừ thêm); ngay lúc trừ buổi cuối, gói UPCOMING nối tiếp được kéo về bắt đầu từ ngày mai (F4). Gói PT hết buổi thì EXPIRED ngay và gói PT nối tiếp bắt đầu hôm nay (F9).
- **Tác vụ nền chuyển EXPIRED chạy trước tác vụ kích hoạt UPCOMING** (00:00 giờ Việt Nam, chạy bù khi khởi động); gói SESSION 0 buổi chỉ EXPIRED khi hôm nay chưa trừ buổi.

## Quy trình làm một chức năng

1. Nhận issue user story trong sprint; đọc đặc tả của chức năng (user story, tiêu chí chấp nhận, quy tắc nghiệp vụ, bảng liên quan trong ERD).
2. Lên kế hoạch (API, service, migration, màn hình, kiểm thử). Khi dùng công cụ AI: yêu cầu AI đề xuất kế hoạch trước, đọc và đồng ý rồi mới cho sinh mã.
3. Viết kiểm thử cho quy tắc nghiệp vụ cùng lúc với mã.
4. Chạy `./mvnw -B verify` và/hoặc `npm run lint && npm test -- --run && npm run build`; sửa đến khi qua, không tắt hay bỏ qua kiểm thử.
5. Tự đọc lại toàn bộ thay đổi, rồi mở pull request theo [CONTRIBUTING.md](../../CONTRIBUTING.md).
