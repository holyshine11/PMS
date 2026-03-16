-- =============================================
-- V5_15_0: 호수(htl_room_number) description_ko 데이터 추가
-- GMP(30실), GMS(18실), OBH(40실) 전체 호수에 한글 설명 업데이트
-- =============================================

-- ===== GMP 프로퍼티 (올라 그랜드 명동) =====

-- 10F (1001~1005): STD-S 스탠다드 싱글 → "시티뷰 싱글"
UPDATE htl_room_number SET description_ko = '10층 1001호. 명동 시티뷰가 펼쳐지는 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1001';

UPDATE htl_room_number SET description_ko = '10층 1002호. 명동 시티뷰가 펼쳐지는 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1002';

UPDATE htl_room_number SET description_ko = '10층 1003호. 명동 시티뷰가 펼쳐지는 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1003';

UPDATE htl_room_number SET description_ko = '10층 1004호. 명동 시티뷰가 펼쳐지는 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1004';

UPDATE htl_room_number SET description_ko = '10층 1005호. 명동 시티뷰가 펼쳐지는 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1005';

-- 11F (1101~1105): STD-D 스탠다드 더블 → "시티뷰 더블"
UPDATE htl_room_number SET description_ko = '11층 1101호. 도심 전망의 시티뷰 스탠다드 더블 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1101';

UPDATE htl_room_number SET description_ko = '11층 1102호. 도심 전망의 시티뷰 스탠다드 더블 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1102';

UPDATE htl_room_number SET description_ko = '11층 1103호. 도심 전망의 시티뷰 스탠다드 더블 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1103';

UPDATE htl_room_number SET description_ko = '11층 1104호. 도심 전망의 시티뷰 스탠다드 더블 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1104';

UPDATE htl_room_number SET description_ko = '11층 1105호. 도심 전망의 시티뷰 스탠다드 더블 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1105';

-- 12F (1201~1205): DLX-T 디럭스 트윈 → "마리나뷰 트윈"
UPDATE htl_room_number SET description_ko = '12층 1201호. 마리나 전망의 디럭스 트윈 객실로, 넓은 트윈 베드룸이 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1201';

UPDATE htl_room_number SET description_ko = '12층 1202호. 마리나 전망의 디럭스 트윈 객실로, 넓은 트윈 베드룸이 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1202';

UPDATE htl_room_number SET description_ko = '12층 1203호. 마리나 전망의 디럭스 트윈 객실로, 넓은 트윈 베드룸이 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1203';

UPDATE htl_room_number SET description_ko = '12층 1204호. 마리나 전망의 디럭스 트윈 객실로, 넓은 트윈 베드룸이 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1204';

UPDATE htl_room_number SET description_ko = '12층 1205호. 마리나 전망의 디럭스 트윈 객실로, 넓은 트윈 베드룸이 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1205';

-- 13F (1301~1305): DLX-D 디럭스 더블 → "마리나뷰 더블"
UPDATE htl_room_number SET description_ko = '13층 1301호. 마리나 전망의 디럭스 더블 객실로, 킹 사이즈 침대가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1301';

UPDATE htl_room_number SET description_ko = '13층 1302호. 마리나 전망의 디럭스 더블 객실로, 킹 사이즈 침대가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1302';

UPDATE htl_room_number SET description_ko = '13층 1303호. 마리나 전망의 디럭스 더블 객실로, 킹 사이즈 침대가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1303';

UPDATE htl_room_number SET description_ko = '13층 1304호. 마리나 전망의 디럭스 더블 객실로, 킹 사이즈 침대가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1304';

UPDATE htl_room_number SET description_ko = '13층 1305호. 마리나 전망의 디럭스 더블 객실로, 킹 사이즈 침대가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1305';

-- 14F (1401~1405): SUI-R 스위트 → "코너 스위트"
UPDATE htl_room_number SET description_ko = '14층 1401호. 코너 위치의 로열 스위트로 거실과 침실이 분리된 개방감 있는 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1401';

UPDATE htl_room_number SET description_ko = '14층 1402호. 코너 위치의 로열 스위트로 거실과 침실이 분리된 개방감 있는 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1402';

UPDATE htl_room_number SET description_ko = '14층 1403호. 코너 위치의 로열 스위트로 거실과 침실이 분리된 개방감 있는 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1403';

