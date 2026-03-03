package com.example.kloset_lab.comment.controller;

import static com.example.kloset_lab.global.constants.PaginationDefaults.COMMENT_LIST;
import static com.example.kloset_lab.global.constants.PaginationDefaults.REPLY_LIST;

import com.example.kloset_lab.comment.dto.*;
import com.example.kloset_lab.comment.service.CommentService;
import com.example.kloset_lab.global.response.ApiResponse;
import com.example.kloset_lab.global.response.ApiResponses;
import com.example.kloset_lab.global.response.LikeResponse;
import com.example.kloset_lab.global.response.Message;
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
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글 작성 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param feedId 피드 ID
     * @param request 댓글 작성 요청
     * @return 생성된 댓글 정보
     */
    @PostMapping("/v1/feeds/{feedId}/comments")
    public ResponseEntity<ApiResponse<CommentItem>> createComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long feedId,
            @Valid @RequestBody CommentCreateRequest request) {
        CommentItem response = commentService.createComment(userId, feedId, request);
        return ApiResponses.created(Message.COMMENT_CREATED, response);
    }

    /**
     * 댓글 목록 조회 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param feedId 피드 ID
     * @param after 커서 (이전 페이지 마지막 댓글 ID)
     * @param limit 조회 개수
     * @return 댓글 목록 및 페이지 정보
     */
    @GetMapping("/v1/feeds/{feedId}/comments")
    public ResponseEntity<ApiResponse<CommentPagedResponse<CommentItem>>> getComments(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long feedId,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = COMMENT_LIST) int limit) {
        CommentPagedResponse<CommentItem> response = commentService.getComments(userId, feedId, after, limit);
        return ApiResponses.ok(Message.COMMENTS_RETRIEVED, response);
    }

    /**
     * 대댓글 목록 조회 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param feedId 피드 ID
     * @param commentId 부모 댓글 ID
     * @param after 커서 (이전 페이지 마지막 대댓글 ID)
     * @param limit 조회 개수
     * @return 대댓글 목록 및 페이지 정보
     */
    @GetMapping("/v1/feeds/{feedId}/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<CommentPagedResponse<CommentItem>>> getReplies(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long feedId,
            @PathVariable Long commentId,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = REPLY_LIST) int limit) {
        CommentPagedResponse<CommentItem> response = commentService.getReplies(userId, feedId, commentId, after, limit);
        return ApiResponses.ok(Message.COMMENTS_RETRIEVED, response);
    }

    /**
     * 댓글 수정 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param feedId 피드 ID
     * @param commentId 댓글 ID
     * @param request 댓글 수정 요청
     * @return 수정된 댓글 정보
     */
    @PatchMapping("/v1/feeds/{feedId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentItem>> updateComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long feedId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request) {
        CommentItem response = commentService.updateComment(userId, feedId, commentId, request);
        return ApiResponses.ok(Message.COMMENT_UPDATED, response);
    }

    /**
     * 댓글 삭제 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param feedId 피드 ID
     * @param commentId 댓글 ID
     * @return 삭제 성공 응답
     */
    @DeleteMapping("/v1/feeds/{feedId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal Long userId, @PathVariable Long feedId, @PathVariable Long commentId) {
        commentService.deleteComment(userId, feedId, commentId);
        return ApiResponses.ok(Message.COMMENT_DELETED, null);
    }

    /**
     * 댓글 좋아요 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param feedId 피드 ID
     * @param commentId 댓글 ID
     * @return 좋아요 응답
     */
    @PostMapping("/v1/feeds/{feedId}/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<LikeResponse>> likeComment(
            @AuthenticationPrincipal Long userId, @PathVariable Long feedId, @PathVariable Long commentId) {
        LikeResponse response = commentService.likeComment(userId, feedId, commentId);
        return ApiResponses.created(Message.COMMENT_LIKED, response);
    }

    /**
     * 댓글 좋아요 취소 API
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param feedId 피드 ID
     * @param commentId 댓글 ID
     * @return 좋아요 응답
     */
    @DeleteMapping("/v1/feeds/{feedId}/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<LikeResponse>> unlikeComment(
            @AuthenticationPrincipal Long userId, @PathVariable Long feedId, @PathVariable Long commentId) {
        LikeResponse response = commentService.unlikeComment(userId, feedId, commentId);
        return ApiResponses.ok(Message.COMMENT_LIKE_CANCELLED, response);
    }
}
