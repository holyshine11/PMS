-- ============================================================
-- V5_9_0: 테스트 데이터 추가 Batch 1
-- 마켓코드(30), 층코드(30), 호수코드(30), 객실그룹(30), 예약채널(30)
-- 프로퍼티: GMP(1), GMS(2), OBH(3) 각 10건씩
-- ============================================================

-- ============================================================
-- 1. 마켓코드 (htl_market_code) - 30건 추가
-- 기존: FIT, GRP, CORP, OTA, GOV, WEB (프로퍼티당 6건)
-- ============================================================
INSERT INTO htl_market_code (id, property_id, market_code, market_name, description_ko, description_en, sort_order, use_yn, created_at, created_by) VALUES
-- GMP (property_id=4)
(100, 4, 'MICE', 'MICE', 'MICE(회의/인센티브/컨벤션/전시) 관련 투숙객 세그먼트. 국제회의, 기업 인센티브 여행, 학술 컨퍼런스, 전시회 참가자 등이 포함됩니다.', 'MICE (Meetings, Incentives, Conventions, Exhibitions) guest segment. Includes international conference attendees, corporate incentive travelers, and exhibition participants.', 7, true, NOW(), 'admin'),
(101, 4, 'PKG', '패키지 투어', '여행사 또는 호텔 자체 기획 패키지 상품을 이용하는 투숙객. 항공+숙박, 숙박+조식, 숙박+관광 등 복합 상품이 해당됩니다.', 'Guests using package tour products. Includes combined products such as flight+stay, stay+breakfast, and stay+sightseeing packages.', 8, true, NOW(), 'admin'),
(102, 4, 'CREW', '항공사 승무원', '항공사 계약 기반 승무원 숙박 세그먼트. 정기 계약 할인 요금 적용, 조기 체크인 및 레이트 체크아웃이 빈번합니다.', 'Airline crew accommodation segment based on airline contracts. Regular contract discounted rates apply, with frequent early check-in and late check-out.', 9, true, NOW(), 'admin'),
(103, 4, 'LONG', '장기투숙', '30일 이상 장기 투숙 고객 세그먼트. 비즈니스 출장, 프로젝트 파견, 해외 주재원 등이 해당되며 별도 월간 요금이 적용됩니다.', 'Long-stay guest segment for stays of 30 days or more. Includes business travelers, project dispatches, and expatriates with special monthly rates.', 10, true, NOW(), 'admin'),
(104, 4, 'AGT', '여행사 에이전트', '국내외 여행사를 통해 예약하는 고객 세그먼트. 여행사 제휴 요금 적용 및 커미션 정산이 수반됩니다.', 'Guest segment booking through domestic and international travel agents. Affiliated agency rates and commission settlements apply.', 11, true, NOW(), 'admin'),
(105, 4, 'FAM', '가족 여행', '가족 단위 여행객 세그먼트. 어린이 동반 투숙, 패밀리룸 선호, 키즈 프로그램 참여 등의 특성이 있습니다.', 'Family traveler segment. Characterized by children accompaniment, family room preference, and kids program participation.', 12, true, NOW(), 'admin'),
(106, 4, 'SPO', '스포츠 단체', '스포츠 팀, 대회 참가자, 스포츠 이벤트 관련 투숙객. 단체 숙박, 식사 일괄 제공, 회의실 사용 등이 필요합니다.', 'Sports teams, competition participants, and sports event guests. Group accommodation, meal packages, and meeting room usage required.', 13, true, NOW(), 'admin'),
(107, 4, 'MED', '의료관광', '의료 목적 방문 해외 관광객 세그먼트. 병원 연계 서비스, 통역 지원, 장기 체류 등이 수반됩니다.', 'Medical tourism segment for foreign visitors. Hospital-linked services, interpretation support, and extended stays included.', 14, true, NOW(), 'admin'),
(108, 4, 'EDU', '교육/연수', '교육, 연수, 세미나 참가 목적 투숙객. 기업 워크숍, 학술 세미나, 직원 연수 프로그램 참가자가 포함됩니다.', 'Education, training, and seminar guest segment. Includes corporate workshop, academic seminar, and staff training participants.', 15, true, NOW(), 'admin'),
(109, 4, 'HON', '허니문/신혼', '신혼여행객 프리미엄 세그먼트. 스위트룸 선호, 특별 장식, 웰컴 기프트, 로맨틱 디너 등의 서비스가 동반됩니다.', 'Premium honeymoon segment. Suite room preference, special decorations, welcome gifts, and romantic dinner services included.', 16, true, NOW(), 'admin'),
-- GMS (property_id=5)
(110, 5, 'MICE', 'MICE', 'MICE(회의/인센티브/컨벤션/전시) 관련 투숙객 세그먼트. 국제회의, 기업 인센티브 여행, 학술 컨퍼런스, 전시회 참가자 등이 포함됩니다.', 'MICE (Meetings, Incentives, Conventions, Exhibitions) guest segment. Includes international conference attendees and exhibition participants.', 7, true, NOW(), 'admin'),
(111, 5, 'PKG', '패키지 투어', '여행사 또는 호텔 자체 기획 패키지 상품을 이용하는 투숙객. 항공+숙박, 숙박+조식 등 복합 상품이 해당됩니다.', 'Guests using package tour products. Includes combined products such as flight+stay and stay+breakfast packages.', 8, true, NOW(), 'admin'),
(112, 5, 'CREW', '항공사 승무원', '항공사 계약 기반 승무원 숙박 세그먼트. 정기 계약 할인 요금이 적용됩니다.', 'Airline crew accommodation segment. Regular contract discounted rates apply.', 9, true, NOW(), 'admin'),
(113, 5, 'LONG', '장기투숙', '30일 이상 장기 투숙 고객 세그먼트. 비즈니스 출장, 프로젝트 파견 등이 해당됩니다.', 'Long-stay guest segment for stays of 30 days or more. Includes business travelers and project dispatches.', 10, true, NOW(), 'admin'),
(114, 5, 'AGT', '여행사 에이전트', '국내외 여행사를 통해 예약하는 고객 세그먼트. 제휴 요금 및 커미션 정산이 수반됩니다.', 'Guest segment booking through travel agents. Affiliated rates and commission settlements apply.', 11, true, NOW(), 'admin'),
(115, 5, 'FAM', '가족 여행', '가족 단위 여행객 세그먼트. 어린이 동반 투숙 및 패밀리룸 선호 특성이 있습니다.', 'Family traveler segment. Children accompaniment and family room preference.', 12, true, NOW(), 'admin'),
(116, 5, 'SPO', '스포츠 단체', '스포츠 팀 및 대회 참가자 투숙객. 단체 숙박과 식사 일괄 제공이 필요합니다.', 'Sports teams and competition participants. Group accommodation and meal packages required.', 13, true, NOW(), 'admin'),
(117, 5, 'MED', '의료관광', '의료 목적 해외 방문 관광객. 병원 연계 서비스 및 통역 지원이 포함됩니다.', 'Medical tourism for foreign visitors. Hospital-linked services and interpretation support included.', 14, true, NOW(), 'admin'),
(118, 5, 'EDU', '교육/연수', '교육, 연수, 세미나 참가 목적 투숙객. 기업 워크숍 및 세미나 참가자가 포함됩니다.', 'Education, training, and seminar guests. Corporate workshop and seminar participants included.', 15, true, NOW(), 'admin'),
(119, 5, 'HON', '허니문/신혼', '신혼여행객 프리미엄 세그먼트. 스위트룸, 특별 장식, 웰컴 기프트 등이 동반됩니다.', 'Premium honeymoon segment. Suite rooms, special decorations, and welcome gifts included.', 16, true, NOW(), 'admin'),
-- OBH (property_id=6)
(120, 6, 'MICE', 'MICE', 'MICE(회의/인센티브/컨벤션/전시) 관련 투숙객 세그먼트. 해운대 컨벤션센터 인접으로 대규모 행사 수요가 높습니다.', 'MICE guest segment. High demand for large-scale events due to proximity to Haeundae Convention Center.', 7, true, NOW(), 'admin'),
(121, 6, 'PKG', '패키지 투어', '여행사 패키지 상품 투숙객. 부산 관광+숙박, 해운대 비치+호텔 등의 결합 상품이 포함됩니다.', 'Package tour guests. Combined products such as Busan sightseeing+stay and Haeundae beach+hotel packages.', 8, true, NOW(), 'admin'),
(122, 6, 'CREW', '항공사 승무원', '항공사 계약 기반 승무원 숙박 세그먼트. 김해공항 인접 노선 승무원이 주로 이용합니다.', 'Airline crew accommodation segment. Primarily used by crew on routes to/from Gimhae Airport.', 9, true, NOW(), 'admin'),
(123, 6, 'LONG', '장기투숙', '30일 이상 장기 투숙 고객 세그먼트. 부산 지역 비즈니스 출장 및 프로젝트 파견이 해당됩니다.', 'Long-stay guest segment. Includes Busan area business travelers and project dispatches.', 10, true, NOW(), 'admin'),
(124, 6, 'AGT', '여행사 에이전트', '국내외 여행사 예약 고객 세그먼트. 부산/해운대 전문 여행사 제휴 요금이 적용됩니다.', 'Travel agent booking segment. Busan/Haeundae specialized agency affiliated rates apply.', 11, true, NOW(), 'admin'),
(125, 6, 'FAM', '가족 여행', '가족 단위 여행객 세그먼트. 해수욕장 인접으로 여름철 가족 수요가 높습니다.', 'Family traveler segment. High summer family demand due to beach proximity.', 12, true, NOW(), 'admin'),
(126, 6, 'SPO', '스포츠 단체', '스포츠 팀 및 대회 참가자. 부산 아시아드 등 스포츠 시설 인접 단체 수요입니다.', 'Sports teams and competition participants. Group demand near Busan Asiad sports facilities.', 13, true, NOW(), 'admin'),
(127, 6, 'MED', '의료관광', '의료관광 해외 방문객. 부산 의료관광 클러스터 연계 서비스가 제공됩니다.', 'Medical tourism visitors. Services linked to Busan medical tourism cluster provided.', 14, true, NOW(), 'admin'),
(128, 6, 'EDU', '교육/연수', '교육, 연수 참가 투숙객. 해운대 리조트 지역 기업 워크숍 및 세미나가 활발합니다.', 'Education and training guests. Active corporate workshops and seminars in Haeundae resort area.', 15, true, NOW(), 'admin'),
(129, 6, 'HON', '허니문/신혼', '신혼여행객 프리미엄 세그먼트. 오션뷰 스위트, 해변 로맨틱 디너 등 특화 서비스를 제공합니다.', 'Premium honeymoon segment. Specialized services including ocean view suites and beach romantic dinners.', 16, true, NOW(), 'admin');

