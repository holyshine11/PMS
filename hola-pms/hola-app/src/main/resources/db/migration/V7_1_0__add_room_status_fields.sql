-- V7_1_0: 객실 상태 필드 추가 (프론트데스크/하우스키핑 기반)
-- hk_status: CLEAN, DIRTY, OOO(사용불가), OOS(일시중단)
-- fo_status: VACANT, OCCUPIED

ALTER TABLE htl_room_number ADD COLUMN hk_status VARCHAR(20) DEFAULT 'CLEAN' NOT NULL;
ALTER TABLE htl_room_number ADD COLUMN fo_status VARCHAR(20) DEFAULT 'VACANT' NOT NULL;
ALTER TABLE htl_room_number ADD COLUMN hk_updated_at TIMESTAMP;
ALTER TABLE htl_room_number ADD COLUMN hk_memo VARCHAR(500);

-- 복합 인덱스: 상태별 조회 최적화
CREATE INDEX idx_htl_room_number_status ON htl_room_number(property_id, hk_status, fo_status) WHERE deleted_at IS NULL;
