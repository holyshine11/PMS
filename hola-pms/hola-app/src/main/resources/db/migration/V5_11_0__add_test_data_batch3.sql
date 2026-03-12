-- ============================================================
-- V5_11_0: 테스트 데이터 추가 Batch 3
-- 객실 타입(30) + 층/호수 매핑 + 무료/유료 서비스 매핑
-- 객실 기본정보, 수용인원, 어메니티, 베드, 유료서비스, 층/호수, 뷰 모두 포함
-- ============================================================

-- ============================================================
-- 0. 추가 호수 코드 (층에 매핑할 객실 번호 보충)
-- ============================================================
INSERT INTO htl_room_number (id, property_id, room_number, description_ko, description_en, sort_order, use_yn, created_at, created_by) VALUES
(230, 4, '401', '4층 401호. 패밀리 전용 구역 객실입니다.', '4F Room 401. Family dedicated zone room.', 11, true, NOW(), 'admin'),
(231, 4, '701', '7층 701호. 프리미엄 시티뷰 객실입니다.', '7F Room 701. Premium city view room.', 12, true, NOW(), 'admin'),
(232, 4, '801', '8층 801호. 클럽 플로어 객실입니다.', '8F Room 801. Club floor room.', 13, true, NOW(), 'admin'),
(233, 4, '901', '9층 901호. 주니어 스위트 객실입니다.', '9F Room 901. Junior suite room.', 14, true, NOW(), 'admin'),
(234, 5, '401', '4층 401호. 비즈니스 객실 구역입니다.', '4F Room 401. Business room zone.', 11, true, NOW(), 'admin'),
(235, 5, '1401', '14층 1401호. 주니어 스위트 객실입니다.', '14F Room 1401. Junior suite room.', 12, true, NOW(), 'admin');

SELECT setval('htl_room_number_id_seq', (SELECT COALESCE(MAX(id), 0) FROM htl_room_number));

-- ============================================================
-- 1. 객실 타입 (rm_room_type) - 30건 추가 (프로퍼티당 10건)
-- 기존: GMP(5), GMS(4), OBH(5) = 14건
-- ============================================================

-- GMP (property_id=4) - 10개 신규 객실 타입
INSERT INTO rm_room_type (property_id, room_class_id, room_type_code, description, room_size, max_adults, max_children, extra_bed_yn, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'ECO' AND deleted_at IS NULL),
     'ECO-S', '이코노미 싱글 - 합리적 가격의 1인 비즈니스 객실. 컴팩트한 공간에 기본 편의시설 구비.', 18.0, 1, 0, FALSE, 10, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'ECO' AND deleted_at IS NULL),
     'ECO-T', '이코노미 트윈 - 합리적 가격의 2인 객실. 트윈 베드 배치로 친구/동료 여행에 적합.', 22.0, 2, 0, FALSE, 11, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'BUS' AND deleted_at IS NULL),
     'BUS-K', '비즈니스 킹 - 넓은 업무 데스크와 인체공학 의자. 고속 인터넷과 커피머신 구비.', 28.0, 2, 1, FALSE, 12, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'EXE' AND deleted_at IS NULL),
     'EXE-K', '이그제큐티브 킹 - 라운지 무료 이용. 프리미엄 어메니티와 시티뷰 제공.', 35.0, 2, 1, TRUE, 13, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'PRE' AND deleted_at IS NULL),
     'PRE-K', '프리미엄 킹 - 고층 시티뷰 파노라마. 고급 인테리어와 프리미엄 침구류.', 40.0, 2, 1, TRUE, 14, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'CLB' AND deleted_at IS NULL),
     'CLB-K', '클럽 킹 - 클럽 라운지 전용 접근. 조식, 칵테일 아워, 프리미엄 간식 포함.', 38.0, 2, 1, TRUE, 15, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'FAM' AND deleted_at IS NULL),
     'FAM-T', '패밀리 트윈 - 가족 전용 넓은 객실. 소파베드 추가 가능, 키즈 어메니티 제공.', 45.0, 2, 2, TRUE, 16, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'JNR' AND deleted_at IS NULL),
     'JNR-K', '주니어 스위트 킹 - 거실과 침실 오픈형. 넓은 공간과 프리미엄 배스 어메니티.', 55.0, 2, 1, TRUE, 17, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'PEN' AND deleted_at IS NULL),
     'PEN-K', '펜트하우스 킹 - 최상층 럭셔리. 넓은 거실, 별도 다이닝, 개인 테라스.', 85.0, 2, 2, TRUE, 18, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_class_code = 'RYL' AND deleted_at IS NULL),
     'RYL-K', '로열 킹 - 전층 독점 최상위 객실. 전용 엘리베이터, 24시간 버틀러 서비스.', 120.0, 4, 2, TRUE, 19, NOW(), 'admin');

