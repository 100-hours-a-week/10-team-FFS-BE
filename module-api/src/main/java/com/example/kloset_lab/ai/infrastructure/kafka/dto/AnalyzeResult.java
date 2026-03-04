package com.example.kloset_lab.ai.infrastructure.kafka.dto;

import com.example.kloset_lab.ai.infrastructure.http.RawJsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public record AnalyzeResult(
        String batchId,
        String sourceId,
        Boolean passed,
        Integer segmentation,
        String taskId,
        Long fileId,
        @JsonDeserialize(using = RawJsonDeserializer.class) String major,
        @JsonDeserialize(using = RawJsonDeserializer.class) String extra) {}
