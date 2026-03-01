package com.example.kloset_lab.chat.dto;

import java.util.List;
import lombok.Builder;

/** 채팅방 목록 응답 */
@Builder
public record ChatRoomListResponse(List<ChatRoomItem> rooms, boolean hasNextPage, Double nextCursor) {}
