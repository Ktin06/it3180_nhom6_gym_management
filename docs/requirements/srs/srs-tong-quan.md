# SRS – Tổng quan hệ thống quản lý chuỗi phòng tập gym

- **Người biên soạn:** Nguyễn Anh Tú (`natus26`).
- **Người duyệt chéo:** Phạm Gia Bảo (`GamBug`), theo [phân công Sprint 1](../../plans/sprint-1-tien-do.md#24-cặp-duyệt-pull-request).
- **Nhiệm vụ:** [#13](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/13) · WBS 1.2.1.
- **Cập nhật:** 05/11/2026.
- **Trạng thái:** Bản biên soạn để duyệt; các cách kiểm chứng dưới đây là kế hoạch, chưa xác nhận hệ thống đã đạt yêu cầu.

## 1. Mục đích và căn cứ

Tài liệu đặc tả yêu cầu phần mềm (SRS) này trình bày bài toán, phạm vi, người sử dụng và các yêu cầu chung của web quản lý chuỗi phòng tập gym, thuộc bài tập lớn IT3180 của Nhóm 6, Đại học Bách khoa Hà Nội. Tài liệu dùng làm căn cứ chung cho thiết kế, lập trình, kiểm thử và báo cáo.

Nội dung được tổng hợp từ [bản chốt phạm vi](../ban-chot-pham-vi.md), [biên bản kick-off ngày 02/11](../../meetings/2026-11-02-kick-off.md) và các ADR được dẫn tại từng mục. Bản chốt phạm vi là nguồn chuẩn về nghiệp vụ; [ERD v2](../../design/erd/erd-v2.dbml) là nguồn chuẩn về dữ liệu. Khi nguồn chuẩn thay đổi, phần tương ứng trong SRS cần được cập nhật theo.

Phần này mô tả yêu cầu ở mức hệ thống. Luồng chính, ngoại lệ, API và tiêu chí chấp nhận của từng chức năng nằm trong các đặc tả use case cùng thư mục, theo [mẫu đặc tả](mau-dac-ta-use-case.md). Nội dung user story và tiêu chí chấp nhận được duy trì tại các issue tương ứng trên GitHub.

| Thuật ngữ | Ý nghĩa trong tài liệu |
| --- | --- |
| F1–F20 | Mã chức năng theo bản chốt phạm vi |
| NF1–NF6 | Mã yêu cầu phi chức năng |
| S1–S10 | Mã yêu cầu bảo mật và kiểm tra an toàn |
| PT | Huấn luyện viên; gói PT dùng cho buổi tập 1–1 |
| As-Is | Quy trình hiện tại dùng làm bối cảnh phân tích |
| Version | Bản tăng dần về phạm vi chức năng, từ v1 đến v5 |
| Sprint | Đợt làm việc theo kế hoạch; một version có thể cần nhiều sprint |

## 2. Bài toán và quy trình hiện tại (As-Is)

### 2.1. Cách xác lập bối cảnh

Theo quyết định kick-off, nhóm **không khảo sát phòng gym thật**. Bối cảnh trong bản chốt phạm vi là cơ sở quản lý hội viên, gói tập, lịch PT và doanh thu bằng sổ sách, Excel và tin nhắn. Quy trình ở mục 2.2 là mô hình phân tích từ bối cảnh đó, có đối chiếu tài liệu công khai; không đại diện cho một phòng tập cụ thể hoặc cho số liệu khảo sát thực tế.

Các tài liệu Gymdesk được dùng để đối chiếu những thông tin cần quản lý:

- [Member & Visitor Accounts](https://docs.gymdesk.com/en/help/docs/member-accounts): hồ sơ có thể tập hợp gói tập, lịch sử tham gia và thông tin thu tiền của từng người.
- [Attendance Tracking](https://docs.gymdesk.com/en/help/docs/attendance-tracking): việc ghi nhận lượt đến tập cần gắn với người tập và có lịch sử tra cứu.
- [Reports & Data Exports in Gymdesk](https://docs.gymdesk.com/en/help/docs/reports-data-exports-in-gymdesk): báo cáo có thể khai thác dữ liệu hội viên, lượt tập và thu tiền.

Các nguồn trên được truy cập ngày 05/11/2026. Chúng mô tả một sản phẩm tham khảo, không chứng minh phòng tập trong đề bài thực sự dùng sổ sách hay áp dụng quy tắc của sản phẩm đó. Quy tắc của hệ thống Nhóm 6 vẫn theo bản chốt phạm vi.

### 2.2. Quy trình mô hình hóa

| Nghiệp vụ | Cách thực hiện trong bối cảnh giả định | Thông tin cần theo dõi | Vấn đề cần giải quyết |
| --- | --- | --- | --- |
| Tiếp nhận hội viên | Lễ tân nhận thông tin, tìm hồ sơ cũ rồi ghi mới hoặc cập nhật trong sổ/bảng tính | Thông tin liên hệ, hồ sơ hội viên | Thông tin phân tán, khó tìm và dễ tạo hồ sơ trùng |
| Bán và gia hạn gói | Lễ tân đối chiếu gói đang dùng, ghi gói bán, hạn dùng hoặc số buổi, ghi nhận tiền tại quầy | Gói của từng hội viên, thời hạn, số buổi, khoản thu | Dễ tính sai hạn gói, nhầm số buổi hoặc lệch dữ liệu gói và thu tiền |
| Ghi lượt đến tập | Lễ tân tìm hồ sơ, kiểm tra quyền sử dụng gói và ghi lượt tập | Hội viên, thời điểm đến, gói sử dụng | Tra cứu chậm, khó theo dõi lượt tập và số buổi đã dùng |
| Sắp lịch lớp và PT | Hội viên liên hệ nhân viên hoặc PT qua tin nhắn; người phụ trách đối chiếu lịch và ghi nhận | Người tham gia, PT, phòng và thời gian | Khó phát hiện trùng lịch và quản lý số người trong lớp |
| Tổng hợp doanh thu | Nhân viên tập hợp các khoản thu và lượt tập để báo cáo quản lý | Khoản thu, hội viên, lượt tập theo kỳ | Mất công tổng hợp, khó đối soát và theo dõi nhiều chi nhánh |

Không đưa vào đây kết quả phỏng vấn, tỷ lệ sai sót hay thời gian thao tác thực tế vì nhóm chưa có dữ liệu khảo sát để kết luận các con số đó.

### 2.3. Mục tiêu hệ thống

Hệ thống tập trung hóa hồ sơ hội viên, gói tập, lịch tập và các khoản thu; giúp nhân viên tra cứu, kiểm tra điều kiện và ghi nhận nghiệp vụ trên cùng nguồn dữ liệu. Hội viên có thể tự xem thông tin cá nhân, đặt lớp, đặt lịch PT và gửi yêu cầu mua/gia hạn gói; việc check-in và xác nhận thu tiền được thực hiện tại quầy theo phạm vi đã chốt.

## 3. Phạm vi và ranh giới hệ thống

### 3.1. Các bản tăng dần

Theo [ADR 0002](../../decisions/0002-phat-trien-tang-dan-5-version.md), hệ thống có 20 chức năng chia thành năm version. Chi tiết từng mã F giữ tại [danh sách chức năng chuẩn](../ban-chot-pham-vi.md#danh-sách-chức-năng).

| Version | Phạm vi chức năng | Mức cam kết |
| --- | --- | --- |
| v1 | F1–F6: tài khoản và phân quyền, hồ sơ hội viên, gói tập/gia hạn, check-in, thanh toán tại quầy, dashboard cơ bản | Bắt buộc |
| v2 | F7–F10: cổng hội viên, lớp học nhóm và danh sách chờ, huấn luyện viên, thông báo | Bắt buộc |
| v3 | F11–F17: thiết bị, POS/kho, nhân sự/lương, khuyến mãi, bảo lưu/chuyển nhượng, báo cáo nâng cao, nhật ký hoạt động | Mục tiêu |
| v4 | F18–F19: đa chi nhánh và gamification theo quy tắc | Mục tiêu; cắt sau v5 nếu trễ |
| v5 | F20: check-in bằng nhận diện khuôn mặt | Cắt đầu tiên nếu trễ |

Từ v1 đến v3, vận hành với một chi nhánh mặc định; chức năng quản lý nhiều chi nhánh mở ở v4. Theo kick-off, F12 và F13 xếp cuối v3; giữa Sprint 3 đánh giá tiến độ, nếu trễ thì cắt F13 trước rồi F12. Phạm vi version là mục tiêu chức năng, không khẳng định toàn bộ chức năng đó đã hoàn thành trong Sprint 1.

### 3.2. Ranh giới sử dụng và dịch vụ liên quan

- Người dùng thao tác bằng web responsive trên máy tính và điện thoại, giao diện tiếng Việt. Máy tại quầy phục vụ lễ tân; webcam phục vụ F20 khi triển khai v5.
- Thanh toán chỉ tại quầy bằng tiền mặt hoặc chuyển khoản VietQR. Lễ tân hoặc quản lý xác nhận đã nhận tiền; hệ thống không tự đối soát ngân hàng. Yêu cầu mua/gia hạn gửi từ F7 tạo hóa đơn chờ, gói chỉ kích hoạt sau khi hóa đơn đã thanh toán. Căn cứ: [ADR 0004](../../decisions/0004-thanh-toan-chi-tai-quay.md).
- Dữ liệu nghiệp vụ lưu tập trung trong PostgreSQL. Các thời điểm lưu UTC; nghiệp vụ theo ngày Việt Nam dùng múi giờ `Asia/Ho_Chi_Minh`, theo [quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md).
- Email qua SMTP và thông báo trong web thuộc F10. Trình duyệt gọi API cùng domain qua proxy; WebSocket kết nối theo [ADR 0008](../../decisions/0008-goi-api-cung-domain.md). Đây là ràng buộc thiết kế đã chọn, không phải bằng chứng tích hợp đã hoạt động.
- Nền tảng triển khai theo [ADR 0001](../../decisions/0001-cong-nghe.md): Vercel cho frontend, Render cho backend, Neon cho cơ sở dữ liệu. Chi tiết công nghệ và phiên bản giữ tại ADR này.

### 3.3. Ngoài phạm vi

Theo [bản chốt phạm vi](../ban-chot-pham-vi.md#phạm-vi-không-làm), dự án không triển khai:

- Ứng dụng iOS/Android riêng; cổng từ, máy chấm công vân tay hoặc IoT trên thiết bị tập.
- Thanh toán qua cổng online, ví điện tử hay đối soát tự động qua API ngân hàng.
- AI ngoài F20: gợi ý lớp/gói, cảnh báo bỏ tập, chatbot; tự huấn luyện mô hình nhận diện.
- Tự check-in khuôn mặt không có lễ tân giám sát hoặc phát hiện giả mạo chuyên sâu.
- Kế toán, thuế, hóa đơn điện tử theo quy định; CRM và marketing tự động.
- Đăng nhập bằng số điện thoại, mạng xã hội hoặc giao diện đa ngôn ngữ.

## 4. Vai trò và nguyên tắc truy cập

Hệ thống có **6 vai trò sử dụng**, trong đó **5 vai trò có tài khoản**. Mỗi tài khoản có đúng một vai trò; khách là người dùng chưa đăng nhập, không phải một giá trị vai trò lưu trong `users`. Nhân viên gắn với một chi nhánh; chủ chuỗi có phạm vi toàn chuỗi. Căn cứ: [vai trò chuẩn](../ban-chot-pham-vi.md#vai-trò-người-dùng), [ERD v2](../../design/erd/erd-v2.dbml) và quyết định kick-off.

| Vai trò | Mã vai trò tài khoản | Nhu cầu và quyền chính theo phạm vi chức năng |
| --- | --- | --- |
| Chủ chuỗi | `SUPER_ADMIN` | Quản lý chi nhánh, tài khoản quản lý, cấu hình; xem báo cáo toàn chuỗi và nhật ký hoạt động |
| Quản lý chi nhánh | `BRANCH_MANAGER` | Quản lý gói, lớp, nhân viên, ca làm, thiết bị, kho, khuyến mãi; duyệt bảo lưu, hoàn tiền; xem báo cáo chi nhánh mình |
| Lễ tân | `RECEPTIONIST` | Quản lý hồ sơ hội viên, tạo tài khoản cho hồ sơ có sẵn, check-in, bán gói/hàng, thu tiền, in hóa đơn; đăng ký khuôn mặt ở v5 |
| Huấn luyện viên | `TRAINER` | Khai báo lịch rảnh, xem lịch dạy, điểm danh, xác nhận buổi PT, soạn giáo án, ghi tiến độ và chỉ số cơ thể học viên mình dạy |
| Hội viên | `MEMBER` | Xem gói, lịch sử tập, hóa đơn và tiến độ của mình; gửi yêu cầu mua/gia hạn, đặt lớp và PT; nhận thông báo, huy hiệu |
| Khách | Không có tài khoản đăng nhập | Xem bảng giá, lịch lớp công khai và đăng ký tài khoản |

Các quyền chỉ áp dụng khi chức năng tương ứng được triển khai. Bảng vai trò không cấp quyền quản trị mọi API theo suy đoán; quyền cụ thể cần khớp đặc tả use case của chức năng và S3. Huấn luyện viên chỉ xem hồ sơ học viên có buổi PT hoặc lớp do mình dạy, không được tìm kiếm toàn bộ hội viên. Hồ sơ hội viên có thể tồn tại trước tài khoản; người chưa có tài khoản không dùng cổng hội viên hoặc nhận email.

## 5. Yêu cầu phi chức năng NF1–NF6

Các ngưỡng dưới đây giữ nguyên từ mục [Yêu cầu phi chức năng và bảo mật](../ban-chot-pham-vi.md#yêu-cầu-phi-chức-năng-và-bảo-mật). Cột kiểm chứng nêu cách thu thập bằng chứng khi nghiệm thu, không thay thế kết quả đo thực tế.

| Mã | Yêu cầu | Cách kiểm chứng dự kiến |
| --- | --- | --- |
| NF1 | Tra cứu hội viên và tải trang danh sách dưới **2 giây** với **10.000 hội viên**; check-in thủ công F4 phản hồi dưới **2 giây** | Chuẩn bị dữ liệu 10.000 hội viên; đo từng thao tác tra cứu/tải danh sách và check-in. Lưu cấu hình môi trường, dữ liệu đầu vào, mốc bắt đầu/kết thúc và kết quả đo |
| NF2 | Nhận diện khuôn mặt và trả kết quả check-in F20 dưới **3 giây** | Khi triển khai F20, đo luồng nhận diện đến phản hồi check-in; ghi thiết bị, trình duyệt, điều kiện thử và kết quả |
| NF3 | Phục vụ **100 người dùng đồng thời không lỗi** | Chạy kịch bản tải với 100 người dùng đồng thời; lưu yêu cầu đã gửi, phản hồi và lỗi. Các trường hợp cố ý kiểm tra từ chối nghiệp vụ phải được phân biệt với lỗi hệ thống |
| NF4 | Dùng tốt trên màn hình từ **360px**, trên Chrome, Edge và Firefox bản mới | Kiểm tra thao tác, bố cục, biểu mẫu và điều hướng ở chiều rộng 360px cùng các màn hình lớn hơn; ghi phiên bản trình duyệt thực tế |
| NF5 | Cơ sở dữ liệu được **sao lưu tự động hằng ngày** | Kiểm tra cấu hình lịch sao lưu và lịch sử bản sao được tạo; lưu bằng chứng ngày/giờ. Có cơ sở dữ liệu trên môi trường triển khai chưa đủ để kết luận đã đáp ứng |
| NF6 | Đặt chỗ lớp và đặt lịch PT chạy trong giao dịch cơ sở dữ liệu có khóa bản ghi; không vượt sĩ số, không trùng lịch khi nhiều người đặt cùng lúc | Kiểm thử tích hợp trên PostgreSQL với các yêu cầu cạnh tranh vào chỗ cuối, cùng khung giờ PT hoặc lịch trùng của hội viên; kiểm tra dữ liệu sau giao dịch theo ràng buộc và thứ tự khóa trong ERD |

NF2 gắn với F20 ở v5; nếu cắt v5 theo kế hoạch, ghi rõ NF2 chưa được nghiệm thu vì chức năng tương ứng chưa triển khai. Không thay ngưỡng trong SRS để phù hợp kết quả đo. Môi trường, kịch bản tải và cách đo chi tiết được ghi trong kế hoạch/báo cáo kiểm thử của từng đợt.

## 6. Yêu cầu bảo mật S1–S10

Nguồn chuẩn của mã và nội dung yêu cầu là cùng mục với NF1–NF6. Các ràng buộc bổ sung về cookie lấy từ ADR 0008; quy tắc bảo vệ dữ liệu lấy từ bản chốt phạm vi và kick-off.

| Mã | Yêu cầu | Cách kiểm chứng dự kiến |
| --- | --- | --- |
| S1 | Mật khẩu băm bằng **bcrypt**, tối thiểu **8 ký tự gồm chữ và số** | Kiểm tra dữ liệu lưu là mã băm; kiểm thử mật khẩu đạt chuẩn và các trường hợp thiếu độ dài/chữ/số |
| S2 | Access token JWT hết hạn sau **15 phút**; refresh token trong cookie **HttpOnly** | Kiểm tra thời hạn và việc từ chối JWT hết hạn; xem cookie thực tế và luồng làm mới/đăng xuất. Theo ADR 0008, cookie khi triển khai có `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth` |
| S3 | Backend kiểm tra vai trò và quyền sở hữu trên mọi API; hội viên không xem dữ liệu người khác | Thử gọi API trực tiếp bằng các vai trò, tài khoản hội viên khác, nhân viên chi nhánh khác và PT không dạy học viên đó; đối chiếu phạm vi được phép. Ẩn nút trên giao diện không đủ để đáp ứng |
| S4 | Khóa đăng nhập **15 phút** sau **5 lần sai liên tiếp** | Kiểm thử chuỗi đăng nhập sai, lần đạt ngưỡng, lần thử trong thời gian khóa và sau thời gian khóa; ghi kết quả theo đặc tả F1 |
| S5 | Validate mọi request ở backend bằng Bean Validation; dùng JPA với tham số, không nối chuỗi SQL | Kiểm thử dữ liệu thiếu/sai định dạng và rà soát truy vấn; không chỉ dựa vào validation trên trình duyệt |
| S6 | Bí mật lưu trong biến môi trường, không commit lên Git; toàn bộ web chạy **HTTPS** | Rà soát file được theo dõi và lịch sử thay đổi để phát hiện bí mật; kiểm tra cấu hình môi trường và HTTPS trên bản triển khai; bằng chứng không được chứa giá trị bí mật |
| S7 | Số tiền tính ở server; chỉ **lễ tân, quản lý** được xác nhận thu tiền | Thử sửa giá/số tiền gửi từ trình duyệt và gọi xác nhận bằng vai trò không được phép; kiểm tra số tiền lưu theo tính toán server và người xác nhận hợp lệ |
| S8 | Đăng ký sinh trắc học cần xác nhận đồng ý; chỉ lưu vector khuôn mặt, xóa khi hội viên yêu cầu | Khi triển khai F20, kiểm tra từ chối nếu chưa đồng ý, kiểm tra nơi lưu dữ liệu không giữ ảnh gốc và xác minh vector được xóa theo yêu cầu |
| S9 | Thay đổi quan trọng về giá, hoàn tiền, phân quyền và xóa dữ liệu phải ghi nhật ký hoạt động | Thực hiện từng loại thay đổi và đối chiếu bản ghi nhật ký theo F17 và ERD; xác định được người thao tác, thời điểm, dữ liệu trước/sau |
| S10 | Quét thư viện bằng **npm audit** ở frontend và **OWASP Dependency-Check** ở backend; quét bản triển khai bằng **OWASP ZAP trước khi bảo vệ** | Lưu báo cáo từng công cụ, phiên bản mã nguồn hoặc URL được quét, thời điểm và kết quả xử lý phát hiện; CI xanh không tự thay thế các báo cáo này |

Theo kế hoạch Sprint 1, chống dò mật khẩu S4 được thực hiện cùng phần F1 ở Sprint 2; S8 gắn với F20 ở v5. F17 nằm ở v3 theo lộ trình, nhưng các nghiệp vụ đã yêu cầu ghi nhật ký theo S9, như hoàn tiền F5, phải có lưu vết khi được triển khai, theo [ADR 0004](../../decisions/0004-thanh-toan-chi-tai-quay.md). Việc ghi yêu cầu ở đây không xác nhận các chức năng đó đã có trên `main`. Những lần nghiệm thu cần nêu rõ phạm vi version/sprint và bằng chứng tương ứng.

## 7. Liên kết thiết kế và quản lý thay đổi

- Danh mục chức năng và quy tắc nghiệp vụ chi tiết: [bản chốt phạm vi](../ban-chot-pham-vi.md).
- Đặc tả từng use case: [thư mục SRS](./), theo mẫu dùng chung; mã use case giữ nguyên theo tài liệu của người phụ trách chức năng.
- Thiết kế dữ liệu và ràng buộc: [ERD v2](../../design/erd/erd-v2.dbml), [giải thích dữ liệu](../../design/erd/thiet-ke-du-lieu.md). SRS tổng quan không sao chép từ điển cột.
- Sơ đồ và wireframe: [use case](../../design/use-case/), [hoạt động](../../design/activity/), [wireframe](../../design/wireframe/); bộ sơ đồ UML theo [ADR 0009](../../decisions/0009-bo-so-do-phan-tich-thiet-ke.md).
- Bảng truy vết chức năng–use case–dữ liệu và ERD/từ điển phục vụ báo cáo là hai sản phẩm riêng tại [#14](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/14) và [#15](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/15).

Thay đổi nghiệp vụ cần được trưởng nhóm Hoàng Kim Tiến chốt và cập nhật vào nguồn chuẩn trước khi đồng bộ SRS. Kết quả đo, báo cáo quét và biên bản nghiệm thu được lưu riêng theo đợt kiểm thử. Tài liệu được duy trì bằng Markdown trong Sprint 1; chuyển sang LaTeX khi tổng hợp báo cáo theo phân công.

## 8. Lịch sử thay đổi

| Ngày | Nội dung |
| --- | --- |
| 05/11/2026 | Biên soạn tổng quan SRS cho #13: mô hình As-Is và nguồn tham khảo, phạm vi năm version, sáu vai trò, NF1–NF6, S1–S10 và cách kiểm chứng dự kiến |
