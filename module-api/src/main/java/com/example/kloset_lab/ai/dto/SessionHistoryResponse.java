package com.example.kloset_lab.ai.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

/**
 * 세션 히스토리 응답 DTO (Internal API용)
 *
 * @param sessionId 세션 ID
 * @param uptoTurnNo 조회 상한 턴 번호
 * @param turns 턴 히스토리 목록 (최신 턴 우선)
 */
@Builder
public record SessionHistoryResponse(String sessionId, int uptoTurnNo, List<TurnHistory> turns) {

    @Builder
    public record TurnHistory(
            int turnNo,
            String requestText,
            String querySummary,
            List<Long> outfitIds,
            String reaction,
            LocalDateTime createdAt) {}
}
