-- =============================================
-- V5.0.0: 테스트 기초 데이터 일괄 등록
-- 호텔, 프로퍼티, 층, 호수, 마켓코드, 예약채널,
-- 관리자 사용자, 역할
-- =============================================

-- =============================================
-- 0. 기존 비즈니스 데이터 정리 (역순 FK 삭제)
--    시스템 관리자(id=1)와 메뉴 시드 데이터는 보존
-- =============================================

-- 예약 관련 (자식 → 부모 순서)
DELETE FROM rsv_reservation_memo;
DELETE FROM rsv_payment_adjustment;
DELETE FROM rsv_reservation_payment;
DELETE FROM rsv_reservation_deposit;
DELETE FROM rsv_reservation_service;
DELETE FROM rsv_daily_charge;
DELETE FROM rsv_reservation_guest;
DELETE FROM rsv_sub_reservation;
DELETE FROM rsv_master_reservation;
DELETE FROM rsv_reservation_no_seq;

-- 레이트/프로모션
DELETE FROM rt_promotion_code;
DELETE FROM rt_rate_pricing_person;
DELETE FROM rt_rate_pricing;
DELETE FROM rt_rate_code_paid_service;
DELETE FROM rt_rate_code_room_type;
DELETE FROM rt_rate_code;

-- 객실 매핑 → 객실 타입/클래스
DELETE FROM rm_room_type_paid_service;
DELETE FROM rm_room_type_free_service;
DELETE FROM rm_room_type_floor;
DELETE FROM rm_room_type;
DELETE FROM rm_room_class;

-- 서비스 옵션
DELETE FROM rm_paid_service_option;
DELETE FROM rm_free_service_option;

-- 프로퍼티 부속 테이블 (정산, 취소수수료)
DELETE FROM htl_property_settlement;
DELETE FROM htl_cancellation_fee;

-- 관리자-프로퍼티 매핑
DELETE FROM sys_admin_user_property;

-- 역할-메뉴 매핑 (보존 안 함 - 역할 데이터가 새로 들어가므로)
DELETE FROM sys_role_menu;

-- 관리자 사용자 (시스템관리자 id=1 제외)
DELETE FROM sys_admin_user WHERE id != 1;

-- 역할
DELETE FROM sys_role;

-- 예약채널, 마켓코드
DELETE FROM htl_reservation_channel;
DELETE FROM htl_market_code;

-- 호수, 층
DELETE FROM htl_room_number;
DELETE FROM htl_floor;

-- 프로퍼티, 호텔
DELETE FROM htl_property;
DELETE FROM htl_hotel;

-- =============================================
-- 1. 호텔 (htl_hotel) - 2건
-- =============================================
INSERT INTO htl_hotel (hotel_code, hotel_name, representative_name, representative_name_en, country_code, phone, email, zip_code, address, address_detail, introduction, sort_order, created_at, created_by)
VALUES
    ('HTL00001', '올라 서울 호텔', '김대표', 'Kim Daepyo', '+82', '02-1234-5678', 'seoul@holapms.com', '04538', '서울특별시 중구 명동길 74', NULL, '서울 중심부에 위치한 올라 호텔 그룹 플래그십', 1, NOW(), 'admin'),
    ('HTL00002', '올라 부산 호텔', '박해운', 'Park Haeun', '+82', '051-9876-5432', 'busan@holapms.com', '48099', '부산광역시 해운대구 해운대해변로 264', NULL, '해운대 해변에 위치한 올라 호텔 그룹 리조트', 2, NOW(), 'admin');

-- =============================================
-- 2. 프로퍼티 (htl_property) - 3건
-- =============================================
INSERT INTO htl_property (hotel_id, property_code, property_name, property_type, check_in_time, check_out_time, total_rooms, phone, email, star_rating, timezone, business_number, tax_rate, tax_decimal_places, tax_rounding_method, service_charge_rate, service_charge_decimal_places, service_charge_rounding_method, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001'), 'GMP', '올라 그랜드 명동', 'HOTEL', '15:00', '11:00', 150, '02-1234-5678', 'gmp@holapms.com', '5STAR', 'Asia/Seoul', '123-45-67890', 10.00, 0, 'ROUND', 10.00, 0, 'ROUND', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001'), 'GMS', '올라 그랜드 서초', 'HOTEL', '15:00', '11:00', 100, '02-1234-5679', 'gms@holapms.com', '4STAR', 'Asia/Seoul', '123-45-67891', 10.00, 0, 'ROUND', 10.00, 0, 'ROUND', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002'), 'OBH', '올라 비치 해운대', 'HOTEL', '15:00', '11:00', 200, '051-9876-5433', 'obh@holapms.com', '5STAR', 'Asia/Seoul', '987-65-43210', 10.00, 0, 'ROUND', 10.00, 0, 'ROUND', 1, NOW(), 'admin');