-- ============================================================
-- 2. 층 코드 (htl_floor) - 30건 추가
-- 기존: GMP(10F-15F), GMS(5F-10F), OBH(1F-8F)
-- ============================================================
INSERT INTO htl_floor (id, property_id, floor_number, floor_name, description_ko, description_en, sort_order, use_yn, created_at, created_by) VALUES
-- GMP (property_id=4) - 지하층 및 저층부 추가
(100, 4, 'B2', '지하 2층', '지하 2층. 주차장(100대 수용), 기계실, 전기실, 직원 편의시설이 위치합니다.', 'Basement level 2. Parking garage (100 cars), mechanical room, electrical room, and staff amenities.', -2, true, NOW(), 'admin'),
(101, 4, 'B1', '지하 1층', '지하 1층. 연회장(대연회장 300석, 소연회장 80석), 비즈니스센터, 피트니스센터가 위치합니다.', 'Basement level 1. Banquet hall (grand 300 seats, small 80 seats), business center, and fitness center.', -1, true, NOW(), 'admin'),
(102, 4, '1F', '1층', '1층 로비 및 프론트데스크. 올데이다이닝 레스토랑, 카페, 기프트샵이 위치합니다.', '1st floor lobby and front desk. All-day dining restaurant, cafe, and gift shop located here.', 1, true, NOW(), 'admin'),
(103, 4, '2F', '2층', '2층 컨퍼런스 구역. 비즈니스 미팅룸(4실), 세미나실(2실)이 위치합니다.', '2nd floor conference area. Business meeting rooms (4 rooms) and seminar rooms (2 rooms).', 2, true, NOW(), 'admin'),
(104, 4, '3F', '3층', '3층 이코노미/비즈니스 객실 구역. 이코노미 싱글, 비즈니스 트윈 등 20실이 배치되어 있습니다.', '3rd floor economy/business room zone. 20 rooms including economy singles and business twins.', 3, true, NOW(), 'admin'),
(105, 4, '4F', '4층', '4층 패밀리 객실 전용 구역. 패밀리룸 및 커넥팅룸 15실이 배치되어 있습니다.', '4th floor family room exclusive zone. 15 family rooms and connecting rooms available.', 4, true, NOW(), 'admin'),
(106, 4, '5F', '5층', '5층 이그제큐티브 객실 구역. 이그제큐티브 라운지 및 전용 체크인 데스크가 있습니다.', '5th floor executive room zone. Executive lounge and dedicated check-in desk available.', 5, true, NOW(), 'admin'),
(107, 4, '7F', '7층', '7층 프리미엄 객실 구역. 시티뷰 프리미엄 객실 18실이 배치되어 있습니다.', '7th floor premium room zone. 18 premium rooms with city views.', 7, true, NOW(), 'admin'),
(108, 4, '8F', '8층', '8층 클럽 플로어. 클럽 라운지 전용 접근, 프리미엄 어메니티 제공 구역입니다.', '8th floor club level. Exclusive club lounge access and premium amenity zone.', 8, true, NOW(), 'admin'),
(109, 4, '9F', '9층', '9층 주니어 스위트 구역. 넓은 거실 공간의 주니어 스위트 12실이 배치되어 있습니다.', '9th floor junior suite zone. 12 junior suites with spacious living areas.', 9, true, NOW(), 'admin'),
-- GMS (property_id=5) - 저층 및 고층 추가
(110, 5, 'B1', '지하 1층', '지하 1층. 주차장(80대 수용), 피트니스센터, 스파/사우나 시설이 위치합니다.', 'Basement level 1. Parking (80 cars), fitness center, and spa/sauna facilities.', -1, true, NOW(), 'admin'),
(111, 5, '1F', '1층', '1층 로비 및 프론트데스크. 카페, 비즈니스코너, 컨시어지 데스크가 위치합니다.', '1st floor lobby and front desk. Cafe, business corner, and concierge desk located here.', 1, true, NOW(), 'admin'),
(112, 5, '2F', '2층', '2층 레스토랑 및 바. 한식당, 양식당, 루프탑 바가 위치합니다.', '2nd floor restaurants and bar. Korean restaurant, Western restaurant, and rooftop bar.', 2, true, NOW(), 'admin'),
(113, 5, '3F', '3층', '3층 이코노미 객실 구역. 이코노미 싱글 및 트윈 16실이 배치되어 있습니다.', '3rd floor economy room zone. 16 economy singles and twins available.', 3, true, NOW(), 'admin'),
(114, 5, '4F', '4층', '4층 비즈니스 객실 구역. 비즈니스 출장객 특화 객실 16실이 배치되어 있습니다.', '4th floor business room zone. 16 rooms specialized for business travelers.', 4, true, NOW(), 'admin'),
(115, 5, '11F', '11층', '11층 프리미엄 객실 구역. 서초/강남 시티뷰를 감상할 수 있는 프리미엄 객실 14실입니다.', '11th floor premium room zone. 14 premium rooms with Seocho/Gangnam city views.', 11, true, NOW(), 'admin'),
(116, 5, '12F', '12층', '12층 이그제큐티브 객실 구역. 이그제큐티브 전용 라운지 접근 가능 구역입니다.', '12th floor executive room zone. Executive lounge access available.', 12, true, NOW(), 'admin'),
(117, 5, '13F', '13층', '13층 클럽 플로어. 클럽 라운지, 조식, 해피아워 서비스가 제공됩니다.', '13th floor club level. Club lounge, breakfast, and happy hour services provided.', 13, true, NOW(), 'admin'),
(118, 5, '14F', '14층', '14층 주니어 스위트 구역. 넓은 거실과 분리된 침실 공간의 객실 10실입니다.', '14th floor junior suite zone. 10 rooms with spacious living areas and separate bedrooms.', 14, true, NOW(), 'admin'),
(119, 5, '15F', '15층', '15층 펜트하우스/로열 스위트. 최상층 VIP 전용 공간으로 3실이 운영됩니다.', '15th floor penthouse/royal suite. Top floor VIP exclusive space with 3 rooms.', 15, true, NOW(), 'admin'),
-- OBH (property_id=6) - 고층 및 루프탑 추가
(120, 6, 'B1', '지하 1층', '지하 1층. 주차장(150대 수용), 직원 시설, 세탁실, 창고가 위치합니다.', 'Basement level 1. Parking (150 cars), staff facilities, laundry room, and storage.', -1, true, NOW(), 'admin'),
(121, 6, '9F', '9층', '9층 이그제큐티브 오션뷰 구역. 해운대 바다 전망의 이그제큐티브 객실 20실이 배치되어 있습니다.', '9th floor executive ocean view zone. 20 executive rooms with Haeundae ocean views.', 9, true, NOW(), 'admin'),
(122, 6, '10F', '10층', '10층 프리미엄 오션프론트 구역. 해변 최전방 프리미엄 객실 18실이 위치합니다.', '10th floor premium oceanfront zone. 18 premium rooms with direct beachfront views.', 10, true, NOW(), 'admin'),
(123, 6, '11F', '11층', '11층 클럽 플로어. 클럽 라운지, 전용 체크인, 칵테일 아워 서비스 구역입니다.', '11th floor club level. Club lounge, private check-in, and cocktail hour service zone.', 11, true, NOW(), 'admin'),
(124, 6, '12F', '12층', '12층 주니어 스위트 구역. 오션뷰 주니어 스위트 15실이 배치되어 있습니다.', '12th floor junior suite zone. 15 ocean view junior suites available.', 12, true, NOW(), 'admin'),
(125, 6, '13F', '13층', '13층 스위트 구역. 다양한 등급의 스위트 객실 10실이 운영됩니다.', '13th floor suite zone. 10 suite rooms of various grades in operation.', 13, true, NOW(), 'admin'),
(126, 6, '14F', '14층', '14층 프레지덴셜 구역. 대형 스위트 및 프레지덴셜 스위트 5실이 위치합니다.', '14th floor presidential zone. Large suites and 5 presidential suites located here.', 14, true, NOW(), 'admin'),
(127, 6, '15F', '15층', '15층 펜트하우스 구역. 최고급 펜트하우스 3실이 운영되며 개인 테라스가 있습니다.', '15th floor penthouse zone. 3 luxury penthouses with private terraces.', 15, true, NOW(), 'admin'),
(128, 6, '16F', '16층', '16층 로열 펜트하우스. 전층 독점 사용 로열 펜트하우스 1실이 운영됩니다.', '16th floor royal penthouse. 1 royal penthouse with exclusive full-floor use.', 16, true, NOW(), 'admin'),
(129, 6, 'ROOF', '루프탑', '루프탑층. 인피니티 풀, 선셋 바, 바비큐 가든이 위치한 야외 시설 구역입니다.', 'Rooftop level. Outdoor facility zone with infinity pool, sunset bar, and BBQ garden.', 99, true, NOW(), 'admin');

