package com.example.kloset_lab.feed.controller;

import com.example.kloset_lab.feed.dto.FeedCreateRequest;
import com.example.kloset_lab.feed.dto.FeedDetailResponse;
import com.example.kloset_lab.feed.service.FeedService;
import com.example.kloset_lab.global.response.ApiResponse;
import com.example.kloset_lab.global.response.ApiResponses;
import com.example.kloset_lab.global.response.Message;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * 피드 업로드 API
     *
     * @param userId  현재 로그인한 사용자 ID
     * @param request 피드 생성 요청
     * @return 생성된 피드 상세 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FeedDetailResponse>> createFeed(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody FeedCreateRequest request) {
        FeedDetailResponse response = feedService.createFeed(userId, request);
        return ApiResponses.created(Message.FEED_CREATED, response);
    }
}
