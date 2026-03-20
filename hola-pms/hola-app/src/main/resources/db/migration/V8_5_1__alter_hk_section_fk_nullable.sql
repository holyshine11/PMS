-- V8.5.1: JPA 단방향 @OneToMany 호환을 위해 section_id를 nullable로 변경
-- JPA는 INSERT 시 FK=null → 이후 UPDATE로 설정하는 방식이므로 NOT NULL 제약과 충돌
ALTER TABLE hk_section_floor ALTER COLUMN section_id DROP NOT NULL;
ALTER TABLE hk_section_housekeeper ALTER COLUMN section_id DROP NOT NULL;