UPDATE htl_room_number SET description_ko = '14층 1404호. 코너 위치의 로열 스위트로 거실과 침실이 분리된 개방감 있는 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1404';

UPDATE htl_room_number SET description_ko = '14층 1405호. 코너 위치의 로열 스위트로 거실과 침실이 분리된 개방감 있는 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1405';

-- 15F (1501~1505): SUI-R 스위트 → "하이플로어 스위트"
UPDATE htl_room_number SET description_ko = '15층 1501호. 최고층 하이플로어 로열 스위트로 전망이 탁월하며 특별한 투숙 경험을 제공합니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1501';

UPDATE htl_room_number SET description_ko = '15층 1502호. 최고층 하이플로어 로열 스위트로 전망이 탁월하며 특별한 투숙 경험을 제공합니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1502';

UPDATE htl_room_number SET description_ko = '15층 1503호. 최고층 하이플로어 로열 스위트로 전망이 탁월하며 특별한 투숙 경험을 제공합니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1503';

UPDATE htl_room_number SET description_ko = '15층 1504호. 최고층 하이플로어 로열 스위트로 전망이 탁월하며 특별한 투숙 경험을 제공합니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1504';

UPDATE htl_room_number SET description_ko = '15층 1505호. 최고층 하이플로어 로열 스위트로 전망이 탁월하며 특별한 투숙 경험을 제공합니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMP') AND room_number = '1505';


-- ===== GMS 프로퍼티 (올라 그랜드 서초) =====

-- 5F (501~503): STD-S 스탠다드 싱글 → "가든뷰 싱글"
UPDATE htl_room_number SET description_ko = '5층 501호. 정원이 내려다보이는 가든뷰 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '501';

UPDATE htl_room_number SET description_ko = '5층 502호. 정원이 내려다보이는 가든뷰 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '502';

UPDATE htl_room_number SET description_ko = '5층 503호. 정원이 내려다보이는 가든뷰 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '503';

-- 6F (601~603): STD-D 스탠다드 더블 → "가든뷰 더블"
UPDATE htl_room_number SET description_ko = '6층 601호. 정원 전망의 가든뷰 스탠다드 더블 객실로 커플 투숙에 적합합니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '601';

UPDATE htl_room_number SET description_ko = '6층 602호. 정원 전망의 가든뷰 스탠다드 더블 객실로 커플 투숙에 적합합니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '602';

UPDATE htl_room_number SET description_ko = '6층 603호. 정원 전망의 가든뷰 스탠다드 더블 객실로 커플 투숙에 적합합니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '603';

-- 7F (701~703): STD-D 스탠다드 더블 → "시티뷰 더블"
UPDATE htl_room_number SET description_ko = '7층 701호. 강남/서초 도심이 보이는 시티뷰 스탠다드 더블 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '701';

UPDATE htl_room_number SET description_ko = '7층 702호. 강남/서초 도심이 보이는 시티뷰 스탠다드 더블 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '702';

UPDATE htl_room_number SET description_ko = '7층 703호. 강남/서초 도심이 보이는 시티뷰 스탠다드 더블 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '703';

-- 8F (801~803): DLX-T 디럭스 트윈 → "시티뷰 트윈"
UPDATE htl_room_number SET description_ko = '8층 801호. 강남/서초 도심 전망의 시티뷰 디럭스 트윈 객실로 넓은 공간이 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '801';

UPDATE htl_room_number SET description_ko = '8층 802호. 강남/서초 도심 전망의 시티뷰 디럭스 트윈 객실로 넓은 공간이 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '802';

UPDATE htl_room_number SET description_ko = '8층 803호. 강남/서초 도심 전망의 시티뷰 디럭스 트윈 객실로 넓은 공간이 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '803';

-- 9F (901~903): DLX-T 디럭스 트윈 → "하버뷰 트윈"
UPDATE htl_room_number SET description_ko = '9층 901호. 한강 전망의 하버뷰 디럭스 트윈 객실로 탁 트인 강변 경치를 즐길 수 있습니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '901';

UPDATE htl_room_number SET description_ko = '9층 902호. 한강 전망의 하버뷰 디럭스 트윈 객실로 탁 트인 강변 경치를 즐길 수 있습니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '902';

