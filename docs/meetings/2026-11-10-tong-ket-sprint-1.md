# Biên bản họp tổng kết Sprint 1 và kế hoạch Sprint 2

- **Ngày họp:** [12/11/2026]
- **Hình thức:** [trực tiếp / trực tuyến], thời lượng [số phút]
- **Điều phối:** Nguyễn Trọng Việt
- **Ghi biên bản:** [họ tên, nên là người khác với người điều phối]
- **Có mặt:** [ ] Hoàng Kim Tiến · [ ] Bùi Trần Đức Việt · [ ] Thái Hùng Lân · [ ] Lê Phước Minh Quân · [ ] Nguyễn Trọng Việt · [ ] Nguyễn Phú Trọng · [ ] Nguyễn Anh Tú · [ ] Phạm Gia Bảo
- **Tài liệu tham chiếu:**
  - [Tiến độ và phân công Sprint 1](../plans/sprint-1-tien-do.md)
  - [Kế hoạch Sprint 2](../plans/sprint-2-ke-hoach.md)
  - [Bản chốt phạm vi](../requirements/ban-chot-pham-vi.md)
  - [ADR 0009: bộ sơ đồ phân tích thiết kế](../decisions/0009-bo-so-do-phan-tich-thiet-ke.md)
  - [Biên bản kick-off 02/11](2026-11-02-kick-off.md)

---

## 1. Mục tiêu cuộc họp

1. Rà kết quả Sprint 1 (27/10 – 16/11) so với kế hoạch.
2. Rút kinh nghiệm (retrospective) và chọn hành động cải tiến.
3. Chốt kế hoạch Sprint 2 (17/11 – 30/11): mỗi việc có người phụ trách và hạn.
4. Chốt câu hỏi "lưu ảnh đại diện ở đâu".

## 2. Chương trình

| Thời lượng | Nội dung | Ai nói |
| --- | --- | --- |
| 20 phút | Sprint review: mỗi người nêu việc đã xong, chưa xong, đang vướng (2 phút mỗi người) | Từng người |
| 5 phút | Kết quả demo Sprint 1 hoặc chuẩn bị demo | Gia Bảo |
| 20 phút | Retrospective: làm tốt, chưa tốt, hành động cải tiến | Cả nhóm |
| 25 phút | Kế hoạch Sprint 2: duyệt từng dòng của bản dự thảo, đổi người hoặc hạn nếu cần | Trọng Việt |
| 10 phút | Câu hỏi mở: lưu ảnh đại diện ở đâu | Hoàng Kim Tiến chốt |
| 5 phút | Đọc lại danh sách việc sau họp (mục 7) | Người ghi biên bản |

## 3. Sprint review: kết quả Sprint 1

Điền theo trạng thái thật trên GitHub (PR đã gộp, issue đã đóng). Dùng ✅ xong, 🟡 xong một phần, ⬜ chưa làm; việc chưa xong ghi rõ chuyển sang Sprint 2 hay bỏ.

**Hoàng Kim Tiến**

- [ ] 1.1.1 Họp kick-off, chốt ERD (02/11): [kết quả]
- [ ] 1.1.3 Ruleset, mời thành viên, nhãn, GitHub Projects, kết nối Vercel và Render: [kết quả]

**Bùi Trần Đức Việt**

- [ ] 1.2.5 Đặc tả F1, F2: [PR, kết quả]
- [ ] 1.3.2 Migration nhóm 1, 2: [PR, kết quả]
- [ ] 1.6.2 Đọc JWT, chặn API theo vai trò: [PR, kết quả]
- [ ] 1.6.1 API đăng ký, đăng nhập: [PR, kết quả]
- [ ] 1.3.10 Kiểm thử ràng buộc cơ sở dữ liệu: [PR, kết quả]

**Thái Hùng Lân**

