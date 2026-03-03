package com.example.kloset_lab.clothes.dto;

import com.example.kloset_lab.clothes.entity.BatchStatus;
import com.example.kloset_lab.clothes.entity.SourceStatus;
import com.example.kloset_lab.clothes.entity.TaskStatus;
import com.example.kloset_lab.global.ai.http.dto.MajorFeature;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClothesPollingResponse(String batchId, BatchStatus status, List<SourceResult> results) {

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SourceResult(
            String sourceId, SourceStatus status, Boolean passed, Integer detectedCount, List<TaskResult> tasks) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TaskResult(String taskId, TaskStatus status, Long fileId, String imageUrl, MajorFeature major) {}
}
