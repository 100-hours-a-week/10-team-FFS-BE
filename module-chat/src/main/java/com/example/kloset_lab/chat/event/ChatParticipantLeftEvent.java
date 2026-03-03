package com.example.kloset_lab.chat.event;

/** leaveRoom 후 Redis 캐시 정리를 위한 이벤트 */
public record ChatParticipantLeftEvent(Long userId, Long roomId) {}
