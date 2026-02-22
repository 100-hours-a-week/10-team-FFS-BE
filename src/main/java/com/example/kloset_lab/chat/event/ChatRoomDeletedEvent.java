package com.example.kloset_lab.chat.event;

/** leaveRoom 마지막 퇴장 후 Redis·MongoDB 정리를 위한 이벤트 */
public record ChatRoomDeletedEvent(Long roomId) {}
