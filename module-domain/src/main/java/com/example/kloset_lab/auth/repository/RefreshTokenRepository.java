package com.example.kloset_lab.auth.repository;

import com.example.kloset_lab.auth.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findById(Long id);

    Optional<RefreshToken> findByUserId(Long userId);

    void deleteById(Long id);

    void deleteByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
