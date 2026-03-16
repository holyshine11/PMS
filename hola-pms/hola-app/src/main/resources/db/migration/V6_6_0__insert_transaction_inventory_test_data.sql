-- =============================================
-- V6_6_0: 트랜잭션 코드 + 재고 관리 테스트 데이터
-- 3개 프로퍼티 (GMP, GMS, OBH) 전체에 다양한 데이터 삽입
-- =============================================

-- ===== 0. 기존 테스트 데이터 정리 (FK 순서 고려) =====
-- PaidServiceOption의 TC/Inventory 참조 해제
UPDATE rm_paid_service_option SET transaction_code_id = NULL WHERE transaction_code_id IS NOT NULL;
UPDATE rm_paid_service_option SET inventory_item_id = NULL WHERE inventory_item_id IS NOT NULL;
-- 트랜잭션 코드 → 그룹 순서로 삭제
DELETE FROM rm_transaction_code;
DELETE FROM rm_transaction_code_group;
-- 재고 가용성 → 아이템 순서로 삭제
DELETE FROM rm_inventory_availability;
DELETE FROM rm_inventory_item;

-- ===== 1. 트랜잭션 코드 그룹 - MAIN (5 per property = 15) =====

-- GMP MAIN 그룹
INSERT INTO rm_transaction_code_group (property_id, group_code, group_name_ko, group_name_en, group_type, parent_group_id, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'ROOM', '객실매출', 'Room Revenue', 'MAIN', NULL, 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'FNB', '식음매출', 'Food & Beverage', 'MAIN', NULL, 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'MISC', '기타매출', 'Miscellaneous', 'MAIN', NULL, 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'TAX', '세금', 'Tax & Surcharge', 'MAIN', NULL, 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'PYMT', '결제', 'Payment', 'MAIN', NULL, 5, NOW(), 'admin');

-- GMS MAIN 그룹
INSERT INTO rm_transaction_code_group (property_id, group_code, group_name_ko, group_name_en, group_type, parent_group_id, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'ROOM', '객실매출', 'Room Revenue', 'MAIN', NULL, 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'FNB', '식음매출', 'Food & Beverage', 'MAIN', NULL, 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'MISC', '기타매출', 'Miscellaneous', 'MAIN', NULL, 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'TAX', '세금', 'Tax & Surcharge', 'MAIN', NULL, 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'PYMT', '결제', 'Payment', 'MAIN', NULL, 5, NOW(), 'admin');

-- OBH MAIN 그룹
INSERT INTO rm_transaction_code_group (property_id, group_code, group_name_ko, group_name_en, group_type, parent_group_id, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'ROOM', '객실매출', 'Room Revenue', 'MAIN', NULL, 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'FNB', '식음매출', 'Food & Beverage', 'MAIN', NULL, 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'MISC', '기타매출', 'Miscellaneous', 'MAIN', NULL, 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'TAX', '세금', 'Tax & Surcharge', 'MAIN', NULL, 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'PYMT', '결제', 'Payment', 'MAIN', NULL, 5, NOW(), 'admin');

-- ===== 2. 트랜잭션 코드 그룹 - SUB (GMP: 17개, GMS: 15개, OBH: 18개) =====

-- GMP SUB 그룹
INSERT INTO rm_transaction_code_group (property_id, group_code, group_name_ko, group_name_en, group_type, parent_group_id, sort_order, created_at, created_by)
VALUES
    -- ROOM 하위
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'ROOM-CHRG', '객실요금', 'Room Charge',  'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'ROOM'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'ROOM-PKG', '객실패키지', 'Room Package', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'ROOM'), 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'ROOM-ADJ', '객실조정', 'Room Adjustment', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'ROOM'), 3, NOW(), 'admin'),
    -- FNB 하위
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'FNB-BF', '조식', 'Breakfast', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'FNB'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'FNB-REST', '레스토랑', 'Restaurant', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'FNB'), 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'FNB-BAR', '바/라운지', 'Bar & Lounge', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'FNB'), 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'FNB-ROOM', '룸서비스', 'Room Service', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'FNB'), 4, NOW(), 'admin'),
    -- MISC 하위
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'MISC-SPA', '스파/피트니스', 'Spa & Fitness', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'MISC-LDRY', '세탁', 'Laundry', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC'), 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'MISC-MINI', '미니바', 'Minibar', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC'), 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'MISC-EQPT', '장비대여', 'Equipment Rental', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC'), 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'MISC-BIZ', '비즈니스센터', 'Business Center', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC'), 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'MISC-ETC', '기타서비스', 'Other Services', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC'), 6, NOW(), 'admin'),
    -- TAX 하위
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'TAX-VAT', '부가세', 'VAT', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'TAX'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'TAX-SVC', '봉사료', 'Service Charge', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'TAX'), 2, NOW(), 'admin'),
    -- PYMT 하위
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'PYMT-CASH', '현금', 'Cash', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'PYMT'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'PYMT-CARD', '카드', 'Credit Card', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'PYMT'), 2, NOW(), 'admin');

