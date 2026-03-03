package com.example.kloset_lab.chat.event;

/** markAsRead·getMessages 후 Redis 미읽음 카운터 초기화를 위한 이벤트 */
public record ChatReadEvent(Long userId, Long roomId) {}
