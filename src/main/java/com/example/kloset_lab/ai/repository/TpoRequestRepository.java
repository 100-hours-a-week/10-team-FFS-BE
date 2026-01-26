package com.example.kloset_lab.ai.repository;

import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.user.entity.User;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TpoRequestRepository extends JpaRepository<TpoRequest, Long> {
    List<TpoRequest> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
