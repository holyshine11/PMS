-- =============================================
-- V5.2.0: 테스트 레이트 데이터 일괄 등록
-- 레이트 코드, 객실타입 매핑, 요금, 인원별 추가요금,
-- 유료서비스 매핑, 프로모션 코드
-- =============================================

-- =============================================
-- 1. 레이트 코드 (rt_rate_code) - 11건
--    GMP: 4건, GMS: 3건, OBH: 4건
-- =============================================

-- GMP 레이트 코드
INSERT INTO rt_rate_code (property_id, rate_code, rate_name_ko, rate_name_en, rate_category, market_code_id, currency, sale_start_date, sale_end_date, min_stay_days, max_stay_days, sort_order, use_yn, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     'RACK', '정상가', 'Rack Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'FIT'),
     'KRW', '2026-01-01', '2026-12-31', 1, 365, 1, TRUE, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     'EARLY', '얼리버드 특가', 'Early Bird', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'WEB'),
     'KRW', '2026-01-01', '2026-12-31', 2, 365, 2, TRUE, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     'PKG-BF', '조식 포함 패키지', 'Breakfast Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'FIT'),
     'KRW', '2026-01-01', '2026-12-31', 1, 365, 3, TRUE, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     'CORP-SP', '기업 특가', 'Corporate Special', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'CORP'),
     'KRW', '2026-01-01', '2026-12-31', 1, 365, 4, TRUE, NOW(), 'admin');

-- GMS 레이트 코드
INSERT INTO rt_rate_code (property_id, rate_code, rate_name_ko, rate_name_en, rate_category, market_code_id, currency, sale_start_date, sale_end_date, min_stay_days, max_stay_days, sort_order, use_yn, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     'RACK', '정상가', 'Rack Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'FIT'),
     'KRW', '2026-01-01', '2026-12-31', 1, 365, 1, TRUE, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     'EARLY', '얼리버드 특가', 'Early Bird', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'WEB'),
     'KRW', '2026-01-01', '2026-12-31', 2, 365, 2, TRUE, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     'PKG-BF', '조식 포함 패키지', 'Breakfast Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'FIT'),
     'KRW', '2026-01-01', '2026-12-31', 1, 365, 3, TRUE, NOW(), 'admin');

-- OBH 레이트 코드
INSERT INTO rt_rate_code (property_id, rate_code, rate_name_ko, rate_name_en, rate_category, market_code_id, currency, sale_start_date, sale_end_date, min_stay_days, max_stay_days, sort_order, use_yn, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     'RACK', '정상가', 'Rack Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'FIT'),
     'KRW', '2026-01-01', '2026-12-31', 1, 365, 1, TRUE, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     'EARLY', '얼리버드 특가', 'Early Bird', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'WEB'),
     'KRW', '2026-01-01', '2026-12-31', 2, 365, 2, TRUE, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     'PKG-BF', '조식 포함 패키지', 'Breakfast Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'FIT'),
     'KRW', '2026-01-01', '2026-12-31', 1, 365, 3, TRUE, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     'RESORT', '리조트 패키지', 'Resort Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'WEB'),
     'KRW', '2026-03-01', '2026-08-31', 1, 365, 4, TRUE, NOW(), 'admin');

-- =============================================
-- 2. 레이트 코드 - 객실 타입 매핑 (rt_rate_code_room_type)
-- =============================================

-- GMP RACK → 전체 객실타입 (STD-S, STD-D, DLX-T, DLX-D, SUI-R)
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'RACK' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'STD-S'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'RACK' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'STD-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'RACK' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'DLX-T'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'RACK' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'DLX-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'RACK' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'SUI-R'), NOW());

-- GMP EARLY → 전체 객실타입
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'STD-S'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'STD-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'DLX-T'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'DLX-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'SUI-R'), NOW());

-- GMP PKG-BF → 전체 객실타입
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'STD-S'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'STD-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'DLX-T'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'DLX-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'SUI-R'), NOW());

-- GMP CORP-SP → STD-S, STD-D, DLX-T, DLX-D (SUI 제외)
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'CORP-SP' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'STD-S'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'CORP-SP' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'STD-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'CORP-SP' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'DLX-T'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'CORP-SP' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_type_code = 'DLX-D'), NOW());

-- GMS RACK → 전체 객실타입 (STD-S, STD-D, DLX-T, SUI-R)
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'RACK' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'STD-S'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'RACK' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'STD-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'RACK' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'DLX-T'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'RACK' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'SUI-R'), NOW());

