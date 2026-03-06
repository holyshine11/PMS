-- sys_role에 property_id 컬럼 추가 (프로퍼티 관리자 권한용)
ALTER TABLE sys_role ADD COLUMN property_id BIGINT;

-- 기존 unique constraint 삭제 후 property_id 포함하여 재생성
ALTER TABLE sys_role DROP CONSTRAINT IF EXISTS sys_role_role_name_hotel_id_key;
ALTER TABLE sys_role ADD CONSTRAINT uk_sys_role_name_hotel_property
    UNIQUE (role_name, hotel_id, property_id);

COMMENT ON COLUMN sys_role.property_id IS '소속 프로퍼티 ID (PROPERTY_ADMIN 전용)';
