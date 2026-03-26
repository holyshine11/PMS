-- =============================================
-- 호텔/프로퍼티명 영문 전환 (부킹 엔진 프론트 표기용)
-- =============================================

-- 호텔
UPDATE htl_hotel SET hotel_name = 'Hola Seoul Hotel'  WHERE hotel_code = 'HTL00001';
UPDATE htl_hotel SET hotel_name = 'Hola Busan Hotel'  WHERE hotel_code = 'HTL00002';

-- 프로퍼티
UPDATE htl_property SET property_name = 'Hola Grand Myeongdong' WHERE property_code = 'GMP';
UPDATE htl_property SET property_name = 'Hola Grand Seocho'     WHERE property_code = 'GMS';
UPDATE htl_property SET property_name = 'Hola Beach Haeundae'   WHERE property_code = 'OBH';
