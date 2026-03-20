-- =============================================
-- V8.3.0: 하우스키핑 테스트 데이터
-- =============================================

-- 0. sys_admin_user.role 컬럼 확장 (HOUSEKEEPING_SUPERVISOR = 25자)
ALTER TABLE sys_admin_user ALTER COLUMN role TYPE VARCHAR(30);

-- 1. HK 설정 (첫 번째 프로퍼티 기준)
INSERT INTO hk_config (property_id, inspection_required, auto_create_checkout, auto_create_stayover,
                       default_checkout_credit, default_stayover_credit, default_turndown_credit,
                       default_deep_clean_credit, rush_threshold_minutes,
                       created_by, updated_by)
SELECT p.id, false, true, false, 1.0, 0.5, 0.3, 2.0, 120, 'SYSTEM', 'SYSTEM'
FROM htl_property p
WHERE p.property_code = 'GMP'
  AND p.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM hk_config c WHERE c.property_id = p.id)
LIMIT 1;

-- 2. 하우스키퍼 테스트 계정
INSERT INTO sys_admin_user (login_id, password, user_name, email, phone, role, account_type,
                            hotel_id, department, position, created_by, updated_by)
SELECT 'hk_supervisor1', '$2a$10$lEKY9wNGwFk.VJrMSfXvne09W4RfTbAqFB0IOJ2kkt.lS.y9eAksK', '김미영',
       'miyoung@hola.com', '010-1111-2222', 'HOUSEKEEPING_SUPERVISOR', 'HOTEL_ADMIN',
       h.id, '하우스키핑', '매니저', 'SYSTEM', 'SYSTEM'
FROM htl_hotel h WHERE h.hotel_code = 'HTL00001' AND h.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM sys_admin_user u WHERE u.login_id = 'hk_supervisor1');

INSERT INTO sys_admin_user (login_id, password, user_name, email, phone, role, account_type,
                            hotel_id, department, position, created_by, updated_by)
SELECT 'hk_staff1', '$2a$10$lEKY9wNGwFk.VJrMSfXvne09W4RfTbAqFB0IOJ2kkt.lS.y9eAksK', '박수진',
       'sujin@hola.com', '010-3333-4444', 'HOUSEKEEPER', 'HOTEL_ADMIN',
       h.id, '하우스키핑', '담당', 'SYSTEM', 'SYSTEM'
FROM htl_hotel h WHERE h.hotel_code = 'HTL00001' AND h.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM sys_admin_user u WHERE u.login_id = 'hk_staff1');

INSERT INTO sys_admin_user (login_id, password, user_name, email, phone, role, account_type,
                            hotel_id, department, position, created_by, updated_by)
SELECT 'hk_staff2', '$2a$10$lEKY9wNGwFk.VJrMSfXvne09W4RfTbAqFB0IOJ2kkt.lS.y9eAksK', '이정은',
       'jungeun@hola.com', '010-5555-6666', 'HOUSEKEEPER', 'HOTEL_ADMIN',
       h.id, '하우스키핑', '담당', 'SYSTEM', 'SYSTEM'
FROM htl_hotel h WHERE h.hotel_code = 'HTL00001' AND h.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM sys_admin_user u WHERE u.login_id = 'hk_staff2');

-- 3. 하우스키퍼 프로퍼티 매핑
INSERT INTO sys_admin_user_property (admin_user_id, property_id)
SELECT au.id, p.id
FROM sys_admin_user au
CROSS JOIN (SELECT id FROM htl_property WHERE property_code = 'GMP' AND deleted_at IS NULL LIMIT 1) p
WHERE au.login_id IN ('hk_supervisor1', 'hk_staff1', 'hk_staff2')
  AND NOT EXISTS (
    SELECT 1 FROM sys_admin_user_property aup
    WHERE aup.admin_user_id = au.id AND aup.property_id = p.id
  );
