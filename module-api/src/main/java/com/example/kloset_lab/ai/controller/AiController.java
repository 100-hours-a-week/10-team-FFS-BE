package com.example.kloset_lab.ai.controller;

import com.example.kloset_lab.ai.dto.OutfitAcceptedResponse;
import com.example.kloset_lab.ai.dto.OutfitClothesResponse;
import com.example.kloset_lab.ai.dto.OutfitStatusResponse;
import com.example.kloset_lab.ai.dto.SessionHistoryResponse;
import com.example.kloset_lab.ai.dto.SessionListResponse;
import com.example.kloset_lab.ai.dto.ShopRecommendationRequest;
import com.example.kloset_lab.ai.dto.ShopRecommendationResponse;
import com.example.kloset_lab.ai.dto.TpoFeedbackRequest;
import com.example.kloset_lab.ai.dto.TpoOutfitsRequest;
import com.example.kloset_lab.ai.dto.TpoOutfitsResponse;
import com.example.kloset_lab.ai.dto.TpoRequestHistoryResponse;
import com.example.kloset_lab.ai.service.AiService;
import com.example.kloset_lab.ai.service.OutfitService;
import com.example.kloset_lab.ai.service.SessionHistoryService;
import com.example.kloset_lab.global.response.ApiResponse;
import com.example.kloset_lab.global.response.ApiResponses;
import com.example.kloset_lab.global.response.Message;
import jakarta.validation.Valid;
import java.util.List;
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
    private final SessionHistoryService sessionHistoryService;

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
     * 세션 목록 조회 API (최근 활동순, 페이징)
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param page 페이지 번호 (0부터)
     * @param size 페이지 크기 (기본 10)
     * @return 세션 미리보기 목록
     */
    @GetMapping("/v2/outfits/sessions")
    public ResponseEntity<ApiResponse<SessionListResponse>> getSessionList(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        SessionListResponse response = sessionHistoryService.getSessionList(userId, page, size);
        return ApiResponses.ok(Message.SESSION_LIST_RETRIEVED, response);
    }

    /**
     * 세션 상세 조회 API (최신 턴 우선, 페이징)
     *
     * <p>PENDING 상태의 턴은 outfits가 빈 배열이며, status 필드로 로딩 상태를 구분한다.
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param sessionId 세션 ID
     * @param page 페이지 번호 (0부터)
     * @param size 페이지 크기 (기본 10)
     * @return 세션 내 턴 히스토리
     */
    @GetMapping("/v2/outfits/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<SessionHistoryResponse>> getSessionDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        SessionHistoryResponse response = sessionHistoryService.getSessionDetail(sessionId, userId, page, size);
        return ApiResponses.ok(Message.SESSION_DETAIL_RETRIEVED, response);
    }

    /**
     * 코디 결과에 포함된 옷 상세 정보 일괄 조회 API
     *
     * <p>한 턴에서 나온 여러 코디 결과의 옷 정보를 1회 호출로 조회한다.
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param resultIds 코디 결과 ID 목록 (쉼표 구분)
     * @return resultId별 옷 상세 정보
     */
    @GetMapping("/v2/outfits/results/clothes")
    public ResponseEntity<ApiResponse<OutfitClothesResponse>> getOutfitClothes(
            @AuthenticationPrincipal Long userId, @RequestParam List<Long> resultIds) {
        OutfitClothesResponse response = sessionHistoryService.getOutfitClothes(userId, resultIds);
        return ApiResponses.ok(Message.OUTFIT_CLOTHES_RETRIEVED, response);
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
