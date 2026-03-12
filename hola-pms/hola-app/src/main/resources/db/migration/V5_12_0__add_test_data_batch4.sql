-- ============================================================
-- V5_12_0: 테스트 데이터 추가 Batch 4
-- 레이트 코드(30) + 요금정보 + 인원별 요금 + 옵션요금 + 프로모션 코드(30)
-- 요금 설정: 평일/주말 구분, 인원별 요금 세분화
-- ============================================================

-- ============================================================
-- 1. 레이트 코드 (rt_rate_code) - 30건 (프로퍼티당 10건)
-- 기존: GMP(4), GMS(3), OBH(4) = 11건
-- ============================================================

-- GMP 레이트 코드
INSERT INTO rt_rate_code (property_id, rate_code, rate_name_ko, rate_name_en, rate_category, market_code_id, currency, sale_start_date, sale_end_date, min_stay_days, max_stay_days, sort_order, use_yn, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'WKND', '주말 특가', 'Weekend Special', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'FIT' AND deleted_at IS NULL),
     'KRW', '2026-03-01', '2026-12-31', 1, 3, 10, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'CORP-STD', '기업 표준 요금', 'Corporate Standard Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'CORP' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 1, 365, 11, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'AGT-NET', '여행사 넷레이트', 'Travel Agent Net Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'AGT' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 1, 30, 12, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'LONG-30', '장기투숙 30일+', 'Long Stay 30 Days+', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'LONG' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2027-12-31', 30, 365, 13, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'MICE-GRP', 'MICE 단체 패키지', 'MICE Group Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'MICE' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 1, 14, 14, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'HON-PKG', '허니문 패키지', 'Honeymoon Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'HON' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 2, 7, 15, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'FAM-PKG', '패밀리 패키지', 'Family Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'FAM' AND deleted_at IS NULL),
     'KRW', '2026-06-01', '2026-08-31', 2, 14, 16, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'MED-PKG', '의료관광 패키지', 'Medical Tourism Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'MED' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2027-12-31', 3, 90, 17, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'CREW-RT', '항공사 승무원 요금', 'Airline Crew Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'CREW' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2027-12-31', 1, 3, 18, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'GOV-RT', '관공서 관용 요금', 'Government Official Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND market_code = 'GOV' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 1, 30, 19, true, NOW(), 'admin');

-- GMS 레이트 코드
INSERT INTO rt_rate_code (property_id, rate_code, rate_name_ko, rate_name_en, rate_category, market_code_id, currency, sale_start_date, sale_end_date, min_stay_days, max_stay_days, sort_order, use_yn, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'WKND', '주말 특가', 'Weekend Special', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'FIT' AND deleted_at IS NULL),
     'KRW', '2026-03-01', '2026-12-31', 1, 3, 10, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'CORP-STD', '기업 표준 요금', 'Corporate Standard Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'CORP' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 1, 365, 11, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'AGT-NET', '여행사 넷레이트', 'Travel Agent Net Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'AGT' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 1, 30, 12, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'LONG-30', '장기투숙 30일+', 'Long Stay 30 Days+', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'LONG' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2027-12-31', 30, 365, 13, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'MICE-GRP', 'MICE 단체 패키지', 'MICE Group Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'MICE' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 1, 14, 14, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'HON-PKG', '허니문 패키지', 'Honeymoon Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'HON' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 2, 7, 15, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'FAM-PKG', '패밀리 패키지', 'Family Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'FAM' AND deleted_at IS NULL),
     'KRW', '2026-06-01', '2026-08-31', 2, 14, 16, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'MED-PKG', '의료관광 패키지', 'Medical Tourism Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'MED' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2027-12-31', 3, 90, 17, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'CREW-RT', '항공사 승무원 요금', 'Airline Crew Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'CREW' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2027-12-31', 1, 3, 18, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'GOV-RT', '관공서 관용 요금', 'Government Official Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND market_code = 'GOV' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 1, 30, 19, true, NOW(), 'admin');

