# Bối cảnh đề tài: Web quản lý chuỗi phòng tập gym (Nhóm 6)

Cập nhật: 01/11/2026. File này là tóm tắt các quyết định đã chốt. Chi tiết đầy đủ nằm trong "Bản chốt phạm vi" (phạm vi, quy tắc nghiệp vụ) và "Thiết kế dữ liệu" cùng file `erd-v2.dbml` (cơ sở dữ liệu). Khi có mâu thuẫn, các tài liệu đó là bản chuẩn.

## Thông tin chung
- Môn: Nhập môn Công nghệ phần mềm (IT3180), Đại học Bách khoa Hà Nội
- Giảng viên hướng dẫn: TS. Bùi Thị Mai Anh
- Nhóm 6, 8 thành viên; trưởng nhóm (quản lý dự án): Hoàng Kim Tiến

| Họ tên | Mã số sinh viên |
|---|---|
| Hoàng Kim Tiến | 202416367 |
| Bùi Trần Đức Việt | 202416402 |
| Nguyễn Anh Tú | 202400078 |
| Lê Phước Minh Quân | 202416324 |
| Thái Hùng Lân | 202416261 |
| Nguyễn Trọng Việt | 202416399 |
| Nguyễn Phú Trọng | 202400075 |
| Phạm Gia Bảo | 202416425 |

- Thời gian: 4 sprint mỗi sprint 2 tuần, từ 27/10/2026 (tuần 9) đến 21/12/2026. Bàn giao Sprint 1 ngày 10/11/2026; bàn giao cuối và bảo vệ ở tuần 16. Phân công Sprint 1 theo file kế hoạch Sprint 1.
- Mã nguồn: https://github.com/Ktin06/it3180_nhom6_gym_management
- Định hướng của cô: chức năng dễ kiểm thử, dễ demo, chủ yếu là thao tác quản lý dữ liệu; không chấm điểm dựa trên tính năng AI. Vì vậy hệ thống chỉ có một tính năng AI duy nhất là nhận diện khuôn mặt ở version 5.

## Mục tiêu
Một hệ thống tập trung cho chuỗi phòng tập: chủ chuỗi, quản lý chi nhánh, lễ tân, huấn luyện viên (PT) và hội viên. Hội viên tự đặt lớp, đặt lịch PT; check-in và thanh toán tại quầy. Web responsive, không làm app riêng.

## 6 vai trò
- Chủ chuỗi (Super Admin): quản lý chi nhánh, tài khoản quản lý, cấu hình, báo cáo toàn chuỗi, nhật ký hoạt động.
- Quản lý chi nhánh: gói tập, lớp học, nhân viên, ca làm, thiết bị, kho, khuyến mãi; duyệt bảo lưu, hoàn tiền; báo cáo chi nhánh.
- Lễ tân: hồ sơ hội viên, tạo tài khoản cho hồ sơ có sẵn, check-in tại quầy, bán gói, bán hàng, thu tiền, in hóa đơn; đăng ký khuôn mặt (v5).
- Huấn luyện viên (PT): lịch rảnh, lịch dạy, điểm danh lớp, xác nhận buổi PT, giáo án, tiến độ và chỉ số cơ thể học viên.
- Hội viên: xem gói, lịch sử tập, gửi yêu cầu mua hoặc gia hạn gói (thanh toán tại quầy), đặt lớp, đặt lịch PT, nhận thông báo, huy hiệu.
- Khách: xem bảng giá, lịch lớp, đăng ký tài khoản.
Từ v1 đến v3 hệ thống chạy với một chi nhánh mặc định; nhiều chi nhánh mở ở v4.

## 20 chức năng, 5 version
| Version | Chủ đề | Chức năng | Mức cam kết |
|---|---|---|---|
| v1 | Vận hành cơ bản tại quầy | F1 Tài khoản và phân quyền · F2 Hồ sơ hội viên · F3 Gói tập và gia hạn · F4 Check-in tại quầy (mã hội viên/số điện thoại) · F5 Thanh toán tại quầy · F6 Dashboard cơ bản | Bắt buộc |
| v2 | Hội viên, lớp học, huấn luyện viên | F7 Cổng hội viên · F8 Lớp học nhóm + danh sách chờ · F9 Huấn luyện viên (PT) · F10 Thông báo | Bắt buộc |
| v3 | Vận hành nâng cao | F11 Thiết bị · F12 Bán hàng tại quầy và kho · F13 Nhân sự và lương · F14 Khuyến mãi · F15 Bảo lưu và chuyển nhượng · F16 Báo cáo nâng cao · F17 Nhật ký hoạt động | Mục tiêu |
| v4 | Chuỗi phòng tập | F18 Đa chi nhánh · F19 Gamification (theo quy tắc, không AI) | Mục tiêu; cắt thứ hai nếu trễ |
| v5 | Nhận diện khuôn mặt | F20 Check-in bằng khuôn mặt (face-api.js) | Cắt đầu tiên nếu trễ |

