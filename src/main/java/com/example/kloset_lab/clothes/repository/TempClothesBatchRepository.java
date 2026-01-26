package com.example.kloset_lab.clothes.repository;

import com.example.kloset_lab.clothes.entity.TempClothesBatch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempClothesBatchRepository extends JpaRepository<TempClothesBatch, Long> {
    Optional<TempClothesBatch> findByBatchId(String batchId);
}
