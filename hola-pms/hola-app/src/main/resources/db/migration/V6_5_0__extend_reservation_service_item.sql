-- ReservationServiceItem에 TransactionCode 연결 + 포스팅 상태 추가
ALTER TABLE rsv_reservation_service
    ADD COLUMN transaction_code_id  BIGINT REFERENCES rm_transaction_code(id),
    ADD COLUMN posting_status       VARCHAR(10) NOT NULL DEFAULT 'POSTED';

CREATE INDEX idx_rsv_service_tc ON rsv_reservation_service(transaction_code_id);

COMMENT ON COLUMN rsv_reservation_service.transaction_code_id IS '트랜잭션 코드 FK';
COMMENT ON COLUMN rsv_reservation_service.posting_status IS 'POSTED/PENDING/VOIDED';