-- GMS EARLY → 전체 객실타입
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'STD-S'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'STD-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'DLX-T'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'SUI-R'), NOW());

-- GMS PKG-BF → 전체 객실타입
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'STD-S'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'STD-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'DLX-T'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_type_code = 'SUI-R'), NOW());

-- OBH RACK → 전체 객실타입 (STD-S, STD-D, DLX-O, DLX-D, SUI-P)
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RACK' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'STD-S'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RACK' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'STD-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RACK' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'DLX-O'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RACK' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'DLX-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RACK' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'SUI-P'), NOW());

-- OBH EARLY → 전체 객실타입
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'STD-S'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'STD-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'DLX-O'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'DLX-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'SUI-P'), NOW());

-- OBH PKG-BF → 전체 객실타입
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'STD-S'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'STD-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'DLX-O'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'DLX-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'SUI-P'), NOW());

-- OBH RESORT → DLX-O, DLX-D, SUI-P (프리미엄 객실만)
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RESORT' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'DLX-O'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RESORT' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'DLX-D'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RESORT' AND deleted_at IS NULL),
     (SELECT id FROM rm_room_type WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_type_code = 'SUI-P'), NOW());

-- =============================================
-- 3. 레이트 코드 - 유료 서비스 매핑 (rt_rate_code_paid_service)
-- =============================================

-- GMP PKG-BF → BF-ADD
INSERT INTO rt_rate_code_paid_service (rate_code_id, paid_service_option_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_paid_service_option WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND service_option_code = 'BF-ADD'), NOW());

-- GMS PKG-BF → BF-ADD
INSERT INTO rt_rate_code_paid_service (rate_code_id, paid_service_option_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_paid_service_option WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND service_option_code = 'BF-ADD'), NOW());

-- OBH PKG-BF → BF-ADD
INSERT INTO rt_rate_code_paid_service (rate_code_id, paid_service_option_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     (SELECT id FROM rm_paid_service_option WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND service_option_code = 'BF-ADD'), NOW());

-- OBH RESORT → BF-ADD, SPA-BASIC
INSERT INTO rt_rate_code_paid_service (rate_code_id, paid_service_option_id, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RESORT' AND deleted_at IS NULL),
     (SELECT id FROM rm_paid_service_option WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND service_option_code = 'BF-ADD'), NOW()),
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RESORT' AND deleted_at IS NULL),
     (SELECT id FROM rm_paid_service_option WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND service_option_code = 'SPA-BASIC'), NOW());

-- =============================================
-- 4. 요금 정보 (rt_rate_pricing) - 레이트코드당 2건 (평일/주말), 총 22건
-- =============================================

-- GMP RACK 평일 (월~금)
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'RACK' AND deleted_at IS NULL),
     TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, 'KRW', 150000.00, 15000.00, 165000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- GMP RACK 주말 (토~일)
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'RACK' AND deleted_at IS NULL),
     FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, 'KRW', 200000.00, 20000.00, 220000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- GMP EARLY 평일
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, 'KRW', 120000.00, 12000.00, 132000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- GMP EARLY 주말
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, 'KRW', 160000.00, 16000.00, 176000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- GMP PKG-BF 평일
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, 'KRW', 180000.00, 18000.00, 198000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- GMP PKG-BF 주말
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, 'KRW', 240000.00, 24000.00, 264000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- GMP CORP-SP 평일
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'CORP-SP' AND deleted_at IS NULL),
     TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, 'KRW', 105000.00, 10500.00, 115500.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- GMP CORP-SP 주말
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'CORP-SP' AND deleted_at IS NULL),
     FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, 'KRW', 140000.00, 14000.00, 154000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- GMS RACK 평일
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'RACK' AND deleted_at IS NULL),
     TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, 'KRW', 120000.00, 12000.00, 132000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- GMS RACK 주말
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'RACK' AND deleted_at IS NULL),
     FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, 'KRW', 160000.00, 16000.00, 176000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- GMS EARLY 평일
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, 'KRW', 96000.00, 9600.00, 105600.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- GMS EARLY 주말
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, 'KRW', 128000.00, 12800.00, 140800.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- GMS PKG-BF 평일
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, 'KRW', 150000.00, 15000.00, 165000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- GMS PKG-BF 주말
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, 'KRW', 200000.00, 20000.00, 220000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- OBH RACK 평일
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RACK' AND deleted_at IS NULL),
     TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, 'KRW', 180000.00, 18000.00, 198000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- OBH RACK 주말
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RACK' AND deleted_at IS NULL),
     FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, 'KRW', 250000.00, 25000.00, 275000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- OBH EARLY 평일
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, 'KRW', 144000.00, 14400.00, 158400.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- OBH EARLY 주말
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'EARLY' AND deleted_at IS NULL),
     FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, 'KRW', 200000.00, 20000.00, 220000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- OBH PKG-BF 평일
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, 'KRW', 220000.00, 22000.00, 242000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- OBH PKG-BF 주말
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'PKG-BF' AND deleted_at IS NULL),
     FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, 'KRW', 300000.00, 30000.00, 330000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- OBH RESORT 평일
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RESORT' AND deleted_at IS NULL),
     TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, 'KRW', 280000.00, 28000.00, 308000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- OBH RESORT 주말
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, created_at)
VALUES
    ((SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RESORT' AND deleted_at IS NULL),
     FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, 'KRW', 380000.00, 38000.00, 418000.00, NULL, NULL, NULL, 0, 0, NULL, NOW());

-- =============================================
-- 5. 인원별 추가 요금 (rt_rate_pricing_person) - 요금행당 2건, 총 44건
--    평일: ADULT seq2 (30000/3000/33000), CHILD seq1 (15000/1500/16500)
--    주말: ADULT seq2 (40000/4000/44000), CHILD seq1 (20000/2000/22000)
-- =============================================

-- GMP RACK 평일 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'RACK' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'ADULT', 2, 30000.00, 3000.00, 33000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'RACK' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'CHILD', 1, 15000.00, 1500.00, 16500.00);

