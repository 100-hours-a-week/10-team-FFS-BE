-- 분석 배치 테이블
CREATE TABLE analyze_batch
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id        VARCHAR(26)  NOT NULL,
    user_id         BIGINT       NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    total_count     INT          NOT NULL,
    completed_count INT          NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE KEY uk_batch_id (batch_id),
    KEY idx_user_id (user_id),
    KEY idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 분석 소스 테이블
CREATE TABLE analyze_source
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id       VARCHAR(26)  NOT NULL,
    batch_id        BIGINT       NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    passed          TINYINT(1)   NULL,
    detected_count  INT          NULL,
    completed_count INT          NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE KEY uk_source_id (source_id),
    KEY idx_batch_id (batch_id),
    KEY idx_status (status),
    CONSTRAINT fk_source_batch FOREIGN KEY (batch_id) REFERENCES analyze_batch (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 분석 태스크 테이블
CREATE TABLE analyze_task
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id    VARCHAR(26)  NOT NULL,
    source_id  BIGINT       NOT NULL,
    status     VARCHAR(30)  NOT NULL,
    file_id    BIGINT       NULL,
    major      JSON         NULL,
    extra      JSON         NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE KEY uk_task_id (task_id),
    KEY idx_source_id (source_id),
    KEY idx_status (status),
    KEY idx_file_id (file_id),
    CONSTRAINT fk_task_source FOREIGN KEY (source_id) REFERENCES analyze_source (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;