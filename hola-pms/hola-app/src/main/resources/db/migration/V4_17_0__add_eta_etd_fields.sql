-- 서브 예약에 예상 도착/출발 시간 추가
ALTER TABLE rsv_sub_reservation ADD COLUMN eta TIME;
ALTER TABLE rsv_sub_reservation ADD COLUMN etd TIME;

COMMENT ON COLUMN rsv_sub_reservation.eta IS '예상 도착 시간 (Expected Time of Arrival)';
COMMENT ON COLUMN rsv_sub_reservation.etd IS '예상 출발 시간 (Expected Time of Departure)';
