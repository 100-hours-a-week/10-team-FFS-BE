package com.example.kloset_lab.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * 코디추천 요청 상태 조회 응답 DTO (상태 복구용)
 *
 * @param requestId 요청 추적 ID
 * @param sessionId 세션 ID
 * @param turnNo 턴 번호
 * @param status 상태 ("PENDING", "COMPLETED", "FAILED")
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OutfitStatusResponse(String requestId, String sessionId, Integer turnNo, String status) {}
