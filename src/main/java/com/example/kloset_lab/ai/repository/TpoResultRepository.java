package com.example.kloset_lab.ai.repository;

import com.example.kloset_lab.ai.entity.TpoResult;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TpoResultRepository extends JpaRepository<TpoResult, Long> {

    /**
     * tpoResult + tpoRequest + user를 단일 쿼리로 조회 (소유권 검증용)
     *
     * @param id tpoResult PK
     * @return TpoResult (user까지 페치)
     */
    @Query("SELECT tr FROM TpoResult tr "
            + "JOIN FETCH tr.tpoRequest req "
            + "JOIN FETCH req.user "
            + "WHERE tr.id = :id")
    Optional<TpoResult> findByIdWithUser(@Param("id") Long id);
}
