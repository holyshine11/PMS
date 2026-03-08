-- =============================================
-- V5.3.0: 예약 모듈 테스트 데이터 등록
-- 총 15건의 예약 (GMP 6건, GMS 4건, OBH 5건)
-- 상태: RESERVED, CHECK_IN, INHOUSE, CHECKED_OUT, CANCELED, NO_SHOW
-- 전제: V5_0_0 (기초 데이터), V5_1_0 (객실 데이터), V5_2_0 (레이트 데이터)
-- =============================================

-- =============================================
-- R1: GMP - RESERVED - 김예약 (2026-03-15 ~ 2026-03-17, 2박)
-- =============================================

-- 마스터 예약
INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_name_ko, guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, birth_date, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    customer_request, created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'GMP'),
    'GMP260315-0001', 'ABCD1234', 'RESERVED',
    '2026-03-15', '2026-03-17', '2026-03-05 10:00:00',
    '김예약', 'Yeyak', 'Kim',
    '+82', '010-1111-1001', 'yeyak.kim@example.com', '1985-05-15', 'M', 'KR',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'RACK' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'FIT'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND channel_code = 'WALK_IN'),
    '조용한 객실 요청', NOW(), 'admin'
);

-- 예약번호 시퀀스
INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'GMP'), '2026-03-15', 1);

-- 서브 예약: STD-D, 11F, 1101
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260315-0001'),
    'GMP260315-0001-01', 'RESERVED',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'STD-D'),
    (SELECT id FROM htl_floor WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND floor_number = '11F'),
    (SELECT id FROM htl_room_number WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1101'),
    2, 0, '2026-03-15', '2026-03-17', NOW(), 'admin'
);

-- 투숙객
INSERT INTO rsv_reservation_guest (sub_reservation_id, guest_seq, guest_name_ko, guest_first_name_en, guest_last_name_en, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260315-0001-01'), 1, '김예약', 'Yeyak', 'Kim', NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260315-0001-01'), 2, '이동행', 'Donghaeng', 'Lee', NOW());

-- 일별 요금 (2박)
INSERT INTO rsv_daily_charge (sub_reservation_id, charge_date, supply_price, tax, service_charge, total, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260315-0001-01'), '2026-03-15', 150000.00, 15000.00, 15000.00, 180000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260315-0001-01'), '2026-03-16', 150000.00, 15000.00, 15000.00, 180000.00, NOW());

-- 예치금
INSERT INTO rsv_reservation_deposit (
    master_reservation_id, deposit_method, card_company, card_number_encrypted,
    card_cvc_encrypted, card_expiry_date, card_password_encrypted,
    currency, amount, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260315-0001'),
    'CREDIT_CARD', '삼성카드', 'ENC_4111111111111111', 'ENC_123', '12/2028', 'ENC_00',
    'KRW', 360000.00, NOW(), 'admin'
);

-- 결제 정보
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260315-0001'),
    'PENDING',
    360000.00, 0.00, 30000.00, 0.00, 390000.00,
    NOW(), 'admin'
);

-- 메모
INSERT INTO rsv_reservation_memo (master_reservation_id, content, created_at, created_by)
VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260315-0001'),
    '조용한 객실 요청', NOW(), 'admin'
);

-- =============================================
-- R2: GMP - RESERVED - 박비즈 (2026-03-20 ~ 2026-03-22, 2박, 2객실)
-- =============================================

INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_name_ko, guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    customer_request, created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'GMP'),
    'GMP260320-0001', 'EFGH5678', 'RESERVED',
    '2026-03-20', '2026-03-22', '2026-03-06 14:30:00',
    '박비즈', 'Biz', 'Park',
    '+82', '010-1111-1002', 'biz.park@corp.com', 'M', 'KR',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'CORP-SP' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'CORP'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND channel_code = 'PHONE'),
    '기업 출장 - 별도 영수증 요청', NOW(), 'admin'
);

INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'GMP'), '2026-03-20', 1);

-- 서브1: STD-S (객실 미배정)
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, sort_order, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260320-0001'),
    'GMP260320-0001-01', 'RESERVED',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'STD-S'),
    NULL, NULL,
    1, 0, '2026-03-20', '2026-03-22', 1, NOW(), 'admin'
);

