package com.example.kloset_lab.global.infrastructure;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * 코디추천 WebSocket 메시지 DTO (Redis Pub/Sub 경유)
 *
 * <p>module-api(발행)와 module-chat(구독) 양쪽에서 사용한다.
 *
 * @param requestId 요청 추적 ID
 * @param sessionId 세션 ID
 * @param status 상태 ("processing", "success", "failed", "clarification_needed")
 * @param step 진행 단계 (processing일 때)
 * @param stepLabel 진행 단계 라벨 (processing일 때)
 * @param errorCode 에러 코드 (failed일 때)
 * @param errorMessage 에러 메시지 (failed일 때)
 * @param message 재질문 메시지 (clarification_needed일 때)
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OutfitWebSocketMessage(
        String requestId,
        String sessionId,
        String status,
        String step,
        String stepLabel,
        String errorCode,
        String errorMessage,
        String message) {

    public static OutfitWebSocketMessage progress(String requestId, String sessionId, String step, String stepLabel) {
        return OutfitWebSocketMessage.builder()
                .requestId(requestId)
                .sessionId(sessionId)
                .status("processing")
                .step(step)
                .stepLabel(stepLabel)
                .build();
    }

    public static OutfitWebSocketMessage success(String requestId, String sessionId) {
        return OutfitWebSocketMessage.builder()
                .requestId(requestId)
                .sessionId(sessionId)
                .status("success")
                .build();
    }

    public static OutfitWebSocketMessage failed(
            String requestId, String sessionId, String errorCode, String errorMessage) {
        return OutfitWebSocketMessage.builder()
                .requestId(requestId)
                .sessionId(sessionId)
                .status("failed")
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    public static OutfitWebSocketMessage clarificationNeeded(String requestId, String sessionId, String message) {
        return OutfitWebSocketMessage.builder()
                .requestId(requestId)
                .sessionId(sessionId)
                .status("clarification_needed")
                .message(message)
                .build();
    }
}
