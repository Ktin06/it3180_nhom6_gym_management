-- Khóa ngoại từ các bảng nhóm 5 tới staff (staff tạo ở V7_01, sau nhóm 5).
-- CHỈ nộp file này nếu nhóm chưa có file khóa ngoại riêng cho cột trainer_id.
ALTER TABLE trainer_availabilities
  ADD CONSTRAINT fk_trainer_availabilities_trainer FOREIGN KEY (trainer_id) REFERENCES staff (id);
ALTER TABLE pt_sessions
  ADD CONSTRAINT fk_pt_sessions_trainer FOREIGN KEY (trainer_id) REFERENCES staff (id);
ALTER TABLE workout_plans
  ADD CONSTRAINT fk_workout_plans_trainer FOREIGN KEY (trainer_id) REFERENCES staff (id);
