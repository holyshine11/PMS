-- =====================================================
-- V4_19_0: Dayuse(데이유즈) 지원 스키마 확장
-- 기존 숙박 예약 데이터에 영향 없음 (모두 DEFAULT 값 적용)
-- =====================================================

-- 1) 서브 예약에 숙박유형(stayType) 추가
ALTER TABLE rsv_sub_reservation
    ADD COLUMN stay_type VARCHAR(20) NOT NULL DEFAULT 'OVERNIGHT';
COMMENT ON COLUMN rsv_sub_reservation.stay_type IS '숙박유형: OVERNIGHT(숙박), DAY_USE(데이유즈)';

-- Dayuse 이용 시간 범위
ALTER TABLE rsv_sub_reservation
    ADD COLUMN day_use_start_time TIME,
    ADD COLUMN day_use_end_time TIME;
COMMENT ON COLUMN rsv_sub_reservation.day_use_start_time IS 'Dayuse 입실 시간';
COMMENT ON COLUMN rsv_sub_reservation.day_use_end_time IS 'Dayuse 퇴실 시간';

-- 2) 레이트코드에 숙박유형 추가 (패키지 기반 Dayuse 자동 인식)
ALTER TABLE rt_rate_code
    ADD COLUMN stay_type VARCHAR(20) NOT NULL DEFAULT 'OVERNIGHT';
COMMENT ON COLUMN rt_rate_code.stay_type IS '숙박유형: OVERNIGHT(숙박), DAY_USE(데이유즈)';

-- 3) 프로퍼티에 Dayuse 운영 설정 추가
ALTER TABLE htl_property
    ADD COLUMN day_use_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN day_use_start_time VARCHAR(10) DEFAULT '10:00',
    ADD COLUMN day_use_end_time VARCHAR(10) DEFAULT '20:00',
    ADD COLUMN day_use_default_hours INTEGER DEFAULT 5;
COMMENT ON COLUMN htl_property.day_use_enabled IS 'Dayuse 허용 여부';
COMMENT ON COLUMN htl_property.day_use_start_time IS 'Dayuse 시작 가능 시간';
COMMENT ON COLUMN htl_property.day_use_end_time IS 'Dayuse 종료 시간';
COMMENT ON COLUMN htl_property.day_use_default_hours IS 'Dayuse 기본 이용 시간(시간)';

-- 4) Dayuse 전용 요금 테이블
CREATE TABLE rt_day_use_rate (
    id              BIGSERIAL PRIMARY KEY,
    rate_code_id    BIGINT NOT NULL REFERENCES rt_rate_code(id),
    duration_hours  INTEGER NOT NULL,
    supply_price    NUMERIC(15, 2) NOT NULL,
    description     VARCHAR(200),
    sort_order      INTEGER DEFAULT 0,
    use_yn          BOOLEAN NOT NULL DEFAULT true,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE INDEX idx_day_use_rate_rate_code ON rt_day_use_rate(rate_code_id);
COMMENT ON TABLE rt_day_use_rate IS 'Dayuse 시간별 요금 (5시간/6시간 등)';
COMMENT ON COLUMN rt_day_use_rate.duration_hours IS '이용 시간 (시간 단위)';
COMMENT ON COLUMN rt_day_use_rate.supply_price IS '공급가 (세금/봉사료 별도)';