-- 서브2: DLX-T (객실 미배정)
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, sort_order, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260320-0001'),
    'GMP260320-0001-02', 'RESERVED',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'DLX-T'),
    NULL, NULL,
    1, 0, '2026-03-20', '2026-03-22', 2, NOW(), 'admin'
);

-- 투숙객 (서브1: 박비즈, 서브2: 이기업)
INSERT INTO rsv_reservation_guest (sub_reservation_id, guest_seq, guest_name_ko, guest_first_name_en, guest_last_name_en, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260320-0001-01'), 1, '박비즈', 'Biz', 'Park', NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260320-0001-02'), 1, '이기업', 'Gieop', 'Lee', NOW());

-- 일별 요금: STD-S (2박)
INSERT INTO rsv_daily_charge (sub_reservation_id, charge_date, supply_price, tax, service_charge, total, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260320-0001-01'), '2026-03-20', 105000.00, 10500.00, 10500.00, 126000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260320-0001-01'), '2026-03-21', 105000.00, 10500.00, 10500.00, 126000.00, NOW());

-- 일별 요금: DLX-T (2박)
INSERT INTO rsv_daily_charge (sub_reservation_id, charge_date, supply_price, tax, service_charge, total, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260320-0001-02'), '2026-03-20', 150000.00, 15000.00, 15000.00, 180000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260320-0001-02'), '2026-03-21', 150000.00, 15000.00, 15000.00, 180000.00, NOW());

-- 예치금
INSERT INTO rsv_reservation_deposit (
    master_reservation_id, deposit_method, card_company, card_number_encrypted,
    card_cvc_encrypted, card_expiry_date, card_password_encrypted,
    currency, amount, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260320-0001'),
    'CREDIT_CARD', '현대카드', 'ENC_4222222222222222', 'ENC_456', '06/2029', 'ENC_00',
    'KRW', 612000.00, NOW(), 'admin'
);

-- 결제 정보 (STD-S 252000 + DLX-T 360000 = 612000)
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260320-0001'),
    'PENDING',
    612000.00, 0.00, 0.00, 0.00, 612000.00,
    NOW(), 'admin'
);

-- 메모
INSERT INTO rsv_reservation_memo (master_reservation_id, content, created_at, created_by)
VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260320-0001'),
    '기업 출장 - 별도 영수증 요청', NOW(), 'admin'
);

-- =============================================
-- R3: GMP - CHECK_IN - 최체크 (2026-03-08 ~ 2026-03-10, 2박)
-- =============================================

INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_name_ko, guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, birth_date, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'GMP'),
    'GMP260308-0001', 'IJKL9012', 'CHECK_IN',
    '2026-03-08', '2026-03-10', '2026-03-01 09:15:00',
    '최체크', 'Check', 'Choi',
    '+82', '010-1111-1003', 'check.choi@example.com', '1990-11-20', 'M', 'KR',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'RACK' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'FIT'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND channel_code = 'WEBSITE'),
    NOW(), 'admin'
);

INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'GMP'), '2026-03-08', 1);

-- 서브: DLX-D, 13F, 1301, CHECK_IN
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260308-0001'),
    'GMP260308-0001-01', 'CHECK_IN',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'DLX-D'),
    (SELECT id FROM htl_floor WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND floor_number = '13F'),
    (SELECT id FROM htl_room_number WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1301'),
    2, 1, '2026-03-08', '2026-03-10', NOW(), 'admin'
);

-- 투숙객 (3명: 성인2 + 아이1)
INSERT INTO rsv_reservation_guest (sub_reservation_id, guest_seq, guest_name_ko, guest_first_name_en, guest_last_name_en, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260308-0001-01'), 1, '최체크', 'Check', 'Choi', NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260308-0001-01'), 2, '최동행', 'Donghaeng', 'Choi', NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260308-0001-01'), 3, '최아이', 'Ai', 'Choi', NOW());

-- 일별 요금: 3/8(토) 주말, 3/9(일) 평일
INSERT INTO rsv_daily_charge (sub_reservation_id, charge_date, supply_price, tax, service_charge, total, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260308-0001-01'), '2026-03-08', 200000.00, 20000.00, 20000.00, 240000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260308-0001-01'), '2026-03-09', 150000.00, 15000.00, 15000.00, 180000.00, NOW());

-- 결제 정보 (240000 + 180000 = 420000)
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260308-0001'),
    'PENDING',
    420000.00, 0.00, 0.00, 0.00, 420000.00,
    NOW(), 'admin'
);

