-- Cho phép phiếu thu 0 đồng: gói được có giá 0 (ck_packages_price >= 0, ví dụ gói tập thử),
-- nên hóa đơn 0 đồng khi xác nhận "đã thu" vẫn phải có một phiếu thu để tra cứu ai xác nhận, lúc nào.
-- Trưởng nhóm chọn phương án C ngày 20/11/2026 (prototype Sprint 2, S2-09.1). Số âm vẫn bị chặn.
ALTER TABLE payments DROP CONSTRAINT ck_payments_amount;
ALTER TABLE payments ADD CONSTRAINT ck_payments_amount CHECK (amount >= 0);
