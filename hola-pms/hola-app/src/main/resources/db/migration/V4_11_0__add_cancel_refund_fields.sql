-- 예약 결제 테이블에 취소/환불 필드 추가
ALTER TABLE rsv_reservation_payment
    ADD COLUMN cancel_fee_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    ADD COLUMN refund_amount NUMERIC(15, 2) NOT NULL DEFAULT 0;

COMMENT ON COLUMN rsv_reservation_payment.cancel_fee_amount IS '취소 수수료 금액';
COMMENT ON COLUMN rsv_reservation_payment.refund_amount IS '환불 금액';
