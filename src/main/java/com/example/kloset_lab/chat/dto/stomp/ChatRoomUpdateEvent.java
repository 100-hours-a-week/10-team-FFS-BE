package com.example.kloset_lab.chat.dto.stomp;

import java.time.Instant;
import lombok.Builder;

/** 채팅 목록 갱신 이벤트 (STOMP /user/queue/chat-room-updates 전달용) */
@Builder
public record ChatRoomUpdateEvent(
        Long roomId,
        String lastMessageId,
        String lastMessageContent,
        String lastMessageType,
        Instant lastMessageAt,
        Long senderId) {}
