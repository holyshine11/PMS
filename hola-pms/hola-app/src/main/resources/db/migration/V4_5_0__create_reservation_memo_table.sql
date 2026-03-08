-- 관리자 메모
CREATE TABLE rsv_reservation_memo (
    id                      BIGSERIAL PRIMARY KEY,
    master_reservation_id   BIGINT       NOT NULL REFERENCES rsv_master_reservation(id),
    content                 TEXT         NOT NULL,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(50)
);

CREATE INDEX idx_rsv_memo_master ON rsv_reservation_memo(master_reservation_id);

COMMENT ON TABLE rsv_reservation_memo IS '관리자 메모';
COMMENT ON COLUMN rsv_reservation_memo.content IS '메모 내용';
