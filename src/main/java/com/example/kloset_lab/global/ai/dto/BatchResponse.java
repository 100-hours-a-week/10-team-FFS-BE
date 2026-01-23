package com.example.kloset_lab.global.ai.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record BatchResponse(String batchId, BatchStatus status, Meta meta, List<TaskResult> results) {
    @Builder
    public record Meta(Integer total, Integer completed, Integer processing, Boolean isFinished) {}

    @Builder
    public record TaskResult(String taskId, TaskStatus status, Long fileId, Attributes attributes) {}

    @Builder
    public record Attributes(String category, List<String> color, List<String> material, List<String> styleTags) {}
}
