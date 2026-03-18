package com.example.kloset_lab.ai.infrastructure.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * outfit-response 토픽 메시지 DTO (processing / success / failed / clarification_needed 공용)
 *
 * @param requestId 요청 추적 ID
 * @param status 상태 ("processing", "success", "failed", "clarification_needed")
 * @param querySummary 요청 요약문 (success일 때)
 * @param step 진행 단계 (processing일 때)
 * @param stepLabel 진행 단계 라벨 (processing일 때)
 * @param outfits 코디 결과 목록 (success일 때)
 * @param metadata 메타데이터 (success일 때)
 * @param error 에러 정보 (failed일 때)
 * @param message 재질문 메시지 (clarification_needed일 때)
 * @param timestamp 발행 시각
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OutfitKafkaResponse(
        String requestId,
        String status,
        String querySummary,
        String step,
        String stepLabel,
        List<Outfit> outfits,
        Metadata metadata,
        Error error,
        String message,
        String timestamp) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Outfit(
            String outfitId,
            String description,
            List<Long> clothesIds,
            List<Item> items,
            String vtonImageUrl,
            Long fileId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(Long clothesId, String imageUrl, String category, String role) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(boolean shopSupplemented, boolean fallbackUsed, long processingTimeMs) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(String code, String message, Integer retryAfterSeconds) {}

    public boolean isProcessing() {
        return "processing".equals(status);
    }

    public boolean isSuccess() {
        return "success".equals(status);
    }

    public boolean isFailed() {
        return "failed".equals(status);
    }

    public boolean isClarificationNeeded() {
        return "clarification_needed".equals(status);
    }
}
