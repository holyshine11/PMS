-- =============================================
-- V1.2.0: 호텔 테이블 설계 명세 정렬
-- htl_hotel: 불필요 컬럼 제거, 신규 컬럼 추가
-- =============================================

-- 설계서에 없는 컬럼 제거
ALTER TABLE htl_hotel DROP COLUMN IF EXISTS hotel_type;
ALTER TABLE htl_hotel DROP COLUMN IF EXISTS star_rating;
ALTER TABLE htl_hotel DROP COLUMN IF EXISTS fax;
ALTER TABLE htl_hotel DROP COLUMN IF EXISTS website;

-- 설계서 기반 신규 컬럼 추가
ALTER TABLE htl_hotel ADD COLUMN representative_name     VARCHAR(50);
ALTER TABLE htl_hotel ADD COLUMN representative_name_en  VARCHAR(100);
ALTER TABLE htl_hotel ADD COLUMN country_code            VARCHAR(10)  DEFAULT '+82';
ALTER TABLE htl_hotel ADD COLUMN introduction             TEXT;
ALTER TABLE htl_hotel ADD COLUMN address_en               VARCHAR(500);
ALTER TABLE htl_hotel ADD COLUMN address_detail_en        VARCHAR(500);

-- description 컬럼명은 유지 (기존 데이터 호환)

-- 호텔코드 자동생성용 시퀀스
CREATE SEQUENCE IF NOT EXISTS htl_hotel_code_seq START WITH 1 INCREMENT BY 1;

COMMENT ON COLUMN htl_hotel.representative_name IS '대표자명 (국문)';
COMMENT ON COLUMN htl_hotel.representative_name_en IS '대표자명 (영문)';
COMMENT ON COLUMN htl_hotel.country_code IS '국가번호 (기본 +82)';
COMMENT ON COLUMN htl_hotel.introduction IS '호텔 소개';
COMMENT ON COLUMN htl_hotel.address_en IS '영문주소';
COMMENT ON COLUMN htl_hotel.address_detail_en IS '영문상세주소';
COMMENT ON SEQUENCE htl_hotel_code_seq IS '호텔코드 자동생성 시퀀스 (HTL00001~)';
