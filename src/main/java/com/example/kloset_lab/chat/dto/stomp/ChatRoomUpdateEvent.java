package com.example.kloset_lab.chat.dto.stomp;

import com.example.kloset_lab.chat.dto.LastMessageDto;
import lombok.Builder;

/** 채팅 목록 갱신 이벤트 (STOMP /user/queue/chat-room-updates 전달용) */
@Builder
public record ChatRoomUpdateEvent(Long roomId, LastMessageDto lastMessage, Long senderId) {}
