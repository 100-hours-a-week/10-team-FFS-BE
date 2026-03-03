package com.example.kloset_lab.clothes.repository;

import com.example.kloset_lab.clothes.entity.TempClothesTask;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempClothesTaskRepository extends JpaRepository<TempClothesTask, Long> {
    Optional<TempClothesTask> findByTaskId(String s);
}
