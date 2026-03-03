package com.example.kloset_lab.clothes.dto;

import com.example.kloset_lab.ai.entity.BatchStatus;
import com.example.kloset_lab.ai.entity.TaskStatus;
import com.example.kloset_lab.ai.infrastructure.http.dto.MajorFeature;
import com.example.kloset_lab.ai.infrastructure.http.dto.Meta;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClothesPollingResponse(String batchId, BatchStatus status, Meta meta, List<TaskResult> results) {

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TaskResult(String taskId, TaskStatus status, Long fileId, String imageUrl, MajorFeature major) {}
}