-- GMP RACK 주말 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'RACK' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'ADULT', 2, 40000.00, 4000.00, 44000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'RACK' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'CHILD', 1, 20000.00, 2000.00, 22000.00);

-- GMP EARLY 평일 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'EARLY' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'ADULT', 2, 30000.00, 3000.00, 33000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'EARLY' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'CHILD', 1, 15000.00, 1500.00, 16500.00);

-- GMP EARLY 주말 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'EARLY' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'ADULT', 2, 40000.00, 4000.00, 44000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'EARLY' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'CHILD', 1, 20000.00, 2000.00, 22000.00);

-- GMP PKG-BF 평일 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'PKG-BF' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'ADULT', 2, 30000.00, 3000.00, 33000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'PKG-BF' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'CHILD', 1, 15000.00, 1500.00, 16500.00);

-- GMP PKG-BF 주말 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'PKG-BF' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'ADULT', 2, 40000.00, 4000.00, 44000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'PKG-BF' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'CHILD', 1, 20000.00, 2000.00, 22000.00);

-- GMP CORP-SP 평일 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'CORP-SP' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'ADULT', 2, 30000.00, 3000.00, 33000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'CORP-SP' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'CHILD', 1, 15000.00, 1500.00, 16500.00);

-- GMP CORP-SP 주말 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'CORP-SP' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'ADULT', 2, 40000.00, 4000.00, 44000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'CORP-SP' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'CHILD', 1, 20000.00, 2000.00, 22000.00);

-- GMS RACK 평일 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'RACK' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'ADULT', 2, 30000.00, 3000.00, 33000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'RACK' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'CHILD', 1, 15000.00, 1500.00, 16500.00);

-- GMS RACK 주말 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'RACK' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'ADULT', 2, 40000.00, 4000.00, 44000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'RACK' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'CHILD', 1, 20000.00, 2000.00, 22000.00);

-- GMS EARLY 평일 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'EARLY' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'ADULT', 2, 30000.00, 3000.00, 33000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'EARLY' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'CHILD', 1, 15000.00, 1500.00, 16500.00);

-- GMS EARLY 주말 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'EARLY' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'ADULT', 2, 40000.00, 4000.00, 44000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'EARLY' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'CHILD', 1, 20000.00, 2000.00, 22000.00);

-- GMS PKG-BF 평일 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'PKG-BF' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'ADULT', 2, 30000.00, 3000.00, 33000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'PKG-BF' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'CHILD', 1, 15000.00, 1500.00, 16500.00);

-- GMS PKG-BF 주말 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'PKG-BF' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'ADULT', 2, 40000.00, 4000.00, 44000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'PKG-BF' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'CHILD', 1, 20000.00, 2000.00, 22000.00);

