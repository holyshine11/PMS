-- 요금행에 기간(startDate, endDate) 필드 추가
ALTER TABLE rt_rate_pricing ADD COLUMN start_date DATE NOT NULL DEFAULT '2026-01-01';
ALTER TABLE rt_rate_pricing ADD COLUMN end_date DATE NOT NULL DEFAULT '2026-12-31';

-- 기존 데이터: 소속 레이트코드의 판매기간으로 업데이트
UPDATE rt_rate_pricing rp
SET start_date = rc.sale_start_date, end_date = rc.sale_end_date
FROM rt_rate_code rc WHERE rp.rate_code_id = rc.id;

-- 기간 검색 성능 인덱스
CREATE INDEX idx_rt_rate_pricing_period ON rt_rate_pricing(rate_code_id, start_date, end_date);
