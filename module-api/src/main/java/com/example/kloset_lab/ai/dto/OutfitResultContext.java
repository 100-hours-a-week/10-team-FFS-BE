package com.example.kloset_lab.ai.dto;

import java.util.List;
import lombok.Builder;

/**
 * OutfitResultService 처리 결과 컨텍스트 (WebSocket 발행에 필요한 정보)
 *
 * @param userId 사용자 ID
 * @param sessionId 세션 ID
 * @param outfits 저장된 코디 결과 요약 (success일 때만 존재)
 */
@Builder
public record OutfitResultContext(Long userId, String sessionId, List<OutfitSummary> outfits) {

    /**
     * 코디 결과 요약 (TX2에서 저장 후 Consumer로 전달)
     */
    @Builder
    public record OutfitSummary(Long resultId, List<Long> clothesIds, String reaction, String vtonImageUrl) {}
}
