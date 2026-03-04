-- =============================================
-- V1.0.0: 시스템 기본 테이블 생성
-- 관리자, 그룹코드, 공통코드
-- =============================================

-- 관리자 사용자 테이블
CREATE TABLE sys_admin_user (
    id              BIGSERIAL PRIMARY KEY,
    login_id        VARCHAR(50)  NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    user_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(200),
    phone           VARCHAR(20),
    role            VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    login_fail_count INTEGER     DEFAULT 0,
    account_locked  BOOLEAN      DEFAULT FALSE,
    sort_order      INTEGER      DEFAULT 0,
    use_yn          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP
);

-- 그룹 코드 테이블
CREATE TABLE sys_group_code (
    id              BIGSERIAL PRIMARY KEY,
    group_code      VARCHAR(50)  NOT NULL UNIQUE,
    group_name      VARCHAR(200) NOT NULL,
    description     TEXT,
    sort_order      INTEGER      DEFAULT 0,
    use_yn          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP
);

-- 공통 코드 테이블
CREATE TABLE sys_common_code (
    id              BIGSERIAL PRIMARY KEY,
    group_code_id   BIGINT       NOT NULL REFERENCES sys_group_code(id),
    code            VARCHAR(50)  NOT NULL,
    code_name       VARCHAR(200) NOT NULL,
    code_value      VARCHAR(500),
    description     TEXT,
    sort_order      INTEGER      DEFAULT 0,
    use_yn          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    UNIQUE (group_code_id, code)
);

-- 인덱스
CREATE INDEX idx_sys_common_code_group ON sys_common_code(group_code_id);
CREATE INDEX idx_sys_admin_user_login ON sys_admin_user(login_id);

-- =============================================
-- 초기 데이터
-- =============================================

-- 기본 관리자 (admin / holapms1!)
-- BCrypt hash of 'holapms1!' (Spring Security BCryptPasswordEncoder로 생성)
INSERT INTO sys_admin_user (login_id, password, user_name, email, role, created_by)
VALUES ('admin', '$2a$10$lEKY9wNGwFk.VJrMSfXvne09W4RfTbAqFB0IOJ2kkt.lS.y9eAksK', '시스템관리자', 'admin@holapms.com', 'SUPER_ADMIN', 'SYSTEM');

-- 그룹 코드
INSERT INTO sys_group_code (group_code, group_name, description, created_by) VALUES
('HOTEL_TYPE', '호텔유형', '호텔 분류 유형', 'SYSTEM'),
('PROPERTY_TYPE', '프로퍼티구분', '프로퍼티 구분 유형', 'SYSTEM'),
('STAR_RATING', '호텔등급', '호텔 성급', 'SYSTEM'),
('ROOM_STATUS', '객실상태', '객실 상태 코드', 'SYSTEM'),
('RESERVATION_STATUS', '예약상태', '예약 상태 코드', 'SYSTEM');

-- 공통 코드: 호텔유형
INSERT INTO sys_common_code (group_code_id, code, code_name, sort_order, created_by)
SELECT id, 'HOTEL', '호텔', 1, 'SYSTEM' FROM sys_group_code WHERE group_code = 'HOTEL_TYPE'
UNION ALL
SELECT id, 'RESORT', '리조트', 2, 'SYSTEM' FROM sys_group_code WHERE group_code = 'HOTEL_TYPE'
UNION ALL
SELECT id, 'BOUTIQUE', '부티크 호텔', 3, 'SYSTEM' FROM sys_group_code WHERE group_code = 'HOTEL_TYPE'
UNION ALL
SELECT id, 'BUSINESS', '비즈니스 호텔', 4, 'SYSTEM' FROM sys_group_code WHERE group_code = 'HOTEL_TYPE';

-- 공통 코드: 프로퍼티구분
INSERT INTO sys_common_code (group_code_id, code, code_name, sort_order, created_by)
SELECT id, 'CONSOLE', '콘솔', 1, 'SYSTEM' FROM sys_group_code WHERE group_code = 'PROPERTY_TYPE'
UNION ALL
SELECT id, 'HOTEL', '호텔', 2, 'SYSTEM' FROM sys_group_code WHERE group_code = 'PROPERTY_TYPE';

-- 공통 코드: 호텔등급
INSERT INTO sys_common_code (group_code_id, code, code_name, sort_order, created_by)
SELECT id, '5STAR', '5성급', 1, 'SYSTEM' FROM sys_group_code WHERE group_code = 'STAR_RATING'
UNION ALL
SELECT id, '4STAR', '4성급', 2, 'SYSTEM' FROM sys_group_code WHERE group_code = 'STAR_RATING'
UNION ALL
SELECT id, '3STAR', '3성급', 3, 'SYSTEM' FROM sys_group_code WHERE group_code = 'STAR_RATING';
