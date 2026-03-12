-- ============================================================
-- V5_10_0: 테스트 데이터 추가 Batch 2
-- 무료 서비스 옵션(30), 유료 서비스 옵션(30)
-- 모든 서비스 유형(service_type)에 빠짐없이 여러 개 추가
-- ============================================================

-- ============================================================
-- 1. 무료 서비스 옵션 (rm_free_service_option) - 30건 추가
-- 기존 유형: BED, VIEW, ROOM_AMENITY, BREAKFAST, INTERNET, WELCOME, AMENITY
-- 각 유형별 최소 1건 이상 추가하여 모든 유형 커버
-- ============================================================
INSERT INTO rm_free_service_option (id, property_id, service_option_code, service_name_ko, service_name_en, service_type, applicable_nights, quantity, quantity_unit, sort_order, use_yn, created_at, created_by) VALUES
-- GMP (property_id=4)
(100, 4, 'BED-QUEEN', '퀸 베드', 'Queen Bed', 'BED', 'ALL_NIGHTS', 1, 'EA', 4, true, NOW(), 'admin'),
(101, 4, 'BED-SOFA', '소파 베드 (보조침대)', 'Sofa Bed (Extra Bed)', 'BED', 'ALL_NIGHTS', 1, 'EA', 5, true, NOW(), 'admin'),
(102, 4, 'VIEW-GARDEN', '가든뷰 (정원 전망)', 'Garden View', 'VIEW', 'ALL_NIGHTS', 1, 'EA', 7, true, NOW(), 'admin'),
(103, 4, 'VIEW-POOL', '풀뷰 (수영장 전망)', 'Pool View', 'VIEW', 'ALL_NIGHTS', 1, 'EA', 8, true, NOW(), 'admin'),
(104, 4, 'RA-ROBE', '고급 바스로브 2벌', 'Premium Bathrobe (Set of 2)', 'ROOM_AMENITY', 'ALL_NIGHTS', 2, 'EA', 11, true, NOW(), 'admin'),
(105, 4, 'RA-COFFEE', '네스프레소 커피머신 및 캡슐 6개', 'Nespresso Coffee Machine with 6 Capsules', 'ROOM_AMENITY', 'ALL_NIGHTS', 1, 'SET', 12, true, NOW(), 'admin'),
(106, 4, 'BF-CONTI', '컨티넨탈 조식 (빵, 과일, 주스)', 'Continental Breakfast (Bread, Fruit, Juice)', 'BREAKFAST', 'ALL_NIGHTS', 1, 'SERVICE', 14, true, NOW(), 'admin'),
(107, 4, 'WIFI-PRE', '프리미엄 고속 Wi-Fi (100Mbps)', 'Premium High-Speed Wi-Fi (100Mbps)', 'INTERNET', 'ALL_NIGHTS', 1, 'SERVICE', 16, true, NOW(), 'admin'),
(108, 4, 'WEL-FRUIT', '웰컴 과일 바구니 (계절 과일 5종)', 'Welcome Fruit Basket (5 Seasonal Fruits)', 'WELCOME', 'FIRST_NIGHT_ONLY', 1, 'SET', 18, true, NOW(), 'admin'),
(109, 4, 'AM-BATH', '프리미엄 배스 어메니티 세트 (로레알/로크시탕)', 'Premium Bath Amenity Set (L''Oreal/L''Occitane)', 'AMENITY', 'ALL_NIGHTS', 1, 'SET', 20, true, NOW(), 'admin'),
-- GMS (property_id=5)
(110, 5, 'BED-QUEEN', '퀸 베드', 'Queen Bed', 'BED', 'ALL_NIGHTS', 1, 'EA', 4, true, NOW(), 'admin'),
(111, 5, 'BED-SOFA', '소파 베드 (보조침대)', 'Sofa Bed (Extra Bed)', 'BED', 'ALL_NIGHTS', 1, 'EA', 5, true, NOW(), 'admin'),
(112, 5, 'VIEW-GARDEN', '가든뷰 (강남 가로수길 방향)', 'Garden View (Gangnam Garosu-gil Direction)', 'VIEW', 'ALL_NIGHTS', 1, 'EA', 7, true, NOW(), 'admin'),
(113, 5, 'VIEW-RIVER', '리버뷰 (한강 방향 전망)', 'River View (Han River Direction)', 'VIEW', 'ALL_NIGHTS', 1, 'EA', 8, true, NOW(), 'admin'),
(114, 5, 'RA-ROBE', '고급 바스로브 2벌', 'Premium Bathrobe (Set of 2)', 'ROOM_AMENITY', 'ALL_NIGHTS', 2, 'EA', 11, true, NOW(), 'admin'),
(115, 5, 'RA-COFFEE', '네스프레소 커피머신 및 캡슐 6개', 'Nespresso Coffee Machine with 6 Capsules', 'ROOM_AMENITY', 'ALL_NIGHTS', 1, 'SET', 12, true, NOW(), 'admin'),
(116, 5, 'BF-CONTI', '컨티넨탈 조식 (빵, 과일, 주스)', 'Continental Breakfast (Bread, Fruit, Juice)', 'BREAKFAST', 'ALL_NIGHTS', 1, 'SERVICE', 14, true, NOW(), 'admin'),
(117, 5, 'WIFI-PRE', '프리미엄 고속 Wi-Fi (100Mbps)', 'Premium High-Speed Wi-Fi (100Mbps)', 'INTERNET', 'ALL_NIGHTS', 1, 'SERVICE', 16, true, NOW(), 'admin'),
(118, 5, 'WEL-FRUIT', '웰컴 과일 바구니 (계절 과일 5종)', 'Welcome Fruit Basket (5 Seasonal Fruits)', 'WELCOME', 'FIRST_NIGHT_ONLY', 1, 'SET', 18, true, NOW(), 'admin'),
(119, 5, 'AM-BATH', '프리미엄 배스 어메니티 세트', 'Premium Bath Amenity Set', 'AMENITY', 'ALL_NIGHTS', 1, 'SET', 20, true, NOW(), 'admin'),
-- OBH (property_id=6)
(120, 6, 'BED-QUEEN', '퀸 베드', 'Queen Bed', 'BED', 'ALL_NIGHTS', 1, 'EA', 4, true, NOW(), 'admin'),
(121, 6, 'BED-SOFA', '소파 베드 (보조침대)', 'Sofa Bed (Extra Bed)', 'BED', 'ALL_NIGHTS', 1, 'EA', 5, true, NOW(), 'admin'),
(122, 6, 'VIEW-BEACH', '비치프론트뷰 (해변 직면 전망)', 'Beachfront View (Direct Beach View)', 'VIEW', 'ALL_NIGHTS', 1, 'EA', 7, true, NOW(), 'admin'),
(123, 6, 'VIEW-POOL', '풀뷰 (인피니티 풀 전망)', 'Pool View (Infinity Pool View)', 'VIEW', 'ALL_NIGHTS', 1, 'EA', 8, true, NOW(), 'admin'),
(124, 6, 'RA-ROBE', '고급 바스로브 2벌', 'Premium Bathrobe (Set of 2)', 'ROOM_AMENITY', 'ALL_NIGHTS', 2, 'EA', 11, true, NOW(), 'admin'),
(125, 6, 'RA-COFFEE', '네스프레소 커피머신 및 캡슐 6개', 'Nespresso Coffee Machine with 6 Capsules', 'ROOM_AMENITY', 'ALL_NIGHTS', 1, 'SET', 12, true, NOW(), 'admin'),
(126, 6, 'BF-CONTI', '컨티넨탈 조식 (빵, 과일, 주스)', 'Continental Breakfast (Bread, Fruit, Juice)', 'BREAKFAST', 'ALL_NIGHTS', 1, 'SERVICE', 14, true, NOW(), 'admin'),
(127, 6, 'WIFI-PRE', '프리미엄 고속 Wi-Fi (100Mbps)', 'Premium High-Speed Wi-Fi (100Mbps)', 'INTERNET', 'ALL_NIGHTS', 1, 'SERVICE', 16, true, NOW(), 'admin'),
(128, 6, 'WEL-FRUIT', '웰컴 열대 과일 바구니 (망고, 파인애플 등)', 'Welcome Tropical Fruit Basket (Mango, Pineapple, etc.)', 'WELCOME', 'FIRST_NIGHT_ONLY', 1, 'SET', 18, true, NOW(), 'admin'),
(129, 6, 'AM-BATH', '해양 미네랄 배스 어메니티 세트', 'Ocean Mineral Bath Amenity Set', 'AMENITY', 'ALL_NIGHTS', 1, 'SET', 20, true, NOW(), 'admin');

