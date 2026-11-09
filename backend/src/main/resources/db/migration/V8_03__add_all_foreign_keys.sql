-- Tổng hợp khóa ngoại liên nhóm (từ Nhóm 1 đến Nhóm 8)
-- Đây là file cuối cùng trong dải V8_xx

-- Nhóm 1: Tài khoản và hệ thống
ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_logs_branch FOREIGN KEY (branch_id) REFERENCES branches (id);
ALTER TABLE system_configs ADD CONSTRAINT fk_system_configs_branch FOREIGN KEY (branch_id) REFERENCES branches (id);

-- Nhóm 2: Chi nhánh và cơ sở vật chất
ALTER TABLE maintenance_logs ADD CONSTRAINT fk_maintenance_logs_reported_by FOREIGN KEY (reported_by) REFERENCES users (id);

-- Nhóm 3: Hội viên và gói tập
ALTER TABLE members ADD CONSTRAINT fk_members_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE members ADD CONSTRAINT fk_members_home_branch FOREIGN KEY (home_branch_id) REFERENCES branches (id);
ALTER TABLE members ADD CONSTRAINT fk_members_created_by FOREIGN KEY (created_by) REFERENCES users (id);

ALTER TABLE face_embeddings ADD CONSTRAINT fk_face_embeddings_created_by FOREIGN KEY (created_by) REFERENCES users (id);

ALTER TABLE body_metrics ADD CONSTRAINT fk_body_metrics_recorded_by FOREIGN KEY (recorded_by) REFERENCES users (id);

ALTER TABLE packages ADD CONSTRAINT fk_packages_branch FOREIGN KEY (branch_id) REFERENCES branches (id);

ALTER TABLE memberships ADD CONSTRAINT fk_memberships_branch FOREIGN KEY (branch_id) REFERENCES branches (id);
ALTER TABLE memberships ADD CONSTRAINT fk_memberships_sold_branch FOREIGN KEY (sold_branch_id) REFERENCES branches (id);
ALTER TABLE memberships ADD CONSTRAINT fk_memberships_invoice_item FOREIGN KEY (invoice_item_id) REFERENCES invoice_items (id);

ALTER TABLE membership_freezes ADD CONSTRAINT fk_membership_freezes_requested_by FOREIGN KEY (requested_by) REFERENCES users (id);
ALTER TABLE membership_freezes ADD CONSTRAINT fk_membership_freezes_approved_by FOREIGN KEY (approved_by) REFERENCES users (id);

ALTER TABLE membership_transfers ADD CONSTRAINT fk_membership_transfers_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id);
ALTER TABLE membership_transfers ADD CONSTRAINT fk_membership_transfers_approved_by FOREIGN KEY (approved_by) REFERENCES users (id);

-- Nhóm 4: Check-in và lớp học
ALTER TABLE checkins ADD CONSTRAINT fk_checkins_member FOREIGN KEY (member_id) REFERENCES members (id);
ALTER TABLE checkins ADD CONSTRAINT fk_checkins_membership FOREIGN KEY (membership_id) REFERENCES memberships (id);
ALTER TABLE checkins ADD CONSTRAINT fk_checkins_branch FOREIGN KEY (branch_id) REFERENCES branches (id);
ALTER TABLE checkins ADD CONSTRAINT fk_checkins_created_by FOREIGN KEY (created_by) REFERENCES users (id);

ALTER TABLE class_schedules ADD CONSTRAINT fk_class_schedules_branch FOREIGN KEY (branch_id) REFERENCES branches (id);
ALTER TABLE class_schedules ADD CONSTRAINT fk_class_schedules_trainer FOREIGN KEY (trainer_id) REFERENCES staff (id);
ALTER TABLE class_schedules ADD CONSTRAINT fk_class_schedules_room FOREIGN KEY (room_id) REFERENCES rooms (id);

ALTER TABLE class_sessions ADD CONSTRAINT fk_class_sessions_branch FOREIGN KEY (branch_id) REFERENCES branches (id);
ALTER TABLE class_sessions ADD CONSTRAINT fk_class_sessions_trainer FOREIGN KEY (trainer_id) REFERENCES staff (id);
ALTER TABLE class_sessions ADD CONSTRAINT fk_class_sessions_room FOREIGN KEY (room_id) REFERENCES rooms (id);

