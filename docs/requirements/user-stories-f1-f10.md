# User story và tiêu chí chấp nhận F1–F10

- **Người biên soạn:** Phạm Gia Bảo (GamBug).
- **Nhiệm vụ:** #16 · WBS 1.2.4 · cập nhật 03/11/2026.
- **Nguồn chuẩn:** [Bản chốt phạm vi](ban-chot-pham-vi.md), [ERD v2](../design/erd/erd-v2.dbml), [quy tắc mã nguồn](../guides/quy-tac-ma-nguon.md), [kick-off](../meetings/2026-11-02-kick-off.md), [phân công Sprint 1](../plans/sprint-1-tien-do.md).
- Tuân theo mẫu `.github/ISSUE_TEMPLATE/user-story.md`. Story point chưa ước lượng; không gán lại người làm. Các task #5–#33 đã có được tham chiếu, không tạo bản sao task.
- 31 story, 86 kịch bản Given/When/Then. Nhãn dùng nhãn hiện có: `type:feature`, `F<n>`, `v1`/`v2`; milestone theo #16: phần cơ bản F1–F3 ở Sprint 1, còn lại Sprint 2. Mốc là kế hoạch, không xác nhận đã triển khai.
- Bảng này chỉ truy vết **story → issue → chức năng/phụ thuộc**, không thay bảng chức năng–use case–dữ liệu do Anh Tú phụ trách ở #14.
- Projects: chưa xác minh được việc thêm/di chuyển từng story; cần người có quyền kiểm tra bảng. Trưởng nhóm xác nhận auto-add issue vào Todo tại [#3](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/3); cần kiểm tra lại trên bảng bằng tài khoản có quyền, không coi auto-add đã được kiểm chứng cho các issue dưới đây.
- Giá trị nghiệp vụ chưa được nguồn chuẩn định nghĩa (ví dụ thời hạn reset, horizon sinh buổi, ranh giới grace/tháng phạt, hủy WAITLIST) cần chủ nghiệp vụ xác nhận; không tạo giá trị mới trong backlog.

## Chỉ mục truy vết

| Story | Yêu cầu | Version | Milestone | Issue |
| --- | --- | --- | --- | --- |
| US-F1.1 | Tự đăng ký tài khoản hội viên | v1 | Sprint 1 | [#46](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/46) |
| US-F1.2 | Đăng nhập, làm mới phiên và đăng xuất | v1 | Sprint 1 | [#47](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/47) |
| US-F1.3 | Giới hạn truy cập theo vai trò và quyền sở hữu | v1 | Sprint 1 | [#48](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/48) |
| US-F1.4 | Đổi mật khẩu | v1 | Sprint 2 | [#49](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/49) |
| US-F1.5 | Quên và đặt lại mật khẩu | v1 | Sprint 2 | [#50](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/50) |
| US-F1.6 | Khóa đăng nhập tạm khi dò mật khẩu | v1 | Sprint 2 | [#51](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/51) |
| US-F2.1 | Quản lý và tìm kiếm hồ sơ hội viên | v1 | Sprint 1 | [#52](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/52) |
| US-F2.2 | Tạo tài khoản cho hồ sơ có sẵn | v1 | Sprint 2 | [#53](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/53) |
| US-F2.3 | Ảnh đại diện và lịch sử chỉ số cơ thể | v1 | Sprint 2 | [#54](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/54) |
| US-F3.1 | Quản lý danh mục gói tập | v1 | Sprint 1 | [#55](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/55) |
| US-F3.2 | Bán và gia hạn gói tập tại quầy | v1 | Sprint 2 | [#56](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/56) |
| US-F3.3 | Xem bảng giá công khai | v1 | Sprint 2 | [#57](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/57) |
| US-F4.1 | Check-in thủ công với gói hợp lệ | v1 | Sprint 2 | [#58](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/58) |
| US-F5.1 | Thu tiền và in hóa đơn tại quầy | v1 | Sprint 2 | [#59](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/59) |
| US-F5.2 | Hoàn tiền có kiểm soát | v1 | Sprint 2 | [#60](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/60) |
| US-F6.1 | Xem dashboard vận hành cơ bản | v1 | Sprint 2 | [#61](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/61) |
| US-F7.1 | Xem dữ liệu tập luyện và hóa đơn cá nhân | v2 | Sprint 2 | [#62](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/62) |
| US-F7.2 | Gửi yêu cầu mua hoặc gia hạn từ web | v2 | Sprint 2 | [#63](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/63) |
| US-F8.1 | Quản lý lịch lớp lặp và sinh buổi | v2 | Sprint 2 | [#64](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/64) |
| US-F8.2 | Đặt chỗ lớp hoặc vào danh sách chờ | v2 | Sprint 2 | [#65](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/65) |
| US-F8.3 | Hủy chỗ và áp dụng quy tắc vi phạm | v2 | Sprint 2 | [#66](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/66) |
| US-F8.4 | Tự động vào lớp từ danh sách chờ | v2 | Sprint 2 | [#67](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/67) |
| US-F8.5 | Đổi phòng, đổi giờ và hủy buổi lớp | v2 | Sprint 2 | [#68](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/68) |
| US-F8.6 | Xem lịch dạy và điểm danh lớp | v2 | Sprint 2 | [#69](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/69) |
| US-F8.7 | Xem lịch lớp công khai | v2 | Sprint 2 | [#70](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/70) |
| US-F9.1 | Khai báo lịch rảnh huấn luyện viên | v2 | Sprint 2 | [#71](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/71) |
| US-F9.2 | Đặt buổi PT không trùng lịch | v2 | Sprint 2 | [#72](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/72) |
| US-F9.3 | Xác nhận buổi PT đã diễn ra | v2 | Sprint 2 | [#73](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/73) |
| US-F9.4 | Theo dõi giáo án và tiến độ học viên | v2 | Sprint 2 | [#74](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/74) |
| US-F10.1 | Nhận nhắc hết hạn gói và lịch lớp | v2 | Sprint 2 | [#75](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/75) |
| US-F10.2 | Nhận thông báo thay đổi lớp và được vào lớp | v2 | Sprint 2 | [#76](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/76) |

## Nội dung story

### US-F1.1 — Tự đăng ký tài khoản hội viên

**Mã chức năng:** F1
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F1.1 · **Mốc dự kiến:** Sprint 1

#### User story

Là **Khách**, tôi muốn **đăng ký bằng email, số điện thoại và mật khẩu**, để **có tài khoản và hồ sơ hội viên tại chi nhánh mặc định**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Tạo mới**

- **Given** email và SĐT hợp lệ chưa tồn tại, mật khẩu đạt S1.
- **When** khách gửi đăng ký.
- **Then** tạo đồng thời user MEMBER và hồ sơ ở chi nhánh mặc định, nhận JWT 15 phút và cookie refresh HttpOnly theo SRS F1.

**Kịch bản 2: Trùng hồ sơ**

- **Given** SĐT đã thuộc hồ sơ do lễ tân tạo trước.
- **When** khách tự đăng ký bằng SĐT đó.
- **Then** từ chối 409, hướng dẫn liên hệ quầy, không tự liên kết tài khoản với hồ sơ cũ.

**Kịch bản 3: Dữ liệu bị từ chối**

- **Given** email đã tồn tại hoặc mật khẩu không đạt S1 hoặc thiếu SĐT.
- **When** gửi đăng ký.
- **Then** email trùng trả 409; dữ liệu sai trả 400; không tạo tài khoản hay hồ sơ dở dang.

#### Quy tắc nghiệp vụ liên quan

Tài khoản F1/F2; S1, S3, S5. Email và SĐT duy nhất; mật khẩu tối thiểu 8 ký tự có chữ và số; tạo tài khoản và hồ sơ trong cùng giao dịch.

Nguồn: [Bản chốt phạm vi — F1](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `users`, `members`, `branches`.
- Phụ thuộc/truy vết: Tasks #26, #30; SRS UC-F1.1; wireframe #17.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F1.2 — Đăng nhập, làm mới phiên và đăng xuất

**Mã chức năng:** F1
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F1.2 · **Mốc dự kiến:** Sprint 1

#### User story

Là **Người dùng có tài khoản**, tôi muốn **đăng nhập bằng email và duy trì hoặc kết thúc phiên**, để **sử dụng đúng giao diện vai trò của mình**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Đăng nhập đúng**

- **Given** tài khoản ACTIVE và thông tin đúng.
- **When** đăng nhập bằng email.
- **Then** nhận access token và refresh cookie, mở trang theo vai trò; không hỗ trợ đăng nhập bằng SĐT.

**Kịch bản 2: Thông tin sai và tài khoản bị khóa**

- **Given** thông tin sai hoặc tài khoản có trạng thái khóa/ngừng hoạt động.
- **When** đăng nhập.
- **Then** sai email/mật khẩu báo chung 401; tài khoản khóa/ngừng hoạt động trả 403.

**Kịch bản 3: Làm mới**

- **Given** refresh cookie còn hạn và chưa bị thu hồi.
- **When** làm mới sau khi access token hết hạn.
- **Then** xoay refresh token, thu hồi token cũ và cấp JWT mới; dùng lại refresh cũ bị 401.

**Kịch bản 4: Kết thúc phiên**

- **Given** người dùng đã đăng nhập.
- **When** đăng xuất hoặc refresh không hợp lệ.
- **Then** đăng xuất thu hồi refresh và xóa cookie; frontend xóa access token; refresh lỗi không tạo vòng lặp đăng nhập.

#### Quy tắc nghiệp vụ liên quan

S2: access JWT 15 phút trong bộ nhớ frontend, refresh trong cookie HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth. Xoay vòng và thu hồi token theo SRS F1. Chống dò mật khẩu S4 được tách riêng thành US-F1.6 ở Sprint 2.

Nguồn: [Bản chốt phạm vi — F1](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `users`, `refresh_tokens`.
- Phụ thuộc/truy vết: Tasks #25, #26, #30; SRS UC-F1.2, UC-F1.3; ADR 0008.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F1.3 — Giới hạn truy cập theo vai trò và quyền sở hữu

**Mã chức năng:** F1
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F1.3 · **Mốc dự kiến:** Sprint 1

#### User story

Là **Người dùng có tài khoản**, tôi muốn **chỉ truy cập chức năng và dữ liệu được phép**, để **bảo vệ thông tin hội viên và vận hành chi nhánh**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Đúng quyền**

- **Given** JWT hợp lệ và vai trò được phép.
- **When** gọi API nghiệp vụ của mình.
- **Then** backend cho phép trong phạm vi quyền sở hữu, frontend hiện menu phù hợp.

**Kịch bản 2: Sai quyền**

- **Given** hội viên A hoặc PT không dạy hội viên B.
- **When** yêu cầu dữ liệu hội viên B.
- **Then** backend từ chối; PT không dùng API tìm kiếm hội viên chung, chỉ xem học viên mình dạy theo kick-off.

**Kịch bản 3: Chưa xác thực**

- **Given** JWT thiếu, hết hạn hoặc sai chữ ký.
- **When** gọi API bảo vệ.
- **Then** trả 401; có JWT nhưng thiếu quyền trả 403; không tin role hay memberId do trình duyệt tự gửi.

#### Quy tắc nghiệp vụ liên quan

S3 kiểm tra ở backend mọi API; mỗi tài khoản đúng một vai trò; khách không có tài khoản; v1–v3 dùng chi nhánh mặc định, đa chi nhánh v4.

Nguồn: [Bản chốt phạm vi — F1](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `users`, `staff`, `members`.
- Phụ thuộc/truy vết: Task #25; SRS UC-F1.6; kick-off 02/11; #27 menu F2/F3 thuộc Anh Tú.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F1.4 — Đổi mật khẩu

**Mã chức năng:** F1
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F1.4 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Người dùng có tài khoản**, tôi muốn **đổi mật khẩu hiện tại**, để **bảo vệ tài khoản khi cần thay thông tin xác thực**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Đổi thành công**

- **Given** đã đăng nhập và mật khẩu hiện tại đúng.
- **When** gửi mật khẩu mới hợp lệ khác mật khẩu cũ.
- **Then** cập nhật mã băm, thu hồi các refresh token và yêu cầu xác thực lại.

**Kịch bản 2: Từ chối**

- **Given** mật khẩu hiện tại sai hoặc mật khẩu mới trùng cũ.
- **When** yêu cầu đổi.
- **Then** trả 422 và không thay mật khẩu; dữ liệu sai định dạng trả 400 theo kick-off.

#### Quy tắc nghiệp vụ liên quan

S1 mật khẩu bcrypt tối thiểu 8 ký tự gồm chữ và số; SRS F1 yêu cầu kiểm tra mật khẩu hiện tại và thu hồi refresh sau thay đổi; mã lỗi theo kick-off.

Nguồn: [Bản chốt phạm vi — F1](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `users`, `refresh_tokens`.
- Phụ thuộc/truy vết: SRS UC-F1.4; backend F1 thuộc Đức Việt.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F1.5 — Quên và đặt lại mật khẩu

**Mã chức năng:** F1
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F1.5 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Người dùng quên mật khẩu**, tôi muốn **nhận liên kết đặt lại mật khẩu qua email**, để **khôi phục quyền sử dụng tài khoản**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Đặt lại**

- **Given** tài khoản ACTIVE và yêu cầu quên mật khẩu.
- **When** dùng liên kết email có token còn hạn và mật khẩu mới đạt S1.
- **Then** cập nhật mật khẩu, đánh dấu token đã dùng và thu hồi refresh token.

**Kịch bản 2: Token không hợp lệ**

- **Given** token hết hạn, đã dùng hoặc không tồn tại.
- **When** gửi đặt lại mật khẩu.
- **Then** từ chối, không thay mật khẩu và không cấp phiên từ token đó.

#### Quy tắc nghiệp vụ liên quan

S1; SRS F1: token reset lưu mã băm, dùng một lần, có expires_at; không tự đặt thêm thời hạn token chưa được chốt.

Nguồn: [Bản chốt phạm vi — F1](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `users`, `password_resets`, `refresh_tokens`.
- Phụ thuộc/truy vết: SRS UC-F1.5; F10 email.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F1.6 — Khóa đăng nhập tạm khi dò mật khẩu

**Mã chức năng:** F1
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F1.6 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Người dùng có tài khoản**, tôi muốn **tài khoản được bảo vệ khi có nhiều lần nhập sai liên tiếp**, để **giảm nguy cơ bị dò mật khẩu**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Đủ số lần sai**

- **Given** đã có 4 lần sai liên tiếp trên tài khoản.
- **When** đăng nhập sai lần thứ 5.
- **Then** khóa đăng nhập 15 phút và báo tài khoản bị khóa tạm theo mã 403 đã chốt.

**Kịch bản 2: Trong thời gian khóa**

- **Given** locked_until vẫn ở tương lai.
- **When** gửi thông tin đăng nhập kể cả đúng.
- **Then** từ chối đăng nhập, không cấp phiên.

**Kịch bản 3: Đăng nhập hợp lệ**

- **Given** thời gian khóa đã hết và tài khoản ACTIVE.
- **When** đăng nhập đúng.
- **Then** cho đăng nhập, đặt lại failed_login_count và locked_until theo SRS F1.

#### Quy tắc nghiệp vụ liên quan

S4: khóa đăng nhập 15 phút sau 5 lần sai liên tiếp; SRS F1: failed_login_count, locked_until. Không đồng nhất khóa tạm locked_until với trạng thái LOCKED của tài khoản.

Nguồn: [Bản chốt phạm vi — F1](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `users`.
- Phụ thuộc/truy vết: US-F1.2; S4; sprint plan mục 6.3 (Sprint 2).
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F2.1 — Quản lý và tìm kiếm hồ sơ hội viên

**Mã chức năng:** F2
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F2.1 · **Mốc dự kiến:** Sprint 1

#### User story

Là **Lễ tân**, tôi muốn **tạo, sửa và tìm hồ sơ hội viên trong chi nhánh**, để **phục vụ hội viên tại quầy kể cả khi họ chưa có tài khoản**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Hồ sơ không tài khoản**

- **Given** SĐT hợp lệ chưa dùng.
- **When** lễ tân tạo hồ sơ và tìm lại theo mã/SĐT.
- **Then** lưu hồ sơ độc lập với tài khoản và tìm đúng dữ liệu chi nhánh.

**Kịch bản 2: Cập nhật và ngừng hoạt động**

- **Given** có hồ sơ trong chi nhánh.
- **When** sửa thông tin hợp lệ hoặc ngừng hoạt động.
- **Then** lưu thay đổi; nếu ngừng hồ sơ thì đồng thời khóa tài khoản liên kết.

**Kịch bản 3: Dữ liệu không hợp lệ**

- **Given** SĐT trùng hoặc nhân viên khác chi nhánh hoặc PT tìm kiếm chung.
- **When** gửi yêu cầu.
- **Then** từ chối theo S3/validation, không ghi đè hồ sơ khác.

#### Quy tắc nghiệp vụ liên quan

F2; S3; SĐT hội viên duy nhất; mã HV + 6 số; ngừng hồ sơ đồng thời khóa tài khoản. Sprint 1 chưa có ảnh/chỉ số; phần đó là US-F2.3 Sprint 2.

Nguồn: [Bản chốt phạm vi — F2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `members`, `users`.
- Phụ thuộc/truy vết: Task #28 backend; #27 frontend Anh Tú; SRS F2; ảnh/chỉ số triển khai Sprint 2 theo phân công.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F2.2 — Tạo tài khoản cho hồ sơ có sẵn

**Mã chức năng:** F2
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F2.2 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Lễ tân**, tôi muốn **kích hoạt tài khoản cho hội viên đã có hồ sơ**, để **giúp họ dùng cổng hội viên mà không sinh hồ sơ trùng**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Cấp tài khoản**

- **Given** hồ sơ chưa liên kết user và email chưa dùng.
- **When** lễ tân tạo tài khoản cho hồ sơ.
- **Then** liên kết đúng hồ sơ và gửi email đặt mật khẩu, không tạo thêm member.

**Kịch bản 2: Không thể cấp**

- **Given** hồ sơ đã có tài khoản hoặc email đã dùng.
- **When** tạo tài khoản lần nữa.
- **Then** từ chối, không chuyển quyền sở hữu hồ sơ hay tạo user trùng.

#### Quy tắc nghiệp vụ liên quan

Tài khoản F1/F2; S3: chỉ lễ tân tạo cho hồ sơ có sẵn, gửi email đặt mật khẩu, không tự nhận hồ sơ bằng SĐT.

Nguồn: [Bản chốt phạm vi — F2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `users`, `members`, `password_resets`.
- Phụ thuộc/truy vết: SRS UC-F2.2; US-F1.5; backend Đức Việt phối hợp F2 Lân.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F2.3 — Ảnh đại diện và lịch sử chỉ số cơ thể

**Mã chức năng:** F2
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F2.3 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Nhân viên được phép**, tôi muốn **cập nhật ảnh và chỉ số cơ thể hội viên thuộc phạm vi quản lý**, để **đối chiếu tại quầy và theo dõi tiến độ**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Dữ liệu hợp lệ**

- **Given** nơi lưu ảnh đã chốt và nhân viên có quyền trên hồ sơ.
- **When** cập nhật ảnh hoặc ghi số đo cơ thể.
- **Then** lưu ảnh/giá trị và lịch sử đúng hội viên, body_metrics có recorded_by theo SRS.

**Kịch bản 2: Ngoài phạm vi**

- **Given** PT không dạy hội viên hoặc hội viên yêu cầu chỉ số người khác.
- **When** xem hay sửa dữ liệu.
- **Then** backend từ chối theo S3, không lộ ảnh/chỉ số của người khác.

#### Quy tắc nghiệp vụ liên quan

F2; S3; kick-off: PT chỉ xem học viên mình dạy, hội viên chỉ xem số đo của chính mình. Không dùng ổ đĩa tạm Render làm nơi lưu ảnh theo kế hoạch; nơi lưu phải được nhóm chốt trước triển khai.

Nguồn: [Bản chốt phạm vi — F2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `members`, `body_metrics`, `users`.
- Phụ thuộc/truy vết: SRS UC-F2.5; F4 cần ảnh; #33 cần chốt nơi lưu ảnh đầu Sprint 2; F9 dùng body_metrics.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F3.1 — Quản lý danh mục gói tập

**Mã chức năng:** F3
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F3.1 · **Mốc dự kiến:** Sprint 1

#### User story

Là **Quản lý chi nhánh**, tôi muốn **tạo, sửa và ngừng bán gói TIME, SESSION hoặc PT**, để **cung cấp danh mục đúng cho quầy**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Danh mục hợp lệ**

- **Given** quản lý được phép và thông tin gói hợp lệ.
- **When** thêm hoặc sửa gói.
- **Then** lưu đúng loại, thời hạn/số buổi và giá VND.

**Kịch bản 2: Ngừng bán**

- **Given** gói đã tồn tại.
- **When** ngừng bán gói.
- **Then** không cho bán mới; giữ dữ liệu nghiệp vụ và giá đã chốt trên hóa đơn cũ.

**Kịch bản 3: Từ chối**

- **Given** vai trò không được phép hoặc dữ liệu loại/thời hạn/giá sai.
- **When** ghi danh mục.
- **Then** từ chối và không làm thay đổi danh mục.

#### Quy tắc nghiệp vụ liên quan

F3; TIME 1/3/6/12 tháng, 1 tháng=30 ngày; SESSION theo buổi; PT chỉ tập 1-1; tiền số nguyên VND và giá server.

Nguồn: [Bản chốt phạm vi — F3](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `packages`.
- Phụ thuộc/truy vết: Task #29 backend; #27 frontend Anh Tú; #6 SRS F3/F5.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F3.2 — Bán và gia hạn gói tập tại quầy

**Mã chức năng:** F3
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F3.2 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Lễ tân**, tôi muốn **đăng ký hoặc gia hạn gói cho hội viên**, để **đảm bảo hạn và quyền sử dụng chính xác**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Mua gói**

- **Given** hồ sơ có sẵn và gói đang bán.
- **When** lập hóa đơn rồi xác nhận thanh toán tại quầy.
- **Then** gói chỉ có hiệu lực sau PAID, dùng giá server và thời hạn đúng loại.

**Kịch bản 2: Gia hạn nối tiếp**

- **Given** gói cùng loại còn hạn.
- **When** thanh toán gói gia hạn.
- **Then** gói mới bắt đầu ngay sau ngày hết hạn cũ, trạng thái UPCOMING trước khi tới lượt.

**Kịch bản 3: Giữ nhiều loại**

- **Given** hội viên đang có gói TIME.
- **When** mua gói PT hợp lệ.
- **Then** cho giữ PT cùng TIME; quyền lợi mỗi loại theo đúng F3.

**Kịch bản 4: Từ chối hiệu lực cùng loại**

- **Given** hội viên đã có gói TIME đang hiệu lực tại chi nhánh.
- **When** cố kích hoạt thêm TIME cùng chi nhánh trong thời gian chồng nhau.
- **Then** từ chối tạo gói thứ hai cùng loại đang hiệu lực; gia hạn nối tiếp đi theo UPCOMING.

#### Quy tắc nghiệp vụ liên quan

F3: mỗi loại tối đa một gói hiệu lực tại chi nhánh; giữ đồng thời nhiều loại. Gia hạn nối sau ngày hết hạn cũ, UPCOMING trong thời gian chờ; chỉ kích hoạt khi PAID.

Nguồn: [Bản chốt phạm vi — F3](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `memberships`, `packages`, `invoices`, `invoice_items`.
- Phụ thuộc/truy vết: US-F5.1; #6 SRS; F3 backend Lân.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F3.3 — Xem bảng giá công khai

**Mã chức năng:** F3
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F3.3 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Khách**, tôi muốn **xem các gói đang bán và giá**, để **chọn gói trước khi liên hệ quầy**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Xem giá**

- **Given** có gói đang bán.
- **When** khách mở bảng giá không đăng nhập.
- **Then** thấy loại gói, thời hạn/số buổi, giá VND từ server và chỉ dẫn thanh toán tại quầy.

**Kịch bản 2: Không có dữ liệu**

- **Given** không có gói đang bán hoặc tải lỗi.
- **When** mở bảng giá.
- **Then** hiển thị trạng thái trống/lỗi, không bịa giá; không cho thanh toán online.

#### Quy tắc nghiệp vụ liên quan

Vai trò Khách; F3; gói TIME/SESSION/PT đúng mục đích; mọi thanh toán tại quầy, không có cổng thanh toán online.

Nguồn: [Bản chốt phạm vi — F3](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `packages`.
- Phụ thuộc/truy vết: Wireframe #17 WBS 1.4.2; API danh mục #29 do Lân sở hữu, endpoint public cần owner xác nhận.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F4.1 — Check-in thủ công với gói hợp lệ

**Mã chức năng:** F4
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F4.1 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Lễ tân**, tôi muốn **tra cứu và đối chiếu ảnh hội viên để check-in**, để **ghi nhận đúng lượt vào và số buổi sử dụng**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Gói TIME**

- **Given** hội viên có TIME ACTIVE hợp lệ trong giờ mở cửa.
- **When** lễ tân tìm mã/SĐT, đối chiếu ảnh và xác nhận.
- **Then** ghi check-in, không trừ SESSION dù đồng thời có SESSION.

**Kịch bản 2: Gói theo buổi**

- **Given** chỉ có SESSION ACTIVE còn buổi.
- **When** check-in lần đầu và quay lại cùng ngày Việt Nam.
- **Then** lần đầu trừ 1 buổi, lần sau không trừ thêm.

**Kịch bản 3: Từ chối**

- **Given** chỉ có PT hoặc gói hết hạn/không hợp lệ hoặc ngoài giờ.
- **When** yêu cầu check-in.
- **Then** báo lý do, không tạo lượt vào hay trừ buổi.

**Kịch bản 4: Liên kết lớp**

- **Given** hội viên có chỗ lớp sắp bắt đầu theo SRS F4.
- **When** check-in tại quầy.
- **Then** đánh dấu arrived_at; PT vẫn phải xác nhận attendance, không tự coi đã dự lớp.

#### Quy tắc nghiệp vụ liên quan

F4; NF1 dưới 2 giây; TIME ưu tiên SESSION; PT không cho vào; SESSION chỉ trừ lần đầu trong ngày Việt Nam; trong giờ mở cửa.

Nguồn: [Bản chốt phạm vi — F4](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `checkins`, `members`, `memberships`, `class_bookings`.
- Phụ thuộc/truy vết: SRS F4 (đã merge #44/#45); ảnh lưu Sprint 2; F8 điểm danh riêng.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F5.1 — Thu tiền và in hóa đơn tại quầy

**Mã chức năng:** F5
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F5.1 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Lễ tân**, tôi muốn **xác nhận tiền mặt hoặc chuyển khoản VietQR và in PDF**, để **kích hoạt gói sau khi phòng tập đã nhận tiền**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Tiền mặt hoặc VietQR**

- **Given** hóa đơn hợp lệ và đã nhận tiền tại quầy.
- **When** lễ tân/quản lý xác nhận.
- **Then** ghi phương thức/tiền nhận, PAID, kích hoạt gói phù hợp và in PDF; VietQR có số tiền để chuyển thủ công.

**Kịch bản 2: Sửa giá ở trình duyệt**

- **Given** trình duyệt gửi giá hoặc tổng tiền giả.
- **When** lập/thanh toán hóa đơn.
- **Then** server tính theo dữ liệu chuẩn, không chấp nhận số tiền do client quyết định.

**Kịch bản 3: Sai quyền hoặc thiếu hội viên**

- **Given** hội viên tự yêu cầu xác nhận thu hoặc hóa đơn có gói nhưng thiếu hội viên.
- **When** gửi yêu cầu.
- **Then** từ chối; khách lẻ mua hàng không cần member chỉ thuộc POS v3, không mở POS trong Sprint 2.

#### Quy tắc nghiệp vụ liên quan

F5/F7; S7: server tính giá, dòng hóa đơn giữ giá lúc bán; lễ tân/quản lý xác nhận thu; một mã khuyến mãi tối đa (F14 v3); không thanh toán online.

Nguồn: [Bản chốt phạm vi — F5](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `invoices`, `invoice_items`, `payments`, `memberships`.
- Phụ thuộc/truy vết: SRS #6; US-F3.2; US-F7.2; ADR 0004.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F5.2 — Hoàn tiền có kiểm soát

**Mã chức năng:** F5
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F5.2 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Quản lý chi nhánh**, tôi muốn **thực hiện hoàn tiền hóa đơn thuộc quyền quản lý**, để **xử lý khoản thu sai có truy vết**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Đúng quyền**

- **Given** có hóa đơn và quản lý chi nhánh được phép.
- **When** ghi nhận hoàn tiền hợp lệ theo đặc tả F5.
- **Then** lưu dữ liệu hoàn và nhật ký người thao tác/thời điểm.

**Kịch bản 2: Sai quyền**

- **Given** lễ tân hoặc hội viên.
- **When** yêu cầu hoàn tiền.
- **Then** backend từ chối, không ghi nhận khoản hoàn.

#### Quy tắc nghiệp vụ liên quan

Thanh toán: chỉ quản lý chi nhánh hoàn tiền, mọi lần hoàn ghi nhật ký hoạt động. Chính sách hoàn khác chưa nêu trong bản chốt không tự suy diễn.

Nguồn: [Bản chốt phạm vi — F5](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `invoices`, `payments`, `audit_logs`.
- Phụ thuộc/truy vết: #6 đặc tả F5; F17 audit log (v3), cần phối hợp khi triển khai; không tự đặt chính sách mức/phần trăm hoàn.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F6.1 — Xem dashboard vận hành cơ bản

**Mã chức năng:** F6
**Version:** v1
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F6.1 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Quản lý chi nhánh**, tôi muốn **xem doanh thu, hội viên và lượt check-in theo ngày/giờ**, để **theo dõi tình hình vận hành chi nhánh**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Có dữ liệu**

- **Given** nhân viên được phép tại chi nhánh có giao dịch/check-in.
- **When** mở dashboard.
- **Then** tổng hợp đúng doanh thu, số hội viên và lượt check-in theo ngày/giờ Việt Nam theo SRS F6.

**Kịch bản 2: Hóa đơn không member**

- **Given** có hóa đơn PAID của khách lẻ ở v3.
- **When** tổng hợp doanh thu.
- **Then** không bỏ sót hóa đơn do member_id NULL; chỉ tính khoản thu hợp lệ theo SRS.

**Kịch bản 3: Chưa có dữ liệu hoặc sai quyền**

- **Given** không có lượt/thu hoặc người dùng ngoài phạm vi.
- **When** mở dashboard.
- **Then** trường hợp trống hiển thị số 0; sai quyền bị từ chối, không lộ dữ liệu.

#### Quy tắc nghiệp vụ liên quan

F6; S3 phạm vi chi nhánh; VND số nguyên; doanh thu LEFT JOIN members để giữ hóa đơn khách lẻ; không mở báo cáo nâng cao F16.

Nguồn: [Bản chốt phạm vi — F6](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `invoices`, `payments`, `members`, `checkins`.
- Phụ thuộc/truy vết: SRS F6; wireframe đã merge #42; SRS sửa #44/#45.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F7.1 — Xem dữ liệu tập luyện và hóa đơn cá nhân

**Mã chức năng:** F7
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F7.1 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Hội viên**, tôi muốn **xem gói, lịch sử check-in và hóa đơn của mình**, để **theo dõi quyền tập và giao dịch**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Dữ liệu của mình**

- **Given** hội viên đã đăng nhập.
- **When** mở cổng hội viên.
- **Then** hiện gói/trạng thái/hạn, lịch sử check-in và hóa đơn của chính mình.

**Kịch bản 2: Truy cập chéo**

- **Given** hội viên đổi id trên URL/request sang người khác.
- **When** yêu cầu dữ liệu.
- **Then** backend từ chối theo S3, không dựa vào việc frontend ẩn liên kết.

#### Quy tắc nghiệp vụ liên quan

F7; S3: chỉ xem dữ liệu của mình; hồ sơ chưa có tài khoản không dùng cổng hội viên.

Nguồn: [Bản chốt phạm vi — F7](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `members`, `memberships`, `checkins`, `invoices`.
- Phụ thuộc/truy vết: SRS #8; F1 xác thực; cổng hội viên do Trọng Việt sở hữu.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F7.2 — Gửi yêu cầu mua hoặc gia hạn từ web

**Mã chức năng:** F7
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F7.2 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Hội viên**, tôi muốn **chọn gói để lập yêu cầu thanh toán tại quầy**, để **chuẩn bị giao dịch mà không phải trả tiền online**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Yêu cầu mới**

- **Given** hội viên đăng nhập và gói đang bán.
- **When** chọn mua/gia hạn.
- **Then** tạo hóa đơn và gói PENDING, chưa cho tập; hướng dẫn trả tại quầy.

**Kịch bản 2: Thanh toán sau**

- **Given** hóa đơn chờ còn hợp lệ.
- **When** lễ tân nhận tiền và xác nhận.
- **Then** tính lại ngày bắt đầu tại thời điểm thanh toán, xử lý nối tiếp đúng F3.

**Kịch bản 3: Quá hạn**

- **Given** hóa đơn MEMBER_PORTAL quá PENDING_INVOICE_TTL_DAYS=3.
- **When** tác vụ hết hạn chạy.
- **Then** hủy hóa đơn và gói PENDING đi cùng; không tự hủy hóa đơn nguồn STAFF.

#### Quy tắc nghiệp vụ liên quan

F7: tạo hóa đơn PENDING nguồn MEMBER_PORTAL và gói PENDING; ngày bắt đầu tính lại khi thanh toán; hóa đơn F7 quá 3 ngày* hủy cùng gói, hóa đơn STAFF không tự hủy.

Nguồn: [Bản chốt phạm vi — F7](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `invoices`, `invoice_items`, `memberships`.
- Phụ thuộc/truy vết: US-F3.2, US-F5.1; SRS #8; PENDING_INVOICE_TTL_DAYS.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F8.1 — Quản lý lịch lớp lặp và sinh buổi

**Mã chức năng:** F8
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F8.1 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Quản lý chi nhánh**, tôi muốn **lập lịch lớp hằng tuần và sinh các buổi cụ thể**, để **công bố lịch có phòng, PT và sức chứa hợp lệ**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Lịch hợp lệ**

- **Given** quản lý có quyền và lịch/phòng/PT hợp lệ.
- **When** tạo lịch tuần và sinh buổi.
- **Then** lưu các buổi đúng lịch với thời gian UTC, hiển thị Việt Nam.

**Kịch bản 2: Sinh lại**

- **Given** buổi cùng schedule+occurrence_date đã tồn tại kể cả đã đổi giờ.
- **When** chạy sinh lần nữa.
- **Then** không tạo buổi trùng hay phục hồi giờ cũ.

**Kịch bản 3: Sức chứa sai**

- **Given** capacity lớn hơn phòng hoặc không dương.
- **When** lưu lịch/buổi.
- **Then** từ chối, không công bố buổi không hợp lệ.

#### Quy tắc nghiệp vụ liên quan

F8; ERD: ISO day_of_week 1–7; capacity>0 và <=room.capacity; trainer trỏ staff role TRAINER; session unique schedule+occurrence_date, occurrence_date giữ nguyên khi đổi giờ.

Nguồn: [Bản chốt phạm vi — F8](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `class_types`, `class_schedules`, `class_sessions`, `rooms`, `staff`.
- Phụ thuộc/truy vết: Đặc tả #9; migration #22 nhóm4; sinh buổi horizon/cadence chưa chốt.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F8.2 — Đặt chỗ lớp hoặc vào danh sách chờ

**Mã chức năng:** F8
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F8.2 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Hội viên**, tôi muốn **đặt chỗ vào buổi học nhóm phù hợp**, để **tham gia lớp nằm trong quyền lợi gói TIME**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Còn chỗ**

- **Given** TIME ACTIVE, không bị khóa, không trùng giờ, trong cửa sổ 7 ngày.
- **When** đặt buổi còn chỗ.
- **Then** giữ BOOKED; nếu đầy thì WAITLIST theo FIFO và hiển thị trạng thái.

**Kịch bản 2: Không đủ điều kiện**

- **Given** chỉ SESSION/PT, TIME hết hạn, bị khóa hoặc ngoài cửa sổ.
- **When** đặt hoặc vào chờ.
- **Then** từ chối ngay kèm lý do, không tạo chỗ/chờ không hợp lệ.

**Kịch bản 3: Trùng và đồng thời**

- **Given** đã có chỗ buổi đó hoặc hai yêu cầu tranh chỗ cuối/đặt buổi trùng giờ.
- **When** gửi yêu cầu đồng thời.
- **Then** không tạo trùng, không vượt sĩ số, không lọt trùng giờ; khóa members trước class_sessions theo NF6.

**Kịch bản 4: Đặt lại và nhiều chờ**

- **Given** đã hủy buổi hoặc đang chờ nhiều buổi.
- **When** đặt lại khi đủ điều kiện hoặc vào thêm danh sách chờ.
- **Then** cho đặt lại; không áp giới hạn số danh sách chờ chưa có trong phạm vi.

#### Quy tắc nghiệp vụ liên quan

F8: TIME ACTIVE dùng được tại chi nhánh; mở trước 7 ngày*; không trùng giờ/trùng chỗ; SESSION/PT không đủ; không giới hạn số WAITLIST; lễ tân thao tác hộ không ưu tiên.

Nguồn: [Bản chốt phạm vi — F8](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `class_bookings`, `class_sessions`, `members`, `memberships`.
- Phụ thuộc/truy vết: Đặc tả #9; BOOKING_WINDOW_DAYS; NF6; F9 kiểm tra trùng.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F8.3 — Hủy chỗ và áp dụng quy tắc vi phạm

**Mã chức năng:** F8
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F8.3 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Hội viên**, tôi muốn **hủy chỗ và biết hậu quả hủy muộn/vắng**, để **chủ động lịch tập và hiểu lý do bị khóa đặt**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Hủy sớm**

- **Given** đã giữ chỗ và còn đúng hoặc hơn 2 giờ trước lớp.
- **When** hủy chỗ.
- **Then** CANCELLED, không hủy muộn, giải phóng chỗ và xét chờ theo cutoff.

**Kịch bản 2: Đủ vi phạm**

- **Given** hủy muộn/vắng đủ PENALTY_LIMIT_PER_MONTH=3 trong tháng.
- **When** ghi nhận vi phạm.
- **Then** khóa đặt lớp PENALTY_LOCK_DAYS=7, không cho đặt/vào chờ và báo lý do.

**Kịch bản 3: Ngoại lệ không phạt**

- **Given** đang trong grace promotion hoặc mất chỗ do không reconfirm/hủy buổi.
- **When** hủy hay hệ thống hủy.
- **Then** không tăng vi phạm theo ngoại lệ đã chốt.

#### Quy tắc nghiệp vụ liên quan

F8: trước ít nhất 2h* không phạt; hủy muộn hoặc NO_SHOW đủ 3 lần/tháng* khóa đặt 7 ngày*; promotion có grace; hủy do đổi giờ/hủy buổi không phạt. Chi tiết cancel WAITLIST và ranh giới tháng cần xác nhận.

Nguồn: [Bản chốt phạm vi — F8](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `class_bookings`, `members`, `system_configs`.
- Phụ thuộc/truy vết: Đặc tả #9; US-F8.4; LATE_CANCEL_HOURS, PENALTY_LIMIT_PER_MONTH, PENALTY_LOCK_DAYS.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F8.4 — Tự động vào lớp từ danh sách chờ

**Mã chức năng:** F8
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F8.4 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Hội viên trong danh sách chờ**, tôi muốn **được đưa vào lớp đúng thứ tự khi có chỗ**, để **không phải canh chỗ và không bị lễ tân chen thứ tự**.

#### Tiêu chí chấp nhận

**Kịch bản 1: FIFO và bỏ qua**

- **Given** có chỗ và còn ít nhất30 phút, người đầu không còn đủ điều kiện.
- **When** chạy xét chờ.
- **Then** bỏ qua người không đủ điều kiện, chọn người đủ điều kiện đầu tiên và thông báo, không chờ xác nhận.

**Kịch bản 2: Cutoff**

- **Given** còn đúng30 phút hoặc dưới30 phút trước lớp.
- **When** có chỗ trống.
- **Then** đúng30 không thuộc điều kiện ngừng; dưới30 không tự đưa người vào và để chỗ trống theo quy tắc.

**Kịch bản 3: Ban đêm và grace**

- **Given** ngoài giờ mở cửa và có người được tự vào lớp.
- **When** promotion xảy ra.
- **Then** giữ chỗ ngay, thông báo web ngay, gửi email lúc mở cửa; grace_until từ lúc gửi email+15 phút, hủy trong grace không phạt.

**Kịch bản 4: Không chen chờ**

- **Given** có danh sách chờ và lễ tân thao tác hộ.
- **When** cố xếp người vượt FIFO.
- **Then** từ chối mọi ưu tiên trái thứ tự; giao dịch đồng thời không vượt capacity.

#### Quy tắc nghiệp vụ liên quan

FIFO waitlisted_at tăng dần; tự BOOKED không xác nhận; revalidate TIME/khóa/trùng; ngừng khi còn dưới30 phút*; ân hạn15 phút* tính từ gửi email, ban đêm email đợi giờ mở cửa.

Nguồn: [Bản chốt phạm vi — F8](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `class_bookings`, `class_sessions`, `members`, `notifications`.
- Phụ thuộc/truy vết: Đặc tả #9; ADR0003; F10; PROMOTION_CUTOFF_MIN, PROMOTION_GRACE_MIN.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F8.5 — Đổi phòng, đổi giờ và hủy buổi lớp

**Mã chức năng:** F8
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F8.5 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Quản lý chi nhánh**, tôi muốn **điều chỉnh buổi và thông báo người liên quan**, để **xử lý thay đổi lịch có xác nhận lại và không phạt oan**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Đổi phòng**

- **Given** buổi có chỗ và phòng mới đủ sức chứa.
- **When** đổi phòng.
- **Then** thông báo, không bắt xác nhận lại chỉ vì thay phòng.

**Kịch bản 2: Đổi giờ**

- **Given** buổi có hội viên giữ chỗ.
- **When** đổi giờ.
- **Then** lưu giờ cũ theo ERD, yêu cầu reconfirm trong60 phút; xác nhận giữ chỗ theo điều kiện hợp lệ, không xác nhận/từ chối thì mất chỗ không phạt và xét chờ.

**Kịch bản 3: Hủy buổi**

- **Given** buổi có BOOKED/WAITLIST.
- **When** quản lý hủy buổi.
- **Then** hủy mọi chỗ và chờ, không phạt, thông báo tất cả; không tự promote vào buổi đã hủy.

#### Quy tắc nghiệp vụ liên quan

F8: đổi phòng chỉ báo; đổi giờ yêu cầu chỗ đã giữ reconfirm60 phút*; quá hạn/từ chối mất chỗ không phạt; hủy buổi hủy mọi chỗ và chờ; occurrence_date không đổi.

Nguồn: [Bản chốt phạm vi — F8](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `class_sessions`, `class_bookings`, `notifications`.
- Phụ thuộc/truy vết: Đặc tả #9; F10; RECONFIRM_TTL_MIN=60.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F8.6 — Xem lịch dạy và điểm danh lớp

**Mã chức năng:** F8
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F8.6 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Huấn luyện viên**, tôi muốn **xem buổi mình dạy và xác nhận có mặt hoặc vắng**, để **ghi nhận tham gia và xử lý vắng đúng quy tắc**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Điểm danh đúng lớp**

- **Given** PT được phân công và có chỗ đặt.
- **When** PT xác nhận có mặt/vắng.
- **Then** ghi ATTENDED/NO_SHOW và người điểm danh, NO_SHOW xử lý vi phạm theo F8.

**Kịch bản 2: Đã tới quầy**

- **Given** chỗ có arrived_at do check-in.
- **When** PT mở điểm danh.
- **Then** hiển thị đã đến nhưng vẫn cần PT xác nhận tham dự.

**Kịch bản 3: Sai PT**

- **Given** PT không được phân công lớp.
- **When** xem danh sách hoặc gửi điểm danh.
- **Then** backend từ chối, không lộ dữ liệu hay đổi attendance.

#### Quy tắc nghiệp vụ liên quan

F8; attendance_marked_by trỏ users.id; trainer_id trỏ staff.id; ATTENDED/NO_SHOW; check-in quầy chỉ arrived_at, không thay điểm danh; S3 chỉ lớp/học viên mình dạy.

Nguồn: [Bản chốt phạm vi — F8](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `class_sessions`, `class_bookings`, `staff`.
- Phụ thuộc/truy vết: Đặc tả #9; SRS F4 arrived_at; F10 thông báo.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F8.7 — Xem lịch lớp công khai

**Mã chức năng:** F8
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F8.7 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Khách**, tôi muốn **xem lịch lớp mà không đăng nhập**, để **chọn buổi phù hợp trước khi đăng ký hội viên**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Lịch công khai**

- **Given** có lịch lớp.
- **When** khách mở lịch không đăng nhập.
- **Then** thấy thời gian, loại lớp, phòng/PT và trạng thái buổi công khai; không lộ thông tin người đặt.

**Kịch bản 2: Chưa đủ quyền đặt**

- **Given** khách chưa đăng nhập.
- **When** muốn đặt lớp.
- **Then** chuyển đăng nhập/đăng ký, không tự tạo chỗ; khi không có buổi hiển thị trạng thái trống.

#### Quy tắc nghiệp vụ liên quan

Vai trò Khách và F8; chỉ dữ liệu lịch công khai, không danh sách hội viên; muốn đặt lớp cần tài khoản và TIME ACTIVE.

Nguồn: [Bản chốt phạm vi — F8](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `class_schedules`, `class_sessions`, `class_types`.
- Phụ thuộc/truy vết: Wireframe #17; đặc tả #9; public API cần chủ F8 xác nhận.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F9.1 — Khai báo lịch rảnh huấn luyện viên

**Mã chức năng:** F9
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F9.1 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Huấn luyện viên**, tôi muốn **khai báo khung giờ rảnh và xem lịch dạy**, để **cho hội viên chọn thời gian hợp lệ**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Lịch rảnh của mình**

- **Given** PT đã đăng nhập.
- **When** khai báo lịch rảnh hợp lệ và xem lịch.
- **Then** lưu khung giờ và hiển thị buổi PT/lớp được phân công.

**Kịch bản 2: Sai quyền/dữ liệu**

- **Given** PT sửa lịch của người khác hoặc giờ kết thúc không hợp lệ.
- **When** gửi thay đổi.
- **Then** từ chối, không sửa lịch người khác.

#### Quy tắc nghiệp vụ liên quan

F9; chỉ đặt trong lịch rảnh; chặn lịch trùng PT và lớp nhóm; dữ liệu trainer trỏ staff.

Nguồn: [Bản chốt phạm vi — F9](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `trainer_availabilities`, `staff`, `pt_sessions`, `class_sessions`.
- Phụ thuộc/truy vết: SRS #10; F8 lịch lớp; F9 Trọng Việt.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F9.2 — Đặt buổi PT không trùng lịch

**Mã chức năng:** F9
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F9.2 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Hội viên**, tôi muốn **đặt buổi1-1 trong giờ PT rảnh**, để **sử dụng gói PT hợp lệ**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Hợp lệ**

- **Given** hội viên có gói PT ACTIVE, huấn luyện viên rảnh và còn ít nhất 12 giờ, không lịch trùng.
- **When** đặt PT.
- **Then** giữ buổi mà chưa trừ số buổi của gói.

**Kịch bản 2: Không đủ điều kiện**

- **Given** chỉ có TIME/SESSION, ngoài giờ PT rảnh hoặc sát giờ dưới12 giờ.
- **When** đặt PT.
- **Then** từ chối kèm lý do.

**Kịch bản 3: Đồng thời**

- **Given** hai yêu cầu có thể trùng giờ của PT hoặc hội viên.
- **When** đặt đồng thời.
- **Then** giao dịch có khóa NF6 chỉ nhận lịch không trùng, kể cả với class_sessions.

#### Quy tắc nghiệp vụ liên quan

F9: PT ACTIVE; trước ít nhất12 giờ*; không trùng lịch cả PT và hội viên, kể cả giờ PT dạy lớp; khóa members rồi staff.

Nguồn: [Bản chốt phạm vi — F9](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `pt_sessions`, `trainer_availabilities`, `memberships`, `staff`, `members`.
- Phụ thuộc/truy vết: SRS #10; PT_MIN_LEAD_HOURS=12; NF6; F8.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F9.3 — Xác nhận buổi PT đã diễn ra

**Mã chức năng:** F9
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F9.3 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Huấn luyện viên**, tôi muốn **xác nhận buổi1-1 của mình đã hoàn thành**, để **ghi nhận số buổi gói và bằng chứng nghiệp vụ**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Xác nhận**

- **Given** buổi PT đã diễn ra, PT đúng phân công và gói hợp lệ.
- **When** PT xác nhận.
- **Then** ghi DONE, trừ một buổi và chốt commission_amount theo ERD; giá/tỷ lệ thay đổi về sau không làm đổi số đã chốt.

**Kịch bản 2: Không trừ sớm/lặp**

- **Given** buổi mới đặt hoặc đã được xác nhận.
- **When** chỉ đặt hoặc xác nhận lại.
- **Then** không trừ khi chỉ đặt, không trừ hai lần cho cùng buổi; sai PT bị từ chối.

#### Quy tắc nghiệp vụ liên quan

F9: chỉ trừ gói PT lúc PT xác nhận đã diễn ra. ERD yêu cầu commission_amount có giá trị khi và chỉ khi DONE: tỷ lệ hoa hồng × (giá gói sau giảm / số buổi), làm tròn VND và chốt lúc xác nhận, không đổi về sau. Chỉ phần tính lương/giao diện F13 thuộc v3.

Nguồn: [Bản chốt phạm vi — F9](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `pt_sessions`, `memberships`.
- Phụ thuộc/truy vết: SRS #10; F13 hoa hồng thuộc v3, không mở UI lương trong Sprint 2.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F9.4 — Theo dõi giáo án và tiến độ học viên

**Mã chức năng:** F9
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F9.4 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Huấn luyện viên**, tôi muốn **soạn giáo án, ghi nhật ký tập và chỉ số cơ thể học viên mình dạy**, để **đánh giá tiến bộ qua biểu đồ**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Đúng học viên**

- **Given** PT có quan hệ dạy với hội viên.
- **When** ghi giáo án/nhật ký/chỉ số.
- **Then** lưu đúng học viên và hiển thị tiến độ trên dữ liệu thực.

**Kịch bản 2: Truy cập chéo**

- **Given** PT không dạy hội viên hoặc hội viên yêu cầu tiến độ người khác.
- **When** xem/sửa dữ liệu.
- **Then** backend từ chối theo S3, không thêm gợi ý AI.

#### Quy tắc nghiệp vụ liên quan

F9; kick-off: PT chỉ xem học viên có buổi PT/lớp do mình dạy; hội viên chỉ xem tiến độ cá nhân, không AI gợi ý.

Nguồn: [Bản chốt phạm vi — F9](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `workout_plans`, `workout_logs`, `body_metrics`, `members`.
- Phụ thuộc/truy vết: SRS #10; F2 chỉ số; S3; cần đối chiếu tên bảng ERD khi triển khai.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F10.1 — Nhận nhắc hết hạn gói và lịch lớp

**Mã chức năng:** F10
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F10.1 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Hội viên**, tôi muốn **nhận nhắc gói sắp hết hạn và buổi lớp sắp tới**, để **chủ động gia hạn và đi tập đúng lịch**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Nhắc hợp lệ**

- **Given** hội viên có tài khoản và sự kiện nhắc đến hạn theo cấu hình/đặc tả đã chốt.
- **When** hệ thống gửi nhắc.
- **Then** email và thông báo web tới đúng hội viên với nội dung gói/lịch của họ.

**Kịch bản 2: Không có tài khoản/sai quyền**

- **Given** hồ sơ không có tài khoản hoặc người khác đọc notification.
- **When** gửi email hoặc truy vấn thông báo.
- **Then** không gửi email cho hồ sơ không tài khoản; không cho truy cập thông báo của người khác.

#### Quy tắc nghiệp vụ liên quan

F10: email và thông báo trong web thời gian thực; hội viên không tài khoản không nhận email; S3 chỉ thông báo của mình.

Nguồn: [Bản chốt phạm vi — F10](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `notifications`, `users`, `memberships`, `class_bookings`.
- Phụ thuộc/truy vết: SRS #8 F10; F7 cổng hội viên; thời điểm nhắc cụ thể chưa chốt, không tự đặt.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.


### US-F10.2 — Nhận thông báo thay đổi lớp và được vào lớp

**Mã chức năng:** F10
**Version:** v2
**Story point:** Chưa ước lượng; nhóm chốt trong kế hoạch Sprint 2 (#33).
**Mã truy vết:** US-F10.2 · **Mốc dự kiến:** Sprint 2

#### User story

Là **Hội viên**, tôi muốn **được báo khi tự vào lớp hoặc lớp đổi/hủy**, để **biết trạng thái chỗ và thời gian cần phản hồi**.

#### Tiêu chí chấp nhận

**Kịch bản 1: Được vào lớp**

- **Given** người đứng chờ đủ điều kiện vừa được BOOKED.
- **When** phát sự kiện promotion.
- **Then** thông báo đúng người về chỗ đã có và grace; không gửi lời mời yêu cầu nhận chỗ.

**Kịch bản 2: Ngoài giờ**

- **Given** promotion xảy ra khi chi nhánh đóng cửa.
- **When** gửi thông báo.
- **Then** web ngay, email đợi mở cửa, grace_until tính từ email+15 phút.

**Kịch bản 3: Thay đổi buổi**

- **Given** buổi đổi phòng, đổi giờ hoặc bị hủy.
- **When** gửi sự kiện.
- **Then** báo đúng đối tượng; đổi phòng chỉ báo, đổi giờ nêu hạn reconfirm60 phút, hủy báo cả người có chỗ lẫn chờ.

#### Quy tắc nghiệp vụ liên quan

F8/F10: promotion auto không cần xác nhận; đổi giờ cần reconfirm60 phút*; promotion ngoài giờ gửi web ngay/email giờ mở cửa, grace15 phút tính lúc gửi email.

Nguồn: [Bản chốt phạm vi — F10](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/requirements/ban-chot-pham-vi.md), [ERD v2](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/design/erd/erd-v2.dbml), [quy tắc mã nguồn](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/guides/quy-tac-ma-nguon.md).

#### Ghi chú

- Bảng dữ liệu: `notifications`, `class_bookings`, `class_sessions`, `branches`.
- Phụ thuộc/truy vết: SRS #8; ADR0003; US-F8.4, US-F8.5; WebSocket theo ADR0008.
- Backlog được biên soạn trong #16 (WBS 1.2.4). Đây là yêu cầu để nhóm triển khai, không phải bằng chứng chức năng đã hoàn thành.
- Không thay phân công hiện tại; issue để trống assignee. Sprint 1 chỉ bao gồm phần cơ bản F1–F3 đã giao; tiêu chí bảo mật/phần còn lại chưa triển khai được theo dõi tiếp ở Sprint 2.
