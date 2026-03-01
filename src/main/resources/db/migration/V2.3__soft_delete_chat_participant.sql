-- chat_participant 테이블에 left_at 컬럼 추가 (soft delete)
-- NULL이면 현재 참여 중, NOT NULL이면 해당 시각에 퇴장한 상태
ALTER TABLE chat_participant
    ADD COLUMN left_at DATETIME(6) NULL DEFAULT NULL COMMENT '퇴장 시각. NULL이면 현재 참여 중 (UTC)';
