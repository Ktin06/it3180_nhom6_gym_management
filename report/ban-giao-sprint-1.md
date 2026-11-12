# Bàn giao Sprint 1 — Hệ thống quản lý chuỗi phòng tập Gym

**DRAFT · Cập nhật 12/11/2026.** Slide và tài liệu này dùng snapshot main `336e13c`, sau khi #77, #95, #96, #98, #101, #102, #106, #97, #78, #105 và #107 đã merge. Các PR chưa merge được ghi riêng. Frontend F1 (#80, Draft) đã được thử cục bộ với backend `336e13c` và PostgreSQL thật (24/24 HTTP, 5/5 phân quyền, 32/32 trình duyệt); Postman F1–F3 trên staging do trưởng nhóm chạy ngày 12/11 (mục 4). HTTPS của #80 chưa kiểm chứng vì preview bị Vercel SSO chặn. CI không thay thế các kiểm chứng này. Demo ngày 10/11; Sprint 1 kết thúc ngày 09/11.

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
| Wireframe | Dashboard #42, F2/F3 #89, cổng hội viên/PT #102, check-in/hóa đơn/VietQR F4/F5 #106 và trang công khai/đăng ký #78 đã merge; [thư mục](../docs/design/wireframe/) |

[DBML](../docs/design/erd/erd-v2.dbml) là nguồn chuẩn dữ liệu. Bản xuất dbdiagram PDF/PNG và các hình vector theo nhóm đã sẵn sàng; không còn coi #13/#15 là thiếu sản phẩm. Slide dùng PDF nhóm3 từ bộ #96 đã duyệt, không tự vẽ lại schema hoặc thêm bản xuất khác. Thu nhỏ trên slide chỉ phục vụ cấu trúc chung; mở vector/từ điển để đọc chi tiết. Schema vật lý #101 đã merge ngày 09/11; DBML trên main vẫn là nguồn chuẩn, không suy ra migration từ ảnh ERD.

## 3. Trạng thái triển khai và kiểm chứng

| Phần | Trạng thái thật tại snapshot |
| --- | --- |
| Frontend main | Router/layout và công cụ giả lập role cho phát triển; luồng auth của #80 (Draft) chưa vào main |
| Backend main | JWT/phân quyền #87, đăng ký/login/refresh/logout #91, API hồ sơ hội viên F2 #97, vá JWT #105 (10/11) và API danh mục gói F3 #107 (11/11) đã merge |
| Schema | Schema 39 bảng #101 đã merge; trên PostgreSQL 16 cục bộ, Flyway áp 45 migration (gồm dữ liệu mẫu) và tạo 39 bảng |
| F2 / F3 | F2 #97 đã merge ngày 10/11; đăng ký ghi hồ sơ hội viên bền vững (kiểm chứng cục bộ qua #80). API danh mục gói F3 #107 đã merge ngày 11/11 (issue #29 đã đóng) |
| Frontend F1 | #80 Draft: form, token trong bộ nhớ, refresh/me, menu theo role, phục hồi khi `/me`, logout hoặc refresh lỗi. Đã thử với backend và CSDL thật cục bộ (24/24 HTTP, 32/32 trình duyệt); HTTPS trên preview chưa kiểm chứng vì bị Vercel SSO chặn |
| Frontend F2/F3 | #103 Draft, chưa coi là đã tích hợp backend |

**Bằng chứng có thể kiểm tra:**

