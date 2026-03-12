-- rsv_reservation_payment 테이블에 얼리/레이트 요금 컬럼 추가
ALTER TABLE rsv_reservation_payment
    ADD COLUMN IF NOT EXISTS total_early_late_fee NUMERIC(15,2) NOT NULL DEFAULT 0;

COMMENT ON COLUMN rsv_reservation_payment.total_early_late_fee IS '얼리체크인/레이트체크아웃 요금 합계';
