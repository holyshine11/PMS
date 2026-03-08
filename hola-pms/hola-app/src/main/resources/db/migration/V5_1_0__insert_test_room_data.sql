-- =============================================
-- V5_1_0: 객실 테스트 데이터
-- 객실 클래스, 객실 타입, 층/호수 매핑, 서비스 옵션
-- =============================================

-- ===== 1. 객실 클래스 (3 per property = 9) =====
-- GMP
INSERT INTO rm_room_class (property_id, room_class_code, room_class_name, description, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'STD', '스탠다드', '기본 객실 그룹', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'DLX', '디럭스', '고급 객실 그룹', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'SUI', '스위트', '최고급 객실 그룹', 3, NOW(), 'admin');
-- GMS
INSERT INTO rm_room_class (property_id, room_class_code, room_class_name, description, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'STD', '스탠다드', '기본 객실 그룹', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'DLX', '디럭스', '고급 객실 그룹', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'SUI', '스위트', '최고급 객실 그룹', 3, NOW(), 'admin');
-- OBH
INSERT INTO rm_room_class (property_id, room_class_code, room_class_name, description, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'STD', '스탠다드', '기본 객실 그룹', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'DLX', '디럭스', '고급 객실 그룹', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'SUI', '스위트', '최고급 객실 그룹', 3, NOW(), 'admin');

-- ===== 2. 객실 타입 (GMP 5 + GMS 4 + OBH 5 = 14) =====
-- *** room_type_name 컬럼 없음 (V2_1_1에서 삭제됨) ***

-- GMP
INSERT INTO rm_room_type (property_id, room_class_id, room_type_code, description, room_size, max_adults, max_children, extra_bed_yn, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'STD'),
     'STD-S', '스탠다드 싱글 - 비즈니스 여행에 최적', 22.5, 1, 0, FALSE, 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'STD'),
     'STD-D', '스탠다드 더블 - 커플 여행에 적합', 28.0, 2, 1, TRUE, 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'DLX'),
     'DLX-T', '디럭스 트윈 - 넓은 트윈 베드룸', 35.0, 2, 1, TRUE, 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'DLX'),
     'DLX-D', '디럭스 더블 - 시티뷰 더블룸', 38.0, 2, 2, TRUE, 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'SUI'),
     'SUI-R', '로열 스위트 - 거실+침실 분리형', 65.0, 2, 2, TRUE, 5, NOW(), 'admin');

-- GMS
INSERT INTO rm_room_type (property_id, room_class_id, room_type_code, description, room_size, max_adults, max_children, extra_bed_yn, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_class_code = 'STD'),
     'STD-S', '스탠다드 싱글 - 비즈니스 여행에 최적', 20.0, 1, 0, FALSE, 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_class_code = 'STD'),
     'STD-D', '스탠다드 더블 - 커플 여행에 적합', 25.0, 2, 1, TRUE, 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_class_code = 'DLX'),
     'DLX-T', '디럭스 트윈 - 넓은 트윈 베드룸', 32.0, 2, 1, TRUE, 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_class_code = 'SUI'),
     'SUI-R', '로열 스위트 - 거실+침실 분리형', 55.0, 2, 2, TRUE, 4, NOW(), 'admin');

-- OBH
INSERT INTO rm_room_type (property_id, room_class_id, room_type_code, description, room_size, max_adults, max_children, extra_bed_yn, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'STD'),
     'STD-S', '스탠다드 싱글 - 해운대 싱글 객실', 24.0, 1, 0, FALSE, 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'STD'),
     'STD-D', '스탠다드 더블 - 해운대 더블 객실', 30.0, 2, 1, TRUE, 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'DLX'),
     'DLX-O', '디럭스 오션뷰 - 해운대 바다 전망', 38.0, 2, 1, TRUE, 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'DLX'),
     'DLX-D', '디럭스 더블 - 넓은 더블 객실', 40.0, 2, 2, TRUE, 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'SUI'),
     'SUI-P', '프레지덴셜 스위트 - 최상층 오션뷰', 85.0, 4, 2, TRUE, 5, NOW(), 'admin');

-- ===== 3. 객실 타입 - 층/호수 매핑 =====
-- 패턴: (room_type_id, floor_id, room_number_id)

