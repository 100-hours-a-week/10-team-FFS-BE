package com.example.kloset_lab.ai.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

/**
 * 세션 목록 조회 응답 DTO
 *
 * @param sessions 세션 미리보기 목록 (최근 활동순)
 * @param hasNext 다음 페이지 존재 여부
 */
@Builder
public record SessionListResponse(List<SessionPreview> sessions, boolean hasNext) {

    /**
     * 세션 미리보기 (목록에서 한 줄로 표시)
     *
     * @param sessionId 세션 ID
     * @param title 세션 제목 (첫 번째 요청 텍스트)
     * @param lastActivityAt 마지막 활동 시각
     */
    @Builder
    public record SessionPreview(String sessionId, String title, LocalDateTime lastActivityAt) {}
}