Đã bỏ khỏi phạm vi: thanh toán online, gợi ý lớp/gói, cảnh báo bỏ tập, chatbot.

## Quyết định nghiệp vụ chính
- Tài khoản: chỉ đăng nhập bằng email. Tự đăng ký bằng số điện thoại đã có hồ sơ thì bị từ chối; lễ tân tạo tài khoản cho hồ sơ có sẵn.
- Gói tập: theo thời gian (TIME), theo số buổi (SESSION), gói PT. Hội viên được giữ đồng thời nhiều gói khác loại (ví dụ TIME và PT). Lớp nhóm đã gồm trong gói TIME; gói PT chỉ dùng cho tập 1-1. Gia hạn nối tiếp ngày hết hạn cũ (gói chờ ở trạng thái UPCOMING). Bảo lưu 7–30 ngày, tối đa 2 lần, cần quản lý duyệt.
- Cổng hội viên (F7): hội viên chọn gói trên web, hệ thống tạo hóa đơn chờ; thanh toán tại quầy thì gói kích hoạt; hóa đơn chờ quá 3 ngày tự hủy.
- Check-in: thành công khi xác định được hội viên, có gói TIME hoặc SESSION đang ACTIVE còn hạn/còn buổi tại chi nhánh, trong giờ mở cửa. Gói PT không dùng để check-in. Có cả TIME và SESSION thì dùng TIME. Gói SESSION trừ 1 buổi ở lần check-in đầu trong ngày. Màn hình hiện ảnh đại diện để lễ tân đối chiếu. v5 thay bước tìm thủ công bằng nhận diện khuôn mặt; không nhận diện được thì quay về cách thủ công.
- Lớp học nhóm: chỉ hội viên có gói TIME được đặt chỗ. Danh sách chờ theo thứ tự đến trước; có chỗ trống thì tự động đưa người đủ điều kiện đứng đầu vào lớp và báo cho họ, có 15 phút ân hạn hủy; ngừng tự động đưa vào lớp khi còn dưới 30 phút trước giờ học; hủy muộn/vắng 3 lần trong tháng bị khóa đặt lớp 7 ngày. Các con số là tham số cấu hình.
- Huấn luyện viên (PT): hội viên có gói PT đặt vào khung giờ PT đã khai báo rảnh, trước ít nhất 12 giờ; chặn trùng lịch; gói PT chỉ trừ buổi khi PT xác nhận buổi đã diễn ra.
- Thanh toán: chỉ tại quầy (tiền mặt hoặc chuyển khoản VietQR, lễ tân xác nhận); giá luôn tính ở server; gói chỉ kích hoạt khi hóa đơn đã thanh toán. Khách lẻ mua hàng không cần hồ sơ hội viên.

## Công nghệ
- Frontend: React, TypeScript, Vite, Ant Design.
- Backend: Java, Spring Boot (Spring Web, Spring Security, Spring Data JPA); Swagger (springdoc-openapi).
- Cơ sở dữ liệu: PostgreSQL, Flyway.
- Nhận diện khuôn mặt (v5): face-api.js trên trình duyệt ở quầy, vector 128 chiều gửi về máy chủ so khớp; không lưu ảnh gốc.
- Thời gian thực: WebSocket của Spring (STOMP). Email: Spring Mail.
- Kiểm thử: JUnit 5, Mockito, Testcontainers, Postman. Mã nguồn: GitHub, GitHub Actions, GitHub Projects.
- Triển khai: Vercel (frontend), Render (backend, Docker), Neon (cơ sở dữ liệu).
- Cách làm: Agile, chia sprint, mỗi chức năng bắt đầu từ user story có tiêu chí chấp nhận (quản lý trên GitHub Projects); viết mã với công cụ AI hỗ trợ. Tài liệu, báo cáo, slide viết bằng LaTeX (slide dùng template Beamer HUST).

## Dữ liệu
39 bảng chia 8 nhóm: tài khoản và hệ thống; chi nhánh và cơ sở vật chất; hội viên và gói tập; check-in và lớp học; huấn luyện viên; bán hàng và thanh toán; nhân sự; gamification. Mọi bảng nghiệp vụ gốc có `branch_id` ngay từ v1 (bảng con lấy qua bảng cha). Mỗi thông tin chỉ lưu ở một nơi (vai trò và email ở `users`, số điện thoại ở `members`/`staff`, chi nhánh nhân viên ở `staff`). Trạng thái lưu `varchar` kèm `CHECK`. Chi tiết: "Thiết kế dữ liệu" và `erd-v2.dbml`.
