package com.example.kloset_lab.ai.controller;

import com.example.kloset_lab.ai.dto.SessionHistoryResponse;
import com.example.kloset_lab.ai.service.SessionHistoryService;
import com.example.kloset_lab.global.response.ApiResponse;
import com.example.kloset_lab.global.response.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI Worker용 내부 API 컨트롤러 (X-Internal-Api-Key 인증)
 */
@RestController
@RequestMapping("/api/internal/ai")
@RequiredArgsConstructor
public class InternalAiController {

    private final SessionHistoryService sessionHistoryService;

    /**
     * 세션 히스토리 조회 (AI Worker가 Redis 캐시 miss 시 호출)
     *
     * @param sessionId 세션 ID
     * @param userId 사용자 ID
     * @param uptoTurnNo 조회 상한 턴 번호
     * @param limit 최대 조회 건수 (기본 10)
     * @return 세션 히스토리
     */
    @GetMapping("/sessions/{sessionId}/history")
    public ResponseEntity<ApiResponse<SessionHistoryResponse>> getSessionHistory(
            @PathVariable String sessionId,
            @RequestParam Long userId,
            @RequestParam int uptoTurnNo,
            @RequestParam(defaultValue = "10") int limit) {
        SessionHistoryResponse response = sessionHistoryService.getSessionHistory(sessionId, userId, uptoTurnNo, limit);
        return ApiResponses.ok("session_history_retrieved", response);
    }
}
