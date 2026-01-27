package com.example.kloset_lab.global.ai.dto;

import com.example.kloset_lab.clothes.entity.Category;
import lombok.Builder;

import java.util.List;

@Builder
public record MajorFeature(
        Category category,
        List<String> color,
        List<String> material,
        List<String> styleTags
) {
}
