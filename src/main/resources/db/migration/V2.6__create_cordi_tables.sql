CREATE TABLE `cordi_request`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`       BIGINT       NOT NULL COMMENT 'FK: user.id',
    `request_text`  VARCHAR(100) NOT NULL COMMENT '사용자 요청 텍스트',
    `request_count` INT          NOT NULL DEFAULT 0 COMMENT '요청 횟수',
    `created_at`    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    INDEX `idx_cordi_request_user` (`user_id`),
    CONSTRAINT `fk_cordi_request_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `cordi_result`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `cordi_request_id` BIGINT       NOT NULL COMMENT 'FK: cordi_request.id',
    `query_summary`    VARCHAR(255) NOT NULL COMMENT 'AI 요청문 요약',
    `reaction`         VARCHAR(10)  NOT NULL DEFAULT 'NONE' COMMENT '사용자 반응 (NONE|GOOD|BAD)',
    `created_at`       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    INDEX `idx_cordi_result_request` (`cordi_request_id`),
    CONSTRAINT `fk_cordi_result_request` FOREIGN KEY (`cordi_request_id`) REFERENCES `cordi_request` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
