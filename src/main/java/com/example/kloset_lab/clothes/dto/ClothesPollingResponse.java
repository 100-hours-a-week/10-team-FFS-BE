package com.example.kloset_lab.clothes.dto;

import com.example.kloset_lab.global.ai.dto.BatchStatus;
import com.example.kloset_lab.global.ai.dto.MajorFeature;
import com.example.kloset_lab.global.ai.dto.Meta;
import com.example.kloset_lab.global.ai.dto.TaskStatus;
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