-- OBH 레이트 코드
INSERT INTO rt_rate_code (property_id, rate_code, rate_name_ko, rate_name_en, rate_category, market_code_id, currency, sale_start_date, sale_end_date, min_stay_days, max_stay_days, sort_order, use_yn, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'WKND', '주말 특가', 'Weekend Special', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'FIT' AND deleted_at IS NULL),
     'KRW', '2026-03-01', '2026-12-31', 1, 3, 10, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'CORP-STD', '기업 표준 요금', 'Corporate Standard Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'CORP' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 1, 365, 11, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'AGT-NET', '여행사 넷레이트', 'Travel Agent Net Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'AGT' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 1, 30, 12, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'LONG-30', '장기투숙 30일+', 'Long Stay 30 Days+', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'LONG' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2027-12-31', 30, 365, 13, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'MICE-GRP', 'MICE 단체 패키지', 'MICE Group Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'MICE' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 1, 14, 14, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'HON-PKG', '허니문 패키지', 'Honeymoon Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'HON' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 2, 7, 15, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'BEACH-SUM', '해운대 썸머 패키지', 'Haeundae Summer Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'FAM' AND deleted_at IS NULL),
     'KRW', '2026-06-01', '2026-08-31', 2, 14, 16, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'MED-PKG', '의료관광 패키지', 'Medical Tourism Package', 'PACKAGE',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'MED' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2027-12-31', 3, 90, 17, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'CREW-RT', '항공사 승무원 요금', 'Airline Crew Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'CREW' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2027-12-31', 1, 3, 18, true, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'GOV-RT', '관공서 관용 요금', 'Government Official Rate', 'ROOM_ONLY',
     (SELECT id FROM htl_market_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND market_code = 'GOV' AND deleted_at IS NULL),
     'KRW', '2026-01-01', '2026-12-31', 1, 30, 19, true, NOW(), 'admin');

-- ============================================================
-- 2. 레이트 코드 - 객실 타입 매핑 (rt_rate_code_room_type)
-- 각 레이트 코드에 2-3개 객실 타입 연결
-- ============================================================

-- GMP 레이트-객실 매핑
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id)
SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL
  AND rt.property_id = rc.property_id AND rt.room_type_code IN ('ECO-S', 'ECO-T', 'BUS-K') AND rt.deleted_at IS NULL;

INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id)
SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'CORP-STD' AND rc.deleted_at IS NULL
  AND rt.property_id = rc.property_id AND rt.room_type_code IN ('BUS-K', 'EXE-K', 'PRE-K') AND rt.deleted_at IS NULL;

INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id)
SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'AGT-NET' AND rc.deleted_at IS NULL
  AND rt.property_id = rc.property_id AND rt.room_type_code IN ('ECO-S', 'ECO-T', 'BUS-K', 'EXE-K') AND rt.deleted_at IS NULL;

INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id)
SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'LONG-30' AND rc.deleted_at IS NULL
  AND rt.property_id = rc.property_id AND rt.room_type_code IN ('ECO-S', 'ECO-T', 'BUS-K') AND rt.deleted_at IS NULL;

INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id)
SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'MICE-GRP' AND rc.deleted_at IS NULL
  AND rt.property_id = rc.property_id AND rt.room_type_code IN ('BUS-K', 'EXE-K') AND rt.deleted_at IS NULL;

INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id)
SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'HON-PKG' AND rc.deleted_at IS NULL
  AND rt.property_id = rc.property_id AND rt.room_type_code IN ('JNR-K', 'PEN-K', 'RYL-K') AND rt.deleted_at IS NULL;

INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id)
SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'FAM-PKG' AND rc.deleted_at IS NULL
  AND rt.property_id = rc.property_id AND rt.room_type_code IN ('FAM-T') AND rt.deleted_at IS NULL;

INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id)
SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'MED-PKG' AND rc.deleted_at IS NULL
  AND rt.property_id = rc.property_id AND rt.room_type_code IN ('EXE-K', 'PRE-K') AND rt.deleted_at IS NULL;

INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id)
SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'CREW-RT' AND rc.deleted_at IS NULL
  AND rt.property_id = rc.property_id AND rt.room_type_code IN ('ECO-S', 'ECO-T') AND rt.deleted_at IS NULL;

INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id)
SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'GOV-RT' AND rc.deleted_at IS NULL
  AND rt.property_id = rc.property_id AND rt.room_type_code IN ('BUS-K', 'EXE-K') AND rt.deleted_at IS NULL;

