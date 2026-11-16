-- Đặt chỗ lớp nhóm luôn ghi người đã bấm đặt hoặc vào chờ (hội viên, hoặc lễ tân thao tác hộ).
-- Được tự động đưa từ danh sách chờ vào lớp thì giữ nguyên người tạo ban đầu.
-- Nguồn: ADR 0010, quyết định 16 khi review đặc tả F8 (04/11/2026).
-- Chưa có API F8 nên bảng chưa có dữ liệu; dữ liệu mẫu (db/seed) không tạo dòng class_bookings.

ALTER TABLE class_bookings ALTER COLUMN created_by SET NOT NULL;