-- GMS SUB 그룹
INSERT INTO rm_transaction_code_group (property_id, group_code, group_name_ko, group_name_en, group_type, parent_group_id, sort_order, created_at, created_by)
VALUES
    -- ROOM 하위
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'ROOM-CHRG', '객실요금', 'Room Charge', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'ROOM'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'ROOM-PKG', '객실패키지', 'Room Package', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'ROOM'), 2, NOW(), 'admin'),
    -- FNB 하위
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'FNB-BF', '조식', 'Breakfast', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'FNB'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'FNB-REST', '레스토랑', 'Restaurant', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'FNB'), 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'FNB-ROOM', '룸서비스', 'Room Service', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'FNB'), 3, NOW(), 'admin'),
    -- MISC 하위
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'MISC-SPA', '스파/피트니스', 'Spa & Fitness', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'MISC-LDRY', '세탁', 'Laundry', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC'), 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'MISC-MINI', '미니바', 'Minibar', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC'), 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'MISC-EQPT', '장비대여', 'Equipment Rental', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC'), 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'MISC-MTG', '회의실', 'Meeting Room', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC'), 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'MISC-ETC', '기타서비스', 'Other Services', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC'), 6, NOW(), 'admin'),
    -- TAX 하위
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'TAX-VAT', '부가세', 'VAT', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'TAX'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'TAX-SVC', '봉사료', 'Service Charge', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'TAX'), 2, NOW(), 'admin'),
    -- PYMT 하위
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'PYMT-CASH', '현금', 'Cash', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'PYMT'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'PYMT-CARD', '카드', 'Credit Card', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'PYMT'), 2, NOW(), 'admin');

-- OBH SUB 그룹
INSERT INTO rm_transaction_code_group (property_id, group_code, group_name_ko, group_name_en, group_type, parent_group_id, sort_order, created_at, created_by)
VALUES
    -- ROOM 하위
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'ROOM-CHRG', '객실요금', 'Room Charge', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'ROOM'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'ROOM-PKG', '객실패키지', 'Room Package', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'ROOM'), 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'ROOM-ADJ', '객실조정', 'Room Adjustment', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'ROOM'), 3, NOW(), 'admin'),
    -- FNB 하위
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'FNB-BF', '조식', 'Breakfast', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'FNB-REST', '레스토랑', 'Restaurant', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB'), 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'FNB-BAR', '바/라운지', 'Bar & Lounge', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB'), 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'FNB-ROOM', '룸서비스', 'Room Service', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB'), 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'FNB-POOL', '풀바', 'Pool Bar', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB'), 5, NOW(), 'admin'),
    -- MISC 하위
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'MISC-SPA', '스파/피트니스', 'Spa & Fitness', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'MISC-LDRY', '세탁', 'Laundry', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC'), 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'MISC-MINI', '미니바', 'Minibar', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC'), 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'MISC-EQPT', '장비대여', 'Equipment Rental', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC'), 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'MISC-WATR', '워터스포츠', 'Water Sports', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC'), 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'MISC-ETC', '기타서비스', 'Other Services', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC'), 6, NOW(), 'admin'),
    -- TAX 하위
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'TAX-VAT', '부가세', 'VAT', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'TAX'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'TAX-SVC', '봉사료', 'Service Charge', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'TAX'), 2, NOW(), 'admin'),
    -- PYMT 하위
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'PYMT-CASH', '현금', 'Cash', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'PYMT'), 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'PYMT-CARD', '카드', 'Credit Card', 'SUB',
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'PYMT'), 2, NOW(), 'admin');


