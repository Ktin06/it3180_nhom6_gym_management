# Bản chốt phạm vi – Web quản lý chuỗi phòng tập Gym

Cập nhật: 03/11/2026 (bản đầu: 27/10/2026) · Hoàng Kim Tiến

## Tổng quan

Nhóm 6 (8 người) xây dựng **web quản lý chuỗi phòng tập gym** gồm 20 chức năng chia 5 version, có hỗ trợ của AI khi lập trình. Đây là bản chuẩn về phạm vi và quy tắc nghiệp vụ; các tài liệu SRS, use case, API và báo cáo LaTeX đều viết dựa trên bản này. Thiết kế dữ liệu chi tiết nằm trong tài liệu "Thiết kế dữ liệu" và file `erd-v2.dbml`.

- **Thời gian:** 4 đợt phát triển (sprint) mỗi đợt 2 tuần, từ 27/10/2026 (tuần 9) đến 21/12/2026. Bàn giao và trình bày Sprint 1 ngày 10/11/2026 (tuần 11); bàn giao cuối và bảo vệ ở tuần 16.
- **Bài toán:** phòng tập đang quản lý hội viên, gói tập, lịch PT và doanh thu bằng sổ sách, Excel và tin nhắn, dẫn đến sai hạn gói, trùng lịch, khó đối soát doanh thu và không theo dõi được nhiều chi nhánh.
- **Mục tiêu:** một hệ thống tập trung cho chủ chuỗi, quản lý chi nhánh, lễ tân, huấn luyện viên (PT) và hội viên; hội viên tự đặt lớp, đặt lịch PT; check-in và thanh toán tại quầy.
- **Định hướng của giảng viên (cô Bùi Thị Mai Anh):** ưu tiên chức năng quản lý dễ kiểm thử, dễ demo; tính năng AI không phải trọng tâm chấm điểm, nên hệ thống chỉ có một tính năng AI là nhận diện khuôn mặt ở v5.
- **Nền tảng:** web responsive, dùng được trên máy tính quầy lễ tân và trên điện thoại của hội viên. Không làm app riêng.
- **Cách phát triển:** tăng dần theo 5 version, mỗi version là một bản chạy được trên môi trường staging. Nếu trễ tiến độ thì cắt v5 trước, rồi đến v4.

| Version | Chủ đề | Chức năng | Mức cam kết |
| --- | --- | --- | --- |
| v1 | Vận hành cơ bản tại quầy (một chi nhánh mặc định) | F1–F6 | Bắt buộc |
| v2 | Hội viên, lớp học và huấn luyện viên | F7–F10 | Bắt buộc |
| v3 | Vận hành nâng cao | F11–F17 | Mục tiêu |
| v4 | Chuỗi phòng tập | F18–F19 | Mục tiêu; cắt thứ hai nếu trễ |
| v5 | Nhận diện khuôn mặt (tính năng AI duy nhất) | F20 | Cắt đầu tiên nếu trễ |

## Vai trò người dùng

Hệ thống có 6 vai trò: 5 vai trò có tài khoản đăng nhập, riêng khách dùng web không cần đăng nhập. Mỗi tài khoản có đúng một vai trò; nhân viên gắn với một chi nhánh, riêng chủ chuỗi xem được mọi chi nhánh. Từ v1 đến v3 hệ thống chạy với một chi nhánh mặc định; quản lý nhiều chi nhánh mở ở v4.

| Vai trò | Là ai | Quyền chính |
| --- | --- | --- |
| Chủ chuỗi (Super Admin) | Chủ doanh nghiệp | Quản lý chi nhánh, tài khoản quản lý, cấu hình hệ thống, xem báo cáo toàn chuỗi, xem nhật ký hoạt động |
| Quản lý chi nhánh | Người điều hành một cơ sở | Quản lý gói tập, lớp học, nhân viên, ca làm, thiết bị, kho, khuyến mãi; duyệt bảo lưu, hoàn tiền; xem báo cáo chi nhánh |
| Lễ tân | Nhân viên trực quầy | Tạo hồ sơ hội viên, tạo tài khoản cho hồ sơ có sẵn, check-in tại quầy, bán gói, bán hàng tại quầy, thu tiền, in hóa đơn; đăng ký khuôn mặt (v5) |
| Huấn luyện viên (PT) | Nhân viên dạy | Khai báo lịch rảnh, xem lịch dạy, điểm danh lớp, xác nhận buổi PT, soạn giáo án, ghi tiến độ và chỉ số cơ thể học viên |
| Hội viên | Khách đã đăng ký | Xem gói và lịch sử tập, gửi yêu cầu mua hoặc gia hạn gói (thanh toán tại quầy), đặt lớp, đặt lịch PT, xem tiến độ, nhận thông báo, huy hiệu |
| Khách | Người chưa có tài khoản | Xem bảng giá, lịch lớp, đăng ký tài khoản |

