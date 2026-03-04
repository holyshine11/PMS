-- =============================================
-- V1.3.0: 하위 테이블 설계 명세 정렬
-- htl_floor, htl_room_number, htl_market_code, htl_property
-- =============================================

-- ===== htl_floor 변경 =====
-- floor_number: INTEGER → VARCHAR(20) (코드값: "1F", "MF", "ROOF" 등)
ALTER TABLE htl_floor ALTER COLUMN floor_number TYPE VARCHAR(20) USING floor_number::VARCHAR;
-- description 분리: 국문/영문
ALTER TABLE htl_floor RENAME COLUMN description TO description_ko;
ALTER TABLE htl_floor ADD COLUMN description_en TEXT;

COMMENT ON COLUMN htl_floor.floor_number IS '층 코드 (예: 1F, B1, MF, ROOF)';
COMMENT ON COLUMN htl_floor.description_ko IS '설명 (국문)';
COMMENT ON COLUMN htl_floor.description_en IS '설명 (영문)';

-- ===== htl_room_number 변경 =====
-- floor_id FK 제거
ALTER TABLE htl_room_number DROP CONSTRAINT IF EXISTS htl_room_number_floor_id_fkey;
ALTER TABLE htl_room_number DROP COLUMN IF EXISTS floor_id;
ALTER TABLE htl_room_number DROP COLUMN IF EXISTS room_code;
-- description 분리
ALTER TABLE htl_room_number RENAME COLUMN description TO description_ko;
ALTER TABLE htl_room_number ADD COLUMN description_en TEXT;
-- 불필요 인덱스 제거
DROP INDEX IF EXISTS idx_htl_room_number_floor;

COMMENT ON COLUMN htl_room_number.description_ko IS '설명 (국문)';
COMMENT ON COLUMN htl_room_number.description_en IS '설명 (영문)';

-- ===== htl_market_code 변경 =====
-- description 분리
ALTER TABLE htl_market_code RENAME COLUMN description TO description_ko;
ALTER TABLE htl_market_code ADD COLUMN description_en TEXT;

COMMENT ON COLUMN htl_market_code.description_ko IS '설명 (국문)';
COMMENT ON COLUMN htl_market_code.description_en IS '설명 (영문)';

-- ===== htl_property 변경 =====
ALTER TABLE htl_property ADD COLUMN star_rating           VARCHAR(10);
ALTER TABLE htl_property ADD COLUMN timezone              VARCHAR(50)  DEFAULT 'Asia/Seoul';
ALTER TABLE htl_property ADD COLUMN representative_name   VARCHAR(50);
ALTER TABLE htl_property ADD COLUMN representative_name_en VARCHAR(100);
ALTER TABLE htl_property ADD COLUMN country_code          VARCHAR(10)  DEFAULT '+82';
ALTER TABLE htl_property ADD COLUMN business_number       VARCHAR(20);
ALTER TABLE htl_property ADD COLUMN introduction          TEXT;
ALTER TABLE htl_property ADD COLUMN zip_code              VARCHAR(10);
ALTER TABLE htl_property ADD COLUMN address               VARCHAR(500);
ALTER TABLE htl_property ADD COLUMN address_detail        VARCHAR(500);
ALTER TABLE htl_property ADD COLUMN address_en            VARCHAR(500);
ALTER TABLE htl_property ADD COLUMN address_detail_en     VARCHAR(500);

COMMENT ON COLUMN htl_property.star_rating IS '등급 (5STAR, 4STAR, 3STAR)';
COMMENT ON COLUMN htl_property.timezone IS '시간대 (기본 Asia/Seoul)';
COMMENT ON COLUMN htl_property.representative_name IS '대표자명 (국문)';
COMMENT ON COLUMN htl_property.representative_name_en IS '대표자명 (영문)';
COMMENT ON COLUMN htl_property.country_code IS '국가번호 (기본 +82)';
COMMENT ON COLUMN htl_property.business_number IS '사업자등록번호';
COMMENT ON COLUMN htl_property.introduction IS '프로퍼티 소개';
COMMENT ON COLUMN htl_property.zip_code IS '우편번호';
COMMENT ON COLUMN htl_property.address IS '주소';
COMMENT ON COLUMN htl_property.address_detail IS '상세주소';
COMMENT ON COLUMN htl_property.address_en IS '영문주소';
COMMENT ON COLUMN htl_property.address_detail_en IS '영문상세주소';
