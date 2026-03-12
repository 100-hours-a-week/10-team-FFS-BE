package com.example.kloset_lab.ai.entity;

import com.example.kloset_lab.global.entity.BaseTimeEntity;
import com.example.kloset_lab.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tpo_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TpoSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_turn_no", nullable = false)
    private int lastTurnNo;

    @Column(name = "inflight_request_id")
    private String inflightRequestId;

    @Column(name = "inflight_started_at")
    private LocalDateTime inflightStartedAt;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    @Builder
    public TpoSession(User user) {
        this.sessionId = UUID.randomUUID().toString();
        this.user = user;
        this.lastTurnNo = 0;
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * 턴 번호를 증가시키고 반환한다.
     *
     * @return 증가된 턴 번호
     */
    public int nextTurn() {
        this.lastTurnNo++;
        return this.lastTurnNo;
    }

    /**
     * 인플라이트 상태로 전환한다.
     *
     * @param requestId 현재 처리 중인 요청 ID
     */
    public void startInflight(String requestId) {
        this.inflightRequestId = requestId;
        this.inflightStartedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * 인플라이트 상태를 해제한다. (requestId 일치 시에만)
     *
     * <p>지연/중복 응답이 현재 진행 중인 다른 요청의 inflight를 잘못 해제하는 것을 방지한다.
     *
     * @param requestId 해제 대상 요청 ID
     */
    public void clearInflight(String requestId) {
        if (this.inflightRequestId != null && this.inflightRequestId.equals(requestId)) {
            this.inflightRequestId = null;
            this.inflightStartedAt = null;
            this.lastActivityAt = LocalDateTime.now();
        }
    }

    /**
     * 현재 인플라이트 상태인지 확인한다.
     *
     * @return 인플라이트 여부
     */
    public boolean isInflight() {
        return this.inflightRequestId != null;
    }
}