## Danh sách chức năng

20 chức năng có mã F1–F20; mã này dùng thống nhất trong SRS, user story, task, nhãn GitHub và commit. Độ khó dùng để cân bằng khi chia việc.

| Mã | Chức năng | Nội dung chính | Version | Độ khó |
| --- | --- | --- | --- | --- |
| F1 | Tài khoản và phân quyền | Đăng ký, đăng nhập bằng email, quên mật khẩu, đổi mật khẩu, phân quyền theo vai trò (5 vai trò có tài khoản, khách không cần đăng nhập) | v1 | Trung bình |
| F2 | Hồ sơ hội viên | Tạo, sửa, tìm kiếm hội viên; ảnh đại diện; chỉ số cơ thể; tạo tài khoản cho hồ sơ có sẵn | v1 | Dễ |
| F3 | Gói tập và gia hạn | Danh mục gói theo thời gian, theo buổi, gói PT; đăng ký, gia hạn, kiểm tra hạn | v1 | Trung bình |
| F4 | Check-in tại quầy | Lễ tân tìm hội viên theo mã hội viên hoặc số điện thoại, đối chiếu ảnh đại diện, kiểm tra gói hợp lệ, ghi lượt vào | v1 | Trung bình |
| F5 | Thanh toán tại quầy | Ghi nhận tiền mặt hoặc chuyển khoản (hiện mã VietQR có sẵn số tiền, lễ tân xác nhận đã nhận), tạo và in hóa đơn PDF; xử lý hóa đơn chờ do hội viên gửi từ web | v1 | Trung bình |
| F6 | Dashboard cơ bản | Doanh thu, số hội viên, lượt check-in theo ngày và theo giờ | v1 | Dễ |
| F7 | Cổng hội viên | Hội viên xem gói, lịch sử check-in, hóa đơn; chọn gói để mua hoặc gia hạn (tạo hóa đơn chờ, thanh toán tại quầy) | v2 | Dễ |
| F8 | Lớp học nhóm | Lịch lớp lặp hằng tuần, đặt chỗ, hủy chỗ, danh sách chờ tự động đưa vào lớp, điểm danh | v2 | Khó |
| F9 | Huấn luyện viên (PT) | Lịch rảnh, đặt lịch PT có kiểm tra trùng, giáo án, nhật ký tập, biểu đồ tiến độ | v2 | Khó |
| F10 | Thông báo | Email và thông báo trong web theo thời gian thực: nhắc hết hạn gói, nhắc lịch lớp, được đưa vào lớp từ danh sách chờ, đổi giờ lớp | v2 | Trung bình |
| F11 | Thiết bị | Danh sách thiết bị, báo hỏng, lịch và lịch sử bảo trì | v3 | Dễ |
| F12 | Bán hàng tại quầy (POS) và kho | Bán nước, đồ ăn, phụ kiện; nhập hàng; tồn kho; cảnh báo sắp hết | v3 | Trung bình |
| F13 | Nhân sự và lương | Ca làm, chấm công, tính lương cứng và hoa hồng PT | v3 | Trung bình |
| F14 | Khuyến mãi | Mã giảm giá theo phần trăm hoặc số tiền, giới hạn thời gian và số lượt | v3 | Dễ |
| F15 | Bảo lưu và chuyển nhượng gói | Tạm dừng gói có duyệt, chuyển gói cho người khác có phí | v3 | Trung bình |
| F16 | Báo cáo nâng cao | Báo cáo theo gói, PT, lớp; tỷ lệ giữ chân; xuất Excel và PDF (theo chi nhánh từ v4) | v3 | Trung bình |
| F17 | Nhật ký hoạt động (audit log) | Ghi lại ai thay đổi dữ liệu quan trọng, lúc nào, giá trị cũ và mới | v3 | Dễ |
| F18 | Đa chi nhánh | Quản lý chi nhánh, phân quyền theo chi nhánh, gói dùng một hoặc toàn chuỗi, báo cáo toàn chuỗi | v4 | Trung bình |
| F19 | Gamification | Chuỗi ngày tập, huy hiệu, bảng xếp hạng trong chi nhánh (tính theo quy tắc, không dùng AI) | v4 | Dễ |
| F20 | Check-in bằng nhận diện khuôn mặt | Đăng ký khuôn mặt (có xác nhận đồng ý); webcam tại quầy nhận diện bằng face-api.js thay cho bước tìm thủ công của F4 | v5 | Khó |

