# Tích hợp frontend F1 (tạm thời)

Tài liệu này ghi lại adapter frontend được triển khai trong khi API F1 backend chưa có trên `main`. Đây là **giả định để viết UI và kiểm thử Vitest**, không phải hợp đồng API đã được hai phía duyệt. Trước khi nối backend thật, người phụ trách F1 backend cần xác nhận DTO response và cập nhật adapter cùng tài liệu.

## Đường dẫn đã có trong SRS

Axios dùng base `/api/v1`; các hàm trong `features/auth/api/` truyền phần đường dẫn `/auth/...`. Trình duyệt gọi cùng domain để cookie refresh đi qua proxy Vite/Vercel theo ADR 0008.

| Request                      | Contract request đã nêu trong SRS                                                                      | Dùng ở frontend                                                         |
| ---------------------------- | ------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------- |
| `POST /api/v1/auth/register` | `fullName`, `email`, `phone`, `password`, `dob?`, `gender?`; trả `201`, access token và cookie refresh | Gửi `fullName`, `email`, `phone`, `password`, `dob?`; chưa gửi `gender` |
| `POST /api/v1/auth/login`    | `email`, `password`; trả `200`, access token và cookie refresh                                         | Gửi email/mật khẩu; không hỗ trợ đăng nhập bằng số điện thoại           |
| `POST /api/v1/auth/refresh`  | Cookie `refresh_token`; xoay refresh token                                                             | Dùng riêng client cùng domain, không gắn bearer                         |
| `POST /api/v1/auth/logout`   | Thu hồi cookie refresh và trả `204`                                                                    | Gọi trước khi kết thúc logout trên server; giao diện xóa token ngay     |
| `GET /api/v1/auth/me`        | Lấy tài khoản hiện tại; trả `200`                                                                      | Bootstrap và lấy role/email cho menu                                    |

## DTO tạm trong frontend

SRS chốt trường request và tên `accessToken`, nhưng chưa định nghĩa JSON shape của đối tượng tài khoản trong login/register/me. Adapter hiện tại tạm coi login/register/refresh trả `{ "accessToken": "..." }`, sau đó gọi `/auth/me` và tạm coi response là `{ "email": "...", "role": "MEMBER" }`. Các response này có thể còn trường khác; frontend chỉ dùng access token và email/role.

**Cần backend owner xác nhận trước khi tích hợp thật:** envelope có bọc `user` hay không; chính xác shape/casing của `/me`; role có đúng năm giá trị hiện ở `auth/roles.ts`; giá trị hợp lệ của `gender`; phone DTO nhận chuỗi đã chuẩn hóa hay chấp nhận cả dạng `+84`. Backend phải tự chuẩn hóa và validate request dù frontend đã làm kiểm tra trải nghiệm.

Refresh cookie theo SRS: `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`. Access JWT sống 15 phút và chỉ lưu trong memory; frontend không đọc cookie hoặc ghi token vào Web Storage. Form mật khẩu xác nhận chỉ dùng phía frontend và không nằm trong request.

## Còn thiếu để xác minh với backend thật

- Migration/schema `users`, `members`, `refresh_tokens` và seed chi nhánh mặc định `CN01`.
- Public routes cho register/login/refresh/logout, JWT resource server cho API bảo vệ, `/auth/me`, và mapping role trong backend.
- Quy tắc chuẩn hóa +84, kiểm tra trùng phone/email, `ProblemDetail.errors`, cookie flags theo môi trường.
- Request Postman và JUnit theo user stories [#46 đăng ký](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/46), [#47 login/refresh/logout](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/47), [#48 phân quyền](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/48).

Các bài Vitest mock ở frontend kiểm tra hành vi giao diện, luồng token và adapter; chúng **không thay thế** kiểm thử HTTP với backend, cookie thật, migration hoặc xác minh response DTO. Cần chạy Postman/staging sau khi backend F1 sẵn sàng.
