package com.example.kloset_lab.global.ai.dto;

import lombok.Builder;

@Builder
public record ClothesDto(Long clothesId, String imageUrl, String name) {}
