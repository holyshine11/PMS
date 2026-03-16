-- 업그레이드 차액, 얼리/레이트 수수료 등 PaidServiceOption과 연결되지 않는 서비스 항목 지원
ALTER TABLE rsv_reservation_service ALTER COLUMN service_option_id DROP NOT NULL;

COMMENT ON COLUMN rsv_reservation_service.service_option_id IS '유료 서비스 옵션 ID (업그레이드 차액 등 자동 생성 항목은 NULL)';
