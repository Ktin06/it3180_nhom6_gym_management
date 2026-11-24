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

-- Bo sung S2-29.1: chi nhanh CN02 va tai khoan demo cho F3-F10.
INSERT INTO branches (code,name,address,phone,open_time,close_time,capacity,status) VALUES
('CN02','Chi nhánh Cầu Giấy','Số 144 Xuân Thủy, Cầu Giấy, Hà Nội','0900000002',TIME '06:00',TIME '22:00',150,'ACTIVE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO users (email,password_hash,role,status) VALUES
('manager2@example.com','${seed_password_hash}','BRANCH_MANAGER','ACTIVE'),
('receptionist2@example.com','${seed_password_hash}','RECEPTIONIST','ACTIVE'),
('member3@example.com','${seed_password_hash}','MEMBER','ACTIVE'),
('member4@example.com','${seed_password_hash}','MEMBER','ACTIVE'),
('member5@example.com','${seed_password_hash}','MEMBER','ACTIVE'),
('member6@example.com','${seed_password_hash}','MEMBER','ACTIVE'),
('member7@example.com','${seed_password_hash}','MEMBER','ACTIVE'),
('member8@example.com','${seed_password_hash}','MEMBER','ACTIVE'),
('member9@example.com','${seed_password_hash}','MEMBER','DISABLED'),
('member11@example.com','${seed_password_hash}','MEMBER','ACTIVE')
ON CONFLICT (email) DO NOTHING;
