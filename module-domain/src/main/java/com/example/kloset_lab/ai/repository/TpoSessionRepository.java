package com.example.kloset_lab.ai.repository;

import com.example.kloset_lab.ai.entity.TpoSession;
import com.example.kloset_lab.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TpoSessionRepository extends JpaRepository<TpoSession, Long> {

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

    /**
     * 사용자의 세션 목록을 최근 활동순으로 조회한다. (Slice 기반, count 쿼리 없음)
     *
     * @param user 사용자
     * @param pageable 페이징
     * @return 세션 슬라이스 (최신 활동 우선)
     */
    Slice<TpoSession> findByUserOrderByLastActivityAtDesc(User user, Pageable pageable);
}
