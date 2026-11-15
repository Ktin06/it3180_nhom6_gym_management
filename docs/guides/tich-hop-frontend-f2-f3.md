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

Các kết quả 28 ca phía trên là tích hợp thật **cục bộ qua HTTP**; profile local không bật Secure. Kết quả HTTPS bổ sung nằm ở mục dưới. Không xem kết quả này là bằng chứng sửa hai tab refresh đồng thời (#112).

Lint, build và bộ Vitest sau sửa đạt 103/103 ở timeout mặc định; không tăng timeout toàn cục. Test race được đổi sang dán cả giá trị để giảm chi phí gõ từng ký tự, vẫn trì hoãn response chi tiết và kiểm tra form mới không bị ghi đè.

## HTTPS Vercel → Render ngày 15/11/2026

**12/12 ca đạt**, hoàn tất lúc 13:40 giờ Việt Nam. [Báo cáo JSON](evidence/f2-f3-staging-2026-11-15.json), [kịch bản chạy lại](../../frontend/scripts/check-f2-f3-staging.mjs).

- Frontend: preview `https://it3180-nhom6-gym-git-feature-f2-f3-giao-dien-ktin06.vercel.app`, commit **`c6da982`**, deployment Vercel [HV8tkUZUp2tJByyjKYmpthGv51wJ](https://vercel.com/ktin06/it3180-nhom6-gym/HV8tkUZUp2tJByyjKYmpthGv51wJ). Trạng thái deployment trên GitHub của đúng commit là success; CI Frontend/Backend cũng success. Bundle có mã auth mới cùng F2/F3.
- Backend: `https://it3180-nhom6-gym-management.onrender.com`; health trả `200 UP`. Chưa xác minh độc lập commit backend đang triển khai vì không có endpoint công khai trả revision. Không dùng xác nhận backend của phiên 12/11 để suy ra revision hôm nay.
- Trình duyệt: Edge 157.0.4341.46 headless. Các request ứng dụng đi trên origin HTTPS của preview, Vercel rewrite `/api` sang Render.
- Tài khoản: tự đăng ký một hội viên thử mới thuộc miền `example.com`, họ tên “Kiem thu HTTPS PR103”. Phiên chạy lịch sử này dùng mật khẩu suy ra được từ định danh thử, không phải mật khẩu ngẫu nhiên an toàn như mô tả ban đầu. Email đã được bỏ khỏi bằng chứng hiện tại. Tiến đã nhận vô hiệu hóa tài khoản cũ; chưa có xác nhận hoàn tất. Việc xóa email ở bản mới không xóa dữ liệu trong lịch sử Git.

Đã xác nhận:

1. Frontend preview và API proxy hoạt động; `/auth/me`, `/members`, `/packages` không token trả `401`.
2. Đăng ký trên trình duyệt trả `201`, lấy identity `200`, role MEMBER, vào `/member`.
3. Cookie refresh thuộc domain preview, `Secure; HttpOnly; SameSite=Strict; Path=/api/v1/auth`; không đọc được từ JavaScript. Web Storage trống.
4. Reload và tab mới gọi refresh `200`, giữ phiên.
5. Với JWT MEMBER thật, API F2/F3 trả `403`; giao diện `/members` và `/packages` chuyển `/403`.
6. Logout `204`; truy cập lại trang bảo vệ làm refresh `401`, về `/login`, không tự khôi phục phiên.
7. Sai mật khẩu `401`; đúng mật khẩu `200`. Login/register không tràn ngang ở 360px. Không có lỗi JavaScript không được bắt trong trang kiểm thử.

Lượt đầu preview tải được nhưng API qua proxy bị timeout/`ECONNRESET`; health Render cũng chưa phản hồi. Gọi trực tiếp health sau đó trả UP sau 33,5 giây. Chạy lại sau khi health UP đạt 12/12; lượt lỗi ban đầu không được tính là đạt.

**Giới hạn còn lại:** người dùng xác nhận chưa có tài khoản nhân viên staging được cấp. Vì vậy chưa kiểm tra thao tác thêm/sửa/tìm/lọc bằng Lễ tân/Quản lý hoặc quyền chi nhánh trên HTTPS. Các luồng đó đã đạt trong phiên 28/28 với backend/PostgreSQL cục bộ, không được ghi thành kết quả staging. Phiên HTTPS cũng không kiểm tra hai tab đổi tài khoản nhân viên.

Để chạy lại, cài Playwright riêng và đặt `PLAYWRIGHT_MODULE` như hướng dẫn trên, sau đó tại gốc repo:

```powershell
$env:GYM_TEST_URL = 'https://it3180-nhom6-gym-git-feature-f2-f3-giao-dien-ktin06.vercel.app'
# Đặt GYM_TEST_COMMIT theo commit được xác nhận từ deployment đang chạy.
$env:GYM_TEST_COMMIT = 'c6da982'
$env:GYM_TEST_OUTPUT = Join-Path $env:TEMP 'gym-staging-results'
node frontend/scripts/check-f2-f3-staging.mjs
```

Kịch bản HTTPS không dùng mật khẩu seed và không áp fixture SQL lên staging; mỗi lượt tạo một hội viên thử mới rồi đăng xuất khi hoàn tất.

## Sửa theo review ngày 15/11

Kịch bản mới sinh phần bí mật của mật khẩu bằng `crypto.randomBytes(24)`, độc lập với email và thời gian. Báo cáo mới không xuất email, mật khẩu hoặc token. Bằng chứng 12/12 bên trên vẫn thuộc phiên bản lịch sử đã ghi, không phải kết quả chạy lại kịch bản sau sửa bảo mật; chưa chạy thêm staging trong lúc chờ tài khoản nhân viên được cấp.

Giao diện hội viên áp dụng từ khóa đang nhập khi đổi trạng thái, đổi trang hoặc tải lại; từ khóa thay đổi đưa về trang đầu. Trạng thái đang tải hiển thị “Đang tải hội viên…”; chỉ hiển thị thông báo không có hội viên sau khi API trả kết quả rỗng.

Sau các sửa review này: 105/105 Vitest ở timeout mặc định, ESLint, TypeScript/build và định dạng các file thay đổi đều đạt. Hai test bổ sung kiểm tra từ khóa chưa bấm Tìm khi đổi bộ lọc/tải lại và phân biệt đang tải với kết quả rỗng. Chưa chạy lại 28 ca cục bộ hoặc 12 ca HTTPS cho phiên bản sửa này; các số liệu tích hợp phía trên vẫn thuộc đúng phiên bản lịch sử đã ghi.

## HTTPS nhân viên — 23:00 ngày 15/11/2026

**23/23 ca đạt** trên frontend **`6d985be`**, preview nhánh #103 → Render. [Báo cáo JSON](evidence/f2-f3-staff-staging-2026-11-15.json), [kịch bản chạy lại](../../frontend/scripts/check-f2-f3-staff-staging.mjs). GitHub xác nhận deployment Vercel [GmWh37kbvkUBxRZz8LgZkL7g3rP8](https://vercel.com/ktin06/it3180-nhom6-gym/GmWh37kbvkUBxRZz8LgZkL7g3rP8) thành công cho đúng commit. Render health `200 UP`, Edge 157.0.4341.46; không giả lập response hay cookie.

Người dùng đã nhận hai tài khoản thử do Tiến cấp riêng, cùng CN01. Server `/auth/me` xác nhận vai trò thực tế là **BRANCH_MANAGER** và **RECEPTIONIST**; nhãn “MANAGER” trong bàn giao là tài khoản BRANCH_MANAGER của API. Thông tin đăng nhập chỉ nằm trong file tạm ngoài repo và bộ nhớ, không có trong kịch bản hoặc báo cáo công khai.

Với **từng vai trò**, đã kiểm tra:

1. Đăng nhập API và giao diện thành công, trở lại `/members`.
2. Thêm hồ sơ bằng giao diện qua API thật `201`; reload refresh `200`, hồ sơ và SĐT vẫn lưu bền.
3. Gõ SĐT chưa bấm Tìm rồi đổi trạng thái: request dùng đúng từ khóa, lọc ACTIVE và trả đúng một hồ sơ.
4. Sửa tên và trạng thái INACTIVE qua PUT `200`; reload và GET chi tiết vẫn trả đúng dữ liệu đã sửa.
5. Tìm theo mã/đầu họ tên, lọc INACTIVE; SĐT trùng `409`, giữ dữ liệu đã nhập trong form.
6. Đọc mọi trang danh sách: tất cả hồ sơ trả về có `homeBranchId=1` (CN01 theo tài khoản được cấp). Thêm bộ lọc `branchId=999999` không mở rộng/đổi phạm vi danh sách.
7. Danh mục tải `200`, chỉ quan sát được gói toàn chuỗi hoặc `branchId=1`; lọc SESSION và ACTIVE/INACTIVE gửi đúng bộ lọc, nội dung trả về khớp.
8. Cookie Secure/HttpOnly/SameSite/Path đúng; logout `204`, truy cập lại route bảo vệ về login. Không có lỗi JavaScript không được bắt trong hai trang thử.

Tạo riêng bốn hồ sơ thử mang tiền tố `PR103 Staff`: hai lượt đầu dừng ở selector kiểm thử khớp cả dropdown ẩn và hiện, sau đó đã sửa selector và chạy lại trọn bộ đạt 23/23. Hai hồ sơ của lượt đạt có ID 11/12, mã HV000007/HV000008, trạng thái INACTIVE. Hai hồ sơ từ lượt đầu có ID 9/10, mã HV000005/HV000006; không sửa hồ sơ có sẵn của người khác. Các hồ sơ thử còn trong staging vì không có API xóa; trưởng nhóm có thể dọn theo các mã này.

**Chưa đủ để xác nhận toàn bộ cách ly chi nhánh:** người dùng chưa được cấp ID/mã hồ sơ thử thuộc CN02. Kiểm tra danh sách cùng CN01 và tham số không mở rộng phạm vi không thay thế kiểm tra đọc/sửa một hồ sơ khác chi nhánh đang thực sự tồn tại. Cần Tiến cấp fixture/ID đã xác nhận thuộc CN02 để chạy phần từ chối `404`. Revision backend đang chạy và việc vô hiệu hóa tài khoản thử cũ cũng chưa được xác nhận độc lập. Giữ Draft trong khi hoàn tất các mục này và chờ người duyệt kết luận.

Để chạy lại, dùng Playwright riêng như mục trên, tạo file JSON **ngoài repo** với hai key `manager`, `receptionist`, mỗi key có `email` và `password` nhận riêng. Không đưa file đó vào báo cáo hoặc commit. Sau đó:

```powershell
$env:GYM_STAFF_CREDENTIALS = 'C:/duong-dan-rieng/credentials.json'
$env:GYM_TEST_URL = 'https://it3180-nhom6-gym-git-feature-f2-f3-giao-dien-ktin06.vercel.app'
$env:GYM_TEST_COMMIT = '6d985be' # thay theo deployment thực tế được xác nhận
$env:GYM_TEST_OUTPUT = Join-Path $env:TEMP 'gym-staff-results'
# Chỉ đặt GYM_FOREIGN_MEMBER_ID nếu Tiến xác nhận ID thuộc chi nhánh khác.
node frontend/scripts/check-f2-f3-staff-staging.mjs
```

Không dùng ID không tồn tại để thay cho hồ sơ chi nhánh khác. Báo cáo không xuất email tài khoản hoặc mật khẩu; lỗi được lọc các thông tin đăng nhập trước khi ghi log.