-- ===== 3. 트랜잭션 코드 - GMP (34개) =====
INSERT INTO rm_transaction_code (property_id, transaction_group_id, transaction_code, code_name_ko, code_name_en, revenue_category, code_type, sort_order, created_at, created_by)
VALUES
    -- ROOM-CHRG: 객실요금
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'ROOM-CHRG'),
     '1000', '객실 기본요금', 'Room Charge', 'LODGING', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'ROOM-CHRG'),
     '1001', '객실 추가요금', 'Extra Room Charge', 'LODGING', 'CHARGE', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'ROOM-CHRG'),
     '1010', '업그레이드 차액', 'Upgrade Charge', 'LODGING', 'CHARGE', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'ROOM-CHRG'),
     '1020', '조기 체크인 요금', 'Early Check-in Fee', 'LODGING', 'CHARGE', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'ROOM-CHRG'),
     '1021', '레이트 체크아웃 요금', 'Late Check-out Fee', 'LODGING', 'CHARGE', 5, NOW(), 'admin'),
    -- ROOM-PKG: 객실패키지
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'ROOM-PKG'),
     '1100', '패키지 객실', 'Package Room', 'LODGING', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'ROOM-PKG'),
     '1101', '패키지 할인', 'Package Discount', 'LODGING', 'CHARGE', 2, NOW(), 'admin'),
    -- ROOM-ADJ: 객실조정
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'ROOM-ADJ'),
     '1200', '객실 요금 조정', 'Room Rate Adjustment', 'LODGING', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'ROOM-ADJ'),
     '1201', '컴플리멘터리', 'Complimentary', 'LODGING', 'CHARGE', 2, NOW(), 'admin'),
    -- FNB-BF: 조식
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'FNB-BF'),
     '2000', '조식 뷔페', 'Breakfast Buffet', 'FOOD_BEVERAGE', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'FNB-BF'),
     '2001', '추가 조식', 'Extra Breakfast', 'FOOD_BEVERAGE', 'CHARGE', 2, NOW(), 'admin'),
    -- FNB-REST: 레스토랑
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'FNB-REST'),
     '2100', '레스토랑 식사', 'Restaurant Dining', 'FOOD_BEVERAGE', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'FNB-REST'),
     '2101', '레스토랑 음료', 'Restaurant Beverage', 'FOOD_BEVERAGE', 'CHARGE', 2, NOW(), 'admin'),
    -- FNB-BAR: 바
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'FNB-BAR'),
     '2200', '바 음료', 'Bar Beverage', 'FOOD_BEVERAGE', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'FNB-BAR'),
     '2201', '바 스낵', 'Bar Snack', 'FOOD_BEVERAGE', 'CHARGE', 2, NOW(), 'admin'),
    -- FNB-ROOM: 룸서비스
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'FNB-ROOM'),
     '2300', '룸서비스 기본', 'Room Service Basic', 'FOOD_BEVERAGE', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'FNB-ROOM'),
     '2301', '룸서비스 프리미엄', 'Room Service Premium', 'FOOD_BEVERAGE', 'CHARGE', 2, NOW(), 'admin'),
    -- MISC-SPA: 스파
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC-SPA'),
     '3000', '스파 기본', 'Spa Basic', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC-SPA'),
     '3001', '스파 VIP', 'Spa VIP', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC-SPA'),
     '3010', '피트니스 이용', 'Fitness Usage', 'MISC', 'CHARGE', 3, NOW(), 'admin'),
    -- MISC-LDRY: 세탁
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC-LDRY'),
     '3100', '세탁 서비스', 'Laundry Service', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC-LDRY'),
     '3101', '드라이클리닝', 'Dry Cleaning', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    -- MISC-MINI: 미니바
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC-MINI'),
     '3200', '미니바 프리미엄', 'Minibar Premium', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC-MINI'),
     '3201', '미니바 일반', 'Minibar Standard', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    -- MISC-EQPT: 장비대여
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC-EQPT'),
     '3400', '엑스트라 베드', 'Extra Bed', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC-EQPT'),
     '3401', '아기 침대', 'Baby Crib', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC-EQPT'),
     '3402', '이동식 침대', 'Rollaway Bed', 'MISC', 'CHARGE', 3, NOW(), 'admin'),
    -- MISC-BIZ: 비즈니스센터
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC-BIZ'),
     '3500', '비즈니스센터 이용', 'Business Center Usage', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC-BIZ'),
     '3501', '인쇄/복사 서비스', 'Print & Copy Service', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    -- MISC-ETC: 기타
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC-ETC'),
     '3600', '셔틀 서비스', 'Shuttle Service', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'MISC-ETC'),
     '3601', '주차 서비스', 'Parking Service', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    -- TAX
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'TAX-VAT'),
     '8000', '부가세', 'VAT', 'TAX', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'TAX-SVC'),
     '8100', '봉사료', 'Service Charge', 'TAX', 'CHARGE', 1, NOW(), 'admin'),
    -- PYMT
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'PYMT-CASH'),
     '9000', '현금 결제', 'Cash Payment', 'NON_REVENUE', 'PAYMENT', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'PYMT-CASH'),
     '9001', '현금 환불', 'Cash Refund', 'NON_REVENUE', 'PAYMENT', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'PYMT-CARD'),
     '9100', '카드 결제', 'Card Payment', 'NON_REVENUE', 'PAYMENT', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'PYMT-CARD'),
     '9101', '카드 환불', 'Card Refund', 'NON_REVENUE', 'PAYMENT', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND group_code = 'PYMT-CARD'),
     '9110', '포인트 결제', 'Point Payment', 'NON_REVENUE', 'PAYMENT', 3, NOW(), 'admin');


