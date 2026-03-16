-- 낙관적 잠금을 위한 version 컬럼 추가
ALTER TABLE rsv_master_reservation
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE rsv_sub_reservation
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN rsv_master_reservation.version IS '낙관적 잠금 버전';
COMMENT ON COLUMN rsv_sub_reservation.version IS '낙관적 잠금 버전';
