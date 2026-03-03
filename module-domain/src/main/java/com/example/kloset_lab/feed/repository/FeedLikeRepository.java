package com.example.kloset_lab.feed.repository;

import com.example.kloset_lab.feed.entity.FeedLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    Optional<FeedLike> findByFeedIdAndUserId(Long feedId, Long userId);

    /**
     * 특정 유저가 누른 모든 피드 좋아요 조회 (삭제되지 않은 피드만)
     *
     * @param userId 유저 ID
     * @return 피드 좋아요 목록
     */
    @Query("SELECT fl FROM FeedLike fl WHERE fl.user.id = :userId AND fl.feed.deletedAt IS NULL")
    List<FeedLike> findAllActiveByUserId(Long userId);

    List<FeedLike> findByFeedIdInAndUserId(List<Long> feedIds, Long userId);

    /**
     * 피드 좋아요 목록 커서 기반 조회 (최신순)
     *
     * @param feedId   피드 ID
     * @param cursor   커서 (이전 페이지 마지막 좋아요 ID), null이면 처음부터
     * @param pageable 페이징 정보
     * @return 좋아요 Slice
     */
    @Query(
            "SELECT fl FROM FeedLike fl JOIN FETCH fl.user WHERE fl.feed.id = :feedId AND (:cursor IS NULL OR fl.id < :cursor) ORDER BY fl.id DESC")
    Slice<FeedLike> findByFeedIdWithCursor(
            @Param("feedId") Long feedId, @Param("cursor") Long cursor, Pageable pageable);
}