-- ===== 4. 트랜잭션 코드 - GMS (28개) =====
INSERT INTO rm_transaction_code (property_id, transaction_group_id, transaction_code, code_name_ko, code_name_en, revenue_category, code_type, sort_order, created_at, created_by)
VALUES
    -- ROOM-CHRG
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'ROOM-CHRG'),
     '1000', '객실 기본요금', 'Room Charge', 'LODGING', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'ROOM-CHRG'),
     '1001', '객실 추가요금', 'Extra Room Charge', 'LODGING', 'CHARGE', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'ROOM-CHRG'),
     '1010', '업그레이드 차액', 'Upgrade Charge', 'LODGING', 'CHARGE', 3, NOW(), 'admin'),
    -- ROOM-PKG
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'ROOM-PKG'),
     '1100', '패키지 객실', 'Package Room', 'LODGING', 'CHARGE', 1, NOW(), 'admin'),
    -- FNB-BF
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'FNB-BF'),
     '2000', '조식 뷔페', 'Breakfast Buffet', 'FOOD_BEVERAGE', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'FNB-BF'),
     '2001', '추가 조식', 'Extra Breakfast', 'FOOD_BEVERAGE', 'CHARGE', 2, NOW(), 'admin'),
    -- FNB-REST
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'FNB-REST'),
     '2100', '레스토랑 식사', 'Restaurant Dining', 'FOOD_BEVERAGE', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'FNB-REST'),
     '2101', '레스토랑 음료', 'Restaurant Beverage', 'FOOD_BEVERAGE', 'CHARGE', 2, NOW(), 'admin'),
    -- FNB-ROOM
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'FNB-ROOM'),
     '2300', '룸서비스 기본', 'Room Service Basic', 'FOOD_BEVERAGE', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'FNB-ROOM'),
     '2301', '룸서비스 프리미엄', 'Room Service Premium', 'FOOD_BEVERAGE', 'CHARGE', 2, NOW(), 'admin'),
    -- MISC-SPA
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC-SPA'),
     '3000', '스파 기본', 'Spa Basic', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC-SPA'),
     '3001', '스파 VIP', 'Spa VIP', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    -- MISC-LDRY
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC-LDRY'),
     '3100', '세탁 서비스', 'Laundry Service', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC-LDRY'),
     '3101', '드라이클리닝', 'Dry Cleaning', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    -- MISC-MINI
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC-MINI'),
     '3200', '미니바 프리미엄', 'Minibar Premium', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    -- MISC-EQPT
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC-EQPT'),
     '3400', '엑스트라 베드', 'Extra Bed', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC-EQPT'),
     '3401', '아기 침대', 'Baby Crib', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    -- MISC-MTG: 회의실
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC-MTG'),
     '3500', '회의실 대관 (소)', 'Meeting Room (Small)', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC-MTG'),
     '3501', '회의실 대관 (대)', 'Meeting Room (Large)', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC-MTG'),
     '3502', 'AV 장비 대여', 'AV Equipment Rental', 'MISC', 'CHARGE', 3, NOW(), 'admin'),
    -- MISC-ETC
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'MISC-ETC'),
     '3600', '주차 서비스', 'Parking Service', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    -- TAX
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'TAX-VAT'),
     '8000', '부가세', 'VAT', 'TAX', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'TAX-SVC'),
     '8100', '봉사료', 'Service Charge', 'TAX', 'CHARGE', 1, NOW(), 'admin'),
    -- PYMT
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'PYMT-CASH'),
     '9000', '현금 결제', 'Cash Payment', 'NON_REVENUE', 'PAYMENT', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'PYMT-CASH'),
     '9001', '현금 환불', 'Cash Refund', 'NON_REVENUE', 'PAYMENT', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'PYMT-CARD'),
     '9100', '카드 결제', 'Card Payment', 'NON_REVENUE', 'PAYMENT', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'PYMT-CARD'),
     '9101', '카드 환불', 'Card Refund', 'NON_REVENUE', 'PAYMENT', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND group_code = 'PYMT-CARD'),
     '9110', '계좌이체', 'Bank Transfer', 'NON_REVENUE', 'PAYMENT', 3, NOW(), 'admin');


