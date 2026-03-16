-- 객실 업그레이드 이력
CREATE TABLE rsv_room_upgrade_history (
    id                      BIGSERIAL       PRIMARY KEY,
    sub_reservation_id      BIGINT          NOT NULL REFERENCES rsv_sub_reservation(id),
    from_room_type_id       BIGINT          NOT NULL REFERENCES rm_room_type(id),
    to_room_type_id         BIGINT          NOT NULL REFERENCES rm_room_type(id),
    upgraded_at             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upgrade_type            VARCHAR(20)     NOT NULL,       -- COMPLIMENTARY, PAID, UPSELL
    price_difference        NUMERIC(15,2),
    reason                  VARCHAR(500),
    created_by              VARCHAR(50),
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rsv_upgrade_sub ON rsv_room_upgrade_history(sub_reservation_id);

COMMENT ON TABLE rsv_room_upgrade_history IS '객실 업그레이드 이력';
COMMENT ON COLUMN rsv_room_upgrade_history.upgrade_type IS 'COMPLIMENTARY: 무료, PAID: 유료, UPSELL: 업셀';
COMMENT ON COLUMN rsv_room_upgrade_history.price_difference IS '잔여 숙박일 기준 총 차액';
