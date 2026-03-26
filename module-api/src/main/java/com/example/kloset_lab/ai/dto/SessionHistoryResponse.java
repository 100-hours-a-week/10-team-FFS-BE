package com.example.kloset_lab.ai.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

/**
 * 세션 히스토리 응답 DTO
 *
 * @param sessionId 세션 ID
 * @param uptoTurnNo 조회 상한 턴 번호
 * @param turns 턴 히스토리 목록
 * @param hasNext 다음 페이지 존재 여부
 */
@Builder
public record SessionHistoryResponse(String sessionId, int uptoTurnNo, List<TurnHistory> turns, boolean hasNext) {

    /**
     * @param status 요청 상태 (PENDING, COMPLETED, FAILED, CLARIFICATION_NEEDED)
     */
    @Builder
    public record TurnHistory(
            int turnNo,
            String requestText,
            String querySummary,
            String status,
            List<OutfitDetail> outfits,
            LocalDateTime createdAt) {}

    @Builder
    public record OutfitDetail(Long resultId, List<Long> clothesIds, String reaction, String vtonImageUrl) {}
}