Đối chiếu với mã của bản 10/10 (nếu đã lỡ dùng trong task, commit): F1–F3, F5–F9 giữ nguyên; F4 cũ tách thành F4 (check-in thủ công) và F20 (khuôn mặt); F11 → F10; F12 → F18; F13 → F11; F14 → F12; F15 → F13; F16 → F14; F17 → F15; F18 → F16; F19 → F17; F23 → F19. Đã bỏ: F10 (thanh toán online), F20 (gợi ý lớp và gói), F21 (cảnh báo bỏ tập), F22 (chatbot).

## Quy tắc nghiệp vụ

Các giá trị có dấu \* là tham số cấu hình (bảng `system_configs`), quản lý thay đổi được mà không sửa mã. Người viết mã và người viết kiểm thử dùng chung các quy tắc này.

**Tài khoản (F1, F2)**

- Chỉ đăng nhập bằng email. Mỗi email và mỗi số điện thoại hội viên là duy nhất trong hệ thống.
- Khách tự đăng ký trên web thì hệ thống tạo đồng thời tài khoản và hồ sơ hội viên (bắt buộc có số điện thoại), gắn với chi nhánh mặc định.
- Nếu số điện thoại khi tự đăng ký đã thuộc một hồ sơ hội viên có sẵn (do lễ tân tạo trước), hệ thống từ chối và hướng dẫn liên hệ quầy. Lễ tân tạo tài khoản cho hồ sơ đó và hệ thống gửi email đặt mật khẩu. Không tự gắn tài khoản mới vào hồ sơ cũ, để tránh xem trộm dữ liệu của người khác (S3).
- Lễ tân được tạo hồ sơ hội viên chưa có tài khoản; hội viên chưa có tài khoản không dùng được cổng hội viên và không nhận email.
- Ngừng hoạt động một hội viên thì đồng thời khóa tài khoản của họ.

**Gói tập (F3, F7, F15)**

