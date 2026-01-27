package com.example.kloset_lab.global.ai.dto;

import com.example.kloset_lab.global.security.config.RawJsonDeserializer;
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
