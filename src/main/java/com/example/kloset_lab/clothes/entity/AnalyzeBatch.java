package com.example.kloset_lab.clothes.entity;

import com.example.kloset_lab.global.entity.BaseTimeEntity;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "analyze_batch")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalyzeBatch extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false, unique = true, length = 26)
    private String batchId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(30)")
    private BatchStatus status;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "completed_count", nullable = false)
    private Integer completedCount;

    public AnalyzeBatch(Long userId, int totalCount) {
        this.batchId = UlidCreator.getMonotonicUlid().toString();
        this.userId = userId;
        this.status = BatchStatus.RUNNING;
        this.totalCount = totalCount;
        this.completedCount = 0;
    }

    public void incrementCompleted() {
        this.completedCount++;
        if (this.completedCount.equals(this.totalCount)) {
            this.status = BatchStatus.COMPLETED;
        }
    }
}