-- OBH RACK 평일 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'RACK' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'ADULT', 2, 30000.00, 3000.00, 33000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'RACK' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'CHILD', 1, 15000.00, 1500.00, 16500.00);

-- OBH RACK 주말 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'RACK' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'ADULT', 2, 40000.00, 4000.00, 44000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'RACK' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'CHILD', 1, 20000.00, 2000.00, 22000.00);

-- OBH EARLY 평일 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'EARLY' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'ADULT', 2, 30000.00, 3000.00, 33000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'EARLY' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'CHILD', 1, 15000.00, 1500.00, 16500.00);

-- OBH EARLY 주말 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'EARLY' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'ADULT', 2, 40000.00, 4000.00, 44000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'EARLY' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'CHILD', 1, 20000.00, 2000.00, 22000.00);

-- OBH PKG-BF 평일 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'PKG-BF' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'ADULT', 2, 30000.00, 3000.00, 33000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'PKG-BF' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'CHILD', 1, 15000.00, 1500.00, 16500.00);

-- OBH PKG-BF 주말 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'PKG-BF' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'ADULT', 2, 40000.00, 4000.00, 44000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'PKG-BF' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'CHILD', 1, 20000.00, 2000.00, 22000.00);

-- OBH RESORT 평일 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'RESORT' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'ADULT', 2, 30000.00, 3000.00, 33000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'RESORT' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE),
     'CHILD', 1, 15000.00, 1500.00, 16500.00);

-- OBH RESORT 주말 인원별 추가
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
VALUES
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'RESORT' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'ADULT', 2, 40000.00, 4000.00, 44000.00),
    ((SELECT rp.id FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'RESORT' AND rc.deleted_at IS NULL AND rp.day_sat = TRUE),
     'CHILD', 1, 20000.00, 2000.00, 22000.00);

-- =============================================
-- 6. 프로모션 코드 (rt_promotion_code) - 프로퍼티당 2건, 총 6건
-- =============================================

-- GMP 프로모션 코드
INSERT INTO rt_promotion_code (property_id, rate_code_id, promotion_code, promotion_start_date, promotion_end_date, description_ko, description_en, promotion_type, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, use_yn, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'RACK' AND deleted_at IS NULL),
     'SUMMER26', '2026-06-01', '2026-08-31', '2026 여름 특가', 'Summer Special 2026', 'SEASONAL', '-', 15.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'RACK' AND deleted_at IS NULL),
     'WINTER26', '2026-11-01', '2027-02-28', '2026 겨울 특가', 'Winter Special 2026', 'SEASONAL', '-', 10.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 2, NOW(), 'admin');

-- GMS 프로모션 코드
INSERT INTO rt_promotion_code (property_id, rate_code_id, promotion_code, promotion_start_date, promotion_end_date, description_ko, description_en, promotion_type, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, use_yn, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'RACK' AND deleted_at IS NULL),
     'SUMMER26', '2026-06-01', '2026-08-31', '2026 여름 특가', 'Summer Special 2026', 'SEASONAL', '-', 15.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'RACK' AND deleted_at IS NULL),
     'WINTER26', '2026-11-01', '2027-02-28', '2026 겨울 특가', 'Winter Special 2026', 'SEASONAL', '-', 10.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 2, NOW(), 'admin');

-- OBH 프로모션 코드
INSERT INTO rt_promotion_code (property_id, rate_code_id, promotion_code, promotion_start_date, promotion_end_date, description_ko, description_en, promotion_type, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, use_yn, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RACK' AND deleted_at IS NULL),
     'SUMMER26', '2026-06-01', '2026-08-31', '2026 여름 특가', 'Summer Special 2026', 'SEASONAL', '-', 15.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'RACK' AND deleted_at IS NULL),
     'WINTER26', '2026-11-01', '2027-02-28', '2026 겨울 특가', 'Winter Special 2026', 'SEASONAL', '-', 10.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 2, NOW(), 'admin');

-- =============================================
-- 7. 시퀀스 리셋
-- =============================================
SELECT setval('rt_rate_code_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rt_rate_code));
SELECT setval('rt_rate_code_room_type_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rt_rate_code_room_type));
SELECT setval('rt_rate_code_paid_service_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rt_rate_code_paid_service));
SELECT setval('rt_rate_pricing_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rt_rate_pricing));
SELECT setval('rt_rate_pricing_person_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rt_rate_pricing_person));
SELECT setval('rt_promotion_code_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rt_promotion_code));
