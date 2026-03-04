package com.example.kloset_lab.clothes.repository;

import com.example.kloset_lab.clothes.entity.AnalyzeBatch;
import com.example.kloset_lab.clothes.entity.AnalyzeSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalyzeSourceRepository extends JpaRepository<AnalyzeSource, Long> {
    Optional<AnalyzeSource> findBySourceId(String sourceId);

    @Query("SELECT DISTINCT s FROM AnalyzeSource s " + "LEFT JOIN FETCH s.batch " + "WHERE s.batch = :batch")
    List<AnalyzeSource> findAllByBatchWithBatch(@Param("batch") AnalyzeBatch batch);
}
