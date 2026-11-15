# Tích hợp frontend F2/F3

## Phiên kiểm tra ngày 15/11/2026

Nhánh `feature/f2-f3-giao-dien` đã đồng bộ main `2262010` qua merge commit `b7eec11`, gồm frontend auth #80 và API danh mục #107. Phiên này dùng thêm các thay đổi frontend sửa điều hướng F2/F3 sau đăng nhập, ổn định test race và cập nhật tài liệu. Backend không đổi so với main `2262010`.

**Kết quả: 28/28 ca tích hợp đạt**, hoàn tất lúc 13:31 ngày 15/11/2026 (giờ Việt Nam). [Báo cáo JSON của lượt chạy đạt](evidence/f2-f3-live-2026-11-15.json). Một lượt trước đã đạt 27 ca; khi bổ sung ca hai tab, selector nút có icon không khớp tên chính xác nên timeout. Đã sửa selector và chạy lại toàn bộ đạt 28/28; không tính các lượt timeout đó là đạt.

| Thành phần    | Môi trường thực tế                                                                                                                        |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| Backend       | Spring Boot 4.1.2, JDK 21.0.12.1; build Maven `package -DskipTests`, chạy JAR profile `local`, cổng 8080                                  |
| Cơ sở dữ liệu | PostgreSQL 16.16, container riêng `gym-pr103-review-db`, cổng 15432, database `gymreview`; Flyway áp 46 migration gồm seed, đến `9.01`    |
| Frontend      | Vite 8.3.5 ở `http://localhost:5173`, proxy `/api` đến backend; `VITE_F2_F3_MOCK=false`                                                   |
| Trình duyệt   | Microsoft Edge 157.0.4341.46 headless qua Playwright, viewport mặc định và 360 × 800                                                      |
| Dữ liệu       | Tài khoản seed với mật khẩu ngẫu nhiên chỉ dùng cho database thử; dữ liệu giả CN01/CN02, hồ sơ và gói riêng để kiểm tra phạm vi chi nhánh |

Kịch bản có thể chạy lại: [check-f2-f3-live.mjs](../../frontend/scripts/check-f2-f3-live.mjs), dữ liệu bổ sung: [f2-f3-live-fixtures.sql](../../frontend/scripts/f2-f3-live-fixtures.sql). Các bước tạo/sửa đều đi qua HTTP, Spring Boot và PostgreSQL thật; không dùng mock API F2/F3.

## Phạm vi đã kiểm tra

- API không có token bị từ chối `401`; SUPER_ADMIN/BRANCH_MANAGER/RECEPTIONIST vào được F2/F3; TRAINER/MEMBER nhận `403`.
- Lễ tân CN01 không thấy hội viên CN02; đọc/sửa trực tiếp mã hồ sơ ngoài chi nhánh nhận `404`. Chủ chuỗi thấy được dữ liệu hai chi nhánh.
- Danh mục trả đúng DTO, loại và trạng thái. Lễ tân không thấy gói riêng CN02, nhưng thấy gói toàn chuỗi và CN01; lọc SESSION + INACTIVE trả đúng gói đã ngừng bán.
- Trình duyệt đăng nhập thật rồi quay lại `/members` hoặc `/packages` theo quyền. MEMBER/TRAINER vào hai route này sau đăng nhập bị chuyển `/403`.
- Tạo hồ sơ `201`, sửa `200`, tìm theo SĐT/mã/đầu họ tên, phân trang từ UI trang 2 thành API `page=1`, lọc INACTIVE. Reload vẫn đọc được dữ liệu đã lưu.
- SĐT trùng trả `409` và giữ form; ngày sinh tương lai trả `400`, thông báo `errors.dob` hiện đúng trường.
- Reload khôi phục phiên bằng refresh cookie; access token không ghi vào Web Storage. Cookie có HttpOnly, SameSite=Strict, Path=/api/v1/auth và không đọc được từ `document.cookie`.
- Một request nghiệp vụ bị thay bearer thành token sai để backend thật trả `401`; client gọi refresh thật `200` rồi gửi lại request thành công. Đây là fault injection ở request, không chờ token hết hạn tự nhiên và không giả lập response/cookie.
- Logout thật `204`; truy cập lại route bảo vệ không khôi phục phiên. Bảng cuộn trong vùng riêng, không tràn trang ở 360px.
- Hai tab chung cookie: A đăng nhập lễ tân, B đăng nhập chủ chuỗi. Ép một request của A mang bearer sai để backend trả `401`: refresh trả token của B, A về login với thông báo đổi tài khoản, không retry request bằng B và không gọi logout. Cookie của B vẫn refresh được `200`.

Kết quả tự động được ghi vào `results.json` trong thư mục `GYM_TEST_OUTPUT`. File chỉ chứa tên ca kiểm tra, thời gian, phiên bản trình duyệt và mã hội viên thử; không chứa mật khẩu/token/cookie.

## Chạy lại

1. Dùng **database cục bộ riêng có thể bỏ**, khởi tạo theo [hướng dẫn môi trường](cai-dat-moi-truong.md) với profile `local` và tài khoản seed. Dùng mật khẩu thử mới, không dùng tài khoản thật. Kịch bản tạo hồ sơ mới và sửa trạng thái dữ liệu thử.
2. Áp fixture vào đúng database đó, ví dụ với container của phiên này:

   ```powershell
   docker cp frontend/scripts/f2-f3-live-fixtures.sql gym-pr103-review-db:/tmp/pr103-fixtures.sql
   docker exec gym-pr103-review-db psql -U gymreview -d gymreview -v ON_ERROR_STOP=1 -f /tmp/pr103-fixtures.sql
   ```

3. Bật backend ở cổng 8080. Tại `frontend/`, đặt `VITE_F2_F3_MOCK=false`, chạy `npm run dev`, dùng origin `http://localhost:5173`.
4. Cài Playwright riêng vào thư mục tạm và dùng Edge đã cài trên máy. Tại gốc repo:

   ```powershell
   $browserDir = Join-Path $env:TEMP 'gym-live-browser'
   npm install --prefix $browserDir --no-audit --no-fund playwright
   $env:PLAYWRIGHT_MODULE = ([uri](Join-Path $browserDir 'node_modules/playwright/index.mjs')).AbsoluteUri
   $env:GYM_TEST_URL = 'http://localhost:5173'
   # Đặt GYM_TEST_PASSWORD bằng mật khẩu seed thử; không ghi vào repo hoặc nhật ký.
   $env:GYM_TEST_OUTPUT = Join-Path $env:TEMP 'gym-live-results'
   node frontend/scripts/check-f2-f3-live.mjs
   ```

Dependency Playwright không được thêm vào package.json/package-lock của ứng dụng. Nếu đã có Playwright trong môi trường, có thể bỏ `PLAYWRIGHT_MODULE`.

## Giới hạn

Đây là tích hợp thật **cục bộ qua HTTP**, không phải kết quả HTTPS Vercel → Render. Cookie Secure và bản triển khai staging của #103 chưa được xác minh trong phiên này. Profile local không bật Secure. Không xem kết quả này là bằng chứng sửa hai tab refresh đồng thời (#112).

Lint, build và bộ Vitest sau sửa đạt 103/103 ở timeout mặc định; không tăng timeout toàn cục. Test race được đổi sang dán cả giá trị để giảm chi phí gõ từng ký tự, vẫn trì hoãn response chi tiết và kiểm tra form mới không bị ghi đè.
