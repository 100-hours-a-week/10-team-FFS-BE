package com.example.kloset_lab.global.ai.kafka.dto;

import com.example.kloset_lab.global.config.RawJsonDeserializer;
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
