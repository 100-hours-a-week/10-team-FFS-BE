package com.example.kloset_lab.clothes.repository;

import com.example.kloset_lab.clothes.entity.AnalyzeBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalyzeBatchRepository extends JpaRepository<AnalyzeBatch, Long> {
    Optional<AnalyzeBatch> findByBatchId(String batchId);
}
