package com.example.kloset_lab.chat.dto;

import lombok.Builder;

/** 채팅 상대방 프로필 정보 */
@Builder
public record OpponentDto(Long userId, String nickname, String profileImageUrl) {}
