-- Down/Up sale 컬럼을 rt_rate_code에서 rt_rate_pricing으로 이동
-- 각 요금 행(요일별)마다 독립적인 Down/Up sale 설정 가능

-- rt_rate_pricing에 Down/Up sale 컬럼 추가
ALTER TABLE rt_rate_pricing ADD COLUMN down_up_sign VARCHAR(1);
ALTER TABLE rt_rate_pricing ADD COLUMN down_up_value NUMERIC(15,2);
ALTER TABLE rt_rate_pricing ADD COLUMN down_up_unit VARCHAR(10);
ALTER TABLE rt_rate_pricing ADD COLUMN rounding_decimal_point INTEGER DEFAULT 0;
ALTER TABLE rt_rate_pricing ADD COLUMN rounding_digits INTEGER DEFAULT 0;
ALTER TABLE rt_rate_pricing ADD COLUMN rounding_method VARCHAR(20);

-- 기존 rt_rate_code의 Down/Up sale 데이터를 rt_rate_pricing으로 마이그레이션
-- 주의: 하나의 레이트코드에 요금 행이 여러 개인 경우 동일한 Down/Up sale 값이 모든 행에 복사됨
-- (기존 구조가 레이트코드 레벨이었으므로 의도된 동작)
UPDATE rt_rate_pricing rp
SET down_up_sign = rc.down_up_sign,
    down_up_value = rc.down_up_value,
    down_up_unit = rc.down_up_unit,
    rounding_decimal_point = rc.rounding_decimal_point,
    rounding_digits = rc.rounding_digits,
    rounding_method = rc.rounding_method
FROM rt_rate_code rc
WHERE rp.rate_code_id = rc.id
  AND rc.down_up_sign IS NOT NULL;

-- rt_rate_code에서 Down/Up sale 컬럼 제거
ALTER TABLE rt_rate_code DROP COLUMN down_up_sign;
ALTER TABLE rt_rate_code DROP COLUMN down_up_value;
ALTER TABLE rt_rate_code DROP COLUMN down_up_unit;
ALTER TABLE rt_rate_code DROP COLUMN rounding_decimal_point;
ALTER TABLE rt_rate_code DROP COLUMN rounding_digits;
ALTER TABLE rt_rate_code DROP COLUMN rounding_method;
