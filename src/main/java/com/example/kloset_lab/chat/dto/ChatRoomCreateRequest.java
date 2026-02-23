package com.example.kloset_lab.chat.dto;

import jakarta.validation.constraints.NotNull;

/** 채팅방 생성 요청 */
public record ChatRoomCreateRequest(@NotNull Long opponentUserId) {}
