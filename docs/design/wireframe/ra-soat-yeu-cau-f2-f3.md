# Rà soát yêu cầu trước khi vẽ wireframe F2, F3

- Ngày rà soát: 03/11/2026.
- Người phụ trách wireframe và giao diện: Nguyễn Anh Tú.
- Mốc đối chiếu local: `main` tại `fcf6d8c`.
- Trạng thái: đã rà soát nguồn; các đề xuất bố cục và câu hỏi bên dưới chưa phải quyết định của nhóm.

> Cập nhật 04/11/2026: nhánh đã cập nhật main `446ceb3`. Nội dung ngày rà soát 03/11 bên dưới là ảnh chụp trước PR #82/#84; quy tắc gia hạn SESSION/PT và yêu cầu kiểm tra bằng Draw.io đã có bổ sung. Xem [điểm cần hoàn tất trước khi duyệt](huong-dan-wireframe-f2-f3.md) và ưu tiên bản chốt phạm vi hiện hành khi triển khai.
- Tài liệu này chuẩn bị cho wireframe, không thay thế đặc tả, user story hay hợp đồng API.

> Phạm vi đang thực hiện là **wireframe #18**: đủ danh sách/tìm kiếm hội viên, form hồ sơ, danh mục gói, bán và gia hạn. Các câu hỏi về phạm vi code, DTO và tích hợp #27 bên dưới là việc cần chuẩn bị cho giai đoạn triển khai, **không phải điều kiện chặn việc vẽ #18**. Bố cục và thao tác được đề xuất trong wireframe để nhóm review; không tự coi đó là hợp đồng API đã chốt.

Các bản phác thảo đã tạo và hướng dẫn mở xem nằm trong [hướng dẫn wireframe F2/F3](huong-dan-wireframe-f2-f3.md).

## 1. Nguồn đối chiếu