-- =============================================
-- R4: GMP - INHOUSE - 정인하 (2026-03-07 ~ 2026-03-09, 2박)
-- =============================================

INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_name_ko, guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, birth_date, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'GMP'),
    'GMP260307-0001', 'MNOP3456', 'INHOUSE',
    '2026-03-07', '2026-03-09', '2026-02-28 16:00:00',
    '정인하', 'Inha', 'Jung',
    '+82', '010-1111-1004', 'inha.jung@example.com', '1988-07-22', 'F', 'KR',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'FIT'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND channel_code = 'EMAIL'),
    NOW(), 'admin'
);

INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'GMP'), '2026-03-07', 1);

-- 서브: SUI-R, 15F, 1501, INHOUSE
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260307-0001'),
    'GMP260307-0001-01', 'INHOUSE',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'SUI-R'),
    (SELECT id FROM htl_floor WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND floor_number = '15F'),
    (SELECT id FROM htl_room_number WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1501'),
    2, 0, '2026-03-07', '2026-03-09', NOW(), 'admin'
);

-- 투숙객
INSERT INTO rsv_reservation_guest (sub_reservation_id, guest_seq, guest_name_ko, guest_first_name_en, guest_last_name_en, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260307-0001-01'), 1, '정인하', 'Inha', 'Jung', NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260307-0001-01'), 2, '정배우', 'Baeu', 'Jung', NOW());

-- 일별 요금: 3/7(금) 250000, 3/8(토) 300000
INSERT INTO rsv_daily_charge (sub_reservation_id, charge_date, supply_price, tax, service_charge, total, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260307-0001-01'), '2026-03-07', 250000.00, 25000.00, 25000.00, 300000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260307-0001-01'), '2026-03-08', 300000.00, 30000.00, 30000.00, 360000.00, NOW());

-- 서비스: BF-ADD (조식 추가, 2일)
INSERT INTO rsv_reservation_service (sub_reservation_id, service_type, service_option_id, service_date, quantity, unit_price, tax, total_price, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260307-0001-01'), 'PAID',
     (SELECT id FROM rm_paid_service_option WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND service_option_code = 'BF-ADD'),
     '2026-03-07', 1, 27273.00, 2727.00, 30000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260307-0001-01'), 'PAID',
     (SELECT id FROM rm_paid_service_option WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND service_option_code = 'BF-ADD'),
     '2026-03-08', 1, 27273.00, 2727.00, 30000.00, NOW());

-- 결제 정보 (객실 660000 + 서비스 60000 = 720000)
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260307-0001'),
    'PENDING',
    660000.00, 60000.00, 0.00, 0.00, 720000.00,
    NOW(), 'admin'
);

-- =============================================
-- R5: GMP - CHECKED_OUT - 한퇴실 (2026-03-01 ~ 2026-03-03, 2박)
-- =============================================

INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_name_ko, guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'GMP'),
    'GMP260301-0001', 'QRST7890', 'CHECKED_OUT',
    '2026-03-01', '2026-03-03', '2026-02-20 11:00:00',
    '한퇴실', 'Toesil', 'Han',
    '+82', '010-1111-1005', 'toesil.han@example.com', 'M', 'KR',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'EARLY' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'WEB'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND channel_code = 'WEBSITE'),
    NOW(), 'admin'
);

INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'GMP'), '2026-03-01', 1);

-- 서브: STD-D, 11F, 1102, CHECKED_OUT
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260301-0001'),
    'GMP260301-0001-01', 'CHECKED_OUT',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'STD-D'),
    (SELECT id FROM htl_floor WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND floor_number = '11F'),
    (SELECT id FROM htl_room_number WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1102'),
    2, 0, '2026-03-01', '2026-03-03', NOW(), 'admin'
);

-- 투숙객
INSERT INTO rsv_reservation_guest (sub_reservation_id, guest_seq, guest_name_ko, guest_first_name_en, guest_last_name_en, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260301-0001-01'), 1, '한퇴실', 'Toesil', 'Han', NOW());

-- 일별 요금 (2박 × 144000)
INSERT INTO rsv_daily_charge (sub_reservation_id, charge_date, supply_price, tax, service_charge, total, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260301-0001-01'), '2026-03-01', 120000.00, 12000.00, 12000.00, 144000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP260301-0001-01'), '2026-03-02', 120000.00, 12000.00, 12000.00, 144000.00, NOW());

