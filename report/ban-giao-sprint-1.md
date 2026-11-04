# Gói bàn giao Sprint 1 — Web quản lý chuỗi phòng tập Gym

> **Trạng thái tài liệu: DRAFT.** Đây là bản bàn giao theo snapshot `main` tại `446ceb3` và trạng thái GitHub được đọc ngày 04/11/2026. Slide đã áp theme HUST do trưởng nhóm cung cấp ở PR #88 theo chỉ đạo tại #32; bản bàn giao vẫn nháp do các phần SRS/ERD xuất/staging chưa đủ. Tài liệu này không chứng minh hệ thống đã triển khai, có staging, hay đã hoàn tất kiểm thử đầu cuối.

## 1. Tóm tắt cho buổi bàn giao

Nhóm 6 xây dựng một web responsive quản lý phòng tập theo 5 version, 20 chức năng và 6 vai trò. Phạm vi nghiệp vụ đã được chốt trong [bản chốt phạm vi](../docs/requirements/ban-chot-pham-vi.md). ERD bản 2 (39 bảng, 8 nhóm) đã được cả nhóm duyệt tại kick-off; bản chuẩn là DBML, không phải ảnh ERD.

Ở snapshot `main` hiện có khung React/Vite, khung Spring Boot, quy trình CI, tài liệu use case/activity và một wireframe dashboard. Mã nguồn nghiệp vụ vẫn ở giai đoạn khung: đăng nhập giả lập vai trò chỉ dành cho môi trường phát triển; issue nối đăng ký/đăng nhập với API thật còn mở. CI xanh trên commit hiện tại là bằng chứng các job trong CI chạy qua, không phải bằng chứng kiểm thử end-to-end hay demo trên staging.

| Nội dung | Trạng thái tại snapshot |
| --- | --- |
| Phạm vi / ERD | Phạm vi đã chốt; ERD DBML v2 đã duyệt, chờ xuất ảnh ERD và từ điển dữ liệu cho báo cáo |
| Use case / activity | Các sơ đồ use case và 3 sơ đồ activity đã merge; SRS F1, F2, F4, F6 có trong `main` |
| Giao diện mẫu | Wireframe dashboard đã merge; PR #78 về wireframe công khai đang mở, chưa thuộc `main`; CI xanh, đang chờ review |
| Mã ứng dụng | Khung FE/BE và CI; nhiều API, migration và luồng UI nghiệp vụ còn là đầu việc |
| Kiểm chứng | CI trên `main` xanh; chưa có bằng chứng Postman trên staging hoặc E2E |
| Triển khai | Chưa có URL staging thật; đích rewrite trong Vercel vẫn là placeholder `.invalid` |

## 2. Phạm vi đã duyệt

Nguồn chuẩn là [bản chốt phạm vi](../docs/requirements/ban-chot-pham-vi.md), được ghi nhận tại [biên bản kick-off 02/11](../docs/meetings/2026-11-02-kick-off.md). Dự án kéo dài 4 sprint, từ 27/10 đến 21/12/2026; bàn giao Sprint 1 dự kiến ngày 10/11/2026.

| Version | Chức năng | Cam kết |
| --- | --- | --- |
| v1 | F1–F6: tài khoản/phân quyền, hồ sơ hội viên, gói tập, check-in tại quầy, thanh toán tại quầy, dashboard | Bắt buộc |
| v2 | F7–F10: cổng hội viên, lớp học nhóm, huấn luyện viên, thông báo | Bắt buộc |
| v3 | F11–F17: thiết bị, POS/kho, nhân sự/lương, khuyến mãi, bảo lưu/chuyển nhượng, báo cáo nâng cao, audit log | Mục tiêu |
| v4 | F18–F19: đa chi nhánh, gamification theo quy tắc | Mục tiêu; cắt thứ hai nếu trễ |
| v5 | F20: check-in bằng nhận diện khuôn mặt | Cắt đầu tiên nếu trễ |

Hệ thống có **6 vai trò**: Chủ chuỗi, Quản lý chi nhánh, Lễ tân, Huấn luyện viên, Hội viên và Khách. Năm vai trò đầu đăng nhập; Khách dùng trang công khai. Từ v1 đến v3 dùng một chi nhánh mặc định; quản lý nhiều chi nhánh thuộc v4.

