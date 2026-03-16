-- 예약 서비스 항목의 service_type 컬럼 확장 (RATE_INCLUDED 값 수용)
-- 기존: VARCHAR(10) → 변경: VARCHAR(20)
ALTER TABLE rsv_reservation_service ALTER COLUMN service_type TYPE VARCHAR(20);

COMMENT ON COLUMN rsv_reservation_service.service_type IS '서비스 유형 (PAID/FREE/RATE_INCLUDED)';