-- GMS/OBH 동일 패턴으로 매핑 (간결하게 처리)
-- GMS
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('ECO-S', 'ECO-T', 'BUS-K') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'CORP-STD' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('BUS-K', 'EXE-K', 'PRE-T') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'AGT-NET' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('ECO-S', 'ECO-T', 'BUS-K') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'LONG-30' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('ECO-S', 'BUS-K') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'MICE-GRP' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('BUS-K', 'EXE-K') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'HON-PKG' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('HNM-K', 'JNR-K', 'PEN-K') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'FAM-PKG' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('FAM-Q') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'MED-PKG' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('EXE-K', 'PRE-T') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'CREW-RT' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('ECO-S', 'ECO-T') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'GOV-RT' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('BUS-K', 'EXE-K') AND rt.deleted_at IS NULL;

-- OBH
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('ECO-S', 'ECO-T', 'BUS-O') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'CORP-STD' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('BUS-O', 'EXE-O', 'PRE-O') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'AGT-NET' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('ECO-S', 'ECO-T', 'BUS-O') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'LONG-30' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('ECO-S', 'BUS-O') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'MICE-GRP' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('BUS-O', 'EXE-O') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'HON-PKG' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('JNR-O', 'PEN-O') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'BEACH-SUM' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('FAM-O', 'CLB-O') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'MED-PKG' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('EXE-O', 'PRE-O') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'CREW-RT' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('ECO-S', 'ECO-T') AND rt.deleted_at IS NULL;
INSERT INTO rt_rate_code_room_type (rate_code_id, room_type_id) SELECT rc.id, rt.id FROM rt_rate_code rc, rm_room_type rt WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'GOV-RT' AND rc.deleted_at IS NULL AND rt.property_id = rc.property_id AND rt.room_type_code IN ('BUS-O', 'EXE-O') AND rt.deleted_at IS NULL;

-- ============================================================
-- 3. 요금 정보 (rt_rate_pricing) - 평일/주말 구분
-- 레이트 코드당 2건 (평일: 월-목, 주말: 금-일) = 60건
-- ============================================================

-- === GMP 요금 정보 ===
-- WKND: 평일 180,000 / 주말 250,000
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total)
SELECT rc.id, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, 'KRW', 180000, 18000, 198000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total)
SELECT rc.id, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, 'KRW', 250000, 25000, 275000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL;

-- CORP-STD: 균일 150,000
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total)
SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 150000, 15000, 165000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'CORP-STD' AND rc.deleted_at IS NULL;

-- AGT-NET: 평일 120,000 / 주말 150,000
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total)
SELECT rc.id, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, 'KRW', 120000, 12000, 132000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'AGT-NET' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total)
SELECT rc.id, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, 'KRW', 150000, 15000, 165000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'AGT-NET' AND rc.deleted_at IS NULL;

-- LONG-30: 균일 100,000 (장기 할인)
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total)
SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 100000, 10000, 110000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'LONG-30' AND rc.deleted_at IS NULL;

-- MICE-GRP: 균일 140,000
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total)
SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 140000, 14000, 154000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'MICE-GRP' AND rc.deleted_at IS NULL;

-- HON-PKG: 평일 350,000 / 주말 400,000
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total)
SELECT rc.id, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, 'KRW', 350000, 35000, 385000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'HON-PKG' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total)
SELECT rc.id, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, 'KRW', 400000, 40000, 440000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'HON-PKG' AND rc.deleted_at IS NULL;

-- FAM-PKG: 평일 220,000 / 주말 280,000
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total)
SELECT rc.id, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, 'KRW', 220000, 22000, 242000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'FAM-PKG' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total)
SELECT rc.id, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, 'KRW', 280000, 28000, 308000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'FAM-PKG' AND rc.deleted_at IS NULL;

-- MED-PKG: 균일 160,000
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total)
SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 160000, 16000, 176000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'MED-PKG' AND rc.deleted_at IS NULL;

-- CREW-RT: 균일 80,000 (심야 할인)
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total)
SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 80000, 8000, 88000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'CREW-RT' AND rc.deleted_at IS NULL;

-- GOV-RT: 균일 130,000
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total)
SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 130000, 13000, 143000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'GOV-RT' AND rc.deleted_at IS NULL;