UPDATE htl_room_number SET description_ko = '9층 903호. 한강 전망의 하버뷰 디럭스 트윈 객실로 탁 트인 강변 경치를 즐길 수 있습니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '903';

-- 10F (1001~1003): SUI-R 스위트 → "펜트하우스 스위트"
UPDATE htl_room_number SET description_ko = '10층 1001호. 최상층 펜트하우스 스위트로 거실과 침실이 분리된 최고급 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '1001';

UPDATE htl_room_number SET description_ko = '10층 1002호. 최상층 펜트하우스 스위트로 거실과 침실이 분리된 최고급 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '1002';

UPDATE htl_room_number SET description_ko = '10층 1003호. 최상층 펜트하우스 스위트로 거실과 침실이 분리된 최고급 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'GMS') AND room_number = '1003';


-- ===== OBH 프로퍼티 (올라 비치 해운대) =====

-- 1F (101~105): STD-S 스탠다드 싱글 → "가든뷰 싱글"
UPDATE htl_room_number SET description_ko = '1층 101호. 호텔 정원이 내려다보이는 가든뷰 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '101';

UPDATE htl_room_number SET description_ko = '1층 102호. 호텔 정원이 내려다보이는 가든뷰 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '102';

UPDATE htl_room_number SET description_ko = '1층 103호. 호텔 정원이 내려다보이는 가든뷰 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '103';

UPDATE htl_room_number SET description_ko = '1층 104호. 호텔 정원이 내려다보이는 가든뷰 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '104';

UPDATE htl_room_number SET description_ko = '1층 105호. 호텔 정원이 내려다보이는 가든뷰 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '105';

-- 2F (201~205): STD-S 스탠다드 싱글 → "풀사이드 싱글"
UPDATE htl_room_number SET description_ko = '2층 201호. 수영장 옆에 위치한 풀사이드 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '201';

UPDATE htl_room_number SET description_ko = '2층 202호. 수영장 옆에 위치한 풀사이드 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '202';

UPDATE htl_room_number SET description_ko = '2층 203호. 수영장 옆에 위치한 풀사이드 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '203';

UPDATE htl_room_number SET description_ko = '2층 204호. 수영장 옆에 위치한 풀사이드 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '204';

UPDATE htl_room_number SET description_ko = '2층 205호. 수영장 옆에 위치한 풀사이드 스탠다드 싱글 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '205';

-- 3F (301~305): STD-D 스탠다드 더블 → "가든뷰 더블"
UPDATE htl_room_number SET description_ko = '3층 301호. 호텔 정원 전망의 가든뷰 스탠다드 더블 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '301';

UPDATE htl_room_number SET description_ko = '3층 302호. 호텔 정원 전망의 가든뷰 스탠다드 더블 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '302';

UPDATE htl_room_number SET description_ko = '3층 303호. 호텔 정원 전망의 가든뷰 스탠다드 더블 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '303';

UPDATE htl_room_number SET description_ko = '3층 304호. 호텔 정원 전망의 가든뷰 스탠다드 더블 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '304';

UPDATE htl_room_number SET description_ko = '3층 305호. 호텔 정원 전망의 가든뷰 스탠다드 더블 객실입니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '305';

-- 4F (401~405): STD-D 스탠다드 더블 → "풀사이드 더블"
UPDATE htl_room_number SET description_ko = '4층 401호. 풀사이드 스탠다드 더블 객실로 수영장을 바라보며 휴식을 즐길 수 있습니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '401';

UPDATE htl_room_number SET description_ko = '4층 402호. 풀사이드 스탠다드 더블 객실로 수영장을 바라보며 휴식을 즐길 수 있습니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '402';

UPDATE htl_room_number SET description_ko = '4층 403호. 풀사이드 스탠다드 더블 객실로 수영장을 바라보며 휴식을 즐길 수 있습니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '403';

UPDATE htl_room_number SET description_ko = '4층 404호. 풀사이드 스탠다드 더블 객실로 수영장을 바라보며 휴식을 즐길 수 있습니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '404';

UPDATE htl_room_number SET description_ko = '4층 405호. 풀사이드 스탠다드 더블 객실로 수영장을 바라보며 휴식을 즐길 수 있습니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '405';

