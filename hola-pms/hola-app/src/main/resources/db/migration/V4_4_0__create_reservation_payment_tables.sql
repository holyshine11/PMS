-- 결제 정보
CREATE TABLE rsv_reservation_payment (
    id                          BIGSERIAL PRIMARY KEY,
    master_reservation_id       BIGINT       NOT NULL REFERENCES rsv_master_reservation(id),
    payment_status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    total_room_amount           NUMERIC(15,2) NOT NULL DEFAULT 0,
    total_service_amount        NUMERIC(15,2) NOT NULL DEFAULT 0,
    total_service_charge_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    total_adjustment_amount     NUMERIC(15,2) NOT NULL DEFAULT 0,
    grand_total                 NUMERIC(15,2) NOT NULL DEFAULT 0,
    payment_date                TIMESTAMP,
    payment_method              VARCHAR(20),
    sort_order                  INTEGER      NOT NULL DEFAULT 0,
    use_yn                      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP,
    created_by                  VARCHAR(50),
    updated_by                  VARCHAR(50),
    deleted_at                  TIMESTAMP,
    UNIQUE (master_reservation_id)
);

CREATE INDEX idx_rsv_payment_master ON rsv_reservation_payment(master_reservation_id);

-- 금액 조정
CREATE TABLE rsv_payment_adjustment (
    id                      BIGSERIAL PRIMARY KEY,
    master_reservation_id   BIGINT       NOT NULL REFERENCES rsv_master_reservation(id),
    adjustment_seq          INTEGER      NOT NULL,
    currency                VARCHAR(10)  NOT NULL DEFAULT 'KRW',
    adjustment_sign         VARCHAR(1)   NOT NULL DEFAULT '+',
    supply_price            NUMERIC(15,2) NOT NULL DEFAULT 0,
    tax                     NUMERIC(15,2) NOT NULL DEFAULT 0,
    total_amount            NUMERIC(15,2) NOT NULL DEFAULT 0,
    comment                 TEXT,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(50)
);

CREATE INDEX idx_rsv_adjustment_master ON rsv_payment_adjustment(master_reservation_id);

COMMENT ON TABLE rsv_reservation_payment IS '결제 정보';
COMMENT ON COLUMN rsv_reservation_payment.payment_status IS '결제 상태 (PENDING/COMPLETED)';
COMMENT ON COLUMN rsv_reservation_payment.grand_total IS '최종 합계';
COMMENT ON TABLE rsv_payment_adjustment IS '금액 조정';
COMMENT ON COLUMN rsv_payment_adjustment.adjustment_sign IS '조정 방향 (+/-)';
