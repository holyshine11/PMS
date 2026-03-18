-- OOO/OOS 객실 관리 테이블
CREATE TABLE htl_room_unavailable (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL REFERENCES htl_property(id),
    room_number_id  BIGINT NOT NULL REFERENCES htl_room_number(id),
    unavailable_type VARCHAR(10) NOT NULL,        -- OOO, OOS
    reason_code     VARCHAR(20),                   -- MAINTENANCE, RENOVATION, SHOWROOM 등
    reason_detail   VARCHAR(500),
    from_date       DATE NOT NULL,
    through_date    DATE NOT NULL,
    return_status   VARCHAR(20) DEFAULT 'DIRTY',   -- OOO/OOS 해제 시 복귀할 HK 상태
    -- audit
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    use_yn          BOOLEAN DEFAULT TRUE,
    sort_order      INTEGER DEFAULT 0
);

CREATE INDEX idx_room_unavail_property ON htl_room_unavailable(property_id, unavailable_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_room_unavail_room ON htl_room_unavailable(room_number_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_room_unavail_dates ON htl_room_unavailable(from_date, through_date) WHERE deleted_at IS NULL;

COMMENT ON TABLE htl_room_unavailable IS 'OOO(Out of Order)/OOS(Out of Service) 객실 관리';
COMMENT ON COLUMN htl_room_unavailable.unavailable_type IS 'OOO: 사용불가(재고차감), OOS: 일시중단(재고미차감)';
COMMENT ON COLUMN htl_room_unavailable.return_status IS 'OOO/OOS 해제 시 돌아갈 HK 상태 (CLEAN, DIRTY)';
