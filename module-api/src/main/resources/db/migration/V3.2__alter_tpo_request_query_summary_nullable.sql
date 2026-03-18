-- tpo_request.query_summary NOT NULL → NULL 변경
-- 비동기 전환 후 TX1(요청 생성)에서 querySummary 없이 INSERT,
-- TX3(AI 응답 수신)에서 addQuerySummary()로 채우는 구조이므로 NULL 허용 필요
ALTER TABLE `tpo_request`
    MODIFY COLUMN `query_summary` VARCHAR(255) NULL COMMENT 'AI 요청문 요약 (비동기 시 응답에서 설정)';