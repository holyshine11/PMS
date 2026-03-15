-- 결제 거래 이력에 거래 유형 필드 추가
ALTER TABLE rsv_payment_transaction
    ADD COLUMN transaction_type VARCHAR(20) NOT NULL DEFAULT 'PAYMENT';

COMMENT ON COLUMN rsv_payment_transaction.transaction_type IS '거래 유형: PAYMENT(결제), REFUND(환불), CANCEL_FEE(취소수수료)';