-- === GMS 요금 정보 (GMP 대비 약 10% 할인) ===
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, 'KRW', 160000, 16000, 176000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, 'KRW', 220000, 22000, 242000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 135000, 13500, 148500 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'CORP-STD' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, 'KRW', 110000, 11000, 121000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'AGT-NET' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, 'KRW', 140000, 14000, 154000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'AGT-NET' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 90000, 9000, 99000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'LONG-30' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 125000, 12500, 137500 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'MICE-GRP' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, 'KRW', 320000, 32000, 352000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'HON-PKG' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, 'KRW', 380000, 38000, 418000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'HON-PKG' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 200000, 20000, 220000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'FAM-PKG' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 145000, 14500, 159500 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'MED-PKG' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 70000, 7000, 77000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'CREW-RT' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 120000, 12000, 132000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'GOV-RT' AND rc.deleted_at IS NULL;

-- === OBH 요금 정보 (GMP 대비 약 10% 할증, 리조트급) ===
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, 'KRW', 200000, 20000, 220000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, 'KRW', 300000, 30000, 330000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 170000, 17000, 187000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'CORP-STD' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, 'KRW', 140000, 14000, 154000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'AGT-NET' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, 'KRW', 180000, 18000, 198000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'AGT-NET' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 110000, 11000, 121000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'LONG-30' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 155000, 15500, 170500 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'MICE-GRP' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, 'KRW', 400000, 40000, 440000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'HON-PKG' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, 'KRW', 480000, 48000, 528000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'HON-PKG' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, 'KRW', 250000, 25000, 275000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'BEACH-SUM' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, 'KRW', 350000, 35000, 385000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'BEACH-SUM' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 175000, 17500, 192500 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'MED-PKG' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 90000, 9000, 99000 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'CREW-RT' AND rc.deleted_at IS NULL;
INSERT INTO rt_rate_pricing (rate_code_id, day_mon, day_tue, day_wed, day_thu, day_fri, day_sat, day_sun, currency, base_supply_price, base_tax, base_total) SELECT rc.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'KRW', 145000, 14500, 159500 FROM rt_rate_code rc WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'GOV-RT' AND rc.deleted_at IS NULL;

-- ============================================================
-- 4. 인원별 요금 (rt_rate_pricing_person) - 주요 레이트 코드에 대해 추가
-- GMP WKND, HON-PKG, FAM-PKG에 대해 인원별 요금 설정
-- ============================================================

-- GMP WKND 평일 - 성인2, 아동1
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
SELECT rp.id, 'ADULT', 1, 180000, 18000, 198000 FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE AND rp.day_fri = FALSE;
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
SELECT rp.id, 'ADULT', 2, 40000, 4000, 44000 FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE AND rp.day_fri = FALSE;
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
SELECT rp.id, 'CHILD', 1, 20000, 2000, 22000 FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE AND rp.day_fri = FALSE;

-- GMP WKND 주말 - 성인2, 아동1
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
SELECT rp.id, 'ADULT', 1, 250000, 25000, 275000 FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL AND rp.day_fri = TRUE AND rp.day_mon = FALSE;
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
SELECT rp.id, 'ADULT', 2, 50000, 5000, 55000 FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL AND rp.day_fri = TRUE AND rp.day_mon = FALSE;
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
SELECT rp.id, 'CHILD', 1, 25000, 2500, 27500 FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'WKND' AND rc.deleted_at IS NULL AND rp.day_fri = TRUE AND rp.day_mon = FALSE;

-- GMP HON-PKG 평일 - 성인2
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
SELECT rp.id, 'ADULT', 1, 350000, 35000, 385000 FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'HON-PKG' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE AND rp.day_fri = FALSE;
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
SELECT rp.id, 'ADULT', 2, 0, 0, 0 FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'HON-PKG' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE AND rp.day_fri = FALSE;

-- GMP FAM-PKG 평일 - 성인2, 아동2
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
SELECT rp.id, 'ADULT', 1, 220000, 22000, 242000 FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'FAM-PKG' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE AND rp.day_fri = FALSE;
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
SELECT rp.id, 'ADULT', 2, 50000, 5000, 55000 FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'FAM-PKG' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE AND rp.day_fri = FALSE;
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
SELECT rp.id, 'CHILD', 1, 30000, 3000, 33000 FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'FAM-PKG' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE AND rp.day_fri = FALSE;
INSERT INTO rt_rate_pricing_person (rate_pricing_id, person_type, person_seq, supply_price, tax, total_price)
SELECT rp.id, 'CHILD', 2, 20000, 2000, 22000 FROM rt_rate_pricing rp JOIN rt_rate_code rc ON rp.rate_code_id = rc.id WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'FAM-PKG' AND rc.deleted_at IS NULL AND rp.day_mon = TRUE AND rp.day_fri = FALSE;

