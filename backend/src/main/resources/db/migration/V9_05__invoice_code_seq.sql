-- S2-07 (#182): V9_04 dành cho ảnh đại diện ở PR #250; không sửa migration đã gộp.
-- Mã hóa đơn tự sinh dùng dải HD000001...HD999999; mã dữ liệu cũ giữ nguyên.
CREATE SEQUENCE invoice_code_seq START WITH 1 INCREMENT BY 1;