-- ===== 5. 트랜잭션 코드 - OBH (38개, 리조트 특화 코드 포함) =====
INSERT INTO rm_transaction_code (property_id, transaction_group_id, transaction_code, code_name_ko, code_name_en, revenue_category, code_type, sort_order, created_at, created_by)
VALUES
    -- ROOM-CHRG
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'ROOM-CHRG'),
     '1000', '객실 기본요금', 'Room Charge', 'LODGING', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'ROOM-CHRG'),
     '1001', '객실 추가요금', 'Extra Room Charge', 'LODGING', 'CHARGE', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'ROOM-CHRG'),
     '1010', '업그레이드 차액', 'Upgrade Charge', 'LODGING', 'CHARGE', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'ROOM-CHRG'),
     '1020', '조기 체크인 요금', 'Early Check-in Fee', 'LODGING', 'CHARGE', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'ROOM-CHRG'),
     '1021', '레이트 체크아웃 요금', 'Late Check-out Fee', 'LODGING', 'CHARGE', 5, NOW(), 'admin'),
    -- ROOM-PKG
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'ROOM-PKG'),
     '1100', '패키지 객실', 'Package Room', 'LODGING', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'ROOM-PKG'),
     '1101', '리조트 패키지', 'Resort Package', 'LODGING', 'CHARGE', 2, NOW(), 'admin'),
    -- ROOM-ADJ
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'ROOM-ADJ'),
     '1200', '객실 요금 조정', 'Room Rate Adjustment', 'LODGING', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'ROOM-ADJ'),
     '1201', '컴플리멘터리', 'Complimentary', 'LODGING', 'CHARGE', 2, NOW(), 'admin'),
    -- FNB-BF
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB-BF'),
     '2000', '조식 뷔페', 'Breakfast Buffet', 'FOOD_BEVERAGE', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB-BF'),
     '2001', '추가 조식', 'Extra Breakfast', 'FOOD_BEVERAGE', 'CHARGE', 2, NOW(), 'admin'),
    -- FNB-REST
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB-REST'),
     '2100', '레스토랑 식사', 'Restaurant Dining', 'FOOD_BEVERAGE', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB-REST'),
     '2101', '씨푸드 레스토랑', 'Seafood Restaurant', 'FOOD_BEVERAGE', 'CHARGE', 2, NOW(), 'admin'),
    -- FNB-BAR
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB-BAR'),
     '2200', '바 음료', 'Bar Beverage', 'FOOD_BEVERAGE', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB-BAR'),
     '2201', '루프탑 바', 'Rooftop Bar', 'FOOD_BEVERAGE', 'CHARGE', 2, NOW(), 'admin'),
    -- FNB-ROOM
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB-ROOM'),
     '2300', '룸서비스 기본', 'Room Service Basic', 'FOOD_BEVERAGE', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB-ROOM'),
     '2301', '룸서비스 프리미엄', 'Room Service Premium', 'FOOD_BEVERAGE', 'CHARGE', 2, NOW(), 'admin'),
    -- FNB-POOL: 풀바 (OBH 특화)
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB-POOL'),
     '2400', '풀사이드 음료', 'Poolside Beverage', 'FOOD_BEVERAGE', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'FNB-POOL'),
     '2401', '풀사이드 스낵', 'Poolside Snack', 'FOOD_BEVERAGE', 'CHARGE', 2, NOW(), 'admin'),
    -- MISC-SPA
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-SPA'),
     '3000', '스파 기본', 'Spa Basic', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-SPA'),
     '3001', '스파 VIP', 'Spa VIP', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-SPA'),
     '3010', '피트니스 이용', 'Fitness Usage', 'MISC', 'CHARGE', 3, NOW(), 'admin'),
    -- MISC-LDRY
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-LDRY'),
     '3100', '세탁 서비스', 'Laundry Service', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    -- MISC-MINI
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-MINI'),
     '3200', '미니바 프리미엄', 'Minibar Premium', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-MINI'),
     '3201', '미니바 일반', 'Minibar Standard', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    -- MISC-EQPT
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-EQPT'),
     '3400', '엑스트라 베드', 'Extra Bed', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-EQPT'),
     '3401', '아기 침대', 'Baby Crib', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-EQPT'),
     '3402', '비치 파라솔 세트', 'Beach Parasol Set', 'MISC', 'CHARGE', 3, NOW(), 'admin'),
    -- MISC-WATR: 워터스포츠 (OBH 특화)
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-WATR'),
     '3700', '카약 대여', 'Kayak Rental', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-WATR'),
     '3701', '서핑보드 대여', 'Surfboard Rental', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-WATR'),
     '3702', '스노쿨링 세트', 'Snorkeling Set', 'MISC', 'CHARGE', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-WATR'),
     '3703', '수영장 카바나', 'Pool Cabana', 'MISC', 'CHARGE', 4, NOW(), 'admin'),
    -- MISC-ETC
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-ETC'),
     '3600', '셔틀 서비스', 'Shuttle Service', 'MISC', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'MISC-ETC'),
     '3601', '주차 서비스', 'Parking Service', 'MISC', 'CHARGE', 2, NOW(), 'admin'),
    -- TAX
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'TAX-VAT'),
     '8000', '부가세', 'VAT', 'TAX', 'CHARGE', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'TAX-SVC'),
     '8100', '봉사료', 'Service Charge', 'TAX', 'CHARGE', 1, NOW(), 'admin'),
    -- PYMT
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'PYMT-CASH'),
     '9000', '현금 결제', 'Cash Payment', 'NON_REVENUE', 'PAYMENT', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'PYMT-CARD'),
     '9100', '카드 결제', 'Card Payment', 'NON_REVENUE', 'PAYMENT', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_transaction_code_group WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND group_code = 'PYMT-CARD'),
     '9101', '카드 환불', 'Card Refund', 'NON_REVENUE', 'PAYMENT', 2, NOW(), 'admin');


