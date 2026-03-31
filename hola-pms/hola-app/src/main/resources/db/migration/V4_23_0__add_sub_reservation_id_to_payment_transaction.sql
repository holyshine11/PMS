-- Leg별 결제 추적을 위한 sub_reservation_id 컬럼 추가
ALTER TABLE rsv_payment_transaction ADD COLUMN sub_reservation_id BIGINT;

-- 인덱스 추가
CREATE INDEX idx_rsv_txn_sub ON rsv_payment_transaction(sub_reservation_id);

-- FK 참조 (soft delete 사용하므로 CASCADE 없이)
ALTER TABLE rsv_payment_transaction
    ADD CONSTRAINT fk_rsv_txn_sub_reservation
    FOREIGN KEY (sub_reservation_id) REFERENCES rsv_sub_reservation(id);

-- 기존 부킹엔진 PG 결제를 첫 번째 Leg에 귀속
UPDATE rsv_payment_transaction pt
SET sub_reservation_id = (
    SELECT MIN(sr.id)
    FROM rsv_sub_reservation sr
    WHERE sr.master_reservation_id = pt.master_reservation_id
    AND sr.deleted_at IS NULL
)
WHERE pt.pg_cno IS NOT NULL
AND pt.transaction_type = 'PAYMENT'
AND pt.sub_reservation_id IS NULL;
