package com.example.kloset_lab.comment.repository;

import com.example.kloset_lab.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 원댓글 목록 조회 (커서 기반 페이지네이션)
     *
     * @param feedId 피드 ID
     * @param cursor 커서 (이전 페이지 마지막 댓글 ID)
     * @param pageable 페이지 정보
     * @return 원댓글 목록
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.user "
            + "WHERE c.feed.id = :feedId AND c.parent IS NULL "
            + "AND (:cursor IS NULL OR c.id < :cursor) "
            + "ORDER BY c.id DESC")
    Slice<Comment> findParentCommentsByCursor(
            @Param("feedId") Long feedId, @Param("cursor") Long cursor, Pageable pageable);

    /**
     * 대댓글 목록 조회 (커서 기반 페이지네이션)
     *
     * @param parentId 부모 댓글 ID
     * @param cursor 커서 (이전 페이지 마지막 대댓글 ID)
     * @param pageable 페이지 정보
     * @return 대댓글 목록
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.user "
            + "WHERE c.parent.id = :parentId "
            + "AND (:cursor IS NULL OR c.id < :cursor) "
            + "ORDER BY c.id DESC")
    Slice<Comment> findRepliesByCursor(
            @Param("parentId") Long parentId, @Param("cursor") Long cursor, Pageable pageable);

    /**
     * 특정 피드의 모든 댓글 조회 (원댓글 + 대댓글 포함)
     *
     * @param feedId 피드 ID
     * @return 해당 피드의 모든 댓글
     */
    @Query("SELECT c FROM Comment c WHERE c.feed.id = :feedId")
    List<Comment> findByFeedId(@Param("feedId") Long feedId);

    @Query(
            """
        SELECT COUNT(c)
        FROM Comment c
        WHERE c.parent.id = :parentId
          AND c.deletedAt IS NULL
    """)
    long countRepliesByParentId(@Param("parentId") Long parentId);

    @Query(
            """
        SELECT c.parent.id, COUNT(c)
        FROM Comment c
        WHERE c.parent.id IN :parentIds
          AND c.deletedAt IS NULL
        GROUP BY c.parent.id
    """)
    List<Object[]> countRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    /**
     * 특정 유저의 모든 댓글을 soft delete 처리
     *
     * @param userId    유저 ID
     * @param deletedAt 삭제 시각
     */
    @Modifying
    @Query("UPDATE Comment c SET c.deletedAt = :deletedAt WHERE c.user.id = :userId AND c.deletedAt IS NULL")
    void softDeleteAllByUserId(@Param("userId") Long userId, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 특정 유저의 삭제되지 않은 모든 댓글 조회 (soft delete 전 카운트 감소용)
     *
     * @param userId 유저 ID
     * @return 댓글 목록
     */
    @Query("SELECT c FROM Comment c WHERE c.user.id = :userId AND c.deletedAt IS NULL")
    List<Comment> findAllActiveByUserId(@Param("userId") Long userId);
}
