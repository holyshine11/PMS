-- 프로퍼티 파일 업로드 컬럼 추가 (사업자등록증, 로고)
ALTER TABLE htl_property ADD COLUMN biz_license_path VARCHAR(500);
ALTER TABLE htl_property ADD COLUMN logo_path VARCHAR(500);
