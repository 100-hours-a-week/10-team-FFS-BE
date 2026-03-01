package com.example.kloset_lab.chat.dto;

import jakarta.validation.constraints.NotBlank;

/** 메시지 읽음 처리 요청 */
public record ReadRequest(@NotBlank String lastReadMessageId) {}