-- ===== 6. 재고 아이템 마스터 (GMP: 7, GMS: 5, OBH: 9 = 21) =====

-- GMP: 5성급 시내 호텔 재고
INSERT INTO rm_inventory_item (property_id, item_code, item_name_ko, item_name_en, item_type, management_type, external_system_code, total_quantity, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'EB-001', '엑스트라 베드', 'Extra Bed', 'EXTRA_BED', 'INTERNAL', NULL, 30, 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'CB-001', '아기 침대', 'Baby Crib', 'CRIB', 'INTERNAL', NULL, 15, 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'RB-001', '이동식 침대', 'Rollaway Bed', 'ROLLAWAY', 'INTERNAL', NULL, 20, 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'EQ-001', '가습기', 'Humidifier', 'EQUIPMENT', 'INTERNAL', NULL, 25, 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'EQ-002', '공기청정기', 'Air Purifier', 'EQUIPMENT', 'INTERNAL', NULL, 20, 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'EQ-003', '블루투스 스피커', 'Bluetooth Speaker', 'EQUIPMENT', 'INTERNAL', NULL, 30, 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'EQ-004', '노트북 거치대', 'Laptop Stand', 'EQUIPMENT', 'INTERNAL', NULL, 15, 7, NOW(), 'admin');

-- GMS: 4성급 비즈니스 호텔 재고
INSERT INTO rm_inventory_item (property_id, item_code, item_name_ko, item_name_en, item_type, management_type, external_system_code, total_quantity, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'EB-001', '엑스트라 베드', 'Extra Bed', 'EXTRA_BED', 'INTERNAL', NULL, 20, 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'CB-001', '아기 침대', 'Baby Crib', 'CRIB', 'INTERNAL', NULL, 10, 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'RB-001', '이동식 침대', 'Rollaway Bed', 'ROLLAWAY', 'INTERNAL', NULL, 15, 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'EQ-001', '가습기', 'Humidifier', 'EQUIPMENT', 'INTERNAL', NULL, 15, 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'EQ-002', '공기청정기', 'Air Purifier', 'EQUIPMENT', 'INTERNAL', NULL, 10, 5, NOW(), 'admin');

