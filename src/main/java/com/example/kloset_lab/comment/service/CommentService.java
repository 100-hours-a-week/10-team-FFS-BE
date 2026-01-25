package com.example.kloset_lab.comment.service;

import com.example.kloset_lab.comment.dto.*;
import com.example.kloset_lab.comment.entity.Comment;
import com.example.kloset_lab.comment.entity.CommentLike;
import com.example.kloset_lab.comment.repository.CommentLikeRepository;
import com.example.kloset_lab.comment.repository.CommentRepository;
import com.example.kloset_lab.feed.entity.Feed;
import com.example.kloset_lab.feed.repository.FeedRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.global.response.LikeResponse;
import com.example.kloset_lab.global.response.PageInfo;
import com.example.kloset_lab.user.dto.UserProfileDto;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.entity.UserProfile;
import com.example.kloset_lab.user.repository.UserProfileRepository;
import com.example.kloset_lab.user.repository.UserRepository;
import com.example.kloset_lab.user.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserService userService;

    /**
     * 원댓글 목록 조회
     *
     * @param userId 현재 사용자 ID
     * @param feedId 피드 ID
     * @param after 커서 (이전 페이지 마지막 댓글 ID)
     * @param limit 조회 개수
     * @return 원댓글 목록 및 페이지 정보
     */
    public CommentPagedResponse<CommentItem> getComments(Long userId, Long feedId, Long after, int limit) {
        if (!feedRepository.existsById(feedId)) {
            throw new CustomException(ErrorCode.FEED_NOT_FOUND);
        }

        Slice<Comment> commentSlice =
                commentRepository.findParentCommentsByCursor(feedId, after, PageRequest.of(0, limit));
        List<Comment> comments = commentSlice.getContent();

        return buildCommentPagedResponse(0L, comments, commentSlice.hasNext(), userId);
    }

    /**
     * 대댓글 목록 조회
     *
     * @param userId 현재 사용자 ID
     * @param feedId 피드 ID
     * @param commentId 부모 댓글 ID
     * @param after 커서 (이전 페이지 마지막 대댓글 ID)
     * @param limit 조회 개수
     * @return 대댓글 목록 및 페이지 정보
     */
    public CommentPagedResponse<CommentItem> getReplies(
            Long userId, Long feedId, Long commentId, Long after, int limit) {
        if (!feedRepository.existsById(feedId)) {
            throw new CustomException(ErrorCode.FEED_NOT_FOUND);
        }

        Comment parentComment = commentRepository
                .findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));

        if (!parentComment.getFeed().getId().equals(feedId)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (!parentComment.isParentComment()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Slice<Comment> replySlice = commentRepository.findRepliesByCursor(commentId, after, PageRequest.of(0, limit));
        List<Comment> replies = replySlice.getContent();

        return buildCommentPagedResponse(parentComment.getId(), replies, replySlice.hasNext(), userId);
    }

    /**
     * 댓글 작성
     *
     * @param userId 현재 사용자 ID
     * @param feedId 피드 ID
     * @param request 댓글 작성 요청
     * @return 생성된 댓글 정보
     */
    @Transactional
    public CommentItem createComment(Long userId, Long feedId, CommentCreateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Feed feed = feedRepository.findById(feedId).orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));

        Comment parent = Optional.ofNullable(request.parentId())
                .map(parentId -> getVerifiedParentComment(parentId, feedId))
                .orElse(null);

        Comment comment = commentRepository.save(Comment.builder()
                .user(user)
                .feed(feed)
                .parent(parent)
                .content(request.content())
                .build());

        feed.incrementCommentCount();

        return buildCommentItem(comment, false);
    }

    /**
     * 댓글 수정
     *
     * @param userId 현재 사용자 ID
     * @param feedId 피드 ID
     * @param commentId 댓글 ID
     * @param request 댓글 수정 요청
     * @return 수정된 댓글 정보
     */
    @Transactional
    public CommentItem updateComment(Long userId, Long feedId, Long commentId, CommentUpdateRequest request) {
        Comment comment = getVerifiedComment(feedId, commentId);

        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.COMMENT_EDIT_DENIED);
        }

        comment.updateContent(request.content());

        boolean isLiked = commentLikeRepository
                .findByCommentIdAndUserId(commentId, userId)
                .isPresent();

        return buildCommentItem(comment, isLiked);
    }

    /**
     * 댓글 삭제
     *
     * @param userId 현재 사용자 ID
     * @param feedId 피드 ID
     * @param commentId 댓글 ID
     */
    @Transactional
    public void deleteComment(Long userId, Long feedId, Long commentId) {
        Comment comment = getVerifiedComment(feedId, commentId);

        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.COMMENT_DELETE_DENIED);
        }

        comment.softDelete();
        comment.getFeed().decrementCommentCount();
    }

    /**
     * 댓글 좋아요
     *
     * @param userId 현재 사용자 ID
     * @param feedId 피드 ID
     * @param commentId 댓글 ID
     * @return 좋아요 응답
     */
    @Transactional
    public LikeResponse likeComment(Long userId, Long feedId, Long commentId) {
        Comment comment = getVerifiedComment(feedId, commentId);

        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean alreadyLiked = commentLikeRepository
                .findByCommentIdAndUserId(commentId, userId)
                .isPresent();

        if (!alreadyLiked) {
            commentLikeRepository.save(
                    CommentLike.builder().comment(comment).user(user).build());
            comment.incrementLikeCount();
        }

        return LikeResponse.builder()
                .likeCount(comment.getLikeCount())
                .isLiked(true)
                .build();
    }

    /**
     * 댓글 좋아요 취소
     *
     * @param userId 현재 사용자 ID
     * @param feedId 피드 ID
     * @param commentId 댓글 ID
     * @return 좋아요 응답
     */
    @Transactional
    public LikeResponse unlikeComment(Long userId, Long feedId, Long commentId) {
        Comment comment = getVerifiedComment(feedId, commentId);

        Optional<CommentLike> existingLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);

        if (existingLike.isPresent()) {
            commentLikeRepository.delete(existingLike.get());
            comment.decrementLikeCount();
        }

        return LikeResponse.builder()
                .likeCount(comment.getLikeCount())
                .isLiked(false)
                .build();
    }


    /**
     * 댓글 조회 및 피드 소속 검증
     *
     * @param feedId 피드 ID
     * @param commentId 댓글 ID
     * @return 검증된 댓글
     */
    private Comment getVerifiedComment(Long feedId, Long commentId) {
        if (!feedRepository.existsById(feedId)) {
            throw new CustomException(ErrorCode.FEED_NOT_FOUND);
        }

        Comment comment = commentRepository
                .findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getFeed().getId().equals(feedId)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return comment;
    }

    /**
     * 댓글 페이징 응답 생성
     */
    private CommentPagedResponse<CommentItem> buildCommentPagedResponse(
            Long parentId, List<Comment> comments, boolean hasNext, Long currentUserId) {

        List<Long> commentIds = comments.stream().map(Comment::getId).toList();

        List<Long> userIds =
                comments.stream().map(c -> c.getUser().getId()).distinct().toList();
        Map<Long, UserProfile> userProfileMap = userProfileRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(up -> up.getUser().getId(), Function.identity()));

        Set<Long> likedCommentIds = commentLikeRepository.findByCommentIdInAndUserId(commentIds, currentUserId).stream()
                .map(cl -> cl.getComment().getId())
                .collect(Collectors.toSet());

        List<CommentItem> items = comments.stream()
                .map(comment -> {
                    boolean isLiked = likedCommentIds.contains(comment.getId());
                    boolean isOwner = comment.getUser().getId().equals(currentUserId);
                    return buildCommentItemWithProfile(comment, userProfileMap, isLiked, isOwner);
                })
                .toList();

        Long nextCursor = hasNext ? comments.getLast().getId() : null;
        PageInfo pageInfo = new PageInfo(hasNext, nextCursor);

        return new CommentPagedResponse<>(parentId, items, pageInfo);
    }

    /**
     * 댓글 아이템 생성 (단건 조회용)
     */
    private CommentItem buildCommentItem(Comment comment, boolean isLiked) {
        UserProfileDto userProfileDto =
                userService.buildUserProfileDto(comment.getUser().getId());

        return CommentItem.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .modifiedAt(comment.getUpdatedAt())
                .userProfile(userProfileDto)
                .isLiked(isLiked)
                .isOwner(true)
                .build();
    }

    /**
     * 댓글 아이템 생성 (목록 조회용, 프로필 맵 사용)
     */
    private CommentItem buildCommentItemWithProfile(
            Comment comment, Map<Long, UserProfile> userProfileMap, boolean isLiked, boolean isOwner) {
        UserProfileDto userProfileDto =
                userService.buildUserProfileDto(comment.getUser().getId(), userProfileMap);

        return CommentItem.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .modifiedAt(comment.getUpdatedAt())
                .userProfile(userProfileDto)
                .isLiked(isLiked)
                .isOwner(isOwner)
                .build();
    }

    /**
     * 부모 댓글 조회 및 검증
     *
     * @param parentId 부모 댓글 ID
     * @param feedId 피드 ID
     * @return 검증된 부모 댓글
     */
    private Comment getVerifiedParentComment(Long parentId, Long feedId) {
        Comment parentComment = commentRepository
                .findById(parentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));

        if (!parentComment.getFeed().getId().equals(feedId)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (!parentComment.isParentComment()) {
            throw new CustomException(ErrorCode.REPLY_TO_REPLY_NOT_ALLOWED);
        }

        return parentComment;
    }
}
