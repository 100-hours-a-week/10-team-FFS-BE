package com.example.kloset_lab.ai.infrastructure.http.dto;

import lombok.Builder;

@Builder
public record EmbeddingResponse(Long clothesId, Boolean indexed) {}