- Có 3 loại gói: theo thời gian (TIME: 1, 3, 6, 12 tháng; hạn gói tính theo số ngày, 1 tháng = 30 ngày), theo số buổi (SESSION), và gói PT (số buổi tập 1-1 với huấn luyện viên).
- Một hội viên được giữ đồng thời nhiều gói khác loại, ví dụ một gói TIME và một gói PT; mỗi loại tối đa một gói đang hiệu lực tại mỗi chi nhánh.
- Lớp học nhóm đã gồm trong gói TIME. Gói PT chỉ dùng cho buổi tập 1-1, không dùng để vào phòng tập hay đặt lớp.
- Gói SESSION và gói PT cũng có hạn dùng. Ngày hết hạn là ngày cuối còn dùng được: ngày hết hạn = ngày bắt đầu + số ngày của gói − 1 (gói 30 ngày bắt đầu 01/11 thì hết hạn 30/11). Gói hết hạn mà còn buổi thì số buổi còn lại bị hủy, không hoàn tiền.
- Gói hết hiệu lực khi qua ngày hết hạn; riêng gói SESSION còn hết hiệu lực từ ngày sau ngày bị trừ buổi cuối cùng, gói PT hết hiệu lực ngay khi buổi cuối được xác nhận (tùy điều nào đến trước).
- Gia hạn khi gói cũ còn hiệu lực thì gói mới bắt đầu ngay sau ngày gói cũ hết hiệu lực; trong thời gian chờ, gói mới ở trạng thái UPCOMING. Ngày bắt đầu tính lúc hóa đơn được thanh toán: khi đó gói SESSION cũ đã về 0 buổi thì gói mới bắt đầu từ ngày mai.
- Gói SESSION cũ dùng hết buổi trước ngày hết hạn trong khi đã có gói nối tiếp (UPCOMING) cùng loại: ngay khi trừ buổi cuối, gói nối tiếp được kéo lịch về bắt đầu từ ngày mai, giữ nguyên số ngày hiệu lực.
- Hội viên mua hoặc gia hạn từ cổng hội viên (F7): hội viên chọn gói, hệ thống tạo hóa đơn chờ thanh toán (PENDING) và gói PENDING; hội viên ra quầy trả tiền, lễ tân xác nhận thì gói được kích hoạt. Ngày bắt đầu gói tính lại tại thời điểm thanh toán. Hóa đơn chờ do hội viên tạo từ cổng hội viên quá 3 ngày\* tự động bị hủy; hóa đơn lễ tân lập tại quầy không tự hủy.
- Bảo lưu: không áp dụng cho gói SESSION, PT đã hết buổi (kiểm tra cả lúc tạo yêu cầu và lúc quản lý duyệt); mỗi lần từ 7 đến 30 ngày\*, tối đa 2 lần mỗi gói\*, cần quản lý chi nhánh duyệt; ngày hết hạn được cộng thêm đúng số ngày bảo lưu. Nếu gói đang có gói gia hạn nối tiếp (UPCOMING) thì gói nối tiếp cũng được dời thêm đúng số ngày đó.
- Chuyển nhượng: chỉ gói còn ít nhất 30 ngày\* (gói SESSION, PT phải còn ít nhất 1 buổi, kiểm tra cả lúc tạo yêu cầu và lúc xác nhận thanh toán phí; gói hết buổi giữa chừng thì không nhận phí, hủy yêu cầu và hóa đơn phí), người nhận phải có hồ sơ hội viên, thu phí chuyển nhượng\* (mặc định 200.000 đ) bằng hóa đơn; gói cũ chuyển trạng thái TRANSFERRED, người nhận nhận một gói mới giữ nguyên ngày hết hạn và số buổi còn lại.

**Check-in (F4, F20)**

- Check-in thành công khi: xác định được hội viên; có gói TIME hoặc SESSION đang ACTIVE dùng được tại chi nhánh đó (còn hạn; gói SESSION còn buổi, hoặc đã trừ buổi trong hôm nay); và trong giờ mở cửa. Hội viên chỉ có gói PT thì không check-in được.
- Hội viên có cả gói TIME và gói SESSION thì hệ thống dùng gói TIME và không trừ buổi.
- F4 (từ v1): lễ tân tìm hội viên theo mã hội viên hoặc số điện thoại; màn hình luôn hiện ảnh đại diện để đối chiếu, chống mượn thẻ.
- Gói SESSION bị trừ 1 buổi ở lần check-in đầu tiên trong ngày (tính theo giờ Việt Nam); check-in lại trong ngày không bị trừ thêm. Buổi đã trừ có giá trị đến hết ngày đó: mỗi ngày trừ tối đa 1 buổi, gói vừa về 0 buổi vẫn check-in lại được trong ngày, từ ngày hôm sau mới bị từ chối. Nếu hội viên có buổi lớp sắp bắt đầu, chỗ đặt được đánh dấu "đã đến"; huấn luyện viên vẫn xác nhận có mặt tại lớp.
- F20 (v5): chỉ đăng ký khuôn mặt khi hội viên đã đánh dấu đồng ý. face-api.js chạy trên trình duyệt ở quầy, trích vector đặc trưng 128 chiều và gửi về máy chủ để so khớp; không lưu ảnh gốc; hội viên có quyền yêu cầu xóa.
- Không nhận diện được thì quay về cách của F4; hệ thống ghi rõ phương thức check-in (MANUAL hoặc FACE).

**Dashboard (F6)**

