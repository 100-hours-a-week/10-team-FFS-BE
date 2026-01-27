package com.example.kloset_lab.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;

@Builder
public record EmbeddingRequest(
        Long userId, Long clothesId, String imageUrl, MajorFeature major, @JsonRawValue String extra) {}
