package com.example.kloset_lab.chat.dto;

import java.util.List;
import lombok.Builder;

/** 채팅 메시지 목록 응답 */
@Builder
public record ChatMessageListResponse(List<ChatMessageItem> messages, boolean hasNextPage, String nextCursor) {}
