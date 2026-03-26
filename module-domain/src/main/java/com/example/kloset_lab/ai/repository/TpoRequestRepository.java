package com.example.kloset_lab.ai.repository;

import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoSession;
import com.example.kloset_lab.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TpoRequestRepository extends JpaRepository<TpoRequest, Long> {
    List<TpoRequest> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Optional<TpoRequest> findByRequestId(String requestId);

    /**
     * 세션 히스토리 조회 (Internal API용): turnNo ≤ uptoTurnNo, 최신 턴부터 내림차순
     *
     * @param sessionId 세션 ID
     * @param userId 사용자 ID
     * @param uptoTurnNo 조회 상한 턴 번호
     * @param pageable 페이징 (limit)
     * @return TpoRequest 목록 (최신 턴 우선)
     */
    @Query("SELECT r FROM TpoRequest r "
            + "WHERE r.tpoSession.sessionId = :sessionId "
            + "AND r.user.id = :userId "
            + "AND r.turnNo <= :uptoTurnNo "
            + "ORDER BY r.turnNo DESC")
    List<TpoRequest> findSessionHistory(
            @Param("sessionId") String sessionId,
            @Param("userId") Long userId,
            @Param("uptoTurnNo") int uptoTurnNo,
            Pageable pageable);

    /**
     * 세션 상세 조회 (Public API용): 최신 턴부터 내림차순, Slice 기반 페이징
     *
     * @param sessionId 세션 ID
     * @param userId 사용자 ID
     * @param pageable 페이징
     * @return TpoRequest 슬라이스 (최신 턴 우선)
     */
    @Query("SELECT r FROM TpoRequest r "
            + "WHERE r.tpoSession.sessionId = :sessionId "
            + "AND r.user.id = :userId "
            + "ORDER BY r.turnNo DESC")
    Slice<TpoRequest> findSessionDetail(
            @Param("sessionId") String sessionId, @Param("userId") Long userId, Pageable pageable);

    /**
     * 세션 목록에서 첫 번째 턴 요청을 배치로 조회한다. (N+1 방지)
     *
     * @param sessions 세션 목록
     * @param turnNo 턴 번호 (일반적으로 1)
     * @return 첫 번째 턴 요청 목록
     */
    List<TpoRequest> findByTpoSessionInAndTurnNo(List<TpoSession> sessions, int turnNo);
}
