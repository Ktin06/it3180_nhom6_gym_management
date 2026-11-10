-- Demo ban hang, thanh toan va goi TIME duoc kich hoat.
INSERT INTO products (sku,name,price,unit) VALUES
('SP01','Nước khoáng 500ml',10000,'Chai'),('SP02','Khăn tắm',50000,'Cái')
ON CONFLICT (sku) DO NOTHING;
INSERT INTO inventories (branch_id,product_id,quantity,min_quantity)
SELECT b.id,p.id,100,10 FROM branches b CROSS JOIN products p
WHERE b.code='CN01' AND p.sku IN ('SP01','SP02')
ON CONFLICT (branch_id,product_id) DO NOTHING;
INSERT INTO promotions (branch_id,code,type,value,max_discount,min_amount,start_at,end_at,usage_limit)
SELECT b.id,'OPENING20','PERCENT',20,100000,0,now(),now()+INTERVAL '30 days',100
FROM branches b WHERE b.code='CN01' ON CONFLICT (code) DO NOTHING;

INSERT INTO invoices (code,branch_id,member_id,created_by,source,subtotal,discount,total,status,paid_at)
SELECT 'INV2611001',b.id,m.id,u.id,'COUNTER',p.price,0,p.price,'PAID',now()
FROM branches b JOIN members m ON m.home_branch_id=b.id
JOIN users u ON u.email='receptionist@example.com' JOIN packages p ON p.code='TIME_1M'
WHERE b.code='CN01' AND m.member_code='HV999001' ON CONFLICT (code) DO NOTHING;

INSERT INTO invoice_items (invoice_id,item_type,package_id,description,quantity,unit_price,amount,discount_amount)
SELECT i.id,'PACKAGE',p.id,p.name,1,p.price,p.price,0 FROM invoices i JOIN packages p ON p.code='TIME_1M'
WHERE i.code='INV2611001' AND NOT EXISTS
(SELECT 1 FROM invoice_items ii WHERE ii.invoice_id=i.id AND ii.item_type='PACKAGE' AND ii.package_id=p.id);

INSERT INTO payments (invoice_id,method,amount,status,received_by,paid_at)
SELECT i.id,'CASH',i.total,'CONFIRMED',u.id,i.paid_at FROM invoices i
JOIN users u ON u.email='receptionist@example.com'
WHERE i.code='INV2611001' AND i.status='PAID' AND i.paid_at IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM payments pay WHERE pay.invoice_id=i.id);

INSERT INTO memberships (member_id,package_id,package_type,branch_id,sold_branch_id,invoice_item_id,
start_date,end_date,total_sessions,remaining_sessions,status)
SELECT m.id,p.id,p.type,b.id,b.id,ii.id,
(i.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date,
(i.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date + p.duration_days - 1,
NULL,NULL,'ACTIVE'
FROM invoices i JOIN invoice_items ii ON ii.invoice_id=i.id
JOIN packages p ON p.id=ii.package_id JOIN members m ON m.id=i.member_id
JOIN branches b ON b.id=i.branch_id
WHERE i.code='INV2611001' AND ii.item_type='PACKAGE' AND p.code='TIME_1M' AND p.type='TIME'
AND i.status='PAID' AND EXISTS
(SELECT 1 FROM payments pay WHERE pay.invoice_id=i.id AND pay.status='CONFIRMED' AND pay.amount=i.total)
AND NOT EXISTS (SELECT 1 FROM memberships ms WHERE ms.invoice_item_id=ii.id);