- Main `872386d`: [CI 42771961483](https://github.com/Ktin06/it3180_nhom6_gym_management/actions/runs/42771961483) SUCCESS. Job Backend (Maven, Testcontainers) chạy thật; job Frontend bỏ qua vì không đổi frontend.
- Frontend #80 tại `b0e87a7`: [CI 42797835483](https://github.com/Ktin06/it3180_nhom6_gym_management/actions/runs/42797835483) chạy lint, kiểm thử và build qua; cục bộ ESLint, kiểm tra kiểu và 10 file/54 Vitest qua. Vitest dùng API giả lập ở ranh giới frontend, không chứng minh backend thật.
- **Tích hợp cục bộ #80 (10/11): 15/15 đạt.** Môi trường: Chromium qua Playwright; backend đúng mã main `872386d` đóng gói bằng `backend/Dockerfile`, profile `local` có dữ liệu mẫu; PostgreSQL 16 (`postgres:16-alpine`). Không giả lập API, trừ một ca lai ghi rõ bên dưới. Đã kiểm chứng: đăng ký tạo `users` và `members` bền vững; đăng nhập năm vai trò dữ liệu mẫu vào đúng trang chủ; tải lại giữ phiên; đăng xuất thu hồi phiên (refresh bằng cookie cũ trả 401, 0 token còn hiệu lực); cookie refresh `HttpOnly`, `SameSite=Strict`, `Path=/api/v1/auth`, Web Storage rỗng; trùng SĐT và trùng email bị từ chối; không tràn ngang ở 360 px; phục hồi khi backend bị dừng thật (đăng xuất và tải lại đều trả 502 rồi thử lại thành công). Ca lai: đăng ký thật nhưng `/me` bị chặn bằng công cụ kiểm thử, thử lại không đăng ký lần hai. Chi tiết trong [guide tích hợp tại nguồn ghim](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/b0e87a7ec46e32318de72827854673199f66bedb/docs/guides/tich-hop-frontend-f1.md).
- **Phát hiện khi thử:** mở frontend bằng `http://localhost:5173`; origin `127.0.0.1` bị backend trả `403 Invalid CORS request` do `CORS_ALLOWED_ORIGINS` mặc định.
- Slide nguồn snapshot 10/11 đã biên dịch local và được rà hiển thị toàn bộ 17 trang; chi tiết ở mục 6. Postman F1–F3 trên staging đã có kết quả do trưởng nhóm báo ngày 12/11 (mục 4). Chưa có kiểm tra cookie `Secure` qua HTTPS hoặc báo cáo nghiệm thu NF/S để tuyên bố đã đạt.

## 4. Staging và phụ thuộc tích hợp

Trưởng nhóm [công bố staging ngày05/11](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/31#issuecomment-6653959733): [frontend Vercel](https://it3180-nhom6-gym.vercel.app), [backend Render](https://it3180-nhom6-gym-management.onrender.com). Phần Neon được trưởng nhóm xác nhận đã tự thực hiện trong cùng bình luận.

**GET kiểm tra lúc 10:06 UTC ngày 10/11:** frontend HTTP 200; `/api/v1/auth/me` qua Vercel không phản hồi trong 20 giây; Render `/actuator/health` không phản hồi trong 60 giây, chưa xác nhận UP. Không kết luận backend chết từ timeout vì Render Free có thể khởi động chậm. Không tạo công cụ gọi định kỳ để giữ thức.

| Phụ thuộc | Trạng thái và việc còn lại |
| --- | --- |
| #101 — schema | Đã merge 09/11 |
| #97 — F2 | Đã merge 10/11 |
| #99 — proxy | Đã đóng, không merge; `frontend/vercel.json` trên main đã trỏ tới Render qua #101 |
| #105 — vá JWT | Đã merge 10/11 |
| #107 — API F3 (issue #29) | Đã merge 11/11; issue #29 đã đóng |
| #31 — staging/Postman | Issue đã đóng. Trưởng nhóm báo ngày 12/11: Postman F1–F3 chạy bằng newman trên backend Render của main mới nhất, F1 9/11 kiểm tra đạt (6 request), F2 25/26 (20 request), F3 31/34 (34 request). Các bước không đạt do dữ liệu staging: tài khoản thử đã tồn tại, thiếu chi nhánh thứ hai, và một biến môi trường đặt sẵn. Kết quả này chưa được tái chạy độc lập |

Hợp đồng F1 đã được Đức Việt xác nhận: `{accessToken}` và `/me` `{userId,email,role}`; gender optional MALE/FEMALE/OTHER/null. #80 giữ Draft theo chỉ đạo của trưởng nhóm cho đến khi kiểm chứng được HTTPS trên preview; phần còn thiếu là một preview của nhánh #80 truy cập được (hiện bị Vercel SSO chặn). Xem [guide tích hợp của #80 tại nguồn ghim](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/b0e87a7ec46e32318de72827854673199f66bedb/docs/guides/tich-hop-frontend-f1.md).

## 5. Review còn lại và phân công bàn giao

| Hạng mục | Trạng thái / bước tiếp theo | Người theo phân công |
| --- | --- | --- |
| Stories | #77 đã được natus26 duyệt và đồng đội merge5bcfaa5; #16 đã đóng | Gia Bảo |
| #78 wireframe | Đã merge 10/11 (`896c1aa`). Ba file F4/F5 của Lân đã vào main qua #106 | Gia Bảo; Lân sở hữu #19 |
| #79 F8 | Tiến đã chốt [cách tính lại vi phạm khi sửa điểm danh ngày 09/11](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79#issuecomment-6734172961). Đã cập nhật đến `ec3d331`: kịch bản 14 do trưởng nhóm chốt 12/11, liên kết wireframe lịch công khai (#78) ở UC-F8.0; đang chờ Tú approve. Đồng bộ ADR/ERD do trưởng nhóm giao người trong buổi lập kế hoạch Sprint 2; thiết kế kỹ thuật thuộc backend F8 Sprint 2 | Gia Bảo đặc tả; trưởng nhóm giao người đồng bộ nguồn; backend F8 Sprint 2 |
| #80 UI auth | Đã sửa ba nhánh lỗi/phục hồi theo [review](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/80#issuecomment-6690299880), đồng bộ main `336e13c` (`093fa55`), CI xanh; đã thử tích hợp cục bộ với backend thật (mục 3). Còn preview HTTPS truy cập được (Vercel SSO đang chặn); không thay backend của đồng đội | Gia Bảo; phối hợp Đức Việt/Tiến (Vercel) |
| #103 UI F2/F3 | Review code/AC/CI trên head hiện hành; không tự duyệt/merge khi có blocker | Anh Tú làm, Gia Bảo review |
| Triển khai/Postman | #31 đã đóng; Postman F1–F3 trên staging có kết quả ngày 12/11 (mục 4), chưa tái chạy độc lập | Phú Trọng; Kim Tiến |
| Slide/rehearsal | Deck và tài liệu này cập nhật theo snapshot 12/11; còn rehearsal và kiểm chứng HTTPS của #80 | Gia Bảo, hạn 09/11 |

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

Kiểm tra 10/11: Tectonic 0.17/Noto Sans xuất PDF 17 trang vào thư mục tạm; `pdfinfo` đọc được, không có cảnh báo overfull/underfull hay thiếu glyph, đã render và xem toàn bộ 17 trang. Lượt đầu phát hiện trang 14 (bằng chứng) tràn xuống logo chân trang do nội dung mới; đã rút gọn, giảm cỡ chữ và dựng lại, rồi xem lại các trang 13–17. Cũng sửa hai lỗi thiếu dấu cách có từ bản cũ. Cảnh báo hệ thống về đường dẫn font vẫn còn; không thay theme của trưởng nhóm để che cảnh báo.

Output PDF/PNG/tệp phụ trợ không đưa vào Git; [report/.gitignore](.gitignore) vẫn bảo vệ output. PDF cuối do trưởng nhóm đính kèm GitHub Release, không commit. Giữ #81 Draft vì còn kiểm chứng tích hợp/staging/Postman và review/rehearsal cuối; không phải vì thiếu theme/SRS/ERD.