-- ============================================================
-- 3. 호수 코드 (htl_room_number) - 30건 추가
-- ============================================================
INSERT INTO htl_room_number (id, property_id, room_number, description_ko, description_en, sort_order, use_yn, created_at, created_by) VALUES
-- GMP (property_id=4) - 지하/저층 객실 번호 추가
(200, 4, 'B101', '지하1층 101호. 비즈니스센터 인접 객실로 출장 고객에게 적합합니다.', 'B1F Room 101. Adjacent to business center, ideal for business travelers.', 1, true, NOW(), 'admin'),
(201, 4, 'B102', '지하1층 102호. 피트니스센터 인접 객실로 운동을 즐기는 고객에게 적합합니다.', 'B1F Room 102. Adjacent to fitness center, ideal for fitness-oriented guests.', 2, true, NOW(), 'admin'),
(202, 4, 'B201', '지하2층 201호. 직원 전용 숙소/휴게실 용도로 사용됩니다.', 'B2F Room 201. Used as staff accommodation/rest room.', 3, true, NOW(), 'admin'),
(203, 4, 'B202', '지하2층 202호. 직원 전용 숙소/휴게실 용도로 사용됩니다.', 'B2F Room 202. Used as staff accommodation/rest room.', 4, true, NOW(), 'admin'),
(204, 4, '101', '1층 101호. 로비 인접 장애인 편의시설 구비 배리어프리 객실입니다.', '1F Room 101. Barrier-free room near lobby with accessible facilities.', 5, true, NOW(), 'admin'),
(205, 4, '102', '1층 102호. 로비 인접 장애인 편의시설 구비 배리어프리 객실입니다.', '1F Room 102. Barrier-free room near lobby with accessible facilities.', 6, true, NOW(), 'admin'),
(206, 4, '301', '3층 301호. 이코노미 싱글 객실, 도심 방향 전망입니다.', '3F Room 301. Economy single room with city direction view.', 7, true, NOW(), 'admin'),
(207, 4, '302', '3층 302호. 이코노미 트윈 객실, 도심 방향 전망입니다.', '3F Room 302. Economy twin room with city direction view.', 8, true, NOW(), 'admin'),
(208, 4, '501', '5층 501호. 이그제큐티브 킹 객실, 이그제큐티브 라운지 이용 가능합니다.', '5F Room 501. Executive king room with executive lounge access.', 9, true, NOW(), 'admin'),
(209, 4, '502', '5층 502호. 이그제큐티브 트윈 객실, 이그제큐티브 라운지 이용 가능합니다.', '5F Room 502. Executive twin room with executive lounge access.', 10, true, NOW(), 'admin'),
-- GMS (property_id=5) - 저층/고층 객실 번호 추가
(210, 5, 'B101', '지하1층 101호. 스파/사우나 인접 객실, 피트니스 이용 편리합니다.', 'B1F Room 101. Near spa/sauna, convenient for fitness activities.', 1, true, NOW(), 'admin'),
(211, 5, '101', '1층 101호. 로비 인접 배리어프리 객실, 휠체어 접근 가능합니다.', '1F Room 101. Barrier-free room near lobby, wheelchair accessible.', 2, true, NOW(), 'admin'),
(212, 5, '102', '1층 102호. 로비 인접 배리어프리 객실, 휠체어 접근 가능합니다.', '1F Room 102. Barrier-free room near lobby, wheelchair accessible.', 3, true, NOW(), 'admin'),
(213, 5, '301', '3층 301호. 이코노미 싱글, 서초/강남 도심 방향 전망입니다.', '3F Room 301. Economy single with Seocho/Gangnam city view.', 4, true, NOW(), 'admin'),
(214, 5, '302', '3층 302호. 이코노미 트윈, 서초/강남 도심 방향 전망입니다.', '3F Room 302. Economy twin with Seocho/Gangnam city view.', 5, true, NOW(), 'admin'),
(215, 5, '1101', '11층 1101호. 프리미엄 킹, 강남 스카이라인 파노라마뷰 객실입니다.', '11F Room 1101. Premium king with Gangnam skyline panoramic view.', 6, true, NOW(), 'admin'),
(216, 5, '1102', '11층 1102호. 프리미엄 트윈, 강남 스카이라인 파노라마뷰 객실입니다.', '11F Room 1102. Premium twin with Gangnam skyline panoramic view.', 7, true, NOW(), 'admin'),
(217, 5, '1201', '12층 1201호. 이그제큐티브 킹, 이그제큐티브 라운지 접근 가능 객실입니다.', '12F Room 1201. Executive king with executive lounge access.', 8, true, NOW(), 'admin'),
(218, 5, '1301', '13층 1301호. 클럽 플로어 킹, 클럽 라운지 및 해피아워 서비스 포함입니다.', '13F Room 1301. Club floor king with club lounge and happy hour services.', 9, true, NOW(), 'admin'),
(219, 5, '1501', '15층 1501호. 펜트하우스 스위트, 서초/반포 한강뷰 최상층 VIP 객실입니다.', '15F Room 1501. Penthouse suite, top floor VIP room with Seocho/Banpo Han River view.', 10, true, NOW(), 'admin'),
-- OBH (property_id=6) - 고층 객실 번호 추가
(220, 6, '901', '9층 901호. 이그제큐티브 킹, 해운대 바다 정면 오션뷰 객실입니다.', '9F Room 901. Executive king with direct Haeundae ocean front view.', 1, true, NOW(), 'admin'),
(221, 6, '902', '9층 902호. 이그제큐티브 트윈, 해운대 바다 정면 오션뷰 객실입니다.', '9F Room 902. Executive twin with direct Haeundae ocean front view.', 2, true, NOW(), 'admin'),
(222, 6, '1001', '10층 1001호. 프리미엄 오션프론트 킹, 해변 최전방 파노라마 전망입니다.', '10F Room 1001. Premium oceanfront king with panoramic beachfront view.', 3, true, NOW(), 'admin'),
(223, 6, '1002', '10층 1002호. 프리미엄 오션프론트 트윈, 해변 최전방 파노라마 전망입니다.', '10F Room 1002. Premium oceanfront twin with panoramic beachfront view.', 4, true, NOW(), 'admin'),
(224, 6, '1101', '11층 1101호. 클럽 플로어 킹, 클럽 라운지 전용 접근 및 오션뷰입니다.', '11F Room 1101. Club floor king with exclusive club lounge access and ocean view.', 5, true, NOW(), 'admin'),
(225, 6, '1201', '12층 1201호. 주니어 스위트, 넓은 거실과 오션뷰가 제공됩니다.', '12F Room 1201. Junior suite with spacious living area and ocean view.', 6, true, NOW(), 'admin'),
(226, 6, '1301', '13층 1301호. 디럭스 스위트, 침실 분리형 해운대 오션뷰 객실입니다.', '13F Room 1301. Deluxe suite with separate bedroom and Haeundae ocean view.', 7, true, NOW(), 'admin'),
(227, 6, '1401', '14층 1401호. 프레지덴셜 스위트, 대형 거실/침실/서재 구성의 VIP 객실입니다.', '14F Room 1401. Presidential suite, VIP room with large living room, bedroom, and study.', 8, true, NOW(), 'admin'),
(228, 6, '1501', '15층 1501호. 펜트하우스, 개인 테라스 및 자쿠지가 있는 최고급 객실입니다.', '15F Room 1501. Penthouse with private terrace and jacuzzi, the finest room.', 9, true, NOW(), 'admin'),
(229, 6, '1601', '16층 1601호. 로열 펜트하우스, 전층 독점 사용 최상위 객실입니다.', '16F Room 1601. Royal penthouse, the ultimate room with exclusive full-floor use.', 10, true, NOW(), 'admin');

