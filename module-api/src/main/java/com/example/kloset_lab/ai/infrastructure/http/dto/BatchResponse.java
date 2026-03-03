package com.example.kloset_lab.ai.infrastructure.http.dto;

import com.example.kloset_lab.ai.entity.BatchStatus;
import com.example.kloset_lab.ai.entity.TaskStatus;
import com.example.kloset_lab.ai.infrastructure.http.RawJsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import lombok.Builder;

@Builder
public record BatchResponse(String batchId, BatchStatus status, Meta meta, List<TaskResult> results) {
    @Builder
    public record TaskResult(
            String taskId,
            TaskStatus status,
            Long fileId,
            @JsonDeserialize(using = RawJsonDeserializer.class) String major,
            @JsonDeserialize(using = RawJsonDeserializer.class) String extra) {}
}
