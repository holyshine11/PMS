-- =============================================
-- V8.4.0: 하우스키핑 메뉴 데이터 (권한관리 메뉴 트리용)
-- =============================================
-- 기존 메뉴 구조: depth 1(대분류) → depth 2(중분류) → depth 3(소분류)
-- target_type별로 별도 트리 (HOTEL_ADMIN, PROPERTY_ADMIN)

-- ===== HOTEL_ADMIN용 하우스키핑 메뉴 =====

-- 대분류: 하우스키핑 (객실 청소/관리)
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
VALUES ('HK_MGMT', '하우스키핑', NULL, 1, 'HOTEL_ADMIN', 5, true, NOW(), NOW());

-- 중분류: 하우스키핑 관리
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
SELECT 'HK_OPS', '하우스키핑 관리', id, 2, 'HOTEL_ADMIN', 1, true, NOW(), NOW()
FROM sys_menu WHERE menu_code = 'HK_MGMT' AND target_type = 'HOTEL_ADMIN' AND depth = 1;

-- 소분류: 대시보드 (오늘 청소 현황 요약)
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
SELECT 'HK_DASH', '대시보드', id, 3, 'HOTEL_ADMIN', 1, true, NOW(), NOW()
FROM sys_menu WHERE menu_code = 'HK_OPS' AND target_type = 'HOTEL_ADMIN' AND depth = 2;

-- 소분류: 작업 관리 (청소 작업 생성/배정/상태관리)
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
SELECT 'HK_TASK', '작업 관리', id, 3, 'HOTEL_ADMIN', 2, true, NOW(), NOW()
FROM sys_menu WHERE menu_code = 'HK_OPS' AND target_type = 'HOTEL_ADMIN' AND depth = 2;

-- 소분류: 작업 보드 (칸반 보드 - 대기/진행/완료/검수 한눈에 보기)
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
SELECT 'HK_BOARD', '작업 보드', id, 3, 'HOTEL_ADMIN', 3, true, NOW(), NOW()
FROM sys_menu WHERE menu_code = 'HK_OPS' AND target_type = 'HOTEL_ADMIN' AND depth = 2;

-- 소분류: 이력 조회 (기간별 청소 이력 및 통계)
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
SELECT 'HK_HIST', '이력 조회', id, 3, 'HOTEL_ADMIN', 4, true, NOW(), NOW()
FROM sys_menu WHERE menu_code = 'HK_OPS' AND target_type = 'HOTEL_ADMIN' AND depth = 2;

-- ===== PROPERTY_ADMIN용 하우스키핑 메뉴 (동일 구조) =====

INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
VALUES ('HK_MGMT', '하우스키핑', NULL, 1, 'PROPERTY_ADMIN', 5, true, NOW(), NOW());

INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
SELECT 'HK_OPS', '하우스키핑 관리', id, 2, 'PROPERTY_ADMIN', 1, true, NOW(), NOW()
FROM sys_menu WHERE menu_code = 'HK_MGMT' AND target_type = 'PROPERTY_ADMIN' AND depth = 1;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
SELECT 'HK_DASH', '대시보드', id, 3, 'PROPERTY_ADMIN', 1, true, NOW(), NOW()
FROM sys_menu WHERE menu_code = 'HK_OPS' AND target_type = 'PROPERTY_ADMIN' AND depth = 2;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
SELECT 'HK_TASK', '작업 관리', id, 3, 'PROPERTY_ADMIN', 2, true, NOW(), NOW()
FROM sys_menu WHERE menu_code = 'HK_OPS' AND target_type = 'PROPERTY_ADMIN' AND depth = 2;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
SELECT 'HK_BOARD', '작업 보드', id, 3, 'PROPERTY_ADMIN', 3, true, NOW(), NOW()
FROM sys_menu WHERE menu_code = 'HK_OPS' AND target_type = 'PROPERTY_ADMIN' AND depth = 2;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order, use_yn, created_at, updated_at)
SELECT 'HK_HIST', '이력 조회', id, 3, 'PROPERTY_ADMIN', 4, true, NOW(), NOW()
FROM sys_menu WHERE menu_code = 'HK_OPS' AND target_type = 'PROPERTY_ADMIN' AND depth = 2;
