package com.example.kloset_lab.ai.dto;

import lombok.Builder;

/**
 * 코디 추천 요청 수락 응답 DTO (202 Accepted)
 *
 * @param requestId 요청 추적 ID (UUID)
 * @param sessionId 세션 ID
 * @param turnNo 턴 번호
 * @param status 상태 ("accepted")
 */
@Builder
public record OutfitAcceptedResponse(String requestId, String sessionId, int turnNo, String status) {

    public static OutfitAcceptedResponse of(String requestId, String sessionId, int turnNo) {
        return new OutfitAcceptedResponse(requestId, sessionId, turnNo, "accepted");
    }
}