-- ============================================================
-- 2. 유료 서비스 옵션 (rm_paid_service_option) - 30건 추가
-- 기존 유형: ROOM_AMENITY, BREAKFAST_PAID, ROOM_SERVICE, SPA_WELLNESS, LAUNDRY, MINIBAR, ROOM_UPGRADE
-- 각 유형별 최소 1건 이상 추가하여 모든 유형 커버
-- ============================================================
INSERT INTO rm_paid_service_option (id, property_id, service_option_code, service_name_ko, service_name_en, service_type, applicable_nights, currency_code, vat_included, tax_rate, supply_price, tax_amount, vat_included_price, quantity, quantity_unit, admin_memo, sort_order, use_yn, created_at, created_by) VALUES
-- GMP (property_id=4)
(100, 4, 'PA-FLOWER', '객실 꽃 장식 서비스', 'Room Flower Decoration Service', 'ROOM_AMENITY', 'FIRST_NIGHT_ONLY', 'KRW', true, 10.00, 45455, 4545, 50000, 1, 'SET', '생일, 프로포즈, 기념일 등 특별 이벤트 시 예약 가능. 장미, 카네이션 등 선택 가능.', 9, true, NOW(), 'admin'),
(101, 4, 'PA-TURNDOWN', '프리미엄 턴다운 서비스', 'Premium Turndown Service', 'ROOM_AMENITY', 'ALL_NIGHTS', 'KRW', true, 10.00, 27273, 2727, 30000, 1, 'SERVICE', '저녁 턴다운 시 고급 초콜릿, 아로마 디퓨저 세팅, 침구 교체 서비스.', 10, true, NOW(), 'admin'),
(102, 4, 'BF-ROOM', '룸서비스 조식 (코스)', 'Room Service Breakfast (Course)', 'BREAKFAST_PAID', 'ALL_NIGHTS', 'KRW', true, 10.00, 50000, 5000, 55000, 1, 'SERVICE', '객실 내 풀코스 조식 서비스. 양식/한식 선택 가능, 30분 전 주문 필요.', 11, true, NOW(), 'admin'),
(103, 4, 'RS-WINE', '와인 딜리버리 서비스', 'Wine Delivery Service', 'ROOM_SERVICE', 'ALL_NIGHTS', 'KRW', true, 10.00, 72727, 7273, 80000, 1, 'EA', '소믈리에 추천 와인 객실 배달. 레드/화이트/스파클링 선택 가능.', 12, true, NOW(), 'admin'),
(104, 4, 'RS-CAKE', '케이크 딜리버리 서비스', 'Cake Delivery Service', 'ROOM_SERVICE', 'FIRST_NIGHT_ONLY', 'KRW', true, 10.00, 40909, 4091, 45000, 1, 'EA', '생일/기념일 케이크 객실 배달. 초코/딸기/치즈 중 선택, 1일 전 예약 필요.', 13, true, NOW(), 'admin'),
(105, 4, 'SPA-COUPLE', '커플 스파 패키지', 'Couple Spa Package', 'SPA_WELLNESS', 'FIRST_NIGHT_ONLY', 'KRW', true, 10.00, 181818, 18182, 200000, 2, 'SERVICE', '2인 동시 아로마 마사지 90분 + 족욕 + 허브티 서비스.', 14, true, NOW(), 'admin'),
(106, 4, 'SPA-AROMA', '아로마 테라피 60분', 'Aroma Therapy 60min', 'SPA_WELLNESS', 'NOT_APPLICABLE', 'KRW', true, 10.00, 109091, 10909, 120000, 1, 'SERVICE', '라벤더/유칼립투스/페퍼민트 등 에센셜 오일 선택 가능. 예약 필수.', 15, true, NOW(), 'admin'),
(107, 4, 'LAUNDRY-EX', '특급 세탁 서비스 (당일)', 'Express Laundry Service (Same Day)', 'LAUNDRY', 'NOT_APPLICABLE', 'KRW', true, 10.00, 22727, 2273, 25000, 1, 'EA', '오전 10시 이전 접수 시 당일 오후 6시 완료. 1벌 기준 가격.', 16, true, NOW(), 'admin'),
(108, 4, 'MINIBAR-DX', '디럭스 미니바 패키지', 'Deluxe Minibar Package', 'MINIBAR', 'ALL_NIGHTS', 'KRW', true, 10.00, 136364, 13636, 150000, 1, 'SET', '프리미엄 주류 4종 + 스낵 6종 + 음료 4종 풀 패키지. 매일 리필.', 17, true, NOW(), 'admin'),
(109, 4, 'UPGRADE-VW', '뷰 업그레이드 (시티뷰→프리미엄)', 'View Upgrade (City→Premium)', 'ROOM_UPGRADE', 'ALL_NIGHTS', 'KRW', true, 10.00, 90909, 9091, 100000, 1, 'SERVICE', '일반 시티뷰에서 고층 프리미엄뷰로 업그레이드. 잔여 객실 상황에 따라 제공.', 18, true, NOW(), 'admin'),
-- GMS (property_id=5)
(110, 5, 'PA-FLOWER', '객실 꽃 장식 서비스', 'Room Flower Decoration Service', 'ROOM_AMENITY', 'FIRST_NIGHT_ONLY', 'KRW', true, 10.00, 45455, 4545, 50000, 1, 'SET', '생일, 프로포즈, 기념일 등 특별 이벤트 시 예약 가능.', 9, true, NOW(), 'admin'),
(111, 5, 'PA-TURNDOWN', '프리미엄 턴다운 서비스', 'Premium Turndown Service', 'ROOM_AMENITY', 'ALL_NIGHTS', 'KRW', true, 10.00, 27273, 2727, 30000, 1, 'SERVICE', '저녁 턴다운 시 초콜릿, 아로마 디퓨저, 침구 교체 서비스.', 10, true, NOW(), 'admin'),
(112, 5, 'BF-ROOM', '룸서비스 조식 (코스)', 'Room Service Breakfast (Course)', 'BREAKFAST_PAID', 'ALL_NIGHTS', 'KRW', true, 10.00, 50000, 5000, 55000, 1, 'SERVICE', '객실 내 풀코스 조식 서비스. 양식/한식 선택 가능.', 11, true, NOW(), 'admin'),
(113, 5, 'RS-WINE', '와인 딜리버리 서비스', 'Wine Delivery Service', 'ROOM_SERVICE', 'ALL_NIGHTS', 'KRW', true, 10.00, 72727, 7273, 80000, 1, 'EA', '소믈리에 추천 와인 객실 배달 서비스.', 12, true, NOW(), 'admin'),
(114, 5, 'RS-CAKE', '케이크 딜리버리 서비스', 'Cake Delivery Service', 'ROOM_SERVICE', 'FIRST_NIGHT_ONLY', 'KRW', true, 10.00, 40909, 4091, 45000, 1, 'EA', '생일/기념일 케이크 배달. 1일 전 예약 필요.', 13, true, NOW(), 'admin'),
(115, 5, 'SPA-COUPLE', '커플 스파 패키지', 'Couple Spa Package', 'SPA_WELLNESS', 'FIRST_NIGHT_ONLY', 'KRW', true, 10.00, 181818, 18182, 200000, 2, 'SERVICE', '2인 아로마 마사지 90분 + 족욕 + 허브티.', 14, true, NOW(), 'admin'),
(116, 5, 'SPA-AROMA', '아로마 테라피 60분', 'Aroma Therapy 60min', 'SPA_WELLNESS', 'NOT_APPLICABLE', 'KRW', true, 10.00, 109091, 10909, 120000, 1, 'SERVICE', '에센셜 오일 선택 가능 아로마 테라피. 예약 필수.', 15, true, NOW(), 'admin'),
(117, 5, 'LAUNDRY-EX', '특급 세탁 서비스 (당일)', 'Express Laundry Service (Same Day)', 'LAUNDRY', 'NOT_APPLICABLE', 'KRW', true, 10.00, 22727, 2273, 25000, 1, 'EA', '오전 10시 접수 시 당일 오후 6시 완료. 1벌 기준.', 16, true, NOW(), 'admin'),
(118, 5, 'MINIBAR-DX', '디럭스 미니바 패키지', 'Deluxe Minibar Package', 'MINIBAR', 'ALL_NIGHTS', 'KRW', true, 10.00, 136364, 13636, 150000, 1, 'SET', '프리미엄 주류 4종 + 스낵 6종 + 음료 4종. 매일 리필.', 17, true, NOW(), 'admin'),
(119, 5, 'UPGRADE-VW', '뷰 업그레이드 (스탠다드→강남뷰)', 'View Upgrade (Standard→Gangnam View)', 'ROOM_UPGRADE', 'ALL_NIGHTS', 'KRW', true, 10.00, 90909, 9091, 100000, 1, 'SERVICE', '스탠다드뷰에서 강남 스카이라인뷰로 업그레이드.', 18, true, NOW(), 'admin'),
-- OBH (property_id=6)
(120, 6, 'PA-FLOWER', '객실 꽃 장식 서비스', 'Room Flower Decoration Service', 'ROOM_AMENITY', 'FIRST_NIGHT_ONLY', 'KRW', true, 10.00, 45455, 4545, 50000, 1, 'SET', '해운대 특별 이벤트용 꽃 장식. 장미/카네이션 선택 가능.', 9, true, NOW(), 'admin'),
(121, 6, 'PA-TURNDOWN', '프리미엄 턴다운 서비스', 'Premium Turndown Service', 'ROOM_AMENITY', 'ALL_NIGHTS', 'KRW', true, 10.00, 27273, 2727, 30000, 1, 'SERVICE', '저녁 턴다운 시 해양 초콜릿, 아로마 세팅 서비스.', 10, true, NOW(), 'admin'),
(122, 6, 'BF-ROOM', '룸서비스 조식 (해산물 코스)', 'Room Service Breakfast (Seafood Course)', 'BREAKFAST_PAID', 'ALL_NIGHTS', 'KRW', true, 10.00, 54545, 5455, 60000, 1, 'SERVICE', '부산 해산물 활용 프리미엄 조식. 전복죽/해물파전 등.', 11, true, NOW(), 'admin'),
(123, 6, 'RS-WINE', '와인 딜리버리 서비스', 'Wine Delivery Service', 'ROOM_SERVICE', 'ALL_NIGHTS', 'KRW', true, 10.00, 72727, 7273, 80000, 1, 'EA', '소믈리에 추천 와인 객실 배달. 오션뷰와 함께 즐기기.', 12, true, NOW(), 'admin'),
(124, 6, 'RS-CAKE', '케이크 딜리버리 서비스', 'Cake Delivery Service', 'ROOM_SERVICE', 'FIRST_NIGHT_ONLY', 'KRW', true, 10.00, 40909, 4091, 45000, 1, 'EA', '생일/기념일 케이크 배달. 해운대 인기 베이커리 제휴.', 13, true, NOW(), 'admin'),
(125, 6, 'SPA-COUPLE', '오션뷰 커플 스파 패키지', 'Ocean View Couple Spa Package', 'SPA_WELLNESS', 'FIRST_NIGHT_ONLY', 'KRW', true, 10.00, 227273, 22727, 250000, 2, 'SERVICE', '해운대 바다 전망 스파실, 2인 핫스톤 마사지 90분.', 14, true, NOW(), 'admin'),
(126, 6, 'SPA-AROMA', '해양 아로마 테라피 60분', 'Ocean Aroma Therapy 60min', 'SPA_WELLNESS', 'NOT_APPLICABLE', 'KRW', true, 10.00, 109091, 10909, 120000, 1, 'SERVICE', '해양 미네랄 에센셜 오일 사용 아로마 테라피.', 15, true, NOW(), 'admin'),
(127, 6, 'LAUNDRY-EX', '특급 세탁 서비스 (당일)', 'Express Laundry Service (Same Day)', 'LAUNDRY', 'NOT_APPLICABLE', 'KRW', true, 10.00, 22727, 2273, 25000, 1, 'EA', '오전 10시 접수 시 당일 오후 6시 완료. 1벌 기준.', 16, true, NOW(), 'admin'),
(128, 6, 'MINIBAR-DX', '디럭스 미니바 패키지', 'Deluxe Minibar Package', 'MINIBAR', 'ALL_NIGHTS', 'KRW', true, 10.00, 136364, 13636, 150000, 1, 'SET', '부산 수제 맥주 2종 + 프리미엄 주류 3종 + 스낵 풀 패키지.', 17, true, NOW(), 'admin'),
(129, 6, 'UPGRADE-VW', '뷰 업그레이드 (시티뷰→오션프론트)', 'View Upgrade (City→Oceanfront)', 'ROOM_UPGRADE', 'ALL_NIGHTS', 'KRW', true, 10.00, 136364, 13636, 150000, 1, 'SERVICE', '시티뷰에서 해운대 오션프론트뷰로 업그레이드.', 18, true, NOW(), 'admin');

-- ============================================================
-- 3. 시퀀스 리셋
-- ============================================================
SELECT setval('rm_free_service_option_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_free_service_option));
SELECT setval('rm_paid_service_option_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rm_paid_service_option));
