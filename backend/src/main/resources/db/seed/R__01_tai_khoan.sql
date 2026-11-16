-- Tai khoan demo; bcrypt tu Flyway placeholder.
INSERT INTO users (email,password_hash,role,status) VALUES
('admin@example.com','${seed_password_hash}','SUPER_ADMIN','ACTIVE'),
('manager@example.com','${seed_password_hash}','BRANCH_MANAGER','ACTIVE'),
('receptionist@example.com','${seed_password_hash}','RECEPTIONIST','ACTIVE'),
('trainer1@example.com','${seed_password_hash}','TRAINER','ACTIVE'),
('trainer2@example.com','${seed_password_hash}','TRAINER','ACTIVE'),
('member1@example.com','${seed_password_hash}','MEMBER','ACTIVE'),
('member2@example.com','${seed_password_hash}','MEMBER','ACTIVE')
ON CONFLICT (email) DO NOTHING;
