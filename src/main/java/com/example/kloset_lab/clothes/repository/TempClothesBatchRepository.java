package com.example.kloset_lab.clothes.repository;

import com.example.kloset_lab.clothes.entity.TempClothesBatch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TempClothesBatchRepository extends JpaRepository<TempClothesBatch, Long> {
    Optional<TempClothesBatch> findByBatchId(String batchId);

    @Query("SELECT b FROM TempClothesBatch b " + "LEFT JOIN FETCH b.tasks " + "WHERE b.batchId = :batchId")
    Optional<TempClothesBatch> findByBatchIdWithTasks(@Param("batchId") String batchId);
}
