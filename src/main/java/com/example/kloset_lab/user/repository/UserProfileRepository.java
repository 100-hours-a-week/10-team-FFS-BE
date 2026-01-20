package com.example.kloset_lab.user.repository;

import com.example.kloset_lab.user.entity.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    boolean existsByNickname(String nickname);

    Optional<UserProfile> findByUserId(Long userId);
}