- Cảnh báo gói sắp hết: gói ACTIVE hết hạn trong 7 ngày tới, hoặc gói SESSION còn không quá 2 buổi\*. Không cảnh báo gói mà hội viên đã có gói gia hạn nối tiếp (UPCOMING) cùng loại.

**Lớp học nhóm (F8)**

- Chỉ hội viên có gói TIME đang ACTIVE dùng được tại chi nhánh mới được đặt chỗ hoặc vào danh sách chờ.
- Mở đặt chỗ trước 7 ngày\*; mỗi lớp có sĩ số tối đa không vượt sức chứa phòng; một hội viên không được đăng ký hai buổi trùng giờ; không giới hạn số danh sách chờ một người tham gia. Đã hủy một buổi thì vẫn đặt lại được buổi đó.
- Lớp đầy thì vào danh sách chờ theo nguyên tắc đến trước, xếp trước (cùng thời điểm thì ai vào chờ trước theo thứ tự ghi nhận đứng trước). Khi có chỗ trống, hệ thống **tự động đưa vào lớp** người đủ điều kiện đứng đầu và báo cho họ; không cần xác nhận. Người được đưa vào có 15 phút\* ân hạn để hủy mà không bị tính hủy muộn, tính từ lúc được đưa vào lớp. Người không còn đủ điều kiện (gói hết hạn, đang bị khóa, đã có lịch trùng giờ) bị bỏ qua. Buổi còn chỗ mà có người chờ thì xét danh sách chờ trước, rồi mới nhận người đặt mới. Rời danh sách chờ không bị tính hủy muộn.
- Khi còn dưới 30 phút\* trước giờ học: ngừng tự động đưa vào lớp, đồng thời đóng đặt chỗ mới và vào danh sách chờ; chỗ trống sau mốc này được để trống. Chỗ trống ban đêm vẫn được lấp ngay; thông báo trong web hiện ngay, email gửi khi tới giờ mở cửa chi nhánh. Thời điểm gửi email không ảnh hưởng ân hạn ([ADR 0010](../decisions/0010-danh-sach-cho-tu-dong-dua-vao-lop-ban-2.md)).
- Hủy trước giờ học ít nhất 2 giờ\* thì không bị phạt; hủy muộn hoặc vắng mặt (theo điểm danh của huấn luyện viên) 3 lần trong tháng\* thì bị khóa đặt lớp 7 ngày\*. Người không đủ điều kiện không được đặt chỗ hay vào chờ, và được báo ngay lý do.
- Đổi phòng: chỉ thông báo. Đổi giờ: hội viên đã có chỗ xác nhận lại trong 60 phút\*; không xác nhận hoặc báo không tham gia thì mất chỗ, không bị phạt, chỗ được chuyển cho danh sách chờ. Hủy buổi: hủy mọi chỗ đặt và danh sách chờ, không phạt, thông báo cho tất cả.
- Lễ tân được thao tác hộ hội viên nhưng không có ưu tiên; không ai được xếp hội viên vào lớp ngoài thứ tự danh sách chờ.

**Huấn luyện viên (F9)**

- Hội viên có gói PT đang ACTIVE mới đặt được buổi PT; chỉ đặt vào khung giờ huấn luyện viên đã khai báo rảnh, trước ít nhất 12 giờ\*. Số buổi PT đã đặt mà chưa diễn ra phải ít hơn số buổi còn lại của gói thì mới đặt thêm được, để không bao giờ đặt quá số buổi.
- Hệ thống chặn trùng lịch cho cả huấn luyện viên (kể cả với giờ dạy lớp nhóm) và hội viên.
- Gói PT chỉ bị trừ buổi khi huấn luyện viên xác nhận buổi tập đã diễn ra. Xác nhận buổi cuối thì gói PT chuyển EXPIRED ngay; nếu có gói PT nối tiếp (UPCOMING) cùng phạm vi, gói đó được kéo về bắt đầu và kích hoạt ngay hôm đó, giữ nguyên số ngày hiệu lực.
- Hoa hồng PT (F13) tính cho mỗi buổi đã xác nhận: tỷ lệ hoa hồng của huấn luyện viên × giá một buổi (giá gói PT sau giảm giá ÷ số buổi của gói). Số tiền được chốt lúc xác nhận, không đổi khi tỷ lệ hay giá gói thay đổi về sau.

