-- Soft Delete와 호환되도록 UNIQUE 제약조건을 Partial Unique Index로 변경
-- 기존: 삭제된 레코드도 유니크 체크 → 삭제 후 같은 코드로 재등록 불가
-- 변경: deleted_at IS NULL인 레코드만 유니크 체크

-- 1. rt_rate_code: property_id + rate_code
ALTER TABLE rt_rate_code DROP CONSTRAINT rt_rate_code_property_id_rate_code_key;
CREATE UNIQUE INDEX uk_rate_code_active ON rt_rate_code(property_id, rate_code) WHERE deleted_at IS NULL;

-- 2. rt_promotion_code: property_id + promotion_code
ALTER TABLE rt_promotion_code DROP CONSTRAINT uk_promotion_code;
CREATE UNIQUE INDEX uk_promotion_code_active ON rt_promotion_code(property_id, promotion_code) WHERE deleted_at IS NULL;

-- 테스트 데이터 정리
DELETE FROM rt_promotion_code WHERE deleted_at IS NOT NULL;
