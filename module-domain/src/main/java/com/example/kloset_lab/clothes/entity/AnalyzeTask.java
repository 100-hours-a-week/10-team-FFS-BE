package com.example.kloset_lab.clothes.entity;

import com.example.kloset_lab.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "analyze_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalyzeTask extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, unique = true, length = 26)
    private String taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private AnalyzeSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(30)")
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
    public AnalyzeTask(String taskId, AnalyzeSource source, Long fileId) {
        this.taskId = taskId;
        this.source = source;
        this.status = TaskStatus.ANALYZING;
        this.fileId = fileId;
    }

    public void completeAnalysis(String major, String extra) {
        this.status = TaskStatus.ANALYZING_COMPLETED;
        this.major = major;
        this.extra = extra;
        this.source.incrementTaskCompleted();
    }
}
