-- KICC PG 연동을 위한 PaymentTransaction 필드 추가
ALTER TABLE rsv_payment_transaction
    ADD COLUMN pg_provider VARCHAR(20),
    ADD COLUMN pg_cno VARCHAR(20),
    ADD COLUMN pg_transaction_id VARCHAR(60),
    ADD COLUMN pg_status_code VARCHAR(10),
    ADD COLUMN pg_approval_no VARCHAR(100),
    ADD COLUMN pg_approval_date VARCHAR(14),
    ADD COLUMN pg_card_no VARCHAR(20),
    ADD COLUMN pg_issuer_name VARCHAR(50),
    ADD COLUMN pg_acquirer_name VARCHAR(50),
    ADD COLUMN pg_installment_month INTEGER DEFAULT 0,
    ADD COLUMN pg_card_type VARCHAR(10),
    ADD COLUMN pg_raw_response TEXT;

-- 기존 거래에 MOCK 프로바이더 설정
UPDATE rsv_payment_transaction SET pg_provider = 'MOCK' WHERE pg_provider IS NULL;

-- PG 거래번호 인덱스
CREATE INDEX idx_rsv_txn_pg_cno ON rsv_payment_transaction(pg_cno);
