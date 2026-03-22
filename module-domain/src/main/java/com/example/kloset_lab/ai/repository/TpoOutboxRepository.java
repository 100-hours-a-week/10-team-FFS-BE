package com.example.kloset_lab.ai.repository;

import com.example.kloset_lab.ai.entity.TpoOutbox;
import com.example.kloset_lab.ai.entity.TpoOutboxStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TpoOutboxRepository extends JpaRepository<TpoOutbox, Long> {

    List<TpoOutbox> findTop100ByStatusOrderByCreatedAtAsc(TpoOutboxStatus status);
}
