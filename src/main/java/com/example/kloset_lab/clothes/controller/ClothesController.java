package com.example.kloset_lab.clothes.controller;

import com.example.kloset_lab.clothes.dto.*;
import com.example.kloset_lab.clothes.service.ClothesAnalysisService;
import com.example.kloset_lab.clothes.service.ClothesService;
import com.example.kloset_lab.feed.dto.ClothesDto;
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
@RequestMapping("/api/v1/clothes")
@RequiredArgsConstructor
public class ClothesController {

    private final ClothesService clothesService;
    private final ClothesAnalysisService clothesAnalysisService;

    @PostMapping("/analyses")
    public ResponseEntity<ApiResponse<ClothesAnalysisResponse>> requestAnalysis(
            @AuthenticationPrincipal Long currentUserId, @Valid @RequestBody ClothesAnalysisRequest request) {
        ClothesAnalysisResponse response = clothesAnalysisService.requestAnalysis(currentUserId, request.fileIds());

        return ApiResponses.accepted(Message.AI_PRECHECK_COMPLETED, response);
    }

    @GetMapping("/analyses/{batchId}")
    public ResponseEntity<ApiResponse<ClothesPollingResponse>> getAnalysisResult(
            @AuthenticationPrincipal Long currentUserId, @PathVariable String batchId) {
        ClothesPollingResponse response = clothesAnalysisService.getAnalysisResult(currentUserId, batchId);

        return ApiResponses.ok(Message.CLOTHES_POLLING_RESULT_RETRIEVED, response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClothesDetailResponse>> createClothes(
            @AuthenticationPrincipal Long currentUserId, @RequestBody ClothesCreateRequest request) {
        ClothesDetailResponse response = clothesService.createClothes(currentUserId, request);

        return ApiResponses.created(Message.CLOTHES_CREATED, response);
    }

    @GetMapping("/{clothesId}")
    public ResponseEntity<ApiResponse<ClothesDetailResponse>> getClothesDetail(
            @AuthenticationPrincipal Long currentUserId, @PathVariable Long clothesId) {
        ClothesDetailResponse response = clothesService.getClothesDetail(currentUserId, clothesId);

        return ApiResponses.ok(Message.CLOTHES_DETAIL_RETRIEVED, response);
    }

    @PatchMapping("/{clothesId}")
    public ResponseEntity<ApiResponse<ClothesDetailResponse>> updateClothes(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long clothesId,
            @RequestBody ClothesUpdateRequest request) {
        ClothesDetailResponse response = clothesService.updateClothes(currentUserId, clothesId, request);

        return ApiResponses.ok(Message.CLOTHES_DETAIL_UPDATED, response);
    }

    @DeleteMapping("/{clothesId}")
    public ResponseEntity<ApiResponse<Void>> deleteClothes(
            @AuthenticationPrincipal Long currentUserId, @PathVariable Long clothesId) {
        clothesService.deleteClothes(currentUserId, clothesId);

        return ApiResponses.ok(Message.CLOTHES_DELETED);
    }

    @GetMapping("/clothes-details")
    public ResponseEntity<ApiResponse<List<ClothesDto>>> getClothesDetails(
            @AuthenticationPrincipal Long currentUserId, @RequestParam List<Long> clothesIds) {
        return ApiResponses.ok("옷 세부 정보 반환", clothesService.getClothesDetails(clothesIds));
    }
}
