package com.example.kloset_lab.chat.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

/** 채팅 메시지 목록 아이템 */
@Builder
public record ChatMessageItem(
        String messageId,
        Long senderId,
        String type,
        String content,
        List<ChatImageDto> images,
        Long relatedFeedId,
        Instant createdAt) {}
