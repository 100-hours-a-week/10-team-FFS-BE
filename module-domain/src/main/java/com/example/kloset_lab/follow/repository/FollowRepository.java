package com.example.kloset_lab.follow.repository;

import com.example.kloset_lab.follow.entity.Follow;
import com.example.kloset_lab.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerAndFollowee(User follower, User followeeId);

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    Optional<Follow> findByFollowerAndFollowee(User follower, User following);

    @Query("SELECT f FROM Follow f " + "JOIN FETCH f.followee "
            + "WHERE f.follower = :user AND (:cursor IS NULL OR f.id < :cursor) "
            + "ORDER BY f.id DESC")
    List<Follow> findFollowingsByUserDesc(@Param("user") User user, @Param("cursor") Long cursor);

    @Query("SELECT f FROM Follow f " + "JOIN FETCH f.followee "
            + "WHERE f.follower = :user AND (:cursor IS NULL OR f.id > :cursor) "
            + "ORDER BY f.id ASC")
    List<Follow> findFollowingsByUserAsc(@Param("user") User user, @Param("cursor") Long cursor);

    // =============================================
    // 팔로워 목록 (나를 팔로우하는 사람들) - id 기반 커서
    // =============================================

    @Query("SELECT f FROM Follow f " + "JOIN FETCH f.follower "
            + "WHERE f.followee = :user AND (:cursor IS NULL OR f.id < :cursor) "
            + "ORDER BY f.id DESC")
    List<Follow> findFollowersByUserDesc(@Param("user") User user, @Param("cursor") Long cursor);

    @Query("SELECT f FROM Follow f " + "JOIN FETCH f.follower "
            + "WHERE f.followee = :user AND (:cursor IS NULL OR f.id > :cursor) "
            + "ORDER BY f.id ASC")
    List<Follow> findFollowersByUserAsc(@Param("user") User user, @Param("cursor") Long cursor);

    long countByFollowee(User followee);

    long countByFollower(User follower);
}