-- GMP: STD-S → 10F (1001~1005)
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP')
  AND rt.room_type_code = 'STD-S'
  AND f.property_id = rt.property_id AND f.floor_number = '10F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('1001','1002','1003','1004','1005');

-- GMP: STD-D → 11F (1101~1105)
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP')
  AND rt.room_type_code = 'STD-D'
  AND f.property_id = rt.property_id AND f.floor_number = '11F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('1101','1102','1103','1104','1105');

-- GMP: DLX-T → 12F (1201~1205)
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP')
  AND rt.room_type_code = 'DLX-T'
  AND f.property_id = rt.property_id AND f.floor_number = '12F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('1201','1202','1203','1204','1205');

-- GMP: DLX-D → 13F (1301~1305)
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP')
  AND rt.room_type_code = 'DLX-D'
  AND f.property_id = rt.property_id AND f.floor_number = '13F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('1301','1302','1303','1304','1305');

-- GMP: SUI-R → 14F (1401~1405) + 15F (1501~1505)
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP')
  AND rt.room_type_code = 'SUI-R'
  AND f.property_id = rt.property_id AND f.floor_number = '14F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('1401','1402','1403','1404','1405');

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP')
  AND rt.room_type_code = 'SUI-R'
  AND f.property_id = rt.property_id AND f.floor_number = '15F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('1501','1502','1503','1504','1505');

-- GMS: STD-S → 5F (501~503)
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS')
  AND rt.room_type_code = 'STD-S'
  AND f.property_id = rt.property_id AND f.floor_number = '5F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('501','502','503');

-- GMS: STD-D → 6F (601~603) + 7F (701~703)
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS')
  AND rt.room_type_code = 'STD-D'
  AND f.property_id = rt.property_id AND f.floor_number = '6F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('601','602','603');

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS')
  AND rt.room_type_code = 'STD-D'
  AND f.property_id = rt.property_id AND f.floor_number = '7F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('701','702','703');

-- GMS: DLX-T → 8F (801~803) + 9F (901~903)
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS')
  AND rt.room_type_code = 'DLX-T'
  AND f.property_id = rt.property_id AND f.floor_number = '8F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('801','802','803');

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS')
  AND rt.room_type_code = 'DLX-T'
  AND f.property_id = rt.property_id AND f.floor_number = '9F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('901','902','903');

-- GMS: SUI-R → 10F (1001~1003)
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS')
  AND rt.room_type_code = 'SUI-R'
  AND f.property_id = rt.property_id AND f.floor_number = '10F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('1001','1002','1003');

-- OBH: STD-S → 1F (101~105) + 2F (201~205)
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'STD-S'
  AND f.property_id = rt.property_id AND f.floor_number = '1F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('101','102','103','104','105');

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'STD-S'
  AND f.property_id = rt.property_id AND f.floor_number = '2F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('201','202','203','204','205');

-- OBH: STD-D → 3F (301~305) + 4F (401~405)
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'STD-D'
  AND f.property_id = rt.property_id AND f.floor_number = '3F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('301','302','303','304','305');

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'STD-D'
  AND f.property_id = rt.property_id AND f.floor_number = '4F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('401','402','403','404','405');

-- OBH: DLX-O → 5F (501~505) + 6F (601~605)
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'DLX-O'
  AND f.property_id = rt.property_id AND f.floor_number = '5F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('501','502','503','504','505');

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'DLX-O'
  AND f.property_id = rt.property_id AND f.floor_number = '6F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('601','602','603','604','605');

-- OBH: DLX-D → 7F (701~705)
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'DLX-D'
  AND f.property_id = rt.property_id AND f.floor_number = '7F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('701','702','703','704','705');

-- OBH: SUI-P → 8F (801~805)
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id
FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'SUI-P'
  AND f.property_id = rt.property_id AND f.floor_number = '8F'
  AND rn.property_id = rt.property_id AND rn.room_number IN ('801','802','803','804','805');

