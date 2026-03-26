-- 간편결제 카드 (빌키) 테이블
CREATE TABLE rsv_easy_pay_card (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(100) NOT NULL,
    batch_key       VARCHAR(100) NOT NULL,
    card_mask_no    VARCHAR(30),
    issuer_name     VARCHAR(50),
    card_type       VARCHAR(20),
    pg_cno          VARCHAR(20),
    card_alias      VARCHAR(50),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50),
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    use_yn          BOOLEAN DEFAULT TRUE,
    sort_order      INTEGER DEFAULT 0
);

-- 이메일별 조회 인덱스
CREATE INDEX idx_rsv_easy_pay_card_email ON rsv_easy_pay_card(email) WHERE deleted_at IS NULL;

-- 빌키 유니크 인덱스
CREATE UNIQUE INDEX idx_rsv_easy_pay_card_batch_key ON rsv_easy_pay_card(batch_key) WHERE deleted_at IS NULL;

COMMENT ON TABLE rsv_easy_pay_card IS '간편결제 카드 (KICC 빌키)';
COMMENT ON COLUMN rsv_easy_pay_card.email IS '게스트 이메일 (카드 소유자 식별)';
COMMENT ON COLUMN rsv_easy_pay_card.batch_key IS 'KICC 빌키 (batchKey)';
COMMENT ON COLUMN rsv_easy_pay_card.card_mask_no IS '마스킹 카드번호 (UI 표시용)';
COMMENT ON COLUMN rsv_easy_pay_card.issuer_name IS '발급사명 (삼성/현대/신한 등)';
COMMENT ON COLUMN rsv_easy_pay_card.card_type IS '카드종류 (신용/체크)';
COMMENT ON COLUMN rsv_easy_pay_card.pg_cno IS 'PG 거래고유번호';
COMMENT ON COLUMN rsv_easy_pay_card.card_alias IS '카드 별칭 (선택)';
