package com.example.kloset_lab.clothes.repository;

import com.example.kloset_lab.clothes.entity.AnalyzeSource;
import com.example.kloset_lab.clothes.entity.AnalyzeTask;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalyzeTaskRepository extends JpaRepository<AnalyzeTask, Long> {
    Optional<AnalyzeTask> findByTaskId(String taskId);

    @Query("SELECT t FROM AnalyzeTask t " +
            "LEFT JOIN FETCH t.source " +
            "WHERE t.source IN :sources")
    List<AnalyzeTask> findAllBySourceIn(@Param("sources") List<AnalyzeSource> sources);
}
