-- chat_room.created_at, chat_participant.created_at / entered_at 컬럼에
-- DEFAULT CURRENT_TIMESTAMP(6) 추가 (스키마 문서와 동기화)
ALTER TABLE chat_room
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시 (UTC)';

ALTER TABLE chat_participant
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시 (UTC)',
    MODIFY COLUMN entered_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '입장 시각. 이 시각 이후 메시지만 조회 가능 (UTC)';
