# Bàn giao Sprint 1 — Hệ thống quản lý chuỗi phòng tập Gym

**DRAFT · Cập nhật 07/11/2026.** Slide và tài liệu này dùng snapshot main `e9baa7730de80889586ae5067513787122c4de32`, sau khi #77, #95, #96, #98 và #102 đã merge. Các PR chưa merge được ghi riêng. Chưa có bằng chứng nghiệm thu tích hợp HTTP/cookie hoặc Postman F1–F3; CI không thay thế chúng. Demo dự kiến 10/11, kết thúc Sprint 1 ngày 09/11.

## 1. Phạm vi và yêu cầu đã duyệt

Nguồn chuẩn: [bản chốt phạm vi](../docs/requirements/ban-chot-pham-vi.md), [kick-off](../docs/meetings/2026-11-02-kick-off.md), [SRS tổng quan](../docs/requirements/srs/srs-tong-quan.md). Không có khảo sát thực địa; SRS phân biệt mô hình As-Is với dữ liệu khảo sát.

| Version | Phạm vi | Cam kết |
| --- | --- | --- |
| v1 | F1–F6: tài khoản, hội viên, gói tập, check-in, thanh toán tại quầy, dashboard | Bắt buộc |
| v2 | F7–F10: cổng hội viên, lớp nhóm, huấn luyện viên, thông báo | Bắt buộc |
| v3 | F11–F17: thiết bị, POS/kho, nhân sự/lương, khuyến mãi, bảo lưu/chuyển nhượng, báo cáo, nhật ký | Mục tiêu |
| v4 | F18–F19: đa chi nhánh, gamification theo quy tắc | Cắt sau v5 nếu trễ |
| v5 | F20: nhận diện khuôn mặt tại quầy | Cắt đầu tiên nếu trễ |

Sáu vai trò sử dụng: chủ chuỗi, quản lý chi nhánh, lễ tân, huấn luyện viên, hội viên và khách. Năm vai trò có tài khoản; khách dùng trang công khai. v1–v3 vận hành một chi nhánh mặc định. Các ngưỡng NF1–NF6 và S1–S10 được giữ trong SRS tổng quan, không sao chép thành nguồn chuẩn thứ hai. Đặc biệt: bcrypt/S1; JWT15phút và refresh HttpOnly/S2; phân quyền backend/S3; responsive360px/NF4; giao dịch/khóa chống vượt sĩ số và trùng lịch/NF6 là yêu cầu, chưa phải kết quả đo.

## 2. Kiến trúc, ERD và tài liệu đã có trên main

Theo [ADR0008](../docs/decisions/0008-goi-api-cung-domain.md), trình duyệt gọi `/api/v1` cùng domain, qua Vite proxy ở local hoặc Vercel rewrite tới Render; Spring Boot dùng PostgreSQL/JPA/Flyway. Thời điểm lưu UTC, nghiệp vụ ngày theo `Asia/Ho_Chi_Minh`. WebSocket F10 vẫn thuộc kiến trúc đích, không nhận là đã triển khai.