-- =============================================
-- 3. 층 (htl_floor)
--    GMP: 10F~15F (6건), GMS: 5F~10F (6건), OBH: 1F~8F (8건)
-- =============================================

-- GMP 층
INSERT INTO htl_floor (property_id, floor_number, floor_name, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '10F', '10층', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '11F', '11층', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '12F', '12층', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '13F', '13층', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '14F', '14층', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '15F', '15층', 6, NOW(), 'admin');

-- GMS 층
INSERT INTO htl_floor (property_id, floor_number, floor_name, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '5F', '5층', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '6F', '6층', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '7F', '7층', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '8F', '8층', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '9F', '9층', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '10F', '10층', 6, NOW(), 'admin');

-- OBH 층
INSERT INTO htl_floor (property_id, floor_number, floor_name, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '1F', '1층', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '2F', '2층', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '3F', '3층', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '4F', '4층', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '5F', '5층', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '6F', '6층', 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '7F', '7층', 7, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '8F', '8층', 8, NOW(), 'admin');

-- =============================================
-- 4. 호수 (htl_room_number)
--    GMP: 1001~1505 (30건), GMS: 501~1003 (18건), OBH: 101~805 (40건)
-- =============================================

-- GMP 호수 (10F: 1001-1005, 11F: 1101-1105, ..., 15F: 1501-1505)
INSERT INTO htl_room_number (property_id, room_number, sort_order, created_at, created_by)
VALUES
    -- 10F
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1001', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1002', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1003', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1004', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1005', 5, NOW(), 'admin'),
    -- 11F
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1101', 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1102', 7, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1103', 8, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1104', 9, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1105', 10, NOW(), 'admin'),
    -- 12F
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1201', 11, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1202', 12, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1203', 13, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1204', 14, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1205', 15, NOW(), 'admin'),
    -- 13F
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1301', 16, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1302', 17, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1303', 18, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1304', 19, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1305', 20, NOW(), 'admin'),
    -- 14F
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1401', 21, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1402', 22, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1403', 23, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1404', 24, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1405', 25, NOW(), 'admin'),
    -- 15F
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1501', 26, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1502', 27, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1503', 28, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1504', 29, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1505', 30, NOW(), 'admin');

-- GMS 호수 (5F: 501-503, 6F: 601-603, ..., 10F: 1001-1003)
INSERT INTO htl_room_number (property_id, room_number, sort_order, created_at, created_by)
VALUES
    -- 5F
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '501', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '502', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '503', 3, NOW(), 'admin'),
    -- 6F
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '601', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '602', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '603', 6, NOW(), 'admin'),
    -- 7F
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '701', 7, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '702', 8, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '703', 9, NOW(), 'admin'),
    -- 8F
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '801', 10, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '802', 11, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '803', 12, NOW(), 'admin'),
    -- 9F
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '901', 13, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '902', 14, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '903', 15, NOW(), 'admin'),
    -- 10F
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1001', 16, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1002', 17, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), '1003', 18, NOW(), 'admin');

-- OBH 호수 (1F: 101-105, 2F: 201-205, ..., 8F: 801-805)
INSERT INTO htl_room_number (property_id, room_number, sort_order, created_at, created_by)
VALUES
    -- 1F
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '101', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '102', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '103', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '104', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '105', 5, NOW(), 'admin'),
    -- 2F
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '201', 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '202', 7, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '203', 8, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '204', 9, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '205', 10, NOW(), 'admin'),
    -- 3F
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '301', 11, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '302', 12, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '303', 13, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '304', 14, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '305', 15, NOW(), 'admin'),
    -- 4F
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '401', 16, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '402', 17, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '403', 18, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '404', 19, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '405', 20, NOW(), 'admin'),
    -- 5F
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '501', 21, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '502', 22, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '503', 23, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '504', 24, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '505', 25, NOW(), 'admin'),
    -- 6F
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '601', 26, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '602', 27, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '603', 28, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '604', 29, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '605', 30, NOW(), 'admin'),
    -- 7F
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '701', 31, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '702', 32, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '703', 33, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '704', 34, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '705', 35, NOW(), 'admin'),
    -- 8F
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '801', 36, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '802', 37, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '803', 38, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '804', 39, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), '805', 40, NOW(), 'admin');

