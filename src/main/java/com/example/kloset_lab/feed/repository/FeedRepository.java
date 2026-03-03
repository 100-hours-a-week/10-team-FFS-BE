package com.example.kloset_lab.feed.repository;

import com.example.kloset_lab.feed.entity.Feed;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    /**
     * 커서 기반 피드 목록 조회 (최신순, 무한스크롤용)
     *
     * @param cursor   커서 (이전 페이지 마지막 피드 ID), null이면 처음부터
     * @param pageable 페이징 정보
     * @return 피드 Slice (hasNext 자동 계산)
     */
    @Query("SELECT f FROM Feed f JOIN FETCH f.user WHERE (:cursor IS NULL OR f.id < :cursor) ORDER BY f.id DESC")
    Slice<Feed> findByCursor(@Param("cursor") Long cursor, Pageable pageable);

    /**
     * 특정 유저의 피드 커서 기반 조회 (최신순)
     *
     * @param userId   대상 사용자 ID
     * @param cursor   커서 (이전 페이지 마지막 피드 ID), null이면 처음부터
     * @param pageable 페이징 정보
     * @return 피드 Slice
     */
    @Query(
            """
            SELECT f FROM Feed f
            JOIN FETCH f.user
            WHERE f.user.id = :userId
              AND (:cursor IS NULL OR f.id < :cursor)
            ORDER BY f.id DESC
            """)
    Slice<Feed> findByUserIdAndCursor(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    /**
     * 특정 유저의 모든 피드를 soft delete 처리
     *
     * @param userId 유저 ID
     * @param deletedAt 삭제 시각
     */
    @Modifying
    @Query("UPDATE Feed f SET f.deletedAt = :deletedAt WHERE f.user.id = :userId AND f.deletedAt IS NULL")
    void softDeleteAllByUserId(@Param("userId") Long userId, @Param("deletedAt") LocalDateTime deletedAt);

    @Query("SELECT f FROM Feed f " + "WHERE (f.user.id IN ("
            + "  SELECT fo.followee.id FROM Follow fo WHERE fo.follower.id = :userId"
            + ") OR f.user.id = :userId) "
            + "AND f.deletedAt IS NULL "
            + "AND (:cursor IS NULL OR f.id < :cursor) "
            + "ORDER BY f.id DESC")
    Slice<Feed> findFollowingFeedsByCursor(
            @Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT f FROM Feed f " +
            "JOIN FeedClothesMapping m ON m.feed.id = f.id " +
            "WHERE m.clothes.id = :clothesId " +
            "AND (:after IS NULL OR f.id < :after) " +
            "ORDER BY f.id DESC")
    Slice<Feed> findByClothesIdByCursor(
            @Param("clothesId") Long clothesId,
            @Param("after") Long after,
            Pageable pageable);
}
