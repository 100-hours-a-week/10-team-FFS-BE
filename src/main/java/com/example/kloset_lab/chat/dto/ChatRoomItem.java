package com.example.kloset_lab.chat.dto;

import lombok.Builder;

/** 채팅방 목록 아이템 */
@Builder
public record ChatRoomItem(Long roomId, OpponentDto opponent, LastMessageDto lastMessage, long unreadCount) {}
