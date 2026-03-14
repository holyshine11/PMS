-- 결제 거래 이력 테이블
CREATE TABLE rsv_payment_transaction (
    id                      BIGSERIAL PRIMARY KEY,
    master_reservation_id   BIGINT NOT NULL,
    transaction_seq         INTEGER NOT NULL,
    payment_method          VARCHAR(20) NOT NULL,
    amount                  NUMERIC(15,2) NOT NULL,
    currency                VARCHAR(10) NOT NULL DEFAULT 'KRW',
    transaction_status      VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    approval_no             VARCHAR(50),
    memo                    TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(50)
);
CREATE INDEX idx_rsv_txn_master ON rsv_payment_transaction(master_reservation_id);

-- ReservationPayment에 결제 누적액 컬럼 추가
ALTER TABLE rsv_reservation_payment ADD COLUMN total_paid_amount NUMERIC(15,2) NOT NULL DEFAULT 0;

-- 기존 COMPLETED → PAID 변환 + 누적액 설정
UPDATE rsv_reservation_payment SET payment_status = 'PAID', total_paid_amount = grand_total
WHERE payment_status = 'COMPLETED';

-- 기존 PENDING → UNPAID 변환 (프론트엔드 statusMap과 통일)
UPDATE rsv_reservation_payment SET payment_status = 'UNPAID'
WHERE payment_status = 'PENDING';

-- 기존 완료 결제를 거래 이력으로 이관
INSERT INTO rsv_payment_transaction (master_reservation_id, transaction_seq, payment_method, amount, currency, transaction_status, created_at, created_by)
SELECT rp.master_reservation_id, 1, COALESCE(rp.payment_method, 'CARD'), rp.grand_total, 'KRW', 'COMPLETED',
       COALESCE(rp.payment_date, rp.created_at), COALESCE(rp.created_by, 'SYSTEM')
FROM rsv_reservation_payment rp WHERE rp.payment_status = 'PAID' AND rp.total_paid_amount > 0;