-- ============================================================
-- 5. 레이트 코드 - 유료 서비스 매핑 (rt_rate_code_paid_service)
-- 패키지 요금에 유료 서비스 포함
-- ============================================================

-- GMP 패키지 유료 서비스
INSERT INTO rt_rate_code_paid_service (rate_code_id, paid_service_option_id)
SELECT rc.id, ps.id FROM rt_rate_code rc, rm_paid_service_option ps
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'HON-PKG' AND rc.deleted_at IS NULL
  AND ps.property_id = rc.property_id AND ps.service_option_code IN ('SPA-COUPLE', 'PA-FLOWER', 'RS-WINE');

INSERT INTO rt_rate_code_paid_service (rate_code_id, paid_service_option_id)
SELECT rc.id, ps.id FROM rt_rate_code rc, rm_paid_service_option ps
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'FAM-PKG' AND rc.deleted_at IS NULL
  AND ps.property_id = rc.property_id AND ps.service_option_code IN ('BF-ROOM');

INSERT INTO rt_rate_code_paid_service (rate_code_id, paid_service_option_id)
SELECT rc.id, ps.id FROM rt_rate_code rc, rm_paid_service_option ps
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rc.rate_code = 'MED-PKG' AND rc.deleted_at IS NULL
  AND ps.property_id = rc.property_id AND ps.service_option_code IN ('LAUNDRY-EX', 'BF-ROOM');

-- GMS 패키지 유료 서비스
INSERT INTO rt_rate_code_paid_service (rate_code_id, paid_service_option_id)
SELECT rc.id, ps.id FROM rt_rate_code rc, rm_paid_service_option ps
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'HON-PKG' AND rc.deleted_at IS NULL
  AND ps.property_id = rc.property_id AND ps.service_option_code IN ('SPA-COUPLE', 'PA-FLOWER', 'RS-WINE');

INSERT INTO rt_rate_code_paid_service (rate_code_id, paid_service_option_id)
SELECT rc.id, ps.id FROM rt_rate_code rc, rm_paid_service_option ps
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rc.rate_code = 'FAM-PKG' AND rc.deleted_at IS NULL
  AND ps.property_id = rc.property_id AND ps.service_option_code IN ('BF-ROOM');

-- OBH 패키지 유료 서비스
INSERT INTO rt_rate_code_paid_service (rate_code_id, paid_service_option_id)
SELECT rc.id, ps.id FROM rt_rate_code rc, rm_paid_service_option ps
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'HON-PKG' AND rc.deleted_at IS NULL
  AND ps.property_id = rc.property_id AND ps.service_option_code IN ('SPA-COUPLE', 'PA-FLOWER', 'RS-WINE');

INSERT INTO rt_rate_code_paid_service (rate_code_id, paid_service_option_id)
SELECT rc.id, ps.id FROM rt_rate_code rc, rm_paid_service_option ps
WHERE rc.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rc.rate_code = 'BEACH-SUM' AND rc.deleted_at IS NULL
  AND ps.property_id = rc.property_id AND ps.service_option_code IN ('BF-ROOM', 'MINIBAR-DX');

-- ============================================================
-- 6. 프로모션 코드 (rt_promotion_code) - 30건 추가
-- 12가지 프로모션 타입 모두 포함 (각 타입 최소 2건)
-- 기존: SUMMER26, WINTER26 (프로퍼티당 2건, SEASONAL 타입만)
-- ============================================================

