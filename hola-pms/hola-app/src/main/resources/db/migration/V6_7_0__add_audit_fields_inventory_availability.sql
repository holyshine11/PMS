-- 재고 가용성 테이블에 감사 필드 추가
ALTER TABLE rm_inventory_availability
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN created_by VARCHAR(50),
    ADD COLUMN updated_by VARCHAR(50);

COMMENT ON COLUMN rm_inventory_availability.created_at IS '생성일시';
COMMENT ON COLUMN rm_inventory_availability.updated_at IS '수정일시';
COMMENT ON COLUMN rm_inventory_availability.created_by IS '생성자';
COMMENT ON COLUMN rm_inventory_availability.updated_by IS '수정자';