-- OBH: 5성급 리조트 재고 (워터스포츠/비치 장비 포함)
INSERT INTO rm_inventory_item (property_id, item_code, item_name_ko, item_name_en, item_type, management_type, external_system_code, total_quantity, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'EB-001', '엑스트라 베드', 'Extra Bed', 'EXTRA_BED', 'INTERNAL', NULL, 40, 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'CB-001', '아기 침대', 'Baby Crib', 'CRIB', 'INTERNAL', NULL, 20, 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'RB-001', '이동식 침대', 'Rollaway Bed', 'ROLLAWAY', 'INTERNAL', NULL, 25, 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'EQ-001', '가습기', 'Humidifier', 'EQUIPMENT', 'INTERNAL', NULL, 30, 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'EQ-002', '공기청정기', 'Air Purifier', 'EQUIPMENT', 'INTERNAL', NULL, 25, 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'EQ-003', '블루투스 스피커', 'Bluetooth Speaker', 'EQUIPMENT', 'INTERNAL', NULL, 35, 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'EQ-004', '비치 파라솔 세트', 'Beach Parasol Set', 'EQUIPMENT', 'INTERNAL', NULL, 50, 7, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'EQ-005', '서핑보드', 'Surfboard', 'EQUIPMENT', 'INTERNAL', NULL, 20, 8, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'EQ-006', '카약', 'Kayak', 'EQUIPMENT', 'INTERNAL', NULL, 12, 9, NOW(), 'admin');


-- ===== 7. 일자별 재고 가용성 (2026-03-01 ~ 2026-05-31, 92일) =====
-- 주중/주말에 따라 예약률 차등 적용

-- GMP 재고 가용성
INSERT INTO rm_inventory_availability (inventory_item_id, availability_date, available_count, reserved_count)
SELECT
    inv.id,
    d::date,
    inv.total_quantity,
    CASE
        WHEN EXTRACT(DOW FROM d) IN (0, 6)  -- 주말: 예약률 높음
            THEN LEAST(FLOOR(inv.total_quantity * (0.3 + (EXTRACT(DOY FROM d) % 5) * 0.05))::int, inv.total_quantity)
        ELSE  -- 주중: 예약률 낮음
            LEAST(FLOOR(inv.total_quantity * (0.1 + (EXTRACT(DOY FROM d) % 7) * 0.02))::int, inv.total_quantity)
    END
FROM rm_inventory_item inv
CROSS JOIN generate_series('2026-03-01'::date, '2026-05-31'::date, '1 day'::interval) d
WHERE inv.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP');

-- GMS 재고 가용성
INSERT INTO rm_inventory_availability (inventory_item_id, availability_date, available_count, reserved_count)
SELECT
    inv.id,
    d::date,
    inv.total_quantity,
    CASE
        WHEN EXTRACT(DOW FROM d) IN (0, 6)
            THEN LEAST(FLOOR(inv.total_quantity * (0.25 + (EXTRACT(DOY FROM d) % 4) * 0.04))::int, inv.total_quantity)
        ELSE
            LEAST(FLOOR(inv.total_quantity * (0.15 + (EXTRACT(DOY FROM d) % 6) * 0.02))::int, inv.total_quantity)
    END
FROM rm_inventory_item inv
CROSS JOIN generate_series('2026-03-01'::date, '2026-05-31'::date, '1 day'::interval) d
WHERE inv.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS');