-- ===== 4. 무료 서비스 옵션 (10 per property = 30) =====
-- GMP
INSERT INTO rm_free_service_option (property_id, service_option_code, service_name_ko, service_name_en, service_type, applicable_nights, quantity, quantity_unit, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'BED-KING', '킹베드', 'King Bed', 'BED', 'NOT_APPLICABLE', 1, 'EA', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'BED-TWIN', '트윈베드', 'Twin Bed', 'BED', 'NOT_APPLICABLE', 2, 'EA', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'BED-SINGLE', '싱글베드', 'Single Bed', 'BED', 'NOT_APPLICABLE', 1, 'EA', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'VIEW-CITY', '시티뷰', 'City View', 'VIEW', 'NOT_APPLICABLE', 1, 'EA', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'VIEW-OCEAN', '오션뷰', 'Ocean View', 'VIEW', 'NOT_APPLICABLE', 1, 'EA', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'VIEW-MTN', '마운틴뷰', 'Mountain View', 'VIEW', 'NOT_APPLICABLE', 1, 'EA', 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'WIFI', '무료 와이파이', 'Free WiFi', 'INTERNET', 'ALL_NIGHTS', 1, 'SERVICE', 7, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'AMENITY-BS', '기본 어메니티 세트', 'Basic Amenity Set', 'AMENITY', 'ALL_NIGHTS', 1, 'SET', 8, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'BF-BUFFET', '조식 뷔페', 'Breakfast Buffet', 'BREAKFAST', 'ALL_NIGHTS', 1, 'TIME', 9, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'WELCOME', '웰컴 드링크', 'Welcome Drink', 'WELCOME', 'FIRST_NIGHT_ONLY', 1, 'EA', 10, NOW(), 'admin');

-- GMS
INSERT INTO rm_free_service_option (property_id, service_option_code, service_name_ko, service_name_en, service_type, applicable_nights, quantity, quantity_unit, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'BED-KING', '킹베드', 'King Bed', 'BED', 'NOT_APPLICABLE', 1, 'EA', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'BED-TWIN', '트윈베드', 'Twin Bed', 'BED', 'NOT_APPLICABLE', 2, 'EA', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'BED-SINGLE', '싱글베드', 'Single Bed', 'BED', 'NOT_APPLICABLE', 1, 'EA', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'VIEW-CITY', '시티뷰', 'City View', 'VIEW', 'NOT_APPLICABLE', 1, 'EA', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'VIEW-OCEAN', '오션뷰', 'Ocean View', 'VIEW', 'NOT_APPLICABLE', 1, 'EA', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'VIEW-MTN', '마운틴뷰', 'Mountain View', 'VIEW', 'NOT_APPLICABLE', 1, 'EA', 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'WIFI', '무료 와이파이', 'Free WiFi', 'INTERNET', 'ALL_NIGHTS', 1, 'SERVICE', 7, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'AMENITY-BS', '기본 어메니티 세트', 'Basic Amenity Set', 'AMENITY', 'ALL_NIGHTS', 1, 'SET', 8, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'BF-BUFFET', '조식 뷔페', 'Breakfast Buffet', 'BREAKFAST', 'ALL_NIGHTS', 1, 'TIME', 9, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'WELCOME', '웰컴 드링크', 'Welcome Drink', 'WELCOME', 'FIRST_NIGHT_ONLY', 1, 'EA', 10, NOW(), 'admin');

-- OBH
INSERT INTO rm_free_service_option (property_id, service_option_code, service_name_ko, service_name_en, service_type, applicable_nights, quantity, quantity_unit, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'BED-KING', '킹베드', 'King Bed', 'BED', 'NOT_APPLICABLE', 1, 'EA', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'BED-TWIN', '트윈베드', 'Twin Bed', 'BED', 'NOT_APPLICABLE', 2, 'EA', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'BED-SINGLE', '싱글베드', 'Single Bed', 'BED', 'NOT_APPLICABLE', 1, 'EA', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'VIEW-CITY', '시티뷰', 'City View', 'VIEW', 'NOT_APPLICABLE', 1, 'EA', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'VIEW-OCEAN', '오션뷰', 'Ocean View', 'VIEW', 'NOT_APPLICABLE', 1, 'EA', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'VIEW-MTN', '마운틴뷰', 'Mountain View', 'VIEW', 'NOT_APPLICABLE', 1, 'EA', 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'WIFI', '무료 와이파이', 'Free WiFi', 'INTERNET', 'ALL_NIGHTS', 1, 'SERVICE', 7, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'AMENITY-BS', '기본 어메니티 세트', 'Basic Amenity Set', 'AMENITY', 'ALL_NIGHTS', 1, 'SET', 8, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'BF-BUFFET', '조식 뷔페', 'Breakfast Buffet', 'BREAKFAST', 'ALL_NIGHTS', 1, 'TIME', 9, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'WELCOME', '웰컴 드링크', 'Welcome Drink', 'WELCOME', 'FIRST_NIGHT_ONLY', 1, 'EA', 10, NOW(), 'admin');

