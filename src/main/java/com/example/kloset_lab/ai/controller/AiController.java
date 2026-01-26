package com.example.kloset_lab.ai.controller;

import com.example.kloset_lab.ai.dto.TpoFeedbackRequest;
import com.example.kloset_lab.ai.dto.TpoOutfitsRequest;
import com.example.kloset_lab.ai.dto.TpoOutfitsResponse;
import com.example.kloset_lab.ai.dto.TpoRequestHistoryResponse;
import com.example.kloset_lab.ai.service.AiService;
import com.example.kloset_lab.global.response.ApiResponse;
import com.example.kloset_lab.global.response.ApiResponses;
import com.example.kloset_lab.global.response.Message;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * TPO 코디 생성 요청 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param request TPO 코디 생성 요청 내용 DTO
     * @return 생성된 TPO 코디 결과
     */
    @PostMapping("/v1/outfits")
    public ResponseEntity<ApiResponse<TpoOutfitsResponse>> generateTpoOutfits(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody TpoOutfitsRequest request) {
        TpoOutfitsResponse response = aiService.generateTpoOutfits(userId, request);
        return ApiResponses.ok(Message.TPO_OUTFITS_RETRIEVED, response);
    }

    /**
     * 최근 TPO 요청 기록 조회 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @return 최근 TPO 요청 기록 리스트
     */
    @GetMapping("/v1/outfits/histories")
    public ResponseEntity<ApiResponse<TpoRequestHistoryResponse>> getRecentTpoRequests(
            @AuthenticationPrincipal Long userId) {
        TpoRequestHistoryResponse response = aiService.getRecentTpoRequests(userId);
        return ApiResponses.ok(Message.RECENT_TPO_REQUESTS_RETRIEVED, response);
    }

    /**
     * TPO 결과 피드백 등록 API
     *
     * @param resultId TPO 결과 ID
     * @param request 피드백 요청 DTO
     * @return 성공 응답
     */
    @PatchMapping("/v1/outfits/feedbacks/{resultId}")
    public ResponseEntity<ApiResponse<Void>> recordReaction(
            @PathVariable Long resultId, @Valid @RequestBody TpoFeedbackRequest request) {
        aiService.recordReaction(resultId, request);
        return ApiResponses.ok(Message.REACTION_RECORDED);
    }
}
