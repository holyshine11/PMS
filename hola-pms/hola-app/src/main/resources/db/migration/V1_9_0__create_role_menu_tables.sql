-- =====================================================
-- V1_9_0: 권한관리 테이블 생성 (sys_menu, sys_role, sys_role_menu)
-- + sys_admin_user.role_id 컬럼 추가
-- =====================================================

-- 메뉴 마스터
CREATE TABLE sys_menu (
    id          BIGSERIAL PRIMARY KEY,
    menu_code   VARCHAR(50)  NOT NULL,
    menu_name   VARCHAR(100) NOT NULL,
    parent_id   BIGINT       REFERENCES sys_menu(id),
    depth       INTEGER      NOT NULL DEFAULT 1,
    target_type VARCHAR(20)  NOT NULL,
    sort_order  INTEGER      DEFAULT 0,
    use_yn      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    deleted_at  TIMESTAMP,
    UNIQUE (menu_code, target_type)
);

COMMENT ON TABLE  sys_menu IS '메뉴 마스터';
COMMENT ON COLUMN sys_menu.menu_code   IS '메뉴 고유코드';
COMMENT ON COLUMN sys_menu.menu_name   IS '메뉴명';
COMMENT ON COLUMN sys_menu.parent_id   IS '상위메뉴 ID (1depth=NULL)';
COMMENT ON COLUMN sys_menu.depth       IS '메뉴 깊이 (1 또는 2)';
COMMENT ON COLUMN sys_menu.target_type IS '대상 유형 (HOTEL_ADMIN 등)';

-- 권한 마스터
CREATE TABLE sys_role (
    id          BIGSERIAL    PRIMARY KEY,
    role_name   VARCHAR(100) NOT NULL,
    hotel_id    BIGINT,
    target_type VARCHAR(20)  NOT NULL DEFAULT 'HOTEL_ADMIN',
    sort_order  INTEGER      DEFAULT 0,
    use_yn      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    deleted_at  TIMESTAMP,
    UNIQUE (role_name, hotel_id)
);

COMMENT ON TABLE  sys_role IS '권한 마스터';
COMMENT ON COLUMN sys_role.role_name   IS '권한명';
COMMENT ON COLUMN sys_role.hotel_id    IS '소속 호텔 ID';
COMMENT ON COLUMN sys_role.target_type IS '대상 유형 (HOTEL_ADMIN 등)';

-- 권한-메뉴 매핑
CREATE TABLE sys_role_menu (
    id         BIGSERIAL PRIMARY KEY,
    role_id    BIGINT    NOT NULL REFERENCES sys_role(id),
    menu_id    BIGINT    NOT NULL REFERENCES sys_menu(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (role_id, menu_id)
);

COMMENT ON TABLE  sys_role_menu IS '권한-메뉴 매핑';
COMMENT ON COLUMN sys_role_menu.role_id IS '권한 ID';
COMMENT ON COLUMN sys_role_menu.menu_id IS '메뉴 ID';

-- sys_admin_user에 role_id 컬럼 추가
ALTER TABLE sys_admin_user ADD COLUMN role_id BIGINT REFERENCES sys_role(id);

-- =====================================================
-- 시드 데이터: HOTEL_ADMIN 메뉴 15건
-- =====================================================

-- 1depth (5건)
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES
    ('AUTH_MGMT',    '권한관리',     NULL, 1, 'HOTEL_ADMIN', 1),
    ('PRODUCT_MGMT', '상품관리',    NULL, 1, 'HOTEL_ADMIN', 2),
    ('RSV_MGMT',     '예약관리',    NULL, 1, 'HOTEL_ADMIN', 3),
    ('SYS_MGMT',     '시스템 관리', NULL, 1, 'HOTEL_ADMIN', 4),
    ('SETTLE_MGMT',  '정산관리',    NULL, 1, 'HOTEL_ADMIN', 5);

-- 2depth (10건)
INSERT INTO sys_menu (menu_code, menu_name, parent_id, depth, target_type, sort_order)
VALUES
    -- 권한관리 하위
    ('AUTH_PARTNER',    '협력사 권한 관리',     (SELECT id FROM sys_menu WHERE menu_code = 'AUTH_MGMT'    AND target_type = 'HOTEL_ADMIN'), 2, 'HOTEL_ADMIN', 1),
    -- 상품관리 하위
    ('PROD_ROOM',       '객실관리',            (SELECT id FROM sys_menu WHERE menu_code = 'PRODUCT_MGMT' AND target_type = 'HOTEL_ADMIN'), 2, 'HOTEL_ADMIN', 1),
    ('PROD_PARTNER_ROOM','협력사 객실 관리',   (SELECT id FROM sys_menu WHERE menu_code = 'PRODUCT_MGMT' AND target_type = 'HOTEL_ADMIN'), 2, 'HOTEL_ADMIN', 2),
    ('PROD_OPTION',     '옵션관리',            (SELECT id FROM sys_menu WHERE menu_code = 'PRODUCT_MGMT' AND target_type = 'HOTEL_ADMIN'), 2, 'HOTEL_ADMIN', 3),
    ('PROD_RATE',       '레이트 관리',         (SELECT id FROM sys_menu WHERE menu_code = 'PRODUCT_MGMT' AND target_type = 'HOTEL_ADMIN'), 2, 'HOTEL_ADMIN', 4),
    ('PROD_PROMO',      '프로모션 상품 관리',  (SELECT id FROM sys_menu WHERE menu_code = 'PRODUCT_MGMT' AND target_type = 'HOTEL_ADMIN'), 2, 'HOTEL_ADMIN', 5),
    -- 예약관리 하위
    ('RSV_LIST',        '예약내역 관리',       (SELECT id FROM sys_menu WHERE menu_code = 'RSV_MGMT'     AND target_type = 'HOTEL_ADMIN'), 2, 'HOTEL_ADMIN', 1),
    ('RSV_CANCEL',      '취소내역 관리',       (SELECT id FROM sys_menu WHERE menu_code = 'RSV_MGMT'     AND target_type = 'HOTEL_ADMIN'), 2, 'HOTEL_ADMIN', 2),
    -- 시스템 관리 하위
    ('SYS_MARKET_CODE', '마켓코드 관리',       (SELECT id FROM sys_menu WHERE menu_code = 'SYS_MGMT'     AND target_type = 'HOTEL_ADMIN'), 2, 'HOTEL_ADMIN', 1),
    -- 정산관리 하위
    ('SETTLE_LIST',     '정산 내역 관리',      (SELECT id FROM sys_menu WHERE menu_code = 'SETTLE_MGMT'  AND target_type = 'HOTEL_ADMIN'), 2, 'HOTEL_ADMIN', 1);
