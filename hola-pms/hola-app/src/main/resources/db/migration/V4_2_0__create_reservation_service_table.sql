-- 예약 서비스 (유료/무료)
CREATE TABLE rsv_reservation_service (
    id                      BIGSERIAL PRIMARY KEY,
    sub_reservation_id      BIGINT       NOT NULL REFERENCES rsv_sub_reservation(id) ON DELETE CASCADE,
    service_type            VARCHAR(10)  NOT NULL,
    service_option_id       BIGINT       NOT NULL,
    service_date            DATE,
    quantity                INTEGER      NOT NULL DEFAULT 1,
    unit_price              NUMERIC(15,2) NOT NULL DEFAULT 0,
    tax                     NUMERIC(15,2) NOT NULL DEFAULT 0,
    total_price             NUMERIC(15,2) NOT NULL DEFAULT 0,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP
);

CREATE INDEX idx_rsv_service_sub ON rsv_reservation_service(sub_reservation_id);
CREATE INDEX idx_rsv_service_type ON rsv_reservation_service(service_type);

COMMENT ON TABLE rsv_reservation_service IS '예약 서비스 (유료/무료)';
COMMENT ON COLUMN rsv_reservation_service.service_type IS '서비스 유형 (PAID/FREE)';
COMMENT ON COLUMN rsv_reservation_service.service_option_id IS '서비스 옵션 ID (유료/무료 테이블 참조)';
COMMENT ON COLUMN rsv_reservation_service.service_date IS '서비스 제공 일자';