| Sản phẩm | Bằng chứng đã merge |
| --- | --- |
| User story | [#77](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/77), chỉ mục [31 story](../docs/requirements/user-stories-f1-f10.md); Issues #46–#76 là nguồn nội dung/AC. Trưởng nhóm đã xác nhận đủ31story trên Projects tại comment16#6633043096 |
| SRS tổng quan | [#95](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/95), bài toán/As-Is có nguồn,20F/5version/6vai trò/NF/S |
| SRS chi tiết | F1–F7, F9, F10 trong [thư mục SRS](../docs/requirements/srs/); F8 chưa merge |
| Bảng truy vết | [#98](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/98), [56UC và39bảng](../docs/requirements/srs/bang-truy-vet-yeu-cau.md). Chín UC F8 vẫn ghim bản nháp5233c53; rà lại sau khi #79 cập nhật/merge |
| ERD và từ điển | [#96](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/96), [hướng dẫn/nguồn xuất](../docs/design/erd/xuat-ban-erd.md), [từ điển](../docs/design/erd/tu-dien-du-lieu.md):39bảng/422cột/89quan hệ |
| Use case / activity | [Use case](../docs/design/use-case/) và [activity](../docs/design/activity/), #38/#41 đã merge |
| Wireframe | Dashboard#42, F2/F3#89 và cổng hội viên/PT#102 đã merge; [thư mục](../docs/design/wireframe/). #78 trang công khai/đăng ký vẫn review |

[DBML](../docs/design/erd/erd-v2.dbml) là nguồn chuẩn dữ liệu. Bản xuất dbdiagram PDF/PNG và các hình vector theo nhóm đã sẵn sàng; không còn coi #13/#15 là thiếu sản phẩm. Slide dùng PDF nhóm3 từ bộ #96 đã duyệt, không tự vẽ lại schema hoặc thêm bản xuất khác. Thu nhỏ trên slide chỉ phục vụ cấu trúc chung; mở vector/từ điển để đọc chi tiết. Bản schema vật lý #101 chưa merge; không suy ra migration đã chạy từ ảnh ERD. Không dùng DBML của nhánh schema chưa duyệt thay bản main.

## 3. Trạng thái triển khai và kiểm chứng

| Phần | Trạng thái thật tại snapshot |
| --- | --- |
| Frontend main | Router/layout và công cụ giả lập role cho phát triển; chưa có luồng auth #80 |
| Backend main | JWT/phân quyền #87 và đăng ký/login/refresh/logout #91 đã merge; `DefaultMemberService` còn in-memory |
| Schema | Main có hai migration auth; schema39bảng #101 chưa merge |
| F2 | #97 chưa merge. F1 đã bổ sung service vào nhánh đó tại03bab42; adapter/migration còn review. Không nhập nhánh chưa duyệt để nghiệm thu frontend |
| Frontend F1 | #80 Draft; có form, token memory, refresh/me và menu theo role; đang xử lý review về đăng ký201/me lỗi, logout lỗi và refresh tạm thời |
| Frontend F2/F3 | #103 đang review, chưa coi là đã tích hợp backend |

**Bằng chứng có thể kiểm tra:**

- Main e9baa77: [CI42345472272](https://github.com/Ktin06/it3180_nhom6_gym_management/actions/runs/42345472272) SUCCESS; jobs ứng dụng bỏ qua vì docs-only. Không gọi đây là chạy lại unit/E2E.
- Baseline frontend #80 tại199a929: [CI42221658657](https://github.com/Ktin06/it3180_nhom6_gym_management/actions/runs/42221658657) thực sự chạy ESLint,9file/45Vitest và TypeScript/Vite build qua ngày06/11; bundle968.56kB còn cảnh báo. Test dùng API giả lập ở ranh giới frontend, không chứng minh cookie/backend thật.
- Kiểm tra trình duyệt với API mock tại360/768/1280px thuộc baseline27693ab ngày03/11; không coi là bằng chứng bản auth mới đã thử backend.
- Slide nguồn snapshot07/11 đã biên dịch local và được rà hiển thị toàn bộ17trang; chi tiết kiểm tra ở mục6. Không có Postman/E2E hoặc báo cáo nghiệm thu NF/S mới để tuyên bố đã đạt.

## 4. Staging và phụ thuộc tích hợp

Trưởng nhóm [công bố staging ngày05/11](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/31#issuecomment-6653959733): [frontend Vercel](https://it3180-nhom6-gym.vercel.app), [backend Render](https://it3180-nhom6-gym-management.onrender.com). Phần Neon được trưởng nhóm xác nhận đã tự thực hiện trong cùng bình luận.

**GET kiểm tra ngày07/11:** frontend HTTP200; `/api/v1/auth/me` qua Vercel HTTP502; Render `/actuator/health` hết thời gian chờ20giây, chưa xác nhận UP. Không kết luận backend chết từ timeout vì Render Free có thể khởi động chậm. Không tạo công cụ gọi định kỳ để giữ thức.

| Phụ thuộc | Việc của chủ phần việc trước nghiệm thu |
| --- | --- |
| #101 — schema | [Review07/11](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/101#issuecomment-6702165875): bốn CHECK còn thiếu trong migration, bốn chỗ cú pháp DBML và placeholder dữ liệu mẫu. Không sửa schema/biến môi trường thay người phụ trách |
| #97 — F2 | [Review](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/97#issuecomment-6702340970), [F1 bàn giao03bab42](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/97#issuecomment-6703078410): service F1 đã bổ sung trên nhánh, còn nối adapter trực tiếp, đồng bộ migration và các mục review F2. Chưa merge main |
| #99 — proxy | [Review](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/99#issuecomment-6685208195): destination phải giữ `/api/:path*`, tách schema sau#101; main còn đích.invalid |
| #80 / #31 | Sau khi phụ thuộc được duyệt: thử đăng ký ghi member bền vững, login, refresh/tải lại, logout, role, cookie qua proxy; PostmanF1–F3 trên staging. Ghi môi trường, commit, thời gian và kết quả |

Hợp đồng F1 đã được Đức Việt xác nhận: `{accessToken}` và `/me` `{userId,email,role}`; gender optional MALE/FEMALE/OTHER/null. Giữ #80 Draft tới khi có kiểm chứng thật. Xem [guide tích hợp của #80 tại nguồn ghim](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/199a9291e3aae8cfa061cadeb6b3eb960b4caa14/docs/guides/tich-hop-frontend-f1.md); phần chẩn đoán19:53 ngày06/11 trong đó là lịch sử, bảng trên ghi tiến độ mới07/11.

## 5. Review còn lại và phân công bàn giao

| Hạng mục | Trạng thái / bước tiếp theo | Người theo phân công |
| --- | --- | --- |
| Stories | #77 đã được natus26 duyệt và đồng đội merge5bcfaa5; #16 đã đóng | Gia Bảo |
| #78 wireframe | Gender đã sửa4f54950; phối hợp Lân tách3fileF4/F5 thuộc#19, chưa tự xóa hay nhận phần việc của Lân | Gia Bảo; Lân sở hữu#19 |
| #79 F8 | Trưởng nhóm đã [trả lời hai câu hỏi07/11](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6701988493): sửa sức chứa buổi chưa bắt đầu tính BOOKED+PENDING_RECONFIRM; sửa điểm danh tính lại và xử lý khóa ngay trong giao dịch, lưu người sửa/audit. Đang cập nhật luồng/AC; còn đồng bộ ADR/ERD và thiết kế kỹ thuật Sprint2 | Gia Bảo đặc tả; trưởng nhóm/người phụ trách nguồn; backendF8 Sprint2 |
| #80 UI auth | Sửa3nhánh lỗi/phục hồi theo [review](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/80#issuecomment-6690299880); không thay backend của đồng đội | Gia Bảo; phối hợp Đức Việt/Lân |
| #103 UI F2/F3 | Review code/AC/CI trên head hiện hành; không tự duyệt/merge khi có blocker | Anh Tú làm, Gia Bảo review |
| Triển khai/Postman | #31 còn mở, proxy/schema/API và bằng chứng nghiệm thu còn chờ | Phú Trọng; Kim Tiến đã dựng Neon theo xác nhận |
| Slide/rehearsal | Deck hiện đã cập nhật07/11; hoàn thiện evidence cuối và rehearsal trước demo | Gia Bảo, hạn09/11 |

Schema theo Sprint Plan: nhóm1/2 Đức Việt; nhóm3 Lân; nhóm4 Minh Quân; nhóm5/7 Trọng Việt; nhóm6/8 và khóa ngoại Phú Trọng. API F1 Đức Việt, F2/F3 Lân. Tài liệu này không thay phân công hoặc cho phép sửa phần người khác.

## 6. Nguồn slide, cách build và kiểm tra

Nguồn: [slide-sprint-1.tex](slide-sprint-1.tex). Theme HUST theo [chỉ đạo tại#32](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/32#issuecomment-6633041875), đã merge qua#88. `.sty` giữ logic/attribution; chỉ chuẩn hóa whitespace dòng168 và EOF để giải quyết add/add trước đây. Hai nền xanh16:9 đang dùng giữ nguyên byte từ nguồn dca1fc4; bộ mẫu đầy đủ trên main được bảo toàn.

- [Theme](mau-slide-hust/beamerthemeHUST.sty), [nền bìa](mau-slide-hust/images/hust-blue-169-title.png), [nền nội dung](mau-slide-hust/images/hust-blue-169-content-full.png) là tài nguyên nguồn, không phải output slide.
- Hai minh họa đăng ký/lịch dùng TikZ inline, tham chiếu wireframe Draw.io ở#78/464a31a; không phải ảnh UI chạy thật.
- Hình ERD dùng [PDF nhóm3 đã duyệt](../docs/design/erd/hinh/nhom-3.pdf) của#96, kèm link nguồn/từ điển; không sao chép hoặc sinh thêm file nhị phân vào PR slide.
- Cần XeLaTeX hoặc Tectonic, Beamer/fontspec/TikZ/booktabs/tabularx/array và font Noto Sans. Có thể build từ gốc repo hoặc thư mụcreport; đường dẫn nguồn có nhánh tương ứng.

```bash
# Từ gốc repository, xuất vào thư mục tạm để không lẫn với nguồn.
mkdir -p /tmp/gym-slide-check
tectonic --outdir /tmp/gym-slide-check report/slide-sprint-1.tex
# Hoặc XeLaTeX, chạy hai lượt để cập nhật số trang/liên kết.
xelatex -interaction=nonstopmode -halt-on-error -output-directory=/tmp/gym-slide-check report/slide-sprint-1.tex
xelatex -interaction=nonstopmode -halt-on-error -output-directory=/tmp/gym-slide-check report/slide-sprint-1.tex
```

Kiểm tra07/11: Tectonic0.17/NotoSans xuất PDF17trang vào `/tmp/gym-slide-check-261107`; `pdfinfo` đọc được, đã render và rà toàn bộ17trang. Lượt đầu bắt lỗi TeX xuống dòng trước dấu `[` và fragment URL chưa escape, đã sửa rồi build lại. Rà hình phát hiện nguồn slide7 chạm vùng logo, đã giảm cỡ chữ để tách rõ; chỉ kết quả build/rà cuối được dùng để bàn giao. Cảnh báo hệ thống về font và tên package theme còn được ghi nhận; không thay theme của trưởng nhóm để che cảnh báo.

Output PDF/PNG/tệp phụ trợ không đưa vào Git; [report/.gitignore](.gitignore) vẫn bảo vệ output. PDF cuối do trưởng nhóm đính kèm GitHub Release, không commit. Giữ #81 Draft vì còn kiểm chứng tích hợp/staging/Postman và review/rehearsal cuối; không phải vì thiếu theme/SRS/ERD.
