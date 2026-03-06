-- 층/호수 매핑 unique constraint 수정: 같은 호수가 다른 층에 배정 가능하도록 floor_id 포함
ALTER TABLE rm_room_type_floor DROP CONSTRAINT IF EXISTS uk_rm_room_type_floor;
ALTER TABLE rm_room_type_floor ADD CONSTRAINT uk_rm_room_type_floor UNIQUE (room_type_id, floor_id, room_number_id);
