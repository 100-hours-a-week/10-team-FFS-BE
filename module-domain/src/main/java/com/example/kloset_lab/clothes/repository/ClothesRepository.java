package com.example.kloset_lab.clothes.repository;

import com.example.kloset_lab.clothes.entity.Category;
import com.example.kloset_lab.clothes.entity.Clothes;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClothesRepository extends JpaRepository<Clothes, Long> {

    /**
     * 사용자의 옷 개수를 조회
     *
     * @param userId 사용자 ID
     * @return 옷 개수
     */
    long countByUserId(Long userId);

    /**
     * 사용자의 옷 목록을 커서 기반으로 조회
     *
     * @param userId 사용자 ID
     * @param cursor 커서 (마지막 옷 ID)
     * @param pageable 페이지 정보
     * @return 옷 목록
     */
    @Query(
            """
        SELECT c
        FROM Clothes c
        JOIN FETCH c.user u
        WHERE (u.id = :userId)
          AND (:cursor IS NULL OR c.id < :cursor)
        ORDER BY c.id DESC
    """)
    Slice<Clothes> findByCursor(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    /**
     * 사용자의 옷 목록을 카테고리 필터링하여 커서 기반으로 조회
     *
     * @param userId 사용자 ID
     * @param category 카테고리
     * @param cursor 커서 (마지막 옷 ID)
     * @param pageable 페이지 정보
     * @return 옷 목록
     */
    @Query(
            """
        SELECT c
        FROM Clothes c
        JOIN FETCH c.user u
        WHERE (u.id = :userId)
          AND (c.category = :category)
          AND (:cursor IS NULL OR c.id < :cursor)
        ORDER BY c.id DESC
    """)
    Slice<Clothes> findByCursorAndCategory(
            @Param("userId") Long userId,
            @Param("category") Category category,
            @Param("cursor") Long cursor,
            Pageable pageable);

    /**
     * 특정 유저의 모든 옷을 soft delete 처리
     *
     * @param userId 유저 ID
     * @param deletedAt 삭제 시각
     */
    @Modifying
    @Query("UPDATE Clothes c SET c.deletedAt = :deletedAt WHERE c.user.id = :userId AND c.deletedAt IS NULL")
    void softDeleteAllByUserId(@Param("userId") Long userId, @Param("deletedAt") LocalDateTime deletedAt);
}