-- GMS (property_id=5) - 10개 신규 객실 타입
INSERT INTO rm_room_type (property_id, room_class_id, room_type_code, description, room_size, max_adults, max_children, extra_bed_yn, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_class_code = 'ECO' AND deleted_at IS NULL),
     'ECO-S', '이코노미 싱글 - 서초 지역 합리적 가격의 1인 객실.', 18.0, 1, 0, FALSE, 10, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_class_code = 'ECO' AND deleted_at IS NULL),
     'ECO-T', '이코노미 트윈 - 서초 지역 합리적 가격의 2인 객실.', 22.0, 2, 0, FALSE, 11, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_class_code = 'BUS' AND deleted_at IS NULL),
     'BUS-K', '비즈니스 킹 - 강남/서초 비즈니스 출장객 특화 객실.', 28.0, 2, 1, FALSE, 12, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_class_code = 'EXE' AND deleted_at IS NULL),
     'EXE-K', '이그제큐티브 킹 - 라운지 이용 프리미엄 객실. 강남 스카이라인뷰.', 35.0, 2, 1, TRUE, 13, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_class_code = 'PRE' AND deleted_at IS NULL),
     'PRE-T', '프리미엄 트윈 - 고층 강남뷰 프리미엄 트윈 객실.', 40.0, 2, 1, TRUE, 14, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_class_code = 'CLB' AND deleted_at IS NULL),
     'CLB-K', '클럽 킹 - 클럽 라운지 접근, 강남뷰.', 38.0, 2, 1, TRUE, 15, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_class_code = 'FAM' AND deleted_at IS NULL),
     'FAM-Q', '패밀리 퀸 - 가족 전용 퀸 객실. 소파베드, 키즈 어메니티.', 45.0, 2, 2, TRUE, 16, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_class_code = 'JNR' AND deleted_at IS NULL),
     'JNR-K', '주니어 스위트 킹 - 오픈형 거실+침실, 프리미엄 어메니티.', 55.0, 2, 1, TRUE, 17, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_class_code = 'PEN' AND deleted_at IS NULL),
     'PEN-K', '펜트하우스 킹 - 한강/서초 파노라마뷰, 별도 다이닝, 테라스.', 85.0, 2, 2, TRUE, 18, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_class_code = 'HNM' AND deleted_at IS NULL),
     'HNM-K', '허니문 킹 - 신혼부부 특화 로맨틱 객실. 자쿠지, 샴페인, 로맨틱 장식.', 50.0, 2, 0, FALSE, 19, NOW(), 'admin');

