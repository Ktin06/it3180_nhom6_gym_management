-- Only run against a disposable database initialized with profile local seed data.
INSERT INTO branches (code, name, open_time, close_time)
VALUES ('CN02', 'Integration branch 2', '06:00', '22:00') ON CONFLICT (code) DO NOTHING;

INSERT INTO members (home_branch_id, member_code, full_name, phone, status)
SELECT id, 'HV888888', 'Integration foreign member', '0988000001', 'ACTIVE'
FROM branches WHERE code = 'CN02' ON CONFLICT DO NOTHING;

INSERT INTO packages (code, name, type, duration_days, session_count, price, status, branch_id)
SELECT 'CN02_ONLY', 'Integration foreign package', 'TIME', 30, NULL, 100000, 'ACTIVE', id
FROM branches WHERE code = 'CN02' ON CONFLICT (code) DO NOTHING;

INSERT INTO packages (code, name, type, duration_days, session_count, price, status, branch_id)
SELECT 'CN01_INACTIVE', 'Integration inactive package', 'SESSION', 30, 5, 100000, 'INACTIVE', id
FROM branches WHERE code = 'CN01' ON CONFLICT (code) DO NOTHING;

INSERT INTO members (home_branch_id, member_code, full_name, phone, status)
SELECT b.id, 'HV887' || lpad(n::text, 3, '0'), 'Integration pagination ' || n,
       '097700' || lpad(n::text, 4, '0'), 'ACTIVE'
FROM branches b CROSS JOIN generate_series(1, 22) n
WHERE b.code = 'CN01' ON CONFLICT DO NOTHING;
