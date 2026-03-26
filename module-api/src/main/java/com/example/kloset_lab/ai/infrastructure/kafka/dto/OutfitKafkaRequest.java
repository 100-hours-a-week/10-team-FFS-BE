package com.example.kloset_lab.ai.infrastructure.kafka.dto;

import java.time.Instant;
import java.util.List;

/**
 * Kafka outfit-request 토픽으로 발행되는 메시지 DTO
 *
 * @param requestId Spring Boot가 생성한 UUID
 * @param userId 사용자 ID
 * @param query 사용자 자연어 요청
 * @param sessionId 멀티턴 세션 ID (파티션 키)
 * @param uploadSlots VTON 이미지 업로드용 presigned URL + fileId 목록
 * @param timestamp 요청 시각 (ISO 8601)
 */
public record OutfitKafkaRequest(
        String requestId, Long userId, String query, String sessionId, List<UploadSlot> uploadSlots, String timestamp) {

    // TODO: AI 팀 협의 후 추가 예정 필드: turnNo, prevTurnNo, prevResultId, prevReaction

    public static OutfitKafkaRequest of(
            String requestId, Long userId, String query, String sessionId, List<UploadSlot> uploadSlots) {
        return new OutfitKafkaRequest(
                requestId, userId, query, sessionId, uploadSlots, Instant.now().toString());
    }
}
