package com.example.kloset_lab.ai.repository;

import com.example.kloset_lab.ai.entity.TpoResult;
import com.example.kloset_lab.ai.entity.TpoResultClothes;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TpoResultClothesRepository extends JpaRepository<TpoResultClothes, Long> {

    List<TpoResultClothes> findByTpoResultIn(List<TpoResult> tpoResults);

    /**
     * 여러 결과에 매핑된 옷을 파일 정보까지 fetch join하여 일괄 조회 (이미지 URL 생성용)
     *
     * @param tpoResults TpoResult 목록
     * @return TpoResultClothes 목록 (clothes.file 페치 조인)
     */
    @Query("SELECT rc FROM TpoResultClothes rc "
            + "JOIN FETCH rc.clothes c "
            + "JOIN FETCH c.file "
            + "WHERE rc.tpoResult IN :results")
    List<TpoResultClothes> findByTpoResultInWithClothes(@Param("results") List<TpoResult> results);
}
