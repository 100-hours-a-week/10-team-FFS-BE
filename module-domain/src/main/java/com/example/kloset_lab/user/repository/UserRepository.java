package com.example.kloset_lab.user.repository;

import com.example.kloset_lab.user.entity.Provider;
import com.example.kloset_lab.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByProviderAndProviderId(Provider provider, String providerId);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
}
