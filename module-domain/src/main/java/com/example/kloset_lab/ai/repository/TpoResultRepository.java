package com.example.kloset_lab.ai.repository;

import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoResult;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
     * tpoResult + tpoRequest + user + tpoSession을 단일 쿼리로 조회 (TX3 피드백 검증용)
     *
     * <p>비관적 락(FOR UPDATE)으로 동시 리액션 등록 경쟁 조건을 방지한다.
     *
     * @param id tpoResult PK
     * @return TpoResult (user, tpoSession까지 페치)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tr FROM TpoResult tr "
            + "JOIN FETCH tr.tpoRequest req "
            + "JOIN FETCH req.user "
            + "LEFT JOIN FETCH req.tpoSession "
            + "WHERE tr.id = :id")
    Optional<TpoResult> findByIdWithSession(@Param("id") Long id);

    /**
     * 여러 TpoRequest에 속한 결과를 일괄 조회 (N+1 방지)
     *
     * @param tpoRequests TpoRequest 목록
     * @return TpoResult 목록
     */
    List<TpoResult> findByTpoRequestIn(List<TpoRequest> tpoRequests);

    /**
     * 여러 결과 ID로 일괄 조회 + 소유자 검증용 user 페치 (옷 상세 조회 API용)
     *
     * @param ids TpoResult PK 목록
     * @return TpoResult 목록 (tpoRequest.user 페치 조인)
     */
    @Query("SELECT tr FROM TpoResult tr "
            + "JOIN FETCH tr.tpoRequest req "
            + "JOIN FETCH req.user "
            + "WHERE tr.id IN :ids")
    List<TpoResult> findAllByIdWithUser(@Param("ids") List<Long> ids);
}
