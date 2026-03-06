-- 유료 서비스 옵션 테이블
CREATE TABLE rm_paid_service_option (
    id                  BIGSERIAL       PRIMARY KEY,
    property_id         BIGINT          NOT NULL REFERENCES htl_property(id),
    service_option_code VARCHAR(50)     NOT NULL,
    service_name_ko     VARCHAR(200)    NOT NULL,
    service_name_en     VARCHAR(200),
    service_type        VARCHAR(20)     NOT NULL,
    applicable_nights   VARCHAR(20)     NOT NULL,
    currency_code       VARCHAR(3)      NOT NULL DEFAULT 'KRW',
    vat_included        BOOLEAN         NOT NULL DEFAULT TRUE,
    tax_rate            NUMERIC(5,2)    NOT NULL DEFAULT 0,
    supply_price        NUMERIC(15,2)   NOT NULL DEFAULT 0,
    tax_amount          NUMERIC(15,2)   NOT NULL DEFAULT 0,
    vat_included_price  NUMERIC(15,2)   NOT NULL DEFAULT 0,
    quantity            INTEGER         NOT NULL DEFAULT 1,
    quantity_unit       VARCHAR(10)     NOT NULL DEFAULT 'EA',
    admin_memo          TEXT,
    sort_order          INTEGER         NOT NULL DEFAULT 0,
    use_yn              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50),
    deleted_at          TIMESTAMP,
    UNIQUE (property_id, service_option_code)
);

CREATE INDEX idx_rm_paid_svc_opt_property ON rm_paid_service_option(property_id);
CREATE INDEX idx_rm_paid_svc_opt_type ON rm_paid_service_option(service_type);

COMMENT ON TABLE rm_paid_service_option IS '유료 서비스 옵션';
COMMENT ON COLUMN rm_paid_service_option.property_id IS '프로퍼티 ID';
COMMENT ON COLUMN rm_paid_service_option.service_option_code IS '서비스 옵션 코드';
COMMENT ON COLUMN rm_paid_service_option.service_name_ko IS '서비스명 (국문)';
COMMENT ON COLUMN rm_paid_service_option.service_name_en IS '서비스명 (영문)';
COMMENT ON COLUMN rm_paid_service_option.service_type IS '서비스 유형 (ROOM_AMENITY/BREAKFAST/ROOM_SERVICE)';
COMMENT ON COLUMN rm_paid_service_option.applicable_nights IS '적용 박수 (FIRST_NIGHT_ONLY/ALL_NIGHTS/NOT_APPLICABLE)';
COMMENT ON COLUMN rm_paid_service_option.currency_code IS '통화 (KRW/USD)';
COMMENT ON COLUMN rm_paid_service_option.vat_included IS '부가세 포함여부';
COMMENT ON COLUMN rm_paid_service_option.tax_rate IS '세율 (%)';
COMMENT ON COLUMN rm_paid_service_option.supply_price IS '공급가';
COMMENT ON COLUMN rm_paid_service_option.tax_amount IS 'TAX 금액';
COMMENT ON COLUMN rm_paid_service_option.vat_included_price IS 'VAT 포함 가격';
COMMENT ON COLUMN rm_paid_service_option.quantity IS '수량';
COMMENT ON COLUMN rm_paid_service_option.quantity_unit IS '수량 단위 (EA/SET/TIME/SERVICE)';
COMMENT ON COLUMN rm_paid_service_option.admin_memo IS '관리자 메모';