-- =============================================
-- 5. 마켓코드 (htl_market_code) - 프로퍼티당 6건, 총 18건
-- =============================================

-- GMP 마켓코드
INSERT INTO htl_market_code (property_id, market_code, market_name, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'FIT', '개인 여행', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'GRP', '단체', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'CORP', '기업', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'OTA', '온라인 여행사', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'GOV', '정부/공공기관', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'WEB', '자사 웹사이트', 6, NOW(), 'admin');

-- GMS 마켓코드
INSERT INTO htl_market_code (property_id, market_code, market_name, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'FIT', '개인 여행', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'GRP', '단체', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'CORP', '기업', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'OTA', '온라인 여행사', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'GOV', '정부/공공기관', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'WEB', '자사 웹사이트', 6, NOW(), 'admin');

-- OBH 마켓코드
INSERT INTO htl_market_code (property_id, market_code, market_name, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), 'FIT', '개인 여행', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), 'GRP', '단체', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), 'CORP', '기업', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), 'OTA', '온라인 여행사', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), 'GOV', '정부/공공기관', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), 'WEB', '자사 웹사이트', 6, NOW(), 'admin');

-- =============================================
-- 6. 예약채널 (htl_reservation_channel) - 프로퍼티당 7건, 총 21건
-- =============================================

-- GMP 예약채널
INSERT INTO htl_reservation_channel (property_id, channel_code, channel_name, channel_type, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'WALK_IN', '워크인', 'WALK_IN', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'PHONE', '전화 예약', 'PHONE', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'EMAIL', '이메일 예약', 'EMAIL', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'OTA_BOOKING', '부킹닷컴', 'OTA', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'OTA_AGODA', '아고다', 'OTA', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'OTA_EXPEDIA', '익스피디아', 'OTA', 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'WEBSITE', '자사 웹사이트', 'WEBSITE', 7, NOW(), 'admin');

-- GMS 예약채널
INSERT INTO htl_reservation_channel (property_id, channel_code, channel_name, channel_type, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'WALK_IN', '워크인', 'WALK_IN', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'PHONE', '전화 예약', 'PHONE', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'EMAIL', '이메일 예약', 'EMAIL', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'OTA_BOOKING', '부킹닷컴', 'OTA', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'OTA_AGODA', '아고다', 'OTA', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'OTA_EXPEDIA', '익스피디아', 'OTA', 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 'WEBSITE', '자사 웹사이트', 'WEBSITE', 7, NOW(), 'admin');

-- OBH 예약채널
INSERT INTO htl_reservation_channel (property_id, channel_code, channel_name, channel_type, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), 'WALK_IN', '워크인', 'WALK_IN', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), 'PHONE', '전화 예약', 'PHONE', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), 'EMAIL', '이메일 예약', 'EMAIL', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), 'OTA_BOOKING', '부킹닷컴', 'OTA', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), 'OTA_AGODA', '아고다', 'OTA', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), 'OTA_EXPEDIA', '익스피디아', 'OTA', 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), 'WEBSITE', '자사 웹사이트', 'WEBSITE', 7, NOW(), 'admin');