-- OBH (property_id=6) - 10개 신규 객실 타입
INSERT INTO rm_room_type (property_id, room_class_id, room_type_code, description, room_size, max_adults, max_children, extra_bed_yn, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'ECO' AND deleted_at IS NULL),
     'ECO-S', '이코노미 싱글 - 해운대 합리적 가격의 1인 객실.', 20.0, 1, 0, FALSE, 10, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'ECO' AND deleted_at IS NULL),
     'ECO-T', '이코노미 트윈 - 해운대 합리적 가격의 2인 객실.', 24.0, 2, 0, FALSE, 11, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'BUS' AND deleted_at IS NULL),
     'BUS-O', '비즈니스 오션뷰 - 해운대 바다 전망 비즈니스 객실.', 30.0, 2, 1, FALSE, 12, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'EXE' AND deleted_at IS NULL),
     'EXE-O', '이그제큐티브 오션뷰 - 해운대 바다 전망 프리미엄 객실.', 38.0, 2, 1, TRUE, 13, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'PRE' AND deleted_at IS NULL),
     'PRE-O', '프리미엄 오션프론트 - 해변 최전방 파노라마 오션뷰.', 42.0, 2, 1, TRUE, 14, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'CLB' AND deleted_at IS NULL),
     'CLB-O', '클럽 오션뷰 - 클럽 라운지 접근, 프라이빗 비치 서비스.', 40.0, 2, 1, TRUE, 15, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'FAM' AND deleted_at IS NULL),
     'FAM-O', '패밀리 오션뷰 - 가족 전용 넓은 오션뷰 객실. 키즈풀 인접.', 50.0, 2, 2, TRUE, 16, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'JNR' AND deleted_at IS NULL),
     'JNR-O', '주니어 스위트 오션뷰 - 넓은 거실과 해운대 오션뷰.', 60.0, 2, 1, TRUE, 17, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'PEN' AND deleted_at IS NULL),
     'PEN-O', '펜트하우스 오션뷰 - 최상층 개인 테라스, 자쿠지, 파노라마 오션뷰.', 90.0, 2, 2, TRUE, 18, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH'),
     (SELECT id FROM rm_room_class WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_class_code = 'RYL' AND deleted_at IS NULL),
     'RYL-O', '로열 오션프론트 - 전층 독점 오션프론트. 전용 버틀러, 프라이빗 비치.', 130.0, 4, 2, TRUE, 19, NOW(), 'admin');

-- ============================================================
-- 2. 객실 타입 - 층/호수 매핑 (rm_room_type_floor)
-- ============================================================

-- GMP 매핑
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'ECO-S'
  AND f.property_id = rt.property_id AND f.floor_number = '3F'
  AND rn.property_id = rt.property_id AND rn.room_number = '301';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'ECO-T'
  AND f.property_id = rt.property_id AND f.floor_number = '3F'
  AND rn.property_id = rt.property_id AND rn.room_number = '302';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'BUS-K'
  AND f.property_id = rt.property_id AND f.floor_number = '4F'
  AND rn.property_id = rt.property_id AND rn.room_number = '401';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'EXE-K'
  AND f.property_id = rt.property_id AND f.floor_number = '5F'
  AND rn.property_id = rt.property_id AND rn.room_number = '501';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'PRE-K'
  AND f.property_id = rt.property_id AND f.floor_number = '7F'
  AND rn.property_id = rt.property_id AND rn.room_number = '701';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'CLB-K'
  AND f.property_id = rt.property_id AND f.floor_number = '8F'
  AND rn.property_id = rt.property_id AND rn.room_number = '801';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'FAM-T'
  AND f.property_id = rt.property_id AND f.floor_number = '5F'
  AND rn.property_id = rt.property_id AND rn.room_number = '502';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'JNR-K'
  AND f.property_id = rt.property_id AND f.floor_number = '9F'
  AND rn.property_id = rt.property_id AND rn.room_number = '901';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'PEN-K'
  AND f.property_id = rt.property_id AND f.floor_number = '1F'
  AND rn.property_id = rt.property_id AND rn.room_number = '101';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'RYL-K'
  AND f.property_id = rt.property_id AND f.floor_number = '1F'
  AND rn.property_id = rt.property_id AND rn.room_number = '102';

