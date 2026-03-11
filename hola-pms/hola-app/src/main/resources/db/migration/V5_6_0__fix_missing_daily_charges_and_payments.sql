-- =============================================
-- V5.6.0: 누락된 일별 요금(daily_charge) 및 결제 정보(payment) 보정
-- 대상: R10, R11, R12, R13, R14 (daily charge 누락)
--       R7, R11, R12 (payment 누락)
--       R8, R10, R13, R14 (payment 금액 0원 → 정상 금액)
-- 참고: R8 daily charge는 앱에서 자동 생성됨 (792000원)
-- 제외: R6(CANCELED), R9(NO_SHOW), R15(CANCELED) - 0원 정상
-- =============================================

-- =============================================
-- 1. 일별 요금 (rsv_daily_charge) INSERT
-- =============================================

-- R8: GMS260307-0001 - daily charge 이미 앱에서 생성 (288000+288000+216000=792000), SKIP

-- R10: GMS260225-0001 (GMS, EARLY, STD-D, 2박: 02/25 수~02/26 목)
INSERT INTO rsv_daily_charge (sub_reservation_id, charge_date, supply_price, tax, service_charge, total, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMS260225-0001-01'), '2026-02-25', 96000.00, 9600.00, 9600.00, 115200.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMS260225-0001-01'), '2026-02-26', 96000.00, 9600.00, 9600.00, 115200.00, NOW());

-- R11-Sub1: OBH260320-0001-01 (OBH, RESORT, DLX-O, 3박: 03/20 금~03/22 일)
INSERT INTO rsv_daily_charge (sub_reservation_id, charge_date, supply_price, tax, service_charge, total, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260320-0001-01'), '2026-03-20', 280000.00, 28000.00, 28000.00, 336000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260320-0001-01'), '2026-03-21', 380000.00, 38000.00, 38000.00, 456000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260320-0001-01'), '2026-03-22', 380000.00, 38000.00, 38000.00, 456000.00, NOW());

-- R11-Sub2: OBH260320-0001-02 (OBH, RESORT, SUI-P, 3박: 03/20 금~03/22 일)
INSERT INTO rsv_daily_charge (sub_reservation_id, charge_date, supply_price, tax, service_charge, total, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260320-0001-02'), '2026-03-20', 280000.00, 28000.00, 28000.00, 336000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260320-0001-02'), '2026-03-21', 380000.00, 38000.00, 38000.00, 456000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260320-0001-02'), '2026-03-22', 380000.00, 38000.00, 38000.00, 456000.00, NOW());

-- R12: OBH260325-0001 (OBH, RACK, DLX-O, 2박: 03/25 수~03/26 목)
INSERT INTO rsv_daily_charge (sub_reservation_id, charge_date, supply_price, tax, service_charge, total, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260325-0001-01'), '2026-03-25', 180000.00, 18000.00, 18000.00, 216000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260325-0001-01'), '2026-03-26', 180000.00, 18000.00, 18000.00, 216000.00, NOW());

-- R13: OBH260306-0001 (OBH, RACK, STD-D, 3박: 03/06 금~03/08 일)
INSERT INTO rsv_daily_charge (sub_reservation_id, charge_date, supply_price, tax, service_charge, total, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260306-0001-01'), '2026-03-06', 180000.00, 18000.00, 18000.00, 216000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260306-0001-01'), '2026-03-07', 250000.00, 25000.00, 25000.00, 300000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260306-0001-01'), '2026-03-08', 250000.00, 25000.00, 25000.00, 300000.00, NOW());

-- R14: OBH260228-0001 (OBH, PKG-BF, DLX-D, 2박: 02/28 토~03/01 일)
INSERT INTO rsv_daily_charge (sub_reservation_id, charge_date, supply_price, tax, service_charge, total, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260228-0001-01'), '2026-02-28', 300000.00, 30000.00, 30000.00, 360000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260228-0001-01'), '2026-03-01', 300000.00, 30000.00, 30000.00, 360000.00, NOW());

-- =============================================
-- 2. 결제 정보 (rsv_reservation_payment) INSERT - 누락 건
-- =============================================

-- R7: GMS260318-0001 (RESERVED OTA, 일별요금 합계: 144000 × 2 = 288000)
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMS260318-0001'),
    'PENDING',
    288000.00, 0.00, 0.00, 0.00, 288000.00,
    NOW(), 'admin'
);

-- R11: OBH260320-0001 (RESERVED, 객실 2496000 + 서비스 100000 = 2596000)
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260320-0001'),
    'PENDING',
    2496000.00, 100000.00, 0.00, 0.00, 2596000.00,
    NOW(), 'admin'
);

-- R12: OBH260325-0001 (RESERVED OTA, 일별요금 합계: 216000 × 2 = 432000)
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260325-0001'),
    'PENDING',
    432000.00, 0.00, 0.00, 0.00, 432000.00,
    NOW(), 'admin'
);

-- =============================================
-- 3. 결제 정보 (rsv_reservation_payment) UPDATE - 0원 → 정상 금액
-- =============================================

-- R8: GMS260307-0001 (INHOUSE, 일별요금 합계: 288000+288000+216000 = 792000, 앱에서 생성된 값)
UPDATE rsv_reservation_payment
SET total_room_amount = 792000.00, grand_total = 792000.00, updated_at = NOW()
WHERE master_reservation_id = (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMS260307-0001');

-- R10: GMS260225-0001 (CHECKED_OUT, 일별요금 합계: 115200 × 2 = 230400)
UPDATE rsv_reservation_payment
SET total_room_amount = 230400.00, grand_total = 230400.00, updated_at = NOW()
WHERE master_reservation_id = (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMS260225-0001');

-- R13: OBH260306-0001 (INHOUSE, 일별요금 합계: 216000+300000+300000 = 816000)
UPDATE rsv_reservation_payment
SET total_room_amount = 816000.00, grand_total = 816000.00, updated_at = NOW()
WHERE master_reservation_id = (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260306-0001');

-- R14: OBH260228-0001 (CHECKED_OUT, 일별요금 합계: 360000 × 2 = 720000)
UPDATE rsv_reservation_payment
SET total_room_amount = 720000.00, grand_total = 720000.00, updated_at = NOW()
WHERE master_reservation_id = (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260228-0001');

-- =============================================
-- 4. 시퀀스 리셋
-- =============================================
SELECT setval('rsv_daily_charge_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM rsv_daily_charge), 1));
SELECT setval('rsv_reservation_payment_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM rsv_reservation_payment), 1));
