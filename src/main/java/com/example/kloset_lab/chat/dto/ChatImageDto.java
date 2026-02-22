package com.example.kloset_lab.chat.dto;

import lombok.Builder;

/** 채팅 메시지 이미지 DTO */
@Builder
public record ChatImageDto(Long mediaFileId, String imageUrl, int displayOrder) {}
