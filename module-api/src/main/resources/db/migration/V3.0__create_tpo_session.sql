-- 코디추천 Kafka 비동기 전환: tpo_session 테이블 생성 및 tpo_request 확장

CREATE TABLE `tpo_session`
(
    `id`                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `session_id`          VARCHAR(36) NOT NULL COMMENT '세션 고유 ID (UUID)',
    `user_id`             BIGINT      NOT NULL COMMENT 'FK: user.id',
    `last_turn_no`        INT         NOT NULL DEFAULT 0 COMMENT '마지막 턴 번호',
    `inflight_request_id` VARCHAR(36) NULL COMMENT '현재 처리 중인 요청 ID',
    `inflight_started_at` DATETIME(6) NULL COMMENT '인플라이트 시작 시각',
    `last_activity_at`    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '마지막 활동 시각',
    `created_at`          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tpo_session_session_id` (`session_id`),
    INDEX `idx_tpo_session_user` (`user_id`),
    CONSTRAINT `fk_tpo_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- tpo_request에 세션/턴/요청ID/상태 컬럼 추가
ALTER TABLE `tpo_request`
    ADD COLUMN `tpo_session_id` BIGINT      NULL COMMENT 'FK: tpo_session.id' AFTER `user_id`,
    ADD COLUMN `request_id`     VARCHAR(36) NULL COMMENT '요청 고유 ID (UUID)' AFTER `tpo_session_id`,
    ADD COLUMN `turn_no`        INT         NULL COMMENT '턴 번호' AFTER `request_id`,
    ADD COLUMN `status`         VARCHAR(20) NULL COMMENT '요청 상태 (PENDING|COMPLETED|FAILED)' AFTER `turn_no`;

ALTER TABLE `tpo_request`
    ADD UNIQUE KEY `uk_tpo_request_request_id` (`request_id`),
    ADD UNIQUE KEY `uk_tpo_request_session_turn` (`tpo_session_id`, `turn_no`),
    ADD CONSTRAINT `fk_tpo_request_session` FOREIGN KEY (`tpo_session_id`) REFERENCES `tpo_session` (`id`);