-- =============================================
-- V1.8.0: 관리자 사용자 확장 (호텔 관리자 관리)
-- sys_admin_user 확장 컬럼 + sys_admin_user_property 테이블
-- =============================================

-- 회원번호 시퀀스
CREATE SEQUENCE IF NOT EXISTS sys_member_number_seq START WITH 1 INCREMENT BY 1;

-- sys_admin_user 확장 컬럼
ALTER TABLE sys_admin_user ADD COLUMN IF NOT EXISTS member_number VARCHAR(20) UNIQUE;
ALTER TABLE sys_admin_user ADD COLUMN IF NOT EXISTS account_type VARCHAR(20) DEFAULT 'BLUEWAVE_ADMIN';
ALTER TABLE sys_admin_user ADD COLUMN IF NOT EXISTS hotel_id BIGINT;
ALTER TABLE sys_admin_user ADD COLUMN IF NOT EXISTS mobile_country_code VARCHAR(10);
ALTER TABLE sys_admin_user ADD COLUMN IF NOT EXISTS mobile VARCHAR(20);
ALTER TABLE sys_admin_user ADD COLUMN IF NOT EXISTS phone_country_code VARCHAR(10);
ALTER TABLE sys_admin_user ADD COLUMN IF NOT EXISTS department VARCHAR(100);
ALTER TABLE sys_admin_user ADD COLUMN IF NOT EXISTS position VARCHAR(100);
ALTER TABLE sys_admin_user ADD COLUMN IF NOT EXISTS role_name VARCHAR(100);

-- 기존 관리자 회원번호 부여
UPDATE sys_admin_user
SET member_number = 'U' || LPAD(nextval('sys_member_number_seq')::TEXT, 9, '0')
WHERE member_number IS NULL;

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_sys_admin_user_hotel ON sys_admin_user(hotel_id);
CREATE INDEX IF NOT EXISTS idx_sys_admin_user_account_type ON sys_admin_user(account_type);
CREATE INDEX IF NOT EXISTS idx_sys_admin_user_member_number ON sys_admin_user(member_number);

-- 관리자-프로퍼티 매핑 테이블
CREATE TABLE sys_admin_user_property (
    id              BIGSERIAL PRIMARY KEY,
    admin_user_id   BIGINT NOT NULL REFERENCES sys_admin_user(id),
    property_id     BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (admin_user_id, property_id)
);

CREATE INDEX idx_sys_admin_user_property_admin ON sys_admin_user_property(admin_user_id);
CREATE INDEX idx_sys_admin_user_property_property ON sys_admin_user_property(property_id);