-- GMP 프로모션 코드 (10건)
INSERT INTO rt_promotion_code (property_id, rate_code_id, promotion_code, promotion_start_date, promotion_end_date, description_ko, description_en, promotion_type, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, use_yn, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'CORP-STD' AND deleted_at IS NULL),
     'CORP-SAM', '2026-01-01', '2026-12-31', '삼성그룹 기업 특별 할인 프로모션', 'Samsung Group Corporate Special Discount', 'COMPANY', '-', 20.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'CORP-STD' AND deleted_at IS NULL),
     'CORP-LG', '2026-01-01', '2026-12-31', 'LG그룹 기업 특별 할인 프로모션', 'LG Group Corporate Special Discount', 'COMPANY', '-', 15.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'WKND' AND deleted_at IS NULL),
     'SPRING26', '2026-03-15', '2026-05-31', '2026 봄 벚꽃 시즌 특별 프로모션', 'Spring Cherry Blossom Season Special 2026', 'SEASONAL', '-', 10.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'WKND' AND deleted_at IS NULL),
     'XMAS26', '2026-12-20', '2026-12-31', '2026 크리스마스 특별 이벤트 프로모션', 'Christmas Special Event Promotion 2026', 'EVENT', '-', 5.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'WKND' AND deleted_at IS NULL),
     'EARLY14', '2026-01-01', '2026-12-31', '14일 전 조기예약 시 15% 할인', '15% Discount for Booking 14 Days in Advance', 'EARLY_BIRD', '-', 15.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 7, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'WKND' AND deleted_at IS NULL),
     'LASTMIN3', '2026-01-01', '2026-12-31', '체크인 3일 전 직전예약 10% 할인', '10% Discount for Last-Minute Booking (3 Days)', 'LAST_MINUTE', '-', 10.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 8, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'AGT-NET' AND deleted_at IS NULL),
     'OTA-BOOK', '2026-01-01', '2026-12-31', 'Booking.com OTA 전용 추가 할인', 'Booking.com OTA Exclusive Additional Discount', 'OTA', '-', 5.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 9, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'WKND' AND deleted_at IS NULL),
     'MEMBER-G', '2026-01-01', '2026-12-31', '골드 회원 전용 12% 할인 프로모션', 'Gold Member Exclusive 12% Discount', 'MEMBER', '-', 12.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 10, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'MICE-GRP' AND deleted_at IS NULL),
     'GROUP20', '2026-01-01', '2026-12-31', '20실 이상 단체 예약 시 추가 할인', 'Additional Discount for Group Booking 20+ Rooms', 'GROUP', '-', 10.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 11, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rate_code = 'GOV-RT' AND deleted_at IS NULL),
     'GOV-2026', '2026-01-01', '2026-12-31', '2026년 관공서 관용 특별 요금', '2026 Government Official Special Rate', 'GOVERNMENT', '-', 25.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 12, NOW(), 'admin');

-- GMS 프로모션 코드 (10건)
INSERT INTO rt_promotion_code (property_id, rate_code_id, promotion_code, promotion_start_date, promotion_end_date, description_ko, description_en, promotion_type, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, use_yn, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'CORP-STD' AND deleted_at IS NULL),
     'CORP-HYUN', '2026-01-01', '2026-12-31', '현대그룹 기업 특별 할인', 'Hyundai Group Corporate Special Discount', 'COMPANY', '-', 18.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'WKND' AND deleted_at IS NULL),
     'PROMO-NEW', '2026-04-01', '2026-06-30', '신규 오픈 기념 특별 프로모션', 'Grand Opening Celebration Promotion', 'PROMOTION', '-', 20.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'AGT-NET' AND deleted_at IS NULL),
     'OTA-AGODA', '2026-01-01', '2026-12-31', 'Agoda OTA 전용 추가 할인', 'Agoda OTA Exclusive Additional Discount', 'OTA', '-', 7.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'HON-PKG' AND deleted_at IS NULL),
     'PKG-HONEY', '2026-01-01', '2026-12-31', '허니문 패키지 스파+조식 포함 특가', 'Honeymoon Package with Spa+Breakfast Special', 'PACKAGE', '-', 10.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'WKND' AND deleted_at IS NULL),
     'AUTUMN26', '2026-09-01', '2026-11-30', '2026 가을 단풍 시즌 특별 프로모션', 'Autumn Foliage Season Special 2026', 'SEASONAL', '-', 8.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 7, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'WKND' AND deleted_at IS NULL),
     'NEWYEAR27', '2026-12-28', '2027-01-05', '2027 신년 카운트다운 이벤트', 'New Year 2027 Countdown Event', 'EVENT', '-', 5.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 8, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'WKND' AND deleted_at IS NULL),
     'EARLY21', '2026-01-01', '2026-12-31', '21일 전 조기예약 시 20% 할인', '20% Discount for Booking 21 Days in Advance', 'EARLY_BIRD', '-', 20.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 9, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'WKND' AND deleted_at IS NULL),
     'MEMBER-V', '2026-01-01', '2026-12-31', 'VIP 회원 전용 15% 할인', 'VIP Member Exclusive 15% Discount', 'MEMBER', '-', 15.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 10, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'LONG-30' AND deleted_at IS NULL),
     'LONG60-DIS', '2026-01-01', '2027-12-31', '60일 이상 장기투숙 추가 할인', 'Additional Discount for 60+ Day Long Stay', 'LONG_STAY', '-', 15.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 11, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rate_code = 'GOV-RT' AND deleted_at IS NULL),
     'GOV-SEOUL', '2026-01-01', '2026-12-31', '서울시 관공서 관용 특별 요금', 'Seoul City Government Official Special Rate', 'GOVERNMENT', '-', 22.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 12, NOW(), 'admin');

