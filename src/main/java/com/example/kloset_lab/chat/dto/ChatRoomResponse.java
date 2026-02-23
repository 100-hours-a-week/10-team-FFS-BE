package com.example.kloset_lab.chat.dto;

import java.time.Instant;
import lombok.Builder;

/** 채팅방 생성/조회 응답 */
@Builder
public record ChatRoomResponse(Long roomId, OpponentDto opponent, Instant createdAt) {}
