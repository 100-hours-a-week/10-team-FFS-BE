package com.example.kloset_lab.comment.repository;

import com.example.kloset_lab.comment.entity.CommentLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    /**
     * @param commentId 댓글 ID
     * @param userId 사용자 ID
     * @return 좋아요 정보
     */
    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    /**
     * @param commentIds 댓글 ID 목록
     * @param userId 사용자 ID
     * @return 좋아요 목록
     */
    List<CommentLike> findByCommentIdInAndUserId(List<Long> commentIds, Long userId);
}
