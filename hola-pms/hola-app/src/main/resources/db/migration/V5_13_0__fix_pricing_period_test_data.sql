-- V3_5_0에서 DEFAULT로 들어간 기간을 레이트코드 판매기간으로 보정
UPDATE rt_rate_pricing rp
SET start_date = rc.sale_start_date, end_date = rc.sale_end_date
FROM rt_rate_code rc WHERE rp.rate_code_id = rc.id;