-- ============================================================
-- 4. 객실 그룹 (rm_room_class) - 30건 추가
-- 기존: STD(Standard), DLX(Deluxe), SUI(Suite) 프로퍼티당 3건
-- ============================================================
INSERT INTO rm_room_class (id, property_id, room_class_code, room_class_name, description, sort_order, use_yn, created_at, created_by) VALUES
-- GMP (property_id=4)
(100, 4, 'ECO', '이코노미', '합리적인 가격의 기본 객실 그룹. 출장 및 단기 투숙에 적합하며, 필수 편의시설을 제공합니다. Economy class rooms with essential amenities at reasonable prices.', 1, true, NOW(), 'admin'),
(101, 4, 'BUS', '비즈니스', '비즈니스 출장객 특화 객실 그룹. 넓은 업무 공간, 고속 인터넷, 업무용 데스크가 구비됩니다. Business class rooms with spacious work area and high-speed internet.', 2, true, NOW(), 'admin'),
(102, 4, 'EXE', '이그제큐티브', '이그제큐티브 전용 라운지 이용 가능 프리미엄 객실 그룹. 조식, 해피아워, 전용 체크인 서비스가 포함됩니다. Executive class with lounge access, breakfast, and happy hour.', 5, true, NOW(), 'admin'),
(103, 4, 'PRE', '프리미엄', '고급 인테리어와 프리미엄 어메니티가 제공되는 상위 객실 그룹. 고층 시티뷰 및 프리미엄 침구류가 특징입니다. Premium class with luxury interior, premium amenities and city views.', 6, true, NOW(), 'admin'),
(104, 4, 'CLB', '클럽', '클럽 라운지 접근 가능 특별 객실 그룹. 전용 라운지, 칵테일 아워, 프리미엄 간식 서비스가 제공됩니다. Club class with exclusive lounge access, cocktail hour, and premium snacks.', 7, true, NOW(), 'admin'),
(105, 4, 'FAM', '패밀리', '가족 투숙객 특화 객실 그룹. 넓은 공간, 추가 침대, 키즈 어메니티, 커넥팅룸 옵션이 제공됩니다. Family class with spacious rooms, extra beds, kids amenities, and connecting room options.', 8, true, NOW(), 'admin'),
(106, 4, 'JNR', '주니어 스위트', '거실과 침실이 오픈형으로 구성된 준 스위트급 객실 그룹. 넓은 공간과 고급 어메니티가 특징입니다. Junior suite class with open-plan living and bedroom areas.', 9, true, NOW(), 'admin'),
(107, 4, 'PEN', '펜트하우스', '최상층 전용 최고급 객실 그룹. 넓은 거실, 별도 다이닝, 개인 테라스, 버틀러 서비스가 제공됩니다. Penthouse class on top floors with large living room, dining, terrace, and butler service.', 11, true, NOW(), 'admin'),
(108, 4, 'RYL', '로열', '왕실급 최상위 객실 그룹. 전층 독점 사용, 전용 엘리베이터, 24시간 버틀러 서비스가 제공됩니다. Royal class, the finest rooms with full-floor exclusive use and 24hr butler service.', 12, true, NOW(), 'admin'),
(109, 4, 'HNM', '허니문', '신혼여행객 특화 로맨틱 객실 그룹. 자쿠지, 로맨틱 장식, 웰컴 샴페인, 조식 서비스가 포함됩니다. Honeymoon class with jacuzzi, romantic decoration, welcome champagne, and breakfast.', 13, true, NOW(), 'admin'),
-- GMS (property_id=5)
(110, 5, 'ECO', '이코노미', '합리적인 가격의 기본 객실 그룹. 서초 지역 출장 및 단기 투숙에 적합합니다. Economy class at reasonable prices, ideal for Seocho area business trips.', 1, true, NOW(), 'admin'),
(111, 5, 'BUS', '비즈니스', '비즈니스 출장객 특화 객실 그룹. 강남/서초 비즈니스 지구 인접으로 업무 편의성이 높습니다. Business class specialized for Gangnam/Seocho business district travelers.', 2, true, NOW(), 'admin'),
(112, 5, 'EXE', '이그제큐티브', '이그제큐티브 라운지 이용 프리미엄 객실 그룹. 조식, 해피아워, 전용 체크인이 포함됩니다. Executive class with lounge access, breakfast, happy hour, and private check-in.', 5, true, NOW(), 'admin'),
(113, 5, 'PRE', '프리미엄', '고급 인테리어와 프리미엄 어메니티 상위 객실 그룹. 강남 스카이라인 뷰가 특징입니다. Premium class with luxury interior and Gangnam skyline views.', 6, true, NOW(), 'admin'),
(114, 5, 'CLB', '클럽', '클럽 라운지 접근 가능 특별 객실 그룹. 전용 라운지와 칵테일 아워가 제공됩니다. Club class with exclusive lounge access and cocktail hour services.', 7, true, NOW(), 'admin'),
(115, 5, 'FAM', '패밀리', '가족 투숙객 특화 객실 그룹. 넓은 공간과 키즈 어메니티가 제공됩니다. Family class with spacious rooms and kids amenities.', 8, true, NOW(), 'admin'),
(116, 5, 'JNR', '주니어 스위트', '거실과 침실 오픈형 준 스위트 객실 그룹. 넓은 공간이 특징입니다. Junior suite class with open-plan living and bedroom areas.', 9, true, NOW(), 'admin'),
(117, 5, 'PEN', '펜트하우스', '최상층 전용 최고급 객실 그룹. 한강/서초 파노라마뷰와 버틀러 서비스가 제공됩니다. Penthouse class with Han River/Seocho panoramic view and butler service.', 11, true, NOW(), 'admin'),
(118, 5, 'RYL', '로열', '왕실급 최상위 객실 그룹. 전층 독점, 전용 엘리베이터, 24시간 버틀러 서비스입니다. Royal class with full-floor exclusive use, private elevator, and 24hr butler.', 12, true, NOW(), 'admin'),
(119, 5, 'HNM', '허니문', '신혼여행객 로맨틱 객실 그룹. 자쿠지, 로맨틱 장식, 샴페인 서비스가 포함됩니다. Honeymoon class with jacuzzi, romantic decoration, and champagne service.', 13, true, NOW(), 'admin'),
-- OBH (property_id=6)
(120, 6, 'ECO', '이코노미', '합리적인 가격의 기본 객실 그룹. 해운대 여행 합리적 가격을 원하는 고객에게 적합합니다. Economy class at reasonable prices for budget-conscious Haeundae travelers.', 1, true, NOW(), 'admin'),
(121, 6, 'BUS', '비즈니스', '비즈니스 출장객 특화 객실 그룹. 해운대 MICE 행사 참가자에게 적합합니다. Business class for Haeundae MICE event participants.', 2, true, NOW(), 'admin'),
(122, 6, 'EXE', '이그제큐티브', '이그제큐티브 오션뷰 프리미엄 객실 그룹. 해운대 바다 전망과 라운지 서비스가 포함됩니다. Executive class with Haeundae ocean view and lounge services.', 5, true, NOW(), 'admin'),
(123, 6, 'PRE', '프리미엄', '프리미엄 오션프론트 상위 객실 그룹. 해변 최전방 파노라마 오션뷰가 특징입니다. Premium oceanfront class with panoramic beachfront ocean views.', 6, true, NOW(), 'admin'),
(124, 6, 'CLB', '클럽', '클럽 플로어 오션뷰 특별 객실 그룹. 클럽 라운지, 칵테일 아워, 프라이빗 비치 서비스입니다. Club class with ocean view, club lounge, cocktail hour, and private beach service.', 7, true, NOW(), 'admin'),
(125, 6, 'FAM', '패밀리', '가족 투숙객 특화 객실 그룹. 해수욕장 인접, 키즈풀, 워터파크 접근이 편리합니다. Family class near beach with kids pool and waterpark access.', 8, true, NOW(), 'admin'),
(126, 6, 'JNR', '주니어 스위트', '오션뷰 주니어 스위트 객실 그룹. 넓은 거실과 해운대 바다 전망이 특징입니다. Junior suite class with spacious living area and Haeundae ocean views.', 9, true, NOW(), 'admin'),
(127, 6, 'PEN', '펜트하우스', '최상층 오션프론트 펜트하우스. 개인 테라스, 자쿠지, 파노라마 오션뷰가 제공됩니다. Penthouse with private terrace, jacuzzi, and panoramic ocean views.', 11, true, NOW(), 'admin'),
(128, 6, 'RYL', '로열', '왕실급 최상위 객실 그룹. 전층 독점 오션프론트, 전용 버틀러 서비스입니다. Royal class with exclusive full-floor oceanfront and private butler service.', 12, true, NOW(), 'admin'),
(129, 6, 'HNM', '허니문', '신혼여행객 특화 오션뷰 객실 그룹. 자쿠지, 해변 디너, 커플 스파가 포함됩니다. Honeymoon class with ocean view, jacuzzi, beach dinner, and couple spa.', 13, true, NOW(), 'admin');

