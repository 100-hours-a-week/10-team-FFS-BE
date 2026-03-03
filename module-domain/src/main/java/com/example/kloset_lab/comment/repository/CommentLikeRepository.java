package com.example.kloset_lab.comment.repository;

import com.example.kloset_lab.comment.entity.CommentLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    /**
     * @param commentId 댓글 ID
     * @param userId 사용자 ID
     * @return 좋아요 정보
     */
    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    /**
     * 특정 유저가 누른 모든 댓글 좋아요 조회 (삭제되지 않은 댓글만)
     *
     * @param userId 유저 ID
     * @return 댓글 좋아요 목록
     */
    @Query("SELECT cl FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.deletedAt IS NULL")
    List<CommentLike> findAllActiveByUserId(Long userId);

    /**
     * @param commentIds 댓글 ID 목록
     * @param userId 사용자 ID
     * @return 좋아요 목록
     */
    List<CommentLike> findByCommentIdInAndUserId(List<Long> commentIds, Long userId);
}
