-- TAX/봉사료 정보 컬럼 추가
ALTER TABLE htl_property
    ADD COLUMN tax_rate                       NUMERIC(5,2)  NOT NULL DEFAULT 0,
    ADD COLUMN tax_decimal_places             INTEGER       NOT NULL DEFAULT 0,
    ADD COLUMN tax_rounding_method            VARCHAR(20),
    ADD COLUMN service_charge_rate            NUMERIC(5,2)  NOT NULL DEFAULT 0,
    ADD COLUMN service_charge_decimal_places  INTEGER       NOT NULL DEFAULT 0,
    ADD COLUMN service_charge_rounding_method VARCHAR(20);
