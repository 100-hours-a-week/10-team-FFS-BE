package com.example.kloset_lab.chat.dto.stomp;

import com.example.kloset_lab.chat.dto.ChatImageDto;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/** 채팅방 내 메시지 브로드캐스트 페이로드 */
@Builder
public record ChatBroadcastMessage(
        String messageId,
        Long roomId,
        Long senderId,
        String senderNickname,
        String type,
        String content,
        List<ChatImageDto> images,
        Long relatedFeedId,
        String clientMessageId,
        Instant createdAt) {}
