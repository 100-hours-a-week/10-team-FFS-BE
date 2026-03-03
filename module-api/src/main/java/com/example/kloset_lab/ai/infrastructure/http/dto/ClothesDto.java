package com.example.kloset_lab.ai.infrastructure.http.dto;

import lombok.Builder;

@Builder
public record ClothesDto(Long clothesId, String imageUrl, String name) {}
