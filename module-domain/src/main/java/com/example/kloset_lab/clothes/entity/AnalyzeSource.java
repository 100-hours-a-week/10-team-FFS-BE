package com.example.kloset_lab.clothes.entity;

import com.example.kloset_lab.global.entity.BaseTimeEntity;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "analyze_source")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalyzeSource extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id", nullable = false, unique = true, length = 26)
    private String sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private AnalyzeBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(30)")
    private SourceStatus status;

    @Column(name = "passed")
    private Boolean passed;

    @Column(name = "detected_count")
    private Integer detectedCount;

    @Column(name = "completed_count")
    private Integer completedCount;

    public AnalyzeSource(AnalyzeBatch batch) {
        this.sourceId = UlidCreator.getMonotonicUlid().toString();
        this.batch = batch;
        this.status = SourceStatus.ACCEPTED;
    }

    public void completeAbuseCheck(boolean passed) {
        this.status = SourceStatus.ABUSING_COMPLETED;
        this.passed = passed;
        if (!passed) {
            this.completedCount++;
            if (this.detectedCount.equals(this.completedCount)) {
                this.batch.incrementCompleted();
            }
        }
    }

    public void completePreprocessing(int detectedCount) {
        this.status = SourceStatus.PREPROCESSING_COMPLETED;
        this.detectedCount = detectedCount;
        this.completedCount = 0;
    }

    public void incrementTaskCompleted() {
        this.completedCount++;
        if (this.detectedCount.equals(this.completedCount)) {
            this.batch.incrementCompleted();
        }
    }
}
