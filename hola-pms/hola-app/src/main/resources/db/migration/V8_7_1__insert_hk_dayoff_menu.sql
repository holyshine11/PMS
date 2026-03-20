-- 하우스키핑 휴무일 관리 메뉴 추가
-- HOTEL_ADMIN: 근태관리 다음 (sort_order=6)
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
SELECT 'HK_DAYOFF', '휴무일 관리', id, 3, 'HOTEL_ADMIN', 6, true, NOW(), NOW()
FROM sys_menu WHERE menu_code = 'HK_OPS' AND target_type = 'HOTEL_ADMIN' AND depth = 2;

-- PROPERTY_ADMIN
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
SELECT 'HK_DAYOFF', '휴무일 관리', id, 3, 'PROPERTY_ADMIN', 6, true, NOW(), NOW()
FROM sys_menu WHERE menu_code = 'HK_OPS' AND target_type = 'PROPERTY_ADMIN' AND depth = 2;