## 3. Một số yêu cầu bảo mật và chất lượng đã duyệt

Các mã dưới đây giữ đúng nội dung trong bản chốt phạm vi:

- **S1 — Mật khẩu:** băm bằng bcrypt; ít nhất 8 ký tự, gồm chữ và số.
- **S2 — Phiên đăng nhập:** access token JWT hết hạn sau 15 phút; refresh token nằm trong cookie `HttpOnly`.
- **S3 — Phân quyền:** backend kiểm tra vai trò và quyền sở hữu dữ liệu trên mọi API; hội viên không xem được dữ liệu hội viên khác.
- **NF4 — Giao diện:** dùng tốt từ màn hình rộng 360 px; hỗ trợ Chrome, Edge và Firefox phiên bản mới.
- **NF6 — Toàn vẹn đồng thời:** đặt chỗ lớp và đặt lịch PT dùng giao dịch CSDL có khóa bản ghi; không vượt sĩ số hoặc tạo lịch trùng khi nhiều người đặt đồng thời.

## 4. Kiến trúc và dữ liệu

### Kiến trúc đích

Theo [ADR 0008](../docs/decisions/0008-goi-api-cung-domain.md), trình duyệt gọi đường dẫn tương đối `/api/v1` cùng domain. Ở local, Vite proxy sang `localhost:8080`; khi deploy, Vercel rewrite sang backend Render. Backend Spring Boot làm việc với PostgreSQL. Đây là **kiến trúc mục tiêu**, không phải sơ đồ triển khai đang chạy. WebSocket F10 là kết nối riêng; không trình bày như đã có dịch vụ production.

### Mô hình dữ liệu

ERD v2 có **39 bảng, 8 nhóm**:

1. Tài khoản và hệ thống.
2. Chi nhánh và cơ sở vật chất.
3. Hội viên và gói tập.
4. Check-in và lớp học.
5. Huấn luyện viên.
6. Bán hàng và thanh toán.
7. Nhân sự.
8. Gamification.

