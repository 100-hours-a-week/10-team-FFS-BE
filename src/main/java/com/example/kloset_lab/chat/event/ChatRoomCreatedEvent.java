package com.example.kloset_lab.chat.event;

/** createOrGetRoom에서 신규 방 생성 후 Redis 캐시 등록을 위한 이벤트 */
public record ChatRoomCreatedEvent(Long userId, Long opponentUserId, Long roomId, double score) {}
