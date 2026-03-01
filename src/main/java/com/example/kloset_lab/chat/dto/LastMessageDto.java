package com.example.kloset_lab.chat.dto;

import java.time.Instant;
import lombok.Builder;

/** 채팅방 마지막 메시지 정보 */
@Builder
public record LastMessageDto(String messageId, String content, String type, Instant sentAt) {}
