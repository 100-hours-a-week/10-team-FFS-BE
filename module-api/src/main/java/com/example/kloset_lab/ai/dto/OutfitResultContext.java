package com.example.kloset_lab.ai.dto;

/**
 * OutfitResultService 처리 결과 컨텍스트 (WebSocket 발행에 필요한 정보)
 *
 * @param userId 사용자 ID
 * @param sessionId 세션 ID
 */
public record OutfitResultContext(Long userId, String sessionId) {}
