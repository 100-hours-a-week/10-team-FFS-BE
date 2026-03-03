package com.example.kloset_lab.chat.event;

import com.example.kloset_lab.chat.dto.stomp.ChatBroadcastMessage;
import com.example.kloset_lab.chat.entity.ChatParticipant;
import java.time.Instant;
import java.util.List;

/** sendMessage MySQL 커밋 후 Redis 캐시·Pub/Sub 처리를 위한 이벤트 */
public record ChatMessageSentEvent(
        Long roomId,
        Long senderId,
        List<ChatParticipant> participants,
        String messageId,
        String contentPreview,
        String type,
        Instant sentAt,
        ChatBroadcastMessage broadcastMessage) {}
