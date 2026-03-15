-- 결제 동시성 보호: 낙관적 락(@Version)용 버전 컬럼 추가
-- 동시 결제 요청 시 OptimisticLockException 발생으로 초과 결제 방지
ALTER TABLE rsv_reservation_payment ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
