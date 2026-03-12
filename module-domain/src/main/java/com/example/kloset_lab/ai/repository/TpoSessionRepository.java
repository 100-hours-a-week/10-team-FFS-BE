package com.example.kloset_lab.ai.repository;

import com.example.kloset_lab.ai.entity.TpoSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TpoSessionRepository extends JpaRepository<TpoSession, Long> {

    Optional<TpoSession> findBySessionId(String sessionId);

    /**
     * 비관적 락(FOR UPDATE)으로 세션을 조회한다.
     * TX1(요청수락), TX2(피드백), TX3(응답처리)에서 동시성 제어에 사용.
     *
     * @param sessionId 세션 ID
     * @return TpoSession (locked)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TpoSession s WHERE s.sessionId = :sessionId")
    Optional<TpoSession> findBySessionIdForUpdate(@Param("sessionId") String sessionId);
}
