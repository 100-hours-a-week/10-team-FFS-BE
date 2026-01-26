package com.example.kloset_lab.clothes.dto;

import lombok.Builder;

@Builder
public record ClothesAnalysisResponse(String batchId, int total, int passed, int failed) {}
