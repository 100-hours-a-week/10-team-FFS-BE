package com.example.kloset_lab.ai.infrastructure.http.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record OutfitResponse(String querySummary, List<Outfit> outfits, String sessionId) {
    @Builder
    public record Outfit(
            String outfitId, String description, String fallbackNotice, List<Long> clothesIds, Long fileId) {}
}