**Thanh toán (F5, F7, F12, F14)**

- Chỉ thanh toán tại quầy: tiền mặt hoặc chuyển khoản qua mã VietQR có sẵn số tiền; lễ tân hoặc quản lý bấm xác nhận đã nhận tiền.
- Hóa đơn do lễ tân lập tại quầy, hoặc do hội viên tạo ở trạng thái chờ khi gửi yêu cầu mua hoặc gia hạn gói (F7).
- Giá luôn lấy từ server; không tin số tiền do trình duyệt gửi lên. Dòng hóa đơn lưu giá tại thời điểm bán.
- Gói chỉ kích hoạt khi hóa đơn ở trạng thái đã thanh toán.
- Khách lẻ mua hàng tại quầy không cần hồ sơ hội viên; hóa đơn có gói tập hoặc phí chuyển nhượng bắt buộc gắn với một hội viên.
- Mỗi hóa đơn áp dụng tối đa một mã khuyến mãi.
- Chỉ quản lý chi nhánh được hoàn tiền; mọi lần hoàn tiền đều ghi vào nhật ký hoạt động.

## Phạm vi không làm

Các mục dưới đây cố ý để ngoài phạm vi để giữ dự án vừa thời gian 4 sprint; đưa vào phần hướng phát triển của báo cáo.

- Ứng dụng di động riêng (iOS, Android).
- Thiết bị phần cứng: cổng từ, máy chấm công vân tay, thiết bị IoT gắn trên máy tập.
- Hội viên tự check-in bằng khuôn mặt khi không có lễ tân giám sát; phát hiện giả mạo bằng ảnh (liveness detection) chuyên sâu.
- Thanh toán online qua cổng thanh toán (VNPay, ví điện tử); mọi khoản thu đều ghi nhận tại quầy.
- Các tính năng AI khác ngoài nhận diện khuôn mặt: gợi ý lớp và gói, cảnh báo nguy cơ bỏ tập, chatbot. Giảng viên không chấm điểm dựa trên tính năng AI.
- Tự huấn luyện mô hình AI; chỉ dùng mô hình có sẵn của face-api.js.
- Kế toán, thuế, xuất hóa đơn điện tử theo quy định.
- CRM và marketing tự động (chăm sóc khách tiềm năng, chiến dịch email).
- Đăng nhập bằng số điện thoại hoặc tài khoản mạng xã hội.
- Đa ngôn ngữ; giao diện chỉ có tiếng Việt.

## Yêu cầu phi chức năng và bảo mật

Mỗi yêu cầu có con số hoặc tiêu chí kiểm tra được, để phần kiểm thử chứng minh được hệ thống đạt.

| Mã | Nhóm | Yêu cầu |
| --- | --- | --- |
| NF1 | Hiệu năng | Tra cứu hội viên và tải trang danh sách dưới 2 giây với 10.000 hội viên; check-in thủ công (F4) phản hồi dưới 2 giây |
| NF2 | Hiệu năng | Nhận diện khuôn mặt và trả kết quả check-in (F20) dưới 3 giây |
| NF3 | Tải | Phục vụ 100 người dùng đồng thời không lỗi |
| NF4 | Giao diện | Dùng tốt trên màn hình từ 360px; chạy trên Chrome, Edge, Firefox bản mới |
| NF5 | Dữ liệu | Sao lưu cơ sở dữ liệu tự động hằng ngày |
| NF6 | Toàn vẹn | Đặt chỗ lớp và đặt lịch PT chạy trong giao dịch cơ sở dữ liệu có khóa bản ghi; không vượt sĩ số, không trùng lịch khi nhiều người đặt cùng lúc |
| S1 | Mật khẩu | Băm bằng bcrypt; tối thiểu 8 ký tự gồm chữ và số |
| S2 | Phiên đăng nhập | Access token (JWT) hết hạn sau 15 phút; refresh token trong cookie httpOnly |
| S3 | Phân quyền | Kiểm tra vai trò và quyền sở hữu dữ liệu ở backend (Spring Security) trên mọi API; hội viên không xem được dữ liệu của hội viên khác |
| S4 | Dò mật khẩu | Khóa đăng nhập 15 phút sau 5 lần sai liên tiếp |
| S5 | Đầu vào | Validate mọi request ở backend (Bean Validation); dùng JPA với tham số, không nối chuỗi SQL |
| S6 | Khóa bí mật | Lưu trong biến môi trường, không commit lên Git; toàn bộ web chạy HTTPS |
| S7 | Thanh toán | Số tiền tính ở server; chỉ lễ tân, quản lý được xác nhận đã thu tiền |
| S8 | Sinh trắc học | Có xác nhận đồng ý; chỉ lưu vector khuôn mặt; xóa khi hội viên yêu cầu |
| S9 | Truy vết | Thay đổi quan trọng (giá, hoàn tiền, phân quyền, xóa dữ liệu) ghi vào nhật ký hoạt động |
| S10 | Kiểm tra | Quét lỗ hổng thư viện bằng npm audit (frontend) và OWASP Dependency-Check (backend); quét bản deploy bằng OWASP ZAP trước khi bảo vệ |

