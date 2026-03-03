package com.example.kloset_lab.ai.entity;

import com.example.kloset_lab.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tpo_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TpoResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tpo_request_id", nullable = false)
    private TpoRequest tpoRequest;

    @Column(name = "cordi_explain_text", nullable = false, columnDefinition = "varchar(255)")
    private String cordiExplainText;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction", nullable = false, columnDefinition = "varchar(10)")
    private Reaction reaction;

    @Column(name = "outfit_id", nullable = false)
    private String outfitId;

    @Builder
    public TpoResult(TpoRequest tpoRequest, String cordiExplainText, String outfitId) {
        this.tpoRequest = tpoRequest;
        this.cordiExplainText = cordiExplainText;
        this.reaction = Reaction.NONE;
        this.outfitId = outfitId;
    }

    public void updateReaction(Reaction reaction) {
        this.reaction = reaction;
    }
}
