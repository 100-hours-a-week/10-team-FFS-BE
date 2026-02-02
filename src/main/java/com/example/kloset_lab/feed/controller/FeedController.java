package com.example.kloset_lab.feed.controller;

import static com.example.kloset_lab.global.constants.PaginationDefaults.FEED_LIST;
import static com.example.kloset_lab.global.constants.PaginationDefaults.LIKE_USER_LIST;

import com.example.kloset_lab.feed.dto.FeedCreateRequest;
import com.example.kloset_lab.feed.dto.FeedDetailResponse;
import com.example.kloset_lab.feed.dto.FeedLikeUserItem;
import com.example.kloset_lab.feed.dto.FeedListItem;
import com.example.kloset_lab.feed.dto.FeedUpdateRequest;
import com.example.kloset_lab.feed.service.FeedService;
import com.example.kloset_lab.global.response.ApiResponse;
import com.example.kloset_lab.global.response.ApiResponses;
import com.example.kloset_lab.global.response.LikeResponse;
import com.example.kloset_lab.global.response.Message;
import com.example.kloset_lab.global.response.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
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
    @PostMapping("/v1/feeds")
    public ResponseEntity<ApiResponse<FeedDetailResponse>> createFeed(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody FeedCreateRequest request) {
        FeedDetailResponse response = feedService.createFeed(userId, request);
        return ApiResponses.created(Message.FEED_CREATED, response);
    }

    /**
     * 피드 홈 목록 조회 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param after  커서 (이전 페이지 마지막 피드 ID)
     * @param limit  조회 개수
     * @return 피드 목록 및 페이지 정보
     */
    @GetMapping("/v1/feeds")
    public ResponseEntity<ApiResponse<PagedResponse<FeedListItem>>> getFeeds(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = FEED_LIST) int limit) {
        PagedResponse<FeedListItem> response = feedService.getFeeds(userId, after, limit);
        return ApiResponses.ok(Message.FEEDS_RETRIEVED, response);
    }

    /**
     * 피드 상세 조회 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param feedId 조회할 피드 ID
     * @return 피드 상세 정보
     */
    @GetMapping("/v1/feeds/{feedId}")
    public ResponseEntity<ApiResponse<FeedDetailResponse>> getFeed(
            @AuthenticationPrincipal Long userId, @PathVariable Long feedId) {
        FeedDetailResponse response = feedService.getFeed(userId, feedId);
        return ApiResponses.ok(Message.FEED_RETRIEVED, response);
    }

    /**
     * 피드 수정 API
     *
     * @param userId  현재 로그인한 사용자 ID
     * @param feedId  수정할 피드 ID
     * @param request 피드 수정 요청
     * @return 수정된 피드 상세 정보
     */
    @PatchMapping("/v1/feeds/{feedId}")
    public ResponseEntity<ApiResponse<FeedDetailResponse>> updateFeed(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long feedId,
            @Valid @RequestBody FeedUpdateRequest request) {
        FeedDetailResponse response = feedService.updateFeed(userId, feedId, request);
        return ApiResponses.ok(Message.FEED_EDITED, response);
    }

    /**
     * 피드 삭제 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param feedId 삭제할 피드 ID
     * @return 삭제 성공 응답
     */
    @DeleteMapping("/v1/feeds/{feedId}")
    public ResponseEntity<ApiResponse<Void>> deleteFeed(
            @AuthenticationPrincipal Long userId, @PathVariable Long feedId) {
        feedService.deleteFeed(userId, feedId);
        return ApiResponses.ok(Message.FEED_DELETED, null);
    }

    /**
     * 피드 좋아요 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param feedId 좋아요할 피드 ID
     * @return 좋아요 응답 (좋아요 개수, 좋아요 여부)
     */
    @PostMapping("/v1/feeds/{feedId}/likes")
    public ResponseEntity<ApiResponse<LikeResponse>> likeFeed(
            @AuthenticationPrincipal Long userId, @PathVariable Long feedId) {
        LikeResponse response = feedService.likeFeed(userId, feedId);
        return ApiResponses.created(Message.FEED_LIKED, response);
    }

    /**
     * 피드 좋아요 취소 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param feedId 좋아요 취소할 피드 ID
     * @return 좋아요 응답 (좋아요 개수, 좋아요 여부)
     */
    @DeleteMapping("/v1/feeds/{feedId}/likes")
    public ResponseEntity<ApiResponse<LikeResponse>> unlikeFeed(
            @AuthenticationPrincipal Long userId, @PathVariable Long feedId) {
        LikeResponse response = feedService.unlikeFeed(userId, feedId);
        return ApiResponses.ok(Message.FEED_LIKE_CANCELLED, response);
    }

    /**
     * 피드 좋아요 사용자 목록 조회 API
     *
     * @param feedId 피드 ID
     * @param after  커서 (이전 페이지 마지막 좋아요 ID)
     * @param limit  조회 개수
     * @return 좋아요 사용자 목록 및 페이지 정보
     */
    @GetMapping("/v1/feeds/{feedId}/likes")
    public ResponseEntity<ApiResponse<PagedResponse<FeedLikeUserItem>>> getLikedUsers(
            @PathVariable Long feedId,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = LIKE_USER_LIST) int limit) {
        PagedResponse<FeedLikeUserItem> response = feedService.getLikedUsers(feedId, after, limit);
        return ApiResponses.ok(Message.FEED_LIKES_RETRIEVED, response);
    }
}
