-- =============================================
-- V8.6.1: 하우스키핑 근태관리 메뉴 추가
-- =============================================

-- HOTEL_ADMIN: 근태관리 (이력 조회 다음, sort_order=5)
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
SELECT 'HK_ATT', '근태관리', id, 3, 'HOTEL_ADMIN', 5, true, NOW(), NOW()
FROM sys_menu WHERE menu_code = 'HK_OPS' AND target_type = 'HOTEL_ADMIN' AND depth = 2;

-- PROPERTY_ADMIN: 근태관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
SELECT 'HK_ATT', '근태관리', id, 3, 'PROPERTY_ADMIN', 5, true, NOW(), NOW()
FROM sys_menu WHERE menu_code = 'HK_OPS' AND target_type = 'PROPERTY_ADMIN' AND depth = 2;
