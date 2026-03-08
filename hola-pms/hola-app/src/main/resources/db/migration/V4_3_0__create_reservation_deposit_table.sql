-- 예치금 정보
CREATE TABLE rsv_reservation_deposit (
    id                      BIGSERIAL PRIMARY KEY,
    master_reservation_id   BIGINT       NOT NULL REFERENCES rsv_master_reservation(id),
    deposit_method          VARCHAR(20)  NOT NULL DEFAULT 'CREDIT_CARD',
    card_company            VARCHAR(50),
    card_number_encrypted   VARCHAR(500),
    card_cvc_encrypted      VARCHAR(200),
    card_expiry_date        VARCHAR(7),
    card_password_encrypted VARCHAR(200),
    currency                VARCHAR(10)  NOT NULL DEFAULT 'KRW',
    amount                  NUMERIC(15,2) NOT NULL DEFAULT 0,
    sort_order              INTEGER      NOT NULL DEFAULT 0,
    use_yn                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(50),
    updated_by              VARCHAR(50),
    deleted_at              TIMESTAMP
);

CREATE INDEX idx_rsv_deposit_master ON rsv_reservation_deposit(master_reservation_id);

COMMENT ON TABLE rsv_reservation_deposit IS '예치금 정보';
COMMENT ON COLUMN rsv_reservation_deposit.deposit_method IS '예치 방법 (CREDIT_CARD/CASH)';
COMMENT ON COLUMN rsv_reservation_deposit.card_number_encrypted IS '카드번호 (AES 암호화)';
COMMENT ON COLUMN rsv_reservation_deposit.card_cvc_encrypted IS 'CVC (AES 암호화)';
COMMENT ON COLUMN rsv_reservation_deposit.card_expiry_date IS '카드 유효기간 (MM/YYYY)';
COMMENT ON COLUMN rsv_reservation_deposit.card_password_encrypted IS '카드 비밀번호 (AES 암호화)';
