-- 서브 예약에 실제 체크인/체크아웃 시각 및 얼리/레이트 요금 필드 추가
ALTER TABLE rsv_sub_reservation ADD COLUMN actual_check_in_time TIMESTAMP;
ALTER TABLE rsv_sub_reservation ADD COLUMN actual_check_out_time TIMESTAMP;
ALTER TABLE rsv_sub_reservation ADD COLUMN early_check_in_fee NUMERIC(15, 2) DEFAULT 0;
ALTER TABLE rsv_sub_reservation ADD COLUMN late_check_out_fee NUMERIC(15, 2) DEFAULT 0;

-- 결제 테이블에 얼리/레이트 요금 합계 필드 추가
ALTER TABLE rsv_reservation_payment ADD COLUMN total_early_late_fee NUMERIC(15, 2) DEFAULT 0;
