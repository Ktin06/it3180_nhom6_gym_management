# Dữ liệu mẫu cho demo

Thư mục này chứa dữ liệu giả để chạy thử và demo: tài khoản cho 5 vai trò có đăng nhập, hội viên, gói tập, lịch lớp. Chỉ profile `local` và `staging` đọc thư mục này (`spring.flyway.locations` trong `application-local.yml` và `application-staging.yml`). Kiểm thử và mọi môi trường khác chỉ chạy `db/migration`.

Dữ liệu cần có ở mọi môi trường (chi nhánh mặc định, giá trị mặc định của `system_configs`) **không** để ở đây mà là migration bình thường (xem [db/migration/README.md](../migration/README.md)).

## Quy tắc

1. **Dùng migration lặp lại** của Flyway, đặt tên `R__<số>_<mo_ta>.sql`, ví dụ `R__01_tai_khoan.sql`, `R__02_hoi_vien.sql`. Flyway chạy các file này sau mọi migration có số phiên bản, theo thứ tự tên, và chạy lại khi nội dung file thay đổi. Không dùng tên `V...` ở đây, vì số phiên bản của dữ liệu mẫu sẽ chặn migration schema về sau.
2. **Chạy lại nhiều lần không lỗi, không sinh dòng trùng:** mọi câu `INSERT` có `ON CONFLICT (...) DO NOTHING` trên một cột duy nhất (email, mã hội viên, mã gói...). Không dùng id cố định; tra id bằng truy vấn con theo cột duy nhất.
3. **Không ghi mật khẩu trong file.** Mật khẩu tài khoản mẫu là mã băm bcrypt lấy từ biến môi trường qua placeholder của Flyway, ví dụ `${seed_password_hash}`. Khi viết file đầu tiên cần mật khẩu, khai báo `spring.flyway.placeholders.seed_password_hash: ${SEED_PASSWORD_HASH}` trong hai profile và thêm biến `SEED_PASSWORD_HASH` vào `.env.example` và Render. Tạo mã băm bcrypt bằng Docker (máy nào cũng có): `docker run --rm httpd:2.4-alpine htpasswd -bnBC 10 "" <mat-khau> | tr -d ':
'`. Kết quả dạng `$2y$10$...`, Spring Security đọc được. Trong `.env`, đặt giá trị trong dấu nháy đơn vì có ký tự `$`.
4. Dữ liệu là dữ liệu giả: tên, số điện thoại, email không thuộc người thật (dùng tên miền `example.com`). Repo công khai.
5. Có ít nhất một kiểm thử tích hợp chạy Flyway với cả `db/seed` để CI phát hiện lỗi SQL trong dữ liệu mẫu.
