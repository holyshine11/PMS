-- 무료 서비스 옵션 테이블
CREATE TABLE rm_free_service_option (
    id                  BIGSERIAL       PRIMARY KEY,
    property_id         BIGINT          NOT NULL REFERENCES htl_property(id),
    service_option_code VARCHAR(50)     NOT NULL,
    service_name_ko     VARCHAR(200)    NOT NULL,
    service_name_en     VARCHAR(200),
    service_type        VARCHAR(20)     NOT NULL,
    applicable_nights   VARCHAR(20)     NOT NULL,
    quantity            INTEGER         NOT NULL DEFAULT 1,
    quantity_unit       VARCHAR(10)     NOT NULL DEFAULT 'EA',
    sort_order          INTEGER         NOT NULL DEFAULT 0,
    use_yn              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50),
    deleted_at          TIMESTAMP,
    UNIQUE (property_id, service_option_code)
);

CREATE INDEX idx_rm_free_svc_opt_property ON rm_free_service_option(property_id);
CREATE INDEX idx_rm_free_svc_opt_type ON rm_free_service_option(service_type);

COMMENT ON TABLE rm_free_service_option IS '무료 서비스 옵션';
COMMENT ON COLUMN rm_free_service_option.property_id IS '프로퍼티 ID';
COMMENT ON COLUMN rm_free_service_option.service_option_code IS '서비스 옵션 코드';
COMMENT ON COLUMN rm_free_service_option.service_name_ko IS '서비스명 (국문)';
COMMENT ON COLUMN rm_free_service_option.service_name_en IS '서비스명 (영문)';
COMMENT ON COLUMN rm_free_service_option.service_type IS '서비스 유형 (BED/VIEW/ROOM_AMENITY/BREAKFAST)';
COMMENT ON COLUMN rm_free_service_option.applicable_nights IS '적용 박수 (FIRST_NIGHT_ONLY/ALL_NIGHTS/NOT_APPLICABLE)';
COMMENT ON COLUMN rm_free_service_option.quantity IS '수량';
COMMENT ON COLUMN rm_free_service_option.quantity_unit IS '수량 단위 (EA/SET/TIME/SERVICE)';
