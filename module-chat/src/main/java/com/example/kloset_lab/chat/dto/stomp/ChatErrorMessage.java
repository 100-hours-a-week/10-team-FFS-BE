package com.example.kloset_lab.chat.dto.stomp;

import lombok.Builder;

/** STOMP 에러 메시지 (/user/queue/errors 전달용) */
@Builder
public record ChatErrorMessage(String clientMessageId, String errorCode, String message) {}
