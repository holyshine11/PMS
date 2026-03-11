-- =============================================
-- V5.5.0: 과거 예약 테스트 데이터 (2026-01-01 체크아웃 완료)
-- 캘린더 과거 조회 테스트용
-- =============================================

-- 마스터 예약 (2025-12-31 ~ 2026-01-02, 2박, CHECKED_OUT)
INSERT INTO rsv_master_reservation (
    property_id, master_reservation_no, confirmation_no, reservation_status,
    master_check_in, master_check_out, reservation_date,
    guest_name_ko, guest_first_name_en, guest_last_name_en,
    phone_country_code, phone_number, email, birth_date, gender, nationality,
    rate_code_id, market_code_id, reservation_channel_id,
    customer_request, created_at, created_by
) VALUES (
    (SELECT id FROM htl_property WHERE property_code = 'GMP'),
    'GMP251231-0001', 'NEWY2026', 'CHECKED_OUT',
    '2025-12-31', '2026-01-02', '2025-12-20 14:00:00',
    '박새해', 'Saehae', 'Park',
    '+82', '010-2026-0101', 'newyear@example.com', '1990-01-01', 'M', 'KR',
    (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'RACK' AND deleted_at IS NULL),
    (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'FIT'),
    (SELECT id FROM htl_reservation_channel WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND channel_code = 'WALK_IN'),
    '새해 카운트다운 객실 요청', NOW(), 'admin'
);

-- 서브 예약: STD-D, 11F, 1101
INSERT INTO rsv_sub_reservation (
    master_reservation_id, sub_reservation_no, room_reservation_status,
    room_type_id, floor_id, room_number_id,
    adults, children, check_in, check_out,
    actual_check_in_time, actual_check_out_time,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP251231-0001'),
    'GMP251231-0001-01', 'CHECKED_OUT',
    (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'STD-D'),
    (SELECT id FROM htl_floor WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND floor_number = '11F'),
    (SELECT id FROM htl_room_number WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1101'),
    2, 0, '2025-12-31', '2026-01-02',
    '2025-12-31 15:00:00', '2026-01-02 11:00:00',
    NOW(), 'admin'
);

-- 투숙객
INSERT INTO rsv_reservation_guest (sub_reservation_id, guest_seq, guest_name_ko, guest_first_name_en, guest_last_name_en, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP251231-0001-01'), 1, '박새해', 'Saehae', 'Park', NOW());

-- 일별 요금 (2박)
INSERT INTO rsv_daily_charge (sub_reservation_id, charge_date, supply_price, tax, service_charge, total, created_at)
VALUES
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP251231-0001-01'), '2025-12-31', 200000.00, 20000.00, 20000.00, 240000.00, NOW()),
    ((SELECT id FROM rsv_sub_reservation WHERE sub_reservation_no = 'GMP251231-0001-01'), '2026-01-01', 200000.00, 20000.00, 20000.00, 240000.00, NOW());

-- 결제 정보 (완료)
INSERT INTO rsv_reservation_payment (
    master_reservation_id, payment_status,
    total_room_amount, total_service_amount, total_service_charge_amount,
    total_adjustment_amount, grand_total,
    created_at, created_by
) VALUES (
    (SELECT id FROM rsv_master_reservation WHERE master_reservation_no = 'GMP251231-0001'),
    'PAID',
    400000.00, 0.00, 40000.00, 0.00, 480000.00,
    NOW(), 'admin'
);