-- GMS 매핑
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'ECO-S'
  AND f.property_id = rt.property_id AND f.floor_number = '3F'
  AND rn.property_id = rt.property_id AND rn.room_number = '301';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'ECO-T'
  AND f.property_id = rt.property_id AND f.floor_number = '3F'
  AND rn.property_id = rt.property_id AND rn.room_number = '302';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'BUS-K'
  AND f.property_id = rt.property_id AND f.floor_number = '4F'
  AND rn.property_id = rt.property_id AND rn.room_number = '401';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'EXE-K'
  AND f.property_id = rt.property_id AND f.floor_number = '11F'
  AND rn.property_id = rt.property_id AND rn.room_number = '1101';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'PRE-T'
  AND f.property_id = rt.property_id AND f.floor_number = '11F'
  AND rn.property_id = rt.property_id AND rn.room_number = '1102';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'CLB-K'
  AND f.property_id = rt.property_id AND f.floor_number = '13F'
  AND rn.property_id = rt.property_id AND rn.room_number = '1301';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'FAM-Q'
  AND f.property_id = rt.property_id AND f.floor_number = '12F'
  AND rn.property_id = rt.property_id AND rn.room_number = '1201';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'JNR-K'
  AND f.property_id = rt.property_id AND f.floor_number = '14F'
  AND rn.property_id = rt.property_id AND rn.room_number = '1401';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'PEN-K'
  AND f.property_id = rt.property_id AND f.floor_number = '15F'
  AND rn.property_id = rt.property_id AND rn.room_number = '1501';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'HNM-K'
  AND f.property_id = rt.property_id AND f.floor_number = '1F'
  AND rn.property_id = rt.property_id AND rn.room_number = '101';

-- OBH 매핑
INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'ECO-S'
  AND f.property_id = rt.property_id AND f.floor_number = '9F'
  AND rn.property_id = rt.property_id AND rn.room_number = '901';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'ECO-T'
  AND f.property_id = rt.property_id AND f.floor_number = '9F'
  AND rn.property_id = rt.property_id AND rn.room_number = '902';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'BUS-O'
  AND f.property_id = rt.property_id AND f.floor_number = '10F'
  AND rn.property_id = rt.property_id AND rn.room_number = '1001';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'EXE-O'
  AND f.property_id = rt.property_id AND f.floor_number = '10F'
  AND rn.property_id = rt.property_id AND rn.room_number = '1002';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'PRE-O'
  AND f.property_id = rt.property_id AND f.floor_number = '11F'
  AND rn.property_id = rt.property_id AND rn.room_number = '1101';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'CLB-O'
  AND f.property_id = rt.property_id AND f.floor_number = '12F'
  AND rn.property_id = rt.property_id AND rn.room_number = '1201';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'FAM-O'
  AND f.property_id = rt.property_id AND f.floor_number = '13F'
  AND rn.property_id = rt.property_id AND rn.room_number = '1301';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'JNR-O'
  AND f.property_id = rt.property_id AND f.floor_number = '14F'
  AND rn.property_id = rt.property_id AND rn.room_number = '1401';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'PEN-O'
  AND f.property_id = rt.property_id AND f.floor_number = '15F'
  AND rn.property_id = rt.property_id AND rn.room_number = '1501';

INSERT INTO rm_room_type_floor (room_type_id, floor_id, room_number_id)
SELECT rt.id, f.id, rn.id FROM rm_room_type rt, htl_floor f, htl_room_number rn
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'RYL-O'
  AND f.property_id = rt.property_id AND f.floor_number = '16F'
  AND rn.property_id = rt.property_id AND rn.room_number = '1601';

-- ============================================================
-- 3. 객실 타입 - 무료 서비스 매핑 (베드, 뷰, 어메니티, 조식, 인터넷, 웰컴)
-- ============================================================

-- === GMP 무료 서비스 매핑 ===
-- ECO-S: 싱글베드, 와이파이, 기본 어메니티
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'ECO-S'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-SINGLE', 'WIFI', 'AMENITY-BS');

-- ECO-T: 트윈베드, 와이파이, 기본 어메니티
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'ECO-T'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-TWIN', 'WIFI', 'AMENITY-BS');

-- BUS-K: 킹베드, 와이파이, 시티뷰, 커피머신
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'BUS-K'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI', 'VIEW-CITY', 'RA-COFFEE');

-- EXE-K: 킹베드, 프리미엄와이파이, 시티뷰, 바스로브, 커피머신
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'EXE-K'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'VIEW-CITY', 'RA-ROBE', 'RA-COFFEE');

-- PRE-K: 퀸베드, 프리미엄와이파이, 시티뷰, 바스로브, 프리미엄배스
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'PRE-K'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-QUEEN', 'WIFI-PRE', 'VIEW-CITY', 'RA-ROBE', 'AM-BATH');