-- 5F (501~505): DLX-O 디럭스 오션뷰 → "오션뷰 디럭스"
UPDATE htl_room_number SET description_ko = '5층 501호. 해운대 바다가 펼쳐지는 오션뷰 디럭스 객실로 조식 뷔페가 포함됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '501';

UPDATE htl_room_number SET description_ko = '5층 502호. 해운대 바다가 펼쳐지는 오션뷰 디럭스 객실로 조식 뷔페가 포함됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '502';

UPDATE htl_room_number SET description_ko = '5층 503호. 해운대 바다가 펼쳐지는 오션뷰 디럭스 객실로 조식 뷔페가 포함됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '503';

UPDATE htl_room_number SET description_ko = '5층 504호. 해운대 바다가 펼쳐지는 오션뷰 디럭스 객실로 조식 뷔페가 포함됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '504';

UPDATE htl_room_number SET description_ko = '5층 505호. 해운대 바다가 펼쳐지는 오션뷰 디럭스 객실로 조식 뷔페가 포함됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '505';

-- 6F (601~605): DLX-O 디럭스 오션뷰 → "오션프론트 디럭스"
UPDATE htl_room_number SET description_ko = '6층 601호. 해운대 해변 정면의 오션프론트 디럭스 객실로 파노라마 오션뷰가 펼쳐집니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '601';

UPDATE htl_room_number SET description_ko = '6층 602호. 해운대 해변 정면의 오션프론트 디럭스 객실로 파노라마 오션뷰가 펼쳐집니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '602';

UPDATE htl_room_number SET description_ko = '6층 603호. 해운대 해변 정면의 오션프론트 디럭스 객실로 파노라마 오션뷰가 펼쳐집니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '603';

UPDATE htl_room_number SET description_ko = '6층 604호. 해운대 해변 정면의 오션프론트 디럭스 객실로 파노라마 오션뷰가 펼쳐집니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '604';

UPDATE htl_room_number SET description_ko = '6층 605호. 해운대 해변 정면의 오션프론트 디럭스 객실로 파노라마 오션뷰가 펼쳐집니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '605';

-- 7F (701~705): DLX-D 디럭스 더블 → "오션뷰 더블"
UPDATE htl_room_number SET description_ko = '7층 701호. 해운대 바다가 보이는 오션뷰 디럭스 더블 객실로 넓은 킹 사이즈 침대가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '701';

UPDATE htl_room_number SET description_ko = '7층 702호. 해운대 바다가 보이는 오션뷰 디럭스 더블 객실로 넓은 킹 사이즈 침대가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '702';

UPDATE htl_room_number SET description_ko = '7층 703호. 해운대 바다가 보이는 오션뷰 디럭스 더블 객실로 넓은 킹 사이즈 침대가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '703';

UPDATE htl_room_number SET description_ko = '7층 704호. 해운대 바다가 보이는 오션뷰 디럭스 더블 객실로 넓은 킹 사이즈 침대가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '704';

UPDATE htl_room_number SET description_ko = '7층 705호. 해운대 바다가 보이는 오션뷰 디럭스 더블 객실로 넓은 킹 사이즈 침대가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '705';

-- 8F (801~805): SUI-P 프레지덴셜 스위트 → "프레지덴셜 스위트"
UPDATE htl_room_number SET description_ko = '8층 801호. 프레지덴셜 스위트로 최상층 파노라마 오션뷰와 넓은 거실/침실/서재가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '801';

UPDATE htl_room_number SET description_ko = '8층 802호. 프레지덴셜 스위트로 최상층 파노라마 오션뷰와 넓은 거실/침실/서재가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '802';

UPDATE htl_room_number SET description_ko = '8층 803호. 프레지덴셜 스위트로 최상층 파노라마 오션뷰와 넓은 거실/침실/서재가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '803';

UPDATE htl_room_number SET description_ko = '8층 804호. 프레지덴셜 스위트로 최상층 파노라마 오션뷰와 넓은 거실/침실/서재가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '804';

UPDATE htl_room_number SET description_ko = '8층 805호. 프레지덴셜 스위트로 최상층 파노라마 오션뷰와 넓은 거실/침실/서재가 제공됩니다.'
WHERE property_id = (SELECT id FROM htl_property WHERE property_code = 'OBH') AND room_number = '805';