-- ===== 5. 유료 서비스 옵션 (8 per property = 24) =====
-- GMP
INSERT INTO rm_paid_service_option (property_id, service_option_code, service_name_ko, service_name_en, service_type, applicable_nights, currency_code, vat_included, tax_rate, supply_price, tax_amount, vat_included_price, quantity, quantity_unit, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'BF-ADD', '조식 추가', 'Additional Breakfast', 'BREAKFAST_PAID', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 27273, 2727, 30000, 1, 'EA', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'RS-BASIC', '룸서비스 기본', 'Room Service Basic', 'ROOM_SERVICE', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 36364, 3636, 40000, 1, 'EA', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'RS-PREMIUM', '룸서비스 프리미엄', 'Room Service Premium', 'ROOM_SERVICE', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 63636, 6364, 70000, 1, 'EA', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'SPA-BASIC', '스파 기본', 'Spa Basic Package', 'SPA_WELLNESS', 'NOT_APPLICABLE', 'KRW', TRUE, 10.00, 90909, 9091, 100000, 1, 'EA', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'SPA-VIP', '스파 VIP', 'Spa VIP Package', 'SPA_WELLNESS', 'NOT_APPLICABLE', 'KRW', TRUE, 10.00, 181818, 18182, 200000, 1, 'EA', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'LAUNDRY', '세탁 서비스', 'Laundry Service', 'LAUNDRY', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 13636, 1364, 15000, 1, 'EA', 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'MINIBAR-P', '미니바 프리미엄', 'Minibar Premium', 'MINIBAR', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 45455, 4545, 50000, 1, 'EA', 7, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'UPGRADE', '객실 업그레이드', 'Room Upgrade', 'ROOM_UPGRADE', 'NOT_APPLICABLE', 'KRW', TRUE, 10.00, 45455, 4545, 50000, 1, 'EA', 8, NOW(), 'admin');

-- GMS
INSERT INTO rm_paid_service_option (property_id, service_option_code, service_name_ko, service_name_en, service_type, applicable_nights, currency_code, vat_included, tax_rate, supply_price, tax_amount, vat_included_price, quantity, quantity_unit, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'BF-ADD', '조식 추가', 'Additional Breakfast', 'BREAKFAST_PAID', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 22727, 2273, 25000, 1, 'EA', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'RS-BASIC', '룸서비스 기본', 'Room Service Basic', 'ROOM_SERVICE', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 31818, 3182, 35000, 1, 'EA', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'RS-PREMIUM', '룸서비스 프리미엄', 'Room Service Premium', 'ROOM_SERVICE', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 54545, 5455, 60000, 1, 'EA', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'SPA-BASIC', '스파 기본', 'Spa Basic Package', 'SPA_WELLNESS', 'NOT_APPLICABLE', 'KRW', TRUE, 10.00, 72727, 7273, 80000, 1, 'EA', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'SPA-VIP', '스파 VIP', 'Spa VIP Package', 'SPA_WELLNESS', 'NOT_APPLICABLE', 'KRW', TRUE, 10.00, 136364, 13636, 150000, 1, 'EA', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'LAUNDRY', '세탁 서비스', 'Laundry Service', 'LAUNDRY', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 13636, 1364, 15000, 1, 'EA', 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'MINIBAR-P', '미니바 프리미엄', 'Minibar Premium', 'MINIBAR', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 36364, 3636, 40000, 1, 'EA', 7, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'UPGRADE', '객실 업그레이드', 'Room Upgrade', 'ROOM_UPGRADE', 'NOT_APPLICABLE', 'KRW', TRUE, 10.00, 36364, 3636, 40000, 1, 'EA', 8, NOW(), 'admin');