-- 결제 정보 (COMPLETED)
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    payment_date, payment_method,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260301-0001'),
    'COMPLETED',
    288000.00, 0.00, 0.00, 0.00, 288000.00,
    '2026-03-03 11:00:00', 'CREDIT_CARD',
    NOW(), 'admin'
);

-- =============================================
-- R6: GMP - CANCELED - 이취소 (2026-03-10 ~ 2026-03-12)
-- =============================================

INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_name_ko, guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'GMP'),
    'GMP260310-0001', 'UVWX1234', 'CANCELED',
    '2026-03-10', '2026-03-12', '2026-03-02 13:00:00',
    '이취소', 'Chwiso', 'Lee',
    '+82', '010-1111-1006', 'chwiso.lee@example.com', 'F', 'KR',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'RACK' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'FIT'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND channel_code = 'PHONE'),
    NOW(), 'admin'
);

INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'GMP'), '2026-03-10', 1);

-- 서브: STD-S, CANCELED (객실 미배정)
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260310-0001'),
    'GMP260310-0001-01', 'CANCELED',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'STD-S'),
    NULL, NULL,
    1, 0, '2026-03-10', '2026-03-12', NOW(), 'admin'
);

-- 결제 정보 (취소 - 금액 0)
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP260310-0001'),
    'PENDING',
    0.00, 0.00, 0.00, 0.00, 0.00,
    NOW(), 'admin'
);

-- =============================================
-- R7: GMS - RESERVED (OTA) - John Smith (2026-03-18 ~ 2026-03-20, 2박)
-- =============================================

INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    ota_reservation_no, is_ota_managed,
    created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'GMS'),
    'GMS260318-0001', 'YZAB5678', 'RESERVED',
    '2026-03-18', '2026-03-20', '2026-03-04 08:00:00',
    'John', 'Smith',
    '+1', '555-1234-5678', 'john.smith@email.com', 'M', 'US',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'RACK' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'OTA'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND channel_code = 'OTA_BOOKING'),
    'BK-20260318-KR001', TRUE,
    NOW(), 'admin'
);

INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'GMS'), '2026-03-18', 1);

-- 서브: DLX-T (객실 미배정)
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMS260318-0001'),
    'GMS260318-0001-01', 'RESERVED',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'DLX-T'),
    NULL, NULL,
    2, 0, '2026-03-18', '2026-03-20', NOW(), 'admin'
);

-- 투숙객
INSERT INTO rsv_reservation_guest (sub_reservation_id, guest_seq, guest_name_ko, guest_first_name_en, guest_last_name_en, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMS260318-0001-01'), 1, NULL, 'John', 'Smith', NOW());

-- 일별 요금 (2박 × 144000)
INSERT INTO rsv_daily_charge (sub_reservation_id, charge_date, supply_price, tax, service_charge, total, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMS260318-0001-01'), '2026-03-18', 120000.00, 12000.00, 12000.00, 144000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMS260318-0001-01'), '2026-03-19', 120000.00, 12000.00, 12000.00, 144000.00, NOW());

-- 메모
INSERT INTO rsv_reservation_memo (master_reservation_id, content, created_at, created_by)
VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMS260318-0001'),
    'OTA 예약 - Booking.com', NOW(), 'admin'
);

-- =============================================
-- R8: GMS - INHOUSE - 김인하우스 (2026-03-07 ~ 2026-03-10, 3박)
-- =============================================

INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_name_ko, guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'GMS'),
    'GMS260307-0001', 'CDEF9012', 'INHOUSE',
    '2026-03-07', '2026-03-10', '2026-03-06 17:00:00',
    '김인하우스', 'Inhouse', 'Kim',
    '+82', '010-2222-2001', 'inhouse.kim@example.com', 'M', 'KR',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'FIT'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND channel_code = 'WALK_IN'),
    NOW(), 'admin'
);

INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'GMS'), '2026-03-07', 1);

-- 서브: SUI-R, 10F, 1001, INHOUSE
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMS260307-0001'),
    'GMS260307-0001-01', 'INHOUSE',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'SUI-R'),
    (SELECT id FROM htl_floor WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND floor_number = '10F'),
    (SELECT id FROM htl_room_number WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '1001'),
    2, 0, '2026-03-07', '2026-03-10', NOW(), 'admin'
);