## Tech stack

Nhóm đã chốt ReactJS + Java Spring Boot + PostgreSQL. Spring Boot được chọn vì phân tầng rõ (controller, service, repository) nên sơ đồ lớp trong báo cáo khớp với mã nguồn, xử lý giao dịch cơ sở dữ liệu tốt và kiểm thử bằng JUnit chuẩn mực.

| Tầng | Công nghệ | Lý do |
| --- | --- | --- |
| Frontend | React, TypeScript, Vite, Ant Design | Có sẵn bảng, form, lịch cho web quản lý; AI sinh code React tốt |
| Backend | Java, Spring Boot (Spring Web, Spring Security, Spring Data JPA) | Phân tầng rõ, khớp sơ đồ lớp; có sẵn phân quyền, validate, giao dịch |
| Cơ sở dữ liệu | PostgreSQL, Flyway | Flyway quản lý thay đổi schema bằng các file migration có đánh số, 8 người không sửa đè nhau |
| Nhận diện khuôn mặt (v5) | face-api.js chạy trên trình duyệt | Dùng mô hình có sẵn, không cần máy chủ AI riêng |
| Thời gian thực | WebSocket của Spring (STOMP) | Thông báo tức thời, số người đang tập |
| Email | Spring Mail qua SMTP | Nhắc hết hạn, báo được đưa vào lớp |
| Thanh toán | VietQR (mã chuyển khoản có sẵn số tiền) | Miễn phí; lễ tân xác nhận thủ công |
| Tài liệu API | Swagger (springdoc-openapi), sinh tự động | Không phải viết tay |
| Kiểm thử | JUnit 5, Mockito, Testcontainers, Postman | Kiểm thử đơn vị, kiểm thử với PostgreSQL thật, kiểm thử API |
| Quản lý mã nguồn | GitHub, GitHub Actions, GitHub Projects | Pull request, review, CI tự động, theo dõi task |
| Triển khai | Vercel (frontend), Render (backend chạy bằng Docker), Neon (cơ sở dữ liệu) | Có gói miễn phí, đủ cho demo |

## Câu hỏi cần chốt ở buổi kick-off

- [x] Giữ đủ 20 chức năng; F12 (POS và kho) và F13 (lương) xếp cuối v3. Giữa Sprint 3 xem lại tiến độ, trễ thì cắt F13 trước rồi đến F12 (02/11).
- [x] Giữ nguyên các con số trong phần quy tắc nghiệp vụ; phí chuyển nhượng mặc định 200.000 đ (02/11).
- [x] Dùng bộ 8 loại sơ đồ UML, không dùng DFD ([ADR 0009](../decisions/0009-bo-so-do-phan-tich-thiet-ke.md), 02/11).
- [x] Không khảo sát phòng gym thật; quy trình As-Is lấy từ tìm hiểu trên mạng và kinh nghiệm đi tập của thành viên (02/11).
- [x] Phân công Sprint 1: theo [sprint-1-tien-do.md](../plans/sprint-1-tien-do.md), cả nhóm nhận việc tại buổi kick-off 02/11.
- [x] Thời gian: tuần 9 bắt đầu 27/10/2026; bàn giao Sprint 1 ngày 10/11/2026; bàn giao cuối ở tuần 16.
- [x] Tech stack: ReactJS + Java Spring Boot + PostgreSQL, nhận diện khuôn mặt bằng face-api.js.
- [x] Các câu hỏi mở về dữ liệu: đã đóng, xem tài liệu "Thiết kế dữ liệu".

