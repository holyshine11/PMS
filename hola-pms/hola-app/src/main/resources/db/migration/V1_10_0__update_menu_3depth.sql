-- =====================================================
-- V1_10_0: sys_menu 3-depth 구조로 전환
-- 기존 2-depth 메뉴 삭제 → 3-depth 메뉴 삽입
-- =====================================================

-- 기존 매핑 삭제
DELETE FROM sys_role_menu;

-- 기존 메뉴 삭제 (자식 먼저)
DELETE FROM sys_menu WHERE depth = 2;
DELETE FROM sys_menu WHERE depth = 1;

-- =====================================================
-- 새 3-depth 메뉴 구조 (권한 매트릭스 기준)
-- =====================================================

-- ===== 1depth: 객실 관리 =====
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('ROOM_MGMT', '객실 관리', NULL, 1, 'HOTEL_ADMIN', 1);

-- 2depth: 객실 관리 > 객실 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('ROOM_ROOM', '객실 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'ROOM_MGMT' AND target_type = 'HOTEL_ADMIN'),
    2, 'HOTEL_ADMIN', 1);

-- 3depth: 객실 그룹 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('ROOM_GROUP', '객실 그룹 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'ROOM_ROOM' AND target_type = 'HOTEL_ADMIN'),
    3, 'HOTEL_ADMIN', 1);

-- 3depth: 객실 타입 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('ROOM_TYPE', '객실 타입 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'ROOM_ROOM' AND target_type = 'HOTEL_ADMIN'),
    3, 'HOTEL_ADMIN', 2);

-- 2depth: 객실 관리 > 서비스 옵션관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('SVC_OPTION', '서비스 옵션관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'ROOM_MGMT' AND target_type = 'HOTEL_ADMIN'),
    2, 'HOTEL_ADMIN', 2);

-- 3depth: 무료 옵션 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('SVC_FREE', '무료 옵션 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'SVC_OPTION' AND target_type = 'HOTEL_ADMIN'),
    3, 'HOTEL_ADMIN', 1);

-- 3depth: 유료 옵션 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('SVC_PAID', '유료 옵션 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'SVC_OPTION' AND target_type = 'HOTEL_ADMIN'),
    3, 'HOTEL_ADMIN', 2);

-- 2depth: 객실 관리 > 레이트 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('RATE_MGMT', '레이트 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'ROOM_MGMT' AND target_type = 'HOTEL_ADMIN'),
    2, 'HOTEL_ADMIN', 3);

-- 3depth: 레이트 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('RATE_LIST', '레이트 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'RATE_MGMT' AND target_type = 'HOTEL_ADMIN'),
    3, 'HOTEL_ADMIN', 1);

-- 2depth: 객실 관리 > 프로모션관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('PROMO_MGMT', '프로모션관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'ROOM_MGMT' AND target_type = 'HOTEL_ADMIN'),
    2, 'HOTEL_ADMIN', 4);

-- 3depth: 프로모션 코드 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('PROMO_CODE', '프로모션 코드 관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'PROMO_MGMT' AND target_type = 'HOTEL_ADMIN'),
    3, 'HOTEL_ADMIN', 1);

-- ===== 1depth: 예약관리 =====
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('RSV_MGMT2', '예약관리', NULL, 1, 'HOTEL_ADMIN', 2);

-- 2depth: 예약관리 > 예약관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES ('RSV_LIST2', '예약관리',
    (SELECT id FROM sys_menu WHERE menu_code = 'RSV_MGMT2' AND target_type = 'HOTEL_ADMIN'),
    2, 'HOTEL_ADMIN', 1);

-- depth 컬럼 주석 업데이트
COMMENT ON COLUMN sys_menu.depth IS '메뉴 깊이 (1, 2, 또는 3)';
