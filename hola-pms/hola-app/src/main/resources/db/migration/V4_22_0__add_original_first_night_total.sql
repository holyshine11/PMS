-- 취소 수수료 기준: 최초 예약 시점 1박 총액 보존
-- 업그레이드 후에도 원래 예약 요금 기준으로 취소 수수료 계산
ALTER TABLE rsv_reservation_payment
    ADD COLUMN IF NOT EXISTS original_first_night_total NUMERIC(15,2);

COMMENT ON COLUMN rsv_reservation_payment.original_first_night_total
    IS '최초 예약 시점 1박 총액 (취소 수수료 기준, 업그레이드 후에도 불변)';