-- OBH 프로모션 코드 (10건)
INSERT INTO rt_promotion_code (property_id, rate_code_id, promotion_code, promotion_start_date, promotion_end_date, description_ko, description_en, promotion_type, down_up_sign, down_up_value, down_up_unit, rounding_decimal_point, rounding_digits, rounding_method, use_yn, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'CORP-STD' AND deleted_at IS NULL),
     'CORP-SK', '2026-01-01', '2026-12-31', 'SK그룹 기업 특별 할인', 'SK Group Corporate Special Discount', 'COMPANY', '-', 17.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'WKND' AND deleted_at IS NULL),
     'PROMO-SUM', '2026-06-15', '2026-08-31', '해운대 여름 바다 프로모션 - 워터파크 무료', 'Haeundae Summer Beach Promotion - Free Waterpark', 'PROMOTION', '-', 0.00, 'AMOUNT', 0, 0, 'ROUND', TRUE, 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'AGT-NET' AND deleted_at IS NULL),
     'OTA-EXPEDIA', '2026-01-01', '2026-12-31', 'Expedia OTA 전용 추가 할인', 'Expedia OTA Exclusive Additional Discount', 'OTA', '-', 6.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'BEACH-SUM' AND deleted_at IS NULL),
     'PKG-BEACH', '2026-06-01', '2026-08-31', '해운대 비치 패키지 - 비치파라솔+음료 포함', 'Haeundae Beach Package - Parasol+Drinks Included', 'PACKAGE', '-', 5.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'WKND' AND deleted_at IS NULL),
     'CHUSEOK26', '2026-09-25', '2026-10-05', '2026 추석 연휴 특별 프로모션', 'Chuseok Holiday Special 2026', 'SEASONAL', '-', 5.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 7, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'WKND' AND deleted_at IS NULL),
     'FIREWORK', '2026-10-01', '2026-10-10', '부산 불꽃축제 이벤트 프로모션', 'Busan Fireworks Festival Event Promotion', 'EVENT', '-', 0.00, 'AMOUNT', 0, 0, 'ROUND', TRUE, 8, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'WKND' AND deleted_at IS NULL),
     'LASTMIN7', '2026-01-01', '2026-12-31', '체크인 7일 전 직전예약 12% 할인', '12% Discount for Last-Minute Booking (7 Days)', 'LAST_MINUTE', '-', 12.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 9, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'MICE-GRP' AND deleted_at IS NULL),
     'GROUP10', '2026-01-01', '2026-12-31', '10실 이상 단체 예약 시 8% 추가 할인', '8% Additional Discount for Group Booking 10+ Rooms', 'GROUP', '-', 8.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 10, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'LONG-30' AND deleted_at IS NULL),
     'LONG30-DIS', '2026-01-01', '2027-12-31', '30일 이상 장기투숙 추가 10% 할인', 'Additional 10% Discount for 30+ Day Stay', 'LONG_STAY', '-', 10.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 11, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rt_rate_code WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rate_code = 'GOV-RT' AND deleted_at IS NULL),
     'GOV-BUSAN', '2026-01-01', '2026-12-31', '부산시 관공서 관용 특별 요금', 'Busan City Government Official Special Rate', 'GOVERNMENT', '-', 20.00, 'PERCENT', 0, 0, 'ROUND', TRUE, 12, NOW(), 'admin');

-- ============================================================
-- 7. 시퀀스 리셋
-- ============================================================
SELECT setval('rt_rate_code_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rt_rate_code));
SELECT setval('rt_rate_code_room_type_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rt_rate_code_room_type));
SELECT setval('rt_rate_code_paid_service_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rt_rate_code_paid_service));
SELECT setval('rt_rate_pricing_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rt_rate_pricing));
SELECT setval('rt_rate_pricing_person_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rt_rate_pricing_person));
SELECT setval('rt_promotion_code_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rt_promotion_code));