| Nguồn | Dùng để xác định |
| --- | --- |
| [Issue #18](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/18) | Wireframe danh sách/tìm kiếm, form hội viên chưa có ảnh, danh mục gói, bán và gia hạn; hạn 04/11 |
| [Issue #27](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/27) | Code danh sách/tìm kiếm/phân trang, form thêm/sửa hội viên, danh mục gói; mock theo API nháp, nối API thật, Vitest, responsive từ 360px; hạn 08/11 |
| [Bản chốt phạm vi](../../requirements/ban-chot-pham-vi.md) | Nguồn chuẩn về nghiệp vụ, vai trò và phạm vi |
| [ERD](../erd/erd-v2.dbml), [thiết kế dữ liệu](../erd/thiet-ke-du-lieu.md) | Các bảng `members`, `packages`, `memberships` và ràng buộc |
| [Đặc tả F2](../../requirements/srs/dac-ta-use-case-f2-ho-so-hoi-vien.md) | Use case, API nháp và tiêu chí chấp nhận F2 |
| [Biên bản kick-off](../../meetings/2026-11-02-kick-off.md) | Quyết định bỏ ảnh ở Sprint 1, giới hạn quyền TRAINER và phạm vi chi nhánh |
| [PR #35](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/35) | Bản sửa đặc tả F1/F2; lúc kiểm tra còn mở, chưa merge; head `3c3aebd` |
| [Issue #6](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/6), [#28](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/28), [#29](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/29) | Đặc tả F3/F5 và API F2/F3 do Thái Hùng Lân phụ trách |
| [Kế hoạch Sprint 1](../../plans/sprint-1-tien-do.md) | Phân công, phụ thuộc và mốc bàn giao API nháp |
| [Quy ước tài liệu](../../README.md), [quy tắc mã nguồn](../../guides/quy-tac-ma-nguon.md) | Wireframe draw.io dạng `.drawio.svg`, tổ chức mã, kiểm thử |

Trạng thái issue/PR là ảnh chụp tại ngày rà soát, cần kiểm tra lại trước khi triển khai. Không lấy trạng thái đóng issue làm bằng chứng PR đã merge. Khi mâu thuẫn, ưu tiên bản chốt phạm vi về nghiệp vụ và ERD về dữ liệu; các quyết định kick-off cần được phản ánh trong đặc tả.

## 2. Phạm vi và phân công

| Phần việc | Wireframe #18 | Code giao diện #27 |
| --- | --- | --- |
| Danh sách, tìm kiếm hội viên | Có | Có, kèm phân trang |
| Form thêm/sửa hồ sơ | Có, không có ảnh | Có, không có ảnh |
| Danh mục gói TIME, SESSION, PT | Có | Có |
| Thêm/sửa/ngừng bán gói | Đề xuất thể hiện để khớp API #29 | Cần xác nhận phạm vi vì #27 chỉ ghi “màn hình danh mục gói” |
| Bán và gia hạn gói | Có, ghi rõ luồng phục vụ Sprint 2 | Không thuộc triển khai Sprint 1; #29 ghi đăng ký/gia hạn để Sprint 2 |
| Tạo tài khoản cho hồ sơ, chỉ số cơ thể | Có trong đặc tả F2 rộng hơn, không tự bổ sung vào #18 | Chưa được #27 giao rõ; cần chốt trước khi làm |
| Xem chi tiết riêng, đổi trạng thái hội viên | Đề xuất theo UC-F2.3/UC-F2.4, cần chốt cách thể hiện | Xác nhận trong phạm vi #27 trước khi triển khai |

- Anh Tú: wireframe #18, frontend #27; không nhận thay phần backend.
- Lân: đặc tả F3/F5, dữ liệu và API hội viên/gói; bàn giao API nháp F2/F3 theo mốc 05/11, API theo hạn 07/11.
- Đức Việt: đặc tả F2, sửa theo kick-off trong PR #35; đăng nhập/phân quyền backend F1.
- Gia Bảo: user story, quy ước frontend chung, tích hợp đăng nhập frontend; người review chính của Anh Tú.

## 3. Quy tắc đã có nguồn, cần thể hiện đúng

### Hội viên

- Tạo hồ sơ tại quầy có thể chưa có tài khoản đăng nhập. Không bắt nhập email/mật khẩu chỉ để tạo hồ sơ.
- Họ tên và số điện thoại là dữ liệu bắt buộc theo ERD. Giới tính và ngày sinh có thể rỗng; danh sách giá trị giới tính và cách biểu diễn trong JSON cần DTO xác nhận.
- Backend sinh mã `HV` + 6 chữ số; frontend không tự cấp mã hay để người dùng sửa mã.
- Backend chuẩn hóa số điện thoại về 10 chữ số bắt đầu bằng `0`, kiểm tra duy nhất. Đặc tả cho phép ví dụ đầu vào `+84 912 345 678`; không chặn đầu vào này bằng quy tắc frontend trái hợp đồng API.
- Email lấy từ `users.email`; hồ sơ chưa có tài khoản có thể không có email.
- Sprint 1 bỏ trường tải ảnh và `avatarUrl` khỏi form. Đây đã là quyết định kick-off và yêu cầu #18/#27, không cần chờ PR #35 mới hiểu phạm vi này.
- Không có thao tác xóa cứng. Nếu bổ sung đổi trạng thái, backend chịu trách nhiệm đồng bộ khóa tài khoản khi hội viên ngừng hoạt động.
- Lễ tân/quản lý chỉ xem hồ sơ thuộc chi nhánh của mình; chủ chuỗi xem toàn chuỗi. TRAINER không dùng tìm kiếm hội viên chung, chỉ xem học viên mình dạy; MEMBER chỉ xem hồ sơ của chính mình. Quyền chi tiết cần đối chiếu API khi tích hợp.
- Bản đặc tả hiện trên main còn cho TRAINER gọi danh sách; biên bản kick-off đã sửa quyết định này, PR #35 đang cập nhật lại tài liệu. Không xây giao diện theo quyền cũ.

### Gói tập

- Phân biệt `packages` (danh mục sản phẩm) với `memberships` (gói cụ thể hội viên đã mua).
- Theo #29, chỉ quản lý chi nhánh được quản lý gói. Không suy ra lễ tân được sửa gói chỉ vì thấy menu. Quyền đọc danh mục của từng vai trò cần chốt trong API.
- Gói TIME, SESSION và PT đều có thời hạn `duration_days > 0`; một tháng TIME tương đương 30 ngày.
- `session_count` bắt buộc với SESSION/PT, rỗng với TIME. Giá là số nguyên VND, không âm theo ERD.
- Ngừng bán là `packages.status = INACTIVE`, không xóa cứng. Gói toàn chuỗi để v4 theo #6, không đưa lựa chọn này vào form Sprint 1.
- `packages.status = ACTIVE` nghĩa là sản phẩm đang bán, khác việc kích hoạt `memberships` sau thanh toán.
- Khi vẽ bán/gia hạn: giá và tổng tiền lấy từ server; thanh toán tại quầy; gói mua chỉ được kích hoạt sau khi hóa đơn PAID. Không tự dựng thanh toán online.
- Gia hạn khi gói cũ còn hạn: gói mới bắt đầu sau ngày hết hạn của gói cũ, có trạng thái UPCOMING trong thời gian chờ. Cách thể hiện các bước chờ thanh toán/UPCOMING cần đối chiếu đặc tả F3/F5 trước khi hoàn tất luồng.

## 4. Danh sách màn hình đề xuất cho bước vẽ

Các cột, cách mở form và bố cục dưới đây là đề xuất để review, không phải hợp đồng API mới.

| Màn hình | Nội dung dự kiến | Trạng thái cần thể hiện |
| --- | --- | --- |
| Danh sách hội viên | Mã, họ tên, số điện thoại, trạng thái; ô tìm theo mã/SĐT/tiền tố tên; lọc trạng thái; phân trang; nút thêm/sửa theo quyền | Đang tải, chưa có dữ liệu, tìm không thấy, lỗi tải, không đủ quyền |
| Thêm hội viên | Họ tên, số điện thoại, giới tính/ngày sinh tùy chọn; Lưu/Hủy | Thiếu dữ liệu, sai định dạng, số điện thoại trùng, đang gửi, thành công, lỗi server |
| Sửa hội viên | Nạp thông tin hiện có; sửa trường cho phép; mã chỉ đọc; trạng thái nếu nhóm xác nhận | Tải hồ sơ, 404, 403, dữ liệu lỗi, SĐT trùng, đang lưu; giữ dữ liệu nhập khi lỗi |
| Danh mục gói | Mã, tên, loại, hạn ngày, số buổi khi áp dụng, giá VND, trạng thái bán | Đang tải, rỗng, lỗi; thao tác quản lý chỉ cho quản lý chi nhánh |
| Form gói/ngừng bán (chờ xác nhận phạm vi) | Mã, tên, loại, hạn ngày, số buổi theo loại, giá, mô tả; xác nhận ngừng bán | Ràng buộc đầu vào, lỗi API; cách cấp/sửa mã gói chờ DTO |
| Bán gói (wireframe Sprint 2) | Chọn hội viên và gói; xem thông tin/giá do server cung cấp; chuyển sang luồng hóa đơn F5 | Chưa chọn hội viên/gói, điều kiện không hợp lệ, chờ thanh toán; điểm nối F5 |
| Gia hạn (wireframe Sprint 2) | Hội viên, gói hiện tại, ngày hết hạn, gói nối tiếp; ngày dự kiến/trạng thái do hệ thống xác định | Gói còn hạn/hết hạn, lỗi điều kiện, chờ thanh toán/UPCOMING theo đặc tả được duyệt |

Mỗi màn hình cần phương án desktop và chiều rộng 360px. Đề xuất mobile chuyển danh sách sang thẻ hoặc bố trí cuộn hợp lý, form một cột; chọn cách cuối cùng theo quy ước giao diện của Gia Bảo. Không thêm biểu đồ, thông tin sức khỏe hoặc chức năng ngoài phạm vi vào màn hình mặc định.

## 5. Hợp đồng dữ liệu hiện có và phần còn thiếu

F2 đã có bảng API nháp trong đặc tả:

| API nháp | Dùng cho | Cần xác nhận thêm |
| --- | --- | --- |
| `GET /api/v1/members` | Danh sách, tìm kiếm, lọc, phân trang | DTO response đầy đủ, giới hạn size, thứ tự mặc định, hành vi keyword rỗng và tìm SĐT |
| `GET /api/v1/members/{id}` | Nạp hồ sơ để xem/sửa | Trường nullable, phạm vi quyền, phản hồi không tồn tại |
| `POST /api/v1/members` | Thêm hội viên | DTO và validation chính xác, giá trị gender, định dạng dob, lỗi theo trường |
| `PUT /api/v1/members/{id}` | Sửa hội viên | Trường được sửa, quy tắc giá trị rỗng, đổi trạng thái trong Sprint 1 |

[`PageResponse`](../../../backend/src/main/java/vn/edu/hust/nhom6/gym/common/dto/PageResponse.java) hiện gồm `content`, `page`, `size`, `totalElements`, `totalPages`; `page` bắt đầu từ 0. Giao diện hiển thị trang 1 phải gửi trang 0. Có thể dùng kiểu phân trang này làm cơ sở, nhưng không suy ra tên toàn bộ trường MemberResponse từ tên cột ERD.

F2 dùng lỗi `ProblemDetail` theo đặc tả. Cần mẫu JSON thực tế của lỗi 400/409 để ánh xạ thông báo vào form; xử lý 401/403 cùng cơ chế đăng nhập chung.

Chưa tìm thấy đặc tả F3/hợp đồng API F3 đầy đủ trên main tại mốc rà soát. Không tự chốt endpoint, phương thức HTTP, tên DTO, quyền đọc hay quy tắc tạo mã gói từ tên bảng. Các trường ERD ở mục 3 chỉ là cơ sở thiết kế giao diện.

## 6. Các câu hỏi cho giai đoạn triển khai #27 và trao đổi khi review

Chưa gửi tin nhắn hay thay đổi issue của nhóm; danh sách này để Anh Tú trao đổi. Các mốc “trước chốt” trong bảng là điểm rà lại khi duyệt hoặc triển khai, không yêu cầu dừng phác thảo #18. Ưu tiên hoàn thành bốn nhóm màn hình đã được #18 giao.

| Mã | Cần chốt | Người phối hợp | Trước bước nào |
| --- | --- | --- | --- |
| Q1 | Liên kết user story/tiêu chí chấp nhận cho #18/#27 (hiện đều ghi “—”) | Gia Bảo, trưởng nhóm | Trước sinh mã chức năng theo CONTRIBUTING |
| Q2 | #27 bao gồm form thêm/sửa/ngừng bán gói hay chỉ danh sách? Quyền đọc gói của lễ tân/chủ chuỗi? | Lân, trưởng nhóm | Trước chốt wireframe F3 và code |
| Q3 | DTO F2/F3, ví dụ response và lỗi, enum, nullable, validation, quy tắc mã gói | Lân | Trước mock và tích hợp API |
| Q4 | Xem chi tiết riêng và đổi trạng thái hội viên có nằm trong #27? Tạo tài khoản/chỉ số cơ thể làm sprint nào? | Đức Việt, Lân, trưởng nhóm | Trước thêm các thao tác này |
| Q5 | Quy ước bảng/form, modal hay trang riêng, bố cục mobile; tích hợp router/menu | Gia Bảo | Trước chốt bố cục và sửa file chung |
| Q6 | Điểm nối bán/gia hạn với hóa đơn F5, quyền thao tác và trạng thái theo từng bước | Lân | Trước duyệt hai wireframe bán/gia hạn |

## 7. Tiêu chí rà lại wireframe và giao diện

Các tiêu chí sau là checklist đề xuất cho review, không thay thế user story chính thức:

- Truy vết được từng màn hình về #18/#27 hoặc phần có ghi rõ chờ xác nhận; đủ bốn nhóm màn hình của #18.
- Danh sách tìm theo mã/SĐT/tên, phân trang và trường hợp không có kết quả đều có cách thể hiện. Đổi điều kiện tìm kiếm nên về trang đầu (đề xuất UX).
- Form thêm/sửa không có ảnh; không buộc email; mã hội viên do backend cấp.
- Lỗi trùng SĐT hiển thị rõ và giữ dữ liệu đã nhập; khi đang lưu ngăn gửi lặp (đề xuất UX).
- Quyền TRAINER không dựa vào đặc tả cũ; các thao tác quản lý gói chỉ dành cho quản lý chi nhánh. Backend vẫn là nơi thực thi phân quyền.
- Danh mục phân biệt TIME/SESSION/PT và trạng thái đang bán với trạng thái gói đã mua. Không có xóa cứng hay gói toàn chuỗi ở Sprint 1.
- Wireframe bán/gia hạn ghi rõ phục vụ Sprint 2 và điểm cần xác nhận với F5.
- Có trạng thái tải/rỗng/lỗi, nhãn tiếng Việt và phương án 360px.
- File hình sau này phải là `.drawio.svg` mở sửa được bằng draw.io; tài liệu này chưa phải sản phẩm hoàn tất #18.
- Khi chuyển sang #27: mock đúng API đã thống nhất, nối API thật, kiểm thử hành vi chính bằng Vitest, chạy lint/test/build và kiểm tra responsive. Chưa dùng dữ liệu giả để kết luận tích hợp đã xong.

## 8. Phạm vi thay đổi để hạn chế xung đột

- Bước rà soát chỉ thêm tài liệu này trên `docs/wireframe-f2-f3`; không sửa đặc tả thuộc PR #35 hoặc code dùng chung.
- Bước vẽ sẽ thêm file F2/F3 trong thư mục wireframe, không sửa wireframe dashboard của người khác.
- Bước code #27 làm trên nhánh riêng từ main mới nhất, chủ yếu trong `frontend/src/features/member/` và `frontend/src/features/package/`; router/menu sửa tối thiểu sau khi phối hợp Gia Bảo.
- Không tự làm backend/migration của Lân, đăng nhập của Đức Việt/Gia Bảo hay đổi schema. Kiểm tra main và các PR liên quan trước khi tích hợp; không thể bảo đảm tránh mọi xung đột với thay đổi chưa được người khác push.
- Tiếp theo có thể phác thảo phần danh sách/form hội viên và cấu trúc danh mục đã rõ; các phần chờ Q2/Q4/Q6 cần ghi chú, không trình bày như yêu cầu đã duyệt.
