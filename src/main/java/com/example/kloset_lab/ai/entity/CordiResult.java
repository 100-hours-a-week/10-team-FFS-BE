package com.example.kloset_lab.ai.entity;

import com.example.kloset_lab.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cordi_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CordiResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cordi_request_id", nullable = false)
    private CordiRequest cordiRequest;

    @Column(name = "query_summary", nullable = false, columnDefinition = "varchar(255)")
    private String querySummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction", nullable = false, columnDefinition = "varchar(10)")
    private Reaction reaction;

    @Builder
    public CordiResult(CordiRequest cordiRequest, String querySummary) {
        this.cordiRequest = cordiRequest;
        this.querySummary = querySummary;
        this.reaction = Reaction.NONE;
    }

    /** 사용자 반응 업데이트 */
    public void updateReaction(Reaction reaction) {
        this.reaction = reaction;
    }
}
