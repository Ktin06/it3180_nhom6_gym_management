# Tích hợp frontend F1

Cập nhật 07/11/2026. Hợp đồng JSON đã được Bùi Trần Đức Việt (`VietBTD306`, phụ trách backend F1) xác nhận tại [comment PR #80](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/80#issuecomment-6624574276) và bổ sung vào [SRS F1 mục 6.1 tại commit a2fa017](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/a2fa01764b377fad74dac68cbeee97dce8c569d1/docs/requirements/srs/dac-ta-use-case-f1-tai-khoan-va-phan-quyen.md). Backend #25/#26 đã merge vào main qua [#87](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/87) và [#91](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/91). Đã đối chiếu request/response với mã đã merge; tích hợp HTTP/cookie thật vẫn chưa được xác minh.

## Đường dẫn và dữ liệu đã thống nhất

Axios dùng base `/api/v1`; các hàm trong `features/auth/api/` truyền phần đường dẫn `/auth/...`. Trình duyệt gọi cùng domain để cookie refresh đi qua proxy Vite/Vercel theo ADR 0008.

| API | Yêu cầu / phản hồi | Dùng ở frontend |
| --- | --- | --- |
| `POST /api/v1/auth/register` | `fullName`, `email`, `phone`, `password`, `dob?`, `gender?`; trả `201`, `{accessToken}` và cookie refresh | Form gửi trường bắt buộc và `dob?`; không gửi `gender` vì trường này tùy chọn |
| `POST /api/v1/auth/login` | `email`, `password`; trả `200`, `{accessToken}` và cookie refresh | Đăng nhập bằng email/mật khẩu |
| `POST /api/v1/auth/refresh` | Không có body, dùng cookie `refresh_token`; trả `200`, `{accessToken}`, xoay cookie | Client riêng cùng domain, không gắn bearer |
| `POST /api/v1/auth/logout` | Thu hồi cookie refresh, trả `204` | Chỉ xóa access token và điều hướng sau khi server xác nhận; nếu lỗi mạng thì giữ giao diện hiện tại để thử lại. Tải lại trang sẽ kiểm tra phiên bằng cookie refresh hiện có |
| `GET /api/v1/auth/me` | Bearer access token; trả `200`, `{userId,email,role}` | Sau lấy token, đọc tài khoản và ánh xạ email/role cho menu |

## DTO xác nhận

Login/register/refresh trả JSON phẳng, không bọc thêm đối tượng:

```json
{ "accessToken": "<jwt>" }
```

`GET /api/v1/auth/me` trả JSON phẳng:

```json
{ "userId": 15, "email": "reception@example.com", "role": "RECEPTIONIST" }
```

- `AuthMeResponse.userId` là số trong JSON, ánh xạ khóa `bigint` backend. Adapter kiểm tra số nguyên dương; `AuthUser` hiện chỉ giữ email/role vì các màn hình hiện tại chưa dùng ID.
- Vai trò hợp lệ: `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST`, `TRAINER`, `MEMBER`.
- `RegisterRequest.gender?`: `MALE`, `FEMALE`, `OTHER` hoặc `null`. Form hiện để trống/không gửi trường tùy chọn này; kiểu TypeScript hỗ trợ đầy đủ tập giá trị đã chốt, không còn dùng chuỗi bất kỳ.
- Backend nhận số điện thoại `0912345678` hoặc `+84 912 345 678`, tự chuẩn hóa về 10 chữ số bắt đầu bằng `0` trước khi kiểm tra. Frontend vẫn chuẩn hóa để hỗ trợ người dùng; backend chịu trách nhiệm kiểm tra và tính duy nhất.

Refresh cookie: `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`. JWT sống 15 phút và chỉ lưu trong bộ nhớ tab; frontend không đọc cookie hoặc ghi token vào Web Storage. Mật khẩu xác nhận chỉ dùng phía giao diện, không nằm trong request.

## Lỗi và phục hồi phiên sau review 07/11

- Khi đăng ký đã thành công nhưng `/auth/me` lỗi, giao diện báo tài khoản đã được tạo. Nút tải lại thông tin chỉ hoàn tất phiên, không gửi lại đăng ký; người dùng cũng có thể chuyển sang đăng nhập. Nội dung cần quyền vẫn đóng tới khi nhận được identity hợp lệ.
- Refresh trả `401` xác nhận phiên không hợp lệ thì kết thúc phiên. Lỗi kết nối hoặc server tạm thời đưa giao diện vào trạng thái chưa xác minh, ẩn nội dung được bảo vệ và cho thử lại; không tự coi là đã đăng xuất. Token access chỉ ở bộ nhớ, cookie vẫn do backend quản lý.
- Logout chỉ kết thúc phiên sau khi backend xác nhận. Lỗi mạng giữ giao diện hiện tại và cảnh báo “đăng xuất chưa hoàn tất” qua message portal; nút Thử lại vẫn có thể dùng. Nếu tải lại trước khi logout thành công, bootstrap kiểm tra cookie và có thể khôi phục phiên còn hiệu lực. Frontend không tự xóa được cookie HttpOnly.

Các ca này đã được kiểm thử frontend, chưa phải kết quả HTTP/cookie thật. Ngày07/11, #99/#101/#97 vẫn chưa merge. F1 đã bổ sung service vào nhánh #97 tại [03bab42](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/97#issuecomment-6703078410); phần adapter/migration F2 và [các blocker schema mới](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/101#issuecomment-6702165875) còn review. Phần ghi “19:53 ngày06/11” bên dưới là snapshot trước đó; không coi tiến độ trên nhánh này là đã có trên main.

## Điều kiện để kiểm tra tích hợp thật

Theo [review của trưởng nhóm](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/80#pullrequestreview-5756512423), **PR #80 phải giữ Draft tới khi #25 và #26 merge vào main và tích hợp thật được xác minh**. Main vẫn giữ công cụ giả lập vai trò để đồng đội tiếp tục phát triển giao diện; nhánh này không được merge trước backend.

Tại main `4928b2e` ngày 06/11, phần auth có migration `users`, `refresh_tokens`, nhưng chưa có schema `members`, `branches` và dữ liệu chi nhánh mặc định; schema chung ở [PR #101](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/101) còn mở. Ngoài ra, [DefaultMemberService](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/4928b2e8c8e76ced999df815a3f0e346854bf59c/backend/src/main/java/vn/edu/hust/nhom6/gym/feature/auth/service/DefaultMemberService.java) hiện chỉ giữ số điện thoại trong bộ nhớ, chưa ghi hồ sơ hội viên bền vững. Backend auth có thể khởi động với hai bảng auth và cấu hình phù hợp; điều đó chưa đủ để nghiệm thu đăng ký theo UC-F1.1. Cần service F2 thật cùng schema liên quan; chỉ merge schema chưa thay được phần service này. Phần triển khai thuộc người phụ trách backend F1/F2, không thuộc PR frontend.

**Phụ thuộc được kiểm tra lại lúc 19:53 ngày 06/11 (giờ Việt Nam):**

- Schema #101 chưa merge. Trưởng nhóm yêu cầu bổ sung các nhóm CHECK và đồng bộ ghi chú ERD, sửa chú thích migration và điền mô tả PR trước khi gộp: [review ràng buộc](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/101#issuecomment-6685207011), [các việc còn lại](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/101#issuecomment-6685207661). Phần này thuộc người phụ trách schema.
- [PR F2 #97](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/97) chưa merge. Tác giả ghi rõ adapter production cho dịch vụ tài khoản/chi nhánh chưa có; kết quả tích hợp trong PR dùng adapter kiểm thử. Không coi các kết quả đó là bằng chứng frontend đã đăng ký hồ sơ thật trên main hoặc staging.
- Proxy staging còn phụ thuộc #99. Theo [review của trưởng nhóm](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/99#issuecomment-6685208195), destination phải giữ `/api/:path*`; nhánh hiện còn mang schema chưa merge nên tác giả cần tách thay đổi Vercel sau khi #101 được gộp. Chờ cấu hình đúng vào main trước khi kiểm tra cookie qua domain/proxy; không lấy nguyên nhánh #99 để vượt phụ thuộc schema.

Sau khi các phụ thuộc sẵn sàng, fetch main và kiểm tra:

1. Schema `users`, `members`, `refresh_tokens`, seed chi nhánh mặc định `CN01` và backend F1 chạy được theo quy trình nhóm.
2. Đăng ký ghi tài khoản/hồ sơ thật vào CSDL qua service F2; dữ liệu và tính duy nhất SĐT còn đúng sau khi khởi động lại backend. Trùng số điện thoại/email và lỗi trường trả đúng `ProblemDetail`.
3. Đăng nhập cho các vai trò; `/me` trả đúng JSON và điều hướng đúng quyền.
4. Refresh xoay token, tải lại trang giữ phiên; kiểm tra `Set-Cookie` và hành vi cookie thực tế qua domain/proxy.
5. Đăng xuất thu hồi refresh token; refresh token cũ không tiếp tục dùng, route bảo vệ quay về đăng nhập.
6. Chạy Postman/staging theo các story [#46](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/46), [#47](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/47), [#48](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/48) cùng người phụ trách backend/triển khai.

Vitest mock kiểm tra giao diện, luồng token và adapter theo DTO đã xác nhận; **không chứng minh backend, cookie thật hay staging hoạt động**. Chưa thực hiện các bước tích hợp trên: còn phụ thuộc schema/service F2 bền vững và môi trường thử phù hợp. Không nhập schema chưa duyệt, sửa backend hoặc quy trình phát triển trên main để vượt phụ thuộc này. Giữ PR #80 Draft dù #25/#26 đã merge.
