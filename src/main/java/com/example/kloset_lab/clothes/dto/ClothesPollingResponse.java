package com.example.kloset_lab.clothes.dto;

import com.example.kloset_lab.global.ai.dto.BatchStatus;
import com.example.kloset_lab.global.ai.dto.TaskStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClothesPollingResponse(String batchId, BatchStatus status, Meta meta, List<TaskResult> results) {

    @Builder
    public record Meta(int total, int completed, int processing, boolean isFinished) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TaskResult(String taskId, TaskStatus status, String imageUrl, Analysis analysis) {}

    @Builder
    public record Analysis(Attributes attributes) {}

    @Builder
    public record Attributes(String category, List<String> color, List<String> material, List<String> styleTags) {}
}
