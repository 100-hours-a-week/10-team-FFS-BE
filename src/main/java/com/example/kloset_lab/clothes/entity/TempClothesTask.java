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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "file_id")
    private Long fileId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "major", columnDefinition = "JSON")
    private String major;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra", columnDefinition = "JSON")
    private String extra;

    @Builder
    public TempClothesTask(String taskId, TaskStatus status) {
        this.taskId = taskId;
        this.status = status;
    }

    public void updateResult(TaskStatus status, Long fileId, String major, String extra) {
        this.status = status;
        this.fileId = fileId;
        this.major = major;
        this.extra = extra;
    }

    public void setBatch(TempClothesBatch tempClothesBatch) {
        this.batch = tempClothesBatch;
    }
}
