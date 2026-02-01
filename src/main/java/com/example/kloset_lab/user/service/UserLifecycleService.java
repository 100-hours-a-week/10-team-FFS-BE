package com.example.kloset_lab.user.service;

import com.example.kloset_lab.auth.repository.RefreshTokenRepository;
import com.example.kloset_lab.clothes.repository.ClothesRepository;
import com.example.kloset_lab.comment.entity.Comment;
import com.example.kloset_lab.comment.entity.CommentLike;
import com.example.kloset_lab.comment.repository.CommentLikeRepository;
import com.example.kloset_lab.comment.repository.CommentRepository;
import com.example.kloset_lab.feed.entity.Feed;
import com.example.kloset_lab.feed.entity.FeedLike;
import com.example.kloset_lab.feed.repository.FeedLikeRepository;
import com.example.kloset_lab.feed.repository.FeedRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 생애주기 관리 서비스 (Facade)
 * 회원 탈퇴/복구 시 여러 도메인에 걸친 작업을 조율합니다.
 */
@Service
@RequiredArgsConstructor
public class UserLifecycleService {

    private final UserRepository userRepository;
    private final FeedRepository feedRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ClothesRepository clothesRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 회원 탈퇴 처리
     *
     * @param userId 탈퇴할 회원 ID
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        deleteFeedLikesAndDecrementCounts(userId);

        deleteCommentLikesAndDecrementCounts(userId);

        softDeleteCommentsAndDecrementFeedCounts(userId, now);

        feedRepository.softDeleteAllByUserId(userId, now);

        clothesRepository.softDeleteAllByUserId(userId, now);

        user.softDelete();

        refreshTokenRepository.deleteByUserId(userId);
    }

    /**
     * 피드 좋아요 삭제 및 Feed.likeCount 감소
     * <br>
     * 동시 탈퇴 상황: 데드락 방지를 위해 Feed ID 오름차순으로 정렬 후 처리
     */
    private void deleteFeedLikesAndDecrementCounts(Long userId) {
        List<FeedLike> feedLikes = feedLikeRepository.findAllByUserId(userId);

        if (feedLikes.isEmpty()) {
            return;
        }

        Map<Long, List<FeedLike>> likesByFeedId = feedLikes.stream()
                .collect(Collectors.groupingBy(fl -> fl.getFeed().getId()));

        List<Long> sortedFeedIds = likesByFeedId.keySet().stream().sorted().toList();

        for (Long feedId : sortedFeedIds) {
            Feed feed = likesByFeedId.get(feedId).getFirst().getFeed();
            feed.decrementLikeCount();
        }

        feedLikeRepository.deleteAll(feedLikes);
    }

    /**
     * 댓글 좋아요 삭제 및 Comment.likeCount 감소
     * 데드락 방지를 위해 Comment ID 오름차순으로 정렬 후 처리
     */
    private void deleteCommentLikesAndDecrementCounts(Long userId) {
        List<CommentLike> commentLikes = commentLikeRepository.findAllByUserId(userId);

        if (commentLikes.isEmpty()) {
            return;
        }

        Map<Long, List<CommentLike>> likesByCommentId = commentLikes.stream()
                .collect(Collectors.groupingBy(cl -> cl.getComment().getId()));

        List<Long> sortedCommentIds =
                likesByCommentId.keySet().stream().sorted().toList();

        for (Long commentId : sortedCommentIds) {
            Comment comment = likesByCommentId.get(commentId).getFirst().getComment();
            comment.decrementLikeCount();
        }

        commentLikeRepository.deleteAll(commentLikes);
    }

    /**
     * 댓글 soft delete 및 Feed.commentCount 감소
     * 데드락 방지를 위해 Feed ID 오름차순으로 정렬 후 처리
     */
    private void softDeleteCommentsAndDecrementFeedCounts(Long userId, LocalDateTime deletedAt) {
        List<Comment> comments = commentRepository.findAllActiveByUserId(userId);

        if (comments.isEmpty()) {
            return;
        }

        Map<Long, List<Comment>> commentsByFeedId =
                comments.stream().collect(Collectors.groupingBy(c -> c.getFeed().getId()));

        List<Long> sortedFeedIds = commentsByFeedId.keySet().stream().sorted().toList();

        for (Long feedId : sortedFeedIds) {
            List<Comment> feedComments = commentsByFeedId.get(feedId);
            Feed feed = feedComments.getFirst().getFeed();

            for (int i = 0; i < feedComments.size(); i++) {
                feed.decrementCommentCount();
            }
        }

        commentRepository.softDeleteAllByUserId(userId, deletedAt);
    }
}
