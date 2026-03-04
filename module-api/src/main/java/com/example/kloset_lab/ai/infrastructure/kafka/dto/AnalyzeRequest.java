package com.example.kloset_lab.ai.infrastructure.kafka.dto;

public record AnalyzeRequest(String batchId, String sourceId, Long userId, String targetImage) {}