-- OBH
INSERT INTO rm_paid_service_option (property_id, service_option_code, service_name_ko, service_name_en, service_type, applicable_nights, currency_code, vat_included, tax_rate, supply_price, tax_amount, vat_included_price, quantity, quantity_unit, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'BF-ADD', '조식 추가', 'Additional Breakfast', 'BREAKFAST_PAID', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 31818, 3182, 35000, 1, 'EA', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'RS-BASIC', '룸서비스 기본', 'Room Service Basic', 'ROOM_SERVICE', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 40909, 4091, 45000, 1, 'EA', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'RS-PREMIUM', '룸서비스 프리미엄', 'Room Service Premium', 'ROOM_SERVICE', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 72727, 7273, 80000, 1, 'EA', 3, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'SPA-BASIC', '스파 기본', 'Spa Basic Package', 'SPA_WELLNESS', 'NOT_APPLICABLE', 'KRW', TRUE, 10.00, 109091, 10909, 120000, 1, 'EA', 4, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'SPA-VIP', '스파 VIP', 'Spa VIP Package', 'SPA_WELLNESS', 'NOT_APPLICABLE', 'KRW', TRUE, 10.00, 227273, 22727, 250000, 1, 'EA', 5, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'LAUNDRY', '세탁 서비스', 'Laundry Service', 'LAUNDRY', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 18182, 1818, 20000, 1, 'EA', 6, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'MINIBAR-P', '미니바 프리미엄', 'Minibar Premium', 'MINIBAR', 'ALL_NIGHTS', 'KRW', TRUE, 10.00, 54545, 5455, 60000, 1, 'EA', 7, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'), 'UPGRADE', '객실 업그레이드', 'Room Upgrade', 'ROOM_UPGRADE', 'NOT_APPLICABLE', 'KRW', TRUE, 10.00, 54545, 5455, 60000, 1, 'EA', 8, NOW(), 'admin');

-- ===== 6. 객실 타입 - 무료 서비스 매핑 =====
-- 모든 객실에 WIFI, AMENITY-BS 공통 할당
-- 디럭스 이상에 BF-BUFFET, WELCOME 추가
-- 싱글에 BED-SINGLE, 더블/스위트에 BED-KING, 트윈에 BED-TWIN
-- 적절한 VIEW 타입 할당

-- GMP 매핑
-- STD-S: WIFI, AMENITY-BS, BED-SINGLE, VIEW-CITY
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1
FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP')
  AND rt.room_type_code = 'STD-S'
  AND fs.property_id = rt.property_id
  AND fs.service_option_code IN ('WIFI', 'AMENITY-BS', 'BED-SINGLE', 'VIEW-CITY');

-- STD-D: WIFI, AMENITY-BS, BED-KING, VIEW-CITY
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1
FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP')
  AND rt.room_type_code = 'STD-D'
  AND fs.property_id = rt.property_id
  AND fs.service_option_code IN ('WIFI', 'AMENITY-BS', 'BED-KING', 'VIEW-CITY');

-- DLX-T: WIFI, AMENITY-BS, BED-TWIN, VIEW-CITY, BF-BUFFET, WELCOME
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1
FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP')
  AND rt.room_type_code = 'DLX-T'
  AND fs.property_id = rt.property_id
  AND fs.service_option_code IN ('WIFI', 'AMENITY-BS', 'BED-TWIN', 'VIEW-CITY', 'BF-BUFFET', 'WELCOME');

-- DLX-D: WIFI, AMENITY-BS, BED-KING, VIEW-CITY, BF-BUFFET, WELCOME
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1
FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP')
  AND rt.room_type_code = 'DLX-D'
  AND fs.property_id = rt.property_id
  AND fs.service_option_code IN ('WIFI', 'AMENITY-BS', 'BED-KING', 'VIEW-CITY', 'BF-BUFFET', 'WELCOME');

-- SUI-R: WIFI, AMENITY-BS, BED-KING, VIEW-CITY, BF-BUFFET, WELCOME
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1
FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP')
  AND rt.room_type_code = 'SUI-R'
  AND fs.property_id = rt.property_id
  AND fs.service_option_code IN ('WIFI', 'AMENITY-BS', 'BED-KING', 'VIEW-CITY', 'BF-BUFFET', 'WELCOME');

-- GMS 매핑
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1
FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS')
  AND rt.room_type_code = 'STD-S'
  AND fs.property_id = rt.property_id
  AND fs.service_option_code IN ('WIFI', 'AMENITY-BS', 'BED-SINGLE', 'VIEW-CITY');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1
FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS')
  AND rt.room_type_code = 'STD-D'
  AND fs.property_id = rt.property_id
  AND fs.service_option_code IN ('WIFI', 'AMENITY-BS', 'BED-KING', 'VIEW-CITY');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1
FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS')
  AND rt.room_type_code = 'DLX-T'
  AND fs.property_id = rt.property_id
  AND fs.service_option_code IN ('WIFI', 'AMENITY-BS', 'BED-TWIN', 'VIEW-CITY', 'BF-BUFFET', 'WELCOME');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1
FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS')
  AND rt.room_type_code = 'SUI-R'
  AND fs.property_id = rt.property_id
  AND fs.service_option_code IN ('WIFI', 'AMENITY-BS', 'BED-KING', 'VIEW-CITY', 'BF-BUFFET', 'WELCOME');

-- OBH 매핑
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1
FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'STD-S'
  AND fs.property_id = rt.property_id
  AND fs.service_option_code IN ('WIFI', 'AMENITY-BS', 'BED-SINGLE', 'VIEW-CITY');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1
FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'STD-D'
  AND fs.property_id = rt.property_id
  AND fs.service_option_code IN ('WIFI', 'AMENITY-BS', 'BED-KING', 'VIEW-CITY');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1
FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'DLX-O'
  AND fs.property_id = rt.property_id
  AND fs.service_option_code IN ('WIFI', 'AMENITY-BS', 'BED-KING', 'VIEW-OCEAN', 'BF-BUFFET', 'WELCOME');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1
FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'DLX-D'
  AND fs.property_id = rt.property_id
  AND fs.service_option_code IN ('WIFI', 'AMENITY-BS', 'BED-KING', 'VIEW-CITY', 'BF-BUFFET', 'WELCOME');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1
FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'SUI-P'
  AND fs.property_id = rt.property_id
  AND fs.service_option_code IN ('WIFI', 'AMENITY-BS', 'BED-KING', 'VIEW-OCEAN', 'BF-BUFFET', 'WELCOME');

-- ===== 7. 객실 타입 - 유료 서비스 매핑 =====
-- 디럭스: SPA-BASIC
-- 스위트: SPA-BASIC, MINIBAR-P, RS-BASIC

-- GMP
INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1
FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP')
  AND rt.room_type_code = 'DLX-T'
  AND ps.property_id = rt.property_id
  AND ps.service_option_code IN ('SPA-BASIC');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1
FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP')
  AND rt.room_type_code = 'DLX-D'
  AND ps.property_id = rt.property_id
  AND ps.service_option_code IN ('SPA-BASIC');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1
FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP')
  AND rt.room_type_code = 'SUI-R'
  AND ps.property_id = rt.property_id
  AND ps.service_option_code IN ('SPA-BASIC', 'MINIBAR-P', 'RS-BASIC');

-- GMS
INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1
FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS')
  AND rt.room_type_code = 'DLX-T'
  AND ps.property_id = rt.property_id
  AND ps.service_option_code IN ('SPA-BASIC');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1
FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS')
  AND rt.room_type_code = 'SUI-R'
  AND ps.property_id = rt.property_id
  AND ps.service_option_code IN ('SPA-BASIC', 'MINIBAR-P', 'RS-BASIC');

-- OBH
INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1
FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'DLX-O'
  AND ps.property_id = rt.property_id
  AND ps.service_option_code IN ('SPA-BASIC');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1
FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'DLX-D'
  AND ps.property_id = rt.property_id
  AND ps.service_option_code IN ('SPA-BASIC');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1
FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH')
  AND rt.room_type_code = 'SUI-P'
  AND ps.property_id = rt.property_id
  AND ps.service_option_code IN ('SPA-BASIC', 'SPA-VIP', 'MINIBAR-P', 'RS-BASIC');

-- ===== 8. 시퀀스 리셋 =====
SELECT setval('rm_room_class_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_room_class));
SELECT setval('rm_room_type_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_room_type));
SELECT setval('rm_room_type_floor_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_room_type_floor));
SELECT setval('rm_free_service_option_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_free_service_option));
SELECT setval('rm_paid_service_option_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_paid_service_option));
SELECT setval('rm_room_type_free_service_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_room_type_free_service));
SELECT setval('rm_room_type_paid_service_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_room_type_paid_service));
-- 코드 시퀀스도 리셋
SELECT setval('rm_room_class_code_seq', (SELECT COUNT(*) FROM rm_room_class));
SELECT setval('rm_room_type_code_seq', (SELECT COUNT(*) FROM rm_room_type));
