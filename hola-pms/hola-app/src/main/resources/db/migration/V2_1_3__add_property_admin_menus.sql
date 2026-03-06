-- =====================================================
-- V2_1_3: PROPERTY_ADMIN 메뉴 데이터 추가
-- HOTEL_ADMIN 메뉴와 동일한 3-depth 구조
-- =====================================================

-- ===== 1depth: 객실 관리 =====
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('ROOM_MGMT', '객실 관리', NULL, 1, 'PROPERTY_ADMIN', 1);

-- 2depth: 객실 관리 > 객실 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('ROOM_ROOM', '객실 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'ROOM_MGMT' AND target_type = 'PROPERTY_ADMIN'),
    2, 'PROPERTY_ADMIN', 1);

-- 3depth: 객실 그룹 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('ROOM_GROUP', '객실 그룹 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'ROOM_ROOM' AND target_type = 'PROPERTY_ADMIN'),
    3, 'PROPERTY_ADMIN', 1);

-- 3depth: 객실 타입 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('ROOM_TYPE', '객실 타입 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'ROOM_ROOM' AND target_type = 'PROPERTY_ADMIN'),
    3, 'PROPERTY_ADMIN', 2);

-- 2depth: 객실 관리 > 서비스 옵션관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('SVC_OPTION', '서비스 옵션관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'ROOM_MGMT' AND target_type = 'PROPERTY_ADMIN'),
    2, 'PROPERTY_ADMIN', 2);

-- 3depth: 무료 옵션 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('SVC_FREE', '무료 옵션 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'SVC_OPTION' AND target_type = 'PROPERTY_ADMIN'),
    3, 'PROPERTY_ADMIN', 1);

-- 3depth: 유료 옵션 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('SVC_PAID', '유료 옵션 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'SVC_OPTION' AND target_type = 'PROPERTY_ADMIN'),
    3, 'PROPERTY_ADMIN', 2);

-- 2depth: 객실 관리 > 레이트 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('RATE_MGMT', '레이트 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'ROOM_MGMT' AND target_type = 'PROPERTY_ADMIN'),
    2, 'PROPERTY_ADMIN', 3);

-- 3depth: 레이트 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('RATE_LIST', '레이트 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'RATE_MGMT' AND target_type = 'PROPERTY_ADMIN'),
    3, 'PROPERTY_ADMIN', 1);

-- 2depth: 객실 관리 > 프로모션관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('PROMO_MGMT', '프로모션관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'ROOM_MGMT' AND target_type = 'PROPERTY_ADMIN'),
    2, 'PROPERTY_ADMIN', 4);

-- 3depth: 프로모션 코드 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('PROMO_CODE', '프로모션 코드 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'PROMO_MGMT' AND target_type = 'PROPERTY_ADMIN'),
    3, 'PROPERTY_ADMIN', 1);

-- ===== 1depth: 예약관리 =====
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('RSV_MGMT2', '예약관리', NULL, 1, 'PROPERTY_ADMIN', 2);

-- 2depth: 예약관리 > 예약관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('RSV_LIST2', '예약관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'RSV_MGMT2' AND target_type = 'PROPERTY_ADMIN'),
    2, 'PROPERTY_ADMIN', 1);