-- 투숙객
INSERT INTO rsv_reservation_guest (sub_reservation_id, guest_seq, guest_name_ko, guest_first_name_en, guest_last_name_en, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMS260307-0001-01'), 1, '김인하우스', 'Inhouse', 'Kim', NOW());

-- 결제 정보
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMS260307-0001'),
    'PENDING',
    0.00, 0.00, 0.00, 0.00, 0.00,
    NOW(), 'admin'
);

-- =============================================
-- R9: GMS - NO_SHOW - 유노쇼 (2026-03-08 ~ 2026-03-09, 1박)
-- =============================================

INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_name_ko, guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'GMS'),
    'GMS260308-0001', 'GHIJ3456', 'NO_SHOW',
    '2026-03-08', '2026-03-09', '2026-03-05 10:30:00',
    '유노쇼', 'Nosho', 'Yu',
    '+82', '010-2222-2002', 'nosho.yu@example.com', 'M', 'KR',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'RACK' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'FIT'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND channel_code = 'PHONE'),
    NOW(), 'admin'
);

INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'GMS'), '2026-03-08', 1);

-- 서브: STD-S, NO_SHOW (객실 미배정)
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMS260308-0001'),
    'GMS260308-0001-01', 'NO_SHOW',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'STD-S'),
    NULL, NULL,
    1, 0, '2026-03-08', '2026-03-09', NOW(), 'admin'
);

-- 결제 정보
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMS260308-0001'),
    'PENDING',
    0.00, 0.00, 0.00, 0.00, 0.00,
    NOW(), 'admin'
);

-- =============================================
-- R10: GMS - CHECKED_OUT - 박완료 (2026-02-25 ~ 2026-02-27, 2박)
-- =============================================

INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_name_ko, guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'GMS'),
    'GMS260225-0001', 'KLMN7890', 'CHECKED_OUT',
    '2026-02-25', '2026-02-27', '2026-02-15 09:00:00',
    '박완료', 'Wanryo', 'Park',
    '+82', '010-2222-2003', 'wanryo.park@example.com', 'F', 'KR',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'EARLY' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'WEB'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND channel_code = 'WEBSITE'),
    NOW(), 'admin'
);

INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'GMS'), '2026-02-25', 1);

-- 서브: STD-D, 6F, 601, CHECKED_OUT
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMS260225-0001'),
    'GMS260225-0001-01', 'CHECKED_OUT',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'STD-D'),
    (SELECT id FROM htl_floor WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND floor_number = '6F'),
    (SELECT id FROM htl_room_number WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '601'),
    2, 0, '2026-02-25', '2026-02-27', NOW(), 'admin'
);

-- 투숙객
INSERT INTO rsv_reservation_guest (sub_reservation_id, guest_seq, guest_name_ko, guest_first_name_en, guest_last_name_en, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMS260225-0001-01'), 1, '박완료', 'Wanryo', 'Park', NOW());

-- 결제 정보 (COMPLETED)
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    payment_date, payment_method,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMS260225-0001'),
    'COMPLETED',
    0.00, 0.00, 0.00, 0.00, 0.00,
    '2026-02-27 11:00:00', 'CREDIT_CARD',
    NOW(), 'admin'
);

-- =============================================
-- R11: OBH - RESERVED - 김해운 (2026-03-20 ~ 2026-03-23, 3박, 2객실)
-- =============================================

INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_name_ko, guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, birth_date, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    customer_request, created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'OBH'),
    'OBH260320-0001', 'OPQR1234', 'RESERVED',
    '2026-03-20', '2026-03-23', '2026-03-05 14:00:00',
    '김해운', 'Haeun', 'Kim',
    '+82', '010-3333-3001', 'haeun.kim@example.com', '1982-03-10', 'M', 'KR',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RESORT' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'WEB'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND channel_code = 'WEBSITE'),
    '가족 여행 - 오션뷰 요청', NOW(), 'admin'
);

INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'OBH'), '2026-03-20', 1);

-- 서브1: DLX-O (객실 미배정), adults 2
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, sort_order, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260320-0001'),
    'OBH260320-0001-01', 'RESERVED',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'DLX-O'),
    NULL, NULL,
    2, 0, '2026-03-20', '2026-03-23', 1, NOW(), 'admin'
);

-- 서브2: SUI-P (객실 미배정), adults 2, children 2
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, sort_order, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260320-0001'),
    'OBH260320-0001-02', 'RESERVED',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'SUI-P'),
    NULL, NULL,
    2, 2, '2026-03-20', '2026-03-23', 2, NOW(), 'admin'
);