-- ============================================================
-- 5. 예약 채널 (htl_reservation_channel) - 30건 추가
-- 기존: WALK_IN, PHONE, EMAIL, OTA_BOOKING, OTA_AGODA, OTA_EXPEDIA, WEBSITE (프로퍼티당 7건)
-- ============================================================
INSERT INTO htl_reservation_channel (id, property_id, channel_code, channel_name, channel_type, description_ko, description_en, sort_order, use_yn, created_at, created_by) VALUES
-- GMP (property_id=4)
(100, 4, 'OTA_TRIP', '트립닷컴', 'OTA', '트립닷컴(Trip.com) OTA 채널. 중화권 고객 유입 비중이 높으며, 실시간 재고 연동됩니다.', 'Trip.com OTA channel. High proportion of Chinese-speaking guests with real-time inventory sync.', 8, true, NOW(), 'admin'),
(101, 4, 'OTA_HOTELS', '호텔스닷컴', 'OTA', '호텔스닷컴(Hotels.com) OTA 채널. Expedia 그룹 소속으로 글로벌 고객 대상입니다.', 'Hotels.com OTA channel. Part of Expedia Group targeting global guests.', 9, true, NOW(), 'admin'),
(102, 4, 'OTA_CTRIP', '씨트립', 'OTA', '씨트립(Ctrip) OTA 채널. 중국 최대 OTA 플랫폼으로 중국인 관광객 전문 채널입니다.', 'Ctrip OTA channel. China''s largest OTA platform specializing in Chinese tourists.', 10, true, NOW(), 'admin'),
(103, 4, 'B2B_CORP', '기업 직접계약', 'B2B', '기업 법인과 직접 계약을 통한 B2B 채널. 정기 계약 할인 요금이 적용됩니다.', 'B2B channel through direct contracts with corporate clients. Regular contract discount rates apply.', 11, true, NOW(), 'admin'),
(104, 4, 'B2B_AGT', '여행사 계약', 'B2B', '국내외 여행사와의 B2B 계약 채널. 넷 레이트(net rate) 기반 커미션 정산입니다.', 'B2B contract channel with domestic/international travel agencies. Net rate based commission settlement.', 12, true, NOW(), 'admin'),
(105, 4, 'B2B_MICE', 'MICE 단체', 'B2B', 'MICE 행사 기획사 및 단체 계약 채널. 대규모 객실 블록 예약 및 연회장 패키지가 포함됩니다.', 'MICE event planner and group contract channel. Large room block bookings and banquet packages included.', 13, true, NOW(), 'admin'),
(106, 4, 'SOCIAL', '소셜커머스', 'WEBSITE', '쿠팡, 티몬, 위메프 등 소셜커머스 플랫폼을 통한 예약 채널입니다.', 'Reservation channel through social commerce platforms such as Coupang, Tmon, WeMakePrice.', 14, true, NOW(), 'admin'),
(107, 4, 'APP', '자사 앱', 'WEBSITE', '호텔 자체 모바일 앱을 통한 직접 예약 채널. 앱 전용 할인 및 포인트 적립이 제공됩니다.', 'Direct booking channel through hotel mobile app. App-exclusive discounts and point accumulation available.', 15, true, NOW(), 'admin'),
(108, 4, 'SNS', 'SNS 채널', 'WEBSITE', '인스타그램, 카카오톡 채널 등 소셜미디어를 통한 예약 채널. DM 또는 링크 예약입니다.', 'Booking channel through social media platforms like Instagram and KakaoTalk. Reservation via DM or link.', 16, true, NOW(), 'admin'),
(109, 4, 'GOV_DIR', '관공서 직접', 'B2B', '정부 기관, 지자체, 공기업 등 관공서 직접 예약 채널. 관용 요금이 적용됩니다.', 'Direct booking channel for government agencies and public enterprises. Government rates apply.', 17, true, NOW(), 'admin'),
-- GMS (property_id=5)
(110, 5, 'OTA_TRIP', '트립닷컴', 'OTA', '트립닷컴(Trip.com) OTA 채널. 중화권 고객 실시간 재고 연동 채널입니다.', 'Trip.com OTA channel with real-time inventory sync for Chinese-speaking guests.', 8, true, NOW(), 'admin'),
(111, 5, 'OTA_HOTELS', '호텔스닷컴', 'OTA', '호텔스닷컴(Hotels.com) OTA 채널. Expedia 그룹 글로벌 고객 대상 채널입니다.', 'Hotels.com OTA channel targeting global guests through Expedia Group.', 9, true, NOW(), 'admin'),
(112, 5, 'OTA_CTRIP', '씨트립', 'OTA', '씨트립(Ctrip) OTA 채널. 중국 관광객 전문 채널입니다.', 'Ctrip OTA channel specializing in Chinese tourists.', 10, true, NOW(), 'admin'),
(113, 5, 'B2B_CORP', '기업 직접계약', 'B2B', '강남/서초 지역 기업 법인 직접 계약 B2B 채널. 정기 계약 요금 적용입니다.', 'B2B channel for Gangnam/Seocho area corporate direct contracts. Regular contract rates apply.', 11, true, NOW(), 'admin'),
(114, 5, 'B2B_AGT', '여행사 계약', 'B2B', '여행사 B2B 계약 채널. 넷 레이트 기반 커미션 정산이 수반됩니다.', 'Travel agency B2B contract channel. Net rate based commission settlement.', 12, true, NOW(), 'admin'),
(115, 5, 'B2B_MICE', 'MICE 단체', 'B2B', 'MICE 행사 및 단체 계약 채널. 강남 COEX 인접으로 컨벤션 수요가 높습니다.', 'MICE event and group contract channel. High convention demand due to COEX proximity.', 13, true, NOW(), 'admin'),
(116, 5, 'SOCIAL', '소셜커머스', 'WEBSITE', '소셜커머스 플랫폼 통한 예약 채널. 쿠팡, 티몬 등 주요 플랫폼 연동입니다.', 'Reservation via social commerce platforms. Integrated with major platforms like Coupang, Tmon.', 14, true, NOW(), 'admin'),
(117, 5, 'APP', '자사 앱', 'WEBSITE', '호텔 모바일 앱 직접 예약 채널. 앱 전용 할인과 포인트 적립이 가능합니다.', 'Direct booking via hotel mobile app. App-exclusive discounts and point accumulation available.', 15, true, NOW(), 'admin'),
(118, 5, 'SNS', 'SNS 채널', 'WEBSITE', '소셜미디어 통한 예약 채널. 인스타그램, 카카오톡 채널 DM/링크 예약입니다.', 'Booking via social media. Reservation through Instagram and KakaoTalk DM/link.', 16, true, NOW(), 'admin'),
(119, 5, 'GOV_DIR', '관공서 직접', 'B2B', '관공서 직접 예약 채널. 정부 기관, 공기업 관용 요금이 적용됩니다.', 'Government direct booking channel. Government and public enterprise rates apply.', 17, true, NOW(), 'admin'),
-- OBH (property_id=6)
(120, 6, 'OTA_TRIP', '트립닷컴', 'OTA', '트립닷컴 OTA 채널. 부산/해운대 방문 중화권 관광객 비중이 높은 채널입니다.', 'Trip.com OTA channel. High proportion of Chinese-speaking tourists visiting Busan/Haeundae.', 8, true, NOW(), 'admin'),
(121, 6, 'OTA_HOTELS', '호텔스닷컴', 'OTA', '호텔스닷컴 OTA 채널. 해운대 비치 리조트 글로벌 마케팅 채널입니다.', 'Hotels.com OTA channel. Global marketing channel for Haeundae beach resort.', 9, true, NOW(), 'admin'),
(122, 6, 'OTA_CTRIP', '씨트립', 'OTA', '씨트립 OTA 채널. 부산 방문 중국 관광객 전문 채널입니다.', 'Ctrip OTA channel specializing in Chinese tourists visiting Busan.', 10, true, NOW(), 'admin'),
(123, 6, 'B2B_CORP', '기업 직접계약', 'B2B', '부산 지역 기업 법인 직접 계약 B2B 채널. 해운대 워크숍/세미나 수요입니다.', 'B2B for Busan area corporate contracts. Haeundae workshop/seminar demand.', 11, true, NOW(), 'admin'),
(124, 6, 'B2B_AGT', '여행사 계약', 'B2B', '부산/해운대 전문 여행사 B2B 채널. 관광 패키지 요금이 적용됩니다.', 'Busan/Haeundae specialized travel agency B2B channel. Tour package rates apply.', 12, true, NOW(), 'admin'),
(125, 6, 'B2B_MICE', 'MICE 단체', 'B2B', 'MICE 행사 단체 채널. 해운대 BEXCO 컨벤션 참가 단체 수요입니다.', 'MICE group channel. Demand from BEXCO convention participant groups.', 13, true, NOW(), 'admin'),
(126, 6, 'SOCIAL', '소셜커머스', 'WEBSITE', '소셜커머스 예약 채널. 여름철 해운대 특가 프로모션이 활발합니다.', 'Social commerce booking channel. Active summer Haeundae special promotions.', 14, true, NOW(), 'admin'),
(127, 6, 'APP', '자사 앱', 'WEBSITE', '호텔 모바일 앱 직접 예약 채널. 해변 시설 실시간 예약 기능이 포함됩니다.', 'Hotel mobile app direct booking. Includes real-time beach facility reservation.', 15, true, NOW(), 'admin'),
(128, 6, 'SNS', 'SNS 채널', 'WEBSITE', '소셜미디어 예약 채널. 해운대 뷰 인스타 인증샷 이벤트 연계 예약입니다.', 'Social media booking channel. Linked to Haeundae view Instagram certification shot events.', 16, true, NOW(), 'admin'),
(129, 6, 'GOV_DIR', '관공서 직접', 'B2B', '관공서 직접 예약 채널. 부산시청, 해운대구청 등 지자체 관용 요금 적용입니다.', 'Government direct booking. Busan City Hall and Haeundae District government rates apply.', 17, true, NOW(), 'admin');

-- ============================================================
-- 6. 시퀀스 리셋
-- ============================================================
SELECT setval('htl_market_code_id_seq', (SELECT COALESCE(MAX(id), 0) FROM htl_market_code));
SELECT setval('htl_floor_id_seq', (SELECT COALESCE(MAX(id), 0) FROM htl_floor));
SELECT setval('htl_room_number_id_seq', (SELECT COALESCE(MAX(id), 0) FROM htl_room_number));
SELECT setval('rm_room_class_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_room_class));
SELECT setval('htl_reservation_channel_id_seq', (SELECT COALESCE(MAX(id), 0) FROM htl_reservation_channel));
