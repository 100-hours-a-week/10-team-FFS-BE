-- 채팅방 테이블
CREATE TABLE chat_room
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    created_at           DATETIME(6)  NOT NULL,
    deleted_at           DATETIME(6)  DEFAULT NULL,
    last_message_id      VARCHAR(24)  DEFAULT NULL COMMENT 'MongoDB ObjectId',
    last_message_content VARCHAR(255) DEFAULT NULL,
    last_message_at      DATETIME(6)  DEFAULT NULL,
    last_message_type    VARCHAR(10)  DEFAULT NULL COMMENT 'TEXT | IMAGE | FEED',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 채팅 참여자 테이블
CREATE TABLE chat_participant
(
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    room_id             BIGINT      NOT NULL,
    user_id             BIGINT      NOT NULL,
    created_at          DATETIME(6) NOT NULL,
    entered_at          DATETIME(6) NOT NULL COMMENT '이 시각 이후 메시지만 조회 가능',
    last_read_message_id VARCHAR(24) DEFAULT NULL COMMENT 'MongoDB ObjectId',
    PRIMARY KEY (id),
    UNIQUE KEY uq_room_user (room_id, user_id),
    INDEX idx_user_rooms (user_id),
    CONSTRAINT fk_chat_participant_room FOREIGN KEY (room_id) REFERENCES chat_room (id),
    CONSTRAINT fk_chat_participant_user FOREIGN KEY (user_id) REFERENCES user (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;