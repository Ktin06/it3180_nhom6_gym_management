# ADR 0008: Gọi API cùng domain qua proxy của Vercel

- **Ngày:** 01/11/2026
- **Người quyết định:** Hoàng Kim Tiến (trưởng nhóm), cần cả nhóm xác nhận
- **Trạng thái:** Đã chấp nhận

## Bối cảnh

S2 yêu cầu refresh token nằm trong cookie httpOnly. Frontend triển khai trên Vercel (`*.vercel.app`), backend trên Render (`*.onrender.com`) ([ADR 0001](0001-cong-nghe.md)). Nếu trình duyệt gọi thẳng backend, cookie của backend là cookie bên thứ ba. Safari (mặc định trên iPhone) chặn loại cookie này, nên hội viên dùng iPhone sẽ bị đăng xuất mỗi khi access token hết hạn (15 phút). Muốn gửi được cookie khác domain còn phải đặt `SameSite=None` và tự chống CSRF cho API làm mới token.

## Quyết định

- Trình duyệt chỉ gọi API theo đường dẫn tương đối `/api/v1/...` trên chính domain của frontend.
  - Khi deploy: `frontend/vercel.json` có quy tắc rewrite chuyển tiếp `/api/*` sang backend trên Render.
  - Khi chạy máy cá nhân: `vite.config.ts` chuyển tiếp `/api` sang `http://localhost:8080`.
- Refresh token đặt trong cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`. Vì cùng domain nên mọi trình duyệt đều gửi cookie, và `SameSite=Strict` chặn CSRF cho API làm mới token.
- Access token (JWT) gửi qua header `Authorization`, nên vẫn tắt CSRF của Spring Security như hiện tại.
- WebSocket (F10) không đi qua rewrite của Vercel mà nối thẳng Render; xác thực bằng access token trong khung CONNECT của STOMP, không dùng cookie. `CORS_ALLOWED_ORIGINS` của backend giữ lại cho kết nối này.

## Phương án đã cân nhắc

| Phương án | Lý do không chọn |
| --- | --- |
| Gọi thẳng Render, cookie `SameSite=None; Secure` | Safari trên iPhone chặn cookie bên thứ ba; phải tự làm chống CSRF |
| Lưu refresh token trong `localStorage` | Trái S2; mã độc chạy trên trang (XSS) đọc được token |
| Mua tên miền riêng cho cả frontend và backend | Tốn phí, cấu hình DNS; không cần cho demo |

## Hệ quả

- Sau khi tạo dịch vụ trên Render, thay địa chỉ giả `https://thay-bang-dia-chi-render.invalid` trong `frontend/vercel.json` bằng địa chỉ thật. Địa chỉ giả dùng đuôi `.invalid` (không bao giờ phân giải được), để quên thay thì chỉ lỗi chứ không gửi dữ liệu tới máy chủ lạ.
- Frontend không cần biến `VITE_API_URL` nữa; biến chỉ dùng khi muốn gọi thẳng một backend khác.
- Mọi request đi qua Vercel nên địa chỉ IP thật của người dùng (cột `audit_logs.ip`, F17) nằm trong header `X-Forwarded-For`.
- Render bản miễn phí tạm dừng dịch vụ sau một thời gian không có request; lần gọi đầu sau đó chậm. Trước buổi demo nên mở web trước vài phút.
