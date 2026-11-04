# Chỉ mục user story F1–F10

- **Người biên soạn:** Phạm Gia Bảo (GamBug).
- **Nhiệm vụ:** #16 · WBS 1.2.4 · cập nhật 04/11/2026.
- **Nguồn chuẩn:** [Bản chốt phạm vi](ban-chot-pham-vi.md), [ERD v2](../design/erd/erd-v2.dbml), [quy tắc mã nguồn](../guides/quy-tac-ma-nguon.md), [kick-off](../meetings/2026-11-02-kick-off.md), [phân công Sprint 1](../plans/sprint-1-tien-do.md).
- Tuân theo mẫu `.github/ISSUE_TEMPLATE/user-story.md`. Story point chưa ước lượng; không gán lại người làm. Các task #5–#33 đã có được tham chiếu, không tạo bản sao task.
- 31 story; nội dung và số tiêu chí chấp nhận hiện hành được duy trì trong từng issue. Nhãn dùng nhãn hiện có: `type:feature`, `F<n>`, `v1`/`v2`; milestone theo #16: phần cơ bản F1–F3 ở Sprint 1, còn lại Sprint 2. Mốc là kế hoạch, không xác nhận đã triển khai.
- Bảng này chỉ truy vết **story → issue → chức năng/phụ thuộc**, không thay bảng chức năng–use case–dữ liệu do Anh Tú phụ trách ở #14.
- Projects: chưa xác minh được việc thêm/di chuyển từng story; cần người có quyền kiểm tra bảng. Trưởng nhóm xác nhận auto-add issue vào Todo tại [#3](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/3); cần kiểm tra lại trên bảng bằng tài khoản có quyền, không coi auto-add đã được kiểm chứng cho các issue dưới đây.
- Giá trị nghiệp vụ chưa được nguồn chuẩn định nghĩa (ví dụ thời hạn reset, số ngày sinh buổi trước, ranh giới ân hạn/tháng phạt, hủy WAITLIST) cần trưởng nhóm Hoàng Kim Tiến xác nhận; không tạo giá trị mới trong backlog.

**Nội dung và tiêu chí chấp nhận xem trong issue tương ứng; issue là bản chuẩn.** File này chỉ giữ quy ước và chỉ mục, không chép lại nội dung story để tránh hai bản lệch nhau. Các cập nhật nghiệp vụ sau ADR 0005, bao gồm #56, #58, #61, #72 và #73, được giữ tại các issue tương ứng.

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
