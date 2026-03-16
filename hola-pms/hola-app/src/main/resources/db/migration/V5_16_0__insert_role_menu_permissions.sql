-- =====================================================
-- V5_16_0: 전체 테스트 계정에 역할-메뉴 권한 매핑
-- 1. 프로퍼티별 역할 추가 (GMS, OBH)
-- 2. 프로퍼티 관리자에 role_id 할당
-- 3. 모든 역할에 전체 메뉴 매핑
-- =====================================================

-- =====================================================
-- 1. 누락된 프로퍼티 역할 추가
--    기존: GMP 프로퍼티 매니저만 존재
--    추가: GMS, OBH 프로퍼티 매니저
-- =====================================================
INSERT INTO sys_role (role_name, hotel_id, target_type, property_id, sort_order, created_at, created_by)
VALUES
    ('프로퍼티 매니저',
     (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001'),
     'PROPERTY_ADMIN',
     (SELECT id FROM htl_property WHERE property_code = 'GMS' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')),
     2, NOW(), 'admin'),
    ('프로퍼티 매니저',
     (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002'),
     'PROPERTY_ADMIN',
     (SELECT id FROM htl_property WHERE property_code = 'OBH' AND hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')),
     1, NOW(), 'admin');

-- =====================================================
-- 2. 프로퍼티 관리자에 role_id 할당
-- =====================================================

-- prop1admin → GMP 프로퍼티 매니저
UPDATE sys_admin_user
SET role_id = (
    SELECT r.id FROM sys_role r
    JOIN htl_property p ON r.property_id = p.id
    WHERE r.role_name = '프로퍼티 매니저'
      AND r.target_type = 'PROPERTY_ADMIN'
      AND p.property_code = 'GMP'
),
role_name = '프로퍼티 매니저'
WHERE login_id = 'prop1admin';

-- prop2admin → GMS 프로퍼티 매니저
UPDATE sys_admin_user
SET role_id = (
    SELECT r.id FROM sys_role r
    JOIN htl_property p ON r.property_id = p.id
    WHERE r.role_name = '프로퍼티 매니저'
      AND r.target_type = 'PROPERTY_ADMIN'
      AND p.property_code = 'GMS'
),
role_name = '프로퍼티 매니저'
WHERE login_id = 'prop2admin';

-- prop3admin → OBH 프로퍼티 매니저
UPDATE sys_admin_user
SET role_id = (
    SELECT r.id FROM sys_role r
    JOIN htl_property p ON r.property_id = p.id
    WHERE r.role_name = '프로퍼티 매니저'
      AND r.target_type = 'PROPERTY_ADMIN'
      AND p.property_code = 'OBH'
),
role_name = '프로퍼티 매니저'
WHERE login_id = 'prop3admin';

-- hotel1admin, hotel2admin에도 role_name 할당
UPDATE sys_admin_user SET role_name = '총괄 매니저' WHERE login_id = 'hotel1admin';
UPDATE sys_admin_user SET role_name = '프론트 매니저' WHERE login_id = 'hotel2admin';

-- =====================================================
-- 3. 역할-메뉴 매핑 (모든 역할에 전체 메뉴 할당)
-- =====================================================

-- 기존 매핑 정리
DELETE FROM sys_role_menu;

-- 총괄 매니저 (HTL00001, HOTEL_ADMIN) → 모든 HOTEL_ADMIN 메뉴
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT
    (SELECT r.id FROM sys_role r WHERE r.role_name = '총괄 매니저' AND r.hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00001')),
    m.id,
    NOW()
FROM sys_menu m
WHERE m.target_type = 'HOTEL_ADMIN'
  AND m.deleted_at IS NULL;

-- 프론트 매니저 (HTL00002, HOTEL_ADMIN) → 모든 HOTEL_ADMIN 메뉴
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT
    (SELECT r.id FROM sys_role r WHERE r.role_name = '프론트 매니저' AND r.hotel_id = (SELECT id FROM htl_hotel WHERE hotel_code = 'HTL00002')),
    m.id,
    NOW()
FROM sys_menu m
WHERE m.target_type = 'HOTEL_ADMIN'
  AND m.deleted_at IS NULL;

-- GMP 프로퍼티 매니저 → 모든 PROPERTY_ADMIN 메뉴
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT
    (SELECT r.id FROM sys_role r
     JOIN htl_property p ON r.property_id = p.id
     WHERE r.role_name = '프로퍼티 매니저' AND r.target_type = 'PROPERTY_ADMIN' AND p.property_code = 'GMP'),
    m.id,
    NOW()
FROM sys_menu m
WHERE m.target_type = 'PROPERTY_ADMIN'
  AND m.deleted_at IS NULL;

-- GMS 프로퍼티 매니저 → 모든 PROPERTY_ADMIN 메뉴
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT
    (SELECT r.id FROM sys_role r
     JOIN htl_property p ON r.property_id = p.id
     WHERE r.role_name = '프로퍼티 매니저' AND r.target_type = 'PROPERTY_ADMIN' AND p.property_code = 'GMS'),
    m.id,
    NOW()
FROM sys_menu m
WHERE m.target_type = 'PROPERTY_ADMIN'
  AND m.deleted_at IS NULL;

-- OBH 프로퍼티 매니저 → 모든 PROPERTY_ADMIN 메뉴
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT
    (SELECT r.id FROM sys_role r
     JOIN htl_property p ON r.property_id = p.id
     WHERE r.role_name = '프로퍼티 매니저' AND r.target_type = 'PROPERTY_ADMIN' AND p.property_code = 'OBH'),
    m.id,
    NOW()
FROM sys_menu m
WHERE m.target_type = 'PROPERTY_ADMIN'
  AND m.deleted_at IS NULL;

-- =====================================================
-- 4. 시퀀스 리셋
-- =====================================================
SELECT setval('sys_role_id_seq', (SELECT COALESCE(MAX(id), 0) FROM sys_role));
SELECT setval('sys_role_menu_id_seq', (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu));
