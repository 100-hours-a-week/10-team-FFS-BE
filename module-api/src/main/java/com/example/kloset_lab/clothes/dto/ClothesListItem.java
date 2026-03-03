package com.example.kloset_lab.clothes.dto;

import lombok.Builder;

@Builder
public record ClothesListItem(Long clothesId, String imageUrl) {}
