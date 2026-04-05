-- 봉사료 타입 (PERCENTAGE/FIXED) 및 정액 금액 컬럼 추가
ALTER TABLE htl_property
    ADD COLUMN service_charge_type   VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',
    ADD COLUMN service_charge_amount NUMERIC(15,2) NOT NULL DEFAULT 0;
