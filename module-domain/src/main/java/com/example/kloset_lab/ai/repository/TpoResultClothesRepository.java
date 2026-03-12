package com.example.kloset_lab.ai.repository;

import com.example.kloset_lab.ai.entity.TpoResult;
import com.example.kloset_lab.ai.entity.TpoResultClothes;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TpoResultClothesRepository extends JpaRepository<TpoResultClothes, Long> {

    List<TpoResultClothes> findByTpoResultIn(List<TpoResult> tpoResults);
}
