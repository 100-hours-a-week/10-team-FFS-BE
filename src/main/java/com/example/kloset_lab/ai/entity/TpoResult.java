package com.example.kloset_lab.ai.entity;

import com.example.kloset_lab.global.entity.BaseTimeEntity;
import com.example.kloset_lab.media.entity.MediaFile;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private MediaFile file;

    @Column(name = "cordi_explain_text", nullable = false, columnDefinition = "varchar(255)")
    private String cordiExplainText;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction", nullable = false, columnDefinition = "varchar(10)")
    private Reaction reaction;

    @Builder
    public TpoResult(TpoRequest tpoRequest, MediaFile file, String cordiExplainText) {
        this.tpoRequest = tpoRequest;
        this.file = file;
        this.cordiExplainText = cordiExplainText;
        this.reaction = Reaction.NONE;
    }

    public void updateReaction(Reaction reaction) {
        this.reaction = reaction;
    }
}
