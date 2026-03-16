-- =====================================================
-- V5_17_0: 양쪽 호텔에 동일한 HOTEL_ADMIN 역할 추가
-- 기존: HTL00001에 총괄 매니저만, HTL00002에 프론트 매니저만
-- 추가: 양쪽 호텔 모두 총괄 매니저/프론트 매니저/객실 매니저 보유
-- =====================================================

-- HTL00001에 프론트 매니저 추가
INSERT INTO sys_role (role_name, hotel_id, target_type, property_id, sort_order, created_at, created_by)
SELECT '프론트 매니저', h.id, 'HOTEL_ADMIN', NULL, 2, NOW(), 'admin'
FROM htl_hotel h WHERE h.hotel_code = 'HTL00001'
AND NOT EXISTS (SELECT 1 FROM sys_role r WHERE r.role_name = '프론트 매니저' AND r.hotel_id = h.id AND r.target_type = 'HOTEL_ADMIN' AND r.deleted_at IS NULL);

-- HTL00001에 객실 매니저 추가
INSERT INTO sys_role (role_name, hotel_id, target_type, property_id, sort_order, created_at, created_by)
SELECT '객실 매니저', h.id, 'HOTEL_ADMIN', NULL, 3, NOW(), 'admin'
FROM htl_hotel h WHERE h.hotel_code = 'HTL00001'
AND NOT EXISTS (SELECT 1 FROM sys_role r WHERE r.role_name = '객실 매니저' AND r.hotel_id = h.id AND r.target_type = 'HOTEL_ADMIN' AND r.deleted_at IS NULL);

-- HTL00002에 총괄 매니저 추가
INSERT INTO sys_role (role_name, hotel_id, target_type, property_id, sort_order, created_at, created_by)
SELECT '총괄 매니저', h.id, 'HOTEL_ADMIN', NULL, 1, NOW(), 'admin'
FROM htl_hotel h WHERE h.hotel_code = 'HTL00002'
AND NOT EXISTS (SELECT 1 FROM sys_role r WHERE r.role_name = '총괄 매니저' AND r.hotel_id = h.id AND r.target_type = 'HOTEL_ADMIN' AND r.deleted_at IS NULL);

-- HTL00002에 객실 매니저 추가
INSERT INTO sys_role (role_name, hotel_id, target_type, property_id, sort_order, created_at, created_by)
SELECT '객실 매니저', h.id, 'HOTEL_ADMIN', NULL, 3, NOW(), 'admin'
FROM htl_hotel h WHERE h.hotel_code = 'HTL00002'
AND NOT EXISTS (SELECT 1 FROM sys_role r WHERE r.role_name = '객실 매니저' AND r.hotel_id = h.id AND r.target_type = 'HOTEL_ADMIN' AND r.deleted_at IS NULL);

-- 새 역할에 모든 HOTEL_ADMIN 메뉴 매핑 (기존 매핑이 없는 역할만)
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT r.id, m.id, NOW()
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.target_type = 'HOTEL_ADMIN'
  AND m.target_type = 'HOTEL_ADMIN'
  AND m.deleted_at IS NULL
  AND r.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);

-- 시퀀스 리셋
SELECT setval('sys_role_id_seq', (SELECT COALESCE(MAX(id), 0) FROM sys_role));
SELECT setval('sys_role_menu_id_seq', (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu));
