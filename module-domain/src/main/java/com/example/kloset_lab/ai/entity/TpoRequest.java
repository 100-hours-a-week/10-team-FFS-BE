package com.example.kloset_lab.ai.entity;

import com.example.kloset_lab.global.entity.BaseTimeEntity;
import com.example.kloset_lab.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tpo_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TpoRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tpo_session_id")
    private TpoSession tpoSession;

    @Column(name = "request_id", unique = true)
    private String requestId;

    @Column(name = "turn_no")
    private Integer turnNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(20)")
    private TpoRequestStatus status;

    @Column(name = "request_text", nullable = false)
    private String requestText;

    @Column(name = "request_count", nullable = false)
    private Integer requestCount;

    @Column(name = "query_summary")
    private String querySummary;

    @Builder
    public TpoRequest(User user, String requestText, String querySummary) {
        this.user = user;
        this.requestText = requestText;
        this.requestCount = 0;
        this.querySummary = querySummary;
    }

    /**
     * 비동기 Kafka 요청용 생성자
     *
     * @param user 사용자
     * @param tpoSession 세션
     * @param requestId 요청 추적 ID (UUID)
     * @param turnNo 턴 번호
     * @param requestText 사용자 요청 텍스트
     */
    public TpoRequest(User user, TpoSession tpoSession, String requestId, int turnNo, String requestText) {
        this.user = user;
        this.tpoSession = tpoSession;
        this.requestId = requestId;
        this.turnNo = turnNo;
        this.requestText = requestText;
        this.requestCount = 0;
        this.status = TpoRequestStatus.PENDING;
    }

    public void addQuerySummary(String querySummary) {
        this.querySummary = querySummary;
    }

    public void complete() {
        this.status = TpoRequestStatus.COMPLETED;
    }

    public void fail() {
        this.status = TpoRequestStatus.FAILED;
    }

    public boolean isCompleted() {
        return this.status == TpoRequestStatus.COMPLETED;
    }
}
