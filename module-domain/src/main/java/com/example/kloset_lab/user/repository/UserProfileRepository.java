package com.example.kloset_lab.user.repository;

import com.example.kloset_lab.user.entity.UserProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    boolean existsByNickname(String nickname);

    Optional<UserProfile> findByUserId(Long userId);

    List<UserProfile> findByUserIdIn(List<Long> userIds);

    @Query("SELECT up FROM UserProfile up JOIN up.user u WHERE up.nickname LIKE %:nickname% AND u.deletedAt IS NULL")
    List<UserProfile> findByNicknameContainingAndUserNotDeleted(@Param("nickname") String nickname);
}