Trạng thái và loại dữ liệu trong CSDL vật lý lưu bằng `varchar` kèm `CHECK`; các khối Enum trong DBML biểu diễn tập giá trị logic. [DBML ERD v2](../docs/design/erd/erd-v2.dbml) là nguồn chuẩn. [Issue #15](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/15) theo dõi phần xuất hình ERD và từ điển dữ liệu; chưa có ảnh ERD để đưa vào tài liệu này nên không dựng ảnh thay thế.

## 5. Tài liệu thiết kế đã có và phần còn thiếu

| Hạng mục | Bằng chứng có trong `main` | Ghi chú |
| --- | --- | --- |
| Use case | [Tổng quát](../docs/design/use-case/so-do-use-case-tong-quat.drawio.svg), 6 sơ đồ actor: [Chủ chuỗi](../docs/design/use-case/use-case-chu-chuoi.drawio.svg), [Hội viên](../docs/design/use-case/use-case-hoi-vien.drawio.svg), [Huấn luyện viên](../docs/design/use-case/use-case-huan-luyen-vien.drawio.svg), [Khách](../docs/design/use-case/use-case-khach.drawio.svg), [Lễ tân](../docs/design/use-case/use-case-le-tan.drawio.svg), [Quản lý chi nhánh](../docs/design/use-case/use-case-quan-ly-chi-nhanh.drawio.svg) | PR [#38](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/38) đã merge |
| Activity | [Check-in](../docs/design/activity/so-do-hoat-dong-check-in.drawio.svg), [danh sách chờ](../docs/design/activity/so-do-hoat-dong-danh-sach-cho.drawio.svg), [đặt lịch PT](../docs/design/activity/so-do-hoat-dong-dat-lich-pt.drawio.svg) | PR [#41](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/41) đã merge |
| SRS use case | [F1](../docs/requirements/srs/dac-ta-use-case-f1-tai-khoan-va-phan-quyen.md), [F2](../docs/requirements/srs/dac-ta-use-case-f2-ho-so-hoi-vien.md), [F4](../docs/requirements/srs/dac-ta-use-case-f4-check-in-tai-quay.md), [F6](../docs/requirements/srs/dac-ta-use-case-f6-dashboard-co-ban.md) | F4/F6 có PR [#40](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/40), [#44](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/44), [#45](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/45) đã merge; tổng quan SRS [#13](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/13) còn mở; SRS F8 đang ở PR draft [#79](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79) |
| Wireframe | [Dashboard cơ bản](../docs/design/wireframe/dashboard-co-ban.drawio.svg) | PR [#42](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/42) đã merge; PR [#78](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/78) về trang công khai đang mở, CI xanh, review được yêu cầu nhưng chưa có review/merge; không nhập nội dung nhánh chưa merge vào báo cáo |

Các sơ đồ use case, activity, ERD và wireframe mô tả thiết kế; chúng không chứng minh giao diện/API tương ứng đã được lập trình.

### Nguồn minh họa wireframe trong slide

Hai minh họa rút gọn được viết trực tiếp bằng TikZ trong `slide-sprint-1.tex`, biên dịch cùng slide; không cần PDF/PNG xuất sẵn hay tải tài nguyên từ mạng. Đây là hình phục vụ trình chiếu, không thay các wireframe chuẩn chỉnh sửa bằng Draw.io ở PR #78 và không phải ảnh giao diện đang chạy.

| Minh họa | Wireframe chuẩn tham chiếu (PR #78 chưa merge) |
| --- | --- |
| Bố cục đăng ký mobile 360 px | [dang-ky.drawio.svg @ 464a31a](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/464a31a41b78820e41b5657a8e6d39f06903fc47/docs/design/wireframe/dang-ky.drawio.svg) |
| Bảng lịch công khai; tên lớp/giờ chỉ là dữ liệu mẫu | [lich-lop-cong-khai.drawio.svg @ 464a31a](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/464a31a41b78820e41b5657a8e6d39f06903fc47/docs/design/wireframe/lich-lop-cong-khai.drawio.svg) |

**Cập nhật 04/11/2026:** bỏ PDF và hai PNG wireframe xuất sẵn theo yêu cầu chỉ lưu mã nguồn. Sau đó áp theme HUST được trưởng nhóm cung cấp; ảnh nguồn của theme được giữ riêng theo chỉ đạo, không phải ảnh xuất wireframe. Snapshot/bằng chứng bên dưới được đối chiếu lại ngày 04/11 và vẫn cần cập nhật trước demo.

## 6. Trạng thái mã nguồn và bằng chứng kiểm tra

Snapshot đối chiếu cho tài liệu này là `main` commit `446ceb3`. Phần triển khai hiện thấy trong cây mã:

- Frontend có router/layout, trang đăng nhập/điều hướng và công tắc giả lập vai trò chỉ xuất hiện ở chế độ dev. Chưa có trang đăng ký nối backend.
- Backend có Spring Boot, cấu hình bảo mật, xử lý lỗi chung và cấu trúc gói; hiện chưa có feature API hội viên/gói tập trong cây nguồn.
- Thư mục migration mới có hướng dẫn; chưa có migration nghiệp vụ trên `main` snapshot.
- [Issue #30](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/30) về giao diện đăng ký và nối đăng nhập với API thật còn mở. Không mô tả issue này như tính năng đã hoàn tất.

**Bằng chứng CI:** [CI run #41772233543](https://github.com/Ktin06/it3180_nhom6_gym_management/actions/runs/41772233543) hoàn tất `success` cho commit `446ceb3`. Điều này xác nhận các job CI của snapshot chạy thành công; không xác nhận Postman, E2E, deploy hoặc toàn bộ quy tắc nghiệp vụ.

**Backlog và tài liệu:** [#77](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/77) đã sửa theo review của Ktin06: Issues #46–#76 là nguồn chuẩn, file chỉ giữ chỉ mục 31 story và đã chuyển Ready (`df27aa2`), chưa merge. [#78](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/78) giữ Ready/chờ `natus26`, không có góp ý mới; [#79](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79) đã sửa review (`ccae3cf`) nhưng còn Draft do quyết định nghiệp vụ F8. CI từng PR xanh: [#77 run 41844424169](https://github.com/Ktin06/it3180_nhom6_gym_management/actions/runs/41844424169), [#78 run 41643707041](https://github.com/Ktin06/it3180_nhom6_gym_management/actions/runs/41643707041), [#79 run 41846335262](https://github.com/Ktin06/it3180_nhom6_gym_management/actions/runs/41846335262). Chưa có phê duyệt/merge; không coi là nội dung đã có trên main. Projects vẫn chưa xác minh được do scope token.

### Frontend F1 ngoài snapshot: PR nháp #80

PR [#80](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/80) chưa merge. Các commit `adbddcb` (phiên), `02037ce` (đăng ký), `27693ab` (tests) và `74fc936` (sửa DTO theo backend) bổ sung form, adapter/token memory và menu theo role. Main vẫn giữ công cụ giả lập vai trò để đồng đội phát triển UI.

Kiểm tra local ngày 04/11 trên `74fc936`: lint qua, **9 file/45 Vitest qua**, build TypeScript/Vite qua; còn cảnh báo JS **968.56 kB** vượt 500 kB. [CI run 41847056463](https://github.com/Ktin06/it3180_nhom6_gym_management/actions/runs/41847056463) xanh; Frontend chạy lint/Vitest/build. Kiểm tra Chromium với API mock ở 360/768/1280 px thuộc baseline `27693ab` ngày 03/11, không chạy lại trong sửa DTO.

[Đức Việt đã xác nhận hợp đồng](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/80#issuecomment-6624574276): login/register/refresh trả `{accessToken}`, `/me` trả `{userId,email,role}`, gender tùy chọn MALE/FEMALE/OTHER/null; backend tự chuẩn hóa số điện thoại. SRS mục 6.1 nằm trên PR backend #87 chưa merge. **Giữ Draft theo trưởng nhóm đến khi #25/#26 merge và kiểm tra HTTP thật**: đăng ký, login, refresh, logout, role và cookie. Mock tests/CI chưa chứng minh backend/cookie thật hoạt động.

## 7. Ma trận bàn giao và việc còn lại

Phân công dưới đây giữ nguyên [kế hoạch Sprint 1](../docs/plans/sprint-1-tien-do.md), không thay người phụ trách.

| Hạng mục | Trạng thái / bằng chứng | Người phụ trách theo kế hoạch | Việc tiếp theo / phụ thuộc |
| --- | --- | --- | --- |
| Phạm vi và quyết định | Phạm vi đã chốt; biên bản kick-off và ERD được ghi nhận trong PR [#34](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/34) đã merge | Hoàng Kim Tiến điều phối; cả nhóm duyệt ERD | Giữ bản chốt phạm vi là nguồn chuẩn khi cập nhật SRS |
| Use case / activity | PR #38 và #41 đã merge; các SRS F1/F2/F4/F6 đang có trong main; SRS F8 ở PR draft [#79](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79), CI run #41846335262 xanh | Minh Quân: sơ đồ/activity; tác giả SRS theo từng chức năng; Gia Bảo: F8 theo Sprint Plan | Review/merge PR #79; hoàn thiện SRS tổng quan #13 và phần còn lại theo kế hoạch |
| ERD xuất bản | DBML 39 bảng/8 nhóm đã duyệt; [issue #15](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/15) còn mở | Nguyễn Anh Tú | Xuất hình ERD và từ điển dữ liệu, đối chiếu với DBML canonical |
| User story F1–F10 | 31 issue #46–#76; PR #77 Ready, Issues là bản chuẩn, chỉ mục 31 story; CI run #41844424169 xanh | Phạm Gia Bảo | Chờ review/merge; xác minh board khi quyền Projects sẵn sàng |
| Wireframe | Dashboard ở PR #42 đã merge; PR [#78](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/78) mở, CI run #41643707041 xanh, đang chờ review; issue [#17](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/17) còn mở | Gia Bảo: trang khách/đăng ký; các wireframe khác giữ người trong Sprint Plan | Review/merge PR #78; hoàn thiện các wireframe F2/F3, check-in/hóa đơn, cổng hội viên, trainer theo kế hoạch |
| Schema / API | Migration, API F1–F3 và tích hợp được giao theo issues [#22](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/22), [#25](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/25), [#26](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/26), [#28](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/28), [#29](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/29); chưa được coi là có trên main snapshot | Chủ issue/WBS hiện tại trong Sprint Plan | Hoàn tất schema chung và API thật trước khi kết nối UI; chạy kiểm thử theo quy trình nhóm |
| FE đăng ký/đăng nhập | PR nháp [#80](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/80), chưa merge; form/token/role + 45 Vitest qua, browser mock ở baseline 03/11; #30 còn mở | Phạm Gia Bảo; phụ thuộc API/auth của Bùi Trần Đức Việt | DTO đã chốt; chờ #25/#26 merge, tích hợp HTTP/cookie backend thật, staging; role simulator chỉ bị thay khi PR được merge |
| Staging / Postman | Issue [#31](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/31) còn mở; chưa có URL staging thật | Nguyễn Phú Trọng triển khai và chạy Postman; Hoàng Kim Tiến hỗ trợ nối Vercel/Render | Tạo Neon/Render/Vercel, thay placeholder `.invalid`, xác nhận biến môi trường, sau đó mới ghi URL và kết quả Postman |
| Slide / bàn giao | Tài liệu này và `slide-sprint-1.tex` là bản draft; đã áp mẫu HUST theo [chỉ đạo của trưởng nhóm](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/32#issuecomment-6633041875) | Phạm Gia Bảo, hạn theo kế hoạch 09/11 | Theme HUST đã áp dụng từ PR #88; cập nhật snapshot/bằng chứng trước lần trình bày cuối |

**Giữ nguyên phân công kỹ thuật theo Sprint Plan:** migration nhóm 1/2 — Bùi Trần Đức Việt; nhóm 3 — Thái Hùng Lân; nhóm 4 — Lê Phước Minh Quân; nhóm 5/7 — Nguyễn Trọng Việt; nhóm 6/8, khóa ngoại và gộp schema — Nguyễn Phú Trọng. API F1 — Đức Việt; API F2/F3 — Lân. Các issue schema/API [#22](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/22), [#25](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/25), [#26](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/26), [#28](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/28), [#29](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/29) cần tiếp tục theo đúng chủ issue/WBS tương ứng.

## 8. Giới hạn cần nói rõ khi trình bày

1. **Chưa có staging URL.** `frontend/vercel.json` hiện dùng domain `.invalid` làm placeholder; lần kiểm tra không thấy deployment được công bố, `repository.homepage` trống và GitHub Pages trả 404. Issue #31 chưa có comment cung cấp link staging. Không ghi đây là môi trường đang chạy và không đưa URL giả vào slide như link demo.
2. **Chưa có kiểm thử đầu-cuối được báo cáo.** CI thành công chỉ là CI; issue #31 theo dõi triển khai và Postman F1–F3.
3. **Chưa có hình ERD xuất từ bản chuẩn.** Dùng liên kết DBML và nói rõ issue #15 còn mở; không tự vẽ một sơ đồ khác rồi gọi là ERD được duyệt.
4. **Các nhánh/PR đang mở không phải `main`.** Tại lúc kiểm tra, PR #77 (chỉ mục story) Ready, #79 (SRS F8) và #80 (frontend F1) là Draft; #78 (wireframe công khai) Ready. Các PR đã được kiểm tra CI xanh, vẫn cần phê duyệt/merge. Chỉ trình bày chúng như phần đang chờ review.
5. **Theme HUST đã áp dụng.** Trưởng nhóm cung cấp ở PR #88 và cho phép lấy source trước merge tại comment #32; slide dùng bộ xanh 16:9. DRAFT còn giữ vì các tài liệu/bằng chứng bàn giao chưa đủ, không phải vì thiếu theme.
6. **S1–S3/NF4/NF6 là yêu cầu đã duyệt, không phải chứng cứ đã đáp ứng.** Chỉ trình bày tiêu chí và chỉ rõ việc triển khai/kiểm chứng còn lại.
7. **Audit dependency:** kết quả `npm ci` baseline ghi nhận 13 moderate advisories, đều thuộc tooling ESLint/minimatch; không có high/critical. Không thay dependency trong nhánh tài liệu này; nhóm xem xét theo quy trình riêng.

## 9. Trình tự đề xuất cho rehearsal

1. Nêu bài toán, 6 vai trò, 5 version; nhấn v1/v2 là bắt buộc, v5 có thể cắt đầu tiên nếu trễ.
2. Đi qua S1/S2/S3/NF4/NF6 như tiêu chí thiết kế và kiểm thử, không tuyên bố đã đạt.
3. Mở các sơ đồ và wireframe đã merge; phân biệt rõ tài liệu thiết kế với chức năng chạy được.
4. Cho xem mã khung và CI green nếu được hỏi về tiến độ kỹ thuật; nói rõ chưa có staging/Postman/E2E.
5. Chốt các mốc còn lại: hoàn tất migrations/API, SRS và ERD xuất bản, UI, staging/Postman, rồi cập nhật slide từ bằng chứng mới nhất.

### Nguồn theme HUST

Theo [chỉ đạo của Ktin06 tại #32](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/32#issuecomment-6633041875), áp mẫu trong [PR #88](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/88) ngay khi PR đó chưa merge. Lấy ba tệp đang sử dụng từ commit `dca1fc4e3707c03854ba080e67ae334b690b3bb7`:

- [`mau-slide-hust/beamerthemeHUST.sty`](mau-slide-hust/beamerthemeHUST.sty).
- [`mau-slide-hust/images/hust-blue-169-title.png`](mau-slide-hust/images/hust-blue-169-title.png) — nền trang bìa.
- [`mau-slide-hust/images/hust-blue-169-content-full.png`](mau-slide-hust/images/hust-blue-169-content-full.png) — nền nội dung, thanh mảnh.

Đây là **ảnh nguồn của mẫu do trưởng nhóm cung cấp**, không phải PDF/PNG xuất từ slide/wireframe. Giữ nguyên logic theme và hai ảnh nguồn; `.sty` chỉ bỏ khoảng trắng cuối dòng trống 168 và thêm xuống dòng cuối tệp để qua `git diff --check`, giữ attribution của người cung cấp. Không nhập commit/backend/nội dung ví dụ từ nhánh #88. Slide đã tự chứa các nguồn cần build, không cần checkout PR #88; Nếu PR #88 được merge từ commit này, hai ảnh nguồn trùng nội dung; `.sty` chỉ khác chuẩn hóa khoảng trắng đã nêu. `git merge-tree` với nhánh #88 hiện báo một xung đột thêm/thêm ở `.sty` do chuẩn hóa khoảng trắng này; worktree hiện tại không có xung đột và build vẫn qua. Sau khi #88 merge, cần đồng bộ `origin/main` vào nhánh #81 và đối chiếu lại `.sty`, giữ nguyên logic theme.

Các kiểu nền khác của theme không dùng ở deck này; nếu đổi kiểu, lấy thêm đúng ảnh nguồn tương ứng từ bộ mẫu.

### Biên dịch slide

Nguồn: [`slide-sprint-1.tex`](slide-sprint-1.tex), gồm hai minh họa TikZ và theme HUST nêu trên. Cần XeLaTeX hoặc Tectonic, các gói Beamer/fontspec/TikZ/booktabs/tabularx/array và font Noto Sans. Không phụ thuộc ảnh sinh sẵn hoặc nội dung một nhánh chưa merge để build.

```bash
# Từ thư mục gốc repository; output nằm trong thư mục bị bỏ qua bởi Git.
mkdir -p report/build
xelatex -interaction=nonstopmode -halt-on-error -output-directory=report/build report/slide-sprint-1.tex
xelatex -interaction=nonstopmode -halt-on-error -output-directory=report/build report/slide-sprint-1.tex

# Hoặc dùng Tectonic (tự lấy gói TeX vào cache ở lần chạy đầu).
tectonic --outdir report/build report/slide-sprint-1.tex
```

Lần chạy XeLaTeX thứ hai cập nhật số trang và liên kết. Mở `report/build/slide-sprint-1.pdf` để xem; PDF, PNG và tệp phụ trợ là sản phẩm build, không đưa vào Git. `report/.gitignore` bỏ qua build/PDF/PNG sinh ra; chỉ cho phép đúng hai ảnh nguồn HUST đang dùng. Bản PDF nộp cuối do trưởng nhóm đính kèm vào GitHub Release, không commit vào nhánh.

### Kiểm tra sản phẩm bản nháp

Đã biên dịch bản nguồn ngày 04/11 bằng Tectonic 0.17.0 với Noto Sans, kiểm tra PDF 15 trang render và xem đủ 15 trang sau khi đổi theme để rà soát. Kết quả chỉ xác nhận tài liệu build và mở được; chưa thay thế nghiệm thu chức năng hoặc bộ bàn giao đầy đủ. Các output chỉ lưu local trong `report/build/`.