ALTER TABLE class_bookings ADD CONSTRAINT fk_class_bookings_member FOREIGN KEY (member_id) REFERENCES members (id);
ALTER TABLE class_bookings ADD CONSTRAINT fk_class_bookings_created_by FOREIGN KEY (created_by) REFERENCES users (id);
ALTER TABLE class_bookings ADD CONSTRAINT fk_class_bookings_attendance_marked_by FOREIGN KEY (attendance_marked_by) REFERENCES users (id);

-- Nhóm 5: Huấn luyện viên
ALTER TABLE trainer_availabilities ADD CONSTRAINT fk_trainer_availabilities_branch FOREIGN KEY (branch_id) REFERENCES branches (id);
ALTER TABLE trainer_availabilities ADD CONSTRAINT fk_trainer_availabilities_trainer FOREIGN KEY (trainer_id) REFERENCES staff (id);

ALTER TABLE pt_sessions ADD CONSTRAINT fk_pt_sessions_member FOREIGN KEY (member_id) REFERENCES members (id);
ALTER TABLE pt_sessions ADD CONSTRAINT fk_pt_sessions_membership FOREIGN KEY (membership_id) REFERENCES memberships (id);
ALTER TABLE pt_sessions ADD CONSTRAINT fk_pt_sessions_branch FOREIGN KEY (branch_id) REFERENCES branches (id);
ALTER TABLE pt_sessions ADD CONSTRAINT fk_pt_sessions_created_by FOREIGN KEY (created_by) REFERENCES users (id);
ALTER TABLE pt_sessions ADD CONSTRAINT fk_pt_sessions_trainer FOREIGN KEY (trainer_id) REFERENCES staff (id);

ALTER TABLE workout_plans ADD CONSTRAINT fk_workout_plans_member FOREIGN KEY (member_id) REFERENCES members (id);
ALTER TABLE workout_plans ADD CONSTRAINT fk_workout_plans_trainer FOREIGN KEY (trainer_id) REFERENCES staff (id);

ALTER TABLE workout_logs ADD CONSTRAINT fk_workout_logs_member FOREIGN KEY (member_id) REFERENCES members (id);

-- Nhóm 6: Bán hàng và thanh toán
ALTER TABLE promotions ADD CONSTRAINT fk_promotions_branch FOREIGN KEY (branch_id) REFERENCES branches (id);

ALTER TABLE inventories ADD CONSTRAINT fk_inventories_branch FOREIGN KEY (branch_id) REFERENCES branches (id);

ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movements_branch FOREIGN KEY (branch_id) REFERENCES branches (id);
ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movements_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id);
ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movements_created_by FOREIGN KEY (created_by) REFERENCES users (id);

ALTER TABLE invoices ADD CONSTRAINT fk_invoices_branch FOREIGN KEY (branch_id) REFERENCES branches (id);
ALTER TABLE invoices ADD CONSTRAINT fk_invoices_member FOREIGN KEY (member_id) REFERENCES members (id);
ALTER TABLE invoices ADD CONSTRAINT fk_invoices_created_by FOREIGN KEY (created_by) REFERENCES users (id);
ALTER TABLE invoices ADD CONSTRAINT fk_invoices_refunded_by FOREIGN KEY (refunded_by) REFERENCES users (id);

ALTER TABLE invoice_items ADD CONSTRAINT fk_invoice_items_package FOREIGN KEY (package_id) REFERENCES packages (id);

ALTER TABLE payments ADD CONSTRAINT fk_payments_received_by FOREIGN KEY (received_by) REFERENCES users (id);

-- Nhóm 7: Nhân sự
ALTER TABLE staff ADD CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE staff ADD CONSTRAINT fk_staff_branch FOREIGN KEY (branch_id) REFERENCES branches (id);

ALTER TABLE shifts ADD CONSTRAINT fk_shifts_branch FOREIGN KEY (branch_id) REFERENCES branches (id);
ALTER TABLE shifts ADD CONSTRAINT fk_shifts_created_by FOREIGN KEY (created_by) REFERENCES users (id);

ALTER TABLE payrolls ADD CONSTRAINT fk_payrolls_branch FOREIGN KEY (branch_id) REFERENCES branches (id);

-- Nhóm 8: Gamification
ALTER TABLE member_badges ADD CONSTRAINT fk_member_badges_member FOREIGN KEY (member_id) REFERENCES members (id);
