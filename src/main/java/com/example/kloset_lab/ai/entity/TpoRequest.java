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

    @Column(name = "request_text", nullable = false)
    private String requestText;

    @Column(name = "request_count", nullable = false)
    private Integer requestCount;

    @Builder
    public TpoRequest(User user, String requestText) {
        this.user = user;
        this.requestText = requestText;
        this.requestCount = 0;
    }
}
