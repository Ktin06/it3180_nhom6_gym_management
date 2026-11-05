-- Nhóm 5: giáo án tập luyện (F9). Nguồn: erd-v2.dbml
-- trainer_id trỏ tới staff (nhóm 7, tạo sau) nên khóa ngoại thêm ở V7_05.
CREATE TABLE workout_plans (
  id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  trainer_id bigint       NOT NULL,
  member_id  bigint       NOT NULL,
  title      varchar(255) NOT NULL,
  content    jsonb,
  start_date date,
  end_date   date,
  created_at timestamptz  NOT NULL DEFAULT now(),
  updated_at timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT fk_workout_plans_member FOREIGN KEY (member_id) REFERENCES members (id)
);