-- 투숙객 (서브1: 김해운, 김동반 / 서브2: 이가족, 이배우, 이아이1, 이아이2)
INSERT INTO rsv_reservation_guest (sub_reservation_id, guest_seq, guest_name_ko, guest_first_name_en, guest_last_name_en, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260320-0001-01'), 1, '김해운', 'Haeun', 'Kim', NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260320-0001-01'), 2, '김동반', 'Dongban', 'Kim', NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260320-0001-02'), 1, '이가족', 'Gajok', 'Lee', NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260320-0001-02'), 2, '이배우', 'Baeu', 'Lee', NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260320-0001-02'), 3, '이아이1', 'Ai1', 'Lee', NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260320-0001-02'), 4, '이아이2', 'Ai2', 'Lee', NOW());

-- 서비스: SPA-BASIC (유료, 100000)
INSERT INTO rsv_reservation_service (sub_reservation_id, service_type, service_option_id, service_date, quantity, unit_price, tax, total_price, created_at)
VALUES (
    (SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260320-0001-01'), 'PAID',
    (SELECT id FROM rm_paid_service_option WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND service_option_code = 'SPA-BASIC'),
    '2026-03-21', 1, 90909.00, 9091.00, 100000.00, NOW()
);

-- 예치금
INSERT INTO rsv_reservation_deposit (
    master_reservation_id, deposit_method, card_company, card_number_encrypted,
    card_cvc_encrypted, card_expiry_date, card_password_encrypted,
    currency, amount, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260320-0001'),
    'CREDIT_CARD', '신한카드', 'ENC_4333333333333333', 'ENC_789', '03/2029', 'ENC_00',
    'KRW', 1000000.00, NOW(), 'admin'
);

-- 메모
INSERT INTO rsv_reservation_memo (master_reservation_id, content, created_at, created_by)
VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260320-0001'),
    '가족 여행 - 오션뷰 요청', NOW(), 'admin'
);

-- =============================================
-- R12: OBH - RESERVED (OTA) - Michael Brown (2026-03-25 ~ 2026-03-27, 2박)
-- =============================================

INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    ota_reservation_no, is_ota_managed,
    created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'OBH'),
    'OBH260325-0001', 'STUV5678', 'RESERVED',
    '2026-03-25', '2026-03-27', '2026-03-06 12:00:00',
    'Michael', 'Brown',
    '+1', '555-9876-5432', 'michael.brown@email.com', 'M', 'US',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RACK' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'OTA'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND channel_code = 'OTA_AGODA'),
    'AGD-KR-2026032500123', TRUE,
    NOW(), 'admin'
);

INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'OBH'), '2026-03-25', 1);

-- 서브: DLX-O (객실 미배정)
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260325-0001'),
    'OBH260325-0001-01', 'RESERVED',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'DLX-O'),
    NULL, NULL,
    2, 0, '2026-03-25', '2026-03-27', NOW(), 'admin'
);

-- 투숙객
INSERT INTO rsv_reservation_guest (sub_reservation_id, guest_seq, guest_name_ko, guest_first_name_en, guest_last_name_en, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260325-0001-01'), 1, NULL, 'Michael', 'Brown', NOW());

-- 메모
INSERT INTO rsv_reservation_memo (master_reservation_id, content, created_at, created_by)
VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260325-0001'),
    'OTA 예약 - Agoda', NOW(), 'admin'
);

-- =============================================
-- R13: OBH - INHOUSE - 이투숙 (2026-03-06 ~ 2026-03-09, 3박)
-- =============================================

INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_name_ko, guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'OBH'),
    'OBH260306-0001', 'WXYZ9012', 'INHOUSE',
    '2026-03-06', '2026-03-09', '2026-03-05 16:00:00',
    '이투숙', 'Tusuk', 'Lee',
    '+82', '010-3333-3002', 'tusuk.lee@example.com', 'M', 'KR',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RACK' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'FIT'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND channel_code = 'WALK_IN'),
    NOW(), 'admin'
);

INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'OBH'), '2026-03-06', 1);

-- 서브: STD-D, 3F, 301, INHOUSE
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260306-0001'),
    'OBH260306-0001-01', 'INHOUSE',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'STD-D'),
    (SELECT id FROM htl_floor WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND floor_number = '3F'),
    (SELECT id FROM htl_room_number WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '301'),
    2, 0, '2026-03-06', '2026-03-09', NOW(), 'admin'
);