-- CLB-K: 킹베드, 프리미엄와이파이, 시티뷰, 조식뷔페, 웰컴과일
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'CLB-K'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'VIEW-CITY', 'BF-BUFFET', 'WEL-FRUIT');

-- FAM-T: 트윈+소파베드, 와이파이, 가든뷰, 기본어메니티
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'FAM-T'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-TWIN', 'BED-SOFA', 'WIFI', 'VIEW-GARDEN', 'AMENITY-BS');

-- JNR-K: 킹베드, 프리미엄와이파이, 바스로브, 컨티넨탈조식, 프리미엄배스
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'JNR-K'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'RA-ROBE', 'BF-CONTI', 'AM-BATH');

-- PEN-K: 킹베드, 프리미엄와이파이, 풀뷰, 바스로브, 조식뷔페, 웰컴과일
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'PEN-K'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'VIEW-POOL', 'RA-ROBE', 'BF-BUFFET', 'WEL-FRUIT');

-- RYL-K: 킹베드, 프리미엄와이파이, 풀뷰, 바스로브, 조식뷔페, 컨티넨탈조식, 웰컴드링크, 웰컴과일, 프리미엄배스
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'RYL-K'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'VIEW-POOL', 'RA-ROBE', 'BF-BUFFET', 'WELCOME', 'WEL-FRUIT', 'AM-BATH', 'RA-COFFEE');

-- === GMS 무료 서비스 매핑 ===
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'ECO-S'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-SINGLE', 'WIFI', 'AMENITY-BS');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'ECO-T'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-TWIN', 'WIFI', 'AMENITY-BS');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'BUS-K'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI', 'VIEW-CITY', 'RA-COFFEE');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'EXE-K'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'VIEW-CITY', 'RA-ROBE', 'RA-COFFEE');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'PRE-T'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-TWIN', 'WIFI-PRE', 'VIEW-CITY', 'RA-ROBE', 'AM-BATH');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'CLB-K'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'VIEW-CITY', 'BF-BUFFET', 'WEL-FRUIT');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'FAM-Q'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-QUEEN', 'BED-SOFA', 'WIFI', 'VIEW-GARDEN', 'AMENITY-BS');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'JNR-K'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'RA-ROBE', 'BF-CONTI', 'AM-BATH');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'PEN-K'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'VIEW-RIVER', 'RA-ROBE', 'BF-BUFFET', 'WEL-FRUIT', 'AM-BATH');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'HNM-K'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'VIEW-RIVER', 'RA-ROBE', 'WEL-FRUIT', 'AM-BATH');

-- === OBH 무료 서비스 매핑 ===
INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'ECO-S'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-SINGLE', 'WIFI', 'AMENITY-BS');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'ECO-T'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-TWIN', 'WIFI', 'AMENITY-BS');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'BUS-O'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI', 'VIEW-OCEAN', 'RA-COFFEE');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'EXE-O'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'VIEW-OCEAN', 'RA-ROBE', 'RA-COFFEE');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'PRE-O'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'VIEW-BEACH', 'RA-ROBE', 'AM-BATH');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'CLB-O'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'VIEW-OCEAN', 'BF-BUFFET', 'WEL-FRUIT');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'FAM-O'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-TWIN', 'BED-SOFA', 'WIFI', 'VIEW-OCEAN', 'AMENITY-BS');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'JNR-O'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'VIEW-OCEAN', 'RA-ROBE', 'BF-CONTI', 'AM-BATH');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'PEN-O'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'VIEW-BEACH', 'VIEW-POOL', 'RA-ROBE', 'BF-BUFFET', 'WEL-FRUIT', 'AM-BATH');

INSERT INTO rm_room_type_free_service (room_type_id, free_service_option_id, quantity)
SELECT rt.id, fs.id, 1 FROM rm_room_type rt, rm_free_service_option fs
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'RYL-O'
  AND fs.property_id = rt.property_id AND fs.service_option_code IN ('BED-KING', 'WIFI-PRE', 'VIEW-BEACH', 'VIEW-POOL', 'RA-ROBE', 'BF-BUFFET', 'BF-CONTI', 'WELCOME', 'WEL-FRUIT', 'AM-BATH', 'RA-COFFEE');

