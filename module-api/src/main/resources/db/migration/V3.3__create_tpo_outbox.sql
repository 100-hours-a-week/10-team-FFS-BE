-- Outbox 패턴: TX1과 동일 트랜잭션 내 저장 → Relay가 Kafka 발행 보장
CREATE TABLE tpo_outbox (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    request_id    VARCHAR(36)  NOT NULL                   COMMENT 'TpoRequest requestId (FK 없음, 논리적 참조)',
    partition_key VARCHAR(255) NOT NULL                   COMMENT 'Kafka 파티션 키 (sessionId)',
    payload       TEXT         NOT NULL                   COMMENT '직렬화된 OutfitKafkaRequest JSON',
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | PUBLISHED',
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tpo_outbox_request_id (request_id),
    INDEX idx_tpo_outbox_status_created (status, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;