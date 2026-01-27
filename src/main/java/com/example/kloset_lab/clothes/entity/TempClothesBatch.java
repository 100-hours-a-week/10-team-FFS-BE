package com.example.kloset_lab.clothes.entity;

import com.example.kloset_lab.global.ai.dto.BatchStatus;
import com.example.kloset_lab.global.entity.BaseTimeEntity;
import com.example.kloset_lab.user.entity.User;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "temp_clothes_batch")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TempClothesBatch extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "batch_id", nullable = false, unique = true, length = 50)
    private String batchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BatchStatus status;

    @Column(name = "total", nullable = false)
    private int total;

    @Column(name = "completed", nullable = false)
    private int completed;

    @Column(name = "processing", nullable = false)
    private int processing;

    @Column(name = "is_finished", nullable = false)
    private boolean isFinished;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TempClothesTask> tasks = new ArrayList<>();

    @Builder
    public TempClothesBatch(User user, String batchId, BatchStatus status, int total) {
        this.user = user;
        this.batchId = batchId;
        this.status = status;
        this.total = total;
        this.completed = 0;
        this.processing = total;
        this.isFinished = false;
    }

    public void updateMeta(BatchStatus status, int completed, int processing, boolean isFinished) {
        this.status = status;
        this.completed = completed;
        this.processing = processing;
        this.isFinished = isFinished;
    }

    public boolean isOwner(Long userId) {
        return this.user.getId().equals(userId);
    }

    public void addTask(TempClothesTask task) {
        this.tasks.add(task);
        task.setBatch(this);
    }
}