-- 투숙객
INSERT INTO rsv_reservation_guest (sub_reservation_id, guest_seq, guest_name_ko, guest_first_name_en, guest_last_name_en, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260306-0001-01'), 1, '이투숙', 'Tusuk', 'Lee', NOW());

-- 결제 정보
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260306-0001'),
    'PENDING',
    0.00, 0.00, 0.00, 0.00, 0.00,
    NOW(), 'admin'
);

-- =============================================
-- R14: OBH - CHECKED_OUT - 강완료 (2026-02-28 ~ 2026-03-02, 2박)
-- =============================================

INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_name_ko, guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'OBH'),
    'OBH260228-0001', 'ABEF3456', 'CHECKED_OUT',
    '2026-02-28', '2026-03-02', '2026-02-20 15:00:00',
    '강완료', 'Wanryo', 'Kang',
    '+82', '010-3333-3003', 'wanryo.kang@example.com', 'F', 'KR',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'FIT'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND channel_code = 'EMAIL'),
    NOW(), 'admin'
);

INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'OBH'), '2026-02-28', 1);

-- 서브: DLX-D, 7F, 701, CHECKED_OUT
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260228-0001'),
    'OBH260228-0001-01', 'CHECKED_OUT',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'DLX-D'),
    (SELECT id FROM htl_floor WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND floor_number = '7F'),
    (SELECT id FROM htl_room_number WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '701'),
    2, 0, '2026-02-28', '2026-03-02', NOW(), 'admin'
);

-- 투숙객
INSERT INTO rsv_reservation_guest (sub_reservation_id, guest_seq, guest_name_ko, guest_first_name_en, guest_last_name_en, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'OBH260228-0001-01'), 1, '강완료', 'Wanryo', 'Kang', NOW());

-- 결제 정보 (COMPLETED)
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    payment_date, payment_method,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260228-0001'),
    'COMPLETED',
    0.00, 0.00, 0.00, 0.00, 0.00,
    '2026-03-02 11:00:00', 'CREDIT_CARD',
    NOW(), 'admin'
);

-- =============================================
-- R15: OBH - CANCELED - 문취소 (2026-03-15 ~ 2026-03-17)
-- =============================================

INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_name_ko, guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'OBH'),
    'OBH260315-0001', 'CDGH7890', 'CANCELED',
    '2026-03-15', '2026-03-17', '2026-03-03 09:00:00',
    '문취소', 'Chwiso', 'Moon',
    '+82', '010-3333-3004', 'chwiso.moon@example.com', 'F', 'KR',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'EARLY' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'WEB'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND channel_code = 'WEBSITE'),
    NOW(), 'admin'
);

INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
VALUES ((SELECT id FROM htl_property WHERE property_code = 'OBH'), '2026-03-15', 1);

-- 서브: STD-S, CANCELED (객실 미배정)
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out, created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260315-0001'),
    'OBH260315-0001-01', 'CANCELED',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'STD-S'),
    NULL, NULL,
    1, 0, '2026-03-15', '2026-03-17', NOW(), 'admin'
);

-- 메모
INSERT INTO rsv_reservation_memo (master_reservation_id, content, created_at, created_by)
VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'OBH260315-0001'),
    '고객 요청으로 취소', NOW(), 'admin'
);

-- =============================================
-- 시퀀스 리셋
-- =============================================
SELECT setval('rsv_master_reservation_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM rsv_master_reservation), 1));
SELECT setval('rsv_reservation_no_seq_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM rsv_reservation_no_seq), 1));
SELECT setval('rsv_sub_reservation_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM rsv_sub_reservation), 1));
SELECT setval('rsv_reservation_guest_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM rsv_reservation_guest), 1));
SELECT setval('rsv_daily_charge_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM rsv_daily_charge), 1));
SELECT setval('rsv_reservation_service_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM rsv_reservation_service), 1));
SELECT setval('rsv_reservation_deposit_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM rsv_reservation_deposit), 1));
SELECT setval('rsv_reservation_payment_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM rsv_reservation_payment), 1));
SELECT setval('rsv_payment_adjustment_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM rsv_payment_adjustment), 1));
SELECT setval('rsv_reservation_memo_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM rsv_reservation_memo), 1));
