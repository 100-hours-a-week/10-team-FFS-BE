package com.example.kloset_lab.chat.dto;

import lombok.Builder;

/** 안읽은 메시지 현황 응답 */
@Builder
public record UnreadStatusResponse(boolean hasUnread, long totalUnreadCount) {}