## Lịch sử thay đổi

| Ngày | Thay đổi |
| --- | --- |
| 27/10/2026 | Bản đầu: 20 chức năng, 5 version, 6 vai trò |
| 31/10/2026 | Hội viên được giữ đồng thời gói TIME và gói PT; chỉ gói TIME được đặt lớp nhóm; khách lẻ mua hàng không cần hồ sơ; đăng nhập bằng email |
| 01/11/2026 | Thời gian chuyển sang 4 sprint (27/10 – 21/12); gói PT không dùng để check-in; có cả gói TIME và SESSION thì dùng gói TIME; F7 tạo hóa đơn chờ, tự hủy sau 3 ngày; trùng số điện thoại khi tự đăng ký thì từ chối, lễ tân tạo tài khoản cho hồ sơ có sẵn; trạng thái gói UPCOMING cho gia hạn nối tiếp |
| 01/11/2026 | Sau review ERD: chỉ hóa đơn chờ từ cổng hội viên tự hủy; bảo lưu dời gói gia hạn nối tiếp; công thức hoa hồng PT; gói TIME tính theo ngày (1 tháng = 30 ngày) |
| 02/11/2026 | Kick-off: giữ đủ 20 chức năng (F12, F13 cuối v3); giữ các con số mặc định, phí chuyển nhượng 200.000 đ; bộ sơ đồ UML theo ADR 0009; không khảo sát phòng gym thật ([biên bản](../meetings/2026-11-02-kick-off.md)) |
| 03/11/2026 | Sau review đặc tả F4, F6 (trưởng nhóm chốt, xem [ADR 0005](../decisions/0005-erd-v2.md) mục 25–32): buổi của gói SESSION tính theo ngày, mỗi ngày trừ tối đa 1 buổi, gói về 0 buổi vẫn check-in lại được đến hết ngày; gói SESSION, PT giữ hạn dùng, ngày hết hạn = ngày bắt đầu + số ngày − 1, hết hạn mà còn buổi thì mất buổi, không hoàn tiền; hết buổi trước hạn thì gói nối tiếp được kéo lịch (SESSION về ngày mai, PT về hôm nay); không bảo lưu, chuyển nhượng gói hết buổi; số buổi PT đặt trước không vượt số buổi còn lại; F6 cảnh báo gói SESSION còn không quá 2 buổi\* |
| 04/11 – 12/11/2026 | Sau review đặc tả F8 (trưởng nhóm chốt ở [PR #79](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79); chi tiết trong đặc tả F8 mục 7): danh sách chờ theo [ADR 0010](../decisions/0010-danh-sach-cho-tu-dong-dua-vao-lop-ban-2.md), thay ADR 0003 (ân hạn tính từ lúc được đưa vào lớp, không từ lúc gửi email; trùng thời điểm vào chờ thì xếp theo thứ tự ghi nhận; xét danh sách chờ trước người đặt mới; dưới 30 phút\* đóng cả đặt mới và vào chờ; rời danh sách chờ không bị phạt). Cửa sổ đặt chỗ 7 ngày\* tính theo ngày lịch giờ Việt Nam. "Tháng" khi đếm vi phạm là tháng dương lịch giờ Việt Nam, chỉ đếm vi phạm sau lần khóa gần nhất; mỗi vi phạm chỉ tính cho một lần khóa, sửa điểm danh thì tính lại ngay (dưới ngưỡng thì gỡ khóa). Hạn xác nhận đổi giờ là mốc đến trước giữa 60 phút\* và giờ học mới; đổi giờ nhiều lần thì tính lại hạn từ lần đổi mới nhất. Sĩ số mới không nhỏ hơn số chỗ đang giữ. Điểm danh từ giờ học đến 23:59 cùng ngày, sau đó quản lý chi nhánh sửa. Chỉ quản lý chi nhánh và chủ chuỗi được đổi phòng, đổi giờ, hủy buổi. Chỗ đặt ghi người bấm đặt (hội viên hoặc lễ tân) |
