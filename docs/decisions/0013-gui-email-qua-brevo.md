# ADR 0013: Gửi email qua API HTTPS của Brevo

- **Ngày:** 18/11/2026
- **Người quyết định:** Hoàng Kim Tiến (trưởng nhóm), việc S2-45 trong [kế hoạch Sprint 2](../plans/sprint-2-ke-hoach.md)
- **Trạng thái:** Đã chấp nhận

## Bối cảnh

Sprint 2 có ba việc cần gửi email:

| Việc | Email gửi đi |
| --- | --- |
| S2-05 (F1) | Liên kết đặt lại mật khẩu khi quên mật khẩu |
| S2-36 (F2) | Liên kết đặt mật khẩu khi lễ tân tạo tài khoản cho hồ sơ có sẵn |
| S2-21 (F10) | Nhắc gói sắp hết hạn, nhắc lịch lớp |

Ràng buộc:

- **Render bản miễn phí chặn mọi kết nối ra ngoài tới cổng SMTP 25, 465 và 587**, từ tháng 9/2025 ([thông báo của Render](https://render.com/changelog/free-web-services-will-no-longer-allow-outbound-traffic-to-smtp-ports)). SMTP là giao thức gửi email truyền thống, ví dụ `smtp.gmail.com:587`. Vì vậy backend trên Render **không gửi được email bằng SMTP**, kể cả qua Gmail. Muốn mở lại thì phải trả phí Render.
- Nhóm **không có tên miền riêng**. Frontend chạy ở `*.vercel.app`, backend ở `*.onrender.com` ([ADR 0001](0001-cong-nghe.md)).
- Chỉ dùng gói miễn phí, không cần thẻ thanh toán.
- Lượng email rất nhỏ: vài chục email mỗi ngày khi demo.

## Quyết định

1. **Gửi email bằng API HTTPS của [Brevo](https://www.brevo.com/features/email-api/)** (tên cũ Sendinblue). API này đi qua cổng 443 như mọi request web, nên Render không chặn.
   - Gọi `POST https://api.brevo.com/v3/smtp/email`, khóa API đặt trong header `api-key`.
   - Gói miễn phí: 300 email mỗi ngày, không hết hạn.
   - Không dùng thư viện `spring-boot-starter-mail` (thư viện này gửi bằng SMTP, sẽ bị chặn trên Render). Dùng `RestClient` có sẵn của Spring.
2. **Người gửi là Gmail cá nhân của trưởng nhóm, đã xác minh trong Brevo.** Brevo gửi mã xác nhận tới địa chỉ đó; không cần tên miền riêng. Hệ quả: email gửi cho hội viên hiện địa chỉ này. Chấp nhận được trong phạm vi bài tập lớn; muốn đổi sang Gmail chung thì xác minh địa chỉ mới trong Brevo và sửa `MAIL_FROM_ADDRESS` trên Render, không cần tạo lại khóa API.
3. **Ba biến môi trường**, khai báo trong [`.env.example`](../../.env.example):

   | Biến | Ý nghĩa |
   | --- | --- |
   | `BREVO_API_KEY` | Khóa API. **Chỉ nhập trên Render** (yêu cầu S6), không ghi vào Git, không gửi qua chat |
   | `MAIL_FROM_ADDRESS` | Địa chỉ người gửi đã xác minh trong Brevo |
   | `MAIL_FROM_NAME` | Tên hiển thị người gửi, ví dụ `Gym Nhóm 6` |

4. **Máy cá nhân và kiểm thử không gửi email thật.** Khi `BREVO_API_KEY` để trống, backend chỉ ghi nội dung email vào log (người nhận, tiêu đề, liên kết). Nhờ vậy mọi thành viên chạy được luồng quên mật khẩu mà không cần khóa, và kiểm thử JUnit không tốn hạn mức 300 email.
5. Gửi email nằm sau một interface chung, ví dụ `EmailSender`. F1, F2 và F10 chỉ gọi interface này. Đổi nhà cung cấp về sau thì chỉ thêm một lớp cài đặt mới.

## Phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| **Brevo, API HTTPS (đã chọn)** | Không bị Render chặn; 300 email/ngày miễn phí; không cần tên miền riêng | Phải tạo tài khoản và xác minh người gửi; email gửi từ địa chỉ Gmail có thể vào thư rác |
| Gmail SMTP với mật khẩu ứng dụng | Quen thuộc, không cần đăng ký thêm | Render miễn phí chặn cổng SMTP, không gửi được trên staging |
| Resend | API dễ dùng | Không có tên miền riêng thì chỉ gửi được tới chính email chủ tài khoản ([tài liệu Resend](https://resend.com/docs/knowledge-base/403-error-resend-dev-domain)), không dùng được cho hội viên |
| Mailjet | Cũng có API HTTPS và gói miễn phí | Hạn mức ngày thấp hơn Brevo; không có lợi thế gì thêm |
| Nâng Render lên gói trả phí để mở cổng SMTP | Giữ SMTP truyền thống | Tốn tiền; trái với chủ trương chỉ dùng gói miễn phí ở ADR 0001 |

## Hệ quả

- **Dễ hơn:**
  - Staging gửi được email thật để demo quên mật khẩu, tạo tài khoản và nhắc lịch.
  - Máy cá nhân không cần khóa gì vẫn chạy được, vì email chỉ ghi ra log.
- **Khó hơn, cần theo dõi:**
  - **Email có thể vào thư rác,** vì người gửi là địa chỉ Gmail chứ không phải tên miền riêng có xác thực. Khi demo, dặn người thử mở cả mục Spam. Nếu ảnh hưởng nặng thì cân nhắc mua tên miền và xác thực tên miền trong Brevo, bằng ADR mới.
  - **Hạn mức 300 email mỗi ngày.** Tác vụ nền F10 (S2-21) không được gửi hàng loạt cho dữ liệu mẫu. Dữ liệu mẫu dùng địa chỉ không có thật (ví dụ `@example.com`), mà email gửi tới địa chỉ không tồn tại vẫn bị trừ hạn mức và làm giảm uy tín người gửi. Cần chặn gửi tới các địa chỉ này trên staging.
  - Thêm một dịch vụ ngoài [ADR 0001](0001-cong-nghe.md) (Vercel, Render, Neon). Khóa API do trưởng nhóm giữ.
- **Cần cập nhật theo:**
  1. `.env.example`: thêm ba biến ở trên (cùng PR với ADR này).
  2. ~~Trưởng nhóm tạo tài khoản Brevo, xác minh người gửi, tạo khóa API rồi nhập ba biến lên Render.~~ Đã xong 18/11/2026: khóa API chỉ có quyền Transactional email; tắt chặn IP lạ (Render đổi IP liên tục); gửi thử qua API trả `201` và người nhận đã nhận được.
  3. S2-05 (Bùi Trần Đức Việt): viết `EmailSender`, lớp gửi qua Brevo và lớp chỉ ghi log; khai báo biến trong `application-staging.yml` và `application-local.yml`.
