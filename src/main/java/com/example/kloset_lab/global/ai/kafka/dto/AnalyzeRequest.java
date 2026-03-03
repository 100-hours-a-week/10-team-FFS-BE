package com.example.kloset_lab.global.ai.kafka.dto;

public record AnalyzeRequest(String batchId, String sourceId, Long userId, String targetImage) {}
