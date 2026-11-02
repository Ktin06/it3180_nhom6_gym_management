# Migration Flyway

Schema cơ sở dữ liệu **chỉ** được tạo và sửa bằng các file migration trong thư mục này. Hibernate chạy với `ddl-auto: validate` nên không tự sinh bảng. Bản chuẩn của ERD: [docs/design/erd/erd-v2.dbml](../../../../../../docs/design/erd/erd-v2.dbml); giải thích: [thiet-ke-du-lieu.md](../../../../../../docs/design/erd/thiet-ke-du-lieu.md).

ERD bản 2 đã được cả nhóm duyệt ngày 02/11/2026; schema ban đầu đang được viết trên nhánh chung `chore/db-schema-ban-dau`.

## Hai giai đoạn

Flyway **từ chối chạy** migration có số phiên bản nhỏ hơn số đã chạy trên cơ sở dữ liệu. Ví dụ staging đã chạy `V6_01` thì một file `V3_05` thêm sau sẽ làm backend không khởi động được. CI không phát hiện lỗi này vì Testcontainers luôn tạo cơ sở dữ liệu mới. Vì vậy:

1. **Schema ban đầu (Sprint 1):** sau khi ERD được duyệt, **một** pull request tạo đủ 39 bảng theo dải `V1_xx` … `V8_xx` bên dưới, kể cả bảng của chức năng v3–v5. Entity chỉ viết khi làm chức năng tương ứng; `ddl-auto: validate` chỉ kiểm tra bảng có entity. Cách nhiều người cùng viết: xem [Làm schema ban đầu qua nhánh chung](#làm-schema-ban-đầu-qua-nhánh-chung).
2. **Sau khi schema ban đầu đã gộp vào `main`:** mọi migration mới dùng dải tuần tự chung `V9_01`, `V9_02`, … (hết `V9_99` thì sang `V10_01`), bất kể sửa bảng thuộc nhóm nào. **Không** thêm file mới vào dải `V1_xx` … `V8_xx`.

## Đặt tên

```
V<nhóm>_<số>__<mo_ta>.sql
```

- `<nhóm>`: số thứ tự nhóm bảng (1–8) theo `erd-v2.dbml` cho schema ban đầu; từ 9 trở đi cho migration sau đó.
- `<số>`: hai chữ số, tăng dần trong dải (`01`, `02`, ...).
- `<mo_ta>`: chữ thường, không dấu, nối bằng dấu gạch dưới.
- Giữa phiên bản và mô tả là **hai** dấu gạch dưới.

Ví dụ schema ban đầu: `V1_01__create_users.sql`, `V3_02__create_memberships.sql`. Ví dụ về sau: `V9_01__add_members_note.sql`, `V9_02__seed_badges.sql`.

Flyway đọc `V1_01` là phiên bản `1.01` và `V9_01` là `9.01`, nên mọi migration nhóm 1 chạy trước nhóm 2, và migration về sau luôn chạy sau schema ban đầu.

## Dải phiên bản của schema ban đầu

| Dải | Nhóm bảng | Bảng |
| --- | --- | --- |
| `V1_xx` | 1. Tài khoản và hệ thống | users, refresh_tokens, password_resets, audit_logs, notifications, system_configs |
| `V2_xx` | 2. Chi nhánh và cơ sở vật chất | branches, rooms, equipment, maintenance_logs |
| `V3_xx` | 3. Hội viên và gói tập | members, face_embeddings, body_metrics, packages, memberships, membership_freezes, membership_transfers |
| `V4_xx` | 4. Check-in và lớp học | checkins, class_types, class_schedules, class_sessions, class_bookings |
| `V5_xx` | 5. Huấn luyện viên | trainer_availabilities, pt_sessions, workout_plans, workout_logs |
| `V6_xx` | 6. Bán hàng và thanh toán | products, inventories, stock_movements, promotions, invoices, invoice_items, payments |
| `V7_xx` | 7. Nhân sự | staff, shifts, attendances, payrolls |
| `V8_xx` | 8. Gamification | badges, member_badges |

**Khóa ngoại trỏ sang nhóm bảng khác** (ví dụ `audit_logs.branch_id` → `branches`, `memberships.invoice_item_id` → `invoice_items`) đặt trong migration cuối của dải `V8_xx`, sau khi mọi bảng đã có. Khóa ngoại trong cùng nhóm đặt ngay trong migration tạo bảng.

## Làm schema ban đầu qua nhánh chung

Mỗi người viết migration cho nhóm bảng được giao, nhưng `main` chỉ nhận **một** pull request cho đủ 39 bảng:

1. Người giữ nhánh chung tạo `chore/db-schema-ban-dau` từ `main` và đẩy lên GitHub.
2. Mỗi người tách nhánh từ nhánh chung (ví dụ `chore/db-nhom-3`), chỉ viết file trong dải số của nhóm mình, rồi mở pull request **vào `chore/db-schema-ban-dau`**, không vào `main`. Người giữ nhánh chung duyệt.
3. Khóa ngoại trong cùng nhóm đặt ngay trong file tạo bảng. Khóa ngoại sang nhóm khác do người giữ nhánh chung đặt ở cuối dải `V8_xx`. Trước khi có khóa ngoại đó, migration từng nhóm vẫn phải chạy được độc lập.
4. Khi đủ 39 bảng và `./mvnw -B verify` qua, người giữ nhánh chung mở **một** pull request từ `chore/db-schema-ban-dau` vào `main`.
5. Sau khi gộp vào `main`, xóa nhánh chung. Từ đây mọi migration mới theo giai đoạn 2.

## Dữ liệu tham chiếu và dữ liệu mẫu

- **Dữ liệu tham chiếu** cần có ở mọi môi trường (chi nhánh mặc định, giá trị mặc định của `system_configs`, danh mục huy hiệu) là migration bình thường trong thư mục này.
- **Dữ liệu mẫu cho demo** (tài khoản thử, hội viên giả, gói, lịch lớp) để trong [db/seed](../seed/README.md), không để ở đây.

## Quy tắc

1. **Không sửa** migration đã gộp vào `main`, kể cả sửa lỗi chính tả. Cần thay đổi thì tạo migration mới với số tiếp theo. Flyway kiểm tra checksum và sẽ báo lỗi nếu file cũ bị sửa.
2. Trước khi tạo migration, `git pull` nhánh `main` và lấy số tiếp theo sau số lớn nhất hiện có. Hai pull request cùng lấy một số thì pull request gộp sau phải đổi số trước khi gộp.
3. Tuân theo quy ước dữ liệu (xem [thiet-ke-du-lieu.md](../../../../../../docs/design/erd/thiet-ke-du-lieu.md)):
   - bảng và cột snake_case, tên bảng số nhiều; khóa chính `id bigint generated always as identity`;
   - thời gian kiểu `timestamptz`; tiền kiểu `integer` (VND);
   - trạng thái kiểu `varchar` kèm `CHECK (status IN ('ACTIVE', ...))`, không dùng kiểu enum của PostgreSQL;
   - mọi bảng có `created_at`, `updated_at` (`timestamptz not null`);
   - chỉ mục duy nhất có điều kiện viết bằng `CREATE UNIQUE INDEX ... WHERE ...`.
4. Kiểm thử Testcontainers (`./mvnw verify`) chạy toàn bộ migration trên PostgreSQL thật; phải qua trước khi mở pull request.
