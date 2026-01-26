package com.example.kloset_lab.clothes.entity;

import com.example.kloset_lab.global.ai.dto.TaskStatus;
import com.example.kloset_lab.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "temp_clothes_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TempClothesTask extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private TempClothesBatch batch;

    @Column(name = "task_id", nullable = false, unique = true, length = 50)
    private String taskId;

    @Column(name = "file_id")
    private Long fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis", columnDefinition = "JSON")
    private String analysis;

    @Builder
    public TempClothesTask(String taskId, Long fileId, TaskStatus status) {
        this.taskId = taskId;
        this.fileId = fileId;
        this.status = status;
    }

    public void updateResult(TaskStatus status, String analysis) {
        this.status = status;
        this.analysis = analysis;
    }

    public void setBatch(TempClothesBatch tempClothesBatch) {
        this.batch = tempClothesBatch;
    }
}
