package com.example.kloset_lab.ai.repository;

import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoResult;
import java.util.List;
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

    /**
     * tpoResult + tpoRequest + user + tpoSession을 단일 쿼리로 조회 (TX2 피드백 검증용)
     *
     * @param id tpoResult PK
     * @return TpoResult (user, tpoSession까지 페치)
     */
    @Query("SELECT tr FROM TpoResult tr "
            + "JOIN FETCH tr.tpoRequest req "
            + "JOIN FETCH req.user "
            + "LEFT JOIN FETCH req.tpoSession "
            + "WHERE tr.id = :id")
    Optional<TpoResult> findByIdWithSession(@Param("id") Long id);

    /**
     * 특정 TpoRequest에 속한 결과 목록 조회
     *
     * @param tpoRequest TpoRequest
     * @return TpoResult 목록
     */
    List<TpoResult> findByTpoRequest(TpoRequest tpoRequest);
}
