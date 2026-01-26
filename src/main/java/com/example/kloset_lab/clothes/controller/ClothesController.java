package com.example.kloset_lab.clothes.controller;

import com.example.kloset_lab.clothes.dto.*;
import com.example.kloset_lab.clothes.service.ClothesAnalysisService;
import com.example.kloset_lab.clothes.service.ClothesService;
import com.example.kloset_lab.global.response.ApiResponse;
import com.example.kloset_lab.global.response.ApiResponses;
import com.example.kloset_lab.global.response.Message;
import jakarta.validation.Valid;
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
}
