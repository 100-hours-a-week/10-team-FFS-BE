package com.example.kloset_lab.global.ai.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record EmbeddingRequest(Long userId, Long clothesId, String imageUrl, Metadata metadata) {
    @Builder
    public record Metadata(String category, List<String> color, List<String> material, List<String> styleTags) {}
}
