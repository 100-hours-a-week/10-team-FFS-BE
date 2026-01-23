package com.example.kloset_lab.global.ai.dto;

import lombok.Builder;

@Builder
public record EmbeddingResponse(Long clothesId, String caption, Boolean indexed) {}