-- ============================================================
-- 4. 객실 타입 - 유료 서비스 매핑
-- ============================================================

-- === GMP 유료 서비스 매핑 ===
INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'ECO-S'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('LAUNDRY');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'ECO-T'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('LAUNDRY', 'BF-ADD');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'BUS-K'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('BF-ADD', 'LAUNDRY-EX', 'RS-BASIC');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'EXE-K'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('RS-WINE', 'SPA-BASIC', 'LAUNDRY-EX');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'PRE-K'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('RS-WINE', 'SPA-AROMA', 'UPGRADE-VW');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'CLB-K'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('RS-WINE', 'RS-CAKE', 'SPA-COUPLE');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'FAM-T'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('BF-ROOM', 'LAUNDRY-EX');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'JNR-K'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('SPA-COUPLE', 'MINIBAR-DX', 'UPGRADE-VW');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'PEN-K'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('SPA-COUPLE', 'MINIBAR-DX', 'PA-FLOWER', 'RS-WINE');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND rt.room_type_code = 'RYL-K'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('SPA-VIP', 'MINIBAR-DX', 'PA-FLOWER', 'PA-TURNDOWN', 'RS-WINE', 'RS-CAKE');

-- === GMS 유료 서비스 매핑 ===
INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'ECO-S'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('LAUNDRY');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'ECO-T'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('LAUNDRY', 'BF-ADD');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'BUS-K'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('BF-ADD', 'LAUNDRY-EX', 'RS-BASIC');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'EXE-K'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('RS-WINE', 'SPA-BASIC', 'LAUNDRY-EX');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'PRE-T'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('RS-WINE', 'SPA-AROMA', 'UPGRADE-VW');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'CLB-K'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('RS-WINE', 'RS-CAKE', 'SPA-COUPLE');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'FAM-Q'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('BF-ROOM', 'LAUNDRY-EX');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'JNR-K'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('SPA-COUPLE', 'MINIBAR-DX', 'UPGRADE-VW');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'PEN-K'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('SPA-VIP', 'MINIBAR-DX', 'PA-FLOWER', 'RS-WINE');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND rt.room_type_code = 'HNM-K'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('SPA-COUPLE', 'PA-FLOWER', 'PA-TURNDOWN', 'RS-CAKE', 'RS-WINE');

-- === OBH 유료 서비스 매핑 ===
INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'ECO-S'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('LAUNDRY');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'ECO-T'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('LAUNDRY', 'BF-ADD');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'BUS-O'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('BF-ADD', 'LAUNDRY-EX', 'RS-BASIC');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'EXE-O'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('RS-WINE', 'SPA-BASIC', 'LAUNDRY-EX');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'PRE-O'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('RS-WINE', 'SPA-AROMA', 'UPGRADE-VW');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'CLB-O'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('RS-WINE', 'RS-CAKE', 'SPA-COUPLE');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'FAM-O'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('BF-ROOM', 'LAUNDRY-EX');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'JNR-O'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('SPA-COUPLE', 'MINIBAR-DX', 'UPGRADE-VW');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'PEN-O'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('SPA-VIP', 'MINIBAR-DX', 'PA-FLOWER', 'RS-WINE');

INSERT INTO rm_room_type_paid_service (room_type_id, paid_service_option_id, quantity)
SELECT rt.id, ps.id, 1 FROM rm_room_type rt, rm_paid_service_option ps
WHERE rt.property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND rt.room_type_code = 'RYL-O'
  AND ps.property_id = rt.property_id AND ps.service_option_code IN ('SPA-VIP', 'MINIBAR-DX', 'PA-FLOWER', 'PA-TURNDOWN', 'RS-WINE', 'RS-CAKE', 'BF-ROOM');

-- ============================================================
-- 5. 시퀀스 리셋
-- ============================================================
SELECT setval('rm_room_type_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_room_type));
SELECT setval('rm_room_type_floor_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_room_type_floor));
SELECT setval('rm_room_type_free_service_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_room_type_free_service));
SELECT setval('rm_room_type_paid_service_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_room_type_paid_service));
