package com.example.kloset_lab.ai.repository;

import com.example.kloset_lab.ai.entity.CordiResult;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CordiResultRepository extends JpaRepository<CordiResult, Long> {

    /**
     * cordiResult + cordiRequest + user를 단일 쿼리로 조회 (소유권 검증용)
     *
     * @param id cordiResult PK
     * @return CordiResult (user까지 페치)
     */
    @Query("SELECT cr FROM CordiResult cr "
            + "JOIN FETCH cr.cordiRequest req "
            + "JOIN FETCH req.user "
            + "WHERE cr.id = :id")
    Optional<CordiResult> findByIdWithUser(@Param("id") Long id);
}
