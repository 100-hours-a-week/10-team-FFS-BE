-- chat_room 테이블에 room_type 컬럼 추가 (1:1 DM / 그룹 채팅 구분)
-- 기존 데이터는 모두 DM으로 간주
ALTER TABLE chat_room
    ADD COLUMN room_type VARCHAR(10) NOT NULL DEFAULT 'DM' COMMENT '채팅방 유형 (DM: 1:1, GROUP: 그룹)';