- [ ] 1.2.6 Đặc tả F3, F5: [PR, kết quả]
- [ ] 1.3.3 Migration nhóm 3: [PR, kết quả]
- [ ] 1.4.4 Wireframe check-in, hóa đơn: [PR, kết quả]
- [ ] 1.7.1 API hội viên: [PR, kết quả]
- [ ] 1.8.1 API danh mục gói: [PR, kết quả]

**Lê Phước Minh Quân**

- [ ] 1.2.3 Sơ đồ use case: [PR, kết quả]
- [ ] 1.2.7 Đặc tả F4, F6: [PR, kết quả]
- [ ] 1.2.11 Sơ đồ hoạt động: [PR, kết quả]
- [ ] 1.3.4 Migration nhóm 4: [PR, kết quả]
- [ ] 1.4.7 Wireframe dashboard: [PR, kết quả]
- [ ] 1.5.6 Mẫu đặc tả use case: [kết quả]

**Nguyễn Trọng Việt**

- [ ] 1.2.8 Đặc tả F7, F10: [PR #85, kết quả]
- [ ] 1.2.10 Đặc tả F9: [PR #86, kết quả]
- [ ] 1.3.5 Migration nhóm 5, 7: [PR, kết quả]
- [ ] 1.4.5 Wireframe cổng hội viên: [PR, kết quả]
- [ ] 1.4.6 Wireframe huấn luyện viên: [PR, kết quả]
- [ ] 1.1.5 Họp tổng kết, kế hoạch Sprint 2: biên bản này

**Nguyễn Phú Trọng**

- [ ] 1.3.6 Migration nhóm 6: [PR, kết quả]
- [ ] 1.3.7 Migration nhóm 8 và khóa ngoại giữa các nhóm: [PR, kết quả]
- [ ] 1.3.8 Rà ràng buộc, chỉ mục, gộp schema vào `main`: [PR, kết quả]
- [ ] 1.3.9 Dữ liệu mẫu: [PR, kết quả]
- [ ] 1.5.2 Cơ sở dữ liệu Neon: [kết quả]
- [ ] 1.9.2 Triển khai staging: [kết quả]
- [ ] 1.9.1 Chạy Postman F1–F3 trên staging: [kết quả]

**Nguyễn Anh Tú**

- [ ] 1.2.1 SRS: [PR, kết quả]
- [ ] 1.2.2 Bảng truy vết: [PR, kết quả]
- [ ] 1.3.11 Xuất ERD, từ điển dữ liệu: [PR, kết quả]
- [ ] 1.4.3 Wireframe hội viên, gói: [PR, kết quả]
- [ ] 1.7.2 Giao diện hội viên: [PR, kết quả]
- [ ] 1.8.2 Giao diện danh mục gói: [PR, kết quả]

**Phạm Gia Bảo**

- [ ] 1.2.4 User story F1–F10: [kết quả]
- [ ] 1.4.1 Quy ước giao diện, wireframe đăng ký: [PR, kết quả]
- [ ] 1.4.2 Wireframe trang khách: [PR, kết quả]
- [ ] 1.2.9 Đặc tả F8: [PR, kết quả]
- [ ] 1.6.3 Trang đăng ký, nối đăng nhập với API: [PR, kết quả]
- [ ] 1.1.4 Slide, gói bàn giao: [kết quả]

**Tổng kết số liệu**

| Chỉ số | Giá trị |
| --- | --- |
| Điểm đã hoàn thành trên tổng 136 | [điền] |
| Số pull request đã gộp | [điền] |
| Việc chuyển sang Sprint 2 | [liệt kê mã WBS] |
| Kết quả demo Sprint 1 | [điền sau khi demo] |

## 4. Retrospective

<!-- Gợi ý để thảo luận, xóa phần này trước khi mở PR. Các điểm dưới đây lấy từ tin nhắn tiến độ của trưởng nhóm ngày 04/11; cả nhóm quyết định điểm nào đáng ghi.
- Migration nhóm 3, 5, 6, 7 trễ hạn 04/11 và các việc API, giao diện, staging phải chờ.
- Chưa pull request nào được Approve; người duyệt chỉ để Comment.
- File đặc tả đặt sai thư mục hoặc tên, còn nhiều chỗ trống chưa điền từ ERD.
- Làm trên web GitHub thay vì nhánh trên máy theo quy ước.
- Dùng công cụ AI: checklist-review.md có giúp bắt lỗi trước khi mở PR không.
- Lịch Sprint 1 phải dời từ 09/11 sang 16/11 và kéo theo lịch Sprint 2.
- Điểm làm tốt: ERD cả nhóm duyệt ngày kick-off, phân công rõ người và hạn, quy ước tài liệu thống nhất.
-->

### 4.1 Làm tốt (giữ lại)

- [điền]

### 4.2 Chưa tốt (cần sửa)

- [điền]

### 4.3 Hành động cải tiến

| Hành động | Người | Hạn |
| --- | --- | --- |
| [điền] | [điền] | [điền] |

## 5. Kế hoạch Sprint 2 (17/11 – 30/11)

Bản dự thảo nằm ở [sprint-2-ke-hoach.md](../plans/sprint-2-ke-hoach.md). Kết quả duyệt tại cuộc họp:

| Nội dung | Kết quả |
| --- | --- |
| Ngày Sprint 2: 17/11 – 30/11, hoặc giữ kết thúc 23/11 và chuyển bớt v2 sang Sprint 3 | [phương án đã chọn] |
| Phạm vi: phần còn lại của v1 và F7–F10 | [giữ nguyên / thay đổi gì] |
| Ba user story chưa có việc ở bản trước (#53, #57, #60), nay là S2-36, S2-37, S2-38 | [giữ như dự thảo / đổi người hoặc chuyển sprint] |
| Phân công đề xuất mới: Phú Trọng hỗ trợ F8 (S2-17) và làm hoàn tiền F5.2 (S2-38); Đức Việt làm API F2.2 (S2-36) | [đồng ý / đổi] |
| Người phụ trách và hạn từng việc còn lại | [giữ như dự thảo / các dòng đã đổi] |
| Sơ đồ còn lại (phân cấp chức năng, tuần tự, lớp phân tích, kiến trúc, lớp thiết kế) | [người và hạn đã chốt] |
| Ngày demo Sprint 1, ngày họp giữa sprint và ngày bàn giao Sprint 2 | [điền] |
| Tạo issue riêng cho từng việc S2-xx sau khi gộp kế hoạch | [người tạo, hạn] |

## 6. Câu hỏi mở: lưu ảnh đại diện ở đâu

Bối cảnh: F4 (check-in tại quầy) hiển thị ảnh đại diện để lễ tân đối chiếu; ổ đĩa của Render bản miễn phí không giữ file sau khi khởi động lại; Sprint 1 làm F2 chưa có ảnh ([thiet-ke-du-lieu.md](../design/erd/thiet-ke-du-lieu.md)). Hạn chốt theo kế hoạch là 12/11 (việc S2-01); nếu đã chốt trước cuộc họp thì ghi quyết định vào cuối mục này.

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Lưu trong PostgreSQL (cột nhị phân, ảnh đã nén nhỏ) | Không thêm dịch vụ; chịu chung phân quyền và sao lưu của backend | Tăng dung lượng cơ sở dữ liệu; phải giới hạn kích thước ảnh |
| Dịch vụ lưu file bên ngoài có gói miễn phí | Không tốn dung lượng cơ sở dữ liệu | Thêm dịch vụ và khóa bí mật; phải kiểm soát ai xem được ảnh; cần đối chiếu điều khoản và hạn mức hiện tại |

Quyết định: [phương án, người ghi ADR, hạn]. Nếu là quyết định kiến trúc thì ghi thêm vào `docs/decisions/` theo mẫu ADR.

## 7. Việc cần làm sau họp

| Việc | Người | Hạn |
| --- | --- | --- |
| [điền] | [điền] | [điền] |

## 8. Lịch sử thay đổi

| Ngày | Thay đổi |
| --- | --- |
| [dd/11/2026] | Biên bản đầu |
