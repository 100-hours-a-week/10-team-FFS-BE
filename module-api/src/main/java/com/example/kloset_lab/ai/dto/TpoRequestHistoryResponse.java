package com.example.kloset_lab.ai.dto;

import java.util.List;
import lombok.Builder;

/**
 * TPO 요청 기록 조회 응답 DTO
 *
 * @param requestHistories TPO 요청 기록 리스트
 */
@Builder
public record TpoRequestHistoryResponse(List<RequestHistory> requestHistories) {
    @Builder
    public record RequestHistory(Long requestId, String content) {}
}
