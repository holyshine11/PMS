-- 프로모션 코드 테이블
CREATE TABLE rt_promotion_code (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL,
    rate_code_id BIGINT NOT NULL,
    promotion_code VARCHAR(50) NOT NULL,
    promotion_start_date DATE NOT NULL,
    promotion_end_date DATE NOT NULL,
    description_ko VARCHAR(500),
    description_en VARCHAR(500),
    promotion_type VARCHAR(20) NOT NULL,
    down_up_sign VARCHAR(1),
    down_up_value NUMERIC(15,2),
    down_up_unit VARCHAR(10),
    rounding_decimal_point INTEGER DEFAULT 0,
    rounding_digits INTEGER DEFAULT 0,
    rounding_method VARCHAR(20),
    use_yn BOOLEAN DEFAULT true,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    CONSTRAINT fk_promo_property FOREIGN KEY (property_id) REFERENCES htl_property(id),
    CONSTRAINT fk_promo_rate_code FOREIGN KEY (rate_code_id) REFERENCES rt_rate_code(id),
    CONSTRAINT uk_promotion_code UNIQUE (property_id, promotion_code)
);

CREATE INDEX idx_promo_code_property ON rt_promotion_code(property_id);
CREATE INDEX idx_promo_code_rate_code ON rt_promotion_code(rate_code_id);
