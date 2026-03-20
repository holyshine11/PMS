-- hk_config 테이블에 간단정리(TOUCH_UP) 기본 크레딧 컬럼 추가
ALTER TABLE hk_config
    ADD COLUMN IF NOT EXISTS default_touch_up_credit DECIMAL(3,1) DEFAULT 0.3;