-- OBH 재고 가용성 (리조트: 주말 예약률 더 높음)
INSERT INTO rm_inventory_availability (inventory_item_id, availability_date, available_count, reserved_count)
SELECT
    inv.id,
    d::date,
    inv.total_quantity,
    CASE
        WHEN EXTRACT(DOW FROM d) IN (0, 6)
            THEN LEAST(FLOOR(inv.total_quantity * (0.4 + (EXTRACT(DOY FROM d) % 6) * 0.04))::int, inv.total_quantity)
        WHEN EXTRACT(DOW FROM d) = 5  -- 금요일: 중간
            THEN LEAST(FLOOR(inv.total_quantity * (0.3 + (EXTRACT(DOY FROM d) % 5) * 0.03))::int, inv.total_quantity)
        ELSE
            LEAST(FLOOR(inv.total_quantity * (0.1 + (EXTRACT(DOY FROM d) % 8) * 0.02))::int, inv.total_quantity)
    END
FROM rm_inventory_item inv
CROSS JOIN generate_series('2026-03-01'::date, '2026-05-31'::date, '1 day'::interval) d
WHERE inv.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH');


-- ===== 8. 기존 PaidServiceOption에 트랜잭션 코드 연결 =====
-- BF-ADD (조식 추가) → TC 2001 (추가 조식)
UPDATE rm_paid_service_option pso
SET transaction_code_id = tc.id
FROM rm_transaction_code tc
WHERE pso.property_id = tc.property_id
  AND pso.service_option_code = 'BF-ADD'
  AND tc.transaction_code = '2001'
  AND pso.transaction_code_id IS NULL;

-- RS-BASIC (룸서비스 기본) → TC 2300
UPDATE rm_paid_service_option pso
SET transaction_code_id = tc.id
FROM rm_transaction_code tc
WHERE pso.property_id = tc.property_id
  AND pso.service_option_code = 'RS-BASIC'
  AND tc.transaction_code = '2300'
  AND pso.transaction_code_id IS NULL;

-- RS-PREMIUM (룸서비스 프리미엄) → TC 2301
UPDATE rm_paid_service_option pso
SET transaction_code_id = tc.id
FROM rm_transaction_code tc
WHERE pso.property_id = tc.property_id
  AND pso.service_option_code = 'RS-PREMIUM'
  AND tc.transaction_code = '2301'
  AND pso.transaction_code_id IS NULL;

-- SPA-BASIC → TC 3000
UPDATE rm_paid_service_option pso
SET transaction_code_id = tc.id
FROM rm_transaction_code tc
WHERE pso.property_id = tc.property_id
  AND pso.service_option_code = 'SPA-BASIC'
  AND tc.transaction_code = '3000'
  AND pso.transaction_code_id IS NULL;

-- SPA-VIP → TC 3001
UPDATE rm_paid_service_option pso
SET transaction_code_id = tc.id
FROM rm_transaction_code tc
WHERE pso.property_id = tc.property_id
  AND pso.service_option_code = 'SPA-VIP'
  AND tc.transaction_code = '3001'
  AND pso.transaction_code_id IS NULL;

-- LAUNDRY → TC 3100
UPDATE rm_paid_service_option pso
SET transaction_code_id = tc.id
FROM rm_transaction_code tc
WHERE pso.property_id = tc.property_id
  AND pso.service_option_code = 'LAUNDRY'
  AND tc.transaction_code = '3100'
  AND pso.transaction_code_id IS NULL;

-- MINIBAR-P → TC 3200
UPDATE rm_paid_service_option pso
SET transaction_code_id = tc.id
FROM rm_transaction_code tc
WHERE pso.property_id = tc.property_id
  AND pso.service_option_code = 'MINIBAR-P'
  AND tc.transaction_code = '3200'
  AND pso.transaction_code_id IS NULL;

-- UPGRADE → TC 1010
UPDATE rm_paid_service_option pso
SET transaction_code_id = tc.id
FROM rm_transaction_code tc
WHERE pso.property_id = tc.property_id
  AND pso.service_option_code = 'UPGRADE'
  AND tc.transaction_code = '1010'
  AND pso.transaction_code_id IS NULL;


-- ===== 9. 시퀀스 리셋 =====
SELECT setval('rm_transaction_code_group_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_transaction_code_group));
SELECT setval('rm_transaction_code_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_transaction_code));
SELECT setval('rm_inventory_item_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_inventory_item));
SELECT setval('rm_inventory_availability_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_inventory_availability));
