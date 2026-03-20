-- hk_day_off 테이블 FK 제약조건 추가 (V8.7.0에서 누락)
ALTER TABLE hk_day_off
    ADD CONSTRAINT fk_hk_day_off_property
        FOREIGN KEY (property_id) REFERENCES htl_property(id);

ALTER TABLE hk_day_off
    ADD CONSTRAINT fk_hk_day_off_housekeeper
        FOREIGN KEY (housekeeper_id) REFERENCES sys_admin_user(id);

-- 월별 조회 성능을 위한 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_hk_day_off_property_date
    ON hk_day_off(property_id, day_off_date);
