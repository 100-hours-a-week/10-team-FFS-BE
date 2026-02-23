package com.example.kloset_lab.follow.controller;

import static com.example.kloset_lab.global.response.Message.*;

import com.example.kloset_lab.follow.dto.FollowUserItem;
import com.example.kloset_lab.follow.service.FollowService;
import com.example.kloset_lab.global.response.ApiResponse;
import com.example.kloset_lab.global.response.ApiResponses;
import com.example.kloset_lab.global.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    /**
     * 팔로우
     * POST /api/v2/users/{userId}/follows
     */
    @PostMapping("/{userId}/follows")
    public ResponseEntity<ApiResponse<Void>> follow(
            @AuthenticationPrincipal Long currentUserId, @PathVariable Long userId) {
        followService.follow(currentUserId, userId);
        return ApiResponses.created(FOLLOW_CREATED);
    }

    /**
     * 언팔로우
     * DELETE /api/v2/users/{userId}/follows
     */
    @DeleteMapping("/{userId}/follows")
    public ResponseEntity<ApiResponse<Void>> unfollow(
            @AuthenticationPrincipal Long currentUserId, @PathVariable Long userId) {
        followService.unfollow(currentUserId, userId);
        return ApiResponses.ok(FOLLOW_DELETED);
    }

    /**
     * 팔로잉 목록 조회
     * GET /api/v2/users/{userId}/following?after=xx&limit=20&sort=timeDesc
     */
    @GetMapping("/{userId}/following")
    public ResponseEntity<ApiResponse<PagedResponse<FollowUserItem>>> getFollowings(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long userId,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "timeDesc") String sort) {
        PagedResponse<FollowUserItem> response = followService.getFollowings(userId, currentUserId, after, limit, sort);
        return ApiResponses.ok(FOLLOWING_RETRIEVED, response);
    }

    /**
     * 팔로워 목록 조회
     * GET /api/v2/users/{userId}/followers?after=xx&limit=20&sort=timeAsc
     */
    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<PagedResponse<FollowUserItem>>> getFollowers(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long userId,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "timeDesc") String sort) {
        PagedResponse<FollowUserItem> response = followService.getFollowers(userId, currentUserId, after, limit, sort);
        return ApiResponses.ok(FOLLOWER_RETRIEVED, response);
    }
}
