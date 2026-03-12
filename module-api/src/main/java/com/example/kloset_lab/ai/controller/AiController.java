package com.example.kloset_lab.ai.controller;

import com.example.kloset_lab.ai.dto.OutfitAcceptedResponse;
import com.example.kloset_lab.ai.dto.OutfitStatusResponse;
import com.example.kloset_lab.ai.dto.ShopRecommendationRequest;
import com.example.kloset_lab.ai.dto.ShopRecommendationResponse;
import com.example.kloset_lab.ai.dto.TpoFeedbackRequest;
import com.example.kloset_lab.ai.dto.TpoOutfitsRequest;
import com.example.kloset_lab.ai.dto.TpoOutfitsResponse;
import com.example.kloset_lab.ai.dto.TpoRequestHistoryResponse;
import com.example.kloset_lab.ai.service.AiService;
import com.example.kloset_lab.ai.service.OutfitService;
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
    private final OutfitService outfitService;

    /**
     * 비동기 코디 추천 요청 API (Kafka 기반, 202 Accepted)
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param request 코디 추천 요청 DTO
     * @param sessionId 세션 ID (null이면 새 세션 생성)
     * @return 수락 응답 (requestId, sessionId, turnNo)
     */
    @PostMapping("/v2/outfits")
    public ResponseEntity<ApiResponse<OutfitAcceptedResponse>> requestOutfit(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TpoOutfitsRequest request,
            @RequestParam(required = false) String sessionId) {
        OutfitAcceptedResponse response = outfitService.requestOutfit(userId, request, sessionId);
        return ApiResponses.accepted(Message.OUTFIT_REQUEST_ACCEPTED, response);
    }

    /**
     * TPO 코디 생성 요청 API (v1 동기 방식 — 레거시)
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
     * 코디추천 요청 상태 조회 API (WebSocket 재연결 시 상태 복구용)
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param requestId 요청 추적 ID
     * @return 요청 상태 (PENDING / COMPLETED / FAILED)
     */
    @GetMapping("/v2/outfits/requests/{requestId}")
    public ResponseEntity<ApiResponse<OutfitStatusResponse>> getRequestStatus(
            @AuthenticationPrincipal Long userId, @PathVariable String requestId) {
        OutfitStatusResponse response = outfitService.getRequestStatus(userId, requestId);
        return ApiResponses.ok(Message.CLOTHES_POLLING_RESULT_RETRIEVED, response);
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
     * 코디 결과 피드백 등록 API (v2 세션 기반 — 서버 규칙 적용)
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param resultId TPO 결과 ID
     * @param request 피드백 요청 DTO
     * @return 성공 응답
     */
    @PatchMapping("/v2/outfits/feedbacks/{resultId}")
    public ResponseEntity<ApiResponse<Void>> recordOutfitReaction(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long resultId,
            @Valid @RequestBody TpoFeedbackRequest request) {
        outfitService.recordReaction(userId, resultId, request);
        return ApiResponses.ok(Message.REACTION_RECORDED);
    }

    /**
     * TPO 결과 피드백 등록 API (v1 레거시)
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param resultId TPO 결과 ID
     * @param request 피드백 요청 DTO
     * @return 성공 응답
     */
    @PatchMapping("/v1/outfits/feedbacks/{resultId}")
    public ResponseEntity<ApiResponse<Void>> recordReaction(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long resultId,
            @Valid @RequestBody TpoFeedbackRequest request) {
        aiService.recordReaction(userId, resultId, request);
        return ApiResponses.ok(Message.REACTION_RECORDED);
    }

    /**
     * 쇼핑 검색 기반 코디 추천 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param request 쇼핑 코디 추천 요청 DTO
     * @return 쇼핑 코디 추천 결과
     */
    @PostMapping("/v2/product-recommendations")
    public ResponseEntity<ApiResponse<ShopRecommendationResponse>> searchShopOutfits(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody ShopRecommendationRequest request) {
        return ApiResponses.ok(Message.PRODUCTS_FETCHED, aiService.searchShopOutfits(userId, request));
    }

    /**
     * 쇼핑 코디 피드백 등록 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param resultId 쇼핑 코디 결과 ID
     * @param request 피드백 요청 DTO
     * @return 성공 응답
     */
    @PatchMapping("/v2/product-recommendations/feedbacks/{resultId}")
    public ResponseEntity<ApiResponse<Void>> recordShopReaction(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long resultId,
            @Valid @RequestBody TpoFeedbackRequest request) {
        aiService.recordShopReaction(userId, resultId, request);
        return ApiResponses.ok(Message.REACTION_RECORDED);
    }
}
