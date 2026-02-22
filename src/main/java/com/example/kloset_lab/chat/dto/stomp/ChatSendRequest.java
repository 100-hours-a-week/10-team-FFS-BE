package com.example.kloset_lab.chat.dto.stomp;

import java.util.List;

/** STOMP 메시지 전송 요청 */
public record ChatSendRequest(
        /** TEXT | IMAGE | FEED */
        String type,
        /** 텍스트 내용 또는 부가 텍스트 */
        String content,
        /** IMAGE 타입일 때 media_file ID 목록 */
        List<Long> mediaFileIds,
        /** FEED 타입일 때 피드 ID */
        Long relatedFeedId,
        /** 클라이언트 임시 메시지 ID (중복 방지용) */
        String clientMessageId) {}
