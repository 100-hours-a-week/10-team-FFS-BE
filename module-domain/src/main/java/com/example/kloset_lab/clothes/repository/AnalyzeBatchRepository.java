package com.example.kloset_lab.clothes.repository;

import com.example.kloset_lab.clothes.entity.AnalyzeBatch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyzeBatchRepository extends JpaRepository<AnalyzeBatch, Long> {
    Optional<AnalyzeBatch> findByBatchId(String batchId);
}