-- =============================================
-- 7. 역할 (sys_role) - 3건
-- =============================================
INSERT INTO sys_role (role_name, hotel_id, target_type, property_id, sort_order, created_at, created_by)
VALUES
    ('총괄 매니저', (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001'), 'HOTEL_ADMIN', NULL, 1, NOW(), 'admin'),
    ('프론트 매니저', (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002'), 'HOTEL_ADMIN', NULL, 1, NOW(), 'admin'),
    ('프로퍼티 매니저', (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001'), 'PROPERTY_ADMIN', (SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 1, NOW(), 'admin');

-- =============================================
-- 8. 관리자 사용자 (sys_admin_user) - 5건
--    비밀번호: holapms1! (BCrypt)
-- =============================================
INSERT INTO sys_admin_user (login_id, password, user_name, email, phone, role, member_number, account_type, hotel_id, department, position, role_id, sort_order, created_at, created_by)
VALUES
    ('hotel1admin', '$2a$10$lEKY9wNGwFk.VJrMSfXvne09W4RfTbAqFB0IOJ2kkt.lS.y9eAksK', '김서울', 'hotel1admin@holapms.com', '02-1111-1111', 'HOTEL_ADMIN', 'U000000002', 'HOTEL_ADMIN', (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001'), '프론트', '총지배인', (SELECT id FROM sys_role WHERE role_name = '총괄 매니저' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), 2, NOW(), 'admin'),
    ('hotel2admin', '$2a$10$lEKY9wNGwFk.VJrMSfXvne09W4RfTbAqFB0IOJ2kkt.lS.y9eAksK', '박부산', 'hotel2admin@holapms.com', '051-2222-2222', 'HOTEL_ADMIN', 'U000000003', 'HOTEL_ADMIN', (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002'), '프론트', '총지배인', (SELECT id FROM sys_role WHERE role_name = '프론트 매니저' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), 3, NOW(), 'admin'),
    ('prop1admin', '$2a$10$lEKY9wNGwFk.VJrMSfXvne09W4RfTbAqFB0IOJ2kkt.lS.y9eAksK', '이명동', 'prop1admin@holapms.com', '02-3333-3333', 'PROPERTY_ADMIN', 'U000000004', 'PROPERTY_ADMIN', (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001'), '객실관리', '매니저', NULL, 4, NOW(), 'admin'),
    ('prop2admin', '$2a$10$lEKY9wNGwFk.VJrMSfXvne09W4RfTbAqFB0IOJ2kkt.lS.y9eAksK', '최서초', 'prop2admin@holapms.com', '02-4444-4444', 'PROPERTY_ADMIN', 'U000000005', 'PROPERTY_ADMIN', (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001'), '객실관리', '매니저', NULL, 5, NOW(), 'admin'),
    ('prop3admin', '$2a$10$lEKY9wNGwFk.VJrMSfXvne09W4RfTbAqFB0IOJ2kkt.lS.y9eAksK', '정해운', 'prop3admin@holapms.com', '051-5555-5555', 'PROPERTY_ADMIN', 'U000000006', 'PROPERTY_ADMIN', (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002'), '객실관리', '매니저', NULL, 6, NOW(), 'admin');

-- =============================================
-- 9. 관리자-프로퍼티 매핑 (sys_admin_user_property) - 6건
-- =============================================
INSERT INTO sys_admin_user_property (admin_user_id, property_id, created_at)
VALUES
    -- hotel1admin → GMP, GMS
    ((SELECT id FROM sys_admin_user WHERE login_id = 'hotel1admin'), (SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), NOW()),
    ((SELECT id FROM sys_admin_user WHERE login_id = 'hotel1admin'), (SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), NOW()),
    -- hotel2admin → OBH
    ((SELECT id FROM sys_admin_user WHERE login_id = 'hotel2admin'), (SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), NOW()),
    -- prop1admin → GMP
    ((SELECT id FROM sys_admin_user WHERE login_id = 'prop1admin'), (SELECT id FROM htl_property WHERE property_code = 'GMP' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), NOW()),
    -- prop2admin → GMS
    ((SELECT id FROM sys_admin_user WHERE login_id = 'prop2admin'), (SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')), NOW()),
    -- prop3admin → OBH
    ((SELECT id FROM sys_admin_user WHERE login_id = 'prop3admin'), (SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')), NOW());

-- =============================================
-- 10. 시퀀스 리셋
-- =============================================
SELECT setval('htl_hotel_id_seq', (SELECT COALESCE(MAX(id), 0) FROM htl_hotel));
SELECT setval('htl_property_id_seq', (SELECT COALESCE(MAX(id), 0) FROM htl_property));
SELECT setval('htl_floor_id_seq', (SELECT COALESCE(MAX(id), 0) FROM htl_floor));
SELECT setval('htl_room_number_id_seq', (SELECT COALESCE(MAX(id), 0) FROM htl_room_number));
SELECT setval('htl_market_code_id_seq', (SELECT COALESCE(MAX(id), 0) FROM htl_market_code));
SELECT setval('htl_reservation_channel_id_seq', (SELECT COALESCE(MAX(id), 0) FROM htl_reservation_channel));
SELECT setval('sys_role_id_seq', (SELECT COALESCE(MAX(id), 0) FROM sys_role));
SELECT setval('sys_admin_user_id_seq', (SELECT COALESCE(MAX(id), 0) FROM sys_admin_user));
SELECT setval('sys_admin_user_property_id_seq', (SELECT COALESCE(MAX(id), 0) FROM sys_admin_user_property));

-- 호텔코드 시퀀스: HTL00001, HTL00002 → 다음은 3번부터
SELECT setval('htl_hotel_code_seq', 2);

-- 회원번호 시퀀스: U000000001~U000000006 → 다음은 7번부터
SELECT setval('sys_member_number_seq', 6);
