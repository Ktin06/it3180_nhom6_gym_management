-- Nhóm 5: nhật ký tập (F9). Nguồn: erd-v2.dbml
CREATE TABLE workout_logs (
  id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  member_id     bigint       NOT NULL,
  pt_session_id bigint,
  logged_at     date         NOT NULL,
  exercise      varchar(100) NOT NULL,
  sets          int,
  reps          int,
  weight        decimal(6,2),
  created_at    timestamptz  NOT NULL DEFAULT now(),
  updated_at    timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT fk_workout_logs_pt_session FOREIGN KEY (pt_session_id) REFERENCES pt_sessions (id)
);
CREATE INDEX ix_workout_logs_member_logged ON workout_logs (member_id, logged_at);
